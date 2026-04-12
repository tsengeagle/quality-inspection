# frontend-back-merge-backlog.md

## 1. Purpose

This backlog records the **confirmed true conflicts** that still exist between the current MFE-specific frontend governance package and the broader frontend baseline.

It is not a general cleanup list. It is a **governance follow-up backlog** for conflicts that are already resolved operationally by the MFE package, but still need to be formalized back into the broader baseline files.

## 2. Scope

Included here:
- only **true conflicts**
- only items already confirmed by `frontend-docset-review-r1.md`
- only items that require broader baseline back-merge

Excluded from this backlog:
- acceptable layered distinctions
- wording overlap only
- broader baseline items that are merely less detailed than the MFE package
- new rules not already decided in the current document set

## 3. Confirmed True Conflicts

| Conflict | Current broader baseline statement | Current MFE-specific temporary statement | Conflict type |
|---|---|---|---|
| Breadcrumbs | `DESIGN-components.v2.1.md` says system pages **MUST** use breadcrumbs in header position | `frontend-design-language-mfe.md` says hosted tabbed-shell MFE pages **omit breadcrumbs** | Rule override formalization |
| In-page tabs | `DESIGN-components.v2.1.md` says tabs **MUST** use `nav-tabs-custom` | `frontend-design-language-mfe.md` says MFE in-page tabs **MUST** use `nav-tabs-scoped` | Component baseline correction |
| `table-custom` requirement | `DESIGN-components.v2.1.md` says `table-custom` **MUST** be present on all tables | `frontend-design-language-mfe.md` allows domain-specific variants to omit `table-custom` when they provide full base styling | Rule override formalization / component baseline correction |

No additional true conflicts are currently confirmed by the review artifact.

## 4. Current Temporary Owners

| Conflict | Current temporary MFE owner | Why this file temporarily owns the answer |
|---|---|---|
| Breadcrumbs | `frontend-design-language-mfe.md` §2.2, supported by `frontend-mfe-decisions-r1.md` DEC-001 | This file currently provides the MFE-safe operational answer for hosted tabbed-shell runtime |
| In-page tabs | `frontend-design-language-mfe.md` §5.2, with short canonical reinforcement in `frontend-mfe-governance.md` §7.2 and rationale in DEC-004 | The MFE design language file currently owns the in-page tab answer for module-local tabs |
| `table-custom` requirement | `frontend-design-language-mfe.md` §6.1–§6.2, supported by DEC-006 / review findings | The MFE design language file currently owns the only explicit MFE-safe clarification for domain-specific table variants |

## 5. Broader Baseline Back-Merge Targets

| Conflict | Target broader file(s) | Back-merge scope | Suggested change type |
|---|---|---|---|
| Breadcrumbs | `docs/design/DESIGN.v2.1.md`, `docs/design/DESIGN-components.v2.1.md` | Add explicit hosted-shell MFE exception/override so breadcrumb omission is no longer in conflict with broader system-page guidance | Rule override formalization + wording update |
| In-page tabs | `docs/design/DESIGN-components.v2.1.md` | Clarify that `nav-tabs-custom` remains broader baseline, but MFE in-page tabs under shell-hosted runtime use `nav-tabs-scoped` | Component baseline correction |
| `table-custom` requirement | `docs/design/DESIGN-components.v2.1.md` and, if needed, `docs/design/DESIGN.v2.1.md` shared-rules wording | Clarify that `table-custom` remains the standard base for clinical tables, but domain-specific variants may omit it when they provide complete base styling | Rule override formalization + component baseline correction |

## 6. Priority Order

1. **Breadcrumbs**
   - Highest priority because it changes page-level navigation guidance for hosted MFE pages.
   - Current broader baseline can still push agents toward adding redundant breadcrumbs unless back-merged.

2. **In-page tabs**
   - High priority because it affects a common MFE interaction pattern and currently points to a wrong tab class in broader component guidance.

3. **`table-custom` requirement**
   - Third priority because it is narrower than breadcrumbs/tabs, but still produces a real broader-vs-MFE rule mismatch.

## 7. Recommended Merge Style

Use the following merge style per backlog item:

| Conflict | Recommended merge style |
|---|---|
| Breadcrumbs | **Rule override formalization** — explicitly state that hosted tabbed-shell MFE pages omit breadcrumbs, while broader standalone/system pages may still use the canonical breadcrumb pattern |
| In-page tabs | **Component baseline correction** — add a shell-hosted MFE in-page tab variant note rather than replacing the broader tab baseline outright |
| `table-custom` requirement | **Rule override formalization + component baseline correction** — keep `table-custom` as the standard base class, but formalize the already-reviewed exception for domain-specific variants |

Back-merge should be **minimal and surgical**. Do not reopen R1 decisions. Do not redesign the entire broader baseline.

## 8. Pending Confirmation

The following are not blockers for this backlog, but should be checked during the actual back-merge work:

1. Whether breadcrumbs should be updated in both `DESIGN.v2.1.md` and `DESIGN-components.v2.1.md`, or if one file should own the explicit MFE exception and the other only cross-reference it.
2. Whether the `table-custom` clarification should live only in `DESIGN-components.v2.1.md`, or whether `DESIGN.v2.1.md` also needs a shorter shared-rules note.
3. Whether any future additional true conflicts emerge after the broader baseline is re-read during actual back-merge work. If they do, they should be added only if they meet the same bar as the three confirmed items above.
