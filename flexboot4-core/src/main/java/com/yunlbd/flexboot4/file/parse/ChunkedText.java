package com.yunlbd.flexboot4.file.parse;

public record ChunkedText(
        String text,
        Integer pageNumber,
        String sectionTitle
) {
}

