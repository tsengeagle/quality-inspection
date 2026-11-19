# 規格格式 v2 草案

狀態：Q-1～Q-9 全部已裁決（2026-11-20）。A 組已成為 D-063（格式 v1.1）並生效；B 組（v2.0）、C 組（v2.1）待各期的修法 PR。本文件不是律法：格式以 `specs/README.md` 與 `decision-log.md` 為準。

## 一、範圍

負責人 2026-11-19 決定：

- 這一輪只處理問題側的規格（`specs/`）。裁決、閘門、基線、傳統的格式不在範圍內。
- v2 要回應三組需求：

| 組 | 需求 | 起因 |
|---|---|---|
| A | 補 v1 的兩個漏洞 | `10-spec-format.md` 第五節：未定義的欄位被靜默接受；`given`、`when` 相同而 `then` 不同的範例檢查不出來 |
| B | ReviewCase 立法需要的寫法 | 狀態機、多角色、讀取遮罩、時間、跨 aggregate；漏網紀錄 E-01、E-02 的根本回應是把 ReviewCase 立法 |
| C | 外部資源協定 | 同機共用醫事人員卡（負責人提供的 `hca-card-mutex-eventstorming.drawio`，三頁：現況與實驗事實、目標流程、決議與驗收；未進 repo）：呼叫的順序、資源有沒有交出去、部分成功、兩個行程搶同一把鎖 |

本文件的檢驗方式是第五節的對照表：B 與 C 的每一項具體需求，都要能用 v2 寫成可執行的範例，或明確說明為什麼留在規格之外。

## 二、現況盤點

### v1 的 README 與實際使用不一致

2026-11-20 以腳本列出 `specs/` 現有規格的全部鍵路徑（`http.yaml` 除外，它在第七節已有封閉的形狀），逐一對照 `specs/README.md`：

- **README 描述了規則與範例，但沒有描述檔案的結構。**
  - 有說明的：規則的 `id`、`status`、`source`、`outcome`；`guards` 的順序；`error_codes`；範例的 `basis`、`needs`、`fixtures`、`given`／`when`／`then`／`observe`、`save`、`input_pending`、`derived_from`、`not_covered`；型別詞彙。
  - **`model.yaml` 與 `usecases.yaml` 的骨架都沒有欄位表。**
- 沒有說明、但已經在用的欄位如下（括號內是出現次數）：

| 檔案 | 沒有說明的欄位 |
|---|---|
| `model.yaml` 骨架 | `name`、`term`、`identity`（`name`、`type`、`assigned_by`）、`sources`、`attributes`、`entities`（`name`、`term`、`attributes`）、`enums`（`name`、`values`）、`transitions`（`from`、`to`、`by`）、`terminal_states`、`invariants`、`deferred_attributes`、`deferred_behaviors` |
| `model.yaml` 屬性 | `name`、`type`、`required`、`initial`（9）、`assigned_by`（3）、`mutable`（2）、`range`（1）、`external`（型別詞彙裡提過） |
| `model.yaml` 其他 | `invariants[].enforced_by`（1） |
| `usecases.yaml` 骨架 | `aggregate`、`sources`、`input_types`、`usecases[]` 的 `name`、`kind`（command 9、query 7）、`summary`（16）、`actor.roles`、`input`、`input_note`（1）、`guard_exceptions`（5）、`rules`、`success`（`state`、`emits`、`output`）、`output`（`status`、`source`、`fields`） |
| `examples.yaml` | `not_covered[]` 的 `rules`（4）、`item`（2）、`reason`；`actors` 的鍵是操作者名稱 |

所以「封閉結構」不能只照 README 現有的欄位來寫，否則兩份已經立法的規格會立刻全紅。實際上要做的是**為三種檔案補寫完整的欄位表**。處理方式見 Q-1。

盤點時另外發現三處形狀不一致：

1. **兩種輸出寫法。**
   - command 的輸出寫成 `success.output`：一個 `{name, type}` 的清單，3 處。
   - query 的輸出寫成 `usecases[].output`：一個帶 `status`、`source`、`fields` 的物件，7 處。
2. **`not_covered` 的兩種寫法。**
   - `rules: [...]`：指向規則 id。
   - `item: 文字`：score-sheet 的 2 處。
3. **`not_covered.rules` 裡有一個值不是規則 id。** review-project 寫了 `rules: [emits]`，`emits` 是一個欄位名稱，不是 id。其餘 3 個值都是存在的 id。
   - 若第 3 項的引用檢查涵蓋 `not_covered.rules`，這一處會變紅。
   - 它應該改寫成 `item: emits`，與 score-sheet 的寫法一致。

其中 `guard_exceptions[].guard` 指向 guard 的 id，但 `spec_lint` 不核對它是否存在。這是 A 組之外的另一個小漏洞，一併處理。

2026-11-19 以腳本核對現有規格（review-project 3 條轉移、score-sheet 1 條轉移）：`transitions[].by` 都是同目錄的 use case，`terminal_states` 都是列舉值，`guard_exceptions` 指向的 guard 都存在。所以第四節第 3 項的檢查上線時，這三種引用不會讓現有規格變紅。`not_covered.rules` 例外，見上一段第 3 點。

### 執行器的現況

`acceptance/ExampleRunner.java` 的行為與 v2 相關的部分：

- `then`、`expect` 只讀 `result`、`code`、`output`，其餘的鍵忽略。
- 沒有 `run` 的步驟丟出 `RunnerProblem`，範例未通過。
- `expect.output` 是部分比對，只比列出的欄位，**無法斷言某個欄位不存在**。這是讀取遮罩最需要的能力。
- 每個範例在一個交易裡執行，只有一個執行緒。

## 三、設計原則

1. **v1 的三個前提不變。**
   - 範例只透過 use case 準備、執行、觀察。
   - 一條規則只有一個結果（`SUCCESS` 或 `REJECTED`）。
   - 規格不出現實作詞彙。
   
   v2 新增的寫法都要在這三個前提下成立；做不到的，寧可不支援。
2. **新增的外部接觸點只有兩種：時鐘、埠。** 規格之外的世界，只能透過這兩種宣告過的東西進入範例。不提供「直接寫資料」的後門。
3. **沒有對照表。** 埠的名稱和 use case 一樣，要求程式使用規格的名稱，由執行器依名稱找到對應的介面。
4. **判決是確定性的。** 並行也要用確定性的方式表達（第四節第 9 項），不靠真的同時執行、看運氣。
5. **封閉。** 每一種檔案的每一層都列出允許的欄位，沒列出的欄位是問題。唯一的例外是 `note`：任何一層都可以有 `note`，內容是自由文字，不參與判決。
6. **可攜。** `spec_lint` 屬於 `harness-core/`，不含專案專屬的路徑或詞彙。執行器是各專案自己的程式，v2 只規定它要做到什麼。

## 四、v2 的寫法

### A 組：補漏洞

**1. 封閉結構**

`spec_lint` 內建每一種檔案的欄位表，結構與型別都檢查。對 v1 檔案也生效（Q-1）。

```
[問題] examples.yaml ext_close_fails_three_times then：不認得的欄位 resource、call_count
```

**2. 分辨不了情境的範例**

把 `given` 展開 fixture 之後，若兩個範例的 `given`、`when`（含時鐘與埠的設定）完全相同，而 `then` 不同，判為問題。若兩者的 `then` 也相同，只列出，因為可能是刻意的重複。

**3. 引用的完整性**

`guard_exceptions[].guard`、`transitions[].by`（必須是同目錄的 use case）、`terminal_states`（必須是狀態列舉的值）都要核對。

### B 組：ReviewCase 需要的

**4. 欄位不存在的斷言（讀取遮罩）**

```yaml
observe:
  - run: GetReviewCaseDetail
    as: rereviewer_h1
    input: { caseId: "${c}" }
    expect:
      result: SUCCESS
      output: { status: REREVIEW_IN_PROGRESS }
      absent: [initialAnswers, initialScore, initialComments]   # 永久盲審（D-033）
```

`absent` 的每一項是屬性路徑，可以用 `.` 指到巢狀屬性。執行器確認該屬性不存在或為 null。哪一種算遮罩見 Q-6。

**5. 時鐘**

```yaml
examples:
  - name: overdue_assignment_is_flagged
    clock: { start: "2026-11-19T09:00:00Z" }        # 這個範例的起始時間
    given:
      - run: AssignReviewer
        as: manager_h1
        input: { caseId: "${c}", reviewerId: u-r1, dueDate: "2026-11-20" }
      - advance: P2D                                 # 讓時間前進（ISO 8601 期間）
    when: { run: GetReviewCaseDetail, as: manager_h1, input: { caseId: "${c}" } }
    then: { result: SUCCESS }
```

- 沒有寫 `clock` 的範例，執行器仍要提供固定的時鐘，起始值是規格目錄層級的預設值。範例的結果不能依賴執行當下的時間。
- 程式必須從注入的時鐘取時間，這和 ARCH-02 的方向一致（`domain`、`application` 不得直接取系統時間，`now(Clock)` 不算違規）。但 2026-11-19 的 `domain` 仍有 19 處無參數的取時呼叫，都凍結在 ARCH-02 的基線內：`ReviewCase` 6 處、`ReviewSubmission` 9 處（`LocalDateTime.now()`），`SamplingBatch` 3 處、`DomainEvent` 1 處（`Instant.now()`）。這 19 處要先改成注入的時鐘，時鐘的範例才能作用在 ReviewCase 上。
  - 2026-11-20 的建構 PR 改掉 `ReviewCase`、`ReviewSubmission`、`SamplingBatch` 的 18 處：domain 方法改收時間參數，use case 以注入的 `Clock` 傳入，ARCH-02 基線刪 18 行。行為不變，型別也沒改。
  - 剩下 `DomainEvent` 的 1 處（`occurredAt`）。建立事件的 29 支 use case 含已立法範圍，另案處理；ReviewCase 的第一個薄切片不比對事件時間，不影響 v2.0。其中 `LocalDateTime` 要不要改成 UTC 的 `Instant`，是 `05-current-state.md` 第五節第 3 項待裁決的事。

**6. 矩陣範例（角色 × 狀態）**

讀取權限與遮罩是「多個角色 × 多個狀態」的表。逐一寫範例會有幾十個幾乎一樣的範例，人審不出漏掉哪一格。

```yaml
  - name: initial_content_visibility
    basis: [RC-VIS-01, RC-VIS-02, RC-VIS-03]
    matrix:
      columns: [actor, state, expect]
      rows:
        - [manager_h1,    INITIAL_REVIEW_SUBMITTED, visible]
        - [rereviewer_h1, REREVIEW_IN_PROGRESS,     masked]
        - [admin,         CLOSED,                   metadata_only]
    given:
      - use: "case_in_${state}"
    when: { run: GetReviewCaseDetail, as: "${actor}", input: { caseId: "${c}" } }
    then: { result: SUCCESS }
    observe:
      - use_expect: "${expect}"      # 指向同檔 expectations 底下的具名斷言
```

- 展開後每一列是一個範例，名稱是 `<name>[<第一欄>,<第二欄>]`，例如 `initial_content_visibility[rereviewer_h1,REREVIEW_IN_PROGRESS]`。這個名稱就是它在已知失敗清單裡的身分，所以列的順序可以改，但值不能改。
- `expectations` 是可重用的具名斷言，和 `fixtures` 對稱。
- `spec_lint` 檢查每一列的欄數、引用的 fixture 與斷言都存在。

**7. 事件**

v1 的 `success.emits` 已經存在，但不驗證。v2 讓範例可以斷言：

```yaml
    then:
      result: SUCCESS
      emits:
        - { event: InitialReviewSubmitted, with: { caseId: "${c}" } }
      emits_only: true            # 除了列出的事件，不得有其他事件
```

執行器要能取得這次 use case 執行期間發出的事件。事件名稱必須在 `usecases.yaml` 的 `success.emits` 有宣告。

**8. 轉移的涵蓋**

`model.yaml` 的每一條 `transitions` 都必須至少被一個範例的 `basis` 引用，否則判為問題（v1 只列出）。狀態機是 ReviewCase 最容易漏的地方，所以只有轉移這一類先改成「擋下」。其餘規則仍然只列出（Q-8）。

### C 組：外部資源協定

**9. 埠**

在 `model.yaml` 宣告問題側看得到的外部依賴，以及它的操作：

```yaml
ports:
  - name: CardModule
    term: 卡片模組
    operations:
      - { name: open }
      - { name: readCardNo, returns: id }
      - { name: login }                 # 會送出 PIN
      - { name: sign, returns: text }
      - { name: closeSession }
      - { name: close }
  - name: CardLock
    term: 卡片鎖
    operations:
      - { name: acquire, waits: true }  # 可能等待，受時鐘控制
      - { name: release }
```

- 埠的名稱、操作名稱是規格的詞彙，和 use case 名稱一樣受禁用詞彙檢查。這裡不寫 DLL 的函式名稱。
- 執行器依名稱找程式裡的介面（命名慣例見 Q-3），換成可以腳本化的替身。替身會記錄每一次呼叫。

在範例裡設定埠的行為：

```yaml
    given:
      - port: CardModule.readCardNo
        returns: OTHER-CARD
      - port: CardModule.close
        fails: { times: 3, code: "9xxx" }     # 前 3 次失敗
```

**10. 呼叫的斷言**

```yaml
    then:
      result: REJECTED
      code: CARD_MISMATCH
      calls:
        CardModule: [open, readCardNo, close]   # 依序、完整
        CardLock:   [acquire, release]
      never:
        - CardModule.login                      # 規則 3：不送 PIN
```

- `calls` 是完整的序列：多一次、少一次、順序不同都算不符。要部分比對時，改用 `calls_include`。
- 跨埠的順序（規則 1：先取鎖才開模組；規則 2：模組關閉才放鎖）用 `order`：

```yaml
      order:
        - [CardLock.acquire, CardModule.open]
        - [CardModule.close, CardLock.release]
```

**11. 面向（資源有沒有交出去）**

白板的重點是「拿到簽章」與「卡片資源可以交給對方」是兩件事。v2 不放寬「一條規則一個結果」，改成在結果之外宣告**面向**：

```yaml
# model.yaml
facets:
  - name: resource
    term: 卡片資源
    values: [RELEASED, HELD, FAULTED]
    observed_by: GetCardResourceStatus        # 一支查詢 use case
```

```yaml
# usecases.yaml 的規則
      - id: HC-USE-03
        when: 簽章已取得，但關閉模組重試三次仍失敗
        outcome: { result: SUCCESS, facets: { resource: FAULTED } }
```

```yaml
# examples.yaml
    then: { result: SUCCESS, facets: { resource: FAULTED } }
```

- `result` 仍然只有一個值，回答的是「使用者拿到了什麼」。面向回答的是「系統之後處於什麼狀態」，兩者互不取代，所以不是「A 或 B」。
- 面向必須有 `observed_by`。執行器在 `then` 之後執行那支查詢來比對，所以面向不是只寫在紙上的東西。

**12. 行程與確定性的交錯**

白板驗收 T1「A 在鎖內時，B 等待且沒有碰 DLL；A 放鎖後 B 取得」。v2 用「停在某個埠的操作上」來表達交錯：

```yaml
  - name: waits_while_other_process_holds_the_card
    basis: [HC-G-01, HC-INV-01]
    processes: [A, B]
    given:
      - process: B
        run: UseCardOnce
        as: doctor_b
        input: { document: "處方 B" }
        pause_at: CardModule.login          # B 停在登入之前，此時 B 持鎖
    when:
      process: A
      run: UseCardOnce
      as: doctor_a
      input: { document: "處方 A" }
      advance: PT15S                         # A 等待期間時間前進 15 秒
    then:
      result: REJECTED
      code: CARD_IN_USE
      never: [A:CardModule.open]             # A 沒有碰卡
```

- 執行器以執行緒模擬行程，在 `pause_at` 的操作上讓它停住，所以每次的交錯都一樣。
- 這只驗證「程式依規則使用鎖」，不驗證鎖本身真的能跨 JVM 互斥。後者是實機的整合測試（T1 的實機部分、T4、T5），不屬於規格（Q-4）。

## 五、檢驗：需求對照

### C 組：白板與驗收項目

| 來源 | 內容 | v2 的寫法 | 可執行 |
|---|---|---|---|
| 規則 1 | 先取鎖才開模組 | 第 10 項 `order` | 是 |
| 規則 2 | 模組確認關閉才放鎖 | 第 10 項 `order`；關閉失敗時 `never: [CardLock.release]` | 是 |
| 規則 3 | 對方可能持卡時不送 PIN | 第 10 項 `never: [CardModule.login]` | 是 |
| 白板第 2 列 | 15 秒內等不到鎖 → 卡片使用中，沒有碰卡 | 第 12 項 | 是 |
| 白板第 3 列 | 鎖檔無法建立 → 拒絕用卡 | 第 9 項 `CardLock.acquire` 設定 `fails` | 是 |
| 白板第 4 列 | 卡號不符 → 不送 PIN、已清理、鎖已釋放 | 第 9 項 `returns`；第 10 項；第 11 項 `resource: RELEASED` | 是 |
| 白板第 5 列 | DLL 回錯誤碼；PIN 類錯誤不重試 | 第 9 項 `fails`；第 10 項 `calls` 只出現一次 `login` | 是 |
| 白板第 6 列 | 簽章已取得但關閉失敗三次 → 簽章照常回傳、鎖沒有釋放、資源異常 | 第 9 項 `fails: { times: 3 }`；第 11 項 `resource: FAULTED`；第 10 項 `calls` 只出現一次 `sign` | 是 |
| 白板 | 顯示佔用者 | **不支援**：`REJECTED` 不帶輸出（v1 前提）。改成另一支查詢 use case 觀察佔用者，或列為 Q-5 | 有條件 |
| 批次 | 第 N 份簽章失敗 → 前 N−1 份照常保存 | 第 9 項 `fails: { on_call: N }`；`observe` 查詢已保存的文件 | 是 |
| 批次 | 第 M 份保存失敗 → 未保存的簽章丟棄 | 同上，對保存的埠設定 | 是 |
| 批次 | 分段 K、記憶體、整批持鎖超過 15 秒 | `proposed` 加題目；量測（T6）不屬於規格 | 否，留給人 |
| T1 | 兩個 JVM 搶鎖 | 第 12 項驗證使用方式；真正的跨 JVM 互斥是實機測試 | 部分 |
| T2 | 清理與結果分開（假 DLL） | 第 9、10、11 項 | 是 |
| T3 | 卡號不符、等不到鎖、鎖檔失敗都沒有呼叫 InitSession | 第 10 項 `never` | 是 |
| T4～T6 | POC 重跑、正式程式實機、量測 | 不屬於規格（S–W，由人判斷） | 否 |

### B 組：ReviewCase

| 需求 | 來源 | v2 的寫法 | 可執行 |
|---|---|---|---|
| 永久盲審：複審者看不到初審內容 | D-033、permission-matrix §5 | 第 4 項 `absent` | 是 |
| SYSTEM_ADMIN 只看得到 metadata 白名單 | permission-matrix §5.1 | 第 4 項 `absent`，或封閉的輸出（Q-6） | 是 |
| 角色 × 狀態的可見性表 | permission-matrix §5、§6 | 第 6 項矩陣 | 是 |
| 讀過完整初審的人不得再被指派為複審 | D-055 | v1 已可寫：先讀、再指派，`REJECTED REREVIEWER_HAS_READ_INITIAL` | 是（v1） |
| 歷史不完整的案件（導入前建立）一律拒絕 | D-055 | **use case 建立不出「歷史不完整」的案件**。依 Q-7 的裁決，列在 `not_covered`，由建構者的白箱測試負責；不開資料後門，也不把系統自己的儲存宣告成埠 | 否 |
| 指派期限、逾期 | domain-model `dueAt` | 第 5 項時鐘 | 是 |
| updatedAt 不倒退、建案初始化為 UTC | D-037 | 第 5 項時鐘加 `advance` | 是 |
| 狀態機每一條轉移都有範例 | state-machine §3.2 | 第 8 項 | 是 |
| 提交、固定、結案發出事件 | domain-events | 第 7 項 | 是 |
| 裁決撤銷之後，舊的入口不得再出現（E-02） | D-032 | 第 4 項 `absent` 斷言該欄位不存在 | 是 |

## 六、待裁決

全部已裁決，負責人都選了建議的選項：

- Q-1、Q-2（2026-11-20）：成為 D-063（格式 v1.1），已生效（#109、#110）。條文見第九節。
- Q-3～Q-9（2026-11-20）：查證與理由見第十節。Q-7 的建議在查證後由 (a) 改為 (c)。

它們會在各期的修法 PR 裡寫成裁決：v2.0 用 Q-6、Q-7、Q-8，v2.1 用 Q-3、Q-4、Q-5、Q-9。

| # | 題目 | 選項 | 建議 |
|---|---|---|---|
| Q-1 | 封閉結構怎麼處理 README 沒有說明、但已經在用的欄位 | (a) 全部寫進 README，成為正式欄位；(b) 逐一檢討，不需要的刪掉；(c) v1 不封閉，只有 v2 封閉 | **已裁決（2026-11-20）：(a) 加 (b)。** 照現狀補完整的欄位表，現有規格保持綠燈；另標出 4 項下一次修法改寫（第九節第 3 點） |
| Q-2 | 版本策略 | (a) A 組對 v1 直接生效，B、C 組要 `spec_version: 2`；(b) 全部等 v2 | **已裁決（2026-11-20）：(a)。** 檔案仍是 `spec_version: 1` |
| Q-3 | 埠在程式裡的命名慣例 | (a) 埠 `X` 對到介面 `XPort`，操作對到同名方法；(b) 用註解標記 | **已裁決（2026-11-20）：(a)。** |
| Q-4 | 並行要做到哪裡 | (a) 只做第 12 項的確定性交錯；(b) 另外支援真的多行程 | **已裁決（2026-11-20）：(a)。** |
| Q-5 | 被拒絕時要不要能帶輸出（例如佔用者） | (a) 維持 v1：拒絕不帶輸出；(b) 允許 `REJECTED` 帶具名的 `details` | **已裁決（2026-11-20）：(a)。** |
| Q-6 | 遮罩的語意 | (a) `absent` 指屬性不存在或為 null；(b) 分成 `absent`（不存在）與 `masked`（存在但被遮罩的固定值） | **已裁決（2026-11-20）：(a)。** `absent` 指屬性不存在或為 null（第十節） |
| Q-7 | 用 use case 建立不出來的前提（歷史不完整的案件） | (a) 宣告成埠，由範例設定；(b) 允許 `seed` 直接寫入，但限定宣告過的情境；(c) 不寫範例，列在 `not_covered` | **已裁決（2026-11-20）：(c)。** 起草時建議 (a)，查證後改為 (c)（第十節） |
| Q-8 | 哪些「只列出」要改成「擋下」 | 轉移、沒有範例的規則、`basis` 含 `proposed` | **已裁決（2026-11-20）：先擋轉移。** |
| Q-9 | 醫事人員卡的規格放哪裡 | (a) 它自己的專案，安裝 `harness-core/`；(b) 本 repo 的 `spec-draft/` 試寫 | **已裁決（2026-11-20）：(a)。** |

## 七、實作順序

每一期都是：裁決 → 修法 PR（`specs/README.md`、`spec_lint.py` 與測試）→ 執行器的修法 PR → 建構 PR（程式配合）。`spec_lint` 和執行器都是律法，不能和建構 PR 混在一起（P4）。

| 期 | 內容 | 前提 | 對現有規格的影響 |
|---|---|---|---|
| v1.1 | A 組（第 1～3 項）、Q-1 的欄位表 | Q-1、Q-2 | **已完成**（D-063，#109、#110）。現有規格零問題 |
| v2.0 | B 組（第 4～8 項） | Q-6、Q-7、Q-8 已裁決；`domain` 的取時呼叫已改掉 18 處，剩 `DomainEvent` 1 處（第 5 項） | 現有範例照跑；新寫法供 ReviewCase 使用 |
| v2.1 | C 組（第 9～12 項） | Q-3、Q-4、Q-5、Q-9 已裁決；**第十一節的缺口一（語法）要先補進第四節；缺口二（harness-core 寫死的假設）已由 D-064 修正（#115）** | 規格目前沒有宣告任何埠（程式裡的 11 個 `XPort` 是內部的查詢與儲存介面，不因此變成規格的一部分）；回歸樣本是 `samples/card-v2-trial/`（第十一節） |

v2.0 之後的第一個使用者是 ReviewCase 的薄切片（分派、初審提交、revision-history 的讀取遮罩）。用它驗證 B 組夠不夠，之後再擴大。

## 八、這份草案沒有處理的

- 請求端的 HTTP 契約（W-9／W-10）。屬於 `http.yaml`，不在這三組需求裡。
- 規則 8（架構詞彙只用 glossary 有定義的詞）的機械檢查。
- 禁用詞彙清單依專案分開（`10-spec-format.md` 第五節 v2 候選第 8 項）。與 `harness-core/` 拆成獨立 repo 一起處理。
- 第四節 B、C 組的語法只在紙上推演過，還沒有實作來證明它可行。A 組已在 v1.1 實作（D-063），回歸樣本是 `10-spec-format.md` 第五節的兩份草稿：
  - 嚴格版的空洞範例被第 2 項擋下。
  - 擴充版的硬寫：試寫結果二的前 7 列都被擋下，其中 6 列由第 1 項擋下；第 8 列是實作詞彙，不屬於 A 組。
- C 組已用白板 V10 試寫過一次（第十一節），但只有暫時的檢查，執行器沒有跑。

## 九、D-063 草案（v1.1）

依 Q-1、Q-2 的裁決起草。D-063 只涵蓋 v1.1（A 組）；B、C 組等 Q-3～Q-9 裁決後另立。

**2026-11-20 更新：條文已和實作放在同一個修法 PR，寫進 `decision-log.md` 的 D-063。以 decision-log 為準。**

實作時對下面的草案做了兩處更正：

- 第 6 點：擴充版不是「8 種寫法全部被第 2 點擋下」。試寫結果二的第 1 列 v1 就已經擋下，第 8 列是實作詞彙，不屬於 v1.1。
- 「起草時要注意的」第二點：決定欄位表內建在 `spec_lint`，README 由人工維持一致（D-063 第 2 點）。

下面保留起草時的版本，供對照。

### 草案條文

> ## D-063 — 規格格式 v1.1：封閉結構、分辨不了情境的範例、引用完整性
>
> ### Status
>
> `accepted`（日期待定，依負責人 2026-11-20 對 Q-1、Q-2 的裁決）
>
> ### Decision
>
> 1. **適用範圍。**
>    - 適用於 `specs/` 與 `--report-only` 草稿的 `model.yaml`、`usecases.yaml`、`examples.yaml`。
>    - 檔案仍寫 `spec_version: 1`。
>    - `http.yaml`、`http-common.yaml` 不變：它們的形狀已經是封閉的（D-061）。
> 2. **封閉結構。**
>    - `specs/README.md` 為三種檔案補完整的欄位表，列出每一層允許的欄位與型別。
>    - 欄位表照現有規格的寫法收錄。現有規格用到的欄位全部列入，不因收錄而改寫任何規格。
>    - `spec_lint` 依欄位表檢查；表上沒有的欄位、型別不符的值都是問題，擋下。
>    - 任何一層都可以有 `note`。它是自由文字，不參與判決。
> 3. **收錄但待改寫的四項。** 這四項列入欄位表並標為「待改寫」。v1.1 照樣接受，在下一次規格格式的修法中改寫：
>    1. command 的 `success.output`（清單）與 query 的 `usecases[].output`（帶 `fields` 的物件）合併成一種寫法。
>    2. `usecases[].input_note`、`invariants[].enforced_by` 併入 `note`。
>    3. `attributes[].range` 改寫成有 id 的規則。它目前沒有任何範例或程式在驗證；範圍的依據待 D-046 確認。
>    4. review-project 的 `not_covered` 寫了 `rules: [emits]`，`emits` 不是規則 id。改成 `item: emits`，與 score-sheet 一致。
> 4. **分辨不了情境的範例。**
>    - 範例的 `given` 依 fixture 展開之後，若兩個範例的 `given`、`when` 完全相同：
>      - `then` 不同，判為問題，擋下。
>      - `then` 也相同，只列出：可能是刻意的重複。
>    - 比較時忽略 `name`、`basis`、`derived_from`、`note`。
>    - `${名稱}` 依字面比較。變數名稱不同、實質相同的兩個範例，檢查不出來。
> 5. **引用完整性。** 下列引用都要核對，不符就擋下：
>    - `guard_exceptions[].guard`：必須是同一支 use case `guards` 裡的 id。
>    - `transitions[].by`：必須是同目錄 `usecases.yaml` 的 use case。
>    - `transitions[].from`、`to`：必須是同一個列舉的值。
>    - `terminal_states`：必須是上一點那個列舉的值。
>    - `not_covered[].rules`：等第 3 點第 4 項改寫後才核對。
> 6. **回歸樣本。** `docs/harness/10-spec-format.md` 第五節的兩份試寫草稿，放進 `harness-core/tests/` 當測試資料：
>    - 嚴格版的空洞範例要被第 4 點擋下。
>    - 擴充版的 8 種寫法要全部被第 2 點擋下。
> 7. **順序。** 本裁決用一個修法 PR 完成，內容是：
>    - `decision-log.md`
>    - `specs/README.md` 的欄位表與第六節
>    - `harness-core/spec_lint.py` 與它的測試
>
>    執行器不動，因為 v1.1 沒有新的語意。現有規格在這個 PR 上必須維持零問題。若出現問題，那就是真的缺陷，另案處理，不在這個 PR 裡放寬欄位表。
>
> ### Relationship / Evidence
>
> - 起因：`docs/harness/10-spec-format.md` 第五節。試寫的擴充版用了 8 種不存在的欄位，`spec_lint` 回報零問題；嚴格版的兩個範例只差在 `then`，也檢查不出來。
> - 盤點：`docs/harness/15-spec-format-v2-draft.md` 第二節（2026-11-20）。
> - 以腳本核對過現有規格，第 5 點的每一項都不會讓它們變紅：
>   - 例外的 guard 都在那支 use case 自己的 `guards` 裡（5 處）。
>   - 轉移的狀態各屬於 `ProjectStatus`、`ScoreSheetStatus`。
>   - `terminal_states` 也都屬於同一個列舉。
>
> ### Mapping
>
> - `specs/README.md`、`harness-core/spec_lint.py`、`harness-core/tests/test_spec_lint.py`
> - 後續：B 組（v2.0）、C 組（v2.1）另立裁決

### 起草時要注意的

- **回歸樣本現在只在本機的暫存目錄裡**，還沒進 repo。修法 PR 開工前要先確認它們還在；不在的話，依 `10-spec-format.md` 第五節的描述重寫。
- **`spec_lint` 屬於 `harness-core/`，要可攜。** 欄位表應該是 `spec_lint` 內建的格式定義，不能讀本專案的 README。README 的欄位表是給人看的同一份內容。兩者是否一致，由 `harness-core` 的測試核對，或接受人工維持。這一點在實作時決定，不影響條文。

## 十、Q-3～Q-9 的裁決與查證（2026-11-20）

負責人逐題選了建議的選項。下面記錄裁決前查證的事實，以及它們怎麼影響建議。

### Q-3 埠的命名：埠 `X` 對到介面 `XPort`

- application 層已經有 11 個 `XPort` 介面，例如 `ReviewProjectQueryPort`、`CandidateSourcePort`、`RereviewerAssignmentHistoryPort`。這個慣例已經存在，不是新創。
- 只有規格宣告過的埠能在範例裡注入。現有的內部查詢與儲存介面，不會因為名稱是 `XPort` 就變成規格的一部分。
- 規格裡寫的是 `CardModule` 這種不帶 `Port` 的名稱，所以 `Port` 不必加進禁用詞彙。

### Q-4 並行：只做確定性交錯

真正跨 JVM 的互斥（白板 T1）由實機測試負責，不放進判決路徑。

### Q-5 拒絕的結果：維持只有代碼

「顯示佔用者」改用另一支查詢 use case 觀察。拒絕時錯誤回應的形狀由 HTTP 契約負責。

### Q-6 遮罩：`absent` 指屬性不存在或為 null

查證了三件事：

1. **執行器分不出「沒有」和「null」。** 執行器讀的是 use case 回傳的 Java 物件，屬性依名稱比對，看不到 JSON 的鍵。
2. **現行程式的兩種遮罩都不用替代值。**
   - metadata-only（D-036 的 8 欄）：改用另一個型別 `CaseDetailMetadataView`。
   - 盲審（D-033）：值設成 null，`SubmissionDetailView` 以 `NON_NULL` 在 JSON 裡拿掉，並附上 `maskedFields` 清單。
   - 程式裡沒有 `***`、`MASKED` 之類的固定遮罩值，所以選項 (b) 的 `masked` 用不到。
3. **「鍵不存在」是 JSON 的要求，已經有東西擋。** D-036 規定欄位「不得出現在 metadata-only JSON，包含不得以 null 保留」。HTTP 契約是封閉形狀（D-061），已經能擋。

分工：

- use case 層用 `absent` 斷言「看不到值」。
- JSON 層由 `http.yaml` 的封閉形狀斷言「鍵不存在」。
- `maskedFields` 是一般的輸出欄位，用 `expect.output` 比對。

### Q-7 用 use case 建立不出來的前提：不寫範例，列在 `not_covered`

**起草時建議 (a)「宣告成埠」，查證後改為 (c)。**

- 「歷史是否完整」存在 `RereviewerAssignmentHistoryPort.isComplete`，完整性在案件建立時決定（D-055 第 1～3 點）。這是系統自己的儲存，不是外部世界。
- 若把它宣告成規格的埠，範例就能替換系統的內部儲存。原則 2「外部接觸點只有時鐘和埠」中的埠，會從外部世界擴大到內部儲存。實質上等於選項 (b) 的資料後門。
- 所以這條規則列在 `not_covered`，由建構者的白箱測試負責。
- 將來若規格化「導入舊案件」的 use case，再用它建立前提，並把這條規則改成可執行的範例。

### Q-8 改成擋下：先擋轉移

- 現有 4 條轉移都有範例，改成擋下不會讓現有規格變紅。
- 沒有範例的一般規則（`RP-BND-03`、`RP-INV-04`）、`basis` 含 `proposed` 的範例，維持只列出。

### Q-9 醫事人員卡的規格：放在它自己的專案

本 repo 只把那兩份試寫當成格式的回歸樣本，已經放在 `harness-core/tests/fixtures/spec-format-trial/`。

## 十一、白板 V10 的 v2 試寫（2026-11-20）

### 目的與做法

負責人提供了白板 V10（6 頁，新增 02 共用規則、03 iccard 的 use case、04 HCAAPI 的 use case）與設計文件第四版，想知道規格能不能支援。

先對照現行的 v1.1：只寫得出骨架。白板約 25 個條件只有 2 個不碰卡、能寫成範例。硬寫的話，會被 v1.1 的封閉結構與「分辨不了情境的範例」擋下。

接著照第四節的 v2 寫法試寫一份：

- **位置**：依 Q-9，卡片規格屬於 iccard 的 repo，所以樣本放在 `samples/card-v2-trial/`，不放 `specs/`。
- **寫法**：嚴格照第四節。第四節沒有定義的寫法，不偷偷發明，在檔案裡標 `[缺口 G-xx]`。
- **檢查**：v2 還沒有實作，另寫一支暫時的檢查 `samples/card-v2-trial/v2check.py`。它只核對第四節已定義的引用與結構，執行器沒有跑。

### 結果

| | card-gate（02 頁） | iccard-signing（03 頁） | hcaapi-signing（04 頁） |
|---|---|---|---|
| 規則 | 25 | 16 | 19 |
| 範例（矩陣展開後） | 19 | 12 | 10 |

- 暫時的檢查：問題 0 個。故意改壞 5 處（拼錯的操作、未宣告的行程、不存在的 fixture、未定義的代碼、不存在的面向值），5 處都抓到。
- 沒有範例的規則 5 條，都是 `proposed`（待決 B、D、H）或寫在 `not_covered`。
- 現行的 `spec_lint`（v1.1）：165 個問題，原因是 `spec_version: 2` 與 v2 的欄位。

白板的三欄剛好對到三種斷言：

- 回應結果 → `then`
- 設備紀錄 → `calls`、`never`、`order`，加上面向 `resource`
- 業務事件：HCAAPI 的業務事件是寫進外部資料庫（工作清單、簽章登記），在規格裡是埠的呼叫，不是 `emits`

### 白板對照

| 白板 | 寫成範例 | 備註 |
|---|---|---|
| 02 一切正常 | 是 | `calls`、`order` 驗證規則 1～3 |
| 02 等不到鎖 | 是 | 佔用者用查詢 `GetCardLockOwner` 觀察（Q-5） |
| 02 鎖檔無法建立、卡號不符、DLL 回錯誤碼 | 是 | 登入只出現一次，驗證「不重試」 |
| 02 登入回 9043 | 是 | 在 iccard 驗證；元件這一層看不到錯誤碼（G-06） |
| 02 關 Session 失敗 | 是 | 修飾規則，用矩陣（G-02、G-11） |
| 02 關 Module 失敗（完成、失敗路徑） | 是 | 面向 `resource: FAULTED` |
| 02 資源狀態與復原 | 是 | 5 條轉移都有範例 |
| 02 重新啟動程式、行程崩潰 | 否 | 作業系統的行為，實機 T1 |
| 03 UC-I1 完成、9039、9041、9043 | 是 | |
| 03 UC-I2 完成 | 是 | 「QR Code 在放鎖之後產生」驗證不到（G-18） |
| 03 UC-I2 尚未設定、已到期、卡號不符、HCAAPI 正在用卡 | 是 | |
| 03 UC-I2 遇到 9039 就停（T9） | 是 | 第二張處方沒有送 PIN |
| 04 UC-H1 完成、失敗時清除 | 是 | 初始化狀態沒有查詢，只能用下一個命令觀察（G-23） |
| 04 UC-H2 鎖內簽、鎖外存 | 是 | `order` |
| 04 UC-H2 第 N 份簽章失敗、第 M 份保存失敗、① 失敗、登記開關關閉、換卡 | 是 | 部分成功寫成 SUCCESS 加輸出（G-08） |
| 04 兩個狀態欄位的組合（L＋已簽等） | 部分 | 外部資料庫的狀態，只能斷言對埠的呼叫 |
| 04 callAPI 整體序列化 | 否 | 同一個行程內的並行沒有寫法（G-26） |
| 05 T2、T3、T9 | 是 | |
| 05 T1 | 部分 | 確定性交錯可以；真正跨 JVM 不行（Q-4） |
| 05 T4～T8 | 否 | 實機、量測、相容性、回歸 |

### 缺口

一共 26 個。依處理的時機分四類。

**一、v2.1 實作前要先補進第四節的語法**

| # | 缺口 |
|---|---|
| G-08 | `then` 不能比對 `when` 自己的輸出。元件回傳的簽章、批次的「第幾份起失敗」都需要 |
| G-09 | 第 12 項只能讓對方停住、自己逾時。「等待中對方放鎖、自己取得」要能在 `when` 執行期間插入 `resume` |
| G-12 | 埠只能設定 `fails`、`returns`。「先失敗、之後恢復」要能寫成依呼叫次數的序列 |
| G-13 | `calls` 記錄的範圍沒有定義：只算 `when`，還是 `given` 也算 |
| G-24 | `fails: { on_call: N }` 只出現在第五節的對照表，第四節沒有定義 |
| G-25 | 同一個呼叫出現多次時，`order`、`never` 怎麼判（第一次、每一次、第幾次） |
| G-05 | 等待上限是設定值（待決 A），範例要能宣告設定 |
| G-14、G-15 | fixture、面向要能指定行程 |
| G-16 | 會等待的埠（`CardLock.acquire`）的替身要在行程之間共用，並由執行器實作互斥；否則交錯範例沒有意義 |
| G-10、G-11 | 具名斷言要能用在 `then`；矩陣要能代入 `then`，fixture 要能放埠的設定 |
| G-21 | `given` 的步驟要能預期被拒（T9：先被拒一次，再看下一次） |
| G-01 | 面向的值要能取自列舉 |

**二、harness-core 寫死了本專案的假設（v1.1 已經生效）**

| # | 缺口 | 會不會擋 |
|---|---|---|
| G-07 | `actors` 固定是 `user`、`roles`、`hospital`（`spec_lint.py` 的欄位表） | **會**：元件的操作者是程式，寫 `program` 就是不認得的欄位 |
| G-04 | guard 的順序寫死 D-024（角色 → 載入 → 院區範圍 → 領域規則）（`specs/README.md` 第三節規則 5） | 不會：`spec_lint` 不檢查順序，但這是律法的條文 |
| G-03 | 錯誤分類是為 HTTP 業務錯誤設計的 | 不會：試寫用了合法的分類，只是裝置錯誤放進去不貼切 |

依 Q-9，卡片規格要在 iccard 的 repo 安裝 `harness-core/`。G-07 不改，iccard 一寫就被擋；G-04、G-03 要讓條文不再假設本專案的領域。三項都是修法：`spec_lint.py` 與 `specs/README.md` 都是律法。

**三、跨 aggregate、跨 repo**

| # | 缺口 |
|---|---|
| G-17 | 埠與面向宣告在 card-gate，iccard 與 HCAAPI 都要引用；HCAAPI 的規格在另一個 repo |
| G-20 | 錯誤代碼不能共用，每份 `usecases.yaml` 都要重複定義 |
| G-22 | 交錯範例的另一個行程是另一支程式，但規格只能執行自己的 use case |

**四、模型本身的限制**

| # | 缺口 | 試寫時的處理 |
|---|---|---|
| G-02 | 「不論前面是哪一種結果」的修飾規則，不能寫成單一結果的規則 | 寫成不變量，用矩陣範例驗證 |
| G-06 | 元件的例外帶錯誤碼與佔用者；Q-5 讓拒絕只有代碼 | 錯誤碼在呼叫端驗證，佔用者用查詢觀察 |
| G-18 | 內部計算（QR Code）不是埠，「在放鎖之後」驗證不到 | 只有對外的步驟能用 `order` |
| G-19 | 到期是時間造成的轉移，沒有對應的 use case | 在範例裡用 `advance` 驗證，不寫成轉移 |
| G-23 | 沒有查詢可以觀察初始化狀態 | 用下一個命令的結果觀察，違反「observe 只跑查詢」 |
| G-26 | 同一個行程內的並行 | 列在 `not_covered` |

### 試寫逼出來的設計問題

要寫出完整的呼叫序列，才會碰到這四題。設計文件第四版目前沒有答案，已轉給負責人：

1. 關 Module 失敗時總共呼叫幾次？設計 §6.3 寫「重試最多三次」，白板寫「重試三次仍失敗」，是 3 次還是 4 次？重試間隔多長？
2. 卡號不符時沒有登入，清理還要不要呼叫 CloseSession？
3. iccard 開處方時卡號不符，簽章情境要不要撤銷？
4. 簽章情境的期限多長（spec 022 的範圍）？

### 還沒做的

- 缺口一的語法還沒寫進第四節，要等負責人決定。缺口二是 v1.1 的修法。
- 執行器沒有跑過，替身、交錯、面向的觀察都只在紙上。
- 範例沒有涵蓋 HCAAPI 的 `CARD_LOCKED`（9043）與 `updateCardinfo`（待決 E）。


### 四題的回答（2026-11-20，負責人）

| 題 | 回答 | 對範例的影響 |
|---|---|---|
| 1. 關 Module 失敗的重試 | 先定**重試 3 次，間隔 1 秒** | 第一次呼叫之後再重試 3 次，共呼叫 4 次，每次之間 `advance` 1 秒。負責人已確認「共 4 次」。次數與間隔是設定值（缺口 G-05），範例要宣告設定 |
| 2. 卡號不符時要不要 CloseSession | **不要**（沒登入就不關 Session） | 卡號不符的範例要寫 `never: CloseSession`，但 CloseModule 照常呼叫 |
| 3. 開處方時卡號不符，要不要撤銷簽章情境 | **要** | iccard-signing 要補一條規則與範例：撤銷之後，下一次開處方回 `SIGNING_NOT_SET` |
| 4. 簽章情境的期限 | 先定 **4 小時** | 到期範例用 `advance` 4 小時（缺口 G-19）；期限也是設定值 |

這四項的出處目前只有本節。設計文件第四版（不在 repo）還沒更新。寫成正式規格時，`source` 要指向更新後的設計文件，或另立裁決。

## 十二、缺口二的修法：D-064 草案（2026-11-20）

**2026-11-20 更新：已生效。** 負責人對「待負責人決定」的三點都照草案裁決，條文已和實作放在同一個修法 PR（#115），寫進 `decision-log.md` 的 D-064。以 decision-log 為準。

實作時對下面的草案做了三處補充：

- 第 1 點：設定檔本身也是封閉結構，要有 `kind: spec-profile`、`profile_version: 1`；讀不進來或內容不合法也判為無法評估。
- 第 2 點：型別名稱定為 `str`、`str_list`；兩項都不可以是空的。
- 第 4 點：`guard_order` 寫成 `text` 加 `source`，`source` 的裁決必須存在於決策紀錄。

下面保留起草時的版本，供對照。

### 起草時查到的寫死之處

| 位置 | 寫死的內容 | 缺口 |
|---|---|---|
| `spec_lint.py` 的 `EXAMPLES_SCHEMA` | `actors` 每一項只能有 `user`、`roles`、`hospital` | G-07 |
| `spec_lint.py` 的 `USECASE` | use case 的 `actor` 只能有 `roles` | G-07（試寫時沒標出，起草時查到） |
| `spec_lint.py` 的 `CATEGORIES` | 錯誤分類固定五種 | G-03 |
| `specs/README.md` 第三節規則 5 | guard 的順序依 D-024 | G-04 |
| `specs/README.md` 第三節規則 7 | 五種分類與「對應 HTTP 狀態碼」的說明 | G-03 |

已經可攜的前例：決策紀錄的位置讀律法清冊的 `@decision-log`；禁用詞彙讀 `specs/forbidden-vocabulary.txt`。本草案沿用第二種做法：專案專屬的內容放在 `specs/` 底下的一個檔案，`spec_lint` 讀它，不寫死在程式裡。

### 草案條文

> ## D-064 — 規格格式 v1.2：專案設定檔，harness-core 不再寫死本專案的操作者、錯誤分類與 guard 順序
>
> ### Status
>
> `proposed`（2026-11-20 起草；處理格式 v2 草案第十一節的缺口二：G-07、G-04、G-03）
>
> ### Decision
>
> 1. **專案設定檔。**
>    - 新增 `specs/spec-profile.yaml`，寫下規格格式裡屬於專案的部分。`spec_lint` 從規格目錄讀它，與 `forbidden-vocabulary.txt` 相同。
>    - 這個檔案在 `specs/` 底下，依律法清冊屬於規格類，修改要修法紀錄。
>    - 檔案不存在、或缺少下面任何一項，`spec_lint` 判為無法評估，不使用預設值。預設值會讓本專案的假設以另一種形式留在 `harness-core/`。
> 2. **操作者的欄位（G-07）。**
>    - `actor_fields`：`examples.yaml` 的 `actors` 每一項允許的欄位與型別。型別用欄位表既有的詞彙（`str`、字串清單）。
>    - `usecase_actor_fields`：`usecases.yaml` 的 `actor` 允許的欄位與型別。
>    - 欄位表的其他部分不變，仍然封閉：`actors` 只是改成依設定檔決定哪些欄位合法，設定檔沒列的欄位照樣擋下。
>    - 本專案的設定照現狀：`actor_fields` 是 `user`（字串）、`roles`（字串清單）、`hospital`（字串）；`usecase_actor_fields` 是 `roles`（字串清單）。
> 3. **錯誤分類（G-03）。**
>    - `error_categories`：`error_codes[].category` 允許的值。
>    - 本專案照現狀列五種：`denied`、`not_found`、`invalid_input`、`conflict`、`rule_violation`。
>    - `specs/README.md` 規則 7 改寫成「`category` 只能是設定檔列出的值」；「分類對應 HTTP 狀態碼」的說明移到第七節（HTTP 契約），因為只有提供 HTTP 介面的專案需要。
> 4. **guard 的順序（G-04）。**
>    - `specs/README.md` 規則 5 改寫成「`guards` 的順序就是檢查的順序」，不再寫死 D-024。
>    - 順序的依據寫在設定檔的 `guard_order`（自由文字，附出處）。本專案寫「角色 → 載入 → 院區範圍 → 領域規則（D-024）」。
>    - `spec_lint` 不檢查順序，這一點與現狀相同；`guard_order` 只給人看。
> 5. **不變的部分。**
>    - 規格檔仍寫 `spec_version: 1`，現有規格一個字都不改。
>    - 執行器（`ExampleRunnerTest`）不動：它本來就是本專案的程式，依本專案的操作者建立使用者。
>    - 範例的語法、引用完整性、分辨不了情境的範例，都與 D-063 相同。
> 6. **驗證。**
>    - 現有規格在 v1.2 下維持零問題。
>    - 以 `docs/harness/samples/card-v2-trial/` 的 `actors` 寫法為測試資料：設定檔允許 `program`、`user` 時不擋；本專案的設定檔仍然擋下 `program`。
>    - 拿掉設定檔、或拿掉其中一項，要判為無法評估。
>    - `harness-core/VERSION` 升為 0.7.0。
> 7. **順序。** 一個修法 PR：`decision-log.md`、`specs/README.md`、`specs/spec-profile.yaml`、`harness-core/spec_lint.py` 與它的測試、修法紀錄。不夾帶程式（P4）。
>
> ### Relationship / Evidence
>
> - 起因：格式 v2 草案第十一節的缺口二。依 Q-9，卡片規格放在 iccard 的 repo 並安裝 `harness-core/`；G-07 不改，iccard 寫 `program` 就被擋。
> - 前例：D-063 第 2 點要求欄位表內建在 `harness-core/` 以保持可攜。本裁決把欄位表裡屬於專案的部分移出來，其餘照舊內建。
> - 相關裁決：D-024（guard 順序，內容不變，只是改由設定檔引用）、D-061（HTTP 契約的 `status_by_category`）。
>
> ### Mapping
>
> - `specs/spec-profile.yaml`、`specs/README.md` 第三、七、八節
> - `harness-core/spec_lint.py`、`harness-core/tests/test_spec_lint.py`
> - 後續：缺口一的語法（v2.1 前補進第四節）；格式 v2.0、v2.1

### 待負責人決定

1. **設定檔的位置與名稱。** 草案用 `specs/spec-profile.yaml`。另一個做法是寫進律法清冊（像 `@decision-log`），但清冊目前只放路徑，不放格式內容。
2. **沒有設定檔時要不要有預設值。** 草案選「不給預設值，判為無法評估」。代價是任何安裝 `harness-core/` 的專案都要先寫設定檔。
3. **`http-common.yaml` 的 `status_by_category` 要不要核對涵蓋了每一種分類。** 草案沒有放進來：那是 D-061 的範圍，目前也沒有出錯。

### 這份草案沒有處理的

- 出處的種類（`design_decision` 指 `docs/02-ddd/`、`example`／`scenario` 指 `docs/04-testing/`）也帶有本專案的目錄。程式已經允許專案不設定（不設定時，用到這兩類出處的規則會被列為問題），iccard 可以只用 `decision` 與 `doc`，所以不擋，不列入本草案。
- 禁用詞彙依專案分開（第八節），與 `harness-core/` 拆成獨立 repo 一起處理。
