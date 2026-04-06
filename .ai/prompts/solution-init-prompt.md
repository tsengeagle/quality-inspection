# solution-init-prompt.md

請根據我提供的 `.ai/bootstrap/solution-bootstrap.yaml`，初始化此 repository 的 **solution-level GitHub Copilot / agent mode 治理文件**。

這是一個 **solution-level repository init** 任務。  
你的目標不是直接開始寫 backend 或 frontend 程式碼，也不是只根據想像中的技術棧生成通用模板，而是要根據：

1. `.ai/bootstrap/solution-bootstrap.yaml`
2. repository 實際結構
3. docs authority 結構
4. 已存在的需求 / DDD / contracts / testing / UI 文件
5. bootstrap 指定的治理來源與 assumptions

共同收斂出此 repository 的第一版 solution-level 治理結構。

---

## 0. 初始化前的 authority source

開始前，先讀取並整理以下來源。

### A. bootstrap
- `.ai/bootstrap/solution-bootstrap.yaml`

### B. repository 實際結構
至少盤點：
- repo root
- `docs`
- `services/quality-inspection-service`【若存在】
- `frontends/quality-inspection-frontend`【若存在】
- 其他能反映 solution-level 結構的代表性檔案或資料夾

### C. docs authority
必須先讀取並辨識以下 docs 分層與角色：

#### `docs/01-requirements`
- `0_品質審查系統規格草案.md`
- `decision-log.md`
- `glossary.md`

#### `docs/02-ddd`
- `application-service.md`
- `context-map.md`
- `domain-events.md`
- `domain-model.md`
- `permission-matrix.md`
- `state-machine.md`

#### `docs/03-contracts`
- `api-spec.md`
- `persistence-model.md`

#### `docs/04-testing`
- `specification-by-example.md`
- `test-scenarios.md`
- 其他 testing support docs

#### `docs/05-ui`
- `ui-information-architecture.md`
- `ui-page-inventory.md`
- 各頁 page spec

#### `docs/99-archive`
- 只可作歷史參考，不作現行 authority，除非現行文件不足且需追溯演進脈絡

### D. governance baseline【若 bootstrap 指定】
若 bootstrap 指定 governance repo / shared rules baseline，則其角色為：
- 提供治理方向
- 提供 workflow / shared baseline 線索
- 協助決定 solution-level 文件落點與規則語氣

但不得忽略 repository 實際 docs authority，也不得把 baseline 原文整份照搬進 repo。

### E. 資訊不足時的處理
若有任何事無法從 bootstrap、repo 或 docs authority 證實，必須：
- 標示為 `assumption`
- 或標示為 `pending confirmation`
- 寫入 `ai/project-context.yaml`
- 在 summary 中揭露

不得自行補造已確認事實。

---

## 1. 本次任務的定位

這次是 **solution-level init**，不是 backend init，也不是 frontend init。

你要做的是先建立：

- repo root 的治理入口
- solution-level authority context
- solution-level operating guide
- 能正確導向既有 docs authority 的文件結構

你現在 **不應該**：

- 把 backend / frontend 寫成已完整驗證的工程骨架
- 產生 stack-specific implementation rules 當成已確認
- 把 solution-level init 做成 backend/frontend re-init
- 忽略既有 docs authority，另外發明一套並行文件結構

---

## 2. 本次要先判斷的內容

在輸出任何文件前，先明確整理：

1. 目前 repo 是否已有 solution-level docs authority
2. docs authority root 是否為 `docs`
3. backend implementation area 是否位於：
   - `services/quality-inspection-service`
4. frontend implementation area 是否位於：
   - `frontends/quality-inspection-frontend`
5. 哪些 implementation area 已有 evidence
6. 哪些技術棧仍屬 `pending confirmation`
7. 哪些 build / test / run / lint / gate 入口仍未驗證

若 source evidence 不足，不得把它們寫成 confirmed facts。

---

## 3. 固定產出文件

本次 solution-level init 至少應固定產出以下共通文件。

### 3.1 `.github/copilot-instructions.md`
角色：
- repository-wide 全域硬規則
- solution-level repository hard rules
- 不重複 domain / UI / API 細節全文
- 不把 archive 當 authority
- 要求依 docs 分層落點正確修改

至少應包含：
- backward compatibility
- no incidental refactor
- cannot verify → cannot claim done
- change placement discipline
- document authority precedence
- solution-level completion format

### 3.2 `AGENTS.md`
角色：
- repo operating guide
- repository map
- docs index
- inspection order
- representative files
- placement rules
- escalation rules
- build / test / run / lint / gate inventory
- assumptions / pending confirmations summary

若某些 build/test/gate 無法確認，必須揭露。

### 3.3 `ai/project-context.yaml`
角色：
- repository 內第一優先的 authoritative project context
- 收斂 bootstrap、repo 結構、docs authority、solution-level implementation areas
- 記錄 assumptions / pending confirmations
- 明確指出：
  - docs authority root
  - requirements / ddd / contracts / testing / ui 分層
  - backend / frontend area path
  - tech stack confirmation status
  - gate inventory confirmation status

### 3.4 path-specific instructions【僅在 evidence 足夠時】
若 repo evidence 足夠支持某些 solution-level common instructions，可產出最小必要 instructions。  
若 evidence 不足，寧可保留 pending confirmation，也不要虛構 stack-specific 規則。

---

## 4. solution-level 文件分工規則

請依下列原則決定規則落點：

- repository-wide hard rules -> `.github/copilot-instructions.md`
- repo operating guide -> `AGENTS.md`
- project authority context -> `ai/project-context.yaml`
- stack/path-specific rules -> `.github/instructions/*.instructions.md`【僅在 evidence 足夠時】

以下內容 **不得** 錯放：

- 詳細 domain invariant -> 不進 `AGENTS.md`
- 詳細 API schema -> 不進 `AGENTS.md`
- UI page spec 詳細內容 -> 不進 `.github/copilot-instructions.md`
- archive 歷史稿 -> 不得當作現行 authority
- 未確認的技術棧 -> 不得進 path-specific instructions 當成 confirmed rule

---

## 5. docs authority handling rules

本 repo 的 docs authority 結構已存在，必須以其真實分層為準：

```text
docs
├─ 01-requirements
├─ 02-ddd
├─ 03-contracts
├─ 04-testing
├─ 05-ui
├─ 99-archive
└─ README.md
```

你不得：

* 重新發明另一套 docs 分類
* 把現有文件硬搬到新的虛構資料夾
* 把 `99-archive` 誤當現行設計 authority
* 把 `04-testing/specification-by-example.md` 與 `test-scenarios.md` 混為同一角色

你必須：

* 尊重現有 docs 樹
* 在治理文件中正確反映這份真實結構
* 讓後續 agent 能以這份結構判斷落點與 authority

---

## 6. implementation area handling rules

本次 solution-level init 已知 implementation area path 為：

* backend: `services/quality-inspection-service`
* frontend: `frontends/quality-inspection-frontend`

但目前只確認 **路徑規劃**，不代表以下事項已自動成立：

* backend 技術棧已確認
* frontend 技術棧已確認
* build files 已存在
* test runner 已存在
* lint/gate 已存在

因此：

### 可以確認的事

* solution-level repo 預期有 backend area
* solution-level repo 預期有 frontend area
* 後續 README / bootstrap / init prompt 應以這兩個 path 為落點

### 不可假裝已確認的事

* Spring Boot / Angular / 其他 stack
* build / test / run / lint / gate commands
* source root 內部 package/module layout
* CI pipeline

---

## 7. 執行步驟

### Step 1. Current Repository Inventory

盤點：

* repo root
* docs authority root
* implementation area root
* 已存在的治理文件
* 缺少哪些治理入口文件

### Step 2. Authority Source Summary

整理：

* bootstrap 是否存在
* docs authority 是否存在
* requirements / ddd / contracts / testing / ui 層是否存在
* governance baseline 是否存在
* 哪些來源已確認
* 哪些來源仍屬 partial / pending confirmation

### Step 3. Placement Decision

先說明：

* 哪些內容應進 `.github/copilot-instructions.md`
* 哪些內容應進 `AGENTS.md`
* 哪些內容應進 `ai/project-context.yaml`
* 哪些內容暫不應產出

### Step 4. Minimal Init Output

產出 solution-level 治理文件，優先：

* 最小必要修改
* 正確文件分工
* 不與既有 docs authority 重複
* 不把 init 做成整套重建

### Step 5. Result Summary

最後輸出：

1. 新增 / 更新了哪些文件
2. 各文件扮演什麼角色
3. 使用了哪些 authority sources
4. 哪些 assumptions / pending confirmations 仍存在
5. 哪些 backend/frontend 事項尚未驗證
6. 哪些工作留待下一階段

---

## 8. 輸出格式要求

請至少輸出：

### A. Authority Source Summary

### B. Repository Inventory

### C. Placement Decision

### D. Files Created / Updated

### E. Assumptions / Pending Confirmations

### F. Minimal Next Step Suggestion

---

## 9. Completion Rules

你不得宣稱：

* backend stack 已確認
* frontend stack 已確認
* build / test / lint / CI gate 已完整確認
* backend/frontend implementation skeleton 已完整存在
* archive 與現行 authority 完全一致
* 所有 assumptions 已消除

除非有足夠 evidence 支持。

若存在以下情況，必須明確揭露：

* implementation area 只有 path 規劃，尚無工程 evidence
* build/test/run/lint/static-analysis/ci 入口未確認
* 某些 solution-level governance files 尚未存在
* path-specific instructions 因 evidence 不足而尚未產出
* repository name 或 governance baseline 仍待確認
