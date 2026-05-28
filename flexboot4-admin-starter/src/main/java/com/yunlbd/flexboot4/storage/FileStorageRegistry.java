package com.yunlbd.flexboot4.storage;

import com.yunlbd.flexboot4.config.FileStorageProperties;
import com.yunlbd.flexboot4.file.FileStorage;
import com.yunlbd.flexboot4.file.StorageType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class FileStorageRegistry {

    private final FileStorageProperties properties;
    private final Map<StorageType, FileStorage> storages;

    public FileStorageRegistry(FileStorageProperties properties, List<FileStorage> fileStorages) {
        this.properties = properties;
        this.storages = new EnumMap<>(StorageType.class);
        for (FileStorage fileStorage : fileStorages) {
            this.storages.put(fileStorage.storageType(), fileStorage);
        }
    }

    public FileStorage active() {
        return get(resolveConfiguredType());
    }

    public FileStorage get(StorageType storageType) {
        FileStorage fileStorage = storages.get(storageType);
        if (fileStorage == null) {
            throw new IllegalStateException("File storage is not configured: " + storageType);
        }
        return fileStorage;
    }

    private StorageType resolveConfiguredType() {
        String type = properties.getType();
        if (type == null || type.isBlank()) {
            return StorageType.LOCAL;
        }
        return StorageType.valueOf(type.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
