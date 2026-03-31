package com.yunlbd.flexboot4.service.media;

import com.yunlbd.flexboot4.entity.media.MediaDevice;
import com.yunlbd.flexboot4.media.dto.MediaDeviceDetail;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.time.LocalDateTime;

public interface MediaDeviceService extends IExtendedService<MediaDevice> {

    MediaDeviceDetail getDetail(String deviceId);

    MediaDevice upsertGbDevice(String gatewayId, String deviceCode, String deviceName);

    void markOnline(String deviceCode, String gatewayId, LocalDateTime registerTime);

    void markKeepalive(String deviceCode, LocalDateTime keepaliveTime);
}
