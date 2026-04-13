import type {
  CaseDetailPreset,
  CommandAction,
  RevisionPreset,
  RereviewPreset,
  SummaryField,
  TimelineRecord,
  VariantOption,
} from './types';

export const demoCaseId = 'CASE-240013';

const commonSummary = [
  { label: '案件編號', value: demoCaseId },
  { label: '專案', value: '2026 住院病歷品質審查' },
  { label: '批次', value: 'BATCH-2026-04-09' },
  { label: '病歷類型', value: '住院摘要' },
];

const commonTraceability: SummaryField[] = [
  { label: 'projectVersionId', value: 'PROJ-V7' },
  { label: 'scoreSheetVersionId', value: 'SS-2026-R4' },
  { label: 'ruleSnapshotChecksum', value: 'CHK-88A1CE' },
  { label: 'medicalRecordKey', value: 'MR-88410021' },
  { label: 'hospitalId', value: 'HSP-TPE-01' },
  { label: 'departmentId', value: 'MED-21', muted: true },
];

const initialSummary: SummaryField[] = [
  { label: '初審版本', value: 'INITIAL v2' },
  { label: '初審醫師', value: 'REVIEWER-A21' },
  { label: '初審總分', value: '88.5' },
  { label: '提交時間', value: '2026-04-11 09:30' },
  { label: 'revision 次數', value: '2' },
  { label: '品質線進複審', value: '是' },
];

const rereviewSummary: SummaryField[] = [
  { label: '複審版本', value: 'REREVIEW v1' },
  { label: '複審醫師', value: 'REREVIEWER-B03' },
  { label: '最終分數', value: '91.0' },
  { label: '提交時間', value: '2026-04-12 14:15' },
  { label: '決策摘要', value: '維持初審主結論，補充評語' },
];

const timelineRecords: TimelineRecord[] = [
  {
    id: 'INITIAL-1',
    stage: 'INITIAL',
    revisionNo: 1,
    submissionId: 'SUB-I-001',
    submittedAt: '2026-04-10 11:20',
    submittedBy: 'REVIEWER-A21',
    status: 'SUPERSEDED',
    isCurrentEffective: false,
  },
  {
    id: 'INITIAL-2',
    stage: 'INITIAL',
    revisionNo: 2,
    submissionId: 'SUB-I-002',
    submittedAt: '2026-04-11 09:30',
    submittedBy: 'REVIEWER-A21',
    status: 'SUBMITTED',
    isCurrentEffective: true,
  },
  {
    id: 'REREVIEW-1',
    stage: 'REREVIEW',
    revisionNo: 1,
    submissionId: 'SUB-R-001',
    submittedAt: '2026-04-12 14:15',
    submittedBy: 'REREVIEWER-B03',
    status: 'SUBMITTED',
    isCurrentEffective: true,
  },
];

const rereviewReference: SummaryField[] = [
  { label: 'latest initial submissionId', value: 'SUB-I-002' },
  { label: '初審 reviewer', value: 'REVIEWER-A21' },
  { label: '初審 totalScore', value: '88.5' },
  { label: '初審 submittedAt', value: '2026-04-11 09:30' },
  { label: 'initial revision count', value: '2' },
  { label: '進入複審原因', value: '高於品質線門檻' },
];

const formOutline = [
  '評分表區塊 A placeholder',
  '評分表區塊 B placeholder',
  '評語 placeholder',
  '附件 placeholder',
  'final score preview placeholder',
];

const baseDecisionFields: SummaryField[] = [
  { label: 'base revision', value: 'REREVIEW v1' },
  { label: 'final score preview', value: '91.0' },
  { label: '結案語意', value: '提交後案件 CLOSED' },
];

const baseVersionFields: SummaryField[] = [
  { label: 'currentSubmissionId', value: 'SUB-R-001' },
  { label: 'revisionNo', value: '1' },
  { label: 'stage', value: 'REREVIEW' },
  { label: 'history entry', value: '可前往 UI-014', muted: true },
];

function command(label: string, resultMessage: string, tone: CommandAction['tone'] = 'default'): CommandAction {
  return { label, resultMessage, tone };
}

export const detailVariants: VariantOption[] = [
  { id: 'FULL', label: 'FULL', description: '完整 control center 視角' },
  { id: 'METADATA_ONLY', label: 'METADATA_ONLY', description: '僅 metadata 投影' },
  { id: 'TERMINAL_BLOCKED', label: 'TERMINAL_BLOCKED', description: 'terminal 狀態封鎖' },
  { id: 'ROLE_LIMITED', label: 'ROLE_LIMITED', description: '僅顯示合法角色入口' },
  { id: 'CLOSED_REREVIEWER_HINT', label: 'CLOSED_REREVIEWER_HINT', description: 'CLOSED 時只提示從歷程頁進例外修正' },
];

export const rereviewVariants: VariantOption[] = [
  { id: 'NEW_REREVIEW', label: 'NEW_REREVIEW', description: '第一次進入複審' },
  { id: 'EDIT_DRAFT', label: 'EDIT_DRAFT', description: '已有 draft，續編' },
  { id: 'REVISION', label: 'REVISION', description: '以已提交版本建立新 revision' },
  { id: 'READONLY_BLOCKED', label: 'READONLY_BLOCKED', description: '非合法狀態進入' },
  { id: 'CLOSED_REVISION_EXCEPTION', label: 'CLOSED_REVISION_EXCEPTION', description: 'CLOSED 但原複審者可做例外修正' },
];

export const revisionVariants: VariantOption[] = [
  { id: 'ALL', label: 'ALL', description: '全部 stage timeline' },
  { id: 'REREVIEW_ONLY', label: 'REREVIEW_ONLY', description: '聚焦複審 revision' },
  { id: 'FULL_COMPARE', label: 'FULL_COMPARE', description: '雙版比較摘要' },
  { id: 'METADATA_ONLY_COMPARE', label: 'METADATA_ONLY_COMPARE', description: 'metadata-only 比較降級' },
  { id: 'CLOSED_EXCEPTION_ENTRY', label: 'CLOSED_EXCEPTION_ENTRY', description: 'CLOSED 條件式例外入口' },
];

export const caseDetailPresets: Record<string, CaseDetailPreset> = {
  FULL: {
    variantId: 'FULL',
    variantLabel: 'FULL',
    role: 'HOSPITAL_MANAGER',
    projectionMode: 'FULL',
    caseStatus: 'REREVIEW_IN_PROGRESS',
    modeStrip: 'FULL_CONTROL_CENTER',
    summaryFields: [
      ...commonSummary,
      { label: '狀態', value: 'REREVIEW_IN_PROGRESS' },
      { label: '截止日', value: '2026-04-15' },
      { label: '初審醫師', value: 'REVIEWER-A21' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
    ],
    reviewerNotes: [
      'UI-010 是 control center，不直接 submit',
      'action 區只做 navigate-only',
    ],
    navigateActions: [
      { label: '前往案件分派', to: `/cases/${demoCaseId}/detail?variant=FULL#assign`, tone: 'primary' },
      { label: '前往初審作業', to: `/cases/${demoCaseId}/detail?variant=FULL#initial` },
      { label: '前往複審作業', to: `/cases/${demoCaseId}/rereview?variant=NEW_REREVIEW` },
      { label: '前往 Revision 歷程', to: `/cases/${demoCaseId}/revisions?variant=ALL` },
    ],
    nextStepHint: '目前控制中心可導向分派、複審與歷程頁。',
    traceabilityFields: commonTraceability,
    initialSummaryFields: initialSummary,
    rereviewSummaryFields: rereviewSummary,
  },
  METADATA_ONLY: {
    variantId: 'METADATA_ONLY',
    variantLabel: 'METADATA_ONLY',
    role: 'SYSTEM_ADMIN',
    projectionMode: 'METADATA_ONLY',
    caseStatus: 'REREVIEW_IN_PROGRESS',
    modeStrip: 'METADATA_CONTROL_CENTER',
    projectionNotice: '目前為 metadata-only 視角。完整答案、評語與附件摘要均已遮罩。',
    summaryFields: [
      ...commonSummary,
      { label: '狀態', value: 'REREVIEW_IN_PROGRESS' },
      { label: '院區', value: 'HSP-TPE-01' },
      { label: '初審醫師', value: 'REVIEWER-A21' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
    ],
    reviewerNotes: [
      'metadata-only 不拆新頁，仍是同一個 UI-010',
      '作業 action 必須被隱藏或降級',
    ],
    navigateActions: [
      { label: '前往 Revision 歷程', to: `/cases/${demoCaseId}/revisions?variant=METADATA_ONLY_COMPARE`, tone: 'primary' },
    ],
    nextStepHint: '系統管理員只可查看 metadata 歷程入口。',
    traceabilityFields: commonTraceability,
    initialSummaryFields: [
      { label: '初審版本', value: 'INITIAL v2' },
      { label: '提交時間', value: '2026-04-11 09:30' },
      { label: '內容', value: '已遮罩', muted: true },
    ],
    rereviewSummaryFields: [
      { label: '複審版本', value: 'REREVIEW v1' },
      { label: '提交時間', value: '2026-04-12 14:15' },
      { label: '內容', value: '已遮罩', muted: true },
    ],
  },
  TERMINAL_BLOCKED: {
    variantId: 'TERMINAL_BLOCKED',
    variantLabel: 'TERMINAL_BLOCKED',
    role: 'AUDITOR',
    projectionMode: 'FULL',
    caseStatus: 'CANCELLED',
    modeStrip: 'ROLE_LIMITED_CONTROL_CENTER',
    banner: {
      tone: 'blocked',
      title: '案件已取消',
      detail: '不可再進行初審或複審作業，只保留閱讀與追溯能力。',
    },
    summaryFields: [
      ...commonSummary,
      { label: '狀態', value: 'CANCELLED' },
      { label: '截止日', value: '已終止', muted: true },
      { label: '初審醫師', value: 'REVIEWER-A21' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
    ],
    reviewerNotes: ['terminal 狀態必須有 blocked reason', 'UI-010 仍可導向歷程頁'],
    navigateActions: [
      { label: '前往案件分派', disabled: true, reason: 'CASE_ALREADY_CANCELLED' },
      { label: '前往初審作業', disabled: true, reason: 'CASE_ALREADY_CANCELLED' },
      { label: '前往複審作業', disabled: true, reason: 'CASE_ALREADY_CANCELLED' },
      { label: '前往 Revision 歷程', to: `/cases/${demoCaseId}/revisions?variant=ALL`, tone: 'primary' },
    ],
    nextStepHint: '此頁僅保留 traceability 與歷程導頁。',
    traceabilityFields: commonTraceability,
    initialSummaryFields: initialSummary,
    rereviewSummaryFields: rereviewSummary,
  },
  ROLE_LIMITED: {
    variantId: 'ROLE_LIMITED',
    variantLabel: 'ROLE_LIMITED',
    role: 'REVIEWER',
    projectionMode: 'FULL',
    caseStatus: 'INITIAL_REVIEW_IN_PROGRESS',
    modeStrip: 'ROLE_LIMITED_CONTROL_CENTER',
    summaryFields: [
      ...commonSummary,
      { label: '狀態', value: 'INITIAL_REVIEW_IN_PROGRESS' },
      { label: '截止日', value: '2026-04-14' },
      { label: '初審醫師', value: 'REVIEWER-A21' },
      { label: '複審醫師', value: '尚未指派', muted: true },
    ],
    reviewerNotes: ['此 variant 用於驗證 role-limited action panel', '非本人或非對應 stage 的入口必須收斂'],
    navigateActions: [
      { label: '前往初審作業', to: `/cases/${demoCaseId}/detail?variant=ROLE_LIMITED#initial`, tone: 'primary' },
      { label: '前往案件分派', disabled: true, reason: 'ROLE_DENIED' },
      { label: '前往複審作業', disabled: true, reason: 'STAGE_MISMATCH' },
      { label: '前往 Revision 歷程', to: `/cases/${demoCaseId}/revisions?variant=ALL` },
    ],
    nextStepHint: 'Reviewer 只可前往自己的初審或歷程入口。',
    traceabilityFields: commonTraceability,
    initialSummaryFields: initialSummary,
    rereviewSummaryFields: [{ label: '複審摘要', value: '尚未進入複審', muted: true }],
  },
  CLOSED_REREVIEWER_HINT: {
    variantId: 'CLOSED_REREVIEWER_HINT',
    variantLabel: 'CLOSED_REREVIEWER_HINT',
    role: 'REREVIEWER',
    projectionMode: 'FULL',
    caseStatus: 'CLOSED',
    modeStrip: 'ROLE_LIMITED_CONTROL_CENTER',
    banner: {
      tone: 'exception',
      title: '案件已結案',
      detail: '若你是原複審醫師，請從 Revision 歷程頁進入複審修正例外流程。',
    },
    summaryFields: [
      ...commonSummary,
      { label: '狀態', value: 'CLOSED' },
      { label: '截止日', value: '已結案', muted: true },
      { label: '初審醫師', value: 'REVIEWER-A21' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
    ],
    reviewerNotes: ['closed exception 不在 UI-010 直接出 command', 'UI-010 只能提示轉往 UI-014'],
    navigateActions: [
      { label: '前往複審作業', disabled: true, reason: 'USE_REVISION_HISTORY_ENTRY' },
      { label: '前往 Revision 歷程', to: `/cases/${demoCaseId}/revisions?variant=CLOSED_EXCEPTION_ENTRY`, tone: 'primary' },
    ],
    nextStepHint: '若需修正，先進 UI-014，再由條件式入口導向 UI-013。',
    traceabilityFields: commonTraceability,
    initialSummaryFields: initialSummary,
    rereviewSummaryFields: rereviewSummary,
  },
};

export const rereviewPresets: Record<string, RereviewPreset> = {
  NEW_REREVIEW: {
    variantId: 'NEW_REREVIEW',
    variantLabel: 'NEW_REREVIEW',
    role: 'REREVIEWER',
    projectionMode: 'FULL',
    caseStatus: 'PENDING_REREVIEW',
    modeStrip: 'NEW_REREVIEW',
    summaryFields: [
      ...commonSummary,
      { label: '狀態', value: 'PENDING_REREVIEW' },
      { label: '截止日', value: '2026-04-15' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
      { label: '評分表版本', value: 'SS-2026-R4' },
    ],
    reviewerNotes: ['UI-013 必須是 command page', '初審摘要區是複審前置資訊'],
    initialReferenceFields: rereviewReference,
    formOutline,
    decisionFields: [
      ...baseDecisionFields,
      { label: '模式說明', value: '第一次開始複審' },
    ],
    versionFields: [{ label: '版本摘要', value: '尚無複審版本', muted: true }],
    commandActions: [
      command('保存草稿', '已建立 mock draft，仍停留在 UI-013。'),
      command('提交複審', '已模擬 submit rereview。下一步可前往 UI-014 驗證 history。', 'primary'),
      { label: '提交複審修正', disabled: true, reason: 'REVISION_NOT_STARTED' },
    ],
  },
  EDIT_DRAFT: {
    variantId: 'EDIT_DRAFT',
    variantLabel: 'EDIT_DRAFT',
    role: 'REREVIEWER',
    projectionMode: 'FULL',
    caseStatus: 'REREVIEW_IN_PROGRESS',
    modeStrip: 'EDIT_DRAFT',
    banner: {
      tone: 'info',
      title: '草稿模式',
      detail: '目前為複審草稿編輯模式，尚未正式提交。',
    },
    summaryFields: [
      ...commonSummary,
      { label: '狀態', value: 'REREVIEW_IN_PROGRESS' },
      { label: '截止日', value: '2026-04-15' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
      { label: '評分表版本', value: 'SS-2026-R4' },
    ],
    reviewerNotes: ['draft 續編時仍是同一 UI-013 骨架', 'command bar 可 save / submit，不可 revise'],
    initialReferenceFields: rereviewReference,
    formOutline,
    decisionFields: [
      ...baseDecisionFields,
      { label: '模式說明', value: '草稿續編' },
    ],
    versionFields: baseVersionFields,
    commandActions: [
      command('保存草稿', '已模擬更新 mock draft。'),
      command('提交複審', '已模擬從草稿提交，案件將進入 CLOSED。', 'primary'),
      { label: '提交複審修正', disabled: true, reason: 'DRAFT_MODE_ONLY' },
    ],
  },
  REVISION: {
    variantId: 'REVISION',
    variantLabel: 'REVISION',
    role: 'REREVIEWER',
    projectionMode: 'FULL',
    caseStatus: 'REREVIEW_IN_PROGRESS',
    modeStrip: 'REVISION',
    banner: {
      tone: 'info',
      title: '修正模式',
      detail: '系統將建立新 revision，不會覆寫既有複審提交。',
    },
    summaryFields: [
      ...commonSummary,
      { label: '狀態', value: 'REREVIEW_IN_PROGRESS' },
      { label: '截止日', value: '2026-04-15' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
      { label: '評分表版本', value: 'SS-2026-R4' },
    ],
    reviewerNotes: ['revision 不是 reopen', 'submit label 必須與一般 submit 區分'],
    initialReferenceFields: rereviewReference,
    formOutline,
    decisionFields: [
      ...baseDecisionFields,
      { label: '模式說明', value: '基於 REREVIEW v1 建立新 revision' },
    ],
    versionFields: [
      ...baseVersionFields,
      { label: 'basedOn', value: 'SUB-R-001 / rev1' },
    ],
    commandActions: [
      command('保存修正草稿', '已模擬 revision draft。'),
      command('提交複審修正', '已模擬建立新 revision，可返回 UI-014 檢視 compare。', 'primary'),
    ],
  },
  READONLY_BLOCKED: {
    variantId: 'READONLY_BLOCKED',
    variantLabel: 'READONLY_BLOCKED',
    role: 'REREVIEWER',
    projectionMode: 'FULL',
    caseStatus: 'CANCELLED',
    modeStrip: 'READONLY_BLOCKED',
    banner: {
      tone: 'blocked',
      title: '不可執行複審作業',
      detail: '案件已取消或 actor 不合法，表單不應可編輯。',
    },
    summaryFields: [
      ...commonSummary,
      { label: '狀態', value: 'CANCELLED' },
      { label: '截止日', value: '已終止', muted: true },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
      { label: '評分表版本', value: 'SS-2026-R4' },
    ],
    reviewerNotes: ['blocked 模式不可渲染可編輯主表單', 'command 只保留返回或閱讀導頁'],
    initialReferenceFields: rereviewReference,
    formOutline,
    decisionFields: [{ label: '模式說明', value: '全部 command blocked', muted: true }],
    versionFields: baseVersionFields,
    commandActions: [
      { label: '保存草稿', disabled: true, reason: 'CASE_ALREADY_CANCELLED' },
      { label: '提交複審', disabled: true, reason: 'CASE_ALREADY_CANCELLED' },
      { label: '提交複審修正', disabled: true, reason: 'CASE_ALREADY_CANCELLED' },
    ],
  },
  CLOSED_REVISION_EXCEPTION: {
    variantId: 'CLOSED_REVISION_EXCEPTION',
    variantLabel: 'CLOSED_REVISION_EXCEPTION',
    role: 'REREVIEWER',
    projectionMode: 'FULL',
    caseStatus: 'CLOSED',
    modeStrip: 'CLOSED_REVISION_EXCEPTION',
    banner: {
      tone: 'exception',
      title: 'CLOSED 例外修正模式',
      detail: '案件已結案。新提交已禁止，但原複審醫師可發起複審修正。',
    },
    summaryFields: [
      ...commonSummary,
      { label: '狀態', value: 'CLOSED' },
      { label: '截止日', value: '已結案', muted: true },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
      { label: '評分表版本', value: 'SS-2026-R4' },
    ],
    reviewerNotes: ['CLOSED 時 submit new rereview 必須 blocked', '合法 revise 入口必須保留且可執行'],
    initialReferenceFields: rereviewReference,
    formOutline,
    decisionFields: [
      ...baseDecisionFields,
      { label: '模式說明', value: '提交複審修正（例外）' },
    ],
    versionFields: [
      ...baseVersionFields,
      { label: 'basedOn', value: 'last effective submission / rev1' },
    ],
    commandActions: [
      command('保存修正草稿', '已模擬 CLOSED revision draft。'),
      { label: '提交複審', disabled: true, reason: 'CASE_ALREADY_CLOSED' },
      command('提交複審修正（例外）', '已模擬 CLOSED exception revise，下一步可回 UI-014 驗證新 revision。', 'warning'),
    ],
  },
};

export const revisionPresets: Record<string, RevisionPreset> = {
  ALL: {
    variantId: 'ALL',
    variantLabel: 'ALL',
    role: 'HOSPITAL_MANAGER',
    projectionMode: 'FULL',
    caseStatus: 'REREVIEW_IN_PROGRESS',
    modeStrip: 'HISTORY_ALL',
    tabMode: 'ALL',
    summaryFields: [
      { label: '案件編號', value: demoCaseId },
      { label: '專案', value: '2026 住院病歷品質審查' },
      { label: '狀態', value: 'REREVIEW_IN_PROGRESS' },
      { label: '初審醫師', value: 'REVIEWER-A21' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
      { label: '最新初審版', value: 'v2' },
      { label: '最新複審版', value: 'v1' },
    ],
    reviewerNotes: ['UI-014 僅能 navigate，不可 submit', 'ALL 模式下仍要清楚標示 stage'],
    timeline: timelineRecords,
    compareMode: 'SINGLE',
    compareSummary: [
      { label: 'detail mode', value: 'SINGLE_REVISION_SUMMARY' },
      { label: '聚焦版本', value: 'REREVIEW v1' },
      { label: '摘要', value: '已提交，為目前有效版本' },
    ],
    navigateActions: [
      { label: '返回案件詳情', to: `/cases/${demoCaseId}/detail?variant=FULL`, tone: 'primary' },
      { label: '前往複審作業', to: `/cases/${demoCaseId}/rereview?variant=EDIT_DRAFT` },
    ],
  },
  REREVIEW_ONLY: {
    variantId: 'REREVIEW_ONLY',
    variantLabel: 'REREVIEW_ONLY',
    role: 'REREVIEWER',
    projectionMode: 'FULL',
    caseStatus: 'REREVIEW_IN_PROGRESS',
    modeStrip: 'HISTORY_REREVIEW_ONLY',
    tabMode: 'REREVIEW_ONLY',
    summaryFields: [
      { label: '案件編號', value: demoCaseId },
      { label: '專案', value: '2026 住院病歷品質審查' },
      { label: '狀態', value: 'REREVIEW_IN_PROGRESS' },
      { label: '初審醫師', value: 'REVIEWER-A21' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
      { label: '最新複審版', value: 'v1' },
    ],
    reviewerNotes: ['REREVIEWER 預設聚焦複審 timeline', 'tab 是 query preset，不是新 page'],
    timeline: timelineRecords.filter((item) => item.stage === 'REREVIEW'),
    compareMode: 'SINGLE',
    compareSummary: [
      { label: 'detail mode', value: 'SINGLE_REVISION_SUMMARY' },
      { label: '聚焦版本', value: 'REREVIEW v1' },
      { label: '摘要', value: '可由本頁導向複審作業，不可在此提交' },
    ],
    navigateActions: [
      { label: '返回案件詳情', to: `/cases/${demoCaseId}/detail?variant=CLOSED_REREVIEWER_HINT` },
      { label: '前往複審作業', to: `/cases/${demoCaseId}/rereview?variant=REVISION`, tone: 'primary' },
    ],
  },
  FULL_COMPARE: {
    variantId: 'FULL_COMPARE',
    variantLabel: 'FULL_COMPARE',
    role: 'HOSPITAL_MANAGER',
    projectionMode: 'FULL',
    caseStatus: 'CLOSED',
    modeStrip: 'HISTORY_ALL',
    tabMode: 'ALL',
    summaryFields: [
      { label: '案件編號', value: demoCaseId },
      { label: '專案', value: '2026 住院病歷品質審查' },
      { label: '狀態', value: 'CLOSED' },
      { label: '初審醫師', value: 'REVIEWER-A21' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
      { label: '比較組合', value: 'INITIAL v1 vs v2' },
    ],
    reviewerNotes: ['FULL_COMPARE 用於驗證 compare panel 不是 full diff editor', '比較區只顯示摘要'],
    timeline: timelineRecords,
    compareMode: 'COMPARE',
    compareSummary: [
      { label: 'base', value: 'INITIAL v1' },
      { label: 'target', value: 'INITIAL v2' },
      { label: '題目變更數', value: '3 題' },
      { label: '分數差異', value: '84.0 → 88.5' },
      { label: '評語變更', value: '已更新' },
    ],
    navigateActions: [
      { label: '返回案件詳情', to: `/cases/${demoCaseId}/detail?variant=FULL` },
      { label: '前往複審作業', disabled: true, reason: 'HISTORY_PAGE_IS_NAVIGATE_ONLY' },
    ],
  },
  METADATA_ONLY_COMPARE: {
    variantId: 'METADATA_ONLY_COMPARE',
    variantLabel: 'METADATA_ONLY_COMPARE',
    role: 'SYSTEM_ADMIN',
    projectionMode: 'METADATA_ONLY',
    caseStatus: 'CLOSED',
    modeStrip: 'METADATA_HISTORY',
    tabMode: 'ALL',
    projectionNotice: '目前為 metadata-only compare。答案、評語與附件差異已遮罩。',
    summaryFields: [
      { label: '案件編號', value: demoCaseId },
      { label: '專案', value: '2026 住院病歷品質審查' },
      { label: '狀態', value: 'CLOSED' },
      { label: '最新初審版', value: 'v2' },
      { label: '最新複審版', value: 'v1' },
    ],
    reviewerNotes: ['metadata-only compare 必須降級，而不是隱含 full compare', 'system admin 不應看到作業入口'],
    timeline: timelineRecords,
    compareMode: 'METADATA_COMPARE',
    compareSummary: [
      { label: 'stage', value: 'REREVIEW' },
      { label: 'revisionNo', value: '1' },
      { label: 'submittedAt', value: '2026-04-12 14:15' },
      { label: 'submittedBy', value: 'REREVIEWER-B03' },
      { label: '比較內容', value: '詳細差異需完整視角', muted: true },
    ],
    navigateActions: [
      { label: '返回案件詳情', to: `/cases/${demoCaseId}/detail?variant=METADATA_ONLY`, tone: 'primary' },
    ],
  },
  CLOSED_EXCEPTION_ENTRY: {
    variantId: 'CLOSED_EXCEPTION_ENTRY',
    variantLabel: 'CLOSED_EXCEPTION_ENTRY',
    role: 'REREVIEWER',
    projectionMode: 'FULL',
    caseStatus: 'CLOSED',
    modeStrip: 'HISTORY_REREVIEW_ONLY',
    tabMode: 'REREVIEW_ONLY',
    banner: {
      tone: 'exception',
      title: 'CLOSED 條件式例外入口',
      detail: '本頁只能導向複審修正，不可直接提交。',
    },
    summaryFields: [
      { label: '案件編號', value: demoCaseId },
      { label: '專案', value: '2026 住院病歷品質審查' },
      { label: '狀態', value: 'CLOSED' },
      { label: '複審醫師', value: 'REREVIEWER-B03' },
      { label: '最新複審版', value: 'v1' },
    ],
    reviewerNotes: ['O1-C: UI-014 僅提供條件式導頁入口', '例外入口是 navigate-only'],
    timeline: timelineRecords.filter((item) => item.stage === 'REREVIEW'),
    compareMode: 'SINGLE',
    compareSummary: [
      { label: '目前有效版', value: 'REREVIEW v1' },
      { label: 'case status', value: 'CLOSED' },
      { label: '入口語意', value: '前往複審修正（例外）' },
    ],
    navigateActions: [
      { label: '返回案件詳情', to: `/cases/${demoCaseId}/detail?variant=CLOSED_REREVIEWER_HINT` },
      { label: '前往複審修正（例外）', to: `/cases/${demoCaseId}/rereview?variant=CLOSED_REVISION_EXCEPTION`, tone: 'warning' },
    ],
  },
};

export const homeScenarios = [
  {
    title: '高風險場景快速切換',
    items: [
      { label: 'UI-010 metadata-only', to: `/cases/${demoCaseId}/detail?variant=METADATA_ONLY` },
      { label: 'UI-010 CLOSED rereviewer hint', to: `/cases/${demoCaseId}/detail?variant=CLOSED_REREVIEWER_HINT` },
      { label: 'UI-014 metadata compare', to: `/cases/${demoCaseId}/revisions?variant=METADATA_ONLY_COMPARE` },
      { label: 'UI-014 CLOSED exception entry', to: `/cases/${demoCaseId}/revisions?variant=CLOSED_EXCEPTION_ENTRY` },
      { label: 'UI-013 CLOSED exception command', to: `/cases/${demoCaseId}/rereview?variant=CLOSED_REVISION_EXCEPTION` },
    ],
  },
  {
    title: 'flow validation',
    items: [
      { label: '從 UI-010 到 UI-014', to: `/cases/${demoCaseId}/detail?variant=CLOSED_REREVIEWER_HINT` },
      { label: '從 UI-014 到 UI-013', to: `/cases/${demoCaseId}/revisions?variant=CLOSED_EXCEPTION_ENTRY` },
      { label: '直接進 UI-013 revision', to: `/cases/${demoCaseId}/rereview?variant=REVISION` },
    ],
  },
];
