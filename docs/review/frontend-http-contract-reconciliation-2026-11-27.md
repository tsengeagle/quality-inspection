# 前端回應型別與 HTTP 契約的核對（2026-11-27）

`docs/harness/03-harness-plan.md` 第三節 P10 的缺口「前後端各自持有的 API 契約沒有互相比對」的第一次核對。

- 核對時間點：`main` 的 `f9fe97e`。
- 範圍：前端 service 宣告的**回應型別**，對照 `specs/*/http.yaml` 的成功回應形狀。
- **沒有核對**：請求的本文與查詢參數、錯誤代碼、欄位值的語意（例如列舉有哪些值）。
- 本文件只記錄差異，不改前端、不改後端、不改契約。要修哪一邊，見第六節。
- **所有發現都是讀程式得到的，沒有拿前端去打真實的後端。** 前端的單元測試與 E2E 都以 mock 的回應執行（`05-current-state.md` 第八節），所以下面的差異不會讓任何現有的測試失敗。

## 一、方法

| 來源 | 取法 |
|---|---|
| 前端 | `features/**/services/*.service.ts` 裡每一個 `http.get/post<ApiResponse<T>>` 的 `T`，展開 `interface` 的欄位（含巢狀與陣列） |
| 契約 | `specs/*/http.yaml` 每個端點的 `success.data`；有 `variants` 的端點，每一種形狀各比一次 |

契約是律法，而且由觀察式的比對閘門驗證（D-061、D-088）：形狀是封閉的，程式多輸出或少輸出欄位都會讓 `check` 失敗。所以**契約寫的就是後端實際送出的 JSON**，可以當成後端這一邊的事實。

比對規則：

| 類別 | 意思 |
|---|---|
| 前端有、契約沒有 | 前端的型別宣告了後端不會送的欄位，執行時是 `undefined` |
| 契約有、前端沒有宣告 | 後端送了，前端沒有讀 |
| 契約一律 null | 契約把值寫成字面值 `'null'` |
| 契約可為 null、前端必填 | 契約是 `型別?`，前端的型別既不是選填也沒有 `\| null` |
| 型別不同 | 字串、數字、布林、陣列、物件的種類不同 |

工具與輸入在 `docs/review/tools-2026-11-27/`：

```bash
python3 -I docs/review/tools-2026-11-27/fe_contract.py . docs/review/tools-2026-11-27/calls.json
```

`calls.json` 是前端 27 個呼叫與端點、回應型別的對應表，**手寫的**（從各 service 的呼叫逐一抄出）。

## 二、結論

1. **前端的 27 個呼叫都對得到契約裡的端點。** 沒有型別種類不同的欄位。
2. **有 2 個發現會讓頁面在真實的後端上行為錯誤**（第三節 F-1、F-2）。
3. 有 1 個潛在的問題，目前的頁面走不到（F-3）；3 個影響小的差異（F-4～F-6）。
4. 契約的 52 個端點裡，前端沒有呼叫的有 25 個（第五節）。
5. 5 個呼叫前端宣告成 `ApiResponse<unknown>`、不讀回應（開始複審與四個指派命令）；契約的 `data` 都是 null，一致。

彙總（一個欄位在好幾種形狀裡出現時，各算一次）：

| 類別 | 筆數 |
|---|---:|
| 前端有、契約沒有 | 27 |
| 契約有、前端沒有宣告 | 9 |
| 契約一律 null | 8 |
| 契約可為 null、前端必填 | 2 |
| 型別不同 | 0 |

## 三、發現

### F-1　案件詳情的 `isOwner`：前端讀 `isOwner`，後端送的是 `owner`

| | |
|---|---|
| 端點 | `GET /review-cases/{caseId}`，`full` 形狀 |
| 契約 | `owner: boolean`（`specs/review-case/http.yaml`）。沒有 `isOwner` |
| 後端 | `ReviewCaseDetailView.isOwner()`（第 141 行）。Jackson 把 `isXxx()` 的布林 getter 輸出成 `xxx`，所以 JSON 的鍵是 `owner` |
| 前端 | `ReviewCaseDetailDto.isOwner: boolean`（`cases.service.ts` 第 158 行）；`mapCaseDetail` 讀 `item.isOwner`（第 726 行）。沒有任何地方讀 `owner` |
| 結果 | 對真實的後端，`isOwner` 永遠是 `undefined` |

影響：`case-detail-page.component.ts` 第 148 行用 `d.isOwner || this.canonicalActorRole === 'AUDITOR'` 決定頁面模式。案件還沒結束、操作者是這個案件的初審或複審醫師時：

| | 預期 | 實際（依程式推得） |
|---|---|---|
| 頁面模式 | `ROLE_LIMITED`（受限檢視） | `READONLY_TERMINAL`（唯讀（已結案/取消）），並顯示 blocked 的提示 |

`HOSPITAL_MANAGER`（走 `isManagementRole`）與 `AUDITOR` 不受影響。

為什麼測試沒有擋到：前端的單元測試、E2E（`app.e2e.spec.ts`、`wave5-routes.e2e.spec.ts`）的 mock 都用 `isOwner`；後端的契約比對只看後端自己。`docs/06-frontend/frontend-backend-alignment-baseline.md` 第 113 行記載「`actorRole`、`projectionType`、`isOwner` 均由 backend 提供」，欄位的確有提供，但名稱不同。

### F-2　`SYSTEM_ADMIN` 的案件詳情：前端沒有處理 `metadata` 形狀

| | |
|---|---|
| 端點 | `GET /review-cases/{caseId}`，`metadata` 形狀（D-084、D-094 第 9 點） |
| 契約 | 只有七個欄位：`caseId`、`caseStatus`、`projectName`、`hospitalId`、`reviewerId`、`rereviewerId`、`updatedAt` |
| 前端 | 用同一個 `ReviewCaseDetailDto` 與 `mapCaseDetail` 處理。必填的 `status`、`submissions`、`projectionType`、`actorRole`、`projectId`、`batchId`、`medicalRecordType`、`sourceCaseKey` 都不在這個形狀裡 |

影響（依程式推得）：

- `mapCaseDetail` 執行 `item.submissions.map(...)`（第 740 行）。`submissions` 是 `undefined`，會丟出 `TypeError`，案件詳情載入失敗。
- 即使沒有丟錯：`status` 是 `undefined`（後端在這個形狀裡叫 `caseStatus`）；`projectionType` 是 `undefined`，所以 `d.projectionType === 'METADATA_ONLY'` 不成立，`METADATA_CONTROL_CENTER` 模式進不去。`revision-history-page.component.ts` 第 94 行有同樣的判斷。

受影響的是 `SYSTEM_ADMIN` 開案件詳情頁與修正歷程頁。前端為 metadata 模式寫的程式與測試（例如 `resolves the metadata-only control center`）假設的是「完整的骨架加上 `projectionType: 'METADATA_ONLY'`」，那不是後端現在送的形狀。

### F-3　提交明細的 `masked`、`whitelist` 形狀：`answers` 不是陣列（目前走不到）

| | |
|---|---|
| 端點 | `GET /review-submissions/{submissionId}` |
| 契約 | `full`：`answers` 是陣列。`masked`（複審醫師讀初審）：`answers`、`comment`、`attachmentRefsJson`、`totalScore` 是字面值 null。`whitelist`（`SYSTEM_ADMIN`）：沒有 `answers`、`comment`、`attachmentRefsJson`、`totalScore`、`createdAt` |
| 前端 | `SubmissionDetailDto.answers` 是必填的陣列；`mapSubmissionDetail` 執行 `item.answers.map(...)`（第 774 行），沒有判斷 null |

目前只有初審作業頁與複審作業頁呼叫這個端點（各自讀自己要修正的那一筆提交），拿到的是 `full`，所以走不到。之後若有頁面讓複審醫師或 `SYSTEM_ADMIN` 開提交明細，會丟 `TypeError`。

### F-4　案件詳情的 `submissions[]`：前端宣告了 `comment`、`supersededBySubmissionId`，契約沒有

`full` 形狀的 `submissions[]` 只有 `submissionId`、`stage`、`reviewerId`、`revisionNo`、`status`、`totalScore`、`submittedAt`。前端共用 `SubmissionSummaryDto`（修正歷程的端點有這兩個欄位，案件詳情沒有）。

影響小：`mapSubmission` 用 `status === 'SUBMITTED' && !supersededBySubmissionId` 算 `isCurrentEffective`；被取代的提交狀態是 `SUPERSEDED`，所以結果仍然正確。`comment` 在案件詳情是 `undefined`。

### F-5　案件詳情的 `medicalRecordType`、`sourceCaseKey`：契約可為 null，前端必填

契約是 `text?`，前端是 `string`。影響只在顯示。這兩個欄位在什麼情況下是 null，沒有查。

### F-6　後端送了、前端沒有讀的欄位

| 欄位 | 說明 |
|---|---|
| `owner` | 見 F-1 |
| `caseStatus`、`projectName`、`updatedAt`（`metadata` 形狀） | 見 F-2 |
| `latestInitialSubmissionId`、`scoreSheetVersionRef` | 前端沒有用到 |
| `rereviewContext.initialTotalScore`、`rereviewContext.finalScore` | 前端刻意不讀（D-033，`cases.service.ts` 的註解與測試） |
| `rereviewContext.closedRevisionException` | 契約是 `boolean`。D-032 已撤銷 CLOSED 的複審例外修正，前端不讀；**後端仍在輸出這個欄位** |

最後一項是後端的殘留：D-032 撤銷的概念還留在 HTTP 的輸出裡。它的值目前是什麼，沒有查。

### 已知、已裁決的（D-093）

契約一律 null、前端有宣告的：`rereviewContext.initialReviewerName`、`initialReviewContext.editableSubmission.submittedAt`、抽選執行頁的 `previewStats`、提交明細 `full` 形狀的 `maskedFields`。這些 D-088、D-093 已經處理過，前端有備援，不算差異。

## 四、其他看到的

- `cases.service.ts` 第 14～18 行的 `ApiErrorResponse`（`message`、`errorCode`）與後端的錯誤外殼（`error.code`、`error.message`）不符，但這個型別**沒有被使用**；實際讀錯誤的四處都用 `error.error?.error?.code` 或 `.message`，與 `specs/http-common.yaml` 一致。它是可以刪掉的殘留。

## 五、前端沒有呼叫的端點（25 個）

與頁面進度一致，列出來供對照，不是缺陷。

| 範圍 | 端點 |
|---|---|
| 專案（9） | `/review-projects` 的全部九個端點 |
| 評分表（7） | `/score-sheets` 的全部七個端點 |
| 案件的命令（5） | `cancel`、`initial-review/return`、`reopen`、`close-without-rereview`、`send-to-rereview` |
| 案件的查詢（1） | `GET /review-cases/{caseId}/lifecycle` |
| 稽核（1） | `GET /audit-trails` |
| 抽選（2） | `GET /sampling-batches/{batchId}`、`GET /sampling-batches/{batchId}/export` |

## 六、選項（待負責人決定）

### F-1（`isOwner` 與 `owner`）

| 選項 | 作法 | 要不要修法 |
|---|---|---|
| **A（建議）** | 改前端：讀 `owner`，view model 內部仍叫 `isOwner`。mock 與 E2E 的假資料一起改 | 不用。HTTP 契約（D-088）已經寫明是 `owner`，這是前端不符合契約 |
| B | 改後端：輸出 `isOwner` | 要：`http.yaml` 是律法，改形狀要簽章，並用 `transition` 過渡 |

### F-2（`metadata` 形狀）

| 選項 | 作法 | 要不要修法 |
|---|---|---|
| **A（建議）** | 改前端：依回應有沒有 `caseStatus`、沒有 `status` 判斷是 `metadata` 形狀，另外對應成 metadata 的 view model；`METADATA_CONTROL_CENTER` 不再依賴 `projectionType` | 不用。但 UI-010、UI-014 的頁面規格描述的 metadata 模式要一起核對 |
| B | 改後端：`metadata` 形狀加上 `projectionType` 等欄位 | 要，而且與 D-084、D-094 第 9 點（七個欄位的白名單）衝突 |

F-2 的 A 不是一行的修改：metadata 模式的頁面目前顯示哪些區塊，要對照七個欄位重新看一次。建議先寫頁面規格的差異，再改程式。

### F-3～F-6

- F-3：`mapSubmissionDetail` 加上 null 的判斷，或等有頁面用到時再處理。
- F-4、F-5：前端的型別照契約修正。
- F-6 的 `closedRevisionException`：要不要從後端的輸出移除，是修法（`http.yaml`）加建構。

### 讓這種漂移之後會被擋下

這次是人工比對。要變成閘門，可以考慮（都還沒有設計）：

1. 前端的 E2E 假資料改由契約產生，或由契約驗證（mock 的回應不符合 `http.yaml` 就失敗）。
2. 前端的 dto 型別由 `http.yaml` 產生。
3. 把本文件的比對腳本放進 `frontend-check`。它的對應表是手寫的，要先解決「前端新增呼叫時對應表沒有跟著更新」。

這對應題目清單裡還沒有的一題：前端納入問題側（D-060 第 2 點）。

## 七、限制

1. 沒有對真實的後端執行前端。F-1、F-2 的「影響」是依程式推得的。
2. `calls.json` 是手寫的 27 筆。前端若有不經過 service 的 HTTP 呼叫，不在裡面；用 `HttpClient` 找過，只有這四個 service。
3. 型別的解析是用正規表示式做的，只處理 `interface`、型別別名、行內的物件型別；泛型只認 `PageResponse<T>` 與陣列。
4. 只比對回應。請求端的差異（例如前端送了後端不讀的欄位）沒有比。
5. 列舉只比到「是字串」，沒有比對有哪些值。
