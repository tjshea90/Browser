'use strict';
// RX3-G2 (research/REVIEW-EXTERNAL-3.md part G): the evidence "the owner really tapped this link" must not be forgeable by the page.
// content.js (the isolated content script) records real taps itself (trusted click / pointerup events only); the guard in the page world may still REPORT what it refused (open, click, overlay,
// form, redirect), but a `tap|...` or `vmenu|...` that arrives through the page-visible report event is never believed: a page script can dispatch that event itself.
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

const BODY = `<a id="l" href="https://shop.example/item" onclick="return false" style="display:block;width:300px;height:100px">a link</a>
<div id="d" style="width:300px;height:100px;margin-top:20px">not a link</div>
<a id="far" href="https://other.example/x" onclick="return false" style="display:block;width:300px;height:60px;margin-top:20px">another link</a>`;

async function open(opts) {
  const o = opts || {};
  const context = await browser.newContext({ hasTouch: !!o.touch, viewport: { width: 400, height: 700 } });
  // the page itself gets the body of the test, a frame address ending in /inner gets a plain document (so a frame never nests itself)
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${r.request().url().indexOf('/inner') > 0 ? '<div style="height:100px">inner document</div>' : (o.body || BODY)}</body></html>` }));
  const page = await context.newPage();
  await page.addInitScript(() => {
    window.__msgs = [];
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } };
  });
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/');
  return page;
}
const pops = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'pop').map((m) => m.d));
const taps = async (page) => (await pops(page)).filter((d) => d.indexOf('tap|') === 0);
const forge = (page, detail) => page.evaluate((d) => document.dispatchEvent(new CustomEvent('__kst_pop_v1', { detail: d })), detail);

test('a real click on a link is reported once as a tap with the link\'s address', async () => {
  const page = await open();
  await page.mouse.click(50, 50);
  assert.deepEqual(await taps(page), ['tap|https://shop.example/item']);
});

test('a real click on something that is not a link is a tap with no address', async () => {
  const page = await open();
  await page.mouse.click(50, 150);
  assert.deepEqual(await taps(page), ['tap|']);
});

test('a script that dispatches the report event itself is not believed: no tap, no long-press report', async () => {
  const page = await open();
  await forge(page, 'tap|https://ad.test/');
  await forge(page, 'vmenu|https://cdn.example/v.mp4');
  await forge(page, 'tap|');
  assert.deepEqual(await pops(page), []);
});

test('a script that clicks a link is not a tap (untrusted events never count)', async () => {
  const page = await open();
  await page.evaluate(() => { document.getElementById('l').click(); document.getElementById('l').dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true })); });
  assert.deepEqual(await taps(page), []);
});

test('what the guard refused is still forwarded, and only those kinds', async () => {
  const page = await open();
  for (const k of ['open', 'click', 'overlay', 'form', 'redirect']) await forge(page, k + '|https://ad.test/x');
  for (const k of ['tap', 'vmenu', 'os', 'foo', '', 'OPEN']) await forge(page, k + '|https://ad.test/y');
  await forge(page, 'open');                                    // no address: still a refusal report
  assert.deepEqual(await pops(page), ['open|https://ad.test/x', 'click|https://ad.test/x', 'overlay|https://ad.test/x', 'form|https://ad.test/x', 'redirect|https://ad.test/x', 'open']);
});

test('a report event with a hostile detail never breaks the page or the script', async () => {
  const page = await open();
  await page.evaluate(() => {
    document.dispatchEvent(new CustomEvent('__kst_pop_v1', { detail: { toString() { throw new Error('boom'); } } }));
    document.dispatchEvent(new CustomEvent('__kst_pop_v1', { detail: null }));
    document.dispatchEvent(new CustomEvent('__kst_pop_v1', { detail: 'open|' + 'x'.repeat(5000) }));
  });
  const p = await pops(page);
  assert.equal(p.length, 1);
  assert.ok(p[0].length <= 700, 'the address is cut');
});

test('a real tap after a forged report still gives exactly one honest tap', async () => {
  const page = await open();
  await forge(page, 'tap|https://ad.test/');
  await page.mouse.click(50, 50);
  assert.deepEqual(await taps(page), ['tap|https://shop.example/item']);
});

test('a touch tap (touch events, pointerup, then the click) is one report', async () => {
  const page = await open({ touch: true });
  await page.touchscreen.tap(50, 50);
  assert.deepEqual(await taps(page), ['tap|https://shop.example/item']);
});

test('a tap whose click the page cancels is still reported (pointerup)', async () => {
  const page = await open({ touch: true, body: `<a id="l" href="https://shop.example/item" style="display:block;width:300px;height:100px">a link</a><script>document.getElementById('l').addEventListener('touchend', function (e) { e.preventDefault(); });</script>` });
  await page.touchscreen.tap(50, 50);
  assert.deepEqual(await taps(page), ['tap|https://shop.example/item']);
});

test('a swipe that scrolls the page is not a tap', async () => {
  const page = await open({ touch: true, body: `<div style="height:3000px"><a id="l" href="https://shop.example/item" style="display:block;width:300px;height:100px">a link</a></div>` });
  const cdp = await page.context().newCDPSession(page);
  await cdp.send('Input.synthesizeScrollGesture', { x: 100, y: 300, yDistance: -400, gestureSourceType: 'touch', speed: 800 });
  assert.deepEqual(await taps(page), []);
});

test('a long press with a finger is not a tap', async () => {
  const page = await open({ touch: true });
  const cdp = await page.context().newCDPSession(page);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: 50, y: 50 }] });
  await page.waitForTimeout(700);                                  // held: a long press (the page's menu), not a tap
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  await page.waitForTimeout(100);
  assert.deepEqual(await taps(page), []);
});

test('a right click is not a tap', async () => {
  const page = await open();
  await page.mouse.click(50, 50, { button: 'right' });
  assert.deepEqual(await taps(page), []);
});

test('a drag that starts on a link and ends elsewhere is not a tap on the link', async () => {
  const page = await open();
  await page.mouse.move(40, 50); await page.mouse.down(); await page.mouse.move(200, 190, { steps: 4 }); await page.mouse.up();
  const t = await taps(page);
  assert.ok(!t.includes('tap|https://shop.example/item'), JSON.stringify(t));
});

test('two different links tapped in a row are two reports; the same link twice within a blink is one', async () => {
  const page = await open();
  await page.mouse.click(50, 50);
  await page.waitForTimeout(800);
  await page.mouse.click(50, 280);
  const t = await taps(page);
  assert.deepEqual(t, ['tap|https://shop.example/item', 'tap|https://other.example/x']);
});

test('a frame reports its own tap too (the guard runs in frames)', async () => {
  const page = await open({ body: `<iframe src="https://site.example/inner" style="width:300px;height:100px;border:0"></iframe>` });
  const frame = page.frames().find((f) => f !== page.mainFrame());
  assert.ok(frame);
  await page.mouse.click(50, 50);
  const inFrame = await frame.evaluate(() => (window.__msgs || []).filter((m) => m.k === 'pop').map((m) => m.d));
  assert.deepEqual(inFrame, ['tap|']);
});
