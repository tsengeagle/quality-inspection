# UI Wireframe Brief v1 — P2 Extension (Cases Cluster)

## 1. 文件目的

本文件是在既有 P1 baseline 已完成的前提下，將相同的 **page-role first / shared-structure first / projection-as-same-page** 方法，延伸到 P2 頁面：

- UI-012 初審作業頁
- UI-011 案件分派頁
- UI-009 案件列表頁

本文件是 **P2 wireframe / low-fidelity prototype extension 的執行補充**，不是重做 P1，也不是重新打開 DD-001 ~ DD-008。

---

## 2. Source Inputs

| 類型 | 路徑 | 採用內容 |
|---|---|---|
| P1 visual baseline | `docs/07-design/ui-visual-baseline.md` | hierarchy、projection、navigate-only vs command、semantic tone |
| P1 design decisions | `docs/07-design/ui-design-decisions.md` | DD-001 ~ DD-008 已批准結論 |
| Base wireframe brief | `docs/07-design/ui-wireframe-brief.md` | P1/P2 page sequence、S1~S4 shared structure、per-page must-show |
| P1 wireframes | `docs/07-design/wireframes/ui010-case-detail-first-pass.md` | control center baseline |
| P1 wireframes | `docs/07-design/wireframes/ui013-rereview-first-pass.md` | A4 command page baseline、C16 expression |
| P1 wireframes | `docs/07-design/wireframes/ui014-revision-history-first-pass.md` | history page baseline、projection downgrade |
| Layout archetypes | `docs/05-ui/ui-layout-archetypes.md` | A1 / A3 / A4 定義 |
| Component map | `docs/06-frontend/ui-component-map.md` | C01~C17、C23 ownership 與 reuse 邊界 |
| View model bridge | `docs/06-frontend/ui-view-model-spec.md` | UI-009/UI-011/UI-012 的 page-level VM |
| State / action matrix | `docs/06-frontend/ui-state-actions-matrix.md` | mode resolution、blocked reasons、projection policy |
| Test scenarios | `docs/06-frontend/frontend-test-scenarios.md` | route guard、mode、projection、query preset 情境 |
| Route / module map | `docs/06-frontend/ui-route-module-map.md` | `/cases`、`/assign`、`/initial-review` 的 route 邊界 |
| UI authority | `docs/05-ui/ui-page-spec-ui009案件列表頁.md` | list / work queue truth |
| UI authority | `docs/05-ui/ui-page-spec-ui011案件分派頁.md` | assignment page truth |
| UI authority | `docs/05-ui/ui-page-spec-ui012初審作業頁.md` | initial review command page truth |

---

## 3. Scope

### 本輪覆蓋

- P2 page typing 與 archetype 確認
- P2 shared structure reuse / extension
- UI-012 / UI-011 / UI-009 的 first-pass wireframe
- 與 P1 相同 review harness pattern 的 low-fidelity prototype extension

### 本輪不覆蓋

- 不重開 P1 visual baseline
- 不重開 DD-001 ~ DD-008
- 不改 01–06 authority truth
- 不進 Angular implementation
- 不新增 repo 未出現的新 page archetype 或新 high-risk target

---

## 4. P2 Page Role / Archetype Decision

| Page | Page Role | Archetype | 判定理由 |
|---|---|---|---|
| UI-012 | reviewer command page | A4 Action / Review Form | 與 UI-013 同屬 review form，但不承接 CLOSED exception，聚焦 initial review draft / submit / revision |
| UI-011 | manager assignment command page | A3 Action / Assignment | 任務是 assign / reassign orchestration，不是 control center，也不是 generic form editor |
| UI-009 | role-aware work queue | A1 List / Work Queue | 任務是 query preset + filter + row-level entry，不得退化成 page-level command page |

> P2 順序沿用 `ui-wireframe-brief.md` 已凍結的 P2 priority，未發現需要調整順序的 repo evidence。

---

## 5. Shared Structure Reuse / Extension

### 5.1 Direct Reuse

| Shared structure | P2 reuse |
|---|---|
| C01 CaseSummaryHeader | UI-009 / UI-011 / UI-012 全沿用；資訊密度依 A1/A3/A4 調整 |
| C02 StatusOrBlockedBanner | UI-011 / UI-012 沿用；UI-009 僅在 forbidden / empty / error 場景用 list-level banner |
| C03 ModeIndicatorStrip | UI-011 / UI-012 沿用；UI-009 用於 role + query preset mode 提示 |
| C04 ProjectionNoticeBanner | UI-009 metadata-only 沿用 projection 語意；延續 P1 作法，以 header-level inline indicator 為優先 |
| C15 ReviewFormSummaryStrip | UI-012 直接沿用 UI-013 的 A4 shared structure |
| C16 ReviewCommandBar | UI-012 直接沿用 UI-013 的 A4 command bar grammar，但移除 CLOSED exception branch |
| C17 ReviewVersionSummaryRegion | UI-012 直接沿用 A4 版本摘要邏輯 |

### 5.2 P2-Specific Extension

| Structure | 類型 | 說明 |
|---|---|---|
| C05 / C06 / C07 | UI-009 feature-scoped | work queue 的 filter / table / row action set，沿用 P1 navigate-only grammar，不新增 page-level command bar |
| C12 / C13 / C14 | UI-011 feature-scoped | assignment mode 與 reviewer/rereviewer 分離，避免混成單一 generic assignment form |

### 5.3 Deferred

| Structure | 狀態 | 理由 |
|---|---|---|
| C23 AssignmentDueDateHelperRegion | deferred placeholder | component-map 與 brief 已標記 deferred，不在本輪定稿 |
| UI-009 bulk action / export | placeholder only | brief 已標記 optional / placeholder |
| UI-012 附件深層互動 | placeholder only | page spec 有 interaction need，但不需在 low-fidelity prototype 定稿 |

---

## 6. Per-Page P2 Execution Notes

## UI-012 初審作業頁

- page role: reviewer command page
- must-show regions: C01 / C02 / C03 / C15 / main form / commentary+attachment / C16 / C17
- must-visualize modes: `NEW_INITIAL_REVIEW` / `EDIT_DRAFT` / `REVISION` / `READONLY_BLOCKED`
- projection rule: 無 metadata-only 主情境
- blocked rule: 非本人、CLOSED、CANCELLED、非法 stage 時，主表單與 C16 必須進 blocked
- exception rule: 不承接 CLOSED revision-exception；此語意保留在 UI-013
- inherited high-risk: C16

## UI-011 案件分派頁

- page role: manager assignment command page
- must-show regions: C01 / C02 / C03 / C12 / C13 / C14 / page-level submit-back action zone
- must-visualize modes: `ASSIGN_INITIAL` / `REASSIGN_INITIAL` / `MIXED_ASSIGNMENT` / `READONLY_BLOCKED`
- projection rule: 無 metadata-only command 視角
- blocked rule: 非 manager、scope mismatch、CLOSED、CANCELLED 時 blocked
- exception rule: 無 revision exception；assignment page 不承接 O1-C
- review focus: reviewer/rereviewer section 必須語意分離；不是新的 high-risk target

## UI-009 案件列表頁

- page role: role-aware work queue
- must-show regions: C01 / C04(metadata-only) / C05 / C06 / C07 / paging
- must-visualize modes: manager default、reviewer default、metadata-only、forbidden/empty、row action overflow
- projection rule: metadata-only 是同一頁 projection，不拆 metadata page
- blocked rule: row action 依 role/status 呈現 hidden 或 disabled + reason
- exception rule: 不在 UI-009 直接發起 exception command，只能 row-level 導頁
- review focus: action overflow 與 row-level navigate-only 邊界；不是新的 high-risk target

---

## 7. Prototype Extension Handoff Notes

若延伸 `prototype-react`，應維持與 P1 相同 pattern：

1. route 結構沿用 `/cases`, `/cases/:caseId/assign`, `/cases/:caseId/initial-review`
2. variant 切換沿用 query param `?variant=`
3. review harness 仍由首頁 scenario registry 進入，不要求 reviewer 手打 URL
4. UI-009 / UI-011 action zones 保持 navigate-only vs command 分工，不把 P2 prototype 做成正式產品 shell
5. UI-012 command bar 沿用 P1 `CommandBar` 心智模型，但不把 CLOSED exception 分支帶進來

---

## 8. High-Risk / Deferred Summary

### 繼承的高風險

- C16 ReviewCommandBar（UI-012）

### 本輪不新增高風險 target

- UI-011 assignment mode 對照
- UI-009 row action overflow

以上兩者列為 review focus，不升級為新的 repo-level high-risk target。

### Deferred / Placeholder

- C23 AssignmentDueDateHelperRegion
- UI-009 bulk action / export
- UI-012 附件進階互動

---

## 9. Completion Notes

P2 完成條件在本文件中的落點是：

1. 先確定 page role，再畫 wireframe
2. 先標示 reuse / extension / deferred，再補 prototype
3. prototype 僅驗證 role / flow / state / action / projection，不做產品級 polish
4. 不回頭修改 P1 truth
