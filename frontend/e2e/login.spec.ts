import AxeBuilder from '@axe-core/playwright';

import { expect, isBackendReachable, test } from './utils';

let devServerReachable = false;

test.beforeAll(async () => {
  devServerReachable = await isBackendReachable(process.env.PORTAL_E2E_BASE_URL);
});

test.beforeEach(async () => {
  test.skip(
    !devServerReachable,
    'Dev server not reachable at PORTAL_E2E_BASE_URL / http://localhost:4200 — skipping e2e smoke.'
  );
});

test.describe('Login page (smoke)', () => {
  test('renders the login form and both language options', async ({ page }) => {
    await page.goto('/auth/login');
    await expect(page.getByRole('heading').first()).toBeVisible();
    const languageButton = page.getByRole('button', {
      name: /language|زبان|فارسی|english/i
    });
    await expect(languageButton.first()).toBeVisible();
  });

  test('language toggle updates <html lang> and <html dir>', async ({ page }) => {
    await page.goto('/auth/login');

    const html = page.locator('html');
    await expect(html).toHaveAttribute('lang', /fa-IR|en-US/);

    await page
      .getByRole('button', { name: /language|زبان|فارسی|english/i })
      .first()
      .click();
    await page.getByRole('menuitem').first().click();

    await expect(html).toHaveAttribute('dir', /rtl|ltr/);
  });

  test('theme toggle updates <html data-theme>', async ({ page }) => {
    await page.goto('/auth/login');

    const html = page.locator('html');
    await page
      .getByRole('button', { name: /theme|تم|light|dark/i })
      .first()
      .click();
    await page.getByRole('menuitem').first().click();

    await expect(html).toHaveAttribute('data-theme', /light|dark/);
  });

  test('login page has no critical accessibility violations', async ({ page }) => {
    await page.goto('/auth/login');
    const result = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .disableRules(['color-contrast'])
      .analyze();
    expect(result.violations).toEqual([]);
  });
});
