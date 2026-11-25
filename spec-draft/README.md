# 結構化規格試作（草稿）

- 狀態：**草稿，尚未經人工審核，不是 authority**。權威來源仍是 `docs/01-requirements` 到 `docs/04-testing`。
- 2026-11-15：`ReviewProject` 已升格到 `specs/review-project/`，草稿已移除。正式的格式是 `specs/README.md` 的格式 v1；本文件只留作試作紀錄。
- 2026-11-15：`ScoreSheetDefinition` 草稿（直接採格式 v1，Q-16～Q-23 由 D-050 裁決）已升格到 `specs/score-sheet/`，草稿已移除。
- 2026-11-18：HTTP 契約試做（`http-common.yaml`、`score-sheet/http.yaml`），只列出、不擋。說明見 `docs/harness/13-http-contract-options.md` 第八、九節。
- 2026-11-19：HTTP 契約依 D-061 升格到 `specs/http-common.yaml`、`specs/score-sheet/http.yaml`，草稿已移除。之後放在這裡的 HTTP 契約草稿仍會被比對，但只列出、不擋（`specs/README.md` 第七節）。
- 2026-11-25：`review-case/http.yaml`、`sampling-batch/http.yaml`（D-088 第 10 點）。建構 PR 之後由修法 PR 二移到 `specs/`，草稿移除。
- 建立日期：2026-11-11，由 Claude Code 依現有文件轉換。
- 範圍：`ReviewProject` 的 5 支命令與 1 支查詢。
- 目的：試出一個機器可讀、可驗證的規格格式，並找出現有文件補不出來的地方。

## 檔案

| 檔案 | 內容 |
|---|---|
| `review-project/model.yaml` | aggregate 模型：屬性、狀態、轉移、不變量 |
| `review-project/usecases.yaml` | 每支 use case 的合約：誰能做、輸入、規則與結果、成功後的狀態 |
| `review-project/examples.yaml` | 可執行範例：只透過 use case 準備資料、執行、觀察 |
| `review-project/OPEN-QUESTIONS.md` | 轉換時補不出來的地方，附選項與建議 |

正式使用時建議一支 use case 一個檔案；草稿階段放在一起方便審閱。

## 格式規則

1. **每一條規則都有穩定的 id**（例如 `RP-PUB-03`）。範例、測試、gate 都用 id 引用，不用陣列位置。
2. **每一條規則都寫出結果。** 不寫「不可以」，寫「回 `REJECTED` 與哪個代碼」。一條規則只能有一個結果，不允許「A 或 B」。
3. **每一條規則都有來源與狀態。**
   - `decided`：現有文件找得到依據，`source` 指向原文。
   - `proposed`：文件沒寫，這是草擬的提案，`question` 指向 `OPEN-QUESTIONS.md` 的題號。人裁決後才改成 `decided`，並把決定寫回 `decision-log.md`。
4. **規則的順序就是檢查的順序。** 依 D-024 的標準順序：角色 → 載入 → 院區範圍 → 領域規則。
5. **不出現實作詞彙。** 沒有類別名、方法名、HTTP 路徑、狀態碼、資料表。這些屬於實作端的對應表。
6. **範例只透過 use case。** `given` 用其他 use case 準備資料，`observe` 用查詢 use case 看結果，不讀資料表。
7. **用到尚未納入規格的 use case 時，用 `needs` 標出。** 執行器遇到還沒實作的 use case 就跳過該範例，跳過不算通過。

## 範例檔的其他欄位

| 欄位 | 意思 |
|---|---|
| `basis` | 這個範例驗證哪些規則 id。只要其中一條是 `proposed`，範例就還不能當驗收依據 |
| `derived_from` | 轉換自現有文件的哪個 `EX-` 或 `TS-`，供追溯用 |
| `fixtures` / `use` | 可重複使用的準備步驟 |
| `needs` | 依賴哪些尚未納入規格的 use case |
| `input_pending` | 這一步的輸入要等另一份規格完成才寫得出來 |
| `expect.output` | 部分比對：只比列出的欄位。集合不比順序 |
| `not_covered` | 目前無法用範例驗證的規則與原因 |

## 型別詞彙

| 型別 | 意思 |
|---|---|
| `id` | 不透明的識別字串。範例裡不能假設它的格式 |
| `text` | 字串。`required` 表示不可為 null，也不可為空白 |
| `enum <Name>` | 列舉，值定義在 `model.yaml` |
| `set<T>` | 不重複、無順序的集合。空集合與 null 的差別要在屬性上寫明 |
| `decimal(n)` | 十進位數，小數 n 位。範例裡一律寫成字串，例如 `"0.50"` |
| `int` | 整數 |
| `boolean` | 布林 |
| `ref <Aggregate>` | 指向另一個 aggregate 的 id。`external: true` 表示資料由外部系統提供 |

## 結果詞彙

| 結果 | 意思 |
|---|---|
| `SUCCESS` | 狀態已改變（或查詢有結果） |
| `REJECTED` + `code` | 沒有任何狀態改變，也沒有 event |

錯誤代碼的分類（給實作端決定 HTTP 狀態碼用）定義在 `usecases.yaml` 的 `error_codes`。

## 範例裡的變數

`projectId` 由系統產生，範例無法事先知道。所以步驟可以用 `save` 把輸出存成變數，後面用 `${名稱}` 引用：

```yaml
given:
  - run: CreateReviewProject
    as: manager_h1
    input: { name: "門診品質審查", medicalRecordTypes: [OUTPATIENT] }
    save: { p: projectId }
when:
  run: ActivateReviewProject
  as: manager_h1
  input: { projectId: "${p}" }
```
