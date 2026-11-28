'use strict';
// RX3-G3 (research/REVIEW-EXTERNAL-3.md part G): the one-press-plays assist reaches a player inside a cross-origin frame WITHOUT a channel the embedding page can use.
// Before: guard-main.js (page world) in every frame obeyed a postMessage from window.parent, so any page could play videos and click buttons, same-site links and script-handled elements inside ANY frame
// it embeds, with no user action. Now: the parent's guard only signals "go" (a DOM event, no parameters); the parent's content.js acts only on its OWN trusted press record (RX3-G2), works out the frame
// and the place from THAT press, and asks the background page; the background relays to the child frame (tabs.sendMessage: a channel a web page cannot reach); the child's content.js plays its video or
// presses through at that place (never on a link or a form control). Same-frame assist stays in guard-main.js.
// content.js + guard-main.js run in the page's main world here (the real extension keeps them apart); the background page is played by the test (it relays {k:'assist'} to the frame whose address matches).
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const DIR = path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const CONTENT = fs.readFileSync(path.join(DIR, 'content.js'), 'utf8');
const GUARD = fs.readFileSync(path.join(DIR, 'guard-main.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

const AD = 'https://ads.example/landing?x=1';
const STUB_PLAY = 'HTMLMediaElement.prototype.play = function () { window.__plays = (window.__plays || 0) + 1; return Promise.resolve(); };';
const FRAME = '<iframe id="f" src="https://player.example/embed" style="position:absolute;left:0;top:0;width:400px;height:300px;border:0"></iframe>';
// these layers are VISIBLE (a faint tint): RX3-G4 switches an INVISIBLE layer off before the press, which would leave nothing for the assist (the path under test here) to do
const LAYER_AD = `<div id="layer" style="position:absolute;left:0;top:0;width:400px;height:300px;background:rgba(0,0,0,0.3)" onclick="window.open('${AD}')"></div>`;
const LAYER_QUIET = '<div id="layer" style="position:absolute;left:0;top:0;width:400px;height:300px;background:rgba(0,0,0,0.3)"></div>';

async function open(body, opts) {
  opts = opts || {};
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.hostname === 'movie.example' && u.pathname === '/') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>movie</title></head><body style="margin:0">${body}</body></html>` });
    if (u.hostname === 'player.example' && u.pathname === '/embed') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><body style="margin:0">${opts.embed || ''}</body></html>` });
    return r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>opened</title>opened' });
  });
  const page = await context.newPage();
  page.popups = [];
  context.on('page', (p) => page.popups.push(p));
  // the background page's part: relay an assist to the child frame whose address is the iframe's (the real one uses its frame table); only the extension can do this, a web page cannot
  await page.exposeFunction('__bg', async (m) => {
    if (m.k !== 'assist') return;
    const want = String(m.s || '').replace(/#.*$/, '');
    const frame = page.frames().find((f) => f !== page.mainFrame() && f.url().replace(/#.*$/, '') === want);
    if (frame) await frame.evaluate((mm) => { (window.__listeners || []).forEach((f) => f(mm, {})); }, m);
  });
  // the guard belongs in the page world, where there is no extension API (it stays inert wherever `browser.runtime` exists): it is loaded BEFORE the fake API below, which stands for the isolated world of content.js
  await page.addInitScript({ content: GUARD });
  await page.addInitScript((cfg) => {
    window.__msgs = []; window.__listeners = [];
    window.browser = { runtime: {
      sendMessage: (m) => {
        window.__msgs.push(m);
        if (m.k === 'assist') window.__bg(m);
        if (m.k === 'guard') return Promise.resolve({ on: !cfg.off, top: 'movie.example' });
        return Promise.resolve(undefined);
      },
      onMessage: { addListener: (f) => window.__listeners.push(f) },
    } };
  }, { off: !!opts.off });
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://movie.example/');
  return page;
}
const settle = (ms) => new Promise((r) => setTimeout(r, ms || 600));
const child = (page) => page.frames().find((f) => f.url().indexOf('player.example') >= 0);
const assists = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'assist'));
const embedOf = (inner) => ({ embed: inner + `<script>${STUB_PLAY}</script>` });

test('RX3-G3: a parent page that posts the old assist tag into an embedded frame gets no play and no click inside it', async () => {
  const page = await open(FRAME, embedOf('<video src="https://player.example/clip.mp4" style="width:400px;height:100px"></video><div id="b" style="width:400px;height:150px" onclick="window.__clicks=(window.__clicks||0)+1">a button of the frame</div>'));
  await settle(900);
  await page.evaluate(() => { const w = document.getElementById('f').contentWindow; w.postMessage('__kst_assist_v1|50|50', '*'); w.postMessage('__kst_assist_v1|50|180', '*'); w.postMessage('__kst_assist_v1|-1|-1', '*'); });
  await settle(900);
  const f = child(page);
  assert.equal(await f.evaluate(() => window.__plays || 0), 0, 'the frame\'s video was not started');
  assert.equal(await f.evaluate(() => window.__clicks || 0), 0, 'nothing in the frame was clicked');
});

test('RX3-G3: a forged "go" event in the parent without a real press of the owner sends nothing', async () => {
  const page = await open(FRAME, embedOf('<video src="https://player.example/clip.mp4" style="width:400px;height:300px"></video>'));
  await settle(900);
  await page.evaluate(() => { for (let i = 0; i < 4; i++) document.dispatchEvent(new CustomEvent('__kst_assist_v1', { detail: '10|10' })); });
  await settle(600);
  assert.deepEqual(await assists(page), []);
  assert.equal(await child(page).evaluate(() => window.__plays || 0), 0);
});

test('RX3-G3: a "go" event long after the last real press sends nothing (the press must be fresh)', async () => {
  const page = await open(FRAME + LAYER_QUIET, embedOf('<video src="https://player.example/clip.mp4" style="width:400px;height:300px"></video>'));
  await settle(900);
  await page.mouse.click(100, 100);                               // a real press on the layer over the frame
  await settle(2500);
  await page.evaluate(() => document.dispatchEvent(new CustomEvent('__kst_assist_v1')));
  await settle(400);
  assert.deepEqual(await assists(page), []);
});

test('RX3-G3: at most five assists per press, and the place comes from the owner\'s press, never from the event', async () => {
  const page = await open(FRAME + LAYER_QUIET, embedOf('<video src="https://player.example/clip.mp4" style="width:400px;height:300px"></video>'));
  await settle(900);
  await page.mouse.click(120, 80);
  await page.evaluate(() => { for (let i = 0; i < 9; i++) document.dispatchEvent(new CustomEvent('__kst_assist_v1', { detail: '399|299' })); });
  await settle(500);
  const a = await assists(page);
  assert.equal(a.length, 5, 'rate limited');
  for (const m of a) { assert.equal(m.x, 120); assert.equal(m.y, 80); assert.equal(m.s, 'https://player.example/embed'); }
});

test('AP1/RX3-G3: a player in a frame under a layer of the page: the refused press is passed to the frame, which starts its video', async () => {
  const page = await open(FRAME + LAYER_AD, embedOf('<video id="v" src="https://player.example/clip.mp4" style="width:400px;height:300px;background:#000"></video>'));
  await settle(900);
  await page.mouse.click(100, 100);
  await settle(1200);
  assert.equal(page.popups.length, 0, 'the ad window was refused');
  assert.equal(await child(page).evaluate(() => window.__plays || 0), 1, 'the frame started its own video');
  const a = await assists(page);
  assert.ok(a.length >= 1 && a[0].x === 100 && a[0].y === 100);
});

test('AP1/RX3-G3: a player in a frame that builds its video only after the first press gets the press passed through, at the owner\'s place', async () => {
  const page = await open(FRAME + LAYER_AD, embedOf('<div id="p" class="player" style="width:400px;height:300px;background:#111" onclick="window.__real=(window.__real||0)+1">play</div>'));
  await settle(900);
  await page.mouse.click(100, 100);
  await settle(1200);
  assert.equal(page.popups.length, 0);
  assert.equal(await child(page).evaluate(() => window.__real || 0), 1, 'the press reached the frame\'s play control');
});

test('RX3-G3: the frame never presses on a button, a link or a form control, however the assist was asked for', async () => {
  const page = await open(FRAME + LAYER_AD, embedOf('<button id="b" style="width:400px;height:150px" onclick="window.__clicks=(window.__clicks||0)+1">Allow</button><a id="a" href="https://player.example/x" onclick="window.__clicks=(window.__clicks||0)+10;return false" style="display:block;width:400px;height:150px">a link</a>'));
  await settle(900);
  await page.mouse.click(100, 60);                                // refused ad, assist -> the frame is asked to press at (100, 60), which is the button
  await settle(800);
  await page.mouse.click(100, 220);                               // ... and then the link
  await settle(800);
  assert.equal(await child(page).evaluate(() => window.__clicks || 0), 0);
});

test('RX3-G3: with the guard off for the site nothing is assisted and nothing is sent', async () => {
  const page = await open(FRAME + LAYER_AD, Object.assign(embedOf('<video src="https://player.example/clip.mp4" style="width:400px;height:300px"></video>'), { off: true }));
  await settle(900);
  const popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(100, 100);
  await popup;
  await settle(600);
  assert.deepEqual(await assists(page), []);
  assert.equal(await child(page).evaluate(() => window.__plays || 0), 0);
});

test('RX3-G3: a message the page itself sends to the extension\'s listener cannot exist: the frame only listens through the extension API, and only for an assist', async () => {
  const page = await open(FRAME, embedOf('<video src="https://player.example/clip.mp4" style="width:400px;height:300px"></video><div style="width:400px;height:100px" onclick="window.__clicks=(window.__clicks||0)+1"></div>'));
  await settle(900);
  const f = child(page);
  // an assist for the TOP page (not a frame) and a message of another kind are ignored by every listener
  await page.evaluate(() => (window.__listeners || []).forEach((l) => { l({ k: 'assist', x: 10, y: 10, s: '' }, {}); l({ k: 'other' }, {}); l(null, {}); }));
  await f.evaluate(() => (window.__listeners || []).forEach((l) => { l({ k: 'other' }, {}); l(null, {}); l({ k: 'assist', x: 'a', y: 'b' }, {}); }));
  await settle(500);
  assert.equal(await page.evaluate(() => window.__plays || 0), 0, 'the top page is never assisted by this channel');
});
