package com.yunlbd.flexboot4.aigateway.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PgVectorUtilsTest {

    @Test
    void toPgVectorString_empty() {
        assertEquals("[]", PgVectorUtils.toPgVectorString(List.of()));
    }

    @Test
    void toPgVectorString_values() {
        assertEquals("[1.0, 2.5, 3.0]", PgVectorUtils.toPgVectorString(List.of(1.0f, 2.5f, 3.0f)));
    }
}
