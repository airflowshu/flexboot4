package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.converter.sys.SysRoleCrudMapper;
import com.yunlbd.flexboot4.dto.sys.SysRoleCreateReq;
import com.yunlbd.flexboot4.dto.sys.SysRoleUpdateReq;
import com.yunlbd.flexboot4.entity.sys.SysRole;
import com.yunlbd.flexboot4.excel.sys.SysRoleExportRow;
import com.yunlbd.flexboot4.excel.sys.SysRoleImportRow;
import com.yunlbd.flexboot4.service.sys.SysRoleService;
import com.yunlbd.flexboot4.vo.sys.SysRoleDetailVO;
import com.yunlbd.flexboot4.vo.sys.SysRoleListVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门表 控制层。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@RestController
@RequestMapping("/api/admin/role")
@Tag(name = "角色管理", description = "SysRole - 角色管理")
@ApiTagGroup(group = "系统管理")
public class SysRoleController extends BaseCrudController<SysRoleService, SysRole, String,
        SysRoleCreateReq, SysRoleUpdateReq, SysRoleListVO, SysRoleDetailVO>  {

    private final SysRoleCrudMapper mapper;

    public SysRoleController(SysRoleService service, SysRoleCrudMapper mapper) {
        super(service, mapper);
        this.mapper = mapper;
    }


    @Override
    public Class<SysRole> getEntityClass() {
        return SysRole.class;
    }

    @Override
    protected CrudFieldPolicy fieldPolicy() {
        return CrudFieldPolicy.same(List.of(
                "id", "roleName", "roleValue", "status", "description",
                "orderNo", "remark", "createTime", "lastModifyTime"
        )).withQueryFields("menus.name", "menus.authCode");
    }

    @Override
    protected CrudExcelSupport<SysRole, ?, ?> excelSupport() {
        return CrudExcelSupport.of(SysRoleExportRow.class, SysRoleImportRow.class, mapper::toExportRow, null);
    }
}
