# Demo Automation SOP v1 - Quality Inspection

## 1. 文件目的

本文件提供 Demo Automation Phase 的最小 SOP，讓其他人在 Windows / PowerShell 環境下可以：

- 啟動 live standalone demo
- 做展示前 preflight
- 執行 headed demo mode
- 執行 fast e2e mode
- 在失敗時快速定位 frontend / proxy / backend 哪一層有問題

本文件不宣稱：

- integrated shell demo ready
- gateway integration demo ready
- 所有頁面都已納入 demo automation

---

## 2. 前置條件

### 必要條件
- Windows + PowerShell / pwsh
- JDK 已可執行 backend Gradle wrapper
- Node.js / npm 已安裝
- frontend 相依套件已安裝

### 必要 working roots
- backend: services/quality-inspection-service
- frontend: frontends/quality-inspection-frontend

### 正式 anchor data
- PROJECT-001
- BATCH-001
- CASE-011

資料 authority 以以下文件為準：
- [docs/demo/demo-script.md](docs/demo/demo-script.md)
- [docs/demo/demo-data-contract.md](docs/demo/demo-data-contract.md)
- [docs/demo/demo-acceptance-checklist.md](docs/demo/demo-acceptance-checklist.md)

---

## 3. 啟動 Demo Launcher

### 建議方式
從 frontend module root 執行：

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:launch
```

這個 launcher 會：

1. 啟 backend local profile
2. 等 backend execute-context API ready
3. 啟 frontend standalone
4. 等 4200 首頁與 proxy execute-context ready
5. 輸出三個 demo URL
6. 將 log 寫到 repo root 的 .demo-runtime/

### 可選：啟動後直接做 preflight

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:launch:preflight
```

---

## 4. 檢查目前狀態

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:status
```

會顯示：

- backendReady
- frontendReady
- proxyReady
- tracked backend/frontend PID
- log 檔位置

---

## 5. 執行 Playwright Preflight

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:preflight
```

Preflight 會驗：

1. backend 8080 execute-context 直連 ready
2. frontend 4200 ready
3. proxy execute-context ready
4. UI-006 reachable
5. UI-007 reachable
6. UI-011 reachable
7. 關鍵 anchor data visible
8. 無新的致命 console hard error / pageerror

Preflight 角色是：
展示前確認 demo ready，不是完整展示本身。

---

## 6. 執行 Demo Mode

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:run
```

Demo mode 特性：

- headed
- 單 worker
- deliberate pacing
- 依 demo-script 的固定順序停留 UI-006 → UI-007 → UI-011
- 仍保留最小必要 assert，失敗時可定位頁面

Canonical flow：

1. 開 UI-006
2. 展示 project / version / score sheet / threshold / latest batch time
3. 開 UI-007
4. 展示 batch row summary
5. 開 UI-011
6. 展示 assignment context、fallback、candidate list

---

## 7. 執行 E2E Mode

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:e2e
```

E2E mode 特性：

- 可 headless
- 單 worker
- 使用同一 canonical flow
- assert 較嚴格
- 含 reload 驗證與 API / UI cross-check

---

## 8. 關閉 / 清理

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:stop
```

說明：

- 會停止 launcher 管理的 backend / frontend process
- 若你重用的是外部既有 instance，launcher 不一定能替你回收該 process

---

## 9. 常見錯誤排查

### backend 沒起來
症狀：
- launcher 卡在 backend ready
- preflight 直連 8080 失敗

先看：
- .demo-runtime/backend.out.log
- .demo-runtime/backend.err.log

### frontend 沒起來
症狀：
- 4200 無法連線
- preflight 首頁檢查失敗

先看：
- .demo-runtime/frontend.out.log
- .demo-runtime/frontend.err.log

### proxy 不通
症狀：
- 4200 首頁可開，但 proxy execute-context 失敗

意義：
- frontend dev server 活著
- 但 backend upstream 不可用，或 proxy 轉發鏈路失敗

### 504 的意義
在本 repo 的 standalone 模式下，504 通常代表：

- Angular dev proxy 已收到 request
- 但 backend upstream unavailable

這不是 frontend route path 自身錯誤的主要證據，而是 live backend 沒有成功回應。

### Playwright 失敗但頁面看起來正常
優先檢查：

1. anchor data 是否改變
2. seed 是否漂移
3. 是否有人修改了 fallback 文案
4. 是否引入新的 console hard error

---

## 10. 本輪明確不處理的項目

- integrated shell demo
- gateway integration demo
- effectivePeriod
- truly independent current scoreSheetVersionId
- UI-007 批次詳情 deferred
- 完整案件分派提交流程
- 非 UI-006 / UI-007 / UI-011 頁面 demo automation

---

## 11. 建議執行順序

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:launch
npm run demo:preflight
npm run demo:run
npm run demo:e2e
```

若要一次啟動並預檢：

```powershell
Set-Location frontends/quality-inspection-frontend
npm run demo:launch:preflight
```
