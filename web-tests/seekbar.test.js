'use strict';
// YS1 (owner message 42, 2026-11-15; research/SEEKBAR.md): seekbar.js, the safety net that makes a tap on a video player's progress bar jump the video, against MOCK players in real headless Chromium with a
// touch screen. The mocks use markup read from the players' own public sources / known to be stable (YouTube: .ytp-progress-bar role=slider with aria-valuemax = the length; Video.js: .vjs-progress-holder;
// Plyr: input[type=range][data-plyr=seek]; JW Player: .jw-slider-time role=slider). A real YouTube page could not be had from the sandbox (bot-check page): what the real page does is unverified until the owner's
// Diagnostics line "Tap on the video bar" shows numbers.
// The extension API is a stub that answers the `guard` question like the app does ({seek: true} unless a test says otherwise) and records every message.
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SB = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'seekbar.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required', '--mute-audio'] }); });
after(async () => { await browser.close(); });

/** An 8-bit mono silent WAV of [seconds]: a media element that really loads, has a length and seeks, with no codec. */
function wavBytes(seconds) {
  const n = 8000 * seconds;
  const h = Buffer.alloc(44);
  h.write('RIFF', 0); h.writeUInt32LE(36 + n, 4); h.write('WAVEfmt ', 8); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
  h.writeUInt32LE(8000, 24); h.writeUInt32LE(8000, 28); h.writeUInt16LE(1, 32); h.writeUInt16LE(8, 34); h.write('data', 36); h.writeUInt32LE(n, 40);
  return Buffer.concat([h, Buffer.alloc(n, 0x80)]);
}
const FILM = wavBytes(60);
const LEN = 60;
const FAST = { waitMs: 120, flushMs: 80, tapMs: 400, reaskMs: 500, keepMs: 800 };   // the clocks of the script, shortened

const VIDEO = '<video id="v" src="https://cdn.example/film.wav" preload="auto" muted style="position:absolute;left:0;top:0;width:400px;height:240px;background:#222"></video>';
/** A player box with the picture and [inner] over it. */
const PLAYER = (inner) => `<div id="player" class="html5-video-player" style="position:relative;width:400px;height:240px;background:#000">${VIDEO}${inner || ''}</div>`;
/** YouTube-like chrome: the bar sits over the bottom of the picture, thin, with a taller hit area (touch-action:none as on the real bar); the page does NOT seek on a tap unless a test script says so. */
const YT = (extra) => PLAYER(`<div class="ytp-chrome-bottom" style="position:absolute;left:0;right:0;bottom:0;height:44px"><div class="ytp-progress-bar-container" style="position:absolute;left:12px;right:12px;bottom:30px;height:6px">`
  + `<div id="bar" class="ytp-progress-bar" role="slider" aria-label="Seek slider" aria-valuemin="0" aria-valuemax="${LEN}" aria-valuenow="0" tabindex="0" style="position:absolute;left:0;right:0;top:-10px;height:26px;touch-action:none"><div class="ytp-play-progress" style="width:10%;height:6px;background:red;margin-top:10px"></div><div class="ytp-scrubber-container" style="position:absolute;left:10%;top:6px;width:12px;height:12px;border-radius:6px;background:red"></div></div>`
  + `</div>${extra || ''}</div>`);

async function open(body, o) {
  o = o || {};
  const context = await browser.newContext({ hasTouch: true, viewport: { width: 500, height: 700 } });
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.hostname === 'cdn.example' && u.pathname === '/film.wav') {                   // a file server that answers byte ranges, as every real one does (without them the film is not seekable)
      const rg = /bytes=(\d+)-(\d*)/.exec(r.request().headers().range || '');
      if (!rg) return r.fulfill({ contentType: 'audio/wav', headers: { 'accept-ranges': 'bytes' }, body: FILM });
      const a = +rg[1], b = rg[2] ? Math.min(+rg[2], FILM.length - 1) : FILM.length - 1;
      return r.fulfill({ status: 206, contentType: 'audio/wav', headers: { 'accept-ranges': 'bytes', 'content-range': `bytes ${a}-${b}/${FILM.length}` }, body: FILM.subarray(a, b + 1) });
    }
    if (u.hostname === 'movie.example' && u.pathname === '/') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>movie</title></head><body style="margin:0">${body}</body></html>` });
    if (u.hostname === 'player.example') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><body style="margin:0">${o.frameBody || ''}</body></html>` });
    return r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>other</title>other' });
  });
  const page = await context.newPage();
  await page.addInitScript((cfg) => {
    window.__msgs = []; window.__asked = 0; window.__seeks = 0;
    window.__kstSBcfg = cfg.fast;
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); if (m && m.k === 'guard') { window.__asked++; return Promise.resolve({ on: true, top: location.hostname, skip: false, seek: cfg.seek !== false }); } return Promise.resolve(undefined); }, onMessage: { addListener() {} } } };
    document.addEventListener('seeking', () => { window.__seeks++; }, true);
  }, { fast: o.slow ? {} : FAST, seek: o.seek });
  await page.addInitScript({ content: SB });
  await page.goto('https://movie.example/');
  await page.waitForFunction(() => { const v = document.getElementById('v'); return !v || (v.readyState >= 3 && v.duration > 50); }, null, { timeout: 15000 });
  return page;
}
const rectOf = (page, sel) => page.evaluate((s) => { const r = document.querySelector(s).getBoundingClientRect(); return { left: r.left, top: r.top, width: r.width, height: r.height }; }, sel);
const at = (r, f) => ({ x: r.left + r.width * f, y: r.top + r.height / 2 });
const time = (page) => page.evaluate(() => document.getElementById('v').currentTime);
const msgs = (page, k) => page.evaluate((kk) => window.__msgs.filter((m) => m && m.k === kk), k);
async function tapBar(page, f, sel) { const r = await rectOf(page, sel || '#bar'); const p = at(r, f); await page.touchscreen.tap(p.x, p.y); return p; }
async function report(page) { await page.waitForTimeout(300); const all = await msgs(page, 'sbk'); const sum = { t: 0, s: 0, f: 0, o: 0, r: '' }; for (const m of all) { sum.t += m.t; sum.s += m.s; sum.f += m.f; sum.o += m.o; if (m.r) sum.r = m.r; } return sum; }
const near = (a, b, tol) => Math.abs(a - b) <= (tol || 1.5);

/** A touch that stays where it is for [ms], or slides to (x2, y2): raw touch events through the debugger protocol. */
async function touch(page, x, y, o) {
  o = o || {};
  const c = await page.context().newCDPSession(page);
  await c.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x, y }] });
  if (o.toX !== undefined) { for (let i = 1; i <= 6; i++) await c.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: x + (o.toX - x) * i / 6, y: y + ((o.toY === undefined ? y : o.toY) - y) * i / 6 }] }); }
  if (o.holdMs) await page.waitForTimeout(o.holdMs);
  await c.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
}

// ---- the owner's case: the page does not answer the tap

test('YS1: the page ignores a tap on its bar: the video jumps to the tapped place (70 percent), paused stays paused, one seek, one report', async () => {
  const page = await open(YT());
  assert.ok(near(await time(page), 0, 0.5));
  await tapBar(page, 0.7);
  await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0.7 * LEN, 1.5), 'jumped to about 70 percent, at ' + await time(page));
  assert.equal(await page.evaluate(() => document.getElementById('v').paused), true, 'a paused video stays paused');
  assert.equal(await page.evaluate(() => window.__seeks), 1, 'one seek');
  const r = await report(page);
  assert.deepEqual({ t: r.t, s: r.s, f: r.f, o: r.o }, { t: 1, s: 0, f: 1, o: 0 });
  await page.context().close();
});

test('YS1: a tap near the left end, the right end and above / below the thin bar (inside its hit area) all land on the right place', async () => {
  const page = await open(YT());
  await tapBar(page, 0.02); await page.waitForTimeout(400);
  assert.ok(near(await time(page), 0.02 * LEN, 1.5), 'left end ' + await time(page));
  await tapBar(page, 0.98); await page.waitForTimeout(400);
  assert.ok(near(await time(page), 0.98 * LEN, 1.5), 'right end ' + await time(page));
  const r = await rectOf(page, '#bar');                      // the finger lands 9 px above the middle of the 26 px hit area, still on the bar
  await page.touchscreen.tap(r.left + r.width * 0.4, r.top + 4); await page.waitForTimeout(400);
  assert.ok(near(await time(page), 0.4 * LEN, 1.5), 'high in the hit area ' + await time(page));
  await page.context().close();
});

test('YS1: a tap on the red dot itself (a child of the bar) jumps too, and a playing video keeps playing from the new place', async () => {
  const page = await open(YT());
  await page.evaluate(() => { const v = document.getElementById('v'); v.play(); });
  await page.waitForFunction(() => !document.getElementById('v').paused && document.getElementById('v').currentTime > 0.2);
  const dot = await rectOf(page, '.ytp-scrubber-container');
  const bar = await rectOf(page, '#bar');
  const x = dot.left + dot.width / 2, y = dot.top + dot.height / 2;
  await page.touchscreen.tap(x, y); await page.waitForTimeout(500);
  const want = (x - bar.left) / bar.width * LEN;
  assert.ok(near(await time(page), want, 2), 'near ' + want + ', at ' + await time(page));
  assert.equal(await page.evaluate(() => document.getElementById('v').paused), false);
  await page.context().close();
});

// ---- when the page moves the video itself nothing more is done

test('YS1: the page seeks itself on pointerup (to a chapter start) - no second seek, the page keeps its place', async () => {
  const page = await open(YT());
  await page.evaluate(() => { document.getElementById('bar').addEventListener('pointerup', () => { document.getElementById('v').currentTime = 12; }); });
  await tapBar(page, 0.7);
  await page.waitForTimeout(600);
  assert.ok(near(await time(page), 12, 0.5), 'the page\'s own place, at ' + await time(page));
  assert.equal(await page.evaluate(() => window.__seeks), 1);
  const r = await report(page);
  assert.deepEqual({ t: r.t, s: r.s, f: r.f }, { t: 1, s: 1, f: 0 });
  assert.equal((await msgs(page, 'guard')).length, 0, 'the app is not even asked when the page did its job');
  await page.context().close();
});

test('YS1: the page seeks itself on pointerdown, on click after 250 ms and on mouseup: each counts as the page\'s own work', async () => {
  for (const how of ['pointerdown', 'click250', 'mouseup']) {
    const page = await open(YT());
    await page.evaluate((h) => {
      const b = document.getElementById('bar'), v = document.getElementById('v');
      if (h === 'click250') b.addEventListener('click', () => setTimeout(() => { v.currentTime = 20; }, 80));
      else b.addEventListener(h, () => { v.currentTime = 20; });
    }, how);
    await tapBar(page, 0.7);
    await page.waitForTimeout(700);
    assert.ok(near(await time(page), 20, 0.6), how + ': the page\'s place, at ' + await time(page));
    assert.equal(await page.evaluate(() => window.__seeks), 1, how + ': one seek only');
    await page.context().close();
  }
});

// ---- when nothing must happen

test('YS1: controls hidden (the first tap only brings them up): no seek; once they show, the next tap seeks', async () => {
  const page = await open(YT());
  await page.evaluate(() => { const c = document.querySelector('.ytp-chrome-bottom'); c.style.opacity = '0'; });
  await tapBar(page, 0.7); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5), 'hidden: nothing moved, at ' + await time(page));
  await page.evaluate(() => { document.querySelector('.ytp-chrome-bottom').style.opacity = '1'; });
  await tapBar(page, 0.7); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0.7 * LEN, 1.5), 'shown: moved, at ' + await time(page));
  await page.context().close();
});

test('YS1: not a seek bar: a volume slider, a speed slider, a thin line far from the video, a course-progress bar under the page, a decorative line over the video', async () => {
  const cases = [
    ['volume slider (short, named)', `<div id="x" role="slider" aria-label="Volume" aria-valuemax="100" style="position:absolute;left:300px;top:200px;width:80px;height:10px;background:#888"></div>`, 0.5],
    ['a wide slider named speed', `<div id="x" role="slider" aria-label="Playback speed" style="position:absolute;left:12px;top:200px;width:376px;height:10px;background:#888"></div>`, 0.5],
    ['a lesson progress bar far below the video', `<div id="x" class="lesson-progress" style="position:absolute;left:12px;top:400px;width:376px;height:8px;background:#888"></div>`, 0.5],
    ['a wide thin line over the video that has no seek word', `<div id="x" class="divider" style="position:absolute;left:12px;top:200px;width:376px;height:4px;background:#888"></div>`, 0.5],
    ['an advertisement\'s progress line', `<div id="x" class="ytp-ad-persistent-progress-bar-container" style="position:absolute;left:0;top:225px;width:400px;height:4px;background:#fc0"></div>`, 0.5],
  ];
  for (const [what, extra, f] of cases) {
    const page = await open(PLAYER(extra));
    const r = await rectOf(page, '#x');
    await page.touchscreen.tap(r.left + r.width * f, r.top + r.height / 2); await page.waitForTimeout(500);
    assert.ok(near(await time(page), 0, 0.5), what + ': nothing moved, at ' + await time(page));
    await page.context().close();
  }
});

test('YS1: a live stream (no length) and an ad that plays are never moved; the report says why', async () => {
  let page = await open(YT());
  await page.evaluate(() => { Object.defineProperty(document.getElementById('v'), 'duration', { get: () => Infinity }); });
  await tapBar(page, 0.7); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5));
  let r = await report(page); assert.deepEqual({ t: r.t, f: r.f, o: r.o, r: r.r }, { t: 1, f: 0, o: 1, r: 'live' });
  await page.context().close();
  page = await open(YT());
  await page.evaluate(() => { document.getElementById('player').classList.add('ad-showing'); });
  await tapBar(page, 0.7); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5));
  r = await report(page); assert.deepEqual({ f: r.f, o: r.o, r: r.r }, { f: 0, o: 1, r: 'ad' });
  await page.context().close();
});

test('YS1: a bar whose own range is not the film\'s length is left alone; a percent bar (max 100) and a millisecond bar are handled', async () => {
  const cases = [['30', false], ['100', true], [String(LEN * 1000), true], [String(LEN), true], ['1', true]];
  for (const [mx, moves] of cases) {
    const page = await open(YT());
    await page.evaluate((m) => { document.getElementById('bar').setAttribute('aria-valuemax', m); }, mx);
    await tapBar(page, 0.7); await page.waitForTimeout(500);
    if (moves) assert.ok(near(await time(page), 0.7 * LEN, 1.5), 'max ' + mx + ': moved, at ' + await time(page));
    else assert.ok(near(await time(page), 0, 0.5), 'max ' + mx + ': left alone, at ' + await time(page));
    await page.context().close();
  }
});

test('YS1: another control lies over the bar at the finger (a button): the finger is on that, nothing is seeked', async () => {
  const page = await open(YT());
  await page.evaluate(() => {
    const b = document.createElement('button'); b.id = 'over'; b.textContent = 'Next'; b.style.cssText = 'position:absolute;left:300px;bottom:24px;width:70px;height:40px;z-index:5';
    document.getElementById('player').appendChild(b);
  });
  const r = await rectOf(page, '#over');
  await page.touchscreen.tap(r.left + r.width / 2, r.top + 8); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5), 'at ' + await time(page));
  await page.context().close();
});

test('YS1: the Settings switch is off (the app answers seek:false): nothing is seeked, the app is asked once for a while, a later tap after the pause asks again', async () => {
  const page = await open(YT(), { seek: false });
  await tapBar(page, 0.7); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5));
  await tapBar(page, 0.5); await page.waitForTimeout(400);
  assert.equal(await page.evaluate(() => window.__asked), 1, 'asked once');
  assert.ok(near(await time(page), 0, 0.5));
  const r = await report(page); assert.deepEqual({ f: r.f, o: r.o, r: r.r }, { f: 0, o: 1, r: 'off' });
  await page.waitForTimeout(700);                             // the 500 ms pause of the test clock
  await tapBar(page, 0.5); await page.waitForTimeout(400);
  assert.equal(await page.evaluate(() => window.__asked), 2, 'asked again after the pause');
  await page.context().close();
});

test('YS1: a long press on the bar (the mini panel\'s) and a vertical swipe that starts on it are not taps', async () => {
  const page = await open(YT());
  const r = await rectOf(page, '#bar'); const p = at(r, 0.7);
  await touch(page, p.x, p.y, { holdMs: 700 });               // tapMs is 400 in the test clock
  await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5), 'long press, at ' + await time(page));
  await touch(page, p.x, p.y, { toX: p.x + 3, toY: p.y - 90 });
  await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0, 0.5), 'vertical swipe, at ' + await time(page));
  await page.context().close();
});

// ---- sliding along the bar (pulling the red dot)

test('YS1: a slide along the bar that the page ignores seeks to the place where the finger lifted; one the page scrubs itself is left alone', async () => {
  let page = await open(YT());
  let r = await rectOf(page, '#bar'); let a = at(r, 0.2), b = at(r, 0.8);
  await touch(page, a.x, a.y, { toX: b.x, toY: b.y + 6 });
  await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0.8 * LEN, 2), 'slid to 80 percent, at ' + await time(page));
  await page.context().close();
  page = await open(YT());
  await page.evaluate(() => { document.addEventListener('pointerup', () => { document.getElementById('v').currentTime = 33; }, true); });
  r = await rectOf(page, '#bar'); a = at(r, 0.2); b = at(r, 0.8);
  await touch(page, a.x, a.y, { toX: b.x, toY: b.y });
  await page.waitForTimeout(600);
  assert.ok(near(await time(page), 33, 0.6), 'the page\'s scrub, at ' + await time(page));
  assert.equal(await page.evaluate(() => window.__seeks), 1);
  await page.context().close();
});

// ---- other players' markup

test('YS1: Video.js, Plyr, JW Player and a plain custom bar are recognised the same way', async () => {
  const wrap = (bar) => `<div id="player" style="position:relative;width:400px;height:240px">${VIDEO}${bar}</div>`;
  const cases = [
    ['Video.js', wrap('<div class="vjs-control-bar" style="position:absolute;left:0;right:0;bottom:0;height:30px"><div class="vjs-progress-control vjs-control" style="position:absolute;left:60px;right:60px;top:0;height:30px"><div id="bar" class="vjs-progress-holder vjs-slider vjs-slider-horizontal" role="slider" aria-valuemin="0" aria-valuemax="100" style="position:absolute;left:0;right:0;top:12px;height:6px;background:#555"></div></div></div>')],
    ['Plyr', wrap('<div class="plyr__controls" style="position:absolute;left:0;right:0;bottom:0;height:40px"><div class="plyr__progress" style="position:absolute;left:40px;right:40px;top:8px;height:24px"><input id="bar" type="range" min="0" max="100" step="0.01" value="0" data-plyr="seek" aria-label="Seek" style="width:100%;height:24px;margin:0"></div></div>')],
    ['JW Player', wrap('<div class="jw-controlbar" style="position:absolute;left:0;right:0;bottom:0;height:40px"><div id="bar" class="jw-slider-time jw-reset" role="slider" aria-label="Seek" style="position:absolute;left:40px;right:40px;top:12px;height:12px;background:#555"></div></div>')],
    ['a custom bar named seekbar', wrap('<div id="bar" class="my-seekbar" style="position:absolute;left:10px;right:10px;bottom:6px;height:8px;background:#555"></div>')],
  ];
  for (const [what, body] of cases) {
    const page = await open(body);
    await tapBar(page, 0.5); await page.waitForTimeout(500);
    assert.ok(near(await time(page), 0.5 * LEN, 2), what + ': moved, at ' + await time(page));
    await page.context().close();
  }
});

test('YS1: a right-to-left bar counts from the right', async () => {
  const page = await open(YT());
  await page.evaluate(() => { document.getElementById('bar').style.direction = 'rtl'; });
  await tapBar(page, 0.7); await page.waitForTimeout(500);
  assert.ok(near(await time(page), 0.3 * LEN, 1.5), 'at ' + await time(page));
  await page.context().close();
});

test('YS1: two quick taps: the last one wins', async () => {
  const page = await open(YT());
  await tapBar(page, 0.3); await tapBar(page, 0.8);
  await page.waitForTimeout(700);
  assert.ok(near(await time(page), 0.8 * LEN, 1.5), 'at ' + await time(page));
  await page.context().close();
});

test('YS1: a player inside a frame of another site is handled by the copy of the script that runs in that frame', async () => {
  const frameBody = YT();
  const page = await open('<iframe id="f" src="https://player.example/embed" style="position:absolute;left:20px;top:20px;width:400px;height:240px;border:0"></iframe>', { frameBody });
  const frame = page.frames().find((f) => f.url().startsWith('https://player.example'));
  await frame.waitForFunction(() => { const v = document.getElementById('v'); return v && v.readyState >= 3 && v.duration > 50; }, null, { timeout: 15000 });
  const fr = await (await page.$('#f')).boundingBox();
  const r = await frame.evaluate(() => { const b = document.getElementById('bar').getBoundingClientRect(); return { left: b.left, top: b.top, width: b.width, height: b.height }; });
  await page.touchscreen.tap(fr.x + r.left + r.width * 0.7, fr.y + r.top + r.height / 2);
  await page.waitForTimeout(600);
  const t = await frame.evaluate(() => document.getElementById('v').currentTime);
  assert.ok(near(t, 0.7 * LEN, 1.5), 'the frame\'s video at ' + t);
  await page.context().close();
});

// ---- a bar the script does not know is named in Diagnostics

test('YS1: a tap on a thin wide strip inside a player that the script did NOT take for a bar is reported with a harmless name (the next fix needs it), once a second at most; a tap high in the picture is not', async () => {
  const page = await open(PLAYER('<div class="mystery-strip-7" style="position:absolute;left:10px;right:10px;bottom:10px;height:12px;background:#555"></div>'));
  const r = await rectOf(page, '.mystery-strip-7');
  await page.touchscreen.tap(r.left + r.width * 0.5, r.top + 6); await page.waitForTimeout(300);
  await page.touchscreen.tap(r.left + r.width * 0.2, r.top + 6); await page.waitForTimeout(300);       // within a second of the first: not counted again
  await page.touchscreen.tap(200, 40); await page.waitForTimeout(300);                                  // high in the picture: not a bar place
  assert.ok(near(await time(page), 0, 0.5), 'nothing moved');
  await page.waitForTimeout(300);
  const all = await msgs(page, 'sbk');
  const u = all.reduce((a, m) => a + m.u, 0);
  assert.equal(u, 1);
  assert.equal(all.filter((m) => m.n).pop().n, 'div.mystery-strip-7');
  assert.equal(all.reduce((a, m) => a + m.t, 0), 0, 'it is not a counted tap on a bar');
  await page.context().close();
});

test('YS1: a recognised bar is never reported as unknown, and a name with odd characters is cut to harmless ones by the extension before it reaches the app (checked in the app relay by its code)', async () => {
  const page = await open(YT());
  await tapBar(page, 0.7); await page.waitForTimeout(500);
  const all = await msgs(page, 'sbk');
  assert.equal(all.reduce((a, m) => a + m.u, 0), 0);
  const bg = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'background.js'), 'utf8');
  assert.ok(bg.includes("n: String(m.n || '').replace(/[^A-Za-z0-9_.-]/g, '').slice(0, 40)"), 'the relay keeps letters, digits and - _ . only');
  await page.context().close();
});

// ---- cost and privacy

test('YS1: a page without a video does no work at a touch (no hit test at all), and one with a video does one hit test per touch', async () => {
  let page = await open('<div id="b" style="width:200px;height:200px;background:#eee">no video here</div>');
  await page.evaluate(() => { window.__hits = 0; const f = document.elementsFromPoint.bind(document); document.elementsFromPoint = (x, y) => { window.__hits++; return f(x, y); }; });
  await page.touchscreen.tap(50, 50); await page.touchscreen.tap(60, 60); await page.waitForTimeout(300);
  assert.equal(await page.evaluate(() => window.__hits), 0);
  await page.context().close();
  page = await open(YT());
  await page.evaluate(() => { window.__hits = 0; const f = document.elementsFromPoint.bind(document); document.elementsFromPoint = (x, y) => { window.__hits++; return f(x, y); }; });
  await page.touchscreen.tap(450, 400); await page.waitForTimeout(300);                 // far from the player
  assert.equal(await page.evaluate(() => window.__hits), 1);
  assert.ok(near(await time(page), 0, 0.5));
  await page.context().close();
});

test('YS1: the report holds numbers and a few words only - never an address - and the script never stops, delays or changes the page\'s own events', async () => {
  const page = await open(YT());
  await page.evaluate(() => {
    window.__ev = [];
    ['pointerdown', 'pointerup', 'click'].forEach((n) => document.addEventListener(n, (e) => { window.__ev.push(n + (e.defaultPrevented ? '!' : '')); }));
  });
  await tapBar(page, 0.7); await page.waitForTimeout(400);
  const all = await msgs(page, 'sbk');
  assert.ok(all.length >= 1);
  for (const m of all) { assert.deepEqual(Object.keys(m).sort(), ['f', 'k', 'n', 'o', 'r', 's', 't', 'u']); assert.ok(!/example|http|:\/\//.test(JSON.stringify(m)), JSON.stringify(m)); }
  const ev = await page.evaluate(() => window.__ev);
  assert.ok(ev.includes('pointerdown') && ev.includes('pointerup') && ev.includes('click'), 'the page still got every event: ' + ev);
  assert.ok(!ev.some((x) => x.endsWith('!')), 'none of them cancelled');
  await page.context().close();
});

test('YS1: the real clocks (450 ms wait) work too: a slow page that seeks 300 ms after the tap is not overruled, one that never does is', async () => {
  let page = await open(YT(), { slow: true });
  await page.evaluate(() => { document.getElementById('bar').addEventListener('click', () => setTimeout(() => { document.getElementById('v').currentTime = 20; }, 300)); });
  await tapBar(page, 0.7); await page.waitForTimeout(1000);
  assert.ok(near(await time(page), 20, 0.6), 'the slow page\'s place, at ' + await time(page));
  assert.equal(await page.evaluate(() => window.__seeks), 1);
  await page.context().close();
  page = await open(YT(), { slow: true });
  await tapBar(page, 0.7); await page.waitForTimeout(1000);
  assert.ok(near(await time(page), 0.7 * LEN, 1.5), 'at ' + await time(page));
  await page.context().close();
});
