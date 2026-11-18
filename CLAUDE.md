# CLAUDE.md

本文件是 Claude Code 在此 repository 的 repo-level instructions。

## Governance entry points

- **Hard rules**: `.ai/rules/repo-hard-rules.md` — 必讀，repository-wide 硬規則
- **Operating guide**: `AGENTS.md` — repo map、docs index、inspection order、placement rules
- **Project context**: `.ai/project-context.yaml` — authoritative project context、assumptions
- **修法機制**: `law/README.md` — 律法清冊與修法紀錄；判決程式在 `harness-core/`（`harness-core/README.md`）。修改律法路徑前必讀
- **Frontend MFE governance**: `.ai/governance/repo-governance-instruction.md` — frontend MFE 治理入口，日常 frontend 任務從此進入

## Document authority precedence

1. `docs/01-requirements/decision-log.md`
2. `docs/01-requirements/0_品質審查系統規格草案.md`
3. `docs/01-requirements/glossary.md`
4. `docs/02-ddd/*`
5. `docs/03-contracts/*`
6. `docs/04-testing/*`
7. `docs/05-ui/*`
8. `docs/06-frontend/*`
9. `docs/07-design/*`
10. `docs/99-archive/*` — 僅歷史參考，非現行 authority

## Key rules

- 修改前先讀 authority source，確認正確落點再改
- 不做任務範圍外的 incidental refactor
- 未經 evidence 確認的事項不得宣稱完成
- archive 不作現行 authority
- 一般 wireframe / prototype 任務可直接執行；若涉及 Figma / Figma MCP、付費能力，或遇到額度 / 權限 / API / MCP / 方案限制，必須先回報；免費 / 本地優先
- specification-by-example 與 test-scenarios 分工不可混淆
- 核心規則要變時，先更新 decision-log，不直接改實作
- 動到後端問題側（`domain`、`application`、`interfaces`、`reporting`）之前，先查 `law/coverage.txt`（D-060）。範圍是 `grandfathered` 或不在清單內時，先停下來向負責人說明「這裡沒有可執行的律法」，請負責人決定：先立法、以修法列為暫准，或在暫准範圍內照現有權威文件實作。不在清單內的新範圍，`law-check` 會判為未通過

## Implementation areas

- Backend: `services/quality-inspection-service/` — Java + Spring Boot + Gradle (Groovy DSL)
- Frontend: `frontends/quality-inspection-frontend/` — Angular 14 (microfrontend) + npm + NgRx

兩個 area 均有 README.md、CLAUDE.md 與最小工程骨架。
Tech stack 已確認，build/test/run 指令已建立。lint/static analysis/CI gates 尚未有 source evidence。

## Build / test / lint

Backend:
- Build: `cd services/quality-inspection-service && ./gradlew build`
- Test: `cd services/quality-inspection-service && ./gradlew test`
- Run: `cd services/quality-inspection-service && ./gradlew bootRun`

Frontend:
- Install: `cd frontends/quality-inspection-frontend && npm install`
- Start: `cd frontends/quality-inspection-frontend && npm start`
- Standalone: `cd frontends/quality-inspection-frontend && npm run start:standalone`
- Build: `cd frontends/quality-inspection-frontend && npm run build`
- Typecheck: `cd frontends/quality-inspection-frontend && npm run typecheck`
- Unit test: `cd frontends/quality-inspection-frontend && npm test`
- Unit test (CI): `cd frontends/quality-inspection-frontend && npm run test:unit:ci`
- E2E test: `cd frontends/quality-inspection-frontend && npm run e2e`
- Verify all: `cd frontends/quality-inspection-frontend && npm run verify:frontend`
- Demo launch: `cd frontends/quality-inspection-frontend && npm run demo:launch`
- Demo launch + preflight: `cd frontends/quality-inspection-frontend && npm run demo:launch:preflight`

Lint / formatter / static analysis 尚未設定。
