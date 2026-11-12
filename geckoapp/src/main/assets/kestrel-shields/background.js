'use strict';
// Kestrel Shields, background page. Sub-resource requests are decided HERE by engine.js (a port of the app's filter engine over an image of the app's lists), synchronously:
// no message to the app per request. The app only answers one question per page ("does blocking apply to this site?" = cfg) and hands over the list image in chunks (img).
// While no engine is loaded yet (start-up, a failed transfer, the compatibility switch in Diagnostics) every request is still sent to the app one by one - the older, slower path - so
// nothing is ever left unblocked. Documents are NOT handled here (the app's NavPolicy and malware screen do that). Design: research/D1-DESIGN.md.
var NATIVE = 'kestrel';
var E = KestrelEngine;
function send(o) { return browser.runtime.sendNativeMessage(NATIVE, JSON.stringify(o)); }

// ---------------------------------------------------------------- engine state
var engine = null;       // the loaded KestrelEngine instance (null until the first image arrived)
var engineGen = 0;       // generation of that image (the app's Engine.generation)
var pulling = false;
var pullFailures = 0, pullRetryAt = 0;
var engineOff = false;   // too many errors: back to the per-request path for the rest of the session
var stats = { checks: 0, us: 0, maxUs: 0, slow: 0, legacy: 0, legacyUs: 0, errors: 0, loadMs: 0, pulls: 0, last: '' };
var MODE_OFF = 0, MODE_ON = 1, MODE_LEGACY = 2;

// ---------------------------------------------------------------- per-page configuration ("cfg"): one question per main-frame navigation
var cfgMode = new Map();   // page host -> MODE_*
var cfgAt = new Map();     // page host -> when the answer arrived
var cfgFlags = new Map();  // page host -> flags from the app (1 = the pop-up and redirect guard applies)
var cfgWait = new Map();   // page host -> promise of the answer being fetched
var CFG_MAX = 300;
var CFG_TTL = 60000;       // a Shields switch changed in Settings reaches pages that are already open within this time (a switch on the page itself reloads it at once)
function askCfg(host) {
  var p = cfgWait.get(host);
  if (p) return p;
  p = send({ k: 'cfg', h: host }).then(function (r) {
    cfgWait.delete(host);
    var parts = ((typeof r === 'string') ? r : '').split(':'); // "<mode>:<generation>:<content id>"
    if (parts.length < 2) return MODE_OFF; // not an answer from the app: do not cache, never break the page
    var mode = parts[0] === '1' ? MODE_ON : (parts[0] === 'L' ? MODE_LEGACY : MODE_OFF);
    if (cfgMode.size >= CFG_MAX) { var old = cfgMode.keys().next().value; cfgMode.delete(old); cfgAt.delete(old); cfgFlags.delete(old); }
    // generation 0 = the app's lists are still loading: the answer is provisional (the site allow-list is not known yet), so it is asked again in a few seconds instead of after a minute
    cfgMode.set(host, mode); cfgAt.set(host, (parseInt(parts[1], 10) > 0) ? Date.now() : Date.now() - CFG_TTL + 3000);
    cfgFlags.set(host, parseInt(parts[3], 10) || 0);
    if (mode !== MODE_LEGACY) wantImage(parseInt(parts[1], 10) || 0, parts[2] || '');
    return mode;
  }, function () { cfgWait.delete(host); return MODE_OFF; }); // the app could not answer: fail open
  cfgWait.set(host, p);
  return p;
}

// ---------------------------------------------------------------- the list image: a saved copy at start-up, otherwise a transfer in chunks; replaced atomically
var engineId = ''; // content id of the loaded image (the app's ExtImage.contentId): equal ids mean equal bytes
function noop() {}
function b64Into(s, out, at) {
  var bin = atob(s), n = bin.length;
  for (var i = 0; i < n; i++) out[at + i] = bin.charCodeAt(i);
  return n;
}
function later(ms) { return new Promise(function (res) { setTimeout(res, ms); }); }

// The last image is kept in the extension's own IndexedDB, so a new launch has an engine before the first request instead of transferring the lists again.
function idb() {
  return new Promise(function (res, rej) {
    var r = indexedDB.open('kestrel-engine', 1);
    r.onupgradeneeded = function () { r.result.createObjectStore('img'); };
    r.onsuccess = function () { res(r.result); };
    r.onerror = function () { rej(r.error); };
  });
}
function idbDo(mode, f) {
  return idb().then(function (db) {
    return new Promise(function (res, rej) {
      var tx = db.transaction('img', mode), out = f(tx.objectStore('img'));
      tx.oncomplete = function () { db.close(); res(out && out.result); };
      tx.onerror = tx.onabort = function () { db.close(); rej(tx.error); };
    });
  });
}
var cacheLoaded = (function () {
  var t0 = performance.now();
  return idbDo('readonly', function (st) { return st.get('current'); }).then(function (rec) {
    if (!rec || !rec.buf || !rec.id) return;
    var e = E.create(rec.buf); // throws when damaged
    if (engine === null) { engine = e; engineId = rec.id; engineGen = -1; stats.loadMs = Math.round(performance.now() - t0); stats.last = 'saved copy ' + rec.id; }
  }).catch(function (e) {
    stats.last = 'saved copy unusable: ' + (e && e.message);
    idbDo('readwrite', function (st) { return st.delete('current'); }).catch(noop);
  });
})();

function wantImage(gen, id) { cacheLoaded.then(function () { wantImage2(gen, id); }); }
function wantImage2(gen, id) {
  if (gen <= 0 || pulling || engineOff) return;
  if (id && id === engineId) { engineGen = gen; return; } // the copy we hold IS the app's current lists
  if (gen === engineGen || Date.now() < pullRetryAt) return;
  pulling = true;
  var t0 = performance.now(), buf = null, got = 0, total = 0, i = 0, tFetch = 0, tDecode = 0, encMs = 0, tSend = 0, newId = '';
  function next() {
    tSend = performance.now();
    return send({ k: 'img', g: gen, i: i }).then(function (r) {
      tFetch += performance.now() - tSend;
      if (typeof r !== 'string' || r.length === 0) throw new Error('the app no longer has generation ' + gen);
      var body = r;
      if (i === 0) { // "<gen>:<total bytes>:<chunk bytes>:<encode ms>:<content id>:" then base64
        var h = r.split(':', 5);
        if (h.length < 5 || parseInt(h[0], 10) !== gen) throw new Error('image header');
        total = parseInt(h[1], 10); encMs = parseInt(h[3], 10) || 0; newId = h[4];
        if (!(total > 0 && total < 64 * 1024 * 1024)) throw new Error('image size ' + total);
        buf = new Uint8Array(total);
        body = r.substring(h[0].length + h[1].length + h[2].length + h[3].length + h[4].length + 5);
      }
      if (got + Math.floor(body.length * 3 / 4) > total + 3) throw new Error('image overrun');
      var td = performance.now();
      got += b64Into(body, buf, got);
      tDecode += performance.now() - td;
      i++;
      if (got < total) return later(0).then(next); // yield between chunks: this thread also serves page loads
      if (got !== total) throw new Error('image length');
      var tc = performance.now();
      engine = E.create(buf.buffer); engineGen = gen; engineId = newId; pullFailures = 0; stats.pulls++;
      stats.loadMs = Math.round(performance.now() - t0);
      stats.last = 'image ' + gen + ' from the app: ' + i + ' chunks, waiting for the app ' + Math.round(tFetch) + ' ms (its encode ' + encMs + ' ms), base64 decode ' + Math.round(tDecode) + ' ms, verify+warm-up ' + Math.round(performance.now() - tc) + ' ms';
      idbDo('readwrite', function (st) { return st.put({ id: newId, buf: buf.buffer }, 'current'); }).catch(noop); // keep it for the next launch
    });
  }
  next().catch(function (e) {
    pullFailures++; stats.last = 'image ' + gen + ' failed: ' + (e && e.message);
    if (pullFailures >= 5) engineOff = true; else pullRetryAt = Date.now() + 5000 * pullFailures;
  }).then(function () { pulling = false; reportStats(true); });
}

// ---------------------------------------------------------------- counters (blocked requests are added up and sent twice a second)
var pendingCounts = new Map(); // page URL -> [ads, trackers, malware, annoyance]
var countTimer = 0;
function countBlock(page, cat) {
  var a = pendingCounts.get(page);
  if (!a) { a = [0, 0, 0, 0]; pendingCounts.set(page, a); }
  a[cat & 3]++;
  if (!countTimer) countTimer = setTimeout(flushCounts, 500);
}
function flushCounts() {
  countTimer = 0;
  pendingCounts.forEach(function (a, p) { send({ k: 'cnt', p: p, c: a }).catch(function () {}); });
  pendingCounts.clear();
}

// ---------------------------------------------------------------- numbers for Diagnostics (at most one small message every 5 s while requests are being checked)
var statsDirty = false, statsTimer = 0;
function markStats() { // lazy timer: nothing is scheduled while no request is being judged (a permanent interval would wake the page on an idle tablet)
  statsDirty = true;
  if (!statsTimer) statsTimer = setTimeout(function () { statsTimer = 0; reportStats(false); }, 5000);
}
function reportStats(force) {
  if (!force && !statsDirty) return;
  statsDirty = false;
  send({ k: 'st', g: engineGen, n: stats.checks, us: Math.round(stats.us), mx: Math.round(stats.maxUs), sl: stats.slow, lg: stats.legacy, lu: Math.round(stats.legacyUs), er: stats.errors, ld: stats.loadMs, by: engine ? engine.bytes : 0, pl: stats.pulls, off: engineOff ? 1 : 0, w: (guardState + ', ' + guardBlocked + ' blocked | ' + stats.last).slice(0, 400) }).catch(function () {});
}

// ---------------------------------------------------------------- the legacy path: one question to the app per request (cached per page; used while no engine is loaded)
var verdicts = new Map();
var VERDICT_MAX = 4000;
var legacyHits = new Map();
var legacyTimer = 0;
function legacyCountLater(page) {
  legacyHits.set(page, (legacyHits.get(page) || 0) + 1);
  if (!legacyTimer) legacyTimer = setTimeout(function () {
    legacyTimer = 0;
    legacyHits.forEach(function (n, p) { send({ k: 'cnt', p: p, n: n }).catch(function () {}); });
    legacyHits.clear();
  }, 500);
}
function legacy(d, page) {
  stats.legacy++; markStats();
  var key = d.type + '|' + d.url + '|' + page;
  var hit = verdicts.get(key);
  if (hit !== undefined) {
    if (hit) { legacyCountLater(page); return { cancel: true }; }
    return {};
  }
  // Plain strings both ways (JSON text in, "1"/"0" out): the engine's object marshalling rejected structured replies ("Invalid event data for callback").
  var t0 = performance.now();
  return send({ k: 'req', u: d.url, t: d.type, p: page }).then(function (r) {
    stats.legacyUs += (performance.now() - t0) * 1000; // how long Gecko held this request waiting for the app
    var blocked = r === '1';
    if (verdicts.size >= VERDICT_MAX) verdicts.delete(verdicts.keys().next().value);
    verdicts.set(key, blocked);
    return blocked ? { cancel: true } : {};
  }, function () { return {}; }); // never break a page because the app could not answer
}

// ---------------------------------------------------------------- pop-up and forced-redirect guard
// guard-main.js (registered below, runs inside every page and frame) refuses windows the user did not ask for, and reports every real tap with the link it was on. The app decides about a page that sends
// its tab to another site right after such a tap (core/.../guard/JumpGuard): the decision needs the app's own touch times and happens BEFORE the navigation starts, so nothing of the page state is disturbed.
var guardState = 'page guard: starting';
var guardBlocked = 0;
function trimMap(m, max) { while (m.size > max) m.delete(m.keys().next().value); }
function guardAnswer(host, topHost) {
  var f = cfgFlags.get(host);
  if (f !== undefined) return Promise.resolve({ on: (f & 1) !== 0, top: topHost || host });
  return askCfg(host).then(function () { return { on: ((cfgFlags.get(host) || 0) & 1) !== 0, top: topHost || host }; });
}
browser.runtime.onMessage.addListener(function (m, sender) {
  if (!m) return undefined;
  var tabId = (sender && sender.tab) ? sender.tab.id : -1;
  if (m.k === 'guard') {
    var top = tabId >= 0 ? tabTop.get(tabId) : null;
    return guardAnswer((top && pageHost(top)) || m.h, (top && pageHost(top)) || '');
  }
  if (m.k === 'dom') { // a media address found in the page itself (content.js): handed to the app like a sniffed one, marked so the app can drop ad networks
    var dp = (tabId >= 0 && tabTop.get(tabId)) || m.p || '';
    send({ k: 'media', u: String(m.u || ''), c: '', l: -1, p: dp, f: m.p !== dp ? (m.p || '') : '', s: 'dom' }).catch(function () {});
    return undefined;
  }
  if (m.k === 'pop') {
    var d = String(m.d || ''), c = d.indexOf('|'), kind = c > 0 ? d.slice(0, c) : d, url = c > 0 ? d.slice(c + 1) : '';
    var page = (tabId >= 0 && tabTop.get(tabId)) || m.p || '';
    if (kind === 'tap' || kind === 'vmenu') { send({ k: 'pop', d: d, p: page }).catch(function () {}); return undefined; } // every real tap (the app judges a jump that follows) and a long press on a video (the app shows its menu): neither is a block
    guardBlocked++;
    send({ k: 'pop', d: d, p: page }).catch(function () {});
    markStats();
  }
  return undefined;
});
try {
  browser.contentScripts.register({
    matches: ['http://*/*', 'https://*/*'], js: [{ file: 'guard-main.js' }], runAt: 'document_start', allFrames: true, matchAboutBlank: true, matchOriginAsFallback: true, world: 'MAIN',
  }).then(function () { guardState = 'page guard on'; }, function (e) { guardState = 'page guard FAILED: ' + (e && e.message); });
} catch (e) { guardState = 'page guard FAILED: ' + (e && e.message); }

// ---------------------------------------------------------------- the request listener
var hostOfPage = new Map(); // page URL -> host (every request of a page names the same page)
function pageHost(url) {
  var h = hostOfPage.get(url);
  if (h === undefined) {
    h = E.hostOf(url);
    if (hostOfPage.size >= 200) hostOfPage.delete(hostOfPage.keys().next().value);
    hostOfPage.set(url, h);
  }
  return h;
}
function topPage(d) {
  if (d.frameAncestors && d.frameAncestors.length) return d.frameAncestors[d.frameAncestors.length - 1].url;
  return d.documentUrl || d.originUrl || '';
}
function decide(d, page, host, mode) {
  if (mode === MODE_OFF) return {};
  if (mode === MODE_ON && engine !== null && !engineOff) {
    try {
      var t0 = performance.now();
      var v = engine.check(d.url, host, E.typeMask(d.type));
      var us = (performance.now() - t0) * 1000;
      stats.checks++; stats.us += us; if (us > stats.maxUs) stats.maxUs = us; if (us > 2000) stats.slow++;
      markStats();
      if ((v & 1) !== 0) { countBlock(page, (v >>> 2) & 3); return { cancel: true }; }
      return {};
    } catch (e) {
      stats.errors++; stats.last = 'check failed: ' + (e && e.message); markStats();
      if (stats.errors >= 20) { engineOff = true; reportStats(true); }
      return {}; // fail open and loud
    }
  }
  return legacy(d, page);
}

browser.webRequest.onBeforeRequest.addListener(function (d) {
  if (d.type === 'main_frame') {
    sniffed.delete(d.tabId); tabTop.set(d.tabId, d.url); if (tabTop.size > 60) tabTop.delete(tabTop.keys().next().value); // a new page in this tab: what it loads is news again
    verdicts.clear(); // legacy verdicts are only worth keeping while ONE page loads
    var h = pageHost(d.url);
    if (h) { cfgMode.delete(h); cfgAt.delete(h); askCfg(h); } // a Shields switch may have changed: ask again. Not awaited - the answer beats the document's first bytes over a hotspot
    return {};
  }
  var page = topPage(d);
  if (!page) return {};
  var host = pageHost(page);
  if (!host) return {};
  var mode = cfgMode.get(host);
  if (mode === undefined) return askCfg(host).then(function (m) { return decide(d, page, host, m); }); // asked once per page, before its first sub-resource is judged
  if (Date.now() - cfgAt.get(host) > CFG_TTL) askCfg(host); // refresh in the background; this request still uses the answer we have
  return decide(d, page, host, mode);
}, { urls: ['http://*/*', 'https://*/*', 'ws://*/*', 'wss://*/*'] }, ['blocking']);

// ---------------------------------------------------------------- element hiding: still asked of the app, once per frame, answered from a cache for repeat visits
var cssCache = new Map();
browser.runtime.onMessage.addListener(function (m) {
  if (!m || m.k !== 'css' || !m.h) return undefined;
  var key = (m.top ? 't|' : 'f|') + m.h; // the top document and an iframe of the same host get different CSS
  var cached = cssCache.get(key);
  if (cached !== undefined) return Promise.resolve(cached);
  return send({ k: 'css', h: m.h, top: !!m.top }).then(function (r) {
    var css = (typeof r === 'string') ? r : '';
    if (cssCache.size >= 200) cssCache.delete(cssCache.keys().next().value);
    cssCache.set(key, css);
    return css;
  }, function () { return ''; });
});

// ---- video / audio sniffer: tells the app which media files and playlists a page loads, so a long press can offer "Download video".
// Passive: nothing is fetched here and nothing is blocked. One native message per NEW candidate (a player's flurry of Range requests for the same file is one).
var MEDIA_TYPES = ['media', 'xmlhttprequest', 'other', 'object', 'sub_frame', 'main_frame'];
var MEDIA_EXT = /\.(mp4|m4v|mov|webm|mkv|avi|wmv|flv|3gp|mpg|mpeg|m4a|mp3|aac|ogg|oga|ogv|opus|wav|flac|m3u8|m3u|mpd)(?:$|[?#])/i;
var MEDIA_CT = /^(video|audio)\/|mpegurl|dash\+xml|\/mp4|\/ogg|matroska/;
var SEGMENT = /\.(ts|m4s|cmfv|cmfa|cmft|m4f|mts)(?:$|[?#])/i;
var tabTop = new Map(); // tabId -> the address of the page the tab is on (an embedded player's request names its own frame, not this)
var sniffed = new Map(); // tabId -> Set of "url|sizeKnown" already reported for the tab's CURRENT page (a reload or another page reports the same file again)
var SNIFFED_TABS = 40;
var SNIFFED_PER_TAB = 300;

browser.webRequest.onHeadersReceived.addListener(function (d) {
  if (d.statusCode !== 200 && d.statusCode !== 206) return;
  var url = d.url;
  if (SEGMENT.test(url)) return;
  var ct = '', len = -1, total = -1, h = d.responseHeaders || [];
  for (var i = 0; i < h.length; i++) {
    var n = h[i].name.toLowerCase();
    if (n === 'content-type') ct = (h[i].value || '').toLowerCase();
    else if (n === 'content-length') len = parseInt(h[i].value, 10);
    else if (n === 'content-range') { var m = /\/(\d+)\s*$/.exec(h[i].value || ''); if (m) total = parseInt(m[1], 10); }
  }
  if (!MEDIA_CT.test(ct) && !MEDIA_EXT.test(url)) return;
  if (ct.indexOf('video/mp2t') === 0 || ct.indexOf('iso.segment') > 0) return;
  var size = total >= 0 ? total : (d.statusCode === 200 && len >= 0 ? len : -1);
  var key = url + '|' + (size >= 0 ? 1 : 0);
  var seen = sniffed.get(d.tabId);
  if (!seen) {
    if (sniffed.size >= SNIFFED_TABS) sniffed.delete(sniffed.keys().next().value);
    seen = new Set(); sniffed.set(d.tabId, seen);
  }
  if (seen.has(key)) return;
  if (seen.size >= SNIFFED_PER_TAB) seen.delete(seen.values().next().value);
  seen.add(key);
  var page = tabTop.get(d.tabId) || ((d.frameAncestors && d.frameAncestors.length) ? d.frameAncestors[d.frameAncestors.length - 1].url : (d.documentUrl || d.originUrl || ''));
  send({ k: 'media', u: url, c: ct, l: size, p: page, f: d.documentUrl || '' }).catch(function () {});
}, { urls: ['http://*/*', 'https://*/*'], types: MEDIA_TYPES }, ['responseHeaders']);

// What the page's own <video> says about the file it plays (picture size, length, the label next to a <source>): relayed by content.js.
browser.runtime.onMessage.addListener(function (m) {
  if (!m || m.k !== 'vid' || !m.u) return undefined;
  send({ k: 'vid', u: m.u, w: m.w | 0, h: m.h | 0, d: Math.round((m.d || 0) * 1000), l: m.l || '', p: m.p || '' }).catch(function () {});
  return undefined;
});
