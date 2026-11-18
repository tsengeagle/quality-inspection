# 工作交接：2026-11-18 契約漂移收斂、及格線驗證與 D-060 涵蓋清單閘門

> 交接負責人：Claude Code（本 session）。本文件只整理現況，沒有修改程式。
> 事實的權威來源依序是：`docs/01-requirements/decision-log.md`、`docs/harness/05-current-state.md`（快照）、GitHub PR 與 CI 紀錄。本文件與它們衝突時，以它們為準。

> **2026-11-18 18:20 補註（交接後的變化）**：另一個 session 的 #93（`d4a3023`）已處理本文件列的第一個下一步。負責人裁決 HTTP 契約採**選項 B**，比對器用 **B-1（觀察）**，放在 `acceptance/`；以 score-sheet 在 `spec-draft/` 試做，只列出、不擋。D-061（Q-3、Q-4 與升格條件）待試做結果後起草。現況與待決事項以 `docs/harness/13-http-contract-options.md` 為準，本文件下方的「下一步」第 1 項已過時。

---

## A. Session Overview

| 項目 | 內容 |
|---|---|
| 專案 | `tsengeagle/quality-inspection`（醫療 HIS 品質審查系統；後端 Java/Spring Boot，前端 Angular 14 MFE） |
| 工作日期 | 2026-11-18（含之前被摘要的同日前段對話） |
| 主要目標 | (1) 前段：UI-015、UI-017 規格與前端、前端 CI、api-spec 路徑核對與收斂。(2) 後段：清掉契約漂移缺陷、讓實作告一段落，並回頭補 harness：在 PR 上驗證及格線、建立「未立法的問題側先停下來」的閘門（D-060） |
| 工作範圍 | 後端程式與測試、前端程式與測試、`docs/`、`law/`、`harness-core/`、`AGENTS.md`、`CLAUDE.md` |
| 相關裁決 | D-032、D-050、D-058、D-059、D-060（`decision-log.md`） |
| 整體狀態 | **DONE**（本 session 規劃的工作已全部合併）；harness 的下一階段在等負責人決策，見第 G、H 節 |

---

## B. Repository Baseline（2026-11-18 交接時唯讀檢查）

| 項目 | 值 | 檢查方式 |
|---|---|---|
| Repository | `tsengeagle/quality-inspection` | — |
| 本機 checkout | `HEAD` detached at `origin/main` | `git rev-parse --abbrev-ref HEAD` → `HEAD` |
| HEAD commit | `f4bade1a30e349a4e62c997df91901b91e8eb83d`（Merge PR #91） | `git log -1`；與 `origin/main` 相同 |
| Working tree | 交接前乾淨。本文件 `docs/handoffs/2026-11-18-harness-coverage-gate.md` 是新增的**未追蹤**檔案，沒有 commit | `git status --short` |
| 開啟中的 PR | 0 | GitHub `list_pull_requests state=open` |
| 排程中的提醒 | 0 | `list_triggers enabled=true` |
| 殘留的遠端分支 | `claude/verify-gate-1-layer-violation`、`claude/verify-gate-2-unsigned-amendment`、`claude/verify-gate-4-new-requirement`（驗證用，PR #87～#89 已關閉未合併）；`claude/coverage-gate`（#91 已合併） | `git ls-remote --heads` |
| 本 session 指定的開發分支 | `claude/peaceful-heisenberg-hes856`：遠端存在，本 session 沒有使用。實際工作都在各主題分支，經 PR 合併 | `git ls-remote` |

與本次工作相關的重要檔案：

| 檔案 | 用途 |
|---|---|
| `law/coverage.txt` | D-060 問題側涵蓋清單（新增） |
| `law/manifest.txt` | 律法清冊，新增 `@coverage` |
| `harness-core/law_check.py`（0.4.0） | 判決程式，新增涵蓋判決 |
| `harness-core/tests/test_law_check.py` | 新增 `CoverageTest`（13 個） |
| `docs/harness/03-harness-plan.md` | harness 計畫；第六節有「及格線的驗證紀錄」與 D-060 |
| `docs/harness/05-current-state.md` | 專案現況快照（2026-11-18 晚間版） |
| `docs/harness/12-escaped-defects.md` | 漏網紀錄 E-01～E-03（新增） |
| `docs/harness/07-github-settings.md` | 必要檢查的驗收紀錄 |
| `specs/README.md` | 規格格式 v1（第 G 節的格式缺口） |
| `AGENTS.md` §5.0、`CLAUDE.md` Key rules | AI 動到問題側前要先查涵蓋清單 |

---

## C. Work Completed

> 狀態定義：**VERIFIED**＝有可核對的 CI 或本機執行紀錄；**IMPLEMENTED_NOT_VERIFIED**＝已實作但沒有驗證；**REPORTED_ONLY**＝只有對話中的敘述。
> 前段（被摘要的部分）的細節以 PR 描述與 `05-current-state.md` 為準，這裡只列結果。

### C-1 前段（同日稍早，已被摘要）

| PR | 內容 | 狀態 | 證據 |
|---|---|---|---|
| #68、#70 | UI-017、UI-015 頁面規格（修法，已簽章） | VERIFIED | PR 已合併；`law/amendments/2026-11-17-*` |
| #69、#71 | UI-017 報表匯出、UI-015 儀表板前端 | VERIFIED | PR CI；`05-current-state.md` 第二節 |
| #72 | `frontend-check` 加入 CI（修法） | VERIFIED | main run 44293068034 |
| #73 | 現況快照（早上版） | VERIFIED | 已合併 |
| #76 | api-spec 路徑三方核對報告 | VERIFIED | `docs/review/api-spec-path-reconciliation-2026-11-18.md` |
| #80 | D-058：路徑以程式為準（修法） | VERIFIED | 已合併、已簽章 |
| #81 | D-059：移除 `rereview-revisions`（修法） | VERIFIED | 已合併、已簽章 |

### C-2 後段（本次交接的重點）

| # | 工作項目 | 實際完成內容 | PR / Commit | 驗證方法 | 結果 | 狀態 |
|---|---|---|---|---|---|---|
| 1 | 移除 `rereview-revisions`（E-01） | 刪除端點、use case、query；新增 4 個 D-059 權限測試；先紅後綠 | #82 → `b8cd894` | 本機 `gradlew cleanTest check`；mutation（SYSTEM_ADMIN 改成可讀，D-059 測試失敗）；CI 四項必要檢查 | 全綠 | VERIFIED |
| 2 | 移除 `hasExceptionEntry`（E-02，D-032） | 後端刪除欄位；前端刪除例外橫幅、C22 元件、`navigateToRereview` | #83 → `3a42cde` | 先紅：後端 API `found: true`、前端 unit 2 個失敗、e2e 1 個失敗；再綠：後端 1223 次執行 0 失敗、unit 221、e2e 18 通過 3 skip；CI 全綠 | 全綠 | VERIFIED |
| 3 | 文件同步：例外入口已撤銷 | 16 份前端、UI、設計文件加 D-032 註記或更新現況 | #84 → `31555aa` | `law_check`（建構 PR）、`spec_lint`、逐檔 grep 殘留 | 通過 | VERIFIED |
| 4 | 評分表回應格式（E-03，D-050 第 7 點） | `GET /score-sheets` 改為 `{items}`，`/sections` 改為 `{scoreSheetId, sections}`；新增兩個 DTO；刪除 api-spec 的「程式現況」註記 | #85 → `2feb5bf` | 先紅：`ScoreSheetApiTest` 5 個失敗；再綠：19/19、全套 0 失敗；CI 全綠 | 全綠 | VERIFIED |
| 5 | 現況快照（晚間版） | `05-current-state.md` 第一、三、五、七、九節 | #86 → `3fa719e` | CI 全綠 | 通過。內含一個錯誤，見 C-3 | VERIFIED（文件） |
| 6 | 及格線第 1 項：手寫分層違規 | 驗證用 PR，關閉、未合併 | #87 | CI run 44345483626：`check` 失敗（ARCH-01、ARCH-04 不在清單上）；`mergeable_state: blocked` | 符合預期 | VERIFIED |
| 7 | 及格線第 2 項：改律法沒有簽章 | 驗證用 PR，關閉、未合併 | #88 | CI run 44345432268：`law-check` 只有簽章一項未通過；`blocked` | 符合預期 | VERIFIED |
| 8 | 及格線第 4 項：改需求不改程式 | 驗證用 PR，關閉、未合併 | #89 | CI run 44345434438：新範例不在已知失敗清單上；`blocked` | 符合預期 | VERIFIED |
| 9 | 計畫文件、漏網紀錄 | `03-harness-plan` 現況、驗證紀錄、D-060 提案；新增 `12-escaped-defects.md`；`07` 驗收紀錄；更正 #86 的測試數 | #90 → `81bd267` | CI 全綠；Copilot 的意見（「四項都已驗證」說法過度）已修正（`e3bc121`） | 通過 | VERIFIED（文件） |
| 10 | **D-060 涵蓋清單閘門**（修法，負責人已簽章） | `law/coverage.txt`（問題側 256 個檔案：已立法 50、暫准 206）；`@coverage`；`law_check.py` 0.4.0；`AGENTS.md`、`CLAUDE.md` 的指示 | #91 → `f4bade1` | 先紅：`CoverageTest` 13 個中 10 個失敗；再綠：harness-core 112 個測試通過；3 種 mutation 都被抓到；本機以暫時分支模擬生效後 4 種情況（暫准提醒、已立法不提醒、新目錄擋、新 controller 擋）；CI 全綠；合併後本機 `law_check --base HEAD~1 --head HEAD`：簽章有效、通過 | 全綠 | VERIFIED |

### C-3 已知的紀錄錯誤（已更正）

- #86 把後端測試方法數寫成「930，口徑不同」。這是錯的：CI 的建置閘門會回報方法數，`main` 是 **957**（#87 的 CI 紀錄）。已在 #90 更正。

---

## D. Work Incomplete

| # | 未完成事項 | 目前進度 | 停止位置 | 原因 | 已嘗試 | 下一個具體動作 |
|---|---|---|---|---|---|---|
| D-1 | **HTTP 契約如何立法**（D-060 與規格格式 v1 規則 6 互相矛盾） | 已在對話中提出問題，沒有起草裁決 | 最後一則回覆，負責人尚未回答 | 需要負責人決策 | — | 起草選項比較，請負責人裁決（第 H 節第 1 項） |
| D-2 | ReviewCase 立法（薄切片：分派、初審提交、revision-history 讀取遮罩） | 只有建議 | 未開始 | 等 D-1 與格式缺口的決定 | — | D-1 決定後，在 `spec-draft/` 起草切片 |
| D-3 | 格式缺口：範例無法固定時鐘、`emits` 不驗證、沒有範例涵蓋的規則只列出不擋、規則 8 只靠人工檢查 | 已盤點（第 G 節） | 未處理 | 等格式 v2 的裁決 | — | 隨 D-2 切片一併提出 |
| D-4 | 「測試列為律法」的裁決 | 前提已部分成立（2026-11-14 裁決） | 未裁決 | 需要負責人決策 | — | 等格式穩定後提出 |
| D-5 | E-01 偵測器：每個讀取端點都要有權限測試 | 列在 `12-escaped-defects.md` | 未排期 | 尚未裁決 | — | 提案 |
| D-6 | E-03 介面層閘門（回應格式納入範例，或 api-spec 與 controller 的比對） | 部分回應：D-060 會提醒 | 未排期 | 依賴 D-1 | #76 的手動比對腳本（在已失效的 scratchpad） | 依 D-1 的結果決定 |
| D-7 | 及格線第 3 項（弄壞偵測器）沒有在 PR 上驗證 | 只有 2026-11-13 的本機紀錄 | — | 本 session 判斷為低優先 | — | 選擇性：開驗證 PR 把一條 ArchUnit 規則改成永遠通過 |
| D-8 | 專案層的待辦（實作已暫停） | 見 `05-current-state.md` 第五、七節 | — | 新功能，或要等裁決或外部資訊 | — | 回到專案工作時再處理 |

D-8 的明細：UI-001～005（UI-005 缺版本清單 API）、UI-008、UI-016；身分就緒（要等 shell 的事件順序）；15 處 `LocalDateTime.now()`（時間欄位要先裁決）；standalone 樣式；共用 403 元件；UI-015 的評分分布；Hibernate validate；Oracle 驗證 V6。

---

## E. Decisions & Constraints

| 項目 | 內容 | 狀態 |
|---|---|---|
| D-058 | api-spec 路徑以程式為準（stage 在前）；D-022 部分取代；保留 `GET /review-submissions/{id}` | ACCEPTED |
| D-059 | 移除 `rereview-revisions`；複審歷程一律經 `revision-history`（`ReviewReadAccessPolicy`） | ACCEPTED，已落地（#82） |
| D-032 | CLOSED 後沒有複審 revision 例外，也沒有例外入口 | ACCEPTED，程式已對齊（#39、#83） |
| D-050 第 7 點 | 評分表列表回傳 `{items}`，章節回傳 `{scoreSheetId, sections}` | ACCEPTED，已落地（#85）。D-058 第 6 點的文字仍描述修正前的狀態（屬裁決，未改） |
| **D-060** | 問題側涵蓋清單：擋新增；暫准範圍只提醒；清單讀 base；刪除不擋；只含後端（`domain`、`application`、`interfaces`、`reporting`）；前端之後再納入 | ACCEPTED，已生效（#91） |
| `hasExceptionEntry` 處理方式 | 直接刪除欄位，不保留固定 `false` | ACCEPTED（負責人 2026-11-18） |
| 修法程序 | 律法路徑的修改要有修法紀錄，加上負責人 SSH 簽章；只能用 merge commit 合併；修法 PR 不夾帶程式（P4） | ACCEPTED（既有） |
| 必要檢查 | `check`、`law-check`、`law-selftest`、`frontend-check`；分支須與 `main` 同步 | ACCEPTED（既有，#75、#78、#79） |
| 非律法路徑 | `docs/**`（`decision-log.md` 除外）不是律法；建構 PR 可以一起改 | FACT（`law/manifest.txt`） |
| HTTP 契約的立法方式 | 選項：契約規格或 OpenAPI 加比對閘門；或把 `interfaces` 移出問題側 | **PROPOSED**（未裁決） |
| ReviewCase 薄切片作為格式穩定度的驗證 | — | **PROPOSED** |
| 範例要能固定時鐘 | — | **PROPOSED** |
| 測試列為律法 | 2026-11-14 裁決延後生效，生效時點未裁決 | UNKNOWN／待裁決 |

技術限制：
- CI runner 固定 `ubuntu-24.04`，因為 Playwright 1.59.1 不支援 26.04。
- 後端用 JDK 17，`build.gradle` 宣告的是 Java 11。
- Oracle 不在 CI 裡（負責人的決定）。

---

## F. Verification Status

| 範圍 | 最後一次執行 | 結果 | 證據 |
|---|---|---|---|
| 後端 `./gradlew cleanTest check`（JDK 17，本機） | `2feb5bf`（#85 合併後）；#91 只改 harness，沒有重跑後端全套 | BUILD SUCCESSFUL；1223 次執行，0 失敗，0 跳過；閘門回報 957 個方法 | 本機紀錄（scratchpad，已不保留）；CI #87 run 顯示 957 |
| 前端 typecheck、Karma、Playwright（本機） | `2feb5bf` | typecheck 通過；unit 221 SUCCESS；e2e 18 通過、3 skip | 同上 |
| harness-core 測試 | `2a93471`（#91） | 112 個測試通過 | 本機；CI `law-selftest` 在 #91 通過（Linux、Windows） |
| `law_check`（#91 合併後） | `f4bade1` | 修法 PR、簽章有效、通過；`--base HEAD --head HEAD` 通過 | 本機 |
| CI（#82～#86、#90、#91） | 各 PR 的 head | 四項必要檢查加 windows、`spec-check` 全綠 | GitHub check runs |
| 合併後 `main`（`f4bade1`）的 push CI | **NOT_VERIFIED** | 本 session 沒有查詢 | — |
| 驗收範例 | 74 個（review-project 38、score-sheet 36）全部通過 | — | JUnit 報告（`2feb5bf` 本機） |
| hosted（shell）模式 | NOT_TESTED（從未驗證） | — | — |
| Oracle 上的 V6、SERIALIZABLE 匯出、儀表板查詢 | NOT_TESTED | — | — |
| D-060 閘門在真實 PR 上的提醒與阻擋 | 只在本機模擬過；**還沒有任何真實 PR 被新判決程式判決** | INFERENCE：應該會照模擬結果運作 | 本機模擬（分支已刪除） |

已知失敗：`known-test-failures.txt` 為空；目前沒有已知的紅燈測試（FACT，`2feb5bf`）。

---

## G. Risks & Blockers

| # | 項目 | 類型 | 說明 |
|---|---|---|---|
| G-1 | D-060 與規格格式 v1 規則 6 矛盾 | FACT | 格式禁止規格出現 HTTP 路徑與狀態碼，所以 `interfaces` 在現行格式下永遠只能是「暫准」。E-03 這一類缺陷因此擋不到 |
| G-2 | 暫准範圍佔多數（206/256） | FACT | ReviewCase、抽選、稽核、報表的行為只受建構者自己寫的測試保護 |
| G-3 | 格式 v1 沒有在複雜 aggregate 上驗證過 | INFERENCE | 狀態機、多角色、跨 aggregate 查詢、時間相關的規則，可能需要格式 v2 |
| G-4 | D-060 尚未在真實 PR 上運作過 | FACT | 第一個改到問題側的 PR 要確認 `::warning` 與阻擋確實出現 |
| G-5 | `exception-amber` 樣式在 `StatusOrBlockedBannerComponent` 已無頁面使用 | FACT | 低風險，屬於清理項目 |
| G-6 | D-058 第 6 點的文字過時（仍寫「程式回傳陣列」） | FACT | 屬裁決，要以修法更正 |
| G-7 | 身分就緒（hosted 模式身分切換的中間狀態） | FACT／外部依賴 | 要 shell 負責人提供事件順序 |
| G-8 | 殘留的驗證用遠端分支（`claude/verify-gate-*`） | FACT | 沒有影響。是否刪除由負責人決定 |
| G-9 | 第二支 YubiKey（#74）是否已在 GitHub 上簽過修法 | UNKNOWN | 本 session 沒有確認 #80、#81、#91 用的是哪一支金鑰 |
| G-10 | 需要負責人決策 | — | G-1（HTTP 契約）、格式 v2 的範圍、測試列為律法的時點、E-01／E-03 偵測器要不要做 |

---

## H. Next Session Starting Point

1. **起草「HTTP 契約如何立法」的選項，請負責人裁決**
   - **起始位置**：`specs/README.md` 第三節規則 6；`decision-log.md` D-060 第 2 點；`law/coverage.txt` 的 interfaces 區段；`docs/harness/12-escaped-defects.md` E-03；`docs/03-contracts/api-spec.md`。
   - **動作**：寫一份 1～2 頁的比較，不改律法。三個選項：
     - A：api-spec 的回應格式成為律法，再加一個 controller 比對閘門。
     - B：另立契約規格（例如 OpenAPI），在測試中比對實際回應。
     - C：把 `interfaces` 移出問題側，接受 HTTP 層不立法。
     
     逐項比較擋得住 E-03 的程度、維護成本、和格式 v1 的衝突，並附上建議。
   - **驗證**：每個選項都以 E-03（評分表回應格式）推演「是否會被擋下」。
   - **DoD**：負責人選定一個選項，並記錄為待起草的裁決（D-061）。
2. **依裁決起草 ReviewCase 薄切片規格**
   - **起始位置**：`spec-draft/`、`docs/02-ddd/state-machine.md`、`permission-matrix.md`、`application/reviewcase/`。
   - **動作**：只寫分派、初審提交、revision-history 讀取遮罩的 `usecases.yaml` 與 `examples.yaml`；用 `spec_lint --report-only` 檢查。
   - **驗證**：`spec_lint` 沒有擋下的問題；記錄格式 v1 寫不出來的規則。
   - **DoD**：有一份草稿與「格式缺口清單」，交給負責人裁決格式 v2。
3. **確認 D-060 在第一個真實 PR 上的行為**
   - **起始位置**：任何改到暫准範圍的建構 PR。
   - **動作**：看 `law-check` 的 step summary 與 `::warning`。
   - **驗證**：報告出現「未立法範圍的修改」。
   - **DoD**：結果記進 `docs/harness/03-harness-plan.md` 的驗證紀錄。
4. **以修法更正 D-058 第 6 點的過時文字（G-6）**
   - **起始位置**：`decision-log.md` D-058。
   - **動作**：附註「已由 #85 修正」；建立修法紀錄。
   - **驗證**：本機 `law_check` 只剩簽章未通過。
   - **DoD**：負責人簽章後合併。
5. **（選擇性）及格線第 3 項的 PR 驗證**
   - **起始位置**：`architecture/ArchitectureRules.java`。
   - **動作**：開驗證 PR，把一條規則改成永遠通過。這屬於律法路徑，所以要附上不簽章的修法紀錄；確認樣本測試失敗後關閉。
   - **驗證**：`check` 失敗，`blocked`。
   - **DoD**：記錄進 `03-harness-plan.md`。

---

## I. Quick Resume Summary

2026-11-18 這個 session 收斂了契約漂移，並補上 harness 的一個閘門。

**已驗證完成**（`main` = `f4bade1`，沒有開啟中的 PR）：
- 移除 `rereview-revisions`（#82）、移除 `hasExceptionEntry`（#83）、文件同步（#84）、修正評分表回應格式（#85）。都是先紅後綠，CI 全綠。
- 及格線第 1、2、4 項在 PR #87～#89 上驗證通過；第 3 項仍只有本機紀錄。
- D-060 問題側涵蓋清單已生效（#91，已簽章）：清單外的新範圍會被擋下，暫准範圍會被提醒。

**未完成**：ReviewCase 立法、格式缺口（時鐘、事件、HTTP 層）、測試列為律法的裁決、E-01 與 E-03 的偵測器。

**最大的阻礙**：D-060 與規格格式 v1 規則 6 矛盾，HTTP 契約目前無法立法，要先由負責人裁決。

**下一步**：先起草 HTTP 契約立法的三個選項比較，請負責人裁決（第 H 節第 1 項）。
