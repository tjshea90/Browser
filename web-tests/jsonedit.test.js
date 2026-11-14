'use strict';
// RX3-Y1: the JSON edit language of scriptlets-main.js (json-edit, json-edit-fetch-response, json-edit-xhr-response, their trusted- forms, trusted-edit-inbound-object, trusted-prevent-dom-bypass),
// in real headless Chromium. The queries are the SHAPES of the real list lines (88 of them in the lists the app downloads, listed in research/YOUTUBE_ADS.md section 3); the YouTube ones are copied
// word for word. Run: cd web-tests && npm test
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

/** A page at https://site.example/ with the library installed and [calls] sent before any page script runs (what content.js does). /data.json answers with opts.json. */
async function open(calls, body, opts) {
  opts = opts || {};
  const context = await browser.newContext();
  await context.route('https://**/*', (r) => {
    const u = new URL(r.request().url());
    if (u.hostname === 'site.example' && u.pathname === '/') return r.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><title>t</title></head><body>${body || ''}</body></html>` });
    if (u.pathname === '/data.json' || u.pathname === '/get_watch') return r.fulfill({ contentType: 'application/json', body: opts.json || '{"ok":1}' });
    return r.fulfill({ contentType: 'text/html', body: '<!doctype html><title>other</title><body>other</body>' });
  });
  const page = await context.newPage();
  await page.addInitScript({ content: LIB });
  await page.addInitScript((payload) => { window.__n = []; document.addEventListener('__kst_sl_n_v1', (e) => window.__n.push(String(e.detail)), true); document.dispatchEvent(new CustomEvent('__kst_sl_v1', { detail: payload })); }, JSON.stringify(calls));
  await page.goto('https://site.example/');
  return page;
}
const settle = (ms) => new Promise((r) => setTimeout(r, ms || 300));

/** JSON.parse(doc) in a page where [rules] were sent first: the result as text (or "ERR ..."). */
async function parsed(rules, doc) {
  const page = await open(rules, '');
  const out = await page.evaluate((d) => { try { return JSON.stringify(JSON.parse(d)); } catch (e) { return 'ERR ' + e; } }, doc);
  await page.context().close();
  return out;
}
const edit = (q) => [['trusted-json-edit', q]];
const del = (q) => [['json-edit', q]];

// ---------------------------------------------------------------- the real mobile YouTube line

const M_YT = ['json-edit-fetch-response', '..itemSectionRenderer.contents.*[?.adSlotRenderer]', 'propsToMatch', '/get_watch'];
const FEED = JSON.stringify({ contents: { twoColumnWatchNextResults: { results: { results: { contents: [{ itemSectionRenderer: { contents: [{ adSlotRenderer: { x: 1 } }, { videoRenderer: { id: 'a' } }, { adSlotRenderer: { x: 2 } }, { videoRenderer: { id: 'b' } }, { adSlotRenderer: { x: 3 } }] } }] } } } } });

test('RX3-Y1: the m.youtube.com line (copied from the lists) cuts the ad slots out of the feed answer of a matching fetch, and leaves no holes', async () => {
  const page = await open([M_YT], '<script>fetch("/get_watch?v=1").then(r => r.json()).then(j => { window.__out = j; });</script>', { json: FEED });
  await page.waitForFunction(() => window.__out);
  const out = await page.evaluate(() => window.__out);
  const items = out.contents.twoColumnWatchNextResults.results.results.contents[0].itemSectionRenderer.contents;
  assert.deepEqual(items.map((i) => i.videoRenderer && i.videoRenderer.id), ['a', 'b'], 'two videos left, in order');
  assert.equal(JSON.stringify(out).indexOf('adSlotRenderer'), -1);
  assert.equal(JSON.stringify(out).indexOf('null'), -1, 'array items are cut out, not nulled');
});

test('RX3-Y1: a fetch whose address does not match the needle is left alone', async () => {
  const page = await open([M_YT], '<script>fetch("/data.json").then(r => r.text()).then(t => { window.__out = t; });</script>', { json: FEED });
  await page.waitForFunction(() => window.__out);
  assert.equal(await page.evaluate(() => window.__out), FEED);
});

test('RX3-Y1: an edit rule for fetch without any request needle is not accepted (it would parse every answer of the page)', async () => {
  const page = await open([['json-edit-fetch-response', '..adSlotRenderer']], '<script>fetch("/data.json").then(r => r.text()).then(t => { window.__out = t; });</script>', { json: FEED });
  await page.waitForFunction(() => window.__out);
  assert.equal(await page.evaluate(() => window.__out), FEED);
});

test('RX3-Y1: the xhr form edits responseText and response (json) of a matching request', async () => {
  const page = await open([['json-edit-xhr-response', '..itemSectionRenderer.contents.*[?.adSlotRenderer]', 'propsToMatch', '/get_watch']],
    '<script>var x = new XMLHttpRequest(); x.open("GET", "/get_watch?v=2"); x.onload = function () { window.__t = x.responseText; }; x.send();' +
    'var y = new XMLHttpRequest(); y.open("GET", "/get_watch?v=3"); y.responseType = "json"; y.onload = function () { window.__j = y.response; }; y.send();</script>', { json: FEED });
  await page.waitForFunction(() => window.__t && window.__j);
  assert.equal((await page.evaluate(() => window.__t)).indexOf('adSlotRenderer'), -1);
  assert.equal(JSON.stringify(await page.evaluate(() => window.__j)).indexOf('adSlotRenderer'), -1);
});

test('RX3-Y1: the second argument on its own is the request needle (player.pl shape), and an empty one is skipped (idlixku shape)', async () => {
  const a = await open([['trusted-json-edit-fetch-response', '.ok=false', '/data.json']], '<script>fetch("/data.json").then(r => r.json()).then(j => { window.__out = j; });</script>');
  await a.waitForFunction(() => window.__out);
  assert.deepEqual(await a.evaluate(() => window.__out), { ok: false });
  const b = await open([['trusted-json-edit-fetch-response', '.ok=0', '', 'propsToMatch', '/data.json']], '<script>fetch("/data.json").then(r => r.json()).then(j => { window.__out = j; });</script>');
  await b.waitForFunction(() => window.__out);
  assert.deepEqual(await b.evaluate(() => window.__out), { ok: 0 });
});

// ---------------------------------------------------------------- the path language

test('RX3-Y1: ..name reaches a key at any depth, inside arrays too', async () => {
  assert.equal(await parsed(del('..adSlotRenderer'), '{"a":{"adSlotRenderer":1,"b":2},"c":[{"adSlotRenderer":3,"d":4}]}'), '{"a":{"b":2},"c":[{"d":4}]}');
});

test('RX3-Y1: .name and .* and ./regex/ pick children', async () => {
  assert.equal(await parsed(del('.a.x'), '{"a":{"x":1,"y":2},"b":3}'), '{"a":{"y":2},"b":3}');
  assert.equal(await parsed(del('.a.*'), '{"a":{"x":1,"y":2},"b":3}'), '{"a":{},"b":3}');
  assert.equal(await parsed(del('.a./^ad_/'), '{"a":{"ad_one":1,"ad_two":2,"keep":3}}'), '{"a":{"keep":3}}');
});

test('RX3-Y1: predicates: has a property, equals, contains, starts with, ends with, matches, true / number', async () => {
  const items = '{"items":[{"type":"ad","n":1},{"type":"video","n":2},{"type":"ad2","n":3,"isAd":true},{"name":"nc-ad-x","n":4},{"name":"my.ad","n":5},{"name":"foo","n":6},{"n":7}]}';
  const ids = (out) => JSON.parse(out).items.map((i) => i.n).join(',');
  assert.equal(ids(await parsed(del('.items.*[?.type]'), items)), '4,5,6,7', 'has a property');
  assert.equal(ids(await parsed(del('.items.*[?.type=="ad"]'), items)), '2,3,4,5,6,7', 'equals');
  assert.equal(ids(await parsed(del('.items.*[?.type*="ad"]'), items)), '2,4,5,6,7', 'contains');
  assert.equal(ids(await parsed(del('.items.*[?.name^="nc-ad"]'), items)), '1,2,3,5,6,7', 'starts with');
  assert.equal(ids(await parsed(del('.items.*[?.name$=".ad"]'), items)), '1,2,3,4,6,7', 'ends with');
  assert.equal(ids(await parsed(del('.items.*[?.name=/^(foo|my\\.ad)$/]'), items)), '1,2,3,4,7', 'matches a regular expression');
  assert.equal(ids(await parsed(del('.items.*[?.isAd==true]'), items)), '1,2,4,5,6,7', 'true');
  assert.equal(ids(await parsed(del('.items.*[?.n==6]'), items)), '1,2,3,4,5,7', 'a number');
});

test('RX3-Y1: a test may look deep (..name) and across children (.*)', async () => {
  const doc = '{"nodes":[{"id":1,"a":{"b":{"resource":"nc-ad-1"}}},{"id":2,"a":{"b":{"resource":"story"}}},{"id":3,"x":{"__typename":"SponsoredData"}},{"id":4,"x":{"__typename":"Post"}}]}';
  const ids = (o) => JSON.parse(o).nodes.map((i) => i.id).join(',');
  assert.equal(ids(await parsed(del('.nodes.*[?..resource^="nc-ad"]'), doc)), '2,3,4');
  assert.equal(ids(await parsed(del('..nodes.*[?.*.__typename=="SponsoredData"]'), doc)), '1,2,4');
});

test('RX3-Y1: actions: delete (default), set, merge, repl, on trusted forms; the root can be tested and merged into', async () => {
  assert.equal(await parsed(edit('.a.b=false'), '{"a":{"b":true}}'), '{"a":{"b":false}}');
  assert.equal(await parsed(edit('..show_ads=false'), '{"x":{"show_ads":true},"show_ads":true}'), '{"x":{"show_ads":false},"show_ads":false}');
  assert.equal(await parsed(edit('.ads_mode="0"'), '{"ads_mode":"1"}'), '{"ads_mode":"0"}');
  assert.equal(await parsed(edit('.streams.*.adUnits=[]'), '{"streams":[{"adUnits":[1,2]},{"adUnits":[3]}]}'), '{"streams":[{"adUnits":[]},{"adUnits":[]}]}');
  assert.equal(await parsed(edit('.ads={"movie":false,"series":false}'), '{"ads":{"movie":true}}'), '{"ads":{"movie":false,"series":false}}');
  assert.equal(await parsed(edit('$+={"ads_suppressed":true}'), '{"k":1}'), '{"k":1,"ads_suppressed":true}');
  assert.equal(await parsed(edit('.params+={"x":1}'), '{"params":{"y":2}}'), '{"params":{"y":2,"x":1}}');
  assert.equal(await parsed(edit('$..adTimeout=0'), '{"a":{"adTimeout":9}}'), '{"a":{"adTimeout":0}}');
  assert.equal(await parsed(edit('[?.type=="ads"].visibility.status="hidden"'), '{"type":"ads","visibility":{"status":"shown"}}'), '{"type":"ads","visibility":{"status":"hidden"}}');
  assert.equal(await parsed(edit('[?.type=="ads"].visibility.status="hidden"'), '{"type":"post","visibility":{"status":"shown"}}'), '{"type":"post","visibility":{"status":"shown"}}', 'a root that fails the test is untouched');
  assert.equal(await parsed(edit('v2:$.param_stores.*./^interstitial_/.value.shouldShow=false'), '{"param_stores":{"interstitial_a":{"value":{"shouldShow":true}},"other":{"value":{"shouldShow":true}}}}'),
    '{"param_stores":{"interstitial_a":{"value":{"shouldShow":false}},"other":{"value":{"shouldShow":true}}}}', 'the v2:$ prefix and a regex key (nytimes shape)');
  assert.equal(await parsed(edit('.u=repl({"regex":"(?:#reloadxhr)?$","replacement":"#reloadxhr"})'), '{"u":"https://x/y"}'), '{"u":"https://x/y#reloadxhr"}');
  assert.equal(await parsed(edit('.u=repl({"regex":"/; (adunit|channel)\\\\b/","replacement":""})'), '{"u":"Mozilla; channel more"}'), '{"u":"Mozilla more"}', 'the /regex/ form (list shape)');
});

test('RX3-Y1: a list that is not trusted can only delete: json-edit with a set, merge or repl action changes nothing', async () => {
  assert.equal(await parsed(del('.a.b=false'), '{"a":{"b":true}}'), '{"a":{"b":true}}');
  assert.equal(await parsed(del('$+={"x":1}'), '{"k":1}'), '{"k":1}');
  assert.equal(await parsed(del('.u=repl({"regex":"a","replacement":"b"})'), '{"u":"aaa"}'), '{"u":"aaa"}');
});

test('RX3-Y1: matches gates json-edit on the text that was parsed (the facebook shape)', async () => {
  const rule = [['json-edit', '..directLink', 'matches', '/^.{60,}/']];
  assert.equal(await parsed(rule, '{"directLink":1,"k":2}'), '{"directLink":1,"k":2}', 'a short text is not our business');
  const long = '{"directLink":1,"k":2,"pad":"' + 'x'.repeat(80) + '"}';
  assert.equal(await parsed(rule, long), '{"k":2,"pad":"' + 'x'.repeat(80) + '"}');
});

test('RX3-Y1: JSON.parse keeps working with a reviver, with a non-string argument and with text that is not JSON', async () => {
  const page = await open(del('..ad'), '');
  const out = await page.evaluate(() => {
    const r1 = JSON.parse('{"ad":1,"n":2}', (k, v) => (k === 'n' ? v * 10 : v));
    const r2 = JSON.parse(5);
    let r3; try { JSON.parse('{oops'); r3 = 'no'; } catch (e) { r3 = e instanceof SyntaxError ? 'syntax' : 'other'; }
    return JSON.stringify([r1, r2, r3]);
  });
  assert.equal(out, JSON.stringify([{ n: 20 }, 5, 'syntax']));
});

test('RX3-Y1: queries that are not understood are ignored and never break the page', async () => {
  for (const q of ['..a[?', '..a=', '.a=notjson', '..', '', '[?.a', '.a[?.b==]', 'x.y']) {
    assert.equal(await parsed(edit(q), '{"a":{"b":1}}'), '{"a":{"b":1}}', 'query: ' + JSON.stringify(q));
  }
});

test('RX3-Y1: a huge document is bounded: no hang, no stack overflow', async () => {
  const page = await open(del('..nothing'), '');
  const ms = await page.evaluate(() => {
    const big = { a: new Array(300000).fill(0).map((v, i) => ({ i })) };
    let deep = {}; const root = deep; for (let i = 0; i < 4000; i++) { deep.x = {}; deep = deep.x; }
    const t = performance.now(); JSON.parse(JSON.stringify(big)); JSON.parse(JSON.stringify(root)); return performance.now() - t;
  });
  assert.ok(ms < 8000, 'took ' + ms + ' ms');
});

// ---------------------------------------------------------------- the two guards (real lines)

const INBOUND = [
  ['trusted-edit-inbound-object', 'JSON.stringify', '0', '[?.attestationRequest][?.context.client.userAgent*="inline"].playbackContext.contentPlaybackContext+={"isInlinePlaybackNoAd":true}'],
  ['trusted-edit-inbound-object', 'JSON.stringify', '0', '[?.attestationRequest][?.context.client.userAgent*="yahi"]+={"params":"YAHI"}'],
  ['trusted-edit-inbound-object', 'JSON.stringify', '0', '[?.attestationRequest][?.context.client.userAgent=/adunit|channel|inline|yahi|oaeb|eafg|yaf4/].playbackContext.contentPlaybackContext.referer=repl({"regex":"(?:#reloadxhr)?$","replacement":"#reloadxhr"})'],
  ['trusted-edit-inbound-object', 'JSON.stringify', '0', '[?.createCommentParams].context.client.userAgent=repl({"regex":"/; (adunit|channel|inline|yahi|oaeb|eafg|yaf4)\\\\b/","replacement":""})'],
];

test('RX3-Y1: trusted-edit-inbound-object (the real YouTube lines): the body the page builds is edited by its own marker, and only then', async () => {
  const page = await open(INBOUND, '');
  const r = await page.evaluate(() => {
    const body = (ua, extra) => Object.assign({ attestationRequest: { x: 1 }, context: { client: { userAgent: ua } }, playbackContext: { contentPlaybackContext: { referer: 'https://www.youtube.com/watch' } } }, extra || {});
    return [
      JSON.stringify(body('Mozilla/5.0 (X11; inline) Firefox')),        // the inline marker: no-ad playback flag, and the referer gets the reload mark (the marker is in the regex list too)
      JSON.stringify(body('Mozilla/5.0 (X11) Firefox')),                 // no marker: untouched
      JSON.stringify({ context: { client: { userAgent: 'Mozilla/5.0 (X11) Firefox' } } }),   // no attestation request: untouched
      JSON.stringify({ createCommentParams: 'p', context: { client: { userAgent: 'Mozilla/5.0; channel more' } } }),   // the comment request loses the marker
      JSON.stringify(body('Mozilla/5.0 (yahi)')),
      JSON.stringify('a string'), JSON.stringify(null), JSON.stringify([1, 2]),
    ];
  });
  const [inline, plain, noAtt, comment, yahi] = r;
  assert.ok(inline.indexOf('"isInlinePlaybackNoAd":true') > 0, inline);
  assert.ok(inline.indexOf('#reloadxhr') > 0, inline);
  assert.equal(plain.indexOf('isInlinePlaybackNoAd'), -1); assert.equal(plain.indexOf('#reloadxhr'), -1); assert.equal(plain.indexOf('YAHI'), -1);
  assert.equal(noAtt.indexOf('#reloadxhr'), -1);
  assert.ok(comment.indexOf('channel') === -1 && comment.indexOf('Mozilla/5.0 more') > 0, comment);
  assert.ok(yahi.indexOf('"params":"YAHI"') > 0, yahi);
  assert.deepEqual(r.slice(5), ['"a string"', 'null', '[1,2]'], 'JSON.stringify of anything else is unchanged');
});

test('RX3-Y1: trusted-edit-inbound-object with a path that is not a function, a bad index or a bad query does nothing and does not throw', async () => {
  const page = await open([['trusted-edit-inbound-object', 'JSON.nothing', '0', '..a=1'], ['trusted-edit-inbound-object', 'JSON.stringify', '99', '..a=1'], ['trusted-edit-inbound-object', 'JSON.stringify', '0', '..a=notjson']], '');
  assert.equal(await page.evaluate(() => JSON.stringify({ a: 5 })), '{"a":5}');
});

test('RX3-Y1: trusted-prevent-dom-bypass: a frame the page inserts gets OUR fetch and Request, not its own untouched ones', async () => {
  const page = await open([['trusted-prevent-dom-bypass', 'Node.prototype.appendChild', 'fetch'], ['trusted-prevent-dom-bypass', 'Node.prototype.appendChild', 'Request'], ['json-prune-fetch-response', 'x', '', 'propsToMatch', '/data.json']], '');
  const r = await page.evaluate(() => {
    const f = document.createElement('iframe');
    document.body.appendChild(f);
    return { fetch: f.contentWindow.fetch === window.fetch, request: f.contentWindow.Request === window.Request, ours: window.fetch.toString().indexOf('[native code]') < 0 };
  });
  assert.deepEqual(r, { fetch: true, request: true, ours: true });
});

test('RX3-Y1: without the guard a fresh frame has its own fetch (what the guard exists to stop)', async () => {
  const page = await open([['json-prune-fetch-response', 'x', '', 'propsToMatch', '/data.json']], '');
  const same = await page.evaluate(() => { const f = document.createElement('iframe'); document.body.appendChild(f); return f.contentWindow.fetch === window.fetch; });
  assert.equal(same, false);
});
