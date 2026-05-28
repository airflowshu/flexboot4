package com.yunlbd.flexboot4.monitor;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContainerRuntimeDetector {

    private static final Pattern CONTAINER_ID_PATTERN = Pattern.compile("([a-f0-9]{12,64})");

    private final Path root;

    public ContainerRuntimeDetector() {
        this(Path.of("/"));
    }

    ContainerRuntimeDetector(Path root) {
        this.root = root;
    }

    public RuntimeInfo detect() {
        List<String> cgroupLines = readCgroupLines();
        boolean dockerEnv = Files.exists(resolve(".dockerenv"));
        String runtimeType = detectRuntimeType(cgroupLines, dockerEnv);
        boolean containerized = dockerEnv || !"host".equals(runtimeType);
        String cgroupVersion = detectCgroupVersion();
        String containerId = extractContainerId(cgroupLines).orElse(null);

        return new RuntimeInfo(
                containerized,
                containerized ? runtimeType : "host",
                containerized ? "CONTAINER" : "HOST_PROCESS",
                hostname(),
                containerId,
                cgroupVersion
        );
    }

    private String detectRuntimeType(List<String> cgroupLines, boolean dockerEnv) {
        String cgroupText = String.join("\n", cgroupLines).toLowerCase(Locale.ROOT);
        if (cgroupText.contains("kubepods")) {
            return "kubernetes";
        }
        if (cgroupText.contains("containerd")) {
            return "containerd";
        }
        if (cgroupText.contains("libpod") || cgroupText.contains("podman")) {
            return "podman";
        }
        if (cgroupText.contains("docker") || dockerEnv) {
            return "docker";
        }
        if (!cgroupText.isBlank() && (cgroupText.contains("cri-containerd") || cgroupText.contains("crio"))) {
            return "containerd";
        }
        return "host";
    }

    private Optional<String> extractContainerId(List<String> cgroupLines) {
        for (String line : cgroupLines) {
            Matcher matcher = CONTAINER_ID_PATTERN.matcher(line.toLowerCase(Locale.ROOT));
            String last = null;
            while (matcher.find()) {
                last = matcher.group(1);
            }
            if (last != null) {
                return Optional.of(last);
            }
        }
        return Optional.empty();
    }

    private List<String> readCgroupLines() {
        List<String> lines = new ArrayList<>();
        lines.addAll(readLines("proc/1/cgroup"));
        lines.addAll(readLines("proc/self/cgroup"));
        return lines;
    }

    private String detectCgroupVersion() {
        if (Files.exists(resolve("sys/fs/cgroup/cgroup.controllers"))) {
            return "v2";
        }
        if (Files.exists(resolve("sys/fs/cgroup/memory")) || Files.exists(resolve("sys/fs/cgroup/cpu"))) {
            return "v1";
        }
        return "unknown";
    }

    private List<String> readLines(String path) {
        try {
            Path resolved = resolve(path);
            return Files.exists(resolved) ? Files.readAllLines(resolved) : List.of();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return System.getenv().getOrDefault("HOSTNAME", "unknown");
        }
    }

    private Path resolve(String path) {
        return root.resolve(path).normalize();
    }

    public record RuntimeInfo(
            boolean containerized,
            String runtimeType,
            String scope,
            String hostname,
            String containerId,
            String cgroupVersion
    ) {
    }
}
