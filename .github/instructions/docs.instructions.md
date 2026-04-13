---
applyTo: "docs/**"
---
# Docs Authority Instructions — quality-inspection

> Path-specific supplement to `.github/copilot-instructions.md`.
> For full placement rules and escalation guide, see `AGENTS.md` §7–§10.

---

## Placement Rules

When modifying docs, use the correct file for each type of change:

| Change type | Target file |
|---|---|
| System requirements | `docs/01-requirements/0_品質審查系統規格草案.md` |
| Accepted decisions | `docs/01-requirements/decision-log.md` |
| Terminology / semantics | `docs/01-requirements/glossary.md` |
| Aggregate / invariant / entity | `docs/02-ddd/domain-model.md` |
| State transitions / terminal states | `docs/02-ddd/state-machine.md` |
| Role permissions / visibility / scope | `docs/02-ddd/permission-matrix.md` |
| Application orchestration / use cases | `docs/02-ddd/application-service.md` |
| Domain events | `docs/02-ddd/domain-events.md` |
| HTTP API contract | `docs/03-contracts/api-spec.md` |
| DB / persistence mapping | `docs/03-contracts/persistence-model.md` |
| Concrete examples / acceptance rules | `docs/04-testing/specification-by-example.md` |
| Test scenarios | `docs/04-testing/test-scenarios.md` |
| UI page behavior / screen spec | `docs/05-ui/ui-page-spec-*.md` |
| Frontend design method / implementation bridge | `docs/06-frontend/*.md` |
| Wireframe / prototype / design execution artifact | `docs/07-design/*.md` |

**`docs/99-archive/` is NOT current authority** — historical reference only. If it conflicts with current docs, current docs win.

---

## Escalation: Stop and Update decision-log.md First

Before changing any rule that affects:

- Quality threshold
- Terminal states (`CLOSED`, `CANCELLED`)
- Revision policy (append-only, no overwrite)
- Sampling duplicate scope

…update `docs/01-requirements/decision-log.md` first. Then sync downstream docs.

---

## Do NOT

- Duplicate authority across multiple docs files
- Write permission rules into domain invariants (use `permission-matrix.md`)
- Write state machine rules only in API spec (use `state-machine.md` as the primary source)
- Let UI page spec drive aggregate invariant decisions
- Treat archive as current authority
- Treat markdown frame specs / ASCII / Mermaid as Figma artifacts or executable prototypes
