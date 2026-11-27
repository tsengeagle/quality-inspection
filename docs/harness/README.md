# Harness 建置筆記

本目錄記錄「依 MGD 精神為本專案建立 harness」的討論結論、計畫與現況。
內容整理自 2026-11-11 至 2026-11-12 的工作 session。

## 文件定位

- **本目錄不是權威文件。** 業務規則仍以 `docs/01-requirements/decision-log.md` 為最高依據，權威順序見 `AGENTS.md`。
- 本目錄只回答三件事：harness 要做什麼、為什麼這樣做、目前做到哪裡。
- 內容若與權威文件衝突，以權威文件為準，並回頭修正本目錄。

## 目標

依 MGD（Misfit-Governed Development）的精神，仿照 `ezddd-java`，建立組織自己的 harness。

限制：

- 不繼承 ez 系列的框架和元件。
- 不維護翻譯表。
- 不建立第 N 套 binding。

本專案是組織內的第一個樣本。先在這裡做到「會擋人」，第二個專案要用時再抽出共用部分。

## 文件索引

| 檔案 | 內容 |
|---|---|
| `01-mgd-concepts.md` | MGD 的基本概念、十條義務、名詞對照 |
| `02-ezddd-java-lessons.md` | ezddd-java 踩過的坑、能力限制、哪些值得學 |
| `03-harness-plan.md` | 本專案的 harness 計畫：義務對照、搬與不搬、工具、順序 |
| `04-definition-of-enough.md` | 怎麼判斷 harness 做得夠不夠 |
| `05-current-state.md` | 2026-11-18 的專案現況：頁面進度、驗證閘門、已知缺口、待決事項、建議的下一步、未驗證事項 |
| `06-paper-digest.md` | MGD 兩篇原文的逐節重點整理，含出處與授權 |
| `07-github-settings.md` | GitHub 端要手動做的設定（main 的保護規則、必要檢查）與驗收方式 |
| `08-legislation-design.md` | 修法機制的設計草稿：律法清冊、修法紀錄、簽章核准、CI 判決規則；涵蓋 GitHub 與 GitLab、多種 AI 工具。待審，尚未實作 |
| `09-law-inventory.md` | 律法清冊的盤點：方法、缺口、裁決（測試與規格延後列入）、未裁決的項目 |
| `10-spec-format.md` | 規格格式 v1 的決定與理由；對 ReviewProject 草稿的試跑結果；對外部資源協定（醫事人員卡）的試寫與格式 v2 的候選 |
| `11-example-runner.md` | 第 4 期：驗收範例的執行器。設計決定、驗證、第一次執行的結果 |
| `12-escaped-defects.md` | 漏網紀錄：閘門全過卻漏掉的缺陷，以及對 harness 的回應。E-01～E-03（2026-11-18）、E-04～E-05（2026-11-27，前端與 HTTP 契約不符）、E-06～E-09（回頭補記的四筆）；立法時在未立法範圍找到的不逐筆記；之後怎麼記 |
| `13-http-contract-options.md` | HTTP 契約如何立法的選項比較（A／B／C），待負責人裁決（D-061 候選） |
| `14-d061-draft.md` | D-061 草案：HTTP 契約升格為律法。草案條文、待裁決的 Q-1～Q-6、裁決後的實作順序 |
| `15-spec-format-v2-draft.md` | 規格格式 v2 草案（含白板 V10 的 v2 試寫與 26 個缺口，第十一節）：補 v1 的漏洞、ReviewCase 需要的寫法、外部資源協定；需求對照、Q-1～Q-9 的裁決（Q-1、Q-2 成為 D-063；Q-3～Q-9 的查證見第十節）、實作順序、D-063（v1.1）起草時的條文 |
| `16-vertical-slice-draft.md` | 直向切片（抽選到初審提交）立法前的草案：程式與試寫規格對不上的三處（M-1～M-3）的事實、選項、建議與待裁決，以及裁決後的順序 |
| `17-slice-open-questions-draft.md` | 直向切片的六題（Q-01 後三列、Q-02、Q-03、Q-05～Q-07，已由 D-067 裁決）：程式現況、權威文件、選項、建議與裁決後的順序 |
| `18-slice-followup-questions-draft.md` | 直向切片的後續四題（F-1～F-4，已由 D-068 裁決）：草稿的狀態錯誤、複審的指派錯誤、批次完成事件的計數、未發布版本的抽選；程式現況、權威文件、選項、建議與裁決後的順序 |
| `19-rereview-slice-draft.md` | 複審切片（S-1～S-6，已由 D-069 裁決）：初審固定到複審結案的範圍、狀態錯誤代碼、事件名稱、盲審、版本；程式現況、權威文件、選項與建議 |
| `20-q08-rereview-event-fields-draft.md` | Q-08 四個複審事件的最低必要欄位（E-1～E-4，已由 D-070 裁決）：`RereviewerAssigned`、`RereviewStarted`、`RereviewCompleted`、`ReviewCaseClosed`；程式現況、稽核遮罩的查證、選項與建議 |
| `21-audit-detail-initial-reader-draft.md` | 稽核的 `detail` 繞過 D-055 第 5 點的讀取者紀錄（A-1～A-3，已由 D-071 裁決）：生命週期與稽核軌跡兩個出口的遮罩條件、粒度、`SYSTEM_ADMIN` |
| `22-constitution-datetime-draft.md` | 憲法草案第一條：日期時間的表示（T-1～T-5，已由 D-072 裁決）：條文、憲法節的結構與位階、違憲清單、稽核 `detail` 的例外、D-071 範例 |
| `23-dueat-input-timezone-draft.md` | 違憲清單第 2 項：HTTP 收進來的 `dueAt` 必須帶時區（I-1～I-5，已由 D-073 裁決）：前端送出的時區、換算的位置、錯誤回應、未立法範圍 |
| `24-request-side-examples-draft.md` | 規格格式 v2.1：請求端的範例（R-1～R-8，已由 D-074 裁決）：範圍、寫法、`body_raw`、`then`、操作者與 `api-key`、執行器；附三項查證 |
| `25-topic-backlog.md` | 題目清單：負責人核准的立法題目與順序（不是律法） |
| `27-rereview-submission-events-draft.md` | D-069 留下的兩件：複審的提交事件與必答題檢查（V-1～V-5，已由 D-076 裁決）；附 `ReviewSubmission` 稽核紀錄沒有遮罩的發現 |
| `28-report-date-filter-taipei-draft.md` | 報表的日期篩選固定以台北解讀（V-1～V-5，已由 D-077 裁決）：送出轉換、預設區間、範圍、測試的時區 |
| `29-review-submission-audit-mask-draft.md` | `ReviewSubmission` 稽核紀錄的遮罩（V-1～V-5，已由 D-078 裁決）：兩個出口的查證、範圍、找到所屬案件、遮罩條件；附其他種類 `detail` 的掃描 |
| `30-initial-draft-audit-hospital-draft.md` | 初審草稿的稽核紀錄沒有院區（W-1～W-3，已由 D-079 裁決）：查證、補院區、已寫入的紀錄、要不要立一般規則 |
| `31-case-return-cancel-close-draft.md` | ReviewCase 切片：退回、重開、取消、不經複審結案（X-1～X-8，已由 D-080 裁決）：實測、可取消的狀態、重開事件、舊草稿、理由的 `detail`、授權、結案事件、錯誤代碼 |
| `32-restart-draft-owner-draft.md` | 重開後改派，新醫師開始初審拿到原醫師的草稿而卡住（Y-1，已由 D-081 裁決）：實測、原因、修法 |
| `33-reassign-draft.md` | ReviewCase 切片 A：改派初審醫師、改派複審醫師（Z-1～Z-7，已由 D-082 裁決）：實測、狀態錯誤、輸入檢查、改派給同一人、未指派就改派、事件、檢查順序 |
| `34-revise-and-reviewcase-draft.md` | ReviewCase 切片 D：修正已提交的初審與複審，並立法 `ReviewCase.java`（U-1～U-10，已由 D-083 裁決）：實測、必答題、沒有變更、稽核紀錄、錯誤代碼、事件、不變式、沒有被寫入的欄位 |
| `35-read-draft.md` | ReviewCase 切片 E：讀取（V-1～V-11，已由 D-084 裁決）：實測、多角色與院區的順序、初審醫師看複審、複審醫師看初審醫師身分、SYSTEM_ADMIN 白名單、退回案件的分派頁、讀過初審的候選人、讀取者紀錄的範例、輸出欄位、沒有被寫入的欄位、登記範圍 |
| `36-review-submission-draft.md` | 切片 C：`ReviewSubmission` 本體、計分，以及 ReviewCase 的收尾（T-1～T-14，已由 D-085 裁決）：實測、RANGE 的錯誤作答與上限、小數位數、未知題目、重複作答、三種題目的計分、狀態轉移與不變式、草稿的分數、評分表版本、附件、修正歷史的可見規則、補範例、登記範圍 |
| `37-sampling-batch-draft.md` | 切片 A：SamplingBatch 其餘部分（S-1～S-16，已由 D-086 裁決）：失敗的批次、D-013 與重跑、母體 0 筆、重複、抽樣與排除、不變式、規則快照、預覽、執行頁、列表、`SYSTEM_ADMIN` 的詳情、匯出、架構基線、登記範圍 |
| `45-response-dto-and-list-shape-draft.md` | 回應 dto 與列表的形狀（P-1～P-8，已由 D-096 裁決）：直接輸出 application 物件的 13 個端點、`Map` 回應、application 的 Jackson 標註、`Page` 與 `{ items }`、匯出與裸陣列、分批；第六節是附帶發現：domain 事件自己組稽核的 JSON 字串 |
| `44-versions-and-pending-draft.md` | 專案版本管理與未裁決事項（已由 D-094、D-095 裁決）：不開放改版、逾期的定義、多角色拒絕定案、`latestRevisionSummary` 的移除與 HTTP 契約的過渡形狀 |
| `43-api-spec-gap-draft.md` | HTTP 回應與 `api-spec.md` 的落差（G-1～G-7，已由 D-093 裁決）：執行與重跑的回應、請求本文的 `mode`、一律 null 的欄位、範圍、分批 |
| `42-event-occurred-at-draft.md` | 事件的發生時間改用注入的時鐘（O-1～O-6，已由 D-092 裁決）：時間在哪裡決定、`eventId`、同時間的順序、範例、儀表板的「現在」、分批 |
| `41-project-scoresheet-events-draft.md` | review-project、score-sheet 的事件驗證（E-1～E-7，已由 D-090 裁決）：驗證的程度、事件的屬性、沒有變更也發事件、綁定評分表的事件、domain-events.md、occurredAt |
| `40-project-scoresheet-request-side-draft.md` | review-project、score-sheet 的格式 v2 與請求端（V-1～V-8，已由 D-089 裁決）：分批、升級的作法、本文的列舉、經過 HTTP 的成功範例、建立專案的回應、登記、事件的驗證 |
| `39-http-interfaces-common-draft.md` | 切片 D、E：HTTP 介面與共用元件（H-1～H-12，已由 D-088 裁決）：分批、回應契約照程式、涵蓋缺口、請求端的檢查、沒有外殼的 400、操作者的傳遞、共用元件的登記；第九節是 D-1 的實測 |
| `38-reporting-draft.md` | 切片 B：報表與儀表板（R-1～R-10，已由 D-087 裁決）：入口與範例、時間區間的檢查與錯誤代碼、規格的位置、件數的定義（已取消的案件）、逾期、比率、篩選與院區、匯出的規則與遮罩、匯出的稽核紀錄、登記範圍 |
| `26-datetime-instant-draft.md` | 違憲清單第 3～6 項：時間改成 `Instant`（N-1～N-8，已由 D-075 裁決）：順序、`ReviewSubmission` 的時間、既有資料與時鐘、前端顯示、匯出、`local_time_zone`、文件範例、未立法範圍 |
| `47-event-detail-draft.md` | 事件的 `detail` 改成結構化（E-1～E-8，負責人 2026-11-27 裁決全部照建議、E-6 動工前再確定範圍，條文待修法 PR 寫入）：32 個事件怎麼產生 `detail`、`SamplingBatchFailedEvent` 的潛在缺陷、誰讀 `detail`、範例與測試釘住了什麼；型別、逐字相同、特徵測試、序列化、基底建構子、閘門、分批 |
| `samples/card-v2-trial/` | 格式樣本（不是律法）：白板 V10 照格式 v2 草案的試寫、暫時的檢查 `v2check.py`；v2.1 的回歸樣本 |
| `samples/reviewcase-v2-trial/` | 格式樣本（不是律法）：ReviewCase 照格式 v2 草案 B 組的試寫。`round-1/` 是薄切片與 12 個缺口；`round-2/` 是直向切片，用草案第十三節 Q-10～Q-20 建議的寫法重寫 |
| `48-escaped-defects-inventory-draft.md` | 漏網紀錄的盤點與維護（L-1～L-5，負責人 2026-11-27 裁決全部照建議，已照做；給 AI 的指示還沒有改）：2026-11-18 之後找到的缺陷分三類（未立法範圍、已立法之後、閘門或流程）、什麼算漏網、怎麼記、誰在什麼時候記、植入測試 |
| `49-admin-case-list-whitelist-draft.md` | `SYSTEM_ADMIN` 的案件列表照白名單收斂（W-1～W-9；負責人 2026-11-27 裁決 W-1～W-6、W-9 照建議，W-8 匯出也是缺陷，W-7 照修正後的建議；條文待修法 PR）：三個來源對列表欄位的說法、程式只加標記沒有刪減、沒有任何 `SYSTEM_ADMIN` 的列表範例；欄位、名稱、契約的寫法、規則與範例、篩選、匯出、分批 |
| `50-frontend-contract-gate-draft.md` | 前端與 HTTP 契約之間的閘門（G-1～G-10，負責人 2026-11-27 裁決全部照建議、G-9 先做，條文待修法 PR 寫入）：2026-11-27 的六個前端缺陷、前端的假回應沒有型別、E2E 沒有型別檢查、前端閘門的設定不受律法保護（實驗）；型別由契約產生、落後清單、涵蓋、分批 |
| `51-admin-export-whitelist-draft.md` | `SYSTEM_ADMIN` 的匯出照白名單收斂（X-1～X-8，負責人 2026-11-27 裁決全部照建議，條文待修法 PR 寫入）：匯出的裁決都只談分數、UI-017 明文把十欄給了 `SYSTEM_ADMIN`、權限矩陣沒有報表兩頁；欄位、分數怎麼處理、契約、前端、與案件列表併成一題 |
| `references/` | 原文的本機副本。**受著作權保護，已列入 `.gitignore`，不進 repo** |

`01` 與 `06` 依原文撰寫。`02` 至 `04` 中標明「論文」的段落出自原文，其餘是討論中的建議。兩者衝突時以原文為準。

理論篇的出處：arXiv:2610.01372，<https://arxiv.org/abs/2610.01372>。

## 接續工作的方式

1. 在本 repo 的根目錄開 Claude Code session，不要在參考 repo 開。
2. 第一句話請 AI 讀 `docs/harness/README.md` 和 `05-current-state.md`。
3. 下一步：立法題目的順序與進度見 `25-topic-backlog.md`；harness 各期的現況見 `03-harness-plan.md` 第三、六節。`05-current-state.md` 只有最前面一節是 2026-11-26 更新的。
4. 找到缺陷時記漏網（負責人 2026-11-27 裁決，細節在 `12-escaped-defects.md`「之後怎麼記」）：
   - 起草時，「查到的事實」裡每一個缺陷都註明當時是否已立法。
   - 修缺陷的建構 PR，說明裡回答「閘門為什麼沒擋到」。
   - 已立法的範圍裡找到的，在 `12-escaped-defects.md` 記一筆，並寫明回應（還沒決定就寫「待裁決」）。

## Repo 之間的關係

| Repo | 角色 | 用法 |
|---|---|---|
| 本 repo（`quality-inspection`） | 工作區，harness 建在這裡 | session 從這裡開 |
| `~/source/poc/ai-coding-exercise-skills-uc` | ezddd-java 與 ezgo 的 harness，僅供參考 | 唯讀；用絕對路徑讀，或啟動時加 `--add-dir` |

注意事項：

- 參考 repo 在本機有兩份（`~/source/` 和 `~/source/poc/` 底下各一份）。本目錄引用的都是 `poc/` 那份。兩份是否相同尚未比對。
- 參考 repo 的授權條款尚未確認。要複製它的腳本或文件進本 repo 前，先確認可以這樣用。概念和格式自行重寫則無此問題。
- 本目錄提到的參考 repo 路徑（如 `.claude/skills/ezgo/references/mgd-obligations.md`）都是相對於參考 repo 的根目錄。
