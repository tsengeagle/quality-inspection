import { test } from '@playwright/test';
import { attachDemoDiagnostics, assertBackendReady, assertFrontendReady, assertProxyReady, runCanonicalDemoFlow } from './demo-support/demo-flow';

test.describe.serial('live standalone canonical e2e path', () => {
  test.skip(process.env['QI_PLAYWRIGHT_FLOW'] !== 'e2e', 'Use npm run demo:e2e to execute canonical live standalone e2e mode.');

  test('verifies the canonical three-page live flow against the accepted Phase 1 data contract', async ({ page, request }) => {
    const diagnostics = attachDemoDiagnostics(page);

    await assertFrontendReady(request);
    await assertBackendReady(request);
    await assertProxyReady(request);
    await runCanonicalDemoFlow(page, {
      mode: 'e2e',
      request,
      strictApiUi: true,
      verifyReload: true,
      diagnostics
    });

    diagnostics.assertClean('e2e mode');
    diagnostics.dispose();
  });
});
