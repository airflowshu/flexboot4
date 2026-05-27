package com.yunlbd.flexboot4.autoconfigure;

import com.yunlbd.flexboot4.config.FileEmbeddingStreamConfig;
import com.yunlbd.flexboot4.config.FileEmbeddingStreamInitializer;
import com.yunlbd.flexboot4.config.FileParseConfig;
import com.yunlbd.flexboot4.listener.KbEmbeddingResultStreamListener;
import com.yunlbd.flexboot4.listener.SysFileChunkListener;
import com.yunlbd.flexboot4.listener.SysFileParseListener;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        FileEmbeddingStreamConfig.class,
        FileEmbeddingStreamInitializer.class,
        FileParseConfig.class,
        KbEmbeddingResultStreamListener.class,
        SysFileChunkListener.class,
        SysFileParseListener.class
})
@ComponentScan(
        basePackages = {
                "com.yunlbd.flexboot4.controller.kb",
                "com.yunlbd.flexboot4.file.parse.impl",
                "com.yunlbd.flexboot4.service.kb"
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootConfiguration.class)
)
public class FlexBoot4KbAutoConfiguration {
}
