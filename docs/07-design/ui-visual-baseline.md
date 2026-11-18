# UI Visual Baseline v1 — P1 Cases Cluster

> D-032 規格變更（2026-11-18 補註）：下文涉及 O1-C、CLOSED 例外修正入口（UI-014 C22「前往複審修正（例外）」、`hasExceptionEntry`、UI-013 `CLOSED_REVISION_EXCEPTION` 模式及其 amber 提示）的內容，已被 D-032 取代，僅保留歷史依據，不作現行可實作規則。CLOSED 不得 revision／reopen；UI-013、UI-014 在 CLOSED 只顯示唯讀提示。應用程式已對齊：UI-013 CLOSED 唯讀（PR #39）；UI-014 移除 `hasExceptionEntry` 與 C22 例外入口（PR #83）。C22 的其他導頁設計（例如返回案件詳情）不在 D-032 撤銷範圍，目前應用程式沒有實作 C22。

> 本文件固定 **P1（UI-010 / UI-013 / UI-014）已收斂的共通視覺基線**。  
> 供後續 wireframe refinement、P2 extension 與 implementation handoff 前的共同視覺語言參照。  
> 非 brand system 規範，非 CSS token catalog，非 Angular component API。

---

## 1. 文件目的

本文件基於 P1 wireframe first-pass、prototype review harness、DD-001~DD-008 已批准設計決策，
以及 ui-wireframe-brief.md 凍結的 shared structure 規則，
**將已收斂的視覺語意固定下來**，讓 wireframe refinement、prototype validation 與 Angular page skeleton 三者
能引用同一份 visual baseline，而不是各自解讀分散的 authority。

本文件 **不打開** 任何已批准的 design decision。  
本文件 **不改動** domain / permission / API truth。  
本文件 **不涵蓋** P2 頁面（UI-009 / UI-011 / UI-012）。  
本文件 **不宣稱** 任何 Portal51 color token 已確認可用。

---

## 2. Source Inputs

| 來源 | 路徑 | 採用內容 |
|------|------|---------|
| Design decisions | `docs/07-design/ui-design-decisions.md` | DD-001 ~ DD-008 全部已批准結論 |
| Wireframe brief | `docs/07-design/ui-wireframe-brief.md` | S1~S4 shared structure 規則、§6/§7 per-page brief |
| P1 wireframes | `docs/07-design/wireframes/ui010-case-detail-first-pass.md` | 結構版型、variant set、region 順序 |
| P1 wireframes | `docs/07-design/wireframes/ui013-rereview-first-pass.md` | C16 variant 差異、exception 結構 |
| P1 wireframes | `docs/07-design/wireframes/ui014-revision-history-first-pass.md` | C21/C22 high-risk、metadata masking |
| Wireframes README | `docs/07-design/wireframes/README.md` | High-risk 摘要、key decisions summary |
| Component map | `docs/06-frontend/ui-component-map.md` | C01~C22 分類、ownership、notes |
| Layout archetypes | `docs/05-ui/ui-layout-archetypes.md` | A2/A4/A5 定義 |
| Review gate | `docs/06-frontend/frontend-review-gate-pending-items.md` | O1-C / O2-A / O3-C adopted |
| State / action policy | `docs/06-frontend/ui-state-actions-matrix.md` | action 規則基線 |
| Frontend design method | `docs/06-frontend/frontend-design-method.md` | 設計方法論基線 |

---

## 3. Scope

### 本輪覆蓋

- P1 三頁面：UI-010（控制中心）、UI-013（複審命令頁）、UI-014（Revision 歷程頁）
- 共用 shared region baseline（C01/C02/C03/C04）
- 語意 tone baseline（info / blocked / exception / metadata-only）
- High-risk target baseline（C16 / C21 / C22）
- Placeholder / deferred 的表達原則

### 本輪 **不** 覆蓋

- Domain rule / permission policy / API contract（不改動、不補充）
- Brand design system 或 Portal51 token catalog（無 repo authority 支持）
- P2 頁面（UI-009 / UI-011 / UI-012）
- Angular component API / CSS 實作規格
- 附件上傳、AI 輔助功能等尚未在 P1 authority 的互動細節

---

## 4. Visual Baseline Principles

以下為跨頁面一致的視覺基線原則。若具體頁面有衝突，以 §6 per-page baseline 為準。

### 4.1 Hierarchy First

頁面上方的 region 優先於頁面下方。  
使用者取得識別（C01）→ 確認可操作性（C02）→ 確認行為模式（C03）→ 再進入主要工作區。  
主要工作區不得視覺上壓過 header + banner 層。

### 4.2 Semantics Before Decoration

視覺區分的充分理由是「語意不同」，而非樣式多樣化。  
- `blocked` 與 `exception` 必須視覺可區分，因為它們的操作後果根本不同。  
- `navigate action` 與 `submit command` 必須視覺可區分，因為混淆會導致意外提交。  
- metadata-only indicator 與 blocked banner 必須視覺可區分，因為一個是視角限制、一個是操作限制。

### 4.3 Low-Ambiguity Page Typing

每一頁的「頁面角色」應在進入後 3 秒內可被辨識：
- UI-010：唯讀控制中心，所有按鈕語意均為「前往」。
- UI-013：命令頁，主要 CTA 是提交或儲存。
- UI-014：歷程頁，只讀與導頁，不直接提交。

### 4.4 Projection Must Stay Visible

METADATA_ONLY（metadata-only）視角必須始終有可辨識的標示。  
標示形式為 C01 header 內的 inline indicator（subtitle / badge），不以獨立 banner row 呈現。  
即使其他 banner（C02）存在，也不可讓 projection indicator 消失。

### 4.5 Blocked vs Exception Must Be Visually Distinguishable

- `blocked`：使用者 **不可** 做任何事，應離開此頁或等待狀態改變。
- `exception`：使用者 **可以** 做有限度的事（複審修正），但這是非常規路徑，需注意。

兩者絕對不得共用相同的視覺 tone。  
`exception` 採用 amber / warning 語意；`blocked` 採用獨立的阻斷語意。

### 4.6 Navigate-Only vs Command Must Not Be Visually Confused

UI-010 和 UI-014 是 navigate-only 頁。它們的 action zone 必須看起來像「入口/連結」，而非「命令/提交按鈕」。  
UI-013 是 command page。C16 是命令列，視覺重量明確高於導頁入口。  
特別禁止：在 history page（UI-014）出現與 C16 類似的固定底部 bar。

---

## 5. Shared Region Baseline

本節固定跨 P1 頁面共用的 C01~C04 region 的相對順序與呈現規則。

### 5.1 渲染順序（固定）

```
C01  CaseSummaryHeader       ← 始終顯示（含 metadata-only 的 inline indicator）
  └─ [inline C04]            ← metadata-only 時嵌入 C01，不獨立 row
C02  StatusOrBlockedBanner   ← 條件出現
C03  ModeIndicatorStrip      ← 始終顯示
```

多重 authority 交叉確認（prototype JSX、ui013/ui014/ui010 wireframe §2 source alignment）：
- C04 inline → C02（banner）→ C03（strip）
- C02 優先於 C03（狀態警告比行為模式更緊急）

### 5.2 C01 CaseSummaryHeader

| 屬性 | 規則 |
|------|------|
| 出現條件 | 始終顯示，不論 projection / blocked / exception |
| 視覺重量 | 最高層識別區，不可被下方區域壓過 |
| Metadata-only 時 | 仍顯示，但欄位依 per-page extension whitelist 降級 |
| Projection indicator | METADATA_ONLY 時，在 C01 範圍內嵌入 subtitle / badge / inline indicator；<br/>**不以獨立 banner row 呈現**（DD-003 已批准） |
| 跨頁一致性 | 同一 case 在 UI-010/UI-013/UI-014 的 C01 骨架相同；欄位密度視 archetype 調整 |

### 5.3 C02 StatusOrBlockedBanner

| 屬性 | 規則 |
|------|------|
| 出現條件 | terminal / blocked / exception / scope-denied 時條件出現 |
| 視覺重量 | 是頁面的主要警示／通知層，視覺優先度僅次於 C01 |
| 相對順序 | 在 C01 之後、C03 之前，不得換位 |
| tone 分類 | `info`（一般狀態通知）/ `blocked`（封鎖）/ `exception`（例外可繼續）— 三類不可混 |
| `blocked` tone | 語意：使用者不能做任何事；視覺：阻斷語意（與 exception 明確區分） |
| `exception` tone | 語意：非常規路徑但可繼續；視覺：amber / warning 語意（DD-006 已批准） |
| 與 C04 的關係 | C02 是主 banner，C04 降格為 C01 inline indicator；二者不競爭同一位置 |

### 5.4 C03 ModeIndicatorStrip

| 屬性 | 規則 |
|------|------|
| 出現條件 | 始終顯示（metadata-only 時可簡化為 read/navigate 語意） |
| 相對順序 | 在 C02 之後、主要工作區之前 |
| 視覺重量 | 低於 C02，作為語境識別條而非主要警示 |
| UI-013 特殊值 | `CLOSED_REVISION_EXCEPTION` 固定顯示為 **「例外修正（已結案）」**（DD-008 已批准）；不得與一般「修正模式」混淆 |
| 不得混淆之值 | `REVISION mode` ≠ `CLOSED_REVISION_EXCEPTION`；必須使用不同標籤 |

### 5.5 C04 ProjectionNoticeBanner（降格為 Inline Indicator）

> **此元件在複合狀態下已不再是獨立 banner row。**（DD-003 已批准）

| 屬性 | 規則 |
|------|------|
| 出現條件 | 僅 METADATA_ONLY projection 時顯示 |
| 呈現形式 | 嵌入 C01 作為 subtitle / badge / inline projection indicator |
| 視覺優先序 | 低於 C02；降級通知，不與 terminal/exception/blocked 競爭主位置 |
| 複合狀態下 | C04（metadata indicator）+ C02（status banner）同時出現時，各自在自己的區域，不合併、不遮蓋 |
| 適用頁面 | UI-010（V3 METADATA_CONTROL_CENTER）、UI-014（V5 METADATA_ONLY compare） |

---

## 6. Page Type Visual Baseline

### 6.1 UI-010 — Control Center Baseline

**頁面角色**：案件全貌閱讀與工作導頁中心，不執行任何 command。

| 特性 | 規則 |
|------|------|
| Primary focus region | C08 CaseStatusActionPanel — 使用者主要在此判斷下一步行動 |
| Secondary region | C09 / C10 / C11 — 追蹤與摘要，讓使用者有充足背景決策 |
| Action zone role | Navigate-only：所有 CTA 均為「前往...」語意；禁止出現 submit / assign / save command |
| 頁面角色識別 | 進入頁面時應立即能判斷：「這是選擇去哪裡的頁面，不是執行作業的頁面」 |
| C08 vs C16 區分 | C08（navigate action）不得被畫成 C16（command bar）外觀；不使用 fixed-bottom bar |
| ROLE_DENIED 處理 | action hidden（不顯示，避免混淆）（DD-007）|
| Status 暫時不符 | action visible + disabled + reason（讓使用者了解功能存在與當前無法使用的原因）（DD-007）|
| CLOSED exception hint | C08 顯示 disabled 原 action + 可見提示「請改從 Revision 歷程頁進入」；不在本頁開命令 |
| 底部 | 無 fixed-bottom command bar；C24 只是 placeholder，不得畫成實際 action bar |
| Projection | METADATA_ONLY 時 C04 inline，欄位依 UI-010 extension whitelist（patientAge/patientGender/admissionDate/dischargeDate）|

### 6.2 UI-014 — History / Traceability Baseline

**頁面角色**：閱覽 revision 演進、辨識 effective version、條件式導頁，不執行 command。

| 特性 | 規則 |
|------|------|
| Primary focus region | C20 RevisionTimelineRegion + C21 RevisionCompareSummaryRegion — 歷程閱讀是主要工作 |
| Secondary region | C22 RevisionNavigationActionZone — 離開或條件式導頁 |
| Action zone role | Navigate-only；C22 不是 submit bar |
| 頁面角色識別 | 進入頁面時應立即知道：「這是查閱歷程的頁面，我在這裡不提交任何東西」 |
| Effective version 強調 | Current effective version 在 C20 timeline 中必須有明確視覺強調（badge / highlight），與 superseded 版本可視覺區分 |
| CLOSED exception entry | C22 出現「前往複審修正（例外）」— warning-tone outlined navigate action（不是 filled button）（DD-004）|
| Exception entry vs submit | Exception entry 外觀必須明確低於 UI-013 C16 的命令按鈕；使用「次要外觀 + warning tone」 |
| 底部 | 無 fixed-bottom command bar；C22 是 navigate zone，不得渲染成 command bar |
| Projection | METADATA_ONLY 時 C04 inline；C21 欄位：stage/revisionNo/submittedAt/submittedBy/isCurrentEffective 顯示；FULL-only 欄位以 `—` 佔位（DD-005）|
| 不顯示 diff 數量 | METADATA_ONLY 時不顯示「N 題有差異」，顯示「詳細差異內容不適用於此視角」提示（DD-005）|

### 6.3 UI-013 — Command Page Baseline

**頁面角色**：複審決策執行頁，是 P1 中唯一的 command page。

| 特性 | 規則 |
|------|------|
| Primary focus region | 複審評分主作業區（score sheet segments + commentary） |
| Secondary region | C18 InitialSummaryReferenceRegion（參考用，不可編輯）；C19 FinalDecisionSummaryRegion（決策預覽）|
| Action zone role | C16 ReviewCommandBar — submit / save / revise 命令，視覺重量最高 |
| 頁面角色識別 | 進入頁面時立即知道：「這是填寫與提交複審的頁面」；C16 固定底部始終可見 |
| C16 位置策略 | viewport-fixed bottom bar（DD-001 已批准）；主作業區下方需預留等高補白，避免最後一題被遮蓋 |
| C16 variant 差異 | 5 種 mode 的 action set 明確不同（見 §8 high-risk detail） |
| V4 vs V5 視覺區分 | READONLY_BLOCKED（V4）與 CLOSED exception（V5）的 C02 / C03 / C16 三點必須視覺可區分 |
| CLOSED exception mode | C03 顯示「例外修正（已結案）」；C02 amber/warning tone；C16 revise ENABLED，submit new DISABLED |
| 不得混淆 | 「提交複審修正（例外）」（submit in C16）≠「前往複審修正（例外）」（navigate in C22 of UI-014）；兩者語意不同，視覺層次不同 |

---

## 7. Semantic Tone Baseline

本節固定四種語意 tone 的視覺意圖、相對嚴重程度、對比期望，以及必須區分的混淆對象。  
**不宣稱具體色碼或 design token 已定稿**（Portal51 token detail 尚無 repo authority）。

### 7.1 `info`

| 屬性 | 描述 |
|------|------|
| 語意 | 一般過程提示，非警告；告知使用者目前狀態（如「草稿尚未提交」、「修正模式中」）|
| 場景 | EDIT_DRAFT 的草稿提示；REVISION 的修正說明 |
| 對比期望 | 低度對比；不搶奪使用者注意力，作為背景語境通知 |
| 相對嚴重程度 | 最低 |
| 不得與...混淆 | `blocked`（info 不阻止操作）；`exception`（info 不代表非常規路徑） |

### 7.2 `blocked`

| 屬性 | 描述 |
|------|------|
| 語意 | 使用者 **不可** 做任何事；操作已鎖定；應離開此頁或等待 |
| 場景 | READONLY_BLOCKED：角色不符、案件未到此 stage、案件已取消 |
| 對比期望 | 中至高對比；清晰傳達「此處無法操作」 |
| 相對嚴重程度 | 中（非危險，但阻斷） |
| 不得與...混淆 | `exception`（blocked 不代表有例外出口）；`info`（blocked 非中性通知） |

### 7.3 `exception`

| 屬性 | 描述 |
|------|------|
| 語意 | 非常規路徑，使用者 **可以** 繼續（僅複審修正），但需意識到這是例外行為 |
| 場景 | CLOSED_REVISION_EXCEPTION（UI-013 V5）；CLOSED_EXCEPTION_ENTRY（UI-014 V6）；CLOSED_REREVIEWER_HINT（UI-010）|
| 視覺語感 | amber / warning 語意（「注意，非常規，但可以繼續」）（DD-006 已批准） |
| 對比期望 | 中至高對比；需讓使用者意識到「這不是預設路徑」，但不阻止操作 |
| 相對嚴重程度 | 中（可繼續，但需謹慎） |
| 不得與...混淆 | `blocked`（exception 有合法操作出口）；`info`（exception 帶有警示義務）|
| 語意家族一致性 | C02 exception banner 的 warning tone、C22 exception entry 的 warning-outlined widget、C03 CLOSED_REVISION_EXCEPTION 的 mode strip 三者應傳達同一語意家族 |

### 7.4 `metadata-only`（Projection Indicator）

| 屬性 | 描述 |
|------|------|
| 語意 | 使用者目前以 system-admin 視角閱覽，只能看到 metadata 欄位，完整評分內容不可見 |
| 場景 | SYSTEM_ADMIN 進入 UI-010 / UI-014 |
| 視覺形式 | 嵌入 C01 的 inline indicator（subtitle / badge）；不是 C02 等級的 banner |
| 對比期望 | 低至中對比；作為視角說明，不應與主要警示（C02）競爭 |
| 相對嚴重程度 | 最低（視角限制，非操作阻斷） |
| 不得與...混淆 | `blocked`（metadata-only 不阻止瀏覽，只限縮資料）；`exception`（metadata-only 是正常 projection，不是非常規路徑）|
| 複合狀態 | METADATA_ONLY + terminal/exception 同時出現時，C04 indicator 仍在 C01 內，C02 banner 仍是主警示；兩者不合併 |

---

## 8. High-Risk Target Baseline

### 8.1 C16 — ReviewCommandBar（UI-013）

**為何高風險**：
- 5 種 mode variants，action set 差異極大
- `READONLY_BLOCKED`（V4）與 `CLOSED exception`（V5）的 C16 表現看似相似（都有 disabled），但語意截然不同
- 任何實作偏差都可能封鎖合法操作（V5 應該能 revise）或開放非法操作（V4 不應能提交）

**必須避免的視覺錯誤**：
- 不得把 C16 畫成流式 footer（須是 viewport-fixed bottom bar）
- 不得讓 C16 在 scroll 後消失（主作業區需補白）
- V4 與 V5 的 C16 不得視覺相同
- V5 不得只有 `back` 按鈕（revise 是合法開放的）

**必須保留的視覺訊號**：

| Variant | C16 應呈現的狀態 |
|---------|----------------|
| V1 NEW_REREVIEW | save draft ✓ + submit rereview ✓；無 revise |
| V2 EDIT_DRAFT | save draft ✓ + submit rereview ✓；無 revise |
| V3 REVISION | save draft ✓ + submit revision（不同 label: 「提交複審修正」）；無 new submit |
| V4 READONLY_BLOCKED | 所有命令 hidden/disabled；only back；blocked tone 明確 |
| V5 CLOSED exception | submit new = DISABLED（reason 說明）；**revise = ENABLED（「提交複審修正（例外）」）**；save draft ✓；back ✓ |

**Submit button label 必須依 mode 不同**：
- V1/V2：「提交複審」
- V3：「提交複審修正」
- V5：「提交複審修正（例外）」

### 8.2 C21 — RevisionCompareSummaryRegion（UI-014）

**為何高風險**：
- FULL vs METADATA_ONLY 的欄位顯示差異很大
- 若實作不嚴格，可能洩漏 FULL-only 欄位給 SYSTEM_ADMIN（違反 D-010 minimal privilege）
- 「部分 hidden / 部分 dash / 部分洩漏」三種不一致的實作方式後果不同

**必須避免的視覺錯誤**：
- 不得在 METADATA_ONLY 時顯示任何分數值
- 不得在 METADATA_ONLY 時顯示差異題數（如「共 N 題有差異」）
- 不得完全隱藏 FULL-only 欄位的 label（應保留 label，值以 `—` 佔位）

**必須保留的視覺訊號**：

| 欄位類型 | FULL 顯示 | METADATA_ONLY 顯示 |
|---------|----------|-------------------|
| stage / revisionNo / submittedAt / submittedBy / isCurrentEffective | ✅ 顯示 | ✅ 顯示 |
| totalScore / 各題得分 | ✅ 顯示 | ❌ label 保留，值 = `—` |
| comment / attachment / diff 摘要 | ✅ 顯示 | ❌ label 保留，值 = `—` |
| 比較 diff（答案差異） | ✅ 顯示 | ❌ 顯示「詳細差異內容不適用於此視角」提示文字 |

### 8.3 C22 — RevisionNavigationActionZone（UI-014）

**為何高風險**：
- 此 zone 在 UI-014 是 navigate-only；若被畫成 command bar，使用者可能誤解為在此頁提交
- CLOSED exception entry（「前往複審修正（例外）」）的視覺重量若誤升格為 primary filled button，會與 UI-013 C16 的 submit button 語意混淆

**必須避免的視覺錯誤**：
- 不得使用 viewport-fixed bottom bar（那是 C16 的領域）
- 不得使用 primary filled button 渲染例外 navigate entry（應使用 warning-tone outlined 次要外觀）
- 不得在 C22 出現「提交」語意的 label

**必須保留的視覺訊號**：
- Exception entry 採 **warning-tone outlined 次要導頁 action**（DD-004 已批准）
- 顯示文案：**「前往複審修正（例外）」**（navigate 語意，非 submit 語意）
- Exception entry 必須比一般 navigate link 更醒目（warning tone），但比 C16 primary command 弱
- 條件控制：只在 CLOSED + 合法 rereviewer 時顯示；其他情況不渲染此 entry

---

## 9. Placeholder / Deferred Baseline

### 9.1 已明確識別的 Deferred 區塊

| 區塊 | 所在頁面 | 原因 | 展示方式 |
|------|---------|------|---------|
| C23 AssignmentDueDateHelperRegion | UI-011 | 功能範圍尚未穩定，不在 P1 | placeholder 框 + 標示「TBD」 |
| C24 RevisionLifecycleEntryArea | UI-010 | lifecycle API 尚未穩定 | placeholder 框 + 標示「TBD — revision lifecycle entry」 |
| C21 FULL_COMPARE detail link | UI-014 | 深層比較細節範圍未定 | 顯示 `[view detail — placeholder]` 提示 |
| Metadata-only compare density | UI-014 C21 | Portal51 masked field density 未確認 | 套用 `label + —` 規則，density 細節留 deferred |

### 9.2 Placeholder 呈現原則

1. **存在但未定稿的區塊**：以明確邊框或灰底區域標示，內部只有功能名稱 + `（TBD）` 或 `（placeholder）` 標記。
2. **永遠不得把 deferred 畫成 finalized content**：不得在 placeholder 區加入實際標籤或資料欄位，避免讓 reviewer 誤以為設計已定稿。
3. **Placeholder 不等於佔位空白**：應有明確邊界，讓讀者知道「這裡有設計工作待進行」，而不是空曠的未使用空間。
4. **Deferred 區塊不影響已定稿區塊的 review**：C24 是 deferred，不影響 UI-010 的 C08/C09/C10/C11 的設計審查。

---

## 10. Practical Review Checklist

供後續 wireframe refinement、prototype validation、Angular page skeleton 快速自查。

### 10.1 Shared Region Check（每頁適用）

- [ ] C01 CaseSummaryHeader 始終顯示，且在 metadata-only 時包含 inline projection indicator
- [ ] 渲染順序：C01 → (C04 inline) → C02 → C03（不得換位）
- [ ] C02 存在且 tone 正確（info / blocked / exception 三類明確不同）
- [ ] C03 存在且 mode label 正確（UI-013 CLOSED exception 使用「例外修正（已結案）」）
- [ ] metadata-only 時沒有獨立 C04 banner row（已降格為 C01 inline）

### 10.2 UI-010 Control Center Check

- [ ] 頁面上所有 CTA 均為「前往...」語意，無任何 submit / assign command
- [ ] C08 外觀不像 C16（沒有 fixed-bottom command bar）
- [ ] ROLE_DENIED action 不顯示（hidden）
- [ ] stage/status 暫時不符的 action visible + disabled + reason 文字
- [ ] CLOSED rereviewer 場景：disabled 原 action + 可見提示
- [ ] C24 是 placeholder，不假裝成實際 action 元素

### 10.3 UI-014 History Page Check

- [ ] 頁面無直接提交按鈕
- [ ] C22 不是 fixed-bottom bar
- [ ] C20 timeline 中 effective version 有明確視覺強調（≠ superseded）
- [ ] C21 METADATA_ONLY 時：FULL-only 欄位 label 保留，值為 `—`
- [ ] C21 METADATA_ONLY 時：無分數值、無差異題數統計
- [ ] exception entry（C22）使用 warning-tone outlined navigate，不用 primary filled button
- [ ] exception entry 文案：「前往複審修正（例外）」

### 10.4 UI-013 Command Page Check

- [ ] C16 採用 viewport-fixed bottom bar
- [ ] 主作業區有預留 C16 等高的底部補白
- [ ] V4 READONLY_BLOCKED 的 C02 tone ≠ V5 CLOSED exception 的 C02 tone
- [ ] V4 C16：所有命令 disable/hidden，只有 back
- [ ] V5 C16：revise ENABLED（「提交複審修正（例外）」），submit new DISABLED
- [ ] C03 在 V5 顯示「例外修正（已結案）」，不顯示一般「修正模式」

### 10.5 Semantic Tone Cross-Check

- [ ] `blocked` tone 與 `exception` tone 在同頁不可視覺混同
- [ ] `exception` tone 與 `info` tone 在同頁不可視覺混同
- [ ] metadata-only indicator 不與 C02 banner 視覺競爭（C04 在 C01 內）
- [ ] UI-014 C22 的 navigate entry 視覺重量明確低於 UI-013 C16 的 submit button

---

## 11. Deferred Topics

以下議題本輪刻意不處理，應留待 P2 或更後段再接手。

| 議題 | 原因 | 預計落點 |
|------|------|---------|
| Portal51 具體 design token（色碼、border-radius） | 無 repo authority；實作時依 portal51-lib 實際能力確認 | implementation planning |
| C16 CLOSED exception button 的精確 label 文案統一  | 在 wireframe README 中列為 open question；未進入 decision | 下一輪 DD-009 候選 |
| UI-013 C16 的 breakpoint 與精確高度規格 | 視 portal51-lib wrapper 而定；非 P1 設計層必要資訊 | visual baseline v2 或 implementation |
| Effective version badge 的 token 語意 | 色碼選擇與 portal51 token 掛鉤，無法獨立定稿 | implementation planning |
| UI-014 stage tab 預設切換策略（interaction vs routing） | wireframe README 列為 open question，需前端與 UX 確認 | frontend 治理層或 P2 wireframe |
| C10/C11 cross-role summary 可見性（UI-010） | DD 中已識別但未處理 | P2 設計層或 authority 補充 |
| UI-012 / UI-011 / UI-009 的 visual baseline | P2 頁面；本輪不擴張 | P2 visual baseline extension |
| Animation / transition / hover interaction | 不在設計層 P1 範圍 | UX spec 或 implementation |

---

## 附錄：DD 決策與 Visual Baseline 對應索引

| Design Decision | 影響的 Visual Baseline 章節 |
|----------------|---------------------------|
| DD-001 C16 fixed-bottom strategy | §6.3、§8.1、§10.4 |
| DD-002 C02 → C03 ordering | §5.1、§10.1 |
| DD-003 C04 降格 inline indicator | §5.1、§5.5、§7.4、§10.1 |
| DD-004 C22 warning outlined navigate | §6.2、§8.3、§10.3 |
| DD-005 C21 METADATA_ONLY masking | §6.2、§8.2、§10.3 |
| DD-006 exception vs blocked tone | §4.5、§5.3、§7.2、§7.3、§10.5 |
| DD-007 C08 ROLE_DENIED hidden strategy | §6.1、§10.2 |
| DD-008 mode label 例外修正（已結案） | §5.4、§6.3、§10.4 |
