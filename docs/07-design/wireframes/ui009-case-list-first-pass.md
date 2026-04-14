# UI-009 案件列表頁 — First-Pass Wireframe

> First-pass low-fidelity structure. Not visual baseline. Not Angular code.  
> This is a **role-aware work queue page** — row-level navigate actions only, never a page-level command bar.

---

## 1. Page Identity

| 欄位 | 值 |
|---|---|
| pageId | `UI-009` |
| pageName | 案件列表頁 |
| archetype | A1 List / Work Queue |
| primary user goal | 以 role-default tab + filter 找到下一步工作入口 |
| primary actors | HOSPITAL_MANAGER / REVIEWER / REREVIEWER / AUDITOR / SYSTEM_ADMIN |
| page type | work queue |

**明確禁止**：本頁不畫 page-level submit bar；所有操作都是 row-level navigate / read entry。

---

## 2. Source Alignment

| 類型 | 來源 |
|---|---|
| Archetype | `ui-layout-archetypes.md` A1 List / Work Queue |
| Component candidates | C01 / C04 / C05 / C06 / C07 |
| Page spec authority | `ui-page-spec-ui009案件列表頁.md` |
| State / action policy | `ui-state-actions-matrix.md` §4.1 |
| View model bridge | `ui-view-model-spec.md` `CaseListRowViewModel` / `CaseListFilterViewModel` |
| Wireframe brief | `ui-wireframe-brief.md` §7 UI-009 |

---

## 3. Structural Region Map（基線版型 — manager default 為代表）

```
┌─────────────────────────────────────────────────────────────┐
│ C01  CaseSummaryHeader                                      │
│  [page title | role summary | current result count]        │
│  [metadata-only 時：inline projection indicator]            │
├─────────────────────────────────────────────────────────────┤
│ C05  CaseWorkQueueFilterBar                                 │
│  [tab query preset row]                                     │
│  [keyword / hospital / project / status / due range]        │
│  [active filter chips]                                      │
├─────────────────────────────────────────────────────────────┤
│ C06  CaseWorkQueueTable                                     │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ caseId | project | batch | status | dueAt | updatedAt │ │
│  │ reviewer | rereviewer | score summary(optional)      │ │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  [metadata-only rows: shared base + page extension only]    │
│  [forbidden / empty / error 用 table-level banner 取代 rows] │
├─────────────────────────────────────────────────────────────┤
│ C07  CaseRowActionSet                                       │
│  [前往案件詳情] [前往分派] [前往初審] [前往複審] [前往歷程]     │
│  row-level only; hidden or disabled + reason                │
│  narrow screen: overflow trigger placeholder                │
├─────────────────────────────────────────────────────────────┤
│ paging zone [page-owned]                                    │
│  [page / pageSize / totalCount]                             │
└─────────────────────────────────────────────────────────────┘
```

> **Region 順序原則**：C01 → C05 → C06/C07 → paging。  
> **projection rule**：metadata-only 是同一 work queue 下的 projection，不拆獨立頁。  
> **C04 在 UI-009 的落點**：沿用 P1 projection semantics，以 C01 內的 inline indicator 為優先，不獨立拉出 page-level status row。

---

## 4. Variant Set

### V1 — MANAGER_DEFAULT_WORKLIST

```
C01: [page title = 案件列表 / role summary = manager]
C04: [absent]
C05: [tab preset = 待分派]
C06: [rows = SELECTED_PENDING_ASSIGNMENT centered]
C07: [row actions = 前往案件詳情 / 前往分派 / 前往歷程]
paging: [visible]
```

---

### V2 — REVIEWER_DEFAULT_WORKLIST

```
C01: [page title = 案件列表 / role summary = reviewer]
C04: [absent]
C05: [tab preset = 待初審]
C06: [rows = PENDING_INITIAL_REVIEW / INITIAL_REVIEW_IN_PROGRESS]
C07: [row actions = 前往案件詳情 / 前往初審 / 前往歷程]
     [前往分派 hidden]
```

---

### V3 — METADATA_ONLY_WORKLIST

```
C01: [page title = 案件列表 / role summary = system admin]
     [inline projection indicator = metadata-only 視角]
C05: [僅 metadata filter 最小集]
C06: [rows = shared base + UI-009 extension(batchId / dueAt)]
     [score / full answer digest hidden]
C07: [row actions = 前往案件詳情 / 前往歷程]
     [assign / initial / rereview hidden]
```

---

### V4 — FORBIDDEN_OR_EMPTY

```
C01: [page title + role summary]
C05: [保留 filter context]
C06: [table-level banner: 無可顯示案件 / scope denied / query empty]
C07: [absent]
paging: [hidden or disabled]
```

---

### V5 — ROW_ACTION_OVERFLOW

```
C01: [page title + role summary]
C05: [tab preset + filters]
C06: [single row focus]
C07: [多個 row actions 收斂到 overflow trigger placeholder]
     [仍維持 row-level navigate-only，不升級為 page action bar]
```

---

## 5. Shared Structure Placement

| Component | Role in UI-009 | Position |
|---|---|---|
| C01 CaseSummaryHeader | work queue 標題、角色視角、projection indicator | 頁首 |
| C04 ProjectionNoticeBanner | metadata-only 投影說明 | 併入 C01 |
| C05 CaseWorkQueueFilterBar | tab=query preset + filters | header 後 |
| C06 CaseWorkQueueTable | rows / empty / error 承載區 | 主區塊 |
| C07 CaseRowActionSet | row-level entry actions | row 內或 row 下方 |

---

## 6. Review Focus Notes

- row action 必須維持 navigate-only，不能被誤畫成 page command bar
- metadata-only rows 必須保留 shared base + UI-009 extension，不可漏出 score / answer digest
- tab 是 query preset，不是另一個 page identity
- narrow screen 時 action overflow 只能改變呈現方式，不改變 row-level ownership

> `row action overflow` 是本輪 P2 review focus，不升級為新的 repo-level high-risk target。

---

## 7. Placeholder / Deferred Notes

| Region | 狀態 | 下一步 |
|---|---|---|
| bulk action / export | placeholder only | 本輪不定稿 |
| system admin filter 最小集 | placeholder note allowed | 仍屬 pending confirmation |
| row overflow widget 細節 | placeholder only | prototype 驗證概念即可 |

---

## 8. Prototype Handoff Notes

1. route 採 `/cases?variant=...`
2. variant 至少對應 manager default、reviewer default、metadata-only、forbidden/empty、row action overflow
3. prototype 需呈現 row-level action visibility / blocked reason / metadata row 差異
4. 不需做真實 table sorting/filter interaction；保留 filter chips 與 rows 即可
