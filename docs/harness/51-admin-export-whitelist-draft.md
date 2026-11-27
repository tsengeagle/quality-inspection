# `SYSTEM_ADMIN` 的匯出照白名單收斂

狀態：**已裁決，條文是 `decision-log.md` 的 D-100，以那裡為準**（2026-11-27）。寫條文時有兩件事與本草稿不同，負責人另外裁決照建議，見 D-100 第 10 點。負責人裁決：X-1 先照建議（B：白名單現有的五欄加上 `createdAt`、`closedAt`），其餘各題照建議。依 X-8 與草稿 49 併成同一個裁決。下面是起草時的版本（main = `98d97ab`）。本文件不是律法；題號 X-1～X-8 只在本文件使用（草稿 49 第七節預告的是 X-1～X-6，起草時多了兩題）。

題目清單「`SYSTEM_ADMIN` 的匯出照白名單收斂」。負責人 2026-11-27 判斷它是缺陷（漏網紀錄的 E-11）。它與草稿 49（案件列表，E-10）是同一類。

## 一、問題

`POST /reports/export` 的每一列，對 `SYSTEM_ADMIN` 只把兩個分數設成 null，其他欄位與別的角色相同。其中 `projectId`、`createdAt`、`closedAt` 不在 metadata 的白名單裡（`permission-matrix.md` §5.1）。

## 二、查到的事實

**讀裁決、規格、契約、頁面規格與前端的程式，沒有實測。**

### 各個來源怎麼說

1. 匯出的每一列（`specs/review-case/http.yaml`）：`caseId`、`projectId`、`hospitalId`、`status`、`reviewerId`、`rereviewerId`、`initialScore`、`finalScore`、`createdAt`、`closedAt`、`maskedFields`。所有角色同一種形狀。
2. 白名單（D-036、權限矩陣 §5.1）：shared base 是 `caseId`、`caseStatus`、`projectName`、`hospitalId`、`reviewerId`、`rereviewerId`、`updatedAt`；頁面延伸只列了 UI-009（`batchId`、`dueAt`）與 UI-014。**沒有匯出頁（UI-017）的延伸。** 權限矩陣 §4 的頁面進入表也沒有列 UI-015、UI-017。
3. 對照白名單，匯出的每一列：

   | 欄位 | 在白名單裡 | 備註 |
   |---|:-:|---|
   | `caseId`、`hospitalId`、`reviewerId`、`rereviewerId` | ✓ | |
   | `status` | ✓ | 白名單叫 `caseStatus` |
   | `initialScore`、`finalScore` | — | 已經是 null（RC-RPT-07） |
   | `maskedFields` | — | 標記哪些欄位被遮罩，不是案件的資料 |
   | `projectId` | — | 白名單有的是 `projectName`，匯出沒有 |
   | `createdAt` | — | 匯出用它當篩選的時間（D-057） |
   | `closedAt` | — | |

4. 匯出相關的裁決都只談分數：
   - D-055 第 6 點：「`SYSTEM_ADMIN` 依既有的 metadata／aggregated-only 政策（D-033、api-spec 15.2），任何狀態都不取得逐案分數」。
   - D-057：依建立時間過濾、分頁；理由之一是「每次匯出都帶出全院歷年的每案醫師代號與分數，不符合資料最小化」。
   - D-087 第 8 點：照 D-055、D-056、D-057 立法，四種遮罩都是遮分數。

   沒有任何一個裁決談過 `SYSTEM_ADMIN` 的列該有哪些**非分數**的欄位。
5. api-spec §15.2 對 `SYSTEM_ADMIN` 的權限寫「metadata / aggregated only」。它可以讀成「只有白名單的 metadata 與彙總數字」，也可以讀成「不含分數的逐案資料加上彙總數字」。現在的程式是後一種讀法。
6. **匯出頁的規格（UI-017）明文把這些欄位給了 `SYSTEM_ADMIN`**：§3.4 的逐案表格列出十欄（含專案、建立時間、結案時間），§7 寫「`SYSTEM_ADMIN`：可選院區或全部院區；逐案分數一律被後端遮罩，畫面與 CSV 都顯示『已遮罩』」。這份規格的呈現方式是負責人 2026-11-17 裁決的。也就是程式符合頁面規格，頁面規格與白名單不一致。依權威順序，D-036 與權限矩陣高於頁面規格。

### 規格與範例

7. `ExportReport` 對 `SYSTEM_ADMIN` 的規則：RC-RPT-05（匯出所有院區，可用 `hospitalId` 縮小）、RC-RPT-07（分數是 null、`maskedFields` 列出兩個欄位）。有兩個以 `admin` 身分的範例，比對的是 `caseId`、`status`、`maskedFields` 與分數不存在；沒有用 `only` 封閉每一列的欄位。
8. 範例執行器把 null 與沒有這個欄位視為相同，所以 `absent: rows[*].initialScore` 現在是靠 null 通過的。

### 前端

9. 匯出頁的表格與 CSV 用同一組欄位（UI-017 §3.4、§5），不分角色。CSV 是前端逐頁取回之後自己組的。
10. 統計摘要（`summary`）只有件數與比率，與儀表板相同；`SYSTEM_ADMIN` 的儀表板是已裁決的（RC-DSH-04、UI-015）。

## 三、待裁決

### X-1　`SYSTEM_ADMIN` 匯出的每一列有哪些欄位

| 選項 | 每一列 | 白名單要不要改 |
|---|---|---|
| A | `caseId`、`status`、`hospitalId`、`reviewerId`、`rereviewerId`（白名單現有的） | 不用 |
| **B（建議）** | A 加上 `createdAt`、`closedAt` | 要：白名單加上 UI-017 的頁面延伸（`createdAt`、`closedAt`） |
| C | 沒有逐案的列，只有統計摘要（「aggregated only」的最嚴格讀法） | 不用 |
| D | `SYSTEM_ADMIN` 不能匯出（從角色拿掉，回 403） | 不用；UI-017、api-spec 要改 |

B 的理由：

- 這份報表是「某段時間內建立的案件，完成了沒有」。建立與結案的時間是這份報表的本體，拿掉之後每一列只剩 id 與狀態，`SYSTEM_ADMIN` 沒辦法用它追蹤平台的進度。
- 這兩個時間是流程的 metadata，不是審查的內容；案件詳情的白名單裡也有 `updatedAt`。用頁面延伸把它們明文加進白名單，比照 UI-009 的 `batchId`、`dueAt`。
- `projectId` 拿掉，與案件列表一致（草稿 49 的 W-2）。白名單有的是 `projectName`，匯出目前沒有，這次不補（只刪減、不新增）。

C、D 的考量：`SYSTEM_ADMIN` 已經有儀表板可以看彙總數字。若負責人認為平台管理者不需要逐案的資料，C 或 D 更乾淨，前端也最簡單。這一題是業務判斷，請負責人決定。

### X-2　分數與 `maskedFields`

收斂之後，`SYSTEM_ADMIN` 的列裡分數怎麼處理。

- **A（建議）**：`SYSTEM_ADMIN` 的列沒有 `initialScore`、`finalScore`、`maskedFields` 三個欄位。前端依形狀不顯示這兩欄（不是顯示「已遮罩」）。
- B：維持現況：兩個分數是 null，`maskedFields` 列出它們。

A 的理由：封閉的形狀能證明「沒有這個欄位」；null 只能證明這一次是 null。「已遮罩」是告訴使用者「這個值存在但你現在看不到」，對其他角色有意義（狀態變了之後可能看得到）；`SYSTEM_ADMIN` 永遠看不到，直接不顯示比較清楚。

A 要改 RC-RPT-07 的文字與 UI-017 §3.4、§7。

### X-3　統計摘要

**建議**：不變。`summary` 與儀表板相同，不在收斂的範圍；在裁決裡明講。

### X-4　契約怎麼寫

**建議**：與草稿 49 的 W-5 相同，`variants` 兩種形狀（`full`、`metadata`），差別在 `rows` 每一項的欄位；用 `transition` 過渡。

`variants` 是整個 `data` 的形狀，不是陣列項目的。兩種形狀的外層（`reportType`、`summary`、分頁欄位）相同，只有 `rows` 的項目不同，寫起來會重複外層；可以接受（D-096 第 7 點對 `Page` 的重複也是這樣處理）。

### X-5　規則與範例

**建議**：

- 新增規則：操作者是 `SYSTEM_ADMIN` 時，每一列只有 X-1 的欄位。來源 D-036、權限矩陣 §5.1 與本裁決。
- RC-RPT-07 改寫（X-2）。
- 既有的兩個 `admin` 範例加上 `only`，封閉每一列的欄位；補一個 `http` 步驟的範例觀察 `metadata` 形狀。

### X-6　前端

**建議**：匯出頁的表格與 CSV 依回應的形狀決定欄位：`metadata` 形狀時沒有「專案」「初審分數」「最終分數」三欄。分辨的方式與 #208 相同（看形狀，不看角色）。UI-017 §3.4、§5、§7 跟著改。

若 X-1 選 C：`SYSTEM_ADMIN` 的匯出頁只顯示統計摘要，沒有表格與下載。若選 D：`SYSTEM_ADMIN` 看不到匯出頁的入口。

### X-7　權限矩陣補上報表的兩頁

權限矩陣 §4 的頁面進入表沒有 UI-015（儀表板）、UI-017（匯出），§5.1 也沒有它們的延伸（事實 2）。這是 E-10、E-11 共同的成因：白名單是一張表，但哪些頁面要套用它沒有列完。

**建議**：這次一併補上：§4 加兩列（`SYSTEM_ADMIN` 是 `R (metadata)`），§5.1 的頁面延伸加上 UI-017（依 X-1），並註明 UI-015 只有彙總數字。

### X-8　與案件列表併成同一題

**建議**：併。一個裁決涵蓋 E-10 與 E-11，一組「修法 PR 一 → 建構 PR → 修法 PR 二」。理由：兩件是同一張白名單、同一種修法（`variants` 加 `transition`）、同一個新檢查的候選（草稿 49 第五節）；而且等簽章的修法 PR 已經排了四個，再拆成兩組就是六個。

併了之後，草稿 49 的 W-9 的清單加上：`ExportReport` 的規則與範例、`http.yaml` 的匯出、匯出用的新類別（登記進 `law/coverage.txt`）、UI-017、權限矩陣 §4 與 §5.1。

## 四、建議總表

| 題 | 建議 |
|---|---|
| X-1 | B：白名單現有的五欄加上 `createdAt`、`closedAt`；白名單加 UI-017 的延伸。C、D 是業務判斷，請負責人決定 |
| X-2 | A：`SYSTEM_ADMIN` 的列沒有分數與 `maskedFields` |
| X-3 | `summary` 不變 |
| X-4 | `variants` 兩種形狀，用 `transition` 過渡 |
| X-5 | 新規則、改寫 RC-RPT-07、範例用 `only` 封閉 |
| X-6 | 前端依形狀決定表格與 CSV 的欄位 |
| X-7 | 權限矩陣補上 UI-015、UI-017 |
| X-8 | 與案件列表併成同一個裁決、同一組 PR |

## 五、起草時沒有確認的

1. 沒有實測。
2. D-055、D-056、D-057、D-087 只讀了提到 `SYSTEM_ADMIN`、欄位、遮罩的段落，沒有通讀；UI-017 是 2026-11-17 裁決的，當時的討論紀錄（#68）沒有查。
3. `closedAt` 對已取消的案件是什麼值，沒有查。
4. 匯出回傳另一個類別時，`ExportResult`、`ExportRow` 與查詢埠要怎麼改，沒有試寫。`reporting` 受 ARCH-05 限制（只能依賴 `application.common`）。
5. 前端的 CSV 組法（`report-export.service.ts`）在兩種形狀下怎麼改，沒有試寫。
