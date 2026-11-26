# HTTP 回應與 `api-spec.md` 的落差

狀態：**已裁決**（2026-11-26）。負責人裁決：G-1～G-7 全部照建議；條文是 `decision-log.md` 的 D-093，以那裡為準。下面保留起草時的版本（main = `8e2a1c9`）。本文件不是律法；題號 G-1～G-7 只在本文件使用。

題目清單待辦第 1 題（D-088 第 2 點）。D-088 把 HTTP 契約（`specs/*/http.yaml`）照程式立法，和 `api-spec.md` 不同的地方記在題目清單，留到本題處理。

## 一、範圍

題目清單列的五件：

| # | 端點 | `api-spec.md` | 程式與 `http.yaml` |
|---|---|---|---|
| 1 | `POST /review-projects/{projectId}/sampling/execute` 的回應（§9.3） | `batchId`、`status`、`selectedCount` | 201，只有 `batchId` |
| 2 | `POST /sampling-batches/{batchId}/retry` 的回應（§9.4） | `newBatchId`、`retryOfBatchId` | 201，只有 `batchId` |
| 3 | 預覽、執行的請求本文（§9.2、§9.3） | `{ "mode": "MANUAL" }` | 不讀請求本文 |
| 4 | 案件詳情的 `rereviewContext.initialReviewerName` | 沒有寫 | 一律 null |
| 5 | 抽選執行頁的 `previewReady`、`previewStats`（§9.1） | 範例是 `false`、`null`，沒有說明 | 一律 `false`、null |

不在範圍內（題目清單寫明之後再做）：沒有回應 dto 的端點改成 dto（D-088 H-3 B）、統一 `Page` 與 `{ items }`（D-088 第 9 點）。見 G-6。

## 二、查到的事實

全部是讀程式與文件，**沒有實測**。HTTP 的實際回應已由 D-088 的 `http.yaml` 範例實測過（觀察式比對閘門）。

1. **程式**：`SamplingController` 的執行與重跑都回 `Map.of("batchId", …)`，狀態 201（第 57～73 行）；兩支方法沒有 `@RequestBody`，不讀 `mode`。`ExecuteSamplingUseCase` 的執行模式寫死為 `"MANUAL"`（發出的 `SamplingBatchStarted` 事件）。
2. **前端與程式一致**：`sampling.service.ts` 的 `previewSampling`、`executeSampling`、`retrySamplingBatch` 都送 `{}`，回應型別都只有 `batchId`（執行、重跑）；執行與重跑成功後重新載入，不讀其他欄位。所以落差只在 `api-spec.md`。
3. **執行的 `status`、`selectedCount`**：抽選中途出錯時不留下批次（D-086 第 1 點），所以成功的回應一定是 `COMPLETED`。抽中件數可以從批次詳情（§9.6）讀到。
4. **重跑**：目前沒有任何行為會產生 `FAILED` 的批次，成功的重跑走不到（§9.4、D-086 第 1、2 點），等母體來源的介接（待辦第 4 題）。
5. **`initialReviewerName`**：`GetReviewCaseDetailUseCase` 建 `RereviewBootstrapView` 時傳 null。D-084 第 3 點：複審醫師看得到初審醫師是誰（`reviewerId`、`rereviewContext.initialReviewerId`）。前端的複審頁顯示 `initialReviewerName || initialReviewerId || '未提供'`，所以目前顯示 id。系統有人員目錄（`his_staff_directory`，`HisStaffDirectoryAssignmentCandidateAdapter`，分派頁的候選人用它），但沒有「依 id 取名稱」的規則。
6. **`previewReady`、`previewStats`**：`GetSamplingExecuteContextUseCase` 傳入常數 `false`、`null`。UI-006 頁面規格（`docs/05-ui/ui-page-spec-ui006抽選執行頁.md` 第 97 行）寫明「`previewStats` 仍屬 page-local session state」，preview 之後的狀態歸屬「仍待後續收斂」。前端用 `context.previewReady` 決定首屏的模式，預覽的數字存在頁面自己的狀態。
7. **律法**：`http.yaml` 是律法（D-061、D-088）；`api-spec.md` 不是律法，在權威順序的第 5 層（`docs/03-contracts/*`）。

## 三、待裁決

### G-1　執行抽選的回應

- **A（建議）**：`api-spec.md` §9.3 照程式改成 201、`{ batchId }`，寫明成功一定是 `COMPLETED`、件數從批次詳情讀。程式、前端、`http.yaml` 不動。
- B：程式補上 `status`、`selectedCount`（改 `http.yaml`、程式；前端可以不改）。

### G-2　重跑的回應

- **A（建議）**：`api-spec.md` §9.4 照程式改成 201、`{ batchId }`（新批次的 id）。`retryOfBatchId` 可以從批次詳情、清單讀到。成功的重跑目前走不到，等母體來源的題目再檢視。
- B：程式改成 `newBatchId`、`retryOfBatchId`（改 `http.yaml`、程式、前端）。

### G-3　預覽、執行的請求本文 `mode`

- **A（建議）**：`api-spec.md` §9.2、§9.3 刪除請求本文，寫明「不讀請求本文；執行模式一律是 `MANUAL`」。排程抽選若要做，另立題目。
- B：程式讀 `mode`，只接受 `MANUAL`，其他值回 `400`（新增規則與範例）。

### G-4　`rereviewContext.initialReviewerName`

| 選項 | 作法 |
|---|---|
| **A（建議）** | 維持一律 null，`api-spec.md` 寫明「目前一律 null，請用 `initialReviewerId`」。不動程式與前端 |
| B | 從人員目錄取名稱。要先裁決：目錄裡找不到時怎麼辦、要不要依院區找。新增規則、範例、程式 |
| C | 從回應移除這個欄位。改 `http.yaml`、程式、前端的型別 |

A 的理由：前端已經有備援，B 是新的行為，值得的話另立題目；C 動到三個地方，效益只是少一個 null。

### G-5　抽選執行頁的 `previewReady`、`previewStats`

- **A（建議）**：維持現況，`api-spec.md` §9.1 寫明「預覽的結果是頁面自己的狀態（UI-006 第 97 行），本端點的 `previewReady` 一律 `false`、`previewStats` 一律 null」。preview 狀態要不要由後端保存，等 UI-006 的狀態歸屬定案。
- B：從回應移除兩個欄位（改 `http.yaml`、程式、前端；前端的首屏判斷要改）。

### G-6　範圍

- **A（建議）**：本題只處理上面五件。沒有回應 dto 的端點與 `Page`／`{ items }` 的統一，留在題目清單、另立題目。
- B：一起處理。

### G-7　分批

全部選 A 時，只改 `api-spec.md`、決策紀錄、題目清單與本草稿。決策紀錄是律法，所以是**一個修法 PR**，沒有建構 PR，也沒有新的範例（`http.yaml` 已經照程式）。

任何一題選 B 時，照「修法 PR（規格、範例凍結）→ 建構 PR」。

## 四、建議總表

| 題 | 建議 |
|---|---|
| G-1 | A：`api-spec.md` 照程式 |
| G-2 | A：`api-spec.md` 照程式 |
| G-3 | A：刪除請求本文，寫明一律 `MANUAL` |
| G-4 | A：維持 null，寫明用 `initialReviewerId` |
| G-5 | A：維持現況，寫明是頁面自己的狀態 |
| G-6 | A：只處理五件 |
| G-7 | 一個修法 PR |
