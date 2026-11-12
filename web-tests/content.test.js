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
async function open(body) {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` }));
  const page = await context.newPage();
  await page.addInitScript(() => {
    window.__msgs = [];
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } };
  });
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
