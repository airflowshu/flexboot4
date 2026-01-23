package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.entity.SysDictType;
import com.yunlbd.flexboot4.service.SysDictTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部门表 控制层。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@RestController
@RequestMapping("/api/dict-type")
@RequiredArgsConstructor
@Tag(name = "字典管理", description = "SysDictType - 字典类型管理")
public class SysDictTypeController extends BaseController<SysDictTypeService, SysDictType, String>  {

    @Override
    public Class<SysDictType> getEntityClass() {
        return SysDictType.class;
    }
}
