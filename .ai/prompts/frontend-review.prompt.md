你現在要接手本專案的 **runtime review** 任務。

# 任務目標

使用本地執行能力，實際啟動並打開目前 repo 內可用的 prototype / frontend artifact，然後依 repo 內現有 authority、wireframe、prototype 與 design execution 文件，自行盤點並完成 runtime review。

本輪重點不是改 code，也不是補文件，而是：

1. 先盤點 repo，找出本輪應 review 的 authority、design spec、wireframe、prototype artifact
2. 實際啟動 artifact
3. 實際打開頁面與切換 scenarios / variants
4. 根據 repo 內已存在的規格與 flow 做 runtime review
5. 回報：
   - 哪些項目通過
   - 哪些項目部分符合
   - 哪些項目不符合
   - 哪些議題應進下一步 design / implementation decision

---

# 硬性要求

1. 你必須真的啟動 artifact，不能只看 source code 推測。
2. 你必須真的打開頁面，不能只讀 markdown / wireframe / spec 後就宣稱 review 完成。
3. 若啟動、路由、scenario 切換、頁面打開任一步失敗，必須明確回報阻塞原因，不得假裝 review 完成。
4. 若你無法操作瀏覽器或本地執行能力，必須明確說明你實際能做到的 runtime 驗證邊界。
5. 本輪只做 review，不做修改，除非我之後另外要求。
6. 除非 repo 內真的找不到必要 authority / wireframe / runtime artifact，否則不要先問我要看哪些檔案。
7. 若找不到必要規格，先列出你已搜尋過的候選文件與缺口，再問我。

---

# 先輸出的內容：執行計畫

你第一則回覆只能包含：

## 1. Repo discovery result
先說明你盤點到的：
- authority files
- wireframe / design execution files
- prototype / runtime artifact
- route / scenario / review harness 入口
- 你判定本輪最 relevant 的 pages / scenarios / critical flow

## 2. Runtime plan
- artifact 路徑
- 啟動方式
- 預計打開的 URL
- scenario / variant 切換方式
- 你要如何做 runtime 驗證

## 3. Coverage plan
- 本輪預計檢視哪些 pages
- 本輪預計檢視哪些 scenarios / variants
- 是否有 critical flow 要走
- 你選這些頁與場景的依據是什麼

## 4. Failure criteria
- 什麼情況下你必須停止並回報失敗
- 若找不到足夠規格，缺口是什麼
- 若要向我提問，需先列出已盤點過的文件與缺漏

在我回覆「批准執行」之前，不要開始 review。

---

# Repo discovery 原則

你必須自行盤點 repo，找出本輪應該依循的內容，至少包含：

1. repo-level hard rules / governance / instruction files
2. frontend design method / roadmap / bridge files
3. wireframe brief / wireframe outputs / prototype execution相關文件
4. 實際可執行的 runtime artifact
5. route / scenario index / review harness 入口
6. 與 page mode / projection / blocked / exception / flow 有直接關聯的規格

若 repo 已明確收斂 P1 / P2、high-risk targets、critical flow，應優先使用現有收斂結果，不要自行重發明範圍。

---

# Runtime review scope 選擇原則

你應優先 review：
1. repo 中已明確標為 P1、first-pass、high-risk、critical flow 的頁面
2. 能驗證 command page / history page / control center 邊界的頁面
3. 能驗證 blocked / exception / metadata-only projection 差異的場景
4. repo 中已有固定 scenario / review harness 的頁面與 variants

除非 repo 內沒有任何收斂結果，否則不要自行擴張到整個系統。

---

# Review checklist 原則

你應依 repo 現有規格，自行整理出本輪最適用的 runtime review checklist，至少應涵蓋：

1. exception flow
2. blocked vs exception clarity
3. metadata-only projection
4. page type boundaries
5. high-risk targets
6. deferred / placeholder handling

若 repo 內已有更具體 checklist 或 wireframe review criteria，應優先沿用。

---

# 執行方式要求

## 1. 先盤點，再啟動
先找出：
- authority
- wireframe / prototype docs
- runtime artifact
- review harness / scenario entry

再開始執行 runtime review。

## 2. 先啟動 artifact
你必須：
- 找到專案路徑
- 確認啟動指令
- 實際執行
- 確認本地 URL

## 3. 再打開頁面
你必須實際進入對應頁面或 scenario index / review harness。

## 4. 再逐步檢視
請依你從 repo 盤點出來的 pages、scenarios、critical flow 與 checklist 順序做檢視。

## 5. 本輪不要修改
除非我明確要求，否則本輪只 review，不得順手修改實作。

---

# 回報格式

完成後請用以下格式回報：

## 1. Repo discovery summary
- 你找到哪些 authority files
- 你找到哪些 wireframe / design execution files
- 你找到哪個 runtime artifact
- 你找到哪些 review harness / scenario 入口
- 你最後選定 review 的 pages / scenarios / critical flow

## 2. Runtime setup
- artifact 路徑
- 啟動指令
- 啟動是否成功
- 實際打開的 URL
- 是否真的看到了頁面 / scenario index / harness

## 3. Scenario coverage
列出你實際檢視了哪些 pages 與 scenarios。

## 4. Checklist result
請至少用以下結構回報：

```yaml
review_result:
  exception_flow:
    status: pass | partial | fail
    notes: ""

  blocked_vs_exception_clarity:
    status: pass | partial | fail
    notes: ""

  metadata_projection:
    status: pass | partial | fail
    notes: ""

  page_type_boundaries:
    status: pass | partial | fail
    notes: ""

  high_risk_targets:
    status: pass | partial | fail
    notes: ""

  deferred_handling:
    status: pass | partial | fail
    notes: ""
```

## 5. Evidence

請補充你是根據哪些**實際頁面觀察**得出結論。
不要只說「從 code 看起來」。
必須明確寫出：

* 看了哪個 page / scenario
* 看到了什麼
* 為什麼判定 pass / partial / fail

## 6. Candidate issues

列出你認為應進下一步 design / implementation decision 的議題。

## 7. Blocking issues

若有任何 runtime / route / scenario 切換 / 頁面呈現問題，明確列出。

## 8. Missing authority or gaps

若本輪真的有規格缺口，請列出：

* 缺什麼
* 你已搜尋過哪些文件
* 為什麼你判定這是缺口
* 這時才可問我

---

# 明確禁止事項

1. 不得只讀 code 就宣稱 runtime review 完成
2. 不得只讀 spec / wireframe / markdown 就宣稱 runtime review 完成
3. 不得跳過你自己盤點出來的高風險頁或 critical flow
4. 不得只給抽象評論，不給實際 scenario 觀察
5. 不得在本輪直接修改 artifact
6. 不得把推測當成實際檢視結果
7. 不得在尚未盤點 repo 的情況下先向我索取文件清單

現在先只輸出執行計畫，不要開始 review。
