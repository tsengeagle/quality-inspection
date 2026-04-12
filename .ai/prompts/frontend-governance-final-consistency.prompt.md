請執行一個 **frontend governance final consistency cleanup** 任務。

## Goal

本次任務只做最小必要的內容一致性修補，不做任何結構調整。

只處理以下三類問題：

1. `portal51-lib` 的角色描述強度過高
2. `portal51-lib` 的版本資訊可能過時
3. frontend source tree 的確認狀態在不同文件間不一致

---

## Scope

請只 review 並在必要時最小修改以下文件：

- `.ai/bootstrap/frontend-bootstrap.yaml`
- `.ai/project-context.yaml`
- `AGENTS.md`

如有必要可參照：
- `.ai/governance/frontend-mfe-governance.md`
- `.ai/governance/frontend-design-language-mfe.md`
- `.ai/governance/frontend-ui.instructions.md`
- `frontend-governance-final-review.md`
- `frontend-bootstrap-reconciliation-summary.md`

---

## Required tasks

### Task A — Reconcile `portal51-lib` role wording

請檢查上述文件中所有關於 `portal51-lib` 的描述。

原則：

1. 不要把 `portal51-lib` 寫成過強、過滿的已確認真相
2. 若目前 repo evidence 不足以證明它是完整 shared UI / design-system library，請降低描述強度
3. 優先使用較保守且可驗證的說法，例如：
   - organization frontend baseline reference
   - shared frontend baseline reference
   - implementation usage details remain pending confirmation
4. 保留有 evidence 支持的事實
5. 不要 invent 新角色定義

---

### Task B — Verify or downgrade `portal51-lib` version

請先嘗試從 repo evidence 找出 `portal51-lib` 的實際版本，例如：

- `package.json`
- `package-lock.json`
- `angular.json`
- 任何 frontend module 依賴宣告
- 其他 repo 內明確可驗證的依賴來源

規則：

1. 若 repo evidence 可確認版本，請更新文件中的版本資訊
2. 若 repo evidence 無法確認版本，請不要猜測新版本
3. 若現有版本 `0.0.16` 無法被 repo evidence 支持，請將固定版本降級為：
   - pending confirmation
   - or "version requires repo evidence confirmation"
4. 在 summary 中明確說明：
   - 版本是否被 repo evidence 證實
   - 若未證實，如何降級處理

---

### Task C — Reconcile frontend source tree status

請檢查下列兩份文件是否對 frontend source tree 狀態表述一致：

- `.ai/bootstrap/frontend-bootstrap.yaml`
- `.ai/project-context.yaml`

目前要對齊的是：
- module root 已確認
- skeleton 已存在
- 但 detailed route/page/folder structure 是否 fully confirmed，要用一致語言表達

規則：

1. 不要讓一份寫 `confirmed`，另一份寫 `pending_confirmation` 而沒有解釋
2. 若已能確認 skeleton，但細部結構仍未完全穩定，請使用較精準的分層表述
3. 保持 bootstrap 與 project-context 的角色差異：
   - bootstrap = initialization context
   - project-context = authoritative project context
4. 只做最小必要 wording 收斂

---

## Required output

請產出：

`frontend-governance-consistency-cleanup-summary.md`

---

## `frontend-governance-consistency-cleanup-summary.md` required structure

# frontend-governance-consistency-cleanup-summary.md

## 1. Purpose
## 2. Files Reviewed
## 3. portal51-lib Role Wording Changes
## 4. portal51-lib Version Verification Result
## 5. Frontend Source Tree Status Reconciliation
## 6. Files Updated
## 7. Remaining Pending Confirmation

---

## Constraints

- 不要改目錄結構
- 不要修改 `.ai/governance/*` 的整體治理模型
- 不要 invent `portal51-lib` 的新版本
- 不要在沒有 repo evidence 的情況下強行確認 component-level usage detail
- 不要做本任務範圍外的 incidental refactor

---

## Final requirement

最終你必須明確回答：

1. `portal51-lib` 的角色描述是否已降到合理強度
2. `portal51-lib` 的版本是否被 repo evidence 證實；若否，如何處理
3. frontend source tree 的確認狀態是否已在 bootstrap 與 project-context 之間一致
