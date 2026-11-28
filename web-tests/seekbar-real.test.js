'use strict';
// YS1 (research/SEEKBAR.md): the cases an INDEPENDENT REVIEW of seekbar.js found (2026-11-15), each reproduced against the first version and fixed:
//  1. YouTube's REAL bar (markup captured from the live embed page, build 20261114) was rejected when the finger lay on the 3 px rail or the dot: the rail's background div (opacity .3) was taken for the bar and
//     then "hidden". Now the role=slider 44 px drag container wins and a bar's own opacity is not its visibility.
//  2. things of the page over the bar (a cookie banner, a modal scrim, a playlist of links under the picture), a rating range input, a <progress>, a price slider made the video jump;
//  3. a live stream with a huge finite length, a DVR window; 4. another media element's loop hid the page's own seek; 5. real players missed: Plyr (bar 40 percent of the picture), media-chrome's
//  <media-time-range>, Shaka's invisible range input, aria-valuemax 1000 / 10000; Video.js' wider wrapper gave a 2 percent error; 6. an answer of the app that comes seconds late.
// Helpers and mock markup: seekbar-lib.js. Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { launch, close, open, LEN, PLAYER, YT, REAL_YT, VIDEO, rectOf, time, msgs, tapBar, report, near } = require('./seekbar-lib');

before(launch);
after(close);

const reset = async (page) => { await page.evaluate(() => { document.getElementById('v').currentTime = 0; }); await page.waitForTimeout(200); };

// ---- the owner's real player

test('YS1 review 1: the REAL YouTube bar (yt-progress-bar, role=slider drag container, 3 px rail with a .3 opacity background): a tap anywhere on it jumps - on the rail, 1 to 20 px off it, on the red dot - while the page only paints its light "Hovered" line', async () => {
  const page = await open(REAL_YT('', 50));
  await page.evaluate(() => {            // what the page itself does with a finger: the light line up to the finger, no seek (the owner's screen)
    const bar = document.getElementById('bar'), hov = document.getElementById('hov');
    ['pointerdown', 'pointermove'].forEach((n) => bar.addEventListener(n, (e) => { hov.style.width = (e.clientX - bar.getBoundingClientRect().left) + 'px'; }));
  });
  const line = await rectOf(page, '#line'), bar = await rectOf(page, '#bar');
  for (const dy of [0, 1, 2, 4, 8, 15, 20, -8, -15]) {
    await reset(page);
    await page.touchscreen.tap(bar.left + bar.width * 0.7, line.top + line.height / 2 + dy);
    await page.waitForTimeout(450);
    assert.ok(near(await time(page), 0.7 * LEN, 1.5), 'finger ' + dy + ' px from the rail centre: jumped, at ' + await time(page));
  }
  assert.ok(await page.evaluate(() => parseFloat(document.getElementById('hov').style.width) > 100), 'the page\'s own light line is there (the symptom), and the jump came on top of it');
  await reset(page);
  const dot = await rectOf(page, '#dot');                               // the red dot (played 50 percent)
  await page.touchscreen.tap(dot.left + dot.width / 2, dot.top + dot.height / 2 + 1);
  await page.waitForTimeout(450);
  assert.ok(near(await time(page), 0.5 * LEN, 1.8), 'the dot: at ' + await time(page));
  await page.context().close();
});

test('YS1 review 1: the real YouTube bar with its controls hidden (the first tap only brings them up): not moved; the rail line at opacity .3 is not "hidden" by itself', async () => {
  const page = await open(REAL_YT());
  const line = await rectOf(page, '#line'), bar = await rectOf(page, '#bar');
  await page.evaluate(() => { document.querySelector('.player-controls-content').style.opacity = '0'; });
  await page.touchscreen.tap(bar.left + bar.width * 0.7, line.top + 1); await page.waitForTimeout(450);
  assert.ok(near(await time(page), 0, 0.5), 'hidden: at ' + await time(page));
  await page.evaluate(() => { document.querySelector('.player-controls-content').style.opacity = '1'; });
  await page.touchscreen.tap(bar.left + bar.width * 0.7, line.top + 1); await page.waitForTimeout(450);
  assert.ok(near(await time(page), 0.7 * LEN, 1.5), 'shown: at ' + await time(page));
  await page.context().close();
});

// ---- not a bar

test('YS1 review 2: a cookie banner over the bar, a modal scrim over the player, a playlist of links under the picture, a rating range input, a <progress> strip, a price slider: none of them moves the film', async () => {
  const cases = [
    ['cookie banner text over the bar', YT() + '<div id="x" style="position:absolute;left:0;top:176px;width:400px;height:64px;background:#fff;z-index:50;font:14px sans-serif">We use cookies. Tap for details.</div>', '#x', 0.5],
    ['full-page modal scrim over the player', YT() + '<div id="x" style="position:fixed;left:0;top:0;width:500px;height:700px;background:rgba(0,0,0,.5);z-index:99"></div>', '#bar', 0.7],
    ['a playlist strip of links right under the picture (class playlist-slider)', '<div style="position:relative;width:400px;height:240px">' + VIDEO + '</div><div id="x" class="playlist-slider" style="position:absolute;left:0;top:246px;width:400px;height:64px;display:flex;gap:4px;background:#eee"><a href="#a" class="th" style="flex:1;background:#8a8">one</a><a href="#b" class="th" style="flex:1;background:#a88">two</a></div>', '.th', 0.5],
    ['a rating range input (max 5) under the picture', PLAYER('') + '<div style="position:absolute;left:0;top:248px;width:400px"><label>Rate <input id="x" type="range" min="1" max="5" step="1" value="3" style="width:380px;margin:0"></label></div>', '#x', 0.8],
    ['a <progress> strip over the picture', PLAYER('<progress id="x" class="progress" max="100" value="80" style="position:absolute;left:0;bottom:2px;width:400px;height:10px"></progress>'), '#x', 0.3],
    ['a price slider (role=slider, no range of its own) just under the picture', '<div style="position:relative;width:400px;height:240px">' + VIDEO + '</div><div id="x" role="slider" aria-label="Maximum price" aria-valuemin="0" aria-valuenow="50" style="position:absolute;left:10px;top:250px;width:380px;height:12px;background:#ccc"></div>', '#x', 0.8],
  ];
  for (const [what, body, sel, f] of cases) {
    const page = await open(body);
    const r = await rectOf(page, sel);
    await page.touchscreen.tap(r.left + r.width * f, r.top + r.height / 2); await page.waitForTimeout(500);
    assert.ok(near(await time(page), 0, 0.5), what + ': nothing moved, at ' + await time(page));
    await page.context().close();
  }
});

test('YS1 review 3: a live stream whose length is 2^32 s (as Shaka reports it) and a DVR window (seekable only 10 s of 60) are left alone, with the reasons', async () => {
  let page = await open(YT());
  await page.evaluate(() => { Object.defineProperty(document.getElementById('v'), 'duration', { get: () => 4294967296 }); });
  await tapBar(page, 0.1); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5));
  let r = await report(page); assert.deepEqual({ f: r.f, o: r.o, r: r.r }, { f: 0, o: 1, r: 'live' });
  await page.context().close();
  page = await open(YT());
  await page.evaluate(() => { Object.defineProperty(document.getElementById('v'), 'seekable', { get: () => ({ length: 1, start: () => 0, end: () => 10 }) }); });
  await tapBar(page, 0.7); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5));
  r = await report(page); assert.deepEqual({ f: r.f, o: r.o, r: r.r }, { f: 0, o: 1, r: 'range' });
  await page.context().close();
});

// ---- the page's own seek is respected

test('YS1 review 4: the page seeks the film itself while ANOTHER media element of the page loops (its seeking events come after the film\'s): the page\'s seek stands, no second one', async () => {
  const page = await open(YT('') + '<video id="loop" src="https://cdn.example/loop.wav" muted loop preload="auto" style="position:absolute;left:420px;top:0;width:60px;height:40px"></video>');
  await page.waitForFunction(() => document.getElementById('loop').readyState >= 2);
  await page.evaluate(() => {
    document.getElementById('bar').addEventListener('pointerup', () => {
      document.getElementById('v').currentTime = 12;                                  // the page's own jump
      setTimeout(() => { document.getElementById('loop').currentTime = 0.2; }, 40);  // and the other element wraps around right after
    });
  });
  await tapBar(page, 0.7); await page.waitForTimeout(700);
  assert.ok(near(await time(page), 12, 0.6), 'the page\'s place, at ' + await time(page));
  assert.equal(await page.evaluate(() => window.__seeks), 2, 'two seeking events on the page: the film once, the loop once');
  const r = await report(page); assert.deepEqual({ t: r.t, s: r.s, f: r.f }, { t: 1, s: 1, f: 0 });
  await page.context().close();
});

test('YS1 review 4: two players on one page: the bar of the second moves only the second film', async () => {
  const mk = (id, top) => `<div class="html5-video-player" style="position:absolute;left:0;top:${top}px;width:400px;height:240px;background:#000"><video id="${id}" src="https://cdn.example/film.wav" preload="auto" muted style="position:absolute;left:0;top:0;width:400px;height:240px"></video>`
    + `<div id="bar-${id}" class="ytp-progress-bar" role="slider" aria-label="Seek slider" aria-valuemin="0" aria-valuemax="${LEN}" style="position:absolute;left:12px;right:12px;bottom:14px;height:20px;touch-action:none"></div></div>`;
  const page = await open(mk('v', 0) + mk('v2', 260));
  await page.waitForFunction(() => document.getElementById('v2').readyState >= 3);
  const r = await rectOf(page, '#bar-v2');
  await page.touchscreen.tap(r.left + r.width * 0.5, r.top + r.height / 2); await page.waitForTimeout(500);
  const t = await page.evaluate(() => [document.getElementById('v').currentTime, document.getElementById('v2').currentTime]);
  assert.ok(near(t[0], 0, 0.3), 'the first film is untouched, at ' + t[0]);
  assert.ok(near(t[1], 0.5 * LEN, 1.5), 'the second film moved, at ' + t[1]);
  await page.context().close();
});

// ---- other players

test('YS1 review 5: Plyr (the bar is only 40 percent of the picture, between the buttons) and media-chrome\'s <media-time-range> (no class, a custom element) are recognised; Plyr\'s volume range is not', async () => {
  const plyr = PLAYER('<div class="plyr__controls" style="position:absolute;left:0;right:0;bottom:0;height:40px;display:flex;align-items:center;gap:6px;padding:0 6px;box-sizing:border-box"><button type="button" style="width:30px;height:30px">P</button><span style="width:40px">0:00</span>'
    + '<div class="plyr__progress" style="width:160px;height:24px"><input id="bar" type="range" min="0" max="100" step="0.01" value="0" data-plyr="seek" aria-label="Seek" style="width:100%;height:24px;margin:0"></div><span style="width:40px">1:00</span><input id="vol" type="range" min="0" max="1" data-plyr="volume" aria-label="Volume" style="width:50px;height:20px;margin:0"></div>');
  let page = await open(plyr);
  const r = await rectOf(page, '#bar');
  assert.ok(r.width / 400 < 0.45, 'the bar is under half of the picture: ' + r.width);
  await page.touchscreen.tap(r.left + r.width * 0.5, r.top + r.height / 2); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0.5 * LEN, 4), 'Plyr: at ' + await time(page));
  await reset(page);
  const vr = await rectOf(page, '#vol');
  await page.touchscreen.tap(vr.left + vr.width * 0.5, vr.top + vr.height / 2); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5), 'the volume range moves nothing, at ' + await time(page));
  await page.context().close();
  page = await open(PLAYER('<media-time-range id="bar" style="display:block;position:absolute;left:30px;right:30px;bottom:10px;height:12px;background:#555"></media-time-range>'));
  await tapBar(page, 0.5); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0.5 * LEN, 1.5), 'media-chrome: at ' + await time(page));
  await page.context().close();
});

test('YS1 review 5: Shaka\'s real range input is invisible itself (opacity 0) on top of a drawn bar and works; its container hidden does not; Video.js\' wider wrapper does not skew the place', async () => {
  const shaka = (hidden) => PLAYER(`<div class="shaka-controls-container" style="position:absolute;left:0;right:0;bottom:0;height:44px;opacity:${hidden ? 0 : 1}"><div style="position:absolute;left:10px;right:10px;bottom:10px;height:6px;background:#777"></div><input id="bar" type="range" min="0" max="1" step="any" value="0" class="shaka-seek-bar" aria-label="Seek" style="position:absolute;left:10px;right:10px;bottom:4px;width:380px;height:18px;margin:0;opacity:0"></div>`);
  let page = await open(shaka(false));
  await tapBar(page, 0.6); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0.6 * LEN, 3), 'Shaka: at ' + await time(page));
  await page.context().close();
  page = await open(shaka(true));
  await tapBar(page, 0.6); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5), 'Shaka, controls hidden: at ' + await time(page));
  await page.context().close();
  page = await open(PLAYER('<div class="vjs-control-bar" style="position:absolute;left:0;right:0;bottom:0;height:30px"><div class="vjs-progress-control vjs-control" style="position:absolute;left:20px;width:360px;top:0;height:30px"><div id="bar" class="vjs-progress-holder vjs-slider" role="slider" aria-valuemin="0" aria-valuemax="100" style="position:absolute;left:60px;width:240px;top:12px;height:6px;background:#555"></div></div></div>'));
  const r = await rectOf(page, '#bar');
  await page.touchscreen.tap(r.left + r.width * 0.1, r.top + 3); await page.waitForTimeout(450);
  assert.ok(near(await time(page), 0.1 * LEN, 1.2), 'Video.js 10 percent of the RAIL, at ' + await time(page));
  await reset(page);
  await page.touchscreen.tap(r.left + r.width * 0.9, r.top + 3); await page.waitForTimeout(450);
  assert.ok(near(await time(page), 0.9 * LEN, 1.2), 'Video.js 90 percent of the RAIL, at ' + await time(page));
  await page.context().close();
});

test('YS1 review 5: a bar whose range is 1000 or 10000 (per mille, basis points) is treated like a percent bar', async () => {
  for (const mx of ['1000', '10000']) {
    const page = await open(YT());
    await page.evaluate((m) => { document.getElementById('bar').setAttribute('aria-valuemax', m); }, mx);
    await tapBar(page, 0.7); await page.waitForTimeout(500);
    assert.ok(near(await time(page), 0.7 * LEN, 1.5), 'max ' + mx + ': at ' + await time(page));
    await page.context().close();
  }
});

// ---- time

test('YS1 review 6: an answer of the app that comes seconds after the finger went up is too late: the person has moved on, nothing jumps, the report says so', async () => {
  const page = await open(YT(), { delay: 900 });                       // the test clock: late means over 600 ms
  await tapBar(page, 0.7); await page.waitForTimeout(1400);
  assert.ok(near(await time(page), 0, 0.5), 'at ' + await time(page));
  const r = await report(page); assert.deepEqual({ f: r.f, o: r.o, r: r.r }, { f: 0, o: 1, r: 'late' });
  await page.context().close();
});
