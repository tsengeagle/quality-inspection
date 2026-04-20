# Demo Data Contract v1 - Quality Inspection — Demo Automation Phase

## 1. 文件目的

本文件定義品質審查系統在 **Demo Automation Phase** 中所依賴的穩定展示資料契約。

此文件的用途：

- 固定 demo / e2e 所依賴的 seed / fixture / local data
- 定義哪些資料值必須穩定存在
- 定義哪些欄位是展示與驗收的正式觀察點
- 作為以下交付物的共同資料基線：
  - demo launcher
  - Playwright preflight
  - Playwright demo script
  - demo / e2e 雙模式切換
  - 最小 SOP

本文件不是：

- domain model
- API spec
- persistence schema
- 測試程式碼

---

## 2. 使用原則

1. 本文件只記錄 **demo / e2e 必須穩定依賴** 的資料。
2. 只有會影響展示與驗收的資料，才應納入本文件。
3. 若某資料值可變但不影響展示成功，應明確標示為 `flexible`。
4. 若某資料值一旦改變會讓 demo / e2e 失效，應標示為 `fixed`。
5. 若某欄位目前尚無穩定 authority source，不得寫成正式依賴值。
6. 若後續變更 seed / query / fallback 行為，應先更新本文件，再更新 Playwright 腳本。

---

## 3. 適用範圍

本文件適用於以下頁面：

- UI-006 抽選執行頁
- UI-007 抽選批次列表頁
- UI-011 案件分派頁

本文件不處理：

- integrated shell demo
- gateway integration
- UI-007 批次詳情 deferred
- 完整案件分派提交流程
- `effectivePeriod`
- 真正獨立語意的 `current scoreSheetVersionId`

---

## 4. Demo 資料基線總覽

### 4.1 固定主識別資料

以下資料視為本 demo 的正式固定識別值：

```yaml
demo_anchor_data:
  projectId: PROJECT-001
  batchId: BATCH-001
  caseId: CASE-011
```

### 4.2 預設執行模式

```yaml
demo_runtime_mode:
  frontend: standalone
  proxy: local dev proxy
  backend: local profile
  data_source: local demo seed
```

### 4.3 預設資料來源

請在此填入實際 seed / fixture 來源檔：

```yaml
data_authority_sources:
  backend_local_seed:
    - LocalStandaloneDemoDataSeed.java
  supporting_seed:
    - LocalHisStaffDirectorySeed.java
  note: >
    只有這些來源被視為 demo 正式 authority source。
    其他臨時手動資料不得視為正式依賴。
```

---

## 5. 固定資料契約

---

## 5.1 Project Anchor Contract

### 目的

定義 UI-006 / UI-007 / UI-011 共用的專案級固定資料。

### 正式依賴資料

```yaml
project_anchor_contract:
  projectId:
    value: PROJECT-001
    stability: fixed
    required: true

  projectName:
    value: 2026 Q2 門診品質抽選專案
    stability: fixed
    required: true

  projectStatus:
    value: ACTIVE
    stability: fixed
    required: true

  versionNo:
    value: 1
    display_form: v1
    stability: fixed
    required: true

  scoreSheetId:
    value: SS-PROJECT-001-V1
    stability: fixed
    required: true

  qualityThreshold:
    value: 85.00
    stability: fixed
    required: true
```

### 說明

* 上述欄位若改變，將直接影響：

  * UI-006
  * UI-007
  * UI-011
  * Playwright preflight
  * Playwright demo mode
  * Playwright e2e mode

---

## 5.2 Batch Anchor Contract

### 目的

定義 UI-006 / UI-007 所依賴的批次級固定資料。

### 正式依賴資料

```yaml
batch_anchor_contract:
  batchId:
    value: BATCH-001
    stability: fixed
    required: true

  batchStatus:
    value: COMPLETED
    stability: fixed
    required: true

  projectId:
    value: PROJECT-001
    stability: fixed
    required: true

  projectVersionNumber:
    value: 1
    display_form: v1
    stability: fixed
    required: true

  projectName:
    value: 2026 Q2 門診品質抽選專案
    stability: fixed
    required: true

  excludedCount:
    value: 0
    stability: fixed
    required: true

  startedAt:
    value: 2026-04-18T01:00:00Z
    stability: fixed
    required: true

  finishedAt:
    value: 2026-04-18T01:05:00Z
    stability: fixed
    required: true
```

### 說明

* `startedAt` / `finishedAt` 目前是 UI-006 最近一次正式結果摘要的重要觀察點。
* `excludedCount` 是 UI-007 重要摘要欄位。
* 若這些值改變，需同步更新：

  * demo script
  * acceptance checklist
  * Playwright assertions

---

## 5.3 Case Anchor Contract

### 目的

定義 UI-011 所依賴的案件級固定資料。

### 正式依賴資料

```yaml
case_anchor_contract:
  caseId:
    value: CASE-011
    stability: fixed
    required: true

  projectId:
    value: PROJECT-001
    stability: fixed
    required: true

  projectName:
    value: 2026 Q2 門診品質抽選專案
    stability: fixed
    required: true

  sourceCaseKey:
    value: MR-CASE-011
    stability: fixed
    required: true

  pageMode:
    value: ASSIGN_INITIAL
    stability: fixed
    required: true
```

### 說明

* `CASE-011` 是 UI-011 demo 的正式 anchor case。
* `sourceCaseKey` 是本輪 Phase 1 已驗證的正式畫面觀察點之一。

---

## 5.4 Assignment Fallback Contract

### 目的

定義 UI-011 在「尚未指派」情境下的穩定 fallback 行為。

### 正式依賴資料

```yaml
assignment_fallback_contract:
  currentReviewer:
    value: null
    stability: fixed
    required: true

  currentRereviewer:
    value: null
    stability: fixed
    required: true

  assignedAt:
    api_value: null
    ui_expected_display: 尚未指派
    stability: fixed
    required: true
```

### 說明

* 此契約的重點不是 `assignedAt` 的實際時間值，而是：

  * 若無 reviewer
  * 頁面必須穩定顯示 fallback
  * 不得出現 binding 斷裂、空 label、`undefined`

---

## 5.5 Candidate List Contract

### 目的

定義 UI-011 候選人清單的最小穩定依賴。

### 正式依賴資料

```yaml
candidate_list_contract:
  role:
    value: REVIEWER
    stability: fixed
    required: true

  minimum_visible_candidates:
    value: 3
    stability: flexible
    required: true

  expected_candidate_ids:
    - reviewer-1
    - reviewer-2
    - reviewer-3

  expected_inactive_candidate:
    value: reviewer-inactive
    stability: fixed
    required: true

  inactive_candidate_blocked_reason:
    value: CANDIDATE_INACTIVE
    stability: fixed
    required: true
```

### 說明

* 若 candidate 順序可能變動，可把順序標示為 `flexible`，但 identity 必須穩定。
* 若 inactive candidate 的存在是 demo 重點，則其 id 與 blockedReason 應固定。

---

## 6. 頁面對照表

---

## 6.1 UI-006 對照資料

```yaml
ui_006_contract:
  route: /sampling/PROJECT-001
  depends_on:
    - project_anchor_contract
    - batch_anchor_contract

  required_visible_fields:
    - projectStatus
    - versionNo
    - scoreSheetId
    - qualityThreshold
    - batchId
    - startedAt
    - finishedAt
```

---

## 6.2 UI-007 對照資料

```yaml
ui_007_contract:
  route: /sampling/batches
  depends_on:
    - batch_anchor_contract

  required_visible_fields:
    - batchId
    - batchStatus
    - projectName
    - projectVersionNumber
    - excludedCount
```

---

## 6.3 UI-011 對照資料

```yaml
ui_011_contract:
  route: /cases/CASE-011/assign
  depends_on:
    - case_anchor_contract
    - assignment_fallback_contract
    - candidate_list_contract

  required_visible_fields:
    - projectName
    - sourceCaseKey
    - assignedAt_or_fallback
    - candidateList
    - inactiveCandidateBlockedReason
```

---

## 7. 固定值與可變值規則

### 7.1 Fixed

以下資料一旦改變，視為 breaking change：

* `PROJECT-001`
* `BATCH-001`
* `CASE-011`
* projectName
* projectStatus
* versionNo
* scoreSheetId
* qualityThreshold
* batchStatus
* sourceCaseKey
* fallback 預期文案
* inactive candidate 的識別值【若納入展示重點】

### 7.2 Flexible

以下資料可允許小幅變動，但需評估是否影響 demo / e2e：

* candidate 顯示順序
* 額外候選人數量
* 一些非關鍵附屬欄位
* 若未納入正式觀察點的 secondary text

---

## 8. Breaking Change 判定規則

若出現以下任一情況，視為 breaking change，必須先更新本文件，再更新 demo / e2e 腳本：

1. projectId / batchId / caseId 改變
2. projectName 改變
3. version 顯示規則改變
4. scoreSheetId 改變
5. qualityThreshold 改變
6. batchStatus 改變
7. sourceCaseKey 改變
8. assignedAt fallback 文案改變
9. inactive candidate 或 blockedReason 改變

---

## 9. API / UI 對照原則

本 demo data contract 的正式驗證方式是：

* API response 中存在該欄位
* UI 上可見該欄位或對應 fallback
* API 與 UI 的值一致
* 無 `undefined`、空 label、壞掉 fallback

---

## 10. Not In Scope Data

以下資料本版明確不納入正式 demo data contract：

```yaml
not_in_scope_data:
  - effectivePeriod
  - currentScoreSheetVersionId_with_independent_semantics
  - batch_detail_deferred_fields
  - full_assignment_submission_result
  - integrated_shell_context
  - gateway_runtime_metadata
```

### 說明

* 這些資料目前不應被 Playwright demo / e2e 視為正式依賴。
* 若未來納入，應另開版本更新本文件。

---

## 11. 驗收映射

本文件應映射到：

* `demo-script.md`
* `demo-acceptance-checklist.md`
* Playwright preflight assertions
* Playwright demo mode assertions
* Playwright e2e mode assertions

---

## 12. 待補區

後續可補，但不在本版定稿範圍：

* candidate blockedReason 的正式固定值
* batch row 其他 secondary fields 是否納入固定觀察點
* demo mode 是否要容忍時間格式轉換
* 是否要把 null / fallback 規則做成獨立 appendix
