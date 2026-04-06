# AGENTS.md

## 1. 文件角色

本文件是本 repository 的 **solution-level operating guide**。  
它的用途是協助 AI agent、開發者與 reviewer 在此 repo 內：

- 快速理解 repository map
- 找到正確文件入口
- 判斷修改應落在哪裡
- 依既有文件分工持續工作
- 在缺乏 evidence 時正確標示 assumption / pending confirmation

本文件處理：

- repository map
- docs index
- inspection order
- representative files
- build / test / run / lint / gate inventory
- placement rules
- escalation rules

本文件不處理：

- repository-wide 全域硬規則
- project authority context
- stack/path-specific instructions
- domain 規則全文
- API 契約全文
- persistence schema 全文
- UI page spec 全文

這些內容應分別放在：

- `.ai/rules/repo-hard-rules.md`（canonical hard rules）
- `.ai/project-context.yaml`
- per-agent instructions（`CLAUDE.md` / `.github/copilot-instructions.md` / `.cursor/rules/*.mdc` / `.windsurfrules`）
- `docs/01-requirements`
- `docs/02-ddd`
- `docs/03-contracts`
- `docs/04-testing`
- `docs/05-ui`

---

## 2. Repository 定位

本 repository 目前定位為：

- 類型：solution-level repository
- 主題：品質審查系統
- 業務範圍：
  - 病歷抽選
  - 案件分派
  - 初審
  - 複審
  - 查詢與追蹤
  - 統計與報表
  - revision 歷程追溯

目前已知的核心設計語意包括：

- ReviewProject
- ScoreSheetDefinition
- SamplingBatch
- ReviewCase
- ReviewSubmission

目前已知核心 bounded contexts 包括：

- Review Project Configuration
- Sampling
- Assignment and Review
- Audit and Reporting

---

## 3. Repository Map

目前 repository 的 docs 結構以 `docs` 為 authority 中心。

```text
quality-inspection/          ← repo root
├─ .ai/
│  ├─ bootstrap/
│  └─ prompts/
├─ AGENTS.md
├─ docs/
│  ├─ 01-requirements/
│  ├─ 02-ddd/
│  ├─ 03-contracts/
│  ├─ 04-testing/
│  ├─ 05-ui/
│  ├─ 99-archive/
│  └─ README.md
├─ services/
│  └─ quality-inspection-service/
└─ frontends/
   └─ quality-inspection-frontend/
```

### 3.1 目錄責任

#### `docs/01-requirements`

需求 authority 區。
放系統需求、decision log、glossary。

#### `docs/02-ddd`

DDD 與核心業務設計區。
放 domain model、state machine、context map、domain events、application service、permission matrix。

#### `docs/03-contracts`

對外與落地契約區。
放 API spec 與 persistence model。

#### `docs/04-testing`

測試設計與測試映射區。
放 specification by example、test scenarios、example-to-test mapping、test template / guidelines。

#### `docs/05-ui`

UI 資訊架構與頁面規格區。
放 IA、page inventory、各頁 page spec。

#### `docs/99-archive`

歷史稿與被取代但需保留的設計筆記。
此區不是現行 authority，除非現行文件明確引用。

---

## 4. Docs Index

以下是目前 docs 的正式索引與用途。

### 4.1 `docs/README.md`

docs 入口說明。
後續若需要文件導覽、閱讀順序與維護說明，應優先更新此檔。

---

### 4.2 `docs/01-requirements`

#### `0_品質審查系統規格草案.md`

系統需求與業務範圍基線。

#### `decision-log.md`

已接受的關鍵決策 authority。
若要推翻既有決策，不應靜默改規格，應先更新 decision log。

#### `glossary.md`

共用術語 authority。
若同一名詞在不同文件中可能有不同意思，先回頭看 glossary。

---

### 4.3 `docs/02-ddd`

#### `context-map.md`

bounded context 邊界、上下游關係、責任分工。

#### `domain-model.md`

aggregate、entity、value object、domain invariant。

#### `state-machine.md`

案件與 submission 狀態機、terminal state、合法轉移。

#### `permission-matrix.md`

角色權限、scope、可見性、資料邊界。

#### `domain-events.md`

domain event catalog。
只處理 domain event，不處理 notification / analytics / integration payload。

#### `application-service.md`

application layer orchestration 邊界與 use cases。

---

### 4.4 `docs/03-contracts`

#### `api-spec.md`

HTTP API 契約、command / query API、error code、authorization 行為。

#### `persistence-model.md`

aggregate 與 persistence 的對應、version / snapshot / revision 持久化策略。

---

### 4.5 `docs/04-testing`

#### `specification-by-example.md`

用具體例子固定規則，作為 acceptance 與測試設計上游。

#### `test-scenarios.md`

正式測試情境清單。

#### `example-to-test-case-skeleton.md`

example 到 test case 的骨架對映。

#### `example-to-test-level-matrix.md`

example 對應測試層級的矩陣。

#### `test-scenario-template-and-guidelines.md`

測試情境模板與撰寫規範。

---

### 4.6 `docs/05-ui`

#### `ui-information-architecture.md`

UI 資訊架構。

#### `ui-page-inventory.md`

頁面盤點與頁面清單。

#### `ui-page-spec-ui006抽選執行頁.md`

抽選執行頁規格。

#### `ui-page-spec-ui007抽選批次列表頁.md`

抽選批次列表頁規格。

#### `ui-page-spec-ui009案件列表頁.md`

案件列表頁規格。

#### `ui-page-spec-ui010案件詳情頁.md`

案件詳情頁規格。

#### `ui-page-spec-ui011案件分派頁.md`

案件分派頁規格。

#### `ui-page-spec-ui012初審作業頁.md`

初審作業頁規格。

#### `ui-page-spec-ui013複審作業頁.md`

複審作業頁規格。

#### `ui-page-spec-ui014revision歷程頁.md`

revision 歷程頁規格。

---

### 4.7 `docs/99-archive`

#### `aggregate-design-notes-v1.md`

歷史 aggregate 設計筆記。
只作歷史參考，不作現行 authority；除非現行文件不足且需追溯演進脈絡。

---

## 5. Inspection Order

### 5.1 新需求、規則釐清、架構討論

1. `docs/01-requirements/0_品質審查系統規格草案.md`
2. `docs/01-requirements/decision-log.md`
3. `docs/01-requirements/glossary.md`
4. `docs/02-ddd/context-map.md`
5. `docs/02-ddd/domain-model.md`
6. `docs/02-ddd/state-machine.md`

### 5.2 domain / application / permission 相關修改

1. `docs/01-requirements/decision-log.md`
2. `docs/01-requirements/glossary.md`
3. `docs/02-ddd/domain-model.md`
4. `docs/02-ddd/state-machine.md`
5. `docs/02-ddd/permission-matrix.md`
6. `docs/02-ddd/application-service.md`
7. `docs/02-ddd/domain-events.md`

### 5.3 API / persistence 相關修改

1. `docs/01-requirements/decision-log.md`
2. `docs/02-ddd/domain-model.md`
3. `docs/02-ddd/state-machine.md`
4. `docs/02-ddd/application-service.md`
5. `docs/02-ddd/permission-matrix.md`
6. `docs/03-contracts/api-spec.md`
7. `docs/03-contracts/persistence-model.md`

### 5.4 testing 相關修改

1. `docs/01-requirements/decision-log.md`
2. `docs/04-testing/specification-by-example.md`
3. `docs/04-testing/test-scenarios.md`
4. `docs/04-testing/example-to-test-case-skeleton.md`
5. `docs/04-testing/example-to-test-level-matrix.md`
6. `docs/04-testing/test-scenario-template-and-guidelines.md`

### 5.5 UI 相關修改

1. `docs/01-requirements/decision-log.md`
2. `docs/02-ddd/permission-matrix.md`
3. `docs/03-contracts/api-spec.md`
4. `docs/05-ui/ui-information-architecture.md`
5. `docs/05-ui/ui-page-inventory.md`
6. 對應頁面的 `ui-page-spec-*.md`

---

## 6. Representative Files

若需要快速理解本系統，優先看以下文件：

### 6.1 業務與決策

* `docs/01-requirements/0_品質審查系統規格草案.md`
* `docs/01-requirements/decision-log.md`
* `docs/01-requirements/glossary.md`

### 6.2 核心 DDD

* `docs/02-ddd/domain-model.md`
* `docs/02-ddd/state-machine.md`
* `docs/02-ddd/context-map.md`
* `docs/02-ddd/application-service.md`
* `docs/02-ddd/permission-matrix.md`
* `docs/02-ddd/domain-events.md`

### 6.3 對外契約

* `docs/03-contracts/api-spec.md`
* `docs/03-contracts/persistence-model.md`

### 6.4 驗證

* `docs/04-testing/specification-by-example.md`
* `docs/04-testing/test-scenarios.md`

### 6.5 UI

* `docs/05-ui/ui-page-inventory.md`
* `docs/05-ui/ui-information-architecture.md`
* 各頁 page spec

---

## 7. Placement Rules

### 7.1 規則放置原則

#### 要改「需求定義」

更新：

* `docs/01-requirements/0_品質審查系統規格草案.md`

#### 要改「已接受決策」

更新：

* `docs/01-requirements/decision-log.md`

#### 要改「詞彙語意」

更新：

* `docs/01-requirements/glossary.md`

#### 要改「aggregate / invariant / domain 邊界」

更新：

* `docs/02-ddd/domain-model.md`

#### 要改「狀態流轉 / terminal state / revision 與 lifecycle 關係」

更新：

* `docs/02-ddd/state-machine.md`

#### 要改「角色可做什麼 / 可看什麼 / scope 邊界」

更新：

* `docs/02-ddd/permission-matrix.md`

#### 要改「application orchestration / use case」

更新：

* `docs/02-ddd/application-service.md`

#### 要改「domain events」

更新：

* `docs/02-ddd/domain-events.md`

#### 要改「HTTP contract」

更新：

* `docs/03-contracts/api-spec.md`

#### 要改「DB / persistence 映射」

更新：

* `docs/03-contracts/persistence-model.md`

#### 要改「example / acceptance 上游規則固定」

更新：

* `docs/04-testing/specification-by-example.md`

#### 要改「正式測試情境」

更新：

* `docs/04-testing/test-scenarios.md`

#### 要改「UI 頁面行為或畫面規格」

更新：

* `docs/05-ui/` 對應頁面文件

---

## 8. Working Rules Inside This Repo

### 8.1 先讀 authority，再改文件

不要跳過 requirements / decision / domain authority，直接改 contract 或 UI。

### 8.2 先改最小正確落點

不要把 permission 規則寫進 domain invariant。
不要把 state machine 規則寫進 API spec 當唯一真相。
不要把 UI 行為定義偷塞進 requirements 造成重複 authority。

### 8.3 specification-by-example 與 test-scenarios 分工不可混淆

* `specification-by-example.md`：固定共同理解與例子
* `test-scenarios.md`：轉成可驗證情境與覆蓋

### 8.4 archive 不作現行 authority

`docs/99-archive` 只保留歷史脈絡。
若 archive 與現行文件衝突，現行文件優先。

### 8.5 無 evidence 不得宣稱完成

若 repo 內尚未出現實際 source code、build script、test runner、CI pipeline、lint config，就不得宣稱已確認相關 gate。

---

## 9. Build / Test / Run / Lint / Gate Inventory

### 9.1 已確認狀態

目前依你提供的實際結構，可確認的是 **文件設計樹已存在且分層清楚**。

### 9.2 尚未確認

目前沒有足夠 repo evidence 證明以下入口已存在：

```yaml
pending_confirmation:
  - backend_source_root
  - frontend_source_root
  - build_command
  - test_command
  - run_command
  - lint_command
  - formatter_command
  - static_analysis_gate
  - ci_pipeline_entry
  - packaging_entry
```

### 9.3 不得自行假設

以下目前不得寫成已確認事實：

```yaml
assumptions:
  - backend_stack_unknown: true
  - frontend_stack_unknown: true
  - monorepo_runtime_layout_unknown: true
  - implementation_skeleton_not_verified: true
```

---

## 10. Escalation Rules

遇到以下情況，先停在文件層處理，不要直接往下實作。

### 10.1 核心規則要變

例如：

* quality threshold 規則改變
* revision policy 改變
* terminal state 改變
* duplicate sampling scope 改變

先更新：

* `docs/01-requirements/decision-log.md`
* 必要時同步：

  * `docs/04-testing/specification-by-example.md`
  * `docs/04-testing/test-scenarios.md`

### 10.2 文件 authority 衝突

例如：

* `domain-model.md` 與 `state-machine.md` 衝突
* `permission-matrix.md` 與 `api-spec.md` 衝突
* `decision-log.md` 與其他文件衝突

處理順序：

1. `decision-log.md`
2. `0_品質審查系統規格草案.md`
3. `glossary.md`
4. 對應專業文件

### 10.3 想新增新文件

先問：

* 既有資料夾是否已有正確落點？
* 是不是其實應該 patch 既有文件？
* 是否會造成 authority 分裂？

若既有文件足以承載，優先 patch，不新增平行文件。

### 10.4 UI 規格與 domain / API 脫鉤

若 UI page spec 開始發明不存在的 domain state、權限、欄位或 command，先回頭對齊：

* `domain-model.md`
* `state-machine.md`
* `permission-matrix.md`
* `api-spec.md`

### 10.5 testing 與 example 脫鉤

若 test-scenarios 開始出現沒有對應 example / decision 的高風險規則，先補：

* `decision-log.md`
* 或 `specification-by-example.md`

---

## 11. Non-goals and Current Boundaries

本文件目前 **不宣稱** 以下事項已完成：

* backend implementation skeleton 已存在
* frontend implementation skeleton 已存在
* Spring Boot / Angular / 其他技術棧已確認
* build / test / run / lint / CI gate 已確認

以下事項已確認存在：

* `.ai/rules/repo-hard-rules.md` — canonical hard rules source 已存在
* `.ai/bootstrap/*` — solution / backend / frontend bootstrap 均已存在
* `.ai/prompts/*` — solution / backend / frontend init prompt 均已存在
* `.ai/project-context.yaml` — solution-level authoritative project context 已存在
* `AGENTS.md` — repo operating guide 已存在
* `CLAUDE.md` — Claude Code repo-level instructions 已存在

其餘待後續依實際 repo evidence 再確認。
