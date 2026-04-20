import { test } from '@playwright/test';
import { attachDemoDiagnostics, runCanonicalDemoFlow } from './demo-support/demo-flow';

test.describe.serial('demo showcase mode', () => {
  test.skip(process.env['QI_PLAYWRIGHT_FLOW'] !== 'demo', 'Use npm run demo:run to execute headed demo mode.');

  test('plays the canonical three-page live demo story', async ({ page }) => {
    const diagnostics = attachDemoDiagnostics(page);

    await runCanonicalDemoFlow(page, {
      mode: 'demo',
      strictApiUi: false,
      verifyReload: false,
      diagnostics
    });

    diagnostics.assertClean('demo mode');
    diagnostics.dispose();
  });
});
