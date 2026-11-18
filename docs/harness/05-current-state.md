# 專案現況（2026-11-18）

這份文件是時間點快照。之後狀況改變時，更新本檔並修改標題日期。

- 快照時間點：`main` 的 `6f1303f`（PR #71 合併後），2026-11-18；第一、三節另補上之後合併的 #72、#74（`main` 的 `6c055e8`）。
- 同日晚間更新：第一、三、五、七、九節更新到 `main` 的 `2feb5bf`（PR #85 合併後），反映 #75～#85。第二節（頁面進度）沒有變化。
- 本次改寫了第一到第九節；2026-11-12 版的內容在 git 歷史（本檔在 `6f1303f` 的版本）。2026-11-12 版列出、但這次沒有重新核對的項目，集中在第九節，沒有刪除。
- 第十節是 2026-11-13～14 的閘門建置紀錄，原樣保留。
- 數字都是這次實際執行或查詢的結果；「未驗證」的就寫未驗證。

## 一、版本控制與進行中的 PR

| PR | 內容 | 狀態 |
|---|---|---|
| #72 | 修法：新增 `frontend-check`，在 CI 執行前端 typecheck、單元測試、E2E | 已合併（`6c055e8`） |
| #74 | 修法：登記第二支實體金鑰（YubiKey 5 Nano）為核准者 tsengeagle 的備援；核准者指引補上 Windows 注意事項 | 已合併 |
| #75、#78、#79 | 必要檢查加入 `frontend-check`；記錄必要檢查的驗收結果（#77：驗證用，未合併）；ruleset 只允許 merge commit | 已合併 |
| #76 | api-spec 路徑核對報告（`docs/review/api-spec-path-reconciliation-2026-11-18.md`） | 已合併 |
| #80 | 修法：D-058，api-spec 路徑以程式為準，D-022 部分取代 | 已合併 |
| #81 | 修法：D-059，移除 `GET /review-cases/{caseId}/rereview-revisions` | 已合併 |
| #82 | 建構：移除 `rereview-revisions` 端點，並以測試鎖定 `revision-history` 對複審資料的保護 | 已合併（`b8cd894`） |
| #83 | 建構：移除 D-032 已撤銷的 CLOSED 複審例外入口（後端 `hasExceptionEntry`、前端 C22 例外按鈕） | 已合併（`3a42cde`） |
| #84 | 文件：16 份前端規劃、UI、設計文件標記例外入口已撤銷並已自程式移除 | 已合併（`31555aa`） |
| #85 | 建構：評分表列表、章節查詢的回應依 D-050 第 7 點包成物件 | 已合併（`2feb5bf`） |

除了本快照所在的 PR，其餘 PR 都已合併或關閉。合併一律用 merge commit；修法 PR 需要負責人以 SSH 金鑰對內容雜湊簽章（`law/README.md`）。

## 二、頁面進度

17 頁中，10 頁有頁面規格也有前端。缺的 7 頁中，6 頁的後端 API 已存在；UI-005 只有「目前版本」的 API，缺頁面需要的版本清單。

| 模組 | 頁面 | 頁面規格 | 前端 | 後端 API |
|---|---|:-:|:-:|:-:|
| 專案與規則管理 | UI-001 專案列表、UI-002 專案詳情、UI-003 專案編輯、UI-004 評分表管理 | ✗ | ✗ | ✓ `/review-projects`、`/score-sheets` |
| | UI-005 版本檢視 | ✗ | ✗ | 部分：只有 `GET /review-projects/{projectId}/version`、`GET /score-sheets/{id}/version`，回傳目前版本。頁面清冊要的 project／score sheet 版本清單沒有 API（清冊本身寫「建議後續補 version query API」） |
| 抽選作業 | UI-006 抽選執行、UI-007 批次列表 | ✓ | ✓ | ✓ |
| | UI-008 批次詳情 | ✗ | ✗（沒有 `batches/:batchId` route） | ✓ `GET /sampling-batches/{batchId}` |
| 案件與審查作業 | UI-009 案件列表～UI-014 revision 歷程 | ✓ | ✓ | ✓ |
| 查詢與報表 | UI-015 Dashboard | ✓（#70） | ✓（#71） | ✓ |
| | UI-016 稽核查詢 | ✗ | ✗ | ✓ `/audit-trails`、`/lifecycle` |
| | UI-017 報表匯出 | ✓（#68） | ✓（#69） | ✓ |

前端 route：`/sampling/*`、`/cases/*`、`/reports/dashboard`、`/reports/export`、`/demo`。整個「專案與規則管理」沒有前端，專案與評分表目前只能透過 API 建立。

## 三、驗證閘門

| 閘門 | 在 CI | 這次的結果 |
|---|:-:|---|
| 後端 `./gradlew check`（Linux、Windows） | ✓ `backend-check` | 2026-11-18 晚間本機（`2feb5bf`，JDK 17，`cleanTest check`）：1223 次測試執行；建置的測試閘門回報「執行 957 個測試方法」，失敗 0、跳過 0，已知失敗清單為空。含 ArchUnit 與凍結基線。比早上的 958 少 1：#83 刪除了只驗證 `hasExceptionEntry` 的單元測試。（#86 曾寫成「930 個方法、口徑不同」，那是另外用 JUnit 報告去重的數字；閘門的數字以 CI 紀錄為準，例如 #87 的 run 44345483626） |
| 修法判決 `law-check`、`law-selftest` | ✓ | — |
| 規格格式 `spec-check` | ✓ | `specs/review-project`、`specs/score-sheet` |
| 前端 typecheck、Karma、Playwright | ✓ `frontend-check`（#72） | 2026-11-18 晚間本機（`2feb5bf`）：typecheck 通過；單元測試 221 個通過（#83 刪除 4 個例外入口測試、新增 1 個）；E2E 18 個通過、3 個 demo 流程 skip。同日早上在乾淨 checkout 以 `CI=true`：typecheck 通過；單元測試 224 個通過；E2E 18 個通過、3 個 demo 流程 skip。GitHub 上：#72 的 PR（`76cbd79`）結果相同；合併後 `main` 的第一次執行（`6c055e8`，run 44293068034）通過 |
| Oracle | ✗ | V1～V5 與 Hibernate validate 在 Oracle XE 21c 驗證過（PR #55）。V6、SERIALIZABLE 匯出、儀表板的單一聚合查詢沒有在 Oracle 上跑過。負責人決定暫不加入 CI |
| hosted（shell）模式 | ✗ | 從未驗證 |

2026-11-12 時的 14 個已知失敗（D-037、D-038、D-032 未實作）已全數轉綠，清單已清空。

## 四、近期的裁決與修法

詳細內容以 `docs/01-requirements/decision-log.md` 為準。

| 裁決 | 摘要 |
|---|---|
| D-048 | 後端採 DDD 加 CQRS；查詢經 application 的查詢埠 |
| D-049 | 已發布版本再次發布的錯誤代碼；更新草稿時沒給的權重回預設 |
| D-050 | 評分表的版本、建立、更新、發布與查詢 |
| D-051 | `REVIEWER`／`REREVIEWER` 不能讀取專案與抽選批次 |
| D-052 | 備援核准金鑰的修法（PR #33）破例以人工審查取代簽章 |
| D-053 | 複審結案命令的 API 與錯誤代碼 |
| D-054 | 初審固定的 API 與錯誤代碼 |
| D-055 | 以複審指派歷史證明「不是歷史複審醫師」；匯出逐案分數的遮罩 |
| D-056 | 匯出請求格式以程式為準 |
| D-057 | 匯出依案件建立時間過濾、區間必填、逐案資料列分頁；儀表板區間選填 |

2026-11-16～17 的修法紀錄（`law/amendments/`）：Flyway 導入（step1、step3）、D-055 複審指派歷史、D-054／D-055 文件同步、匯出請求格式、D-057、UI-017 頁面規格（#68）、UI-015 頁面規格（#70）。

後端本輪的重點變更：

- Flyway migration V1～V6；V6 新增 `review_case.created_at`。
- 匯出在 SERIALIZABLE 交易中讀取，摘要與資料列是同一份快照。
- 儀表板以單一聚合查詢算出 4 個數字（#67）。
- 找不到資源時回 `*_NOT_FOUND` 錯誤代碼（#59）。

## 五、已知缺口與技術債

| # | 項目 | 類型 | 狀態 |
|---|---|---|---|
| 1 | 身分就緒：hosted 模式下 shell 分別送出使用者、角色清單、目前角色（`TOPIC_USER_INFO`、`TOPIC_USER_ROLES`、`TOPIC_CURR_ROLE`），切換身分的中間狀態可能是「新使用者＋舊角色」。UI-015 已在身分未解析或不持有報表角色時停用查詢，但這個中間狀態仍會通過 | 前端共用層（`core/actor-context`） | 需先確認 shell 送出事件的順序、有沒有「切換完成」的訊號（PR #71 Copilot 第三則意見） |
| 2 | UI-017 匯出頁的查詢與下載按鈕，沒有「持有報表角色」的判斷（UI-015 在 #71 已補） | 前端缺陷，已合併 | 併入第 1 項一起處理 |
| 3 | domain 內 15 處無參數的 `LocalDateTime.now()`（`ReviewCase` 6 處、`ReviewSubmission` 9 處）：用伺服器預設時區，且繞過注入的 `Clock` | 架構債 | 在 ARCH-02 凍結基線內；`closedAt` 因此在匯出頁標示為伺服器時間。改為 UTC `Instant` 需先裁決 |
| 4 | api-spec 的路徑與程式不一致 | 契約漂移 | 2026-11-18 已逐一核對（只核對方法與路徑）：見 `docs/review/api-spec-path-reconciliation-2026-11-18.md`。前端呼叫的端點後端都有；9 組同一動作路徑不同、8 個後端端點 api-spec 沒有記載；D-022 的路徑樣式與程式及 D-053／D-054 衝突。已處理：2026-11-18 負責人裁決採選項 A，D-058（以程式為準，修正 D-022 的路徑樣式），api-spec 與測試、前端文件同步（#80） |
| 4a | `GET /review-cases/{caseId}/rereview-revisions` 沒有角色檢查：院區範圍內的任何角色（含只應取得 metadata 的 `SYSTEM_ADMIN`）都能讀到複審的 `totalScore` 與 `comment`。目前沒有前端使用 | 權限缺口 | 已處理：2026-11-18 以測試證實（4 個情境全紅，`revision-history` 同樣情境全綠）；D-059 裁決移除（#81）；#82 移除端點，並新增 4 個測試鎖定 `revision-history` 的保護 |
| 4b | 評分表列表、章節查詢的回應：D-050 第 7 點規定 `{ items }`、`{ scoreSheetId, sections }`，程式直接回傳陣列 | 程式與裁決不符 | 已處理：#85（先改 5 個測試、修改前全紅）。`decision-log.md` D-058 第 6 點的文字仍描述修正前的狀態，屬裁決，未改 |
| 4c | `revision-history` 的 `hasExceptionEntry`：案件 CLOSED 且查看者是原複審醫師時為 `true`，前端據此顯示 D-032 已撤銷的「前往複審修正（例外）」入口 | 程式與裁決不符 | 已處理：#83 移除後端欄位與前端 C22 例外按鈕；#84 同步 16 份文件。共用的 `StatusOrBlockedBannerComponent` 仍保留 `exception-amber` 樣式，目前沒有頁面使用 |
| 5 | standalone 模式沒有 Bootstrap 樣式、空狀態插圖不會載入（shell 提供的資產，依 GOV-CSS-002／GOV-BND-003 不另外載入） | 開發／demo 體驗 | 待負責人決定 |
| 6 | 「無權限」區塊不是共用元件（UI-015、UI-017 各寫一份） | 前端重用 | 待負責人決定 |
| 7 | UI-015 的「評分分布」不在範圍內：API 沒有分數欄位 | 功能 | 需先裁決後端契約與遮罩規則 |
| 8 | 正式環境是否使用 Hibernate `validate` | 維運 | 負責人決定延後 |

## 六、待負責人決定

- 第五節第 1 項：shell 的事件順序（需要 shell 負責人提供）。
- 第五節第 3、5、6、7、8 項。
- Oracle 驗證是否加入 CI（目前決定暫不加入）。

## 七、建議的下一步

2026-11-18 晚間：第五節第 4～4c 項處理完畢後，實作暫告一段落。`main` 的四項必要檢查全綠，沒有進行中的建構工作；剩下的項目都是新功能，或要等裁決或外部資訊。

接下來先回到 harness 本身（`03-harness-plan.md`）：

1. 更新 `03-harness-plan.md` 的現況。第三節 P4、P6、P9 和第六節仍寫「尚未設為必要檢查」，但 2026-11-18 起已是必要檢查（第八節）。
2. 在 PR 上實際驗一次 `04-definition-of-enough.md` 的及格線四項：2026-11-18 已完成（#87、#88、#89，`03-harness-plan.md` 第六節）。
3. 「未立法的問題側先停下來」的機制：待負責人裁決（`03-harness-plan.md` 第六節）。

之後再回到專案工作，依序：

1. 專案與規則管理（UI-001～005）：補上流程的起點。先寫頁面規格，再實作前端。UI-005 需要先補版本清單 API（見第二節）。
2. 身分就緒（第五節第 1、2 項）：確認 shell 行為後開始。
3. 之後是 UI-008、UI-016、時間欄位的裁決，以及 standalone 模式的決定。

## 八、尚未驗證的事

- hosted（shell）模式從未實際執行：版面、`system-container`／`system-block` 的呈現、身分切換。
- 前端 E2E 都以 mock API 執行；standalone 模式固定為 `HOSPITAL_MANAGER`，其他角色只由單元測試涵蓋。
- V6、SERIALIZABLE 匯出、儀表板聚合查詢在 Oracle 上的行為。
- api-gateway 是否另外擋院區（2026-11-12 即列為未確認，這次沒有重新核對）。
- 2026-11-18 起，`check`、`law-check`、`law-selftest`、`frontend-check` 是 `main` 的必要檢查：ruleset `main*` 改為 Active，PR #77 驗證必要檢查失敗時，合併按鈕無法使用（`07-github-settings.md` 第三節）。runner 固定 `ubuntu-24.04`，因為 `ubuntu-latest`（26.04）不被 lockfile 的 Playwright 1.59.1 支援。
- 第二支實體金鑰（#74）尚未在 GitHub 上簽過修法。

## 九、2026-11-12 快照遺留的項目

以下逐條列出 2026-11-12 版第六到第九節的**全部**項目，保留當時的數字。標「已處理」或「仍成立」的附上這次的依據；其餘這次**沒有重新核對**，不代表仍成立或已解決。

### 原第六節：待人決定

| 項目（2026-11-12 原文摘要） | 這次的狀態 |
|---|---|
| 唯一的 law 放在 `/specs`；格式、與 `decision-log` 的關係、`docs/` 與 `openspec/` 的去留仍待定 | 部分處理：`specs/` 已有格式 v1（`10-spec-format.md`），含 `review-project`、`score-sheet`。與 `decision-log` 的關係、`openspec/` 的去留未重新核對 |
| 正式資料庫與 migration 工具 | 已處理：Oracle 與 Flyway（D-045、PR #55） |
| RP-PUB-01：對已發布版本再次發布的錯誤代碼 | 已處理：D-049 |
| 初審總分的量尺（範例品質線 80 與 D-046 的 1～10 不一致） | 未重新核對 |
| `REVIEWER`、`REREVIEWER` 能否看專案 | 已處理：D-051 |
| 綁定時是否要求評分表版本存在且已發布 | 未重新核對（相關裁決 D-041、D-050） |
| 參考 repo 的授權條款 | 未重新核對 |
| D-037 的原始決策日期 | 未重新核對 |

### 原第七節：程式與決策不符之處

| 項目（2026-11-12 原文摘要） | 這次的狀態 |
|---|---|
| D-039：程式把停用當終點；重複啟用仍發 event | 未重新核對 |
| D-041：程式綁定評分表 id，且到啟用時才檢查 | 未重新核對 |
| D-043：程式拒絕 `AUDITOR` 讀專案詳情 | 未重新核對 |
| D-046、D-047：品質線與權重沒有輸入管道 | 未重新核對 |
| D-031、D-032、D-034、DD-03 已接受但未實作；「固定」「結案」的 use case 與 API 仍是 `UNKNOWN` | 部分處理：複審結案（D-053）與初審固定（D-054）已定案並實作；D-032 的紅燈測試已轉綠。D-031、D-034、DD-03 未重新核對 |
| D-037、D-038 未實作（12 個紅燈測試） | 已處理：已知失敗清單已清空 |
| `GetRereviewRevisionHistoryUseCase` 沒有角色檢查 | 已處理：2026-11-18 以測試證實後，依 D-059 移除端點與 use case（#82），見第五節第 4a 項 |
| D-042 在報表與匯出的缺口（2026-11-14 已止血）；兩支服務仍在 `reporting`，依 D-048 之後要搬進 `application` 的查詢側 | 仍成立：`DashboardQueryService`、`ExportQueryService` 仍在 `reporting` 套件（ARCH-05 過渡規則：只能依賴 `application.common`）。api-gateway 是否另外擋，未重新核對 |

### 原第八節：尚未驗證的事

| 項目（2026-11-12 原文摘要） | 這次的狀態 |
|---|---|
| 抽選批次列表的院區過濾，沒有對真實資料庫跑過跨院區案例 | 未重新核對 |
| api-gateway 是否另外擋院區 | 未重新核對（同第八節） |
| 前端從未測試 | 已處理：見第三節 |
| 前端是否把 `NO_CHANGES` 當成已是最新（D-044） | 未重新核對 |
| `design-implementation-gap-2026-11-11.md` 未經人工複核 | 未重新核對 |
| 宣告 Java 11、實際用 17；Spring Boot 2.7 已停止維護 | 未重新核對 |

### 原第九節：已知的結構問題（數字是 gap 報告當時的統計）

| 項目（2026-11-12 原文摘要） | 這次的狀態 |
|---|---|
| 沒有 CI（`.github/` 底下沒有 workflows）、沒有 lint | CI 已處理（第三節）；lint 仍未設定 |
| 沒有 migration、沒有樂觀鎖 | 已處理：Flyway V1～V6、D-045 樂觀鎖 |
| 索引為 0 | 部分處理：V5 新增 2 個、V6 新增 1 個索引。其餘查詢是否需要索引未重新核對 |
| 測試只跑 H2 | 仍成立：CI 只跑 H2；Oracle 只在 PR #55 手動驗證過 V1～V5 |
| 3 處分層違規；12 個檔案直接取系統時間或亂數 id；15 處 `IllegalArgumentException` | 仍在 ArchUnit 凍結基線內（只擋新增），數字未重新統計。其中 domain 取系統時間的部分見第五節第 3 項 |
| 68 個 `EX-` 範例與 97 個 `TS-` 情境在測試中的引用數是 0 | 未重新核對（之後有驗收範例的執行器，見 `11-example-runner.md`） |
| 15 支 use case、28 個錯誤代碼只存在程式裡，文件沒有 | 未重新核對 |

## 十、harness 進度（2026-11-13～14 的建置紀錄）

本節保留 2026-11-13～14 建立閘門時的紀錄，其他文件以「第十節」引用。之後閘門的現況見第三節。


2026-11-13：P1 的閘門已建立，路徑都在 `services/quality-inspection-service/` 底下。同日接著做了 P6 的檔案部分與 P2，見本節末尾「P6 與 P2」。下一步是在 GitHub 上設定 main 的保護規則，然後是 P10。

| 機制 | 檔案 | 擋什麼 |
|---|---|---|
| 已知失敗清單 | `known-test-failures.txt`、`build.gradle` 的 `test` 任務 | 實際失敗的測試與清單不同就失敗。清單目前 11 個方法、14 次失敗（D-037、D-038、D-032 未實作） |
| ArchUnit 規則 | `src/test/java/.../architecture/ArchitectureRules.java` | ARCH-01 分層依賴方向；ARCH-02 `domain` 與 `application` 不得取系統時間或產生亂數 id；ARCH-03 同兩層不得丟 `IllegalArgumentException` |
| ArchUnit 凍結基線 | `src/test/resources/archunit_store/` | 既有違規 14、31、15 筆；只擋新增的。基線平常唯讀，更新用 `./gradlew test -PupdateArchBaseline` |
| CI | `.github/workflows/backend-check.yml` | 每個 PR 與 `main` 的 push 跑 `./gradlew check` |
| import 規則 ARCH-04 | `.../architecture/LayerImportRule.java`、`src/test/resources/layer-import-baseline.txt` | 與 ARCH-01 相同的依賴方向，改看原始碼的 import 敘述。既有違規 5 筆，實際違規必須與基線完全相同 |
| 違規樣本 | `.../architecture/samples/`、`ArchitectureRuleSamplesTest`、`LayerImportRuleTest` | 規則被弄壞（改成永遠通過） |
| 空執行檢查 | `build.gradle` 的 `verifyTestsExecuted` | 沒有測試原始碼時 `check` 仍然通過 |

本機驗證紀錄（2026-11-13，每項都實際弄壞一次再還原）：

- 清單：新增失敗、清單上的測試變綠、失敗次數改變、清單上的測試沒執行，四種都讓建置失敗。
- ArchUnit：在 `interfaces` 引用 `infrastructure` 的類別、在 `domain` 呼叫 `Instant.now()`、丟 `IllegalArgumentException`，三條規則各自失敗；`Instant.now(clock)` 不算違規。
- 基線：沒有基線時不會自動建立；基線裡留著已修掉的違規時失敗。

擋不到的：

- 清單上的測試換了失敗原因。
- 只引用另一層的編譯期常數，而且不寫 import、直接用完整類別名稱。有 import 的情況已由 ARCH-04 擋下（`ActorContextArgumentResolver` 引用 `ApiKeyAuthFilter` 已在 ARCH-04 的基線裡）。常數 `ACTOR_CONTEXT_ATTR` 較合理的落點是 `application.common`。
- ARCH-02、ARCH-03 以外的寫法：`new Date()`、`Clock.systemUTC()`、`IllegalArgumentException` 的子類別、JDK 內部丟出的 `IllegalArgumentException`、反射。
- 清單、基線、規則本身被改。這要等 P6。
- 2026-11-18 起，CI 是必要檢查，紅燈不能合併（PR #77 驗證）。這也屬於 P6。

三條規則的依據是 `03-harness-plan.md` 第六節和 gap 報告 §3.7；ARCH-01 的依賴方向是依 backend `README.md` §6.1 的層責任推定的。`decision-log.md` 裡沒有對應的裁決。

CI 用 JDK 17，與本機實際執行的版本一致；`build.gradle` 宣告的是 Java 11。

### P6 與 P2（2026-11-13，接在 PR #6 之後）

以下修改疊在 PR #6 的分支上，尚未合併。

| 項目 | 內容 | 狀態 |
|---|---|---|
| P6：CODEOWNERS | `.github/CODEOWNERS` 涵蓋 `decision-log.md`、`/specs/`、`/spec-draft/`、後端的建置設定、已知失敗清單、架構規則、樣本、三份基線、workflows、CODEOWNERS 本身 | ⚠️ 只有檔案。main 的保護規則要在 GitHub 上手動設，步驟與驗收見 `07-github-settings.md` |
| P2：違規樣本 | 四條規則各有一組故意違規的類別和合規對照。規則改成以根套件為參數，閘門與樣本執行同一份定義 | 完成 |
| ARCH-04 | 補 ARCH-01 看不到編譯期常數的缺口 | 完成 |
| PR #6 的審查意見 | ARCH-02、ARCH-03 改看「存取」，方法參考（`UUID::randomUUID`、`IllegalArgumentException::new`）也算，基線不變；`verifyTestsExecuted` 擋沒有測試原始碼的執行 | 完成。四則留言都是自動審查工具留的，尚未回覆 |

本機驗證紀錄（JDK 17，每項弄壞一次再還原）：

| 操作 | 結果 |
|---|---|
| 不改任何東西跑 `./gradlew check` | 通過：820 個測試，14 個失敗，全部在清單上，0 個跳過 |
| 把 ARCH-01 到 ARCH-04 都改成永遠通過 | 失敗：四個樣本測試全部失敗；四個閘門測試也失敗（基線與現況不符） |
| 在 `application` 新增一個只引用 `infrastructure` 常數的類別 | 失敗：ARCH-04。ARCH-01 沒有報 |
| 移走 `src/test/java` 後跑 `check` | 失敗。Gradle 有兩種回報方式（NO-SOURCE；清掉舊輸出後回報已執行），兩種都驗證過。第一版只擋 NO-SOURCE，第二種漏掉，已修正 |
| 還原後連跑兩次 | 第二次 `test` 是 UP-TO-DATE，`check` 通過 |
| 在 `application` 的一個檔案裡，把違規的 import 接在既有 import 的同一行（位元組碼不變） | 失敗：ARCH-04。修正前有兩個缺口：`test` 沒把原始碼列為輸入，所以是 UP-TO-DATE 直接通過；列為輸入後，逐行比對又漏掉同一行的第二個 import。兩者都是 PR #7 的審查意見與重跑驗證找到的，已修正並補上跨行、同一行、註解三個樣本 |

沒有驗證的：

- 這些修改還沒有在 GitHub 的 CI 上跑過。workflow 只在 PR 和 `main` 的 push 觸發，這個分支沒有開 PR。
- CODEOWNERS 的語法沒有經 GitHub 驗證（要在 GitHub 的檔案頁面看有沒有錯誤提示）。
- 及格線（`04-definition-of-enough.md` 第一節）：第 3 項在本機通過；第 1 項在本機通過，還沒在 PR 上做；第 2 項依 2026-11-14 的裁決目前無法達成（code owner 只保留形式）；第 4 項要等驗收範例的執行器。

待決定：

| 題目 | 現況 |
|---|---|
| ARCH-01 的依賴方向 | 2026-11-14 已裁決（D-048）：採 DDD 加 CQRS，查詢經過 `application` 的查詢埠。過渡期 `reporting` 只能依賴 `application.common`（新增 ARCH-05，含樣本，植入違規後失敗）；`infrastructure` 不得依賴 `reporting` |
| 單人 repo 的 code owner 核准 | 2026-11-14 已裁決：只保留形式，不啟用強制核准。見 `07-github-settings.md` 第二節 |
| 修法機制 | 2026-11-14 啟用：修法要有修法紀錄，加上負責人用實體金鑰對內容雜湊的簽章，`law-check` 才會通過。完整流程已在 GitHub 上驗收（PR #15）。核心在 `harness-core/`（0.2.0，50 個測試）。2026-11-18 起是必要檢查。核准者 tsengeagle 登記了兩支實體金鑰上的三把金鑰（第二支 YubiKey 5 Nano 於 2026-11-18 登記，PR #74）。紀錄見 `08-legislation-design.md` 第十一節，分期見 `03-harness-plan.md` 第六節 |
| 閘門訊息的編碼 | `build.gradle` 的中文訊息在非 UTF-8 的終端機顯示成問號（本次的雲端環境就是）。GitHub 的 runner 是否正常，未確認 |
