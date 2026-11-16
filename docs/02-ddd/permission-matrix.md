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
4. revision 採 append-only，且 actor/stage guard 嚴格遵守 D-002 / D-003 / D-031 / D-032。

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
| UI-007 抽選批次列表頁 | R (metadata) | Y | N（D-051） | N（D-051） | Y |
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
| HOSPITAL_MANAGER | hospital-only | FULL（須通過 D-035） | 條件式；UNKNOWN 拒絕 |
| REVIEWER | self-assigned + self-submitted | FULL（within scope，須通過 D-035） | 條件式；UNKNOWN 拒絕 |
| REREVIEWER | self-assigned + self-submitted | 合法範圍且永久盲審（D-033） | 本人複審內容可見；初審答案／分數／評語與可反推衍生資訊不可見 |
| AUDITOR | authorized hospitals only | FULL（within authorization，須通過 D-035） | 條件式；UNKNOWN 拒絕 |

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
| reviseInitialReview (UI-012) | N | N | C | N | N | actor=original submitter, result not fixed, not CLOSED/CANCELLED |
| 固定初審（binding UNKNOWN） | N | N | C | N | N | 目前有效合法 INITIAL 提交者；未固定、非 terminal；既有 role/scope/ownership guard（DD-03） |
| startRereview (UI-013) | N | N | N | Y | N | actor = assigned rereviewer; initial result fixed |
| saveRereviewDraft (UI-013) | N | N | N | Y | N | actor/scope valid |
| submitRereview (UI-013) | N | N | N | Y | N | draft valid + actor owned |
| reviseRereview (UI-013) | N | N | N | C | N | actor=original submitter, result not fixed, CLOSED/CANCELLED denied |
| viewRevisionHistory (UI-014) | R (metadata) | Y | Y (scope) | Y (scope) | Y | data scope + projection policy |

---

## 7) Scope / Illegal Entry Handling Matrix

| Scenario | Handling |
|---|---|
| 角色不符頁面（例如 reviewer 進 UI-011） | route guard deny（403 / redirect to allowed page） |
| scope 不符（跨院或非本人案件） | deny + forbidden message，不回 full content |
| metadata-only actor 請求 full detail | downgrade 為 metadata projection；隱藏敏感欄位 |
| terminal state 下嘗試 command | 顯示 blocked reason + disabled/hidden action（不得 silent fail） |
| CLOSED + 任一 stage revision | deny；原提交者亦不可例外修正／reopen／新提交（D-032） |
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

## 10) 固定、結案與永久盲審（D-031～D-033）

| 業務操作 | Actor 與必要條件 |
|---|---|
| 初審 revision | 原提交者；已合法提交、未固定、未 CLOSED／CANCELLED |
| 初審固定 | 僅目前有效初審結果的合法提交醫師；須通過既有 REVIEWER、ownership、hospital scope guard；manager／指派身份或歷史版本提交者不足以授權（DD-03） |
| 進入複審（自動／手工） | 初審已固定；自動依既有閾值，手工仍由 manager |
| 無需複審時正式結案 | HOSPITAL_MANAGER；初審已固定，且不需複審 |
| 複審提交 | assigned rereviewer 且擁有合法 submission；初審已固定；不結案 |
| 複審 revision | 合法原提交者；結果未固定，案件未 CLOSED／CANCELLED |
| 複審正式結案（獨立 Close Command） | 目前有效合法複審結果的提交醫師；REREVIEWER、ownership、hospital scope 均合法，初審已固定且無其他既有阻擋條件；同時固定尚未固定的複審結果。僅 manager／指派／複審角色／歷史提交者不足以授權（D-034） |
| CLOSED revision／reopen | 所有 actor 均禁止 |

複審醫師所有階段皆不得取得初審答案、分數、評語，包含提交後、revision、固定後及 CLOSED。finalScore／preview／diff 等可反推初審資訊者亦不得提供；所有查詢、command ack、歷程／compare、報表／匯出與快取均適用，不能靠頁面隱藏代替 response 保護。
其他角色存取政策不變；多角色 actor 的技術授權合成 unknown，不得以切換入口繞過同案盲審。


## D-035 歷史身份 Fail-closed 讀取契約

完整初審內容的 Role／hospital／ownership 合法不足以單獨授權：Proven historical 與 UNKNOWN 均拒絕；Proven non-historical 才能在既有 scope 下繼續。現有證據來源尚無負面身份證明能力，因此 Manager／Auditor、無需複審案件及初審 REVIEWER 本人的完整讀取均可能 403。合法 REREVIEWER 仍只讀本人複審內容，初審永遠遮罩；SYSTEM_ADMIN 僅 metadata-only，D-024 scope 不變。

> D-055（2026-11-16）：以只增不改的複審指派歷史提供負面身份證明。案件歷史完整（導入後建立的案件）且登入者不在歷史中，即為 Proven non-historical；歷史不完整仍為 UNKNOWN 並拒絕。HOSPITAL_MANAGER、AUDITOR、REVIEWER 在歷史完整的案件恢復完整讀取。每次准許完整初審讀取都記錄讀取者，讀過的人不得再被指派或改派為同案複審醫師（`422 REREVIEWER_HAS_READ_INITIAL`）。其餘規則不變。


## D-036 — UI-010 metadata 八欄與 Lifecycle 延後邊界

UI-010 SYSTEM_ADMIN metadata-only 僅含 `caseId`, `caseStatus`, `projectName`, `hospitalId`, `reviewerId`, `rereviewerId`, `updatedAt`, `latestRevisionSummary`。`lifecycleLatestStage` 暫移除，JSON 必須不存在，不能回 null；目前狀態使用 `caseStatus`。

`updatedAt`、`latestRevisionSummary` 的值契約與實作仍未完成（UNKNOWN），目前 null 不表示已完成。不得加入 FULL-only、敏感巢狀資訊或因新增 DTO getter 自動擴大白名單。

既有 Lifecycle Query／API 與 EX-039／TS-TR-005 完整關鍵路徑需求保留；新增 Milestone／Latest Stage 投影延後，未取消需求。D-035 Fail-closed、D-033 永久盲審及既有 Role／Scope 不變。


## D-037 — updatedAt 契約

D-037 不授予新權限；無變更仍先驗 role／scope／status／owner／有效引用。D-035／D-033 不變；SYSTEM_ADMIN UI-010 保留 D-036 八欄，只補 updatedAt 語意，不擴大敏感內容可見性。


## D-038 — No-change API 契約補充

無變更的 Draft Save 回 `409 Conflict`／`NO_CHANGES`。完整內容見 `decision-log.md` D-038；比較方式見 D-044。
