# 規格格式 v2.1 草案：請求端的範例

狀態：**已裁決**（2026-11-22）。負責人對 R-1～R-8 全部照建議裁決，並同意查證後新增的院區覆寫檢查。條文是 `decision-log.md` 的 D-074，以那裡為準。本文件不是律法，下面保留起草時的版本（main = `04bbdb0`），第十二節是裁決前完成的查證。題號 R-1～R-8 只在本文件使用。

## 一、背景

- D-073 第 7 點：「不帶時區的 `dueAt` 回 400 `VALIDATION_ERROR`」沒有可以執行的規格，目前由 JUnit 的 API 測試（`DueAtTimeZoneApiTest`）保證。這有兩個弱點：
  - `given` 是手工存 entity，不是 use case 走出來的狀態；
  - 測試不在律法清冊裡，建構 PR 不簽章就能改掉或刪掉。
- 負責人裁決（2026-11-22）：把「讓規格能寫請求端範例」列入題目清單（方向 B）。
- 之後會用到同一個能力的題目還有：違憲清單第 3 項（HTTP 輸出的時間格式）、其他無法讀取的請求本文、query 參數的格式。

## 二、查到的事實

**範例的執行路徑**
- `ExampleRunner`（1003 行）把 `run` 步驟交給 `UseCaseInvoker`。`SpringUseCaseInvoker` 依參數名稱呼叫 use case 的 `execute`，操作者直接建成 `ActorContext`。整條路徑不經過 HTTP、JSON 讀取或認證。
- `AcceptanceExamplesTest` 是 `@SpringBootTest`，沒有 MockMvc。
- 步驟只有 `run`、`use`、`port`、`advance` 四種（`spec_lint.py:910`）；`when` 必須是 `run`（`spec_lint.py:1073`）。

**時間的檢查**
- `spec_lint.py:236` 的 `TIME_LIKE` 要求：看起來像時間的字串必須帶時區（D-065 第 5 點）。不合憲的輸入因此寫不進範例。

**HTTP 契約**
- `specs/<aggregate>/http.yaml` 以 `usecase` 對到 `method`、`path`，只比對回應的形狀，不涵蓋請求端（README 第七節第 8 點）。
- 回應由 `HttpObservationInitializer` 掛在每個 Spring 測試 context 的 MockMvc 上觀察。
- `review-case` 沒有 `http.yaml`。

**認證**
- 請求帶 `api-key`，內容是 JWT：`user_id`、`user_name`、`hospital_id`、`user_role`。`user_role` 是清單，所以可以表示多個角色。由 `TzJwtParser` 解析。
- 測試用 `TestTokenFactory` 產生 token，但只有固定幾種角色組合。

**律法分類**
- `spec_lint.py`（`harness-core/`）與執行器（`acceptance/`）都是 gate 類，修改要走修法 PR。
- D-065 的先例是分兩個修法 PR：先改格式與 `spec_lint`，再改執行器。

## 三、R-1：請求端範例的範圍

### 選項

- **A（建議）：只寫 use case 範例看不到的行為。** 也就是請求本文的讀取（格式、型別、時區）、認證，以及錯誤代碼對到的 HTTP 狀態碼與外殼。業務規則仍然只寫在 use case 範例，同一條規則不在兩層重複。
- **B：所有行為都可以在 HTTP 層寫範例。** 範例會變成兩套，同一條規則可能在兩層寫法不一致。

### 待負責人決定

A／B？

## 四、R-2：寫在哪裡、怎麼指定端點

### 選項

- **A（建議）：寫在同一份 `examples.yaml`，`when` 新增 `http` 步驟。**
  - `given` 照舊沿用 fixture，狀態由 use case 走出來，解決第一個弱點。
  - 範例屬於規格，解決第二個弱點。
  - 端點直接寫 `method` 與 `path`，路徑可以用 `${變數}`。該目錄有 `http.yaml` 時，`spec_lint` 檢查 `method`、`path` 必須是其中的端點；沒有 `http.yaml` 時只檢查寫法。
- **B：寫在 `http.yaml`，新增 `examples:`。** 要另外寫 `given`，或者跨檔引用 `examples.yaml` 的 fixture。而且沒有 `http.yaml` 的目錄（例如 `review-case`）得先建一份完整的回應契約，範圍大很多。
- **C：同 A，但端點以 `usecase` 名稱指定，從 `http.yaml` 查出 `method`、`path`。** 一樣得先替 `review-case` 建 `http.yaml`。

### 待負責人決定

A／B／C？

## 五、R-3：請求本文的寫法

### 建議

- `body`：YAML 對應表，執行時轉成 JSON。照常套用所有檢查，包括 D-065 第 5 點的時間要帶時區。
- `body_raw`：一段原始字串，原封不動送出。這是專為「不合憲或讀不進來的輸入」準備的寫法：
  - 不受 D-065 第 5 點約束；
  - 不檢查內容；
  - 只能用在 `then.result` 是 `REJECTED` 的範例。合憲的輸入一律用 `body`。
- `body` 與 `body_raw` 只能擇一。
- `query`：對應表，同 `body` 的檢查。

### 待負責人決定

同意 `body_raw` 的豁免與「只能用在被拒絕的範例」這個限制嗎？

## 六、R-4：`then` 怎麼寫

### 建議

- 沿用 use case 範例的寫法：`result`、`code`、`output`、`absent`、`only`、`emits`。
  - `output` 等比對的是回應外殼中的 `data`；被拒絕時比對 `error.code`。
  - HTTP 狀態碼不在範例裡寫，執行器依 `http-common.yaml` 的 `status_by_category` 與 `transport_codes` 推得並檢查。這與規則 7「錯誤代碼只寫分類」一致。
  - 外殼的形狀由 HTTP 契約檢查，範例不重複。
- **例外**：沒有外殼的回應（D-073「其他讀不進來的本文」維持 Spring 預設的空本文）。這種回應以 `then: { status: 400, body: empty }` 表示，只能與 `body_raw` 一起使用。

### 待負責人決定

同意不寫狀態碼、改由推得嗎？例外的寫法是否可以？

## 七、R-5：操作者怎麼變成 `api-key`

### 建議

- 執行器依範例的 `actors`（`user`、`roles`、`hospital`）產生 JWT，角色轉成 `user_role`。
- 角色名稱的對照放進 `spec-profile.yaml`，由執行器讀取，不寫死在程式裡（D-064 的精神）。
- 新增 `as: anonymous`，表示不帶 `api-key`，用來寫認證失敗（401 `UNAUTHORIZED`）的範例。
- `spec-profile.yaml` 是律法，對照表要隨格式的修法一起加。

### 待負責人決定

同意嗎？

## 八、R-6：版本與 `spec_lint`

### 建議

- 這次只是新增，不改變既有範例的意思，所以版本號是**格式 v2.1**，檔案仍是 `spec_version: 2`。README 第十節列出 v2.1 新增的項目。
- `spec_lint` 要配合三件事：
  - `when` 可以是 `run` 或 `http`；
  - 判斷「分辨不了情境」時，`http` 步驟也要納入比較；
  - 檢查 `body_raw` 的限制。
- `given` 不允許 `http` 步驟。準備狀態一律走 use case，HTTP 只出現在 `when`。

### 待負責人決定

同意 `given` 不允許 `http` 嗎？

## 九、R-7：執行器

### 建議

- `AcceptanceExamplesTest` 加上 MockMvc。`http` 步驟用 MockMvc 送出請求，回應同時也會被 HTTP 契約觀察到（第二節），等於多了涵蓋。
- 事件（`emits`）的擷取方式與 `run` 相同。
- 時鐘與埠照舊。

## 十、R-8：既有 JUnit 測試的處理

### 建議

- 範例寫好、變綠之後，`DueAtTimeZoneApiTest` 中與範例重複的部分在建構 PR 刪除，避免兩處各寫一次。
- 不重複的部分保留：換算的單元層級檢查（`+08:00` 與 `Z` 存成同一個台北時刻）要看資料庫的內容，範例做不到。
- 其他既有的 API 測試不搬，之後遇到相關題目時再逐一評估。

### 待負責人決定

同意嗎？

## 十一、裁決後的順序

比照 D-065，分成兩個修法 PR，最後一個建構 PR：

1. **修法 PR 一（格式）**：
   - `decision-log.md` 新增一條；
   - `specs/README.md` 第十節加 v2.1；
   - `spec_lint.py` 與它的測試；
   - `spec-profile.yaml` 的角色對照；
   - 本草稿標為已裁決；
   - 新增 `docs/harness/` 的題目清單（之前答應過）；
   - 補上 D-072 違憲清單第 2 項由 #136 修正的註記。

   這個 PR 合併之前，`specs/` 不得出現 `http` 步驟。
2. **修法 PR 二（執行器與第一批範例）**：
   - `acceptance/` 的執行器與它的測試；
   - `specs/review-case/examples.yaml` 加 D-073 的範例：四個端點收到不帶時區的值回 `VALIDATION_ERROR`，以及其他讀不進來的本文維持現狀。

   範例應該直接是綠的，因為 #136 已經實作。
3. **建構 PR**：依 R-8 刪除 `DueAtTimeZoneApiTest` 中重複的部分。

### 還沒有查證的

- `TzJwtParser` 的角色名稱與 `DomainRole` 的對照（例如 `hospital_manager` 與 `HOSPITAL_MANAGER`）是否一對一。
- `AcceptanceExamplesTest` 加上 MockMvc 之後，現有的 85 個範例的執行時間與結果是否不變。
- 被拒絕時是否「沒有發出事件」。use case 範例的被拒絕會檢查這一點，`http` 步驟要比照。

## 十二、裁決前完成的查證（2026-11-22）

第十一節「還沒有查證的」三項，在負責人要求下完成查證。

### 1. JWT 角色名稱與 `DomainRole`

- `TzJwtParser` 以 `DomainRole.valueOf(role_name.toUpperCase())` 轉換，五個角色一對一，大小寫不拘。`user_role` 是清單，可以表示多個角色。
- 停用與不認得的角色會被安靜略過。
- JWT 沒有驗簽，只做 base64 解碼。
- **新發現**：`quality-inspection.hospital-id`（環境變數 `QUALITY_INSPECTION_HOSPITAL_ID`）有值時，JWT 的院區會被忽略。本機與 CI 都沒有設定。執行器若不檢查，`http` 範例的院區可能被悄悄換掉。由此新增 D-074 第 7 點。

### 2. 加上 MockMvc 的影響

在本機暫時替 `AcceptanceExamplesTest` 加上 `@AutoConfigureMockMvc`，量測後還原：

| | 範例 | 失敗 | 執行時間 | 牆鐘時間 |
|---|---|---|---|---|
| 現狀（3 次） | 174 | 0 | 3.71～3.78 秒 | 22 秒 |
| 加 MockMvc（2 次） | 174 | 0 | 3.77～4.03 秒 | 23 秒 |

174 個範例名稱相同（原草稿寫 85 個，那是 review-case 一個目錄的數字）。這個測試本來就有獨立的 context（`@Import(AcceptanceWorldConfiguration)`），不會多建一個。

### 3. 被拒絕時的事件

以暫時的測試經 MockMvc 打 `assign-reviewer`，請求都在 `Test worker` 執行緒處理，`SpringExampleWorld` 同步擷取事件：

| 請求 | 狀態碼 | 擷取到的事件 |
|---|---|---|
| `dueAt` 不帶時區 | 400 | 無 |
| `dueAt` 帶 `+08:00` | 200 | `ReviewCaseAssigned` |
| 再指派一次（業務規則拒絕） | 422 | 無 |

執行器的 `checkEvents` 不用改：`http` 步驟把「回應不是 2xx」當成被拒絕即可。

