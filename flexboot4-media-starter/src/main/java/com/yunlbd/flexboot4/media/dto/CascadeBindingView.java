package com.yunlbd.flexboot4.media.dto;

import com.yunlbd.flexboot4.entity.media.MediaCascadeBinding;
import com.yunlbd.flexboot4.entity.media.MediaChannel;

public record CascadeBindingView(
        MediaCascadeBinding binding,
        MediaChannel channel
) {
}
