# Permission Matrix v3 (Foundational Convergence)

## 文件狀態
- 類型：更新（整合既有 `docs/02-ddd/permission-matrix.md`）
- 目的：收斂角色 × 頁面 × 資料可見性 × 操作權限 × scope/illegal entry，供 frontend action policy 與 route guard 直接使用

---

## 1) Source Inputs

- `docs/01-requirements/decision-log.md`
- `docs/02-ddd/permission-matrix.md`（v2 既有內容）
- `docs/02-ddd/state-machine.md`
- `docs/03-contracts/api-spec.md`
- `docs/05-ui/ui-information-architecture.md`
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

1. page access、data visibility、action executability 分離建模。
2. metadata-only / FULL 屬同頁不同 projection，不拆雙頁。
3. action visibility 與 action executability 由統一 policy 推導，不散落 component if-else。
4. revision 採 append-only，且 actor/stage guard 嚴格遵守 D-002 / D-003 / D-021。

---

## 3) Roles

```yaml
roles:
  - SYSTEM_ADMIN
  - HOSPITAL_MANAGER
  - REVIEWER
  - REREVIEWER
  - AUDITOR
```

---

## 4) Page Access Matrix

Legend: `Y` 可進入、`R` 僅唯讀/有限視角、`N` 不可進入

| Page | SYSTEM_ADMIN | HOSPITAL_MANAGER | REVIEWER | REREVIEWER | AUDITOR |
|---|---:|---:|---:|---:|---:|
| UI-006 抽選執行頁 | N | Y | N | N | N |
| UI-007 抽選批次列表頁 | R (metadata) | Y | R | R | Y |
| UI-009 案件列表頁 | R (metadata) | Y | Y | Y | Y |
| UI-010 案件詳情頁 | R (metadata) | Y | Y (合法範圍) | Y (合法範圍) | Y |
| UI-011 案件分派頁 | N | Y | N | N | N |
| UI-012 初審作業頁 | N | N | Y (assigned/self) | N | N |
| UI-013 複審作業頁 | N | N | N | Y (assigned/self) | N |
| UI-014 Revision 歷程頁 | R (metadata) | Y | Y (合法範圍) | Y (合法範圍) | Y |

---

## 5) Data Visibility Matrix

| Role | Scope | Default Projection | Full Answers/Comments/Attachments |
|---|---|---|---|
| SYSTEM_ADMIN | GLOBAL_METADATA | metadata-only | N |
| HOSPITAL_MANAGER | hospital-only | FULL | Y |
| REVIEWER | self-assigned + self-submitted | FULL (within scope) | Y |
| REREVIEWER | self-assigned + self-submitted | FULL (within scope) | Y |
| AUDITOR | authorized hospitals only | FULL (within authorization) | Y |

補充：
- `SYSTEM_ADMIN` detail/revision 仍可進入，但必須 masked/metadata projection。
- `REVIEWER` / `REREVIEWER` 不得查看超出 assignment/scope 的 case/submission。

---


## 5.1 Metadata-only 欄位白名單策略（O3-C 已裁決）

```yaml
metadata_whitelist_strategy:
  model: SHARED_BASE_PLUS_PAGE_EXTENSION
  shared_base:
    - caseId
    - caseStatus
    - projectName
    - hospitalId
    - reviewerId
    - rereviewerId
    - updatedAt
  page_extension:
    UI-009:
      - batchId
      - dueAt
    UI-010:
      - lifecycleLatestStage
      - latestRevisionSummary
    UI-014:
      - stage
      - revisionNo
      - submittedAt
      - submittedBy
      - isCurrentEffective
```

補充：`SYSTEM_ADMIN` 僅可見上述白名單集合，不得取得 full answers/comments/attachments。

## 6) Action Permission Matrix（主幹 + 最小 sampling）

Legend: `Y` 可執行、`C` 條件式、`N` 不可

| Action | SYSTEM_ADMIN | HOSPITAL_MANAGER | REVIEWER | REREVIEWER | AUDITOR | Key Guard |
|---|---:|---:|---:|---:|---:|---|
| executeSampling (UI-006) | N | Y | N | N | N | project active + published version |
| retrySamplingBatch (UI-007) | N | Y | N | N | N | batch retryable |
| assignReviewer / reassignReviewer (UI-011) | N | Y | N | N | N | case state assignable |
| assignRereviewer / reassignRereviewer (UI-011) | N | Y | N | N | N | rereview stage assignable |
| startInitialReview (UI-012) | N | N | Y | N | N | actor = assigned reviewer |
| saveInitialDraft (UI-012) | N | N | Y | N | N | actor/scope valid |
| submitInitialReview (UI-012) | N | N | Y | N | N | draft valid + actor owned |
| reviseInitialReview (UI-012) | N | N | C | N | N | actor=original reviewer, not CLOSED/CANCELLED |
| startRereview (UI-013) | N | N | N | Y | N | actor = assigned rereviewer |
| saveRereviewDraft (UI-013) | N | N | N | Y | N | actor/scope valid |
| submitRereview (UI-013) | N | N | N | Y | N | draft valid + actor owned |
| reviseRereview (UI-013) | N | N | N | C | N | actor=original rereviewer, CLOSED allowed, CANCELLED denied |
| viewRevisionHistory (UI-014) | R (metadata) | Y | Y (scope) | Y (scope) | Y | data scope + projection policy |

---

## 7) Scope / Illegal Entry Handling Matrix

| Scenario | Handling |
|---|---|
| 角色不符頁面（例如 reviewer 進 UI-011） | route guard deny（403 / redirect to allowed page） |
| scope 不符（跨院或非本人案件） | deny + forbidden message，不回 full content |
| metadata-only actor 請求 full detail | downgrade 為 metadata projection；隱藏敏感欄位 |
| terminal state 下嘗試 command | 顯示 blocked reason + disabled/hidden action（不得 silent fail） |
| CLOSED + rereview revision 合法責任人 | 允許導向 revision command flow（UI-014 -> UI-013），仍禁止 reopen/new submit |
| submission actor mismatch | deny（`REVISION_ACTOR_MISMATCH` / `SUBMISSION_NOT_OWNED_BY_ACTOR`） |

---

## 8) 與 Roadmap 的關係

本文件是以下 bridge 文件的 policy source：

- `ui-route-module-map.md`：page-level route guard rule
- `ui-view-model-spec.md`：role-sensitive mode resolution
- `ui-api-binding-matrix.md`：button visibility/executability 與 API guard mapping

---

## 9) Pending Confirmation

1. SYSTEM_ADMIN 在 UI-006/UI-011 是否允許 metadata-readonly 落地入口（目前採 N）。
2. `REREVIEW_IN_PROGRESS` 是否允許 manager 改派 rereviewer（UI-011 目前偏保守不開）。
3. REVIEWER / REREVIEWER 在 UI-014 的 default tab 是否固定（目前依 page spec 建議，不宣稱強制）。
4. audit trail 入口在各頁權限是否需單獨 matrix（本版先視為 UI-010/UI-014 的 read capability）。
