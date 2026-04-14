# UI Layout Archetypes v1 (Cases Cluster)

## 1. 文件目的

本文件將 cases 主幹頁面（UI-009 ~ UI-014）收斂為可重用的 **layout archetypes**，作為下一輪：
- `ui-component-map.md`
- `ui-wireframe-brief.md`

的前置基線。  
本文件只定義結構語意與區域關係，不進入 component hierarchy、wireframe 細節、視覺 token 或實作碼。

---

## 2. Source Inputs

### Foundational / Bridge
- `docs/06-frontend/frontend-design-method.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/06-frontend/ui-state-actions-matrix.md`
- `docs/06-frontend/frontend-test-scenarios.md`
- `docs/06-frontend/ui-route-module-map.md`
- `docs/06-frontend/ui-view-model-spec.md`
- `docs/06-frontend/ui-api-binding-matrix.md`
- `docs/06-frontend/frontend-review-gate-pending-items.md`

### Upstream UI authority
- `docs/05-ui/ui-information-architecture.md`
- `docs/05-ui/ui-page-inventory.md`
- `docs/05-ui/ui-page-spec-ui009案件列表頁.md`
- `docs/05-ui/ui-page-spec-ui010案件詳情頁.md`
- `docs/05-ui/ui-page-spec-ui011案件分派頁.md`
- `docs/05-ui/ui-page-spec-ui012初審作業頁.md`
- `docs/05-ui/ui-page-spec-ui013複審作業頁.md`
- `docs/05-ui/ui-page-spec-ui014revision歷程頁.md`

### Roadmap
- `docs/06-frontend/frontend-design-roadmap.md`

---

## 3. 適用範圍與非範圍

### 適用範圍
- 僅涵蓋 UI-009 ~ UI-014（cases 主幹）。

### 非範圍
- 不做 component map
- 不做 wireframe brief
- 不做 visual baseline
- 不做 implementation / Angular code / route-module code

---

## 4. Archetype 定義原則

1. **以業務任務命名**，非純視覺命名。  
   archetype 名稱需保留 work queue / control center / command / traceability 語意。
2. **archetype 受 page responsibility + page mode + action policy 驅動**。  
   layout 不是靜態版型，而是承接狀態、角色、權限與操作可執行性。
3. **metadata-only 為 projection**。  
   同頁同 archetype 下切換資料與區塊顯示，不分裂新頁型。
4. **blocked/forbidden/exception 是 layout 一級結構需求**。  
   需有固定 banner/status/mode indicator 區域，不可臨時散落。
5. **responsive / density 只談結構影響**。  
   例如區塊堆疊順序、主次資訊優先級，不談像素與視覺 token。

---

## 5. Archetype 清單（Cases）

```yaml
archetypes:
  - A1 List / Work Queue
  - A2 Detail / Control Center
  - A3 Action / Assignment
  - A4 Action / Review Form
  - A5 History / Traceability
```

---

## 6. Archetype 詳細規格

## A1. List / Work Queue

- **archetype name**: List / Work Queue
- **primary purpose**: 角色導向工作入口與下一步導頁
- **applicable pages**: UI-009
- **primary actor patterns**: manager / reviewer / rereviewer / auditor / system admin
- **required regions**:
  - page summary/header
  - tab query preset zone
  - filter zone
  - result table zone
  - row action zone
  - paging zone
- **optional regions**:
  - bulk action/export zone
  - quick hint zone
- **action bar pattern**:
  - 以 row-level action 為主，page-level action 為輔
- **summary header pattern**:
  - 顯示「目前角色視角 + 工作量摘要」
- **status / banner pattern**:
  - forbidden/empty/error 橫幅
  - metadata-only notice（system admin）
- **mode indicator pattern**:
  - 角色模式 + tab preset 模式
- **projection behavior**:
  - FULL: 業務欄位完整
  - METADATA_ONLY: shared base + UI-009 extension
- **blocked / forbidden behavior**:
  - action 隱藏或 disabled + blocked reason
- **exception handling notes**:
  - 無 closed exception command；僅導頁到 detail/history/command page
- **navigation / entry expectations**:
  - shell 預設導入 `/cases?tab=...`
- **responsive structure notes**:
  - 小螢幕先 summary/filter，再 table（row action 轉 overflow）
- **dependencies**:
  - state/actions matrix、permission projection、route map、list API binding
- **notes for future component extraction**:
  - 可抽 shared summary/filter/table/action-column 結構
- **notes for future wireframe**:
  - 先畫 role default tabs、metadata-only table row 差異

---

## A2. Detail / Control Center

- **archetype name**: Detail / Control Center
- **primary purpose**: 判讀狀態、彙整脈絡、導向正確 command/history
- **applicable pages**: UI-010
- **primary actor patterns**: 全角色（含 metadata-only）
- **required regions**:
  - summary header strip
  - state/action zone
  - traceability zone
  - initial summary zone
  - rereview summary zone
  - revision/lifecycle entry zone
- **optional regions**:
  - role restriction hint zone
  - score digest zone
- **action bar pattern**:
  - navigate-only actions（不直接 submit）
- **summary header pattern**:
  - case 核心識別 + status + assignment
- **status / banner pattern**:
  - terminal/blocked reason banner
  - metadata-only projection banner
- **mode indicator pattern**:
  - FULL_CONTROL_CENTER / ROLE_LIMITED / METADATA_CONTROL_CENTER
- **projection behavior**:
  - FULL: 摘要可見完整業務內容
  - METADATA_ONLY: shared base + UI-010 extension
- **blocked / forbidden behavior**:
  - scope/role 不符顯示 forbidden；不渲染敏感區
- **exception handling notes**:
  - CLOSED rereview exception 不在本頁提交，僅提供導頁語意
- **navigation / entry expectations**:
  - 由 list row 或 deep link 進入；返回保留 list query context
- **responsive structure notes**:
  - summary+state action 優先固定；traceability 與摘要區可折疊
- **dependencies**:
  - detail VM、action panel VM、detail/lifecycle/revision read bindings
- **notes for future component extraction**:
  - 可抽 control-center action zone、traceability region
- **notes for future wireframe**:
  - 重點畫 FULL vs METADATA_ONLY 區塊顯示差

---

## A3. Action / Assignment

- **archetype name**: Action / Assignment
- **primary purpose**: manager 指派/改派 command 操作
- **applicable pages**: UI-011
- **primary actor patterns**: HOSPITAL_MANAGER
- **required regions**:
  - task-focused summary header
  - assignment mode banner
  - reviewer assignment section
  - rereviewer assignment section
  - submit/confirm action zone
- **optional regions**:
  - due date helper
  - change reason note
- **action bar pattern**:
  - page-level primary command + secondary cancel/back
- **summary header pattern**:
  - 僅保留指派決策必要欄位
- **status / banner pattern**:
  - READONLY_BLOCKED banner（CLOSED/CANCELLED 或非法狀態）
- **mode indicator pattern**:
  - ASSIGN / REASSIGN / MIXED / READONLY_BLOCKED
- **projection behavior**:
  - 本頁主情境無 metadata-only command 視角
- **blocked / forbidden behavior**:
  - 非 manager 直接 deny；scope mismatch 顯示 forbidden
- **exception handling notes**:
  - 不承接 revision exception，僅做 assignment
- **navigation / entry expectations**:
  - 由 UI-009/UI-010 導入，成功後回 detail 或 worklist
- **responsive structure notes**:
  - 先 summary+mode，再 reviewer/rereviewer 區塊
- **dependencies**:
  - assignment mode rules、permission matrix、assign/reassign command APIs
- **notes for future component extraction**:
  - reviewer picker / rereviewer picker 共享輸入結構可抽離
- **notes for future wireframe**:
  - 需畫 ASSIGN 與 READONLY_BLOCKED 對照

---

## A4. Action / Review Form

- **archetype name**: Action / Review Form
- **primary purpose**: reviewer/rereviewer command 作業（草稿、提交、revision、例外）
- **applicable pages**: UI-012（initial）, UI-013（rereview）
- **primary actor patterns**: REVIEWER / REREVIEWER
- **required regions**:
  - summary strip
  - mode indicator banner
  - main form region
  - commentary/attachment region
  - command action bar
  - version summary region
- **optional regions**:
  - UI-013 initial summary reference region
  - final decision summary region（UI-013）
- **action bar pattern**:
  - save draft / submit / revise command，明確語意分離
- **summary header pattern**:
  - case context + stage context + version context
- **status / banner pattern**:
  - draft banner
  - revision banner
  - readonly blocked banner
  - CLOSED exception banner（UI-013）
- **mode indicator pattern**:
  - NEW / EDIT_DRAFT / REVISION / READONLY_BLOCKED
  - UI-013 另含 CLOSED revision-exception 呈現
- **projection behavior**:
  - 主情境 FULL；不建立 metadata-only 作業頁
- **blocked / forbidden behavior**:
  - ownership/role/status 不符時禁用命令區並顯示原因
- **exception handling notes**:
  - O1-C：CLOSED 僅允許合法 rereview revision exception，不可 new submit、不可 reopen
- **navigation / entry expectations**:
  - 由 detail 或 history 導入；提交後回 detail/history
- **responsive structure notes**:
  - mode/banner 與 command bar 需維持高可見優先序
- **dependencies**:
  - state-machine revision guard、permission actor ownership、command API bindings
- **notes for future component extraction**:
  - summary strip / command bar / version summary 可形成跨 UI-012/013 shared structure
- **notes for future wireframe**:
  - 必畫 draft vs revision vs closed-exception 三種差異

---

## A5. History / Traceability

- **archetype name**: History / Traceability
- **primary purpose**: revision 演進閱讀、effective version 判讀、導頁至 command
- **applicable pages**: UI-014
- **primary actor patterns**: manager / auditor / reviewer / rereviewer / system admin
- **required regions**:
  - summary header
  - stage grouping tabs
  - timeline region
  - detail/compare summary region
  - navigation action region
- **optional regions**:
  - compare helper hint
  - actor-scope hint
- **action bar pattern**:
  - navigate-only；不直接提交 revision
- **summary header pattern**:
  - case summary + latest revision indicators
- **status / banner pattern**:
  - metadata-only projection indicator（paired with status banner 時併入 header）
  - closed/cancelled semantics banner
- **mode indicator pattern**:
  - ALL / INITIAL_ONLY / REREVIEW_ONLY
- **projection behavior**:
  - FULL：timeline + detail/compare 摘要
  - METADATA_ONLY：shared base + UI-014 extension，隱藏 full answer diff
- **blocked / forbidden behavior**:
  - scope 不符禁止載入 timeline
- **exception handling notes**:
  - O1-C：CLOSED + 合法 rereviewer 顯示條件式「前往複審修正」導頁
- **navigation / entry expectations**:
  - from list/detail；to review command page with revision intent
- **responsive structure notes**:
  - 小螢幕以 timeline 優先，detail/compare 區切換呈現
- **dependencies**:
  - revision timeline VM、submission detail binding、projection whitelist
- **notes for future component extraction**:
  - timeline card、version badge、compare summary 可形成共享結構
- **notes for future wireframe**:
  - 必畫 effective version 強調與 closed-exception 導頁行為

---

## 7. Page-to-Archetype Mapping

| Page | Primary Archetype | Secondary / Hybrid Behavior |
|---|---|---|
| UI-009 案件列表頁 | A1 List / Work Queue | 部分 row action 連到 A2/A3/A4/A5 |
| UI-010 案件詳情頁 | A2 Detail / Control Center | 導頁到 A3/A4/A5，含 metadata-only control center 變體 |
| UI-011 案件分派頁 | A3 Action / Assignment | 與 A2 共用 summary/header 結構 |
| UI-012 初審作業頁 | A4 Action / Review Form | 無 CLOSED exception；僅一般 revision/blocked |
| UI-013 複審作業頁 | A4 Action / Review Form | 含 CLOSED revision-exception 變體 |
| UI-014 Revision 歷程頁 | A5 History / Traceability | 條件式導向 A4（UI-013） |

---

## 8. Cross-Archetype Shared Structure

> 這裡只定 shared structure，不命名最終 reusable component。

```yaml
shared_structures:
  - page_summary_header
  - status_or_blocked_banner
  - mode_indicator_strip
  - action_zone
  - traceability_region
  - projection_notice_banner(metadata-only; inline indicator when paired with status banner)
```

### Shared structure 用途
- `page_summary_header`: A1/A2/A3/A4/A5 全覆蓋
- `status_or_blocked_banner`: A2/A3/A4/A5 核心
- `mode_indicator_strip`: A3/A4/A5 高頻
- `action_zone`: A1 row actions + A3/A4 commands + A5 navigate actions
- `traceability_region`: A2/A5 主區 + A4 版本摘要
- `projection_notice_banner`: A1/A2/A5 metadata-only 必備；在 A2/A5 與 status banner 共存時可降格為 header-level inline indicator

---

## 9. Known Variants（給 wireframe 的結構變體）

| Archetype | Variants to visualize in next step |
|---|---|
| A1 List / Work Queue | role-default tab variants、FULL vs METADATA_ONLY、forbidden/empty/error |
| A2 Detail / Control Center | FULL vs METADATA_ONLY、terminal blocked、role-limited action zone |
| A3 Action / Assignment | ASSIGN/REASSIGN/MIXED/READONLY_BLOCKED |
| A4 Action / Review Form | NEW/EDIT_DRAFT/REVISION/READONLY_BLOCKED、UI-013 CLOSED revision-exception |
| A5 History / Traceability | stage grouping variants、effective version emphasis、metadata-only、closed with conditional exception entry |

---

## 10. Pending Confirmation

僅保留本輪仍無法由現有 authority 明確支持者：

1. `INITIAL_REVIEW_COMPLETED` manager handoff commands 的最終 API 顯式化程度（影響 A2/A3 action zone 變體）。
2. lifecycle API 欄位粒度與排序（影響 A2 traceability region detail depth）。

> 已批准回寫的 O1-C / O2-A / O3-C **不列入 pending**。

---

## 11. Next-Step Handoff

### 給 `ui-component-map.md`
- 直接使用本文件的 5 個 archetype 與 shared structures 作為抽象邊界。
- 以 archetype required/optional regions 決定「共用 vs 頁面專屬」抽取優先序。

### 給 `ui-wireframe-brief.md`
- 直接使用 `Known Variants` 作為首輪 wireframe 必畫清單。
- 先畫結構差異（mode/projection/blocked/exception），後畫視覺細節。

