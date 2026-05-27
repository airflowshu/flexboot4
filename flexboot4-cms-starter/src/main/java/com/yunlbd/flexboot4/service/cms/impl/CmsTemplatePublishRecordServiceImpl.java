package com.yunlbd.flexboot4.service.cms.impl;

import com.yunlbd.flexboot4.entity.cms.CmsTemplatePublishRecord;
import com.yunlbd.flexboot4.mapper.CmsTemplatePublishRecordMapper;
import com.yunlbd.flexboot4.service.cms.CmsTemplatePublishRecordService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "cmsTemplatePublishRecord")
public class CmsTemplatePublishRecordServiceImpl
        extends BaseServiceImpl<CmsTemplatePublishRecordMapper, CmsTemplatePublishRecord>
        implements CmsTemplatePublishRecordService {
}
