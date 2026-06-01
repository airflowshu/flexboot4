package com.yunlbd.flexboot4.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LogTableUtilsTest {

    @Test
    void getQuarterTableNameShouldResolveByDate() {
        assertThat(LogTableUtils.getQuarterTableName(LocalDateTime.of(2026, 1, 1, 0, 0)))
                .isEqualTo("sys_oper_log_2026_q1");
        assertThat(LogTableUtils.getQuarterTableName(LocalDateTime.of(2026, 12, 31, 23, 59)))
                .isEqualTo("sys_oper_log_2026_q4");
    }

    @Test
    void getNextQuarterTableNameShouldSkipToNextQuarterStart() {
        assertThat(LogTableUtils.getNextQuarterTableName(LocalDate.of(2026, 5, 1)))
                .isEqualTo("sys_oper_log_2026_q3");
        assertThat(LogTableUtils.getNextQuarterTableName(LocalDate.of(2026, 12, 25)))
                .isEqualTo("sys_oper_log_2027_q1");
    }
}
