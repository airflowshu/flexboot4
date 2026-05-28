package com.yunlbd.flexboot4.service.sys;

import com.yunlbd.flexboot4.dto.sys.SecurityPhoneBindReq;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneBindResp;
import com.yunlbd.flexboot4.dto.sys.SecurityPhoneCodeReq;
import com.yunlbd.flexboot4.entity.sys.SysUser;

public interface UserSecurityPhoneService {

    String sendBindCode(SysUser currentUser, SecurityPhoneCodeReq req);

    SecurityPhoneBindResp bindPhone(SysUser currentUser, SecurityPhoneBindReq req);
}
