# backend-init-prompt.md

請根據我提供的 `.ai/bootstrap/backend-bootstrap.yaml`，初始化此 repository 中 backend implementation area 的 GitHub Copilot / agent mode 治理文件與最小必要工程入口。

這是一個 **backend implementation area init** 任務。  
你的目標不是直接完成整個業務系統，也不是重新初始化 solution-level repo，而是要根據：

1. `.ai/bootstrap/backend-bootstrap.yaml`
2. repository 實際結構
3. `services/quality-inspection-service/README.md`
4. `docs` 下既有 authority 文件
5. backend implementation area 的已知 responsibility / assumptions

共同收斂出此 backend area 的第一版治理與工程入口。

---

## 0. 初始化前的 authority source

開始前，先讀取並整理以下來源。

### A. backend bootstrap
- `.ai/bootstrap/backend-bootstrap.yaml`

### B. solution-level / backend area guide
- `AGENTS.md`
- `services/quality-inspection-service/README.md`

### C. repository 實際結構
至少盤點：
- `services/quality-inspection-service`
- 該目錄下是否已有：
  - source tree
  - build files
  - test files
  - config files
  - migration files
  - package/module structure evidence

### D. docs authority
必須先讀取並辨識以下文件。

#### requirements
- `docs/01-requirements/0_品質審查系統規格草案.md`
- `docs/01-requirements/decision-log.md`
- `docs/01-requirements/glossary.md`

#### ddd
- `docs/02-ddd/context-map.md`
- `docs/02-ddd/domain-model.md`
- `docs/02-ddd/state-machine.md`
- `docs/02-ddd/permission-matrix.md`
- `docs/02-ddd/domain-events.md`
- `docs/02-ddd/application-service.md`

#### contracts
- `docs/03-contracts/api-spec.md`
- `docs/03-contracts/persistence-model.md`

#### testing
- `docs/04-testing/specification-by-example.md`
- `docs/04-testing/test-scenarios.md`
- `docs/04-testing/test-scenario-template-and-guidelines.md`

#### ui reference
- `docs/05-ui/*`
只作 backend query shape、command timing、互動語意參考，  
不是 backend domain / contract 第一 authority。

### E. archive
- `docs/99-archive/*`
只作歷史參考，不作現行 authority，除非現行文件不足且需追溯演進脈絡。

### F. 資訊不足時的處理
若有任何事無法從 bootstrap、repo 或 docs authority 證實，必須：
- 標示為 `assumption`
- 或標示為 `pending confirmation`
- 寫入 backend area 的治理輸出或 project context
- 在 summary 中揭露

不得自行補造已確認事實。

---

## 1. 本次任務的定位

這次是 **backend area init**，不是 solution re-init，也不是 frontend init，更不是完整業務實作。

你要做的是先建立：

- backend area 的治理入口
- backend-specific project context 補充
- backend path-specific instructions【若 evidence 足夠】
- 最小必要工程入口骨架【若任務明確要求且 evidence 足夠】

你現在 **不應該**：

- 重寫 solution-level 治理文件
- 忽略既有 docs authority，自創另一套 backend 規則
- 把尚未證實的 framework / build tool / test tool 寫成 confirmed facts
- 把 init 任務做成完整 domain / controller / repository 全量實作

---

## 2. 開始前先判斷的內容

在輸出任何文件前，先明確整理：

1. backend module root 是否為 `services/quality-inspection-service`
2. module 是否已有實際 source tree
3. 是否已有 build file
4. 是否已有 test source root
5. 是否已有 config / migration / infrastructure evidence
6. backend 語言是否可確認
7. backend framework 是否可確認
8. build / test / run / lint / static analysis / CI gate 是否可確認

若 evidence 不足，不得把它們寫成 confirmed facts。

---

## 3. backend init 至少應產出的內容

### 3.1 `ai/project-context.yaml`【若 solution-level 尚未建立或需最小必要補充】
角色：
- repository 內第一優先的 authoritative project context
- 若 solution-level project context 已存在，則本次只做 backend 相關最小必要補充
- 必須反映：
  - backend module root
  - backend authority chain
  - backend tech stack confirmation status
  - gate inventory confirmation status
  - backend assumptions / pending confirmations

### 3.2 `.github/instructions/*backend*.instructions.md`【僅在 evidence 足夠時】
僅當 repo evidence 足以支持 backend-specific instructions 時才產出。  
例如可視情況產出：

- `backend-common.instructions.md`
- `backend-layering.instructions.md`

但若技術棧未確認，不得假裝已有 framework-specific instructions。

### 3.3 backend 最小工程入口【僅在任務明確要求時】
若本次任務明確要求建立 backend skeleton，可建立最小必要入口，例如：

- build file
- settings file
- source root
- test root
- package skeleton

但前提是：
- 不與 docs authority 衝突
- 不把未確認的 stack 寫成已確認
- 若 stack 仍未確認，應先保留 minimal neutral skeleton 或揭露 pending confirmation

---

## 4. backend 文件分工規則

請依下列原則決定規則落點：

- repository-wide hard rules -> `.github/copilot-instructions.md`
- repo operating guide -> `AGENTS.md`
- backend implementation area guide -> `services/quality-inspection-service/README.md`
- project authority context -> `ai/project-context.yaml`
- backend path-specific instructions -> `.github/instructions/*.instructions.md`

以下內容 **不得** 錯放：

- 詳細 domain invariant -> 不進 backend README
- 詳細 API schema -> 不進 backend README
- UI page spec 詳細內容 -> 不進 backend instructions 當 authority
- archive 歷史稿 -> 不得當現行 backend authority
- 未確認的 tech stack -> 不得進 framework-specific instructions

---

## 5. backend authority handling rules

backend area 必須以以下 authority chain 為準：

1. `decision-log.md`
2. `0_品質審查系統規格草案.md`
3. `glossary.md`
4. `domain-model.md`
5. `state-machine.md`
6. `permission-matrix.md`
7. `application-service.md`
8. `domain-events.md`
9. `api-spec.md`
10. `persistence-model.md`
11. `specification-by-example.md`
12. `test-scenarios.md`
13. `05-ui/*`【reference only】
14. `99-archive/*`【historical reference only】

你不得：

- 讓 controller / DTO 重新定義 domain rule
- 讓 persistence mapping 反向決定 aggregate invariant
- 讓 UI page spec 取代 API / permission / state authority
- 讓 archive 取代現行 design truth

---

## 6. backend implementation handling rules

本次 backend init 已知 module root 為：

- `services/quality-inspection-service`

但目前不代表以下事項已自動成立：

- backend language 已確認
- backend framework 已確認
- build tool 已確認
- test runner 已確認
- lint/static-analysis/ci 已確認
- actual source layout 已確認

因此：

### 可以確認的事
- backend implementation area 應位於該 path
- 後續 backend source / build / test / config 應以該 path 為根
- backend bootstrap 與 README 已提供 responsibility baseline

### 不可假裝已確認的事
- framework-specific code skeleton
- exact package naming
- build/test/run commands
- migration tool
- ORM choice
- actual DB tech

---

## 7. 執行步驟

### Step 1. Backend Module Inventory
盤點：
- module root
- README
- source tree
- build files
- config files
- test files
- missing governance / missing engineering entrypoints

### Step 2. Authority Source Summary
整理：
- backend bootstrap 是否存在
- docs authority 是否完整可讀
- backend README 是否存在
- 哪些來源已確認
- 哪些來源 partial / pending confirmation

### Step 3. Placement Decision
先說明：
- 哪些內容應進 `ai/project-context.yaml`
- 哪些內容應進 backend-specific instructions
- 哪些內容暫不應產出
- 若要建立工程入口，為何這樣放置

### Step 4. Minimal Backend Init Output
執行最小必要初始化：
- 優先 patch 既有治理入口
- 優先尊重 docs authority
- 不重複整套 solution init
- 不把 init 擴張成完整實作

### Step 5. Result Summary
最後輸出：
1. 新增 / 更新了哪些文件
2. backend governance 角色如何分工
3. 使用了哪些 authority sources
4. backend assumptions / pending confirmations
5. 哪些 build/test/gate 尚未驗證
6. 哪些工作留待下一階段

---

## 8. 輸出格式要求

請至少輸出：

### A. Authority Source Summary
### B. Backend Module Inventory
### C. Placement Decision
### D. Files Created / Updated
### E. Assumptions / Pending Confirmations
### F. Minimal Next Step Suggestion

---

## 9. Completion Rules

你不得宣稱：

- backend language 已確認
- backend framework 已確認
- build tool 已確認
- test runner 已確認
- lint / static analysis / CI gate 已完整確認
- persistence tech 已完整確認
- backend source layout 已完整確認

除非有足夠 evidence 支持。

若存在以下情況，必須明確揭露：

- module path 已知，但 source tree 尚未落檔
- build/test/run/lint/static-analysis/ci 入口未確認
- backend instructions 因 evidence 不足而尚未產出
- 某些工程骨架因 stack 未確認而暫不建立
- repository name 或 governance baseline 仍待確認