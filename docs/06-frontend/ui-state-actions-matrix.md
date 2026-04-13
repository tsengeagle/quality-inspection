# UI State Actions Matrix (Foundational Design Truth)

## 文件狀態
- 類型：新增
- 目的：建立 page-level UI business modes 與 action policy 的單一真相
- 覆蓋：UI-009~UI-014（主幹） + UI-006/UI-007（最小必要）

---

## 1) Source Inputs

- `docs/01-requirements/decision-log.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/03-contracts/api-spec.md`
- `docs/05-ui/ui-page-inventory.md`
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

1. page mode 必須是業務語意（不是 loading/success/error）。
2. action 分為 `visible + executable`、`visible + blocked`、`hidden` 三類。
3. blocked action 必須帶 reason（domain status / role / scope / terminal）。
4. metadata-only 與 FULL 為同頁 projection 差異，不複製頁面。
5. tab 一律視為 query preset。

---

## 3) Global Mode Resolution Rule

```yaml
resolve_mode_order:
  - route_guard_and_scope
  - projection_mode(metadata-only|FULL)
  - domain_business_mode
  - technical_state(loading|empty|error)

action_policy_inputs:
  - role
  - case_or_batch_status
  - assignment_ownership
  - projection_mode
  - terminal_state
  - API_guard_feedback
```

---

## 4) Page Matrix

## 4.1 UI-009 案件列表頁

- page purpose：跨角色案件工作入口（list + next action navigation）
- inputs affecting UI：role、tab preset、case status、projection mode
- page modes：`WORKLIST_MANAGER` / `WORKLIST_REVIEWER` / `WORKLIST_REREVIEWER` / `WORKLIST_AUDITOR` / `WORKLIST_METADATA_ONLY`
- mode resolution：先角色，再 projection，再 tab preset（tab 不改 page identity）
- role-sensitive behavior：sysadmin 只 metadata actions；reviewer/rereviewer 只顯示合法作業入口
- visible sections：summary + tab + filters + table + paging
- available actions：view detail、go assign、go initial、go rereview、go revisions（依 role/status）
- hidden actions：metadata-only 隱藏作業 action
- disabled/blocked actions：terminal 或 stage mismatch 顯示 blocked label
- blocked reasons：`ROLE_DENIED` / `HOSPITAL_SCOPE_MISMATCH` / `INVALID_STATE_TRANSITION`
- next-step hints：依 tab 顯示「下一步建議操作」
- data projection differences：FULL 顯示業務欄位；metadata-only 採 shared base + page extension 白名單
- route guard notes：頁面可進入但 action-level guard 嚴格
- pending confirmation：sysadmin 篩選欄位最小集

## 4.2 UI-010 案件詳情頁

- page purpose：control center（狀態判讀 + 正確導頁）
- inputs affecting UI：role、projection、case status、assignment、latest submission stage
- page modes：`FULL_CONTROL_CENTER` / `ROLE_LIMITED_CONTROL_CENTER` / `METADATA_CONTROL_CENTER`
- mode resolution：先 projection，再依 role/status 組合顯示 action cluster
- role-sensitive behavior：manager 可分派；reviewer/rereviewer 僅自身合法作業；auditor read-only
- visible sections：summary、status/action、traceability、initial/rereview summary、revision entry
- available actions：navigate only（assign/review/rereview/revisions）
- hidden actions：不在本頁直接 submit/revise
- disabled/blocked actions：status 不符或 terminal 顯示 blocked reason
- blocked reasons：`CASE_ALREADY_CLOSED` / `CASE_ALREADY_CANCELLED` / actor mismatch
- next-step hints：明確提示下一頁 command page
- data projection differences：sysadmin metadata-only banner + shared base/page extension 白名單遮罩
- route guard notes：scope guard 先於 detail content render
- pending confirmation：複審中是否可看初審完整內容（D-018 相關）

## 4.3 UI-011 案件分派頁

- page purpose：manager command page（assignment orchestration）
- inputs affecting UI：case status、current reviewer/rereviewer、role(scope)
- page modes：`ASSIGN_INITIAL` / `REASSIGN_INITIAL` / `ASSIGN_REREVIEW` / `REASSIGN_REREVIEW` / `MIXED_ASSIGNMENT` / `READONLY_BLOCKED`
- mode resolution：由 case status + existing assignments 決定
- role-sensitive behavior：僅 HOSPITAL_MANAGER 可執行
- visible sections：case summary、mode hint、initial assignment、rereview assignment、submit controls
- available actions：assign/reassign reviewer、assign/reassign rereviewer、update due date
- hidden actions：非 manager 全隱藏
- disabled/blocked actions：CLOSED/CANCELLED 或 stage 不合法
- blocked reasons：terminal / scope mismatch / assignment rule
- next-step hints：完成後導回 UI-010 或 UI-009
- data projection differences：無 metadata-only 操作型場景
- route guard notes：role hard deny + scope verify
- pending confirmation：`REREVIEW_IN_PROGRESS` 改派策略

## 4.4 UI-012 初審作業頁

- page purpose：reviewer command page（initial review）
- inputs affecting UI：actor ownership、case status、submission existence(stage INITIAL)
- page modes：`NEW_INITIAL_REVIEW` / `EDIT_DRAFT` / `REVISION` / `READONLY_BLOCKED`
- mode resolution：owner+status 合法後，再由 submission status 決定
- role-sensitive behavior：僅 assigned reviewer；非本人禁止
- visible sections：case summary、mode hint、score sheet、comment/attachments、command bar、version summary
- available actions：start、save draft、submit、revise（符合 guard）
- hidden actions：非本人/非 REVIEWER 全隱藏
- disabled/blocked actions：CLOSED/CANCELLED、stage mismatch、not owner
- blocked reasons：`REVISION_NOT_ALLOWED` / `REVISION_ACTOR_MISMATCH`
- next-step hints：提交後進 detail/revision history
- data projection differences：本頁不設 metadata-only 主情境
- route guard notes：assignment ownership 必須在 route/data resolver 前驗證
- pending confirmation：`INITIAL_REVIEW_COMPLETED` 到 `PENDING_REREVIEW` 階段初審 revision 邊界

## 4.5 UI-013 複審作業頁

- page purpose：rereviewer command page（final decision）
- inputs affecting UI：actor ownership、case status、rereview submission stage
- page modes：`NEW_REREVIEW` / `EDIT_DRAFT` / `REVISION` / `READONLY_BLOCKED`
- mode resolution：owner+status guard 通過後由 draft/submitted 決定
- role-sensitive behavior：僅 assigned rereviewer
- visible sections：case summary、mode hint、initial summary、rereview form、final score、command bar、version summary
- available actions：start、save draft、submit rereview、revise rereview
- hidden actions：非 rereviewer 或非 owner
- disabled/blocked actions：CANCELLED 一律 blocked；CLOSED 僅 submit blocked、revise 視 D-021
- blocked reasons：stage mismatch / actor mismatch / terminal constraints
- next-step hints：提交後結案；revision 後回 revision history
- data projection differences：本頁不設 metadata-only 主情境
- route guard notes：`CLOSED` 進入策略需區分「revision allowed」與「new submit denied」
- decision backwrite（O1-C）：CLOSED 時「新提交 blocked、合法 rereview revision 入口可由本頁啟動」

## 4.6 UI-014 Revision 歷程頁

- page purpose：revision traceability page（閱讀與導頁，不直接提交）
- inputs affecting UI：role/scope、projection、case status、selected stage tab
- page modes：`HISTORY_ALL` / `HISTORY_INITIAL_ONLY` / `HISTORY_REREVIEW_ONLY` / `METADATA_HISTORY`
- mode resolution：先角色預設 tab，再允許 user 切換（tab=query preset）
- role-sensitive behavior：reviewer 偏 INITIAL；rereviewer 偏 REREVIEW；sysadmin metadata-only
- visible sections：summary、stage group tabs、timeline、summary/compare panel、navigation actions
- available actions：view revisions、compare、navigate to UI-012/UI-013（僅合法）
- hidden actions：本頁不顯示 direct submit revision
- disabled/blocked actions：不合法角色/範圍/terminal action entry
- blocked reasons：scope denied、actor mismatch、revision not allowed
- next-step hints：顯示「若需修正請前往對應作業頁」
- data projection differences：metadata-only 僅顯示白名單欄位（stage/revisionNo/submittedAt/submittedBy/currentEffective），不顯示 full answer diff
- route guard notes：forbidden 時不回傳 timeline 資料
- decision backwrite（O1-C）：CLOSED + 合法責任人顯示「前往複審修正」導頁入口，仍不在歷程頁直接提交

## 4.7 UI-006 抽選執行頁（最小必要）

- page purpose：sampling command page
- inputs affecting UI：project active、published version、role
- page modes：`READY_TO_PREVIEW` / `READY_TO_EXECUTE` / `EXECUTING` / `EXECUTION_RESULT` / `READONLY_BLOCKED`
- available actions：preview、execute、go batch list
- blocked reasons：project inactive / no published version / role denied
- pending confirmation：preview response schema 細節

## 4.8 UI-007 抽選批次列表頁（最小必要）

- page purpose：sampling batch query + retry entry
- inputs affecting UI：role/projection、batch status、execution mode
- page modes：`LIST_ACTIVE` / `LIST_METADATA_ONLY` / `FAILED_RETRY_FOCUS`
- available actions：view detail、view cases、retry（manager + failed）
- blocked reasons：not retryable / role denied / scope mismatch
- pending confirmation：retry chain 的完整可視欄位

---

## 5) 與 Roadmap 的關係

- 給 `ui-route-module-map.md`：每頁 route guard 與 redirect 決策。
- 給 `ui-view-model-spec.md`：每頁 mode resolution 輸入與輸出 contract。
- 給 `ui-api-binding-matrix.md`：actions -> commands/query mapping + blocked reason mapping。

---

## 6) Pending Confirmation（Cross-file Gaps）

1. `INITIAL_REVIEW_COMPLETED` 的 manager close / manager assign rereview command 是否已在所有 API 端點明確化。
2. metadata-only compare 欄位顯示密度（摘要 vs 完全遮罩）仍需 UI governance 層細化。
