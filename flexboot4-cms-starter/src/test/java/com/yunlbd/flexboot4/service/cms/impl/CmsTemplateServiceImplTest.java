package com.yunlbd.flexboot4.service.cms.impl;

import com.yunlbd.flexboot4.config.CmsRenderProperties;
import com.yunlbd.flexboot4.config.CmsTemplateProperties;
import com.yunlbd.flexboot4.entity.cms.CmsTemplatePublishRecord;
import com.yunlbd.flexboot4.service.cms.CmsTemplatePublishRecordService;
import com.yunlbd.flexboot4.service.cms.CmsTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CmsTemplateServiceImplTest {

    @TempDir
    Path tempDir;

    private Path templateRootDir;
    private Path outputRootDir;
    private Path publishRootDir;
    private CmsTemplateServiceImpl cmsTemplateService;
    private CmsTemplatePublishRecordService publishRecordService;

    @BeforeEach
    void setUp() throws Exception {
        templateRootDir = tempDir.resolve("webapp/html/web");
        outputRootDir = tempDir.resolve("cms-output");
        publishRootDir = outputRootDir.resolve("site-published");

        Files.createDirectories(templateRootDir);
        Files.createDirectories(outputRootDir);

        CmsTemplateProperties cmsTemplateProperties = new CmsTemplateProperties();
        cmsTemplateProperties.setRootDir(templateRootDir.toString());
        cmsTemplateProperties.setAssetBaseUrl("https://assets.example.com");
        cmsTemplateProperties.setPublishDir(publishRootDir.toString());

        CmsRenderProperties cmsRenderProperties = new CmsRenderProperties();
        cmsRenderProperties.setOutputDir(outputRootDir.toString());
        cmsRenderProperties.setUrlPrefix("/static/cms-pages");

        publishRecordService = Mockito.mock(CmsTemplatePublishRecordService.class);
        when(publishRecordService.save(any())).thenAnswer(invocation -> {
            CmsTemplatePublishRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId("publish-record-id");
            }
            return true;
        });

        cmsTemplateService = new CmsTemplateServiceImpl(cmsTemplateProperties, cmsRenderProperties, publishRecordService);
    }

    @Test
    void shouldBuildTemplateTreeAndRewritePreviewContent() throws Exception {
        Files.writeString(templateRootDir.resolve("index.html"), """
                <!DOCTYPE html>
                <html>
                <head>
                    <link rel="stylesheet" href="/template/1/default/css/style.css">
                </head>
                <body>
                    <a href="/about/index.html">关于我们</a>
                    <img src="http://localhost:8080/uploads/cms/demo.png" alt="">
                </body>
                </html>
                """, StandardCharsets.UTF_8);
        Files.createDirectories(templateRootDir.resolve("about"));
        Files.writeString(templateRootDir.resolve("about/index.html"), "<html><body>about</body></html>", StandardCharsets.UTF_8);

        List<CmsTemplateService.TemplateTreeNode> tree = cmsTemplateService.getTemplateTree();
        assertEquals(2, tree.size());
        assertEquals("about", tree.get(0).name());
        assertTrue(tree.get(0).directory());
        assertEquals("index.html", tree.get(1).path());

        CmsTemplateService.TemplateFileDetail detail = cmsTemplateService.getTemplateFile("index.html");
        assertTrue(detail.content().contains("/template/1/default/css/style.css"));
        assertTrue(detail.previewContent().contains("https://assets.example.com/template/1/default/css/style.css"));
        assertTrue(detail.previewContent().contains("https://assets.example.com/about/index.html"));
        assertTrue(detail.previewContent().contains("https://assets.example.com/uploads/cms/demo.png"));
        assertEquals("https://assets.example.com", detail.assetBaseUrl());
    }

    @Test
    void shouldSaveHtmlFileAndRejectIllegalPath() throws Exception {
        Files.writeString(templateRootDir.resolve("index.html"), "<html><body>old</body></html>", StandardCharsets.UTF_8);
        Files.writeString(templateRootDir.resolve("note.txt"), "plain text", StandardCharsets.UTF_8);

        assertTrue(cmsTemplateService.saveTemplateFile("index.html", "<html><body>new</body></html>"));
        assertTrue(Files.readString(templateRootDir.resolve("index.html"), StandardCharsets.UTF_8).contains("new"));

        assertThrows(IllegalArgumentException.class, () -> cmsTemplateService.getTemplateFile("../secret.html"));
        assertThrows(IllegalArgumentException.class, () -> cmsTemplateService.getTemplateFile("note.txt"));
    }

    @Test
    void shouldPublishCurrentTemplateDirectoryAndCreateZip() throws Exception {
        Files.writeString(templateRootDir.resolve("index.html"), "<html><body>home</body></html>", StandardCharsets.UTF_8);
        Files.createDirectories(templateRootDir.resolve("about"));
        Files.writeString(templateRootDir.resolve("about/index.html"), "<html><body>about</body></html>", StandardCharsets.UTF_8);

        CmsTemplateService.PublishResult result = cmsTemplateService.publishCurrentTemplate();

        Path publishedIndex = publishRootDir.resolve("current/index.html");
        Path publishedAbout = publishRootDir.resolve("current/about/index.html");
        Path packageDir = outputRootDir.resolve("site-packages");

        assertTrue(Files.exists(publishedIndex));
        assertTrue(Files.exists(publishedAbout));
        assertNotNull(result.zipRelativeUrl());
        assertTrue(result.zipRelativeUrl().startsWith("/static/cms-pages/site-packages/"));
        assertEquals("/static/cms-pages/site-published/current/index.html", result.indexRelativeUrl());
        assertEquals(2, result.fileCount());
        assertTrue(Files.list(packageDir).anyMatch(path -> path.getFileName().toString().endsWith(".zip")));

        ArgumentCaptor<CmsTemplatePublishRecord> captor = ArgumentCaptor.forClass(CmsTemplatePublishRecord.class);
        verify(publishRecordService, atLeastOnce()).save(captor.capture());
        CmsTemplatePublishRecord saved = captor.getValue();
        assertEquals("SUCCESS", saved.getStatus());
        assertEquals(2, saved.getFileCount());
        assertFalse(saved.getZipFilePath().isBlank());
    }
}
