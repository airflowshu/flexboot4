package com.yunlbd.flexboot4.service.cms.impl;

import com.yunlbd.flexboot4.config.CmsRenderProperties;
import com.yunlbd.flexboot4.entity.cms.CmsArticle;
import com.yunlbd.flexboot4.service.cms.CmsTemplateRenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class CmsTemplateRenderServiceImpl implements CmsTemplateRenderService {

    private final SpringTemplateEngine templateEngine;
    private final CmsRenderProperties cmsRenderProperties;

    @Override
    public RenderResult renderArticle(CmsArticle article) {
        if (article == null || article.getId() == null || article.getId().isBlank()) {
            throw new IllegalArgumentException("文章不存在或 ID 为空");
        }

        Context context = new Context();
        context.setVariable("article", article);

        String html = templateEngine.process(cmsRenderProperties.getTemplateName(), context);
        Path outputDir = Path.of(cmsRenderProperties.getOutputDir()).toAbsolutePath().normalize();
        Path outputFile = outputDir.resolve(article.getId() + ".html");

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputFile, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("生成静态页面失败: " + outputFile, e);
        }

        String relativeUrl = normalizePrefix(cmsRenderProperties.getUrlPrefix()) + "/" + article.getId() + ".html";
        return new RenderResult(article.getId(), outputFile, relativeUrl);
    }

    private static String normalizePrefix(String prefix) {
        String normalized = (prefix == null || prefix.isBlank()) ? "/static/cms-pages" : prefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
