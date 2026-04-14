# UI View Model Spec (Cases Bridge)

## 文件狀態
- 類型：新增
- Layer：Layer 2 Repo-Aligned Frontend Bridge
- 範圍：僅 UI-009~UI-014

---

## 1) Source Inputs

- `docs/06-frontend/frontend-design-method.md`
- `docs/06-frontend/ui-state-actions-matrix.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/06-frontend/frontend-test-scenarios.md`
- `docs/03-contracts/api-spec.md`
- `docs/05-ui/ui-page-spec-ui009案件列表頁.md`
- `docs/05-ui/ui-page-spec-ui010案件詳情頁.md`
- `docs/05-ui/ui-page-spec-ui011案件分派頁.md`
- `docs/05-ui/ui-page-spec-ui012初審作業頁.md`
- `docs/05-ui/ui-page-spec-ui013複審作業頁.md`
- `docs/05-ui/ui-page-spec-ui014revision歷程頁.md`
- `docs/01-requirements/decision-log.md`

---

## 2) 與 Foundational Truth 的依賴

```yaml
dependency_mapping:
  mode_and_action_fields:
    source: ui-state-actions-matrix.md
  projection_rules:
    source: permission-matrix.md + frontend-design-method.md
  lifecycle_fields:
    source: state-machine.md
  revision_semantics:
    source: decision-log.md
```

---

## 3) Repo-Aligned Bridge 原則（ViewModel）

1. ViewModel 是 UI 用資料形狀，不等於 backend DTO。
2. Full/metadata-only 以 projection 欄位控制，不拆雙 model 樹。
3. 以 page responsibility 定義 page-level model；共享結構抽 shared sub-model。
4. action 顯示與可執行欄位以 policy 聚合欄位表達（例如 `canStartInitialReview`, `blockedReasonCode`）。
5. revision timeline 必留 `currentEffective` 與 `superseded` 語意。

---

## 4) Shared Sub-models

```yaml
shared_sub_models:
  - ActorScopeViewModel
  - ActionCapabilityViewModel
  - BlockedReasonViewModel
  - ProjectionModeViewModel
  - SubmissionVersionBadgeViewModel
```

### Shared 欄位定義（bridge）

| Model | Fields | 說明 |
|---|---|---|
| ActorScopeViewModel | `role`, `scopeType`, `isOwner`, `isInHospitalScope` | route/page guard 與 action policy 共用 |
| ActionCapabilityViewModel | `actionKey`, `visible`, `executable`, `blockedReasonCode`, `blockedReasonMessage` | 統一 action policy 輸出 |
| BlockedReasonViewModel | `code`, `message`, `source` | source = state/permission/api guard |
| ProjectionModeViewModel | `projectionMode` (`FULL`/`METADATA_ONLY`), `maskingLevel` | 同頁 projection 控制 |
| SubmissionVersionBadgeViewModel | `stage`, `revisionNo`, `isCurrentEffective`, `isSuperseded` | revision timeline /摘要共用 |

---

## 5) Page-level ViewModels

## 5.1 CaseListRowViewModel

- 用於頁面：UI-009
- 來源 API：`GET /review-cases`
- 欄位：
  - `caseId`, `projectName`, `batchId`, `medicalRecordType`, `caseStatus`, `dueAt`, `updatedAt`
  - `projectionMode`
  - `actionCapabilities[]`（UI 聚合）
  - `nextStepHint`（UI 聚合）
- FULL only：可延伸顯示業務摘要欄位（依 authority）
- METADATA_ONLY 保留（O3-C）：shared base + UI-009 extension（batchId/dueAt）
- 受 page mode/action policy 影響：`actionCapabilities`, `nextStepHint`

## 5.2 CaseListFilterViewModel

- 用於頁面：UI-009
- 來源 API：`GET /review-cases` query inputs
- 欄位：
  - `tabPresetKey`, `statusSet[]`, `keyword`, `hospitalId?`, `projectId?`, `page`, `pageSize`, `sort`
  - `roleDefaultPreset`（UI 聚合）
- UI 聚合結果：`roleDefaultPreset`, tab->filter mapping
- FULL/METADATA 差異：僅影響可見 filter 控件，不影響模型主結構

## 5.3 CaseDetailViewModel

- 用於頁面：UI-010
- 來源 API：`GET /review-cases/{caseId}`
- 欄位：
  - `caseId`, `caseStatus`, `projectSummary`, `assignmentSummary`, `lifecycleSummary`, `initialSummary`, `rereviewSummary`
  - `projectionMode`, `actionPanel`, `revisionEntry`
- UI 聚合結果：`actionPanel`, `revisionEntry`
- FULL only：`initialSummary.fullAnswerDigest`, `rereviewSummary.fullAnswerDigest`（若回傳）
- METADATA_ONLY 保留（O3-C）：shared base + UI-010 extension（lifecycleLatestStage/latestRevisionSummary）
- 受 policy/mode 影響：`actionPanel.availableActions`, `blockedHints`

## 5.4 CaseActionPanelViewModel

- 用於頁面：UI-010（主）、UI-009（次）
- 來源 authority：ui-state-actions-matrix + permission-matrix
- 欄位：
  - `pageMode`, `availableActions[]`, `hiddenActions[]`, `blockedActions[]`, `blockedReasons[]`, `nextStepHints[]`
- UI 聚合結果：全部為 policy 聚合
- FULL/METADATA 差異：metadata-only 僅保留 read/navigate 類 action

## 5.5 AssignmentPageViewModel

- 用於頁面：UI-011
- 來源 API：`GET /review-cases/{caseId}` + assign/reassign command responses
- 欄位：
  - `caseSummary`, `assignmentMode`, `currentReviewer`, `currentRereviewer`, `dueAt`
  - `candidateReviewerOptions[]`（若 API 支援）
  - `candidateRereviewerOptions[]`（若 API 支援）
  - `actionCapabilities[]`, `blockedReasons[]`
- UI 聚合結果：`assignmentMode`, `actionCapabilities`
- FULL only：候選人擴展資訊（若有）
- METADATA_ONLY：不適用主情境（本頁預設 command-only）

## 5.6 InitialReviewPageViewModel

- 用於頁面：UI-012
- 來源 API：`GET /review-cases/{caseId}` + draft/submit/revise responses
- 欄位：
  - `caseSummary`, `pageMode`, `baseSubmissionRef`, `scoreSheetSnapshotRef`
  - `draftFormState`, `revisionContext`, `versionSummary`, `actionCapabilities`, `blockedReasons`
- UI 聚合結果：`pageMode`, `revisionContext`, `actionCapabilities`
- FULL only：form answer/comment/attachment editable content
- METADATA_ONLY：不適用主情境
- policy 影響欄位：`pageMode`, `actionCapabilities`, `blockedReasons`

## 5.7 RereviewPageViewModel

- 用於頁面：UI-013
- 來源 API：`GET /review-cases/{caseId}` + rereview draft/submit/revise responses
- 欄位：
  - `caseSummary`, `pageMode`, `pageModeDisplayLabel`, `initialSummaryForReference`, `baseSubmissionRef`
  - `draftFormState`, `finalScorePreview`, `revisionContext`, `versionSummary`
  - `actionCapabilities`, `blockedReasons`
- UI 聚合結果：`pageMode`, `pageModeDisplayLabel`, `finalScorePreview`, `actionCapabilities`
- FULL only：rereview editable content
- METADATA_ONLY：不適用主情境
- policy 影響欄位：`pageMode`, `actionCapabilities`, `blockedReasons`
- display label note：`CLOSED_REVISION_EXCEPTION` → `例外修正（已結案）`

## 5.8 RevisionTimelineItemViewModel

- 用於頁面：UI-014
- 來源 API：`GET /review-cases/{caseId}/revisions`
- 欄位：
  - `submissionId`, `stage`, `revisionNo`, `submittedAt`, `submittedBy`, `status`
  - `isCurrentEffective`, `isSuperseded`, `projectionMode`
- UI 聚合結果：`isCurrentEffective`（由版本序列計算）
- FULL only：差異摘要字段（若 API 回）
- METADATA_ONLY 保留（O3-C）：shared base + UI-014 extension（stage/revisionNo/submittedAt/submittedBy/isCurrentEffective）

## 5.9 RevisionDetailSummaryViewModel

- 用於頁面：UI-014 detail/compare panel
- 來源 API：`GET /review-cases/{caseId}/submissions/{submissionId}`（單版，O2-A case-rooted）
- 欄位：
  - `mode` (`SINGLE`/`COMPARE`), `baseRevisionRef`, `targetRevisionRef`
  - `scoreDeltaSummary`, `answerChangeSummary`, `commentChangeSummary`, `attachmentChangeSummary`
  - `projectionMode`, `maskedFields[]`, `maskedValuePlaceholder`, `compareNotice`
- UI 聚合結果：`scoreDeltaSummary`, `maskedFields[]`, `compareNotice`
- FULL only：detail-level change summaries
- METADATA_ONLY 保留（DD-005 / O3-C）：`stage`, `revisionNo`, `submittedAt`, `submittedBy`, `isCurrentEffective`
- METADATA_ONLY 呈現策略：保留欄位 label，FULL-only 值以 `—` 佔位；compare 區顯示 `詳細差異內容不適用於此視角`

## 5.10 MetadataOnlyCaseProjectionViewModel

- 用於頁面：UI-009/UI-010/UI-014（system admin 或 metadata policy）
- 來源 authority：permission-matrix + api-spec metadata policy
- 欄位：
  - `caseId`, `caseStatus`, `projectName`, `assignmentMeta`, `revisionMeta`, `timestamps`
  - `maskingReason`, `allowedReadActions[]`, `projectionIndicatorText`, `projectionIndicatorPlacement`
- UI 聚合結果：`maskingReason`, `allowedReadActions[]`, `projectionIndicatorText`
- FULL only：無（此模型專為 metadata projection）
- 備註：同頁 projection 子模型，不是獨立 page model；在 UI-010/UI-014 與 C02 共存時，projection notice 以 header-level inline indicator 呈現。

---

## 6) ViewModel 與 Page 類型對齊

| Page Type | Pages | 核心 VM |
|---|---|---|
| Worklist | UI-009 | `CaseListRowViewModel`, `CaseListFilterViewModel` |
| Control Center | UI-010 | `CaseDetailViewModel`, `CaseActionPanelViewModel` |
| Command (Assignment) | UI-011 | `AssignmentPageViewModel` |
| Command (Initial) | UI-012 | `InitialReviewPageViewModel` |
| Command (Rereview) | UI-013 | `RereviewPageViewModel` |
| History | UI-014 | `RevisionTimelineItemViewModel`, `RevisionDetailSummaryViewModel` |

---

## 7) Cross-file Consistency Check（ViewModel 視角）

### 一致項（高信心）
- control center / command page / history page 差異可由不同 page-level VM 清楚承接。
- metadata-only 與 FULL 已以 projection 欄位實現，不需拆雙 route/page model。

### Inconsistency / Gaps
1. review gate 已批准 O1-C/O2-A/O3-C，本檔已回寫對齊；其餘差異需以 page spec 最終回寫檢核。

---

## 8) Pending Confirmation

1. revision compare 是否由單一 submission detail API 組合，或存在專用 compare API。
2. candidate reviewer/rereviewer options 的正式 API 來源與最小欄位。

