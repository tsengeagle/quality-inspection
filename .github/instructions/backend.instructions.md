---
applyTo: "services/quality-inspection-service/**"
---
# Backend Instructions — quality-inspection-service

> Path-specific supplement to `.github/copilot-instructions.md`.
> For full area guide and build commands, see `services/quality-inspection-service/CLAUDE.md`.

---

## Authority Reading Order (Backend Changes)

Before any backend change, read in order:

1. `docs/01-requirements/decision-log.md`
2. `docs/02-ddd/domain-model.md`
3. `docs/02-ddd/state-machine.md`
4. `docs/02-ddd/permission-matrix.md`
5. `docs/02-ddd/application-service.md`
6. `docs/02-ddd/domain-events.md`
7. `docs/03-contracts/api-spec.md`
8. `docs/03-contracts/persistence-model.md`
9. `docs/04-testing/specification-by-example.md`
10. `docs/04-testing/test-scenarios.md`

---

## Package Root & Layer Placement

- **Package root**: `com.tzuchi.his.quality.inspection`
- `domain/` — aggregates, entities, value objects, repository interfaces, domain invariants
- `application/` — use case orchestration, command handlers, transaction boundary
- `interfaces/` — REST controllers, DTOs, error mapping
- `infrastructure/` — JPA entities, repository impls, external adapters, config
- `reporting/` — read models, query projections, report support

---

## Currently Implemented Aggregates (Do Not Re-Implement)

| Aggregate | Package | Stage |
|---|---|---|
| `ReviewProject` | `domain.project` | B1 ✅ |
| `ScoreSheetDefinition` | `domain.scoresheet` | B1 ✅ |
| `SamplingBatch` | `domain.sampling` | B2 ✅ |
| `ReviewCase` | `domain.reviewcase` | B2 ✅ (minimal) |

**Implemented use cases (B1):** `CreateReviewProject`, `PublishReviewProjectVersion`, `CreateScoreSheetDraft`, `PublishScoreSheetVersion`

**Implemented use cases (B2):** `PreviewSamplingPopulation`, `ExecuteSampling`, `RetrySamplingBatch`

---

## Key Invariants Already in Code (Never Violate)

- Published `ProjectVersion` is **immutable** — no edit after `publishNewVersion()`
- Published `ScoreSheetDefinition` is **immutable** — no section/item change after `publishVersion()`
- `SamplingBatch` once `COMPLETED` is immutable
- Same `sourceCaseKey` cannot appear twice in the **same** batch (duplicate guard is per-batch, not cross-batch)
- **Retry** creates a NEW `SamplingBatch` with `retryOfBatchId` set — never overwrites the failed batch
- Terminal states `CLOSED` / `CANCELLED` — no revision allowed after reaching these
- Revision is **append-only** — never overwrites old `ReviewSubmission`

---

## Not Yet Implemented (Next Stages)

- B3 — Assignment and Initial Review (`ReviewSubmission`, assign/review commands)
- B4+ — Complex Review / Audit and Reporting

Do not extend into these stages without a separate work order.

---

## Terminal Execution Governance

For backend terminal work in this multi-module repo, use these rules directly:

1. Before running a backend command, identify and state:
	- repo root
	- target backend module root: `services\quality-inspection-service`
	- current working directory
	- exact command to run
2. Do not guess relative paths from memory. Resolve the repo root first, then change explicitly into the backend module root before build, test, targeted test, or bootRun commands.
3. PowerShell examples must use module-root execution, for example:
	```powershell
	$RepoRoot = git rev-parse --show-toplevel
	$BackendRoot = Join-Path $RepoRoot 'services\quality-inspection-service'
	Set-Location $BackendRoot
	.\gradlew.bat test --tests com.tzuchi.his.quality.inspection.interfaces.rest.ReviewCaseApiTest
	```
4. Required pre-checks before running backend commands:
	- `build.gradle`
	- `settings.gradle`
	- `gradlew.bat`
	- `src\main\resources\application.yml` when the command depends on Spring Boot runtime config
	- target test class under `src\test\java` when running a targeted test
5. Command priority for backend Gradle work:
	- first: module-root wrapper command such as `.\gradlew.bat <task>` on Windows
	- second: `./gradlew <task>` when the shell is not PowerShell but still starts from the same module root
	- last resort: system `gradle`, only when the wrapper is absent and the repo explicitly allows it
6. Preferred execution order inside the backend module:
	- build
	- full test
	- targeted test when narrowing failures or validating a focused area
	- bootRun for local app execution
7. Prohibited patterns:
	- running backend Gradle commands from repo root
	- running backend Gradle commands from `frontends\quality-inspection-frontend`
	- bare `gradle test` when `gradlew.bat` exists
	- mixing backend build in one directory with backend test or bootRun in another directory
	- assuming a fully qualified test class name is runnable when the current working directory is not the backend module root
8. Backend command baseline in this repo:
	- build: `.\gradlew.bat build`
	- full test: `.\gradlew.bat test`
	- targeted test: `.\gradlew.bat test --tests com.tzuchi.his.quality.inspection.interfaces.rest.ReviewCaseApiTest`
	- app run: `.\gradlew.bat bootRun`

Treat backend execution as `partial` rather than `ready` when the wrapper or module-local Gradle files have not been confirmed in the target working directory.
