# ReviewProject 規格的待裁決問題

- 狀態：草稿，2026-11-11 由 Claude Code 整理，尚未經人工審核。
- 來源：把 `docs/` 的現有內容轉成 `usecases.yaml` 時，文件沒寫、寫得不夠精確、或互相矛盾的地方。
- 「程式現況」欄是閱讀程式碼得到的，沒有執行驗證。它只是參考資訊，不是答案；依權威順序，規格要先決定，程式再跟上。
- 裁決後請把決定寫進 `docs/01-requirements/decision-log.md`，再把對應規則的 `status` 改成 `decided`。

## 總覽

`model.yaml` 與 `usecases.yaml` 共有 45 條帶狀態的項目：15 條 `decided`，30 條 `proposed`。也就是說，`ReviewProject` 這 6 支 use case 的合約，**約三分之二在現有文件裡找不到依據**。

17 個範例中，10 個只依賴本次範圍內的 use case；7 個要等評分表的規格完成才跑得動。`basis` 全部是 `decided` 規則的範例只有 2 個（`rejects_create_by_a_non_manager`、`rejects_update_of_an_unknown_project`），其餘 15 個在裁決前都還不能當驗收依據。

## 先看這三題

這三題不只是規格缺口，閱讀程式碼後看起來是功能缺口。

### Q-10 品質線與權重從哪裡設定？

- 文件：D-020 說權重存在 `ProjectVersion`、預設各 0.5、發布時驗證。D-019 與 D-031 用品質線決定是否進複審。但沒有任何 use case 的輸入包含品質線或權重；`api-spec.md` §7.1 的請求範例有 `qualityThreshold`，沒有權重。
- 程式現況：建立專案時版本固定是「品質線為 null、權重各 0.5」。建立與更新的請求 DTO 都沒有這三個欄位。`ProjectVersion.validateWeightsForPublish()` 沒有任何地方呼叫。
- 影響：若以上閱讀正確，**透過正常操作建立的專案品質線永遠是 null，「超過品質線自動進複審」不會發生**，除非資料是直接寫進資料庫的。請先確認這一點。
- 選項：
  1. 品質線與權重是 `CreateReviewProject` 與 `UpdateReviewProjectDraft` 的輸入（建議）。
  2. 另開一支 use case 專門設定計分規則。
- 建議選 1，並把 `RP-PUB-03` 的驗證補上。

### Q-07 已發布的版本要怎麼修改？

- 文件：EX-018 與 `glossary.md` 都說已發布版本不可改，「若需要調整，必須建立新 draft / 新 version」。但 `application-service.md` 沒有任何一支 use case 能建立新的草稿版本。拒絕時的錯誤代碼，TS-PV-004 寫「409 或 422」。
- 程式現況：
  - `ReviewProject.prepareDraftVersion()` 存在，但沒有任何 use case 呼叫它，所以版本號永遠是 1。
  - `updateDraft()` 檢查的是「專案狀態為 DRAFT」，不是「版本未發布」。已發布但尚未啟用的專案，看起來仍然可以被更新。這可能違反 EX-018，請確認。
- 要裁決的事：
  1. 需不需要「建立新草稿版本」這支 use case？本次試作先不納入。
  2. `name`、`description`、`medicalRecordTypes` 算不算「版本內容」？若不算，已發布後仍可修改就不違反 EX-018。
  3. 拒絕時用哪個錯誤代碼。草稿暫用 `INVALID_STATE_TRANSITION`。
- 建議：三個欄位都算版本內容（抽選依賴病歷類型）；拒絕用 `INVALID_STATE_TRANSITION`；新草稿版本的 use case 另案處理。

### Q-09 評分表綁定的對象，以及發布是否需要先綁定

- 文件互相矛盾：`application-service.md` §5 寫輸入是 `scoreSheetVersionId`；`api-spec.md` §7.6 的請求是 `scoreSheetId`。TS-PV-003 把「綁定合法 score sheet version」列為**發布**的前置條件。
- 程式現況：綁定用 `scoreSheetId`，而且不檢查該評分表是否存在或已發布。發布不檢查是否已綁定；**啟用**才檢查。
- 選項：
  1. 綁定評分表「版本」，發布時必須已綁定（照文件，建議）。D-011 要求案件可追溯到評分表版本，綁版本才追溯得到。
  2. 綁定評分表本體，啟用時才檢查（照程式）。
- 草稿依選項 1 寫了 `RP-PUB-02`。

## 其他問題

| # | 問題 | 文件怎麼說 | 程式現況 | 建議 |
|---|---|---|---|---|
| Q-01 | 專案有哪些狀態 | 沒有定義。只在 TS-PV-001 出現 `DRAFT`，錯誤代碼 `PROJECT_NOT_ACTIVE` 暗示有 `ACTIVE` | `DRAFT`、`ACTIVE`、`DEACTIVATED` | 採用程式的三個值，補進 `state-machine.md` |
| Q-02 | 啟用的前置條件 | 沒寫。`permission-matrix.md` §6 只說抽選需要「active + published version」 | 狀態為 `DRAFT`、版本已發布、已綁定評分表 | 狀態為 `DRAFT` 且版本已發布。綁定的檢查移到發布（見 Q-09） |
| Q-03 | 停用後能不能再啟用；草稿能不能直接停用 | 沒寫 | 停用是終點；只有 `ACTIVE` 能停用 | 照程式。但規格草案 §8.1 把它叫「啟用狀態」，聽起來像開關，請向業務確認 |
| Q-04 | 對已啟用的專案再啟用（停用亦同） | `domain-events.md` §4.2 說「由非啟用轉為啟用」才發 event | 回成功，不改狀態；但 use case 仍然發布 event | 回成功、不改狀態、**不發 event**。程式目前會多發一筆稽核紀錄 |
| Q-05 | 建立時的輸入 | `api-spec.md` §7.1 的請求帶 `hospitalId` 與多個 policy；沒說哪些必填 | 院區取自登入者；輸入只有名稱、說明、病歷類型；病歷類型可以是空集合 | 院區取自登入者，請求不帶（DD-03 的原則是不相信 client 自報）。病歷類型至少一個。更新採整組取代 |
| Q-06 | 同院區的專案名稱可否重複 | 沒寫 | 未查 | 先允許重複，除非業務要求 |
| Q-08 | 版本號從幾開始、何時遞增 | TS-PV-003 寫「發布後 versionNo 遞增」，沒說起始值 | 從 1 開始；發布不遞增；建立下一個草稿才遞增 | 照程式，並修正 TS-PV-003 的說法 |
| Q-11 | 專案的 use case 要不要檢查院區範圍 | D-024 的條文只寫 `ReviewCase` 與 `SamplingBatch`；`api-spec.md` 的錯誤代碼說明寫「案件/專案」 | 有檢查 | 要檢查，並修正 D-024 的適用範圍 |
| Q-12 | 專案詳情回傳哪些欄位；誰能看 | `api-spec.md` §7.8 沒有回應內容。權限列了 5 個角色，其中 REVIEWER / REREVIEWER「僅限唯讀必要範圍」沒有定義 | 只允許 `HOSPITAL_MANAGER` 與 `SYSTEM_ADMIN`（文件有的 `AUDITOR` 被拒）；`SYSTEM_ADMIN` 不受院區限制 | 草稿列了 11 個欄位供確認。角色請裁決 |
| Q-13 | 更新草稿要不要發 event | `domain-events.md` 沒有這個 event。規格草案要求保留「專案建立、修改、停用紀錄」 | 會發 `ReviewProjectDraftUpdatedEvent` | 要發，補進 `domain-events.md` |
| Q-14 | 輸入不合法的錯誤代碼 | 只有通用的 `VALIDATION_ERROR`。權重不合法沒有指定代碼 | 名稱空白回 `INVALID_PROJECT_NAME`（不在規格的代碼表） | 欄位格式問題用 `VALIDATION_ERROR`；權重不合法用 `PROJECT_VERSION_NOT_PUBLISHABLE` |

## 文件需要修正的地方（不需要裁決）

這幾項依現有的權威順序已有答案，只是文件沒同步。

| 位置 | 問題 | 依據 |
|---|---|---|
| `test-scenarios.md` TS-PV-002 | 預期寫 `403 FORBIDDEN`，應為 `ROLE_DENIED` | D-023 |
| `test-scenarios.md` TS-PV-003 | 路徑寫 `/publish-version`，`api-spec.md` §7.5 是 `/publish` | `api-spec.md` |
| `test-scenarios.md` TS-PV-004 | 預期寫「409 或 422」，一個情境只能有一個結果 | 待 Q-07 |
| `api-spec.md` §7.5 | 內文有「Implementation note」描述實作行為，規格不應描述實作 | — |
| `domain-model.md` §5.1 | 列了 8 個 policy 屬性與 `replaceSamplingPolicy(...)` 等方法，沒有任何規則內容 | 需補內容或標為未定 |

## 這次試作對格式的回饋

1. **「已決定 / 提案中」這個狀態欄很有用。** 它讓「文件其實沒寫」這件事變得看得見、數得出來。
2. **錯誤代碼是常見的模糊來源。** 好幾條規則是「要拒絕」已有依據，但用哪個代碼沒定（例如 `RP-UPD-01`、`RP-PUB-03`）。
3. **範例需要變數。** id 由系統產生，沒有 `save` 與 `${...}` 就寫不出第二步。
4. **跨 aggregate 的依賴一開始就出現。** 專案要發布就得先有評分表，所以 7 個範例被 `needs` 擋住。下一個該轉換的是評分表。
5. **event 目前驗證不到。** 範圍內沒有查詢看得到 event，需要決定是由稽核軌跡的查詢來觀察，還是交給實作端的測試。
