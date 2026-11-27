'use strict';
// PERF4 (owner 2026-11-22: "i still can't upload photos to this claude chat"): content.js tells the extension about a promise nobody handled (kind of error and a short message), like it does for a script that threw.
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

test('PERF4: an unhandled rejection is told as a short word, a handled one is not, at most eight a frame', async () => {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><html><head><title>t</title></head><body>x</body></html>' }));
  const page = await context.newPage();
  await page.addInitScript(() => { window.__msgs = []; window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } }; });
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/');
  await page.evaluate(() => { Promise.reject(new TypeError('canvas toBlob returned null')); Promise.reject('plain words'); const ok = Promise.reject(new Error('handled')); ok.catch(() => {}); });
  await page.waitForTimeout(300);
  const m = await page.evaluate(() => window.__msgs.filter((x) => x.k === 'jserr'));
  assert.ok(m.some((x) => x.m === 'rejected: TypeError: canvas toBlob returned null'), JSON.stringify(m));
  assert.ok(m.some((x) => x.m === 'rejected: plain words'), JSON.stringify(m));
  assert.ok(!m.some((x) => x.m.indexOf('handled') >= 0 && x.m.indexOf('unhandled') < 0 && x.m === 'rejected: Error: handled'), 'a handled rejection is not told');
  await page.evaluate(() => { for (let i = 0; i < 20; i++) Promise.reject(new Error('n' + i)); });
  await page.waitForTimeout(300);
  assert.ok((await page.evaluate(() => window.__msgs.filter((x) => x.k === 'jserr').length)) <= 8, 'at most eight a frame');
  await context.close();
});
