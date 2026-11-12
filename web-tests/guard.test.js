'use strict';
// guard-main.js (the pop-up and forced-redirect guard) in real headless Chromium, with the situations movie sites create. The script is plain DOM code that behaves the same in Gecko; what only the real
// browser can show (world MAIN registration, GeckoView's own pop-up rules) is checked on the emulator. Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const GUARD = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'guard-main.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

const AD = 'https://ads.example/landing?x=1';
const TARGET = '<!doctype html><title>opened</title><body>opened</body>';

/** A page at https://movie.example/ with the guard installed before any page script runs. */
async function open(body, opts) {
  opts = opts || {};
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.hostname === 'movie.example' && u.pathname === '/') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>movie</title></head><body style="margin:0">${body}</body></html>` });
    return r.fulfill({ contentType: 'text/html', body: TARGET });
  });
  const page = await context.newPage();
  await page.addInitScript({ content: GUARD });
  await page.addInitScript(() => {
    window.__notes = [];
    document.addEventListener('__kst_pop_v1', (e) => window.__notes.push(String(e.detail)), true);
  });
  if (opts.off) await page.addInitScript(() => { document.dispatchEvent(new CustomEvent('__kst_cfg_v1', { detail: '0' })); });
  await page.goto('https://movie.example/');
  page.popups = [];
  context.on('page', (p) => page.popups.push(p));
  return page;
}
const settle = (ms) => new Promise((r) => setTimeout(r, ms || 600));
const notes = (page) => page.evaluate(() => window.__notes);

test('a tap on the player layer that calls window.open for an ad opens nothing and is reported', async () => {
  const page = await open(`<div id="layer" style="position:fixed;inset:0;background:#123" onclick="window.open('${AD}','_blank')"></div>`);
  await page.mouse.click(100, 100);
  await settle();
  assert.equal(page.popups.length, 0);
  assert.deepEqual(await notes(page), ['open|' + AD]);
});

test('the page gets a stand-in window back, so a script that falls back to redirecting when the pop-up "failed" does not redirect', async () => {
  const page = await open(`<div id="layer" style="position:fixed;inset:0" onclick="var w=window.open('${AD}'); if (!w || w.closed) { location.href='${AD}'; } else { w.blur(); window.__ok = typeof w.focus; }"></div>`);
  await page.mouse.click(50, 50);
  await settle();
  assert.equal(page.url(), 'https://movie.example/');
  assert.equal(await page.evaluate(() => window.__ok), 'function');
});

test('a real link the user taps still opens in a new tab', async () => {
  const page = await open(`<a id="l" href="https://news.example/story" target="_blank" style="display:block;width:300px;height:80px">story</a>`);
  const popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(50, 40);
  const p = await popup;
  assert.equal(new URL(p.url() === 'about:blank' ? 'https://news.example/story' : p.url()).hostname, 'news.example');
  assert.deepEqual(await notes(page), []);
});

test('window.open of the very link that was tapped is allowed (sites that open links from a click handler)', async () => {
  const page = await open(`<a id="l" href="https://news.example/story" onclick="window.open(this.href); return false;" style="display:block;width:300px;height:80px">story</a>`);
  const popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(50, 40);
  await popup;
  assert.deepEqual(await notes(page), []);
});

test('a sign-in window and a page of the same site, each from a real tap, are allowed', async () => {
  const page = await open(`<button id="g" style="width:200px;height:60px" onclick="window.open('https://accounts.google.com/o/oauth2/auth?x=1')">Sign in</button>
    <button id="c" style="position:absolute;top:100px;width:200px;height:60px" onclick="window.open('/chat')">Chat</button>`);
  let popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(50, 30);
  await popup;
  popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(50, 130);
  await popup;
  assert.deepEqual(await notes(page), []);
});

test('a pop-up that a timer opens a few seconds after the tap is refused', async () => {
  const page = await open(`<div style="width:300px;height:300px" onclick="setTimeout(function(){ window.open('${AD}'); }, 1800)">play</div>`);
  await page.mouse.click(50, 50);
  await settle(2600);
  assert.equal(page.popups.length, 0);
  assert.equal((await notes(page)).length, 1);
});

test('a pop-up opened on page load (no tap) is refused', async () => {
  const page = await open(`<script>window.open('${AD}');</script><p>loading</p>`);
  await settle();
  assert.equal(page.popups.length, 0);
  assert.equal((await notes(page))[0], 'open|' + AD);
});

test('a link a script creates and clicks inside the play tap opens nothing', async () => {
  const page = await open(`<div style="width:300px;height:300px" onclick="var a=document.createElement('a'); a.href='${AD}'; a.target='_blank'; document.body.appendChild(a); a.click();">play</div>`);
  await page.mouse.click(50, 50);
  await settle();
  assert.equal(page.popups.length, 0);
  assert.deepEqual(await notes(page), ['click|' + AD]);
});

test('a script click that would move the page to another site is refused too (forced redirect through a link)', async () => {
  const page = await open(`<div style="width:300px;height:300px" onclick="var a=document.createElement('a'); a.href='${AD}'; document.body.appendChild(a); a.click();">play</div>`);
  await page.mouse.click(50, 50);
  await settle();
  assert.equal(page.url(), 'https://movie.example/');
  assert.deepEqual(await notes(page), ['redirect|' + AD]);
});

test('an invisible link laid over the page does nothing when the play tap lands on it', async () => {
  const page = await open(`<a id="cover" href="${AD}" target="_blank" style="position:fixed;inset:0;opacity:0"></a>`);
  await page.mouse.click(120, 120);
  await settle();
  assert.equal(page.popups.length, 0);
  assert.equal(page.url(), 'https://movie.example/');
  assert.deepEqual(await notes(page), ['overlay|' + AD]);
});

test('a script-saved download (hidden link with a download attribute) still works', async () => {
  const page = await open(`<div style="width:300px;height:300px" onclick="var a=document.createElement('a'); a.href='data:text/plain,hello'; a.download='x.txt'; document.body.appendChild(a); a.click(); window.__clicked=1;">save</div>`);
  const dl = page.waitForEvent('download', { timeout: 3000 });
  await page.mouse.click(50, 50);
  const d = await dl;
  assert.equal(d.suggestedFilename(), 'x.txt');
  assert.deepEqual(await notes(page), []);
});

test('a form that targets a new window from a script is refused', async () => {
  const page = await open(`<form id="f" action="${AD}" target="_blank" method="post"></form><script>setTimeout(function(){ document.getElementById('f').submit(); }, 50);</script>`);
  await settle();
  assert.equal(page.popups.length, 0);
  assert.equal((await notes(page))[0], 'form|' + AD);
});

test('when the app says the guard is off for this site (Shields off) nothing is refused', async () => {
  const page = await open(`<div style="width:300px;height:300px" onclick="window.open('${AD}')">play</div>`, { off: true });
  const popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(50, 50);
  await popup;
  assert.deepEqual(await notes(page), []);
});

test('the guard does not break an ordinary page: links, buttons and forms in the same window', async () => {
  const page = await open(`<a id="same" href="/next">next</a><button onclick="document.title='clicked'">b</button>`);
  await page.click('button');
  assert.equal(await page.title(), 'clicked');
  await page.click('#same');
  await page.waitForLoadState();
  assert.equal(new URL(page.url()).hostname, 'movie.example');
});
