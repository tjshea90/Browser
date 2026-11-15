'use strict';
// FB2 / FB3 (owner message 47, 2026-11-15): "The video didn't pause when switching orientation ... for a few seconds the video is the wrong size in either mode. Make it pause the video and properly set up
// the size and orientation smoothly then resume." turn.js in real headless Chromium: the window is turned with setViewportSize (500 x 800 <-> 800 x 500) against the fake extension API of videobar-lib.js.
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { launch, close, open, msgs, tapAt, VIDEO } = require('./videobar-lib');

before(launch);
after(close);

const wait = (page, ms) => page.waitForTimeout(ms);
const PLAYING = (page) => page.evaluate(async () => { const v = document.getElementById('v'); v.muted = true; await v.play(); window.__resizes = 0; window.addEventListener('resize', () => window.__resizes++); return !v.paused; });
const paused = (page) => page.evaluate(() => document.getElementById('v').paused);
const turns = async (page) => (await msgs(page, 'vbk')).filter((m) => m.e === 'turn');

test('FB2: a playing video (muted: no media session) is paused when the screen turns, the page gets one more resize, and it plays again once the viewport is still', async () => {
  const page = await open(VIDEO(), { turn: true });
  assert.equal(await PLAYING(page), true, 'the clip plays');
  await page.setViewportSize({ width: 800, height: 500 });
  await wait(page, 150);
  assert.equal(await paused(page), true, 'paused while the screen turns');
  await wait(page, 1000);
  assert.equal(await paused(page), false, 'plays again after the turn');
  assert.ok(await page.evaluate(() => window.__resizes) >= 2, 'the real resize and the one more the guard sends: ' + await page.evaluate(() => window.__resizes));
  const t = await turns(page);
  assert.equal(t.length, 1, JSON.stringify(t));
  assert.deepEqual([t[0].a, t[0].n], ['played', 1]);
  await page.context().close();
});

test('FB2: turning back works the same, in both directions', async () => {
  const page = await open(VIDEO(), { turn: true });
  await PLAYING(page);
  await page.setViewportSize({ width: 800, height: 500 }); await wait(page, 1100);
  assert.equal(await paused(page), false);
  await page.setViewportSize({ width: 500, height: 800 }); await wait(page, 150);
  assert.equal(await paused(page), true, 'paused again for the turn back');
  await wait(page, 1000);
  assert.equal(await paused(page), false);
  assert.deepEqual((await turns(page)).map((m) => m.a), ['played', 'played']);
  await page.context().close();
});

test('FB2: a touch during the turn is the person taking over: the video stays paused', async () => {
  const page = await open(VIDEO(), { turn: true });
  await PLAYING(page);
  await page.setViewportSize({ width: 800, height: 500 }); await wait(page, 100);
  await tapAt(page, 200, 200); await wait(page, 1000);
  assert.equal(await paused(page), true);
  assert.equal((await turns(page))[0].a, 'skipped');
  await page.context().close();
});

test('FB2: a change of size that keeps the shape (a keyboard, a toolbar) is not a turn: nothing is paused, nothing is sent', async () => {
  const page = await open(VIDEO(), { turn: true });
  await PLAYING(page);
  await page.setViewportSize({ width: 500, height: 520 }); await wait(page, 400);     // (a keyboard in portrait: the window stays taller than wide)
  await page.setViewportSize({ width: 500, height: 510 }); await wait(page, 600);
  assert.equal(await paused(page), false);
  assert.deepEqual(await turns(page), []);
  await page.context().close();
});

test('FB2: a video that was paused stays paused (nothing is ever started that we did not pause), and the app is told "none"', async () => {
  const page = await open(VIDEO(), { turn: true });
  await page.evaluate(() => { window.__resizes = 0; window.addEventListener('resize', () => window.__resizes++); });
  await page.setViewportSize({ width: 800, height: 500 }); await wait(page, 900);
  assert.equal(await paused(page), true);
  assert.deepEqual((await turns(page)).map((m) => [m.a, m.n]), [['none', 0]]);
  assert.ok(await page.evaluate(() => window.__resizes) >= 2, 'the page still gets its one more resize');
  await page.context().close();
});

test('FB2: a video inside a web component\'s shadow root is paused and played again too (the Yahoo player is one)', async () => {
  const body = '<div id="host" style="position:absolute;left:20px;top:100px;width:400px;height:240px"></div><script>var root = document.getElementById("host").attachShadow({ mode: "open" });'
    + 'root.innerHTML = \'<video id="v" src="https://cdn.example/film.wav" preload="auto" muted style="width:400px;height:240px"></video>\'; window.__deepV = function () { return root.getElementById("v"); };</script>';
  const page = await open(body, { turn: true });
  await page.evaluate(async () => { const v = window.__deepV(); await v.play(); });
  await page.setViewportSize({ width: 800, height: 500 }); await wait(page, 150);
  assert.equal(await page.evaluate(() => window.__deepV().paused), true);
  await wait(page, 1000);
  assert.equal(await page.evaluate(() => window.__deepV().paused), false);
  await page.context().close();
});

test('FB2: a page that starts the video again by itself during the turn is left alone (no second play, the app is told "skipped")', async () => {
  const page = await open(VIDEO(), { turn: true });
  await PLAYING(page);
  await page.evaluate(() => { const v = document.getElementById('v'); v.addEventListener('pause', () => setTimeout(() => v.play().catch(() => {}), 40)); });
  await page.setViewportSize({ width: 800, height: 500 }); await wait(page, 1000);
  assert.equal(await paused(page), false);
  assert.equal((await turns(page))[0].a, 'skipped');
  await page.context().close();
});

test('FB2: a second turn before the first one settled extends it: the video is paused once and played once', async () => {
  const page = await open(VIDEO(), { turn: true });
  await PLAYING(page);
  await page.evaluate(() => { const v = document.getElementById('v'); window.__log = []; v.addEventListener('pause', () => window.__log.push('pause')); v.addEventListener('play', () => window.__log.push('play')); });
  await page.setViewportSize({ width: 800, height: 500 }); await wait(page, 120);
  await page.setViewportSize({ width: 500, height: 800 }); await wait(page, 1200);
  assert.deepEqual(await page.evaluate(() => window.__log), ['pause', 'play']);
  assert.equal((await turns(page)).length, 1);
  await page.context().close();
});

test('FB3: the extra resize comes once the viewport is still, and a player that sizes itself on every resize ends with the final size', async () => {
  const page = await open(VIDEO() + '<div id="box" style="position:absolute;left:0;top:0;width:10px;height:10px"></div>', { turn: true });
  await PLAYING(page);
  // a page that sizes a box from the viewport but reads a STALE height the first time (as the owner's player did: the width was new, the height was the old one)
  await page.evaluate(() => {
    let first = true;
    window.addEventListener('resize', () => { const b = document.getElementById('box'); b.style.width = innerWidth + 'px'; b.style.height = (first ? 575 : innerHeight) + 'px'; first = false; });
  });
  await page.setViewportSize({ width: 800, height: 500 }); await wait(page, 1100);
  assert.deepEqual(await page.evaluate(() => { const b = document.getElementById('box'); return [parseInt(b.style.width, 10), parseInt(b.style.height, 10)]; }), [800, 500], 'the box has the final size');
  await page.context().close();
});

test('FB4 + ROT2: a page says its viewport once soon after it is up (f = 0), and, once it has been still, in full screen (f = 1), in device pixels; a resize outside full screen says nothing', async () => {
  const page = await open(VIDEO(), { turn: true });
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vpt'), null, { timeout: 8000 });   // the one report of a page that is up (a second after it started: no fixed wait)
  const hello = await msgs(page, 'vpt');
  assert.equal(hello.length, 1, 'the one report of a page that is up: ' + JSON.stringify(hello));
  assert.deepEqual([hello[0].w, hello[0].h, hello[0].f], [500, 800, 0], JSON.stringify(hello[0]));
  await page.setViewportSize({ width: 500, height: 790 });                 // a resize outside full screen (a toolbar): nothing is said
  await wait(page, 500);
  assert.equal((await msgs(page, 'vpt')).length, 1, 'a resize outside full screen says nothing');
  await page.setViewportSize({ width: 500, height: 800 });
  await wait(page, 300);
  await page.evaluate(() => document.getElementById('v').requestFullscreen());
  await page.waitForFunction(() => window.__msgs.filter((m) => m && m.k === 'vpt').length >= 2, null, { timeout: 8000 });
  const m = await msgs(page, 'vpt');
  const last = m[m.length - 1];
  assert.deepEqual([last.w, last.h, last.f], [500, 800, 1], JSON.stringify(last));
  await page.context().close();
});

test('review of alpha53: a turn and back inside one window leaves the shape right, so the next unrelated resize is not taken for a turn', async () => {
  const page = await open(VIDEO(), { turn: true });
  await PLAYING(page);
  await page.setViewportSize({ width: 800, height: 500 });                  // the turn ...
  await wait(page, 100);
  await page.setViewportSize({ width: 500, height: 800 });                  // ... and back, before it settled
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vbk' && m.e === 'turn'), null, { timeout: 8000 });
  await wait(page, 900);
  const before = (await msgs(page, 'vbk')).filter((m) => m.e === 'turn').length;
  await page.setViewportSize({ width: 500, height: 520 });                  // a keyboard: the shape is the same
  await wait(page, 1000);
  assert.equal((await msgs(page, 'vbk')).filter((m) => m.e === 'turn').length, before, 'no new turn for a resize that keeps the shape');
  await page.context().close();
});
