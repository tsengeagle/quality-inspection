# domain 事件：禁則

狀態：**草稿**（2026-11-28）。DE-P4、DE-P5、DE-P6 依據的 D-098 尚未生效。

| 編號 | 禁則 | 取代的寫法 | 正本 | 閘門 | 現況 |
|---|---|---|---|---|---|
| DE-P1 | 不得在事件裡取系統時間或產生 id | 事件的建構子自己取 `Instant.now()`、`UUID.randomUUID()` | D-092；ARCH-02 | ARCH-02 | 基線是空的（#198） |
| DE-P2 | 不得有 setter 或可變的欄位 | 可變的資料物件 | 無，本目錄提出 | 無 | 沒有量 |
| DE-P3 | 規格 `success.emits` 宣告的事件，不得沒有對應的 `…Event` 類別與同名屬性的 getter | 用一個通用的事件類別帶 map | `specs/README.md` 第十節 | 範例執行器（`emits`、`emits_include`） | 範例全過 |
| DE-P4 | **（D-098）** 不得在事件裡產生 JSON 字串：不用 `String.format`、字串相加、`ObjectMapper` | 在建構子裡拼出 `detail` | D-098 第 1 點（**尚未生效**） | 無。D-098 第 7 點：建構完成後 ARCH-06 加上 `domain`，擋得到 Jackson；擋不到 `String.format` 與字串相加 | 26 個不符（27 個有 `detail` 的事件裡，1 個是 use case 傳入字串） |
| DE-P5 | **（D-098）** `detail` 的值不得是字串、整數、`BigDecimal`、布林、`null` 以外的型別 | 把物件或清單放進去 | D-098 第 2 點（**尚未生效**） | 無 | 不適用（還沒有改成 map） |
| DE-P6 | **（D-098）** 不得由 use case 組好 `detail` 的字串再傳給事件 | use case 決定稽核的內容 | D-098 第 6 點（**尚未生效**） | 無 | 1 個（`CaseClosedWithoutRereviewEvent`） |
| DE-P7 | `domain` 的事件不得依賴 `application`、`infrastructure`、`interfaces` | 事件引用 use case 的型別 | D-048 | ARCH-01、ARCH-04 | 無舊違規 |
| DE-P8 | 不帶時區的時間不得進 `detail` | `LocalDateTime` 的字串 | D-072、D-075 | 範例比對 `detail` 的字串（8 種 action）；其餘事件無 | 違憲清單已全部修正（D-075 第 12 點） |

DE-P4 提醒：D-098 第 7 點的閘門只擋 Jackson。「用 `String.format` 拼 JSON」正是 17 個事件現在的寫法，也是會寫出不合法 JSON 的那一種，改完之後沒有閘門防止它再出現。要不要另外加規則（例如事件類別不得呼叫 `String.format`），要在 D-098 的修法二裁決。
