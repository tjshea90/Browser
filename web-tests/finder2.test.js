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
