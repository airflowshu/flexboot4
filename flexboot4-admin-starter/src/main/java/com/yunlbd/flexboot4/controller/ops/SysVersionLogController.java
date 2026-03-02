package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.ops.SysVersionLog;
import com.yunlbd.flexboot4.service.ops.SysVersionLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/version-log")
@RequiredArgsConstructor
@Tag(name = "版本日志", description = "SysVersionLog - 系统版本更新日志")
@ApiTagGroup(group = "系统管理")
public class SysVersionLogController extends BaseController<SysVersionLogService, SysVersionLog, String> {

    @Override
    public Class<SysVersionLog> getEntityClass() {
        return SysVersionLog.class;
    }
}
