package com.yunlbd.flexboot4.auth;

public class NoopCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser currentUser() {
        return null;
    }
}
