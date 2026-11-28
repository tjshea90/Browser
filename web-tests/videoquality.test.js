'use strict';
// RX4-M0a (research/REVIEW-EXTERNAL-4.md): how smoothly do REAL videos play on this tablet? content.js reads video.getVideoPlaybackQuality() while an element plays (every 15 s, and when it
// pauses, ends or the page is hidden) and sends only the DIFFERENCE since its last look as {k:'vq', t, d, c, s, w, h}: frames shown, dropped, corrupted, seconds, picture size. Numbers only.
// Real headless Chromium, fake extension API, a real (tiny, silent) media file; the frame counters are faked because a silent WAV has no picture (they grow 25 per second of playback, 10 % dropped).
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const CONTENT = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'content.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required'] }); });
after(async () => { await browser.close(); });

function wav(seconds) {
  const n = 8000 * seconds;
  const h = Buffer.alloc(44);
  h.write('RIFF', 0); h.writeUInt32LE(36 + n, 4); h.write('WAVEfmt ', 8); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
  h.writeUInt32LE(8000, 24); h.writeUInt32LE(8000, 28); h.writeUInt16LE(1, 32); h.writeUInt16LE(8, 34); h.write('data', 36); h.writeUInt32LE(n, 40);
  return Buffer.concat([h, Buffer.alloc(n, 0x80)]);
}
const MEDIA = wav(40);

async function open(body, withQuality = true) {
  const context = await browser.newContext({ viewport: { width: 500, height: 600 } });
  await context.route('https://**/*', (r) => {
    if (r.request().url().endsWith('.wav')) return r.fulfill({ contentType: 'audio/wav', body: MEDIA });
    return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` });
  });
  const page = await context.newPage();
  await page.addInitScript((q) => {
    window.__msgs = [];
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } };
    if (q) HTMLVideoElement.prototype.getVideoPlaybackQuality = function () { const f = Math.round(this.currentTime * 25); return { totalVideoFrames: f, droppedVideoFrames: Math.round(f * 0.1), corruptedVideoFrames: 0 }; };
  }, withQuality);
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/page');
  return page;
}
const vq = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'vq'));
const playFor = async (page, ms) => { await page.evaluate(() => document.getElementById('v').play()); await page.waitForFunction(() => document.getElementById('v').currentTime > 0.2, null, { timeout: 8000 }); await page.waitForTimeout(ms); };

test('pausing after a few seconds sends one look: the frames and the dropped ones since the video started, as numbers only', async () => {
  const page = await open('<video id="v" src="https://cdn.example/movie.wav" muted style="width:300px;height:170px"></video>');
  await playFor(page, 3000);
  await page.evaluate(() => document.getElementById('v').pause());
  await page.waitForTimeout(300);
  const m = await vq(page);
  assert.equal(m.length, 1, JSON.stringify(m));
  assert.deepEqual(Object.keys(m[0]).sort(), ['c', 'd', 'h', 'k', 's', 't', 'w']);
  for (const k of ['t', 'd', 'c', 's', 'w', 'h']) assert.equal(typeof m[0][k], 'number', k);
  assert.ok(m[0].t >= 60 && m[0].t <= 130, 'frames ' + m[0].t);       // about 25 per second of playback
  assert.ok(m[0].d >= 6 && m[0].d <= 13, 'dropped ' + m[0].d);       // 10 % of them
  assert.ok(m[0].s >= 2 && m[0].s <= 6, 'seconds ' + m[0].s);
  assert.ok(!JSON.stringify(m[0]).includes('cdn.example') && !JSON.stringify(m[0]).includes('movie'), 'no address in the message');
});

test('a second stretch of playing sends only what is new', async () => {
  const page = await open('<video id="v" src="https://cdn.example/movie.wav" muted style="width:300px;height:170px"></video>');
  await playFor(page, 2500);
  await page.evaluate(() => document.getElementById('v').pause());
  await page.waitForTimeout(200);
  const first = (await vq(page))[0];
  assert.ok(first && first.t >= 45, 'first look ' + JSON.stringify(first));
  await playFor(page, 2500);
  await page.evaluate(() => document.getElementById('v').pause());
  await page.waitForTimeout(300);
  const all = await vq(page);
  assert.equal(all.length, 2, JSON.stringify(all));
  assert.ok(all[1].t >= 45 && all[1].t <= 110, 'second look only the new frames: ' + all[1].t);
});

test('too short to say anything: no message', async () => {
  const page = await open('<video id="v" src="https://cdn.example/movie.wav" muted style="width:300px;height:170px"></video>');
  await page.evaluate(() => document.getElementById('v').play());
  await page.waitForFunction(() => document.getElementById('v').currentTime > 0.1, null, { timeout: 8000 });
  await page.evaluate(() => document.getElementById('v').pause());
  await page.waitForTimeout(400);
  assert.deepEqual(await vq(page), []);
});

test('hiding the page sends the look of a video that is still playing', async () => {
  const page = await open('<video id="v" src="https://cdn.example/movie.wav" muted style="width:300px;height:170px"></video>');
  await playFor(page, 2500);
  await page.evaluate(() => { Object.defineProperty(document, 'hidden', { value: true, configurable: true }); document.dispatchEvent(new Event('visibilitychange')); });
  await page.waitForTimeout(300);
  const m = await vq(page);
  assert.equal(m.length, 1, JSON.stringify(m));
  assert.ok(m[0].t >= 45, 'frames ' + m[0].t);
});

test('an engine without getVideoPlaybackQuality sends nothing and breaks nothing', async () => {
  const page = await open('<video id="v" src="https://cdn.example/movie.wav" muted style="width:300px;height:170px"></video>', false);
  await page.evaluate(() => { HTMLVideoElement.prototype.getVideoPlaybackQuality = undefined; });
  await playFor(page, 2500);
  await page.evaluate(() => document.getElementById('v').pause());
  await page.waitForTimeout(300);
  assert.deepEqual(await vq(page), []);
});
