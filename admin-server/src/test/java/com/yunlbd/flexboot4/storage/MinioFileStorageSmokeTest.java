package com.yunlbd.flexboot4.storage;

import com.yunlbd.flexboot4.config.MinioProperties;
import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileLocation;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.file.StorageType;
import io.minio.MinioClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Duration;

class MinioFileStorageSmokeTest {

    @Test
    @Disabled("仅用于本地联通性验证")
    void storeAndGenerateUrl() {
        MinioProperties properties = new MinioProperties("http://192.168.11.104:9000", "minioadmin", "minioadmin", "flexboot4-files", false);
        MinioClient client = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
        MinioFileStorage storage = new MinioFileStorage(client, properties);
        byte[] bytes = "hello".getBytes();
        FileObject meta = new FileObject("test-id", "tenant", null, null, null, "test.txt", "txt", "text/plain", bytes.length, null, new FileLocation(StorageType.MINIO, null, null, null, null), "UPLOADED", 0, null, null);
        FileObject stored = storage.store(new ByteArrayInputStream(bytes), bytes.length, meta.fileName(), meta.mimeType(), meta);
        FileAccessDescriptor descriptor = storage.generateAccessUrl(stored.location(), Duration.ofSeconds(60), true);
        assert descriptor.url() != null;
    }
}

