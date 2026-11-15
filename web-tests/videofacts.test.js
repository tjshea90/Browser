'use strict';
// RX3-V1 (research/REVIEW-EXTERNAL-3.md part V, research/FINDER_LAB.md section 1): content.js sends FACTS with every video metadata report, so that the app can tell a hover preview, a hero loop or an ad
// from a video somebody watches: muted / loop / autoplay / controls / visible / ad as bit flags, the rendered size, and how much of the box is inside the viewport. Real headless Chromium, fake extension API.
// (Own file: content.test.js is already near the 60 s per-file limit of the node test runner.)
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

const MUTED = 1, LOOP = 2, AUTOPLAY = 4, CONTROLS = 8, VISIBLE = 16, AD = 32;

function wav(seconds) {
  const n = 8000 * seconds;
  const h = Buffer.alloc(44);
  h.write('RIFF', 0); h.writeUInt32LE(36 + n, 4); h.write('WAVEfmt ', 8); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
  h.writeUInt32LE(8000, 24); h.writeUInt32LE(8000, 28); h.writeUInt16LE(1, 32); h.writeUInt16LE(8, 34); h.write('data', 36); h.writeUInt32LE(n, 40);
  return Buffer.concat([h, Buffer.alloc(n, 0x80)]);
}
const AUDIO = wav(3);

/** A page with [body]; its videos load https://cdn.example/clip.wav (a real, tiny, silent media file the engine really loads, so loadedmetadata really fires). */
async function open(body, opts) {
  opts = opts || {};
  const context = await browser.newContext({ viewport: { width: opts.w || 500, height: opts.h || 600 } });
  await context.route('https://**/*', (r) => {
    if (r.request().url().endsWith('.wav')) return r.fulfill({ contentType: 'audio/wav', body: AUDIO });
    return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` });
  });
  const page = await context.newPage();
  await page.addInitScript(() => { window.__msgs = []; window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } }; });
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/page');
  await page.waitForFunction(() => window.__msgs.some((m) => m.k === 'vid'), null, { timeout: 8000 });
  return page;
}
const vid = async (page) => { const m = await page.evaluate(() => window.__msgs.filter((x) => x.k === 'vid')[0]); assert.ok(Number.isInteger(m.f), 'the report carries the facts: ' + JSON.stringify(m)); return m; };
const SRC = 'src="https://cdn.example/clip.wav"';

test('a hover preview: muted, looping, autoplaying, no controls, visible, with its rendered size', async () => {
  const page = await open(`<video ${SRC} muted loop autoplay style="width:300px;height:170px"></video>`);
  const m = await vid(page);
  assert.equal(m.f & MUTED, MUTED); assert.equal(m.f & LOOP, LOOP); assert.equal(m.f & AUTOPLAY, AUTOPLAY); assert.equal(m.f & CONTROLS, 0);
  assert.equal(m.f & VISIBLE, VISIBLE); assert.equal(m.f & AD, 0);
  assert.equal(m.rw, 300); assert.equal(m.rh, 170); assert.equal(m.vp, 100);
  assert.equal(m.u, 'https://cdn.example/clip.wav'); assert.ok(m.p.startsWith('https://site.example'), 'the old fields are still there');
  await page.context().close();
});

test('a video with the native controls attribute has the controls bit; one without sound off does not have the muted bit', async () => {
  const page = await open(`<video ${SRC} controls style="width:300px;height:170px"></video>`);
  const m = await vid(page);
  assert.equal(m.f & CONTROLS, CONTROLS); assert.equal(m.f & MUTED, 0); assert.equal(m.f & LOOP, 0);
  await page.context().close();
});

test('a player with its OWN control bar (no controls attribute) counts as having controls: jwplayer, video-js, plyr, html5-video-player around it', async () => {
  for (const root of ['jwplayer jw-reset', 'video-js vjs-default-skin', 'plyr plyr--video', 'html5-video-player', 'mejs__container']) {
    const page = await open(`<div class="${root}"><div class="inner"><video ${SRC} muted autoplay style="width:300px;height:170px"></video></div></div>`);
    const m = await vid(page);
    assert.equal(m.f & CONTROLS, CONTROLS, root + ' is a player with its own controls');
    assert.equal(m.f & AD, 0, root + ' is not an ad');
    await page.context().close();
  }
});

test('the ad flag: ad words as WHOLE tokens of an ancestor class or id (up to 8 levels), and the ad-state classes of the players', async () => {
  const ads = ['<div class="ad-container">', '<div class="banner-ad top">', '<div id="adslot-3">', '<div class="advert">', '<div class="advertisement">', '<div class="ads">',
    '<div class="jwplayer jw-flag-ads">', '<div class="video-js vjs-ad-playing">', '<div class="ima-ad-container">', '<div class="plyr__ads">', '<div class="html5-video-player ad-showing">',
    '<section id="x"><div><div><div><div class="ad">'];                       // the ad word 5 levels up
  for (const open1 of ads) {
    const closes = (open1.match(/<div|<section/g) || []).length;
    const page = await open(`${open1}<video ${SRC} controls style="width:300px;height:170px"></video>${'</div>'.repeat(closes - (open1.includes('<section') ? 1 : 0))}${open1.includes('<section') ? '</section>' : ''}`);
    assert.equal((await vid(page)).f & AD, AD, open1 + ' is an ad');
    await page.context().close();
  }
});

test('words that only CONTAIN "ad" are not ads: address, headline, add-to-cart, download, load-more, shadow, thread', async () => {
  const page = await open(`<div class="address-box headline add-to-cart download load-more shadow thread" id="loading_head"><video ${SRC} controls style="width:300px;height:170px"></video></div>`);
  assert.equal((await vid(page)).f & AD, 0);
  await page.context().close();
});

test('an ad word further than 8 levels away, or on the body, is not counted', async () => {
  const deep = '<div class="ad">' + '<div>'.repeat(9) + `<video ${SRC} controls style="width:300px;height:170px"></video>` + '</div>'.repeat(10);
  const page = await open(deep);
  assert.equal((await vid(page)).f & AD, 0);
  await page.context().close();
  const page2 = await open(`<script>document.body.className='ad-page';</script><video ${SRC} controls style="width:300px;height:170px"></video>`);
  assert.equal((await vid(page2)).f & AD, 0, 'a class on the body says nothing about one element');
  await page2.context().close();
});

test('a small box and a box below the fold report their size and how much of them is in view', async () => {
  const page = await open(`<video ${SRC} controls style="width:100px;height:50px"></video><div style="height:2000px"></div><video id="low" ${SRC.replace('clip', 'clip2')} controls style="width:300px;height:200px"></video>`);
  await page.waitForFunction(() => window.__msgs.filter((m) => m.k === 'vid').length >= 2, null, { timeout: 8000 });
  const all = await page.evaluate(() => window.__msgs.filter((m) => m.k === 'vid'));        // (the two files load at the same time: their reports come in either order)
  const small = all.find((m) => m.u.endsWith('/clip.wav')), low = all.find((m) => m.u.endsWith('/clip2.wav'));
  assert.equal(small.rw, 100); assert.equal(small.rh, 50); assert.equal(small.vp, 100);
  assert.equal(low.vp, 0, 'far below the visible part of the page');
  await page.context().close();
});

test('a video that is not shown at all has no visible bit and no size', async () => {
  const page = await open(`<video ${SRC} controls style="display:none"></video>`);
  const m = await vid(page);
  assert.equal(m.f & VISIBLE, 0); assert.equal(m.rw, 0); assert.equal(m.rh, 0);
  await page.context().close();
});

test('the facts are small whole numbers', async () => {
  const page = await open(`<video ${SRC} muted loop style="width:300.6px;height:170.4px"></video>`);
  const m = await vid(page);
  for (const k of ['f', 'rw', 'rh', 'vp']) assert.ok(Number.isInteger(m[k]) && m[k] >= 0 && m[k] < 100000, k + ' = ' + m[k]);
  assert.ok(m.f >= 0 && m.f < 64);
  await page.context().close();
});
