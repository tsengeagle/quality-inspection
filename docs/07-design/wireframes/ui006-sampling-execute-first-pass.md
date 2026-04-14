# UI-006 抽選執行頁 — First-Pass Wireframe

> First-pass low-fidelity structure. Not visual baseline. Not Angular code.
> This is a **single-actor sequential command page** — HOSPITAL_MANAGER only; no multi-role variant.

---

## 1. Page Identity

| 欄位 | 值 |
|---|---|
| pageId | `UI-006` |
| pageName | 抽選執行頁 |
| archetype | SA1 Action / Preview-Execute |
| primary user goal | 預估母體後正式執行抽選，建立新 SamplingBatch |
| primary actors | HOSPITAL_MANAGER（唯一可進入角色） |
| page type | sequential command |

**Archetype 定義 (SA1 — Sampling Cluster 專屬，首次定義)**:
單一角色、雙階段循序命令頁面。頁面狀態機（READY_FOR_PREVIEW → PREVIEW_COMPLETED → EXECUTING → EXECUTE_SUCCESS / EXECUTE_FAILED / READONLY_BLOCKED）驅動 region 可見性。
在此 archetype 下，沒有多角色版本；BLOCKED 態直接以具體 reason 顯示取代 action zone。

**明確禁止**:
- 不畫 row-level action table
- 不畫 multi-role tab preset
- 不畫 page-level data table
- Preview 與 Execute 不可排列為並排按鈕；必須是循序兩步驟

---

## 2. Source Alignment

| 類型 | 來源 |
|---|---|
| Archetype | SA1 Action / Preview-Execute（本文件首次定義，sampling cluster 專屬；非 cases cluster A1-A5） |
| Page spec authority | `docs/05-ui/ui-page-spec-ui006抽選執行頁.md` |
| Permission | `docs/02-ddd/permission-matrix.md` — UI-006: MANAGER=Y; all others=N |
| Domain state | `docs/02-ddd/state-machine.md` §5 SamplingBatch lifecycle |
| API binding | `docs/03-contracts/api-spec.md` §9.1 POST /preview, §9.2 POST /execute |
| Key decisions | `docs/01-requirements/decision-log.md` D-007（同批次查重）, D-011（version+snapshot traceability）, D-013（retry 沿用原 snapshot） |
| Mode design | `ui-page-spec-ui006抽選執行頁.md` §6 |
| Blocked semantics | `ui-page-spec-ui006抽選執行頁.md` §7 |

---

## 3. Structural Region Map（基線模式：READY_FOR_PREVIEW）

```
┌─────────────────────────────────────────────────────────────────┐
│ SamplingPageHeader                                              │
│  [projectName | hospital | status=ACTIVE | activePeriod]       │
│  [publishedVersionId | scoresheetName | qualityThreshold]      │
│  (BLOCKED mode: + inline blocked-reason banner here)           │
├─────────────────────────────────────────────────────────────────┤
│ SamplingRuleSummaryRegion              [READ-ONLY; always show] │
│  [samplingType | targetingScope | samplingDateRange]           │
│  [samplingMethod | stratificationDimension | excludedDepts]    │
├─────────────────────────────────────────────────────────────────┤
│ SamplingPreviewActionZone                                       │
│  [mode=READY_FOR_PREVIEW:  [預估母體] primary CTA]             │
│  [mode=PREVIEW_COMPLETED:  [重新預估] secondary CTA]           │
│  [mode=EXECUTING:          zone disabled, loading indicator]   │
├─────────────────────────────────────────────────────────────────┤
│ SamplingPreviewStatsRegion                                      │
│  [hidden in READY_FOR_PREVIEW; visible in PREVIEW_COMPLETED+]  │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐  │
│  │ 母體總數   │ │ 符合條件   │ │ 排除件數   │ │ 估算抽中   │  │
│  │population │ │eligible    │ │excluded    │ │[CONDITIONAL│  │
│  │Count      │ │Count       │ │Count       │ │see OQ-S01] │  │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│ SamplingExecuteConfirmZone                                      │
│  [hidden in READY_FOR_PREVIEW]                                  │
│  [visible in PREVIEW_COMPLETED: [正式執行抽選] primary CTA]    │
│  [on click: confirm modal — see V5 CONFIRM_MODAL variant]      │
│  [secondary: [返回專案詳情]]                                    │
│  [mode=EXECUTING: CTA replaced by loading indicator]           │
├─────────────────────────────────────────────────────────────────┤
│ SamplingPostExecuteRegion                                       │
│  [hidden until EXECUTE_SUCCESS / EXECUTE_FAILED]               │
│                                                                 │
│  --- SUCCESS variant ---                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ ✓  抽選執行完成                                          │  │
│  │ batchId:       {batchId}                                 │  │
│  │ selectedCount: {N} 件已抽選                              │  │
│  │ startedAt / finishedAt: [DEFERRED — see OQ-S02]          │  │
│  └──────────────────────────────────────────────────────────┘  │
│  [前往抽選批次列表] (primary)   [前往案件列表] (secondary)      │
│                                                                 │
│  --- FAILED variant ---                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ ✕  抽選執行失敗                                          │  │
│  │ {errorReason from response}                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│  [前往抽選批次列表] (primary, to check/retry)                   │
└─────────────────────────────────────────────────────────────────┘
```

**Region 順序原則**: SamplingPageHeader → SamplingRuleSummaryRegion → SamplingPreviewActionZone → SamplingPreviewStatsRegion → SamplingExecuteConfirmZone → SamplingPostExecuteRegion

---

## 4. Variant Set

### V1 — READY_FOR_PREVIEW（初始狀態）

```
SamplingPageHeader          [visible — full project info]
SamplingRuleSummaryRegion   [visible — all rule fields, read-only]
SamplingPreviewActionZone   [visible — [預估母體] CTA only]
SamplingPreviewStatsRegion  [HIDDEN]
SamplingExecuteConfirmZone  [HIDDEN]
SamplingPostExecuteRegion   [HIDDEN]
```

### V2 — PREVIEW_COMPLETED（preview 回傳成功）

```
SamplingPageHeader          [visible]
SamplingRuleSummaryRegion   [visible — collapsible optional]
SamplingPreviewActionZone   [visible — [重新預估] secondary CTA]
SamplingPreviewStatsRegion  [VISIBLE — 3 confirmed cards + 1 conditional]
SamplingExecuteConfirmZone  [VISIBLE — [正式執行抽選] CTA unlocked]
SamplingPostExecuteRegion   [HIDDEN]
```

### V3 — EXECUTING（execute submit in-flight）

```
SamplingPageHeader          [visible]
SamplingRuleSummaryRegion   [visible — read-only]
SamplingPreviewActionZone   [DISABLED / loading]
SamplingPreviewStatsRegion  [visible — frozen stats from preview]
SamplingExecuteConfirmZone  [LOADING — loading indicator replaces CTA]
SamplingPostExecuteRegion   [HIDDEN]
```

### V4 — EXECUTE_SUCCESS

```
SamplingPageHeader          [visible]
SamplingRuleSummaryRegion   [COLLAPSED or HIDDEN]
SamplingPreviewActionZone   [HIDDEN]
SamplingPreviewStatsRegion  [HIDDEN or summary-only]
SamplingExecuteConfirmZone  [HIDDEN]
SamplingPostExecuteRegion   [VISIBLE — SUCCESS variant with batchId + nav CTAs]
```

### V5 — EXECUTE_FAILED

```
SamplingPageHeader          [visible]
SamplingRuleSummaryRegion   [visible — for reference]
SamplingPreviewActionZone   [visible — allow re-preview after failed execute]
SamplingPreviewStatsRegion  [visible — previous stats still shown]
SamplingExecuteConfirmZone  [HIDDEN]
SamplingPostExecuteRegion   [VISIBLE — FAILED variant with errorReason + batch list nav]
```

### V6 — READONLY_BLOCKED

```
SamplingPageHeader          [visible + BLOCKED REASON BANNER inline]
SamplingRuleSummaryRegion   [visible — read-only, no action anchors]
SamplingPreviewActionZone   [HIDDEN or DISABLED with reason label]
SamplingPreviewStatsRegion  [HIDDEN]
SamplingExecuteConfirmZone  [HIDDEN]
SamplingPostExecuteRegion   [HIDDEN]
```

> **Blocked reason 必須明確顯示，4 種可能原因（來自 page spec §7）**:
> 1. project 非 ACTIVE 狀態
> 2. 無已發佈 version
> 3. scope 不匹配
> 4. 未預期錯誤（fallback）
>
> 永遠不能只顯示 disabled button 無說明。

### V5-modal — EXECUTE_CONFIRM_MODAL（overlay on V2）

```
[V2 background, dimmed]
┌──────────────────────────────────────────┐
│ 確認正式執行抽選？                       │
│                                          │
│ 執行後將正式建立抽選批次，不可取消。      │
│                                          │
│ [取消]              [確認執行]           │
└──────────────────────────────────────────┘
```

> 確認 modal 確切文案為 DEFERRED（互動規格層）；結構佔位已畫。

---

## 5. High-Risk Targets

| Region | 風險 | First-pass 處理 |
|---|---|---|
| SamplingPreviewStatsRegion（第4卡） | `api-spec §9.1` preview response 不含 `selectedCount`；page spec §5.4 qualifier "若 preview 能計算"；§11.2 draft 有該欄位。衝突存在。 | 第4卡標注 `[CONDITIONAL]`；實作前須 backend 確認（OQ-S01） |
| SamplingPostExecuteRegion（SUCCESS 時間欄） | `api-spec §9.2` execute response 只含 batchId/status/selectedCount；page spec §5.6 要求顯示 startedAt/finishedAt | SUCCESS variant 時間欄位標注 `[DEFERRED]`；可能需 execute 後 `GET /sampling-batches/{batchId}` 補齊（OQ-S02） |
| V6 READONLY_BLOCKED | 容易實作成無說明的 disabled 頁面 | 已有明確 blocked reason 4 類；wireframe 要求 inline reason display |

---

## 6. Deferred Items

- Confirm modal 確切文案及 visual tone（deferred 至 interaction spec）
- EXECUTING 狀態動畫 / transition（deferred 至 visual baseline）
- Preview stats 是否在 page lifecycle 中 persist（pending API 釐清）
- SamplingPageHeader 元件規格（sampling cluster 尚無元件地圖；此為結構佔位，非正式 C-code）
- Route module 定義（`ui-route-module-map.md` 不涵蓋 sampling cluster）
- View-model spec（`ui-view-model-spec.md` 涵蓋 cases cluster；sampling cluster 尚未定義）

---

## 7. Open Questions

| # | 問題 | 影響範圍 | 優先順序 |
|---|---|---|---|
| OQ-S01 | preview response 是否包含 `selectedCount`？`api-spec §9.1` 未含；page spec §5.4 qualifier "若 preview 能計算"；§11.2 draft 有該欄位。兩份文件衝突。 | SamplingPreviewStatsRegion 第4卡結構 | 高 |
| OQ-S02 | execute response 是否補 startedAt/finishedAt？或前端 execute success 後另 `GET /sampling-batches/{batchId}`？ | SamplingPostExecuteRegion SUCCESS variant | 中 |
| OQ-S03 | 「重新預估」後 SamplingExecuteConfirmZone 是否需要 reset 至 READY_FOR_PREVIEW？ | V2 → re-preview UX flow | 低 |
