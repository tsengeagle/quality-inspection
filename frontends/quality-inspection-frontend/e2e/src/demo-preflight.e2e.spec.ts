import { test } from '@playwright/test';
import { attachDemoDiagnostics, assertBackendReady, assertFrontendReady, assertProxyReady, runCanonicalDemoFlow } from './demo-support/demo-flow';

test.describe.serial('demo preflight', () => {
  test.skip(process.env['QI_PLAYWRIGHT_FLOW'] !== 'preflight', 'Use npm run demo:preflight to execute preflight checks.');

  test('confirms the live standalone demo chain is ready', async ({ page, request }) => {
    const diagnostics = attachDemoDiagnostics(page);

    await assertFrontendReady(request);
    await assertBackendReady(request);
    await assertProxyReady(request);
    await runCanonicalDemoFlow(page, {
      mode: 'preflight',
      request,
      strictApiUi: false,
      verifyReload: false,
      diagnostics
    });

    diagnostics.assertClean('preflight');
    diagnostics.dispose();
  });
});
