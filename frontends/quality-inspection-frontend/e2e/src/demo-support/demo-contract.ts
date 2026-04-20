export const DEMO_API_KEY = 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJ0ei11c2VyIjoie1widXNlclwiOntcInVzZXJfaWRcIjpcImRlbW8tbWFuYWdlclwiLFwidXNlcl9uYW1lXCI6XCJEZW1vIE1hbmFnZXJcIixcInVzZXJfcm9sZVwiOlt7XCJyb2xlX25hbWVcIjpcImhvc3BpdGFsX21hbmFnZXJcIixcImVuYWJsZWRcIjp0cnVlfV0sXCJob3NwaXRhbF9pZFwiOlwiSDAwMVwifX0ifQ.demo';

export const DEMO_CONTRACT = {
  projectId: 'PROJECT-001',
  batchId: 'BATCH-001',
  caseId: 'CASE-011',
  projectName: '2026 Q2 門診品質抽選專案',
  projectStatus: 'ACTIVE',
  versionNo: 1,
  versionLabel: 'v1',
  scoreSheetId: 'SS-PROJECT-001-V1',
  qualityThreshold: '85.00',
  sourceCaseKey: 'MR-CASE-011',
  batchStatus: 'COMPLETED',
  excludedCount: 0,
  startedAt: '2026-04-18T01:00:00Z',
  finishedAt: '2026-04-18T01:05:00Z',
  assignedAtFallback: '尚未指派',
  inactiveCandidateId: 'reviewer-inactive',
  inactiveCandidateLabel: 'reviewer-inactive | Dr. Inactive',
  inactiveCandidateBlockedReason: 'CANDIDATE_INACTIVE'
} as const;

export const DEMO_ROUTES = {
  samplingExecute: `/sampling/${DEMO_CONTRACT.projectId}`,
  samplingBatches: '/sampling/batches',
  caseAssignment: `/cases/${DEMO_CONTRACT.caseId}/assign`
} as const;

export const DEMO_API_PATHS = {
  proxyExecuteContext: `/api/quality-inspection/review-projects/${DEMO_CONTRACT.projectId}/sampling/execute-context`,
  proxySamplingBatches: `/api/quality-inspection/sampling-batches?projectId=${DEMO_CONTRACT.projectId}`,
  proxyAssignmentContext: `/api/quality-inspection/review-cases/${DEMO_CONTRACT.caseId}/assignment-context`,
  proxyReviewerCandidates: `/api/quality-inspection/review-cases/${DEMO_CONTRACT.caseId}/assignment-candidates?role=REVIEWER`,
  proxyRereviewerCandidates: `/api/quality-inspection/review-cases/${DEMO_CONTRACT.caseId}/assignment-candidates?role=REREVIEWER`,
  directExecuteContext: `http://localhost:8080/api/quality-inspection/review-projects/${DEMO_CONTRACT.projectId}/sampling/execute-context`
} as const;

export type DemoFlowMode = 'preflight' | 'demo' | 'e2e';
