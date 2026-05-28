package com.yunlbd.flexboot4.vo.media;

import lombok.Data;

@Data
public class MediaScreenSlotVO {
    private String id;
    private String screenId;
    private Integer slotIndex;
    private String slotName;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private String channelId;
    private String sessionType;
    private String optionsJson;
}
