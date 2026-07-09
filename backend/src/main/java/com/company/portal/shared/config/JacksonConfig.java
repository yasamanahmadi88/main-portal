package com.company.portal.shared.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.TimeZone;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

/**
 * Portal-wide JSON conventions layered on top of Spring Boot's Jackson 3 auto-
 * configuration. Complements the {@code spring.jackson.*} keys in
 * {@code application.yml}; anything that cannot be expressed there lives here.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer portalJsonMapperBuilderCustomizer() {
        return builder -> builder
                .changeDefaultPropertyInclusion(inc -> inc.withValueInclusion(JsonInclude.Include.NON_NULL))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .defaultTimeZone(TimeZone.getTimeZone("UTC"));
    }
}
