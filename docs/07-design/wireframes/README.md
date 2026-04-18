# Wireframes — First-Pass Directory

## Round Scope

This directory contains the **first-pass low-fidelity wireframes** for two clusters:
- **Cases cluster** (P1 baseline + P2 extension): UI-009 ~ UI-014
- **Sampling cluster** (補齊至 handoff 評估程度): UI-006, UI-007

---

### Cases Cluster（A1–A5 archetypes）

Round status: P1 structural wireframes complete; P2 extension added using the same page-role-first method.

| Page | File | Archetype | Status |
| --- | --- | --- | --- |
| UI-013 複審作業頁 | [ui013-rereview-first-pass.md](ui013-rereview-first-pass.md) | A4 Action / Review Form | ✅ P1 first-pass complete |
| UI-014 Revision 歷程頁 | [ui014-revision-history-first-pass.md](ui014-revision-history-first-pass.md) | A5 History / Traceability | ✅ P1 first-pass complete |
| UI-010 案件詳情頁 | [ui010-case-detail-first-pass.md](ui010-case-detail-first-pass.md) | A2 Detail / Control Center | ✅ P1 first-pass complete |
| UI-012 初審作業頁 | [ui012-initial-review-first-pass.md](ui012-initial-review-first-pass.md) | A4 Action / Review Form | ✅ P2 first-pass complete |
| UI-011 案件分派頁 | [ui011-assignment-first-pass.md](ui011-assignment-first-pass.md) | A3 Action / Assignment | ✅ P2 first-pass complete |
| UI-009 案件列表頁 | [ui009-case-list-first-pass.md](ui009-case-list-first-pass.md) | A1 List / Work Queue | ✅ P2 first-pass complete |

P2 extension coordination brief: [ui-wireframe-brief-p2-extension.md](../ui-wireframe-brief-p2-extension.md)

---

### Sampling Cluster（SA1–SA2 archetypes，首次定義）

Archetype 定義範圍：SA1 / SA2 為 sampling cluster 專屬。A1–A5 為 cases cluster 專屬。兩組 archetype 不互用。

| Page | File | Archetype | Status |
| --- | --- | --- | --- |
| UI-006 抽選執行頁 | [ui006-sampling-execute-first-pass.md](ui006-sampling-execute-first-pass.md) | SA1 Action / Preview-Execute | ✅ first-pass complete |
| UI-007 抽選批次列表頁 | [ui007-batch-list-first-pass.md](ui007-batch-list-first-pass.md) | SA2 List / Execution Tracking | ✅ first-pass complete |

**Pending Confirmation — Sampling Cluster**:

| # | 問題 | 影響頁面 |
|---|---|---|
| OQ-S01 | preview response 是否包含 `selectedCount`？（UI-006 第4 stat card） | UI-006 |
| OQ-S02 | execute response 缺 startedAt/finishedAt；SUCCESS variant 是否需 extra GET？ | UI-006 |
| OQ-S04 | SYSTEM_ADMIN SamplingBatch metadata projection 欄位白名單未定義 | UI-007 |
| OQ-S05 | REVIEWER / REREVIEWER route guard 行為未確認 | UI-007 |
| OQ-S06 | UI-008 page spec 不存在；UI-007 的 [查看詳情] 目前屬 deferred / 未啟用 detail action | UI-007 |

P2 extension coordination brief: [ui-wireframe-brief-p2-extension.md](../ui-wireframe-brief-p2-extension.md)

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
| --- | --- |
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
| --- | --- | --- | --- |
| C16 ReviewCommandBar | UI-013 | 5 variants with different action sets; CLOSED exception must be distinct from READONLY | Explicit per-variant button state table |
| C16 ReviewCommandBar | UI-012 | Draft / submit / revision 的 command 邊界若畫錯，會把初審頁誤畫成一般編輯頁 | Explicit mode-driven command set and append-only revision note |
| C21 RevisionCompareSummaryRegion | UI-014 | FULL vs METADATA_ONLY structure differs; metadata must mask content | Two explicit variant structures |
| C22 RevisionNavigationActionZone | UI-014 | Must never look like a submit bar; exception entry = navigate only | Annotated as navigate-only with explicit prohibition |
| CLOSED revision-exception | UI-013 / UI-014 / UI-010 | O1-C cross-page design; easy to wrongly block or wrongly enable submission | Per-page explicit treatment |

> P2 沒有新增 repo 未出現的 high-risk target；僅繼承既有 brief 已標示的 C16 風險，並把 UI-009 row action overflow、UI-011 assignment mode 對照列為 review focus，而非新的高風險元件。

---

## Key Decisions Used (from decision-log.md)

| Decision | Impact |
| --- | --- |
| O1-C | CLOSED case: new submit blocked, rereview revision allowed for original rereviewer; UI-014 shows conditional navigate entry |
| O2-A | API paths are case-rooted; use `case-rooted` paths in binding |
| O3-C | metadata-only = shared base + per-page extension whitelist; same page, different projection |

---

## Remaining Candidate Issues for `ui-design-decisions.md`

The following items remain open across the three wireframes after the approved backwrite for DD-001 / DD-003 / DD-004 / DD-005 / DD-006 / DD-007 / DD-008:

### Category: Layout / Visual Priority

1. C02 vs C03 優先序 (all pages): when both blocked banner and mode indicator must show

### Category: Information Architecture

1. C10/C11 cross-role summary visibility (UI-010): can reviewer see C11?

### Category: Visual Language

1. Effective version badge design token (UI-014): current vs superseded colour semantics
1. UI-014 stage tab 預設切換策略 (interaction vs routing)

### Category: Deferred Reactivation

1. C24 RevisionLifecycleEntryArea 細分策略 (UI-010): single zone vs sub-zones
1. C16 CLOSED exception button label 文案統一 (UI-013)

---

## Wireframe Production Notes

- All files are Markdown with ASCII layout diagrams
- Each file includes: page identity, source alignment, structural region map, variant set, shared structure rules, high-risk notes, placeholder status, open questions
- Component codes (C01–C24) reference `ui-component-map.md` directly
- Variant codes (V1–Vn) are page-local identifiers, not cross-page
- All file names follow pattern: `ui{NNN}-{slug}-first-pass.md`
- `figma-frame-spec.md` is a translation spec only; it does not prove a Figma artifact exists
