# DESIGN-components.v2.1.md — Component Specifications

> **Tier-2 design reference.** Read this document alongside `DESIGN.v2.1.md` when building or modifying a specific component.
> For core principles and canonical baseline, see `DESIGN.v2.1.md`. For migration/engineering details, see `DESIGN-engineering.v2.1.md`.

---

## How to Use This Document

This document specifies **canonical HTML patterns, CSS classes, and behavioral rules** for each major UI component in HIS51. It is the authoritative source for component-level implementation decisions.

- Each section contains: **Purpose**, **Canonical HTML**, **Key Classes**, **Behavioral Notes**, and **Rules**.
- **Library B** (HIS51_Template) patterns are canonical. Library A patterns are noted only where legacy coexistence requires awareness.
- Color values reference CSS variables defined in `DESIGN.v2.1.md` §5.1. **MUST NOT** hardcode hex values.
- All sizing MUST use `rem` units (see `DESIGN.v2.1.md` §5.2).

---

## 1. Tables

### Purpose

Tables display clinical data grids — patient records, schedules, lab results, and administrative lists. Optimized for high-density scanning in time-pressured clinical environments.

### Canonical HTML

```html
<!-- Clinical data table (high-density) -->
<table class="table table-custom table-sm table-borderless">
  <thead>
    <tr>
      <th>病歷號</th>
      <th>姓名</th>
      <th>日期</th>
      <th>狀態</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>A123456789</td>
      <td>陳美美</td>
      <td>2025-01-15</td>
      <td><span class="badge bg-success">完成</span></td>
    </tr>
  </tbody>
</table>

<!-- Rounded variant -->
<table class="table table-custom table-rounded">
  <!-- ... -->
</table>

<!-- Rounded tbody variant -->
<table class="table table-custom table-tbody-rounded">
  <!-- ... -->
</table>
```

### Key Classes

| Class | Description |
|-------|-------------|
| `table-custom` | **Base class** — MUST be present on all HIS51 tables. Applies system styling. |
| `table-sm` | Compact row height for dense clinical data |
| `table-borderless` | Removes cell borders for cleaner scan lines |
| `table-rounded` | Rounds the outer corners of the entire table (B canonical) |
| `table-tbody-rounded` | Rounds only the `<tbody>` container corners (B canonical) |

### Behavioral Notes

No dedicated JS module. Tables are static markup styled via CSS. For sortable tables, see `UI-Sortable.js` in `DESIGN-engineering.v2.1.md`.

### Rules

1. **MUST** use `table-custom` as the base class on every table.
2. Clinical data tables **SHOULD** combine `table-sm table-borderless` for maximum scanning density (see `DESIGN.v2.1.md` §5.3).
3. **MUST NOT** use deprecated A-only variants: `table-outline`, `table-shadow`, `table-custom-rounded`. These are not present in Library B.
4. For rounded styling, use `table-rounded` or `table-tbody-rounded` — never invent custom border-radius utilities.

---

## 2. Forms & Inputs

### Purpose

Forms collect clinical and administrative data — patient intake, scheduling, search criteria, and configuration. Large interactive targets accommodate gloved hands and distance viewing.

### Canonical HTML

```html
<!-- Standard form layout with grid gaps -->
<form>
  <div class="row g-2">
    <div class="col-md-4">
      <label class="form-label">姓名</label>
      <input type="text" class="form-control form-control-lg">
    </div>
    <div class="col-md-4">
      <label class="form-label">病歷號</label>
      <input type="text" class="form-control form-control-lg">
    </div>
    <div class="col-md-4">
      <label class="form-label">院區</label>
      <select class="form-select form-control-lg">
        <option>花蓮</option>
        <option>台北</option>
      </select>
    </div>
  </div>
</form>

<!-- Enhanced select with bootstrap-select plugin -->
<select class="selectpicker" data-live-search="true">
  <option>Option A</option>
  <option>Option B</option>
</select>

<!-- Read-only data display with definition lists -->
<dl class="dl-list dl-list-reverse flex-center-start g-ps-4">
  <dt>血型</dt>
  <dd>O+</dd>
</dl>
```

### Key Classes

| Class | Description |
|-------|-------------|
| `form-control` | Standard Bootstrap 5 text input styling |
| `form-select` | Standard Bootstrap 5 select styling |
| `form-control-lg` | Large input target — **SHOULD** use in clinical contexts for accessibility |
| `selectpicker` | Triggers the `bootstrap-select` plugin for enhanced dropdowns (search, multi-select) |
| `row` + `g-1` / `g-2` | Grid layout with gap classes for form field spacing |
| `dl-list` | Definition list base class for read-only data display |
| `dl-list-reverse` | Reverses dt/dd visual order (label after value) |
| `dl-list-increment` | Incremental numbering variant for ordered definition lists |
| `form-label-customer` | Custom label styling matching `dl-list` appearance |

### Behavioral Notes

- `UI-Forms.js` — handles form-level behaviors.
- `UI-FormsSelectBadge.js` — enhances select elements with badge display.
- `bootstrap-select` plugin (located at `assets/js/plugin/bootstrap-select-1.14/`) provides searchable, multi-select dropdowns.

### Rules

1. **SHOULD** use `form-control-lg` for form inputs in clinical contexts — staff interact at distance and with gloves (see `DESIGN.v2.1.md` §7 rule 3).
2. **MUST** group form fields with `row` + `g-1` or `g-2` gap classes (see `DESIGN.v2.1.md` §7 rule 10).
3. Read-only data **SHOULD** use `dl-list` definition lists, not disabled form inputs.
4. For enhanced select behavior (search, multi-select), use `selectpicker` class with the `bootstrap-select` plugin.
5. **MUST** use `rem`-based sizing — never `px` on form elements.

---

## 3. Buttons

### Purpose

Buttons drive user actions — CRUD operations, navigation, form submissions, and clinical workflow transitions. Standard Bootstrap `btn` patterns are the foundation; B-only modules add CRUD automation and hover overlays.

### Canonical HTML

```html
<!-- Standard buttons -->
<button class="btn btn-primary">儲存</button>
<button class="btn btn-outline-primary">取消</button>
<button class="btn btn-dark">確認</button>

<!-- CRUD button group (generated by UI-ButtonCRUD.js) -->
<div class="btn-group">
  <button class="btn btn-primary"><i class="mdi mdi-plus"></i> 新增</button>
  <button class="btn btn-primary"><i class="mdi mdi-content-save"></i> 儲存</button>
  <button class="btn btn-danger"><i class="mdi mdi-delete"></i> 刪除</button>
</div>

<!-- Nav bookmark with star overlay -->
<div class="btn-hover-container">
  <a class="btn btn-sm btn-primary" href="#">
    門診掛號
  </a>
  <a class="btn btn-hover btn-gray-static" href="#">
    <i class="mdi mdi-star"></i>
  </a>
</div>
```

### Key Classes

| Class | Description |
|-------|-------------|
| `btn btn-*` | Standard Bootstrap 5 button variants (`btn-primary`, `btn-dark`, `btn-outline-*`, etc.) |
| `btn-hover-container` | Container enabling a hover-reveal overlay button (e.g., bookmark star) |
| `btn-hover` | The overlay button that appears on hover within `btn-hover-container` |
| `btn-gray-static` | Static gray styling for hover overlay buttons |

### Behavioral Notes

- `UI-ButtonCRUD.js` — B-only module that generates standardized CRUD button groups. Use for consistent create/read/update/delete actions across system pages.

### Rules

1. **MUST** use standard `btn btn-*` patterns — no custom button classes outside the design system.
2. CRUD button sets **SHOULD** use `UI-ButtonCRUD.js` to ensure consistency across pages.
3. Nav bookmark overlays **MUST** use the `btn-hover-container` + `btn-hover` pattern.
4. Icons within buttons **MUST** use `mdi mdi-*` prefix (see §8).

---

## 4. Panels & Containers

### Purpose

Panels and containers define content regions within page archetypes. System pages use the `system-container` / `system-block` pattern; template pages use collapsible `panel` / `panel-body` sections. See `DESIGN.v2.1.md` §4 and §6.4 for archetype context.

### Canonical HTML

```html
<!-- System page containers (B canonical — production pages) -->
<div class="system-container" container-type="main">
  <system-block block-type="header">
    <!-- Pinned patient context bar -->
    <div class="personal-info-container"><!-- patient demographics --></div>
  </system-block>
  <system-block block-type="main">
    <!-- Scrollable primary content -->
  </system-block>
</div>

<!-- System page with side panel -->
<div class="system-container" container-type="aside-start">
  <system-block block-type="aside">
    <!-- Side panel content -->
  </system-block>
  <system-block block-type="main">
    <!-- Main content -->
  </system-block>
</div>

<!-- System page with collapsible side panel -->
<div class="system-container" container-type="aside-start-collapse">
  <system-block block-type="aside-collapse">
    <!-- Collapsible side panel -->
  </system-block>
  <system-block block-type="main">
    <!-- Main content -->
  </system-block>
</div>

<!-- Template page panels (legacy / demo only) -->
<section class="panel" toggle-parent>
  <h5>Section Title</h5>
  <div class="panel-body" toggle-element>
    <!-- Collapsible content -->
  </div>
</section>
```

### Key Classes & Attributes

| Class / Attribute | Description |
|-------------------|-------------|
| `system-container` | Top-level content region for system pages |
| `container-type="main"` | Standard main content layout |
| `container-type="aside-start"` | Layout with persistent side panel |
| `container-type="aside-start-collapse"` | Layout with collapsible side panel |
| `block-type="header"` | Pinned context block (patient info); does not scroll |
| `block-type="main"` | Scrollable primary content block |
| `block-type="aside"` | Persistent side panel block |
| `block-type="aside-collapse"` | Collapsible side panel block |
| `panel` / `panel-body` | Template page collapsible content sections |
| `toggle-parent` / `toggle-element` | Attributes enabling panel collapse behavior |
| `personal-info-container` | Patient demographics bar within the header block |

### Behavioral Notes

- `UI-SystemContainer.js` — B-only module managing system-container layout behavior (scroll containment, resize, block coordination).
- `UI-Toggle.js` — handles `toggle-parent` / `toggle-element` expand/collapse in template pages.

### Rules

1. New production pages **MUST** use `system-container` + `system-block` — never the `panel` pattern (see `DESIGN.v2.1.md` §4).
2. Template page `panel` pattern is allowed only for demo/documentation pages.
3. Container and block types **MUST** use the custom attributes (`container-type`, `block-type`) — not CSS classes.
4. The header block (`block-type="header"`) is pinned; the main block scrolls independently.

---

## 5. Modal / Offcanvas / Drawer

### Purpose

Modals present confirmations, alerts, and focused data-entry tasks. Offcanvas panels serve as side drawers for notifications and supplementary information. Drawer navigation controls the main sidebar. These are the three overlay patterns in the system.

### Canonical HTML

```html
<!-- Bootstrap 5 Modal (confirmations, alerts) -->
<button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#confirmModal">
  確認刪除
</button>
<div class="modal fade" id="confirmModal" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title">確認</h5>
        <button class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">確定要刪除此筆資料嗎？</div>
      <div class="modal-footer">
        <button class="btn btn-outline-dark" data-bs-dismiss="modal">取消</button>
        <button class="btn btn-danger">刪除</button>
      </div>
    </div>
  </div>
</div>

<!-- Offcanvas notification panel (system page header) -->
<a class="header-item nav-icon" href="#Offcanvas-News"
   notification-more="2" data-bs-toggle="offcanvas">
  <i class="mdi mdi-bell mb-0"></i>
</a>
<div class="offcanvas offcanvas-end w-30rem" id="Offcanvas-News">
  <div class="offcanvas-header border-bottom px-3">
    <h5 class="offcanvas-title">系統通知</h5>
    <button class="btn-close text-reset" data-bs-dismiss="offcanvas" aria-label="Close"></button>
  </div>
  <div class="offcanvas-body px-3 pb-0 bg-light">
    <!-- Notification items -->
  </div>
</div>

<!-- Drawer navigation (sidebar toggle) -->
<div class="hamburger-icon drawer-btn me-3" data-drawer-btn="nav-main"><i></i></div>
<!-- ... -->
<div class="ArticleRows-wrap" data-drawer-element="nav-main">
  <!-- Page content that responds to drawer state -->
  <div class="nav-mask" data-drawer-btn="nav-main"></div>
</div>
```

### Key Classes & Attributes

| Class / Attribute | Description |
|-------------------|-------------|
| `data-bs-toggle="modal"` | Bootstrap 5 standard modal trigger |
| `data-bs-toggle="offcanvas"` | Bootstrap 5 offcanvas trigger |
| `offcanvas-end` | Slides in from the right side |
| `w-30rem` | Fixed offcanvas width for notification panels |
| `notification-more` | Badge count attribute for unread notifications |
| `data-drawer-btn` | Drawer trigger (binds to matching `data-drawer-element`) |
| `data-drawer-element` | Drawer target container (e.g., `"nav-main"`) |
| `hamburger-icon` | Animated hamburger icon for drawer trigger |
| `drawer-btn` | Class on the hamburger trigger element |
| `nav-mask` | Overlay mask that closes the drawer on click |

### Behavioral Notes

- `UI-Modal.js` — enhances Bootstrap modal behavior for system use cases.
- `UI-Nav.js` — manages drawer navigation state and animation.
- `UI-ArticleRows.js` — coordinates article-rows layout with drawer state.

### Rules

1. **SHOULD** prefer `offcanvas` for side panels (notifications, supplementary data); use `modal` for confirmations and alerts (see `DESIGN.v2.1.md` §7 rule 11).
2. Drawer navigation **MUST** use the `data-drawer-btn` + `data-drawer-element` attribute pair — not custom JS toggling.
3. Notification offcanvas **MUST** use `offcanvas-end` (right-side slide-in) with `w-30rem` width.
4. Modals **MUST** include a `btn-close` with `data-bs-dismiss="modal"` for accessibility.

---

## 6. Loading & Skeleton

### Purpose

Loading indicators and skeleton screens provide visual feedback during data fetching and page initialization. They reduce perceived wait time and prevent layout shift in clinical workflows where responsiveness is critical.

### Canonical HTML

```html
<!-- Loading indicator (managed by UI-Loading.js) -->
<!-- UI-Loading.js provides programmatic loading overlays.
     Typical invocation is via JS, not static HTML. -->

<!-- Skeleton loading template pattern -->
<!-- Skeleton pages mirror the target page's layout structure
     with placeholder shapes instead of real content. -->
<!-- Examples:
     Tzuchi-Skeleton-基本資料維護.html
     Tzuchi-Skeleton-包裹列印管理.html
     Tzuchi-Skeleton-場控流程-站點.html
     Tzuchi-Skeleton-排程規劃.html
-->
```

### Key Files

| File | Description |
|------|-------------|
| `UI-Loading.js` | B-only JS module providing visual loading overlays and spinners |
| `Tzuchi-Skeleton-*.html` | B-only skeleton loading templates — one per system page |

### Behavioral Notes

- `UI-Loading.js` manages loading overlay display/hide programmatically. It is invoked from JS, not instantiated via static HTML.
- Skeleton templates (`Tzuchi-Skeleton-*.html`) are full HTML pages that mirror their target page's layout structure, using placeholder shapes in place of real data. They are displayed during initial page load, then replaced with the actual page content.

### Rules

1. System pages **SHOULD** use `UI-Loading.js` for loading states during async data operations.
2. Skeleton templates **SHOULD** mirror the exact layout structure of their target page — do not use generic skeleton patterns.
3. Loading states **MUST** use standardized loading indicators (see `DESIGN.v2.1.md` §7 rule 13) — no custom spinners or progress bars.
4. Skeleton templates are B-only. Library A pages do not have skeleton equivalents.

---

## 7. Account Panel

### Purpose

The account panel provides user identity, settings, and system-level actions in the system page header. It includes avatar display, role identification, accessibility controls, theme switching, and session management — the primary user-facing control surface.

### Canonical HTML

```html
<!-- Account panel in system page header -->
<div class="header-form header-item-square ms-auto header-account-container">
  <!-- Notification bell with badge count -->
  <a class="header-item nav-icon" href="#Offcanvas-News"
     notification-more="2" data-bs-toggle="offcanvas"
     tooltip-trig="theme-dark" tooltip-placement="bottom" tooltip-title="系統通知">
    <i class="mdi mdi-bell mb-0"></i>
  </a>

  <!-- Account dropdown -->
  <div class="dropdown">
    <span class="header-item dropdown-toggle me-0 pe-0"
          data-bs-toggle="dropdown" data-bs-auto-close="outside">
      <i class="mdi mdi-account-circle-outline nav-icon"></i>
      <span class="pe-3">陳美美 (A123456789)</span>
    </span>
    <div class="dropdown-menu dropdown-menu-end">
      <div class="account-wrap">
        <!-- Avatar, role badge -->
        <!-- Font-size control (data-layout-size switcher) -->
        <!-- Language switcher -->
        <!-- Theme switcher -->
        <!-- Version info -->
      </div>
      <div class="action-btn-wrap flex-center-start g-me-2">
        <a class="btn btn-dark w-50" href="#">
          <i class="mdi mdi-account-convert"></i><span>切換身分</span>
        </a>
        <a class="btn btn-dark w-50" href="Login.html">
          <i class="mdi mdi-logout"></i><span>登出</span>
        </a>
      </div>
    </div>
  </div>
</div>
```

### Key Classes & Attributes

| Class / Attribute | Description |
|-------------------|-------------|
| `header-account-container` | Wrapper for the account panel region in the header |
| `header-item` | Inline header action item (icon, link) |
| `header-item-square` | Square-shaped header action region |
| `nav-icon` | Icon styling within header navigation |
| `account-wrap` | Container for account details inside the dropdown |
| `action-btn-wrap` | Container for action buttons (switch identity, logout) |
| `notification-more` | Attribute for notification badge count display |
| `dropdown-hoverable` | Enables dropdown open-on-hover (used for settings/bookmarks in header) |

### Account Panel Features

| Feature | Implementation |
|---------|---------------|
| Avatar | User avatar image within `account-wrap` |
| Role badge | Badge indicating user role/department |
| Font-size control | `data-layout-size` switcher (xs/sm/md/lg/xlg), managed by `UI-LayoutSize.js` |
| Language switcher | Language selection within account dropdown |
| Theme switcher | Theme selection via `theme-trig` attribute (see §9) |
| Version info | System version display |
| Switch identity | `mdi-account-convert` button — switches active user role |
| Logout | `mdi-logout` button — redirects to `Login.html` |
| Notification bell | Opens `#Offcanvas-News` offcanvas panel (see §5) |

### Behavioral Notes

- The account panel is part of the system page header (`header.theme-secondary`).
- `UI-LayoutSize.js` manages the font-size control.
- `UI-Theme.js` manages the theme switcher (see §9).
- Notification offcanvas is documented in §5.

### Rules

1. System pages **MUST** include the account panel in the header.
2. The notification bell **MUST** link to the offcanvas panel via `data-bs-toggle="offcanvas"`.
3. The account dropdown **MUST** use `data-bs-auto-close="outside"` to remain open during interaction.
4. Switch identity and logout buttons **MUST** be present in the `action-btn-wrap`.

---

## 8. Icons

### Purpose

Icons provide visual cues for actions, navigation, and status indicators. A single icon library ensures visual consistency across all pages.

### Canonical HTML

```html
<!-- Material Design Icons (canonical) -->
<i class="mdi mdi-plus"></i>              <!-- Add action -->
<i class="mdi mdi-content-save"></i>      <!-- Save action -->
<i class="mdi mdi-delete"></i>            <!-- Delete action -->
<i class="mdi mdi-bell"></i>              <!-- Notifications -->
<i class="mdi mdi-account-circle-outline"></i>  <!-- User account -->
<i class="mdi mdi-cog-outline"></i>       <!-- Settings -->
<i class="mdi mdi-book-outline"></i>      <!-- Bookmarks -->
<i class="mdi mdi-star"></i>              <!-- Favorite/bookmark -->
<i class="mdi mdi-information-outline"></i>     <!-- Information -->
<i class="mdi mdi-logout"></i>            <!-- Logout -->
<i class="mdi mdi-account-convert"></i>   <!-- Switch identity -->

<!-- Brand icon (Tzu Chi logo) -->
<span class="Tzuchi-logo"></span>
```

### Key Classes

| Class | Description |
|-------|-------------|
| `mdi` | Base class for Material Design Icons — **MUST** be present |
| `mdi-*` | Specific icon glyph (e.g., `mdi-plus`, `mdi-delete`, `mdi-bell`) |
| `Tzuchi-logo` | Tzu Chi institutional brand logo icon font |

### Rules

1. **MUST** use the `mdi mdi-*` prefix exclusively for all icons (see `DESIGN.v2.1.md` §7 rule 12).
2. **MUST NOT** mix icon libraries — no FontAwesome, no inline SVG icon sets, no other icon fonts.
3. Brand logo **MUST** use the `Tzuchi-logo` icon font class.
4. Deprecated: `Medus.svg` (A-only) — **MUST NOT** use in new pages.

---

## 9. Theme Variants

Three color themes provide role-based or personal visual identity. Theme switching is an **optional capability** — not mandatory for all pages (see `DESIGN.v2.1.md` §2.5).

### Theme Catalog

| Theme | Chinese Name | CSS File | `theme-trig` Value |
|-------|-------------|----------|-------------------|
| **Default** | 沉穩藍 / Calm Blue | `UI.css` (base stylesheet) | _(none — default)_ |
| Alternative 1 | 陽光橘 / Sunny Orange | `UI-Theme-OrangeBrown.css` | `UI-Theme-OrangeBrown` |
| Alternative 2 | 玫瑰紫 / Rose Purple | `UI-Theme-DarkMauve.css` | `UI-Theme-DarkMauve` |

### Switching Mechanism

Theme switching operates by swapping the `href` attribute on the `#ThemeCSS` link element:

```html
<!-- In <head>: the theme-switchable stylesheet -->
<link rel="stylesheet" href="assets/css/UI.css" id="ThemeCSS">

<!-- Theme trigger (anchor with theme-trig attribute) -->
<a href="#" theme-trig="UI-Theme-OrangeBrown">陽光橘</a>
<a href="#" theme-trig="UI-Theme-DarkMauve">玫瑰紫</a>

<!-- Or via <select> element with data-theme attribute -->
<select>
  <option value="">沉穩藍 (預設)</option>
  <option data-theme="UI-Theme-OrangeBrown" value="UI-Theme-OrangeBrown">陽光橘</option>
  <option data-theme="UI-Theme-DarkMauve" value="UI-Theme-DarkMauve">玫瑰紫</option>
</select>

<!-- Requires: -->
<script src="assets/js/UI-Theme.js"></script>
```

### How It Works

1. `UI-Theme.js` listens for clicks on elements with `[theme-trig]` attributes.
2. On trigger, it reads the `theme-trig` value and updates `#ThemeCSS` link `href` to `assets/css/UI-Theme-{value}.css`.
3. The alternative CSS file overrides CSS custom properties (`--bs-primary`, `--bs-secondary`, etc.) to recolor the entire interface.
4. Reverting to default restores `href` to `assets/css/UI.css`.

### Color Roles per Theme

The default theme (沉穩藍 / Calm Blue) color roles are fully specified in `DESIGN.v2.1.md` §5.1. Alternative themes override the same CSS variables to provide their color palettes.

### Rules

1. New pages **SHOULD** preserve theme-switch support — include `id="ThemeCSS"` on the main stylesheet link.
2. **MUST NOT** hardcode color hex values — always use CSS variables so theme switching works correctly (see `DESIGN.v2.1.md` §5.1).
3. Theme switching is **optional** (not all pages require a theme switcher UI), but the `#ThemeCSS` id MUST be present for forward compatibility.
4. Theme CSS files live at `assets/css/UI-Theme-*.css` — do not create custom theme files outside this convention.

---

## 10. Breadcrumbs

### Purpose

Contextual navigation trail showing page hierarchy. Found in 60+ B pages, 40+ A pages. Provides location awareness within the application.

### Canonical HTML

```html
<!-- Standard breadcrumb in header position -->
<nav aria-label="breadcrumb">
  <ul class="breadcrumbs" breadcrumbs-position="header">
    <li>門診系統</li>
    <li>掛號作業</li>
    <li>掛號報到</li>
  </ul>
</nav>

<!-- With home icon -->
<ul class="breadcrumbs" breadcrumbs-position="header" data-home="caption-border">
  <li>首頁</li>
  <li>子頁面</li>
</ul>

<!-- Without home icon -->
<ul class="breadcrumbs" breadcrumbs-position="header" data-home="off">
  <li>當前路徑</li>
</ul>
```

### Key Classes / Attributes

| Class / Attribute | Role |
|---|---|
| `.breadcrumbs` | Base class for breadcrumb list |
| `breadcrumbs-position="header"` | Positions within header area |
| `data-home="off"` | Hides the home icon |
| `data-home="caption-border"` | Shows home icon with border |

### Behavioral Notes

- No dedicated JS module — breadcrumbs are static navigation aids.
- Content is typically server-rendered (not client-side routed).
- Separator styling is CSS-generated via `::before` pseudo-elements.

### Rules

1. **MUST** use `.breadcrumbs` with `breadcrumbs-position="header"` for system pages.
2. **MUST** use semantic `<nav aria-label="breadcrumb">` wrapper for accessibility.
3. Last item in the list represents the current page — **MUST NOT** be a link.
4. **SHOULD** include at least two items (parent + current page).

---

## 11. Tabs

### Purpose

Divide related content into switchable panes within a single view. Used for clinical data grouping (e.g., patient history tabs, order categories).

### Canonical HTML

```html
<!-- Tab navigation -->
<ul class="nav nav-tabs nav-tabs-custom border-bottom-0 theme-gray">
  <li class="nav-item">
    <a class="nav-link active" href="#Tab-target-a1" data-bs-toggle="tab">基本資料</a>
  </li>
  <li class="nav-item">
    <a class="nav-link" href="#Tab-target-a2" data-bs-toggle="tab">病歷紀錄</a>
  </li>
</ul>

<!-- Tab content -->
<div class="tab-content">
  <div class="tab-pane active" id="Tab-target-a1">
    <!-- First tab content -->
  </div>
  <div class="tab-pane" id="Tab-target-a2">
    <!-- Second tab content -->
  </div>
</div>
```

### Key Classes / Attributes

| Class / Attribute | Role |
|---|---|
| `.nav-tabs-custom` | HIS51 custom tab styling |
| `.border-bottom-0` | Removes bottom border for cleaner integration |
| `.theme-gray` | Gray-toned tab appearance |
| `.nav-tabs-hover` | Hover-activated tab variant |
| `data-bs-toggle="tab"` | Bootstrap 5 tab toggle behavior |
| `.active` | Currently visible tab/pane |

### Behavioral Notes

- Uses `UI-Tab.js` for enhanced tab behavior beyond Bootstrap defaults.
- Tab IDs follow `Tab-target-*` naming convention.
- Content panes are shown/hidden via Bootstrap's `data-bs-toggle="tab"` mechanism.
- `.nav-tabs-hover` variant activates panes on hover instead of click.

### Rules

1. **MUST** use `nav-tabs-custom` as the base tab class (not plain `nav-tabs`).
2. **MUST** pair tab links with `data-bs-toggle="tab"` and matching `#id` targets.
3. Exactly one tab **MUST** have `.active` class on both the nav-link and tab-pane at load time.
4. Tab IDs **MUST** use `Tab-target-*` prefix convention for consistency.
5. **SHOULD** use `.theme-gray` for standard appearance. Only override for specific design needs.

---

## 12. Alerts and Notifications

### Purpose

System feedback messages for user actions (success, error, warning, info). Used after form submissions, data operations, and system events.

### Canonical HTML

```html
<!-- Standard alert -->
<div class="alert alert-warning alert-dismissible fade show" role="alert">
  <strong>注意：</strong> 此筆資料尚未儲存。
  <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
</div>

<!-- Success alert -->
<div class="alert alert-success fade show" role="alert">
  <i class="mdi mdi-check-circle"></i> 儲存成功
</div>

<!-- Danger alert with link -->
<div class="alert alert-danger fade show" role="alert">
  驗證失敗，請 <a href="#" class="alert-link">重新檢查</a>。
</div>

<!-- Info alert -->
<div class="alert alert-info fade show" role="alert">
  系統將於 10 分鐘後進行維護。
</div>
```

### Key Classes / Attributes

| Class / Attribute | Role |
|---|---|
| `.alert` | Base alert class |
| `.alert-success` / `.alert-warning` / `.alert-danger` / `.alert-info` | Semantic state variants |
| `.alert-dismissible` | Enables close button |
| `.fade.show` | Entrance animation |
| `.alert-link` | Styled link within alert body |
| `.btn-close` | Bootstrap close button |
| `data-bs-dismiss="alert"` | Auto-close behavior |

### Behavioral Notes

- `UI-Alerts.js` provides enhanced alert behaviors beyond Bootstrap defaults.
- Alerts appear within the page flow (not floating). For floating notifications, consider offcanvas or toast patterns.
- `.fade.show` classes work together — `.fade` provides the animation, `.show` triggers visibility.
- State colors **MUST** match semantic intent (see `DESIGN.v2.1.md` §5.4 State Semantics).

### Rules

1. **MUST** use semantic alert classes matching the message type (success/warning/danger/info).
2. Dismissible alerts **MUST** include `.alert-dismissible` + `.btn-close` with `data-bs-dismiss="alert"`.
3. **MUST** include `role="alert"` for accessibility.
4. **SHOULD** use MDI icons to reinforce the alert type visually.
5. **MUST NOT** repurpose alert colors for non-feedback purposes.
6. Auto-dismiss timing is not yet standardized — mark as `<!-- pending confirmation -->` if implementing auto-dismiss.

---

## 13. Empty States

### Purpose

Visual placeholder when a view contains no data. Provides user guidance on what to expect or what action to take. Prevents confusion from blank screens.

### Canonical HTML

```html
<!-- Standard empty state -->
<div class="flex-center text-center py-3">
  <div>
    <img class="h-10rem" src="assets/img/Empty.svg" alt="無資料">
    <p class="text-muted mt-2">查無相關資料</p>
  </div>
</div>

<!-- Empty state with action prompt -->
<div class="flex-center text-center py-3">
  <div>
    <img class="h-10rem" src="assets/img/Empty-2.svg" alt="請查詢">
    <p class="text-muted mt-2">請選擇查詢條件</p>
  </div>
</div>

<!-- Error/timeout empty state -->
<div class="flex-center text-center py-3">
  <div>
    <img class="h-10rem" src="assets/img/Empty-3.svg" alt="錯誤">
    <p class="text-muted mt-2">載入失敗，請重新整理</p>
  </div>
</div>
```

### Key Classes / Attributes

| Class / Attribute | Role |
|---|---|
| `.flex-center` | Centers content both horizontally and vertically |
| `.text-center` | Text alignment |
| `.py-3` | Vertical padding for breathing room |
| `.h-10rem` | Illustration height constraint |
| `.text-muted` | Subdued text color for guidance message |

### Behavioral Notes

- Illustration SVGs: `Empty.svg` (no data), `Empty-2.svg` (search prompt), `Empty-3.svg` (error), `Empty-4.svg` (variant). Also: `Payment_Success.svg`, `Payment_Failed.svg` for transaction states.
- Dedicated demo page: `Template-Components-Empty.html`.
- Empty states are static — no JS module. Content appears when data queries return zero results.

### Rules

1. **MUST** use `.flex-center.text-center.py-3` container pattern for consistent centering.
2. **MUST** include an illustration SVG (`Empty*.svg`) and a descriptive message.
3. Illustration height **SHOULD** use `.h-10rem` for consistent sizing.
4. Message text **MUST** be meaningful — distinguish "no results found" from "please enter search criteria" from "error occurred".
5. Which SVG variants are canonical is `<!-- pending confirmation -->`.

---

## 14. Search and Filter Bar

### Purpose

Input controls for filtering list/table content. Appears in headers, modals, and above data grids. Critical for clinical data lookup (patient search, medication search).

### Canonical HTML

```html
<!-- Header search (inline) -->
<div class="header-form">
  <input type="text" class="form-control" placeholder="搜尋...">
  <button class="btn btn-sm"><i class="mdi mdi-magnify"></i></button>
</div>

<!-- Select with live search (bootstrap-select) -->
<select class="selectpicker form-control theme-light" data-live-search="true">
  <option>選項一</option>
  <option>選項二</option>
  <option>選項三</option>
</select>

<!-- Modal search group -->
<div class="input-group modal-group">
  <span class="input-group-text">搜尋條件</span>
  <input type="text" class="form-control" placeholder="輸入關鍵字">
  <button class="btn btn-primary"><i class="mdi mdi-magnify"></i></button>
</div>
```

### Key Classes / Attributes

| Class / Attribute | Role |
|---|---|
| `.header-form` | Search container within header area |
| `.selectpicker` | Bootstrap-select enhanced dropdown |
| `.theme-light` | Light theme for select elements |
| `data-live-search="true"` | Enables type-to-filter in selectpicker |
| `.input-group` | Bootstrap input group for combined controls |
| `.modal-group` | Input group variant for modal context |

### Behavioral Notes

- `bootstrap-select` plugin provides enhanced dropdowns with search. See `DESIGN-engineering.v2.1.md` §6 for plugin details.
- `UI-Forms.js` handles form-level behaviors including search interactions.
- `.header-form` is used for quick-search in the page header area.
- `.modal-group` pattern is used for search within modal dialogs.

### Rules

1. **SHOULD** use `bootstrap-select` with `data-live-search="true"` for searchable dropdowns.
2. **MUST** use `.input-group` pattern for combined search input + button.
3. Search icon **MUST** use `mdi-magnify`.
4. Standard search/filter bar layout is not yet fully standardized across system pages `<!-- pending confirmation -->`.
5. For patient name search that triggers a modal lookup, see §15 Patient Context Strip.

---

## 15. Patient Context Strip / personal-info-container

### Purpose

Pinned patient identification and clinical context block. Appears at the top of system pages as `block-type="header"`. Provides persistent patient identity anchor to prevent clinical errors. B-only component — not found in Library A.

### Canonical HTML

```html
<system-block block-type="header">
  <div class="personal-info-container">
    <!-- Patient name with search trigger -->
    <div class="personal-name">
      <h1>陳美美</h1>
      <button class="btn" data-bs-toggle="modal" data-bs-target="#PatientSearch">
        <i class="mdi mdi-magnify"></i>
      </button>
    </div>

    <!-- Demographics -->
    <div class="personal-info-main">
      <dl class="dl-list">
        <dt>性別</dt><dd>女</dd>
        <dt>出生日期/年齡</dt><dd>1985-03-15 / 40歲</dd>
        <dt>身分證號</dt><dd>A234****89</dd>
        <dt>病歷號</dt><dd>12345678</dd>
      </dl>
    </div>

    <!-- Status badges -->
    <div>
      <span class="badge bg-gray">門診</span>
      <span class="badge bg-dark">初診</span>
    </div>

    <!-- Quick actions -->
    <div class="d-flex gap-1">
      <button class="btn btn-sm btn-outline-primary">
        <i class="mdi mdi-history"></i> 歷史
      </button>
    </div>
  </div>
</system-block>
```

### Key Classes / Attributes

| Class / Attribute | Role |
|---|---|
| `.personal-info-container` | Top-level patient context wrapper |
| `.personal-name` | Patient name section (h1 + search modal trigger) |
| `.personal-info-main` | Demographics area using dl-list |
| `.dl-list` | Definition list for key-value demographics |
| `.badge.bg-gray` / `.badge.bg-dark` | Clinical status badges |
| `block-type="header"` | Pinned header block within system-container |

### Behavioral Notes

- Found in 21 system pages — indicates this is the standard patient-context pattern.
- The `.personal-name` section typically includes a modal trigger for patient search/lookup.
- Demographics use `.dl-list` definition-list pattern for structured key-value display.
- Badges display clinical status (visit type, patient category, special flags).
- Quick action buttons provide shortcuts to related workflows (history, notes, etc.).
- This component is PINNED (does not scroll) — it lives in `block-type="header"` per `DESIGN.v2.1.md` §2.4 pinning rules.

### Rules

1. **MUST** be placed inside `system-block[block-type="header"]` for pinned behavior.
2. **MUST** include `.personal-info-container` as the top-level wrapper.
3. Patient name **SHOULD** include a search/lookup trigger for quick patient switching.
4. Demographics **MUST** use `.dl-list` pattern (not tables or custom layouts).
5. Status badges **MUST** use `.badge.bg-*` with semantic colors.
6. This component is B-only — **MUST NOT** be used in Library A pages.
7. Per `DESIGN.v2.1.md` §2.4: patient identity **MUST** be pinned because removing it from view could cause clinical errors.

---

## Changelog

| Date | Sections | Summary |
|------|----------|---------|
| 2025-07-21 | 1–10 | Initial component specifications extracted from design authority sources |
| 2025-07-21 | 1–15 | v2.1 refinement: removed B-Only UI Modules inventory (→ engineering doc), added Breadcrumbs, Tabs, Alerts and Notifications, Empty States, Search and Filter Bar, Patient Context Strip |
