package com.yunlbd.flexboot4.vo.ops;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysVersionLogListVO extends BaseAuditVO {
    private String versionNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime releaseDate;
    private String type;
    private String typeStr;
    private String title;
    private String description;
    private Integer status;
    private List<String> features;
    private List<String> fixes;
}
