package com.yunlbd.flexboot4.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ApiResult<String> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Login failed: {}", e.getMessage());
        return ApiResult.error("用户名或密码错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Invalid Argument", e);
        return ApiResult.error(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<String> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access Denied: {}", e.getMessage());
        return ApiResult.error(403, "无权访问此资源");
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<String> handleException(Exception e) {
        log.error("System Error", e);
        return ApiResult.error(e.getMessage());
    }
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFoundException(NoResourceFoundException e) {
        // 仅仅记录 debug 日志，或者干脆什么都不做
        // 这样就不会在控制台打印大段的 ERROR 堆栈信息了
        log.debug("Resource not found: {}", e.getResourcePath());
    }
    // Add more specific handlers as needed
}
