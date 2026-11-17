# quality-inspection-frontend

## 1. 文件角色

本文件是 `frontends/quality-inspection-frontend` 的 **frontend implementation area guide**。

它的用途是：

- 說明 frontend implementation area 在整個 solution 中的角色
- 對齊 frontend 實作與既有 docs authority
- 定義前端應承接哪些責任，哪些不應承接
- 提供後續頁面實作、模組劃分、測試與落點指引

本文件不取代：

- 需求書
- decision log
- glossary
- domain model
- state machine
- permission matrix
- API spec
- test scenarios
- UI page specs

若本文件與上述 authority 衝突，應優先回頭檢查 `docs` 下對應文件。

---

## 2. 模組定位

`quality-inspection-frontend` 是品質審查系統的 **frontend implementation area**。

它預期承載以下責任：

- 管理端 UI 落地
- 頁面流程與操作互動
- query / command 對應的前端整合
- 權限與內容可見性的前端呈現
- 表單、列表、詳情、流程頁與歷程頁實作
- 與 backend API contract 的前端對接
- frontend 測試與驗證

它不應承載以下責任：

- domain rule authority
- API contract authority
- persistence truth
- 以畫面行為重新定義 state machine
- 以 component state 取代業務狀態
- 以 UI convenience 覆蓋 permission / scope 規則

---

## 3. 對應的 docs authority

frontend 開發前，必須先讀取並遵守下列文件。

### 3.1 需求與決策
- `docs/01-requirements/0_品質審查系統規格草案.md`
- `docs/01-requirements/decision-log.md`
- `docs/01-requirements/glossary.md`

### 3.2 DDD / 規則邊界
- `docs/02-ddd/domain-model.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/02-ddd/application-service.md`

### 3.3 對接契約
- `docs/03-contracts/api-spec.md`

### 3.4 測試
- `docs/04-testing/specification-by-example.md`
- `docs/04-testing/test-scenarios.md`

### 3.5 UI authority
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

### 3.6 frontend 不直接作為 authority 的文件
以下文件可作補充參考，但不是前端頁面行為的第一 authority：

- `docs/03-contracts/persistence-model.md`
- `docs/99-archive/*`

---

## 4. Frontend 應承接的核心能力

依目前 docs authority，frontend 應優先承接以下核心能力：

1. 抽選執行頁
2. 抽選批次列表頁
3. 案件列表頁
4. 案件詳情頁
5. 案件分派頁
6. 初審作業頁
7. 複審作業頁
8. revision 歷程頁
9. 權限 / scope / 內容可見性的 UI 呈現
10. 與 backend API 的 query / command 對接

---

## 5. 預期前端資訊架構對應

frontend 實作應至少對應以下頁面或功能區：

### 5.1 抽選與批次
- 抽選執行
- 抽選批次列表
- 抽選結果檢視

### 5.2 案件管理
- 案件列表
- 案件詳情
- 案件分派

### 5.3 審查作業
- 初審作業
- 複審作業

### 5.4 歷程與追溯
- revision 歷程
- submission / case 歷程檢視

---

## 6. 前端分層（實際骨架）

工程骨架已建立，目前目錄結構：

```text
frontends/quality-inspection-frontend
├─ src/
│  ├─ app/
│  │  ├─ shell/                 # layout / navigation / route container
│  │  ├─ core/                  # singleton services / interceptors / guards
│  │  ├─ shared/                # reusable UI components / pipes / directives
│  │  └─ features/              # business feature modules (lazy-loaded)
│  ├─ environments/             # environment configs (standalone / production)
│  └─ assets/                   # static assets
├─ e2e/
│  └─ src/                      # Playwright e2e test files
├─ package.json
├─ angular.json
├─ tsconfig.json
├─ proxy.conf.json              # gateway-layer substitute (dev proxy)
├─ karma.conf.js                # Karma + Jasmine unit test config
├─ playwright.config.ts         # Playwright e2e config
├─ webpack.config.js            # module federation config
└─ README.md
```

### 6.1 layer 責任

* `shell`：layout、navigation、route container
* `pages`：page entry 與 page composition
* `features`：以業務功能切分的 UI 模組
* `components`：可重用元件
* `services`：API client、page-level orchestration、DTO mapping
* `guards`：route guard、permission-aware navigation
* `state`：畫面狀態或 client-side coordination，不得冒充 domain truth

---

## 7. 前端落點規則

### 7.1 應落在 pages / features 的內容

* 頁面流程
* 頁面欄位與互動
* command/query 觸發時機
* loading / empty / error state 呈現
* revision history 呈現方式

### 7.2 應落在 services 的內容

* API client
* request/response mapping
* page-level orchestration
* query parameter 組裝
* error handling adaptation

### 7.3 應落在 guards / permission-aware UI 的內容

* route access control
* action visibility
* page-level capability gating
* metadata-only / full-detail UI 差異化呈現

### 7.4 不應出現的錯位

* 在前端自行重算 domain invariant
* 在 component 中硬寫最終權限真相
* 以畫面流程偷改 backend state machine
* 以 local state 覆蓋 server truth
* 以 UI 方便性直接跳過 permission / scope 限制

---

## 8. 前端與 backend / domain 的邊界

frontend 與 backend / domain 的關係如下：

### 8.1 frontend 可以做的事

* 依 API spec 發送 query / command
* 依 permission matrix 顯示或隱藏操作
* 依 state machine 決定按鈕可用性與流程提示
* 依 UI page spec 組裝畫面與互動

### 8.2 frontend 不應做的事

* 自行創造新的 case status
* 自行定義 revision 可不可做
* 自行放寬角色 scope
* 直接假設 SYSTEM_ADMIN 可看 full detail
* 直接假設 HOSPITAL_MANAGER 可跨院查詢

前端顯示與操作能力，必須與：

* `permission-matrix.md`
* `state-machine.md`
* `api-spec.md`

保持一致。

---

## 9. 權限與可見性在前端的落地重點

根據目前已接受規則，前端至少要正確表現以下差異：

* `HOSPITAL_MANAGER` 可查看本院完整作答明細
* `SYSTEM_ADMIN` 僅能查看 metadata
* `AUDITOR` 僅能查授權院區 / 授權範圍
* 初審 revision 僅限原審查醫師
* 複審 revision 僅限複審醫師
* `CLOSED` / `CANCELLED` 不允許 revision

這些規則不應只停留在 backend，frontend 也應有正確的：

* action visibility
* button enable/disable
* view shape difference
* forbidden state feedback

---

## 10. 測試策略對應

frontend 實作應以 docs/04-testing 為上游，建立至少以下測試層級：

### 10.1 Component tests

驗證：

* 欄位顯示
* 互動事件
* disabled / hidden state
* loading / error / empty state

### 10.2 Page / feature integration tests

驗證：

* query / command flow
* route navigation
* permission-aware UI
* revision / terminal state UI behavior

### 10.3 Contract-aware frontend tests

驗證：

* API request shape
* response mapping
* error handling
* metadata-only vs full-detail rendering

### 10.4 E2E tests

驗證：

* 核心流程可操作
* page-to-page flow
* 權限差異
* 初審 / 複審 / revision 主要路徑

執行環境：`npm run e2e` 預設使用 Playwright 依專案鎖定版本下載的瀏覽器（`npx playwright install chromium`）。
若環境無法下載該版本（例如雲端 session 只預裝較新的 Chromium），可設定環境變數 `QI_PLAYWRIGHT_CHROMIUM_PATH` 指向既有的 Chromium 執行檔：

```bash
QI_PLAYWRIGHT_CHROMIUM_PATH=/opt/pw-browsers/chromium npm run e2e
```

* 未設定或為空白時行為不變。
* 路徑不存在時，`playwright.config.ts` 會直接報錯，不會靜默改回預設瀏覽器。
* 指定的 Chromium 版本可能比專案鎖定的 Playwright 預期的新；以實際 e2e 結果為準。

---

## 11. Build / Test / Run / Gate Status

### 11.1 已確認

```yaml
confirmed:
  frontend_framework: Angular 14
  frontend_architecture: microfrontend
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
    test: "npm test"
    e2e: "npm run e2e"
  ui_governance:
    organization_ui_style: must align with organization existing UI style
    shared_ui_library: portal51-lib@0.0.16
    portal51_lib_role: shared UI lib baseline / wrapper-style priority reference
  api_baseline:
    formal_base_path: "/api/quality-inspection/**"
    required_auth_header: api-key
  shell_baseline:
    shell_provides: login and user context
  development_mode:
    shell_layer_substitute: >
      standalone mode 作為 MFE shell 開發期替身。
      frontend 同時考慮 standalone mode 與 future MFE integrated mode。
      standalone mode 不代表正式架構非 MFE。
      standalone mode 需自行模擬 user context。
    gateway_layer_substitute: >
      local dev proxy 模擬 api-gateway。
      frontend 仍使用正式 gateway-style API path（/api/quality-inspection/**），
      不得寫死 backend 直連 URL。
      proxy 須至少模擬 api-key header。
  ui_component_detail:
    status: partial
    note: >
      portal51-lib@0.0.16 已確認為 shared UI lib baseline 與 wrapper/style 優先參考來源。
      component-level 使用細節（wrapper pattern、design token mechanism）仍為 pending confirmation。
      若需要更精準落地，可向使用者索取既有 UI component 範例檔案。
```

### 11.2 尚未確認

以下事項 **尚未確認**，不得在本 README 中宣稱已存在：

```yaml
pending_confirmation:
  - lint_tool
  - formatter
  - static_analysis_gate
  - ci_pipeline
  - portal51_lib_component_level_usage_detail
  - exact_design_token_mechanism
  - organization_mfe_shell_technical_detail_beyond_module_federation
```

若後續 source tree 建立並證實上述項目，應再回來更新本文件。

---

## 12. 初始實作順序建議

在 frontend implementation area 中，建議依下列順序推進：

1. 建立 module skeleton
2. 固定 route / page / feature boundary
3. 建立 shared layout / navigation
4. 建立 API client 與 error handling skeleton
5. 先做列表 / 詳情 / 分派等骨幹頁
6. 再做初審 / 複審 / revision 等流程頁
7. 補上 permission-aware UI
8. 補上測試骨架
9. 最後收斂 UX 細節與 page-level polish

---

## 13. Escalation Rules

遇到以下情況，先停在 docs authority 對齊，不要直接在 UI 端自行解讀。

### 13.1 頁面流程與 state machine 衝突

先對齊：

* `docs/02-ddd/state-machine.md`
* `docs/03-contracts/api-spec.md`

### 13.2 權限顯示與 permission matrix 衝突

先對齊：

* `docs/02-ddd/permission-matrix.md`
* `docs/03-contracts/api-spec.md`

### 13.3 畫面要顯示的欄位與 API 不一致

先對齊：

* `docs/05-ui` 對應頁面 spec
* `docs/03-contracts/api-spec.md`

### 13.4 UI 想新增新操作，但 decision / DDD 尚未定義

先更新：

* `docs/01-requirements/decision-log.md`
* 必要時同步：

  * `docs/02-ddd/*`
  * `docs/03-contracts/api-spec.md`
  * `docs/04-testing/*`

---

## 14. 當前邊界

本 README 目前 **不宣稱** 以下事項已完成：

* lint / static analysis gate 已確認
* CI pipeline 已確認
* component library usage detail 已確認
* route structure 已完整
* framework-specific instructions 已完整

工程骨架已建立，可使用 `npm install`、`npm start`、`npm test`、`npm run e2e`。
