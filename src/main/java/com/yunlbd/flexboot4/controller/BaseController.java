package com.yunlbd.flexboot4.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.dto.SearchDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用 Controller 基类
 * 提供基础的 CRUD 和分页查询功能
 * @param <S> Service 类型
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public abstract class BaseController<S extends IService<T>, T, ID extends Serializable> {

    public static final String SearchDtoExample = "{\n" +
            "  \"pageNumber\": 1,\n" +
            "  \"pageSize\": 10,\n" +
            "  \"keyword\": \"test\",\n" +
            "  \"searchFields\": [\"name\", \"code\"],\n" +
            "  \"logic\": \"AND\",\n" +
            "  \"items\": [\n" +
            "    { \"field\": \"status\", \"op\": \"eq\", \"val\": 1 },\n" +
            "    { \n" +
            "      \"logic\": \"OR\",\n" +
            "      \"children\": [\n" +
            "        { \"field\": \"type\", \"op\": \"eq\", \"val\": \"A\" },\n" +
            "        { \"field\": \"type\", \"op\": \"eq\", \"val\": \"B\" }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"orders\": [\n" +
            "    { \"column\": \"createTime\", \"asc\": false },\n" +
            "    { \"column\": \"id\", \"asc\": true }\n" +
            "  ]\n" +
            "}";
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected S service;

    /**
     * 新增或更新
     * 若主键有值则更新，无值则插入
     */
    @Operation(summary = "Create or Update", description = "Insert if ID is null, update otherwise. Ignores null values.")
    @PutMapping
    public ApiResult<Boolean> save(@RequestBody T entity) {
        return ApiResult.success(service.saveOrUpdate(entity));
    }

    /**
     * 批量新增
     */
    @Operation(summary = "Batch Create", description = "Batch insert entities.")
    @PostMapping("/batch")
    public ApiResult<Boolean> saveBatch(@RequestBody List<T> entities) {
        return ApiResult.success(service.saveBatch(entities));
    }

    /**
     * 根据ID删除
     */
    @Operation(summary = "Delete by ID", description = "Delete entity by ID.")
    @DeleteMapping("/{id}")
    public ApiResult<Boolean> remove(@PathVariable ID id) {
        return ApiResult.success(service.removeById(id));
    }

    /**
     * 批量删除
     */
    @Operation(summary = "Batch Delete", description = "Delete entities by IDs.")
    @DeleteMapping
    public ApiResult<Boolean> removeBatch(@RequestBody Collection<ID> ids) {
        return ApiResult.success(service.removeByIds(ids));
    }

    /**
     * 根据ID获取详情
     */
    @Operation(summary = "Get by ID", description = "Get entity details by ID.")
    @GetMapping("/{id}")
    public ApiResult<T> get(@PathVariable ID id) {
        return ApiResult.success(service.getById(id));
    }

    /**
     * 分页查询
     * 子类可以通过重写 buildQueryWrapper 方法来定制查询条件
     */
    @Operation(summary = "Page Query", description = "Paged query with optional search parameters.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Search parameters",
        content = @io.swagger.v3.oas.annotations.media.Content(
            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SearchDto.class),
            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                value = SearchDtoExample
            )
        )
    )
    @PostMapping("/page")
    public ApiResult<Page<T>> page(@RequestBody SearchDto searchDto) {
        Page<T> page = new Page<>(searchDto.getPageNumber(), searchDto.getPageSize());
        QueryWrapper queryWrapper = buildQueryWrapper(searchDto);
        return ApiResult.success(service.page(page, queryWrapper));
    }

    /**
     * 列表查询
     */
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Search parameters",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SearchDto.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = SearchDtoExample
                    )
            )
    )
    @Operation(summary = "List Query", description = "List all entities matching criteria.")
    @PostMapping("/list")
    public ApiResult<List<T>> list(@RequestBody SearchDto searchDto) {
        QueryWrapper queryWrapper = buildQueryWrapper(searchDto);
        return ApiResult.success(service.list(queryWrapper));
    }

    /**
     * 构建查询条件
     * 默认实现支持 keyword 搜索、高级条件搜索和多字段排序
     * 子类可重写此方法进行扩展，建议调用 super.buildQueryWrapper(searchDto)
     */
    protected QueryWrapper buildQueryWrapper(SearchDto searchDto) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        
        // 1. 处理高级搜索条件 (items)
        if (searchDto.getItems() != null && !searchDto.getItems().isEmpty()) {
            queryWrapper.and(qw -> {
                processGroup(qw, searchDto.getItems(), searchDto.getLogic());
            });
        }

        // 2. 处理 Keyword 搜索 (keyword + searchFields)
        // 通常 keyword 搜索与高级搜索是 AND 关系
        if (searchDto.getKeyword() != null && !searchDto.getKeyword().isEmpty() 
                && searchDto.getSearchFields() != null && !searchDto.getSearchFields().isEmpty()) {
            
            String keyword = searchDto.getKeyword();
            queryWrapper.and(qw -> {
                for (String field : searchDto.getSearchFields()) {
                    // 简单的 SQL 注入防护：仅允许字母数字下划线和点号
                    if (!field.matches("^[a-zA-Z0-9_.]+$")) continue;
                    
                    // keyword 匹配多个字段之间通常是 OR 关系
                    // QueryWrapper 的 or(Consumer<QueryWrapper>) 需要正确使用
                    qw.or(sub -> { applyCondition(sub, field, "like", keyword); });
                }
            });
        }
        
        // 3. 处理排序 (orders > orderBy)
        if (searchDto.getOrders() != null && !searchDto.getOrders().isEmpty()) {
            for (SearchDto.OrderItem order : searchDto.getOrders()) {
                if (order.getColumn() != null && order.getColumn().matches("^[a-zA-Z0-9_.]+$")) {
                    String snakeColumn = camelToSnake(order.getColumn());
                    queryWrapper.orderBy(snakeColumn, order.isAsc());
                }
            }
        }
        return queryWrapper;
    }

    private void processGroup(QueryWrapper qw, List<SearchDto.SearchItem> items, String logic) {
        boolean isOr = "OR".equalsIgnoreCase(logic);
        for (SearchDto.SearchItem item : items) {
            Consumer<QueryWrapper> consumer = sub -> processItem(sub, item);
            if (isOr) {
                qw.or(consumer);
            } else {
                qw.and(consumer);
            }
        }
    }

    private void processItem(QueryWrapper qw, SearchDto.SearchItem item) {
        // 如果有子条件，递归处理
        if (item.getChildren() != null && !item.getChildren().isEmpty()) {
            processGroup(qw, item.getChildren(), item.getLogic());
        } else {
            // 处理单个条件
            String field = item.getField();
            String op = item.getOp() != null ? item.getOp().toLowerCase() : "eq";
            Object val = item.getVal();

            if (field == null || field.isEmpty()) return;
            
            // 简单的 SQL 注入防护：仅允许字母数字下划线和点号
            if (!field.matches("^[a-zA-Z0-9_.]+$")) return;

            applyCondition(qw, field, op, val);
        }
    }

    /**
     * 将驼峰命名转换为下划线命名
     * 支持点号分隔的表名.字段名格式，例如: sysUser.userName -> sys_user.user_name
     */
    private String camelToSnake(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        // 简单防注入：只允许字母、数字、下划线和点号
        if (!str.matches("^[a-zA-Z0-9_.]+$")) {
            return str; // 或者抛出异常，这里原样返回交给 QueryWrapper 处理可能会报错但安全
        }

        // 如果包含点号，分别处理
        if (str.contains(".")) {
            String[] parts = str.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(".");
                sb.append(camelToSnakeSingle(parts[i]));
            }
            return sb.toString();
        }
        
        return camelToSnakeSingle(str);
    }

    private String camelToSnakeSingle(String str) {
        Matcher matcher = Pattern.compile("[A-Z]").matcher(str);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString().startsWith("_") ? sb.substring(1) : sb.toString();
    }

    @SuppressWarnings({"rawtypes"})
    private void applyCondition(QueryWrapper qw, String field, String op, Object val) {
        // 将前端传来的驼峰字段名转换为数据库的下划线字段名
        String snakeField = camelToSnake(field);
        
        // 针对 PostgreSQL 时间字段的处理：尝试将字符串解析为 LocalDateTime
        Object processedVal = val;
        if (val instanceof String valStr && (field.toLowerCase().contains("time") || field.toLowerCase().contains("date"))) {
            // 尝试解析常见的日期时间格式
            try {
                // 1. yyyy-MM-dd HH:mm:ss
                if (valStr.length() == 19) {
                    processedVal = LocalDateTime.parse(valStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } 
                // 2. yyyy-MM-dd
                else if (valStr.length() == 10) {
                    // 如果是日期，根据操作符决定是当天的开始还是结束
                    // 对于 >=, > 使用当天的开始
                    // 对于 <=, < 使用当天的结束
                    // 对于 = 精确匹配日期通常比较困难，这里简化为当天的开始，或者需要改为范围查询
                    if ("le".equalsIgnoreCase(op) || "lt".equalsIgnoreCase(op)) {
                         processedVal = LocalDateTime.parse(valStr + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } else {
                         processedVal = LocalDateTime.parse(valStr + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                }
            } catch (DateTimeParseException e) {
                // 解析失败，保持原值，交给数据库层处理或报错
            }
        }

        // 使用 QueryColumn 直接调用条件方法，不通过 QueryCondition.create (因为该静态方法可能不存在)
        com.mybatisflex.core.query.QueryColumn column = new com.mybatisflex.core.query.QueryColumn(snakeField);
        
        switch (op) {
            case "eq": qw.and(column.eq(processedVal)); break;
            case "ne": qw.and(column.ne(processedVal)); break;
            case "gt": qw.and(column.gt(processedVal)); break;
            case "ge": qw.and(column.ge(processedVal)); break;
            case "lt": qw.and(column.lt(processedVal)); break;
            case "le": qw.and(column.le(processedVal)); break;
            case "like": qw.and(column.like(processedVal)); break;
            case "notlike": qw.and(column.notLike(processedVal)); break;
            case "in": 
                if (processedVal instanceof Collection) {
                    qw.and(column.in((Collection) processedVal));
                } else if (processedVal instanceof Object[]) {
                    qw.and(column.in((Object[]) processedVal));
                }
                break;
            case "notin":
                if (processedVal instanceof Collection) {
                    qw.and(column.notIn((Collection) processedVal));
                } else if (processedVal instanceof Object[]) {
                    qw.and(column.notIn((Object[]) processedVal));
                }
                break;
            case "isnull": qw.and(column.isNull()); break;
            case "notnull": qw.and(column.isNotNull()); break;
            default: qw.and(column.eq(processedVal)); break;
        }
    }
}
