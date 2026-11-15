'use strict';
// RX4-M0e (research/REVIEW-EXTERNAL-4.md): the opt-in scroll measurement. Only when the app's answer to the `guard` question carries sg:true (the Settings switch "Measure scrolling on real pages") the
// top frame counts the frames of a scroll burst (first scroll event until 200 ms after the last one) and sends {k:'sg', n, a, b, mx}: frames, gaps over 50 ms, over 100 ms, the longest gap. Numbers only.
// Switch off (the default): nothing is measured, no frame callback is ever requested by the script and nothing is sent.
// Real headless Chromium, fake extension API.  Run: cd web-tests && npm test
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

async function open(sg) {
  const context = await browser.newContext({ viewport: { width: 500, height: 600 } });
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><html><head><title>t</title></head><body style="margin:0"><div style="height:20000px;background:linear-gradient(#fff,#888)">tall</div></body></html>' }));
  const page = await context.newPage();
  await page.addInitScript((answer) => {
    window.__msgs = [];
    window.__raf = 0;
    const raf = window.requestAnimationFrame.bind(window);
    window.requestAnimationFrame = (f) => { window.__raf++; return raf(f); };
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(m.k === 'guard' ? { on: false, sg: answer } : undefined); } } };
  }, sg);
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/page');
  return page;
}
const sg = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'sg'));
async function scrollFor(page, ms) {
  const end = Date.now() + ms;
  while (Date.now() < end) { await page.mouse.wheel(0, 120); await page.waitForTimeout(30); }
  await page.waitForTimeout(450);
}

test('switch on: a scroll burst is reported as numbers (frames, gaps, the longest gap) and nothing else', async () => {
  const page = await open(true);
  await scrollFor(page, 1200);
  const m = await sg(page);
  assert.ok(m.length >= 1, 'a burst was reported: ' + JSON.stringify(m));
  assert.deepEqual(Object.keys(m[0]).sort(), ['a', 'b', 'k', 'mx', 'n']);
  for (const k of ['n', 'a', 'b', 'mx']) assert.equal(typeof m[0][k], 'number', k);
  assert.ok(m[0].n >= 5 && m[0].n <= 5000, 'frames ' + m[0].n);
  assert.ok(m[0].b <= m[0].a && m[0].a <= m[0].n, 'gaps are consistent');
});

test('switch off (the default): nothing is measured, not even a frame callback is requested', async () => {
  const page = await open(false);
  await scrollFor(page, 1200);
  assert.deepEqual(await sg(page), []);
  assert.equal(await page.evaluate(() => window.__raf), 0, 'the script asked for no frame callback');
});

test('a second burst after a pause is a second report', async () => {
  const page = await open(true);
  await scrollFor(page, 800);
  const first = (await sg(page)).length;
  await page.waitForTimeout(500);
  await scrollFor(page, 800);
  assert.ok((await sg(page)).length > first, 'two bursts, two reports');
});

test('a page that never scrolls sends nothing', async () => {
  const page = await open(true);
  await page.waitForTimeout(500);
  assert.deepEqual(await sg(page), []);
});
