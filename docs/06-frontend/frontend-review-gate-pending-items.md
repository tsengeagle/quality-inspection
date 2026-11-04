# Frontend Focused Review Gate — Pending Items

> D-031～D-033 現行規格更新：下文 O1-C／D-021／D-027 與 CLOSED 例外修正的推薦、guard、mode、按鈕、驗收及「可實作」表格均已被 D-032 取代，僅保留歷史交接證據，禁止照其建立 CLOSED 修正入口。提交後不結案、未固定可修訂、具資格由原複審提交者正式結案並固定，CLOSED 禁止 revision／reopen。初審固定前不得進複審；複審永久盲審含衍生分數。新規格以 state-machine、permission-matrix、API 與 UI-010～014 更新為準；新操作 binding／狀態表示／初審固定角色 unknown，不構成已可實作入口。應用程式未於本輪更新。


## 1. 文件目的

本文件是 **focused review gate** 決策準備文件，僅針對 bridge phase 的 3 個高影響 pending items 做衝突收斂、選項評估與回寫規劃。  
本文件目的為「供人類拍板」，不是直接定案，也不是 implementation 指南。

---

## 2. Source Inputs

### Foundational truth
- `docs/06-frontend/frontend-design-method.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/06-frontend/ui-state-actions-matrix.md`
- `docs/06-frontend/frontend-test-scenarios.md`

### Bridge files
- `docs/06-frontend/ui-route-module-map.md`
- `docs/06-frontend/ui-view-model-spec.md`
- `docs/06-frontend/ui-api-binding-matrix.md`

### Upstream authority
- `docs/01-requirements/decision-log.md`
- `docs/03-contracts/api-spec.md`
- `docs/05-ui/ui-information-architecture.md`
- `docs/05-ui/ui-page-inventory.md`
- `docs/05-ui/ui-page-spec-ui009案件列表頁.md`
- `docs/05-ui/ui-page-spec-ui010案件詳情頁.md`
- `docs/05-ui/ui-page-spec-ui011案件分派頁.md`
- `docs/05-ui/ui-page-spec-ui012初審作業頁.md`
- `docs/05-ui/ui-page-spec-ui013複審作業頁.md`
- `docs/05-ui/ui-page-spec-ui014revision歷程頁.md`
- `docs/06-frontend/frontend-design-roadmap.md`

---

## 3. Review Scope

本輪僅 review 以下 3 項：
1. CLOSED 後 rereview revise UI 入口策略
2. submission-rooted vs case-rooted command path
3. metadata-only 欄位白名單

明確不處理：其他 pending confirmation、layout/component/wireframe/implementation。

---

## 4. 議題逐項分析

## 4.1 議題一：CLOSED 後 rereview revise UI 入口策略

- **topic**: CLOSED case 是否應提供 rereview revision 入口（以及入口放在哪個頁面/模式）
- **why_this_matters**:
  - 直接影響 UI-013 page mode、UI-014 action entry、blocked reason 一致性
  - 直接影響 revision append-only 與 effective version timeline 的認知正確性

### current_conflict
- **source_a（明確 authority）**
  - decision-log D-001 / D-021：CLOSED 後允許 **複審 revision**（初審不允許）
  - state-machine / permission-matrix / ui-state-actions-matrix / frontend-test-scenarios 也已承接此例外
- **source_b（明確規格但與 A 衝突）**
  - UI-013：CLOSED 時 `READONLY_BLOCKED` 並寫明不可 revision
  - UI-014：CLOSED 時「僅供查閱」，不顯示導向 revision 作業頁按鈕
- **conflict_summary**
  - 上游決策允許例外；兩份 page spec 文案仍採「CLOSED 一律禁止修正」

### options

| option_id | option_name | description | pros | cons | authority_alignment |
|---|---|---|---|---|---|
| O1-A | Strict Closed Readonly | CLOSED 一律不提供 rereview revise 入口 | UI 文案直覺、實作簡單 | 與 D-021 直接衝突；破壞已接受決策 | 低（衝突） |
| O1-B | Exception Entry in UI-013 only | UI-013 在 CLOSED + 原 rereviewer +有 base submission 時開啟 `REVISION_EXCEPTION` 模式；UI-014維持 history-only | 入口集中在 command page；維持 command 語意 | UI-014「可修正發現點」回流成本高 | 中高 |
| O1-C | Exception Entry in UI-014 + UI-013 | UI-014 顯示「前往複審修正」條件入口，導去 UI-013 的 `REVISION_EXCEPTION`；UI-013不開新提交，只開 revise | 與決策一致且 user flow 完整；history→command 最順 | 規格與測試回寫面較大 | 高（推薦） |

- **recommended_option**: `O1-C`
- **recommendation_reason**:
  1. 符合 D-021（允許 CLOSED 後複審 revision）
  2. 不違反「review page 是 command page」：真正提交仍在 UI-013
  3. UI-014 保持 history page 定位，只提供條件式導頁，不直接提交
- **impact_area**:
  - page mode: UI-013 新增 `REVISION_EXCEPTION_CLOSED`（或等價語意）
  - blocked state: CLOSED 不再一律 blocked，改為「新提交 blocked、例外修正條件開放」
  - timeline/effective version: revise 後 current effective 需更新標示
- **required_backwrite_targets**:
  - must: `ui-page-spec-ui013複審作業頁.md`, `ui-page-spec-ui014revision歷程頁.md`, `ui-state-actions-matrix.md`, `frontend-test-scenarios.md`
  - should: `state-machine.md`, `permission-matrix.md`, `ui-view-model-spec.md`, `ui-api-binding-matrix.md`
  - optional follow-up: `ui-layout-archetypes.md`, `ui-component-map.md`, `ui-wireframe-brief.md`
- **test_impact**:
  - 新增 CLOSED exception route guard / mode resolution / action visibility / revision flow e2e
- **unresolved_risks**:
  - 若 exception 條件定義不完整，可能誤開放非原 rereviewer
- **human_decision_needed**:
  - 是否採 `O1-C`，以及 UI-014 是否顯示按鈕或僅顯示可點文字入口

---

## 4.2 議題二：submission-rooted vs case-rooted command path

- **topic**: submit/revise command 的 API path 真相採 submission-rooted 還是 case-rooted
- **why_this_matters**:
  - 影響 service facade 命名、command orchestration、error mapping、測試 fixture 與 API binding matrix

### current_conflict
- **source_a（明確 authority）**
  - `api-spec.md` 目前明確寫為 case-rooted：`/review-cases/{caseId}/submissions/{submissionId}/...`
- **source_b（明確規格/roadmap與A不同）**
  - UI page specs、roadmap、bridge 分析中常使用 submission-rooted：`/review-submissions/{submissionId}/...`
- **conflict_summary**
  - 合約層與 UI/roadmap 參考路徑存在雙軌，不利實作一致性

### options

| option_id | option_name | description | pros | cons | authority_alignment |
|---|---|---|---|---|---|
| O2-A | Case-rooted only | 全面以 api-spec 路徑為唯一真相 | 與現行 API authority一致；追蹤 case context 清楚 | 需回寫 UI specs/roadmap/bridge 文案 | 高（以 api-spec 為準） |
| O2-B | Submission-rooted only | 全面改為 `/review-submissions/{submissionId}` | command 目標資源明確 | 需大幅改 api-spec 與既有 case-rooted規格 | 中低 |
| O2-C | Dual path compatibility | 保留雙路徑（一主一別名） | 過渡成本低 | 長期增加實作與測試複雜度，易漂移 | 中 |

- **recommended_option**: `O2-A`
- **recommendation_reason**:
  1. 不改動既有合約 authority 可降低 integration 風險
  2. UI 應以 command semantic 建模；path 作 transport detail，不應反向主導 UI 模型
  3. 可在 bridge 層以 command key 抽象，避免 path 變更擴散
- **impact_area**:
  - `ui-api-binding-matrix.md` 路徑欄位定錨
  - `ui-view-model-spec.md` 中 API source 註記統一
  - `frontend-test-scenarios.md` API mocking route keys
- **required_backwrite_targets**:
  - must: `api-spec.md`（確認唯一真相）、`ui-page-spec-ui012初審作業頁.md`, `ui-page-spec-ui013複審作業頁.md`
  - should: `frontend-design-roadmap.md`, `ui-api-binding-matrix.md`, `ui-view-model-spec.md`, `frontend-test-scenarios.md`
  - optional follow-up: `ui-layout-archetypes.md`, `ui-component-map.md`, `ui-wireframe-brief.md`
- **test_impact**:
  - contract/integration test 路徑斷言需單一路徑化
- **unresolved_risks**:
  - 若後端實作已部分採 submission-rooted，需 migration/compat plan
- **human_decision_needed**:
  - 是否接受 `O2-A` 並要求文件全面單一路徑回寫

---

## 4.3 議題三：metadata-only 欄位白名單

- **topic**: SYSTEM_ADMIN / metadata-only projection 的可見欄位白名單策略
- **why_this_matters**:
  - 影響 list/detail/history 三頁一致性、view model projection 安全性、後續 layout 元件可重用程度

### current_conflict
- **source_a（明確 authority）**
  - decision-log D-010、permission-matrix、api-spec：SYSTEM_ADMIN 僅 metadata，不得 full answers/comments/attachments
- **source_b（未定稿缺口）**
  - 白名單尚未形成統一結構（global/per-page/shared+extension 未定）
  - ui-view-model-spec / ui-api-binding-matrix 目前僅先留 pending
- **conflict_summary**
  - 原則清楚，但欄位級 contract 未定，導致投影 mapping 與前端 section visibility 不可穩定落地

### options

| option_id | option_name | description | pros | cons | authority_alignment |
|---|---|---|---|---|---|
| O3-A | Global whitelist only | 全頁共用單一 metadata 欄位集合 | 簡單、易控 | 無法滿足 detail/history 差異化需求 | 中 |
| O3-B | Per-page whitelist only | 每頁各自定白名單 | 彈性高 | 重複多、易飄移、治理成本高 | 中 |
| O3-C | Shared base + page extension | 定義 shared base metadata 欄位，再按 UI-009/010/014 擴充 | 一致性+彈性平衡；最利於 projection mapper | 需要額外治理規則 | 高（推薦） |

- **recommended_option**: `O3-C`
- **recommendation_reason**:
  1. 符合「metadata-only 是 projection，不是另一套頁」
  2. 可直接映射到 view-model shared sub-model + page extension
  3. 對後續 layout/component 的 metadata banner/section visibility 最穩定
- **impact_area**:
  - list/detail/history 的欄位與 section visibility
  - `MetadataOnlyCaseProjectionViewModel` 的字段結構
  - API binding mapper 的 masking 規則
- **required_backwrite_targets**:
  - must: `api-spec.md`（metadata schema分層）、`permission-matrix.md`
  - should: `ui-view-model-spec.md`, `ui-api-binding-matrix.md`, `ui-state-actions-matrix.md`, `frontend-test-scenarios.md`
  - optional follow-up: `ui-layout-archetypes.md`, `ui-component-map.md`, `ui-wireframe-brief.md`
- **test_impact**:
  - 增加 projection snapshot tests（list/detail/history）與 forbidden field assertions
- **unresolved_risks**:
  - 若 shared base 設太大，可能洩漏過多業務訊息；設太小則影響稽核可讀性
- **human_decision_needed**:
  - shared base 欄位集合與各 page extension 範圍

---

## 5. Cross-Issue Dependencies

| 依賴關係 | 說明 |
|---|---|
| Issue-2 → Issue-1 | command path 真相會影響 CLOSED exception 入口的導頁與 command 觸發配置 |
| Issue-3 → Issue-1 | metadata-only 白名單決定 UI-014 在 exception 場景可呈現的提示與欄位 |
| Issue-2 ↔ Issue-3 | API path 與 response schema 分層共同決定 projection mapper 實作邊界 |

先決建議：**先決 Issue-2，再決 Issue-3，最後決 Issue-1 UI 入口細節**。

---

## 6. Proposed Decision Order

1. **Issue-2**（command path 單一路徑真相）
2. **Issue-3**（metadata-only whitelist 結構）
3. **Issue-1**（CLOSED rereview revise 入口最終 UX 模式）

原因：先固定 transport/contract，再固定 projection schema，最後定 UI 入口最小歧義。

---

## 7. Backwrite Plan

### must update
- `docs/03-contracts/api-spec.md`
- `docs/05-ui/ui-page-spec-ui012初審作業頁.md`
- `docs/05-ui/ui-page-spec-ui013複審作業頁.md`
- `docs/05-ui/ui-page-spec-ui014revision歷程頁.md`

### should update
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/06-frontend/ui-state-actions-matrix.md`
- `docs/06-frontend/frontend-test-scenarios.md`
- `docs/06-frontend/ui-view-model-spec.md`
- `docs/06-frontend/ui-api-binding-matrix.md`
- `docs/06-frontend/frontend-design-roadmap.md`

### optional follow-up
- `docs/07-design/ui-layout-archetypes.md`（若建立）
- `docs/07-design/ui-component-map.md`（若建立）
- `docs/07-design/ui-wireframe-brief.md`（若建立）

---

## 8. Ready-to-Approve Summary

| topic | recommendation | confidence | human decision needed | files to update after approval |
|---|---|---|---|---|
| CLOSED 後 rereview revise 入口 | 採 O1-C：UI-014 條件導頁 + UI-013 exception command mode | Medium | 入口呈現型態（按鈕/文字）、exception 條件 | UI-013/UI-014 page specs, ui-state-actions-matrix, frontend-test-scenarios, ui-view-model-spec, ui-api-binding-matrix |
| command path 真相 | 採 O2-A：case-rooted only | Medium-High | 是否允許過渡期 dual path | api-spec, UI-012/UI-013 page specs, roadmap, ui-api-binding-matrix, ui-view-model-spec, test-scenarios |
| metadata-only 白名單 | 採 O3-C：shared base + page extension | Medium | shared base 欄位集合與 page extension 邊界 | api-spec, permission-matrix, ui-view-model-spec, ui-api-binding-matrix, ui-state-actions-matrix, test-scenarios |

---

## 附註

- 本文件僅提供 recommendation，不代表已裁決。
- 本輪未修改 foundational 或 bridge 其他文件；僅新增本 review gate 文件。

