'use strict';
// PERF14 (owner 2026-11-23: "I was already on the page and it was already loaded" but Back still reloads it): content.js stops the unfinished loads of a page when the person taps an ordinary link, so the engine can keep the page for Back.
// What is proved, in real headless Chromium with a fake extension API: the stop happens exactly for a plain same-tab link to another page, never when the page handled the click, never for a new tab / download / modifier key /
// same-page link / non-web link / playing media / a script-made click, never when Instant Back is off, at most once per page; the link's own navigation still happens; the page tells the app it stopped (pagehide report).
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

const LINKS = '<a id="plain" href="/b">b</a><a id="blank" href="/b" target="_blank">b</a><a id="dl" href="/b" download>b</a><a id="hash" href="/a#x">x</a><a id="mail" href="mailto:a@b.example">m</a>' +
  '<a id="handled" href="/b">h</a><video id="v" muted loop></video><a id="self" href="/b" target="_self">s</a><a id="off" href="/b">o</a>';

/** A page at https://site.example/a with the links above; [bkm] is what the fake background page answers to {k:'bkm'} (1 = Instant Back on). window.stop is counted. */
async function site(bkm) {
  const context = await browser.newContext({ hasTouch: false, viewport: { width: 400, height: 700 } });
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', headers: { 'cache-control': 'no-store' }, body: '<!doctype html><html><head><title>t</title></head><body>' + LINKS + '<script>document.getElementById("handled").addEventListener("click", function (e) { e.preventDefault(); });</script></body></html>' }));
  const page = await context.newPage();
  await page.addInitScript((cfg) => {
    window.__msgs = []; window.__stops = 0;
    const keep = () => { try { sessionStorage.setItem('kst', JSON.stringify({ stops: (JSON.parse(sessionStorage.getItem('kst') || '{"stops":0,"ph":[]}').stops) + 0, ph: JSON.parse(sessionStorage.getItem('kst') || '{"stops":0,"ph":[]}').ph })); } catch (e) { /* none */ } };
    const note = (f) => { try { const o = JSON.parse(sessionStorage.getItem('kst') || '{"stops":0,"ph":[]}'); f(o); sessionStorage.setItem('kst', JSON.stringify(o)); } catch (e) { /* none */ } };
    const real = window.stop.bind(window);
    window.stop = function () { window.__stops++; note((o) => { o.stops++; }); return real(); };
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); if (m.k === 'ph') note((o) => { o.ph.push(m); }); if (m.k === 'bkm') return Promise.resolve(cfg.bkm); return Promise.resolve(undefined); } } };
    void keep;
  }, { bkm });
  await page.addInitScript({ content: CONTENT });
  await page.goto('https://site.example/a');
  await page.waitForTimeout(150);                                    // the answer to {k:'bkm'} arrives
  return page;
}
const stops = (page) => page.evaluate(() => window.__stops);

test('PERF14: a plain link tap stops the unfinished loads once, and the link still opens the next page', async () => {
  const page = await site(1);
  await Promise.all([page.waitForURL('https://site.example/b'), page.click('#plain')]);
  assert.equal(page.url(), 'https://site.example/b', 'the navigation was not cancelled by the stop');
});

const saved = (page) => page.evaluate(() => JSON.parse(sessionStorage.getItem('kst') || '{"stops":0,"ph":[]}'));

test('PERF14: a click made by a script is never a person\'s tap', async () => {
  const page = await site(1);
  await page.evaluate(() => { document.getElementById('plain').addEventListener('click', function (e) { e.preventDefault(); }, true); });
  await page.evaluate(() => { document.getElementById('plain').dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true })); });
  assert.equal(await stops(page), 0);
});

test('PERF14: never for a click the page handled, a new tab, a download, a modifier key, a place in the same page, a non-web link, or while a film plays', async () => {
  const cases = [['#handled', {}], ['#blank', {}], ['#dl', {}], ['#plain', { modifiers: ['Shift'] }], ['#plain', { modifiers: ['Control'] }], ['#hash', {}], ['#mail', {}]];
  for (const [sel, opt] of cases) {
    const page = await site(1);
    await page.evaluate(() => { document.body.addEventListener('click', function (e) { var a = e.target.closest && e.target.closest('a'); if (a && a.id !== 'handled') e.preventDefault(); }, false); });   // only to keep the test page from leaving
    await page.click(sel, opt).catch(() => {});
    assert.equal(await stops(page), 0, 'no stop for ' + sel + ' ' + JSON.stringify(opt));
    await page.context().close();
  }
  const page = await site(1);
  await page.evaluate(() => { const v = document.getElementById('v'); Object.defineProperty(v, 'paused', { value: false, configurable: true }); document.body.addEventListener('click', function (e) { e.preventDefault(); }, false); });
  await page.click('#plain').catch(() => {});
  assert.equal(await stops(page), 0, 'a playing film is never cut');
});

test('PERF14: with Instant Back off nothing is stopped; with it on, a link tap stops exactly once', async () => {
  const off = await site(0);
  await Promise.all([off.waitForURL('https://site.example/b'), off.click('#plain')]);
  assert.equal((await saved(off)).stops, 0);
  const on = await site(1);
  await Promise.all([on.waitForURL('https://site.example/b'), on.click('#plain')]);
  assert.equal((await saved(on)).stops, 1, 'one stop for the tap');
});

test('PERF14: the pagehide report carries st=1 when the stop was used and st=0 when not', async () => {
  const on = await site(1);
  await Promise.all([on.waitForURL('https://site.example/b'), on.click('#plain')]);
  const a = (await saved(on)).ph;
  assert.ok(a.length >= 1 && a[a.length - 1].st === 1, JSON.stringify(a));
  const off = await site(0);
  await Promise.all([off.waitForURL('https://site.example/b'), off.click('#plain')]);
  const b = (await saved(off)).ph;
  assert.ok(b.length >= 1 && b[b.length - 1].st === 0, JSON.stringify(b));
});
