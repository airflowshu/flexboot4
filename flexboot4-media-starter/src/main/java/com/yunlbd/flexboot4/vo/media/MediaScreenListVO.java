package com.yunlbd.flexboot4.vo.media;

import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediaScreenListVO extends BaseAuditVO {
    private String screenName;
    private String layoutType;
    private String layoutJson;
    private Boolean enabled;
    private Boolean isDefault;
}
