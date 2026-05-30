package com.yunlbd.flexboot4.service.sys;

import com.yunlbd.flexboot4.common.annotation.BumpTableVersion;
import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileManagerService {

    FileObject upload(MultipartFile file,
                      String tenantId,
                      String bizType,
                      String bizId);

    FileObject upload(MultipartFile file,
                      String tenantId,
                      String bizType,
                      String bizId,
                      boolean forceNew);

    FileAccessDescriptor access(String fileId, long ttlSeconds, boolean attachment);

    InputStream load(String fileId);

    @BumpTableVersion(tables = "sys_file")
    boolean delete(String fileId);
}
