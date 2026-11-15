# GitHub 端要手動做的設定

`.github/CODEOWNERS` 和 `.github/workflows/backend-check.yml` 已在 repo 裡，但兩者本身沒有強制力。
下列設定只有 repo 管理員能在 GitHub 上做。做完之前，P1、P6、P9 都只是「檔案存在」。

## 一、main 的保護規則

位置：Settings → Rules → Rulesets → New branch ruleset（或 Settings → Branches → Add rule）。目標分支：`main`。

下表是依 2026-11-14 的裁決要設定的值。

| 設定 | 值 | 對應義務 |
|---|---|---|
| Require a pull request before merging | 開 | P6 |
| Require review from Code Owners | **不開**（見第二節） | P6 |
| Required approvals | **0**（見第二節） | P6 |
| Require status checks to pass | 開，加入三項：`check`、`law-check`、`law-selftest` | P1、P9 |
| Require branches to be up to date before merging | 開 | P1 |
| Block force pushes | 開 | P4 |
| Require linear history | **不開**：它禁止 merge commit，而修法 PR 必須用 merge commit 合併（見下方） | P6 |
| Bypass list（ruleset）／Do not allow bypassing the above settings（舊版規則） | **不放任何人**／勾選 | P9 |

三個檢查的名稱是 job 的名稱，不是 workflow 的名稱。它們都已經在 GitHub 上跑過，會出現在可選清單裡。`law-check (post-merge)` 只在推送到 `main` 時執行，不要列為必要檢查。

另外在 Settings → General → Pull Requests，**只勾選「Allow merge commits」**，取消「Allow squash merging」與「Allow rebase merging」。原因：核准檔記錄核准者檢查的 commit（`reviewed_commit`），squash 或 rebase 合併會產生新的 commit，被核准的 commit 不會進入 `main` 的歷史，合併後的 `law-check (post-merge)` 會失敗（2026-11-15，PR #20）。GitHub 的這個設定對整個 repo 生效，無法只限修法 PR；本 repo 從 PR #8 起都用 merge commit 合併，不影響現有做法。

**2026-11-14 裁決：不留繞過權限。** 包含 repo 管理員在內，沒有人能在必要檢查未通過時合併。必要檢查本身壞掉的情況，照第四節的 SOP 處理。

## 二、單人 repo 的限制與目前的裁決

GitHub 不允許 PR 的作者核准自己的 PR。目前 CODEOWNERS 只有一個人，而所有 PR（包含 AI 產生的）都是用這個帳號開的，所以沒有人能核准。

**2026-11-14 裁決：code owner 目前只保留形式。** 具體做法：

- `.github/CODEOWNERS` 留著，標明哪些路徑屬於律法與閘門，PR 頁面會顯示這些檔案的負責人。
- 第一節的「Require review from Code Owners」與「Required approvals」**暫不啟用**。啟用後負責人自己的 PR 無法合併。
- 其餘設定（必須經 PR、必要檢查 `check`、禁止 force push）不受影響，仍然要設。

後果：P6 沒有伺服器端的強制力。修改律法或閘門的 PR 只要 CI 通過就能合併，防線只剩合併前的人工檢視。義務對照表的 P6 維持 ⚠️。

重新啟用的條件（任一成立）：有第二位可核准的人；或 AI 改用另一個 GitHub 帳號開 PR，讓負責人成為核准者而不是作者。

## 三、設定完成後的驗收

對應 `04-definition-of-enough.md` 的及格線第 1、2 項，各做一次並把 PR 連結記到 `05-current-state.md`：

1. 開一個 PR，在 `interfaces` 層直接引用 `infrastructure` 的類別。預期：`check` 紅燈，無法合併。
2. 用非負責人的身分開一個 PR 修改 `decision-log.md`。預期：顯示需要 code owner 核准，無法合併。**依第二節的裁決，這一項目前無法驗收。**
3. 開一個 PR，讓 `check` 失敗（例如第 1 項的修改）。預期：合併按鈕無法使用，repo 管理員也一樣。

## 四、必要檢查本身壞掉時的處置 SOP

### 什麼時候適用

只適用於一種情況：**必要檢查失敗的原因在檢查本身，而修正它的 PR 也因此過不了那個檢查。**

實例：PR #9。`law-check` 執行的是 `main` 上的 workflow，當時 `main` 上的版本抓不到 PR 的內容。PR #9 正是修這個問題的 PR，但它自己的 `law-check` 一定是紅的，因為判決用的是 base 上還沒修好的版本。

不適用的情況：

- 檢查是對的、是 PR 的內容有問題。那就修 PR。
- 檢查偶爾失敗（flaky）。先找出原因；重跑一次仍失敗就當成真的失敗處理。
- 趕時間。

### 判斷是不是檢查本身壞掉

三個條件都成立才算：

1. 失敗發生在檢查的準備步驟（取得程式碼、安裝工具、讀設定），或檢查在 `main` 上對一個沒有任何修改的 commit 也會失敗。
2. 失敗的原因可以指出是哪一個檔案的哪一段，而那個檔案屬於檢查本身（workflow、判決程式、建置設定）。
3. 修正的 PR 只改檢查本身，不夾帶其他修改。

第 3 點是為了讓暫時放行的範圍盡量小。

### 步驟

| # | 動作 | 誰做 | 要留下的紀錄 |
|---|---|---|---|
| 1 | 在修正的 PR 上留言：哪個檢查失敗、原因、為什麼這個 PR 自己過不了 | 開 PR 的人或 AI | PR 留言 |
| 2 | 確認其他必要檢查全部通過。只有壞掉的那一項可以是紅的 | 負責人 | — |
| 3 | 在本機用修正後的版本跑一次同樣的檢查，確認修正有效 | 開 PR 的人或 AI | 把指令與結果寫進 PR 留言 |
| 4 | 到 Settings → Rules，把**壞掉的那一項**從必要檢查移除。其他項目不動 | 負責人 | 記下時間 |
| 5 | 合併修正的 PR | 負責人 | — |
| 6 | **立刻**把那一項加回必要檢查 | 負責人 | 記下時間 |
| 7 | 開一個新的小 PR，確認那個檢查現在會通過，而且確實執行到了判決（看它的輸出，不只看綠燈） | 開 PR 的人或 AI | 新 PR 的連結 |
| 8 | 在下面的「例外紀錄」加一列 | 負責人或 AI | 本文件 |

第 4 到第 6 步之間，那個檢查對所有 PR 都沒有強制力。這段時間不要合併其他 PR。

如果壞掉的是 `law-check`，而修正的 PR 改的是律法路徑（幾乎一定是），它仍然要附修法紀錄。`main` 上的合併後檢查會在第 5 步之後用合併前的判決程式再檢查一次；那一次如果也因為同樣的原因失敗，是預期內的，在例外紀錄裡註明。

### 不可以做的事

- 不可以把自己加進繞過清單來合併。
- 不可以一次移除多個必要檢查，或整個關掉保護規則。
- 不可以為了讓檢查通過而把檢查改成永遠通過、跳過測試、或刪掉判決步驟。
- 不可以在第 4 到第 6 步之間合併別的 PR。

### 例外紀錄

每次走這個 SOP 都要留一列。沒有紀錄的放行視為缺陷。

| 日期 | 檢查 | 原因 | 修正的 PR | 移除與加回的時間 | 驗證的 PR |
|---|---|---|---|---|---|
| 2026-11-14 | `law-check` | 私有 repo 上自行執行的 `git fetch` 沒有權限，判決程式沒有執行到 | #9 | 不適用：當時還沒有設必要檢查，紅燈下直接合併 | #10：`law-check` 通過，輸出「建構 PR」 |
| 2026-11-15 | `law-check` | **不是 SOP 的情況，依 D-052 破例**：登記備援核准金鑰時，唯一登記的金鑰的把手檔在暫時拿不到的舊電腦上，且沒有備份。由負責人逐行審查取代簽章；`main` 上的 `law-check (post-merge)` 預期會判定未通過 | #33 | 不適用：免費私有 repo 的必要檢查不會強制執行，紅燈下直接合併 | #28：用備援金鑰（`SHA256:8hyCboa8…`）簽章，`law-check` 通過（[執行紀錄](https://github.com/tsengeagle/quality-inspection/actions/runs/43734454100/job/132873843916)）；合併後 `main` 的 `law-check (post-merge)` 也通過（[執行紀錄](https://github.com/tsengeagle/quality-inspection/actions/runs/43735242499)） |
