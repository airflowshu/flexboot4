package com.yunlbd.flexboot4.storage;

import com.yunlbd.flexboot4.config.FileStorageProperties;
import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileLocation;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.file.StorageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalFileStorageTest {

    @TempDir
    Path rootDir;

    @Test
    void shouldStoreLoadAndDeleteLocalFile() throws Exception {
        LocalFileStorage storage = storage(rootDir);
        FileObject meta = meta("file-1");

        FileObject stored = storage.store(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
                5, "hello.txt", "text/plain", meta);

        assertThat(stored.location().storageType()).isEqualTo(StorageType.LOCAL);
        assertThat(stored.location().bucket()).isEqualTo("local");
        assertThat(stored.location().objectKey()).startsWith("tenant-a/files/");
        assertThat(storage.load(stored.location()).readAllBytes()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));

        storage.delete(stored.location());

        assertThat(Files.exists(storage.resolveObjectPath(stored.location().objectKey()))).isFalse();
    }

    @Test
    void shouldRejectObjectKeyOutsideRoot() {
        LocalFileStorage storage = storage(rootDir);

        assertThrows(IllegalArgumentException.class,
                () -> storage.resolveObjectPath("../escape.txt"));
    }

    @Test
    void shouldGenerateSignedAccessUrl() {
        LocalFileStorage storage = storage(rootDir);
        FileObject fileObject = meta("file-1");

        FileAccessDescriptor descriptor = storage.generateAccessUrl(fileObject, Duration.ofSeconds(60), true);

        assertThat(descriptor.url()).contains("/api/admin/file/access/");
        assertThat(descriptor.url()).endsWith("/preview.txt");
        assertThat(descriptor.token()).isNotBlank();
        assertThat(descriptor.disposition()).isEqualTo("attachment");
    }

    @Test
    void shouldGeneratePreviewFriendlySignedUrlForNonAsciiFileName() {
        LocalFileStorage storage = storage(rootDir);
        FileObject fileObject = meta("file-1", "QQ图片20260407140545(4).jpeg");

        FileAccessDescriptor descriptor = storage.generateAccessUrl(fileObject, Duration.ofSeconds(60), false);

        assertThat(descriptor.url()).endsWith("/preview.jpeg");
        assertThat(descriptor.url()).doesNotContain("%25");
        assertThat(descriptor.disposition()).isEqualTo("inline");
    }

    private LocalFileStorage storage(Path rootDir) {
        FileStorageProperties properties = new FileStorageProperties();
        properties.getLocal().setRootDir(rootDir);
        properties.getLocal().setBucket("local");
        properties.setAccessTokenSecret("secret-1234567890");
        return new LocalFileStorage(properties, new FileAccessTokenService(properties));
    }

    private FileObject meta(String id) {
        return meta(id, "hello.txt");
    }

    private FileObject meta(String id, String fileName) {
        return new FileObject(
                id,
                "tenant-a",
                "biz",
                "biz-1",
                fileName,
                null,
                "text/plain",
                5,
                "hash",
                new FileLocation(StorageType.LOCAL, null, null, null, null),
                "UPLOADED",
                0,
                null,
                null
        );
    }
}
