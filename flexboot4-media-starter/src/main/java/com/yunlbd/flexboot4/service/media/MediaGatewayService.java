package com.yunlbd.flexboot4.service.media;

import com.yunlbd.flexboot4.entity.media.MediaGateway;
import com.yunlbd.flexboot4.media.dto.GatewayReloadRequest;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

public interface MediaGatewayService extends IExtendedService<MediaGateway> {

    boolean reloadGateway(GatewayReloadRequest request);

    boolean startGateway(String gatewayId);

    boolean stopGateway(String gatewayId);

    MediaGateway getActiveGateway();
}
