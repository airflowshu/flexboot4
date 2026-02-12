package com.yunlbd.flexboot4.service.kb;

import com.yunlbd.flexboot4.entity.kb.KbMember;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.util.Collection;
import java.util.List;

public interface KbMemberService extends IExtendedService<KbMember> {

    /**
     * 根据知识库 ID 查询成员列表
     */
    List<KbMember> listByKbId(String kbId);

    /**
     * 添加知识库成员
     */
    boolean addMembers(String kbId, Collection<String> userIds);

    /**
     * 移除知识库成员
     */
    boolean removeMembers(String kbId, Collection<String> userIds);
}

