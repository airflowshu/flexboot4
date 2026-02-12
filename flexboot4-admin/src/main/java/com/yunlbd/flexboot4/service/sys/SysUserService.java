package com.yunlbd.flexboot4.service.sys;

import com.yunlbd.flexboot4.entity.sys.SysUser;

/**
 *
 * @author Wangts
 * @Project_Name flexboot4
 * @since 2026年01月07日 12:38
 */
public interface SysUserService extends IExtendedService<SysUser> {

    boolean updatePasswordById(String id, String newPassword);
}
