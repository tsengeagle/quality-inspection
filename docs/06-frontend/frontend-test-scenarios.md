# Frontend Test Scenarios (Foundational)

## 文件狀態
- 類型：新增
- 目的：提供 frontend reviewer / QA 可直接使用的 scenario 基線
- 主範圍：UI-009~UI-014；sampling (UI-006/UI-007) 最小必要

---

## 1) Source Inputs

- `docs/01-requirements/decision-log.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/03-contracts/api-spec.md`
- `docs/05-ui/ui-page-spec-ui006抽選執行頁.md`
- `docs/05-ui/ui-page-spec-ui007抽選批次列表頁.md`
- `docs/05-ui/ui-page-spec-ui009案件列表頁.md`
- `docs/05-ui/ui-page-spec-ui010案件詳情頁.md`
- `docs/05-ui/ui-page-spec-ui011案件分派頁.md`
- `docs/05-ui/ui-page-spec-ui012初審作業頁.md`
- `docs/05-ui/ui-page-spec-ui013複審作業頁.md`
- `docs/05-ui/ui-page-spec-ui014revision歷程頁.md`
- `docs/06-frontend/frontend-design-roadmap.md`

---

## 2) 收斂原則

- 以 Given / When / Then 表示。
- 每個 scenario 標註推薦測試層級：unit / integration / e2e / QA checklist。
- 聚焦 route guard、mode resolution、action visibility、projection、revision、query preset。

---

## A. Route Guard Scenarios

### A-01 非 manager 進 UI-011
- Given actor=`REVIEWER`
- When navigate `/cases/{id}/assignment`
- Then route denied 並導回可用頁（或顯示 forbidden）
- Level: integration, e2e, QA checklist

### A-02 reviewer 進他人 UI-012
- Given actor=`REVIEWER` 且非 assigned reviewer
- When navigate `/cases/{id}/initial-review`
- Then 顯示 blocked/forbidden，不渲染可編輯表單
- Level: integration, e2e

### A-03 rereviewer 進非複審階段 UI-013
- Given actor=`REREVIEWER`、case status=`PENDING_INITIAL_REVIEW`
- When navigate `/cases/{id}/rereview`
- Then 進 `READONLY_BLOCKED` 或拒絕進入
- Level: integration, e2e

### A-04 system admin 進 UI-010
- Given actor=`SYSTEM_ADMIN`
- When navigate `/cases/{id}`
- Then 允許進入但只顯示 metadata-only projection
- Level: integration, e2e, QA checklist

---

## B. Page Mode Resolution Scenarios

### B-01 UI-012 新案模式
- Given actor=assigned reviewer, case=`PENDING_INITIAL_REVIEW`, no submission
- When open UI-012
- Then mode=`NEW_INITIAL_REVIEW`
- Level: unit, integration

### B-02 UI-012 草稿續編
- Given actor=assigned reviewer, has INITIAL draft
- When open UI-012
- Then mode=`EDIT_DRAFT`
- Level: unit, integration

### B-03 UI-012 revision 模式
- Given actor=original reviewer, has INITIAL submitted, initial result not fixed, case not CLOSED/CANCELLED
- When choose revise
- Then mode=`REVISION` 且標示 append-only
- Level: unit, integration, e2e

### B-04 UI-013 CLOSED 只讀

* Authority: D-032／033。
* Given 原複審提交者與 CLOSED；When open UI-013；Then submit／revision／reopen 全 blocked，無例外模式；永久盲審。
* Level: integration, e2e, QA checklist。

### B-05 UI-011 分派模式切換
- Given manager + case status 不同
- When open UI-011
- Then mode 在 ASSIGN/REASSIGN/MIXED/READONLY_BLOCKED 間正確解析
- Level: unit, integration

---

## C. Action Visibility Scenarios

### C-01 UI-009 metadata-only 行為
- Given actor=`SYSTEM_ADMIN`
- When load list
- Then only metadata actions visible，作業 action hidden
- Level: unit, integration, e2e

### C-02 UI-010 control center action
- Given actor=`HOSPITAL_MANAGER`, case=`SELECTED_PENDING_ASSIGNMENT`
- When view detail
- Then show「前往分派」action；不顯示 reviewer submit action
- Level: integration, e2e

### C-03 UI-012 submit vs save
- Given UI-012 in `EDIT_DRAFT`
- When open command bar
- Then save/submit 皆 visible；語意明確區分
- Level: unit, QA checklist

### C-04 UI-014 CLOSED 無修正入口

* Authority: D-032／033。
* Given CLOSED + 原複審提交者；When inspect actions；Then 僅合法唯讀歷程，無 revision 例外導頁入口，無 direct submit。
* Level: integration, e2e, QA checklist。

### C-05 UI-010 role-legal but status-blocked action
- Given actor 屬於該 action family，但案件因 stage / terminal / ownership 條件不可執行
- When view UI-010 action panel
- Then 該 action 維持 visible + disabled + reason；不屬於 actor 角色的 action 仍 hidden
- Level: integration, e2e, QA checklist

---

## D. Projection Scenarios

### D-01 UI-010 FULL projection
- Given actor=`HOSPITAL_MANAGER` in hospital scope
- When open detail
- Then see FULL summaries + legal action entries
- Level: integration, e2e

### D-02 UI-010 metadata-only projection
- Given actor=`SYSTEM_ADMIN`
- When open detail
- Then sensitive fields masked/hidden + metadata projection indicator shown in header；若同時有 terminal/blocked 提示，仍由 C02 作為主 banner
- Level: integration, e2e, QA checklist

### D-03 UI-014 metadata diff restriction
- Given actor=`SYSTEM_ADMIN`
- When open revision compare
- Then 僅顯示 O3-C 白名單欄位；FULL-only 類別保留 label + `—` placeholder，並顯示「詳細差異內容不適用於此視角」
- Level: integration, QA checklist

### D-04 UI-013 CLOSED blocked 與永久盲審

* Authority: D-032／033。
* Given 提交後、revision 或 CLOSED；When inspect response、cache 與畫面；Then 初審內容及可反推的 finalScore 不可取得，CLOSED 為 blocked 而非可操作 exception。
* Level: integration, e2e, QA checklist。

### E-01 initial revision actor mismatch
- Given actor 非 original reviewer
- When attempt INITIAL revise
- Then blocked with `REVISION_ACTOR_MISMATCH`
- Level: unit, integration, e2e

### E-02 CLOSED 複審 revision 拒絕（D-032）

* Authority: D-032／033。
* Given 原複審提交者、CLOSED；When attempt revision；Then 拒絕、無新 submission／成功事件；舊資料不變。
* Level: integration, e2e, QA checklist。

### E-03 revision append-only
- Given existing submitted revision N
- When revise and submit
- Then revision N -> `SUPERSEDED`, new revision N+1 -> `SUBMITTED`
- Level: integration, e2e

### E-04 CANCELLED denies all revisions
- Given case=`CANCELLED`
- When actor tries INITIAL/REREVIEW revise
- Then blocked with `REVISION_NOT_ALLOWED`
- Level: integration, e2e

---

## F. Query Preset (Tab) Scenarios

### F-01 reviewer default tab on UI-009
- Given actor=`REVIEWER`
- When first open UI-009
- Then default preset = 待初審/初審中（依 page spec）
- Level: integration, e2e, QA checklist

### F-02 rereviewer default tab on UI-009
- Given actor=`REREVIEWER`
- When first open UI-009
- Then default preset = 待複審/複審中
- Level: integration, e2e

### F-03 tab switch keeps page identity
- Given actor 在 UI-009
- When switch tab
- Then route/pageId 不變，只改 query preset/filter
- Level: unit, integration, e2e

### F-04 UI-014 stage grouping tab
- Given actor on UI-014
- When switch `ALL` / `INITIAL_ONLY` / `REREVIEW_ONLY`
- Then 同頁切換資料分組，不產生新頁行為
- Level: integration, e2e

---

## 3) 與 Roadmap 的關係

- 這些 scenarios 可直接映到下一輪 `ui-route-module-map` 的 guard tests。
- 可映到 `ui-view-model-spec` 的 mode resolver unit tests。
- 可映到 `ui-api-binding-matrix` 的 command enable/disable integration tests。

---

## 4) Pending Confirmation

1. sampling retry 的完整禁止原因集合（目前僅最小必要）。


## G. Command Path Truth Scenarios

### G-01 case-rooted command path only (O2-A)
- Given command dispatch for submit/revise
- When frontend builds endpoint
- Then must use `/review-cases/{caseId}/submissions/{submissionId}/...`
- Level: unit, integration

### G-02 reject legacy submission-rooted path
- Given legacy config with `/review-submissions/{submissionId}/...`
- When executing command
- Then client validation/test should fail fast
- Level: unit

## D-031～D-033 同步契約

以下映射以 Decision Log D-031～D-033、state-machine 與 permission-matrix 為 authority。

以 EX-041～052／TS-DA-001～012 補足 frontend 驗證：初審未固定不能進複審／manager 結案，固定後禁修；複審 submit 不 CLOSED、未固定可合法修訂、原提交者具資格才可正式結案並固定；CLOSED 全 blocked。所有階段測 server response、UI、cache、history／compare 與跨入口的永久盲審，含 finalScore 反推防護。其他角色正向存取亦須保留。

驗證追溯 EX-041～052／TS-DA-001～012；狀態、儲存與新操作表示未知處不由 frontend 自行定案。
