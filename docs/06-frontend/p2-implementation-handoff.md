# P2 Implementation Handoff

## 1. 文件目的

本文件承接 P2 已完成的 page spec、wireframe、low-fidelity prototype 與人工 review 結論，
將 UI-009、UI-011、UI-012 收斂成可直接提供前端工程使用的 implementation handoff 輸入。

本文件的用途是：

- 固定 P2 頁面的 page role / archetype / primary responsibility
- 固定 P2 與既有 P1 shared structure / visual baseline 的承接方式
- 整理 Angular page skeleton、feature boundary、implementation planning 所需的穩定輸入
- 在不重開 design decision 的前提下，明確標示 must keep / deferred / implementation caution

本文件不是 implementation spec，不直接定義 Angular component API、template 細節、樣式 token 或互動微行為。

---

## 2. Source Inputs

### 2.1 Authority / Bridge Inputs

| 類型 | 路徑 | 採用內容 |
| --- | --- | --- |
| Decision authority | `docs/01-requirements/decision-log.md` | D-027、D-028：CLOSED rereview revision exception、metadata-only whitelist |
| Permission | `docs/02-ddd/permission-matrix.md` | page access、metadata-only scope、action permission |
| State machine | `docs/02-ddd/state-machine.md` | case lifecycle、terminal/blocking、revision guard |
| API contract | `docs/03-contracts/api-spec.md` | case-rooted API、metadata-only whitelist、error codes |
| UI archetype | `docs/05-ui/ui-layout-archetypes.md` | A1 / A3 / A4 定義 |
| UI page spec | `docs/05-ui/ui-page-spec-ui009案件列表頁.md` | UI-009 page responsibility、role defaults、tab/filter/list truth |
| UI page spec | `docs/05-ui/ui-page-spec-ui011案件分派頁.md` | UI-011 assignment truth、mode set、guard boundary |
| UI page spec | `docs/05-ui/ui-page-spec-ui012初審作業頁.md` | UI-012 command page truth、mode set、revision constraint |
| Frontend bridge | `docs/06-frontend/ui-component-map.md` | shared / feature-scoped / page-owned / deferred mapping |
| Frontend bridge | `docs/06-frontend/ui-view-model-spec.md` | page-level view model boundary |
| Frontend bridge | `docs/06-frontend/ui-state-actions-matrix.md` | mode resolution、action grammar、blocked reasons |
| Frontend bridge | `docs/06-frontend/ui-route-module-map.md` | route boundary、guard boundary、feature grouping |
| Frontend bridge | `docs/06-frontend/frontend-test-scenarios.md` | route guard、mode resolution、projection、query preset scenarios |

### 2.2 Design / Prototype Inputs

| 類型 | 路徑 | 採用內容 |
| --- | --- | --- |
| P1 visual baseline | `docs/07-design/ui-visual-baseline.md` | hierarchy-first、projection visible、navigate-only vs command 分離 |
| P1 design decision | `docs/07-design/ui-design-decisions.md` | DD-001 ~ DD-008 已批准結論，僅承接不重開 |
| Base brief | `docs/07-design/ui-wireframe-brief.md` | shared structure、P2 must-show 區塊 |
| P2 extension brief | `docs/07-design/ui-wireframe-brief-p2-extension.md` | P2 page role / reuse / deferred baseline |
| Wireframe index | `docs/07-design/wireframes/README.md` | P2 first-pass complete 狀態與 high-risk 摘要 |
| P2 wireframe | `docs/07-design/wireframes/ui009-case-list-first-pass.md` | UI-009 結構、row action / overflow / metadata-only 同頁投影 |
| P2 wireframe | `docs/07-design/wireframes/ui011-assignment-first-pass.md` | UI-011 reviewer/rereviewer separation、A3 submit zone |
| P2 wireframe | `docs/07-design/wireframes/ui012-initial-review-first-pass.md` | UI-012 A4 grammar、C16 inheritance、append-only revision |
| Prototype route harness | `frontends/quality-inspection-frontend/prototype-react/src/prototype/App.tsx` | `/cases`、`/assign`、`/initial-review` route presence |
| Prototype data | `frontends/quality-inspection-frontend/prototype-react/src/prototype/p2Data.ts` | P2 variant registry、low-fidelity data density、picker placeholder |
| Prototype screen | `frontends/quality-inspection-frontend/prototype-react/src/prototype/screens/WorkQueueScreen.tsx` | UI-009 page frame / list-region usage |
| Prototype screen | `frontends/quality-inspection-frontend/prototype-react/src/prototype/screens/AssignmentScreen.tsx` | UI-011 assignment sections、page-level command zone |
| Prototype screen | `frontends/quality-inspection-frontend/prototype-react/src/prototype/screens/InitialReviewScreen.tsx` | UI-012 command grammar / revision context / version summary |

### 2.3 Review Inputs

| 類型 | 來源 | 採用內容 |
| --- | --- | --- |
| Human review conclusion | current task context | UI-009 prototype 已足以表達 work queue；UI-011 已能清楚看見選人動作；可進 handoff preparation |
| Prototype refinement result | current repo state | UI-009 已補足列表密度；UI-011 已補足選人動作顯性化；兩者均為 prototype 表達補強，不新增 truth |

---

## 3. Scope

### 本文件涵蓋

- 僅涵蓋 P2：UI-009、UI-011、UI-012
- 僅整理 implementation handoff 所需的 page boundary、shared reuse、page-owned region、action grammar、deferred 邊界
- 僅提供 Angular page skeleton / feature boundary / implementation planning 的穩定輸入

### 本文件不涵蓋

- 不等於 implementation spec
- 不改動 domain / permission / API truth
- 不重開 DD-001 ~ DD-008 或任何 P1 / P2 design decision
- 不重做 wireframe 或 prototype
- 不涵蓋 P1 頁面或 P2 以外頁面
- 不把 prototype placeholder 寫成正式 UI law

---

## 4. P2 Page Role Summary

| Page | Page Role | Archetype | Primary Responsibility | Non-goals | 與 P1 baseline 關係 |
| --- | --- | --- | --- | --- | --- |
| UI-009 | role-aware work queue | A1 List / Work Queue | 以 role-default preset + filter + row-level navigate action 帶使用者進入正確下一步 | 不做 page-level command、不顯示 full submission 內容、不直接執行 revision | 承接 P1 的 projection visible 與 navigate-only 語法，但屬 A1，不承接 A4 command bar |
| UI-011 | manager assignment command page | A3 Action / Assignment | 讓 manager 完成 assign / reassign reviewer 與 rereviewer 命令，並清楚區分兩個 assignment family | 不做 control center、不做 review form、不看 full submission、不承接 revision exception | 承接 P1 的 C01/C02/C03 shared hierarchy 與 blocked semantics，但 submit zone 是 A3 page-level command，不是 A4 fixed bar |
| UI-012 | reviewer initial review command page | A4 Action / Review Form | 讓合法 reviewer 執行 draft / submit / revise initial review，並保留 append-only revision 語意 | 不做 assignment、不做 rereview、不做 lifecycle center、不承接 CLOSED exception | 直接承接 P1 A4 grammar、C15/C16/C17 reuse 與 C16 高可見規則，但移除 UI-013 專屬 CLOSED exception branch |

### P2 page typing 固定結論

1. UI-009 是 A1 work queue，不可因實作便利退化成 dashboard、generic table page 或 page-level command page。
2. UI-011 是 A3 assignment command page，不可因欄位長得像 form 而誤實作成 generic CRUD editor。
3. UI-012 是 A4 review command page，不可因表單內容存在而誤實作成一般 edit page。

---

## 5. Shared Structure Reuse

## 5.1 Direct Reuse from Existing Shared Structure

| Shared structure | Reused by | Handoff interpretation |
| --- | --- | --- |
| C01 CaseSummaryHeader | UI-009 / UI-011 / UI-012 | 所有 P2 頁面都要保留 case context；不可被頁面主區吃掉，也不可混入主要 command |
| C02 StatusOrBlockedBanner | UI-011 / UI-012 | blocked/readonly/terminal 要先於主要操作區被看見；避免把錯誤狀態只寫進按鈕 disabled 文案 |
| C03 ModeIndicatorStrip | UI-011 / UI-012，UI-009 則以 role + preset mode 語意承接 | page mode 是業務語意，不是技術狀態；需由 mode resolver 統一輸出 |
| C04 ProjectionNoticeBanner | UI-009 | metadata-only 仍是同頁 projection；在 UI-009 以 header inline indicator 為優先，不另建 metadata route |
| C15 ReviewFormSummaryStrip | UI-012 | 直接沿用 A4 shared context strip，不放答案欄位 |
| C16 ReviewCommandBar | UI-012 | 直接沿用 A4 command grammar，保留 save / submit / revise 的語意分離 |
| C17 ReviewVersionSummaryRegion | UI-012 | 直接沿用 append-only / basedOn / version context 顯示 |

## 5.2 P2-Specific Extension

| Structure family | Owned by | 說明 |
| --- | --- | --- |
| C05 / C06 / C07 | UI-009 / cases worklist | A1 專屬 filter、result、row-action family；是 feature-scoped，不要硬抽成 generic app-wide table shell |
| C12 / C13 / C14 | UI-011 / cases assignment | assignment mode、reviewer assignment、rereviewer assignment 明確分離；避免混成單一 generic assignment form |
| 評分表主作業區 + commentary / attachment 區 | UI-012 page-owned composition on top of A4 | 結構必須保留，但 item-level control 與附件細節仍屬後續 implementation decision |

## 5.3 Page-Owned Regions

| Page | Page-owned region | Handoff note |
| --- | --- | --- |
| UI-009 | paging zone、table-level forbidden/empty/error state | 可沿用 shared paging pattern，但 page-level state 文案與 query context 屬 UI-009 自有 |
| UI-011 | submit / back action zone | 可以共用 command widget 外框，但語意必須維持 A3 page-level submit/back，不可冒充 A4 fixed-bottom review bar |
| UI-012 | main review form region、commentary / attachment region | 是 A4 page composition，不等於共享 component API；先保留 region 邊界，再進一步拆 component |

## 5.4 Deferred

| Item | 狀態 | Handoff interpretation |
| --- | --- | --- |
| C23 AssignmentDueDateHelperRegion | deferred | UI-011 預留可插拔位置即可，不得在本輪升級成 must-have 完成互動 |
| UI-009 bulk action / export | placeholder only | 不是本輪 Angular skeleton 必備範圍 |
| UI-011 candidate picker 深層互動 | placeholder only | prototype 只證明選人入口位置，不代表真實搜尋器或 modal 規格已定稿 |
| UI-012 score item-level interaction detail | placeholder only | page skeleton 需保留 form region，但 item control 細節待 implementation planning |
| UI-012 attachment deep interaction | placeholder only | 保留 region 與 intent，不在 handoff 中升級為完整互動規格 |

---

## 6. Per-Page Implementation Handoff Notes

## 6.1 UI-009 案件列表頁

### UI-009 page boundary

- route group：`/cases`
- page identity：單一 work queue page，tab 僅是 query preset，不是子頁
- default landing：依 role default preset 進入，不同角色只改 preset 與 action visibility

### UI-009 must-have regions

1. C01 CaseSummaryHeader
2. C04 metadata-only inline indicator（僅 metadata-only）
3. C05 CaseWorkQueueFilterBar
4. C06 CaseWorkQueueTable
5. C07 CaseRowActionSet
6. paging zone
7. table-level forbidden / empty / error state

### UI-009 action grammar

- 所有 action 均為 row-level navigate / read entry
- page 上不建立 submit / save / bulk command bar
- visible action 由 role + state + scope 決定
- blocked action 可以 visible + disabled + reason，也可以 hidden；但 metadata-only 的 business command 必須 hidden

### UI-009 projection / blocked / placeholder handling

- metadata-only 是同 route、同頁、同 archetype 下的 projection，不得拆 metadata page
- metadata-only 欄位只允許 shared base + UI-009 extension
- forbidden / empty / error 應以 table-level state 呈現，不保留假 rows
- row action overflow 只是小螢幕呈現策略，不改 row ownership

### UI-009 implementation caution

- 不要把 tab 切換實作成不同 page component
- 不要因為資料表格容易抽象就把 row action policy 變成 generic table action schema
- 不要把 prototype 中的示範 row 筆數誤讀成正式 pagination 規格
- back navigation 必須保留 query context（tab / filters / pagination / sort）

### UI-009 what must not be changed casually

- A1 work queue 身分
- metadata-only 同頁投影策略
- row-level navigate-only 邊界
- role-default preset + action visibility 的 policy-based 決策方式

## 6.2 UI-011 案件分派頁

### UI-011 page boundary

- route group：`/cases/:caseId/assign`
- page identity：manager assignment command page
- guard boundary：role hard guard + scope guard + status guard

### UI-011 must-have regions

1. C01 CaseSummaryHeader
2. C02 StatusOrBlockedBanner
3. C03 ModeIndicatorStrip
4. C12 AssignmentModeBanner
5. C13 ReviewerAssignmentSection
6. C14 RereviewerAssignmentSection
7. page-level submit / back action zone
8. C23 placeholder slot（可選、可插拔）

### UI-011 action grammar

- page-level primary command + secondary back/cancel
- submit label 必須隨 mode 對應實際命令，例如初審指派、初審改派、複審指派
- reviewer 與 rereviewer 兩個 assignment family 必須分開承載，不用單一 generic section 吃掉全部情境
- READONLY_BLOCKED 時不保留可誤觸的 picker 或 submit CTA

### UI-011 projection / blocked / placeholder handling

- 本頁沒有 metadata-only command variant
- terminal / scope mismatch / role mismatch 需進 blocked 或 deny；不可渲染可操作的 assignment input
- candidate picker 的存在是結構真相，但 prototype 中的 low-fidelity picker preview 只證明「選人入口位置」
- C23 due date helper 目前仍是 deferred slot，不要寫成 implementation 必備完整功能

### UI-011 implementation caution

- 不要把 A3 submit zone 實作成 A4 fixed-bottom review bar
- 不要把 due date / reason / candidate search 一次綁成過度肥大的單一 form
- 不要以 prototype 的 chip list 或 placeholder slot 直接推導真實 picker interaction
- status/mode 先決定 section 是否 readonly，再決定 command label，不要倒過來由 UI 控制 rule

### UI-011 what must not be changed casually

- A3 assignment page 身分
- reviewer / rereviewer section separation
- ASSIGN / REASSIGN / MIXED / READONLY_BLOCKED mode grammar
- blocked 時 command 壓制策略

## 6.3 UI-012 初審作業頁

### UI-012 page boundary

- route group：`/cases/:caseId/initial-review`
- page identity：reviewer initial review command page
- guard boundary：role + ownership + status guard

### UI-012 must-have regions

1. C01 CaseSummaryHeader
2. C02 StatusOrBlockedBanner
3. C03 ModeIndicatorStrip
4. C15 ReviewFormSummaryStrip
5. main review form region
6. commentary / attachment region
7. C17 ReviewVersionSummaryRegion
8. C16 ReviewCommandBar

### UI-012 action grammar

- mode-driven command grammar：NEW / EDIT_DRAFT / REVISION / READONLY_BLOCKED
- save draft、submit initial、submit initial revision 必須在語意上分離
- REVISION 必須明確表達 append-only，不得被誤做成 edit existing submission
- blocked mode 只能保留 back 或 readonly context，不可殘留可提交命令

### UI-012 projection / blocked / placeholder handling

- 本頁不建立 metadata-only command variant
- 非本人、CLOSED、CANCELLED、stage mismatch 必須 blocked
- revision context 與 version summary 要能顯示 basedOn / current submission reference
- score item-level control、attachment deep interaction 在本輪只需保留 region 與 intent

### UI-012 implementation caution

- UI-012 承接 A4 grammar，但不承接 UI-013 的 CLOSED exception branch
- C16 command bar 應維持高可見優先序；若沿用 P1 A4 fixed strategy，需預留底部補白避免遮擋主作業區
- 不要讓表單 component 直接決定 revision guard 或 ownership rule
- 不要把 prototype placeholder 當成正式題型 schema

### UI-012 what must not be changed casually

- A4 command page 身分
- append-only revision 語意
- C15 / C16 / C17 的 shared A4 結構
- blocked 與 revision mode 的可視差異

---

## 7. Prototype-to-Implementation Notes

## 7.1 Prototype 中哪些只是 low-fidelity 表達

| Prototype artifact | 不應直接當成 implementation law 的部分 |
| --- | --- |
| UI-009 demo rows | row 數量、文案、caseId、示範摘要值只是 review density proof，不是正式資料 schema |
| UI-009 filter chips | chips 的文字與排序只是 query preset 的低保真表達，不是最終控件設計 |
| UI-011 AssignmentPickerPreview | 只證明 reviewer / rereviewer 選人入口的位置與分離，不等於最終 picker、search、modal 或 autocomplete 規格 |
| UI-011 helper lines | due date helper / change reason 的 placeholder 只證明 slot 存在，不代表本輪必做完整互動 |
| UI-012 PlaceholderRegion | 只證明 main form / commentary / attachment 區塊邊界，不等於題型元件設計 |
| VariantSwitcher / review harness | 僅供設計 review 與 state 對照，不應直接搬進正式產品路由或頁面 UI |

## 7.2 哪些結構必須保留到 implementation

| 結構 | 必須保留原因 |
| --- | --- |
| UI-009 單頁 work queue + query preset | 是 page identity 與 route policy 的核心 |
| UI-009 metadata-only 同頁 projection | 是 O3-C 與 permission truth 的直接落點 |
| UI-011 reviewer / rereviewer section separation | 是 assignment grammar 的核心，不可被 generic form 吃掉 |
| UI-011 page-level submit zone | 是 A3 與 A4 的主要分界 |
| UI-012 C15 / C16 / C17 hierarchy | 是 A4 command page 的 shared structure 核心 |
| UI-012 append-only revision context | 直接承接 domain / decision truth，不能在 implementation 被稀釋 |

## 7.3 哪些是 review artifact，不要誤當正式 UI 細節

1. prototype 的 variant 名稱是 review/harness naming，不一定等於最終使用者看到的 label。
2. prototype 的 command result message 只是驗證 action grammar 的假回饋，不是正式 toast 或 success flow。
3. prototype 的 inline reviewer notes、section title 與 scenario description 是 review 語言，不是正式產品 copy。
4. prototype route query `?variant=` 是驗證 mode 的工具，不是 production routing policy。

---

## 8. Deferred / Non-Goals

### 明確 deferred

1. UI-009 bulk action / export
2. UI-011 candidate picker 深層互動（搜尋、autocomplete、modal、清單分頁）
3. UI-011 C23 due date helper 的完整欄位規則與驗證
4. UI-011 change reason 的正式 requirement 與資料約束
5. UI-012 score item-level control library 與 input 詳規
6. UI-012 attachment 深層互動（上傳流程、預覽、刪除、錯誤復原）

### 明確 non-goals

1. 不新增 metadata-only command page variants 到 UI-011 / UI-012
2. 不將 prototype review harness 納入正式產品 UI
3. 不在本文件內定義 Angular component API、NgRx state shape 或 service contracts
4. 不在本輪內補齊非 P2 頁面
5. 不將 deferred 項目偷偷轉成 must-have implementation

---

## 9. Readiness Assessment

## 9.1 已足夠進 implementation 的部分

| Page | Ready items |
| --- | --- |
| UI-009 | page role、route boundary、query preset 心智模型、metadata-only projection 策略、row-level action grammar、shared vs page-owned 結構已足夠支撐 Angular skeleton |
| UI-011 | page role、A3 submit zone 身分、reviewer/rereviewer section 分離、mode grammar、blocked handling、deferred 邊界已足夠支撐 Angular skeleton |
| UI-012 | A4 grammar、C15/C16/C17 reuse、append-only revision 語意、blocked handling、page-owned form region 邊界已足夠支撐 Angular skeleton |

## 9.2 implementation 時需特別留意

1. UI-009 要守住 row-level navigate-only 邊界，不要被 generic table action 或 bulk UX 拉偏。
2. UI-011 要守住 reviewer/rereviewer section separation，不要以單一可重用表單抽象破壞語意。
3. UI-011 的 picker 互動細節仍未定稿，實作者應先以 slot + capability boundary 落 skeleton，而非臆造完整互動規格。
4. UI-012 要守住 append-only revision 與 ownership/status blocked rule，不要把 revision 寫成 edit existing submission。
5. UI-012 若採 A4 fixed-bottom command bar，需一併處理主作業區底部補白與可見性。

## 9.3 blocker assessment

- 目前沒有 blocker 需要回頭重開 P1 truth、DD-001 ~ DD-008 或 P2 page role。
- 目前沒有證據顯示 UI-009 / UI-011 / UI-012 仍缺關鍵 page truth、wireframe 或 prototype input。
- 本輪 handoff 可直接作為下一步 Angular page skeleton / implementation planning 的穩定輸入。

---

## 10. Handoff Outcome

P2 implementation handoff 的固定結論如下：

1. UI-009 / UI-011 / UI-012 的 page role 已固定，且與 A1 / A3 / A4 archetype 對齊。
2. P2 與既有 P1 的 shared structure 承接關係已固定，不需再回頭重開 P1 baseline。
3. shared reuse、page-owned region、critical action grammar、deferred 邊界已足夠支撐 Angular skeleton 與 feature boundary 規劃。
4. prototype 只作為 role / structure / flow / action grammar 的驗證證據，不作為正式 UI 細節真相。
5. P2 已可進下一步 Angular page skeleton / implementation planning。
