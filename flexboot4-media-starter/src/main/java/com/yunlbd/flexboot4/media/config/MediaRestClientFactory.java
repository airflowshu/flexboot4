package com.yunlbd.flexboot4.media.config;

import org.springframework.web.client.RestClient;

public class MediaRestClientFactory {

    private final RestClient.Builder builder;

    public MediaRestClientFactory(RestClient.Builder builder) {
        this.builder = builder;
    }

    public RestClient create(String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
