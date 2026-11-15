'use strict';
// RX3-S3 (research/REVIEW-EXTERNAL-3.md part S): the class/id token observer of content.js (D2: the top document tells the app which names it holds, to get only the generic selectors that can match) must have a
// TIME BUDGET like the procedural observer next to it: it stops after 300 ms of its own work in total or 20 s after the page was parsed, does nothing while the page is hidden (and looks again when it is shown), and
// ignores what happens inside an editable region (typing in a rich editor adds nodes all the time). Real headless Chromium, fake extension API. The tests shorten the two limits through window.__kstTokCfg (in the
// real extension a page's globals are not visible to the content script).
// Run: cd web-tests && npm test
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

async function open(body, cfg) {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` }));
  const page = await context.newPage();
  await page.addInitScript((c) => {
    window.__kstTokCfg = c || {}; window.__gen = []; window.__msgs = []; window.__hidden = false;
    Object.defineProperty(document, 'hidden', { configurable: true, get: () => window.__hidden });
    window.requestIdleCallback = (f) => setTimeout(f, 30);
    window.browser = { runtime: { sendMessage: (m) => {
      window.__msgs.push(m);
      if (m.k === 'css') return Promise.resolve('/*kg*/.site-rule{display:none!important}');   // the marker: this top document reports its names
      if (m.k === 'gen') window.__gen.push(m.t);
      return Promise.resolve(undefined);
    } } };
  }, cfg || null);
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/');
  return page;
}
const sent = async (page) => [].concat(...(await page.evaluate(() => window.__gen)));
const add = (page, html) => page.evaluate((h) => { const d = document.createElement('div'); d.innerHTML = h; document.body.appendChild(d); }, html);

test('the observer stops after its time budget; everything found before the stop was sent, what comes after is not', async () => {
  const page = await open('<div class="first">a</div>', { budgetMs: 0.001 });   // any callback that does work uses it up
  await page.waitForTimeout(700);
  let many = ''; for (let i = 0; i < 4000; i++) many += `<i class="burst${i}"></i>`;
  await add(page, many);                                                         // one big callback: the budget is gone after it
  await page.waitForTimeout(1500);
  await add(page, '<p class="after-the-stop"></p>');
  await page.waitForTimeout(1200);
  const names = await sent(page);
  assert.ok(names.includes('.first'));
  assert.ok(names.includes('.burst0'), 'the burst itself was handled (the budget is checked before a callback, not in the middle of one)');
  assert.ok(!names.includes('.after-the-stop'), 'the observer was disconnected');
  await page.context().close();
});

test('with an ordinary budget a late name is still sent (nothing changed for normal pages)', async () => {
  const page = await open('<div class="first">a</div>');
  await page.waitForTimeout(700);
  await add(page, '<p class="late-one"></p>');
  await page.waitForTimeout(1000);
  assert.ok((await sent(page)).includes('.late-one'));
  await page.context().close();
});

test('the observer also stops a fixed time after the page was parsed', async () => {
  const page = await open('<div class="first">a</div>', { windowMs: 600 });
  await page.waitForTimeout(250);
  await add(page, '<p class="early"></p>');
  await page.waitForTimeout(900);
  await add(page, '<p class="too-late"></p>');
  await page.waitForTimeout(1000);
  const names = await sent(page);
  assert.ok(names.includes('.early')); assert.ok(!names.includes('.too-late'));
  await page.context().close();
});

test('while the page is hidden nothing is collected, and the names are found when it is shown again', async () => {
  const page = await open('<div class="first">a</div>');
  await page.waitForTimeout(700);
  await page.evaluate(() => { window.__hidden = true; });
  await add(page, '<p class="added-while-hidden"></p>');
  await page.waitForTimeout(900);
  assert.ok(!(await sent(page)).includes('.added-while-hidden'));
  await page.evaluate(() => { window.__hidden = false; document.dispatchEvent(new Event('visibilitychange')); });
  await page.waitForTimeout(1000);
  assert.ok((await sent(page)).includes('.added-while-hidden'), 'looked at again when shown');
  await page.context().close();
});

test('typing in an editable region does not feed the observer', async () => {
  const page = await open('<div class="first">a</div><div id="ed" contenteditable="true"></div>');
  await page.waitForTimeout(700);
  await page.evaluate(() => { const ed = document.getElementById('ed'); for (let i = 0; i < 50; i++) { const s = document.createElement('span'); s.className = 'typed' + i; ed.appendChild(s); } });
  await page.waitForTimeout(1000);
  assert.ok(!(await sent(page)).some((n) => n.indexOf('.typed') === 0));
  await page.context().close();
});
