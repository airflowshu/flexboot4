package com.yunlbd.flexboot4.service.cms.impl;

import com.yunlbd.flexboot4.entity.cms.CmsArticle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CmsContentSanitizerImplTest {

    private final CmsContentSanitizerImpl sanitizer = new CmsContentSanitizerImpl();

    @Test
    void shouldRemoveScriptAndMaskSensitiveText() {
        CmsArticle article = CmsArticle.builder()
                .title("<script>alert(1)</script>测试标题")
                .summary("联系邮箱 test.user@example.com")
                .content("<p>手机号 13812345678</p><script>alert('xss')</script>")
                .build();

        sanitizer.sanitizeForPersistence(article);

        Assertions.assertFalse(article.getTitle().contains("script"));
        Assertions.assertTrue(article.getSummary().contains("t****@example.com"));
        Assertions.assertFalse(article.getContent().contains("<script>"));
        Assertions.assertTrue(article.getContent().contains("138****5678"));
    }
}

