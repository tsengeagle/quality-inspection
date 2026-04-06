# 品質審查系統設計文件索引

本目錄收斂品質審查系統的需求、DDD 設計、API / Persistence 設計與 UI 設計文件。  
目標是提供一套可追溯、可驗證、可落地的設計基線，供以下用途使用：

- 業務需求對齊
- DDD 設計與實作
- API / DB 設計
- UI / UX 設計
- 測試案例設計
- AI agent / 開發人員 onboarding

---

## 1. 建議閱讀順序

若是第一次接觸本系統，建議依以下順序閱讀：

### A. 先理解需求與決策
1. 系統需求書
2. `decision-log.md`
3. `glossary.md`

### B. 再理解 DDD 核心模型
4. `context-map.md`
5. `domain-model.md`
6. `state-machine.md`
7. `domain-events.md`
8. `application-service.md`

### C. 再看授權、API 與資料模型
9. `permission-matrix.md`
10. `api-spec.md`
11. `persistence-model.md`

### D. 最後看驗證與 UI
12. `test-scenarios.md`
13. `ui-page-inventory.md`
14. `ui-information-architecture.md`
15. `ui-page-spec.md`

---

## 2. 文件總覽

---

## 2.1 需求與決策層

### 系統需求書
**用途**  
定義品質審查系統的業務需求、角色、流程、規則與範圍。

**建議讀者**  
PM、BA、架構師、開發、測試

**說明**  
本系統所有設計文件都應以需求書為最上游依據。

---

### `decision-log.md`
**用途**  
記錄已正式定稿的關鍵設計決策，避免不同文件各自解讀。

**主要內容**
- 結案後不允許 revision
- 初審 revision 僅原審查醫師
- 複審 revision 僅複審醫師
- 重複抽選限制為同批次
- manager / auditor / sysadmin 的資料可見性決策

**建議讀者**  
架構師、開發、測試、AI agent

**重要性**  
高。  
若其他文件與實作有衝突，應先回頭檢查這份。

---

### `glossary.md`
**用途**  
統一系統中的共用詞彙，避免不同文件中的術語歧義。

**主要內容**
- 審查專案
- 抽選批次
- 案件
- 初審 / 複審
- revision
- snapshot
- project version
- domain event
- metadata view / full detail view

**建議讀者**  
全角色

---

## 2.2 DDD 戰略與戰術設計層

### `context-map.md`
**用途**  
定義 bounded context 劃分、上下游關係與外部 supporting context。

**主要內容**
- Review Project Configuration
- Sampling
- Assignment and Review
- Audit and Reporting
- Medical Record Source / IAM / Notification 等 supporting context

**建議讀者**  
架構師、後端開發、AI agent

---

### `domain-model.md`
**用途**  
定義核心 aggregate、entity、value object 與 invariant。

**主要內容**
- `ReviewProject`
- `ScoreSheetDefinition`
- `SamplingBatch`
- `ReviewCase`
- `ReviewSubmission`
- revision model
- snapshot model

**建議讀者**  
後端開發、架構師、測試

**重要性**  
高。  
是 domain truth 的核心文件之一。

---

### `state-machine.md`
**用途**  
定義案件與 submission 的合法狀態轉移。

**主要內容**
- `ReviewCase` 狀態機
- `ReviewSubmission` 子狀態機
- `SamplingBatch` 狀態機
- terminal state 規則
- revision 與 state 的關係

**建議讀者**  
後端開發、測試、前端開發

---

### `domain-events.md`
**用途**  
定義 domain event catalog 與業務事實事件。

**主要內容**
- project / version events
- sampling events
- case lifecycle events
- submission events
- revision events

**建議讀者**  
後端開發、整合開發、架構師

---

### `application-service.md`
**用途**  
定義 application layer 的 use case 與 orchestration 邊界。

**主要內容**
- Project Configuration use cases
- Sampling use cases
- Assignment / Initial Review use cases
- Re-review use cases
- Revision use cases
- Platform boundary

**建議讀者**  
後端開發、架構師

---

## 2.3 權限、API 與資料模型層

### `permission-matrix.md`
**用途**  
定義角色可做什麼、可看什麼、scope 到哪裡。

**主要內容**
- SYSTEM_ADMIN
- HOSPITAL_MANAGER
- REVIEWER
- REREVIEWER
- AUDITOR
- metadata only / full detail 規則
- revision 權限邊界

**建議讀者**  
後端開發、前端開發、測試、資安 / 權限設計

---

### `api-spec.md`
**用途**  
定義 HTTP API 契約與 command / query 邊界。

**主要內容**
- review projects APIs
- score sheets APIs
- sampling APIs
- review cases APIs
- initial review / rereview APIs
- revision APIs
- audit / reports APIs
- error code 模型

**建議讀者**  
後端開發、前端開發、整合開發、測試

---

### `persistence-model.md`
**用途**  
定義 transaction model、version / snapshot 持久化策略、主要資料表與約束。

**主要內容**
- review_project / review_project_version
- score_sheet / score_sheet_version
- sampling_batch / snapshot / selected_case
- review_case / snapshot_ref
- review_submission / answer / snapshot / attachment
- audit_trail / outbox_event
- unique keys / FK / index 建議

**建議讀者**  
後端開發、DBA、架構師

---

## 2.4 驗證層

### `test-scenarios.md`
**用途**  
把需求、決策與 DDD 規則轉成可驗證測試情境。

**主要內容**
- project / version scenarios
- sampling scenarios
- initial review scenarios
- re-review scenarios
- revision scenarios
- permission / visibility scenarios
- traceability / audit scenarios
- negative / conflict scenarios

**建議讀者**  
測試、後端開發、前端開發、AI agent

**重要性**  
高。  
是 test-first 與 regression 設計的重要基線。

---

## 2.5 UI 設計層

### `ui-page-inventory.md`
**用途**  
盤點系統需要的功能頁與頁面責任。

**主要內容**
- 專案與規則管理頁
- 抽選作業頁
- 案件與審查作業頁
- 查詢與報表頁

**建議讀者**  
前端開發、PM、UX、架構師

---

### `ui-information-architecture.md`
**用途**  
定義 UI 主導航、頁面層級、角色入口與主要使用路徑。

**主要內容**
- 主導航模組
- 頁面樹狀結構
- manager / reviewer / rereviewer / auditor / sysadmin 視角
- route 群組建議

**建議讀者**  
前端開發、UX、PM

---

### `ui-page-spec.md`
**用途**  
定義核心頁面的 UI 規格。

**目前已完成頁面**
- `UI-006 抽選執行頁`
- `UI-007 抽選批次列表頁`
- `UI-009 案件列表頁`
- `UI-010 案件詳情頁`
- `UI-011 案件分派頁`
- `UI-012 初審作業頁`
- `UI-013 複審作業頁`
- `UI-014 Revision 歷程頁`

**建議讀者**  
前端開發、UX、測試、後端開發

---

## 3. 文件之間的依賴關係

可用以下方式理解文件關係：

```text
需求書
  -> decision-log.md
  -> glossary.md
  -> context-map.md
  -> domain-model.md
      -> state-machine.md
      -> domain-events.md
      -> application-service.md
      -> permission-matrix.md
      -> api-spec.md
      -> persistence-model.md
      -> test-scenarios.md
      -> ui-page-inventory.md
          -> ui-information-architecture.md
          -> ui-page-spec.md
```

---

## 4. 不同角色建議優先閱讀文件

### PM / BA

* 需求書
* `decision-log.md`
* `glossary.md`
* `ui-page-inventory.md`
* `ui-information-architecture.md`

### 架構師

* 需求書
* `decision-log.md`
* `context-map.md`
* `domain-model.md`
* `state-machine.md`
* `application-service.md`
* `persistence-model.md`

### 後端開發

* `decision-log.md`
* `domain-model.md`
* `state-machine.md`
* `application-service.md`
* `domain-events.md`
* `permission-matrix.md`
* `api-spec.md`
* `persistence-model.md`
* `test-scenarios.md`

### 前端開發

* `decision-log.md`
* `permission-matrix.md`
* `api-spec.md`
* `ui-page-inventory.md`
* `ui-information-architecture.md`
* `ui-page-spec.md`
* `test-scenarios.md`

### 測試

* 需求書
* `decision-log.md`
* `state-machine.md`
* `permission-matrix.md`
* `api-spec.md`
* `test-scenarios.md`
* `ui-page-spec.md`

### AI Agent / 自動化輔助開發

* `decision-log.md`
* `glossary.md`
* `context-map.md`
* `domain-model.md`
* `state-machine.md`
* `permission-matrix.md`
* `application-service.md`
* `api-spec.md`
* `persistence-model.md`
* `test-scenarios.md`

---

## 5. 現階段文件完成度

### 已完成核心文件

* [x] `decision-log.md`
* [x] `glossary.md`
* [x] `context-map.md`
* [x] `permission-matrix.md`
* [x] `domain-model.md`
* [x] `state-machine.md`
* [x] `domain-events.md`
* [x] `application-service.md`
* [x] `api-spec.md`
* [x] `persistence-model.md`
* [x] `test-scenarios.md`
* [x] `ui-page-inventory.md`
* [x] `ui-information-architecture.md`
* [x] `ui-page-spec.md`

### 後續可補文件

* [ ] `openapi.yaml`
* [ ] `ddl-spec.sql` / migration 草稿
* [ ] `ui-state-actions-matrix.md`
* [ ] `ui-page-spec.md` 其餘頁面
* [ ] `ui-wireframe.md` 或 Figma 對照文件

---

## 6. 維護原則

1. 若需求或決策改變，優先更新：

   * 需求書
   * `decision-log.md`

2. 若 domain 規則改變，至少同步更新：

   * `domain-model.md`
   * `state-machine.md`
   * `application-service.md`
   * `api-spec.md`
   * `persistence-model.md`
   * `test-scenarios.md`

3. 若權限或資料可見性改變，至少同步更新：

   * `permission-matrix.md`
   * `api-spec.md`
   * UI 相關文件

4. 若頁面或導航改變，至少同步更新：

   * `ui-page-inventory.md`
   * `ui-information-architecture.md`
   * `ui-page-spec.md`
