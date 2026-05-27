package com.yunlbd.flexboot4.auth;

import java.util.List;

public interface CurrentUserProvider {

    CurrentUser currentUser();

    default String getUserId() {
        CurrentUser user = currentUser();
        return user == null ? null : user.userId();
    }

    default String getUsername() {
        CurrentUser user = currentUser();
        return user == null ? null : user.username();
    }

    default List<String> getPermissionCodes() {
        CurrentUser user = currentUser();
        return user == null || user.permissionCodes() == null ? List.of() : user.permissionCodes();
    }
}
