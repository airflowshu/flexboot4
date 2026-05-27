package com.yunlbd.flexboot4.service.cms.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.config.CmsRenderProperties;
import com.yunlbd.flexboot4.config.CmsTemplateProperties;
import com.yunlbd.flexboot4.entity.cms.CmsTemplatePublishRecord;
import com.yunlbd.flexboot4.service.cms.CmsTemplatePublishRecordService;
import com.yunlbd.flexboot4.service.cms.CmsTemplateService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class CmsTemplateServiceImpl implements CmsTemplateService {

    private static final DateTimeFormatter PUBLISH_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String LOCAL_UPLOAD_PREFIX = "http://localhost:8080/uploads/";

    private final CmsTemplateProperties cmsTemplateProperties;
    private final CmsRenderProperties cmsRenderProperties;
    private final CmsTemplatePublishRecordService publishRecordService;

    @Override
    public List<TemplateTreeNode> getTemplateTree() {
        Path root = templateRootDir();
        ensureDirectory(root, "模板根目录不存在");
        return listTree(root, root);
    }

    @Override
    public TemplateFileDetail getTemplateFile(String relativePath) {
        Path file = resolveTemplateFile(relativePath, true);
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            return new TemplateFileDetail(
                    toRelativePath(templateRootDir(), file),
                    file.getFileName().toString(),
                    content,
                    buildPreviewHtml(content),
                    Files.size(file),
                    LocalDateTime.ofInstant(Files.getLastModifiedTime(file).toInstant(), java.time.ZoneId.systemDefault()),
                    normalizeAssetBaseUrl(cmsTemplateProperties.getAssetBaseUrl())
            );
        } catch (IOException e) {
            throw new IllegalStateException("读取模板文件失败: " + relativePath, e);
        }
    }

    @Override
    public boolean saveTemplateFile(String relativePath, String content) {
        Path file = resolveTemplateFile(relativePath, true);
        try {
            Files.writeString(file, Objects.requireNonNullElse(content, ""), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("保存模板文件失败: " + relativePath, e);
        }
    }

    @Override
    public PublishResult publishCurrentTemplate() {
        Path root = templateRootDir();
        ensureDirectory(root, "模板根目录不存在");

        Path publishRoot = publishRootDir();
        Path publishCurrentDir = publishRoot.resolve("current");
        Path outputRoot = cmsOutputRootDir();
        String publishName = "site-publish-" + LocalDateTime.now().format(PUBLISH_NAME_FORMATTER);
        Path packageDir = resolvePackageDir(publishRoot);
        Path zipFile = packageDir.resolve(publishName + ".zip");

        CmsTemplatePublishRecord record = CmsTemplatePublishRecord.builder()
                .publishName(publishName)
                .templateRootDir(root.toString())
                .publishDir(publishCurrentDir.toString())
                .zipFilePath(zipFile.toString())
                .status("FAILED")
                .fileCount(0)
                .build();

        try {
            Files.createDirectories(outputRoot);
            Files.createDirectories(publishRoot);
            Files.createDirectories(packageDir);
            deleteIfExists(publishCurrentDir);
            int fileCount = copyDirectory(root, publishCurrentDir);
            zipDirectory(publishCurrentDir, zipFile);

            String indexRelativeUrl = Files.exists(publishCurrentDir.resolve("index.html"))
                    ? toStaticUrlIfPossible(outputRoot, publishCurrentDir.resolve("index.html"))
                    : null;
            String zipRelativeUrl = toStaticUrlIfPossible(outputRoot, zipFile);

            record.setStatus("SUCCESS");
            record.setFileCount(fileCount);
            record.setIndexRelativeUrl(indexRelativeUrl);
            record.setZipRelativeUrl(zipRelativeUrl);
            publishRecordService.save(record);

            return new PublishResult(
                    record.getId(),
                    publishName,
                    record.getStatus(),
                    publishCurrentDir.toString(),
                    indexRelativeUrl,
                    indexRelativeUrl,
                    zipRelativeUrl,
                    zipRelativeUrl,
                    fileCount,
                    null
            );
        } catch (Exception e) {
            deleteIfExists(publishCurrentDir);
            record.setErrorMessage(e.getMessage());
            publishRecordService.save(record);
            throw new IllegalStateException("模板发布失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<CmsTemplatePublishRecord> pagePublishHistory(int pageNumber, int pageSize) {
        Page<CmsTemplatePublishRecord> page = new Page<>(pageNumber, pageSize);
        QueryWrapper queryWrapper = QueryWrapper.create().orderBy("create_time", false);
        return publishRecordService.page(page, queryWrapper);
    }

    private List<TemplateTreeNode> listTree(Path currentDir, Path rootDir) {
        try {
            List<Path> paths = Files.list(currentDir)
                    .sorted(Comparator
                            .comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
            List<TemplateTreeNode> result = new ArrayList<>();
            for (Path path : paths) {
                boolean directory = Files.isDirectory(path);
                if (!directory && !isHtmlFile(path)) {
                    continue;
                }
                result.add(new TemplateTreeNode(
                        path.getFileName().toString(),
                        toRelativePath(rootDir, path),
                        directory,
                        directory ? listTree(path, rootDir) : List.of()
                ));
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("读取模板目录失败: " + currentDir, e);
        }
    }

    private String buildPreviewHtml(String html) {
        String assetBaseUrl = normalizeAssetBaseUrl(cmsTemplateProperties.getAssetBaseUrl());
        if (!StringUtils.hasText(html) || !StringUtils.hasText(assetBaseUrl)) {
            return html;
        }

        String normalizedHtml = html.replace(LOCAL_UPLOAD_PREFIX, assetBaseUrl + "/uploads/");
        Document document = Jsoup.parse(normalizedHtml);
        document.outputSettings().prettyPrint(false);
        if (document.head() != null) {
            document.head().select("base").remove();
            document.head().prependElement("base").attr("href", assetBaseUrl + "/");
        }

        rewriteAttr(document, "href", assetBaseUrl);
        rewriteAttr(document, "src", assetBaseUrl);
        rewriteAttr(document, "poster", assetBaseUrl);
        rewriteAttr(document, "data-src", assetBaseUrl);
        return document.outerHtml();
    }

    private void rewriteAttr(Document document, String attrName, String assetBaseUrl) {
        document.select("[" + attrName + "]").forEach(element -> {
            String value = element.attr(attrName);
            if (!StringUtils.hasText(value)) {
                return;
            }
            element.attr(attrName, rewriteUrl(value, assetBaseUrl));
        });
    }

    private String rewriteUrl(String value, String assetBaseUrl) {
        String trimmed = value.trim();
        if (!StringUtils.hasText(trimmed)) {
            return trimmed;
        }
        if (trimmed.startsWith(LOCAL_UPLOAD_PREFIX)) {
            return assetBaseUrl + trimmed.substring("http://localhost:8080".length());
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("//")
                || trimmed.startsWith("#") || trimmed.startsWith("javascript:") || trimmed.startsWith("mailto:")
                || trimmed.startsWith("tel:") || trimmed.startsWith("data:")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return assetBaseUrl + trimmed;
        }
        return trimmed;
    }

    private int copyDirectory(Path sourceRoot, Path targetRoot) {
        Counter counter = new Counter();
        try {
            Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relative = sourceRoot.relativize(dir);
                    Files.createDirectories(targetRoot.resolve(relative));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relative = sourceRoot.relativize(file);
                    Files.copy(file, targetRoot.resolve(relative), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    counter.increment();
                    return FileVisitResult.CONTINUE;
                }
            });
            return counter.value();
        } catch (IOException e) {
            throw new IllegalStateException("复制模板目录失败: " + sourceRoot, e);
        }
    }

    private void zipDirectory(Path sourceDir, Path zipFile) {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile), StandardCharsets.UTF_8)) {
            Files.walk(sourceDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        Path relative = sourceDir.relativize(path);
                        ZipEntry entry = new ZipEntry(relative.toString().replace('\\', '/'));
                        try {
                            zos.putNextEntry(entry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("打包模板文件失败: " + zipFile, e.getCause());
        } catch (IOException e) {
            throw new IllegalStateException("创建模板压缩包失败: " + zipFile, e);
        }
    }

    private void deleteIfExists(Path path) {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("删除目录失败: " + path, e);
        }
    }

    private Path resolveTemplateFile(String relativePath, boolean mustExist) {
        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("模板路径不能为空");
        }
        String normalized = relativePath.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("../") || normalized.contains("..\\")) {
            throw new IllegalArgumentException("非法模板路径: " + relativePath);
        }

        Path root = templateRootDir();
        Path resolved = root.resolve(normalized).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("模板路径超出允许范围: " + relativePath);
        }
        if (!isHtmlFile(resolved)) {
            throw new IllegalArgumentException("仅允许访问 html 模板文件: " + relativePath);
        }
        if (mustExist && Files.notExists(resolved)) {
            throw new IllegalArgumentException("模板文件不存在: " + relativePath);
        }
        return resolved;
    }

    private boolean isHtmlFile(Path path) {
        return path != null && path.getFileName() != null
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".html");
    }

    private Path templateRootDir() {
        return Path.of(cmsTemplateProperties.getRootDir()).toAbsolutePath().normalize();
    }

    private Path cmsOutputRootDir() {
        return Path.of(cmsRenderProperties.getOutputDir()).toAbsolutePath().normalize();
    }

    private Path publishRootDir() {
        return Path.of(cmsTemplateProperties.getPublishDir()).toAbsolutePath().normalize();
    }

    private Path resolvePackageDir(Path publishRoot) {
        Path parent = publishRoot.getParent();
        if (parent == null) {
            return publishRoot.resolve("site-packages");
        }
        return parent.resolve("site-packages");
    }

    private String toStaticUrlIfPossible(Path outputRoot, Path file) {
        Path normalizedOutputRoot = outputRoot.toAbsolutePath().normalize();
        Path normalizedFile = file.toAbsolutePath().normalize();
        if (!normalizedFile.startsWith(normalizedOutputRoot)) {
            return null;
        }
        String relative = normalizedOutputRoot.relativize(normalizedFile).toString().replace('\\', '/');
        return normalizePrefix(cmsRenderProperties.getUrlPrefix()) + "/" + relative;
    }

    private void ensureDirectory(Path dir, String message) {
        if (dir == null || Files.notExists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalStateException(message + ": " + dir);
        }
    }

    private String toRelativePath(Path root, Path path) {
        Path relative = root.relativize(path);
        String normalized = relative.toString().replace('\\', '/');
        return ".".equals(normalized) ? "" : normalized;
    }

    private String normalizeAssetBaseUrl(String assetBaseUrl) {
        if (!StringUtils.hasText(assetBaseUrl)) {
            return null;
        }
        String normalized = assetBaseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizePrefix(String prefix) {
        String normalized = StringUtils.hasText(prefix) ? prefix.trim() : "/static/cms-pages";
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static final class Counter {
        private int value;

        void increment() {
            value++;
        }

        int value() {
            return value;
        }
    }
}
