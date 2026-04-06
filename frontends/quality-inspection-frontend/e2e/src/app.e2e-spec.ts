import { test, expect } from '@playwright/test';

/**
 * Minimal smoke test — verifies the app loads in standalone mode.
 * This is the e2e baseline entry point; feature-level e2e tests will be added later.
 */
test.describe('Application smoke test', () => {
  test('should load the application', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('app-root')).toBeVisible();
  });
});
