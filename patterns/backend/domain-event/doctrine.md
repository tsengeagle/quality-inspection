# domain 事件：準則

狀態：**草稿**（2026-11-28）。第二節標「D-098」的寫法依據 #201 的條文，**D-098 尚未簽章合併，還沒有生效**；在它生效之前，那幾點不是現在該照做的寫法。D-098 的建構完成之後，本文件要依實際的成品改寫。

## 一、適用範圍

aggregate 的狀態改變之後，要留下稽核紀錄、或要讓別的元件知道的事實。一種事實一個類別。

## 二、寫法

| # | 寫法 | 出處 |
|---|---|---|
| 1 | 類別放在 `domain/<aggregate>/event/`，名稱是 `<事實的過去式>Event`，繼承 `DomainEvent` | 現況；規格的事件 `X` 對到類別 `XEvent`（`specs/README.md` 第十節） |
| 2 | 事件由 use case 建立並發布，不由 aggregate 自己發布 | 現況 |
| 3 | 建構子的前幾個參數固定：`source`、`eventId`、`occurredAt`、操作者的 id 與名稱。`eventId` 與 `occurredAt` 由 use case 傳入，事件自己不產生 | D-092 第 1、2 點 |
| 4 | 傳給基底的固定資料：院區、aggregate 的型別名稱、aggregate 的 id、動作名稱（大寫，例如 `ASSIGNED`） | 現況 |
| 5 | 規格的 `emits` 會比對的屬性，要有同名的 getter | `specs/README.md` 第十節「事件」 |
| 6 | 事件不可變：欄位是 `final`，沒有 setter | 現況 |
| 7 | 哪些資料進稽核的 `detail`、鍵叫什麼、順序，由事件決定 | D-070、D-071；D-098 第 2 點 |
| 8 | **（D-098）** `detail` 是 `Map<String, Object>`，保留建立的順序。值只能是字串、整數、`BigDecimal`、布林、`null`，不用巢狀結構 | D-098 第 2 點 |
| 9 | **（D-098）** 事件不自己產生 JSON 字串。序列化在 `AuditTrailListener`，用它專屬的 `ObjectMapper` | D-098 第 1、5 點 |
| 10 | 時間寫進 `detail` 時是時間點，輸出 UTC | D-072（憲法第一條）、D-075 |
| 11 | 帶分數的 `detail` 要能被遮罩：鍵的名稱要與遮罩的規則一致 | D-071、D-078 |

## 三、為什麼

- 第 3 點：範例執行器固定時鐘時，事件的時間要跟著走；同一支 use case 寫入 aggregate 與事件的時間要相同（D-092 的起因）。
- 第 8、9 點：事件自己拼字串時，值裡有引號、反斜線或換行就會寫出不合法的 JSON。D-080 第 5 點修過一次；`SamplingBatchFailedEvent` 還留著同樣的寫法（D-098「查到的潛在缺陷」）。
- 第 7 點留在 domain、第 9 點移到 infrastructure 的分界：「記什麼」是業務決定，「怎麼變成字串」不是。

## 四、現況（main = `02811a3`，字串樣式計數）

- 事件類別 32 個。
- `detail` 的寫法：`String.format` 17 個、Jackson 5 個；草稿 47 另外分出字串相加 4 個、由 use case 傳入 1 個、沒有 `detail` 5 個。**第 8、9 點現在 0 個符合**，要等 D-098 的建構。
- `DomainEvent` 繼承 Spring 的 `ApplicationEvent`，是 `domain` 唯一匯入 `org.springframework` 的檔案。

## 五、還沒有定的

1. `DomainEvent` 繼承 `ApplicationEvent` 是傳統的一部分，還是要拿掉的舊寫法（草稿 53 的 M-7）。
2. D-098 第 6 點：基底建構子怎麼換、是否一次改完，負責人指示動工前再確定。
3. 事件由 use case 建立（第 2 點）是現況，沒有裁決。事件的內容因此要靠 use case 把值傳對；aggregate 不保證。
4. 需要巢狀結構的 `detail` 出現時怎麼辦（D-098 第 2 點：另外裁決）。
