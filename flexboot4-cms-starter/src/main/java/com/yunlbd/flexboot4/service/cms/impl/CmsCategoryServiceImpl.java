package com.yunlbd.flexboot4.service.cms.impl;

import com.yunlbd.flexboot4.entity.cms.CmsCategory;
import com.yunlbd.flexboot4.mapper.CmsCategoryMapper;
import com.yunlbd.flexboot4.service.cms.CmsCategoryService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "cmsCategory")
public class CmsCategoryServiceImpl extends BaseServiceImpl<CmsCategoryMapper, CmsCategory> implements CmsCategoryService {
}

