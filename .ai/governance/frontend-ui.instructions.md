# frontend-ui.instructions.md

## 1. Scope

Use this file as the **frontend MFE operational entrypoint** under the aligned `.ai/*` governance model.

This file is short on purpose. It does **not** replace the deeper authority files.

## 2. Reading Order

For frontend MFE work under the aligned governance structure:

1. Start from `repo-governance-instruction.md`.
2. Then read `frontend-ui.instructions.md` (this operational entrypoint).
3. If the task touches shell boundary, shell contract, standalone mode, or asset ownership, read `frontend-mfe-governance.md`.
4. If the task touches page composition, patient context, tabs, tables, empty states, loading, modal boundaries, or styling, read `frontend-design-language-mfe.md`.
5. If you need rationale for an R1 rule or override, read `frontend-mfe-decisions-r1.md`.
6. If you need broader baseline or migration context, follow the broader design references listed in `authority-sources.md`.

## 3. Authority Linkage

Use the following authority model:

- `frontend-ui.instructions.md` = operational entrypoint
- `frontend-mfe-governance.md` = execution governance authority
- `frontend-design-language-mfe.md` = MFE design language authority
- `frontend-mfe-decisions-r1.md` = formal decision log
- `authority-sources.md` = authority index for broader baseline references

Do not treat this file as a replacement for the deeper authority files.

## 4. Task Classification

Before making changes, classify the task:

| Task type | Read next |
|---|---|
| Shell-facing interaction, workspace ownership, standalone support, runtime assets | `frontend-mfe-governance.md` |
| New page, page layout, patient context, tabs, tables, empty states, loading, modal choice, styling | `frontend-design-language-mfe.md` |
| Wireframe, low-fidelity prototype, design execution, flow validation artifact | `../../docs/06-frontend/frontend-design-method.md`, `../../docs/06-frontend/frontend-design-roadmap.md`, `../../docs/07-design/ui-wireframe-brief.md`, `../../docs/07-design/wireframes/README.md`, `../../.ai/rules/repo-hard-rules.md` |
| Need rule rationale or R1 override background | `frontend-mfe-decisions-r1.md` |
| Need broader baseline or migration context | `authority-sources.md` |

If broader baseline and MFE-specific authority appear to disagree for an MFE task, follow the MFE-specific authority and mark the broader-vs-MFE mismatch as cleanup or pending confirmation.

## 5. Terminal Execution Governance

For frontend terminal work in this multi-module repo, use these rules directly:

1. Before running a command, identify and state:
   - repo root
   - target frontend module root: `frontends\quality-inspection-frontend`
   - current working directory
   - exact command to run
2. Do not guess relative paths from memory. Resolve the repo root first, then change explicitly into the frontend module root before install, typecheck, unit, or e2e commands.
3. PowerShell examples must use module-root execution, for example:
   ```powershell
   $RepoRoot = git rev-parse --show-toplevel
   $FrontendRoot = Join-Path $RepoRoot 'frontends\quality-inspection-frontend'
   Set-Location $FrontendRoot
   npm run verify:frontend
   ```
4. Required pre-checks before running frontend commands:
   - `package.json`
   - `angular.json`
   - `tsconfig.json`
   - `node_modules` when the command depends on local binaries
   - `tsconfig.spec.json` + `karma.conf.js` for unit test commands
   - `playwright.config.ts` + `e2e\src` + at least one Playwright-discoverable test file for e2e commands
5. Command priority for Node / TypeScript work:
   - first: `npm run <script>`
   - second: module-root local binary such as `.\node_modules\.bin\playwright.cmd`
   - last resort: `npx`, only when no safe script or local binary path exists
6. Angular CLI commands in the same frontend module must run serially. Do not start a second `ng build` or `ng test` process while another Angular CLI process is still compiling or holding `.ngcc_lock_file`.
7. Preferred execution order inside the frontend module:
   - install
   - typecheck
   - unit
   - e2e
8. Prohibited patterns:
   - running frontend commands from repo root or backend module root
   - bare `npx tsc`
   - bare `npx playwright test` when a repo-local npm script exists
   - parallel Angular CLI execution inside the same module root
   - mixing install in one directory with typecheck / unit / e2e in another directory
   - treating the repo-root `package-lock.json` as the frontend module lockfile
   - treating e2e config presence alone as proof that e2e is executable
9. Frontend command baseline in this repo:
   - install: `npm install`
   - typecheck: `npm run typecheck`
   - unit: `npm run test:unit` or `npm run test:unit:ci`
   - e2e: `npm run test:e2e`
   - minimal verify: `npm run verify:frontend`

If e2e discovery does not find any tests, classify e2e as `partial`, not `ready`, even when the script and config files exist.

## 6. Reuse-First Rules

Before changing code:

1. Reuse the existing HIS51 MFE pattern before inventing a new one.
2. Reuse shell facade paths before touching shell internals.
3. Reuse canonical page composition before introducing custom wrappers.
4. Reuse approved component patterns before inventing ad-hoc UI states.

Prefer existing patterns for:
- `system-container + system-block`
- patient context in header block
- `nav-tabs-scoped`
- shell facade usage via `EventBusService`
- illustration-based empty states
- token-safe styling

## 7. Page Composition Rules

Use these rules directly:

1. New production MFE pages start from `system-container[container-type]` + `system-block[block-type]`.
2. Patient-facing pages must include patient context in the header block.
3. Angular patient context wrappers must render the canonical `.personal-info-container` pattern.
4. Breadcrumbs are omitted in hosted tabbed-shell mode.
5. In-page tabs use `nav-tabs-scoped`.
6. Standard clinical tables use `table-custom`.
7. Domain-specific table variants may diverge only when they provide complete base styling.
8. Primary empty states use the illustration-based empty-state pattern, not alert-only placeholders.

## 8. Shell Communication Rules

Use these rules directly:

1. Shell-facing interactions must go through `EventBusService` or another approved shell facade.
2. Raw `PubSub.subscribe/publish(TOPIC_*)` is prohibited.
3. Do not dispatch shell UI behavior through shell store internals from feature code.
4. Do not create shell chrome inside the MFE.
5. Do not duplicate shell-global runtime assets.
6. Support hosted mode and standalone mode through a module-side boundary.

## 9. CSS and Styling Rules

Use these rules directly:

1. Do not hardcode hex colors in component CSS.
2. Use CSS variables, design tokens, or approved utility classes.
3. Keep shell-global CSS and module-local CSS ownership clear.
4. If you need the light gray-blue surface color, use `--bs-tertiary`, not legacy assumptions about `--bs-secondary`.

## 10. Validation Checklist

Before claiming completion:

1. Read the deeper authority files required by the task type.
2. Check the command was executed from `frontends\quality-inspection-frontend`, not repo root or another module root.
3. Check the command used an npm script before falling back to a local binary or `npx`.
4. Check required frontend config files existed before command execution.
5. Check e2e commands had at least one Playwright-discoverable test file before claiming e2e readiness.
6. Check page structure and page-root composition.
7. Check patient context placement when the page is patient-facing.
8. Check breadcrumbs omission for hosted tabbed-shell runtime.
9. Check in-page tabs use `nav-tabs-scoped`.
10. Check shell-facing interactions go through `EventBusService`.
11. Check no raw `PubSub` or shell store internals were introduced.
12. Check no duplicate shell-global assets were imported.
13. Check component CSS has no hardcoded hex colors.
14. Check hosted + standalone compatibility when the task touches runtime/bootstrap/context assumptions.

## 11. Escalation / Pending Confirmation Rules

Escalate or mark pending confirmation when:

1. The MFE-specific authority does not answer the task clearly.
2. A broader baseline reference and MFE-specific authority appear to conflict, and the MFE override is not explicit.
3. The task crosses shell ownership boundaries.
4. The task would require introducing a new pattern not already covered by authority.
5. You cannot determine whether a table is standard clinical or a justified domain-specific variant.
6. You cannot preserve hosted + standalone compatibility with confidence.

When escalating:
- state what authority files you checked
- state what rule is missing or conflicting
- state what needs confirmation
