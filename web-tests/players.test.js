'use strict';
// RX3-G7 (research/REVIEW-EXTERNAL-3.md part G, research/YOUTUBE_ADS.md section 6): players.js, the ad skipper for video players other than YouTube, against MOCK players in real headless Chromium.
// The mocks use the class names READ IN THE PLAYERS' OWN PUBLIC SOURCE on 2026-11-14 (not remembered): Video.js + videojs-contrib-ads 7.5.2 puts vjs-ad-playing on the player root while an ad plays (vjs-ad-loading while it
// waits, vjs-ad-content-resuming while the film comes back); JW Player 8.33 puts jw-flag-ads on its root and builds its skip button as .jw-skip, which gets jw-skippable when skipping is allowed; Google IMA (videojs-ima 2.3.0) and
// Plyr 3.7.8 give the ad its own container, ima-ad-container / plyr__ads, and the ad's video lives inside it. A real ad could not be had from the sandbox: what a real ad does stays unverified until Diagnostics shows it.
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const PL = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'players.js'), 'utf8');
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
const FAST = { loopMs: 40, flushMs: 100 };   // the clocks of the script, shortened

// the player families whose ad state is a class on the player root
const FAMILY = {
  videojs: {
    html: '<div id="player" class="video-js vjs-default-skin vjs-big-play-centered" style="position:relative;width:400px;height:240px"><video class="vjs-tech" style="width:400px;height:240px"></video></div>',
    before: ['vjs-ad-loading'], during: ['vjs-ad-playing'], after: ['vjs-ad-content-resuming'],
  },
  jw: {
    html: '<div id="player" class="jwplayer jw-reset jw-skin-seven jw-state-playing" style="position:relative;width:400px;height:240px"><div class="jw-media jw-reset"><video class="jw-video jw-reset" style="width:400px;height:240px"></video></div></div>',
    before: [], during: ['jw-flag-ads', 'jw-flag-ads-hide-controls'], after: [],
  },
};
// the families where the ad's own video sits inside the ad container, beside the film's video
const HOSTED = (cls) => `<div id="wrap" style="position:relative;width:400px;height:240px"><video id="content" style="width:400px;height:240px" loop></video>`
  + `<div class="${cls}" style="position:absolute;left:0;top:0;width:400px;height:240px"><video id="adv" style="width:400px;height:240px"></video></div></div>`;

/** The page's own part: how a mock player starts and ends an ad. The ad replaces the media of the SAME video element (state families) or plays in its own element inside the container (hosted families). */
const MOCK = `
var MAIN = window.__MAIN, AD = window.__AD, FAM = window.__FAM || {};
window.__ended = 0; window.__skipClicks = 0;
function root() { return document.getElementById('player'); }
function vid() { return root().querySelector('video'); }
window.__startAd = function (o) {
  o = o || {};
  var r = root(), v = vid();
  window.__mainTime = v.currentTime;
  FAM.before.forEach(function (c) { r.classList.add(c); });
  function done() {
    FAM.during.forEach(function (c) { r.classList.remove(c); });
    FAM.after.forEach(function (c) { r.classList.add(c); });
    v.loop = true; v.src = MAIN; v.currentTime = window.__mainTime || 0;
    if (o.unmuteAtEnd) v.muted = false;
    window.__ended++; v.play();
  }
  function go() {
    FAM.before.forEach(function (c) { r.classList.remove(c); });
    FAM.during.forEach(function (c) { r.classList.add(c); });
    if (o.stuckAd) { v.loop = true; v.src = o.src || AD; v.play(); }
    else if (!o.filmStays) { v.loop = false; v.src = o.src || AD; v.play(); v.onended = done; }
    if (o.fakeDuration) Object.defineProperty(v, 'duration', { configurable: true, get: function () { return o.fakeDuration; } });
    if (o.skipAfterMs) setTimeout(function () {
      var b = document.createElement('div'); b.className = 'jw-skip jw-reset'; b.style.cssText = 'position:absolute;right:4px;bottom:4px;width:80px;height:30px;background:#000';
      b.addEventListener('click', function () { if (!b.classList.contains('jw-skippable')) return; window.__skipClicks++; b.remove(); done(); });
      r.appendChild(b);
      setTimeout(function () { b.classList.add('jw-skippable'); }, o.skippableAfterMs || 300);
    }, o.skipAfterMs);
  }
  setTimeout(go, FAM.before.length ? 150 : 0);
};
window.__startHosted = function () {
  var a = document.getElementById('adv');
  a.src = AD; a.onended = function () { window.__ended++; }; a.play();
};
(function () { var v = document.querySelector('#player video, #content'); if (v) { v.src = MAIN; v.loop = true; v.muted = false; v.play().catch(function () {}); } })();
`;

async function open(opts) {
  opts = opts || {};
  const html = opts.html || FAMILY.videojs.html;
  const context = await browser.newContext({ viewport: { width: 500, height: 400 } });
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>article</title></head><body style="margin:0">${html}<script>${MOCK}</script></body></html>` }));
  const page = await context.newPage();
  await page.addInitScript((cfg) => {
    window.__MAIN = cfg.main; window.__AD = cfg.ad; window.__FAM = cfg.fam; window.__msgs = [];
    window.__kstPLcfg = cfg.timing || {};
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(m.k === 'guard' ? { on: true, top: location.hostname, skip: cfg.skip !== false } : undefined); } } };
  }, { main: MAIN, ad: AD12, fam: opts.fam || FAMILY.videojs, skip: opts.skip, timing: opts.timing });
  await page.addInitScript({ content: PL });                                  // document_start, like the manifest: before the page's own scripts
  await page.goto(opts.url || 'https://news.example/article');
  await page.waitForFunction(() => { const v = document.querySelector('#player video, #content'); return v && !v.paused && v.currentTime > 0.1; }, null, { timeout: 8000 }).catch(() => {});
  return page;
}
async function waitFor(page, fn, ms) { await page.waitForFunction(fn, null, { timeout: ms || 6000 }); }
const sum = (msgs, k) => msgs.filter((m) => m.k === 'yts').reduce((n, m) => n + (m[k] || 0), 0);
const state = (page) => page.evaluate(() => { const v = document.querySelector('#player video, #content'); return { muted: v.muted, rate: v.playbackRate, t: v.currentTime, paused: v.paused, msgs: window.__msgs, ended: window.__ended, clicks: window.__skipClicks }; });

test('Video.js: an ad is muted, sped up and moved to its end, then the film comes back as it was', async () => {
  const page = await open({ fam: FAMILY.videojs, timing: FAST });
  await page.waitForTimeout(500);
  const before = await state(page);
  assert.equal(before.msgs.length, 0, 'nothing is sent before an ad shows: ' + JSON.stringify(before.msgs));
  await page.evaluate(() => window.__startAd());
  await waitFor(page, () => window.__ended === 1);
  await page.waitForTimeout(700);
  const s = await state(page);
  assert.equal(s.muted, false, 'the film is not left muted');
  assert.equal(s.rate, 1, 'the film is not left sped up');
  assert.ok(s.t < before.t + 4, 'the film itself was not moved: ' + before.t + ' -> ' + s.t);
  const yts = s.msgs.filter((m) => m.k === 'yts');
  assert.ok(yts.length >= 1);
  assert.equal(sum(s.msgs, 'a'), 1, 'one ad seen'); assert.equal(sum(s.msgs, 's'), 1, 'one ad skipped'); assert.equal(sum(s.msgs, 'f'), 1, 'moved to its end once');
  assert.ok(yts.every((m) => m.w === 'p'), 'the reports say they come from another player, not YouTube');
  assert.equal(s.msgs.filter((m) => m.k === 'guard').length, 1, 'the app is asked once');
  await page.context().close();
});

test('Video.js: vjs-ad-loading alone (waiting for an ad) and the resuming class change nothing', async () => {
  const page = await open({ fam: FAMILY.videojs, timing: FAST });
  await page.evaluate(() => { const r = document.getElementById('player'); r.classList.add('vjs-ad-loading'); });
  await page.waitForTimeout(900);
  await page.evaluate(() => { const r = document.getElementById('player'); r.classList.remove('vjs-ad-loading'); r.classList.add('vjs-ad-content-resuming'); });
  await page.waitForTimeout(500);
  const s = await state(page);
  assert.equal(s.muted, false); assert.equal(s.rate, 1); assert.equal(s.msgs.length, 0, 'not even the app was asked: ' + JSON.stringify(s.msgs));
  await page.context().close();
});

test('JW Player: the skip button is pressed once it says skippable, not before', async () => {
  const page = await open({ fam: FAMILY.jw, timing: FAST });
  await page.evaluate(() => window.__startAd({ stuckAd: true, fakeDuration: 400, skipAfterMs: 100, skippableAfterMs: 700 }));
  await page.waitForTimeout(450);
  assert.equal((await state(page)).clicks, 0, 'not pressed while it is not skippable');
  await waitFor(page, () => window.__skipClicks === 1 && window.__ended === 1);
  await page.waitForTimeout(500);
  const s = await state(page);
  assert.equal(s.clicks, 1);
  assert.equal(sum(s.msgs, 'c'), 1, 'one skip button pressed'); assert.equal(sum(s.msgs, 's'), 1);
  assert.equal(sum(s.msgs, 'f'), 0, 'a 400 s stream is not moved: it could be the film');
  await page.waitForTimeout(300);
  assert.equal((await state(page)).muted, false);
  await page.context().close();
});

test('JW Player: an ad stream is moved to its end', async () => {
  const page = await open({ fam: FAMILY.jw, timing: FAST });
  await page.evaluate(() => window.__startAd());
  await waitFor(page, () => window.__ended === 1);
  await page.waitForTimeout(500);
  const s = await state(page);
  assert.equal(sum(s.msgs, 'f'), 1); assert.equal(sum(s.msgs, 's'), 1);
  await page.context().close();
});

for (const [name, cls] of [['Google IMA container', 'ima-ad-container'], ['Plyr ads container', 'plyr__ads']]) {
  test(name + ': the ad video inside it is sped through, the film video beside it is never touched', async () => {
    const page = await open({ html: HOSTED(cls), timing: FAST });
    await page.waitForTimeout(400);
    await page.evaluate(() => window.__startHosted());
    await waitFor(page, () => window.__ended === 1);
    await page.waitForTimeout(400);
    const r = await page.evaluate(() => { const c = document.getElementById('content'), a = document.getElementById('adv'); return { contentMuted: c.muted, contentRate: c.playbackRate, contentPlaying: !c.paused, adRate: a.playbackRate, adMuted: a.muted, msgs: window.__msgs }; });
    assert.equal(r.contentMuted, false); assert.equal(r.contentRate, 1); assert.equal(r.contentPlaying, true, 'the film keeps playing');
    assert.equal(sum(r.msgs, 'a'), 1); assert.equal(sum(r.msgs, 'f'), 1); assert.equal(sum(r.msgs, 's'), 1);
    assert.equal(r.adRate, 1, 'what we changed on the ad element is put back');
    await page.context().close();
  });
}

test('an ordinary page video (no ad words anywhere) is left alone and nothing is sent', async () => {
  const page = await open({ html: '<div class="post video-wrapper"><div class="inner"><video id="content" style="width:400px;height:240px" loop></video></div></div><div id="player"></div>', timing: FAST });
  await page.waitForTimeout(1200);
  const r = await page.evaluate(() => { const c = document.getElementById('content'); return { muted: c.muted, rate: c.playbackRate, playing: !c.paused, msgs: window.__msgs }; });
  assert.equal(r.playing, true); assert.equal(r.muted, false); assert.equal(r.rate, 1); assert.deepEqual(r.msgs, []);
  await page.context().close();
});

test('the Settings switch off: the app is asked, and nothing at all is done', async () => {
  const page = await open({ fam: FAMILY.jw, skip: false, timing: FAST });
  await page.evaluate(() => window.__startAd({ stuckAd: true, fakeDuration: 400 }));
  await page.waitForTimeout(900);
  const s = await state(page);
  assert.equal(s.muted, false); assert.equal(s.rate, 1);
  assert.equal(s.msgs.filter((m) => m.k === 'yts').length, 0);
  assert.equal(s.msgs.filter((m) => m.k === 'guard').length, 1);
  await page.context().close();
});

test('on the YouTube hosts this script does nothing (youtube.js owns them)', async () => {
  const page = await open({ fam: FAMILY.jw, url: 'https://www.youtube.com/watch?v=mock', timing: FAST });
  await page.evaluate(() => window.__startAd({ stuckAd: true, fakeDuration: 400 }));
  await page.waitForTimeout(900);
  const s = await state(page);
  assert.equal(s.muted, false); assert.equal(s.rate, 1); assert.deepEqual(s.msgs, []);
  await page.context().close();
});

test('while the stream is still the film (the ad class came first) the film is not moved', async () => {
  const page = await open({ fam: FAMILY.jw, timing: FAST });
  await page.waitForTimeout(500);
  const t0 = (await state(page)).t;
  await page.evaluate(() => window.__startAd({ filmStays: true }));
  await page.waitForTimeout(1200);
  const s = await state(page);
  assert.ok(s.t < t0 + 3, 'the film was not thrown to its end: ' + t0 + ' -> ' + s.t);
  assert.equal(sum(s.msgs, 'f'), 0); assert.equal(s.muted, false, 'not muted either'); assert.equal(s.rate, 1, 'not sped up either');
  await page.context().close();
});

test('an ad that will not end is given up after the time limit, the film is put back, and repeated ones switch it off', async () => {
  const page = await open({ fam: FAMILY.jw, timing: Object.assign({}, FAST, { maxMs: 350, fails: 2 }) });
  for (let i = 0; i < 2; i++) {
    await page.evaluate(() => window.__startAd({ stuckAd: true, fakeDuration: 400 }));
    await page.waitForTimeout(700);
    await page.evaluate(() => { const r = document.getElementById('player'); r.classList.remove('jw-flag-ads', 'jw-flag-ads-hide-controls'); });
    await page.waitForTimeout(250);
  }
  await page.waitForTimeout(600);
  const s = await state(page);
  assert.equal(s.muted, false); assert.equal(s.rate, 1);
  assert.ok(sum(s.msgs, 'g') >= 2, 'given up on twice: ' + JSON.stringify(s.msgs.filter((m) => m.k === 'yts')));
  assert.ok(s.msgs.some((m) => m.k === 'yts' && /gave up/.test(m.r || '')), 'the reason says it switched itself off');
  await page.context().close();
});

test('what the page itself changed while the ad ran is left as the page has it', async () => {
  const page = await open({ fam: FAMILY.jw, timing: FAST });
  await page.evaluate(() => window.__startAd({ unmuteAtEnd: true }));
  await waitFor(page, () => window.__ended === 1);
  await page.waitForTimeout(300);
  assert.equal((await state(page)).muted, false);
  await page.context().close();
});

test('the reports carry numbers and a few words, never an address', async () => {
  const page = await open({ fam: FAMILY.videojs, timing: FAST });
  await page.evaluate(() => window.__startAd());
  await waitFor(page, () => window.__ended === 1);
  await page.waitForTimeout(500);
  const msgs = (await state(page)).msgs;
  const text = JSON.stringify(msgs.filter((x) => x.k === 'yts'));   // the guard question carries the page's host name, as it always has; the reports must not
  assert.ok(!/https?:|example|data:/i.test(text), 'no address in: ' + text);
  for (const m of msgs.filter((x) => x.k === 'yts')) assert.deepEqual(Object.keys(m).sort(), ['a', 'c', 'd', 'f', 'g', 'k', 'o', 'r', 's', 'w'].sort());
  await page.context().close();
});

test('a page with many videos: the ad of a late one is still skipped (the cap forgets finished videos and keeps up to a dozen playing ones)', async () => {
  const feed = Array.from({ length: 6 }, (_, i) => `<video class="feed" id="f${i}" style="width:60px;height:30px" muted loop></video>`).join('');
  const page = await open({ html: feed + FAMILY.jw.html, fam: FAMILY.jw, timing: FAST });
  await page.evaluate((wavUrl) => { document.querySelectorAll('video.feed').forEach((v) => { v.src = wavUrl; v.play().catch(() => {}); }); }, MAIN);   // six more videos playing: seven in all
  await page.waitForTimeout(500);
  await page.evaluate(() => window.__startAd());
  await waitFor(page, () => window.__ended === 1);
  await page.waitForTimeout(500);
  const s = await state(page);
  assert.equal(sum(s.msgs, 'f'), 1, 'the ad of the seventh video was handled');
  await page.context().close();
});

test('a long feed: videos that played and stopped are forgotten, so the player that comes after them is still followed', async () => {
  const feed = Array.from({ length: 16 }, (_, i) => `<video class="feed" id="f${i}" style="width:40px;height:20px" muted></video>`).join('');
  const page = await open({ html: feed + FAMILY.jw.html, fam: FAMILY.jw, timing: FAST });
  await page.evaluate(async (wavUrl) => {
    const list = Array.from(document.querySelectorAll('video.feed'));
    for (const v of list) { v.src = wavUrl; try { await v.play(); } catch (e) { /* ignore */ } v.pause(); }          // each one plays and is stopped again, like a feed that plays the video on screen
  }, MAIN);
  await page.waitForTimeout(300);
  await page.evaluate(() => window.__startAd());
  await waitFor(page, () => window.__ended === 1);
  await page.waitForTimeout(500);
  assert.equal(sum((await state(page)).msgs, 'f'), 1, 'handled although sixteen videos came first');
  await page.context().close();
});
