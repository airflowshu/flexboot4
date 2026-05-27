package com.yunlbd.flexboot4.auth;

import java.util.List;

public record CurrentUser(
        String userId,
        String username,
        List<String> permissionCodes
) {
}
