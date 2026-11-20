'use strict';
// QL1 (owner message 2026-11-18: "When i put YouTube video in full screen, it doesn't let me choose the video quality. I want to be able to choose the video quality on as many sites and videos as possible"):
// quality.js (the quality list of the film, found through the site's OWN player) and quality-main.js (notes the hls.js / dash.js / Shaka players a page makes) against MOCK players in real headless Chromium.
// The mocks have the method names the players' public APIs document (YouTube's player element: getAvailableQualityLevels / setPlaybackQualityRange / getPlaybackQuality; JW Player 8: getQualityLevels /
// setCurrentQuality; videojs-contrib-quality-levels; hls.js: levels / currentLevel / autoLevelEnabled; dash.js: getBitrateInfoListFor / setQualityFor / updateSettings; Shaka: getVariantTracks /
// selectVariantTrack / configure). A mock agrees with what the code believes the API is: it proves the logic (rows, order, labels, the call each pick makes, the report to the app, the command from it),
// NOT that a real site's player answers the same way: that is unverified until a real film is tried on the tablet.
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { launch, close, open, msgs } = require('./videobar-lib');

before(launch);
after(close);

const wait = (page, ms) => page.waitForTimeout(ms);
const FILM = '<video id="v" src="https://cdn.example/film.wav" preload="auto" muted style="width:100%;height:100%"></video>';
const find = (page) => page.evaluate(() => { const a = window.KestrelQ.find(document.getElementById('v')); return a ? { fam: a.fam, levels: a.levels(), cur: a.cur() } : null; });
const calls = (page) => page.evaluate(() => window.__calls.slice());

const YT = (extra) => `<div id="movie_player" class="html5-video-player" style="position:fixed;left:0;top:0;width:300px;height:200px;background:#000">${FILM}</div>
  <script>window.__calls = []; var el = document.getElementById('movie_player'); var cur = 'hd720';
  el.getAvailableQualityLevels = function () { return ['hd1080', 'hd720', 'large', 'medium', 'small', 'tiny', 'auto']; };
  el.getPlaybackQuality = function () { return cur; };
  el.setPlaybackQualityRange = function (a, b) { window.__calls.push(['range', a, b]); if (a === b) cur = a; };
  el.setPlaybackQuality = function (q) { window.__calls.push(['one', q]); };
  ${extra || ''}</script>`;

test('YouTube: the player element\'s own levels, Automatic first, biggest picture first, with the site\'s labels when it has them', async () => {
  const page = await open(YT(), { quality: true });
  const r = await find(page);
  assert.equal(r.fam, 'yt');
  assert.deepEqual(r.levels.map((x) => x.i), ['auto', 'hd1080', 'hd720', 'large', 'medium', 'small', 'tiny']);
  assert.deepEqual(r.levels.map((x) => x.l), ['Automatic', '1080p', '720p', '480p', '360p', '240p', '144p']);
  assert.equal(r.cur, 'hd720');
  await page.context().close();
  const page2 = await open(YT("el.getAvailableQualityData = function () { return [{ quality: 'hd1080', qualityLabel: '1080p60' }, { quality: 'hd720', qualityLabel: '720p' }]; };"), { quality: true });
  const r2 = await find(page2);
  assert.equal(r2.levels.find((x) => x.i === 'hd1080').l, '1080p60', 'the site\'s own label wins');
  await page2.context().close();
});

test('YouTube: a pick calls the player (a range of exactly that picture), Automatic gives the whole range back', async () => {
  const page = await open(YT(), { quality: true });
  await page.evaluate(() => { const a = window.KestrelQ.find(document.getElementById('v')); window.__a = a; a.set('hd1080'); a.set('auto'); });
  assert.deepEqual(await calls(page), [['range', 'hd1080', 'hd1080'], ['one', 'hd1080'], ['range', 'tiny', 'highres']]);
  await page.context().close();
});

test('a page whose player has no quality API offers nothing (no list, no button)', async () => {
  const page = await open(`<div id="movie_player" class="html5-video-player" style="position:fixed;width:300px;height:200px">${FILM}</div>`, { quality: true });
  assert.equal(await find(page), null);
  await page.context().close();
  const page2 = await open(`<div style="width:300px;height:200px">${FILM}</div>`, { quality: true });
  assert.equal(await find(page2), null);
  await page2.context().close();
});

test('labels and ids are cut short and cleaned: a page cannot send long text or markup to the app', async () => {
  const page = await open(YT("el.getAvailableQualityData = function () { return [{ quality: 'hd1080', qualityLabel: '<b>1080p</b> ' + new Array(80).join('x') }]; }; el.getAvailableQualityLevels = function () { return ['hd1080', 'hd720', 'a/b c<d>e', new Array(40).join('z')]; };"), { quality: true });
  const r = await find(page);
  for (const l of r.levels) { assert.ok(l.l.length <= 22, 'label ' + l.l); assert.ok(l.i.length <= 24, 'id ' + l.i); assert.ok(!/[<>]/.test(l.l), l.l); assert.ok(/^[A-Za-z0-9_.:\-]+$/.test(l.i), 'id chars ' + l.i); }
  assert.ok(r.levels.length <= 12);
  await page.context().close();
});

test('JW Player: the levels of jwplayer(id), the pick is setCurrentQuality(index)', async () => {
  const html = `<div id="jwp" class="jwplayer" style="position:fixed;left:0;top:0;width:300px;height:200px">${FILM}</div><script>window.__calls = [];
    window.jwplayer = function (id) { return { getQualityLevels: function () { return [{ label: 'Auto' }, { label: '1080p' }, { label: '720p' }, { label: '360p' }]; }, getCurrentQuality: function () { return 2; }, setCurrentQuality: function (i) { window.__calls.push(['set', i]); } }; };</script>`;
  const page = await open(html, { quality: true });
  const r = await find(page);
  assert.equal(r.fam, 'jw');
  assert.deepEqual(r.levels.map((x) => x.l), ['Automatic', '1080p', '720p', '360p']);
  assert.equal(r.cur, '2');
  await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).set('1'));
  assert.deepEqual(await calls(page), [['set', 1]]);
  await page.context().close();
});

test('Video.js with the quality-levels plugin: the pick enables exactly one level, Automatic enables all', async () => {
  const html = `<div id="vp" class="video-js" style="position:fixed;left:0;top:0;width:300px;height:200px">${FILM}</div><script>
    var lv = [{ height: 360, bitrate: 800000, enabled: true }, { height: 720, bitrate: 2500000, enabled: true }, { height: 1080, bitrate: 5000000, enabled: true }]; lv.selectedIndex = 1;
    window.videojs = { getPlayer: function (id) { return { qualityLevels: function () { return lv; } }; } }; window.__lv = lv;</script>`;
  const page = await open(html, { quality: true });
  const r = await find(page);
  assert.equal(r.fam, 'vjs');
  assert.deepEqual(r.levels.map((x) => x.l), ['Automatic', '1080p', '720p', '360p']);
  assert.equal(r.cur, 'auto');
  await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).set('h720'));
  assert.deepEqual(await page.evaluate(() => window.__lv.map((x) => x.enabled)), [false, true, false]);
  await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).set('auto'));
  assert.deepEqual(await page.evaluate(() => window.__lv.map((x) => x.enabled)), [true, true, true]);
  await page.context().close();
});

test('hls.js: the constructor is noted when the page sets the global; levels, pick and Automatic work on the instance; the original class still works', async () => {
  const html = `<div style="position:fixed;left:0;top:0;width:300px;height:200px">${FILM}</div><script>
    window.Hls = (function () { function H() { this.levels = [{ height: 360, bitrate: 600000 }, { height: 720, bitrate: 2000000 }, { height: 1080, bitrate: 4500000 }, { height: 1080, bitrate: 3000000 }]; this.currentLevel = -1; this.autoLevelEnabled = true; this.media = null; }
      H.isSupported = function () { return true; }; H.prototype.attachMedia = function (m) { this.media = m; }; return H; })();
    window.__hls = new Hls(); window.__hls.attachMedia(document.getElementById('v')); window.__inst = (window.__hls instanceof Hls); window.__sup = Hls.isSupported();</script>`;
  const page = await open(html, { quality: true, qmain: true });
  assert.equal(await page.evaluate(() => window.__inst), true, 'instanceof still works');
  assert.equal(await page.evaluate(() => window.__sup), true, 'statics still work');
  const r = await find(page);
  assert.equal(r.fam, 'hls');
  assert.deepEqual(r.levels.map((x) => x.l), ['Automatic', '1080p', '720p', '360p']);
  assert.equal(r.cur, 'auto');
  await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).set('l1'));
  assert.equal(await page.evaluate(() => window.__hls.currentLevel), 1);
  await page.evaluate(() => { window.__hls.autoLevelEnabled = false; });
  assert.equal(await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).cur()), 'l1');
  await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).set('auto'));
  assert.equal(await page.evaluate(() => window.__hls.currentLevel), -1);
  await page.context().close();
});

test('dash.js: the player made through dashjs.MediaPlayer().create() is noted; a pick turns ABR off and sets the quality, Automatic turns it on', async () => {
  const html = `<div style="position:fixed;left:0;top:0;width:300px;height:200px">${FILM}</div><script>window.__calls = [];
    window.dashjs = { MediaPlayer: function () { return { create: function () { var abr = true; return { getVideoElement: function () { return document.getElementById('v'); },
      getBitrateInfoListFor: function () { return [{ qualityIndex: 0, bitrate: 600000, height: 360, width: 640 }, { qualityIndex: 1, bitrate: 2000000, height: 720, width: 1280 }, { qualityIndex: 2, bitrate: 4000000, height: 1080, width: 1920 }]; },
      getQualityFor: function () { return 1; }, setQualityFor: function (t, i) { window.__calls.push(['q', t, i]); },
      getSettings: function () { return { streaming: { abr: { autoSwitchBitrate: { video: abr } } } }; },
      updateSettings: function (s) { abr = s.streaming.abr.autoSwitchBitrate.video; window.__calls.push(['abr', abr]); } }; } }; } };
    window.__dash = dashjs.MediaPlayer().create();</script>`;
  const page = await open(html, { quality: true, qmain: true });
  const r = await find(page);
  assert.equal(r.fam, 'dash');
  assert.deepEqual(r.levels.map((x) => x.l), ['Automatic', '1080p', '720p', '360p']);
  assert.equal(r.cur, 'auto');
  await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).set('q2'));
  assert.deepEqual(await calls(page), [['abr', false], ['q', 'video', 2]]);
  await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).set('auto'));
  assert.deepEqual((await calls(page)).slice(2), [['abr', true]]);
  await page.context().close();
});

test('Shaka: the player built on shaka.Player is noted; one row per picture (the best of its variants), a pick turns ABR off and selects that track', async () => {
  const html = `<div style="position:fixed;left:0;top:0;width:300px;height:200px">${FILM}</div><script>window.__calls = [];
    window.shaka = {}; shaka.Player = function (m) { this.m = m; this.abr = true;
      this.getVariantTracks = function () { return [{ id: 1, height: 360, bandwidth: 600000, active: false }, { id: 2, height: 720, bandwidth: 1800000, active: true }, { id: 3, height: 720, bandwidth: 2400000, active: false }, { id: 4, height: 1080, bandwidth: 4500000, active: false }]; };
      this.selectVariantTrack = function (t, c) { window.__calls.push(['sel', t.id, c]); }; this.configure = function (c) { this.abr = c.abr.enabled; window.__calls.push(['abr', c.abr.enabled]); };
      this.getMediaElement = function () { return this.m; }; this.getConfiguration = function () { return { abr: { enabled: this.abr } }; }; };
    window.__shaka = new shaka.Player(document.getElementById('v'));</script>`;
  const page = await open(html, { quality: true, qmain: true });
  const r = await find(page);
  assert.equal(r.fam, 'shaka');
  assert.deepEqual(r.levels.map((x) => x.l), ['Automatic', '1080p', '720p', '360p']);
  await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).set('v720'));
  assert.deepEqual(await calls(page), [['abr', false], ['sel', 3, true]], 'the best 720p variant');
  assert.equal(await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).cur()), 'v720');
  await page.context().close();
});

test('<source> lists with a size: the film moves to the other file at the same second and plays on', async () => {
  const wav = (s) => { const n = 8000 * s, h = Buffer.alloc(44); h.write('RIFF', 0); h.writeUInt32LE(36 + n, 4); h.write('WAVEfmt ', 8); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22); h.writeUInt32LE(8000, 24); h.writeUInt32LE(8000, 28); h.writeUInt16LE(1, 32); h.writeUInt16LE(8, 34); h.write('data', 36); h.writeUInt32LE(n, 40); return 'data:audio/wav;base64,' + Buffer.concat([h, Buffer.alloc(n, 0x80)]).toString('base64'); };
  const A = wav(60), B = wav(61);
  const html = `<div style="position:fixed;left:0;top:0;width:300px;height:200px"><video id="v" preload="auto" muted style="width:100%;height:100%"><source src="${A}" size="720"><source src="${B}" size="360"></video></div>`;
  const page = await open(html, { quality: true });
  const r = await find(page);
  assert.equal(r.fam, 'src');
  assert.deepEqual(r.levels.map((x) => x.l), ['720p', '360p']);
  await page.evaluate(() => { const v = document.getElementById('v'); v.currentTime = 20; return v.play(); });
  await page.evaluate(() => window.KestrelQ.find(document.getElementById('v')).set('s1'));
  await page.waitForFunction(() => { const v = document.getElementById('v'); return Math.round(v.duration) === 61 && v.currentTime >= 19.5; }, null, { timeout: 8000 });
  assert.equal((await find(page)).cur, 's1');
  await page.context().close();
});

test('the full-screen report carries the page\'s list and the current pick; the app\'s command switches the page\'s player (and the list is sent again)', async () => {
  const page = await open(YT(), { player: true, quality: true, cfg: { kp: true } });
  await page.evaluate(() => document.getElementById('movie_player').requestFullscreen());
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vst' && m.q), null, { timeout: 8000 });
  const m = (await msgs(page, 'vst')).find((x) => x.q);
  assert.equal(m.qf, 'yt'); assert.equal(m.qc, 'hd720');
  assert.deepEqual(m.q[0], ['auto', 'Automatic']); assert.deepEqual(m.q[1], ['hd1080', '1080p']);
  await page.evaluate((x) => { (window.top.__vpQueue = window.top.__vpQueue || []).push(x); }, { s: 1, c: [{ a: 'ql', q: 'large' }] });
  await page.waitForFunction(() => window.__calls.some((c) => c[0] === 'range' && c[1] === 'large'), null, { timeout: 6000 });
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vst' && m.qc === 'large'), null, { timeout: 6000 });
  await page.context().close();
});

test('a command with a long or odd id does nothing; a page without a list reports no q fields at all', async () => {
  const page = await open(YT(), { player: true, quality: true, cfg: { kp: true } });
  await page.evaluate(() => document.getElementById('movie_player').requestFullscreen());
  await page.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vst' && m.q), null, { timeout: 8000 });
  await page.evaluate((x) => { (window.top.__vpQueue = window.top.__vpQueue || []).push(x); }, { s: 1, c: [{ a: 'ql', q: new Array(40).join('x') }, { a: 'ql', q: 7 }] });
  await wait(page, 800);
  assert.deepEqual(await calls(page), [], 'nothing was called');
  await page.context().close();
  const plain = await open(`<div id="box" style="position:fixed;left:0;top:0;width:300px;height:200px">${FILM}</div>`, { player: true, quality: true, cfg: { kp: true } });
  await plain.evaluate(() => document.getElementById('box').requestFullscreen());
  await plain.waitForFunction(() => window.__msgs.some((m) => m && m.k === 'vst'), null, { timeout: 8000 });
  const st = (await msgs(plain, 'vst'))[0];
  assert.ok(!('q' in st) && !('qc' in st) && !('qf' in st), JSON.stringify(Object.keys(st)));
  await plain.context().close();
});
