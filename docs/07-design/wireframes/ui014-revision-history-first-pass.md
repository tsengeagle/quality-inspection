# UI-014 Revision 歷程頁 — First-Pass Wireframe

> First-pass low-fidelity structure. Not visual baseline. Not Angular code.  
> This is a **history / traceability page** — it does NOT submit commands.  
> All action zones are navigate-only. Exception entry is conditional and navigate-only.

---

## 1. Page Identity

| 欄位 | 值 |
|---|---|
| pageId | `UI-014` |
| pageName | Revision 歷程頁 |
| archetype | A5 History / Traceability |
| primary user goal | 閱讀 revision 演進、辨識 effective version、條件式導向 revision command |
| primary actors | HOSPITAL_MANAGER / AUDITOR / REVIEWER / REREVIEWER |
| page type | history / traceability — navigate-only |

**明確禁止**：本頁不出現直接提交按鈕；C22 是 navigate zone，不是 command bar。

---

## 2. Source Alignment

| 類型 | 來源 |
|---|---|
| Archetype | `ui-layout-archetypes.md` A5 History / Traceability |
| Component candidates | C01 / C02 / C03 / C04 / C09 / C20 / C21 / C22（component-map.md） |
| Page spec authority | `ui-page-spec-ui014revision歷程頁.md` |
| State / action policy | `ui-state-actions-matrix.md` §4.6 |
| Domain state | `state-machine.md` §3.3 (CLOSED revision exception) |
| Permission guard | `permission-matrix.md` §4 UI-014 row |
| Decision backwrite | O1-C（CLOSED + 合法 rereviewer → 顯示「前往複審修正」導頁入口，不在本頁提交） |
| Metadata whitelist | O3-C（shared base + UI-014 extension: stage/revisionNo/submittedAt/submittedBy/currentEffective） |
| Wireframe brief | `ui-wireframe-brief.md` §7 UI-014, §9 high-risk C21/C22 |

---

## 3. Structural Region Map（基線版型 — FULL, ALL, HOSPITAL_MANAGER 為代表）

```
┌─────────────────────────────────────────────────────────────┐
│ C01  CaseSummaryHeader                                      │
│  [caseId | project | status | reviewer | rereviewer]       │
│  [initialRevisionCount | rereviewRevisionCount]            │
├─────────────────────────────────────────────────────────────┤
│ C04  ProjectionNoticeBanner  [METADATA_ONLY 時才出現]       │
│  [metadata-only 視角：完整答案差異已遮罩]                   │
├─────────────────────────────────────────────────────────────┤
│ C02  StatusOrBlockedBanner  [terminal/scope/exception]      │
│  [條件出現：CLOSED 提示 / CANCELLED 提示 / scope denied]   │
├─────────────────────────────────────────────────────────────┤
│ C03  ModeIndicatorStrip                                     │
│  [ALL / INITIAL_ONLY / REREVIEW_ONLY / METADATA_HISTORY]   │
├─────────────────────────────────────────────────────────────┤
│  ── Stage Group Tabs ──   [C20 driving]                    │
│  [ALL] [初審 INITIAL] [複審 REREVIEW]                       │
├─────────────────────────────────────────────────────────────┤
│ C20  RevisionTimelineRegion  ★ HIGH-RISK (effective ver)   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Stage | Rev# | SubID | Submitted | By | Status       │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ INITIAL | v1 | ... | 2024-01-10 | 初審醫師 | [Superseded] │
│  │ INITIAL | v2 | ... | 2024-01-15 | 初審醫師 | [★ Current] │
│  │ REREVIEW| v1 | ... | 2024-01-20 | 複審醫師 | [★ Current] │
│  └──────────────────────────────────────────────────────┘  │
│  [Current = 強調色 badge; Superseded = 次要色 badge]       │
│  [★ Effective version 必須有明確視覺強調]                  │
├─────────────────────────────────────────────────────────────┤
│ C21  RevisionCompareSummaryRegion  ★ HIGH-RISK             │
│                                                             │
│  FULL mode:                                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 選版模式: [單版摘要] [雙版比較]                        │  │
│  │                                                        │  │
│  │ 單版摘要:                                              │  │
│  │  submissionId | stage | rev# | status | submittedAt   │  │
│  │  totalScore | comment摘要 | attachment摘要             │  │
│  │  答案摘要 [依權限]                                     │  │
│  │                                                        │  │
│  │ 雙版比較（when selected）:                             │  │
│  │  base rev# vs target rev#                              │  │
│  │  題目變更數 | 分數差異 | comment變更 | attachment變更  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  METADATA_ONLY mode（降級）:                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [ProjectionNoticeBanner 已在上方提示降級]              │  │
│  │  submissionId | stage | revisionNo | status           │  │
│  │  submittedAt | actorId                                 │  │
│  │  [答案摘要已遮罩] [comment 已遮罩] [attachment 已遮罩] │  │
│  │  [比較差異不顯示: "詳細比較需完整視角"]               │  │
│  └──────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│ C22  RevisionNavigationActionZone  ★ HIGH-RISK             │
│  [★ 此區是 navigate-only，不是 submit bar]                 │
│                                                             │
│  Navigate actions (always visible if applicable):          │
│  [返回案件詳情]                                             │
│  [查看初審作業頁 — 僅合法 reviewer + 非 terminal]          │
│  [查看複審作業頁 — 僅合法 rereviewer + 非 terminal]        │
│                                                             │
│  Conditional exception entry (O1-C):                       │
│  [前往複審修正 — 僅 CLOSED + 合法 rereviewer]              │
│  (是 navigate link 而非 submit button)                     │
└─────────────────────────────────────────────────────────────┘
```

> **C22 關鍵設計規範**：
> - 按鈕/連結語意必須是「前往...」而非「提交...」
> - CLOSED exception entry 顯示為 navigate link，不是 primary action button
> - 不得出現任何 form submit / POST 語意的控件

---

## 4. Variant Set

### V1 — ALL（全切換，HOSPITAL_MANAGER / AUDITOR 預設）

```
C01: [Summary Header — FULL: all fields]
C04: [absent]
C02: [absent or CLOSED notice if applicable]
C03: [Mode = HISTORY_ALL]
     Tab: [★ ALL 已選]
C20: [Timeline — ALL stages 混合，INITIAL/REREVIEW 分 stage 標示]
     [Current effective versions 各強調]
C21: [Compare — FULL mode: 全欄位可見]
C22: [返回案件詳情 | 視 role/status 的 review navigate]
     [CLOSED + 合法 rereviewer → exception entry]
```

---

### V2 — INITIAL_ONLY（REVIEWER 預設切換）

```
C01: [Summary Header — FULL]
C04: [absent]
C02: [absent]
C03: [Mode = HISTORY_INITIAL_ONLY]
     Tab: [ALL | ★ 初審 INITIAL 已選]
C20: [Timeline — 只顯示 INITIAL stage revisions]
     [Current initial effective 強調]
C21: [Compare: only INITIAL revision pairs selectable]
C22: [返回案件詳情]
     [前往初審作業頁 — 若合法 reviewer + non-terminal]
```

**Structural difference**: Timeline 只顯示 INITIAL；C21 compare scope 限 INITIAL；C22 只入 review link。

---

### V3 — REREVIEW_ONLY（REREVIEWER 預設切換）

```
C01: [Summary Header — FULL]
C04: [absent]
C02: [absent or CLOSED notice]
C03: [Mode = HISTORY_REREVIEW_ONLY]
     Tab: [ALL | 初審 | ★ 複審 REREVIEW 已選]
C20: [Timeline — 只顯示 REREVIEW stage revisions]
     [Current rereview effective 強調]
C21: [Compare: only REREVIEW revision pairs selectable]
C22: [返回案件詳情]
     [前往複審作業頁 — 若合法 rereviewer + non-terminal]
     [CLOSED + 合法 rereviewer → exception entry]
```

**Structural difference**: Timeline/compare scope 限 REREVIEW；C22 顯示複審 navigate 入口。

---

### V4 — FULL compare（雙版比對展開）

> 仍在 FULL projection，使用者選了兩版進行比較。

```
C01: [Summary Header — FULL]
C04: [absent]
C02: [absent]
C03: [Mode based on tab]
C20: [Revision timeline — 兩版均 highlighted]
C21: [Compare panel EXPANDED]:
     ┌────────────────────────────────────────┐
     │ BASE: REREVIEW v1 vs TARGET: REREVIEW v2 │
     │ 題目變更: 3 題                           │
     │ 分數差異: 88.5 → 91.0                   │
     │ 評語: 已更新                             │
     │ 附件: 新增 1 筆                          │
     │ [view detail — placeholder]              │
     └────────────────────────────────────────┘
C22: [返回案件詳情]
```

---

### V5 — METADATA_ONLY compare（系統管理員視角）

> System Admin 進入，projection = metadata-only。差異摘要降級顯示。

```
C01: [Summary Header — metadata-only: shared base + UI-014 extension]
C04: [★ ProjectionNoticeBanner = VISIBLE]
     [banner: "目前為 metadata-only 視角，完整答案差異已遮罩"]
C02: [absent or minimal]
C03: [Mode = METADATA_HISTORY]
     Tab: [ALL / INITIAL / REREVIEW]
C20: [Timeline — metadata only]:
     [stage | revisionNo | submittedAt | actorId | status (current/superseded)]
     [答案內容 hidden; score hidden]
C21: [Compare panel — DEGRADED]:
     ┌────────────────────────────────────────┐
     │ submissionId | stage | revisionNo      │
     │ submittedAt | actorId                  │
     │ [答案摘要已遮罩]                        │
     │ [詳細比較需完整視角]                    │
     └────────────────────────────────────────┘
     [★ 不顯示 score diff、answer diff、comment diff]
C22: [返回案件詳情]
     [NO 作業頁入口 — sysadmin 不作業]
```

**Structural difference**: C04 出現；C21 只剩 metadata 欄位；C22 無作業 navigate；C20 無 score/answer。

---

### V6 — closed with conditional exception entry（O1-C）

> 案件 CLOSED，但 actor 是合法 rereviewer。

```
C01: [Summary Header — FULL]
C04: [absent]
C02: [banner: "此案件已結案" + 語意提示]
C03: [Mode = HISTORY_REREVIEW_ONLY or ALL]
C20: [Timeline — effective version 標示清晰]
     [CLOSED badge on case]
C21: [Compare — FULL / readonly as CLOSED]
C22: [返回案件詳情]
     [★ 條件式 exception entry (O1-C)]:
     "前往複審修正（CLOSED 例外）"
     [此為 navigate link，導向 UI-013 CLOSED_REVISION_EXCEPTION 模式]
     [★ 不是 submit button — 是 navigate to command page]
```

**Structural difference**: C02 CLOSED notice；C22 顯示 exception navigate link（僅合法 rereviewer）；timeline 顯示結案狀態。

---

## 5. Shared Structure Placement

| Component | Role in UI-014 | Position |
|---|---|---|
| C01 CaseSummaryHeader | case + revision summary 識別 | 頁首固定 |
| C04 ProjectionNoticeBanner | metadata-only 降級提示 | C01 之後，C02 之前 |
| C02 StatusOrBlockedBanner | terminal/scope/exception 訊號 | C04 之後，C03 之前 |
| C03 ModeIndicatorStrip | 階段切換模式標示 | stage tabs 之前 |

**注意**：C04 在 UI-014 是高頻出現成員（sysadmin 進入必顯示）；與 C02 共存時不可互相遮蔽。

---

## 6. High-Risk Visualization Notes

### HR-1 C21 RevisionCompareSummaryRegion（高風險）

**風險描述**：
- FULL vs METADATA_ONLY 的 compare 差異最大
- metadata-only 時若仍渲染比較欄位會造成資料洩漏
- 容易被誤畫成「完整 diff 永遠可見」

**First-pass 處理策略**：
- V4（FULL compare）vs V5（METADATA_ONLY）結構明確對比
- METADATA_ONLY 的 C21 只顯示允許欄位 + 「已遮罩」提示
- 最終資訊密度細節（摘要粒度 vs 完全遮罩）進 ui-design-decisions.md

**Open question**: 比較差異的最細粒度（題目 diff vs 摘要 diff only）— pending confirmation (見 wireframe brief §11)。

### HR-2 C22 RevisionNavigationActionZone（高風險）

**風險描述**：
- history page 的 navigate-only 語意最容易被畫成 command submit bar
- O1-C exception entry 容易被誤實作為「在本頁直接提交 revision」

**First-pass 處理策略**：
- 所有 C22 variants 均明確標示「navigate-only / 前往...」語意
- CLOSED exception entry 明確標注「是 navigate link，不是 submit button」
- C22 與 C16（command bar in UI-013）的視覺語意必須不同 — 細節進 ui-design-decisions.md

### HR-3 Effective Version 強調

**風險描述**：
- 若 effective version 沒有明確視覺標示，使用者無法判斷哪版是「現在有效的」
- superseded versions 需要降級顯示但仍可查看

**First-pass 處理策略**：
- C20 timeline card 中：Current = 強調色 badge；Superseded = 次要色 badge
- 每個 stage 各有自己的 current effective marker
- badge 的精確視覺設計進 ui-design-decisions.md

---

## 7. Placeholder / Deferred Notes

| Region | 狀態 | 下一步 |
|---|---|---|
| C21 metadata-only compare 欄位密度細節 | 只凍結「有降級」，不凍結最終資訊量 | 待 O3 pending confirmation 解除後補齊 |
| C21 逐題 diff（fine-grained） | 只畫「差異摘要」層；逐題 diff 為 deferred | 等 implementation planning 決定是否第一版支援 |
| C22 exception entry 的精確控件類型（link vs button） | 只標「navigate-only intent」 | 進 ui-design-decisions.md |
| C20 timeline card 的 hover / expand 行為 | 只畫靜態骨架 | 後續 interaction spec |
| compare helper hint（可選區塊） | 不在 first-pass 定稿 | 後續 UX 補充 |
| actor-scope hint（可選區塊） | 不在 first-pass 定稿 | 後續 UX 補充 |

---

## 8. Open Questions for Design Decisions

> 以下議題第一輪 wireframe 無法單方面拍板，需進 `ui-design-decisions.md`。

1. **C04 與 C02 共存的優先序**：metadata-only banner vs blocked/CLOSED banner，哪個先？視覺如何分層？
2. **C22 exception entry 的控件類型**：文字連結 vs outlined button vs其他？需與 C16 command bar 有明確視覺差異。
3. **C21 METADATA_ONLY 遮罩層級**：顯示「N 題已遮罩」vs 完全不顯示比較區域——哪種降級語意更佳？
4. **effective version badge 的設計 token**：需定義 current / superseded 的 badge 語意色彩（進 visual baseline）。
5. **stage tab 預設切換策略**：各角色的預設 tab 是要在 route resolver 決定，還是 component 自行根據 role 選擇？（影響 route/VM binding）
6. **比較版本選擇方式**：使用者如何「選兩版比較」— timeline row check / dropdown select？（interaction detail）
7. **C03 mode strip 在 METADATA_HISTORY vs 其他模式的文案**：是否需要提示「metadata-only 不可比較」？
