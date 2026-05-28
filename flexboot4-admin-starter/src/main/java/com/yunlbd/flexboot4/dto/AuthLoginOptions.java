package com.yunlbd.flexboot4.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AuthLoginOptions {

    public static final String METHOD_FORGET_PASSWORD = "forgetPassword";
    public static final String METHOD_PASSWORD = "password";
    public static final String METHOD_QRCODE = "qrcode";
    public static final String METHOD_REGISTER = "register";
    public static final String METHOD_SMS = "sms";
    public static final String METHOD_THIRD_PARTY = "thirdParty";

    private Map<String, LoginMethodOption> methods = new LinkedHashMap<>();

    public static AuthLoginOptions defaults() {
        AuthLoginOptions options = new AuthLoginOptions();
        options.getMethods().put(METHOD_PASSWORD, LoginMethodOption.enabled(true));
        options.getMethods().put(METHOD_SMS, LoginMethodOption.smsDefaults(false));
        options.getMethods().put(METHOD_QRCODE, LoginMethodOption.enabled(false));
        options.getMethods().put(METHOD_THIRD_PARTY, LoginMethodOption.thirdPartyDefaults(false));
        options.getMethods().put(METHOD_REGISTER, LoginMethodOption.enabled(false));
        options.getMethods().put(METHOD_FORGET_PASSWORD, LoginMethodOption.enabled(true));
        return options;
    }

    public AuthLoginOptions mergeDefaults() {
        AuthLoginOptions merged = defaults();
        if (methods == null) {
            return merged;
        }
        methods.forEach((name, option) -> {
            if (name == null || name.isBlank() || option == null) {
                return;
            }
            merged.getMethods().compute(name, (k, base) -> base == null ? option.copy() : base.merge(option));
        });
        return merged;
    }

    public LoginMethodOption method(String name) {
        return mergeDefaults().getMethods().get(name);
    }
}
