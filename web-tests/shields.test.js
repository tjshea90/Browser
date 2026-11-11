'use strict';
// Runs the injected page script in real headless Chromium (the engine family Android WebView is built on).
// Run: cd web-tests && npm test        (Playwright is preinstalled globally in the build sandbox)
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SCRIPT = fs.readFileSync(path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'js', 'shields.js'), 'utf8');
const TOK = 4242;
const MARK = '__kst' + TOK.toString(36);
const cfg = (o) => 'var __KST_CFG=' + JSON.stringify(Object.assign({ fp: 1, sd: 1, pn: 1, seed: 12345, tok: TOK }, o || {})) + ';';
const PNG_1X1 = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==', 'base64');

let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

/** A page at a fake https origin (no network): serves `html`, and tiny cross-origin images from https://img.test/. */
async function open(host, html, inject) {
  const page = await browser.newPage();
  const errors = [];
  page.on('pageerror', (e) => errors.push(String(e)));
  await page.route(`https://${host}/`, (r) => r.fulfill({ contentType: 'text/html', body: html || '<!doctype html><body></body>' }));
  await page.route('https://img.test/**', (r) => r.fulfill({ contentType: 'image/png', body: PNG_1X1 }));
  await page.goto(`https://${host}/`);
  if (inject) await page.evaluate(inject);
  page.errors = errors;
  return page;
}
// Same wrapping as ScriptInjector.wrap(): the config is a LOCAL of a wrapper function, never a page global.
const withScript = (o) => '(function(){' + cfg(o) + '\n' + SCRIPT + '\n})();';

// A typical fingerprinting canvas: text, shapes, emoji.
const DRAW = `
  window.draw = function (w, h) {
    var c = document.createElement('canvas'); c.width = w || 240; c.height = h || 60;
    var x = c.getContext('2d');
    x.fillStyle = '#f60'; x.fillRect(10, 10, 100, 40);
    x.fillStyle = '#069'; x.font = '15px Arial'; x.fillText('Kestrel canvas test \\ud83d\\ude03', 2, 15);
    x.strokeStyle = 'rgba(102,204,0,0.7)'; x.beginPath(); x.arc(120, 30, 20, 0, 6.28); x.stroke();
    return c;
  };`;

test('canvas readback is perturbed by at most 1/255, deterministically, without touching the visible canvas', async () => {
  const base = await open('example.test', '', DRAW);
  const original = await base.evaluate(() => { const c = draw(); return { url: c.toDataURL(), px: Array.from(c.getContext('2d').getImageData(0, 0, 240, 60).data) }; });
  const p = await open('example.test', '', DRAW + withScript());
  const r = await p.evaluate(() => {
    const c = draw(); const ctx = c.getContext('2d');
    const a = c.toDataURL(), b = c.toDataURL();
    const px1 = Array.from(ctx.getImageData(0, 0, 240, 60).data), px2 = Array.from(ctx.getImageData(0, 0, 240, 60).data);
    return { a, b, px1, px2 };
  });
  assert.notEqual(r.a, original.url, 'toDataURL must differ from the unprotected result');
  assert.equal(r.a, r.b, 'same canvas, same session, same site -> identical output (apps keep working)');
  assert.deepEqual(r.px1, r.px2, 'getImageData is deterministic too');
  let diffs = 0, max = 0;
  for (let i = 0; i < r.px1.length; i++) { const d = Math.abs(r.px1[i] - original.px[i]); if (d) diffs++; if (d > max) max = d; }
  assert.ok(max <= 1, `visible change must be at most 1/255, was ${max}`);
  assert.ok(diffs > 0 && diffs < r.px1.length * 0.1, `noise must be sparse but present: ${diffs} of ${r.px1.length}`);
  assert.deepEqual(p.errors, []);
});

test('noise differs between sessions (seed) and between sites', async () => {
  const get = async (host, seed) => { const p = await open(host, '', DRAW + withScript({ seed })); const v = await p.evaluate(() => draw().toDataURL()); await p.close(); return v; };
  const a = await get('example.test', 1), b = await get('example.test', 2), c = await get('other.test', 1), a2 = await get('www.example.test', 1);
  assert.notEqual(a, b, 'a new browser session must produce a different fingerprint');
  assert.notEqual(a, c, 'a different site must see a different fingerprint');
  assert.equal(a, a2, 'subdomains of one site agree (last two host labels are the site key)');
});

test('tiny canvases are left alone', async () => {
  const base = await open('example.test', '', DRAW);
  const o = await base.evaluate(() => draw(8, 8).toDataURL());
  const p = await open('example.test', '', DRAW + withScript());
  assert.equal(await p.evaluate(() => draw(8, 8).toDataURL()), o);
});

test('toBlob returns the same farbled bytes as toDataURL', async () => {
  const p = await open('example.test', '', DRAW + withScript());
  const same = await p.evaluate(() => new Promise((resolve) => {
    const c = draw(); const url = c.toDataURL();
    c.toBlob((blob) => { const fr = new FileReader(); fr.onload = () => resolve(fr.result === url); fr.readAsDataURL(blob); });
  }));
  assert.equal(same, true);
});

test('tainted (cross-origin) canvases still throw exactly as before', async () => {
  const p = await open('example.test', '', withScript());
  const name = await p.evaluate(() => new Promise((resolve) => {
    const img = new Image(); img.onload = () => {
      const c = document.createElement('canvas'); c.width = 30; c.height = 30; c.getContext('2d').drawImage(img, 0, 0, 30, 30);
      try { c.toDataURL(); resolve('no error'); } catch (e) { resolve(e.name); }
    };
    img.onerror = () => resolve('image failed'); img.src = 'https://img.test/x.png';
  }));
  assert.equal(name, 'SecurityError');
});

test('WebGL canvases are not blanked', async () => {
  const code = `window.gl = function () { var c = document.createElement('canvas'); c.width = 64; c.height = 64; var g = c.getContext('webgl'); if (!g) return null;
    g.clearColor(1, 0, 0, 1); g.clear(g.COLOR_BUFFER_BIT); return c.toDataURL(); };`;
  const base = await open('example.test', '', code);
  const o = await base.evaluate(() => gl());
  if (o === null) return; // no WebGL in this headless build
  const p = await open('example.test', '', code + withScript());
  assert.equal(await p.evaluate(() => gl()), o);
});

test('audio buffers get inaudible noise, deterministic per channel', async () => {
  const render = `window.render = async function () {
    var ctx = new OfflineAudioContext(1, 4410, 44100); var o = ctx.createOscillator(); o.frequency.value = 440; o.connect(ctx.destination); o.start();
    var buf = await ctx.startRendering(); var d = buf.getChannelData(0); var again = buf.getChannelData(0);
    return { data: Array.from(d), same: d === again || Array.from(d).join() === Array.from(again).join() }; };`;
  const base = await open('example.test', '', render);
  const o = await base.evaluate(() => render());
  const p = await open('example.test', '', render + withScript());
  const r = await p.evaluate(() => render());
  let diffs = 0, max = 0;
  for (let i = 0; i < o.data.length; i++) { const d = Math.abs(r.data[i] - o.data[i]); if (d) diffs++; if (d > max) max = d; }
  assert.ok(diffs > 0, 'some samples must change');
  assert.ok(max < 1e-4, `noise must be inaudible, max ${max}`);
  assert.equal(r.same, true);
});

test('hardwareConcurrency is reduced but never below 2 and never above the real value', async () => {
  const base = await open('example.test', '', '');
  const real = await base.evaluate(() => navigator.hardwareConcurrency);
  const p = await open('example.test', '', withScript());
  const v = await p.evaluate(() => [navigator.hardwareConcurrency, navigator.hardwareConcurrency]);
  assert.equal(v[0], v[1]);
  assert.ok(v[0] >= Math.min(2, real) && v[0] <= real, `got ${v[0]} of real ${real}`);
});

test('data saver signal is exposed', async () => {
  const p = await open('example.test', '', withScript());
  assert.equal(await p.evaluate(() => navigator.connection && navigator.connection.saveData), true);
  const off = await open('example.test', '', withScript({ sd: 0 }));
  assert.notEqual(await off.evaluate(() => navigator.connection && navigator.connection.saveData), true);
});

test('media never preloads or autoplays, including elements added later', async () => {
  const p = await open('example.test', '<!doctype html><body><video id="a" autoplay preload="auto"></video></body>', withScript());
  assert.deepEqual(await p.evaluate(() => { const v = document.getElementById('a'); return [v.preload, v.autoplay]; }), ['none', false]);
  await p.evaluate(() => { document.body.insertAdjacentHTML('beforeend', '<div><audio id="b" autoplay preload="metadata"></audio></div>'); });
  await p.waitForTimeout(50);
  assert.deepEqual(await p.evaluate(() => { const v = document.getElementById('b'); return [v.preload, v.autoplay]; }), ['none', false]);
});

test('a disabled feature stays disabled', async () => {
  const base = await open('example.test', '', DRAW);
  const o = await base.evaluate(() => draw().toDataURL());
  const p = await open('example.test', '', DRAW + withScript({ fp: 0 }));
  assert.equal(await p.evaluate(() => draw().toDataURL()), o, 'fp=0: canvas output untouched');
});

test('replaced functions look native and the script is idempotent', async () => {
  const p = await open('example.test', '', DRAW + withScript());
  const s = await p.evaluate(() => [HTMLCanvasElement.prototype.toDataURL.toString(), CanvasRenderingContext2D.prototype.getImageData.toString()]);
  for (const t of s) assert.ok(t.includes('[native code]'), t);
  const first = await p.evaluate(() => draw().toDataURL());
  await p.evaluate(withScript()); // second injection (onPageFinished does this) must be a no-op, not a double wrap
  assert.equal(await p.evaluate(() => draw().toDataURL()), first);
  assert.equal(await p.evaluate((m) => window[m], MARK), 1);
  assert.deepEqual(p.errors, []);
});

test('the script never throws, even on a hostile page that deleted the APIs it patches', async () => {
  const hostile = `delete HTMLCanvasElement.prototype.toBlob; delete window.AudioBuffer; Object.defineProperty(navigator, 'connection', { value: null });`;
  const p = await open('example.test', '', hostile);
  await p.evaluate(withScript());
  assert.deepEqual(p.errors, []);
  assert.equal(await p.evaluate((m) => window[m], MARK), 1);
});

test('the page cannot read the configuration or the session seed', async () => {
  const p = await open('example.test', '', withScript({ seed: 987654321 }));
  const r = await p.evaluate(() => {
    const names = Object.getOwnPropertyNames(window).filter((n) => n.indexOf('__kst') === 0 || n.indexOf('KST') >= 0);
    let found = false;
    try { found = JSON.stringify(Object.getOwnPropertyNames(window).map((n) => { try { return String(window[n]); } catch (e) { return ''; } })).indexOf('987654321') >= 0; } catch (e) { /* ignore */ }
    return { cfg: typeof window.__KST_CFG, names, found };
  });
  assert.equal(r.cfg, 'undefined', '__KST_CFG must not be a page global');
  assert.deepEqual(r.names.filter((n) => n !== MARK), [], 'only the per-page marker may remain on window');
  assert.equal(r.found, false, 'the seed must not appear in any window property');
});

test('pre-defining a marker cannot switch the protections off', async () => {
  const base = await open('example.test', '', DRAW);
  const original = await base.evaluate(() => draw().toDataURL());
  const guess = 'window.__kst = 1; window.__kst0 = 1; window.__kst1 = 1;'; // the old fixed name and guesses at the new one
  const p = await open('example.test', '', DRAW + guess + withScript());
  assert.notEqual(await p.evaluate(() => draw().toDataURL()), original, 'the script must still have run');
});

test('a page-set expando on a canvas does not ask for the unmodified readback', async () => {
  const base = await open('example.test', '', DRAW);
  const original = await base.evaluate(() => draw().toDataURL());
  const p = await open('example.test', '', DRAW + withScript());
  const v = await p.evaluate(() => { const c = draw(); c.__kstSkip = 1; return c.toDataURL(); });
  assert.notEqual(v, original, 'canvas.__kstSkip = 1 must not bypass farbling');
});
