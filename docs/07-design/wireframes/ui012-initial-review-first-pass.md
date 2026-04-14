# UI-012 初審作業頁 — First-Pass Wireframe

> First-pass low-fidelity structure. Not visual baseline. Not Angular code.  
> This is a **review command page** shared with the A4 grammar used by UI-013, but **without** CLOSED exception behavior.

---

## 1. Page Identity

| 欄位 | 值 |
| --- | --- |
| pageId | `UI-012` |
| pageName | 初審作業頁 |
| archetype | A4 Action / Review Form |
| primary user goal | reviewer 完成初審 draft / submit / revision |
| primary actor | REVIEWER（assigned self only） |
| page type | command page |

**明確禁止**：本頁不承接 CLOSED exception；也不以 generic edit page 呈現 revision。

---

## 2. Source Alignment

| 類型 | 來源 |
| --- | --- |
| Archetype | `ui-layout-archetypes.md` A4 Action / Review Form |
| Component candidates | C01 / C02 / C03 / C15 / C16 / C17（component-map.md） |
| Page spec authority | `ui-page-spec-ui012初審作業頁.md` |
| State / action policy | `ui-state-actions-matrix.md` §4.4 |
| Permission guard | `permission-matrix.md` UI-012 row |
| Review baseline | `ui-visual-baseline.md` §4~§7, `ui013-rereview-first-pass.md` |
| Wireframe brief | `ui-wireframe-brief.md` §7 UI-012 |

---

## 3. Structural Region Map（基線版型 — NEW_INITIAL_REVIEW 為代表）

```text
┌─────────────────────────────────────────────────────────────┐
│ C01  CaseSummaryHeader                                      │
│  [caseId | project | medicalRecord | status | due | version] │
├─────────────────────────────────────────────────────────────┤
│ C02  StatusOrBlockedBanner                                  │
│  [條件出現 — draft / revision / blocked]                     │
├─────────────────────────────────────────────────────────────┤
│ C03  ModeIndicatorStrip                                     │
│  [NEW_INITIAL_REVIEW / EDIT_DRAFT / REVISION / READONLY_BLOCKED] │
├─────────────────────────────────────────────────────────────┤
│ C15  ReviewFormSummaryStrip                                 │
│  [case context | stage=INITIAL | version | assignee]        │
├─────────────────────────────────────────────────────────────┤
│  ── Initial Review Form Main Region ──                      │
│  [score sheet sections / cards / accordion placeholder]     │
│  [required hints | score summary | carried-over answers]    │
├─────────────────────────────────────────────────────────────┤
│  ── Commentary / Attachment Region ──                       │
│  [comment textarea placeholder]                             │
│  [attachment list / upload placeholder]                     │
│  [revision mode: append-only note]                          │
├─────────────────────────────────────────────────────────────┤
│ C17  ReviewVersionSummaryRegion                             │
│  [currentSubmissionId | revisionNo | stage=INITIAL]         │
│  [revision mode: basedOn submissionId / revisionNo]         │
│  [→ link: 前往 Revision 歷程]                                │
├─────────────────────────────────────────────────────────────┤
│ C16  ReviewCommandBar  ★ inherited high-risk                │
│  [save draft] [submit initial] [submit initial revision] [back] │
│  mode-driven: action set changes per variant                │
└─────────────────────────────────────────────────────────────┘
```

> **Region 順序原則**：C01 → C02 → C03 → C15 → 主作業區 → C17 → C16。  
> **A4 reuse 原則**：延用 UI-013 的 command-page hierarchy，但移除 UI-013 專屬的 C18/C19 與 CLOSED exception 分支。  
> **C16** 延用 viewport-fixed bottom bar mental model；主作業區需預留底部補白，不得讓最後一題被遮住。

---

## 4. Variant Set

### V1 — NEW_INITIAL_REVIEW

```text
C01: [Summary Header — FULL]
C02: [absent]
C03: [Mode = NEW_INITIAL_REVIEW]
C15: [context strip]
主作業區: [空白表單 — 依 score sheet snapshot]
Commentary/Attachment: [空]
C17: [version summary: 尚無已提交初審版本]
C16: [save draft ✓ | submit initial ✓ | submit initial revision ✗ hidden | back ✓]
```

---

### V2 — EDIT_DRAFT

```text
C01: [Summary Header — FULL]
C02: [banner: INFO — 目前為草稿模式，尚未正式提交]
C03: [Mode = EDIT_DRAFT]
C15: [context strip]
主作業區: [帶入 draft 答案]
Commentary/Attachment: [帶入 draft]
C17: [current draft submissionId | revisionNo]
C16: [save draft ✓ | submit initial ✓ | submit initial revision ✗ hidden | back ✓]
```

---

### V3 — REVISION

```text
C01: [Summary Header — FULL]
C02: [banner: INFO — 將建立新 revision，不覆寫既有初審提交]
C03: [Mode = REVISION]
C15: [context strip + revision indicator]
主作業區: [帶入上一版答案（可編輯）]
Commentary/Attachment: [帶入上一版 + 可修改]
C17: [basedOn submissionId / revisionNo → new revision pending]
C16: [save revision draft ✓ | submit initial revision ✓ | back ✓]
     [submit label = 「提交初審修正」]
```

---

### V4 — READONLY_BLOCKED

```text
C01: [Summary Header — FULL or limited]
C02: [banner: BLOCKED tone / reason — 非本人 / CLOSED / CANCELLED / stage mismatch]
C03: [Mode = READONLY_BLOCKED]
C15: [context strip — readonly]
主作業區: [disabled or hidden]
Commentary/Attachment: [readonly placeholder]
C17: [readonly version summary（若有）]
C16: [所有 draft / submit / revise hidden 或 disabled + reason]
     [只保留 back]
```

---

## 5. Shared Structure Placement

| Component | Role in UI-012 | Position |
| --- | --- | --- |
| C01 CaseSummaryHeader | 案件與版本上下文 | 頁首固定 |
| C02 StatusOrBlockedBanner | 草稿 / revision / blocked 提示 | C01 之後 |
| C03 ModeIndicatorStrip | 顯示 initial review page mode | C02 之後 |
| C15 ReviewFormSummaryStrip | A4 共用 summary strip | 主作業區前 |
| C17 ReviewVersionSummaryRegion | append-only / basedOn version context | C16 前 |
| C16 ReviewCommandBar | A4 共用 command zone | 頁底高可見區 |

---

## 6. High-Risk / Review Focus Notes

### HR-1 C16 ReviewCommandBar（繼承既有高風險）

- draft / submit / revision label 若未分離，UI-012 會看起來像一般編輯頁
- REVISION 必須明確標示 append-only，不可被誤讀為覆寫既有提交
- READONLY_BLOCKED 需與 REVISION 保持清楚語意差異

> 本頁沒有新增新的 repo-level high-risk target；僅繼承 `ui-wireframe-brief.md` 已凍結的 C16 風險。

---

## 7. Placeholder / Deferred Notes

| Region | 狀態 | 下一步 |
| --- | --- | --- |
| score sheet item-level spec | placeholder only | implementation planning 才補 item schema |
| attachment 深層互動 | placeholder only | 本輪只保留 interaction intent |
| compare / cross-role reference | not in scope | UI-012 不引入 UI-013 的 reference regions |

---

## 8. Prototype Handoff Notes

1. route 採 `/cases/:caseId/initial-review?variant=...`
2. variant 至少對應 `NEW_INITIAL_REVIEW` / `EDIT_DRAFT` / `REVISION` / `READONLY_BLOCKED`
3. 延用 P1 `PageFrame + VariantSwitcher + CommandBar` pattern
4. prototype 只需驗證 command grammar、append-only revision、blocked reasons，不做真實表單實作
