'use strict';
// RVW4: the video finder's NETWORK half (geckoapp/src/main/assets/kestrel-shields/background.js) driven by REAL pages instead of modelled ones.
// Headless Chromium (Playwright, preinstalled globally) opens each page, presses play on every <video>, and every request / response is replayed into background.js running in a fake browser
// (the same vm harness idea as web-tests/background.test.js). What background.js would tell the app (`media` = a certain address, `maybe` = an address the app probes) is printed per page.
// This is a measuring tool, not a test: it needs the internet and the sites change. It reads nothing from the owner's tablet and writes nothing outside OUT.
//   usage: NODE_PATH=$(npm root -g) node tools/sitelab/realfinder.js [--wait ms] [--json out.json] url [url ...]
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const { chromium } = require('playwright');

const SHIELDS = path.join(__dirname, '..', '..', 'geckoapp', 'src', 'main', 'assets', 'kestrel-shields');
const SRC = ['engine.js', 'redirects.js', 'hlscap.js', 'background.js'].map((f) => fs.readFileSync(path.join(SHIELDS, f), 'utf8')).join('\n');

function harness() {
  const sent = [], listeners = {}, timers = [];
  const ctx = {
    console: { log() {}, warn() {}, error() {}, info() {}, debug() {} }, Promise, Map, Set, Uint8Array, ArrayBuffer, Int32Array, Math, Date, JSON, parseInt, Object, Error, RegExp, String, Array, TextDecoder, TextEncoder,
    performance: { now: () => Number(process.hrtime.bigint()) / 1e6 },
    atob: (s) => Buffer.from(s, 'base64').toString('latin1'),
    setTimeout: (f, ms) => { if (!ms || ms < 100) { setImmediate(f); return -1; } timers.push(f); return timers.length; },
    clearTimeout: () => {},
    indexedDB: { open() { const r = {}; setImmediate(() => { r.error = new Error('no idb'); if (r.onerror) r.onerror(); }); return r; } },
    browser: {
      runtime: {
        getURL: (p) => 'moz-extension://x/' + p,
        sendNativeMessage: (a, text) => { const m = JSON.parse(text); sent.push(m); return Promise.resolve(m.k === 'cfg' ? '0:0:x' : ''); },
        onMessage: { addListener: (f) => { (listeners.message = listeners.message || []).push(f); } },
      },
      tabs: { sendMessage: () => Promise.resolve() },
      webRequest: {
        onBeforeRequest: { addListener: (f) => { listeners.before = f; } },
        onBeforeSendHeaders: { addListener: (f) => { listeners.sendHeaders = f; } },
        onHeadersReceived: { addListener: (f, filter, spec) => { if (!(spec && spec.indexOf('blocking') >= 0)) listeners.headers = f; } },
        filterResponseData: () => { throw new Error('no stream filters here'); },
        onBeforeRedirect: { addListener: (f) => { listeners.redirect = f; } },
        onErrorOccurred: { addListener: (f) => { listeners.error = f; } },
        onCompleted: { addListener: (f) => { listeners.completed = f; } },
      },
    },
  };
  vm.createContext(ctx);
  vm.runInContext(SRC, ctx);
  return { sent, listeners };
}

const MEDIA_TYPES = ['media', 'xmlhttprequest', 'other', 'object', 'sub_frame', 'main_frame']; // keep equal to background.js
const TYPE = { document: 'sub_frame', xhr: 'xmlhttprequest', fetch: 'xmlhttprequest', media: 'media', script: 'script', stylesheet: 'stylesheet', image: 'image', font: 'font', other: 'other', websocket: 'websocket', ping: 'ping' };

async function probe(browser, url, waitMs) {
  const h = harness();
  const ctx = await browser.newContext({ viewport: { width: 1024, height: 700 }, userAgent: 'Mozilla/5.0 (Android 10; Mobile; rv:160.0) Gecko/160.0 Firefox/160.0' });
  const page = await ctx.newPage();
  const ids = new Map();
  const idOf = (fr) => { if (!ids.has(fr)) ids.set(fr, ids.size); return ids.get(fr); };
  const ancestors = (fr) => { const a = []; for (let p = fr.parentFrame(); p; p = p.parentFrame()) a.push({ url: p.url(), frameId: idOf(p) }); return a; };
  let n = 0, rid = 0;
  const seen = new Map();
  page.on('response', async (resp) => {
    try {
      const rq = resp.request(), fr = rq.frame();
      const type = rq.isNavigationRequest() && fr === page.mainFrame() ? 'main_frame' : (TYPE[rq.resourceType()] || 'other');
      const base = { requestId: String(++rid), url: rq.url(), method: rq.method(), type, tabId: 1, frameId: idOf(fr), parentFrameId: fr.parentFrame() ? idOf(fr.parentFrame()) : -1, documentUrl: fr.url(), originUrl: fr.url(), frameAncestors: ancestors(fr) };
      const reqH = await rq.allHeaders(); const rh = await resp.allHeaders();
      n++;
      h.listeners.before && h.listeners.before(base);
      h.listeners.sendHeaders && h.listeners.sendHeaders(Object.assign({}, base, { requestHeaders: Object.keys(reqH).map((k) => ({ name: k, value: reqH[k] })) }));
      const responseHeaders = Object.keys(rh).map((k) => ({ name: k, value: rh[k] }));
      // Firefox only calls the sniffer for the resource types it registered for (background.js MEDIA_TYPES)
      if (MEDIA_TYPES.indexOf(type) >= 0 && /^https?:/i.test(rq.url())) h.listeners.headers && h.listeners.headers(Object.assign({}, base, { statusCode: resp.status(), responseHeaders }));
      if (/mpegurl|dash\+xml|video\/|audio\//i.test(rh['content-type'] || '') || /\.(m3u8|mpd|mp4|webm|m4a|mp3)(\?|$)/i.test(rq.url())) seen.set(rq.url(), (rh['content-type'] || '') + ' ' + (rh['content-length'] || '?'));
    } catch (e) { /* a page may close while answers still arrive */ }
  });
  // the DOM half: the real content scripts run in every frame against a fake `browser`; what they report to the background page is collected
  await ctx.addInitScript(() => {
    window.__ksent = [];
    const noop = () => {};
    window.browser = { runtime: { sendMessage: (m) => { try { window.__ksent.push(JSON.parse(JSON.stringify(m))); } catch (e) { /* ignore */ } return Promise.resolve(undefined); }, onMessage: { addListener: noop }, getURL: (p) => 'moz-extension://x/' + p }, i18n: {} };
  });
  const DOM_SCRIPTS = ['vfind.js', 'content.js'].map((f) => fs.readFileSync(path.join(SHIELDS, f), 'utf8'));
  let nav = 'ok';
  try { await page.goto(url, { timeout: 30000, waitUntil: 'domcontentloaded' }); } catch (e) { nav = 'goto: ' + e.message.slice(0, 80); }
  await page.waitForTimeout(2500);
  for (const fr of page.frames()) { try { await fr.evaluate(() => { document.querySelectorAll('video').forEach((v) => { v.muted = true; const p = v.play(); if (p && p.catch) p.catch(() => {}); }); }); } catch (e) { /* cross-origin frame gone */ } }
  try { await page.mouse.click(512, 350); } catch (e) { /* nothing to click */ }
  await page.waitForTimeout(waitMs);
  const dom = [];
  for (const fr of page.frames()) {
    try { dom.push(...(await fr.evaluate(() => Array.from(document.querySelectorAll('video,audio')).map((v) => ({ src: (v.currentSrc || v.src || '').slice(0, 160), w: v.videoWidth, d: Math.round(v.duration || 0), rs: v.readyState })))) ); } catch (e) { /* frame gone */ }
  }
  const domSent = [];
  for (const fr of page.frames()) {
    try {
      for (const code of DOM_SCRIPTS) await fr.evaluate(code);
      await fr.waitForTimeout(1500);
      domSent.push(...(await fr.evaluate(() => (window.__ksent || []).filter((m) => ['gen','css','guard','scroll','bk','bf','warm','sl','proc','probe'].indexOf(m.k) < 0)))); 
    } catch (e) { /* frame gone or a page that refuses the scripts */ }
  }
  const title = await page.title().catch(() => '');
  await ctx.close();
  const found = h.sent.filter((m) => m.k === 'media' || m.k === 'maybe').map((m) => ({ k: m.k, u: m.u, c: m.c, l: m.l, ty: m.ty }));
  return { url, title: title.slice(0, 70), nav, requests: n, videoElements: dom, mediaLike: [...seen.entries()].slice(0, 12), told: found, domSent };
}

(async () => {
  const args = process.argv.slice(2);
  let wait = 9000, out = null; const urls = [];
  for (let i = 0; i < args.length; i++) { if (args[i] === '--wait') wait = +args[++i]; else if (args[i] === '--json') out = args[++i]; else urls.push(args[i]); }
  const browser = await chromium.launch();
  const all = [];
  for (const u of urls) {
    const r = await probe(browser, u, wait).catch((e) => ({ url: u, error: String(e).slice(0, 120) }));
    all.push(r);
    console.log('\n== ' + u + '\n   title: ' + (r.title || '') + ' | nav: ' + (r.nav || r.error) + ' | requests: ' + (r.requests || 0));
    for (const v of r.videoElements || []) console.log('   <video> ' + v.src + ' w=' + v.w + ' d=' + v.d + ' rs=' + v.rs);
    for (const m of r.told || []) console.log('   TOLD ' + m.k + ' ' + (m.u || '').slice(0, 150) + ' | ' + (m.c || '') + ' | ' + m.l);
    for (const m of r.domSent || []) console.log('   DOM ' + m.k + ' ' + JSON.stringify(m).slice(0, 220));
    if (!(r.told || []).length) for (const [k, v] of r.mediaLike || []) console.log('   (seen on the wire, NOT told) ' + k.slice(0, 130) + ' | ' + v);
  }
  await browser.close();
  if (out) fs.writeFileSync(out, JSON.stringify(all, null, 1));
})();
