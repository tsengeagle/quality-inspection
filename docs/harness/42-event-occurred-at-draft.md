# 事件的發生時間改用注入的時鐘

狀態：**待裁決**（2026-11-26 起草，main = `49181ac`）。本文件不是律法；題號 O-1～O-6 只在本文件使用。

題目清單待辦第 1 題（D-090 第 6 點）。負責人 2026-11-26 定的順序：本題 → HTTP 與 `api-spec.md` 的落差 → 專案版本管理與未裁決事項 → 前端移除 `finalConclusion`。

## 一、範圍

- 所有領域事件的 `occurredAt`，以及它寫進的稽核紀錄（`GetCaseLifecycle`、`ListAuditTrails` 的 `occurredAt`）。
- 題目清單要求一起看的 D-087 第 5 點：儀表板的逾期件數用資料庫的時間。
- 不在範圍內：逾期的規則本身（只看初審期限等），屬於待辦第 3 題。

## 二、查到的事實

**第 1 項是實測的**：本機在 `review-case/lifecycle_masks_scored_detail_before_rereview` 的 `entries[action=ASSIGNED]` 加上 `occurredAt: "2026-11-19T09:00:00Z"`（設定檔的 `clock_start`），跑驗收範例，從失敗訊息讀出實際值；用完已還原。其餘是讀程式與文件。

1. **範例的時鐘管不到事件時間**：實際的 `occurredAt` 是 `2026-11-26T03:25:21.036468600Z`（執行當下的系統時間）。同一筆紀錄的 `detail` 裡 `dueAt` 是 `2026-11-26T23:59:59Z`，跟著範例時鐘走。也就是說，同一個 use case 裡，aggregate 的時間取注入的 `Clock`，事件的時間取系統時間。
2. **時間在哪裡決定**：`DomainEvent` 的建構子 `this.occurredAt = Instant.now()`（`domain/common/DomainEvent.java:42`），同一個建構子還有 `eventId = UUID.randomUUID()`（第 41 行）。兩者都違反 ARCH-02，凍結在 ArchUnit 基線（`archunit_store/c335a5b1-…`，共 5 行，其中這 2 行屬於 `DomainEvent`）。
3. **規模**：32 個類別繼承 `DomainEvent`；`application` 有 31 種事件、41 個 `new …Event(` 的地方。
4. **哪些 use case 還沒有 `Clock`**：會發事件、但沒有注入 `Clock` 的有 12 支：review-project 6 支（`Create`、`UpdateDraft`、`PublishVersion`、`Activate`、`Deactivate`、`BindScoreSheet`）、score-sheet 3 支（`CreateDraft`、`UpdateDraft`、`PublishVersion`）、review-case 3 支（`ReopenInitialReview`、`ReturnInitialReview`、`SendToRereview`）。其他會發事件的 use case 已經有 `Clock`，自己的時間都用 `Instant.now(clock)`。
5. **一支 use case 發多個事件**：7 支。`ExecuteSampling`、`RetrySamplingBatch` 各 4 個；`SubmitInitialReview`、`SubmitRereview`、`SaveInitialReviewDraft`、`FixInitialReview`、`ReviseRereview` 各 2 個。
6. **稽核紀錄的排序**：`GetCaseLifecycle` 依 `occurredAt` 遞增（`AuditTrailQueryAdapter.java:54`），`ListAuditTrails` 依 `occurredAt` 遞減（第 38 行），都沒有第二排序鍵。`api-spec.md` §14.1、§14.2 沒有寫排序。範例比對清單時不比順序（`specs/README.md` 第四節）。
7. **稽核的時間篩選**：`ListAuditTrails` 的 `from`、`to` 篩的是 `occurredAt`（`AuditTrailQueryAdapter.java:79、82`）。規格沒有 `from`、`to` 的規則與範例。
8. **儀表板的「現在」**：`GetDashboardSummaryUseCase.java:73` 的 `e.dueAt < CURRENT_TIMESTAMP`，取資料庫的時間；這支 use case 沒有 `Clock`。逾期列在 `deferred_behaviors`，沒有範例。
9. **稽核紀錄寫入**：`AuditTrailListener` 把 `event.getOccurredAt()` 原樣寫進 `AuditTrailEntity`（第 34 行），自己不取時間。

## 三、待裁決

### O-1　時間在哪裡決定

| 選項 | 作法 | 優點 | 缺點 |
|---|---|---|---|
| **A（建議）** | `DomainEvent` 的建構子多一個 `Instant occurredAt` 參數；每支 use case 傳入 `Instant.now(clock)`，同一支 use case 用同一個 `now` 寫 aggregate 與事件 | 事件時間與 aggregate 的時間一致（例如提交時間等於提交事件的時間）；`domain` 不再取系統時間，ARCH-02 基線縮短 | 改 32 個事件類別、41 個發事件的地方，12 支 use case 要注入 `Clock` |
| B | 由 `AuditTrailListener`（infrastructure）用注入的 `Clock` 決定寫入的時間 | 只改 1 個檔案 | 事件物件上的 `occurredAt` 仍是系統時間，與稽核紀錄不一致；`DomainEvent` 的違規留在基線 |
| C | 由 `SpringDomainEventPublisher` 在發出前補上時間 | 改動少 | 事件要變成可修改的物件；事件在發出前沒有時間 |

### O-2　`eventId` 要不要一起改

`eventId` 在同一個建構子、違反同一條 ARCH-02。

- **A（建議）**：一起改。由 use case 用既有的 `IdGenerator` 產生並傳入。範例不比對 `eventId`，沒有可觀察的變化；改完 `DomainEvent` 從 ARCH-02 基線移除。
- B：本題只改時間，`eventId` 留在基線。

### O-3　同一支 use case 發出的多個事件同時間時的順序

選 O-1 A 之後，第 5 項的 7 支 use case 發出的事件 `occurredAt` 完全相同（現在是相差幾微秒，順序碰巧是發出的順序）。範例的時鐘固定時，整個範例的事件也可能同時間。

| 選項 | 作法 |
|---|---|
| **A（建議）** | 稽核紀錄加一個寫入序號，兩個查詢以 `occurredAt` 加序號排序（生命週期遞增、稽核軌跡遞減，照現況）。同時間的事件依發出的順序排 |
| B | 不加第二排序鍵，同時間的順序不保證，在規格寫明 |
| C | 同一支 use case 裡每個事件的時間各加 1 奈秒 | 

C 讓時間不再是真實的發生時間，不建議。A 要改資料表：`AuditTrailEntity` 加欄位，加一支 Flyway migration（目前到 `V7`）。測試以 `ddl-auto: validate` 對照 migration 建出的 schema，所以漏寫 migration 會讓測試失敗。migration 不在 `law/manifest.txt`、也不在問題側，屬於建構 PR。

### O-4　範例要驗證到什麼程度

- **A（建議）**：
  - 至少一個 `GetCaseLifecycle` 的範例比對 `occurredAt` 等於範例時鐘；其中一步前有 `advance`，驗證時間會前進。
  - O-3 選 A 時，加一個範例驗證同時間的事件依發出的順序排列。範例比對不比順序，要先確認格式能不能表達順序；不能的話改由整合測試保護，不改格式。
  - 修法 PR 時這些範例是紅的，凍結在 `known-test-failures.txt`，建構 PR 解凍。
- B：只改程式，不加範例。

`ListAuditTrails` 的 `from`、`to` 篩選（第 7 項）沒有規則，本題不立法；列進 `deferred_behaviors` 或另立題目。

### O-5　儀表板的「現在」（D-087 第 5 點）

- **A（建議）**：本題一起改成注入的 `Clock`，以查詢參數傳入 JPQL。逾期的規則仍不立法，維持 `deferred_behaviors`，不加範例。改完之後，逾期的題目才寫得出範例。
- B：留給逾期的題目（待辦第 3 題）。

注意：A 會讓「現在」從資料庫伺服器的時間變成應用程式伺服器的時間。兩台的時間不同步時，逾期件數在期限前後會有差異。

### O-6　分批

照 D-090 的作法：

1. **修法 PR**：裁決、`domain-events.md`（寫明 `occurredAt` 取 use case 的時鐘）、範例（紅的，凍結）、題目清單。
2. **建構 PR**：`DomainEvent` 與事件類別、use case 注入 `Clock`、稽核紀錄的序號與 migration（O-3 A）、儀表板（O-5 A）；ARCH-02 基線縮短；解凍範例。

建構 PR 改的是 `domain`、`application`、`infrastructure`、`reporting`，都在 `law/coverage.txt` 的已立法範圍（問題側已沒有暫准範圍，D-089）。新增的 migration 不在問題側。

## 四、建議總表

| 題 | 建議 |
|---|---|
| O-1 | A：use case 傳入時間 |
| O-2 | A：`eventId` 一起改 |
| O-3 | A：寫入序號當第二排序鍵 |
| O-4 | A：加範例，凍結後由建構 PR 解凍 |
| O-5 | A：儀表板一起改用注入的時鐘 |
| O-6 | 修法 PR → 建構 PR |
