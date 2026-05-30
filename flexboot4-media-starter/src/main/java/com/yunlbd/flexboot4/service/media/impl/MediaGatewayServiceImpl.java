package com.yunlbd.flexboot4.service.media.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaGateway;
import com.yunlbd.flexboot4.mapper.MediaGatewayMapper;
import com.yunlbd.flexboot4.media.dto.GatewayReloadRequest;
import com.yunlbd.flexboot4.media.enums.MediaGatewayRuntimeStatus;
import com.yunlbd.flexboot4.service.media.MediaGatewayRuntimeManager;
import com.yunlbd.flexboot4.service.media.MediaGatewayService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@CacheConfig(cacheNames = "mediaGateway")
public class MediaGatewayServiceImpl extends BaseServiceImpl<MediaGatewayMapper, MediaGateway> implements MediaGatewayService {

    private final MediaGatewayRuntimeManager mediaGatewayRuntimeManager;

    public MediaGatewayServiceImpl(@Lazy MediaGatewayRuntimeManager mediaGatewayRuntimeManager) {
        this.mediaGatewayRuntimeManager = mediaGatewayRuntimeManager;
    }

    @Override
    public boolean reloadGateway(GatewayReloadRequest request) {
        MediaGateway gateway = cacheProxy().getById(request.gatewayId());
        if (gateway == null) {
            throw new IllegalArgumentException("Gateway not found");
        }
        boolean autoStart = request.autoStart() == null || request.autoStart();
        boolean ok = mediaGatewayRuntimeManager.reload(gateway, autoStart);
        gateway.setRuntimeStatus(ok && autoStart ? MediaGatewayRuntimeStatus.RUNNING : MediaGatewayRuntimeStatus.STOPPED);
        gateway.setLastError(ok ? null : "Gateway reload failed");
        if (ok && autoStart) {
            gateway.setLastStartTime(LocalDateTime.now());
        }
        cacheProxy().updateById(gateway, true);
        return ok;
    }

    @Override
    public boolean startGateway(String gatewayId) {
        MediaGateway gateway = cacheProxy().getById(gatewayId);
        if (gateway == null) {
            throw new IllegalArgumentException("Gateway not found");
        }
        boolean ok = mediaGatewayRuntimeManager.start(gateway);
        gateway.setRuntimeStatus(ok ? MediaGatewayRuntimeStatus.RUNNING : MediaGatewayRuntimeStatus.ERROR);
        gateway.setLastStartTime(LocalDateTime.now());
        gateway.setLastError(ok ? null : "Gateway start failed");
        cacheProxy().updateById(gateway, true);
        return ok;
    }

    @Override
    public boolean stopGateway(String gatewayId) {
        MediaGateway gateway = cacheProxy().getById(gatewayId);
        if (gateway == null) {
            return true;
        }
        boolean ok = mediaGatewayRuntimeManager.stop(gatewayId);
        gateway.setRuntimeStatus(MediaGatewayRuntimeStatus.STOPPED);
        gateway.setLastStopTime(LocalDateTime.now());
        gateway.setLastError(ok ? null : "Gateway stop failed");
        cacheProxy().updateById(gateway, true);
        return ok;
    }

    @Override
    public MediaGateway getActiveGateway() {
        MediaGateway gateway = cacheProxy().getOne(QueryWrapper.create()
                .from(MediaGateway.class)
                .where(MediaGateway::getActive).eq(true)
                .and(MediaGateway::getEnabled).eq(true)
                .orderBy(MediaGateway::getCreateTime, false));
        if (gateway != null) {
            return gateway;
        }
        return cacheProxy().getOne(QueryWrapper.create()
                .from(MediaGateway.class)
                .where(MediaGateway::getEnabled).eq(true)
                .orderBy(MediaGateway::getCreateTime, false));
    }
}
