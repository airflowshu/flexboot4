package com.yunlbd.flexboot4.vo.sys;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseAuditVO {
    private String id;
    private Long version;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastModifyTime;
    private String createBy;
    private String lastModifyBy;
}
