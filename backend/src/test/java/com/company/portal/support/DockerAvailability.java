package com.company.portal.support;

import java.util.concurrent.TimeUnit;
import org.testcontainers.DockerClientFactory;

/**
 * Lightweight probe to decide whether a test that needs Docker can run.
 * Used with JUnit {@code @EnabledIf} / Assumptions to skip gracefully when
 * the Docker daemon is unreachable or cannot actually start containers
 * (e.g. broken overlay mounts in some Cloud Agent environments).
 */
public final class DockerAvailability {

    private static volatile Boolean cached;

    private DockerAvailability() { }

    public static boolean isAvailable() {
        Boolean value = cached;
        if (value != null) {
            return value;
        }
        synchronized (DockerAvailability.class) {
            if (cached == null) {
                cached = probe();
            }
            return cached;
        }
    }

    private static boolean probe() {
        try {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
        }
        // DockerClientFactory can report "available" when `docker info` works
        // but container creation fails (overlay mount errors). Probe a real run.
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm", "alpine:3.20", "echo", "portal-docker-ok");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(90, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
