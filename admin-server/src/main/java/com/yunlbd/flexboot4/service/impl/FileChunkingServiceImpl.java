package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.config.FileParseProperties;
import com.yunlbd.flexboot4.entity.SysFile;
import com.yunlbd.flexboot4.entity.SysFileChunk;
import com.yunlbd.flexboot4.entity.SysFileParsed;
import com.yunlbd.flexboot4.file.ai.AiEmbedStatus;
import com.yunlbd.flexboot4.file.ai.AiStatus;
import com.yunlbd.flexboot4.file.parse.*;
import com.yunlbd.flexboot4.service.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class FileChunkingServiceImpl implements FileChunkingService {

    private final SysFileService sysFileService;
    private final SysFileParsedService sysFileParsedService;
    private final SysFileChunkService sysFileChunkService;
    private final FileParseProperties properties;
    private final FileEmbeddingPublisher embeddingPublisher;

    public FileChunkingServiceImpl(SysFileService sysFileService,
                                   SysFileParsedService sysFileParsedService,
                                   SysFileChunkService sysFileChunkService,
                                   FileParseProperties properties,
                                   FileEmbeddingPublisher embeddingPublisher) {
        this.sysFileService = sysFileService;
        this.sysFileParsedService = sysFileParsedService;
        this.sysFileChunkService = sysFileChunkService;
        this.properties = properties;
        this.embeddingPublisher = embeddingPublisher;
    }

    @Override
    public void chunk(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return;
        }
        SysFile file = sysFileService.getById(fileId);
        if (file == null) {
            return;
        }
        SysFileParsed parsed = sysFileParsedService.getOne(QueryWrapper.create()
                .from(SysFileParsed.class)
                .where(SysFileParsed::getFileId).eq(fileId));
        if (parsed == null || parsed.getFullText() == null || parsed.getFullText().isBlank()) {
            return;
        }

        ChunkingOptions opts = properties.toChunkingOptions();
        List<ChunkedText> chunks;
        if (parsed.getBlocks() != null && !parsed.getBlocks().isEmpty()) {
            chunks = BlockChunker.chunkBlocks(parsed.getBlocks(), opts);
        } else {
            chunks = TextChunker.chunk(parsed.getFullText(), opts).stream()
                    .map(t -> new ChunkedText(t, null, null))
                    .toList();
        }

        sysFileChunkService.remove(QueryWrapper.create()
                .from(SysFileChunk.class)
                .where(SysFileChunk::getFileId).eq(fileId));

        List<SysFileChunk> entities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkedText chunk = chunks.get(i);
            String content = chunk.text();
            SysFileChunk c = new SysFileChunk();
            c.setFileId(fileId);
            c.setChunkIndex(i);
            c.setContent(content);
            c.setContentHash(sha256Hex(content));
            c.setTokenCount(TokenEstimator.estimateTokens(content));
            c.setPageNumber(chunk.pageNumber());
            c.setSectionTitle(chunk.sectionTitle());
            c.setEmbeddingModel(file.getEmbeddingModel());
            c.setEmbedStatus(AiEmbedStatus.PENDING.name());
            entities.add(c);
        }
        if (!entities.isEmpty()) {
            sysFileChunkService.saveBatch(entities);
        }

        file.setChunkCount(entities.size());
        file.setAiStatus(AiStatus.CHUNKED.name());
        file.setAiEmbedStatus(AiEmbedStatus.EMBEDDING_PENDING.name());
        sysFileService.updateById(file, true);
        embeddingPublisher.publish(fileId, entities.size(), file.getEmbeddingModel());
    }

    private String sha256Hex(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            return "";
        }
    }
}
