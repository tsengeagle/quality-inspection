# 修法機制

本目錄是這個專案的律法資料：律法清冊與修法紀錄。判決程式在 `harness-core/`，它是不分專案的核心，說明見 `harness-core/README.md`。設計與分期見 `docs/harness/08-legislation-design.md`。

**目前的狀態**：判決程式已經能驗核准簽章，但這個專案的清冊還沒有啟用它，所以修法紀錄仍然不代表有人核准。啟用要等負責人的公鑰登記進來；步驟見 `harness-core/APPROVING.md`。

## 檔案

| 檔案 | 用途 |
|---|---|
| `manifest.txt` | 律法清冊：哪些路徑是律法、屬於哪一類 |
| `amendments/` | 修法紀錄，每次修法一個 JSON 檔 |

## 判決規則

`harness-core/law_check.py` 比較 base 與 head 兩個 commit，清冊一律讀 base 上的版本。

| 這次修改的內容 | 判決 |
|---|---|
| 沒有動到律法 | 通過（建構 PR） |
| 只把基線檔縮短（只刪除行） | 通過，免修法 |
| 動到律法 | 修法 PR，下列全部成立才通過 |

修法 PR 的檢查項目：

1. 有且只有一筆新增的修法紀錄。
2. 既有的修法紀錄沒有被修改或刪除。
3. 紀錄的 `id` 與檔名一致，`reason` 不是空的。
4. 改動的律法路徑都在紀錄的 `paths` 範圍內。
5. `decision` 是 `none`，或是 `decision-log.md` 裡存在的編號。裁決可以在同一個 PR 裡新增。
6. 沒有夾帶律法與文件以外的修改。修法與改程式要分成兩個 PR。

報告會列出沒有評估的項目。清冊啟用核准簽章之前，固定有一項：核准簽章。

結束代碼：`0` 通過，`1` 未通過，`2` 無法評估。無法評估不算通過。

## 怎麼修法

1. 修改律法檔案。
2. 在 `law/amendments/` 新增一個紀錄檔，檔名是 `<id>.json`：

   ```json
   {
     "id": "2026-11-14-arch-05",
     "decision": "D-048",
     "reason": "新增 ARCH-05，限制 reporting 對 application 的依賴",
     "drafted_by": "claude-code",
     "paths": [
       "docs/01-requirements/decision-log.md",
       "services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/architecture/"
     ]
   }
   ```

   - `decision`：對應的裁決編號。純粹修正閘門、沒有對應裁決時填 `none`，並在 `reason` 說明。
   - `paths`：這次修法涵蓋的律法路徑。寫法與清冊相同：`*` 比對一層，`**` 可跨目錄，結尾是 `/` 表示整個目錄。
   - `drafted_by`：誰起草的，僅供紀錄，不檢查。
3. 同一個 PR 可以一起改 `docs/` 與其他 `.md`，不能改程式。
4. 推送前在本機先跑一次（見下）。

把新規則抓到的既有違規凍結進基線，算律法修改，可以放在同一個修法 PR。

## 在本機執行

```bash
# 檢查目前的分支相對於 main 的修改
python3 harness-core/law_check.py --base origin/main --head HEAD

# 判決程式的測試
python3 -m unittest discover -s harness-core/tests -v
```

人和各種 AI 工具都跑同一個指令。

## 在 CI 上

| 平台 | 檔案 | 狀態 |
|---|---|---|
| GitHub | `.github/workflows/law-check.yml` | 用 `pull_request_target`，判決程式與清冊都取自 base。2026-11-14 第一次在 GitHub 上執行（PR #9）就失敗：私有 repo 上抓不到 PR 的內容。已改寫取得方式；**改寫後的版本還沒有在 GitHub 上執行過**，要等它進到 `main` 之後的下一個 PR |
| GitHub | `.github/workflows/law-selftest.yml` | 判決程式的測試 |
| GitLab | 尚未實作 | 第 3 期。要先查證怎麼讓合併請求改不了判決程式 |

`law-check.yml` 另有一個合併後的後援：推送到 `main` 時，用合併前的判決程式再檢查一次。它只能事後發現。

## 這個機制擋什麼、不擋什麼

擋：

- 修改律法卻沒有留下紀錄。
- 紀錄宣告的範圍小於實際修改的範圍。
- 修法的 PR 夾帶程式修改。
- PR 把某個路徑從清冊拿掉，藉此讓它不算律法。
- 事後改寫舊的修法紀錄。

不擋：

- **偽造核准。** 清冊啟用核准簽章之前，任何人或任何 AI 都能寫出一筆合格的紀錄。
- 直接推送到 `main`，或用管理員權限繞過保護規則。合併後的檢查只會事後報告。
- 內容對不對。機制只管程序，不判斷修改本身合不合理。
- 只縮短基線的修改不需要紀錄，所以「把基線裡的一行刪掉、卻沒有真的修掉違規」不由這裡擋；那會讓 `./gradlew check` 失敗。

## 更名與搬移

更名視為刪除加新增。把律法檔案搬到清冊沒列的路徑，舊路徑的刪除仍然算律法修改。
