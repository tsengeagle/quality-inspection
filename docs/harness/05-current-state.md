# 專案現況（2026-11-12）

這份文件是時間點快照。之後狀況改變時，更新本檔並修改標題日期。

## 一、版本控制

2026-11-13 更新（本節以外仍是 2026-11-12 的快照）：

- `fix/imp01-submission-integrity` 的 12 個 commit 已由 PR #5 以 rebase 方式併入 `origin/main`（2026-11-12）。內容相同，commit 編號不同。
- 本目錄的文件在 `main` 上，接在 PR #5 之後。
- 2026-11-12 推送之前，`origin/main` 停在 2026-04-21，七個月的工作只存在本機。

## 二、測試

`./gradlew test`（JDK 17；`build.gradle` 宣告 Java 11）：**812 個測試，14 個失敗，全部是已知原因**。

| 失敗 | 數量 | 原因 |
|---|---:|---|
| `UpdatedAtContractIntegrationTest` | 12 | D-037、D-038 尚未實作，屬於先寫的測試 |
| `SubmitRereviewUseCaseTest`、`RereviewApiTest` | 2 | 測試要求複審提交不結案（D-032），程式仍直接結案 |

前端沒有跑過測試。

本次新增的測試基礎設定：

- 測試專用 profile（`application-test.yml`）：每個測試環境有自己的空 H2 資料庫，不載入示範資料。原因是測試原本跑在預設的 `local` profile，示範資料的 seed 造成 3 個筆數不符的失敗。
- `ReviewCaseQueryApiTest` 原本暗中依賴示範用的醫師名單，改成自己準備資料。

## 三、本次寫入的決策

詳細內容以 `docs/01-requirements/decision-log.md` 為準，此處只列摘要。

| 決策 | 摘要 |
|---|---|
| D-037、D-038 | `updatedAt` 契約；相同內容重送回 409 `NO_CHANGES`。經本人確認 |
| D-039 | 專案三個狀態；停用後可再啟用；草稿不能停用；重複操作回成功但不發 event |
| D-040 | 院區取自登入者；名稱可重複；病歷類型至少一個；已發布後更新回 409；版本號規則 |
| D-041 | 綁定的是評分表版本，發布前必須已綁定 |
| D-042 | 院區一律取自登入者，適用範圍擴及專案與所有查詢、匯出 |
| D-043 | `AUDITOR` 可讀專案詳情，回傳 11 個欄位 |
| D-044 | 無變更的比較方式；前端把 `NO_CHANGES` 當成已是最新 |
| D-045 | 併發改採樂觀鎖。因資料庫與 migration 工具未定，標為無法落地 |
| D-046 | 品質線在草稿可為空，發布時必填，整數 1 到 10（暫定）；D-031 的 null 分支保留 |
| D-047 | `SYSTEM_ADMIN` 讀取一律跨院區；權重和品質線走同一個輸入管道，數值規則維持 D-020 |

## 四、spec-draft

- `spec-draft/review-project/`：52 條 `decided`、1 條 `proposed`（RP-PUB-01 的錯誤代碼）。
- 只涵蓋 ReviewProject 一個 aggregate 的 6 支 use case。
- 還沒有執行器，範例不能跑。

## 五、本次修改的程式

九支 use case 補上院區範圍檢查（D-042、D-047）：

| Use case | 修法 |
|---|---|
| 改派複審醫師、抽選預覽、匯出抽選結果 | 載入後檢查登入者院區 |
| 抽選批次詳情、案件生命週期、複審修訂歷程 | 同上，`SYSTEM_ADMIN` 例外 |
| 案件列表、稽核軌跡列表、抽選批次列表 | 院區一律取登入者的；`SYSTEM_ADMIN` 可用參數縮小範圍 |

連帶修改：

- `ActorContext` 新增 `requireReadScope`、`readScopeHospitalId`。
- `ReviewReadAccessPolicy` 原本連 `SYSTEM_ADMIN` 也檢查院區，已依 D-047 放寬。這是盲審防護的程式，值得人工複看。
- 抽選批次沒有自己的院區欄位，改成透過所屬專案的院區過濾。

## 六、待人決定

| 題目 | 現有建議 |
|---|---|
| 唯一的 law 已裁決放在 `/specs`；格式、與 `decision-log` 的關係、`docs/` 與 `openspec/` 的去留仍待定 | 見 `03-harness-plan.md` 第二節 |
| 正式資料庫與 migration 工具 | 無依據可建議；D-045 與「測試跑真實資料庫」都卡在這裡 |
| RP-PUB-01：對已發布版本再次發布的錯誤代碼 | 409，與現行測試一致 |
| 初審總分的量尺 | 文件範例的品質線是 80，與 D-046 的 1 到 10 不一致，需確認總分是幾分制 |
| `REVIEWER`、`REREVIEWER` 能否看專案 | 先不開放 |
| 綁定時是否要求評分表版本存在且已發布 | 要求 |
| 參考 repo 的授權條款 | 複製腳本或文件前先確認 |
| D-037 的原始決策日期 | 不明，目前只加註「2026-11-12 再次確認」 |

## 七、程式與決策不符之處

- D-039：程式把停用當終點；重複啟用仍發 event。
- D-041：程式綁定的是評分表 id，且到啟用時才檢查。
- D-043：程式拒絕 `AUDITOR` 讀專案詳情。
- D-046、D-047：品質線與權重沒有任何輸入管道，正常操作建出的專案品質線是 null。
- D-031、D-032、D-034、DD-03：已接受但尚未實作。「固定」「結案」的 use case 名稱與 API 在文件裡仍是 `UNKNOWN`，要先補設計。
- D-037、D-038：尚未實作（對應 12 個紅燈測試）。
- `GetRereviewRevisionHistoryUseCase` 沒有角色檢查，任何角色都可查。本次只補了院區。

## 八、尚未驗證的事

- 抽選批次列表的院區過濾，只有單元測試驗到參數傳遞，沒有對真實資料庫跑過跨院區的案例。
- api-gateway 是否另外擋院區，不清楚。
- 前端從未測試。前端是否把 `NO_CHANGES` 當成已是最新（D-044），未確認。
- `design-implementation-gap-2026-11-11.md` 未經人工複核。集合比對的部分可信度較高；「決策是否落地」只確認了存在，沒驗證行為。
- 本機的建置環境與宣告不一致：宣告 Java 11，實際用 17 跑。Spring Boot 2.7 已停止維護。

## 九、已知的結構問題

取自 gap 報告，數字是報告當時的統計。

- 沒有 CI（`.github/` 底下沒有 workflows）、沒有 lint。
- 沒有 migration、沒有樂觀鎖、索引為 0，測試只跑 H2。
- 3 處分層違規；12 個檔案直接取系統時間或亂數 id；15 處 `IllegalArgumentException`。
- 68 個 `EX-` 範例與 97 個 `TS-` 情境，在測試中的引用數是 0。
- 15 支 use case、28 個錯誤代碼只存在程式裡，文件沒有。

## 十、harness 進度

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
| 把 ARCH-01 到 ARCH-04 都改成永遠通過 | 失敗：五個樣本測試全部失敗；四個閘門測試也失敗（基線與現況不符） |
| 在 `application` 新增一個只引用 `infrastructure` 常數的類別 | 失敗：ARCH-04。ARCH-01 沒有報 |
| 移走 `src/test/java` 後跑 `check` | 失敗。Gradle 有兩種回報方式（NO-SOURCE；清掉舊輸出後回報已執行），兩種都驗證過。第一版只擋 NO-SOURCE，第二種漏掉，已修正 |
| 還原後連跑兩次 | 第二次 `test` 是 UP-TO-DATE，`check` 通過 |

沒有驗證的：

- 這些修改還沒有在 GitHub 的 CI 上跑過。workflow 只在 PR 和 `main` 的 push 觸發，這個分支沒有開 PR。
- CODEOWNERS 的語法沒有經 GitHub 驗證（要在 GitHub 的檔案頁面看有沒有錯誤提示）。
- 及格線（`04-definition-of-enough.md` 第一節）：第 3 項在本機通過；第 1 項在本機通過，還沒在 PR 上做；第 2 項要等保護規則；第 4 項要等驗收範例的執行器。

待決定：

| 題目 | 現況 |
|---|---|
| ARCH-01 的依賴方向 | 仍是依層責任推定的，`decision-log.md` 沒有對應的裁決。有爭議的兩點：`reporting` 依賴 `application`（允許）、`infrastructure` 依賴 `reporting`（禁止） |
| 單人 repo 的 code owner 核准 | GitHub 不允許作者核准自己的 PR。見 `07-github-settings.md` 第二節 |
| `.claude/settings.json` 的 `permissions.deny` | 沒做。直接 deny 會連「依指示把裁決寫進 `decision-log.md`」也擋掉 |
| 閘門訊息的編碼 | `build.gradle` 的中文訊息在非 UTF-8 的終端機顯示成問號（本次的雲端環境就是）。GitHub 的 runner 是否正常，未確認 |
