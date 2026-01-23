package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.AiApiKey;
import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.entity.table.AiApiKeyTableDef;
import com.yunlbd.flexboot4.entity.table.SysUserTableDef;
import com.yunlbd.flexboot4.mapper.AiApiKeyMapper;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
import com.yunlbd.flexboot4.service.AiApiKeyService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理 API Key 服务层实现。
 *
 * @author Wangts
 * @since 1.0.0
 */
@Service
@CacheConfig(cacheNames = "aiApiKey")
public class AiApiKeyServiceImpl extends BaseServiceImpl<AiApiKeyMapper, AiApiKey> implements AiApiKeyService {

    private final SysUserMapper sysUserMapper;

    public AiApiKeyServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<SysUser> selectOrphanedUsers() {
        // SELECT * FROM sys_user WHERE id NOT IN (SELECT user_id FROM ai_api_key WHERE user_id IS NOT NULL)
        SysUserTableDef sysUser = SysUserTableDef.SYS_USER;
        AiApiKeyTableDef aiApiKey = AiApiKeyTableDef.AI_API_KEY;

        QueryWrapper subQuery = QueryWrapper.create()
                .select(aiApiKey.USER_ID)
                .from(aiApiKey);

        return sysUserMapper.selectListByQuery(QueryWrapper.create()
                .where(sysUser.ID.notIn(subQuery)));
    }
}
