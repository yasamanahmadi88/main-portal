# Portal Frontend

Enterprise portal SPA built with **Angular 22**, **Angular Material 22**, and
**@ngx-translate v18**. It targets the Spring Boot backend that ships alongside
this repo under `/backend` and consumes the OpenAPI contract at
`/contracts/openapi/portal-v1.yaml`.

## Stack

| Concern          | Choice |
|------------------|--------|
| Framework        | Angular 22 (standalone APIs, signals) |
| UI               | Angular Material 22 with custom CSS-variable theming |
| Language         | TypeScript 5.x, strict mode |
| Styling          | SCSS design tokens, logical CSS properties, RTL-first |
| Fonts            | Self-hosted **Vazirmatn** with a curated system-font fallback stack |
| i18n             | `@ngx-translate/core` + `@ngx-translate/http-loader`, multi-namespace loader |
| Bidi             | Angular CDK `Directionality` bound to `<html dir>` |
| Auth             | Cookie session, `X-XSRF-TOKEN` CSRF (double-submit) |
| State            | Angular signals + effects, no Redux |
| Tests            | Vitest for unit, Playwright + `@axe-core/playwright` for e2e |
| Lint             | ESLint 9 flat config + `angular-eslint` + `typescript-eslint` |

## Directory layout

```
src/
  app/
    core/
      authentication/    # AuthService, CsrfService, guards
      authorization/     # HasPermission directive
      configuration/     # ApiConfig injection token
      error-handling/    # Error interceptor + global handler + Problem Details
      http/              # Interceptors (csrf, accept-language, api-base-url)
                         # + typed API clients under core/http/api/*.api.ts
      i18n/              # I18nService, ThemeService, multi-namespace loader
      observability/     # LoggerService, ToastService
      routing/           # (reserved)
      session/           # bootstrap.provider.ts, PreferencesSyncService
      shell/             # AppShell + AuthLayout + navbar/user-menu/switchers
    shared/
      models/            # DTO interfaces derived from portal-v1.yaml
      ui/                # button, input, password-input, table, pagination,
                         # dialog/confirm, snackbar wrapper, skeleton,
                         # empty-state, error-state, page-header,
                         # breadcrumb, status-badge, permission-matrix
    features/
      authentication/    # login, forgot-password, reset-password, mfa,
                         # session-expired, access-denied, maintenance, 404
      dashboard/
      profile/           # overview / security / sessions
      users/             # list, detail, form
      roles/             # list, detail, form
      permissions/       # list + matrix
      audit/             # list, detail
      security-events/
      settings/
    app.config.ts
    app.routes.ts
  assets/
    i18n/<namespace>/<lang>.json
  environments/
  styles/
    _tokens.scss   # design tokens (spacing, radii, motion, colors)
    _fonts.scss    # @font-face declarations (Vazirmatn)
    _utilities.scss
public/
  fonts/vazirmatn/*.woff2
```

## Local development

```bash
# Node 24.15.0, npm 11.12.1
source /home/ubuntu/.portal-env   # sets PATH if needed
npm install
npm start                         # ng serve with proxy to http://localhost:8080
```

The dev server proxies `/api/*` to the backend so cookie-based sessions and
CSRF work identically to production. See `proxy.conf.json`.

## Scripts

| Command              | Purpose |
|----------------------|---------|
| `npm start`          | Dev server on http://localhost:4200 with API proxy |
| `npm run build`      | Production build to `dist/portal-frontend` |
| `npm run build:dev`  | Development build (source maps, no optimization) |
| `npm test`           | Vitest unit suite (headless) |
| `npm run lint`       | ESLint on `.ts` and `.html` |
| `npm run e2e`        | Playwright smoke suite (skips gracefully when backend is offline) |

## Testing

Unit tests live next to the code (`*.spec.ts`). The suite includes:

- `core/authentication/csrf.service.spec.ts` – token bootstrap and header
  attachment via `csrfInterceptor`
- `core/authentication/auth.service.spec.ts` – bootstrap, login, MFA
  challenge, logout, permission helpers
- `core/authentication/guards.spec.ts` – auth / guest / permission /
  mfa-challenge guards
- `core/i18n/i18n.service.spec.ts` – language switching and `<html>`
  `lang` / `dir` attribute wiring
- `core/i18n/theme.service.spec.ts` – LIGHT / DARK / SYSTEM behaviour
- `assets/i18n/i18n-parity.spec.ts` – parity check between `fa-IR` and
  `en-US` bundles across every namespace

E2E smoke tests (see `e2e/`) exercise:

- Login page loads and renders both languages
- Runtime language switch changes `<html lang>` and `<html dir>`
- Runtime theme switch changes `<html data-theme>`
- `@axe-core/playwright` accessibility check on the login page

The e2e tests self-detect the dev server and skip when it is not running so
that `npm run e2e` never fails in CI environments without a backend.

## Security notes

- No tokens are persisted to storage. The browser session lives in a Spring
  Session cookie (`SESSION`, `HttpOnly`, `Secure`, `SameSite=Lax`).
- CSRF: the app implements the double-submit cookie pattern. `CsrfService`
  reads the `XSRF-TOKEN` cookie and echoes it in `X-XSRF-TOKEN` for every
  unsafe request. It refreshes automatically on `403` due to a stale token.
- Login errors and MFA prompts do not leak whether a username exists.
- The AuthService exposes signals only. There is no synchronous
  "is authenticated" cache derived from storage; the source of truth is the
  cookie session validated by `GET /api/v1/me` on startup.

## i18n

Two languages ship out of the box: **Persian (`fa-IR`, default, RTL)** and
**English (`en-US`, LTR)**. Translation JSON is loaded lazily by namespace
via `@ngx-translate/http-loader`'s multi-file loader, from
`assets/i18n/<namespace>/<lang>.json`.

Namespaces: `common`, `validation`, `navigation`, `authentication`,
`dashboard`, `profile`, `users`, `roles`, `permissions`, `audit`,
`security-events`, `settings`.

## Theming and fonts

Material is themed via CSS variables so that switching between LIGHT / DARK
without a page flash is trivial. The initial theme is applied by a tiny inline
script in `index.html` that reads `localStorage.portal.theme` before the
Angular bundle boots.

Typography uses self-hosted **Vazirmatn** (OFL, `public/fonts/vazirmatn/*.woff2`)
with the following fallback stack for both Persian and Latin scripts:

```
"Vazirmatn", "Segoe UI", "Tahoma", "Noto Sans Arabic", system-ui, sans-serif
```

The stack is intentionally distinct from Inter/Roboto/Arial-only defaults and
never loads a remote CDN at runtime.

## Accessibility

- All interactive icons are labelled via `aria-label` from the `common`
  namespace.
- Focus outlines use `outline-color: var(--md-sys-color-primary)` and are
  never removed.
- Reduced-motion users get static skeletons and no shimmer animation.
- Login page passes an `axe` sweep (`e2e/login.spec.ts`).
