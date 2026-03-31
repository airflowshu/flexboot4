package com.yunlbd.flexboot4.media.dto;

import java.util.List;

public record ScreenSaveRequest(
        String id,
        String screenName,
        String layoutType,
        String layoutJson,
        Boolean enabled,
        Boolean isDefault,
        List<ScreenSlotRequest> slots
) {
    public record ScreenSlotRequest(
            String id,
            Integer slotIndex,
            String slotName,
            Integer x,
            Integer y,
            Integer width,
            Integer height,
            String channelId,
            String sessionType,
            String optionsJson
    ) {
    }
}
