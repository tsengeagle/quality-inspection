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
| `10-spec-format.md` | 規格格式 v1 的決定與理由；對 ReviewProject 草稿的試跑結果 |
| `11-example-runner.md` | 第 4 期：驗收範例的執行器。設計決定、驗證、第一次執行的結果 |
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
