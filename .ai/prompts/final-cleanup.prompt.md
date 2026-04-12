請執行一個 **frontend governance entrypoint cleanup** 任務。

## Goal

只做兩個最小修補，不做其他重構：

1. 修補 `DESIGN.md`
   - 將「一般 design baseline 閱讀入口」與「frontend MFE implementation 入口」分流說清楚
2. 檢查並修補 root-level `frontend-ui.instructions.md`
   - 確保它只是 compatibility wrapper
   - 不與 `ai/governance/frontend-ui.instructions.md` 形成雙入口混淆

---

## Scope

請只處理以下檔案：

- `DESIGN.md`
- root-level `frontend-ui.instructions.md`
- 如有必要可參照：
  - `ai/governance/repo-governance-instruction.md`
  - `ai/governance/frontend-ui.instructions.md`
  - `ai/governance/authority-sources.md`

---

## Requirements

### A. `DESIGN.md`
請讓它清楚表達：

- 若是一般設計/版型/基線閱讀，從 `docs/design/DESIGN.v2.1.md` 開始
- 若是 frontend MFE implementation work，從 `ai/governance/repo-governance-instruction.md` 開始
- 不要讓 agent 先誤進 broader baseline，再忽略 MFE governance package

### B. root-level `frontend-ui.instructions.md`
請讓它成為明確的 compatibility wrapper：

- 清楚說明正式 operational entrypoint 在 `ai/governance/frontend-ui.instructions.md`
- 本檔不是正式 authority
- 本檔只負責把舊入口導向新入口
- 內容要短，不要再複製完整規則

---

## Output expectation

直接更新上述檔案即可。

---

## Constraints

- 不要改動其他治理文件
- 不要重寫 broader baseline
- 不要新增新規則
- 只做入口澄清與 wrapper 定位修補
