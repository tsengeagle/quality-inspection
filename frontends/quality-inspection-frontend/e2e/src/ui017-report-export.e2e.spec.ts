import { readFileSync } from 'fs';
import { expect, test } from '@playwright/test';

// UI-017 報表匯出頁：docs/05-ui/ui-page-spec-ui017報表匯出頁.md
// 以 Asia/Taipei 時區執行，驗證 §4 的日期轉換範例。
test.use({ timezoneId: 'Asia/Taipei' });

const EXPORT_URL = '**/api/quality-inspection/reports/export';

const SUMMARY = {
  totalCases: 3, closedCases: 2, rereviewedCases: 1, overdueCases: 0,
  completionRate: 2 / 3, rereviewRate: 1 / 3, overdueRate: 0
};

function exportRow(caseId: string, overrides: Record<string, unknown> = {}) {
  return {
    caseId, projectId: 'PRJ-001', hospitalId: 'HOSPITAL-001', status: 'CLOSED',
    reviewerId: 'reviewer-1', rereviewerId: null, initialScore: 85.5, finalScore: 90,
    createdAt: '2026-10-02T01:00:00Z', closedAt: '2026-10-05T09:30:00', maskedFields: [],
    ...overrides
  };
}

const ROWS = [
  exportRow('CASE-001'),
  exportRow('CASE-002', { initialScore: null, finalScore: null, maskedFields: ['initialScore', 'finalScore'] }),
  exportRow('=CMD()', { projectId: 'A,B', closedAt: null })
];

test.describe('UI-017 report export', () => {
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2026-11-17T09:00:00+08:00'));
  });

  test('queries the previous full month, shows masked scores and server-time closedAt', async ({ page }) => {
    const requests: Record<string, unknown>[] = [];
    await page.route(EXPORT_URL, async route => {
      const body = route.request().postDataJSON();
      requests.push(body);
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            reportType: 'REVIEW_COMPLETION', rows: ROWS, summary: SUMMARY,
            page: body.page, size: body.size, totalElements: ROWS.length, totalPages: 1
          }
        })
      });
    });

    await page.goto('/reports/export');
    await expect(page.getByTestId('export-start-date')).toHaveValue('2026-10-01');
    await expect(page.getByTestId('export-end-date')).toHaveValue('2026-10-31');
    await expect(page.getByTestId('export-initial')).toHaveText('請按查詢');
    await expect(page.getByTestId('export-hospital')).toHaveCount(0);

    await page.getByTestId('export-query').click();

    await expect(page.getByTestId('export-scope')).toContainText('本院');
    await expect(page.getByTestId('export-summary')).toContainText('66.7%');
    expect(requests[0]).toEqual({
      reportType: 'REVIEW_COMPLETION',
      from: '2026-09-30T16:00:00.000Z',
      to: '2026-10-31T16:00:00.000Z',
      page: 0,
      size: 100
    });

    const rows = page.getByTestId('export-row');
    await expect(rows).toHaveCount(3);
    await expect(rows.nth(0).locator('td').nth(8)).toHaveText('2026-10-02 09:00:00');
    await expect(rows.nth(0).locator('td').nth(9)).toHaveText('2026-10-05T09:30:00');
    await expect(rows.nth(1).locator('td').nth(6)).toHaveText('已遮罩');
    await expect(rows.nth(1).locator('td').nth(7)).toHaveText('已遮罩');
  });

  test('downloads every page as a CSV with BOM, escaping and formula guard', async ({ page }) => {
    const pages = [[ROWS[0], ROWS[1]], [ROWS[2]]];
    const downloadRequests: Record<string, unknown>[] = [];
    await page.route(EXPORT_URL, async route => {
      const body = route.request().postDataJSON();
      const size = body.size as number;
      if (size === 1000) {
        downloadRequests.push(body);
      }
      const index = body.page as number;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            reportType: 'REVIEW_COMPLETION',
            rows: size === 1000 ? pages[index] : ROWS,
            summary: SUMMARY, page: index, size,
            totalElements: ROWS.length,
            totalPages: size === 1000 ? pages.length : 1
          }
        })
      });
    });

    await page.goto('/reports/export');
    await page.getByTestId('export-query').click();
    await expect(page.getByTestId('export-row')).toHaveCount(3);

    const downloadPromise = page.waitForEvent('download');
    await page.getByTestId('export-download').click();
    const download = await downloadPromise;

    expect(download.suggestedFilename()).toBe('review-completion_2026-10-01_2026-10-31.csv');
    expect(downloadRequests.map(r => r['page'])).toEqual([0, 1]);

    const bytes = readFileSync(await download.path());
    expect([...bytes.subarray(0, 3)]).toEqual([0xef, 0xbb, 0xbf]);
    const text = bytes.subarray(3).toString('utf-8');
    expect(text.split('\r\n')).toEqual([
      '案件,專案,院區,狀態,初審醫師,複審醫師,初審分數,最終分數,建立時間,結案時間（伺服器時間）',
      'CASE-001,PRJ-001,HOSPITAL-001,CLOSED,reviewer-1,,85.5,90,2026-10-02T01:00:00Z,2026-10-05T09:30:00',
      'CASE-002,PRJ-001,HOSPITAL-001,CLOSED,reviewer-1,,已遮罩,已遮罩,2026-10-02T01:00:00Z,2026-10-05T09:30:00',
      "'=CMD(),\"A,B\",HOSPITAL-001,CLOSED,reviewer-1,,85.5,90,2026-10-02T01:00:00Z,",
      ''
    ]);
  });

  test('aborts the download without a file when totalElements changes between pages', async ({ page }) => {
    await page.route(EXPORT_URL, async route => {
      const body = route.request().postDataJSON();
      const index = body.page as number;
      const downloading = body.size === 1000;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            reportType: 'REVIEW_COMPLETION', rows: [ROWS[index] ?? ROWS[0]], summary: SUMMARY,
            page: index, size: body.size,
            totalElements: downloading && index === 1 ? 4 : 3,
            totalPages: downloading ? 3 : 1
          }
        })
      });
    });

    let downloaded = false;
    page.on('download', () => (downloaded = true));

    await page.goto('/reports/export');
    await page.getByTestId('export-query').click();
    await page.getByTestId('export-download').click();

    await expect(page.getByTestId('export-download-error')).toHaveText('資料在下載期間有變動，請重新下載');
    expect(downloaded).toBe(false);
  });
});
