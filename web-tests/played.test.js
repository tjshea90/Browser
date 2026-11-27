'use strict';
// RX3-V3 (research/REVIEW-EXTERNAL-3.md part V): which element actually PLAYS. content.js listens for `playing` and, after 3 s of real playback, sends {k:'vid', ev:'played', el, ...facts}; `el` is a small
// whole number that names the element for the page's lifetime, so the app can tell "the same player changed from a 15 s clip to the film" (a pre-roll) from "two different videos".
// Real headless Chromium, fake extension API, real (tiny, silent) media files.
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
    if (u.endsWith('/long.wav') || u.endsWith('/long2.wav')) return r.fulfill({ contentType: 'audio/wav', body: LONG });
    if (u.endsWith('.wav')) return r.fulfill({ contentType: 'audio/wav', body: SHORT });
    return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` });
  });
  const page = await context.newPage();
  await page.addInitScript(() => { window.__msgs = []; window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } }; });
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/page');
  return page;
}
const played = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'vid' && m.ev === 'played'));
const meta = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'vid' && !m.ev));

test('an element that really plays for 3 s reports it once, with the facts, a length and its element number', async () => {
  const page = await open('<video id="v" src="https://cdn.example/long.wav" muted autoplay controls style="width:300px;height:170px"></video>');
  await page.waitForFunction(() => window.__msgs.some((m) => m.ev === 'played'), null, { timeout: 12000 });
  const p = await played(page);
  assert.equal(p.length, 1);
  assert.equal(p[0].u, 'https://cdn.example/long.wav'); assert.ok(p[0].d >= 29 && p[0].d <= 31, 'length ' + p[0].d);
  assert.ok(Number.isInteger(p[0].el) && p[0].el >= 1);
  assert.ok(Number.isInteger(p[0].f) && p[0].rw === 300 && p[0].rh === 170 && p[0].vp === 100);
  const m = await meta(page);
  assert.equal(m.length, 1); assert.equal(m[0].el, p[0].el, 'the metadata report names the same element');
  await page.waitForTimeout(1500);
  assert.equal((await played(page)).length, 1, 'once per element and file');
  await page.context().close();
});

test('a clip that is paused before 3 s of playback is never reported as played', async () => {
  const page = await open('<video id="v" src="https://cdn.example/long.wav" muted autoplay style="width:300px;height:170px"></video>');
  await page.waitForFunction(() => { const v = document.getElementById('v'); return v && v.currentTime > 0.3; }, null, { timeout: 8000 });
  await page.evaluate(() => document.getElementById('v').pause());
  await page.waitForTimeout(3800);
  assert.equal((await played(page)).length, 0);
  await page.context().close();
});

test('the same element that plays another file later reports it with the SAME element number (a pre-roll, then the film)', async () => {
  const page = await open('<video id="v" src="https://cdn.example/short.wav" muted autoplay style="width:300px;height:170px"></video>');
  await page.waitForFunction(() => window.__msgs.some((m) => m.ev === 'played'), null, { timeout: 12000 });
  await page.evaluate(() => { const v = document.getElementById('v'); v.src = 'https://cdn.example/long.wav'; v.play(); });
  await page.waitForFunction(() => window.__msgs.filter((m) => m.ev === 'played').length >= 2, null, { timeout: 12000 });
  const p = await played(page);
  assert.equal(p.length, 2);
  assert.equal(p[0].el, p[1].el, 'one element');
  assert.ok(p[0].d < 10 && p[1].d > 25, 'the first was the short clip: ' + p[0].d + ' then ' + p[1].d);
  await page.context().close();
});

test('two different elements get different numbers', async () => {
  const page = await open('<video src="https://cdn.example/long.wav" muted autoplay style="width:200px;height:100px"></video><video src="https://cdn.example/long2.wav" muted autoplay style="width:200px;height:100px"></video>');
  await page.waitForFunction(() => window.__msgs.filter((m) => m.ev === 'played').length >= 2, null, { timeout: 12000 });
  const p = await played(page);
  assert.notEqual(p[0].el, p[1].el);
  await page.context().close();
});

test('a seek or a loop of the same file does not report it again', async () => {
  const page = await open('<video id="v" src="https://cdn.example/short.wav" muted autoplay loop style="width:300px;height:170px"></video>');
  await page.waitForFunction(() => window.__msgs.some((m) => m.ev === 'played'), null, { timeout: 12000 });
  await page.evaluate(() => { document.getElementById('v').currentTime = 1; });
  await page.waitForTimeout(4500);
  assert.equal((await played(page)).length, 1);
  await page.context().close();
});

test('a slow tablet: playback that is a little behind 3 s of wall time is looked at again, not forgotten', async () => {
  const page = await open('<video id="v" src="https://cdn.example/long.wav" muted autoplay style="width:300px;height:170px"></video>');
  await page.evaluate(() => { document.getElementById('v').playbackRate = 0.5; });          // the clock runs at half speed: 3 s of waiting is 1.5 s of film
  await page.waitForFunction(() => window.__msgs.some((m) => m.ev === 'played'), null, { timeout: 15000 });
  assert.equal((await played(page)).length, 1);
  await page.context().close();
});
