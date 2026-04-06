# Test Scenario Template and Guidelines v1

## 1. 文件目的

本文件用於定義品質審查系統的測試情境撰寫格式、分層原則、驗證範圍與交付規範，作為以下工作的共同基準：

- 新增 test scenario
- 將 example 轉成 acceptance scenario
- 交付 coding agent 實作測試
- 做測試覆蓋盤點與 gap analysis
- 做 regression suite 分層

本文件不取代：
- `specification-by-example.md`
- `test-scenarios.md`
- `example-to-test-level-matrix.md`

本文件的角色是：

> 規定「測試情境應該怎麼寫、要驗到哪裡、不要混什麼」。

---

## 2. 適用範圍

本文件適用於以下類型的測試情境設計：

1. Domain rule scenario
2. Application service scenario
3. API acceptance scenario
4. Persistence verification scenario
5. Permission / visibility scenario
6. Reporting / query scenario
7. Negative / conflict scenario
8. Regression candidate scenario

---

## 3. 核心原則

### 3.1 規則先於測試
測試情境必須先能對應到：
- 需求規則
- decision log
- specification by example

若沒有上游 authority，不應先寫測試。

---

### 3.2 一個 scenario 只固定一條主規則
一個 scenario 可以涵蓋多個結果，但只能有一條主規則。  
避免一個 scenario 同時混：
- state transition
- revision
- visibility
- reporting

導致失焦。

---

### 3.3 先寫業務語意，再寫技術驗證
scenario 應先說清楚：
- 業務前提
- 觸發動作
- 預期業務結果

再補：
- API 結果
- persistence 結果
- event 結果

---

### 3.4 明確分離 primary assertion 與 secondary assertion
每個 scenario 都應標出：

- `Primary Assertion`
- `Secondary Assertions`

避免 coding agent 把所有 assertion 視為同等重要，導致測試過胖。

---

### 3.5 Negative scenario 不得只寫「失敗」
拒絕型 scenario 必須明確說明：
- 失敗原因
- 哪一層拒絕
- 預期錯誤碼或結果
- 哪些資料不得被改變

---

### 3.6 權限情境必須同時驗證 scope 與內容視圖
例如：
- 查不查得到
- 查得到時看得到多少
- 是 full detail 還是 metadata only

不能只驗證 `403`。

---

### 3.7 revision 情境必須同時驗證 append-only
凡是 revision 類情境，至少要驗：

- 新 submission 建立
- revisionNo 遞增
- 舊版變 `SUPERSEDED`
- 舊資料未被覆寫
- case 主狀態是否維持不變

---

### 3.8 terminal state 情境必須同時驗證「不變」
凡是 `CLOSED` / `CANCELLED` 類 scenario，至少要驗：

- 請求被拒絕
- 不建立新資料
- 不改變舊狀態
- 不產生不該有的 event

---

## 4. Scenario 標準格式

每個 scenario 一律使用以下欄位。

## 4.1 必填欄位

```yaml
scenario_template:
  scenario_id: TS-XXX
  title: 測試情境標題
  objective: 此情境要驗證什麼規則
  primary_rule: 對應的主要規則
  authority:
    requirement: []
    decision_log: []
    examples: []
  level:
    primary: L1|L2|L3|L4|L5
    secondary: []
  actors: []
  preconditions: []
  input_data: []
  action: []
  expected_domain_result: []
  expected_api_result: []
  expected_persistence_result: []
  expected_event_or_audit_result: []
  non_goals: []
```

---

## 4.2 欄位說明

### `scenario_id`

唯一編號，例如：

* `TS-IR-006`
* `TS-RV-005`

---

### `title`

用一句話描述情境，不要混兩個規則。

好例子：

* 結案後不可 revision
* 同批次不得重複抽中同一來源案件

壞例子：

* revision 與權限與事件與狀態驗證

---

### `objective`

描述這個 scenario 的驗證目標，應聚焦在規則本身，而不是測試方法。

---

### `primary_rule`

明確寫出此 scenario 固定的主規則。

---

### `authority`

必須標出上游 authority。

格式建議：

```yaml id="yyfogs"
authority:
  requirement:
    - "需求書 章節 / 規則名"
  decision_log:
    - "D-001"
  examples:
    - "EX-007"
```

若沒有 authority，先補上游文件，再補 scenario。

---

### `level`

標出：

* primary 測試層級
* secondary 補充層級

例如：

```yaml id="w831vg"
level:
  primary: L1
  secondary:
    - L2
    - L3
```

---

### `actors`

列出參與者，例如：

* `HOSPITAL_MANAGER`
* `REVIEWER`
* `REREVIEWER`
* `SYSTEM_ADMIN`
* `AUDITOR`

---

### `preconditions`

只寫本情境成立所需的必要前提。

例如：

* case status = `INITIAL_REVIEW_IN_PROGRESS`
* actor = assigned reviewer
* project status = active

不要把與規則無關的背景全部塞進來。

---

### `input_data`

列出這個 scenario 需要關注的資料值。

例如：

* qualityThreshold = 80
* totalScore = 88.5
* submission stage = `INITIAL`

---

### `action`

描述觸發動作：

* command
* use case
* API call

先寫業務動作，再補 API。

---

### `expected_domain_result`

只寫 domain truth，例如：

* case status 變成 `PENDING_REREVIEW`
* revision 不成立
* finalScore = 92

---

### `expected_api_result`

只寫對外行為，例如：

* 200 / 403 / 409 / 422
* response shape
* metadata view / full detail view

---

### `expected_persistence_result`

只寫資料層可驗證結果，例如：

* 新增一筆 submission
* unique constraint 生效
* snapshot 被持久化
* 舊版 status = `SUPERSEDED`

---

### `expected_event_or_audit_result`

只寫事件 / audit 期待，例如：

* `ReviewCaseClosed` 產生
* 不應有 `ReviewSubmissionRevised`
* audit trail 記錄某事件

---

### `non_goals`

明確說此 scenario 不涵蓋什麼，避免後續被硬塞 assertion。

例如：

* 不驗 detail visibility
* 不驗 dashboard
* 不驗 attachment storage

---

## 5. Scenario 撰寫規範

---

## 5.1 成功型情境寫法

成功型 scenario 應至少包含：

1. 誰執行
2. 在什麼狀態
3. 做了什麼
4. 預期狀態變成什麼
5. 預期哪些資料新增 / 更新
6. 預期哪些 event 成立

---

## 5.2 拒絕型情境寫法

拒絕型 scenario 應至少包含：

1. 為什麼此操作不合法
2. 由哪一層拒絕
3. 預期錯誤碼
4. 預期不應發生的 side effect

建議使用這種句型：

* 系統拒絕
* 不建立新資料
* 不變更舊狀態
* 不產生 domain event

---

## 5.3 邊界值情境寫法

凡是有比較運算的規則，都應至少補三種情況：

* 小於邊界
* 等於邊界
* 大於邊界

例如品質線：

* `75 < 80`
* `80 == 80`
* `88.5 > 80`

第一版 scenario 若尚未補全，後續優先補邊界值。

---

## 5.4 權限情境寫法

權限情境至少拆成兩類：

### A. 操作權限

例：

* 非原審查醫師不可 revise

### B. 內容可見性

例：

* system admin 只能看 metadata

這兩類不要混成同一 scenario，除非主規則本來就是「操作 + 視圖同時成立」。

---

## 5.5 Persistence 情境寫法

Persistence 類 scenario 不要只寫：

* DB 有資料

而要明確寫：

* 哪張表
* 哪個 key / relation / checksum / snapshot
* 哪個 unique constraint
* 哪個欄位不應被改寫

---

## 6. Assertion 分層規範

每個 scenario 至少應拆成以下幾層 assertion。

## 6.1 Primary Assertion

此 scenario 最重要、最不可缺的 assertion。

例如：

* case status = `CLOSED`
* request 被拒絕
* 新 revision 被建立

---

## 6.2 Secondary Assertions

重要但不是主軸的 assertion。

例如：

* event 被發布
* audit trail 被記錄
* response 含某些 metadata

---

## 6.3 Forbidden Side Effects

這一欄非常重要，尤其是 negative / revision / terminal state scenario。

例如：

* 不建立新 submission
* 不改寫舊 submission
* 不變更 case status
* 不產生 `ReviewSubmissionRevised`

---

## 7. 各測試層級的 scenario 設計重點

---

## 7.1 L1 Domain Unit Test

Scenario 應聚焦：

* state transition
* terminal state
* threshold rule
* version immutability
* revision guard

不應混入：

* HTTP status
* auth token
* response masking

---

## 7.2 L2 Application Service Test

Scenario 應聚焦：

* actor ownership
* orchestration flow
* aggregate coordination
* retry / revise / publish 命令流程

不應混入：

* controller request binding 細節
* response schema 欄位遮罩

---

## 7.3 L3 API Integration Test

Scenario 應聚焦：

* path / method
* auth / scope
* error code
* response shape
* full detail vs metadata only

不應把：

* 純 domain 規則細節
  全部壓在這一層當 primary。

---

## 7.4 L4 Persistence Test

Scenario 應聚焦：

* unique key
* FK
* append-only persistence
* snapshot persistence
* 查詢 relation 是否存在

---

## 7.5 L5 Acceptance / End-to-End Test

Scenario 應聚焦：

* 主要 business journey
* 高價值主流程
* 少量但關鍵的回歸路徑

不要把所有 edge case 都堆到 acceptance 層。

---

## 8. Counter-example 規範

若某規則存在清楚的反例，建議同時寫：

* happy path scenario
* counter-example scenario

例如：

* 原審查醫師可修正初審
* 非原審查醫師不可修正初審

這樣最能幫助 coding agent 與 reviewer 理解規則邊界。

---

## 9. 交付 coding agent 的 handoff 規範

當要把 scenario 交給 coding agent 實作時，每個 scenario 應至少包含：

```yaml id="6huq2r"
handoff_minimum:
  - scenario_id
  - primary_rule
  - primary_test_level
  - primary_assertion
  - forbidden_side_effects
  - suggested_test_class
  - suggested_test_method
  - fixture_requirements
```

建議附帶：

* 上游 authority
* 對應 example id
* 對應 API / use case / aggregate

---

## 10. 建議的 scenario 寫作順序

新增一條規則時，建議依序做：

1. 先補 `decision-log.md`
2. 再補 `specification-by-example.md`
3. 再補 `test-scenarios.md`
4. 再補 `example-to-test-level-matrix.md`
5. 必要時再補 `example-to-test-case-skeleton.md`

不要反過來先寫 test case skeleton。

---

## 11. Scenario 品質檢查清單

每新增一條 scenario，至少檢查：

```yaml id="f0h078"
scenario_quality_checklist:
  - 是否只有一條主規則
  - 是否有清楚 authority
  - 是否有 actor
  - 是否有必要前置條件
  - 是否有 primary assertion
  - 是否有 forbidden side effects
  - 是否明確區分 domain / api / persistence / event 結果
  - 是否標出 primary test level
  - 是否沒有把 out-of-scope 內容混進來
```

---

## 12. 附錄：Scenario 範例骨架

```yaml id="n42sg8"
scenario_example:
  scenario_id: TS-RV-005
  title: 結案後不可 revision
  objective: 驗證 CLOSED terminal state 不允許任何 revision
  primary_rule: 案件進入 CLOSED 後，不可建立新的 submission revision
  authority:
    requirement:
      - 已送出結果不得直接覆寫，需保留修正紀錄
    decision_log:
      - D-001
    examples:
      - EX-007
  level:
    primary: L1
    secondary:
      - L2
      - L3
  actors:
    - REVIEWER
    - REREVIEWER
  preconditions:
    - case status = CLOSED
    - existing submitted submission
  input_data:
    - stage = INITIAL or REREVIEW
  action:
    - actor attempts revise command
  expected_domain_result:
    - revision rejected
    - case status remains CLOSED
  expected_api_result:
    - 409 CASE_ALREADY_CLOSED or 422 REVISION_NOT_ALLOWED
  expected_persistence_result:
    - no new submission row
    - old submission unchanged
  expected_event_or_audit_result:
    - no ReviewSubmissionRevised event
  non_goals:
    - does not cover correction workflow
```
