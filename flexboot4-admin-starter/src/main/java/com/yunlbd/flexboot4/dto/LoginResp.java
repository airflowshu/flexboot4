package com.yunlbd.flexboot4.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginResp {
    private String id;
    private String username;
    private String realName;
    private String password; // Not recommended for security, but requested by requirement
    private String accessToken;
    private List<String> roles;
    private Boolean mfaRequired;
    private String mfaChallengeToken;
    private List<String> mfaMethods;
    private Long expiresIn;
}
