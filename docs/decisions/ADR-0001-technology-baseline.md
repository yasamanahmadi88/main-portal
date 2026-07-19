# ADR-0001: Technology Baseline

## Status
Accepted

## Context
Need a stable, production-capable stack for a secure enterprise portal with Java 25 and Angular 22 requirements.

## Decision
- Java 25 (Temurin) + Spring Boot 4.1.0 modular monolith
- Angular 22.0.5 + Material 22.0.4 + ngx-translate 18
- PostgreSQL + Flyway + Redis Spring Session
- OpenTelemetry + Micrometer Prometheus

## Consequences
Aligns with official Boot 4.1 Java 17–26 support and Angular 22 engines. Requires Node >=22.22.3 (we use 24.15.0).
