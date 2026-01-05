package com.yunlbd.flexboot4.security;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.SysRole;
import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
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

    private final SysUserMapper sysUserMapper;

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Use selectOneWithRelationsByQuery to fetch roles eagerly
        SysUser sysUser = sysUserMapper.selectOneWithRelationsByQuery(
                QueryWrapper.create()
                        .where(SysUser::getUsername).eq(username)
                        .and(SysUser::getDelFlag).eq(0)
        );

        if (sysUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        if (sysUser.getStatus() != null && sysUser.getStatus() == 0) {
            throw new DisabledException("User is disabled: " + username);
        }
        
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (sysUser.getRoles() != null) {
            authorities = sysUser.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getRoleValue()))
                    .collect(Collectors.toList());
        }

        return new LoginUser(sysUser, authorities);
    }
}
