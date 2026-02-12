package com.yunlbd.flexboot4.service.kb.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.kb.KbMember;
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
    private final SysKbMemberMapper sysKbMemberMapper;

    @Override
    public List<KbMember> listByKbId(String kbId) {
        QueryWrapper qw = QueryWrapper.create()
                .from(KbMember.class)
                .where(KbMember::getKbId).eq(kbId);
        return super.list(qw);
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
            KbMember existing = sysKbMemberMapper.selectByKbAndUser(kbId, userId);
            if (existing == null) {
                ok = ok && this.save(KbMember.builder().kbId(kbId).userId(userId).build());
                continue;
            }
            if (existing.getDelFlag() != null && existing.getDelFlag() != 0) {
                ok = ok && sysKbMemberMapper.restoreById(existing.getId()) > 0;
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
        return this.remove(qw);
    }
}

