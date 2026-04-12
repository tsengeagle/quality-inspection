# HIS51 Frontend MFE Decisions — R1

> **Status:** R1 — Active decision log  
> **Date:** 2026-04-12  
> **Applies to:** All HIS51 frontend MFE modules  
> **Purpose:** Record formal governance decisions with evidence and back-merge targets

---

## 1. Purpose

This document records the six core governance decisions formalized in R1 (Release 1) of the HIS51 Frontend MFE Governance Package. Each decision represents a formal resolution of ambiguity found during the THMS module scan and design alignment analysis.

> **Note on evidence citations:** Many decisions cite internal scan-analysis documents (`design-to-module-alignment-thms-r1.md`, `module-to-shell-contract-thms-r1.md`) that represent working analysis outputs from the R1 THMS module scan. These cited documents are **not committed to this repository**. They served as the evidentiary foundation during governance formalization but remain internal working artifacts. The evidence citations are preserved here to document the analytical provenance of each decision.

These decisions:

1. Establish **canonical MFE implementation defaults** for coding agents
2. Resolve **design baseline gaps** observed in existing modules
3. Define **back-merge targets** for updating the core DESIGN.v2.1 family
4. Preserve **evidence and rationale** for future governance evolution

This decision log works in conjunction with:

- **`frontend-mfe-governance.md`** — normative execution rules
- **`frontend-design-language-mfe.md`** — MFE design language spec
- **`DESIGN.v2.1` family** — core design authority

---

## 2. Decision Summary

| Decision ID | Title | Strength | Impact |
|-------------|-------|----------|--------|
| **DEC-001** | Breadcrumbs are omitted for tabbed-shell MFE pages | MUST | High — affects all MFE page composition |
| **DEC-002** | Patient context is owned by the module header block | MUST | High — clinical safety, page structure |
| **DEC-003** | Shell-facing behavior must use shell facades | MUST | High — enforces contract boundary |
| **DEC-004** | In-page tabs use `nav-tabs-scoped` | MUST | Medium — prevents style bleeding |
| **DEC-005** | User context must support hosted and standalone mode through a module-side boundary | MUST | High — preserves standalone development |
| **DEC-006** | Component CSS cannot hardcode hex colors | PROHIBITED | High — theme compliance |

---

## 3. Decision Details

### DEC-001: Breadcrumbs are omitted for tabbed-shell MFE pages

**Decision:**  
Breadcrumbs MUST be omitted in MFE pages running inside the tabbed shell. Breadcrumbs MAY appear only when the MFE is running in standalone mode or in a host that does not provide shell tabs.

**Reason:**  
The shell workspace provides tab-based navigation. Breadcrumbs inside the content area create redundant navigation and confuse the information architecture. Users navigate between MFE modules via shell workspace tabs, not in-page breadcrumbs.

**Evidence:**

- **THMS scan:** All 5 scanned modules (customer, reservation, flow-control, health-report, exam-calculator) omit breadcrumbs entirely
- **Design baseline gap:** `DESIGN.v2.1.md` mandates `.breadcrumbs` with `breadcrumbs-position="header"` but does not address whether breadcrumbs are needed, optional, or prohibited inside MFE pages running under a shell with workspace tabs
- **Source:** `design-to-module-alignment-thms-r1.md` §6.1, `module-to-shell-contract-thms-r1.md` gap analysis row 3

**Downstream rule impact:**

- **`frontend-design-language-mfe.md` §2.2** — formalizes breadcrumb omission policy
- **Agent guidance:** Do not generate breadcrumbs for MFE pages in hosted mode

**Exception:**  
Standalone startup/development mode MAY render breadcrumbs if the MFE provides its own application frame outside the shell.

---

### DEC-002: Patient context is owned by the module header block

**Decision:**  
Patient-facing MFE modules MUST render patient context in `system-block[block-type="header"]` with `position-sticky="top"`. The shell does not render patient context for MFE pages.

**Reason:**  
The shell provides workspace tabs, global navigation, and user session context. Each patient-facing MFE module is responsible for rendering its own patient context header. This keeps patient safety responsibility with the clinical feature module, not the infrastructure shell.

**Evidence:**

- **THMS scan:** customer and health-report modules both implement patient context locally (`app-customer-basic-info` in header block); flow-control uses `app-encounter-station-status-v2` pattern
- **Design baseline gap:** `DESIGN.v2.1.md` §Patient Context says `personal-info-container` MUST be in `block-type="header"` but does not clarify whether MFE modules or the shell should render it
- **Clinical safety implication:** If patient context is shell-owned but implementation is undocumented, future modules may omit it
- **Source:** `design-to-module-alignment-thms-r1.md` §6.3, `module-to-shell-contract-thms-r1.md` gap analysis row 2

**Downstream rule impact:**

- **`frontend-design-language-mfe.md` §4.1** — defines patient context ownership and placement
- **Agent guidance:** For patient-facing modules, always include patient context in header block

---

### DEC-003: Shell-facing behavior must use shell facades

**Decision:**  
Shell-facing behavior (loading overlay, toast, alert, tab open/close) MUST go through `EventBusService` or another explicitly approved shell facade. Direct `PubSub.publish/subscribe` on shell topics and direct shell store dispatch are PROHIBITED.

**Reason:**  
Shell facades provide version-stable contracts. Direct PubSub or store coupling creates fragile dependencies on shell internals and prevents independent module testing.

**Evidence:**

- **THMS scan:** customer and flow-control modules correctly use `EventBusService.publishEvent()`
- **THMS scan — violation:** health-report module bypasses `EventBusService` and uses `PubSub.subscribe()` directly for shell topics
- **Design alignment:** existing modules with facade usage show no runtime issues; direct PubSub creates undocumented coupling
- **Source:** `design-to-module-alignment-thms-r1.md` §7.2, `module-to-shell-contract-thms-r1.md` §3 Shell Communication

**Downstream rule impact:**

- **`frontend-mfe-governance.md` §4.1 (GOV-SHELL-001)** — requires EventBusService
- **`frontend-mfe-governance.md` §8 Prohibited Patterns** — lists PubSub bypass as forbidden
- **Agent guidance:** Always delegate shell actions through `EventBusService`; reject requests to use direct PubSub

---

### DEC-004: In-page tabs use `nav-tabs-scoped`

**Decision:**  
In-page content tabs MUST use `nav-tabs-scoped`. Shell workspace tabs use shell-native tab patterns. The two tab types MUST remain visually and semantically distinct.

**Reason:**  
`nav-tabs-scoped` prevents style bleeding between in-page tabs and shell workspace tabs. It is the MFE-safe variant of `nav-tabs-custom`.

**Evidence:**

- **THMS scan:** customer and health-report modules both use `nav-tabs-scoped` for in-page content tabs
- **Design baseline gap:** `DESIGN-components.v2.1.md` §Tabs documents `nav-tabs-custom` but does not mention `nav-tabs-scoped`
- **Functional correctness:** modules using `nav-tabs-scoped` show no style leakage; variant is working as intended
- **Source:** `design-to-module-alignment-thms-r1.md` §4.3, §6.4, `module-to-shell-contract-thms-r1.md` table row "In-page tab distinction"

**Downstream rule impact:**

- **`frontend-design-language-mfe.md` §5.2** — documents `nav-tabs-scoped` as canonical in-page tab class
- **Agent guidance:** Use `nav-tabs-scoped` for in-page tabs; do not use `nav-tabs-custom` in MFE module code

---

### DEC-005: User context must support hosted and standalone mode through a module-side boundary

**Decision:**  
User context consumption MUST go through a module-side adapter or service boundary that can be backed by either shell facade (hosted mode) or local/mocked provider (standalone mode). Feature code MUST NOT read shell store selectors directly.

**Reason:**  
Standalone mode is a first-class governance concern, not an exception. Modules must support both hosted runtime (full shell + shared store) and standalone startup (no shell, local providers) without changing feature component code. This keeps MFEs testable and reduces coupling to shell internals.

**Evidence:**

- **Governance requirement:** `frontend-mfe-governance.md` §5.2 (GOV-CTX-002) — standalone mode compatibility is a MUST requirement for module-side context boundaries
- **THMS scan — coupling risk:** flow-control module imports shared store directly from other modules (`import { SharedStoreService } from '@THMS/customer'`) — this creates cross-module coupling and breaks standalone mode
- **Source:** `design-to-module-alignment-thms-r1.md` §7.2

**Downstream rule impact:**

- **`frontend-mfe-governance.md` §5.1 (GOV-CTX-001)** — requires module-side context adapter
- **`frontend-mfe-governance.md` §5.2 (GOV-CTX-002)** — mandates standalone mode support
- **Agent guidance:** Consume user context through module-owned service boundary; do not import shell store selectors into feature components

---

### DEC-006: Component CSS cannot hardcode hex colors

**Decision:**  
Component-scoped CSS is PROHIBITED from hardcoding hex colors. All color references MUST use CSS custom properties (`var(--bs-primary)`, etc.) or Bootstrap utility classes.

**Reason:**  
Hardcoded colors break theme switching. The HIS51 design system supports three theme variants (沉穩藍/陽光橘/玫瑰紫) via CSS variable injection. Component CSS with hardcoded hex values will not update when the user switches themes.

**Evidence:**

- **THMS scan — violations:** customer module has 5 hardcoded colors (`#00f7de`, `#2a536e`, `#dfe9ef`, `#535665`, others); health-report has 3+ hardcoded colors across component CSS files
- **Design baseline:** `DESIGN.v2.1.md` §Theme mandates "NEVER hardcode colors" and requires CSS variable usage
- **Theme compliance risk:** hardcoded `#dfe9ef` (light gray-blue) duplicates `--bs-tertiary` and will break under orange/purple themes
- **Source:** `design-to-module-alignment-thms-r1.md` §4.5, §7.3, `module-to-shell-contract-thms-r1.md` table row "Theme switching"

**Downstream rule impact:**

- **`frontend-mfe-governance.md` §6.1 (GOV-CSS-001)** — prohibits hardcoded hex colors
- **`frontend-design-language-mfe.md` §10.1** — documents safe styling patterns
- **Agent guidance:** Never generate hardcoded hex values in component CSS; use `var(--bs-*)` or utility classes

---

## 4. Back-merge Targets

These decisions should be back-merged into the core `DESIGN.v2.1` family after R1 validation:

### `DESIGN.v2.1.md`

**Section updates:**

- **§Breadcrumbs** — add: "Breadcrumbs are OMITTED in MFE pages when the shell provides workspace tab navigation. If shell tabs are not available, breadcrumbs MUST follow baseline pattern."
- **§Patient Context** — clarify: "For patient-facing MFE modules, the patient context strip (`personal-info-container`) MUST be rendered by each module in `block-type="header"`. The shell does NOT provide it."
- **§Shell Communication** — add: "Shell-facing behavior MUST use `EventBusService` or another approved shell facade. Direct `PubSub` or shell store access is PROHIBITED."

**Reference:** DEC-001, DEC-002, DEC-003

---

### `DESIGN-components.v2.1.md`

**Section updates:**

- **§Tabs** — document: "`nav-tabs-scoped` is the canonical in-page tab class for MFE modules. Reserve `nav-tabs-custom` for shell workspace tabs."

**Reference:** DEC-004

---

### `DESIGN-engineering.v2.1.md`

**Section updates:**

- **§Standalone Mode** — add: "MFE modules MUST support both hosted (shell-embedded) and standalone (development/isolated) startup modes. User context and shell services MUST be consumed through module-side adapter boundaries."
- **§CSS Ownership** — add: "Component-scoped CSS MUST use CSS variables or Bootstrap utility classes. Hardcoded hex color values are PROHIBITED."
- **§Shell Facade Enforcement** — add: "All shell-facing actions MUST go through approved facades (`EventBusService`). Direct `PubSub` or shell store access is PROHIBITED."

**Reference:** DEC-005, DEC-006, DEC-003

---

**Document version:** R1  
**Last updated:** 2026-04-12  
**Next review:** After first production MFE deployment using R1 governance
