'use strict';
// redirects.js (AW1c: the stand-ins a `$redirect=` rule answers a blocked request with) checked three ways:
//  1. its id table against the one the Kotlin filter engine writes into the rule words (fixture mini.redirects.tsv, made by ExtImageTest);
//  2. the binary files are what they claim to be (picture sizes, audio and video container);
//  3. every script stand-in does in a real browser what a page that waits for the original needs from it (the SDK shapes pages and players call).
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const R = require('../geckoapp/src/main/assets/kestrel-shields/redirects.js');
const FX = path.join(__dirname, 'fixtures');
const lines = (n) => fs.readFileSync(path.join(FX, n), 'utf8').split('\n').filter((l) => l.length > 0);

let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

async function open(body) {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><html><head><title>t</title></head><body>' + (body || '') + '</body></html>' }));
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', (e) => errors.push(String(e && e.message || e)));
  await page.goto('https://site.example/');
  page.errors = errors;
  return page;
}
const id = (name) => { const i = R.names.indexOf(name); assert.ok(i > 0, name + ' is in the table'); return i; };
/** loads stand-in [name] as a script the way a page would; resolves 'load' or 'error' */
const loadScript = (page, name) => page.evaluate((url) => new Promise((res) => { const s = document.createElement('script'); s.src = url; s.onload = () => res('load'); s.onerror = () => res('error'); document.head.appendChild(s); }), R.urlFor(id(name), 'script'));
const settle = (ms) => new Promise((r) => setTimeout(r, ms || 100));

// ---------------------------------------------------------------- 1. the table
test('the id table is the one the Kotlin engine writes into its rule words (RedirectResources.FILES)', () => {
  const rows = lines('mini.redirects.tsv').filter((l) => !l.startsWith('name:')).map((l) => l.split('\t'));
  assert.equal(rows.length, R.names.length, 'same number of entries');
  for (const [i, name] of rows) assert.equal(R.names[Number(i)], name, 'id ' + i);
  assert.equal(R.count, rows.length - 1);
  assert.ok(R.count <= 31, 'five bits in the rule word');
});

test('the names the lists use map to the ids that the stand-in table has a resource for', () => {
  for (const l of lines('mini.redirects.tsv').filter((l) => l.startsWith('name:'))) {
    const [n, got, want] = l.split('\t');
    assert.equal(got, want, n);
    assert.ok(Number(got) >= 1 && Number(got) <= R.count, n);
  }
});

test('a stand-in answers only the kinds of request it suits, anything else is left to be cancelled', () => {
  assert.ok(R.urlFor(id('noop.js'), 'script').startsWith('data:text/javascript'));
  assert.equal(R.urlFor(id('noop.js'), 'image'), null);
  assert.ok(R.urlFor(id('noop.js'), 'xmlhttprequest'));
  assert.ok(R.urlFor(id('1x1.gif'), 'image').startsWith('data:image/gif;base64,'));
  assert.ok(R.urlFor(id('1x1.gif'), 'imageset'));
  assert.equal(R.urlFor(id('1x1.gif'), 'script'), null);
  assert.ok(R.urlFor(id('noop-1s.mp4'), 'media'));
  assert.equal(R.urlFor(id('noop-1s.mp4'), 'image'), null);
  assert.ok(R.urlFor(id('noop.html'), 'sub_frame'));
  assert.equal(R.urlFor(id('noop.html'), 'script'), null);
  assert.ok(R.urlFor(id('noop.css'), 'stylesheet'));
  assert.ok(R.urlFor(id('noop.json'), 'xmlhttprequest'));
  assert.equal(R.urlFor(id('google-ima.js'), 'xmlhttprequest'), null);
  assert.equal(R.urlFor(0, 'script'), null);
  assert.equal(R.urlFor(99, 'script'), null);
  assert.equal(R.urlFor(id('noop.js'), 'main_frame'), null, 'a document is never answered with a stand-in');
  assert.equal(R.urlFor(id('noop.js'), 'script'), R.urlFor(id('noop.js'), 'script'), 'built once');
});

test('no address leaks the extension: every stand-in is a data: URL', () => {
  for (let i = 1; i <= R.count; i++) assert.ok(/^data:[a-z0-9/+.-]+(;charset=utf-8)?(;base64)?,/.test(R.urlFor(i, R.typesOf(i)[0])), R.names[i]);
});

// ---------------------------------------------------------------- 2. the binary files
const bytesOf = (name) => Buffer.from(R.sourceOf(id(name)), 'base64');

test('the pictures are a transparent GIF and PNGs of the sizes their names say', () => {
  const gif = bytesOf('1x1.gif');
  assert.equal(gif.subarray(0, 6).toString('latin1'), 'GIF89a');
  assert.equal(gif.readUInt16LE(6), 1); assert.equal(gif.readUInt16LE(8), 1);
  for (const [n, w, h] of [['2x2.png', 2, 2], ['3x2.png', 3, 2], ['32x32.png', 32, 32]]) {
    const b = bytesOf(n);
    assert.equal(b.subarray(0, 8).toString('hex'), '89504e470d0a1a0a', n);
    assert.equal(b.readUInt32BE(16), w, n); assert.equal(b.readUInt32BE(20), h, n);
    assert.equal(b[25], 6, n + ' is RGBA');
  }
  assert.ok(Buffer.concat([bytesOf('1x1.gif')]).length < 100);
});

test('the audio is an MP3 and the video an MP4 (H.264) with a header the media stack recognises, both small', () => {
  const mp3 = bytesOf('noop-0.1s.mp3');
  const syncAt = mp3.findIndex((b, i) => b === 0xff && (mp3[i + 1] & 0xe0) === 0xe0);
  assert.ok(syncAt >= 0 && syncAt < 64, 'an MPEG audio frame sync near the start');
  assert.ok(mp3.length < 2000);
  const mp4 = bytesOf('noop-1s.mp4');
  assert.equal(mp4.subarray(4, 8).toString('latin1'), 'ftyp');
  assert.ok(mp4.includes(Buffer.from('moov')) && mp4.includes(Buffer.from('mdat')) && mp4.includes(Buffer.from('avc1')));
  assert.ok(mp4.length < 4000);
});

// ---------------------------------------------------------------- 3. the stand-ins in a real browser
test('pictures decode at their sizes, the audio has a duration, the text stand-ins parse', async () => {
  const page = await open('');
  const out = await page.evaluate((u) => Promise.all(Object.keys(u.img).map((k) => new Promise((res) => { const i = new Image(); i.onload = () => res([k, i.naturalWidth, i.naturalHeight]); i.onerror = () => res([k, 0, 0]); i.src = u.img[k]; }))), { img: Object.fromEntries(['1x1.gif', '2x2.png', '3x2.png', '32x32.png'].map((n) => [n, R.urlFor(id(n), 'image')])) });
  assert.deepEqual(out.sort(), [['1x1.gif', 1, 1], ['2x2.png', 2, 2], ['32x32.png', 32, 32], ['3x2.png', 3, 2]]);
  const audio = await page.evaluate((u) => new Promise((res) => { const a = new Audio(); a.onloadedmetadata = () => res(a.duration); a.onerror = () => res(-1); a.src = u; }), R.urlFor(id('noop-0.1s.mp3'), 'media'));
  assert.ok(audio > 0.03 && audio < 0.5, 'duration ' + audio);
  const t = await page.evaluate(async (u) => ({
    json: await (await fetch(u.json)).json(),
    txt: await (await fetch(u.txt)).text(),
    vast: new DOMParser().parseFromString(await (await fetch(u.vast)).text(), 'text/xml').documentElement.nodeName,
    html: new DOMParser().parseFromString(await (await fetch(u.html)).text(), 'text/html').body.children.length,
  }), { json: R.urlFor(id('noop.json'), 'xmlhttprequest'), txt: R.urlFor(id('noop.txt'), 'xmlhttprequest'), vast: R.urlFor(id('noop-vast.xml'), 'xmlhttprequest'), html: R.urlFor(id('noop.html'), 'sub_frame') });
  assert.deepEqual(t, { json: {}, txt: '', vast: 'VAST', html: 0 });
});

test('every script stand-in loads as a script without an error, also twice and also after the page broke the names it touches', async () => {
  const names = R.names.filter((n) => /\.js$/.test(n));
  const page = await open('<script>window.google = 5; window.googletag = "x"; window.dataLayer = null; window.ga = 1; window._gaq = 7; window.adsbygoogle = {}; window.apstag = 0;</script>');
  for (const n of names) {
    assert.equal(await loadScript(page, n), 'load', n);
    assert.equal(await loadScript(page, n), 'load', n + ' (second time)');
  }
  await settle();
  assert.deepEqual(page.errors, []);
});

test('google-ima.js: an ad request is answered with "no ad", the player is told and carries on', async () => {
  const page = await open('');
  assert.equal(await loadScript(page, 'google-ima.js'), 'load');
  const r = await page.evaluate(() => new Promise((res) => {
    const ima = google.ima;
    const events = [];
    const container = new ima.AdDisplayContainer(document.body, document.createElement('video'));
    container.initialize();
    const loader = new ima.AdsLoader(container);
    loader.getSettings().setVpaidMode(ima.ImaSdkSettings.VpaidMode.ENABLED);
    loader.addEventListener(ima.AdsManagerLoadedEvent.Type.ADS_MANAGER_LOADED, () => events.push('manager'), false);
    loader.addEventListener(ima.AdErrorEvent.Type.AD_ERROR, (e) => { events.push('error:' + e.getError().getErrorCode() + ':' + e.getError().getType() + ':' + e.getUserRequestContext()); res(events); }, false);
    const req = new ima.AdsRequest();
    req.adTagUrl = 'https://ads.example/vast.xml'; req.linearAdSlotWidth = 640; req.linearAdSlotHeight = 360;
    req.setAdWillAutoPlay(true);
    loader.requestAds(req, 'ctx1');
    events.push('requested');
  }));
  assert.deepEqual(r, ['requested', 'error:1009:adLoadError:ctx1']);
  const shape = await page.evaluate(() => [google.ima.AdEvent.Type.CONTENT_RESUME_REQUESTED, google.ima.AdEvent.Type.ALL_ADS_COMPLETED, google.ima.ViewMode.FULLSCREEN, typeof google.ima.VERSION, google.ima.AdError.ErrorCode.VAST_EMPTY_RESPONSE]);
  assert.deepEqual(shape, ['contentResumeRequested', 'allAdsCompleted', 'fullscreen', 'string', 1009]);
  assert.deepEqual(page.errors, []);
});

test('google-ima.js: a removed listener is not called, a destroyed loader says nothing, a real SDK that is already there is left alone', async () => {
  const page = await open('');
  await loadScript(page, 'google-ima.js');
  const n = await page.evaluate(() => new Promise((res) => {
    const l = new google.ima.AdsLoader({}); let calls = 0;
    const f = () => { calls++; };
    l.addEventListener('adError', f); l.removeEventListener('adError', f);
    const d = new google.ima.AdsLoader({}); d.addEventListener('adError', f); d.requestAds({}); d.destroy();
    l.requestAds({});
    setTimeout(() => res(calls), 60);
  }));
  assert.equal(n, 0);
  const real = await open('<script>window.google = { ima: { AdsLoader: function RealLoader() {}, marker: 7 } }</script>');
  await loadScript(real, 'google-ima.js');
  assert.deepEqual(await real.evaluate(() => [google.ima.marker, google.ima.AdsLoader.name, typeof google.ima.AdError]), [7, 'RealLoader', 'undefined']);
});

test('googletagservices_gpt.js: the page\'s queue runs, later pushes run at once, the calls chain and answer like an empty ad server', async () => {
  const page = await open('<script>window.googletag = window.googletag || {}; googletag.cmd = googletag.cmd || []; googletag.cmd.push(function () { window.__first = "ran"; });</script>');
  assert.equal(await loadScript(page, 'googletagservices_gpt.js'), 'load');
  const r = await page.evaluate(() => {
    const out = { first: window.__first };
    googletag.cmd.push(function () {
      const slot = googletag.defineSlot('/1/unit', [300, 250], 'div-1').addService(googletag.pubads()).setTargeting('k', 'v');
      googletag.pubads().enableSingleRequest().collapseEmptyDivs().setTargeting('a', 'b').disableInitialLoad();
      googletag.defineOutOfPageSlot('/1/oop', 'div-2').addService(googletag.pubads());
      googletag.enableServices();
      googletag.display('div-1');
      googletag.pubads().refresh([slot]);
      out.path = slot.getAdUnitPath(); out.id = slot.getSlotElementId(); out.slots = googletag.pubads().getSlots().length;
      out.mapping = googletag.sizeMapping().addSize([0, 0], [1, 1]).build().length;
      out.sync = true;
    });
    out.after = out.sync === true;
    out.ready = [googletag.apiReady, googletag.pubadsReady];
    return out;
  });
  assert.deepEqual(r, { first: 'ran', path: '/1/unit', id: 'div-1', slots: 0, mapping: 0, sync: true, after: true, ready: [true, true] });
  assert.deepEqual(page.errors, []);
});

test('googletagmanager_gtm.js: the data layer works and the callbacks a page waits for are called', async () => {
  const page = await open('<script>window.dataLayer = [{ event: "early", eventCallback: function () { window.__early = 1; } }];</script>');
  assert.equal(await loadScript(page, 'googletagmanager_gtm.js'), 'load');
  const r = await page.evaluate(() => new Promise((res) => {
    window.dataLayer.push({ event: 'x', eventCallback: () => { window.__late = 1; }, hitCallback: () => { window.__hit = 1; } });
    setTimeout(() => res({ early: window.__early, late: window.__late, hit: window.__hit, len: window.dataLayer.length, gtm: typeof window.google_tag_manager }), 60);
  }));
  assert.deepEqual(r, { early: 1, late: 1, hit: 1, len: 2, gtm: 'object' });
});

test('google-analytics_analytics.js: analytics.js and ga.js commands are accepted, ready and hit callbacks are called, queued commands run', async () => {
  const page = await open('<script>window.ga = window.ga || function () { (ga.q = ga.q || []).push(arguments); }; ga("create", "UA-1-1", "auto"); ga("send", "pageview", { hitCallback: function () { window.__queuedHit = 1; } }); window._gaq = window._gaq || []; _gaq.push(["_setAccount", "UA-1"]); _gaq.push(function () { window.__gaqFn = 1; });</script>');
  assert.equal(await loadScript(page, 'google-analytics_analytics.js'), 'load');
  const r = await page.evaluate(() => new Promise((res) => {
    const out = {};
    ga('create', 'UA-2-2', 'auto');
    ga(function (t) { out.ready = typeof t.get('clientId'); out.trackers = ga.getAll().length; });
    ga('send', 'event', 'a', 'b', { hitCallback: () => { out.hit = 1; } });
    _gaq.push(['_trackPageview']); _gaq.push(function () { out.fn = 1; });
    out.tracker = typeof _gat._getTracker('UA-3').a === 'undefined' ? 'stub' : 'other';
    setTimeout(() => { out.queuedHit = window.__queuedHit; out.gaqFn = window.__gaqFn; out.loaded = ga.loaded; res(out); }, 60);
  }));
  assert.deepEqual(r, { ready: 'undefined', trackers: 1, hit: 1, fn: 1, tracker: 'stub', queuedHit: 1, gaqFn: 1, loaded: true });
});

test('googlesyndication_adsbygoogle.js: the array says it is loaded and swallows what the page pushes', async () => {
  const page = await open('<script>(window.adsbygoogle = window.adsbygoogle || []).push({});</script>');
  assert.equal(await loadScript(page, 'googlesyndication_adsbygoogle.js'), 'load');
  const r = await page.evaluate(() => { (window.adsbygoogle = window.adsbygoogle || []).push({ google_ad_client: 'x' }); return [adsbygoogle.loaded, typeof adsbygoogle.push]; });
  assert.deepEqual(r, [true, 'function']);
});

test('fingerprint2.js, nobab.js, amazon_apstag.js, chartbeat.js, prebid-ads.js: what pages call on them answers and returns', async () => {
  const page = await open('');
  for (const n of ['fingerprint2.js', 'nobab.js', 'amazon_apstag.js', 'chartbeat.js', 'prebid-ads.js']) assert.equal(await loadScript(page, n), 'load', n);
  const r = await page.evaluate(() => new Promise((res) => {
    const out = {};
    Fingerprint2.get({}, (c) => { out.fp2 = Array.isArray(c) ? c.length : -1; });
    new Fingerprint2().get((h) => { out.fp2old = typeof h + h.length; });
    const bab = new BlockAdBlock({ checkOnLoad: true, resetOnEnd: true });
    bab.onDetected(() => { out.detected = true; });
    bab.onNotDetected(() => { out.notDetected = 1; });
    fuckAdBlock.onNotDetected(() => { out.fuckNot = 1; });
    apstag.init({ pubID: 'x', adServer: 'googletag' });
    apstag.fetchBids({ slots: [] }, (bids) => { out.bids = bids.length; });
    pSUPERFLY.virtualPage('/p'); pSUPERFLY.activity();
    out.canRun = [window.canRunAds, window.isAdBlockActive];
    setTimeout(() => res(out), 80);
  }));
  assert.deepEqual(r, { fp2: 0, fp2old: 'string32', notDetected: 1, fuckNot: 1, bids: 0, canRun: [true, false] });
});

test('a callback registered after the check still hears "no blocker" (nobab.js)', async () => {
  const page = await open('');
  await loadScript(page, 'nobab.js');
  const r = await page.evaluate(() => new Promise((res) => {
    const b = new FuckAdBlock(); let n = 0;
    setTimeout(() => { b.onNotDetected(() => { n++; }); setTimeout(() => res(n), 40); }, 30);
  }));
  assert.equal(r, 1);
});

test('noop.js: loads, defines nothing, throws nothing', async () => {
  const page = await open('');
  const before = await page.evaluate(() => Object.getOwnPropertyNames(window).length);
  assert.equal(await loadScript(page, 'noop.js'), 'load');
  assert.equal(await page.evaluate(() => Object.getOwnPropertyNames(window).length), before);
  assert.deepEqual(page.errors, []);
});

test('the video stand-in has a container the browser accepts (decoding H.264 needs a build with proprietary codecs, the tablet has one)', async () => {
  const page = await open('');
  const r = await page.evaluate((u) => new Promise((res) => {
    const v = document.createElement('video');
    const can = v.canPlayType('video/mp4; codecs="avc1.42E00A"');
    if (!can) return res('skip');
    v.onloadedmetadata = () => res(['meta', Math.round(v.duration), v.videoWidth]);
    v.onerror = () => res('error');
    v.src = u;
  }), R.urlFor(id('noop-1s.mp4'), 'media'));
  if (r === 'skip') return; // this browser has no H.264: the container check above is all that can be done here
  assert.deepEqual(r, ['meta', 1, 16]);
});
