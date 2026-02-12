package com.yunlbd.flexboot4.service.sys.impl;

import com.yunlbd.flexboot4.entity.sys.SysDept;
import com.yunlbd.flexboot4.mapper.SysDeptMapper;
import com.yunlbd.flexboot4.service.sys.SysDeptService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

/**
 * 部门表 服务层实现。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@Service
@CacheConfig(cacheNames = "sysDept")
public class SysDeptServiceImpl extends BaseServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

}
