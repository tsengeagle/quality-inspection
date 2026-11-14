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
| Require status checks to pass | 開，加入 `check`（`backend-check` workflow 的 job 名稱） | P1、P9 |
| Require branches to be up to date before merging | 開 | P1 |
| Block force pushes | 開 | P4 |
| Bypass list（ruleset）／Do not allow bypassing the above settings（舊版規則） | 不放任何人／勾選 | P6、P9 |

這個名稱要等 workflow 在 GitHub 上跑過才會出現在可選清單裡；PR #6 已經跑過一次。

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
