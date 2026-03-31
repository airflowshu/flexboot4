package com.yunlbd.flexboot4.media.dto;

import com.yunlbd.flexboot4.entity.media.MediaScreen;
import com.yunlbd.flexboot4.entity.media.MediaScreenSlot;

import java.util.List;

public record MediaScreenDetail(
        MediaScreen screen,
        List<MediaScreenSlot> slots
) {
}
