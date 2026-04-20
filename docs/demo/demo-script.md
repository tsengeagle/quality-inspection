# Demo Script v1 - Quality Inspection — Demo Automation Phase

## 1. 文件目的

本文件定義品質審查系統在 **Demo Automation Phase** 中的標準展示劇本。

此文件的用途：

- 固定 demo 的展示順序與故事線
- 固定每一頁的操作步驟
- 固定每一步的預期觀察點
- 作為以下交付物的上游依據：
  - demo launcher
  - Playwright preflight
  - Playwright demo script
  - demo / e2e 雙模式切換
  - 最小 SOP

本文件不是：

- Playwright 測試碼
- API spec
- contract spec
- UI wireframe
- 完整 UAT 腳本

---

## 2. 使用原則

1. demo script 優先固定 **業務展示流程**，不是先寫技術操作細節。
2. 每一步都必須能映射到：
   - 頁面
   - 穩定資料
   - 驗收觀察點
3. demo script 與 e2e flow 應共用同一條主流程，不可各走各的。
4. 若某步驟依賴固定 seed / fixture，必須在 `demo-data-contract.md` 中有對應資料。
5. 若某畫面欄位尚屬 not in scope，必須明確標示，不可在 demo script 假設它已存在。

---

## 3. Demo 目標

本次 demo 的目標是展示：

- 本系統已可用 **live standalone chain** 運作
- 抽選執行、批次檢視、案件分派三個關鍵頁面已可串成一條穩定的展示主線
- Phase 1 最小 contract 補值已實際呈現在真頁面上
- 後續 Playwright 可依此劇本同時支援：
  - demo 模式
  - e2e 驗收模式

---

## 4. Demo 範圍

### In Scope
- UI-006 抽選執行頁
- UI-007 抽選批次列表頁
- UI-011 案件分派頁

### Out of Scope
- integrated shell demo
- gateway integration demo
- 批次詳情 deferred 功能
- 完整指派提交成功流程
- `effectivePeriod`
- 真正獨立語意的 `current scoreSheetVersionId`

---

## 5. 展示前提

### 5.1 執行模式
本 demo 使用：

- frontend standalone
- local dev proxy
- local backend
- local demo seed

### 5.2 預設資料前提
本 demo 預設依賴以下固定資料：

- projectId = `PROJECT-001`
- batchId = `BATCH-001`
- caseId = `CASE-011`

詳細固定值由 `demo-data-contract.md` 定義。

### 5.3 成功前提
在 demo 開始前，系統應滿足：

- frontend 可開啟
- backend 可回應
- proxy 可轉發
- 三頁 URL 可進入
- 關鍵 API 回 200
- 無致命 console / page error

---

## 6. 展示故事線總覽

本 demo 採用以下展示順序：

1. UI-006 抽選執行頁
2. UI-007 抽選批次列表頁
3. UI-011 案件分派頁

此順序的業務語意為：

- 先展示「抽選作業與專案摘要」
- 再展示「抽選批次結果」
- 最後展示「抽中案件後續分派作業」

---

## 7. 頁面級展示劇本

---

## 7.1 UI-006 抽選執行頁

### 頁面角色
展示單一審查專案的抽選執行入口、摘要資訊與最近一次正式結果。

### 路由
`/sampling/PROJECT-001`

### 展示目標
讓觀眾看到：

- 這是一個已啟用的專案
- 專案已有版本與評分表
- 有明確的品質門檻
- 系統已存在正式批次執行結果
- 開始 / 完成時間已能顯示在畫面上

### 必要操作步驟
1. 開啟 UI-006
2. 等待頁面主內容載入完成
3. 停留於專案摘要區
4. 停留於規則 / 執行摘要區
5. 停留於最近一次正式執行結果區

### 必要觀察點
- 專案狀態可見
- 專案版本可見
- 評分表資訊可見
- 品質門檻可見
- 最近正式批次編號可見
- 開始時間可見
- 完成時間可見

### 建議講解重點
- 此頁是抽選執行與結果摘要的入口
- Phase 1 補值已使專案與執行摘要更完整
- 畫面目前展示的是 live backend 回傳值，不是靜態假資料

### 成功條件
- 頁面正常載入
- 無致命 page error
- 必要觀察點都存在
- API 回應成功且與畫面一致

### 非必要但可加分的展示點
- 顯示最近一次正式批次編號
- 說明品質門檻的用途
- 說明這裡是後續 UI-007 / UI-011 的上游入口

---

## 7.2 UI-007 抽選批次列表頁

### 頁面角色
展示抽選批次清單與批次級摘要。

### 路由
`/sampling/batches`

### 展示目標
讓觀眾看到：

- 系統已有正式批次
- 每個批次可顯示專案名稱與版本
- 批次狀態與排除件數可視
- 列表頁主結構穩定

### 必要操作步驟
1. 開啟 UI-007
2. 等待列表載入完成
3. 停留於第一筆 batch row
4. 停留於狀態 / 摘要欄位
5. 停留於 row action 區

### 必要觀察點
- batchId 可見
- 批次狀態可見
- 專案名稱可見
- 專案版本可見
- excludedCount 可見
- row action 可見

### 建議講解重點
- 此頁把抽選批次從單次執行畫面提升為可查詢的清單
- Phase 1 補值使批次列更接近可讀的業務摘要
- 目前列表顯示的是 live seed 資料，不是 route mock

### 成功條件
- 頁面正常載入
- 列表有資料
- 第一筆批次列的關鍵欄位可見
- 無表格錯位、空 row、壞掉 action

### 非必要但可加分的展示點
- 說明排除件數與批次狀態的閱讀方式
- 說明之後可延伸到批次詳情，但不在本次 demo scope

---

## 7.3 UI-011 案件分派頁

### 頁面角色
展示單一案件的分派上下文、來源案件資訊與候選人清單。

### 路由
`/cases/CASE-011/assign`

### 展示目標
讓觀眾看到：

- 抽中案件可進入分派作業
- 案件具備專案上下文
- 可看到來源案件識別
- 未指派情境下，`assignedAt` fallback 正常
- 候選人清單與 blocked / inactive 呈現仍正常

### 必要操作步驟
1. 開啟 UI-011
2. 等待 assignment context 載入完成
3. 停留於 header / context 區
4. 停留於來源案件資訊區
5. 停留於候選人列表
6. 停留於 action zone

### 必要觀察點
- 專案名稱可見
- sourceCaseKey 可見
- assignedAt 或其 fallback 顯示可見
- 候選人列表可見
- inactive / blocked reason 呈現正常
- action zone 可見

### 建議講解重點
- 此頁展示抽中案件進入人工流程的入口
- Phase 1 補值讓案件上下文更完整
- 即使目前未指派，頁面 fallback 也已穩定，不會出現 binding 破裂

### 成功條件
- 頁面正常載入
- context 與 candidate list 皆成功回應
- fallback 行為正常
- 無 pageerror / 致命 console error

### 非必要但可加分的展示點
- 說明 inactive 候選人的 blockedReason
- 說明這裡展示的是分派上下文，不是完整提交流程

---

## 8. 展示節奏建議

### Demo 模式
建議節奏：

- 開頁後先等待穩定
- 每頁至少停留一段可閱讀時間
- 僅做必要 click / reload / focus
- 不做過多來回跳轉

### E2E 模式
建議節奏：

- 相同步驟順序
- 減少停留時間
- 強化 assert
- 失敗即停止

---

## 9. 共用成功判準

以下判準同時適用於：

- 人工 demo
- Playwright demo mode
- Playwright e2e mode

### 必須成立
- 三頁都可載入
- 關鍵資料由 live backend 回應
- API 與 UI 顯示一致
- 無致命 page error
- 無破壞既有頁面骨架的顯示錯誤

### 不必在本文件內宣稱完成
- integrated demo
- shell / gateway integration ready
- Phase 2 以外的 contract redesign
- 所有 spec drift 已消除

---

## 10. 與其他文件的關係

本文件應與以下文件一起使用：

- `demo-data-contract.md`
- `demo-acceptance-checklist.md`

關係如下：

- `demo-script.md`：定義展示順序與頁面操作
- `demo-data-contract.md`：定義穩定資料依賴
- `demo-acceptance-checklist.md`：定義成功判準與可驗收觀察點

---

## 11. 後續映射

此文件完成後，後續應映射到：

1. demo launcher
2. Playwright preflight
3. Playwright demo script
4. demo / e2e 雙模式切換
5. demo SOP

---

## 12. 待補區

後續可補，但不在本版定稿範圍：

- 每頁停留秒數建議
- 是否需要自動 reload / highlight
- 是否要加入展示旁白文本
- 是否要把 row action / button click 也納入 scripted demo
