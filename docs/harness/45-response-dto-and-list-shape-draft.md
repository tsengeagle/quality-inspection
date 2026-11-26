# 回應 dto 與列表的形狀

狀態：**草稿，待裁決**（2026-11-26 起草，main = `1244182`）。本文件不是律法；題號 P-1～P-8 只在本文件使用。

題目清單待辦第 1 題：D-093 第 6 點留下的兩件。

- **第一件**：沒有回應 dto 的端點改成 dto（D-088 第 8 點，草稿 39 的 H-3 B）。
- **第二件**：統一 `Page` 與 `{ items }`（D-088 第 9 點，草稿 39 的 H-11 B）。

## 一、範圍

後端 4 份 HTTP 契約共 52 個端點（review-case 29、review-project 9、sampling-batch 7、score-sheet 7）。本題只談成功回應的 `data` 怎麼產生、列表的外殼長什麼樣子。

不在範圍內：

- 請求端（請求 dto、查詢參數），D-088 第 11、13 點與 D-089 已處理。
- 錯誤回應的外殼，`specs/http-common.yaml` 已立法。
- 欄位的增刪。本題的兩件都不新增業務欄位。

## 二、查到的事實

**全部是讀程式、契約與文件，沒有實測。** HTTP 的實際回應由 `http.yaml` 的觀察式比對閘門驗證（D-061、D-088）。

### 成功回應的 `data` 怎麼產生

1. 52 個端點依 `data` 的來源分成四類：

   | 類別 | 端點數 | 說明 |
   |---|---|---|
   | `data` 是 null | 16 | 指派、取消、退回等命令；review-project 5 個、score-sheet 1 個、review-case 10 個 |
   | 回應 dto | 16 | `interfaces/rest/dto/` 底下的 `*Response`、`SubmissionSummaryDto` |
   | application 物件直接輸出 | 13 | 見事實 2 |
   | controller 裡組的 `Map` | 7 | 見事實 3 |

2. 直接輸出 application 物件的 13 個端點：

   | 端點 | 輸出的類別 | 所在套件 |
   |---|---|---|
   | `GET /review-cases` | `Page<ReviewCaseSummary>` | `application/common`、`application/reviewcase/query` |
   | `GET /review-cases/{caseId}` | `ReviewCaseDetailView`、`CaseDetailMetadataView`（內含 `RereviewBootstrapView` 等） | `application/reviewcase/query` |
   | `GET /review-cases/{caseId}/assignment-context` | `AssignmentBootstrapView` | 同上 |
   | `GET /review-cases/{caseId}/assignment-candidates` | `List<AssignmentCandidateView>` | 同上 |
   | `GET /review-cases/{caseId}/lifecycle` | `List<AuditTrailEntry>` | `application/audit` |
   | `GET /review-submissions/{submissionId}` | `SubmissionDetailView`、`WhitelistedSubmissionDetailView` | `application/reviewcase/query` |
   | `GET /audit-trails` | `Page<AuditTrailEntry>` | `application/audit` |
   | `GET /reports/dashboard-summary` | `DashboardSummary` | `reporting` |
   | `POST /reports/export` | `ExportResult`、`ExportRow` | `reporting` |
   | `POST /review-projects/{projectId}/sampling/preview` | `SamplingPreviewResult` | `application/sampling` |
   | `GET /sampling-batches` | `Page<SamplingBatchSummary>` | `application/sampling` |
   | `GET /sampling-batches/{batchId}` | `SamplingBatchDetailView` | 同上 |
   | `GET /sampling-batches/{batchId}/export` | `SamplingBatchExportResult` | 同上 |

   涉及的頂層類別 16 個，欄位合計約 160 個（最大的是 `ReviewCaseDetailView` 27 個、`RereviewBootstrapView` 19 個、`AssignmentBootstrapView` 17 個）。巢狀的子類別沒有逐一數。

3. 用 `Map` 組回應的 7 個端點：

   | 端點 | `Map` 的鍵 |
   |---|---|
   | `POST /review-projects` | `projectId`（`Map<String, String>`） |
   | `GET /review-projects/{projectId}/version` | `versionNumber`、`published` |
   | `POST /score-sheets` | `scoreSheetId` |
   | `POST /score-sheets/{id}/publish` | `scoreSheetVersionId` |
   | `GET /score-sheets/{id}/version` | `version`、`published`、`scoreSheetVersionId` |
   | `POST /review-projects/{projectId}/sampling/execute` | `batchId` |
   | `POST /sampling-batches/{batchId}/retry` | `batchId` |

   `ScoreSheetController` 的 7 個方法回傳型別都是 `ResponseEntity<?>`，從方法簽章看不出回應的型別。

4. application 層有 3 個讀取模型帶著 Jackson 的標註，也就是 application 為了 HTTP 的輸出形狀依賴序列化函式庫：
   - `ReviewCaseDetailView`：兩處 `@JsonIgnore`。
   - `CaseDetailMetadataView`：`@JsonValue`。
   - `WhitelistedSubmissionDetailView`：`@JsonIgnoreProperties`。

5. 這 20 個端點（事實 2、3）的形狀都已寫進 `http.yaml`。契約是封閉的對應表，application 類別改了欄位名稱、多了或少了欄位，比對閘門會擋下。這是 D-088 第 8 點選「程式不動」的理由，現在仍然成立。

6. `law/coverage.txt` 逐一列出 `interfaces/rest/` 底下的檔案，不在清單內的新檔案會被 `law-check` 擋下（D-089 第 6 點）。所以新增任何回應 dto 類別，都要先用修法把檔案登記進涵蓋清單，即使 HTTP 的行為完全不變。

### 列表的形狀

7. 回傳多筆資料的端點有四種寫法：

   | 寫法 | `data` 的形狀 | 端點 | 分頁 |
   |---|---|---|---|
   | `Page` | `{ content, page, size, totalElements, totalPages }` | `GET /review-cases`、`GET /sampling-batches`、`GET /audit-trails` | 有（`page`、`size` 查詢參數） |
   | `{ items }` | `{ items }` | `GET /review-projects`、`GET /score-sheets` | 沒有，一次回全部 |
   | 匯出 | `{ reportType, rows, summary, page, size, totalElements, totalPages }` | `POST /reports/export` | 有 |
   | 裸陣列 | `[ ... ]` | `GET /review-cases/{caseId}/assignment-candidates`、`GET /review-cases/{caseId}/lifecycle` | 沒有 |

   另外有把清單放在具名欄位裡的：修正歷史 `{ caseId, submissions }`、評分表章節 `{ scoreSheetId, sections }`、批次匯出 `{ detail: { …, selectedCases } }`。這些是「一個物件帶著它的子項目」，不是列表端點。

8. `Page` 與 `{ items }` 不只是欄位名稱不同：前者有分頁，後者沒有。`GET /review-projects`、`GET /score-sheets` 只收 `hospitalId`，不收 `page`、`size`。

9. `{ items }` 是已裁決的形狀：評分表列表是 D-050 第 7 點，專案列表是 D-062 第 1 點（「與評分表列表一致」）。`Page` 的形狀沒有專屬的裁決，是 D-088 第 2 點「照程式」立法的。

10. `api-spec.md` 的寫法：
    - §7.7（專案清單）、§8.5（評分表列表）的範例是 `"items"`，與程式一致。
    - §9.5（批次清單）、§10.1（案件清單）列出 `page`、`size` 查詢參數，但沒有寫出回應的分頁外殼。
    - §15 的匯出寫明 `rows`、`totalElements`、`totalPages`。

11. `Page` 的形狀在 `http.yaml` 寫了三次（`specs/review-case/http.yaml` 兩次、`specs/sampling-batch/http.yaml` 一次），因為共用層沒有共用形狀的寫法（D-088 第 9 點）。

12. 前端：
    - `cases.service.ts`、`sampling.service.ts` 各自宣告一個 `content`／`totalElements` 的分頁型別，讀進來之後轉成自己的 view model，欄位叫 `items`。也就是前端內部已經統一用 `items`，只有讀 HTTP 的那一層認得 `content`。
    - `report-export.service.ts` 讀匯出的 `rows` 與分頁欄位。
    - 稽核軌跡（`GET /audit-trails`）在前端的使用情形沒有查。

13. 正式環境還沒上線（D-045），沒有外部的呼叫端；HTTP 的呼叫端只有本 repo 的前端與測試。

## 三、待裁決

### 第一件：回應 dto

#### P-1　直接輸出 application 物件的 13 個端點

| 選項 | 作法 | 影響 |
|---|---|---|
| **A（建議）** | 維持現況，不改成 dto。本題結案時在題目清單寫明「由封閉的契約保護，不另做」 | 無 |
| B | 13 個端點全部改成回應 dto | 新增約 16 個以上的 dto 類別與對應程式、每個類別都要登記進 `coverage.txt`（事實 6）；HTTP 的行為不變 |
| C | 只改帶 Jackson 標註的 3 個讀取模型對應的 2 個端點（案件詳情、提交明細），把標註移到 dto | 新增 3～5 個 dto 類別；application 不再依賴 Jackson（見 P-2） |

A 的理由：

- B 的效益是「application 與 HTTP 分開」。這個風險（改 application 的欄位名稱會悄悄改掉 HTTP）已經被封閉的契約擋住（事實 5），dto 再擋一次是重複的。
- B 的成本不小：約 160 個欄位的逐欄對應是純機械的程式，本身就可能抄錯；抄錯時是比對閘門擋下，不是 dto 擋下。
- 沒有外部呼叫端（事實 13），目前沒有「application 要演進、HTTP 要凍結」的實際需求。有這個需求時再做，屆時只做受影響的端點。

#### P-2　application 讀取模型上的 Jackson 標註（事實 4）

| 選項 | 作法 |
|---|---|
| **A（建議）** | 維持現況，但加一條 ArchUnit 規則凍結基線：`application`、`reporting` 不得新增對 `com.fasterxml.jackson` 的依賴，現有 3 個檔案列入凍結基線 |
| B | 不處理 |
| C | 把這 3 個讀取模型改成 dto（同 P-1 的 C），application 完全不依賴 Jackson |

A 的理由：這 3 處是 D-084、D-088 立法時為了做出契約的形狀而加的，行為已由範例與契約涵蓋。問題不在現有的 3 處，在於之後會不會越加越多。凍結基線只擋新增，成本是一條規則與一組違規樣本。

A 需要修法：`architecture/` 的測試與 `archunit_store/` 的新增都是律法路徑。

#### P-3　用 `Map` 組回應的 7 個端點（事實 3）

| 選項 | 作法 |
|---|---|
| **A（建議）** | 維持現況 |
| B | 改成具名的 dto（例如 `IdResponse`、`VersionResponse`），約 4～5 個類別 |

A 的理由同 P-1：形狀已由契約封閉。`Map` 的鍵打錯字，比對閘門會擋下。

#### P-4　`ScoreSheetController` 的 `ResponseEntity<?>`（事實 3）

| 選項 | 作法 |
|---|---|
| **A（建議）** | 把 7 個方法的回傳型別改成具體的 `ResponseEntity<ApiResponse<…>>`，與其他 controller 一致。不新增類別，HTTP 的行為不變 |
| B | 維持現況 |

A 的理由：這是唯一一個從簽章看不出回應型別的 controller，改動只在一個已立法的檔案內，不需要改 `coverage.txt`。P-3 選 A 時，其中 3 個方法的型別是 `ApiResponse<Map<String, …>>`。

### 第二件：列表的形狀

#### P-5　`Page` 與 `{ items }` 要不要統一

| 選項 | 作法 | 要改的地方 |
|---|---|---|
| **A（建議）** | 不統一，並把規則寫明：**有分頁的列表用 `Page`，沒有分頁的列表用 `{ items }`** | 只有文件與裁決 |
| B | `Page` 的 `content` 改名為 `items`，分頁列表變成 `{ items, page, size, totalElements, totalPages }` | `Page` 類別、3 份 `http.yaml` 的 3 處、前端 2 個 service、測試；用 `transition`（D-094 第 10 點）過渡 |
| C | 專案列表、評分表列表也改成分頁 | 2 個 use case 的輸入與規則、2 份 `http.yaml`、D-050 第 7 點與 D-062 第 1 點、前端 2 個頁面 |

A 的理由：

- 兩種寫法對應兩種不同的東西（事實 8）。把它寫成規則之後，「並存」就不是不一致，而是有意義的區分。
- B 的效益是呼叫端少記一個欄位名稱。前端已經在讀取層把 `content` 轉成 `items`（事實 12），實際上感受不到差異。
- C 是新的行為（分頁、排序、預設的每頁筆數都要裁決），目前沒有資料量的需求。

若選 B：`content` 是 Spring Data 的慣用名稱，`items` 是本專案已裁決的名稱（事實 9）。統一成 `items` 與既有裁決一致。

#### P-6　匯出與裸陣列（事實 7）

| 選項 | 作法 |
|---|---|
| **A（建議）** | 維持現況。匯出是報表的回應，`rows` 之外還帶 `reportType`、`summary`，不是一般的列表；裸陣列的兩個端點（候選人、生命週期）維持陣列 |
| B | 裸陣列改成 `{ items }` |

A 的理由：B 會動到兩個端點的契約與前端，效益是之後要加欄位（例如總筆數）時不用改形狀。目前沒有這個需求。P-5 的規則只規範「列表端點」，A 之下這兩個端點列為已知的例外並寫進規則。

#### P-7　`Page` 的形狀在契約裡寫三次（事實 11）

| 選項 | 作法 |
|---|---|
| **A（建議）** | 維持各寫一次 |
| B | 擴充格式，讓 `http-common.yaml` 可以定義共用的形狀（例如 `$page`），各 `http.yaml` 引用 |

A 的理由：B 要改 `specs/README.md` 第七節、比對器（`acceptance/http/`）與它的測試，是格式的擴充。三處重複的維護成本很低，而且比對閘門會擋住任何一處與程式不一致。P-5 選 B 時要改這三處，但那是一次性的。

### 分批

#### P-8　怎麼分批

全部照建議時，程式的改動只有 P-2（一條 ArchUnit 規則）與 P-4（一個 controller 的回傳型別）：

1. **修法 PR**：本裁決；P-5 的規則寫進 `specs/README.md` 第七節或 `api-spec.md` 的共通章節（哪一個由裁決決定，見下）；P-2 的 ArchUnit 規則、違規樣本與凍結基線；`api-spec.md` §9.5、§10.1 補上分頁外殼的說明（事實 10）；題目清單與本草稿。
2. **建構 PR**：P-4 的回傳型別。不改律法，不需要簽章。

P-5 的規則放哪裡：

- 放 `api-spec.md`：它是 HTTP 的說明文件，讀的人會在那裡找。不是律法，約束力靠 `http.yaml`。
- 放 `specs/README.md` 第七節：是律法，但那一節講的是契約的格式，不是 HTTP 的設計規則。
- 建議放 `api-spec.md` 的共通章節，裁決條文在 `decision-log.md`。

任何一題選了要改形狀的選項（P-1 B／C、P-3 B、P-5 B／C、P-6 B）時，照「修法 PR（契約、`coverage.txt`、範例凍結）→ 建構 PR」；改形狀的用 `transition` 過渡。

## 四、建議總表

| 題 | 建議 | 改程式 | 改律法 |
|---|---|---|---|
| P-1 | A：13 個端點維持直接輸出 | 否 | 只有裁決 |
| P-2 | A：ArchUnit 凍結基線，不得新增 Jackson 依賴 | 否 | `architecture/`、`archunit_store/` |
| P-3 | A：`Map` 維持現況 | 否 | 只有裁決 |
| P-4 | A：`ScoreSheetController` 的回傳型別寫明 | 是（1 個檔案） | 否 |
| P-5 | A：不統一，寫明「有分頁用 `Page`，沒有分頁用 `{ items }`」 | 否 | 只有裁決 |
| P-6 | A：匯出與裸陣列維持現況，列為已知的例外 | 否 | 只有裁決 |
| P-7 | A：`Page` 的形狀維持各寫一次 | 否 | 否 |
| P-8 | 一個修法 PR、一個建構 PR | | |

全部照建議時，本題的結果是：兩件都「不做大改」，把現況寫成規則，加一道防止惡化的閘門。

## 五、起草時沒有確認的

1. 事實 1 的分類是讀 controller 的方法簽章與 `http.yaml` 得到的，沒有逐一執行。
2. 事實 2 的欄位數是用 `private` 欄位的行數估的，巢狀類別沒有逐一數。P-1 選 B 時要重新盤點。
3. 稽核軌跡在前端的使用情形沒有查（事實 12）。P-5 選 B 時要查。
4. P-2 的 ArchUnit 規則沒有試寫。`domain` 底下另有 5 個事件類別依賴 Jackson（`ReviewCaseReturnedEvent` 等，用的是 `ObjectMapper`，不是輸出形狀的標註；只看了其中一個檔案的 import）。那是另一種依賴，本草稿的 P-2 只談 `application`、`reporting`；`domain` 要不要一起凍結，建議另外裁決。
