請執行一個 **frontend governance final convergence under current repo structure** 任務。

## Goal

本 repo 的前端治理檔案結構已經建立完成，現在要做的是 **最終內容收斂**，不是再做檔案搬遷或重跑初始化。

本次任務的目標是：

1. 以目前已定案的前端規則組作為 **frontend governance authority input**
2. 回頭檢視並收斂既有 `.ai/bootstrap/frontend-bootstrap.yaml`
3. 將前端規則組融合進目前 repo 已存在的 AI 規則體系
4. 驗證並修補 `.ai/governance/` 的治理鏈與 authority linkage
5. 更新 repo-level agent entry chain，讓 frontend work 能正確走進新的治理層
6. 正規化所有與本 repo 相關的 `ai/...` 舊路徑引用為 `.ai/...`

---

## Current structure to preserve

你必須保留以下結構，不得重組：

```text
.ai/
  project-context.yaml
  bootstrap/
    backend-bootstrap.yaml
    frontend-bootstrap.yaml
    solution-bootstrap.yaml
  governance/
    authority-sources.md
    frontend-back-merge-backlog.md
    frontend-design-language-mfe.md
    frontend-mfe-decisions-r1.md
    frontend-mfe-governance.md
    frontend-ui.instructions.md
    repo-governance-instruction.md
  prompts/
    backend-init-prompt.md
    frontend-governance-convergence.prompt.md
    frontend-init-prompt.md
    solution-init-prompt.md
  rules/
    repo-hard-rules.md

docs/
  01-requirements/
  02-ddd/
  03-contracts/
  04-testing/
  05-ui/
  99-archive/
  design/
    DESIGN.v2.1.md
    DESIGN-components.v2.1.md
    DESIGN-engineering.v2.1.md
```

---

## What this task is NOT

本次任務不是：

* 重做 bootstrap
* 重跑 init
* 重建治理結構
* 建立第二套 `ai/`
* 用新前端規則組取代 frontend-bootstrap
* 把 broader baseline 變成 repo governance
* 回補 `frontend-back-merge-backlog.md` 中的 broader baseline 衝突
* invent 新 frontend 規則

---

## Authority intent

請以以下原則作為本次收斂基準：

### Repo-wide hard rules

* `.ai/rules/repo-hard-rules.md` = canonical hard rules source

### Project context

* `.ai/project-context.yaml` = authoritative project context

### Bootstrap layer

* `.ai/bootstrap/*.yaml` = bootstrap / initialization context
* bootstrap 必須保留
* 但 `.ai/bootstrap/frontend-bootstrap.yaml` 不應繼續保留過時、過強、未證實、或已被新前端規則組收斂的 frontend authority 說法

### Governance layer

* `.ai/governance/repo-governance-instruction.md` = repo-level governance routing entry
* `.ai/governance/frontend-ui.instructions.md` = frontend operational entrypoint
* `.ai/governance/frontend-mfe-governance.md` = frontend MFE execution governance authority
* `.ai/governance/frontend-design-language-mfe.md` = frontend MFE design language authority
* `.ai/governance/frontend-mfe-decisions-r1.md` = frontend MFE decision log
* `.ai/governance/authority-sources.md` = authority index
* `.ai/governance/frontend-back-merge-backlog.md` = governance follow-up backlog for true conflicts only

### Broader baseline

* `docs/design/*` = broader design baseline references
* 不是 repo-governance wrapper
* 不是 solution page spec
* 應由 `.ai/governance/authority-sources.md` 引用

### Solution docs authority

* `docs/01-*` ~ `docs/05-ui/*` 仍維持 solution/domain/contracts/testing/ui authority
* `docs/05-ui/*` 不是 AI governance 容器

---

## Input authority sources

請以以下文件作為前端治理 authority input：

### Frontend governance package

* `.ai/governance/frontend-ui.instructions.md`
* `.ai/governance/frontend-mfe-governance.md`
* `.ai/governance/frontend-design-language-mfe.md`
* `.ai/governance/frontend-mfe-decisions-r1.md`
* `.ai/governance/authority-sources.md`
* `.ai/governance/frontend-back-merge-backlog.md`

### Existing repo AI chain

* `.ai/project-context.yaml`
* `.ai/bootstrap/frontend-bootstrap.yaml`
* `.ai/bootstrap/backend-bootstrap.yaml`
* `.ai/bootstrap/solution-bootstrap.yaml`
* `.ai/rules/repo-hard-rules.md`
* `AGENTS.md`
* `CLAUDE.md`

### Broader baseline references

* `docs/design/DESIGN.v2.1.md`
* `docs/design/DESIGN-components.v2.1.md`
* `docs/design/DESIGN-engineering.v2.1.md`

### Solution/UI references when needed

* `docs/05-ui/*`

---

## Required tasks

### Task A — Governance chain validation

檢查 `.ai/governance/*` 是否形成清楚且一致的治理鏈：

* `repo-governance-instruction.md`
  → `frontend-ui.instructions.md`
  → deeper frontend authorities
* `authority-sources.md`
  → `docs/design/*` broader baseline references
* `frontend-back-merge-backlog.md` 只承載 true conflicts
* decision log 不變成 operational spec
* `frontend-ui.instructions.md` 保持短、直接、可執行

若有不一致、重複過度、authority linkage 不清、或舊路徑殘留，做最小必要修正。

---

### Task B — Frontend bootstrap reconciliation

檢查 `.ai/bootstrap/frontend-bootstrap.yaml`，並依新前端規則組進行最小必要收斂：

1. 保留仍然有效的 bootstrap context
2. 修正已過時、過強、或不夠精準的 frontend authority 說法
3. 將應為 pending confirmation 的內容降級
4. 不把 bootstrap 改寫成日常治理文件
5. 不刪掉 bootstrap 角色本身

特別注意：

* 不要讓 `frontend-bootstrap.yaml` 與 `.ai/governance/*` 發生 authority 競爭
* 不要讓過時的 `portal51-lib` 描述壓過新前端規則組
* 不要直接刪掉有價值的 context；優先修 wording、降強度、改狀態

---

### Task C — Repo AI entrypoint integration

檢查並在必要時最小修正：

* `AGENTS.md`
* `CLAUDE.md`

目標：

1. frontend 任務能被導向 `.ai/governance/repo-governance-instruction.md`
2. frontend 任務不再只依賴 `docs/05-ui` 或 bootstrap 推論 implementation 規則
3. 既有 solution-level / backend-oriented operating guide 不被破壞
4. frontend governance layer 被納入既有 repo AI 規則體系

---

### Task D — Project context alignment

檢查 `.ai/project-context.yaml` 是否需要最小必要補充：

* 新 frontend governance layer 的存在
* frontend governance package 的角色
* bootstrap 與 governance 的分工

注意：

* `.ai/project-context.yaml` 仍應保持 solution-level authoritative project context
* 不要把它改寫成 frontend 規則手冊
* 只補必要的 governance awareness
* 若其中 frontend 描述與新前端規則組衝突，應做最小必要修正或降級為 pending confirmation

---

### Task E — Broader baseline reference sanity check

確認：

* `docs/design/*` 仍被當作 broader baseline reference
* 不被錯誤當成 repo governance layer
* 與 `.ai/governance/authority-sources.md` 的引用一致
* 不與 `docs/05-ui/*` 角色混淆

不要在本輪處理 `frontend-back-merge-backlog.md` 的實際回補。
不要修改 broader baseline 規則內容，除非只是修正明顯錯誤的 repo-relative path / reference。

---

### Task F — Path normalization for `.ai/`

This repository uses `.ai/` as the governance root, not `ai/`.

你必須檢查所有與本次任務相關的文件，並正規化舊的 `ai/...` 路徑引用。

至少檢查並修正以下檔案中的路徑引用：

* `.ai/governance/*`
* `.ai/bootstrap/frontend-bootstrap.yaml`
* `.ai/project-context.yaml`
* `AGENTS.md`
* `CLAUDE.md`

規則如下：

1. 當路徑明確是指向本 repo 的治理結構時，將過時的 `ai/...` 改為正確的 `.ai/...`
2. 若路徑是外部範例、歷史參考、或明確不是本 repo 路徑，不要強制改寫
3. 若某路徑語意不清，保留原樣並在 summary 中標記為 `pending confirmation`
4. 所有路徑正規化結果都必須記錄在最終 summary 中

---

## Required outputs

請至少產出以下兩份 summary：

1. `frontend-governance-final-review.md`
2. `frontend-bootstrap-reconciliation-summary.md`

並直接對需要的既有檔案做最小必要修改。

---

## `frontend-governance-final-review.md` required structure

# frontend-governance-final-review.md

## 1. Purpose

## 2. Current Governance Chain Observed

## 3. Governance Linkage Validation Result

## 4. Repo Entrypoint Integration Result

## 5. Project Context Alignment Result

## 6. Broader Baseline Reference Handling

## 7. Path Reference Normalization

## 8. Files Updated

## 9. Remaining Gaps or Pending Confirmation

## 10. Final Adoption Readiness

---

## `frontend-bootstrap-reconciliation-summary.md` required structure

# frontend-bootstrap-reconciliation-summary.md

## 1. Purpose

## 2. Bootstrap Content Preserved

## 3. Bootstrap Content Refined by Governance

## 4. Bootstrap Claims Downgraded to Pending Confirmation

## 5. Wording Changes Applied

## 6. Remaining Bootstrap Uncertainties

---

## Specific convergence requirements

你必須明確處理以下問題：

### 1. frontend-bootstrap is not removed

它仍保留 bootstrap/context 角色。

### 2. frontend governance package is the active frontend governance layer

日常 frontend work 應走：

* `.ai/governance/repo-governance-instruction.md`
* `.ai/governance/frontend-ui.instructions.md`
* deeper frontend governance files

### 3. hard rules and project context remain intact

* `.ai/rules/repo-hard-rules.md` 仍是 canonical hard rules
* `.ai/project-context.yaml` 仍是 authoritative project context

### 4. docs/05-ui remains solution UI authority

不要把 frontend governance 文件塞進 `docs/05-ui`
不要讓 `docs/design/*` 與 `docs/05-ui/*` 角色混淆

### 5. docs/design remains broader baseline reference

不要把它升級成 repo governance layer
不要本輪回補 backlog

### 6. path references must match `.ai/`

本 repo 內所有相關治理引用，應以 `.ai/` 為準，不得殘留錯誤的 `ai/` 路徑引用

---

## Constraints

* 不要改目錄結構
* 不要重做 bootstrap
* 不要重跑 init
* 不要 invent 新 frontend 規則
* 不要把 broader baseline 改成 solution UI spec
* 不要順便處理 backlog back-merge
* 只做最小必要內容收斂、authority linkage 修補、entrypoint wiring、與 `.ai` 路徑正規化

---

## Final requirement

最終你必須明確回答：

1. 目前這個 repo 的 frontend governance chain 是否已閉合
2. `frontend-bootstrap.yaml` 是否已被正確收斂為 bootstrap/context，而不是日常 authority
3. `AGENTS.md` / `CLAUDE.md` 是否已足夠把 frontend 任務導進新的 governance layer
4. `docs/design/*` 的角色是否已清楚維持為 broader baseline reference
5. 與本 repo 有關的 `ai/...` 舊路徑是否已正規化為 `.ai/...`
6. 現在是否已可正式採用這套前端治理結構
