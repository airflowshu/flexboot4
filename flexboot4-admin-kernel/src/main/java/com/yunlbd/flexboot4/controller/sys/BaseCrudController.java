package com.yunlbd.flexboot4.controller.sys;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.converter.CrudMapper;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.yunlbd.flexboot4.excel.ReactiveExportSupport;
import com.yunlbd.flexboot4.service.sys.IExtendedService;
import com.yunlbd.flexboot4.util.ExcelExportUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * DTO/VO oriented CRUD controller base.
 */
public abstract class BaseCrudController<S extends IExtendedService<E>, E, ID extends Serializable,
        CreateReq, UpdateReq, ListVO, DetailVO> {

    public static final String SearchDtoExample = """
            {
              "pageNumber": 1,
              "pageSize": 10,
              "logic": "AND",
              "items": [
                { "field": "status", "op": "eq", "val": 1 },
                {
                  "logic": "OR",
                  "children": [
                    { "field": "type", "op": "eq", "val": "A" },
                    { "field": "createTime", "op": "gt", "val": "2026-01-01 00:00:00" }
                  ]
                }
              ],
              "orders": [
                { "column": "createTime", "asc": false },
                { "column": "id", "asc": true }
              ]
            }""";

    private static final Logger log = LoggerFactory.getLogger(BaseCrudController.class);

    protected final S service;
    protected final CrudMapper<E, CreateReq, UpdateReq, ListVO, DetailVO> crudMapper;

    protected BaseCrudController(S service,
                                 CrudMapper<E, CreateReq, UpdateReq, ListVO, DetailVO> crudMapper) {
        this.service = service;
        this.crudMapper = crudMapper;
    }

    @Operation(summary = "Create", description = "Create entity.")
    @OperLog(businessType = BusinessType.INSERT)
    @PostMapping
    public ApiResult<Boolean> create(@Valid @RequestBody CreateReq request) {
        E entity = beforeCreate(crudMapper.toEntity(request), request);
        return ApiResult.success(service.save(entity));
    }

    @Operation(summary = "Update by ID", description = "Update entity by ID. Ignores null values.")
    @OperLog(businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public ApiResult<Boolean> update(@PathVariable ID id, @Valid @RequestBody UpdateReq request) {
        E entity = service.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("数据不存在: " + id);
        }
        crudMapper.updateEntity(request, entity);
        setEntityId(entity, id);
        beforeUpdate(entity, id, request);
        return ApiResult.success(service.updateById(entity, true));
    }

    @Operation(summary = "Delete by ID", description = "Delete entity by ID.")
    @OperLog(businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public ApiResult<Boolean> remove(@PathVariable ID id) {
        return ApiResult.success(service.removeById(id));
    }

    @Operation(summary = "Batch Delete", description = "Delete entities by IDs.")
    @OperLog(businessType = BusinessType.DELETE)
    @DeleteMapping
    public ApiResult<Boolean> removeBatch(@RequestBody Collection<ID> ids) {
        return ApiResult.success(service.removeByIds(ids));
    }

    @Operation(summary = "Get by ID", description = "Get entity details by ID.")
    @OperLog(businessType = BusinessType.QUERY, isSaveResponseData = false)
    @GetMapping("/{id}")
    public ApiResult<DetailVO> get(@PathVariable ID id) {
        E entity = service.getById(id);
        return ApiResult.success(entity == null ? null : crudMapper.toDetailVO(entity));
    }

    @Operation(summary = "Page Query", description = "Paged query with optional search parameters.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Search parameters",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SearchDto.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = SearchDtoExample)
            )
    )
    @OperLog(businessType = BusinessType.QUERY, isSaveResponseData = false)
    @PostMapping("/page")
    public ApiResult<Page<ListVO>> page(@RequestBody SearchDto searchDto) {
        SearchDto normalized = normalizeSearchDto(searchDto);
        validateSearchDto(normalized);
        Page<E> sourcePage = service.pageWithRelations(normalized);
        Page<ListVO> targetPage = new Page<>(sourcePage.getPageNumber(), sourcePage.getPageSize());
        targetPage.setTotalRow(sourcePage.getTotalRow());
        targetPage.setRecords(crudMapper.toListVOList(sourcePage.getRecords()));
        return ApiResult.success(targetPage);
    }

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Search parameters",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SearchDto.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = SearchDtoExample)
            )
    )
    @Operation(summary = "List Query", description = "List all entities matching criteria.")
    @OperLog(businessType = BusinessType.QUERY, isSaveResponseData = false)
    @PostMapping("/list")
    public ApiResult<List<ListVO>> list(@RequestBody SearchDto searchDto) {
        SearchDto normalized = normalizeSearchDto(searchDto);
        validateSearchDto(normalized);
        return ApiResult.success(crudMapper.toListVOList(service.listWithRelations(normalized)));
    }

    @Operation(summary = "Export Excel", description = "Export matching records to Excel. Supports GET/POST and HTTP Range resume.")
    @OperLog(businessType = BusinessType.EXPORT, isSaveResponseData = false)
    @GetMapping("/export")
    public void exportGet(@ModelAttribute SearchDto searchDto, HttpServletRequest request, HttpServletResponse response) {
        doExport(normalizeSearchDto(searchDto), request, response);
    }

    @Operation(summary = "Export Excel", description = "Export matching records to Excel. Supports GET/POST and HTTP Range resume.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Search parameters",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SearchDto.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = SearchDtoExample)
            )
    )
    @OperLog(businessType = BusinessType.EXPORT, isSaveResponseData = false)
    @PostMapping("/export")
    public void exportPost(@RequestBody SearchDto searchDto, HttpServletRequest request, HttpServletResponse response) {
        doExport(normalizeSearchDto(searchDto), request, response);
    }

    @Operation(summary = "Import Excel", description = "Import records from Excel. Disabled unless controller enables it.")
    @OperLog(businessType = BusinessType.IMPORT, isSaveResponseData = false)
    @PostMapping("/import")
    public ApiResult<Boolean> importExcel() {
        throw new UnsupportedOperationException("Excel导入未开启");
    }

    protected void doExport(SearchDto searchDto, HttpServletRequest request, HttpServletResponse response) {
        validateSearchDto(searchDto);
        long startNs = System.nanoTime();
        QueryWrapper queryWrapper = buildQueryWrapper(searchDto, getEntityClass());
        var file = writeExportFile(queryWrapper);
        String name = getEntityClass().getSimpleName() + "_" + UUID.randomUUID();
        ExcelExportUtil.streamFileWithRange(request, response, file, name + ".xlsx");
        long endNs = System.nanoTime();
        log.info("Export {} Content-Length done in {} ms", file.length(), (endNs - startNs) / 1_000_000);
    }

    private <ExportRow> java.io.File writeExportFile(QueryWrapper queryWrapper) {
        @SuppressWarnings("unchecked")
        CrudExcelSupport<E, ExportRow, ?> excel = (CrudExcelSupport<E, ExportRow, ?>) excelSupport();
        Flux<ExportRow> flux = ReactiveExportSupport.<E, E>queryFlux(service, queryWrapper, getEntityClass(), 1000)
                .map(excel::toExportRow);
        return ExcelExportUtil.writeFluxToTempFile(flux, excel.exportRowClass(), 1000);
    }

    protected E beforeCreate(E entity, CreateReq request) {
        return entity;
    }

    protected void beforeUpdate(E entity, ID id, UpdateReq request) {
    }

    protected CrudFieldPolicy fieldPolicy() {
        return CrudFieldPolicy.entitySimpleFields(getEntityClass());
    }

    protected CrudExcelSupport<E, ?, ?> excelSupport() {
        return CrudExcelSupport.entity(getEntityClass());
    }

    public abstract Class<E> getEntityClass();

    public QueryWrapper buildQueryWrapper(SearchDto searchDto, Class<?> entityClass) {
        validateSearchDto(searchDto);
        return com.yunlbd.flexboot4.query.DefaultQueryWrapperBuilder.get().build(searchDto, entityClass);
    }

    protected void validateSearchDto(SearchDto searchDto) {
        fieldPolicy().validate(searchDto, getEntityClass());
    }

    protected SearchDto normalizeSearchDto(SearchDto searchDto) {
        return searchDto == null ? new SearchDto() : searchDto;
    }

    protected void setEntityId(E entity, ID id) {
        if (entity instanceof BaseEntity baseEntity) {
            baseEntity.setId(String.valueOf(id));
        }
    }
}
