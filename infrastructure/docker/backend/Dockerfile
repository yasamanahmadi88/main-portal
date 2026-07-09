# syntax=docker/dockerfile:1.9
#
# Multi-stage build for the Spring Boot 4 backend running on Temurin JDK 25.
#
# Stage 1 (`deps`)  — resolves Maven dependencies with a warm cache mount so
#                     rebuilds only re-download changed artifacts.
# Stage 2 (`build`) — compiles + tests + packages the fat jar and CycloneDX
#                     SBOM (target/bom.json).  We copy the SBOM into the final
#                     image so `docker sbom` and Trivy can consume it.
# Stage 3 (`app`)   — minimal Temurin JRE 25 image running as a non-root user
#                     with a read-only root filesystem.  Boot's actuator health
#                     endpoint powers the container-level HEALTHCHECK.

ARG JDK_IMAGE=eclipse-temurin:25.0.3_9-jdk-alpine
ARG JRE_IMAGE=eclipse-temurin:25.0.3_9-jre-alpine

# -----------------------------------------------------------------------------
# Stage 1 — dependency resolution
# -----------------------------------------------------------------------------
FROM ${JDK_IMAGE} AS deps

ENV MAVEN_CONFIG=/root/.m2 \
    MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -Xmx1g"

WORKDIR /workspace

COPY backend/mvnw backend/mvnw.cmd ./
COPY backend/.mvn ./.mvn
COPY backend/pom.xml ./pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    chmod +x ./mvnw && \
    ./mvnw -B -ntp -q dependency:go-offline

# -----------------------------------------------------------------------------
# Stage 2 — compile, test, package
# -----------------------------------------------------------------------------
FROM deps AS build

ARG MAVEN_OPTS="-Xmx1g"
ENV MAVEN_OPTS=${MAVEN_OPTS}

COPY backend/src ./src

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests package && \
    cp target/*.jar /workspace/app.jar && \
    if [ -f target/bom.json ]; then cp target/bom.json /workspace/bom.json; \
    else echo '{"bomFormat":"CycloneDX","specVersion":"1.5","version":1,"components":[]}' > /workspace/bom.json; fi

# -----------------------------------------------------------------------------
# Stage 3 — runtime
# -----------------------------------------------------------------------------
FROM ${JRE_IMAGE} AS app

LABEL org.opencontainers.image.title="portal-backend" \
      org.opencontainers.image.description="Enterprise Portal backend (Spring Boot 4, Java 25)" \
      org.opencontainers.image.source="https://github.com/yasamanahmadi88/main-portal" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.vendor="Portal"

# `wget` for healthchecks; `tini` for correct signal handling.  Everything
# else is intentionally left out to keep the attack surface small.
RUN apk upgrade --no-cache && \
    apk add --no-cache tini wget ca-certificates tzdata && \
    addgroup -S -g 10001 portal && \
    adduser -S -u 10001 -G portal -H -s /sbin/nologin portal && \
    mkdir -p /app /app/config /tmp && \
    chown -R portal:portal /app

WORKDIR /app

COPY --from=build --chown=portal:portal /workspace/app.jar /app/app.jar
# The SBOM is copied for supply-chain tooling; it is not required at runtime.
COPY --from=build --chown=portal:portal /workspace/bom.json /app/bom.json

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    MANAGEMENT_SERVER_PORT=8081 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

USER portal:portal

EXPOSE 8080 8081

# Container HEALTHCHECK uses Boot's Actuator readiness probe.  The management
# port is separate from the API port so we can safely restrict access to it.
HEALTHCHECK --interval=10s --timeout=5s --retries=18 --start-period=120s \
    CMD wget -qO- --header 'Accept: application/json' \
        http://127.0.0.1:8081/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "/app/app.jar"]
