package com.yunlbd.flexboot4.service.kb;

import com.mybatisflex.core.paginate.Page;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.kb.KnowledgeBase;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.util.Set;

public interface KnowledgeBaseService extends IExtendedService<KnowledgeBase> {

    /**
     * 创建知识库
     */
    boolean createKnowledgeBase(KnowledgeBase kb, String userId);

    /**
     * 更新知识库（验证 owner）
     */
    boolean updateKnowledgeBase(String id, KnowledgeBase kb, String userId);

    /**
     * 删除知识库（验证 owner）
     */
    boolean deleteKnowledgeBase(String id, String userId);

    /**
     * 获取用户可见的知识库 ID 集合
     */
    Set<String> getVisibleKbIds(String userId);

    /**
     * 可见知识库分页
     */
    Page<KnowledgeBase> pageVisible(SearchDto searchDto, String userId);

    /**
     * 判断用户是否是知识库的 owner
     */
    boolean isOwner(KnowledgeBase kb, String userId);

    /**
     * 验证用户是否有权限访问知识库
     */
    void checkVisible(String kbId, String userId);

    /**
     * 验证用户是否是知识库 owner
     */
    void checkOwner(String kbId, String userId);
}

