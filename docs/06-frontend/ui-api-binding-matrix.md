# UI API Binding Matrix (Cases Bridge)

## 文件狀態
- 類型：新增
- Layer：Layer 2 Repo-Aligned Frontend Bridge
- 範圍：僅 UI-009~UI-014

---

## 1) Source Inputs

- `docs/06-frontend/frontend-design-method.md`
- `docs/06-frontend/ui-state-actions-matrix.md`
- `docs/06-frontend/ui-view-model-spec.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/03-contracts/api-spec.md`
- `docs/01-requirements/decision-log.md`
- `docs/05-ui/ui-page-spec-ui009案件列表頁.md`
- `docs/05-ui/ui-page-spec-ui010案件詳情頁.md`
- `docs/05-ui/ui-page-spec-ui011案件分派頁.md`
- `docs/05-ui/ui-page-spec-ui012初審作業頁.md`
- `docs/05-ui/ui-page-spec-ui013複審作業頁.md`
- `docs/05-ui/ui-page-spec-ui014revision歷程頁.md`

---

## 2) 與 Foundational Truth 的依賴

```yaml
depends_on:
  page_mode_and_action_policy:
    source: ui-state-actions-matrix.md
  projection_and_scope:
    source: permission-matrix.md
  lifecycle_and_terminal:
    source: state-machine.md
  revision_rules:
    source: decision-log.md
  target_view_models:
    source: ui-view-model-spec.md
```

---

## 3) API Binding 原則

1. 區分 read model API（query）與 command API（state change）。
2. 不假設「所有區塊進頁即打 API」：依 page mode / user trigger 決定。
3. blocked/forbidden/metadata-only 必須有 API 層處理策略。
4. command 成功後採「最小必要 refetch」維持 control center 與 history 一致。
5. revision 一律 append-only 語意，不映射成 update existing submission。

---

## 4) Page Section x API Binding Matrix

| pageId | pageSection | uiPurpose | api | method | triggerTiming | requestShape | responseBinding | targetViewModel | loadingStateHandling | errorStateHandling | blockedStateHandling | cacheOrRefetchPolicy | orchestrationNotes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-009 | Worklist table | list+next action | `/review-cases` | GET | page enter / filter change / tab switch | query params from filter VM | list rows + paging | `CaseListRowViewModel[]` + `CaseListFilterViewModel` | table skeleton | inline error banner | forbidden -> empty+message | cache by query key; tab switch reuse | tab=query preset |
| UI-010 | Case summary | control center baseline read | `/review-cases/{caseId}` | GET | page enter | path caseId + projection context | case detail payload | `CaseDetailViewModel` | page skeleton | page-level retry | 403/404 -> blocked page | cache by caseId, invalidate after commands | projection-aware mapping |
| UI-010 | Lifecycle panel | traceability read | `/review-cases/{caseId}/lifecycle` | GET | panel expand or tab activate | path caseId | lifecycle timeline | submodel in `CaseDetailViewModel` | section loader | section error card | scope denied -> hidden section | stale-while-revalidate | lifecycle 為 secondary fetch |
| UI-014 | Revision timeline | history list | `/review-cases/{caseId}/revisions` | GET | page enter / stage tab switch | caseId + stage filter | revision rows | `RevisionTimelineItemViewModel[]` | timeline skeleton | timeline error + retry | forbidden -> no list | cache by caseId+stageTab | stage tab persisted |
| UI-014 | Revision detail panel | selected revision detail | `/review-cases/{caseId}/submissions/{submissionId}` | GET | select timeline row | caseId+submissionId | submission detail | `RevisionDetailSummaryViewModel` | panel loader | panel error | metadata-only -> masked summary | cache per submissionId | compare mode may require 2 fetches |
| UI-011 | Assign reviewer | manager command | `/review-cases/{caseId}/assign-reviewer` | POST | user click submit assign | caseId + reviewerId + dueAt? | updated assignment status | `AssignmentPageViewModel` partial merge | button loading | toast + field error | state blocked -> disable + reason | success -> refetch case detail | command page |
| UI-011 | Reassign reviewer | manager command | `/review-cases/{caseId}/reassign-reviewer` | POST | click reassign submit | caseId + reviewerId + reason? | updated assignment status | `AssignmentPageViewModel` partial merge | button loading | toast + field error | illegal transition blocked | success -> refetch case detail | keep audit reason if required |
| UI-011 | Assign rereviewer | manager command | `/review-cases/{caseId}/assign-rereviewer` | POST | click assign submit | caseId + rereviewerId + dueAt? | updated assignment status | `AssignmentPageViewModel` partial merge | button loading | toast + field error | blocked by status | success -> refetch case detail | only after initial fixed and rereview-assignable (D-031) |
| UI-011 | Reassign rereviewer | manager command | `/review-cases/{caseId}/reassign-rereviewer` | POST | click reassign submit | caseId + rereviewerId + reason? | updated assignment status | `AssignmentPageViewModel` partial merge | button loading | toast + field error | blocked by status/policy | success -> refetch case detail | `REREVIEW_IN_PROGRESS` policy pending |
| UI-012 | Start initial review | enter command flow | `/review-cases/{caseId}/initial-review/start` | POST | first actionable entry | caseId | start ack + maybe draft id | `InitialReviewPageViewModel` mode transition | page action loading | banner + retry | blocked -> readonly mode | success -> fetch latest case/detail | convert to NEW/EDIT mode |
| UI-012 | Save initial draft | draft persist | `/review-cases/{caseId}/initial-review/draft` | POST | save draft click / autosave policy | draft payload from form state | draft snapshot ack | `InitialReviewPageViewModel.draftFormState` | button/auto-save indicator | non-blocking inline errors | role/status blocked disables save | keep local draft sync | autosave cadence pending |
| UI-012 | Submit initial review | push main flow | `/review-cases/{caseId}/submissions/{submissionId}/initial-review/submit` | POST | submit click | submissionId + form final payload | submitted ack + case status | `InitialReviewPageViewModel` + list/detail invalidation | primary button loading | blocking modal + error mapping | terminal/ownership blocked | invalidate case detail + worklist | submit != draft |
| UI-012 | Revise initial review | append-only initial revision | `/review-cases/{caseId}/submissions/{submissionId}/initial-review/revise` | POST | revise submit click | base submissionId + revised payload | new revision ack | `InitialReviewPageViewModel.revisionContext` | primary button loading | explicit revision error map | fixed/CLOSED/CANCELLED/actor mismatch blocked | invalidate revisions + detail + worklist | revision != reopen |
| UI-013 | Start rereview | enter rereview command | `/review-cases/{caseId}/rereview/start` | POST | first actionable entry | caseId | start ack + maybe draft id | `RereviewPageViewModel` mode transition | page action loading | banner + retry | blocked -> readonly mode | success -> fetch latest case/detail | ensure initial fixed; never expose initial content (D-031/033) |
| UI-013 | Save rereview draft | draft persist | `/review-cases/{caseId}/rereview/draft` | POST | save draft click / autosave policy | draft payload | draft snapshot ack | `RereviewPageViewModel.draftFormState` | button/auto-save indicator | inline errors | blocked when mode readonly | local+server reconcile | autosave cadence pending |
| UI-013 | Submit rereview | submit without close (D-032) | `/review-cases/{caseId}/submissions/{submissionId}/rereview/submit` | POST | submit click | submissionId + rereview payload | submitted ack; no automatic close; derived score masked (D-033) | `RereviewPageViewModel` + detail invalidation | primary button loading | blocking modal + error map | status/ownership blocked | invalidate detail/worklist/revisions | submission and formal close are separate |
| UI-013 | Revise rereview | append-only rereview revision | `/review-cases/{caseId}/submissions/{submissionId}/rereview/revise` | POST | revise submit click | base submissionId + revised payload | new revision ack | `RereviewPageViewModel.revisionContext` | primary button loading | explicit revision error map | fixed/CLOSED/CANCELLED denied | invalidate revisions + detail + worklist | D-032 supersedes D-021; no closed revision |

---

## 5) Metadata-only / Forbidden / Blocked 處理規則

```yaml
api_binding_projection_rules:
  metadata_only:
    - keep read APIs allowed within scope
    - mask detail fields in binding mapper (O3-C shared base + page extension)
    - hide command triggers in UI policy layer
  forbidden:
    - map 403 to blocked/forbidden page mode
    - do not render stale privileged payload
  blocked:
    - retain page shell + blocked reason banner
    - prevent command API dispatch
```

---

## 6) Cache / Refetch 策略（Bridge 建議）

- Query cache key 粒度：
  - worklist: by query params
  - case detail: by `caseId`
  - revisions: by `caseId + stageTab`
- Command 成功後 refetch 優先序：
  1. current page essential query
  2. dependent control center/history query
  3. worklist query（若影響列表 action/status）

---

## 7) Cross-file Consistency Check（API Binding 視角）

### 高信心一致
- state/permission/action 對 command enable/disable 的分層一致。
- revision append-only 與 command API 分離原則一致。

### Inconsistency / Pending
1. `GET /review-cases/{caseId}/lifecycle` 欄位 schema 細節仍需下輪固定。

---

## 8) Pending Confirmation

1. lifecycle API 的最小欄位集合與排序規格。

## D-031～D-033 同步契約

以下映射以 Decision Log D-031～D-033、state-machine 與 permission-matrix 為 authority。

現有 submit/revise 路徑保留；初審固定／複審正式結案的 binding unknown，不新增 endpoint。初審固定前不推進複審，複審 submit ack 不代表 CLOSED；正式結案限具資格原複審提交者且同時固定結果（D-031／032）。D-033 適用所有 query／command ack、history／report 及 cache，不得只在 template 隱藏初審或可反推分數。

驗證追溯 EX-041～052／TS-DA-001～012；狀態、儲存與新操作表示未知處不由 frontend 自行定案。
