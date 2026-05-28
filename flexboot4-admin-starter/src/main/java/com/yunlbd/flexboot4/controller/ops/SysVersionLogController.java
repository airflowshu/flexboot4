package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.ops.SysVersionLogCreateReq;
import com.yunlbd.flexboot4.dto.ops.SysVersionLogUpdateReq;
import com.yunlbd.flexboot4.entity.ops.SysVersionLog;
import com.yunlbd.flexboot4.service.ops.SysVersionLogService;
import com.yunlbd.flexboot4.vo.ops.SysVersionLogDetailVO;
import com.yunlbd.flexboot4.vo.ops.SysVersionLogListVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/version-log")
@Tag(name = "版本日志", description = "SysVersionLog - 系统版本更新日志")
@ApiTagGroup(group = "系统管理")
public class SysVersionLogController extends EntityCrudController<SysVersionLogService, SysVersionLog, String,
        SysVersionLogCreateReq, SysVersionLogUpdateReq, SysVersionLogListVO, SysVersionLogDetailVO> {

    public SysVersionLogController(SysVersionLogService service) {
        super(service, SysVersionLog.class, SysVersionLogListVO.class, SysVersionLogDetailVO.class);
    }


    @Override
    public Class<SysVersionLog> getEntityClass() {
        return SysVersionLog.class;
    }
}
