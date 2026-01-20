package com.yunlbd.flexboot4.listener;

import com.mybatisflex.annotation.InsertListener;
import com.yunlbd.flexboot4.util.SecurityUtils;
import com.yunlbd.flexboot4.entity.BaseEntity;
import com.yunlbd.flexboot4.entity.SysUser;

public class MybatisInsertListener implements InsertListener {
    @Override
    public void onInsert(Object o) {
        SysUser sysUser = SecurityUtils.getSysUser();
        String userId = sysUser != null ? sysUser.getId() : null;
        if (userId != null && o instanceof BaseEntity entity) {
            entity.setCreateBy(userId);
        }
    }
}
