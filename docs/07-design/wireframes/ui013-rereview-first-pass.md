# UI-013 複審作業頁 — First-Pass Wireframe

> First-pass low-fidelity structure. Not visual baseline. Not Angular code.  
> Reviewer should read each variant's structure difference — not just the default layout.

---

## 1. Page Identity

| 欄位 | 值 |
|---|---|
| pageId | `UI-013` |
| pageName | 複審作業頁 |
| archetype | A4 Action / Review Form |
| primary user goal | rereviewer 完成複審決策（含 CLOSED revision exception） |
| primary actor | REREVIEWER（僅 assigned self） |
| command type | final decision page — submit 後結案 |

---

## 2. Source Alignment

| 類型 | 來源 |
|---|---|
| Archetype | `ui-layout-archetypes.md` A4 Action / Review Form |
| Component candidates | C01 / C02 / C03 / C15 / C16 / C17 / C18 / C19（component-map.md） |
| Page spec authority | `ui-page-spec-ui013複審作業頁.md` |
| State / action policy | `ui-state-actions-matrix.md` §4.5 |
| Domain state guard | `state-machine.md` §3.3 (CLOSED: allow_rereview_revision_exception) |
| Permission guard | `permission-matrix.md` §4 UI-013 row |
| Decision backwrite | O1-C（CLOSED + 合法 rereviewer → revise allowed, new submit blocked） |
| Wireframe brief | `ui-wireframe-brief.md` §7 UI-013, §9 high-risk C16 |

---

## 3. Structural Region Map（基線版型 — NEW_REREVIEW 為代表）

```
┌─────────────────────────────────────────────────────────────┐
│ C01  CaseSummaryHeader                                      │
│  [caseId | project | medicalRecord | status | due | version] │
├─────────────────────────────────────────────────────────────┤
│ C02  StatusOrBlockedBanner                                  │
│  [條件出現 — blocked / CLOSED_exception 時顯示]              │
├─────────────────────────────────────────────────────────────┤
│ C03  ModeIndicatorStrip                                     │
│  [NEW_REREVIEW / EDIT_DRAFT / REVISION / READONLY / 例外修正（已結案）] │
├─────────────────────────────────────────────────────────────┤
│ C15  ReviewFormSummaryStrip                                 │
│  [case context | stage=REREVIEW | version | assignee]       │
├─────────────────────────────────────────────────────────────┤
│ C18  InitialSummaryReferenceRegion  [reference-only]        │
│  [initialScore | reviewer | submittedAt | revisionCount]    │
│  [→ link: 前往初審作業頁 / 查看 revision 歷程（唯讀入口）] │
├─────────────────────────────────────────────────────────────┤
│  ── Rereview Form Main Region ──                           │
│  [score sheet sections / accordion]                         │
│  [items: title | input | maxScore | required | derived]     │
├─────────────────────────────────────────────────────────────┤
│  ── Commentary / Attachment / Final Score Region ──         │
│  [comment textarea]                                         │
│  [attachment list + upload control]                         │
│  [final score summary panel]                               │
│  [⚠ 提交後案件將結案 — semantic notice]                   │
├─────────────────────────────────────────────────────────────┤
│ C19  FinalDecisionSummaryRegion   [page-owned]              │
│  [finalScore preview | rereview stage label]               │
│  [revision mode: base revisionNo → new revision]           │
├─────────────────────────────────────────────────────────────┤
│ C16  ReviewCommandBar  ★ HIGH-RISK                         │
│  [save draft] [submit rereview] [revise rereview] [back]   │
│  mode-driven: action set changes per variant (see below)   │
├─────────────────────────────────────────────────────────────┤
│ C17  ReviewVersionSummaryRegion                             │
│  [currentSubmissionId | revisionNo | stage=REREVIEW]       │
│  [revision mode: basedOn submissionId / revisionNo]        │
│  [→ link: revision 歷程入口]                               │
└─────────────────────────────────────────────────────────────┘
```

> **Region 順序原則**：上下脈絡（C01）→ 警示（C02）→ 行為模式（C03）→ 頁面摘要（C15）→ 前置參考（C18）→ 主作業區 → 決策摘要（C19）→ 命令列（C16）→ 版本摘要（C17）。
> 
> **C16 必須維持高可見優先序**，不得沈到頁底被 scroll 遮蓋。P1 backwrite 採 **viewport-fixed bottom bar**；主作業區需預留對應底部補白，避免最後一題被遮住。

---

## 4. Variant Set

### V1 — NEW_REREVIEW

> 第一次進入複審，尚無 draft 或 submitted。

```
C01: [Summary Header — FULL]
C02: [absent]
C03: [Mode = NEW_REREVIEW]
C15: [context strip]
C18: [Initial Summary Reference — FULL]
     主作業區: [空白表單 — 依 score sheet snapshot]
     Commentary/Attachment: [空]
C19: [FinalDecisionSummaryRegion — score preview 待填]
C16: [save draft ✓ | submit rereview ✓ | revise ✗ hidden | back ✓]
C17: [no previous submission — placeholder: "尚無版本"]
```

**Structural difference from baseline**: C16 無 revise；C17 顯示無版本提示。

---

### V2 — EDIT_DRAFT

> 已存在複審 draft，使用者繼續填寫。

```
C01: [Summary Header — FULL]
C02: [banner: "目前為草稿模式，尚未正式提交"]
C03: [Mode = EDIT_DRAFT]
C15: [context strip]
C18: [Initial Summary Reference — FULL]
     主作業區: [帶入 draft 答案]
     Commentary/Attachment: [帶入 draft 內容]
C19: [FinalDecisionSummaryRegion — draft score preview]
C16: [save draft ✓ | submit rereview ✓ | revise ✗ hidden | back ✓]
C17: [currentSubmissionId(draft) | revisionNo | stage=REREVIEW]
```

**Structural difference**: C02 出現草稿提示；C17 顯示 draft submission；表單預填上次草稿。

---

### V3 — REVISION

> 基於已提交版本建立新 revision（案件仍進行中）。

```
C01: [Summary Header — FULL]
C02: [banner: "目前為複審修正模式，將建立新 revision，不覆寫既有提交"]
C03: [Mode = REVISION]
C15: [context strip + revision indicator]
C18: [Initial Summary Reference — FULL]
     主作業區: [帶入上一版答案（可編輯）]
     Commentary/Attachment: [帶入上一版 + 可修改]
C19: [FinalDecisionSummaryRegion — 含 base revisionNo 標示]
C16: [save draft ✓ | submit revision ✓ | back ✓]
     [注意: button label = "提交複審修正" not "提交複審"]
C17: [basedOn submissionId / revisionNo → new revision pending]
```

**Structural difference**: C02 出現 revision 警示；C16 的 submit 改成「提交複審修正」；C17 顯示 base→new revision 關係；C03 = REVISION。

---

### V4 — READONLY_BLOCKED

> 非合法狀態：已取消 / 非本人 / 案件未至複審 / 其他非法進入。

```
C01: [Summary Header — FULL or limited]
C02: [banner: BLOCKED tone / reason — e.g. "此案件已取消，不可執行複審作業" OR
              "你不是本案件的複審醫師" OR
              "案件尚未進入複審流程"]
C03: [Mode = READONLY_BLOCKED]
C15: [context strip — readonly, no action possibility]
C18: [若已有初審資料: readonly summary]
     主作業區: [DISABLED — 灰化或不渲染]
C19: [absent or readonly]
C16: [所有 submit/draft/revise 按鈕 hidden 或 disabled + reason]
     [只保留 back 按鈕]
C17: [readonly 版本摘要（若有）]
```

**Structural difference**: C02 顯示封鎖原因（區分 3 種 reason）；C16 主要命令全 disable/hidden，only back；主作業區不可編輯（灰化或不渲染）。

---

### V5 — CLOSED revision-exception（O1-C 回寫）

> 案件已 CLOSED，但 actor 是原 rereviewer → revise 入口條件開放。  
> **關鍵差異**：submit new rereview = blocked；revision = allowed（例外模式）。  
> **與 READONLY_BLOCKED 的結構區別必須明確**。

```
C01: [Summary Header — FULL]
C02: [banner: EXCEPTION / amber-warning tone — "此案件已結案，新提交已禁止。
              合法例外：你可以發起複審修正（revision），不開啟新提交。"
              ★ 文案與 READONLY_BLOCKED 必須視覺不同（tone/icon/語意）]
C03: [Mode = CLOSED_REVISION_EXCEPTION]
     [display label = "例外修正（已結案）"]
     [★ mode strip 須明確標示例外語意，不可與一般 REVISION 模式混淆，也不可暗示 reopen]
C15: [context strip + CLOSED case indicator]
C18: [Initial Summary Reference — FULL (readonly)]
     主作業區: [帶入最後一版複審答案（可編輯用於 revision）]
     Commentary/Attachment: [帶入上一版 + 可修改]
C19: [FinalDecisionSummaryRegion — 含 base = last effective revision]
C16: [★ submit new rereview = DISABLED with reason "案件已結案，不可新提交"]
     [★ revise rereview = ENABLED ("提交複審修正（例外）")]
     [save draft ✓ (for revision draft)]
     [back ✓]
C17: [basedOn = last effective submissionId / revisionNo]
     [顯示: CLOSED case effective version context]
```

**Critical structural differences from READONLY_BLOCKED**:
- C02: exception banner vs blocked banner（不同視覺訊號）
- C03: CLOSED_REVISION_EXCEPTION vs READONLY_BLOCKED（不同 mode label）
- C16: revise = ENABLED（vs READONLY_BLOCKED 全 disabled）
- 主作業區: 可編輯（vs READONLY_BLOCKED disabled）

---

## 5. Shared Structure Placement

| Component | Role in UI-013 | Position |
|---|---|---|
| C01 CaseSummaryHeader | case context識別，全變體均顯示 | 頁首固定 |
| C02 StatusOrBlockedBanner | blocked / draft / revision / exception 訊號 | C01 之後，C03 之前 |
| C03 ModeIndicatorStrip | 行為模式標示（5種modes） | C02 之後、C15 之前 |

注意：C04 ProjectionNoticeBanner **不出現在 UI-013**（本頁無 metadata-only 主情境）。

---

## 6. High-Risk Visualization Notes

### HR-1 C16 ReviewCommandBar（高風險）

**風險描述**：
- 若 C16 被畫成 generic footer action bar，會喪失 command page 語意
- 5 種 variants 的 action set 差異很大，若畫面不區分則 reviewer 無法辨別可用操作

**First-pass 處理策略**：
- 在每個 variant 都明確寫出 C16 的 present/disabled/hidden 狀態
- 特別區分 V4 READONLY_BLOCKED（全 disable）vs V5 CLOSED exception（revise enable，submit disable）
- C16 採 fixed-bottom 命令列表達；不以流式 footer 呈現
- C16 的 button labels 不一致（"提交複審" vs "提交複審修正" vs "提交複審修正（例外）"）— 視為 open question 進 ui-design-decisions.md

**Open question for ui-design-decisions.md**:
- CLOSED exception 的 button label 與顏色語意是否需要獨立設計 token

### HR-2 CLOSED revision-exception（結構區分）

**風險描述**：
- V4 READONLY_BLOCKED 與 V5 CLOSED exception 常被錯誤畫成同樣的「禁止」畫面
- 正確語意：V5 的 revise 是合法例外，不是一般禁止

**First-pass 處理策略**：
- V4 vs V5 的 C02 banner、C03 mode strip、C16 action set 三點差異均明確列出
- V4 `READONLY_BLOCKED` = blocked tone；V5 `CLOSED revision-exception` = amber / warning tone
- C03 在 V5 固定顯示 `例外修正（已結案）`

---

## 7. Placeholder / Deferred Notes

| Region | 狀態 | 下一步 |
|---|---|---|
| C18 InitialSummaryReferenceRegion | page-owned，first-pass 畫出骨架；不升 shared | 等 UI-010/UI-013 共用邊界更明確後決定 |
| C19 FinalDecisionSummaryRegion | page-owned，骨架可見；final score calc 細節未定 | 等 API 欄位粒度確認後補齊 |
| 主作業區（score sheet form） | 只畫佔位符；不做 item-level form spec | 等 implementation planning 階段補齊 |
| C16 command bar 版面 | 已回寫為 viewport-fixed bottom bar；主作業區需預留底部補白 | 視覺尺寸 / token 細節留待 visual baseline |
| commentary/attachment 詳細操作 | 只標 placeholder；不定稿最終控件類型 | 後續 component planning |
| revision draft save 可行性 | 未在 API spec 明確，標 pending confirmation | 等 O3 / API spec 更新 |

---

## 8. Open Questions for Design Decisions

> 以下議題第一輪 wireframe 無法單方面拍板，需進 `ui-design-decisions.md`。

1. **C16 submit button label 在 CLOSED exception 時**：「提交複審修正（例外）」vs 其他；label 需設計統一。
2. **C19 FinalDecisionSummaryRegion 是否在 REVISION 模式輸入中顯示即時分數**：live calculation vs submit-time display？
3. **mode strip（C03）與 blocked banner（C02）的優先序**：當兩者都要顯示時，哪個先？
