package com.yunlbd.flexboot4.dto.ops;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysVersionLogUpdateReq {
    private String versionNo;
    private LocalDateTime releaseDate;
    private String type;
    private String title;
    private String description;
    private Integer status;
    private List<String> features;
    private List<String> fixes;
    private String remark;
}
