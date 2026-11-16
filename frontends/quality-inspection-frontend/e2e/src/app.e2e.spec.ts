import { test, expect } from '@playwright/test';

async function mockWave3CasesApi(page: Parameters<typeof test>[0]['page']): Promise<void> {
  await page.route('**/api/quality-inspection/review-cases?*', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          content: [
            {
              caseId: 'CASE-009',
              projectId: 'PROJECT-001',
              batchId: 'BATCH-001',
              hospitalId: 'H001',
              status: 'PENDING_INITIAL_REVIEW',
              reviewerId: 'reviewer-1',
              rereviewerId: 'rereviewer-1',
              medicalRecordType: 'OUTPATIENT',
              sourceCaseKey: 'SK-009',
              createdAt: '2026-04-01T08:00:00',
              assignedAt: '2026-04-01T09:00:00',
              dueAt: '2026-04-03T23:59:00',
              projectionType: 'FULL'
            }
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1
        }
      })
    });
  });

  await page.route('**/api/quality-inspection/review-cases/CASE-010', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          caseId: 'CASE-010',
          projectId: 'PROJECT-001',
          batchId: 'BATCH-001',
          hospitalId: 'H001',
          status: 'PENDING_REREVIEW',
          reviewerId: 'reviewer-1',
          rereviewerId: 'rereviewer-1',
          medicalRecordType: 'OUTPATIENT',
          sourceCaseKey: 'SK-010',
          submissions: [
            {
              submissionId: 'SUB-010',
              stage: 'INITIAL',
              reviewerId: 'reviewer-1',
              status: 'SUBMITTED',
              revisionNo: 1,
              submittedAt: '2026-04-01T08:00:00'
            },
            {
              submissionId: 'SUB-011',
              stage: 'REREVIEW',
              reviewerId: 'rereviewer-1',
              status: 'SUBMITTED',
              revisionNo: 2,
              submittedAt: '2026-04-02T08:00:00'
            }
          ],
          latestRereviewSubmissionId: 'SUB-011',
          projectionType: 'FULL',
          actorRole: 'HOSPITAL_MANAGER',
          isOwner: false
        }
      })
    });
  });

  await page.route('**/api/quality-inspection/review-cases/CASE-014', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          caseId: 'CASE-014',
          projectId: 'PROJECT-001',
          batchId: 'BATCH-001',
          hospitalId: 'H001',
          status: 'CLOSED',
          reviewerId: 'reviewer-1',
          rereviewerId: 'rereviewer-1',
          medicalRecordType: 'OUTPATIENT',
          sourceCaseKey: 'SK-014',
          submissions: [],
          latestRereviewSubmissionId: 'SUB-014-2',
          projectionType: 'FULL',
          actorRole: 'REREVIEWER',
          isOwner: true
        }
      })
    });
  });

  await page.route('**/api/quality-inspection/review-cases/CASE-014/revision-history', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          caseId: 'CASE-014',
          hasExceptionEntry: true,
          submissions: [
            {
              submissionId: 'SUB-014-1',
              stage: 'INITIAL',
              reviewerId: 'reviewer-1',
              status: 'SUPERSEDED',
              revisionNo: 1,
              submittedAt: '2026-04-01T08:00:00',
              supersededBySubmissionId: 'SUB-014-2'
            },
            {
              submissionId: 'SUB-014-2',
              stage: 'REREVIEW',
              reviewerId: 'rereviewer-1',
              status: 'SUBMITTED',
              revisionNo: 2,
              submittedAt: '2026-04-02T08:00:00',
              supersededBySubmissionId: null
            }
          ]
        }
      })
    });
  });
}

async function mockSamplingApi(page: Parameters<typeof test>[0]['page']): Promise<void> {
  await page.route('**/api/quality-inspection/sampling-batches?*', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          content: [
            {
              batchId: 'BATCH-FAILED-001',
              projectId: 'PROJECT-001',
              executionMode: 'MANUAL',
              status: 'FAILED',
              populationCount: 120,
              eligibleCount: 80,
              selectedCount: 0,
              retryOfBatchId: null,
              startedAt: '2026-04-01T08:00:00',
              finishedAt: '2026-04-01T08:10:00'
            }
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1
        }
      })
    });
  });

  await page.route('**/api/quality-inspection/review-projects/PROJECT-001/sampling/execute-context', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          projectId: 'PROJECT-001',
          projectName: '2026 年第二季品質審查',
          hospitalId: 'H001',
          ruleSummary: '專案版本：v1\n病歷類型：OUTPATIENT\n評分表：ss-test\n品質門檻：未設定',
          previewReady: false,
          executing: false,
          executeFailed: false,
          executedBatchId: null,
          blockedReason: null,
          previewStats: null,
          postExecuteSummary: null
        }
      })
    });
  });
}

test.describe('frontend smoke baseline', () => {
  test('loads the shell from the root route', async ({ page }) => {
    await page.goto('/');

    await expect(page).toHaveTitle('品質審查系統');
    await expect(page.getByRole('heading', { name: '品質審查系統' })).toBeVisible();
  });

  test('renders the cases worklist shared regions', async ({ page }) => {
    await mockWave3CasesApi(page);
    await page.goto('/cases');

    await expect(page.locator('.case-summary-header')).toBeVisible();
    // standalone 模式的 actor 固定為 HOSPITAL_MANAGER（standalone-mock-actor-context.source.ts），
    // UI-009 §4.1 依目前角色顯示視角。
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('管理者視角');
  });

  test('renders the sampling batch list placeholder region', async ({ page }) => {
    await mockSamplingApi(page);
    await page.goto('/sampling/batches');

    await expect(page.locator('.batch-list-tabs')).toBeVisible();
    await expect(page.getByRole('button', { name: '全部批次' })).toBeVisible();
    await expect(page.locator('.batch-filter-bar')).toBeVisible();
  });

  test('marks batch detail as deferred on /sampling/batches', async ({ page }) => {
    await mockSamplingApi(page);
    await page.goto('/sampling/batches');

    await expect(page.getByRole('button', { name: '查看詳情（Deferred）' }).first()).toBeDisabled();
    await expect(page.locator('.deferred-note')).toContainText('批次詳情仍屬 deferred；展示流程請以「查看案件」延續至案件列表。');
  });

  test('loads the sampling execute route without being shadowed by /sampling/batches', async ({ page }) => {
    await mockSamplingApi(page);
    await page.goto('/sampling/PROJECT-001');

    await expect(page.locator('.sampling-page-title')).toHaveText('抽選前確認與正式執行');
    await expect(page.getByRole('button', { name: '預估母體' })).toBeVisible();
  });

  test('does not let /sampling/batches get swallowed by the :projectId route', async ({ page }) => {
    await page.goto('/sampling/batches');

    await expect(page.locator('.sampling-page-title')).toHaveCount(0);
    await expect(page.locator('.batch-list-title')).toHaveText('抽選批次追蹤');
  });

  test('loads the Wave 3 case detail control center route', async ({ page }) => {
    await mockWave3CasesApi(page);
    await page.goto('/cases/CASE-010');

    await expect(page.locator('.case-detail-content')).toBeVisible();
    await expect(page.locator('.control-center-nav')).toBeVisible();
  });

  test('loads the Wave 3 revision history route', async ({ page }) => {
    await mockWave3CasesApi(page);
    await page.goto('/cases/CASE-014/revisions');

    await expect(page.locator('.stage-tabs')).toBeVisible();
    await expect(page.locator('.compare-summary')).toBeVisible();
    await expect(page.locator('.compare-summary')).toContainText('版本選擇互動尚未定案；目前採固定差異摘要檢視。');
    await expect(page.locator('button[type="submit"]')).toHaveCount(0);
  });
});
