# harness-core

修法機制的核心：判斷一次修改有沒有動到律法、有沒有照程序留下修法紀錄。不分程式語言，也不含任何專案專屬的設定。

版本見 `VERSION`。它做兩件事：分類與結構檢查（一定會做），以及核准簽章的驗證（清冊有要求時才做）。

## 這個目錄之後會搬走

它現在放在 `quality-inspection` 這個 repo 裡，目標是拆成獨立的 repo，讓任何專案都能安裝。為了之後拆得乾淨，這個目錄守三條規則：

1. 裡面的程式不得引用目錄外的任何路徑。專案專屬的設定一律從專案的律法清冊讀。
2. 測試只測自己，在暫存的 git repo 裡進行，不依賴所在專案的內容。
3. 所在專案使用它的方式，要和將來從外部安裝時相同，只差在來源位置。

## 內容

| 檔案 | 用途 |
|---|---|
| `law_check.py` | 判決程式。只用 Python 標準函式庫與 git |
| `law_approve.py` | 核准工具。核准者在自己的電腦上執行，對修法的內容雜湊簽章 |
| `APPROVING.md` | 核准者指引：準備實體金鑰、登記公鑰、每次怎麼核准 |
| `spec_lint.py` | 規格格式的檢查程式（格式見所在專案的 `specs/README.md`）。需要 PyYAML |
| `tests/` | 判決程式與核准工具的測試 |
| `ci/github/law-check.yml` | GitHub Actions 的 workflow 範本 |
| `VERSION` | 版本號 |

GitLab CI 的範本尚未實作。

## 專案要自己準備的東西

| 檔案 | 內容 |
|---|---|
| 律法清冊（預設 `law/manifest.txt`） | 哪些路徑是律法、屬於哪一類；修法紀錄放哪個目錄；裁決紀錄是哪個檔案 |
| 修法紀錄的目錄（清冊裡用 `@amendments` 指定） | 每次修法一個 JSON 檔 |

清冊的格式：

```
@decision-log <裁決紀錄的檔案>
@amendments <修法紀錄的目錄>/
@allowed-with-amendment <修法 PR 可以一起改的路徑樣式>

<路徑樣式>  <類別>
```

- 類別：`ruling`、`spec`、`gate`、`tradition`、`baseline`。只有 `baseline` 的規則不同：只刪除行的修改免修法。
- 路徑樣式：`*` 比對一層目錄內的任意字元，`**` 可跨目錄，結尾是 `/` 表示整個目錄。一個路徑符合多行時以最後一行為準。
- **清冊必須把 `harness-core/`（或安裝後的位置）、CI 設定、清冊自己列為律法。** 否則 PR 可以直接改掉它們。

實際的例子見所在專案的 `law/manifest.txt` 與 `law/README.md`。

## 規格檢查要的設定

`spec_lint.py` 不寫死任何專案路徑：

| 設定 | 來源 |
|---|---|
| 裁決紀錄 | 清冊的 `@decision-log`（`--manifest` 指定清冊，預設 `law/manifest.txt`） |
| 設計決策（`design_decision` 出處）所在的目錄 | `--design-docs` |
| 驗收範例與情境（`example`、`scenario` 出處）所在的目錄 | `--testing-docs` |
| 禁用的實作詞彙 | 規格目錄裡的 `forbidden-vocabulary.txt`，缺少時判定無法評估 |

沒有設定 `--design-docs` 或 `--testing-docs` 時，用到該類出處的規則一律算問題，不會略過。

## 核准簽章

清冊加上下列指示之後，修法 PR 除了結構檢查，還要有核准者的簽章才會通過：

```
@approval-schemes ssh
@approvers-ssh <核准者清單的檔案>
@approvals-required 1
```

| 指示 | 說明 |
|---|---|
| `@approval-schemes` | 接受哪些簽章方案，用逗號分隔。目前支援 `ssh`。沒有這一行就不檢查簽章，報告會把它列為沒有評估的項目 |
| `@approvers-<方案>` | 那個方案的核准者清單在 repo 裡的路徑。`ssh` 用 OpenSSH 的 allowed_signers 格式 |
| `@approvals-required` | 需要幾個不同核准者的有效簽章，預設 1 |

運作方式：

- 判決程式為每次修法算一個**內容雜湊**：修法紀錄本身，加上每個被改動的律法檔案的內容與檔案模式（一般檔案、可執行檔、符號連結）。報告會印出這個值。
- 核准者用 `law_approve.py` 對這個值簽章，產生 `<修法紀錄的 id>.<核准者名稱>.approval.json`，和修法紀錄放在同一個目錄。
- 核准檔的欄位：`amendment`（修法紀錄 id）、`scheme`、`signer`、`digest`（被簽的內容雜湊）、`reviewed_commit`（核准者檢查的 commit）、`signature`。
- `reviewed_commit` 不在簽章裡，但必填，判決程式驗證它是存在的 commit（不是 tag）、在這次修改的歷史裡，而且在它上面重新認定的修法內容算出的雜湊等於 `digest`。
- 簽完之後，紀錄或律法檔案再有任何修改，雜湊就不同，簽章失效。
- 核准者清單和清冊一樣，讀的是 base 上的版本。**清冊必須把核准者清單列為律法。**

## 問題側涵蓋清單

清冊加上 `@coverage <清單檔案>` 之後，每個 PR（建構 PR 也一樣）都會檢查問題側的修改是否在清單內。清單讀 base 上的版本，所以清單本身要列為律法。

```
@problem-side <路徑樣式>                 問題側的範圍，可以有多行
<路徑樣式>  legislated     <規格>         已立法：規格（目錄或檔案）必須存在於 base
<路徑樣式>  grandfathered  <說明>         未立法但暫准
```

| 修改的檔案 | 判決 |
|---|---|
| 不在 `@problem-side` 範圍內 | 不檢查 |
| 刪除 | 不檢查 |
| 符合 `legislated` | 規格存在就通過。規格可以是目錄（aggregate），也可以是檔案（跨 aggregate 的共用層，例如 `specs/http-common.yaml`，D-061） |
| 符合 `grandfathered` | 通過；報告列出「未立法範圍的修改」，在 GitHub Actions 上另外輸出 `::warning` |
| 不符合任何一行 | 未通過：未立法的新範圍 |

路徑樣式的寫法與清冊相同，一個路徑符合多行時以最後一行為準。清單格式錯誤、缺少 `@problem-side`、或 base 上沒有清單檔案時，判定無法評估。

新增簽章方案的方式：在 `law_check.py` 的 `VERIFIERS` 登記一個驗證函式，輸入是核准者清單、核准者名稱、被簽的訊息、簽章。

核准者的操作步驟見 `APPROVING.md`。

## 執行

```bash
python3 harness-core/law_check.py --base <合併的目標 commit> --head <要檢查的 commit>
```

| 參數 | 說明 |
|---|---|
| `--base` | 合併的目標。清冊讀這個 commit 上的版本 |
| `--head` | 要檢查的 commit |
| `--repo` | git 工作目錄，預設是目前目錄 |
| `--manifest` | 清冊在 repo 裡的路徑，預設 `law/manifest.txt` |

結束代碼：`0` 通過，`1` 未通過，`2` 無法評估。無法評估不算通過。

需求：Python 3.8 以上、git。清冊要求 SSH 簽章時，另外需要 OpenSSH 8.2 以上的 `ssh-keygen`。

## 安裝到另一個專案（目前的做法）

還沒有拆成獨立的 repo，所以目前只能複製：

1. 把整個 `harness-core/` 複製到專案裡。
2. 寫專案自己的律法清冊。
3. 把 `ci/github/law-check.yml` 複製到專案的 `.github/workflows/`。
4. 在專案的 CI 裡執行 `python3 -m unittest discover -s harness-core/tests`。

複製的做法收不到之後的修正。拆成獨立的 repo 之後，改成用版本號引用。

## CI 上的兩個要點

- **判決程式不能取自被檢查的那一方。** 範本用 `pull_request_target`，再用 `git show` 從 base 的 commit 把判決程式抽出來執行，所以 PR 改不到判決自己的程式。
- **不執行 PR 的任何程式。** 工作目錄裡是 PR 的檔案，job 只用 git 讀它們。

## 測試

```bash
python3 -m unittest discover -s harness-core/tests -v
```
