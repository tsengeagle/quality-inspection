# CLAUDE.md — Backend Implementation Area

本文件是 Claude Code 在 `services/quality-inspection-service/` 的 path-specific instructions。

## 必讀文件

開始 backend 工作前，依序讀取：

1. `.ai/rules/repo-hard-rules.md` — repository-wide 硬規則
2. `AGENTS.md` — repo map、docs index、inspection order
3. 本目錄 `README.md` — backend area guide（責任、分層、落點規則）

## Authority chain

Backend 實作必須以以下 authority chain 為準（衝突時靠前者優先）：

1. `docs/01-requirements/decision-log.md`
2. `docs/01-requirements/0_品質審查系統規格草案.md`
3. `docs/01-requirements/glossary.md`
4. `docs/02-ddd/domain-model.md`
5. `docs/02-ddd/state-machine.md`
6. `docs/02-ddd/permission-matrix.md`
7. `docs/02-ddd/application-service.md`
8. `docs/02-ddd/domain-events.md`
9. `docs/03-contracts/api-spec.md`
10. `docs/03-contracts/persistence-model.md`
11. `docs/04-testing/specification-by-example.md`
12. `docs/04-testing/test-scenarios.md`

`docs/05-ui/*` 可作 query shape / command timing 參考，非 backend 第一 authority。

## 預期分層

```
interfaces/      # controller / DTO / error mapping / auth entry
application/     # use case orchestration / command handler / transaction boundary
domain/          # aggregate / entity / value object / policy / domain service
infrastructure/  # repository impl / persistence mapping / external adapter / config
reporting/       # read model / query projection / report support
```

## 落點規則

- Domain invariant / state guard / revision rule -> `domain/`
- Use case orchestration / actor guard -> `application/`
- HTTP endpoint / request-response mapping -> `interfaces/`
- DB mapping / external integration -> `infrastructure/`
- 不得在 controller 中硬寫 domain rule
- 不得在 repository 中偷做業務決策
- 不得讓 UI page spec 反向決定 aggregate invariant

## Build / test / lint

Backend tech stack 已確認為 Java 17 + Spring Boot 2.7.18 + Gradle 7.6.4 (Groovy DSL)。
Integration baseline 為 organization existing api-gateway。
Formal API base path: `/api/quality-inspection/**`，required auth header: `api-key`。
開發期可使用 local dev proxy 模擬 api-gateway（須模擬 api-key header），upstream service 不足時可採 adapter-level stub。

Commands:
- Build: `.\gradlew.bat build`
- Test: `.\gradlew.bat test`
- Targeted test: `.\gradlew.bat test --tests com.tzuchi.his.quality.inspection.interfaces.rest.ReviewCaseApiTest`
- Run: `.\gradlew.bat bootRun`

Backend terminal execution rules:

- backend Gradle work must execute from `services/quality-inspection-service`, not repo root and not the frontend module root
- before running a command, state repo root, target backend module root, current working directory, and the exact command
- pre-check `build.gradle`, `settings.gradle`, and `gradlew.bat`; for targeted test also confirm the target class exists under `src/test/java`
- prefer module-root wrapper commands first; do not fall back to bare `gradle` while `gradlew.bat` exists
- keep build / full test / targeted test / bootRun in the same backend module root

PowerShell canonical pattern:

```powershell
$RepoRoot = git rev-parse --show-toplevel
$BackendRoot = Join-Path $RepoRoot 'services\quality-inspection-service'
Set-Location $BackendRoot
.\gradlew.bat test --tests com.tzuchi.his.quality.inspection.interfaces.rest.ReviewCaseApiTest
```

Package root: `com.tzuchi.his.quality.inspection`
Layers: domain, application, interfaces, infrastructure, reporting

Lint / formatter / static analysis 尚未設定。
