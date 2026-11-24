'use strict';
// OPT1 (research/OPTIMIZER.md): the Auto-tune benchmark pages (geckoapp/src/main/assets/bench) in real headless Chromium, served the way the app serves them: a loopback server, a token in the path, results POSTed
// to /<token>/r. Each page must finish, report finite numbers under the metric names the app's scoring expects, send nothing off the machine, and end with a title the app can read. `?fast=1` shortens the runs.
// Run: cd web-tests && npm test
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const DIR = path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'bench');
const TOKEN = 'tok9f3a';
const TYPES = { '.html': 'text/html', '.js': 'text/javascript', '.mp4': 'video/mp4' };
let server, port, browser; const results = []; const outside = [];

before(async () => {
  server = http.createServer((req, res) => {
    const u = new URL(req.url, 'http://x'); const parts = u.pathname.split('/').filter(Boolean);
    if (parts[0] !== TOKEN) { res.statusCode = 404; return res.end('no'); }
    if (req.method === 'POST' && parts[1] === 'r') { let b = ''; req.on('data', (c) => { b += c; }); req.on('end', () => { try { results.push(JSON.parse(b)); } catch (e) { results.push({ bad: b }); } res.end('ok'); }); return; }
    const f = path.join(DIR, path.basename(parts[1] || ''));
    if (!fs.existsSync(f)) { res.statusCode = 404; return res.end('missing'); }
    res.setHeader('Content-Type', TYPES[path.extname(f)] || 'application/octet-stream'); res.end(fs.readFileSync(f));
  });
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  port = server.address().port;
  browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required', '--mute-audio'] });
});
after(async () => { await browser.close(); server.close(); });

async function runPage(name, extra) {
  const context = await browser.newContext({ viewport: { width: 400, height: 700 } });
  const page = await context.newPage();
  page.on('request', (r) => { const h = new URL(r.url()).hostname; if (h !== '127.0.0.1') outside.push(r.url()); });
  const before = results.length;
  await page.goto(`http://127.0.0.1:${port}/${TOKEN}/${name}.html?n=7&fast=1${extra || ''}`);
  const t0 = Date.now();
  while (results.length === before && Date.now() - t0 < 40000) await new Promise((r) => setTimeout(r, 100));
  const title = await page.title();
  await context.close();
  assert.ok(results.length > before, name + ' reported nothing');
  return { r: results[results.length - 1], title };
}
function finite(m, keys) { for (const k of keys) assert.ok(typeof m[k] === 'number' && Number.isFinite(m[k]), `${k} is a finite number, got ${m[k]}`); }

test('js page reports script_ms and its parts', async () => {
  const { r, title } = await runPage('js');
  assert.equal(r.page, 'js'); assert.equal(r.n, '7'); assert.equal(r.err, ''); assert.equal(title, 'bench-done js');
  finite(r.m, ['script_ms', 'js_int', 'js_float', 'js_str', 'js_json', 'js_sort', 'js_alloc', 'js_dom']);
  assert.ok(r.m.script_ms > 0);
  const sum = ['js_int', 'js_float', 'js_str', 'js_json', 'js_sort', 'js_alloc', 'js_dom'].reduce((a, k) => a + r.m[k], 0);
  assert.ok(Math.abs(sum - r.m.script_ms) < 0.5, 'script_ms is the sum of the parts');
});

test('layout page reports layout_ms for a real tree', async () => {
  const { r } = await runPage('layout');
  assert.equal(r.err, ''); finite(r.m, ['layout_ms', 'nodes']); assert.ok(r.m.layout_ms > 0); assert.ok(r.m.nodes >= 100);
});

test('anim page reports three arms and the late-frame share', async () => {
  const { r } = await runPage('anim');
  assert.equal(r.err, ''); finite(r.m, ['fps_css', 'fps_blur', 'fps_clips', 'late_pct', 'late_a', 'late_b', 'late_c', 'clips_playing']);
  for (const k of ['fps_css', 'fps_blur', 'fps_clips']) assert.ok(r.m[k] > 3, k + ' ran: ' + r.m[k]);
  assert.ok(r.m.late_pct >= 0 && r.m.late_pct <= 100);
});

test('scroll page scrolls itself when the app does not, and says so', async () => {
  const { r } = await runPage('scroll');
  assert.equal(r.err, ''); finite(r.m, ['scroll_gap_p95', 'scroll_fps', 'scroll_ms', 'scroll_mode', 'scroll_px']);
  assert.equal(r.m.scroll_mode, 1, 'no app scroll came: the page used its own');
  assert.ok(r.m.scroll_px > 500, 'it moved: ' + r.m.scroll_px);
});

test('input page measures the wait from an input event to the next frame', async () => {
  const { r } = await runPage('input');
  assert.equal(r.err, ''); finite(r.m, ['input_p50', 'input_p95', 'input_max', 'input_n']);
  assert.equal(r.m.input_n, 25); assert.ok(r.m.input_p95 >= r.m.input_p50); assert.ok(r.m.input_p50 > 0);
});

test('compat page counts the features that work and notes the ones that do not', async () => {
  const { r } = await runPage('compat');
  assert.equal(r.err, ''); finite(r.m, ['compat_pass', 'compat_total']);
  assert.equal(r.m.compat_total, 26);
  assert.ok(r.m.compat_pass >= 20, 'most work in Chromium: ' + r.m.compat_pass + ' ' + r.note);
  if (r.m.compat_pass < r.m.compat_total) assert.ok(/^failed: /.test(r.note), 'the failed names are in the note: ' + r.note);
});

test('text page is ready for the owner and measures nothing', async () => {
  const context = await browser.newContext({ viewport: { width: 400, height: 700 } });
  const page = await context.newPage();
  await page.goto(`http://127.0.0.1:${port}/${TOKEN}/text.html?n=1&fast=1`);
  await page.waitForFunction(() => document.title === 'bench-ready text');
  assert.ok((await page.locator('th').count()) === 3 && (await page.locator('#lines div').count()) === 18);
  await context.close();
});

test('a wrong token gets nothing, and no page asked anything outside the machine', async () => {
  const res = await fetch(`http://127.0.0.1:${port}/wrong/js.html`);
  assert.equal(res.status, 404);
  const post = await fetch(`http://127.0.0.1:${port}/wrong/r`, { method: 'POST', body: '{}' });
  assert.equal(post.status, 404);
  assert.deepEqual(outside, [], 'every request stayed on 127.0.0.1');
});
