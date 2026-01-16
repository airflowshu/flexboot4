package com.yunlbd.flexboot4.controller;

import com.yunlbd.flexboot4.service.SysDeptService;
import org.springframework.web.bind.annotation.RequestMapping;
import com.yunlbd.flexboot4.entity.SysDept;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部门表 控制层。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@RestController
@RequestMapping("/api/dept")
public class SysDeptController extends BaseController<SysDeptService, SysDept, String>  {

    @Override
    public Class<SysDept> getEntityClass() {
        return SysDept.class;
    }
}
