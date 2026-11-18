# D-061 草案：HTTP 契約升格為律法（待裁決）

> 狀態：**負責人 2026-11-18 已裁決 Q-1～Q-6，全部採建議選項（A）**，見第六節。尚未寫入 `decision-log.md`；實作順序第 2 步的修法 PR 會把條文寫進去。`decision-log.md` 第 2 節第 1 條規定只記錄已決定的事項，所以裁決前的草案寫在這裡。
> 依據：`docs/harness/13-http-contract-options.md`（選項比較、範圍與位置、score-sheet 試做結果）；#93、#95。
> 撰寫時的基準：`main` = `168ff7d`。

## 一、要決定的事

試做（#93、#95）證明 B-1 觀察能抓到 E-03 這一類缺陷，但現在只列出、不擋，`interfaces/` 仍全部是暫准。D-061 要決定三件事：

1. 契約升格到 `specs/` 之後，格式規則怎麼改。
2. 閘門怎麼擋。
3. 哪些程式從暫准改成已立法。

## 二、草案條文

以下是寫入 `decision-log.md` 時的條文草稿。【Q-x】標出要負責人裁決的地方，第三節列出選項與建議。

### D-061 — HTTP 契約升格為律法：觀察式比對與閘門

**Decision**

1. **契約的位置。** 共用層放在 `specs/http-common.yaml`，aggregate 層放在 `specs/<aggregate>/http.yaml`。端點屬於它呼叫的 use case 所在的規格目錄。試做檔案（`spec-draft/http-common.yaml`、`spec-draft/score-sheet/http.yaml`）在升格時刪除。
2. **格式。** 規格格式 v1 第三節規則 6（不出現實作詞彙）只適用於 `model.yaml`、`usecases.yaml`、`examples.yaml`。`http.yaml` 與 `http-common.yaml` 用來寫 HTTP 契約，可以出現路徑與狀態碼。這兩種檔案的格式寫在 `specs/README.md` 新增的一節，由 `acceptance/http/` 的 `HttpContractFilesTest` 檢查，不重複寫進 `spec_lint`（理由見 Q-9）。
3. **比對方式（B-1 觀察）。** 測試期間經過 MockMvc 的每一個回應，都用契約比對：外殼、成功狀態碼、data 的形狀（物件是封閉的）、錯誤代碼的分類對應的狀態碼。涵蓋的要求：
   - 每個端點至少觀察到一次成功回應。
   - 每個陣列至少觀察到一次非空。
   - 【Q-4】可為 null 的欄位，null 與非 null 都要觀察到。
4. **閘門。** 以下任一項成立，`check` 就未通過：
   - `specs/` 的契約有任何問題（載入問題、回應不符、涵蓋不足）。
   - 本次執行沒有產生報告：報告必須帶有 Gradle 這次給的執行識別碼，所以觀察器被關掉、或只留下舊報告，都算未通過。
   - 只有完整執行時才判涵蓋；有篩選條件時，只判每一筆回應有沒有不符。
   - `spec-draft/` 的契約維持只列出、不擋。
   - `build.gradle` 把 `specs/` 與 `spec-draft/` 都宣告為 test 的輸入。
5. **涵蓋清單。** 【Q-2】由暫准改為已立法的範圍：
   - 共用層：`dto/ApiResponse.java`、`GlobalExceptionHandler.java`。
   - score-sheet 的回應 dto：`ScoreSheetListResponse`、`ScoreSheetSummaryResponse`、`ScoreSheetSectionsResponse`、`ScoreSheetSectionResponse`、`ScoreSheetItemResponse`。
   - 【Q-3】`ScoreSheetController` 與請求 dto 視請求端是否驗證而定。
   - `ActorContextArgumentResolver`、`CurrentActor` **不列入**：契約沒有規定操作者怎麼傳進來（更正 `13` 第八節）。
6. **範圍。** 【Q-1】先做 score-sheet，還是連 review-project 一起。
7. **401 錯誤代碼。** 【Q-5】
8. **暫准只減不增**（D-060 第 5 點）。之後每立法一個 aggregate，就同時寫它的 `http.yaml`，並把 controller 與 dto 改為已立法。

**Relationship / Evidence**

- E-03（`12-escaped-defects.md`）：score-sheet 已立法，範例全過，回應形狀卻不符 D-050 第 7 點長達 7 個月。
- 試做的驗證（`13` 第九節）：
  - 把 #85 之前的寫法放回去，列出 6 個問題。
  - 不打 HTTP 時，列出 7 個端點都沒有成功觀察。
  - 現行程式 71 次觀察，問題 0。
  - 全套 984 個方法，0 失敗。
- 擋不到的範圍：W-5（不經過 MockMvc 的呼叫，判為沒有觀察）、W-8（值的正確性，由 use case 範例負責），以及第三節沒有選做的 W-7、W-9、W-10。

## 三、待裁決

### Q-1 這次的範圍

| 選項 | 內容 | 代價 |
|---|---|---|
| **A（建議）** | 只做 score-sheet，review-project 另開 PR，在同一條 D-061 下補上 | 試做只驗證過 score-sheet；一次一個 aggregate，修法 PR 比較小 |
| B | score-sheet 與 review-project 一起做 | review-project 有 9 個端點，綁定評分表也在其中；一次立法較完整，但新契約沒有試做過 |

### Q-2 共用層在涵蓋清單裡指向哪裡

`law_check` 目前要求 `legislated` 指向 base 上的**目錄**（`harness-core/law_check.py` 的 `ls-tree -d`）。共用層契約是單一檔案。

| 選項 | 內容 |
|---|---|
| **A（建議）** | `law_check` 改為接受檔案或目錄（gate 修改，測試先寫），共用層指向 `specs/http-common.yaml` |
| B | 指向 `specs/`。不必改判決程式，但「哪份規格涵蓋它」變得不精確 |

### Q-3 請求端（W-9）

B-1 只看回應。controller 也負責把請求對應到 use case，請求 dto 的欄位名稱是 HTTP 契約的一部分，目前沒有律法涵蓋。

| 選項 | 內容 | 涵蓋清單 |
|---|---|---|
| **A（建議）** | 這次不做請求端；controller 與請求 dto 維持暫准，另立題目 | 回應 dto 與共用層改已立法 |
| B | 這次一起做：`http.yaml` 加請求形狀，觀察器也比對請求本文 | controller 與請求 dto 也改已立法 |

理由：請求本文是建構者的測試送出去的，比對它只能證明「測試照契約送」，不能證明「伺服器照契約收」。要證明後者，需要律法擁有的請求（W-10），所以 W-9 和 W-10 應該一起設計。

### Q-4 可為 null 的欄位（W-7）

| 選項 | 內容 |
|---|---|
| **A（建議）** | 這次做：`?` 欄位 null 與非 null 都要觀察到。實作小，與「陣列至少一次非空」同一種機制 |
| B | 不做 |

做了之後，現有測試要能讓 `scoreSheetVersionId` 兩種都出現；試做報告只觀察到 `GET .../version` 成功 1 次，**可能不夠，要補建構者測試**。這是 INFERENCE，實作時先跑一次確認。

### Q-5 401 錯誤代碼

| 事實 | 出處 |
|---|---|
| D-023 Consequences：「`UNAUTHORIZED`（401）維持原語意（未登入 / 憑證失效）」 | `decision-log.md` D-023 |
| api-spec §4.1：`UNAUTHORIZED` 401 | `api-spec.md` |
| 程式回 `MISSING_API_KEY`、`INVALID_TOKEN`（401） | `infrastructure/security/ApiKeyAuthFilter.java` |
| 前端沒有引用這三個代碼 | `frontends/quality-inspection-frontend/src` grep 0 筆 |

| 選項 | 內容 |
|---|---|
| **A（建議）** | 程式照 D-023 改回 `UNAUTHORIZED`；缺少與無效的差別寫在 `message`。`ApiKeyAuthFilter` 在 `infrastructure`，不是問題側，走建構 PR |
| B | 契約照程式：`transport_codes` 列 `MISSING_API_KEY`、`INVALID_TOKEN`；修訂 D-023 與 api-spec §4.1 |

### Q-6 閘門的時點

| 選項 | 內容 |
|---|---|
| **A（建議）** | D-061 生效時 `specs/` 的契約立刻擋 |
| B | 先觀察一段時間再擋 |

理由：試做在現行程式上問題 0，沒有已知失敗要凍結。

## 四、裁決後的實作順序

每一步都先寫測試，先紅後綠；律法修改需要修法紀錄與簽章。

| # | PR | 類別 | 內容 | 驗證 |
|---|---|---|---|---|
| 1 | 建構 PR（Q-5 選 A 時） | 建構 | `ApiKeyAuthFilter` 改回 `UNAUTHORIZED` | 先紅：新增的 401 測試；全套 |
| 2 | 修法 PR | ruling、spec、gate | D-061 寫入 `decision-log.md`；契約搬到 `specs/`；`specs/README.md` 新增一節；閘門（Gradle、報告的執行識別碼、強制模式）；`law_check` 接受檔案（Q-2 選 A 時）；`coverage.txt`；W-7（Q-4 選 A 時） | 先紅：閘門的測試；mutation：放回陣列回應時 `check` 失敗；刪掉觀察器設定時 `check` 失敗；全套 |
| 3 | 修法 PR（Q-1 選 A 時） | spec | review-project 的 `http.yaml` | 同上 |

第 1 步要在第 2 步之前：閘門生效後，任何測試打到 401 都會被判為錯誤代碼沒有定義。

## 五、未驗證

- Q-4 的「現有測試是否同時觀察到 null 與非 null」沒有跑過。
- Gradle 怎麼把執行識別碼傳給報告、`doLast` 怎麼讀，沒有試做過。設計上與既有的 `known-test-failures.txt` 閘門相同，都寫在 `test` 任務的 `doLast`。
- review-project 的契約沒有試做過。

## 六、裁決紀錄（2026-11-18，負責人）

| 題目 | 裁決 |
|---|---|
| Q-1 範圍 | A：只做 score-sheet；review-project 另開修法 PR，在同一條 D-061 下補上 |
| Q-2 共用層的指向 | A：`law_check` 改為接受檔案，共用層指向 `specs/http-common.yaml` |
| Q-3 請求端（W-9） | A：這次不做；controller 與請求 dto 維持暫准，之後與 W-10 一起設計 |
| Q-4 可為 null 的欄位（W-7） | A：這次做 |
| Q-5 401 錯誤代碼 | A：程式照 D-023 改回 `UNAUTHORIZED` |
| Q-6 閘門的時點 | A：生效時立刻擋 |

### 第 1 步（Q-5）：已實作，建構 PR

- `infrastructure/security/ApiKeyAuthFilter.java`：缺少與無效的 api-key 都回 `UNAUTHORIZED`（401），差別只寫在 `message`。`infrastructure` 不是問題側（D-060 第 2 點）。
- 方法簽章沒有改：改了的話，既有的 ARCH-01 凍結違規描述會跟著變，被當成新違規（第一次全套執行時 `arch01LayerDependencies` 失敗，已改回原簽章）。凍結基線沒有改。
- 測試先寫：`infrastructure/security/ApiKeyAuthFilterApiTest`（2 個）。修正前 2 個都失敗（`expected:<UNAUTHORIZED> but was:<MISSING_API_KEY>`、`<INVALID_TOKEN>`），HTTP 契約試做的報告也同時列出這兩個代碼「沒有定義」。
- 修正後，後端全套 986 個方法、1252 次執行，0 失敗。HTTP 契約報告：契約範圍內 73 次觀察（含 2 次 401），問題 0。
