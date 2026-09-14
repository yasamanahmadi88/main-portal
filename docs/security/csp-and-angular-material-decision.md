# CSP Hardening Decision: Angular Material Style-src 'unsafe-inline'

**Date:** 2026-09-13  
**Status:** Documented  
**Compliance:** ASVS V5.1.2 (Content Security Policy)

## Issue

Angular Material uses dynamic inline styles (`<style>` tags) at runtime for:
- Component theme application
- Layout and responsive design
- Dynamic color palettes and material ripple effects

The current CSP policy includes `style-src 'unsafe-inline'` to support this, which weakens XSS protection by allowing any inline stylesheet to execute.

## Current Implementation

```
Content-Security-Policy: 
  default-src 'self'; 
  script-src 'self'; 
  style-src 'self' 'unsafe-inline';  /* Angular Material dependency */
  frame-ancestors 'none';
  ...
```

## Risk Assessment

### Style-src 'unsafe-inline' Risk Level: Medium

**Mitigating Factors:**
- `script-src 'self'` (no inline scripts allowed)
- `default-src 'self'` (restricts resources to same-origin)
- Angular Material team maintains security of generated styles
- XSS via CSS injection is lower severity than JS injection

**Attack Surface:**
- DOM-based XSS could inject malicious stylesheets if attacker controls element content
- CSS can be used for UI redressing (overlay attacks) but not data exfiltration without JS

## Hardening Options

### Option 1: Keep 'unsafe-inline' (Current)
**Pros:**
- Simple, no code changes
- Angular Material works out of box
- No maintenance burden

**Cons:**
- Weaker XSS mitigation (doesn't prevent style injection)
- Does not meet ASVS V5.1.2 best practices
- CI/CD tools flag as security debt

### Option 2: Migrate to Hashes
**Approach:** Generate hash of each static `<style>` tag in index.html
```
style-src 'self' 'sha256-abc123...';
```

**Pros:**
- No 'unsafe-inline'
- Static styles protected by CSP

**Cons:**
- Angular Material still generates runtime styles
- Would need nonce fallback for dynamic styles anyway
- High maintenance (hash updates on each build)

### Option 3: Use Nonces (Recommended Path)
**Approach:** Generate unique nonce on each request, inject into ALL style tags

```html
<!-- Server generates nonce: nonce="xyz789" -->
<style nonce="xyz789">/* Angular Material styles */</style>
```

CSP:
```
style-src 'self' 'nonce-xyz789';
```

**Pros:**
- No 'unsafe-inline'
- Works with dynamic styles
- Per-request nonce prevents reuse attacks
- Supports ASVS V5.1.2 best practices

**Cons:**
- Requires SSR or middleware to inject nonces
- Must be implemented for every HTTP response
- Nonce must be cryptographically random
- Needs Angular configuration changes

## Recommendation

### Immediate (Current)
**Accept** `style-src 'unsafe-inline'` with documented risk and mitigation plan.

**Document in Security Headers:**
```
X-CSP-Note: style-src 'unsafe-inline' required for Angular Material
             v17.x dynamic theming. Review for nonce migration in v18+.
```

**Rationale:**
- Angular Material team is working on nonce support
- Nonce implementation is significant undertaking
- Current code review practices mitigate style injection risks
- Can be improved in next major Angular/Material upgrade

### Future (v18+)
1. Evaluate Angular Material's nonce support improvements
2. Implement per-request nonce generation in frontend middleware
3. Inject nonce into CSP header and all `<style>` tags
4. Remove 'unsafe-inline' and move to 'nonce-*' directive
5. Verify in security testing suite

## Verification

### Current State (Playwright e2e/mfa-and-security.spec.ts)
```typescript
test('CSP header contains frame-ancestors none', async ({ page }) => {
  const cspHeader = response.headers()['content-security-policy'];
  expect(cspHeader).toContain("frame-ancestors 'none'");
  
  // Document Angular Material dependency
  if (cspHeader.includes("style-src 'unsafe-inline'")) {
    console.log('CSP includes style-src unsafe-inline (Angular Material)');
  }
});
```

### Test Execution
```bash
npm run e2e  # Playwright tests verify CSP headers
bash infrastructure/scripts/live-api-verify.sh  # Live CSP checks
```

## ASVS Compliance

| ASVS Requirement | Status | Notes |
|---|---|---|
| V5.1.2 CSP implemented | ✓ Compliant | frame-ancestors, default-src set |
| V5.1.2 No 'unsafe-eval' | ✓ Compliant | Not present in CSP |
| V5.1.2 No unsafe styles | ⚠ Accepted Risk | style-src 'unsafe-inline' for Angular Material |
| V5.1.3 X-Content-Type-Options | ✓ Compliant | nosniff header present |
| V5.1.4 HSTS (HTTPS) | ✓ Compliant | max-age set for production |
| V5.1.6 Referrer-Policy | ✓ Compliant | no-referrer-when-downgrade set |

## References

- [OWASP CSP Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Content_Security_Policy_Cheat_Sheet.html)
- [Angular Material Theme Guide](https://material.angular.io/guide/theming)
- [ASVS V5.1 - Input Validation and Output Encoding](https://github.com/OWASP/ASVS/blob/master/4.0/en/0x13-V5-Validation-Sanitization-Encoding.md)
- [CSP with Nonces](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/style-src#nonce)

## Decision Log

**Approved by:** Security Architecture Review  
**Decision:** Accept style-src 'unsafe-inline' as documented dependency with migration plan for nonce-based approach in v18 upgrade
