你現在要接手本專案的 **P1 low-fidelity frontend prototype execution**。

# 任務目標

不要使用 Figma。
不要使用任何設計平台作為主輸出。

請直接產出一個 **可執行的 low-fidelity frontend prototype**，作為：
- wireframe
- flow validation artifact
- 前端開發前置骨架
- 後續 design decisions 與 implementation planning 的依據

本輪只處理以下 3 頁：

1. UI-013 複審作業頁
2. UI-014 Revision 歷程頁
3. UI-010 案件詳情頁

---

# 硬性要求

1. 你必須先完成 authority 與現有 artifact 盤點，再給出執行計畫；若無新衝突，可直接開始實作，不需要等待額外批准。
2. 本輪主輸出必須是 **可執行 prototype**，不得以 markdown wireframe、ASCII 草圖、Mermaid、純文字說明或僅文件更新視為完成。
3. 若你無法產出可執行 prototype，必須明確回報阻塞原因，不得改用設計文件替代並宣稱完成。
4. prototype 必須能讓 reviewer 在瀏覽器中切換 variants、理解 flow、檢視 high-risk 結構。
5. prototype 只做 low-fidelity，不做品牌視覺設計，不做 high-fidelity mockup。
6. 免費 / 本地方案優先；不要把 Figma 當成預設方案。
7. 若任務改為需要 Figma / Figma MCP、付費能力，或遇到額度 / 權限 / API / MCP / 方案限制，必須先停下來回報。

---

# 首選輸出形式

請優先以 **React low-fi prototype** 方式產出。

建議但不強制：
- React
- Vite
- TypeScript
- 單一最小專案或可直接掛在現有 repo 的 prototype 區
- mock data / mock state
- 不依賴真後端
- 不依賴真 API 呼叫

若 repo 既有前端骨架不適合直接承載，可在 repo 內建立最小 prototype 區，但要在盤點與執行計畫中說明。

---

# 必讀 authority inputs

請先完整閱讀以下文件：

## Foundational truth
- docs/06-frontend/frontend-design-method.md
- docs/02-ddd/state-machine.md
- docs/02-ddd/permission-matrix.md
- docs/06-frontend/ui-state-actions-matrix.md
- docs/06-frontend/frontend-test-scenarios.md

## Bridge files
- docs/06-frontend/ui-route-module-map.md
- docs/06-frontend/ui-view-model-spec.md
- docs/06-frontend/ui-api-binding-matrix.md
- docs/06-frontend/ui-component-map.md

## Review / backwrite
- docs/06-frontend/frontend-review-gate-pending-items.md

## UI / design structure
- docs/05-ui/ui-information-architecture.md
- docs/05-ui/ui-page-inventory.md
- docs/05-ui/ui-page-spec-ui010案件詳情頁.md
- docs/05-ui/ui-page-spec-ui013複審作業頁.md
- docs/05-ui/ui-page-spec-ui014revision歷程頁.md
- docs/05-ui/ui-layout-archetypes.md
- docs/07-design/ui-wireframe-brief.md

## P1 wireframe outputs
- docs/07-design/wireframes/README.md
- docs/07-design/wireframes/ui013-rereview-first-pass.md
- docs/07-design/wireframes/ui014-revision-history-first-pass.md
- docs/07-design/wireframes/ui010-case-detail-first-pass.md

---

# 已定稿前提

以下內容已定稿，不得重新打開：

1. CLOSED 後 rereview revise UI 入口策略已批准並回寫
2. command path 已統一為 case-rooted
3. metadata-only 欄位白名單已採 shared base + page extension
4. metadata-only 是 projection，不是另一套頁
5. UI-013 是 command page
6. UI-014 是 history / traceability page
7. UI-010 是 control center
8. shared structures 已凍結：
   - C01 CaseSummaryHeader
   - C02 StatusOrBlockedBanner
   - C03 ModeIndicatorStrip
   - C04 ProjectionNoticeBanner
9. high-risk visualization targets 已凍結：
   - C16 ReviewCommandBar
   - C21 RevisionCompareSummaryRegion
   - C22 RevisionNavigationActionZone

---

# 先輸出的內容：盤點與執行計畫

你第一則回覆至少要包含下列內容；完成盤點與計畫後，若無新阻塞，可直接開始實作：

## 1. Prototype architecture plan
至少說明：
- 你打算把 prototype 放在哪裡
- 是否在現有 repo 中建立新目錄
- 你打算如何組織 routes / screens / variant switchers
- 你打算用哪些最小依賴
- 你如何避免污染正式 implementation area

## 2. Screen plan
列出你要建立哪些 screen / route：
- UI-013
- UI-014
- UI-010

並說明每頁如何切換 variants。

## 3. Variant handling plan
必須說明如何讓 reviewer 快速切換 high-risk 場景，例如：
- URL query
- local switcher panel
- top debug toolbar
- mock state selector

## 4. Verification plan
你要如何證明 prototype 真的可用。
至少要包含：
- 可啟動方式
- 可看到哪些頁
- 可切哪些 variants
- 哪些 high-risk targets 可直接被 reviewer 驗證

## 5. File plan
預計建立哪些檔案與目錄。
要清楚分出：
- prototype-only files
- shared mock data
- variant definitions
- minimal styles

## 6. Failure criteria
明確列出什麼情況下你必須停止並回報失敗，例如：
- 無法在 repo 中建立可運行 prototype
- 只能輸出文件而無法輸出可執行頁面
- authority 之間出現新衝突，導致無法安全實作

若 authority 無新衝突，盤點與計畫完成後即可開始實作。

---

# Prototype 範圍與最低要求

## UI-013 複審作業頁
必須支援切換：
- NEW_REREVIEW
- EDIT_DRAFT
- REVISION
- READONLY_BLOCKED
- CLOSED_REVISION_EXCEPTION

必須呈現：
- CaseSummaryHeader
- StatusOrBlockedBanner
- ModeIndicatorStrip
- ReviewFormSummaryStrip
- ReviewCommandBar
- ReviewVersionSummaryRegion
- InitialSummaryReferenceRegion
- FinalDecisionSummaryRegion
- rereview form 主區
- commentary / attachment 區

特別要求：
- READONLY_BLOCKED 與 CLOSED_REVISION_EXCEPTION 必須一眼區分
- CLOSED_REVISION_EXCEPTION 必須表達成 submit blocked / revise allowed
- 不可讓畫面看起來像 generic edit form
- C16 必須高可見

## UI-014 Revision 歷程頁
必須支援切換：
- ALL
- REREVIEW_ONLY
- FULL_COMPARE
- METADATA_ONLY_COMPARE
- CLOSED_EXCEPTION_ENTRY

必須呈現：
- CaseSummaryHeader
- StatusOrBlockedBanner
- ModeIndicatorStrip
- ProjectionNoticeBanner
- RevisionTimelineRegion
- RevisionCompareSummaryRegion
- RevisionNavigationActionZone

特別要求：
- effective version 必須有明顯強調
- C22 必須是 navigate-only
- metadata-only compare 只能表達「降級」，不能假裝資訊量已定稿
- UI-014 不得出現 submit bar

## UI-010 案件詳情頁
必須支援切換：
- FULL
- METADATA_ONLY
- TERMINAL_BLOCKED
- ROLE_LIMITED
- CLOSED_REREVIEWER_HINT

必須呈現：
- CaseSummaryHeader
- StatusOrBlockedBanner
- ModeIndicatorStrip
- ProjectionNoticeBanner
- CaseStatusActionPanel
- TraceabilitySummaryRegion
- InitialReviewSummaryRegion
- RereviewSummaryRegion
- RevisionLifecycleEntryArea [placeholder only]

特別要求：
- C08 只能是 navigate-only
- traceability 與 review summaries 必須分區
- FULL / METADATA_ONLY 必須是同頁不同變體
- C24 只能 placeholder

---

# Prototype 原則

## 一、只做 low-fidelity
請只表達：
- 區塊位置
- 資訊層級
- action 類型
- banner / mode / projection / blocked 差異
- flow

不要做：
- 品牌色
- 設計 token
- 最終 icon
- 精緻視覺裝飾
- 高保真視覺規格

## 二、共享結構必須一致
至少要讓 reviewer 看得出跨頁一致的：
- header
- banner
- mode strip
- projection notice

## 三、deferred 區塊只能 placeholder
尤其：
- C23 AssignmentDueDateHelperRegion
- C24 RevisionLifecycleEntryArea
- lifecycle 深層細節
- metadata-only compare 最終資訊密度

## 四、不可自行補造新規則
若某個行為或欄位無法從 authority 推導：
- 用 prototype 註記顯示為 open question
- 不得自行當成定案畫死

---

# 建議的 prototype 互動方式

至少要提供一種 reviewer 易用的方式切換 variants，例如：
- 左側 variant list
- 頁首 debug toolbar
- query param
- route-based scenario pages

你可以自行選擇，但要在執行計畫中說明理由。

---

# 交付內容

完成時至少要交付：

## 1. 可執行 prototype
能在本地啟動，並可打開 UI-013 / UI-014 / UI-010。

## 2. Variant switching
每頁可切換指定 variants。

## 3. Flow validation
至少能表達：
- UI-010 → UI-014
- UI-014 → UI-013 [closed exception entry]
- UI-010 → UI-013 [navigate-only hint context]
不要求真路由邏輯完整，但要能看懂 flow。

## 4. Open questions panel or notes
在 prototype 中或伴隨文件中清楚列出應進 `ui-design-decisions.md` 的議題。

## 5. Minimal run instructions
說明如何啟動與驗證。

---

# 明確禁止事項

1. 不得先輸出 markdown wireframe 來替代 prototype
2. 不得開始 ui-design-decisions.md
3. 不得開始 ui-visual-baseline.md
4. 不得寫正式 implementation code 並宣稱已開始產品開發
5. 不得把 metadata-only 畫成獨立頁
6. 不得把 UI-014 畫成 submit page
7. 不得把 UI-010 畫成 command submit 頁
8. 不得把 deferred 區塊畫成定稿內容

---

# 完成條件

只有在以下條件都滿足時，才能宣稱本輪完成：

- 已產出可執行的 low-fidelity frontend prototype
- 已包含 UI-013 / UI-014 / UI-010 三頁
- 每頁都可切換指定 variants
- high-risk targets 已被實際表達
- deferred 區塊仍維持 placeholder
- 已列出應進 `ui-design-decisions.md` 的 open questions
- 不再依賴 Figma 作為主輸出

現在先只輸出執行計畫，不要實作。
