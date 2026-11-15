# 第 4 期：驗收範例的執行器

2026-11-15 依負責人裁決動工。本文件記錄設計決定、驗證方式與第一次執行的結果。執行的語意以 `specs/README.md` 第四節「執行」為準。

## 一、決定（2026-11-15，負責人照建議裁決）

| # | 項目 | 決定 | 理由 |
|---|---|---|---|
| 1 | 範例怎麼接到程式 | **命名慣例加反射**：規格的 `X` 對應 `XUseCase.execute`，輸入依參數名稱、輸出依屬性名稱 | 沒有翻譯表。名稱對不上時範例變紅，修正方向是讓程式使用規格的詞彙。不採用逐支手寫轉接層（那就是翻譯表，而且由 AI 維護）；不走 HTTP（範例只透過 use case，且 HTTP 還要一張對應表） |
| 2 | 執行器放哪 | **Java 測試端**（JUnit 5 動態測試） | 最簡單，直接用 Spring context。語意寫在 `specs/README.md`，別的語言可照著重做；`harness-core/` 拆成獨立 repo 時再評估把直譯邏輯搬過去 |
| 3 | 一開始的紅燈 | **沿用 `known-test-failures.txt`**，一個範例一行 | 不另開機制。清單上的可以紅；變綠就刪行（免修法）；新範例一開始就紅，加進清單要修法簽章。這就是及格線第 4 項「改需求 → 驗收紅燈，直到程式補上」 |
| 4 | 範圍 | 分三個 PR：執行器（本次）→ 建構 PR 逐一讓範例變綠 → 評分表規格升格後解開 `needs` | 執行器是律法，要簽章；改程式是建構工作，分開審 |

## 二、組成

| 檔案 | 角色 |
|---|---|
| `ExampleRunner.java` | 直譯 `examples.yaml`：fixture 展開、`${}` 變數、`needs`／`input_pending`、`then`、`observe`、部分比對。只懂規格，不認識任何 use case |
| `UseCaseInvoker.java`、`Outcome.java`、`RunnerProblem.java` | 執行器與程式之間唯一的介面 |
| `SpringUseCaseInvoker.java` | 命名慣例的實作：找 bean、依參數名稱轉換輸入、取 `getErrorCode()`、用 Jackson 讀輸出屬性 |
| `AcceptanceExamplesTest.java` | 掃描 `specs/*/examples.yaml`，每個範例一個動態測試，各自在交易裡執行後還原；找不到任何範例就失敗 |
| `ExampleRunnerTest.java` | 執行器自己的測試，用假的 use case，不依賴應用程式 |

建置設定的兩處修改（`build.gradle`）：

- `-parameters`：位元組碼保留參數名稱，才能依名稱對應輸入。
- 已知失敗清單的閘門：動態測試在 Gradle 裡的名稱是「方法()[序號]」，序號會隨範例增減改變，所以改用顯示名稱當身分。否則 28 個範例會被合併成一筆「x28」，換成另外 28 個失敗也不會被發現。參數化測試照舊歸到同一個方法。
- `specs/` 宣告為測試的輸入：只改規格也會讓測試重跑。

## 三、驗證

| 驗證 | 結果 |
|---|---|
| 執行器自己的測試 | 26 個，全部通過：該通過的 5 種、結果不符 9 種、無法執行 7 種、範例名稱、值的比較規則 |
| 把執行器的錯誤代碼比對拿掉 | `wrongErrorCodeFails` 失敗 |
| 把一個本來通過的範例的預期代碼改掉（`rejects_create_by_a_non_manager` 改成 `HOSPITAL_SCOPE_MISMATCH`） | 建置失敗，報告指出「不在清單上的失敗：review-project/rejects_create_by_a_non_manager」 |
| 完整的 `./gradlew check` | 681 個測試，39 個失敗，與清單相同（原有 11 筆加 28 個範例），通過 |

第二、三列等於實際做了一次及格線第 4 項：改規格、不改程式，驗收變紅。

### Copilot 審查後的修正（PR #23）

| 發現 | 原本的行為 | 修正 |
|---|---|---|
| 範例名稱的空白換成底線 | `check result` 與 `check_result` 變成同一筆，一紅一綠互換時閘門看不出來 | 名稱只能用英數字與 `_` `.` `-`；閘門不改寫顯示名稱，含空白直接判錯 |
| 有 `examples.yaml` 但沒有範例 | 測試清單是空的，什麼都沒評估就通過 | 判為失敗 |
| `expect.output` 不代入變數 | 不能斷言系統產生的 id | 比對前代入 `${名稱}` |
| 集合比對先到先得 | 部分比對的物件會因順序不同而誤判 | 改成一對一配對（增廣路徑），兩種順序結果相同 |

新的 3 個測試對修正前的執行器都失敗。

## 四、第一次執行的結果（ReviewProject，30 個範例）

| 結果 | 數量 | 原因 | 怎麼變綠 |
|---|---|---|---|
| 通過 | 2 | `rejects_create_by_a_non_manager`、`rejects_update_of_an_unknown_project` | — |
| needs 未滿足 | 12 | 要等評分表的規格 | 評分表規格升格 |
| 輸入名稱對不上 | 13 | `CreateReviewProjectUseCase.execute` 沒有 `qualityThreshold`、`initialReviewWeight`、`rereviewWeight`；fixture `draft_project` 帶品質線，所以用到它的範例都停在這一步 | 實作 D-046、D-047 的輸入 |
| 輸出欄位缺少 | 1 | 專案詳情沒有 `medicalRecordTypes`、`versionNo`、`versionPublished`、兩個權重（程式叫 `currentVersionNumber`、`currentVersionPublished`） | 輸出改用規格的名稱並補齊欄位 |
| 錯誤代碼不同 | 1 | 名稱空白時程式丟 `INVALID_PROJECT_NAME`，規格是 `VALIDATION_ERROR`（Q-14、D-040） | 改代碼 |
| 沒有拒絕 | 1 | 病歷類型是空集合時程式接受，規格要拒絕（D-040） | 補檢查 |

輸入名稱對不上的 13 個修好之後，背後的範例才會真正執行，可能還會出現下一層的落差（例如 `AUDITOR` 讀取、更新草稿的參數）。這是預期的：執行器一次只能看到第一個擋住的地方。

## 五、建構 PR 的進度

### 2-1：品質線與權重的輸入（D-046、D-047、D-049）

`CreateReviewProject`、`UpdateReviewProjectDraft` 加上 `qualityThreshold`、`initialReviewWeight`、`rereviewWeight`。品質線在建立與更新時驗證（整數 1～10，否則 `VALIDATION_ERROR`）；權重只在發布時驗證。更新草稿整組取代：沒給的品質線變成 null，沒給的權重回到 0.50。

自動審查後補上的兩項（同一個 PR）：

- 已發布但尚未啟用的專案狀態仍是 `DRAFT`，原本只檢查狀態，已發布的版本仍可被更新；加上計分輸入後連品質線與權重都會被改掉。現在版本已發布就拒絕（`INVALID_STATE_TRANSITION`，RP-UPD-01）。
- 權重可以輸入之後，發布要依 D-020 驗證（RP-PUB-03），否則 0.70 + 0.50 也會被發布。`validateWeightsForPublish` 原本存在但沒有被呼叫，丟的是會變成 500 的 `IllegalArgumentException`；改成 `PROJECT_VERSION_NOT_PUBLISHABLE`（422），同時少了 3 筆凍結的 ARCH-03 違規。權重在輸入時只檢查型別 `decimal(2)`（最多兩位小數、整數部分一位），讓草稿接受的值都存得進資料庫。

順帶修掉 `CreateReviewProjectUseCase` 的 ARCH-02 違規：改用新的 `IdGenerator` 埠，不再直接呼叫 `UUID.randomUUID()`。改簽名會讓凍結基線裡的那一筆「換名字」，修掉違規才能只刪行。

| 結果 | 範例 |
|---|---|
| 變綠（6） | 三個品質線範圍檢查、`rejects_update_from_another_hospital`、`rejects_activate_before_any_version_is_published`、`rejects_deactivate_of_a_draft_project` |
| 進到下一層 | 見下表 |

已知失敗清單只能刪行，不能改註解，所以剩下範例的最新原因記在這裡：

| 範例 | 現在停在哪裡 | 由哪一批處理 |
|---|---|---|
| `creates_a_draft_project`、`creates_a_project_with_given_scoring_inputs`、`updates_a_draft_project`、`rejects_update_with_a_quality_threshold_out_of_range`、`update_resets_weights_that_are_not_given` | 專案詳情沒有 `versionNo`、`versionPublished`、`medicalRecordTypes`、`qualityThreshold`、兩個權重 | 2-2 |
| `rejects_create_with_a_blank_name`、`rejects_update_with_a_blank_name` | 錯誤代碼是 `INVALID_PROJECT_NAME`，規格是 `VALIDATION_ERROR` | 2-3 |
| `rejects_create_without_any_medical_record_type`、`rejects_update_without_any_medical_record_type` | 空集合沒有被拒絕 | 2-3 |
| `rejects_publish_without_a_bound_score_sheet` | 發布沒有檢查是否已綁定評分表（RP-PUB-02，D-041） | 另一批（2-4） |

### 2-2：專案詳情的輸出使用規格的名稱（D-043）

`ReviewProjectSummary`（application 的查詢 view）改用規格的欄位名稱：`versionNo`、`versionPublished`，並補上 `medicalRecordTypes`、`qualityThreshold`、`initialReviewWeight`、`rereviewWeight`。HTTP 回應的 JSON 欄位名稱不變（HTTP 對應屬於實作端）。

同一支 use case 順帶對齊 D-043：`AUDITOR` 可以讀專案詳情，限自己的院區；`REVIEWER`、`REREVIEWER` 仍被拒（Q-12 未裁決）。這條規則沒有範例，執行器看不到這個落差，是讀規格時發現的。原本有一個測試把 `AUDITOR` 當成「沒有權限的角色」，那是照著舊行為寫的，已改用 `REVIEWER`。

| 結果 | 範例 |
|---|---|
| 變綠（5） | `creates_a_draft_project`、`creates_a_project_with_given_scoring_inputs`、`updates_a_draft_project`、`rejects_update_with_a_quality_threshold_out_of_range`、`update_resets_weights_that_are_not_given` |
| 仍是紅的（5） | 名稱空白與空集合 4 個（2-3）、`rejects_publish_without_a_bound_score_sheet`（2-4） |

ReviewProject 30 個範例目前：13 個通過、5 個等建構 PR、12 個等評分表的規格。

## 六、還沒做的

- 「沒有範例涵蓋的規則」與「`basis` 含 `proposed` 規則」從只列出改為擋下。
- 驗證 `emits`：範圍內沒有查詢看得到 event。
- 執行器的直譯邏輯是否搬進 `harness-core/`（跨語言）。
- 建構 PR：依第四節逐一讓範例變綠，每個 PR 從清單刪掉對應的行。
