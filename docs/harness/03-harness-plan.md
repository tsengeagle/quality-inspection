# 本專案的 harness 計畫

狀態：計畫。2026-11-13 起動工，進度見 `05-current-state.md` 第十節。工具選擇是討論中的建議，不是既有文件規定的做法。

## 一、三個前提的修正

討論一開始對目標的三個限制做了檢視，結論如下。

1. **翻譯表是第二個繫結才有的成本。** 論文的說法是：規格刻意以主版繫結的詞彙撰寫，轉接的成本由第二個繫結承擔。本專案只有一個繫結，而且規格由自己從頭寫，所以沒有翻譯表要維護。規格可以直接用組織自己的詞彙。
2. **真正的翻譯表風險在本 repo 裡。** 目前有三個 law 的候選：`docs/`（散文）、`spec-draft/`（結構化草稿）、`openspec/specs`（空的）。三者並存，就是在維護另一種翻譯表。
3. **「組織整套」現在做太早。** 先在本 repo 做到會擋人。前端 Angular MFE 是另一個技術家族，先不碰。

另一個被評估後擱置的想法：在參考 repo 裡為 ez spec 寫一份一比一對照的「tzuchi spec」。

- 擱置的原因仍然成立的部分：要和 ez spec 逐份對照、長期同步，那就是翻譯表。
- 當時的另一個反對理由**不成立**：「只換成組織的詞彙不夠，規格必須和解法完全分開」。對照論文後，這個要求比 MGD 嚴格。論文明說問題的表徵是依架構而定的，許多專案只需要一個繫結，中立的定義語言反而寫起來不自然。「用符合組織文化的詞彙寫規格」是合乎 MGD 的做法。

### 關於「繫結」這個詞

依論文的定義，本專案要建的 harness 本身就是一個繫結：用自己的機制履行同樣的義務。目標裡「不建第 N 套 binding」實際要避免的是「去翻譯別人的規格」，這一點做得到。

### 規格要不要含實作詞彙

`spec-draft/README.md` 的規則 5 是「不出現實作詞彙」。這是一個可以選的設計，不是 MGD 的要求。兩種選擇的代價：

| 選擇 | 好處 | 代價 |
|---|---|---|
| 規格不含實作詞彙（現行草稿） | 換技術時規格不用改；非工程人員較容易參與 | 需要另一份對應表說明路徑、狀態碼等；論文說這條路尚未測試 |
| 規格使用本組織架構的詞彙 | 寫起來自然；論文的兩個繫結都這樣做 | 之後若有第二種技術棧，要付轉接成本 |

**2026-11-12 裁決：規格應盡量避免太偏向特定實作的詞彙。** 起因是 ezddd 的規格夾著 ez 系列框架的詞彙。現行草稿的規則 5 維持。

「盡量」要有一條可以檢查的線，否則只是指引。同日裁決採用下列三層：

| 層次 | 例子 | 處理 |
|---|---|---|
| 框架、函式庫、程式結構 | 類別名、方法名、框架的型別、HTTP 路徑、狀態碼、資料表名 | 禁止。這就是 ezddd 規格出問題的那一層 |
| 架構概念 | aggregate、use case、event | 只允許 `glossary.md` 有定義的詞。現行草稿已經在用這幾個詞 |
| 業務詞彙 | 專案、院區、評分表、複審 | 規格的主體 |

落實方式：規格的 linter 帶一份禁用詞清單，出現就失敗。這份清單屬於律法。

### 論文的方法是從零重建，本專案是既有程式

論文的做法是從規格重新生成整個後端，每次換模型就從空目錄重來。本專案有既有的程式和 812 個測試。論文沒有處理這種情況。本文件的順序（先對既有程式加閘門、用凍結基線處理舊違規）是為既有程式做的調整，不是論文的做法。

## 二、已裁決：唯一的 law 放在 `/specs`

2026-11-12 裁決：唯一的 law（規格）放在 repo 根目錄的 `/specs`，與 `/docs` 同層。

裁決本身只定了位置。下列配套尚未決定或尚未執行：

| 配套 | 狀態 | 建議 |
|---|---|---|
| `/specs` 的格式 | 未定 | 沿用 `spec-draft/README.md` 的七條格式規則 |
| `spec-draft/` 的內容何時進 `/specs` | 未定 | 不直接改名搬移。`spec-draft/` 是 AI 轉換的草稿，留作待審區；一個 aggregate 通過下方的升格條件後才搬進 `/specs` |
| `decision-log.md` 與 `/specs` 的關係 | 未定 | `decision-log` 是裁決紀錄（為什麼），`/specs` 是現行規則（是什麼）。每條規則的 `source` 指向決策編號；兩者不一致視為缺陷，以 `decision-log` 為準並修正 `/specs` |
| `docs/02-ddd` 到 `docs/04-testing` | 未定 | 某個 aggregate 轉進 `/specs` 之後，`docs/` 對應章節降為說明，開頭加註「規則以 `/specs` 為準」 |
| `openspec/` | 未定 | 目前是空的。只當修法流程用，或拿掉。它底下也有一個 `specs` 目錄，容易和 `/specs` 混淆 |
| 各處的權威定義 | 未改 | `AGENTS.md`、`.ai/governance/authority-sources.md`、`.github/copilot-instructions.md`、`.github/instructions/docs.instructions.md`、`docs/README.md` 都還寫著 `docs/` 是權威中心。不改的話，AI 工具會繼續把 `docs/` 當 law |
| `spec-draft/README.md` 的狀態聲明 | 未改 | 目前寫「草稿，不是 authority」 |

未轉換的 aggregate 在轉換完成前，仍以 `docs/` 為準。這段過渡期要在 `/specs` 的 README 寫明哪些 aggregate 已轉換。

### 從 `spec-draft/` 升格到 `/specs` 的條件（建議）

1. 該 aggregate 的規則全部是 `decided`，沒有 `proposed`。
2. 每條規則的 `source` 指向實際存在的決策或文件段落。
3. 人逐條讀過 `model.yaml`、`usecases.yaml`、`examples.yaml`，不只是回答過 `OPEN-QUESTIONS.md`。
4. 範例的 `basis` 全部指向 `decided` 的規則。
5. 升格由 PR 完成，核准人是 law 的負責人。

`review-project` 的現況（2026-11-12）：第 1 項差一條（RP-PUB-01）；第 3 項未做。人裁決過的只有 `OPEN-QUESTIONS.md` 的題目；原本就標為 `decided` 的規則、全部範例，以及 AI 依裁決同步的修改，都沒有經過人工審閱。

## 三、十條義務對照

現況欄是 2026-11-12 的狀況。

| 義務 | 在本專案用什麼落實 | 現況 |
|---|---|---|
| P1 negative fit | `./gradlew check`（編譯、測試、ArchUnit）在 CI 必過 | 2026-11-13：閘門與 CI 已建立，本機驗證過會擋；CI 尚未設為必要檢查。證據見 `05-current-state.md` 第十節 |
| P2 manifestation | 每條架構規則配一個故意違規的樣本 | 無 |
| P3 viscosity | 分層規則寫成 ArchUnit；慣例寫成 pattern 文件 | 只有文件（`AGENTS.md`、`repo-hard-rules.md`） |
| P4 stable context | 修程式的 PR 不得同時改 law，由 CI 檢查 | 無 |
| P5 repair locality | 四層套件加依賴方向規則；每次都跑全套測試 | 有分層，未強制 |
| P6 tradition-bearer | CODEOWNERS 加 branch protection；本機用 `permissions.deny` | 無 |
| P7 detection cost | 本機跑單元測試，CI 跑全套，人審決策 | 無分級 |
| P8 residue of judgment | `spec-draft/` 的 `OPEN-QUESTIONS.md` 和 `proposed` 標記 | 已有 |
| P9 scaffolding | CI 檢查設為必要，不可跳過 | 無 |
| P10 formal picture | 驗收範例實際執行；規則編號可追到測試 | 有範例，還不能執行 |

之後每加一個機制就更新這張表。寫不出證據的列標 ⚠️，照實留著。

對照論文後，這張表要補的地方：

- **P1、P2**：閘門必須是確定性的程式，判決路徑上不能有語言模型。被跳過或掃描範圍是空的檢查，不能算通過；報告要列出有幾項沒被評估。
- **P3**：論文要求每個模式有四種形式（準則、範本、禁則、閘門），缺一種就視為不存在。只有 ArchUnit 規則而沒有範本和準則，只做到四分之一。但只需要寫下模型的預設無法可靠提供的在地選擇，不必重述通用知識。
- **P4**：論文凍結的是規格、傳統、閘門、框架版本四樣。實際案例是建構者把規格刪短來通過閘門。
- **P5**：論文的防護機制會還原超出修補範圍的修改。實際案例是建構者改寫別的使用案例的測試去接受錯誤。本專案至少要讓「改了不相干的測試」在 PR 上看得出來。
- **P6**：論文的做法是建構者可以起草修訂，但修改在人類接受之前不具效力。這和 PR 加 CODEOWNERS 核准是同一個結構。
- **P10**：`decision-log.md` 記錄的是裁決，屬於主要脈絡；`/specs`、`docs/` 其餘文件、ArchUnit 規則都是代理。需要機械化的核對：規則的出處指向存在的決策、同一條規則在各處一致。另外，前端和後端各自持有一份 API 契約的圖像，論文記錄了兩邊各自全綠卻互相漂移的情況，本專案的 Angular 前端有同樣的風險。
- **表上沒有的一項**：一組不交給建構者的獨立測試，用來判斷閘門全過時行為對不對。

## 四、從 ezddd-java 搬什麼

原則：搬格式和概念，內容自己寫。

| 元件 | 處理方式 | 原因 |
|---|---|---|
| Gate 流程（產生、檢查、修正、再檢查） | 搬概念 | 和技術棧無關 |
| 規則檔格式（id、pattern、target、severity、authority_ref） | 搬 | 格式通用，`authority_ref` 讓每條規則指回出處 |
| 每條規則配一個違規樣本 | 搬 | 落實 P2 |
| 驗收格式 | 搬 | 黑箱格式；`spec-draft` 的範例已經很接近 |
| 完成報告的分類（SPEC_GAP 等） | 搬 | 落實 P8 |
| 義務對照表 | 搬 | 當作自我檢查表 |
| Stop hook 加 marker 加 checksum | 改寫成輕量版，或不做 | 有 CI 之後它只是加快回饋的輔助 |
| 57 條規則的內容 | 自己寫 | 幾乎全是 ez 框架的 import 和註解 |
| Pattern 文件、範例、模板 | 自己寫 | 內容是 Aggregate、Event Sourcing、Outbox |
| UC spec 的 JSON 格式 | 不搬 | 混了解法詞彙 |
| `ezgo-law-guard.py` 與修法窗口 | 不搬 | 用 CODEOWNERS 和 branch protection 取代 |
| 雙 profile 測試、Outbox 相關規定 | 不搬 | 本專案沒有這個架構 |

law guard 不搬的原因：它存在是因為參考 repo 沒有 CI 和 PR 流程，所有防護只能在本機做，而本機上 AI 和人是同一個 OS 使用者。本專案有 GitHub 和 PR 流程，同一條義務用伺服器端的設定就能落實，而且更強。

## 五、工具選擇

| 用途 | 工具 | 理由 |
|---|---|---|
| 架構規則 | ArchUnit，隨 `./gradlew check` 執行 | 看編譯後的依賴，不用窮舉寫法；Copilot 和人寫的程式也擋得到 |
| 既有違規 | `FreezingArchRule` | 凍結成基線，只擋新增的 |
| 少數文字規則 | regex，沿用 ezddd 的 YAML 格式 | 只用在只有一種拼法的東西，例如禁用某個 import |
| 強制執行 | GitHub Actions 跑 `./gradlew check`，設為必要檢查 | 開發者、AI、CI 跑同一個指令 |
| law 保護 | `.github/CODEOWNERS` 加 branch protection | 擋在伺服器端，本機繞不過 |
| 本機提醒 | `.claude/settings.json` 的 `permissions.deny` | 輕量；真正的防線在 GitHub |

CODEOWNERS 的兩個限制：

- 它只管合併進受保護的分支。CI 要用主分支上的規則來跑 gate，不能用 PR 自己帶的版本。
- repo 管理員預設可以繞過，要另外勾選「Do not allow bypassing the above settings」。

Claude Code 的 hook 只管得到 Claude Code。本 repo 同時有 `.github/copilot-instructions.md`，所以主要的 gate 必須放在 build 和 CI，不能只放在 hook。

## 六、建議順序

先做 P1、P6、P2，其他義務都建立在它們上面。

1. **P1**：建 CI 跑 `./gradlew check`；加入 ArchUnit。
2. **P6**：CODEOWNERS 加 branch protection，保護 `decision-log.md`、`spec-draft/`、規則本身。
3. **P2**：為每條 ArchUnit 規則寫一個違規樣本。
4. **P10**：讓 `spec-draft/review-project/examples.yaml` 變成可執行的測試。這是最花工的一項。
5. 在本目錄維護義務對照表（本文件第三節）。
6. 把組織的在地傳統寫成模式語言：先挑一支範本 use case，為它涉及的每個模式補齊準則、範本、禁則（閘門在前面幾步已有）。這是 P3 的主體，不是可有可無的便利功能。
7. **最後**才做「AI 依規格逐步產生程式」的工作流程（相當於 `/execute-uc`）。論文把工作流程歸在律法，它決定生成的順序；harness 只負責在閘門沒過時不讓它結束。

和論文順序的差異：論文是先畫出模式網路，再為每個節點寫四種形式，最後才有工作流程，閘門是每個模式的第四形式。本文件把閘門提到最前面，是因為本專案已經有程式，先擋住新增的違規最划算。這是為既有程式做的調整。

### 第一批 ArchUnit 規則

取自 `design-implementation-gap-2026-11-11.md` §3.7。數字是該報告當時的統計，動工前要重新確認。

- 分層依賴方向（當時有 3 處違規）。
- `domain` 與 `application` 禁用 `Instant.now()`、`UUID.randomUUID()`（當時 12 個檔案）。
- `domain` 與 `application` 禁丟 `IllegalArgumentException`（當時 15 處，會變成 HTTP 500）。

## 七、其餘工作量

| 項目 | 工作量 | 備註 |
|---|---|---|
| 裁決 `proposed` 規則並寫回 `decision-log` | 最大，只有人能做 | ReviewProject 的 6 支 use case 已裁決完；全系統約 49 支 |
| ArchUnit 加驗收範例的執行器 | 中 | |
| 違規樣本；測試改跑真實資料庫與 migration | 中 | 目前是 H2 加 `ddl-auto`；卡在資料庫與 migration 工具未定 |
| 挑一支範本 use case 寫成 pattern 文件 | 中 | |
| 清掉凍結基線裡的舊違規 | 小，持續做 | |
| 驗證 harness 本身 | 每輪都會找到問題 | 用沒看過答案的 agent 從乾淨 repo 照文件做 |
