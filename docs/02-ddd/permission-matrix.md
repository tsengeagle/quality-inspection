# Permission Matrix v2

## 1. 文件目的

本文件用於定義品質審查系統的角色權限、資料範圍與內容可見性規則，作為以下設計與實作的授權依據：

- 畫面功能顯示與操作限制
- API authorization policy
- query scope 與資料過濾
- revision / audit / reporting 權限邊界

本文件只處理「誰可以做什麼、誰可以看到什麼」。  
不處理 domain state transition、versioning、snapshot 或 aggregate invariant；這些應由 domain model、state machine 與 versioning 文件負責。

---

## 2. 設計原則

本系統權限設計遵守以下原則：

1. 系統管理員負責平台與系統治理，不直接介入院內審查業務。
2. 院方管理者負責本院審查專案、抽選、分派、查詢與追蹤。
3. 審查醫師負責初審。
4. 複審醫師負責複審並決定最終分數。
5. 稽核角色僅能查詢，不得修改規則或審查結果。
6. 已送出之審查結果不得直接覆寫；若允許修正，必須保留修正紀錄。
7. 結案後不允許 revision。
8. 權限需同時區分：
   - 操作權限
   - 資料範圍權限
   - 內容明細可見性

---

## 3. 角色定義

```yaml
roles:
  - SYSTEM_ADMIN
  - HOSPITAL_MANAGER
  - REVIEWER
  - REREVIEWER
  - AUDITOR
```

### 3.1 SYSTEM_ADMIN

平台治理角色。
負責系統共用參數、帳號、角色、權限、系統監控、異常追蹤與平台層級查詢。

### 3.2 HOSPITAL_MANAGER

院方管理角色。
負責本院審查專案、規則、抽選、分派、進度追蹤、報表與本院案件明細查閱。

### 3.3 REVIEWER

初審角色。
負責被指派案件之初審草稿、提交與未結案案件的本人初審修正。

### 3.4 REREVIEWER

複審角色。
負責被指派複審案件之草稿、提交、最終分數決定與未結案案件的本人複審修正。

### 3.5 AUDITOR

稽核 / 查詢角色。
負責在授權範圍內查詢抽選、分派、審查、複審、異動歷程與稽核資料；不得修改規則或審查結果。

---

## 4. 資料範圍模型

```yaml
data_scope:
  - GLOBAL_METADATA
  - HOSPITAL
  - SELF_ASSIGNED
  - AUTHORIZED_HOSPITALS_ONLY
```

### 4.1 範圍說明

* `GLOBAL_METADATA`

  * 可跨院查看平台層級 metadata、執行狀態、錯誤訊息、稽核事件標頭
  * 不含完整評分內容、作答明細、評語全文、附件內容

* `HOSPITAL`

  * 僅限所屬院區資料

* `SELF_ASSIGNED`

  * 僅限自己被指派案件，及自己可合法查看的提交內容

* `AUTHORIZED_HOSPITALS_ONLY`

  * 僅限被授權的院區或授權範圍
  * 不具預設跨院全域查詢權

### 4.2 角色預設資料範圍

| 角色               | 預設資料範圍                    |
| ---------------- | ------------------------- |
| SYSTEM_ADMIN     | GLOBAL_METADATA           |
| HOSPITAL_MANAGER | HOSPITAL                  |
| REVIEWER         | SELF_ASSIGNED + 必要本院輔助資訊  |
| REREVIEWER       | SELF_ASSIGNED + 必要本院輔助資訊  |
| AUDITOR          | AUTHORIZED_HOSPITALS_ONLY |

---

## 5. 內容可見性規則

本節處理「即使可查某筆資料，是否可以看到完整內容」。

```yaml
content_visibility:
  SYSTEM_ADMIN:
    can_view_metadata: true
    can_view_full_answers: false
    can_view_full_comments: false
    can_view_attachment_content: false

  HOSPITAL_MANAGER:
    can_view_metadata: true
    can_view_full_answers: true
    can_view_full_comments: true
    can_view_attachment_content: true
    scope: hospital_only

  REVIEWER:
    can_view_metadata: true
    can_view_full_answers: true
    can_view_full_comments: true
    can_view_attachment_content: true
    scope: self_assigned_or_self_submitted

  REREVIEWER:
    can_view_metadata: true
    can_view_full_answers: true
    can_view_full_comments: true
    can_view_attachment_content: true
    scope: self_assigned_or_self_submitted

  AUDITOR:
    can_view_metadata: true
    can_view_full_answers: true
    can_view_full_comments: true
    can_view_attachment_content: true
    scope: authorized_hospitals_only
```

### 5.1 補充說明

1. SYSTEM_ADMIN 僅可查看平台治理所需 metadata，不可查看完整評分內容。
2. HOSPITAL_MANAGER 可查看本院案件完整作答明細。
3. REVIEWER 與 REREVIEWER 只能查看其合法範圍內的案件與提交內容。
4. AUDITOR 可查看授權院區內的完整內容，但不得修改規則或審查結果。

---

## 6. 操作權限矩陣

---

## 6.1 主檔 / 專案 / 規則

| 功能               | SYSTEM_ADMIN | HOSPITAL_MANAGER | REVIEWER | REREVIEWER | AUDITOR |
| ---------------- | -----------: | ---------------: | -------: | ---------: | ------: |
| 維護院區主檔           |            Y |                N |        N |          N |       N |
| 維護科別主檔           |            Y |                N |        N |          N |       N |
| 維護帳號 / 角色 / 權限   |            Y |                N |        N |          N |       N |
| 建立審查專案           |            N |                Y |        N |          N |       N |
| 修改審查專案草稿 / 未發布版本 |            N |                Y |        N |          N |       N |
| 啟用 / 停用審查專案      |            N |                Y |        N |          N |       N |
| 維護抽選規則           |            N |                Y |        N |          N |       N |
| 維護評分表草稿          |            N |                Y |        N |          N |       N |
| 發布專案版本           |            N |                Y |        N |          N |       N |
| 發布評分表版本          |            N |                Y |        N |          N |       N |
| 查詢專案與版本          |            Y |                Y |        R |          R |       R |

### 補充

* `Y`：可操作
* `R`：唯讀
* `N`：不可操作

---

## 6.2 抽選與批次

| 功能              |    SYSTEM_ADMIN | HOSPITAL_MANAGER | REVIEWER | REREVIEWER | AUDITOR |
| --------------- | --------------: | ---------------: | -------: | ---------: | ------: |
| 手動執行抽選          |               N |                Y |        N |          N |       N |
| 排程抽選設定          |               N |                Y |        N |          N |       N |
| 查詢抽選批次 metadata |               Y |                Y |        R |          R |       R |
| 查詢抽選依據          | Y metadata only |                Y |        R |          R |       R |
| 匯出抽選結果          | Y metadata only |                Y |        N |          N |       R |
| 失敗重跑抽選          |               N |                Y |        N |          N |       N |

### 補充

1. SYSTEM_ADMIN 對抽選批次僅有 metadata 查詢與平台監控用途。
2. HOSPITAL_MANAGER 可查詢本院抽選批次完整資訊並執行重跑。
3. AUDITOR 僅限授權範圍內查詢。

---

## 6.3 案件分派與期限

| 功能              | SYSTEM_ADMIN | HOSPITAL_MANAGER | REVIEWER | REREVIEWER | AUDITOR |
| --------------- | -----------: | ---------------: | -------: | ---------: | ------: |
| 指派初審醫師          |            N |                Y |        N |          N |       N |
| 改派初審醫師          |            N |                Y |        N |          N |       N |
| 設定審查期限          |            N |                Y |        N |          N |       N |
| 指派複審醫師          |            N |                Y |        N |          N |       N |
| 改派複審醫師          |            N |                Y |        N |          N |       N |
| 查詢案件進度 metadata |            Y |                Y |        R |          R |       R |

### 補充

* SYSTEM_ADMIN 不直接介入院內案件分派。
* HOSPITAL_MANAGER 擁有本院案件分派控制權。

---

## 6.4 初審作業

| 功能              | SYSTEM_ADMIN | HOSPITAL_MANAGER | REVIEWER | REREVIEWER | AUDITOR |
| --------------- | -----------: | ---------------: | -------: | ---------: | ------: |
| 查看待審案件          |            N |                R |        Y |          N |       R |
| 保存初審草稿          |            N |                N |        Y |          N |       N |
| 送出初審結果          |            N |                N |        Y |          N |       N |
| 上傳初審附件          |            N |                N |        Y |          N |       N |
| 查看初審結果 metadata |            Y |                Y |        Y |          R |       R |
| 查看初審完整作答明細      |            N |             Y 本院 |  僅本人合法範圍 |     R 合法範圍 |  R 授權範圍 |
| 修正已提交初審         |            N |                N |   僅原審查醫師 |          N |       N |

### 初審修正規則

1. 只能由原審查醫師發起。
2. 僅限未結案案件。
3. 修正需建立新 revision，不得覆寫既有提交。
4. 非原審查醫師、院方管理者、系統管理員、稽核角色皆不可代為修正。

---

## 6.5 複審作業

| 功能              | SYSTEM_ADMIN | HOSPITAL_MANAGER | REVIEWER | REREVIEWER | AUDITOR |
| --------------- | -----------: | ---------------: | -------: | ---------: | ------: |
| 查看待複審案件         |            N |                R |        N |          Y |       R |
| 保存複審草稿          |            N |                N |        N |          Y |       N |
| 送出複審結果          |            N |                N |        N |          Y |       N |
| 上傳複審附件          |            N |                N |        N |          Y |       N |
| 決定最終分數          |            N |                N |        N |          Y |       N |
| 查看複審結果 metadata |            Y |                Y |        R |          Y |       R |
| 查看複審完整作答明細      |            N |             Y 本院 |   R 合法範圍 |    僅本人合法範圍 |  R 授權範圍 |
| 修正已提交複審         |            N |                N |        N |      僅複審醫師 |       N |

### 複審修正規則

1. 只能由複審醫師發起。
2. 僅限未結案案件。
3. 修正需建立新 revision，不得覆寫既有提交。
4. 最終分數仍由複審醫師決定。

---

## 6.6 結案與 revision 限制

| 功能           | SYSTEM_ADMIN | HOSPITAL_MANAGER | REVIEWER | REREVIEWER | AUDITOR |
| ------------ | -----------: | ---------------: | -------: | ---------: | ------: |
| 結案後 revision |            N |                N |        N |          N |       N |

### 規則

1. 案件一旦進入 `CLOSED` 狀態，不允許任何角色再發起 revision。
2. 結案後不可重新送出初審或複審提交。
3. 若未來需要例外修正，必須設計獨立的 administrative correction workflow；本版未納入。

---

## 6.7 稽核 / 報表 / 歷程

| 功能                         |    SYSTEM_ADMIN | HOSPITAL_MANAGER | REVIEWER | REREVIEWER | AUDITOR |
| -------------------------- | --------------: | ---------------: | -------: | ---------: | ------: |
| 查詢待審案件清單                   | Y metadata only |                Y |        Y |          Y |       R |
| 查詢已完成案件清單                  | Y metadata only |                Y |        R |          R |       R |
| 查詢複審案件清單                   | Y metadata only |                Y |        R |          Y |       R |
| 查詢抽選批次                     | Y metadata only |                Y |        R |          R |       R |
| 查詢審查完成率 / 逾期率 / 評分分布 / 複審率 | Y metadata only |                Y |        N |          N |       R |
| 匯出統計報表                     | Y metadata only |                Y |        N |          N |       R |
| 查閱操作軌跡與異動歷程                | Y metadata only |                Y |        N |          N |       Y |
| 修改規則                       |       Y 僅平台權限治理 |         Y 本院業務規則 |        N |          N |       N |
| 修改審查結果                     |               N |                N |        N |          N |       N |

### 補充

1. SYSTEM_ADMIN 可管理平台帳號 / 角色 / 權限，但不可修改審查結果內容。
2. HOSPITAL_MANAGER 可管理本院業務規則，但不可繞過正式 revision 規則直接改寫已提交結果。
3. AUDITOR 僅可查詢，不可修改。

---

## 7. Revision 權限總結

```yaml
revision_policy:
  initial_review_revision:
    allowed_actor: original_reviewer_only
    case_must_not_be_closed: true
    overwrite_existing_submission: false
    requires_new_revision_record: true

  rereview_revision:
    allowed_actor: assigned_rereviewer_only
    case_must_not_be_closed: true
    overwrite_existing_submission: false
    requires_new_revision_record: true

  revision_after_closed:
    allowed: false
```

---

## 8. 系統管理員特別限制

```yaml
system_admin_constraints:
  can_manage_platform_security: true
  can_view_operational_metadata: true
  can_view_full_review_answers: false
  can_view_full_comments: false
  can_view_attachment_content: false
  can_modify_review_results: false
  can_assign_reviewers: false
  can_trigger_business_sampling: false
```

---

## 9. 院方管理者特別權限

```yaml
hospital_manager_capabilities:
  can_manage_review_projects: true
  can_manage_rules_and_versions: true
  can_trigger_sampling: true
  can_assign_reviewers: true
  can_view_full_answers_in_hospital: true
  can_modify_submitted_review_results_directly: false
  can_bypass_revision_workflow: false
```

---

## 10. 稽核角色特別限制

```yaml
auditor_constraints:
  default_cross_hospital_access: false
  access_scope: authorized_hospitals_only
  can_modify_rules: false
  can_modify_review_results: false
```

---

## 11. 後續落地要求

本文件的規則應同步落地於：

* UI 按鈕與頁面可見性控制
* API authorization guard
* Query scope filter
* Revision service guard
* Audit / reporting data masking

若與其他設計文件衝突，優先檢查：

1. 需求書
2. decision log
3. permission matrix
4. UI / API 實作
