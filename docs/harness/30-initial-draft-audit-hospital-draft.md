# 初審草稿的稽核紀錄沒有院區

狀態：**已裁決**（2026-11-24）。負責人表示補院區、已寫入的紀錄不回填，並指示範圍不大就直接開修法 PR；W-1～W-3 照建議（W-3 選 A）。條文是 `decision-log.md` 的 D-079，以那裡為準。下面保留起草時的版本（main = `2eb643d`）。本文件不是律法；題號 W-1～W-3 只在本文件使用。

## 一、背景

D-078 建構時發現（草稿 29 的更正、#148）：初審保存草稿寫入稽核紀錄時，院區是空字串。院區人員以 `ListAuditTrails` 查不到初審的 `DRAFT_SAVED`。#148 因此把兩個範例改成只檢查 `SUBMITTED`。

## 二、查到的事實

- **只有一處**：`SaveInitialReviewDraftUseCase.java:111`（沒有作答）與 `:145`（有作答）的 `ReviewDraftSavedEvent` 第 4 個參數是 `""`。
- **掃描**：`src/main/java` 共 38 個 `publish(new …)`，其他都傳了院區。限制：只掃了 `publish(new …)` 的呼叫，不在這個形式的事件沒有涵蓋。
- **由來**：2026-04-09 建立這個 use case 時（`d887b69`）就是 `""`，之後沒有改過。沒有找到刻意這樣做的紀錄。
- **同一方法已有院區**：第 66 行載入 `reviewCase`，第 69 行以 `reviewCase.getHospitalId()` 檢查權限。複審草稿就是傳 `reviewCase.getHospitalId()`（`SaveRereviewDraftUseCase.java:104`）。
- **影響**：
  - `HOSPITAL_MANAGER`、`AUDITOR`：`ListAuditTrails` 以院區過濾，查不到。
  - `SYSTEM_ADMIN`：指定院區時查不到，不指定時查得到。
  - `GetCaseLifecycle` 只查 `ReviewCase` 的紀錄，不受影響。
- **分數**：補上院區後紀錄會被讀到，`detail` 的 `totalScore` 已由 D-078 遮罩。
- **文件**：`domain-events.md` 7.1 `ReviewDraftSaved` 的最低必要欄位沒有 `hospitalId`；其他事件的範例有帶（例如 4.1）。
- **規格與律法**：`SaveInitialReviewDraftUseCase` 已立法（`specs/review-case/`）；規格沒有寫稽核紀錄要帶院區。
- **已寫入的紀錄**：`audit_trail.hospital_id` 可以是 null；正式資料庫沒有資料（D-045，負責人確認）。

> 這次的事實是讀程式確認的。「院區人員查不到」已由 #148 之前的範例實際走寫入與查詢路徑證實（`absent … 選不到任何一筆`）。

## 三、W-1：補院區

### 建議

- 初審保存草稿的事件帶所屬案件的院區（`reviewCase.getHospitalId()`），與複審草稿相同。
- 規格：D-078 的兩個範例恢復檢查 `DRAFT_SAVED`；這兩個範例一開始是紅的，加進 `known-test-failures.txt`（D-066 第 5 點），由建構 PR 移除。
- `domain-events.md` 7.1 的最低必要欄位補上 `hospitalId`。

### 待負責人決定

同意嗎？

## 四、W-2：已寫入的紀錄

### 建議

不回填（負責人已表示）。開發與測試資料庫的舊紀錄維持空字串，不另外處理。

### 待負責人決定

同意嗎？

## 五、W-3：要不要立一條一般規則

### 選項

- **A（建議）：寫進裁決。** 稽核紀錄的院區一律是所屬案件（或專案）的院區，不得是空字串或 null；之後新增事件也適用。本題只修初審草稿，其他事件已經符合。
- **B：只修這一處，不立一般規則。**

A 的理由：這次的缺口是「沒有規則說要帶」才沒被發現；有規則之後，新事件漏帶會被審查擋下。規則本身不新增測試，只寫進裁決。

### 待負責人決定

A／B？

## 六、裁決後的順序

1. **修法 PR**：`decision-log.md` 新增 D-079；`specs/review-case/examples.yaml` 的兩個範例恢復檢查 `DRAFT_SAVED`，加進 `known-test-failures.txt`；`domain-events.md` 7.1；本草稿標為已裁決；題目清單、README 索引。
2. **建構 PR**：`SaveInitialReviewDraftUseCase` 兩處改傳 `reviewCase.getHospitalId()`，`SaveInitialReviewDraftUseCaseTest` 補上院區的斷言；移除 `known-test-failures.txt` 的項目。
