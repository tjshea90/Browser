'use strict';
// The tap-to-load page of an embedded player (assets/kestrel-shields/embed.html + embed.js, EX-10) in real headless Chromium with a stand-in for the extension's messaging.
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const PAGE = 'file://' + path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'embed.html');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

async function open(hash, sendMessage) {
  const ctx = await browser.newContext({ viewport: { width: 320, height: 180 } });
  const page = await ctx.newPage();
  await page.addInitScript((src) => { window.__sent = []; window.browser = { runtime: { sendMessage: (m) => { window.__sent.push(m); return src === 'fail' ? Promise.reject(new Error('no background')) : Promise.resolve(true); } } }; }, sendMessage || 'ok');
  await page.route('http://player.example/**', (r) => r.fulfill({ status: 200, contentType: 'text/html', body: '<title>real player</title>REAL PLAYER' }));
  await page.goto(PAGE + hash);
  return { ctx, page };
}

test('the box names the host and waits: nothing is requested before the tap', async () => {
  const { ctx, page } = await open('#' + encodeURIComponent('http://player.example/embed/abc?x=1'));
  assert.equal(await page.textContent('#t'), 'Tap to load');
  assert.equal(await page.textContent('#h'), 'player.example');
  assert.equal(await page.evaluate(() => window.__sent.length), 0);
  assert.ok(page.url().startsWith('file:'), 'still the box');
  await ctx.close();
});

test('a tap asks the background to allow that one address, then loads it', async () => {
  const u = 'http://player.example/embed/abc?x=1';
  const { ctx, page } = await open('#' + encodeURIComponent(u));
  await Promise.all([page.waitForURL(u), page.click('body')]);
  assert.equal(await page.title(), 'real player');
  await ctx.close();
});

test('the message carries exactly the address from the fragment', async () => {
  const u = 'http://player.example/embed/zzz';
  const ctx = await browser.newContext(); const page = await ctx.newPage();
  await page.addInitScript(() => { window.__sent = []; window.browser = { runtime: { sendMessage: (m) => { window.__sent.push(m); return new Promise(() => {}); } } }; }); // never answers: the page must not load by itself
  await page.goto(PAGE + '#' + encodeURIComponent(u));
  await page.click('body');
  assert.deepEqual(await page.evaluate(() => window.__sent), [{ k: 'embed-allow', u }]);
  assert.equal(await page.textContent('#t'), 'Loading...');
  await page.click('body'); // a second tap changes nothing
  assert.equal(await page.evaluate(() => window.__sent.length), 1);
  await ctx.close();
});

test('if the background cannot be reached the tap still loads the player', async () => {
  const u = 'http://player.example/embed/q';
  const { ctx, page } = await open('#' + encodeURIComponent(u), 'fail');
  await Promise.all([page.waitForURL(u), page.click('body')]);
  await ctx.close();
});

test('a fragment that is not a web address shows nothing to load and a tap does nothing', async () => {
  for (const bad of ['', '#', '#javascript%3Aalert(1)', '#' + encodeURIComponent('file:///etc/passwd'), '#%E0%A4%A', '#' + encodeURIComponent('moz-extension://x/embed.html')]) {
    const { ctx, page } = await open(bad);
    assert.equal(await page.textContent('#t'), 'Nothing to load', 'for ' + bad);
    await page.click('body');
    assert.equal(await page.evaluate(() => window.__sent.length), 0);
    assert.ok(page.url().startsWith('file:'), 'did not navigate for ' + bad);
    await ctx.close();
  }
});
