package com.yunlbd.flexboot4.dto.media;

import lombok.Data;

@Data
public class MediaScreenCreateReq {
    private String screenName;
    private String layoutType;
    private String layoutJson;
    private Boolean enabled;
    private Boolean isDefault;
    private String remark;
}
