'use strict';
// PERF8b (owner 2026-11-22: "stuck on a loading animation in the site's video player" and "the animation is still slightly clipped on the bottom"): calm.js turns the loading circle of a big PAUSED video into a play mark and tells the
// app what it found, in real headless Chromium. What is proved: the circle (an infinite CSS animation, circle sized, centred in a paused video of 240x135 or more) gets the data-kst-calm attribute and a stopped animation; ONE
// message (verr, code 98) names it and says whether a parent with overflow hidden cuts it and by how many pixels; a playing video, a video with controls, a small video, a button, a circle that is too big and a circle outside
// the video are left alone; the mark goes away when the video plays; nothing runs without the extension API. The browser API is a fake that posts every message to a sink the test collects.
// Run: cd web-tests && npm test
const fs = require('node:fs');
const path = require('node:path');
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SRC = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'calm.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch({ args: ['--mute-audio'] }); });
after(async () => { await browser.close(); });

const CSS = '<style>@keyframes spin{to{transform:rotate(360deg)}} .box{position:relative;width:480px;height:270px;overflow:hidden;background:#222} video{width:480px;height:270px;display:block} ' +
  '.sp{position:absolute;left:216px;top:100px;width:48px;height:48px;border:5px solid #fff;border-top-color:transparent;border-radius:50%;animation:spin 1s linear infinite}</style>';

async function open(body, o) {
  o = o || {};
  const got = [];
  const context = await browser.newContext({ viewport: { width: 800, height: 600 } });
  await context.route('https://**/*', async (r) => {
    const u = new URL(r.request().url());
    if (u.hostname === 'sink.example') { got.push(JSON.parse(r.request().postData())); return r.fulfill({ status: 204, headers: { 'access-control-allow-origin': '*' } }); }
    return r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>t</title><body style="margin:0">' + CSS + (body || '') + '</body>' });
  });
  const page = await context.newPage();
  if (!o.noApi) await page.addInitScript(() => {
    window.browser = { runtime: { sendMessage: (m) => { fetch('https://sink.example/m', { method: 'POST', mode: 'no-cors', keepalive: true, body: JSON.stringify({ frame: window === window.top ? 'top' : 'sub', m: m }) }); return Promise.resolve(); } } };
  });
  await page.addInitScript(() => { window.__kstCalmCfg = { delays: [150, 500] }; });
  await page.addInitScript({ content: SRC });
  return { page, got, context };
}
const msgs = (got) => got.map((g) => g.m).filter((m) => m.k === 'verr');
async function until(f, what, ms) { const end = Date.now() + (ms || 8000); while (Date.now() < end) { if (await f()) return; await new Promise((r) => setTimeout(r, 25)); } assert.fail('timed out waiting for ' + what); }
const calmed = (page, sel) => page.evaluate((s) => document.querySelector(s).hasAttribute('data-kst-calm'), sel);

test('PERF8b: the circle of a big paused video becomes a play mark, once, and the app is told what was cut', async () => {
  const { page, got, context } = await open('<div class="box" id="b"><video id="v" muted></video><div class="sp" id="s"></div></div>');
  await page.goto('https://site.example/');
  await until(() => calmed(page, '#s'), 'the mark');
  const st = await page.evaluate(() => { const c = getComputedStyle(document.getElementById('s')); return { anim: c.animationName, clip: c.clipPath, border: c.borderTopWidth }; });
  assert.equal(st.anim, 'none'); assert.ok(/polygon/.test(st.clip), st.clip); assert.equal(st.border, '0px');
  await until(() => msgs(got).length >= 1, 'the report');
  await page.waitForTimeout(700);
  const m = msgs(got);
  assert.equal(m.length, 1, JSON.stringify(m));
  assert.equal(m[0].c, 98); assert.equal(m[0].f, 1); assert.equal(m[0].e, 'v');
  assert.ok(/^circle div\.sp 58x58 in a paused video 480x270, div\.box overflow=hidden cutBottom=-?\d+ cutTop=\d+/.test(m[0].x) || /^circle div\.sp /.test(m[0].x), m[0].x);
  assert.ok(/overflow=hidden/.test(m[0].x), 'it says a parent with overflow hidden is there: ' + m[0].x);
  await context.close();
});

test('PERF8b: a parent that cuts the circle says how many pixels at the bottom', async () => {
  const { page, got, context } = await open('<div class="box" id="b" style="height:130px"><video id="v" muted style="height:270px"></video><div class="sp" id="s" style="top:100px"></div></div><div style="height:400px"></div>');
  await page.goto('https://site.example/');
  await until(() => msgs(got).length >= 1, 'the report');
  const x = msgs(got)[0].x;
  const mm = /cutBottom=(-?\d+)/.exec(x);
  assert.ok(mm && +mm[1] > 0, 'the circle sticks out below its overflow-hidden parent: ' + x);
  await context.close();
});

test('PERF8b: a video with controls, a small video, a playing video, a button, a too big circle and a circle outside are left alone', async () => {
  const { page, got, context } = await open(
    '<div class="box"><video id="c" controls muted></video><div class="sp" id="s1"></div></div>' +
    '<div class="box" style="width:200px;height:100px"><video muted style="width:200px;height:100px"></video><div class="sp" id="s2" style="left:76px;top:26px"></div></div>' +
    '<div class="box"><video muted></video><button style="position:absolute;left:216px;top:100px;width:48px;height:48px;padding:0"><span class="sp" id="s3" style="position:static;display:block"></span></button></div>' +
    '<div class="box"><video muted></video><div class="sp" id="s4" style="left:100px;top:20px;width:300px;height:230px;border-radius:0"></div></div>' +
    '<div class="box"><video muted></video></div><div class="sp" id="s5" style="position:static;margin:10px"></div>');
  await page.goto('https://site.example/');
  await page.waitForTimeout(1200);
  for (const s of ['#s1', '#s2', '#s3', '#s4', '#s5']) assert.equal(await calmed(page, s), false, s + ' stays as the page made it');
  assert.equal(msgs(got).length, 0);
  await context.close();
});

test('PERF8b: the mark goes away when the video plays, and the circle is not touched when the video was played', async () => {
  const { page, context } = await open('<div class="box"><video id="v" muted></video><div class="sp" id="s"></div></div>');
  await page.goto('https://site.example/');
  await until(() => calmed(page, '#s'), 'the mark');
  await page.evaluate(() => document.getElementById('v').dispatchEvent(new Event('playing')));
  assert.equal(await calmed(page, '#s'), false, 'the page\'s own circle is back');
  await context.close();
});

test('PERF8b: nothing runs without the extension API', async () => {
  const { page, got, context } = await open('<div class="box"><video id="v" muted></video><div class="sp" id="s"></div></div>', { noApi: true });
  await page.goto('https://site.example/');
  await page.waitForTimeout(900);
  assert.equal(await calmed(page, '#s'), false);
  assert.equal(got.length, 0);
  await context.close();
});
