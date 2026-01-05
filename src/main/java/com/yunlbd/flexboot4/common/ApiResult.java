package com.yunlbd.flexboot4.common;

import lombok.Data;

@Data
public class ApiResult<T> {
    private Integer code;
    private String message;
    private T data;
    private String error = null;

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.setCode(0);
        r.setData(data);
        r.setMessage("ok");
        return r;
    }

    public static <T> ApiResult<T> success(T data, String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.setCode(0);
        r.setData(data);
        r.setMessage(msg);
        return r;
    }

    public static <T> ApiResult<T> error(String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.setCode(-1);
        r.setMessage(msg);
        r.setError(msg);
        return r;
    }
    
    public static <T> ApiResult<T> error(Integer code, String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.setCode(code);
        r.setMessage(msg);
        r.setError("error");
        return r;
    }
}
