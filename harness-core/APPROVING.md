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

**核准工具一定要從 `main` 執行，不要簽出修法的分支再執行。** 修法的 PR 可以改到核准工具本身；
被改過的版本可以顯示一個雜湊，卻讓金鑰簽另一個。所以整個流程中，工作目錄都停在 `main`，
工具只透過 git 讀取修法分支的內容，不執行其中任何檔案。工作目錄停在修法的分支上時，工具會拒絕執行。

1. 讓工作目錄停在最新的 `main`，並取得修法的分支：

   ```bash
   git switch main
   git pull
   git fetch origin
   ```

2. **看過修改。** 簽章代表你同意這些內容。這個指令只顯示差異，不會執行分支上的任何東西：

   ```bash
   git diff origin/main...origin/<修法的分支>
   ```

   PR 上 `law-check` 的報告也會列出動到哪些律法檔案，以及內容雜湊。

3. 簽章（插著金鑰，過程中要碰一下）：

   ```bash
   python3 -I harness-core/law_approve.py --head origin/<修法的分支> --signer <核准者名稱> --key ~/.ssh/law_amendment_sk
   ```

   工具會印出核准的是哪個 commit、動到哪些律法檔案、內容雜湊，然後呼叫 `ssh-keygen` 簽章。
   **核對它印出的內容雜湊和 PR 上 `law-check` 報告裡的相同**，再碰金鑰。
   核准檔會寫到 `.git/law-approvals/`，檔名是 `<修法紀錄的 id>.<核准者名稱>.approval.json`。

4. 把核准檔放進修法的分支並推送。工具會印出確切的指令，形式如下：

   ```bash
   git switch <修法的分支>
   cp .git/law-approvals/<核准檔> law/amendments/
   git add law/amendments/ && git commit -m "approve: <修法紀錄的 id>" && git push
   git switch main
   ```

   這一步簽出了修法的分支，但沒有執行它的任何程式。

5. PR 上的 `law-check` 重跑，報告會出現「核准者：<你的名稱>（ssh）」。

## 五、要知道的事

- **簽的是內容。** 簽完之後，修法紀錄或任何一個律法檔案再改一個字，簽章就失效，要回到第四節重簽。PR 上的報告會寫「簽的是另一份內容」。
- **加上核准檔不會讓簽章失效。** 內容雜湊不包含核准檔。
- **只把基線縮短的修改不需要核准**，也不會讓已有的簽章失效。
- **核准者清單讀的是合併目標分支上的版本。** PR 自己把別的金鑰加進清單，不會讓那把金鑰的簽章生效。
- **機制只能保證「有人用那把金鑰同意」，不能保證「有人看懂了」。** 第四節的第 2 步不能省。
- **`python3 -I` 是隔離模式**：不讀環境變數與使用者目錄裡的 Python 設定，避免被動手腳。

## 六、金鑰遺失或更換

| 情況 | 做法 |
|---|---|
| 還有另一把登記過的金鑰 | 用它核准一筆修法，把遺失的那把從核准者清單移除、加入新的 |
| 所有登記過的金鑰都不能用了 | 沒有人能核准。只能依專案的處置程序暫時關閉檢查來更換核准者清單，並留下紀錄。這正是要準備備援金鑰的原因 |
