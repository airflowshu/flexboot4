package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.converter.sys.SysUserCrudMapper;
import com.yunlbd.flexboot4.dto.sys.CurrentUserPasswordUpdateReq;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpStatusResp;
import com.yunlbd.flexboot4.security.UserDetailsCacheService;
import com.yunlbd.flexboot4.service.sys.FileManagerService;
import com.yunlbd.flexboot4.service.sys.SocialAuthService;
import com.yunlbd.flexboot4.service.sys.SysUserService;
import com.yunlbd.flexboot4.service.sys.UserMfaService;
import com.yunlbd.flexboot4.service.sys.UserSecurityEmailService;
import com.yunlbd.flexboot4.service.sys.UserSecurityPhoneService;
import com.yunlbd.flexboot4.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserControllerProfileTest {

    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = org.mockito.Mockito.mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void getUserInfoReturnsSecurityPhoneMetadataWithoutRawPhone() {
        SysUser user = new SysUser();
        user.setId("u1");
        user.setUsername("alice");
        user.setRealName("Alice");
        user.setEmail("alice@example.com");
        user.setPhone("13800138000");
        securityUtils.when(SecurityUtils::getSysUser).thenReturn(user);

        UserMfaService userMfaService = mock(UserMfaService.class);
        when(userMfaService.getTotpStatus("u1")).thenReturn(new UserMfaTotpStatusResp(true, "TOTP", "认证器应用"));

        SysUserController controller = new SysUserController(
                mock(SysUserService.class),
                mock(SysUserCrudMapper.class),
                mock(PasswordEncoder.class),
                mock(FileManagerService.class),
                mock(UserSecurityPhoneService.class),
                mock(UserSecurityEmailService.class),
                userMfaService,
                mock(UserDetailsCacheService.class),
                mock(SocialAuthService.class)
        );

        var data = controller.getUserInfo().getData();

        assertThat(data).containsEntry("securityPhoneBound", true);
        assertThat(data).containsEntry("securityPhoneMasked", "138****8000");
        assertThat(data).containsEntry("securityEmailBound", true);
        assertThat(data).containsEntry("securityEmailMasked", "ali***@example.com");
        assertThat(data).containsEntry("mfaEnabled", true);
        assertThat(data).containsEntry("mfaType", "TOTP");
        assertThat(data).containsEntry("mfaDeviceName", "认证器应用");
        assertThat(data).doesNotContainKey("phone");
        assertThat(data).doesNotContainKey("email");
    }

    @Test
    void updateCurrentUserPasswordRejectsWrongOldPassword() {
        SysUser currentUser = new SysUser();
        currentUser.setId("u1");
        currentUser.setUsername("alice");
        securityUtils.when(SecurityUtils::getSysUser).thenReturn(currentUser);

        SysUser dbUser = new SysUser();
        dbUser.setId("u1");
        dbUser.setUsername("alice");
        dbUser.setPassword("encoded-old");
        dbUser.setStatus(1);

        SysUserService service = mock(SysUserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserMfaService userMfaService = mock(UserMfaService.class);
        UserDetailsCacheService userDetailsCacheService = mock(UserDetailsCacheService.class);
        when(service.getById("u1")).thenReturn(dbUser);
        when(passwordEncoder.matches("wrong-old", "encoded-old")).thenReturn(false);

        SysUserController controller = newController(service, passwordEncoder, userMfaService, userDetailsCacheService);
        CurrentUserPasswordUpdateReq req = passwordReq("wrong-old", "new-password-1", "new-password-1");

        assertThatThrownBy(() -> controller.updateCurrentUserPassword(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("旧密码不正确");
        verify(service, never()).updatePasswordById("u1", "new-password-1");
        verify(userDetailsCacheService, never()).evictUserCache("alice");
    }

    @Test
    void updateCurrentUserPasswordUpdatesAndEvictsUserDetailsCache() {
        SysUser currentUser = new SysUser();
        currentUser.setId("u1");
        currentUser.setUsername("alice");
        securityUtils.when(SecurityUtils::getSysUser).thenReturn(currentUser);

        SysUser dbUser = new SysUser();
        dbUser.setId("u1");
        dbUser.setUsername("alice");
        dbUser.setPassword("encoded-old");
        dbUser.setStatus(1);

        SysUserService service = mock(SysUserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserMfaService userMfaService = mock(UserMfaService.class);
        UserDetailsCacheService userDetailsCacheService = mock(UserDetailsCacheService.class);
        when(service.getById("u1")).thenReturn(dbUser);
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("new-password-1", "encoded-old")).thenReturn(false);
        when(service.updatePasswordById("u1", "new-password-1")).thenReturn(true);

        SysUserController controller = newController(service, passwordEncoder, userMfaService, userDetailsCacheService);
        CurrentUserPasswordUpdateReq req = passwordReq("old-password", "new-password-1", "new-password-1");

        var result = controller.updateCurrentUserPassword(req);

        assertThat(result.getData()).isEqualTo("密码修改成功，请重新登录");
        verify(service).updatePasswordById("u1", "new-password-1");
        verify(userDetailsCacheService).evictUserCache("alice");
    }

    private static CurrentUserPasswordUpdateReq passwordReq(String oldPassword, String newPassword, String confirmPassword) {
        CurrentUserPasswordUpdateReq req = new CurrentUserPasswordUpdateReq();
        req.setOldPassword(oldPassword);
        req.setNewPassword(newPassword);
        req.setConfirmPassword(confirmPassword);
        return req;
    }

    private static SysUserController newController(
            SysUserService service,
            PasswordEncoder passwordEncoder,
            UserMfaService userMfaService,
            UserDetailsCacheService userDetailsCacheService
    ) {
        return new SysUserController(
                service,
                mock(SysUserCrudMapper.class),
                passwordEncoder,
                mock(FileManagerService.class),
                mock(UserSecurityPhoneService.class),
                mock(UserSecurityEmailService.class),
                userMfaService,
                userDetailsCacheService,
                mock(SocialAuthService.class)
        );
    }
}
