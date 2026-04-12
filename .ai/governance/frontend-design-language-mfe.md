# HIS51 Frontend MFE Design Language

> **Status:** R1 — Active design language spec  
> **Applies to:** All HIS51 frontend MFE modules (micro-frontend feature modules)  
> **Primary audience:** Coding agents, reviewers, MFE engineers  
> **Scope:** Design language, page composition, and component patterns for MFE production pages

---

## How to Use This Document

| If you need to…                                  | Start at                           |
|--------------------------------------------------|------------------------------------|
| Understand MFE page structure and archetypes     | §2 Page Archetype Rules            |
| Learn container and block composition            | §3 Container and Block Composition |
| Know patient context placement rules             | §4 Patient Context                 |
| Distinguish workspace tabs from in-page tabs     | §5 Tabs                            |
| Apply table styling and variants                 | §6 Tables and Dense Data           |
| Implement empty states correctly                 | §7 Empty States                    |
| Choose loading patterns                          | §8 Loading States                  |
| Understand modal boundaries with shell           | §9 Modal Boundaries                |
| Use CSS variables and design tokens              | §10 Component CSS and Tokens       |

---

## 1. Purpose

### 1.1 Document Purpose

This document defines the **design language and composition rules** for all HIS51 frontend MFE modules. It establishes:

- **Page archetypes** — what layout patterns MFE pages MUST use
- **Container and block composition** — the canonical page root structure
- **Patient context placement** — where and when patient information renders
- **Tab distinctions** — workspace tabs vs. in-page content tabs
- **Table styling** — required classes and approved variants
- **Empty state patterns** — canonical no-data rendering
- **Loading patterns** — shell vs. local loading strategies
- **Modal boundaries** — when to use shell APIs vs. internal modals
- **CSS constraints** — design tokens and color variable usage

### 1.2 Relationship to Other Governance Documents

This design language spec works in conjunction with:

- **`frontend-mfe-governance.md`** — defines MUST/MUST NOT execution rules and shell boundaries
- **`DESIGN.v2.1.md`** — core design authority for the broader HIS51 design system
- **`DESIGN-components.v2.1.md`** — component specifications for the template baseline

When rules appear to conflict, the precedence is:

1. `frontend-mfe-governance.md` (MFE execution governance)
2. `frontend-design-language-mfe.md` (this document — MFE design language)
3. `DESIGN.v2.1` family (broader design authority)

---

## 2. Page Archetype Rules

### 2.1 System Page Language (Production MFE)

MFE production pages MUST use the **System Page** archetype, not the Template Page pattern.

**Why:**  
The Template Page pattern (`article-type="FullNav"`, position-fixed header, BrandNav-area sidebar, panel-based content) is used in the static template for documentation and demos. MFE pages running in a shell-hosted environment render inside the shell's content outlet and MUST NOT recreate the shell frame.

**Canonical pattern:**
```html
<system-container container-type="main">
  <system-block block-type="header" position-sticky="top">
    <!-- Pinned context, patient info, actions -->
  </system-block>
  <system-block block-type="main">
    <!-- Scrollable work area -->
  </system-block>
</system-container>
```

**Not approved:**
```html
<!-- PROHIBITED: Do not recreate shell frame -->
<div class="workspace">
  <header class="workspace-header">…</header>
  <aside class="workspace-sidebar">…</aside>
  <main class="workspace-main">…</main>
</div>
```

### 2.2 Breadcrumbs in Tabbed Shell MFE Pages

Breadcrumbs MUST be omitted in MFE pages running inside the tabbed shell.

**Why:**  
The shell workspace provides tab-based navigation. Breadcrumbs inside the content area create redundant navigation and confuse the information architecture. Users navigate between MFE modules via shell workspace tabs, not in-page breadcrumbs.

**Applies when:**  
The MFE page is loaded via the shell router and rendered inside the shell's `<router-outlet>`.

**Exception:**  
Standalone startup/development mode MAY render breadcrumbs if the MFE provides its own application frame outside the shell.

---

## 3. Container and Block Composition

### 3.1 Canonical Page Root: `system-container` + `system-block`

**Governance requirement:**  
MFE pages MUST use `system-container[container-type]` with nested `system-block[block-type]` as the page root structure (see **GOV-BND-001** in `frontend-mfe-governance.md`).

**Available composition patterns:**

**Container types:**

| `container-type`        | Use when                                                                 |
|-------------------------|-------------------------------------------------------------------------|
| `main`                  | Default single-column page layout (no sidebar)                          |
| `aside-start-collapse`  | Page with a collapsible working sidebar on the start (left) side        |

**Block types:**

| `block-type` | Semantic role                                           | Position behavior               |
|--------------|---------------------------------------------------------|--------------------------------|
| `header`     | Pinned context, patient info, filters, primary actions  | Use `position-sticky="top"`    |
| `main`       | Main scrollable work area                               | Scrollable content region      |

### 3.2 Canonical Starter Example

A minimal patient-facing MFE page with pinned patient context:

```html
<system-container container-type="main">
  <system-block block-type="header" position-sticky="top">
    <patient-info-container>
      <!-- Angular component renders the canonical .personal-info-container pattern: -->
      <div class="personal-info-container">
        <div class="personal-name">
          <h1>王小明</h1>
        </div>
        <div class="personal-info-main">
          <dl class="dl-list">
            <dt>病歷號</dt><dd>A123456789</dd>
            <dt>性別</dt><dd>男</dd>
            <dt>年齡</dt><dd>45歲</dd>
          </dl>
        </div>
      </div>
    </patient-info-container>
  </system-block>
  
  <system-block block-type="main">
    <div class="panel panel-white p-3">
      <h5>主訴</h5>
      <p>胸悶、呼吸困難三日</p>
    </div>
  </system-block>
</system-container>
```

### 3.3 Sidebar Layout Example

A page with a collapsible working sidebar (e.g., specimen status board with station filter):

```html
<system-container container-type="aside-start-collapse" [class.collapsed]="sidebarCollapsed">
  <system-block block-type="header" position-sticky="top">
    <div class="d-flex justify-content-between align-items-center">
      <h5>檢體狀態監控</h5>
      <button class="btn btn-sm btn-secondary" (click)="toggleSidebar()">
        <i class="mdi mdi-menu"></i>
      </button>
    </div>
  </system-block>
  
  <aside class="system-aside">
    <div class="panel panel-white p-3">
      <!-- Sidebar content: filters, navigation, tools -->
    </div>
  </aside>
  
  <system-block block-type="main">
    <div class="panel panel-white p-3">
      <!-- Main content: data grid, dashboard -->
    </div>
  </system-block>
</system-container>
```

### 3.4 Panel Padding Standard

Use `p-3` for panel interior padding in system pages.

**Why:**  
System pages use compact padding (`p-3`) for clinical density. Template pages (documentation/demos) use `py-3`.

**Example:**
```html
<system-block block-type="main">
  <div class="panel panel-white p-3">
    <!-- Clinical work area content -->
  </div>
</system-block>
```

---

## 4. Patient Context

### 4.1 Patient Context Ownership

Patient-facing MFE modules MUST render patient context in `system-block[block-type="header"]`.

**Why:**  
The shell does not render patient context for MFE pages. The shell provides workspace tabs, global navigation, and user session context. Each patient-facing MFE module is responsible for rendering its own patient context header.

**Required placement:**  
Patient context MUST appear in `system-block[block-type="header"]` with `position-sticky="top"` so it remains visible during scroll.

**Component contract:**  
The `<patient-info-container>` component MUST render the canonical `.personal-info-container` pattern defined in `DESIGN-components.v2.1.md` §15. The rendered HTML MUST include `.personal-info-container` as the top-level CSS class, `.personal-name` for the name section, and `.personal-info-main` with `.dl-list` for demographics.

**Example:**
```html
<system-container container-type="main">
  <system-block block-type="header" position-sticky="top">
    <patient-info-container>
      <!-- Component MUST render the canonical pattern: -->
      <div class="personal-info-container">
        <div class="personal-name">
          <h1>王小明</h1>
        </div>
        <div class="personal-info-main">
          <dl class="dl-list">
            <dt>病歷號</dt><dd>A123456789</dd>
            <dt>性別</dt><dd>男</dd>
            <dt>年齡</dt><dd>45歲</dd>
          </dl>
        </div>
      </div>
    </patient-info-container>
  </system-block>
  
  <system-block block-type="main">
    <!-- Patient data work area -->
  </system-block>
</system-container>
```

### 4.2 Non-Patient-Facing Modules

Modules that do not work with patient data (e.g., system administration, configuration modules) MAY omit patient context.

**Example: Non-patient module header**
```html
<system-container container-type="main">
  <system-block block-type="header" position-sticky="top">
    <div class="d-flex justify-content-between align-items-center">
      <h5>系統參數設定</h5>
      <button class="btn btn-primary">儲存變更</button>
    </div>
  </system-block>
  
  <system-block block-type="main">
    <!-- Configuration content -->
  </system-block>
</system-container>
```

---

## 5. Tabs

### 5.1 Workspace Tabs vs. In-Page Content Tabs

**Workspace tabs** are shell-owned and represent distinct MFE modules or major application sections loaded via the shell router.

**In-page content tabs** are module-owned and represent different views or data facets within a single MFE page.

| Tab Type         | Owned by | Pattern                  | Example                                     |
|------------------|----------|--------------------------|---------------------------------------------|
| Workspace tabs   | Shell    | Shell tab bar            | 掛號 · 批價 · 檢驗 · 報告 (shell workspace)  |
| In-page tabs     | Module   | `nav-tabs-scoped`        | 基本資料 · 過敏史 · 病歷摘要 (customer detail page) |

### 5.2 In-Page Content Tabs: `nav-tabs-scoped`

In-page content tabs MUST use `nav-tabs-scoped`.

**Why:**  
`nav-tabs-scoped` prevents style bleeding between in-page tabs and shell workspace tabs. It is the MFE-safe variant of `nav-tabs-custom`.

**Example:**
```html
<system-block block-type="main">
  <div class="panel panel-white p-3">
    <ul class="nav nav-tabs nav-tabs-scoped" role="tablist">
      <li class="nav-item">
        <button class="nav-link active" data-bs-toggle="tab" data-bs-target="#tab-basic">基本資料</button>
      </li>
      <li class="nav-item">
        <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-allergy">過敏史</button>
      </li>
      <li class="nav-item">
        <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-summary">病歷摘要</button>
      </li>
    </ul>
    
    <div class="tab-content">
      <div class="tab-pane fade show active" id="tab-basic">
        <!-- Basic info content -->
      </div>
      <div class="tab-pane fade" id="tab-allergy">
        <!-- Allergy history content -->
      </div>
      <div class="tab-pane fade" id="tab-summary">
        <!-- Medical summary content -->
      </div>
    </div>
  </div>
</system-block>
```

### 5.3 Workspace Tab Navigation

Workspace tab navigation is managed by the shell router. MFE modules MUST NOT create custom workspace-level tab bars.

---

## 6. Tables and Dense Data

### 6.1 Standard Clinical Tables: `table-custom`

Standard clinical tables SHOULD include `table-custom`.

**Why:**  
`table-custom` provides the base HIS51 table styling consistent with the design system. It establishes borders, spacing, and alignment for clinical data tables.

**Additional modifiers for clinical density:**
- `table-sm` — compact row height
- `table-borderless` — remove internal borders
- `table-hover-border` — highlight row on hover
- `table-striped` — zebra striping

**Example:**
```html
<table class="table table-custom table-sm table-borderless table-hover-border">
  <thead>
    <tr>
      <th>檢驗項目</th>
      <th>結果</th>
      <th>參考值</th>
      <th>單位</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>WBC</td>
      <td>8.5</td>
      <td>4.0-10.0</td>
      <td>10³/μL</td>
    </tr>
    <tr>
      <td>RBC</td>
      <td>4.2</td>
      <td>4.5-5.5</td>
      <td>10⁶/μL</td>
    </tr>
  </tbody>
</table>
```

### 6.2 Domain-Specific Table Variants

Domain-specific table variants such as `table-grid`, `table-booking`, or specialized tree tables MAY omit `table-custom` only when they provide their own complete base styling.

**Why:**  
Some domain-specific table patterns (e.g., calendar grids, specimen flow boards, hierarchical trees) require fundamentally different layout models that conflict with `table-custom` base styles.

**Approved domain-specific variants:**
- `table-grid` — grid-based layout tables (schedule, calendar)
- `table-booking` — booking/reservation calendar tables
- `table-tree` — hierarchical tree tables with expand/collapse

**When to use:**  
Only when the table pattern is semantically distinct from a standard data table and `table-custom` base styles prevent the required layout behavior.

**Example: Calendar booking table**
```html
<table class="table table-booking">
  <thead>
    <tr>
      <th>時間</th>
      <th>診間A</th>
      <th>診間B</th>
      <th>診間C</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>09:00</td>
      <td class="slot available"></td>
      <td class="slot booked">王小明</td>
      <td class="slot available"></td>
    </tr>
  </tbody>
</table>
```

### 6.3 Table Variants to Avoid

Do not use `table-outline`, `table-shadow`, or `table-custom-rounded` — these classes have no CSS definitions in the current design system.

---

## 7. Empty States

### 7.1 Canonical Empty State Pattern

The canonical empty state uses the HIS51 empty illustration pattern with a short message.

**Required elements:**
- `Empty.svg` or equivalent illustration asset
- `flex-center text-center` layout
- `py-3` vertical padding
- Short, helpful message

**Example:**
```html
<div class="flex-center text-center py-3" *ngIf="results.length === 0">
  <div>
    <img src="assets/img/Empty.svg" alt="無資料" class="empty-illustration">
    <p class="text-muted mt-2">目前沒有檢驗報告</p>
  </div>
</div>
```

### 7.2 When to Use Empty States

Use the canonical empty state pattern when a no-data condition is a **primary page outcome**, not an error or edge case.

**Appropriate use cases:**
- Search returns no results
- Patient has no allergy history
- No pending appointments for the selected date
- Report list is empty

**Not appropriate:**
- Data loading failed (use error state)
- User lacks permission (use permission-denied message)
- Temporary loading state (use skeleton/loading pattern)

### 7.3 Prohibited: Ad-Hoc Alert-Only Empty States

Do not use ad-hoc alert-only empty states when a no-data state is a primary page outcome.

**Not approved:**
```html
<!-- PROHIBITED: Text-only alert for primary empty state -->
<div class="alert alert-gray text-center border-dashed" *ngIf="results.length === 0">
  暫無資料
</div>
```

**Why:**  
Alert-based empty states lack visual consistency and do not provide the illustration-based affordance that helps users understand the empty state is expected and normal.

**Exception:**  
Small component-scoped empty states (e.g., optional metadata lists, secondary detail sections) MAY use minimal text-only rendering if the context makes the empty state immediately obvious.

---

## 8. Loading States

### 8.1 Shell Loading vs. Local Loading

Use **shell loading** for cross-page or blocking operations.

Use **local skeleton/loading patterns** for component-scoped fetches and dense-content refresh.

| Loading Type   | When to use                                         | Pattern                          |
|----------------|-----------------------------------------------------|----------------------------------|
| Shell loading  | Cross-page navigation, MFE module initialization    | Shell-provided blocking overlay  |
| Local loading  | Component-scoped data fetch, table refresh, in-page update | Skeleton or spinner in component template |

### 8.2 Shell Loading

The shell provides a blocking loading overlay for route transitions and MFE initialization.

MFE modules SHOULD rely on shell loading for:
- Initial page load
- Navigation between MFE modules
- Full-page data refresh that blocks user interaction

**How to trigger:**  
Shell loading is typically triggered by Angular router navigation or shell-provided loading facade APIs.

### 8.3 Local Skeleton/Loading Patterns

Use local skeleton or spinner patterns for component-scoped data fetches.

**Skeleton pattern:**  
Skeleton templates mirror the target layout to reduce visual jarring during load.

**Example: Table skeleton**
```html
<table class="table table-custom table-sm" *ngIf="loading">
  <tbody>
    <tr *ngFor="let i of [1,2,3,4,5]">
      <td><div class="skeleton-bar"></div></td>
      <td><div class="skeleton-bar"></div></td>
      <td><div class="skeleton-bar"></div></td>
    </tr>
  </tbody>
</table>

<table class="table table-custom table-sm" *ngIf="!loading && results.length > 0">
  <!-- Actual data rows -->
</table>
```

**Spinner pattern:**  
Use Bootstrap spinner for small component-scoped loading indicators.

**Example: Inline spinner**
```html
<button class="btn btn-primary" [disabled]="saving">
  <span class="spinner-border spinner-border-sm" *ngIf="saving"></span>
  儲存
</button>
```

### 8.4 Loading Pattern Selection

| Scenario                                  | Recommended pattern       |
|-------------------------------------------|---------------------------|
| Initial page load                         | Shell loading             |
| Full-page data refresh                    | Shell loading             |
| Table data refresh                        | Table skeleton            |
| Form submission                           | Button spinner            |
| Secondary panel update                    | Panel skeleton or spinner |
| Autocomplete search                       | Input spinner             |

---

## 9. Modal Boundaries

### 9.1 Shell-Facing Confirm and Alert Flows

Shell-facing confirm and alert flows MUST use shell facade APIs.

**Why:**  
The shell provides standard confirm, alert, and notification APIs that maintain consistent UX across all MFE modules and integrate with shell theming and accessibility features.

**Use shell APIs for:**
- Global confirm dialogs (e.g., "確定要刪除此記錄？")
- Global alert/notification messages
- Success/error toast notifications
- Blocking confirmation flows that affect cross-module state

**Example (pseudo-code):**
```typescript
// Use shell-provided confirmation API
this.shellService.confirm({
  title: '確認刪除',
  message: '確定要刪除此病歷記錄？此操作無法復原。',
  confirmText: '刪除',
  cancelText: '取消'
}).subscribe(confirmed => {
  if (confirmed) {
    this.deleteRecord();
  }
});
```

### 9.2 Module-Internal Pickers, Forms, and Rich Dialogs

Module-internal pickers, forms, and rich dialogs MAY use Bootstrap modal or Angular CDK dialog.

**Why:**  
Complex domain-specific dialogs with rich form controls, multi-step workflows, or specialized pickers do not require shell coordination. These are module-internal UI that do not affect cross-module state.

**Appropriate use cases:**
- Advanced search/filter dialogs
- Date/time pickers
- Multi-step wizards
- Record detail forms
- Specimen collection dialogs
- Report parameter configuration

**Example: Bootstrap modal for advanced filter**
```html
<div class="modal fade" id="advancedFilterModal" tabindex="-1">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title">進階篩選</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <!-- Complex filter form -->
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
        <button type="button" class="btn btn-primary" (click)="applyFilters()">套用</button>
      </div>
    </div>
  </div>
</div>
```

### 9.3 Testing Module-Internal Modals

When using module-internal modals, test modal rendering in the hosted shell environment to ensure proper layering and theming.

---

## 10. Component CSS and Tokens

### 10.1 CSS Variables and Design Tokens Required

**Governance requirement:**  
Component-scoped CSS is PROHIBITED from hardcoding hex colors (see **GOV-CSS-001** in `frontend-mfe-governance.md`).

**Safe styling patterns:**  
Use CSS custom properties (`var(--bs-primary)`, etc.) or Bootstrap utility classes.

**Why:**  
Hardcoded hex colors undermine theme switching and break visual consistency when the user changes themes (沉穩藍 / 陽光橘 / 玫瑰紫).

**Approved CSS variable patterns:**
```css
/* ✅ APPROVED: Use CSS variables */
.custom-header {
  background-color: var(--bs-secondary);
  border-bottom: 1px solid var(--bs-border-color);
  color: var(--bs-body-color);
}

.status-active {
  background-color: var(--bs-success);
}

.highlight-row {
  background-color: var(--bs-tertiary);
}
```

**Prohibited patterns:**
```css
/* ❌ PROHIBITED: Hardcoded hex colors */
.custom-header {
  background-color: #2a536e;
  border-bottom: 1px solid #dfe9ef;
  color: #535665;
}

.status-active {
  background-color: #00f7de;
}

.highlight-row {
  background-color: #dfe9ef;
}
```

### 10.2 Available CSS Variables

The HIS51 design system provides CSS variables for common semantic colors:

| Variable                | Purpose                          |
|-------------------------|----------------------------------|
| `--bs-primary`          | Primary brand color              |
| `--bs-secondary`        | Secondary brand color (dark blue in default theme) |
| `--bs-tertiary`         | Tertiary brand color (light gray-blue) |
| `--bs-success`          | Success state                    |
| `--bs-danger`           | Danger/error state               |
| `--bs-warning`          | Warning state                    |
| `--bs-info`             | Info state                       |
| `--bs-light`            | Light background                 |
| `--bs-dark`             | Dark text/background             |
| `--bs-body-color`       | Body text color                  |
| `--bs-border-color`     | Standard border color            |
| `--bs-white`            | White                            |

### 10.3 Semantic Color Note: `--bs-secondary` vs. `--bs-tertiary`

**Important:** In HIS51_Template, `--bs-secondary` = `#2A536E` (dark blue), NOT the light gray-blue that legacy code may expect.

Use `--bs-tertiary` for the light gray-blue (`#dfe9ef`) that earlier versions called "secondary".

**Example:**
```css
/* ✅ Correct */
.active-tab-indicator {
  background-color: var(--bs-tertiary); /* Light gray-blue */
}

.header-background {
  background-color: var(--bs-secondary); /* Dark blue */
}
```

### 10.4 Utility-First CSS Preferred

Prefer Bootstrap utility classes over custom CSS when possible.

**Example:**
```html
<!-- ✅ Preferred: Use utilities -->
<div class="bg-light border-bottom p-3">
  <h5 class="text-primary mb-0">檢驗項目</h5>
</div>

<!-- ⚠️ Less preferred: Custom CSS -->
<div class="custom-section-header">
  <h5>檢驗項目</h5>
</div>
```

**When to write custom CSS:**
- Complex layouts not achievable with utilities
- Domain-specific component states
- Reusable component patterns

**When writing custom CSS, always use CSS variables for colors.**

---

## Appendix: Quick Reference

### Canonical Page Structure

See **§3.2 Canonical Starter Example** for the full patient-facing MFE page template.

### Standard Table

```html
<table class="table table-custom table-sm table-borderless table-hover-border">
  <thead>…</thead>
  <tbody>…</tbody>
</table>
```

### In-Page Tabs

```html
<ul class="nav nav-tabs nav-tabs-scoped" role="tablist">
  <li class="nav-item">
    <button class="nav-link active" data-bs-toggle="tab">…</button>
  </li>
</ul>
<div class="tab-content">…</div>
```

### Empty State

```html
<div class="flex-center text-center py-3">
  <div>
    <img src="assets/img/Empty.svg" alt="無資料">
    <p class="text-muted mt-2">目前沒有資料</p>
  </div>
</div>
```

### CSS Variables

```css
background-color: var(--bs-secondary);
color: var(--bs-body-color);
border: 1px solid var(--bs-border-color);
```

---

**Document end.**
