# UI Route Module Map (Cases Bridge)

## 文件狀態
- 類型：新增
- Layer：Layer 2 Repo-Aligned Frontend Bridge
- 範圍：僅 cases 主幹（UI-009~UI-014）

---

## 1) Source Inputs

- `docs/06-frontend/frontend-design-method.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/06-frontend/ui-state-actions-matrix.md`
- `docs/06-frontend/frontend-test-scenarios.md`
- `docs/01-requirements/decision-log.md`
- `docs/03-contracts/api-spec.md`
- `docs/05-ui/ui-information-architecture.md`
- `docs/05-ui/ui-page-inventory.md`
- `docs/05-ui/ui-page-spec-ui009案件列表頁.md`
- `docs/05-ui/ui-page-spec-ui010案件詳情頁.md`
- `docs/05-ui/ui-page-spec-ui011案件分派頁.md`
- `docs/05-ui/ui-page-spec-ui012初審作業頁.md`
- `docs/05-ui/ui-page-spec-ui013複審作業頁.md`
- `docs/05-ui/ui-page-spec-ui014revision歷程頁.md`
- `docs/06-frontend/frontend-design-roadmap.md`

---

## 2) 與 Foundational Truth 的依賴

```yaml
depends_on:
  page_modes_and_actions:
    from: ui-state-actions-matrix.md
  role_scope_projection:
    from: permission-matrix.md
  lifecycle_and_terminal_rules:
    from: state-machine.md
  revision_semantics:
    from: decision-log.md
  bridge_method_rules:
    from: frontend-design-method.md
```

---

## 3) Repo-Aligned Bridge 原則

1. 僅做 route/module/guard mapping，不產生 Angular code。
2. 以 `features/` 做 cases feature cluster 主邊界，page 為 use-case entry。
3. metadata-only 為同 route projection mode，不新增 metadata route。
4. command pages（UI-011/012/013）與 detail/history page（UI-010/014）分離 lazy-load 邊界。
5. tab 一律 query preset，使用 query params 持久化。

---

## 4) Route Group 與 Feature Cluster 切分

```yaml
route_groups:
  - cases-worklist: /cases
  - cases-detail-center: /cases/:caseId
  - cases-assignment-command: /cases/:caseId/assign
  - cases-initial-review-command: /cases/:caseId/initial-review
  - cases-rereview-command: /cases/:caseId/rereview
  - cases-revision-history: /cases/:caseId/revisions

feature_cluster:
  - feature_key: cases
    repo_alignment:
      shell: role-aware navigation entry
      features: cases cluster route boundary
      guards: role/scope/ownership guards
      state: query preset + mode resolution state
```

---

## 5) Route / Module Mapping Table

> 備註：`featureModule` 與 `routeGroup` 為 bridge 建議命名；非既有程式碼真相。

| routePath | pageId | pageName | featureModule (bridge) | routeGroup | ownership | entrySource | guardType | lazyLoadBoundary | queryStatePolicy | notes |
|---|---|---|---|---|---|---|---|---|---|---|
| `/cases` | UI-009 | 案件列表頁 | `cases-worklist` | cases-worklist | cases feature | shell nav / detail back | role+scope guard, projection guard | worklist boundary | persist `tab`, filters, pagination, sort | tab = query preset |
| `/cases/:caseId` | UI-010 | 案件詳情頁 | `cases-detail` | cases-detail-center | cases feature | list row click / deep link | role+scope guard | detail center boundary | preserve return query context | control center, no submit commands |
| `/cases/:caseId/assign` | UI-011 | 案件分派頁 | `cases-assignment-command` | cases-assignment-command | assignment subdomain | detail action / list action | role hard guard (`HOSPITAL_MANAGER`) + scope + status guard | isolated command boundary | inherit return-to list/detail context | command page |
| `/cases/:caseId/initial-review` | UI-012 | 初審作業頁 | `cases-initial-review-command` | cases-initial-review-command | review operations | detail/revision history entry | role+ownership+status guard | isolated command boundary | optional draft/revision intent in query | command page; revision != edit |
| `/cases/:caseId/rereview` | UI-013 | 複審作業頁 | `cases-rereview-command` | cases-rereview-command | rereview operations | detail/revision history entry | role+ownership+status guard | isolated command boundary | optional draft/revision intent in query | command page; final decision flow |
| `/cases/:caseId/revisions` | UI-014 | Revision 歷程頁 | `cases-revision-history` | cases-revision-history | traceability | detail action / list action | role+scope guard + projection guard | history boundary | persist stage tab (`ALL/INITIAL_ONLY/REREVIEW_ONLY`) and compare selection | history page, navigate-only |

---

## 6) Guard Boundary 建議

```yaml
guard_boundary:
  route_level:
    - role_guard
    - scope_guard
    - projection_guard(metadata-only vs FULL)
  page_mode_level:
    - status_guard
    - ownership_guard
    - action_policy_guard
  command_level:
    - terminal_state_guard
    - API_error_guard_to_blocked_reason
```

非法進入處理對齊 foundational：
- role mismatch → deny / redirect
- scope mismatch → forbidden
- terminal state command → blocked mode with reason

---

## 7) Shell Navigation Entry 與 Role-Aware Navigation

| Role | Primary Entry to Cases Cluster | Default Landing | Navigation Rule |
|---|---|---|---|
| HOSPITAL_MANAGER | Cases menu | `/cases?tab=manager_default` | 可見 assign/detail/revisions，依狀態顯示 command entry |
| REVIEWER | My Cases | `/cases?tab=reviewer_default` | 初審入口僅在 ownership + status 合法時可見 |
| REREVIEWER | My Rereview Cases | `/cases?tab=rereviewer_default` | 複審入口僅在 ownership + status 合法時可見 |
| AUDITOR | Audit Cases | `/cases?tab=auditor_default` | read-first，無 command entry |
| SYSTEM_ADMIN | Metadata Cases | `/cases?tab=metadata_default` | metadata-only projection，不顯示業務 command |

---

## 8) Query Param / Filter Persistence 原則

```yaml
query_state_policy:
  worklist:
    persist:
      - tab
      - filters
      - pagination
      - sort
    restore_on_back_navigation: true
  detail_to_worklist_back:
    preserve_list_context: true
  revision_history:
    persist:
      - stage_tab
      - selected_revision
      - compare_target
```

---

## 9) Cross-file Consistency Check（Bridge 視角）

### 一致項（高信心）
- page identity / command-vs-detail / tab=query preset 與 foundational method 一致。
- route-level guard + action-level guard 分層與 permission/state/action matrix 一致。

### Cross-file Inconsistency（需待確認）
1. `INITIAL_REVIEW_COMPLETED` 的 manager handoff commands 在 API 文件是否完整顯式化仍不足。

---

## 10) Pending Confirmation

1. shell 內 role default tab key 最終命名（本文件僅提供 bridge key）。

## D-031～D-033 同步契約

以下映射以 Decision Log D-031～D-033、state-machine 與 permission-matrix 為 authority。

路由／guard 不得把複審已提交視為 CLOSED 或允許 CLOSED revision。初審已固定才可進複審；未固定合法結果的原提交者才可修訂（D-031／032）。資料 resolver 遵守 D-033 永久盲審，不能以 FULL 或合法 route entry 放行初審內容／可反推分數。初審固定角色依 DD-03；複審正式結案的 API binding 見 D-053，在複審頁以按鈕執行（D-053 第 8 點），不新增 route。

驗證追溯 EX-041～052／TS-DA-001～012；狀態、儲存與新操作表示未知處不由 frontend 自行定案。
