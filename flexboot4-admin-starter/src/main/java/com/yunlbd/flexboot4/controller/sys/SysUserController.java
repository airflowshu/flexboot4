package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.converter.sys.SysUserCrudMapper;
import com.yunlbd.flexboot4.dto.sys.SysUserCreateReq;
import com.yunlbd.flexboot4.dto.sys.SysUserUpdateReq;
import com.yunlbd.flexboot4.entity.sys.SysRole;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.excel.sys.SysUserExportRow;
import com.yunlbd.flexboot4.excel.sys.SysUserImportRow;
import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.service.sys.FileManagerService;
import com.yunlbd.flexboot4.service.sys.SysUserService;
import com.yunlbd.flexboot4.util.SecurityUtils;
import com.yunlbd.flexboot4.vo.sys.SysUserDetailVO;
import com.yunlbd.flexboot4.vo.sys.SysUserListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final SysUserCrudMapper mapper;

    public SysUserController(SysUserService service,
                             SysUserCrudMapper mapper,
                             PasswordEncoder passwordEncoder,
                             FileManagerService fileManagerService) {
        super(service, mapper);
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.fileManagerService = fileManagerService;
    }


    @Override
    public Class<SysUser> getEntityClass() {
        return SysUser.class;
    }

    @Override
    protected SysUser beforeCreate(SysUser user, SysUserCreateReq request) {
        //新增用户，默认密码111111，
        // 重置密码需通过忘记密码，邮件方式重置;或者管理员才有操作权限reset
        user.setPassword(passwordEncoder.encode("111111"));
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
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        // 头像URL（优先取 avatar_file_id 对应的公有URL）
        if (user.getProfileFileId() != null && !user.getProfileFileId().isBlank()) {
            FileAccessDescriptor avatar = fileManagerService.access(user.getProfileFileId(), 3600, false);
            info.put("avatar", avatar.url());
        }
        info.put("roles", user.getRoles() != null
                ? user.getRoles().stream().map(SysRole::getRoleValue).collect(Collectors.toList())
                : new ArrayList<>());
        return ApiResult.success(info);
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
        FileAccessDescriptor access = fileManagerService.access(obj.id(), 3600, false);
        return ApiResult.success(new UserAvatarUploadResponse(obj.id(), access.url(), access.expireAt()));
    }

    public record UserAvatarUploadResponse(String fileId, String url, Instant expireAt) {
    }

}
