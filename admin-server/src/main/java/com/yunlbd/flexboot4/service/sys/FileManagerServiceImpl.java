package com.yunlbd.flexboot4.service.sys;

import com.yunlbd.flexboot4.cache.TableVersions;
import com.yunlbd.flexboot4.config.MinioProperties;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import com.yunlbd.flexboot4.file.*;
import com.yunlbd.flexboot4.file.ai.AiParseStatus;
import com.yunlbd.flexboot4.mapper.SysFileMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@CacheConfig(cacheNames = "fileAccess")
public class FileManagerServiceImpl implements FileManagerService {

    private final FileStorage fileStorage;
    private final SysFileService sysFileService;
    private final SysFileMapper sysFileMapper;
    private final MinioProperties minioProperties;
    private final CacheManager cacheManager;

    public FileManagerServiceImpl(FileStorage fileStorage, SysFileService sysFileService, SysFileMapper sysFileMapper, MinioProperties minioProperties, CacheManager cacheManager) {
        this.fileStorage = fileStorage;
        this.sysFileService = sysFileService;
        this.sysFileMapper = sysFileMapper;
        this.minioProperties = minioProperties;
        this.cacheManager = cacheManager;
    }

    @Override
    public FileObject upload(MultipartFile file, String tenantId, String bizType, String bizId){
        return upload(file, tenantId, bizType, bizId, false);
    }

    @Override
    public FileObject upload(MultipartFile file, String tenantId, String bizType, String bizId, boolean forceNew){
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }

        // 1. 计算文件 hash
        HashResult hr;
        try (InputStream hashIn = file.getInputStream()) {
            hr = sha256AndCount(hashIn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long size = Math.max(file.getSize(), hr.size);
        if (size <= 0) {
            throw new IllegalArgumentException("上传文件为空");
        }
        String hash = hr.hash;

        // 2. 查询是否已存在
        SysFile existing = findByHash(hash);
        if (existing != null && !forceNew) {
            // 复用已有记录
            return handleExistingFile(existing, tenantId, bizType, bizId);
        }
        // forceNew=true 时，跳过复用逻辑，创建新记录

        // 3. 准备文件元数据
        String id = UUID.randomUUID().toString();
        String fileName = getFileName(file, id);
        String contentType = getContentType(file, fileName);

        FileLocation location = new FileLocation(StorageType.MINIO, null, null, null, null);
        FileObject meta = new FileObject(
                id, tenantId, bizType, bizId,
                fileName, null, contentType, size, hash,
                location, "UPLOADED", 0, null, null
        );

        // 4. 上传到存储
        FileObject stored;
        try (InputStream uploadIn = file.getInputStream()) {
            stored = fileStorage.store(uploadIn, size, fileName, contentType, meta);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 5. 保存数据库记录
        SysFile entity = buildSysFileEntity(stored);
        try {
            sysFileService.save(entity);
        } catch (DuplicateKeyException e) {
            // 并发插入时，唯一键冲突
            if (!forceNew) {
                // 非强制创建模式，尝试查找已有记录并复用
                SysFile dup = findByHash(hash);
                if (dup != null) {
                    return handleExistingFile(dup, tenantId, bizType, bizId);
                }
            }
            throw e;
        }

        return stored;
    }

    /**
     * 处理已存在的文件（去重逻辑）
     */
    private FileObject handleExistingFile(SysFile existing, String tenantId, String bizType, String bizId) {
        boolean changed = false;

        // 恢复已删除的文件（使用原生 SQL 绕过 TableLogic）
        if (existing.getDelFlag() != null && existing.getDelFlag() != 0) {
            existing.setDelFlag(0);
            existing.setLastModifyTime(LocalDateTime.now());
            changed = true;
        }
        // 更新空字段
        changed = updateIfBlank(existing::getBizType, existing::setBizType, bizType) || changed;
        changed = updateIfBlank(existing::getBizId, existing::setBizId, bizId) || changed;
        changed = updateIfBlank(existing::getTenantId, existing::setTenantId, tenantId) || changed;

        if (changed) {
            // 使用 Mapper 更新其他字段
            sysFileMapper.restoreById(existing.getId());
            // 清除缓存
            clearFileCaches(existing.getId());
        }

        return toFileObject(existing);
    }

    /**
     * 如果目标值为空，则更新为新值
     */
    private <T> boolean updateIfBlank(java.util.function.Supplier<T> getter, java.util.function.Consumer<T> setter, T newValue) {
        if (newValue == null) {
            return false;
        }
        if (newValue instanceof String s && (s.isBlank())) {
            return false;
        }
        T current = getter.get();
        if (current == null || (current instanceof String cs && cs.isBlank())) {
            setter.accept(newValue);
            return true;
        }
        return false;
    }

    /**
     * 根据 hash 查询文件（绕过 TableLogic 软删除过滤）
     */
    private SysFile findByHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return null;
        }
        // 使用自定义 Mapper 方法，绕过 TableLogic 自动添加 del_flag 条件
        return sysFileMapper.selectByHash(hash);
    }

    /**
     * 构建数据库实体
     */
    private SysFile buildSysFileEntity(FileObject stored) {
        SysFile entity = new SysFile();
        entity.setId(stored.id());
        entity.setTenantId(stored.tenantId());
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
        entity.setAiParseStatus(AiParseStatus.PENDING.name());
        entity.setChunkCount(stored.chunkCount());
        entity.setTokenEstimate(stored.tokenEstimate());
        entity.setEmbeddingModel(stored.embeddingModel());
        return entity;
    }

    /**
     * 获取文件名
     */
    private String getFileName(MultipartFile file, String id) {
        String name = file.getOriginalFilename();
        return (name == null || name.isBlank()) ? "file-" + id : name;
    }

    /**
     * 获取 Content-Type
     */
    private String getContentType(MultipartFile file, String fileName) {
        String type = file.getContentType();
        if (type == null || type.isBlank() || "application/octet-stream".equalsIgnoreCase(type)) {
            String guessed = URLConnection.guessContentTypeFromName(fileName);
            if (guessed != null && !guessed.isBlank()) {
                return guessed;
            }
        }
        return type;
    }

    @Override
    @Cacheable(key = "#fileId + ':' + #attachment", unless = "#result == null")
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
    @CacheEvict(cacheNames = "fileAccess", allEntries = true)
    public void delete(String fileId) {
        // 这里先注释掉,暂不真物理删除minIO中的文件
        // SysFile entity = sysFileService.getById(fileId);
        // if (entity == null) {
        //     return;
        // }
        // FileLocation location = new FileLocation(
        //         StorageType.valueOf(entity.getStorageType()),
        //         entity.getBucketName(),
        //         entity.getObjectKey(),
        //         null,
        //         null
        // );
        // fileStorage.delete(location);
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

    /**
     * 构建 FileObject
     */
    private FileObject toFileObject(SysFile f) {
        boolean isPublic = minioProperties.publicBucket() != null
                && minioProperties.publicBucket().equals(f.getBucketName());
        String endpoint = isPublic && minioProperties.publicEndpoint() != null && !minioProperties.publicEndpoint().isBlank()
                ? minioProperties.publicEndpoint()
                : minioProperties.endpoint();

        FileLocation location = null;
        if (f.getStorageType() != null && !f.getStorageType().isBlank()) {
            location = new FileLocation(
                    StorageType.valueOf(f.getStorageType()),
                    f.getBucketName(),
                    f.getObjectKey(),
                    null,
                    endpoint
            );
        }
        return new FileObject(
                f.getId(),
                f.getTenantId(),
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

    /**
     * 清除文件相关缓存
     */
    private void clearFileCaches(String fileId) {
        // 清除 fileAccess 缓存（访问链接缓存）
        Cache fileAccessCache = cacheManager.getCache("fileAccess");
        if (fileAccessCache != null) {
            fileAccessCache.evict(fileId + ":false");
            fileAccessCache.evict(fileId + ":true");
        }
        // 通过 bumpVersion 让 sysFile 表的版本化缓存失效（与 BaseServiceImpl 保持一致）
        TableVersions.bumpVersion("sys_file");
    }
}
