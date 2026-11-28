# 模式語言的四形式盤點

狀態：**草稿，待裁決**（2026-11-28，main = `02811a3`）。本文件不是律法；題號 M-1～M-7 是本草稿自己的編號。負責人 2026-11-28 同意先做盤點；盤點之後要不要立法、怎麼立，都還沒有裁決。

對應 `03-harness-plan.md` 第六節的第 6 期（「把在地傳統寫成模式語言」，義務 P3，狀態「未動工」）。

## 一、問題

論文要求每個模式以四種形式存在：準則、範本、禁則、閘門，缺一種就視為不存在（`01-mgd-concepts.md`「四形式模式語言」）。本專案的義務對照表在 P3 這一列寫「只有文件」。

問題側現在沒有暫准的範圍（D-089），接下來排隊的是幾批建構：D-098 要改 27 個事件，D-101 要新增一支查詢 use case，D-100 要改列表與匯出。這些建構怎麼寫，目前靠建構者模仿旁邊的檔案。

這份草稿回答三件事：

1. 後端現在實際上有哪些在地的寫法。
2. 每一種寫法，四種形式各有沒有、放在哪裡。
3. 要立法的話，先立哪幾個、放在哪裡。

範圍只有後端。前端沒有盤點。

## 二、查到的事實

**作法**：讀裁決、計畫文件與架構規則的程式；對 `services/quality-inspection-service/src/main/java` 用字串樣式計數；完整打開讀過的程式檔只有 6 個（`AssignReviewCaseUseCase`、`GetRevisionHistoryUseCase`、`ReviewCaseAssignedEvent`、`DomainEvent`、`ArchitectureRules`、`ReviewCaseRepositoryImpl` 的前 60 行）。**沒有執行任何測試。** 計數是樣式比對的結果，不是逐檔確認的；各項的限制寫在第七節。

### 2.1 use case 的骨架很一致

`application` 與 `reporting` 底下名稱以 `UseCase` 結尾的類別共 52 個（`reporting` 2 個）。

| 寫法 | 符合的數量 |
|---|---:|
| 類別標 `@Service` | 52 / 52 |
| 唯一的 public 方法叫 `execute` | 52 / 52 |
| `execute` 的第一個參數是 `ActorContext actor` | 52 / 52 |
| 參數是原始型別逐一傳入，沒有 Command／Input 物件 | 52 / 52 |
| 類別層有 `@Transactional` | 51 / 52（沒有的是 `PreviewSamplingPopulationUseCase`） |
| 發布事件經過 `DomainEventPublisher`，沒有直接用 Spring 的發布器 | 30 支發布事件，0 支直接用 |
| 發布事件的 use case 同時注入 `Clock` 與 `IdGenerator`（D-092） | `Clock` 31、`IdGenerator` 30、發布事件 30 |

檢查順序（D-024：角色 → 載入 → 院區範圍 → 領域規則）用一支粗略的腳本比過行號，50 支 `application` 的 use case 沒有找到順序顛倒的。腳本只認得 `requireRole`、`requireAnyRole`、`find…`、`requireHospitalScope` 這幾個字樣。

回傳的型別：`application`、`reporting` 底下名稱以 `Result`、`View`、`Summary`、`Entry` 結尾的類別 38 個，都是一般的 class，沒有 record。

### 2.2 寫法分歧的地方

| 地方 | 事實 | 有沒有裁決 |
|---|---|---|
| 事件怎麼組 `detail` | 32 個事件類別：`String.format` 17 個、Jackson 5 個（草稿 47 的分類更細） | D-098（#201，未生效） |
| 查詢的 use case 用不用寫入側的 repository | 名稱以 `Get`、`List`、`Preview`、`Export` 開頭或含 `Dashboard` 的 use case 22 支：9 支匯入了 `…Repository`，13 支沒有 | D-048 的 Pending 寫「查詢側不得依賴寫入用的 repository」尚未寫成閘門 |
| 查詢的 use case 是不是唯讀交易 | 22 支裡 18 支是 `@Transactional(readOnly = true)`。其餘 4 支裡，`GetRevisionHistoryUseCase` 是刻意可寫入（D-055 第 5 點，程式裡有註解）；另外 3 支沒有看 | D-048 的 Pending：「查詢處理器一律是唯讀交易」尚未寫成閘門 |
| controller 回傳什麼 | `ReviewProjectController` 有 `Map<String, String>`、`Map<String, Object>` 的回傳 | D-096（#194，未生效） |
| `domain` 與 `application` 的 `IllegalStateException` | 8 處：5 處在事件裡包 Jackson 的例外，2 處在 `ScoreCalculationService`，1 處在 `ProjectVersion` | ARCH-03 只禁 `IllegalArgumentException`。事件的 5 處會隨 D-098 的建構消失 |

命令側沒有反向的問題：30 支不是查詢的 use case，沒有一支匯入 `.query.` 套件。

### 2.3 `domain` 對外的依賴

- `domain` 底下匯入 `org.springframework` 的檔案只有 1 個：`domain/common/DomainEvent.java`，它繼承 Spring 的 `ApplicationEvent`。
- `domain` 底下沒有匯入 `javax.persistence`、`jakarta.persistence`。整個 main 沒有匯入 lombok。
- 這兩件「沒有」現在沒有閘門看著：ARCH-01 只看本專案五層之間的方向（`consideringOnlyDependenciesInLayers`），不看對外部函式庫的依賴。ARCH-06（#194，未生效）只管 `application`、`reporting` 對 Jackson。

### 2.4 準則散在哪裡

沒有一份文件是「後端怎麼寫」的正本。和寫法有關的內容分散在：

| 位置 | 內容 | 是不是律法 |
|---|---|---|
| `decision-log.md` | D-024（檢查順序）、D-048（CQRS）、D-072、D-075（時間）、D-092（事件的時間與 id）、D-088（HTTP 介面）；未生效的 D-096、D-098 | 是 |
| `specs/README.md` 第十節 | 埠 `X` 對到介面 `XPort`、要依介面注入；事件 `X` 對到類別 `XEvent` | 是 |
| `spec-profile.yaml` | `guard_order` 的文字（`spec_lint` 不檢查順序）、錯誤代碼的分類 | 是 |
| `architecture/ArchitectureRules.java` | ARCH-01、02、03、05 的規則與理由（`because(...)`） | 是（gate） |
| `services/quality-inspection-service/README.md` §6、§7、`CLAUDE.md` | 各層的責任與落點規則 | 這次沒有查它在不在清冊 |
| `docs/02-ddd/application-service.md`、`domain-events.md` | 這次沒有讀 | — |

### 2.5 範本

找不到任何範本。全 repo（不含前端）檔名帶 `template` 的只有 `docs/04-testing/test-scenario-template-and-guidelines.md`，是測試情境的格式，不是程式骨架。

最接近範本的是架構規則的合規對照樣本（`architecture/samples/` 底下名稱以 `Allowed` 開頭的 5 個檔案）。它們會被編譯、被測試執行，但只示範「這樣不算違規」，不是完整的骨架。

### 2.6 D-048 列出、還沒有寫成閘門的規則

D-048（2026-11-14）的 Pending 原文列了五條：

1. 查詢側不得依賴寫入用的 repository 或命令 use case。
2. 命令側不得依賴查詢的 view。
3. 查詢處理器一律是唯讀交易。
4. 每個查詢處理器都必須帶授權註解並接收範圍條件。
5. 原生 SQL 只能出現在 adapter。

對照 2.2：第 1 條現在有 9 支不符；第 2 條 0 支不符；第 3 條至少 1 支是有裁決的例外。第 4、5 條這次沒有量。

## 三、四形式盤點表

候選的模式是從程式的實際寫法歸納的，不是既有的清單。「準則」欄只列出處；「—」表示找不到。

| # | 模式 | 準則 | 範本 | 禁則 | 閘門 | 有幾種 |
|---|---|---|---|---|---|:-:|
| 1 | 分層與依賴方向 | D-048；後端 README §6、§7 | — | ARCH-01、ARCH-04 的允許方向 | ARCH-01、ARCH-04，各有違規樣本 | 3 |
| 2 | 時間與 id 由外部傳入 | D-072、D-075、D-092 | —（只有合規對照 `AllowedReadsInjectedClock`） | ARCH-02 | ARCH-02 與樣本；範例執行器固定時鐘 | 3 |
| 3 | 錯誤用自己的例外，對到錯誤代碼的分類 | `spec-profile.yaml`、`http-common.yaml` | — | ARCH-03（只禁 `IllegalArgumentException`） | ARCH-03 與樣本；範例的 `code`；HTTP 契約 | 3 |
| 4 | 命令 use case（`execute`、檢查順序、存檔後發布事件） | D-024、D-092 | — | — | 範例只驗結果，不驗寫法 | 1 |
| 5 | 查詢 use case 與查詢埠（view 在 `application`，adapter 在 `infrastructure`） | D-048 | — | D-048 的 Pending 五條（文字） | ARCH-05 只管 `reporting` | 1～2 |
| 6 | domain 事件（基底類別、屬性、`detail`） | D-092；D-098（未生效） | — | D-098（未生效） | 範例的 `emits` 驗屬性；`detail` 的格式沒有 | 1 |
| 7 | 埠（`XPort`，依介面注入） | `specs/README.md` 第十節 | — | 同左（「依實作類別注入」） | 範例執行器：依實作類別注入時啟動失敗 | 3 |
| 8 | aggregate 的建立與還原（private 建構子、工廠方法、`reconstitute`） | — | — | — | — | 0 |
| 9 | 持久化（repository 介面在 `domain`；實作、JPA entity、對應在 `infrastructure`） | D-048 第 3 點（只講讀取模型） | — | — | ARCH-01 擋反向依賴；`domain` 用 JPA 註解不會被擋 | 1 |
| 10 | 授權與院區範圍（`ActorContext.requireRole`、`requireHospitalScope`、讀取用 policy） | D-042、D-047、D-048 第 5 點 | — | D-048 第 5 點（不得只靠切面） | 範例的矩陣驗結果 | 2 |
| 11 | HTTP 介面（`@CurrentActor`、`ApiResponse` 外殼、請求與回應 dto） | D-088；D-096（未生效） | — | D-096（未生效） | HTTP 契約比對器；涵蓋清單逐檔登記 | 2 |
| 12 | 稽核寫入（listener 接所有事件） | D-071、D-078；D-098（未生效） | — | — | 範例驗遮罩的結果 | 1 |
| 13 | use case 測試的寫法 | — | — | — | — | 0 |

第 8、13 項只確認了「有這種寫法」，沒有量一致的程度（第七節）。

讀這張表：

- **範本是 0 / 13。** 這是離四形式最遠的一欄。
- 四種形式有三種的是第 1、2、3、7 項。它們都是「先有閘門，準則寫在裁決裡」。
- 第 4 項（命令 use case）只有準則，卻是最一致的寫法（2.1）。骨架靠模仿就維持住了。
- 出現分歧的第 5、6、11 項，都是後來補裁決收拾的：D-048 之後 `reporting` 沒有搬完；D-098、D-096 還在等簽章。

## 四、從盤點得到的判斷

以下是起草者的判斷，不是事實。

1. **不需要把 13 項都寫成模式。** 論文說模式網路是選擇性的，只需要寫下模型的預設無法可靠提供的在地選擇。2.1 顯示 use case 的骨架不寫範本也守得住。該優先的是已經出現分歧、或接下來要大量建構的地方。
2. **第一批建議三個**：
   - **domain 事件**（第 6 項）：D-098 的建構要改 27 個類別。沒有範本的話，27 個由建構者各寫一次。
   - **查詢 use case 與查詢埠**（第 5 項）：D-101 要新增一支查詢；D-048 的五條規則擱了兩週；現在有 9 支不符第 1 條。
   - **命令 use case**（第 4 項）：計畫裡的「範本 use case」。它已經一致，寫成範本的成本最低，可以用來定四形式的格式。
3. **第 2、3、7 項只差範本**，內容小，可以排在第二批。
4. **`domain` 不得依賴持久化與框架的函式庫**（2.3）現在是事實，沒有任何東西保護它。這是一條便宜的禁則加閘門。`DomainEvent` 繼承 Spring 的類別是唯一的例外，要裁決它是傳統的一部分還是舊違規。

## 五、待裁決

### M-1 模式文件放在哪裡

| 選項 | 內容 |
|---|---|
| A | `docs/harness/patterns/`，當一般文件 |
| B | repo 根目錄新的 `patterns/`，每個模式一個目錄 |
| C | `services/quality-inspection-service/patterns/`，跟著後端 |

**建議 B。** 模式語言是律法的三樣產物之一，與 `specs/` 並列比較清楚；之後前端有自己的模式時用子目錄分開（`patterns/backend/`）。C 的缺點是後端目錄底下有律法也有產物，清冊要逐一列。

### M-2 列不列為律法、什麼時候

**建議：照 `specs/` 的前例分兩步。** 先放進 `patterns/`、不列入清冊，第一批用過一輪建構之後，再以修法列為律法。理由：

- 2026-11-14 對規格的裁決就是「格式完整、內容穩定之後才生效」。
- 現在列為律法要簽章，鏈上已經有六個修法 PR 在等。
- 還沒用過的準則與範本，第一輪一定會改。

代價：沒有列入清冊之前，建構者可以改模式文件而不被擋。這段期間它只是文件。

### M-3 第一批是哪幾個

**建議：domain 事件、查詢 use case 與查詢埠、命令 use case。** 理由見第四節第 2 點。

### M-4 範本的形式

| 選項 | 內容 |
|---|---|
| A | Markdown 裡的程式區塊 |
| B | 會被編譯的 Java 檔，放在測試原始碼底下（作法同 `architecture/samples/` 的合規對照） |

**建議 B。** 會編譯的範本不會無聲地過期：框架或基底類別改了，範本編不過就會被發現。論文要求範本精確到匯入路徑，Markdown 做不到這個保證。

### M-5 模式與建構的先後

| 選項 | 內容 |
|---|---|
| A | 四種形式都備齊，再開 D-098、D-101 的建構 |
| B | 準則與禁則先寫（只動文件）；範本從第一支建構的成品抽出來；閘門跟著修法二 |

**建議 B。** D-098 的修法二本來就要加「`domain` 不得依賴 Jackson」，那就是事件模式的閘門。先憑空寫範本，容易寫出沒有被真實規格檢驗過的骨架。代價是第一支建構沒有範本可以照，要由負責人多看一眼。

### M-6 D-048 的五條規則要不要趁這次寫成閘門

第 1 條現在有 9 支不符。要寫成閘門，得先裁決這 9 支怎麼辦：

| 選項 | 內容 |
|---|---|
| A | 規則照 D-048 的原文，9 支凍結在基線，之後逐支改成查詢埠 |
| B | 修改 D-048：查詢 use case 可以用 repository 載入單一 aggregate 做授權檢查，只有清單與投影必須走查詢埠 |

**建議：先不選，動工前把 9 支逐一打開看用途，再裁決。** `GetRevisionHistoryUseCase` 用 repository 載入案件做讀取授權，這種用法 B 會允許；其他 8 支沒有看。

### M-7 `domain` 不得依賴持久化與框架的函式庫

**建議：立一條規則**（`domain` 不得依賴 `javax.persistence`、`org.springframework`、lombok、Jackson），`DomainEvent` 繼承 `ApplicationEvent` 凍結在基線，或在準則裡寫明是允許的例外。Jackson 的部分與 D-098 的修法二重疊，可以併在一起。要簽章。

## 六、建議總表

| 題 | 建議 |
|---|---|
| M-1 | 根目錄 `patterns/`，後端放 `patterns/backend/` |
| M-2 | 先不列入清冊；第一批用過一輪建構之後再修法列入 |
| M-3 | domain 事件、查詢 use case 與查詢埠、命令 use case |
| M-4 | 會被編譯的 Java 檔 |
| M-5 | 準則與禁則先寫；範本從第一支建構的成品抽出；閘門跟修法二 |
| M-6 | 先逐一看 9 支，再裁決 |
| M-7 | 立一條規則，與 D-098 的修法二併在一起 |

照建議裁決的話，接下來不需要簽章就能做的：

1. 建立 `patterns/backend/` 與一份格式說明（每個模式的四個檔案各叫什麼、準則要寫哪幾段）。
2. 寫三個模式的準則與禁則。事件的那一份要等 D-098 生效才能定稿，可以先依 #201 的條文起草。
3. M-6 的唯讀查證：9 支查詢 use case 各用 repository 做什麼。

## 七、起草時沒有確認的

1. 計數都是字串樣式比對，沒有逐檔打開。「查詢的 use case」是用類別名稱的開頭判斷的，可能有名稱不符合慣例的查詢被算成命令。
2. 「匯入了 `…Repository`」是比對 `Repository;` 結尾的 import 行，沒有確認匯入之後怎麼用。
3. 檢查順序的腳本很粗略（2.1）。它沒有找到顛倒的，不等於沒有。
4. 第 8 項只看了 `ReviewCase`、`ReviewProject` 兩個 aggregate 的工廠方法；第 13 項只數了檔案（`application` 的測試檔 55 個，其中 6 個用到 Mockito、4 個是 `@SpringBootTest`），沒有看其餘的怎麼寫。
5. `docs/02-ddd/application-service.md`、`domain-events.md`、後端 README §6、§7 的內文沒有讀，只知道它們存在。它們和實際寫法一不一致沒有核對。
6. 沒有看參考 repo（ezddd-java）的模式文件長什麼樣子；`02-ezddd-java-lessons.md` 也沒有重讀。M-1、M-4 的建議是依本 repo 的現況推的。
7. 前端沒有盤點。
8. 本草稿沒有加進 `docs/harness/README.md` 的索引，也沒有加進 `25-topic-backlog.md`。兩個檔案都是並行的 PR 容易衝突的地方（#217 正在加草稿 52 的索引；鏈上的 #197、#201 會在題目清單衝突）。等 #217 合併之後再補索引；題目清單要不要加這一題由負責人決定。

## 八、順帶發現

`03-harness-plan.md` 第六節「不在這 7 期裡」的表寫「正式資料庫與 migration 工具｜未決定。卡住樂觀鎖（D-045）」。D-045 在 2026-11-16 已經補上裁決（Oracle 19c 以上、Flyway），狀態寫明「解除 BLOCKED」；`ReviewCaseRepositoryImpl` 也已經用版本欄位做樂觀鎖。計畫文件的這一列過期了，這次沒有改。
