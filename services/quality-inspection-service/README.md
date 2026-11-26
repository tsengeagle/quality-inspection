# quality-inspection-service

## 1. 文件角色

本文件是 `services/quality-inspection-service` 的 **backend implementation area guide**。

它的用途是：

- 說明 backend implementation area 在整個 solution 中的角色
- 對齊 backend 實作與既有 docs authority
- 定義 backend 內部應承接哪些責任，哪些不應承接
- 提供後續程式碼結構、模組邊界、測試與實作的落點指引

本文件不取代：

- 需求書
- decision log
- domain model
- state machine
- permission matrix
- API spec
- persistence model
- test scenarios
- UI page spec

若本文件與上述 authority 衝突，應優先回頭檢查 `docs` 下對應文件。

---

## 2. 模組定位

`quality-inspection-service` 是品質審查系統的 **backend implementation area**。

它預期承載以下責任：

- domain model 的後端落地
- application service orchestration
- HTTP API 實作
- persistence model 落地
- authorization / visibility enforcement
- audit / traceability 落地
- 與外部 supporting context 的整合
- backend 測試與驗證

它不應承載以下責任：

- 需求 authority 本身
- UI 畫面規格 authority
- archive 歷史稿 authority
- 以 controller / DTO 重新發明 domain rule
- 以 DB schema 取代 domain truth
- 以 UI page spec 反向決定 domain state

---

## 3. 對應的 docs authority

backend 開發前，必須先讀取並遵守下列文件。

### 3.1 需求與決策
- `docs/01-requirements/0_品質審查系統規格草案.md`
- `docs/01-requirements/decision-log.md`
- `docs/01-requirements/glossary.md`

### 3.2 DDD
- `docs/02-ddd/context-map.md`
- `docs/02-ddd/domain-model.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/02-ddd/domain-events.md`
- `docs/02-ddd/application-service.md`

### 3.3 Contracts
- `docs/03-contracts/api-spec.md`
- `docs/03-contracts/persistence-model.md`

### 3.4 Testing
- `docs/04-testing/specification-by-example.md`
- `docs/04-testing/test-scenarios.md`
- `docs/04-testing/test-scenario-template-and-guidelines.md`

### 3.5 backend 不直接作為 authority 的文件
以下文件可作參考，但不是 backend domain / contract 的第一 authority：

- `docs/05-ui/*`
- `docs/99-archive/*`

---

## 4. Backend 應承接的核心能力

依目前 docs authority，backend 應優先承接以下核心能力：

1. 審查專案與評分表版本管理
2. 抽選執行與抽選批次管理
3. ReviewCase 生命週期管理
4. 初審與複審 submission 管理
5. revision append-only 模型
6. 權限、scope 與可見性控制
7. API contract 落地
8. persistence model 落地
9. traceability / audit / event 落地
10. 測試情境對應與驗證

---

## 5. 預期 bounded contexts 對應

backend 實作應至少對應以下 bounded contexts：

### 5.1 Review Project Configuration
負責：
- ReviewProject
- ScoreSheetDefinition
- version / publish
- project / score sheet snapshot basis

### 5.2 Sampling
負責：
- SamplingBatch
- rule snapshot
- selected case refs
- retry relation

### 5.3 Assignment and Review
負責：
- ReviewCase
- ReviewSubmission
- 初審 / 複審
- revision
- terminal state guard

### 5.4 Audit and Reporting
負責：
- audit trail
- read model projection
- reporting query support

---

## 6. 後端分層（實際骨架）

工程骨架已建立，目前目錄結構：

```text
services/quality-inspection-service
├─ src/
│  ├─ main/
│  │  ├─ java/com/example/qualityinspection/
│  │  │  ├─ QualityInspectionApplication.java   # Spring Boot entry
│  │  │  ├─ interfaces/         # controller / web / dto / authorization entry
│  │  │  ├─ application/        # use cases / orchestration / command handlers
│  │  │  ├─ domain/             # aggregates / entities / value objects / policies
│  │  │  ├─ infrastructure/     # persistence / external adapters / config
│  │  │  └─ reporting/          # read model / query projection / report support
│  │  └─ resources/
│  │     └─ application.yml     # server config (context-path: /api/quality-inspection)
│  └─ test/
│     └─ java/com/example/qualityinspection/
│        └─ QualityInspectionApplicationTests.java  # context loads smoke test
├─ build.gradle                 # Spring Boot 2.7.18, Java 17
├─ settings.gradle
├─ gradle/wrapper/gradle-wrapper.properties  # Gradle 7.6.4
└─ README.md
```

### 6.1 layer 責任

* `interfaces`：HTTP/API entry、request/response mapping、auth guard entry
* `application`：use case orchestration
* `domain`：核心業務規則
* `infrastructure`：DB / repository / external system integration
* `reporting`：查詢投影、read model、報表支援

---

## 7. 後端落點規則

### 7.1 應落在 domain 的內容

* aggregate invariant
* state transition guard
* revision append-only rule
* terminal state rule
* quality threshold 判定
* same-batch duplicate rule

### 7.2 應落在 application 的內容

* use case orchestration
* transaction boundary
* aggregate loading order
* outbox / audit 發布時機
* actor guard 與 scope guard 協調

### 7.3 應落在 interfaces 的內容

* controller / endpoint
* request / response DTO
* error mapping
* HTTP status mapping

### 7.4 應落在 infrastructure 的內容

* repository implementation
* ORM / SQL / persistence mapping
* external adapter
* config
* migration

### 7.5 不應出現的錯位

* controller 內硬寫 domain rule
* repository 內偷做業務決策
* DB trigger 取代核心 state machine
* UI page 行為直接決定 aggregate invariant

---

## 8. 與 UI 的邊界

backend 與 UI page spec 的關係如下：

* UI page spec 可作為 query shape、command timing、畫面操作語意的參考
* 但 backend 的業務規則仍以：

  * requirements
  * decision-log
  * domain-model
  * state-machine
  * permission-matrix
  * api-spec
    為準

backend 不應因單一 UI 頁面描述，反向創造未經 DDD / contract 確認的規則。

---

## 9. 測試策略對應

backend 實作應以 docs/04-testing 為上游，建立至少以下測試層級：

### 9.1 Domain tests

驗證：

* invariant
* state transition
* revision rule
* terminal state rule
* threshold rule
* same-batch duplicate rule

### 9.2 Application tests

驗證：

* use case orchestration
* actor / scope guard
* transaction boundary
* audit / event behavior

### 9.3 Contract / API tests

驗證：

* request / response contract
* error code
* authorization behavior
* visibility masking

### 9.4 Persistence / integration tests

驗證：

* unique constraint
* version / snapshot / revision persistence
* repository correctness
* external adapter integration

---

## 10. Build / Test / Run / Gate Status

### 10.1 已確認

```yaml
confirmed:
  backend_language: Java
  backend_framework: Spring Boot
  build_tool: Gradle (Groovy DSL)
  spring_boot_version: 2.7.18
  java_version: 11
  gradle_version: 7.6.4
  integration_baseline: must integrate with organization existing api-gateway
  api_base_path: "/api/quality-inspection/**"
  required_auth_header: api-key
  package_root: com.tzuchi.his.quality.inspection
  layers: [domain, application, interfaces, infrastructure, reporting]
  commands:
    build: ".\\gradlew.bat build"
    test: ".\\gradlew.bat test"
    targeted_test: ".\\gradlew.bat test --tests <fqcn-or-pattern>"
    run: ".\\gradlew.bat bootRun"
  development_mode:
    gateway_layer_substitute: >
      local dev proxy 可模擬 api-gateway，須至少模擬 api-key header。
    upstream_substitute: >
      adapter-level stub / mock provider 可模擬尚未齊備的 upstream services。
  note: >
    正式 service boundary 不變。proxy 與 stub 僅作 development substitute，
    不得寫成正式 topology 或業務真相。
```

### 10.2 尚未確認

以下事項 **尚未確認**，不得在本 README 中宣稱已存在：

```yaml
pending_confirmation:
  - lint_tool
  - formatter
  - static_analysis_gate
  - ci_pipeline
  - migration_tool
  - organization_api_gateway_technical_detail_beyond_base_path_and_auth_header
```

若後續 source tree 建立並證實上述項目，應再回來更新本文件。

### 10.3 Backend terminal execution governance

backend terminal work 必須以 `services/quality-inspection-service` 為唯一 module root。
在這個 multi-module repo 中，不得在 repo root 或 frontend module root 猜測 backend Gradle 相對路徑。

執行 backend command 前，至少先確認：

* repo root
* backend module root
* current working directory
* `build.gradle`
* `settings.gradle`
* `gradlew.bat`
* 若為 targeted test，目標 test class 是否存在於 `src/test/java`

PowerShell canonical pattern：

```powershell
$RepoRoot = git rev-parse --show-toplevel
$BackendRoot = Join-Path $RepoRoot 'services\quality-inspection-service'
if (-not (Test-Path (Join-Path $BackendRoot 'build.gradle'))) { throw 'Missing build.gradle in backend module root.' }
if (-not (Test-Path (Join-Path $BackendRoot 'settings.gradle'))) { throw 'Missing settings.gradle in backend module root.' }
if (-not (Test-Path (Join-Path $BackendRoot 'gradlew.bat'))) { throw 'Missing gradlew.bat in backend module root.' }
Set-Location $BackendRoot
.\gradlew.bat test --tests com.tzuchi.his.quality.inspection.interfaces.rest.ReviewCaseApiTest
```

backend Gradle command priority：

1. `.\gradlew.bat <task>`
2. `./gradlew <task>`（僅限非 PowerShell shell，且仍在相同 module root）
3. `gradle <task>` 只能在 wrapper 缺失且 repo 明確允許時使用

backend command baseline：

* build：`.\gradlew.bat build`
* full test：`.\gradlew.bat test`
* targeted test：`.\gradlew.bat test --tests com.tzuchi.his.quality.inspection.interfaces.rest.ReviewCaseApiTest`
* app run：`.\gradlew.bat bootRun`

禁止事項：

* 在 repo root 直接執行 backend Gradle command
* 在 repo root 以 `services\quality-inspection-service\gradlew.bat` 形式硬跑 backend Gradle command，而不是先進入 backend module root
* 在 `frontends/quality-inspection-frontend` 執行 backend Gradle command
* wrapper 存在時改用裸 `gradle test`
* 在不同目錄分別跑 backend build / test / bootRun

---

## 11. 初始實作順序建議

在 backend implementation area 中，建議依下列順序推進：

1. 建立 module skeleton
2. 固定 package / layer boundary
3. 建立 domain model 骨架
4. 建立 state machine guard
5. 建立 application service 骨架
6. 建立 API contract 對應 controller / DTO
7. 建立 persistence mapping
8. 建立測試骨架
9. 建立 audit / event / reporting projection 支援

---

## 12. Escalation Rules

遇到以下情況，先停下直接實作，回到 docs authority：

### 12.1 規則要改

先更新：

* `decision-log.md`
* 必要時同步 `specification-by-example.md`

### 12.2 domain 與 API / persistence 衝突

先對齊：

* `domain-model.md`
* `state-machine.md`
* `application-service.md`
* `api-spec.md`
* `persistence-model.md`

### 12.3 權限與查詢形狀衝突

先對齊：

* `permission-matrix.md`
* `api-spec.md`

### 12.4 UI 要求與 domain contract 衝突

先確認：

* 是否應更新 docs/05-ui
* 或實際應先更新 DDD / contracts

---

## 13. 當前邊界

本 README 目前 **不宣稱** 以下事項已完成：

* lint / static analysis gate 已確認
* migration / DB tool 已確認
* CI pipeline 已確認
* framework-specific instructions 已完整

工程骨架已建立，可使用 `.\gradlew.bat build`、`.\gradlew.bat test`、`.\gradlew.bat bootRun`，但前提是目前 working directory 已切到 `services/quality-inspection-service`。
