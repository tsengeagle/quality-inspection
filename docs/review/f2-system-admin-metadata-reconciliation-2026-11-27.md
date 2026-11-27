# `SYSTEM_ADMIN` 的 metadata 視角：頁面規格、後端、前端的核對（2026-11-27）

`docs/review/frontend-http-contract-reconciliation-2026-11-27.md` 的 F-2（`12-escaped-defects.md` 的 E-05）動手修之前的核對。

- 核對時間點：`main` 的 `98d97ab`。
- 範圍：`SYSTEM_ADMIN` 進案件詳情頁（UI-010）與 revision 歷程頁（UI-014）時，四個來源各自怎麼說。
- 本文件只記錄差異與選項，**不改頁面規格、不改前端、不改後端**。
- **負責人 2026-11-27 裁決**：M-1～M-6 全部照建議；S-1 算缺陷（記成 `docs/harness/12-escaped-defects.md` 的 E-10，修正另立題目）。頁面規格與前端的修改在另一個 PR。
- 都是讀文件與程式得到的，沒有執行前端，也沒有對真實的後端操作。

## 一、四個來源

依權威順序（`AGENTS.md`）：

| # | 來源 | 位置 |
|---|---|---|
| 1 | 裁決 | D-036（metadata 的白名單）、D-094 第 9 點（七欄）、D-067（歷程的欄位）、D-084 |
| 2 | 權限矩陣 | `docs/02-ddd/permission-matrix.md` §5.1（白名單）、第 195～199 行 |
| 3 | 規格與契約（律法） | `specs/review-case/usecases.yaml` 的 RC-VIS-03、RC-VIS-05；`specs/review-case/http.yaml` 的 `GetReviewCaseDetail`（`metadata` 形狀）、`GetRevisionHistory`、`ListReviewCases` |
| 4 | api-spec | §3.6、§10.2 |
| 5 | 頁面規格 | `docs/05-ui/ui-page-spec-ui010案件詳情頁.md`、`ui-page-spec-ui014revision歷程頁.md` |
| — | 前端的程式 | `features/cases/case-detail/`、`features/cases/revision-history/`、`features/cases/services/cases.service.ts` |

## 二、後端實際給 `SYSTEM_ADMIN` 什麼

以契約為準（契約是封閉的形狀，由比對閘門對照後端實際的回應）。

| 端點 | `SYSTEM_ADMIN` 拿到的 | 依據 |
|---|---|---|
| `GET /review-cases/{caseId}` | 七個欄位：`caseId`、`caseStatus`、`projectName`、`hospitalId`、`reviewerId`、`rereviewerId`、`updatedAt` | `http.yaml` 的 `metadata` 形狀；D-036、D-094 第 9 點 |
| `GET /review-cases/{caseId}/revision-history` | `caseId` 與每一筆的 `submissionId`、`stage`、`reviewerId`、`status`、`revisionNo`、`submittedAt`、`supersededBySubmissionId`；`totalScore`、`comment` 是 null | RC-VIS-03、RC-VIS-05（D-067） |
| `GET /review-cases`（列表） | 與其他角色**相同的欄位**，只是 `projectionType` 是 `METADATA_ONLY` | 見第六節 S-1 |

案件詳情沒有 `status`（叫 `caseStatus`）、沒有 `projectionType`、沒有 `actorRole`、沒有 `submissions`、沒有任何 id 參照（`projectId`、`batchId`）、沒有 `updatedAt` 以外的時間。

## 三、UI-010 案件詳情頁

### 3.1 頁面規格內部就不一致

| 位置 | 內容 | 與七欄相比 |
|---|---|---|
| §12.2 的範例、檔尾的「D-036」一節 | 七欄（D-094 第 9 點更正之後） | 一致 |
| §5.5「SYSTEM_ADMIN 視角：可看」 | metadata、status、actor ids、timestamps、batch／project／version 參照、lifecycle metadata | **比七欄多**：七欄裡的時間只有 `updatedAt`；沒有 batch、project、version 的 id（只有 `projectName`） |
| §9.4「Metadata-only View」 | 仍可顯示 status／ids／timestamps／actor ids | 同上 |
| §4.3「來源與追溯區」 | 必須顯示 `projectId`、`projectVersionId`、`batchId`、`scoreSheetVersionId` 等；「system admin 在這區仍可看 metadata」 | 七欄裡這一區只有 `projectName`、`hospitalId` |
| §4.4、§4.5 摘要區 | 「system admin：metadata only，不顯示完整答案內容」；建議顯示 latest submissionId、submission status、submittedAt、reviewer、revision count | 七欄裡沒有任何提交的資訊 |
| §4.2 的 action | system admin：查看 metadata、查看 lifecycle metadata | 與資料無關 |

也就是：D-036 把白名單收斂成固定的欄位之後，頁面規格只改了範例與檔尾，正文各區塊對 system admin 的描述還停在「看得到各區的 metadata」。D-036 寫明「不得加入 FULL-only、敏感巢狀資訊或因新增 DTO getter 自動擴大白名單」，裁決的權威高於頁面規格，所以要改的是頁面規格的正文。

### 3.2 前端的現況

`METADATA_CONTROL_CENTER` 模式下，頁面實際顯示的東西很少：

| 區塊 | 顯示什麼 | 用到的資料 | 對七欄 |
|---|---|---|---|
| C01 頁首 | `caseId`（來自路由）、`status` | `status` | **拿不到**：後端叫 `caseStatus` |
| C04 投影提示 | 「metadata only」的提示 | 模式 | 模式判斷不出來（見下） |
| C03 模式列 | 「控制中心（Metadata）」 | 模式 | 同上 |
| C08 導覽 | 只有「Revision 歷程」一個按鈕（初審、複審、指派都不顯示） | 模式 | 同上。內容符合規格「僅 metadata 層追蹤與導航」 |
| C09 初審摘要 | 不顯示 | — | 一致 |
| C10 複審摘要 | **顯示**，內容是「第 N 版 / 狀態」，從 `submissions` 算出來 | `submissions`、`latestRereviewSubmissionId`、`finalScore` | **拿不到** |
| C11 生命週期 | 目前的階段 | `status` | 拿不到，改用 `caseStatus` 就可以 |

模式的判斷是 `d.projectionType === 'METADATA_ONLY'`（`case-detail-page.component.ts`）。七欄裡沒有 `projectionType`。

而且走不到判斷那一步：`mapCaseDetail` 先執行 `item.submissions.map(...)`，`submissions` 不存在，丟出 `TypeError`。

七欄裡的 `projectName`、`hospitalId`、`reviewerId`、`rereviewerId`、`updatedAt`，頁面目前**一個都沒有顯示**。

前端的測試（`resolves the metadata-only control center and hides command CTAs`）用的是完整形狀的 view model 加上 `projectionType: 'METADATA_ONLY'` 與一筆複審提交，那不是後端會給的形狀。

## 四、UI-014 revision 歷程頁

### 4.1 頁面規格

| 位置 | 內容 | 與 RC-VIS-05 相比 |
|---|---|---|
| §6.5「可看」 | stage、revisionNo、submittedAt、submittedBy、currentEffective | 一致。RC-VIS-05 註明 `submittedBy` 就是 `reviewerId`，`isCurrentEffective` 由 `status` 與 `supersededBySubmissionId` 表示 |
| §12.3 的範例 | `submittedBy`、`isCurrent`、`viewMode: "METADATA_ONLY"` | **欄位名稱與 API 不同**：API 是 `reviewerId`、`supersededBySubmissionId`，沒有 `isCurrent`、`viewMode`。範例若是 view model 就沒有問題，但文件沒有說明它是哪一層 |

欄位的範圍沒有落差，只有 §12.3 的名稱要釐清。

### 4.2 前端的現況

`getRevisionHistory` 同時呼叫案件詳情與歷程兩個端點，從案件詳情取 `status`、`actorRole`、`projectionType`：

- 案件詳情那一邊會丟 `TypeError`（同第三節），整頁載入失敗。
- 即使不丟錯：`projectionType` 不存在，`METADATA_HISTORY` 模式進不去；`status` 不存在。
- 歷程那一邊沒有問題：`mapSubmission` 用到的欄位 `SYSTEM_ADMIN` 都拿得到，`totalScore`、`comment` 是 null，型別本來就允許。

## 五、待決定

### M-1　頁面規格的正文要不要改成七欄

- **A（建議）**：改。UI-010 的 §4.3、§4.4、§4.5、§5.5、§9.4 對 system admin 的描述改成「只有七欄」，並寫明七欄各在哪一區顯示（M-2）。`docs/05-ui/` 不是律法，不需要簽章。
- B：不改文件，只改程式。規格與程式會繼續不一致。

### M-2　metadata 模式的版面

七欄撐不起現在的區塊。

| 選項 | 版面 |
|---|---|
| **A（建議）** | 頁首（`caseId`、`caseStatus`）＋投影提示＋一個「案件 metadata」區塊顯示其餘五欄（`projectName`、`hospitalId`、`reviewerId`、`rereviewerId`、`updatedAt`）＋生命週期（用 `caseStatus`）＋導覽只留「Revision 歷程」。初審、複審摘要都不顯示 |
| B | A 再加上：複審摘要區改用歷程端點的資料（`SYSTEM_ADMIN` 拿得到每一筆的階段、版次、狀態、時間） |
| C | 維持現在的區塊，後端的 metadata 形狀加欄位 |

A 的理由：摘要區在規格裡的內容（分數、提交狀態）大多不在白名單裡；要看提交的 metadata，導覽到歷程頁就有。B 多一次呼叫，換來的是把歷程頁的第一筆搬到詳情頁。C 要改 `http.yaml`（律法）而且與 D-036「不得擴大白名單」衝突。

### M-3　前端怎麼判斷拿到的是 metadata 形狀

| 選項 | 作法 | 評估 |
|---|---|---|
| **A（建議）** | 看回應的形狀：有 `caseStatus`、沒有 `status` 就是 metadata。service 回傳兩種 view model 的聯集，頁面依種類分支 | 以伺服器的回應為準。前端的規則「不得以 local state 覆蓋 server truth」 |
| B | 看目前的角色：`ActorContextService` 的角色是 `SYSTEM_ADMIN` 就當成 metadata | 角色是 shell 給的；身分切換的中間狀態可能與回應不一致（`05-current-state.md` 第五節第 1 項） |
| C | 後端在 metadata 形狀加上 `projectionType` | 要修法；D-036 的白名單要改 |

A 的判斷寫在 service 的一個地方，並加上測試：回應是七欄時得到 metadata 的 view model、是完整形狀時得到完整的 view model、兩種都不是時明確失敗而不是默默當成其中一種。

### M-4　歷程頁怎麼拿到狀態與模式

- **A（建議）**：沿用 M-3。`getRevisionHistory` 仍然呼叫兩個端點；案件詳情是 metadata 形狀時，`status` 取 `caseStatus`、模式是 `METADATA_HISTORY`、預設的頁籤是全部。
- B：歷程頁不再呼叫案件詳情，只用歷程端點。但歷程端點沒有案件的狀態，`READONLY_TERMINAL` 模式判斷不出來。

### M-5　UI-014 §12.3 的範例

- **A（建議）**：註明它是 view model，並附上 API 欄位的對應（`submittedBy` ← `reviewerId`、`isCurrent` ← `status` 與 `supersededBySubmissionId`、`viewMode` ← 案件詳情的形狀）。
- B：改成 API 的欄位。

### M-6　測試的假回應

不論上面怎麼選：`SYSTEM_ADMIN` 的案件詳情，單元測試與 E2E 的假回應要用七欄的形狀。現在沒有任何一個測試用七欄的形狀，這就是 F-2 沒有被擋到的原因。

目前 E2E 有沒有 `SYSTEM_ADMIN` 的情境，沒有查；`05-current-state.md` 第八節記載 standalone 模式固定為 `HOSPITAL_MANAGER`。

### 建議總表

| 題 | 建議 | 改什麼 | 要不要簽章 |
|---|---|---|---|
| M-1 | A | UI-010 的正文 | 不用 |
| M-2 | A | UI-010；前端的案件詳情頁 | 不用 |
| M-3 | A | 前端的 service 與 view model | 不用 |
| M-4 | A | 前端的歷程頁 | 不用 |
| M-5 | A | UI-014 §12.3 | 不用 |
| M-6 | — | 前端的測試 | 不用 |

全部照建議時，順序是：頁面規格（M-1、M-5，把 M-2 的版面寫進去）→ 前端（先改測試的假回應，再改 service、案件詳情頁、歷程頁）。都不碰律法。提交的依據是 D-036、D-094 第 9 點（七欄）與 D-088（HTTP 契約）。

## 六、核對時另外看到的

### S-1　`SYSTEM_ADMIN` 的案件列表拿到的欄位比白名單多（後端，待負責人判斷）

| | |
|---|---|
| 白名單 | `permission-matrix.md` §5.1：shared base（`caseId`、`caseStatus`、`projectName`、`hospitalId`、`reviewerId`、`rereviewerId`、`updatedAt`）加上 UI-009 的 `batchId`、`dueAt`。「`SYSTEM_ADMIN` 僅可見上述白名單集合」。api-spec §3.6 相同 |
| 程式 | `ListReviewCasesUseCase` 第 44 行：`SYSTEM_ADMIN` 只把 `projectionType` 設成 `METADATA_ONLY`，欄位沒有刪減 |
| 契約 | `ListReviewCases` 只有一種形狀，每一筆有 `projectId`、`batchId`、`hospitalId`、`status`、`reviewerId`、`rereviewerId`、`medicalRecordType`、`sourceCaseKey`、`createdAt`、`assignedAt`、`dueAt`、`projectionType` |

白名單以外、但 `SYSTEM_ADMIN` 在列表拿得到的：`projectId`、`medicalRecordType`、`sourceCaseKey`、`createdAt`、`assignedAt`。反過來，白名單有、列表沒有的：`projectName`、`updatedAt`；`caseStatus` 在列表叫 `status`。

這算不算缺陷，要負責人判斷：

- 這些欄位都不是作答內容、分數或評語（D-010 保護的對象）。`sourceCaseKey` 是病歷的來源鍵。
- 但白名單的文字是「僅可見」，D-036 也要求不得因為 DTO 的 getter 自動擴大白名單。案件詳情照白名單收斂了，列表沒有。
- `ListReviewCases` 已經立法（D-066），輸出的來源寫 api-spec。規格沒有 `SYSTEM_ADMIN` 的列表該有哪些欄位的規則。

若判斷是缺陷，它是一筆漏網（已立法、閘門全綠），要記進 `12-escaped-defects.md`，修正要修法（`http.yaml` 要有 `SYSTEM_ADMIN` 的形狀）。**本文件沒有把它當成已確認的缺陷。**

有沒有範例以 `SYSTEM_ADMIN` 的身分列出案件並比對欄位，沒有查。

### S-2　同一個狀態欄位有兩個名稱

案件的狀態在完整形狀與列表叫 `status`，在 metadata 形狀與各個命令的回應叫 `caseStatus`。這是後端的現況（契約照程式立法，D-088 第 2 點），不是這次要處理的，但它是 F-2 的成因之一。

## 七、限制

1. 沒有執行前端，也沒有對真實的後端操作。
2. 頁面規格只讀了與 system admin、metadata 有關的段落，沒有通讀。
3. `SYSTEM_ADMIN` 實際上從哪裡進入這兩頁（列表、直接輸入網址、報表），路由的守衛怎麼寫，沒有查。
4. UI-009（案件列表頁）對 metadata 的規格沒有核對，只在 S-1 看了後端。
5. 前端其他頁面（分派頁、初審與複審作業頁）對 `SYSTEM_ADMIN` 的行為沒有核對；權限矩陣把這些頁面對 `SYSTEM_ADMIN` 列為不可進入。
