# 命令 use case：禁則

狀態：試行（2026-11-28）。「現況」是字串樣式計數。

| 編號 | 禁則 | 取代的寫法 | 正本 | 閘門 | 現況 |
|---|---|---|---|---|---|
| CUC-P1 | 不得直接取系統時間或產生亂數 id（`Instant.now()`、`LocalDateTime.now()`、`UUID.randomUUID()`、`System.currentTimeMillis()`） | 在需要的地方直接呼叫 | D-092；ARCH-02 | ARCH-02，有違規樣本 | 基線是空的（#198） |
| CUC-P2 | 不得丟 `IllegalArgumentException` | 用 JDK 的例外表示輸入錯誤 | ARCH-03 | ARCH-03，有違規樣本 | 基線是空的（#198） |
| CUC-P3 | 不得沒有角色檢查；不得在載入 aggregate 之後才檢查角色 | 先載入再一起檢查 | D-024 | 無。範例只驗得到結果的錯誤代碼 | 沒有找到不符的（粗略腳本） |
| CUC-P4 | 處理有院區的 aggregate 時，不得沒有 `requireHospitalScope` | 只靠角色，或只靠請求帶的院區 | D-024 | 無。範例有「別的院區被拒絕」的情境 | 沒有量 |
| CUC-P5 | 不得用 Spring 的 `ApplicationEventPublisher` 直接發布事件 | 直接注入框架的發布器 | 無，本目錄提出 | 無 | 0 |
| CUC-P6 | 發出事件時不得用另外取的時間；事件與 aggregate 用同一個 `now` | 建構事件時再取一次時間 | D-092 第 1 點 | 範例 `audit_trails_share_the_time_of_one_use_case` 驗抽選的那一支 | 沒有量 |
| CUC-P7 | 被拒絕時不得已經發出事件 | 先發事件再檢查 | D-065 第 7 點 | 範例執行器（每個 `REJECTED` 的範例） | 範例全過 |
| CUC-P8 | 不得依賴 `interfaces`、`infrastructure` 的類別 | 直接用實作類別 | D-048 | ARCH-01、ARCH-04 | `application` 沒有舊違規 |
| CUC-P9 | 不得依賴 `application/**/query/` 的 view | 命令回傳查詢的投影 | D-048 的 Pending（尚未成為規則） | 無 | 0 |
| CUC-P10 | 不得依賴 Jackson | 在 use case 裡組 JSON | D-096（#194，**尚未生效**） | ARCH-06（#194，尚未生效） | 見 #194 |

沒有閘門的是 CUC-P3、P4、P5、P9。P5、P9 現在是 0 處不符，寫成 ArchUnit 規則不需要基線。
