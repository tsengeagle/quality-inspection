你現在要執行一個 **implementation vs design gap 重新分類** 任務。

這不是一般 code review。
這不是直接修 bug。
這也不是直接更新 spec 或直接改 code。

你的任務是：

- 掃描目前 repository 的實作
- 讀取既有設計文件與 decision authority
- 驗證目前已列出的 gap
- 將每一個 gap 重新分類為：
  1. acceptable divergence
  2. unacceptable divergence
  3. decision required
- 明確指出為什麼
- 不得虛構 repo 內不存在的事實
- 不得因為實作已存在，就自動把實作視為正確
- 不得因為設計文件已存在，就自動把文件視為絕對正確
- 必須依 authority source 與 repo evidence 做判定

---

## 任務背景

目前 repository 已有一份 design–implementation gap report。
但其中有些 gap 可能其實是：

- 設計文件落後於穩定實作
- 已定稿 authority 被實作偏離
- 尚未完成決策的設計分歧

因此你現在要做的是 **重新分類**，而不是直接接受舊 gap report 的結論。

---

## 目標輸出

你必須輸出一份新的分析文件，例如：

`docs/review/gap-reclassification-report.md`

此文件要針對目前 gap 逐項整理：

- gap 是什麼
- 對應哪些 authority sources
- repo 內實作現況是什麼
- 是否可以接受實作成為 repo truth
- 應分類為：
  - acceptable divergence
  - unacceptable divergence
  - decision required
- 後續最小動作是什麼

---

## 分類定義

### 1. acceptable divergence
符合以下特徵者可歸類為 acceptable divergence：

- 設計文件尚未定稿，或文件彼此存在分歧
- 實作已形成穩定且合理的 repo-specific truth
- 實作未違反已接受的 decision log / domain invariant / security rule
- 實作不會破壞外部契約，或目前尚未形成正式外部契約
- 將設計文件更新為與實作一致，風險低於回頭修改實作

此類項目的預設處理方向：
- update_spec / update_design_doc

### 2. unacceptable divergence
符合以下特徵者應歸類為 unacceptable divergence：

- 實作偏離已接受 decision log
- 實作偏離已明確定稿的 domain invariant / state machine / security boundary
- 實作破壞外部 API contract
- 實作造成資料隔離、安全、可追溯性、兼容性或 correctness 風險
- 不應因「已經做了」就被合理化

此類項目的預設處理方向：
- update_code
- 必要時補 test / contract test / guard

### 3. decision required
符合以下特徵者應歸類為 decision required：

- 設計文件與實作各自都有合理性
- 目前 authority sources 無法單向裁決
- 涉及外部契約、路徑設計、payload、持久化策略、建模取捨等議題
- 若未先做決策，就不適合直接讓 spec 跟 code 單向收斂

此類項目的預設處理方向：
- raise_decision
- 先提出 decision topic 與選項，不直接改 code 或 spec

---

## Authority Sources

僅可使用以下 authority sources。

### A. Gap report
若 repo 中已有 gap report，必須讀取並盤點：
- gap id
- gap summary
- status
- recommended action
- target artifact
- priority

### B. Decision authority
必須優先讀取：
- `decision-log.md`
- 任何明確 accepted decisions 的文件

這些屬於高優先 authority source。
若實作偏離已 accepted decision，預設不可直接接受為 repo truth。

### C. Domain / design authority
必須讀取並整理：
- `glossary.md`
- `context-map.md`
- `domain-model.md`
- `state-machine.md`
- `domain-events.md`
- `application-service.md`
- `permission-matrix.md`
- `api-spec.md`
- `persistence-model.md`
- `specification-by-example.md`
- `test-scenarios.md`

### D. Repository implementation evidence
必須實際掃描 repo 內的：
- controller
- application service / use case
- domain aggregate
- repository / persistence mapping
- security / auth / scope guard
- exception mapping
- test code
- representative DTO / request / response model

### E. Tests as evidence
若測試已存在，必須檢查：
- 測試是在支持現行實作
- 還是在支持設計文件
- 還是兩者都未完整覆蓋

注意：
測試存在不代表實作一定正確；
只能代表目前 repo 對該行為已有某種 evidence。

---

## Authority Precedence

若不同來源衝突，優先序如下：

1. repository 實際已確認事實
2. accepted decision log
3. 明確定稿且彼此一致的 design documents
4. gap report 既有結論
5. 未經驗證的歷史描述

注意：
- 不得因 gap report 已下結論，就忽略 repo 現況
- 不得因 code 已存在，就直接覆寫 accepted decision
- 不得將未定稿設計誤當成絕對 authority

---

## 執行步驟

### Step 1. Gap Inventory
先列出目前 gap report 中所有 gap，至少整理：
- gap id
- title / topic
- current classification
- recommended action
- target artifact
- priority

### Step 2. Authority Mapping
對每一個 gap，找出它對應的 authority source：
- accepted decision?
- domain invariant?
- permission boundary?
- API contract?
- persistence tactic?
- documentation only?

### Step 3. Repo Evidence Check
針對每一個 gap，實際掃描 repo：
- 哪些 class / file / endpoint / DTO / entity 與此 gap 相關
- 目前實作實際如何運作
- 是否已有測試覆蓋
- 是否已有下游依賴跡象

### Step 4. Reclassification
對每一個 gap，回答：

1. 這是不是已接受 authority 的偏離？
2. 這是不是設計文件落後於穩定實作？
3. 這是不是尚未完成決策的分歧？
4. 是否可以接受目前實作成為 repo truth？
5. 最終分類是：
   - acceptable divergence
   - unacceptable divergence
   - decision required

### Step 5. Minimal Action
每一個 gap 都要給最小必要動作：
- update_spec
- update_code
- add_test
- add_contract_test
- raise_decision
- no_change_now

### Step 6. Summary
最後要輸出：
- 哪些 gap 可接受
- 哪些 gap 不可接受
- 哪些 gap 待決策
- 哪些屬於高風險 blocker
- 哪些不應阻斷後續 frontend / downstream 開發
- 哪些應先處理

---

## 特別判定規則

以下類型，預設傾向判為 **unacceptable divergence**，除非 repo evidence 明確推翻：

### A. Security / scope isolation
例如：
- hospital scope guard
- actor ownership guard
- unauthorized / forbidden semantics
- data visibility boundary

### B. Accepted decision log mismatch
例如：
- CLOSED 後不可 revision
- CANCELLED terminal state
- revision append-only
- actor-specific revision guard
- same-batch duplicate rule

### C. External contract break
例如：
- API path 已被前端或整合測試正式依賴
- request / response schema 不一致
- error code / HTTP status semantic mismatch

---

以下類型，預設傾向判為 **decision required**：

### D. API path / method design divergence
當 spec 與 controller 已分岔，但尚無明確 authority 可裁決時。

### E. Persistence strategy divergence
當設計文件是較理想化 DDD persistence，而實作採用簡化策略，且尚未有 accepted decision 規定必須做到哪個程度時。

### F. Tactical modeling divergence
當命名、分層、事件粒度、freeze 實作方式等有分歧，但未直接違反已接受 business rule 時。

---

以下類型，預設傾向判為 **acceptable divergence**：

### G. Implemented but undocumented
若實作合理、穩定、未違反 authority，只是文件沒更新。

### H. Low-risk document lag
例如：
- 文件命名落後
- event catalog 缺少低風險補充事件
- context map / glossary 未收斂實際命名，但不影響核心規則

---

## 輸出格式

請固定輸出以下章節：

# Gap Reclassification Report

## 1. Review Scope
## 2. Authority Sources Used
## 3. Gap Inventory
## 4. Reclassification Result Summary

### 4.1 Acceptable Divergences
### 4.2 Unacceptable Divergences
### 4.3 Decision-Required Divergences

## 5. Detailed Gap Analysis

每個 gap 使用以下格式：

### GAP-XX [topic]

- Original classification:
- Related authority sources:
- Related implementation evidence:
- Current repo behavior:
- Test evidence:
- Risk assessment:
- Can current implementation be accepted as repo truth: yes / no / pending decision
- Reclassified as:
  - acceptable divergence
  - unacceptable divergence
  - decision required
- Recommended minimal action:
- Why:

## 6. High-Risk First Fix Candidates
## 7. Gaps That Should Not Block Frontend Progress
## 8. Decision Topics Requiring Human Confirmation
## 9. Minimal Next-Step Plan

---

## Completion Rules

你不得宣稱：
- 所有 gap 都可讓設計文件向實作收斂
- 所有 gap 都應回頭修 code
- 所有設計文件都已定稿
- 實作已自然成為唯一 authority

除非有足夠 evidence 支持。

若 evidence 不足，必須標記：
- pending decision
- pending confirmation
- not fully verifiable from current repo

---

## 額外要求

1. 先分析，再分類，不要直接 patch code。
2. 不得跳過 repo 掃描，只根據既有 gap report 下結論。
3. 不得用模糊語句帶過，必須指出對應檔案或實作位置。
4. 對每一個高風險 gap，優先考慮：
   - security
   - contract stability
   - traceability
   - backward compatibility
5. 若某 gap 對 frontend 整合有直接影響，必須明確標示是否為 frontend blocker。

---

本輪請優先聚焦以下 topic：

1. API path / method divergence
2. request / response payload divergence
3. error code / HTTP status divergence

請先完成這三類的 reclassification。
若時間或上下文有限，先不要全面展開其他低優先 gap。

---

特別注意：

- 不得因 controller 已存在某條 path，就自動判定 spec 應修改
- 不得因 response DTO 目前回 null，就自動判定 null response 可接受
- 不得因 exception handler 現況回 403，就自動判定 401/403 語意可忽略
- 只要涉及 frontend / integration contract，預設先判斷是否需要人類決策
