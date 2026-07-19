package com.company.portal.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

/**
 * Provides a portal-flavoured {@link RedisTemplate}. Uses the Jackson 3-based
 * {@link GenericJacksonJsonRedisSerializer} for values so entries stored by
 * the app are self-describing JSON documents.
 */
@Configuration(proxyBeanMethods = false)
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> portalRedisTemplate(RedisConnectionFactory connectionFactory,
                                                             ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJacksonJsonRedisSerializer valueSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();
        return template;
    }
}
