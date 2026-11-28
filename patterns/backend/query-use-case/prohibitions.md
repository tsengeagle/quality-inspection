# 查詢 use case 與查詢埠：禁則

狀態：試行（2026-11-28）。

| 編號 | 禁則 | 取代的寫法 | 正本 | 閘門 | 現況 |
|---|---|---|---|---|---|
| QUC-P1 | 不得發布事件、不得呼叫 aggregate 會改變狀態的方法 | 查詢順便更新 | 無，本目錄提出 | 無 | 沒有量。D-055 的讀取者紀錄是有裁決的例外 |
| QUC-P2 | 不得依賴命令 use case | 查詢呼叫命令取得資料 | D-048 的 Pending | 無（ARCH-05 只管 `reporting`） | 沒有量 |
| QUC-P3 | 不得用寫入側的 repository 做清單、搜尋或跨資料列的投影；這些一律經過查詢埠 | 在 repository 加查詢方法 | D-048 第 2 點 | 無 | 0（9 支用到 repository 的都是載入單一 aggregate） |
| QUC-P4 | 不得回傳 aggregate 或 JPA entity | 直接把 domain 物件交給 controller | D-048 第 3 點 | HTTP 契約比對器擋得到多出來的欄位；型別本身無 | 沒有量 |
| QUC-P5 | 不得沒有 `@Transactional(readOnly = true)`，除非讀取要留下紀錄並寫明依據 | 不標交易，或一律用可寫入的交易 | D-048 的 Pending | 無 | 1 支不符（`PreviewSamplingPopulationUseCase`） |
| QUC-P6 | 不得沒有角色檢查；讀取有院區的資料時不得沒有 `requireReadScope` 或查詢條件裡的院區 | 只在 controller 檢查 | D-024、D-048 第 5 點 | 無。範例有「別的院區」的情境 | 沒有找到沒有角色檢查的（粗略腳本）；院區沒有量 |
| QUC-P7 | 查詢埠的實作不得放在 `infrastructure` 以外；原生 SQL 不得出現在 adapter 以外 | 在 use case 裡用 `EntityManager` | D-048 第 2 點與 Pending | ARCH-01 擋 `application` 依賴 `infrastructure`；SQL 的位置無 | 沒有量 |
| QUC-P8 | `SYSTEM_ADMIN` 讀得到的查詢，不得沒有封閉的範例（`only`）列出它看得到的欄位 | 只遮分數，其他欄位照給 | D-100（#215，**尚未生效**）新增的題目 | 無 | 漏網紀錄 E-10、E-11 |

「用 id 載入單一 aggregate」是否允許，取決於 `doctrine.md` 第五節第 1 項的裁決。照 D-048 Pending 的原文裁決的話，QUC-P3 要改寫成「不得依賴寫入側的 repository」，現況變成 9 支不符。
