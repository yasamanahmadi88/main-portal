# ADR-0002: Same-Origin Session BFF Authentication

## Status
Accepted

## Context
Browser SPAs often misuse JWTs in localStorage, increasing XSS impact.

## Decision
Opaque HttpOnly session cookies backed by Redis Spring Session, with CSRF double-submit cookie pattern compatible with Angular. No JWT in browser storage.

## Consequences
Requires same-origin (or careful proxy) deployment; CSRF mandatory; session fixation protection and rotation after login.
