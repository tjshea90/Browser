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
const SRC = fs.readFileSync(path.join(SHIELDS, 'engine.js'), 'utf8') + '\n' + fs.readFileSync(path.join(SHIELDS, 'background.js'), 'utf8');
const IMAGE = fs.readFileSync(path.join(__dirname, 'fixtures', 'mini.kex'));
const ID = '1.111.1700000000000.1.2.3.4.5.6';
// objects built inside the vm context have another Object.prototype: compare them as JSON
const eq = (a, b, msg) => assert.equal(JSON.stringify(a), JSON.stringify(b), msg);
const tick = (n) => new Promise((r) => setTimeout(r, n || 5));
async function until(f, what) { for (let i = 0; i < 400; i++) { if (f()) return; await tick(5); } assert.fail('timed out waiting for ' + what); }

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
  const sent = [], timers = [], listeners = {}, clock = { now: 1700000000000 };
  const ctx = {
    console, Promise, Map, Set, Uint8Array, ArrayBuffer, Int32Array, Math, Date: { now: () => clock.now }, JSON, parseInt, Object, Error, RegExp, String, Array,
    performance: { now: () => Number(process.hrtime.bigint()) / 1e6 },
    atob: (s) => Buffer.from(s, 'base64').toString('latin1'),
    // short timers (the chunk-transfer yields) run at once; the 500 ms / 5 s batching timers wait until the test runs them
    setTimeout: (f, ms) => { if (!ms || ms < 100) { setImmediate(f); return 1; } timers.push(f); return timers.length; },
    indexedDB: fakeIdb(opts.idb || new Map()),
    browser: {
      runtime: {
        sendNativeMessage: (app_, text) => { const m = JSON.parse(text); sent.push(m); return Promise.resolve(app(m)); },
        onMessage: { addListener: (f) => { (listeners.message = listeners.message || []).push(f); } },
      },
      webRequest: {
        onBeforeRequest: { addListener: (f) => { listeners.before = f; } },
        onHeadersReceived: { addListener: (f) => { listeners.headers = f; } },
      },
    },
  };
  vm.createContext(ctx);
  vm.runInContext(SRC, ctx);
  const req = (type, url, page) => listeners.before({ type, url, tabId: 1, frameId: 0, documentUrl: page, frameAncestors: [{ url: page, frameId: 0 }] });
  return {
    sent, ctx, listeners, clock,
    mainFrame: (url, extra) => listeners.before(Object.assign({ type: 'main_frame', url, tabId: 1, frameId: 0 }, extra || {})),
    /** a message from a content script (the first listener that answers wins, like the browser) */
    message: (m, tabId) => { for (const f of listeners.message || []) { const r = f(m, { tab: tabId === undefined ? { id: 1 } : (tabId < 0 ? undefined : { id: tabId }) }); if (r !== undefined) return r; } return undefined; },
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
  eq(await h.message({ k: 'guard', h: 'movie.example' }), { on: true });
  const off = await onPage(guardApp(0));
  eq(await off.message({ k: 'guard', h: 'movie.example' }), { on: false });
  h.message({ k: 'pop', d: 'open|' + OTHER, p: 'https://frame.example/embed' });
  await tick(10);
  const pop = h.of('pop');
  assert.equal(pop.length, 1);
  assert.equal(pop[0].d, 'open|' + OTHER);
  assert.equal(pop[0].p, PAGE, 'attributed to the tab\'s page, not to the embedded player frame');
});
