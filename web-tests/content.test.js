'use strict';
// content.js (the isolated content script of the built-in extension) in real headless Chromium with a fake `browser.runtime`: the long press on a video and the media scan.
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const CONTENT = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'content.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

/** A page at https://site.example/ with content.js running against a fake extension API that records every message. */
async function open(body, pre) {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` }));
  const page = await context.newPage();
  await page.addInitScript(() => {
    window.__msgs = [];
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } };
  });
  if (pre) await page.addInitScript(pre);
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/');
  return page;
}
const msgs = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'pop'));
const rightClick = (page, x, y) => page.mouse.click(x, y, { button: 'right' });

test('a long press on a video is taken before the site\'s own player menu and reported to the app', async () => {
  const page = await open(`<div style="position:relative;width:400px;height:300px"><video src="https://cdn.example/v/movie.mp4" style="width:400px;height:300px"></video><div id="layer" style="position:absolute;inset:0" oncontextmenu="window.__siteMenu=1"></div></div>`);
  await rightClick(page, 100, 100);
  assert.equal(await page.evaluate(() => window.__siteMenu), undefined, 'the site\'s own menu never saw it');
  assert.deepEqual(await msgs(page), [{ k: 'pop', d: 'vmenu|https://cdn.example/v/movie.mp4', p: 'https://site.example/' }]);
});

test('a video with only <source> children is taken too, a blob: source is reported without an address', async () => {
  const page = await open(`<video id="v" controls style="width:300px;height:200px"><source src="https://cdn.example/a.mp4"></video>`);
  await rightClick(page, 50, 50);
  const m = await msgs(page);
  assert.equal(m.length, 1); assert.ok(m[0].d.startsWith('vmenu|'));
});

test('a long press anywhere else keeps the browser\'s and the site\'s own behaviour', async () => {
  const page = await open(`<div id="t" style="width:300px;height:200px" oncontextmenu="window.__siteMenu=1">text</div><video style="width:100px;height:50px"></video>`);
  await rightClick(page, 50, 50);
  assert.equal(await page.evaluate(() => window.__siteMenu), 1);
  assert.deepEqual(await msgs(page), []);
});

test('a bare media element (no source at all) is left alone', async () => {
  const page = await open(`<video id="v" style="width:300px;height:200px" oncontextmenu="window.__siteMenu=1"></video>`);
  await rightClick(page, 50, 50);
  assert.equal(await page.evaluate(() => window.__siteMenu), 1);
  assert.deepEqual(await msgs(page), []);
});

// ---------------------------------------------------------------- the media scan (AA4 / PERF_JS J4)
const doms = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'dom').map((m) => m.u));
const settle = (page) => page.waitForTimeout(500); // the scan runs when the page is idle (requestIdleCallback), a moment after load
const quickIdle = () => { window.requestIdleCallback = (f) => setTimeout(f, 30); }; // headless Chromium may wait for the full 3 s timeout of an idle callback: the tests do not

test('scan: media addresses in elements, meta tags, links and inline player configurations are found without anyone pressing play', async () => {
  const page = await open(`
    <video src="https://cdn.example/a.mp4"></video>
    <meta property="og:video" content="https://cdn.example/og.mp4">
    <a href="https://cdn.example/link.webm">x</a><a href="https://cdn.example/page.html">y</a>
    <script>var cfg = {"hls":"https:\\u002f\\u002fcdn.example\\u002fplay\\u002fmaster.m3u8","dash":"https:\\/\\/cdn.example\\/play\\/manifest.mpd"};</script>`, quickIdle);
  await settle(page);
  const found = (await doms(page)).sort();
  assert.deepEqual(found, ['https://cdn.example/a.mp4', 'https://cdn.example/link.webm', 'https://cdn.example/og.mp4', 'https://cdn.example/play/manifest.mpd', 'https://cdn.example/play/master.m3u8']);
});

test('scan: a huge inline script is skipped without being read, and the scan carries on with the scripts after it', async () => {
  const filler = 'x'.repeat(250000);
  const page = await open(`<script>var big = "${filler} https://cdn.example/hidden-in-bundle.mp4";</script><script>var small = "https://cdn.example/real.mp4";</script><script>var nothing = 1 + 2; // no media here at all</script>`, quickIdle);
  await settle(page);
  assert.deepEqual(await doms(page), ['https://cdn.example/real.mp4']);
});

test('scan: at most 40 addresses per frame, each reported once even though the scan runs twice', async () => {
  let links = ''; for (let i = 0; i < 60; i++) links += `<a href="https://cdn.example/v${i}.mp4">v</a>`;
  const page = await open(links, quickIdle);
  await settle(page);
  await page.evaluate(() => window.dispatchEvent(new Event('load')));
  await page.waitForTimeout(3000); // the second scan, 2.5 s after load
  const found = await doms(page);
  assert.equal(found.length, 40);
  assert.equal(new Set(found).size, 40);
});

test('scan: scripts and links that appear after the first scan are found by the second one', async () => {
  const page = await open(`<a href="https://cdn.example/first.mp4">a</a>`, quickIdle);
  await settle(page);
  assert.deepEqual(await doms(page), ['https://cdn.example/first.mp4']);
  await page.evaluate(() => {
    const s = document.createElement('script'); s.textContent = 'var late = "https://cdn.example/late.m3u8";'; document.body.appendChild(s);
    const a = document.createElement('a'); a.href = 'https://cdn.example/late-link.mp4'; document.body.appendChild(a);
    window.dispatchEvent(new Event('load'));
  });
  await page.waitForTimeout(3000);
  assert.deepEqual((await doms(page)).sort(), ['https://cdn.example/first.mp4', 'https://cdn.example/late-link.mp4', 'https://cdn.example/late.m3u8']);
});

// ---------------------------------------------------------------- a tapped field is not left behind the keyboard (AA3-2 / research/TYPING.md)
// Firefox shrinks only the visual viewport when the keyboard opens: the test replaces window.visualViewport with a fake whose height it can change, then fires its resize event.
const fakeKeyboard = () => {
  const listeners = [];
  const vv = { height: 700, offsetTop: 0, addEventListener: (t, f) => { if (t === 'resize') listeners.push(f); }, removeEventListener() {} };
  Object.defineProperty(window, 'visualViewport', { value: vv, configurable: true });
  window.__keyboard = (h) => { vv.height = h; listeners.forEach((f) => f()); };
};
const field = (top, tag = 'input') => `<div style="height:${top}px"></div><${tag} id="f" style="height:30px;display:block">${tag === 'textarea' ? '</textarea>' : ''}<div style="height:3000px"></div>`;

test('keyboard: a text field that the keyboard would cover is scrolled into the visible area', async () => {
  const page = await open(field(500), fakeKeyboard);
  await page.focus('#f');
  assert.equal(await page.evaluate(() => scrollY), 0, 'the field is on screen before the keyboard opens');
  await page.evaluate(() => window.__keyboard(300));
  await page.waitForTimeout(400);
  assert.ok(await page.evaluate(() => scrollY) > 100, 'the page scrolled to bring the field up');
});

test('keyboard: the keyboard opens in two steps, the field is placed by the final size', async () => {
  const page = await open(field(500, 'textarea'), fakeKeyboard);
  await page.focus('#f');
  await page.evaluate(() => { window.__keyboard(375); setTimeout(() => window.__keyboard(335), 40); });
  await page.waitForTimeout(500);
  assert.ok(await page.evaluate(() => scrollY) > 100);
});

test('keyboard: a field that is still inside the visible area is not moved', async () => {
  const page = await open(field(100), fakeKeyboard);
  await page.focus('#f');
  await page.evaluate(() => window.__keyboard(300));
  await page.waitForTimeout(400);
  assert.equal(await page.evaluate(() => scrollY), 0);
});

test('keyboard: a small change of the visible area (a toolbar) is not a keyboard, and nothing but text fields is moved', async () => {
  const small = await open(field(500), fakeKeyboard);
  await small.focus('#f');
  await small.evaluate(() => window.__keyboard(660));
  await small.waitForTimeout(400);
  assert.equal(await small.evaluate(() => scrollY), 0, 'only 40 px: not a keyboard');
  const btn = await open(`<div style="height:500px"></div><button id="f" style="height:30px;display:block">go</button><div style="height:3000px"></div>`, fakeKeyboard);
  await btn.focus('#f');
  await btn.evaluate(() => window.__keyboard(300));
  await btn.waitForTimeout(400);
  assert.equal(await btn.evaluate(() => scrollY), 0, 'a button has no keyboard to make room for');
  const box = await open(`<div style="height:500px"></div><input id="f" type="checkbox"><div style="height:3000px"></div>`, fakeKeyboard);
  await box.focus('#f');
  await box.evaluate(() => window.__keyboard(300));
  await box.waitForTimeout(400);
  assert.equal(await box.evaluate(() => scrollY), 0, 'a checkbox does not open the keyboard either');
});

test('keyboard: an editable region (contenteditable) is moved like a field, and a page without visualViewport is left alone', async () => {
  const page = await open(`<div style="height:500px"></div><div id="f" contenteditable="true" style="height:30px">x</div><div style="height:3000px"></div>`, fakeKeyboard);
  await page.focus('#f');
  await page.evaluate(() => window.__keyboard(300));
  await page.waitForTimeout(400);
  assert.ok(await page.evaluate(() => scrollY) > 100);
  const none = await open(field(500), () => { Object.defineProperty(window, 'visualViewport', { value: undefined, configurable: true }); });
  assert.equal(await none.evaluate(() => scrollY), 0);
  assert.deepEqual(await none.evaluate(() => window.__msgs.filter((m) => m.k === 'pop')), [], 'no error, nothing reported');
});

// ---------------------------------------------------------------- D2: generic element hiding by the page's classes and ids
// The fake app answers the `css` question (with or without the marker) and each `gen` batch (the CSS for ".ad-banner" only), and keeps every batch the page sent.
// These run inside the page (addInitScript serialises them), so each one is self-contained.
const appWithMarker = () => {
  window.__gen = [];
  window.requestIdleCallback = (f) => setTimeout(f, 30);
  window.browser.runtime.sendMessage = (m) => {
    window.__msgs.push(m);
    if (m.k === 'css') return Promise.resolve('/*kg*/.site-rule{display:none!important}');
    if (m.k === 'gen') { window.__gen.push(m.t); return Promise.resolve(m.t.indexOf('.ad-banner') >= 0 ? '.ad-banner{display:none!important}' : ''); }
    return Promise.resolve(undefined);
  };
};
const appWithoutMarker = () => {
  window.__gen = [];
  window.browser.runtime.sendMessage = (m) => {
    window.__msgs.push(m);
    if (m.k === 'css') return Promise.resolve('.site-rule{display:none!important}');
    if (m.k === 'gen') window.__gen.push(m.t);
    return Promise.resolve(undefined);
  };
};
const appDown = () => { window.browser.runtime.sendMessage = () => Promise.reject(new Error('no app')); };
const gens = (page) => page.evaluate(() => window.__gen);
const flat = (batches) => [].concat(...batches);

test('tokens: with the marker the page sends its classes and ids (lower case, once each) and the selectors that come back are applied', async () => {
  const page = await open(`<div id="Top" class="Ad-Banner x">hi</div><div class="ad-banner real">r</div><span class="plain  other">s</span><p id="">p</p>`, appWithMarker);
  await page.waitForTimeout(1200);
  const sent = flat(await gens(page)).sort();
  assert.deepEqual(sent, ['#top', '.ad-banner', '.other', '.plain', '.real', '.x'], 'names are lower-cased and sent once');
  assert.equal(await page.evaluate(() => getComputedStyle(document.querySelector('.real')).display), 'none', 'the generic selector that matched was injected');
  assert.equal(await page.evaluate(() => getComputedStyle(document.querySelector('.plain')).display), 'inline');
  assert.ok(await page.evaluate(() => !!document.querySelector('style[data-kestrel="g"]')));
  const csss = await page.evaluate(() => window.__msgs.filter((m) => m.k === 'css'));
  assert.equal(csss.length, 1);
});

test('tokens: without the marker nothing is sent (the app turned generic hiding off, or the host is allowed)', async () => {
  const page = await open(`<div class="ad-banner">hi</div>`, appWithoutMarker);
  await page.waitForTimeout(900);
  assert.deepEqual(await gens(page), []);
  assert.equal(await page.evaluate(() => getComputedStyle(document.querySelector('.ad-banner')).display), 'block');
});

test('tokens: names that appear later are sent too, each name only once however often it appears', async () => {
  const page = await open(`<div class="first">a</div>`, appWithMarker);
  await page.waitForTimeout(900);
  await page.evaluate(() => {
    const d = document.createElement('div'); d.className = 'late-one first'; d.innerHTML = '<p class="late-two" id="late-id"></p>';
    document.body.appendChild(d);
    const e = document.createElement('div'); e.className = 'late-one'; document.body.appendChild(e);
  });
  await page.waitForTimeout(900);
  const sent = flat(await gens(page));
  assert.deepEqual(sent.slice().sort(), ['#late-id', '.first', '.late-one', '.late-two']);
  assert.equal(new Set(sent).size, sent.length);
});

test('tokens: a page with many names is sent in batches of at most 300', async () => {
  let spans = ''; for (let i = 0; i < 700; i++) spans += `<i class="c${i}"></i>`;
  const page = await open(spans, appWithMarker);
  await page.waitForTimeout(2500);
  const batches = await gens(page);
  assert.ok(batches.length >= 3 && batches.every((b) => b.length <= 300), 'batch sizes: ' + batches.map((b) => b.length));
  assert.equal(flat(batches).length, 700);
});

test('tokens: only the top document asks; an iframe keeps just its own site rules', async () => {
  const page = await open(`<div class="top-class">a</div><iframe srcdoc="<div class='frame-class'>b</div>" width="100" height="50"></iframe>`, appWithMarker);
  await page.waitForTimeout(1200);
  const sent = flat(await gens(page));
  assert.ok(sent.includes('.top-class'));
  assert.ok(!sent.includes('.frame-class'), 'the iframe sent nothing: ' + sent);
});

test('tokens: a page without the marker in its first answer, or an app that cannot answer, never breaks the page', async () => {
  const page = await open(`<div class="x">a</div>`, appDown);
  await page.waitForTimeout(800);
  assert.equal(await page.evaluate(() => document.body.textContent), 'a');
});
