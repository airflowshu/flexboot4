package com.yunlbd.flexboot4.listener;

import com.mybatisflex.annotation.UpdateListener;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.util.SecurityUtils;

public class MybatisUpdateListener implements UpdateListener {
    @Override
    public void onUpdate(Object o) {
        SysUser sysUser = SecurityUtils.getSysUser();
        String userId = sysUser != null ? sysUser.getId() : null;
        if (userId != null && o instanceof BaseEntity entity) {
            entity.setLastModifyBy(userId);
        }
    }
}
