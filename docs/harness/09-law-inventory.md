# 律法清冊盤點

2026-11-14 盤點。清冊在 `law/manifest.txt`。本文件記錄盤點的方法、找到的缺口、裁決，以及還沒做的事。

## 一、方法

1. 把 repo 裡受版本控制的檔案（779 個）逐一用清冊分類：109 個是律法，670 個不是。
2. 列出 CI 實際執行或讀取的東西，看有沒有不在清冊裡的。
3. 列出「現在不存在、但只要有人新增就會改變閘門行為」的路徑。這一類最容易漏；`gradlew` 就是這樣漏的（PR #9 補上）。

沒有涵蓋的範圍：

- 前端沒有任何閘門，沒有盤點。
- 只檢查了路徑。清冊列了某個檔案，不代表那個檔案裡的每一種寫法都被規則涵蓋。

## 二、應該補進清冊的缺口

狀態：2026-11-14 裁決全部列入，已寫進清冊，隨 PR #17 的修法生效（待簽章與合併）。

| 路徑 | 現在存在嗎 | 為什麼該是律法 |
|---|---|---|
| `services/quality-inspection-service/src/test/resources/archunit_ignore_patterns.txt` | 不存在 | **已實測**：新增這個檔案就能讓 ArchUnit 忽略指定的違規。植入一個分層違規並加上這個檔案後，ARCH-01 不再報出；ARCH-04（import 檢查，不經過 ArchUnit）仍然擋下。ARCH-02、ARCH-03、ARCH-05 沒有第二道防線 |
| `services/quality-inspection-service/src/test/resources/junit-platform.properties` | 不存在 | JUnit 的全域設定，可以改變測試的執行方式 |
| `services/quality-inspection-service/buildSrc/` 與該目錄下其他 `*.gradle` | 不存在 | Gradle 會自動載入並執行 `buildSrc/`；`build.gradle` 也可以引用別的 gradle 檔 |
| `.gitattributes` | 2026-11-15 新增 | 決定換行轉換，會影響基線檔的比對。內容：版本庫與工作目錄一律 LF，只有 `.bat`、`.cmd` 是 CRLF |
| `.github/actions/` | 不存在 | workflow 可以引用 repo 內的自訂 action |
| `.claude/`（含將來的 `settings.json`） | 18 個檔案 | 指令與 skill 是給 AI 的指示；`settings.json` 可以設定 hook 與權限 |
| `.github/java-upgrade/hooks/` | 2 個腳本 | AI 工具會執行的 hook 腳本 |
| `.github/prompts/`、根目錄的 `frontend-ui.instructions.md` | 有 | 給 AI 的指示，和已列入的 `.github/instructions/` 同性質 |

## 三、裁決

### 測試與規格要列為律法，但延後生效（2026-11-14）

負責人裁決：**測試和規格要列為律法，但必須在 `/specs` 的格式完整、問題側的規格穩定之後才上線。**

現在不列入清冊。理由是順序：規格還在從 `docs/` 與 `spec-draft/` 往 `/specs` 轉換，格式與內容都還會大幅變動；這時候就要求每次修改都簽章，會把轉換工作本身卡住。

生效的前提（兩項都成立）：

1. `/specs` 的格式定案。見 `03-harness-plan.md` 第二節的配套表，目前「格式」一列仍是未定。
2. 問題側的規格穩定。至少要有一個 aggregate 通過同一節的升格條件並搬進 `/specs`。

生效之前的現況，照實記下：

| 項目 | 現況 |
|---|---|
| `/specs/`、`spec-draft/` | 已在清冊裡（`spec` 類）。`/specs/` 目前是空的 |
| 規格草案、`glossary.md`、`docs/02-ddd` 到 `docs/04-testing` | 不是律法。`CLAUDE.md` 把它們列為第 2 到第 6 順位的權威，但修改不需要修法紀錄 |
| 測試程式（88 個檔案） | 不是律法，只有 `architecture/` 底下的規則與樣本是。改弱一個既有測試來讓它通過，目前沒有任何機制看得出來 |

「測試」的範圍（2026-11-14 裁決，暫定）：**先採用論文的定義。** 由規格推導出來的驗收測試是律法，屬於問題側，由人擁有；建構者自己寫的測試不是律法，可以自由新增，但不能動驗收範例。

這個定義要等第 4 期（讓驗收範例可以執行）做出來才有實際的對象。

## 四、其他裁決（2026-11-14）

| 項目 | 裁決 |
|---|---|
| 第二節的缺口 | 全部列入清冊 |
| `src/test/resources/application-test.yml`（測試用的 profile） | 列入清冊 |
| `DESIGN.md`、`.editorconfig`、`openspec/config.yaml` | 未裁決，目前不列 |

## 五、順便發現、與律法無關的雜項

- `services/quality-inspection-service/bin/main/application.yml`：看起來是 IDE 的建置輸出被提交進來了。
- 根目錄的 `package-lock.json`、`copilot-session-*.md`、兩份 `design-implementation-gap*.md`：工作過程的產物。
