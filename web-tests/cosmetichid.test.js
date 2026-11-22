'use strict';
// GL1 (owner 2026-11-21: "some elements of pages are not drawn ... an invisible line, and under the line is blank"; on pornhub the loading circle over a video was cut off with Shields on and whole with Shields off):
// content.js counts, once the page has loaded, how many elements each element-hiding rule of the injected CSS matches and tells the extension the rules that matched ([selector, count, hides 1|0], at most 8).
// Real headless Chromium with a fake `browser.runtime` that answers the `css` message with a fixed style sheet. Run: cd web-tests && npm test
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

async function open(css, body) {
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><html><head><title>t</title></head><body>' + body + '</body></html>' }));
  const page = await context.newPage();
  await page.addInitScript((c) => {
    window.__msgs = [];
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(m.k === 'css' ? c : undefined); } } };
  }, css);
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/');
  return { page, context };
}
const hid = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'jhid'));
async function until(f, what, ms) { const end = Date.now() + (ms || 8000); while (Date.now() < end) { if (await f()) return; await new Promise((r) => setTimeout(r, 50)); } assert.fail('timed out waiting for ' + what); }

test('GL1: the rules that matched elements are told with their counts, most first; a rule that matched nothing is not told; a style rule says it hides nothing', async () => {
  const css = '/*kg*/.mgp_overlayContainer{display:none!important} .absent{display:none!important} div.container{margin-top:0px !important} .a, .b{display:none!important} img[srcset]{display:none!important}';
  const { page, context } = await open(css, '<div class="mgp_overlayContainer"></div><div class="container"></div><div class="a"></div><div class="b"></div><div class="b"></div><p>text</p>');
  await until(async () => (await hid(page)).length >= 1, 'the count', 8000);
  const m = (await hid(page))[0];
  assert.equal(m.k, 'jhid'); assert.ok(m.n >= 5, 'rules looked at ' + m.n);
  assert.deepEqual(m.m[0], ['.a, .b', 3, 1]);
  assert.ok(m.m.some((e) => e[0] === '.mgp_overlayContainer' && e[1] === 1 && e[2] === 1));
  assert.ok(m.m.some((e) => e[0] === 'div.container' && e[1] === 1 && e[2] === 0), 'a style rule says it hides nothing: ' + JSON.stringify(m.m));
  assert.ok(!m.m.some((e) => e[0] === '.absent' || e[0].indexOf('srcset') >= 0), 'what matched nothing is not told');
  assert.ok(JSON.stringify(m).indexOf('text') < 0, 'no page text');
  await context.close();
});

test('GL1: a page where no rule matches sends nothing, and a selector the engine cannot parse does not stop the others', async () => {
  const { page, context } = await open('.nothing{display:none!important} :::bad[[{display:none!important} .here{display:none!important}', '<div class="here"></div>');
  await until(async () => (await hid(page)).length >= 1, 'the count', 8000);
  const m = (await hid(page))[0];
  assert.deepEqual(m.m, [['.here', 1, 1]]);
  await context.close();
  const quiet = await open('.nothing{display:none!important}', '<p>hello</p>');
  await new Promise((r) => setTimeout(r, 4400));
  assert.equal((await hid(quiet.page)).length, 0, 'nothing matched: nothing sent');
  await quiet.context.close();
});

test('GL1: at most eight rules are sent and a page with many rules is looked at only once', async () => {
  let css = '', body = '';
  for (let i = 0; i < 20; i++) { css += '.r' + i + '{display:none!important} '; body += '<i class="r' + i + '"></i>'; }
  const { page, context } = await open(css, body);
  await until(async () => (await hid(page)).length >= 1, 'the count', 8000);
  await page.waitForTimeout(600);
  const all = await hid(page);
  assert.equal(all.length, 1, 'told once'); assert.equal(all[0].m.length, 8);
  await context.close();
});
