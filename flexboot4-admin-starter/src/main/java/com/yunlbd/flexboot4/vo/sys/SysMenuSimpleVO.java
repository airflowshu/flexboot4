package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;

@Data
public class SysMenuSimpleVO {
    private String id;
    private String parentId;
    private String title;
    private String name;
    private String path;
    private String type;
    private String icon;
    private String authCode;
    private Integer orderNo;
}
