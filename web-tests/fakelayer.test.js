'use strict';
// RX3-G4 (research/REVIEW-EXTERNAL-3.md part G): a fake play layer is switched off BEFORE the press, so ONE real tap plays the video instead of opening an ad ("plays on the second try").
// guard-main.js (the page-world guard) is loaded alone, as in the real engine; taps are real touch taps (hasTouch) so the click that follows is hit-tested after the guard has run.
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const GUARD = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'guard-main.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

const AD = 'https://ads.example/landing?x=1';
const VIDEO = '<video id="v" style="position:absolute;left:20px;top:20px;width:400px;height:300px;background:#222"></video>';
const COUNT = '<script>window.__v = 0; window.__l = 0; var v = document.getElementById("v"); if (v) v.addEventListener("click", function () { window.__v++; }); var l = document.getElementById("layer"); if (l) l.addEventListener("click", function () { window.__l++; });</script>';

async function open(body, opts) {
  opts = opts || {};
  const context = await browser.newContext({ hasTouch: true, viewport: { width: 500, height: 700 } });
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.hostname === 'movie.example' && u.pathname === '/') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>movie</title></head><body style="margin:0">${body}${COUNT}</body></html>` });
    return r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>other</title>other' });
  });
  const page = await context.newPage();
  page.popups = [];
  context.on('page', (p) => page.popups.push(p));
  await page.addInitScript({ content: GUARD });
  await page.goto('https://movie.example/');
  if (opts.off) await page.evaluate(() => document.dispatchEvent(new CustomEvent('__kst_cfg_v1', { detail: '0' })));
  return page;
}
const tapAt = async (page, x, y) => { await page.touchscreen.tap(x, y); await page.waitForTimeout(350); };
const counts = (page) => page.evaluate(() => ({ v: window.__v, l: window.__l }));
const noneOf = (page, sel) => page.evaluate((s) => getComputedStyle(document.querySelector(s)).pointerEvents, sel);

const FIXED_LAYER = `<div id="layer" style="position:fixed;left:0;top:0;width:100%;height:100%;opacity:0" onclick="window.open('${AD}')"></div>`;

test('an invisible full-screen layer over the player is switched off by the first touch: ONE tap reaches the player and no ad opens', async () => {
  const page = await open(VIDEO + FIXED_LAYER);
  await tapAt(page, 220, 170);
  assert.deepEqual(await counts(page), { v: 1, l: 0 }, 'the video got the click, the layer did not');
  assert.equal(await noneOf(page, '#layer'), 'none');
  assert.equal(page.popups.length, 0, 'no ad window opened');
  await page.context().close();
});

test('the same layer WITHOUT the guard really does take the tap and open the ad (the test setup is sound)', async () => {
  const context = await browser.newContext({ hasTouch: true, viewport: { width: 500, height: 700 } });
  await context.route('https://**/*', (r) => { const u = new URL(r.request().url()); return r.fulfill({ contentType: 'text/html', body: u.hostname === 'movie.example' ? `<!doctype html><body style="margin:0">${VIDEO}${FIXED_LAYER}${COUNT}</body>` : '<!doctype html>other' }); });
  const page = await context.newPage(); const pops = []; context.on('page', (p) => pops.push(p));
  await page.goto('https://movie.example/');
  await page.touchscreen.tap(220, 170); await page.waitForTimeout(500);
  assert.deepEqual(await counts(page), { v: 0, l: 1 });
  assert.ok(pops.length >= 1, 'the layer opened its ad');
  await context.close();
});

test('an invisible cross-site frame laid over the player is switched off too', async () => {
  const page = await open(VIDEO + '<iframe id="layer" src="https://ads.example/frame" style="position:fixed;left:0;top:0;width:100%;height:100%;opacity:0;border:0"></iframe>');
  await page.waitForTimeout(200);                                               // the page has loaded: the guard looked at it
  await tapAt(page, 220, 170);
  assert.equal((await counts(page)).v, 1);
  assert.equal(await noneOf(page, '#layer'), 'none');
  await page.context().close();
});

test('a frame an ad script adds a second after the page loaded is found by a later look', async () => {
  const page = await open(VIDEO);
  await page.evaluate(() => { setTimeout(() => { const f = document.createElement('iframe'); f.id = 'layer'; f.src = 'https://ads.example/late'; f.style.cssText = 'position:fixed;left:0;top:0;width:100%;height:100%;opacity:0;border:0'; document.body.appendChild(f); }, 600); });
  await page.waitForTimeout(2200);                                              // the look at 1.5 s
  await tapAt(page, 220, 170);
  assert.equal((await counts(page)).v, 1);
  await page.context().close();
});

test('the player\'s own transparent click layer inside its wrapper is left alone before anything was refused', async () => {
  const page = await open('<div class="wrap" style="position:absolute;left:20px;top:20px;width:400px;height:300px"><video id="v" style="width:400px;height:300px;background:#222"></video><div id="layer" style="position:absolute;left:0;top:0;width:100%;height:100%"></div></div>');
  await tapAt(page, 220, 170);
  assert.deepEqual(await counts(page), { v: 0, l: 1 }, 'the page\'s own layer still gets its tap');
  assert.notEqual(await noneOf(page, '#layer'), 'none');
  await page.context().close();
});

test('after the guard refused an ad in this document, an invisible layer in the same wrapper is switched off as well', async () => {
  const page = await open('<div class="wrap" style="position:absolute;left:20px;top:20px;width:400px;height:300px"><video id="v" style="width:400px;height:300px;background:#222"></video><div id="layer" style="position:absolute;left:0;top:0;width:100%;height:100%"></div></div>');
  await page.evaluate((ad) => { window.open(ad); }, AD);                          // no tap behind it: refused, which the guard remembers
  await page.waitForTimeout(1000);                                              // (the assist of an earlier refusal only joins presses within 800 ms of it)
  await tapAt(page, 220, 170);
  assert.deepEqual(await counts(page), { v: 1, l: 0 });
  await page.context().close();
});

test('layers that are not fake are never switched off: a cookie banner with words, an opaque modal, the named player\'s own overlay, a layer with a picture, a tap beside the player', async () => {
  const cases = [
    ['a banner with words', VIDEO + '<div id="layer" style="position:fixed;left:0;top:0;width:100%;height:100%;opacity:0.01">We use cookies to improve your experience on this site</div>', 220, 170],
    ['an opaque modal', VIDEO + '<div id="layer" style="position:fixed;left:0;top:0;width:100%;height:100%;background:#fff"></div>', 220, 170],
    ['the named player\'s own overlay', '<div class="video-js" style="position:absolute;left:20px;top:20px;width:400px;height:300px"><video id="v" style="width:400px;height:300px"></video><div id="layer" style="position:fixed;left:0;top:0;width:100%;height:100%;opacity:0"></div></div>', 220, 170],
    ['a layer with an icon', VIDEO + '<div id="layer" style="position:fixed;left:0;top:0;width:100%;height:100%;opacity:0"><svg width="10" height="10"></svg></div>', 220, 170],
    ['a tap beside the player', VIDEO + FIXED_LAYER, 460, 600],
  ];
  for (const [what, body, x, y] of cases) {
    const page = await open(body);
    await tapAt(page, x, y);
    assert.notEqual(await noneOf(page, '#layer'), 'none', what + ' stays');
    await page.context().close();
  }
});

test('with the guard switched off for the site nothing is touched', async () => {
  const page = await open(VIDEO + FIXED_LAYER, { off: true });
  await page.touchscreen.tap(220, 170); await page.waitForTimeout(500);
  assert.notEqual(await noneOf(page, '#layer'), 'none');
  await page.context().close();
});

test('a page cannot make the guard switch layers off with events it made up itself', async () => {
  const page = await open(VIDEO + FIXED_LAYER);
  await page.evaluate(() => { const e = new PointerEvent('pointerdown', { bubbles: true, clientX: 220, clientY: 170 }); document.body.dispatchEvent(e); window.dispatchEvent(new PointerEvent('pointerdown', { clientX: 220, clientY: 170 })); });
  assert.notEqual(await noneOf(page, '#layer'), 'none', 'untrusted events are ignored');
  await page.context().close();
});
