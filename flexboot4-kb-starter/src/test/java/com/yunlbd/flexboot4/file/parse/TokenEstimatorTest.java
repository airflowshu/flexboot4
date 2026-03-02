package com.yunlbd.flexboot4.file.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenEstimatorTest {

    @Test
    void estimateTokensShouldBePositive() {
        int a = TokenEstimator.estimateTokens("hello world");
        int b = TokenEstimator.estimateTokens("你好世界");
        assertTrue(a > 0);
        assertTrue(b > 0);
    }
}

