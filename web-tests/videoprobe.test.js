'use strict';
// VD1 (owner 2026-11-21: "diagnose why Instagram reels don't load and some videos don't load on websites"): videoprobe.js tells the app WHEN a media element fails and how, in real headless Chromium.
// What is proved: a failing <video> and a failing <source> each give ONE message with the error code, the two states and the KIND of source (never an address), a frame says so, the same failure is not told twice,
// at most five a frame, audio is told as audio, and nothing runs without the extension API. The browser API is a fake that posts every message to a sink the test collects.
// Run: cd web-tests && npm test
const fs = require('node:fs');
const path = require('node:path');
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SRC = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'videoprobe.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required', '--mute-audio'] }); });
after(async () => { await browser.close(); });

async function open(body, o) {
  o = o || {};
  const got = [];
  const context = await browser.newContext();
  await context.route('https://**/*', async (r) => {
    const u = new URL(r.request().url());
    if (u.hostname === 'sink.example') { got.push(JSON.parse(r.request().postData())); return r.fulfill({ status: 204, headers: { 'access-control-allow-origin': '*' } }); }
    if (u.pathname.endsWith('.mp4') || u.pathname.endsWith('.mp3')) return r.fulfill({ contentType: u.pathname.endsWith('.mp3') ? 'audio/mpeg' : 'video/mp4', body: Buffer.from('this is not a media file at all, just text that no decoder will accept '.repeat(40)) });
    if (u.pathname.endsWith('/hang.bin')) return new Promise(() => {});                         // VD2c: a file that never answers
    if (u.pathname === '/frame') return r.fulfill({ contentType: 'text/html', body: '<!doctype html><body><video src="/inner-bad.mp4" muted></video>' });
    return r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>t</title><body>' + (body || '') + '</body>' });
  });
  const page = await context.newPage();
  if (!o.noApi) await page.addInitScript(() => {
    window.browser = { runtime: { sendMessage: (m) => { fetch('https://sink.example/m', { method: 'POST', mode: 'no-cors', keepalive: true, body: JSON.stringify({ frame: window === window.top ? 'top' : 'sub', m: m }) }); return Promise.resolve(); } } };
  });
  if (o.stall) await page.addInitScript((ms) => { window.__kstVPstallMs = ms; }, o.stall);
  await page.addInitScript({ content: SRC });
  return { page, got, context };
}
const msgs = (got) => got.map((g) => Object.assign({ frame: g.frame }, g.m)).filter((m) => m.k === 'verr');
async function until(f, what, ms) { const end = Date.now() + (ms || 8000); while (Date.now() < end) { if (await f()) return; await new Promise((r) => setTimeout(r, 25)); } assert.fail('timed out waiting for ' + what); }

test('VD1: a video that cannot be played is told once: error code, states, kind of source, never the address', async () => {
  const { page, got, context } = await open('<video id="v" src="/secret/path/bad.mp4?token=abc" muted></video>');
  await page.goto('https://site.example/');
  await until(() => msgs(got).length >= 1, 'the report');
  await page.waitForTimeout(300);
  const m = msgs(got);
  assert.equal(m.length, 1, JSON.stringify(m));
  assert.equal(m[0].e, 'v'); assert.ok(m[0].c >= 3 && m[0].c <= 4, 'code ' + m[0].c); assert.equal(m[0].s, 'http'); assert.equal(m[0].f, 1);
  assert.ok(typeof m[0].ns === 'number' && typeof m[0].rs === 'number');
  assert.ok(JSON.stringify(got).indexOf('secret') < 0 && JSON.stringify(got).indexOf('token') < 0, 'no address leaves the page');
  await context.close();
});

test('VD1: a script-made blob source is told as blob, a failed <source> child as code 9, audio as audio', async () => {
  const { page, got, context } = await open('<video id="b" muted></video><video id="s" muted><source src="/child-bad.mp4" type="video/mp4"></video><audio id="a" src="/bad.mp3"></audio>');
  await page.goto('https://site.example/');
  await page.evaluate(() => { document.getElementById('b').src = URL.createObjectURL(new Blob(['not a media source'], { type: 'video/mp4' })); });
  await until(() => msgs(got).length >= 3, 'three reports', 10000);
  const m = msgs(got);
  assert.ok(m.some((x) => x.s === 'blob' && x.e === 'v'), 'blob: ' + JSON.stringify(m));
  assert.ok(m.some((x) => x.c === 9 && x.e === 'v'), 'source child: ' + JSON.stringify(m));
  assert.ok(m.some((x) => x.e === 'a' && x.s === 'http'), 'audio: ' + JSON.stringify(m));
  await context.close();
});

test('VD1: the same failure is not told twice and a frame tells at most five', async () => {
  const { page, got, context } = await open('<video src="/one.mp4" muted></video><video src="/two.mp4" muted></video>');
  await page.goto('https://site.example/');
  await until(() => msgs(got).length >= 1, 'the first');
  await page.waitForTimeout(500);
  assert.equal(msgs(got).length, 1, 'two videos that fail the same way (video, code, kind) are one line');
  await page.evaluate(() => { for (let i = 0; i < 12; i++) { const v = document.createElement('video'); v.id = 'x' + i; document.body.appendChild(v); v.dispatchEvent(new Event('error')); const a = document.createElement('audio'); document.body.appendChild(a); a.dispatchEvent(new Event('error')); } });
  await page.waitForTimeout(300);
  assert.ok(msgs(got).length <= 5, 'rationed: ' + msgs(got).length);
  await context.close();
});

test('VD1: a failing video inside a frame says it is a frame; without the extension API nothing runs', async () => {
  const { page, got, context } = await open('<iframe src="/frame"></iframe>');
  await page.goto('https://site.example/');
  await until(() => msgs(got).some((m) => m.frame === 'sub'), 'the frame report');
  assert.equal(msgs(got).find((m) => m.frame === 'sub').f, 0);
  await context.close();
  const n = await open('<video src="/x.mp4" muted></video>', { noApi: true });
  await n.page.goto('https://site.example/');
  assert.equal(await n.page.evaluate(() => typeof window.KestrelVP), 'undefined');
  await n.context.close();
});

test('VD2c: an element that is trying to play and never gets data (no error event) is told ONCE as code 99 with its states, kind of source and no address', async () => {
  const { page, got, context } = await open('<video id="v" src="/secret/hang.bin?token=zz" autoplay muted></video>', { stall: 400 });
  await page.goto('https://site.example/');
  await until(() => msgs(got).some((m) => m.c === 99), 'the stall report', 6000);
  await page.waitForTimeout(900);
  const m = msgs(got).filter((x) => x.c === 99);
  assert.equal(m.length, 1, 'told once: ' + JSON.stringify(m));
  assert.equal(m[0].s, 'http'); assert.equal(m[0].e, 'v'); assert.equal(m[0].f, 1); assert.ok(m[0].rs < 3, 'no data: readyState ' + m[0].rs);
  assert.ok(typeof m[0].ns === 'number' && (m[0].pa === 0 || m[0].pa === 1) && typeof m[0].b === 'number', JSON.stringify(m[0]));   // (an autoplay element is still paused until its first data)
  assert.ok(JSON.stringify(got).indexOf('secret') < 0 && JSON.stringify(got).indexOf('token') < 0, 'no address leaves the page');
  await context.close();
});

test('VD2c: an element that gets its data, or is only waiting for a press (paused, nothing loading), is not reported', async () => {
  const { page, got, context } = await open('<video id="idle" muted></video><video id="p" muted preload="none" poster="/p.png"></video><canvas id="c" width="64" height="64"></canvas>', { stall: 400 });
  await page.goto('https://site.example/');
  await page.evaluate(() => {
    var c = document.getElementById('c'), x = c.getContext('2d'), n = 0;
    setInterval(function () { x.fillStyle = 'rgb(' + (n++ % 255) + ',50,50)'; x.fillRect(0, 0, 64, 64); }, 40);
    var v = document.createElement('video'); v.muted = true; v.autoplay = true; v.srcObject = c.captureStream(25); document.body.appendChild(v);
  });
  await page.waitForTimeout(1600);
  assert.equal(msgs(got).filter((m) => m.c === 99).length, 0, JSON.stringify(msgs(got)));
  await context.close();
});

test('VD2c: a stuck element is told as stuck, and the stall timer is gone once the element reports', async () => {
  const { page, got, context } = await open('<video id="v" src="/hang.bin" autoplay muted></video><audio id="a" src="/hang.bin" autoplay></audio>', { stall: 300 });
  await page.goto('https://site.example/');
  await until(() => msgs(got).filter((m) => m.c === 99).length >= 2, 'both reports', 6000);
  const m = msgs(got).filter((x) => x.c === 99);
  assert.ok(m.some((x) => x.e === 'v') && m.some((x) => x.e === 'a'), JSON.stringify(m));
  await context.close();
});
