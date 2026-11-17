import { existsSync } from 'fs';
import { defineConfig, devices } from '@playwright/test';

const playwrightFlow = process.env['QI_PLAYWRIGHT_FLOW'];

/**
 * 選用：以指定的 Chromium 執行檔取代 Playwright 依自身版本下載的瀏覽器。
 * 用於無法下載專案鎖定版本瀏覽器的環境（例如雲端 session 只預裝較新的 Chromium）。
 * 未設定時行為不變；設定了但檔案不存在時直接報錯，避免誤以為已改用指定瀏覽器。
 */
const chromiumExecutablePath = process.env['QI_PLAYWRIGHT_CHROMIUM_PATH']?.trim() || undefined;
if (chromiumExecutablePath && !existsSync(chromiumExecutablePath)) {
  throw new Error(`QI_PLAYWRIGHT_CHROMIUM_PATH points to a missing file: ${chromiumExecutablePath}`);
}

/**
 * Playwright e2e test configuration.
 *
 * E2e tests run against the frontend in standalone mode (shell-layer substitute)
 * with proxy forwarding to the local backend (gateway-layer substitute).
 *
 * See AGENTS.md §9.4 for the layered substitute strategy.
 */
export default defineConfig({
  testDir: './e2e/src',
  testMatch: '**/*.e2e.spec.ts',
  fullyParallel: playwrightFlow !== 'demo',
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  workers: process.env['CI'] || playwrightFlow === 'demo' || playwrightFlow === 'preflight' || playwrightFlow === 'e2e' ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        ...(chromiumExecutablePath ? { launchOptions: { executablePath: chromiumExecutablePath } } : {}),
      },
    }
  ],
  webServer: {
    command: 'npm run start:standalone',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env['CI'],
    timeout: 120 * 1000,
  },
});
