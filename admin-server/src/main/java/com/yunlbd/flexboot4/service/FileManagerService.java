package com.yunlbd.flexboot4.service;

import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileObject;
import org.springframework.web.multipart.MultipartFile;

public interface FileManagerService {

    FileObject upload(MultipartFile file,
                      String tenantId,
                      String projectId,
                      String bizType,
                      String bizId);

    FileAccessDescriptor access(String fileId, long ttlSeconds, boolean attachment);

    void delete(String fileId);
}

