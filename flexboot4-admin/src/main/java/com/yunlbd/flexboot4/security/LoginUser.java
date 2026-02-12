package com.yunlbd.flexboot4.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yunlbd.flexboot4.common.constant.SysConstant;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser implements UserDetails {

    private SysUser sysUser;
    private Collection<? extends GrantedAuthority> authorities;

    /** 用户拥有的权限码列表 */
    private List<String> permissionCodes;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return sysUser.getPassword();
    }

    @Override
    public String getUsername() {
        return sysUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return sysUser.getStatus() != null && sysUser.getStatus() == 1;
    }

    /**
     * 检查是否拥有指定权限码
     */
    public boolean hasPermission(String code) {
        return permissionCodes != null && permissionCodes.contains(code);
    }

    /**
     * 是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return SysConstant.SYS_SUPER_USER_ID.equals(sysUser.getId());
    }
}
