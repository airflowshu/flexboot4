package com.yunlbd.flexboot4.service.cms.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.relation.RelationManager;
import com.mybatisflex.core.update.UpdateChain;
import com.yunlbd.flexboot4.auth.CurrentUserProvider;
import com.yunlbd.flexboot4.common.annotation.BumpTableVersion;
import com.yunlbd.flexboot4.config.CmsRenderProperties;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.cms.ArticleStatusEnum;
import com.yunlbd.flexboot4.entity.cms.CmsArticle;
import com.yunlbd.flexboot4.entity.cms.table.CmsArticleTableDef;
import com.yunlbd.flexboot4.mapper.CmsArticleMapper;
import com.yunlbd.flexboot4.query.DefaultQueryWrapperBuilder;
import com.yunlbd.flexboot4.query.SearchDtoUtils;
import com.yunlbd.flexboot4.service.cms.CmsArticleService;
import com.yunlbd.flexboot4.service.cms.CmsContentSanitizer;
import com.yunlbd.flexboot4.service.cms.CmsTemplateRenderService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@CacheConfig(cacheNames = "cmsArticle")
public class CmsArticleServiceImpl extends BaseServiceImpl<CmsArticleMapper, CmsArticle> implements CmsArticleService {

    private final CmsTemplateRenderService templateRenderService;
    private final CmsContentSanitizer cmsContentSanitizer;
    private final CmsRenderProperties cmsRenderProperties;
    private final CurrentUserProvider currentUserProvider;

    public CmsArticleServiceImpl(CmsTemplateRenderService templateRenderService,
                                 CmsContentSanitizer cmsContentSanitizer,
                                 CmsRenderProperties cmsRenderProperties,
                                 CurrentUserProvider currentUserProvider) {
        this.templateRenderService = templateRenderService;
        this.cmsContentSanitizer = cmsContentSanitizer;
        this.cmsRenderProperties = cmsRenderProperties;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Page<CmsArticle> pageWithPermissionFilter(SearchDto searchDto) {
        // 检查用户是否有审核权限（管理员）
        List<String> permissionCodes = currentUserProvider.getPermissionCodes();
        boolean isAdmin = permissionCodes != null && permissionCodes.contains("cms:article:review");

        Page<CmsArticle> page = new Page<>(searchDto.getPageNumber(), searchDto.getPageSize());

        // 构建查询条件
        QueryWrapper queryWrapper = DefaultQueryWrapperBuilder.get().build(searchDto, CmsArticle.class);

        // 如果不是管理员，只查询自己创建的文章
        if (!isAdmin) {
            String currentUsername = Objects.requireNonNull(currentUserProvider.getUsername(), "current username is required");
            queryWrapper.and(CmsArticle::getCreateBy).eq(currentUsername);
        }

        Page<CmsArticle> result = cacheProxy().page(page, queryWrapper);

        // 加载关系数据
        if (SearchDtoUtils.hasRelationPaths(searchDto, CmsArticle.class)) {
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
        CmsArticle article = cacheProxy().getById(articleId);
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
        return cacheProxy().updateById(update, true);
    }

    @Override
    @CacheEvict(allEntries = true)
    public boolean approveArticle(String articleId, String reviewComment) {
        CmsArticle article = cacheProxy().getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!ArticleStatusEnum.PENDING.name().equals(article.getStatus())) {
            throw new IllegalStateException("只有待审核的文章可以审核通过");
        }

        String reviewerId = currentUserProvider.getUserId();
        LocalDateTime now = LocalDateTime.now();

        CmsArticle update = CmsArticle.builder()
                .id(articleId)
                .status(ArticleStatusEnum.PUBLISHED.name())
                .reviewerId(reviewerId)
                .reviewTime(now)
                .reviewComment(reviewComment)
                .publishTime(now)
                .build();
        boolean updated = cacheProxy().updateById(update, true);
        if (updated && cmsRenderProperties.isAutoGenerateOnApprove()) {
            renderPublishedPage(articleId);
        }
        return updated;
    }

    @Override
    @CacheEvict(allEntries = true)
    public boolean rejectArticle(String articleId, String reviewComment) {
        CmsArticle article = cacheProxy().getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!ArticleStatusEnum.PENDING.name().equals(article.getStatus())) {
            throw new IllegalStateException("只有待审核的文章可以驳回");
        }

        String reviewerId = currentUserProvider.getUserId();
        LocalDateTime now = LocalDateTime.now();

        CmsArticle update = CmsArticle.builder()
                .id(articleId)
                .status(ArticleStatusEnum.REJECTED.name())
                .reviewerId(reviewerId)
                .reviewTime(now)
                .reviewComment(reviewComment)
                .build();
        return cacheProxy().updateById(update, true);
    }

    @Override
    @BumpTableVersion(CmsArticle.class)
    public boolean incrementViewCount(String articleId) {
        CmsArticleTableDef article = CmsArticleTableDef.CMS_ARTICLE;
        return UpdateChain.of(getMapper())
                .setRaw(article.VIEW_COUNT, "coalesce(view_count, 0) + 1", true)
                .where(article.ID.eq(articleId))
                .and(article.DEL_FLAG.eq(0))
                .update();
    }

    @Override
    @CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
    public boolean save(@NonNull CmsArticle entity) {
        cmsContentSanitizer.sanitizeForPersistence(entity);
        return super.save(entity);
    }

    @Override
    @CacheEvict(key = "#entity.id", cacheResolver = "dynamicCacheResolver")
    public boolean updateById(@NonNull CmsArticle entity, boolean ignoreNulls) {
        cmsContentSanitizer.sanitizeForPersistence(entity);
        return super.updateById(entity, ignoreNulls);
    }

    @Override
    public String renderPreviewPage(String articleId) {
        CmsArticle article = cacheProxy().getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        return templateRenderService.renderArticle(article).relativeUrl();
    }

    @Override
    public String renderPublishedPage(String articleId) {
        CmsArticle article = cacheProxy().getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!ArticleStatusEnum.PUBLISHED.name().equals(article.getStatus())) {
            throw new IllegalStateException("只有已发布文章可以生成发布页");
        }
        return templateRenderService.renderArticle(article).relativeUrl();
    }
}
