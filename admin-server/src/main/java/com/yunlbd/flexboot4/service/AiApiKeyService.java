package com.yunlbd.flexboot4.service;

import com.yunlbd.flexboot4.entity.AiApiKey;
import com.yunlbd.flexboot4.entity.SysUser;

import java.util.List;

/**
 * 管理 API Key 服务层。
 *
 * @author Wangts
 * @since 1.0.0
 */
public interface AiApiKeyService extends IExtendedService<AiApiKey> {

    boolean removeById(String id);

    /**
     * 查询 user_id 不在 sys_user 表中的 用户集合
     *
     * @return 用户数据
     */
    List<SysUser> selectOrphanedUsers();
}
