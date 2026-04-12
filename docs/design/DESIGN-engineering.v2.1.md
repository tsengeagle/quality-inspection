# DESIGN-engineering.v2.1.md — Engineering Guidance & Migration

> **Tier 3** of the HIS51 design authority.
> Read alongside **DESIGN.v2.1.md** (§1) for migration or low-level implementation details.
>
> Terminology: **A** = Template library (legacy 模板庫);  **B** = System library / HIS51\_Template (canonical 系統庫).

---

## How to Use This Document

| If you need to …                        | Start at           |
|-----------------------------------------|---------------------|
| Migrate an A-based page to B            | §1 Migration Warnings, §8 Deprecated Patterns |
| Understand JS wiring conventions        | §2 Event Binding, §4 JS Module Architecture |
| Know which plugins/modules ship with B  | §5 UI Module Inventory, §6 Plugin Inventory |
| Modify or extend the theme layer        | §7 SCSS Architecture |
| Check B-only capabilities               | §9 B-Only Capabilities Reference |

> **For MFE implementation work:** The frontend MFE governance package defines the execution baseline for shell interaction, standalone compatibility, and CSS ownership. See [frontend-mfe-governance.md](../../ai/governance/frontend-mfe-governance.md), [frontend-design-language-mfe.md](../../ai/governance/frontend-design-language-mfe.md), and [frontend-mfe-decisions-r1.md](../../ai/governance/frontend-mfe-decisions-r1.md).

---

## 1. Migration Warnings (A → B)

### 1.1 CSS Variable Semantic Change

The most disruptive change between A and B is the **semantic shift of `--bs-secondary`**.

| Variable         | Library A (legacy)            | Library B (canonical)         |
|------------------|-------------------------------|-------------------------------|
| `--bs-primary`   | `#2A536E`                     | `#2A536E` (same)              |
| `--bs-secondary` | `#dfe9ef` (light gray-blue)   | `#2A536E` (dark blue)         |
| `--bs-tertiary`  | *(does not exist)*            | `#dfe9ef` (= A's secondary)  |
| `--bs-light`     | `#eeeff5`                     | `#e9ebef`                     |
| `--bs-dark`      | `#22252c`                     | `#292e35`                     |

**Migration rule:**  
Any element that used `bg-secondary` in A (expecting a *light* background) **must** be changed to `bg-tertiary` in B. Leaving it unchanged produces a dark-blue background instead of the intended light gray-blue.

```diff
- <div class="bg-secondary">…</div>
+ <div class="bg-tertiary">…</div>
```

### 1.2 Font-Size Mapping

B introduces a **5-step** layout-size scale (adding `xs`). Pages render **one step larger** at the same `data-layout-size` value.

| B step | B size    | ≈ A step | A size    |
|--------|-----------|----------|-----------|
| `xs`   | 0.75 rem  | ≈ `sm`   | 0.725 rem |
| `sm`   | 0.875 rem | ≈ `md`   | 0.875 rem |
| `md`   | 1 rem     | ≈ `lg`   | 1 rem     |
| `lg`   | 1.125 rem | ≈ `xlg`  | 1.125 rem |
| `xlg`  | 1.25 rem  | *(none)* | —         |

> **Impact:** A page that set `data-layout-size="sm"` in A will appear at roughly `xs` density in B. Audit all `data-layout-size` attributes during migration.

### 1.3 Font Family

```diff
- font-family: "Noto Sans", sans-serif;
+ font-family: "Noto Sans TC", sans-serif;
```

B uses **"Noto Sans TC"** (Traditional Chinese–optimized 繁體中文) across all pages. Search-and-replace any explicit `"Noto Sans"` references in inline styles or overrides.

---

## 2. Event Binding Pattern

**Canonical (B) — delegated binding:**

```js
$(document).on("click", "[data-action='save']", function () {
  // handler
});
```

**Deprecated (A) — direct binding:**

```js
$("[data-action='save']").on("click", function () {
  // handler
});
```

**Why delegated?**
- **Performance:** A single listener on `document` vs. one per element.
- **Memory:** No orphaned handlers when DOM nodes are replaced (common in SPA-like page sections).
- **Dynamic DOM:** Handlers fire for elements added *after* binding, which is essential when content is loaded asynchronously.

> Per design decision D10 in `frontend-template-design-diff-decision-log.md`.

---

## 3. Bootstrap Loading Strategy

**Canonical (B) — split load:**

```html
<script src="js/popper.min.js"></script>
<script src="js/bootstrap-5.2.3.min.js"></script>
```

**Deprecated (A) — bundle load:**

```html
<script src="js/bootstrap.bundle.min.js"></script>
```

**Why split?**
- Reduced total bundle size — pages that do not need Popper-dependent components (tooltips, dropdowns, popovers) can omit `popper.min.js`.
- Finer cache granularity — Popper and Bootstrap versions can be updated independently.

> Per design decision D11 in `frontend-template-design-diff-decision-log.md`.

---

## 4. JS Module Architecture

### 4.1 Pattern

Each feature is isolated into a dedicated `UI-*.js` file. There is **no bundler** — scripts are loaded individually via `<script>` tags.

### 4.2 Module Wrapper

Every module uses the jQuery ready wrapper with delegated handlers keyed off **semantic `data-*` attributes**:

```js
$(function () {
  // Delegated handlers
  $(document).on("click", "[data-toggle-drawer]", function () {
    // …
  });

  // Initialization logic
  // …
});
```

### 4.3 Library Versions

| Library   | A (legacy)                | B (canonical)              |
|-----------|---------------------------|----------------------------|
| jQuery    | `jquery-3.3.1.min.js`     | `jquery-3.6.3.min.js`      |
| Bootstrap | `bootstrap.bundle.min.js` | `bootstrap-5.2.3.min.js` + `popper.min.js` |

Both A and B use Bootstrap 5 `data-bs-*` attributes for native Bootstrap behavior (modals, tooltips, collapses, etc.).

---

## 5. UI Module Inventory

B ships **38 UI modules**. The full manifest is listed below with library availability.

| Module | Purpose | B | A |
|--------|---------|---|---|
| `UI-Alerts.js` | Enhanced alert/notification display behaviors | ✓ | — |
| `UI-Approve.js` | Approval workflow UI behaviors | ✓ | — |
| `UI-ArticleRows.js` | Coordinates article-rows layout with drawer navigation | ✓ | — |
| `UI-Bootstrap.js` | Bootstrap initialization and split-loading orchestration | ✓ | — |
| `UI-ButtonCRUD.js` | Generates standardized CRUD button groups | ✓ | ✓ |
| `UI-Carousel.js` | Image/content carousel component | ✓ | ✓ |
| `UI-Copy.js` | Copy-to-clipboard functionality | ✓ | — |
| `UI-Datetimepicker.js` | Date and time picker integration | ✓ | ✓ |
| `UI-Demo.js` | Demo/documentation page utilities (template pages only) | ✓ | — |
| `UI-Dropdown.js` | Enhanced dropdown behaviors (including `dropdown-hoverable`) | ✓ | — |
| `UI-Filedrag.js` | File drag-and-drop upload handling | ✓ | — |
| `UI-FloatingWindow.js` | Floating/draggable window overlays | ✓ | — |
| `UI-Focus.js` | Focus management and highlight behaviors | ✓ | — |
| `UI-Forms.js` | Form-level behaviors and validation helpers | ✓ | ✓ |
| `UI-FormsSelectBadge.js` | Select elements with badge display | ✓ | — |
| `UI-FullScreenSize.js` | Full-screen toggle for content regions | ✓ | — |
| `UI-Gotop.js` | Scroll-to-top button behavior | ✓ | — |
| `UI-GridDarg.js` | Grid drag-and-drop reordering (interact.js based) | ✓ | — |
| `UI-Guideline.js` | Development guideline display utilities | ✓ | — |
| `UI-LayoutSize.js` | `data-layout-size` management (5-step: xs/sm/md/lg/xlg) | ✓ | ✓ |
| `UI-Lightbox.js` | Image lightbox viewer | ✓ | ✓ |
| `UI-LimitedText.js` | Text truncation with expand/collapse | ✓ | — |
| `UI-Loading.js` | Visual loading overlays and spinners | ✓ | ✓ |
| `UI-Modal.js` | Enhanced Bootstrap modal behaviors | ✓ | ✓ |
| `UI-MouseWheel.js` | Custom mouse-wheel scrolling behavior | ✓ | — |
| `UI-Nav.js` | Drawer navigation state and animation | ✓ | — |
| `UI-Simplebar.js` | Custom scrollbar styling (SimpleBar integration) | ✓ | ✓ |
| `UI-Slide.js` | Slide/transition animations | ✓ | ✓ |
| `UI-Sortable.js` | Sortable table/list behavior | ✓ | — |
| `UI-Sticky.js` | Sticky positioning behaviors | ✓ | — |
| `UI-StickyHeader.js` | Pinned header behavior for system pages | ✓ | — |
| `UI-StickyTopBottom.js` | Dual sticky (top and bottom) positioning | ✓ | — |
| `UI-SystemContainer.js` | System-container layout management | ✓ | — |
| `UI-Tab.js` | Tab navigation behaviors | ✓ | ✓ |
| `UI-Theme.js` | Theme switching via `#ThemeCSS` + `[theme-trig]` | ✓ | — |
| `UI-Toggle.js` | Panel collapse via `toggle-parent` / `toggle-element` | ✓ | ✓ |
| `UI-Tooltips.js` | Tooltip display with custom attributes | ✓ | — |
| `UI-Tree.js` | Tree navigation component behavior | ✓ | ✓ |

### Additional B-Only Assets

| Asset | Purpose |
|-------|---------|
| `Tzuchi-Skeleton-*.html` | Skeleton loading templates (one per system page) |
| `EUDC-tzuchi` font | Medical custom characters (醫療造字) |
| `interact.min.js` | Third-party library for drag-and-resize interactions |
| `bootstrap-select` plugin | Enhanced select dropdowns with search and multi-select |

### Rules

1. B-only modules **MUST NOT** be introduced into Library A pages.
2. When building new system pages, **SHOULD** leverage existing UI modules rather than writing custom JS.
3. Module filenames follow the `UI-*.js` convention — do not create modules outside this pattern.

---

## 6. Plugin Inventory

| Plugin             | File                  | Purpose                    | B coverage  |
|--------------------|-----------------------|----------------------------|-------------|
| bootstrap-select   | *(vendor)*            | Enhanced select dropdowns  | common      |
| interact.js        | `interact.min.js`     | Drag & resize interactions | 67/67 pages |
| simplebar          | *(vendor)*            | Custom scrollbars          | common      |
| splide             | *(vendor)*            | Carousel / slider          | as needed   |

> **interact.js** is universal in B — every page includes it (67 of 67 scanned pages).

---

## 7. SCSS Architecture

### 7.1 Entry Point

The main SCSS entry file is **`UI.scss`**, which imports partials in dependency order.

### 7.2 Key Variable Files

| File                          | Role                              |
|-------------------------------|-----------------------------------|
| `_Var-Color-DarkBlue.scss`    | Primary color palette definition  |
| `_Var-Tints.scss`             | Tint / shade generation           |
| `_Var-Layout-Tzuchi.scss`     | Layout tokens for Tzu Chi theme   |

> These files are referenced in the SCSS source but may reside in a separate SCSS repository not included in the scanned HTML/JS distribution.

### 7.3 Theme Switching Mechanism

Runtime theme switching is driven by a `<link>` element with `id="ThemeCSS"`. The `UI-Theme.js` module swaps the `href` attribute to load a different compiled stylesheet:

```html
<link id="ThemeCSS" rel="stylesheet" href="css/UI-DarkBlue.css">
```

```js
// Conceptual — actual implementation in UI-Theme.js
$("#ThemeCSS").attr("href", "css/UI-AltTheme.css");
```

### 7.4 Build Pipeline

SCSS is compiled to CSS before deployment. The build tooling details are not fully documented in the scanned repository; consult the SCSS source repository for build scripts.

---

## 8. Deprecated Patterns (Full Migration Table)

The following patterns from A **must** be replaced when migrating to B:

| # | Deprecated (A) Pattern                          | Replacement (B)                              | Reason                     |
|---|--------------------------------------------------|----------------------------------------------|----------------------------|
| 1 | `article-type="template"`                       | `article-type="FullNav"`                     | Semantic clarity            |
| 2 | `BrandNav-area theme-primary`                   | `BrandNav-area theme-secondary`              | Color standardization       |
| 3 | `PanelLayout`                                   | `SystemContainer`                            | Advanced layout features    |
| 4 | 4-step `data-layout-size` (`sm/md/lg/xlg`)      | 5-step (`xs/sm/md/lg/xlg`)                   | Finer density control       |
| 5 | `table-outline`, `table-shadow`, `table-custom-rounded` | `table-rounded`, `table-tbody-rounded` | Simplified table variants   |
| 6 | Direct `.on()` binding                          | Delegated `$(document).on()`                 | Performance & dynamic DOM   |
| 7 | `bootstrap.bundle.min.js`                       | `bootstrap-5.2.3.min.js` + `popper.min.js`  | Reduced bundle size         |
| 8 | `Medus.svg` logo                                | `Tzuchi-logo` icon font glyph               | Asset consolidation         |
| 9 | `"Noto Sans"` font family                       | `"Noto Sans TC"`                             | Traditional Chinese support |
| 10| `jquery-3.3.1.min.js`                           | `jquery-3.6.3.min.js`                        | Latest LTS at time of B    |

---

## 9. B-Only Capabilities Reference

The following capabilities exist **only in B** and have no A equivalent:

| Capability              | Module / Mechanism        | Notes                                            |
|-------------------------|---------------------------|--------------------------------------------------|
| Runtime theme switching | `UI-Theme.js` + `#ThemeCSS` | Swap compiled CSS at runtime                   |
| SystemContainer layout  | `UI-SystemContainer.js`   | Replaces legacy `PanelLayout`                    |
| Sticky header           | `UI-StickyHeader.js`      | Fixed header with scroll-aware state             |
| Drawer navigation       | `UI-Nav.js`               | Side-panel navigation state and animation        |
| Drag-and-drop grid      | `UI-GridDarg.js`          | interact.js–powered grid rearrangement           |
| `xs` layout-size step   | `UI-LayoutSize.js`        | 5th density step (0.75 rem) not available in A   |
| `--bs-tertiary` token   | SCSS variables            | New semantic color slot absorbing A's secondary   |
| `xlg` font-size step    | `UI-LayoutSize.js`        | 1.25 rem — largest density step, B-only          |

---

## Changelog

| Date | Sections | Summary |
|------|----------|---------|
| 2025-07-21 | 1–9 | Initial engineering guidance extracted from design authority sources |
| 2025-07-21 | 5 | v2.1 refinement: expanded UI Module Inventory to full 38-module manifest (absorbed from DESIGN-components.md v2.0 §10), added Changelog |

---

*End of DESIGN-engineering.v2.1.md*
