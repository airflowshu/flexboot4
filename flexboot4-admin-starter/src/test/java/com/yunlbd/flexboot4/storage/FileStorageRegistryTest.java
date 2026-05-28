package com.yunlbd.flexboot4.storage;

import com.yunlbd.flexboot4.config.FileStorageProperties;
import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileLocation;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.file.FileStorage;
import com.yunlbd.flexboot4.file.StorageType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageRegistryTest {

    @Test
    void activeStorageShouldDefaultToLocal() {
        FileStorageRegistry registry = new FileStorageRegistry(new FileStorageProperties(), List.of(new StubStorage(StorageType.LOCAL)));

        assertThat(registry.active().storageType()).isEqualTo(StorageType.LOCAL);
    }

    @Test
    void activeLocalShouldStillAllowLookupOfHistoricalMinioStorage() {
        FileStorageRegistry registry = new FileStorageRegistry(new FileStorageProperties(), List.of(
                new StubStorage(StorageType.LOCAL),
                new StubStorage(StorageType.MINIO)
        ));

        assertThat(registry.active().storageType()).isEqualTo(StorageType.LOCAL);
        assertThat(registry.get(StorageType.MINIO).storageType()).isEqualTo(StorageType.MINIO);
    }

    @Test
    void activeStorageShouldUseConfiguredType() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setType("minio");
        FileStorageRegistry registry = new FileStorageRegistry(properties, List.of(
                new StubStorage(StorageType.LOCAL),
                new StubStorage(StorageType.MINIO)
        ));

        assertThat(registry.active().storageType()).isEqualTo(StorageType.MINIO);
    }

    @Test
    void shouldFailWhenConfiguredStorageMissing() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setType("minio");
        FileStorageRegistry registry = new FileStorageRegistry(properties, List.of(new StubStorage(StorageType.LOCAL)));

        assertThrows(IllegalStateException.class, registry::active);
    }

    private record StubStorage(StorageType storageType) implements FileStorage {
        @Override
        public FileObject store(InputStream data, long size, String fileName, String contentType, FileObject meta) {
            return meta;
        }

        @Override
        public InputStream load(FileLocation location) {
            return InputStream.nullInputStream();
        }

        @Override
        public void delete(FileLocation location) {
        }

        @Override
        public FileAccessDescriptor generateAccessUrl(FileObject fileObject, Duration ttl, boolean attachment) {
            return null;
        }
    }
}
