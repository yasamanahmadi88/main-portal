import { expect, test as base, type APIRequestContext, type Page } from '@playwright/test';

const DEFAULT_BASE_URL = 'http://localhost:4200';

/**
 * Simple TCP/HTTP probe run from Node (no browser needed) so we can skip
 * entire specs when the dev server is not up. Playwright's `page` and
 * `request` fixtures both require a running browser context which we want
 * to avoid instantiating just to discover the server isn't there.
 */
export async function isBackendReachable(baseURL: string | undefined): Promise<boolean> {
  const url = baseURL ?? process.env.PORTAL_E2E_BASE_URL ?? DEFAULT_BASE_URL;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 2_000);
  try {
    const res = await fetch(url, { signal: controller.signal });
    return res.ok || res.status < 500;
  } catch {
    return false;
  } finally {
    clearTimeout(timeout);
  }
}

export async function fetchCaptcha(
  request: APIRequestContext
): Promise<{ captchaId: string; captchaAnswer: string }> {
  const res = await request.get('/api/v1/auth/captcha');
  if (!res.ok()) {
    throw new Error(`captcha fetch failed: ${res.status()} ${await res.text()}`);
  }
  const body = (await res.json()) as {
    captchaId: string;
    revealAnswer?: string;
  };
  if (!body.captchaId) {
    throw new Error('captcha response missing captchaId');
  }
  if (!body.revealAnswer) {
    throw new Error(
      'captcha revealAnswer missing — set CAPTCHA_REVEAL_ANSWER=true for e2e/CI'
    );
  }
  return { captchaId: body.captchaId, captchaAnswer: body.revealAnswer };
}

export async function fillLoginCaptcha(page: Page): Promise<void> {
  await expect(page.getByTestId('captcha-image')).toBeVisible({ timeout: 20_000 });
  const reveal = page.getByTestId('captcha-reveal');
  await expect(reveal).toBeAttached({ timeout: 15_000 });
  const answer = await reveal.getAttribute('data-answer');
  if (!answer) {
    throw new Error('captcha-reveal missing data-answer (CAPTCHA_REVEAL_ANSWER required)');
  }
  await page.getByTestId('captcha-answer').fill(answer);
}

export const test = base;
export { expect };
