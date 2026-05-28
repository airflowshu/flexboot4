package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.converter.sys.SysUserCrudMapper;
import com.yunlbd.flexboot4.dto.sys.CurrentUserPasswordUpdateReq;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailBindReq;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailBindResp;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailCodeReq;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneBindReq;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneBindResp;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneCodeReq;
import com.yunlbd.flexboot4.dto.sys.SysUserCreateReq;
import com.yunlbd.flexboot4.dto.sys.SysUserUpdateReq;
import com.yunlbd.flexboot4.dto.sys.UserProfileUpdateReq;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpConfirmReq;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpDisableReq;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpSetupResp;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpStatusResp;
import com.yunlbd.flexboot4.entity.sys.SysRole;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.excel.sys.SysUserExportRow;
import com.yunlbd.flexboot4.excel.sys.SysUserImportRow;
import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.security.UserDetailsCacheService;
import com.yunlbd.flexboot4.service.sys.FileManagerService;
import com.yunlbd.flexboot4.service.sys.SysUserService;
import com.yunlbd.flexboot4.service.sys.UserMfaService;
import com.yunlbd.flexboot4.service.sys.UserSecurityEmailService;
import com.yunlbd.flexboot4.service.sys.UserSecurityPhoneService;
import com.yunlbd.flexboot4.util.SecurityUtils;
import com.yunlbd.flexboot4.vo.sys.SysUserDetailVO;
import com.yunlbd.flexboot4.vo.sys.SysUserListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/user")
@Tag(name = "用户管理", description = "SysUser - 用户管理")
@ApiTagGroup(group = "系统管理")
public class SysUserController extends BaseCrudController<SysUserService, SysUser, String,
        SysUserCreateReq, SysUserUpdateReq, SysUserListVO, SysUserDetailVO> {

    private final PasswordEncoder passwordEncoder;
    private final FileManagerService fileManagerService;
    private final UserSecurityPhoneService userSecurityPhoneService;
    private final UserSecurityEmailService userSecurityEmailService;
    private final UserMfaService userMfaService;
    private final UserDetailsCacheService userDetailsCacheService;
    private final SysUserCrudMapper mapper;

    public SysUserController(SysUserService service,
                             SysUserCrudMapper mapper,
                             PasswordEncoder passwordEncoder,
                             FileManagerService fileManagerService,
                             UserSecurityPhoneService userSecurityPhoneService,
                             UserSecurityEmailService userSecurityEmailService,
                             UserMfaService userMfaService,
                             UserDetailsCacheService userDetailsCacheService) {
        super(service, mapper);
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.fileManagerService = fileManagerService;
        this.userSecurityPhoneService = userSecurityPhoneService;
        this.userSecurityEmailService = userSecurityEmailService;
        this.userMfaService = userMfaService;
        this.userDetailsCacheService = userDetailsCacheService;
    }


    @Override
    public Class<SysUser> getEntityClass() {
        return SysUser.class;
    }

    @Override
    protected SysUser beforeCreate(SysUser user, SysUserCreateReq request) {
        //新增用户，默认密码111111，
        // 重置密码需通过忘记密码，邮件方式重置;或者管理员才有操作权限reset
        user.setPassword(passwordEncoder.encode("11111111"));
        return user;
    }

    @Override
    protected CrudFieldPolicy fieldPolicy() {
        return CrudFieldPolicy.same(List.of(
                "id", "username", "realName", "profileFileId", "email", "phone",
                "gender", "deptId", "status", "remark", "createTime", "lastModifyTime"
        )).withQueryFields("dept.deptName", "roles.roleValue", "roles.roleName");
    }

    @Override
    protected CrudExcelSupport<SysUser, ?, ?> excelSupport() {
        return CrudExcelSupport.of(SysUserExportRow.class, SysUserImportRow.class, mapper::toExportRow, null);
    }

    @Operation(summary = "获取用户信息", description = "获取登录认证用户信息")
    @RequirePermission(skip = true)
    @GetMapping("/info")
    public ApiResult<Map<String, Object>> getUserInfo() {
        SysUser user = SecurityUtils.getSysUser();
        if (user == null) {
            return ApiResult.error("User not logged in");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("userId", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("profileFileId", user.getProfileFileId());
        info.put("remark", user.getRemark());
        info.put("desc", user.getRemark());
        info.put("avatar", "");
        // 头像URL（优先取 avatar_file_id 对应的公有URL）
        if (user.getProfileFileId() != null && !user.getProfileFileId().isBlank()) {
            FileAccessDescriptor avatar = fileManagerService.access(user.getProfileFileId(), 3600, false);
            info.put("avatar", avatar.url());
        }
        boolean securityPhoneBound = user.getPhone() != null && !user.getPhone().isBlank();
        info.put("securityPhoneBound", securityPhoneBound);
        info.put("securityPhoneMasked", securityPhoneBound ? maskPhone(user.getPhone()) : null);
        boolean securityEmailBound = user.getEmail() != null && !user.getEmail().isBlank();
        info.put("securityEmailBound", securityEmailBound);
        info.put("securityEmailMasked", securityEmailBound ? maskEmail(user.getEmail()) : null);
        UserMfaTotpStatusResp mfaStatus = userMfaService.getTotpStatus(user.getId());
        info.put("mfaEnabled", mfaStatus.enabled());
        info.put("mfaType", mfaStatus.type());
        info.put("mfaDeviceName", mfaStatus.deviceName());
        info.put("roles", user.getRoles() != null
                ? user.getRoles().stream().map(SysRole::getRoleValue).collect(Collectors.toList())
                : new ArrayList<>());
        return ApiResult.success(info);
    }

    @Operation(summary = "更新当前用户基本资料", description = "更新当前登录用户的姓名、头像和个人简介")
    @OperLog(title = "更新当前用户基本资料", businessType = BusinessType.UPDATE)
    @RequirePermission(skip = true)
    @PutMapping("/profile")
    public ApiResult<Map<String, Object>> updateCurrentUserProfile(@Valid @RequestBody UserProfileUpdateReq req) {
        SysUser user = requireCurrentUser();

        String realName = normalizeRequiredText(req.getRealName(), "姓名不能为空");
        String profileFileId = normalizeBlankToNull(req.getProfileFileId());
        String remark = normalizeBlankToNull(req.getRemark());

        boolean updated = service.updateCurrentProfile(user.getId(), realName, profileFileId, remark);
        if (!updated) {
            throw new IllegalStateException("更新用户基本资料失败");
        }

        user.setRealName(realName);
        user.setProfileFileId(profileFileId);
        user.setRemark(remark);
        userDetailsCacheService.evictUserCache(user.getUsername());
        refreshAuthenticationPrincipal(user);

        return getUserInfo();
    }

    @Operation(summary = "发送密保手机绑定验证码", description = "给当前登录用户待绑定的手机号发送验证码")
    @OperLog(title = "发送密保手机绑定验证码", businessType = BusinessType.OTHER,
            isSaveRequestData = false, isSaveResponseData = false)
    @RequirePermission(skip = true)
    @PostMapping("/security-phone/code")
    public ApiResult<String> sendSecurityPhoneCode(@Valid @RequestBody SecurityPhoneCodeReq req) {
        SysUser user = SecurityUtils.getSysUser();
        return ApiResult.success(userSecurityPhoneService.sendBindCode(user, req));
    }

    @Operation(summary = "绑定密保手机", description = "校验验证码并绑定或更换当前登录用户密保手机")
    @OperLog(title = "绑定密保手机", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @RequirePermission(skip = true)
    @PutMapping("/security-phone")
    public ApiResult<SecurityPhoneBindResp> bindSecurityPhone(@Valid @RequestBody SecurityPhoneBindReq req) {
        SysUser user = SecurityUtils.getSysUser();
        return ApiResult.success(userSecurityPhoneService.bindPhone(user, req));
    }

    @Operation(summary = "发送备用邮箱绑定验证码", description = "给当前登录用户待绑定的邮箱发送验证码")
    @OperLog(title = "发送备用邮箱绑定验证码", businessType = BusinessType.OTHER,
            isSaveRequestData = false, isSaveResponseData = false)
    @RequirePermission(skip = true)
    @PostMapping("/security-email/code")
    public ApiResult<String> sendSecurityEmailCode(@Valid @RequestBody SecurityEmailCodeReq req) {
        SysUser user = SecurityUtils.getSysUser();
        return ApiResult.success(userSecurityEmailService.sendBindCode(user, req));
    }

    @Operation(summary = "绑定备用邮箱", description = "校验验证码并绑定或更换当前登录用户备用邮箱")
    @OperLog(title = "绑定备用邮箱", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @RequirePermission(skip = true)
    @PutMapping("/security-email")
    public ApiResult<SecurityEmailBindResp> bindSecurityEmail(@Valid @RequestBody SecurityEmailBindReq req) {
        SysUser user = SecurityUtils.getSysUser();
        return ApiResult.success(userSecurityEmailService.bindEmail(user, req));
    }

    @Operation(summary = "初始化 TOTP MFA 绑定", description = "生成认证器二维码内容，确认前不会启用 MFA")
    @OperLog(title = "初始化 TOTP MFA 绑定", businessType = BusinessType.OTHER,
            isSaveRequestData = false, isSaveResponseData = false)
    @RequirePermission(skip = true)
    @PostMapping("/mfa/totp/setup")
    public ApiResult<UserMfaTotpSetupResp> setupMfaTotp() {
        SysUser user = SecurityUtils.getSysUser();
        return ApiResult.success(userMfaService.setupTotp(user));
    }

    @Operation(summary = "确认绑定 TOTP MFA", description = "校验认证器动态码并启用 MFA")
    @OperLog(title = "确认绑定 TOTP MFA", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @RequirePermission(skip = true)
    @PostMapping("/mfa/totp/confirm")
    public ApiResult<UserMfaTotpStatusResp> confirmMfaTotp(@Valid @RequestBody UserMfaTotpConfirmReq req) {
        SysUser user = SecurityUtils.getSysUser();
        return ApiResult.success(userMfaService.confirmTotp(user, req));
    }

    @Operation(summary = "关闭 TOTP MFA", description = "使用当前密码和认证器动态码关闭 MFA")
    @OperLog(title = "关闭 TOTP MFA", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @RequirePermission(skip = true)
    @DeleteMapping("/mfa/totp")
    public ApiResult<UserMfaTotpStatusResp> disableMfaTotp(@Valid @RequestBody UserMfaTotpDisableReq req) {
        SysUser user = SecurityUtils.getSysUser();
        return ApiResult.success(userMfaService.disableTotp(user, req));
    }

    @Operation(summary = "修改当前用户密码", description = "校验旧密码后修改当前登录用户密码")
    @OperLog(title = "修改当前用户密码", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @RequirePermission(skip = true)
    @PutMapping("/password")
    public ApiResult<String> updateCurrentUserPassword(@Valid @RequestBody CurrentUserPasswordUpdateReq req) {
        SysUser currentUser = SecurityUtils.getSysUser();
        if (currentUser == null || currentUser.getId() == null || currentUser.getId().isBlank()) {
            throw new SecurityException("未认证或令牌无效/过期");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }

        SysUser user = service.getById(currentUser.getId());
        if (user == null) {
            throw new SecurityException("未认证或令牌无效/过期");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalStateException("当前账号已停用");
        }
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new IllegalStateException("旧密码不正确");
        }
        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }

        boolean updated = service.updatePasswordById(user.getId(), req.getNewPassword());
        if (!updated) {
            throw new IllegalStateException("密码修改失败，请稍后再试");
        }
        userDetailsCacheService.evictUserCache(user.getUsername());
        return ApiResult.success("密码修改成功，请重新登录");
    }

    @Operation(summary = "上传当前用户头像", description = "上传当前登录用户头像并返回短期签名访问地址")
    @OperLog(title = "上传当前用户头像", businessType = BusinessType.UPLOAD)
    @RequirePermission(skip = true)
    @PostMapping(value = "/avatar/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<UserAvatarUploadResponse> uploadCurrentUserAvatar(
            @Parameter(description = "头像文件", required = true, schema = @Schema(type = "string", format = "binary"))
            @RequestParam("file") MultipartFile file) {
        SysUser user = SecurityUtils.getSysUser();
        if (user == null) {
            return ApiResult.error("User not logged in");
        }

        FileObject obj = fileManagerService.upload(file, "1", "sys_user_avatar", user.getId(), true);
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setProfileFileId(obj.id());
        service.updateById(update, true);
        user.setProfileFileId(obj.id());
        userDetailsCacheService.evictUserCache(user.getUsername());
        FileAccessDescriptor access = fileManagerService.access(obj.id(), 3600, false);
        return ApiResult.success(new UserAvatarUploadResponse(obj.id(), access.url(), access.expireAt()));
    }

    public record UserAvatarUploadResponse(String fileId, String url, Instant expireAt) {
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        int visibleLength = at > 3 ? 3 : 1;
        return email.substring(0, visibleLength) + "***" + email.substring(at);
    }

    private SysUser requireCurrentUser() {
        SysUser user = SecurityUtils.getSysUser();
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            throw new SecurityException("未认证或令牌无效/过期");
        }
        return user;
    }

    private void refreshAuthenticationPrincipal(SysUser user) {
        var loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null || loginUser.getSysUser() == null
                || !user.getId().equals(loginUser.getSysUser().getId())) {
            return;
        }
        loginUser.getSysUser().setRealName(user.getRealName());
        loginUser.getSysUser().setProfileFileId(user.getProfileFileId());
        loginUser.getSysUser().setRemark(user.getRemark());
    }

    private static String normalizeRequiredText(String value, String message) {
        String normalized = normalizeBlankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
