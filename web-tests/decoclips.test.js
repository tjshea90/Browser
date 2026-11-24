'use strict';
// PERF21 (owner 2026-11-24: "The animations on this page are very laggy and make the entire site laggy. They also each have a play button, which is incorrect for in site animations"): decoclips.js in real headless
// Chromium. A decorative clip (muted, looping, no controls) plays only while it is on screen and at most MAX at a time; the others are parked and come back when they are visible and there is room; anything that is
// not decorative is never touched; a page that pauses a clip itself, or keeps starting a parked one, is respected. The browser API is a fake that answers the `guard` question and records `vbk` reports.
// Run: cd web-tests && npm test
const fs = require('node:fs');
const path = require('node:path');
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const SRC = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'decoclips.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required', '--mute-audio'] }); });
after(async () => { await browser.close(); });

function wav(seconds) {
  const n = 8000 * seconds, h = Buffer.alloc(44);
  h.write('RIFF', 0); h.writeUInt32LE(36 + n, 4); h.write('WAVEfmt ', 8); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
  h.writeUInt32LE(8000, 24); h.writeUInt32LE(8000, 28); h.writeUInt16LE(1, 32); h.writeUInt16LE(8, 34); h.write('data', 36); h.writeUInt32LE(n, 40);
  return Buffer.concat([h, Buffer.alloc(n, 0x80)]);
}
const FILM = wav(30);
/** [n] clips: the first [vis] on screen (rows of 60 px), the rest far below the fold. Extra attributes per clip from [attrs]. */
function clips(n, vis, attrs) {
  let s = '';
  for (let i = 0; i < n; i++) s += `<video id="c${i}" ${(attrs && attrs[i]) || 'muted loop'} src="https://cdn.example/clip.wav" preload="auto" style="position:absolute;left:10px;top:${i < vis ? 10 + i * 70 : 1800 + i * 70}px;width:120px;height:60px;background:#333"></video>`;
  return s;
}

async function open(body, o) {
  o = o || {};
  const context = await browser.newContext({ viewport: { width: 400, height: 700 } });
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.pathname === '/clip.wav') {
      const rg = /bytes=(\d+)-(\d*)/.exec(r.request().headers().range || '');
      if (!rg) return r.fulfill({ contentType: 'audio/wav', headers: { 'accept-ranges': 'bytes' }, body: FILM });
      const a = +rg[1], b = rg[2] ? Math.min(+rg[2], FILM.length - 1) : FILM.length - 1;
      return r.fulfill({ status: 206, contentType: 'audio/wav', headers: { 'accept-ranges': 'bytes', 'content-range': `bytes ${a}-${b}/${FILM.length}` }, body: FILM.subarray(a, b + 1) });
    }
    return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><body style="margin:0;height:3600px">${body}</body></html>` });
  });
  const page = await context.newPage();
  await page.addInitScript((cfg) => {
    window.__kstDecoCfg = { tick: 60, wait: 20000 };
    window.__msgs = [];
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); if (m && m.k === 'guard') return Promise.resolve({ on: true, dc: cfg.dc !== false }); return Promise.resolve(undefined); } } };
  }, { dc: o.dc });
  await page.goto('https://site.example/');
  await page.waitForFunction(() => [...document.querySelectorAll('video')].every((v) => v.readyState >= 3), null, { timeout: 15000 });
  if (o.playBefore) await page.evaluate(() => { document.querySelectorAll('video').forEach((v) => v.play()); });   // started by the page before the script runs
  await page.addScriptTag({ content: SRC });
  if (!o.playBefore) await page.evaluate(() => { document.querySelectorAll('video').forEach((v) => v.play().catch(() => {})); });
  return { page, context };
}
const playing = (page) => page.evaluate(() => [...document.querySelectorAll('video')].filter((v) => !v.paused).map((v) => v.id));
async function until(f, what, ms) { const end = Date.now() + (ms || 6000); while (Date.now() < end) { if (await f()) return; await new Promise((r) => setTimeout(r, 40)); } assert.fail('timed out waiting for ' + what); }
const reports = (page) => page.evaluate(() => window.__msgs.filter((m) => m && m.k === 'vbk' && m.e === 'deco').map((m) => m.a));

test('PERF21: five decorative clips (three on screen, two far below): at most two play, only on screen, the others are parked', async () => {
  const { page, context } = await open(clips(5, 3), {});
  await until(async () => (await playing(page)).length <= 2, 'the cap');
  await page.waitForTimeout(400);
  const p = await playing(page);
  assert.ok(p.length === 2, 'two play: ' + p);
  assert.ok(p.every((id) => ['c0', 'c1', 'c2'].includes(id)), 'only clips on screen play: ' + p);
  const r = await reports(page);
  assert.ok(r.includes('cap') && r.includes('off'), 'the app is told both reasons: ' + r);
  await context.close();
});

test('PERF21: a parked clip comes back when it is on screen and there is room; a clip that leaves the screen is parked', async () => {
  const { page, context } = await open(clips(4, 2), { playBefore: true });
  await until(async () => { const p = await playing(page); return p.length === 2 && p.every((id) => id === 'c0' || id === 'c1'); }, 'two visible playing');
  await page.evaluate(() => window.scrollTo(0, 1800));                                   // now the far clips are on screen and the first two are not
  await until(async () => { const p = await playing(page); return p.length === 2 && p.every((id) => id === 'c2' || id === 'c3'); }, 'the clips that are on screen now play, the others are parked', 8000);
  await page.evaluate(() => window.scrollTo(0, 0));
  await until(async () => { const p = await playing(page); return p.length === 2 && p.every((id) => id === 'c0' || id === 'c1'); }, 'back at the top the first two play again', 8000);
  assert.ok((await reports(page)).includes('back'));
  await context.close();
});

test('PERF21: with the Settings switch off nothing is touched', async () => {
  const { page, context } = await open(clips(5, 5), { dc: false });
  await page.waitForTimeout(800);
  assert.equal((await playing(page)).length, 5);
  assert.deepEqual(await reports(page), []);
  await context.close();
});

test('PERF21: a clip with sound, one with controls and one that does not loop are never touched', async () => {
  const attrs = ['loop', 'muted loop controls', 'muted', 'muted loop', 'muted loop', 'muted loop'];   // c0 has sound, c1 own controls, c2 no loop, c3 c4 c5 decorative
  const { page, context } = await open(clips(6, 6, attrs), { playBefore: true });
  await until(async () => (await playing(page)).length === 5, 'the cap took one of the three decorative clips');
  await page.waitForTimeout(300);
  const p = await playing(page);
  assert.ok(['c0', 'c1', 'c2'].every((id) => p.includes(id)), 'the three that are not decorative still play: ' + p);
  assert.equal(p.filter((id) => ['c3', 'c4', 'c5'].includes(id)).length, 2, 'two of the three decorative ones play: ' + p);
  await context.close();
});

test('PERF21: a clip the page pauses itself stays paused, and the room it leaves goes to a parked clip', async () => {
  const { page, context } = await open(clips(3, 3), { playBefore: true });
  await until(async () => (await playing(page)).length === 2, 'the cap');
  await page.waitForTimeout(300);
  const state = await page.evaluate(() => ({ playing: [...document.querySelectorAll('video')].filter((v) => !v.paused).map((v) => v.id), parked: [...document.querySelectorAll('video')].filter((v) => v.paused).map((v) => v.id) }));
  assert.equal(state.playing.length, 2); assert.equal(state.parked.length, 1);
  const a = state.playing[0], x = state.parked[0];
  await page.evaluate((id) => document.getElementById(id).pause(), a);                       // the page's own decision about a playing clip
  await until(async () => (await playing(page)).includes(x), 'the parked clip takes the free room');
  await page.waitForTimeout(500);
  const p = await playing(page);
  assert.ok(!p.includes(a), 'the clip the page paused stays paused: ' + p);
  assert.ok(p.includes(x), 'and the parked one plays: ' + p);
  await context.close();
});

test('PERF21: a page that keeps starting a parked clip is left alone after four parkings (no tug of war)', async () => {
  const { page, context } = await open(clips(3, 3), { playBefore: true });
  await until(async () => (await playing(page)).length === 2, 'the cap');
  await page.evaluate(() => { const vs = [...document.querySelectorAll('video')]; window.__fight = setInterval(() => vs.forEach((v) => { if (v.paused) v.play().catch(() => {}); }), 90); });
  await page.waitForTimeout(3500);
  await page.evaluate(() => clearInterval(window.__fight));
  await page.waitForTimeout(500);
  const stable = await playing(page);
  await page.waitForTimeout(600);
  assert.deepEqual(await playing(page), stable, 'it settles: no clip is paused and played again for ever');
  const caps = (await reports(page)).filter((a) => a === 'cap' || a === 'off').length;
  assert.ok(caps <= 6, 'the reports are bounded: ' + caps);
  await context.close();
});

test('PERF21: no decorative clip, no timer and no message', async () => {
  const { page, context } = await open(clips(2, 2, ['muted', 'loop']), { playBefore: true });
  await page.waitForTimeout(500);
  assert.equal((await playing(page)).length, 2);
  assert.deepEqual(await reports(page), []);
  assert.equal(await page.evaluate(() => window.__msgs.filter((m) => m && m.k === 'guard').length), 0, 'the app is not even asked');
  await context.close();
});
