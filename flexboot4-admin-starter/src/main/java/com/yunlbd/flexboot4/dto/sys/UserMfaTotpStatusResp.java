package com.yunlbd.flexboot4.dto.sys;

public record UserMfaTotpStatusResp(
        boolean enabled,
        String type,
        String deviceName
) {
}
