package com.yunlbd.flexboot4.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginProviderOption {

    private String code;
    private Boolean enabled;

    public static LoginProviderOption github(boolean enabled) {
        LoginProviderOption option = new LoginProviderOption();
        option.setCode("github");
        option.setEnabled(enabled);
        return option;
    }

    public boolean isEnabled(boolean defaultValue) {
        return enabled == null ? defaultValue : enabled;
    }
}
