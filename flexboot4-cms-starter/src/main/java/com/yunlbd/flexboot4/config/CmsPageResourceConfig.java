package com.yunlbd.flexboot4.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class CmsPageResourceConfig implements WebMvcConfigurer {

    private final CmsRenderProperties cmsRenderProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String urlPrefix = normalizePrefix(cmsRenderProperties.getUrlPrefix());
        String location = Path.of(cmsRenderProperties.getOutputDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }

    private static String normalizePrefix(String prefix) {
        String normalized = StringUtils.hasText(prefix) ? prefix.trim() : "/static/cms-pages";
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

