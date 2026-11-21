'use strict';
// LD4 (owner 2026-11-21: "Claude.Ai and YouTube load slow, even on fast wifi ... make sure the live diagnostics ... tells claude a lot of information about how the app is running"): the page's OWN account of one load.
// The app already knows when a load started, painted and stopped (PageTimes). It cannot know WHERE the time went: waiting for the network, or the page's own scripts keeping the four slow cores busy, or many requests.
// This script, in the TOP page only, from the first moment of the page, keeps small counters and sends them ONCE, 3 s after the load event (or at 45 s if the load never ends, or when the page is left):
//  - the main document's phases from Navigation Timing: dns, connect (con) and its TLS part, time to the first byte since the navigation began (fb), the server's wait (ttfb), dom ready (dcl), load, the first and the largest paint;
//  - how many requests the page made (rn), how many came from the cache (rc), kilobytes over the network (kb), how many took 1 s or more (r1) and 3 s or more (r3), the sum of their times (rsum), their protocols (h2, h1, h3: only where the
//    browser shows them) and their kinds (js, css, img, xhr, oth); the three slowest as [host, kind, ms] (a host name only: never a path, a query, a title or a page's text);
//  - the page's lateness: a 100 ms timer shows how late the page answered (lm = [the worst lateness, the sum of every lateness over 50 ms, how many timers were over 50 ms]); a page whose scripts are the load has a big sum,
//    a page that only waits for the network has none. The timer stops for good when the report is sent: nothing keeps running;
//  - the weight: elements (nodes), scripts (scr), frames (fr); whether the person looked away (hid: then the numbers say nothing about speed) and whether the report was cut short (pt).
// The resources are counted through a PerformanceObserver as they finish, NOT read from the performance buffer (it holds only about 250 entries: claude.ai makes over a thousand). Conservative ES5, plain numbers.
(function () {
  try {
    if (window !== window.top || window.__kstPT) return;
    if (typeof browser === 'undefined' || !browser || !browser.runtime) return;
    var perf = window.performance;
    if (!perf || typeof perf.now !== 'function') return;
    window.__kstPT = 1;
    var C = (window.__kstPTcfg && typeof window.__kstPTcfg === 'object') ? window.__kstPTcfg : {};
    var TICK = C.tick || 100, GRACE = C.grace || 3000, CAP = C.cap || 45000, MAXSLOW = 3;
    var sent = false, hid = (document.hidden === true) ? 1 : 0;

    // ---- the page's lateness
    var lagMax = 0, lagTot = 0, lagTicks = 0, expected = perf.now() + TICK, timer = 0, sampling = true;
    function tick() {
      if (!sampling) return;
      var now = perf.now(), late = now - expected;
      if (late > 50) { lagTicks++; lagTot += late; }
      if (late > lagMax) lagMax = late;
      expected = now + TICK;                                 // from now: one long block counts once, not once per missed tick
      timer = setTimeout(tick, TICK);
    }
    timer = setTimeout(tick, TICK);

    // ---- the requests, as they finish
    var R = { n: 0, cache: 0, kb: 0, r1: 0, r3: 0, sum: 0, h2: 0, h1: 0, h3: 0, js: 0, css: 0, img: 0, xhr: 0, oth: 0 }, slow = [];
    function kind(t) {
      if (t === 'script') return 'js';
      if (t === 'css' || t === 'link') return 'css';
      if (t === 'img' || t === 'image' || t === 'input') return 'img';
      if (t === 'xmlhttprequest' || t === 'fetch' || t === 'beacon') return 'xhr';
      return 'oth';
    }
    function hostOf(u) { var m = /^[a-z]+:\/\/([^\/?#:@]+)/i.exec(String(u || '')); return m ? m[1].toLowerCase().slice(0, 60) : ''; }
    function keepSlow(h, g, d) {
      if (!h) return;
      if (slow.length < MAXSLOW) { slow.push([h, g, d]); return; }
      var lo = 0, i;
      for (i = 1; i < slow.length; i++) if (slow[i][2] < slow[lo][2]) lo = i;
      if (d > slow[lo][2]) slow[lo] = [h, g, d];
    }
    function add(e) {
      var d = +e.duration || 0, ts = +e.transferSize || 0, g = kind(e.initiatorType), p = e.nextHopProtocol;
      R.n++; R[g]++;
      if (d > 0) R.sum += d;
      if (d >= 1000) R.r1++;
      if (d >= 3000) R.r3++;
      R.kb += ts / 1024;
      if (ts === 0 && (+e.decodedBodySize || 0) > 0) R.cache++;           // a file that is visibly there but cost no bytes: the cache
      if (p === 'h2') R.h2++; else if (p === 'h3' || p === 'h3-29') R.h3++; else if (p && String(p).indexOf('http/1') === 0) R.h1++;
      if (d >= 400) keepSlow(hostOf(e.name), g, Math.round(d));
    }
    var lcp = -1;
    try {
      var ro = new PerformanceObserver(function (list) { var es = list.getEntries(); for (var i = 0; i < es.length; i++) add(es[i]); });
      try { ro.observe({ type: 'resource', buffered: true }); } catch (e1) { ro.observe({ entryTypes: ['resource'] }); }
    } catch (e2) { /* no observer: the other numbers still go */ }
    try {
      var lo = new PerformanceObserver(function (list) { var es = list.getEntries(), l = es[es.length - 1]; if (l) lcp = Math.round(l.renderTime || l.loadTime || l.startTime || -1); });
      try { lo.observe({ type: 'largest-contentful-paint', buffered: true }); } catch (e3) { lo.observe({ entryTypes: ['largest-contentful-paint'] }); }
    } catch (e4) { /* this engine has no LCP: -1 */ }

    function onVis() { if (document.hidden) { hid = 1; report(true); } }
    try { document.addEventListener('visibilitychange', onVis, true); } catch (e5) { /* optional */ }

    function ms(v) { v = +v; return (v >= 0 && v < 6e5) ? Math.round(v) : -1; }
    function diff(a, b) { a = +a; b = +b; return (a > 0 && b >= a) ? Math.round(b - a) : -1; }
    function navEntry() { try { var l = perf.getEntriesByType('navigation'); return l && l.length ? l[0] : null; } catch (e) { return null; } }
    function paint() {
      try {
        var l = perf.getEntriesByType('paint');
        for (var i = 0; i < l.length; i++) if (l[i].name === 'first-contentful-paint') return Math.round(l[i].startTime);
      } catch (e) { /* none */ }
      return -1;
    }
    function count(sel) { try { return document.getElementsByTagName(sel).length; } catch (e) { return 0; } }
    function navType(n) { var t = n && n.type; return t === 'reload' ? 1 : t === 'back_forward' ? 2 : t === 'prerender' ? 3 : 0; }

    function report(partial) {
      if (sent) return;
      sent = true; sampling = false;
      try { clearTimeout(timer); } catch (e) { /* gone */ }
      try { if (ro) ro.disconnect(); if (lo) lo.disconnect(); } catch (e) { /* gone */ }
      try { document.removeEventListener('visibilitychange', onVis, true); } catch (e) { /* gone */ }
      var n = navEntry(), sl = [], i;
      for (i = 0; i < slow.length; i++) sl.push(slow[i]);
      sl.sort(function (a, b) { return b[2] - a[2]; });
      var m = {
        k: 'pt', h: String(location.hostname || '').toLowerCase().slice(0, 80), nt: navType(n),
        dns: n ? diff(n.domainLookupStart, n.domainLookupEnd) : -1,
        con: n ? diff(n.connectStart, n.connectEnd) : -1,
        tls: (n && n.secureConnectionStart > 0) ? diff(n.secureConnectionStart, n.connectEnd) : -1,
        fb: n ? ms(n.responseStart) : -1,
        ttfb: n ? diff(n.requestStart, n.responseStart) : -1,
        dcl: n ? ms(n.domContentLoadedEventEnd) : -1,
        load: (n && n.loadEventEnd > 0) ? ms(n.loadEventEnd) : -1,
        fcp: paint(), lcp: lcp,
        rn: R.n, rc: R.cache, kb: Math.round(R.kb), r1: R.r1, r3: R.r3, rsum: Math.min(Math.round(R.sum), 1e7),
        h2: R.h2, h1: R.h1, h3: R.h3, js: R.js, css: R.css, img: R.img, xhr: R.xhr, oth: R.oth,
        lm: [Math.round(lagMax), Math.round(lagTot), lagTicks],
        nodes: count('*'), scr: count('script'), fr: count('iframe'),
        hid: hid, pt: partial ? 1 : 0, sl: sl, at: Math.round(perf.now()),
        rd: n ? (+n.redirectCount || 0) : 0
      };
      try { browser.runtime.sendMessage(m).catch(function () { /* the background page is not there: nothing to do */ }); } catch (e) { /* same */ }
    }

    // when to tell: GRACE after the load event, never later than CAP after the start, and at once if the page is left
    function afterLoad() { setTimeout(function () { report(false); }, GRACE); }
    if (document.readyState === 'complete') afterLoad(); else window.addEventListener('load', afterLoad, false);
    setTimeout(function () { report(true); }, CAP);
    try { window.addEventListener('pagehide', function () { report(true); }, false); } catch (e6) { /* optional */ }
    window.KestrelPT = { report: function () { report(false); } };           // for the tests
  } catch (e) { /* a page must never feel this script */ }
})();
