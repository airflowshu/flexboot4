package com.yunlbd.flexboot4.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class VueRoute implements Serializable {
    private Long id;
    private Long pid;
    private String path;
    private String name;
    private String component;
    private String redirect;
    private String type;
    private Integer status;
    private String authCode;
    private RouteMeta meta;
    private List<VueRoute> children;
}
