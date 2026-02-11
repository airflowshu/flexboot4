package com.yunlbd.flexboot4.service.kb.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.kb.KbMember;
import com.yunlbd.flexboot4.entity.kb.KnowledgeBase;
import com.yunlbd.flexboot4.entity.kb.table.KbMemberTableDef;
import com.yunlbd.flexboot4.entity.kb.table.KnowledgeBaseTableDef;
import com.yunlbd.flexboot4.mapper.KnowledgeBaseMapper;
import com.yunlbd.flexboot4.query.DefaultQueryWrapperBuilder;
import com.yunlbd.flexboot4.service.kb.KbMemberService;
import com.yunlbd.flexboot4.service.kb.KnowledgeBaseService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "knowledgeBase")
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl extends BaseServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    private final KbMemberService kbMemberService;

    @Override
    public boolean createKnowledgeBase(KnowledgeBase kb, String userId) {
        if (kb == null) {
            return false;
        }
        kb.setOwnerId(userId);
        if (kb.getType() == null || kb.getType().isBlank()) {
            kb.setType("private");
        }
        if (kb.getStatus() == null) {
            kb.setStatus(1);
        }
        boolean ok = this.save(kb);
        if (ok && "team".equalsIgnoreCase(kb.getType())) {
            KbMember member = KbMember.builder().kbId(kb.getId()).userId(userId).build();
            kbMemberService.save(member);
        }
        return ok;
    }

    @Override
    public boolean updateKnowledgeBase(String id, KnowledgeBase kb, String userId) {
        KnowledgeBase existing = super.getById(id);
        if (existing == null) {
            return false;
        }
        if (!isOwner(existing, userId)) {
            throw new RuntimeException("仅知识库创建者可执行该操作");
        }
        kb.setId(existing.getId());
        kb.setOwnerId(existing.getOwnerId());
        return this.updateById(kb, true);
    }

    @Override
    public boolean deleteKnowledgeBase(String id, String userId) {
        KnowledgeBase existing = super.getById(id);
        if (existing == null) {
            return false;
        }
        if (!isOwner(existing, userId)) {
            throw new RuntimeException("仅知识库创建者可执行该操作");
        }
        return this.removeById(id);
    }

    @Override
    public Set<String> getVisibleKbIds(String userId) {
        QueryWrapper memberQw = QueryWrapper.create()
                .select(KbMemberTableDef.KB_MEMBER.KB_ID)
                .from(KbMember.class);
        if (userId == null || userId.isBlank()) {
            memberQw.where("1=0");
        } else {
            memberQw.where(KbMember::getUserId).eq(userId);
        }
        List<String> memberKbIds = kbMemberService.listAs(memberQw, String.class);

        QueryWrapper kbIdQw = QueryWrapper.create()
                .select(KnowledgeBaseTableDef.KNOWLEDGE_BASE.ID)
                .from(KnowledgeBase.class)
                .where(KnowledgeBase::getType).eq("public");

        if (userId != null && !userId.isBlank()) {
            kbIdQw.or(KnowledgeBase::getOwnerId).eq(userId);
        }
        if (memberKbIds != null && !memberKbIds.isEmpty()) {
            kbIdQw.or(KnowledgeBase::getId).in(memberKbIds);
        }

        List<String> kbIds = super.listAs(kbIdQw, String.class);
        return kbIds == null ? Set.of() : kbIds.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());
    }

    @Override
    public Page<KnowledgeBase> pageVisible(SearchDto searchDto, String userId) {
        Set<String> visibleKbIds = getVisibleKbIds(userId);

        Page<KnowledgeBase> page = new Page<>(searchDto.getPageNumber(), searchDto.getPageSize());
        if (visibleKbIds.isEmpty()) {
            page.setTotalRow(0);
            page.setRecords(List.of());
            return page;
        }

        QueryWrapper qw = DefaultQueryWrapperBuilder.get().build(searchDto, KnowledgeBase.class);
        qw.and(KnowledgeBase::getId).in(visibleKbIds);
        return super.page(page, qw);
    }

    @Override
    public boolean isOwner(KnowledgeBase kb, String userId) {
        return kb != null && userId != null && userId.equals(kb.getOwnerId());
    }

    @Override
    public void checkVisible(String kbId, String userId) {
        KnowledgeBase kb = super.getById(kbId);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }
        if (isOwner(kb, userId)) {
            return;
        }
        String type = kb.getType();
        if ("public".equalsIgnoreCase(type)) {
            return;
        }
        if ("team".equalsIgnoreCase(type) && userId != null && !userId.isBlank()) {
            QueryWrapper qw = QueryWrapper.create()
                    .from(KbMember.class)
                    .where(KbMember::getKbId).eq(kbId)
                    .and(KbMember::getUserId).eq(userId);
            if (kbMemberService.count(qw) > 0) {
                return;
            }
        }
        throw new RuntimeException("无权限访问该知识库");
    }

    @Override
    public void checkOwner(String kbId, String userId) {
        KnowledgeBase kb = super.getById(kbId);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }
        if (!isOwner(kb, userId)) {
            throw new RuntimeException("仅知识库创建者可执行该操作");
        }
    }
}

