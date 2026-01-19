package com.yunlbd.flexboot4.listener;

import com.mybatisflex.annotation.UpdateListener;
import com.yunlbd.flexboot4.common.util.SecurityUtils;
import com.yunlbd.flexboot4.entity.BaseEntity;
import com.yunlbd.flexboot4.entity.SysUser;

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
