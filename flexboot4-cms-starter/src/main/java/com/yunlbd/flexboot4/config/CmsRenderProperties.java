package com.yunlbd.flexboot4.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cms.render")
public class CmsRenderProperties {

    /**
     * 静态页面输出目录（建议配置为项目外目录）
     */
    private String outputDir = System.getProperty("user.home") + "/flexboot4-cms-pages";

    /**
     * 访问静态页的 URL 前缀，对应 ResourceHandler
     */
    private String urlPrefix = "/static/cms-pages";

    /**
     * Thymeleaf 模板名（classpath:/templates/ 下）
     */
    private String templateName = "cms/article";

    /**
     * 文章审核通过时是否自动生成静态页
     */
    private boolean autoGenerateOnApprove = true;
}

