package com.yunlbd.flexboot4.controller;

import com.yunlbd.flexboot4.entity.SysDictItem;
import com.yunlbd.flexboot4.service.SysDictItemService;
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
@RequestMapping("/api/dict-item")
@RequiredArgsConstructor
public class SysDictItemController extends BaseController<SysDictItemService, SysDictItem, String>  {

    @Override
    public Class<SysDictItem> getEntityClass() {
        return SysDictItem.class;
    }
}
