package com.yunlbd.flexboot4.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cms.template")
public class CmsTemplateProperties {

    /**
     * 模板根目录。
     */
    private String rootDir = System.getProperty("user.dir") + "/webapp/html/web";

    /**
     * 预览时静态资源根地址。
     */
    private String assetBaseUrl = "http://localhost:8080";

    /**
     * 模板发布根目录。
     */
    private String publishDir = System.getProperty("user.home") + "/flexboot4-cms-pages/site-published";
}
