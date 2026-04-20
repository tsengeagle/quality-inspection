# Demo Automation Phase README

## 1. 這是什麼

本文件是品質審查系統 **Demo Automation Phase** 的入口頁。

它的用途是讓第一次接手的人，可以在最短時間內理解：

- 這套 demo automation 是什麼
- 目前涵蓋哪些頁面
- 要怎麼啟動 live standalone demo
- 要怎麼做展示前預檢
- 要怎麼執行 headed demo mode 與 fast e2e mode
- 相關 authority 文件該看哪幾份

本文件不是：

- demo 劇本本身
- 資料契約本身
- 驗收清單本身
- 完整操作 SOP 本身

---

## 2. 適用範圍

### 目前涵蓋
- UI-006 抽選執行頁
- UI-007 抽選批次列表頁
- UI-011 案件分派頁
- live standalone chain
- demo launcher
- Playwright preflight
- Playwright demo mode
- Playwright e2e mode

### 適用讀者

#### demo 操作者
你想快速把展示跑起來，重點是：

- 啟動環境
- 先做 preflight
- 執行 headed demo mode

#### 工程維護者
你要維護這套 demo automation，重點是：

- canonical flow 在哪裡
- 固定資料契約在哪裡
- 驗收基線在哪裡
- SOP 與 launcher 怎麼配合

#### 只想快速跑 demo 的使用者
你不一定要讀完整設計文件，只需要知道：

- 先跑哪個命令
- 展示用哪個命令
- 驗收用哪個命令
- 出錯先看哪裡

---

## 3. 快速開始

### 建議起手順序

第一次操作請把 launcher 當成正式入口。`demo:preflight`、`demo:run`、`demo:e2e` 都只會對已經啟動的 live standalone chain 執行 Playwright，不會替你啟 backend / frontend。

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:launch
npm run demo:preflight
npm run demo:run
```

### 若要一次啟動並預檢

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:launch:preflight
```

### 若要做快速驗收

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:launch
npm run demo:e2e
```

### 若要看目前狀態

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:status
```

### 若要停止 launcher 管理的程序

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:stop
```

---

## 4. 模式說明

### Launcher

入口命令：

```powershell
npm run demo:launch
```

作用：

- 啟 backend local profile
- 等 backend ready
- 啟 frontend standalone
- 等 frontend 與 proxy ready
- 輸出 demo URL

前置條件：

- JDK、Node.js、npm 與 frontend 相依套件已就緒

如果你要的是把整套 live standalone chain 拉起來，先從 launcher 開始。這是本輪 demo 的正式啟動入口。

### Preflight

入口命令：

```powershell
npm run demo:preflight
```

作用：

- 展示前預檢
- 驗 backend、frontend、proxy 是否 ready
- 驗 UI-006、UI-007、UI-011 是否可進入
- 驗關鍵 anchor data 是否存在

前置條件：

- `npm run demo:launch` 已成功完成，或 backend / frontend / proxy 已由你自行啟動且 ready

preflight 不是展示本身，而是 **展示前先確認可 demo**。

### Demo Mode

入口命令：

```powershell
npm run demo:run
```

作用：

- headed
- 慢速
- 適合展示
- 每頁會以較自然的節奏停留
- 保留最小必要 assert，失敗時可定位頁面

前置條件：

- `npm run demo:launch` 已成功完成，或 backend / frontend / proxy 已由你自行啟動且 ready

`demo:run` 不是完整入口，也不會替你啟動 live standalone chain。

### E2E Mode

入口命令：

```powershell
npm run demo:e2e
```

作用：

- 快速
- 適合回歸驗收
- assert 較嚴格
- 含 reload 驗證與 API / UI cross-check

前置條件：

- `npm run demo:launch` 已成功完成，或 backend / frontend / proxy 已由你自行啟動且 ready

### 共通原則

preflight、demo mode、e2e mode 共用 **同一條 canonical flow**：

1. UI-006
2. UI-007
3. UI-011

這不是三套不同故事線，而是同一套 demo automation 的三種使用方式。

---

## 5. Canonical Flow 摘要

本輪固定主線如下：

1. 開 UI-006 抽選執行頁
2. 驗證或展示 project / version / score sheet / threshold / batch result
3. 開 UI-007 抽選批次列表頁
4. 驗證或展示 batch row summary
5. 開 UI-011 案件分派頁
6. 驗證或展示 assignment context、fallback、candidate list

正式故事線 authority 請看：

- [docs/demo/demo-script.md](demo-script.md)

---

## 6. 正式依賴資料

本輪 demo automation 依賴固定 anchor data：

- PROJECT-001
- BATCH-001
- CASE-011

以及固定觀察欄位：

- projectName
- versionNo
- scoreSheetId
- qualityThreshold
- sourceCaseKey
- assignedAt fallback
- inactive candidate / blockedReason

正式資料契約請看：

- [docs/demo/demo-data-contract.md](demo-data-contract.md)

---

## 7. 文件索引

### 流程文件
- [docs/demo/demo-script.md](demo-script.md)

用途：
- 定義 canonical demo flow
- 固定展示順序與每頁觀察點

### 資料契約
- [docs/demo/demo-data-contract.md](demo-data-contract.md)

用途：
- 固定 demo / e2e 的 anchor data
- 定義哪些值是正式依賴

### 驗收清單
- [docs/demo/demo-acceptance-checklist.md](demo-acceptance-checklist.md)

用途：
- 定義 preflight、demo mode、e2e mode 的成功判準

### 操作 SOP
- [docs/demo/demo-automation-sop.md](demo-automation-sop.md)

用途：
- 提供完整操作步驟
- 提供常見錯誤排查

---

## 8. 常見錯誤 / 排查

### 啟動前先確認什麼
- 先進入 frontend module root：`frontends/quality-inspection-frontend`
- 新操作者先跑 `npm run demo:launch` 或 `npm run demo:launch:preflight`
- 再用 `npm run demo:status` 看 ready 狀態
- 展示前先做 `npm run demo:preflight`

### backend 沒起來
先看：

- `.demo-runtime/backend.out.log`
- `.demo-runtime/backend.err.log`

### frontend 沒起來
先看：

- `.demo-runtime/frontend.out.log`
- `.demo-runtime/frontend.err.log`

### proxy 不通
若 4200 首頁可開，但 proxy API 失敗，代表：

- frontend standalone 活著
- 但 backend upstream 不可用，或 proxy 轉發鏈路失敗

### 504 代表什麼
在本 repo 的 standalone 模式下，504 通常代表：

- Angular dev proxy 收到 request
- 但 backend upstream unavailable

這通常不是 frontend route path 本身壞掉，而是 live backend 沒回應。

### Playwright 失敗時先看哪裡
優先順序：

1. anchor data 是否漂移
2. seed 是否改變
3. fallback 文案是否改變
4. 是否新增 console hard error 或 pageerror

---

## 9. 這輪明確不處理的項目

以下項目 **not in scope**：

- integrated shell demo
- gateway integration demo
- `effectivePeriod`
- truly independent `current scoreSheetVersionId`
- UI-007 批次詳情 deferred
- 完整案件分派提交流程
- 非 UI-006 / UI-007 / UI-011 的 demo automation

本文件也不宣稱：

- 整個 repo README 體系已重整完成
- 所有 demo 文件都已最佳化
- integrated demo ready
- shell / gateway integration ready

---

## 10. 建議閱讀順序

### 如果你只想快速展示
1. 本文件
2. [docs/demo/demo-automation-sop.md](demo-automation-sop.md)
3. 執行 `demo:launch`、`demo:preflight`、`demo:run`

### 如果你要做驗收
1. 本文件
2. [docs/demo/demo-acceptance-checklist.md](demo-acceptance-checklist.md)
3. [docs/demo/demo-data-contract.md](demo-data-contract.md)
4. 執行 `demo:launch`、`demo:preflight`、`demo:e2e`

### 如果你要維護腳本
1. 本文件
2. [docs/demo/demo-script.md](demo-script.md)
3. [docs/demo/demo-data-contract.md](demo-data-contract.md)
4. [docs/demo/demo-acceptance-checklist.md](demo-acceptance-checklist.md)
5. [docs/demo/demo-automation-sop.md](demo-automation-sop.md)
