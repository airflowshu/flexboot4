package com.yunlbd.flexboot4.service.media.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaScreen;
import com.yunlbd.flexboot4.entity.media.MediaScreenSlot;
import com.yunlbd.flexboot4.mapper.MediaScreenMapper;
import com.yunlbd.flexboot4.media.dto.MediaScreenDetail;
import com.yunlbd.flexboot4.media.dto.ScreenSaveRequest;
import com.yunlbd.flexboot4.service.media.MediaScreenService;
import com.yunlbd.flexboot4.service.media.MediaScreenSlotService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "mediaScreen")
public class MediaScreenServiceImpl extends BaseServiceImpl<MediaScreenMapper, MediaScreen> implements MediaScreenService {

    private final MediaScreenSlotService mediaScreenSlotService;

    @Override
    public MediaScreenDetail getDetail(String screenId) {
        MediaScreen screen = cacheProxy().getById(screenId);
        if (screen == null) {
            throw new IllegalArgumentException("Screen not found");
        }
        return new MediaScreenDetail(screen, mediaScreenSlotService.listByScreenId(screenId));
    }

    @Override
    public MediaScreenDetail saveScreen(ScreenSaveRequest request) {
        MediaScreen screen = request.id() == null || request.id().isBlank()
                ? new MediaScreen()
                : cacheProxy().getById(request.id());
        if (screen == null) {
            throw new IllegalArgumentException("Screen not found");
        }
        screen.setScreenName(request.screenName());
        screen.setLayoutType(request.layoutType());
        screen.setLayoutJson(request.layoutJson());
        screen.setEnabled(request.enabled() == null || request.enabled());
        screen.setIsDefault(request.isDefault() != null && request.isDefault());

        if (screen.getIsDefault() != null && screen.getIsDefault()) {
            List<MediaScreen> others = cacheProxy().list(QueryWrapper.create()
                    .from(MediaScreen.class)
                    .where(MediaScreen::getIsDefault).eq(true));
            for (MediaScreen other : others) {
                if (screen.getId() != null && screen.getId().equals(other.getId())) {
                    continue;
                }
                other.setIsDefault(false);
                cacheProxy().updateById(other, true);
            }
        }

        if (screen.getId() == null || screen.getId().isBlank()) {
            cacheProxy().save(screen);
        } else {
            cacheProxy().updateById(screen, true);
        }

        mediaScreenSlotService.deleteByScreenId(screen.getId());
        List<MediaScreenSlot> slots = new ArrayList<>();
        if (request.slots() != null) {
            for (ScreenSaveRequest.ScreenSlotRequest slotRequest : request.slots()) {
                MediaScreenSlot slot = MediaScreenSlot.builder()
                        .screenId(screen.getId())
                        .slotIndex(slotRequest.slotIndex())
                        .slotName(slotRequest.slotName())
                        .x(slotRequest.x())
                        .y(slotRequest.y())
                        .width(slotRequest.width())
                        .height(slotRequest.height())
                        .channelId(slotRequest.channelId())
                        .sessionType(slotRequest.sessionType())
                        .optionsJson(slotRequest.optionsJson())
                        .build();
                slots.add(slot);
            }
        }
        if (!slots.isEmpty()) {
            mediaScreenSlotService.saveBatch(slots);
        }
        return new MediaScreenDetail(screen, mediaScreenSlotService.listByScreenId(screen.getId()));
    }
}
