package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
import java.util.Map;

/**
 * 系统监控控制器
 *
 * @author Wangts
 * @Project_Name flexboot4
 * @since 2026年02月10日
 */
@RestController
@RequestMapping("/api/admin/monitor")
@Tag(name = "系统监控", description = "系统监控 - 系统性能监控")
@ApiTagGroup(group = "运维管理")
public class SysMonitorController {

    private static final SystemInfo SYSTEM_INFO = new SystemInfo();
    private static final long CPU_SAMPLE_INTERVAL_MS = 1000L;
    private static volatile long[] lastCpuTicks = SYSTEM_INFO.getHardware().getProcessor().getSystemCpuLoadTicks();
    private static volatile long lastCpuSampleMillis = System.currentTimeMillis();
    private static volatile double lastCpuLoad = 0.0D;

    /**
     * 获取系统监控统计信息
     */
    @RequirePermission("sys:monitor:stats")
    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> getStats() {
        Map<String, Object> result = new LinkedHashMap<>();

        // CPU信息
        result.put("cpu", getCpuInfo());

        // 内存信息
        result.put("memory", getMemoryInfo());

        // JVM信息
        result.put("jvm", getJvmInfo());

        // 磁盘信息
        result.put("disks", getDiskInfo());

        // 线程信息
        result.put("threads", getThreadInfo());

        return ApiResult.success(result);
    }

    /**
     * 获取CPU信息
     */
    private Map<String, Object> getCpuInfo() {
        HardwareAbstractionLayer hal = SYSTEM_INFO.getHardware();
        CentralProcessor processor = hal.getProcessor();

        long now = System.currentTimeMillis();
        if (now - lastCpuSampleMillis >= CPU_SAMPLE_INTERVAL_MS) {
            lastCpuLoad = processor.getSystemCpuLoadBetweenTicks(lastCpuTicks);
            lastCpuTicks = processor.getSystemCpuLoadTicks();
            lastCpuSampleMillis = now;
        }
        double cpuLoad = Math.max(0D, Math.min(lastCpuLoad, 1D));

        Map<String, Object> cpuInfo = new LinkedHashMap<>();
        cpuInfo.put("usage", Math.round(cpuLoad * 100 * 10) / 10.0);
        cpuInfo.put("cores", processor.getLogicalProcessorCount());
        cpuInfo.put("physicalCores", processor.getPhysicalProcessorCount());

        long maxFreq = processor.getMaxFreq();
        cpuInfo.put("frequency", maxFreq > 0 ? FormatUtil.formatHertz(maxFreq) : "N/A");

        double userUsage = cpuLoad * 100;
        cpuInfo.put("userUsage", String.format("%.1f%%", userUsage));

        return cpuInfo;
    }

    /**
     * 获取内存信息
     */
    private Map<String, Object> getMemoryInfo() {
        HardwareAbstractionLayer hal = SYSTEM_INFO.getHardware();
        GlobalMemory memory = hal.getMemory();

        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        double usage = (double) used / total * 100;

        Map<String, Object> memInfo = new LinkedHashMap<>();
        memInfo.put("usage", Math.round(usage * 10) / 10.0);
        memInfo.put("total", FormatUtil.formatBytes(total));
        memInfo.put("used", FormatUtil.formatBytes(used));
        memInfo.put("available", FormatUtil.formatBytes(available));

        return memInfo;
    }

    /**
     * 获取JVM信息
     */
    private Map<String, Object> getJvmInfo() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - runtime.freeMemory();

        // 非堆内存
        long nonHeapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        // 近似非堆使用（堆外内存估算）
        long nonHeapCommitted = memoryMXBean.getNonHeapMemoryUsage().getCommitted();
        long nonHeapUsedActual = memoryMXBean.getNonHeapMemoryUsage().getUsed();

        double usage = (double) usedMemory / maxMemory * 100;

        Map<String, Object> jvmInfo = new LinkedHashMap<>();
        jvmInfo.put("usage", Math.round(usage * 10) / 10.0);
        jvmInfo.put("heapUsed", FormatUtil.formatBytes(usedMemory));
        jvmInfo.put("heapTotal", FormatUtil.formatBytes(totalMemory));
        jvmInfo.put("nonHeapUsed", FormatUtil.formatBytes(nonHeapUsedActual));
        jvmInfo.put("nonHeapTotal", FormatUtil.formatBytes(nonHeapCommitted > 0 ? nonHeapCommitted : nonHeapMax));
        jvmInfo.put("version", System.getProperty("java.version"));
        jvmInfo.put("vendor", System.getProperty("java.vendor"));

        return jvmInfo;
    }

    /**
     * 获取磁盘信息
     */
    private List<Map<String, Object>> getDiskInfo() {
        List<Map<String, Object>> disks = new ArrayList<>();
        OperatingSystem os = SYSTEM_INFO.getOperatingSystem();
        FileSystem fileSystem = os.getFileSystem();

        List<OSFileStore> fileStores = fileSystem.getFileStores();
        for (OSFileStore fs : fileStores) {
            long total = fs.getTotalSpace();
            long usable = fs.getUsableSpace();
            long used = total - usable;

            if (total <= 0) continue;

            double usage = (double) used / total * 100;

            Map<String, Object> disk = new LinkedHashMap<>();
            disk.put("path", fs.getMount());
            disk.put("fsType", fs.getType());
            disk.put("usage", Math.round(usage * 10) / 10.0);
            disk.put("total", FormatUtil.formatBytes(total));
            disk.put("used", FormatUtil.formatBytes(used));

            disks.add(disk);
        }

        return disks;
    }

    /**
     * 获取线程信息
     */
    private Map<String, Object> getThreadInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        int activeCount = threadMXBean.getThreadCount();
        long peakCount = threadMXBean.getPeakThreadCount();

        // 获取线程状态统计
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

        return threadInfo;
    }
}
