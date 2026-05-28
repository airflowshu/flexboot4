package com.yunlbd.flexboot4.monitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ContainerRuntimeDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsHostProcessWhenNoContainerMarkersExist() {
        ContainerRuntimeDetector.RuntimeInfo info = new ContainerRuntimeDetector(tempDir).detect();

        assertThat(info.containerized()).isFalse();
        assertThat(info.runtimeType()).isEqualTo("host");
        assertThat(info.scope()).isEqualTo("HOST_PROCESS");
        assertThat(info.containerId()).isNull();
    }

    @Test
    void detectsDockerFromDockerEnvAndCgroup() throws Exception {
        Files.createFile(tempDir.resolve(".dockerenv"));
        write("proc/1/cgroup", "0::/docker/1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\n");
        write("sys/fs/cgroup/cgroup.controllers", "cpu memory io");

        ContainerRuntimeDetector.RuntimeInfo info = new ContainerRuntimeDetector(tempDir).detect();

        assertThat(info.containerized()).isTrue();
        assertThat(info.runtimeType()).isEqualTo("docker");
        assertThat(info.scope()).isEqualTo("CONTAINER");
        assertThat(info.cgroupVersion()).isEqualTo("v2");
        assertThat(info.containerId()).isEqualTo("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
    }

    @Test
    void detectsKubernetesRuntimeFromCgroup() throws Exception {
        write("proc/self/cgroup", "0::/kubepods.slice/kubepods-burstable.slice/cri-containerd-abcdef1234567890.scope\n");

        ContainerRuntimeDetector.RuntimeInfo info = new ContainerRuntimeDetector(tempDir).detect();

        assertThat(info.containerized()).isTrue();
        assertThat(info.runtimeType()).isEqualTo("kubernetes");
        assertThat(info.scope()).isEqualTo("CONTAINER");
        assertThat(info.containerId()).isEqualTo("abcdef1234567890");
    }

    private void write(String path, String value) throws Exception {
        Path target = tempDir.resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, value);
    }
}
