import { test as base } from '@playwright/test';

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

export const test = base;
export { expect } from '@playwright/test';
