---
applyTo: "frontends/quality-inspection-frontend/**"
---
# Frontend Instructions — quality-inspection-frontend

> Path-specific supplement to `.github/copilot-instructions.md`.
> For full area guide and commands, see `frontends/quality-inspection-frontend/CLAUDE.md`.

---

## Authority Reading Order (Frontend / UI Changes)

Before any frontend change, read in order:

1. `docs/01-requirements/decision-log.md`
2. `docs/02-ddd/permission-matrix.md`
3. `docs/02-ddd/state-machine.md`
4. `docs/03-contracts/api-spec.md`
5. `docs/05-ui/ui-information-architecture.md`
6. `docs/05-ui/ui-page-inventory.md`
7. Relevant `docs/05-ui/ui-page-spec-*.md` for the page being changed

---

## Two Operating Modes

| Mode | Purpose | Shell | Note |
|---|---|---|---|
| **standalone** | Local dev without MFE shell | Simulated user context | Development substitute only |
| **integrated** | Production target | Organization existing MFE shell | Formal architecture |

**Standalone mode is a shell-layer substitute** — it is not the production architecture replacement.
`portal51-lib@0.0.16` usage, routing, and state apply in both modes.

---

## API Calls

- Always use `/api/quality-inspection/**` as the API base path
- In development, `proxy.conf.json` routes requests (gateway-layer substitute, must simulate `api-key` header)
- Shell provides login and user context in integrated mode; standalone mode must simulate user context
- **Never hard-code direct backend URLs** as production contracts

---

## Frontend Layer Placement

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

---

## UI Baseline

- `portal51-lib@0.0.16` is the **shared UI library and wrapper/style priority reference**
- Component-level usage details (wrapper pattern, design token mechanism) are **pending confirmation**
- Do NOT invent component details or design tokens beyond what is confirmed in the repo
- If precise component usage is needed, ask the user for existing UI component sample files

---

## Role-Based Visibility

Frontend must correctly represent (do not invent rules):

- `HOSPITAL_MANAGER` — full answer details for own hospital
- `SYSTEM_ADMIN` — metadata only
- `AUDITOR` — authorized district / scope only
- Initial review revision — original reviewer only
- Second review revision — second reviewer only
- `CLOSED` / `CANCELLED` — revision not allowed
