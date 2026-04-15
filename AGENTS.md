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
- `docs/06-frontend`
- `docs/07-design`

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
│  ├─ governance/
│  ├─ prompts/
│  └─ rules/
├─ AGENTS.md
├─ docs/
│  ├─ 01-requirements/
│  ├─ 02-ddd/
│  ├─ 03-contracts/
│  ├─ 04-testing/
│  ├─ 05-ui/
│  ├─ 06-frontend/
│  ├─ 07-design/
│  ├─ 99-archive/
│  ├─ design/
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

#### `docs/06-frontend`

Frontend design method 與 implementation bridge 區。
放 frontend design method、roadmap、route/module/view-model/API binding/component map、review gate 與 frontend test scenarios。

#### `docs/07-design`

Design execution 區。
放 wireframe brief、wireframes、prototype / flow validation artifacts、以及必要時的 Figma translation spec。

#### `docs/99-archive`

歷史稿與被取代但需保留的設計筆記。
此區不是現行 authority，除非現行文件明確引用。

#### `docs/design`

Broader frontend design baseline references。
放組織層級的前端設計規範（`DESIGN.v2.1` 家族）。
此區不是 repo governance layer，不是 solution UI page spec。
由 `.ai/governance/authority-sources.md` 引用作為 broader baseline reference。

#### `.ai/governance`

Frontend MFE 治理層。
放前端微前端實作的治理規則、設計語言、決策記錄與 authority index。
日常 frontend MFE 任務應從此層進入，而非直接依賴 bootstrap 推論實作規則。
入口為 `.ai/governance/repo-governance-instruction.md`。

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

### 4.7 `docs/06-frontend`

#### `frontend-design-method.md`

Frontend foundational design truth 與方法論基線。

#### `frontend-design-roadmap.md`

Frontend 文件推進順序、落點與 design execution roadmap。

#### `frontend-review-gate-pending-items.md`

Frontend focused review gate 與待人類拍板議題。

#### `frontend-test-scenarios.md`

Frontend reviewer / QA 可驗證的 scenario 基線。

#### `ui-api-binding-matrix.md`

頁面區塊到 API / loading / error / orchestration 的 binding。

#### `ui-component-map.md`

跨頁 page regions 與 reusable component map。

#### `ui-route-module-map.md`

UI IA 映射到 Angular route / feature module / guard 邊界。

#### `ui-state-actions-matrix.md`

跨頁 UI 狀態與操作矩陣。

#### `ui-view-model-spec.md`

前端實際使用的 view model 與 projection 規格。

---

### 4.8 `docs/07-design`

#### `ui-wireframe-brief.md`

第一輪 wireframe / prototype execution 的 scope freezing 與 shared structure 規則。

#### `wireframes/README.md`

目前低保真 wireframe artifact 索引與狀態說明。

#### `wireframes/figma-frame-spec.md`

可選用的 Figma translation spec；不是 Figma artifact 本身。

---

### 4.9 `docs/99-archive`

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

### 5.6 Frontend design / wireframe / prototype 相關修改

1. `docs/01-requirements/decision-log.md`
2. `.ai/rules/repo-hard-rules.md`
3. `docs/06-frontend/frontend-design-method.md`
4. `docs/06-frontend/frontend-design-roadmap.md`
5. `docs/05-ui/ui-information-architecture.md`
6. `docs/05-ui/ui-page-inventory.md`
7. 對應頁面的 `docs/05-ui/ui-page-spec-*.md`
8. `docs/07-design/ui-wireframe-brief.md`
9. `docs/07-design/wireframes/*`

### 5.7 Frontend MFE 實作相關修改

1. `docs/01-requirements/decision-log.md`
2. `.ai/governance/repo-governance-instruction.md`（governance entrypoint）
3. `.ai/governance/frontend-ui.instructions.md`（operational entrypoint）
4. 依任務類型讀取 deeper governance files（見 `frontend-ui.instructions.md` §4 Task Classification）
5. `docs/02-ddd/permission-matrix.md`
6. `docs/02-ddd/state-machine.md`
7. `docs/03-contracts/api-spec.md`
8. 對應頁面的 `docs/05-ui/ui-page-spec-*.md`
9. 若任務涉及 frontend terminal execution / typecheck / unit / e2e，先讀 `frontends/quality-inspection-frontend/package.json`
10. 同類任務需同步確認 `frontends/quality-inspection-frontend/angular.json`
11. typecheck / unit task 需同步確認 `frontends/quality-inspection-frontend/tsconfig.json`、`tsconfig.spec.json`、`karma.conf.js`
12. e2e task 需同步確認 `frontends/quality-inspection-frontend/playwright.config.ts` 與 `e2e/`

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

### 6.6 Frontend Design / Bridge

* `docs/06-frontend/frontend-design-method.md`
* `docs/06-frontend/frontend-design-roadmap.md`
* `docs/06-frontend/ui-state-actions-matrix.md`
* `docs/06-frontend/ui-route-module-map.md`
* `docs/06-frontend/ui-view-model-spec.md`
* `docs/06-frontend/ui-api-binding-matrix.md`
* `docs/06-frontend/ui-component-map.md`

### 6.7 Design Execution

* `docs/07-design/ui-wireframe-brief.md`
* `docs/07-design/wireframes/README.md`
* `docs/07-design/wireframes/figma-frame-spec.md`

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

#### 要改「frontend design method / implementation bridge / prototype 驗證規則」

更新：

* `docs/06-frontend/`

#### 要改「wireframe / prototype / design execution artifact」

更新：

* `docs/07-design/`

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

目前已確認的 tech stack、integration baseline 與工具鏈方向：

```yaml
confirmed:
  backend:
    module_root: "services/quality-inspection-service"
    language: Java
    framework: Spring Boot
    build_tool: Gradle (Groovy DSL)
    integration_baseline: must integrate with organization existing api-gateway
    commands:
      build: ".\\gradlew.bat build"
      test: ".\\gradlew.bat test"
      targeted_test: ".\\gradlew.bat test --tests <fqcn-or-pattern>"
      run: ".\\gradlew.bat bootRun"
  frontend:
    module_root: "frontends/quality-inspection-frontend"
    framework: Angular 14
    architecture: microfrontend
    package_manager: npm
    build_dev_tool: Angular CLI
    state_management: NgRx
    unit_test_baseline: Karma + Jasmine
    e2e_test_baseline: Playwright
    module_federation_reference: "@angular-architects/module-federation"
    integration_baseline: must integrate with organization existing MFE shell
    commands:
      install: "npm install"
      start: "npm start"
      start_standalone: "npm run start:standalone"
      build: "npm run build"
      typecheck: "npm run typecheck"
      test: "npm test"
      test_unit: "npm run test:unit"
      test_unit_ci: "npm run test:unit:ci"
      e2e: "npm run e2e"
      test_e2e: "npm run test:e2e"
      verify_frontend: "npm run verify:frontend"
  ui_governance:
    organization_ui_style: must align with organization existing UI style
    shared_ui_library: portal51-lib@0.0.16 (repo-local dependency evidence)
    portal51_lib_role: frontend baseline reference; implementation usage detail pending confirmation
  api_baseline:
    formal_base_path: "/api/quality-inspection/**"
    required_auth_header: api-key
  shell_baseline:
    shell_provides: login and user context
  development_mode:
    gateway_layer_substitute: local dev proxy (must simulate api-key header)
    shell_layer_substitute: standalone mode (must simulate user context)
    upstream_substitute: adapter-level stub / mock provider
```

文件設計樹已存在且分層清楚。
Backend / frontend 工程骨架已建立。

Frontend terminal execution governance note:

- repo root currently contains `package-lock.json` but no repo-root `package.json`
- frontend npm install / typecheck / unit / e2e must execute from `frontends/quality-inspection-frontend`
- prefer `npm run <script>` first, module-root local binary second, and `npx` only as the last resort
- keep install / typecheck / unit / e2e in the same module root, preferably in the order install -> typecheck -> unit -> e2e
- e2e readiness requires `playwright.config.ts`, `e2e/`, and at least one Playwright-discoverable test file; config presence alone is not sufficient
- do not treat repo-root lockfile as the frontend module lockfile

Backend terminal execution governance note:

- backend Gradle commands must execute from `services/quality-inspection-service`, not repo root and not `frontends/quality-inspection-frontend`
- before any backend terminal command, explicitly identify repo root, target backend module root, current working directory, and the exact command to run
- in PowerShell, resolve repo root first, then enter the backend module root before build / test / targeted test / bootRun commands
- required pre-checks before backend execution: `build.gradle`, `settings.gradle`, `gradlew.bat`; targeted test also requires confirming the target test class exists under `src/test/java`
- command priority for backend Gradle work: module-root `./gradlew` or `.\gradlew.bat` first; system `gradle` only when wrapper is absent and the repo explicitly allows it
- keep backend build / full test / targeted test / app run in the same backend module root; do not mix backend Gradle execution across repo root or frontend module root
- targeted backend tests use the standard Gradle `test --tests <fqcn-or-pattern>` path from the backend module root; do not assume the same command works from another cwd
- module-local build artifacts and reports live under `services/quality-inspection-service/build`; do not use repo-root artifacts as backend execution evidence

PowerShell canonical pre-check pattern:

```powershell
$RepoRoot = git rev-parse --show-toplevel
$BackendRoot = Join-Path $RepoRoot 'services\quality-inspection-service'

if (-not (Test-Path (Join-Path $BackendRoot 'build.gradle'))) { throw 'Missing build.gradle in backend module root.' }
if (-not (Test-Path (Join-Path $BackendRoot 'settings.gradle'))) { throw 'Missing settings.gradle in backend module root.' }
if (-not (Test-Path (Join-Path $BackendRoot 'gradlew.bat'))) { throw 'Missing gradlew.bat in backend module root.' }

Set-Location $BackendRoot
.\gradlew.bat test --tests com.tzuchi.his.quality.inspection.interfaces.rest.ReviewCaseApiTest
```

Additional prohibited patterns:

- running `services\quality-inspection-service\gradlew.bat` from repo root instead of entering the backend module root first
- verifying a targeted test class name without first confirming the corresponding file exists under `src/test/java`
- using repo-root or frontend-root `build/` artifacts as evidence for backend execution readiness

### 9.2 尚未確認

目前沒有足夠 repo evidence 證明以下入口已存在：

```yaml
pending_confirmation:
  - lint_command
  - formatter_command
  - static_analysis_gate
  - ci_pipeline_entry
  - packaging_entry
  - backend_migration_tool
  - portal51_lib_component_level_usage_detail
  - organization_api_gateway_technical_detail_beyond_base_path
  - organization_mfe_shell_technical_detail_beyond_module_federation
```

### 9.3 不得自行假設

以下目前不得寫成已確認事實：

```yaml
assumptions:
  - organization_api_gateway_detail_beyond_base_path_not_to_be_fabricated: true
  - organization_mfe_shell_detail_beyond_module_federation_not_to_be_fabricated: true
  - portal51_lib_component_level_detail_not_to_be_fabricated: true
```

### 9.4 分層替身策略

正式拓樸不因開發便利而改變。開發期若正式 shell / gateway / upstream services 尚未齊備，
採用分層替身策略。原則：

1. 正式 contract 與 integration boundary 不變
2. 以可控替身模擬缺的那一層
3. 替身僅限 development / non-production
4. 不得讓替身策略污染正式 architecture 描述

替身配置：

| Layer | 正式目標 | 開發期替身 | 限制 |
|-------|----------|-----------|------|
| Gateway | organization existing API Gateway | local dev proxy | 不得寫死 backend 直連 URL；須模擬 api-key header |
| Shell | organization existing MFE shell | standalone mode | 不代表正式架構非 MFE；須模擬 user context |
| Upstream | 正式 upstream services | adapter-level stub / mock | 不得把 stub 寫成業務真相 |

正式 API baseline：

| 項目 | 值 |
|------|---|
| Formal API base path | `/api/quality-inspection/**` |
| Required auth header | `api-key` |
| Shell provides | login + user context |

開發模式：

| Mode | 用途 | 拓樸 |
|------|------|------|
| Backend-only | 後端先行開發 | test client → local service |
| Frontend standalone | 前端先行開發 | Browser → Angular standalone → dev proxy → local service |
| Integrated | 正式整合驗證 | Browser → MFE shell → API Gateway → service |

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

* build / test / run 已經過完整驗證（骨架已建立，但尚未在 CI 中確認）
* lint / static analysis gate 已確認
* organization existing api-gateway 的具體技術實作已確認
* organization existing MFE shell 的具體技術實作已確認
* organization existing UI component system 的具體細節已確認

以下事項已確認：

* backend implementation skeleton 已存在（Gradle + Spring Boot + layered packages）
* frontend implementation skeleton 已存在（Angular 14 + NgRx + Karma/Jasmine + Playwright）

* backend tech stack: Java + Spring Boot + Gradle (Groovy DSL)（已確認）
* frontend tech stack: Angular 14 + microfrontend + npm + Angular CLI + NgRx + Karma/Jasmine（已確認）
* module federation reference: @angular-architects/module-federation（已確認）
* repo-local frontend dependency evidence: portal51-lib@0.0.16（已確認）；implementation role / usage detail 仍待確認
* backend integration baseline: must integrate with organization existing api-gateway（已確認）
* frontend integration baseline: must integrate with organization existing MFE shell（已確認）
* formal API base path: /api/quality-inspection/**（已確認）
* required auth header: api-key（已確認）
* shell provides: login + user context（已確認）
* development mode: proxy + standalone mode（已確認）
* UI guidance: must align with organization existing UI style（已確認）
* `.ai/rules/repo-hard-rules.md` — canonical hard rules source 已存在
* `.ai/bootstrap/*` — solution / backend / frontend bootstrap 均已存在
* `.ai/governance/*` — frontend MFE governance package 已存在（R1 active）
* `.ai/prompts/*` — solution / backend / frontend init prompt 均已存在
* `.ai/project-context.yaml` — solution-level authoritative project context 已存在
* `AGENTS.md` — repo operating guide 已存在
* `CLAUDE.md` — Claude Code repo-level instructions 已存在

其餘待後續依實際 repo evidence 再確認。
