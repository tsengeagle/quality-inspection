import { expect, test } from '@playwright/test';

test.describe.serial('live standalone demo path', () => {
  test('loads UI-006 from the live standalone backend chain', async ({ page }) => {
    await page.goto('/sampling/PROJECT-001');

    await expect(page.getByRole('heading', { name: '抽選前確認與正式執行' })).toBeVisible();
    await expect(page.getByText('2026 Q2 門診品質抽選專案')).toBeVisible();
    await expect(page.getByText('正式批次：BATCH-001')).toBeVisible();
  });

  test('loads UI-007 with live batch data', async ({ page }) => {
    await page.goto('/sampling/batches');

    await expect(page.getByRole('heading', { name: '抽選批次追蹤' })).toBeVisible();
    await expect(page.getByRole('cell', { name: /BATCH-001/ })).toBeVisible();
    await expect(page.getByRole('button', { name: '查看案件' })).toBeVisible();
  });

  test('loads UI-011 with live assignment context and candidates', async ({ page }) => {
    await page.goto('/cases/CASE-011/assign');

    await expect(page.getByRole('heading', { name: '初審指派' })).toBeVisible();
    await expect(page.getByText('待初審指派')).toBeVisible();
    await expect(page.getByText('reviewer-1 | Dr. Reviewer One')).toBeVisible();
    await expect(page.getByRole('button', { name: '提交初審指派' })).toBeVisible();
  });
});
