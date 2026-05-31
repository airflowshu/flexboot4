package com.yunlbd.flexboot4.service.sys;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.common.annotation.BumpTableVersion;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import com.yunlbd.flexboot4.entity.sys.table.SysFileTableDef;
import com.yunlbd.flexboot4.file.*;
import com.yunlbd.flexboot4.file.ai.AiParseStatus;
import com.yunlbd.flexboot4.storage.FileStorageRegistry;
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
public class FileManagerServiceImpl implements FileManagerService {

    private final FileStorageRegistry fileStorageRegistry;
    private final SysFileService sysFileService;

    public FileManagerServiceImpl(FileStorageRegistry fileStorageRegistry, SysFileService sysFileService) {
        this.fileStorageRegistry = fileStorageRegistry;
        this.sysFileService = sysFileService;
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

        FileStorage fileStorage = fileStorageRegistry.active();
        FileLocation location = new FileLocation(fileStorage.storageType(), null, null, null, null);
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
            fileStorage.delete(stored.location());
            throw new IllegalStateException("sys_file.file_hash 唯一索引未按文件存储规范调整，请确认 Admin Flyway 初始化脚本 flexboot4-admin-starter/src/main/resources/db/flexboot4-migration/admin/postgresql/V1000__admin_core_schema.sql 已执行", e);
        }

        return stored;
    }

    /**
     * 处理已存在的文件（去重逻辑）
     */
    private FileObject handleExistingFile(SysFile existing, String tenantId, String bizType, String bizId) {
        boolean changed = false;

        // 更新空字段
        changed = updateIfBlank(existing::getBizType, existing::setBizType, bizType) || changed;
        changed = updateIfBlank(existing::getBizId, existing::setBizId, bizId) || changed;
        changed = updateIfBlank(existing::getTenantId, existing::setTenantId, tenantId) || changed;

        if (changed) {
            existing.setLastModifyTime(LocalDateTime.now());
            sysFileService.updateById(existing, true);
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
        SysFileTableDef file = SysFileTableDef.SYS_FILE;
        QueryWrapper query = QueryWrapper.create()
                .select(file.ALL_COLUMNS)
                .from(file)
                .where(file.FILE_HASH.eq(hash))
                .and(file.DEL_FLAG.eq(0))
                .limit(1);
        return sysFileService.getOne(query);
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
    public FileAccessDescriptor access(String fileId, long ttlSeconds, boolean attachment) {
        SysFile entity = sysFileService.getById(fileId);
        if (entity == null || entity.getDelFlag() != null && entity.getDelFlag() != 0) {
            throw new IllegalArgumentException("file not found");
        }
        FileObject fileObject = toFileObject(entity);
        return fileStorageRegistry.get(fileObject.location().storageType())
                .generateAccessUrl(fileObject, Duration.ofSeconds(ttlSeconds), attachment);
    }

    @Override
    public InputStream load(String fileId) {
        SysFile entity = sysFileService.getById(fileId);
        if (entity == null || entity.getDelFlag() != null && entity.getDelFlag() != 0) {
            throw new IllegalArgumentException("file not found");
        }
        FileObject fileObject = toFileObject(entity);
        return fileStorageRegistry.get(fileObject.location().storageType()).load(fileObject.location());
    }

    @Override
    @BumpTableVersion(tables = "sys_file")
    public boolean delete(String fileId) {
        SysFile entity = sysFileService.getById(fileId);
        if (entity == null || entity.getStorageType() == null || entity.getStorageType().isBlank()) {
            return false;
        }
        FileObject fileObject = toFileObject(entity);
        fileStorageRegistry.get(fileObject.location().storageType()).delete(fileObject.location());
        return true;
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
        FileLocation location = null;
        if (f.getStorageType() != null && !f.getStorageType().isBlank()) {
            location = new FileLocation(
                    StorageType.valueOf(f.getStorageType()),
                    f.getBucketName(),
                    f.getObjectKey(),
                    null,
                    null
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

}
