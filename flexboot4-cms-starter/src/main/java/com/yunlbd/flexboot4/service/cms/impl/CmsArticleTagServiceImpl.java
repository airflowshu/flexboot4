package com.yunlbd.flexboot4.service.cms.impl;

import com.yunlbd.flexboot4.entity.cms.CmsArticleTag;
import com.yunlbd.flexboot4.mapper.CmsArticleTagMapper;
import com.yunlbd.flexboot4.service.cms.CmsArticleTagService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "cmsArticleTag")
public class CmsArticleTagServiceImpl extends BaseServiceImpl<CmsArticleTagMapper, CmsArticleTag> implements CmsArticleTagService {
}

