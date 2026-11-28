# 查詢 use case 與查詢埠：準則

狀態：試行（2026-11-28，main = `02811a3`）。

## 一、適用範圍

只讀取、不改變 aggregate 狀態、不發出事件的操作。報表與儀表板也算（目前在 `reporting`，D-048 第 4 點要搬進 `application`）。

## 二、寫法

| # | 寫法 | 出處 |
|---|---|---|
| 1 | 骨架與命令相同：`@Service`、唯一的 `execute`、第一個參數是 `ActorContext actor`。名稱以 `Get`、`List`、`Preview`、`Export` 開頭 | 現況 |
| 2 | 類別層標 `@Transactional(readOnly = true)` | D-048 的 Pending（尚未成為規則）；現況 18 / 22 |
| 3 | 例外：讀取本身要留下紀錄時（完整初審的讀取者紀錄），用可寫入的 `@Transactional`，並在類別上寫明依據 | D-055 第 5 點 |
| 4 | 檢查的順序與命令相同（角色 → 載入 → 院區）。院區用 `actor.requireReadScope`，不是 `requireHospitalScope`：只有 `SYSTEM_ADMIN` 可以跨院區讀 | D-024、D-042、D-047 |
| 5 | 清單、搜尋、跨資料列的投影：經過查詢埠。埠（`…QueryPort`）與回傳的 view 定義在 `application/<aggregate>/query/`，實作（`…QueryAdapter`）在 `infrastructure/persistence/` | D-048 第 2 點 |
| 6 | 院區範圍是查詢條件裡明確的欄位，由操作者決定，不靠切面隱含套用 | D-048 第 5 點 |
| 7 | 回傳 `application` 自己的 view，不回傳 aggregate、不回傳 JPA entity | D-048 第 3 點 |
| 8 | 不同角色看到不同欄位時（遮罩、白名單），在 use case 或專屬的 policy 裡明確處理，回傳不同的 view 或把欄位設成 null | D-033、D-036、D-048 的 Pending |
| 9 | 用 id 載入單一 aggregate 來做授權檢查，或從它本身組出 view：**現況是直接用寫入側的 repository**。見第五節 | 無 |

## 三、為什麼

- 第 5、6 點的理由寫在 D-048 的 Evidence：`reporting` 另走一條讀取路徑時，漏掉了院區範圍（2026-11-14 以測試確認、同日修正）。
- 第 7 點讓讀取的形狀與寫入的模型可以各自改。
- 第 8 點：盲審的遮罩不適合切面，因為要看操作者與這一筆資料的關係。

## 四、現況（2026-11-28 查證）

名稱符合第 1 點的 use case 有 22 支（`application` 20、`reporting` 2）。

**交易**：18 支 `readOnly = true`。其餘 4 支：

| use case | 寫法 | 說明 |
|---|---|---|
| `GetReviewCaseDetailUseCase`、`GetSubmissionDetailUseCase`、`GetRevisionHistoryUseCase` | 可寫入的 `@Transactional` | 類別上有註解引用 D-055 第 5 點。符合第 3 點 |
| `PreviewSamplingPopulationUseCase` | 沒有 `@Transactional` | 沒有說明。不符第 2 點 |

**用到寫入側 repository 的 9 支**（逐一打開 `execute` 看過）：

| use case | 用 repository 做什麼 |
|---|---|
| `GetCaseLifecycleUseCase` | 載入案件，只為了檢查院區；資料來自查詢埠 |
| `GetSamplingBatchDetailUseCase`、`ExportSamplingBatchResultUseCase`、`GetSamplingExecuteContextUseCase` | 載入專案，檢查存在與院區；資料來自查詢埠（第三支沒有逐行確認資料來源） |
| `PreviewSamplingPopulationUseCase` | 載入專案，檢查院區與狀態；母體來自埠 |
| `GetScoreSheetSectionsUseCase`、`GetScoreSheetVersionUseCase`、`GetReviewProjectVersionUseCase` | 載入單一 aggregate，直接從它組出 view |
| `GetRevisionHistoryUseCase` | 載入案件與它的全部提交，做讀取授權並組出 view |

9 支都是「用 id 載入單一 aggregate（或一個案件底下的提交）」。沒有一支用 repository 做清單或搜尋。

## 五、還沒有定的

1. **第 9 點與 D-048 的 Pending 衝突。** Pending 的原文是「查詢側不得依賴寫入用的 repository」，照字面 9 支都不符。草稿 53 的 M-6 有兩個選項：照原文、9 支凍結在基線；或修改 D-048，允許用 id 載入單一 aggregate。查證的結果是 9 支都屬於後者會允許的用法。**要負責人裁決**，在那之前本準則照現況寫。D-101 的 `GetReviewCaseScoreSheet` 也會是這種用法（載入案件做授權，再載入評分表）。
2. `PreviewSamplingPopulationUseCase` 要不要補 `readOnly`。
3. `reporting` 搬進 `application` 的時程與套件命名（D-048 的 Pending）。
4. D-048 的 Pending 第 4 條「每個查詢處理器都必須帶授權註解」：現在的寫法是在 `execute` 裡呼叫 `actor.requireAnyRole`，沒有註解。要改 Pending 的文字，還是改程式，沒有裁決。
