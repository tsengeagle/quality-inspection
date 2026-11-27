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

現況欄是 2026-11-12 的狀況；P1、P4、P6、P9、P10 於 2026-11-18 更新（必要檢查生效、及格線在 PR 上驗證，見第六節「及格線的驗證紀錄」）；P1、P2、P5、P6、P8、P10 於 2026-11-26 補上現況（`main` = `5d1f462`，數字的來源見第六節「2026-11-26 的核對」）。P4 的 2026-11-26 現況由 D-097 的修法 PR（#197）更新，這裡沒有動。

| 義務 | 在本專案用什麼落實 | 現況 |
|---|---|---|
| P1 negative fit | `./gradlew check`（編譯、測試、ArchUnit）在 CI 必過 | 2026-11-13：閘門與 CI 已建立，本機驗證過會擋。2026-11-18：`check` 是 `main` 的必要檢查；#87 在 PR 上手寫分層違規，`check` 失敗（ARCH-01、ARCH-04），PR 無法合併。2026-11-26：架構規則 ARCH-01～ARCH-05；範例執行器與 HTTP 契約的比對也在 `check` 裡；已知失敗清單為空 |
| P2 manifestation | 每條架構規則配一個故意違規的樣本 | 2026-11-13：ARCH-01 到 ARCH-04 都有違規樣本與合規對照；把四條規則改成永遠通過後，樣本測試全部失敗。已知失敗清單的閘門沒有樣本，只有手動弄壞的驗證紀錄。2026-11-26：ARCH-05 也有違規樣本與合規對照（`samples/layering/reporting/`）；ARCH-06 與它的樣本在 #194，還沒合併。範例執行器、HTTP 契約比對器、判決程式各有自己的測試，但「把偵測器改成永遠通過，測試會不會失敗」只對 ARCH-01～04 做過 |
| P3 viscosity | 分層規則寫成 ArchUnit；慣例寫成 pattern 文件 | 只有文件（`AGENTS.md`、`repo-hard-rules.md`） |
| P4 stable context | 修程式的 PR 不得同時改 law，由 CI 檢查 | ⚠️ `harness-core/law_check.py` 會擋修法 PR 夾帶程式修改；`law-check` 已在 GitHub 上判決過建構 PR（#10）與修法 PR（#11、#15）。2026-11-18 起是必要檢查，紅燈不能合併（#88） |
| P5 repair locality | 四層套件加依賴方向規則；每次都跑全套測試 | 有分層，未強制。2026-11-26：分層方向由 ARCH-01、ARCH-04 強制（凍結基線各剩 1、2 筆舊違規）；問題側的新檔案要先登記進涵蓋清單（D-060、D-089）。「改了不相干的測試」仍然沒有偵測 |
| P6 tradition-bearer | 修法要有負責人的簽章；核准者清單讀合併目標分支上的版本 | ⚠️ 2026-11-14 啟用並驗收：沒有簽章的修法被 `law-check` 擋下，負責人用 YubiKey 簽章後通過（PR #15）。2026-11-18：`law-check` 是必要檢查；#88 的修法紀錄完整但沒有簽章，`law-check` 只有簽章一項未通過，PR 無法合併。核准者登記了兩支實體金鑰上的三把金鑰（第二支尚未在 GitHub 上簽過修法）。限制：簽章只證明持有金鑰的人同意，不證明看過內容。CODEOWNERS 只保留形式（`07-github-settings.md` 第二節）。2026-11-26：`law/amendments/` 有 86 筆修法紀錄、83 個核准檔 |
| P7 detection cost | 本機跑單元測試，CI 跑全套，人審決策 | 無分級 |
| P8 residue of judgment | `spec-draft/` 的 `OPEN-QUESTIONS.md` 和 `proposed` 標記 | 已有。2026-11-26：規格的規則有 `status`（`decided`／`proposed`），`proposed` 的規則要指向 `OPEN-QUESTIONS.md` 的題目；未裁決的題目與順序記在 `25-topic-backlog.md`。各 `OPEN-QUESTIONS.md` 目前還有幾題，這次沒有數 |
| P9 scaffolding | CI 檢查設為必要，不可跳過 | 2026-11-18：ruleset `main*` 生效，`check`、`law-check`、`law-selftest`、`frontend-check` 為必要檢查，分支須與 `main` 同步，只允許 merge commit；紅燈時 repo 管理員也不能合併（#77 驗證 `frontend-check`；#87、#88、#89 由 API 看到 `mergeable_state: blocked`）。閘門內部：被跳過的測試、沒有測試原始碼的執行都會失敗 |
| P10 formal picture | 驗收範例實際執行；規則編號可追到測試 | ⚠️ 範例可執行（`11-example-runner.md`）：2026-11-18 共 74 個（review-project 38、score-sheet 36），全部通過；#89 新增規則與範例、不改程式，`check` 失敗。只涵蓋 2 個 aggregate，且只執行到 use case 層，HTTP 回應格式不在範例內（見 `12-escaped-defects.md`）。2026-11-26：範例 438 個（review-case 291、review-project 55、sampling-batch 50、score-sheet 42），全部通過；規格的規則 384 條，其中 35 條沒有任何範例涵蓋（`spec_lint.py` 列出，不擋）。HTTP 回應由契約涵蓋：4 份 `http.yaml` 共 52 個端點，封閉的形狀，每種形狀都要被觀察到（D-061、D-088）。後端問題側 254 個檔案，涵蓋清單 198 列全部是已立法，沒有暫准的範圍（D-089）。仍然沒有的：前端規格未納入（D-060 第 2 點）；前後端各自持有的 API 契約沒有互相比對 |

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

**2026-11-14 補註**：下面這段的前提（用 CODEOWNERS 和 branch protection 取代）在單人 repo 不成立，見 `07-github-settings.md` 第二節。取代方案的設計草稿在 `08-legislation-design.md`：防線放在 CI 與簽章。

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

## 六、分期與進度

狀態欄是 2026-11-14 傍晚的狀況（PR #15 合併之後），第 2、4 期與下方兩張表於 2026-11-18 更新；第 4、5 期與「不在 7 期裡」的表於 2026-11-26 更新（見本節最後的「2026-11-26 的核對」）。**立法題目的即時進度不在這張表，在 `25-topic-backlog.md`。**

主線共 7 期。先做 P1、P6、P2，其他義務都建立在它們上面。第 2 期原本是「CODEOWNERS 加 branch protection」；單人 repo 無法自我核准（見 `07-github-settings.md` 第二節），改由修法機制取代，拆成 4 個子期（設計見 `08-legislation-design.md`）。

| 期 | 內容 | 對應義務 | 狀態 |
|---|---|---|---|
| 1 | CI 跑 `./gradlew check`、ArchUnit、已知失敗清單 | P1 | 完成，已進 `main`（PR #6） |
| 2 | 律法保護 | P6、P4 | 進行中。2-1、2-2 的主要部分已完成並啟用；2-3、2-4 未動工 |
| 2-1 | 清冊、判決程式、GitHub 接法（不驗簽章） | | 完成，已進 `main`（PR #7、#8、#9）。`law-check` 在 GitHub 上的執行紀錄見 `08-legislation-design.md` 第十一節 |
| 2-2 | 把核心搬進 `harness-core/`；簽章核准，支援多種簽章方案 | | **主要部分完成並已啟用**（PR #12 到 #15）。修法要有負責人用實體金鑰的簽章才會通過，完整流程已在 GitHub 上驗收（PR #15）。備援金鑰：同一支實體金鑰上的 resident 金鑰（2026-11-15，防換電腦）；第二支實體金鑰 YubiKey 5 Nano（2026-11-18，防金鑰遺失，PR #74，尚未在 GitHub 上用它簽過修法）。未完成：SSH 以外的簽章方案（OpenPGP、自然人憑證）；把 `harness-core/` 拆成獨立的 repo |
| 2-3 | GitLab 接法（以 GitLab.com 為起點） | | 未動工。要先查證怎麼防止合併請求改掉判決程式 |
| 2-4 | 各工具的告知文字與本機提醒（Copilot、Codex、Kiro、Claude Code） | | 未動工 |
| 3 | 每條規則配一組違規樣本 | P2 | 完成，已進 `main`（PR #7、#8） |
| 4 | 讓 `specs/*/examples.yaml` 變成可執行的測試 | P10 | 執行器完成（2026-11-15，`11-example-runner.md`）。2026-11-18：74 個範例，只涵蓋 2 個 aggregate。2026-11-26：438 個範例全部通過，已知失敗清單為空；規格有 review-project、score-sheet、sampling-batch、review-case、review-submission 五個目錄；格式到 v2.1（請求端的範例，D-074）；HTTP 契約 52 個端點；後端問題側沒有暫准的範圍（D-089）。未涵蓋：35 條規則沒有範例；前端 |
| 5 | 維護義務對照表（本文件第三節） | 全部 | 持續進行。2026-11-26 更新過一次；題目清單把「訂定本專案 harness 的有界主張並更新義務對照表」排在目前的題目之後 |
| 6 | 把在地傳統寫成模式語言：先挑一支範本 use case，為它涉及的每個模式補齊準則、範本、禁則 | P3 | 未動工。D-048 的 CQRS 閘門規則屬於這一期。這是 P3 的主體，不是可有可無的便利功能 |
| 7 | 「AI 依規格逐步產生程式」的工作流程（相當於 `/execute-uc`） | — | 未動工，排在最後。論文把工作流程歸在律法，它決定生成的順序；harness 只負責在閘門沒過時不讓它結束 |

接下來的順序（2026-11-14 的建議，尚未裁決；2026-11-18 更新各項狀態）：

1. 把 `check`、`law-check`、`law-selftest` 設為必要檢查：已完成（2026-11-18，另加 `frontend-check`）。
2. 登記備援金鑰：已完成（2026-11-15 同一支金鑰上的 resident 金鑰；2026-11-18 第二支實體金鑰，PR #74）。
3. 盤點律法清冊：已盤點（`09-law-inventory.md`）。2026-11-14 裁決：測試與規格要列為律法，但要等 `/specs` 格式完整、問題側規格穩定之後才生效。2026-11-26：`specs/`、`spec-draft/`、架構規則的測試（`architecture/`）與驗收範例的執行器（`acceptance/`）已在清冊裡；其餘的測試還沒有列入，`09-law-inventory.md` 的其他缺口這次沒有逐一核對。
4. 把 `harness-core/` 拆成獨立的 repo，接著做 2-3（GitLab）。
5. 第 4 期：讓驗收範例可以執行。已完成（見上表第 4 期）。

不在這 7 期裡、但已經提到的工作：

| 項目 | 狀態 |
|---|---|
| 合併 PR #6、PR #7 | 完成（2026-11-14）。PR #7 當時合併進了 `harness/p1-gate` 而不是 `main`，由 PR #8 補進；PR #9 是三項修正 |
| GitHub 設定：把 `check`、`law-check`、`law-selftest` 設為必要檢查，不留繞過權限，禁止 force push | 已完成（2026-11-18，#75、#78、#79；另加 `frontend-check`）。步驟在 `07-github-settings.md` 第一節；檢查本身壞掉時的處置在第四節 |
| `/specs` 的格式與 `spec-draft/` 的升格（本文件第二節） | 格式 v1 已定（2026-11-15，`specs/README.md`，決定見 `10-spec-format.md`），由 `spec-check` 機械檢查。ReviewProject 2026-11-15 升格到 `specs/review-project/`（裁決 D-049）。2026-11-26：格式 v1.1（D-063）、v2（D-065）、v2.1（D-074）、HTTP 契約（D-061、D-088、D-094 的 `transition`）；五個規格目錄都在 `specs/` |
| `reporting` 搬進 `application` 的查詢側（D-048） | 未搬。2026-11-26：`reporting` 套件還在；入口已改成 use case 並立法（D-087），ARCH-05 限制它對 `application` 只能依賴 `application.common` |
| 測試分級：本機跑快的，CI 跑全套 | P7，未排期 |
| 一組不交給建構者的獨立測試 | 未排期 |
| 正式資料庫與 migration 工具 | 未決定。卡住樂觀鎖（D-045）與「測試跑真實資料庫」 |
| 驗證 harness 本身（`04-definition-of-enough.md` 的及格線四項） | 2026-11-18：第 1、2、4 項在 PR 上驗證通過（#87、#88、#89）；第 3 項沒有另開 PR，沿用 2026-11-13 的本機驗證紀錄。見下方「及格線的驗證紀錄」。第 2 項的形式是簽章加必要檢查，不是 code owner 核准。2026-11-27：對 2026-11-18 之後新增的偵測器重做，27 組實驗裡 3 組沒有擋下（建置的測試閘門、驗收測試的接線、以整個目錄登記的涵蓋範圍），見「及格線對新的偵測器重做」 |

### 及格線的驗證紀錄（2026-11-18）

三個驗證用 PR 都以 `main` 的 `3fa719e` 為基礎，本機先重現失敗再推送，記錄結果後關閉，未合併、未簽章。

| 項 | PR | 做法 | `check` | `law-check` | 合併狀態 | 結論 |
|---|---|---|---|---|---|---|
| 1 手寫違規 | #87 | 在 `interfaces/rest` 新增類別，直接使用 `infrastructure.id.UuidIdGenerator` | 失敗：`ArchitectureRulesTest#arch01LayerDependencies`、`LayerImportRuleTest#arch04_mainSourcesMatchFrozenBaseline` 不在已知失敗清單上（Linux 與 Windows 都失敗） | 通過（建構 PR） | `blocked` | 通過 |
| 2 改律法沒有核准 | #88 | 修改 `decision-log.md`，附格式完整的修法紀錄，不附簽章 | 通過 | 失敗：其餘 9 項通過，只有「有 1 個有效的核准簽章：有效 0 個」未通過 | `blocked` | 通過 |
| 3 弄壞偵測器 | — | 沒有另開 PR；2026-11-13 本機驗證過（`05-current-state.md` 第十節），樣本測試在每次 `check` 都執行 | — | — | — | 沿用本機紀錄 |
| 4 改需求不改程式 | #89 | 在 `specs/review-project` 新增假想規則 `RP-CRT-04`（名稱超過 20 字要拒絕）與一個範例 | 失敗：`AcceptanceExamplesTest#review-project/rejects_a_project_name_longer_than_20_characters` 不在已知失敗清單上 | 失敗（`specs/` 是律法，沒有簽章；不是本項驗證的對象） | `blocked` | 通過 |

限制：

- 合併狀態是由 GitHub API 讀到的 `mergeable_state`，沒有另外開 PR 頁面看合併按鈕。#77 曾在頁面上確認紅燈時按鈕無法使用。
- 及格線只證明「最小可用」。之後的指標是漏網率（`04-definition-of-enough.md` 第二節），漏網紀錄見 `12-escaped-defects.md`。

### 2026-11-26 的核對

`main` = `5d1f462`（PR #196 合併後）。下面是這次實際執行或查詢的結果；沒有重新核對的項目照實寫出。

| 項目 | 結果 | 怎麼得到的 |
|---|---|---|
| 後端測試 | `./gradlew test` 通過，已知失敗清單為空 | 本機 JDK 17，分支 `claude/build-shrink-arch-baseline`（`main` 加 #198 的兩個提交） |
| 驗收範例 | 438 個，失敗 0、跳過 0：review-case 291、review-project 55、sampling-batch 50、score-sheet 42 | 同一次執行的測試結果檔 |
| 規格的規則 | 384 條：review-case 249、review-project 47、score-sheet 39、sampling-batch 37、review-submission 12；沒有範例涵蓋的 35 條 | `spec_lint.py`，結果通過 |
| HTTP 契約 | 52 個端點：review-case 29、review-project 9、sampling-batch 7、score-sheet 7 | 數 `specs/*/http.yaml` 的 `usecase:` |
| 涵蓋清單 | 198 列，全部 `legislated`，`grandfathered` 0 列；問題側 254 個 `.java` 檔 | 讀 `law/coverage.txt`、數檔案。沒有逐檔比對「每個檔案都對得到一列」，那由 `law-check` 在每個 PR 上判 |
| 修法紀錄 | 86 筆，核准檔 83 個；裁決到 D-095 | 數 `law/amendments/` |
| 判決程式的測試 | 203 個通過 | `python3 -m unittest discover -s harness-core/tests`（本機裝了 `ssh-keygen` 之後） |
| 前端 | typecheck 通過；單元測試 236 個通過；E2E 18 個通過、3 個略過 | 本機，2026-11-26 在 #192 的分支上（已合併）。之後沒有再跑 |
| 凍結基線 | ARCH-01：1 筆；ARCH-02：3 筆；ARCH-03：1 筆；ARCH-04：2 筆 | 讀 `main` 上的基線檔。#198 把 ARCH-02、ARCH-03 清空，還沒合併 |
| CI 的檢查 | `backend-check`、`frontend-check`、`law-check`、`law-selftest`、`spec-check` 五個 workflow | 讀 `.github/workflows/`。哪些是必要檢查，沿用 2026-11-18 的紀錄，這次沒有查 GitHub 的設定 |

這次沒有重新核對的：

- 第 2 期的 2-2 欄（簽章方案、備援金鑰、第二支金鑰有沒有在 GitHub 上簽過）。
- 第六節「及格線的驗證紀錄」。2026-11-18 之後新增的偵測器（範例執行器的 v2、HTTP 契約比對器、涵蓋清單、ARCH-05）沒有重做一次及格線的四項。
- 漏網紀錄（`12-escaped-defects.md`）停在 2026-11-18 的 E-01～E-03。之後有沒有新的漏網，這次沒有查。
- `05-current-state.md` 的更新範圍，見該檔的說明。

2026-11-26 還在等簽章、沒有合併的：D-096（#194，ARCH-06）、D-097（#197，建構要宣告依據的裁決）。這兩項的現況等合併後再寫進上面的表。


### 及格線對新的偵測器重做（2026-11-27）

題目清單「及格線對新的偵測器重做一次」的實驗。`main` = `f9fe97e`，本機 JDK 17，在獨立的 worktree 裡做，每個實驗做完還原，**沒有任何修改被提交**。驅動腳本在 `docs/review/tools-2026-11-27/gate_mutations.py`。

每個實驗只做一處修改，然後跑完整的 `./gradlew test`（後端）或 `python3 -m unittest discover -s harness-core/tests`（判決程式、規格檢查），看閘門有沒有擋下。對照組（不做任何修改）通過：執行 1538 個測試方法，失敗 0。

#### 弄壞偵測器（及格線第 3 項）

| # | 偵測器 | 怎麼弄壞 | 結果 | 被什麼抓到 |
|---|---|---|---|---|
| J01 | ARCH-05 | 規則改成永遠不會違規 | 擋下 | `ArchitectureRuleSamplesTest`（違規樣本）1 個 |
| J02 | 範例執行器 | 不檢查 SUCCESS／REJECTED 與錯誤代碼 | 擋下 | 執行器自己的測試 12 個 |
| J03 | 範例執行器（格式 v1） | 不比對輸出 | 擋下 | 執行器自己的測試 3 個 |
| J04 | 範例執行器（格式 v2） | `output`、`absent`、`only` 有問題也不報 | 擋下 | 執行器自己的測試 12 個 |
| J05 | 範例執行器 | 不檢查事件 | 擋下 | 執行器自己的測試 7 個 |
| J06 | 範例執行器 | 執行到沒有設定的埠也不報 | 擋下 | 執行器自己的測試 1 個 |
| J07 | 範例執行器 | 不檢查 HTTP 狀態碼 | 擋下 | 執行器自己的測試 5 個 |
| J08 | HTTP 契約比對器 | 不看任何回應 | 擋下 | 比對器自己的測試（失敗 34 個） |
| J09 | HTTP 契約比對器 | 涵蓋要求永遠沒有問題 | 擋下 | 比對器與報告的測試 9 個 |
| J10 | HTTP 契約比對器 | 形狀永遠相符 | 擋下 | 比對器自己的測試（失敗 24 個） |
| J13 | HTTP 契約報告 | 問題數永遠寫 0 | 擋下 | 報告的測試 4 個 |
| J11 | HTTP 觀察器 | 不記錄任何回應 | 擋下 | 建置的閘門：52 個端點「沒有觀察到任何成功回應」。沒有任何測試失敗 |
| J12 | 驗收測試的接線 | 列出每個範例，但不執行 | **間接擋下** | 只有 HTTP 契約的涵蓋要求（21 個問題）。沒有任何測試失敗 |
| J14 | 驗收測試的接線 | 名稱以 `rejects` 開頭的範例列出但不執行 | **沒有擋下** | 建置通過：執行 1538 個測試方法，失敗 0 |
| G01 | 建置的測試閘門 | 不在清單上的失敗不擋；同時放一個會失敗的測試 | **沒有擋下** | 建置通過，訊息是「執行 1539 個測試方法，其中失敗 1（清單 0）」 |
| P01 | 判決程式 | 涵蓋清單以外的新範圍不擋 | 擋下 | `CoverageTest` 2 個 |
| P02 | 判決程式 | 暫准範圍的修改不提醒 | 擋下 | `CoverageTest` 3 個 |
| P03 | 規格檢查 | 不檢查規則的出處 | 擋下 | `test_spec_lint` 8 個 |
| P04 | 規格檢查 | 不檢查禁用詞彙 | 擋下 | `test_spec_lint` 1 個 |
| P05 | 規格檢查 | 不檢查範例是否可區分 | 擋下 | `test_spec_lint` 6 個 |
| P06 | 規格檢查 | 不檢查 `http` 步驟的端點是否在契約裡 | 擋下 | `test_spec_lint` 1 個 |

#### 改需求不改程式（及格線第 4 項）

| # | 修改 | 結果 | 被什麼抓到 |
|---|---|---|---|
| R01 | review-case 的範例：稽核 `detail` 的預期值從 5 改成 6 | 擋下 | `AcceptanceExamplesTest#review-case/closes_without_rereview` |
| R02 | HTTP 契約：提交初審的回應多要求一個欄位 | 擋下 | 契約閘門：「缺少欄位 extra」 |
| R03 | HTTP 契約：提交初審的回應拿掉 `totalScore`（程式仍然輸出） | 擋下 | 契約閘門：「契約沒有的欄位 totalScore」 |

#### 手寫違規（及格線第 1 項）

| # | 修改 | 結果 | 被什麼抓到 |
|---|---|---|---|
| V02 | 提交初審的回應 dto 多輸出一個欄位 | 擋下 | 契約閘門：「契約沒有的欄位 extra」 |
| V03 | `reporting` 引用 `application.common` 以外的 application 類別 | 擋下 | `ArchitectureRulesTest#arch05ReportingUsesOnlyApplicationCommon` |
| V01 | 在問題側新增沒有登記的檔案（`interfaces/rest`、`interfaces/rest/dto`、新的 `application` 與 `domain` 子套件、`reporting`、`application/common`、`domain/common`、`application/reviewcase`、`domain/reviewcase`、`application/sampling`） | 擋下 | `law_check`：「問題側的修改都在涵蓋清單內」未通過 |
| V01b | 在**以整個目錄登記**的範圍新增檔案（試了 `application/project`、`domain/project`、`application/scoresheet`、`application/audit`） | **沒有擋下** | `law_check` 通過 |

V01、V01b 是把檔案加在 `main` 的樹上做成一個沒有推送的提交，再跑 `law_check.py`。`infrastructure` 不是問題側，新增檔案會通過，這是設計如此。

#### 沒有擋下的四件

1. **建置的測試閘門沒有自己的測試（G01）**。`build.gradle` 裡核對已知失敗清單的那一段，被改成不擋之後沒有任何東西會發現。第三節 P2 早就記著「已知失敗清單的閘門沒有樣本，只有手動弄壞的驗證紀錄」，這次確認仍然如此。它受律法保護（`build.gradle` 是 gate，修改要簽章），所以風險是「核准者沒有看出來」，不是建構者可以自己改。
2. **驗收測試的接線沒有自己的測試（J12、J14）**。`AcceptanceExamplesTest` 把範例交給執行器的那一行拿掉，範例就只是被列出來、每一個都算通過。全部不執行時，是 HTTP 契約的涵蓋要求間接發現的（J12）；只跳過一部分、而那一部分剛好不影響契約的涵蓋時，沒有任何東西發現（J14）。四份 `examples.yaml` 裡名稱以 `rejects` 開頭的範例有 216 個。同樣受律法保護（`acceptance/` 是 gate）。
3. **涵蓋清單有六個範圍是以整個目錄登記的（V01b）**：`domain/project/`、`application/project/`、`domain/scoresheet/`、`application/scoresheet/`、`application/audit/`、`interfaces/config/`。在這些目錄裡新增檔案（例如一支新的 use case）不需要先立法。其他範圍是逐檔登記（D-089 第 6 點對 `interfaces/rest` 的作法），擋得住。這一項**不受律法保護**：建構 PR 就做得到。
4. 第 2、3 項的共同點：範例執行器、契約比對器本身的邏輯都有測試，弄壞任何一處都會被抓到；沒有被測試的是「把它們接起來」的那一層，以及建置腳本。

#### 這次沒有做的

- 及格線第 2 項（改律法沒有核准）沒有重做。修法機制的測試在 `harness-core/tests`（203 個），2026-11-18 之後新增的只有涵蓋判決，P01、P02 已經涵蓋。
- ARCH-06（#194）、建構依據的裁決（#197）還沒有合併，沒有納入。#197 的 24 個測試是隨實作一起寫的，沒有做過弄壞的實驗。
- 前端的閘門（typecheck、單元測試、E2E）沒有做任何實驗。
- 每個偵測器只挑了幾處弄壞，不是窮舉。擋下代表「這幾處有測試看著」，不代表每一處都有。
- 實驗都在本機，沒有在 CI 上重現。

### 已裁決：未立法的問題側先停下來（D-060，2026-11-18）

起因：2026-11-18 發現的 3 個缺陷都落在可執行律法照不到的地方（`12-escaped-defects.md`）。原本的計畫是先拿少量 aggregate 驗證 harness，但之後的實作逐漸延伸到未立法的範圍，而且沒有任何機制提醒這件事。

提案：

| 元件 | 內容 | 類別 |
|---|---|---|
| 涵蓋清單 | 程式路徑對應到 aggregate，標記「已立法」或「未立法（暫准）」。現有未立法範圍全部列為暫准，之後每立法一個 aggregate 就移除 | 律法（gate），修改要簽章 |
| 判決 | PR 改到暫准範圍：通過，報告列出「本 PR 改了未立法的 X」；改到清單以外的新範圍：失敗，要先立法或以修法把它加進清單 | 閘門 |
| AI 指示 | 動到未立法的問題側之前先停下來，向負責人說明「這裡沒有律法，要先立法，還是暫准實作」 | 傳統（2-4 期） |

負責人 2026-11-18 裁決：採「擋新增」。問題側包含前端規格，但先完成後端 harness，前端之後再納入。D-060 的實作：`law/coverage.txt`（生效時問題側 256 個檔案：已立法 50、暫准 206），判決程式 `harness-core/law_check.py` 0.4.0 以清冊的 `@coverage` 啟用，測試是 `CoverageTest` 的 13 個。

這個機制擋不到的：已立法 aggregate 的介面層缺口（範例只到 use case 層），以及 `/specs` 以外的裁決變更。前者要把回應格式納入範例或另做比對閘門；後者要靠擴大立法範圍。

D-060 在真實 PR 上的驗證（2026-11-19，交接 H-3）：#99 修改暫准範圍的 `ReviewProjectController`。CI 的 `law-check` 判為通過，報告列出「未立法範圍的修改」，並在 GitHub 上顯示為 `warning` annotation：「ReviewProject 的 HTTP 介面（use case 已立法、HTTP 未立法）：…/ReviewProjectController.java」。行為與本機模擬一致。

2-2 的簽章方案：判決程式做成可抽換，核准檔帶 `scheme` 欄位，每種方案一個驗證器。已實作並啟用的是「SSH 簽章加實體金鑰」。其次是「X.509 加自然人憑證」，要先查證憑證內的個資、到期與廢止、命令列環境三件事；OpenPGP 視需要；檔案金鑰不建議當核准用。

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
