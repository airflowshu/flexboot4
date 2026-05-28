package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;

@Data
public class SysFileSimpleVO {
    private String id;
    private String fileName;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String bucketName;
    private String objectKey;
}
