package com.yunlbd.flexboot4.file.parse;

import java.io.InputStream;

public interface FileParser {
    boolean supports(String contentType, String fileName);

    ParsedDocument parse(String fileId, InputStream in);
}

