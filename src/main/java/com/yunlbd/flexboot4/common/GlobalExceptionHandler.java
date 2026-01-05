package com.yunlbd.flexboot4.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ApiResult<String> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Login failed: {}", e.getMessage());
        return ApiResult.error("用户名或密码错误");
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<String> handleException(Exception e) {
        log.error("System Error", e);
        return ApiResult.error(e.getMessage());
    }
    
    // Add more specific handlers as needed
}
