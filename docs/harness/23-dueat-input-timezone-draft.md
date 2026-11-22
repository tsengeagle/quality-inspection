# 違憲清單第 2 項：HTTP 收進來的 `dueAt` 不帶時區

狀態：**已裁決**（2026-11-22）。負責人對 I-1～I-5 全部照建議裁決，條文是 `decision-log.md` 的 D-073，以那裡為準。本文件不是律法，下面保留起草時的版本（main = `f94254a`）。

**修法時的調整**：HTTP 契約不涵蓋請求端，I-3 只能在 `transport_codes` 補 `VALIDATION_ERROR`，行為改由建構 PR 的 API 測試保證（D-073 第 7 點）。題號 I-1～I-5 只在本文件使用。

## 一、背景

- 憲法草案第一條第 2 點規定，不帶時區的輸入一律拒絕，回 `VALIDATION_ERROR`。
- D-072 第 4 點第 2 項把「指派、改派初審醫師與複審醫師的 `dueAt` 接受不帶時區的值」列為違憲，並註明修正前要先確認前端送出的格式。
- 負責人確認過，`dueAt` 是一個確切的時刻（D-072 第 3 點）。

## 二、查到的事實

**四個端點**（都在 `ReviewCaseController`），request DTO 的 `dueAt` 都是 `LocalDateTime`：

| 端點 | DTO | use case | `law/coverage.txt` |
|---|---|---|---|
| `POST /{caseId}/assign-reviewer` | `AssignReviewerRequest` | `AssignReviewCaseUseCase` | use case 是 `legislated` |
| `POST /{caseId}/reassign-reviewer` | `ReassignReviewerRequest` | `ReassignReviewerUseCase` | `grandfathered`（改派未立法） |
| `POST /{caseId}/assign-rereviewer` | `AssignRereviewerRequest` | `AssignRereviewerUseCase` | use case 是 `legislated` |
| `POST /{caseId}/reassign-rereviewer` | `ReassignRereviewerRequest` | `ReassignRereviewerUseCase` | `grandfathered`（改派未立法） |

- `ReviewCaseController` 與 `interfaces/rest/dto/` 都是 `grandfathered`（未立法）。

**前端**（`case-assignment-page.component`）
- 用 `<input type="datetime-local">` 取得牆上時刻。
- `normalizeDueAtInput` 只補上秒數，送出的是 `"2026-11-26T23:59:00"`，**不帶時區**。
- 所以後端一改成拒絕不帶時區的值，前端的指派就會全部失敗。前後端必須同時改。

**錯誤處理**
- `GlobalExceptionHandler` 是 `legislated`（`specs/http-common.yaml`），但沒有處理 `HttpMessageNotReadableException`，也就是 JSON 無法轉成目標型別的情況。
- 如果把 DTO 改成帶時區的型別，Jackson 遇到不帶時區的字串會在 controller 之前就失敗。回應會變成 Spring 預設的格式，而不是 `ApiResponse` 的外殼，也不是 `VALIDATION_ERROR`。這是推論，還沒有實際驗證。

**domain 的現況**
- `ReviewCase` 的 `dueAt` 仍是 `LocalDateTime`，屬於違憲清單第 4 項，不在本題範圍。
- 本題只改輸入的邊界：收到帶時區的值之後，以 `Asia/Taipei` 換回牆上時刻，再交給 use case。

## 三、I-1：前端送出時用哪個時區

### 選項

- **A（建議）：固定用 `Asia/Taipei`。** 把 `datetime-local` 的牆上時刻當成台北時間，加上 `+08:00` 送出，例如 `"2026-11-26T23:59:00+08:00"`。與憲法第一條第 3 點、後端的換算一致，使用者的電腦設在別的時區也不會出錯。
- **B：用瀏覽器的時區**（`new Date(value).toISOString()`）。使用者的電腦時區設錯時，期限會跟著偏移。

### 待負責人決定

A／B？

## 四、I-2：在哪一層換算與拒絕

### 選項

- **A（建議）：在 HTTP 邊界。**
  - DTO 的 `dueAt` 改成 `OffsetDateTime`。不帶時區的字串，Jackson 會直接拒絕。
  - controller 用 `Asia/Taipei` 換成 `LocalDateTime`，再呼叫 use case。use case 的簽名不變。
  - 等違憲清單第 4 項（domain 改成 `Instant`）時，再一起改 use case。
- **B：use case 改收 `Instant`，在 use case 裡換算。** 會動到已立法的 `AssignReviewCase`、`AssignRereviewer` 的簽名，規格範例也要跟著改，範圍比較大。換算的工作在第 4 項本來就要做。

### 待負責人決定

A／B？

## 五、I-3：拒絕時的錯誤回應

- 憲法要求回 `VALIDATION_ERROR`。HTTP 狀態碼依 `http-common.yaml`，`invalid_input` 是 400。
- **建議**：
  - `GlobalExceptionHandler` 新增 `HttpMessageNotReadableException` 的處理：原因是時間格式（Jackson 的 `InvalidFormatException`，目標型別是時間）時，回 400 `VALIDATION_ERROR`。
  - 其他無法讀取的 JSON 怎麼處理（`BAD_REQUEST` 或 `VALIDATION_ERROR`），本題不決定，維持現狀。
- `GlobalExceptionHandler` 已立法，這要修法：在 `specs/http-common.yaml` 補一條規則，並寫 HTTP 契約的範例。

### 待負責人決定

同意嗎？範圍要不要擴大到所有無法讀取的 JSON？

## 六、I-4：未立法的範圍（D-060）

- 改派的兩個端點與 use case，以及 controller、DTO，都是 `grandfathered`。CLAUDE.md 規定，動到這些地方之前要先請負責人決定，三個選項是：
  - 先立法；
  - 以修法列為暫准；
  - 在暫准範圍內照現有權威文件實作。
- **建議**：照第三個選項，在暫准範圍內照憲法第一條與 D-072 實作。
  - 理由：本題只改輸入格式，不改業務規則，權威是憲法，條文已經很明確。
  - 改派的業務規則等改派立法時再處理。
  - 已立法的兩個 use case 簽名不變（I-2 選 A），所以規格不用改。

### 待負責人決定

同意在暫准範圍內實作？還是要先為四個端點的輸入格式立法？

## 七、I-5：要不要同時修正前端的顯示

- 前端顯示 `dueAt` 時（`displayDate`）收到的是不帶時區的字串（違憲清單第 3 項）。
- **建議**：不在本題處理。前端的送出（I-1）必須跟本題一起改，顯示等第 3 項時再改。

## 八、裁決後的順序

1. **修法 PR**：
   - `decision-log.md` 新增一條；
   - `specs/http-common.yaml` 補時間格式錯誤的規則與範例（I-3），紅的範例列進已知失敗清單；
   - 本草稿標為已裁決。
2. **建構 PR**（前後端一起改）：
   - 四個 DTO 改成 `OffsetDateTime`；
   - controller 換算成台北時間；
   - `GlobalExceptionHandler` 新增處理；
   - 前端送出加上 `+08:00`；
   - API 測試的 request 都改成帶時區，新增「不帶時區的值回 400」的測試；
   - 清空已知失敗清單。
3. 在下一個修法 PR，於 D-072 的違憲清單註明第 2 項（與第 1 項的 `ASSIGNED`）由哪個 PR 修正。

### 還沒有查證的

- Jackson 遇到不帶時區的字串轉 `OffsetDateTime` 時，丟出的確切例外型別與回應格式。
- 前端的單元測試與 E2E 測試是否斷言送出的 `dueAt` 格式。
- 本機 seed 或 demo 資料是否以不帶時區的格式呼叫這四個端點。
