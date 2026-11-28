const { chromium } = require('playwright');
const FE = 'http://localhost:4200', BE = 'http://localhost:8080';
function token(uid, role, hosp = 'H001') {
  const p = { 'tz-user': JSON.stringify({ user: { user_id: uid, user_name: uid, user_role: [{ role_name: role.toLowerCase(), enabled: true }], hospital_id: hosp } }) };
  return 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.' + Buffer.from(JSON.stringify(p)).toString('base64url') + '.demo';
}
let browser;
async function open(role, uid, hosp = 'H001') {
  browser = browser || await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1366, height: 900 }, acceptDownloads: true });
  const actor = { userInfo: { userId: uid, userName: uid, hospitalId: hosp }, userRoles: [role], currentRole: role };
  await ctx.addInitScript(a => window.localStorage.setItem('qi-e2e-actor', JSON.stringify(a)), actor);
  const page = await ctx.newPage();
  const log = { api: [], errors: [] };
  await page.route('**/api/quality-inspection/**', async route => {
    const req = route.request();
    const url = req.url().replace(FE, BE);
    try {
      const resp = await route.fetch({ url, headers: { ...req.headers(), 'api-key': token(uid, role, hosp) } });
      let body = ''; try { body = await resp.text(); } catch {}
      log.api.push({ s: resp.status(), m: req.method(), u: url.replace(/^.*\/api\/quality-inspection/, ''), req: req.postData() ? req.postData().slice(0, 600) : undefined, body: body.slice(0, resp.status() >= 400 ? 500 : 3000) });
      await route.fulfill({ response: resp, body });
    } catch (e) { log.errors.push('route: ' + String(e).slice(0, 200)); await route.abort(); }
  });
  page.on('response', r => { if (r.status() >= 400 && !r.url().includes('/api/quality-inspection/')) log.errors.push('http ' + r.status() + ' ' + r.url()); });
  page.on('download', async d => { const p = (process.env.QI_E2E_OUT || './out') + '/' + d.suggestedFilename(); await d.saveAs(p); log.errors.push('DOWNLOAD ' + p); });
  page.on('pageerror', e => log.errors.push('pageerror: ' + String(e).slice(0, 300)));
  page.on('console', m => { if (m.type() === 'error') log.errors.push('console: ' + m.text().slice(0, 300)); });
  page.on('dialog', d => { log.errors.push('dialog: ' + d.message().slice(0, 200)); d.accept().catch(() => {}); });
  const settle = async (ms = 700) => { await page.waitForLoadState('networkidle').catch(() => {}); await page.waitForTimeout(ms); };
  const dump = async () => ({
    url: page.url().replace(FE, ''),
    text: (await page.locator('body').innerText()).replace(/\n+/g, ' | ').slice(0, 1800),
    buttons: await page.locator('button, a[href], [role=button]').evaluateAll(els => els.map(e => (e.innerText || '').trim().slice(0, 30) + (e.disabled ? '[x]' : '') + (e.getAttribute('data-testid') ? '{' + e.getAttribute('data-testid') + '}' : '')).filter(Boolean)),
    inputs: await page.locator('input, select, textarea').evaluateAll(els => els.map(e => e.tagName.toLowerCase() + ':' + (e.type || '') + ':' + (e.name || e.id || e.getAttribute('data-testid') || e.getAttribute('formcontrolname') || e.placeholder || '') + (e.disabled ? '[x]' : '') + '=' + String(e.value).slice(0, 20))),
  });
  return { page, log, settle, dump, goto: async p => { await page.goto(FE + p); await settle(); } };
}
module.exports = { open, close: async () => browser && browser.close(), FE, BE, token };
