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
8. Relevant `docs/06-frontend/*.md` when the task touches frontend bridge, wireframe, or prototype rules
9. Relevant `docs/07-design/*.md` when the task touches wireframe, prototype, or design execution artifacts

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
- In development, `proxy.conf.js` routes requests (gateway-layer substitute, with env-driven `api-key` generation)
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

## Prototype / Wireframe Execution

- General wireframe / prototype tasks do not require prior user approval.
- Prefer free or local solutions first; Figma is not the default path.
- If a task requires Figma / Figma MCP / paid capabilities, or hits quota / permission / API / MCP / plan limits, stop and report before proceeding.
- If Figma MCP is used, try free API / free capabilities first.
- Markdown wireframes, frame specs, ASCII, and Mermaid are documentation artifacts only; they do not prove a Figma artifact exists.
- If Figma is not feasible, prefer a local React / HTML low-fidelity prototype.

---

## Access Control Strategy（D-029 / D-030）

- **Route Guard**（`guards/`）：page-level gate（RoleGuard / ScopeGuard / ProjectionGuard）控制「能不能進這頁」。目前為 PASSTHROUGH；actor context provider 完成注入後正式化。
- **Directive**：component / element-level gate，控制頁面內 UI 元素的可見性與操作啟用。
- 兩者共用同一份 actor context 來源（`userId / userName / roles / hospitalId`），不得各自維護獨立 role truth。
- **RBAC 為正式 role 來源**：新碼不得新增 hardcoded role 字串比對；所有新 access check 一律走 RBAC-backed guard 或 directive。
- **Legacy grandfathering**：`case-list-page` / `batch-list-page` 現有 hardcoded role 暫時允許；被修改時必須同步改接 RBAC-backed 機制。
- 詳見 `docs/01-requirements/decision-log.md` D-029 / D-030

---

## Role-Based Visibility

Frontend must correctly represent (do not invent rules):

- `HOSPITAL_MANAGER` — full answer details for own hospital
- `SYSTEM_ADMIN` — metadata only
- `AUDITOR` — authorized district / scope only
- Initial review revision — original reviewer only
- Second review revision — second reviewer only
- `CLOSED` / `CANCELLED` — revision not allowed
