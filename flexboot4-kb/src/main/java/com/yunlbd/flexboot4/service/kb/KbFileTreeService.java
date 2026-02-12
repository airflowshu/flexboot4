package com.yunlbd.flexboot4.service.kb;

import com.yunlbd.flexboot4.entity.kb.KbFileTree;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.util.List;

public interface KbFileTreeService extends IExtendedService<KbFileTree> {

    /**
     * 获取某个目录下的子节点
     */
    List<KbFileTree> fsList(String kbId, String parentId);

    /**
     * 添加文件节点到目录
     */
    boolean addFile(String kbId, String parentId, String fileId);

    /**
     * 创建目录
     */
    boolean createFolder(String kbId, String parentId, String name);

    /**
     * 删除节点
     */
    boolean deleteNode(String id);

    /**
     * 移动节点到目标目录
     */
    boolean move(String kbId, List<String> nodeIds, String targetParentId);

    /**
     * 重命名节点
     */
    boolean rename(String id, String newName);
}
