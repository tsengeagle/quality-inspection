# api-spec 路徑核對（2026-11-18）

`docs/harness/05-current-state.md` 第五節第 4 項「api-spec 的路徑與程式不一致，尚未逐一核對」的核對結果。

- 核對時間點：`main` 的 `81a235f`。
- 範圍：只核對 **HTTP 方法與路徑**。request／response 的欄位、query 參數、錯誤代碼都**沒有**核對。
- 本文件只記錄差異與選項，不改 api-spec、不改程式。要改哪一邊，需要負責人裁決（見第五節）。

## 一、方法

以腳本取三個來源的端點，把路徑參數一律正規化為 `{}`（例如 `{id}` 與 `{scoreSheetId}` 視為相同），去掉 query string 後比對：

| 來源 | 取法 | 數量 |
|---|---|---:|
| api-spec | `docs/03-contracts/api-spec.md` 中所有 `` `方法 /路徑` `` 形式的字串 | 46 個不重複端點，其中 `PUT /resource/{id}` 是第 6 節的寫法說明，不是端點，扣除後 45 個 |
| 後端 | 各 `*Controller.java` 的 `@RequestMapping` 前綴加上 `@Get/Post/Put/Patch/DeleteMapping` | 53 |
| 前端 | `features/**/services/*.ts` 的 `http.get/post/...` 呼叫，代入各 service 的 base URL | 27 |

## 二、結論

1. **前端呼叫的 27 個端點，後端全部都有。** 目前沒有「前端呼叫不存在的路徑」這種執行期錯誤。
2. **api-spec 有、後端沒有：9 個。** 其中 9 個都在後端有對應的「同一個動作、不同路徑」（第三節）。
3. **後端有、api-spec 沒有：17 個。** 9 個是第三節的另一半；另外 8 個在 api-spec 完全沒有記載（第四節）。
4. **與裁決衝突：** D-022 規定的路徑樣式與程式、與之後的 D-053／D-054 都不一致；`GET /review-submissions/{id}` 是 D-022 明文廢除的路徑，後端與前端仍在使用（第五節）。

## 三、同一個動作、路徑不同（9 組）

| # | api-spec（節次、行號） | 後端實際路徑 | 前端使用 |
|---|---|---|---|
| 1 | 8.3 `POST /score-sheets/{scoreSheetId}/publish-version`（L532） | `POST /score-sheets/{id}/publish` | — |
| 2 | 10.8 `POST /review-cases/{caseId}/return-initial-review`（L980） | `POST /review-cases/{caseId}/initial-review/return` | — |
| 3 | 11.3 `POST /review-cases/{caseId}/submissions/{submissionId}/initial-review/submit`（L1059） | `POST /review-cases/{caseId}/initial-review/submit`（**沒有** `submissionId`） | ✓ 後端路徑 |
| 4 | 11.4 `POST /review-cases/{caseId}/submissions/{submissionId}/initial-review/revise`（L1085） | `POST /review-cases/{caseId}/initial-review/submissions/{submissionId}/revise` | ✓ 後端路徑 |
| 5 | 12.1 `POST /review-cases/{caseId}/rereview/start`（L1138） | `POST /review-cases/{caseId}/start-rereview` | ✓ 後端路徑 |
| 6 | 12.3 `POST /review-cases/{caseId}/submissions/{submissionId}/rereview/submit`（L1160） | `POST /review-cases/{caseId}/rereview/submit`（路徑沒有 `submissionId`，但 request body 的 `submissionId` 為必填） | ✓ 後端路徑 |
| 7 | 12.4 `POST /review-cases/{caseId}/submissions/{submissionId}/rereview/revise`（L1172） | `POST /review-cases/{caseId}/rereview/submissions/{submissionId}/revise` | ✓ 後端路徑 |
| 8 | 13.1 `GET /review-cases/{caseId}/submissions/{submissionId}`（L1223） | `GET /review-submissions/{submissionId}` | ✓ 後端路徑 |
| 9 | 13.2 `GET /review-cases/{caseId}/revisions`（L1257） | `GET /review-cases/{caseId}/revision-history`（另有 `GET .../rereview-revisions`，見第四節） | ✓ 後端路徑 |

第 3、6 組的 `submissionId` 位置不同，兩者行為也不一樣：

- 第 3 組（初審提交）：路徑與 body 都沒有 `submissionId`，後端由案件取得目前的 submission。
- 第 6 組（複審提交）：路徑沒有，但 `SubmitRereviewRequest.submissionId` 是 `@NotBlank` 必填，`SubmitRereviewUseCase` 以它載入並驗證是否為目前版本。**修正 api-spec 時不能把它拿掉**，只是從路徑移到 body。

## 四、後端有、api-spec 完全沒有記載（8 個）

| 端點 | 用途（依程式） | 前端使用 |
|---|---|---|
| `POST /review-cases/{caseId}/close-without-rereview` | 不送複審直接結案 | — |
| `POST /review-cases/{caseId}/send-to-rereview` | 送複審 | — |
| `POST /review-cases/{caseId}/reopen` | `RETURNED` → `PENDING_INITIAL_REVIEW`（`ReopenInitialReviewUseCase`，限 `HOSPITAL_MANAGER`）。是「退回初審」之後重新開放初審，**不是**重開 `CLOSED` 案件，與「CLOSED 不可 reopen」的裁決不衝突；但名稱容易誤解 | — |
| `GET /review-cases/{caseId}/rereview-revisions` | 複審 revision 歷程 | — |
| `GET /sampling-batches/{batchId}/export` | 匯出抽選結果 | — |
| `GET /score-sheets` | 評分表清單 | — |
| `GET /score-sheets/{id}/sections` | 評分表的段落 | — |
| `GET /score-sheets/{id}/version` | 評分表目前版本（UI-005 需要的「版本清單」不是這支，見 `05-current-state.md` 第二節） | — |

這 8 個都沒有前端在用。本次確認的只是 **api-spec 沒有記載它們的路徑**，不代表契約都沒有定義：

- `GET /score-sheets`、`/score-sheets/{id}/sections`、`/score-sheets/{id}/version`：D-050 第 7 點已裁決讀取角色（`SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`AUDITOR`）與院區範圍，並規定列表與章節的回應格式。補寫 api-spec 時要依 D-050。
- 其餘 5 個：在 `decision-log.md` 中搜尋不到這些路徑字串。是否有裁決以其他名稱（例如 use case 名稱）定義其行為，本次沒有逐一核對；補寫 api-spec 前要先查。

2026-11-12 快照列出「15 支 use case、28 個錯誤代碼只存在程式裡」，本次未逐一對照該清單。

## 五、與裁決的衝突（需要負責人裁決）

### 5.1 D-022 的路徑樣式

D-022（`decision-log.md` 約 L819 起，`accepted`）規定：

> 所有 review workflow command endpoint 採用 case-rooted sub-resource path 風格：`/review-cases/{caseId}/submissions/{submissionId}/{stage}/action`。……舊的 `/review-submissions/{submissionId}/...` paths 廢除。

實際情況：

| 來源 | 樣式 |
|---|---|
| D-022、api-spec 11.3／11.4／12.3／12.4／13.1 | `/review-cases/{caseId}/submissions/{submissionId}/{stage}/action`（stage 在 `submissions` 之後） |
| D-053（複審結案）、D-054（初審固定）與 api-spec 中對應的兩支 | `/review-cases/{caseId}/{stage}/submissions/{submissionId}/action`（stage 在前） |
| 後端程式、前端 | 與 D-053／D-054 相同（stage 在前）；提交則不帶 `submissionId` |

D-022 的理由之一是「實作已以 case-rooted paths 通過測試」，但現在的實作與 D-022 寫下的樣式不同。也就是裁決之間、裁決與程式之間都不一致。

### 5.2 `GET /review-submissions/{submissionId}`

D-022 明文廢除 `/review-submissions/...`，並規定改為 `GET /review-cases/{caseId}/submissions/{submissionId}`。後端（`SubmissionController`）與前端（`cases.service.ts` 的 `submissionBaseUrl`）仍使用舊路徑。

D-022 的另一個理由是「caseId 成為必要路徑參數，強化 hospital scope 驗證」。本次查看 `GetSubmissionDetailUseCase`：它由 submission 取得所屬案件，再以該案件呼叫 `accessPolicy.requireAccess`，並對複審內容做讀取限制與遮罩。所以**院區與角色的檢查仍在**，只是路徑不符合裁決。沒有寫測試驗證這一點，只看了程式。

### 5.3 選項

| 選項 | 內容 | 影響 |
|---|---|---|
| A. 以程式為準（建議） | 新增裁決修正 D-022 的路徑樣式為「stage 在前」，與 D-053／D-054 一致；api-spec 第 3、4 節的 9 組依程式改寫，並補上第四節的 8 個端點 | 只改文件（修法 PR）。做法與 D-056「匯出請求格式以程式為準」相同。前端、後端、測試都不動 |
| B. 以 D-022 為準 | 後端 9 組路徑改為 D-022 樣式，前端跟著改，D-053／D-054 也要改 | 改程式、前端與測試；需要過渡期或同時切換 |
| C. 混合 | 例如命令端點依 A，`GET /review-submissions/{id}` 依 B 改為 case-rooted | 5.2 單獨處理 |

建議 A 的理由：前端、後端、測試已一致，且較新的裁決（D-053、D-054）採用的就是程式的樣式；改程式只為了符合較舊的文字，風險與成本較高。5.2 的院區檢查目前存在，是否仍要改為 case-rooted，可在 A 的同一份裁決中一併決定（維持現狀或改路徑）。

## 六、這次沒有核對的

- request／response 欄位、query 參數、錯誤代碼、權限。只核對了方法與路徑。
- 第四節 8 個端點的行為是否符合其他裁決（例如 `close-without-rereview`、`send-to-rereview` 與狀態機的關係）。
- `docs/06-frontend/ui-api-binding-matrix.md` 等前端文件中的路徑。
- api-gateway 是否另外改寫路徑。
