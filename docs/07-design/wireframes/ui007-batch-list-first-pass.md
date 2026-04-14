# UI-007 抽選批次列表頁 — First-Pass Wireframe

> First-pass low-fidelity structure. Not visual baseline. Not Angular code.
> This is a **multi-role execution tracking list page** — manager / auditor / system_admin have distinct variants.

---

## 1. Page Identity

| 欄位 | 值 |
|---|---|
| pageId | `UI-007` |
| pageName | 抽選批次列表頁 |
| archetype | SA2 List / Execution Tracking |
| primary user goal | 追蹤抽選批次執行狀態；失敗時觸發 retry；下鑽至批次詳情或案件列表 |
| primary actors | HOSPITAL_MANAGER / AUDITOR / SYSTEM_ADMIN |
| secondary actors | REVIEWER / REREVIEWER（R — 待確認，見 §7 OQ-S05） |
| page type | execution tracking list |

**Archetype 定義 (SA2 — Sampling Cluster 專屬，首次定義)**:
多角色批次執行追蹤列表頁。批次 state machine 驅動 tab preset 與 row action 可用性。角色決定 projection、tab 選項、retry 可用性。

**與 A1 Work Queue 的差異**：本頁不做 work routing，task focus 是 execution tracking + retry gateway，而非案件工作站。Row actions 不含 work entry。

**明確禁止**:
- 不畫 page-level command bar
- 不做 work queue tab preset（非案件工作站）
- [重跑] 僅限 row-level，僅限 manager + FAILED batch，不得出現在 page header 或 command bar

---

## 2. Source Alignment

| 類型 | 來源 |
|---|---|
| Archetype | SA2 List / Execution Tracking（本文件首次定義，sampling cluster 專屬；非 cases cluster A1-A5） |
| Page spec authority | `docs/05-ui/ui-page-spec-ui007抽選批次列表頁.md` |
| Permission | `docs/02-ddd/permission-matrix.md` — UI-007: MANAGER=Y, AUDITOR=Y, SYSADMIN=R, REVIEWER=R, REREVIEWER=R |
| Domain state | `docs/02-ddd/state-machine.md` §5 SamplingBatch lifecycle（STARTED / COLLECTING_CANDIDATES / SAMPLING / COMPLETED / FAILED） |
| API binding | `docs/03-contracts/api-spec.md` §9.3 GET /sampling-batches, §9.4 POST /sampling-batches/{batchId}/retry |
| Key decisions | `docs/01-requirements/decision-log.md` D-009（AUDITOR scope-limited）, D-010（SYSADMIN metadata only）, D-013（retry 沿用原 snapshot，retryOfBatchId 保留） |

---

## 3. Structural Region Map（基線角色：MANAGER，tab=全部批次）

```
┌─────────────────────────────────────────────────────────────────┐
│ BatchListPageHeader                                             │
│  [page title = 抽選批次列表 | role summary = 院屬管理員]        │
│  [quick stats: 全部批次: N | FAILED 可重跑: M]                  │
│  (SYSADMIN mode: + ProjectionNoticeBanner inline)              │
├─────────────────────────────────────────────────────────────────┤
│ BatchStatusTabZone                                              │
│  [role-driven tabs — see role variant matrix below]            │
│  [active tab = 全部批次 by default; count badge per tab]       │
├─────────────────────────────────────────────────────────────────┤
│ BatchFilterBar                                                  │
│  [projectId | hospitalId | status | executionMode]             │
│  [startedFrom ~ startedTo | batchId keyword]                   │
│  [active filter chips row]                                     │
│  [[套用篩選] [清除篩選]]                                        │
├─────────────────────────────────────────────────────────────────┤
│ BatchListTable                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ batchId │ projectName │ versionId │ mode │ status       │  │
│  │ populationC │ eligibleC │ excludedC │ selectedC        │  │
│  │ retryOfBatchId │ startedAt │ finishedAt │ actions      │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ [COMPLETED row]  status=success, neutral display         │  │
│  │   actions: [查看詳情→UI-008] [查看案件→UI-009]           │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ [FAILED row]  status=failed, warning tone                │  │
│  │   retryOfBatchId: {id if is retry}                       │  │
│  │   actions: [查看詳情] [查看案件] [重跑]*                   │  │
│  │   * [重跑] = manager only; with confirm modal            │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ [IN_PROGRESS row]  status badge only                     │  │
│  │   actions: [查看詳情]                                    │  │
│  └──────────────────────────────────────────────────────────┘  │
│  (table-level states: empty / forbidden / error — see V4/V6)   │
├─────────────────────────────────────────────────────────────────┤
│ BatchPagingZone                                                 │
│  [page / pageSize / totalCount]                                │
└─────────────────────────────────────────────────────────────────┘
```

**角色 Tab Preset 矩陣**:

| 角色 | Tab 1 | Tab 2 | Tab 3 | Tab 4 |
|---|---|---|---|---|
| MANAGER | 全部批次 | 成功 | 失敗 | **可重跑** |
| AUDITOR | 全部批次 | 成功 | 失敗 | — |
| SYSTEM_ADMIN | 全部批次 | 失敗 | — | — |
| REVIEWER / REREVIEWER | 待確認（OQ-S05） | | | |

---

## 4. Variant Set

### V1 — MANAGER_TRACKING（完整存取）

```
BatchListPageHeader    [title + manager summary + quick stats (total / retryable count)]
BatchStatusTabZone     [全部批次 | 成功 | 失敗 | 可重跑]
BatchFilterBar         [all filter fields visible]
BatchListTable         [full data; FAILED rows + [重跑]; COMPLETED/IN_PROGRESS rows standard nav]
BatchPagingZone        [standard paging]
```

**Row action grammar（MANAGER 視角）**:
- `[查看詳情]` → navigate to UI-008（STUB — page spec 尚不存在，all status）
- `[查看案件]` → navigate to UI-009 with `?batchId={batchId}`（COMPLETED / FAILED）
- `[重跑]` → confirm modal V5 → `POST /sampling-batches/{batchId}/retry`（FAILED only）

---

### V2 — AUDITOR_READONLY（授權範圍，無 retry）

```
BatchListPageHeader    [title + auditor summary]
BatchStatusTabZone     [全部批次 | 成功 | 失敗]   ← no 可重跑 tab
BatchFilterBar         [all filter fields; hospitalId = scope-limited to authorized 院區]
BatchListTable         [full data visible; FAILED rows NO [重跑]; [查看詳情] [查看案件] only]
BatchPagingZone        [standard paging]
```

---

### V3 — SYSADMIN_METADATA（metadata projection）

```
BatchListPageHeader         [title + sysadmin summary]
ProjectionNoticeBanner      [metadata-only projection indicated; inline or below header]
BatchStatusTabZone          [全部批次 | 失敗]   ← minimal tabs
BatchFilterBar              [metadata-safe fields only; score-related filters hidden]
BatchListTable
  visible columns:   batchId | projectName | hospitalId | executionMode
                     status | retryOfBatchId | startedAt | finishedAt
  MASKED columns:    populationCount | eligibleCount | excludedCount | selectedCount
                     (masked per D-010; exact whitelist = OQ-S04 pending confirmation)
  actions:           [查看詳情] only — links to metadata-only batch detail view (UI-008 STUB)
BatchPagingZone             [standard paging]
```

> **注意**: metadata projection 欄位白名單由 D-010 要求「metadata only」，但 permission-matrix.md 未列出 SamplingBatch 具體欄位清單。V3 以 status/timing/retryChain 可見、stats 欄位遮蔽為假設畫出，待 OQ-S04 確認後勘誤。

---

### V4 — EMPTY_STATE（無符合條件批次）

```
BatchListPageHeader    [visible]
BatchStatusTabZone     [active tab shows (0)]
BatchFilterBar         [visible — allow filter reset]
BatchListTable         [replaced by: table-level empty state message]
                       [「目前沒有符合條件的批次」]
BatchPagingZone        [hidden or disabled]
```

---

### V5 — RETRY_CONFIRM_MODAL（overlay on V1）

```
[V1 background, dimmed]
┌──────────────────────────────────────────────────────┐
│ 確認重跑批次                                         │
│                                                      │
│ 將以新的抽選批次重跑，原失敗批次會保留。             │
│                          （D-013 固定語意，不可改動）│
│                                                      │
│ 目標批次: {batchId}                                  │
│                                                      │
│             [取消]          [確認重跑]               │
└──────────────────────────────────────────────────────┘
```

> **Retry 語意（D-013）**: 新 batch 使用 *原始 snapshot*，不使用後續新 published version。confirm 文字必須照此語意書寫，不得暗示「用最新規則重跑」。

---

### V6 — FORBIDDEN / ERROR_STATE

```
BatchListPageHeader    [visible if role info available]
BatchStatusTabZone     [hidden or placeholder]
BatchFilterBar         [hidden]
BatchListTable         [replaced by: table-level 403 / error banner]
BatchPagingZone        [hidden]
```

---

## 5. High-Risk Targets

| Region | 風險 | First-pass 處理 |
|---|---|---|
| BatchListTable — V3 SYSADMIN | SamplingBatch metadata projection 欄位白名單未定義；容易 over-expose 或 under-expose | V3 drawn with explicit MASKED markers；OQ-S04 pending |
| BatchStatusTabZone — 角色差異 | 錯誤 tab preset 可能讓 auditor 看到不應見的 retryable entries，或 manager 看不到 retry opportunity | 每個 role variant 明確列出 tab set |
| [重跑] row action | D-013：retry 必須使用原 snapshot，確認 modal 文字不得誤導為「用最新版本重跑」 | Modal text 按 page spec §7 verbatim；D-013 標注不可改動 |
| [查看詳情] 目標頁 | UI-008 page spec 不存在；row action 目標缺設計基礎 | 所有 [查看詳情] 標注 STUB；handoff scope 明確排除此 action 的完整實作 |

---

## 6. Deferred Items

- Real-time batch status refresh 策略（polling / SSE / manual）：無 API spec → deferred
- 匯出功能：page spec §5.5 提及但無 API spec → deferred
- UI-008 抽選批次詳情頁：[查看詳情] 目標頁尚無 page spec，row action destination 為 STUB
- BatchListTable 欄位在窄螢幕的顯示優先順序（deferred 至 visual baseline）
- retryOfBatchId 欄位在多次 retry 時的 chain display（deferred 至 interaction spec）
- Route module 定義（`ui-route-module-map.md` 不涵蓋 sampling cluster）
- View-model spec（`ui-view-model-spec.md` 僅涵蓋 cases cluster；sampling cluster 尚未定義）

---

## 7. Open Questions

| # | 問題 | 影響範圍 | 優先順序 |
|---|---|---|---|
| OQ-S04 | SYSTEM_ADMIN SamplingBatch metadata projection 欄位白名單：D-010 要求 metadata only，但 `permission-matrix.md` 未列出 SamplingBatch 的具體可見欄位清單 | V3 variant MASKED 欄位範圍 | 高 |
| OQ-S05 | REVIEWER / REREVIEWER：`permission-matrix` = R，page spec §8 = 「不建議作為主要使用角色」。是否路由守衛 deny？或允許有限 read-only 進入？若允許，tab preset 為何？ | Route guard 實作；是否需補第5 role variant | 中 |
| OQ-S06 | UI-008 page spec 不存在。[查看詳情] row action destination 在 UI-008 完成前為 STUB。何時補齊？ | BatchTableRowActions —查看詳情— 完整實作 | 中 |
| OQ-S07 | 「可重跑」tab（MANAGER 專屬）：API §9.3 GET /sampling-batches 是否支援 `status=FAILED` filter 作為 proxy？還是 frontend 需自行 compute retryable count？ | BatchStatusTabZone + BatchFilterBar query logic | 低 |
