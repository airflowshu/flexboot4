package com.yunlbd.flexboot4.service.sys.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SysUserRoleServiceImplTest {

    private final SysUserRoleServiceImpl service = new SysUserRoleServiceImpl();

    @Test
    void assignRolesToUserRejectsSuperUser() {
        assertThatThrownBy(() -> service.assignRolesToUser("1", List.of("2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("超级管理员用户不允许调整角色");
    }

    @Test
    void assignRolesToUserRejectsSuperRoleForOtherUsers() {
        assertThatThrownBy(() -> service.assignRolesToUser("2", List.of("1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("超级管理员角色不允许分配给其他用户");
    }
}
