import { expect, fetchCaptcha, fillLoginCaptcha, test } from './utils';
import AxeBuilder from '@axe-core/playwright';
import type { APIRequestContext, Page } from '@playwright/test';

/**
 * Live end-to-end suite against a fully running portal stack.
 * Requires PORTAL_E2E_BASE_URL (e.g. http://localhost:8080) and
 * PORTAL_E2E_ADMIN_EMAIL / PORTAL_E2E_ADMIN_PASSWORD.
 *
 * Skips are disabled when PORTAL_E2E_REQUIRE_LIVE=1 (CI fullstack job).
 */

const BASE = process.env.PORTAL_E2E_BASE_URL ?? 'http://localhost:8080';
const ADMIN_EMAIL = process.env.PORTAL_E2E_ADMIN_EMAIL ?? '';
const ADMIN_PASSWORD = process.env.PORTAL_E2E_ADMIN_PASSWORD ?? '';
const REQUIRE_LIVE = process.env.PORTAL_E2E_REQUIRE_LIVE === '1';

async function requireStack(): Promise<void> {
  try {
    const res = await fetch(`${BASE}/healthz`, { signal: AbortSignal.timeout(5000) });
    if (!res.ok && res.status >= 500) {
      throw new Error(`healthz ${res.status}`);
    }
  } catch (err) {
    if (REQUIRE_LIVE) {
      throw new Error(`Live stack required but unreachable at ${BASE}: ${err}`);
    }
    test.skip(true, `Stack not reachable at ${BASE}`);
  }
}

async function csrf(request: APIRequestContext): Promise<string> {
  const res = await request.get('/api/v1/auth/csrf');
  expect(res.ok()).toBeTruthy();
  const body = (await res.json()) as { token: string };
  expect(body.token).toBeTruthy();
  return body.token;
}

async function loginApi(
  request: APIRequestContext,
  email: string,
  password: string
): Promise<{ status: string; token: string }> {
  let lastBody: unknown;
  for (let attempt = 0; attempt < 6; attempt++) {
    const token = await csrf(request);
    const captcha = await fetchCaptcha(request);
    const res = await request.post('/api/v1/auth/login', {
      headers: { 'X-XSRF-TOKEN': token, 'Content-Type': 'application/json' },
      data: {
        username: email,
        password,
        captchaId: captcha.captchaId,
        captchaAnswer: captcha.captchaAnswer,
        rememberDevice: false
      }
    });
    lastBody = await res.json().catch(() => ({}));
    if (res.status() === 429) {
      const retryAfter =
        Number((lastBody as { retry_after_seconds?: number })?.retry_after_seconds ?? 5) || 5;
      await new Promise((r) => setTimeout(r, (retryAfter + 1) * 1000));
      continue;
    }
    expect(res.status(), JSON.stringify(lastBody)).toBe(200);
    const token2 = await csrf(request);
    return { status: (lastBody as { status: string }).status, token: token2 };
  }
  throw new Error(`login rate-limited after retries: ${JSON.stringify(lastBody)}`);
}

async function expectNoStorageAuth(page: Page): Promise<void> {
  const leaked = await page.evaluate(() => {
    const keys = [...Object.keys(localStorage), ...Object.keys(sessionStorage)];
    // Language/theme preference keys are expected; auth tokens are not.
    return keys.filter(
      (k) =>
        /token|jwt|session|auth|password|csrf/i.test(k) &&
        !/^portal\.(lang|theme)$/i.test(k)
    );
  });
  expect(leaked, `auth-like keys in browser storage: ${leaked.join(',')}`).toEqual([]);
}

async function openLanguageMenu(page: Page): Promise<void> {
  const trigger = page.locator('[data-testid="language-switcher"]');
  await expect(trigger).toBeVisible({ timeout: 15_000 });
  await trigger.click();
}

async function openThemeMenu(page: Page): Promise<void> {
  const trigger = page.locator('[data-testid="theme-switcher"]');
  await expect(trigger).toBeVisible({ timeout: 15_000 });
  await trigger.click();
}

async function switchToEnglish(page: Page): Promise<void> {
  await openLanguageMenu(page);
  await page.getByRole('menuitem', { name: /English/i }).click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'en-US');
}

async function fillLoginForm(page: Page, username: string, password: string): Promise<void> {
  // Material password control does not expose formcontrolname on the native input.
  await page.getByRole('textbox', { name: /username or email|نام کاربری|ایمیل/i }).fill(username);
  await page.getByLabel(/^password$|^رمز عبور$/i).fill(password);
  await fillLoginCaptcha(page);
}

async function submitLogin(page: Page): Promise<void> {
  const submit = page.getByRole('button', { name: /sign in|ورود/i });
  await expect(submit).toBeEnabled({ timeout: 10_000 });
  await submit.click();
}

async function dismissTransientOverlays(page: Page): Promise<void> {
  // Wait for Material snackbars to leave the DOM before Axe scans.
  const snack = page.locator('mat-snack-bar-container');
  if ((await snack.count()) > 0) {
    await snack.first().waitFor({ state: 'detached', timeout: 12_000 }).catch(() => undefined);
  }
}

async function axeSeriousCritical(page: Page, label: string): Promise<void> {
  await dismissTransientOverlays(page);
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag22aa'])
    .analyze();
  const serious = results.violations.filter((v) =>
    ['serious', 'critical'].includes(v.impact ?? '')
  );
  expect(serious, `${label}: ${JSON.stringify(serious, null, 2)}`).toEqual([]);
}

test.describe.configure({ mode: 'serial' });

test.beforeAll(async () => {
  await requireStack();
});

test.describe('Live auth UI — language and theme', () => {
  test('Persian is default with RTL on login', async ({ page }) => {
    const pageErrors: string[] = [];
    page.on('pageerror', (err) => pageErrors.push(String(err)));
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        pageErrors.push(msg.text());
      }
    });
    await page.addInitScript(() => {
      localStorage.removeItem('portal.lang');
      localStorage.removeItem('portal.theme');
    });
    await page.goto('/auth/login', { waitUntil: 'domcontentloaded' });
    // Confirm i18n JSON is not served as the SPA fallback HTML.
    const i18nProbe = await page.request.get('/assets/i18n/common/fa-IR.json');
    expect(i18nProbe.status(), 'i18n common/fa-IR.json').toBe(200);
    expect(i18nProbe.headers()['content-type'] ?? '').toMatch(/json/);
    const html = page.locator('html');
    await expect(html).toHaveAttribute('lang', 'fa-IR');
    await expect(html).toHaveAttribute('dir', 'rtl');
    // Wait for Angular to replace the static loading placeholder.
    await expect(page.locator('[data-testid="language-switcher"]')).toBeVisible({
      timeout: 45_000
    });
    await expect(page.locator('[data-testid="theme-switcher"]')).toBeVisible({ timeout: 15_000 });
    // Accessible names resolve once i18n bundles load.
    await expect(
      page.getByRole('button', { name: /change language|تغییر زبان|فارسی/i })
    ).toBeVisible({ timeout: 15_000 });
    await expect(
      page.getByRole('button', { name: /change theme|تغییر پوسته|روشن|تیره|سیستم|light|dark|system/i })
    ).toBeVisible({ timeout: 15_000 });
    // Anonymous bootstrap probes GET /api/v1/me; browsers log the expected 401.
    const unexpected = pageErrors.filter(
      (e) => !/status of 401|Failed to load resource:.*401/i.test(e)
    );
    expect(unexpected, `SPA boot errors: ${unexpected.join('\n')}`).toEqual([]);
  });

  test('switches to English LTR without full navigation reload barrier', async ({ page }) => {
    await page.goto('/auth/login');
    await openLanguageMenu(page);
    await page.getByRole('menuitem', { name: /English/i }).click();
    const html = page.locator('html');
    await expect(html).toHaveAttribute('lang', 'en-US');
    await expect(html).toHaveAttribute('dir', 'ltr');
    await expect(page.locator('form')).toBeVisible();
  });

  test('theme toggles light, dark, and system', async ({ page }) => {
    await page.goto('/auth/login');
    const html = page.locator('html');
    await openThemeMenu(page);
    await page.getByRole('menuitem', { name: /روشن|Light/i }).click();
    await expect(html).toHaveAttribute('data-theme', 'light');
    await openThemeMenu(page);
    await page.getByRole('menuitem', { name: /تیره|Dark/i }).click();
    await expect(html).toHaveAttribute('data-theme', 'dark');
    await openThemeMenu(page);
    await page.getByRole('menuitem', { name: /سیستم|System|Automatic/i }).click();
    await expect(html).toHaveAttribute('data-theme', /light|dark/);
  });
});

test.describe('Live Axe accessibility', () => {
  test('login Persian RTL has no serious/critical axe violations', async ({ page }) => {
    await page.goto('/auth/login');
    await expect(page.locator('[data-testid="language-switcher"]')).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
    await axeSeriousCritical(page, 'login-fa');
  });

  test('login English LTR has no serious/critical axe violations', async ({ page }) => {
    await page.goto('/auth/login');
    await expect(page.locator('[data-testid="language-switcher"]')).toBeVisible({ timeout: 30_000 });
    await openLanguageMenu(page);
    await page.getByRole('menuitem', { name: /English/i }).click();
    await expect(page.locator('html')).toHaveAttribute('dir', 'ltr');
    await axeSeriousCritical(page, 'login-en');
  });
});

test.describe('Live login / logout / storage', () => {
  test.skip(!ADMIN_EMAIL || !ADMIN_PASSWORD, 'Admin credentials not provided');

  test('failed login shows generic error', async ({ page }) => {
    await page.goto('/auth/login');
    await switchToEnglish(page);
    await fillLoginForm(page, 'nobody@example.com', 'WrongPassword!12345');
    await submitLogin(page);
    await expect(page.getByRole('alert')).toBeVisible({ timeout: 10_000 });
    const alertText = await page.getByRole('alert').innerText();
    expect(alertText.toLowerCase()).not.toMatch(/not found|does not exist|no such/);
  });

  test('successful login, navbar controls, no browser token storage, logout', async ({ page }) => {
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    await page.goto('/auth/login');
    await switchToEnglish(page);
    await fillLoginForm(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await submitLogin(page);
    await page.waitForURL(/\/dashboard/, { timeout: 20_000 });
    await expect(page.locator('[data-testid="language-switcher"]')).toBeVisible({ timeout: 20_000 });
    await expect(page.locator('[data-testid="theme-switcher"]')).toBeVisible();
    await expectNoStorageAuth(page);

    const cookies = await page.context().cookies();
    const session = cookies.find(
      (c) => /session/i.test(c.name) || c.name === 'PORTAL_SESSION' || c.name.startsWith('__Host-')
    );
    expect(session, 'session cookie missing').toBeTruthy();
    expect(session!.httpOnly).toBeTruthy();

    await dismissTransientOverlays(page);
    await axeSeriousCritical(page, 'dashboard-en');

    await openLanguageMenu(page);
    await page.getByRole('menuitem', { name: /فارسی/i }).click();
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
    await dismissTransientOverlays(page);
    await axeSeriousCritical(page, 'dashboard-fa');

    const unexpected = consoleErrors.filter(
      (e) =>
        !/favicon|Download the React DevTools|NG0|ExpressionChanged|status of 401|Failed to load resource:.*(401|409)/i.test(
          e
        )
    );
    expect(unexpected, `console errors: ${unexpected.join('\n')}`).toEqual([]);

    // Logout through the page request context so the browser session cookie is cleared.
    const token = await csrf(page.request);
    const logout = await page.request.post('/api/v1/auth/logout', {
      headers: { 'X-XSRF-TOKEN': token }
    });
    expect(logout.status()).toBeLessThan(400);
  });

  test('CSRF rejection on login without token', async ({ request }) => {
    const captcha = await fetchCaptcha(request);
    const res = await request.post('/api/v1/auth/login', {
      headers: { 'Content-Type': 'application/json' },
      data: {
        username: 'x@y.com',
        password: 'abcdefghijkl',
        captchaId: captcha.captchaId,
        captchaAnswer: captcha.captchaAnswer,
        rememberDevice: false
      }
    });
    expect(res.status()).toBe(403);
  });

  test('login page exposes captcha test ids', async ({ page }) => {
    await page.goto('/auth/login');
    await expect(page.getByTestId('captcha-image')).toBeVisible({ timeout: 30_000 });
    await expect(page.getByTestId('captcha-answer')).toBeVisible();
    await expect(page.getByTestId('captcha-refresh')).toBeVisible();
  });

  test('API login + me + sessions', async ({ request }) => {
    const { token } = await loginApi(request, ADMIN_EMAIL, ADMIN_PASSWORD);
    const me = await request.get('/api/v1/me', { headers: { 'X-XSRF-TOKEN': token } });
    expect(me.ok()).toBeTruthy();
    const sessions = await request.get('/api/v1/me/sessions', {
      headers: { 'X-XSRF-TOKEN': token }
    });
    expect(sessions.status()).toBeLessThan(500);
  });
});

test.describe('Live admin surfaces accessibility', () => {
  test.skip(!ADMIN_EMAIL || !ADMIN_PASSWORD, 'Admin credentials not provided');

  test('users, roles, permissions, profile security pages axe', async ({ page }) => {
    test.setTimeout(120_000);
    // Authenticate via API into this browser context to avoid extra UI logins
    // (and login rate-limit pressure) after the earlier serial suite.
    await page.goto('/auth/login');
    await expect(page.locator('[data-testid="language-switcher"]')).toBeVisible({ timeout: 30_000 });
    const token = await csrf(page.request);
    const captcha = await fetchCaptcha(page.request);
    let loginRes = await page.request.post('/api/v1/auth/login', {
      headers: { 'X-XSRF-TOKEN': token, 'Content-Type': 'application/json' },
      data: {
        username: ADMIN_EMAIL,
        password: ADMIN_PASSWORD,
        captchaId: captcha.captchaId,
        captchaAnswer: captcha.captchaAnswer,
        rememberDevice: false
      }
    });
    if (loginRes.status() === 429) {
      const body = await loginRes.json().catch(() => ({}));
      const retryAfter = Number(body?.retry_after_seconds ?? 30) || 30;
      await page.waitForTimeout((retryAfter + 2) * 1000);
      const token2 = await csrf(page.request);
      const captcha2 = await fetchCaptcha(page.request);
      loginRes = await page.request.post('/api/v1/auth/login', {
        headers: { 'X-XSRF-TOKEN': token2, 'Content-Type': 'application/json' },
        data: {
          username: ADMIN_EMAIL,
          password: ADMIN_PASSWORD,
          captchaId: captcha2.captchaId,
          captchaAnswer: captcha2.captchaAnswer,
          rememberDevice: false
        }
      });
    }
    expect(loginRes.status(), await loginRes.text()).toBe(200);

    await page.goto('/dashboard');
    await page.waitForURL(/\/dashboard/, { timeout: 20_000 });
    await dismissTransientOverlays(page);

    const paths = ['/users', '/roles', '/permissions', '/profile/security', '/dashboard'];
    for (const path of paths) {
      await page.goto(path);
      expect(page.url(), `redirected to login for ${path}`).not.toContain('/auth/login');
      await expect(page.locator('[data-testid="language-switcher"]')).toBeVisible({ timeout: 20_000 });
      await dismissTransientOverlays(page);
      await axeSeriousCritical(page, path);
    }
  });
});

test.describe('Mobile viewport', () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test('login usable on mobile', async ({ page }) => {
    await page.goto('/auth/login');
    await expect(page.locator('form')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });
});

test.describe('Keyboard navigation', () => {
  test('login form is keyboard reachable', async ({ page }) => {
    await page.goto('/auth/login');
    await openLanguageMenu(page);
    await page.getByRole('menuitem', { name: /English/i }).click();
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    const active = page.locator(':focus');
    await expect(active).toBeVisible();
  });
});
