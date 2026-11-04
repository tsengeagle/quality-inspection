# Ready Pages — Angular Implementation Handoff

> D-031～D-033 現行規格更新：下文 O1-C／D-021／D-027 與 CLOSED 例外修正的推薦、guard、mode、按鈕、驗收及「可實作」表格均已被 D-032 取代，僅保留歷史交接證據，禁止照其建立 CLOSED 修正入口。提交後不結案、未固定可修訂、具資格由原複審提交者正式結案並固定，CLOSED 禁止 revision／reopen。初審固定前不得進複審；複審永久盲審含衍生分數。新規格以 state-machine、permission-matrix、API 與 UI-010～014 更新為準；初審固定角色依 DD-03；新操作 binding／狀態表示 UNKNOWN，不構成已可實作入口。應用程式未於本輪更新。


> **文件定位**：本文件是對 `p2-implementation-handoff.md` 的全面補充與統合。  
> `p2-implementation-handoff.md` 涵蓋 UI-009/011/012 的詳細 P2 實作規格，本文件在此基礎上：  
> (a) 補充 UI-006/007（Sampling 叢集）與 UI-010/013/014（P1 Cases 叢集）的完整 12-point handoff；  
> (b) 提供跨頁 cross-page 注意事項、must-not-invent 清單以及 implement-now vs. STUB 決策表。

---

## 1. Ready Pages Inventory — 名單與 readiness basis

| 頁面 | Page ID | 叢集 | Archetype | Readiness Basis | 本文件位置 |
|---|---|---|---|---|---|
| 抽選執行頁 | UI-006 | Sampling | SA1 Action/Execute | wireframe first-pass ✅；OQ-S01/S02/S03 均為非阻塞（4th stat card conditional，timing deferred） | §2.1 |
| 抽選批次列表頁 | UI-007 | Sampling | SA2 List/Tracking | wireframe first-pass ✅；OQ-S04 HIGH 採保守 MASKED 策略（非 resolved）；[查看詳情]→STUB | §2.2 |
| 案件列表頁 | UI-009 | Cases | A1 Work Queue | P2 wireframe ✅；`p2-implementation-handoff.md` §6.1 存在詳細規格 | §2.3（摘要） |
| 案件詳情頁 | UI-010 | Cases | A2 Detail/Control Center | P1 wireframe ✅；§8 OQs 均為 design-layer（C24 置放、copywriting），非架構阻塞 | §2.4 |
| 案件分派頁 | UI-011 | Cases | A3 Action/Assignment | P2 wireframe ✅；`p2-implementation-handoff.md` §6.2 存在詳細規格 | §2.5（摘要） |
| 初審作業頁 | UI-012 | Cases | A4 Action/Review Form | P2 wireframe ✅；`p2-implementation-handoff.md` §6.3 存在詳細規格 | §2.6（摘要） |
| 複審作業頁 | UI-013 | Cases | A4 Action/Review Form | P1 wireframe ✅；O1-C（CLOSED exception）已決策於 decision-log §d089；§8 OQs 均為 UX writing-layer | §2.7 |
| Revision 歷程頁 | UI-014 | Cases | A5 History/Traceability | P1 wireframe ✅；O1-C + O3-C 均已回寫於 wireframe；§8 OQs 均為 design/interaction-layer | §2.8 |

**不在本批次（原因）**：

| 頁面 | Page ID | 原因 |
|---|---|---|
| 抽選批次詳情頁 | UI-008 | page spec 不存在；此頁是 STUB 目標，不是實作目標 |

---

## 2. Per-Page Implementation Handoff

---

### 2.1 UI-006 抽選執行頁

**Source authority**: `docs/07-design/wireframes/ui006-sampling-execute-first-pass.md`  
**Archetype**: SA1 Action/Execute（Sampling 叢集專屬；cases archetypes 不適用）

#### 2.1.1 Page role / archetype / responsibility

- 單一行為者頁面（HOSPITAL_MANAGER only）：依序執行「預估 → 確認執行 → 執行結果」。
- 6-mode state machine 驅動 Region 可視性；沒有並行分支，只有線性前進（可以在 FAILED 重回 READY_FOR_PREVIEW）。
- READONLY_BLOCKED 是完整 Mode，不是降級版頁面：BLOCKED reason 必須明確顯示（4 類）。

#### 2.1.2 Route / guard boundary

```yaml
route: TBD  # Sampling 叢集 route module 尚未定義；建議 /projects/:projectId/sampling 或 /sampling/:projectId
guard:
  role_guard: HOSPITAL_MANAGER only
  scope_guard: projectId scope（同院）
note: >
  ui-route-module-map.md 明確標注「範圍：僅 cases 主幹（UI-009~UI-014）」，
  Sampling 叢集 route path 尚未正式定義；實作前需先補充 sampling route module map。
```

#### 2.1.3 Must-have regions

| 序號 | Region | 可視條件 |
|---|---|---|
| R1 | SamplingPageHeader | 所有 Mode；READONLY_BLOCKED 時含 BLOCKED reason banner |
| R2 | SamplingRuleSummaryRegion | 所有 Mode；永遠 read-only |
| R3 | SamplingPreviewActionZone | READY_FOR_PREVIEW / EXECUTE_FAILED 時 CTA enabled；其他 Mode disabled 或隱藏 |
| R4 | SamplingPreviewStatsRegion | PREVIEW_COMPLETED / EXECUTING / EXECUTE_FAILED 可見；3 confirmed 卡片 + 1 conditional（OQ-S01） |
| R5 | SamplingExecuteConfirmZone | 僅 PREVIEW_COMPLETED / EXECUTING 可見；包含 [正式執行抽選] 按鈕＋確認 modal |
| R6 | SamplingPostExecuteRegion | 僅 EXECUTE_SUCCESS / EXECUTE_FAILED 可見；timing fields = STUB（OQ-S02） |

#### 2.1.4 Shared vs page-owned

| Classification | Regions |
|---|---|
| shared（sampling 叢集級） | SamplingPageHeader（結構佔位；sampling cluster component map 尚未建立） |
| page-owned | R2–R6 全部（SA1 無跨頁共用 region 定義） |

#### 2.1.5 Action / mode / blocked grammar

| Mode | Region 可視 | CTA 狀態 |
|---|---|---|
| READY_FOR_PREVIEW | R1 R2 R3 | [預估抽選人數] enabled |
| PREVIEW_COMPLETED | R1 R2 R3 R4 R5 | [重新預估] enabled；[正式執行抽選] enabled（→ confirm modal） |
| EXECUTING | R1 R2 R3(disabled) R4(frozen) R5(loading indicator) | 所有 CTA loading/disabled；modal 隱藏 |
| EXECUTE_SUCCESS | R1 R6(success) | 無 command CTA |
| EXECUTE_FAILED | R1 R2 R3 R4 R6(failed) | [重新預估] re-enabled；執行 CTA hidden |
| READONLY_BLOCKED | R1(含 banner) R2 | 所有 action zone hidden；BLOCKED reason 必須顯示 |

BLOCKED reason 4 類：project 非 ACTIVE、同版本已有 COMPLETED 批次、project CLOSED、permission revoked。

#### 2.1.6 Feature module / route structure

```
features/sampling/
  sampling-execute/
    SamplingExecutePageComponent    ← Container（owns mode, calls API）
    SamplingRuleSummaryComponent    ← Presentational
    SamplingPreviewStatsComponent   ← Presentational
    SamplingExecuteConfirmModal     ← Modal（receives open$, emits confirm/cancel）
    SamplingPostExecuteComponent    ← Presentational
```

Route: lazy-loaded under `features/sampling/`

#### 2.1.7 Container vs presentational split

- **Container** `SamplingExecutePageComponent`：完整擁有 Mode state；呼叫 API；drives Region visibility。
- **Presentational**（一律 stateless）：SamplingRuleSummaryComponent / SamplingPreviewStatsComponent / SamplingPostExecuteComponent；接收 mode + data inputs，emit 事件。
- **Modal** `SamplingExecuteConfirmModal`：receives `open: boolean` input，emits `confirmed/cancelled`；不持有業務 state。

#### 2.1.8 State / view-model boundary

```typescript
interface SamplingExecutePageState {
  mode: SamplingExecuteMode; // READY_FOR_PREVIEW | PREVIEW_COMPLETED | EXECUTING | EXECUTE_SUCCESS | EXECUTE_FAILED | READONLY_BLOCKED
  ruleParams: SamplingRuleSnapshot | null;
  previewStats: SamplingPreviewStats | null;   // set after POST /preview
  batchId: string | null;                       // set after POST /execute success
  blockedReason: SamplingBlockedReason | null; // one of 4 categories
  loading: boolean;
  error: ApiError | null;
}
```

View-model resolved from state + role（SA1 無需多 role 分支）。

#### 2.1.9 Test-first order

1. Route guard：非 MANAGER → 403 / redirect
2. Mode resolver 正確性：各 mode transition 驅動正確 Region 可視性
3. READONLY_BLOCKED：4 類 blocked reason 分別呈現正確文案 / banner
4. Preview CTA → POST `/sampling-batches/preview` → mode → PREVIEW_COMPLETED；stats 呈現
5. Execute CTA → confirm modal → [確認執行] → POST `/sampling-batches` → EXECUTING → SUCCESS / FAILED
6. EXECUTE_FAILED：[重新預估] re-enabled；前次 stats 保留

#### 2.1.10 Deferred / STUB items

| 項目 | 狀態 | 說明 |
|---|---|---|
| SamplingPageHeader spec | STUB（結構佔位） | Sampling cluster component map 尚未建立 |
| Post-execute timing fields | STUB | OQ-S02：startedAt/finishedAt 可能需要額外 GET |
| EXECUTING animation / transition | DEFERRED | 留待 visual baseline |
| Confirm modal 確認文案精確 copy | DEFERRED | 結構已定，copy 由 interaction spec 決定 |
| Sampling cluster route path | DEFERRED | sampling route module 尚未定義 |
| 4th preview stat card | CONDITIONAL | 視 api-spec §9.1 preview response 是否含 selectedCount（OQ-S01） |

#### 2.1.11 Implementation cautions

- **絕對不可把 6 modes 實作成 6 個獨立 routes**；整個執行流程在同一個 page component 切換 mode。
- READONLY_BLOCKED 不可以「全部 action disabled」代替；blocked reason banner 是 UI 必要元素。
- SamplingExecuteConfirmZone 的 confirm modal 是第二確認關卡；不可省略。
- 不可以 prototype-react artifacts 作為 production component API 依據。
- OQ-S01 尚未解決：4th stat card 若 backend 未提供 selectedCount，不可顯示任何推算數字。

#### 2.1.12 Pending confirmation before implementation

- **OQ-S01（HIGH）**：preview API response 是否含 selectedCount？
- **Sampling cluster route path（BLOCKING）**：實作前必須先定義 sampling route module。
- SamplingPageHeader 精確共用邊界（需補充 sampling cluster component map）。

---

### 2.2 UI-007 抽選批次列表頁

**Source authority**: `docs/07-design/wireframes/ui007-batch-list-first-pass.md`  
**Archetype**: SA2 List/Execution Tracking（Sampling 叢集專屬）

#### 2.2.1 Page role / archetype / responsibility

- 多角色列表追蹤頁：MANAGER 追蹤批次並可重跑 FAILED；AUDITOR 唯讀；SYSADMIN 僅看 metadata。
- **不是** work queue（無 ownership 過濾）；**不是** case list（無 case 直接 row action）。
- Role 在 component init 時決定 tab preset、column 集合、row action 集合。

#### 2.2.2 Route / guard boundary

```yaml
route: TBD  # Sampling 叢集 route module 尚未定義；建議 /sampling/batches
guard:
  role_guard: HOSPITAL_MANAGER ✅ | AUDITOR ✅ | SYSTEM_ADMIN ✅（metadata-only）
  reviewer_rereviewer: PENDING（OQ-S05），現行：視為 deny
cross_cluster_navigation:
  from: UI-007 [查看案件] → /cases?batchId={batchId}
note: Sampling cluster route path 尚未定義
```

#### 2.2.3 Must-have regions

| 序號 | Region | 說明 |
|---|---|---|
| R1 | BatchListPageHeader | title + role-summary + quick stats；SYSADMIN 加 ProjectionNoticeBanner inline |
| R2 | BatchStatusTabZone | role-driven tab preset（見下方矩陣） |
| R3 | BatchFilterBar | metadata-safe fields for SYSADMIN；full fields for MANAGER/AUDITOR |
| R4 | BatchListTable | role-driven columns + row action grammar |
| R5 | BatchPagingZone | 標準 page/pageSize/totalCount |
| V4 | Empty state | table 區替換為空狀態說明 |
| V6 | Forbidden/Error state | table 區替換為 403 / error banner |

**Role Tab Preset 矩陣**：

| Role | Tabs |
|---|---|
| MANAGER | [全部批次] [成功] [失敗] [可重跑] |
| AUDITOR | [全部批次] [成功] [失敗] |
| SYSADMIN | [全部批次] [失敗] |
| REVIEWER/REREVIEWER | PENDING（OQ-S05），現行 deny |

#### 2.2.4 Shared vs page-owned

| Classification | Scope |
|---|---|
| shared（sampling 叢集級） | BatchListPageHeader（structural placeholder） |
| feature-scoped（sampling module） | BatchStatusTabZone / BatchFilterBar / BatchListTable / BatchPagingZone |
| page-owned | Empty state / Forbidden state |

#### 2.2.5 Action / mode / blocked grammar

| Role | Column 集合 | Row actions |
|---|---|---|
| MANAGER | 完整：timing / status / selectedCount 等 | [查看詳情]→STUB；[查看案件]→UI-009；FAILED rows: [重跑]→V5 modal |
| AUDITOR | 完整；hospitalId scope limited | [查看詳情]→STUB；[查看案件]→UI-009；無 [重跑] |
| SYSADMIN | metadata 白名單（D-010）；stats columns MASKED | [查看詳情]→STUB only |

**D-013 語意固定（不可更改）**：
> Retry confirm modal 文案必須包含「將以新的抽選批次重跑，原失敗批次會保留」；  
> 不得暗示「用最新版本重跑」或「覆蓋原批次」。

#### 2.2.6 Feature module / route structure

```
features/sampling/
  batch-list/
    SamplingBatchListPageComponent  ← Container
    BatchStatusTabComponent         ← Presentational
    BatchFilterBarComponent         ← Presentational
    BatchListTableComponent         ← Presentational
    BatchRowActionsComponent        ← Presentational
    RetryConfirmModalComponent      ← Modal
```

Route: lazy-loaded under `features/sampling/`（與 UI-006 共用同一 feature module）

#### 2.2.7 Container vs presentational split

- **Container** `SamplingBatchListPageComponent`：role、tab state、filter state、data load，驅動所有 presentational 的 inputs。
- **Presentational** 一律 stateless；emits 事件（tab change / filter change / row action）。
- **Modal** `RetryConfirmModalComponent`：receives `batch: BatchSummary | null` input；emits `confirmed`。

#### 2.2.8 State / view-model boundary

```typescript
// Query-driven；無持久 form state
interface BatchListQueryState {
  activeTab: BatchStatusTab;
  filters: BatchFilterCriteria;
  pagination: PageRequest;
}

interface BatchListDataState {
  batches: Page<SamplingBatchSummary>;
  loading: boolean;
  error: ApiError | null;
}

// Role projection 決定：tab options + visible columns + action visibility
// SYSADMIN：stats columns MASKED（populationCount/eligibleCount/excludedCount/selectedCount）
```

#### 2.2.9 Test-first order

1. Route guard：REVIEWER/REREVIEWER（現行 deny）→ 403
2. Role → tab preset：MANAGER 有 [可重跑]；AUDITOR 無；SYSADMIN 只有 [全部批次][失敗]
3. MANAGER FAILED row：[重跑] visible；confirm modal V5 text 符合 D-013 FIXED 語意
4. SYSADMIN：stats columns MASKED；timing/metadata columns visible；無 [查看案件] / [重跑]
5. [查看詳情] → STUB placeholder（不測試真實 navigation）
6. [查看案件] → navigate `/cases?batchId={batchId}`

#### 2.2.10 Deferred / STUB items

| 項目 | 狀態 | 說明 |
|---|---|---|
| [查看詳情] row action | STUB | UI-008 page spec 不存在；action present but leads nowhere |
| sampling cluster route path | DEFERRED | Sampling route module 尚未定義 |
| 批次即時狀態 refresh | DEFERRED | api-spec 無 SSE / polling spec |
| 匯出功能 | DEFERRED | api-spec 無匯出端點 |
| retryOfBatchId chain display | DEFERRED | 資料欄位未在 api-spec 確認 |
| SYSADMIN column 精確白名單 | MASKED（保守做法） | OQ-S04：pending confirmation |

#### 2.2.11 Implementation cautions

- **[重跑] confirm modal text 必須使用 D-013 固定語意**；不可自行調整文案。
- Tab preset 必須由 role 在 component init 決定；不可讓 AUDITOR 看到 [可重跑] tab。
- SYSADMIN stats column masking 需要服務端 projection + 前端 guardian 雙重保障。
- [查看詳情] 全程標示 STUB；不可自行假設 UI-008 route path。
- 不可實作 real-time refresh（無 API spec 基礎）。

#### 2.2.12 Pending confirmation before implementation

- **OQ-S04（HIGH）**：SYSADMIN SamplingBatch metadata whitelist 精確欄位。
- **OQ-S05（MID）**：REVIEWER/REREVIEWER 是否有合法訪問路徑。
- **Sampling cluster route path（BLOCKING）**：實作前需先定義。

---

### 2.3 UI-009 案件列表頁（參照 p2 handoff）

**完整規格位置**：`docs/06-frontend/p2-implementation-handoff.md` §6.1

**12-point 摘要**：

1. **Role**：A1 Work Queue；role-default preset tabs；非 dashboard。
2. **Route**：`/cases`；guard = `role+scope guard + projection guard`；lazy-load boundary = worklist。
3. **Must-have regions**：C01 / C04（SYSADMIN 專有）/ C05 FilterBar / C06 Table / C07 RowActionSet / paging / forbidden + empty + error states。
4. **Shared**：C01/C04 shared；C05/C06/C07 feature-scoped `cases-worklist`。
5. **Action grammar**：Row-level navigate-only；無頁面級 command；action visibility = role + state + scope。
6. **Module**：`cases-worklist` feature module；`CaseListPageComponent` container。
7. **Container/Presentational split**：Container owns query state + role projection；Presentational components stateless。
8. **State**：Query-driven；tab preset = role-default；metadata-only = same route same projection。
9. **Test-first order**：metadata projection；route guard；role tab preset；action visibility；[查看案件] row navigate。
10. **STUB**：bulk action / export（無 api-spec）；advanced filter（not in this round）。
11. **Cautions**：tab ≠ sub-page component；metadata-only ≠ 獨立 route；不以 prototype row count 假設分頁設定。
12. **Pending**：見 `p2-implementation-handoff.md` §9.2。

---

### 2.4 UI-010 案件詳情頁

**Source authority**: `docs/07-design/wireframes/ui010-case-detail-first-pass.md`  
**Archetype**: A2 Detail/Control Center

#### 2.4.1 Page role / archetype / responsibility

- **Control center（閱讀樞紐 + navigate hub）**，勿以 command page 理解。
- 呈現 case 摘要、初審/複審 submission summary、lifecycle 進度、以及導向各 command page 的 CTA。
- 不接受任何 form 提交；所有 action 均為 navigate。

#### 2.4.2 Route / guard boundary

```yaml
route: /cases/:caseId
featureModule: cases-detail
guard:
  role_guard: all roles with case access
  scope_guard: hospitalId match
  projection_guard: metadata-only (SYSADMIN) vs FULL
page_mode_level:
  status_guard: determines available navigate CTAs
mode_outputs:
  FULL_CONTROL_CENTER: HOSPITAL_MANAGER; all navigate CTAs visible
  ROLE_LIMITED: REVIEWER / REREVIEWER; section 可見性限制（C10/C11，見 OQ §4）
  METADATA_CONTROL_CENTER: SYSTEM_ADMIN; C04 banner; metadata-only fields
  READONLY_TERMINAL: CLOSED/CANCELLED（無 exception 條件）; command CTAs hidden
lazyLoadBoundary: detail-center boundary
queryStatePolicy: preserve return query context from worklist
```

#### 2.4.3 Must-have regions

| Region | Classification | 可視條件 |
|---|---|---|
| C01 CaseSummaryHeader | shared | 所有 Mode |
| C02 StatusOrBlockedBanner | shared | terminal / forbidden 時 |
| C03 ModeIndicatorStrip | shared | 所有 Mode（metadata friendly text = OQ §3） |
| C04 ProjectionNoticeBanner | shared | METADATA_CONTROL_CENTER only |
| C08 CaseControlCenterNavZone | feature-scoped | FULL / ROLE_LIMITED（CTAs 隨 status 動態） |
| C09 InitialReviewSummaryRegion | feature-scoped | 有初審 submission 時 |
| C10 RereviewSummaryRegion | feature-scoped | 條件可見（OQ §4：REVIEWER/REREVIEWER 不同） |
| C11 LifecycleProgressRegion | feature-scoped | 所有 Mode |
| C24 RevisionHistoryJumpEntry | page-owned | 有 revision 時；位置 DEFERRED（OQ §1） |

#### 2.4.4 Shared vs page-owned

| Classification | Regions |
|---|---|
| shared | C01 / C02 / C03 / C04 |
| feature-scoped（cases-detail） | C08 / C09 / C10 / C11 |
| page-owned | C24（置放 DEFERRED） |

#### 2.4.5 Action / mode / blocked grammar

- **FULL_CONTROL_CENTER**：完整 navigate CTAs（→ assign / initial-review / rereview / revisions）；依 case status 動態縮減。
- **ROLE_LIMITED**（REVIEWER/REREVIEWER）：initial review 入口可見若 ownership + status 合法；採最小收斂規則為「REVIEWER 顯示 C09 + C11、REREVIEWER 顯示 C09 + C10 + C11」，不延伸為 command flow。
- **METADATA_CONTROL_CENTER**（SYSADMIN）：C04 banner；metadata whitelist fields 僅顯示 O3-C shared base + UI-010 extension；無任何 command CTA。
- **READONLY_TERMINAL**（CLOSED/CANCELLED，非例外條件）：C02 terminal notice；command CTAs hidden；C24 jump entry 仍顯示但 CTA 視 case state。

**注意**：CLOSED exception entry 入口在 **UI-014**（不在 UI-010）。

#### 2.4.6 Feature module / route structure

```
features/cases/
  case-detail/
    CaseDetailPageComponent         ← Container（resolves mode from role+state）
    CaseSummaryHeader               ← shared（C01）
    StatusOrBlockedBanner           ← shared（C02）
    ModeIndicatorStrip              ← shared（C03）
    ProjectionNoticeBanner          ← shared（C04，conditional）
    CaseControlCenterNavComponent   ← feature-scoped（C08）
    InitialReviewSummaryComponent   ← feature-scoped（C09）
    RereviewSummaryComponent        ← feature-scoped（C10）
    LifecycleProgressComponent      ← feature-scoped（C11）
```

#### 2.4.7 Container vs presentational split

- **Container**：resolves mode，load case detail via route resolver；drives all child inputs。
- **Presentational** 全部 stateless，emits navigate 事件。
- **無 form submission**；純 read + navigate。

#### 2.4.8 State / view-model boundary

```typescript
interface CaseDetailViewModel {
  mode: CaseDetailMode; // FULL_CONTROL_CENTER | ROLE_LIMITED | METADATA_CONTROL_CENTER | READONLY_TERMINAL
  caseContext: CaseContextViewModel;
  initialReviewSummary: ReviewSummaryViewModel | null;
  rereviewSummary: ReviewSummaryViewModel | null;
  availableNavActions: NavigateActionConfig[];   // derived from mode + status
  metadataOnly: boolean;
  blockedReason?: string;
}
```

O3-C projection：SYSADMIN 僅可見 shared base + UI-010 extension whitelist，與 UI-009/UI-014 whitelist 不同。

#### 2.4.9 Test-first order

1. Route guard：scope mismatch → forbidden
2. Mode resolution：各 role + case status → 正確 mode
3. METADATA_CONTROL_CENTER：C04 visible；FULL-only fields absent
4. READONLY_TERMINAL：C02 shown；command CTAs hidden
5. ROLE_LIMITED：REVIEWER/REREVIEWER 各自 InitialReview/Rereview section 可見性
6. Navigate CTAs：CLOSED + non-exception → assign/review CTAs hidden

#### 2.4.10 Deferred / STUB items

| 項目 | 狀態 | 說明 |
|---|---|---|
| C24 RevisionHistoryJumpEntry 置放位置 | DEFERRED | OQ §1：C11 之後 vs. 側邊欄，待 prototype |
| C03 METADATA_CONTROL_CENTER 文案 | DEFERRED | OQ §3：「metadata-only」是否直接外露 |
| C10/C11 ROLE_LIMITED 可見性細則 | CONDITIONAL | OQ §4：需確認 permission-matrix |
| CLOSED exception entry | **非本頁責任** | 入口在 UI-014（C22），不在 UI-010 |

#### 2.4.11 Implementation cautions

- **UI-010 是 A2 control center，不是 command page**：不得有任何表單提交 / save / submit 行為。
- 不可把 metadata-only 做成獨立 route `/cases/:caseId/meta`；projection 在同路由內切換。
- CLOSED exception 入口不在 UI-010；不可自行在此頁新增任何 revision entry button。
- C10/C11 REVIEWER/REREVIEWER 可見性規則由 permission-matrix 決定，不得以 UI 直覺法則替代。

#### 2.4.12 Pending confirmation before implementation

- C10/C11 ROLE_LIMITED 已採最小收斂規則：REVIEWER 不顯示 C10、REREVIEWER 可見 C10；兩者均保留 C09 + C11。
- C24 最終置放位置（OQ §1：待 prototype 驗證）
- O3-C UI-010 extension whitelist 已由 `api-spec.md` §3.6 與 `permission-matrix.md` §5.1 固定；不再作為 Wave 3 pending。

---

### 2.5 UI-011 案件分派頁（參照 p2 handoff）

**完整規格位置**：`docs/06-frontend/p2-implementation-handoff.md` §6.2

**12-point 摘要**：

1. **Role**：A3 Action/Assignment；頁面級 submit zone（非 fixed bottom bar）。
2. **Route**：`/cases/:caseId/assign`；guard = `role hard guard (MANAGER)` + scope + status；isolated command boundary。
3. **Must-have regions**：C01 / C02 / C03 / C12(初審 assignment section) / C13(複審 assignment section) / C14(scope-aware picker) / C23(due-date helper slot) / 頁面上方 submit + back。
4. **Shared**：C01/C02/C03 shared；C12/C13/C14/C23 feature-scoped `cases-assignment-command`。
5. **Mode grammar**：ASSIGN_INITIAL / REASSIGN_INITIAL / MIXED_ASSIGNMENT / READONLY_BLOCKED；C12/C13 sections 必須分開，不可合并。
6. **Module**：`cases-assignment-command`；`CaseAssignmentPageComponent` container。
7. **Container/Presentational**：Container owns mode + assignment state；不共享 cases-detail state。
8. **State**：assignment form state（reviewerId / rereviewer Id）；blocked reason。
9. **Test-first order**：guard（非 MANAGER → 403）；mode resolution；C12/C13 section 分離；submit flow。
10. **STUB**：C23 due-date helper（slot only）；picker 深層互動細節（prototype 只確認位置，非 picker spec）。
11. **Cautions**：C12/C13 reviewer 與 rereviewer 欄位必須實體分開；不可共用同一 input 元件。
12. **Pending**：見 `p2-implementation-handoff.md` §9.2。

---

### 2.6 UI-012 初審作業頁（參照 p2 handoff）

**完整規格位置**：`docs/06-frontend/p2-implementation-handoff.md` §6.3

**12-point 摘要**：

1. **Role**：A4 Action/Review Form；C16 ReviewCommandBar 固定 viewport bottom。
2. **Route**：`/cases/:caseId/initial-review`；guard = role + ownership + status；isolated command boundary；optional `?intent=draft|revision` query。
3. **Must-have regions**：C01 / C02 / C03 / C15 ReviewFormSummaryStrip / main form area / commentary + attachment zone / C17 ReviewVersionSummaryRegion / C16 ReviewCommandBar。
4. **Shared**：C01/C02/C03/C15/C16/C17 shared；main form = page-owned composition。
5. **Mode grammar**：NEW / EDIT_DRAFT / REVISION / READONLY_BLOCKED；append-only revision = domain truth。
6. **Module**：`cases-initial-review-command`；`InitialReviewPageComponent` container。
7. **Container/Presentational**：Container owns draft + mode；C16/C17 receive mode input。
8. **State**：draft state；submission basedOn reference（REVISION mode）；command config。
9. **Test-first order**：guard（ownership miss → deny）；REVISION append-only；C16 command bar label per mode；draft save/restore。
10. **STUB**：score sheet item-level form controls（not spec'd this round）；attachment deep interaction。
11. **Cautions**：REVISION ≠ edit existing；C16 不可移動到其他位置；main form 需預留 C16 底部 padding。
12. **Pending**：見 `p2-implementation-handoff.md` §9.2。

---

### 2.7 UI-013 複審作業頁

**Source authority**: `docs/07-design/wireframes/ui013-rereview-first-pass.md`  
**Archetype**: A4 Action/Review Form（rereview variant）

#### 2.7.1 Page role / archetype / responsibility

- **同 UI-012 的 A4 家族**，但 rereview 語意不同：含有 CLOSED_REVISION_EXCEPTION mode（O1-C 決策，decision-log §d089）。
- CLOSED_REVISION_EXCEPTION：只有原 rereviewer 可進入；**REVISE enabled，NEW SUBMIT disabled**。
- READONLY_BLOCKED 與 CLOSED_REVISION_EXCEPTION 是完全不同的 mode；不可混淆。

#### 2.7.2 Route / guard boundary

```yaml
route: /cases/:caseId/rereview
featureModule: cases-rereview-command
guard:
  role_guard: REREVIEWER ownership
  status_guard: rereview-in-progress OR (CLOSED + exception conditions)
  exception_conditions:
    - case.status == CLOSED
    - actor == original_rereviewer
    - has_base_rereview_submission
mode_outputs:
  standard: NEW | EDIT_DRAFT | REVISION
  exception: CLOSED_REVISION_EXCEPTION
  blocked: READONLY_BLOCKED
lazyLoadBoundary: isolated command boundary
```

**Guard 重點**：相同的 CLOSED status 需要產生 CLOSED_REVISION_EXCEPTION（例外允許）或 READONLY_BLOCKED（一般封鎖），取決於 exception conditions 的 3 個條件；不可只看 status 就決定 mode。

#### 2.7.3 Must-have regions

| Region | Classification | 說明 |
|---|---|---|
| C01 CaseSummaryHeader | shared | 含 CLOSED exception context |
| C02 StatusOrBlockedBanner | shared | CLOSED exception 用 warning tone（amber）；BLOCKED 用 blocked tone |
| C03 ModeIndicatorStrip | shared | CLOSED exception label：`例外修正（已結案）`（D-013）|
| C15 ReviewFormSummaryStrip | shared（A4） | rereview-focused content |
| main rereview form area | page-owned | rereview score form（score items = STUB） |
| commentary / attachment zone | page-owned | placeholder |
| C17 ReviewVersionSummaryRegion | shared（A4） | contains basedOn reference for REVISION |
| C18 InitialSummaryReferenceRegion | page-owned | read-only reference; not upgrade to shared |
| C19 FinalDecisionSummaryRegion | page-owned | final score 骨架（live calc = DEFERRED OQ §2） |
| C16 ReviewCommandBar | shared（A4） | viewport-fixed bottom；label 依 mode 切換 |

#### 2.7.4 Shared vs page-owned

| Classification | Regions |
|---|---|
| shared（A4 家族） | C01 / C02 / C03 / C15 / C16 / C17 |
| page-owned | C18 / C19 / main form area / commentary+attachment |

#### 2.7.5 Action / mode / blocked grammar

| Mode | C16 Commands | C02 tone |
|---|---|---|
| NEW | [提交複審] | — |
| EDIT_DRAFT | [更新草稿] [提交複審] | — |
| REVISION | [修正後提交複審] | — |
| CLOSED_REVISION_EXCEPTION | [提交複審修正（例外）] label DEFERRED | amber warning |
| READONLY_BLOCKED | — | blocked |

**CLOSED_REVISION_EXCEPTION 命令規則**：
- REVISE（修正）操作 enabled。
- 新的 NEW SUBMIT **disabled**；只允許 revision-type submit。
- C16 submit button label 待 UX writing 決定（OQ §1）。

#### 2.7.6 Feature module / route structure

```
features/cases/
  rereview/
    RereviewPageComponent           ← Container（resolves mode incl. exception）
    shared A4 components: C15/C16/C17  ← shared
    InitialSummaryReferenceComponent ← page-owned C18
    FinalDecisionSummaryComponent    ← page-owned C19（骨架）
    RereviewFormRegion               ← page-owned（score items = STUB）
```

#### 2.7.7 Container vs presentational split

- 與 UI-012 相同的 A4 container pattern。
- Container owns mode resolution；CLOSED exception guard 在 route resolver 或 route guard 中計算。
- A4 shared components（C15/C16/C17）接收 mode input。

#### 2.7.8 State / view-model boundary

```typescript
interface RereviewPageViewModel {
  mode: A4RereviewMode; // NEW | EDIT_DRAFT | REVISION | CLOSED_REVISION_EXCEPTION | READONLY_BLOCKED
  caseContext: CaseContextViewModel;
  currentSubmission: RereviewSubmissionViewModel | null;
  basedOnSubmission: RereviewSubmissionViewModel | null; // REVISION mode
  commandConfig: CommandBarConfig; // derived from mode
  isExceptionMode: boolean; // true = CLOSED_REVISION_EXCEPTION
}
```

#### 2.7.9 Test-first order

1. Route guard：非 rereviewer ownership → deny
2. **CLOSED + original rereviewer + has base submission → CLOSED_REVISION_EXCEPTION**（不是 READONLY_BLOCKED）
3. CLOSED + 非 original rereviewer → READONLY_BLOCKED
4. CLOSED_REVISION_EXCEPTION：REVISE enabled；NEW SUBMIT **disabled**；C03 label = `例外修正（已結案）`；C02 = amber warning tone
5. Standard REVISION mode：append-only；C17 shows basedOn reference
6. C16 command bar label 隨 mode 切換

#### 2.7.10 Deferred / STUB items

| 項目 | 狀態 | 說明 |
|---|---|---|
| CLOSED exception C16 submit button label | DEFERRED | OQ §1：UX writing 決定 |
| C19 FinalDecision live score calculation | DEFERRED | OQ §2：submit-time vs live calc |
| C02/C03 priority 在 exception 同時出現時 | DEFERRED | OQ §3 |
| revision draft save | STUB/PENDING | api-spec 未明確；標 pending confirmation |
| score sheet item-level form controls | STUB 佔位區域 | 不在本輪 spec 範圍 |
| commentary/attachment 深層互動 | STUB 佔位 | 後續 component planning |

#### 2.7.11 Implementation cautions

- **CLOSED_REVISION_EXCEPTION（amber/warning）必須視覺上明顯區別於 READONLY_BLOCKED（blocked）**。
- CLOSED_REVISION_EXCEPTION 的 submit button 必須 DISABLED；只有 revise-type submit enabled。
- Mode 不可由 URL param 決定；mode 由 case state + ownership + role 的 resolver 計算。
- UI-013 承接 A4 grammar，但 CLOSED exception 邏輯不繼承 UI-012 的任何 mode；兩頁業務語意獨立。

#### 2.7.12 Pending confirmation before implementation

- revision draft save API 可行性（api-spec 需更新）
- C16 button label for CLOSED_REVISION_EXCEPTION（UX writing）
- C19 live score calculation timing

---

### 2.8 UI-014 Revision 歷程頁

**Source authority**: `docs/07-design/wireframes/ui014-revision-history-first-pass.md`  
**Archetype**: A5 History/Traceability

#### 2.8.1 Page role / archetype / responsibility

- **Timeline page + navigate-only action zone**：無任何 form submission。
- 呈現 revision 歷程 timeline，每個 stage 有各自的 current effective marker。
- 如果 CLOSED case + original rereviewer + has base submission，則 C22 導頁區出現例外入口（navigate-only to UI-013）。
- C22 navigate entry 是 **navigate link**，不是 submit button。

#### 2.8.2 Route / guard boundary

```yaml
route: /cases/:caseId/revisions
featureModule: cases-revision-history
guard:
  role_guard: all roles with case access
  scope_guard: hospitalId match
  projection_guard: METADATA_HISTORY (SYSADMIN) vs FULL_HISTORY
page_mode_level:
  exception_guard:
    condition: CLOSED + original_rereviewer + has_base_submission
    → adds conditional C22 navigate entry to UI-013
queryStatePolicy:
  persist: stage_tab (ALL/INITIAL_ONLY/REREVIEW_ONLY), selected_revision, compare_target
lazyLoadBoundary: history boundary
```

#### 2.8.3 Must-have regions

| Region | Classification | 說明 |
|---|---|---|
| C01 CaseSummaryHeader | shared | 全 Mode |
| C02 StatusOrBlockedBanner | shared | terminal notice / exception amber hint |
| C03 ModeIndicatorStrip | shared | METADATA_HISTORY 文案 = OQ §4（待 UX writing） |
| C04 ProjectionNoticeBanner | shared | METADATA_HISTORY only |
| C20 RevisionTimelineRegion | feature-scoped | stage tabs；current effective badge；superseded badge |
| C21 RevisionCompareSummaryRegion | feature-scoped | FULL vs METADATA_ONLY variants（see HR-1） |
| C22 RevisionNavigationActionZone | feature-scoped | navigate-only；CLOSED exception 條件性出現 |

#### 2.8.4 Shared vs page-owned

| Classification | Regions |
|---|---|
| shared | C01 / C02 / C03 / C04 |
| feature-scoped（cases-revision-history） | C20 / C21 / C22 |

#### 2.8.5 Action / mode / blocked grammar

| Mode | C21 可見內容 | C22 行為 |
|---|---|---|
| FULL_HISTORY | 完整 revision data；compare 可用 | back / detail navigate；**無 submit** |
| METADATA_HISTORY（SYSADMIN） | 僅 O3-C whitelist：`stage / revisionNo / submittedAt / submittedBy / currentEffective`；FULL-only fields = `—`；顯示 compare notice | STUB (無業務 command) |
| CLOSED exception 條件成立 | 同 FULL_HISTORY | **加顯** warning tone outlined navigate link → UI-013（CLOSED_REVISION_EXCEPTION） |
| READONLY_TERMINAL（CLOSED 無例外） | 同 FULL_HISTORY | 無例外入口 |

**HR-1 FULL-only fields 處理規則**：METADATA_ONLY 時，比較區固定顯示「詳細差異內容不適用於此視角」；不顯示 score/diff 題數摘要。

**HR-2 C22 CLOSED exception entry language**：
- C22 = navigate action（warning tone outlined button / link）。
- 視覺語意必須與 C16 command bar（UI-013）顯著不同。
- 不是 submit button；不在歷程頁直接提交任何內容。

#### 2.8.6 Feature module / route structure

```
features/cases/
  revision-history/
    RevisionHistoryPageComponent    ← Container（resolves mode + exception conditions）
    RevisionTimelineComponent       ← feature-scoped C20
    RevisionCompareSummaryComponent ← feature-scoped C21（FULL vs METADATA_ONLY variants）
    RevisionNavActionZoneComponent  ← feature-scoped C22
```

#### 2.8.7 Container vs presentational split

- **Container**：resolves mode；detects exception conditions；loads revision history。
- **C20 RevisionTimeline**：primarily presentational（receives revision list；marks current effective per stage）。
- **C22**：receives mode + exception entry config；emits navigate action；never emits submit。

#### 2.8.8 State / view-model boundary

```typescript
interface RevisionHistoryViewModel {
  mode: A5Mode; // FULL_HISTORY | METADATA_HISTORY | READONLY_TERMINAL
  hasExceptionEntry: boolean;  // CLOSED + original rereviewer + base submission
  stages: RevisionStageViewModel[]; // each: timeline cards + current effective marker + superseded markers
  selectedComparison?: [RevisionRef, RevisionRef]; // OQ §3：interact spec DEFERRED
  stageTabs: StageTabConfig; // ALL / INITIAL_ONLY / REREVIEW_ONLY
  metadataOnly: boolean;
}
```

#### 2.8.9 Test-first order

1. Route guard：scope mismatch → forbidden
2. FULL_HISTORY：complete revision data；effective version badge visible in each stage
3. METADATA_HISTORY（SYSADMIN）：C21 only shows O3-C whitelist；FULL-only fields = `—`；C04 visible
4. CLOSED + original rereviewer + base submission → C22 shows navigate link to UI-013
5. CLOSED 無例外條件 → C22 無例外入口（READONLY_TERMINAL）
6. C22 elements emit navigate；never emit submit

#### 2.8.10 Deferred / STUB items

| 項目 | 狀態 | 說明 |
|---|---|---|
| Compare version selection 互動（如何選兩版） | DEFERRED | OQ §3：interaction spec 層；pending |
| Stage tab 預設切換策略（route resolver vs component init） | DEFERRED | OQ §2 |
| C20 timeline card hover / expand | DEFERRED | 只畫靜態骨架 |
| C03 METADATA_HISTORY mode strip 文案 | DEFERRED | OQ §4：是否提示「metadata-only 不可比較」 |
| effective version badge 的 design token | DEFERRED | 色彩/視覺 token 留待 visual baseline |

#### 2.8.11 Implementation cautions

- **C22 CLOSED exception entry 必須是 navigate-only**；實作為 `routerLink` 或 navigate call；不是 `(click)` 直接提交。
- effective version badge 必須在每個 stage 的 timeline card 中明確標示（current vs superseded）。
- METADATA_HISTORY 的 C21 必須嚴格服從 O3-C whitelist；不可顯示 score / diff / 題數等 FULL-only fields。
- C22 視覺語意必須顯著不同於 C16 command bar；採 warning tone outlined secondary action（非 primary action）。

#### 2.8.12 Pending confirmation before implementation

- Compare version 選擇 interaction spec（需 interaction spec layer 確認，OQ §3）
- Stage tab pre-selection 採 page spec 建議值作為 frontend default：manager/auditor=`ALL`、reviewer=`INITIAL_ONLY`、rereviewer=`REREVIEW_ONLY`；屬前端預設，不宣稱為 DDD truth。
- C21 METADATA_HISTORY O3-C whitelist 已由 `api-spec.md` §3.6 與 `permission-matrix.md` §5.1 固定；不再作為 Wave 3 pending。

---

## 3. Cross-Page Implementation Notes

### 3.1 叢集邊界與 feature module 分離

**Cases 叢集（UI-009~014）**：

```
features/cases/
  cases-worklist/          → UI-009
  case-detail/             → UI-010
  cases-assignment-command/→ UI-011
  cases-initial-review/    → UI-012
  cases-rereview/          → UI-013
  revision-history/        → UI-014
```

**Sampling 叢集（UI-006/007）**：

```
features/sampling/
  sampling-execute/        → UI-006
  batch-list/              → UI-007
```

⚠️ **兩個叢集必須隔離**：不可在 cases feature module 中引用 sampling services，反之亦然。  
**唯一的跨叢集 navigation 契約**：UI-007 [查看案件] → `/cases?batchId={batchId}`（queryParam navigate，不是 service 呼叫）。

### 3.2 Shared components 使用規則（C01~C04）

| Component | Pages | 規則 |
|---|---|---|
| C01 CaseSummaryHeader | UI-009~014 | 所有 cases 页面均需；FULL + METADATA_ONLY 均支援 |
| C02 StatusOrBlockedBanner | UI-010~014 | BLOCKED tone ≠ EXCEPTION amber tone，must be distinct |
| C03 ModeIndicatorStrip | UI-010~014 | UI-013 的 `CLOSED_REVISION_EXCEPTION` label 固定為特定聲明 |
| C04 ProjectionNoticeBanner | UI-009/010/014 | 僅 SYSADMIN metadata-only projection 時顯示 |

### 3.3 Mode resolution 合約（全叢集）

- Mode 永遠由 container 在 (`role` + `caseState` + `ownership` + `exceptionConditions`) 計算而得。
- Mode 永遠不從 URL 讀取（route param 不是 mode 來源）。
- Mode 永遠不在 presentational component 或 form component 內部決定。

### 3.4 Navigation 合約（跨頁 navigate）

```
cases cluster:
  UI-009 → UI-010 / UI-011 / UI-012 / UI-013 / UI-014  (row navigate)
  UI-010 → UI-011 / UI-012 / UI-013 / UI-014  (control center CTA)
  UI-014 → UI-013  (CLOSED exception entry; navigate-only; NOT submit)
  back: 保留 worklist query context (tab / filter / sort / pagination)

sampling cluster:
  UI-007 → UI-009  (cross-cluster: /cases?batchId={batchId})
  UI-007 → UI-008  (STUB: UI-008 does not exist)
  UI-006 → UI-007  (post-execute success navigate; exact UX TBD)
```

### 3.5 Guard inheritance 架構（Cases 叢集）

```yaml
guard_hierarchy:
  route_level:
    1. role_guard          # outermost: role check
    2. scope_guard         # hospitalId match
    3. projection_guard    # METADATA_ONLY vs FULL
  page_mode_level:
    4. status_guard        # case status allows this page/action
    5. ownership_guard     # commands only: REVIEWER/REREVIEWER ownership
    6. exception_guard     # CLOSED exception path conditions (UI-013 / UI-014)
  action_level:
    7. action_policy_guard # per-action in-flight check
```

Guard 層不可跳躍：exception guard 在 ownership guard 之後；不可先做 exception check。

### 3.6 Projection 策略（O3-C）

- **O3-C 決策**（decision-log §d089/§d095）：SYSTEM_ADMIN 使用 shared base + per-page extension whitelist。
- **shared base**：`stage / revisionNo / submittedAt / submittedBy / currentEffective`（用於 C21 METADATA_HISTORY）。
- **per-page extension 不同**：UI-009 / UI-010 / UI-014 各有自己的 extension；實作時需確認各頁 whitelist。
- Projection 是 backend truth；前端 component visibility 是 defense-in-depth，不是唯一保障。

### 3.7 Sampling 叢集 route 缺口

`ui-route-module-map.md` 明確聲明「範圍：僅 cases 主幹（UI-009~UI-014）」。  
Sampling 叢集目前無正式 route module mapping，以下項目需在實作 UI-006/007 之前補充：

- Sampling cluster route path（建議：`/sampling/...`）
- Sampling cluster lazy-load boundary
- Sampling cluster guard 規格
- SamplingPageHeader shared component 邊界

---

## 4. Must-Not-Invent List

| 項目 | 原因 |
|---|---|
| UI-008 批次詳情頁 route / component | page spec 不存在；[查看詳情] 全程 STUB |
| Sampling cluster route paths | `ui-route-module-map.md` 不涵蓋；未定義 |
| SYSADMIN SamplingBatch 欄位精確白名單（UI-007） | OQ-S04 pending |
| REVIEWER/REREVIEWER UI-007 access policy | OQ-S05 pending |
| UI-006 preview response selectedCount | OQ-S01 pending；4th card 不得顯示推算值 |
| Compare version 選擇 UI 互動細節（UI-014） | OQ §3 pending；interaction spec 層未定 |
| Score sheet item-level form controls（UI-012/013） | 本輪 spec 範圍外；不在 wireframe 定稿 |
| Attachment upload/preview/delete 互動（UI-012/013） | 後續 component planning；未 spec |
| Candidate picker 深層互動（UI-011） | prototype 只確認置放位置，非 picker spec |
| C16 button label for CLOSED_REVISION_EXCEPTION（UI-013） | OQ §1；UX writing 層；不可自行命名 |
| D-013 retry modal 文案以外的任何 retry 語義說明 | D-013 fixed text 之外的 copy 未定 |
| EXECUTING animation spec（UI-006） | 視覺 baseline 層；deferred |
| Sampling cluster SamplingPageHeader component API | sampling component map 尚未建立 |
| C24 最終置放位置（UI-010） | OQ §1；待 prototype 驗證 |

---

## 5. To-Implement-Now vs Placeholder / STUB

### Sampling 叢集

| 功能項目 | 決策 | 條件 / 備注 |
|---|---|---|
| UI-006：SA1 6-mode state machine（R1~R6 region 可視） | ✅ **Implement** | 4 BLOCKED reason 類別須明確 |
| UI-006：SamplingPreviewStatsRegion（3 confirmed cards） | ✅ **Implement** | populationCount / eligibleCount / excludedCount |
| UI-006：4th preview stat card | **PLACEHOLDER** | 條件性：OQ-S01 resolved 後才實作 |
| UI-006：SamplingExecuteConfirmModal structure | ✅ **Implement** | 結構實作；copy = DEFERRED placeholder |
| UI-006：SamplingPostExecuteRegion timing columns | **STUB** | OQ-S02：timing fields 來源未確認 |
| UI-006：READONLY_BLOCKED reason banner | ✅ **Implement** | 4 類 blocked reason；不可省略 |
| UI-006：route path | ⚠️ **需先定義** | sampling route module map 缺口；blocking |
| UI-007：MANAGER role tab preset（4 tabs） | ✅ **Implement** | |
| UI-007：AUDITOR role tab preset（3 tabs） | ✅ **Implement** | |
| UI-007：SYSADMIN metadata projection（V3 with MASKED） | ✅ **Implement w/ MASKED** | 保守做法；OQ-S04 resolved 後補完整欄位 |
| UI-007：[重跑] RetryConfirmModal w/ D-013 text | ✅ **Implement** | D-013 text 固定；不可更改 |
| UI-007：[查看詳情] row action | **STUB** | UI-008 不存在；action present but navigates nowhere |
| UI-007：[查看案件] → /cases?batchId= | ✅ **Implement** | cross-cluster navigate contract |
| UI-007：real-time batch status refresh | **DEFERRED** | no API spec for streaming |

### Cases 叢集

| 功能項目 | 決策 | 條件 / 備注 |
|---|---|---|
| UI-009：A1 work queue + role tab preset | ✅ **Implement** | 詳見 p2-implementation-handoff.md |
| UI-009：O3-C metadata-only same-route projection | ✅ **Implement** | shared base + UI-009 extension |
| UI-010：A2 control center 4-mode | ✅ **Implement** | FULL / ROLE_LIMITED / METADATA / TERMINAL |
| UI-010：C24 RevisionHistoryJumpEntry | **DEFERRED** | OQ §1；位置待 prototype 定案 |
| UI-010：C10/C11 ROLE_LIMITED section visibility | **CONDITIONAL** | OQ §4；需 permission-matrix 確認 |
| UI-011：A3 assignment modes（全部） | ✅ **Implement** | 詳見 p2-implementation-handoff.md |
| UI-011：C23 due-date helper | **STUB slot** | slot-only；no interaction spec |
| UI-012：A4 initial-review modes（全部） | ✅ **Implement** | 詳見 p2-implementation-handoff.md |
| UI-012：score sheet item-level form controls | **STUB region** | 本輪 spec 範圍外 |
| UI-013：A4 rereview modes（含 CLOSED_REVISION_EXCEPTION） | ✅ **Implement** | O1-C 決策已在 decision-log + state-machine |
| UI-013：CLOSED_REVISION_EXCEPTION guard logic | ✅ **Implement** | 3 conditions；guard in route resolver |
| UI-013：revision draft save | **STUB/PENDING** | api-spec 確認後實作 |
| UI-013：C16 label for CLOSED exception | **PLACEHOLDER** | UX writing 決定後替換 |
| UI-013：score sheet item-level form controls | **STUB region** | 本輪 spec 範圍外 |
| UI-014：A5 history + effective version markers | ✅ **Implement** | per-stage current/superseded badge |
| UI-014：CLOSED exception entry（C22 navigate to UI-013） | ✅ **Implement** | navigate-only；NOT submit；warning tone outlined |
| UI-014：METADATA_HISTORY C21 whitelist（O3-C） | ✅ **Implement** | FULL-only fields = `—`；compare notice visible |
| UI-014：compare version selection interaction | **DEFERRED** | OQ §3；interaction spec pending |

---

*文件版本*：v1.0 — 基於 docs/07-design/wireframes/ P1/P2 first-pass + p2-implementation-handoff.md + ui-route-module-map.md + ui-component-map.md + decision-log 當前狀態。
