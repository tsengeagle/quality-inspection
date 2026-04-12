你現在要對既有的 `gap-reclassification-report.md` 做一次 **補強分析**。

這不是重跑整份 gap reclassification。
這不是直接 patch code。
這也不是直接改 spec。

你的任務是基於既有結果，補足目前仍不足以支撐決策的部分，讓這份報告更適合作為：

- 人工決策輸入
- 後續修正任務切分依據
- frontend / backend contract 收斂依據

---

## 本輪任務目標

請只做以下四件事：

1. 補上 **repo truth acceptance criteria**
2. 將 **GAP-06** 進一步拆成更精確的子問題
3. 為 **GAP-05** 補上 downstream / design-supporting evidence
4. 為 **GAP-04** 產出可比較的 decision options table

不要擴張到其他 gap。
不要重寫整份報告。
不要直接修改程式碼或設計文件。

---

## 你必須讀取的既有產物

### A. 本輪基底文件
- `gap-reclassification-report.md`

### B. 相關 authority sources
- `design-implementation-gap.md`
- `docs/01-requirements/decision-log.md`
- `docs/03-contracts/api-spec.md`
- `docs/02-ddd/application-service.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/02-ddd/state-machine.md`
- `docs/04-testing/specification-by-example.md`
- `docs/04-testing/test-scenarios.md`

### C. Repository evidence
- current controllers
- DTOs
- exception handler
- security filter / actor context
- relevant API tests
- 若 repo 內已有 frontend code 或 downstream integration code，也必須一併搜尋是否已有實際依賴

---

## 任務一：補 Repo Truth Acceptance Criteria

在既有報告中，雖然每個 gap 都寫了：

- Can current implementation be accepted as repo truth: yes / no / pending decision

但目前尚未定義其判定標準。

你必須補出一節：

## Repo Truth Acceptance Criteria

至少要回答：

- 什麼情況下可以接受 current implementation 成為 repo truth
- 什麼情況下不能接受
- 哪些情況必須先做 human decision
- authority、security、contract、test stability 在這件事中的權重如何

### 要求
這一節必須是 **可重複套用的判準**，不是只對這三個 gap 的事後解釋。

---

## 任務二：拆解 GAP-06

目前 GAP-06 被當成一個整體：
- `UnauthorizedException` → 403
- spec 定義 `UNAUTHORIZED` → 401
- `VALIDATION_ERROR` 不在 spec table 中

這樣還不夠精準。

你必須把 GAP-06 至少拆成兩個子項：

### GAP-06A
HTTP status semantic mismatch  
例如：
- unauthenticated vs forbidden
- 401 vs 403 語意

### GAP-06B
Error code / exception naming mismatch  
例如：
- `UnauthorizedException` 命名是否與 403 semantics 不一致
- `VALIDATION_ERROR` 是否屬 runtime-only code 或 spec 缺漏

### 對每個子項都要回答
- authority source 是什麼
- runtime 現況是什麼
- test 在支持什麼
- 是否可接受為 repo truth
- 分類應是：
  - acceptable divergence
  - unacceptable divergence
  - decision required
- 最小動作是什麼

---

## 任務三：補強 GAP-05 的 downstream / design-supporting evidence

目前報告已將 GAP-05 判為 `unacceptable divergence`，方向合理，但證據鏈還不夠完整。

你必須額外調查：

### A. Downstream evidence
是否已有以下依賴存在：
- frontend code 依賴某些 response 欄位
- API tests 刻意只驗 minimal response
- 其他 module / integration / mock server 依賴 spec-defined payload
- documentation examples 已被當作對外契約使用

### B. Design-supporting evidence
針對以下差異，確認它們是否只是 spec 理想化，還是有其他設計文件支持：

- `attachments` 出現在 draft / revise payload
- submit response 應回 structured object 而非 `null`
- revise response 是否應包含 `oldSubmissionId`
- `stage` / `caseId` / `rereviewRequired` 這些欄位是否有設計依據

### 輸出要求
你必須明確區分：

- repo 實際已有 downstream 依賴的證據
- 僅存在於 design authority 的證據
- 尚未發現足夠 evidence 的項目

### 目的
讓 GAP-05 的結論從「看起來應該不接受」提升為「有更完整證據支撐的不接受」。

---

## 任務四：為 GAP-04 產出 Decision Options Table

目前報告只有 decision topic，還不夠。

你必須為 GAP-04 產出一張正式 decision options table，至少比較以下方案：

### Option A
以 spec 為準，維持 submission-rooted submit / revise path

### Option B
以 current implementation 為準，採 case-rooted command path

### Option C
短期雙軌共存，之後 deprecate 一條

若 repo evidence 支持，也可補 Option D，但不得虛構。

### 每個 option 至少要比較
- path model
- HTTP method model
- 優點
- 缺點 / 風險
- 對 frontend 的影響
- 對既有 API tests 的影響
- 對 spec / example / scenarios 的影響
- 對 backward compatibility 的影響
- 建議採用條件

### 注意
你不是要替人類直接決定。
你是要把選項整理到足以讓人類做決策。

---

## 執行限制

1. 不得重寫整份原報告，只做 targeted strengthening
2. 不得直接改 code / spec
3. 不得憑空假設 frontend 已依賴某欄位；必須先搜尋 repo
4. 不得把沒有 evidence 的東西包裝成已確認事實
5. 若 repo 中找不到 frontend / downstream evidence，必須明說 `not found in this repo`
6. 若某欄位只有 api-spec 支持、沒有其他文件支持，也必須明說

---

## 建議輸出形式

請輸出成一份補強版文件，例如：

`docs/review/gap-reclassification-strengthening.md`

並固定包含以下章節：

# Gap Reclassification Strengthening Pass

## 1. Scope of This Pass
## 2. Repo Truth Acceptance Criteria
## 3. GAP-06 Refinement

### 3.1 GAP-06A HTTP Status Semantic Mismatch
### 3.2 GAP-06B Error Code / Exception Naming Mismatch

## 4. GAP-05 Evidence Strengthening

### 4.1 Downstream Evidence
### 4.2 Design-Supporting Evidence
### 4.3 Refined Conclusion

## 5. GAP-04 Decision Options Table

表格後再補：
### 5.1 Recommended Decision Framing
### 5.2 Information Still Missing

## 6. What This Changes in the Original Reclassification Report
## 7. Minimal Follow-up Actions

---

## Completion Rules

你不得宣稱：
- GAP-04 已被自動解決
- GAP-05 一定已被完整證明
- GAP-06 所有子問題都應同一方式處理
- current implementation 已自然成為 repo truth

除非 evidence 足夠。

若 evidence 不足，必須標記：
- pending decision
- pending confirmation
- not found in this repo
- supported only by design authority

本輪不是全面重跑 gap reclassification。

請只補：
- repo truth 判準
- GAP-06 拆解
- GAP-05 補證據
- GAP-04 decision options

其他 gap 不要展開。
若發現其他問題，最多放進「out of scope observations」，不要擴張主體。
