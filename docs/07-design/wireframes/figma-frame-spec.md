# Figma Wireframe Frame Spec — Cases Cluster P1

> D-032 規格變更（2026-11-18 補註）：下文涉及 O1-C、CLOSED 例外修正入口（UI-014 C22「前往複審修正（例外）」、`hasExceptionEntry`、UI-013 `CLOSED_REVISION_EXCEPTION` 模式及其 amber 提示）的內容，已被 D-032 取代，僅保留歷史依據，不作現行可實作規則。CLOSED 不得 revision／reopen；UI-013、UI-014 在 CLOSED 只顯示唯讀提示。應用程式已對齊：UI-013 CLOSED 唯讀（PR #39）；UI-014 移除 `hasExceptionEntry` 與 C22 例外入口（PR #83）。C22 的其他導頁設計（例如返回案件詳情）不在 D-032 撤銷範圍，目前應用程式沒有實作 C22。

> 本文件是可選用的 Figma translation spec。  
> 它描述若採 Figma 時應如何轉譯既有 wireframe artifacts；不是 Figma artifact 本身。  
> **不是 visual baseline，不是 implementation handoff，也不是預設執行方案。**

## 使用前提

1. Figma 不是預設方案；免費 / 本地方案優先。
2. 一般 wireframe / prototype 任務不需要先等批准，但若要使用 Figma / Figma MCP、付費能力，或遇到額度 / 權限 / API / MCP / 方案限制，必須先停下來回報。
3. 若使用 Figma MCP，必須先走免費 API / 免費能力。
4. markdown wireframe、ASCII、Mermaid、frame spec 都不能冒充 Figma artifact。
5. 若 Figma 不可行，優先 fallback 到本地 React / HTML low-fidelity prototype。

---

## 目錄

- [Figma 設置指南](#figma-設置指南)
- [Design Element Key（低保真慣例）](#design-element-key低保真慣例)
- [Frame 全覽清單](#frame-全覽清單)
- [UI-013 複審作業頁 — Frames](#ui-013-複審作業頁--frames)
- [UI-014 Revision 歷程頁 — Frames](#ui-014-revision-歷程頁--frames)
- [UI-010 案件詳情頁 — Frames](#ui-010-案件詳情頁--frames)
- [Flow Connection Spec](#flow-connection-spec)
- [High-Risk Visualization Notes](#high-risk-visualization-notes)
- [Deferred / Placeholder Notes](#deferred--placeholder-notes)
- [Open Questions for ui-design-decisions.md](#open-questions-for-ui-design-decisionsmd)

---

## Figma 設置指南

### Canvas 組織（Figma Pages）

| Figma Page 名稱 | 內容 |
|---|---|
| `01_UI-013_複審作業頁` | 5 frames（V1~V5） |
| `02_UI-014_Revision歷程頁` | 5 frames（V1/V3/V4/V5/V6） |
| `03_UI-010_案件詳情頁` | 5 frames（V1~V5） |
| `04_Flow_Navigation` | 跨頁導航連接圖 + High-risk annotations |

### Frame 命名規則

```
[PageID]-[VariantCode] [VariantName]
範例: 013-V1 NEW_REREVIEW
```

### Frame 尺寸

| 設定 | 值 |
|---|---|
| Width | 1440px |
| Height | 根據 content 自動調整（Auto layout vertical） |
| Background | #FFFFFF |
| Layout Grid | 12-column, 80px gutter, 120px margin |

### Region 標示慣例

每個 region 使用 Auto Layout Frame：

| 元素類型 | Fill | Stroke | 標示方式 |
|---|---|---|---|
| Region wrapper | #F9F9F9 | #E0E0E0 1px | 左上角標 region 代號（C01, C02...） |
| Content placeholder block | #EEEEEE | none | 標 content 類型（"caseId + projectName", "status badge"...） |
| Banner（blocked/exception） | #FFF3CD（warning）| #FFC107 1px | 標 banner type |
| Banner（exception/info） | #E3F2FD（info）| #90CAF9 1px | 標 banner type |
| Banner（error/terminal） | #FFEBEE（error）| #EF9A9A 1px | 標 banner type |
| Banner（projection notice）| #F3E5F5（metadata）| #CE93D8 1px | 標 C04 |
| Mode strip | #E8EAF6（mode）| #9FA8DA 1px | 標 mode text |
| Placeholder（deferred）| diagonal-hatch fill or #F5F5F5 | #BDBDBD 1px dashed | 標 "PLACEHOLDER / TBD" |
| Command bar（C16） | #FAFAFA | #B0BEC5 2px（top）| 注意: 不是普通 region |
| Navigate zone（C22）| #F5F5F5 | #CFD8DC 1px | 標 navigate-only |
| High-risk annotation | none | #FF5722 2px dashed | 外框標示，不填充 |

### Typography（低保真慣例）

| 用途 | Size / Weight |
|---|---|
| Region Label (C01, C02…) | 11px / Regular / #9E9E9E |
| Page / Frame Title | 20px / Bold |
| Section Title within Region | 14px / SemiBold |
| Placeholder text | 12px / Regular / #BDBDBD |
| Banner text | 13px / Regular / #212121 |
| Button label in C16 | 13px / Medium |
| Badge text | 11px / SemiBold |

---

## Design Element Key（低保真慣例）

### Banner 語意對應

| 語意 | Fill code | 使用時機 |
|---|---|---|
| WARNING（draft / revision 提示） | `#FFF3CD` | C02 in EDIT_DRAFT, REVISION |
| INFO-EXCEPTION（CLOSED exception） | `#E3F2FD` | C02 in CLOSED_REVISION_EXCEPTION |
| ERROR-BLOCKED（blocked / forbidden） | `#FFEBEE` | C02 in READONLY_BLOCKED, terminal CANCELLED |
| TERMINAL-NOTICE（CLOSED notice） | `#FFF8E1` | C02 in CLOSED (non-exception) |
| PROJECTION（metadata-only） | `#F3E5F5` | C04 all metadata-only variants |

**重要**: INFO-EXCEPTION（`#E3F2FD` 藍）vs ERROR-BLOCKED（`#FFEBEE` 紅）視覺差異必須在 wireframe 中已可辨識。這是 high-risk 目標之一。

### Button 狀態表示

| 狀態 | 視覺處理 |
|---|---|
| Primary CTA（提交） | 深色 fill rectangle + white text |
| Secondary CTA（儲存草稿） | bordered rectangle + dark text |
| Navigate CTA（前往...） | outlined / text-link style，不同於 primary |
| Disabled | Grey fill `#EEEEEE` + grey text `#9E9E9E` + `[disabled reason]` |
| Hidden | 不畫出 / 或用 dashed outline 標示 "HIDDEN in this variant" |

---

## Frame 全覽清單

### UI-013 複審作業頁

| Frame ID | Variant Name | Figma Page | Canvas Position |
|---|---|---|---|
| 013-V1 | NEW_REREVIEW | 01_UI-013 | X:0, Y:0 |
| 013-V2 | EDIT_DRAFT | 01_UI-013 | X:1520, Y:0 |
| 013-V3 | REVISION | 01_UI-013 | X:3040, Y:0 |
| 013-V4 | READONLY_BLOCKED | 01_UI-013 | X:4560, Y:0 |
| 013-V5 | CLOSED_REVISION_EXCEPTION | 01_UI-013 | X:6080, Y:0 |

### UI-014 Revision 歷程頁

| Frame ID | Variant Name | Figma Page | Canvas Position |
|---|---|---|---|
| 014-V1 | ALL | 02_UI-014 | X:0, Y:0 |
| 014-V3 | REREVIEW_ONLY | 02_UI-014 | X:1520, Y:0 |
| 014-V4 | FULL_COMPARE | 02_UI-014 | X:3040, Y:0 |
| 014-V5 | METADATA_ONLY_COMPARE | 02_UI-014 | X:4560, Y:0 |
| 014-V6 | CLOSED_EXCEPTION_ENTRY | 02_UI-014 | X:6080, Y:0 |

### UI-010 案件詳情頁

| Frame ID | Variant Name | Figma Page | Canvas Position |
|---|---|---|---|
| 010-V1 | FULL | 03_UI-010 | X:0, Y:0 |
| 010-V2 | ROLE_LIMITED | 03_UI-010 | X:1520, Y:0 |
| 010-V3 | METADATA_ONLY | 03_UI-010 | X:3040, Y:0 |
| 010-V4 | TERMINAL_BLOCKED | 03_UI-010 | X:4560, Y:0 |
| 010-V5 | CLOSED_REREVIEWER_HINT | 03_UI-010 | X:6080, Y:0 |

---

## UI-013 複審作業頁 — Frames

### Frame 013-V1: NEW_REREVIEW

> 第一次進入複審，尚無草稿或提交紀錄。

```
Frame: 013-V1 NEW_REREVIEW
Width: 1440px | Background: #FFFFFF
Regions (top → bottom):
```

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | `caseId` `projectName` `medicalRecordNo` `status badge: REREVIEW_IN_PROGRESS` `reviewer` `rereviewer` `dueDate` `version` |
| 2 | *(absent)* | C02 | — | **C02 absent** — 無 blocked/exception 狀態 |
| 3 | Mode Indicator Strip | C03 | ~32px | Mode label: `"NEW_REREVIEW"` fill: `#E8EAF6` |
| 4 | Form Summary Strip | C15 | ~48px | `stage: 複審` `assignedReviewerName` `version #1` |
| 5 | Initial Summary Reference | C18 | ~72px | `initialScore: XX.X` `reviewer: Dr.XXX` `submittedAt: YYYY-MM-DD` `revisionCount: 1` `→ link: 前往初審 / 查看歷程` |
| 6 | **Rereview Form Main** | *(page-owned)* | ~400px | Placeholder form: `[score sheet accordion — TBD]` `sections × N items` `每項: 題目 \| input field \| maxScore` |
| 7 | **Commentary Region** | *(page-owned)* | ~120px | `[comment textarea — empty]` `[attachment area — empty]` `[⚠ 提交後案件將結案 notice]` |
| 8 | Final Decision Summary | C19 | ~64px | `finalScore preview: —` `stage: REREVIEW` `★ C19 is page-owned` |
| 9 | **Command Bar ★ HIGH-RISK** | C16 | ~60px | **V1 action set:** `[儲存草稿 secondary]` `[提交複審 primary]` `[back secondary]` `[revise: HIDDEN]` |
| 10 | Version Summary | C17 | ~56px | `"尚無版本"` placeholder `→ link: revision 歷程（暫不適用）` |

**C16 high-risk annotation**: 橘色 dashed 外框圍住 C16，標示 "HIGH-RISK: 5-variant action set"。

---

### Frame 013-V2: EDIT_DRAFT

> 已存在複審草稿，使用者繼續填寫。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | 同 V1，`status: REREVIEW_IN_PROGRESS` |
| 2 | **Draft Banner ★** | C02 | ~44px | Fill: `#FFF3CD` (WARNING) `"目前為草稿模式，尚未正式提交"` `draft icon placeholder` |
| 3 | Mode Strip | C03 | ~32px | Mode: `"EDIT_DRAFT"` fill: `#E8EAF6` |
| 4 | Form Summary Strip | C15 | ~48px | `"草稿已保存: YYYY-MM-DD HH:mm"` |
| 5 | Initial Summary Reference | C18 | ~72px | 同 V1 |
| 6 | **Rereview Form Main** | *(page-owned)* | ~400px | `[帶入 draft 答案 — 已填欄位預填]` |
| 7 | **Commentary Region** | *(page-owned)* | ~120px | `[帶入 draft comment]` `[帶入 draft attachments]` |
| 8 | Final Decision Summary | C19 | ~64px | `finalScore preview: XX.X (draft 值)` |
| 9 | **Command Bar ★ HIGH-RISK** | C16 | ~60px | **V2 action set:** `[儲存草稿 secondary]` `[提交複審 primary]` `[back secondary]` `[revise: HIDDEN]` |
| 10 | Version Summary | C17 | ~56px | `submissionId: draft-xxx` `revisionNo: 0 (draft)` `stage: REREVIEW` |

**diff from V1**: C02 出現 draft banner（`#FFF3CD`）；C17 顯示 draft submission；form 預填。

---

### Frame 013-V3: REVISION

> 基於已提交版本建立新 revision，案件仍進行中。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | `status: REREVIEW_IN_PROGRESS` |
| 2 | **Revision Banner ★** | C02 | ~52px | Fill: `#FFF3CD` (WARNING) `"目前為複審修正模式，將建立新 revision，不覆寫既有提交"` |
| 3 | Mode Strip | C03 | ~32px | Mode: `"REVISION"` fill: `#E8EAF6` |
| 4 | Form Summary Strip | C15 | ~48px | `"基於 revision v1 → new revision"` |
| 5 | Initial Summary Reference | C18 | ~72px | 同 V1 |
| 6 | **Rereview Form Main** | *(page-owned)* | ~400px | `[帶入上一版答案（可編輯）]` `[變更項目 indicator placeholder]` |
| 7 | **Commentary Region** | *(page-owned)* | ~120px | `[帶入上一版 content，可修改]` |
| 8 | Final Decision Summary | C19 | ~64px | `"base revisionNo: v1 → new revision pending"` |
| 9 | **Command Bar ★ HIGH-RISK** | C16 | ~60px | **V3 action set (different button label):** `[儲存草稿 secondary]` `[提交複審修正 primary ← label changed]` `[back secondary]` `[separate draft save]` |
| 10 | Version Summary | C17 | ~56px | `basedOn: submissionId-xxx / rereview v1` `→ new revision pending` |

**diff from V2**: button label 是「提交複審修正」非「提交複審」；C15 顯示 base→new；C17 顯示 basedOn 關係。

---

### Frame 013-V4: READONLY_BLOCKED

> 非法進入：已取消、非本人、狀態不符。所有命令禁用。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | `status: CANCELLED` or scope mismatch |
| 2 | **Blocked Banner ★** | C02 | ~60px | Fill: `#FFEBEE` (ERROR-BLOCKED) `"你不是本案件的複審醫師" OR "此案件已取消，不可執行複審作業" OR "案件尚未進入複審"` `blocked reason label` |
| 3 | Mode Strip | C03 | ~32px | Mode: `"READONLY_BLOCKED"` fill: `#FFEBEE` text: `#C62828` |
| 4 | Form Summary Strip | C15 | ~48px | `readonly: 無法執行作業` |
| 5 | Initial Summary Reference | C18 | ~72px | `若有初審資料: readonly summary` `無法操作` |
| 6 | **Rereview Form Main** | *(page-owned)* | ~300px | Fill: `#F5F5F5` `[DISABLED — 灰化，欄位不可填寫]` `"目前無法執行複審作業"` |
| 7 | **Commentary Region** | *(page-owned)* | ~80px | Fill: `#F5F5F5` `[DISABLED]` |
| 8 | Final Decision Summary | C19 | — | **absent** |
| 9 | **Command Bar ★ HIGH-RISK** | C16 | ~60px | **V4 action set — ALL BLOCKED:** `[儲存草稿: DISABLED grey]` `[提交複審: DISABLED grey]` `[back secondary ✓]` `"blocked reason text"` |
| 10 | Version Summary | C17 | ~56px | `readonly 版本摘要（若有）` |

**diff from V3 — critical**: C02 fill `#FFEBEE` (紅) vs V5's `#E3F2FD` (藍)；C03 red mode text；C16 全 DISABLED；C19 absent。

---

### Frame 013-V5: CLOSED_REVISION_EXCEPTION ⭐ HIGH-RISK

> 案件 CLOSED，actor 是原 rereviewer → revise 允許（例外），新提交禁止。  
> **必須與 V4 READONLY_BLOCKED 在視覺上明確可辨。**

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | `status: CLOSED` |
| 2 | **Exception Banner ★ CRITICAL** | C02 | ~72px | Fill: `#E3F2FD` **(INFO-EXCEPTION — 藍，vs V4 紅)** `"此案件已結案，新提交已禁止。"` `"合法例外：你可以發起複審修正（revision），此操作不開啟新提交。"` `info icon placeholder` `★ 文案與 V4 BLOCKED 有本質差異` |
| 3 | Mode Strip | C03 | ~36px | Mode: `"CLOSED — 複審修正例外"` fill: `#E3F2FD` text: `#1565C0` `★ 必須不同於 V4 的 READONLY_BLOCKED` |
| 4 | Form Summary Strip | C15 | ~48px | `"CLOSED case: 複審例外模式"` `現有版本: rereview vN (effective)` |
| 5 | Initial Summary Reference | C18 | ~72px | `readonly reference — FULL` |
| 6 | **Rereview Form Main** | *(page-owned)* | ~400px | `[帶入最後有效版複審答案（可編輯用於 revision）]` `★ 可編輯（vs V4 禁用）` |
| 7 | **Commentary Region** | *(page-owned)* | ~120px | `[可修改 — revision context]` |
| 8 | Final Decision Summary | C19 | ~64px | `base = last effective revision vN` `→ new revision will be created` |
| 9 | **Command Bar ★ HIGH-RISK CRITICAL** | C16 | ~72px | **V5 action set — KEY DIFFERENCES:** `[儲存草稿 secondary ✓]` `[提交複審修正（例外）primary — ENABLED ✓]` `[提交複審: DISABLED grey + "案件已結案，不可新提交"]` `[back secondary ✓]` `★ 提交複審 = DISABLED; 提交修正 = ENABLED` |
| 10 | Version Summary | C17 | ~64px | `basedOn: last effective submissionId / rereview vN` `CLOSED case effective version context` |

**V4 vs V5 必看對比 annotation (放在兩 frame 之間的 canvas 上)**:

```
差異對照annotation:
                    V4 READONLY_BLOCKED    V5 CLOSED_REVISION_EXCEPTION
C02 banner fill:    #FFEBEE (紅-錯誤)       #E3F2FD (藍-資訊/例外)
C03 mode text:      READONLY_BLOCKED        CLOSED — 複審修正例外
C16 submit:         DISABLED               DISABLED ← 兩個都禁止新提交
C16 revise:         HIDDEN                 ENABLED ← 這是核心差異
C16 draft:          DISABLED               ENABLED
Form main:          灰化禁用               可編輯（revision 用）
```

---

## UI-014 Revision 歷程頁 — Frames

### Frame 014-V1: ALL

> HOSPITAL_MANAGER 或 AUDITOR 進入，全階段 timeline 預設。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | `caseId` `projectName` `status: CLOSED` `reviewer` `rereviewer` `initialRevisionCount: 2` `rereviewRevisionCount: 1` |
| 2 | *(absent)* | C04 | — | **C04 absent** — FULL projection |
| 3 | Optional CLOSED notice | C02 | ~40px | Fill: `#FFF8E1` (TERMINAL-NOTICE) `"此案件已結案"` *(若案件 CLOSED)* |
| 4 | Mode Strip | C03 | ~32px | Mode: `"ALL"` fill: `#E8EAF6` |
| 5 | **Stage Group Tabs** | *(C20 driving)* | ~48px | `[★ ALL selected]` `[初審 INITIAL]` `[複審 REREVIEW]` |
| 6 | **Revision Timeline ★** | C20 | ~240px | **Timeline table (ALL stages):** `Stage \| Rev# \| SubID \| SubmittedAt \| By \| Status` `INITIAL \| v1 \| sub-001 \| 2024-01-10 \| 初審醫師 \| [Superseded badge grey]` `INITIAL \| v2 \| sub-002 \| 2024-01-15 \| 初審醫師 \| [★ Current badge 強調色]` `REREVIEW \| v1 \| sub-003 \| 2024-01-20 \| 複審醫師 \| [★ Current badge 強調色]` `★ Current badge 用 强調色; Superseded badge 用次要色` |
| 7 | **Compare Summary ★ HIGH-RISK** | C21 | ~200px | **FULL mode, 預設單版摘要:** `選版: [單版摘要 selected] [雙版比較]` `submissionId: sub-002 \| stage: INITIAL \| rev: v2` `totalScore: 92.5 \| submittedAt: 2024-01-15` `comment摘要: 摘要文字...` `answer摘要: N題 [依權限]` |
| 8 | **Navigate Zone ★ HIGH-RISK** | C22 | ~80px | Fill: `#F5F5F5` `★ 此區是 navigate-only，不是 submit bar` `[返回案件詳情 outline-button]` `[查看初審作業 text-link]` `[查看複審作業 text-link]` |

**C20 annotation**: 橘色 dashed 框，標 "High-risk: effective version must be visually dominant"  
**C22 annotation**: 橘色 dashed 框，標 "High-risk: navigate-only — NOT submit bar"

---

### Frame 014-V3: REREVIEW_ONLY

> REREVIEWER 預設切換，只看複審階段。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | 同 V1 |
| 2 | *(absent)* | C04 | — | absent |
| 3 | Optional CLOSED notice | C02 | ~40px | 同 V1（若 CLOSED） |
| 4 | Mode Strip | C03 | ~32px | Mode: `"REREVIEW_ONLY"` |
| 5 | **Stage Group Tabs** | *(C20 driving)* | ~48px | `[ALL]` `[初審 INITIAL]` `[★ 複審 REREVIEW selected]` |
| 6 | **Revision Timeline ★** | C20 | ~160px | **REREVIEW only:** `REREVIEW \| v1 \| sub-003 \| 2024-01-20 \| 複審醫師 \| [★ Current 強調色]` *(INITIAL rows 不顯示)* |
| 7 | **Compare Summary ★** | C21 | ~200px | **FULL mode, REREVIEW compare only:** `選版: [單版摘要] [雙版比較]` `scope: REREVIEW stage only` |
| 8 | **Navigate Zone ★** | C22 | ~80px | `[返回案件詳情]` `[前往複審作業 — 若合法 rereviewer + non-terminal]` `若 CLOSED + 合法: [前往複審修正（CLOSED 例外）navigate-link]` |

**diff from V1**: Tab 選中 REREVIEW；C20 只顯示 REREVIEW rows；C22 顯示複審作業入口。

---

### Frame 014-V4: FULL_COMPARE

> 使用者展開雙版比較（FULL projection，兩版均選中）。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | 同 V1 |
| 2 | *(absent)* | C04 | — | absent |
| 3 | *(absent or minimal)* | C02 | — | absent |
| 4 | Mode Strip | C03 | ~32px | Mode based on tab |
| 5 | **Stage Group Tabs** | *(C20 driving)* | ~48px | `[ALL / INITIAL / REREVIEW]` |
| 6 | **Revision Timeline ★** | C20 | ~240px | **兩版 row 均 highlighted:** `REREVIEW \| v1 \| sub-003 \| ... \| [selected for compare: BASE]` `REREVIEW \| v2 \| sub-004 \| ... \| [selected for compare: TARGET] [★ Current badge]` `選版指示: "BASE v1 → TARGET v2 比較中"` |
| 7 | **Compare Summary ★ HIGH-RISK EXPANDED** | C21 | ~280px | **FULL compare panel EXPANDED:** `選版: [單版摘要] [★ 雙版比較 selected]` `BASE: REREVIEW v1 → TARGET: REREVIEW v2` `─────────────────────────────────` `題目變更: 3 題` `分數差異: 88.5 → 91.0 (+2.5)` `評語變更: 已更新` `附件變更: 新增 1 筆` `[view detail — placeholder]` |
| 8 | **Navigate Zone ★** | C22 | ~80px | `[返回案件詳情]` ← minimal navigate only |

**HR annotation on C21**: 橘色 dashed, 標 "HIGH-RISK: Full compare content — MUST be distinct from V5 METADATA_ONLY"

---

### Frame 014-V5: METADATA_ONLY_COMPARE ⭐ HIGH-RISK

> System Admin 進入，projection = metadata-only。Compare 降級顯示。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | **metadata-only**: `caseId` `caseStatus` `projectName` `hospitalId` `reviewerId` `rereviewerId` `updatedAt` ***(UI-014 extension)*** `stage` `revisionNo` `submittedAt` `submittedBy` `currentEffective` `[clinical detail: HIDDEN]` |
| 2 | **Projection Banner ★** | C04 | ~44px | Fill: `#F3E5F5` (PROJECTION-metadata) `"目前為 metadata-only 視角，完整答案差異已遮罩"` |
| 3 | *(absent)* | C02 | — | absent |
| 4 | Mode Strip | C03 | ~32px | Mode: `"METADATA_HISTORY"` fill: `#F3E5F5` text: `#6A1B9A` |
| 5 | **Stage Tabs** | *(C20 driving)* | ~48px | `[ALL / INITIAL / REREVIEW]` |
| 6 | **Revision Timeline ★ degraded** | C20 | ~200px | **metadata-only timeline:** `stage \| revisionNo \| submittedAt \| actorId \| status` `INITIAL \| v1 \| 2024-01-10 \| reviewer-001 \| [Superseded]` `INITIAL \| v2 \| 2024-01-15 \| reviewer-001 \| [★ Current]` `REREVIEW \| v1 \| 2024-01-20 \| rereviewer-001 \| [★ Current]` `[score: HIDDEN]` `[answer content: HIDDEN]` |
| 7 | **Compare Summary ★ HIGH-RISK DEGRADED** | C21 | ~200px | **METADATA_ONLY compare — KEY DIFF:** `選版模式介面 hidden (比較不可用)` `─────────────────────────────────` `submissionId: sub-002` `stage: INITIAL` `revisionNo: v2` `submittedAt: 2024-01-15` `actorId: reviewer-001` `status: Current` `─────────────────────────────────` `[答案摘要已遮罩]` `[評語已遮罩]` `[詳細比較需完整視角]` `★ MUST NOT show: score diff, answer diff, comment diff` |
| 8 | **Navigate Zone ★** | C22 | ~64px | `[返回案件詳情]` `[★ 無作業頁入口 — sysadmin 不作業]` |

**C04+C21 annotation**: 橘色 dashed, 標 "HIGH-RISK PAIR: C04 降級提示 ↔ C21 降級 compare 必須一致"

**V4 vs V5 對比 annotation (canvas 上):**
```
V4 FULL_COMPARE                    V5 METADATA_ONLY_COMPARE
C04: absent                         C04: VISIBLE (purple #F3E5F5)
C03: normal mode                    C03: METADATA_HISTORY (purple)
C20: score/by 可見                  C20: actorId 只顯示 ID, score hidden
C21: score diff, answer diff 可見   C21: 全遮罩, 不顯示 diff
C22: reviewer navigate 可用         C22: 只有返回, 無作業入口
```

---

### Frame 014-V6: CLOSED_EXCEPTION_ENTRY ⭐ HIGH-RISK

> 案件 CLOSED，actor 是合法 rereviewer。C22 顯示條件式例外導頁入口。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | `status: CLOSED` |
| 2 | *(absent)* | C04 | — | absent |
| 3 | **CLOSED Notice** | C02 | ~44px | Fill: `#FFF8E1` (TERMINAL-NOTICE) `"此案件已結案"` |
| 4 | Mode Strip | C03 | ~32px | Mode: `"HISTORY_REREVIEW_ONLY"` |
| 5 | **Stage Tabs** | *(C20 driving)* | ~48px | `[ALL] [初審] [★ 複審 REREVIEW selected]` |
| 6 | **Revision Timeline ★** | C20 | ~180px | **CLOSED 狀態，effective version 清晰:** `REREVIEW \| v1 \| sub-003 \| 2024-01-20 \| 複審醫師 \| [★ Current = effective]` `[CLOSED badge on case context]` `★ effective 版本必須一眼可辨` |
| 7 | **Compare Summary** | C21 | ~200px | **FULL readonly:** `submissionId, score, comment（readonly）` `比較功能: visible but not changeable` |
| 8 | **Navigate Zone ★ HIGH-RISK** | C22 | ~120px | `[返回案件詳情]` `──────────────────────────` `★ CONDITIONAL EXCEPTION ENTRY (O1-C)` `[前往複審修正（CLOSED 例外）text-link or outlined-button]` `label: "前往複審修正"` `★ 必須是 navigate link，不是 primary submit button` `★ 指向 UI-013 CLOSED_REVISION_EXCEPTION 模式` `★ 僅 CLOSED + 合法 rereviewer 時顯示` |

**C22 annotation**: 橘色 dashed 外框，標 "HIGH-RISK: EXCEPTION ENTRY — navigate to UI-013, NOT submit here"

---

## UI-010 案件詳情頁 — Frames

### Frame 010-V1: FULL

> HOSPITAL_MANAGER 或 AUDITOR，完整視角，控制中心。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | `caseId` `projectName` `medicalRecordNo` `hospitalId` `status badge: REREVIEW_IN_PROGRESS` `reviewer: Dr.A` `rereviewer: Dr.B` `version` |
| 2 | *(absent)* | C04 | — | absent |
| 3 | *(absent)* | C02 | — | absent |
| 4 | Mode Strip | C03 | ~32px | Mode: `"FULL_CONTROL_CENTER"` fill: `#E8EAF6` |
| 5 | **Case Action Panel ★** | C08 | ~140px | `★ NAVIGATE-ONLY — NO submit CTA` `[前往分派作業 outline-button]` `[前往初審作業 outline-button]` `[前往複審作業 outline-button]` `[前往 Revision 歷程 outline-button]` `全部語意: 前往... (not: 提交/執行)` |
| 6 | Traceability Summary | C09 | ~96px | `batchId: batch-001` `samplingRule: 規則A` `samplingDate: 2024-01-01` `assignmentHistory: reviewerId/rereviewerId/dates` |
| 7 | Initial Review Summary | C10 | ~80px | `initialScore: 92.5` `submittedAt: YYYY-MM-DD` `revisionCount: 2` `comment摘要: 文字...` |
| 8 | Rereview Summary | C11 | ~80px | `rereviewScore: 88.0` `submittedAt: YYYY-MM-DD` `finalDecision: –` *(若尚無複審: absent)* |
| 9 | **Revision Lifecycle Entry** | C24 | ~64px | Fill: diagonal-hatch `#F5F5F5` dashed border `"★ PLACEHOLDER: RevisionLifecycleEntryArea / TBD"` |

**C08 annotation**: 橘色 dashed, 標 "HIGH-RISK: Navigate-only zone — NOT C16 command bar"

---

### Frame 010-V2: ROLE_LIMITED

> REVIEWER 或 REREVIEWER，只看與自己角色相關的作業入口。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | 同 V1 但 scope within assigned |
| 2 | *(absent)* | C04 | — | absent |
| 3 | *(absent)* | C02 | — | absent |
| 4 | Mode Strip | C03 | ~32px | Mode: `"ROLE_LIMITED_CONTROL_CENTER"` |
| 5 | **Case Action Panel ★ role-filtered** | C08 | ~120px | `★ NAVIGATE-ONLY` **REVIEWER sub-variant:** `[前往初審作業 ✓ (assigned + status match)]` `[前往 Revision 歷程 ✓]` `[分派作業: HIDDEN]` `[複審作業: HIDDEN]` **OR REREVIEWER sub-variant:** `[前往複審作業 ✓]` `[前往 Revision 歷程 ✓]` `[分派作業: HIDDEN]` `[初審作業: HIDDEN or read-only link]` |
| 6 | Traceability Summary | C09 | ~80px | `limited: own assignment info only` |
| 7 | Initial Review Summary | C10 | ~80px | REVIEWER: FULL; REREVIEWER: limited |
| 8 | Rereview Summary | C11 | ~80px | REREVIEWER: FULL; REVIEWER: limited |
| 9 | **Revision Lifecycle Entry** | C24 | ~64px | `PLACEHOLDER / TBD` |

**diff from V1**: C08 action set 依 role 篩選；C09/C10/C11 visibility 差異；annotate "REVIEWER variant" vs "REREVIEWER variant" 兩個 sub-labels。

---

### Frame 010-V3: METADATA_ONLY ⭐

> System Admin，projection = metadata-only。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | **metadata-only:** visible: `caseId` `caseStatus` `projectName` `hospitalId` `reviewerId` `rereviewerId` `updatedAt` ext: `patientAge` `patientGender` `admissionDate` `dischargeDate` hidden: clinical detail |
| 2 | **Projection Banner ★** | C04 | ~44px | Fill: `#F3E5F5` `"目前為 metadata-only 視角，部分欄位已遮罩"` |
| 3 | *(absent or minimal)* | C02 | — | absent |
| 4 | Mode Strip | C03 | ~32px | Mode: `"METADATA_CONTROL_CENTER"` fill: `#F3E5F5` text: `#6A1B9A` |
| 5 | **Case Action Panel ★ sysadmin** | C08 | ~80px | `★ NAVIGATE-ONLY, limited` `[前往 Revision 歷程 ✓ (metadata-only)]` `[分派作業: HIDDEN]` `[初審作業: HIDDEN]` `[複審作業: HIDDEN]` `text: "系統管理員可查看歷程（metadata-only 視角）"` |
| 6 | Traceability Summary | C09 | ~80px | `metadata fields only: batchId, samplingDate` |
| 7 | Initial Review Summary | C10 | ~80px | `structure visible, content masked` `"答案摘要已遮罩"` `score: HIDDEN` |
| 8 | Rereview Summary | C11 | ~80px | same masking as C10 |
| 9 | **Revision Lifecycle Entry** | C24 | ~64px | `PLACEHOLDER / TBD` |

**diff from V1**: C04 visible；C08 minimal（只剩歷程入口）；C10/C11 masked；C03 metadata style。

---

### Frame 010-V4: TERMINAL_BLOCKED

> 案件 CLOSED 或 CANCELLED，所有作業入口關閉。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | `status: CLOSED` or `CANCELLED` |
| 2 | *(absent or by role)* | C04 | — | 若 sysadmin: C04 visible |
| 3 | **Terminal Notice ★** | C02 | ~52px | CLOSED: Fill `#FFF8E1` `"案件已結案，不可再執行審查作業，僅可查看歷程"` CANCELLED: Fill `#FFEBEE` `"案件已取消"` |
| 4 | Mode Strip | C03 | ~32px | Mode: `"TERMINAL"` |
| 5 | **Case Action Panel ★ minimal** | C08 | ~80px | `★ ALL 作業入口 HIDDEN or DISABLED` `[前往 Revision 歷程 ✓ — still navigable]` `"此案件已達終態，無法再執行審查作業"` |
| 6 | Traceability Summary | C09 | ~96px | FULL readonly |
| 7 | Initial Review Summary | C10 | ~80px | FULL readonly |
| 8 | Rereview Summary | C11 | ~80px | FULL readonly |
| 9 | **Revision Lifecycle Entry** | C24 | ~64px | `PLACEHOLDER / TBD` |

---

### Frame 010-V5: CLOSED_REREVIEWER_HINT

> 案件 CLOSED，合法 rereviewer 進入，UI-010 不承擔命令，只顯示導頁提示。

| # | Region | Component | Height | Content / Notes |
|---|---|---|---|---|
| 1 | Case Summary Header | C01 | ~80px | `status: CLOSED` |
| 2 | *(absent)* | C04 | — | absent |
| 3 | **CLOSED Notice** | C02 | ~44px | Fill: `#FFF8E1` `"案件已結案"` |
| 4 | Mode Strip | C03 | ~32px | Mode: `"FULL_CONTROL_CENTER"` or `"ROLE_LIMITED"` depending on role |
| 5 | **Case Action Panel ★ hint-only** | C08 | ~108px | `[前往 Revision 歷程 ✓]` `──────────────────────────` `★ 提示文字（不是 CTA button）:` `"如需複審修正，請從 Revision 歷程頁進入"` `★ NO direct revision CTA here` `★ NO "提交複審修正" button` |
| 6–9 | Traceability + Summaries | C09/C10/C11/C24 | ~300px | readonly summaries + placeholder |

**diff from V4**: C02 CLOSED（非 CANCELLED）；C08 包含提示文字而非完全封鎖；C08 不出現直接修正 CTA（責任在 UI-014）。

---

## Flow Connection Spec

> 在 Figma 的「04_Flow_Navigation」Page 建立連接圖，標示跨頁導航。

### Primary Navigation Flows

```
UI-009 案件列表頁
  └─ row: case detail → UI-010 V1/V2/V3/V4/V5 [by role + status]

UI-010 案件詳情頁
  ├─ C08: [前往分派作業] → UI-011
  ├─ C08: [前往初審作業] → UI-012
  ├─ C08: [前往複審作業] → UI-013 V1/V2/V3 [by status]
  └─ C08: [前往 Revision 歷程] → UI-014 V1/V3/V4/V5 [by role]

UI-014 Revision 歷程頁
  ├─ C22: [查看初審作業] → UI-012 [REVIEWER only, non-terminal]
  ├─ C22: [查看複審作業] → UI-013 V3 REVISION [REREVIEWER, non-terminal]
  └─ C22: [前往複審修正（CLOSED 例外）] → UI-013 V5 CLOSED_REVISION_EXCEPTION ★

UI-013 複審作業頁
  ├─ C16: [提交複審 / 提交複審修正] → UI-010 V4 TERMINAL_BLOCKED (CLOSED case)
  ├─ C16: [back] → UI-014 (進入來源) or UI-010
  └─ C18: [→ 查看歷程] → UI-014
```

### Exception Flow (O1-C)

```
UI-010 V5 CLOSED_REREVIEWER_HINT
  └─[前往 Revision 歷程 hint text] → UI-014 V6 CLOSED_EXCEPTION_ENTRY
         └─ C22: [前往複審修正（CLOSED 例外）navigate-link]
                └─ → UI-013 V5 CLOSED_REVISION_EXCEPTION
                       └─ C16: [提交複審修正（例外）]
                              └─ → UI-010 V4 TERMINAL_BLOCKED (still CLOSED)
```

**Flow annotation**: 用 Figma prototype connection arrow 連接 frames，exception flow 用橘色箭頭標示。

### Navigation Policy Reminder（在 Flow Canvas 上標示）

| 頁面 | 角色 | 禁止事項 |
|---|---|---|
| UI-010 | all | 無法直接 submit/revise |
| UI-014 | all | 無法直接 submit/revise；C22 = navigate only |
| UI-013 V5 | rereviewer | submit new rereview = DISABLED |

---

## High-Risk Visualization Notes

### HR-1 C16 ReviewCommandBar（UI-013）

**Figma 執行指引：**

1. C16 區域必須用明確的上邊框（2px `#B0BEC5`）與上方 form 區域分離
2. 每個 variant 的 C16 截圖必須能放在一起對比，一眼看出差異
3. 在 V4/V5 並排時，用一個 annotation table layer：

   | | V4 READONLY_BLOCKED | V5 CLOSED_EXCEPTION |
   |---|---|---|
   | 儲存草稿 | ❌ DISABLED | ✅ ENABLED |
   | 提交複審 | ❌ DISABLED | ❌ DISABLED (different reason) |
   | 提交修正 | ❌ HIDDEN | ✅ ENABLED |
   | BACK | ✅ | ✅ |

4. V5 的「提交複審: DISABLED」必須帶 reason text："案件已結案，不可新提交"

### HR-2 C21 RevisionCompareSummaryRegion（UI-014）

**Figma 執行指引：**

1. V4 FULL_COMPARE 與 V5 METADATA_ONLY 的 C21 必須並排，視覺差異要求：
   - V4: 顯示 score diff number, answer diff count, comment change 文字
   - V5: 這些欄位全用 `[已遮罩]` 灰色矩形填充，加 "詳細比較需完整視角" 說明
2. V5 的 C04 (projection banner) 必須在 C21 上方，語意一致性一眼可辨
3. C21 的降級程度目前為 first-pass 佔位；最終資訊密度細節待 ui-design-decisions.md

### HR-3 C22 RevisionNavigationActionZone（UI-014）

**Figma 執行指引：**

1. C22 的視覺語言必須與 C16（UI-013）明確不同：
   - C22 = outlined / text-link style，用 secondary 語意控件
   - C16 = primary / secondary button，帶 destructive/danger 可能性
2. V6 CLOSED_EXCEPTION_ENTRY 的例外 navigate link 必須有 annotation：
   - `"→ 導向 UI-013 V5，非在本頁提交"`
3. Figma prototype connection 用 outlined arrow 連到 013-V5，不用 fill arrow

---

## Deferred / Placeholder Notes

| Region | Page | Figma 畫法 | 下一步觸發事件 |
|---|---|---|---|
| C24 RevisionLifecycleEntryArea | UI-010 | diagonal-hatch fill, dashed border, `"PLACEHOLDER: C24 RevisionLifecycleEntryArea — TBD"` | lifecycle API 欄位粒度確認 |
| Score sheet form items | UI-013 | `#EEEEEE` fill blocks, `"Score Sheet Item Placeholder × N"` | implementation planning |
| C21 metadata masking density | UI-014 | `[已遮罩]` blocks, note: "masking density TBD" | O3 whitelist detail confirmation |
| C09 assignment history detail | UI-010 | basic fields only, `"詳細邏輯 TBD"` | assignment API spec update |
| C16 sticky layout | UI-013 | static position for first-pass | ui-design-decisions.md |

---

## Open Questions for ui-design-decisions.md

> 以下問題不得在本輪 wireframe 自行拍板。待收集後統一進 `ui-design-decisions.md`（下一步建立）。

### Layout / Visual Priority（3 題）

**DD-001**: **C16 sticky / fixed 策略**  
場景：UI-013 長表單 scroll 時，command bar 是否固定在 viewport 底部？  
選項 A: sticky bottom（永遠可見）→ 確認可用性；選項 B: scroll with form（保持版面簡潔）  
影響：Figma layout / Angular scroll container 設計

**DD-002**: **C02 + C04 同出現時的版面排列**  
場景：UI-010 V3 sysadmin + CLOSED（metadata-only + terminal state）  
選項 A: 兩個 banner 分別堆疊；選項 B: 合併為複合 banner  
影響：C02/C04 region wrapper 的 merge 策略

**DD-003**: **C02 vs C03 視覺優先序**  
場景：blocked / exception 時 banner 與 mode strip 同時出現  
問題：哪個在上方，哪個的 color weight 更強？  
影響：layout 固定順序（目前 first-pass 是 C02 先、C03 次）

### Exception UX（3 題）

**DD-004**: **V4 READONLY_BLOCKED vs V5 CLOSED_EXCEPTION 視覺語意分化深度**  
場景：C02 banner fill 目前 wireframe 用 `#FFEBEE` vs `#E3F2FD` 區分  
問題：是否需要獨立 design token（`--banner-blocked` vs `--banner-exception`）？icon 類型？  
影響：design system token 設計

**DD-005**: **C03 mode strip 文案 — CLOSED_REVISION_EXCEPTION**  
場景：UI-013 V5 的模式標示文案  
候選 A: `"複審修正例外"` B: `"CLOSED 例外模式"` C: `"案件結案 — 修正入口開啟"`  
影響：UX writing decision

**DD-006**: **C22 例外 navigate entry 控件類型**  
場景：UI-014 V6 的「前往複審修正」  
候選 A: `outlined button`（更顯眼）B: `text-link`（更低調，符合 navigate-only 語意）  
影響：C22 控件規格，影響「不像 submit bar」的視覺效果

### Information Architecture（3 題）

**DD-007**: **C21 METADATA_ONLY compare 遮罩層級**  
場景：UI-014 V5 的 C21 降級程度  
候選 A: 顯示 metadata 欄位 + `[已遮罩]` placeholders（知道有 diff 但看不到）  
候選 B: 完全不顯示 compare 面板（直接 "比較需完整視角"）  
影響：sysadmin 的可感知資訊量

**DD-008**: **C08 action 可見性 vs 可用性分離策略**  
場景：UI-010 role 合法但 status 不允許（例如 reviewer + case PENDING_INITIAL_REVIEW but not assigned）  
候選 A: `HIDDEN`（減少 noise）B: `visible + disabled + reason tooltip`（提示可能性）  
影響：action policy rendering；影響使用者對「我能做什麼」的認知

**DD-009**: **C10/C11 cross-role summary visibility（need authority confirmation）**  
場景：UI-010 V2 ROLE_LIMITED 時，REVIEWER 能看 rereview summary (C11) 嗎？  
問題：permission-matrix 有 Y(assigned scope) 但 C11 的 rereview content 屬 REREVIEWER scope  
阻塞：需 permission-matrix authority 確認再定稿

### Visual Language（2 題）

**DD-010**: **C20 effective version badge design token**  
場景：UI-014 Current vs Superseded badge  
問題：「強調色」和「次要色」的 semantic token 是什麼？brand-primary? success? 或獨立 revision-token?  
影響：badge 顏色系統

**DD-011**: **UI-014 stage tab 預設切換策略**  
場景：不同 role 進入 UI-014 時的 tab 預設  
問題：是 routing-based (URL param) 還是 role-inferred default？  
影響：Angular routing + ViewModel 設計

### Deferred Reactivation（2 題）

**DD-012**: **C24 RevisionLifecycleEntryArea 細分結構**  
阻塞：lifecycle API 欄位粒度確認後  
問題：單一 entry zone 還是分 sub-zones？placement（C11 之後 vs 側欄）？

**DD-013**: **C16 button label 統一**  
場景：V3 REVISION「提交複審修正」vs V5 CLOSED_EXCEPTION「提交複審修正（例外）」  
問題：是否需要語意更強的區分？label 長度對版面的影響？  
影響：C16 button width / layout

---

*frame spec version: 1.0 — cases cluster P1 first-pass*  
*source: wireframe markdown 產物 + authority inputs*  
*next step: create `ui-design-decisions.md` using DD-001 ~ DD-013*
