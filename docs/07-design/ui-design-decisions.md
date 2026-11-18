# UI Design Decisions

> D-032／D-033 規格變更：下文涉及 CLOSED_REVISION_EXCEPTION、CLOSED_EXCEPTION_ENTRY、CLOSED_REREVIEWER_HINT、O1-C 修正入口及其視覺決策，僅保留歷史設計依據，已被取代，不作現行可操作規則。CLOSED 不得 revision／reopen；複審永久盲審，含可反推分數。現有 wireframe／prototype 未於本輪修改，不作新流程完成 evidence。其他不衝突視覺決策保留。

## 文件角色

本文件收斂 P1 wireframe + prototype 視覺驗核後，尚未在上游 authority 中明確落地的
**UI 設計決策**。每一條 decision entry 記錄：
- 決策背景與為何需要在此層處理
- 可選方案與各自的 trade-off
- 推薦方向
- 是否需要人類拍板 (human_decision_needed)
- 決策確認後應回寫的 authority 清單

本文件僅處理 07-design 層的 UI/UX 設計細節，不覆蓋 domain rule、permission policy、API contract。
若發現議題需改動 01–06 authority，應先更新對應 authority 再同步此文件。

---

## 1. Source Inputs

| 來源 | 路徑 | 用途 |
|------|------|------|
| P1 wireframes | `docs/07-design/wireframes/ui010-case-detail-first-pass.md` | 結構骨架、open questions |
| P1 wireframes | `docs/07-design/wireframes/ui013-rereview-first-pass.md` | C16/exception 細節 |
| P1 wireframes | `docs/07-design/wireframes/ui014-revision-history-first-pass.md` | C21/C22/C04 細節 |
| Prototype runtime | `frontends/quality-inspection-frontend/prototype-react/src/prototype/components.tsx` | 元件結構與堆疊順序 |
| Prototype runtime | `frontends/quality-inspection-frontend/prototype-react/src/prototype/data.ts` | 各 variant 的 tone/action 組合 |
| Prototype runtime | `frontends/quality-inspection-frontend/prototype-react/src/prototype/screens/` | 三頁面實作細節 |
| Authority bridge | `docs/06-frontend/frontend-review-gate-pending-items.md` | O1-C / O2-A / O3-C adopted decisions |
| Authority bridge | `docs/06-frontend/ui-component-map.md` | C01–C24 元件責任定義 |
| Authority bridge | `docs/06-frontend/ui-state-actions-matrix.md` | action policy matrix |
| Decision log | `docs/01-requirements/decision-log.md` | D-010, D-021, D-022 |
| Wireframe brief | `docs/07-design/ui-wireframe-brief.md` | scope freeze + variant matrix |

---

## 2. Scope 與 Constraints

- **本文件覆蓋**：P1 頁面（UI-010、UI-013、UI-014）的設計層待決議
- **本文件不改變**：wireframe 視覺基線、domain invariant、API contract、permission policy
- **本文件不包含**：P2 頁面（UI-009、UI-011、UI-012、UI-006、UI-007）的 design decision（可另行補充）
- **優先序**：decision-log > domain-model/state-machine > permission-matrix > api-spec > **本文件**
- **Human decision 旗標**：標示 `⚠ 需人類拍板` 的 entry，需人類確認後才能進入實作

---

## 3. 既採決策引用（from frontend-review-gate-pending-items.md）

下列決策已在 `frontend-review-gate-pending-items.md` 中標記為 `ADOPTED`，本文件直接引用：

| 決策ID | 內容摘要 |
|--------|---------|
| **O1-C** | CLOSED + 合法 rereviewer → UI-014 顯示條件式「前往複審修正」navigate entry → 導至 UI-013 CLOSED_REVISION_EXCEPTION 模式；新提交禁止，revision 開放 |
| **O2-A** | 所有 review workflow command API 採 case-rooted sub-resource，stage 在前：`/review-cases/{caseId}/{stage}/...`、`/review-cases/{caseId}/{stage}/submissions/{submissionId}/{action}`（來源：decision-log D-022，路徑樣式由 D-058 修正） |
| **O3-C** | SYSTEM_ADMIN metadata 視角採 shared base metadata whitelist + per-page extension 白名單；UI-009、UI-010、UI-014 各自維護 extension 欄位清單（來源：decision-log D-010） |

---

## 4. Decision Entries

---

### DD-001 — C16 ReviewCommandBar Sticky / Fixed 策略

| 屬性 | 值 |
|------|---|
| decision_id | DD-001 |
| 相關頁面 | UI-013（同等適用 UI-012） |
| 相關元件 | C16 ReviewCommandBar |
| human_decision_needed | ✅ **否（推薦方向已批准）** |
| 狀態 | ✅ 已批准並進行 backwrite |

#### 背景

C16 ReviewCommandBar 位於 UI-013 的評分表單下方，負責承載 save draft / submit rereview / revise rereview / submit rereview（例外）等命令群組。

評分表單（score sheet）可能包含大量 sections 與 items，在一般視窗高度下，整個表單無法一次全部顯示；使用者需要向下 scroll 填寫答案。

Wireframe 明確指出：「C16 必須維持高可見優先序，不得沉到頁底被 scroll 遮蓋」，並標注「sticky / fixed 策略延後至 ui-design-decisions.md」。

Prototype 目前的 CommandBar 放置於 `PageFrame > children` 最後子節點，屬於文件流中的固定位置，未使用 `position: sticky` 或 `position: fixed`；在較長的評分表單中會隨頁面捲動到視窗之外。

#### 為何需要在設計層決策

此決策影響：
1. 評分中途使用者的操作可達性（送出/儲存草稿是高頻操作）
2. CSS 實作策略（sticky 需要父層 overflow 設定；fixed 需要 z-index + 底部 padding 補位）
3. 例外模式（CLOSED_REVISION_EXCEPTION）的「提交複審修正（例外）」按鈕可見性

#### 選項表

| 選項 | 描述 | 優點 | 缺點 |
|------|------|------|------|
| **A：底部 sticky** | `position: sticky; bottom: 0` | 在 overflow container 內跟隨視窗底部；父層若無 overflow hidden 直接有效 | Angular 的 scroll 策略可能使父 container 不配合；需測試各 breakpoint |
| **B：viewport fixed** | `position: fixed; bottom: 0; left: 0; right: 0` | 絕對保持在視窗底部，不受表單長度影響；最高可達性 | 覆蓋頁面下方內容，需 bottom padding 補空間；在 mobile 上吃掉較多面積 |
| **C：雙態策略（scroll 前/後）** | 初始流式，scroll 超過閾值後切換為 sticky/fixed | 初始狀態整潔，長表單時保持可達性 | 實作複雜（需 IntersectionObserver）；動與靜之間的 layout shift 需處理 |
| **D：表單頂部 + 底部雙呈現** | 在表單頂端與表單底端各放一份 command bar | 使用者在上下兩端皆可操作 | 語意重複（有兩個 submit 按鈕）；複雜 disabled 狀態要同步；不推薦 |

#### 已批准結論

**採選項 B（viewport fixed）作為 P1 backwrite 基線**，理由：
- Portal51 / MFE shell 場景下，parent container 的 overflow 屬性難以保證，sticky 可能失效
- CLOSED 例外模式下「提交複審修正（例外）」按鈕屬高風險操作，可見性最優先
- 需於 page layout 補充 `padding-bottom` 避免表單最後一個 item 被 CommandBar 遮蓋

本輪僅回寫 UI-013 的 P1 設計落點；不把此策略外推為跨頁 scroll 治理原則，也不宣稱已確認 shell scroll truth。

#### 設計層補充落點

1. `C16` 在 UI-013 以 **viewport-fixed bottom bar** 表達，不採 first-pass 流式 footer。
2. 主作業區需預留與 command bar 等高的底部補白，避免最後一題被遮蓋。
3. breakpoints / 精確高度 / token 細節留待 visual baseline 或 implementation planning，不阻擋本輪 backwrite。

#### impact_area

`features/cases/pages/rereview-page/`、page layout template、global scroll strategy

#### 決策確認後回寫目標

| 優先 | 目標 |
|------|------|
| 必須 | `docs/07-design/wireframes/ui013-rereview-first-pass.md` — 在 C16 section 補充決議 note |
| 必須 | `docs/06-frontend/ui-component-map.md` — C16 `notes for future implementation` 補充 sticky/fixed 規則 |
| 建議 | `docs/06-frontend/frontend-design-method.md` — 若此 scroll 策略成為共用原則 |

#### 若未決策的風險

實作時各開發者自行決定，造成 UI-012 與 UI-013 之間 CommandBar 行為不一致；在長表單場景下 submit 按鈕被 scroll 遮蓋，降低送出成功率。

---

### DD-002 — C02 StatusOrBlockedBanner 與 C03 ModeIndicatorStrip 的視覺優先序

| 屬性 | 值 |
|------|---|
| decision_id | DD-002 |
| 相關頁面 | UI-010、UI-013、UI-014（所有使用 C02 + C03 的頁面） |
| 相關元件 | C02 StatusOrBlockedBanner、C03 ModeIndicatorStrip |
| human_decision_needed | ✅ **否（prototype 已確認）** |
| 狀態 | ✅ 已收斂 |

#### 背景

C02 StatusOrBlockedBanner 與 C03 ModeIndicatorStrip 同時出現於多個 P1 頁面的頂部區域。
兩個元件都位置顯要，若同時出現時的堆疊順序未定，會造成視覺優先序錯亂。

#### 收斂結論

**渲染順序（從上到下）：C02（banner） → C03（mode-strip）**

Evidence（多重交叉確認）：
1. **Prototype `components.tsx` PageFrame JSX**：
   ```jsx
   {props.projectionNotice && <div class="projection-notice">...</div>}  // C04
   {props.banner && <div class="banner ...">...</div>}                    // C02
   <div class="mode-strip-panel">...</div>                                // C03（always）
   ```
   明確渲染順序：C04 → C02 → C03
2. **UI-013 wireframe** §3 各 variant 的 `page-structure` 皆列 C02 先於 C03
3. **UI-014 wireframe** §2 結構描述：「C01 → C04 → C02 → C03」
4. **UI-010 wireframe** §2 結構描述：「C01 → C04 → C02 → C03」

#### 語意理由

C02 攜帶的是**狀態警告或阻斷資訊**（「此案件已結案」、「您無權操作此案件」）；
C03 攜帶的是**當前模式識別**（「REVISION 模式」、「CLOSED 例外修正模式」）。
警告資訊比模式標籤優先序更高，使用者應先看到 C02 確認可操作性，再看 C03 確認目前模式。

#### 需拍板的事項

無需額外人類決策。

#### 決策確認後回寫目標

| 優先 | 目標 |
|------|------|
| 建議 | `docs/06-frontend/ui-component-map.md` — C02/C03 的 `notes for future wireframe` 補充「C02 在 C03 之上」 |

---

### DD-003 — C04 ProjectionNoticeBanner 與 C02 在複合狀態下的堆疊與合併策略

| 屬性 | 值 |
|------|---|
| decision_id | DD-003 |
| 相關頁面 | UI-010（V3 METADATA + CLOSED 組合）、UI-014（METADATA_ONLY + CLOSED_EXCEPTION_ENTRY） |
| 相關元件 | C04 ProjectionNoticeBanner、C02 StatusOrBlockedBanner |
| human_decision_needed | ✅ **否（推薦方向已批准）** |
| 狀態 | ✅ 已批准並進行 backwrite |

#### 背景

C04 ProjectionNoticeBanner 是 METADATA_ONLY 視角的降級通知（「您目前以管理員身份查看，僅顯示 metadata 欄位」）。
C02 StatusOrBlockedBanner 是案件狀態或操作封鎖通知（「此案件已結案」）。

在以下場景中兩者同時出現：
- **UI-010 V3+V4（CLOSED + SYSTEM_ADMIN）**：C04（降級視角）+ C02（terminal 狀態）
- **UI-014 METADATA_ONLY + CLOSED_EXCEPTION_ENTRY**：C04（降級視角）+ C02（exception）

Wireframe (ui014) 明確標注：「C04 先（告知降級視角），C02 次（告知狀態問題）；C04 與 C02 共存時不可互相遮蓋」。
Wireframe (ui010) 則提出 open question：「C04 與 C02 同出現時，是否可合併成複合 banner？」

#### 待決議部分：分離顯示 vs 複合 Banner

| 選項 | 描述 | 優點 | 缺點 |
|------|------|------|------|
| **A：分離顯示（prototype 現況）** | C04 獨立 banner、C02 獨立 banner，各自渲染 | 元件獨立、語意清晰；實作簡單 | 兩條 banner 疊加視覺負荷略高 |
| **B：合併為複合 banner** | 設計一個 CompositeNoticeBanner，可接受 `[projectionNotice, statusBanner]` 合併渲染 | 視覺更整潔，只佔一條横幅高度 | 需新 component；合併文案設計複雜；C04 語意（metadata）與 C02 語意（blocked/exception）混在一起可能語意模糊 |
| **C：C04 降格為 inline tag（不佔 banner 列）** | C04 改為 header subtitle 或 badge，不以獨立 banner 呈現；C02 保持 banner | C04 弱化後視覺更乾淨 | 元件責任重新分配；可能使降級警告資訊被忽略 |

#### 已批准結論

**採選項 C：C04 降格為 header subtitle / badge / inline projection indicator；C02 保持主 banner。**

設計層落點：
1. `metadata-only` 仍必須可被辨識，但不再與 terminal / exception / blocked 競爭同一 banner 優先序。
2. `C04` 只承擔 projection 降級說明，不承擔 blocked / exception 語意。
3. `C02` 繼續作為 primary banner，承擔 terminal / blocked / exception 的主要提示。
4. 在 UI-010 / UI-014 的複合狀態下，`C04` 需錨定於 `C01` header 區作為 subtitle、badge 或 inline indicator，而非獨立 banner row。

#### impact_area

`features/cases/pages/case-detail-page/`、`features/cases/pages/revision-history-page/`

#### 決策確認後回寫目標

| 優先 | 目標 |
|------|------|
| 必須 | `docs/07-design/wireframes/ui010-case-detail-first-pass.md` — 解答 open question（C04 + C02 合併 vs 分離） |
| 必須 | `docs/07-design/wireframes/ui014-revision-history-first-pass.md` — 將 C04 回寫為 header-level projection indicator |
| 建議 | `docs/06-frontend/ui-component-map.md` — C04 notes 補充「在複合狀態下降格為 inline projection indicator」 |

#### 若未決策的風險

UI-010 / UI-014 的 METADATA + terminal 複合狀態在實作時各自決定，造成兩頁顯示策略不一致。

---

### DD-004 — C22 RevisionNavigationActionZone：例外入口 Widget 類型

| 屬性 | 值 |
|------|---|
| decision_id | DD-004 |
| 相關頁面 | UI-014 |
| 相關元件 | C22 RevisionNavigationActionZone |
| human_decision_needed | ✅ **否（推薦方向已批准）** |
| 狀態 | ✅ 已批准並進行 backwrite |

#### 背景

O1-C 決策確認：CLOSED 案件 + 合法 rereviewer → UI-014 顯示「前往複審修正（例外）」入口，導向 UI-013 的 CLOSED_REVISION_EXCEPTION 模式。

UI-014 wireframe 明確約束：
- **「C22 是 navigate-only，不是 submit bar」**
- **「前往複審修正」必須是「navigate link，導向 UI-013 CLOSED_REVISION_EXCEPTION 模式」**
- **「不是 submit button」**

Prototype 實作：`CLOSED_EXCEPTION_ENTRY` variant 中，此入口位於 `navigateActions` 陣列，使用 `ActionLinks` 元件，帶有 `tone: 'warning'`；ActionLinks 渲染為 `<Link>` (React Router) 元素。

待決議的是：此 navigate entry 的具體 widget 類型（link-style / outlined button / warning-toned filled button）。

#### 決策維度

此決策的核心是：

1. **區隔性**（與 C16 ReviewCommandBar 的主 CTA 區分）— C16 的「提交複審修正（例外）」是 submit；C22 的「前往複審修正（例外）」是 navigate。兩者必須視覺上不可混淆。
2. **嚴肅性**（例外操作帶有 warning 語意）— CLOSED 是 terminal state，例外修正是高風險操作，需讓使用者意識到這是非常規路徑。
3. **技術可行性**（navigate = React Router Link，不是 button with onClick → form submit）

#### 選項表

| 選項 | Widget 類型 | 視覺語意 | 與 C16 區隔性 |
|------|------------|----------|--------------|
| **A：純文字 Link（anchor 樣式）** | `<Link>` 渲染為文字超連結，加底線或 warning 顏色 | 最輕量；明確「只是導頁」語意 | 高（與 button 完全不同外觀） |
| **B：Outlined 次要按鈕（warning tone）** | `<Link>` 渲染為 outlined button，border 使用 warning 色 | 中等重量；看起來是可操作入口 | 中（外形像 button，但樣式比 C16 弱） |
| **C：Warning 填充按鈕** | `<Link>` 渲染為 filled button，背景使用 warning/amber 色 | 最高強調；視覺衝擊強 | 低（與 C16 的 primary button 視覺重量相近，容易混淆） |
| **D：Icon + text 的 navigate card item** | Portal51 list item 式的 card，帶右箭頭，warning 色 icon | 清晰「navigate entry」語意；與 submit bar 完全不同 | 高 |

#### 已批准結論

**採選項 B（warning tone 的 outlined 次要導頁 action）**，理由：
- Prototype ActionLinks 已用 `tone: 'warning'`，與 outlined button 命名一致
- 強調此入口屬例外操作（視覺上比一般 link 更醒目）
- 與 C16 CommandBar 中 `提交複審修正（例外）` 的 primary button 視覺層次區隔明確

若 runtime / UI library 沒有完全對應的現成 widget，仍以 **navigate-only 語意 + 次要外觀層級** 為優先，不升級為 filled primary CTA。

#### 設計層補充落點

1. 顯示文案以 **「前往複審修正（例外）」** 回寫，避免與 submit/revise command 混淆。
2. 控件語意維持 link / navigate action，不得轉成 submit button。
3. 入口是否出現仍由 action policy 決定；本決策只固定 **可見時的 widget 類型與視覺重量**。

#### impact_area

`features/cases/pages/revision-history-page/`、`features/cases/components/revision-navigation-action-zone/`

#### 決策確認後回寫目標

| 優先 | 目標 |
|------|------|
| 必須 | `docs/07-design/wireframes/ui014-revision-history-first-pass.md` — C22 例外入口 widget 設計 note |
| 必須 | `docs/06-frontend/ui-component-map.md` — C22 `notes for future implementation` 補充 widget type |
| 建議 | `docs/06-frontend/frontend-test-scenarios.md` — 新增 CLOSED + rereviewer UI-014 → UI-013 navigate 測試情境 |

#### 若未決策的風險

實作時可能將 C22 的 navigate entry 實作成外觀與 C16 CommandBar 相近的 button，導致使用者將「導向修正頁」誤認為「直接提交修正」；或反之將其弱化為不顯眼的文字連結，使例外操作入口被忽略。

---

### DD-005 — C21 RevisionCompareSummaryRegion：METADATA_ONLY 遮罩粒度

| 屬性 | 值 |
|------|---|
| decision_id | DD-005 |
| 相關頁面 | UI-014 |
| 相關元件 | C21 RevisionCompareSummaryRegion |
| human_decision_needed | ✅ **否（推薦方向已批准）** |
| 狀態 | ✅ 已批准並進行 backwrite |

#### 背景

Decision D-010 確立：SYSTEM_ADMIN 僅能查看 metadata，不可查看完整評分內容、評語全文與附件。
Decision O3-C 確立：以 shared base + per-page extension whitelist 控制 metadata 視角欄位。

UI-014 Revision 歷程頁在 METADATA_ONLY 模式下，C21 RevisionCompareSummaryRegion 需要決定顯示哪些欄位、遮罩哪些欄位，以及「遮罩行為」是「完全不顯示」還是「顯示遮罩佔位符」。

#### 確認的欄位邊界（依 O3-C / D-028 回寫）

| 欄位 | FULL 顯示 | METADATA_ONLY 顯示 | 來源 |
|------|----------|-------------------|------|
| stage（INITIAL/REREVIEW） | ✅ | ✅ | prototype METADATA_ONLY_COMPARE |
| revisionNo | ✅ | ✅ | prototype METADATA_ONLY_COMPARE |
| submittedAt | ✅ | ✅ | prototype METADATA_ONLY_COMPARE |
| submittedBy（送出者 ID） | ✅ | ✅ | O3-C UI-014 whitelist |
| currentEffective（是否現行版） | ✅ | ✅ | O3-C UI-014 whitelist |
| submissionId | ✅ | ❌（超出 O3-C UI-014 whitelist） | decision-log D-028 / api-spec |
| totalScore | ✅ | ❌（遮罩） | prototype METADATA_ONLY_COMPARE 無 score diff |
| 答案摘要/各題得分 | ✅ | ❌（遮罩） | ui014 wireframe §3 METADATA_ONLY |
| comment / 評語 | ✅ | ❌（遮罩） | ui014 wireframe §3 METADATA_ONLY |
| attachment 列表 | ✅ | ❌（遮罩） | ui014 wireframe §3 METADATA_ONLY |
| 比較 diff（答案差異） | ✅（FULL_COMPARE） | ❌（遮罩，顯示提示文字） | prototype METADATA_ONLY_COMPARE |

Prototype `METADATA_ONLY_COMPARE` compareSummary 的 reviewerNotes：
「METADATA_ONLY_COMPARE 展示 metadata 欄位；差異資訊顯示「詳細差異需完整視角」提示。」

#### 待決議部分

| 問題 | 選項 |
|------|------|
| **遮罩呈現方式** | A：完全不渲染遮罩欄位（hidden）<br/>B：渲染欄位 label，值顯示「—」或「受限」<br/>C：渲染欄位 label，值顯示 `[Masked]` placeholder |
| **score 差異摘要是否顯示「N 題有差異」（不顯示分數）** | A：完全不顯示比較結果<br/>B：顯示「共 N 項有差異（詳細分數受限）」 |
| **per-page extension whitelist 細節** | O3-C 已採，但 UI-014 具體 extension 欄位清單尚未明文化 |

#### 已批准結論

1. **遮罩呈現方式**：採 **label 保留 + 值以 `—` 佔位**，讓 system admin 看得到欄位類型但不取得 FULL-only 內容。
2. **score / diff 摘要**：不顯示任何分數值或「N 項差異」數量，只顯示 **「詳細差異內容不適用於此視角」** 提示。
3. **UI-014 extension whitelist**：回寫時以 O3-C 為準，固定為 `stage`、`revisionNo`、`submittedAt`、`submittedBy`、`isCurrentEffective`。

#### 設計層補充落點

1. panel 內的 FULL-only 類別（score/comment/attachment/diff）保留 label，但值與摘要一律以 `—` 或提示文字表達。
2. 若頁面需要標示目前有效版本，使用 `isCurrentEffective` 的 badge / label 語意，不回退為 submission status 欄位。
3. 欄位名稱以 `submittedBy` 對齊 page extension whitelist，不再新增 `actorId` / `submissionId` 等超出白名單欄位。

#### impact_area

`features/cases/pages/revision-history-page/`、`services/quality-inspection-service/` reporting layer (METADATA_ONLY projection)

#### 決策確認後回寫目標

| 優先 | 目標 |
|------|------|
| 必須 | `docs/06-frontend/ui-view-model-spec.md` — UI-014 METADATA_ONLY extension whitelist 具體欄位清單 |
| 必須 | `docs/07-design/wireframes/ui014-revision-history-first-pass.md` — METADATA_ONLY_COMPARE variant 遮罩佔位符確認 |
| 建議 | `docs/06-frontend/ui-component-map.md` — C21 `notes for future implementation` 補充遮罩粒度規則 |
| 建議 | `docs/06-frontend/frontend-test-scenarios.md` — SYSTEM_ADMIN UI-014 metadata-only 遮罩驗收情境 |

#### 若未決策的風險

前端實作 METADATA_ONLY 遮罩時邏輯不一致（部分欄位 hidden、部分顯示 placeholder、部分直接洩漏分數），不符合 D-010 的 minimal privilege 原則。

---

### DD-006 — C02 Exception Banner 與 Blocked Banner 的視覺語意區分

| 屬性 | 值 |
|------|---|
| decision_id | DD-006 |
| 相關頁面 | UI-013（CLOSED_REVISION_EXCEPTION vs READONLY_BLOCKED）、UI-014（CLOSED_EXCEPTION_ENTRY）、UI-010（CLOSED_REREVIEWER_HINT） |
| 相關元件 | C02 StatusOrBlockedBanner |
| human_decision_needed | ✅ **否（推薦方向已批准）** |
| 狀態 | ✅ 已批准並進行 backwrite |

#### 背景

C02 StatusOrBlockedBanner 需要同時承載**三種語意不同的 banner 類型**：

| Tone | 語意 | 場景 |
|------|------|------|
| `info` | 一般資訊提示 | EDIT_DRAFT、REVISION 進行中 |
| `blocked` | 操作已封鎖，使用者無法進行此頁工作 | READONLY_BLOCKED（角色不符、stage 不合法） |
| `exception` | 可操作，但屬例外路徑，有注意義務 | CLOSED_REVISION_EXCEPTION、CLOSED_EXCEPTION_ENTRY、CLOSED_REREVIEWER_HINT |

Prototype 中 `tone` 已分為三個 CSS class，但：
1. 具體 design token（色相、border、icon）尚未在本 repo 任何文件中明文化
2. UI-013 wireframe 明文：「V5（CLOSED exception）的 C02 文案與顏色/icon/語意必須與 V4（READONLY_BLOCKED）視覺不同」

#### 關鍵區分要件

**`blocked` vs `exception` 的核心語意區別**：
- `blocked`：「**您不能做任何事**。操作已鎖定。」→ 使用者應離開此頁
- `exception`：「**您在特殊條件下可以執行修正**。這是非常規路徑，請注意。」→ 使用者可繼續操作

視覺上：
- `blocked` 應傾向**中性阻斷**（灰色、橙色、或紅色 — 視警告等級）
- `exception` 應傾向**警示但可操作**（amber/warning 色系，帶操作入口）

Prototype CSS class 命名：`banner blocked` vs `banner exception`，尚未定義具體色碼。

#### 選項表（針對 exception tone 色系）

| 選項 | 色系 | 意涵 |
|------|------|------|
| **A：Amber/Warning（#FFA000 系列）** | 黃橙色 | 「注意！非常規路徑，可繼續但需謹慎」；與 warning button 語意一致 |
| **B：Orange（#E65100 系列）** | 橙色 | 介於 warning 與 danger 之間；可能與 blocked 色系混淆 |
| **C：Purple/Violet（跳脫常規警告色）** | 紫色 | 視覺上完全區隔，代表「特例路徑」；但偏離使用者直覺 |

#### 已批准結論

**採選項 A：exception = Amber / Warning tone；blocked = 與 exception 明確區分的 blocked tone。**

設計層落點：
1. `exception` 採 **amber / warning** 語意，表達「可繼續但需注意」。
2. `blocked` 不得與 `exception` 共用同一 tone；需維持「不可操作 / 唯讀 / 被封鎖」的阻斷語意。
3. `C22` 的條件式例外入口已採 warning-toned navigate action，本決策要求 `C02 exception banner` 與其保持同一 warning family 語意。
4. 本輪只回寫 tone / token 指引，不宣稱任何實作色碼或 CSS token 已完成。

#### impact_area

`components/status-or-blocked-banner/`、Portal51 token 使用規範、全域 CSS/style

#### 決策確認後回寫目標

| 優先 | 目標 |
|------|------|
| 必須 | `docs/07-design/wireframes/ui013-rereview-first-pass.md` — V4 vs V5 banner 視覺規格補充 |
| 建議 | `docs/06-frontend/ui-component-map.md` — C02 / C22 notes 補充 warning vs blocked 語意對齊 |
| 選填 | `.ai/governance/` 若治理層有 design token 規範，更新 exception/blocked/info token 對照表 |

#### 若未決策的風險

UI-013 的例外模式 banner 與一般封鎖 banner 看起來完全一樣，使用者無法判斷「是被完全封鎖」還是「有例外修正可能」，導致操作中斷或錯誤操作。

---

### DD-007 — C08 CaseActionPanel：role 合法但 status 不允許時的 action 呈現策略

| 屬性 | 值 |
|------|---|
| decision_id | DD-007 |
| 相關頁面 | UI-010 |
| 相關元件 | C08 CaseStatusActionPanel |
| human_decision_needed | ✅ **否（推薦方向已批准）** |
| 狀態 | ✅ 已批准並進行 backwrite |

#### 背景

UI-010 案件詳情頁是「控制中心」，C08 顯示使用者可採取的下一步行動入口（例如：「前往初審」、「前往複審」、「指派複審者」）。

問題：當使用者**角色合法**（正確的 reviewer/rereviewer/manager）但**案件狀態不符**（例如尚未到複審階段），或**是案件負責人但 stage 不符**（例如案件處於 REREVIEW_IN_PROGRESS 但使用者是初審者）時，C08 的動作應如何呈現？

UI-010 wireframe open question：「role 合法但 status 不允許時，action 是 hidden 還是 visible+disabled+reason？」

Prototype 中：
- `ROLE_LIMITED` variant：部分 action `disabled: true, reason: 'ROLE_DENIED'` （可見 + 停用 + 原因）
- `CLOSED_REREVIEWER_HINT` variant：「前往複審作業」disabled + reason 'USE_REVISION_HISTORY_ENTRY'，「前往 Revision 歷程」active（visible + enabled）

#### 呈現策略選項

| 策略 | 描述 | 優點 | 缺點 |
|------|------|------|------|
| **A：Hidden（完全不顯示）** | action 在條件不符時完全不渲染 | 介面乾淨，不顯示無用選項 | 使用者不知道功能存在；當條件達成後需教學 |
| **B：Visible + Disabled + Reason** | action 可見但 disabled，hover 顯示原因文字 | 使用者了解功能存在與當前無法操作的原因 | 視覺上有多個灰色按鈕，略顯雜亂 |
| **C：依語意分類：結構性限制 Hidden，臨時狀態 Disabled** | 角色根本無此權限（ROLE_DENIED）→ hidden；此次案件暫時不在此階段（STAGE_MISMATCH）→ disabled + reason | 兼顧介面整潔與資訊溝通 | 判斷邏輯複雜，需維護分類規則 |

#### 已批准結論

**推薦選項 C（語意分類策略）**：

| 類型 | 條件 | 呈現方式 |
|------|------|---------|
| 角色根本無此功能 | `ROLE_DENIED`（非此角色） | **Hidden**（不顯示） |
| 案件已到 terminal state | `CASE_ALREADY_CLOSED` / `CASE_ALREADY_CANCELLED` | **Visible + Disabled** + reason text |
| 階段或責任人暫時不對 | `STAGE_MISMATCH` / `OWNED_BY_OTHER` | **Visible + Disabled** + reason text |
| 例外路徑提示 | `USE_REVISION_HISTORY_ENTRY`（CLOSED rereviewer） | **Disabled action + visible alternative entry** |

此策略與 Prototype 的 `CLOSED_REREVIEWER_HINT` variant 行為一致。

#### 設計層補充落點

1. `OWNED_BY_OTHER` 視為 **角色合法但案件條件不符**，採 visible + disabled + reason，不採 hidden。
2. reason text 以頁面可讀文案回寫，例如：
   - `STAGE_MISMATCH` → `目前案件尚未進入此作業階段`
   - `OWNED_BY_OTHER` → `目前案件已指派其他負責人`
   - `USE_REVISION_HISTORY_ENTRY` → `請改從 Revision 歷程頁進入複審修正`
3. `CLOSED` rereviewer 場景保留 disabled 原 action + 可見替代入口，避免使用者誤以為功能消失。

#### impact_area

`features/cases/pages/case-detail-page/`

#### 決策確認後回寫目標

| 優先 | 目標 |
|------|------|
| 必須 | `docs/07-design/wireframes/ui010-case-detail-first-pass.md` — 解答 open question（hidden vs disabled） |
| 必須 | `docs/06-frontend/ui-component-map.md` — C08 `notes for future implementation` 補充 action 顯示策略 |
| 建議 | `docs/06-frontend/ui-state-actions-matrix.md` — UI-010 blocked reasons 欄位補充 reason text 標準清單 |

#### 若未決策的風險

開發者自行決定 hidden vs disabled，造成案件詳情頁各 action 行為不一致；使用者在特定角色/狀態下看到不同的行動建議，產生混淆。

---

### DD-008 — C03 ModeIndicatorStrip：CLOSED_REVISION_EXCEPTION 模式標籤文案

| 屬性 | 值 |
|------|---|
| decision_id | DD-008 |
| 相關頁面 | UI-013 |
| 相關元件 | C03 ModeIndicatorStrip |
| human_decision_needed | ✅ **否（UX copy 已批准）** |
| 狀態 | ✅ 已批准並進行 backwrite |

#### 背景

C03 ModeIndicatorStrip 在 UI-013 頂部顯示當前作業模式，幫助使用者識別「現在在哪個模式下工作」。

已確認的模式標籤（prototype data.ts）：
| 模式 | modeStrip.mode 值 | 目前顯示文字（prototype） |
|------|-------------------|------------------------|
| NEW_REREVIEW | `'NEW_REREVIEW'` | 「新複審」或原始值 |
| EDIT_DRAFT | `'EDIT_DRAFT'` | 「草稿編輯中」 |
| REVISION | `'REVISION'` | 「複審修正中」 |
| READONLY_BLOCKED | 無 mode strip 或 blocked 提示 | — |
| **CLOSED_REVISION_EXCEPTION** | `'CLOSED_REVISION_EXCEPTION'` | ⚠ 尚未轉換為中文 UX copy |

UI-013 wireframe 明文：「mode strip 須明確標示例外語意，不可與一般 REVISION 模式混淆。」

#### 待決：CLOSED_REVISION_EXCEPTION 標籤文案

現有 REVISION 模式標籤若為「修正模式」，CLOSED_REVISION_EXCEPTION 不能使用相同標籤。

| 選項 | 文案 | 優點 | 缺點 |
|------|------|------|------|
| **A** | 「CLOSED 例外修正模式」 | 明確包含 terminal state 資訊 | 較長，使用技術詞彙 CLOSED |
| **B** | 「例外修正（已結案）」 | 中文化較佳，括號補充上下文 | 「已結案」可能引起使用者疑慮 |
| **C** | 「特例修正作業」 | 語氣中性 | 未傳達結案資訊，使用者可能不清楚這是例外 |
| **D** | 「複審修正（例外）」 | 與 C22 按鈕文案「前往複審修正（例外）」一致 | 可能讓人以為「例外」是操作名稱而非狀態說明 |

#### 已批准結論

`CLOSED_REVISION_EXCEPTION` 的 mode label 固定採：

- **例外修正（已結案）**

設計層落點：
1. 必須與一般 `REVISION` 明確區分，不得簡化成相同 label。
2. 文案需與 `C02 exception banner` 語意一致，但不得暗示 reopen。
3. 本輪回寫 display label mapping，不處理 tooltip 或 visual baseline 細節。

#### impact_area

UI-013 mode strip 文案、`features/cases/pages/rereview-page/` view model 的 mode label mapping

#### 決策確認後回寫目標

| 優先 | 目標 |
|------|------|
| 必須 | `docs/07-design/wireframes/ui013-rereview-first-pass.md` — V5 C03 mode strip 文案確認 |
| 建議 | `docs/06-frontend/ui-view-model-spec.md` — Mode label mapping（mode enum → 中文顯示文字） |

#### 若未決策的風險

實作時 CLOSED_REVISION_EXCEPTION 的 mode strip 顯示原始 enum 字串（技術名稱），使用者無法判斷當前是否為例外路徑，混淆一般修正與例外修正的語意差別。

---

## 5. 決策相依關係

```
DD-004（C22 widget type）
  └── 依賴 DD-006（exception tone 色系）
      因為 C22 的 warning-toned button 需要與 C02 exception banner 的 warning 色系一致

DD-003（C04 + C02 stacking）
  └── 依賴 DD-006（exception tone）
      因為 CLOSED + METADATA 複合狀態中 C02 使用 exception tone

DD-008（C03 mode label）
  └── 與 DD-006（C02 exception banner copy）需語意一致
```

---

## 6. 建議決議優先序

| 順序 | Decision | 理由 |
|------|---------|------|
| 1 | **DD-006**（C02 tone 設計 token） | 是 DD-004、DD-003、DD-008 的視覺基礎；需最先確認 Portal51 可用的 token |
| 2 | **DD-001**（C16 sticky scroll strategy） | block 實作路徑，需先確認 shell scroll context |
| 3 | **DD-004**（C22 widget type） | 依賴 DD-006 tone token；影響 O1-C 例外入口的互動設計 |
| 4 | **DD-005**（C21 METADATA_ONLY masking） | 需與 backend projection API 同步確認 whitelist；影響前後端一致性 |
| 5 | **DD-003**（C04 + C02 合併 vs 分離） | 較獨立；prototype 已驗證分離方案，可最後決定 |
| 6 | **DD-007**（C08 hidden vs disabled） | 可在 wireframe 第二輪一起確認 |
| 7 | **DD-008**（C03 mode label copy） | 純 UX copy，依賴上游 banner 文案完成後確認 |
| ✅ | **DD-002**（C02 vs C03 order） | 已收斂，不需人類決策 |

---

## 7. Backwrite Plan（決議後）

所有 DD 項目確認後，需按以下計畫回寫至上游 authority：

### 必須回寫（任何決議後立即執行）

| 目標文件 | 寫入內容 |
|---------|---------|
| `docs/07-design/wireframes/ui013-rereview-first-pass.md` | DD-001（C16 scroll strategy）、DD-006（V4/V5 banner 視覺差異）、DD-008（mode strip copy） |
| `docs/07-design/wireframes/ui014-revision-history-first-pass.md` | DD-003（C04+C02 spacing）、DD-004（C22 widget type）、DD-005（METADATA_ONLY masking） |
| `docs/07-design/wireframes/ui010-case-detail-first-pass.md` | DD-003（C04+C02 open question 解答）、DD-007（C08 hidden/disabled strategy） |
| `docs/06-frontend/ui-component-map.md` | DD-001→C16、DD-003→C04、DD-004/DD-006→C02、DD-005→C21、DD-007→C08 各自 notes 補充 |

### 建議回寫（完成相關 authority 後執行）

| 目標文件 | 寫入內容 |
|---------|---------|
| `docs/06-frontend/ui-view-model-spec.md` | DD-005 UI-014 extension whitelist、DD-008 mode label mapping |
| `docs/06-frontend/ui-state-actions-matrix.md` | DD-007 reason text 清單 |
| `docs/06-frontend/frontend-test-scenarios.md` | DD-004（C22 exception navigate）、DD-005（metadata masking）、DD-006（banner tone visible differentiation）的驗收情境 |

### 選填回寫（若有對應位置）

| 目標文件 | 寫入內容 |
|---------|---------|
| `docs/06-frontend/frontend-design-method.md` | DD-001 若 scroll strategy 成為跨頁共用原則 |
| `.ai/governance/frontend-ui.instructions.md` | DD-006 若 exception/blocked/info token 成為治理層規定 |

---

## 8. 延後議題

以下議題屬 P2 範圍，或依賴更多 authority 穩定後才適合處理，本文件暫不展開：

| 議題 | 狀態 | 延後理由 |
|------|------|---------|
| UI-012 C16 CommandBar 策略 | 延後 | 初審作業頁（UI-012）屬 P2；等 DD-001（UI-013）決議後並用 |
| C23 AssignmentDueDateHelperRegion | 延後 | API 欄位可選性待確認；分類為 `deferred` |
| C24 RevisionLifecycleEntryArea | 延後 | lifecycle API 穩定度待確認；分類為 `deferred` |
| Portal51-lib component 詳細用法 | 延後 | `portal51_lib_component_level_usage_detail` 仍 pending confirmation |
| UI-009~UI-012 design decisions | 延後 | P2 頁面；等 P1 決議完成後依需要補充 |
| Responsive / mobile breakpoint strategy | 延後 | MFE shell 的 mobile 支援策略尚未確認 |

---

## 9. 文件維護說明

- 決策一旦人類拍板確認，將 `狀態` 改為 `✅ 已收斂`，並填入確認日期與確認人
- 確認後依 §7 Backwrite Plan 執行回寫，回寫完成後在對應 entry 補充「已回寫：{目標文件}」
- 若有新的 P1 設計議題需要記錄，補充為 DD-009+，遵循本文件的 schema
- 本文件版本追蹤依 git history，不在文件內維護手動版次

---

*文件建立依據：P1 wireframe + prototype runtime review + authority bridge files（frontend-review-gate-pending-items.md、ui-component-map.md）*
