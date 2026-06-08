package com.yunlbd.flexboot4.auth;

import java.util.List;

/**
 * 当前登录用户信息提供器。
 * <p>
 * 这里使用策略模式：业务模块只依赖这个抽象接口，不直接依赖具体安全框架。
 * 不同运行环境可以提供不同实现，例如 admin-starter 从 Spring Security 上下文读取用户，
 * 没有安全模块时则由 NoopCurrentUserProvider 作为空对象兜底。
 */
public interface CurrentUserProvider {

    /**
     * 返回当前请求上下文中的登录用户；无登录态或无安全实现时返回 null。
     */
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
