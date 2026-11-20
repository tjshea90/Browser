'use strict';
// VP2 (owner message 2026-11-18: "I selected three different video servers on doraby.Com but none of them would play. It showed no error, it just couldn't load the video"): the content script's two parts of
// "a frame the person's tap started": a trusted click in the TOP page is told to the background page (`ptap`, nothing else), and the `pp` of the guard answer reaches the page-side gate as `1p` / `1+p`.
// Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { launch, close, open, msgsAll } = require('./videobar-lib');

before(launch);
after(close);

const HTML = '<div style="position:fixed;left:0;top:0;width:300px;height:200px"><video id="v" src="https://cdn.example/film.wav" preload="auto" muted style="width:100%;height:100%"></video></div><button id="b" style="position:fixed;right:0;bottom:0;width:80px;height:40px">Play</button>';
const taps = (page) => page.evaluate(() => window.__msgs.filter((m) => m && m.k === 'ptap').length);

test('VP2: a real click in the top page sends ptap (and nothing about the page); a click made by the page\'s own script does not', async () => {
  const page = await open(HTML, { content: true, cfg: {} });
  await page.waitForTimeout(300);
  assert.equal(await taps(page), 0, 'nothing before a click');
  await page.evaluate(() => document.getElementById('b').dispatchEvent(new MouseEvent('click', { bubbles: true })));   // isTrusted false
  await page.waitForTimeout(100);
  assert.equal(await taps(page), 0, 'a script\'s click is not a press');
  await page.mouse.click(1280 - 40, 720 - 20);
  await page.waitForTimeout(150);
  assert.equal(await taps(page), 1);
  const m = await page.evaluate(() => window.__msgs.find((x) => x && x.k === 'ptap'));
  assert.deepEqual(Object.keys(m), ['k'], 'only the fact: ' + JSON.stringify(m));
  await page.context().close();
});

test('VP2: clicks are sent at most four a second; Enter and Space count, other keys do not', async () => {
  const page = await open(HTML, { content: true, cfg: {} });
  for (let i = 0; i < 6; i++) await page.mouse.click(1280 - 40, 720 - 20);
  await page.waitForTimeout(150);
  assert.ok((await taps(page)) <= 2, 'throttled: ' + (await taps(page)));
  const before = await taps(page);
  await page.waitForTimeout(300);
  await page.keyboard.press('a'); await page.waitForTimeout(100);
  assert.equal(await taps(page), before, 'a letter is no press');
  await page.keyboard.press('Enter'); await page.waitForTimeout(100);
  assert.equal(await taps(page), before + 1, 'Enter is');
  await page.context().close();
});

test('VP2: the guard answer\'s pp reaches the gate as 1p (1+p when the app may take a film over), and without pp it stays 1 / 1+', async () => {
  for (const [cfg, want] of [[{ pp: true }, '1p'], [{ pp: true, ho: true }, '1+p'], [{}, '1'], [{ ho: true }, '1+'], [{ gt: false }, '0']]) {
    const page = await open(HTML, { content: true, cfg, wait: false });
    await page.evaluate(() => { window.__gate = []; document.addEventListener('__kst_gate_cfg_v1', (e) => window.__gate.push(String(e.detail)), true); document.dispatchEvent(new CustomEvent('__kst_gate_ready_v1')); });
    await page.waitForFunction(() => window.__gate.length > 0, null, { timeout: 5000 });
    assert.equal((await page.evaluate(() => window.__gate))[0], want, JSON.stringify(cfg));
    await page.context().close();
  }
});
