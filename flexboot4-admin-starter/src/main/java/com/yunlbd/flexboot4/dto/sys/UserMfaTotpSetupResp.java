package com.yunlbd.flexboot4.dto.sys;

public record UserMfaTotpSetupResp(
        String otpauthUri,
        String manualKey,
        String issuer,
        String accountName,
        int digits,
        int period
) {
}
