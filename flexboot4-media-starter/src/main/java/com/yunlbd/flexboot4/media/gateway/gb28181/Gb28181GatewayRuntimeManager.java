package com.yunlbd.flexboot4.media.gateway.gb28181;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaCascadePlatform;
import com.yunlbd.flexboot4.entity.media.MediaChannel;
import com.yunlbd.flexboot4.entity.media.MediaDevice;
import com.yunlbd.flexboot4.entity.media.MediaGateway;
import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.entity.media.MediaStreamSession;
import com.yunlbd.flexboot4.media.core.ZlmClient;
import com.yunlbd.flexboot4.media.dto.PlaybackQueryRequest;
import com.yunlbd.flexboot4.media.dto.PlaybackRecordItem;
import com.yunlbd.flexboot4.media.dto.PlaybackStartRequest;
import com.yunlbd.flexboot4.media.dto.PtzControlRequest;
import com.yunlbd.flexboot4.media.enums.MediaAccessType;
import com.yunlbd.flexboot4.media.enums.MediaOnlineStatus;
import com.yunlbd.flexboot4.media.enums.MediaSessionStatus;
import com.yunlbd.flexboot4.media.enums.MediaSessionType;
import com.yunlbd.flexboot4.service.media.MediaCascadePlatformService;
import com.yunlbd.flexboot4.service.media.MediaChannelService;
import com.yunlbd.flexboot4.service.media.MediaDeviceService;
import com.yunlbd.flexboot4.service.media.MediaGatewayRuntimeManager;
import com.yunlbd.flexboot4.service.media.MediaServerService;
import com.yunlbd.flexboot4.service.media.MediaStreamSessionService;
import gov.nist.javax.sip.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class Gb28181GatewayRuntimeManager implements MediaGatewayRuntimeManager {

    private static final DateTimeFormatter GB_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final long NONCE_TTL_SECONDS = 300;
    private static final int DEFAULT_CATALOG_PAGE_SIZE = 100;

    private final MediaServerService mediaServerService;
    private final MediaDeviceService mediaDeviceService;
    private final MediaStreamSessionService mediaStreamSessionService;
    private final MediaCascadePlatformService mediaCascadePlatformService;
    private final MediaChannelService mediaChannelService;

    private final Map<String, RuntimeState> runtimes = new ConcurrentHashMap<>();

    public Gb28181GatewayRuntimeManager(
            MediaServerService mediaServerService,
            MediaDeviceService mediaDeviceService,
            MediaStreamSessionService mediaStreamSessionService,
            @Lazy MediaCascadePlatformService mediaCascadePlatformService,
            @Lazy MediaChannelService mediaChannelService) {
        this.mediaServerService = mediaServerService;
        this.mediaDeviceService = mediaDeviceService;
        this.mediaStreamSessionService = mediaStreamSessionService;
        this.mediaCascadePlatformService = mediaCascadePlatformService;
        this.mediaChannelService = mediaChannelService;
    }

    @Override
    public boolean reload(MediaGateway gateway, boolean autoStart) {
        stop(gateway.getId());
        return !autoStart || start(gateway);
    }

    @Override
    public boolean start(MediaGateway gateway) {
        stop(gateway.getId());
        try {
            SipFactory sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");
            Properties properties = new Properties();
            properties.setProperty("javax.sip.STACK_NAME", "FlexBoot4Media-" + gateway.getId());
            properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "0");

            String bindIp = normalizeHost(gateway.getLocalIp());
            String transport = normalizeTransport(gateway.getTransport());
            int bindPort = gateway.getLocalPort() == null ? 5060 : gateway.getLocalPort();

            SipStack sipStack = sipFactory.createSipStack(properties);
            AddressFactory addressFactory = sipFactory.createAddressFactory();
            HeaderFactory headerFactory = sipFactory.createHeaderFactory();
            MessageFactory messageFactory = sipFactory.createMessageFactory();
            ListeningPoint listeningPoint = sipStack.createListeningPoint(bindIp, bindPort, transport);
            SipProvider sipProvider = sipStack.createSipProvider(listeningPoint);
            RuntimeState state = new RuntimeState(gateway, sipStack, sipProvider, listeningPoint, addressFactory, headerFactory, messageFactory);
            GatewaySipListener listener = new GatewaySipListener(state);
            state.listener = listener;
            sipProvider.addSipListener(listener);
            runtimes.put(gateway.getId(), state);
            return true;
        } catch (Exception e) {
            log.error("Failed to start GB28181 gateway {}", gateway.getGatewayName(), e);
            return false;
        }
    }

    @Override
    public boolean stop(String gatewayId) {
        RuntimeState state = runtimes.remove(gatewayId);
        if (state == null) {
            return true;
        }
        try {
            if (state.listener != null) {
                state.provider.removeSipListener(state.listener);
            }
        } catch (Exception ignored) {
        }
        try {
            state.stack.deleteListeningPoint(state.listeningPoint);
        } catch (ObjectInUseException ignored) {
        }
        try {
            state.stack.deleteSipProvider(state.provider);
        } catch (ObjectInUseException ignored) {
        }
        state.stack.stop();
        return true;
    }

    @Override
    public boolean isRunning(String gatewayId) {
        return gatewayId != null && runtimes.containsKey(gatewayId);
    }

    @Override
    public MediaStreamSession startLive(MediaChannel channel) {
        RuntimeState state = requireRuntime(channel.getGatewayId());
        MediaDevice device = requireDevice(channel.getDeviceId());
        DeviceRegistration registration = resolveRegistration(state, device);
        MediaStreamSession session = baseSession(channel, MediaSessionType.LIVE);
        MediaServer server = resolveServer(channel.getServerId(), device.getServerId(), state.gateway.getServerId());
        if (server != null) {
            ZlmClient client = mediaServerService.createClient(server);
            int rtpPort = client.openRtpServer(session.getStreamId(), 0, 0);
            session.setRtpPort(rtpPort);
            session.setServerId(server.getId());
            session.setPlayUrl(mediaServerService.buildPlayUrls(server.getId(), session.getStreamApp(), session.getStreamId()).get("http-flv"));
        }
        sendInvite(state, device.getDeviceCode(), registration, session, buildLiveSdp(state, session));
        return session;
    }

    @Override
    public boolean stopLive(MediaStreamSession session) {
        MediaServer server = resolveServer(session.getServerId(), null, null);
        if (server != null && session.getStreamId() != null) {
            try {
                mediaServerService.createClient(server).closeRtpServer(session.getStreamId());
            } catch (Exception e) {
                log.warn("Failed to close RTP server {}", session.getStreamId(), e);
            }
        }
        RuntimeState state = session.getGatewayId() == null ? null : runtimes.get(session.getGatewayId());
        if (state != null && session.getDialogId() != null) {
            Dialog dialog = state.dialogs.remove(session.getDialogId());
            if (dialog != null) {
                try {
                    Request bye = dialog.createRequest(Request.BYE);
                    ClientTransaction tx = state.provider.getNewClientTransaction(bye);
                    dialog.sendRequest(tx);
                } catch (Exception e) {
                    log.warn("Failed to send BYE for session {}", session.getId(), e);
                }
            }
        }
        return true;
    }

    @Override
    public List<PlaybackRecordItem> queryPlayback(MediaChannel channel, PlaybackQueryRequest request) {
        RuntimeState state = requireRuntime(channel.getGatewayId());
        MediaDevice device = requireDevice(channel.getDeviceId());
        DeviceRegistration registration = resolveRegistration(state, device);
        int sn = state.sn.incrementAndGet();
        String key = playbackKey(device.getDeviceCode(), sn);
        CompletableFuture<List<PlaybackRecordItem>> future = new CompletableFuture<>();
        state.recordQueries.put(key, future);
        sendMessage(state, device.getDeviceCode(), registration, buildPlaybackQueryXml(channel.getChannelCode(), request, sn));
        try {
            return future.completeOnTimeout(List.of(), 5, TimeUnit.SECONDS).join();
        } finally {
            state.recordQueries.remove(key);
        }
    }

    @Override
    public MediaStreamSession startPlayback(MediaChannel channel, PlaybackStartRequest request) {
        RuntimeState state = requireRuntime(channel.getGatewayId());
        MediaDevice device = requireDevice(channel.getDeviceId());
        DeviceRegistration registration = resolveRegistration(state, device);
        MediaStreamSession session = baseSession(channel, MediaSessionType.PLAYBACK);
        MediaServer server = resolveServer(channel.getServerId(), device.getServerId(), state.gateway.getServerId());
        if (server != null) {
            int rtpPort = mediaServerService.createClient(server).openRtpServer(session.getStreamId(), 0, 0);
            session.setRtpPort(rtpPort);
            session.setServerId(server.getId());
            session.setPlayUrl(mediaServerService.buildPlayUrls(server.getId(), session.getStreamApp(), session.getStreamId()).get("http-flv"));
        }
        sendInvite(state, device.getDeviceCode(), registration, session, buildPlaybackSdp(state, channel, session, request));
        return session;
    }

    @Override
    public boolean stopPlayback(MediaStreamSession session) {
        return stopLive(session);
    }

    @Override
    public boolean ptz(MediaChannel channel, PtzControlRequest request) {
        RuntimeState state = requireRuntime(channel.getGatewayId());
        MediaDevice device = requireDevice(channel.getDeviceId());
        DeviceRegistration registration = resolveRegistration(state, device);
        sendMessage(state, device.getDeviceCode(), registration, buildPtzXml(channel.getChannelCode(), request));
        return true;
    }

    @Override
    public boolean registerCascade(MediaCascadePlatform platform) {
        RuntimeState state = requireRuntime(platform.getGatewayId());
        return sendRegister(
                state,
                platform.getSipId(),
                new DeviceRegistration(platform.getHost(), platform.getPort() == null ? 5060 : platform.getPort(), normalizeTransport(platform.getTransport())),
                platform.getSipDomain(),
                platform.getSipPassword(),
                platform.getId()
        );
    }

    @Override
    public boolean stopCascade(MediaCascadePlatform platform) {
        return true;
    }

    private void handleRegister(RuntimeState state, RequestEvent event) throws Exception {
        Request request = event.getRequest();
        String deviceId = extractUser(((FromHeader) request.getHeader(FromHeader.NAME)).getAddress());
        ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
        ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
        if (requiresRegisterAuth(state.gateway)) {
            AuthorizationHeader authorizationHeader = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
            Gb28181DigestUtils.NonceToken nonceToken = state.nonces.get(deviceId);
            boolean valid = Gb28181DigestUtils.validateAuthorization(
                    authorizationHeader,
                    request,
                    deviceId,
                    state.gateway.getSipDomain(),
                    state.gateway.getSipPassword(),
                    nonceToken,
                    NONCE_TTL_SECONDS
            );
            if (!valid) {
                challengeRegister(state, event, deviceId);
                return;
            }
        }
        int expires = resolveExpires(request, contactHeader);
        String host = viaHeader.getReceived() != null ? viaHeader.getReceived() : viaHeader.getHost();
        int port = resolvePort(contactHeader, viaHeader);
        String transport = viaHeader.getTransport() == null ? normalizeTransport(state.gateway.getTransport()) : viaHeader.getTransport().toUpperCase();

        sendOk(state, event);
        if (expires == 0) {
            state.registrations.remove(deviceId);
            MediaDevice device = findDeviceByCode(deviceId);
            if (device != null) {
                device.setOnlineStatus(MediaOnlineStatus.OFFLINE);
                device.setRegisterStatus("UNREGISTERED");
                mediaDeviceService.updateById(device, true);
            }
            return;
        }

        state.registrations.put(deviceId, new DeviceRegistration(host, port, transport));
        MediaDevice device = findDeviceByCode(deviceId);
        if (device == null) {
            device = mediaDeviceService.upsertGbDevice(state.gateway.getId(), deviceId, deviceId);
        }
        device.setGatewayId(state.gateway.getId());
        device.setServerId(state.gateway.getServerId());
        device.setAccessType(MediaAccessType.GB28181);
        device.setIp(host);
        device.setPort(port);
        device.setOnlineStatus(MediaOnlineStatus.ONLINE);
        device.setRegisterStatus("REGISTERED");
        device.setLastRegisterTime(LocalDateTime.now());
        mediaDeviceService.updateById(device, true);

        requestCatalogPage(state, deviceId, state.registrations.get(deviceId), 1, DEFAULT_CATALOG_PAGE_SIZE);
        sendMessage(state, deviceId, state.registrations.get(deviceId), Gb28181XmlUtils.deviceInfoQuery(deviceId, state.sn.incrementAndGet()));
    }

    private void handleMessage(RuntimeState state, RequestEvent event) throws Exception {
        Request request = event.getRequest();
        String xml = request.getRawContent() == null ? null : new String(request.getRawContent());
        Gb28181XmlUtils.Gb28181Message message = Gb28181XmlUtils.parse(xml);
        sendOk(state, event);
        if (message.cmdType() == null) {
            return;
        }
        String deviceId = message.deviceId() != null ? message.deviceId() : extractUser(((FromHeader) request.getHeader(FromHeader.NAME)).getAddress());
        if (deviceId == null || deviceId.isBlank()) {
            log.warn("Ignore GB28181 MESSAGE without device id, cmdType={}", message.cmdType());
            return;
        }
        String cmdType = message.cmdType().toUpperCase(Locale.ROOT);
        switch (cmdType) {
            case "KEEPALIVE", "MOBILEPOSITION" -> {
                state.registrations.computeIfAbsent(deviceId, ignored -> buildRegistrationFromRequest(request));
                mediaDeviceService.markKeepalive(deviceId, LocalDateTime.now());
            }
            case "CATALOG" -> handleCatalogMessage(state, deviceId, message);
            case "DEVICEINFO" -> handleDeviceInfoMessage(state, deviceId, message);
            case "DEVICESTATUS", "MEDIASTATUS" -> handleDeviceStatusMessage(state, deviceId, message);
            case "ALARM" -> handleAlarmMessage(state, deviceId, message);
            case "RECORDINFO" -> handleRecordInfoMessage(state, deviceId, message);
            default -> log.debug("Unhandled GB28181 MESSAGE cmdType={} deviceId={}", message.cmdType(), deviceId);
        }
    }

    private void handleCatalogMessage(RuntimeState state, String deviceId, Gb28181XmlUtils.Gb28181Message message) {
        MediaDevice device = ensureDevice(state, deviceId, message.deviceName());
        device.setLastCatalogTime(LocalDateTime.now());
        mediaDeviceService.updateById(device, true);

        for (Gb28181XmlUtils.Gb28181CatalogItem item : message.items()) {
            if (item.deviceId() == null || item.deviceId().isBlank()) {
                continue;
            }
            String event = item.event() == null ? "" : item.event().toUpperCase(Locale.ROOT);
            if ("DEL".equals(event)) {
                removeChannelByCode(item.deviceId());
                continue;
            }
            upsertCatalogChannel(state, device, item);
        }

        CatalogCursor cursor = state.catalogQueries.computeIfAbsent(deviceId, ignored -> new CatalogCursor(DEFAULT_CATALOG_PAGE_SIZE));
        int start = message.startNum() != null ? message.startNum() : cursor.requestStart();
        int received = message.items().size();
        int limit = message.deviceListNum() != null && message.deviceListNum() > 0 ? message.deviceListNum() : cursor.limit();
        int total = message.sumNum() != null && message.sumNum() > 0 ? message.sumNum() : cursor.total();
        if (total <= 0) {
            total = received;
        }

        for (Gb28181XmlUtils.Gb28181CatalogItem item : message.items()) {
            if (item.deviceId() != null && !item.deviceId().isBlank()) {
                cursor.itemKeys().put(item.deviceId(), Boolean.TRUE);
            }
        }
        cursor.setLimit(Math.max(1, limit));
        cursor.setTotal(Math.max(total, cursor.itemKeys().size()));
        cursor.setRequestStart(Math.max(1, start));

        int nextStart = start + Math.max(0, received);
        if (received > 0 && nextStart <= cursor.total()) {
            if (nextStart <= cursor.requestStart()) {
                state.catalogQueries.remove(deviceId);
                return;
            }
            cursor.setRequestStart(nextStart);
            DeviceRegistration registration = state.registrations.get(deviceId);
            if (registration != null) {
                requestCatalogPage(state, deviceId, registration, nextStart, cursor.limit());
            } else {
                state.catalogQueries.remove(deviceId);
            }
            return;
        }
        state.catalogQueries.remove(deviceId);
    }

    private void handleDeviceInfoMessage(RuntimeState state, String deviceId, Gb28181XmlUtils.Gb28181Message message) {
        MediaDevice device = ensureDevice(state, deviceId, message.deviceName());
        if (message.deviceName() != null && !message.deviceName().isBlank()) {
            device.setDeviceName(message.deviceName());
        }
        if (message.manufacturer() != null && !message.manufacturer().isBlank()) {
            device.setManufacturer(message.manufacturer());
        }
        if (message.model() != null && !message.model().isBlank()) {
            device.setModel(message.model());
        }
        if (message.owner() != null && !message.owner().isBlank()) {
            device.setOwner(message.owner());
        }
        if (message.civilCode() != null && !message.civilCode().isBlank()) {
            device.setCivilCode(message.civilCode());
        }
        if (message.address() != null && !message.address().isBlank()) {
            device.setAddress(message.address());
        }
        mediaDeviceService.updateById(device, true);
    }

    private void handleDeviceStatusMessage(RuntimeState state, String deviceId, Gb28181XmlUtils.Gb28181Message message) {
        MediaDevice device = ensureDevice(state, deviceId, message.deviceName());
        device.setOnlineStatus(normalizeStatus(message.status()));
        if (MediaOnlineStatus.ONLINE.equals(device.getOnlineStatus())) {
            device.setRegisterStatus("REGISTERED");
            device.setLastKeepaliveTime(LocalDateTime.now());
        } else if (MediaOnlineStatus.OFFLINE.equals(device.getOnlineStatus())) {
            device.setRegisterStatus("UNREGISTERED");
        }
        mediaDeviceService.updateById(device, true);
    }

    private void handleAlarmMessage(RuntimeState state, String deviceId, Gb28181XmlUtils.Gb28181Message message) {
        MediaDevice device = ensureDevice(state, deviceId, message.deviceName());
        device.setLastKeepaliveTime(LocalDateTime.now());
        mediaDeviceService.updateById(device, true);
        log.warn(
                "GB28181 alarm received deviceId={} method={} priority={} time={} description={}",
                deviceId,
                message.alarmMethod(),
                message.alarmPriority(),
                message.alarmTime(),
                message.alarmDescription()
        );
    }

    private void handleRecordInfoMessage(RuntimeState state, String deviceId, Gb28181XmlUtils.Gb28181Message message) {
        String key = playbackKey(deviceId, message.sn());
        CompletableFuture<List<PlaybackRecordItem>> future = state.recordQueries.remove(key);
        if (future != null) {
            future.complete(message.records());
        }
    }

    private void upsertCatalogChannel(RuntimeState state, MediaDevice device, Gb28181XmlUtils.Gb28181CatalogItem item) {
        MediaChannel channel = MediaChannel.builder()
                .serverId(state.gateway.getServerId())
                .gatewayId(state.gateway.getId())
                .deviceId(device.getId())
                .parentChannelId(item.parentId())
                .channelName(item.name() == null || item.name().isBlank() ? item.deviceId() : item.name())
                .channelCode(item.deviceId())
                .channelType("VIDEO")
                .manufacturer(item.manufacturer())
                .model(item.model())
                .owner(item.owner())
                .civilCode(item.civilCode())
                .address(item.address())
                .status(normalizeStatus(item.status()))
                .playStatus("IDLE")
                .longitude(item.longitude())
                .latitude(item.latitude())
                .streamApp("rtp")
                .streamId(item.deviceId())
                .build();
        mediaChannelService.upsertChannel(channel);
    }

    private void removeChannelByCode(String channelCode) {
        MediaChannel channel = mediaChannelService.getOne(QueryWrapper.create()
                .from(MediaChannel.class)
                .where(MediaChannel::getChannelCode).eq(channelCode));
        if (channel != null && channel.getId() != null) {
            mediaChannelService.removeById(channel.getId());
        }
    }

    private MediaDevice ensureDevice(RuntimeState state, String deviceId, String fallbackName) {
        MediaDevice device = findDeviceByCode(deviceId);
        if (device != null) {
            return device;
        }
        String name = fallbackName == null || fallbackName.isBlank() ? deviceId : fallbackName;
        return mediaDeviceService.upsertGbDevice(state.gateway.getId(), deviceId, name);
    }

    private void handleBye(RuntimeState state, RequestEvent event) throws Exception {
        sendOk(state, event);
        CallIdHeader callIdHeader = (CallIdHeader) event.getRequest().getHeader(CallIdHeader.NAME);
        if (callIdHeader == null) {
            return;
        }
        MediaStreamSession session = findSessionByDialog(callIdHeader.getCallId());
        if (session != null) {
            mediaStreamSessionService.closeSession(session.getId(), MediaSessionStatus.CLOSED, LocalDateTime.now());
        }
    }

    private void handleResponse(RuntimeState state, ResponseEvent event) {
        Response response = event.getResponse();
        CSeqHeader cSeqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
        if (cSeqHeader == null || callIdHeader == null) {
            return;
        }
        if (Request.REGISTER.equalsIgnoreCase(cSeqHeader.getMethod())) {
            handleRegisterResponse(state, response, callIdHeader.getCallId());
            return;
        }
        if (Request.INVITE.equalsIgnoreCase(cSeqHeader.getMethod()) && response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            try {
                Dialog dialog = event.getDialog();
                if (dialog != null) {
                    state.dialogs.put(callIdHeader.getCallId(), dialog);
                    Request ack = dialog.createAck(cSeqHeader.getSeqNumber());
                    dialog.sendAck(ack);
                }
            } catch (Exception e) {
                log.warn("Failed to ACK INVITE response {}", callIdHeader.getCallId(), e);
            }
            MediaStreamSession session = findSessionByDialog(callIdHeader.getCallId());
            if (session != null) {
                session.setStatus(MediaSessionStatus.PENDING);
                mediaStreamSessionService.updateById(session, true);
            }
            return;
        }
        if (Request.INVITE.equalsIgnoreCase(cSeqHeader.getMethod()) && response.getStatusCode() >= 300) {
            MediaStreamSession session = findSessionByDialog(callIdHeader.getCallId());
            if (session != null) {
                mediaStreamSessionService.closeSession(session.getId(), MediaSessionStatus.FAILED, LocalDateTime.now());
            }
        }
    }

    private void handleRegisterResponse(RuntimeState state, Response response, String callId) {
        PendingRegister pending = state.pendingRegisters.get(callId);
        if (pending == null) {
            return;
        }
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            state.pendingRegisters.remove(callId);
            updateCascadeRegisterStatus(pending.platformId(), true, null);
            return;
        }
        if (response.getStatusCode() != Response.UNAUTHORIZED && response.getStatusCode() != Response.PROXY_AUTHENTICATION_REQUIRED) {
            state.pendingRegisters.remove(callId);
            updateCascadeRegisterStatus(
                    pending.platformId(),
                    false,
                    "Cascade register failed: SIP " + response.getStatusCode() + " " + response.getReasonPhrase()
            );
            return;
        }
        if (pending.authenticated()) {
            state.pendingRegisters.remove(callId);
            updateCascadeRegisterStatus(
                    pending.platformId(),
                    false,
                    "Cascade register failed: SIP " + response.getStatusCode() + " " + response.getReasonPhrase()
            );
            return;
        }
        WWWAuthenticateHeader authenticateHeader = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
        if (authenticateHeader == null) {
            authenticateHeader = (WWWAuthenticateHeader) response.getHeader(ProxyAuthenticateHeader.NAME);
        }
        if (authenticateHeader == null) {
            state.pendingRegisters.remove(callId);
            updateCascadeRegisterStatus(pending.platformId(), false, "Cascade register failed: missing authenticate header");
            return;
        }
        try {
            Request retry = createRegisterRequest(
                    state,
                    pending.targetId(),
                    pending.registration(),
                    pending.domain(),
                    pending.password(),
                    authenticateHeader
            );
            String retryCallId = ((CallIdHeader) retry.getHeader(CallIdHeader.NAME)).getCallId();
            state.pendingRegisters.remove(callId);
            state.pendingRegisters.put(retryCallId, new PendingRegister(
                    pending.targetId(),
                    pending.registration(),
                    pending.domain(),
                    pending.password(),
                    pending.platformId(),
                    true
            ));
            state.provider.getNewClientTransaction(retry).sendRequest();
        } catch (Exception e) {
            log.warn("Failed to retry REGISTER with digest for {}", pending.targetId(), e);
            state.pendingRegisters.remove(callId);
            updateCascadeRegisterStatus(pending.platformId(), false, "Cascade register failed: " + e.getMessage());
        }
    }

    private void sendOk(RuntimeState state, RequestEvent event) throws Exception {
        Response response = state.messageFactory.createResponse(Response.OK, event.getRequest());
        ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
        if (toHeader != null && toHeader.getTag() == null) {
            toHeader.setTag(randomTag());
        }
        ServerTransaction transaction = event.getServerTransaction();
        if (transaction == null) {
            transaction = state.provider.getNewServerTransaction(event.getRequest());
        }
        transaction.sendResponse(response);
    }

    private void challengeRegister(RuntimeState state, RequestEvent event, String deviceId) throws Exception {
        Gb28181DigestUtils.NonceToken token = Gb28181DigestUtils.issueNonce();
        if (deviceId != null && !deviceId.isBlank()) {
            state.nonces.put(deviceId, token);
        }
        Response response = state.messageFactory.createResponse(Response.UNAUTHORIZED, event.getRequest());
        ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
        if (toHeader != null && toHeader.getTag() == null) {
            toHeader.setTag(randomTag());
        }
        WWWAuthenticateHeader authenticateHeader = state.headerFactory.createWWWAuthenticateHeader("Digest");
        authenticateHeader.setRealm(state.gateway.getSipDomain());
        authenticateHeader.setNonce(token.value());
        authenticateHeader.setAlgorithm("MD5");
        response.addHeader(authenticateHeader);

        ServerTransaction transaction = event.getServerTransaction();
        if (transaction == null) {
            transaction = state.provider.getNewServerTransaction(event.getRequest());
        }
        transaction.sendResponse(response);
    }

    private void requestCatalogPage(RuntimeState state, String deviceId, DeviceRegistration registration, int startNum, int limit) {
        if (registration == null || deviceId == null || deviceId.isBlank()) {
            return;
        }
        CatalogCursor cursor = state.catalogQueries.computeIfAbsent(deviceId, ignored -> new CatalogCursor(limit));
        cursor.setLimit(Math.max(1, limit));
        cursor.setRequestStart(Math.max(1, startNum));
        sendMessage(
                state,
                deviceId,
                registration,
                Gb28181XmlUtils.catalogQuery(deviceId, state.sn.incrementAndGet(), cursor.requestStart(), cursor.limit())
        );
    }

    private void updateCascadeRegisterStatus(String platformId, boolean online, String error) {
        if (platformId == null || platformId.isBlank()) {
            return;
        }
        try {
            mediaCascadePlatformService.markRegisterStatus(platformId, online, error);
        } catch (Exception e) {
            log.warn("Failed to update cascade platform register status, platformId={}", platformId, e);
        }
    }

    private void sendMessage(RuntimeState state, String targetId, DeviceRegistration registration, String body) {
        try {
            Request request = createMessageRequest(state, targetId, registration, body);
            ClientTransaction transaction = state.provider.getNewClientTransaction(request);
            transaction.sendRequest();
        } catch (Exception e) {
            log.warn("Failed to send MESSAGE to {}", targetId, e);
        }
    }

    private void sendInvite(RuntimeState state, String targetId, DeviceRegistration registration, MediaStreamSession session, String sdp) {
        try {
            Request request = createInviteRequest(state, targetId, registration, session, sdp);
            ClientTransaction transaction = state.provider.getNewClientTransaction(request);
            transaction.sendRequest();
        } catch (Exception e) {
            log.warn("Failed to send INVITE to {}", targetId, e);
            session.setStatus(MediaSessionStatus.FAILED);
        }
    }

    private boolean sendRegister(RuntimeState state, String targetId, DeviceRegistration registration, String domain, String password, String platformId) {
        try {
            Request request = createRegisterRequest(state, targetId, registration, domain, password);
            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
            state.pendingRegisters.put(callIdHeader.getCallId(), new PendingRegister(targetId, registration, domain, password, platformId, false));
            ClientTransaction transaction = state.provider.getNewClientTransaction(request);
            transaction.sendRequest();
            return true;
        } catch (Exception e) {
            log.warn("Failed to send REGISTER to {}", targetId, e);
            updateCascadeRegisterStatus(platformId, false, "Cascade register failed: " + e.getMessage());
            return false;
        }
    }

    private Request createMessageRequest(RuntimeState state, String targetId, DeviceRegistration registration, String body) throws Exception {
        SipURI requestUri = state.addressFactory.createSipURI(targetId, registration.host());
        requestUri.setPort(registration.port());
        CallIdHeader callIdHeader = state.provider.getNewCallId();
        FromHeader fromHeader = createFromHeader(state, state.gateway.getSipId(), state.gateway.getSipDomain());
        ToHeader toHeader = createToHeader(state, targetId, state.gateway.getSipDomain());
        ArrayList<ViaHeader> viaHeaders = buildViaHeaders(state);
        MaxForwardsHeader maxForwardsHeader = state.headerFactory.createMaxForwardsHeader(70);
        CSeqHeader cSeqHeader = state.headerFactory.createCSeqHeader((long) state.sn.incrementAndGet(), Request.MESSAGE);
        Request request = state.messageFactory.createRequest(requestUri, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwardsHeader);
        request.addHeader(createContactHeader(state));
        request.setContent(body, state.headerFactory.createContentTypeHeader("Application", "MANSCDP+xml"));
        return request;
    }

    private Request createInviteRequest(RuntimeState state, String targetId, DeviceRegistration registration, MediaStreamSession session, String sdp) throws Exception {
        SipURI requestUri = state.addressFactory.createSipURI(targetId, registration.host());
        requestUri.setPort(registration.port());
        CallIdHeader callIdHeader = state.provider.getNewCallId();
        session.setDialogId(callIdHeader.getCallId());
        FromHeader fromHeader = createFromHeader(state, state.gateway.getSipId(), state.gateway.getSipDomain());
        ToHeader toHeader = createToHeader(state, targetId, state.gateway.getSipDomain());
        ArrayList<ViaHeader> viaHeaders = buildViaHeaders(state);
        MaxForwardsHeader maxForwardsHeader = state.headerFactory.createMaxForwardsHeader(70);
        CSeqHeader cSeqHeader = state.headerFactory.createCSeqHeader(1L, Request.INVITE);
        Request request = state.messageFactory.createRequest(requestUri, Request.INVITE, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwardsHeader);
        request.addHeader(createContactHeader(state));
        request.addHeader(state.headerFactory.createHeader("Subject", targetId + ":" + session.getSsrc() + "," + state.gateway.getSipId() + ":0"));
        request.addHeader(state.headerFactory.createHeader("User-Agent", "FlexBoot4-GB28181"));
        request.setContent(sdp, state.headerFactory.createContentTypeHeader("APPLICATION", "SDP"));
        return request;
    }

    private Request createRegisterRequest(RuntimeState state, String targetId, DeviceRegistration registration, String domain, String password) throws Exception {
        return createRegisterRequest(state, targetId, registration, domain, password, null);
    }

    private Request createRegisterRequest(RuntimeState state,
                                          String targetId,
                                          DeviceRegistration registration,
                                          String domain,
                                          String password,
                                          WWWAuthenticateHeader authenticateHeader) throws Exception {
        SipURI requestUri = state.addressFactory.createSipURI(targetId, registration.host());
        requestUri.setPort(registration.port());
        CallIdHeader callIdHeader = state.provider.getNewCallId();
        FromHeader fromHeader = createFromHeader(state, state.gateway.getSipId(), domain);
        ToHeader toHeader = createToHeader(state, targetId, domain);
        ArrayList<ViaHeader> viaHeaders = buildViaHeaders(state);
        MaxForwardsHeader maxForwardsHeader = state.headerFactory.createMaxForwardsHeader(70);
        CSeqHeader cSeqHeader = state.headerFactory.createCSeqHeader(1L, Request.REGISTER);
        Request request = state.messageFactory.createRequest(requestUri, Request.REGISTER, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwardsHeader);
        request.addHeader(createContactHeader(state));
        request.addHeader(state.headerFactory.createExpiresHeader(state.gateway.getRegisterExpiresSeconds() == null ? 3600 : state.gateway.getRegisterExpiresSeconds()));
        if (authenticateHeader != null && password != null && !password.isBlank()) {
            AuthorizationHeader authorizationHeader = state.headerFactory.createAuthorizationHeader("Digest");
            authorizationHeader.setUsername(state.gateway.getSipId());
            authorizationHeader.setRealm(authenticateHeader.getRealm());
            authorizationHeader.setNonce(authenticateHeader.getNonce());
            authorizationHeader.setURI(requestUri);
            authorizationHeader.setAlgorithm(authenticateHeader.getAlgorithm() == null ? "MD5" : authenticateHeader.getAlgorithm());
            authorizationHeader.setCNonce(randomTag());
            authorizationHeader.setNonceCount(1);
            authorizationHeader.setQop(authenticateHeader.getQop());
            authorizationHeader.setResponse(Gb28181DigestUtils.buildResponse(
                    state.gateway.getSipId(),
                    authenticateHeader.getRealm(),
                    password,
                    Request.REGISTER,
                    requestUri.toString(),
                    authenticateHeader.getNonce(),
                    authenticateHeader.getQop(),
                    "00000001",
                    authorizationHeader.getCNonce()
            ));
            request.addHeader(authorizationHeader);
        }
        return request;
    }

    private ArrayList<ViaHeader> buildViaHeaders(RuntimeState state) throws Exception {
        ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
        viaHeaders.add(state.headerFactory.createViaHeader(
                state.listeningPoint.getIPAddress(),
                state.listeningPoint.getPort(),
                state.listeningPoint.getTransport(),
                Utils.getInstance().generateBranchId()
        ));
        return viaHeaders;
    }

    private ContactHeader createContactHeader(RuntimeState state) throws Exception {
        SipURI contactUri = state.addressFactory.createSipURI(state.gateway.getSipId(), state.listeningPoint.getIPAddress());
        contactUri.setPort(state.listeningPoint.getPort());
        contactUri.setTransportParam(state.listeningPoint.getTransport());
        Address address = state.addressFactory.createAddress(contactUri);
        return state.headerFactory.createContactHeader(address);
    }

    private FromHeader createFromHeader(RuntimeState state, String user, String host) throws Exception {
        Address address = state.addressFactory.createAddress(state.addressFactory.createSipURI(user, host));
        return state.headerFactory.createFromHeader(address, randomTag());
    }

    private ToHeader createToHeader(RuntimeState state, String user, String host) throws Exception {
        Address address = state.addressFactory.createAddress(state.addressFactory.createSipURI(user, host));
        return state.headerFactory.createToHeader(address, null);
    }

    private RuntimeState requireRuntime(String gatewayId) {
        RuntimeState state = gatewayId == null ? null : runtimes.get(gatewayId);
        if (state == null) {
            throw new IllegalStateException("GB28181 gateway is not running");
        }
        return state;
    }

    private MediaDevice requireDevice(String deviceId) {
        MediaDevice device = mediaDeviceService.getById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        return device;
    }

    private MediaServer resolveServer(String... ids) {
        if (ids == null) {
            return null;
        }
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                MediaServer server = mediaServerService.getById(id);
                if (server != null) {
                    return server;
                }
            }
        }
        return null;
    }

    private MediaDevice findDeviceByCode(String deviceCode) {
        return mediaDeviceService.getOne(QueryWrapper.create()
                .from(MediaDevice.class)
                .where(MediaDevice::getDeviceCode).eq(deviceCode));
    }

    private MediaStreamSession findSessionByDialog(String dialogId) {
        if (dialogId == null || dialogId.isBlank()) {
            return null;
        }
        return mediaStreamSessionService.getOne(QueryWrapper.create()
                .from(MediaStreamSession.class)
                .where(MediaStreamSession::getDialogId).eq(dialogId)
                .and(MediaStreamSession::getStatus).ne(MediaSessionStatus.CLOSED));
    }

    private DeviceRegistration resolveRegistration(RuntimeState state, MediaDevice device) {
        DeviceRegistration registration = state.registrations.get(device.getDeviceCode());
        if (registration != null) {
            return registration;
        }
        if (device.getIp() == null || device.getIp().isBlank()) {
            throw new IllegalStateException("Device does not have a reachable register address");
        }
        return new DeviceRegistration(device.getIp(), device.getPort() == null ? 5060 : device.getPort(), normalizeTransport(state.gateway.getTransport()));
    }

    private DeviceRegistration buildRegistrationFromRequest(Request request) {
        ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
        ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
        String host = viaHeader.getReceived() != null ? viaHeader.getReceived() : viaHeader.getHost();
        int port = resolvePort(contactHeader, viaHeader);
        String transport = viaHeader.getTransport() == null ? "UDP" : viaHeader.getTransport().toUpperCase();
        return new DeviceRegistration(host, port, transport);
    }

    private int resolveExpires(Request request, ContactHeader contactHeader) {
        if (contactHeader != null && contactHeader.getExpires() >= 0) {
            return contactHeader.getExpires();
        }
        ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
        return expiresHeader == null ? 3600 : expiresHeader.getExpires();
    }

    private int resolvePort(ContactHeader contactHeader, ViaHeader viaHeader) {
        if (contactHeader != null && contactHeader.getAddress().getURI() instanceof SipURI uri && uri.getPort() > 0) {
            return uri.getPort();
        }
        return viaHeader.getPort() > 0 ? viaHeader.getPort() : 5060;
    }

    private String extractUser(Address address) {
        if (address == null || !(address.getURI() instanceof SipURI sipURI)) {
            return null;
        }
        return sipURI.getUser();
    }

    private String buildLiveSdp(RuntimeState state, MediaStreamSession session) {
        String rtpIp = state.gateway.getRtpIp() == null || state.gateway.getRtpIp().isBlank()
                ? state.listeningPoint.getIPAddress()
                : state.gateway.getRtpIp();
        return """
                v=0
                o=%s 0 0 IN IP4 %s
                s=Play
                c=IN IP4 %s
                t=0 0
                m=video %d RTP/AVP 96 98 97
                a=recvonly
                a=rtpmap:96 PS/90000
                y=%s
                f=
                """.formatted(
                state.gateway.getSipId(),
                rtpIp,
                rtpIp,
                session.getRtpPort() == null ? 0 : session.getRtpPort(),
                session.getSsrc()
        );
    }

    private String buildPlaybackSdp(RuntimeState state, MediaChannel channel, MediaStreamSession session, PlaybackStartRequest request) {
        String rtpIp = state.gateway.getRtpIp() == null || state.gateway.getRtpIp().isBlank()
                ? state.listeningPoint.getIPAddress()
                : state.gateway.getRtpIp();
        return """
                v=0
                o=%s 0 0 IN IP4 %s
                s=Playback
                u=%s:0
                c=IN IP4 %s
                t=%s %s
                m=video %d RTP/AVP 96 98 97
                a=recvonly
                a=rtpmap:96 PS/90000
                a=downloadspeed:1
                y=%s
                f=
                """.formatted(
                state.gateway.getSipId(),
                rtpIp,
                channel.getChannelCode(),
                rtpIp,
                request.startTime().format(GB_TIME_FORMATTER),
                request.endTime().format(GB_TIME_FORMATTER),
                session.getRtpPort() == null ? 0 : session.getRtpPort(),
                session.getSsrc()
        );
    }

    private String buildPlaybackQueryXml(String channelId, PlaybackQueryRequest request, int sn) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Query>
                  <CmdType>RecordInfo</CmdType>
                  <SN>%d</SN>
                  <DeviceID>%s</DeviceID>
                  <StartTime>%s</StartTime>
                  <EndTime>%s</EndTime>
                  <Secrecy>0</Secrecy>
                  <Type>all</Type>
                </Query>
                """.formatted(
                sn,
                channelId,
                request.startTime().format(GB_TIME_FORMATTER),
                request.endTime().format(GB_TIME_FORMATTER)
        );
    }

    private String buildPtzXml(String channelId, PtzControlRequest request) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Control>
                  <CmdType>DeviceControl</CmdType>
                  <SN>1</SN>
                  <DeviceID>%s</DeviceID>
                  <PTZCmd>%s</PTZCmd>
                  <Info>
                    <ControlPriority>5</ControlPriority>
                  </Info>
                </Control>
                """.formatted(channelId, Gb28181PtzUtils.buildCommand(request.command(), request.speed()));
    }

    private MediaStreamSession baseSession(MediaChannel channel, String sessionType) {
        String streamId = sanitizeStreamId(channel.getChannelCode()) + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return MediaStreamSession.builder()
                .gatewayId(channel.getGatewayId())
                .deviceId(channel.getDeviceId())
                .channelId(channel.getId())
                .sessionType(sessionType)
                .streamApp(channel.getStreamApp() == null || channel.getStreamApp().isBlank() ? "rtp" : channel.getStreamApp())
                .streamId(streamId)
                .status(MediaSessionStatus.PENDING)
                .startedTime(LocalDateTime.now())
                .ssrc(UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .build();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return MediaOnlineStatus.UNKNOWN;
        }
        return "ON".equalsIgnoreCase(status) ? MediaOnlineStatus.ONLINE : status.toUpperCase();
    }

    private String sanitizeStreamId(String value) {
        return (value == null || value.isBlank() ? UUID.randomUUID().toString() : value).replaceAll("[^0-9A-Za-z_-]", "_");
    }

    private String playbackKey(String deviceId, Integer sn) {
        return (deviceId == null ? "" : deviceId) + ":" + (sn == null ? -1 : sn);
    }

    private String normalizeHost(String host) {
        return host == null || host.isBlank() ? InetAddress.getLoopbackAddress().getHostAddress() : host;
    }

    private String normalizeTransport(String transport) {
        return transport == null || transport.isBlank() ? ListeningPoint.UDP : transport.toUpperCase();
    }

    private boolean requiresRegisterAuth(MediaGateway gateway) {
        return gateway.getSipPassword() != null && !gateway.getSipPassword().isBlank();
    }

    private String randomTag() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private final class GatewaySipListener implements SipListener {

        private final RuntimeState state;

        private GatewaySipListener(RuntimeState state) {
            this.state = state;
        }

        @Override
        public void processRequest(RequestEvent requestEvent) {
            try {
                String method = requestEvent.getRequest().getMethod();
                switch (method) {
                    case Request.REGISTER -> handleRegister(state, requestEvent);
                    case Request.MESSAGE -> handleMessage(state, requestEvent);
                    case Request.BYE -> handleBye(state, requestEvent);
                    default -> sendOk(state, requestEvent);
                }
            } catch (Exception e) {
                log.warn("Failed to process SIP request on gateway {}", state.gateway.getGatewayCode(), e);
            }
        }

        @Override
        public void processResponse(ResponseEvent responseEvent) {
            handleResponse(state, responseEvent);
        }

        @Override
        public void processTimeout(TimeoutEvent timeoutEvent) {
            log.warn("SIP timeout on gateway {}", state.gateway.getGatewayCode());
        }

        @Override
        public void processIOException(IOExceptionEvent exceptionEvent) {
            log.warn("SIP IO exception on gateway {}", state.gateway.getGatewayCode(), exceptionEvent);
        }

        @Override
        public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        }

        @Override
        public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        }
    }

    private static final class RuntimeState {
        private final MediaGateway gateway;
        private final SipStack stack;
        private final SipProvider provider;
        private final ListeningPoint listeningPoint;
        private final AddressFactory addressFactory;
        private final HeaderFactory headerFactory;
        private final MessageFactory messageFactory;
        private final AtomicInteger sn = new AtomicInteger(1);
        private final Map<String, DeviceRegistration> registrations = new ConcurrentHashMap<>();
        private final Map<String, Dialog> dialogs = new ConcurrentHashMap<>();
        private final Map<String, CompletableFuture<List<PlaybackRecordItem>>> recordQueries = new ConcurrentHashMap<>();
        private final Map<String, CatalogCursor> catalogQueries = new ConcurrentHashMap<>();
        private final Map<String, Gb28181DigestUtils.NonceToken> nonces = new ConcurrentHashMap<>();
        private final Map<String, PendingRegister> pendingRegisters = new ConcurrentHashMap<>();
        private GatewaySipListener listener;

        private RuntimeState(MediaGateway gateway, SipStack stack, SipProvider provider, ListeningPoint listeningPoint, AddressFactory addressFactory, HeaderFactory headerFactory, MessageFactory messageFactory) {
            this.gateway = gateway;
            this.stack = stack;
            this.provider = provider;
            this.listeningPoint = listeningPoint;
            this.addressFactory = addressFactory;
            this.headerFactory = headerFactory;
            this.messageFactory = messageFactory;
        }
    }

    private record DeviceRegistration(String host, int port, String transport) {
    }

    private record PendingRegister(
            String targetId,
            DeviceRegistration registration,
            String domain,
            String password,
            String platformId,
            boolean authenticated
    ) {
    }

    private static final class CatalogCursor {
        private final Map<String, Boolean> itemKeys = new LinkedHashMap<>();
        private int requestStart = 1;
        private int limit;
        private int total = 0;

        private CatalogCursor(int limit) {
            this.limit = Math.max(1, limit);
        }

        public Map<String, Boolean> itemKeys() {
            return itemKeys;
        }

        public int requestStart() {
            return requestStart;
        }

        public void setRequestStart(int requestStart) {
            this.requestStart = Math.max(1, requestStart);
        }

        public int limit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = Math.max(1, limit);
        }

        public int total() {
            return total;
        }

        public void setTotal(int total) {
            this.total = Math.max(0, total);
        }
    }
}
