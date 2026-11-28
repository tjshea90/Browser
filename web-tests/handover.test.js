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
// 5 seconds of silence as a WAV file (8 kHz, 8 bit, mono): the smallest thing a <video> element really plays in headless Chromium, so that play, pause and play again behave like a real film
const WAV = (() => { const rate = 8000, n = rate * 5, b = Buffer.alloc(44 + n, 128); b.write('RIFF', 0); b.writeUInt32LE(36 + n, 4); b.write('WAVEfmt ', 8); b.writeUInt32LE(16, 16); b.writeUInt16LE(1, 20); b.writeUInt16LE(1, 22); b.writeUInt32LE(rate, 24); b.writeUInt32LE(rate, 28); b.writeUInt16LE(1, 32); b.writeUInt16LE(8, 34); b.write('data', 36); b.writeUInt32LE(n, 40); return b; })();
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
    if (u.pathname.endsWith('.wav')) return r.fulfill({ status: 200, contentType: 'audio/wav', body: WAV });         // (a film the engine can really play: silence, 5 s; a <video> plays it like any file)
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

const CONTROLS = '<video id="v" controls src="https://cdn.example/a.wav" style="display:block;width:320px;height:180px"></video>';
// what the engine's own controls do on a press: the engine's play(), no tap event for the page (Gecko, lab run 43826954106). Playwright's evaluate() carries a user activation, like the tap would.
const nativePlay = (page) => page.evaluate(() => { const p = window.__nativePlay.call(document.getElementById('v')); if (p && p.catch) p.catch(() => {}); });
test('the engine\'s own controls start the film with no tap event reaching the page (Gecko): the engine\'s play event is the press', async () => {
  const page = await open(CONTROLS, GUARD_HO, 'yes');
  await settle(page, 300);
  assert.equal(await page.evaluate(() => document.getElementById('v').getAttribute('preload')), 'none', 'closed until pressed');
  await nativePlay(page); await settle(page, 700);
  const q = await vhand(page);
  assert.equal(q.length, 1, JSON.stringify(q)); assert.equal(q[0].y, 'press'); assert.equal(q[0].s, 'https://cdn.example/a.wav');
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

// ---- the independent review of the hand-over (2026-11-16): each of these failed before the fix
test('a question that a script of the page makes up is not relayed while nobody touched the page; the same question right after a real tap is (the evidence is a person\'s touch of this document)', async () => {
  const FORGE = '<div id="x" style="width:200px;height:100px">x</div><script>function forge(n) { document.dispatchEvent(new CustomEvent("__kst_gate_ask_v1", { detail: JSON.stringify({ i: n, y: "play", s: "https://evil.example/x.mp4", b: 0, w: 640, h: 360 }) })); } setTimeout(function () { forge(7); }, 150);</script>';
  const quiet = await open(FORGE, GUARD_HO, 'yes');
  await settle(quiet, 700);
  assert.equal((await vhand(quiet)).length, 0, 'no touch, no relay');
  const touched = await open(FORGE, GUARD_HO, 'yes');
  await settle(touched, 500);                                                      // (the page's own timer has fired by now: nobody had touched the page yet)
  assert.equal((await vhand(touched)).length, 0);
  await tapOn(touched, '#x'); await touched.evaluate(() => forge(8)); await settle(touched, 500);
  assert.equal((await vhand(touched)).length, 1, 'after a real tap the document may ask (the app then wants a finger of its own)');
});

test('Mute, Share or Get started over the picture is no play control: no question, and nothing starts', async () => {
  const html = PLAYER.replace('<button id="btn"', '<button id="mute" style="position:absolute;left:8px;top:8px;width:70px;height:40px">Mute</button><button id="btn"');
  const page = await open(html, GUARD_HO, 'yes');
  await settle(page, 300);
  await tapOn(page, '#mute'); await settle(page, 500);
  assert.equal((await vhand(page)).length, 0);
  await tapOn(page, '#btn'); await settle(page, 500);
  assert.equal((await vhand(page)).length, 1, 'the Play button still asks');
});

test('players that name their play control with the word play ask first: aria-label, class, id and title', async () => {
  for (const attrs of ['aria-label="Play video"', 'class="vjs-big-play-button"', 'id="PlayButton"', 'title="Play"']) {
    const html = '<div id="player" style="position:relative;width:320px;height:180px"><video id="v" src="https://cdn.example/a.wav" style="display:block;width:320px;height:180px"></video><button ' + attrs + ' style="position:absolute;left:100px;top:60px;width:120px;height:60px"></button></div>';
    const page = await open(html, GUARD_HO, 'yes');
    await settle(page, 300);
    await tapOn(page, 'button'); await settle(page, 500);
    assert.equal((await vhand(page)).length, 1, attrs);
  }
});

test('Kestrel\'s own bar asks when the press that showed it has opened the element already', async () => {
  const page = await open('<video id="v" src="https://cdn.example/a.wav" style="display:block;width:320px;height:180px"></video>', GUARD_HO, 'yes');
  await settle(page, 300);
  await tapOn(page, '#v'); await settle(page, 400);                               // (a tap on a bare film: the element opens, nothing is asked: it has no play button)
  assert.equal((await vhand(page)).length, 0);
  await page.evaluate(() => { document.getElementById('v').dispatchEvent(new Event('__kst_gate_v1')); }); await settle(page, 500);
  const q = await vhand(page);
  assert.equal(q.length, 1); assert.equal(q[0].y, 'kestrel');
  assert.ok(await page.evaluate(() => window.__pauses) >= 1);
});

test('a yes that came late counts as a yes: the engine\'s play button pressed again asks again', async () => {
  const page = await open(CONTROLS, GUARD_HO, 'late');                              // (the app answers after 1.2 s: the page plays for a moment, the late yes stops it)
  await nativePlay(page); await settle(page, 1900);
  assert.equal((await vhand(page)).length, 1);
  await nativePlay(page); await settle(page, 400);
  assert.equal((await vhand(page)).length, 2, 'asked again');
});

test('after a yes the page\'s own play() is interrupted until the person presses again (not for 1.5 s only)', async () => {
  const page = await open(PLAYER, GUARD_HO, 'yes');
  await settle(page, 300);
  await tapOn(page, '#btn'); await settle(page, 700);                              // yes: AbortError
  assert.equal(await result(page), 'AbortError');
  await page.evaluate(() => { window.__r = ''; const p = document.getElementById('v').play(); p.then(() => { window.__r = 'ok'; }, (e) => { window.__r = e.name; }); });
  await settle(page, 500);
  assert.equal(await result(page), 'AbortError', 'a play() two seconds later, with no new press, is still the same film');
  await tapOn(page, '#far'); await settle(page, 200);                               // a new tap of the person
  await page.evaluate(() => { window.__r = ''; const p = document.getElementById('v').play(); p.then(() => { window.__r = 'ok'; }, (e) => { window.__r = e.name; }); });
  await settle(page, 600);
  assert.notEqual(await result(page), 'AbortError', 'after a new press the page decides again: ' + await result(page));
});

test('a script player on the engine\'s own controls keeps its source held until the app has answered (yes: held for good, no: it opens)', async () => {
  const html = '<video id="v" controls style="display:block;width:320px;height:180px"></video><script>window.ms = new MediaSource(); window.opened = 0; ms.addEventListener("sourceopen", function () { window.opened++; }); document.getElementById("v").src = URL.createObjectURL(ms);</script>';
  const yes = await open(html, GUARD_HO, 'yes');
  await settle(yes, 300);
  await nativePlay(yes); await settle(yes, 800);
  assert.equal(await yes.evaluate(() => window.opened), 0, 'yes: no sourceopen, no segment');
  const late = await open(html, GUARD_HO, 'late');
  await settle(late, 300);
  await nativePlay(late); await settle(late, 300);
  assert.equal(await late.evaluate(() => window.opened), 0, 'while the app thinks the source is held');
  await settle(late, 700);
  assert.equal(await late.evaluate(() => window.opened), 1, 'no answer in 700 ms: the page\'s player may go');
  const no = await open(html, GUARD_HO, 'no');
  await settle(no, 300);
  await nativePlay(no); await settle(no, 700);
  assert.equal(await no.evaluate(() => window.opened), 1, 'no: it opens');
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

// ---- ANS55f (owner message 55: the player inside the website): the question says where the film's box is
const PLAYER_AT = (wrap, tail) => '<div style="height:400px">top</div>' + wrap + '<div id="player" style="position:relative;width:320px;height:180px"><video id="v" src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video>'
  + '<button id="btn" style="position:absolute;left:100px;top:60px;width:120px;height:60px">Play</button></div>' + tail + '<div style="height:2000px">more</div>'
  + '<script>document.getElementById("btn").onclick = function () { document.getElementById("v").play().catch(function () {}); };</script>';

test('the question carries the film\'s box and where the page is scrolled: eleven numbers, a plain page follows the root scroll (fx 0)', async () => {
  const page = await open(PLAYER_AT('', ''), GUARD_HO, 'yes');
  await settle(page, 300);
  await page.evaluate(() => window.scrollTo(0, 150));
  await settle(page, 200);
  await tapOn(page, '#btn'); await settle(page, 500);
  const q = await vhand(page);
  assert.equal(q.length, 1);
  const g = q[0].g;
  assert.ok(Array.isArray(g) && g.length === 11, 'g: ' + JSON.stringify(g));
  assert.ok(Math.abs(g[0] - 0) < 1 && Math.abs(g[1] - 250) < 1, 'x, y of the box in the viewport: ' + g[0] + ', ' + g[1] + ' (400 above it, scrolled 150)');
  assert.equal(g[2], 320); assert.equal(g[3], 180);
  assert.equal(g[4], 1, 'visual viewport scale'); assert.equal(g[5], 0); assert.equal(g[6], 0);
  assert.ok(g[7] >= 300, 'visual viewport width in CSS px: ' + g[7]);
  assert.equal(g[8], 0); assert.equal(g[9], 150, 'where the visual viewport sits in the document (the page is scrolled 150)');
  assert.equal(g[10], 0, 'nothing fixed, sticky or scrolling of its own around it');
  assert.equal(q[0].tp, undefined, 'tp is the background page\'s: the content script sends no claim about the frame');
});

test('a film under a fixed or sticky ancestor, or inside a scroller of its own, is told as fx 1 (the box does not move with the page\'s scroll)', async () => {
  const cases = [
    ['fixed', '<div style="position:fixed;left:0;top:100px;width:340px;height:200px">', '</div>'],
    ['sticky', '<div style="position:sticky;top:0;width:340px;height:200px">', '</div>'],
    ['a horizontal scroller (carousel)', '<div style="overflow-x:auto;width:340px;height:200px"><div style="width:900px">', '</div></div>'],
    ['a vertical scroller', '<div style="overflow-y:scroll;width:340px;height:120px"><div style="height:600px">', '</div></div>'],
  ];
  for (const [name, a, b] of cases) {
    const page = await open(PLAYER_AT(a, b), GUARD_HO, 'yes');
    await settle(page, 300);
    await page.evaluate(() => { const v = document.getElementById('v'); v.scrollIntoView(); });
    await tapOn(page, '#btn'); await settle(page, 500);
    const q = await vhand(page);
    assert.ok(q.length >= 1, name + ': asked');
    assert.equal(q[0].g && q[0].g[10], 1, name + ': fx ' + JSON.stringify(q[0].g));
    await page.context().close();
  }
});

test('the content script drops a box that is not eleven numbers in range (the film then plays in the full-screen player)', async () => {
  const page = await open('<div>x</div>', GUARD_HO, 'no');
  await settle(page, 300);
  await page.evaluate(() => {
    const ask = (d) => document.dispatchEvent(new CustomEvent('__kst_gate_ask_v1', { detail: JSON.stringify(d) }));
    ask({ i: 1, y: 'play', s: 'https://x.example/a.mp4', g: [0, 0, 320, 180, 1, 0, 0, 564, 0, 0] });                 // ten numbers
    ask({ i: 2, y: 'play', s: 'https://x.example/a.mp4', g: [0, 0, 320, 180, 1, 0, 0, 564, 0, 0, 2] });                // fx out of range
    ask({ i: 3, y: 'play', s: 'https://x.example/a.mp4', g: [0, 0, 320, 180, 1, 0, 0, 564, 0, 'x', 0] });             // not a number
    ask({ i: 4, y: 'play', s: 'https://x.example/a.mp4', g: [0, 0, 320, 180, 0, 0, 0, 564, 0, 0, 0] });               // scale 0
    ask({ i: 5, y: 'play', s: 'https://x.example/a.mp4', g: [0.123456, -20.5, 320, 180, 1, 0, 0, 564, 0, 12.987, 0] }); // fine (rounded to 2 decimals)
  });
  await settle(page, 500);
  const q = await vhand(page);
  assert.equal(q.length, 5);
  assert.deepEqual(q.slice(0, 4).map((x) => x.g), [null, null, null, null]);
  assert.deepEqual(q[4].g, [0.12, -20.5, 320, 180, 1, 0, 0, 564, 0, 12.99, 0]);
});
