package com.yunlbd.flexboot4.service.sys;

import com.yunlbd.flexboot4.dto.sys.UserMfaTotpConfirmReq;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpDisableReq;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpSetupResp;
import com.yunlbd.flexboot4.dto.sys.UserMfaTotpStatusResp;
import com.yunlbd.flexboot4.entity.sys.SysUser;

public interface UserMfaService {

    UserMfaTotpSetupResp setupTotp(SysUser currentUser);

    UserMfaTotpStatusResp confirmTotp(SysUser currentUser, UserMfaTotpConfirmReq req);

    UserMfaTotpStatusResp disableTotp(SysUser currentUser, UserMfaTotpDisableReq req);

    UserMfaTotpStatusResp getTotpStatus(String userId);

    boolean isTotpEnabled(String userId);

    boolean verifyTotp(String userId, String code);
}
