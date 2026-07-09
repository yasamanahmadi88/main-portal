# ADR-0005: Runtime i18n and Theme Preferences

## Status
Accepted

## Context
Compile-time Angular i18n cannot switch language without reload.

## Decision
ngx-translate with feature JSON packs; default `fa-IR` + RTL; themes LIGHT/DARK/SYSTEM via CSS variables + Material; persist preferences server-side when authenticated.

## Consequences
Translation parity tests required; CDK directionality must track language.
