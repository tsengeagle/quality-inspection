# Repository Hard Rules

本文件是 `quality-inspection` repository 的 **canonical hard rules source**。
所有 coding agent 的 repo-level instructions 必須以本文件為硬規則真相來源。

---

## 1. Document Authority Precedence

本 repo 的文件優先順序如下。遇到衝突時，排序靠前者優先：

1. `docs/01-requirements/decision-log.md`
2. `docs/01-requirements/0_品質審查系統規格草案.md`
3. `docs/01-requirements/glossary.md`
4. `docs/02-ddd/*`
5. `docs/03-contracts/*`
6. `docs/04-testing/*`
7. `docs/05-ui/*`
8. `docs/99-archive/*` — 僅供歷史參考，不作現行 authority

## 2. Change Placement Discipline

修改前必須先確認正確落點。不得把規則寫在錯誤的層級：

- 需求與決策 -> `docs/01-requirements/`
- Domain model / state machine / permission / events -> `docs/02-ddd/`
- API contract / persistence model -> `docs/03-contracts/`
- Testing examples and scenarios -> `docs/04-testing/`
- UI IA / page spec -> `docs/05-ui/`

不得：
- 在 controller / DTO 中重新定義 domain rule
- 在 DB schema 中反向決定 aggregate invariant
- 在 UI component 中覆蓋 permission / state machine truth
- 在 archive 中維護現行規則

## 3. Backward Compatibility

修改既有 API contract、persistence schema、或 domain event 結構時，
必須優先考慮向後相容。若必須 breaking change，先更新 `decision-log.md`。

## 4. No Incidental Refactor

修改範圍限定在任務需求內。不得順手重構周邊程式碼、新增未要求的 docstring、
或改動不相關的檔案格式。

## 5. Cannot Verify, Cannot Claim Done

若 repo 內尚未出現實際 source code、build script、test runner、CI pipeline、lint config，
不得宣稱相關 gate 已確認或已通過。

未確認事項必須標記為 `assumption` 或 `pending_confirmation`。

## 6. Archive Is Not Authority

`docs/99-archive/` 僅供歷史脈絡追溯。若 archive 與現行文件衝突，現行文件優先。
除非現行文件明確引用 archive 內容。

## 7. Specification-by-Example vs Test Scenarios

兩者分工不可混淆：
- `specification-by-example.md` — 固定共同理解與規則例子
- `test-scenarios.md` — 轉成可驗證情境與覆蓋

## 8. Escalation Before Implementation

遇到以下情況，先停在文件層處理，不直接實作：
- 核心規則要變（threshold / terminal state / revision policy / sampling scope）
- 文件 authority 之間衝突
- UI 規格與 domain / API 脫鉤
- testing 出現無對應 example / decision 的高風險規則

## 9. Solution-Level Completion Format

每次變更完成後，應提供：
1. Authority sources used
2. Files created / updated
3. Placement decision rationale
4. Assumptions / pending confirmations
5. Unverified items disclosure
