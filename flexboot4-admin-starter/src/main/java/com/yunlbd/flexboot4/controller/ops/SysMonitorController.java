package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.service.ops.MonitorStatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 运行环境监控控制器
 *
 * @author Wangts
 * @Project_Name flexboot4
 * @since 2026年02月10日
 */
@RestController
@RequestMapping("/api/admin/monitor")
@Tag(name = "运行环境监控", description = "运行环境监控 - 容器/进程视角资源监控")
@ApiTagGroup(group = "运维管理")
public class SysMonitorController {

    private final MonitorStatsService monitorStatsService;

    public SysMonitorController(MonitorStatsService monitorStatsService) {
        this.monitorStatsService = monitorStatsService;
    }

    /**
     * 获取运行环境监控统计信息
     */
    @RequirePermission("sys:monitor:stats")
    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> getStats() {
        return ApiResult.success(monitorStatsService.getStats());
    }
}
