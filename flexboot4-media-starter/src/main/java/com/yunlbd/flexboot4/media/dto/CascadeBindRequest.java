package com.yunlbd.flexboot4.media.dto;

import java.util.List;

public record CascadeBindRequest(
        String platformId,
        List<CascadeChannelBindRequest> bindings
) {
    public record CascadeChannelBindRequest(
            String id,
            String channelId,
            String gbChannelCode,
            Boolean enabled,
            Boolean liveEnabled,
            Boolean playbackEnabled
    ) {
    }
}
