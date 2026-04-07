# GitHub Copilot Instructions — quality-inspection

本文件是 GitHub Copilot 在此 repository 的 repo-level instructions。
Copilot Chat 會自動讀取此檔作為所有對話的背景指引。

---

## 1. Governance Entry Points

| 檔案 | 用途 |
|------|------|
| `.ai/rules/repo-hard-rules.md` | Repository-wide **硬規則** — 必讀 |
| `AGENTS.md` | Repo map、docs index、inspection order、placement rules |
| `.ai/project-context.yaml` | Authoritative project context 與 assumptions |

修改任何內容前，請先閱讀上述檔案。

---

## 2. Document Authority Precedence

文件衝突時，排序靠前者優先：

1. `docs/01-requirements/decision-log.md`
2. `docs/01-requirements/0_品質審查系統規格草案.md`
3. `docs/01-requirements/glossary.md`
4. `docs/02-ddd/*` — domain model / state machine / permission / events / application service
5. `docs/03-contracts/*` — API spec / persistence model
6. `docs/04-testing/*` — specification-by-example / test scenarios
7. `docs/05-ui/*` — UI IA / page spec
8. `docs/99-archive/*` — 僅歷史參考，非現行 authority

---

## 3. Repository Overview

- **系統**：品質審查系統（質量檢查 / Quality Inspection）
- **業務範圍**：病歷抽選、案件分派、初審、複審、查詢追蹤、統計報表、revision 歷程
- **核心語意**：ReviewProject、ScoreSheetDefinition、SamplingBatch、ReviewCase、ReviewSubmission
- **Bounded Contexts**：Review Project Configuration、Sampling、Assignment and Review、Audit and Reporting

---

## 4. Key Hard Rules

> 完整規則見 `.ai/rules/repo-hard-rules.md`。

- **修改前先讀 authority**：不跳過 requirements / decision / domain，直接改 contract 或 UI。
- **先改最小正確落點**：permission 規則不寫進 domain invariant；state machine 不只靠 API spec 承載。
- **不做任務範圍外的 incidental refactor**：只改任務需求所在的範圍。
- **未經 evidence 確認的事項不得宣稱完成**：尚未有 source code / build script / CI 的 gate 不得宣稱通過。
- **核心規則要變時，先更新 `decision-log.md`**，不直接改實作。
- **archive 不作現行 authority**：`docs/99-archive/` 與現行文件衝突時，現行文件優先。
- **specification-by-example 與 test-scenarios 分工不可混淆**：
  - `specification-by-example.md` — 固定共同理解與規則例子
  - `test-scenarios.md` — 轉成可驗證情境與覆蓋

---

## 5. Tech Stack

### Backend — `services/quality-inspection-service/`

| 項目 | 值 |
|------|---|
| Language | Java 11 |
| Framework | Spring Boot 2.7.18 |
| Build tool | Gradle 7.6.4 (Groovy DSL) |
| Package root | `com.tzuchi.his.quality.inspection` |
| Layers | `domain` / `application` / `interfaces` / `infrastructure` / `reporting` |
| Integration baseline | Organization existing API Gateway |

**Build / test / run:**

```bash
cd services/quality-inspection-service
./gradlew build
./gradlew test
./gradlew bootRun
```

### Frontend — `frontends/quality-inspection-frontend/`

| 項目 | 值 |
|------|---|
| Framework | Angular 14 (microfrontend) |
| Package manager | npm |
| State management | NgRx |
| Unit test | Karma + Jasmine |
| E2E test | Playwright |
| Module federation | `@angular-architects/module-federation` |
| Shared UI lib | `portal51-lib@0.0.16` |
| Integration baseline | Organization existing MFE shell |

**Build / test / run:**

```bash
cd frontends/quality-inspection-frontend
npm install
npm start                   # dev (integrated)
npm run start:standalone    # dev (standalone mode)
npm run build
npm test                    # unit tests
npm run e2e                 # e2e tests
```

---

## 6. Integration Baseline

| Layer | 正式目標 | 開發期替身 |
|-------|----------|-----------|
| Gateway | Organization existing API Gateway | Local dev proxy（需模擬 `api-key` header） |
| Shell | Organization existing MFE shell | Standalone mode（需模擬 user context） |
| Upstream | 正式 upstream services | Adapter-level stub / mock |

- **Formal API base path**: `/api/quality-inspection/**`
- **Required auth header**: `api-key`
- **Shell provides**: login + user context

> 開發期替身僅限 development / non-production。不得讓替身策略污染正式 architecture 描述。

---

## 7. Backend Layer Rules

- Domain invariant / state guard / revision rule → `domain/`
- Use case orchestration / actor guard / transaction boundary → `application/`
- HTTP endpoint / request-response mapping / DTO → `interfaces/`
- DB mapping / external integration / config → `infrastructure/`
- Read model / query projection / report → `reporting/`

**禁止**：
- 在 controller 中硬寫 domain rule
- 在 repository 中做業務決策
- 讓 UI page spec 反向決定 aggregate invariant

---

## 8. Frontend Layer Rules

```
shell/       # layout / navigation / route container
pages/       # page-level screens
features/    # business feature modules
components/  # reusable UI components
services/    # API clients / DTO mapping / page orchestration
models/      # frontend view models / DTO types
guards/      # route guard / permission-aware navigation
state/       # client state (不得冒充 domain truth)
```

**禁止**：
- 在前端自行重算 domain invariant
- 在 component 中硬寫最終權限真相
- 以畫面流程偷改 backend state machine
- 以 local state 覆蓋 server truth

---

## 9. Escalation Rules

遇到以下情況，**先停在文件層處理，不直接實作**：

- 核心規則要變（quality threshold / terminal state / revision policy / sampling scope）
- 文件 authority 之間衝突
- UI 規格與 domain / API 脫鉤
- Testing 出現無對應 example / decision 的高風險規則

處理優先序：

1. `docs/01-requirements/decision-log.md`
2. `docs/01-requirements/0_品質審查系統規格草案.md`
3. `docs/01-requirements/glossary.md`
4. 對應專業文件（`docs/02-ddd/`、`docs/03-contracts/`、`docs/05-ui/`）

---

## 11. Path-Specific Instructions

下列文件對特定路徑自動生效，補充本文件未覆蓋的路徑專屬規則：

| 路徑 | 補充文件 |
|------|----------|
| `services/quality-inspection-service/**` | `.github/instructions/backend.instructions.md` |
| `frontends/quality-inspection-frontend/**` | `.github/instructions/frontend.instructions.md` |
| `docs/**` | `.github/instructions/docs.instructions.md` |

---

## 10. Unverified Items (Pending Confirmation)

以下事項尚未有 source evidence，不得宣稱已確認：

- Lint / formatter / static analysis gate
- CI pipeline entry
- Organization API Gateway 具體技術實作細節
- Organization MFE Shell 具體技術實作細節
- `portal51-lib` component-level usage detail（wrapper pattern、design token）
