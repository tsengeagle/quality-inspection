# HTTP 契約如何立法：選項比較（待裁決）

> 狀態：**Q-1 已裁決採 B**（負責人，2026-11-18），先在 `spec-draft/` 試做 score-sheet。Q-2 的結論見第八節；其餘待裁決。本文件不是權威文件，也不改律法、不改程式。裁決後記為 D-061，再依裁決另開修法 PR。
> 起因：`docs/handoffs/2026-11-18-harness-coverage-gate.md` 第 D-1、G-1、H-1 項。
> 撰寫時的基準：`main` = `6c80640`。

## 一、問題

D-060 第 2 點把 `interfaces`（HTTP 契約）列為問題側，但規格格式 v1 第三節規則 6 禁止規格出現 HTTP 路徑與狀態碼（`specs/forbidden-vocabulary.txt`），執行器也只呼叫 use case（`specs/README.md` 第四節「執行」）。所以：

- `law/coverage.txt` 的 `interfaces/` 底下 14 行全部是 `grandfathered`，在現行格式下**永遠無法改成 `legislated`**。這違反 D-060 第 5 點「暫准範圍只減不增」的前提：它根本減不了。
- E-03（評分表列表與章節查詢回傳陣列，不符 D-050 第 7 點）這一類缺陷擋不到。score-sheet 已立法、74 個範例全過，缺陷仍然存在了 7 個月（`docs/harness/12-escaped-defects.md`）。

要裁決的是：HTTP 層的期望由什麼來涵蓋。

## 二、事實（逐項可核對）

| # | 事實 | 出處 |
|---|---|---|
| F-1 | 後端有 52 個端點（`@Get/Post/Put/Patch/DeleteMapping`），已立法的兩個 aggregate 佔 16 個（ReviewProject 9、ScoreSheet 7） | `interfaces/rest/*Controller.java`，2026-11-18 grep |
| F-2 | `api-spec.md` 1882 行，散文加 JSON 範例；2026-11-18 核對時路徑有 9 組不一致、8 個端點未記載，D-058 因此裁定「路徑以程式為準」 | `docs/review/api-spec-path-reconciliation-2026-11-18.md`、D-058 |
| F-3 | E-03 修正後已有建構者測試守住形狀（`ScoreSheetApiTest` 的 `jsonPath("$.data.items", ...)`），但建構者測試不是律法，建構者可以改 | #85；`09-law-inventory.md` 2026-11-14 裁決 |
| F-4 | 本 harness 的限制：不維護翻譯表、不建第 N 套 binding；多個律法來源並存本身就是翻譯表風險 | `docs/harness/README.md`「目標」；`03-harness-plan.md` 第 9～10 行 |
| F-5 | 執行器沒有對照表：use case 名稱直接對到 `XUseCase.execute` | `specs/README.md` 第四節 |
| F-6 | Spring Boot 2.7.18；目前沒有 OpenAPI 相關依賴；`build.gradle` 是律法（gate） | `build.gradle`、`law/manifest.txt` |
| F-7 | 錯誤代碼的「分類 → HTTP 狀態碼」由實作端的對應表決定（規則 7），這張表目前也不是律法 | `specs/README.md` 第三節規則 7 |

## 三、選項

### A：api-spec 的回應格式成為律法，加一個 controller 比對閘門

把 `api-spec.md`（或其中的 Request/Response 區段）列進 `law/manifest.txt`，並新增閘門：解析 api-spec 的 JSON 範例，比對 controller 的回傳型別與 DTO 欄位。

### B：另立機器可讀的 HTTP 契約，由閘門在執行期比對實際回應

新增一種規格檔（例如 `specs/<aggregate>/http.yaml`，`kind: http`），只寫「方法 + 路徑 → 請求欄位、回應形狀、錯誤分類對應的狀態碼」，**不寫規則、不寫權限**（這些仍在 `usecases.yaml`）。閘門以 MockMvc 呼叫每個端點，用契約驗證回應的 JSON 形狀。規則 6 改為「只適用於 `model`、`usecases`、`examples`」。

### C：把 `interfaces` 移出問題側

修訂 D-060 第 2 點：HTTP 層屬於解答側，由建構者測試與前端 e2e 涵蓋。依 `04-definition-of-enough.md` 第二節第三種回應方式，明文記錄「這類缺陷放在清單之外，改由什麼涵蓋」。

## 四、比較

| 面向 | A：api-spec 成為律法 | B：另立 HTTP 契約 + 執行期比對 | C：移出問題側 |
|---|---|---|---|
| **E-03 推演**：controller 回傳 `List<ScoreSheetSummary>`，契約要求 `{ items }` | 只有在閘門能從 JSON 範例推出「頂層是物件、有 `items`」並比對回傳型別時才擋得到。靜態比對碰到 `ResponseEntity<?>`、包裝外殼（`success`/`data`）、泛型就失準 → **部分擋得到** | MockMvc 取得 `data` 是陣列，契約要求物件 → **擋下**。與程式寫法無關 | **擋不到**。只靠建構者測試（F-3），建構者改測試就會一起漂移 |
| E-01（多開一個讀取端點，權限漏寫） | 擋不到（api-spec 有權限散文，但無法機械比對） | 新端點不在契約裡 → 閘門可以判「未記載的端點」而擋下；權限本身仍要靠 use case 規則 | 擋不到 |
| 與規格格式 v1 規則 6 的衝突 | 不衝突（api-spec 不在 `specs/`），但等於有兩個律法來源描述同一個 use case（權限、錯誤碼在 api-spec 與 `usecases.yaml` 都有）→ 違反 F-4 | 要修規則 6 的適用範圍。HTTP 詞彙被隔離在 `http.yaml`，`usecases.yaml` 保持純淨 | 不衝突 |
| 翻譯表風險（F-4、F-5） | 高：1882 行散文與程式、與 `specs/` 三方同步 | 中：`http.yaml` 只描述形狀，不連結 use case，不是 use case ↔ 端點的對照表。但仍是一份要維護的新檔 | 無 |
| 初始成本 | 高：先把 api-spec 與程式對齊（F-2 已知差異 17 項，且 request/response 欄位從未核對），再寫解析 markdown 的閘門 | 中：先只做已立法的 16 個端點（F-1）；閘門是驗收執行器的延伸（gate） | 低：一次修法（D-060、`coverage.txt` 刪除 `interfaces` 段） |
| 長期維護 | 每改一個欄位都要修法簽章，且 markdown 解析容易壞 | 每改回應形狀要修法簽章（這正是要的效果）；新增 aggregate 立法時一併寫 | 無 |
| 新依賴 | 無 | 若用 JSON Schema 驗證器要加測試依賴（改 `build.gradle` = 修法）；也可以不加依賴，自寫只比「形狀」的比對器 | 無 |
| 錯誤分類 → 狀態碼對應表（F-7） | 可順便納入 | 可順便納入（`http.yaml` 的錯誤區段） | 不納入 |
| 是否讓 `interfaces` 能變成 `legislated` | 能，但意義在 api-spec | 能，逐 aggregate 改 | 不需要（移出） |

## 五、建議：B，分兩步

1. **先在 score-sheet 試做**（E-03 的發生地），只寫 7 個端點的 `http.yaml` 草稿放在 `spec-draft/`，用 `--report-only` 跑；驗證「把 #85 之前的程式（回傳陣列）放回去時，閘門會紅」。這一步只動 `spec-draft/` 與閘門草稿，不改 `specs/` 的規則。
2. 試做通過後再提 D-061 修法：規則 6 改適用範圍、新增 `kind: http`、閘門進 CI、`coverage.txt` 把 score-sheet 的 controller 與 dto 改為 `legislated`。

不選 A 的主要理由：F-2 已經證明 api-spec 的散文會漂移，D-058 也已裁定路徑以程式為準；把它升為律法等於推翻 D-058，且造成第二個律法來源（F-4）。

C 是合理的備案：如果負責人判斷 HTTP 層的缺陷成本低（前端 e2e 會撞到），C 的成本最低，但要明文寫出「E-03 類缺陷由誰涵蓋」，並接受 F-3 的弱點。

## 六、需要負責人決定

| # | 問題 | 選項 |
|---|---|---|
| Q-1 | 採用哪一個選項 | A / B / C |
| Q-2 | （若 B）契約檔放在 `specs/<aggregate>/http.yaml`，還是獨立目錄 | 建議放在 aggregate 目錄，與規格一起升格 |
| Q-3 | （若 B）是否允許加 JSON Schema 驗證依賴 | 建議先不加，自寫只比形狀的比對器；不夠再修法加 |
| Q-4 | （若 B）錯誤分類 → 狀態碼對應表要不要一起立法 | 建議一起，否則 `REJECTED` 的 HTTP 狀態仍是暫准 |

## 七、未驗證事項

- B 的閘門尚未實作；「E-03 會被擋下」是依設計推演（INFERENCE），第五節第 1 步才會驗證。
- A 的靜態比對能力是推估，沒有試做。
- request 欄位與 query 參數在 api-spec 與程式之間的差異從未核對（F-2 只核對路徑）。

## 八、契約檔的範圍與位置（Q-2，2026-11-18 補）

負責人的方向：位置依契約檔的範圍決定。盤點結果：**契約有兩層，範圍不同，所以分兩處放**。

### 範圍盤點（基準 `3dde90b`）

| 內容 | 誰用 | 範圍 | 證據 |
|---|---|---|---|
| 回應外殼 `{ success, data, meta, error{code,message} }` | 全部 52 個端點 | **跨 aggregate** | `dto/ApiResponse.java`；api-spec §3.5 |
| 錯誤分類 → 狀態碼 | 全部 | **跨 aggregate** | `GlobalExceptionHandler.java`（依例外類別給 400/403/404/409/422）；規則 7 說這張表在實作端 |
| 共用錯誤代碼（`ROLE_DENIED`、`HOSPITAL_SCOPE_MISMATCH`、`VALIDATION_ERROR`…） | 兩個已立法規格都重複定義 | **跨 aggregate** | `specs/*/usecases.yaml` 的 `error_codes` |
| 操作者怎麼傳進來、base path、時間格式 | 全部 | **跨 aggregate** | `ActorContextArgumentResolver.java`；api-spec §3.1～3.4 |
| 端點、請求欄位、回應的 `data` 形狀 | 單一 use case | **aggregate 內** | score-sheet 7 支 use case 對 7 個端點，一對一 |
| `POST /review-projects/{id}/bind-score-sheet` | `BindScoreSheetToProject` | 屬於 **review-project**（use case 所在的規格），不是 URL 看起來的 score-sheet | `ReviewProjectController.java` L99；`specs/review-project/usecases.yaml` |
| 報表、稽核、送審查詢 | 跨多個 aggregate 的讀取 | 沒有單一 aggregate 擁有 | `ReportController`、`AuditController`、`SubmissionController`；目前全部暫准 |

### 位置

| 層 | 正式位置 | 試做位置 | 理由 |
|---|---|---|---|
| 共用層 | `specs/http-common.yaml`（`specs/` 根目錄的單一檔案） | `spec-draft/http-common.yaml` | 跨 aggregate 的東西只寫一次，否則每立法一個 aggregate 就多一份副本（F-4）。不能做成 `specs/http/` 子目錄：`spec_lint` 把 `specs/` 的每個子目錄當成 aggregate 檢查（`spec_lint.py` L362），會被擋下 |
| aggregate 層 | `specs/<aggregate>/http.yaml` | `spec-draft/score-sheet/http.yaml` | 立法、升格、`coverage.txt` 改 `legislated` 都是以 aggregate 為單位；契約跟著規格目錄一起升格 |
| 跨 aggregate 的讀取 | 這些查詢立法時自成一個規格目錄（例如 reporting），`http.yaml` 跟著它 | 不在本次試做範圍 | 沒有單一擁有者；硬塞進某個 aggregate 會讓那個 aggregate 的修法牽動別人 |

端點的歸屬規則：**端點屬於它呼叫的 use case 所在的規格目錄**，不看 URL 前綴。

立法後 `coverage.txt` 的對應：`ApiResponse`、`GlobalExceptionHandler`、`ActorContextArgumentResolver` 由共用層改為 `legislated`；各 controller 與它的 dto 隨 aggregate 改。

### 試做的已知副作用

- `spec-draft/score-sheet/` 只放 `http.yaml`，`spec-check` 的 `--report-only` 會列出「缺少 model.yaml」等 3 項。只列出、不擋；正式升格到 `specs/score-sheet/` 時不會發生。
- `spec-draft/` 是律法（`law/manifest.txt` 的 `spec` 類），試做 PR 本身就是修法 PR，需要修法紀錄與負責人簽章。

### 尚待決定：閘門怎麼取得「實際回應」（Q-5）

| | B-1：觀察 | B-2：以 HTTP 重跑範例 |
|---|---|---|
| 做法 | 測試期間攔截所有 HTTP 回應，逐一以契約驗證；契約裡每個端點至少要被觀察到一次成功，沒觀察到算未通過 | `http.yaml` 寫出 use case ↔ 端點、輸入 ↔ 路徑／query／body 的對應，執行器把 `examples.yaml` 改走 HTTP 再跑一遍 |
| 資料從哪裡來 | 建構者的 API 測試（非律法） | 律法的範例 |
| 是否是翻譯表（F-4、F-5） | 否：契約只描述端點與形狀 | **是**：每支 use case 一筆對應，score-sheet 的六相位更新要逐欄對應 |
| 擋得住 E-03 | 是（只要有任何測試打到該端點） | 是 |
| 弱點 | 建構者測試若只打錯誤路徑，成功形狀就沒被驗；以「至少一次成功」補 | 維護成本、與 README「不維護翻譯表」的目標衝突 |

建議 B-1。
