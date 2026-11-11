'use strict';
// Every sub-resource request asks the app (native messaging) whether Kestrel's filter engine blocks it; decisions are cached so a page that
// repeats a request, and repeat visits, cost no further round trips. Documents are NOT handled here (the app's NavPolicy and malware screen do that).
var NATIVE = 'kestrel';
var verdicts = new Map();
var VERDICT_MAX = 4000;
var cssCache = new Map();

function topPage(d) {
  if (d.frameAncestors && d.frameAncestors.length) return d.frameAncestors[d.frameAncestors.length - 1].url;
  return d.documentUrl || d.originUrl || '';
}

browser.webRequest.onBeforeRequest.addListener(function (d) {
  if (d.type === 'main_frame') return {};
  var page = topPage(d);
  if (!page) return {};
  var key = d.type + '|' + d.url + '|' + page;
  var hit = verdicts.get(key);
  if (hit !== undefined) {
    if (hit) { browser.runtime.sendNativeMessage(NATIVE, JSON.stringify({ k: 'cnt', p: page })).catch(function () {}); return { cancel: true }; }
    return {};
  }
  // Plain strings both ways (JSON text in, "1"/"0" out): the engine's object marshalling rejected structured replies ("Invalid event data for callback").
  return browser.runtime.sendNativeMessage(NATIVE, JSON.stringify({ k: 'req', u: d.url, t: d.type, p: page })).then(function (r) {
    var blocked = r === '1';
    if (verdicts.size >= VERDICT_MAX) verdicts.delete(verdicts.keys().next().value);
    verdicts.set(key, blocked);
    return blocked ? { cancel: true } : {};
  }, function () { return {}; }); // never break a page because the app could not answer
}, { urls: ['http://*/*', 'https://*/*', 'ws://*/*', 'wss://*/*'] }, ['blocking']);

browser.runtime.onMessage.addListener(function (m) {
  if (!m || m.k !== 'css' || !m.h) return undefined;
  var cached = cssCache.get(m.h);
  if (cached !== undefined) return Promise.resolve(cached);
  return browser.runtime.sendNativeMessage(NATIVE, JSON.stringify({ k: 'css', h: m.h, top: !!m.top })).then(function (r) {
    var css = (typeof r === 'string') ? r : '';
    if (cssCache.size >= 200) cssCache.delete(cssCache.keys().next().value);
    cssCache.set(m.h, css);
    return css;
  }, function () { return ''; });
});

// ---- video / audio sniffer: tells the app which media files and playlists a page loads, so a long press can offer "Download video".
// Passive: nothing is fetched here and nothing is blocked. One native message per NEW candidate (a player's flurry of Range requests for the same file is one).
var MEDIA_TYPES = ['media', 'xmlhttprequest', 'other', 'object', 'sub_frame', 'main_frame'];
var MEDIA_EXT = /\.(mp4|m4v|mov|webm|mkv|avi|wmv|flv|3gp|mpg|mpeg|m4a|mp3|aac|ogg|oga|ogv|opus|wav|flac|m3u8|m3u|mpd)(?:$|[?#])/i;
var MEDIA_CT = /^(video|audio)\/|mpegurl|dash\+xml|\/mp4|\/ogg|matroska/;
var SEGMENT = /\.(ts|m4s|cmfv|cmfa|cmft|m4f|mts)(?:$|[?#])/i;
var sniffed = new Map();
var SNIFFED_MAX = 800;

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
  if (sniffed.has(key)) return;
  if (sniffed.size >= SNIFFED_MAX) sniffed.delete(sniffed.keys().next().value);
  sniffed.set(key, true);
  var page = (d.frameAncestors && d.frameAncestors.length) ? d.frameAncestors[d.frameAncestors.length - 1].url : (d.documentUrl || d.originUrl || '');
  browser.runtime.sendNativeMessage(NATIVE, JSON.stringify({ k: 'media', u: url, c: ct, l: size, p: page, f: d.documentUrl || '' })).catch(function () {});
}, { urls: ['http://*/*', 'https://*/*'], types: MEDIA_TYPES }, ['responseHeaders']);

// What the page's own <video> says about the file it plays (picture size, length, the label next to a <source>): relayed by content.js.
browser.runtime.onMessage.addListener(function (m) {
  if (!m || m.k !== 'vid' || !m.u) return undefined;
  browser.runtime.sendNativeMessage(NATIVE, JSON.stringify({ k: 'vid', u: m.u, w: m.w | 0, h: m.h | 0, d: Math.round((m.d || 0) * 1000), l: m.l || '', p: m.p || '' })).catch(function () {});
  return undefined;
});
