# GitHub 端要手動做的設定

`.github/CODEOWNERS` 和 `.github/workflows/backend-check.yml` 已在 repo 裡，但兩者本身沒有強制力。
下列設定只有 repo 管理員能在 GitHub 上做。做完之前，P1、P6、P9 都只是「檔案存在」。

## 一、main 的保護規則

位置：Settings → Rules → Rulesets → New branch ruleset（或 Settings → Branches → Add rule）。目標分支：`main`。

| 設定 | 值 | 對應義務 |
|---|---|---|
| Require a pull request before merging | 開 | P6 |
| Require review from Code Owners | 開 | P6 |
| Required approvals | 1 | P6 |
| Require status checks to pass | 開，加入 `gradle check` | P1、P9 |
| Require branches to be up to date before merging | 開 | P1 |
| Block force pushes | 開 | P4 |
| Bypass list（ruleset）／Do not allow bypassing the above settings（舊版規則） | 不放任何人／勾選 | P6、P9 |

`gradle check` 這個名稱要等 workflow 在 GitHub 上至少跑過一次才會出現在可選清單裡。

## 二、單人 repo 的限制

GitHub 不允許 PR 的作者核准自己的 PR。目前 CODEOWNERS 只有一個人，所以：

- AI 或其他帳號開的 PR：負責人核准後才能合併。這是 P6 要的效果。
- 負責人自己開的 PR：沒有人能核准。只能把自己放進 bypass 清單，或再加一位 code owner。

放進 bypass 清單代表負責人自己的修法不受機制約束，要在義務對照表上照實記為 ⚠️。

## 三、設定完成後的驗收

對應 `04-definition-of-enough.md` 的及格線第 1、2 項，各做一次並把 PR 連結記到 `05-current-state.md`：

1. 開一個 PR，在 `interfaces` 層直接引用 `infrastructure` 的類別。預期：`gradle check` 紅燈，無法合併。
2. 用非負責人的身分開一個 PR 修改 `decision-log.md`。預期：顯示需要 code owner 核准，無法合併。
