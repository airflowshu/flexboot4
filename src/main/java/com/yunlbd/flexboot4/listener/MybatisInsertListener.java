package com.yunlbd.flexboot4.listener;

import com.mybatisflex.annotation.InsertListener;
import com.yunlbd.flexboot4.common.util.SecurityUtils;
import com.yunlbd.flexboot4.entity.BaseEntity;
import com.yunlbd.flexboot4.entity.SysUser;

public class MybatisInsertListener implements InsertListener {
    @Override
    public void onInsert(Object o) {
        SysUser sysUser = SecurityUtils.getSysUser();
        String username = sysUser != null ? sysUser.getUsername() : null;
        if (username != null && o instanceof BaseEntity entity) {
            entity.setCreateBy(username);
        }
    }
}
