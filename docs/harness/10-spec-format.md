# 規格格式的決定

2026-11-15。本文件記錄規格格式 v1 的各項決定與理由。格式本身寫在 `specs/README.md`，那份是律法；本文件只是說明。

## 一、背景

負責人 2026-11-14 裁決：測試與規格要列為律法，但要等 `/specs` 的格式完整、問題側的規格穩定之後才生效（`09-law-inventory.md` 第三節）。所以先定格式。

格式以 `spec-draft/README.md` 的七條規則為起點。那份是 2026-11-11 為 ReviewProject 試作時寫的。

## 二、決定（2026-11-15，負責人照建議裁決）

| # | 題目 | 決定 | 理由 |
|---|---|---|---|
| 1 | 檔案格式 | YAML，CI 安裝固定版本的 PyYAML | 人要讀、要寫。TOML 寫巢狀規則太冗長；JSON 不適合手寫 |
| 2 | 檔案切分 | 每個 aggregate 一組檔案 | 和草稿一致；規則多了再拆 |
| 3 | 出處的寫法 | 結構化：`decision`、`design_decision`、`doc`、`example`、`scenario`、`rule` | 自由文字無法機械核對。試跑時發現 `DD-03` 會被誤認為 `D-03` |
| 4 | 錯誤代碼對應 HTTP 狀態碼 | 放在實作端的對應表，規格只寫分類 | 規格不出現實作詞彙 |
| 5 | `openspec/` | 移除 | 目前是空的；`openspec/specs` 容易和 `/specs` 混淆。另開 PR 處理，見第五節 |
| 6 | 沒有範例涵蓋的規則 | 先只列出，第 4 期之後改為擋下 | 現在擋下的話，ReviewProject 有 10 條規則沒有範例，會卡住升格 |

## 三、和草稿格式的差異

| 項目 | `spec-draft/README.md` | 格式 v1 |
|---|---|---|
| `spec_version` | `0.1-draft` | `1` |
| `source` | 自由文字或文字清單 | 結構化清單，每一項必須指向存在的東西 |
| `proposed` 規則 | `question` 指向題號 | 同，且題號必須是 `OPEN-QUESTIONS.md` 裡的標題 |
| 錯誤代碼分類 | 有，未限定 | 只能是 `denied`、`not_found`、`invalid_input`、`conflict`、`rule_violation` |
| 實作詞彙 | 人工檢查 | `specs/forbidden-vocabulary.txt` 機械比對 |
| 檢查程式 | 無 | `harness-core/spec_lint.py`，CI 的 `spec-check` 工作 |

## 四、對 ReviewProject 草稿的試跑（2026-11-15）

`spec-draft/` 以「只列出」模式檢查，結果：規則 36 條，問題 50 個。

| 問題 | 數量 | 處理 |
|---|---|---|
| `spec_version` 不是 1 | 3 | 升格時改 |
| 出處還是文字寫法 | 46 | 升格時改成結構化 |
| RP-PUB-01 的 `question` 是 Q-08，但 `OPEN-QUESTIONS.md` 沒有這個標題 | 1 | 實際問題，升格前要修 |

另外列出：10 條規則沒有範例涵蓋；1 個範例的 `basis` 含 `proposed` 規則（RP-PUB-01）。

## 五、還沒做的

- 移除 `openspec/`。牽涉 `.claude/commands/opsx/` 與 `.claude/skills/openspec-*`，它們是律法，而 `openspec/` 本身不是；同一個修法 PR 不能夾帶非律法的修改，所以要分兩個 PR。
- 第三節規則 8（架構詞彙只用 `glossary.md` 有定義的詞）還沒有機械檢查。
- ReviewProject 的升格：改寫出處、補範例、裁決 RP-PUB-01，以及負責人逐條讀過。
