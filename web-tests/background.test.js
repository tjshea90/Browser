'use strict';
// The extension's background page (background.js + engine.js) driven in a fake browser: the app's side of the protocol (cfg / img / cnt / st / req) is played by the test,
// the list image is the Kotlin-written fixture. Covers what the engine tests cannot: the state machine around it (legacy path until an engine is loaded, per-page configuration,
// chunked transfer, the saved copy, fail-open behaviour, batched counters). Run: cd web-tests && npm test
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const SHIELDS = path.join(__dirname, '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const SRC = ['engine.js', 'redirects.js', 'background.js'].map((f) => fs.readFileSync(path.join(SHIELDS, f), 'utf8')).join('\n'); // the order of the manifest's background scripts
const IMAGE = fs.readFileSync(path.join(__dirname, 'fixtures', 'mini.kex'));
const ID = '1.111.1700000000000.1.2.3.4.5.6';
// objects built inside the vm context have another Object.prototype: compare them as JSON
const eq = (a, b, msg) => assert.equal(JSON.stringify(a), JSON.stringify(b), msg);
const tick = (n) => new Promise((r) => setTimeout(r, n || 5));
async function until(f, what) { for (let i = 0; i < 2000; i++) { if (f()) return; await tick(5); } assert.fail('timed out waiting for ' + what); }

/** The app's side of the image transfer: Kotlin's ExtImage.chunkReply format with a small chunk size so the image needs several messages. */
function chunkReply(gen, i, size, id) {
  const from = i * size;
  if (from >= IMAGE.length) return '';
  const b64 = IMAGE.subarray(from, Math.min(IMAGE.length, from + size)).toString('base64');
  return i === 0 ? `${gen}:${IMAGE.length}:${size}:7:${id}:${b64}` : b64;
}

function fakeIdb(store) {
  const done = (tx) => setImmediate(() => tx.oncomplete && tx.oncomplete());
  return {
    open() {
      const req = {};
      setImmediate(() => {
        req.result = {
          createObjectStore() {}, close() {},
          transaction() {
            const tx = {};
            const op = (f) => { const r = {}; setImmediate(() => { r.result = f(); done(tx); }); return r; };
            tx.objectStore = () => ({ get: (k) => op(() => store.get(k)), put: (v, k) => op(() => { store.set(k, v); return k; }), delete: (k) => op(() => { store.delete(k); }) });
            return tx;
          },
        };
        if (req.onupgradeneeded) req.onupgradeneeded();
        if (req.onsuccess) req.onsuccess();
      });
      return req;
    },
  };
}

/** @param app (message) => reply string (may be async). @param opts { idb: Map } */
function harness(app, opts) {
  opts = opts || {};
  const sent = [], raws = [], timers = [], tabMsgs = [], listeners = {}, clock = { now: 1700000000000 };
  const ctx = {
    console, Promise, Map, Set, Uint8Array, ArrayBuffer, Int32Array, Math, Date: { now: () => clock.now }, JSON, parseInt, Object, Error, RegExp, String, Array,
    performance: { now: () => Number(process.hrtime.bigint()) / 1e6 },
    atob: (s) => Buffer.from(s, 'base64').toString('latin1'),
    // short timers (the chunk-transfer yields) run at once; the 500 ms / 5 s batching timers wait until the test runs them
    setTimeout: (f, ms) => { if (!ms || ms < 100) { setImmediate(f); return -1; } timers.push(f); return timers.length; },
    clearTimeout: (id) => { if (id > 0 && timers[id - 1]) timers[id - 1] = () => {}; },
    indexedDB: fakeIdb(opts.idb || new Map()),
    browser: {
      runtime: {
        getURL: (p) => 'moz-extension://test-id/' + p,
        sendNativeMessage: (app_, text) => { const m = JSON.parse(text); raws.push(text); sent.push(m); return Promise.resolve(app(m)); },
        onMessage: { addListener: (f) => { (listeners.message = listeners.message || []).push(f); } },
      },
      contentScripts: opts.scripts ? {
        register: (o) => {
          if (opts.scripts.fail && opts.scripts.fail(o)) return Promise.reject(new Error('invalid match pattern'));
          const r = { opts: o, gone: false, unregister() { r.gone = true; return Promise.resolve(); } };
          opts.scripts.list.push(r);
          return Promise.resolve(r);
        },
      } : undefined,
      tabs: { sendMessage: (tabId, m, o) => { tabMsgs.push({ tabId, m, o }); return Promise.resolve(); } }, // what the background page tells a tab's frames (RX3-G3)
      webRequest: {
        onBeforeRequest: { addListener: (f) => { listeners.before = f; } },
        onBeforeSendHeaders: { addListener: (f) => { listeners.sendHeaders = f; } },
        onHeadersReceived: { addListener: (f) => { listeners.headers = f; } },
        onErrorOccurred: { addListener: (f) => { listeners.error = f; } },
        onCompleted: { addListener: (f) => { listeners.completed = f; } },
      },
    },
  };
  vm.createContext(ctx);
  vm.runInContext(SRC, ctx);
  const req = (type, url, page) => listeners.before({ type, url, tabId: 1, frameId: 0, documentUrl: page, frameAncestors: [{ url: page, frameId: 0 }] });
  return {
    sent, raws, ctx, listeners, clock, tabMsgs,
    mainFrame: (url, extra) => listeners.before(Object.assign({ type: 'main_frame', url, tabId: 1, frameId: 0 }, extra || {})),
    /** a message from a content script (the first listener that answers wins, like the browser) */
    message: (m, tabId, senderUrl, frameId) => { for (const f of listeners.message || []) { const r = f(m, { tab: tabId === undefined ? { id: 1 } : (tabId < 0 ? undefined : { id: tabId }), url: senderUrl, frameId: frameId === undefined ? 0 : frameId }); if (r !== undefined) return r; } return undefined; },
    req,
    flushTimers: async () => { const t = timers.splice(0); t.forEach((f) => f()); await tick(); },
    of: (k) => sent.filter((m) => m.k === k),
  };
}

const PAGE = 'https://example.com/page';
const AD = 'https://ads.example.com/x.js'; // ||ads.example.com^ in the fixture list
const PLAIN = 'https://nothing.test/index.js';
const isPromise = (x) => x && typeof x.then === 'function';

function standardApp(extra) {
  extra = extra || {};
  return (m) => {
    if (m.k === 'cfg') return extra.cfg || `1:1:${ID}`;
    if (m.k === 'img') return extra.img ? extra.img(m) : chunkReply(m.g, m.i, 2048, ID);
    if (m.k === 'req') return extra.req ? extra.req(m) : (m.u === AD ? '1' : '0');
    return '';
  };
}

test('until the engine is loaded every request is asked of the app one by one; afterwards decisions are synchronous and need no message', async () => {
  let release;
  const gate = new Promise((r) => { release = r; });
  const h = harness((m) => (m.k === 'img' ? gate.then(() => chunkReply(m.g, m.i, 2048, ID)) : standardApp()(m)));
  h.mainFrame(PAGE);
  const first = h.req('script', AD, PAGE);
  assert.ok(isPromise(first), 'no engine yet: the legacy path answers asynchronously');
  eq(await first, { cancel: true });
  eq(await h.req('script', PLAIN, PAGE), {});
  assert.ok(h.of('req').length >= 2, 'the app was asked request by request');
  release();
  await until(() => h.of('img').length >= 3, 'the image transfer');
  await until(() => { const r = h.req('script', AD, PAGE); return !isPromise(r); }, 'the engine to take over');
  const before = h.of('req').length;
  eq(h.req('script', AD, PAGE), { cancel: true });
  eq(h.req('script', PLAIN, PAGE), {});
  assert.equal(h.of('req').length, before, 'no per-request message once the engine is in');
  assert.ok(h.of('img').length > 1, 'the image came in several chunks');
});

test('blocked requests are counted per category and sent in one batched message', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  await until(() => h.of('img').length >= 3, 'the image transfer');
  await until(() => !isPromise(h.req('script', PLAIN, PAGE)), 'the engine');
  for (let i = 0; i < 5; i++) eq(h.req('script', AD, PAGE), { cancel: true });
  assert.equal(h.of('cnt').length, 0, 'nothing is sent per blocked request');
  await h.flushTimers();
  const c = h.of('cnt');
  assert.equal(c.length, 1);
  assert.equal(c[0].p, PAGE);
  eq(c[0].c, [5, 0, 0, 0]);
});

test('a page the app says is off (Shields down / site allowed) is never blocked and never judged', async () => {
  const h = harness(standardApp({ cfg: `0:1:${ID}` }));
  h.mainFrame(PAGE);
  await until(() => h.of('cfg').length >= 1, 'cfg');
  await tick(20);
  eq(await h.req('script', AD, PAGE), {});
  assert.equal(h.of('req').length, 0);
});

test('compatibility mode (cfg answers L) keeps asking the app for every request and never transfers the image', async () => {
  const h = harness(standardApp({ cfg: `L:1:${ID}`, req: (m) => (m.u === AD ? '1' : '0') }));
  h.mainFrame(PAGE);
  await tick(30);
  eq(await h.req('script', AD, PAGE), { cancel: true });
  eq(await h.req('script', PLAIN, PAGE), {});
  assert.equal(h.of('img').length, 0);
  assert.ok(h.of('req').length >= 2);
});

test('a new navigation asks again: switching Shields off for the site takes effect at once', async () => {
  let mode = '1';
  const h = harness((m) => (m.k === 'cfg' ? `${mode}:1:${ID}` : standardApp()(m)));
  h.mainFrame(PAGE);
  await until(() => h.of('img').length >= 3, 'the image transfer');
  await until(() => !isPromise(h.req('script', PLAIN, PAGE)), 'the engine');
  eq(h.req('script', AD, PAGE), { cancel: true });
  mode = '0';
  h.mainFrame(PAGE); // reload
  eq(await h.req('script', AD, PAGE), {}, 'the first sub-resource of the new load waits for the fresh answer');
  assert.equal(h.of('cfg').length, 2);
});

test('an answer older than a minute is refreshed in the background: a Settings switch reaches open pages without a reload', async () => {
  let mode = '1';
  const h = harness((m) => (m.k === 'cfg' ? `${mode}:1:${ID}` : standardApp()(m)));
  h.mainFrame(PAGE);
  await until(() => h.of('img').length >= 3, 'the image transfer');
  await until(() => !isPromise(h.req('script', PLAIN, PAGE)), 'the engine');
  eq(h.req('script', AD, PAGE), { cancel: true });
  mode = '0';
  eq(h.req('script', AD, PAGE), { cancel: true }, 'inside the minute the old answer stands');
  assert.equal(h.of('cfg').length, 1);
  h.clock.now += 61000;
  eq(h.req('script', AD, PAGE), { cancel: true }, 'the request that finds the answer old still uses it (no waiting)...');
  await until(() => h.of('cfg').length === 2, 'the refresh');
  await tick(10);
  eq(h.req('script', AD, PAGE), {}, '...and the next one follows the new switch');
});

test('while the app\'s lists are still loading (generation 0) the saved copy already judges requests, and the answer is asked again within seconds', async () => {
  const idb = new Map([['current', { id: ID, buf: IMAGE.buffer.slice(IMAGE.byteOffset, IMAGE.byteOffset + IMAGE.byteLength) }]]);
  let cfg = '1:0:';
  const h = harness((m) => (m.k === 'cfg' ? cfg : standardApp()(m)), { idb });
  await tick(30);
  h.mainFrame(PAGE);
  await until(() => h.of('cfg').length >= 1, 'cfg');
  await tick(20);
  eq(h.req('script', AD, PAGE), { cancel: true }, 'blocked at once, from the saved copy');
  assert.equal(h.of('img').length, 0, 'no transfer for generation 0');
  cfg = `0:1:${ID}`; // the lists finished loading and say this site is allowed
  h.clock.now += 3500;
  eq(h.req('script', AD, PAGE), { cancel: true }, 'the request that finds the provisional answer old still uses it');
  await until(() => h.of('cfg').length === 2, 'the second question');
  await tick(10);
  eq(h.req('script', AD, PAGE), {}, 'and the final answer (allowed) takes over');
});

test('the saved copy gives an engine before the first request, and an unchanged list costs no transfer', async () => {
  const idb = new Map([['current', { id: ID, buf: IMAGE.buffer.slice(IMAGE.byteOffset, IMAGE.byteOffset + IMAGE.byteLength) }]]);
  const h = harness(standardApp(), { idb });
  await tick(30); // the saved copy loads at start-up
  h.mainFrame(PAGE);
  await until(() => h.of('cfg').length >= 1, 'cfg');
  await tick(20);
  eq(h.req('script', AD, PAGE), { cancel: true }, 'synchronous: no promise');
  assert.equal(h.of('img').length, 0, 'the lists have not changed, so nothing is transferred');
});

test('a changed list replaces the saved copy; the new image is kept for the next launch', async () => {
  const idb = new Map([['current', { id: 'old-id', buf: IMAGE.buffer.slice(IMAGE.byteOffset, IMAGE.byteOffset + IMAGE.byteLength) }]]);
  const h = harness(standardApp(), { idb });
  await tick(30);
  h.mainFrame(PAGE);
  await until(() => h.of('img').length >= 3, 'the transfer of the newer lists');
  await until(() => idb.get('current') && idb.get('current').id === ID, 'the new copy to be saved');
});

test('a damaged saved copy is dropped; the engine then comes from the app', async () => {
  const bad = IMAGE.buffer.slice(IMAGE.byteOffset, IMAGE.byteOffset + IMAGE.byteLength);
  new Uint8Array(bad)[200] ^= 0xFF;
  const idb = new Map([['current', { id: ID, buf: bad }]]);
  const h = harness(standardApp(), { idb });
  await tick(30);
  assert.equal(idb.has('current'), false, 'the damaged copy was deleted');
  h.mainFrame(PAGE);
  await until(() => h.of('img').length >= 3, 'the transfer');
  await until(() => !isPromise(h.req('script', PLAIN, PAGE)), 'the engine');
  eq(h.req('script', AD, PAGE), { cancel: true });
});

test('an app that cannot answer never breaks a page (fail open), and its non-answer is not remembered', async () => {
  let answer = 'garbage';
  const h = harness((m) => (m.k === 'cfg' ? answer : standardApp()(m)));
  h.mainFrame(PAGE);
  eq(await h.req('script', AD, PAGE), {});
  answer = `1:1:${ID}`;
  h.mainFrame(PAGE);
  await until(() => h.of('img').length >= 3, 'the transfer once the app answers properly');
});

test('a failing transfer leaves the per-request path in charge and backs off instead of looping', async () => {
  const h = harness(standardApp({ img: () => '' }));
  h.mainFrame(PAGE);
  await tick(50);
  assert.equal(h.of('img').length, 1, 'one attempt, then a pause');
  eq(await h.req('script', AD, PAGE), { cancel: true }, 'still blocked, by the app');
  assert.ok(h.of('req').length >= 1);
});

test('documents and unknown pages are left alone', async () => {
  const h = harness(standardApp());
  eq(h.mainFrame(PAGE), {});
  eq(h.listeners.before({ type: 'script', url: AD, tabId: -1 }), {}, 'no page to judge against');
});

// ---------------------------------------------------------------- the pop-up and forced-redirect guard (background half)
const GUARD_CFG = `1:1:${ID}:1`;
const OTHER = 'https://ad-landing.example/promo';
const guardApp = (flags) => (m) => (m.k === 'cfg' ? `1:1:${ID}:${flags === undefined ? 1 : flags}` : standardApp()(m));
async function onPage(app) {
  const h = harness(app || guardApp());
  h.mainFrame(PAGE);
  await until(() => h.of('cfg').length >= 1, 'cfg');
  await tick(10);
  return h;
}
const nav = (h, url, extra) => h.mainFrame(url, Object.assign({ method: 'GET', originUrl: PAGE, requestId: 'r' + Math.random() }, extra || {}));

test('guard: every real tap is passed on to the app (the app judges a jump that follows it) and the extension itself never cancels a navigation', async () => {
  const h = await onPage();
  h.message({ k: 'pop', d: 'tap|', p: PAGE });
  h.message({ k: 'pop', d: 'tap|' + OTHER, p: PAGE });
  await tick(10);
  const taps = h.of('pop').filter((m) => m.d.startsWith('tap|'));
  assert.deepEqual(taps.map((m) => m.d), ['tap|', 'tap|' + OTHER]);
  assert.ok(taps.every((m) => m.p === PAGE));
  eq(nav(h, OTHER), {}, 'the decision is the app\'s, made before the navigation starts');
  assert.equal(h.of('pop').filter((m) => !m.d.startsWith('tap|')).length, 0, 'a tap is not a blocked pop-up');
});

test('guard: the page-side script asks whether it applies and what it blocked is forwarded to the app', async () => {
  const h = await onPage();
  const answer = await h.message({ k: 'guard', h: 'movie.example' });
  assert.equal(answer.on, true);
  assert.equal(typeof answer.top, 'string', 'the answer names the TOP page\'s host (an embedded player must judge "same site" against it)');
  const off = await onPage(guardApp(0));
  assert.equal((await off.message({ k: 'guard', h: 'movie.example' })).on, false);
  h.message({ k: 'pop', d: 'open|' + OTHER, p: 'https://frame.example/embed' });
  await tick(10);
  const pop = h.of('pop');
  assert.equal(pop.length, 1);
  assert.equal(pop[0].d, 'open|' + OTHER);
  assert.equal(pop[0].p, PAGE, 'attributed to the tab\'s page, not to the embedded player frame');
});

// ---------------------------------------------------------------- element-hiding CSS: cache lifetime (AA4 / PERF_JS B1, J5.1)
const cssApp = (gen, extra) => {
  extra = extra || {};
  return (m) => {
    if (m.k === 'cfg') return `1:${gen.v}:${gen.v > 0 ? ID : ''}:1`;
    if (m.k === 'css') return extra.css ? extra.css(m) : (gen.v > 0 ? '.ad{display:none!important}' : '');
    if (m.k === 'img') return chunkReply(m.g, m.i, 2048, ID);
    return '';
  };
};

test('css: an answer is cached for a minute, then asked again; frames of one host that start together share one question', async () => {
  const gen = { v: 1 };
  const h = harness(cssApp(gen));
  h.mainFrame(PAGE); await until(() => h.of('cfg').length > 0, 'cfg');
  const [a, b] = await Promise.all([h.message({ k: 'css', h: 'ads.example', top: false }), h.message({ k: 'css', h: 'ads.example', top: false })]);
  assert.equal(a, '.ad{display:none!important}'); assert.equal(b, a);
  assert.equal(h.of('css').length, 1, 'two frames started together: one native question');
  await h.message({ k: 'css', h: 'ads.example', top: false });
  assert.equal(h.of('css').length, 1, 'a repeat inside the minute is served from the cache');
  h.clock.now += 61000;
  await h.message({ k: 'css', h: 'ads.example', top: false });
  assert.equal(h.of('css').length, 2, 'older than a minute: asked again (a Settings switch reaches open pages)');
});

test('css: while the app\'s lists are still loading nothing is remembered, and a new list generation empties the cache', async () => {
  const gen = { v: 0 };
  const h = harness(cssApp(gen));
  h.mainFrame(PAGE); await until(() => h.of('cfg').length >= 1, 'cfg');
  assert.equal(await h.message({ k: 'css', h: 'news.example', top: true }), '', 'the lists are not loaded: the app answers nothing yet');
  assert.equal(await h.message({ k: 'css', h: 'news.example', top: true }), '');
  assert.equal(h.of('css').length, 2, 'the empty answer was not remembered');
  gen.v = 1; // the lists finished loading; the extension learns it with the next page configuration
  h.mainFrame('https://news.example/', { tabId: 2 }); await until(() => h.of('cfg').length >= 2, 'second cfg'); await tick(20);
  assert.equal(await h.message({ k: 'css', h: 'news.example', top: true }), '.ad{display:none!important}');
  await h.message({ k: 'css', h: 'news.example', top: true });
  assert.equal(h.of('css').length, 3, 'now remembered');
  gen.v = 2; // a refresh of the lists: a new generation arrives with the next page configuration
  h.mainFrame('https://other.example/', { tabId: 3 }); await until(() => h.of('cfg').length >= 3, 'third cfg'); await tick(20);
  await h.message({ k: 'css', h: 'news.example', top: true });
  assert.equal(h.of('css').length, 4, 'the cache was emptied by the new generation');
});

test('css: a navigation to a page asks for its top-document CSS again, so a Shields switch changed on the page takes effect on the reload', async () => {
  const gen = { v: 1 }; let on = true;
  const h = harness(cssApp(gen, { css: () => (on ? '.ad{display:none!important}' : '') }));
  h.mainFrame(PAGE); await until(() => h.of('cfg').length > 0, 'cfg');
  assert.equal(await h.message({ k: 'css', h: 'example.com', top: true }), '.ad{display:none!important}');
  on = false; // the owner turned element hiding off for this site and the page reloads
  h.mainFrame(PAGE);
  assert.equal(await h.message({ k: 'css', h: 'example.com', top: true }), '', 'the stale positive answer is gone');
  assert.equal(h.of('css').length, 2);
});

test('every native message starts with its kind: the app picks the worker thread from the first bytes (core BridgeLane, AA3-3b)', async () => {
  const gen = { v: 1 };
  const h = harness(cssApp(gen));
  h.mainFrame(PAGE); await until(() => h.of('img').length >= 3, 'the image transfer');
  await h.message({ k: 'css', h: 'example.com', top: true });
  await h.message({ k: 'gen', h: 'example.com', t: ['banner', 'ad-box'] });
  const kinds = new Set(h.sent.map((m) => m.k));
  for (const k of ['cfg', 'img', 'css', 'gen']) assert.ok(kinds.has(k), 'this test produced a ' + k + ' message');
  for (const text of h.raws) assert.ok(text.startsWith('{"k":"' + JSON.parse(text).k + '"'), 'the kind comes first: ' + text.slice(0, 40));
});

// ---------------------------------------------------------------- the media sniffer (AA4 / PERF_JS J3, J2a)
const hdr = (url, ct, len, extra) => ({ statusCode: 200, url, tabId: 1, documentUrl: PAGE, responseHeaders: [{ name: 'Server', value: 'x' }, { name: 'Content-Type', value: ct }, { name: 'Content-Length', value: String(len) }, { name: 'Cache-Control', value: 'no-store' }], ...(extra || {}) });

test('sniffer: a plain file under 100 KB is not worth a message, a small playlist is', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  h.listeners.headers(hdr('https://cdn.example/click.mp4', 'video/mp4', 5000));
  h.listeners.headers(hdr('https://cdn.example/master.m3u8', 'application/vnd.apple.mpegurl', 300));
  h.listeners.headers(hdr('https://cdn.example/manifest.mpd', 'application/dash+xml', 800));
  h.listeners.headers(hdr('https://cdn.example/movie.mp4', 'video/mp4', 5_000_000));
  h.listeners.headers(hdr('https://cdn.example/unknown.mp4', 'video/mp4', -1, { responseHeaders: [{ name: 'Content-Type', value: 'video/mp4' }] }));
  await tick(10);
  assert.deepEqual(h.of('media').map((m) => m.u), ['https://cdn.example/master.m3u8', 'https://cdn.example/manifest.mpd', 'https://cdn.example/movie.mp4', 'https://cdn.example/unknown.mp4']);
});

test('sniffer: pieces of one file fetched by byte range in the query are one candidate, a different file is another', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  for (let i = 0; i < 30; i++) h.listeners.headers(hdr(`https://cdn.example/v/clip.mp4?id=7&bytestart=${i * 1000000}&byteend=${i * 1000000 + 999999}`, 'video/mp4', 1_000_000));
  for (let i = 0; i < 10; i++) h.listeners.headers(hdr(`https://cdn.example/v/clip.mp4?id=7&range=${i}-${i + 1}&rn=${i}`, 'video/mp4', 1_000_000));
  h.listeners.headers(hdr('https://cdn.example/v/clip.mp4?id=8&bytestart=0&byteend=999999', 'video/mp4', 1_000_000)); // another id: another file
  h.listeners.headers(hdr('https://cdn.example/v/other.mp4?bytestart=5', 'video/mp4', 1_000_000));
  await tick(10);
  const m = h.of('media');
  assert.equal(m.length, 3, 'id=7 once, id=8 once, other.mp4 once');
  assert.ok(m[0].u.includes('bytestart=0&byteend=999999') && m[0].u.includes('id=7'), 'the first sighting is the one reported');
});

test('sniffer: headers that matter are still read when they come after many that do not', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  const many = []; for (let i = 0; i < 30; i++) many.push({ name: 'X-Header-Number-' + i, value: 'v' });
  h.listeners.headers({ statusCode: 206, url: 'https://cdn.example/part.mp4', tabId: 1, documentUrl: PAGE, responseHeaders: many.concat([{ name: 'content-type', value: 'video/mp4' }, { name: 'Content-Range', value: 'bytes 0-99/7000000' }, { name: 'Content-Length', value: '100' }]) });
  await tick(10);
  const m = h.of('media');
  assert.equal(m.length, 1); assert.equal(m[0].l, 7000000, 'the total from Content-Range, not the 100 bytes of this piece'); assert.equal(m[0].c, 'video/mp4');
});

// ---------------------------------------------------------------- the player's own headers and playlists in disguise (AK4, owner 2026-11-13: a movie on an embedded "vidsrc" server could not be downloaded)
const sent = (id, ref, org) => ({ requestId: id, tabId: 1, requestHeaders: [{ name: 'User-Agent', value: 'x' }].concat(ref ? [{ name: 'Referer', value: ref }] : [], org ? [{ name: 'Origin', value: org }] : []) });
const xhr = (id, url, ct, len, extra) => Object.assign({ requestId: id, type: 'xmlhttprequest', statusCode: 200, url, tabId: 1, documentUrl: 'https://player.test/embed/7', responseHeaders: [{ name: 'Content-Type', value: ct }].concat(len >= 0 ? [{ name: 'Content-Length', value: String(len) }] : []) }, extra || {});

test('sniffer: the Referer and Origin the browser sent travel with the candidate (an embedded player\'s server wants the player\'s, not the web page\'s)', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  h.listeners.sendHeaders(sent('r1', 'https://player.test/', 'https://player.test'));
  h.listeners.headers(xhr('r1', 'https://cdn.example/pl/master.m3u8', 'application/vnd.apple.mpegurl', 300));
  h.listeners.headers(xhr('r2', 'https://cdn.example/pl/other.m3u8', 'application/vnd.apple.mpegurl', 300)); // no request headers seen for this one
  await tick(10);
  const m = h.of('media');
  assert.equal(m.length, 2);
  assert.equal(m[0].rf, 'https://player.test/'); assert.equal(m[0].og, 'https://player.test');
  assert.equal(m[1].rf, ''); assert.equal(m[1].og, '');
  assert.equal(m[0].f, 'https://player.test/embed/7', 'the document that asked is still named');
});

test('sniffer: a script-fetched response that says nothing about being media is reported as a "maybe" so the app can read its first bytes', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  h.listeners.sendHeaders(sent('p1', 'https://player.test/', 'https://player.test'));
  h.listeners.headers(xhr('p1', 'https://cdn.example/hls/abc/playlist.txt', 'text/plain', 900));          // a playlist in disguise
  h.listeners.headers(xhr('p2', 'https://cdn.example/hls/abc/index', 'application/octet-stream', -1));   // no name, no size
  h.listeners.headers(xhr('p3', 'https://cdn.example/hls/abc/pl.css', 'text/css', 1200));                 // "a stylesheet" that a script fetched
  h.listeners.headers(xhr('p4', 'https://cdn.example/hls/abc/img.png', 'image/png', 700));                // "a picture" that a script fetched
  await tick(10);
  const m = h.of('maybe');
  assert.deepEqual(m.map((x) => x.u), ['https://cdn.example/hls/abc/playlist.txt', 'https://cdn.example/hls/abc/index', 'https://cdn.example/hls/abc/pl.css', 'https://cdn.example/hls/abc/img.png']);
  assert.equal(m[0].rf, 'https://player.test/'); assert.equal(m[0].og, 'https://player.test'); assert.equal(m[0].ty, 'xmlhttprequest'); assert.equal(m[0].l, 900);
  assert.equal(h.of('media').length, 0, 'nothing is claimed to be media before the bytes are read');
});

test('sniffer: a "maybe" is rare: not for pages, data, scripts, fonts, tiny or huge replies, <img>, and at most twelve per page', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  h.listeners.headers(xhr('a', 'https://x.test/api/list', 'application/json', 900));
  h.listeners.headers(xhr('b', 'https://x.test/page', 'text/html; charset=utf-8', 5000));
  h.listeners.headers(xhr('c', 'https://x.test/app.js', 'text/javascript', 5000));
  h.listeners.headers(xhr('d', 'https://x.test/f.woff2', 'font/woff2', 5000));
  h.listeners.headers(xhr('e', 'https://x.test/beacon', 'text/plain', 2));                                // two bytes: not a playlist
  h.listeners.headers(xhr('f', 'https://x.test/big.png', 'image/png', 5_000_000));                        // a real picture, far too big for a playlist
  h.listeners.headers(xhr('g', 'https://x.test/shot.png', 'image/png', 900, { type: 'image' }));          // an <img>: its own request type, never probed
  h.listeners.headers(xhr('h', 'https://x.test/pixel.gif', 'image/gif', 900));                            // .gif is never a playlist
  h.listeners.headers(xhr('i', 'https://x.test/err', 'text/plain', 900, { statusCode: 404 }));
  await tick(10);
  assert.equal(h.of('maybe').length, 0, JSON.stringify(h.of('maybe').map((x) => x.u)));
  for (let i = 0; i < 30; i++) h.listeners.headers(xhr('z' + i, 'https://cdn.example/pl/' + i, 'application/octet-stream', 800));
  h.listeners.headers(xhr('dup', 'https://cdn.example/pl/3', 'application/octet-stream', 800));
  await tick(10);
  assert.equal(h.of('maybe').length, 12, 'twelve per page');
  h.mainFrame('https://example.com/next'); // a new page: the budget starts again
  h.listeners.headers(xhr('n1', 'https://cdn.example/other/1', 'application/octet-stream', 800));
  await tick(10);
  assert.equal(h.of('maybe').length, 13);
});

test('scroll: the top frame\'s scroll place is handed to the app with the page address; a frame, a negative or odd number and a message from nowhere are not', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  h.message({ k: 'scroll', y: 1366, p: PAGE + '?x=1' }, 1, PAGE, 0);
  h.message({ k: 'scroll', y: 700, p: PAGE }, 1, PAGE, 7);                  // an embedded frame
  h.message({ k: 'scroll', y: -50, p: PAGE }, 1, PAGE, 0);                  // clamped to the top
  h.message({ k: 'scroll', y: 'high', p: PAGE }, 1, PAGE, 0);
  h.message({ k: 'scroll', y: Infinity, p: PAGE }, 1, PAGE, 0);
  h.message({ k: 'scroll', y: 300, p: PAGE }, -1, PAGE, 0);                 // not from a tab
  await tick(10);
  const s = h.of('scroll');
  assert.deepEqual(s.map((m) => m.y), [1366, 0]);
  assert.equal(s[0].p, PAGE + '?x=1');
});

test('sniffer: the probe budget of a page that stays open is topped up four every twenty seconds (a single-page site swaps its content with no new page request), never above twelve', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  const probe = (i) => h.listeners.headers(xhr('s' + i, 'https://cdn.example/spa/' + i, 'application/octet-stream', 800));
  for (let i = 0; i < 20; i++) probe(i);
  await tick(10);
  assert.equal(h.of('maybe').length, 12, 'twelve at once');
  h.clock.now += 19_000;
  for (let i = 20; i < 25; i++) probe(i);
  await tick(10);
  assert.equal(h.of('maybe').length, 12, 'nineteen seconds later: nothing yet');
  h.clock.now += 2_000;
  for (let i = 25; i < 40; i++) probe(i);
  await tick(10);
  assert.equal(h.of('maybe').length, 16, 'after twenty seconds four more are allowed');
  h.clock.now += 10 * 60_000;
  for (let i = 40; i < 80; i++) probe(i);
  await tick(10);
  assert.equal(h.of('maybe').length, 28, 'ten quiet minutes give back a full twelve, not more');
});

test('sniffer: what is plainly media is still reported at once and is never also a "maybe"', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  h.listeners.headers(xhr('m1', 'https://cdn.example/a.m3u8', 'text/plain', 800));              // by name
  h.listeners.headers(xhr('m2', 'https://cdn.example/movie', 'video/mp4', 9_000_000));          // by type
  await tick(10);
  assert.deepEqual(h.of('media').map((x) => x.u), ['https://cdn.example/a.m3u8', 'https://cdn.example/movie']);
  assert.equal(h.of('maybe').length, 0);
});

// ---------------------------------------------------------------- AL1 / AL5: the page being viewed, and a server switch (owner 2026-11-13 message 26)
const PLAYER1 = 'https://player-one.test/embed/7', PLAYER2 = 'https://player-two.test/e/9';
const inFrame = (frameId, parentFrameId, doc, url, ct, len, id) => Object.assign(hdr(url, ct, len), { requestId: id === undefined ? undefined : String(id), frameId, parentFrameId, documentUrl: doc, type: 'xmlhttprequest' });
const navigate = (h, frameId, parentFrameId, url) => h.listeners.before({ type: 'sub_frame', url, tabId: 1, frameId, parentFrameId, documentUrl: PAGE, frameAncestors: [{ url: PAGE, frameId: 0 }] });

test('page: an answer to a request that began before the tab went to another page is not reported (a player still fetching when the owner left)', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '100' });
  h.listeners.headers(inFrame(0, -1, PAGE, 'https://cdn.example/new.m3u8', 'application/vnd.apple.mpegurl', 300, 101)); // started after: this page's
  h.listeners.headers(inFrame(0, -1, PAGE, 'https://cdn.example/late.m3u8', 'application/vnd.apple.mpegurl', 300, 99));  // started before: the page that was left
  await tick(10);
  assert.deepEqual(h.of('media').map((m) => m.u), ['https://cdn.example/new.m3u8']);
  h.mainFrame('https://other.example/', { requestId: '200' });
  h.listeners.headers(inFrame(0, -1, PAGE, 'https://cdn.example/again.m3u8', 'application/vnd.apple.mpegurl', 300, 150)); // before the second navigation
  await tick(10);
  assert.equal(h.of('media').length, 1, 'nothing from the first page after the second navigation began');
});

test('server switch: the player frame going to another document says which documents are gone, and its files are reported again', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '10' });
  h.listeners.headers(inFrame(7, 0, PLAYER1, 'https://cdn.example/server1/master.m3u8', 'application/vnd.apple.mpegurl', 300, 11));
  await tick(10);
  assert.equal(h.of('media').length, 1); assert.equal(h.of('gone').length, 0);
  navigate(h, 7, 0, PLAYER2);                                    // the site set the iframe's address: another server
  await tick(10);
  const g = h.of('gone');
  assert.equal(g.length, 1); eq(g[0].d, [PLAYER1]); assert.equal(g[0].p, PAGE);
  h.listeners.headers(inFrame(7, 0, PLAYER2, 'https://cdn.example/server1/master.m3u8', 'application/vnd.apple.mpegurl', 300, 12)); // even the SAME file (server one again) is news
  await tick(10);
  assert.equal(h.of('media').length, 2);
});

test('server switch: a player frame that went to another server gets a fresh budget of disguised-file probes (three servers in a row must not use the first ones up)', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '10' });
  const probe = (frame, doc, i, id) => h.listeners.headers(inFrame(frame, 0, doc, 'https://cdn.example/' + doc.slice(-3) + '/pl/' + i, 'application/octet-stream', 800, id));
  for (let i = 0; i < 20; i++) probe(7, PLAYER1, i, 100 + i);          // server one asks for more than twelve
  await tick(10);
  assert.equal(h.of('maybe').length, 12, 'twelve per document');
  navigate(h, 7, 0, PLAYER2);                                          // the site points the iframe at server two
  for (let i = 0; i < 20; i++) probe(7, PLAYER2, i, 200 + i);
  await tick(10);
  assert.equal(h.of('maybe').length, 24, 'the second server has its own twelve');
  navigate(h, 7, 0, PLAYER1);
  for (let i = 0; i < 5; i++) probe(7, PLAYER1, 50 + i, 300 + i);
  await tick(10);
  assert.equal(h.of('maybe').length, 29, 'and so has the third document in the same frame');
  // a page full of frames cannot flood the app: 48 over the whole page
  const h2 = harness(standardApp());
  h2.mainFrame(PAGE, { requestId: '10' });
  for (let f = 1; f <= 10; f++) for (let i = 0; i < 12; i++) h2.listeners.headers(inFrame(f, 0, 'https://f' + f + '.example/d', 'https://cdn.example/f' + f + '/' + i, 'application/octet-stream', 800, 1000 + f * 20 + i));
  await tick(10);
  assert.equal(h2.of('maybe').length, 48, 'forty-eight over all frames');
});

test('server switch: an address found in a player frame\'s own script is filed under the PAGE and the player document, even when the extension never saw the page\'s own request (the first page after a cold start)', async () => {
  const h = harness(standardApp());                                              // no mainFrame(): the page loaded before the extension was active
  navigate(h, 7, 0, PLAYER1);                                                   // the iframe's request names its ancestors: the page is learned from them
  h.message({ k: 'dom', u: 'https://ads.example/preroll.mp4', p: PLAYER1 }, 1, PLAYER1, 7);
  await tick(10);
  const m = h.of('media');
  assert.equal(m.length, 1);
  assert.equal(m[0].p, PAGE, 'the page the tab is on, not the player\'s address'); assert.equal(m[0].f, PLAYER1, 'the document that holds it'); assert.equal(m[0].s, 'dom');
  navigate(h, 7, 0, PLAYER2);                                                   // another server: that document is gone, and so is what its script named
  await tick(10);
  const g = h.of('gone');
  assert.equal(g.length, 1); eq(g[0].d, [PLAYER1]);
  h.message({ k: 'dom', u: 'https://ads.example/preroll.mp4', p: PLAYER2 + '#x' }, 1, PLAYER2, 7); // the second player names the same file; a fragment does not make another document
  navigate(h, 7, 0, PLAYER1);
  await tick(10);
  eq(h.of('gone').map((x) => x.d), [[PLAYER1], [PLAYER2]]);
  // a frame whose page cannot be learned (no ancestors) never names the page by its OWN address: the app then takes the tab in front
  const h2 = harness(standardApp());
  h2.message({ k: 'dom', u: 'https://ads.example/x.mp4', p: 'https://orphan.example/frame' }, 1, 'https://orphan.example/frame', 9);
  await tick(10);
  assert.equal(h2.of('media')[0].p, ''); assert.equal(h2.of('media')[0].f, 'https://orphan.example/frame');
  // the top frame's own message teaches the page
  const h3 = harness(standardApp());
  h3.message({ k: 'dom', u: 'https://cdn.example/top.mp4', p: PAGE }, 1, PAGE, 0);
  await tick(10);
  assert.equal(h3.of('media')[0].p, PAGE); assert.equal(h3.of('media')[0].f, '');
});

test('server switch: an advertisement that plays before the real video is NOT a server switch (same document, or a frame of its own)', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '10' });
  h.listeners.headers(inFrame(7, 0, PLAYER1, 'https://ads.example/preroll.mp4', 'video/mp4', 3_000_000, 11));            // the ad, inside the player document
  h.listeners.headers(inFrame(7, 0, PLAYER1, 'https://cdn.example/movie/master.m3u8', 'application/vnd.apple.mpegurl', 300, 12)); // then the real stream
  navigate(h, 8, 7, 'https://adserver.example/frame');                                                                    // an ad frame appears INSIDE the player
  navigate(h, 9, 0, 'https://adserver.example/banner');                                                                   // another next to it
  await tick(10);
  assert.equal(h.of('media').length, 2); assert.equal(h.of('gone').length, 0, 'nothing was replaced');
});

test('server switch: navigating a frame also drops what the frames INSIDE it offered, and a sibling frame is left alone', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '10' });
  navigate(h, 7, 0, PLAYER1);                                    // the outer player frame
  navigate(h, 9, 7, 'https://inner.example/p');                  // an inner frame
  navigate(h, 8, 0, 'https://sidebar.example/s');                // a sibling
  h.listeners.headers(inFrame(9, 7, 'https://inner.example/p', 'https://cdn.example/inner/a.m3u8', 'application/vnd.apple.mpegurl', 300, 20));
  h.listeners.headers(inFrame(8, 0, 'https://sidebar.example/s', 'https://cdn.example/side/b.m3u8', 'application/vnd.apple.mpegurl', 300, 21));
  await tick(10);
  assert.equal(h.of('media').length, 2);
  navigate(h, 7, 0, PLAYER2);                                    // the outer frame is replaced
  await tick(10);
  eq(h.of('gone').map((g) => g.d), [['https://inner.example/p']]);   // the inner frame's document, not the sibling's
});

test('server switch: a new page forgets the old frames (nothing is "gone" for a frame of the page that was left)', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '10' });
  h.listeners.headers(inFrame(7, 0, PLAYER1, 'https://cdn.example/a.m3u8', 'application/vnd.apple.mpegurl', 300, 11));
  h.mainFrame('https://other.example/', { requestId: '30' });
  navigate(h, 7, 0, PLAYER2);
  await tick(10);
  assert.equal(h.of('gone').length, 0);
});

// ---------------------------------------------------------------- tab bookkeeping (AA4 / PERF_JS B2)
test('a tab that keeps being used keeps its page address in the extension while many other tabs come and go', async () => {
  const h = harness(standardApp());
  h.mainFrame('https://example.com/start', { tabId: 1 });
  for (let t = 2; t <= 70; t++) {
    h.mainFrame('https://tab' + t + '.test/', { tabId: t });
    if (t === 30) h.mainFrame('https://example.com/again', { tabId: 1 }); // the long-lived tab navigates again: it is the newest now
  }
  const a = await h.message({ k: 'guard', h: 'frame.test' }, 1);
  assert.equal(a.top, 'example.com', 'tab 1 was not forgotten (the oldest-INSERTED tab was dropped before)');
});

test('sniffer: the file name the server gives (Content-Disposition) travels with the candidate', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  h.listeners.headers({ statusCode: 200, url: 'https://cdn.example/dl.php?id=9', tabId: 1, documentUrl: PAGE, responseHeaders: [
    { name: 'Content-Type', value: 'video/mp4' }, { name: 'Content-Length', value: '9000000' }, { name: 'Content-Disposition', value: 'attachment; filename="My Holiday 2026.mp4"' }, { name: 'Server', value: 'x' }] });
  h.listeners.headers({ statusCode: 200, url: 'https://cdn.example/plain.mp4', tabId: 1, documentUrl: PAGE, responseHeaders: [{ name: 'Content-Type', value: 'video/mp4' }, { name: 'Content-Length', value: '9000000' }] });
  await tick(10);
  const m = h.of('media');
  assert.equal(m.length, 2);
  assert.equal(m[0].n, 'attachment; filename="My Holiday 2026.mp4"');
  assert.equal(m[1].n, '', 'no header, no name');
});

// ---------------------------------------------------------------- embeds wait for a tap (EX-10, research/REVIEW-EXTERNAL-1.md 4.3)
const EMBED_PAGE = 'https://blog.example/post';
const YT = 'https://www.youtube.com/embed/abc123?rel=0';
const EMBED_UI = 'moz-extension://test-id/embed.html';
/** cfg answers carry the page's flags as a bit field: 1 = pop-up guard, 2 = embeds wait for a tap */
const embedApp = (flags) => standardApp({ cfg: `1:1:${ID}:${flags}` });
const sub = (h, url, tabId, page) => h.listeners.before({ type: 'sub_frame', url, tabId: tabId === undefined ? 1 : tabId, frameId: 7, documentUrl: page || EMBED_PAGE, frameAncestors: [{ url: page || EMBED_PAGE, frameId: 0 }] });

test('embeds: a third-party player frame is answered with the tap-to-load page, carrying the address in the fragment', async () => {
  const h = harness(embedApp(2));
  h.mainFrame(EMBED_PAGE);
  const r = await sub(h, YT);
  assert.equal(r.redirectUrl, EMBED_UI + '#' + encodeURIComponent(YT));
  assert.equal(decodeURIComponent(r.redirectUrl.split('#')[1]), YT);
});

test('embeds: every listed player is caught, an ordinary third-party frame and the page\'s own frames are not', async () => {
  const h = harness(embedApp(3));
  h.mainFrame(EMBED_PAGE);
  for (const u of ['https://www.youtube-nocookie.com/embed/x', 'https://player.vimeo.com/video/123?h=a', 'https://www.dailymotion.com/embed/video/x7', 'https://geo.dailymotion.com/player/xyz.html?video=1',
    'https://www.facebook.com/plugins/video.php?href=a', 'https://www.instagram.com/p/Cabc/embed/', 'https://www.tiktok.com/embed/v2/123', 'https://open.spotify.com/embed/track/1',
    'https://w.soundcloud.com/player/?url=x', 'https://disqus.com/embed/comments/?base=default', 'https://www.google.com/maps/embed?pb=!1m18', 'http://kestrel-embed-test.invalid/embed/x']) {
    const r = await sub(h, u);
    assert.ok(r.redirectUrl && r.redirectUrl.startsWith(EMBED_UI + '#'), 'should wait for a tap: ' + u);
  }
  for (const u of ['https://ads.example/frame.html', 'https://www.youtube.com/watch?v=abc', 'https://www.youtube.com/', 'https://notyoutube.com/embed/x', 'https://youtube.com.evil.example/embed/x', 'https://player.vimeo.com/']) {
    eq(await sub(h, u), {}, 'not an embed: ' + u);
  }
  eq(await sub(h, 'https://www.youtube.com/embed/x', 1, 'https://www.youtube.com/watch?v=1'), {}, 'YouTube\'s own page frames its own player: not an embed');
  eq(await sub(h, 'https://www.youtube.com/embed/x', 1, 'https://music.youtube.com/'), {}, 'a subdomain of the same site');
});

test('embeds: only sub_frame requests are looked at, and only when the page\'s flags say so (Shields down, the Settings switch or the site\'s own override)', async () => {
  const on = harness(embedApp(2)); on.mainFrame(EMBED_PAGE);
  eq(await on.req('script', 'https://www.youtube.com/embed/x.js', EMBED_PAGE), {}, 'a script is not a frame');
  eq(await on.req('image', 'https://www.youtube.com/embed/x.png', EMBED_PAGE), {});
  for (const flags of [0, 1]) {
    const h = harness(embedApp(flags)); h.mainFrame(EMBED_PAGE);
    eq(await sub(h, YT), {}, 'flags=' + flags + ': the embed loads at once');
  }
});

test('embeds: the owner\'s tap lets that one address through for that tab for 30 seconds, then it waits again', async () => {
  const h = harness(embedApp(2));
  h.mainFrame(EMBED_PAGE);
  assert.ok((await sub(h, YT)).redirectUrl);
  assert.equal(await h.message({ k: 'embed-allow', u: YT }, 1, EMBED_UI + '#' + encodeURIComponent(YT)), true);
  eq(await sub(h, YT), {}, 'the tapped player loads');
  eq(await sub(h, YT + '#t=5'), {}, 'a fragment does not make it another address');
  assert.ok((await sub(h, YT, 2)).redirectUrl, 'another tab still waits');
  assert.ok((await sub(h, 'https://www.youtube.com/embed/other')).redirectUrl, 'another player in the same tab still waits');
  h.clock.now += 31000;
  assert.ok((await sub(h, YT)).redirectUrl, 'after 30 s the allowance is gone');
});

test('embeds: only the extension\'s own placeholder page can allow an address, and only a web address', async () => {
  const h = harness(embedApp(2));
  h.mainFrame(EMBED_PAGE);
  assert.equal(await h.message({ k: 'embed-allow', u: YT }, 1, 'https://evil.example/page'), undefined, 'a web page cannot');
  assert.equal(await h.message({ k: 'embed-allow', u: YT }, 1, undefined), undefined, 'a content script (no extension page url) cannot');
  assert.equal(await h.message({ k: 'embed-allow', u: 'javascript:alert(1)' }, 1, EMBED_UI + '#x'), undefined, 'not a web address');
  assert.equal(await h.message({ k: 'embed-allow', u: 'moz-extension://test-id/embed.html' }, 1, EMBED_UI), undefined);
  assert.ok((await sub(h, YT)).redirectUrl, 'nothing was allowed by those');
});

test('embeds: a frame that waits is counted as one thing held back for the page, and the first frame of a page is gated even before the engine is loaded', async () => {
  let release;
  const gate = new Promise((r) => { release = r; });
  const h = harness((m) => (m.k === 'img' ? gate.then(() => chunkReply(m.g, m.i, 2048, ID)) : embedApp(2)(m)));
  h.mainFrame(EMBED_PAGE);
  const r = await sub(h, YT); // no engine yet: the page configuration decides
  assert.ok(r.redirectUrl && r.redirectUrl.startsWith(EMBED_UI + '#'));
  await h.flushTimers();
  const cnt = h.of('cnt');
  assert.ok(cnt.length >= 1 && cnt[0].c && cnt[0].c[3] === 1, 'one annoyance-category count: ' + JSON.stringify(cnt));
  release();
});

// ---------------------------------------------------------------- the page report (Diagnostics > Page report)
const SITE = 'https://example.com/';
function ready(h) { return until(() => h.of('img').length >= 3, 'the image transfer').then(() => until(() => !isPromise(h.req('script', PLAIN, PAGE)), 'the engine')); }

test('page report: what Shields blocked, what failed to load, an HTTP error and a script error reach the app once the page has settled; our own cancel is not a failure', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  await ready(h);
  // Two flakes of this test (CI run 43399774749, alpha30; the same on a loaded laptop): (1) the number of plain requests counted depended on how many times `ready` had to poll (one poll = 2 counted,
  // the assertion wants 3): count one on purpose; (2) the engine answers a plain request before the page's own configuration is applied, so the FIRST request a rule matches can still be a promise
  // (which the JSON comparison prints as {}): await it, the verdict is the same.
  h.req('script', PLAIN, PAGE);
  eq(await h.listeners.before({ type: 'script', url: AD, tabId: 1, frameId: 0, requestId: 'r1', documentUrl: PAGE, frameAncestors: [{ url: PAGE, frameId: 0 }] }), { cancel: true });
  h.listeners.error({ tabId: 1, requestId: 'r1', type: 'script', url: AD, error: 'NS_ERROR_ABORT' });                 // our own cancel comes back as an error: not a failure
  h.listeners.error({ tabId: 1, requestId: 'r2', type: 'script', url: 'https://cdn.example.net/lib.js?ver=2', error: 'NS_ERROR_NET_RESET' });
  h.listeners.error({ tabId: 1, requestId: 'r3', type: 'image', url: 'https://example.com/left.png', error: 'NS_BINDING_ABORTED' });   // the page was left: not a failure either
  h.listeners.completed({ tabId: 1, type: 'stylesheet', url: 'https://example.com/a.css', statusCode: 404 });
  h.listeners.completed({ tabId: 1, type: 'image', url: 'https://example.com/ok.png', statusCode: 200 });
  h.message({ k: 'jserr', t: 'js', m: 'ReferenceError: Swiper is not defined', s: 'https://example.com/wp-content/script.js', l: 1 }, 1);
  assert.equal(h.of('rep').length, 0, 'nothing is sent before the page has settled');
  await h.flushTimers();
  const r = h.of('rep');
  assert.equal(r.length, 1);
  assert.equal(r[0].p, PAGE);
  assert.ok(r[0].n >= 3, 'requests counted: ' + r[0].n);
  assert.equal(r[0].nb, 1);
  eq(r[0].b, ['script ads.example.com/x.js']);
  eq(r[0].f, ['script cdn.example.net/lib.js?.. NET_RESET', 'stylesheet example.com/a.css HTTP 404']);
  eq(r[0].e, ['ReferenceError: Swiper is not defined @ example.com/wp-content/script.js:1']);
  await h.flushTimers();
  assert.equal(h.of('rep').length, 1, 'nothing new: nothing sent again');
});

test('page report: a failed element load told by the page is listed, news after the first report is sent again, at most three times per page, a new page starts clean', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  await ready(h);
  await h.flushTimers();                         // the first report: a page with nothing wrong is reported too (the app shows "no failed loads")
  assert.equal(h.of('rep').length, 1);
  eq(h.of('rep')[0].f, []);
  h.message({ k: 'jserr', t: 'load', m: 'script', s: 'https://cdn.example.net/swiper.js?ver=5' }, 1);
  await h.flushTimers();
  assert.equal(h.of('rep').length, 2);
  eq(h.of('rep')[1].e, ['file not usable: script cdn.example.net/swiper.js?..']);
  h.listeners.completed({ tabId: 1, type: 'script', url: 'https://example.com/a.js', statusCode: 500 }); await h.flushTimers();
  assert.equal(h.of('rep').length, 3);
  h.listeners.completed({ tabId: 1, type: 'script', url: 'https://example.com/b.js', statusCode: 500 }); await h.flushTimers();
  assert.equal(h.of('rep').length, 3, 'the third report was the last one for this page');
  h.mainFrame('https://other.example/'); await h.flushTimers();
  assert.equal(h.of('rep').length, 4);
  assert.equal(h.of('rep')[3].p, 'https://other.example/');
  eq(h.of('rep')[3].f, []); eq(h.of('rep')[3].e, []);
});

test('page report: a message from a frame without a tab, an unknown tab and a malformed message change nothing and throw nothing', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE);
  await ready(h);
  assert.equal(h.message({ k: 'jserr', t: 'js', m: 'x', s: '', l: 0 }, -1), undefined);
  assert.equal(h.message({ k: 'jserr', t: 'js', m: 'x' }, 99), undefined);
  assert.equal(h.message({ k: 'nonsense' }, 1), undefined);
  h.listeners.error({ tabId: -1, requestId: 'x', type: 'script', url: 'https://a.example/b.js', error: 'NS_ERROR_NET_RESET' }); // not a tab (the browser's own request)
  await h.flushTimers();
  eq(h.of('rep')[0].f, []); eq(h.of('rep')[0].e, []);
});

test('page report: a page whose main request this script never saw (a cold start with a link) is still reported from its first sub-resource', async () => {
  const h = harness(standardApp());
  await h.req('script', PLAIN, 'https://early.example/');           // no main_frame request first: the extension was not ready yet
  h.listeners.completed({ tabId: 1, type: 'image', url: 'https://early.example/a.png', statusCode: 404 });
  await h.flushTimers();
  const r = h.of('rep');
  assert.equal(r.length, 1);
  assert.equal(r[0].p, 'https://early.example/');
  eq(r[0].f, ['image early.example/a.png HTTP 404']);
});


// ---------------------------------------------------------------- AW1b+: the scriptlet calls of a site are registered BEFORE its document runs
// The app answers `sc` with "<list generation>:<json>". The extension turns a non-empty answer into a page-world script for the exact host, registered after the library's own.
const SLSITE = 'https://site.example/';
const SLCALLS = '[["set","adblockDetected","false"],["nostif","nag"]]';
function scHarness(answers, extra) {
  const regs = [];
  const app = (m) => (m.k === 'sc' ? (answers[m.h] !== undefined ? answers[m.h] : '1:') : standardApp()(m));
  const h = harness(app, Object.assign({ scripts: { list: regs } }, extra || {}));
  // the guard and the library are registered when the page starts; the scripts of sites are the ones that carry code
  const mine = () => regs.filter((r) => r.opts.js[0].code !== undefined);
  return { h, regs, answers, mine };
}
/** what a registered one-liner does in the page: the events it dispatches on the document */
function runCode(code) {
  const got = [];
  const ctx = { document: { dispatchEvent: (e) => got.push(e) }, CustomEvent: function (name, init) { this.type = name; this.detail = init.detail; } };
  vm.createContext(ctx);
  vm.runInContext(code, ctx);
  return got.map((e) => [e.type, e.detail]);
}

test('early scriptlets: the first navigation to a site asks the app, does not wait for it, and registers a page-world script after the library', async () => {
  const { h, regs, mine } = scHarness({ 'site.example': '1:' + SLCALLS });
  const r = h.mainFrame(SLSITE);
  assert.ok(!isPromise(r), 'a site seen for the first time never holds its request up');
  eq(r, {});
  await until(() => mine().length === 1, 'the site script');
  assert.equal(regs.findIndex((x) => x.opts.js && x.opts.js[0].file === 'scriptlets-main.js'), -1, 'the library and the guard are declared in the manifest (RX3-G1): nothing of them is registered at run time');
  const o = mine()[0].opts;
  eq(o.matches, ['http://site.example/*', 'https://site.example/*']);
  assert.equal(o.runAt, 'document_start'); assert.equal(o.world, 'MAIN'); assert.equal(o.allFrames, true); assert.equal(o.matchAboutBlank, true); assert.equal(o.matchOriginAsFallback, true);
  eq(runCode(o.js[0].code), [['__kst_sl_v1', SLCALLS]]);   // the exact text arrives, quotes and all
  assert.equal(h.of('sc').length, 1);
  assert.equal(h.of('sc')[0].h, 'site.example');
});

test('early scriptlets: the script text is a harmless one-liner whatever the calls contain', async () => {
  const nasty = '[["rpnt","script","</script><!--\\u002a/ \\" \\\\ \'x\'"]]';
  const { h, mine } = scHarness({ 'site.example': '1:' + nasty });
  h.mainFrame(SLSITE);
  await until(() => mine().length === 1, 'the site script');
  eq(runCode(mine()[0].opts.js[0].code), [['__kst_sl_v1', nasty]]);
  assert.ok(!/\n/.test(mine()[0].opts.js[0].code));
});

test('early scriptlets: a site that already has a script waits for the app before its next document, and an unchanged answer registers nothing again', async () => {
  const { h, mine } = scHarness({ 'site.example': '1:' + SLCALLS });
  h.mainFrame(SLSITE);
  await until(() => mine().length === 1, 'the site script');
  const again = h.mainFrame(SLSITE);
  assert.ok(isPromise(again), 'a reload after a Shields or Settings switch must be clean: the answer is awaited');
  eq(await again, {});
  assert.equal(h.of('sc').length, 2);
  assert.equal(mine().length, 1, 'same calls: no second registration');
  assert.equal(mine()[0].gone, false);
});

test('early scriptlets: Shields or the Settings switch turned off (empty answer) unregisters the script BEFORE the next document starts', async () => {
  const { h, mine, answers } = scHarness({ 'site.example': '1:' + SLCALLS });
  h.mainFrame(SLSITE);
  await until(() => mine().length === 1, 'the site script');
  answers['site.example'] = '1:';
  eq(await h.mainFrame(SLSITE), {});
  assert.equal(mine()[0].gone, true, 'the stale script is gone by the time the request is allowed');
  assert.equal(mine().length, 1);
  assert.ok(!isPromise(h.mainFrame(SLSITE)), 'nothing registered any more: no wait, and the empty answer is remembered for a while');
  assert.equal(h.of('sc').length, 2);
});

test('early scriptlets: changed calls (a list update) replace the registration, the old one is removed', async () => {
  const { h, mine, answers } = scHarness({ 'site.example': '1:' + SLCALLS });
  h.mainFrame(SLSITE);
  await until(() => mine().length === 1, 'the site script');
  answers['site.example'] = '1:[["nowoif"]]';
  await h.mainFrame(SLSITE);
  assert.equal(mine().length, 2);
  assert.equal(mine()[0].gone, true); assert.equal(mine()[1].gone, false);
  eq(runCode(mine()[1].opts.js[0].code), [['__kst_sl_v1', '[["nowoif"]]']]);
});

test('early scriptlets: a new list generation drops every early script (they are asked for again on the next visit)', async () => {
  let gen = 1;
  const regs = [];
  const h = harness((m) => (m.k === 'sc' ? gen + ':' + SLCALLS : (m.k === 'cfg' ? `1:${gen}:${ID}` : standardApp()(m))), { scripts: { list: regs } });
  const mine = () => regs.filter((r) => r.opts.js[0].code !== undefined);
  h.mainFrame(SLSITE);
  await until(() => mine().length === 1, 'the site script');
  gen = 2;
  h.mainFrame('https://other.example/');
  await until(() => mine()[0].gone, 'the old script to go');
});

test('early scriptlets: while the app\'s lists are loading (generation 0) nothing is registered and nothing is remembered', async () => {
  const { h, mine, answers } = scHarness({ 'site.example': '0:' + SLCALLS });
  h.mainFrame(SLSITE);
  await until(() => h.of('sc').length === 1, 'the question');
  await tick(20);
  assert.equal(mine().length, 0);
  answers['site.example'] = '1:' + SLCALLS;
  h.mainFrame(SLSITE);
  await until(() => mine().length === 1, 'the script once the lists are in');
  assert.equal(h.of('sc').length, 2, 'asked again at once');
});

test('early scriptlets: "nothing here" is remembered for a minute, then asked again', async () => {
  const { h, mine } = scHarness({});
  h.mainFrame(SLSITE); await until(() => h.of('sc').length === 1, 'the question'); await tick(20);
  h.mainFrame(SLSITE); h.mainFrame(SLSITE); await tick(20);
  assert.equal(h.of('sc').length, 1, 'no new question within the minute');
  h.clock.now += 61000;
  h.mainFrame(SLSITE); await until(() => h.of('sc').length === 2, 'the question after the minute');
  assert.equal(mine().length, 0);
});

test('early scriptlets: a frame of another site is asked about after the network decision; a blocked frame never is', async () => {
  const { h, mine } = scHarness({ 'embed.example': '1:[["nowoif"]]' });
  h.mainFrame(PAGE);
  await h.req('script', PLAIN, PAGE);                       // the page's configuration is in
  const frame = (url) => h.listeners.before({ type: 'sub_frame', url, tabId: 1, frameId: 5, parentFrameId: 0, documentUrl: PAGE, frameAncestors: [{ url: PAGE, frameId: 0 }] });
  eq(await frame(AD), { cancel: true });
  assert.equal(h.of('sc').filter((m) => m.h === 'ads.example.com').length, 0, 'a cancelled frame is not asked about');
  eq(await frame('https://embed.example/player'), {});
  await until(() => mine().some((r) => r.opts.matches[0] === 'http://embed.example/*'), 'the frame site script');
  assert.equal(h.of('sc').filter((m) => m.h === 'embed.example').length, 1);
  // a second frame of the same site within the minute trusts the answer it has
  eq(await frame('https://embed.example/player2'), {});
  assert.equal(h.of('sc').filter((m) => m.h === 'embed.example').length, 1);
});

test('early scriptlets: at most 40 sites are kept, the least recently used one is unregistered', async () => {
  const answers = {};
  for (let i = 0; i < 45; i++) answers['s' + i + '.example'] = '1:[["nowoif"]]';
  const { h, mine } = scHarness(answers);
  for (let i = 0; i < 45; i++) { h.mainFrame('https://s' + i + '.example/'); await until(() => mine().length === i + 1, 'script ' + i); }
  assert.equal(mine().filter((r) => !r.gone).length, 40);
  assert.equal(mine()[0].gone, true); assert.equal(mine()[4].gone, true); assert.equal(mine()[5].gone, false);
});

test('early scriptlets: a registration the browser refuses, an app that cannot answer and a library that could not be registered never break a request', async () => {
  const refuse = scHarness({ 'site.example': '1:' + SLCALLS }, { scripts: { list: [], fail: (o) => o.matches && o.matches[0] === 'http://site.example/*' } });
  eq(refuse.h.mainFrame(SLSITE), {});
  await until(() => refuse.h.of('sc').length === 1, 'the question'); await tick(20);
  eq(refuse.h.mainFrame(SLSITE), {});                            // remembered as "nothing" for a while: no wait, no second question
  const broken = harness((m) => { if (m.k === 'sc') throw new Error('app down'); return standardApp()(m); }, { scripts: { list: [] } });
  eq(broken.mainFrame(SLSITE), {});
  await tick(20);
  eq(broken.mainFrame(SLSITE), {});
});

test('early scriptlets: the numbers for Diagnostics say how many pages ran calls and how many of them before the page\'s first script', async () => {
  const { h, mine } = scHarness({ 'site.example': '1:' + SLCALLS });
  h.mainFrame(SLSITE);
  await until(() => mine().length === 1, 'the site script');
  h.message({ k: 'sl', r: '4:1:1', h: 'site.example', f: 1 }, 1);
  h.message({ k: 'sl', r: '2:0:0', h: 'site.example', f: 0 }, 1);  // more calls of the same page: the page is not counted twice
  h.message({ k: 'sl', r: '3:0:0', h: 'other.example', f: 1 }, 1);
  await h.flushTimers();
  const w = h.of('st').map((m) => m.w).join(' | ');
  assert.match(w, /2 pages ran 9 calls \(1 failed\), 1 before the page's first script/, w);
  assert.match(w, /1 sites set up early \(1 kept, 1 asks\)/, w);
});


// ---------------------------------------------------------------- AW1c: a blocked request is answered with a harmless stand-in when its rule names one
// The fixture list has `||redir.test^$script,redirect=noopjs`, `||redir.test/pix^$image,redirect=1x1.gif` and (for the type-mismatch case) `||ro.test^$script` + `||ro.test^$redirect-rule=noopjs`.
async function standInHarness(flags) {
  const h = harness(standardApp({ cfg: `1:1:${ID}:${flags}` }));
  h.mainFrame(PAGE);
  await until(() => h.of('img').length >= 3, 'the image transfer');
  await until(() => !isPromise(h.req('script', PLAIN, PAGE)), 'the engine');
  return h;
}

test('stand-ins: the rule names one and the page allows it (flag 4): the request is redirected to a data: URL of the right kind and still counted as blocked', async () => {
  const h = await standInHarness(4);
  const js = h.req('script', 'https://redir.test/a.js', PAGE);
  assert.ok(js.redirectUrl && js.redirectUrl.indexOf('data:text/javascript') === 0 && js.cancel === undefined, JSON.stringify(js));
  const gif = h.req('image', 'https://redir.test/pix/a.gif', PAGE);
  assert.ok(gif.redirectUrl && gif.redirectUrl.indexOf('data:image/gif;base64,') === 0, JSON.stringify(gif));
  assert.ok(h.req('script', 'https://ro.test/a.js', PAGE).redirectUrl, 'redirect-rule picks the stand-in for a request the other rule blocks');
  eq(h.req('image', 'https://ro.test/a.gif', PAGE), { cancel: true });            // blocked, but the stand-in the rule names (a script) does not suit a picture: plain cancel
  eq(h.req('script', AD, PAGE), { cancel: true });                                // an ordinary block rule names none
  eq(h.req('script', PLAIN, PAGE), {});                                           // not blocked at all
  await h.flushTimers();
  eq(h.of('cnt')[0].c.reduce((a, b) => a + b, 0), 5, 'every one of the five blocked requests is counted, redirected or not');
});

test('stand-ins: without flag 4 (Settings switch "Fix ad and anti-adblock scripts" off, or Shields off for the page) every blocked request is a plain cancel', async () => {
  const h = await standInHarness(1);
  eq(h.req('script', 'https://redir.test/a.js', PAGE), { cancel: true });
  eq(h.req('image', 'https://redir.test/pix/a.gif', PAGE), { cancel: true });
});

test('stand-ins: a stand-in that cannot be built never breaks the request: it is cancelled as before', async () => {
  const h = await standInHarness(4);
  h.ctx.KestrelRedirects.urlFor = () => { throw new Error('boom'); };
  eq(h.req('script', 'https://redir.test/a.js', PAGE), { cancel: true });
});

test('stand-ins: the Diagnostics numbers count the files answered with a stand-in', async () => {
  const h = await standInHarness(4);
  h.req('script', 'https://redir.test/a.js', PAGE); h.req('script', 'https://redir.test/b.js', PAGE);
  await h.flushTimers();
  assert.match(h.of('st').map((m) => m.w).join(' | '), /2 blocked files answered with a stand-in/);
});

// ---------------------------------------------------------------- RX3-BK1 / RX3-BK2 (research/REVIEW-EXTERNAL-3.md section 3): the Back lab's signals and the place question

test('RX3-BK2: the place question of a reloaded page is relayed to the app and its answer comes back; frames and senders without a tab get nothing', async () => {
  const h = harness((m) => (m.k === 'bk' ? (m.p === PAGE ? '1200' : null) : standardApp()(m)));
  const r = h.message({ k: 'bk', p: PAGE }, 1, PAGE, 0);
  assert.ok(isPromise(r), 'an answer is promised to the page');
  assert.equal(await r, '1200');
  assert.equal(await h.message({ k: 'bk', p: 'https://other.test/' }, 1, PAGE, 0), '', 'nothing armed for that page, or a reply that is not text: an empty answer');
  assert.equal(h.message({ k: 'bk', p: PAGE }, 1, PAGE, 3), undefined, 'an iframe never asks');
  assert.equal(h.message({ k: 'bk', p: PAGE }, -1, PAGE, 0), undefined, 'not from a tab');
  assert.equal(h.of('bk')[0].p, PAGE);
});

test('RX3-BK1: the show report is forwarded with its five fields only, from the top frame only', async () => {
  const h = harness(standardApp());
  h.message({ k: 'bf', p: PAGE, ps: 1, nt: 'back_forward', ts: 4096.7, y: 640.2, junk: 'x'.repeat(5000) }, 1, PAGE, 0);
  h.message({ k: 'bf', p: PAGE, ps: 0, nt: 'back_forward', ts: 1, y: 0 }, 1, PAGE, 2); // a frame
  await tick(20);
  eq(h.of('bf'), [{ k: 'bf', p: PAGE, ps: 1, nt: 'back_forward', ts: 4096, y: 640 }]);
});

test('RX3-BK1: a main page that forbids caching (Cache-Control: no-store) is reported once; other responses and other pages are not', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '5' });
  const res = (type, url, id, headers) => h.listeners.headers({ tabId: 1, frameId: 0, type, url, statusCode: 200, requestId: id, responseHeaders: headers });
  res('main_frame', PAGE, '5', [{ name: 'Content-Type', value: 'text/html' }, { name: 'Cache-Control', value: 'private, No-Store, max-age=0' }]);
  res('xmlhttprequest', 'https://example.com/api', '6', [{ name: 'Cache-Control', value: 'no-store' }]);            // a sub-resource: not the page
  res('main_frame', 'https://example.com/ok', '7', [{ name: 'Cache-Control', value: 'max-age=60' }]);               // cacheable: nothing to say
  res('main_frame', 'https://example.com/old', '3', [{ name: 'Cache-Control', value: 'no-store' }]);                // an answer for a document the tab already left
  await tick(20);
  eq(h.of('ns').map((m) => m.p), [PAGE]);
});

test('RX3-BK1: a page left with requests still pending is reported once with how many; a page that finished is not', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '5' });
  for (let i = 0; i < 5; i++) h.req('script', PLAIN + '?' + i, PAGE);
  h.listeners.completed({ tabId: 1, requestId: '6', type: 'script', url: PLAIN + '?0', statusCode: 200 });
  h.listeners.completed({ tabId: 1, requestId: '7', type: 'script', url: PLAIN + '?1', statusCode: 200 });
  h.listeners.error({ tabId: 1, requestId: '8', type: 'script', url: PLAIN + '?2', error: 'NS_BINDING_ABORTED' });   // an aborted request is no longer pending either
  h.mainFrame('https://example.com/next', { requestId: '20' });
  await tick(20);
  const p = h.of('pend');
  assert.equal(p.length, 1);
  assert.equal(p[0].n, 2);
  assert.equal(p[0].p, PAGE);
  // the next page finishes everything it started: leaving it says nothing
  h.req('script', PLAIN + '?z', 'https://example.com/next');
  h.listeners.completed({ tabId: 1, requestId: '21', type: 'script', url: PLAIN + '?z', statusCode: 200 });
  h.mainFrame('https://example.com/third', { requestId: '30' });
  await tick(20);
  assert.equal(h.of('pend').length, 1);
});

test('RX3-BK1: an answer to a request of the page the tab already left does not count as one of the new page', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '5' });
  h.req('script', PLAIN + '?a', PAGE);
  h.mainFrame('https://example.com/next', { requestId: '20' });
  h.req('script', PLAIN + '?b', 'https://example.com/next');
  h.listeners.completed({ tabId: 1, requestId: '6', type: 'script', url: PLAIN + '?a', statusCode: 200 }); // belongs to the old document (id 6 < 20)
  h.mainFrame('https://example.com/third', { requestId: '30' });
  await tick(20);
  const p = h.of('pend');
  assert.equal(p.filter((m) => m.p === 'https://example.com/next')[0].n, 1, 'the late answer of the old page did not hide the new page\'s own pending request');
});

// ---------------------------------------------------------------- RX3-G3: the assist of a player inside a frame is relayed to the ONE frame the owner pressed

const frameReq = (h, id, parent, url) => h.listeners.before({ type: 'sub_frame', url, tabId: 1, frameId: id, parentFrameId: parent, requestId: String(100 + id), documentUrl: PAGE, frameAncestors: [{ url: PAGE, frameId: 0 }] });

test('RX3-G3: an assist from the parent frame goes to the ONE child frame that has the iframe\'s address, as a number place; frames inside it and other frames get nothing', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '5' });
  frameReq(h, 3, 0, 'https://player.example/embed');
  frameReq(h, 4, 0, 'https://other.example/ad');
  frameReq(h, 5, 3, 'https://inner.example/x');
  h.message({ k: 'assist', x: 100.4, y: 80, s: 'https://player.example/embed#t=1' }, 1, PAGE, 0);
  await tick(20);
  assert.equal(h.tabMsgs.length, 1);
  eq(h.tabMsgs[0], { tabId: 1, m: { k: 'assist', x: 100, y: 80 }, o: { frameId: 3 } });
});

test('RX3-G3: two frames with the same address and none to tell apart: no guess, nothing is sent', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '5' });
  frameReq(h, 3, 0, 'https://player.example/embed');
  frameReq(h, 4, 0, 'https://player.example/embed');
  h.message({ k: 'assist', x: 10, y: 10, s: 'https://player.example/embed' }, 1, PAGE, 0);
  await tick(20);
  assert.equal(h.tabMsgs.length, 0);
});

test('RX3-G3: the only child frame is used even when its address changed since the request; an inner frame can ask about its own child', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '5' });
  frameReq(h, 3, 0, 'https://player.example/embed');
  frameReq(h, 5, 3, 'https://inner.example/x');
  h.message({ k: 'assist', x: 5, y: 6, s: 'https://player.example/other-address' }, 1, PAGE, 0);   // top frame: its only child is frame 3
  h.message({ k: 'assist', x: 7, y: 8, s: '' }, 1, 'https://player.example/embed', 3);              // frame 3: its only child is frame 5
  await tick(20);
  eq(h.tabMsgs.map((t) => [t.o.frameId, t.m.x, t.m.y]), [[3, 5, 6], [5, 7, 8]]);
});

test('RX3-G3: nothing is relayed for a sender without a tab, a place that is not a number, no known frame, or another page after a navigation', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '5' });
  frameReq(h, 3, 0, 'https://player.example/embed');
  h.message({ k: 'assist', x: 10, y: 10, s: 'https://player.example/embed' }, -1, PAGE, 0);      // not from a tab
  h.message({ k: 'assist', x: 'a', y: 10, s: 'https://player.example/embed' }, 1, PAGE, 0);
  h.message({ k: 'assist', x: -4, y: 10, s: 'https://player.example/embed' }, 1, PAGE, 0);
  h.message({ k: 'assist', x: 99999, y: 10, s: 'https://player.example/embed' }, 1, PAGE, 0);
  h.message({ k: 'assist', x: 10, y: 10, s: 'https://player.example/embed' }, 1, PAGE, 9);        // a frame that has no children
  h.mainFrame('https://example.com/next', { requestId: '30' });                                    // a new page: the old frames are forgotten
  h.message({ k: 'assist', x: 10, y: 10, s: 'https://player.example/embed' }, 1, 'https://example.com/next', 0);
  await tick(20);
  assert.equal(h.tabMsgs.length, 0);
});

test('RX3-G7: the manifest declares the other-players ad skipper for every web page and frame except the YouTube hosts (youtube.js owns those), in the extension world, from the first moment', () => {
  const m = JSON.parse(fs.readFileSync(path.join(SHIELDS, 'manifest.json'), 'utf8'));
  const e = m.content_scripts.filter((c) => c.js.includes('players.js'));
  assert.equal(e.length, 1, 'declared once');
  assert.equal(e[0].run_at, 'document_start'); assert.equal(e[0].all_frames, true); assert.equal(e[0].world, undefined, 'it needs the extension API: not the page world');
  assert.deepEqual(e[0].matches, ['http://*/*', 'https://*/*']);
  assert.deepEqual(e[0].exclude_matches, ['*://*.youtube.com/*', '*://*.youtube-nocookie.com/*', '*://*.youtubekids.com/*']);
  const y = m.content_scripts.filter((c) => c.js.includes('youtube.js'));
  assert.deepEqual(y[0].matches, e[0].exclude_matches, 'the two scripts split the web between them: exactly the same hosts');
});

test('RX3-G7: the ad skipper reports are forwarded as numbers with their kind (YouTube y, other players p) and the tab page, nothing else', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '9' });
  h.message({ k: 'yts', w: 'p', a: 2, s: 1, c: 1, o: 0, d: 0, f: 1, g: 0, r: 'speed refused', junk: 'x'.repeat(4000) }, 1, PAGE, 0);
  h.message({ k: 'yts', a: 1, s: 1, c: 0, o: 0, d: 0, f: 0, g: 0, r: '' }, 1, PAGE, 3);               // from youtube.js: no kind given
  h.message({ k: 'yts', w: 'zzz', a: 5000, s: -3, c: 'x', r: 'y'.repeat(300) }, 1, PAGE, 0);          // a made-up kind, absurd numbers, a long reason
  assert.equal(h.message({ k: 'yts', w: 'p', a: 1 }, -1, PAGE, 0), undefined, 'not from a tab');
  await tick(20);
  const got = h.of('yts');
  assert.equal(got.length, 3);
  assert.equal(got[0].w, 'p'); assert.equal(got[0].a, 2); assert.equal(got[0].f, 1); assert.equal(got[0].r, 'speed refused'); assert.equal(got[0].junk, undefined);
  assert.equal(got[1].w, 'y', 'no kind means YouTube');
  assert.equal(got[2].w, 'y'); assert.equal(got[2].a, 0); assert.equal(got[2].s, 0); assert.equal(got[2].c, 0); assert.equal(got[2].r.length, 60);
});

test('RX3-G7: a report from the top frame of a tab whose page the extension never saw (the first page after a cold start) names that page; a frame of such a tab names none', async () => {
  const h = harness(standardApp());                                                             // no main-frame request was judged: tabTop knows nothing about tab 7
  h.message({ k: 'yts', w: 'p', a: 1, s: 1 }, 7, PAGE + '#section', 0);
  h.message({ k: 'yts', w: 'p', a: 1, s: 1 }, 7, 'https://player.example/embed?id=1', 4);   // a frame: its own address is NOT the page
  await tick(20);
  const got = h.of('yts');
  assert.equal(got.length, 2);
  assert.equal(got[0].p, PAGE, 'the top frame\'s address without its fragment');
  assert.equal(got[1].p, '', 'a frame says nothing: the app then drops the report like one from a private tab');
});

test('RX4-M0a: a video look is forwarded as clamped whole numbers with the tab page, nothing else (no address of the video); from no tab, nothing', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '9' });
  h.message({ k: 'vq', t: 400, d: 12, c: 0, s: 15, w: 1280, h: 720, junk: 'x'.repeat(4000), u: 'https://cdn.example/v.mp4', p: 'https://evil.example/' }, 1, PAGE, 0);
  h.message({ k: 'vq', t: 99999999, d: -5, c: 'x', s: 99999, w: 1e9, h: 'q' }, 1, PAGE, 3);          // a frame of the tab, absurd numbers
  assert.equal(h.message({ k: 'vq', t: 10 }, -1, PAGE, 0), undefined, 'not from a tab');
  await tick(20);
  const got = h.of('vq');
  assert.equal(got.length, 2);
  assert.deepEqual(got[0], { k: 'vq', t: 400, d: 12, c: 0, s: 15, w: 1280, h: 720, p: PAGE }, 'only the numbers and the page of the tab');
  assert.deepEqual(got[1], { k: 'vq', t: 0, d: 0, c: 0, s: 0, w: 0, h: 0, p: PAGE }, 'absurd numbers become 0 and the page is the tab\'s, not the frame\'s');
});

test('RX4-M0e: a scroll burst is forwarded as clamped whole numbers with the tab page, from the top frame only; flag 32 of the page configuration is the guard answer\'s sg', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '9' });
  h.message({ k: 'sg', n: 120, a: 6, b: 2, mx: 180, junk: 'x'.repeat(4000), p: 'https://evil.example/' }, 1, PAGE, 0);
  h.message({ k: 'sg', n: 99999, a: -1, b: 'x', mx: 1e9 }, 1, PAGE, 0);
  h.message({ k: 'sg', n: 120, a: 6, b: 2, mx: 180 }, 1, 'https://player.example/embed', 4);        // a frame: never measured
  assert.equal(h.message({ k: 'sg', n: 10 }, -1, PAGE, 0), undefined, 'not from a tab');
  await tick(20);
  const got = h.of('sg');
  assert.equal(got.length, 2);
  assert.deepEqual(got[0], { k: 'sg', n: 120, a: 6, b: 2, mx: 180, p: PAGE });
  assert.deepEqual(got[1], { k: 'sg', n: 0, a: 0, b: 0, mx: 0, p: PAGE }, 'absurd numbers become 0');
});

test('RX4-M0b: cache hits and 304 answers are counted (when the headers arrive and when the request completes), a 304 is timed from its start, pages are counted; the text rides on the stats message', async () => {
  const h = harness(standardApp());
  const sub = (id, ts) => h.listeners.before({ type: 'script', url: 'https://cdn.example/' + id + '.js', tabId: 1, frameId: 0, requestId: id, timeStamp: ts, documentUrl: PAGE, frameAncestors: [{ url: PAGE, frameId: 0 }] });
  h.mainFrame(PAGE, { requestId: '5', timeStamp: 1000 });
  sub('6', 1010); sub('7', 1010); sub('8', 1010);
  h.listeners.headers({ statusCode: 304, requestId: '6', url: 'https://cdn.example/6.js', tabId: 1, frameId: 0, type: 'script', responseHeaders: [], timeStamp: 1140 });
  h.listeners.completed({ requestId: '6', statusCode: 304, fromCache: false, url: 'https://cdn.example/6.js', tabId: 1, frameId: 0, type: 'script', timeStamp: 1150 });
  h.listeners.completed({ requestId: '7', statusCode: 200, fromCache: true, url: 'https://cdn.example/7.js', tabId: 1, frameId: 0, type: 'script', timeStamp: 1012 });
  h.listeners.error({ requestId: '8', error: 'NS_ERROR_ABORT', url: 'https://cdn.example/8.js', tabId: 1, frameId: 0, type: 'script' });
  await h.flushTimers();
  const st = h.of('st'); assert.ok(st.length > 0, 'a stats message was sent');
  const rv = st[st.length - 1].rv;
  assert.ok(/pages 1, requests 2, from cache 1, 304 seen when headers arrived 1 and when completed 1 \(140 ms in all\)/.test(rv), rv);
});

test('RX4-M0c: the wait before a navigation to a site that has scriptlet rules is timed and counted (and the answer still arrives)', async () => {
  const { h, mine } = scHarness({ 'site.example': '1:' + SLCALLS });
  h.mainFrame(SLSITE);
  await until(() => mine().length === 1, 'the site script');
  eq(await h.mainFrame(SLSITE), {});
  await h.flushTimers();
  const st = h.of('st'); assert.ok(st.length > 0);
  const rv = st[st.length - 1].rv;
  assert.ok(/scriptlet waits before a page 1 \(\d+ ms in all, slowest \d+ ms, 0 hit the 250 ms ceiling\)/.test(rv), rv);
});

test('RX3-V1: the video report carries the element facts as clamped whole numbers; a report without them (an older script) stays as it was', async () => {
  const h = harness(standardApp());
  h.message({ k: 'vid', u: 'https://cdn.example/a.mp4', w: 1280, h: 720, d: 25.4, l: '', p: PAGE, f: 27, rw: 300.7, rh: 170, vp: 80 }, 1, PAGE, 0);
  h.message({ k: 'vid', u: 'https://cdn.example/b.mp4', w: 640, h: 360, d: 90, l: '720p', p: PAGE, f: 9999, rw: -4, rh: 99999999, vp: 5000 }, 1, PAGE, 0);
  h.message({ k: 'vid', u: 'https://cdn.example/c.mp4', w: 640, h: 360, d: 90, l: '', p: PAGE }, 1, PAGE, 0);
  await tick(20);
  const got = h.of('vid');
  assert.equal(got.length, 3);
  assert.deepEqual([got[0].f, got[0].rw, got[0].rh, got[0].vp, got[0].d], [27, 300, 170, 80, 25400]);
  assert.deepEqual([got[1].f, got[1].rw, got[1].rh, got[1].vp], [63, 0, 20000, 100]);
  assert.equal('f' in got[2], false, 'no facts were sent, none are invented');
});

test('RX3-V3: the played report and the element number are relayed as small whole numbers, nothing else of the element', async () => {
  const h = harness(standardApp());
  h.message({ k: 'vid', ev: 'played', el: 7, u: 'https://cdn.example/a.mp4', w: 1280, h: 720, d: 300, l: '', p: PAGE, f: 8, rw: 300, rh: 170, vp: 100, junk: 'x'.repeat(3000) }, 1, PAGE, 0);
  h.message({ k: 'vid', ev: 'weird', el: -4, u: 'https://cdn.example/b.mp4', w: 1, h: 1, d: 1, l: '', p: PAGE }, 1, PAGE, 0);
  h.message({ k: 'vid', el: 99999999999, u: 'https://cdn.example/c.mp4', w: 1, h: 1, d: 1, l: '', p: PAGE }, 1, PAGE, 0);
  await tick(20);
  const got = h.of('vid');
  assert.equal(got.length, 3);
  assert.equal(got[0].ev, 'played'); assert.equal(got[0].el, 7); assert.equal(got[0].junk, undefined);
  assert.equal(got[1].ev, undefined, 'only "played" is passed on'); assert.equal(got[1].el, 0);
  assert.equal(got[2].el, 1000000, 'clamped');
});

test('RX3-S2: the numbers for Diagnostics say how often each web request listener ran and how long it held the thread', async () => {
  const h = harness(standardApp());
  h.mainFrame(PAGE, { requestId: '1' });
  await ready(h);
  for (let i = 0; i < 3; i++) h.req('script', PLAIN, PAGE);
  h.listeners.headers({ statusCode: 200, url: 'https://cdn.example/movie.mp4', tabId: 1, documentUrl: PAGE, requestId: 'r9', responseHeaders: [{ name: 'content-type', value: 'video/mp4' }, { name: 'Content-Length', value: '5000000' }] });
  await h.flushTimers();
  const st = h.of('st'), last = st[st.length - 1];
  assert.ok(last, 'a state message went out');
  const req = /req (\d+) \((\d+) us\)/.exec(last.lt);
  assert.ok(req && Number(req[1]) >= 4, 'the request listener ran for the page and for every judged request: ' + last.lt);
  assert.match(last.lt, /rcv 1 \(\d+ us\)/, last.lt);
  assert.doesNotMatch(last.lt, /snd \d/, 'a listener that never ran is not mentioned');
  assert.doesNotMatch(st.map((m) => m.w).join(' | '), /\d+ us\)/, 'the numbers change at every report: they stay out of the text the app logs when it changes');
});

test('RX3-G1: the manifest declares the pop-up guard and the scriptlet library for the page world, every frame, from the first moment; nothing registers them at run time', () => {
  const m = JSON.parse(fs.readFileSync(path.join(SHIELDS, 'manifest.json'), 'utf8'));
  for (const f of ['guard-main.js', 'scriptlets-main.js']) {
    const e = m.content_scripts.filter((c) => c.js.includes(f));
    assert.equal(e.length, 1, f + ' is declared once');
    assert.equal(e[0].world, 'MAIN'); assert.equal(e[0].run_at, 'document_start'); assert.equal(e[0].all_frames, true);
    assert.equal(e[0].match_about_blank, true); assert.equal(e[0].match_origin_as_fallback, true);
    assert.deepEqual(e[0].matches, ['http://*/*', 'https://*/*']);
  }
  assert.ok(m.content_scripts[0].js.includes('content.js'), 'content.js comes first: it listens for the guard\'s ready event');
});

test('VP1: the full-screen film state is relayed as clamped numbers: the PAUSED flag is pa (p is the page address, as in every message), the number of the last carried-out command is ak, and the app\'s JSON answer comes back parsed', async () => {
  const h = harness((m) => (m.k === 'vst' ? '{"s":1,"c":[{"a":"pause","i":3}]}' : standardApp()(m)));
  h.mainFrame(PAGE, { requestId: '1' });
  await ready(h);
  const r = await h.message({ k: 'vst', t: 12.34, d: 60, b: 20, p: 1, e: 0, m: 1, v: 100, r: 1, rs: 4, ms: 0, l: 0, w: 640, h: 360, ak: 7, aw: 3, er: 1, lt: 3500, pc: 12, tf: 1200, df: 15, sh: 1, junk: 'x' }, 1, PAGE, 0);
  assert.deepEqual(r, { s: 1, c: [{ a: 'pause', i: 3 }] }, 'the answer reaches the page parsed');
  const got = h.of('vst')[0];
  assert.deepEqual(Object.keys(got).sort(), ['ak', 'aw', 'b', 'd', 'df', 'e', 'er', 'h', 'k', 'l', 'lt', 'm', 'ms', 'p', 'pa', 'pc', 'r', 'rs', 'sh', 't', 'tf', 'v', 'w'], 'only the known numbers go on: ' + JSON.stringify(got));
  assert.equal(got.pa, 1, 'paused'); assert.equal(got.ak, 7); assert.equal(got.aw, 3); assert.equal(got.er, 1); assert.equal(got.m, 1); assert.equal(got.t, 12.34);
  assert.equal(typeof got.p, 'string', 'p is the page address'); assert.ok(got.p.indexOf('example.com') >= 0, got.p);
  assert.equal(got.lt, 3500, 'ANS51e: how late the page timer ran'); assert.equal(got.pc, 12, 'the player script\'s own work'); assert.equal(got.tf, 1200, 'frames shown'); assert.equal(got.df, 15, 'frames dropped'); assert.equal(got.sh, 1, 'the film sits in a shadow tree');
  await h.message({ k: 'vst', t: 1, d: 60, lt: -5, pc: 1e12, tf: 'x', df: 4e9, sh: 2, p: 0 }, 1, PAGE, 0);
  const bad = h.of('vst')[1];
  assert.equal(bad.lt, 0, 'a negative lateness is nothing'); assert.equal(bad.pc, 0, 'a huge cost is clamped away'); assert.equal(bad.tf, 0); assert.equal(bad.df, 0); assert.equal(bad.sh, 0);
  await h.message({ k: 'vst', t: 1, d: 60, lt: 120000, p: 0 }, 1, PAGE, 0);
  assert.equal(h.of('vst')[2].lt, 0, 'a lateness of minutes is a sleeping device or a clock step, not a stall (review of alpha56)');
  const t = await h.message({ k: 'vst', t: 1e12, d: -2, b: -5, p: 0, ak: -3 }, 1, PAGE, 0);
  const g2 = h.of('vst')[3];
  assert.equal(g2.t, 0, 'clamped'); assert.equal(g2.d, -2, 'infinite is kept as -2'); assert.equal(g2.ak, 0); assert.equal(g2.pa, 0);
  assert.ok(t);
});

test('VP1: a length of 2^32 seconds (the live marker) and a huge position reach the app as they are (review F8)', async () => {
  const h = harness((m) => (m.k === 'vst' ? '{"s":0}' : standardApp()(m)));
  h.mainFrame(PAGE, { requestId: '1' });
  await ready(h);
  await h.message({ k: 'vst', t: 1700000000, d: 4294967296, b: 4294967296, p: 0 }, 1, PAGE, 0);
  const g = h.of('vst')[0];
  assert.equal(g.d, 4294967296); assert.equal(g.t, 1700000000); assert.equal(g.b, 4294967296);
  await h.message({ k: 'vst', t: 1e12, d: 1e12, b: -1, p: 0 }, 1, PAGE, 0);
  const g2 = h.of('vst')[1];
  assert.equal(g2.d, 0, 'still clamped beyond ten billion seconds'); assert.equal(g2.t, 0);
});

// ---------------------------------------------------------------- ANS52g: the video format policy (codec-main.js) and what it refused
const VPOLICY = '00|2048x1088@522000|2048x1088@522000|2048x1088@244800|-';

test('ANS52g: the video format policy rides in the fifth field of the page answer and comes back as `cx` in the guard answer', async () => {
  let policy = VPOLICY;
  const h = harness((m) => (m.k === 'cfg' ? `1:1:${ID}:1:${policy}` : standardApp()(m)));
  h.mainFrame(PAGE);
  await until(() => h.of('cfg').length >= 1, 'cfg');
  await tick(10);
  assert.equal((await h.message({ k: 'guard', h: 'movie.example' })).cx, VPOLICY);
  // a new policy (a Settings change) arrives with the next answer for any host; an empty one means none
  policy = '10|-|-|-|-';
  assert.equal((await h.message({ k: 'guard', h: 'other.example' })).cx, '10|-|-|-|-');
  policy = '';
  assert.equal((await h.message({ k: 'guard', h: 'third.example' })).cx, '', 'told "none"');
});

test('ANS52g: an answer of an older app (four fields) keeps the policy that was told before; nobody has a policy before the first answer', async () => {
  const h = harness((m) => (m.k === 'cfg' ? (m.h === 'old.example' ? `1:1:${ID}:1` : `1:1:${ID}:1:${VPOLICY}`) : standardApp()(m)));
  h.mainFrame(PAGE);
  await until(() => h.of('cfg').length >= 1, 'cfg');
  await tick(10);
  assert.equal((await h.message({ k: 'guard', h: 'new.example' })).cx, VPOLICY);
  assert.equal((await h.message({ k: 'guard', h: 'old.example' })).cx, VPOLICY, 'four fields: nothing new was said');
  const fresh = harness((m) => (m.k === 'cfg' ? `1:1:${ID}:1` : standardApp()(m)));
  fresh.mainFrame(PAGE);
  await until(() => fresh.of('cfg').length >= 1, 'cfg');
  assert.equal((await fresh.message({ k: 'guard', h: 'old.example' })).cx, '');
});

test('ANS52g: what the pages refused is summed, clamped and handed to the app as `vx` of the stats message; nothing is said until a page reports', async () => {
  const h = await onPage();
  await h.flushTimers();
  assert.ok(h.of('st').every((m) => m.vx === ''), 'no report, no text');
  h.message({ k: 'vcx', a: 7, s: 5, m: 1, f: 0, w: 'vp9 3840x2160@60' }, 1, PAGE, 0);
  h.message({ k: 'vcx', a: 3, s: 0, m: 0, f: 2, w: 'avc 1280x720@60' }, 1, PAGE, 3);
  h.message({ k: 'vcx', a: -5, s: 'x', m: 1e12, f: NaN, w: '<script>alert(1)</script> ' + 'z'.repeat(100) }, 1, PAGE, 0);
  await h.flushTimers();
  const st = h.of('st'), vx = st[st.length - 1].vx;
  assert.match(vx, /^questions 10 \(counted when something was refused\), refused 8: above the decoder's limit 5, not H\.264 1, above 30 fps 2; last: [a-z0-9 x@.]{0,30}$/);
  assert.ok(vx.length <= 240 && vx.indexOf('<') < 0);
});
