import { defineConfig, devices } from '@playwright/test';

const PORT = Number(process.env.PORTAL_E2E_PORT ?? 4200);
const BASE_URL = process.env.PORTAL_E2E_BASE_URL ?? `http://localhost:${PORT}`;

/**
 * Playwright configuration for the enterprise portal frontend.
 *
 * The suite is deliberately conservative: individual specs probe the dev
 * server first and skip themselves if it isn't running so that
 * `npm run e2e` never fails on machines without a full backend.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 5_000 },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    locale: 'en-US'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
});
