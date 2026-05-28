package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.ops.SysDictItemCreateReq;
import com.yunlbd.flexboot4.dto.ops.SysDictItemUpdateReq;
import com.yunlbd.flexboot4.entity.ops.SysDictItem;
import com.yunlbd.flexboot4.service.ops.SysDictItemService;
import com.yunlbd.flexboot4.vo.ops.SysDictItemDetailVO;
import com.yunlbd.flexboot4.vo.ops.SysDictItemListVO;
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
@RequestMapping("/api/admin/dict-item")
@Tag(name = "字典管理", description = "SysDictItem - 字典项管理")
@ApiTagGroup(group = "系统管理")
public class SysDictItemController extends EntityCrudController<SysDictItemService, SysDictItem, String,
        SysDictItemCreateReq, SysDictItemUpdateReq, SysDictItemListVO, SysDictItemDetailVO> {

    public SysDictItemController(SysDictItemService service) {
        super(service, SysDictItem.class, SysDictItemListVO.class, SysDictItemDetailVO.class);
    }


    @Override
    public Class<SysDictItem> getEntityClass() {
        return SysDictItem.class;
    }
}
