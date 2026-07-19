# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project uses semantic versioning once releases begin.

## [Unreleased]

### Added

- Architecture documentation for modular monolith overview, module boundaries, data model, deployment, and sequence flows.
- Security documentation for threat model, security architecture, authentication, sessions, authorization, RBAC, cryptography, audit integrity, logging, ASVS evidence, secure coding, data classification, penetration testing, hardening, and accepted risks.
- RBAC role-permission catalog and authorization matrix.
- Observability documentation for logging, metrics, and tracing.
- Operations runbook with startup, bootstrap admin, and backup notes.
- Evidence-based JHipster gap analysis.
- Project README, security policy, and contribution guidelines.

### Changed

- Replaced placeholder README with project overview, startup instructions, architecture summary, and documentation links.

### Security

- Documented same-origin session BFF, CSRF, Argon2id, rate limiting, MFA secret protection, audit hash chain, and log redaction requirements.

### Verification

- Most runtime controls remain NOT VERIFIED until code, tests, and CI evidence are added.
