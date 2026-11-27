'use strict';
// RX3-BK1 / RX3-BK2 (research/REVIEW-EXTERNAL-3.md section 3): content.js after Back and Forward, in real headless Chromium with a fake extension API:
//   - the leave-time scroll report (the app must know the place the owner left a page at),
//   - the `bf` report of what kind of show this was (engine cache or loaded again, what the page's file cost),
//   - the pull: when the engine loaded the page again, ask the app for the place and scroll there if the engine did not.
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

/**
 * A site whose pages cannot enter Chromium's own back/forward cache (Cache-Control: no-store), so Back loads them again - the case the engine's cache cannot help with.
 * `answer` is what the app answers to the page's {k:'bk'} question ('' = nothing armed), `delay` how long the answer takes (ms).
 * Chromium would restore the scroll place itself on Back: history.scrollRestoration is set to 'manual' so that what is measured is OUR pull.
 */
async function site(body, answer, delay) {
  const context = await browser.newContext({ hasTouch: true, viewport: { width: 400, height: 700 } });
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', headers: { 'cache-control': 'no-store' }, body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` }));
  const page = await context.newPage();
  await page.addInitScript((cfg) => {
    window.__msgs = [];
    history.scrollRestoration = 'manual';
    window.browser = { runtime: { sendMessage: (m) => {
      window.__msgs.push(m);
      if (m.k === 'bk') return new Promise((r) => setTimeout(() => r(cfg.answer), cfg.delay || 0));
      return Promise.resolve(undefined);
    } } };
  }, { answer: answer || '', delay: delay || 0 });
  await page.addInitScript({ content: CONTENT });
  return page;
}

/** Visit /a, then /b, then press Back: the engine loads /a again. */
async function backToA(page) {
  await page.goto('https://site.example/a');
  await page.goto('https://site.example/b');
  await page.goBack();
}
const kinds = (page) => page.evaluate(() => window.__msgs.map((m) => m.k));
const ofKind = (page, k) => page.evaluate((kk) => window.__msgs.filter((m) => m.k === kk), k);
const y = (page) => page.evaluate(() => Math.round(window.pageYOffset));
const TALL = '<div style="height:4000px"></div>';

test('a page the engine loaded again for Back reports a back_forward show with the size of its file', async () => {
  const page = await site(TALL, '');
  await backToA(page);
  await page.waitForFunction(() => window.__msgs.some((m) => m.k === 'bf'));
  const bf = await ofKind(page, 'bf');
  assert.equal(bf.length, 1);
  assert.equal(bf[0].p, 'https://site.example/a');
  assert.equal(bf[0].ps, 0);
  assert.equal(bf[0].nt, 'back_forward');
  assert.equal(typeof bf[0].ts, 'number');
  assert.equal(typeof bf[0].y, 'number');
});

test('a page the engine kept (pageshow persisted) is reported as kept at its place and nothing is asked', async () => {
  const page = await site(TALL, '900');
  await page.goto('https://site.example/a');
  await page.evaluate(() => window.scrollTo(0, 640));
  await page.evaluate(() => window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true })));
  const bf = await ofKind(page, 'bf');
  assert.equal(bf.length, 1);
  assert.equal(bf[0].ps, 1);
  assert.equal(bf[0].y, 640);
  assert.ok(!(await kinds(page)).includes('bk'), 'a kept page is already where it was');
});

test('a normal visit sends neither a show report nor a place question', async () => {
  const page = await site(TALL, '900');
  await page.goto('https://site.example/a');
  await page.waitForTimeout(500);
  const ks = await kinds(page);
  assert.ok(!ks.includes('bf') && !ks.includes('bk'), ks.join(','));
});

test('after Back the page is scrolled to the place the app answers', async () => {
  const page = await site(TALL, '1200');
  await backToA(page);
  await page.waitForFunction(() => Math.abs(window.pageYOffset - 1200) <= 4, null, { timeout: 5000 });
  assert.ok((await kinds(page)).includes('bk'));
  const bk = await ofKind(page, 'bk');
  assert.equal(bk.length, 1, 'one question per page load');
  assert.equal(bk[0].p, 'https://site.example/a');
});

test('a page that grows after the load is followed: the later looks reach the place once it is tall enough', async () => {
  const page = await site('<div id="g" style="height:600px"></div><script>setTimeout(function(){document.getElementById("g").style.height="4000px"},900)</script>', '1200');
  await backToA(page);
  await page.waitForFunction(() => Math.abs(window.pageYOffset - 1200) <= 4, null, { timeout: 6000 });
  assert.ok(Math.abs((await y(page)) - 1200) <= 4);
});

test('a touch before the answer arrives cancels the pull for good', async () => {
  const page = await site(TALL, '1200', 400);
  await backToA(page);
  await page.touchscreen.tap(50, 50);
  await page.waitForTimeout(1800);
  assert.ok((await y(page)) <= 4, 'the owner took over: the page stays where it is');
});

test('a touch between two looks stops the rest of them', async () => {
  const page = await site(TALL, '1200');
  await backToA(page);
  await page.waitForFunction(() => Math.abs(window.pageYOffset - 1200) <= 4, null, { timeout: 5000 });
  await page.evaluate(() => window.scrollTo(0, 300));      // the page moves off (or the owner scrolls): a later look would pull it back
  await page.touchscreen.tap(50, 50);
  await page.waitForTimeout(3500);
  assert.equal(await y(page), 300, 'no look after a touch');
});

test('an empty answer or a zero place leaves the page alone', async () => {
  for (const ans of ['', '0', 'x', '-5']) {
    const page = await site(TALL, ans);
    await backToA(page);
    await page.waitForFunction(() => window.__msgs.some((m) => m.k === 'bk'));
    await page.waitForTimeout(300);
    assert.equal(await y(page), 0, 'answer ' + JSON.stringify(ans));
    await page.context().close();
  }
});

test('a page that is already at its place is not scrolled again (within 4 px)', async () => {
  const page = await site(TALL, '1200');
  await backToA(page);
  await page.waitForFunction(() => Math.abs(window.pageYOffset - 1200) <= 4, null, { timeout: 5000 });
  await page.evaluate(() => { window.__scrolls = 0; const o = window.scrollTo; window.scrollTo = function () { window.__scrolls++; return o.apply(window, arguments); }; });
  await page.waitForTimeout(3300);
  assert.equal(await page.evaluate(() => window.__scrolls), 0);
});

test('leaving the page sends its place at once, not 300 ms after the last scroll', async () => {
  const page = await site(TALL, '');
  await page.goto('https://site.example/a');
  await page.evaluate(() => { window.scrollTo(0, 777); window.dispatchEvent(new Event('pagehide')); });
  const m = await ofKind(page, 'scroll');
  assert.equal(m[m.length - 1].y, 777);
  assert.equal(m[m.length - 1].p, 'https://site.example/a');
  // and the debounced report that is still pending does not repeat it
  const n = m.length;
  await page.waitForTimeout(600);
  assert.equal((await ofKind(page, 'scroll')).length, n);
});

test('a frame never asks or reports a show (top frame only)', async () => {
  const page = await site('<iframe src="https://site.example/frame" style="width:300px;height:200px"></iframe>', '500');
  await page.goto('https://site.example/a');
  await page.waitForTimeout(500);
  const frame = page.frames().find((f) => f !== page.mainFrame());
  assert.ok(frame, 'the iframe is there');
  const inFrame = await frame.evaluate(() => (window.__msgs || []).map((m) => m.k));
  assert.ok(!inFrame.includes('bf') && !inFrame.includes('bk') && !inFrame.includes('scroll'), inFrame.join(','));
});
