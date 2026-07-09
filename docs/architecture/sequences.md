# Architecture Sequences

## Login and session creation

```mermaid
sequenceDiagram
  autonumber
  participant B as Browser / Angular
  participant API as Spring Boot BFF
  participant ID as identity
  participant R as Redis Session
  participant A as audit
  participant SE as securityevent

  B->>API: POST /api/auth/login (email, password, CSRF)
  API->>ID: authenticate(email, password)
  ID->>ID: verify Argon2id password hash
  alt valid credentials
    API->>R: create new session and rotate identifier
    API->>A: append LOGIN_SUCCESS
    API-->>B: 204 + HttpOnly session cookie + CSRF cookie
  else invalid credentials
    API->>SE: record LOGIN_FAILURE
    API->>A: append LOGIN_FAILURE
    API-->>B: 401 problem details
  end
```

## CSRF-protected mutation

```mermaid
sequenceDiagram
  autonumber
  participant B as Browser / Angular
  participant API as Spring Security
  participant S as Application Service

  B->>API: GET /api/session
  API-->>B: session summary + XSRF-TOKEN cookie
  B->>API: POST /api/admin/users (Cookie + X-XSRF-TOKEN)
  API->>API: validate session cookie
  API->>API: compare CSRF token with session-bound expected value
  alt token valid
    API->>S: execute command
    S-->>API: result
    API-->>B: 201 created
  else missing or invalid
    API-->>B: 403 CSRF rejected
  end
```

## Password reset

```mermaid
sequenceDiagram
  autonumber
  participant B as Browser
  participant API as Backend
  participant ID as identity
  participant N as notification
  participant A as audit

  B->>API: POST /api/auth/password-reset/request
  API->>ID: create reset request if account exists
  ID->>ID: store reset token hash with expiry
  ID->>N: send localized reset email
  API-->>B: 202 accepted (same response for unknown email)
  B->>API: POST /api/auth/password-reset/confirm
  API->>ID: validate token hash and expiry
  ID->>ID: store new Argon2id password hash
  ID->>ID: revoke active sessions for account
  ID->>A: append PASSWORD_RESET_COMPLETED
  API-->>B: 204
```

## MFA enrollment and challenge

```mermaid
sequenceDiagram
  autonumber
  participant B as Browser
  participant API as Backend
  participant ID as identity
  participant K as Key material
  participant A as audit

  B->>API: POST /api/mfa/totp/enroll
  API->>ID: create TOTP secret
  ID->>K: load active MFA encryption key
  ID->>ID: encrypt secret and generate recovery codes
  API-->>B: QR provisioning URI + one-time recovery codes
  B->>API: POST /api/mfa/totp/verify (code)
  API->>ID: verify TOTP code
  ID->>A: append MFA_ENABLED
  API-->>B: 204
```

## RBAC authorization

```mermaid
sequenceDiagram
  autonumber
  participant B as Browser
  participant API as Controller
  participant SEC as Spring Security
  participant AC as accesscontrol
  participant S as Service
  participant A as audit

  B->>API: PATCH /api/admin/users/{id}/roles
  API->>SEC: require USER_ROLE_ASSIGN
  SEC->>AC: resolve current authorities from session/cache
  alt permission present
    API->>S: assign role
    S->>AC: validate no final SUPER_ADMIN violation
    S->>A: append ROLE_ASSIGNED
    API-->>B: 204
  else missing permission
    API-->>B: 403 problem details
  end
```

## Audit append and hash chain

```mermaid
sequenceDiagram
  autonumber
  participant S as Business Service
  participant A as audit
  participant DB as PostgreSQL

  S->>A: record(action, actor, target, metadata)
  A->>DB: read last current_hash
  A->>A: canonicalize event payload
  A->>A: compute current_hash(previous_hash + payload)
  A->>DB: INSERT audit_events
  alt insert succeeds
    A-->>S: audit id
  else insert fails
    A-->>S: fail closed for critical operation
  end
```

## Telemetry flow

```mermaid
sequenceDiagram
  autonumber
  participant B as Browser
  participant API as Backend
  participant M as Micrometer
  participant O as OpenTelemetry
  participant C as Collector

  B->>API: request with request id
  API->>M: increment HTTP/security/business metrics
  API->>O: create server span and module spans
  API-->>B: response with correlation id
  O->>C: export trace batch
  M->>C: expose/scrape metrics depending on deployment
```

## Logout

```mermaid
sequenceDiagram
  autonumber
  participant B as Browser
  participant API as Backend
  participant R as Redis Session
  participant A as audit

  B->>API: POST /api/auth/logout (CSRF)
  API->>R: invalidate session
  API->>A: append LOGOUT
  API-->>B: 204 + expired cookies
```
