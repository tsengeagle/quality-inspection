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

- 沒有 lint。CI 於 2026-11-13 建立（`.github/workflows/backend-check.yml`），只涵蓋後端。
- 沒有 migration、沒有樂觀鎖、索引為 0，測試只跑 H2。
- 3 處分層違規；12 個檔案直接取系統時間或亂數 id；15 處 `IllegalArgumentException`。
- 68 個 `EX-` 範例與 97 個 `TS-` 情境，在測試中的引用數是 0。
- 15 支 use case、28 個錯誤代碼只存在程式裡，文件沒有。

## 十、harness 進度

2026-11-13 更新。對應 `03-harness-plan.md` 第六節的順序。

| 步驟 | 狀態 | 證據或缺口 |
|---|---|---|
| 1. P1：CI 加 ArchUnit | 閘門已建，本機驗證過 | `./gradlew check` 嚴格模式：819 個測試，805 通過，14 個已知失敗，0 個非預期。植入一個分層違規後失敗。CI 的執行結果與「設為必要檢查」見下方 |
| 2. P6：CODEOWNERS 加 branch protection | 只有檔案 | `.github/CODEOWNERS` 已建；branch protection 要在 GitHub 上手動設，見 `07-github-settings.md` |
| 3. P2：違規樣本 | 完成 | 三條規則各有樣本；把規則改成永遠通過後，樣本測試失敗 |
| 4. P10：驗收範例可執行 | 未動工 | |
| 5 到 7 | 未動工 | |

本次連帶發現並處理的事：

- **Gradle wrapper 的 jar 從未進 repo。** `.gitignore` 的例外規則寫成 `!gradle/wrapper/gradle-wrapper.jar`，只對 repo 根目錄有效，對 `services/quality-inspection-service/gradle/wrapper/` 無效。乾淨的 clone 無法執行 `./gradlew`，CI 也不能。已修正 `.gitignore` 並補上 7.6.4 的 wrapper jar。
- **14 個已知失敗的處理方式。** 直接跑 `./gradlew check` 會永遠紅燈，閘門就沒有用。改成凍結基線：`harness/known-test-failures.txt` 列出這 14 筆和對應的決策；實際失敗的集合必須和清單完全相同，多一筆或少一筆都失敗。測試沒有被停用或跳過。這是本次自行採用的做法，待確認。
- **分層違規重新統計是 5 組類別對**，不是 gap 報告的 3 處。多出來的兩組：`infrastructure.security.ApiKeyAuthFilter` → `interfaces.rest.dto.ApiResponse`、`reporting.ExportQueryService` → `interfaces.rest.BadRequestException`。直接取時間或亂數 id 的 12 個檔案、15 處 `IllegalArgumentException` 與報告一致。

本次自行採用、待確認的設計：

| 項目 | 採用的做法 | 影響 |
|---|---|---|
| 分層依賴的方向矩陣 | 見 `services/quality-inspection-service/harness/README.md` | 既有文件只寫了各層責任，沒有寫誰可以依賴誰。`reporting` → `application` 目前算允許 |
| CI 的 JDK | 17 | `build.gradle` 宣告 Java 11，文件記載本機實際用 17 |
| `.claude/settings.json` 的 `permissions.deny` | 沒做 | 擋掉對 `decision-log.md` 的編輯，會連「依指示寫入裁決」也擋掉。要不要改用 `ask`，待決定 |

及格線四項（`04-definition-of-enough.md` 第一節）的現況：第 3 項在本機通過；第 1 項在本機通過，尚未在 PR 上驗證；第 2 項要等 branch protection；第 4 項要等驗收範例的執行器。
