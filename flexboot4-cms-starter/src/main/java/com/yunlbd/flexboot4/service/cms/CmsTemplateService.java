package com.yunlbd.flexboot4.service.cms;

import com.mybatisflex.core.paginate.Page;
import com.yunlbd.flexboot4.entity.cms.CmsTemplatePublishRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface CmsTemplateService {

    List<TemplateTreeNode> getTemplateTree();

    TemplateFileDetail getTemplateFile(String relativePath);

    boolean saveTemplateFile(String relativePath, String content);

    PublishResult publishCurrentTemplate();

    Page<CmsTemplatePublishRecord> pagePublishHistory(int pageNumber, int pageSize);

    record TemplateTreeNode(String name, String path, boolean directory, List<TemplateTreeNode> children) {
    }

    record TemplateFileDetail(
            String path,
            String name,
            String content,
            String previewContent,
            long size,
            LocalDateTime lastModifiedTime,
            String assetBaseUrl
    ) {
    }

    record PublishResult(
            String recordId,
            String publishName,
            String status,
            String publishDir,
            String indexRelativeUrl,
            String indexUrl,
            String zipRelativeUrl,
            String zipUrl,
            int fileCount,
            String errorMessage
    ) {
    }
}
