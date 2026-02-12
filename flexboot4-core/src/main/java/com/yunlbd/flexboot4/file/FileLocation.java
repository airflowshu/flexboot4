package com.yunlbd.flexboot4.file;

public record FileLocation(
        StorageType storageType,
        String bucket,
        String objectKey,
        String region,
        String endpoint
) {
}

