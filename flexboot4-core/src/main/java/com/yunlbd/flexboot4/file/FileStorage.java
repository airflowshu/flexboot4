package com.yunlbd.flexboot4.file;

import java.io.InputStream;
import java.time.Duration;

public interface FileStorage {

    StorageType storageType();

    FileObject store(InputStream data,
                     long size,
                     String fileName,
                     String contentType,
                     FileObject meta);

    InputStream load(FileLocation location);

    void delete(FileLocation location);

    FileAccessDescriptor generateAccessUrl(FileObject fileObject,
                                           Duration ttl,
                                           boolean attachment);
}

