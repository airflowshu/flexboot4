package com.yunlbd.flexboot4.auth.jwt;

public final class JwtClaimKeys {
    public static final String SUBJECT = "sub";
    public static final String USERNAME = "username";
    public static final String ROLES = "roles";
    public static final String PERMISSIONS = "permissions";
    public static final String SCOPE = "scope";
    public static final String ISSUED_AT = "iat";
    public static final String EXPIRES_AT = "exp";

    private JwtClaimKeys() {
    }
}
