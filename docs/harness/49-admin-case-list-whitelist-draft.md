# `SYSTEM_ADMIN` 的案件列表照白名單收斂

狀態：**負責人已口頭裁決，條文尚未寫入**（2026-11-27）。負責人裁決：W-1～W-6、W-9 照建議；W-8 匯出也是缺陷（記成漏網紀錄的 E-11，後續見第七節）；W-7 照**修正後**的建議（第七節：API 維持 `projectId` 可以篩選、`medicalRecordType` 不採用；畫面的「專案的選擇器」另立一題）。條文要由修法 PR 寫進 `decision-log.md` 才生效。下面第一到第六節是起草時的版本（main = `98d97ab`）。本文件不是律法；題號 W-1～W-9 只在本文件使用。

題目清單「`SYSTEM_ADMIN` 的案件列表照白名單收斂」。負責人 2026-11-27 判斷它是缺陷（漏網紀錄的 E-10）。發現的經過在 `docs/review/f2-system-admin-metadata-reconciliation-2026-11-27.md` 的 S-1。

## 一、問題

`SYSTEM_ADMIN` 只能看 metadata，可以看的欄位有白名單（D-036）。案件**詳情**照白名單收斂成七欄了（D-084、D-094）；案件**列表**沒有：程式只在每一筆加上 `projectionType: METADATA_ONLY`，其他欄位與別的角色完全相同。

D-088 把 HTTP 契約「照程式」立法時，這個現況被寫成了律法：列表只有一種形狀，所有角色共用。現在閘門保護的是有缺陷的形狀。

## 二、查到的事實

**讀程式、規格、契約與文件，沒有實測。**

### 三個來源對 `SYSTEM_ADMIN` 的列表各說各的

1. 每一筆有哪些欄位：

   | 欄位 | 白名單（`permission-matrix.md` §5.1、api-spec §3.6） | UI-009 §11.2 的範例 | 程式與契約 |
   |---|:-:|:-:|:-:|
   | `caseId` | ✓ | ✓ | ✓ |
   | 狀態 | ✓ `caseStatus` | ✓ `caseStatus` | ✓ `status` |
   | `projectName` | ✓ | — | — |
   | `hospitalId` | ✓ | — | ✓ |
   | `reviewerId` | ✓ | ✓ | ✓ |
   | `rereviewerId` | ✓ | ✓ | ✓ |
   | `updatedAt` | ✓ | ✓ | — |
   | `batchId` | ✓（UI-009 的延伸） | ✓ | ✓ |
   | `dueAt` | ✓（UI-009 的延伸） | ✓ | ✓ |
   | `projectId` | — | ✓ | ✓ |
   | `medicalRecordType` | — | — | ✓ |
   | `sourceCaseKey` | — | — | ✓ |
   | `createdAt` | — | — | ✓ |
   | `assignedAt` | — | — | ✓ |
   | 視角的標記 | — | ✓ `viewMode` | ✓ `projectionType` |

   白名單是 shared base 七欄加上 UI-009 的延伸兩欄，共九欄。權限矩陣的文字是「`SYSTEM_ADMIN` 僅可見上述白名單集合」。

2. 依權威順序：D-036（裁決）與權限矩陣（`docs/02-ddd/`）高於 api-spec、頁面規格與程式。api-spec §3.6 與權限矩陣一致。所以白名單以外、程式卻給了的是 `projectId`、`medicalRecordType`、`sourceCaseKey`、`createdAt`、`assignedAt` 五欄。

### 程式

3. `ListReviewCasesUseCase`（第 44 行）：`SYSTEM_ADMIN` 只把 `projectionType` 設成 `METADATA_ONLY`。資料來自 `ReviewCaseQueryAdapter.toSummary`，每個角色都是同一組 13 個欄位。
4. 白名單有、列表目前沒有的兩欄：
   - `projectName`：案件的資料表只有 `projectId`。案件詳情的 metadata 是另外查專案取得名稱（`CaseDetailMetadataMapper`，每個案件查一次）。列表每頁最多 100 筆，照同樣的作法要查很多次，或改成一次查一批。
   - `updatedAt`：案件的資料表有這個欄位（`ReviewCaseEntity.updatedAt`），列表沒有帶出來。
5. `SYSTEM_ADMIN` 的列表不受院區限制，可以用 `hospitalId` 縮小範圍（`actor.readScopeHospitalId`，D-047）。

### 規格、契約與範例

6. `specs/review-case/usecases.yaml` 的 `ListReviewCases`：
   - 規則只有 RC-LST-01（不是 `SYSTEM_ADMIN` 時只列出本院的案件）與 RC-LST-02（`status` 的值不對）。**沒有任何一條規則說 `SYSTEM_ADMIN` 的列表是什麼樣子。**
   - `output` 的來源寫 api-spec，欄位是 `content: list<CaseSummary>`。
   - `input` 只列了 `batchId`、`status`；程式另外收 `hospitalId`、`projectId`、`reviewerId`、`rereviewerId`、`medicalRecordType`、`page`、`size`。
7. `specs/review-case/http.yaml` 的 `ListReviewCases`：一種形狀，`medicalRecordType`、`sourceCaseKey` 是必有的 `text`。
8. `specs/review-case/examples.yaml`：以 `ListReviewCases` 當 `when` 的範例只用了 `manager_h1`、`manager_h2` 兩種身分。**沒有任何範例以 `SYSTEM_ADMIN` 的身分列出案件**，所以「跨院區列出」與「欄位收斂」都沒有被執行過。
9. 對照：案件詳情有 RC-DTV-04（只有白名單的七欄）、修正歷程有 RC-VIS-05、提交明細有 RC-SBV-02，都有以 `admin` 身分、用 `only` 封閉的範例。列表是 `SYSTEM_ADMIN` 讀得到的案件端點裡唯一沒有的。

### 前端

10. 案件列表頁（UI-009）：`projectionType` 是 `METADATA_ONLY` 時切到 metadata 模式（不顯示業務命令）。表格的欄位不分模式，都是案件編號、**來源案件**（`sourceCaseKey`）、狀態、**病歷類型**（`medicalRecordType`）、截止日、操作。所以 `SYSTEM_ADMIN` 在畫面上就看得到白名單以外的兩欄。
11. UI-009 的頁面規格：§3.5、§7.1 寫「只看 metadata、不顯示完整內容導向欄位」；第 277 行「system admin 不應顯示內容導向篩選」；§11.2 的範例如事實 1。

### 其他 `SYSTEM_ADMIN` 讀得到的端點

12. 依規格的 `actor.roles`，`SYSTEM_ADMIN` 讀得到的 use case 與它們的現況：

    | use case | 對 `SYSTEM_ADMIN` 的規則 | 現況 |
    |---|---|---|
    | `GetReviewCaseDetail` | RC-DTV-04：七欄 | 已收斂，有範例 |
    | `GetRevisionHistory` | RC-VIS-03、RC-VIS-05 | 已收斂，有範例 |
    | `GetSubmissionDetail` | RC-SBV-02：白名單 | 已收斂，有範例 |
    | `GetCaseLifecycle`、`ListAuditTrails` | RC-LCY-01、RC-AUD-01：沒有 `detail` | 已裁決（D-071），有範例 |
    | `GetDashboardSummary` | RC-DSH-04：統計所有院區 | 只有統計數字 |
    | `ListSamplingBatches`、`GetSamplingBatchDetail` | SB-LST、SB-DET-02：沒有抽中的病歷 | 已裁決（D-086 第 12 點） |
    | **`ListReviewCases`** | **沒有** | **本題** |
    | `ExportReport` | RC-RPT-07：分數是 null | 見 W-8 |

13. 匯出（`POST /reports/export`）的每一列有 `caseId`、`projectId`、`hospitalId`、`status`、`reviewerId`、`rereviewerId`、`initialScore`、`finalScore`、`createdAt`、`closedAt`。對 `SYSTEM_ADMIN` 只把兩個分數設成 null（RC-RPT-07，D-055）。`projectId`、`createdAt`、`closedAt` 不在 §5.1 的白名單裡；白名單的頁面延伸只列了 UI-009、UI-014，沒有匯出頁（UI-017）。**這算不算同一種缺陷，本文件沒有判斷**，見 W-8。

## 三、待裁決

### W-1　列表的白名單以誰為準

- **A（建議）**：權限矩陣 §5.1 的九欄。這是 D-036 的裁決，權威最高。UI-009 §11.2 的範例跟著改。
- B：UI-009 §11.2（多一個 `projectId`，少 `projectName`、`hospitalId`）。要先修訂白名單。

### W-2　白名單有、列表目前沒有的兩欄要不要補

白名單的文字是「僅可見」，是上限，不要求每一欄都給。

| 選項 | `SYSTEM_ADMIN` 的列表每一筆 |
|---|---|
| **A（建議）** | 七欄：`caseId`、狀態、`hospitalId`、`reviewerId`、`rereviewerId`、`batchId`、`dueAt`。只刪減，不新增 |
| B | A 加上 `updatedAt`（資料表有，成本低） |
| C | 九欄全給（另加 `projectName`，要多查專案） |

A 的理由：這一題是修缺陷（多給了），不是加功能。`projectName`、`updatedAt` 要不要出現在列表是頁面的需求，目前前端的表格兩欄都沒有用；需要時另外裁決。

`SYSTEM_ADMIN` 跨院區看列表，`hospitalId` 是必要的，白名單也有，保留。

### W-3　狀態的欄位名稱

白名單寫 `caseStatus`，列表現在叫 `status`（完整形狀也是 `status`）。

- **A（建議）**：列表維持 `status`，兩種形狀用同一個名稱。在規則裡註明它就是白名單的 `caseStatus`。
- B：`SYSTEM_ADMIN` 的列表改叫 `caseStatus`，與案件詳情的 metadata 形狀一致。

A 的理由：同一個端點的兩種形狀用不同的名稱，前端又要多一層分辨（F-2 就是這樣來的）。「同一個狀態有兩個名稱」是既有的問題（核對報告的 S-2），不在這一題擴大。

### W-4　視角的標記要不要留

白名單沒有 `projectionType`；案件詳情的 metadata 形狀也沒有。但前端的列表頁靠它切換模式。

- **A（建議）**：列表的兩種形狀都保留 `projectionType`（`FULL`、`METADATA_ONLY`）。它是視角的標記，不是案件的資料，在規則裡明講它不屬於白名單管的範圍。
- B：拿掉，前端改成看形狀分辨（同 F-2 的作法）。

A 的理由：列表是分頁的，一頁沒有資料時沒有形狀可以看；`projectionType` 現在放在每一筆裡，空頁時前端同樣拿不到，這是既有的限制，但 B 不會讓它變好。若要徹底解決，應該把標記放在分頁的外殼，那會動到 D-096 的列表形狀，另外裁決。

### W-5　契約怎麼寫

| 選項 | 作法 |
|---|---|
| **A（建議）** | `variants` 兩種形狀：`full`（現在的 13 欄）、`metadata`（W-2 的欄位加 `projectionType`）。與案件詳情的寫法相同 |
| B | 維持一種形狀，把白名單以外的欄位改成可為 null，`SYSTEM_ADMIN` 時輸出 null |

A 的理由：封閉的形狀能證明「沒有這個欄位」；B 只能證明「值是 null」，之後有人把值填回去，契約不會擋（null 與非 null 都是合法的）。

A 要增刪形狀，依 D-094 第 10 點用 `transition` 過渡：修法 PR 加上新的形狀、新舊都標 `transition`；建構 PR 改程式；之後的修法 PR 拿掉 `transition`。

A 的程式面：`SYSTEM_ADMIN` 的列表要回傳另一個類別（只有 metadata 的欄位）。不能用 Jackson 的標註在原本的類別上隱藏欄位，D-096 的 ARCH-06 禁止 `application` 新增對 Jackson 的依賴（#194，還沒有合併）。新類別放在 `application/reviewcase/query/`，要先登記進 `law/coverage.txt`。

### W-6　規則與範例

- **A（建議）**：
  - 新增規則（編號待定，暫稱 RC-LST-03）：操作者是 `SYSTEM_ADMIN` 時，每一筆只有 W-2 的欄位與 `projectionType`。來源 D-036、權限矩陣 §5.1。
  - 新增規則（暫稱 RC-LST-04）：操作者是 `SYSTEM_ADMIN` 時列出所有院區的案件；輸入有 `hospitalId` 時只列出那個院區的（D-047）。這是 RC-LST-01 的另一半，現在沒有規則也沒有範例（事實 8）。
  - 範例：以 `admin` 身分列出案件，用 `only` 封閉每一筆的欄位；跨院區與指定院區各一個；一個 `http` 步驟的範例觀察 `metadata` 形狀。
- B：只加欄位的規則，不補跨院區的。

A 多補的那一條不是這次的缺陷，但它是同一個 use case 對同一個角色缺的另一半，一起補的成本很低。

### W-7　`SYSTEM_ADMIN` 能不能用看不到的欄位篩選

程式的列表接受 `medicalRecordType` 當篩選條件（事實 6）。`SYSTEM_ADMIN` 看不到這個欄位，但若可以用它篩選，篩選的結果就透露了它的值。UI-009 第 277 行也說 system admin 不應顯示內容導向的篩選。

| 選項 | 作法 |
|---|---|
| **A（建議）** | `SYSTEM_ADMIN` 帶 `medicalRecordType` 時不採用（當成沒有給），與 RC-LST-01「輸入的院區不採用」的作法一致。新增一條規則與範例 |
| B | 回 `BAD_REQUEST` |
| C | 不處理 |

`projectId` 也不在白名單裡，但它是 `SYSTEM_ADMIN` 追蹤平台狀況的主要入口（儀表板、匯出都用 `projectId` 當條件，RC-DSH-04），建議維持可以篩選。這一點請負責人確認。

另外：規格的 `input` 只列了 `batchId`、`status`，其餘七個輸入沒有寫進規格（事實 6）。**建議這次一併補上**，否則 W-7 的規則寫不出來。

### W-8　要不要一併處理匯出

事實 13：匯出的每一列對 `SYSTEM_ADMIN` 也有白名單以外的 `projectId`、`createdAt`、`closedAt`。

- **A（建議）**：不在這一題處理。匯出的欄位與遮罩是 D-055、D-056、D-057、D-087 逐項裁決過的，當時明確決定只遮分數；白名單的頁面延伸也沒有列匯出頁。請負責人另外判斷它是「白名單漏列了匯出頁的延伸」還是缺陷。
- B：一併收斂。

本文件**沒有把匯出當成缺陷**。若負責人判斷是，另記一筆漏網。

### W-9　分批

全部照建議時：

1. **修法 PR 一**（要簽章）：
   - 本裁決（`decision-log.md`）。
   - `specs/review-case/usecases.yaml`：`ListReviewCases` 補上輸入、W-6 與 W-7 的規則。
   - `specs/review-case/examples.yaml`：新的範例。現在的程式會讓它們失敗，凍結在 `known-test-failures.txt`。
   - `specs/review-case/http.yaml`：`variants`，新舊都標 `transition`。
   - `law/coverage.txt`：登記新的類別。
   - `permission-matrix.md`、api-spec §10.1、UI-009 §11.2：照裁決修正。
2. **建構 PR**：後端的列表（新類別、`SYSTEM_ADMIN` 的投影、篩選不採用）；解凍範例。前端：列表頁在 metadata 模式不顯示來源案件與病歷類型兩欄，型別與測試的假回應照新的形狀。
3. **修法 PR 二**（要簽章）：`http.yaml` 拿掉 `transition`。

修法 PR 一會是第四個排隊等簽章的修法 PR，裁決同樣加在 `decision-log.md` 的檔尾，要疊在 #201 之上。

前端可以先做的部分（不等修法）：列表頁在 metadata 模式隱藏那兩欄。後端還在送，但畫面上先不顯示。這只是遮住畫面，資料仍然在回應裡，不能當成修正。

## 四、建議總表

| 題 | 建議 |
|---|---|
| W-1 | A：以權限矩陣 §5.1 的九欄為上限 |
| W-2 | A：七欄，只刪減不新增 |
| W-3 | A：列表維持叫 `status` |
| W-4 | A：兩種形狀都保留 `projectionType` |
| W-5 | A：`variants` 兩種形狀，用 `transition` 過渡 |
| W-6 | A：欄位的規則與跨院區的規則都補，範例用 `only` 封閉 |
| W-7 | A：`SYSTEM_ADMIN` 的 `medicalRecordType` 篩選不採用；`projectId` 維持可以篩選；規格的輸入補齊 |
| W-8 | A：匯出不在這一題，另外判斷 |
| W-9 | 修法 PR 一 → 建構 PR → 修法 PR 二 |

## 五、對漏網紀錄的回應

E-10 的兩個候選回應（`12-escaped-defects.md`），本草稿的看法：

1. **回頭核對 D-088 照程式立法的 36 個端點**：事實 12 已經把 `SYSTEM_ADMIN` 讀得到的 use case 掃過一次，只有列表沒有規則，匯出待判斷。其他角色的可見性（例如複審醫師的盲審）沒有掃，D-033 的部分已經有很多範例。建議不另外做全面的核對。
2. **白名單寫成可執行的規則**：W-6 做完之後，`SYSTEM_ADMIN` 讀得到的每一個案件端點都有用 `only` 封閉的範例。可以再加一條檢查：規格裡 `actor.roles` 有 `SYSTEM_ADMIN` 的查詢，必須至少有一個以 `SYSTEM_ADMIN` 身分執行的範例。這條放在 `spec_lint.py`，要修法，建議列為 W-9 之後的一題。

## 六、起草時沒有確認的

1. 沒有實測。「`SYSTEM_ADMIN` 的列表與其他角色的欄位相同」是讀程式與契約得到的。
2. `sourceCaseKey` 的內容是什麼、能不能對回病人，沒有查。它影響這個缺陷的嚴重程度，不影響要不要修。
3. `SYSTEM_ADMIN` 實際上會不會用 `medicalRecordType` 篩選（前端的篩選列對 metadata 模式顯示哪些條件），沒有查。
4. 列表的 `SYSTEM_ADMIN` 回傳另一個類別時，`Page<T>` 的型別、查詢埠的介面要怎麼改，沒有試寫。
5. 事實 12 的表是依規格的 `actor.roles` 列的，沒有對照 controller 確認沒有漏掉的端點。
6. 匯出的事實（事實 13）是讀契約得到的，沒有回頭讀 D-055～D-057 的全文確認當時有沒有討論過 `projectId`、`createdAt`、`closedAt`。

## 七、裁決之後的補充（2026-11-27）

### W-7 的補查：`projectId` 的篩選有沒有意義

負責人的問題：使用者很難記住 `projectId`，用它篩選有意義嗎？

補查前端實際怎麼讓使用者指定專案（讀程式，沒有執行）：

| 頁面 | 現況 |
|---|---|
| 案件列表頁（UI-009） | **沒有專案的篩選**。篩選列只有四個狀態的按鈕；頁面送給後端的條件只有 `status`、`page`、`size`。`cases.service.ts` 的 `getCases` 支援 `projectId`、`batchId`、`hospitalId`，但列表頁沒有傳 |
| 儀表板（UI-015） | 一個**文字框**，標籤是「專案（留空為全部專案）」，要使用者自己輸入 `projectId` |
| 匯出頁（UI-017） | 起草時沒有看到專案的輸入欄位，沒有細查 |
| 從抽選的頁面導覽到案件列表 | 網址帶 `batchId`（見下方「另外看到的」） |

所以負責人的直覺是對的：**以「使用者輸入」來說，`projectId` 的篩選沒有意義**，儀表板現在就是這個狀況。人認得的是專案的名稱。

但 `projectId` 當篩選條件仍然有用，前提是它由畫面帶入，不是由人輸入：

- 從專案的清單點進「這個專案的案件」，網址帶 `projectId`。
- 下拉選單顯示名稱、送出 `projectId`。`GET /review-projects` 每一筆有 id 與名稱，`SYSTEM_ADMIN` 可以讀（D-062）。

修正後的建議，分成兩件：

| | 建議 |
|---|---|
| **API（本題）** | `SYSTEM_ADMIN` 的 `projectId` 篩選維持可用；`medicalRecordType` 的篩選不採用（原本的 W-7 A）。理由：`projectId` 是參照，不是案件的內容；用它篩選透露的是「這個案件屬於哪個專案」，而 `projectName` 本來就在白名單裡 |
| **畫面（另立一題）** | 「專案的選擇器」：儀表板、匯出頁、案件列表要指定專案時，用下拉選單顯示名稱，不讓使用者輸入 id。這是所有角色的問題，不只 `SYSTEM_ADMIN`。目前專案的管理頁（UI-001～005）還沒有做，沒有任何畫面能讓使用者查到 `projectId` |

這也讓 W-2 值得再看一次：白名單有 `projectName`，而人靠名稱認專案。`SYSTEM_ADMIN` 跨院區看列表時，沒有專案名稱的列表不容易用。W-2 已裁決照建議（只刪減、不補 `projectName`），若負責人想改成補上，要在修法之前說。

負責人 2026-11-27 確認：照上表。

### W-8：匯出也是缺陷（E-11）

負責人 2026-11-27 判斷：匯出的每一列對 `SYSTEM_ADMIN` 有白名單以外的 `projectId`、`createdAt`、`closedAt`，是缺陷。已記成 `12-escaped-defects.md` 的 E-11，題目清單另列一題。

修正之前要裁決的（已起草成 `51-admin-export-whitelist-draft.md`，題號在那裡重新編成 X-1～X-8；下表是起草前的預告）：

| 題 | 內容 |
|---|---|
| X-1 | `SYSTEM_ADMIN` 匯出的每一列有哪些欄位。白名單的 shared base 裡，匯出現在有 `caseId`、狀態、`hospitalId`、`reviewerId`、`rereviewerId`，沒有 `projectName`、`updatedAt`。只刪減的話是這五欄加上 `maskedFields` |
| X-2 | 兩個分數現在是 null 加上 `maskedFields`（RC-RPT-07，D-055）。收斂之後是維持 null，還是連欄位都不輸出 |
| X-3 | 匯出的 `summary`（統計數字）對 `SYSTEM_ADMIN` 照給（與儀表板相同），確認不在收斂的範圍 |
| X-4 | 白名單的頁面延伸要不要加上匯出頁（UI-017），讓它成為明文 |
| X-5 | 匯出下載成 CSV 的欄位由前端組，前端的欄位要跟著改 |
| X-6 | 與列表併成同一個修法 PR，還是分開 |

建議 X-6 併在一起：兩件是同一類缺陷、同一張白名單，而且修法 PR 已經排了四個。

### 起草時另外看到的（前端，都還沒有處理）

補查 W-7 時讀案件列表頁的程式看到的。**讀程式得到的，沒有執行。**

| # | 事情 | 依據 |
|---|---|---|
| N-1 | 篩選列的「待處理」「進行中」送出的 `status` 是 `PENDING`、`IN_PROGRESS`，不是後端的狀態值（後端是 `PENDING_INITIAL_REVIEW` 等九個）。依 RC-LST-02，後端會回 400 `BAD_REQUEST`。「已結案」「已取消」送的是 `CLOSED`、`CANCELLED`，是合法的值。頁面預設的狀態（依頁籤）是合法的值，所以進頁面不受影響，按這兩個按鈕才會 | `case-work-queue-filter-bar.component.ts`、`ListReviewCasesUseCase.parseStatus` |
| N-2 | 從抽選執行頁、批次清單導覽到案件列表時，網址帶 `batchId`，但列表頁組查詢條件時沒有讀它，列出來的不是那個批次的案件 | `sampling-execute-page.component.ts`、`sampling-batch-list-page.component.ts`、`case-list-page.component.ts` 的 `_loadCases` |

這兩件與 E-04、E-05 是同一類：前端的測試用自己的假回應，全綠；對真實的後端行為不對。

