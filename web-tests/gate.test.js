'use strict';
// gate-main.js (ANS53a / ANS53b: no video starts or loads before the person presses it) in real headless Chromium. The script is plain DOM code that behaves the same in Gecko; what only the real engine can show
// (the engine's own autoplay rule, world MAIN registration, the hold of a MediaSource attach in Gecko) is checked on the emulator lab (suite ap). Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const GATE = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'gate-main.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required'] }); });   // (the engine itself would allow it: what is refused below is refused by the gate)
after(async () => { await browser.close(); });

/** A page with the gate installed before any page script; [html] is the body, [cfg] what the content script tells ('1' on, '0' off, undefined = nothing yet). Returns the page and the media requests seen. */
async function open(html, cfg) {
  const context = await browser.newContext({ hasTouch: false });
  const media = [];
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.pathname.endsWith('.mp4') || u.pathname.endsWith('.webm')) { media.push(u.pathname); return r.fulfill({ status: 200, contentType: 'video/mp4', body: Buffer.alloc(2000) }); }
    return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${html}</body></html>` });
  });
  const page = await context.newPage();
  await page.addInitScript(() => {
    window.__reports = []; window.__ready = 0;
    document.addEventListener('__kst_gate_rep_v1', (e) => window.__reports.push(String(e.detail)), true);
    document.addEventListener('__kst_gate_ready_v1', () => window.__ready++, true);
  });
  await page.addInitScript({ content: GATE });
  if (cfg !== undefined) await page.addInitScript((c) => { document.dispatchEvent(new CustomEvent('__kst_gate_cfg_v1', { detail: c })); }, cfg);
  await page.goto('https://site.example/');
  page.media = media;
  return page;
}
const box = async (page, sel) => { const b = await page.locator(sel).boundingBox(); return { x: b.x + b.width / 2, y: b.y + b.height / 2 }; };
const tapOn = async (page, sel) => { const c = await box(page, sel); await page.mouse.click(c.x, c.y); };
const settle = (page, ms) => page.waitForTimeout(ms || 250);
// what play() does, without waiting for a film that never gets a byte in these pages (a pending promise is the engine waiting for data)
const PLAY = "(el) => Promise.race([el.play().then(() => 'ok', (e) => e.name + ': ' + e.message.slice(0, 40)), new Promise((r) => setTimeout(() => r('pending'), 300))])";

// NOTE: Playwright's evaluate() runs with a user activation (CDP userGesture), so anything that must happen WITHOUT one is done by the page's own timer at load.
const LATE_PLAY = '<script>setTimeout(function () { var v = document.getElementById("v"); v.play().then(function () { window.__r = "ok"; }, function (e) { window.__r = e.name + ": " + e.message.slice(0, 40); }); }, 150);</script>';
const VID = '<video id="v" style="display:block;width:320px;height:180px"></video>';
const MSE = `<video id="v" style="display:block;width:320px;height:180px"></video><div id="far" style="width:300px;height:100px;margin-top:40px">elsewhere</div>
  <script>window.ms = new MediaSource(); window.opened = 0; ms.addEventListener('sourceopen', function () { window.opened++; });
  var v = document.getElementById('v'); window.url = URL.createObjectURL(ms); v.src = url;</script>`;

test('a MediaSource attach is held: the player gets no sourceopen, so a script player fetches no segment; a tap on the picture lets it go', async () => {
  const page = await open(MSE);
  await settle(page, 400);
  assert.equal(await page.evaluate(() => window.opened), 0, 'no sourceopen while the picture is untouched');
  assert.equal(await page.evaluate(() => ms.readyState), 'closed');
  assert.equal(await page.evaluate(() => document.getElementById('v').src), await page.evaluate(() => window.url), 'the page reads back what it set');
  await tapOn(page, '#far'); await settle(page);
  assert.equal(await page.evaluate(() => window.opened), 0, 'a tap elsewhere on the page opens nothing');
  await tapOn(page, '#v'); await settle(page, 500);
  assert.equal(await page.evaluate(() => window.opened), 1, 'the tap on the picture applies the held source: sourceopen comes');
  assert.equal(await page.evaluate(() => ms.readyState), 'open');
});

test('play() without a user activation is refused like the engine would, with a user activation it opens the element and goes through', async () => {
  const page = await open(MSE + LATE_PLAY, '1');                          // (rule ON, answered: without an answer a play() waits for it, see the test of the late answer)
  await settle(page, 600);
  assert.match(await page.evaluate(() => window.__r), /^NotAllowedError: The play method is not allowed/);
  assert.equal(await page.evaluate(() => window.opened), 0);
  await tapOn(page, '#far');                                                // a real tap on the page (not on the picture): a transient user activation
  const r = await page.evaluate(`(${PLAY})(document.getElementById('v'))`);
  assert.ok(!/^NotAllowedError: The play method is not allowed by the user agent or the platform in the current context, possibly because the user denied permission/.test(r), 'inside the activation the call is the page\'s own: ' + r);
  await settle(page, 400);
  assert.equal(await page.evaluate(() => window.opened), 1, 'and the held source was applied by it');
});

test('preload is none whatever the page says, the page reads its own value back, and it is given back when the element opens', async () => {
  const page = await open('<video id="v" preload="metadata" style="display:block;width:320px;height:180px" src="https://cdn.example/a.mp4"></video><video id="w" style="display:block;width:320px;height:180px"></video><script>var w=document.getElementById("w"); w.preload="auto"; w.setAttribute("src","https://cdn.example/b.mp4");</script>');
  await settle(page, 400);
  const st = await page.evaluate(() => { const v = document.getElementById('v'), w = document.getElementById('w'); return { va: v.getAttribute('preload'), vp: v.preload, wa: w.getAttribute('preload'), wp: w.preload }; });
  assert.deepEqual(st, { va: 'none', vp: 'metadata', wa: 'none', wp: 'auto' });
  assert.deepEqual(page.media, [], 'no byte of any file asked for before a press');
  await tapOn(page, '#v'); await settle(page);
  assert.equal(await page.evaluate(() => document.getElementById('v').getAttribute('preload')), 'metadata', 'given back when it opens');
});

test('autoplay in the markup and set by a script is not applied while the element is closed, and nothing starts', async () => {
  const page = await open('<video id="v" autoplay muted loop src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video><video id="w" style="display:block;width:320px;height:180px"></video><script>var w=document.getElementById("w"); w.autoplay=true; w.setAttribute("autoplay",""); w.muted=true; w.src="https://cdn.example/b.mp4";</script>');
  await settle(page, 600);
  const st = await page.evaluate(() => { const v = document.getElementById('v'), w = document.getElementById('w'); return { vAttr: v.hasAttribute('autoplay'), vPaused: v.paused, wAttr: w.hasAttribute('autoplay'), wProp: w.autoplay, wPaused: w.paused }; });
  assert.deepEqual(st, { vAttr: false, vPaused: true, wAttr: false, wProp: true, wPaused: true }, 'the page thinks its autoplay is set; the engine never saw it');
});

test('load() is held and a new element is made safe at once; a tap on the picture lets the page\'s own source load', async () => {
  const page = await open(VID + '<script>var v = document.getElementById("v"); v.src = "https://cdn.example/a.mp4"; v.load();</script>');
  await settle(page, 500);
  assert.deepEqual(page.media, [], 'preload none (forced): the file is not asked for, not even its first bytes');
  assert.equal(await page.evaluate(() => document.getElementById('v').getAttribute('preload')), 'none');
  await tapOn(page, '#v'); await settle(page, 600);
  assert.deepEqual(page.media, ['/a.mp4'], 'after the press the page\'s own load runs');
  assert.equal(await page.evaluate(() => document.getElementById('v').hasAttribute('preload')), false, 'and the engine\'s own default is back: the page never set one');
});

test('Kestrel\'s own controls open an element with __kst_gate_v1 on it, also one inside a closed shadow tree', async () => {
  const page = await open('<div id="host"></div><script>var root = document.getElementById("host").attachShadow({ mode: "closed" }); root.innerHTML = \'<video id="v" preload="auto" style="display:block;width:320px;height:180px"></video>\'; window.__v = root.getElementById("v"); window.ms = new MediaSource(); window.opened = 0; ms.addEventListener("sourceopen", function () { window.opened++; }); window.__v.src = URL.createObjectURL(ms);</script>');
  await settle(page, 300);
  assert.equal(await page.evaluate(() => window.__v.getAttribute('preload')), 'none', 'the shadow tree is watched too');
  assert.equal(await page.evaluate(() => window.opened), 0);
  await page.evaluate(() => window.__v.dispatchEvent(new CustomEvent('__kst_gate_v1')));
  await settle(page, 400);
  assert.equal(await page.evaluate(() => window.opened), 1);
  assert.equal(await page.evaluate(() => window.__v.getAttribute('preload')), 'auto', 'and it has the page\'s own preload back');
});

test('a tap on the picture of a film inside a closed shadow tree opens it (the picture is found by its place)', async () => {
  const page = await open('<div id="host" style="width:320px;height:180px"></div><script>var root = document.getElementById("host").attachShadow({ mode: "closed" }); root.innerHTML = \'<video style="display:block;width:320px;height:180px"></video>\'; window.__v = root.querySelector("video"); window.ms = new MediaSource(); window.opened = 0; ms.addEventListener("sourceopen", function () { window.opened++; }); window.__v.src = URL.createObjectURL(ms);</script>');
  await settle(page, 300);
  assert.equal(await page.evaluate(() => window.opened), 0);
  await tapOn(page, '#host'); await settle(page, 400);
  assert.equal(await page.evaluate(() => window.opened), 1);
});

test('a stream (camera, call) is never held and its play() is not refused by the gate', async () => {
  const page = await open(VID);
  const r = await page.evaluate(() => {
    const c = document.createElement('canvas'); c.width = 64; c.height = 36; c.getContext('2d').fillRect(0, 0, 64, 36);
    const v = document.getElementById('v'); v.muted = true; v.srcObject = c.captureStream(5);
    return Promise.race([v.play().then(() => 'ok', (e) => e.message), new Promise((res) => setTimeout(() => res('pending'), 400))]);
  });
  assert.ok(!/^The play method is not allowed by the user agent or the platform in the current context, possibly because the user denied permission\./.test(r), r);
});

test('the setting off gives the page\'s autoplay back (markup and script); a tap on a hero loop with no play button does not start it', async () => {
  const HTML = '<video id="v" autoplay muted loop src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video><video id="w" style="display:block;width:320px;height:180px"></video><script>var w=document.getElementById("w"); w.autoplay=true; w.muted=true; w.src="https://cdn.example/b.mp4";</script>';
  const state = (page) => page.evaluate(() => { const v = document.getElementById('v'), w = document.getElementById('w'); return { vAttr: v.hasAttribute('autoplay'), wAttr: w.hasAttribute('autoplay'), wProp: w.autoplay }; });
  const hero = await open(HTML);
  await settle(hero, 500);
  assert.deepEqual(await state(hero), { vAttr: false, wAttr: false, wProp: true }, 'closed: the engine never sees an autoplay');
  await tapOn(hero, '#v'); await settle(hero, 400);
  assert.equal((await state(hero)).vAttr, false, 'a tap on a loop with no play button does not hand its autoplay back (the person did not press a play button)');
  const off = await open(HTML);
  await settle(off, 500);
  await off.evaluate(() => document.dispatchEvent(new CustomEvent('__kst_gate_cfg_v1', { detail: '0' })));
  await settle(off, 500);
  assert.deepEqual(await state(off), { vAttr: true, wAttr: true, wProp: true }, 'rule off: the page\'s own autoplay is back, for the markup and for the script');
});

test('the setting off lets everything go at once, also what was held before, and elements that come later are not touched', async () => {
  const page = await open(MSE);
  await settle(page, 300);
  assert.equal(await page.evaluate(() => window.opened), 0);
  await page.evaluate(() => document.dispatchEvent(new CustomEvent('__kst_gate_cfg_v1', { detail: '0' })));
  await settle(page, 500);
  assert.equal(await page.evaluate(() => window.opened), 1, 'what was held is applied');
  const later = await page.evaluate(() => { const v = document.createElement('video'); v.preload = 'auto'; v.setAttribute('autoplay', ''); document.body.appendChild(v); return [v.preload, v.hasAttribute('autoplay')]; });
  assert.deepEqual(later, ['auto', true], 'a later element is the page\'s own');
  await page.evaluate(() => document.dispatchEvent(new CustomEvent('__kst_gate_cfg_v1', { detail: '1' })));
  const again = await page.evaluate(() => { const v = document.createElement('video'); v.preload = 'auto'; return v.getAttribute('preload'); });
  assert.equal(again, 'none', 'and on again closes new ones');
});

test('what was held and opened is counted and reported once as numbers', async () => {
  const page = await open(MSE + LATE_PLAY, '1');
  await settle(page, 600);
  await tapOn(page, '#v');
  await page.waitForFunction(() => window.__reports.length > 0, null, { timeout: 5000 });
  const rep = await page.evaluate(() => window.__reports);
  assert.equal(rep.length, 1);
  const [held, opened, play, press, kestrel, deferred, refused] = rep[0].split('|').map(Number);
  assert.deepEqual([opened, play, press, kestrel, deferred, refused], [1, 0, 1, 0, 1, 1]);
  assert.ok(held >= 0 && /^[0-9|]+$/.test(rep[0]), rep[0]);
});

test('the replacements look like the engine\'s own and the script announces itself', async () => {
  const page = await open(VID);
  assert.equal(await page.evaluate(() => window.__ready), 1);
  const r = await page.evaluate(() => ({
    play: String(HTMLMediaElement.prototype.play), name: HTMLMediaElement.prototype.play.name, len: HTMLMediaElement.prototype.load.length,
    sd: Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src'), marker: Object.keys(window).indexOf('__kstGate'),
  }));
  assert.match(r.play, /\[native code\]/); assert.equal(r.name, 'play'); assert.equal(r.len, 0); assert.equal(r.marker, -1);
});

test('a page without any media is not touched, and elements that are not media are not either', async () => {
  const page = await open('<div id="d">x</div><a id="a" href="#" onclick="return false">y</a>');
  const r = await page.evaluate(() => { const d = document.getElementById('d'); d.setAttribute('src', 'blob:abc'); d.setAttribute('preload', 'auto'); return [d.getAttribute('src'), d.getAttribute('preload')]; });
  assert.deepEqual(r, ['blob:abc', 'auto']);
});

test('the script does nothing where the extension API exists (an accidental copy in the isolated world)', async () => {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>x</title>' }));
  const page = await context.newPage();
  await page.addInitScript(() => { window.browser = { runtime: {} }; window.__play = HTMLMediaElement.prototype.play; });
  await page.addInitScript({ content: GATE });
  await page.goto('https://site.example/');
  assert.equal(await page.evaluate(() => HTMLMediaElement.prototype.play === window.__play), true);
});

test('a hundred media elements in one go cost little: the scan is bounded', async () => {
  const page = await open('<div id="root"></div>');
  const ms = await page.evaluate(() => { const t0 = performance.now(); const root = document.getElementById('root'); let h = ''; for (let i = 0; i < 300; i++) h += '<video preload="auto" autoplay></video>'; root.innerHTML = h; return performance.now() - t0; });
  await settle(page, 300);
  assert.ok(ms < 800, 'took ' + ms + ' ms');
  const n = await page.evaluate(() => Array.from(document.querySelectorAll('video')).filter((v) => v.getAttribute('preload') === 'none' && !v.hasAttribute('autoplay')).length);
  assert.equal(n, 300, 'all of them are made safe (review: the old cap of sixty left the rest autoplaying): ' + n);
});

// ---------------------------------------------------------------- the independent review of the gate (research/ANS53_GATE_REVIEW.md)
test('review 1: a blob: of a FILE (an upload form\'s preview or duration probe, also on a detached element) is not held; only a MediaSource\'s is', async () => {
  const page = await open('<video id="v" style="display:block;width:320px;height:180px"></video><script>window.ev = []; window.dev = [];'
    + 'window.fileUrl = URL.createObjectURL(new Blob([new Uint8Array(3000)], { type: "video/mp4" }));'
    + 'var v = document.getElementById("v"); ["loadstart", "error"].forEach(function (n) { v.addEventListener(n, function () { window.ev.push(n); }); }); v.src = window.fileUrl;'
    + 'var d = document.createElement("video"); ["loadstart", "error"].forEach(function (n) { d.addEventListener(n, function () { window.dev.push(n); }); }); d.src = window.fileUrl;</script>', '1');
  await settle(page, 800);
  const ev = await page.evaluate(() => ({ attached: window.ev, detached: window.dev }));
  assert.ok(ev.attached.includes('loadstart'), 'the file address reached the engine (attached): ' + JSON.stringify(ev));
  assert.ok(ev.detached.includes('loadstart'), 'and on a detached element: ' + JSON.stringify(ev));
});

test('review 2: a video that is given a stream (a camera, a call) shows it by its own autoplay, also after an earlier press', async () => {
  for (const pressFirst of [false, true]) {
    const page = await open('<video id="v" autoplay muted playsinline style="display:block;width:320px;height:180px"></video><canvas id="c" width="64" height="64"></canvas>', '1');
    await settle(page, 300);
    assert.equal(await page.evaluate(() => document.getElementById('v').hasAttribute('autoplay')), false, 'closed: the autoplay is off the element');
    if (pressFirst) { await tapOn(page, '#v'); await settle(page, 200); }
    await page.evaluate(() => { const c = document.getElementById('c'); c.getContext('2d').fillRect(0, 0, 10, 10); document.getElementById('v').srcObject = c.captureStream(10); });
    await settle(page, 900);
    const st = await page.evaluate(() => { const v = document.getElementById('v'); return { attr: v.hasAttribute('autoplay'), paused: v.paused }; });
    assert.deepEqual(st, { attr: true, paused: false }, 'pressFirst=' + pressFirst);
  }
});

test('review 11: audio is not touched (a music player\'s next track, a sound): attributes stay, play() is not refused', async () => {
  const page = await open('<audio id="a" autoplay preload="auto" src="https://cdn.example/a.mp3"></audio><script>setTimeout(function () { var x = new Audio("https://cdn.example/b.mp3"); window.__r = "waiting"; x.play().then(function () { window.__r = "ok"; }, function (e) { window.__r = e.name; }); }, 150);</script>', '1');
  await settle(page, 700);
  const st = await page.evaluate(() => { const a = document.getElementById('a'); return { autoplay: a.hasAttribute('autoplay'), preload: a.getAttribute('preload'), r: window.__r }; });
  assert.equal(st.autoplay, true); assert.equal(st.preload, 'auto');
  assert.notEqual(st.r, 'NotAllowedError', 'the gate refused an audio play(): ' + st.r);
});

test('review 7: a tap opens the video it landed on or in (its player box), not one that a cookie button merely sits over', async () => {
  const page = await open('<div id="player" style="position:relative;width:320px;height:180px"><video id="v" style="display:block;width:320px;height:180px"></video><button id="playbtn" style="position:absolute;left:100px;top:90px;width:120px;height:60px">Play</button></div>'
    + '<div id="cookie" style="position:fixed;left:0;top:0;width:320px;height:50px;background:#eee;z-index:9"><button id="accept" style="width:200px;height:40px">Accept cookies</button></div>'
    + '<script>window.ms = new MediaSource(); window.opened = 0; ms.addEventListener("sourceopen", function () { window.opened++; }); document.getElementById("v").src = URL.createObjectURL(ms);</script>', '1');
  await settle(page, 300);
  await tapOn(page, '#accept'); await settle(page, 400);
  assert.equal(await page.evaluate(() => window.opened), 0, 'the cookie button sits over the picture and opened nothing');
  await tapOn(page, '#playbtn'); await settle(page, 500);
  assert.equal(await page.evaluate(() => window.opened), 1, 'the player\'s own button opened it');
});

test('review 3: a play() 6 s after a press on the player\'s own button is the person\'s (slow hotspot); 6 s after a tap far away it is not', async () => {
  const mk = (which) => '<div id="player" style="position:relative;width:320px;height:180px"><video id="v" src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video><button id="btn" style="position:absolute;left:100px;top:60px;width:120px;height:60px">Play</button></div>'
    + '<div id="far" style="width:300px;height:80px;margin-top:40px"><button id="other" style="width:200px;height:50px">Other</button></div>'
    + '<script>function later() { setTimeout(function () { window.__r = "waiting"; document.getElementById("v").play().then(function () { window.__r = "ok"; }, function (e) { window.__r = e.name; }); }, 6000); }'
    + 'document.getElementById("' + which + '").onclick = later;</script>';
  const own = await open(mk('btn'), '1');
  await tapOn(own, '#btn');
  const far = await open(mk('other'), '1');
  await tapOn(far, '#other');
  await far.waitForTimeout(6800);
  assert.equal(await far.evaluate(() => window.__r), 'NotAllowedError', 'a tap far from the player does not let a later play() through');
  assert.notEqual(await own.evaluate(() => window.__r), 'NotAllowedError', 'the player\'s own button did: ' + await own.evaluate(() => window.__r));
});

test('review 3b: a click that goes full screen and plays in the same breath is the person\'s press (the full screen call uses the activation up)', async () => {
  const page = await open('<div id="player" style="position:relative;width:320px;height:180px"><video id="v" src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video><button id="btn" style="position:absolute;left:100px;top:60px;width:120px;height:60px">Play</button></div>'
    + '<script>document.getElementById("btn").onclick = function () { try { document.getElementById("player").requestFullscreen(); } catch (e) {} window.__r = "waiting"; document.getElementById("v").play().then(function () { window.__r = "ok"; }, function (e) { window.__r = e.name; }); };</script>', '1');
  await tapOn(page, '#btn');
  await settle(page, 900);
  assert.notEqual(await page.evaluate(() => window.__r), 'NotAllowedError', 'refused after requestFullscreen(): ' + await page.evaluate(() => window.__r));
});

test('review 5a: a play() that comes before the app has answered waits for it: carried out when the rule turns out OFF, refused when it is ON', async () => {
  const SLOW = '<script>setTimeout(function () { window.__p = "waiting"; document.getElementById("v").play().then(function () { window.__p = "ok"; }, function (e) { window.__p = e.name; }); }, 100);</script>';
  const V = '<video id="v" src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video>';
  const off = await open(V + SLOW);                                          // no answer at load
  await settle(off, 500);
  assert.equal(await off.evaluate(() => window.__p), 'waiting', 'nothing is decided before the answer');
  await off.evaluate(() => document.dispatchEvent(new CustomEvent('__kst_gate_cfg_v1', { detail: '0' })));
  await settle(off, 500);
  assert.notEqual(await off.evaluate(() => window.__p), 'NotAllowedError', 'rule OFF: the call went through to the engine: ' + await off.evaluate(() => window.__p));
  const on = await open(V + SLOW);
  await settle(on, 500);
  await on.evaluate(() => document.dispatchEvent(new CustomEvent('__kst_gate_cfg_v1', { detail: '1' })));
  await settle(on, 300);
  assert.equal(await on.evaluate(() => window.__p), 'NotAllowedError', 'rule ON: refused when the answer comes');
});

test('review 5a: an answer that never comes ends the wait after 4 s with a refusal (the person\'s rule is the default)', async () => {
  const page = await open('<video id="v" src="https://cdn.example/a.mp4" style="display:block;width:320px;height:180px"></video><script>setTimeout(function () { window.__p = "waiting"; document.getElementById("v").play().then(function () { window.__p = "ok"; }, function (e) { window.__p = e.name; }); }, 100);</script>');
  await page.waitForTimeout(4600);
  assert.equal(await page.evaluate(() => window.__p), 'NotAllowedError');
});
