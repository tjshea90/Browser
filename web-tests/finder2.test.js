'use strict';
// RVW1 (research/REVIEW_2026-11-20_FINDER.md): the page-text media scan of content.js (F9 whole addresses, relative and encoded addresses; F12 attribute-held sources) in real headless Chromium with a fake
// `browser.runtime`. Own file with its own harness: content.test.js is already near the 60 s per-file test timeout.
// Run: cd web-tests && node --test --test-timeout=60000 finder2.test.js
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const DIR = path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const CONTENT = fs.readFileSync(path.join(DIR, 'content.js'), 'utf8');
const VFIND = fs.readFileSync(path.join(DIR, 'vfind.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

async function open(body, opts) {
  const o = opts || {};
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` }));
  const page = await context.newPage();
  await page.addInitScript(() => {
    window.__msgs = [];
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } };
    window.requestIdleCallback = (f) => setTimeout(f, 30);   // headless Chromium may wait for the full 3 s timeout of an idle callback: the tests do not
  });
  if (o.pre) await page.addInitScript(o.pre);
  await page.addInitScript({ content: VFIND });
  await page.addInitScript({ content: CONTENT });
  await page.goto(o.url || 'https://site.example/');
  await page.waitForTimeout(500);
  return page;
}
const doms = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'dom').map((m) => m.u).sort());

test('F9: an address whose file name carries an extension in the middle is reported WHOLE (Kaltura, Wowza, name.mp4.m3u8)', async () => {
  const page = await open(`<script>
    var a = "https://cfvod.kaltura.com/hls/p/1/name/a.mp4/index.m3u8";
    var b = "https://cdn.example/v/vid.mp4.m3u8?t=1";
    var c = 'https://cdn.example/plain/film.mp4?sig=abc';
    var d = "https:\\/\\/cdn.example\\/esc\\/x.mp4.mpd";
  </script>`);
  assert.deepEqual(await doms(page), [
    'https://cdn.example/esc/x.mp4.mpd',
    'https://cdn.example/plain/film.mp4?sig=abc',
    'https://cdn.example/v/vid.mp4.m3u8?t=1',
    'https://cfvod.kaltura.com/hls/p/1/name/a.mp4/index.m3u8',
  ]);
});

test('F9: a sentence that ends right after the file name still gives the file; a name that goes on (a.mp4x, a.mp4-720.jpg) gives nothing', async () => {
  const page = await open(`<script>
    var s = "Watch https://cdn.example/clip/end.mp4. It is good";
    var t = "https://cdn.example/clip/x.mp4x https://cdn.example/clip/y.mp4-720.jpg";
  </script>`);
  assert.deepEqual(await doms(page), ['https://cdn.example/clip/end.mp4']);
});

test('F9: relative and scheme-relative addresses after a player key are resolved (JW Player file:"//cdn/x.mp4", src:"/media/b.m3u8"); other paths are left alone', async () => {
  const page = await open(`<script>
    jwplayer("p").setup({ file: "//cdn.example/x/a.mp4", sources: [{ src: "/media/b.m3u8" }], image: "/img/poster.jpg" });
    var css = { background: 'url("/img/hero.png")', script: "/static/app.js", other: "/docs/readme.txt" };
  </script>`);
  assert.deepEqual(await doms(page), ['https://cdn.example/x/a.mp4', 'https://site.example/media/b.m3u8']);
});

test('F9: an address percent-encoded inside another address is decoded on its own (player.php?url=https%3A%2F%2F...); the text around it is not decoded', async () => {
  const page = await open(`<script>
    var embed = "/player.php?url=https%3A%2F%2Fcdn.example%2Fa%2Findex.m3u8%3Ftoken%3Dabc&autoplay=1";
    var broken = "/p.php?url=https%3A%2F%2Fcdn.example%2F%E0%A4%A.mp4";
  </script>`);
  assert.deepEqual(await doms(page), ['https://cdn.example/a/index.m3u8?token=abc']);
});

test('F12: sources held in attributes are found (data-hls, a lazy video data-src, video.js data-setup, embed src, data-config JSON); a lazy picture is not', async () => {
  const page = await open(`
    <div class="player" data-hls="https://cdn.example/h/master.m3u8"></div>
    <video data-src="//cdn.example/lazy/clip.mp4"></video>
    <video data-setup='{"sources":[{"src":"https://cdn.example/vjs/v.mp4","type":"video/mp4"}]}'></video>
    <embed src="https://cdn.example/emb/e.webm">
    <div data-config='{"stream":{"hls":"https:\\/\\/cdn.example\\/cfg\\/live.m3u8"}}'></div>
    <img data-src="https://cdn.example/pic/thumb.mp4.jpg">
    <img data-src="https://cdn.example/pic/real.jpg">`);
  assert.deepEqual(await doms(page), [
    'https://cdn.example/cfg/live.m3u8',
    'https://cdn.example/emb/e.webm',
    'https://cdn.example/h/master.m3u8',
    'https://cdn.example/lazy/clip.mp4',
    'https://cdn.example/vjs/v.mp4',
  ]);
});

test('F12: a page with hundreds of lazy pictures still finds the lazy video after them, and the scan stays small', async () => {
  const imgs = Array.from({ length: 400 }, (_, i) => `<img data-src="https://cdn.example/p/${i}.jpg">`).join('');
  const page = await open(`${imgs}<div data-video-src="https://cdn.example/late/last.mp4"></div>`);
  assert.deepEqual(await doms(page), ['https://cdn.example/late/last.mp4']);
});

const vids = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'vid').map((m) => ({ u: m.u, f: m.f, el: m.el })));

test('F7: a muted autoplay clip reported as decoration is reported AGAIN, once, when the person unmutes it', async () => {
  const page = await open(`<video id="v" src="https://cdn.example/clip/short.mp4" muted autoplay loop playsinline style="width:300px;height:200px"></video>`);
  await page.evaluate(() => { document.getElementById('v').dispatchEvent(new Event('loadedmetadata')); });   // the page's clip announces itself (no real file behind the address in this test)
  await page.waitForTimeout(100);
  const first = await vids(page);
  assert.equal(first.length, 1, JSON.stringify(first));
  await page.evaluate(() => { const v = document.getElementById('v'); v.muted = false; v.volume = 1; });
  await page.waitForTimeout(300);
  const second = await vids(page);
  assert.equal(second.length, 2, 'the unmute made it say so again: ' + JSON.stringify(second));
  assert.equal(second[1].u, 'https://cdn.example/clip/short.mp4');
  await page.evaluate(() => { const v = document.getElementById('v'); v.muted = true; v.muted = false; });
  await page.waitForTimeout(300);
  assert.equal((await vids(page)).length, 2, 'once per element: another unmute is not another report');
});

// ---------------------------------------------------------------- RVW1 P6: the generic-hiding scan on a foreground return
const appWithMarker = () => {
  window.__gen = [];
  window.requestIdleCallback = (f) => setTimeout(f, 30);
  window.browser.runtime.sendMessage = (m) => {
    window.__msgs.push(m);
    if (m.k === 'css') return Promise.resolve('/*kg*/.site-rule{display:none!important}');
    if (m.k === 'gen') { window.__gen.push(m.t); return Promise.resolve(''); }
    return Promise.resolve(undefined);
  };
};

test('P6: returning to a page that did not change while hidden does not scan the whole page again; one that did change is scanned once', async () => {
  const page = await open('<div class="a b">x</div><p id="p1">y</p>', { pre: appWithMarker });
  await page.waitForTimeout(1200);
  await page.evaluate(() => {
    window.__scans = 0;
    const o = Document.prototype.querySelectorAll;
    Document.prototype.querySelectorAll = function (s) { if (s === '[id],[class]') window.__scans++; return o.apply(this, arguments); };
  });
  const setHidden = (h) => page.evaluate((hh) => { Object.defineProperty(document, 'hidden', { configurable: true, get: () => hh }); document.dispatchEvent(new Event('visibilitychange')); }, h);
  const scans = () => page.evaluate(() => window.__scans);
  await setHidden(true); await setHidden(false);
  await page.waitForTimeout(300);
  assert.equal(await scans(), 0, 'nothing changed while hidden: no scan on return (was one scan at every unlock)');
  await setHidden(true);
  await page.evaluate(() => { const d = document.createElement('div'); d.className = 'added-while-hidden'; document.body.appendChild(d); });
  await page.waitForTimeout(300);                                                 // the observer saw it and dropped the record
  await setHidden(false);
  await page.waitForTimeout(300);
  assert.equal(await scans(), 1, 'something changed while hidden: one scan on return');
  await setHidden(true); await setHidden(false);
  await page.waitForTimeout(300);
  assert.equal(await scans(), 1, 'and the next plain return scans nothing');
});

test('F11: the quality label of each <source> travels with its address (label, title, res, size); an unlabeled one sends none', async () => {
  const page = await open(`<video controls>
    <source src="https://cdn.example/q/v1080.mp4" label="1080p">
    <source src="https://cdn.example/q/v720.mp4" title="720p HD">
    <source src="https://cdn.example/q/v480.mp4" res="480">
    <source src="https://cdn.example/q/v360.mp4" size="360">
    <source src="https://cdn.example/q/plain.mp4">
    <source src="https://cdn.example/q/long.mp4" label="a very long label that is not a quality at all">
  </video>`);
  const got = await page.evaluate(() => window.__msgs.filter((m) => m.k === 'dom').map((m) => [m.u.split('/').pop(), m.lb === undefined ? null : m.lb]));
  const byName = Object.fromEntries(got);
  assert.equal(byName['v1080.mp4'], '1080p');
  assert.equal(byName['v720.mp4'], '720p HD');
  assert.equal(byName['v480.mp4'], '480');
  assert.equal(byName['v360.mp4'], '360');
  assert.equal(byName['plain.mp4'], null);
  assert.equal(byName['long.mp4'], null, 'a long text is not a quality label');
});

test('F5: a video inside a shadow root reports its metadata like any other (the media events do not cross the shadow boundary, so a listener on the element itself is needed); a light-DOM video is not double-watched', async () => {
  const page = await open(`<div id="host"></div><video id="lv" src="https://cdn.example/s/light.mp4" style="width:200px;height:100px"></video><script>
    var sr = document.getElementById('host').attachShadow({ mode: 'open' });
    sr.innerHTML = '<video id="sv" src="https://cdn.example/s/clip.mp4" style="width:320px;height:180px"></video>';
  </script>`, { pre: () => { window.__kstVfCfg = { shadowMs: 80, shadowTapMs: 60 }; } });
  await page.waitForTimeout(900);
  await page.evaluate(() => {
    document.getElementById('host').shadowRoot.getElementById('sv').dispatchEvent(new Event('loadedmetadata'));
    document.getElementById('lv').dispatchEvent(new Event('loadedmetadata'));
  });
  await page.waitForTimeout(200);
  const got = (await vids(page)).map((x) => x.u).sort();
  assert.deepEqual(got, ['https://cdn.example/s/clip.mp4', 'https://cdn.example/s/light.mp4']);
});

test('F5: a player that is built when it is pressed is found after the tap (the second look)', async () => {
  const page = await open(`<div id="host"></div><script>
    document.addEventListener('click', function () { setTimeout(function () {
      var sr = document.getElementById('host').attachShadow({ mode: 'open' });
      sr.innerHTML = '<video id="late" src="https://cdn.example/s/late.mp4" style="width:320px;height:180px"></video>';
    }, 10); });
  </script>`, { pre: () => { window.__kstVfCfg = { shadowMs: 80, shadowTapMs: 80 }; } });
  await page.waitForTimeout(500);                                   // the first look (after load) found nothing
  await page.mouse.click(5, 5);
  await page.waitForTimeout(600);                                   // 10 ms to build, 80 ms to look again
  await page.evaluate(() => { document.getElementById('host').shadowRoot.getElementById('late').dispatchEvent(new Event('loadedmetadata')); });
  await page.waitForTimeout(150);
  assert.deepEqual((await vids(page)).map((x) => x.u), ['https://cdn.example/s/late.mp4']);
});
