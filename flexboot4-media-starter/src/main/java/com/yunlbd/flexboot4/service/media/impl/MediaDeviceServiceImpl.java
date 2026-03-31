package com.yunlbd.flexboot4.service.media.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaChannel;
import com.yunlbd.flexboot4.entity.media.MediaDevice;
import com.yunlbd.flexboot4.mapper.MediaChannelMapper;
import com.yunlbd.flexboot4.mapper.MediaDeviceMapper;
import com.yunlbd.flexboot4.media.dto.MediaDeviceDetail;
import com.yunlbd.flexboot4.media.enums.MediaAccessType;
import com.yunlbd.flexboot4.media.enums.MediaOnlineStatus;
import com.yunlbd.flexboot4.service.media.MediaDeviceService;
import com.yunlbd.flexboot4.service.media.MediaStreamSessionService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "mediaDevice")
public class MediaDeviceServiceImpl extends BaseServiceImpl<MediaDeviceMapper, MediaDevice> implements MediaDeviceService {

    private final MediaChannelMapper mediaChannelMapper;
    private final MediaStreamSessionService mediaStreamSessionService;

    @Override
    public MediaDeviceDetail getDetail(String deviceId) {
        MediaDevice device = super.getById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        return new MediaDeviceDetail(
                device,
                mediaChannelMapper.selectListByQuery(QueryWrapper.create()
                        .from(MediaChannel.class)
                        .where(MediaChannel::getDeviceId).eq(deviceId)
                        .orderBy(MediaChannel::getCreateTime, true)),
                mediaStreamSessionService.listByDeviceId(deviceId)
        );
    }

    @Override
    public MediaDevice upsertGbDevice(String gatewayId, String deviceCode, String deviceName) {
        MediaDevice existing = super.getOne(QueryWrapper.create()
                .from(MediaDevice.class)
                .where(MediaDevice::getDeviceCode).eq(deviceCode));
        if (existing != null) {
            existing.setGatewayId(gatewayId);
            existing.setDeviceName(deviceName);
            existing.setAccessType(MediaAccessType.GB28181);
            updateById(existing, true);
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
        save(device);
        return device;
    }

    @Override
    public void markOnline(String deviceCode, String gatewayId, LocalDateTime registerTime) {
        MediaDevice device = super.getOne(QueryWrapper.create()
                .from(MediaDevice.class)
                .where(MediaDevice::getDeviceCode).eq(deviceCode));
        if (device == null) {
            device = upsertGbDevice(gatewayId, deviceCode, deviceCode);
        }
        device.setGatewayId(gatewayId);
        device.setOnlineStatus(MediaOnlineStatus.ONLINE);
        device.setRegisterStatus("REGISTERED");
        device.setLastRegisterTime(registerTime);
        updateById(device, true);
    }

    @Override
    public void markKeepalive(String deviceCode, LocalDateTime keepaliveTime) {
        MediaDevice device = super.getOne(QueryWrapper.create()
                .from(MediaDevice.class)
                .where(MediaDevice::getDeviceCode).eq(deviceCode));
        if (device == null) {
            return;
        }
        device.setOnlineStatus(MediaOnlineStatus.ONLINE);
        device.setLastKeepaliveTime(keepaliveTime);
        updateById(device, true);
    }
}
