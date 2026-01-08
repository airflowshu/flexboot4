package com.yunlbd.flexboot4.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.relation.RelationManager;
import com.mybatisflex.core.service.IService;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.query.SearchDtoUtils;
import com.yunlbd.flexboot4.support.ReactiveExportSupport;
import com.yunlbd.flexboot4.util.ExcelExportUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 通用 Controller 基类
 * 提供基础的 CRUD 和分页查询功能
 * @param <S> Service 类型
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public abstract class BaseController<S extends IService<T>, T, ID extends Serializable> {

    public static final String SearchDtoExample = """
            {
              "pageNumber": 1,
              "pageSize": 10,
              "logic": "AND",
              "items": [
                { "field": "status", "op": "eq", "val": 1 },
                {\s
                  "logic": "OR",
                  "children": [
                    { "field": "type", "op": "eq", "val": "A" },
                    { "field": "type", "op": "eq", "val": "B" }
                  ]
                }
              ],
              "orders": [
                { "column": "createTime", "asc": false },
                { "column": "id", "asc": true }
              ]
            }""";
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected S service;
    private static final Logger log = LoggerFactory.getLogger(BaseController.class);

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
        T entity = service.getById(id);
        if (entity != null) {
            RelationManager.queryRelations(service.getMapper(), List.of(entity));
        }
        return ApiResult.success(entity);
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
        service.page(page, queryWrapper);
        if (SearchDtoUtils.hasRelationPaths(searchDto)) {
            RelationManager.queryRelations(service.getMapper(), page.getRecords());
        }
        return ApiResult.success(page);
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
        QueryWrapper queryWrapper = buildQueryWrapper(searchDto, getEntityClass());
        List<T> records = service.list(queryWrapper);
        if (SearchDtoUtils.hasRelationPaths(searchDto)) {
            RelationManager.queryRelations(service.getMapper(), records);
        }
        return ApiResult.success(records);
    }

    @Operation(summary = "Export Excel", description = "Export matching records to Excel. Supports GET/POST and HTTP Range resume.")
    @GetMapping("/export")
    public void exportGet(@ModelAttribute SearchDto searchDto, HttpServletRequest request, HttpServletResponse response) {
        doExport(searchDto, request, response);
    }

    @Operation(summary = "Export Excel", description = "Export matching records to Excel. Supports GET/POST and HTTP Range resume.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Search parameters",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SearchDto.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = SearchDtoExample
                    )
            )
    )
    @PostMapping("/export")
    public void exportPost(@RequestBody SearchDto searchDto, HttpServletRequest request, HttpServletResponse response) {
        doExport(searchDto, request, response);
    }

    protected void doExport(SearchDto searchDto, HttpServletRequest request, HttpServletResponse response) {
        long startNs = System.nanoTime();
        QueryWrapper queryWrapper = buildQueryWrapper(searchDto, getEntityClass());
        var flux = ReactiveExportSupport.queryFlux(service, queryWrapper, getEntityClass(), 1000);
        String name = getEntityClass().getSimpleName() + "_" + UUID.randomUUID();
        var file = ExcelExportUtil.writeFluxToTempFile(flux, getEntityClass(), 1000);
        ExcelExportUtil.streamFileWithRange(request, response, file, name + ".xlsx");
        long endNs = System.nanoTime();
        log.info("Export {} rows done in {} ms", file.length(), (endNs - startNs) / 1_000_000);
    }

    protected QueryWrapper buildQueryWrapper(SearchDto searchDto) {
        return buildQueryWrapper(searchDto, getEntityClass());
    }

    protected abstract Class<T> getEntityClass();

    public QueryWrapper buildQueryWrapper(SearchDto searchDto, Class<?> entityClass) {
        return com.yunlbd.flexboot4.query.DefaultQueryWrapperBuilder.get().build(searchDto, entityClass);
    }


}
