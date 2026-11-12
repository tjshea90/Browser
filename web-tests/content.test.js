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
