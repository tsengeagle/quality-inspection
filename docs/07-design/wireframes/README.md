# Wireframes — Cases Cluster First-Pass

## Round Scope

This directory contains the **first-pass low-fidelity wireframes** for the cases cluster **P1 pages**.  
Round: P1 structural wireframes only.

| Page | File | Archetype | Status |
|---|---|---|---|
| UI-013 複審作業頁 | [ui013-rereview-first-pass.md](ui013-rereview-first-pass.md) | A4 Action / Review Form | ✅ First-pass complete |
| UI-014 Revision 歷程頁 | [ui014-revision-history-first-pass.md](ui014-revision-history-first-pass.md) | A5 History / Traceability | ✅ First-pass complete |
| UI-010 案件詳情頁 | [ui010-case-detail-first-pass.md](ui010-case-detail-first-pass.md) | A2 Detail / Control Center | ✅ First-pass complete |

**Pending pages (P2 — not in this round)**:
- UI-012 初審作業頁 (A4)
- UI-011 案件分派頁 (A3)
- UI-009 案件列表頁 (A1)

## Artifact Status Note

- 這個目錄目前收錄的是 markdown low-fidelity wireframe artifacts。
- 這些檔案不是 Figma artifact，也不是可執行 prototype。
- 本目錄中的 `✅ First-pass complete` 只表示 markdown wireframe first-pass 已完成，不代表 Figma frame 已建立。
- 若需要 flow validation 或互動驗證，優先走本地 React / HTML low-fidelity prototype。

---

## What "First-Pass" Means

**Included**:
- Structural region layout (ASCII)
- Named components (C-code references)
- Variant set per page (distinct UI modes, projection, role, exception)
- High-risk region identification (C16, C21, C22)
- Shared structure placement rules
- Placeholder/deferred markers
- Open questions for future `ui-design-decisions.md`

**NOT included**:
- Visual baseline (colors, spacing, typography, icons)
- Angular component code
- Interaction spec (hover, animation, transitions)
- Final design decisions on open questions
- Form-level item spec (score sheet items)

---

## Authority Inputs Summary

| Layer | Files consulted |
|---|---|
| Decision authority | `docs/01-requirements/decision-log.md` (O1-C, O2-A, O3-C) |
| Domain state | `docs/02-ddd/state-machine.md` |
| Permission | `docs/02-ddd/permission-matrix.md` |
| Page specs | `docs/05-ui/ui-page-spec-ui010/013/014.md` |
| Component map | `docs/06-frontend/ui-component-map.md` (C01–C24) |
| State/action matrix | `docs/06-frontend/ui-state-actions-matrix.md` |
| Wireframe brief | `docs/07-design/ui-wireframe-brief.md` |
| Layout archetypes | `docs/05-ui/ui-layout-archetypes.md` |
| Design method | `docs/06-frontend/frontend-design-method.md` |
| Review gate | `docs/06-frontend/frontend-review-gate-pending-items.md` |

---

## High-Risk Targets Summary

| Component | Page | Risk | First-pass approach |
|---|---|---|---|
| C16 ReviewCommandBar | UI-013 | 5 variants with different action sets; CLOSED exception must be distinct from READONLY | Explicit per-variant button state table |
| C21 RevisionCompareSummaryRegion | UI-014 | FULL vs METADATA_ONLY structure differs; metadata must mask content | Two explicit variant structures |
| C22 RevisionNavigationActionZone | UI-014 | Must never look like a submit bar; exception entry = navigate only | Annotated as navigate-only with explicit prohibition |
| CLOSED revision-exception | UI-013 / UI-014 / UI-010 | O1-C cross-page design; easy to wrongly block or wrongly enable submission | Per-page explicit treatment |

---

## Key Decisions Used (from decision-log.md)

| Decision | Impact |
|---|---|
| O1-C | CLOSED case: new submit blocked, rereview revision allowed for original rereviewer; UI-014 shows conditional navigate entry |
| O2-A | API paths are case-rooted; use `case-rooted` paths in binding |
| O3-C | metadata-only = shared base + per-page extension whitelist; same page, different projection |

---

## Candidate Issues for `ui-design-decisions.md`

The following items were identified across the three wireframes as **open questions** that require design decisions **before visual baseline work can begin**:

### Category: Layout / Visual Priority

1. C16 sticky / fixed 策略 (UI-013): long form scroll time command bar visibility
2. C02 vs C03 優先序 (all pages): when both blocked banner and mode indicator must show
3. C04 vs C02 版面排列 (UI-010, UI-014): metadata-only + terminal state combined banner

### Category: Exception UX

4. C02 CLOSED exception vs READONLY_BLOCKED 視覺語意分化 (UI-013)
5. C03 CLOSED_REVISION_EXCEPTION mode label 文案 (UI-013)
6. C22 exception entry 控件類型: link vs outlined button (UI-014)

### Category: Information Architecture

7. C21 METADATA_ONLY compare 遮罩層級 (UI-014): N題遮罩 vs 完全不顯示比較區
8. C08 action visible vs disabled 分離策略 (UI-010)
9. C10/C11 cross-role summary visibility (UI-010): can reviewer see C11?

### Category: Visual Language

10. Effective version badge design token (UI-014): current vs superseded colour semantics
11. UI-014 stage tab 預設切換策略 (interaction vs routing)

### Category: Deferred Reactivation

12. C24 RevisionLifecycleEntryArea 細分策略 (UI-010): single zone vs sub-zones
13. C16 CLOSED exception button label 文案統一 (UI-013)

---

## Wireframe Production Notes

- All files are Markdown with ASCII layout diagrams
- Each file includes: page identity, source alignment, structural region map, variant set, shared structure rules, high-risk notes, placeholder status, open questions
- Component codes (C01–C24) reference `ui-component-map.md` directly
- Variant codes (V1–Vn) are page-local identifiers, not cross-page
- All file names follow pattern: `ui{NNN}-{slug}-first-pass.md`
- `figma-frame-spec.md` is a translation spec only; it does not prove a Figma artifact exists
