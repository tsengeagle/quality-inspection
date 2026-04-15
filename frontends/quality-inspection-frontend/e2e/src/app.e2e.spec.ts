import { test, expect } from '@playwright/test';

test.describe('frontend smoke baseline', () => {
  test('loads the shell from the root route', async ({ page }) => {
    await page.goto('/');

    await expect(page).toHaveTitle('品質審查系統');
    await expect(page.getByRole('heading', { name: '品質審查系統' })).toBeVisible();
  });

  test('renders the cases worklist shared regions', async ({ page }) => {
    await page.goto('/cases');

    await expect(page.locator('.case-summary-header')).toBeVisible();
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('審查佇列');
  });

  test('renders the sampling batch list placeholder region', async ({ page }) => {
    await page.goto('/sampling/batches');

    await expect(page.locator('.batch-list-tabs')).toBeVisible();
    await expect(page.getByRole('button', { name: '我的批次' })).toBeVisible();
  });
});
