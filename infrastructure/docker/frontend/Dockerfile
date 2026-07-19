# syntax=docker/dockerfile:1.9
#
# Multi-stage build for the Angular portal SPA.
#
# Stage 1 (`deps`)  — installs npm dependencies via `npm ci` in an isolated
#                     layer so re-runs only reinstall on lockfile changes.
# Stage 2 (`build`) — runs the production `ng build`; output lands in
#                     `dist/portal-frontend/browser`.
# Stage 3 (`web`)   — nginx (unprivileged/alpine) serves the built assets.
#                     Runs as UID 101, listens on 8080, root filesystem is
#                     read-only apart from the tmpfs-backed cache dirs.

ARG NODE_IMAGE=node:24.15.0-alpine
ARG NGINX_IMAGE=nginxinc/nginx-unprivileged:1.30.3-alpine

# -----------------------------------------------------------------------------
# Stage 1 — install dependencies
# -----------------------------------------------------------------------------
FROM ${NODE_IMAGE} AS deps

ENV CI=true \
    NPM_CONFIG_FUND=false \
    NPM_CONFIG_AUDIT=false \
    NPM_CONFIG_UPDATE_NOTIFIER=false

WORKDIR /workspace

COPY frontend/package.json frontend/package-lock.json ./

RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-progress

# -----------------------------------------------------------------------------
# Stage 2 — production build
# -----------------------------------------------------------------------------
FROM deps AS build

COPY frontend ./

RUN npm run build -- --configuration production && \
    test -d dist/portal-frontend/browser

# -----------------------------------------------------------------------------
# Stage 3 — static asset server
# -----------------------------------------------------------------------------
FROM ${NGINX_IMAGE} AS web

LABEL org.opencontainers.image.title="portal-frontend" \
      org.opencontainers.image.description="Enterprise Portal SPA served by nginx" \
      org.opencontainers.image.source="https://github.com/yasamanahmadi88/main-portal" \
      org.opencontainers.image.licenses="Apache-2.0"

USER root

# Refresh Alpine packages so CRITICAL openssl/libxml2 fixes from the base
# image channel are applied even if the tag lags slightly.
RUN apk upgrade --no-cache && \
    apk add --no-cache wget && \
    rm -f /etc/nginx/conf.d/default.conf

# Static assets: everything under public/browser is what nginx will serve.
COPY --chown=101:101 --from=build /workspace/dist/portal-frontend/browser /usr/share/nginx/html

# Config: base nginx.conf + our vhost + shared include for /api proxying.
COPY infrastructure/nginx/nginx.conf       /etc/nginx/nginx.conf
COPY infrastructure/nginx/default.conf     /etc/nginx/conf.d/default.conf
COPY infrastructure/nginx/_api_proxy.inc   /etc/nginx/conf.d/_api_proxy.inc

# The nginx-unprivileged base image already runs as UID 101 (`nginx`).
USER 101

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=10s \
    CMD wget -q -O - http://127.0.0.1:8080/healthz || exit 1

STOPSIGNAL SIGQUIT

CMD ["nginx", "-g", "daemon off;"]
