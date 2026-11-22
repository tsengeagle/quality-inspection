import { expect, test } from '@playwright/test';

async function mockAssignmentCase(
  page: Parameters<typeof test>[0]['page'],
  options?: {
    caseId?: string;
    mode?: 'ASSIGN_INITIAL' | 'REASSIGN_INITIAL' | 'MIXED_ASSIGNMENT' | 'READONLY_BLOCKED';
    blocked?: boolean;
    blockedReason?: string | null;
    allowedActions?: string[];
    commandStatus?: number;
    onCommandRequest?: (body: unknown) => void;
  }
): Promise<void> {
  const caseId = options?.caseId ?? 'CASE-011';
  const commandStatus = options?.commandStatus ?? 200;
  let status = options?.mode === 'MIXED_ASSIGNMENT' ? 'PENDING_REREVIEW' : 'SELECTED_PENDING_ASSIGNMENT';
  let mode = options?.mode ?? 'ASSIGN_INITIAL';
  let blocked = options?.blocked ?? false;
  let blockedReason = options?.blockedReason ?? null;
  let allowedActions = options?.allowedActions ?? (mode === 'MIXED_ASSIGNMENT' ? ['ASSIGN_REREVIEWER'] : ['ASSIGN_REVIEWER']);
  let currentReviewer: Record<string, unknown> | null = null;
  let currentRereviewer: Record<string, unknown> | null = null;

  await page.route(`**/api/quality-inspection/review-cases/${caseId}/assignment-context`, async route => {
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
          medicalRecordType: 'OUTPATIENT',
          status,
          pageMode: mode,
          blocked,
          blockedReason,
          allowedActions,
          currentReviewer,
          currentRereviewer,
          reviewerDueAt: '2026-04-20T12:00:00+08:00',
          rereviewerDueAt: mode === 'MIXED_ASSIGNMENT' ? '2026-04-26T12:00:00+08:00' : null,
          effectiveDueAt: mode === 'MIXED_ASSIGNMENT' ? '2026-04-26T12:00:00+08:00' : '2026-04-20T12:00:00+08:00'
        }
      })
    });
  });

  await page.route(`**/api/quality-inspection/review-cases/${caseId}/assignment-candidates?role=REVIEWER*`, async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: [
          {
            userId: 'reviewer-1',
            displayName: 'Dr. Reviewer One',
            displayLabel: 'reviewer-1 | Dr. Reviewer One',
            role: 'REVIEWER',
            active: true,
            selectable: true,
            blockedReason: null
          },
          {
            userId: 'reviewer-inactive',
            displayName: 'Dr. Inactive',
            displayLabel: 'reviewer-inactive | Dr. Inactive',
            role: 'REVIEWER',
            active: false,
            selectable: false,
            blockedReason: 'CANDIDATE_INACTIVE'
          }
        ]
      })
    });
  });

  await page.route(`**/api/quality-inspection/review-cases/${caseId}/assignment-candidates?role=REREVIEWER*`, async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: [
          {
            userId: 'rereviewer-1',
            displayName: 'Dr. ReReviewer One',
            displayLabel: 'rereviewer-1 | Dr. ReReviewer One',
            role: 'REREVIEWER',
            active: true,
            selectable: true,
            blockedReason: null
          },
          {
            userId: 'reviewer-1',
            displayName: 'Dr. Reviewer One',
            displayLabel: 'reviewer-1 | Dr. Reviewer One',
            role: 'REREVIEWER',
            active: true,
            selectable: false,
            blockedReason: 'REREVIEWER_SAME_AS_REVIEWER'
          }
        ]
      })
    });
  });

  await page.route(`**/api/quality-inspection/review-cases/${caseId}/assign-reviewer`, async route => {
    options?.onCommandRequest?.(route.request().postDataJSON());
    if (commandStatus === 200) {
      status = 'PENDING_INITIAL_REVIEW';
      mode = 'REASSIGN_INITIAL';
      allowedActions = ['REASSIGN_REVIEWER'];
      currentReviewer = {
        userId: 'reviewer-1',
        displayName: 'Dr. Reviewer One',
        displayLabel: 'reviewer-1 | Dr. Reviewer One',
        assignedAt: '2026-04-18T09:00:00+08:00'
      };
    }
    await route.fulfill({
      status: commandStatus,
      contentType: 'application/json',
      body: JSON.stringify(commandStatus === 200
        ? {
            success: true,
            data: {
              caseId
            }
          }
        : {
            success: false,
            message: 'assignment failed'
          })
    });
  });
}

async function mockInitialReviewCase(
  page: Parameters<typeof test>[0]['page'],
  options?: {
    caseId?: string;
    allowedActions?: string[];
    commandStatus?: number;
    onReviseRequest?: (body: unknown) => void;
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
    options?.onReviseRequest?.(route.request().postDataJSON());
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
    onReviseRequest?: (body: unknown) => void;
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
          status: 'REREVIEW_IN_PROGRESS',
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
            pageMode: 'REVISION',
            hasDraft: false,
            hasSubmittedBase: true,
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
            // D-033：後端若誤送這些值，頁面也不得顯示
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
          comment: 'revise base',
          totalScore: 84,
          maskedFields: []
        }
      })
    });
  });

  await page.route(`**/api/quality-inspection/review-cases/${caseId}/rereview/submissions/SUB-REREVIEW-002/revise`, async route => {
    options?.onReviseRequest?.(route.request().postDataJSON());
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
  test('loads /cases/:caseId/assign with backend assignment truth and candidate state', async ({ page }) => {
    await mockAssignmentCase(page);
    await page.goto('/cases/CASE-011/assign');

    await expect(page.locator('.assignment-page-title')).toHaveText('初審指派');
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('待初審指派');
    await expect(page.locator('[data-testid="reviewer-picker-slot"]')).toBeVisible();
    await expect(page.getByText('reviewer-1 | Dr. Reviewer One')).toBeVisible();
    await expect(page.getByText('此候選人目前未啟用，不能指派。')).toBeVisible();
    await expect(page.getByRole('button', { name: '提交初審指派' })).toBeEnabled();
  });

  test('executes assignment command and refetches backend truth after success', async ({ page }) => {
    let requestBody: unknown;
    await mockAssignmentCase(page, {
      onCommandRequest: body => {
        requestBody = body;
      }
    });

    await page.goto('/cases/CASE-011/assign');
    await page.getByRole('button', { name: '提交初審指派' }).click();

    expect(requestBody).toEqual({
      reviewerId: 'reviewer-1',
      dueAt: '2026-04-20T12:00:00+08:00'
    });
    await expect(page.getByText('初審指派已送出。')).toBeVisible();
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('初審改派');
    await expect(page.getByText('目前初審醫師：Dr. Reviewer One')).toBeVisible();
  });

  test('consumes backend truth and executes initial-review revise successfully', async ({ page }) => {
    let requestBody: unknown;
    await mockInitialReviewCase(page, {
      onReviseRequest: body => {
        requestBody = body;
      }
    });
    await page.goto('/cases/CASE-012/initial-review');

    await expect(page.locator('.initial-review-title')).toHaveText('初審作業頁');
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('REVISION');
    await expect(page.locator('[data-testid="initial-review-form-placeholder"]')).toBeVisible();
    await expect(page.locator('[data-testid="initial-review-commentary-placeholder"]')).toBeVisible();
    await expect(page.locator('[data-testid="initial-review-append-only-baseline"]')).toBeVisible();
    await expect(page.getByRole('button', { name: '提交初審修正' })).toBeEnabled();

    await page.getByTestId('initial-review-answer-input-0').fill('N');
    await page.getByTestId('initial-review-comment-input').fill('updated from page state');

    await page.getByRole('button', { name: '提交初審修正' }).click();

    expect(requestBody).toEqual({
      answers: [{ itemId: 'ITEM-1', answerValue: 'N' }],
      comment: 'updated from page state'
    });
    await expect(page.getByText('初審修正已提交。')).toBeVisible();
  });

  test('disables initial-review revise CTA when backend allowedActions removes the path', async ({ page }) => {
    await mockInitialReviewCase(page, { caseId: 'CASE-012-DISABLED', allowedActions: [] });
    await page.goto('/cases/CASE-012-DISABLED/initial-review');

    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('REVISION');
    await expect(page.getByRole('button', { name: '提交初審修正' })).toBeDisabled();
  });

  test('shows minimal error handling when rereview revise fails', async ({ page }) => {
    let requestBody: unknown;
    await mockRereviewCase(page, {
      onReviseRequest: body => {
        requestBody = body;
      }
    });
    await page.goto('/cases/CASE-013/rereview');

    await expect(page.locator('.rereview-title')).toHaveText('複審作業頁');
    await expect(page.locator('.mode-indicator-strip__label')).toHaveText('REVISION');
    await expect(page.locator('[data-testid="rereview-initial-summary-reference"]')).toBeVisible();
    await expect(page.locator('[data-testid="rereview-form-placeholder"]')).toBeVisible();
    // D-033：複審頁不顯示初審分數、最終分數與最終結論
    await expect(page.locator('[data-testid="rereview-final-decision-summary"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="rereview-initial-summary-reference"]')).not.toContainText('79');
    await expect(page.locator('body')).not.toContainText('最終分數');
    await expect(page.getByRole('button', { name: '提交複審修正' })).toBeEnabled();

    await page.getByTestId('rereview-answer-input-0').fill('Y');
    await page.getByTestId('rereview-comment-input').fill('revision from page state');

    await page.getByRole('button', { name: '提交複審修正' }).click();

    expect(requestBody).toEqual({
      answers: [{ itemId: 'ITEM-1', answerValue: 'Y' }],
      comment: 'revision from page state'
    });
    await expect(page.getByText('複審 command 執行失敗，limited wiring 僅提供最小錯誤提示。')).toBeVisible();
  });
});
