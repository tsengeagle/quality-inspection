請執行一個 **portal51-lib version evidence layering patch** 任務。

## Goal

只修正 `portal51-lib` 版本證據的表述分層，不做任何其他重構或治理調整。

目前需要修正的是：

- 不應再寫成「沒有 external/current organization MFE module evidence」
- 應明確區分：
  1. `quality-inspection` repo-local dependency evidence
  2. existing organization MFE module evidence
- 並正確表達兩者之間的關係

---

## Evidence scope

請只使用 **repo 內可讀到的本地檔案** 作為 evidence。

### Repo-local evidence
優先檢查：
- `frontends/quality-inspection-frontend/package.json`
- `frontends/quality-inspection-frontend/package-lock.json`（若存在）

### External/current organization MFE module evidence
若以下檔案存在，再使用：
- `docs/mfe-evidence/thms-frontend.package.json`
- `docs/mfe-evidence/thms-frontend.package-lock.json`

若這兩份 external evidence 檔案不存在：
- 不要自行宣稱有 external evidence
- 請在 summary 中明確標示為 `pending confirmation`

---

## Required tasks

### Task A — Correct evidence layering wording

請修正所有相關 summary / wording，使其明確表達：

1. `quality-inspection` repo-local dependency evidence currently shows what version
2. external/current organization MFE module evidence shows what version, **only if local evidence files actually exist in this repo**
3. 因此：
   - repo-local version 可保留為 **this repo current local dependency declaration**
   - 但不可自動寫成 current organization-wide baseline fact
   - 若 external evidence 顯示較新版本，應寫成：
     - current organization module evidence suggests a newer version may already exist
     - whether `quality-inspection` should adopt that newer version remains pending confirmation

---

### Task B — Update summary wording only where needed

請以最小必要修改更新下列文件中與此議題相關的段落：

- `frontend-governance-consistency-cleanup-summary.md`

如有必要，再更新：
- `.ai/bootstrap/frontend-bootstrap.yaml`
- `.ai/project-context.yaml`
- `AGENTS.md`

規則：

- 不要改結構
- 不要修改 governance chain
- 不要 invent 新版本
- 不要把 external evidence 直接宣稱為本 repo 已採用版本
- 只修正 evidence layering 與 wording precision

---

## Required output

請產出：

`portal51-lib-version-evidence-patch-summary.md`

---

## `portal51-lib-version-evidence-patch-summary.md` required structure

# portal51-lib-version-evidence-patch-summary.md

## 1. Purpose
## 2. Repo-local Evidence
## 3. External Organization Module Evidence
## 4. Corrected Wording Applied
## 5. Files Updated
## 6. Remaining Pending Confirmation

---

## Constraints

- 只能使用 repo 內可讀到的檔案作為 evidence
- 若 external evidence 檔案不存在，必須明確標示 unavailable / pending confirmation
- 不要引用 chat 對話中看不到的 uploaded files
- 不要做本任務範圍外的 incidental refactor

---

## Final requirement

最終你必須明確回答：

1. `quality-inspection` repo-local dependency evidence 顯示什麼版本
2. external/current organization MFE module evidence 顯示什麼版本；若 repo 內無對應 evidence 檔案，請明說 unavailable
3. 為什麼 repo-local version 不能再被寫成 current organization-wide baseline fact
4. 目前最正確的 repo wording 應該是什麼
