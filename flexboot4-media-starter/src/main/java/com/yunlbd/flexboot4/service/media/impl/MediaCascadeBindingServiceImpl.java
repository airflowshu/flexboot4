package com.yunlbd.flexboot4.service.media.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaCascadeBinding;
import com.yunlbd.flexboot4.mapper.MediaCascadeBindingMapper;
import com.yunlbd.flexboot4.service.media.MediaCascadeBindingService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "mediaCascadeBinding")
public class MediaCascadeBindingServiceImpl extends BaseServiceImpl<MediaCascadeBindingMapper, MediaCascadeBinding> implements MediaCascadeBindingService {

    @Override
    public List<MediaCascadeBinding> listByPlatformId(String platformId) {
        return cacheProxy().list(QueryWrapper.create()
                .from(MediaCascadeBinding.class)
                .where(MediaCascadeBinding::getPlatformId).eq(platformId));
    }

    @Override
    public boolean deleteByPlatformId(String platformId) {
        return cacheProxy().remove(QueryWrapper.create()
                .from(MediaCascadeBinding.class)
                .where(MediaCascadeBinding::getPlatformId).eq(platformId));
    }
}
