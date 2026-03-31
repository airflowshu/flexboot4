package com.yunlbd.flexboot4.service.media.impl;

import com.yunlbd.flexboot4.entity.media.MediaCascadeBinding;
import com.yunlbd.flexboot4.entity.media.MediaCascadePlatform;
import com.yunlbd.flexboot4.entity.media.MediaChannel;
import com.yunlbd.flexboot4.mapper.MediaCascadePlatformMapper;
import com.yunlbd.flexboot4.media.dto.CascadeBindRequest;
import com.yunlbd.flexboot4.media.dto.CascadeBindingView;
import com.yunlbd.flexboot4.media.enums.MediaOnlineStatus;
import com.yunlbd.flexboot4.service.media.MediaCascadeBindingService;
import com.yunlbd.flexboot4.service.media.MediaCascadePlatformService;
import com.yunlbd.flexboot4.service.media.MediaChannelService;
import com.yunlbd.flexboot4.service.media.MediaGatewayRuntimeManager;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "mediaCascadePlatform")
public class MediaCascadePlatformServiceImpl extends BaseServiceImpl<MediaCascadePlatformMapper, MediaCascadePlatform> implements MediaCascadePlatformService {

    private final MediaChannelService mediaChannelService;
    private final MediaCascadeBindingService mediaCascadeBindingService;
    private final MediaGatewayRuntimeManager mediaGatewayRuntimeManager;

    @Override
    public List<CascadeBindingView> listBindings(String platformId) {
        List<CascadeBindingView> result = new ArrayList<>();
        for (MediaCascadeBinding binding : mediaCascadeBindingService.listByPlatformId(platformId)) {
            MediaChannel channel = mediaChannelService.getById(binding.getChannelId());
            result.add(new CascadeBindingView(binding, channel));
        }
        return result;
    }

    @Override
    public List<CascadeBindingView> bindChannels(CascadeBindRequest request) {
        MediaCascadePlatform platform = super.getById(request.platformId());
        if (platform == null) {
            throw new IllegalArgumentException("Cascade platform not found");
        }
        mediaCascadeBindingService.deleteByPlatformId(platform.getId());
        List<MediaCascadeBinding> bindings = new ArrayList<>();
        if (request.bindings() != null) {
            for (CascadeBindRequest.CascadeChannelBindRequest item : request.bindings()) {
                bindings.add(MediaCascadeBinding.builder()
                        .platformId(platform.getId())
                        .channelId(item.channelId())
                        .gbChannelCode(item.gbChannelCode())
                        .enabled(item.enabled() == null || item.enabled())
                        .liveEnabled(item.liveEnabled() == null || item.liveEnabled())
                        .playbackEnabled(item.playbackEnabled() == null || item.playbackEnabled())
                        .build());
            }
        }
        if (!bindings.isEmpty()) {
            mediaCascadeBindingService.saveBatch(bindings);
        }
        return listBindings(platform.getId());
    }

    @Override
    public boolean registerPlatform(String platformId) {
        MediaCascadePlatform platform = super.getById(platformId);
        if (platform == null) {
            throw new IllegalArgumentException("Cascade platform not found");
        }
        boolean ok = mediaGatewayRuntimeManager.registerCascade(platform);
        if (ok) {
            platform.setOnlineStatus(MediaOnlineStatus.UNKNOWN);
            platform.setLastError(null);
        } else {
            platform.setOnlineStatus(MediaOnlineStatus.OFFLINE);
            platform.setLastError("Cascade register request send failed");
            platform.setLastRegisterTime(LocalDateTime.now());
        }
        updateById(platform, true);
        return ok;
    }

    @Override
    public boolean stopPlatform(String platformId) {
        MediaCascadePlatform platform = super.getById(platformId);
        if (platform == null) {
            return true;
        }
        boolean ok = mediaGatewayRuntimeManager.stopCascade(platform);
        platform.setOnlineStatus(MediaOnlineStatus.OFFLINE);
        platform.setLastKeepaliveTime(LocalDateTime.now());
        platform.setLastError(ok ? null : "Cascade stop failed");
        updateById(platform, true);
        return ok;
    }
}
