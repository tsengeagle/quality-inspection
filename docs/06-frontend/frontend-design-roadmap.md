# Frontend Design Roadmap v1

## 1. 文件目的

本文件用於把品質審查系統目前已完成的 UI authority 與 frontend implementation area 現況，收斂成一份可供 coding agent 直接遵循的 **前端設計文件 roadmap**。

本文件回答以下問題：

- 目前哪些 UI / frontend 文件已完成
- 接下來應先補哪些文件
- 每份文件的目的、理由與內容範圍是什麼
- 為什麼這個順序最符合目前 repo 狀態
- coding agent 應如何依序推進，而不是跳步

本文件不是：

- UI page spec 的替代品
- API spec 的替代品
- 前端實作 code task list
- 設計稿本身

本文件的角色是：

> 把目前的 UI specification-first / state-driven / domain-aligned 方法，轉成一條 repo-aligned 的前端設計推進順序。

---

## 2. Authority 與適用範圍

本 roadmap 適用於：

- `frontends/quality-inspection-frontend/**`
- `docs/05-ui/**`
- `docs/06-frontend/**`
- 後續如有設計稿，可延伸至 `docs/07-design/**`

若與既有 authority 衝突，優先順序如下：

1. `docs/01-requirements/decision-log.md`
2. `docs/02-ddd/permission-matrix.md`
3. `docs/02-ddd/state-machine.md`
4. `docs/03-contracts/api-spec.md`
5. `docs/05-ui/ui-information-architecture.md`
6. `docs/05-ui/ui-page-inventory.md`
7. 各頁 `docs/05-ui/ui-page-spec-*.md`
8. 本文件

wireframe / prototype execution policy 補充：

- 一般 wireframe / prototype 任務，可直接執行，不需要先等待使用者批准。
- 免費或本地方案優先；Figma 不是預設方案。
- 若涉及 Figma / Figma MCP、付費能力，或遇到額度 / 權限 / API / MCP / 方案限制，必須先停下來回報。
- 若 Figma 不可行，優先 fallback 到本地 React / HTML low-fidelity prototype。
- markdown wireframe、ASCII、Mermaid、frame spec 都是文件，不得冒充 Figma artifact 或可執行 prototype。

---

## 3. 目前已完成的文件基線

### 3.1 已存在的 UI authority

目前 repo 與工作區已完成以下 UI authority：

- `docs/05-ui/ui-page-inventory.md`
- `docs/05-ui/ui-information-architecture.md`
- `docs/05-ui/ui-page-spec-ui006抽選執行頁.md`
- `docs/05-ui/ui-page-spec-ui007抽選批次列表頁.md`
- `docs/05-ui/ui-page-spec-ui009案件列表頁.md`
- `docs/05-ui/ui-page-spec-ui010案件詳情頁.md`
- `docs/05-ui/ui-page-spec-ui011案件分派頁.md`
- `docs/05-ui/ui-page-spec-ui012初審作業頁.md`
- `docs/05-ui/ui-page-spec-ui013複審作業頁.md`
- `docs/05-ui/ui-page-spec-ui014revision歷程頁.md`

這表示：

- 頁面盤點已完成
- IA 與主導航已完成
- Phase 1 主幹頁面的單頁責任、區塊、模式、API 與測試重點已有基線

### 3.2 已確認的 frontend implementation area 現況

根據 `frontends/quality-inspection-frontend/README.md` 與 path-specific instructions，目前 frontend area 已確認：

- framework: Angular 14
- architecture: microfrontend
- state management baseline: NgRx
- unit test baseline: Karma + Jasmine
- e2e baseline: Playwright
- shell provides login and user context in integrated mode
- standalone mode 為 shell-layer substitute
- dev proxy 為 gateway-layer substitute
- API base path 固定為 `/api/quality-inspection/**`
- shared UI baseline 為 `portal51-lib@0.0.16`
- 工程骨架已存在：
  - `shell/`
  - `core/`
  - `shared/`
  - `features/`
  - `pages/`
  - `services/`
  - `models/`
  - `guards/`
  - `state/`

因此，接下來的文件設計順序，必須優先考慮：

- 如何把 UI authority 接到 Angular / MFE 實作骨架
- 如何讓 coding agent 不跳過 route / feature / model / API binding 等橋接層
- 如何在開始 wireframe 或 code 前，先把跨頁規則一致性補齊

---

## 4. 方法總結：目前採用的前端設計流程

本專案目前的前端設計方法，可概括為：

### UI specification-first
先定義頁面責任、角色、區塊、模式與 API，再進視覺稿與實作。

### state-driven
畫面行為由 domain state、permission 與 workflow mode 驅動，而不是由靜態欄位清單驅動。

### domain-aligned
前端不得自行發明新狀態、新權限真相或新流程，必須對齊 decision log、permission matrix、state machine、API spec 與 UI page specs。

### repo-aligned
UI 文件順序必須能落到目前 repo 已存在的：

- `shell`
- `features`
- `pages`
- `services`
- `models`
- `guards`
- `state`

而不是停留在純設計文件層。

---

## 5. 接下來的文件順序總覽

以下是建議的後續文件順序。**預設不建議跳步。**

| 順序 | 文件 | 類型 | 核心目的 |
|---|---|---|---|
| 1 | `ui-state-actions-matrix.md` | UI authority | 集中角色 × 狀態 × 操作 × 可見性規則 |
| 2 | `ui-route-module-map.md` | frontend bridge | 把 IA / page inventory 映到 Angular route / feature module |
| 3 | `ui-view-model-spec.md` | frontend bridge | 定義前端真正使用的 view models |
| 4 | `ui-api-binding-matrix.md` | frontend bridge | 把頁面區塊綁到 API / loading / error / orchestration |
| 5 | `ui-layout-archetypes.md` | UI authority | 把頁面收斂成少數可重用版型 |
| 6 | `ui-component-map.md` | frontend bridge | 把 page regions 收斂成 reusable components |
| 7 | `ui-visual-baseline.md` 或 `DESIGN.extracted.md` | design baseline | 給 agent 穩定的視覺 / wrapper / density 規則 |
| 8 | `ui-wireframe-brief.md` | design execution | 凍結第一輪設計稿範圍 |
| 9 | `ui-design-decisions.md` | design governance | 記錄設計評審後的收斂決策 |
| 10 | `ui-implementation-plan.md` | implementation plan | 把前述文件轉成實作順序 |

---

## 6. 詳細順序與每份文件說明

---

## 6.1 `ui-state-actions-matrix.md`

### 目的

把目前散落在多份 page spec 內的規則，集中成單一份 **跨頁 UI 狀態與操作矩陣**。

它要成為以下內容的共同真相來源：

- page action visibility
- page mode switching
- metadata-only vs full view
- blocked / forbidden state
- revision 可否進入
- terminal state UI 行為

### 為什麼必須先做

目前核心規則已分散在：

- `UI-009` 案件列表頁
- `UI-010` 案件詳情頁
- `UI-011` 案件分派頁
- `UI-012` 初審作業頁
- `UI-013` 複審作業頁
- `UI-014` Revision 歷程頁

若 coding agent 直接開始做 wireframe 或 code，最容易出現的問題是：

- 不同頁對同一狀態給出不同按鈕
- terminal state 顯示不一致
- manager / reviewer / rereviewer / auditor / system admin 視角漂移
- metadata-only 視角在某些頁漏掉

### 至少要寫什麼

每列建議包含：

- `pageId`
- `role`
- `domainStatus`
- `entryAllowed`
- `viewMode`
- `visibleActions`
- `hiddenActions`
- `disabledActions`
- `bannerOrHint`
- `notes`

### 第一輪覆蓋範圍

先只做 cases 主幹：

- `UI-009`
- `UI-010`
- `UI-011`
- `UI-012`
- `UI-013`
- `UI-014`

### 完成後才能更穩地做什麼

- route guards
- page action 區
- metadata-only / blocked 畫面
- wireframe 狀態變體

---

## 6.2 `ui-route-module-map.md`

### 目的

把目前的 UI IA / page inventory，正式映射到 repo 已存在的 Angular / MFE 骨架。

### 為什麼第二步做

repo 已有明確 frontend 分層：

- `shell/`
- `features/`
- `pages/`
- `services/`
- `models/`
- `guards/`
- `state/`

而 README 也已明確指出 frontend 初始順序應先固定：

- module skeleton
- route / page / feature boundary
- shared layout / navigation
- API client / error handling skeleton

所以現在最該補的是：

> 哪些頁該落在哪個 feature module、route group 與 guard 邊界。

### 至少要寫什麼

- route group
- feature module 名稱
- page ownership
- lazy-load 邊界
- shell navigation entry
- guard 類型
- query param / filter persistence 原則

### 第一輪建議

優先定：

- `/cases` 群組
- `/cases/:caseId`
- `/cases/:caseId/assign`
- `/cases/:caseId/initial-review`
- `/cases/:caseId/rereview`
- `/cases/:caseId/revisions`

### 完成後才能更穩地做什麼

- Angular route modules
- feature folder skeleton
- permission-aware navigation
- shell menu 與 route guard 實作

---

## 6.3 `ui-view-model-spec.md`

### 目的

把 UI 頁面真正需要的資料形狀定義成前端 view model，避免頁面直接綁 raw API。

### 為什麼第三步做

repo 已把：

- `models/` 視為 frontend view models / DTO types 落點
- `services/` 視為 request / response mapping 與 page orchestration 落點

page specs 雖已有 response 草稿，但還沒有正式形成跨頁共享的前端模型。

### 至少要寫什麼

第一輪建議至少定：

- `CaseListRowViewModel`
- `CaseDetailViewModel`
- `AssignmentPageViewModel`
- `InitialReviewFormViewModel`
- `RereviewFormViewModel`
- `RevisionTimelineItemViewModel`
- `MetadataOnlyCaseViewModel`

每個 model 至少寫：

- 用在哪些頁
- 來源 API
- 欄位清單
- 哪些欄位是 UI 聚合結果
- 哪些欄位只在 `FULL` 出現
- 哪些欄位在 `METADATA_ONLY` 下保留摘要

### 完成後才能更穩地做什麼

- `models/` 定義
- API mapper
- page facade
- selectors / state slices

---

## 6.4 `ui-api-binding-matrix.md`

### 目的

把頁面區塊與 API 對應、trigger timing、loading / error handling 一次收斂。

### 為什麼第四步做

當 route/module 與 view model 定住後，下一步自然就是把：

- 哪頁的哪個 section 用哪個 API
- 進頁就打還是 action 觸發才打
- 成功 / 失敗 / loading 要怎麼對應 UI

集中起來。

這份文件是 page spec 與 `services/` 的直接橋梁。

### 至少要寫什麼

每列建議包含：

- `pageId`
- `section`
- `api`
- `method`
- `triggerTiming`
- `requestBodyModel`
- `responseViewModel`
- `loadingState`
- `errorState`
- `cacheOrRefetchPolicy`
- `notes`

### 第一輪建議覆蓋

- `GET /review-cases`
- `GET /review-cases/{caseId}`
- `GET /review-cases/{caseId}/revisions`
- `POST /review-cases/{caseId}/assign-reviewer`
- `POST /review-cases/{caseId}/reassign-reviewer`
- `POST /review-cases/{caseId}/assign-rereviewer`
- `POST /review-cases/{caseId}/reassign-rereviewer`
- `POST /review-cases/{caseId}/initial-review/start`
- `POST /review-cases/{caseId}/initial-review/draft`
- `POST /review-cases/{caseId}/submissions/{submissionId}/initial-review/submit`
- `POST /review-cases/{caseId}/submissions/{submissionId}/initial-review/revise`
- `POST /review-cases/{caseId}/rereview/start`
- `POST /review-cases/{caseId}/rereview/draft`
- `POST /review-cases/{caseId}/submissions/{submissionId}/rereview/submit`
- `POST /review-cases/{caseId}/submissions/{submissionId}/rereview/revise`

### 完成後才能更穩地做什麼

- `services/` 實作
- page orchestration
- NgRx effects / facades（若採用）
- loading / error state 一致化

---

## 6.5 `ui-layout-archetypes.md`

### 目的

把目前的核心頁面，收斂成少數幾種可重用 layout archetype。

### 為什麼在這一步做

前面 1 ~ 4 步先把：

- 狀態規則
- route / feature 邊界
- view models
- API 綁定

定住，之後再抽 layout archetype 才會是可落地的，而不是空泛的設計討論。

### 至少要寫什麼

建議至少定以下 archetype：

- `List / Work Queue`
- `Detail / Control Center`
- `Action / Assignment`
- `Action / Review Form`
- `History / Traceability`

每個 archetype 寫：

- 適用頁面
- 主要 layout regions
- action bar 位置
- summary card pattern
- filter / table / form / timeline pattern
- banner / mode indicator 使用方式
- metadata-only 呈現原則

### 完成後才能更穩地做什麼

- shared layout primitives
- page composition skeleton
- reusable section 設計

---

## 6.6 `ui-component-map.md`

### 目的

把 page regions 進一步收斂成 reusable component 與 page-only component 邊界。

### 為什麼這一步在 archetype 之後

因為先看到：

- 哪些頁型有共同 layout
- 哪些 view models 可重用
- 哪些區塊真的跨頁出現

之後，才比較知道哪些 component 值得抽。

### 至少要寫什麼

每個 component 至少寫：

- `componentName`
- `usedByPages`
- `responsibility`
- `inputs`
- `outputs`
- `reusabilityLevel`
- `ownedBy`

### 第一輪可先抽的元件

- `CaseSummaryHeader`
- `CaseStatusActionPanel`
- `TraceabilityCard`
- `SubmissionSummaryCard`
- `AssignmentFormSection`
- `ReviewFormSection`
- `RevisionTimeline`
- `BlockedStateBanner`
- `MetadataOnlyBanner`

### 完成後才能更穩地做什麼

- `shared/` 與 `features/*/components` 邊界
- 容器 / 展示元件切分
- page skeleton 實作

---

## 6.7 `ui-visual-baseline.md` 或 `DESIGN.extracted.md`

### 目的

給 coding agent 一份穩定的視覺 / wrapper / density 基線，避免每頁畫出不同風格。

### 為什麼現在才做

repo 已確認 `portal51-lib@0.0.16` 是 shared UI baseline，但 component-level wrapper pattern 與 design token 機制仍未完全確認。

因此這份文件應在 component map 有初步結果後再做，才能更貼近「實際會用哪些 wrapper / section / card」，而不是流於抽象視覺風格。

### 至少要寫什麼

- page header pattern
- filter bar pattern
- summary card pattern
- form section pattern
- status badge / banner hierarchy
- spacing / density
- portal51-lib wrapper 優先原則
- pending confirmation 清單

### 完成後才能更穩地做什麼

- wireframe / mockup 生成
- shared component 外觀一致化
- agent 視覺輸出穩定化

---

## 6.8 `ui-wireframe-brief.md`

### 目的

正式凍結第一輪設計稿範圍，讓 coding agent 知道：

- 先畫哪些頁
- 每頁要畫哪些狀態變體
- 不要一次畫全系統

### 為什麼這時候做

只有在：

- state matrix
- route/module map
- view model
- API binding
- archetype
- component map
- visual baseline

都已有初步基線後，wireframe 才不會漂。

### 至少要寫什麼

- 第一輪頁面範圍
- 每頁要畫的 state variants
- 角色視角
- desktop / responsive 範圍
- 低保真 / 中保真要求

### 第一輪建議頁面

優先 cases 主幹：

- `UI-009`
- `UI-010`
- `UI-011`
- `UI-012`
- `UI-013`

第二輪再補：

- `UI-006`
- `UI-007`
- `UI-014`

---

## 6.9 `ui-design-decisions.md`

### 目的

把 wireframe / mockup 評審後的收斂結果固定下來，避免後續只停留在口頭決策。

### 為什麼不能省

本專案後端已有 decision log 習慣。若前端沒有對應的 UI 決策文件，後續最容易漂移的就是：

- drawer 還是獨立頁
- revision 是否做 sub-tab 還是獨立頁
- metadata-only banner 的固定位置
- `UI-011` 是否保留獨立頁
- `UI-014` 是否保留 timeline + detail panel

### 至少要寫什麼

- decision id
- decision title
- decision content
- rationale
- impacted pages
- impacted components
- 是否需回寫 page spec / state matrix / component map

### 完成後才能更穩地做什麼

- 視覺稿回寫規格
- 前端實作不再反覆推翻 layout 決策

---

## 6.10 `ui-implementation-plan.md`

### 目的

把前述文件成果，轉成 repo 內真正的 frontend 實作順序。

### 為什麼放最後

這份文件不應在前面 bridge 文件未完成前就寫死。它應該建立在：

- state matrix 已完成
- module map 已完成
- models / API binding 已完成
- component map 已完成
- wireframe 已初步評審

之後，再把它們轉成：

- 建目錄順序
- 建 route module 順序
- 建 page skeleton 順序
- 建 services / models / guards 順序
- 補 unit/e2e 測試順序

### 至少要寫什麼

- 實作 phase 切分
- feature/module 建立順序
- shared component 建立順序
- mock data / fake API plan
- 測試骨架順序
- PR 切分建議

### 第一版建議實作順序

1. `features/cases` route + shell entry
2. `UI-009` 案件列表頁 skeleton
3. `UI-010` 案件詳情頁 skeleton
4. `UI-011` 案件分派頁
5. `UI-012` 初審作業頁
6. `UI-013` 複審作業頁
7. `UI-014` Revision 歷程頁
8. permission-aware UI / guards
9. tests
10. polish

---

## 7. 建議的目錄放置

### 7.1 `docs/05-ui/`
保留 **UI authority** 類文件：

- `ui-page-inventory.md`
- `ui-information-architecture.md`
- `ui-page-spec-*.md`
- `ui-state-actions-matrix.md`
- `ui-layout-archetypes.md`

### 7.2 `docs/06-frontend/`
放 **frontend implementation bridge** 類文件：

- `frontend-design-roadmap.md` [本文件]
- `ui-route-module-map.md`
- `ui-view-model-spec.md`
- `ui-api-binding-matrix.md`
- `ui-component-map.md`
- `ui-implementation-plan.md`

### 7.3 `docs/07-design/`
放 **design execution** 類文件：

- `ui-wireframe-brief.md`
- `ui-design-decisions.md`
- `ui-visual-baseline.md`
- 或 `DESIGN.extracted.md`

---

## 8. 第一輪範圍控制建議

為避免文件一次鋪太廣，第一輪請只圍繞 **cases 主幹 feature cluster**。

### 第一輪只覆蓋以下頁面

- `UI-009 案件列表頁`
- `UI-010 案件詳情頁`
- `UI-011 案件分派頁`
- `UI-012 初審作業頁`
- `UI-013 複審作業頁`
- `UI-014 Revision 歷程頁`

### 第一輪先不要擴張到

- `UI-001 ~ UI-005` 專案與規則管理深化
- `UI-006 ~ UI-008` 抽選與批次深化
- `UI-015 ~ UI-017` dashboard / audit / reports 深化

理由：

- cases 主幹最直接對應目前 frontend implementation area guide 的實作順序
- 先把 cases feature cluster 做穩，後續再複製同一方法到 sampling / projects / reports 會更有效率

---

## 9. Coding Agent 執行規則

### 9.1 不要跳步

若尚未完成：

- `ui-state-actions-matrix.md`
- `ui-route-module-map.md`
- `ui-view-model-spec.md`
- `ui-api-binding-matrix.md`

則不應直接進入：

- 大量 page code 實作
- 高保真 wireframe
- 元件庫大規模抽象

### 9.2 文件產出順序

預設順序：

```text
1. ui-state-actions-matrix.md
2. ui-route-module-map.md
3. ui-view-model-spec.md
4. ui-api-binding-matrix.md
5. ui-layout-archetypes.md
6. ui-component-map.md
7. ui-visual-baseline.md / DESIGN.extracted.md
8. ui-wireframe-brief.md
9. ui-design-decisions.md
10. ui-implementation-plan.md
```

### 9.3 產出原則

- 只根據目前 repo authority 與既有 UI specs 產出
- 不得虛構 portal51-lib component 細節
- 不得在前端自創 domain rules
- 若遇到 page spec 與 state machine / permission matrix 衝突，先回 authority source 對齊

---

## 10. 一句話總結

本專案接下來的前端設計順序，不應再停留在 discovery，而應改為：

> 先集中跨頁狀態規則，再把 UI authority 接到 Angular / MFE repo 的 route、model、service、component 骨架，最後才進設計稿與實作計畫。

這條順序的核心，是讓 coding agent 在開始畫圖或寫 code 前，先擁有足夠穩定的：

- state truth
- module boundary
- view models
- API binding
- layout archetypes
- component map

如此，後續實作才不會漂移。
