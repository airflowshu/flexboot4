package com.yunlbd.flexboot4.service.media.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaScreenSlot;
import com.yunlbd.flexboot4.mapper.MediaScreenSlotMapper;
import com.yunlbd.flexboot4.service.media.MediaScreenSlotService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "mediaScreenSlot")
public class MediaScreenSlotServiceImpl extends BaseServiceImpl<MediaScreenSlotMapper, MediaScreenSlot> implements MediaScreenSlotService {

    @Override
    public List<MediaScreenSlot> listByScreenId(String screenId) {
        return cacheProxy().list(QueryWrapper.create()
                .from(MediaScreenSlot.class)
                .where(MediaScreenSlot::getScreenId).eq(screenId)
                .orderBy(MediaScreenSlot::getSlotIndex, true));
    }

    @Override
    public boolean deleteByScreenId(String screenId) {
        return cacheProxy().remove(QueryWrapper.create()
                .from(MediaScreenSlot.class)
                .where(MediaScreenSlot::getScreenId).eq(screenId));
    }
}
