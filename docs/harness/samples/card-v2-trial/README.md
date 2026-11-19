# 格式樣本：白板 V10 的 v2 試寫

**不是律法，也不是本專案的規格。** 這是用格式 v2 草案（`../../15-spec-format-v2-draft.md` 第四節）試寫的醫事人員卡規格，只用來檢驗 v2 的寫法夠不夠。結果與缺口見草案第十一節。

- 依 Q-9，卡片的規格屬於 iccard 的 repo；這裡的檔案將來是 v2.1 實作時的回歸樣本。
- 來源：負責人提供的白板 `hca-card-mutex-eventstorming-V10.drawio`（6 頁）與設計文件 `hca-card-mutex-design.md` 第四版。兩者都不在本 repo。
- 放在 `docs/` 底下，`spec_lint` 與執行器都不會讀到。

| 目錄 | 對應白板 | 規則 | 範例 |
|---|---|---|---|
| `card-gate/` | 02 頁：共用元件、六種結果、卡片資源狀態 | 25 | 17（矩陣展開後 19） |
| `iccard-signing/` | 03 頁：UC-I1、UC-I2 | 16 | 12 |
| `hcaapi-signing/` | 04 頁：UC-H1～H3 | 19 | 10 |

檔案裡的 `[缺口 G-xx]` 是 v2 草案沒有定義、試寫時只能假設的寫法。

`v2check.py` 是試寫時用的暫時檢查，只核對第四節已經定義的引用與結構（埠、操作、面向、行程、fixture、錯誤代碼、basis、轉移的涵蓋、分辨不了情境的範例）。它不是 `spec_lint`，不屬於 `harness-core/`。執行方式：

```bash
python3 docs/harness/samples/card-v2-trial/v2check.py docs/harness/samples/card-v2-trial
```

需要 PyYAML。
