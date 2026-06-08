package com.yunlbd.flexboot4.auth;

/**
 * 当前用户提供器的空对象实现。
 * <p>
 * 当应用未引入 admin-starter 或没有注册其它 CurrentUserProvider Bean 时，
 * 自动配置会注册该实现，保证依赖 CurrentUserProvider 的业务模块仍可启动。
 */
public class NoopCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser currentUser() {
        return null;
    }
}
