package com.yunlbd.flexboot4.service.media.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaDevice;
import com.yunlbd.flexboot4.mapper.MediaDeviceMapper;
import com.yunlbd.flexboot4.media.dto.MediaDeviceDetail;
import com.yunlbd.flexboot4.media.enums.MediaAccessType;
import com.yunlbd.flexboot4.media.enums.MediaOnlineStatus;
import com.yunlbd.flexboot4.service.media.MediaChannelService;
import com.yunlbd.flexboot4.service.media.MediaDeviceService;
import com.yunlbd.flexboot4.service.media.MediaStreamSessionService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@CacheConfig(cacheNames = "mediaDevice")
public class MediaDeviceServiceImpl extends BaseServiceImpl<MediaDeviceMapper, MediaDevice> implements MediaDeviceService {

    private final MediaChannelService mediaChannelService;
    private final MediaStreamSessionService mediaStreamSessionService;

    public MediaDeviceServiceImpl(@Lazy MediaChannelService mediaChannelService,
                                  MediaStreamSessionService mediaStreamSessionService) {
        this.mediaChannelService = mediaChannelService;
        this.mediaStreamSessionService = mediaStreamSessionService;
    }

    @Override
    public MediaDeviceDetail getDetail(String deviceId) {
        MediaDevice device = cacheProxy().getById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        return new MediaDeviceDetail(
                device,
                mediaChannelService.listByDeviceId(deviceId),
                mediaStreamSessionService.listByDeviceId(deviceId)
        );
    }

    @Override
    public MediaDevice upsertGbDevice(String gatewayId, String deviceCode, String deviceName) {
        MediaDevice existing = cacheProxy().getOne(QueryWrapper.create()
                .from(MediaDevice.class)
                .where(MediaDevice::getDeviceCode).eq(deviceCode));
        if (existing != null) {
            existing.setGatewayId(gatewayId);
            existing.setDeviceName(deviceName);
            existing.setAccessType(MediaAccessType.GB28181);
            cacheProxy().updateById(existing, true);
            return existing;
        }
        MediaDevice device = MediaDevice.builder()
                .gatewayId(gatewayId)
                .deviceCode(deviceCode)
                .deviceName(deviceName == null || deviceName.isBlank() ? deviceCode : deviceName)
                .accessType(MediaAccessType.GB28181)
                .onlineStatus(MediaOnlineStatus.UNKNOWN)
                .registerStatus("UNKNOWN")
                .build();
        cacheProxy().save(device);
        return device;
    }

    @Override
    public void markOnline(String deviceCode, String gatewayId, LocalDateTime registerTime) {
        MediaDevice device = cacheProxy().getOne(QueryWrapper.create()
                .from(MediaDevice.class)
                .where(MediaDevice::getDeviceCode).eq(deviceCode));
        if (device == null) {
            device = upsertGbDevice(gatewayId, deviceCode, deviceCode);
        }
        device.setGatewayId(gatewayId);
        device.setOnlineStatus(MediaOnlineStatus.ONLINE);
        device.setRegisterStatus("REGISTERED");
        device.setLastRegisterTime(registerTime);
        cacheProxy().updateById(device, true);
    }

    @Override
    public void markKeepalive(String deviceCode, LocalDateTime keepaliveTime) {
        MediaDevice device = cacheProxy().getOne(QueryWrapper.create()
                .from(MediaDevice.class)
                .where(MediaDevice::getDeviceCode).eq(deviceCode));
        if (device == null) {
            return;
        }
        device.setOnlineStatus(MediaOnlineStatus.ONLINE);
        device.setLastKeepaliveTime(keepaliveTime);
        cacheProxy().updateById(device, true);
    }
}
