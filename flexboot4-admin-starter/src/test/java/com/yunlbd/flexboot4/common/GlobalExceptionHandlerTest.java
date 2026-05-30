package com.yunlbd.flexboot4.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalStateExceptionReturnsBusinessError() {
        ApiResult<String> result = handler.handleIllegalStateException(
                new IllegalStateException("验证码不正确或已过期")
        );

        assertThat(result.getCode()).isEqualTo(-1);
        assertThat(result.getMessage()).isEqualTo("验证码不正确或已过期");
    }

    @Test
    void securityExceptionStillReturnsUnauthorizedError() {
        ApiResult<String> result = handler.handleSecurityException(new SecurityException("Invalid token"));

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("未认证或令牌无效/过期");
    }
}
