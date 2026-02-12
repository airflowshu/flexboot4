package com.yunlbd.flexboot4.file;

import java.io.InputStream;
import java.time.Duration;

public interface FileStorage {

    FileObject store(InputStream data,
                     long size,
                     String fileName,
                     String contentType,
                     FileObject meta);

    InputStream load(FileLocation location);

    void delete(FileLocation location);

    FileAccessDescriptor generateAccessUrl(FileLocation location,
                                           Duration ttl,
                                           boolean attachment);
}

