package com.yunlbd.flexboot4.dto.ops;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiApiKeyCreateReq {
    @NotBlank
    private String keyName;
    private String userId;
    private Integer status;
    private Long quote;
    private String modelScope;
    private LocalDateTime expiresAt;
    private String notes;
    private String remark;
}
