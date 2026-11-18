# 專案現況（2026-11-18）

這份文件是時間點快照。之後狀況改變時，更新本檔並修改標題日期。

基準：`main` @ `6f1303f`（PR #71 合併後）。上一版快照是 2026-11-12（部分補到 11-14），停在 PR #15；本版涵蓋 PR #16 到 #71。上一版的內容可從 git 歷史取得；第十節的閘門驗證紀錄是證據，原文保留在附錄 A。

## 一、版本控制

- PR #16 到 #71 都以 merge commit 合併進 `main`，沒有缺號。所有工作都在 `main` 上。
- 2026-11-18 查到 repo 的可見度是 **public**。D-052（2026-11-15）與 `07-github-settings.md` 的例外紀錄寫的是「免費私有 repo 的必要檢查不會強制執行」；改為 public 的時間點，以及之後 main 的保護規則是否已設定、是否生效，**都未確認**（本次的工具讀不到分支保護設定）。

PR 分組：

| 群組 | PR | 內容 | 決策 |
|---|---|---|---|
| harness 與律法 | #16–#18、#20、#22、#33、#37 | 文件校正；Windows 支援與 UTF-8；`.gitattributes`；以審查時的 commit 重算修法內容；驗收執行器登記為閘門；備援金鑰（破例）；測試套件本身出錯也讓閘門失敗 | D-052 |
| 驗收範例執行器 | #23 | `specs/` 的範例當成驗收測試執行 | — |
| ReviewProject | #19、#21、#24–#27、#29–#32、#34 | 升格到 `specs/`；品質線與權重的輸入；發布檢查；`AUDITOR` 可讀；`VALIDATION_ERROR`；清單讀取範圍；`REVIEWER`/`REREVIEWER` 拒讀 | D-043、D-046、D-047、D-049、D-051 |
| ScoreSheet | #28、#40–#43、#52 | 規格草稿到升格；複合版本 id；綁定、發布、審查改用版本；`scoreSheetVersionRef` 改名 | D-041、D-045、D-050 |
| 草稿無變更 | #35 | 相同內容重送回 409 `NO_CHANGES` | D-038、D-044 |
| 複審結案 | #38、#39 | 結案命令；提交不再結案；前端結案按鈕 | D-032、D-034、D-053 |
| 盲審 | #44、#45 | 生命週期與稽核軌跡對複審醫師遮罩；複審頁不保留初審分數 | D-033 |
| 資料庫 | #47–#51 | Oracle 19c 加 Flyway；V1 baseline；測試改 H2 Oracle 相容模式；案件樂觀鎖與持久化的 `updatedAt` | D-045、D-037 |
| 初審固定 | #47、#53、#56、#57 | 固定命令；併發測試（TS-DD03-002）；前端固定按鈕 | D-054、D-031、DD-03 |
| 複審指派歷史 | #54、#55 | 指派歷史、初審讀取者紀錄、匯出遮罩 | D-055 |
| 錯誤代碼 | #59 | 找不到資源時回契約中的代碼 | — |
| 前端初審、複審頁 | #58、#60、#62、#63 | 提交明細載入完成前鎖定表單；忽略過期的回應 | — |
| 報表與匯出 | #61、#64、#66、#67 | 匯出請求格式以程式為準；依建立時間過濾、區間必填、分頁；同一快照；儀表板改單一查詢 | D-056、D-057 |
| 前端報表頁 | #68–#71 | UI-017 匯出頁、UI-015 Dashboard 頁（頁面規格走修法） | — |
| 其他 | #36、#46、#65 | 前端路由測試；e2e 標題；e2e 可指定 Chromium | — |

## 二、測試

2026-11-18 在雲端 session 實際執行：

| 範圍 | 指令 | 結果 |
|---|---|---|
| 後端 | `./gradlew check`（JDK 17） | 通過。1225 個測試，0 失敗，0 錯誤，0 跳過。其中驗收範例 74 個（`AcceptanceExamplesTest`）、執行器自身的測試 26 個 |
| 前端型別檢查 | `npm run typecheck` | 通過 |
| 前端單元測試 | `npm run test:unit:ci` | 通過。224 個，0 失敗；原始碼沒有 `fit`、`fdescribe`、`xit`、`xdescribe` |
| 前端 e2e（預設） | `npx playwright test` | 18 個通過，3 個跳過。跳過的三個是 live 流程，要以 `QI_PLAYWRIGHT_FLOW` 開啟 |
| 前端 e2e（live） | `QI_PLAYWRIGHT_FLOW=preflight`、`=e2e`，後端以 `bootRun` 啟動 | 兩者都通過 |
| 前端 e2e（demo） | `QI_PLAYWRIGHT_FLOW=demo` | **沒有執行**。它是有畫面的展示模式 |

- `known-test-failures.txt` 已清空，只剩註解。上一版的 14 個失敗都已轉綠。
- GitHub 上 PR #71 合併後，`main` 的 `backend-check`、`law-check`、`law-selftest`、`spec-check` 四個 workflow 都通過。
- **前端沒有 CI。** 四個 workflow 都不跑前端，前端的測試只在有人手動執行時才會跑。

在雲端 session 執行時要注意的環境差異（只影響執行方式，沒有改 repo）：

- 預設 JDK 是 21，Gradle 7.6.4 會失敗（`Unsupported class file major version 65`）。要另外裝 JDK 17 並設定 `JAVA_HOME`。
- Karma 設定的瀏覽器是 `Chrome`。以 root 執行時要加 `--no-sandbox`，本次以 `CHROME_BIN` 指向一個加上該參數的包裝腳本。
- e2e 用 `QI_PLAYWRIGHT_CHROMIUM_PATH=/opt/pw-browsers/chromium`（PR #65）。

## 三、決策

上一版寫到 D-047。之後新增 D-048 到 D-057，詳細內容以 `decision-log.md` 為準：

| 決策 | 摘要 |
|---|---|
| D-048 | 後端採 DDD 加 CQRS；查詢經過 `application` 的查詢埠 |
| D-049 | 已發布版本再次發布的錯誤代碼；更新草稿時沒給的權重回到預設 |
| D-050 | 評分表的版本、建立、更新、發布與查詢 |
| D-051 | `REVIEWER`、`REREVIEWER` 不能讀專案與抽選批次 |
| D-052 | PR #33（登記備援金鑰）以人工審查取代簽章，破例合併，只限一次 |
| D-053 | 複審結案命令的 API、固定的表示方式與錯誤代碼 |
| D-054 | 初審固定的 API、記錄方式與錯誤代碼 |
| D-055 | 以複審指派歷史證明「不是歷史複審醫師」（補足 D-035） |
| D-056 | 匯出請求的格式以程式為準 |
| D-057 | 匯出依案件建立時間過濾、時間區間必填、逐案資料列分頁 |

D-045 已從「無法落地」改為已裁決：正式資料庫是 Oracle 19c 以上，migration 工具是 Flyway 8.5.13。

## 四、規格

- `specs/` 是唯一的 law，格式 v1（`specs/README.md`、`10-spec-format.md`），由 `spec-check` 機械檢查。
- 已轉換的 aggregate：`specs/review-project/`、`specs/score-sheet/`。其餘仍以 `docs/01-requirements` 到 `docs/04-testing` 為準。
- `spec-draft/` 已清空，只留 README 作為試作紀錄。
- 驗收範例由執行器直接執行（`11-example-runner.md`），74 個全部通過。
- `specs/`、`spec-draft/` 已列入律法清冊（`spec`）；執行器的目錄列為 `gate`。

## 五、程式與決策不符之處

上一版第七節列的項目，逐項核對：

| 項目 | 現況 | 證據 |
|---|---|---|
| D-039 停用後可再啟用 | 已實作 | `ReviewProject.activate()` 接受 `DEACTIVATED` |
| D-041 綁定的是評分表版本 | 已實作 | PR #27、#42 |
| D-043 `AUDITOR` 讀專案詳情 | 已實作 | PR #25 |
| D-046、D-047 品質線與權重的輸入管道 | 已實作 | PR #24 |
| D-031、D-032、D-034、DD-03 | 已實作 | PR #39（結案）、#53（固定） |
| D-037、D-038 | 已實作 | PR #35、#51；已知失敗清單已清空 |
| D-042 報表與匯出的院區範圍 | 已止血，之後匯出與儀表板又重做（PR #66、#67） | 服務仍在 `reporting` |

仍未處理：

- `GetRereviewRevisionHistoryUseCase` 沒有角色檢查，任何角色都可查（只有院區檢查）。程式註解仍是 `// Any authenticated role can query`。
- `ProjectStatus.java` 第 5 行與 `ReviewProject.java` 第 23 行的 Javadoc 仍寫「`DEACTIVATED` 是終點」，與程式及 D-039 不符。
- D-048：`DashboardQueryService`、`ExportQueryService` 仍在 `reporting`，搬進 `application` 查詢側的工作未動工。

本次沒有逐條核對 D-048 到 D-057 是否完全落地，只核對了上表的項目。

## 六、待人決定

| 題目 | 現況 |
|---|---|
| `docs/` 與 `openspec/` 的去留、各處的權威定義 | 未決。`openspec/` 只剩 `config.yaml`。`AGENTS.md`、`.ai/governance/authority-sources.md`、`docs/README.md`、`.github/copilot-instructions.md` 都沒有提到 `specs/`；`CLAUDE.md` 的權威順序也沒有 `specs/` |
| 初審總分的量尺 | 未決。D-046 的品質線範圍仍是暫定值；`api-spec.md` §7.1 與 `domain-events.md` 的範例品質線是 80 |
| repo 改為 public 之後的保護規則 | 新題目。見第一節 |
| local 與正式環境是否改用 `ddl-auto=validate` | D-045 第 4 點：需另行裁決，並應在 Oracle 19c 上確認 |
| 參考 repo 的授權條款 | 未變。複製腳本或文件前先確認 |
| D-037 的原始決策日期 | 未變 |

上一版列的「正式資料庫與 migration 工具」（D-045）、「RP-PUB-01 的錯誤代碼」（D-049）、「`REVIEWER`、`REREVIEWER` 能否看專案」（D-051）都已裁決。

## 七、尚未驗證的事

- main 的保護規則是否已設定、是否生效（第一節）。
- 前端 demo 模式的 e2e 沒有執行。
- api-gateway 是否另外擋院區，仍不清楚。
- D-044（前端把 `NO_CHANGES` 當成已是最新）：`cases.service.ts` 有實作，`cases.service.spec.ts` 有單元測試且通過。沒有在 e2e 對真實後端驗證過。
- Oracle：XE 21c 上的 `validate` 通過（D-045 第 4 點的更正）；19c 沒有實測。
- `build.gradle` 宣告 Java 11，CI 與本機都用 17 執行。Spring Boot 2.7 已停止維護。
- `design-implementation-gap-2026-11-11.md` 仍未經人工複核。
- 抽選批次列表的院區過濾，對真實資料庫跑跨院區案例的部分，本次沒有重新確認。

## 八、已知的結構問題

| 項目 | 上一版 | 2026-11-18 |
|---|---|---|
| CI | 沒有 | 4 個 workflow：`backend-check`、`law-check`、`law-selftest`、`spec-check`。前端沒有 |
| lint | 沒有 | 仍沒有 |
| migration | 沒有 | Flyway V1 到 V6 |
| 樂觀鎖 | 沒有 | 只有案件（`ReviewCaseEntity` 的 `@Version`） |
| 索引 | 0 | 2 個 migration 檔含 `CREATE INDEX`，數量未細算 |
| 測試資料庫 | H2 | H2 的 Oracle 相容模式，schema 由 Flyway 建立 |
| ARCH-01 基線 | 14 | 基線檔 3 行 |
| ARCH-02 基線 | 31 | 基線檔 29 行 |
| ARCH-03 基線 | 15 | 基線檔 7 行 |
| ARCH-04 基線 | 5 | 4 行 |

基線數字是檔案的行數，與違規筆數大致相當，沒有逐筆核對。`EX-` 範例與 `TS-` 情境在測試中的引用數、只存在程式裡的 use case 與錯誤代碼數，本次沒有重新統計。

## 九、harness 進度

各期的狀態見 `03-harness-plan.md` 第六節，義務對照見第三節。重點：

- 修法機制：`harness-core` 0.3.0。`law/amendments/` 有 30 筆修法紀錄（2026-11-14 有 8 筆）。D-052 是唯一一次破例，之後的修法都有簽章。
- 金鑰：已登記備援金鑰，但和原本的金鑰在同一支實體金鑰上，防換電腦、不防金鑰遺失。第二支實體金鑰仍未登記。
- 驗收範例的執行器已上線（第 4 期），74 個範例全部通過。
- ARCH-05（D-048 過渡期 `reporting` 的依賴限制）已上線。
- 閘門訊息的編碼：`build.gradle` 的中文訊息在非 UTF-8 的終端機顯示成問號。PR #17 處理了修法工具的 UTF-8 輸出；`build.gradle` 的訊息在 GitHub runner 上是否正常，未確認。

## 附錄 A：2026-11-13 閘門建立時的驗證紀錄（原第十節，原文保留）

以下是上一版第十節的原文。其中的「下一步」與「待決定」是當時的狀態，現況以本文件上方各節與 `03-harness-plan.md` 為準。

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
- CI 還不是必要檢查，紅燈仍可合併。這也屬於 P6。

三條規則的依據是 `03-harness-plan.md` 第六節和 gap 報告 §3.7；ARCH-01 的依賴方向是依 backend `README.md` §6.1 的層責任推定的。`decision-log.md` 裡沒有對應的裁決。

CI 用 JDK 17，與本機實際執行的版本一致；`build.gradle` 宣告的是 Java 11。

#### P6 與 P2（2026-11-13，接在 PR #6 之後）

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
| 修法機制 | 2026-11-14 啟用：修法要有修法紀錄，加上負責人用實體金鑰對內容雜湊的簽章，`law-check` 才會通過。完整流程已在 GitHub 上驗收（PR #15）。核心在 `harness-core/`（0.2.0，50 個測試）。尚未設為必要檢查；只有一把金鑰。紀錄見 `08-legislation-design.md` 第十一節，分期見 `03-harness-plan.md` 第六節 |
| 閘門訊息的編碼 | `build.gradle` 的中文訊息在非 UTF-8 的終端機顯示成問號（本次的雲端環境就是）。GitHub 的 runner 是否正常，未確認 |
