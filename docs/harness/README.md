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
| `12-escaped-defects.md` | 漏網紀錄：閘門全過卻漏掉的缺陷，以及對 harness 的回應 |
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
| `26-datetime-instant-draft.md` | 違憲清單第 3～6 項：時間改成 `Instant`（N-1～N-8，已由 D-075 裁決）：順序、`ReviewSubmission` 的時間、既有資料與時鐘、前端顯示、匯出、`local_time_zone`、文件範例、未立法範圍 |
| `samples/card-v2-trial/` | 格式樣本（不是律法）：白板 V10 照格式 v2 草案的試寫、暫時的檢查 `v2check.py`；v2.1 的回歸樣本 |
| `samples/reviewcase-v2-trial/` | 格式樣本（不是律法）：ReviewCase 照格式 v2 草案 B 組的試寫。`round-1/` 是薄切片與 12 個缺口；`round-2/` 是直向切片，用草案第十三節 Q-10～Q-20 建議的寫法重寫 |
| `references/` | 原文的本機副本。**受著作權保護，已列入 `.gitignore`，不進 repo** |

`01` 與 `06` 依原文撰寫。`02` 至 `04` 中標明「論文」的段落出自原文，其餘是討論中的建議。兩者衝突時以原文為準。

理論篇的出處：arXiv:2610.01372，<https://arxiv.org/abs/2610.01372>。

## 接續工作的方式

1. 在本 repo 的根目錄開 Claude Code session，不要在參考 repo 開。
2. 第一句話請 AI 讀 `docs/harness/README.md` 和 `05-current-state.md`。
3. 下一步：專案層級的工作見 `05-current-state.md` 第七節「建議的下一步」；harness 本身的建置順序見 `03-harness-plan.md` 的「建議順序」。

## Repo 之間的關係

| Repo | 角色 | 用法 |
|---|---|---|
| 本 repo（`quality-inspection`） | 工作區，harness 建在這裡 | session 從這裡開 |
| `~/source/poc/ai-coding-exercise-skills-uc` | ezddd-java 與 ezgo 的 harness，僅供參考 | 唯讀；用絕對路徑讀，或啟動時加 `--add-dir` |

注意事項：

- 參考 repo 在本機有兩份（`~/source/` 和 `~/source/poc/` 底下各一份）。本目錄引用的都是 `poc/` 那份。兩份是否相同尚未比對。
- 參考 repo 的授權條款尚未確認。要複製它的腳本或文件進本 repo 前，先確認可以這樣用。概念和格式自行重寫則無此問題。
- 本目錄提到的參考 repo 路徑（如 `.claude/skills/ezgo/references/mgd-obligations.md`）都是相對於參考 repo 的根目錄。
