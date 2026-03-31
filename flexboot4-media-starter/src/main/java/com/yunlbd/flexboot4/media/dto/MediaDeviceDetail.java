package com.yunlbd.flexboot4.media.dto;

import com.yunlbd.flexboot4.entity.media.MediaChannel;
import com.yunlbd.flexboot4.entity.media.MediaDevice;
import com.yunlbd.flexboot4.entity.media.MediaStreamSession;

import java.util.List;

public record MediaDeviceDetail(
        MediaDevice device,
        List<MediaChannel> channels,
        List<MediaStreamSession> sessions
) {
}
