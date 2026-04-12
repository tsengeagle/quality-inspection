# HIS51 Frontend MFE Governance

> **Status:** R1 — Active governance spec  
> **Applies to:** All HIS51 frontend MFE modules (micro-frontend feature modules)  
> **Primary audience:** Coding agents, reviewers, MFE engineers  
> **Scope:** Execution governance for hosted and standalone MFE development

---

## How to Use This Document

| If you need to…                                  | Start at                      |
|--------------------------------------------------|-------------------------------|
| Understand what MFE modules MUST and MUST NOT do | §3 MFE Boundary Rules         |
| Learn how to communicate with shell              | §4 Shell Communication Rules  |
| Know user context requirements                   | §5 User Context and Standalone Mode |
| Understand CSS and asset ownership               | §6 Styling and Asset Ownership |
| Check agent behavior on rule conflicts           | §9 Agent Execution Notes      |

---

## 1. Purpose and Authority

### 1.1 Purpose

This document defines **normative governance rules** for all HIS51 frontend MFE modules. It establishes:

- **Boundary rules** — what MFE modules MUST render, what they MUST NOT create
- **Shell communication contracts** — how modules delegate to shell services
- **User context contracts** — how modules consume and adapt shared context
- **CSS and asset ownership** — what assets MFEs inherit vs. what they own
- **Prohibited patterns** — what implementations violate the MFE contract

### 1.2 Authority

This governance document takes precedence for **MFE implementation decisions**:

1. **`frontend-mfe-governance.md`** (this document)
2. `frontend-design-language-mfe.md`
3. `DESIGN.v2.1.md`, `DESIGN-components.v2.1.md`, `DESIGN-engineering.v2.1.md`

The existing `DESIGN.v2.1` family remains the broader design authority, but this governance package supersedes it for MFE-specific concerns until rules are fully back-merged.

### 1.3 Applies To

All HIS51 frontend MFE modules, including:

- Feature modules loaded via Module Federation
- Standalone startup/development mode instances
- MFE pages rendering inside the tabbed shell
- Module-local components and services

---

## 2. Rule Strengths

Each governance rule uses one of four normative strength levels:

| Strength     | Meaning                                                                 |
|--------------|-------------------------------------------------------------------------|
| **MUST**     | Required. Non-negotiable compliance. Violation is a defect.            |
| **SHOULD**   | Recommended. Deviation requires justification in review.               |
| **MAY**      | Optional. Choose based on domain needs.                                |
| **PROHIBITED** | Forbidden. Implementation violates the MFE contract or shell boundary. |

---

## 3. MFE Boundary Rules

### 3.1 Content Outlet and Page Root Structure

**GOV-BND-001**  
**Strength:** MUST

**Rule:**  
MFE pages MUST render inside the shell-provided content outlet and MUST use `system-container[container-type]` with nested `system-block[block-type]` as the page root structure.

**Applies when:**  
Creating any page-level component that renders in hosted (shell-embedded) mode.

**Why:**  
The shell provides the outer frame (header, sidebar, tabs, footer). MFE modules are responsible only for the content area rendered inside the `<router-outlet>` within the `.portal-container > .tab-pane` mount point.

**Approved example:**
```html
<system-container container-type="main">
  <system-block block-type="header" position-sticky="top">
    <patient-info-container>…</patient-info-container>
  </system-block>
  <system-block block-type="main">
    <div class="panel panel-white p-3">…</div>
  </system-block>
</system-container>
```

**Not approved:**
```html
<!-- PROHIBITED: MFE creating its own header/sidebar/footer -->
<div class="workspace">
  <header class="workspace-header">…</header>
  <aside class="workspace-sidebar">…</aside>
  <main class="workspace-content">…</main>
</div>
```

**Agent note:**  
If a user requests a full-page layout with header and sidebar, explain that the shell owns the workspace frame and the MFE must render only the content area using `system-container`.

---

### 3.2 Shell Chrome Ownership

**GOV-BND-002**  
**Strength:** PROHIBITED

**Rule:**  
MFE modules are PROHIBITED from creating shell chrome components such as workspace header, global sidebar, footer, or account panel.

**Applies when:**  
Any time an MFE is tempted to create outer frame UI.

**Why:**  
Shell owns the workspace frame. Duplicating these components creates rendering conflicts and breaks shell-managed user context, theme, and tab navigation.

**Not approved:**
```html
<!-- PROHIBITED: MFE-owned global header -->
<div class="global-header">
  <div class="logo">HIS51</div>
  <div class="account-menu">…</div>
</div>
```

**Agent note:**  
If a user requests "add a header with logo and account menu," stop and explain that the shell provides the workspace header and account panel. The MFE should use `system-block[block-type="header"]` for module-specific headers (like patient context banners).

---

### 3.3 Duplicate Runtime Assets

**GOV-BND-003**  
**Strength:** PROHIBITED

**Rule:**  
MFE modules are PROHIBITED from importing duplicate copies of shell-owned runtime assets such as Bootstrap, jQuery, UI.css, or theme CSS.

**Applies when:**  
Setting up module dependencies, configuring `angular.json` or `webpack.config.js`.

**Why:**  
Shell injects these assets globally. Loading duplicates causes version conflicts, increases bundle size, and can break shared state (e.g., jQuery event delegation, Bootstrap modal z-index).

**Approved example:**
```js
// webpack.config.js — shared singleton dependencies
shared: {
  "@angular/core": { singleton: true, strictVersion: true },
  "@angular/common": { singleton: true, strictVersion: true },
  "@ngrx/store": { singleton: true, strictVersion: true },
  // Shell provides Bootstrap, jQuery, UI.css globally
}
```

**Not approved:**
```json
// angular.json — PROHIBITED duplicate CSS import
{
  "styles": [
    "src/styles.scss",
    "node_modules/bootstrap/dist/css/bootstrap.min.css"  // ❌ Shell already loads this
  ]
}
```

**Agent note:**  
If a user requests "install Bootstrap 5," explain that the shell already provides Bootstrap 5.2.3 globally and the MFE should use shell-injected classes. Do not add Bootstrap to `package.json` or `angular.json`.

---

## 4. Shell Communication Rules

### 4.1 Shell Service Facade

**GOV-SHELL-001**  
**Strength:** MUST

**Rule:**  
Shell-facing behavior (loading overlay, toast, alert, tab open/close) MUST go through `EventBusService` or another explicitly approved shell facade.

**Applies when:**  
MFE code needs to trigger shell-managed UI or navigation.

**Why:**  
`EventBusService` is the canonical shell contract. Direct PubSub or store access bypasses the service boundary and creates tight coupling to shell internals.

**Approved example:**
```typescript
// Correct: use EventBusService for shell actions
import { EventBusService } from 'portal51-lib';

constructor(private eventBus: EventBusService) {}

showLoading() {
  this.eventBus.showLoading();
}

openCrossModuleTab() {
  this.eventBus.openTab('reservation', 'Booking', '/reservation/booking/12345');
}
```

**Not approved:**
```typescript
// PROHIBITED: direct PubSub against shell topics
import PubSub from 'pubsub-js';
PubSub.publish('TOPIC_SHOW_LOADING', { show: true });  // ❌ bypasses EventBusService
```

**Agent note:**  
If a user requests "show a loading spinner," use `EventBusService.showLoading()`. Do not call `PubSub.publish('TOPIC_SHOW_LOADING')` directly.

---

### 4.2 Direct PubSub on Shell Topics

**GOV-SHELL-002**  
**Strength:** PROHIBITED

**Rule:**  
MFE modules are PROHIBITED from calling `PubSub.subscribe` or `PubSub.publish` directly against shell-managed topics (`TOPIC_*` constants from `portal51-lib`).

**Applies when:**  
MFE code wants to listen for or trigger shell events.

**Why:**  
Direct PubSub bypasses the `EventBusService` facade and creates fragile coupling to shell implementation details.

**Approved example:**
```typescript
// Correct: use EventBusService subscription methods
this.eventBus.getUserInfo().subscribe(userInfo => {
  console.log('User context updated:', userInfo);
});
```

**Not approved:**
```typescript
// PROHIBITED: direct PubSub subscription
import PubSub from 'pubsub-js';
import { TOPIC_USER_INFO } from 'portal51-lib';

PubSub.subscribe(TOPIC_USER_INFO, (msg, data) => {  // ❌
  console.log('User context:', data);
});
```

**Agent note:**  
If a user requests "listen for user context changes," use `EventBusService.getUserInfo()` observable. Do not subscribe to `TOPIC_USER_INFO` via PubSub.

---

### 4.3 Shell Store Access from Feature Code

**GOV-SHELL-003**  
**Strength:** PROHIBITED

**Rule:**  
MFE feature code is PROHIBITED from dispatching shell UI behavior through shell store internals (e.g., `@ngrx/store` actions for loading/toast/tabs).

**Applies when:**  
MFE code needs to trigger shell-managed state changes.

**Why:**  
Shell store is an internal implementation detail. Feature code must use `EventBusService`, which abstracts the underlying store mechanism.

**Not approved:**
```typescript
// PROHIBITED: direct store dispatch for shell actions
import { Store } from '@ngrx/store';
import { showLoadingAction } from 'shell-store/actions';  // ❌ internal shell action

constructor(private store: Store) {}

this.store.dispatch(showLoadingAction({ show: true }));
```

**Approved example:**
```typescript
// Use EventBusService instead of store internals
import { EventBusService } from 'portal51-lib';

constructor(private eventBus: EventBusService) {}

this.eventBus.showLoading(true);
this.eventBus.showToast('success', 'Operation completed');
this.eventBus.switchTab('tab-key');
```

**Agent note:**  
If a user requests "dispatch a loading action," explain that shell actions are internal. Use `EventBusService.showLoading()` instead.

---

## 5. User Context and Standalone Mode

### 5.1 User Context Consumption Boundary

**GOV-CTX-001**  
**Strength:** MUST

**Rule:**  
Feature code MUST consume user context (user, role, hospital, permissions) through a module-side context adapter or service boundary. Direct shell store selectors or global variable access is not allowed.

**Applies when:**  
Feature components or services need user/role/hospital information.

**Why:**  
A module-side boundary allows the context source to vary between hosted mode (shell-provided via `EventBusService`) and standalone mode (local mock or dev service).

**Approved example:**
```typescript
// user-context.service.ts — module-side adapter
@Injectable({ providedIn: 'root' })
export class UserContextService {
  constructor(private eventBus: EventBusService) {}

  getUserInfo(): Observable<UserInfo> {
    // In hosted mode: delegates to shell
    // In standalone mode: can provide mock/local impl
    return this.eventBus.getUserInfo();
  }
}

// feature.component.ts
constructor(private userContext: UserContextService) {}

ngOnInit() {
  this.userContext.getUserInfo().subscribe(user => {
    console.log('Current user:', user);
  });
}
```

**Not approved:**
```typescript
// PROHIBITED: direct shell store selector
import { Store, select } from '@ngrx/store';
import { selectUserInfo } from 'shell-store/selectors';  // ❌ shell internal

constructor(private store: Store) {}

this.store.pipe(select(selectUserInfo)).subscribe(user => {
  console.log('User:', user);
});
```

**Agent note:**  
If a user requests "get the current user," create or use a module-side `UserContextService` that wraps `EventBusService.getUserInfo()`. Do not access shell store directly.

---

### 5.2 Standalone Mode Compatibility

**GOV-CTX-002**  
**Strength:** MUST

**Rule:**  
The module-side context boundary MUST remain compatible with both hosted mode (shell-provided context) and standalone mode (local dev server, mock context).

**Applies when:**  
Designing module-side services for user context, navigation, or shell actions.

**Why:**  
MFE modules should be runnable in standalone mode for local development and testing without requiring the full shell.

**Approved example:**
```typescript
// user-context.service.ts — environment-aware
@Injectable({ providedIn: 'root' })
export class UserContextService {
  constructor(
    private eventBus: EventBusService,
    private env: EnvironmentService
  ) {}

  getUserInfo(): Observable<UserInfo> {
    if (this.env.isStandalone) {
      // Standalone mode: return mock user
      return of({
        userId: 'DEV001',
        userName: 'Dev User',
        hospitalId: 'H001',
        roleId: 'DOCTOR'
      });
    }
    // Hosted mode: delegate to shell
    return this.eventBus.getUserInfo();
  }
}
```

**Agent note:**  
If a user requests "support running this module standalone," ensure context services check `environment.standalone` and provide mock implementations when not hosted.

---

## 6. Styling and Asset Ownership

### 6.1 Component CSS Color Hardcoding

**GOV-CSS-001**  
**Strength:** PROHIBITED

**Rule:**  
Component-scoped CSS is PROHIBITED from hardcoding hex colors. Use CSS custom properties (`var(--bs-primary)`, etc.) or Bootstrap utility classes.

**Applies when:**  
Writing component-specific `.scss` or `.css` files.

**Why:**  
Shell manages theme switching (3 theme variants: 沉穩藍/陽光橘/玫瑰紫). Hardcoded colors break theme compatibility.

**Approved example:**
```scss
// feature.component.scss — correct
.feature-banner {
  background-color: var(--bs-primary);
  color: var(--bs-white);
  border: 1px solid var(--bs-border-color);
}
```

**Not approved:**
```scss
// PROHIBITED: hardcoded hex colors
.feature-banner {
  background-color: #2A536E;  // ❌ breaks theme switching
  color: #ffffff;
  border: 1px solid #dee2e6;
}
```

**Agent note:**  
If a user requests a specific color (e.g., "make the banner blue"), use `bg-primary` utility class or `var(--bs-primary)`. Do not hardcode `#2A536E`.

---

### 6.2 Shell-Global vs. Module-Local CSS

**GOV-CSS-002**  
**Strength:** MUST

**Rule:**  
Shell-global CSS (UI.css, theme CSS, Bootstrap) and module-local CSS MUST have clear ownership and MUST NOT be double-loaded.

**Applies when:**  
Configuring `angular.json` styles array or importing CSS in components.

**Why:**  
Double-loading causes CSS precedence conflicts and increases bundle size. Shell provides global baseline; modules add only component-specific styles.

**Approved ownership:**

| Asset Category          | Owner    | Loaded By    |
|-------------------------|----------|--------------|
| UI.css                  | Shell    | `angular.json` scripts (shell) |
| Theme CSS (3 variants)  | Shell    | `#ThemeCSS` link injected by shell |
| Bootstrap 5.2.3         | Shell    | `angular.json` scripts (shell) |
| Module feature styles   | Module   | Component `styleUrls` or module `styles.scss` |

**Not approved:**
```json
// angular.json — PROHIBITED
{
  "styles": [
    "src/styles.scss",
    "../../shared/UI.css"  // ❌ Shell already provides this
  ]
}
```

**Agent note:**  
If a user requests "the modal isn't styled," verify that the shell is running and injecting UI.css. Do not copy UI.css into the module.

---

## 7. Canonical Module Patterns

The following patterns are required or approved for MFE modules to ensure correct integration with the shell and compliance with DESIGN baseline standards.

### 7.1 Patient Context in System Block Header

**Pattern:** Patient-facing modules MUST render patient context (patient name, chart number, visit info) in `system-block[block-type="header"]` with `position-sticky="top"`.

**Rationale:** Patient context must remain visible during scrolling for clinical safety.

**Applies to:** Modules like reservation, flow-control, health-report.

**Component contract:**  
Angular patient context components (e.g., `<patient-info-container>`) MUST render the canonical `.personal-info-container` pattern defined in `DESIGN-components.v2.1.md` §15. The component wrapper tag may vary by framework, but the rendered HTML MUST include `.personal-info-container` as the top-level CSS class.

**Example:**
```html
<system-container container-type="main">
  <system-block block-type="header" position-sticky="top">
    <patient-info-container>
      <!-- Angular component MUST render: -->
      <div class="personal-info-container">
        <div class="personal-name">
          <h1>王小明</h1>
        </div>
        <div class="personal-info-main">
          <dl class="dl-list">
            <dt>病歷號</dt><dd>A123456789</dd>
          </dl>
        </div>
      </div>
    </patient-info-container>
  </system-block>
  <system-block block-type="main">…</system-block>
</system-container>
```

---

### 7.2 In-Page Tabs with nav-tabs-scoped

**Pattern:** MFE pages MUST use `nav-tabs-scoped` class for in-page content tabs within the module boundary.

**Rationale:** `nav-tabs-custom` is reserved for shell tab navigation. `nav-tabs-scoped` signals module-local tabbed content that does not trigger shell tab behavior.

**Applies to:** Multi-view feature pages (e.g., health report with exam/medication/history tabs).

**Example:**
```html
<ul class="nav nav-tabs nav-tabs-scoped" role="tablist">
  <li class="nav-item">
    <a class="nav-link active" data-bs-toggle="tab" href="#exam">檢驗</a>
  </li>
  <li class="nav-item">
    <a class="nav-link" data-bs-toggle="tab" href="#medication">用藥</a>
  </li>
</ul>
<div class="tab-content">
  <div class="tab-pane fade show active" id="exam">…</div>
  <div class="tab-pane fade" id="medication">…</div>
</div>
```

---

## 8. Prohibited Patterns

The following patterns violate the MFE governance contract and MUST NOT be implemented:

| Pattern | Rule | Why Prohibited |
|---------|------|----------------|
| **Creating workspace header/sidebar/footer** | GOV-BND-002 | Shell owns workspace chrome |
| **Importing duplicate Bootstrap/jQuery/UI.css** | GOV-BND-003 | Causes version conflicts and bundle bloat |
| **Direct `PubSub.publish/subscribe` on shell topics** | GOV-SHELL-002 | Bypasses `EventBusService` facade |
| **Direct shell store dispatch from feature code** | GOV-SHELL-003 | Couples to shell store internals |
| **Hardcoding hex colors in component CSS** | GOV-CSS-001 | Breaks theme switching |
| **Accessing shell store selectors directly** | GOV-CTX-001 | Bypasses module-side context boundary |

---

## 9. Agent Execution Notes

### 9.1 Rule Conflict Handling

**When a user request conflicts with a MUST or PROHIBITED rule:**

1. **Stop before implementing.**
2. **Explain the conflict** — reference the specific rule ID (e.g., GOV-BND-002) and why the request violates the MFE contract.
3. **Propose the canonical alternative** — suggest the approved pattern that achieves the user's goal within the governance boundary.
4. **Do not work around governance** — do not implement forbidden patterns "temporarily" or with disclaimers.

**Example:**
```
User: "Add a global header with logo and account menu to the reservation module."

Agent response:
"That request conflicts with GOV-BND-002, which prohibits MFE modules from creating 
shell chrome (workspace header, sidebar, footer, account panel). The shell already 
provides these components.

If you need a module-specific header (e.g., patient context banner), use:
  <system-block block-type=\"header\" position-sticky=\"top\">

If you need to customize the shell header, that change must be made in the shell 
repository, not the MFE module."
```

---

### 9.2 Choosing Among Allowed Options

**When multiple allowed options exist:**

1. **Choose the most canonical option first** — existing HIS51 MFE pattern observed in THMS reference implementation.
2. **Apply §7 Canonical Module Patterns** — use the matching pattern there before considering anything custom.
3. **Propose new patterns only if explicitly requested** — and only if not forbidden by `frontend-design-language-mfe.md`.

**Example:**
```
User: "Create a tabbed interface for exam results."

Agent reasoning:
1. Check if in-page tabs are allowed → Yes, GOV canonical pattern §7.2
2. Use nav-tabs-scoped (not nav-tabs-custom, which is for shell tabs)
3. Implement Bootstrap tab-content pattern with tab-pane fade
```

---

### 9.3 Verification Before Claiming Compliance

**Before claiming a feature is "done" or "compliant":**

1. Verify the page uses `system-container` + `system-block` (GOV-BND-001)
2. Verify no shell chrome duplication (GOV-BND-002)
3. Verify no duplicate asset imports in `angular.json` or `webpack.config.js` (GOV-BND-003)
4. Verify shell actions go through `EventBusService`, not direct PubSub (GOV-SHELL-001, GOV-SHELL-002)
5. Verify no direct shell store dispatch from feature code (GOV-SHELL-003)
6. Verify component CSS uses CSS variables, not hex colors (GOV-CSS-001)
7. Verify shell-global CSS and module-local CSS are not double-loaded (GOV-CSS-002)
8. Verify user context is consumed through module-side adapter, not direct shell selectors (GOV-CTX-001)
9. Verify module-side context boundary supports both hosted and standalone modes (GOV-CTX-002)
10. Run the feature in both hosted mode and standalone mode to confirm no runtime errors

---

## 10. References

- **Design baseline:** `DESIGN.v2.1.md`, `DESIGN-components.v2.1.md`, `DESIGN-engineering.v2.1.md`
- **Design language (MFE):** `frontend-design-language-mfe.md`
- **Decision log:** `frontend-mfe-decisions-r1.md`
- **Reference implementation:** THMS-frontend modules (customer, reservation, flow-control, health-report)
- **Shell contract baseline:** Module Federation + `EventBusService` + `portal51-lib`

---

**Document version:** R1  
**Last updated:** 2026-04-12
