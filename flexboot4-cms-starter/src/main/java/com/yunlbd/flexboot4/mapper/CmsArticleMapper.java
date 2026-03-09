package com.yunlbd.flexboot4.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yunlbd.flexboot4.entity.cms.CmsArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CmsArticleMapper extends BaseMapper<CmsArticle> {

    /**
     * 增加文章浏览量
     */
    @Update("UPDATE cms_article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") String id);
}

