package com.yunlbd.flexboot4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "通用搜索参数对象")
public class SearchDto implements Serializable {
    /**
     * 当前页码
     */
    @Schema(description = "当前页码", example = "1")
    private Integer pageNumber = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;

    /**
     * 多字段排序列表
     */
    @Schema(description = "多字段排序列表")
    private List<OrderItem> orders;

    /**
     * 高级搜索条件列表
     */
    @Schema(description = "高级搜索条件列表")
    private List<SearchItem> items;

    /**
     * 高级搜索条件之间的逻辑关系 (AND / OR)，默认 AND
     */
    @Schema(description = "items 条件之间的逻辑关系 (AND / OR)", example = "AND", defaultValue = "AND")
    private String logic = "AND";

    @Data
    @Schema(description = "高级搜索条件项")
    public static class SearchItem implements Serializable {
        /**
         * 字段名
         */
        @Schema(description = "字段名", example = "status")
        private String field;
        /**
         * 操作符: eq, ne, gt, ge, lt, le, like, notlike, in, notin, isnull, notnull
         */
        @Schema(description = "操作符: eq(等于), ne(不等于), gt(大于), ge(大于等于), lt(小于), le(小于等于), like(包含), notlike(不包含), in(包含于), notin(不包含于), isnull(为空), notnull(不为空)", example = "eq")
        private String op;
        /**
         * 值
         */
        @Schema(description = "字段值", example = "1")
        private Object val;

        /**
         * 组内逻辑
         */
        @Schema(description = "组内逻辑 (AND / OR)，仅当 children 不为空时有效", example = "AND")
        private String logic = "AND";

        /**
         * 子条件列表
         */
        @Schema(description = "子条件列表，用于构建嵌套查询")
        private List<SearchItem> children;
    }

    @Data
    @Schema(description = "排序项")
    public static class OrderItem implements Serializable {
        /**
         * 排序字段
         */
        @Schema(description = "排序字段名", example = "createTime")
        private String column;
        /**
         * 是否升序
         */
        @Schema(description = "是否升序", example = "false", defaultValue = "true")
        private boolean asc = true;
    }

    public static SearchDto.SearchItem item(String field, String op, Object val) {
        SearchDto.SearchItem it = new SearchDto.SearchItem();
        it.setField(field);
        it.setOp(op);
        it.setVal(val);
        return it;
    }
}
