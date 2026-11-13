# Backend harness 閘門

本目錄放後端閘門讀取的資料：凍結基線與已知失敗清單。規則的程式在
`src/test/java/com/tzuchi/his/quality/harness/`。計畫與義務對照見 `docs/harness/`。

本目錄與規則程式屬於律法的一部分，修改需要 law 負責人核准（`.github/CODEOWNERS`）。

## 指令

從 `services/quality-inspection-service` 執行。Gradle 7.6.4 需要 JDK 11 到 19，本機與 CI 用 JDK 17。

| 用途 | 指令 |
|---|---|
| 完整閘門（CI 跑的就是這個） | `./gradlew check` |
| 以 CI 的嚴格模式在本機跑 | `./gradlew check -PharnessStrict` |
| 只跑架構規則與違規樣本 | `./gradlew test --tests 'com.tzuchi.his.quality.harness.*'` |
| 重建 ArchUnit 凍結基線（修法行為） | `./gradlew test --tests 'com.tzuchi.his.quality.harness.*' -PharnessRefreeze` |

## 閘門怎麼判決

`./gradlew check` 依序做三件事，判決路徑上都是確定性的程式：

1. 編譯。
2. 執行全部測試，包含 ArchUnit 規則與違規樣本。個別測試失敗不會中止 `test`。
3. `verifyTestBaseline` 讀 JUnit 報告，下列任一成立就失敗：
   - 評估到的測試數是 0。
   - 有不在 `known-test-failures.txt` 的失敗。
   - 有被跳過的測試（`@Disabled`、未成立的 assumption）。跳過不算通過。
   - 清單上的測試沒有失敗。修好之後要把那一筆從清單刪掉。用 `--tests` 跑部分測試時不檢查這一項。

每次執行印出一行統計：評估數、通過數、已知失敗數、非預期失敗數、跳過數。

## 架構規則

| id | 規則 | 範圍 | 嚴重度 | 出處 |
|---|---|---|---|---|
| QI-ARCH-001 | 分層依賴方向（見下表） | 五個分層套件 | error | 分層責任：本模組 `README.md` §6.1、`CLAUDE.md`「預期分層」。**方向矩陣本身是 2026-11-13 的提案，尚未裁決** |
| QI-ARCH-002 | 不得呼叫無參數的 `Instant` / `LocalDate` / `LocalDateTime` / `OffsetDateTime` / `ZonedDateTime` `.now()`、`System.currentTimeMillis()`、`UUID.randomUUID()` | `domain`、`application` | error | `docs/harness/03-harness-plan.md` §六；`design-implementation-gap-2026-11-11.md` §3.7 |
| QI-ARCH-003 | 不得建立 `IllegalArgumentException` 或其子類別 | `domain`、`application` | error | `docs/harness/03-harness-plan.md` §六（未對應錯誤代碼，會變成 HTTP 500） |

QI-ARCH-001 允許的方向（列依賴欄）：

| 從 ＼ 到 | domain | application | reporting | interfaces | infrastructure |
|---|:-:|:-:|:-:|:-:|:-:|
| domain | － | ✗ | ✗ | ✗ | ✗ |
| application | ✓ | － | ✗ | ✗ | ✗ |
| reporting | ✓ | ✓ | － | ✗ | ✗ |
| interfaces | ✓ | ✓ | ✓ | － | ✗ |
| infrastructure | ✓ | ✓ | ✓ | ✗ | － |

矩陣是依程式現況中佔多數的方向訂的。待裁決的點：`reporting` 能否依賴 `application`（現況用到
`ActorContext`、`DomainRole`）；`infrastructure` 能否依賴 `reporting`（現況沒有用到）。

### 這些規則不擋什麼

- QI-ARCH-001 只看五個分層之間的依賴。`domain` 依賴 Spring（`DomainEvent` 繼承 `ApplicationEvent`）不在範圍內。
- QI-ARCH-002 不擋 `new Date()`、`Clock.systemUTC()`、`Math.random()`，也不擋經由其他層的工具類別間接取時間。
- QI-ARCH-003 不擋 JDK 內部丟出的 `IllegalArgumentException`（例如 `Enum.valueOf`），也不擋其他未對應的例外（`IllegalStateException`、`NullPointerException`）。
- 三條規則都看編譯後的位元組碼，反射呼叫看不到。

## 違規樣本（P2）

`src/test/java/com/tzuchi/his/quality/harness/samples/` 底下每條規則有一組故意違規的類別和至少一個合規對照。
`ArchitectureRuleSamplesTest` 用同一份規則定義去掃樣本，斷言每個違規類別都被報出、合規對照沒有被報出。
規則被改成永遠通過時，這個測試會失敗。

新增規則時必須同時新增樣本。

## 凍結基線

### `archunit-store/`

ArchUnit `FreezingArchRule` 的基線：規則上線當時已存在的違規。2026-11-13 的筆數：

| 規則 | 基線行數 | 說明 |
|---|---:|---|
| QI-ARCH-001 | 14 | 5 組類別對：application→interfaces、infrastructure→interfaces、interfaces→infrastructure、reporting→infrastructure、reporting→interfaces |
| QI-ARCH-002 | 31 | 分布在 12 個檔案 |
| QI-ARCH-003 | 15 | |

- 基線只記違規的描述，比對時忽略行號，所以移動程式碼不會造成誤報。
- 本機執行時，修掉的違規會自動從基線檔移除，要把基線檔的變更一起 commit。
- CI（或 `-PharnessStrict`）不允許改寫基線：基線與程式現況不一致就失敗。
- 基線不存在時直接失敗，不會自動建立。
- `stored.rules` 以規則的描述文字為索引鍵。改描述等於丟掉該規則的基線。

### `known-test-failures.txt`

先寫測試、實作尚未跟上的紅燈測試。每一筆指回決策編號。這份清單只能縮短；
要新增一筆（例如為新裁決先寫紅燈測試）必須在同一個 PR 寫明對應的決策，並由 law 負責人核准。

## 已做過的驗證（2026-11-13，本機 JDK 17，嚴格模式）

| 操作 | 結果 |
|---|---|
| 不改任何東西跑 `check` | 通過：819 個測試，805 通過，14 個已知失敗，0 個非預期，0 個跳過 |
| 在 `interfaces` 新增一個依賴 `infrastructure` 的類別 | 失敗：`ArchitectureGateTest#layerDependencies` |
| 把 QI-ARCH-003 的判斷式改成永遠不成立 | 失敗：違規樣本測試，以及閘門測試（基線與現況不一致） |
| 沒有基線檔時執行 | 失敗：拒絕自動建立基線 |
| 測試依賴下載失敗（Maven Central 回 429） | 失敗：`test` 沒跑完，不拿舊報告判決 |
