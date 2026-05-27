package com.yunlbd.flexboot4.config;

import java.util.*;

/**
 * API 标签分组的元数据模型
 * 用于存储和管理 OpenAPI 中的标签分组信息
 *
 * @author Flexboot4
 * @since 2026-02
 */
public class ApiTagGroupInfo {

    private String group;
    private Set<String> tags;

    public ApiTagGroupInfo(String group) {
        this.group = group;
        this.tags = new LinkedHashSet<>();
    }

    /**
     * 添加标签到分组
     */
    public void addTag(String tag) {
        if (tag != null && !tag.isBlank()) {
            this.tags.add(tag);
        }
    }

    /**
     * 添加多个标签到分组
     */
    public void addTags(Collection<String> tags) {
        if (tags != null) {
            tags.stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .forEach(this.tags::add);
        }
    }

    /**
     * 转换为 OpenAPI 配置格式（Map）
     */
    public Map<String, Object> toOpenAPIFormat() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", this.group);
        map.put("tags", new ArrayList<>(this.tags));
        return map;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "ApiTagGroupInfo{" +
                "group='" + group + '\'' +
                ", tags=" + tags +
                '}';
    }
}

