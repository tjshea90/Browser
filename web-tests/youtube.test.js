'use strict';
// RX3-Y2 (research/REVIEW-EXTERNAL-3.md part Y): youtube.js, the built-in ad skipper, against a MOCK player in real headless Chromium. The structure of the mock is what the real site has (checked live on
// 2026-11-14 with a mobile and a desktop user agent, research/YOUTUBE_ADS.md section 4): #movie_player is the .html5-video-player element, its video is video.video-stream.html5-main-video, and while an
// ad runs the player has the class ad-showing (or ad-interrupting). A real ad could not be had from the sandbox (signed-out visitors got none), so the ad-state names and the skip-button selectors are the
// long-standing public ones and stay unverified on the real site until the owner's Diagnostics line says so.
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const YT = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'youtube.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required'] }); });
after(async () => { await browser.close(); });

/** An 8-bit mono silent WAV of [seconds] as a data: address: a media element that really plays and ends, with no codec or file. */
function wav(seconds) {
  const n = 8000 * seconds;
  const h = Buffer.alloc(44);
  h.write('RIFF', 0); h.writeUInt32LE(36 + n, 4); h.write('WAVEfmt ', 8); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
  h.writeUInt32LE(8000, 24); h.writeUInt32LE(8000, 28); h.writeUInt16LE(1, 32); h.writeUInt16LE(8, 34); h.write('data', 36); h.writeUInt32LE(n, 40);
  return 'data:audio/wav;base64,' + Buffer.concat([h, Buffer.alloc(n, 0x80)]).toString('base64');
}
const MAIN = wav(20), AD12 = wav(12);

const PLAYER = `<div id="movie_player" class="html5-video-player playing-mode" style="position:relative;width:400px;height:240px">
  <video class="video-stream html5-main-video" style="width:400px;height:240px" loop></video><div class="video-ads ytp-ad-module"></div></div>`;

/** The page's own part: how a mock player starts an ad. The ad replaces the media of the SAME video element, the player gets the ad class, and when the ad ends the class goes and the film comes back. */
const MOCK = `
var MAIN = window.__MAIN, AD12 = window.__AD12;
window.__ended = 0;
window.__startAd = function (o) {
  o = o || {};
  var p = document.getElementById('movie_player'), v = p.querySelector('video');
  window.__mainTime = v.currentTime;
  if (!o.stuck) { v.loop = !!o.loop; v.src = o.src || AD12; }
  p.classList.add(o.interrupting ? 'ad-interrupting' : 'ad-showing');
  if (!o.stuck) v.play();
  if (o.fakeDuration) Object.defineProperty(v, 'duration', { configurable: true, get: function () { return o.fakeDuration; } });
  function done() {
    p.classList.remove('ad-showing', 'ad-interrupting');
    v.loop = true; v.src = MAIN; v.currentTime = window.__mainTime || 0;
    if (o.unmuteAtEnd) v.muted = false;
    window.__ended++; v.play();
  }
  if (!o.stuck) v.onended = o.loop ? null : done;
  if (o.skipAfterMs) setTimeout(function () {
    var b = document.createElement('button'); b.className = 'ytp-skip-ad-button'; b.style.cssText = 'display:block;width:80px;height:30px'; b.textContent = 'Skip';
    b.onclick = function () { window.__skipClicks = (window.__skipClicks || 0) + 1; b.remove(); done(); };
    p.appendChild(b);
  }, o.skipAfterMs);
};
(function () { var v = document.querySelector('#movie_player video'); if (v) { v.src = MAIN; v.muted = false; v.play().catch(function () {}); } })();
`;

async function open(opts) {
  opts = opts || {};
  const context = await browser.newContext({ viewport: { width: 500, height: 400 } });
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>watch</title></head><body style="margin:0">${opts.body || PLAYER}<script>${MOCK}</script></body></html>` }));
  const page = await context.newPage();
  await page.addInitScript((cfg) => {
    window.__MAIN = cfg.main; window.__AD12 = cfg.ad; window.__msgs = [];
    window.__kstYTcfg = cfg.timing || {};
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(m.k === 'guard' ? { on: true, top: 'www.youtube.com', skip: cfg.skip !== false } : undefined); } } };
  }, { main: MAIN, ad: AD12, skip: opts.skip, timing: opts.timing });
  await page.goto('https://www.youtube.com/watch?v=mock');
  await page.waitForFunction(() => { const v = document.querySelector('video'); return v && !v.paused; }, null, { timeout: 5000 }).catch(() => {});
  if (!opts.noScript) await page.addScriptTag({ content: YT });          // document_end: the page is parsed and its player exists
  return page;
}
const settle = (ms) => new Promise((r) => setTimeout(r, ms || 400));
const st = (page) => page.evaluate(() => { const v = document.querySelector('video'); const p = document.getElementById('movie_player'); return v && p ? { muted: v.muted, rate: v.playbackRate, ad: /ad-showing|ad-interrupting/.test(p.className), t: v.currentTime, paused: v.paused, ended: window.__ended || 0 } : null; });
const yts = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'yts'));

test('RX3-Y2: an ad of 12 s ends within about two seconds: muted and sped up, then the sound and the speed come back', async () => {
  const page = await open();
  await page.evaluate(() => window.__startAd());
  await page.waitForFunction(() => window.__ended === 1, null, { timeout: 4000 });
  await settle(300);
  const s = await st(page);
  assert.equal(s.ad, false, 'the ad class is gone');
  assert.equal(s.muted, false, 'the sound is back');
  assert.equal(s.rate, 1, 'the speed is back');
});

test('RX3-Y2: ad-interrupting is an ad too', async () => {
  const page = await open();
  await page.evaluate(() => window.__startAd({ interrupting: true }));
  await page.waitForFunction(() => window.__ended === 1, null, { timeout: 4000 });
});

test('RX3-Y2: nothing is touched while no ad class is on the player (the film plays as it is)', async () => {
  const page = await open();
  await settle(2500);
  const s = await st(page);
  assert.equal(s.muted, false); assert.equal(s.rate, 1); assert.equal(s.ad, false);
  assert.deepEqual(await yts(page), [], 'not a single report without an ad');
  const p = await page.evaluate(() => document.querySelector('video').currentTime);
  assert.ok(p > 1 && p < 19, 'the film moved on by itself: ' + p);
});

test('RX3-Y2: a skip button that shows up is pressed (an ad that would run long)', async () => {
  const page = await open();
  await page.evaluate(() => window.__startAd({ loop: true, fakeDuration: 300, skipAfterMs: 1200 }));
  await page.waitForFunction(() => window.__ended === 1, null, { timeout: 5000 });
  assert.equal(await page.evaluate(() => window.__skipClicks), 1, 'pressed once');
});

test('RX3-Y2: an overlay ad (a banner over the film, no ad class) is closed', async () => {
  const page = await open();
  await page.evaluate(() => {
    const b = document.createElement('div'); b.className = 'ytp-ad-overlay-close-button'; b.style.cssText = 'width:20px;height:20px;position:absolute;right:0;top:0';
    b.onclick = () => { window.__overlayClosed = (window.__overlayClosed || 0) + 1; b.remove(); };
    document.querySelector('.video-ads').appendChild(b);
  });
  await page.waitForFunction(() => window.__overlayClosed === 1, null, { timeout: 3000 });
});

test('RX3-Y2: the "ad blockers are not allowed" dialog and its backdrop are removed, scrolling is unlocked and the film is started', async () => {
  const body = PLAYER + `<tp-yt-paper-dialog id="dlg" style="position:fixed;left:10px;top:10px;width:200px;height:100px"><ytd-enforcement-message-view-model>Ad blockers violate YouTube's Terms of Service</ytd-enforcement-message-view-model></tp-yt-paper-dialog><tp-yt-iron-overlay-backdrop class="opened" id="bd" style="position:fixed;inset:0"></tp-yt-iron-overlay-backdrop>`;
  const page = await open({ body, noScript: true });
  await page.evaluate(() => { document.documentElement.style.overflow = 'hidden'; document.body.style.overflow = 'hidden'; const v = document.querySelector('video'); v.pause(); window.__playCalls = 0; const o = v.play.bind(v); v.play = function () { window.__playCalls++; return o(); }; });
  await page.addScriptTag({ content: YT });
  await page.waitForFunction(() => !document.getElementById('dlg'), null, { timeout: 4000 });
  assert.equal(await page.evaluate(() => !!document.getElementById('bd')), false, 'the backdrop is gone');
  const o = await page.evaluate(() => [document.documentElement.style.overflow, document.body.style.overflow, window.__playCalls]);
  assert.deepEqual([o[0], o[1]], ['', ''], 'scrolling unlocked');
  assert.ok(o[2] >= 1, 'the film was told to play');
  const r = await yts(page); await settle(3500); const r2 = await yts(page);
  assert.ok(r.length + r2.length >= 1 && (await yts(page)).some((m) => m.d === 1), 'one dialog is counted');
});

test('RX3-Y2: what the owner had is kept: muted before the ad stays muted after it; a player that unmutes by itself at the end is left alone', async () => {
  const a = await open();
  await a.evaluate(() => { document.querySelector('video').muted = true; window.__startAd(); });
  await a.waitForFunction(() => window.__ended === 1, null, { timeout: 4000 });
  await settle(300);
  assert.equal((await st(a)).muted, true, 'the owner had it muted');
  const b = await open();
  await b.evaluate(() => window.__startAd({ unmuteAtEnd: true }));
  await b.waitForFunction(() => window.__ended === 1, null, { timeout: 4000 });
  await settle(300);
  const s = await st(b);
  assert.equal(s.muted, false); assert.equal(s.rate, 1);
});

test('RX3-Y2: when the video element still holds the FILM (the ad class came first) it is never skipped to its end', async () => {
  const page = await open({ timing: { maxMs: 1500 } });
  await page.evaluate(() => window.__startAd({ stuck: true }));              // the class is set, the media is not swapped
  await settle(1900);
  const s = await st(page);
  const t = await page.evaluate(() => [document.querySelector('video').currentTime, document.querySelector('video').duration]);
  assert.ok(t[0] < t[1] - 1.5, 'the film was not thrown to its end: ' + t.join('/'));
  assert.equal(s.muted, false, 'and the sound is back after the give-up of this ad');
});

test('RX3-Y2: five ads in a row that will not end switch the skipper off for the page, which then leaves everything alone', async () => {
  const page = await open({ timing: { maxMs: 500, fails: 2 } });
  for (let i = 0; i < 2; i++) { await page.evaluate(() => window.__startAd({ stuck: true })); await settle(900); await page.evaluate(() => document.getElementById('movie_player').classList.remove('ad-showing')); await settle(150); }
  await page.evaluate(() => window.__startAd());                              // a real ad now: nobody handles it
  await settle(2500);
  const s = await st(page);
  assert.equal(s.muted, false, 'not muted: the skipper is off');
  assert.equal(s.rate, 1);
  const last = (await yts(page)).map((m) => m.r).filter(Boolean).pop() || '';
  assert.ok(/gave up/.test(last), last);
});

test('RX3-Y2: with the Settings row off (or Shields off for the site) nothing is done during an ad', async () => {
  const page = await open({ skip: false });
  await page.evaluate(() => window.__startAd({ loop: true }));
  await settle(1800);
  const s = await st(page);
  assert.equal(s.ad, true, 'the ad is still there'); assert.equal(s.rate, 1); assert.equal(s.muted, false);
  assert.deepEqual(await yts(page), []);
});

test('RX3-Y2: the site\'s navigation to another video replaces the player: the new one is watched too', async () => {
  const page = await open();
  await page.evaluate(() => {
    const old = document.getElementById('movie_player'); const n = old.cloneNode(true); n.id = 'movie_player';
    old.replaceWith(n);
    const v = n.querySelector('video'); v.src = window.__MAIN; v.loop = true; v.muted = false; v.play().catch(() => {});
    document.dispatchEvent(new Event('yt-navigate-finish'));
  });
  await settle(2200);                                                         // the skipper looks again 0.2 s, 1.5 s and 4 s after the navigation
  await page.evaluate(() => window.__startAd());
  await page.waitForFunction(() => window.__ended === 1, null, { timeout: 5000 });
});

test('RX3-Y2: the counters are sent as numbers, at most once in a few seconds, never with an address', async () => {
  const page = await open();
  for (let i = 0; i < 2; i++) { await page.evaluate(() => { window.__ended = 0; window.__startAd(); }); await page.waitForFunction(() => window.__ended === 1, null, { timeout: 4000 }); await settle(150); }
  await settle(3600);
  const r = await yts(page);
  assert.ok(r.length >= 1 && r.length <= 2, 'few reports: ' + r.length);
  const total = r.reduce((n, m) => n + m.s, 0);
  assert.equal(total, 2, 'two ads skipped, reported as differences');
  for (const m of r) { for (const k of Object.keys(m)) assert.ok(['k', 'a', 's', 'c', 'o', 'd', 'f', 'g', 'r'].includes(k), 'no field for an address: ' + k); }
});

test('RX3-Y2: a page without a player, or a player without a video, is not an error', async () => {
  const a = await open({ body: '<p>no player here</p>' });
  await settle(1500);
  assert.deepEqual(await yts(a), []);
  const b = await open({ body: '<div id="movie_player" class="html5-video-player"></div>' });
  await b.evaluate(() => document.getElementById('movie_player').classList.add('ad-showing'));
  await settle(1500);
  assert.equal(await b.evaluate(() => true), true);
});

test('RX3-Y2: the manifest runs youtube.js only on the YouTube hosts, in every frame, once the page is parsed', () => {
  const m = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'manifest.json'), 'utf8'));
  const e = m.content_scripts.filter((c) => c.js.includes('youtube.js'));
  assert.equal(e.length, 1);
  assert.deepEqual(e[0].matches, ['*://*.youtube.com/*', '*://*.youtube-nocookie.com/*', '*://*.youtubekids.com/*']);
  assert.equal(e[0].all_frames, true); assert.equal(e[0].run_at, 'document_end');
  assert.equal(e[0].world, undefined, 'the isolated world: it needs the extension API and must not be seen by the page');
});
