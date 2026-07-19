package com.company.portal.shared.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.TimeZone;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Portal-wide JSON conventions for Spring Boot 4 / Jackson 3.
 * Date/time timestamp behaviour moved to {@link DateTimeFeature} in Jackson 3.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer portalJsonMapperBuilderCustomizer() {
        return builder -> builder
                .changeDefaultPropertyInclusion(inc -> inc.withValueInclusion(JsonInclude.Include.NON_NULL))
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .disable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .defaultTimeZone(TimeZone.getTimeZone("UTC"));
    }
}
