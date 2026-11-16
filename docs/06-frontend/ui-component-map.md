# UI Component Map v1 (Cases Cluster)

## 1. 文件目的

本文件將 `docs/05-ui/ui-layout-archetypes.md` 定義的 layout archetypes（A1~A5）與 cases 主幹頁面結構（UI-009~UI-014）收斂為 **Structural Component Mapping baseline**。

本文件用途：
- 作為下一輪 `ui-wireframe-brief.md` 的直接輸入。
- 作為後續 implementation planning 的穩定結構基線（僅結構分層，不含實作細節）。

本文件不定義：
- 精確 component API
- Angular input/output / DI / module 宣告
- 視覺 token、樣式細節、high-fidelity mockup

---

## 2. Source Inputs

### Foundational truth
- `docs/06-frontend/frontend-design-method.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/06-frontend/ui-state-actions-matrix.md`
- `docs/06-frontend/frontend-test-scenarios.md`

### Bridge files
- `docs/06-frontend/ui-route-module-map.md`
- `docs/06-frontend/ui-view-model-spec.md`
- `docs/06-frontend/ui-api-binding-matrix.md`

### Review / backwrite
- `docs/06-frontend/frontend-review-gate-pending-items.md`

### UI authority
- `docs/05-ui/ui-information-architecture.md`
- `docs/05-ui/ui-page-inventory.md`
- `docs/05-ui/ui-page-spec-ui009案件列表頁.md`
- `docs/05-ui/ui-page-spec-ui010案件詳情頁.md`
- `docs/05-ui/ui-page-spec-ui011案件分派頁.md`
- `docs/05-ui/ui-page-spec-ui012初審作業頁.md`
- `docs/05-ui/ui-page-spec-ui013複審作業頁.md`
- `docs/05-ui/ui-page-spec-ui014revision歷程頁.md`
- `docs/05-ui/ui-layout-archetypes.md`

### Roadmap
- `docs/06-frontend/frontend-design-roadmap.md`

---

## 3. 適用範圍與非範圍

### 適用範圍
- 僅涵蓋 cases 主幹：UI-009 ~ UI-014。
- 僅做 component mapping 收斂：shared / feature-scoped / page-owned / deferred。

### 非範圍
- 不做 `ui-wireframe-brief.md`。
- 不做 visual baseline。
- 不做 implementation plan。
- 不定義精確 component API。
- 不定義 Angular folder / selector / code。

---

## 4. Component Mapping 原則

1. **先 authority 對齊，再抽象**：
   component 候選必須可回溯到 archetype、page responsibility、state/action policy、projection policy。

2. **shared / feature-scoped / page-owned 判準**：
   - `shared`：跨 >=3 頁且語意穩定、狀態差異可由 mode/projection 表達。
   - `feature-scoped`：主要在 cases cluster 重用，但語意仍綁定 cases domain。
   - `page-owned`：僅單頁使用，或語意與其他頁差異過大。
   - `deferred`：重用意圖存在，但受 pending confirmation 或資料粒度不穩影響。

3. **projection / blocked / exception 是一級分類條件**：
   - metadata-only 是同頁 projection，不是新頁。
   - blocked/forbidden/exception 必須在 component 邊界可表達，而非「之後補 if-else」。

4. **action 結構不得混同**：
   - row-level action（A1）
   - navigate-only action zone（A2/A5）
   - command action bar（A3/A4）
   三者即使視覺接近，也先分開定義。

5. **先穩定結構，再談實作 API**：
   本文件只定義 component mapping 與 ownership boundary，不定義技術層 props/contracts。

6. **portal51-lib baseline 關係**：
   可引用「存在 shared UI baseline」作為後續落地參考；本文件不虛構 portal51-lib 的 wrapper 或元件細節。

---

## 5. Component 分類模型

```yaml
classification_layers:
  - Shared Structure Candidates
  - Feature-Scoped Candidates (cases cluster)
  - Page-Owned Composition Regions
  - Deferred / Not Yet Abstracted
```

---

## 6. Component 候選清單

> 說明：以下為結構候選，不代表已落地實作元件。

### C01. CaseSummaryHeader
- componentName: `CaseSummaryHeader`
- classification: `shared`
- primary responsibility: 統一承載 case 識別、stage/status、角色視角摘要。
- usedByPages: UI-009/UI-010/UI-011/UI-012/UI-013/UI-014
- supportedArchetypes: A1/A2/A3/A4/A5
- structural origin: `page_summary_header`
- visibility / projection constraints: FULL + METADATA_ONLY 皆支援；metadata-only 套白名單欄位。
- mode / state / exception constraints: 在 blocked/readonly 仍需顯示，避免使用者失去上下文。
- ownership boundary: `shared`
- notes for future view-model binding: 對應 shared sub-model + page extension。
- notes for future wireframe: 需明確呈現「同一 header 在 5 archetypes 的資訊密度差」。
- notes for future implementation: 避免耦合 command 按鈕；按鈕獨立在 action 結構。

### C02. StatusOrBlockedBanner
- componentName: `StatusOrBlockedBanner`
- classification: `shared`
- primary responsibility: 呈現 terminal/forbidden/blocked/reason 訊息。
- usedByPages: UI-010/UI-011/UI-012/UI-013/UI-014
- supportedArchetypes: A2/A3/A4/A5
- structural origin: `status_or_blocked_banner`
- visibility / projection constraints: FULL/METADATA_ONLY 都可見（內容受遮罩與權限影響）。
- mode / state / exception constraints: D-032：CLOSED 所有 submit／revision blocked，不再區分例外修正。
- ownership boundary: `shared`
- notes for future view-model binding: 依 action policy 產生 machine-readable reason code。
- notes for future wireframe: 必畫 blocked、forbidden、exception 三類訊息差異；exception 使用 amber / warning 語意，blocked 則需維持獨立 blocked tone。
- notes for future implementation: 保留 reason-code 映射，不寫死單一句子；exception tone 需與 C22 的 warning-toned exception entry 同一語意家族。

### C03. ModeIndicatorStrip
- componentName: `ModeIndicatorStrip`
- classification: `shared`
- primary responsibility: 顯示 page mode（ASSIGN/REVISION/READONLY...）與行為語意。
- usedByPages: UI-010/UI-011/UI-012/UI-013/UI-014
- supportedArchetypes: A2/A3/A4/A5
- structural origin: `mode_indicator_strip`
- visibility / projection constraints: metadata-only 可見但 mode 簡化為 read/navigate 語意。
- mode / state / exception constraints: UI-013 必含 CLOSED revision-exception 模式標示。
- ownership boundary: `shared`
- notes for future view-model binding: 綁定 mode resolver 輸出與 display label mapping。
- notes for future wireframe: 需在 command page 顯示高可見優先序；D-032 撤銷 UI-013 CLOSED 例外修正模式；CLOSED 只讀／blocked。
- notes for future implementation: 不內含表單或 command 觸發。

### C04. ProjectionNoticeBanner
- componentName: `ProjectionNoticeBanner`
- classification: `shared`
- primary responsibility: 宣告 metadata-only projection 限制。
- usedByPages: UI-009/UI-010/UI-014
- supportedArchetypes: A1/A2/A5
- structural origin: `projection_notice_banner`
- visibility / projection constraints: 僅 METADATA_ONLY 顯示。
- mode / state / exception constraints: 非 blocked 主體，僅 projection 說明。
- ownership boundary: `shared`
- notes for future view-model binding: 綁定 projectionMode + maskingLevel。
- notes for future wireframe: 在 UI-010/UI-014 的複合狀態下降格為 header subtitle / badge / inline projection indicator；與 C02 分工清楚，不混為同一 alert。
- notes for future implementation: 不承載 domain action；不得升格回 primary status banner。

### C05. CaseWorkQueueFilterBar
- componentName: `CaseWorkQueueFilterBar`
- classification: `feature-scoped`
- primary responsibility: UI-009 tab preset + filter 條件輸入。
- usedByPages: UI-009
- supportedArchetypes: A1
- structural origin: A1 `tab query preset zone` + `filter zone`
- visibility / projection constraints: metadata-only 僅保留允許篩選欄位。
- mode / state / exception constraints: role default tab 改變預設條件集。
- ownership boundary: `features/cases`
- notes for future view-model binding: 對應 `CaseListFilterViewModel`。
- notes for future wireframe: 必畫 role-default tab + metadata 欄位縮減。
- notes for future implementation: 與通用搜尋列區分；保留 tab=query preset 語意。

### C06. CaseWorkQueueTable
- componentName: `CaseWorkQueueTable`
- classification: `feature-scoped`
- primary responsibility: UI-009 結果列與欄位密度承載。
- usedByPages: UI-009
- supportedArchetypes: A1
- structural origin: A1 `result table zone`
- visibility / projection constraints: FULL/METADATA_ONLY 欄位集合不同（shared base + page extension）。
- mode / state / exception constraints: forbidden/empty/error 以 table 外圍狀態呈現。
- ownership boundary: `features/cases`
- notes for future view-model binding: 對應 `CaseListRowViewModel[]`。
- notes for future wireframe: 必畫 metadata row 差異與 overflow 行為。
- notes for future implementation: 不與 dashboard list 共用。

### C07. CaseRowActionSet
- componentName: `CaseRowActionSet`
- classification: `feature-scoped`
- primary responsibility: UI-009 row-level 導頁操作（detail/assign/review/history）。
- usedByPages: UI-009
- supportedArchetypes: A1
- structural origin: A1 `row action zone`
- visibility / projection constraints: metadata-only 僅 read/navigate 類 actions。
- mode / state / exception constraints: 受 role+scope+status 同步限制。
- ownership boundary: `features/cases`
- notes for future view-model binding: 可引用 `CaseActionPanelViewModel` 子集。
- notes for future wireframe: 需畫 row action 在窄屏的 overflow。
- notes for future implementation: 禁止退化成 generic table action without policy.

### C08. CaseStatusActionPanel
- componentName: `CaseStatusActionPanel`
- classification: `feature-scoped`
- primary responsibility: UI-010 control center action cluster（navigate-only）。
- usedByPages: UI-010
- supportedArchetypes: A2
- structural origin: A2 `state/action zone`
- visibility / projection constraints: metadata-only 僅保留 read/navigate。
- mode / state / exception constraints: CLOSED exception 僅顯示導向，不在此提交。
- ownership boundary: `features/cases`
- notes for future view-model binding: 對應 `CaseActionPanelViewModel`。
- notes for future wireframe: 要與 command bar 版位明確區隔；`ROLE_DENIED` action hidden，status/stage/ownership blocked action visible+disabled+reason。
- notes for future implementation: 不混入 UI-011/UI-012/UI-013 提交按鈕；CLOSED rereviewer 場景保留 disabled 原 action + UI-014 替代入口提示。

### C09. TraceabilitySummaryRegion
- componentName: `TraceabilitySummaryRegion`
- classification: `feature-scoped`
- primary responsibility: 顯示來源與 lifecycle / revision 摘要。
- usedByPages: UI-010/UI-014（A4 僅版本摘要關聯）
- supportedArchetypes: A2/A5
- structural origin: `traceability_region`
- visibility / projection constraints: metadata-only 保留摘要但遮罩敏感 diff。
- mode / state / exception constraints: stage 分組切換需穩定保留上下文。
- ownership boundary: `features/cases`
- notes for future view-model binding: 連結 lifecycle + revision summary submodels。
- notes for future wireframe: 需畫 detail center 與 history 頁的共同骨架。
- notes for future implementation: 暫不抽成全域 shared，先留在 cases。

### C10. InitialReviewSummaryRegion
- componentName: `InitialReviewSummaryRegion`
- classification: `feature-scoped`
- primary responsibility: 初審結果摘要閱讀區。
- usedByPages: UI-010/UI-013
- supportedArchetypes: A2/A4
- structural origin: A2/A4 對應 summary regions
- visibility / projection constraints: metadata-only 僅顯示允許摘要欄位（UI-010）。
- mode / state / exception constraints: UI-013 作為 rereview 參考，不可誤變可編輯。
- ownership boundary: `features/cases`
- notes for future view-model binding: 對應 initial summary read model。
- notes for future wireframe: 在 UI-013 應標示「reference-only」。
- notes for future implementation: 與 form component 解耦。

### C11. RereviewSummaryRegion
- componentName: `RereviewSummaryRegion`
- classification: `feature-scoped`
- primary responsibility: 複審摘要閱讀區。
- usedByPages: UI-010
- supportedArchetypes: A2
- structural origin: A2 `rereview summary zone`
- visibility / projection constraints: metadata-only 可降級為摘要欄位。
- mode / state / exception constraints: CLOSED 不影響閱讀但影響 action 引導。
- ownership boundary: `features/cases`
- notes for future view-model binding: 綁定 latest rereview summary fields。
- notes for future wireframe: 與 initial summary 並置關係要穩定。
- notes for future implementation: 可與 InitialReviewSummaryRegion 共用框架但不同資料語意。

### C12. AssignmentModeBanner
- componentName: `AssignmentModeBanner`
- classification: `feature-scoped`
- primary responsibility: UI-011 顯示 ASSIGN/REASSIGN/MIXED/READONLY_BLOCKED。
- usedByPages: UI-011
- supportedArchetypes: A3
- structural origin: A3 `assignment mode banner`
- visibility / projection constraints: 無 metadata-only 主情境。
- mode / state / exception constraints: CLOSED/CANCELLED 必示 READONLY_BLOCKED。
- ownership boundary: `features/cases`
- notes for future view-model binding: 對應 AssignmentPageViewModel.mode。
- notes for future wireframe: ASSIGN vs READONLY_BLOCKED 必畫對照。
- notes for future implementation: 與 generic alert 區分。

### C13. ReviewerAssignmentSection
- componentName: `ReviewerAssignmentSection`
- classification: `feature-scoped`
- primary responsibility: UI-011 初審 reviewer 指派/改派輸入區。
- usedByPages: UI-011
- supportedArchetypes: A3
- structural origin: A3 `reviewer assignment section`
- visibility / projection constraints: 無 metadata-only。
- mode / state / exception constraints: 非 manager 或 illegal state 時 readonly/forbidden。
- ownership boundary: `features/cases`
- notes for future view-model binding: 命令 payload 與 picker 狀態同步。
- notes for future wireframe: 需畫 assign / reassign 欄位差。
- notes for future implementation: 審慎保留 dueAt/reason 可選欄位邊界。

### C14. RereviewerAssignmentSection
- componentName: `RereviewerAssignmentSection`
- classification: `feature-scoped`
- primary responsibility: UI-011 複審者指派/改派輸入區。
- usedByPages: UI-011
- supportedArchetypes: A3
- structural origin: A3 `rereviewer assignment section`
- visibility / projection constraints: 無 metadata-only。
- mode / state / exception constraints: 受 rereview stage assignability 限制。
- ownership boundary: `features/cases`
- notes for future view-model binding: 對應 assign/reassign rereviewer commands。
- notes for future wireframe: 與 reviewer section 視覺相似但語意分離。
- notes for future implementation: 不與 ReviewerAssignmentSection 強制同 component。

### C15. ReviewFormSummaryStrip
- componentName: `ReviewFormSummaryStrip`
- classification: `feature-scoped`
- primary responsibility: UI-012/UI-013 作業頁的 case+stage+version 上下文摘要。
- usedByPages: UI-012/UI-013
- supportedArchetypes: A4
- structural origin: A4 `summary strip`
- visibility / projection constraints: 本頁主情境 FULL。
- mode / state / exception constraints: REVISION / READONLY / CLOSED-exception 皆需穩定顯示。
- ownership boundary: `features/cases`
- notes for future view-model binding: 對應 Initial/RereviewPageViewModel shared context。
- notes for future wireframe: 與 command bar 組合為高優先可見區。
- notes for future implementation: 不承載表單答案欄位。

### C16. ReviewCommandBar
- componentName: `ReviewCommandBar`
- classification: `feature-scoped`
- primary responsibility: UI-012/UI-013 save draft / submit / revise command 群組。
- usedByPages: UI-012/UI-013
- supportedArchetypes: A4
- structural origin: A4 `command action bar`
- visibility / projection constraints: 無 metadata-only。
- mode / state / exception constraints: UI-013 CLOSED 時 submit blocked、revise exception 受限開放。
- ownership boundary: `features/cases`
- notes for future view-model binding: 綁定 mode + guard reason + loading state。
- notes for future wireframe: 必畫 NEW/EDIT_DRAFT/REVISION/CLOSED-exception 差異。
- notes for future implementation: 保留 command 語意分離，不做單一 generic submit bar；UI-013 P1 採 viewport-fixed bottom bar，主作業區需預留底部補白。

### C17. ReviewVersionSummaryRegion
- componentName: `ReviewVersionSummaryRegion`
- classification: `feature-scoped`
- primary responsibility: UI-012/UI-013 顯示版本脈絡與基底 submission 關係。
- usedByPages: UI-012/UI-013
- supportedArchetypes: A4
- structural origin: A4 `version summary region`
- visibility / projection constraints: FULL 主情境。
- mode / state / exception constraints: revision 與 exception 模式需顯示 base revision 關聯。
- ownership boundary: `features/cases`
- notes for future view-model binding: 對應 revisionContext。
- notes for future wireframe: 強調 append-only 與 effective version 對應。
- notes for future implementation: 不承擔 timeline 全量瀏覽（那是 A5）。

### C18. InitialSummaryReferenceRegion (UI-013)
- componentName: `InitialSummaryReferenceRegion`
- classification: `page-owned`
- primary responsibility: UI-013 顯示初審摘要供複審參考。
- usedByPages: UI-013
- supportedArchetypes: A4
- structural origin: A4 optional `UI-013 initial summary reference region`
- visibility / projection constraints: FULL 主情境。
- mode / state / exception constraints: reference-only，不可編輯。
- ownership boundary: `page-local`
- notes for future view-model binding: 讀取初審摘要只讀模型。
- notes for future wireframe: 與複審作答區需明顯分區。
- notes for future implementation: 暫不與 UI-010 摘要區共用，避免語意混淆。

### C19. FinalDecisionSummaryRegion (UI-013)

> 已移除（D-033，PR #45）：複審頁只給複審醫師使用，最終分數與最終結論可反推初審分數，不得顯示。本節保留作為歷史紀錄；C18 也不顯示初審分數。

- componentName: `FinalDecisionSummaryRegion`
- classification: `page-owned`
- primary responsibility: UI-013 呈現複審最終決策摘要。
- usedByPages: UI-013
- supportedArchetypes: A4
- structural origin: A4 optional `final decision summary region`
- visibility / projection constraints: FULL 主情境。
- mode / state / exception constraints: 僅 rereview flow 有意義。
- ownership boundary: `page-local`
- notes for future view-model binding: 對應 rereview final decision fields。
- notes for future wireframe: 提示「final decision 與 revise entry」關聯。
- notes for future implementation: 暫不抽 shared，先保留頁面責任。

### C20. RevisionTimelineRegion
- componentName: `RevisionTimelineRegion`
- classification: `feature-scoped`
- primary responsibility: UI-014 版本時間軸與 effective version 指示。
- usedByPages: UI-014
- supportedArchetypes: A5
- structural origin: A5 `timeline region`
- visibility / projection constraints: metadata-only 保留時間軸骨架但限制 detail。
- mode / state / exception constraints: stage tabs（ALL/INITIAL_ONLY/REREVIEW_ONLY）驅動內容。
- ownership boundary: `features/cases`
- notes for future view-model binding: 對應 `RevisionTimelineItemViewModel[]`。
- notes for future wireframe: 必畫 effective version 強調。
- notes for future implementation: 與 audit log timeline 不直接共用。

### C21. RevisionCompareSummaryRegion
- componentName: `RevisionCompareSummaryRegion`
- classification: `feature-scoped`
- primary responsibility: UI-014 顯示 revision detail/compare 摘要。
- usedByPages: UI-014
- supportedArchetypes: A5
- structural origin: A5 `detail/compare summary region`
- visibility / projection constraints: metadata-only 僅摘要與遮罩訊息。
- mode / state / exception constraints: compare selection 與 stage filter 同步。
- ownership boundary: `features/cases`
- notes for future view-model binding: 對應 `RevisionDetailSummaryViewModel`。
- notes for future wireframe: 必畫 FULL vs METADATA_ONLY 的 compare 差異；metadata-only 僅保留 O3-C 白名單欄位，FULL-only 類別以 `—` placeholder + compare notice 表達。
- notes for future implementation: 先維持 cases 專屬，避免過早 generic diff 抽象；不得在 metadata-only 顯示 score/diff 題數摘要。

### C22. RevisionNavigationActionZone
- componentName: `RevisionNavigationActionZone`
- classification: `feature-scoped`
- primary responsibility: UI-014 提供 navigate-only actions（含條件式導向 UI-013 exception 入口）。
- usedByPages: UI-014
- supportedArchetypes: A5
- structural origin: A5 `navigation action region`
- visibility / projection constraints: metadata-only 僅允許 read/navigation actions。
- mode / state / exception constraints: O1-C：CLOSED + 合法 rereviewer 才顯示 exception entry。
- ownership boundary: `features/cases`
- notes for future view-model binding: 依 action eligibility matrix 輸出。
- notes for future wireframe: 需與 command bar 清楚區隔（不可出現 submit）；條件式 exception entry 可見時採 warning tone outlined navigate action，並與 C02 exception banner 保持同一 amber / warning 語意。
- notes for future implementation: 嚴禁在本區直接提交 revision；若 UI library 無對應元件，也須維持次要導頁層級，不升級為 filled primary CTA。

### C23. AssignmentDueDateHelperRegion
- componentName: `AssignmentDueDateHelperRegion`
- classification: `deferred`
- primary responsibility: UI-011 due date 提示/輸入輔助區。
- usedByPages: UI-011
- supportedArchetypes: A3
- structural origin: A3 optional `due date helper`
- visibility / projection constraints: 無 metadata-only。
- mode / state / exception constraints: 受 assign/reassign policy 與 API 欄位可選性影響。
- ownership boundary: `features/cases` (tentative)
- notes for future view-model binding: 欄位必要性與驗證規則仍待確認。
- notes for future wireframe: 先畫成可插拔區，不強制固定樣式。
- notes for future implementation: 待 API/policy 穩定再決定是否抽象。

### C24. RevisionLifecycleEntryArea
- componentName: `RevisionLifecycleEntryArea`
- classification: `deferred`
- primary responsibility: UI-010 導向 lifecycle/revision 的入口組合。
- usedByPages: UI-010
- supportedArchetypes: A2
- structural origin: A2 `revision/lifecycle entry zone`
- visibility / projection constraints: metadata-only 可見但內容粒度受限。
- mode / state / exception constraints: 受 lifecycle API 欄位粒度與排序策略影響。
- ownership boundary: `features/cases` (tentative)
- notes for future view-model binding: 依 lifecycle API 穩定度決定細分程度。
- notes for future wireframe: 先保留入口存在，不固定細節呈現。
- notes for future implementation: 暫不抽 shared，待 pending 清除。

---

## 7. 第一輪指定候選檢查與分類結論

| Candidate | Classification | 結論 |
|---|---|---|
| CaseSummaryHeader | shared | 穩定，跨 A1~A5 重用 |
| StatusOrBlockedBanner | shared | 穩定，blocked/forbidden/exception 共通載體 |
| ModeIndicatorStrip | shared | 穩定，mode-driven pages 共通 |
| ProjectionNoticeBanner | shared | 穩定，metadata-only 共通提示 |
| CaseWorkQueueFilterBar | feature-scoped | A1 專屬語意，與 tab preset 綁定 |
| CaseWorkQueueTable | feature-scoped | A1 專屬，欄位白名單差異明確 |
| CaseRowActionSet | feature-scoped | row-level action 與 page-level action 不可混 |
| CaseStatusActionPanel | feature-scoped | A2 控制中心 navigate-only action |
| TraceabilitySummaryRegion | feature-scoped | A2/A5 可重用但仍屬 cases 語意 |
| InitialReviewSummaryRegion | feature-scoped | UI-010/UI-013 共用語意但仍 cases 專屬 |
| RereviewSummaryRegion | feature-scoped | 目前主要 UI-010 使用，與初審摘要可同框架 |
| AssignmentModeBanner | feature-scoped | A3 強語意，不宜全域化 |
| ReviewerAssignmentSection | feature-scoped | cases 指派命令語意 |
| RereviewerAssignmentSection | feature-scoped | cases 指派命令語意 |
| ReviewFormSummaryStrip | feature-scoped | A4 command context 共通 |
| ReviewCommandBar | feature-scoped | A4 command 行為核心 |
| ReviewVersionSummaryRegion | feature-scoped | A4 revision context 共通 |
| InitialSummaryReferenceRegion [UI-013] | page-owned | 單頁 reference-only |
| FinalDecisionSummaryRegion [UI-013] | page-owned | 單頁決策摘要語意 |
| RevisionTimelineRegion | feature-scoped | A5 核心區 |
| RevisionCompareSummaryRegion | feature-scoped | A5 核心區，projection 差異大 |
| RevisionNavigationActionZone | feature-scoped | A5 navigate-only + exception entry |

補充：
- `AssignmentDueDateHelperRegion`、`RevisionLifecycleEntryArea` 目前列為 deferred。

---

## 8. Page-to-Component Mapping（UI-009 ~ UI-014）

| Page | Primary Archetype | 主要 Components | Shared / Feature / Page-owned / Deferred | Projection / Exception / Blocked Notes |
|---|---|---|---|---|
| UI-009 案件列表頁 | A1 | C01, C04, C05, C06, C07 | shared: C01/C04; feature: C05/C06/C07 | metadata-only 欄位白名單 + row action 降級；forbidden/empty/error 需可視 |
| UI-010 案件詳情頁 | A2 | C01, C02, C03, C08, C09, C10, C11, C24 | shared: C01/C02/C03; feature: C08/C09/C10/C11; deferred: C24 | metadata-only control center；CLOSED exception 僅導頁不提交 |
| UI-011 案件分派頁 | A3 | C01, C02, C03, C12, C13, C14, C23 | shared: C01/C02/C03; feature: C12/C13/C14; deferred: C23 | READONLY_BLOCKED（CLOSED/CANCELLED）；無 metadata-only 主情境 |
| UI-012 初審作業頁 | A4 | C01, C02, C03, C15, C16, C17 | shared: C01/C02/C03; feature: C15/C16/C17 | command page；blocked/ownership guard；無 metadata-only 主情境 |
| UI-013 複審作業頁 | A4 | C01, C02, C03, C15, C16, C17, C18 | shared: C01/C02/C03; feature: C15/C16/C17; page-owned: C18 | D-032：CLOSED 所有 submit／revision blocked；D-033：C19 已移除，C18 不顯示初審分數 |
| UI-014 Revision 歷程頁 | A5 | C01, C02, C03, C04, C09, C20, C21, C22 | shared: C01/C02/C03/C04; feature: C09/C20/C21/C22 | metadata-only compare 降級；navigate-only action；條件式 exception 導頁 |

---

## 9. Cross-Cutting Constraints

1. **metadata-only whitelist 約束**
   - 影響 C01/C04/C06/C09/C21。
   - UI-009/UI-010/UI-014 需以 shared base + page extension 限制欄位。

2. **CLOSED rereview revision exception（O1-C）**
   - 影響 C02/C03/C16/C22。
   - UI-013：submit blocked，僅 revise-exception。
   - UI-014：僅條件式導向 UI-013，不直接提交。

3. **row-level vs page-level actions**
   - C07（row-level）不得混入 C16（command bar）。
   - C08/C22 為 navigate-only，不可退化成 command bar。

4. **navigate-only vs command action 邊界**
   - A2/A5 的 action 結構與 A3/A4 command 結構保持語意分離。

5. **FULL vs METADATA_ONLY variants**
   - C01/C04/C06/C09/C21 必須明確提供 projection variant。
   - UI-011/UI-012/UI-013 不建立 metadata-only command variants。

---

## 10. Deferred / Not Yet Abstracted

1. `AssignmentDueDateHelperRegion`（C23）
   - 原因：assign/reassign 的 due date 欄位必要性與 API 粒度仍不穩。

2. `RevisionLifecycleEntryArea`（C24）
   - 原因：lifecycle API 欄位粒度/排序尚待最終確認。

3. `InitialSummaryReferenceRegion` 與 `FinalDecisionSummaryRegion` 目前不升級 shared
   - 原因：僅 UI-013 使用且語意高度頁面專屬。

4. `RevisionCompareSummaryRegion` 不升級為全域 shared
   - 原因：projection 差異大、cases 語意強、跨域重用證據不足。

---

## 11. Pending Confirmation

僅保留仍未由既有 authority 明確定稿者：

1. `INITIAL_REVIEW_COMPLETED` manager handoff commands 的 API 顯式化程度（影響 C08/C12 邊界）。
2. lifecycle API 欄位粒度與排序（影響 C24 是否可由 deferred 轉 feature-scoped）。

> 已批准回寫的 O1-C / O2-A / O3-C 不列入 pending。

---

## 12. Next-Step Handoff

### 給 `ui-wireframe-brief.md`
- 以本文件的 component classification 作為 wireframe 版面拆分邊界。
- 優先畫出以下結構變體：
  - C01/C02/C03/C04（shared 結構在不同 archetype 的變體）
  - C16（A4 command 模式：NEW/EDIT_DRAFT/REVISION/CLOSED-exception）
  - C21（FULL vs METADATA_ONLY compare 差異）
  - C22（history navigate-only + exception entry）

### 給 future implementation planning
- shared 與 feature-scoped 的初步 ownership boundary 已可用於估算拆分。
- deferred 區塊在 pending 清除前不得強行共用化。

## D-031～D-033 同步契約

以下映射以 Decision Log D-031～D-033、state-machine 與 permission-matrix 為 authority。

C02／C03／C16／C22 不再呈現 CLOSED 例外修正；歷程區只導向仍合法的未固定 revision（D-031／032）。初審固定 binding unknown，不新增按鈕權限或 capability 欄位；複審正式結案的按鈕依 D-053 第 8 點（最新複審提交為 `SUBMITTED` 且登入者是提交者時顯示）。所有 summary／score／compare 元件套用 D-033：同案複審醫師不得從 response／cache 取得初審內容或可反推分數，其他角色政策維持。

驗證追溯 EX-041～052／TS-DA-001～012；狀態、儲存與新操作表示未知處不由 frontend 自行定案。
