package com.yunlbd.flexboot4.task;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.SysFile;
import com.yunlbd.flexboot4.event.SysFileUploadedEvent;
import com.yunlbd.flexboot4.file.ai.AiParseStatus;
import com.yunlbd.flexboot4.service.SysFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SysFileParseRetryTask {

    private final SysFileService sysFileService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(initialDelay = 30000, fixedDelay = 300000)
    public void retryFailedOrStuck() {
        LocalDateTime stuckBefore = LocalDateTime.now().minusMinutes(15);
        List<SysFile> failed = sysFileService.list(QueryWrapper.create()
                .from(SysFile.class)
                .where(SysFile::getDelFlag).eq(0)
                .and(SysFile::getAiParseStatus).eq(AiParseStatus.FAILED.name())
                .limit(25));
        List<SysFile> stuck = sysFileService.list(QueryWrapper.create()
                .from(SysFile.class)
                .where(SysFile::getDelFlag).eq(0)
                .and(SysFile::getAiParseStatus).eq(AiParseStatus.RUNNING.name())
                .and(SysFile::getLastModifyTime).lt(stuckBefore)
                .limit(25));
        Set<String> ids = new LinkedHashSet<>();
        if (failed != null) {
            for (SysFile f : failed) {
                if (f != null && f.getId() != null && !f.getId().isBlank()) {
                    ids.add(f.getId());
                }
            }
        }
        if (stuck != null) {
            for (SysFile f : stuck) {
                if (f != null && f.getId() != null && !f.getId().isBlank()) {
                    ids.add(f.getId());
                }
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        for (String id : ids) {
            eventPublisher.publishEvent(new SysFileUploadedEvent(id));
        }
    }
}
