package com.yunlbd.flexboot4.service.sys;

import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileObject;
import org.springframework.web.multipart.MultipartFile;

public interface FileManagerService {

    FileObject upload(MultipartFile file,
                      String tenantId,
                      String bizType,
                      String bizId);

    /**
     * 上传文件
     * @param forceNew 是否强制创建新记录（true=同一文件也创建新记录，false=复用已有记录）
     */
    FileObject upload(MultipartFile file,
                      String tenantId,
                      String bizType,
                      String bizId,
                      boolean forceNew);

    FileAccessDescriptor access(String fileId, long ttlSeconds, boolean attachment);

    void delete(String fileId);
}
