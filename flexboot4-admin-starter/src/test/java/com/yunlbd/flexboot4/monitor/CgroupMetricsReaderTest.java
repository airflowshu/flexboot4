package com.yunlbd.flexboot4.monitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CgroupMetricsReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsCgroupV2CpuAndMemory() throws Exception {
        write("sys/fs/cgroup/cpu.max", "200000 100000");
        write("sys/fs/cgroup/cpu.stat", "usage_usec 123456\nuser_usec 1000\nsystem_usec 2000\n");
        write("sys/fs/cgroup/memory.max", "536870912");
        write("sys/fs/cgroup/memory.current", "268435456");

        CgroupMetricsReader reader = new CgroupMetricsReader(tempDir);

        CgroupMetricsReader.CpuSnapshot cpu = reader.readCpuSnapshot().orElseThrow();
        CgroupMetricsReader.MemorySnapshot memory = reader.readMemorySnapshot().orElseThrow();

        assertThat(cpu.source()).isEqualTo("cgroup");
        assertThat(cpu.cgroupVersion()).isEqualTo("v2");
        assertThat(cpu.usageNanos()).isEqualTo(123_456_000L);
        assertThat(cpu.quotaCores()).isEqualTo(2.0D);
        assertThat(memory.source()).isEqualTo("cgroup");
        assertThat(memory.cgroupVersion()).isEqualTo("v2");
        assertThat(memory.usageBytes()).isEqualTo(268_435_456L);
        assertThat(memory.limitBytes()).isEqualTo(536_870_912L);
        assertThat(memory.unlimited()).isFalse();
    }

    @Test
    void treatsCgroupV2UnlimitedValuesAsUnlimited() throws Exception {
        write("sys/fs/cgroup/cpu.max", "max 100000");
        write("sys/fs/cgroup/cpu.stat", "usage_usec 10\n");
        write("sys/fs/cgroup/memory.max", "max");
        write("sys/fs/cgroup/memory.current", "42");

        CgroupMetricsReader reader = new CgroupMetricsReader(tempDir);

        CgroupMetricsReader.CpuSnapshot cpu = reader.readCpuSnapshot().orElseThrow();
        CgroupMetricsReader.MemorySnapshot memory = reader.readMemorySnapshot().orElseThrow();

        assertThat(cpu.quotaCores()).isNull();
        assertThat(memory.unlimited()).isTrue();
        assertThat(memory.limitBytes()).isNull();
    }

    @Test
    void readsCgroupV1CpuAndMemory() throws Exception {
        write("sys/fs/cgroup/cpuacct/cpuacct.usage", "9000000000");
        write("sys/fs/cgroup/cpu/cpu.cfs_quota_us", "50000");
        write("sys/fs/cgroup/cpu/cpu.cfs_period_us", "100000");
        write("sys/fs/cgroup/memory/memory.usage_in_bytes", "1048576");
        write("sys/fs/cgroup/memory/memory.limit_in_bytes", "2097152");

        CgroupMetricsReader reader = new CgroupMetricsReader(tempDir);

        CgroupMetricsReader.CpuSnapshot cpu = reader.readCpuSnapshot().orElseThrow();
        CgroupMetricsReader.MemorySnapshot memory = reader.readMemorySnapshot().orElseThrow();

        assertThat(cpu.cgroupVersion()).isEqualTo("v1");
        assertThat(cpu.usageNanos()).isEqualTo(9_000_000_000L);
        assertThat(cpu.quotaCores()).isEqualTo(0.5D);
        assertThat(memory.cgroupVersion()).isEqualTo("v1");
        assertThat(memory.usageBytes()).isEqualTo(1_048_576L);
        assertThat(memory.limitBytes()).isEqualTo(2_097_152L);
    }

    private void write(String path, String value) throws Exception {
        Path target = tempDir.resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, value);
    }
}
