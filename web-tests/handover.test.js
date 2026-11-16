'use strict';
// ANS54e (owner message 54: "I want the web video players to use this"): the page half of the hand-over in real headless Chromium. gate-main.js (world MAIN, no extension API) asks, content.js (isolated copy with a fake
// extension API) checks and relays the question as a `vhand` message, the "background page" (this test) answers {h: 1} (Kestrel's own player took the film) or {h: 0}. Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SHIELDS = path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const read = (f) => fs.readFileSync(path.join(SHIELDS, f), 'utf8');
const VFIND = read('vfind.js'), CONTENT = read('content.js'), GATE = read('gate-main.js');
let browser;
before(async () => { browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required'] }); });
after(async () => { await browser.close(); });

/**
 * [html]: the body. [guard]: what the background page answers to `guard` (gt: the rule is on, ho: the app may take films over). [handAnswer]: 'yes' | 'no' | 'never' | 'late' (a yes after 1.2 s) for `vhand`.
 * window.__nativePlay: the engine's own play(), kept before the gate replaces it (the way the engine's own controls start a film: not through the page-visible play()).
 */
async function open(html, guard, handAnswer) {
  const context = await browser.newContext({ hasTouch: false });
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.pathname.endsWith('.mp4')) return r.fulfill({ status: 200, contentType: 'video/mp4', body: Buffer.alloc(2000) });
    return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${html}</body></html>` });
  });
  const page = await context.newPage();
  await page.addInitScript(({ g, h }) => {
    window.__msgs = []; window.__pauses = 0; window.__nativePlay = HTMLMediaElement.prototype.play;
    const op = HTMLMediaElement.prototype.pause;
    HTMLMediaElement.prototype.pause = function pause() { window.__pauses++; return op.apply(this, arguments); };   // (the gate keeps the original it finds: this one)
    window.browser = { runtime: {
      sendMessage: (m) => {
        window.__msgs.push(m);
        if (m && m.k === 'guard') return Promise.resolve(g);
        if (m && m.k === 'vhand') {
          if (h === 'yes') return Promise.resolve({ h: 1 });
          if (h === 'no') return Promise.resolve({ h: 0 });
          if (h === 'late') return new Promise((res) => setTimeout(() => res({ h: 1 }), 1200));
          return new Promise(() => {});
        }
        return Promise.resolve(undefined);
      },
      onMessage: { addListener() {} },
    } };
  }, { g: guard, h: handAnswer });
  await page.addInitScript({ content: VFIND });
  await page.addInitScript({ content: CONTENT });
  // the gate lives in the page's world and must not see the extension API
  await page.addInitScript({ content: `(function(){var b=window.browser;try{delete window.browser;}catch(e){window.browser=undefined;}${GATE};window.browser=b;})();` });
  await page.goto('https://site.example/');
  return page;
}
const GUARD_HO = { on: true, top: 'site.example', gt: true, ho: true };
const PLAYER = '<div id="player" style="position:relative;width:320px;height:180px"><video id="v" src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video><button id="btn" style="position:absolute;left:100px;top:60px;width:120px;height:60px">Play</button></div>'
  + '<div id="far" style="width:300px;height:80px;margin-top:40px"><button id="other" style="width:200px;height:50px">Other</button></div>'
  + '<script>window.__r = ""; function go() { window.__r = "waiting"; document.getElementById("v").play().then(function () { window.__r = "ok"; }, function (e) { window.__r = e.name; }); }'
  + 'document.getElementById("btn").onclick = go; document.getElementById("other").onclick = function () { setTimeout(go, 100); };</script>';
const tapOn = async (page, sel) => { const b = await page.locator(sel).boundingBox(); await page.mouse.click(b.x + b.width / 2, b.y + b.height / 2); };
const settle = (page, ms) => page.waitForTimeout(ms || 300);
const vhand = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'vhand'));
const result = (page) => page.evaluate(() => window.__r);

test('a press on the player\'s own button asks the app first; yes = the page\'s play() is interrupted and the element stays closed', async () => {
  const page = await open(PLAYER, GUARD_HO, 'yes');
  await settle(page, 400);
  await tapOn(page, '#btn'); await settle(page, 500);
  const q = await vhand(page);
  assert.equal(q.length, 1, 'one question: ' + JSON.stringify(q));
  assert.equal(q[0].y, 'press', 'the tap on a play button over the picture asks first (the page\'s play() joins that question)'); assert.equal(q[0].s, 'https://cdn.example/a.mp4'); assert.equal(q[0].b, 0); assert.equal(q[0].l, 0);
  assert.ok(q[0].w >= 300 && q[0].h >= 170, 'the box: ' + q[0].w + 'x' + q[0].h);
  assert.equal(await result(page), 'AbortError', 'the page\'s play() was interrupted, not refused with an error screen');
  await tapOn(page, '#btn'); await settle(page, 500);
  assert.equal((await vhand(page)).length, 2, 'pressed again: asked again (the film comes back to Kestrel\'s player)');
});

test('no = the film plays in the page as before (the question was asked once)', async () => {
  const page = await open(PLAYER, GUARD_HO, 'no');
  await tapOn(page, '#btn'); await settle(page, 600);
  assert.equal((await vhand(page)).length, 1);
  assert.notEqual(await result(page), 'AbortError', 'the page\'s own play went on: ' + await result(page));
  await tapOn(page, '#btn'); await settle(page, 300);
  assert.equal((await vhand(page)).length, 1, 'an element that opened is not asked about again');
});

test('no answer within 700 ms: the page plays the film itself', async () => {
  const page = await open(PLAYER, GUARD_HO, 'never');
  await tapOn(page, '#btn'); await settle(page, 300);
  assert.equal(await result(page), 'waiting', 'held while the app thinks');
  await settle(page, 700);
  assert.notEqual(await result(page), 'AbortError'); assert.notEqual(await result(page), 'waiting', 'the wait ended: ' + await result(page));
});

test('a yes that comes too late still stops the page\'s copy', async () => {
  const page = await open(PLAYER, GUARD_HO, 'late');
  await tapOn(page, '#btn'); await settle(page, 900);
  const before = await page.evaluate(() => window.__pauses);
  await settle(page, 900);
  assert.ok(await page.evaluate(() => window.__pauses) > before, 'the element was paused when the late yes came');
});

test('the app may not take films over (flag off, or the rule off): no question is asked', async () => {
  for (const g of [{ on: true, top: 'site.example', gt: true, ho: false }, { on: true, top: 'site.example', gt: false, ho: true }]) {
    const page = await open(PLAYER, g, 'yes');
    await tapOn(page, '#btn'); await settle(page, 500);
    assert.equal((await vhand(page)).length, 0, JSON.stringify(g));
  }
});

test('a play() after a tap somewhere else is not a press on this player: no question, the page plays as the activation allows', async () => {
  const page = await open(PLAYER, GUARD_HO, 'yes');
  await tapOn(page, '#other'); await settle(page, 600);
  assert.equal((await vhand(page)).length, 0);
  assert.notEqual(await result(page), 'AbortError');
});

test('the engine\'s own controls: a press opens the video, the app is asked, a yes pauses it at once', async () => {
  const page = await open('<video id="v" controls src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video>', GUARD_HO, 'yes');
  await settle(page, 300);
  await tapOn(page, '#v'); await settle(page, 600);
  const q = await vhand(page);
  assert.equal(q.length, 1); assert.equal(q[0].y, 'press');
  assert.ok(await page.evaluate(() => window.__pauses) >= 1, 'paused after the yes');
});

const CONTROLS = '<video id="v" controls src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video>';
// what the engine's own controls do on a press: the engine's play(), no tap event for the page (Gecko, lab run 43826954106). Playwright's evaluate() carries a user activation, like the tap would.
const nativePlay = (page) => page.evaluate(() => { const p = window.__nativePlay.call(document.getElementById('v')); if (p && p.catch) p.catch(() => {}); });
test('the engine\'s own controls start the film with no tap event reaching the page (Gecko): the engine\'s play event is the press', async () => {
  const page = await open(CONTROLS, GUARD_HO, 'yes');
  await settle(page, 300);
  assert.equal(await page.evaluate(() => document.getElementById('v').getAttribute('preload')), 'none', 'closed until pressed');
  await nativePlay(page); await settle(page, 700);
  const q = await vhand(page);
  assert.equal(q.length, 1, JSON.stringify(q)); assert.equal(q[0].y, 'press'); assert.equal(q[0].s, 'https://cdn.example/a.mp4');
  assert.ok(await page.evaluate(() => window.__pauses) >= 1, 'a yes pauses the page\'s copy');
  assert.equal(await page.evaluate(() => document.getElementById('v').getAttribute('preload')), null, 'opened by the press: the page\'s own preload is back');
});

test('the same press with the hand-over flag off opens the element and asks nothing', async () => {
  const page = await open(CONTROLS, { on: true, top: 'site.example', gt: true, ho: false }, 'yes');
  await settle(page, 300);
  await nativePlay(page); await settle(page, 600);
  assert.equal((await vhand(page)).length, 0);
  assert.equal(await page.evaluate(() => document.getElementById('v').getAttribute('preload')), null, 'opened by the engine\'s own play');
});

test('after a yes the engine\'s play button pressed again asks again; after a no the film is left alone (no question for every pause and play)', async () => {
  const yes = await open(CONTROLS, GUARD_HO, 'yes');
  await nativePlay(yes); await settle(yes, 700);
  await nativePlay(yes); await settle(yes, 700);
  assert.equal((await vhand(yes)).length, 2, 'asked again: the film comes back to Kestrel\'s player');
  const no = await open(CONTROLS, GUARD_HO, 'no');
  await nativePlay(no); await settle(no, 700);                                  // plays here (the app said no)
  await no.evaluate(() => document.getElementById('v').pause()); await settle(no, 300);
  await nativePlay(no); await settle(no, 700);                                  // play again
  assert.equal((await vhand(no)).length, 1, 'one question, not one for every play');
});

test('a play() the page makes itself after a hand-over is the replacement\'s business, not a press on the engine\'s controls: no second question', async () => {
  const page = await open(CONTROLS, GUARD_HO, 'yes');
  await nativePlay(page); await settle(page, 1800);                             // yes: the element is open and was handed over
  await page.evaluate(() => { const p = document.getElementById('v').play(); if (p && p.catch) p.catch(() => {}); }); await settle(page, 700);
  assert.equal((await vhand(page)).length, 1, 'the page\'s own play() is not asked again by the engine\'s play event');
});

test('a video without controls is not asked about when it is merely tapped (a hero loop has no play button)', async () => {
  const page = await open('<video id="v" autoplay muted loop src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video>', GUARD_HO, 'yes');
  await settle(page, 300);
  await tapOn(page, '#v'); await settle(page, 500);
  assert.equal((await vhand(page)).length, 0);
});

test('Kestrel\'s own controls (the event the video bar sends) ask too, and a yes pauses', async () => {
  const page = await open('<video id="v" src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video>', GUARD_HO, 'yes');
  await settle(page, 300);
  await page.evaluate(() => { document.getElementById('v').dispatchEvent(new Event('__kst_gate_v1')); });
  await settle(page, 500);
  const q = await vhand(page);
  assert.equal(q.length, 1); assert.equal(q[0].y, 'kestrel');
  assert.ok(await page.evaluate(() => window.__pauses) >= 1);
});

test('a MediaSource player: the question says blob and names no address; a yes keeps the source held', async () => {
  const html = PLAYER.replace('<video id="v" src="https://cdn.example/a.mp4"', '<video id="v"')
    + '<script>window.ms = new MediaSource(); window.opened = 0; ms.addEventListener("sourceopen", function () { window.opened++; }); document.getElementById("v").src = URL.createObjectURL(ms);</script>';
  const page = await open(html, GUARD_HO, 'yes');
  await settle(page, 300);
  await tapOn(page, '#btn'); await settle(page, 500);
  const q = await vhand(page);
  assert.equal(q.length, 1); assert.equal(q[0].b, 1); assert.equal(q[0].s, '');
  assert.equal(await page.evaluate(() => window.opened), 0, 'the player was kept from attaching: no sourceopen, no segment');
  assert.equal(await result(page), 'AbortError');
});

test('the content script checks what the page says: a bad address is dropped, numbers are clamped, a document gets a few dozen questions', async () => {
  const page = await open('<div>x</div>', GUARD_HO, 'no');
  await settle(page, 300);
  await page.evaluate(() => {
    const ask = (d) => document.dispatchEvent(new CustomEvent('__kst_gate_ask_v1', { detail: JSON.stringify(d) }));
    ask({ i: 1, y: 'play', s: 'javascript:alert(1)', b: 0, m: 'video/mp4<script>', l: 0, w: 99999999, h: -4, t: 5, d: 9e12 });
    ask({ i: 0, y: 'play', s: 'https://x.example/a.mp4' });            // no id: nothing
    ask({ i: 2, y: 'rm -rf', s: 'https://x.example/a.mp4', b: 1, w: 640, h: 360 });
    document.dispatchEvent(new CustomEvent('__kst_gate_ask_v1', { detail: 'not json' }));
    for (let i = 10; i < 120; i++) ask({ i: i, y: 'play', s: 'https://x.example/a.mp4', w: 640, h: 360 });
  });
  await settle(page, 500);
  const q = await vhand(page);
  assert.ok(q.length <= 60, 'at most 60 questions: ' + q.length);
  assert.equal(q[0].s, '', 'not an http(s) address'); assert.ok(!/[<>]/.test(q[0].m)); assert.equal(q[0].w, 0, 'out of range'); assert.equal(q[0].h, 0); assert.equal(q[0].d, 0);
  assert.equal(q[1].y, 'play', 'an unknown reason is a play');
});
