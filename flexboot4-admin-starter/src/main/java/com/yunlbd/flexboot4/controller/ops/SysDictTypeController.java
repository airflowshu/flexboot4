package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.ops.SysDictTypeCreateReq;
import com.yunlbd.flexboot4.dto.ops.SysDictTypeUpdateReq;
import com.yunlbd.flexboot4.entity.ops.SysDictType;
import com.yunlbd.flexboot4.service.ops.SysDictTypeService;
import com.yunlbd.flexboot4.vo.ops.SysDictTypeDetailVO;
import com.yunlbd.flexboot4.vo.ops.SysDictTypeListVO;
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
@RequestMapping("/api/admin/dict-type")
@Tag(name = "字典管理", description = "SysDictType - 字典类型管理")
@ApiTagGroup(group = "系统管理")
public class SysDictTypeController extends EntityCrudController<SysDictTypeService, SysDictType, String,
        SysDictTypeCreateReq, SysDictTypeUpdateReq, SysDictTypeListVO, SysDictTypeDetailVO> {

    public SysDictTypeController(SysDictTypeService service) {
        super(service, SysDictType.class, SysDictTypeListVO.class, SysDictTypeDetailVO.class);
    }


    @Override
    public Class<SysDictType> getEntityClass() {
        return SysDictType.class;
    }
}
