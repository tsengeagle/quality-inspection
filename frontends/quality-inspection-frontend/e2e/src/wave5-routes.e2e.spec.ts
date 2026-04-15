import { expect, test } from '@playwright/test';

async function mockInitialReviewCase(
  page: Parameters<typeof test>[0]['page'],
  options?: {
    caseId?: string;
    allowedActions?: string[];
    commandStatus?: number;
  }
): Promise<void> {
  const caseId = options?.caseId ?? 'CASE-012';
  const allowedActions = options?.allowedActions ?? ['REVISE_INITIAL_REVIEW'];
  const commandStatus = options?.commandStatus ?? 200;

  await page.route(`**/api/quality-inspection/review-cases/${caseId}`, async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          caseId,
          projectId: 'PROJECT-001',
          batchId: 'BATCH-001',
          hospitalId: 'H001',
          status: 'INITIAL_REVIEW_COMPLETED',
          reviewerId: 'reviewer-1',
          rereviewerId: 'rereviewer-1',
          medicalRecordType: 'OUTPATIENT',
          sourceCaseKey: 'SK-012',
          submissions: [],
          latestRereviewSubmissionId: null,
          projectionType: 'FULL',
          actorRole: 'REVIEWER',
          isOwner: true,
          initialReviewContext: {
            pageMode: 'REVISION',
            hasDraft: false,
            hasSubmittedBase: true,
            readonly: false,
            blocked: false,
            blockedReason: null,
            scoreSheetVersionId: 'SS-001',
            allowedActions,
            basedOnVersion: {
              submissionId: 'SUB-INITIAL-002',
              revisionNo: 2,
              status: 'SUBMITTED',
              submittedAt: '2026-04-15T09:00:00+08:00'
            }
          }
        }
      })
    });
  });

  await page.route('**/api/quality-inspection/review-submissions/SUB-INITIAL-002', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          submissionId: 'SUB-INITIAL-002',
          caseId,
          stage: 'INITIAL',
          revisionNo: 2,
          status: 'SUBMITTED',
          reviewerId: 'reviewer-1',
          answers: [
            { itemId: 'ITEM-1', answerValue: 'Y', derivedScore: 5 }
          ],
          comment: 'revision base comment',
          totalScore: 5,
          maskedFields: []
        }
      })
    });
  });

  await page.route(`**/api/quality-inspection/review-cases/${caseId}/initial-review/submissions/SUB-INITIAL-002/revise`, async route => {
    await route.fulfill({
      status: commandStatus,
      contentType: 'application/json',
      body: JSON.stringify(commandStatus === 200
        ? {
            success: true,
            data: {
              newSubmissionId: 'SUB-INITIAL-003',
              oldSubmissionId: 'SUB-INITIAL-002',
              revisionNo: 3,
              totalScore: 5,
              status: 'SUBMITTED',
              stage: 'INITIAL',
              caseId
            }
          }
        : {
            success: false,
            message: 'revise failed'
          })
    });
  });
}

async function mockRereviewCase(
  page: Parameters<typeof test>[0]['page'],
  options?: {
    caseId?: string;
    allowedActions?: string[];
    commandStatus?: number;
  }
): Promise<void> {
  const caseId = options?.caseId ?? 'CASE-013';
  const allowedActions = options?.allowedActions ?? ['REVISE_REREVIEW'];
  const commandStatus = options?.commandStatus ?? 500;

  await page.route(`**/api/quality-inspection/review-cases/${caseId}`, async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          caseId,
          projectId: 'PROJECT-001',
          batchId: 'BATCH-001',
          hospitalId: 'H001',
          status: 'CLOSED',
          reviewerId: 'reviewer-1',
          rereviewerId: 'rereviewer-1',
          medicalRecordType: 'OUTPATIENT',
          sourceCaseKey: 'SK-013',
          submissions: [],
          latestRereviewSubmissionId: 'SUB-REREVIEW-002',
          projectionType: 'FULL',
          actorRole: 'REREVIEWER',
          isOwner: true,
          rereviewContext: {
            pageMode: 'REVISION_EXCEPTION_CLOSED',
            hasDraft: false,
            hasSubmittedBase: true,
            closedRevisionException: true,
            readonly: false,
            blocked: false,
            blockedReason: null,
            scoreSheetVersionId: 'SS-001',
            allowedActions,
            basedOnVersion: {
              submissionId: 'SUB-REREVIEW-002',
              revisionNo: 2,
              status: 'SUBMITTED',
              submittedAt: '2026-04-15T11:00:00+08:00'
            },
            initialSubmissionId: 'SUB-INITIAL-001',
            initialReviewerId: 'reviewer-1',
            initialReviewerName: null,
            initialTotalScore: 79,
            finalScore: 84,
            finalConclusion: 'PASS'
          }
        }
      })
    });
  });

  await page.route('**/api/quality-inspection/review-submissions/SUB-REREVIEW-002', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          submissionId: 'SUB-REREVIEW-002',
          caseId,
          stage: 'REREVIEW',
          revisionNo: 2,
          status: 'SUBMITTED',
          reviewerId: 'rereviewer-1',
          answers: [
            { itemId: 'ITEM-1', answerValue: 'N', derivedScore: 4 }
          ],
          comment: 'exception revise base',
          totalScore: 84,
          maskedFields: []
        }
      })
    });
  });

  await page.route(`**/api/quality-inspection/review-cases/${caseId}/rereview/submissions/SUB-REREVIEW-002/revise`, async route => {
    await route.fulfill({
      status: commandStatus,
      contentType: 'application/json',
      body: JSON.stringify(commandStatus === 200
        ? {
            success: true,
            data: {
              newSubmissionId: 'SUB-REREVIEW-003',
              oldSubmissionId: 'SUB-REREVIEW-002',
              revisionNo: 3,
              totalScore: 84,
              status: 'SUBMITTED',
              stage: 'REREVIEW',
              caseId
            }
          }
        : {
            success: false,
            message: 'revise failed'
          })
    });
  });
}

test.describe('Wave 5 command-flow limited wiring smoke', () => {
  test('loads /cases/:caseId/assign with Wave 5 assignment skeleton only', async ({ page }) => {
    await page.goto('/cases/CASE-011/assign');

    await expect(page.locator('.assignment-page-title')).toHaveText('案件分派頁');
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('ASSIGN_INITIAL');
    await expect(page.locator('[data-testid="reviewer-picker-slot"]')).toBeVisible();
    await expect(page.getByText('submit 成功')).toHaveCount(0);
    await expect(page.getByText('save 成功')).toHaveCount(0);
  });

  test('consumes backend truth and executes initial-review revise successfully', async ({ page }) => {
    await mockInitialReviewCase(page);
    await page.goto('/cases/CASE-012/initial-review');

    await expect(page.locator('.initial-review-title')).toHaveText('初審作業頁');
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('REVISION');
    await expect(page.locator('[data-testid="initial-review-form-placeholder"]')).toBeVisible();
    await expect(page.locator('[data-testid="initial-review-commentary-placeholder"]')).toBeVisible();
    await expect(page.locator('[data-testid="initial-review-append-only-baseline"]')).toBeVisible();
    await expect(page.getByRole('button', { name: '提交初審修正' })).toBeEnabled();

    await page.getByRole('button', { name: '提交初審修正' }).click();

    await expect(page.getByText('初審修正已提交。')).toBeVisible();
  });

  test('disables initial-review revise CTA when backend allowedActions removes the path', async ({ page }) => {
    await mockInitialReviewCase(page, { caseId: 'CASE-012-DISABLED', allowedActions: [] });
    await page.goto('/cases/CASE-012-DISABLED/initial-review');

    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('REVISION');
    await expect(page.getByRole('button', { name: '提交初審修正' })).toBeDisabled();
  });

  test('shows minimal error handling when rereview revise fails', async ({ page }) => {
    await mockRereviewCase(page);
    await page.goto('/cases/CASE-013/rereview');

    await expect(page.locator('.rereview-title')).toHaveText('複審作業頁');
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('CLOSED_REVISION_EXCEPTION');
    await expect(page.locator('[data-testid="rereview-initial-summary-reference"]')).toBeVisible();
    await expect(page.locator('[data-testid="rereview-form-placeholder"]')).toBeVisible();
    await expect(page.locator('[data-testid="rereview-c16-placeholder-label"]')).toBeVisible();
    await expect(page.locator('[data-testid="rereview-final-decision-summary"]')).toContainText('PASS / 最終分數 84');
    await expect(page.getByRole('button', { name: '提交複審修正' })).toBeEnabled();

    await page.getByRole('button', { name: '提交複審修正' }).click();

    await expect(page.getByText('複審 command 執行失敗，limited wiring 僅提供最小錯誤提示。')).toBeVisible();
  });
});
