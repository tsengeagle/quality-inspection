# 規格格式 v1

本目錄是問題側的規格：系統要做什麼、在什麼條件下得到什麼結果。它是律法（`law/manifest.txt` 的 `spec` 類），修改需要修法紀錄與簽章。

格式由 `harness-core/spec_lint.py` 機械檢查，CI 的 `spec-check` 工作執行它。本文件與檢查程式不一致時，以檢查程式的行為為準，並修正本文件。

2026-11-15 依負責人裁決定案（`docs/harness/10-spec-format.md` 記錄各項決定與理由）。

## 一、現況

| aggregate | 位置 | 狀態 |
|---|---|---|
| `ReviewProject` | `specs/review-project/` | 2026-11-15 由 `spec-draft/` 升格（升格條件見 `docs/harness/03-harness-plan.md` 第二節）。42 條規則全部 `decided`，含評分表升格時加入的綁定（`BindScoreSheetToProject`，D-050 第 8 點）；36 個範例。沒有範例的規則：`RP-INV-04`（由抽選的 use case 執行）、`RP-BND-03`（範例拿不到未發布的版本 id）。沒有待裁決的題目 |
| `ScoreSheetDefinition` | `specs/score-sheet/` | 2026-11-15 由 `spec-draft/` 升格。38 條規則全部 `decided`（D-050）；36 個範例。程式已照規格修改（PR #41、#42），範例全部通過 |

尚未轉換的 aggregate 以 `docs/01-requirements` 到 `docs/04-testing` 為準。

## 二、目錄

每個 aggregate 一個目錄，名稱用小寫加連字號：

```
specs/
  README.md                    本文件
  forbidden-vocabulary.txt     規格裡不得出現的實作詞彙
  http-common.yaml             HTTP 契約的共用層（第七節，D-061）
  <aggregate>/
    model.yaml                 aggregate 模型：屬性、狀態、轉移、不變量
    usecases.yaml              use case 合約：誰能做、輸入、前置檢查、規則與結果
    examples.yaml              驗收範例：只透過 use case 準備資料、執行、觀察
    http.yaml                  HTTP 契約：端點與回應的形狀（第七節；有的 aggregate 才有）
    OPEN-QUESTIONS.md          待裁決的問題（有 proposed 規則時才需要）
```

檔案格式是 YAML。每個檔案開頭有 `spec_version: 1` 與 `kind`（`aggregate`、`usecases`、`examples`）。

## 三、規則

「規則」指任何帶 `id` 的項目：前置檢查（guard）、use case 的規則與成功結果、轉移、不變量。

1. **id 唯一且穩定。** 格式 `<前綴>-<分組>-<序號>`，例如 `RP-PUB-03`。整個 `specs/` 內不得重複。範例、測試、閘門都用 id 引用。id 一旦使用就不改號；規則廢止時刪除，不重用。
2. **每條規則有狀態。** `status` 只能是：
   - `decided`：有依據。必須有 `source`。
   - `proposed`：還沒裁決。必須有 `question`，指向同目錄 `OPEN-QUESTIONS.md` 裡的題號（`### Q-07` 之類的標題）。
3. **出處是結構化的。** `source` 是一個清單，每一項是下列其中一種，且必須指向實際存在的東西：

   | 寫法 | 指向 | 檢查 |
   |---|---|---|
   | `{ decision: D-040 }` | `decision-log.md` 的裁決 | 該檔有 `## D-040` 標題 |
   | `{ design_decision: DD-03 }` | `docs/02-ddd/` 的設計決策 | 某個檔案有 `## DD-03` 標題 |
   | `{ doc: docs/02-ddd/domain-model.md, section: "§5.1" }` | 文件與章節 | 檔案存在；章節不檢查 |
   | `{ example: EX-018 }`、`{ scenario: TS-PV-001 }` | `docs/04-testing/` 的範例與情境 | 該代號出現在 `docs/04-testing/` |
   | `{ rule: RP-INV-01 }` | 另一條規則 | 該 id 存在 |

   `specs/` 裡的規格只能引用 `specs/` 裡的規則；`--report-only` 的草稿可以引用兩者。

4. **每條規則只有一個結果。** `outcome.result` 是 `SUCCESS` 或 `REJECTED`。`REJECTED` 必須帶 `code`，而且 `code` 要定義在同一份 `usecases.yaml` 的 `error_codes`。不允許「A 或 B」。
5. **規則的順序就是檢查的順序。** use case 的 `guards` 依序列出前置檢查的 id，順序依 D-024：角色 → 載入 → 院區範圍 → 領域規則。
6. **不出現實作詞彙。** 類別名、方法名、框架型別、HTTP 路徑、狀態碼、資料表名都不行。禁用的寫法列在 `forbidden-vocabulary.txt`，檢查程式逐一比對規格裡的所有值（YAML 註解不檢查）。本規則只適用於 `model.yaml`、`usecases.yaml`、`examples.yaml`；HTTP 契約（`http.yaml`、`http-common.yaml`）用來寫 HTTP 路徑與狀態碼，見第七節（D-061）。
7. **錯誤代碼只寫分類，不寫 HTTP 狀態碼。** `error_codes` 每一項有 `code` 與 `category`，`category` 只能是 `denied`、`not_found`、`invalid_input`、`conflict`、`rule_violation`。分類對應到哪個 HTTP 狀態碼，由實作端的對應表決定。
8. **架構詞彙只用 `glossary.md` 有定義的詞。** 目前只人工檢查。

## 四、範例

1. **範例只透過 use case。** `given` 用其他 use case 準備資料，`when` 執行一支 use case，`then` 寫結果；要觀察狀態就用查詢 use case，不讀資料表。
2. **`basis` 列出這個範例驗證哪些規則**，必須是存在的 id（`specs/` 的範例只能引用 `specs/` 的規則）。只要其中一條是 `proposed`，這個範例就還不能當驗收依據。
3. **用到還沒納入規格的 use case，要列在 `needs`。** 檢查程式會比對：`given`、`when`、`observe` 裡執行的 use case 若不在 `usecases.yaml`，就必須在 `needs` 裡。
4. **跳過不算通過。** 執行器（第 4 期）遇到 `needs` 未滿足或 `input_pending` 的範例會跳過，並把它算成未通過。
5. **`fixtures` 是可重用的準備步驟**，用 `use: <名稱>` 引用，名稱必須存在。

### 執行（第 4 期）

範例由執行器直接執行，不由 AI 翻成測試程式。執行器在 `services/quality-inspection-service/src/test/java/.../acceptance/`，是律法（`gate`），每個範例是一個測試，名稱是 `<aggregate>/<範例名稱>`。

| 規格 | 執行器怎麼做 |
|---|---|
| use case 名稱 `X` | 呼叫類別 `XUseCase` 唯一一個公開的 `execute`。沒有對照表：程式要使用規格的名稱 |
| `as` | 用 `actors` 裡的 user、roles、hospital 建立操作者 |
| `input` | 依**參數名稱**對到 `execute` 的參數；沒給的參數是 null。規格的輸入名稱在 `execute` 找不到同名參數，範例未通過 |
| `then` 的 `REJECTED` | use case 丟出例外，而且例外的 `getErrorCode()` 等於 `code`。沒有錯誤代碼也算未通過 |
| `expect.output` | 依屬性名稱比對；只比列出的欄位；可以用 `${名稱}`；集合一對一配對、不比順序；數字依數值比對，`"0.50"` 等於 0.5 |
| 範例名稱 | 只能用英數字與 `_` `.` `-`：它是已知失敗清單裡的身分，不能被改寫 |
| `save` | 回傳值是單一值時直接存；是物件時取同名屬性 |
| 隔離 | 每個範例在一個交易裡執行，結束後還原 |
| 沒有範例 | `specs/` 底下找不到任何範例時，驗收測試失敗：沒有評估不能算通過 |
| `needs` | 列出的 use case 在 `specs/` 任何一份 `usecases.yaml` 裡都找不到，就是未滿足：不執行任何一步，算未通過 |
| `input_pending` | 不執行任何一步，算未通過 |
| `emits`、`no_change` | 不驗證，列在 `not_covered` |

未通過的範例由 `known-test-failures.txt` 的閘門核對：清單上的範例可以紅，其他的不行；清單上的範例變綠了，要從清單刪除（只刪行免修法）。新增的範例若一開始就紅，加進清單需要修法紀錄與簽章。

範例檔的其他欄位：

| 欄位 | 意思 |
|---|---|
| `actors` | 範例裡的操作者：使用者、角色、院區 |
| `save` / `${名稱}` | 把輸出存成變數，後面引用。系統產生的 id 無法事先知道 |
| `input_pending` | 這一步的輸入要等另一份規格完成才寫得出來 |
| `expect.output` | 部分比對：只比列出的欄位，集合不比順序 |
| `derived_from` | 轉換自哪個 `EX-` 或 `TS-`，供追溯 |
| `not_covered` | 目前無法用範例驗證的規則與原因 |

## 五、型別詞彙

| 型別 | 意思 |
|---|---|
| `id` | 不透明的識別字串。範例不能假設它的格式 |
| `text` | 字串。`required` 表示不可為 null，也不可為空白 |
| `enum <Name>` | 列舉，值定義在 `model.yaml` 的 `enums` |
| `set<T>` | 不重複、無順序的集合。空集合與 null 的差別要在屬性上寫明 |
| `decimal(n)` | 十進位數，小數 n 位。範例裡寫成字串，例如 `"0.50"` |
| `int`、`boolean` | 整數、布林 |
| `list<T>` | 有順序、可重複的清單。範例比對時不比順序（第四節「執行」） |
| `T`（`input_types` 的名稱） | 輸入裡的物件結構。在 `usecases.yaml` 的 `input_types` 用名稱定義欄位，輸入的型別直接寫名稱或 `list<名稱>`。檢查程式目前不驗證名稱是否有定義 |
| `ref <Aggregate>` | 指向另一個 aggregate 的 id。`external: true` 表示資料由外部系統提供 |

## 六、檢查程式擋什麼、只列出什麼

| 等級 | 項目 |
|---|---|
| 擋下 | 第三節規則 1 到 7、第四節規則 2、3、5，以及 YAML 讀不進來、`spec_version` 或 `kind` 不對 |
| 只列出 | 沒有任何範例涵蓋的規則；`basis` 含 `proposed` 規則的範例 |

「只列出」的兩項原定在第 4 期完成後改為擋下。執行器已完成（第四節「執行」），改為擋下另案處理：`RP-INV-04` 由抽選的 use case 執行，現在擋下會卡住。

## 七、HTTP 契約（D-061）

範例只執行到 use case 層，HTTP 的回應形狀要另外由契約涵蓋（漏網紀錄 E-03）。契約有兩層：

| 檔案 | `kind` | 內容 |
|---|---|---|
| `specs/http-common.yaml` | `http-common` | 跨 aggregate 的共用層：`base_path`、回應外殼 `envelope.success`／`envelope.error`、`status_by_category`（錯誤分類對應的狀態碼，規則 7）、`transport_codes`（不屬於任何 use case 的錯誤代碼與狀態碼） |
| `specs/<aggregate>/http.yaml` | `http` | `endpoints`：每一項有 `usecase`、`method`、`path`、`success.status`、`success.data` |

規則：

1. **端點屬於它呼叫的 use case 所在的規格目錄**，不看 URL 前綴。`usecase` 必須在同目錄的 `usecases.yaml`；同一個方法與路徑（路徑參數視為相同）只能出現一次。
2. **形狀的寫法。**
   - 對應表是封閉物件：列出的欄位都要有，沒列出的欄位不可以出現。
   - 只有一個元素的清單是陣列。
   - 純量型別：`id`、`text`、`int`、`decimal`、`boolean`、`enum <名稱>`（名稱在同目錄 `model.yaml` 的 `enums`）。結尾加 `?` 表示可以是 null，要加引號。
   - 字面值 `'null'`、`'true'`、`'false'` 要加引號。
   - `$data` 只能用在共用層的外殼，代表端點的 data 形狀。
3. **錯誤回應。** 錯誤代碼先查 `transport_codes`，再查該 aggregate `usecases.yaml` 的 `error_codes`，以分類對應狀態碼。兩邊都找不到的代碼算違反。
4. **比對（B-1 觀察）。** 測試期間經過 MockMvc 的每一個回應，都用契約比對；本文必須是單一 JSON 文件。比對器在 `services/quality-inspection-service/src/test/java/.../acceptance/http/`（律法，gate），以 `application-test.yml` 的 `context.initializer.classes` 掛進每個測試 context。
5. **涵蓋。** 每個端點至少觀察到一次成功回應；data 形狀裡的每個陣列至少觀察到一次非空；data 形狀裡可為 null 的欄位，null 與非 null 都要觀察到。共用層的外殼不寫可為 null 的欄位（錯誤回應的 `message` 一律要有值）。
6. **閘門。** 報告寫在 `build/reports/http-contract/report.txt`，`build.gradle` 的 test 閘門讀它判決：
   - `specs/` 的契約有問題就未通過。
   - 完整執行時，報告必須是本次執行產生的。
   - 有篩選條件時只判每一筆回應，涵蓋不判。
   - `spec-draft/` 裡的契約只列出、不擋。
7. **檢查。** `spec_lint` 不檢查這兩種檔案，由 `HttpContractFilesTest` 檢查它們讀得進來、沒有載入問題。
8. **不涵蓋的。** 請求端（請求本文、query、參數名稱）、操作者怎麼傳進來、值的正確性（由 use case 範例負責）。

| aggregate | `http.yaml` | 端點 |
|---|---|---|
| `ScoreSheetDefinition` | `specs/score-sheet/http.yaml` | 7 |
| `ReviewProject` | `specs/review-project/http.yaml` | 7（列表與版本查詢的 use case 不在規格內，沒有寫進契約） |
