# Technology Baseline

Exact versions selected for this portal. Do not invent or pin milestone/snapshot/RC artifacts.

| Component | Version | Evidence |
|-----------|---------|----------|
| Java (Temurin) | 25.0.3+9 LTS | Adoptium GA |
| Spring Boot | 4.1.0 | Maven Central / start.spring.io |
| Spring Framework | managed by Boot 4.1.0 | Boot BOM |
| Spring Security | managed by Boot 4.1.0 (7.x line) | Boot BOM |
| Spring Session | 4.1.0 | Boot BOM `spring-session.version` |
| Spring Modulith | 2.1.0 | Maven Central BOM |
| PostgreSQL driver | managed by Boot | Boot BOM |
| Flyway | managed by Boot | Boot BOM |
| Redis / Lettuce | managed by Boot | Boot BOM |
| Micrometer Tracing | 1.7.0 | Boot BOM |
| OpenTelemetry | 1.62.0 | Boot BOM |
| springdoc-openapi | 3.0.3 | Maven Central (Boot 4 compatible) |
| ArchUnit | 1.4.2 | Maven Central |
| BouncyCastle (Argon2) | 1.84 | Maven Central |
| logstash-logback-encoder | 8.1 | Maven Central |
| JaCoCo | 0.8.13 | Maven plugin |
| CycloneDX Maven | 2.9.1 | Maven plugin |
| Maven | 3.9.10 (wrapper) | Apache |
| Node.js | 24.15.0 (engines: ^22.22.3 \|\| ^24.15.0 \|\| >=26) | Angular 22 engines |
| npm | 11.x | bundled with Node |
| Angular | 22.0.5 | npm `@angular/*` |
| Angular CLI / build | 22.0.5 | npm |
| Angular Material / CDK | 22.0.4 | npm |
| TypeScript | ~6.0.2 | Angular 22 scaffold |
| Vitest | ^4.0.8 | Angular 22 default test runner |
| @ngx-translate/core | 18.0.0 | npm (runtime i18n) |
| @ngx-translate/http-loader | 18.0.0 | npm |
| Playwright | 1.61.1 | npm |
| @axe-core/playwright | 4.12.1 | npm |
| OpenAPI | 3.1.0 | contracts |

## Rationale

- Spring Boot 4.1.0 is the current stable release with official Java 17–26 support (includes Java 25).
- Angular 22.0.5 is the current stable patch; Material 22.0.4 is the latest Material line matching Angular 22.
- Runtime i18n uses `@ngx-translate` so language can switch without reload (compile-time `$localize` alone is insufficient).
- Argon2id via Spring Security `Argon2PasswordEncoder` with BouncyCastle provider.

See ADR-0001 for the baseline decision record.
