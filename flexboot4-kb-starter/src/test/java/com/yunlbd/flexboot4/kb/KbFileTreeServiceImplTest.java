package com.yunlbd.flexboot4.kb;

import com.yunlbd.flexboot4.entity.kb.KbFileTree;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import com.yunlbd.flexboot4.service.kb.impl.KbFileTreeServiceImpl;
import com.yunlbd.flexboot4.service.sys.SysFileService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KbFileTreeServiceImplTest {

    private final SysFileService sysFileService = mock(SysFileService.class);
    private final CapturingKbFileTreeService service = new CapturingKbFileTreeService(sysFileService);

    @Test
    void addFileShouldUseUploadedFileNameForFileTreeNodeName() {
        boolean saved = service.addFile("kb-1", "folder-1", "file-1", " report.pdf ");

        assertThat(saved).isTrue();
        assertThat(service.savedNode.getName()).isEqualTo("report.pdf");
        assertThat(service.savedNode.getType()).isEqualTo("FILE");
        assertThat(service.savedNode.getFileId()).isEqualTo("file-1");
    }

    @Test
    void addFileShouldFallbackToSysFileNameWhenNameIsNotProvided() {
        SysFile file = new SysFile();
        file.setId("file-1");
        file.setFileName("manual.docx");
        when(sysFileService.getById("file-1")).thenReturn(file);

        boolean saved = service.addFile("kb-1", null, "file-1");

        assertThat(saved).isTrue();
        assertThat(service.savedNode.getName()).isEqualTo("manual.docx");
    }

    private static class CapturingKbFileTreeService extends KbFileTreeServiceImpl {

        private KbFileTree savedNode;

        CapturingKbFileTreeService(SysFileService sysFileService) {
            super(sysFileService);
        }

        @Override
        public boolean save(KbFileTree entity) {
            this.savedNode = entity;
            return true;
        }
    }
}
