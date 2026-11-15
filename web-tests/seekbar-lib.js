'use strict';
// YS1 (owner message 42, 2026-11-15; research/SEEKBAR.md): seekbar.js, the safety net that makes a tap on a video player's progress bar jump the video, against MOCK players in real headless Chromium with a
// touch screen. The mocks use markup read from the players' own public sources / known to be stable (YouTube: .ytp-progress-bar role=slider with aria-valuemax = the length; Video.js: .vjs-progress-holder;
// Plyr: input[type=range][data-plyr=seek]; JW Player: .jw-slider-time role=slider). A real YouTube page could not be had from the sandbox (bot-check page): what the real page does is unverified until the owner's
// Diagnostics line "Tap on the video bar" shows numbers.
// The extension API is a stub that answers the `guard` question like the app does ({seek: true} unless a test says otherwise) and records every message.
// Shared by seekbar.test.js and seekbar-real.test.js (two files so that each stays well under the runner's 60 s limit). Not a test file itself.
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SB = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'seekbar.js'), 'utf8');
let browser;
async function launch() { browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required', '--mute-audio'] }); }
async function close() { await browser.close(); }

/** An 8-bit mono silent WAV of [seconds]: a media element that really loads, has a length and seeks, with no codec. */
function wavBytes(seconds) {
  const n = 8000 * seconds;
  const h = Buffer.alloc(44);
  h.write('RIFF', 0); h.writeUInt32LE(36 + n, 4); h.write('WAVEfmt ', 8); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
  h.writeUInt32LE(8000, 24); h.writeUInt32LE(8000, 28); h.writeUInt16LE(1, 32); h.writeUInt16LE(8, 34); h.write('data', 36); h.writeUInt32LE(n, 40);
  return Buffer.concat([h, Buffer.alloc(n, 0x80)]);
}
const FILM = wavBytes(60);
const LOOP = wavBytes(1);
const LEN = 60;
const FAST = { waitMs: 120, flushMs: 80, tapMs: 400, reaskMs: 500, keepMs: 800, lateMs: 600 };   // the clocks of the script, shortened

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
    if (u.hostname === 'cdn.example' && u.pathname === '/loop.wav') return r.fulfill({ contentType: 'audio/wav', headers: { 'accept-ranges': 'bytes' }, body: LOOP });
    if (u.hostname === 'movie.example' && u.pathname === '/') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>movie</title></head><body style="margin:0">${body}</body></html>` });
    if (u.hostname === 'player.example') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><body style="margin:0">${o.frameBody || ''}</body></html>` });
    return r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>other</title>other' });
  });
  const page = await context.newPage();
  await page.addInitScript((cfg) => {
    window.__msgs = []; window.__asked = 0; window.__seeks = 0;
    window.__kstSBcfg = cfg.fast;
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); if (m && m.k === 'guard') { window.__asked++; const a = { on: true, top: location.hostname, skip: false, seek: cfg.seek !== false }; return cfg.delay ? new Promise((res) => setTimeout(() => res(a), cfg.delay)) : Promise.resolve(a); } return Promise.resolve(undefined); }, onMessage: { addListener() {} } } };
    document.addEventListener('seeking', () => { window.__seeks++; }, true);
  }, { fast: o.slow ? {} : Object.assign({}, FAST, o.fast || {}), seek: o.seek, delay: o.delay });
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


/** The REAL YouTube player bar as it is today (captured from the live embed page, build 20261114, by an independent reviewer): a custom element yt-progress-bar holding a div role=slider (the 44 px drag container,
 *  aria-valuemax 100) and, inside it, the 3 px rail made of four divs (Background at opacity .3, Loaded, Hovered = the light line up to the finger that the owner saw, Played) and the playhead dot. The page does
 *  NOT seek on a tap (it only paints the Hovered line): that is the owner's case. Sizes as on the tablet scaled to a 400 px player. */
const REAL_YT = (extra, played) => `<div id="player" class="html5-video-player ytp-exp-bottom-control-flexbox paused-mode" style="position:relative;width:400px;height:240px;background:#000;overflow:hidden">${VIDEO}`
  + `<div class="player-controls-content" style="position:absolute;left:0;top:0;width:400px;height:240px">`
  + `<yt-progress-bar class="ytPlayerProgressBarHost watch-page-progress-bar" style="display:block;position:absolute;left:20px;top:170px;width:360px;height:44px">`
  + `<div id="bar" class="ytPlayerProgressBarDragContainer" role="slider" aria-label="Seek slider" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0" tabindex="0" style="display:block;position:static;height:44px;touch-action:none">`
  + `<div class="ytPlayerProgressBarProgressBar" style="position:relative;height:0">`
  + `<yt-progress-bar-line class="ytProgressBarLineHost" style="display:block;position:absolute;left:0;top:21px;width:360px;height:3px"><div id="line" class="ytProgressBarLineProgressBarLine" style="position:relative;height:3px">`
  + `<div class="ytProgressBarLineProgressBarBackground" style="position:absolute;left:0;top:0;width:100%;height:3px;background:#fff;opacity:.3"></div>`
  + `<div class="ytProgressBarLineProgressBarLoaded" style="position:absolute;left:0;top:0;width:0;height:3px;background:#fff;opacity:.6"></div>`
  + `<div id="hov" class="ytProgressBarLineProgressBarHovered" style="position:absolute;left:0;top:0;width:0;height:3px;background:#fff;opacity:.7"></div>`
  + `<div class="ytProgressBarLineProgressBarPlayed" style="position:absolute;left:0;top:0;width:${played || 0}%;height:3px;background:#f03"></div></div></yt-progress-bar-line>`
  + `<yt-progress-bar-playhead class="ytProgressBarPlayheadHost" style="display:block;position:absolute;left:calc(${played || 0}% - 6px);top:16px;width:12px;height:12px"><div id="dot" class="ytProgressBarPlayheadProgressBarPlayheadDot" style="width:12px;height:12px;border-radius:6px;background:#f03"></div></yt-progress-bar-playhead>`
  + `</div></div></yt-progress-bar>${extra || ''}</div></div>`;

module.exports = { launch, close, open, SB, LEN, FAST, FILM, VIDEO, PLAYER, YT, REAL_YT, rectOf, at, time, msgs, tapBar, report, near, touch };
