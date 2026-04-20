package com.yunlbd.flexboot4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Admin reset user password request DTO
 */
@Data
@Schema(name = "AdminResetPasswordReq")
public class AdminResetPasswordReq {

    @Schema(description = "User ID to reset password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "User ID is required")
    private String userId;
}
