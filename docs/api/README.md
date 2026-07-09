# API contract workflow

The Enterprise Portal API is contract-first. The OpenAPI source of truth lives in:

- `contracts/openapi/portal-v1.yaml`

## Change process

1. Update the OpenAPI contract before changing backend controllers or frontend clients.
2. Keep every public endpoint under `/api/v1`.
3. Document authentication and authorization on each operation:
   - safe authenticated requests use `cookieAuth`
   - unsafe authenticated requests use `cookieAuth` and `csrfHeader`
   - administrative operations include `x-permissions`
4. Reuse shared schemas and responses from `components` instead of creating ad hoc shapes.
5. Represent errors as RFC 9457 Problem Details (`application/problem+json`).
6. Document rate-limit headers when adding new response components.

## Validation

Parse the contract before committing:

```bash
python3 - <<'PY'
import yaml
with open("contracts/openapi/portal-v1.yaml", "r", encoding="utf-8") as fh:
    yaml.safe_load(fh)
PY
```

When implementation generators or validators are introduced, run them in CI after
the parse check so backend DTOs, frontend clients, and documentation stay aligned
with the contract.

## Compatibility expectations

- Additive fields and endpoints are preferred for minor revisions.
- Removing fields, changing enum values, or changing response status codes is a
  breaking change and requires a new versioned contract.
- Security changes must be reviewed against the session, CSRF, RBAC, and audit
  design documents before implementation.
