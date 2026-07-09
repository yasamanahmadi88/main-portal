package com.company.portal.support;

import org.testcontainers.DockerClientFactory;

/**
 * Lightweight probe to decide whether a test that needs Docker can run.
 * Used with JUnit {@code Assumptions.assumeTrue(...)} to skip gracefully in
 * environments where the Docker daemon is not reachable.
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
                boolean available;
                try {
                    available = DockerClientFactory.instance().isDockerAvailable();
                } catch (RuntimeException e) {
                    available = false;
                }
                cached = available;
            }
            return cached;
        }
    }
}
