package com.yunlbd.flexboot4.service.kb.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.kb.KbFileTree;
import com.yunlbd.flexboot4.entity.kb.KnowledgeBase;
import com.yunlbd.flexboot4.entity.kb.SysFileChunk;
import com.yunlbd.flexboot4.entity.kb.SysFileParsed;
import com.yunlbd.flexboot4.entity.kb.table.KbFileTreeTableDef;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import com.yunlbd.flexboot4.file.FileLocation;
import com.yunlbd.flexboot4.file.FileStorage;
import com.yunlbd.flexboot4.file.StorageType;
import com.yunlbd.flexboot4.file.ai.AiParseStatus;
import com.yunlbd.flexboot4.file.ai.AiStatus;
import com.yunlbd.flexboot4.file.parse.FileParser;
import com.yunlbd.flexboot4.file.parse.ParsedDocument;
import com.yunlbd.flexboot4.file.parse.TokenEstimator;
import com.yunlbd.flexboot4.mapper.KbFileTreeMapper;
import com.yunlbd.flexboot4.service.kb.*;
import com.yunlbd.flexboot4.service.ops.SysConfigService;
import com.yunlbd.flexboot4.service.sys.SysFileService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

@Service
public class KnowledgeBaseIndexingServiceImpl implements KnowledgeBaseIndexingService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KbFileTreeMapper kbFileTreeMapper;
    private final SysFileService sysFileService;
    private final SysFileParsedService sysFileParsedService;
    private final SysFileChunkService sysFileChunkService;
    private final FileChunkingService fileChunkingService;
    private final KbEmbeddingPublisher kbEmbeddingPublisher;
    private final FileStorage fileStorage;
    private final List<FileParser> parsers;
    private final SysConfigService sysConfigService;

    public KnowledgeBaseIndexingServiceImpl(KnowledgeBaseService knowledgeBaseService,
                                            KbFileTreeMapper kbFileTreeMapper,
                                            SysFileService sysFileService,
                                            SysFileParsedService sysFileParsedService,
                                            SysFileChunkService sysFileChunkService,
                                            FileChunkingService fileChunkingService,
                                            KbEmbeddingPublisher kbEmbeddingPublisher,
                                            FileStorage fileStorage,
                                            List<FileParser> parsers,
                                            SysConfigService sysConfigService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.kbFileTreeMapper = kbFileTreeMapper;
        this.sysFileService = sysFileService;
        this.sysFileParsedService = sysFileParsedService;
        this.sysFileChunkService = sysFileChunkService;
        this.fileChunkingService = fileChunkingService;
        this.kbEmbeddingPublisher = kbEmbeddingPublisher;
        this.fileStorage = fileStorage;
        this.parsers = parsers;
        this.sysConfigService = sysConfigService;
    }

    @Override
    public int indexFiles(String kbId, Collection<String> fileTreeIds) {
        if (kbId == null || kbId.isBlank()) {
            return 0;
        }
        KnowledgeBase kb = knowledgeBaseService.getById(kbId);
        if (kb == null) {
            return 0;
        }

        // 通过 KbFileTree 查询文件节点
        QueryWrapper treeQw = QueryWrapper.create()
                .where(KbFileTreeTableDef.KB_FILE_TREE.KB_ID.eq(kbId))
                .and(KbFileTreeTableDef.KB_FILE_TREE.TYPE.eq("FILE"))
                .and(KbFileTreeTableDef.KB_FILE_TREE.DEL_FLAG.eq(0));
        if (fileTreeIds != null && !fileTreeIds.isEmpty()) {
            treeQw.and(KbFileTreeTableDef.KB_FILE_TREE.ID.in(fileTreeIds));
        }
        List<KbFileTree> fileNodes = kbFileTreeMapper.selectListByQuery(treeQw);
        if (fileNodes == null || fileNodes.isEmpty()) {
            return 0;
        }

        int processed = 0;
        for (KbFileTree node : fileNodes) {
            String fileId = node.getFileId();
            if (fileId == null || fileId.isBlank()) {
                continue;
            }
            SysFile file = sysFileService.getById(fileId);
            if (file == null) {
                continue;
            }

            if (!isRagSupportedFileType(file)) {
                continue;
            }

            boolean ok = parseFile(file);
            if (!ok) {
                continue;
            }

            fileChunkingService.chunk(fileId);
            List<SysFileChunk> chunks = sysFileChunkService.list(QueryWrapper.create()
                    .from(SysFileChunk.class)
                    .where(SysFileChunk::getFileId).eq(fileId));
            kbEmbeddingPublisher.publishChunks(kbId, chunks);
            processed++;
        }
        return processed;
    }

    private boolean parseFile(SysFile file) {
        String fileId = file.getId();
        String current = file.getAiParseStatus();
        // 如果已解析成功，跳过解析（支持文件复用）
        if (AiParseStatus.SUCCESS.name().equals(current)) {
            // 检查 parsed 记录是否存在
            SysFileParsed existingParsed = sysFileParsedService.getOne(QueryWrapper.create()
                    .from(SysFileParsed.class)
                    .where(SysFileParsed::getFileId).eq(fileId));
            if (existingParsed != null && existingParsed.getFullText() != null) {
                return true;
            }
        }
        if (AiParseStatus.RUNNING.name().equals(current)) {
            return true;
        }

        file.setAiParseStatus(AiParseStatus.RUNNING.name());
        sysFileService.updateById(file, true);

        try (InputStream in = fileStorage.load(new FileLocation(StorageType.valueOf(file.getStorageType()), file.getBucketName(), file.getObjectKey(), null, null))) {
            ParsedDocument doc = parse(fileId, file.getMimeType(), file.getFileName(), in);

            SysFileParsed parsed = sysFileParsedService.getOne(QueryWrapper.create()
                    .from(SysFileParsed.class)
                    .where(SysFileParsed::getFileId).eq(fileId));
            if (parsed == null) {
                parsed = new SysFileParsed();
                parsed.setFileId(fileId);
            }
            parsed.setFullText(doc.fullText());
            parsed.setPageCount(doc.pageCount());
            parsed.setMetadata(doc.metadata());
            parsed.setBlocks(doc.blocks());
            sysFileParsedService.saveOrUpdate(parsed);

            file.setAiParseStatus(AiParseStatus.SUCCESS.name());
            file.setAiStatus(AiStatus.PARSED.name());
            file.setTokenEstimate(TokenEstimator.estimateTokens(doc.fullText()));
            sysFileService.updateById(file, true);
            return true;
        } catch (Exception e) {
            file.setAiParseStatus(AiParseStatus.FAILED.name());
            file.setRemark(e.getMessage());
            sysFileService.updateById(file, true);
            return false;
        }
    }

    private ParsedDocument parse(String fileId, String contentType, String fileName, InputStream in) {
        for (FileParser p : parsers) {
            if (p != null && p.supports(contentType, fileName)) {
                ParsedDocument doc = p.parse(fileId, in);
                if (doc != null) {
                    return doc;
                }
                break;
            }
        }
        throw new IllegalArgumentException("unsupported contentType/fileName: " + contentType + "/" + fileName);
    }

    private boolean isRagSupportedFileType(SysFile file) {
        List<String> supportedTypes = sysConfigService.getConfigValueAs("rag.file.type", "ARRAY");
        if (supportedTypes == null || supportedTypes.isEmpty()) {
            return false;
        }
        String fileName = file.getFileName();
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= fileName.length() - 1) {
            return false;
        }
        String ext = fileName.substring(lastDot + 1).toLowerCase();
        return supportedTypes.stream()
                .map(String::toLowerCase)
                .anyMatch(supported -> supported.equals(ext));
    }
}
