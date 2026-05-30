package com.yunlbd.flexboot4.service.kb.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.yunlbd.flexboot4.common.annotation.BumpTableVersion;
import com.yunlbd.flexboot4.entity.kb.KbMember;
import com.yunlbd.flexboot4.entity.kb.table.KbMemberTableDef;
import com.yunlbd.flexboot4.mapper.SysKbMemberMapper;
import com.yunlbd.flexboot4.service.kb.KbMemberService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@CacheConfig(cacheNames = "kbMember")
@RequiredArgsConstructor
public class KbMemberServiceImpl extends BaseServiceImpl<SysKbMemberMapper, KbMember> implements KbMemberService {

    @Override
    public List<KbMember> listByKbId(String kbId) {
        QueryWrapper qw = QueryWrapper.create()
                .from(KbMember.class)
                .where(KbMember::getKbId).eq(kbId);
        return cacheProxy().list(qw);
    }

    @Override
    public boolean addMembers(String kbId, Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return true;
        }
        boolean ok = true;
        for (String userId : userIds) {
            if (userId == null || userId.isBlank()) {
                continue;
            }
            KbMember existing = findAnyMember(kbId, userId);
            if (existing == null) {
                ok = ok && cacheProxy().save(KbMember.builder().kbId(kbId).userId(userId).build());
                continue;
            }
            if (existing.getDelFlag() != null && existing.getDelFlag() != 0) {
                ok = ok && serviceProxy(KbMemberService.class).restoreMemberById(existing.getId());
            }
        }
        return ok;
    }

    @Override
    public boolean removeMembers(String kbId, Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return true;
        }
        QueryWrapper qw = QueryWrapper.create()
                .from(KbMember.class)
                .where(KbMember::getKbId).eq(kbId)
                .and(KbMember::getUserId).in(userIds);
        return cacheProxy().remove(qw);
    }

    private KbMember findAnyMember(String kbId, String userId) {
        KbMemberTableDef member = KbMemberTableDef.KB_MEMBER;
        return getMapper().selectOneByQuery(QueryWrapper.create()
                .select(member.ALL_COLUMNS)
                .from(member)
                .where(member.KB_ID.eq(kbId))
                .and(member.USER_ID.eq(userId))
                .limit(1));
    }

    @Override
    @BumpTableVersion(KbMember.class)
    public boolean restoreMemberById(String id) {
        KbMemberTableDef member = KbMemberTableDef.KB_MEMBER;
        return UpdateChain.of(getMapper())
                .set(member.DEL_FLAG, 0, true)
                .where(member.ID.eq(id))
                .update();
    }
}

