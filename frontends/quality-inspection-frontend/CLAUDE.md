# CLAUDE.md — Frontend Implementation Area

本文件是 Claude Code 在 `frontends/quality-inspection-frontend/` 的 path-specific instructions。

## 必讀文件

開始 frontend 工作前，依序讀取：

1. `.ai/rules/repo-hard-rules.md` — repository-wide 硬規則
2. `AGENTS.md` — repo map、docs index、inspection order
3. 本目錄 `README.md` — frontend area guide（責任、分層、落點規則）

## Authority chain

Frontend 實作必須以以下 authority chain 為準（衝突時靠前者優先）：

1. `docs/01-requirements/decision-log.md`
2. `docs/01-requirements/0_品質審查系統規格草案.md`
3. `docs/01-requirements/glossary.md`
4. `docs/02-ddd/permission-matrix.md`
5. `docs/02-ddd/state-machine.md`
6. `docs/03-contracts/api-spec.md`
7. `docs/05-ui/ui-information-architecture.md`
8. `docs/05-ui/ui-page-inventory.md`
9. `docs/05-ui/ui-page-spec-*.md`（各頁 page spec）
10. `docs/04-testing/specification-by-example.md`
11. `docs/04-testing/test-scenarios.md`

## 預期分層

```
shell/           # layout / navigation / route container
pages/           # page-level screens
features/        # business feature modules
components/      # reusable UI components
services/        # API clients / DTO mapping / page orchestration
models/          # frontend view models / DTO types
guards/          # route guard / permission-aware navigation
state/           # client state (不得冒充 domain truth)
```

## 落點規則

- 頁面流程 / 欄位互動 / command-query 觸發 -> `pages/` + `features/`
- API client / request-response mapping -> `services/`
- Route access control / action visibility -> `guards/`
- 不得在前端自行重算 domain invariant
- 不得在 component 中硬寫最終權限真相
- 不得以畫面流程偷改 backend state machine
- 不得以 local state 覆蓋 server truth

## 權限與可見性重點

前端必須正確表現：
- `HOSPITAL_MANAGER` — 本院完整作答明細
- `SYSTEM_ADMIN` — 僅 metadata
- `AUDITOR` — 僅授權院區 / 授權範圍
- 初審 revision — 僅限原審查醫師
- 複審 revision — 僅限複審醫師
- `CLOSED` / `CANCELLED` — 不允許 revision

## Build / test / lint

Frontend tech stack 已確認為 Angular 14 (microfrontend) + npm + Angular CLI。
State management: NgRx。Unit test baseline: Karma + Jasmine。E2E test baseline: Playwright。
Module federation reference: @angular-architects/module-federation。
Shared UI lib baseline: portal51-lib@0.0.16（wrapper/style 優先參考來源）。
Integration baseline 為 organization existing MFE shell。
Shell 負責登入並提供 user context。
Formal API base path: `/api/quality-inspection/**`，required auth header: `api-key`。
開發期可使用 standalone mode 作為 shell-layer substitute（需模擬 user context），
local dev proxy 作為 gateway-layer substitute（需模擬 api-key header）。
component-level 使用細節（wrapper pattern、design token）待確認。

Commands:
- Install: `npm install`
- Start: `npm start`
- Standalone: `npm run start:standalone`
- Build: `npm run build`
- Unit test: `npm test`
- E2E test: `npm run e2e`

App modules: core, shared, features, shell
Environments: standalone (development) / production (integrated)

Lint / formatter / static analysis 尚未設定。
