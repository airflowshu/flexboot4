package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.dto.sys.SysDeptCreateReq;
import com.yunlbd.flexboot4.dto.sys.SysDeptUpdateReq;
import com.yunlbd.flexboot4.entity.sys.SysDept;
import com.yunlbd.flexboot4.service.sys.SysDeptService;
import com.yunlbd.flexboot4.vo.sys.SysDeptDetailVO;
import com.yunlbd.flexboot4.vo.sys.SysDeptListVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部门表 控制层。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@RestController
@RequestMapping("/api/admin/dept")
@Tag(name = "部门管理", description = "SysDept - 部门管理")
@ApiTagGroup(group = "系统管理")
public class SysDeptController extends EntityCrudController<SysDeptService, SysDept, String,
        SysDeptCreateReq, SysDeptUpdateReq, SysDeptListVO, SysDeptDetailVO>  {

    public SysDeptController(SysDeptService service) {
        super(service, SysDept.class, SysDeptListVO.class, SysDeptDetailVO.class);
    }


    @Override
    public Class<SysDept> getEntityClass() {
        return SysDept.class;
    }
}
