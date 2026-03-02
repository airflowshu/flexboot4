package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.ops.SysDictItem;
import com.yunlbd.flexboot4.service.ops.SysDictItemService;
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
@RequestMapping("/api/admin/dict-item")
@RequiredArgsConstructor
@Tag(name = "字典管理", description = "SysDictItem - 字典项管理")
@ApiTagGroup(group = "系统管理")
public class SysDictItemController extends BaseController<SysDictItemService, SysDictItem, String> {

    @Override
    public Class<SysDictItem> getEntityClass() {
        return SysDictItem.class;
    }
}
