export type Role =
  | 'HOSPITAL_MANAGER'
  | 'REVIEWER'
  | 'REREVIEWER'
  | 'AUDITOR'
  | 'SYSTEM_ADMIN';

export type ProjectionMode = 'FULL' | 'METADATA_ONLY';

export type CaseStatus =
  | 'SELECTED_PENDING_ASSIGNMENT'
  | 'PENDING_INITIAL_REVIEW'
  | 'INITIAL_REVIEW_IN_PROGRESS'
  | 'INITIAL_REVIEW_COMPLETED'
  | 'PENDING_REREVIEW'
  | 'REREVIEW_IN_PROGRESS'
  | 'RETURNED'
  | 'CLOSED'
  | 'CANCELLED';

export interface SummaryField {
  label: string;
  value: string;
  muted?: boolean;
}

export interface TimelineRecord {
  id: string;
  stage: 'INITIAL' | 'REREVIEW';
  revisionNo: number;
  submissionId: string;
  submittedAt: string;
  submittedBy: string;
  status: 'SUBMITTED' | 'SUPERSEDED';
  isCurrentEffective: boolean;
}

export interface LinkAction {
  label: string;
  to?: string;
  tone?: 'default' | 'primary' | 'warning';
  disabled?: boolean;
  reason?: string;
}

export interface WorkQueueRow {
  caseId: string;
  title: string;
  fields: SummaryField[];
  actions: LinkAction[];
  note?: string;
}

export interface CommandAction {
  label: string;
  tone?: 'default' | 'primary' | 'warning';
  disabled?: boolean;
  reason?: string;
  resultMessage?: string;
}

export interface VariantOption {
  id: string;
  label: string;
  description: string;
}

export interface ReviewScenario {
  label: string;
  pageId: 'UI-009' | 'UI-010' | 'UI-011' | 'UI-012' | 'UI-013' | 'UI-014';
  variantId: string;
  description: string;
  path: string;
  riskTags: string[];
  highlights: string[];
}

export interface ReviewScenarioGroup {
  title: string;
  pageId: 'UI-009' | 'UI-010' | 'UI-011' | 'UI-012' | 'UI-013' | 'UI-014';
  archetype: string;
  pageRole: string;
  summary: string;
  highRiskTargets: string[];
  sharedStructures: string[];
  deferredNotes: string[];
  scenarios: ReviewScenario[];
}

export interface CriticalFlowStep {
  title: string;
  path: string;
  pageRole: string;
  note: string;
  riskTargets: string[];
}

export interface PagePresetBase {
  variantId: string;
  variantLabel: string;
  role: Role;
  projectionMode: ProjectionMode;
  caseStatus: CaseStatus;
  banner?: {
    tone: 'info' | 'blocked' | 'exception';
    title: string;
    detail: string;
  };
  projectionNotice?: string;
  modeStrip: string;
  summaryFields: SummaryField[];
  reviewerNotes: string[];
}

export interface CaseDetailPreset extends PagePresetBase {
  navigateActions: LinkAction[];
  nextStepHint: string;
  traceabilityFields: SummaryField[];
  initialSummaryFields: SummaryField[];
  rereviewSummaryFields: SummaryField[];
}

export interface RereviewPreset extends PagePresetBase {
  initialReferenceFields: SummaryField[];
  formOutline: string[];
  decisionFields: SummaryField[];
  versionFields: SummaryField[];
  commandActions: CommandAction[];
}

export interface RevisionPreset extends PagePresetBase {
  tabMode: 'ALL' | 'REREVIEW_ONLY';
  timeline: TimelineRecord[];
  compareMode: 'SINGLE' | 'COMPARE' | 'METADATA_COMPARE';
  compareSummary: SummaryField[];
  navigateActions: LinkAction[];
}

export interface WorkQueuePreset extends PagePresetBase {
  tabMode: string;
  filterChips: string[];
  rows: WorkQueueRow[];
  nextStepHint: string;
  emptyMessage?: string;
}

export interface AssignmentPickerPreviewModel {
  slotLabel: string;
  currentValue: string;
  actionLabel: string;
  helperText: string;
  candidateOptions: string[];
  state: 'enabled' | 'readonly' | 'blocked';
}

export interface AssignmentPreset extends PagePresetBase {
  assignmentMode: string;
  modeExplanation: string;
  reviewerSectionFields: SummaryField[];
  rereviewerSectionFields: SummaryField[];
  reviewerPicker: AssignmentPickerPreviewModel;
  rereviewerPicker: AssignmentPickerPreviewModel;
  helperLines: string[];
  commandActions: CommandAction[];
  navigateActions: LinkAction[];
}

export interface InitialReviewPreset extends PagePresetBase {
  formOutline: string[];
  revisionContextFields: SummaryField[];
  versionFields: SummaryField[];
  commandActions: CommandAction[];
}
