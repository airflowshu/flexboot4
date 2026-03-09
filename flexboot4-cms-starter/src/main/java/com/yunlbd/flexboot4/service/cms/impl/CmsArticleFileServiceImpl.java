package com.yunlbd.flexboot4.service.cms.impl;

import com.yunlbd.flexboot4.entity.cms.CmsArticleFile;
import com.yunlbd.flexboot4.mapper.CmsArticleFileMapper;
import com.yunlbd.flexboot4.service.cms.CmsArticleFileService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "cmsArticleFile")
public class CmsArticleFileServiceImpl extends BaseServiceImpl<CmsArticleFileMapper, CmsArticleFile> implements CmsArticleFileService {
}

