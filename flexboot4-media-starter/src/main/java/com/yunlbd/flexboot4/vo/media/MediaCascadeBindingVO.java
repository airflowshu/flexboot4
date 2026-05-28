package com.yunlbd.flexboot4.vo.media;

import lombok.Data;

@Data
public class MediaCascadeBindingVO {
    private String id;
    private String platformId;
    private String channelId;
    private String gbChannelCode;
    private Boolean enabled;
    private Boolean liveEnabled;
    private Boolean playbackEnabled;
}
