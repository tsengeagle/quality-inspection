# frontend-init-prompt.md

請根據我提供的 `.ai/bootstrap/frontend-bootstrap.yaml`，初始化此 repository 中 frontend implementation area 的 coding agent 治理文件與最小必要工程入口。

這是一個 **frontend implementation area init** 任務。  
你的目標不是直接完成所有頁面，也不是重新初始化 solution-level repo，而是要根據：

1. `.ai/bootstrap/frontend-bootstrap.yaml`
2. repository 實際結構
3. `frontends/quality-inspection-frontend/README.md`
4. `docs` 下既有 authority 文件
5. frontend implementation area 的 responsibility / assumptions

共同收斂出此 frontend area 的第一版治理與工程入口。

---

## 0. 初始化前的 authority source

開始前，先讀取並整理以下來源。

### A. frontend bootstrap
- `.ai/bootstrap/frontend-bootstrap.yaml`

### B. solution-level / frontend area guide
- `AGENTS.md`
- `frontends/quality-inspection-frontend/README.md`

### C. repository 實際結構
至少盤點：
- `frontends/quality-inspection-frontend`
- 該目錄下是否已有：
  - source tree
  - package/build files
  - test files
  - route/page structure evidence
  - component / services / guards / state evidence

### D. docs authority

#### requirements
- `docs/01-requirements/0_品質審查系統規格草案.md`
- `docs/01-requirements/decision-log.md`
- `docs/01-requirements/glossary.md`

#### ddd reference
- `docs/02-ddd/domain-model.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/02-ddd/application-service.md`

#### contracts
- `docs/03-contracts/api-spec.md`

#### testing
- `docs/04-testing/specification-by-example.md`
- `docs/04-testing/test-scenarios.md`
- `docs/04-testing/test-scenario-template-and-guidelines.md`

#### ui authority
- `docs/05-ui/ui-information-architecture.md`
- `docs/05-ui/ui-page-inventory.md`
- 各頁 page spec

### E. archive
- `docs/99-archive/*`
只作歷史參考，不作現行 authority。

### F. 資訊不足時的處理
若有任何事無法從 bootstrap、repo 或 docs authority 證實，必須：
- 標示為 `assumption`
- 或標示為 `pending confirmation`
- 寫入前端治理輸出或 project context
- 在 summary 中揭露

不得自行補造已確認事實。

---

## 1. 本次任務的定位

這次是 **frontend area init**，不是 solution re-init，也不是 backend init，更不是完整 UI 實作。

你要做的是先建立：

- frontend area 的治理入口
- frontend-specific project context 補充
- frontend path-specific instructions【若 evidence 足夠】
- 最小必要工程入口【若任務明確要求且 evidence 足夠】

你現在 **不應該**：

- 重寫 solution-level 治理文件
- 把所有 UI page spec 直接翻成完整畫面實作
- 把尚未證實的 framework / build tool / test tool 寫成 confirmed facts
- 讓 frontend init 取代 permission / state / API authority

---

## 2. 開始前先判斷的內容

在輸出任何文件前，先明確整理：

1. frontend module root 是否為 `frontends/quality-inspection-frontend`
2. module 是否已有實際 source tree
3. 是否已有 package/build files
4. 是否已有 page / feature / component structure evidence
5. 是否已有 test source root
6. frontend framework 是否可確認
7. build / test / run / lint / static analysis / CI gate 是否可確認
8. 是否已有 design system / component library 接入 evidence

若 evidence 不足，不得把它們寫成 confirmed facts。

---

## 3. frontend init 至少應產出的內容

### 3.1 `ai/project-context.yaml`【若 solution-level 尚未建立或需最小必要補充】
角色：
- repository 內第一優先的 authoritative project context
- 若 solution-level project context 已存在，則本次只做 frontend 相關最小必要補充
- 必須反映：
  - frontend module root
  - frontend authority chain
  - frontend tech stack confirmation status
  - gate inventory confirmation status
  - frontend assumptions / pending confirmations

### 3.2 frontend path-specific instructions【僅在 evidence 足夠時】
僅當 repo evidence 足以支持 frontend-specific instructions 時才產出。
各 coding agent 有不同的 path-specific 機制：

| Agent | Frontend path-specific entry |
|---|---|
| Claude Code | `frontends/quality-inspection-frontend/CLAUDE.md` |
| GitHub Copilot | `.github/instructions/*frontend*.instructions.md` |
| Cursor | `.cursor/rules/*frontend*.mdc` |

但若技術棧未確認，不得假裝已有 framework-specific instructions。

### 3.3 frontend 最小工程入口【僅在任務明確要求時】
若本次任務明確要求建立 frontend skeleton，可建立最小必要入口，例如：

- package/build file
- source root
- page root
- test root
- route skeleton

但前提是：
- 不與 docs authority 衝突
- 不把未確認的 stack 寫成已確認
- 若 stack 仍未確認，應先保留 minimal neutral skeleton 或揭露 pending confirmation

---

## 4. frontend 文件分工規則

請依下列原則決定規則落點：

- repository-wide hard rules (canonical) -> `.ai/rules/repo-hard-rules.md`
- per-agent repo-level instructions -> `CLAUDE.md` / `.github/copilot-instructions.md` / etc.
- repo operating guide -> `AGENTS.md`
- frontend implementation area guide -> `frontends/quality-inspection-frontend/README.md`
- project authority context -> `.ai/project-context.yaml`
- frontend path-specific instructions -> 各 agent 對應機制【僅在 evidence 足夠時】

以下內容 **不得** 錯放：

- 詳細 state machine -> 不進 frontend README
- 詳細 API schema -> 不進 frontend README
- UI page spec 詳細內容 -> 不進 generic frontend instructions 當唯一 authority
- archive 歷史稿 -> 不得當現行 frontend authority
- 未確認的 framework -> 不得進 framework-specific instructions

---

## 5. frontend authority handling rules

frontend area 必須以以下 authority chain 為準：

1. `decision-log.md`
2. `0_品質審查系統規格草案.md`
3. `glossary.md`
4. `permission-matrix.md`
5. `state-machine.md`
6. `api-spec.md`
7. `05-ui/ui-information-architecture.md`
8. `05-ui/ui-page-inventory.md`
9. 各頁 `ui-page-spec-*.md`
10. `specification-by-example.md`
11. `test-scenarios.md`
12. `99-archive/*`【historical reference only】

你不得：

- 讓頁面互動重新定義 terminal state
- 讓 UI convenience 放寬 role/scope rules
- 讓本地 component state 覆蓋 server truth
- 讓 archive 取代現行 design truth

---

## 6. frontend implementation handling rules

本次 frontend init 已知 module root 為：

- `frontends/quality-inspection-frontend`

但目前不代表以下事項已自動成立：

- frontend framework 已確認
- build tool / package manager 已確認
- test tool 已確認
- lint/static-analysis/ci 已確認
- actual source layout 已確認
- design system implementation 已確認

因此：

### 可以確認的事
- frontend implementation area 應位於該 path
- 後續 frontend source / build / test / route / page 應以該 path 為根
- frontend bootstrap 與 README 已提供 responsibility baseline

### 不可假裝已確認的事
- framework-specific code skeleton
- exact route strategy
- exact state management library
- exact build/test commands
- exact component library

---

## 7. 執行步驟

### Step 1. Frontend Module Inventory
盤點：
- module root
- README
- source tree
- package/build files
- route/page structure
- component / service / guard evidence
- missing governance / missing engineering entrypoints

### Step 2. Authority Source Summary
整理：
- frontend bootstrap 是否存在
- docs authority 是否完整可讀
- frontend README 是否存在
- 哪些來源已確認
- 哪些來源 partial / pending confirmation

### Step 3. Placement Decision
先說明：
- 哪些內容應進 `ai/project-context.yaml`
- 哪些內容應進 frontend-specific instructions
- 哪些內容暫不應產出
- 若要建立工程入口，為何這樣放置

### Step 4. Minimal Frontend Init Output
執行最小必要初始化：
- 優先 patch 既有治理入口
- 優先尊重 docs authority
- 不重複整套 solution init
- 不把 init 擴張成完整畫面實作

### Step 5. Result Summary
最後輸出：
1. 新增 / 更新了哪些文件
2. frontend governance 角色如何分工
3. 使用了哪些 authority sources
4. frontend assumptions / pending confirmations
5. 哪些 build/test/gate 尚未驗證
6. 哪些工作留待下一階段

---

## 8. 輸出格式要求

請至少輸出：

### A. Authority Source Summary
### B. Frontend Module Inventory
### C. Placement Decision
### D. Files Created / Updated
### E. Assumptions / Pending Confirmations
### F. Minimal Next Step Suggestion

---

## 9. Completion Rules

你不得宣稱：

- frontend framework 已確認
- build tool / package manager 已確認
- test runner 已確認
- lint / static analysis / CI gate 已完整確認
- component library / state management 已完整確認
- frontend source layout 已完整確認

除非有足夠 evidence 支持。

若存在以下情況，必須明確揭露：

- module path 已知，但 source tree 尚未落檔
- build/test/run/lint/static-analysis/ci 入口未確認
- frontend instructions 因 evidence 不足而尚未產出
- 某些工程骨架因 stack 未確認而暫不建立
- repository name 或 governance baseline 仍待確認