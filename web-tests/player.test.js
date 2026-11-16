'use strict';
// VP1 (owner message 49, research/VIDEO_PLAYER_AUDIT.md): player.js in real headless Chromium against the fake extension API of videobar-lib.js. The app is played by the test: its answers come from
// window.top.__vpQueue (one-shot: commands are delivered once) and window.top.__vpReply (the standing answer: whether the controls are shown). Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { launch, close, open, msgs, touch } = require('./videobar-lib');

before(launch);
after(close);

const wait = (page, ms) => page.waitForTimeout(ms);
// a player box that goes full screen: the film fills it, a page button ("Skip ad"), a button named like a player control ("Mute") and a link; the page counts the clicks it gets
const BOX = `<div id="box" style="position:fixed;left:0;top:0;width:300px;height:200px;background:#000">
  <video id="v" src="https://cdn.example/film.wav" preload="auto" muted style="width:100%;height:100%"></video>
  <button id="skip" style="position:absolute;right:8px;top:8px;width:90px;height:40px">Skip ad</button>
  <button id="mutebtn" aria-label="Mute" style="position:absolute;left:8px;bottom:8px;width:60px;height:40px">M</button>
  <a id="lnk" href="https://ads.example/" style="position:absolute;left:100px;top:8px;width:90px;height:40px;display:block">More</a></div>
  <script>window.__clicks = []; document.addEventListener('click', function (e) { window.__clicks.push(e.target.id || e.target.tagName); if (e.target.id === 'lnk') e.preventDefault(); }, false);</script>`;
const SHADOW = `<div id="host" style="position:fixed;left:0;top:0;width:300px;height:200px"></div>
  <script>var sr = document.getElementById('host').attachShadow({ mode: 'open' }); sr.innerHTML = '<video id="v" src="https://cdn.example/film.wav" preload="auto" muted style="width:100%;height:100%"></video>'; window.__deepV = function () { return sr.getElementById('v'); };</script>`;

const fullscreen = (page, id) => page.evaluate((i) => document.getElementById(i || 'box').requestFullscreen(), id);
const states = (page) => msgs(page, 'vst');
const taps = (page) => msgs(page, 'vtap');
const clicks = (page) => page.evaluate(() => window.__clicks.slice());
const standing = (page, r) => page.evaluate((x) => { window.top.__vpReply = x; }, r);
const queue = (page, r) => page.evaluate((x) => { (window.top.__vpQueue = window.top.__vpQueue || []).push(x); }, r);
const firstState = async (page) => { await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vst'), null, { timeout: 8000 }); return (await states(page))[0]; };
const film = (page) => page.evaluate(() => { const v = document.getElementById('v') || window.__deepV(); return { t: v.currentTime, paused: v.paused, muted: v.muted, rate: v.playbackRate, fit: v.style.getPropertyValue('object-fit'), prio: v.style.getPropertyPriority('object-fit') }; });

test('VP1: a full-screen film tells the app its state (numbers only); nothing before full screen', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await wait(page, 500);
  assert.deepEqual(await states(page), [], 'nothing outside full screen');
  await fullscreen(page);
  const m = await firstState(page);
  assert.equal(m.k, 'vst'); assert.equal(m.d, 60); assert.equal(m.p, 1); assert.equal(m.e, 0); assert.equal(m.ms, 0); assert.equal(m.l, 0); assert.ok(m.rs >= 3, 'ready state ' + m.rs);
  assert.deepEqual(Object.keys(m).sort(), ['ak', 'aw', 'b', 'd', 'e', 'h', 'k', 'l', 'm', 'ms', 'p', 'r', 'rs', 't', 'v', 'w'], 'only numbers: ' + JSON.stringify(m));
  await page.context().close();
});

test('VP1: the app says controls are not Kestrel\'s (guard kp false): nothing is reported and no tap is taken', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: false } });
  await fullscreen(page);
  await wait(page, 900);
  assert.deepEqual(await states(page), []);
  await page.touchscreen.tap(250, 400);
  await wait(page, 150);
  assert.ok((await clicks(page)).includes('v'), 'the page gets the tap: ' + JSON.stringify(await clicks(page)));
  await page.context().close();
});

test('VP1: the commands of the app are done once: play, pause, seek, skip, mute, speed', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await fullscreen(page); await firstState(page);
  await queue(page, { s: 1, c: [{ a: 'play' }] });
  await page.waitForFunction(() => !document.getElementById('v').paused, null, { timeout: 5000 });
  await queue(page, { s: 1, c: [{ a: 'seek', t: 30 }] });
  await page.waitForFunction(() => document.getElementById('v').currentTime >= 29.9, null, { timeout: 5000 });
  const t0 = (await film(page)).t;
  await queue(page, { s: 1, c: [{ a: 'skip', d: 10 }] });
  await page.waitForFunction((t) => document.getElementById('v').currentTime >= t + 9.5, t0, { timeout: 5000 });
  await queue(page, { s: 1, c: [{ a: 'skip', d: -1000 }] });
  await page.waitForFunction(() => document.getElementById('v').currentTime < 3, null, { timeout: 5000 });
  await queue(page, { s: 1, c: [{ a: 'pause' }, { a: 'mute', on: false }, { a: 'rate', r: 1.5 }] });
  await page.waitForFunction(() => { const v = document.getElementById('v'); return v.paused && !v.muted && v.playbackRate === 1.5; }, null, { timeout: 5000 });
  await queue(page, { s: 1, c: [{ a: 'skip', d: 1000 }] });
  await page.waitForFunction(() => document.getElementById('v').currentTime >= 59, null, { timeout: 5000 });
  assert.ok((await film(page)).t <= 60.01, 'a skip never goes past the end');
  await page.context().close();
});

test('VP1: a report says which command of the app the page had carried out last (ak), so the app can tell a stale report from one taken after the film did what was asked', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await fullscreen(page); await firstState(page);
  assert.equal((await states(page))[0].ak, 0, 'nothing carried out yet');
  await queue(page, { s: 1, c: [{ a: 'play', i: 4 }] });
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vst' && m.ak === 4), null, { timeout: 5000 });
  await queue(page, { s: 1, c: [{ a: 'pause', i: 9 }, { a: 'mute', on: false, i: 10 }] });
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vst' && m.ak === 10), null, { timeout: 5000 });
  const m = (await states(page)).filter((x) => x.ak === 10)[0];
  assert.equal(m.p, 1, 'the report that names command 10 shows the film paused'); assert.equal(m.m, 0);
  await page.context().close();
});

test('VP2: a tap on the film is told to the app (place across the picture) and NOT passed to the page; the page\'s own buttons still work', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await fullscreen(page); await firstState(page);
  await page.touchscreen.tap(250, 400);                                    // the film
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vtap'), null, { timeout: 5000 });
  let t = await taps(page);
  assert.equal(t.length, 1); assert.ok(Math.abs(t[0].x - 500) <= 10, 'the middle: ' + t[0].x);
  assert.deepEqual(await clicks(page), [], 'the page did not see it: ' + JSON.stringify(await clicks(page)));
  await page.touchscreen.tap(447, 28);                                      // "Skip ad": the page's own button
  await wait(page, 200);
  assert.deepEqual(await clicks(page), ['skip']); assert.equal((await taps(page)).length, 1, 'not a tap on the film');
  await page.touchscreen.tap(145, 28);                                      // a link
  await wait(page, 200);
  assert.deepEqual(await clicks(page), ['skip', 'lnk']); assert.equal((await taps(page)).length, 1);
  await page.touchscreen.tap(38, 772);                                      // a button named Mute: a control Kestrel's replace, so it is a tap on the film
  await page.waitForFunction(() => window.__msgs.filter((m) => m && m.k === 'vtap').length === 2, null, { timeout: 5000 });
  assert.deepEqual(await clicks(page), ['skip', 'lnk'], 'the page\'s Mute button did not get it');
  await page.context().close();
});

test('VP2: the place of a tap says which side it is on', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await fullscreen(page); await firstState(page);
  await page.touchscreen.tap(50, 400); await wait(page, 120);
  await page.touchscreen.tap(450, 400); await wait(page, 120);
  const t = await taps(page);
  assert.equal(t.length, 2); assert.ok(t[0].x < 150, 'left: ' + t[0].x); assert.ok(t[1].x > 850, 'right: ' + t[1].x);
  await page.context().close();
});

test('VP2: a slide or a long press is not a tap', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await fullscreen(page); await firstState(page);
  await touch(page, 250, 400, { toX: 330, toY: 400 });                      // a swipe
  await touch(page, 250, 400, { holdMs: 700 });                              // a long press
  await wait(page, 300);
  assert.deepEqual(await taps(page), []);
  await page.context().close();
});

test('VP1: it reports faster while the controls are shown than while they are hidden, and the app can switch it off', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await page.evaluate(() => { window.KestrelVP.c.idle = 400; window.KestrelVP.c.busy = 100; });
  await fullscreen(page); await firstState(page);
  await standing(page, { s: 0 });
  const a0 = (await states(page)).length; await wait(page, 1200); const hidden = (await states(page)).length - a0;
  await standing(page, { s: 1 });
  await wait(page, 400);
  const b0 = (await states(page)).length; await wait(page, 1200); const shown = (await states(page)).length - b0;
  assert.ok(shown >= hidden * 2, 'shown ' + shown + ' reports in 1.2 s, hidden ' + hidden);
  assert.ok(shown <= 20 && hidden >= 1, 'bounded: ' + shown + ' / ' + hidden);
  await standing(page, { on: 0 });                                          // the controls are not Kestrel's here after all
  await wait(page, 500);
  const n = (await states(page)).length; await wait(page, 800);
  assert.equal((await states(page)).length, n, 'no more reports');
  await page.touchscreen.tap(250, 400); await wait(page, 150);
  assert.ok((await clicks(page)).includes('v'), 'and the page gets the taps again');
  await page.context().close();
});

test('VP1: leaving full screen ends everything: no more reports, the page gets the taps', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await fullscreen(page); await firstState(page);
  await page.evaluate(() => document.exitFullscreen());
  await wait(page, 600);
  const n = (await states(page)).length; await wait(page, 900);
  assert.equal((await states(page)).length, n, 'no more reports');
  assert.equal((await page.evaluate(() => window.KestrelVP.active())), false);
  await page.touchscreen.tap(100, 100); await wait(page, 150);
  assert.ok((await clicks(page)).includes('v'));
  await page.context().close();
});

test('VP1: a film inside a web component\'s shadow tree is found and controlled', async () => {
  const page = await open(SHADOW, { player: true, cfg: { kp: true } });
  await fullscreen(page, 'host');
  const m = await firstState(page);
  assert.equal(m.d, 60);
  await queue(page, { s: 1, c: [{ a: 'seek', t: 20 }] });
  await page.waitForFunction(() => window.__deepV().currentTime >= 19.9, null, { timeout: 5000 });
  await page.context().close();
});

test('VP6: a film whose box is not its shape is shown whole while it is full screen, and put back afterwards', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await page.evaluate(() => {                                              // a 16:9 picture in a portrait box with the page's object-fit: fill (the owner's stretched picture)
    const v = document.getElementById('v'); v.style.objectFit = 'fill';
    Object.defineProperty(v, 'videoWidth', { get: () => 640 }); Object.defineProperty(v, 'videoHeight', { get: () => 360 });
  });
  await fullscreen(page); await firstState(page);
  await wait(page, 300);
  let f = await film(page);
  assert.equal(f.fit, 'contain'); assert.equal(f.prio, 'important');
  await page.evaluate(() => document.exitFullscreen());
  await wait(page, 500);
  f = await film(page);
  assert.equal(f.fit, 'fill', 'the page\'s own value is back');
  await page.context().close();
});

test('VP6: a film that already has the picture\'s shape, or a page that chose contain, is left alone', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await page.evaluate(() => {
    const v = document.getElementById('v'); v.style.objectFit = 'contain';
    Object.defineProperty(v, 'videoWidth', { get: () => 640 }); Object.defineProperty(v, 'videoHeight', { get: () => 360 });
  });
  await fullscreen(page); await firstState(page);
  await wait(page, 300);
  assert.equal((await film(page)).prio, '', 'nothing forced');
  await page.context().close();
});

test('VP2: while Kestrel\'s full-screen controls own the film the injected video bar draws nothing', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true, vba: true } });
  await fullscreen(page); await firstState(page);
  await page.touchscreen.tap(250, 400);
  await wait(page, 900);
  assert.deepEqual((await msgs(page, 'vbk')).filter((m) => m.e === 'shown'), []);
  await page.context().close();
});

// the page's own controls (YouTube's chrome, Shaka's UI, video.js ...) and the engine's bar of a `<video controls>` are put away while Kestrel's controls own the film, and given back
const CHROME = `<div id="box" style="position:fixed;left:0;top:0;width:300px;height:200px;background:#000">
  <video id="v" src="https://cdn.example/film.wav" preload="auto" muted controls style="width:100%;height:100%"></video>
  <div id="yt" class="ytp-chrome-bottom" style="position:absolute;left:0;bottom:0;width:300px;height:30px;display:block">bar</div>
  <div id="shaka" class="shaka-controls-container" style="position:absolute;left:0;top:0;width:100px;height:30px;display:flex">ui</div>
  <button id="cc" class="captions-button" style="position:absolute;right:0;top:60px;display:block">CC</button></div>
  <div id="outside" class="ytp-chrome-bottom" style="position:fixed;left:0;top:400px;width:50px;height:20px;display:block">another player</div>`;
const disp = (page, id) => page.evaluate((i) => getComputedStyle(document.getElementById(i)).display, id);

test('VP2: the page\'s own player controls are not shown while Kestrel\'s are (also ones made later), the rest of the page is left alone, and all is given back', async () => {
  const page = await open(CHROME, { player: true, cfg: { kp: true } });
  await page.evaluate(() => { window.KestrelVP.c.idle = 300; window.KestrelVP.c.busy = 300; });
  assert.equal(await disp(page, 'yt'), 'block'); assert.equal(await page.evaluate(() => document.getElementById('v').controls), true);
  await fullscreen(page); await firstState(page);
  await page.waitForFunction(() => window.KestrelVP.away() >= 2, null, { timeout: 5000 });
  assert.equal(await disp(page, 'yt'), 'none'); assert.equal(await disp(page, 'shaka'), 'none');
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vst' && m.aw >= 2), null, { timeout: 4000 });   // the reports say how many of the page's own controls are put away
  await page.evaluate(() => { document.getElementById('yt').style.display = 'block'; });                                  // a page that sets the display again gets it taken away again
  await page.waitForFunction(() => getComputedStyle(document.getElementById('yt')).display === 'none', null, { timeout: 4000 });
  assert.equal(await disp(page, 'cc'), 'block', 'a captions button is the page\'s');
  assert.equal(await disp(page, 'outside'), 'block', 'a player that is not in the full-screen box is not touched');
  assert.equal(await page.evaluate(() => document.getElementById('v').controls), false, 'the engine\'s own bar is off');
  await page.evaluate(() => document.getElementById('box').insertAdjacentHTML('beforeend', '<div id="late" class="vjs-control-bar" style="display:block">x</div>'));
  await page.waitForFunction(() => getComputedStyle(document.getElementById('late')).display === 'none', null, { timeout: 4000 });
  await page.evaluate(() => document.exitFullscreen());
  await page.waitForFunction(() => window.KestrelVP.active() === false, null, { timeout: 4000 });
  assert.equal(await disp(page, 'yt'), 'block'); assert.equal(await disp(page, 'shaka'), 'flex'); assert.equal(await disp(page, 'late'), 'block', 'given back');
  assert.equal(await page.evaluate(() => document.getElementById('v').controls), true, 'the engine\'s bar is back');
  await page.context().close();
});

test('VP2: with the app saying the controls are not Kestrel\'s the page keeps all of its own', async () => {
  const page = await open(CHROME, { player: true, cfg: { kp: false } });
  await fullscreen(page);
  await wait(page, 1200);
  assert.equal(await disp(page, 'yt'), 'block'); assert.equal(await page.evaluate(() => document.getElementById('v').controls), true);
  await page.context().close();
});

// ---- the findings of the independent review of alpha55 (F1, F2, F5, F6, F10)
test('VP1: the app is asked again at every full screen, so a Settings change reaches a page that is already open (review F1)', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await fullscreen(page); await firstState(page);
  await page.evaluate(() => document.exitFullscreen());
  await page.waitForFunction(() => window.KestrelVP.active() === false, null, { timeout: 4000 });
  await page.evaluate(() => { window.top.__kpOverride = false; });                 // the person chose "the page's own"
  const n = (await states(page)).length;
  await fullscreen(page); await wait(page, 1500);
  assert.equal((await states(page)).length, n, 'no session, no reports');
  assert.equal(await page.evaluate(() => window.KestrelVP.active()), false);
  await page.touchscreen.tap(250, 400); await wait(page, 150);
  assert.ok((await clicks(page)).includes('v'), 'the page gets the tap');
  await page.evaluate(() => document.exitFullscreen()); await wait(page, 400);
  await page.evaluate(() => { window.top.__kpOverride = true; });                  // and back on, without a reload
  await fullscreen(page);
  await page.waitForFunction((k) => window.__msgs.filter((m) => m && m.k === 'vst').length > k, n, { timeout: 5000 });
  await page.context().close();
});

test('VP2: a tap makes the reports come at the quick pace for a moment, so what the tap produced is carried out without waiting for the slow pace (review F2)', async () => {
  const page = await open(BOX, { player: true, cfg: { kp: true } });
  await page.evaluate(() => { window.KestrelVP.c.idle = 3000; window.KestrelVP.c.busy = 150; });
  await fullscreen(page); await firstState(page);
  await standing(page, { s: 0 });
  await wait(page, 500);
  const n0 = (await states(page)).length;
  await page.touchscreen.tap(250, 400);
  await page.waitForFunction((k) => window.__msgs.filter((m) => m && m.k === 'vst').length >= k + 2, n0, { timeout: 1500 });   // two more reports within 1.5 s although the slow pace is 3 s
  await page.context().close();
});

test('VP1: the film can change under the controls (an ad element, then the film in another element): the session follows it (review F5)', async () => {
  const TWO = BOX.replace('<button id="skip"', '<video id="w" src="https://cdn.example/film.wav" preload="auto" muted style="position:absolute;left:0;top:0;width:100%;height:100%"></video><button id="skip"');
  const page = await open(TWO, { player: true, cfg: { kp: true } });
  await page.evaluate(() => { const v = document.getElementById('v'); v.play().catch(() => {}); });
  await fullscreen(page); await firstState(page);
  await page.evaluate(() => { const v = document.getElementById('v'), w = document.getElementById('w'); v.pause(); v.style.display = 'none'; w.currentTime = 30; w.play().catch(() => {}); });
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vst' && m.t >= 30 && m.p === 0), null, { timeout: 6000 });   // the reports are the second element's
  await queue(page, { s: 1, c: [{ a: 'pause', i: 1 }] });
  await page.waitForFunction(() => document.getElementById('w').paused, null, { timeout: 4000 });                                   // and the commands go to it
  await page.context().close();
});

test('VP2: a tap on something the page made clickable with a script (onclick, a pointer cursor) goes to the page, a layer over the whole picture does not (review F6)', async () => {
  const JS = BOX.replace('<a id="lnk"', '<div id="jsb" onclick="void 0" style="position:absolute;left:210px;top:8px;width:60px;height:40px;background:#333"></div><div id="ptr" style="position:absolute;left:210px;top:60px;width:60px;height:40px;cursor:pointer;background:#444"></div><a id="lnk"');
  const page = await open(JS, { player: true, cfg: { kp: true } });
  await page.evaluate(() => { document.getElementById('box').style.cursor = 'pointer'; });                       // (the page's own click listener on the document records the clicks it gets)
  await fullscreen(page); await firstState(page);
  await page.touchscreen.tap(240, 28); await wait(page, 200);
  assert.deepEqual(await clicks(page), ['jsb'], 'the onclick div got its tap: ' + JSON.stringify(await clicks(page)));
  await page.touchscreen.tap(240, 80); await wait(page, 200);
  assert.deepEqual(await clicks(page), ['jsb', 'ptr'], 'and the pointer-cursor one');
  assert.equal((await taps(page)).length, 0, 'neither was a tap on the film');
  await page.touchscreen.tap(250, 400);                                              // the film itself, although the whole box has a pointer cursor
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vtap'), null, { timeout: 5000 });
  assert.deepEqual(await clicks(page), ['jsb', 'ptr'], 'the page did not get the tap on its film');
  await page.context().close();
});

test('VP2: the engine\'s own bar is put away again when the page turns it on again (review F10)', async () => {
  const page = await open(CHROME, { player: true, cfg: { kp: true } });
  await page.evaluate(() => { window.KestrelVP.c.idle = 300; window.KestrelVP.c.busy = 300; });
  await fullscreen(page); await firstState(page);
  await page.waitForFunction(() => document.getElementById('v').controls === false, null, { timeout: 4000 });
  await page.evaluate(() => { document.getElementById('v').controls = true; });
  await page.waitForFunction(() => document.getElementById('v').controls === false, null, { timeout: 4000 });
  await page.context().close();
});
