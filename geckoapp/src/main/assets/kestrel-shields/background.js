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
    if (hit) { browser.runtime.sendNativeMessage(NATIVE, { k: 'cnt', p: page }).catch(function () {}); return { cancel: true }; }
    return {};
  }
  return browser.runtime.sendNativeMessage(NATIVE, { k: 'req', u: d.url, t: d.type, p: page }).then(function (r) {
    var blocked = !!(r && r.b);
    if (verdicts.size >= VERDICT_MAX) verdicts.delete(verdicts.keys().next().value);
    verdicts.set(key, blocked);
    return blocked ? { cancel: true } : {};
  }, function () { return {}; }); // never break a page because the app could not answer
}, { urls: ['http://*/*', 'https://*/*', 'ws://*/*', 'wss://*/*'] }, ['blocking']);

browser.runtime.onMessage.addListener(function (m) {
  if (!m || m.k !== 'css' || !m.h) return undefined;
  var cached = cssCache.get(m.h);
  if (cached !== undefined) return Promise.resolve(cached);
  return browser.runtime.sendNativeMessage(NATIVE, { k: 'css', h: m.h, top: !!m.top }).then(function (r) {
    var css = (r && r.css) || '';
    if (cssCache.size >= 200) cssCache.delete(cssCache.keys().next().value);
    cssCache.set(m.h, css);
    return css;
  }, function () { return ''; });
});
