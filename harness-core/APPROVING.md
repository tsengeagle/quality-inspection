# 核准者指引：用實體金鑰核准修法

這份文件給核准者（律法的負責人）看：怎麼準備一把只用來核准修法的金鑰，以及每次核准怎麼做。

所有步驟都在核准者**自己的電腦**上做。私鑰留在實體金鑰裡，取不出來；AI 工具和 CI 都碰不到它。

## 一、準備

| 需要 | 說明 |
|---|---|
| 支援 FIDO2 的實體金鑰 | 例如 YubiKey 5 系列。`ed25519-sk` 需要韌體 5.2.3 以上；較舊的改用 `ecdsa-sk` |
| OpenSSH 8.2 以上，且支援 FIDO | 用下面的指令確認 |
| Python 3.8 以上、git | 執行核准工具用 |

確認 OpenSSH 能不能用實體金鑰：

```bash
ssh -V
ssh-keygen -t ed25519-sk -f /tmp/sk-probe -N ""    # 插著金鑰執行；成功的話會要你碰一下金鑰
rm -f /tmp/sk-probe /tmp/sk-probe.pub
```

如果出現 `No FIDO SecurityKeyProvider specified` 或 `unknown key type`，代表這個 OpenSSH 沒有內建 FIDO 支援。
macOS 內建的版本常見這個情況，改裝 Homebrew 的版本：

```bash
brew install openssh
which ssh-keygen      # 應該指向 Homebrew 的路徑；若不是，開一個新的終端機視窗再試
```

### Windows 注意事項

Windows 上用 Win32-OpenSSH。官方的 [FIDO/U2F 說明](https://github.com/PowerShell/Win32-OpenSSH/wiki/FIDO---U2F-usage)寫明 **8.9.0.0 以上**才支援實體金鑰；Windows 內建的版本（`C:\Windows\System32\OpenSSH\`）可能低於這個門檻。是否可用以下面的探測指令成功為準。探測失敗、或 `ssh -V` 低於 8.9 時，依官方的[安裝說明](https://github.com/PowerShell/Win32-OpenSSH/wiki/Install-Win32-OpenSSH)用 `winget install "openssh preview"` 安裝新版，開一個新的視窗再探測一次。官方說明的範例只用 `ecdsa-sk`；`ed25519-sk` 已於 2026-11-18 在負責人的 Windows 上實測可用，探測失敗時可改用 `ecdsa-sk`。

Win32-OpenSSH 不直接存取 USB 金鑰，而是透過 Windows 的 WebAuthn，所以每次產生金鑰或簽章都會跳出「Windows 安全性：選擇要儲存金鑰的位置」的視窗。這個視窗是系統的安全機制，關不掉，也不要設法繞過。

| 要注意的事 | 怎麼做 |
|---|---|
| 視窗列出好幾個選項（1Password、此 Windows 裝置、手機、安全性金鑰） | **一定選「安全性金鑰」**，再碰一下金鑰。選其他項目的話，產生的憑證不在實體金鑰裡，不符合這份文件的前提 |
| 選單裡有 1Password 等通行金鑰提供者，容易誤選 | 可以在 Windows 設定 → 帳戶 → 通行金鑰 → 進階選項，或 1Password 自己的設定裡，關閉它作為通行金鑰提供者。不關也可以，只要每次都選對 |
| 文件裡的 `/tmp/...` | Windows 通常沒有這個目錄。改用 `$env:TEMP\...` |
| 文件裡的 `~/.ssh/...` | PowerShell 不一定會替外部程式展開 `~`，核准工具也不會展開。改寫成 `$HOME\.ssh\...` |
| 用到的 `ssh-keygen` 不是探測成功的那一個 | 核准工具呼叫的是 PATH 裡的第一個 `ssh-keygen`。用 `Get-Command ssh-keygen` 看它的路徑，`ssh -V` 看版本（8.9 以上）。裝了 Git for Windows 或另裝新版 OpenSSH 時，PATH 可能先找到別的版本；要確定 PATH 第一個找到的，就是探測成功的那一個 |
| 以系統管理員身分開啟 PowerShell | 不需要。用一般權限的視窗即可 |

確認能不能用實體金鑰（PowerShell）：

```powershell
ssh -V
ssh-keygen -t ed25519-sk -f "$env:TEMP\sk-probe"    # 跳出視窗時選「安全性金鑰」，碰一下金鑰；問密碼時直接按 Enter
Remove-Item "$env:TEMP\sk-probe*"
```

第二節以後的指令，在 PowerShell 裡把 `~/.ssh/` 換成 `$HOME\.ssh\`、`python3` 換成 `python`。`rm -f` 在 PowerShell 裡不能用（`-f` 同時可能是 `-Filter` 或 `-Force`），改用 `Remove-Item`。例如：

```powershell
# 第二節：產生金鑰
ssh-keygen -t ed25519-sk -O application=ssh:law-amendment -C "law-amendment <你的名稱>" -f "$HOME\.ssh\law_amendment_sk"

# 第二節：確認把手檔綁在這把實體金鑰上
"probe" | Out-File -Encoding ascii "$env:TEMP\probe.txt"
ssh-keygen -Y sign -f "$HOME\.ssh\law_amendment_sk" -n law-amendment "$env:TEMP\probe.txt"
Remove-Item -Force -ErrorAction SilentlyContinue -Path "$env:TEMP\probe.txt", "$env:TEMP\probe.txt.sig"

# 第四節：簽章
python -I harness-core/law_approve.py --head origin/$BRANCH --signer <核准者名稱> --key "$HOME\.ssh\law_amendment_sk"
```

## 二、產生核准用的金鑰（只做一次）

```bash
ssh-keygen -t ed25519-sk -O application=ssh:law-amendment -C "law-amendment <你的名稱>" -f ~/.ssh/law_amendment_sk
```

- 過程中要碰一下金鑰。
- 會問要不要設密碼。這是「把手檔」的密碼，可以留空：把手檔沒有實體金鑰就沒有用。
- `application=ssh:law-amendment` 讓這把金鑰和你登入伺服器、簽 commit 用的金鑰分開。
- 想要每次簽章除了觸碰還要輸入金鑰的 PIN，加上 `-O verify-required`（金鑰要先設定 FIDO2 PIN）。

產生兩個檔案：

| 檔案 | 內容 | 可以公開嗎 |
|---|---|---|
| `~/.ssh/law_amendment_sk` | 把手檔。配合實體金鑰才能簽章 | 不要外流，但單獨外流不足以簽章 |
| `~/.ssh/law_amendment_sk.pub` | 公鑰 | 可以，要登記進專案 |

**建議準備第二把實體金鑰當備援**，用同樣的指令產生另一組檔案（檔名換掉），兩把公鑰都登記。只有一把而遺失的話，就沒有人能核准修法，包含「更換核准者」這個修法本身。

產生之後，確認把手檔真的綁在那把實體金鑰上（備援金鑰尤其要做，平常不會用到它，壞了也不會發現）：

```bash
echo probe > /tmp/probe.txt
ssh-keygen -Y sign -f ~/.ssh/law_amendment_sk -n law-amendment /tmp/probe.txt
rm -f /tmp/probe.txt /tmp/probe.txt.sig
```

| 測試 | 預期結果 |
|---|---|
| 插著**這把**金鑰，碰一下 | 簽章成功，產生 `probe.txt.sig` |
| 改插另一把金鑰，或不插金鑰 | 簽章失敗 |

兩項都符合預期才算完成。Windows 上請改用第一節「Windows 注意事項」裡的 PowerShell 版本。

## 三、把公鑰登記進專案（只做一次，之後更換金鑰時再做）

核准者清單是一個文字檔（位置由專案的清冊用 `@approvers-ssh` 指定），一行一把公鑰：

```
<核准者名稱> namespaces="law-amendment" <公鑰檔的內容>
```

例如：

```
tsengeagle namespaces="law-amendment" sk-ssh-ed25519@openssh.com AAAAGnNrLXNzaC1lZDI1NTE5QG9wZW5zc2guY29tAAAA... law-amendment tsengeagle
```

- `namespaces="law-amendment"` 限制這把公鑰只能用來驗修法的簽章。
- 核准者名稱是之後 `--signer` 要填的值。

登記本身是一次修法：改核准者清單與清冊，附修法紀錄，由 PR 合併。**第一次登記時還沒有人能簽章，所以那個 PR 要由你親自核對公鑰是不是你的**（比對 `cat ~/.ssh/law_amendment_sk.pub` 的輸出）。合併之後，之後所有的修法，包含更換核准者，都要現有核准者的簽章。

## 四、每次核准修法

### 先認識四個編號

核准流程裡有四個長得很像、用途不同的編號。以一次實際的修法為例：

| 名稱 | 例子 | 是什麼 | 簽進簽章了嗎 | 誰驗證 |
|---|---|---|---|---|
| **修法紀錄 id** | `2026-11-15-spec-format-v1` | 這次修法的名稱，等於修法紀錄的檔名 | 間接：紀錄的內容算在內容雜湊裡 | 判決程式核對檔名、核准檔的 `amendment` |
| **內容雜湊** | `sha256:a9743bec…` | 修法紀錄，加上每個被改動律法檔案的內容與檔案模式 | **是，簽章只簽這個** | 判決程式重算並驗章 |
| **被核准的 commit**（`reviewed_commit`） | `6806a751268d…` | 你檢查與簽章時，修法分支最新的 commit | 否 | 判決程式驗證三件事：它存在；它在這次修改的歷史裡；它的內容算出來就是簽的那個雜湊 |
| **核准 commit** | `ccdd79c…` | 把核准檔加進分支的那個 commit | 否 | 不驗證。訊息格式是 `approve: <修法紀錄 id> @ <被核准 commit 的前 12 碼>`，由工具印出 |

- **簽的是內容，不是 commit。** 所以加上核准檔不會讓簽章失效：核准 commit 多了核准檔，律法內容沒變。
- **被核准的 commit 記在核准檔裡，由工具自動填入，不用自己寫。** 它不在簽章裡，但判決程式會驗證它。有人把它改掉，也只能改成另一個內容完全相同的 commit，不能用來假造「核准者看過什麼」。
- 核准後若改了律法又改回原樣，簽章仍然有效：最後的內容就是你看過、簽過的那一份。

整個流程分三段：**簽之前確認、簽章、推上核准檔**。下面的指令可以整段照抄，只有第 0 步要填一個值。

### 0. 填入修法的分支名稱

在 PR 頁面標題下方可以看到分支名稱（`... wants to merge ... from <分支名稱>`）。

```bash
BRANCH=<修法的分支名稱>          # bash / zsh
```

```powershell
$BRANCH = "<修法的分支名稱>"      # PowerShell
```

下面的指令都用這個變數。PowerShell 把 `$BRANCH` 照寫即可；`python3` 換成 `python`。

### 1. 簽之前確認（全部打勾才往下）

- [ ] PR 上**除了 `law-check` 以外的檢查都是綠的**。還在跑或有紅的，先不要簽：之後若要修，內容雜湊會變，簽章就作廢。
- [ ] PR 上**自動審查（Codex、Copilot）的意見都處理完了**，沒有未解決的討論串。理由同上。
- [ ] 打開 PR 上 `law-check` 的報告（Checks → law-check），記下「內容雜湊」那一行。下面叫它**報告的雜湊**。

### 2. 簽章

**核准工具一定要從 `main` 執行，不要簽出修法的分支再執行。** 修法的 PR 可以改到核准工具本身；被改過的版本可以顯示一個雜湊，卻讓金鑰簽另一個。工具只透過 git 讀取修法分支的內容，不執行其中任何檔案；工作目錄停在修法的分支上時，工具會拒絕執行。

```bash
git switch main
git pull
git fetch origin
```

**`git fetch origin` 不能省。** 省了的話，你簽到的是電腦上舊的分支內容，不是 PR 上最新的。

看過修改（簽章代表你同意這些內容；這些指令只顯示差異，不執行任何東西）。先看改了哪些檔案，再看內容：

```bash
git --no-pager diff --stat origin/main...origin/$BRANCH
git --no-pager diff origin/main...origin/$BRANCH
```

`--no-pager` 讓輸出直接留在畫面上。不加的話，差異會用分頁器（`less`）開啟，按 `q` 離開後畫面被清掉，事後看不出自己看過什麼。內容很長時，改用 `git diff origin/main...origin/$BRANCH`，用空白鍵翻頁、`q` 離開。

簽章（插著金鑰）：

```bash
python3 -I harness-core/law_approve.py --head origin/$BRANCH --signer <核准者名稱> --key ~/.ssh/law_amendment_sk
```

工具會先印出修法紀錄、動到的律法檔案、**內容雜湊**、核准的 commit，然後等你碰金鑰。

- [ ] **碰金鑰之前**，核對工具印出的內容雜湊和**報告的雜湊**完全相同。
- [ ] 核准的 commit 和 PR 上最新的 commit 相同（PR 的 Commits 分頁，比對前 7 碼）。

不同就**不要碰金鑰**，按 Ctrl+C 中止。通常是第 2 步的 `git fetch origin` 沒有做，或 PR 在你確認之後又有新的推送。

### 3. 推上核准檔

工具簽完會印出填好分支與檔名的指令，形式如下。**直接複製工具印出的那幾行，不要自己打檔名**：`.git/law-approvals/` 裡會累積以前的核准檔，自己打很容易複製到別次的。

```bash
git switch <修法的分支>
git pull
cp "<.git/law-approvals/ 裡這次的核准檔>" "law/amendments/<同一個檔名>"
git add "law/amendments/<同一個檔名>"
git commit -m "approve: <修法紀錄的 id> @ <被核准 commit 的前 12 碼>"
git push
git switch main
```

一行一行執行，每一行都確認沒有錯誤再執行下一行。這一步簽出了修法的分支，但沒有執行它的任何程式。

### 4. 確認

PR 上的 `law-check` 會自動重跑。通過時報告會出現「核准者：<你的名稱>（ssh）」。

### 常見問題

| 看到什麼 | 原因 | 怎麼做 |
|---|---|---|
| `git commit` 顯示 `nothing to commit` | 複製的核准檔和分支上已有的一模一樣，通常是複製到以前的核准檔 | 用工具印出的 `cp` 那一行重新複製 |
| `git push` 顯示 `rejected`、`non-fast-forward` | 本機的修法分支比 PR 舊 | `git pull` 之後再 `git push`。核准檔是新增的檔案，不會衝突 |
| 報告寫「簽的是另一份內容」 | 簽完之後律法又被改過，或簽到的是舊的內容 | 回到第 1 步重簽。同一位核准者重簽會覆蓋 `.git/law-approvals/` 裡的檔案，推上去也是取代 PR 上的那一份 |
| 工具印出的雜湊和報告的不同 | 沒有 `git fetch origin`，或 PR 又有新的推送 | 先不要碰金鑰。重做第 2 步；仍然不同就等 PR 上新的 `law-check` 跑完再核對 |
| 工具說「工作目錄停在修法的分支上」 | 從修法的分支執行了工具 | `git switch main` 再執行 |
| 報告寫「沒有 reviewed_commit」 | 核准檔是 0.3.0 以前的工具產生的 | 在最新的 `main` 上重新核准 |
| 報告寫「reviewed_commit 不在這次修改的歷史裡」 | 簽的是別的分支，或修法分支被改寫過歷史 | 在最新的分支上重新核准 |
| 報告寫「reviewed_commit 的內容不是簽章簽的那一份」 | 核准檔的 `reviewed_commit` 被改過 | 不要手改核准檔；重新核准 |
| `git diff` 什麼都沒顯示 | 分頁器離開後畫面被清掉；或 `$BRANCH` 沒有設定 | 用 `--no-pager`；`echo $BRANCH` 確認變數 |
| 碰了金鑰仍顯示 `invalid format` 或簽章失敗 | 常見是 FIDO2 PIN 輸入錯誤 | 用 `ykman fido info` 看剩餘嘗試次數，確認 PIN 後重試 |
| Windows 上跳出「選擇要儲存金鑰的位置」 | 系統內建的 OpenSSH 透過 Windows 的 WebAuthn 使用金鑰 | 選「安全性金鑰」再碰金鑰。見第一節的「Windows 注意事項」 |
| Windows 上簽章失敗，而 `--key` 寫的是 `~/.ssh/...` | `~` 可能沒有被展開，`ssh-keygen` 找不到把手檔 | 改成 `"$HOME\.ssh\law_amendment_sk"` |

## 五、要知道的事

- **簽的是內容。** 簽完之後，修法紀錄或任何一個律法檔案再改一個字，簽章就失效，要回到第四節重簽。PR 上的報告會寫「簽的是另一份內容」。這就是第四節第 1 步要先等檢查與審查都結束的原因。
- **加上核准檔不會讓簽章失效。** 內容雜湊不包含核准檔。
- **修法 PR 一律用 merge commit 合併**（GitHub 的「Create a merge commit」）。squash 或 rebase 合併會產生新的 commit，被核准的 commit 不會進入 `main` 的歷史，合併後的 `law-check (post-merge)` 會失敗。這是刻意的：放寬這項檢查，就能用直接推送假造「核准者看過哪個 commit」。
- **只把基線縮短的修改不需要核准**，也不會讓已有的簽章失效。
- **核准者清單讀的是合併目標分支上的版本。** PR 自己把別的金鑰加進清單，不會讓那把金鑰的簽章生效。
- **機制只能保證「有人用那把金鑰同意」，不能保證「有人看懂了」。** 第四節第 2 步的 `git diff` 不能省。
- **`python3 -I` 是隔離模式**：不讀環境變數與使用者目錄裡的 Python 設定，避免被動手腳。

## 六、金鑰遺失或更換

| 情況 | 做法 |
|---|---|
| 還有另一把登記過的金鑰 | 用它核准一筆修法，把遺失的那把從核准者清單移除、加入新的 |
| 所有登記過的金鑰都不能用了 | 沒有人能核准。只能依專案的處置程序暫時關閉檢查來更換核准者清單，並留下紀錄。這正是要準備備援金鑰的原因 |
