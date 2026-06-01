package com.yunlbd.flexboot4.task;

import com.yunlbd.flexboot4.lock.DistributedLockService;
import com.yunlbd.flexboot4.util.LogTableUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SysOperLogTaskTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final DistributedLockService distributedLockService = mock(DistributedLockService.class);
    private final SysOperLogTask task = new SysOperLogTask(jdbcTemplate, distributedLockService);

    @Test
    void initCheckShouldCreateCurrentAndNextQuarterTables() {
        doAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return true;
        }).when(distributedLockService).executeIfLocked(eq("admin:operlog:init-check"), any(Duration.class), any(Runnable.class));

        task.initCheck();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, org.mockito.Mockito.times(2)).execute(sqlCaptor.capture());
        assertThat(sqlCaptor.getAllValues())
                .contains(
                        "CREATE TABLE IF NOT EXISTS " + LogTableUtils.getCurrentQuarterTableName() + " (LIKE sys_oper_log INCLUDING ALL)",
                        "CREATE TABLE IF NOT EXISTS " + LogTableUtils.getNextQuarterTableName() + " (LIKE sys_oper_log INCLUDING ALL)"
                );
    }
}
