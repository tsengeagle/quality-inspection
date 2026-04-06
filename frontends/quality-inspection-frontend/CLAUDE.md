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

Frontend tech stack 已確認為 Angular (microfrontend)，integration baseline 為 organization existing MFE shell。
開發期可使用 standalone mode 作為 shell-layer substitute，local dev proxy 作為 gateway-layer substitute。
Frontend 必須參考組織內既有 UI 風格；具體元件庫、design token 細節待確認。
目前無已確認的 build、test、lint 指令（package manager / test runner / lint tool 待工程骨架建立後確認）。
