package com.yunlbd.flexboot4.controller.ops;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.SysOperLog;
import com.yunlbd.flexboot4.query.DefaultQueryWrapperBuilder;
import com.yunlbd.flexboot4.service.SysOperLogService;
import com.yunlbd.flexboot4.util.LogTableUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static com.yunlbd.flexboot4.controller.sys.BaseController.SearchDtoExample;

/**
 * 系统日志管理
 *
 * @author Wangts
 * @Project_Name flexboot4
 * @since 2026年01月20日 9:18
 */
@RestController
@RequestMapping("/api/oper-log")
@RequiredArgsConstructor
public class SysOperLogController {
    private final SysOperLogService operLogService;

    @Operation(summary = "分页查询日志记录", description = "通用分页查询日志记录，支持跨季度自动分表查询")
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
    public ApiResult<Page<SysOperLog>> page(@RequestBody SearchDto searchDto) {
        // 1. 从 SearchDto 提取时间范围
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;

        if (searchDto.getItems() != null) {
            for (SearchDto.SearchItem item : searchDto.getItems()) {
                if ("operTime".equals(item.getField())) {
                    if ("ge".equals(item.getOp())) startTime = LogTableUtils.parseDate(item.getVal());
                    if ("gt".equals(item.getOp())) startTime = LogTableUtils.parseDate(item.getVal());
                    if ("le".equals(item.getOp())) endTime = LogTableUtils.parseDate(item.getVal());
                    if ("lt".equals(item.getOp())) endTime = LogTableUtils.parseDate(item.getVal());
                }
            }
        }

        // 2. 获取涉及的所有表名
        List<String> tables = LogTableUtils.getTableNames(startTime, endTime);

        // 3. 构建 Union 查询
        QueryWrapper mainWrapper = null;
        for (String table : tables) {
            // 为每个表构建独立的查询条件，传入具体的表名
            QueryWrapper qw = DefaultQueryWrapperBuilder.get().build(searchDto, SysOperLog.class, table);

            if (mainWrapper == null) {
                mainWrapper = qw;
            } else {
                mainWrapper.unionAll(qw);
            }
        }

        // 确保 mainWrapper 不为空（理论上 tables 至少有一个元素）
        if (mainWrapper == null) {
            mainWrapper = QueryWrapper.create().from(LogTableUtils.getCurrentQuarterTableName());
        }

        // 4. 执行分页查询
        // 注意：MyBatis-Flex 会自动处理 UNION ALL 后的分页统计
        Page<SysOperLog> page = new Page<>(searchDto.getPageNumber(), searchDto.getPageSize());
        Page<SysOperLog> result = operLogService.page(page, mainWrapper);
        
        return ApiResult.success(result);
    }

}
