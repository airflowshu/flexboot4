package com.yunlbd.flexboot4.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginMethodOption {

    private Boolean enabled;
    private Integer codeLength;
    private Integer cooldownSeconds;
    private List<LoginProviderOption> providers;

    public static LoginMethodOption enabled(boolean enabled) {
        LoginMethodOption option = new LoginMethodOption();
        option.setEnabled(enabled);
        return option;
    }

    public static LoginMethodOption smsDefaults(boolean enabled) {
        LoginMethodOption option = enabled(enabled);
        option.setCodeLength(6);
        option.setCooldownSeconds(60);
        return option;
    }

    public static LoginMethodOption thirdPartyDefaults() {
        LoginMethodOption option = new LoginMethodOption();
        option.setProviders(List.of(
                LoginProviderOption.github(false),
                LoginProviderOption.qq(false)
        ));
        return option;
    }

    public boolean isEnabled(boolean defaultValue) {
        return enabled == null ? defaultValue : enabled;
    }

    public LoginMethodOption merge(LoginMethodOption override) {
        LoginMethodOption merged = copy();
        if (override.getEnabled() != null) {
            merged.setEnabled(override.getEnabled());
        }
        if (override.getCodeLength() != null) {
            merged.setCodeLength(override.getCodeLength());
        }
        if (override.getCooldownSeconds() != null) {
            merged.setCooldownSeconds(override.getCooldownSeconds());
        }
        if (override.getProviders() != null) {
            merged.setProviders(new ArrayList<>(override.getProviders()));
        }
        return merged;
    }

    public LoginMethodOption copy() {
        LoginMethodOption copy = new LoginMethodOption();
        copy.setEnabled(enabled);
        copy.setCodeLength(codeLength);
        copy.setCooldownSeconds(cooldownSeconds);
        copy.setProviders(providers == null ? null : new ArrayList<>(providers));
        return copy;
    }
}
