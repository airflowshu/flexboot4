package com.yunlbd.flexboot4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * Forget password request DTO
 */
@Data
@Schema(name = "ForgetPasswordReq")
public class ForgetPasswordReq {

    @Schema(description = "User email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "Invalid email format")
    private String email;
}
