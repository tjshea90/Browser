'use strict';
// RX3-V4 (research/REVIEW-EXTERNAL-3.md part V): a player that feeds a blob: address (hls.js, shaka, dash.js: most embeds) used to report nothing, because content.js dropped every source that did not start with
// "http". Now it reports the word "blob" with the element's facts, length and number (never the blob: address itself), and says "played" after 3 s of real playback, so that the app can match the player to
// the playlist of its length (core MediaList.applyBlob).
// Real headless Chromium, fake extension API, a real (tiny, silent) file turned into a blob: address by the page.
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
const LONG = wav(30), SHORT = wav(6);

async function open(body) {
  const context = await browser.newContext({ viewport: { width: 500, height: 600 } });
  await context.route('https://**/*', (r) => {
    const u = r.request().url();
    if (u.endsWith('/long.wav')) return r.fulfill({ contentType: 'audio/wav', body: LONG });
    if (u.endsWith('/short.wav')) return r.fulfill({ contentType: 'audio/wav', body: SHORT });
    return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` });
  });
  const page = await context.newPage();
  await page.addInitScript(() => { window.__msgs = []; window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } }; });
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/page');
  return page;
}
const vids = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'vid'));
// the page turns a real file into a blob: address, like a player that built its own MediaSource-less blob would
const PLAY_BLOB = (file) => `(async () => { const b = await (await fetch('https://cdn.example/${file}')).blob(); const v = document.getElementById('v'); v.src = URL.createObjectURL(b); v.muted = true; v.play(); })()`;

test('a player that feeds a blob: address reports the word blob with its length and element number, never the address', async () => {
  const page = await open('<video id="v" controls style="width:300px;height:170px"></video>');
  await page.evaluate(PLAY_BLOB('long.wav'));
  await page.waitForFunction(() => window.__msgs.some((m) => m.k === 'vid'), null, { timeout: 8000 });
  const v = await vids(page);
  assert.equal(v.length, 1);
  assert.equal(v[0].u, 'blob');
  assert.ok(v[0].d >= 29 && v[0].d <= 31, 'length ' + v[0].d);
  assert.ok(Number.isInteger(v[0].el) && v[0].el >= 1);
  assert.equal(v[0].l, '', 'no label from a source element that cannot be named');
  assert.ok(JSON.stringify(await vids(page)).indexOf('blob:') < 0, 'the blob: address itself is never sent');
  await page.context().close();
});

test('the same blob player says "played" after 3 s of real playback, once, with the same element number', async () => {
  const page = await open('<video id="v" controls style="width:300px;height:170px"></video>');
  await page.evaluate(PLAY_BLOB('long.wav'));
  await page.waitForFunction(() => window.__msgs.some((m) => m.ev === 'played'), null, { timeout: 12000 });
  const v = await vids(page);
  const meta = v.filter((m) => !m.ev), played = v.filter((m) => m.ev === 'played');
  assert.equal(meta.length, 1); assert.equal(played.length, 1);
  assert.equal(played[0].u, 'blob'); assert.equal(played[0].el, meta[0].el);
  assert.ok(played[0].d >= 29 && played[0].d <= 31);
  await page.waitForTimeout(1500);
  assert.equal((await vids(page)).filter((m) => m.ev === 'played').length, 1, 'once per element, address and length');
  await page.context().close();
});

test('a second blob on the same element (a pre-roll, then the film) is a second report with the same element number', async () => {
  const page = await open('<video id="v" style="width:300px;height:170px"></video>');
  await page.evaluate(PLAY_BLOB('short.wav'));
  await page.waitForFunction(() => window.__msgs.some((m) => m.ev === 'played'), null, { timeout: 12000 });
  await page.evaluate(PLAY_BLOB('long.wav'));
  await page.waitForFunction(() => window.__msgs.filter((m) => m.ev === 'played').length >= 2, null, { timeout: 12000 });
  const p = (await vids(page)).filter((m) => m.ev === 'played');
  assert.equal(p.length, 2);
  assert.equal(p[0].el, p[1].el, 'one element');
  assert.ok(p[0].d < 10 && p[1].d > 25, 'the clip first: ' + p[0].d + ' then ' + p[1].d);
  assert.equal(p[0].u, 'blob'); assert.equal(p[1].u, 'blob');
  await page.context().close();
});

test('a blob paused before 3 s of playback is never reported as played, and an ordinary http source still reports its address', async () => {
  const page = await open('<video id="v" style="width:300px;height:170px"></video><video id="w" src="https://cdn.example/short.wav" muted style="width:100px;height:60px"></video>');
  await page.evaluate(PLAY_BLOB('long.wav'));
  await page.waitForFunction(() => { const v = document.getElementById('v'); return v && v.currentTime > 0.3; }, null, { timeout: 8000 });
  await page.evaluate(() => document.getElementById('v').pause());
  await page.waitForTimeout(3800);
  const v = await vids(page);
  assert.equal(v.filter((m) => m.ev === 'played').length, 0);
  assert.ok(v.some((m) => m.u === 'https://cdn.example/short.wav'), 'an http source is reported as before');
  await page.context().close();
});
