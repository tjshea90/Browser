'use strict';
// ANS52g, the page half of the whole chain in real headless Chromium: content.js (isolated copy, fake extension API) asks the "background page" (the test) for the page answer, hands the video format policy it
// carries (`cx`) to codec-main.js with a DOM event, and relays what the page script refused as a `vcx` message. Its own file: content.test.js is already near the 60 s per-file test timeout.
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SHIELDS = path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const read = (f) => fs.readFileSync(path.join(SHIELDS, f), 'utf8');
const VFIND = read('vfind.js'), CONTENT = read('content.js'), CODEC = read('codec-main.js');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

const POLICY = '00|2048x1088@522000|2048x1088@522000|2048x1088@244800|-';
const VP9_4K60 = 'video/webm; codecs="vp09.00.40.08"; width=3840; height=2160; framerate=60';

/** [answer]: what the background page says to the `guard` question: an object, 'fail' (the call is rejected) or a function (page script side effects are not needed). [order]: 'content-first' | 'codec-first'. */
async function open(answer, order) {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><html><head><title>t</title></head><body>page</body></html>' }));
  const page = await context.newPage();
  await page.addInitScript((a) => {
    window.__msgs = [];
    window.__answer = a;
    window.browser = { runtime: {
      sendMessage: (m) => { window.__msgs.push(m); if (m && m.k === 'guard') return window.__answer === 'fail' ? Promise.reject(new Error('no background')) : Promise.resolve(window.__answer); return Promise.resolve(undefined); },
      onMessage: { addListener() {} },
    } };
  }, answer);
  // the page's own stand-in for the engine: it says yes to everything, so a "no" can only come from the policy
  await page.addInitScript(() => {
    Object.defineProperty(MediaSource, 'isTypeSupported', { value: function isTypeSupported() { return true; }, writable: true, enumerable: true, configurable: true });
  });
  const scripts = order === 'codec-first' ? [CODEC, VFIND, CONTENT] : [VFIND, CONTENT, CODEC];
  // the codec script lives in the page's world and must not see the extension API: the fake `browser` is hidden from it the way the real isolated world is
  for (const s of scripts) {
    if (s === CODEC) await page.addInitScript({ content: `(function(){var b=window.browser;try{delete window.browser;}catch(e){window.browser=undefined;}${s};window.browser=b;})();` });
    else await page.addInitScript({ content: s });
  }
  await page.goto('https://site.example/');
  return page;
}
const settle = (page) => page.waitForTimeout(400);
const ask = (page, t) => page.evaluate((x) => MediaSource.isTypeSupported(x), t);
const vcx = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'vcx'));

for (const order of ['content-first', 'codec-first']) {
  test(`${order}: the policy of the guard answer reaches the page script and a refusal comes back as one vcx message`, async () => {
    const page = await open({ on: true, top: 'site.example', cx: POLICY }, order);
    await settle(page);
    assert.equal(await ask(page, VP9_4K60), false, 'refused by the policy');
    assert.equal(await ask(page, 'video/mp4; codecs="avc1.640028"; width=1280; height=720; framerate=30'), true, 'what is fine goes to the engine');
    await ask(page, VP9_4K60);
    await page.waitForFunction(() => window.__msgs.some((m) => m.k === 'vcx'), null, { timeout: 5000 });
    const v = await vcx(page);
    assert.equal(v.length, 1);
    assert.deepEqual(v[0], { k: 'vcx', a: 3, s: 2, m: 0, f: 0, w: 'vp9 3840x2160@60' });
  });
}

test('an empty policy (told "none") and an answer without one (an older background page) leave every answer to the engine', async () => {
  for (const answer of [{ on: true, top: 'site.example', cx: '' }, { on: true, top: 'site.example' }]) {
    const page = await open(answer);
    await settle(page);
    assert.equal(await ask(page, VP9_4K60), true, JSON.stringify(answer));
    assert.deepEqual(await vcx(page), []);
  }
});

test('when the background page does not answer, the page script stays inert and the page works', async () => {
  const page = await open('fail');
  await settle(page);
  assert.equal(await ask(page, VP9_4K60), true);
  assert.equal(await page.evaluate(() => document.body.textContent.trim()), 'page');
});

test('the policy is also told to a frame (every frame has its own copies of both scripts)', async () => {
  const page = await open({ on: true, top: 'site.example', cx: POLICY });
  await settle(page);
  const r = await page.evaluate(() => new Promise((res) => {
    const f = document.createElement('iframe'); document.body.appendChild(f);
    setTimeout(() => res(f.contentWindow.MediaSource.isTypeSupported('video/webm; codecs="vp9"; width=3840; height=2160')), 300);
  }));
  assert.equal(r, false);
});

test('a page that forges the report event gets nothing through but well-formed numbers, a short plain word and ten messages', async () => {
  const page = await open({ on: true, top: 'site.example', cx: POLICY });
  await settle(page);
  await page.evaluate(() => {
    const send = (d) => document.dispatchEvent(new CustomEvent('__kst_codec_rep_v1', { detail: d }));
    send('1|2|3'); send('a|b|c|d|e'); send('-1|0|0|0|x'); send(42); send(null); send({ a: 1 }); send('1|2000000000|0|0|x'); send('1|1|1|1|a|b');   // none of these is a report
    send('5|4|0|1|<img src=x onerror=alert(1)> https://evil.example/' + 'q'.repeat(200));
    for (let i = 0; i < 40; i++) send('1|1|0|0|vp9');
  });
  const v = await vcx(page);
  assert.equal(v.length, 10, 'one document gets ten reports through');
  assert.deepEqual([v[0].a, v[0].s, v[0].m, v[0].f], [5, 4, 0, 1]);
  assert.match(v[0].w, /^[a-z0-9 x@.]{0,30}$/);
  assert.ok(!/[<>:\/]/.test(v[0].w));
});
