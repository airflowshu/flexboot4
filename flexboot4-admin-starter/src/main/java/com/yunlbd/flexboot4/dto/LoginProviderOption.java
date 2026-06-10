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
        return of("github", enabled);
    }

    public static LoginProviderOption qq(boolean enabled) {
        return of("qq", enabled);
    }

    public static LoginProviderOption of(String code, boolean enabled) {
        LoginProviderOption option = new LoginProviderOption();
        option.setCode(code);
        option.setEnabled(enabled);
        return option;
    }

    public boolean isEnabled(boolean defaultValue) {
        return enabled == null ? defaultValue : enabled;
    }
}
