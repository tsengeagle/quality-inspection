# HIS51 Frontend Template — Design Authority

> **Agent-first design reference.** Read this document for every frontend task.
> For component specs, see `DESIGN-components.v2.1.md`. For engineering/migration details, see `DESIGN-engineering.v2.1.md`.

---

## 1. Purpose & Scope

This document is the **design authority** for the HIS51 frontend template system. Agents and developers MUST consult it before any frontend work.

**Three-tier document structure:**

| Document | When to read |
|----------|-------------|
| `DESIGN.v2.1.md` | **Every task** — core design principles and canonical baseline |
| `DESIGN-components.v2.1.md` | When building or modifying a specific component |
| `DESIGN-engineering.v2.1.md` | When migrating A→B pages or implementing low-level patterns |

> **For MFE implementation work:** The frontend MFE governance package defines the execution baseline for shell interaction, standalone compatibility, and CSS ownership. See [frontend-mfe-governance.md](../../ai/governance/frontend-mfe-governance.md), [frontend-design-language-mfe.md](../../ai/governance/frontend-design-language-mfe.md), and [frontend-mfe-decisions-r1.md](../../ai/governance/frontend-mfe-decisions-r1.md).

**Non-goals:** This document is not a CSS token reference, not an engineering how-to, and not a component library catalog.

---

## 2. Design Intent

### 2.1 Visual Atmosphere

Professional, restrained, with subtle warmth. Deep blue (`#2A536E`) as primary conveys trust and reliability. The aesthetic is information-first — not cold or clinical, not decorative. Conservative healthcare: **trust over novelty.**

### 2.2 Product Context

HIS51 is a clinical HIS serving physicians and nurses in high-pressure, fast-paced environments at Tzu Chi (慈濟) hospitals in Taiwan. Every pixel MUST serve efficiency or reduce cognitive load.

Key constraints:
- **Location-aware** — pages carry facility context (hospital/unit)
- **Large interactive targets** — clinical staff use gloved hands and view screens at distance
- **Speed over beauty** — task completion time is the primary UX metric

### 2.3 Density Philosophy

**Dense by default; breathe at interaction boundaries.**

- System pages use tight spacing for maximum information scanning (`table-sm`, `table-borderless`, `gap: 0` where appropriate).
- Template pages are slightly looser for readability.
- Breathing room appears at key interaction points: modals, login, onboarding.
- Default font size: `data-layout-size="lg"` (1.125 rem).

### 2.4 Layout Rhythm

This section defines **how to make layout decisions without mockups.** Agents MUST follow these patterns when composing new pages.

#### Information Density

- System pages default to **dense**: maximize data visible per viewport. Clinical users scan, not read.
- Dense tables for list views (patient lists, schedules, lab results). Use `table-sm table-borderless`.
- Sectioned forms for data entry (intake, configuration). Use `row g-2` with labeled fields.

#### Interaction Boundary Breathing Room

- Breathing room appears at: modal/offcanvas transitions, login page, page-level section boundaries, and between pinned header and scrollable content.
- Within content blocks, density stays tight. Between content blocks, `p-3` or section gaps provide visual separation.

#### Summary vs. Detail Rhythm

- **Summary view** = table or definition list (`dl-list`). Compact, scan-optimized.
- **Detail view** = form or card layout. More generous spacing for editing.
- Pattern: summary list → click row → detail panel/modal. The transition from dense to spacious is deliberate.

#### Page-Level Hierarchy

- **Pinned patient context** (header block) → always visible, provides clinical anchor.
- **Primary content area** (main block) → scrollable, carries the workflow.
- **Side panels** (aside blocks) → supplementary context, collapsible.
- Hierarchy flows top-to-bottom and left-to-right.

#### When to Pin Context vs. Keep Inline

- **Pin:** patient identity, critical status flags, facility context — always needed for clinical safety.
- **Inline:** supplementary notes, historical data, reference information — scroll with content.
- Rule: if removing it from view could cause a clinical error, **PIN it.**

#### When to Prefer Dense Tables vs. Sectioned Forms

- **Dense table:** browsing many records, comparing across rows, time-pressured scanning.
- **Sectioned form:** editing a single record, capturing complex data, guided workflows.
- **Hybrid:** table summary with inline editing for quick corrections.

### 2.5 Theme Personalization

Three themes provide role-based or personal visual identity:

| Theme | Name | Role |
|-------|------|------|
| 沉穩藍 | Calm Blue | **Default** |
| 陽光橘 | Sunny Orange | Alternative |
| 玫瑰紫 | Rose Purple | Alternative |

Theme switching via `#ThemeCSS` / `UI-Theme.js` is an **optional capability**, not mandatory for all pages. New pages SHOULD preserve theme-switch support; never hardcode color values when CSS variables exist.

---

## 3. Canonical Baseline

**HIS51_Template** (Library B) is the canonical reference.

| Property | Value |
|----------|-------|
| CSS version | V5.2-230728 |
| UI modules | 38 `UI-*.js` files |
| Page coverage | 50+ real clinical system pages |
| Source format | SCSS source available |

**慈濟網頁元件_template** (Library A) is historical reference only. Existing pages using A patterns may remain, but new work MUST NOT introduce A-only patterns.

### Migration Warning (A → B)

`--bs-secondary` changed meaning between libraries:

| Variable | Library A | Library B |
|----------|-----------|-----------|
| `--bs-secondary` | `#dfe9ef` (light gray-blue, used for backgrounds) | `#2A536E` (dark blue, same as primary) |

**Rule:** When migrating A→B pages, replace `bg-secondary` with `bg-tertiary`.

Font rendering shifts **one size step larger** at the same `data-layout-size` value. Full migration details in `DESIGN-engineering.v2.1.md`.

---

## 4. Page Archetypes

### 4.1 System Page (production)

```html
<body class="overflow-hidden" data-drawer-element="nav-main">
  <header class="theme-secondary">              <!-- dark header -->
    <button class="hamburger-icon"></button>     <!-- drawer-btn, CSS-animated -->
    <div class="account-panel"><!-- avatar, role badge, font-size, language, theme, version, switch identity, logout --></div>
    <div class="offcanvas" id="Notification"><!-- notification panel --></div>
  </header>
  <div class="system-container" container-type="main">
    <system-block block-type="header">           <!-- pinned patient context -->
      <div class="personal-info-container"><!-- patient name, demographics, quick actions --></div>
    </system-block>
    <system-block block-type="main"><!-- scrollable content --></system-block>
  </div>
  <aside class="Nav-area theme-secondary NavExpand"><!-- sidebar nav + module selector dropdown --></aside>
</body>
```

Key traits: `body.overflow-hidden` for full-viewport containment. Header carries account panel with avatar, role badge, and offcanvas notifications. Patient context header (`.personal-info-container`) is pinned while the main block scrolls.

### 4.2 Template Page (demo / docs only)

```html
<body>
  <header class="theme-light position-fixed">   <!-- light sticky header -->
    <span class="Tzuchi-logo"></span>            <!-- brand -->
  </header>
  <article article-type="FullNav">
    <div class="BrandNav-area theme-secondary d-grid">
      <nav class="tree-wrap tree-toggle tree-title tree-icon">
        <ul class="nav-tree"><!-- hierarchical nav --></ul>
      </nav>
    </div>
    <section class="panel" toggle-parent>        <!-- collapsible panel -->
      <div class="panel-body" toggle-element><!-- content --></div>
    </section>
    <nav aria-label="breadcrumb"><!-- breadcrumbs --></nav>
  </article>
</body>
```

Key traits: `header.theme-light.position-fixed`, tree-based sidebar nav, panel-based content with toggle behavior, breadcrumbs.

### 4.3 Login Page

Standalone `Login.html` with institutional branding. Two-column layout: left column shows branding (logo, hospital name, mission: "守護生命　守護健康　守護愛"); right column presents credentials. Large form controls (`form-control-lg`) ensure clinical accessibility. A location dropdown provides facility-aware context.

### 4.4 When to Use Each

New production pages MUST use the **System Page** archetype. The Template Page exists for documentation and component demos only — never use it for real clinical workflows. The Login Page is the sole entry point; it is standalone and MUST NOT be embedded inside a page shell.

---

## 5. Visual Language

### 5.1 Color Roles

| Variable | Hex | Semantic Intent |
|----------|-----|-----------------|
| `--bs-primary` | `#2A536E` | Trustworthy, professional — primary actions, brand elements, header icons |
| `--bs-secondary` | `#2A536E` (in B) | Dark accent — nav sidebar, system page header backgrounds |
| `--bs-tertiary` | `#dfe9ef` | Light surface — subtle backgrounds, card surfaces. NOTE: was A's `--bs-secondary` |
| `--bs-light` | `#e9ebef` | Page background, content surface base |
| `--bs-dark` | `#292e35` | Text, high-contrast elements |

**State rule:** Light variants → accessible/hover states; dark variants → emphasis/active states.

MUST NOT hardcode color hex values. Always use CSS variables to preserve theme-switching support.

### 5.2 Typography Rules

- Base unit: **rem**. No `px` for sizing. MUST.
- 5-step scale via `data-layout-size` on `<html>`:

  | Step | Value |
  |------|-------|
  | `xs` | 0.75 rem |
  | `sm` | 0.875 rem |
  | `md` | 1 rem |
  | `lg` | 1.125 rem |
  | `xlg` | 1.25 rem |

- Default: `data-layout-size="lg"` (1.125 rem). Clinical pages may use `xlg` for accessibility.
- Font family: **Noto Sans TC** (Traditional Chinese). **EUDC-tzuchi** for medical custom characters (醫療造字).

### 5.3 Spacing & Density

- **System pages:** `p-3` panel padding (compact, scan-optimized).
- **Template pages:** `py-3` panel padding (more vertical breathing room).
- Grid gaps: `g-1` to `g-2` for form layouts. `g-` prefix classes provide grid-aware responsive spacing.
- Tables: `table-sm` + `table-borderless` for high-density clinical data. `table-custom` as base class.
- Content structure: definition lists (`dl-list`) for read-only data display; forms for editable data.

### 5.4 State Semantics

UI states and their visual expression. Agents MUST apply these consistently across all pages.

| State | CSS Class Pattern | Visual Role | When to Use |
|-------|------------------|-------------|-------------|
| Success | `bg-success`, `text-success` | Green — completed, approved, confirmed | Task completion, positive validation, approved status |
| Warning | `bg-warning`, `text-warning` | Yellow/amber — attention needed, pending | Pending actions, approaching limits, caution |
| Danger | `bg-danger`, `text-danger` | Red — error, critical, rejected | Validation errors, critical alerts, rejected status |
| Info | `bg-info`, `text-info` | Blue/cyan — informational, neutral notice | Informational messages, tips, neutral status updates |
| Hover | `:hover` pseudo-class, Bootstrap hover utilities | Subtle lightening or cursor change | Interactive elements responding to mouse proximity |
| Active | `.active` class | Highlighted, visually distinct from siblings | Currently selected tab, active nav item, pressed button |
| Disabled | `:disabled`, `.disabled`, `.opacity-50` | Dimmed, non-interactive | Unavailable actions, locked fields, inactive controls |
| Selected | `.active` on list items, `.selected` on tree nodes | Highlighted row or item | Currently focused record in a list, active tree node |

**Rules:**

1. State colors MUST use Bootstrap utility classes (`bg-success`, `text-danger`, etc.) — MUST NOT hardcode state color hex values.
2. Success/Warning/Danger/Info MUST be used consistently for their semantic meaning across all pages. Do not repurpose danger for non-error highlights.
3. Badges (`<span class="badge bg-*">`) are the standard way to display status indicators in tables and headers.
4. Disabled state SHOULD use both the `:disabled` attribute AND visual dimming (`.opacity-50` or Bootstrap's `.disabled` class).
5. Active vs. Selected: `.active` is Bootstrap's standard; use it for tabs, nav items, and list-group items. Tree nodes may use `.selected`.

### 5.5 Icon and Brand Usage

- **Icon library:** Material Design Icons (`mdi mdi-*`) exclusively. MUST NOT mix icon libraries.
- **Brand:** `Tzuchi-logo` icon font for institutional branding.
- **Deprecated:** `Medus.svg` (A-only) — MUST NOT use in new pages.
- **Common clinical icons:** `mdi-plus` (add), `mdi-content-save` (save), `mdi-delete` (delete), `mdi-bell` (notifications), `mdi-account-circle-outline` (user), `mdi-magnify` (search), `mdi-star` (bookmark).
- Icons within buttons MUST use `<i class="mdi mdi-*"></i>` inside the button element.
- Status/flag icons may combine icon + state color: e.g., `<i class="mdi mdi-timer-sand text-warning"></i>`.

---

## 6. Layout Principles

### 6.1 Page Shell

- `ArticleRows-wrap` is the top-level container.
- `data-drawer-element="nav-main"` binds drawer navigation.
- System pages: `body.overflow-hidden` (full-viewport containment).

### 6.2 Header

- **System pages:** `header.theme-secondary` (dark). Contains hamburger, brand logo, utility icons, account panel with offcanvas notification.
- **Template pages:** `header.theme-light.position-fixed` (light, sticky). Contains brand + layout-size / theme switcher.
- New production pages MUST use the system header.

### 6.3 Sidebar / Navigation

- **System pages:** `aside.Nav-area.theme-secondary` + `NavExpand` + module selector dropdown.
- **Template pages:** `BrandNav-area.theme-secondary.d-grid` + `nav-tree`.
- Nav bookmarks (star overlay via `btn-hover-container`) are an optional feature.

### 6.4 Content Region

**System pages** use `system-container[container-type]` + `system-block[block-type]`. Header block is pinned; main block scrolls independently. See §6.5 for the full container/block-type reference.

**Template pages** use `section.panel[toggle-parent]` + `div.panel-body[toggle-element]` for collapsible content panels. See §6.5 for usage guidance.

### 6.5 Container and Panel Usage

#### System Page Containers (`system-container` + `system-block`)

| container-type | block-type | Purpose |
|----------------|------------|---------|
| `main` | `header` | Pinned patient/record context |
| `main` | `main` | Scrollable primary content |
| `aside-start` | `aside` | Persistent side panel |
| `aside-start-collapse` | `aside-collapse` | Collapsible side panel |

- Default layout: `container-type="main"` with header + main blocks.
- Add side panel when workflow needs persistent reference data (e.g., patient history while editing current visit).
- Use collapsible variant when side content is optional and screen real estate matters.

#### Template Page Panels (`section.panel` + `div.panel-body`)

- Only for demo/documentation pages. MUST NOT use in production.
- `toggle-parent` / `toggle-element` attributes enable collapse behavior.

#### Decision Guide

- New production page → always `system-container`.
- Need pinned context → add `block-type="header"`.
- Need side reference → add `container-type="aside-start"`.
- Demo/docs page → `panel` pattern is acceptable.

### 6.6 Responsive Strategy

Desktop-first. MUST work well on standard workstation displays. SHOULD degrade gracefully to tablet. Mobile is not a current priority, but layout decisions SHOULD NOT preclude future mobile support.

---

## 7. Shared Rules & Constraints

Rules below apply to **all page archetypes** unless explicitly scoped.

### Accessibility

1. **MUST** use `rem` for all sizing — never `px`. See §5.2 for the 5-step scale.
2. **MUST** support the `data-layout-size` switching system (xs / sm / md / lg / xlg).
3. **SHOULD** use `form-control-lg` for form inputs in clinical contexts — staff interact at distance and with gloves.
4. Color contrast **MUST** meet requirements using CSS variable-based colors (`--bs-primary`, `--bs-dark`, etc.). Do not assume contrast from hardcoded values.

### Code Standards

5. **MUST NOT** hardcode color hex values — always use CSS variables (see §5.1).
6. **MUST NOT** use inline styles for layout — use utility classes or custom attributes (`container-type`, `block-type`).
7. For event binding and Bootstrap loading patterns, see `DESIGN-engineering.v2.1.md`.

### Component Usage

8. **Tables:** base stack is `table-custom table-sm table-borderless` for clinical data grids.
9. **Forms:** group with `row` + `g-1` / `g-2` gap classes. Use `form-control-lg` when clinical accessibility matters (see rule 3).
10. **Modals:** prefer `offcanvas` for side panels; use `modal` for confirmations and alerts.
11. **Icons:** `mdi mdi-*` prefix exclusively. **MUST NOT** mix icon libraries (see §5.5).
12. **Loading states:** use standardized loading indicators. Specs in `DESIGN-components.v2.1.md`.

### Naming & Structure

13. Use custom attributes for semantic structure: `container-type`, `block-type`, `toggle-parent`, `toggle-element`.
14. BEM is not enforced, but class names **SHOULD** be descriptive and hyphenated (e.g., `personal-info-container`, `nav-tree`).

---

## 8. Legacy & Deprecated

### Allowed Legacy Variations

Existing pages using Library A patterns **may keep** the following — no forced rewrite required:

- **Template Page archetype** — for demo/docs only; MUST NOT be used for new production workflows.
- **`theme-light` header** — template pages only.
- **Panel-based layout** with `toggle-parent` / `toggle-element` — template pages only.
- **A's original secondary color mapping** — A used a lighter value (`#dfe9ef`) for `--bs-secondary`; this now maps to `--bs-tertiary` in B.

### Deprecated Patterns

These patterns **SHOULD** be migrated away from over time:

- **A-only table variants** not present in B's canonical set — gradual deprecation.
- **Inline `px` sizing** — migrate to `rem`.
- **Direct hex color usage** — migrate to CSS variables.
- **Template Page archetype for production workflows** — migrate to System Page.
- **`Medus.svg`** brand asset — A-only, replaced by `Tzuchi-logo` icon font.

### Migration Notes

- Detailed migration guidance lives in `DESIGN-engineering.v2.1.md`.
- When touching a legacy page, **SHOULD** incrementally adopt B patterns in the area you modify.
- Full rewrite is **NOT** required — incremental adoption is acceptable.

---

## 9. Pending Confirmation

Items below require stakeholder input before finalization.

| Item | Current Assumption | What Needs Confirming |
|------|-------------------|----------------------|
| NavExpand behavior details | `aside.NavExpand` toggles sidebar visibility | Exact expand/collapse states, animation, and persistence rules <!-- pending confirmation --> |
| SCSS variable names | Tokens like `_Var-Color-DarkBlue`, `_Var-Tints`, `_Var-Layout-Tzuchi` referenced in design context | Not found in scanned repo — may live in a separate SCSS source or build artifact <!-- pending confirmation --> |
| Theme variants beyond default | Only 沉穩藍 / Calm Blue is documented here; two alternatives listed in §2.5 | Scope, priority, and variable definitions for 陽光橘 and 玫瑰紫 themes <!-- pending confirmation --> |
| Accessibility audit results | Design targets clinical accessibility (large controls, rem sizing) | Formal WCAG compliance level target (AA vs AAA) and audit schedule <!-- pending confirmation --> |
| Mobile responsive breakpoints | Desktop-first; mobile is not a current priority (§6.6) | If/when mobile becomes a priority, breakpoint definitions and layout adaptation rules <!-- pending confirmation --> |
| Empty state illustrations | No canonical set identified | Which SVG variants are canonical for empty/zero-data states <!-- pending confirmation --> |
| Alert dismissal behavior | Alerts present but auto-dismiss timing not standardized | Auto-dismiss timing vs. manual dismissal rules <!-- pending confirmation --> |
| Search/filter bar pattern | Search inputs exist across system pages | Standard search/filter bar layout not yet fully standardized <!-- pending confirmation --> |

---

## Changelog

| Date | Sections | Summary |
|------|----------|---------|
| 2026-04-11 | 1–9 | Initial three-tier refactor from legacy DESIGN.md |
| 2026-04-11 | 2.4, 5.4, 5.5, 6.5, 7, 9 | v2.1 refinement: layout rhythm, state semantics, icon consolidation, container usage, boundary cleanup |
