import { APIRequestContext, expect, Locator, Page, ConsoleMessage } from '@playwright/test';
import { DEMO_API_KEY, DEMO_API_PATHS, DEMO_CONTRACT, DEMO_ROUTES, DemoFlowMode } from './demo-contract';

type JsonObject = Record<string, any>;

export interface DemoDiagnostics {
  consoleErrors: string[];
  pageErrors: string[];
  clear(): void;
  assertClean(label: string): void;
  dispose(): void;
}

export interface RunCanonicalFlowOptions {
  mode: DemoFlowMode;
  request?: APIRequestContext;
  strictApiUi?: boolean;
  verifyReload?: boolean;
  diagnostics?: DemoDiagnostics;
}

function isFatalConsoleMessage(message: ConsoleMessage): boolean {
  if (message.type() !== 'error') {
    return false;
  }

  const text = message.text();
  return !text.includes('[webpack-dev-server]');
}

export function attachDemoDiagnostics(page: Page): DemoDiagnostics {
  const consoleErrors: string[] = [];
  const pageErrors: string[] = [];

  const onConsole = (message: ConsoleMessage) => {
    if (isFatalConsoleMessage(message)) {
      consoleErrors.push(message.text());
    }
  };
  const onPageError = (error: Error) => {
    pageErrors.push(error.message);
  };

  page.on('console', onConsole);
  page.on('pageerror', onPageError);

  return {
    consoleErrors,
    pageErrors,
    clear() {
      consoleErrors.length = 0;
      pageErrors.length = 0;
    },
    assertClean(label: string) {
      expect.soft(consoleErrors, `${label} 不應有新的 console hard error`).toEqual([]);
      expect.soft(pageErrors, `${label} 不應有 page error`).toEqual([]);
    },
    dispose() {
      page.off('console', onConsole);
      page.off('pageerror', onPageError);
    }
  };
}

export async function assertFrontendReady(request: APIRequestContext): Promise<void> {
  const response = await request.get('/');
  expect(response.status(), 'frontend standalone 應可回應').toBe(200);
}

export async function assertBackendReady(request: APIRequestContext): Promise<JsonObject> {
  const response = await request.get(DEMO_API_PATHS.directExecuteContext, {
    headers: {
      'api-key': DEMO_API_KEY
    }
  });

  expect(response.status(), 'backend 應能直連回傳 execute-context').toBe(200);
  const payload = await response.json();
  expect(payload.data.projectId).toBe(DEMO_CONTRACT.projectId);
  expect(payload.data.projectStatus).toBe(DEMO_CONTRACT.projectStatus);
  return payload;
}

export async function assertProxyReady(request: APIRequestContext): Promise<JsonObject> {
  const response = await request.get(DEMO_API_PATHS.proxyExecuteContext);
  expect(response.status(), 'proxy 應能轉發 execute-context').toBe(200);
  const payload = await response.json();
  expect(payload.data.projectId).toBe(DEMO_CONTRACT.projectId);
  return payload;
}

async function waitForPageStable(page: Page, mode: DemoFlowMode): Promise<void> {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForLoadState('networkidle');
  if (mode === 'demo') {
    await page.waitForTimeout(800);
  }
}

async function deliberatePause(page: Page, mode: DemoFlowMode, locator?: Locator): Promise<void> {
  if (locator) {
    await locator.scrollIntoViewIfNeeded();
  }

  if (mode === 'demo') {
    await page.waitForTimeout(1400);
  }
}

async function reloadAndAssert(page: Page, matcher: (url: string) => boolean, heading: string): Promise<void> {
  const responsePromise = page.waitForResponse(response => matcher(response.url()) && response.status() === 200);
  await page.reload({ waitUntil: 'networkidle' });
  await responsePromise;
  await expect(page.getByRole('heading', { name: heading })).toBeVisible();
}

async function openSamplingExecutePage(page: Page, mode: DemoFlowMode): Promise<JsonObject> {
  const responsePromise = page.waitForResponse(response => response.url().includes(`/review-projects/${DEMO_CONTRACT.projectId}/sampling/execute-context`) && response.status() === 200);
  await page.goto(DEMO_ROUTES.samplingExecute);
  const response = await responsePromise;
  await expect(page.getByRole('heading', { name: '抽選前確認與正式執行' })).toBeVisible();
  await waitForPageStable(page, mode);
  return response.json();
}

async function verifySamplingExecutePage(page: Page, payload: JsonObject, mode: DemoFlowMode, strictApiUi: boolean, verifyReload: boolean): Promise<void> {
  const summaryGrid = page.getByTestId('sampling-execute-summary-grid');
  const resultPanel = page.getByTestId('sampling-execute-result-panel');

  await expect(summaryGrid).toBeVisible();
  await expect(summaryGrid.getByText(DEMO_CONTRACT.projectName)).toBeVisible();
  await expect(summaryGrid.getByText(DEMO_CONTRACT.projectStatus)).toBeVisible();
  await expect(summaryGrid.getByText(DEMO_CONTRACT.versionLabel, { exact: true })).toBeVisible();
  await expect(summaryGrid.getByText(DEMO_CONTRACT.scoreSheetId)).toBeVisible();
  await expect(summaryGrid.getByText(DEMO_CONTRACT.qualityThreshold, { exact: true })).toBeVisible();

  await deliberatePause(page, mode, summaryGrid);

  await expect(resultPanel).toBeVisible();
  await expect(resultPanel.getByText(`正式批次：${DEMO_CONTRACT.batchId}`)).toBeVisible();
  await expect(resultPanel.getByText(`開始時間：${DEMO_CONTRACT.startedAt}`)).toBeVisible();
  await expect(resultPanel.getByText(`完成時間：${DEMO_CONTRACT.finishedAt}`)).toBeVisible();

  if (strictApiUi) {
    expect(payload.data.projectStatus).toBe(DEMO_CONTRACT.projectStatus);
    expect(payload.data.versionNo).toBe(DEMO_CONTRACT.versionNo);
    expect(payload.data.scoreSheetId).toBe(DEMO_CONTRACT.scoreSheetId);
    expect(payload.data.qualityThreshold).toBe(DEMO_CONTRACT.qualityThreshold);
    expect(payload.data.postExecuteSummary.startedAt).toBe(DEMO_CONTRACT.startedAt);
    expect(payload.data.postExecuteSummary.finishedAt).toBe(DEMO_CONTRACT.finishedAt);
  }

  if (verifyReload) {
    await reloadAndAssert(page, url => url.includes(`/review-projects/${DEMO_CONTRACT.projectId}/sampling/execute-context`), '抽選前確認與正式執行');
  }
}

async function openSamplingBatchListPage(page: Page, mode: DemoFlowMode): Promise<JsonObject> {
  const responsePromise = page.waitForResponse(response => response.url().includes('/sampling-batches') && response.status() === 200);
  await page.goto(DEMO_ROUTES.samplingBatches);
  const response = await responsePromise;
  await expect(page.getByRole('heading', { name: '抽選批次追蹤' })).toBeVisible();
  await waitForPageStable(page, mode);
  return response.json();
}

async function verifySamplingBatchListPage(page: Page, payload: JsonObject, mode: DemoFlowMode, strictApiUi: boolean, verifyReload: boolean): Promise<void> {
  const batchRow = page.getByTestId(`batch-row-${DEMO_CONTRACT.batchId}`);
  await expect(batchRow).toBeVisible();
  await expect(batchRow.getByText(DEMO_CONTRACT.batchId, { exact: true })).toBeVisible();
  await expect(batchRow.getByText(`專案：${DEMO_CONTRACT.projectName}`)).toBeVisible();
  await expect(batchRow.getByText(`專案版本：${DEMO_CONTRACT.versionLabel}`)).toBeVisible();
  await expect(batchRow.getByText(`排除件數：${DEMO_CONTRACT.excludedCount}`)).toBeVisible();
  await expect(batchRow.getByRole('button', { name: '查看案件' })).toBeVisible();

  await deliberatePause(page, mode, batchRow);

  if (strictApiUi) {
    const batch = payload.data.content[0];
    expect(batch.projectName).toBe(DEMO_CONTRACT.projectName);
    expect(batch.projectVersionNumber).toBe(DEMO_CONTRACT.versionNo);
    expect(batch.excludedCount).toBe(DEMO_CONTRACT.excludedCount);
    expect(batch.status).toBe(DEMO_CONTRACT.batchStatus);
  }

  if (verifyReload) {
    await reloadAndAssert(page, url => url.includes('/sampling-batches'), '抽選批次追蹤');
  }
}

async function openCaseAssignmentPage(page: Page, mode: DemoFlowMode): Promise<{ context: JsonObject; reviewerCandidates: JsonObject; rereviewerCandidates: JsonObject }> {
  const contextPromise = page.waitForResponse(response => response.url().includes(`/review-cases/${DEMO_CONTRACT.caseId}/assignment-context`) && response.status() === 200);
  const reviewerCandidatesPromise = page.waitForResponse(response => response.url().includes(`/review-cases/${DEMO_CONTRACT.caseId}/assignment-candidates?role=REVIEWER`) && response.status() === 200);
  const rereviewerCandidatesPromise = page.waitForResponse(response => response.url().includes(`/review-cases/${DEMO_CONTRACT.caseId}/assignment-candidates?role=REREVIEWER`) && response.status() === 200);

  await page.goto(DEMO_ROUTES.caseAssignment);

  const [contextResponse, reviewerCandidatesResponse, rereviewerCandidatesResponse] = await Promise.all([
    contextPromise,
    reviewerCandidatesPromise,
    rereviewerCandidatesPromise
  ]);

  await expect(page.getByRole('heading', { name: '初審指派' })).toBeVisible();
  await waitForPageStable(page, mode);

  return {
    context: await contextResponse.json(),
    reviewerCandidates: await reviewerCandidatesResponse.json(),
    rereviewerCandidates: await rereviewerCandidatesResponse.json()
  };
}

async function verifyCaseAssignmentPage(page: Page, payloads: { context: JsonObject; reviewerCandidates: JsonObject; rereviewerCandidates: JsonObject }, mode: DemoFlowMode, strictApiUi: boolean, verifyReload: boolean): Promise<void> {
  const summaryGrid = page.getByTestId('assignment-summary-grid');
  const reviewerSummary = page.getByTestId('assignment-current-reviewer-summary');
  const reviewerPicker = page.getByTestId('reviewer-picker-slot');
  const submitZone = page.getByTestId('assignment-submit-zone');

  await expect(summaryGrid).toBeVisible();
  await expect(summaryGrid.getByText(DEMO_CONTRACT.projectName)).toBeVisible();
  await expect(summaryGrid.getByText(DEMO_CONTRACT.sourceCaseKey)).toBeVisible();
  await deliberatePause(page, mode, summaryGrid);

  await expect(reviewerSummary.getByText(`初審指派時間：${DEMO_CONTRACT.assignedAtFallback}`)).toBeVisible();
  await expect(reviewerPicker.getByText('reviewer-1 | Dr. Reviewer One')).toBeVisible();
  await expect(reviewerPicker.getByText(DEMO_CONTRACT.inactiveCandidateLabel)).toBeVisible();
  await expect(reviewerPicker.getByText('此候選人目前未啟用，不能指派。')).toBeVisible();
  await expect(submitZone).toBeVisible();

  if (strictApiUi) {
    expect(payloads.context.data.projectName).toBe(DEMO_CONTRACT.projectName);
    expect(payloads.context.data.sourceCaseKey).toBe(DEMO_CONTRACT.sourceCaseKey);
    expect(payloads.context.data.currentReviewer).toBeNull();
    expect(payloads.reviewerCandidates.data.some((candidate: JsonObject) => candidate.userId === DEMO_CONTRACT.inactiveCandidateId && candidate.blockedReason === DEMO_CONTRACT.inactiveCandidateBlockedReason)).toBeTruthy();
    expect(payloads.rereviewerCandidates.data.length).toBeGreaterThan(0);
  }

  if (verifyReload) {
    await reloadAndAssert(page, url => url.includes(`/review-cases/${DEMO_CONTRACT.caseId}/assignment-context`), '初審指派');
  }
}

export async function runCanonicalDemoFlow(page: Page, options: RunCanonicalFlowOptions): Promise<void> {
  const diagnostics = options.diagnostics ?? attachDemoDiagnostics(page);

  const executePayload = await openSamplingExecutePage(page, options.mode);
  await verifySamplingExecutePage(page, executePayload, options.mode, !!options.strictApiUi, !!options.verifyReload);
  diagnostics.assertClean('UI-006');
  diagnostics.clear();

  const batchPayload = await openSamplingBatchListPage(page, options.mode);
  await verifySamplingBatchListPage(page, batchPayload, options.mode, !!options.strictApiUi, !!options.verifyReload);
  diagnostics.assertClean('UI-007');
  diagnostics.clear();

  const assignmentPayloads = await openCaseAssignmentPage(page, options.mode);
  await verifyCaseAssignmentPage(page, assignmentPayloads, options.mode, !!options.strictApiUi, !!options.verifyReload);
  diagnostics.assertClean('UI-011');
}
