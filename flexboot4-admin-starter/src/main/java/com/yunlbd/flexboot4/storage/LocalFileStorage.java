package com.yunlbd.flexboot4.storage;

import com.yunlbd.flexboot4.config.FileStorageProperties;
import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileLocation;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.file.FileStorage;
import com.yunlbd.flexboot4.file.StorageType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileStorageProperties properties;
    private final FileAccessTokenService tokenService;

    public LocalFileStorage(FileStorageProperties properties, FileAccessTokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    @Override
    public StorageType storageType() {
        return StorageType.LOCAL;
    }

    @Override
    public FileObject store(InputStream data, long size, String fileName, String contentType, FileObject meta) {
        String ext = extension(fileName);
        String objectKey = buildObjectKey(meta, ext);
        Path target = resolveObjectPath(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        FileLocation location = new FileLocation(StorageType.LOCAL, bucket(), objectKey, null, null);
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

    @Override
    public InputStream load(FileLocation location) {
        try {
            return Files.newInputStream(resolveObjectPath(location.objectKey()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(FileLocation location) {
        try {
            Files.deleteIfExists(resolveObjectPath(location.objectKey()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileAccessDescriptor generateAccessUrl(FileObject fileObject, Duration ttl, boolean attachment) {
        Instant expireAt = Instant.now().plus(ttl);
        String token = tokenService.createToken(fileObject.id(), expireAt, attachment);
        String path = normalizeAccessPath(properties.getAccessUrlPath());
        String url = buildAccessUrl(path, token, fileObject.fileName());
        return new FileAccessDescriptor(url, expireAt, attachment ? "attachment" : "inline", token);
    }

    public Path resolveObjectPath(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("文件对象路径不能为空");
        }
        Path root = rootDir();
        Path path = root.resolve(objectKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("文件对象路径越界");
        }
        return path;
    }

    private Path rootDir() {
        Path root = properties.getLocal().getRootDir();
        if (root == null) {
            root = Path.of(System.getProperty("user.home"), "flexboot4-files");
        }
        return root.toAbsolutePath().normalize();
    }

    private String bucket() {
        String bucket = properties.getLocal().getBucket();
        return bucket == null || bucket.isBlank() ? "local" : bucket;
    }

    private String buildObjectKey(FileObject meta, String ext) {
        StringBuilder sb = new StringBuilder();
        String tenantId = meta.tenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            sb.append(sanitizeSegment(tenantId)).append('/');
        }
        sb.append("files/")
                .append(LocalDate.now().format(DATE_PATH))
                .append('/')
                .append(UUID.randomUUID());
        if (ext != null && !ext.isBlank()) {
            sb.append('.').append(ext);
        }
        return sb.toString();
    }

    private String extension(String fileName) {
        int dot = fileName != null ? fileName.lastIndexOf('.') : -1;
        if (dot < 0 || dot + 1 >= fileName.length()) {
            return null;
        }
        return sanitizeSegment(fileName.substring(dot + 1));
    }

    private String sanitizeSegment(String segment) {
        return segment.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String normalizeAccessPath(String path) {
        if (path == null || path.isBlank()) {
            return "/api/admin/file/access";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String buildAccessUrl(String path, String token, String fileName) {
        String previewName = previewFileName(fileName);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(path)
                    .pathSegment(token, previewName)
                    .toUriString();
        }
        return path + "/" + token + "/" + previewName;
    }

    private String previewFileName(String fileName) {
        String ext = extension(fileName);
        if (ext == null || ext.isBlank()) {
            return "preview";
        }
        return "preview." + ext.toLowerCase(Locale.ROOT);
    }
}
