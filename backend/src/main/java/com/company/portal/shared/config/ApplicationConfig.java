package com.company.portal.shared.config;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * General-purpose infrastructure beans that don't belong to a more specific
 * config class. Only wires beans; no controllers or repositories.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }

    /**
     * Cryptographically strong random. Uses the LXM family which is fast and
     * high-quality; when actual crypto randomness is needed, use
     * {@code java.security.SecureRandom} directly instead of this bean.
     */
    @Bean
    public RandomGenerator randomGenerator() {
        return RandomGenerator.of("L64X128MixRandom");
    }

    @Bean(name = "applicationTaskExecutor")
    public TaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("portal-async-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
