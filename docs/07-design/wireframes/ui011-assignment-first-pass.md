# UI-011 案件分派頁 — First-Pass Wireframe

> First-pass low-fidelity structure. Not visual baseline. Not Angular code.  
> This is a **manager assignment command page** — not a control center, not a generic CRUD form.

---

## 1. Page Identity

| 欄位 | 值 |
|---|---|
| pageId | `UI-011` |
| pageName | 案件分派頁 |
| archetype | A3 Action / Assignment |
| primary user goal | manager 完成 assign / reassign reviewer 與 rereviewer 命令 |
| primary actor | HOSPITAL_MANAGER |
| page type | assignment command page |

**明確禁止**：本頁不承擔審查填答、不承擔歷程閱讀，也不把 reviewer / rereviewer 混成單一 generic section。

---

## 2. Source Alignment

| 類型 | 來源 |
|---|---|
| Archetype | `ui-layout-archetypes.md` A3 Action / Assignment |
| Component candidates | C01 / C02 / C03 / C12 / C13 / C14 / C23 deferred |
| Page spec authority | `ui-page-spec-ui011案件分派頁.md` |
| State / action policy | `ui-state-actions-matrix.md` §4.3 |
| View model bridge | `ui-view-model-spec.md` `AssignmentPageViewModel` |
| Wireframe brief | `ui-wireframe-brief.md` §7 UI-011 |

---

## 3. Structural Region Map（基線版型 — ASSIGN_INITIAL 為代表）

```
┌─────────────────────────────────────────────────────────────┐
│ C01  CaseSummaryHeader                                      │
│  [caseId | project | medicalRecord | status | due | owner] │
├─────────────────────────────────────────────────────────────┤
│ C02  StatusOrBlockedBanner                                  │
│  [條件出現 — blocked / scope mismatch / terminal]           │
├─────────────────────────────────────────────────────────────┤
│ C03  ModeIndicatorStrip                                     │
│  [ASSIGN_INITIAL / REASSIGN_INITIAL / MIXED_ASSIGNMENT / READONLY_BLOCKED] │
├─────────────────────────────────────────────────────────────┤
│ C12  AssignmentModeBanner                                   │
│  [當前指派任務說明：初審指派 / 初審改派 / 混合指派 / blocked] │
├─────────────────────────────────────────────────────────────┤
│ C13  ReviewerAssignmentSection                              │
│  [current reviewer summary]                                 │
│  [candidate picker placeholder]                             │
│  [dueAt / reason 可選欄位 placeholder]                      │
├─────────────────────────────────────────────────────────────┤
│ C14  RereviewerAssignmentSection                            │
│  [current rereviewer summary]                               │
│  [candidate picker placeholder]                             │
│  [當前不可指派時，以 blocked hint 取代輸入區]               │
├─────────────────────────────────────────────────────────────┤
│ C23  AssignmentDueDateHelperRegion  [deferred placeholder]  │
│  [★ 僅標示為可選 helper，不在本輪定稿]                      │
├─────────────────────────────────────────────────────────────┤
│ page-level submit / back action zone [page-owned]           │
│  [提交初審指派] [提交初審改派] [提交複審指派] [返回案件詳情]  │
│  mode-driven: action label changes per variant              │
└─────────────────────────────────────────────────────────────┘
```

> **Region 順序原則**：C01 → C02 → C03 → C12 → C13 → C14 → C23 placeholder → page-level submit/back zone。  
> **A3 principle**：submit zone 是 page-level command，不得畫成 A4 fixed-bottom command bar。

---

## 4. Variant Set

### V1 — ASSIGN_INITIAL

```
C01: [Summary Header — FULL]
C02: [absent]
C03: [Mode = ASSIGN_INITIAL]
C12: [banner: 此案件尚未分派初審醫師]
C13: [reviewer picker enabled]
C14: [readonly hint: 尚未進入複審，暫不開放 rereviewer 指派]
C23: [placeholder]
submit zone: [提交初審指派 ✓ | 返回案件詳情 ✓]
```

---

### V2 — REASSIGN_INITIAL

```
C01: [Summary Header — FULL]
C02: [absent or info]
C03: [Mode = REASSIGN_INITIAL]
C12: [banner: 此案件已分派初審醫師，可重新指定]
C13: [current reviewer shown + picker enabled]
C14: [readonly hint]
C23: [placeholder]
submit zone: [提交初審改派 ✓ | 返回案件詳情 ✓]
```

---

### V3 — MIXED_ASSIGNMENT

```
C01: [Summary Header — FULL]
C02: [absent]
C03: [Mode = MIXED_ASSIGNMENT]
C12: [banner: reviewer 已固定，當前僅需完成 rereviewer 指派]
C13: [current reviewer shown / inputs readonly]
C14: [rereviewer picker enabled]
C23: [placeholder]
submit zone: [提交複審指派 ✓ | 返回案件詳情 ✓]
```

---

### V4 — READONLY_BLOCKED

```
C01: [Summary Header — FULL]
C02: [banner: BLOCKED tone / reason — CLOSED / CANCELLED / role/scope mismatch]
C03: [Mode = READONLY_BLOCKED]
C12: [blocked explanation]
C13: [readonly summary only]
C14: [readonly summary only]
C23: [placeholder hidden or muted]
submit zone: [所有 assignment command hidden 或 disabled + reason]
             [只保留返回案件詳情]
```

---

## 5. Shared Structure Placement

| Component | Role in UI-011 | Position |
|---|---|---|
| C01 CaseSummaryHeader | 指派決策所需上下文 | 頁首 |
| C02 StatusOrBlockedBanner | terminal / scope / blocked 提示 | C01 之後 |
| C03 ModeIndicatorStrip | assignment mode 顯示 | C02 之後 |
| C12 AssignmentModeBanner | 任務型態說明 | assignment sections 之前 |
| C13 ReviewerAssignmentSection | reviewer 指派或改派 | 主區塊 1 |
| C14 RereviewerAssignmentSection | rereviewer 指派或改派 | 主區塊 2 |
| C23 AssignmentDueDateHelperRegion | deferred helper | C14 之後 |

---

## 6. Review Focus Notes

- reviewer / rereviewer 必須視覺與語意分離，不可合併為單一 generic assignment form
- READONLY_BLOCKED 不應留下可誤觸的 picker 或 submit CTA
- submit button label 必須反映真實命令：`提交初審指派`、`提交初審改派`、`提交複審指派`

> 以上為 P2 review focus，不升級為新的 repo-level high-risk target。

---

## 7. Placeholder / Deferred Notes

| Region | 狀態 | 下一步 |
|---|---|---|
| C23 AssignmentDueDateHelperRegion | deferred | 等 API / helper 粒度穩定後補 |
| candidate picker 互動細節 | placeholder only | prototype 僅驗證 section separation |
| change reason note | optional placeholder | 不在本輪定稿 |

---

## 8. Prototype Handoff Notes

1. route 採 `/cases/:caseId/assign?variant=...`
2. variant 至少對應 `ASSIGN_INITIAL` / `REASSIGN_INITIAL` / `MIXED_ASSIGNMENT` / `READONLY_BLOCKED`
3. prototype 需保留 C13 / C14 分離的 section 結構
4. submit zone 可沿用 shared command widget，但不可冒充 A4 fixed-bottom command bar
