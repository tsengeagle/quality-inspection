# 切片 B：報表與儀表板（`reporting/`）

狀態：**已裁決**（2026-11-25）。負責人裁決：R-4 改為件數不包含已取消的案件，其餘照建議；條文是 `decision-log.md` 的 D-087，以那裡為準。下面保留起草時的版本（main = `e04d782`）。本文件不是律法；題號 R-1～R-10 只在本文件使用。

負責人 2026-11-25 同意的順序：C（D-085，已完成）→ A（D-086，已完成）→ **B（本切片）** → D、E HTTP 介面與共用元件 → 專案版本管理與未裁決事項。

## 一、範圍

| 範圍 | 檔案 | 立法狀態 |
|---|---|---|
| 儀表板摘要 | `reporting/` 的 `DashboardQueryService.java`、`DashboardQueryCriteria.java`、`DashboardSummary.java` | 未立法。行為由 D-042、D-047、D-057 第 4 點與 UI-015 頁面規格描述 |
| 匯出 | `reporting/` 的 `ExportQueryService.java`、`ExportCriteria.java`、`ExportResult.java`、`ExportRow.java` | 未立法。行為由 D-055 第 6 點、D-056、D-057 描述 |
| 匯出用的查詢介面 | `application/common/` 的 `ExportCaseQuery.java`、`ExportCaseRecord.java`、`RereviewerHistoryQuery.java` | 未立法（`application/common/` 是暫准） |
| 其他 | `reporting/package-info.java` | — |

不在本切片：`ReportController` 與 HTTP 的請求、回應格式（D、E 切片）；前端的日期換算（D-077，已完成）。

## 二、查到的事實

**本節是讀程式與既有測試確認的，沒有實測。** 報表的兩個入口目前沒辦法寫成規格範例來跑（事實 1），所以沒有像切片 C 那樣以暫時的範例實測。

**入口與範例執行器**

1. 範例執行器依名稱呼叫 `XUseCase.execute`，輸入依參數名稱一個一個對應。報表的兩個入口是 `DashboardQueryService.getSummary(actor, criteria)`、`ExportQueryService.export(actor, criteria)`：類別名稱、方法名稱、參數的形狀都對不上，**現在寫不出能執行的範例**。
2. 時間區間的檢查在 `ReportController`（`interfaces/`），不在問題側：
   - 儀表板：`from`、`to` 只給一個，或 `from` 不早於 `to`，回 400 `BAD_REQUEST`。
   - 匯出：`from`、`to` 缺任何一個，或 `from` 不早於 `to`，回 400 `BAD_REQUEST`。
   - 這些檢查在角色檢查**之前**：沒有權限的角色帶錯誤的時間，得到的是 400 不是 403。與 D-024 的順序（角色最先）不同。
3. 匯出的 `reportType` 不是 `REVIEW_COMPLETION` 時，`ExportQueryService` 丟 `interfaces.rest.BadRequestException`：問題側反過來依賴 HTTP 層，是 ARCH-01 基線裡的一行違規。

**儀表板摘要**

4. 角色：`HOSPITAL_MANAGER`、`AUDITOR`、`SYSTEM_ADMIN`；其他角色 `ROLE_DENIED`。沒有多角色的檢查。
5. 院區：不是 `SYSTEM_ADMIN` 只算本院，請求的 `hospitalId` 不採用；`SYSTEM_ADMIN` 算所有院區，可用 `hospitalId` 縮小（D-042、D-047）。
6. 篩選：`projectId`（不檢查專案是否存在，不存在就是 0）；`from`、`to` 兩個都有時只算 `from ≤ createdAt < to` 的案件。
7. 四個件數：
   - `totalCases`：範圍內所有案件，**包含 `CANCELLED`**。
   - `closedCases`：`status = CLOSED`。
   - `rereviewedCases`：**已指派複審醫師**（`rereviewerId` 不是 null），不代表複審已完成。
   - `overdueCases`：**初審期限**（`dueAt`）早於資料庫現在的時間，而且狀態不是 `CLOSED`、`CANCELLED`。
8. 逾期的三件事：
   - 只看初審期限，不看複審期限（`rereviewerDueAt`）。
   - 初審提交之後、結案之前，只要初審期限已過就一直算逾期，即使初審是準時交的。
   - 「現在」取資料庫的 `CURRENT_TIMESTAMP`，不是應用程式注入的時鐘；範例的 `clock`、`advance` 控制不到它。
9. 三個比率：件數 ÷ `totalCases`，`totalCases` 是 0 時是 0；是沒有四捨五入的浮點數（例如 1/3 是 `0.3333333333333333`）。`CANCELLED` 的案件算在分母。
10. UI-015 的頁面規格對四個件數與比率的說明與程式一致（「有複審＝已指派複審醫師」、「逾期＝查詢當下」）。

**匯出**

11. 角色、院區與儀表板相同。只支援 `reportType = REVIEW_COMPLETION`。沒有 `projectId` 的篩選。
12. 逐案資料列：`from ≤ createdAt < to`，依 `caseId` 遞增；`size` 預設 100、上限 1,000，`page` 負數當成 0；超出範圍的頁回空的清單（D-057）。
13. 每一列的欄位：`caseId`、`projectId`、`hospitalId`、`status`、`reviewerId`、`rereviewerId`、`initialScore`、`finalScore`、`createdAt`、`closedAt`、`maskedFields`。
14. 逐案分數的遮罩（D-055 第 6 點），任一成立就把 `initialScore`、`finalScore` 輸出成 null 並列進 `maskedFields`：
    - 匯出者是 `SYSTEM_ADMIN`。
    - 案件的狀態不是 `REREVIEW_IN_PROGRESS`、`CLOSED`、`CANCELLED`（之後還可能指派複審醫師）。
    - 匯出者是這個案件目前的複審醫師，或出現在它的複審指派歷史裡。
    - 這個案件的複審指派歷史不完整。
15. `summary`：同一個院區與時間區間的儀表板摘要，不受分頁影響。
16. 匯出不寫入讀取者紀錄、不遞增案件版本、**不發事件**（稽核查不到誰匯出）。
17. 一致讀取：儀表板的四個數字用一個查詢算出；匯出用 `SERIALIZABLE` 的唯讀交易。這是實作的作法，有整合測試。

**既有的測試**：`DashboardQueryServiceTest`、`DashboardSingleQueryTest`、`ExportQueryServiceTest`、`ExportScoreMaskingIntegrationTest`（遮罩的每一種情況）、`ExportHistoryQueryScopeTest`、`ExportSnapshotConsistencyTest`、`ReportHospitalScopeIntegrationTest`、`ReportApiTest`。

## 三、怎麼立法

### R-1：入口與範例

- **A（建議）：** 把兩個入口改成範例執行器叫得到的形狀，留在 `reporting/`：
  - `GetDashboardSummaryUseCase.execute(actor, hospitalId, projectId, from, to)`
  - `ExportReportUseCase.execute(actor, reportType, hospitalId, from, to, page, size)`
  - 規格寫成兩支查詢 use case，範例走真實的查詢。時間區間的檢查一併從 `ReportController` 移進來（R-2）。HTTP 的行為除了 R-2 的順序以外不變。
- **B：** 程式不動。規格只寫規則，範例全部列進 `not_covered`，由既有的單元與整合測試驗證。缺點：這個範圍的律法沒有任何一條是可執行的。
- **C：** 改範例執行器，讓它能呼叫任意名稱的入口。執行器是閘門，要另外修法，而且違反「程式使用規格的詞彙」的原則。

### R-2：時間區間的檢查與錯誤代碼

- **A（建議）：** 檢查移到問題側，順序改成 D-024：角色 → 輸入的檢查。錯誤代碼維持 `BAD_REQUEST`（400），前端與 `api-spec.md` 不用改。問題側改丟自己的例外，不再依賴 HTTP 層；ARCH-01 的基線縮短一行。行為變更只有一個：沒有權限的角色帶錯誤的時間，從 400 變成 403。
- **B：** A，但錯誤代碼改成 `VALIDATION_ERROR`，與其他 use case 一致。D-057 的條文與前端要跟著改。
- **C：** 檢查留在 `ReportController`，規格不寫（屬於 D、E 切片）。配合 R-1 選 A 時，範例執行器會繞過這些檢查。

### R-3：規格放在哪個目錄

報表不是 aggregate，它查的是審查案件。

- **A（建議）：** 寫進 `specs/review-case/`（規則前綴 `RC-DSH`、`RC-RPT`）。遮罩的規則可以直接引用已立法的複審指派歷史。
- **B：** 新增 `specs/report/`。規格格式要求每個目錄有 `model.yaml`（aggregate），報表沒有；要先確認檢查程式接不接受，可能要改格式。

## 四、儀表板摘要

### R-4：件數的定義

- **建議（照程式，事實 7、UI-015）：**
  - `totalCases`：範圍內所有案件，包含 `CANCELLED`。
  - `closedCases`：`CLOSED`。
  - `rereviewedCases`：已指派複審醫師。
- 請負責人確認 `CANCELLED` 算在總數與比率的分母裡是要的行為。算進去時，取消的案件會拉低完成率。
  - **另一個選項：** `totalCases` 不含 `CANCELLED`。這是行為變更，UI-015 要跟著改。

### R-5：逾期

逾期在題目清單裡是「未裁決事項」（`api-spec.md` §10.3：期限的檢核尚未定稿）。

- **A（建議）：** 本切片不立逾期的規則。`overdueCases`、`overdueRate` 保留在輸出，列進 `deferred_behaviors`，註明事實 8 的三件事，等逾期的切片一起裁決。程式不動。
- **B：** 照程式立法（初審期限、查詢當下、不是 `CLOSED`／`CANCELLED`）。「現在」要改用注入的時鐘，範例才控制得到。缺點：把事實 8 的前兩件事（不看複審期限、準時交也算逾期）變成律法。
- **C：** 現在就裁決逾期的完整規則。範圍會超出報表（案件列表、指派頁都有期限）。

### R-6：比率

- **A（建議）：** 照程式：件數 ÷ `totalCases`，`totalCases` 是 0 時是 0，不四捨五入。前端顯示時才取到小數一位（UI-015）。
- **B：** 後端四捨五入到四位小數。輸出比較穩定，但前端顯示的分子／分母不變，沒有實際的好處。

### R-7：篩選與院區

- **建議（照程式）：** 角色、院區（事實 4、5）；`projectId` 不檢查是否存在；`from`、`to` 兩個都給才依建立時間篩選（D-057 第 4 點）。別院的 `projectId` 得到的是 0，不是錯誤。

## 五、匯出

### R-8：匯出的規則

- **建議（照程式，D-055 第 6 點、D-056、D-057）：** 事實 11～15 全部立法：角色與院區、`reportType`、時間區間必填、依 `caseId` 遞增分頁、每一列的欄位、四種遮罩、`summary` 與 `totalElements` 是同一個母體。
- 一致讀取（事實 17）是實作的作法，不寫進規格，由既有的整合測試保護。

### R-9：匯出的稽核紀錄

與切片 A 的 S-14 同一題。負責人在 S-14 選了「照程式，不發事件」。

- **A（建議）：** 與 S-14 一致，照程式：不發事件。
- **B：** 匯出逐案的分數與醫師代號比抽選名單敏感，發一個稽核事件記錄誰匯出了哪個院區、哪個時間區間。這是新增的行為，事件的欄位要另外定。

## 六、登記

### R-10：登記範圍

- **建議：** `reporting/` 的檔案（R-1 選 A 時是改名後的檔案）與 `application/common/` 的 `ExportCaseQuery.java`、`ExportCaseRecord.java`、`RereviewerHistoryQuery.java` 登記為已立法。`reporting/` 的目錄列保留。`application/common/` 其餘的檔案留給 D、E 切片。

## 七、題目清單

本切片的修法 PR 一併：

- A（D-086）移到已完成；B 標為進行中。
- R-5 選 A 時，把事實 8 的三件事寫進「逾期」那一題。
