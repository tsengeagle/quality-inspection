# 稽核 `detail` 繞過 D-055 第 5 點的讀取者紀錄

狀態：**已裁決**（2026-11-22）。負責人對 A-1～A-3 全部照建議裁決，條文是 `decision-log.md` 的 D-071，以那裡為準。本文件不是律法，下面保留起草時的版本（main = `956bfe3`）。

**起草後補查**：帶分數的 action 還有 `REREVIEW_REVISED`（`rereviewScore`、`finalScore`）與 `CASE_CLOSED_WITHOUT_REREVIEW`（`finalScore`），D-071 第 1 點一併列入。題號 A-1～A-3 只在本文件使用。

## 一、背景

- D-055 第 5 點規定，准許讀取完整初審內容時，必須留下初審讀取者紀錄。完整初審內容指案件詳情、初審提交明細，以及 revision 歷程中未遮罩的初審內容。之後若指派或改派複審醫師的對象讀過初審，回 `422 REREVIEWER_HAS_READ_INITIAL`。
- D-055 第 6 點規定，其他出口要擇一套用：記錄讀取者，或遮罩。後來新增的出口也一樣。逐案匯出採用遮罩。
- 稽核的兩個出口都沒有套用其中任何一種，這件事是 D-070 草稿（`20-*.md` 第七節）發現的。
- `law/coverage.txt`：`application/audit/` 是 `grandfathered`（稽核查詢，未立法）。本題裁決後，修法 PR 要把它改成 `legislated`，或把範圍限縮到這兩個出口。

## 二、查到的事實

**帶分數的 `detail`。** 有三種 action 的 `detail` 帶分數：

| 事件 | `detail` 中的分數 | 位置 |
|---|---|---|
| `InitialReviewCompleted` | `totalScore`、`qualityThreshold` | `InitialReviewCompletedEvent.java:26` |
| `ReviewCaseEscalatedToRereview`（修正初審路徑） | `score`、`threshold` | `ReviewCaseEscalatedToRereviewEvent.java:37` |
| `RereviewCompleted` | `rereviewScore`、`finalScore`（可反推初審分數） | `RereviewCompletedEvent.java:28` |

手動送複審（`{"trigger":"manual"}`）不帶分數。

**兩個出口現在的行為：**

| 出口 | 可讀角色 | 哪些角色拿不到 `detail` | 讀取者紀錄 |
|---|---|---|---|
| `GET /review-cases/{caseId}/lifecycle`（`GetCaseLifecycleUseCase`） | `HOSPITAL_MANAGER`、`REVIEWER`、`REREVIEWER`、`AUDITOR`、`SYSTEM_ADMIN` | 持有 `REREVIEWER` 或 `SYSTEM_ADMIN` | 不寫 |
| `GET /audit-trails`（`ListAuditTrailsUseCase`） | `SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`AUDITOR` | 只有持有 `REREVIEWER` | 不寫 |

- 兩個出口都在 `@Transactional(readOnly = true)` 中執行，`AuditTrailEntry` 也沒有 `maskedFields`。
- **繞過的情境。** 一個人讀的時候沒有 `REREVIEWER` 角色（例如是 `HOSPITAL_MANAGER`），從生命週期查詢讀到初審分數，之後被授予 `REREVIEWER` 並指派為複審醫師。他沒有初審讀取者紀錄，所以指派會成功，違反 D-033。
- **另一個落差。** `/audit-trails` 回傳 `detail` 給 `SYSTEM_ADMIN`。這與 D-033、D-055 第 6 點，以及 api-spec 15.2 的「只看 metadata」不一致。生命週期查詢則已經不給。

## 三、A-1：兩個出口要記錄讀取者，還是遮罩

### 選項

- **A：記錄讀取者**，比照案件詳情。只要回應中某一列帶分數的 `detail`，就對該案寫入讀取者紀錄，並做歷史身分判斷和版本檢查。
  - 生命週期查詢一次只查一案，做得到。
  - `/audit-trails` 一頁可能有幾十案，每一案都要寫入紀錄並遞增版本，與同時進行的指派衝突的機會大。查詢稽核也會變成會拒絕的寫入操作。
  - 兩個出口都必須改成可寫入的交易。
- **B：遮罩**，比照 D-055 第 6 點的匯出。符合下列任一條件時，帶分數的那一列 `detail` 輸出 `null`，其他欄位（action、操作者、時間）照常輸出：
  - 案件狀態還不是 `REREVIEW_IN_PROGRESS`、`CLOSED`、`CANCELLED`；
  - 讀者是目前的複審醫師，或在複審指派歷史中；
  - 複審指派歷史不完整。

  三個條件都不成立時照常輸出。不寫入紀錄，也不遞增版本。在指派之前要看分數，從案件詳情讀（會留下紀錄）。
- **C：混合**。生命週期查詢採 A，`/audit-trails` 採 B。

### 建議：B

- D-055 第 6 點已經為「多案、唯讀的出口」選了遮罩。`/audit-trails` 和匯出性質相同，生命週期查詢也只是稽核資料的另一個入口。
- 用同一套條件，兩個出口和匯出的規則一致，`ReviewReadAccessPolicy` 的寫入路徑不會擴大。
- 代價：在指派之前（`PENDING_INITIAL_REVIEW`～`PENDING_REREVIEW`），管理者在稽核畫面看不到分數，要改從案件詳情讀。
- 持有 `REREVIEWER` 就整筆不給 `detail` 的現行規則保留，B 是加在它之上的規則。

### 待負責人決定

A／B／C？

## 四、A-2：遮罩的粒度

- **A：整個 `detail` 輸出 `null`**，沿用現有的 `withoutDetail()`。只限帶分數的三種 action，其他 action 的 `detail` 照常輸出。
- **B：只拿掉分數鍵**（`totalScore`、`qualityThreshold`、`score`、`threshold`、`rereviewScore`、`finalScore`）。這要解析並重組 JSON。

建議 **A**。三種 action 的 `detail` 除了分數之外幾乎沒有別的內容（`InitialReviewCompleted` 另有的欄位都可以從案件取得）。`threshold` 雖然不是分數，但和 action 一起看，可以推出初審分數落在哪個區間。

要不要加 `maskedFields`（比照匯出）？建議**不加**，`detail` 為 `null` 本身就表示被遮罩。這需要負責人確認。

## 五、A-3：`/audit-trails` 回 `detail` 給 `SYSTEM_ADMIN`

建議**改成不給**，比照生命週期查詢與 D-033。這只修正程式，不需要新條文，只在 api-spec 14.2 補一句權限說明。

### 待負責人決定

同意？

## 六、不在本題

- D-069 留下的兩件：複審不發 `ReviewDraftSaved`、`ReviewSubmissionSubmitted`；提交複審沒有必答題檢查。
- 稽核的其他 `entityType`（專案、評分表）不帶初審分數，不受影響。
- 之後若新增會帶分數的事件，必須加進帶分數 action 的清單。修法 PR 要加一條規則：凡 `detail` 帶分數的 action，都必須在清單中，並以測試檢查。

## 七、裁決後的順序

1. **修法 PR**：
   - `decision-log.md` 新增 D-071；
   - api-spec 14.1、14.2 補上遮罩規則；
   - 在 `specs/` 加稽核的規則與範例。目前沒有 `specs/audit/`，要新建，或放在 `review-case`。紅的範例列進已知失敗清單；
   - `law/coverage.txt` 中 `application/audit/` 改成 `legislated`。
2. **建構 PR**：兩個 use case 套用遮罩條件（讀取案件狀態與複審指派歷史），`SYSTEM_ADMIN` 在 `/audit-trails` 不給 `detail`，最後清空已知失敗清單。

### 還沒有查證的

- `/audit-trails` 的一頁可能跨多案，要逐案判斷狀態與歷史。要確認是否需要批次查詢 port，避免 N+1。
- `ReviewCaseEscalatedToRereview` 發生時，案件狀態已經是 `PENDING_REREVIEW`。B 依案件**目前**的狀態判斷，而不是事件發生時的狀態，這一點要寫進條文。
- `specs/` 的範例能不能直接呼叫生命週期與稽核端點。這要在修法 PR 裡實際跑過才確定。
