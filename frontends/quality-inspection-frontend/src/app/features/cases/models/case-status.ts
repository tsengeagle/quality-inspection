/**
 * 案件狀態的值。必須與後端的 CaseStatus 相同（specs/review-case/model.yaml 的列舉 CaseStatus）。
 * 列表的 status 查詢參數只接受這些值，其他值後端回 400 BAD_REQUEST（RC-LST-02）。
 */
export const CASE_STATUSES = [
  'SELECTED_PENDING_ASSIGNMENT',
  'PENDING_INITIAL_REVIEW',
  'INITIAL_REVIEW_IN_PROGRESS',
  'INITIAL_REVIEW_COMPLETED',
  'PENDING_REREVIEW',
  'REREVIEW_IN_PROGRESS',
  'RETURNED',
  'CLOSED',
  'CANCELLED'
] as const;

export type CaseStatus = typeof CASE_STATUSES[number];
