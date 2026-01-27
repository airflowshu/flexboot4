package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.SysFile;
import com.yunlbd.flexboot4.file.*;
import com.yunlbd.flexboot4.service.FileManagerService;
import com.yunlbd.flexboot4.service.SysFileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class FileManagerServiceImpl implements FileManagerService {

    private final FileStorage fileStorage;
    private final SysFileService sysFileService;

    public FileManagerServiceImpl(FileStorage fileStorage, SysFileService sysFileService) {
        this.fileStorage = fileStorage;
        this.sysFileService = sysFileService;
    }

    @Override
    public FileObject upload(MultipartFile file, String tenantId, String projectId, String bizType, String bizId) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("上传文件为空");
            }
            String id = UUID.randomUUID().toString();
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                fileName = "file-" + id;
            }
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank() || "application/octet-stream".equalsIgnoreCase(contentType)) {
                String guessed = URLConnection.guessContentTypeFromName(fileName);
                if (guessed != null && !guessed.isBlank()) {
                    contentType = guessed;
                }
            }
            long declaredSize = file.getSize();
            HashResult hr;
            try (InputStream hashIn = file.getInputStream()) {
                hr = sha256AndCount(hashIn);
            }
            long size = declaredSize > 0 ? declaredSize : hr.size;
            if (size <= 0) {
                throw new IllegalArgumentException("上传文件为空");
            }
            String hash = hr.hash;

            SysFile existing = sysFileService.getOne(QueryWrapper.create()
                    .from(SysFile.class)
                    .where(SysFile::getFileHash).eq(hash)
                    .and(SysFile::getDelFlag).eq(0));
            if (existing != null) {
                return toFileObject(existing);
            }

            FileLocation location = new FileLocation(StorageType.MINIO, null, null, null, null);
            FileObject meta = new FileObject(
                    id,
                    tenantId,
                    projectId,
                    bizType,
                    bizId,
                    fileName,
                    null,
                    contentType,
                    size,
                    hash,
                    location,
                    "UPLOADED",
                    0,
                    null,
                    null
            );
            FileObject stored;
            try (InputStream uploadIn = file.getInputStream()) {
                stored = fileStorage.store(uploadIn, size, fileName, contentType, meta);
            }

            SysFile entity = new SysFile();
            entity.setId(stored.id());
            entity.setTenantId(stored.tenantId());
            entity.setProjectId(stored.projectId());
            entity.setBizType(stored.bizType());
            entity.setBizId(stored.bizId());
            entity.setFileName(stored.fileName());
            entity.setFileExt(stored.fileExt());
            entity.setMimeType(stored.mimeType());
            entity.setFileSize(stored.fileSize());
            entity.setFileHash(stored.fileHash());
            entity.setStorageType(stored.location().storageType().name());
            entity.setBucketName(stored.location().bucket());
            entity.setObjectKey(stored.location().objectKey());
            entity.setAiStatus(stored.aiStatus());
            entity.setChunkCount(stored.chunkCount());
            entity.setTokenEstimate(stored.tokenEstimate());
            entity.setEmbeddingModel(stored.embeddingModel());
            sysFileService.save(entity);

            return stored;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileAccessDescriptor access(String fileId, long ttlSeconds, boolean attachment) {
        SysFile entity = sysFileService.getById(fileId);
        if (entity == null || entity.getDelFlag() != null && entity.getDelFlag() != 0) {
            throw new IllegalArgumentException("file not found");
        }
        FileLocation location = new FileLocation(
                StorageType.valueOf(entity.getStorageType()),
                entity.getBucketName(),
                entity.getObjectKey(),
                null,
                null
        );
        return fileStorage.generateAccessUrl(location, Duration.ofSeconds(ttlSeconds), attachment);
    }

    @Override
    public void delete(String fileId) {
        SysFile entity = sysFileService.getById(fileId);
        if (entity == null) {
            return;
        }
        FileLocation location = new FileLocation(
                StorageType.valueOf(entity.getStorageType()),
                entity.getBucketName(),
                entity.getObjectKey(),
                null,
                null
        );
        fileStorage.delete(location);
        sysFileService.removeById(fileId);
    }

    private HashResult sha256AndCount(InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            long total = 0L;
            while ((n = in.read(buf)) > 0) {
                digest.update(buf, 0, n);
                total += n;
            }
            byte[] bytes = digest.digest();
            return new HashResult(HexFormat.of().formatHex(bytes), total);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private record HashResult(String hash, long size) {
    }

    private FileObject toFileObject(SysFile f) {
        FileLocation location = new FileLocation(
                StorageType.valueOf(f.getStorageType()),
                f.getBucketName(),
                f.getObjectKey(),
                null,
                null
        );
        return new FileObject(
                f.getId(),
                f.getTenantId(),
                f.getProjectId(),
                f.getBizType(),
                f.getBizId(),
                f.getFileName(),
                f.getFileExt(),
                f.getMimeType(),
                f.getFileSize() == null ? 0L : f.getFileSize(),
                f.getFileHash(),
                location,
                f.getAiStatus(),
                f.getChunkCount() == null ? 0 : f.getChunkCount(),
                f.getTokenEstimate(),
                f.getEmbeddingModel()
        );
    }
}
