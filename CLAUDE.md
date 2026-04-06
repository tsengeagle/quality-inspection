# CLAUDE.md

本文件是 Claude Code 在此 repository 的 repo-level instructions。

## Governance entry points

- **Hard rules**: `.ai/rules/repo-hard-rules.md` — 必讀，repository-wide 硬規則
- **Operating guide**: `AGENTS.md` — repo map、docs index、inspection order、placement rules
- **Project context**: `.ai/project-context.yaml` — authoritative project context、assumptions

## Document authority precedence

1. `docs/01-requirements/decision-log.md`
2. `docs/01-requirements/0_品質審查系統規格草案.md`
3. `docs/01-requirements/glossary.md`
4. `docs/02-ddd/*`
5. `docs/03-contracts/*`
6. `docs/04-testing/*`
7. `docs/05-ui/*`
8. `docs/99-archive/*` — 僅歷史參考，非現行 authority

## Key rules

- 修改前先讀 authority source，確認正確落點再改
- 不做任務範圍外的 incidental refactor
- 未經 evidence 確認的事項不得宣稱完成
- archive 不作現行 authority
- specification-by-example 與 test-scenarios 分工不可混淆
- 核心規則要變時，先更新 decision-log，不直接改實作

## Implementation areas

- Backend: `services/quality-inspection-service/` — Java + Spring Boot
- Frontend: `frontends/quality-inspection-frontend/` — Angular (microfrontend)

兩個 area 目前均有 README.md 與 CLAUDE.md，但 source tree 僅含 .gitkeep placeholder。
Tech stack 已確認（Java/Spring Boot、Angular/microfrontend），build/test/lint/ci gates 尚未確認。

## Build / test / lint

目前無已確認的 build、test、lint 指令。待工程骨架建立後更新。
