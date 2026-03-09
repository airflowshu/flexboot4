package com.yunlbd.flexboot4.service.cms;

import com.mybatisflex.core.paginate.Page;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.cms.CmsArticle;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

public interface CmsArticleService extends IExtendedService<CmsArticle> {

    /**
     * 分页查询文章列表（带权限过滤）
     * 根据用户角色进行数据权限过滤：
     * - 管理员(拥有cms:article:review权限)：查看所有文章
     * - 普通用户：只能查看自己创建的文章
     * @param searchDto 查询参数
     * @return 分页结果
     */
    Page<CmsArticle> pageWithPermissionFilter(SearchDto searchDto);

    /**
     * 提交文章审核
     * @param articleId 文章ID
     * @return 是否成功
     */
    boolean submitForReview(String articleId);

    /**
     * 审核通过文章
     * @param articleId 文章ID
     * @param reviewComment 审核意见
     * @return 是否成功
     */
    boolean approveArticle(String articleId, String reviewComment);

    /**
     * 驳回文章
     * @param articleId 文章ID
     * @param reviewComment 审核意见
     * @return 是否成功
     */
    boolean rejectArticle(String articleId, String reviewComment);

    /**
     * 增加文章浏览量
     * @param articleId 文章ID
     * @return 是否成功
     */
    boolean incrementViewCount(String articleId);
}

