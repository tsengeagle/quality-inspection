import { expect, test } from '@playwright/test';

// UI-015 Dashboard 頁：docs/05-ui/ui-page-spec-ui015儀表板頁.md
// 瀏覽器以 UTC 時區執行：§4 的日期是台北日期（與 UI-017 §4 相同），結果不依瀏覽器的時區（D-077 第 4 點）。
test.use({ timezoneId: 'UTC' });

const SUMMARY_URL = '**/api/quality-inspection/reports/dashboard-summary**';

const SUMMARY = {
  totalCases: 250, closedCases: 200, rereviewedCases: 50, overdueCases: 25,
  completionRate: 0.8, rereviewRate: 0.2, overdueRate: 0.1
};

test.describe('UI-015 dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2026-11-17T09:00:00+08:00'));
  });

  test('queries the previous full month on entry, then all time after 不限期間', async ({ page }) => {
    const requests: URLSearchParams[] = [];
    await page.route(SUMMARY_URL, async route => {
      requests.push(new URL(route.request().url()).searchParams);
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: SUMMARY })
      });
    });

    await page.goto('/reports/dashboard');

    await expect(page.getByTestId('dashboard-card-totalCases')).toContainText('250');
    await expect(page.getByTestId('dashboard-card-completionRate')).toContainText('80.0%');
    await expect(page.getByTestId('dashboard-card-completionRate')).toContainText('200 / 250');
    await expect(page.getByTestId('dashboard-scope')).toContainText('本院');
    expect(requests).toHaveLength(1);
    expect(Object.fromEntries(requests[0])).toEqual({
      from: '2026-09-30T16:00:00.000Z',
      to: '2026-10-31T16:00:00.000Z'
    });

    await page.getByTestId('dashboard-clear-range').click();
    await page.getByTestId('dashboard-project-input').fill('PRJ-001');
    await page.getByTestId('dashboard-query').click();

    await expect(page.getByTestId('dashboard-scope')).toContainText('不限期間');
    await expect.poll(() => requests.length).toBe(2);
    expect(Object.fromEntries(requests[1])).toEqual({ projectId: 'PRJ-001' });
  });

  test('links to the case list and the export page', async ({ page }) => {
    await page.route(SUMMARY_URL, route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: SUMMARY })
    }));

    await page.goto('/reports/dashboard');
    await page.getByTestId('dashboard-link-export').click();
    await expect(page).toHaveURL(/\/reports\/export$/);
  });
});
