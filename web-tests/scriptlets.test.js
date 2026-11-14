'use strict';
// scriptlets-main.js (AW1b: our own page-world scriptlets) in real headless Chromium, with the situations the filter lists describe. The library is plain DOM and JS and behaves the same in Gecko; what only the
// real browser can show (world MAIN registration, the isolated script's message) is checked on the emulator. Run: cd web-tests && npm test
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let chromium;
try { ({ chromium } = require('playwright')); } catch (e) { ({ chromium } = require('/usr/local/lib/node_modules_global/playwright')); }

const LIB = fs.readFileSync(path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields', 'scriptlets-main.js'), 'utf8');
let browser;
before(async () => { browser = await chromium.launch(); });
after(async () => { await browser.close(); });

/** A page at https://site.example/ with the library installed and [calls] sent before any page script runs (what content.js does once the answer for the site arrived). */
async function open(calls, body, opts) {
  opts = opts || {};
  const context = await browser.newContext();
  const hits = [];
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    hits.push(u.host + u.pathname);
    if (u.hostname === 'site.example' && u.pathname === '/') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title>${opts.head || ''}</head><body>${body || ''}</body></html>` });
    if (u.pathname === '/data.json') return r.fulfill({ contentType: 'application/json', body: opts.json || '{"ok":1}' });
    return r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>other</title><body>other</body>' });
  });
  const page = await context.newPage();
  await page.addInitScript({ content: LIB });
  await page.addInitScript((payload) => {
    window.__n = [];
    document.addEventListener('__kst_sl_n_v1', (e) => window.__n.push(String(e.detail)), true);
    if (payload) document.dispatchEvent(new CustomEvent('__kst_sl_v1', { detail: payload }));
  }, calls === null ? '' : (typeof calls === 'string' ? calls : JSON.stringify(calls)));
  page.hits = hits; page.context2 = context; page.popups = [];
  context.on('page', (p) => page.popups.push(p));
  await page.goto('https://site.example/');
  return page;
}
const ev = (page, f) => page.evaluate(f);
const settle = (ms) => new Promise((r) => setTimeout(r, ms || 300));

test('set-constant: the property always reads as the value, also when it is created later and nested', async () => {
  const page = await open([['set', 'adsOn', 'false'], ['set', 'a.b.c', 'true'], ['set', 'fn', 'noopFunc'], ['set', 'n', '7'], ['set', 'e', 'emptyArr']],
    '<script>window.adsOn = true; window.__a = window.adsOn; window.a = {}; window.a.b = { c: false }; window.__c = window.a.b.c; window.__f = typeof window.fn; window.__fr = window.fn(); window.__n = window.n; window.__e = Array.isArray(window.e)</script>');
  assert.equal(await ev(page, () => window.__a), false);
  assert.equal(await ev(page, () => window.__c), true);
  assert.equal(await ev(page, () => window.__f), 'function');
  assert.equal(await ev(page, () => window.__fr), undefined);
  assert.equal(await ev(page, () => window.__n), 7);
  assert.equal(await ev(page, () => window.__e), true);
});

test('set-constant: a value it does not know is not guessed, a bad chain is ignored', async () => {
  const page = await open([['set', 'x', 'some-odd-value'], ['set', '..', 'true'], ['set', 'ok', 'true']], '<script>window.x = 5; window.__x = window.x; window.__ok = window.ok</script>');
  assert.equal(await ev(page, () => window.__x), 5);
  assert.equal(await ev(page, () => window.__ok), true);
});

test('abort-on-property-read throws when the page reads it, the rest of the page goes on', async () => {
  const page = await open([['aopr', 'bait.detect'], ['aopr', 'plain']], `<script>try { var q = window.plain; window.__r = 'read' } catch (e) { window.__r = e.name }</script>
    <script>window.bait = {}; try { var z = window.bait.detect; window.__d = 'read' } catch (e) { window.__d = e.name }</script><script>window.__after = 1</script>`);
  assert.equal(await ev(page, () => window.__r), 'ReferenceError');
  assert.equal(await ev(page, () => window.__d), 'ReferenceError');
  assert.equal(await ev(page, () => window.__after), 1);
});

test('abort-on-property-write throws on assignment and keeps reading what was there', async () => {
  const page = await open([['aopw', 'adblockDetected']], '<script>try { window.adblockDetected = true; window.__w = "wrote" } catch (e) { window.__w = e.name }; window.__v = window.adblockDetected</script>');
  assert.equal(await ev(page, () => window.__w), 'ReferenceError');
  assert.equal(await ev(page, () => window.__v), undefined);
});

test('abort-current-script: only a script whose text holds the needle is stopped', async () => {
  const page = await open([['acs', 'document.cookie.__marker_unused'], ['acs', 'watched', 'secret-marker']],
    `<script>/* secret-marker */ try { var a = window.watched; window.__s1 = 'read' } catch (e) { window.__s1 = e.name }</script>
     <script>try { var b = window.watched; window.__s2 = 'read' } catch (e) { window.__s2 = e.name }</script>`);
  assert.equal(await ev(page, () => window.__s1), 'ReferenceError');
  assert.equal(await ev(page, () => window.__s2), 'read');
});

test('addEventListener-defuser: a listener of the type whose code holds the needle is never added', async () => {
  const page = await open([['aeld', 'click', 'popunder']],
    `<script>window.__c = [];
      document.addEventListener('click', function () { window.__c.push('popunder'); });
      document.addEventListener('click', function () { window.__c.push('fine'); });
      document.addEventListener('keydown', function () { window.__c.push('popunder-key'); });</script>`);
  await page.mouse.click(5, 5); await page.keyboard.press('a');
  assert.deepEqual(await ev(page, () => window.__c), ['fine', 'popunder-key']);
});

test('no-setTimeout-if / no-setInterval-if: matching callbacks and delays never run, others do', async () => {
  const page = await open([['nostif', 'adblockDetected'], ['nostif', '', '4321'], ['nosiif', 'nagTick']],
    `<script>window.__t = [];
      setTimeout(function () { window.__t.push('detected'); adblockDetected(); }, 10);
      setTimeout(function () { window.__t.push('slow'); }, 4321);
      setTimeout(function () { window.__t.push('keep'); }, 10);
      var n = 0; var id = setInterval(function nagTick() { window.__t.push('nag'); }, 10); var id2 = setInterval(function () { window.__t.push('i'); clearInterval(id2); }, 10);</script>`);
  await settle(250);
  assert.deepEqual((await ev(page, () => window.__t)).sort(), ['i', 'keep']);
});

test('adjust-setTimeout: a countdown with a matching delay runs sooner, the timer id still works', async () => {
  const page = await open([['nano-stb', 'countdown', '*', '0.02'], ['nano-sib', 'ticker', '1000', '0.02']],
    `<script>window.__t = []; window.__ids = typeof setTimeout(function countdown() { window.__t.push('count'); }, 3000);
      setTimeout(function other() { window.__t.push('other'); }, 3000);
      var k = 0; var iv = setInterval(function ticker() { window.__t.push('tick'); if (++k > 1) clearInterval(iv); }, 1000);</script>`);
  await settle(500);
  const t = await ev(page, () => window.__t);
  assert.ok(t.includes('count') && t.filter((x) => x === 'tick').length >= 1, JSON.stringify(t));
  assert.ok(!t.includes('other'));
  assert.equal(await ev(page, () => window.__ids), 'number');
});

test('no-window-open-if: no argument blocks every script-opened window, a needle only the matching ones', async () => {
  const all = await open([['nowoif']], '<script>window.__w = window.open("https://ads.example/x", "_blank"); window.__t = typeof window.__w; window.__c = window.__w.closed</script>');
  await settle(200);
  assert.equal(all.popups.length, 0);
  assert.equal(await ev(all, () => window.__t), 'object');
  assert.equal(await ev(all, () => window.__c), false);
  const some = await open([['nowoif', 'ads.example']], '<button id="b" onclick="window.open(\'https://ads.example/x\'); window.open(\'https://fine.example/y\')">go</button>');
  await some.click('#b'); await settle(400);
  assert.equal(some.popups.length, 1);
  assert.match(some.popups[0].url(), /fine\.example/);
});

test('noeval-if: matching code is not evaluated, other code is', async () => {
  const page = await open([['noeval-if', 'blocked-code']], '<script>eval("window.__a = 1 // blocked-" + "code"); eval("window.__b = 2"); window.__r = eval("1 + 1")</script>');
  assert.equal(await ev(page, () => window.__a), undefined);
  assert.equal(await ev(page, () => window.__b), 2);
  assert.equal(await ev(page, () => window.__r), 2);
  // with no argument nothing is evaluated at all (the harness itself uses eval, so the answer is read from the title)
  const none = await open([['noeval']], '<script>document.title = "r=" + eval("1 + 1")</script>');
  assert.equal(await none.title(), 'r=undefined');
});

test('no-fetch-if: a matching request is answered at once and never leaves, others go out', async () => {
  const page = await open([['no-fetch-if', 'ads.example'], ['no-fetch-if', 'method:HEAD']], '');
  const r = await page.evaluate(async () => {
    const a = await fetch('https://ads.example/pagead.js'); const at = await a.text();
    const b = await fetch('https://site.example/data.json'); const bt = await b.text();
    const c = await fetch('https://site.example/data.json', { method: 'HEAD' });
    return [a.status, at, bt, c.status, (await c.text())];
  });
  assert.deepEqual(r, [200, '{}', '{"ok":1}', 200, '{}']);
  assert.ok(!page.hits.some((h) => h.startsWith('ads.example')), page.hits.join(','));
  assert.equal(page.hits.filter((h) => h === 'site.example/data.json').length, 1, 'the HEAD request never went out either');
});

test('no-xhr-if: a matching XMLHttpRequest finishes at once with an empty answer and never leaves', async () => {
  const page = await open([['no-xhr-if', 'googlesyndication']], '');
  const r = await page.evaluate(() => new Promise((res) => {
    const x = new XMLHttpRequest(); const log = [];
    x.onreadystatechange = () => log.push('rs' + x.readyState);
    x.onload = () => res([x.status, x.responseText, log.join(','), x.responseURL]);
    x.open('GET', 'https://pagead2.googlesyndication.com/x.js'); x.send();
  }));
  assert.equal(r[0], 200); assert.equal(r[1], '{}'); assert.match(r[3], /googlesyndication/);
  assert.ok(!page.hits.some((h) => h.includes('googlesyndication')));
  const ok = await page.evaluate(() => new Promise((res) => { const x = new XMLHttpRequest(); x.onload = () => res(x.responseText); x.open('GET', '/data.json'); x.send(); }));
  assert.equal(ok, '{"ok":1}');
});

test('json-prune: named paths are cut from parsed JSON, wildcards and array steps work, needles gate it', async () => {
  const page = await open([['json-prune', 'adPlacements playerResponse.adSlots list.[-].ad extra.*.tracker'], ['json-prune', 'gated', 'marker']], '');
  const r = await page.evaluate(() => {
    const a = JSON.parse('{"adPlacements":[1],"playerResponse":{"adSlots":[2],"keep":3},"list":[{"ad":1,"v":1},{"ad":2,"v":2}],"extra":{"a":{"tracker":1,"k":1},"b":{"tracker":2}},"x":1}');
    const b = JSON.parse('{"gated":1,"marker":1}'); const c = JSON.parse('{"gated":1}');
    return [JSON.stringify(a), JSON.stringify(b), JSON.stringify(c)];
  });
  assert.equal(r[0], '{"playerResponse":{"keep":3},"list":[{"v":1},{"v":2}],"extra":{"a":{"k":1},"b":{}},"x":1}');
  assert.equal(r[1], '{"marker":1}');
  assert.equal(r[2], '{"gated":1}');
});

test('json-prune-fetch-response and -xhr-response: the answer of a matching request is cut when read', async () => {
  const body = '{"playerAds":[1],"adPlacements":[2],"ok":1}';
  const page = await open([['json-prune-fetch-response', 'playerAds adPlacements', '', 'propsToMatch', '/data.json'], ['json-prune-xhr-response', 'playerAds adPlacements', '', 'propsToMatch', 'xhr=1']], '', { json: body });
  const r = await page.evaluate(async () => {
    const f = await (await fetch('/data.json')).json();
    const other = await (await fetch('/other.json')).text();
    return [JSON.stringify(f), other.length > 0];
  });
  assert.equal(r[0], '{"ok":1}');
  const x = await page.evaluate(() => new Promise((res) => { const q = new XMLHttpRequest(); q.onload = () => res(q.responseText); q.open('GET', '/data.json?xhr=1'); q.send(); }));
  assert.equal(x, '{"ok":1}');
  const j = await page.evaluate(() => new Promise((res) => { const q = new XMLHttpRequest(); q.responseType = 'json'; q.onload = () => res(q.response); q.open('GET', '/data.json?xhr=1'); q.send(); }));
  assert.deepEqual(j, { ok: 1 });
});

test('remove-attr and remove-class: matching elements lose them, with stay also what the page adds later', async () => {
  const page = await open([['ra', 'onclick|data-x', '.item'], ['rc', 'locked', 'body', 'stay']],
    '<div class="item" onclick="x()" data-x="1" id="d1"></div><script>document.body.className = "locked"; setTimeout(function () { document.body.className = "locked again"; }, 100)</script>');
  await settle(500);
  assert.equal(await ev(page, () => document.getElementById('d1').hasAttribute('onclick') || document.getElementById('d1').hasAttribute('data-x')), false);
  assert.equal(await ev(page, () => document.body.classList.contains('locked')), false);
  assert.equal(await ev(page, () => document.body.classList.contains('again')), true);
});

test('remove-node-text and replace-node-text: scripts the page inserts, and the ones in the page itself, are changed before they run', async () => {
  // (the needles are built from pieces so that the test page's own script does not hold them)
  const page = await open([['rmnt', 'script', 'adbait'], ['rpnt', 'script', 'isBlocked = true', 'isBlocked = false', 'condition', 'guard']],
    `<script>
      var A = 'ad' + 'bait', B = 'isBlocked' + ' = true', G = 'gu' + 'ard';
      var s = document.createElement('script'); s.textContent = 'window.__ran1 = 1; /* ' + A + ' */'; document.head.appendChild(s);
      var t = document.createElement('script'); t.textContent = 'window.__v = "' + B + '"; /* ' + G + ' */'; document.body.appendChild(t);
      var u = document.createElement('script'); u.textContent = 'window.__u = "' + B + '"'; document.body.appendChild(u);
     </script>
     <script>window.__parsed = 1; /* adbait */</script>
     <script>window.__plain = 1;</script>`);
  assert.equal(await ev(page, () => window.__ran1), undefined, 'inserted by the page: text removed before it ran');
  assert.equal(await ev(page, () => window.__v), 'isBlocked = false');
  assert.equal(await ev(page, () => window.__u), 'isBlocked = true', 'the condition holds the rule back');
  assert.equal(await ev(page, () => window.__parsed), undefined, 'in the page itself: the observer runs before the parser executes the script');
  assert.equal(await ev(page, () => window.__plain), 1);
});

test('set-cookie and set-local-storage-item: safe values only, trusted forms take more', async () => {
  const page = await open([['set-cookie', 'consent', 'accepted'], ['set-cookie', 'bad', 'x;y=z'], ['set-cookie', 'free', 'whatever'], ['trusted-set-cookie', 'tc', 'any value', '1day'],
    ['set-local-storage-item', 'agreed', 'true'], ['set-local-storage-item', 'odd', 'free text'], ['trusted-set-local-storage-item', 'tl', 'free text'], ['set-session-storage-item', 'ss', 'yes']], '');
  const c = await ev(page, () => document.cookie);
  assert.match(c, /consent=accepted/); assert.match(c, /tc=any value/); assert.doesNotMatch(c, /bad=/); assert.doesNotMatch(c, /free=/);
  assert.deepEqual(await ev(page, () => [localStorage.getItem('agreed'), localStorage.getItem('odd'), localStorage.getItem('tl'), sessionStorage.getItem('ss')]), ['true', null, 'free text', 'yes']);
});

test('remove-cookie: a cookie whose name matches is expired', async () => {
  const page = await open([['remove-cookie', 'tracker_']], '<script>document.cookie = "tracker_id=1; path=/"; document.cookie = "keep=1; path=/"</script>');
  await settle(300);
  const c = await ev(page, () => document.cookie);
  assert.doesNotMatch(c, /tracker_id/); assert.match(c, /keep=1/);
});

test('nowebrtc: a peer connection can still be made but nothing is gathered', async () => {
  const page = await open([['nowebrtc']], '<script>try { var p = new RTCPeerConnection({ iceServers: [] }); window.__ok = typeof p.createDataChannel(); window.__state = p.iceConnectionState; window.__ice = p.onicecandidate } catch (e) { window.__ok = "threw " + e }</script>');
  assert.equal(await ev(page, () => window.__ok), 'object');
  assert.equal(await ev(page, () => window.__state), 'closed');
});

test('abort-on-stack-trace: only a read made under a function with the needle in its stack throws', async () => {
  const page = await open([['aost', 'document.title', 'trackerFn']],
    '<script>function trackerFn() { return document.title } function fine() { return document.title } try { trackerFn(); window.__a = "read" } catch (e) { window.__a = e.name } try { fine(); window.__b = "read" } catch (e) { window.__b = e.name }</script>');
  assert.equal(await ev(page, () => window.__a), 'ReferenceError');
  assert.equal(await ev(page, () => window.__b), 'read');
});

test('robustness: bad calls and bad JSON are ignored, one failing call does not stop the others, the same calls run once, the page is told how it went', async () => {
  const page = await open([['no-such-scriptlet', 'x'], 'not even an array', ['set', 'good', 'true'], ['nostif', '[unclosed'], ['aopr']], '<script>window.__g = window.good</script>');
  assert.equal(await ev(page, () => window.__g), true);
  assert.deepEqual(await ev(page, () => window.__n), ['3:2:1'], 'ran:threw:before any page script');
  const bad = await open('{{{', '<script>window.__x = 1</script>');
  assert.equal(await ev(bad, () => window.__x), 1);
  assert.deepEqual(await ev(bad, () => window.__n), []);
  // the same payload sent again (a second answer for the same page) is not run twice
  const twice = await open([['set', 'once', '1']], '');
  await twice.evaluate(() => document.dispatchEvent(new CustomEvent('__kst_sl_v1', { detail: '[["set","once","1"]]' })));
  assert.deepEqual(await ev(twice, () => window.__n), ['1:0:1']);
});

test('early delivery: every CALL runs once however it is sent; a message with only known calls reports nothing, one with a new call reports just that one', async () => {
  const page = await open([['set', 'a', '1'], ['set', 'b', '2']], '<script>window.__a = window.a; window.__b = window.b</script>');
  assert.deepEqual(await ev(page, () => window.__n), ['2:0:1']);
  await page.evaluate(() => {
    document.dispatchEvent(new CustomEvent('__kst_sl_v1', { detail: '[["set","b","2"],["set","a","1"]]' })); // all known (the late hand-off after an early run)
    document.dispatchEvent(new CustomEvent('__kst_sl_v1', { detail: '[["set","b","2"],["set","c","3"]]' })); // one new
  });
  assert.deepEqual(await ev(page, () => window.__n), ['2:0:1', '1:0:0']);
  assert.equal(await ev(page, () => window.c), 3);
});

test('early delivery: the report says whether the calls ran before the first script of the page', async () => {
  const early = await open([['nowoif']], '<script>window.__x = 1</script>');
  assert.deepEqual(await ev(early, () => window.__n), ['1:0:1']);
  // the same calls arriving after the page's own scripts were parsed (the late hand-off) are marked as late
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: '<!doctype html><html><head><title>t</title><script>window.__first = 1</script></head><body></body></html>' }));
  const late = await context.newPage();
  await late.addInitScript({ content: LIB });
  await late.addInitScript(() => { window.__n = []; document.addEventListener('__kst_sl_n_v1', (e) => window.__n.push(String(e.detail)), true); });
  await late.goto('https://site.example/');
  await late.evaluate(() => document.dispatchEvent(new CustomEvent('__kst_sl_v1', { detail: '[["nowoif"]]' })));
  assert.deepEqual(await ev(late, () => window.__n), ['1:0:0']);
});

test('early delivery: a page that fires our event over and over cannot make the library run more than 300 distinct calls', async () => {
  const page = await open(null, '');
  await page.evaluate(() => {
    for (let i = 0; i < 20; i++) {
      const calls = []; for (let j = 0; j < 60; j++) calls.push(['set', 'v' + i + '_' + j, '1']);
      document.dispatchEvent(new CustomEvent('__kst_sl_v1', { detail: JSON.stringify(calls) }));
    }
  });
  const ran = (await ev(page, () => window.__n)).map((r) => parseInt(r.split(':')[0], 10)).reduce((a, b) => a + b, 0);
  assert.equal(ran, 300);
});

test('nothing leaks onto the page: one marker property, no globals added by the library', async () => {
  const page = await open([['set', 'a', 'true']], '');
  const added = await page.evaluate(() => Object.getOwnPropertyNames(window).filter((n) => /^__kst|^__n$/.test(n)));
  assert.deepEqual(added.sort(), ['__kstSL', '__n'].sort());
  assert.equal(await page.evaluate(() => typeof window.__kstSL), 'number');
});
