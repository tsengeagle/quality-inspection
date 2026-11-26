# 事件的 `detail` 改成結構化

狀態：**草稿，待裁決**（2026-11-26 起草，main = `5d1f462`）。本文件不是律法；題號 E-1～E-8 只在本文件使用。

題目清單待辦「事件的 `detail` 改成結構化」（負責人 2026-11-26 同意列入）。起因與四個選項的比較在 `45-response-dto-and-list-shape-draft.md` 第六節；本文件補完該節「動工前要確認的」五件，並把選項 C 展開成可以裁決的題目。

## 一、問題

domain 的事件在建構子裡自己組出稽核紀錄的 JSON 字串（`DomainEvent.detail`），`AuditTrailListener` 原樣寫進資料表。

- 「稽核紀錄怎麼序列化」是儲存格式的決定，放在 domain。
- 組字串的寫法有三種並存，其中兩種遇到自由文字會產生不合法的 JSON。
- 為了修這種缺陷，5 個事件引入了 Jackson（D-080 第 5 點），domain 因此依賴序列化函式庫。

## 二、查到的事實

**讀程式、規格與測試，沒有實測。** 草稿 45 第六節的數字有兩處不準，這裡更正（事實 2）。

### 現況

1. `DomainEvent`（`domain/common`）的建構子收 `String detail`，`getDetail()` 回傳字串。`AuditTrailListener`（`infrastructure/audit`）做 `entity.setDetail(event.getDetail())`。資料表欄位是 `detail clob`（`V1__baseline_schema.sql`）。
2. `domain` 底下有 32 個事件類別（不含基底）。產生 `detail` 的方式：

   | 方式 | 數量 | 事件 |
   |---|---|---|
   | `String.format` | 17 | `InitialReviewCompleted`、`InitialReviewFixed`、`InitialReviewStarted`、`RereviewCompleted`、`RereviewStarted`、`RereviewerAssigned`、`ReviewCaseAssigned`、`ReviewCaseClosed`、`ReviewCaseEscalatedToRereview`、`ReviewCaseReopened`、`ReviewCasesSelected`、`ReviewDraftSaved`、`ReviewSubmissionSubmitted`、`SamplingBatchCompleted`、`SamplingBatchFailed`、`SamplingBatchStarted`、`SamplingCandidatesPrepared` |
   | 字串相加 | 4 | `ScoreSheetBoundToProject`、`ScoreSheetDraftCreated`、`ScoreSheetDraftUpdated`、`ScoreSheetVersionPublished` |
   | Jackson 的 `ObjectMapper` | 5 | `RereviewRevised`、`ReviewCaseCancelled`、`ReviewCaseReassigned`、`ReviewCaseReturned`、`ReviewSubmissionRevised` |
   | 由 use case 組好字串傳進來 | 1 | `CaseClosedWithoutRereview`（`CloseWithoutRereviewUseCase` 第 46 行的 `String.format`） |
   | `null`（沒有 `detail`） | 5 | `ReviewProjectActivated`、`ReviewProjectCreated`、`ReviewProjectDeactivated`、`ReviewProjectDraftUpdated`、`ReviewProjectVersionPublished` |

   更正草稿 45：事件類別是 32 個不是 33 個；「其餘 11 個」是上表後三列加起來的 10 個。分類是用字串樣式掃出來的，只打開看了其中 11 個檔案。

3. **有一個潛在的缺陷**：`SamplingBatchFailedEvent` 用 `String.format("{\"failureReason\":\"%s\"}", failureReason)` 組字串，`failureReason` 是自由文字。內容有引號、反斜線或換行時，寫進稽核表的會是不合法的 JSON。目前沒有任何行為會產生失敗的批次（D-086 第 1、2 點），所以走不到；母體來源的介接（題目清單待辦）做了之後就走得到。這與 D-080 第 5 點修掉的是同一種缺陷。
4. 數字的寫法：分數用 `%s` 帶入 `BigDecimal`，輸出它的 `toString()`（例如 `10.00`）；件數用 `%d`。字串型的 id 都直接包在引號裡，沒有跳脫。

### `detail` 被誰讀

5. 寫入之後，`detail` 只被當成一整個字串處理，後端沒有任何地方解析它：
   - 稽核查詢（`AuditTrailQueryAdapter`）讀出字串，放進 `AuditTrailEntry.detail`。
   - 遮罩（`ScoredDetailMask`，D-071、D-078）依 action 的名稱決定要不要把整個 `detail` 設成 null，不看內容。
   - HTTP 契約把它寫成 `detail: 'text?'`（`specs/review-case/http.yaml`）。
6. 前端沒有解析 `detail`（在 `src/app` 找不到對稽核 `detail` 的 `JSON.parse` 或欄位存取）。**這一點只用關鍵字找過，沒有逐頁確認。**

### 律法與測試怎麼釘住 `detail`

7. `specs/review-case/examples.yaml` 有 11 處直接比對 `detail` 的字串，涵蓋 8 種 action：

   | action | 範例比對的字串 |
   |---|---|
   | `ASSIGNED` | `{"reviewerId":"u-r1","dueAt":"2026-11-26T23:59:59Z"}` |
   | `REOPENED` | `{"reviewerId":"u-r1","dueAt":"2026-11-26T23:59:59Z"}` |
   | `REASSIGNED` | `{"previousReviewerId":"u-r1","newReviewerId":"u-r2","dueAt":"2026-11-28T23:59:59Z"}` |
   | `SUBMITTED` | `{"caseId":"${c}","totalScore":20,"reviewerId":"u-r1"}` |
   | `REREVIEW_SUBMITTED` | `{"rereviewScore":15,"finalScore":10.00}` |
   | `CASE_CLOSED_WITHOUT_REREVIEW` | `{"finalScore":5}` |
   | `RETURNED` | `{"reason":"a\"b\\c"}` |
   | `CANCELLED` | `{"reason":"第一行\n\"第二行\""}` |

   鍵的順序、數字的格式、跳脫的寫法都被釘住。
8. 有 `detail` 的事件是 27 個；範例比對到的 action 是 8 種。其餘的事件，`detail` 的內容沒有被範例釘住。
9. 範例執行器不讀 `DomainEvent.getDetail()`，是透過稽核查詢的結果比對字串。事件的屬性另由 `events` 的 `with:` 比對（那是事件自己的欄位，不是 `detail`）。
10. `acceptance/AcceptanceWorldTest`（律法，gate）自己定義了一個測試用事件，呼叫基底建構子時 `detail` 傳 `null`。
11. 律法以外的測試有 16 個檔案用到 `getDetail()` 或 `detail`，共 55 處呼叫 `getDetail()`；其中 `domain/SliceEventPropertiesTest`、`domain/RereviewSliceEventPropertiesTest` 逐一比對事件的 `detail` 字串（17 處）。這些測試比範例涵蓋更多的事件，**但涵蓋到 27 個中的幾個沒有數。**
12. `docs/02-ddd/domain-events.md` 有幾處寫出 `detail` 的內容（例如 `RETURNED` 是 `{"reason":"…"}`）。它是權威文件，不是律法。

## 三、目標的形狀

選項 C（草稿 45 第六節）展開之後：

```
domain 事件          帶結構化的資料（鍵與值），不產生 JSON
  ↓
AuditTrailListener   把結構化的資料序列化成 JSON 字串，寫進 detail 欄位
  ↓
稽核表、查詢、HTTP    與現在相同：detail 是一個字串
```

對外完全不變：資料表、稽核查詢的回應、HTTP 契約、前端都不動。變的只有「字串在哪裡產生」。

## 四、待裁決

### E-1　做不做、做到哪裡

| 選項 | 作法 |
|---|---|
| **A（建議）** | 做 C：全部 27 個有 `detail` 的事件改成帶結構化的資料，序列化移到 `AuditTrailListener`；做完後 domain 不再依賴 Jackson |
| B | 只修事實 3 的潛在缺陷（`SamplingBatchFailedEvent` 改用 `ObjectMapper`），其餘不動 |
| C | 統一用 Jackson：21 個拼字串的事件都改用 `ObjectMapper`，明文允許 domain 事件依賴 Jackson |
| D | 不做 |

A 的理由見草稿 45 第六節。補充：事實 3 讓「不做」的代價變得具體，母體來源介接之後，它就是一個會發生的缺陷。

B 是最小的止血。若 A 排不進來，至少做 B；B 是修缺陷，依據是 D-080 第 5 點。

### E-2　事件用什麼型別帶結構化的資料

| 選項 | 作法 | 優點 | 缺點 |
|---|---|---|---|
| **A（建議）** | 基底建構子收 `Map<String, Object>`，事件用保留順序的 `LinkedHashMap` 建立，基底存成不可變的副本 | 改動小；鍵的順序由建立的順序決定，與現在的字串一一對應 | 沒有型別保護，值放什麼都可以 |
| B | 每個事件一個自己的 detail 型別（小類別或 record） | 有型別 | 27 個事件各一個型別，而事件本身已經有這些欄位，是重複 |
| C | 不另外帶：`AuditTrailListener` 依事件的 getter 自己組 | domain 最乾淨 | 鍵名、順序、哪些欄位要進稽核，變成 infrastructure 的知識；現在的鍵名與事件的欄位名並不都相同（例如 `REREVIEW_SUBMITTED`） |

A 的理由：「哪些資料進稽核紀錄、叫什麼名字」是業務上的決定（D-070、D-071 裁決過），應該留在 domain；「怎麼變成字串」才是 infrastructure 的事。A 剛好切在這條線上。

A 之下允許的值：字串、整數、`BigDecimal`、布林、`null`。巢狀結構目前沒有任何事件用到，不開放；需要時另外裁決。

### E-3　輸出的字串要不要與現在逐字相同

| 選項 | 作法 |
|---|---|
| **A（建議）** | 逐字相同。鍵的順序、數字的格式（`10.00` 不變成 `10.0` 或 `10`）、跳脫的寫法都不變 |
| B | 允許格式上的差異（例如數字的小數位數），範例跟著改 |

A 的理由：已經寫進稽核表的紀錄不會重寫，新舊紀錄的格式一致，之後要解析才不用處理兩種。而且 A 讓這次修改成為純重構，11 處範例不用動，不需要為了範例修法。

A 的唯一例外是事實 3 那一類：原本不合法的 JSON 會變成合法的。這是修正，不是差異。

### E-4　怎麼證明逐字相同

範例只釘住 8 種 action（事實 7、8），其餘的要另外想辦法。

| 選項 | 作法 |
|---|---|
| **A（建議）** | 動工前先加一組「特徵測試」：對 27 個事件各建一個實例，把現在的 `detail` 字串寫成預期值。測試先在現在的程式上全綠，重構之後必須仍然全綠 |
| B | 只靠現有的範例與測試（事實 7、11） |

A 的理由：這是重構前先把行為釘住的標準作法。現有的 `SliceEventPropertiesTest` 已經涵蓋一部分，A 是把它補到 27 個都有。

特徵測試放在律法以外的測試目錄，是建構 PR 的一部分，不需要簽章。它要包含會暴露差異的值：有小數的分數、整數的分數、`null`、含引號與換行的文字。

### E-5　`AuditTrailListener` 用什麼序列化

| 選項 | 作法 |
|---|---|
| **A（建議）** | 用一個專屬的、設定固定的 `ObjectMapper`（不用 Spring 容器裡共用的那一個） |
| B | 用 Spring 容器裡的 `ObjectMapper` |

A 的理由：共用的 `ObjectMapper` 的設定會影響 HTTP 的輸出，之後有人為了 HTTP 改了它（例如數字或日期的寫法），稽核紀錄的格式會跟著悄悄改變。稽核的格式應該只由自己的設定決定。

`BigDecimal` 的寫法（事實 4）要在特徵測試裡確認：現在是 `toString()`，Jackson 的預設也是，但科學記號（例如 `1E+2`）的情況兩邊是否一致，**起草時沒有試**。

### E-6　基底建構子怎麼換

`AcceptanceWorldTest`（律法）傳 `detail = null`（事實 10）。

| 選項 | 作法 | 要不要修法 |
|---|---|---|
| **A（建議）** | 直接把參數的型別從 `String` 換成 `Map<String, Object>`，只留一個建構子。`null` 對兩種型別都合法，`AcceptanceWorldTest` 不用改 | 不用 |
| B | 新舊建構子並存一段時間（D-092 第 6 點之 3 的作法） | 要：`null` 會同時符合兩個建構子而編譯失敗，`AcceptanceWorldTest` 要改 |

A 的代價：27 個事件要在同一個 PR 改完，不能分批。考慮到每個事件的修改都很機械，可以接受。

`CaseClosedWithoutRereviewEvent`（事實 2）現在由 use case 傳字串進來，改成由事件自己從 `finalScore` 建立，與其他事件一致。

### E-7　做完之後的閘門

| 選項 | 作法 |
|---|---|
| **A（建議）** | ARCH-06（D-096）的範圍加上 `domain`：`domain`、`application`、`reporting` 都不得依賴 Jackson。那時 `domain` 的基線是空的 |
| B | 另立一條規則 |
| C | 不加 |

A 需要修法（`architecture/` 是律法），而且要等 D-096（#194）先合併。

另外可以考慮、但建議不在本題做的：禁止 domain 與 application 用 `String.format` 或字串相加產生以 `{` 開頭的字串。這種規則用 ArchUnit 不好寫，誤判的機會高。

### E-8　分批

全部照建議時：

1. **修法 PR 一**（要簽章）：本裁決。只有 `decision-log.md`、題目清單、草稿；沒有規格或範例的修改（E-3 選 A，行為不變）。
2. **建構 PR**（D-097 生效後，要等修法 PR 一合併）：
   - 先加特徵測試，在現在的程式上全綠（E-4）。
   - 基底、27 個事件、`CloseWithoutRereviewUseCase`、`AuditTrailListener`、受影響的測試。
   - `domain-events.md` 的說明（事實 12）若有「以 JSON 序列化產生」這類描述，改成現況。
3. **修法 PR 二**（要簽章）：E-7 的 ARCH-06 範圍、違規樣本。

若負責人想先止血：E-1 的 B（只修 `SamplingBatchFailedEvent`）可以獨立成一個小的建構 PR，依據 D-080 第 5 點，不必等本裁決。

## 五、建議總表

| 題 | 建議 |
|---|---|
| E-1 | A：全部 27 個事件改成結構化；排不進來時至少做 B 止血 |
| E-2 | A：`Map<String, Object>`，保留順序，不開放巢狀 |
| E-3 | A：輸出逐字相同 |
| E-4 | A：動工前先補特徵測試到 27 個事件 |
| E-5 | A：專屬的 `ObjectMapper` |
| E-6 | A：直接換型別，一次改完 |
| E-7 | A：ARCH-06 的範圍加上 `domain` |
| E-8 | 修法 PR 一（裁決）→ 建構 PR → 修法 PR 二（閘門） |

## 六、風險

1. **已經寫進資料表的舊紀錄不會變**。事實 3 的缺陷目前走不到，所以推定資料表裡沒有不合法的 JSON；但 D-080 第 5 點修正之前寫入的退回、取消紀錄可能有。正式環境還沒上線（D-045），影響只在開發與測試的資料。
2. **鍵的順序靠 `LinkedHashMap` 的建立順序**。有人把它換成一般的 `HashMap`，順序會變、而只有被範例或特徵測試涵蓋的事件會被發現。E-4 的特徵測試涵蓋全部 27 個，就是為了這一點。
3. **一次改 27 個事件**（E-6）是一個大的 PR。每個修改都機械，但審查的量不小。
4. 事件除了進稽核表，也透過 Spring 的事件機制發布（`DomainEvent` 繼承 `ApplicationEvent`）。**有沒有其他的監聽者讀 `getDetail()`，只找過 `src/main`，找到的只有 `AuditTrailListener`。**

## 七、起草時沒有確認的

1. 事件的分類是用字串樣式掃的，沒有逐一打開 32 個檔案（事實 2）。
2. 前端有沒有解析 `detail`，只用關鍵字找過（事實 6）。
3. 現有的測試涵蓋了 27 個事件中的幾個（事實 11）。
4. `BigDecimal` 在科學記號情況下的輸出（E-5）。
5. `Map` 經過 Jackson 序列化之後，字串的跳脫寫法是否與現在的 5 個 Jackson 事件完全相同。它們現在就是用 `ObjectMapper` 序列化 `Map`，推定相同，沒有試。
6. 拼字串的事件裡，字串型的值現在沒有跳脫（事實 4）。改用序列化之後，值裡若有需要跳脫的字元，輸出會與現在不同（現在是不合法的 JSON）。id 類的值推定不會有這種字元，沒有驗證。
