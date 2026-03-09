package com.yunlbd.flexboot4.service.cms.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.cms.ArticleStatusEnum;
import com.yunlbd.flexboot4.entity.cms.CmsArticle;
import com.yunlbd.flexboot4.mapper.CmsArticleMapper;
import com.yunlbd.flexboot4.query.DefaultQueryWrapperBuilder;
import com.yunlbd.flexboot4.query.SearchDtoUtils;
import com.yunlbd.flexboot4.service.cms.CmsArticleService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import com.yunlbd.flexboot4.util.SecurityUtils;
import com.mybatisflex.core.relation.RelationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@CacheConfig(cacheNames = "cmsArticle")
public class CmsArticleServiceImpl extends BaseServiceImpl<CmsArticleMapper, CmsArticle> implements CmsArticleService {

    private final CmsArticleMapper articleMapper;

    public CmsArticleServiceImpl(CmsArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    @Cacheable(keyGenerator = "versionedQueryKeyGenerator", cacheResolver = "dynamicCacheResolver")
    public Page<CmsArticle> pageWithPermissionFilter(SearchDto searchDto) {
        // 检查用户是否有审核权限（管理员）
        List<String> permissionCodes = Objects.requireNonNull(SecurityUtils.getLoginUser()).getPermissionCodes();
        boolean isAdmin = permissionCodes != null && permissionCodes.contains("cms:article:review");

        Page<CmsArticle> page = new Page<>(searchDto.getPageNumber(), searchDto.getPageSize());

        // 构建查询条件
        QueryWrapper queryWrapper = DefaultQueryWrapperBuilder.get().build(searchDto, CmsArticle.class);

        // 如果不是管理员，只查询自己创建的文章
        if (!isAdmin) {
            String currentUsername = SecurityUtils.getLoginUser().getUsername();
            queryWrapper.and(CmsArticle::getCreateBy).eq(currentUsername);
        }

        Page<CmsArticle> result = super.page(page, queryWrapper);

        // 加载关系数据
        if (SearchDtoUtils.hasRelationPaths(searchDto)) {
            RelationManager.queryRelations(getMapper(), result.getRecords());
            SearchDtoUtils.filterRelationCollections(searchDto, CmsArticle.class, result.getRecords());
        } else {
            // 即使没有指定关系路径，也加载基本关系
            RelationManager.queryRelations(getMapper(), result.getRecords());
        }

        return result;
    }

    @Override
    @CacheEvict(allEntries = true)
    public boolean submitForReview(String articleId) {
        CmsArticle article = getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!ArticleStatusEnum.DRAFT.name().equals(article.getStatus())
                && !ArticleStatusEnum.REJECTED.name().equals(article.getStatus())) {
            throw new IllegalStateException("只有草稿或被驳回的文章可以提交审核");
        }

        CmsArticle update = CmsArticle.builder()
                .id(articleId)
                .status(ArticleStatusEnum.PENDING.name())
                .build();
        return updateById(update, true);
    }

    @Override
    @CacheEvict(allEntries = true)
    public boolean approveArticle(String articleId, String reviewComment) {
        CmsArticle article = getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!ArticleStatusEnum.PENDING.name().equals(article.getStatus())) {
            throw new IllegalStateException("只有待审核的文章可以审核通过");
        }

        String reviewerId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        CmsArticle update = CmsArticle.builder()
                .id(articleId)
                .status(ArticleStatusEnum.PUBLISHED.name())
                .reviewerId(reviewerId)
                .reviewTime(now)
                .reviewComment(reviewComment)
                .publishTime(now)
                .build();
        return updateById(update, true);
    }

    @Override
    @CacheEvict(allEntries = true)
    public boolean rejectArticle(String articleId, String reviewComment) {
        CmsArticle article = getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!ArticleStatusEnum.PENDING.name().equals(article.getStatus())) {
            throw new IllegalStateException("只有待审核的文章可以驳回");
        }

        String reviewerId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        CmsArticle update = CmsArticle.builder()
                .id(articleId)
                .status(ArticleStatusEnum.REJECTED.name())
                .reviewerId(reviewerId)
                .reviewTime(now)
                .reviewComment(reviewComment)
                .build();
        return updateById(update, true);
    }

    @Override
    public boolean incrementViewCount(String articleId) {
        return articleMapper.incrementViewCount(articleId) > 0;
    }
}

