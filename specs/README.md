# 規格格式 v1、v2.0

本目錄是問題側的規格：系統要做什麼、在什麼條件下得到什麼結果。它是律法（`law/manifest.txt` 的 `spec` 類），修改需要修法紀錄與簽章。

格式由 `harness-core/spec_lint.py` 機械檢查，CI 的 `spec-check` 工作執行它。本文件與檢查程式不一致時，以檢查程式的行為為準，並修正本文件。

2026-11-15 依負責人裁決定案（`docs/harness/10-spec-format.md` 記錄各項決定與理由）。

第一到九節是格式 v1（`spec_version: 1`）。格式 v2.0（`spec_version: 2`，D-065）在第十節，只寫它和 v1 不同的地方。執行器兩個版本都能執行；版本 2 怎麼執行，見第十節最後的「執行（版本 2）」。

## 一、現況

| aggregate | 位置 | 狀態 |
|---|---|---|
| `ReviewProject` | `specs/review-project/` | 2026-11-15 由 `spec-draft/` 升格（升格條件見 `docs/harness/03-harness-plan.md` 第二節）。44 條規則全部 `decided`，含評分表升格時加入的綁定（`BindScoreSheetToProject`，D-050 第 8 點）與列表、版本查詢（D-062）；46 個範例；D-062 的 4 個已由 #104 修正，範例全部通過。沒有範例的規則：`RP-INV-04`（由抽選的 use case 執行）、`RP-BND-03`（範例拿不到未發布的版本 id）。沒有待裁決的題目 |
| `ScoreSheetDefinition` | `specs/score-sheet/` | 2026-11-15 由 `spec-draft/` 升格。38 條規則全部 `decided`（D-050）；36 個範例。程式已照規格修改（PR #41、#42），範例全部通過 |
| `SamplingBatch` | `specs/sampling-batch/` | 2026-11-21 立法（D-066，格式 v2.0）：從抽選到初審提交的直向切片，只有 `ExecuteSampling`。9 條規則全部 `decided`（Q-06 由 D-067 裁決，`SB-EXE-S5` 由 D-068 新增）；7 個範例，1 個是紅的（D-068 第 3 點），列在已知失敗清單，等建構 PR |
| `ReviewCase` | `specs/review-case/` | 2026-11-21 立法（D-066，格式 v2.0）：列出、指派、開始初審、保存草稿、提交初審、revision 歷程。2026-11-22 加入複審切片（D-069）：固定初審、送複審、指派複審醫師、開始複審、保存複審草稿、提交複審、複審結案。100 條規則全部 `decided`（複審事件的欄位 Q-08 由 D-070 裁決）。2026-11-22 加入稽核的兩個出口（D-071）：生命週期 `GetCaseLifecycle`、稽核軌跡 `ListAuditTrails`，帶分數的 `detail` 依 D-055 第 6 點遮罩，共 107 條規則；85 個範例（含矩陣展開），D-071 的 9 個是紅的，列在已知失敗清單，等建構 PR。改派、修正、不經複審結案、取消、退回還沒有立法（`model.yaml` 的 `deferred_transitions`） |
| `ReviewSubmission` | `specs/review-submission/` | 2026-11-21 立法（D-066，格式 v2.0）：只有狀態機，改變它的 use case 在 `review-case/`。2026-11-22 加入複審的保存草稿與提交（D-069），共 4 條轉移 |

尚未轉換的 aggregate 以 `docs/01-requirements` 到 `docs/04-testing` 為準。

## 二、目錄

每個 aggregate 一個目錄，名稱用小寫加連字號：

```
specs/
  README.md                    本文件
  forbidden-vocabulary.txt     規格裡不得出現的實作詞彙
  spec-profile.yaml            格式裡屬於本專案的部分：操作者、錯誤分類、guard 順序（第九節，D-064）
  http-common.yaml             HTTP 契約的共用層（第七節，D-061）
  <aggregate>/
    model.yaml                 aggregate 模型：屬性、狀態、轉移、不變量
    usecases.yaml              use case 合約：誰能做、輸入、前置檢查、規則與結果
    examples.yaml              驗收範例：只透過 use case 準備資料、執行、觀察
    http.yaml                  HTTP 契約：端點與回應的形狀（第七節；有的 aggregate 才有）
    OPEN-QUESTIONS.md          待裁決的問題（有 proposed 規則時才需要）
```

檔案格式是 YAML。每個檔案開頭有 `spec_version`（`1` 或 `2`，同一個目錄必須相同）與 `kind`（`aggregate`、`usecases`、`examples`）。

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
5. **規則的順序就是檢查的順序。** use case 的 `guards` 依序列出前置檢查的 id。順序的依據寫在 `spec-profile.yaml` 的 `guard_order`（第九節）；本專案依 D-024：角色 → 載入 → 院區範圍 → 領域規則。檢查程式不檢查順序。
6. **不出現實作詞彙。** 類別名、方法名、框架型別、HTTP 路徑、狀態碼、資料表名都不行。禁用的寫法列在 `forbidden-vocabulary.txt`，檢查程式逐一比對規格裡的所有值（YAML 註解不檢查）。本規則只適用於 `model.yaml`、`usecases.yaml`、`examples.yaml`；HTTP 契約（`http.yaml`、`http-common.yaml`）用來寫 HTTP 路徑與狀態碼，見第七節（D-061）。
7. **錯誤代碼只寫分類，不寫 HTTP 狀態碼。** `error_codes` 每一項有 `code` 與 `category`，`category` 只能是 `spec-profile.yaml` 的 `error_categories` 列出的值（第九節）。本專案是 `denied`、`not_found`、`invalid_input`、`conflict`、`rule_violation`。
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
| `emits`、`no_change` | 版本 1 不驗證，列在 `not_covered`。版本 2 的 `then.emits` 會驗證，見第十節 |

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
| `datetime` | 一個時間點。範例裡寫 ISO 8601 加時區。只有 `spec_version: 2` 的規格能在範例裡比對它（第十節） |

## 六、檢查程式擋什麼、只列出什麼

| 等級 | 項目 |
|---|---|
| 擋下 | 第三節規則 1 到 7、第四節規則 2、3、5，以及 YAML 讀不進來、`spec_version` 或 `kind` 不對；第八節的封閉結構、分辨不了情境的範例、引用完整性（D-063） |
| 無法評估（不算通過） | `spec-profile.yaml` 不存在、讀不進來、缺少任何一項或內容不合法（D-064）；`forbidden-vocabulary.txt` 不存在 |
| 擋下（`spec_version: 2`，D-065） | 第十節的各項：斷言與路徑的寫法、沒有時區的時間、矩陣、事件沒有宣告、沒有範例的轉移、跨目錄的引用、埠 |
| 無法評估（D-065） | 有 `spec_version: 2` 的規格，而 `spec-profile.yaml` 是 `profile_version: 1` |
| 只列出（D-063） | `given`、`when`、`then`、`observe` 完全相同的兩個範例：可能是刻意的重複 |
| 只列出（`spec_version: 2`，D-065） | `given`、`when` 相同、`result` 與 `code` 也相同，但其餘斷言不同的兩個範例；斷言了 `proposed` 事件的範例 |
| 只列出 | 沒有任何範例涵蓋的規則；`basis` 含 `proposed` 規則的範例 |

「只列出」的兩項原定在第 4 期完成後改為擋下。執行器已完成（第四節「執行」），改為擋下另案處理：`RP-INV-04` 由抽選的 use case 執行，現在擋下會卡住。

## 七、HTTP 契約（D-061）

範例只執行到 use case 層，HTTP 的回應形狀要另外由契約涵蓋（漏網紀錄 E-03）。契約有兩層：

| 檔案 | `kind` | 內容 |
|---|---|---|
| `specs/http-common.yaml` | `http-common` | 跨 aggregate 的共用層：`base_path`、回應外殼 `envelope.success`／`envelope.error`、`status_by_category`（錯誤分類對應的狀態碼：規格只寫分類，規則 7；分類對到哪個 HTTP 狀態碼由這裡決定）、`transport_codes`（不屬於任何 use case 的錯誤代碼與狀態碼） |
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
| `ReviewProject` | `specs/review-project/http.yaml` | 9（含列表與版本查詢，D-062） |

## 八、欄位表（D-063，v1.1）

2026-11-20 起，三種檔案都是封閉結構：每一層只允許下表列出的欄位，型別也要相符，否則擋下。檔案仍寫 `spec_version: 1`。

- 任何一層對應表都可以有 `note`。它是自由文字，不參與判決。
- 欄位表內建在 `harness-core/spec_lint.py` 的 `FILE_SCHEMAS`。本節是同一份內容，給人看；兩者不一致時以檢查程式為準（本文件開頭）。
- 下表只管欄位與型別。必填、引用、規則的格式由第三、四節負責。
- 沒寫的欄位不檢查；寫了但值是 null 是問題。只有標「任意值」的欄位可以是 null。
- 標「待改寫」的四項照樣接受，在下一次規格格式的修法中改寫（D-063 第 3 點）。

**寫法說明**：

- 「規則欄位」指 `status`、`source`、`question`（第三節規則 2、3）。
- `source` 的每一項可以有 `decision`、`design_decision`、`doc`、`section`、`example`、`scenario`、`rule`。
- 「名稱 → X」表示鍵是自由的名稱，值依 X 檢查。

### `model.yaml`

| 欄位 | 型別 |
|---|---|
| `spec_version`、`kind`、`name`、`term` | 整數、字串 |
| `identity` | `name`、`type`、`assigned_by` |
| `sources` | `source` 項目的清單 |
| `attributes[]` | 屬性（見下） |
| `entities[]` | `name`、`term`、`attributes`（屬性的清單） |
| `enums[]` | `name`、`values`（字串清單）、規則欄位 |
| `transitions[]` | `id`、`from`、`to`、`by`（都是字串）、規則欄位 |
| `terminal_states` | 字串清單 |
| `invariants[]` | `id`、`text`、`enforced_by`（待改寫：併入 `note`）、規則欄位 |
| `deferred_attributes`、`deferred_behaviors` | 字串清單：文件列出、但本次不處理的屬性與行為 |

屬性：`name`、`type`、`required`（布林）、`initial`（任意值）、`assigned_by`、`mutable`（布林）、`external`（布林）、`range`（整數清單；待改寫：改成有 id 的規則）、規則欄位。

### `usecases.yaml`

| 欄位 | 型別 |
|---|---|
| `spec_version`、`kind`、`aggregate` | 整數、字串 |
| `sources` | `source` 項目的清單 |
| `error_codes[]` | `code`、`category`、規則欄位 |
| `guards[]` | `id`、`when`、`outcome`、規則欄位 |
| `input_types` | 名稱 → 輸入欄位的清單 |
| `usecases[]` | use case（見下） |

use case：

| 欄位 | 型別 |
|---|---|
| `name`、`summary` | 字串 |
| `kind` | `command` 或 `query` |
| `actor` | 欄位依 `spec-profile.yaml` 的 `usecase_actor_fields`（第九節）；本專案是 `roles`（字串清單） |
| `input[]` | 輸入欄位：`name`、`type`、`required`（布林）、規則欄位 |
| `input_note` | 字串（待改寫：併入 `note`） |
| `guards` | 字串清單 |
| `guard_exceptions[]` | `guard`、`text`、規則欄位 |
| `rules[]` | `id`、`when`、`text`、`outcome`、規則欄位 |
| `success` | `state[]`（`id`、`text`、規則欄位）、`emits[]`（`event`、規則欄位）、`output[]`（`name`、`type`、規則欄位；待改寫） |
| `output` | `fields[]`（`name`、`type`）、規則欄位（待改寫：與 `success.output` 合併成一種寫法） |

`outcome`：`result`、`code`、`no_change`（布林）。

### `examples.yaml`

| 欄位 | 型別 |
|---|---|
| `spec_version`、`kind`、`aggregate` | 整數、字串 |
| `actors` | 名稱 → 欄位依 `spec-profile.yaml` 的 `actor_fields`（第九節）；本專案是 `user`、`roles`（字串清單）、`hospital` |
| `fixtures` | 名稱 → `steps`（步驟的清單）、`needs`（字串清單） |
| `examples[]` | `name`、`basis`、`derived_from`、`needs`（字串清單）、`given`（步驟的清單）、`when`（步驟）、`then`、`observe`（觀察步驟的清單） |
| `not_covered[]` | `rules`（字串清單）、`item`、`reason` |

- 步驟：`run`、`as`、`input`（名稱 → 任意值）、`save`（名稱 → 字串）、`use`、`input_pending`。
- 觀察步驟：步驟的欄位，加上 `expect`：`result`、`code`、`output`（名稱 → 任意值）。
- `then`：`result`（`SUCCESS` 或 `REJECTED`）、`code`。執行器只比對這兩個（第四節「執行」）。
- `not_covered[].rules` 應該只放規則 id；review-project 的 `rules: [emits]` 待改寫成 `item: emits`，改寫後才核對（D-063 第 5 點）。

注意：YAML 會把 `on`、`yes`、`no` 之類的鍵讀成布林。它們會被判為不認得的欄位，訊息裡會說明原因。

### 其他 v1.1 的檢查

1. **分辨不了情境的範例。**
   - 兩個範例的 `given`（`use` 依 fixture 展開後）與 `when` 完全相同，`then` 卻不同，擋下：兩者不可能同時通過。
   - 連 `then`、`observe` 都相同，只列出。
   - 比較時忽略 `note`。`${名稱}` 依字面比較：變數名稱不同、實質相同的兩個範例，檢查不出來。
2. **引用完整性。**
   - `guard_exceptions[].guard` 必須是同一支 use case `guards` 裡的 id。
   - `transitions[].by` 必須是同目錄 `usecases.yaml` 的 use case；`from`、`to` 都要有。
   - 轉移的狀態與 `terminal_states` 必須都是同一個列舉的值；有轉移或 `terminal_states`，就必須有 `enums`。

## 九、專案設定檔（D-064，v1.2）

格式裡屬於專案的部分寫在 `specs/spec-profile.yaml`，`harness-core/spec_lint.py` 從規格目錄讀它，不寫死在程式裡。安裝 `harness-core/` 的其他專案（例如 iccard）寫自己的設定檔。檔案不存在、或缺少下表任何一項，判為無法評估，不使用預設值。

| 欄位 | 型別 | 用途 |
|---|---|---|
| `kind` | 固定 `spec-profile` | |
| `profile_version` | `1` 或 `2` | `2` 多了 `clock_start`、`local_time_zone`。有 `spec_version: 2` 的規格時必須是 `2`（D-065） |
| `actor_fields` | 名稱 → `str` 或 `str_list` | `examples.yaml` 的 `actors` 每一項允許的欄位。沒列的欄位照樣擋下 |
| `usecase_actor_fields` | 名稱 → `str` 或 `str_list` | `usecases.yaml` 的 `actor` 允許的欄位 |
| `error_categories` | 字串清單，不可重複 | `error_codes[].category` 允許的值 |
| `guard_order` | `text`（字串）、`source`（`decision` 的清單） | guard 順序的說明（第三節規則 5）。只給人看，不檢查順序；`source` 的裁決要存在於決策紀錄 |

| `clock_start` | 帶時區的 ISO 8601 時間 | 只有 `profile_version: 2`，必填。沒有寫 `clock` 的範例從這個時間開始 |
| `local_time_zone` | `UTC` 或「地區/城市」 | 只有 `profile_version: 2`，必填。不帶時區的時間依這個時區解讀；執行器提供的時鐘也用這個時區。本專案是 `Asia/Taipei` |

`actor_fields`、`usecase_actor_fields`、`error_categories` 不可以是空的。設定檔也是封閉結構，不認得的欄位判為內容不合法。

## 十、格式 v2.0（D-065）

寫 `spec_version: 2` 的目錄適用本節；沒有提到的部分與第一到九節相同。`spec_version: 1` 的目錄不受影響。

理由與試寫的經過見 `docs/harness/15-spec-format-v2-draft.md` 第四、十三節。

檢查程式（`spec_lint`）與執行器都支援本節。執行器怎麼做，見本節最後的「執行（版本 2）」。

### 檔案

- 版本 2 的目錄只有 `model.yaml` 是必要的。一個 aggregate 可以只有狀態機，改變它的 use case 定義在別的目錄。
- 需要 `spec-profile.yaml` 是 `profile_version: 2`（第九節）。

### 欄位表的差別

| 檔案 | 加入 | 拿掉 |
|---|---|---|
| `model.yaml` | `transitions[].when`、`deferred_transitions[]`（`from`、`to`、`by`、`when`）、`ports[]`（`name`、`term`、`operations[]`：`name`、`returns`） | `attributes[].range`、`invariants[].enforced_by` |
| `usecases.yaml` | `output_types`（名稱 → `name`、`type` 的清單）。命令與查詢的輸出都寫 `output.fields` | `input_note`、`success.output` |
| `examples.yaml` | `expectations`（名稱 → 斷言）；範例的 `clock`（`start`）、`matrix`（`columns`、`key`、`rows`）；步驟的 `advance`、`port`、`returns`；`then` 與 `expect` 的斷言欄位（見下） | |

`not_covered[].rules` 只能放規則 id。

### 斷言

`then`、`observe` 的 `expect`、具名斷言用同一組欄位：

| 欄位 | 意思 |
|---|---|
| `result`、`code` | 與 v1 相同。`then.code` 必須定義在 `when` 那支 use case 所在目錄的 `error_codes` |
| `output` | 部分比對。鍵可以是路徑 |
| `absent` | 路徑的清單。選到的屬性必須不存在或為 null |
| `only` | 路徑 → 欄位清單。選到的每一個物件，值不是 null 的屬性都必須在清單裡 |
| `use_expect` | 指向 `expectations` 裡的具名斷言。具名斷言可以包含 `result`、`code`；不能再引用具名斷言 |
| `emits`、`emits_include` | 只能用在 `then`，見「事件」 |

- `then` 比對的是 `when` 的輸出。
- `result` 是 `REJECTED` 時只能有 `code`。

路徑：

| 寫法 | 選到什麼 | 可以用在 |
|---|---|---|
| `.` | 輸出本身 | `only` |
| `a.b` | 巢狀屬性 | 全部 |
| `a[*]` | 清單的每一筆 | `output` 的鍵、`absent`、`only` |
| `a[k=v]` | `k` 等於 `v` 的每一筆。多個條件用逗號；`v` 可以是 `${名稱}` | `output` 的鍵、`absent`、`only` |
| `a[0]` | 第一筆。範例不得依賴它是哪一筆 | `save` |

- `[*]`、`[k=v]` 選不到任何一筆時，斷言不成立。
- `output` 裡直接寫清單，仍然是筆數相同、一對一配對。只想比對其中幾筆時，用路徑當鍵。

### 時間

- 範例裡的時間寫 ISO 8601 加時區。看起來像時間卻沒有時區的字串是問題。比對的是時間點。
- 範例的 `clock: { start }` 指定起始時間；不寫就用設定檔的 `clock_start`。
- 步驟 `advance: PT1H` 讓時間前進（ISO 8601 期間）。

### 矩陣

- `matrix.columns` 是欄名，`matrix.rows` 的每一列是一個範例，欄的值代入範例裡的 `${欄名}`。
- `matrix.key` 必填，指定哪幾欄組成名稱。展開後的名稱是 `<name>.<值>.<值>`。
- `key` 那幾欄的值只能用英數字與 `_` `-`，組合不得重複。
- 欄名不得與 `save` 的變數同名（準備步驟存的變數也算）。
- 範例名稱在同一個檔案裡不得重複。

### 事件

- `then.emits` 是完整的清單，不比順序；`then.emits_include` 是部分比對。每一項是 `event` 與 `with`（依屬性名稱部分比對）。
- 事件必須在 `when` 那支 use case 的 `success.emits` 有宣告。事件 `X` 對到類別 `XEvent`。
- `result` 是 `REJECTED` 時，執行器一律確認沒有發出任何事件；範例不必寫，也不能寫。
- 斷言了 `proposed` 事件的範例，還不能當驗收依據。

### 轉移

- `transitions` 的每一條都必須至少被一個範例的 `basis` 引用，否則擋下。
- 轉移可以有 `when`。同一個 `from`、同一支 use case 的多條轉移，`when` 必須不同。`from` 可以等於 `to`。
- `deferred_transitions` 列出存在、但還沒立法的轉移。不需要 id，不需要範例。`from`、`to` 要是列舉的值；`by` 已經在規格裡的不能留在這裡。
- 轉移、`deferred_transitions`、`terminal_states` 的狀態必須都屬於同一個列舉。

### 跨目錄的引用

| 引用 | 寫法 |
|---|---|
| fixture | `use: <目錄>.<名稱>`。變數一起帶過來 |
| use case | `run: <目錄>.<名稱>`；同目錄的直接寫名稱 |
| 轉移的 `by` | `by: <目錄>.<名稱>` |
| 埠 | `port: <目錄>.<埠>.<操作>` |

- 別的目錄的 use case 不必列在 `needs`。`needs` 只留給還沒立法的 use case。
- `basis` 可以直接引用另一個目錄的規則 id。
- 擋下的規格只能引用擋下的規格；草稿可以引用兩者。

### 埠

- `model.yaml` 的 `ports` 宣告問題側看得到的外部依賴。名稱是規格的詞彙，受禁用詞彙檢查。埠 `X` 對到介面 `XPort`，操作對到同名的方法。
- 步驟 `port: <埠>.<操作>` 加 `returns`，設定這個操作在範例期間回傳什麼。
- v2.0 只有 `returns`。失敗、呼叫紀錄、順序是 v2.1。
- 範例執行到的埠沒有設定 `returns`，範例未通過。

### 步驟

每個步驟是 `run`、`use`、`port`、`advance` 其中一種。`when` 必須是 `run`。`as` 必須是 `actors` 裡的操作者。

### 情境相同的範例

兩個範例的 `given`（展開 fixture 後）、`when`、`clock` 相同時：

- `result` 或 `code` 不同，擋下：兩者不可能同時通過。
- 其餘的斷言不同，只列出：可能是各驗同一個輸出的不同面向。

### 執行（版本 2）

第四節「執行」的表仍然適用（use case 的名稱、`as`、`input`、`REJECTED`、隔離、`needs`、`input_pending`）。版本 2 的目錄另外照下表執行；版本 1 的目錄照舊。

| 規格 | 執行器怎麼做 |
|---|---|
| `then` 的 `output`、`absent`、`only` | 比對 `when` 的輸出。`when` 的 `save` 先存，所以 `then` 可以引用它存的變數 |
| 路徑 | 輸出先轉成對應表與清單再選取。屬性不存在與值是 null，對 `absent` 是同一件事；對 `output` 不是 |
| `use_expect` | 具名斷言在下，直接寫的欄位蓋上去 |
| 時間 | 規格寫帶時區的時間時，比對時間點。程式輸出不帶時區的時間，依 `local_time_zone` 解讀 |
| 輸入的時間 | `execute` 的參數是不帶時區的時間（`LocalDateTime`）時，把輸入換算成 `local_time_zone` 的當地時間。只處理參數本身，不處理物件或清單裡面的時間 |
| `clock`、`advance` | 應用程式注入的 `Clock` 在範例期間固定不動，只有 `advance` 會讓它前進。範例結束後回到系統時間 |
| `matrix` | 每一列是一個測試，名稱是 `<目錄>/<name>.<值>.<值>` |
| `emits`、`emits_include` | 擷取 `when` 執行期間發出的事件；準備步驟發出的不算。事件 `X` 是類別 `XEvent`，`with` 比對它的屬性（getter） |
| `REJECTED` | 另外確認 `when` 沒有發出任何事件 |
| `use: <目錄>.<名稱>` | 展開那個目錄的 fixture。它的步驟用**那個目錄**的 `actors` |
| `run: <目錄>.<名稱>` | 只用名稱找類別；目錄是給檢查程式核對用的 |
| `port` 加 `returns` | 埠 `X` 是介面 `XPort`。範例期間，這個操作回傳 `returns`（轉成方法的回傳型別），不呼叫原本的實作 |
| 沒有設定 `returns` 的埠 | 範例期間執行到，範例未通過；程式把錯誤吃掉也一樣 |
| 找不到埠、操作，或 fixture 不存在 | 無法執行，算未通過 |

限制：

- 只有 `model.yaml` 的 `ports` 宣告過的埠會被替換，而且只在版本 2 的範例期間。版本 1 的範例用原本的實作。
- 被替換的埠，程式必須依介面（`XPort`）注入。依實作類別注入，驗收測試啟動時就會失敗。
- 時鐘只管得到注入的 `Clock`。直接取系統時間的程式不受 `clock`、`advance` 影響。
