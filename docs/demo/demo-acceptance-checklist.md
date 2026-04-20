# Demo Acceptance Checklist v1 - Quality Inspection — Demo Automation Phase

## 1. 文件目的

本文件定義品質審查系統在 **Demo Automation Phase** 中的正式驗收清單。

此文件的用途：

- 作為人工 demo 驗收的檢查清單
- 作為 Playwright preflight / demo mode / e2e mode 的 assert 基線
- 固定本輪 demo 何者必須成功、何者可留在 scope 外
- 讓 demo script、資料契約與自動化腳本共用同一套完成定義

本文件不是：

- demo script
- Playwright 程式碼
- API spec
- 測試報告

---

## 2. 使用原則

1. checklist 只驗收 **正式納入 demo 範圍** 的項目。
2. 每一條 checklist 都應能對應到：
   - 頁面
   - 資料契約
   - 可觀察結果
3. 若某條件需要穩定資料，應以 `demo-data-contract.md` 為準。
4. 若某條件只是展示加分項，不應混入 must-pass checklist。
5. 若某項目不在本輪範圍，必須標示為 `not in scope`，不得當成失敗。

---

## 3. 驗收模式

本文件同時支援兩種驗收模式：

### 3.1 Demo Mode
- 用於展示前人工確認
- 可包含頁面停留與可視化觀察
- 重視畫面呈現、資料存在、流程順暢

### 3.2 E2E Mode
- 用於 Playwright 自動驗收
- 重視可重跑、可斷言、可回歸
- 不要求展示節奏，但要求關鍵 assert 穩定

---

## 4. 驗收範圍

### In Scope
- UI-006 抽選執行頁
- UI-007 抽選批次列表頁
- UI-011 案件分派頁
- live standalone chain
- Phase 1 最小 contract 補值

### Out of Scope
- integrated shell demo
- gateway integration
- `effectivePeriod`
- 真正獨立語意的 `current scoreSheetVersionId`
- UI-007 批次詳情 deferred
- 完整案件分派提交流程
- 非 UI-006 / UI-007 / UI-011 的頁面

---

## 5. Preflight Checklist

---

## 5.1 環境可用性

### 必須通過
- [ ] frontend standalone 已啟動
- [ ] backend local profile 已啟動
- [ ] proxy 已可轉發 `/api/quality-inspection/**`
- [ ] demo seed 已載入
- [ ] 三頁 URL 可開啟

### 驗收說明
- 任一項失敗，demo 不應開始
- Playwright preflight 應先驗這一段

---

## 5.2 API 可用性

### 必須通過
- [ ] UI-006 對應 execute-context API 回 200
- [ ] UI-007 對應 sampling-batches API 回 200
- [ ] UI-011 對應 assignment-context API 回 200
- [ ] UI-011 對應 assignment-candidates API 回 200

### 驗收說明
- 這些 API 是本輪 demo 的最小 live chain 依賴
- 若 API 未過，不應進入頁面驗收

---

## 5.3 致命錯誤檢查

### 必須通過
- [ ] 無致命 page error
- [ ] 無新出現的 console hard error
- [ ] 無 proxy 504
- [ ] 無因 binding 失敗導致的空白頁 / 壞頁

---

## 6. 頁面級 Checklist

---

## 6.1 UI-006 抽選執行頁

### 路由
`/sampling/PROJECT-001`

### Must Pass
- [ ] 頁面成功載入
- [ ] 專案狀態可見
- [ ] 專案版本可見
- [ ] 評分表資訊可見
- [ ] 品質門檻可見
- [ ] 最近正式批次編號可見
- [ ] 開始時間可見
- [ ] 完成時間可見
- [ ] 頁面主要結構未被破壞
- [ ] API 與 UI 顯示一致

### 推薦人工觀察點
- [ ] 專案狀態是否顯示為 `ACTIVE`
- [ ] 版本是否顯示為 `v1`
- [ ] 評分表是否顯示 `SS-PROJECT-001-V1`
- [ ] 品質門檻是否顯示 `85.00`
- [ ] 正式批次是否顯示 `BATCH-001`

### E2E 斷言最低要求
- [ ] 頁面到達正確路由
- [ ] 關鍵摘要欄位存在
- [ ] 關鍵摘要值符合資料契約
- [ ] reload 後仍可成功載入

---

## 6.2 UI-007 抽選批次列表頁

### 路由
`/sampling/batches`

### Must Pass
- [ ] 頁面成功載入
- [ ] 批次列表有資料
- [ ] 第一筆 batchId 可見
- [ ] 批次狀態可見
- [ ] 專案名稱可見
- [ ] 專案版本可見
- [ ] excludedCount 可見
- [ ] row action 可見
- [ ] 表格結構未被破壞
- [ ] API 與 UI 顯示一致

### 推薦人工觀察點
- [ ] 第一筆資料為 `BATCH-001`
- [ ] 批次狀態顯示 `COMPLETED`
- [ ] 專案名稱為 `2026 Q2 門診品質抽選專案`
- [ ] 專案版本顯示 `v1`
- [ ] 排除件數顯示 `0`

### E2E 斷言最低要求
- [ ] 至少一筆批次列存在
- [ ] 第一筆批次列的關鍵摘要欄位存在
- [ ] row action 未壞
- [ ] reload 後列表仍正常

---

## 6.3 UI-011 案件分派頁

### 路由
`/cases/CASE-011/assign`

### Must Pass
- [ ] 頁面成功載入
- [ ] 專案名稱可見
- [ ] sourceCaseKey 可見
- [ ] assignedAt 或 fallback 可見
- [ ] 候選人列表可見
- [ ] inactive / blocked reason 呈現正常
- [ ] action zone 可見
- [ ] API 與 UI 顯示一致
- [ ] fallback 行為未破壞頁面

### 推薦人工觀察點
- [ ] 專案名稱為 `2026 Q2 門診品質抽選專案`
- [ ] sourceCaseKey 為 `MR-CASE-011`
- [ ] 因 current reviewer 為 null，畫面顯示 `尚未指派`
- [ ] inactive 候選人存在
- [ ] inactive 候選人的 blockedReason 正常可見

### E2E 斷言最低要求
- [ ] 頁面到達正確路由
- [ ] context summary 可見
- [ ] candidate list 可見
- [ ] fallback 顯示可見
- [ ] reload 後 assignment-context 與 assignment-candidates 仍成功

---

## 7. API vs UI Cross-check Checklist

---

## 7.1 共通原則

### 必須通過
- [ ] API response 中存在的正式觀察欄位，UI 上也可見
- [ ] UI 顯示值與 API payload 一致
- [ ] 無 `undefined`
- [ ] 無空 label
- [ ] 無壞掉 fallback

---

## 7.2 UI-006 Cross-check

### 必須通過
- [ ] `projectStatus` API / UI 一致
- [ ] `versionNo` API / UI 一致
- [ ] `scoreSheetId` API / UI 一致
- [ ] `qualityThreshold` API / UI 一致
- [ ] `startedAt` API / UI 一致
- [ ] `finishedAt` API / UI 一致

---

## 7.3 UI-007 Cross-check

### 必須通過
- [ ] `projectName` API / UI 一致
- [ ] `projectVersionNumber` API / UI 一致
- [ ] `excludedCount` API / UI 一致

---

## 7.4 UI-011 Cross-check

### 必須通過
- [ ] `projectName` API / UI 一致
- [ ] `sourceCaseKey` API / UI 一致
- [ ] 若 `assignedAt = null`，UI 正確顯示 fallback
- [ ] candidate list 與 inactive candidate 呈現合理

---

## 8. Demo Mode 專屬 Checklist

### 必須通過
- [ ] 三頁切換順暢
- [ ] 每頁可停留並解說
- [ ] 關鍵欄位不需要人工猜測
- [ ] 觀眾能看出三頁的業務連貫性

### 加分項
- [ ] 每頁停留節奏自然
- [ ] click / reload 節奏自然
- [ ] 候選人與 blocked state 足夠清楚
- [ ] 無過快跳頁造成觀察困難

---

## 9. E2E Mode 專屬 Checklist

### 必須通過
- [ ] 腳本可從乾淨環境重跑
- [ ] 關鍵 assert 穩定
- [ ] 不依賴隨機資料
- [ ] 不依賴人工介入
- [ ] 同一條主流程可作為回歸驗收

### 加分項
- [ ] 支援 headed / headless 雙模式
- [ ] 支援 slow demo / fast verify 雙模式
- [ ] selector 穩定且可維護
- [ ] 錯誤訊息可幫助快速定位失敗頁面

---

## 10. 驗收結果分類

### Passed
- 所有 must-pass 項目皆通過

### Partially Passed
- 主流程通過
- 但有非核心項目未通過
- 不影響本輪 demo 主張

### Failed
- 任一頁主流程失敗
- 任一 API / UI cross-check 失敗
- 任一致命錯誤存在
- 無法完成 live standalone 展示

### Not Verified in This Run
- 本輪未實際驗到的項目
- 必須明確列出原因

### Not in Scope
- 本輪明確排除項目
- 不得列為失敗

---

## 11. 正式完成條件

只有在以下條件都成立時，才可宣稱：

**Demo Automation Phase 的資料與頁面基線可用**

### 必須全部成立
- [ ] preflight 全部通過
- [ ] UI-006 must-pass 全部通過
- [ ] UI-007 must-pass 全部通過
- [ ] UI-011 must-pass 全部通過
- [ ] API vs UI cross-check 全部通過
- [ ] 無新的致命錯誤
- [ ] not in scope 項目已明確揭露

---

## 12. 明確不納入本版驗收的項目

```yaml id="0l2bwd"
not_in_scope_acceptance:
  - effectivePeriod
  - currentScoreSheetVersionId_with_independent_semantics
  - batch_detail_deferred_fields
  - full_assignment_submission_flow
  - integrated_shell_demo
  - gateway_integration_demo
  - pages_other_than_UI_006_UI_007_UI_011
```

---

## 13. 與其他文件的關係

本文件應與以下文件一起使用：

* `demo-script.md`
* `demo-data-contract.md`

關係如下：

* `demo-script.md`：定義展示流程
* `demo-data-contract.md`：定義固定資料依賴
* `demo-acceptance-checklist.md`：定義成功判準與驗收邊界

---

## 14. 待補區

後續可補，但不在本版定稿範圍：

* Demo mode 停留時間建議
* 不同瀏覽器的驗收差異
* screenshot / video capture 是否納入正式驗收
* selector 命名規範 appendix
