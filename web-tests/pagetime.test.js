'use strict';
// LD4 (owner 2026-11-21: "Claude.Ai and YouTube load slow, even on fast wifi ... live diagnostics ... tells claude a lot"): pagetime.js, the page's own account of one load, in real headless Chromium.
// What is proved: ONE message per load with the main document's phases, the paints, the request counts by kind (also past the browser's 250-entry buffer), the slowest requests as [host, kind, ms], the page's lateness
// when its own script blocks, a partial report when the page is left or looked away from, nothing from a frame, and nothing after the report. The browser API is a fake that hands every message to the test.
// Run: cd web-tests && npm test
const fs = require('node:fs');
const path = require('node:path');
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SRC = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'pagetime.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

const FAST = { tick: 40, grace: 250, cap: 6000 };

/** A page at https://site.example/ whose subresources the test answers; every message of the fake extension API goes to [got] (as a keepalive POST to sink.example, which the test route collects). */
async function open(route, cfg, o) {
  o = o || {};
  const got = [];
  const context = await browser.newContext({ viewport: { width: 500, height: 800 } });
  await context.route('https://**/*', async (r) => {
    const u = new URL(r.request().url());
    if (u.hostname === 'sink.example') { got.push(JSON.parse(r.request().postData())); return r.fulfill({ status: 204, headers: { 'access-control-allow-origin': '*' } }); }     // a keepalive fetch survives the page being left (a binding call does not)
    const a = await route(u, r);
    if (a) return;
    return r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>t</title><body><h1>Hello</h1>' + (o.body || '') + '</body>' });
  });
  const page = await context.newPage();
  await page.addInitScript((c) => {
    window.__kstPTcfg = c;
    window.browser = { runtime: { sendMessage: (m) => { fetch('https://sink.example/pt', { method: 'POST', mode: 'no-cors', keepalive: true, body: JSON.stringify({ frame: window === window.top ? 'top' : 'sub', m: m }) }); return Promise.resolve(); } } };
  }, Object.assign({}, FAST, cfg || {}));
  await page.addInitScript({ content: SRC });
  return { page, got, context };
}
const reports = (got) => got.filter((g) => g.frame === 'top' && g.m.k === 'pt').map((g) => g.m);
async function until(f, what, ms) { const end = Date.now() + (ms || 8000); while (Date.now() < end) { if (await f()) return; await new Promise((r) => setTimeout(r, 25)); } assert.fail('timed out waiting for ' + what); }

test('LD4: one report per load: phases, paints, request counts by kind and the slowest requests as host, kind, ms', async () => {
  const { page, got, context } = await open(async (u, r) => {
    if (u.hostname === 'slow.example') { await new Promise((x) => setTimeout(x, 700)); await r.fulfill({ contentType: 'application/javascript', body: 'window.s=1' }); return true; }
    if (u.pathname.endsWith('.js')) { await r.fulfill({ contentType: 'application/javascript', body: 'window.a=1' }); return true; }
    if (u.pathname.endsWith('.css')) { await r.fulfill({ contentType: 'text/css', body: 'h1{color:red}' }); return true; }
    if (u.pathname.endsWith('.json')) { await r.fulfill({ contentType: 'application/json', body: '{}' }); return true; }
    return false;
  }, null, { body: '<script src="/a.js"></script><script src="https://slow.example/big.js"></script><link rel="stylesheet" href="/s.css"><script>fetch("/x.json");fetch("/y.json")</script>' });
  await page.goto('https://site.example/');
  await until(() => reports(got).length >= 1, 'the report');
  const m = reports(got)[0];
  assert.equal(m.k, 'pt'); assert.equal(m.h, 'site.example');
  assert.equal(m.pt, 0, 'a full report'); assert.equal(m.hid, 0);
  assert.ok(m.load > 0, 'load ' + m.load); assert.ok(m.dcl > 0); assert.ok(m.fcp > 0, 'first paint ' + m.fcp); assert.ok(m.fb >= 0); assert.ok(m.ttfb >= -1 && (m.ttfb < 0 || m.ttfb <= m.fb + 1), 'ttfb ' + m.ttfb + ' fb ' + m.fb);   // -1 = not reported (the CI browser gives a page answered by the test's own route no request-start time, seen 2026-11-21); when given it cannot exceed the time to the first byte
  assert.ok(m.rn >= 5, 'requests ' + m.rn);                       // a.js, big.js, s.css, x.json, y.json
  assert.ok(m.js >= 2 && m.css >= 1 && m.xhr >= 2, JSON.stringify(m));
  assert.equal(m.sl[0][0], 'slow.example'); assert.equal(m.sl[0][1], 'js'); assert.ok(m.sl[0][2] >= 600, 'slow ms ' + m.sl[0][2]);
  assert.ok(Array.isArray(m.lm) && m.lm.length === 3);
  assert.ok(m.nodes > 3 && m.scr >= 3 && m.fr === 0);
  assert.ok(JSON.stringify(m).indexOf('/big.js') < 0 && JSON.stringify(m).indexOf('/a.js') < 0, 'no path leaves the page');
  await page.waitForTimeout(700);
  assert.equal(reports(got).length, 1, 'ONE message per load, and the timer is stopped for good');
  await context.close();
});

test('LD4: the requests are counted past the 250-entry buffer of the browser (claude.ai makes over a thousand)', async () => {
  const { page, got, context } = await open(async (u, r) => { if (u.pathname.startsWith('/q/')) { await r.fulfill({ contentType: 'text/plain', body: 'x' }); return true; } return false; },
    { grace: 4000 }, { body: '<script>for (var i = 0; i < 600; i++) fetch("/q/" + i);</script>' });   // (the report waits this long after the load event: the 600 fetches are still in flight then, and on a busy machine 600 ms was not enough: 450 counted, seen 2026-11-22)
  await page.goto('https://site.example/');
  await until(() => reports(got).length >= 1, 'the report', 12000);
  assert.ok(reports(got)[0].rn >= 590, 'counted ' + reports(got)[0].rn);
  assert.ok(reports(got)[0].xhr >= 590);
  await context.close();
});

test('LD4: a page whose own script blocks for most of its load shows it as lateness; a quiet page does not', async () => {
  const busy = await open(async () => false, null, { body: '<script>window.addEventListener("load", function(){ setTimeout(function(){ var t = Date.now(); while (Date.now() - t < 900) {} }, 60); });</script>' });
  await busy.page.goto('https://site.example/');
  await until(() => reports(busy.got).length >= 1, 'busy report');
  const b = reports(busy.got)[0];
  assert.ok(b.lm[0] >= 600, 'worst lateness ' + b.lm[0]); assert.ok(b.lm[1] >= 600, 'total lateness ' + b.lm[1]); assert.ok(b.lm[2] >= 1);
  await busy.context.close();
  const quiet = await open(async () => false);
  await quiet.page.goto('https://site.example/');
  await until(() => reports(quiet.got).length >= 1, 'quiet report');
  assert.ok(reports(quiet.got)[0].lm[1] < 300, 'a quiet page: ' + JSON.stringify(reports(quiet.got)[0].lm));
  await quiet.context.close();
});

test('LD4: pagehide before the load ends sends a partial report at once; a page looked away from says so', async () => {
  const { page, got, context } = await open(async (u, r) => { if (u.pathname === '/never.js') { await new Promise((x) => setTimeout(x, 20000)); await r.abort(); return true; } return false; }, { cap: 30000 }, { body: '<script src="/never.js"></script>' });
  page.goto('https://site.example/').catch(() => {});
  await page.waitForTimeout(1200);
  await page.evaluate(() => window.dispatchEvent(new Event('pagehide')));      // (a real unload cannot be watched through the test's request interception: the handler is what counts)
  await until(() => reports(got).length >= 1, 'the partial report');
  const m = reports(got)[0];
  assert.equal(m.pt, 1, 'partial'); assert.equal(m.load, -1, 'the load never ended');
  await context.close();
  const h = await open(async () => false, { grace: 3000 });
  await h.page.goto('https://site.example/');
  await h.page.evaluate(() => { Object.defineProperty(document, 'hidden', { value: true, configurable: true }); document.dispatchEvent(new Event('visibilitychange')); });
  await until(() => reports(h.got).length >= 1, 'the report of a hidden page');
  assert.equal(reports(h.got)[0].hid, 1); assert.equal(reports(h.got)[0].pt, 1);
  await h.context.close();
});

test('LD4: a frame sends nothing, and nothing runs for a page without the extension API', async () => {
  const { page, got, context } = await open(async () => false, null, { body: '<iframe src="/inner"></iframe>' });
  await page.goto('https://site.example/');
  await until(() => reports(got).length >= 1, 'the top report');
  await page.waitForTimeout(500);
  assert.equal(got.filter((g) => g.frame === 'sub').length, 0, 'a sub-frame never reports');
  await context.close();
  const ctx = await browser.newContext();
  const p = await ctx.newPage();
  await ctx.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><body>hi' }));
  await p.addInitScript({ content: SRC });                               // no `browser` object at all
  await p.goto('https://site.example/');
  assert.equal(await p.evaluate(() => typeof window.KestrelPT), 'undefined');
  await ctx.close();
});

// ---- PERF10c (owner 2026-11-22: "I can't attach photos to claude.ai"): what the PAGE receives when a file input changes
const fcs = (got) => got.filter((g) => g.frame === 'top' && g.m.k === 'fc').map((g) => g.m);
async function png(page) { return Buffer.from((await page.evaluate(() => { const c = document.createElement('canvas'); c.width = 64; c.height = 48; const g = c.getContext('2d'); for (let x = 0; x < 64; x++) { g.fillStyle = 'rgb(' + (x * 4) + ',' + (255 - x * 4) + ',90)'; g.fillRect(x, 0, 1, 48); } g.fillStyle = '#000'; g.fillRect(10, 10, 20, 12); return c.toDataURL('image/png'); })).split(',')[1], 'base64'); }

test('PERF10c: a picked picture is read by the page: type, size, first bytes, decode, canvas draw and re-encode; no file name leaves the page', async () => {
  const { page, got, context } = await open(async () => false, { grace: 9000 }, { body: '<input type="file" id="f" multiple>' });
  await page.goto('https://site.example/');
  const buf = await png(page);
  await page.setInputFiles('#f', { name: 'My Private Holiday.png', mimeType: 'image/png', buffer: buf });
  await until(() => fcs(got).length >= 1, 'the file report');
  const m = fcs(got)[0];
  assert.equal(m.n, 1); assert.equal(m.a.length, 1);
  const a = m.a[0];
  assert.equal(a.t, 'image/png'); assert.equal(a.mg, 'png'); assert.equal(a.dec, 1); assert.equal(a.w, 64); assert.equal(a.h, 48);
  assert.equal(a.px, 1, 'drawn, not blank'); assert.equal(a.enc, 1, 're-encoded'); assert.ok(a.ek >= 0 && a.ms >= 0 && a.kb >= 0);
  assert.ok(JSON.stringify(m).indexOf('Holiday') < 0 && JSON.stringify(m).indexOf('.png') < 0, 'no file name');
  await page.waitForTimeout(1800);
  assert.equal(fcs(got).length, 1, 'the change and the input event of one pick count once');
  await context.close();
});

test('PERF10c: a file that is no picture is not decoded; a damaged picture is reported as failed to decode; a change with no files is ignored', async () => {
  const { page, got, context } = await open(async () => false, { grace: 9000 }, { body: '<input type="file" id="f">' });
  await page.goto('https://site.example/');
  await page.setInputFiles('#f', { name: 'a.txt', mimeType: 'text/plain', buffer: Buffer.from('hello world, not a picture') });
  await until(() => fcs(got).length >= 1, 'the text report');
  assert.equal(fcs(got)[0].a[0].mg, 'other'); assert.equal(fcs(got)[0].a[0].dec, 0, 'no decode attempt for text'); assert.equal(fcs(got)[0].a[0].px, 0);
  await page.waitForTimeout(1700);
  const bad = Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]), Buffer.from('this is not a png body at all'.repeat(4))]);
  await page.setInputFiles('#f', { name: 'b.png', mimeType: 'image/png', buffer: bad });
  await until(() => fcs(got).length >= 2, 'the damaged picture report');
  assert.equal(fcs(got)[1].a[0].mg, 'png'); assert.equal(fcs(got)[1].a[0].dec, 2, 'the engine cannot decode it'); assert.equal(fcs(got)[1].a[0].enc, 0);
  await page.waitForTimeout(1700);
  await page.setInputFiles('#f', []);                                                                          // the person cleared the choice: nothing to look at
  await page.waitForTimeout(500);
  assert.equal(fcs(got).length, 2, 'a change with no files is not a pick');
  await context.close();
});

test('PERF10c: only files of an input count, at most 3 files per report, and an empty file is reported as 0 KB', async () => {
  const { page, got, context } = await open(async () => false, { grace: 9000 }, { body: '<input type="file" id="f" multiple><input type="text" id="t">' });
  await page.goto('https://site.example/');
  await page.fill('#t', 'typing is not a pick');
  await page.waitForTimeout(300);
  assert.equal(fcs(got).length, 0, 'typing in a text field is not a file pick');
  const buf = await png(page);
  await page.setInputFiles('#f', [{ name: '1.png', mimeType: 'image/png', buffer: buf }, { name: '2.png', mimeType: 'image/png', buffer: buf }, { name: '3.png', mimeType: 'image/png', buffer: buf }, { name: '4.png', mimeType: 'image/png', buffer: buf }]);
  await until(() => fcs(got).length >= 1, 'the multi report');
  assert.equal(fcs(got)[0].n, 4); assert.equal(fcs(got)[0].a.length, 3, 'three files at most are examined');
  await page.waitForTimeout(1700);
  await page.setInputFiles('#f', { name: 'e.png', mimeType: 'image/png', buffer: Buffer.alloc(0) });
  await until(() => fcs(got).length >= 2, 'the empty report');
  assert.equal(fcs(got)[1].a[0].kb, 0); assert.equal(fcs(got)[1].a[0].mg, 'none');
  await context.close();
});

