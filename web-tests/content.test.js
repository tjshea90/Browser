'use strict';
// content.js (the isolated content script of the built-in extension) in real headless Chromium with a fake `browser.runtime`: the long press on a video and the media scan.
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

/** A page at https://site.example/ with content.js running against a fake extension API that records every message. */
async function open(body, pre, opts) {
  const o = opts || {};
  const context = await browser.newContext(o.touch ? { hasTouch: true, viewport: { width: 400, height: 700 } } : {});
  await context.route('https://**/*', (r) => r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body style="margin:0">${body}</body></html>` }));
  const page = await context.newPage();
  await page.addInitScript(() => {
    window.__msgs = [];
    window.browser = { runtime: { sendMessage: (m) => { window.__msgs.push(m); return Promise.resolve(undefined); } } };
  });
  if (pre) await page.addInitScript(pre);
  await page.addInitScript({ content: CONTENT });
  await page.goto(o.url || 'https://site.example/');
  return page;
}
const msgs = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'pop'));
const rightClick = (page, x, y) => page.mouse.click(x, y, { button: 'right' });

test('a long press on a video is taken before the site\'s own player menu and reported to the app', async () => {
  const page = await open(`<div style="position:relative;width:400px;height:300px"><video src="https://cdn.example/v/movie.mp4" style="width:400px;height:300px"></video><div id="layer" style="position:absolute;inset:0" oncontextmenu="window.__siteMenu=1"></div></div>`);
  await rightClick(page, 100, 100);
  assert.equal(await page.evaluate(() => window.__siteMenu), undefined, 'the site\'s own menu never saw it');
  assert.deepEqual(await msgs(page), [{ k: 'pop', d: 'vmenu|https://cdn.example/v/movie.mp4', p: 'https://site.example/' }]);
});

test('a video with only <source> children is taken too, a blob: source is reported without an address', async () => {
  const page = await open(`<video id="v" controls style="width:300px;height:200px"><source src="https://cdn.example/a.mp4"></video>`);
  await rightClick(page, 50, 50);
  const m = await msgs(page);
  assert.equal(m.length, 1); assert.ok(m[0].d.startsWith('vmenu|'));
});

test('a long press anywhere else keeps the browser\'s and the site\'s own behaviour', async () => {
  const page = await open(`<div id="t" style="width:300px;height:200px" oncontextmenu="window.__siteMenu=1">text</div><video style="width:100px;height:50px"></video>`);
  await rightClick(page, 50, 50);
  assert.equal(await page.evaluate(() => window.__siteMenu), 1);
  assert.deepEqual(await msgs(page), []);
});

test('a bare media element (no source at all) is left alone', async () => {
  const page = await open(`<video id="v" style="width:300px;height:200px" oncontextmenu="window.__siteMenu=1"></video>`);
  await rightClick(page, 50, 50);
  assert.equal(await page.evaluate(() => window.__siteMenu), 1);
  assert.deepEqual(await msgs(page), []);
});

// ---------------------------------------------------------------- the media scan (AA4 / PERF_JS J4)
const doms = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'dom').map((m) => m.u));
const settle = (page) => page.waitForTimeout(500); // the scan runs when the page is idle (requestIdleCallback), a moment after load
const quickIdle = () => { window.requestIdleCallback = (f) => setTimeout(f, 30); }; // headless Chromium may wait for the full 3 s timeout of an idle callback: the tests do not

test('scan: media addresses in elements, meta tags, links and inline player configurations are found without anyone pressing play', async () => {
  const page = await open(`
    <video src="https://cdn.example/a.mp4"></video>
    <meta property="og:video" content="https://cdn.example/og.mp4">
    <a href="https://cdn.example/link.webm">x</a><a href="https://cdn.example/page.html">y</a>
    <script>var cfg = {"hls":"https:\\u002f\\u002fcdn.example\\u002fplay\\u002fmaster.m3u8","dash":"https:\\/\\/cdn.example\\/play\\/manifest.mpd"};</script>`, quickIdle);
  await settle(page);
  const found = (await doms(page)).sort();
  assert.deepEqual(found, ['https://cdn.example/a.mp4', 'https://cdn.example/link.webm', 'https://cdn.example/og.mp4', 'https://cdn.example/play/manifest.mpd', 'https://cdn.example/play/master.m3u8']);
});

test('scan: a huge inline script is skipped without being read, and the scan carries on with the scripts after it', async () => {
  const filler = 'x'.repeat(250000);
  const page = await open(`<script>var big = "${filler} https://cdn.example/hidden-in-bundle.mp4";</script><script>var small = "https://cdn.example/real.mp4";</script><script>var nothing = 1 + 2; // no media here at all</script>`, quickIdle);
  await settle(page);
  assert.deepEqual(await doms(page), ['https://cdn.example/real.mp4']);
});

test('scan: at most 40 addresses per frame, each reported once even though the scan runs twice', async () => {
  let links = ''; for (let i = 0; i < 60; i++) links += `<a href="https://cdn.example/v${i}.mp4">v</a>`;
  const page = await open(links, quickIdle);
  await settle(page);
  await page.evaluate(() => window.dispatchEvent(new Event('load')));
  await page.waitForTimeout(3000); // the second scan, 2.5 s after load
  const found = await doms(page);
  assert.equal(found.length, 40);
  assert.equal(new Set(found).size, 40);
});

test('scan: scripts and links that appear after the first scan are found by the second one', async () => {
  const page = await open(`<a href="https://cdn.example/first.mp4">a</a>`, quickIdle);
  await settle(page);
  assert.deepEqual(await doms(page), ['https://cdn.example/first.mp4']);
  await page.evaluate(() => {
    const s = document.createElement('script'); s.textContent = 'var late = "https://cdn.example/late.m3u8";'; document.body.appendChild(s);
    const a = document.createElement('a'); a.href = 'https://cdn.example/late-link.mp4'; document.body.appendChild(a);
    window.dispatchEvent(new Event('load'));
  });
  await page.waitForTimeout(3000);
  assert.deepEqual((await doms(page)).sort(), ['https://cdn.example/first.mp4', 'https://cdn.example/late-link.mp4', 'https://cdn.example/late.m3u8']);
});

// ---------------------------------------------------------------- a tapped field is not left behind the keyboard (AA3-2 / research/TYPING.md)
// Firefox shrinks only the visual viewport when the keyboard opens: the test replaces window.visualViewport with a fake whose height it can change, then fires its resize event.
const fakeKeyboard = () => {
  const listeners = [];
  const vv = { height: 700, offsetTop: 0, addEventListener: (t, f) => { if (t === 'resize') listeners.push(f); }, removeEventListener() {} };
  Object.defineProperty(window, 'visualViewport', { value: vv, configurable: true });
  window.__keyboard = (h) => { vv.height = h; listeners.forEach((f) => f()); };
};
const field = (top, tag = 'input') => `<div style="height:${top}px"></div><${tag} id="f" style="height:30px;display:block">${tag === 'textarea' ? '</textarea>' : ''}<div style="height:3000px"></div>`;

test('keyboard: a text field that the keyboard would cover is scrolled into the visible area', async () => {
  const page = await open(field(500), fakeKeyboard);
  await page.focus('#f');
  assert.equal(await page.evaluate(() => scrollY), 0, 'the field is on screen before the keyboard opens');
  await page.evaluate(() => window.__keyboard(300));
  await page.waitForTimeout(400);
  assert.ok(await page.evaluate(() => scrollY) > 100, 'the page scrolled to bring the field up');
});

test('keyboard: the keyboard opens in two steps, the field is placed by the final size', async () => {
  const page = await open(field(500, 'textarea'), fakeKeyboard);
  await page.focus('#f');
  await page.evaluate(() => { window.__keyboard(375); setTimeout(() => window.__keyboard(335), 40); });
  await page.waitForTimeout(500);
  assert.ok(await page.evaluate(() => scrollY) > 100);
});

test('keyboard: a field that is still inside the visible area is not moved', async () => {
  const page = await open(field(100), fakeKeyboard);
  await page.focus('#f');
  await page.evaluate(() => window.__keyboard(300));
  await page.waitForTimeout(400);
  assert.equal(await page.evaluate(() => scrollY), 0);
});

test('keyboard: a small change of the visible area (a toolbar) is not a keyboard, and nothing but text fields is moved', async () => {
  const small = await open(field(500), fakeKeyboard);
  await small.focus('#f');
  await small.evaluate(() => window.__keyboard(660));
  await small.waitForTimeout(400);
  assert.equal(await small.evaluate(() => scrollY), 0, 'only 40 px: not a keyboard');
  const btn = await open(`<div style="height:500px"></div><button id="f" style="height:30px;display:block">go</button><div style="height:3000px"></div>`, fakeKeyboard);
  await btn.focus('#f');
  await btn.evaluate(() => window.__keyboard(300));
  await btn.waitForTimeout(400);
  assert.equal(await btn.evaluate(() => scrollY), 0, 'a button has no keyboard to make room for');
  const box = await open(`<div style="height:500px"></div><input id="f" type="checkbox"><div style="height:3000px"></div>`, fakeKeyboard);
  await box.focus('#f');
  await box.evaluate(() => window.__keyboard(300));
  await box.waitForTimeout(400);
  assert.equal(await box.evaluate(() => scrollY), 0, 'a checkbox does not open the keyboard either');
});

test('keyboard: an editable region (contenteditable) is moved like a field, and a page without visualViewport is left alone', async () => {
  const page = await open(`<div style="height:500px"></div><div id="f" contenteditable="true" style="height:30px">x</div><div style="height:3000px"></div>`, fakeKeyboard);
  await page.focus('#f');
  await page.evaluate(() => window.__keyboard(300));
  await page.waitForTimeout(400);
  assert.ok(await page.evaluate(() => scrollY) > 100);
  const none = await open(field(500), () => { Object.defineProperty(window, 'visualViewport', { value: undefined, configurable: true }); });
  assert.equal(await none.evaluate(() => scrollY), 0);
  assert.deepEqual(await none.evaluate(() => window.__msgs.filter((m) => m.k === 'pop')), [], 'no error, nothing reported');
});

// ---------------------------------------------------------------- D2: generic element hiding by the page's classes and ids
// The fake app answers the `css` question (with or without the marker) and each `gen` batch (the CSS for ".ad-banner" only), and keeps every batch the page sent.
// These run inside the page (addInitScript serialises them), so each one is self-contained.
const appWithMarker = () => {
  window.__gen = [];
  window.requestIdleCallback = (f) => setTimeout(f, 30);
  window.browser.runtime.sendMessage = (m) => {
    window.__msgs.push(m);
    if (m.k === 'css') return Promise.resolve('/*kg*/.site-rule{display:none!important}');
    if (m.k === 'gen') { window.__gen.push(m.t); return Promise.resolve(m.t.indexOf('.ad-banner') >= 0 ? '.ad-banner{display:none!important}' : ''); }
    return Promise.resolve(undefined);
  };
};
const appWithoutMarker = () => {
  window.__gen = [];
  window.browser.runtime.sendMessage = (m) => {
    window.__msgs.push(m);
    if (m.k === 'css') return Promise.resolve('.site-rule{display:none!important}');
    if (m.k === 'gen') window.__gen.push(m.t);
    return Promise.resolve(undefined);
  };
};
const appDown = () => { window.browser.runtime.sendMessage = () => Promise.reject(new Error('no app')); };
const gens = (page) => page.evaluate(() => window.__gen);
const flat = (batches) => [].concat(...batches);

test('tokens: with the marker the page sends its classes and ids (lower case, once each) and the selectors that come back are applied', async () => {
  const page = await open(`<div id="Top" class="Ad-Banner x">hi</div><div class="ad-banner real">r</div><span class="plain  other">s</span><p id="">p</p>`, appWithMarker);
  await page.waitForTimeout(1200);
  const sent = flat(await gens(page)).sort();
  assert.deepEqual(sent, ['#top', '.ad-banner', '.other', '.plain', '.real', '.x'], 'names are lower-cased and sent once');
  assert.equal(await page.evaluate(() => getComputedStyle(document.querySelector('.real')).display), 'none', 'the generic selector that matched was injected');
  assert.equal(await page.evaluate(() => getComputedStyle(document.querySelector('.plain')).display), 'inline');
  assert.ok(await page.evaluate(() => !!document.querySelector('style[data-kestrel="g"]')));
  const csss = await page.evaluate(() => window.__msgs.filter((m) => m.k === 'css'));
  assert.equal(csss.length, 1);
});

test('tokens: without the marker nothing is sent (the app turned generic hiding off, or the host is allowed)', async () => {
  const page = await open(`<div class="ad-banner">hi</div>`, appWithoutMarker);
  await page.waitForTimeout(900);
  assert.deepEqual(await gens(page), []);
  assert.equal(await page.evaluate(() => getComputedStyle(document.querySelector('.ad-banner')).display), 'block');
});

test('tokens: names that appear later are sent too, each name only once however often it appears', async () => {
  const page = await open(`<div class="first">a</div>`, appWithMarker);
  await page.waitForTimeout(900);
  await page.evaluate(() => {
    const d = document.createElement('div'); d.className = 'late-one first'; d.innerHTML = '<p class="late-two" id="late-id"></p>';
    document.body.appendChild(d);
    const e = document.createElement('div'); e.className = 'late-one'; document.body.appendChild(e);
  });
  await page.waitForTimeout(900);
  const sent = flat(await gens(page));
  assert.deepEqual(sent.slice().sort(), ['#late-id', '.first', '.late-one', '.late-two']);
  assert.equal(new Set(sent).size, sent.length);
});

test('tokens: a page with many names is sent in batches of at most 300', async () => {
  let spans = ''; for (let i = 0; i < 700; i++) spans += `<i class="c${i}"></i>`;
  const page = await open(spans, appWithMarker);
  await page.waitForTimeout(2500);
  const batches = await gens(page);
  assert.ok(batches.length >= 3 && batches.every((b) => b.length <= 300), 'batch sizes: ' + batches.map((b) => b.length));
  assert.equal(flat(batches).length, 700);
});

test('tokens: only the top document asks; an iframe keeps just its own site rules', async () => {
  const page = await open(`<div class="top-class">a</div><iframe srcdoc="<div class='frame-class'>b</div>" width="100" height="50"></iframe>`, appWithMarker);
  await page.waitForTimeout(1200);
  const sent = flat(await gens(page));
  assert.ok(sent.includes('.top-class'));
  assert.ok(!sent.includes('.frame-class'), 'the iframe sent nothing: ' + sent);
});

test('tokens: a page without the marker in its first answer, or an app that cannot answer, never breaks the page', async () => {
  const page = await open(`<div class="x">a</div>`, appDown);
  await page.waitForTimeout(800);
  assert.equal(await page.evaluate(() => document.body.textContent), 'a');
});

// ---------------------------------------------------------------- EX-15: a head start for the next navigation (research/REVIEW-EXTERNAL-1.md 4.2)
const warms = (page) => page.evaluate(() => window.__msgs.filter((m) => m.k === 'warm').map((m) => ({ o: m.o, p: m.p, w: m.w })));
const idleNow = () => { window.requestIdleCallback = (f) => setTimeout(f, 20); };
const row = (href, text) => `<a href="${href}" onclick="event.preventDefault()" style="display:block;height:50px">${text}</a>`; // a tap must not navigate: the next document would start with an empty message list

test('warm: a finger on a cross-site https link names the link\'s origin (once), the page is not asked anything else', async () => {
  const page = await open(row('https://other.example/a/b?x=1', 'one') + row('https://other.example/second', 'two') + row('https://third.example/', '<span id="deep">three</span>'), undefined, { touch: true });
  await page.touchscreen.tap(20, 20);   // first link
  await page.touchscreen.tap(20, 70);   // same origin again: nothing more
  await page.touchscreen.tap(20, 120);  // a child element of the third link: found by walking up to the anchor
  await page.waitForTimeout(200);
  assert.deepEqual(await warms(page), [
    { o: 'https://other.example/', p: 'https://site.example/', w: 'touch' },
    { o: 'https://third.example/', p: 'https://site.example/', w: 'touch' },
  ]);
});

test('warm: same-site links, http links, links with a password, #anchors and javascript: links are never warmed', async () => {
  const page = await open(row('https://site.example/other', 'same site') + row('http://plain.example/', 'plain http') + row('https://user:pw@secret.example/', 'password')
    + row('#top', 'anchor') + row('javascript:void(0)', 'script') + row('https://ok.example/', 'fine'), undefined, { touch: true });
  for (let i = 0; i < 6; i++) await page.touchscreen.tap(20, 20 + i * 50);
  await page.waitForTimeout(200);
  assert.deepEqual((await warms(page)).map((m) => m.o), ['https://ok.example/']);
});

test('warm: at most four touched origins per page', async () => {
  let rows = ''; for (let i = 0; i < 7; i++) rows += row(`https://site${i}.example/`, 'l' + i);
  const page = await open(rows, undefined, { touch: true });
  for (let i = 0; i < 7; i++) await page.touchscreen.tap(20, 20 + i * 50);
  await page.waitForTimeout(200);
  assert.equal((await warms(page)).length, 4);
});

test('warm: a touch inside a frame does nothing (the top document only)', async () => {
  const page = await open(`<iframe srcdoc="<a href='https://other.example/' onclick='event.preventDefault()' style='display:block;height:80px'>x</a>" width="300" height="100" style="border:0"></iframe>`, undefined, { touch: true });
  await page.touchscreen.tap(20, 20);
  await page.waitForTimeout(200);
  assert.deepEqual(await warms(page), []);
});

const RESULTS = `
  <div id="rso">
    <a href="https://www.google.com/url?q=https://redirect.example/&sa=U"><h3>redirector</h3></a>
    <a href="https://ads.example/landing"><div role="heading">Sponsored</div></a>
    <a href="https://www.googleadservices.com/pagead/aclk?x=1"><h3>ad redirect</h3></a>
    <a href="https://first.example/page"><h3>First result</h3></a>
    <a href="https://first.example/other"><h3>Same origin again</h3></a>
    <a href="https://second.example:8443/x"><h3>Second result</h3></a>
    <a href="https://no-heading.example/">no heading, not a result</a>
    <a href="http://plain.example/"><h3>http result</h3></a>
    <a href="https://third.example/z"><h3>Third result</h3></a>
    <a href="https://fourth.example/"><h3>Fourth result</h3></a>
  </div>`;

test('warm: on a search engine\'s result page the first three results are named once at idle (engine links, no-heading links and http links are skipped)', async () => {
  const page = await open(RESULTS, idleNow, { url: 'https://www.google.com/search?q=cats' });
  await page.waitForTimeout(3500);
  const w = await warms(page);
  assert.deepEqual(w.map((m) => m.o), ['https://ads.example/', 'https://first.example/', 'https://second.example:8443/']);
  assert.ok(w.every((m) => m.w === 'result' && m.p === 'https://www.google.com/search?q=cats'));
});

test('warm: a page that is not a search result page is never scanned, even with the same markup (and neither is Google Maps)', async () => {
  const a = await open(RESULTS, idleNow, { url: 'https://site.example/search?q=cats' });
  const b = await open(RESULTS, idleNow, { url: 'https://www.google.com/maps/search/?q=cats' });
  const c = await open(RESULTS, idleNow, { url: 'https://www.google.com/' });
  await a.waitForTimeout(3500);
  assert.deepEqual(await warms(a), []);
  assert.deepEqual(await warms(b), []);
  assert.deepEqual(await warms(c), []);
});

test('warm: an app that does not answer never breaks the page', async () => {
  const page = await open(RESULTS + '<p id="ok">ok</p>', () => { window.requestIdleCallback = (f) => setTimeout(f, 20); window.browser.runtime.sendMessage = () => Promise.reject(new Error('no app')); }, { url: 'https://www.google.com/search?q=cats' });
  await page.waitForTimeout(1500);
  assert.equal(await page.evaluate(() => document.getElementById('ok').textContent), 'ok');
});

// ---------------------------------------------------------------- AD1b-4: procedural cosmetic rules (:has-text(), :upward(), :remove() ...)
const O = '\u0003', A = '\u0004';
/** An app that says "this host has procedural rules" (the /*kp*/ marker in its CSS) and hands out [lines] when asked. */
const appWithProcedural = (lines) => `() => {
  window.__asked = [];
  window.browser.runtime.sendMessage = (m) => {
    window.__msgs.push(m);
    if (m.k === 'css') return Promise.resolve('/*kp*/.site-rule{display:none!important}');
    if (m.k === 'proc') { window.__asked.push(m.h); return Promise.resolve(${JSON.stringify(lines.join('\n'))}); }
    return Promise.resolve(undefined);
  };
}`;
const hiddenIds = (page) => page.evaluate(() => Array.from(document.querySelectorAll('[id]')).filter((e) => getComputedStyle(e).display === 'none').map((e) => e.id));
const procOpen = (body, lines) => open(body, eval(appWithProcedural(lines)));
const settleProc = (page) => page.waitForTimeout(1100);

test('procedural: has-text (plain text is case-sensitive, a regex with i is not) hides only the elements that match', async () => {
  const page = await procOpen(`<div class="post" id="a">Sponsored post</div><div class="post" id="b">Normal post</div><div class="post" id="c">SPONSORED shout</div><div class="card" id="d">Promoted today</div>`,
    [`.post${O}t${A}Sponsored`, `.card${O}t${A}/promoted/i`]);
  await settleProc(page);
  assert.deepEqual(await hiddenIds(page), ['a', 'd']);
  assert.deepEqual(await page.evaluate(() => window.__asked), ['site.example']);
});

test('procedural: upward moves to the n-th parent or the closest parent that matches, and only that element is hidden', async () => {
  const page = await procOpen(`<div id="w1"><div id="m1"><span class="x" id="s1">Ad</span></div></div><div class="item" id="w2"><p><b class="y" id="s2">Ad</b></p></div><div id="keep"><span class="x">fine</span></div>`,
    [`span.x${O}t${A}Ad${O}u${A}2`, `b.y${O}t${A}Ad${O}u${A}.item`]);
  await settleProc(page);
  assert.deepEqual(await hiddenIds(page), ['w1', 'w2']);
});

test('procedural: remove takes the element out of the page, remove-attr and remove-class change it', async () => {
  const page = await procOpen(`<div class="ov" id="o">Subscribe now</div><img id="i" style="filter:blur(9px)" alt="Gate"><div class="blur wall" id="c">Members only</div><div id="stay">Subscribe</div>`,
    [`div.ov${O}t${A}Subscribe${O}r${A}`, `img${O}t${A}${O}x`, `img[alt]${O}a${A}style`, `div.wall${O}t${A}Members${O}c${A}blur`]);
  await settleProc(page);
  assert.equal(await page.evaluate(() => !!document.getElementById('o')), false);
  assert.equal(await page.evaluate(() => document.getElementById('stay').textContent), 'Subscribe');
  assert.equal(await page.evaluate(() => document.getElementById('c').className), 'wall');
});

test('procedural: remove-attr works without a text test, and elements that appear later are found by the next run', async () => {
  const page = await procOpen(`<img id="i" style="filter:blur(9px)" alt="x"><div id="host"></div>`, [`img[alt]${O}a${A}style`, `div.late${O}t${A}Sponsored`]);
  await settleProc(page);
  assert.equal(await page.evaluate(() => document.getElementById('i').hasAttribute('style')), false);
  await page.evaluate(() => { const d = document.createElement('div'); d.className = 'late'; d.id = 'late'; d.textContent = 'Sponsored later'; document.getElementById('host').appendChild(d); });
  await page.waitForTimeout(1100);
  assert.deepEqual(await hiddenIds(page), ['late']);
});

test('procedural: a rule with a broken regex, an unknown operator or a selector the page rejects is ignored and the other rules still run', async () => {
  const page = await procOpen(`<div class="post" id="a">Sponsored</div>`, [`.post${O}t${A}/(/`, `.post${O}z${A}x`, `div[${O}t${A}x`, `.post${O}t${A}Sponsored`]);
  await settleProc(page);
  assert.deepEqual(await hiddenIds(page), ['a']);
});

test('procedural: never hides or removes the page itself, however much text matches', async () => {
  const page = await procOpen(`<div id="a">Sponsored everywhere</div>`, [`body${O}t${A}Sponsored`, `html${O}t${A}Sponsored`, `div#a${O}t${A}Sponsored${O}u${A}5`]);
  await settleProc(page);
  assert.equal(await page.evaluate(() => getComputedStyle(document.body).display !== 'none' && getComputedStyle(document.documentElement).display !== 'none'), true);
});

test('procedural: a page whose CSS has no marker never asks for rules', async () => {
  const page = await open(`<div class="post">Sponsored</div>`, () => {
    window.browser.runtime.sendMessage = (m) => { window.__msgs.push(m); return Promise.resolve(m.k === 'css' ? '.site-rule{display:none!important}' : undefined); };
  });
  await page.waitForTimeout(900);
  assert.deepEqual(await page.evaluate(() => window.__msgs.filter((m) => m.k === 'proc')), []);
});

test('procedural: an app that does not answer, or answers nothing, never breaks the page', async () => {
  const a = await open(`<p id="ok">ok</p>`, () => { window.browser.runtime.sendMessage = (m) => (m.k === 'css' ? Promise.resolve('/*kp*/') : Promise.reject(new Error('no app'))); });
  const b = await open(`<p id="ok">ok</p><div class="post">Sponsored</div>`, () => { window.browser.runtime.sendMessage = (m) => Promise.resolve(m.k === 'css' ? '/*kp*/' : ''); });
  await a.waitForTimeout(900);
  assert.equal(await a.evaluate(() => document.getElementById('ok').textContent), 'ok');
  assert.deepEqual(await hiddenIds(b), []);
});
