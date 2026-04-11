你現在要執行一個 **實做 vs 設計差異盤點** 任務。

目標：
- 讀取目前 repository 的實作
- 讀取目前已存在的設計文件
- 比對實做與設計差異
- 先產出一份 `design-implementation-gap.md`
- 不直接大改 spec 或 code
- 不得虛構，無 evidence 就標示不確定

---

## Authority Sources

### 設計文件
至少讀取：
- `glossary.md`
- `context-map.md`【若存在】
- `domain-model.md`
- `state-machine.md`
- `domain-events.md`
- `application-service.md`
- `permission-matrix.md`
- `api-spec.md`
- `persistence-model.md`
- `specification-by-example.md`
- `test-scenarios.md`
- `decision-log.md`

### 實作來源
至少盤點：
- module / package 結構
- domain / entity / aggregate 對應程式
- application service / use case
- controller / API
- repository / persistence mapping
- migration / schema
- tests
- security / authorization
- event / audit / outbox【若存在】

---

## 執行原則

1. 先盤點，再下結論  
2. 每個結論都要有：
   - 設計依據
   - 實作依據
   - 判定理由
3. 差異只能分類為：
   - `aligned`
   - `partially_aligned`
   - `missing_in_implementation`
   - `implemented_but_undocumented`
   - `conflict`
   - `not_verifiable`
4. 不得預設一定是 code 錯，也可能是 spec 過時或 decision 未收斂
5. 先寫入 `design-implementation-gap.md`，不要直接改其他文件

---

## 必查面向

- 詞彙是否一致
- bounded context / module 邊界是否一致
- aggregate / invariant / revision / snapshot / versioning 是否一致
- state machine / terminal state 是否一致
- application service 是否只做 orchestration
- API path / payload / error code 是否一致
- persistence model / unique constraint / revision append-only 是否一致
- specification-by-example 是否有對應實作
- test-scenarios 是否有對應測試
- accepted decision 是否被實作遵守

---

## 差異分流判斷規則

盤點後，對每一筆 gap 都必須判斷下一步應落哪裡：

- **已確定業務規則改變**  
  -> `update_decision_log`  
  -> 目標文件：`decision-log.md` + 受影響 spec

- **名詞或語意不一致**  
  -> `update_spec`  
  -> 目標文件：`glossary.md`

- **bounded context / 模組責任錯位**  
  -> `update_spec`  
  -> 目標文件：`context-map.md` 或 `domain-model.md`

- **aggregate / invariant / revision / snapshot / versioning 不一致**  
  -> `update_code` 或 `update_spec`  
  -> 目標文件：`domain-model.md`

- **狀態流轉不一致**  
  -> `update_code` 或 `update_spec`  
  -> 目標文件：`state-machine.md`

- **application orchestration 不一致**  
  -> `update_code` 或 `update_spec`  
  -> 目標文件：`application-service.md`

- **API 契約不一致**  
  -> `update_code` 或 `update_spec`  
  -> 目標文件：`api-spec.md`

- **資料表 / mapping / constraint / persistence 策略不一致**  
  -> `update_code` 或 `update_spec`  
  -> 目標文件：`persistence-model.md`

- **已定規則缺少 example 固定**  
  -> `update_examples`  
  -> 目標文件：`specification-by-example.md`

- **已定規則缺少測試覆蓋**  
  -> `update_tests`  
  -> 目標文件：`test-scenarios.md`

- **evidence 不足，無法判定誰應調整**  
  -> `pending_confirmation`

補充判斷：
- 若 `decision-log.md` 已明確 accepted，而實作偏離，預設優先判為 `update_code`
- 若實作已穩定存在，但 spec 明顯未記錄，判為 `implemented_but_undocumented`，預設優先 `update_spec`
- 若設計與實作都各有依據但互相衝突，判為 `conflict`，優先 `update_decision_log`

---

## 輸出格式

請輸出 `design-implementation-gap.md`，至少包含：

# Design Implementation Gap Report

## 1. Authority Sources Used
## 2. Design Artifact Inventory
## 3. Implementation Inventory
## 4. Alignment Summary
## 5. Detailed Findings
## 6. Gap Classification Table
## 7. Recommended Minimal Follow-up Plan

其中 `Gap Classification Table` 每列至少包含：
- `gap_id`
- `area`
- `design_source`
- `implementation_source`
- `status`
- `summary`
- `evidence`
- `recommended_action`
- `target_artifact`
- `note`

---

## Completion Rules

不得宣稱：
- implementation 已全面符合設計
- 所有差異都應修 code
- 所有 spec 都仍然最新
- 所有核心規則都已有測試覆蓋

若 evidence 不足，必須標示：
- `not_verifiable`
- 或 `pending_confirmation`
