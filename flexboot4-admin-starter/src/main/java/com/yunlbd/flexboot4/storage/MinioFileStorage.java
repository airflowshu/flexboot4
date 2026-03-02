package com.yunlbd.flexboot4.storage;

import com.yunlbd.flexboot4.config.MinioProperties;
import com.yunlbd.flexboot4.file.*;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MinioFileStorage implements FileStorage {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioFileStorage(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public FileObject store(InputStream data, long size, String fileName, String contentType, FileObject meta) {
        String ext = null;
        int dot = fileName != null ? fileName.lastIndexOf('.') : -1;
        if (dot >= 0 && dot + 1 < fileName.length()) {
            ext = fileName.substring(dot + 1);
        }
        String objectKey = buildObjectKey(meta, ext);
        try {
            // 构建 MinIO User Metadata
            Map<String, String> userMetadata = buildUserMetadata(meta);

            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(resolveBucket(meta))
                    .object(objectKey)
                    .stream(data, size, -1);
            if (contentType != null && !contentType.isBlank()) {
                builder.contentType(contentType);
            }
            if (!userMetadata.isEmpty()) {
                builder.extraHeaders(userMetadata);
            }
            minioClient.putObject(builder.build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        boolean isPublic = isPublic(meta);
        String bucket = resolveBucket(meta);
        String endpoint = isPublic && properties.publicEndpoint() != null && !properties.publicEndpoint().isBlank()
                ? properties.publicEndpoint()
                : properties.endpoint();
        FileLocation location = new FileLocation(StorageType.MINIO, bucket, objectKey, null, endpoint);
        return new FileObject(
                meta.id(),
                meta.tenantId(),
                meta.bizType(),
                meta.bizId(),
                fileName,
                ext,
                contentType,
                size,
                meta.fileHash(),
                location,
                meta.aiStatus(),
                meta.chunkCount(),
                meta.tokenEstimate(),
                meta.embeddingModel()
        );
    }

    /**
     * 构建 MinIO User Metadata（S3 兼容协议使用 x-amz-meta- 前缀）
     */
    private Map<String, String> buildUserMetadata(FileObject meta) {
        Map<String, String> metadata = new HashMap<>();

        // 基础元数据 - 使用 x-amz-meta- 前缀（S3/MinIO 兼容）
        if (meta.tenantId() != null && !meta.tenantId().isBlank()) {
            metadata.put("x-amz-meta-tenantId", meta.tenantId());
        }
        if (meta.bizType() != null && !meta.bizType().isBlank()) {
            metadata.put("x-amz-meta-bizType", meta.bizType());
        }
        if (meta.bizId() != null && !meta.bizId().isBlank()) {
            metadata.put("x-amz-meta-bizId", meta.bizId());
        }
        // 文件ID便于查询
        metadata.put("x-amz-meta-fileId", meta.id());
        // 文件哈希值
        if (meta.fileHash() != null && !meta.fileHash().isBlank()) {
            metadata.put("x-amz-meta-fileHash", meta.fileHash());
        }
        // 文件名（原始）
        if (meta.fileName() != null && !meta.fileName().isBlank()) {
            metadata.put("x-amz-meta-fileName", meta.fileName());
        }

        return metadata;
    }

    private boolean isPublic(FileObject meta) {
        String bizType = meta.bizType();
        return "sys_user_avatar".equals(bizType);
    }

    private String resolveBucket(FileObject meta) {
        boolean isPublic = isPublic(meta);
        if (isPublic && properties.publicBucket() != null && !properties.publicBucket().isBlank()) {
            return properties.publicBucket();
        }
        return properties.bucket();
    }

    @Override
    public InputStream load(FileLocation location) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(location.bucket())
                    .object(location.objectKey())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(FileLocation location) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(location.bucket())
                    .object(location.objectKey())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileAccessDescriptor generateAccessUrl(FileLocation location, Duration ttl, boolean attachment) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .bucket(location.bucket())
                    .object(location.objectKey())
                    .method(Method.GET)
                    .expiry((int) ttl.getSeconds())
                    .build();
            String url = minioClient.getPresignedObjectUrl(args);
            Instant expireAt = Instant.now().plusSeconds(ttl.getSeconds());
            String disposition = attachment ? "attachment" : "inline";
            return new FileAccessDescriptor(url, expireAt, disposition, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildObjectKey(FileObject meta, String ext) {
        StringBuilder sb = new StringBuilder();
        if (meta.tenantId() != null && !meta.tenantId().isBlank()) {
            sb.append(meta.tenantId()).append('/');
        }
        sb.append("files/").append(UUID.randomUUID());
        if (ext != null && !ext.isBlank()) {
            sb.append('.').append(ext);
        }
        return sb.toString();
    }
}
