import { defineConfig, devices } from '@playwright/test';

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
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  workers: process.env['CI'] ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    }
  ],
  webServer: {
    command: 'npm run start:standalone',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env['CI'],
    timeout: 120 * 1000,
  },
});
