你現在要接手一個前端設計收斂任務。

# 任務目標

根據現有 UI / frontend 文件，先完成本輪 **foundational design truth** 文件，作為後續 Angular / microfrontend 實作與 design execution 的基線。

本輪只處理以下 5 份文件：

1. frontend-design-method.md
2. state-machine.md
3. permission-matrix.md
4. ui-state-actions-matrix.md
5. frontend-test-scenarios.md

這一輪只做「規格收斂與工程化真相文件」，不要直接開始：
- component implementation
- 畫面切版
- 大量 route code
- API client implementation
- wireframe / mockup
- UI library 抽象

---

# 方法定位

本專案前端方法採用：

UI specification-first / state-driven / domain-aligned / repo-aligned

請依下列原則工作：

1. 先定義頁面責任，再設計畫面與元件
2. 頁面是 use-case boundary，不是 component container
3. UI mode 先於 loading / data / error
4. action visibility 與 executability 必須由統一 policy 推導，不可散落在 component if-else
5. metadata-only 與 FULL view 應以同頁不同 view mode 處理，不複製頁面
6. tab 是 query preset，不是另一個頁面
7. 作業頁是 command page，不是一般資料編輯頁
8. detail page 是 control center，不是欄位全集頁
9. route / module / view-model / API binding 必須對齊 repo 實作骨架
10. 無法由現有 spec 驗證的內容，不得當成確定結論

---

# Authority 與輸入文件

請先完整閱讀 workspace 內相關文件，並遵循以下 authority 順序：

1. docs/01-requirements/decision-log.md
2. docs/02-ddd/permission-matrix.md
3. docs/02-ddd/state-machine.md
4. docs/03-contracts/api-spec.md
5. docs/05-ui/ui-information-architecture.md
6. docs/05-ui/ui-page-inventory.md
7. docs/05-ui/ui-page-spec-*.md
8. docs/06-frontend/frontend-design-roadmap.md

若上述路徑不存在，請在 workspace 中搜尋對應檔名或等價檔案。
若同名文件已存在，應先讀取並整合，而不是另建平行真相。

本輪至少要讀以下文件：

- ui-information-architecture.md
- ui-page-inventory.md
- ui-page-spec-ui006抽選執行頁.md
- ui-page-spec-ui007抽選批次列表頁.md
- ui-page-spec-ui009案件列表頁.md
- ui-page-spec-ui010案件詳情頁.md
- ui-page-spec-ui011案件分派頁.md
- ui-page-spec-ui012初審作業頁.md
- ui-page-spec-ui013複審作業頁.md
- ui-page-spec-ui014revision歷程頁.md
- frontend-design-roadmap.md

若 repo 中已有以下檔案，必須先讀再決定是更新還是補強：
- state-machine.md
- permission-matrix.md
- api-spec.md
- frontend-design-method.md
- frontend-test-scenarios.md

---

# 本輪與 roadmap 的關係

frontend-design-roadmap.md 是 repo-aligned bridge roadmap，不是取代上游真相文件的 authority。

本輪任務是先補齊 foundational design truth。
roadmap 中提到的以下文件屬於下一輪 bridge / design execution，不在本輪建立：
- ui-route-module-map.md
- ui-view-model-spec.md
- ui-api-binding-matrix.md
- ui-layout-archetypes.md
- ui-component-map.md
- ui-visual-baseline.md
- ui-wireframe-brief.md
- ui-design-decisions.md
- ui-implementation-plan.md

但你必須在本輪輸出中，明確讓這 5 份 foundational 文件能自然銜接到 roadmap 的下一輪。

---

# 範圍控制

## 主覆蓋範圍
本輪以 cases 主幹 feature cluster 為主：

- UI-009 案件列表頁
- UI-010 案件詳情頁
- UI-011 案件分派頁
- UI-012 初審作業頁
- UI-013 複審作業頁
- UI-014 Revision 歷程頁

## 補充覆蓋範圍
可對以下頁面做「最小必要覆蓋」，但僅限於目前 spec 已明確支持的 state / permission / action / blocked semantics：

- UI-006 抽選執行頁
- UI-007 抽選批次列表頁

不要在本輪把 sampling 領域擴張成完整 bridge / design execution 任務。

---

# 執行要求

## 一、先分析，再產出
先抽取：
- roles
- page responsibilities
- domain statuses
- page modes
- route guards
- scope rules
- action rules
- metadata-only vs FULL projection rules
- revision semantics
- repo-aligned constraints

先做 cross-file consistency 檢查，再開始寫文件。

## 二、不得虛構
所有結論都必須能回溯到現有 spec。
若某狀態、權限、轉移、操作、角色限制無法由文件明確支持：
- 不得自行補造
- 放入 pending confirmation / assumptions not confirmed
- 明確註記缺口來自哪份文件

## 三、嚴格維持語意
特別注意以下語意不可被弱化：

- revision != edit old data
- revision != reopen
- draft != submit
- submit 會推動主流程
- terminal state 應有 blocked reason
- metadata-only 是 projection，不是另一套頁
- page mode 是業務語意模式，不只是技術狀態
- detail page 是 control center
- review page 是 command page

## 四、若既有文件已存在，優先更新整合
若 repo 已有：
- state-machine.md
- permission-matrix.md
- frontend-design-method.md
- frontend-test-scenarios.md

則優先以最小必要變更方式更新，使其與現有 UI specs 與 roadmap 對齊。
不要建立重複、平行、名稱近似但內容衝突的新文件。

## 五、保留原有用語
盡量沿用現有 spec 名稱，不要任意改名。
例如：
- HOSPITAL_MANAGER
- REVIEWER
- REREVIEWER
- AUDITOR
- SYSTEM_ADMIN
- metadata-only
- FULL
- revision
- initial review
- rereview
- sampling batch

---

# 你要完成的輸出

## 1. frontend-design-method.md
整理成本專案的前端方法文件，內容至少包含：

- 方法目標
- authority chain
- 設計原則
- Layer 0 / Layer 1 / Layer 2 / Layer 3 的文件分層
- page / route / domain / state / action / view model 的關係
- metadata-only / FULL 的 projection 原則
- command page 與 detail page 的差異
- tab 作為 query preset 的原則
- page mode 建模原則
- action policy 建模原則
- repo-aligned 原則
- 從 foundational truth 銜接到 roadmap bridge phase 的方式
- definition of done for frontend design

這份文件必須是團隊工作規格，不是泛用介紹文。

## 2. state-machine.md
根據現有 spec，整理主要 domain state machine。

至少包含：
A. ReviewCase state machine
B. ReviewSubmission lifecycle
C. SamplingBatch lifecycle [僅最小必要覆蓋]

每個 state machine 至少提供：
- state list
- transition table
- trigger / command
- terminal states
- blocked / readonly notes
- role / assignment 關聯
- mermaid state diagram
- pending confirmation

若既有 state-machine.md 已存在，應整合更新，不得平行重建。

## 3. permission-matrix.md
根據現有 spec，整理角色、頁面、資料可見性、操作權限、scope 的矩陣。

至少涵蓋角色：
- HOSPITAL_MANAGER
- REVIEWER
- REREVIEWER
- AUDITOR
- SYSTEM_ADMIN

至少涵蓋頁面：
- UI-006 [最小必要]
- UI-007 [最小必要]
- UI-009
- UI-010
- UI-011
- UI-012
- UI-013
- UI-014

請至少拆成：
- page access matrix
- data visibility matrix
- action permission matrix
- scope / illegal entry handling matrix

若既有 permission-matrix.md 已存在，應整合更新，不得平行重建。

## 4. ui-state-actions-matrix.md
這份文件最重要。

請建立 page-level UI state / actions matrix。
主覆蓋：
- UI-009
- UI-010
- UI-011
- UI-012
- UI-013
- UI-014

補充最小必要：
- UI-006
- UI-007

每頁至少定義：
- page purpose
- inputs affecting UI
- page modes
- mode resolution rules
- role-sensitive behavior
- visible sections
- available actions
- hidden actions
- disabled / blocked actions
- blocked reasons
- next-step hints
- data projection differences
- route guard notes
- pending confirmation

page mode 必須是業務語意模式，不可退化成 generic loading / error / success。

## 5. frontend-test-scenarios.md
請產出可直接給前端測試與 reviewer 使用的測試情境。

至少包含：
A. Route guard scenarios
B. Page mode resolution scenarios
C. Action visibility scenarios
D. Projection scenarios
E. Revision scenarios
F. Query preset scenarios

請使用 Given / When / Then 或等價格式，並標示每個情境適合：
- unit test
- integration test
- e2e test
- QA checklist

cases 主幹為主；sampling 僅最小必要。

---

# 建議工作順序

請依以下順序執行，不要跳步：

1. 讀取全部輸入文件
2. 抽取 domain nouns / roles / statuses / guards / actions / projections / repo constraints
3. 檢查 cross-file consistency
4. 列出 spec gaps / pending confirmation
5. 檢查既有 state-machine / permission-matrix / test-scenarios 是否存在
6. 先完成 state-machine.md 與 permission-matrix.md
7. 再完成 ui-state-actions-matrix.md
8. 再完成 frontend-design-method.md
9. 最後完成 frontend-test-scenarios.md
10. 回報：
   - 讀了哪些 authority inputs
   - 新增或更新了哪些檔案
   - 每份文件摘要
   - 高信心結論
   - 低信心 / pending confirmation
   - 下一輪最自然銜接的 roadmap bridge 文件

---

# 格式要求

- 使用 Markdown
- 結構化標題清楚
- 優先用表格、Mermaid、YAML-like 區塊
- 不要把原 spec 大量重貼
- 要做收斂、對齊、工程化整理
- 每份文件都需包含：
  - source inputs
  - 收斂原則
  - 與 roadmap 的關係
  - pending confirmation
  - 是否為新增或更新

---

# 明確禁止事項

1. 不得直接開始實作 component UI
2. 不得未經證據補造 domain state
3. 不得把 revision 當一般 edit flow
4. 不得把 metadata-only 拆成另一套頁面規格
5. 不得把 action visibility 分散寫成無系統條件
6. 不得略過 pending confirmation
7. 不得把 roadmap 的下一輪 bridge 文件偷做進本輪
8. 不得忽略既有同名文件而另建平行版本

---

# 交付完成條件

只有在以下條件都滿足時，才能宣稱本輪完成：

- 已新增或更新 5 份指定文件
- 每份文件都有實質內容
- 結論能回溯到現有 UI specs 與 roadmap
- 已列出 spec gaps / pending confirmation
- 文件可作為下一輪 route / module / view-model / API binding 文件的基線
- 沒有偷跑進入大量 UI implementation

現在開始執行。
