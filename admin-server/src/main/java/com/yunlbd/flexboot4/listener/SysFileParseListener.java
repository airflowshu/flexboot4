package com.yunlbd.flexboot4.listener;

import com.yunlbd.flexboot4.entity.SysFile;
import com.yunlbd.flexboot4.entity.SysFileParsed;
import com.yunlbd.flexboot4.event.SysFileParsedEvent;
import com.yunlbd.flexboot4.event.SysFileUploadedEvent;
import com.yunlbd.flexboot4.file.FileLocation;
import com.yunlbd.flexboot4.file.FileStorage;
import com.yunlbd.flexboot4.file.StorageType;
import com.yunlbd.flexboot4.file.ai.AiParseStatus;
import com.yunlbd.flexboot4.file.ai.AiStatus;
import com.yunlbd.flexboot4.file.parse.FileParser;
import com.yunlbd.flexboot4.file.parse.ParsedDocument;
import com.yunlbd.flexboot4.file.parse.TokenEstimator;
import com.yunlbd.flexboot4.service.SysFileParsedService;
import com.yunlbd.flexboot4.service.SysFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SysFileParseListener {

    private final SysFileService sysFileService;
    private final SysFileParsedService sysFileParsedService;
    private final FileStorage fileStorage;
    private final List<FileParser> parsers;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onFileUploaded(SysFileUploadedEvent event) {
        String fileId = event.fileId();
        if (fileId == null || fileId.isBlank()) {
            return;
        }

        SysFile file = sysFileService.getById(fileId);
        if (file == null) {
            return;
        }

        String current = file.getAiParseStatus();
        if (current != null && !current.isBlank()) {
            AiParseStatus st;
            try {
                st = AiParseStatus.valueOf(current);
            } catch (Exception e) {
                st = null;
            }
            if (st == AiParseStatus.RUNNING || st == AiParseStatus.SUCCESS) {
                return;
            }
        }

        file.setAiParseStatus(AiParseStatus.RUNNING.name());
        sysFileService.updateById(file, true);

        try (InputStream in = fileStorage.load(new FileLocation(StorageType.valueOf(file.getStorageType()), file.getBucketName(), file.getObjectKey(), null, null))) {
            ParsedDocument doc = parse(fileId, file.getMimeType(), file.getFileName(), in);

            SysFileParsed parsed = sysFileParsedService.getOne(com.mybatisflex.core.query.QueryWrapper.create()
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

            eventPublisher.publishEvent(new SysFileParsedEvent(fileId));
        } catch (Exception e) {
            file.setAiParseStatus(AiParseStatus.FAILED.name());
            file.setRemark(e.getMessage());
            sysFileService.updateById(file, true);
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
}
