import { test, expect, Page } from '@playwright/test';

async function mockWave3CasesApi(page: Page): Promise<void> {
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
              createdAt: '2026-04-01T00:00:00Z',
              assignedAt: '2026-04-01T01:00:00Z',
              dueAt: '2026-04-03T15:59:00Z',
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
              submittedAt: '2026-04-01T00:00:00Z'
            },
            {
              submissionId: 'SUB-011',
              stage: 'REREVIEW',
              reviewerId: 'rereviewer-1',
              status: 'SUBMITTED',
              revisionNo: 2,
              submittedAt: '2026-04-02T00:00:00Z'
            }
          ],
          latestRereviewSubmissionId: 'SUB-011',
          projectionType: 'FULL',
          actorRole: 'HOSPITAL_MANAGER',
          owner: false
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
          owner: true
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
          // 舊版後端曾帶此欄位；D-032 撤銷 CLOSED 複審 revision 例外，畫面不得再顯示入口
          hasExceptionEntry: true,
          submissions: [
            {
              submissionId: 'SUB-014-1',
              stage: 'INITIAL',
              reviewerId: 'reviewer-1',
              status: 'SUPERSEDED',
              revisionNo: 1,
              submittedAt: '2026-04-01T00:00:00Z',
              supersededBySubmissionId: 'SUB-014-2'
            },
            {
              submissionId: 'SUB-014-2',
              stage: 'REREVIEW',
              reviewerId: 'rereviewer-1',
              status: 'SUBMITTED',
              revisionNo: 2,
              submittedAt: '2026-04-02T00:00:00Z',
              supersededBySubmissionId: null
            }
          ]
        }
      })
    });
  });
}

async function mockSamplingApi(page: Page): Promise<void> {
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
              startedAt: '2026-04-01T00:00:00Z',
              finishedAt: '2026-04-01T00:10:00Z'
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
    // CLOSED 案件、原複審醫師：只顯示終止狀態的唯讀提示，沒有例外修正入口（D-032）
    await expect(page.getByText('此案件處於終止狀態，僅可檢視歷程。')).toBeVisible();
    await expect(page.locator('app-revision-nav-action-zone')).toHaveCount(0);
    await expect(page.getByText('前往複審修正')).toHaveCount(0);
  });

  // D-036、D-094 第 9 點：SYSTEM_ADMIN 的案件詳情只有七欄（UI-010 §5.5）。假回應照 specs/review-case/http.yaml 的 metadata 形狀。
  test('shows the seven-field metadata view and reaches the metadata revision history', async ({ page }) => {
    await page.route('**/api/quality-inspection/review-cases/CASE-020', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            caseId: 'CASE-020',
            caseStatus: 'REREVIEW_IN_PROGRESS',
            projectName: '品質審查專案',
            hospitalId: 'H001',
            reviewerId: 'reviewer-1',
            rereviewerId: null,
            updatedAt: '2026-11-19T09:00:00Z'
          }
        })
      });
    });
    await page.route('**/api/quality-inspection/review-cases/CASE-020/revision-history', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            caseId: 'CASE-020',
            submissions: [
              {
                submissionId: 'SUB-020-1',
                stage: 'INITIAL',
                reviewerId: 'reviewer-1',
                status: 'SUBMITTED',
                revisionNo: 1,
                totalScore: null,
                comment: null,
                submittedAt: '2026-11-19T08:00:00Z',
                supersededBySubmissionId: null
              }
            ]
          }
        })
      });
    });

    await page.goto('/cases/CASE-020');

    await expect(page.getByText('控制中心（Metadata）')).toBeVisible();
    await expect(page.getByTestId('case-metadata-block')).toBeVisible();
    await expect(page.getByTestId('case-metadata-project-name')).toHaveText('品質審查專案');
    await expect(page.getByTestId('case-metadata-rereviewer-id')).toHaveText('—');
    await expect(page.getByTestId('case-metadata-updated-at')).toHaveText('2026-11-19 17:00');
    await expect(page.locator('app-initial-summary-region')).toHaveCount(0);
    await expect(page.locator('app-rereview-summary-region')).toHaveCount(0);
    await expect(page.locator('.control-center-nav button')).toHaveCount(1);

    await page.locator('.control-center-nav button', { hasText: 'Revision 歷程' }).click();

    await expect(page).toHaveURL(/\/cases\/CASE-020\/revisions$/);
    await expect(page.getByText('Metadata 歷程')).toBeVisible();
  });

  // 假的列表端點照後端的行為：status 不是 CaseStatus 的值時回 400 BAD_REQUEST（RC-LST-02）。
  // 原本的假回應不看查詢參數，所以篩選列送出後端不認得的值、batchId 沒有被送出，都不會被發現。
  test('case list filters only send what the backend accepts, and carry the batchId from the URL', async ({ page }) => {
    const caseStatuses = ['SELECTED_PENDING_ASSIGNMENT', 'PENDING_INITIAL_REVIEW', 'INITIAL_REVIEW_IN_PROGRESS',
      'INITIAL_REVIEW_COMPLETED', 'PENDING_REREVIEW', 'REREVIEW_IN_PROGRESS', 'RETURNED', 'CLOSED', 'CANCELLED'];
    const requests: URL[] = [];
    const rejected: string[] = [];

    await page.route('**/api/quality-inspection/review-cases?*', async route => {
      const url = new URL(route.request().url());
      requests.push(url);
      const status = url.searchParams.get('status');
      if (status !== null && !caseStatuses.includes(status)) {
        rejected.push(status);
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, data: null, meta: null, error: { code: 'BAD_REQUEST', message: 'status' } })
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } })
      });
    });

    await page.goto('/cases?batchId=BATCH-007');

    await expect(page.getByTestId('case-list-batch-filter')).toHaveText('批次：BATCH-007');
    await expect.poll(() => requests.length).toBeGreaterThan(0);
    expect(requests[requests.length - 1].searchParams.get('batchId')).toBe('BATCH-007');

    const chips = page.locator('.filter-chip');
    const count = await chips.count();
    expect(count).toBe(7);
    for (let i = 0; i < count; i++) {
      const before = requests.length;
      await chips.nth(i).click();
      await expect.poll(() => requests.length).toBeGreaterThan(before);
      // 換篩選條件之後，批次的篩選仍然帶著
      expect(requests[requests.length - 1].searchParams.get('batchId')).toBe('BATCH-007');
    }
    expect(rejected).toEqual([]);

    await page.getByTestId('case-list-batch-filter-clear').click();
    await expect(page.getByTestId('case-list-batch-filter')).toHaveCount(0);
    await expect.poll(() => requests[requests.length - 1].searchParams.has('batchId')).toBe(false);
  });
});
