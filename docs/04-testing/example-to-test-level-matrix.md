# Example → Test Level Matrix v1

## 1. 文件目的

本文件用於將 `specification-by-example.md` 中的每個 example，對應到最合適的測試層級，作為以下工作的依據：

- 測試分層規劃
- test-first 任務拆解
- regression suite 組成
- 測試責任分工
- 避免重複測同一規則或用錯測試層級

本文件不取代：
- `specification-by-example.md`
- `test-scenarios.md`
- 單元測試 / integration test 程式碼

本文件的角色是回答：

> 這個規則，最應該在哪一層測？  
> 哪一層是 primary owner？  
> 哪些層只需要補充驗證？

---

## 2. 測試層級定義

### L1. Domain Unit Test
聚焦：
- aggregate invariant
- state transition
- revision rule
- terminal state
- version immutability
- duplicate-in-batch

特性：
- 不依賴 HTTP
- 不依賴真實資料庫或僅用極小 stub
- 快速、精準、規則導向

---

### L2. Application Service Test
聚焦：
- use case orchestration
- actor ownership / command legality
- aggregate 協作
- transaction 邊界內行為
- publish / retry / revise command flow

特性：
- 可使用 repository stub / fake
- 驗證 orchestration 而不是 controller

---

### L3. API Integration Test
聚焦：
- HTTP contract
- auth / scope / masking
- error code
- request / response shape
- controller → application service wiring

特性：
- 驗證外部行為
- 對前後端契約最重要

---

### L4. Persistence Test
聚焦：
- unique key
- FK
- snapshot persistence
- append-only revision
- query mapping / index behavior

特性：
- 驗證 schema / repository / migration
- 補足 DB 約束層面的保護

---

### L5. Acceptance / End-to-End Test
聚焦：
- 完整業務流程串接
- 跨多個 use case 的主要路徑
- 關鍵 business journey

特性：
- 數量應少而精
- 作為高價值回歸測試

---

## 3. 使用原則

1. 每個 example 至少要有一個 **Primary Test Level**。
2. 若同一規則跨多層都重要，應標示：
   - primary
   - secondary
3. 不應用高成本測試層取代低成本且更精準的測試層。
4. permission / masking 規則通常不應由 domain unit test 擔任 primary。
5. state / invariant 規則通常不應只靠 API integration test 覆蓋。

---

## 4. Matrix 欄位定義

| 欄位 | 說明 |
|---|---|
| Example ID | 來自 `specification-by-example.md` |
| Rule Summary | 規則摘要 |
| Primary Level | 主要測試層級 |
| Secondary Levels | 輔助測試層級 |
| Why Primary | 為何該層最適合當主責 |
| Scenario Ref | 對應 `test-scenarios.md` |
| Regression Tier | 建議回歸層級 |

### Regression Tier
- `critical`
- `high`
- `medium`
- `low`

---

## 5. Core Matrix

| Example ID | Rule Summary | Primary Level | Secondary Levels | Why Primary | Scenario Ref | Regression Tier |
|---|---|---|---|---|---|---|
| EX-001 | 初審低於或等於品質線直接結案 | L1 Domain Unit Test | L2, L3, L5 | 核心是 `ReviewCase` 的狀態與品質線規則 | TS-IR-006 | critical |
| EX-002 | 初審高於品質線進入複審 | L1 Domain Unit Test | L2, L3, L5 | 核心是 `completeInitialReview` 後的 state transition | TS-IR-007 | critical |
| EX-003 | 複審完成後 final score 以複審為準 | L1 Domain Unit Test | L2, L3, L5 | 核心是 `completeRereview` 與 final score 決定權 | TS-RR-003 | critical |
| EX-004 | 原審查醫師可修正已提交初審 | L2 Application Service Test | L1, L3, L4 | 同時涉及 actor ownership、revision orchestration、append-only | TS-RV-001, TS-RV-007, TS-RV-008 | critical |
| EX-005 | 非原審查醫師不可修正初審 | L2 Application Service Test | L3 | 核心在 actor ownership 與 command legality | TS-RV-002 | high |
| EX-006 | 複審醫師可修正已提交複審 | L2 Application Service Test | L1, L3, L4 | 同時涉及責任人、revision 與 stage-specific flow | TS-RV-003 | critical |
| EX-007 | 結案後不可 revision | L1 Domain Unit Test | L2, L3 | 核心是 terminal state invariant | TS-RV-005 | critical |
| EX-008 | 已取消案件不可 revision | L1 Domain Unit Test | L2, L3 | 核心是 `CANCELLED` terminal state invariant | TS-RV-006 | high |
| EX-009 | 正式抽選建立 batch / snapshot / case | L2 Application Service Test | L3, L4, L5 | 涉及跨 aggregate orchestration 與 snapshot 建立 | TS-SM-002 | critical |
| EX-010 | 同批次不得重複抽中同一 sourceCaseKey | L4 Persistence Test | L1, L2 | DB unique key 與 domain invariant 都重要，但 DB 約束是最硬保護 | TS-SM-004 | critical |
| EX-011 | 不同批次可再次抽中相同 sourceCaseKey | L2 Application Service Test | L4 | 要驗證未誤做 cross-batch exclusion | TS-SM-005 | medium |
| EX-012 | failed batch retry 建立新 batch，不覆寫舊 batch | L2 Application Service Test | L4, L5 | 核心是 retry orchestration 與新舊 batch 關聯 | TS-SM-006 | high |
| EX-013 | manager 可看本院完整作答明細 | L3 API Integration Test | L5 | 核心是 response view 與 scope，不是 domain 規則 | TS-PM-001 | high |
| EX-014 | system admin 只能看到 metadata | L3 API Integration Test | L5 | 核心是 response masking 與 visibility policy | TS-PM-003 | high |
| EX-015 | auditor 只能查授權院區 | L3 API Integration Test | L2, L5 | 核心是 auth scope filter 與 detail/query behavior | TS-PM-004 | high |
| EX-016 | case 可追溯 project / batch / version / snapshot | L4 Persistence Test | L3, L5 | 核心是關聯與追溯資料的正確持久化 | TS-TR-001 | high |
| EX-017 | submission 可追溯 score sheet snapshot | L4 Persistence Test | L3 | 核心是 snapshot ref / checksum / version 的正確保存 | TS-TR-002 | high |
| EX-018 | 已發布版本不可直接覆寫 | L1 Domain Unit Test | L2, L3 | 核心是 published version immutability | TS-PV-004 | high |
| EX-019 | 專案未啟用不得抽選 | L1 Domain Unit Test | L2, L3 | 核心是 project active guard | TS-SM-003 | high |
| EX-020 | 未分派案件不可開始初審 | L1 Domain Unit Test | L2, L3 | 核心是 case state guard | TS-NG-001 | critical |
| EX-021 | 未進入複審不可開始複審 | L1 Domain Unit Test | L2, L3 | 核心是 rereview state guard | TS-NG-002 | critical |
| EX-022 | 不可使用未發布版本執行正式抽選 | L2 Application Service Test | L1, L3 | 涉及 published version selection 與 execute sampling orchestration | TS-NG-003 | high |
| EX-023 | revise API 必須與 submission stage 一致 | L3 API Integration Test | L2 | 核心是 API contract 與 command routing 正確性 | TS-NG-004 | high |
| EX-024 | 不可用 generic update 覆寫已提交 submission | L3 API Integration Test | L1, L2 | 核心是外部契約不提供錯誤入口，同時對齊 append-only model | TS-NG-005 | high |
| EX-025 | dashboard / report 也必須遵守可見性與 scope | L3 API Integration Test | L5 | 核心是 aggregated query 的 visibility / scope 行為 | TS-RP-001, TS-RP-002, TS-RP-003 | medium |

---

## 6. 建議的最小測試責任分工

### 6.1 L1 Domain Unit Test 應主責的規則
```yaml id="gb76wi"
domain_primary_examples:
  - EX-001
  - EX-002
  - EX-003
  - EX-007
  - EX-008
  - EX-018
  - EX-019
  - EX-020
  - EX-021
```

### 6.2 L2 Application Service Test 應主責的規則

```yaml id="bti04z"
application_primary_examples:
  - EX-004
  - EX-005
  - EX-006
  - EX-009
  - EX-011
  - EX-012
  - EX-022
```

### 6.3 L3 API Integration Test 應主責的規則

```yaml id="zqjlwm"
api_primary_examples:
  - EX-013
  - EX-014
  - EX-015
  - EX-023
  - EX-024
  - EX-025
```

### 6.4 L4 Persistence Test 應主責的規則

```yaml id="q1yfwo"
persistence_primary_examples:
  - EX-010
  - EX-016
  - EX-017
```

### 6.5 L5 Acceptance / End-to-End Test 應主責的主旅程

```yaml id="0nmh2p"
acceptance_primary_flows:
  - 初審直接結案
  - 初審進複審後結案
  - failed batch retry
  - manager / auditor / sysadmin 的視角差異
```

---

## 7. 建議的回歸測試分層

### Tier 1: Critical

每次 CI 必跑，數量要少但高價值。

```yaml id="xc4v6t"
critical_examples:
  - EX-001
  - EX-002
  - EX-003
  - EX-004
  - EX-006
  - EX-007
  - EX-009
  - EX-010
  - EX-020
  - EX-021
```

### Tier 2: High

重要規則，合併前或 nightly 應跑。

```yaml id="mru0yu"
high_examples:
  - EX-005
  - EX-008
  - EX-012
  - EX-013
  - EX-014
  - EX-015
  - EX-016
  - EX-017
  - EX-018
  - EX-019
  - EX-022
  - EX-023
  - EX-024
```

### Tier 3: Medium

較偏 query / report / cross-view behavior，可在 nightly 或 release validation 跑。

```yaml id="gj9j8j"
medium_examples:
  - EX-011
  - EX-025
```

---

## 8. 測試層級選擇原則補充

### 8.1 什麼情況優先用 Domain Unit Test

當規則主要在回答：

* 狀態能不能轉
* invariant 能不能破
* terminal state 是否生效
* final score / threshold / rereview 是否正確

### 8.2 什麼情況優先用 Application Service Test

當規則主要在回答：

* 誰發 command
* 載入哪些 aggregate
* 怎麼建立 revision / retry / snapshot
* orchestration 是否正確

### 8.3 什麼情況優先用 API Integration Test

當規則主要在回答：

* HTTP path / method / error code
* response shape
* metadata vs full detail masking
* scope filter / auth behavior

### 8.4 什麼情況優先用 Persistence Test

當規則主要在回答：

* unique key 是否足夠保護規則
* snapshot / version / relation 是否真的寫進 DB
* append-only revision 是否真的成立

### 8.5 什麼情況要留給 Acceptance / E2E

當規則主要在回答：

* 從抽選到結案的完整旅程是否串得起來
* 跨多個 use case 的關鍵 business journey 是否穩定

---

## 9. 目前的測試覆蓋觀察

### 9.1 覆蓋較完整的區域

* 主流程 state transition
* revision actor 與 terminal state
* sampling duplicate rule
* basic permission / scope
* traceability 主幹

### 9.2 後續仍建議強化的區域

* dashboard / report 類 acceptance flow
* event-level assertions for sampling
* response masking 欄位級 assertion
* published version immutability 的 cross-layer測試
* snapshot content / ref 的 persistence + query 聯合驗證

---

## 10. 後續使用方式

本文件建議用來驅動：

1. 測試任務拆分
2. CI / nightly / release regression 分層
3. 單元測試與 integration test 責任分工
4. 規則新增時的測試缺口分析
