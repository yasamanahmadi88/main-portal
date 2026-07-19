package com.company.portal.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.portal.shared.config.PortalProperties;
import com.company.portal.shared.error.ErrorCodes;
import com.company.portal.shared.error.PortalException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;
    @Mock Environment environment;

    PortalProperties properties;
    CaptchaService service;

    @BeforeEach
    void setUp() {
        properties = new PortalProperties();
        properties.getCaptcha().setTtl(Duration.ofMinutes(2));
        properties.getCaptcha().setLength(5);
        properties.getCaptcha().setRevealAnswer(true);
        lenient().when(environment.matchesProfiles("prod")).thenReturn(false);
        lenient().when(redis.opsForValue()).thenReturn(values);
        service = new CaptchaService(redis, properties, environment);
    }

    @Test
    void issueStoresHashAndReturnsSvg() {
        CaptchaService.IssuedCaptcha issued = service.issue();
        assertThat(issued.captchaId()).isNotBlank();
        assertThat(issued.imageSvg()).contains("<svg");
        assertThat(issued.revealAnswer()).hasSize(5);
        assertThat(issued.ttlSeconds()).isEqualTo(120);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(values).set(key.capture(), hash.capture(), eq(Duration.ofMinutes(2)));
        assertThat(key.getValue()).startsWith("portal:captcha:");
        assertThat(hash.getValue()).hasSize(64);
    }

    @Test
    void validCaptchaIsConsumedOnce() {
        CaptchaService.IssuedCaptcha issued = service.issue();
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(values).set(anyString(), hash.capture(), any(Duration.class));

        when(values.get("portal:captcha:" + issued.captchaId())).thenReturn(hash.getValue());
        service.consume(issued.captchaId(), issued.revealAnswer());
        verify(redis).delete("portal:captcha:" + issued.captchaId());
    }

    @Test
    void invalidCaptchaRejected() {
        CaptchaService.IssuedCaptcha issued = service.issue();
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(values).set(anyString(), hash.capture(), any(Duration.class));
        when(values.get("portal:captcha:" + issued.captchaId())).thenReturn(hash.getValue());

        assertThatThrownBy(() -> service.consume(issued.captchaId(), "!!!!!") )
                .isInstanceOf(PortalException.BadRequest.class)
                .extracting(ex -> ((PortalException) ex).getCode())
                .isEqualTo(ErrorCodes.CAPTCHA_INVALID);
    }

    @Test
    void expiredOrMissingCaptchaRejected() {
        when(values.get(anyString())).thenReturn(null);
        assertThatThrownBy(() -> service.consume("missing-id", "ABCDE"))
                .isInstanceOf(PortalException.BadRequest.class)
                .extracting(ex -> ((PortalException) ex).getCode())
                .isEqualTo(ErrorCodes.CAPTCHA_EXPIRED);
        verify(redis, never()).delete(anyString());
    }

    @Test
    void missingCaptchaFieldsRejected() {
        assertThatThrownBy(() -> service.consume(null, "ABCDE"))
                .isInstanceOf(PortalException.BadRequest.class)
                .extracting(ex -> ((PortalException) ex).getCode())
                .isEqualTo(ErrorCodes.CAPTCHA_INVALID);
    }

    @Test
    void revealAnswerForbiddenInProdProfile() {
        when(environment.matchesProfiles("prod")).thenReturn(true);
        assertThatThrownBy(() -> new CaptchaService(redis, properties, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reveal-answer");
    }
}
