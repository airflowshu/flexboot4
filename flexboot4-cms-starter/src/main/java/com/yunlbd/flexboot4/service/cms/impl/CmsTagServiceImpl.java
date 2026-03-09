package com.yunlbd.flexboot4.service.cms.impl;

import com.yunlbd.flexboot4.entity.cms.CmsTag;
import com.yunlbd.flexboot4.mapper.CmsTagMapper;
import com.yunlbd.flexboot4.service.cms.CmsTagService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "cmsTag")
public class CmsTagServiceImpl extends BaseServiceImpl<CmsTagMapper, CmsTag> implements CmsTagService {
}

