'use strict';
// PERF3c (owner 2026-11-22: "typing is very laggy"): content.js tells the extension how long typed characters waited before the page saw them: numbers only, once 4 s after the last key, at most six times a page.
// Real headless Chromium with a fake `browser.runtime`. Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }
const CONTENT = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'content.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

async function open() {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><html><head><title>t</title></head><body><input id="i"><div id="d" contenteditable="true"></div><p>secret page text</p></body></html>' }));
  const page = await context.newPage();
  await page.addInitScript(() => { window.__msgs = []; window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } }; });
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/');
  return { page, context };
}
const typ = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'typ'));

test('PERF3c: typed characters are counted with their wait and told once after the typing stops, numbers only', async () => {
  const { page, context } = await open();
  await page.focus('#i');
  await page.keyboard.type('hello secret words', { delay: 20 });
  assert.equal((await typ(page)).length, 0, 'nothing is told while the typing goes on');
  await page.evaluate(() => window.KestrelTyp.tell());
  const m = (await typ(page))[0];
  assert.equal(m.k, 'typ'); assert.ok(m.n >= 15, 'keys ' + m.n); assert.ok(m.a >= 0 && m.m >= m.a && m.o >= 0 && m.f === 1, JSON.stringify(m));
  assert.ok(m.ra >= 0 && m.rm >= m.ra && m.ro >= 0, 'the page response (key to the next frame) is told too: ' + JSON.stringify(m));
  assert.ok(JSON.stringify(m).indexOf('secret') < 0 && JSON.stringify(m).indexOf('hello') < 0, 'no typed text');
  await page.evaluate(() => window.KestrelTyp.tell());
  assert.equal((await typ(page)).length, 1, 'nothing counted since: nothing told');
  await context.close();
});

test('PERF3c: a script-made input event is not counted, and a page without typing sends nothing; at most six reports a page', async () => {
  const { page, context } = await open();
  await page.evaluate(() => { document.getElementById('i').dispatchEvent(new Event('input', { bubbles: true })); window.KestrelTyp.tell(); });
  assert.equal((await typ(page)).length, 0, 'an untrusted event is not a key');
  for (let r = 0; r < 8; r++) { await page.focus('#d'); await page.keyboard.type('ab'); await page.evaluate(() => window.KestrelTyp.tell()); }
  assert.equal((await typ(page)).length, 6, 'six at most');
  await context.close();
});
