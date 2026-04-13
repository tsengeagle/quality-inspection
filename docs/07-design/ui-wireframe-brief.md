# UI Wireframe Brief v1 (Cases Cluster, First-Pass)

## 1. 文件目的

本文件用於凍結 cases 主幹（UI-009~UI-014）第一輪 wireframe 的 **scope / variants / shared structures / placeholder 邊界**，供設計執行者直接產出第一輪 wireframe。

本文件是 scope freezing，不是 wireframe 畫稿本身；不包含 visual baseline、實作規劃與程式碼細節。

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
- `docs/06-frontend/ui-component-map.md`

### Review / backwrite
- `docs/06-frontend/frontend-review-gate-pending-items.md`

### UI authority
- `docs/05-ui/ui-information-architecture.md`
- `docs/05-ui/ui-page-inventory.md`
- `docs/05-ui/ui-page-spec-ui009案件列表頁.md`
- `docs/05-ui/ui-page-spec-ui010案件詳情頁.md`
- `docs/05-ui/ui-page-spec-ui011案件分派頁.md`
- `docs/05-ui/ui-page-spec-ui012初審作業頁.md`
- `docs/05-ui/ui-page-spec-ui013複審作業頁.md`
- `docs/05-ui/ui-page-spec-ui014revision歷程頁.md`
- `docs/05-ui/ui-layout-archetypes.md`

### Roadmap
- `docs/06-frontend/frontend-design-roadmap.md`

---

## 3. 適用範圍與非範圍

### 適用範圍
- 僅 cases cluster：UI-009 ~ UI-014。
- 僅凍結 wireframe first-pass 的頁面範圍、結構變體與呈現重點。

### 非範圍
- 不產生實際 wireframe 畫稿。
- 不產生 `ui-design-decisions.md`。
- 不產生 `ui-visual-baseline.md`。
- 不進 implementation plan / Angular code / component code。
- 不處理 cases 以外模組（UI-001~UI-007、UI-015~UI-017）。

---

## 4. Wireframe Brief 原則

1. **結構差異優先於視覺細節**：先凍結 region presence、hierarchy、action zone type。  
2. **shared structures 先穩定**：跨頁要保持結構角色一致，再進行頁面特化。  
3. **mode/projection/blocked/exception 必須可視化**：不可只用文案註解帶過。  
4. **deferred 區塊只畫 placeholder**：不能在 first-pass 假裝定稿。  
5. **FULL/METADATA_ONLY 是同頁變體**：不得拆成獨立頁。  
6. **command vs history 邊界不可混淆**：UI-012/UI-013 是 command pages；UI-014 只做 navigate-only（含條件入口）。

---

## 5. First-Pass 頁面範圍（凍結）

### P1（先畫，風險最高）
1. UI-013 複審作業頁（A4）
2. UI-014 Revision 歷程頁（A5）
3. UI-010 案件詳情頁（A2）

### P2（接續完成）
4. UI-012 初審作業頁（A4）
5. UI-011 案件分派頁（A3）
6. UI-009 案件列表頁（A1）

> 說明：P1 先處理 O1-C exception flow + compare projection + control center 導頁語意；P2 補齊 command/list consistency。

---

## 6. Shared Structures to Preserve Across Wireframes

### S1. CaseSummaryHeader [C01]
- 固定角色：頁面上下文識別，不承載主要 command。
- 相對位置：頁首主區（所有頁）。
- 變體要求：FULL vs METADATA_ONLY 資訊密度差可視化。

### S2. StatusOrBlockedBanner [C02]
- 固定角色：terminal/forbidden/blocked/exception 狀態提示。
- 相對位置：summary 後、action/內容前。
- 變體要求：blocked 與 exception 文案/狀態不可混為同一類。

### S3. ModeIndicatorStrip [C03]
- 固定角色：顯示頁面行為模式（NEW/REVISION/READONLY...）。
- 相對位置：狀態提示與主要可操作區之間（或緊鄰）。
- 變體要求：UI-013 需可辨識 CLOSED revision-exception。

### S4. ProjectionNoticeBanner [C04]
- 固定角色：metadata-only 降級提示，不取代 blocked banner。
- 相對位置：在 metadata-only 場景中靠近 summary/status 區。
- 變體要求：僅 UI-009/UI-010/UI-014 出現。

---

## 7. Per-Page Wireframe Brief

## UI-009
- pageId: `UI-009`
- archetype: A1 List / Work Queue
- primary user goal: 以 tab=query preset + filter 找到下一步作業入口。
- must-show regions:
  - CaseSummaryHeader [C01]
  - ProjectionNoticeBanner [C04]（metadata-only）
  - CaseWorkQueueFilterBar [C05]
  - CaseWorkQueueTable [C06]
  - CaseRowActionSet [C07]
- optional / placeholder regions:
  - bulk action / export（僅 placeholder）
  - quick hint
- primary actions: row-level navigate actions（detail/assign/review/history）。
- blocked / forbidden presentation: forbidden/empty/error 與 blocked reasons 必須可視。
- projection presentation: FULL vs METADATA_ONLY row 欄位差異同頁呈現。
- must-visualize variants: role-default tab、metadata row、row action overflow。
- deferred areas: 無新增 deferred，但 sysadmin filter 最小集可用 placeholder 註記。
- notes for design execution: action 是 row-level，不得畫成 page-level command bar。

## UI-010
- pageId: `UI-010`
- archetype: A2 Detail / Control Center
- primary user goal: 判讀狀態與脈絡，導向正確 command/history。
- must-show regions:
  - C01/C02/C03/C04（依 projection）
  - CaseStatusActionPanel [C08]
  - TraceabilitySummaryRegion [C09]
  - InitialReviewSummaryRegion [C10]
  - RereviewSummaryRegion [C11]
- optional / placeholder regions:
  - RevisionLifecycleEntryArea [C24]（placeholder only）
- primary actions: navigate-only（到 UI-011/UI-012/UI-013/UI-014）。
- blocked / forbidden presentation: terminal blocked、role-limited action 需有明確位置。
- projection presentation: FULL/METADATA_ONLY 同頁結構，內容密度不同。
- must-visualize variants: terminal blocked、role-limited action zone、closed-exception 導頁語意。
- deferred areas: C24。
- notes for design execution: 本頁不可出現直接 submit/revise command。

## UI-011
- pageId: `UI-011`
- archetype: A3 Action / Assignment
- primary user goal: manager 完成 assign/reassign 指派命令。
- must-show regions:
  - C01/C02/C03
  - AssignmentModeBanner [C12]
  - ReviewerAssignmentSection [C13]
  - RereviewerAssignmentSection [C14]
  - page-level submit/back action zone（對齊 A3）
- optional / placeholder regions:
  - AssignmentDueDateHelperRegion [C23]（placeholder only）
- primary actions: assign/reassign reviewer & rereviewer + submit/cancel。
- blocked / forbidden presentation: READONLY_BLOCKED（CLOSED/CANCELLED）明確壓制輸入區。
- projection presentation: 無 metadata-only 主情境。
- must-visualize variants: ASSIGN / REASSIGN / MIXED / READONLY_BLOCKED。
- deferred areas: C23。
- notes for design execution: 指派區塊語意分離，避免 reviewer/rereviewer 混成單一 generic section。

## UI-012
- pageId: `UI-012`
- archetype: A4 Action / Review Form
- primary user goal: reviewer 完成初審草稿/提交/修正命令。
- must-show regions:
  - C01/C02/C03
  - ReviewFormSummaryStrip [C15]
  - review main form + commentary/attachment region
  - ReviewCommandBar [C16]
  - ReviewVersionSummaryRegion [C17]
- optional / placeholder regions:
  - 進階附件檢視細節（placeholder）
- primary actions: save draft / submit / revise（依 mode 顯示）。
- blocked / forbidden presentation: ownership/status blocked 一定要在 command bar 鄰近可見。
- projection presentation: 無 metadata-only 主情境。
- must-visualize variants: NEW / EDIT_DRAFT / REVISION / READONLY_BLOCKED。
- deferred areas: 無新增 deferred。
- notes for design execution: command bar 必須與 status/mode 關聯，不能孤立。

## UI-013
- pageId: `UI-013`
- archetype: A4 Action / Review Form
- primary user goal: rereviewer 完成複審決策與 revision（含 CLOSED exception）。
- must-show regions:
  - C01/C02/C03
  - C15/C16/C17
  - InitialSummaryReferenceRegion [C18]
  - FinalDecisionSummaryRegion [C19]
- optional / placeholder regions:
  - 深層比較輔助（placeholder）
- primary actions: save draft / submit / revise（mode-driven）。
- blocked / forbidden presentation: READONLY_BLOCKED 與 exception 模式必須明確區隔。
- projection presentation: 無 metadata-only 主情境。
- must-visualize variants:
  - NEW_REREVIEW / EDIT_DRAFT / REVISION / READONLY_BLOCKED
  - CLOSED revision-exception（submit blocked + revise allowed）
- deferred areas: C18/C19 不升級 shared，維持 page-owned 呈現。
- notes for design execution: 要明確表現「command page」而非歷程頁。

## UI-014
- pageId: `UI-014`
- archetype: A5 History / Traceability
- primary user goal: 閱讀 revision 演進、比對摘要、條件導向 revision command。
- must-show regions:
  - C01/C02/C03/C04（metadata-only 時）
  - RevisionTimelineRegion [C20]
  - RevisionCompareSummaryRegion [C21]
  - RevisionNavigationActionZone [C22]
- optional / placeholder regions:
  - compare helper hint
- primary actions: stage tab 切換、選擇 revision、navigate to command（條件式）。
- blocked / forbidden presentation: scope denied 時 timeline/compare 禁用或不載入。
- projection presentation: FULL vs METADATA_ONLY compare 降級同頁呈現。
- must-visualize variants: ALL/INITIAL_ONLY/REREVIEW_ONLY、effective version 強調、conditional exception entry。
- deferred areas: 無新增 deferred。
- notes for design execution: 本頁不可出現直接提交按鈕。

---

## 8. Variant Matrix（First-Pass 必畫）

| Page | 必畫 variants |
|---|---|
| UI-009 | role-default tabs、FULL vs METADATA_ONLY、forbidden/empty/error |
| UI-010 | FULL vs METADATA_ONLY、terminal blocked、role-limited action zone、closed-exception navigate-only hint |
| UI-011 | ASSIGN、REASSIGN、MIXED、READONLY_BLOCKED |
| UI-012 | NEW、EDIT_DRAFT、REVISION、READONLY_BLOCKED |
| UI-013 | NEW_REREVIEW、EDIT_DRAFT、REVISION、READONLY_BLOCKED、CLOSED revision-exception |
| UI-014 | ALL/INITIAL_ONLY/REREVIEW_ONLY、effective version emphasis、FULL vs METADATA_ONLY compare、closed with conditional exception entry |

---

## 9. Priority Visualization Targets（High-Risk）

1. **C16 ReviewCommandBar**
   - 風險：mode/blocked/exception 對 action 可用性影響最大；若畫錯會把 command page 語意畫壞。

2. **C21 RevisionCompareSummaryRegion**
   - 風險：FULL vs METADATA_ONLY 差異最大；容易誤畫成完整 diff 永遠可見。

3. **C22 RevisionNavigationActionZone**
   - 風險：history page 的 navigate-only 與 command submit 邊界最容易被混淆；O1-C 入口條件必須正確。

---

## 10. Placeholder / Deferred Treatment

以下只允許 placeholder，不得作為 first-pass 定稿重點：

1. `AssignmentDueDateHelperRegion [C23]`
2. `RevisionLifecycleEntryArea [C24]`
3. 受 API 粒度未定影響的 lifecycle 深層細節
4. metadata-only compare 欄位密度細節（只凍結「有降級」，不凍結最終資訊量）

---

## 11. Pending Confirmation

僅保留仍待 authority 最終明確者：

1. `INITIAL_REVIEW_COMPLETED` manager handoff commands 的 API 顯式化程度。
2. lifecycle API 欄位粒度與排序。
3. metadata-only compare 欄位顯示密度（摘要粒度）。

> 已批准回寫 O1-C / O2-A / O3-C，不列入 pending。

---

## 12. Next-Step Handoff

### 交付對象
- 設計執行者（wireframe production owner）。

### 下一步
1. 依本 brief 的 P1/P2 順序繪製 first-pass wireframe。
2. 每頁至少覆蓋本 brief 指定的 must-show regions + must-visualize variants。
3. 對 high-risk targets（C16/C21/C22）先行評審，再展開其餘頁。

### 畫完後需進 `ui-design-decisions.md` 的議題類型（僅列議題，不在本輪決策）
- mode strip 與 blocked banner 的相對優先序衝突。
- metadata-only compare 的資訊密度最終級別。
- lifecycle placeholder（C24）是否拆分次區塊。

### deferred 區塊處理規範
- C23/C24 在 pending 清除前不可強行定稿為固定版面。
- 若設計需呈現，僅可用 placeholder 區塊與註記表達。
