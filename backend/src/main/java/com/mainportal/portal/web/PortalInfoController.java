package com.mainportal.portal.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class PortalInfoController {

  private final String applicationName;
  private final String apiVersion;

  public PortalInfoController(
      @Value("${portal.application-name:main-portal}") String applicationName,
      @Value("${portal.api-version:0.1.0}") String apiVersion) {
    this.applicationName = applicationName;
    this.apiVersion = apiVersion;
  }

  @GetMapping("/info")
  public Map<String, Object> info() {
    return Map.of(
        "name", applicationName,
        "version", apiVersion,
        "status", "UP",
        "phase", "portal-foundation",
        "timestamp", Instant.now().toString());
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "UP");
  }
}
