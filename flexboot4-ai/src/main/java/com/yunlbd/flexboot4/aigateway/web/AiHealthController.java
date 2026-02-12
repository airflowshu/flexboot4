package com.yunlbd.flexboot4.aigateway.web;

import com.yunlbd.flexboot4.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiHealthController {
    @GetMapping("/api/ai/health")
    public ApiResult<String> health() {
        return ApiResult.success("ok");
    }
}

