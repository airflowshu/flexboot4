package com.yunlbd.flexboot4.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthenticationManagerWiringTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void authenticationManagerBeanExists() {
        assertThat(authenticationManager).isNotNull();
        assertThat(userDetailsService).isNotNull();
    }
}

