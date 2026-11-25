# 切片 D、E：HTTP 介面（controller、dto）與共用元件

狀態：**已裁決**（2026-11-25）。負責人裁決：照建議分批（H-1 選 A），H-2～H-12 全部照建議。下面保留起草時的版本（main = `69a90a8`），第九節是裁決後的實測。本文件不是律法；題號 H-1～H-12 只在本文件使用。

負責人 2026-11-25 同意的順序：C（D-085，已完成）→ A（D-086，已完成）→ B（D-087，已完成）→ **D、E（本切片）** → 專案版本管理與未裁決事項。

## 一、範圍

`law/coverage.txt` 裡剩下的 `grandfathered` 列，全部屬於本切片：

| 範圍 | 檔案 | 現況 |
|---|---|---|
| D：controller | `interfaces/rest/` 的 `ReviewCaseController`（25 個端點）、`SubmissionController`（1）、`SamplingController`（7）、`AuditController`（1）、`ReportController`（2）、`ReviewProjectController`（9）、`ScoreSheetController`（7） | 後兩個的回應由 HTTP 契約涵蓋，請求端未立法（D-061 Q-3）；前五個完全未立法 |
| D：dto | `interfaces/rest/dto/` 的請求 dto 22 個、回應 dto 9 個（`ApiResponse` 與 score-sheet、review-project 的 7 個已立法） | 未立法 |
| D：HTTP 共用 | `ActorContextArgumentResolver`、`CurrentActor`、`BadRequestException`、`interfaces/package-info.java`、`interfaces/config/WebMvcConfig` | 未立法（D-061 第 6 點「操作者的傳遞」） |
| E：application 共用 | `application/common/` 的 `ActorContext`、`DomainRole`、`RoleDeniedException`、`AccessDeniedException`、`Page`、`IdGenerator`、`DomainEventPublisher`、`HospitalIdResolver`、`package-info.java` | 未立法（匯出用的三個已由 D-087 立法） |
| E：domain 共用 | `domain/common/` 的 `DomainEvent`、四個例外、`MedicalRecordType`，`domain/package-info.java` | 未立法 |

不在本切片：`infrastructure/`（不屬於問題側），包括 `ApiKeyAuthFilter`、`TzJwtParser`；前端。

## 二、查到的事實

**本節是讀程式確認的，沒有實測。** 標「推測」的項目要在建構前用 MockMvc 實測。

**回應的契約**

1. 只有 score-sheet（7 個端點）與 review-project（9 個）有 `http.yaml`。review-case、sampling-batch、review-submission 都沒有，所以下面 36 個端點的回應形狀沒有律法：
   - review-case 的 use case 背後的端點：`ReviewCaseController` 25 個、`SubmissionController` 1 個（`GetSubmissionDetail`）、`AuditController` 1 個（`ListAuditTrails`）、`ReportController` 2 個。`GetCaseLifecycle` 是 `ReviewCaseController` 的一個端點。這些 use case 都在 `specs/review-case/usecases.yaml`。
   - sampling-batch：`SamplingController` 7 個，use case 都在 `specs/sampling-batch/usecases.yaml`。
2. D-061 第 8 點要求「每立法一個 aggregate，就同時寫它的 `http.yaml`」。review-case（D-066～D-084）、sampling-batch（D-086）立法時都沒有寫，現在一次補。
3. 有 15 個左右的端點直接把 application 的物件交給 Jackson 輸出（`Page<ReviewCaseSummary>`、`ReviewCaseDetailView`、`SamplingPreviewResult`、`DashboardSummary`、`ExportResult` 等），沒有經過回應 dto。這時 HTTP 欄位名稱就是 application 類別的 getter 名稱：改 application 的欄位名稱會悄悄改掉 HTTP。
4. 列表的分頁外殼是 `Page`（`application/common/`）：`content`、`page`、`size`、`totalElements`、`totalPages`。review-project、score-sheet 的列表是 `{ items }`（D-050 第 7 點、D-062），兩種寫法並存。
5. 切片 A 草稿第七節記下的抽選落差：
   - 執行抽選的回應只有 `batchId`；`api-spec.md` §9.3 寫 `batchId`、`status`、`selectedCount`。
   - 重跑的回應是 `batchId`；§9.4 寫 `newBatchId`、`retryOfBatchId`。
   - 預覽、執行的請求本文 `mode` 沒有讀取。

**請求端的檢查放在 controller**

6. 查詢參數的列舉在 controller 轉換，不認得的值回 400 `BAD_REQUEST`：
   - 案件列表的 `status`、分派候選人的 `role`、抽選批次列表的 `status`、`executionMode`；
   - review-project 的 `medicalRecordType`、score-sheet 的列舉欄位（`parseEnum`）。

   這些檢查在 use case 之前，所以在角色檢查之前：沒有權限的角色帶不認得的值，得到的是 400 不是 403。與 D-024 的順序不同（同 D-087 第 2 點修正前的報表）。
7. 請求 dto 有 `@NotBlank` 13 處、`@NotNull` 7 處，掛在 `ReviewCaseController` 11 個、`ReviewProjectController` 3 個、`ReportController` 1 個端點。違反時回 400 `VALIDATION_ERROR`，也在角色檢查之前。其中至少 `reason`（取消、退回）在 use case 也有同樣的檢查（`usecases.yaml` 的 `required: true`）。其他欄位 use case 有沒有檢查，**沒有逐一確認**。
8. `GlobalExceptionHandler`（已立法）沒有處理下列例外，推測會由 Spring 預設處理，回 400、沒有外殼（推測，沒有實測）：
   - 查詢參數型別不符：`from`、`to`（`Instant`）、`page`、`size`（`int`）讀不進來；
   - 少了必填的查詢參數：分派候選人的 `role`；
   - 本文讀不進來，但不是時間格式的問題（D-073 第 3 點刻意維持現狀）。

   `http-common.yaml` 的外殼不允許沒有外殼的錯誤回應，但比對器只比對有觀察到的回應，目前沒有測試走到這些情況就不會擋。
9. `IllegalArgumentException` 會變成 500 `INTERNAL_ERROR`（交接文件的發現）。controller 本身不丟，但 `ActorContextArgumentResolver` 找不到操作者時丟 `IllegalStateException`，也是 500。正常流程裡 `ApiKeyAuthFilter` 先擋 401，所以走不到。

**操作者的傳遞**

10. `ApiKeyAuthFilter`（`infrastructure/`）把 JWT 讀成 `ActorContext` 放進請求屬性；`ActorContextArgumentResolver` 把它交給標了 `@CurrentActor` 的參數；`WebMvcConfig` 只負責註冊這個 resolver。D-074 的請求端範例已經實際走過這條路（`as: manager_h1`、`as: anonymous`）。
11. ARCH-04 的基線有一行 `infrastructure/security/ApiKeyAuthFilter.java imports ...interfaces.rest.dto.ApiResponse`（401 的回應本文）。這一行在 `infrastructure/`，不屬於本切片的檔案，但跟 `ApiResponse` 有關。

**共用元件**

12. `ActorContext` 的 `requireRole`、`requireAnyRole`、`requireHospitalScope`、`requireReadScope`、`readScopeHospitalId` 就是規格裡每一個 `guards`（D-023、D-024、D-042、D-047）的實作，已立法的範例每天都在跑它們，只是檔案沒有登記。
13. 例外與錯誤分類一一對應（`GlobalExceptionHandler`）：`RoleDeniedException`、`AccessDeniedException` → 403；`ResourceNotFoundException` → 404；`DomainValidationException` → 400；`DomainStateConflictException` → 409；`DomainRuleViolationException` → 422。這就是 `http-common.yaml` 的 `status_by_category`。
14. `IdGenerator`、`DomainEventPublisher`、`HospitalIdResolver` 是介面（port），實作在 `infrastructure/`。`HospitalIdResolver` 只有 score-sheet 的 6 個 use case 使用。
15. `MedicalRecordType` 的列舉寫在 `specs/review-project/model.yaml`；`DomainRole` 的五個值寫在 `spec-profile.yaml` 的 `role_claims`。

**既有的測試**：`interfaces/rest/` 底下 17 個 `*ApiTest`（`ReviewCaseApiTest`、`SamplingControllerApiTest`、`ReportApiTest`、`AuditApiTest`、`DueAtTimeZoneApiTest` 等）。它們經過 MockMvc，所以補上 `http.yaml` 之後會直接被比對器觀察到。

## 三、怎麼分批

### H-1：一次做完，還是分批

- **A（建議）：** 一份裁決，分三批建構，每批一組修法 PR 與建構 PR：
  1. **D-1 回應契約**：`specs/review-case/http.yaml`、`specs/sampling-batch/http.yaml`，回應 dto 登記為已立法（H-2～H-4）。
  2. **D-2 請求端**：controller、請求 dto、HTTP 共用（H-5～H-9）。
  3. **E 共用元件**：`application/common/`、`domain/common/`（H-10、H-11）。
- **B：** 分成三份裁決，各自起草。每一批的事實都要重新查一次，而且 D-2 依賴 D-1 的 `http.yaml`。
- **C：** 一份裁決、一次建構。36 個端點的契約加上請求端，一個 PR 太大，閘門變紅時不好找原因。

## 四、D-1：回應契約

### H-2：回應形狀以誰為準

- **A（建議）：** 照程式現況寫 `http.yaml`（D-058「路徑以程式為準」的延伸）。修法 PR 前先在本機用現有的 `*ApiTest` 跑出比對報告，確認問題數是 0 再推送（比照 D-087 的作法）。與 `api-spec.md` 不同的地方（事實 5）列進 `OPEN-QUESTIONS.md`，之後一題一題裁決。
- **B：** 以 `api-spec.md` 為準，先改程式再寫契約。前端會受影響，範圍超出立法。

### H-3：application 物件直接輸出

- **A（建議）：** 程式不動。契約是封閉的形狀，application 類別改了欄位名稱，比對器會擋下，事實 3 的風險就被律法涵蓋了。
- **B：** 每個端點都改成回應 dto，與 score-sheet、review-project 一致。大約 15 個新類別，HTTP 的行為不變，好處是 application 跟 HTTP 分開。可以之後再做。

### H-4：涵蓋要求的缺口

D-061 第 3 點要求每個端點都要有成功的觀察、每個陣列都要有非空的觀察、可為 null 的欄位 null 與非 null 都要有。現有的 `*ApiTest` 一定有缺。

- **A（建議）：** D-1 的修法 PR 前在本機先跑，缺的部分用 D-074 的 `http` 步驟補成功的範例（寫在 `specs/review-case/examples.yaml`、`specs/sampling-batch/examples.yaml` 的「請求端」一段），不補 JUnit 測試。範例要先在本機跑綠（新範例在修法 PR 時才能跑）。
- **B：** 補 `*ApiTest`。比較快，但補的測試不在律法清冊裡。

## 五、D-2：請求端

### H-5：查詢參數列舉的檢查（事實 6）

- **A（建議）：** 比照 D-087 第 2 點：轉換移進 use case，use case 收字串，順序改成 D-024（角色 → 輸入的檢查）。錯誤代碼維持 `BAD_REQUEST`，前端不用改。行為變更只有一個：沒有權限的角色帶不認得的值，從 400 變成 403。規格的 `guards` 補上這幾條，各寫一個範例。
- **B：** A，但錯誤代碼改成 `VALIDATION_ERROR`。前端與 `api-spec.md` 要跟著改。
- **C：** 留在 controller，用 `http` 步驟的範例把現況（400 在 403 之前）寫成律法。

### H-6：請求 dto 的必填檢查（事實 7）

- **A（建議）：** 建構前逐一確認 20 個欄位在 use case 有沒有對應的檢查：
  - use case 已有的：刪掉 dto 的註解，交給 use case（順序回到 D-024，代碼一樣是 `VALIDATION_ERROR`）。
  - use case 沒有的：在 use case 補上，再刪掉 dto 的註解。這是規格的新增，各補一個範例。
- **B：** 留著，用 `http` 步驟的範例寫成律法。兩層有同一條規則，違反 D-074 第 1 點「不在兩層重複」。

### H-7：沒有外殼的 400（事實 8）

- **A（建議）：** `GlobalExceptionHandler` 補上這三種例外，一律回 400 `BAD_REQUEST`，有外殼。`BAD_REQUEST` 已經在 `transport_codes`，不用改共用層。各寫一個 `http` 步驟的範例。D-073 第 3 點「其他讀不進來的本文維持現狀」一併修訂。
- **B：** 維持現狀，用 `then: { status: 400, body: empty }` 的範例寫成律法。只適用於本文（`body_raw`）；查詢參數的情況格式寫不出來，要另外改格式。

### H-8：請求的欄位名稱

`specs/README.md` 第七節第 8 點：HTTP 契約不涵蓋請求端。請求的欄位名稱寫錯（例如 dto 的 `reviewerId` 改名），範例與契約都不會發現。

- **A（建議）：** 不改格式。每個有本文或查詢參數的端點，至少一個 `http` 步驟的成功範例，`then.output` 或後續的 `observe` 確認值確實傳到 use case。這跟 H-4 A 補的範例是同一批，不另外寫。
- **B：** `http.yaml` 新增 `request` 形狀，比對器也比對請求。要修訂 D-061 與比對器（閘門），範圍比較大。

### H-9：操作者的傳遞與 HTTP 共用

- **建議：** `ActorContextArgumentResolver`、`CurrentActor`、`WebMvcConfig`、`interfaces/package-info.java` 登記為已立法，指向 `specs/http-common.yaml`：D-074 的範例已經用 `as:` 走過這條路（事實 10），另補一個 `as: anonymous` 回 401 `UNAUTHORIZED` 的範例當代表。`BadRequestException` 在 H-5 選 A 之後只剩 review-project、score-sheet 的本文列舉使用，同樣登記到 `http-common.yaml`。
- controller 與請求 dto：H-5～H-8 做完後，依端點所在的規格目錄登記（`ReviewCaseController`、`SubmissionController`、`AuditController`、`ReportController` → `specs/review-case/`；`SamplingController` → `specs/sampling-batch/`；另外兩個不變）。

## 六、E：共用元件

### H-10：`application/common/`、`domain/common/` 怎麼立法

這些檔案的行為已經寫在規格裡（事實 12、13、15），只是檔案沒有登記。

- **A（建議）：** 不新增規則與範例，直接登記為已立法：
  - `ActorContext`、`DomainRole`、`RoleDeniedException`、`AccessDeniedException`、`Page`、四個 domain 例外、`DomainEvent`：指向 `specs/http-common.yaml`（錯誤分類與分頁外殼）。
  - `MedicalRecordType` → `specs/review-project/`。
  - `HospitalIdResolver` → `specs/score-sheet/`（唯一使用者）。
  - `IdGenerator`、`DomainEventPublisher`、兩個 `package-info.java`：只是介面，指向 `specs/http-common.yaml`。

  登記之後，`law/coverage.txt` 沒有 `grandfathered` 的列。新的共用檔案會被 `law-check` 擋下。
- **B：** 為 `ActorContext` 另外寫一份規格（角色與院區的規則各一個範例）。與每個 aggregate 的 `guards` 重複。

### H-11：`Page` 與 `{ items }` 並存（事實 4）

- **A（建議）：** 兩種都照現況寫進契約，不統一。`Page` 的形狀寫在 `http-common.yaml`（新增一個共用的形狀，例如 `$page`），各 `http.yaml` 引用；要不要統一另立題目。共用層新增形狀要先確認比對器支不支援，不支援就在每個 `http.yaml` 各寫一次。
- **B：** 統一成一種。前端與既有的裁決都要改。

## 七、登記與題目清單

### H-12：登記與涵蓋清單的格式

- **建議：**
  - 每一批的修法 PR 把該批的檔案從 `grandfathered` 改成 `legislated`。E 做完後刪掉所有 `grandfathered` 的列；`law/coverage.txt` 檔頭的 `grandfathered` 說明保留（D-060 的格式），或改寫成「目前沒有暫准的範圍」。
  - 新端點的 controller 依然要先列進清單（D-061「逐一列出 controller」那一段的作法不變）。

本切片的第一個修法 PR 一併：

- 題目清單把 B（D-087，#166、#167）移到已完成；D、E 標為進行中。
- 事實 5（抽選的兩個回應欄位與 `mode`）列進 `specs/sampling-batch/OPEN-QUESTIONS.md`。
- 事實 3、H-3 B（回應 dto）、H-11 B（統一列表的寫法）如果選了「之後再做」，列進題目清單。

## 八、建構前要實測的

裁決後、每一批的修法 PR 推送前：

1. 用現有的 `*ApiTest` 跑出 review-case、sampling-batch 的比對報告（暫時的 `spec-draft/` 契約），看實際的形狀與涵蓋缺口（H-2、H-4）。
2. 事實 8 的三種情況實際回什麼（H-7）。
3. 事實 7 的 20 個欄位在 use case 有沒有對應的檢查（H-6）。
4. 共用層能不能定義 `$page` 這種共用形狀（H-11）。

## 九、實測：D-1 的回應形狀（2026-11-25，裁決後）

作法：本機在 `HttpObservationRecorder` 臨時加一段紀錄，把每一筆回應寫到暫存檔，跑完整的 `./gradlew test`（全部通過），共 644 筆；用完已還原，沒有 commit。

1. **36 個端點都有成功的觀察**，成功狀態碼與程式一致（初審開始、執行抽選、重跑是 201，其他是 200）。
2. **沒有外殼的回應只有一筆**：`POST /review-cases/{caseId}/assign-reviewer` 的 400 空本文，是 D-074 的 `body_raw` 範例。事實 8 的三種情況，現有的測試都沒有走到（H-7 要補範例）。
3. **程式寫死 null 的欄位**（`ReviewCaseQueryAdapter`）：案件列表的 `createdAt` 與最後一個欄位、案件詳情的 `createdAt`。照 H-2 A 寫進契約，就是把「永遠是 null」寫成律法。資料表有建立時間，看起來是漏接，不是設計。
4. **只觀察到 null 的欄位**（D-061 的涵蓋要求會不過，H-4 要補範例，或確認它們本來就可為 null）：
   - 案件列表：`assignedAt`、`dueAt`、`reviewerId`、`rereviewerId`（3 筆，都是剛抽選出來的案件）。
   - 案件詳情：`cancelledAt`、`closedAt`、`dueAt`、`latestRevisionSummary`（未裁決事項）、`rereviewContext.initialReviewerName`。
   - 分派頁：`currentRereviewer`、`effectiveDueAt`、`rereviewerDueAt`。
   - 抽選：執行頁的 `blockedReason`、`previewStats`；批次的 `retryOfBatchId`、`failureReason`。
   - 匯出的每一列：`reviewerId`、`rereviewerId`、`initialScore`、`finalScore`、`closedAt`。
5. **型別**：分數有時是整數、有時是小數（`totalScore`），契約寫 `decimal` 即可（比對器的 `decimal` 接受任何數字）。時間都是字串，列舉都是字串，可以寫成 `enum <名稱>`。
6. **其他觀察**：
   - 案件詳情同時有 `status` 與 `caseStatus`。
   - 提交明細有 `attachmentRefsJson`（JSON 字串，不是陣列）。
   - 抽選的落差與事實 5 相同。

### 實測後追加的裁決（2026-11-25）

實測的結果交給負責人後，追加三題，都照建議：

- **建立時間**：`createdAt` 是漏接，建構 PR 接上（D-088 第 3 點）。
- **可為 null 的物件**：格式只允許純量可為 null，8 個「有時 null、有時物件」的欄位寫不進契約。鍵結尾加 `?`（D-088 第 5 點）。
- **一個端點有幾種形狀**：案件詳情有兩種，提交明細有三種（D-084 的遮罩）。新增 `variants`（D-088 第 6 點）。同時發現比對器讀不進格式 v2 的 `usecases.yaml`，一併修正。

另外兩件照建議處理，沒有另外問：H-7 的「其他讀不進來的本文」提前到 D-1（原本的範例在契約生效時會被擋下）；HTTP 與 `api-spec.md` 的落差記在題目清單，不放 `OPEN-QUESTIONS.md`（那個檔案只收 proposed 規則指向的題目）。

## 十、D-2 的實測（2026-11-25）

- **必填的輸入（第八節第 3 項）**：用 use case 範例逐一確認，20 個欄位裡 6 個 use case 沒有檢查（見 D-088 第 13 點）。另外，本機暫時拿掉 dto 的註解跑完整測試，只有一個測試失敗：初審草稿沒給 `answers` 時，controller 直接對 null 呼叫 `stream()`（`ReviewCaseController` 第 240 行）。
- **事實 8 的更正**：查詢參數型別不符（`page=abc`、`from=not-a-time`）回的是 500 `INTERNAL_ERROR`，不是沒有外殼的 400。少了必填的 `role` 才是沒有外殼的 400。
- **review-project、score-sheet**：範例是格式 v1，不能寫 `http` 步驟，負責人選擇另立題目（D-088 第 13 點）。
