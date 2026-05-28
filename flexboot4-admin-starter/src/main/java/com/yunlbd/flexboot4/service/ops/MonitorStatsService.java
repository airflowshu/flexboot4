package com.yunlbd.flexboot4.service.ops;

import com.yunlbd.flexboot4.monitor.CgroupMetricsReader;
import com.yunlbd.flexboot4.monitor.ContainerRuntimeDetector;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MonitorStatsService {

    private static final SystemInfo SYSTEM_INFO = new SystemInfo();
    private static final long CPU_SAMPLE_INTERVAL_MS = 1000L;

    private final CgroupMetricsReader cgroupMetricsReader;
    private final ContainerRuntimeDetector containerRuntimeDetector;

    private volatile long[] lastCpuTicks = SYSTEM_INFO.getHardware().getProcessor().getSystemCpuLoadTicks();
    private volatile long lastCpuSampleMillis = System.currentTimeMillis();
    private volatile double lastCpuLoad = 0.0D;
    private volatile CgroupMetricsReader.CpuSnapshot lastCgroupCpuSnapshot;
    private volatile double lastCgroupCpuLoad = 0.0D;

    public MonitorStatsService() {
        this(new CgroupMetricsReader(), new ContainerRuntimeDetector());
    }

    MonitorStatsService(CgroupMetricsReader cgroupMetricsReader, ContainerRuntimeDetector containerRuntimeDetector) {
        this.cgroupMetricsReader = cgroupMetricsReader;
        this.containerRuntimeDetector = containerRuntimeDetector;
    }

    public Map<String, Object> getStats() {
        ContainerRuntimeDetector.RuntimeInfo runtime = containerRuntimeDetector.detect();

        Map<String, Object> cpuInfo = getCpuInfo(runtime);
        Map<String, Object> memoryInfo = getMemoryInfo(runtime);
        Map<String, Object> jvmInfo = getJvmInfo();
        List<Map<String, Object>> disks = getDiskInfo(runtime);
        Map<String, Object> threadInfo = getThreadInfo();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("runtime", getRuntimeInfo(runtime, cpuInfo, memoryInfo));
        result.put("cpu", cpuInfo);
        result.put("memory", memoryInfo);
        result.put("jvm", jvmInfo);
        result.put("disks", disks);
        result.put("threads", threadInfo);

        return result;
    }

    private Map<String, Object> getRuntimeInfo(
            ContainerRuntimeDetector.RuntimeInfo runtime,
            Map<String, Object> cpuInfo,
            Map<String, Object> memoryInfo
    ) {
        Map<String, Object> metricSources = new LinkedHashMap<>();
        metricSources.put("cpu", cpuInfo.getOrDefault("source", "oshi"));
        metricSources.put("memory", memoryInfo.getOrDefault("source", "oshi"));
        metricSources.put("disk", runtime.containerized() ? "mount-namespace" : "oshi");
        metricSources.put("jvm", "jvm");
        metricSources.put("thread", "jvm");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("containerized", runtime.containerized());
        info.put("runtimeType", runtime.runtimeType());
        info.put("scope", runtime.scope());
        info.put("hostname", runtime.hostname());
        info.put("containerId", runtime.containerId());
        info.put("cgroupVersion", runtime.cgroupVersion());
        info.put("metricSources", metricSources);
        info.put("note", runtime.containerized()
                ? "当前指标仅代表后端容器可见资源，不是宿主机全量资源。磁盘列表来自容器 mount namespace 可见挂载。"
                : "当前指标来自后端进程所在主机视角。Docker 部署时将自动切换为容器视角。");
        return info;
    }

    private Map<String, Object> getCpuInfo(ContainerRuntimeDetector.RuntimeInfo runtime) {
        HardwareAbstractionLayer hal = SYSTEM_INFO.getHardware();
        CentralProcessor processor = hal.getProcessor();

        Optional<CgroupMetricsReader.CpuSnapshot> cgroupCpu = runtime.containerized()
                ? cgroupMetricsReader.readCpuSnapshot()
                : Optional.empty();

        if (cgroupCpu.isPresent()) {
            CgroupMetricsReader.CpuSnapshot current = cgroupCpu.get();
            CgroupMetricsReader.CpuSnapshot previous = lastCgroupCpuSnapshot;
            if (previous != null && current.sampleNanos() > previous.sampleNanos()) {
                long usageDelta = Math.max(0L, current.usageNanos() - previous.usageNanos());
                long elapsedDelta = current.sampleNanos() - previous.sampleNanos();
                double effectiveCores = current.effectiveCores(processor.getLogicalProcessorCount());
                lastCgroupCpuLoad = Math.min(1D, usageDelta / (elapsedDelta * effectiveCores));
            }
            lastCgroupCpuSnapshot = current;

            return buildCpuInfo(processor, Math.max(0D, lastCgroupCpuLoad), "cgroup", current.cgroupVersion(), current.quotaCores());
        }

        return buildCpuInfo(processor, getOshiCpuLoad(processor), "oshi", null, null);
    }

    private double getOshiCpuLoad(CentralProcessor processor) {
        long now = System.currentTimeMillis();
        if (now - lastCpuSampleMillis >= CPU_SAMPLE_INTERVAL_MS) {
            lastCpuLoad = processor.getSystemCpuLoadBetweenTicks(lastCpuTicks);
            lastCpuTicks = processor.getSystemCpuLoadTicks();
            lastCpuSampleMillis = now;
        }
        return Math.max(0D, Math.min(lastCpuLoad, 1D));
    }

    private Map<String, Object> buildCpuInfo(
            CentralProcessor processor,
            double cpuLoad,
            String source,
            String cgroupVersion,
            Double quotaCores
    ) {
        int logicalCores = processor.getLogicalProcessorCount();
        Map<String, Object> cpuInfo = new LinkedHashMap<>();
        cpuInfo.put("usage", roundOne(cpuLoad * 100));
        cpuInfo.put("cores", quotaCores != null && quotaCores > 0 ? roundOne(quotaCores) : logicalCores);
        cpuInfo.put("physicalCores", processor.getPhysicalProcessorCount());

        long maxFreq = processor.getMaxFreq();
        cpuInfo.put("frequency", maxFreq > 0 ? FormatUtil.formatHertz(maxFreq) : "N/A");
        cpuInfo.put("userUsage", String.format(Locale.ROOT, "%.1f%%", cpuLoad * 100));
        cpuInfo.put("source", source);
        cpuInfo.put("cgroupVersion", cgroupVersion);
        cpuInfo.put("quotaCores", quotaCores == null ? null : roundOne(quotaCores));

        return cpuInfo;
    }

    private Map<String, Object> getMemoryInfo(ContainerRuntimeDetector.RuntimeInfo runtime) {
        Optional<CgroupMetricsReader.MemorySnapshot> memory = runtime.containerized()
                ? cgroupMetricsReader.readMemorySnapshot()
                : Optional.empty();
        if (memory.isPresent()) {
            CgroupMetricsReader.MemorySnapshot snapshot = memory.get();
            if (!snapshot.unlimited() && snapshot.limitBytes() != null && snapshot.limitBytes() > 0) {
                long total = snapshot.limitBytes();
                long used = Math.min(snapshot.usageBytes(), total);
                long available = Math.max(0L, total - used);
                return buildMemoryInfo(total, used, available, "cgroup", snapshot.cgroupVersion(), "limited");
            }

            Map<String, Object> fallback = getOshiMemoryInfo();
            fallback.put("source", "oshi");
            fallback.put("cgroupVersion", snapshot.cgroupVersion());
            fallback.put("limit", "unlimited");
            return fallback;
        }

        return getOshiMemoryInfo();
    }

    private Map<String, Object> getOshiMemoryInfo() {
        HardwareAbstractionLayer hal = SYSTEM_INFO.getHardware();
        GlobalMemory memory = hal.getMemory();

        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        return buildMemoryInfo(total, used, available, "oshi", null, null);
    }

    private Map<String, Object> buildMemoryInfo(
            long total,
            long used,
            long available,
            String source,
            String cgroupVersion,
            String limit
    ) {
        double usage = total > 0 ? (double) used / total * 100 : 0;

        Map<String, Object> memInfo = new LinkedHashMap<>();
        memInfo.put("usage", roundOne(usage));
        memInfo.put("total", FormatUtil.formatBytes(total));
        memInfo.put("used", FormatUtil.formatBytes(used));
        memInfo.put("available", FormatUtil.formatBytes(available));
        memInfo.put("source", source);
        memInfo.put("cgroupVersion", cgroupVersion);
        memInfo.put("limit", limit);

        return memInfo;
    }

    private Map<String, Object> getJvmInfo() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - runtime.freeMemory();

        long nonHeapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long nonHeapCommitted = memoryMXBean.getNonHeapMemoryUsage().getCommitted();
        long nonHeapUsedActual = memoryMXBean.getNonHeapMemoryUsage().getUsed();

        double usage = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0;

        Map<String, Object> jvmInfo = new LinkedHashMap<>();
        jvmInfo.put("usage", roundOne(usage));
        jvmInfo.put("heapUsed", FormatUtil.formatBytes(usedMemory));
        jvmInfo.put("heapTotal", FormatUtil.formatBytes(totalMemory));
        jvmInfo.put("nonHeapUsed", FormatUtil.formatBytes(nonHeapUsedActual));
        jvmInfo.put("nonHeapTotal", FormatUtil.formatBytes(nonHeapCommitted > 0 ? nonHeapCommitted : nonHeapMax));
        jvmInfo.put("version", System.getProperty("java.version"));
        jvmInfo.put("vendor", System.getProperty("java.vendor"));
        jvmInfo.put("source", "jvm");

        return jvmInfo;
    }

    private List<Map<String, Object>> getDiskInfo(ContainerRuntimeDetector.RuntimeInfo runtime) {
        List<Map<String, Object>> disks = new ArrayList<>();
        OperatingSystem os = SYSTEM_INFO.getOperatingSystem();
        FileSystem fileSystem = os.getFileSystem();

        List<OSFileStore> fileStores = fileSystem.getFileStores();
        for (OSFileStore fs : fileStores) {
            long total = fs.getTotalSpace();
            long usable = fs.getUsableSpace();
            long used = total - usable;

            if (total <= 0) {
                continue;
            }

            double usage = (double) used / total * 100;

            Map<String, Object> disk = new LinkedHashMap<>();
            disk.put("path", fs.getMount());
            disk.put("fsType", fs.getType());
            disk.put("usage", roundOne(usage));
            disk.put("total", FormatUtil.formatBytes(total));
            disk.put("used", FormatUtil.formatBytes(used));
            disk.put("scope", runtime.containerized() ? "container-visible" : "host-visible");
            disk.put("source", runtime.containerized() ? "mount-namespace" : "oshi");

            disks.add(disk);
        }

        return disks;
    }

    private Map<String, Object> getThreadInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        int activeCount = threadMXBean.getThreadCount();
        long peakCount = threadMXBean.getPeakThreadCount();

        Map<String, Integer> states = new LinkedHashMap<>();
        states.put("running", 0);
        states.put("waiting", 0);
        states.put("blocked", 0);
        states.put("timedWaiting", 0);

        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        for (ThreadInfo info : threadInfos) {
            Thread.State state = info.getThreadState();
            switch (state) {
                case RUNNABLE:
                    states.put("running", states.get("running") + 1);
                    break;
                case WAITING:
                    states.put("waiting", states.get("waiting") + 1);
                    break;
                case TIMED_WAITING:
                    states.put("timedWaiting", states.get("timedWaiting") + 1);
                    break;
                case BLOCKED:
                    states.put("blocked", states.get("blocked") + 1);
                    break;
                default:
                    break;
            }
        }

        Map<String, Object> threadInfo = new LinkedHashMap<>();
        threadInfo.put("active", activeCount);
        threadInfo.put("peak", (int) peakCount);
        threadInfo.put("states", states);
        threadInfo.put("source", "jvm");

        return threadInfo;
    }

    private double roundOne(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
