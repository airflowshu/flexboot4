package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.GlobalExceptionHandler;
import com.yunlbd.flexboot4.converter.sys.SysFileCrudMapper;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import com.yunlbd.flexboot4.service.sys.FileManagerService;
import com.yunlbd.flexboot4.service.sys.SysFileService;
import com.yunlbd.flexboot4.storage.FileAccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysFileControllerAccessTest {

    private SysFileService sysFileService;
    private FileManagerService fileManagerService;
    private FileAccessTokenService tokenService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sysFileService = mock(SysFileService.class);
        fileManagerService = mock(FileManagerService.class);
        tokenService = mock(FileAccessTokenService.class);
        SysFileCrudMapper mapper = mock(SysFileCrudMapper.class);
        SysFileController controller = new SysFileController(sysFileService, mapper, fileManagerService, tokenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void accessLocalFile_shouldStreamWholeFileForValidToken() throws Exception {
        when(tokenService.verify("valid-token"))
                .thenReturn(new FileAccessTokenService.AccessToken("file-1", Instant.now().plusSeconds(60), true));
        when(sysFileService.getById("file-1")).thenReturn(file("file-1", 6L, 0));
        when(fileManagerService.load("file-1"))
                .thenReturn(new ByteArrayInputStream("abcdef".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/admin/file/access/{token}", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().longValue("Content-Length", 6L))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().bytes("abcdef".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void accessLocalFile_shouldAcceptSignedUrlWithFileNameSuffix() throws Exception {
        when(tokenService.verify("valid-token"))
                .thenReturn(new FileAccessTokenService.AccessToken("file-1", Instant.now().plusSeconds(60), false));
        when(sysFileService.getById("file-1")).thenReturn(file("file-1", 6L, 0));
        when(fileManagerService.load("file-1"))
                .thenReturn(new ByteArrayInputStream("abcdef".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/admin/file/access/{token}/{fileName}", "valid-token", "report.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(content().bytes("abcdef".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void accessLocalFile_shouldSupportSingleRangeRequest() throws Exception {
        when(tokenService.verify("range-token"))
                .thenReturn(new FileAccessTokenService.AccessToken("file-1", Instant.now().plusSeconds(60), false));
        when(sysFileService.getById("file-1")).thenReturn(file("file-1", 6L, 0));
        when(fileManagerService.load("file-1"))
                .thenReturn(new ByteArrayInputStream("abcdef".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/admin/file/access/{token}", "range-token")
                        .header("Range", "bytes=1-3"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 1-3/6"))
                .andExpect(header().longValue("Content-Length", 3L))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(content().bytes("bcd".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void accessLocalFile_shouldRejectTamperedTokenWithUnauthorizedStatus() throws Exception {
        when(tokenService.verify("bad-token")).thenThrow(new IllegalArgumentException("访问令牌签名无效"));

        mockMvc.perform(get("/api/admin/file/access/{token}", "bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("访问令牌签名无效"));
    }

    @Test
    void accessLocalFile_shouldRejectExpiredTokenWithUnauthorizedStatus() throws Exception {
        when(tokenService.verify("expired-token")).thenThrow(new IllegalArgumentException("访问令牌已过期"));

        mockMvc.perform(get("/api/admin/file/access/{token}", "expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("访问令牌已过期"));
    }

    @Test
    void accessLocalFile_shouldReturnNotFoundWhenFileWasDeleted() throws Exception {
        when(tokenService.verify("deleted-token"))
                .thenReturn(new FileAccessTokenService.AccessToken("file-1", Instant.now().plusSeconds(60), true));
        when(sysFileService.getById("file-1")).thenReturn(file("file-1", 6L, 1));

        mockMvc.perform(get("/api/admin/file/access/{token}", "deleted-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("file not found"));
    }

    private SysFile file(String id, Long fileSize, Integer delFlag) {
        SysFile file = new SysFile();
        file.setId(id);
        file.setFileName("report.txt");
        file.setMimeType("text/plain");
        file.setFileSize(fileSize);
        file.setDelFlag(delFlag);
        return file;
    }
}
