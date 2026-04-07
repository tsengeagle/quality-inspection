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
