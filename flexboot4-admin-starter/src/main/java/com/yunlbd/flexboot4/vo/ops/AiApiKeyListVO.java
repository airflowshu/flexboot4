package com.yunlbd.flexboot4.vo.ops;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import com.yunlbd.flexboot4.vo.sys.SysUserSimpleVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiApiKeyListVO extends BaseAuditVO {
    private String keyName;
    private String apiKey;
    private String userId;
    private SysUserSimpleVO user;
    private Integer status;
    private String statusStr;
    private Long quote;
    private Long used;
    private String modelScope;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsedTime;
    private String notes;
}
