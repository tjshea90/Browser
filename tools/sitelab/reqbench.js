'use strict';
// RVW5: what the extension's three per-request listeners (onBeforeRequest, onBeforeSendHeaders, onHeadersReceived of kestrel-shields/background.js) cost on a REAL request stream.
// Headless Chromium loads each page and records every request / response (url, type, frame chain, request + response headers); the recording is then replayed ROUNDS times into a fresh copy of the
// real background.js (same fake browser as realfinder.js) and the time of each listener call is summed per listener. A measuring tool, not a test: needs the internet; prints microseconds on THIS
// machine (the tablet's four A53 cores are roughly 8-15 times slower for JS, so read the ratios and the ranking, not the absolute figure). Optional --cpu-prof (node flag) shows the hot functions.
//   usage: NODE_PATH=$(npm root -g) node [--cpu-prof --cpu-prof-dir D] tools/sitelab/reqbench.js [--rounds 200] [--save rec.json | --load rec.json] [url ...]
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const SHIELDS = path.join(__dirname, '..', '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const SRC = ['engine.js', 'redirects.js', 'hlscap.js', 'background.js'].map((f) => fs.readFileSync(path.join(SHIELDS, f), 'utf8')).join('\n');
const MEDIA_TYPES = ['media', 'xmlhttprequest', 'other', 'object', 'sub_frame', 'main_frame']; // keep equal to background.js
const TYPE = { document: 'sub_frame', xhr: 'xmlhttprequest', fetch: 'xmlhttprequest', media: 'media', script: 'script', stylesheet: 'stylesheet', image: 'image', font: 'font', other: 'other', websocket: 'websocket', ping: 'ping', eventsource: 'other', manifest: 'other', texttrack: 'other' };

function harness() {
  const listeners = {}, timers = [];
  const ctx = {
    console: { log() {}, warn() {}, error() {}, info() {}, debug() {} }, Promise, Map, Set, Uint8Array, ArrayBuffer, Int32Array, Math, Date, JSON, parseInt, Object, Error, RegExp, String, Array, TextDecoder, TextEncoder,
    performance: { now: () => Number(process.hrtime.bigint()) / 1e6 },
    atob: (s) => Buffer.from(s, 'base64').toString('latin1'),
    setTimeout: (f, ms) => { if (!ms || ms < 100) { setImmediate(f); return -1; } timers.push(f); return timers.length; },
    clearTimeout: () => {},
    indexedDB: { open() { const r = {}; setImmediate(() => { r.error = new Error('no idb'); if (r.onerror) r.onerror(); }); return r; } },
    browser: {
      runtime: { getURL: (p) => 'moz-extension://x/' + p, sendNativeMessage: (a, text) => { const m = JSON.parse(text); return Promise.resolve(m.k === 'cfg' ? '0:0:x' : ''); }, onMessage: { addListener: () => {} } },
      tabs: { sendMessage: () => Promise.resolve() },
      webRequest: {
        onBeforeRequest: { addListener: (f) => { listeners.before = f; } },
        onBeforeSendHeaders: { addListener: (f) => { listeners.sendHeaders = f; } },
        onHeadersReceived: { addListener: (f, filter, spec) => { if (!(spec && spec.indexOf('blocking') >= 0)) listeners.headers = f; } },
        filterResponseData: () => { throw new Error('no stream filters here'); },
        onBeforeRedirect: { addListener: () => {} }, onErrorOccurred: { addListener: () => {} }, onCompleted: { addListener: () => {} },
      },
    },
  };
  vm.createContext(ctx);
  vm.runInContext(SRC, ctx);
  return listeners;
}

async function record(urls) {
  const { chromium } = require('playwright');
  const browser = await chromium.launch();
  const rec = [];
  for (const url of urls) {
    const ctx = await browser.newContext({ viewport: { width: 1024, height: 700 }, userAgent: 'Mozilla/5.0 (Android 10; Mobile; rv:160.0) Gecko/160.0 Firefox/160.0' });
    const page = await ctx.newPage();
    const ids = new Map(); const idOf = (fr) => { if (!ids.has(fr)) ids.set(fr, ids.size); return ids.get(fr); };
    let rid = 0;
    page.on('response', async (resp) => {
      try {
        const rq = resp.request(), fr = rq.frame();
        const type = rq.isNavigationRequest() && fr === page.mainFrame() ? 'main_frame' : (TYPE[rq.resourceType()] || 'other');
        const anc = []; for (let p = fr.parentFrame(); p; p = p.parentFrame()) anc.push({ url: p.url(), frameId: idOf(p) });
        const reqH = await rq.allHeaders(); const rh = await resp.allHeaders();
        rec.push({ base: { requestId: String(++rid) + '@' + rec.length, url: rq.url(), method: rq.method(), type, tabId: 1, frameId: idOf(fr), parentFrameId: fr.parentFrame() ? idOf(fr.parentFrame()) : -1, documentUrl: fr.url(), originUrl: fr.url(), frameAncestors: anc },
          reqH: Object.keys(reqH).map((k) => ({ name: k, value: reqH[k] })), status: resp.status(), resH: Object.keys(rh).map((k) => ({ name: k, value: rh[k] })) });
      } catch (e) { /* page closed */ }
    });
    try { await page.goto(url, { timeout: 30000, waitUntil: 'load' }); } catch (e) { /* partial pages are still a stream */ }
    await page.waitForTimeout(4000);
    await ctx.close();
  }
  await browser.close();
  return rec;
}

function replay(rec, rounds) {
  const h = harness();
  const t = { before: 0, sendHeaders: 0, headers: 0 }, n = { before: 0, sendHeaders: 0, headers: 0 };
  const hr = () => Number(process.hrtime.bigint());
  for (let r = 0; r < rounds; r++) {
    for (const e of rec) {
      const base = Object.assign({}, e.base, { requestId: e.base.requestId + '/' + r });
      let a = hr(); try { h.before && h.before(base); } catch (x) { /* a listener may throw on a fake */ } t.before += hr() - a; n.before++;
      const sh = Object.assign({}, base, { requestHeaders: e.reqH });
      a = hr(); try { h.sendHeaders && h.sendHeaders(sh); } catch (x) { /* ignore */ } t.sendHeaders += hr() - a; n.sendHeaders++;
      if (MEDIA_TYPES.indexOf(base.type) >= 0 && /^https?:/i.test(base.url)) {
        const hd = Object.assign({}, base, { statusCode: e.status, responseHeaders: e.resH });
        a = hr(); try { h.headers && h.headers(hd); } catch (x) { /* ignore */ } t.headers += hr() - a; n.headers++;
      }
    }
  }
  return { t, n };
}

(async () => {
  const args = process.argv.slice(2);
  let rounds = 200, save = '', load = ''; const urls = [];
  for (let i = 0; i < args.length; i++) { if (args[i] === '--rounds') rounds = +args[++i]; else if (args[i] === '--save') save = args[++i]; else if (args[i] === '--load') load = args[++i]; else urls.push(args[i]); }
  const rec = load ? JSON.parse(fs.readFileSync(load, 'utf8')) : await record(urls);
  if (save) fs.writeFileSync(save, JSON.stringify(rec));
  replay(rec, 5); // warm-up: the JIT and the first-seen maps
  const { t, n } = replay(rec, rounds);
  const per = (k) => (n[k] ? (t[k] / n[k] / 1000).toFixed(2) : '-');
  console.log('requests recorded: ' + rec.length + ', rounds: ' + rounds);
  console.log('us per call: onBeforeRequest ' + per('before') + ' (' + n.before + ' calls), onBeforeSendHeaders ' + per('sendHeaders') + ' (' + n.sendHeaders + '), onHeadersReceived ' + per('headers') + ' (' + n.headers + ')');
  const page = (t.before + t.sendHeaders + t.headers) / rounds / 1e6;
  console.log('ms of listener time per page load of this stream: ' + page.toFixed(2) + ' (this machine; the tablet is roughly 8-15x slower)');
})();
