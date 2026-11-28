# 命令 use case：準則

狀態：試行（2026-11-28，main = `02811a3`）。

## 一、適用範圍

會改變 aggregate 的狀態、或會發出事件的操作。一個操作一個類別。只讀取的操作用 `../query-use-case/`。

## 二、寫法

| # | 寫法 | 出處 |
|---|---|---|
| 1 | 類別放在 `application/<aggregate>/`，名稱是 `<動詞><名詞>UseCase`，標 `@Service` 與類別層的 `@Transactional` | 現況；名稱是範例執行器找類別的依據（`specs/README.md` 第四節） |
| 2 | 只有一個 public 方法 `execute`。第一個參數是 `ActorContext actor`，其餘輸入逐一列成參數，不包成命令物件 | 現況；範例執行器照參數名稱對 `input` |
| 3 | 依賴由建構子注入。要時間就注入 `Clock`，要 id 就注入 `IdGenerator` | D-092；ARCH-02 |
| 4 | `execute` 開頭取一次 `Instant now = Instant.now(clock)`，寫入 aggregate 與事件都用這一個值 | D-092 第 1 點 |
| 5 | 檢查的順序：`requireRole` → 載入 aggregate → `requireHospitalScope` → 身分檢查（是不是受指派的人）→ 領域規則 | D-024；`spec-profile.yaml` 的 `guard_order` |
| 6 | 建立新 aggregate 的操作沒有東西可載入：院區取自操作者（`actor.requireHospitalId()`） | 現況（`CreateReviewProjectUseCase`）；D-042 |
| 7 | 找不到用 `ResourceNotFoundException`，角色與身分用 `ActorContext` 的檢查或 `AccessDeniedException`，狀態衝突用 `DomainStateConflictException`，規則不符用 `DomainRuleViolationException`，輸入不合法用 `DomainValidationException`。每個例外帶規格裡的錯誤代碼 | `spec-profile.yaml` 的 `error_categories`；ARCH-03 |
| 8 | 改變狀態的規則寫在 aggregate 的方法裡，use case 呼叫它 | 後端 `README.md` §7.1、§7.2 |
| 9 | 先存檔（`repository.save`），再發布事件 | 現況，見第四節 |
| 10 | 事件經過 `DomainEventPublisher` 發布。事件的 id 用 `idGenerator.newId()`，發生時間用第 4 點的 `now` | D-092 第 1、2 點 |
| 11 | 被拒絕時不發出任何事件 | D-065 第 7 點；範例執行器會檢查 |
| 12 | 回傳值：沒有就是 `void`；有就是 `application` 裡的 `…Result` 類別或單一的 id | 現況 |

## 三、為什麼

- 第 2、3、4 點讓同一支 use case 在範例執行器裡可以重現：時間與 id 都由外部決定。
- 第 5 點的順序決定操作者看到哪一個錯誤代碼。順序不同，同一個請求會得到不同的拒絕理由，範例也就驗不準。院區檢查放在載入之後，是因為院區記在 aggregate 上。
- 第 9、10 點：稽核紀錄由 `AuditTrailListener` 在同一個交易裡同步寫入，所以事件與狀態一起成功或一起失敗。

## 四、現況（字串樣式計數，沒有逐檔確認）

- 第 1、2 點：52 支 use case 全部符合（含查詢）。
- 第 3、10 點：發布事件的 30 支都注入 `IdGenerator`，沒有一支直接用 Spring 的發布器。
- 第 5 點：粗略的腳本沒有找到順序顛倒的。
- 第 9 點：有 1 支的發布寫在第一個 `save` 之前（`SaveInitialReviewDraftUseCase`）。沒有打開確認原因。

## 五、還沒有定的

1. **狀態的檢查寫在 use case 還是 aggregate。** 第 8 點是文件的說法，但 `SubmitInitialReviewUseCase` 在 use case 裡檢查案件與提交的狀態並直接丟例外。兩種寫法都存在，沒有裁決。跨兩個 aggregate 的檢查（案件加提交）可能本來就只能寫在 use case。
2. 第 9 點要不要成為規則（`SaveInitialReviewDraftUseCase` 是例外還是錯）。
3. 輸入很多時要不要改用命令物件。現在是 0 支。
