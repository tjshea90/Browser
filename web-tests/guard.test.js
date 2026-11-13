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

/** A page at https://movie.example/ with the guard installed before any page script runs. [opts.embed]: the HTML served for https://player.example/embed (an embedded player in a cross-origin frame). */
async function open(body, opts) {
  opts = opts || {};
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.hostname === 'movie.example' && u.pathname === '/') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>movie</title></head><body style="margin:0">${body}</body></html>` });
    if (u.hostname === 'player.example' && u.pathname === '/embed') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><body style="margin:0">${opts.embed || ''}</body></html>` });
    return r.fulfill({ contentType: 'text/html', body: TARGET });
  });
  const page = await context.newPage();
  await page.addInitScript({ content: GUARD });
  await page.addInitScript(() => {
    window.__notes = [];
    document.addEventListener('__kst_pop_v1', (e) => window.__notes.push(String(e.detail)), true);
  });
  if (opts.off) await page.addInitScript(() => { document.dispatchEvent(new CustomEvent('__kst_cfg_v1', { detail: '0' })); });
  // what content.js tells every frame: the guard applies, and the TOP page's host (the frames of an embedded player judge "same site" against it)
  else await page.addInitScript(() => { document.dispatchEvent(new CustomEvent('__kst_cfg_v1', { detail: '1|movie.example' })); });
  await page.goto('https://movie.example/');
  page.popups = [];
  context.on('page', (p) => page.popups.push(p));
  return page;
}
const settle = (ms) => new Promise((r) => setTimeout(r, ms || 600));
// what the guard REFUSED (the 'tap|...' reports of real taps are checked on their own below)
const notes = (page) => page.evaluate(() => window.__notes.filter((n) => n.indexOf('tap|') !== 0));
const taps = (page) => page.evaluate(() => window.__notes.filter((n) => n.indexOf('tap|') === 0));

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

test('a sign-in window from a real tap is allowed', async () => {
  const page = await open(`<button id="g" style="width:200px;height:60px" onclick="window.open('https://accounts.google.com/o/oauth2/auth?x=1')">Sign in</button>`);
  const popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(50, 30);
  await popup;
  assert.deepEqual(await notes(page), []);
});

test('a page of the same site from a real tap is allowed', async () => {
  const page = await open(`<button id="c" style="width:200px;height:60px" onclick="window.open('/chat')">Chat</button>`);
  const popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(50, 30);
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

test('every real tap is reported, with the link it landed on, so the app can tell the owner\'s jump from a script\'s', async () => {
  const page = await open(`<div style="width:300px;height:80px">nothing here</div><a href="https://news.example/story" target="_blank" style="display:block;width:300px;height:80px">story</a>`);
  await page.mouse.click(50, 40);                                   // on the plain block
  const popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(50, 120);                                  // on the link
  await popup;
  assert.deepEqual(await taps(page), ['tap|', 'tap|https://news.example/story']);
  assert.deepEqual(await notes(page), []);
});

test('a click a script makes is never reported as a tap', async () => {
  const page = await open(`<div id="d" style="width:300px;height:300px"></div><script>setTimeout(function(){ document.getElementById('d').click(); }, 50);</script>`);
  await settle();
  assert.deepEqual(await taps(page), []);
});

test('an embedded player\'s own ad redirector is not "the same site": the frame\'s window.open to its own host is refused', async () => {
  const page = await open(`<iframe src="https://player.example/embed" style="width:400px;height:300px;border:0"></iframe>`,
    { embed: `<div style="width:400px;height:300px" onclick="window.open('https://player.example/ad/redirect?id=1')">play</div>` });
  await settle(900);
  await page.mouse.click(100, 100);
  await settle();
  assert.equal(page.popups.length, 0);
  const frame = page.frames().find((f) => f.url().indexOf('player.example') >= 0);
  assert.deepEqual(await frame.evaluate(() => window.__notes.filter((n) => n.indexOf('tap|') !== 0)), ['open|https://player.example/ad/redirect?id=1']);
});

test('the spoof play button: a link to another site, with only an image, laid on the player, does nothing', async () => {
  const icon = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><circle cx='50' cy='50' r='45' fill='red'/></svg>";
  const page = await open(`<div style="position:relative;width:400px;height:300px"><video style="width:400px;height:300px;background:#000"></video><a id="spoof" href="${AD}" target="_blank" style="position:absolute;left:150px;top:100px;width:100px;height:100px"><img src="${icon}" width="100" height="100"></a></div>`);
  await page.mouse.click(200, 150);
  await settle();
  assert.equal(page.popups.length, 0);
  assert.equal(page.url(), 'https://movie.example/');
  assert.deepEqual(await notes(page), ['overlay|' + AD]);
});

test('a link with words of its own near the player, and a same-site link on it, still work', async () => {
  const page = await open(`<div style="position:relative;width:400px;height:300px"><video style="width:400px;height:300px;background:#000"></video></div><a href="https://news.example/story" target="_blank" style="display:block;width:300px;height:60px">Read the story</a>`);
  const popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(50, 330);
  await popup;
  assert.deepEqual(await notes(page), []);
});


// ---- AP1: one press plays the video, the fake layers are still refused
const STUB_PLAY = 'HTMLMediaElement.prototype.play = function () { window.__plays = (window.__plays || 0) + 1; return Promise.resolve(); };';
const plays = (page) => page.evaluate(() => window.__plays || 0);
const real = (page) => page.evaluate(() => window.__real || 0);

test('AP1: an ad script that takes the first press on the video: the pop-up is refused and the video is started at once', async () => {
  const page = await open(`<video id="v" src="https://movie.example/clip.mp4" style="width:300px;height:200px;background:#000"></video><script>${STUB_PLAY}
    var first = true;
    document.addEventListener('click', function (e) { if (first) { first = false; window.open('${AD}', '_blank'); e.stopImmediatePropagation(); e.preventDefault(); } }, true);</script>`);
  await page.mouse.click(100, 100);
  await settle(800);
  assert.equal(page.popups.length, 0);
  assert.deepEqual(await notes(page), ['open|' + AD]);
  assert.equal(await plays(page), 1, 'one press started the video');
});

test('AP1: an invisible link laid over the player: refused as before, switched off, and the video starts', async () => {
  const page = await open(`<video src="https://movie.example/clip.mp4" style="width:300px;height:200px"></video><a id="cover" href="${AD}" target="_blank" style="position:fixed;inset:0;opacity:0"></a><script>${STUB_PLAY}</script>`);
  await page.mouse.click(100, 100);
  await settle(800);
  assert.equal(page.popups.length, 0);
  assert.deepEqual(await notes(page), ['overlay|' + AD]);
  assert.equal(await plays(page), 1);
  assert.equal(await page.evaluate(() => getComputedStyle(document.getElementById('cover')).pointerEvents), 'none', 'the next press goes straight through');
});

const PLAYER_WITH_LAYERS = (layers) => `<div id="player" class="player" style="position:relative;width:300px;height:200px;background:#111">
  <div id="realplay" style="position:absolute;left:0;top:0;right:0;bottom:0" onclick="window.__real=(window.__real||0)+1">play</div>${layers}</div>`;
const LAYER = (id, remove) => `<div id="${id}" style="position:absolute;left:0;top:0;right:0;bottom:0"></div><script>document.getElementById('${id}').addEventListener('click', function () { window.__layerClicks = (window.__layerClicks || 0) + 1; window.open('${AD}'); ${remove ? 'this.remove();' : ''} });</script>`;

test('AP1: a fake play layer that opens an ad and removes itself: the press is repeated at the same place and reaches the real player', async () => {
  const page = await open(PLAYER_WITH_LAYERS(LAYER('fake1', true)));
  await page.mouse.click(100, 100);
  await settle(900);
  assert.equal(page.popups.length, 0);
  assert.deepEqual(await notes(page), ['open|' + AD]);
  assert.equal(await real(page), 1, 'the real play control got the press');
});

test('AP1: two fake layers on top of each other: one press goes through both', async () => {
  const page = await open(PLAYER_WITH_LAYERS(LAYER('fake1', true) + LAYER('fake2', true)));
  await page.mouse.click(100, 100);
  await settle(1300);
  assert.equal(page.popups.length, 0);
  assert.deepEqual(await notes(page), ['open|' + AD, 'open|' + AD]);
  assert.equal(await real(page), 1);
});

test('AP1: a fake layer that never goes away is stepped over for the replay and still counted once per press', async () => {
  const page = await open(PLAYER_WITH_LAYERS(LAYER('fake1', false)));
  await page.mouse.click(100, 100);
  await settle(900);
  assert.equal(page.popups.length, 0);
  assert.equal(await page.evaluate(() => window.__layerClicks), 1, 'the layer saw only the real press');
  assert.equal(await real(page), 1);
  await settle(1700);
  assert.equal(await page.evaluate(() => getComputedStyle(document.getElementById('fake1')).pointerEvents), 'auto', 'the layer is back to normal afterwards (it may be a control)');
});

test('AP1: at most five rounds, however many layers an ad stacks', async () => {
  const layers = [1, 2, 3, 4, 5, 6, 7, 8, 9].map((n) => LAYER('fake' + n, true)).join('');
  const page = await open(PLAYER_WITH_LAYERS(layers));
  await page.mouse.click(100, 100);
  await settle(2200);
  assert.equal(page.popups.length, 0);
  assert.ok((await notes(page)).length <= 6, 'the real press and five rounds at most: ' + (await notes(page)).length);
  assert.equal(await real(page), 0, 'nine layers: the cap stops the rounds before the player is reached (the next real press continues)');
});

test('AP1: a press on a video with no ad in the way is left alone (nothing is started by us)', async () => {
  const page = await open(`<video id="v" style="width:300px;height:200px"></video><script>${STUB_PLAY}</script>`);
  await page.mouse.click(100, 100);
  await settle(700);
  assert.deepEqual(await notes(page), []);
  assert.equal(await plays(page), 0);
});

test('AP1: a press that pauses a playing video is not turned into a play, even if an ad script opens a pop-up with it', async () => {
  const page = await open(`<video id="v" src="https://movie.example/clip.mp4" style="width:300px;height:200px"></video><script>${STUB_PLAY}
    Object.defineProperty(HTMLMediaElement.prototype, 'paused', { get: function () { return false; } });
    Object.defineProperty(HTMLMediaElement.prototype, 'readyState', { get: function () { return 4; } });
    Object.defineProperty(HTMLMediaElement.prototype, 'currentTime', { get: function () { return 12; } });
    document.addEventListener('click', function () { window.open('${AD}'); }, true);</script>`);
  await page.mouse.click(100, 100);
  await settle(800);
  assert.deepEqual(await notes(page), ['open|' + AD]);
  assert.equal(await plays(page), 0);
});

test('AP1: a press that was not on a player never starts a video, even when it makes the page open an ad', async () => {
  const page = await open(`<button style="width:100px;height:50px" onclick="window.open('${AD}')">b</button><div style="margin-top:300px"><video style="width:300px;height:200px"></video></div><script>${STUB_PLAY}</script>`);
  await page.mouse.click(40, 25);
  await settle(800);
  assert.deepEqual(await notes(page), ['open|' + AD]);
  assert.equal(await plays(page), 0);
});

test('AP1: a player in a frame under a layer of the page: the refused press is passed to the frame, which starts its video', async () => {
  const page = await open(`<iframe src="https://player.example/embed" style="position:absolute;left:0;top:0;width:400px;height:300px;border:0"></iframe>
    <div id="layer" style="position:absolute;left:0;top:0;width:400px;height:300px" onclick="window.open('${AD}')"></div>`,
    { embed: `<video id="v" src="https://player.example/clip.mp4" style="width:400px;height:300px;background:#000"></video><script>${STUB_PLAY}</script>` });
  await settle(900);
  await page.mouse.click(100, 100);
  await settle(900);
  assert.equal(page.popups.length, 0);
  assert.deepEqual(await notes(page), ['open|' + AD]);
  const frame = page.frames().find((f) => f.url().indexOf('player.example') >= 0);
  assert.equal(await frame.evaluate(() => window.__plays || 0), 1, 'the frame started its own video');
});

test('AP1: with the guard off for the site nothing is assisted (the page behaves as it would in any browser)', async () => {
  const page = await open(`<video style="width:300px;height:200px"></video><div style="position:absolute;inset:0" onclick="window.open('${AD}')"></div><script>${STUB_PLAY}</script>`, { off: true });
  const popup = page.waitForEvent('popup', { timeout: 3000 });
  await page.mouse.click(100, 100);
  await popup;
  await settle(600);
  assert.equal(await plays(page), 0);
});

test('AP1: a player that builds its video only after the first press (no source yet) gets the press passed through instead of a play() that cannot work', async () => {
  const page = await open(`<div id="player" class="player" style="position:relative;width:300px;height:200px;background:#111"><video style="position:absolute;inset:0;width:300px;height:200px"></video>
    <div id="realplay" style="position:absolute;inset:0" onclick="window.__real=(window.__real||0)+1">play</div><div id="fake1" style="position:absolute;inset:0"></div></div>
    <script>${STUB_PLAY} document.getElementById('fake1').addEventListener('click', function () { window.open('${AD}'); this.remove(); });</script>`);
  await page.mouse.click(100, 100);
  await settle(900);
  assert.equal(await plays(page), 0, 'a video without a source is not asked to play');
  assert.equal(await real(page), 1, 'the press reached the real play control');
});
