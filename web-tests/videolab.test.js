'use strict';
// The bundled Video lab page (geckoapp/src/main/assets/labs/video.html) in real headless Chromium: it must run without a script error, list every format, and its WebAssembly feature
// probes must be valid byte sequences (Chromium supports all of them, so a "no" here means a typo in the bytes, not a missing feature).
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const PAGE = 'file://' + path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'labs', 'video.html');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

test('the page runs, lists the formats and every WebAssembly feature probe is a valid module', async () => {
  const ctx = await browser.newContext({ viewport: { width: 600, height: 900 } });
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', (e) => errors.push(String(e)));
  await page.goto(PAGE);
  const can = await page.textContent('#can');
  for (const f of ['H.264 baseline', 'H.265 (HEVC)', 'VP9', 'AV1', 'AAC', 'Opus (WebM)', 'FLAC', 'AC-3', 'HLS playlist']) assert.ok(can.includes(f), 'format row: ' + f);
  assert.ok(can.includes('MediaSource:'), 'the capability line');
  const js = await page.textContent('#js');
  for (const k of ['fast math (SIMD)', 'bulk memory', 'multi-value', 'reference types', 'sign extension', 'threads (atomics)', 'tail calls']) assert.ok(js.includes(k + ': yes'), 'wasm probe ' + k + ' must validate in Chromium:\n' + js);
  assert.ok(/number crunching[^\n]*\d+ ms/.test(js), 'the speed lines');
  assert.ok((await page.textContent('#gl')).includes('webgl'), 'the graphics lines');
  assert.deepEqual(errors, [], 'no script error');
  await ctx.close();
});

test('Run: the screen test, the capability table and the clip test finish with a line per clip, even when a format is not offered', async () => {
  const ctx = await browser.newContext({ viewport: { width: 600, height: 900 } });
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', (e) => errors.push(String(e)));
  await page.goto(PAGE);
  await page.click('#run');
  await page.waitForFunction(() => document.getElementById('run').textContent === 'Run again', null, { timeout: 90000 });
  const play = await page.textContent('#play');
  for (const n of ['H.264 720p 30 fps', 'H.264 1080p 30 fps', 'VP9 720p 30 fps', 'H.265 720p 30 fps']) assert.ok(play.includes(n), 'clip line: ' + n + '\n' + play);
  assert.ok(/frames per second over 3 s/.test(await page.textContent('#raf')), 'the screen test');
  assert.ok((await page.textContent('#mc')).includes('video'), 'the capability table');
  assert.deepEqual(errors, [], 'no script error');
  await ctx.close();
});
