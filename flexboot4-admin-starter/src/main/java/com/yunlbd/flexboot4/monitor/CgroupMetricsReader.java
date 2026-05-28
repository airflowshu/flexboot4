package com.yunlbd.flexboot4.monitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class CgroupMetricsReader {

    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;
    private static final long NANOSECONDS_PER_MICROSECOND = 1_000L;

    private final Path root;

    public CgroupMetricsReader() {
        this(Path.of("/"));
    }

    CgroupMetricsReader(Path root) {
        this.root = root;
    }

    public Optional<CpuSnapshot> readCpuSnapshot() {
        return readCpuV2().or(this::readCpuV1);
    }

    public Optional<MemorySnapshot> readMemorySnapshot() {
        return readMemoryV2().or(this::readMemoryV1);
    }

    private Optional<CpuSnapshot> readCpuV2() {
        Optional<String> cpuMax = readString("sys/fs/cgroup/cpu.max");
        Optional<Long> usageUsec = readCpuStatUsageUsec("sys/fs/cgroup/cpu.stat");
        if (usageUsec.isEmpty()) {
            return Optional.empty();
        }

        long usageNanos = usageUsec.get() * NANOSECONDS_PER_MICROSECOND;
        Double quotaCores = cpuMax.flatMap(this::parseCpuMaxQuota).orElse(null);
        return Optional.of(new CpuSnapshot("cgroup", "v2", usageNanos, quotaCores, System.nanoTime()));
    }

    private Optional<CpuSnapshot> readCpuV1() {
        Optional<Long> usageNanos = readLongFirstExisting(
                "sys/fs/cgroup/cpuacct/cpuacct.usage",
                "sys/fs/cgroup/cpu,cpuacct/cpuacct.usage",
                "sys/fs/cgroup/cpuacct,cpu/cpuacct.usage"
        );
        if (usageNanos.isEmpty()) {
            return Optional.empty();
        }

        Optional<Long> quota = readLongFirstExisting(
                "sys/fs/cgroup/cpu/cpu.cfs_quota_us",
                "sys/fs/cgroup/cpu,cpuacct/cpu.cfs_quota_us",
                "sys/fs/cgroup/cpuacct,cpu/cpu.cfs_quota_us"
        );
        Optional<Long> period = readLongFirstExisting(
                "sys/fs/cgroup/cpu/cpu.cfs_period_us",
                "sys/fs/cgroup/cpu,cpuacct/cpu.cfs_period_us",
                "sys/fs/cgroup/cpuacct,cpu/cpu.cfs_period_us"
        );
        Double quotaCores = parseCpuQuota(quota.orElse(-1L), period.orElse(-1L)).orElse(null);
        return Optional.of(new CpuSnapshot("cgroup", "v1", usageNanos.get(), quotaCores, System.nanoTime()));
    }

    private Optional<MemorySnapshot> readMemoryV2() {
        Optional<Long> current = readLong("sys/fs/cgroup/memory.current");
        if (current.isEmpty()) {
            return Optional.empty();
        }
        MemoryLimit limit = readString("sys/fs/cgroup/memory.max")
                .map(this::parseMemoryLimit)
                .orElse(MemoryLimit.unlimitedLimit());
        return Optional.of(new MemorySnapshot("cgroup", "v2", current.get(), limit.limitBytes(), limit.unlimited()));
    }

    private Optional<MemorySnapshot> readMemoryV1() {
        Optional<Long> current = readLongFirstExisting(
                "sys/fs/cgroup/memory/memory.usage_in_bytes",
                "sys/fs/cgroup/memory.usage_in_bytes"
        );
        if (current.isEmpty()) {
            return Optional.empty();
        }
        MemoryLimit limit = readStringFirstExisting(
                "sys/fs/cgroup/memory/memory.limit_in_bytes",
                "sys/fs/cgroup/memory.limit_in_bytes"
        ).map(this::parseMemoryLimit).orElse(MemoryLimit.unlimitedLimit());
        return Optional.of(new MemorySnapshot("cgroup", "v1", current.get(), limit.limitBytes(), limit.unlimited()));
    }

    private Optional<Double> parseCpuMaxQuota(String value) {
        String[] parts = value.trim().split("\\s+");
        if (parts.length < 2 || "max".equals(parts[0])) {
            return Optional.empty();
        }
        return parseLong(parts[0]).flatMap(quota -> parseLong(parts[1]).flatMap(period -> parseCpuQuota(quota, period)));
    }

    private Optional<Double> parseCpuQuota(long quota, long period) {
        if (quota <= 0 || period <= 0) {
            return Optional.empty();
        }
        return Optional.of((double) quota / period);
    }

    private MemoryLimit parseMemoryLimit(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "max".equalsIgnoreCase(trimmed)) {
            return MemoryLimit.unlimitedLimit();
        }
        return parseLong(trimmed)
                .filter(limit -> limit > 0 && limit < Long.MAX_VALUE / 2)
                .map(MemoryLimit::limited)
                .orElseGet(MemoryLimit::unlimitedLimit);
    }

    private Optional<Long> readCpuStatUsageUsec(String path) {
        return readString(path).flatMap(content -> {
            for (String line : content.split("\\R")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 2 && "usage_usec".equals(parts[0])) {
                    return parseLong(parts[1]);
                }
            }
            return Optional.empty();
        });
    }

    private Optional<Long> readLongFirstExisting(String... paths) {
        return readStringFirstExisting(paths).flatMap(this::parseLong);
    }

    private Optional<String> readStringFirstExisting(String... paths) {
        for (String path : paths) {
            Optional<String> value = readString(path);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private Optional<Long> readLong(String path) {
        return readString(path).flatMap(this::parseLong);
    }

    private Optional<String> readString(String path) {
        Path resolved = root.resolve(path).normalize();
        if (!Files.isRegularFile(resolved)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(resolved).trim());
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public record CpuSnapshot(
            String source,
            String cgroupVersion,
            long usageNanos,
            Double quotaCores,
            long sampleNanos
    ) {
        public double effectiveCores(int fallbackCores) {
            return quotaCores != null && quotaCores > 0 ? quotaCores : Math.max(1, fallbackCores);
        }
    }

    public record MemorySnapshot(
            String source,
            String cgroupVersion,
            long usageBytes,
            Long limitBytes,
            boolean unlimited
    ) {
    }

    private record MemoryLimit(Long limitBytes, boolean unlimited) {
        static MemoryLimit limited(long limitBytes) {
            return new MemoryLimit(limitBytes, false);
        }

        static MemoryLimit unlimitedLimit() {
            return new MemoryLimit(null, true);
        }
    }
}
