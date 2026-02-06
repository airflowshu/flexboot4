package com.yunlbd.flexboot4.service.kb.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.kb.KbFileTree;
import com.yunlbd.flexboot4.entity.kb.table.KbFileTreeTableDef;
import com.yunlbd.flexboot4.mapper.KbFileTreeMapper;
import com.yunlbd.flexboot4.service.kb.KbFileTreeService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@CacheConfig(cacheNames = "kbFileTree")
@RequiredArgsConstructor
public class KbFileTreeServiceImpl extends BaseServiceImpl<KbFileTreeMapper, KbFileTree> implements KbFileTreeService {

    private final KbFileTreeMapper kbFileTreeMapper;

    @Override
    public List<KbFileTree> fsList(String kbId, String parentId) {
        QueryWrapper qw = QueryWrapper.create()
                .from(KbFileTree.class)
                .where(KbFileTreeTableDef.KB_FILE_TREE.KB_ID.eq(kbId))
                .and(KbFileTreeTableDef.KB_FILE_TREE.DEL_FLAG.eq(0));
        if (parentId == null) {
            qw.and(KbFileTreeTableDef.KB_FILE_TREE.PARENT_ID.isNull());
        } else {
            qw.and(KbFileTreeTableDef.KB_FILE_TREE.PARENT_ID.eq(parentId));
        }
        qw.orderBy(KbFileTree::getSortOrder);
        return kbFileTreeMapper.selectListWithRelationsByQuery(qw);
    }

    @Override
    public boolean addFile(String kbId, String parentId, String fileId) {
        KbFileTree node = KbFileTree.builder()
                .kbId(kbId)
                .parentId(parentId)
                .fileId(fileId)
                .type("FILE")
                .sortOrder(0)
                .build();
        return this.save(node);
    }

    @Override
    public boolean createFolder(String kbId, String parentId, String name) {
        KbFileTree node = KbFileTree.builder()
                .kbId(kbId)
                .parentId(parentId)
                .name(name)
                .type("FOLDER")
                .sortOrder(0)
                .build();
        return this.save(node);
    }

    @Override
    public boolean deleteNode(String id) {
        KbFileTree node = super.getById(id);
        if (node == null) {
            return false;
        }
        if ("FOLDER".equals(node.getType())) {
            // 递归删除子节点
            deleteChildren(id);
        }
        return removeById(id);
        // node.setDelFlag(1);
        // return this.updateById(node, true);
    }

    private void deleteChildren(String parentId) {
        List<KbFileTree> children = super.list(QueryWrapper.create()
                .from(KbFileTree.class)
                .where(KbFileTree::getParentId).eq(parentId)
                .and(KbFileTree::getDelFlag).eq(0));
        for (KbFileTree child : children) {
            if ("FOLDER".equals(child.getType())) {
                deleteChildren(child.getId());
            }
            removeById(child.getId());
            // child.setDelFlag(1);
            // this.updateById(child, true);
        }
    }

    @Override
    public boolean move(String kbId, List<String> nodeIds, String targetParentId) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return true;
        }
        // 验证目标目录
        if (targetParentId != null && !targetParentId.isBlank()) {
            KbFileTree target = super.getById(targetParentId);
            if (target == null || target.getDelFlag() != null && target.getDelFlag() != 0) {
                throw new RuntimeException("目标目录不存在");
            }
            if (!kbId.equals(target.getKbId())) {
                throw new RuntimeException("目标目录不属于当前知识库");
            }
            if (!"FOLDER".equals(target.getType())) {
                throw new RuntimeException("目标必须是目录");
            }
            // 检查是否移动到自己的子目录
            for (String nodeId : nodeIds) {
                if (isDescendant(nodeId, targetParentId)) {
                    throw new RuntimeException("不能移动到自己的子目录");
                }
            }
        }

        boolean ok = true;
        for (String nodeId : nodeIds) {
            if (nodeId == null || nodeId.isBlank()) {
                continue;
            }
            KbFileTree node = super.getById(nodeId);
            if (node == null || node.getDelFlag() != null && node.getDelFlag() != 0) {
                continue;
            }
            if (!kbId.equals(node.getKbId())) {
                throw new RuntimeException("存在非当前知识库的节点，禁止移动");
            }
            node.setParentId(targetParentId);
            ok = ok && this.updateById(node, true);
        }
        return ok;
    }

    @Override
    public boolean rename(String id, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new RuntimeException("名称不能为空");
        }
        KbFileTree node = super.getById(id);
        if (node == null || node.getDelFlag() != null && node.getDelFlag() != 0) {
            throw new RuntimeException("节点不存在");
        }
        node.setName(newName.trim());
        return this.updateById(node, true);
    }

    private boolean isDescendant(String nodeId, String maybeParentId) {
        if (nodeId == null || nodeId.isBlank() || maybeParentId == null || maybeParentId.isBlank()) {
            return false;
        }
        String current = maybeParentId;
        for (int i = 0; i < 100; i++) {
            if (nodeId.equals(current)) {
                return true;
            }
            KbFileTree f = super.getById(current);
            if (f == null) {
                return false;
            }
            String p = f.getParentId();
            if (p == null || p.isBlank()) {
                return false;
            }
            current = p;
        }
        return true;
    }
}
