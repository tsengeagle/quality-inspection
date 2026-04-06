# 品質審查系統設計文件索引

本目錄收斂品質審查系統的需求、DDD 設計、契約設計、測試設計與 UI 設計文件。  
目標是提供一套可追溯、可驗證、可落地的設計基線，供以下用途使用：

- 業務需求對齊
- DDD 設計與實作
- API / Persistence 契約設計
- 測試設計與驗證
- UI / UX 設計
- AI agent / 開發人員 onboarding

---

## 1. 建議閱讀順序

若是第一次接觸本系統，建議依以下順序閱讀。

### A. 先理解需求與決策
1. `01-requirements/0_品質審查系統規格草案.md`
2. `01-requirements/decision-log.md`
3. `01-requirements/glossary.md`

### B. 再理解 DDD 核心模型
4. `02-ddd/context-map.md`
5. `02-ddd/domain-model.md`
6. `02-ddd/state-machine.md`
7. `02-ddd/domain-events.md`
8. `02-ddd/application-service.md`
9. `02-ddd/permission-matrix.md`

### C. 再看契約與持久化
10. `03-contracts/api-spec.md`
11. `03-contracts/persistence-model.md`

### D. 再看測試與驗證
12. `04-testing/specification-by-example.md`
13. `04-testing/test-scenarios.md`
14. `04-testing/example-to-test-level-matrix.md`
15. `04-testing/example-to-test-case-skeleton.md`
16. `04-testing/test-scenario-template-and-guidelines.md`

### E. 最後看 UI 設計
17. `05-ui/ui-page-inventory.md`
18. `05-ui/ui-information-architecture.md`
19. 各頁面規格：
   - `05-ui/ui-page-spec-ui006抽選執行頁.md`
   - `05-ui/ui-page-spec-ui007抽選批次列表頁.md`
   - `05-ui/ui-page-spec-ui009案件列表頁.md`
   - `05-ui/ui-page-spec-ui010案件詳情頁.md`
   - `05-ui/ui-page-spec-ui011案件分派頁.md`
   - `05-ui/ui-page-spec-ui012初審作業頁.md`
   - `05-ui/ui-page-spec-ui013複審作業頁.md`
   - `05-ui/ui-page-spec-ui014revision歷程頁.md`

---

## 2. 目錄分層說明

### `01-requirements/`
需求與決策層。

放置：
- 需求草案
- 已定案 decision log
- 共用詞彙 glossary

#### 本層文件
- `0_品質審查系統規格草案.md`
  - 系統需求與業務範圍來源文件

- `decision-log.md`
  - 已正式定稿的關鍵決策索引
  - 影響 domain、API、persistence、testing 與 UI

- `glossary.md`
  - 共用詞彙表
  - 用於統一需求、DDD、API、測試、UI 文件中的語意

- `aggregate-design-notes-v1.md`
  - aggregate 設計推導稿 / 歷史設計筆記
  - **不是正式 aggregate authority**
  - 正式 aggregate 設計以 `02-ddd/domain-model.md` 為準

---

### `02-ddd/`
DDD 戰術設計層。

放置：
- bounded context
- domain model
- state machine
- application service
- domain events
- permission matrix

#### 本層文件
- `context-map.md`
  - bounded context 劃分、上下游關係與整合邊界

- `domain-model.md`
  - 核心 aggregate / entity / value object / invariant 的正式 authority

- `state-machine.md`
  - 案件、提交與批次狀態機

- `domain-events.md`
  - domain event catalog
  - 僅處理業務事實事件，不混入 notification / reporting 類事件

- `application-service.md`
  - application layer use case 與 orchestration 邊界

- `permission-matrix.md`
  - 角色、scope、內容可見性與操作邊界

---

### `03-contracts/`
契約與持久化層。

放置：
- 對外 API 契約
- persistence model

#### 本層文件
- `api-spec.md`
  - command / query API 契約草稿

- `persistence-model.md`
  - aggregate 與持久化模型映射
  - version / snapshot / revision 策略

---

### `04-testing/`
測試與驗證設計層。

放置：
- Specification by Example
- test scenarios
- example 與 test case 映射輔助文件

#### 本層文件
- `specification-by-example.md`
  - 以具體業務案例固定規則

- `test-scenarios.md`
  - 將已定稿規則轉成可驗證情境

- `example-to-test-level-matrix.md`
  - example 與測試層級映射

- `example-to-test-case-skeleton.md`
  - example 到 test case 的骨架範本

- `test-scenario-template-and-guidelines.md`
  - test scenario 撰寫模板與指引

---

### `05-ui/`
UI 設計層。

放置：
- UI information architecture
- page inventory
- page-level specs

#### 本層文件
- `ui-page-inventory.md`
  - 頁面盤點與責任邊界

- `ui-information-architecture.md`
  - 導航、角色入口與頁面結構

- `ui-page-spec-*.md`
  - 個別頁面規格
  - 用於前端 route / page / interaction / API integration 設計

---

## 3. Authority 優先順序

若文件之間出現衝突，建議依以下順序判斷：

1. `01-requirements/0_品質審查系統規格草案.md`
2. `01-requirements/decision-log.md`
3. `01-requirements/glossary.md`
4. `02-ddd/context-map.md`
5. `02-ddd/domain-model.md`
6. `02-ddd/state-machine.md`
7. `02-ddd/permission-matrix.md`
8. `02-ddd/application-service.md`
9. `02-ddd/domain-events.md`
10. `03-contracts/api-spec.md`
11. `03-contracts/persistence-model.md`
12. `04-testing/specification-by-example.md`
13. `04-testing/test-scenarios.md`
14. `05-ui/ui-information-architecture.md`
15. `05-ui/ui-page-spec-*.md`

---

## 4. 文件之間的依賴關係

```text
需求草案
  -> decision-log
  -> glossary
  -> context-map
  -> domain-model
      -> state-machine
      -> domain-events
      -> application-service
      -> permission-matrix
      -> api-spec
      -> persistence-model
      -> specification-by-example
      -> test-scenarios
      -> ui-page-inventory
          -> ui-information-architecture
          -> ui-page-specs
```

---

## 5. 給不同角色的建議閱讀入口

### PM / BA

* `01-requirements/0_品質審查系統規格草案.md`
* `01-requirements/decision-log.md`
* `01-requirements/glossary.md`
* `05-ui/ui-page-inventory.md`
* `05-ui/ui-information-architecture.md`

### 架構師 / 後端開發

* `01-requirements/decision-log.md`
* `02-ddd/context-map.md`
* `02-ddd/domain-model.md`
* `02-ddd/state-machine.md`
* `02-ddd/application-service.md`
* `03-contracts/api-spec.md`
* `03-contracts/persistence-model.md`
* `04-testing/test-scenarios.md`

### 前端開發

* `01-requirements/decision-log.md`
* `02-ddd/permission-matrix.md`
* `03-contracts/api-spec.md`
* `04-testing/test-scenarios.md`
* `05-ui/ui-page-inventory.md`
* `05-ui/ui-information-architecture.md`
* 各 `ui-page-spec-*.md`

### AI Agent

* 先讀本檔
* 再讀：

  * `01-requirements/decision-log.md`
  * `01-requirements/glossary.md`
  * `02-ddd/domain-model.md`
  * `02-ddd/state-machine.md`
  * `02-ddd/permission-matrix.md`
  * `03-contracts/api-spec.md`
  * `03-contracts/persistence-model.md`
  * `04-testing/test-scenarios.md`
  * `05-ui/ui-page-spec-*.md`

---

## 6. 目前目錄結構評價

目前 `docs/` 分層已具備下列優點：

* 需求、DDD、contracts、testing、UI 已分層
* authority 來源大致清楚
* 已具備 implementation baseline 所需主文件
* 可直接支援 backend / frontend / testing 三方工作

目前建議的最小優化為：

1. 重新定位 `aggregate-design-notes-v1.md`

   * 建議視為設計推導稿，而非正式 authority
   * 正式 aggregate authority 仍以 `02-ddd/domain-model.md` 為準

2. 後續若 UI page spec 持續增加，可考慮再細分 page spec 子目錄

   * 目前尚非必要

---

## 7. 維護原則

1. 若需求或決策改變，優先更新：

   * `01-requirements/0_品質審查系統規格草案.md`
   * `01-requirements/decision-log.md`

2. 若 domain 規則改變，至少同步更新：

   * `02-ddd/domain-model.md`
   * `02-ddd/state-machine.md`
   * `02-ddd/application-service.md`
   * `03-contracts/api-spec.md`
   * `03-contracts/persistence-model.md`
   * `04-testing/test-scenarios.md`

3. 若權限或內容可見性改變，至少同步更新：

   * `02-ddd/permission-matrix.md`
   * `03-contracts/api-spec.md`
   * 相關 UI page spec
   * 相關 test scenarios

4. 若頁面流程或導航改變，至少同步更新：

   * `05-ui/ui-page-inventory.md`
   * `05-ui/ui-information-architecture.md`
   * 對應 `ui-page-spec-*.md`

---

## 8. 現階段定位

這套文件目前已具備：

* 需求 authority
* DDD tactical design baseline
* API / persistence 契約
* Specification by Example 與 test scenarios
* UI page-level 設計

可作為：

* monorepo implementation baseline
* backend / frontend 初始化基線
* AI agent 輔助開發 authority
