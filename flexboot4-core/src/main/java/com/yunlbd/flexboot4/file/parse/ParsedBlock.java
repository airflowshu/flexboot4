package com.yunlbd.flexboot4.file.parse;

public record ParsedBlock(
        String text,
        Integer pageNumber,
        String sectionTitle
) {
}

