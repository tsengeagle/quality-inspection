// usage: node steps.js <steps.json> ; steps: [{role,uid,goto,acts:[{click|fill|select|check|goto|wait, dump:true}], name}]
const { open, close } = require('./lib'); const fs = require('fs');
(async () => {
  const sessions = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
  const T = +(process.env.TXT || 700);
  for (const ss of sessions) {
    const s = await open(ss.role, ss.uid);
    console.log(`\n===== [${ss.role}/${ss.uid}] ${ss.name || ''}`);
    let mark = 0;
    const report = async (label, full) => {
      const d = await s.dump();
      console.log(`-- ${label} @ ${d.url}`);
      for (const a of s.log.api.slice(mark)) console.log('   API', a.s, a.m, a.u.slice(0, 140), a.req ? 'REQ ' + a.req.slice(0, 260) : '', a.s >= 400 ? 'BODY ' + a.body.slice(0, 260) : '');
      mark = s.log.api.length;
      if (full !== false) { console.log('   TEXT', d.text.slice(0, T)); console.log('   BTN', JSON.stringify(d.buttons).slice(0, 500)); if (d.inputs.length) console.log('   IN', JSON.stringify(d.inputs).slice(0, 600)); }
      if (s.log.errors.length) { console.log('   ERR', JSON.stringify(s.log.errors).slice(0, 600)); s.log.errors.length = 0; }
    };
    if (ss.goto) { await s.goto(ss.goto); await report('goto', ss.quiet ? false : true); }
    for (const a of ss.acts || []) {
      try {
        if (a.goto) await s.page.goto('http://localhost:4200' + a.goto);
        else if (a.click) await s.page.locator(a.click).nth(a.nth || 0).click({ timeout: 5000 });
        else if (a.fill) await s.page.locator(a.fill[0]).nth(a.nth || 0).fill(a.fill[1], { timeout: 5000 });
        else if (a.select) await s.page.locator(a.select[0]).nth(a.nth || 0).selectOption(a.select[1], { timeout: 5000 });
        else if (a.check) await s.page.locator(a.check).nth(a.nth || 0).check({ timeout: 5000 });
        else if (a.shot) await s.page.screenshot({ path: a.shot, fullPage: true });
        await s.settle(a.wait || 600);
      } catch (e) { console.log('   !! ACT FAIL', JSON.stringify(a), String(e).split('\n')[0].slice(0, 160)); }
      if (a.dump !== false) await report(JSON.stringify(a).slice(0, 90), a.full);
    }
    await s.page.context().close();
  }
  await close();
})();
