'use strict';
// VC1 / LP1 (owner message 44, research/VIDEO_CONTROLS.md): helpers for videobar.test.js. vfind.js + videobar.js (+ content.js for the long-press cases) run in real headless Chromium with a touch screen against
// a fake extension API: it answers the `guard` question like the app does (Settings "Video controls": Automatic / Always / Off), plays the part of the background page for frames (`ft` -> the top frame's
// `ftq` listener) and records every message. A media element that really loads, has a length and seeks, with no codec: a silent WAV (as in seekbar-lib.js).
// Not a test file itself.
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const DIR = path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const read = (f) => fs.readFileSync(path.join(DIR, f), 'utf8');
const VFIND = read('vfind.js'), VBAR = read('videobar.js'), CONTENT = read('content.js'), TURN = read('turn.js');

let browser;
async function launch() { browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required', '--mute-audio'] }); }
async function close() { await browser.close(); }

function wav(seconds) {
  const n = 8000 * seconds, h = Buffer.alloc(44);
  h.write('RIFF', 0); h.writeUInt32LE(36 + n, 4); h.write('WAVEfmt ', 8); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
  h.writeUInt32LE(8000, 24); h.writeUInt32LE(8000, 28); h.writeUInt16LE(1, 32); h.writeUInt16LE(8, 34); h.write('data', 36); h.writeUInt32LE(n, 40);
  return Buffer.concat([h, Buffer.alloc(n, 0x80)]);
}
const FILM = wav(60);
const LEN = 60;
const VIDEO = (extra) => `<video id="v" src="https://cdn.example/film.wav" preload="auto" muted ${extra || ''} style="position:absolute;left:20px;top:100px;width:400px;height:240px;background:#222"></video>`;

/**
 * Opens https://movie.example/ with [body]. o.cfg = what the app answers ({ vb: false } = Off, { vba: true } = Always), o.content = also run content.js (long press), o.wait = no wait for the film.
 */
async function open(body, o) {
  o = o || {};
  const context = await browser.newContext({ hasTouch: true, viewport: { width: 500, height: 800 } });
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.hostname === 'cdn.example' && u.pathname === '/film.wav') {
      const rg = /bytes=(\d+)-(\d*)/.exec(r.request().headers().range || '');
      if (!rg) return r.fulfill({ contentType: 'audio/wav', headers: { 'accept-ranges': 'bytes' }, body: FILM });
      const a = +rg[1], b = rg[2] ? Math.min(+rg[2], FILM.length - 1) : FILM.length - 1;
      return r.fulfill({ status: 206, contentType: 'audio/wav', headers: { 'accept-ranges': 'bytes', 'content-range': `bytes ${a}-${b}/${FILM.length}` }, body: FILM.subarray(a, b + 1) });
    }
    return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>movie</title></head><body style="margin:0;height:1600px">${body}</body></html>` });
  });
  const page = await context.newPage();
  await page.addInitScript((cfg) => {
    window.__msgs = [];
    if (window.top === window) { window.__all = []; window.__guardFails = 0; window.__ftDelay = 0; }
    window.__kstVBcfg = { judge: 80, late: 500, hide: 1100, hidePaused: 1700, drag: 20 };      // (tap stays the app's 450 ms)
    window.__kstTurnCfg = { settle: 300, cap: 1500, tick: 60, nudge: 40 };                       // FB2: the turn guard's clocks, shortened
    window.browser = {
      runtime: {
        sendMessage: (m) => {
          window.__msgs.push(m);
          try { window.top.__all.push(m); } catch (e) { /* another origin */ }
          if (m && m.k === 'guard') {
            if ((window.top.__guardFails || 0) > 0) { window.top.__guardFails--; return Promise.reject(new Error('no answer')); }
            return Promise.resolve({ on: true, top: location.hostname, skip: false, seek: true, vb: cfg.vb !== false, vba: !!cfg.vba });
          }
          if (m && m.k === 'ft') {                      // the background page: ask the top frame, answer {video, src} (http addresses only, like background.js)
            try {
              const p = window.top.__ftq({ k: 'ftq', t: m.t, w: m.w, h: m.h }).then((r) => ({ video: !!(r && r.video), src: (r && typeof r.src === 'string' && /^https?:\/\//i.test(r.src)) ? r.src : '' }));
              const d = window.top.__ftDelay || 0;
              return d ? new Promise((res) => setTimeout(res, d)).then(() => p) : p;
            } catch (e) { return Promise.resolve({ video: false, src: '' }); }
          }
          return Promise.resolve(undefined);
        },
        onMessage: { addListener: (fn) => { window.__ftq = (m) => Promise.resolve(fn(m)); } },
      },
    };
  }, o.cfg || {});
  await page.addInitScript({ content: VFIND });
  if (o.content) await page.addInitScript({ content: CONTENT });
  await page.addInitScript({ content: VBAR });
  if (o.turn) await page.addInitScript({ content: TURN });
  await page.goto('https://movie.example/');
  if (o.wait !== false) await page.waitForFunction(() => { const v = document.getElementById('v') || (window.__deepV && window.__deepV()); return v && v.readyState >= 3 && v.duration > 50; }, null, { timeout: 15000 });
  return page;
}

const msgs = (page, k) => page.evaluate((kk) => window.__msgs.filter((m) => m && m.k === kk), k);
/** Every message of every frame of the page (the background page sees all of them). */
const msgsAll = (page, k) => page.evaluate((kk) => window.__all.filter((m) => m && m.k === kk), k);
/** The bar's state as the test can see it (its shadow tree is closed): null = none, else { left, top, width, height, playing, muted, text }. */
const barInfo = (page) => page.evaluate(() => {
  const b = window.KestrelVB && window.KestrelVB.dbg();
  if (!b) return null;
  const r = b.wrap.getBoundingClientRect(), t = b.track.getBoundingClientRect();
  return { left: r.left, top: r.top, width: r.width, height: r.height, track: { left: t.left, top: t.top, width: t.width, height: t.height }, paused: b.v.paused, muted: b.v.muted, time: b.v.currentTime, cur: b.cur.textContent, dur: b.dur.textContent, host: b.host.parentNode === document.documentElement ? 'html' : 'other' };
});
const tapAt = async (page, x, y) => { await page.touchscreen.tap(x, y); };
/** Click a part of the bar by name (the bar's buttons are inside a closed shadow root: the test asks the script for the element's centre). */
const partCentre = (page, name) => page.evaluate((n) => { const b = window.KestrelVB.dbg(); const el = n === 'play' ? b.playB.el : n === 'mute' ? b.muteB.el : n === 'full' ? b.fullB.el : b.track; const r = el.getBoundingClientRect(); return { x: r.left + r.width / 2, y: r.top + r.height / 2, left: r.left, width: r.width }; }, name);
const filmTime = (page) => page.evaluate(() => (document.getElementById('v') || window.__deepV()).currentTime);

/** A touch that stays where it is for [ms], or slides to (toX, toY): raw touch events through the debugger protocol. */
async function touch(page, x, y, o) {
  o = o || {};
  const c = await page.context().newCDPSession(page);
  await c.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x, y }] });
  if (o.toX !== undefined) { for (let i = 1; i <= 6; i++) { await c.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: x + (o.toX - x) * i / 6, y: y + ((o.toY === undefined ? y : o.toY) - y) * i / 6 }] }); await page.waitForTimeout(40); } }
  if (o.holdMs) await page.waitForTimeout(o.holdMs);
  await c.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
}

module.exports = { launch, close, open, msgs, msgsAll, barInfo, tapAt, partCentre, filmTime, touch, VIDEO, LEN, CONTENT };
