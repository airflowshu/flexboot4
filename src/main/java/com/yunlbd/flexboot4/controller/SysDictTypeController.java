package com.yunlbd.flexboot4.controller;

import com.yunlbd.flexboot4.entity.SysDictType;
import com.yunlbd.flexboot4.service.SysDictTypeService;
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
public class SysDictTypeController extends BaseController<SysDictTypeService, SysDictType, String>  {

    @Override
    public Class<SysDictType> getEntityClass() {
        return SysDictType.class;
    }
}
