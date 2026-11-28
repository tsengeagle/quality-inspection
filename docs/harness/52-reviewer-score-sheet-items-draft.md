# 審查醫師怎麼取得評分表的題目

狀態：**已裁決**（2026-11-28）。負責人指示先讀 D-033 確認（結果：D-033 保護的是初審的答案、分數、評語與可以反推的衍生資訊，題目定義不在內，題目的 `weight` 沒有被討論過），確認後 R-1～R-8 全部照建議裁決。條文是 D-101，由修法 PR 寫入 `decision-log.md` 才生效。寫條文時多出幾件本草稿沒有的事（多角色、RC-DET-03 不適用、新端點在 HTTP 契約的過渡、HTTP 的範例延後），記在 D-101 第 7 點，**待負責人確認**。下面是起草時的版本。本文件不是律法；題號 R-1～R-8 只在本文件使用。事實是在 main = `02811a3` 上查的。

起因：前後端全流程實測的 X-01（`docs/review/e2e-live-backend-run-2026-11-28.md`）。題目清單上還沒有這一題；要不要列入、排在哪裡，請負責人決定。

## 一、問題

初審醫師與複審醫師要作答，但系統沒有任何方式讓他們知道題目是什麼。

- 後端：沒有一個他們能呼叫的介面會回傳題目的名稱、題型、滿分、是否必答。
- 前端：初審、複審作業頁的題目區是 STUB，只會顯示草稿裡已經有的答案列（X-02）。就算把表單做出來，也沒有資料可以畫。

結果是：在真實的後端上，醫師開始初審之後畫面沒有任何題目，按提交得到 422 `REQUIRED_ITEMS_NOT_ANSWERED`。流程走不下去。

## 二、查到的事實

| # | 事實 | 來源 |
|---|---|---|
| 1 | 評分表的四支查詢（詳情、章節、版本、列表）的讀取角色是 `SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`AUDITOR`。**`REVIEWER`／`REREVIEWER` 不開放，理由寫的是「審查時用案件的 snapshot」** | D-050 第 7 點；`specs/score-sheet/usecases.yaml` 的 `GetScoreSheetSections`；`GetScoreSheetSectionsUseCase` 第 28 行 |
| 2 | 醫師也不能讀專案與抽選批次。「他們審查所需的資訊從被指派的案件取得：案件詳情帶專案名稱，**評分依據來自案件的 snapshot（D-011）**」 | D-051 |
| 3 | D-051 不開放專案的理由：專案詳情有品質線與初審、複審的權重，初審醫師知道品質線可能把分數打在附近 | D-051 的 Evidence |
| 4 | snapshot 沒有實作。評分表的 `snapshotForSubmission` 已裁決**不做**；抽選的快照「沒有被任何行為使用」，內容與保存方式仍等 D-011 | D-094 第 2、4 點；`specs/score-sheet/model.yaml` 第 118 行；`specs/sampling-batch/model.yaml` 第 76、77 行 |
| 5 | 不開放改版之後，專案目前的評分表版本就是案件建立時的版本，兩者沒有差別 | D-094 第 1、3 點 |
| 6 | 案件詳情（`full` 形狀）給醫師的 `initialReviewContext`、`rereviewContext` 只有 `scoreSheetVersionId`，沒有題目 | `specs/review-case/http.yaml` 第 73～90 行；實測的回應 |
| 7 | 提交明細的 `answers` 只有題目代號與作答的值 | 實測 `GET /review-submissions/{id}` |
| 8 | UI-012 的頁面規格要求「每個 section 下列出 score items」，資料寫的是「`ScoreSheetDefinition`［透過 snapshot 呈現］」；它的「對應 API 規劃」只列了開始、草稿、提交、修正四支命令，**沒有任何一支讀題目的** | `docs/05-ui/ui-page-spec-ui012初審作業頁.md` 第 39、243、630～650 行 |
| 9 | 實測：初審醫師呼叫 `GET /score-sheets/{id}/sections` 得到 403 `ROLE_DENIED` | 2026-11-28，本機，`main` 的後端 |
| 10 | 題目的欄位：`itemId`、`code`、`title`、`scoreType`（`FIXED`、`RANGE`、`BOOLEAN`）、`maxScore`、`weight`、`requiredRule`、`conditionExpression` | `specs/score-sheet/model.yaml` |
| 11 | 計分：`BOOLEAN` 是 true 得滿分 × 權重；`FIXED` 得滿分 × 權重；`RANGE` 得作答 × 權重。必答的檢查只看 `ALWAYS_REQUIRED`，`CONDITIONAL` 的求值延後 | `specs/review-case/model.yaml` 第 101、111 行 |

把 1、2、4 放在一起看：**兩個裁決都假設醫師從 snapshot 拿題目，而 snapshot 不存在，之後也裁決不做。** 這個假設落空之後，沒有人回頭補。這不是程式違反律法，是律法本身缺一塊。

## 三、要裁決的

### R-1 題目從哪裡給

| 選項 | 作法 | 優點 | 缺點 |
|---|---|---|---|
| **A（建議）** | 新增一支以案件為範圍的查詢，例如 `GET /review-cases/{caseId}/score-sheet`。後端用案件的 `scoreSheetVersionRef` 找到評分表，回傳章節與題目 | 符合 D-050、D-051 的本意「從被指派的案件取得」；授權直接沿用案件的讀取規則，不用跨聚合判斷；醫師不需要知道評分表的 id | 多一支端點、一個 use case |
| B | 把章節與題目放進案件詳情的 `initialReviewContext`、`rereviewContext` | 前端少一次呼叫 | 每一次讀案件詳情都帶整份題目；要改已立法的 `full` 形狀；管理者、稽核的詳情也跟著變大 |
| C | 把 `GET /score-sheets/{id}/sections` 開放給醫師 | 不用新端點 | 與 D-050 第 7 點的明文相反；要判斷「這份評分表屬於我被指派的案件」，是跨聚合的檢查；案件給醫師的是版本 id（`…:v1`），不是評分表的 id |
| D | 把 D-011 的 snapshot 做出來 | 與原本的設計一致 | 範圍大；D-094 才裁決不做，理由（不開放改版，版本不會變）仍然成立 |

建議 A。它不需要 snapshot：事實 5 說明版本不會變，直接讀評分表目前的版本就等於案件建立時的版本。

### R-2 誰可以呼叫

建議：**能讀這筆案件完整詳情的人**——本案的初審醫師、複審醫師（現任），以及同院區的 `HOSPITAL_MANAGER`、`AUDITOR`。沿用案件詳情的 guard，不另外訂。

`SYSTEM_ADMIN`：建議**不開放**這一支。它的案件詳情是 metadata 形狀；要看題目可以用既有的 `/score-sheets/{id}/sections`（D-050 第 7 點已說明題目定義不算「完整評分內容」）。

### R-3 給醫師哪些欄位

| 選項 | 內容 |
|---|---|
| **A（建議）** | 與 `GetScoreSheetSections` 相同：章節的 `sectionId`、`title`，題目的全部欄位 |
| B | 不給 `weight` |

建議 A。D-051 擔心的是專案的**品質線**與**初審、複審的權重**，這一支不會給。題目的 `weight` 是另一回事：醫師看得到自己的總分，本來就推得出來；`RANGE` 題型也需要 `maxScore` 才知道能填多少。**這一點請負責人特別確認**，因為它關係到盲審。

### R-4 讀題目算不算「讀過完整初審」

建議：**不算。** 複審醫師指派時的 `REREVIEWER_HAS_READ_INITIAL`（D-055、D-084）記的是讀過初審的作答與分數。題目定義沒有任何人的作答，不留讀取者紀錄。

### R-5 案件在哪些狀態可以讀

建議：**不限狀態**，只要通過 R-2 的授權。結案之後要顯示已提交的內容（X-03），仍然需要題目的名稱。還沒有分派的案件，管理者與稽核也可以讀。

### R-6 回應的形狀

建議：`{ caseId, scoreSheetVersionId, sections: [...] }`，`sections` 與章節查詢相同。帶 `scoreSheetVersionId` 是讓前端能對照案件詳情裡的版本。

案件的專案還沒有綁定評分表的情況不會發生：專案要先綁定、發布、啟用才能抽選出案件。若評分表找不到，建議回 404（代碼請在修法時定，例如沿用 `SCORE_SHEET_NOT_FOUND`）。

### R-7 既有文件要一起改的

- D-050 第 7 點、D-051 的「審查時用案件的 snapshot」：新的裁決註明改由這一支查詢提供。
- `permission-matrix.md`：補上這一支的讀取角色。
- `api-spec.md`：新增這一支；§8、§10 的相關說明。
- UI-012、UI-013 的頁面規格：「對應 API 規劃」補上這一支，把「透過 snapshot 呈現」改掉。

### R-8 分批

| 批 | 內容 | 簽章 |
|---|---|---|
| 1 | 修法：裁決條文；`specs/review-case/` 的 use case、規則、範例（含 HTTP 的）；`http.yaml` 新端點；`law/coverage.txt` 登記新檔案；上面 R-7 的文件。新範例凍結在 `known-test-failures.txt` | 要 |
| 2 | 建構（後端）：use case、controller、回應的型別；解凍範例 | 不用 |
| 3 | 前端：作業頁的題目區（X-02）與唯讀時顯示已提交的內容（X-03） | 不用；**要先寫頁面規格**，另立一題 |

批 1 要排在等簽章的鏈的最後面（現在是 #215 之後），因為它同樣在 `decision-log.md` 的檔尾加裁決。批 2 依 D-097 要等批 1 合併。

## 四、建議的範例（供修法時用，還沒有寫成規格）

| 範例 | 預期 |
|---|---|
| 本案的初審醫師讀題目 | 成功，章節與題目與評分表相同 |
| 本案的複審醫師讀題目 | 成功；之後指派或改派不會因此被 `REREVIEWER_HAS_READ_INITIAL` 拒絕 |
| 不是本案的醫師 | `ACCESS_DENIED` |
| 同院區的管理者、稽核 | 成功 |
| 別的院區的管理者 | 拒絕（代碼依案件詳情現有的規則） |
| `SYSTEM_ADMIN` | 拒絕（依 R-2） |
| 案件不存在 | `CASE_NOT_FOUND` |
| 已結案的案件 | 成功（依 R-5） |
| 經由 HTTP | 回應符合 `http.yaml` 的形狀 |

## 五、沒有確認的

- 案件詳情對醫師的授權規則（哪些 guard、複審醫師「曾經的」算不算）只讀了規格的標題，沒有逐條核對；R-2 寫「沿用」，修法時要對照實際的 guard 寫明。
- 沒有回頭讀 D-033（盲審）的全文確認題目的 `weight` 有沒有被討論過。
- 後端計分時是怎麼用 `scoreSheetVersionRef` 找到評分表的（哪一個 port），沒有讀程式；選項 A 假設可以沿用。
- `conditionExpression` 目前不求值。給了醫師之後前端要不要用它，屬於頁面規格。
- 選項 A 的路徑名稱、錯誤代碼只是舉例。
- 都是讀文件與規格，加上一次實測（事實 9）；選項沒有試寫。
