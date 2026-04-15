import { expect, test } from '@playwright/test';

test.describe('Wave 5 skeleton route smoke', () => {
  test('loads /cases/:caseId/assign with Wave 5 assignment skeleton only', async ({ page }) => {
    await page.goto('/cases/CASE-011/assign');

    await expect(page.locator('.assignment-page-title')).toHaveText('案件分派頁');
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('ASSIGN_INITIAL');
    await expect(page.locator('[data-testid="reviewer-picker-slot"]')).toBeVisible();
    await expect(page.getByText('submit 成功')).toHaveCount(0);
    await expect(page.getByText('save 成功')).toHaveCount(0);
  });

  test('loads /cases/:caseId/initial-review with Wave 5 initial review skeleton only', async ({ page }) => {
    await page.goto('/cases/CASE-012/initial-review');

    await expect(page.locator('.initial-review-title')).toHaveText('初審作業頁');
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('NEW');
    await expect(page.locator('[data-testid="initial-review-form-placeholder"]')).toBeVisible();
    await expect(page.locator('[data-testid="initial-review-commentary-placeholder"]')).toBeVisible();
    await expect(page.getByText('command 完成')).toHaveCount(0);
  });

  test('loads /cases/:caseId/rereview with Wave 5 rereview skeleton only', async ({ page }) => {
    await page.goto('/cases/CASE-013/rereview');

    await expect(page.locator('.rereview-title')).toHaveText('複審作業頁');
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('NEW');
    await expect(page.locator('[data-testid="rereview-initial-summary-reference"]')).toBeVisible();
    await expect(page.locator('[data-testid="rereview-form-placeholder"]')).toBeVisible();
    await expect(page.getByText('submit 成功')).toHaveCount(0);
    await expect(page.getByText('save 成功')).toHaveCount(0);
  });
});
