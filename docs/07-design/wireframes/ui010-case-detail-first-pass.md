# UI-010 案件詳情頁 — First-Pass Wireframe

> First-pass low-fidelity structure. Not visual baseline. Not Angular code.  
> This is a **control center page** — all action zones are navigate-only.  
> No direct commands or form submits appear on this page.

---

## 1. Page Identity

| 欄位 | 值 |
|---|---|
| pageId | `UI-010` |
| pageName | 案件詳情頁 |
| archetype | A2 Detail / Control Center |
| primary user goal | 了解案件全貌、導向各作業頁面（分派/初審/複審/歷程） |
| primary actors | HOSPITAL_MANAGER / REVIEWER / REREVIEWER / AUDITOR |
| page type | control center — navigate-only, no commands |

**明確禁止**：本頁不出現直接 submit / save draft / assign command；所有 CTA 均是「前往...」語意。

---

## 2. Source Alignment

| 類型 | 來源 |
|---|---|
| Archetype | `ui-layout-archetypes.md` A2 Detail / Control Center |
| Component candidates | C01 / C02 / C03 / C04 / C08 / C09 / C10 / C11 / C24 deferred（component-map.md） |
| Page spec authority | `ui-page-spec-ui010案件詳情頁.md` |
| State / action policy | `ui-state-actions-matrix.md` §4.3 |
| Domain state | `state-machine.md` §3.1/3.3 |
| Permission guard | `permission-matrix.md` §4 UI-010 row |
| Decision backwrite | O1-C（CLOSED + 合法 rereviewer → case detail 顯示 CLOSED 提示；不在本頁開 command） |
| Metadata whitelist | O3-C（shared base + UI-010 extension: patientAge/patientGender/admissionDate/dischargeDate） |
| Wireframe brief | `ui-wireframe-brief.md` §7 UI-010 |

---

## 3. Structural Region Map（基線版型 — FULL, HOSPITAL_MANAGER 為代表）

```
┌─────────────────────────────────────────────────────────────┐
│ C01  CaseSummaryHeader                                      │
│  [caseId | project | medicalRecord | hospitalId]           │
│  [status badge | reviewer | rereviewer | version]          │
├─────────────────────────────────────────────────────────────┤
│ C04  ProjectionNoticeBanner  [METADATA_ONLY 時才出現]       │
│  [metadata-only 視角：部分欄位已遮罩]                       │
├─────────────────────────────────────────────────────────────┤
│ C02  StatusOrBlockedBanner  [條件出現]                      │
│  [terminal: CLOSED / CANCELLED 通知 | scope denied]        │
├─────────────────────────────────────────────────────────────┤
│ C03  ModeIndicatorStrip                                     │
│  [FULL_CONTROL_CENTER / ROLE_LIMITED / METADATA_ONLY]      │
├─────────────────────────────────────────────────────────────┤
│ C08  CaseStatusActionPanel  [navigate-only]                │
│                                                             │
│  ── Action Zone ──                                          │
│  [前往分派作業]   ← 僅 HOSPITAL_MANAGER + 合法狀態         │
│  [前往初審作業]   ← 僅 REVIEWER(assigned) + 合法狀態       │
│  [前往複審作業]   ← 僅 REREVIEWER(assigned) + 合法狀態     │
│  [前往 Revision 歷程]  ← 所有合法 role                     │
│  [CLOSED: 顯示提示訊息，不顯示作業入口]                    │
│  [CLOSED excep: 顯示 "案件已結案：合法複審可前往修正歷程"] │
│                                                             │
│  [所有按鈕語意: 前往 / navigate — 無 submit / assign CTA]  │
├─────────────────────────────────────────────────────────────┤
│ C09  TraceabilitySummaryRegion                              │
│  [batchId | samplingRule | samplingDate | batchStatus]     │
│  [assignmentHistory: reviewerId | rereviewerId | dates]    │
├─────────────────────────────────────────────────────────────┤
│ C10  InitialReviewSummaryRegion  [reference-only]          │
│  [initialScore | submittedAt | reviewerId | revisionCount]  │
│  [FULL: summary+comment摘要 | METADATA: 摘要遮罩]          │
├─────────────────────────────────────────────────────────────┤
│ C11  RereviewSummaryRegion  [reference-only]               │
│  [rereviewScore | submittedAt | rereviewerId | finalDecision] │
│  [FULL: summary+comment摘要 | METADATA: 摘要遮罩]          │
│  [absent 若尚無 rereview]                                   │
├─────────────────────────────────────────────────────────────┤
│ C24  RevisionLifecycleEntryArea  [deferred placeholder]    │
│  [TBD: revision / lifecycle 入口組合區]                    │
│  [★ placeholder — 實作細節待 lifecycle API 穩定後決定]     │
└─────────────────────────────────────────────────────────────┘
```

> **Region 順序原則**：識別（C01）→ 降級提示（C04）→ 狀態警示（C02）→ 行為模式（C03）→ 導頁操作（C08）→ 追蹤（C09）→ 摘要（C10/C11）→ revision 入口（C24 placeholder）。
>
> **C08 在此頁不是 command bar**，不能被誤設計成 A4 頁的 ReviewCommandBar（C16）語意。

---

## 4. Variant Set

### V1 — FULL_CONTROL_CENTER（HOSPITAL_MANAGER 或 AUDITOR 全視角）

```
C01: [Summary Header — FULL: all clinical + assignment fields]
C04: [absent]
C02: [absent or non-blocking info]
C03: [Mode = FULL_CONTROL_CENTER]
C08: [Action Panel]:
     [前往分派作業 — if manager + assignable]
     [前往初審作業 — if reviewer status match]
     [前往複審作業 — if rereview status match]
     [前往 Revision 歷程 — always visible]
C09: [Traceability summary — FULL]
C10: [Initial summary — FULL (若有初審)]
C11: [Rereview summary — FULL (若有複審)]
C24: [Placeholder — "revision lifecycle entry"（TBD）]
```

---

### V2 — ROLE_LIMITED_CONTROL_CENTER

> Actor 為 REVIEWER 或 REREVIEWER，僅可看自己範圍的作業入口。

```
C01: [Summary Header — FULL within scope]
C04: [absent]
C02: [absent]
C03: [Mode = ROLE_LIMITED_CONTROL_CENTER]
C08: [Action Panel — role-filtered]:
     [REVIEWER]:
       [前往初審作業 — if assigned + status match]
       [前往 Revision 歷程]
       [分派作業 → hidden]
       [複審作業 → hidden]
     [REREVIEWER]:
       [前往複審作業 — if assigned + status match]
       [前往 Revision 歷程]
       [分派作業 → hidden]
       [初審作業 → hidden or link only]
C09: [Traceability — limited: own assignment info]
C10: [Initial summary — FULL if reviewer / limited if rereviewer]
C11: [Rereview summary — FULL if rereviewer / limited if reviewer]
C24: [Placeholder]
```

**Structural difference**: C08 action set differs by role; C09/C10/C11 visibility differs; C03 = ROLE_LIMITED.

---

### V3 — METADATA_CONTROL_CENTER（SYSTEM_ADMIN）

> System Admin 進入，projection = metadata-only。作業入口仍存在但受限。

```
C01: [Summary Header — metadata-only: shared base + UI-010 extension]
     [visible: caseId / caseStatus / projectName / hospitalId / reviewer / rereviewer]
     [visible ext: patientAge / patientGender / admissionDate / dischargeDate]
     [hidden: clinical detail, specific content]
C04: [★ ProjectionNoticeBanner = VISIBLE]
     [banner: "目前為 metadata-only 視角，部分欄位已遮罩"]
C02: [absent or minimal]
C03: [Mode = METADATA_CONTROL_CENTER]
C08: [Action Panel — sysadmin scope]:
     [前往分派作業 → hidden (N in permission matrix)]
     [前往初審作業 → hidden (N)]
     [前往複審作業 → hidden (N)]
     [前往 Revision 歷程 → visible (R/metadata only)]
     [content: "系統管理員可查看歷程（metadata-only 視角）"]
C09: [Traceability — metadata fields only]
C10: [Initial summary — masked: show structure, hide content]
     ["答案摘要已遮罩"]
C11: [Rereview summary — masked]
C24: [Placeholder — limited or absent for sysadmin]
```

**Structural difference**: C04 visible；C08 minimal actions；C10/C11 masked summary；C03 = METADATA.

---

### V4 — terminal blocked（CLOSED / CANCELLED）

```
C01: [Summary Header — FULL or metadata (by role)]
C04: [METADATA_ONLY 時出現]
C02: [★ terminal notice]:
     CLOSED: "案件已結案，不可再執行審查作業，僅可查看歷程"
     CANCELLED: "案件已取消"
C03: [Mode = terminal]
C08: [Action Panel — minimal]:
     [★ 所有作業入口 hidden or disabled]
     [前往 Revision 歷程 — still navigate-able]
     [CLOSED 例外：顯示提示文字（不是 CTA button）]
C09: [Traceability — FULL]
C10: [Initial summary — FULL (readonly)]
C11: [Rereview summary — FULL (readonly)]
C24: [Placeholder — deferred]
```

**Structural difference**: C02 terminal notice prominent；C08 minimal；作業入口全關閉。

---

### V5 — closed with navigate-only hint for rereviewer（O1-C）

> 案件 CLOSED，actor 是合法 rereviewer，但 UI-010 本身 NOT a command page。

```
C01: [Summary Header — FULL]
C04: [absent]
C02: [CLOSED notice: "案件已結案"]
C03: [Mode = FULL_CONTROL_CENTER or ROLE_LIMITED depending on role]
C08: [Action Panel]:
     [前往 Revision 歷程 — navigate only]
     [顯示提示: "如需複審修正，請從 Revision 歷程頁進入"]
     [★ 不在此頁提供直接 revision 作業 CTA]
     [★ 不出現「提交複審修正」button — 這是 UI-013 的責任]
C09/C10/C11: [readonly summaries — FULL]
C24: [Placeholder]
```

**Structural difference**: C08 顯示提示文字（導向 UI-014）而非 command button；UI-010 不承擔命令發起責任。

---

## 5. Shared Structure Placement

| Component | Role in UI-010 | Position |
|---|---|---|
| C01 CaseSummaryHeader | case 識別，全變體均顯示 | 頁首固定 |
| C04 ProjectionNoticeBanner | metadata-only 降級提示（sysadmin 必顯示） | C01 之後 |
| C02 StatusOrBlockedBanner | terminal / scope denied 訊號（條件出現） | C04 之後 |
| C03 ModeIndicatorStrip | 控制中心模式標示 | C02 之後，C08 之前 |

注意：C04 與 C02 可能同時出現（METADATA + CLOSED 組合）。兩者不可互相遮蓋；C04 先（告知降級視角），C02 次（告知狀態問題）。

---

## 6. C08 CaseStatusActionPanel Design Note

**C08 必須保持 navigate-only 語意**：

| 可出現的控件 | 禁止出現的控件 |
|---|---|
| 「前往...」link 或 outlined button | 任何 submit/assign/save CTA |
| 「查看...」link | 任何 primary/danger action button |
| 狀態說明文字 | Form field / input |
| 角色提示文字 | Dropdown selection |

**C08 vs C16 的語意區分**：
- C08（UI-010 navigate actions）= 「我要去哪裡做事」
- C16（UI-012/013 command bar）= 「我要在這裡提交什麼」

任何將 C08 畫成 C16 的設計都必須回頭修正。

---

## 7. Placeholder / Deferred Notes

| Region | 狀態 | 下一步 |
|---|---|---|
| C24 RevisionLifecycleEntryArea | deferred placeholder — 僅標位置 | 待 lifecycle API 欄位粒度與排序策略確認後補齊 |
| C10/C11 masked field density | 只標「有遮罩」，不定稿遮罩層級 | 等 O3 metadata whitelist 細節確認 |
| C08 role-specific action 細節 | 畫出 role 差異但不定稿 button 樣式 | 等 interaction designer review |
| CANCELLED 案件的 C08 狀態 | 標 disabled，不定稿 UX copywriting | 後續 UX 補充 |
| action 可見性 vs 可用性分問題 | first-pass 只區分 hidden / visible | 等 action policy implementation detail |

---

## 8. Open Questions for Design Decisions

> 以下議題第一輪 wireframe 無法單方面拍板，需進 `ui-design-decisions.md`。

1. **C04 與 C02 同出現時的版面排列**：metadata-only + CLOSED 時兩個 banner 如何堆疊？是否可合併成一個複合 banner？
2. **C08 navigate action 可見性 vs 可用性分離**：當 role 合法但 status 不允許時，action 是 hidden 還是 visible+disabled+reason？（影響使用者認知）
3. **C24 placement**：revision lifecycle entry area 最終在 C11 之後，還是側邊欄？（待 prototype 驗證）
4. **CLOSED exception hint 的 copywriting**：「如需複審修正，請從 Revision 歷程頁進入」的實際文案應在 UX writing 層決定。
5. **C03 mode strip 在 METADATA_CONTROL_CENTER 的文案**：「metadata-only」字樣是否直接暴露給使用者，還是用更友善文案替換？
6. **C10/C11 在 ROLE_LIMITED 時的可見性規則**：REVIEWER 能看 rereview summary (C11) 嗎？REREVIEWER 能看 initial summary (C10) 的頁面入口嗎？（需回頭確認 permission-matrix）
