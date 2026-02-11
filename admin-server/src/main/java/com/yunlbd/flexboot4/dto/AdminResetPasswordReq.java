package com.yunlbd.flexboot4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "New password", example = "NewPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String newPassword;
}
