'use strict';
// RX3-G4 (research/REVIEW-EXTERNAL-3.md part G), VERSION 2 (2026-11-15). Version 1 (alpha44) switched an INVISIBLE layer of the page off at every touch on a player so that one tap reached the player. An independent
// review the same day showed that to be unsafe: it disabled a page's own transparent click layers, dialog backdrops, touch pads and icon boxes (and children of them, for good). The decision of this version: a layer of
// the page's OWN document is never switched off by a guess (the assist in guard-main.js answers it from evidence: a press that made the page open an ad). What is still switched off needs no guess: an INVISIBLE FRAME
// OF ANOTHER SITE that covers at least 60 percent of a player (a touch inside a frame never reaches the guard, so only a look at the page can find it), and it is given back when it turns out to be only fading in.
// guard-main.js (the page-world guard) is loaded alone, as in the real engine; taps are real touch taps (hasTouch).
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
  if (opts.off) await page.addInitScript(() => document.dispatchEvent(new CustomEvent('__kst_cfg_v1', { detail: '0' })));   // the content script's answer "not for this site", before anything runs
  await page.goto('https://movie.example/');
  return page;
}
const tapAt = async (page, x, y) => { await page.touchscreen.tap(x, y); await page.waitForTimeout(350); };
const counts = (page) => page.evaluate(() => ({ v: window.__v, l: window.__l }));
const noneOf = (page, sel) => page.evaluate((s) => getComputedStyle(document.querySelector(s)).pointerEvents, sel);

const FIXED_LAYER = `<div id="layer" style="position:fixed;left:0;top:0;width:100%;height:100%;opacity:0" onclick="window.open('${AD}')"></div>`;
const FRAME = (style) => `<iframe id="layer" src="https://ads.example/frame" style="position:fixed;left:0;top:0;width:100%;height:100%;border:0;${style || 'opacity:0'}"></iframe>`;

// ---- the page's own layers are never switched off by a guess

test('an invisible layer of the page itself over the player is NOT switched off by a touch: no evidence it is hostile, so it keeps its taps', async () => {
  const page = await open(VIDEO + '<div id="layer" style="position:fixed;left:0;top:0;width:100%;height:100%;opacity:0" onclick="window.__toggled = (window.__toggled || 0) + 1"></div>');
  await tapAt(page, 220, 170);
  assert.equal((await counts(page)).l, 1, 'the layer got its tap');
  assert.equal(await page.evaluate(() => window.__toggled), 1, 'and its own handler ran');
  assert.notEqual(await noneOf(page, '#layer'), 'none');
  await page.context().close();
});

test('REVIEW: the page\'s own invisible controls over a player all keep working: toggle overlay outside the wrapper, dialog backdrop, border-only box, icon box, svg and img layers, role=button, touch pad over a canvas, a link card', async () => {
  const L = (extra, inner) => `<div id="layer" role="${extra.role || 'presentation'}" style="position:absolute;left:20px;top:20px;width:400px;height:300px;${extra.css || 'opacity:0'}">${inner || ''}</div>`;
  const cases = [
    ['a toggle overlay one wrapper away from the video', '<div class="wrap" style="position:absolute;left:20px;top:20px;width:400px;height:300px"><video id="v" style="width:400px;height:300px;background:#222"></video></div>' + L({}), 220, 170],
    ['a dialog backdrop of rgba(0,0,0,0)', VIDEO + L({ css: 'position:fixed;left:0;top:0;width:100%;height:100%;background:rgba(0,0,0,0)' }), 220, 170],
    ['a box that is only a border', VIDEO + L({ css: 'background:transparent;border:3px solid #f0f;box-sizing:border-box' }), 220, 170],
    ['an icon box (a glyph made of spans)', VIDEO + L({}, '<span style="display:block;width:30px;height:4px;background:#000"></span><span style="display:block;width:30px;height:4px;background:#000"></span>'), 220, 170],
    ['an svg inside', VIDEO + L({}, '<svg width="20" height="20"><rect width="20" height="20"/></svg>'), 220, 170],
    ['an img inside', VIDEO + L({}, '<img alt="" width="1" height="1" src="data:image/gif;base64,R0lGODlhAQABAAAAACw=">'), 220, 170],
    ['a role=button div with words', VIDEO + L({ role: 'button' }, 'Play'), 220, 170],
    ['a touch pad over a canvas that is not a video', '<canvas id="v" width="400" height="300" style="position:absolute;left:20px;top:20px;width:400px;height:300px;background:#222"></canvas>' + L({}), 220, 170],
  ];
  for (const [what, body, x, y] of cases) {
    const page = await open(body);
    await tapAt(page, x, y);
    assert.equal((await counts(page)).l, 1, what + ': the page\'s own layer got its tap');
    assert.notEqual(await noneOf(page, '#layer'), 'none', what + ' stays');
    await page.context().close();
  }
});

test('the same layer WITHOUT the guard really does take the tap and open the ad (the test setup is sound)', async () => {
  const context = await browser.newContext({ hasTouch: true, viewport: { width: 500, height: 700 } });
  await context.route('https://**/*', (r) => { const u = new URL(r.request().url()); return r.fulfill({ contentType: 'text/html', body: u.hostname === 'movie.example' ? `<!doctype html><body style="margin:0">${VIDEO}${FIXED_LAYER}${COUNT}</body>` : '<!doctype html><title>ad</title>ad' }); });
  const page = await context.newPage(); const pops = []; context.on('page', (p) => pops.push(p));
  await page.goto('https://movie.example/');
  await page.touchscreen.tap(220, 170); await page.waitForTimeout(500);
  assert.deepEqual(await counts(page), { v: 0, l: 1 });
  assert.ok(pops.length >= 1, 'the layer opened its ad');
  await context.close();
});

// ---- an invisible frame of ANOTHER site over a player is switched off, and given back when it was only fading in

test('an invisible cross-site frame laid over the player is switched off, and one tap reaches the player', async () => {
  const page = await open(VIDEO + FRAME());
  await page.waitForTimeout(200);                                               // the page has loaded: the guard looked at it
  assert.equal(await noneOf(page, '#layer'), 'none');
  await tapAt(page, 220, 170);
  assert.equal((await counts(page)).v, 1);
  await page.context().close();
});

test('a frame an ad script adds a second after the page loaded is found by a later look', async () => {
  const page = await open(VIDEO);
  await page.evaluate(() => { setTimeout(() => { const f = document.createElement('iframe'); f.id = 'layer'; f.src = 'https://ads.example/late'; f.style.cssText = 'position:fixed;left:0;top:0;width:100%;height:100%;opacity:0;border:0'; document.body.appendChild(f); }, 1000); });
  await page.waitForTimeout(2200);                                              // the look at 1.5 s
  assert.equal(await noneOf(page, '#layer'), 'none');
  await tapAt(page, 220, 170);
  assert.equal((await counts(page)).v, 1);
  await page.context().close();
});

test('REVIEW: a frame that is only FADING IN (invisible when the page loaded, visible a moment later) is given back its touches', async () => {
  const page = await open(VIDEO + FRAME('opacity:0;transition:opacity 200ms'));
  await page.waitForTimeout(200);
  assert.equal(await noneOf(page, '#layer'), 'none', 'switched off while invisible');
  await page.evaluate(() => { document.getElementById('layer').style.opacity = '1'; });   // the embed fades in
  await page.waitForTimeout(2600);                                              // the look at 1.5 s / the slow check at 2 s
  assert.notEqual(await noneOf(page, '#layer'), 'none', 'given back once it can be seen');
  assert.equal(await page.evaluate(() => document.getElementById('layer').style.pointerEvents), '', 'our property is gone, not overwritten');
  await page.context().close();
});

test('frames that are not hostile are never switched off: a small one, a visible one, one of the page\'s own site, one that does not lie over the player, one beside it', async () => {
  const cases = [
    ['a small invisible frame in the middle (under 60 percent of the player)', VIDEO + '<iframe id="layer" src="https://ads.example/small" style="position:absolute;left:170px;top:120px;width:100px;height:100px;opacity:0;border:0"></iframe>'],
    ['a visible frame', VIDEO + FRAME('opacity:1')],
    ['an invisible frame of the page\'s own site', VIDEO + '<iframe id="layer" src="https://movie.example/own" style="position:fixed;left:0;top:0;width:100%;height:100%;opacity:0;border:0"></iframe>'],
    ['an invisible frame below the player', VIDEO + '<iframe id="layer" src="https://ads.example/below" style="position:absolute;left:20px;top:400px;width:400px;height:200px;opacity:0;border:0"></iframe>'],
    ['an invisible frame that is not positioned', VIDEO + '<iframe id="layer" src="https://ads.example/static" style="opacity:0;border:0;width:100px;height:50px"></iframe>'],
  ];
  for (const [what, body] of cases) {
    const page = await open(body);
    await page.waitForTimeout(2000);
    assert.notEqual(await noneOf(page, '#layer'), 'none', what + ' stays');
    await page.context().close();
  }
});

test('with the guard switched off for the site nothing is touched', async () => {
  const page = await open(VIDEO + FRAME(), { off: true });
  await page.waitForTimeout(1800);
  assert.notEqual(await noneOf(page, '#layer'), 'none');
  await page.context().close();
});

// ---------------------------------------------------------------- RVW2 P5b: the sweep only looks at the page when a frame COULD be a fake layer
test('P5b: a page whose frames are plain visible ones is never swept (no candidate query, no layout reads); a hidden fixed frame of another site still is', async () => {
  const plain = await open(`${VIDEO}<iframe src="https://widget.example/w" style="position:absolute;left:450px;top:20px;width:40px;height:40px"></iframe>`);
  await plain.evaluate(() => {
    window.__q = 0; window.__r = 0;
    const q = Document.prototype.querySelectorAll; Document.prototype.querySelectorAll = function (s) { if (String(s).indexOf('player') >= 0) window.__q++; return q.apply(this, arguments); };
    const r = Element.prototype.getBoundingClientRect; Element.prototype.getBoundingClientRect = function () { window.__r++; return r.apply(this, arguments); };
  });
  await plain.waitForTimeout(10500);                                           // the sweeps at 0, 1.5, 4 and 9 s have all happened
  const seen = await plain.evaluate(() => ({ q: window.__q, r: window.__r }));
  assert.equal(seen.q, 0, 'the whole-document candidate query never ran');
  await plain.context().close();
  const fake = await open(`${VIDEO}<iframe id="fk" src="https://ads.example/f" style="position:fixed;left:0;top:0;width:100%;height:100%;opacity:0"></iframe>`);
  await fake.waitForTimeout(1200);
  assert.equal(await noneOf(fake, '#fk'), 'none', 'a see-through fixed frame of another site over the player is still switched off');
  await fake.context().close();
});
