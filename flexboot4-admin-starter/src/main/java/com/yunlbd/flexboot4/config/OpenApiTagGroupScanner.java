package com.yunlbd.flexboot4.config;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAPI 标签分组扫描器
 * 在应用启动时自动扫描所有被 @ApiTagGroup 注解标记的 Controller，
 * 收集并组织标签分组信息，供 OpenApiConfig 使用
 *
 * @author Flexboot4
 * @since 2026-02
 */
@Slf4j
@Configuration
public class OpenApiTagGroupScanner implements BeanFactoryPostProcessor {

    private static final Map<String, ApiTagGroupInfo> TAG_GROUPS = new LinkedHashMap<>();

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        scanControllers(beanFactory);
    }

    /**
     * 扫描所有 Controller 类，收集被 @ApiTagGroup 注解标记的信息
     */
    private void scanControllers(ConfigurableListableBeanFactory beanFactory) {
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(beanFactory.getBeanClassLoader());

        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (beanDefinition.getRole() == BeanDefinition.ROLE_INFRASTRUCTURE) {
                continue;
            }

            try {
                AnnotationMetadata metadata = getAnnotationMetadata(beanDefinition, metadataReaderFactory);
                if (metadata == null || !isController(metadata)) {
                    continue;
                }

                Map<String, Object> apiTagGroupAttributes = metadata.getAnnotationAttributes(ApiTagGroup.class.getName());
                if (apiTagGroupAttributes == null) {
                    continue;
                }

                boolean enabled = Boolean.TRUE.equals(apiTagGroupAttributes.getOrDefault("enabled", Boolean.TRUE));
                String groupName = (String) apiTagGroupAttributes.get("group");
                if (!enabled || groupName == null || groupName.isBlank()) {
                    continue;
                }

                List<String> tagNames = extractTagNames(metadata);
                if (tagNames.isEmpty()) {
                    continue;
                }

                ApiTagGroupInfo groupInfo = TAG_GROUPS.computeIfAbsent(
                        groupName,
                        key -> new ApiTagGroupInfo(groupName)
                );
                groupInfo.addTags(tagNames);

                log.debug("Registered API tag group: Bean={}, Group={}, Tags={}", beanName, groupName, tagNames);
            } catch (Exception e) {
                log.debug("Skipping bean '{}' during tag group scan: {}", beanName, e.getMessage());
            }
        }

        log.info("OpenAPI tag groups scanned. Total groups: {}, Total tags: {}",
                TAG_GROUPS.size(),
                TAG_GROUPS.values().stream().mapToInt(g -> g.getTags().size()).sum());

        TAG_GROUPS.forEach((groupName, groupInfo) -> {
            log.debug("  Group '{}': {}", groupName, groupInfo.getTags());
        });
    }

    private AnnotationMetadata getAnnotationMetadata(BeanDefinition beanDefinition, MetadataReaderFactory metadataReaderFactory) {
        if (beanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
            return annotatedBeanDefinition.getMetadata();
        }

        String beanClassName = beanDefinition.getBeanClassName();
        if (beanClassName == null || beanClassName.isBlank()) {
            return null;
        }

        try {
            return metadataReaderFactory.getMetadataReader(beanClassName).getAnnotationMetadata();
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isController(AnnotationMetadata metadata) {
        return metadata.hasAnnotation(RestController.class.getName())
                || metadata.hasAnnotation(Controller.class.getName())
                || metadata.hasMetaAnnotation(RestController.class.getName())
                || metadata.hasMetaAnnotation(Controller.class.getName());
    }

    private List<String> extractTagNames(AnnotationMetadata metadata) {
        MultiValueMap<String, Object> tagAttributes = metadata.getAllAnnotationAttributes(Tag.class.getName());
        if (tagAttributes == null || tagAttributes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> names = tagAttributes.get("name");
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }

        return names.stream()
                .map(String::valueOf)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * 获取扫描结果：返回 OpenAPI 格式的标签分组列表
     */
    public static List<Map<String, Object>> getTagGroupsForOpenAPI() {
        return TAG_GROUPS.values().stream()
                .map(ApiTagGroupInfo::toOpenAPIFormat)
                .collect(Collectors.toList());
    }

    /**
     * 获取原始的标签分组信息（用于测试或其他用途）
     */
    public static Map<String, ApiTagGroupInfo> getRawTagGroups() {
        return Collections.unmodifiableMap(TAG_GROUPS);
    }

    /**
     * 清空所有已扫描的标签分组（主要用于测试场景）
     */
    public static void clear() {
        TAG_GROUPS.clear();
    }
}
