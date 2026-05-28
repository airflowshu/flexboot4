package com.yunlbd.flexboot4.service.sys;

import com.yunlbd.flexboot4.dto.sys.SecurityEmailBindReq;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailBindResp;
import com.yunlbd.flexboot4.dto.sys.SecurityEmailCodeReq;
import com.yunlbd.flexboot4.entity.sys.SysUser;

public interface UserSecurityEmailService {

    String sendBindCode(SysUser currentUser, SecurityEmailCodeReq req);

    SecurityEmailBindResp bindEmail(SysUser currentUser, SecurityEmailBindReq req);
}
