package com.yunlbd.flexboot4.security;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserService sysUserService;

    @Override
    @NullMarked
    @Cacheable(value = "userDetails", key = "#username", unless = "#result == null")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 使用服务层方法，这样可以利用缓存
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SysUser::getUsername).eq(username)
                .and(SysUser::getDelFlag).eq(0);

        SysUser sysUser = sysUserService.getOne(queryWrapper);

        if (sysUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        if (sysUser.getStatus() != null && sysUser.getStatus() == 0) {
            throw new DisabledException("User is disabled: " + username);
        }

        // 构建权限列表
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (sysUser.getRoles() != null) {
            authorities = sysUser.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getRoleValue()))
                    .collect(Collectors.toList());
        }

        return new LoginUser(sysUser, authorities);
    }
    
    /**
     * 清除用户缓存
     * @param username 用户名
     */
    @org.springframework.cache.annotation.CacheEvict(value = "userDetails", key = "#username")
    public void evictUserCache(String username) {
        // 仅用于清除缓存，实际方法体不需要任何逻辑
    }
}
