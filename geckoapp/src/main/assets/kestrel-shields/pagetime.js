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
    if (!/^https?:$/.test(String(location.protocol))) return;                       // (the manifest only matches http and https; this keeps a stray about:blank out)
    if (typeof browser === 'undefined' || !browser || !browser.runtime) return;
    var perf = window.performance;
    if (!perf || typeof perf.now !== 'function') return;
    window.__kstPT = 1;
    var C = (window.__kstPTcfg && typeof window.__kstPTcfg === 'object') ? window.__kstPTcfg : {};
    var TICK = C.tick || 100, GRACE = C.grace || 3000, CAP = C.cap || 45000, MAXSLOW = 3;
    var sent = false, hid = (document.hidden === true) ? 1 : 0;

    // ---- PERF10c (owner 2026-11-22: "I can't attach photos to claude.ai"): what the PAGE receives when a file input changes. The app already knows what Android handed over (`filepick picked`); the step it cannot see is
    // what the page gets from the engine: the File's type and size, its first bytes, whether the picture decodes, whether a canvas can draw it and re-encode it (what sites do before an upload). Up to 3 files, at most
    // 4 times a page, only for a change of an <input type=file> that has files, in this top page. Numbers and a few fixed words only: never a file name, a path or a pixel. One message { k: 'fc', n, a: [ { t, kb, mg, dec, w, h, px, enc, ek, ms } ] }
    // dec: 0 not tried, 1 decoded, 2 failed, 3 timed out; px: 0 not tried, 1 drawn (non-blank), 2 all transparent, 3 one flat colour, 4 canvas refused; enc: 0 not tried, 1 re-encoded, 2 empty result, 4 refused.
    var FCN = 0, fcLast = -1e9;
    function magic(u8) {
      var n = u8 ? u8.length : 0;
      if (n >= 8 && u8[0] === 0x89 && u8[1] === 0x50 && u8[2] === 0x4E && u8[3] === 0x47) return 'png';
      if (n >= 3 && u8[0] === 0xFF && u8[1] === 0xD8 && u8[2] === 0xFF) return 'jpeg';
      if (n >= 6 && u8[0] === 0x47 && u8[1] === 0x49 && u8[2] === 0x46 && u8[3] === 0x38) return 'gif';
      if (n >= 12 && u8[0] === 0x52 && u8[1] === 0x49 && u8[2] === 0x46 && u8[3] === 0x46 && u8[8] === 0x57 && u8[9] === 0x45 && u8[10] === 0x42 && u8[11] === 0x50) return 'webp';
      if (n >= 12 && u8[4] === 0x66 && u8[5] === 0x74 && u8[6] === 0x79 && u8[7] === 0x70) return (u8[8] === 0x61 && u8[9] === 0x76 && u8[10] === 0x69) ? 'avif' : 'heic';
      if (n >= 2 && u8[0] === 0x42 && u8[1] === 0x4D) return 'bmp';
      if (n >= 4 && u8[0] === 0x25 && u8[1] === 0x50 && u8[2] === 0x44 && u8[3] === 0x46) return 'pdf';
      return n > 0 ? 'other' : 'none';
    }
    function probeFile(f, done) {
      var r = { t: String(f.type || '').toLowerCase().slice(0, 30), kb: Math.round((+f.size || 0) / 1024), mg: 'none', dec: 0, w: 0, h: 0, px: 0, enc: 0, ek: 0, ms: 0 };
      var t0 = perf.now(), fin = false, url = null, guard = 0;
      function end() { if (fin) return; fin = true; try { clearTimeout(guard); } catch (e) { /* gone */ } try { if (url) URL.revokeObjectURL(url); } catch (e) { /* gone */ } r.ms = Math.round(perf.now() - t0); done(r); }
      guard = setTimeout(function () { if (!r.dec) r.dec = 3; end(); }, 6000);
      function draw(im) {
        try {
          var w = im.naturalWidth || 0, h = im.naturalHeight || 0;
          r.w = w; r.h = h;
          if (!(w > 0 && h > 0)) { r.px = 2; end(); return; }
          var s = Math.min(1, 256 / Math.max(w, h)), c = document.createElement('canvas');
          c.width = Math.max(1, Math.round(w * s)); c.height = Math.max(1, Math.round(h * s));
          var g = c.getContext('2d');
          g.drawImage(im, 0, 0, c.width, c.height);
          var d = g.getImageData(0, 0, c.width, c.height).data, i, solid = 0, first = d[0] + ',' + d[1] + ',' + d[2] + ',' + d[3], flat = true;
          for (i = 0; i < d.length; i += 28) { if (d[i + 3] > 0) solid++; if (flat && (d[i] + ',' + d[i + 1] + ',' + d[i + 2] + ',' + d[i + 3]) !== first) flat = false; }
          r.px = solid === 0 ? 2 : (flat ? 3 : 1);
          try {
            c.toBlob(function (b) { r.enc = (b && b.size > 0) ? 1 : 2; r.ek = b ? Math.round(b.size / 1024) : 0; end(); }, 'image/jpeg', 0.8);
          } catch (e3) { r.enc = 4; end(); }
        } catch (e2) { r.px = 4; end(); }
      }
      function decode() {
        try {
          var im = new Image();
          im.onload = function () { r.dec = 1; draw(im); };
          im.onerror = function () { r.dec = 2; end(); };
          url = URL.createObjectURL(f);
          im.src = url;
        } catch (e) { r.dec = 2; end(); }
      }
      function after() { if (r.t.indexOf('image/') === 0 || r.mg === 'png' || r.mg === 'jpeg' || r.mg === 'webp' || r.mg === 'gif' || r.mg === 'avif' || r.mg === 'heic' || r.mg === 'bmp') decode(); else end(); }
      try {
        var fr = new FileReader();
        fr.onload = function () { try { r.mg = magic(new Uint8Array(fr.result)); } catch (e) { r.mg = 'other'; } after(); };
        fr.onerror = function () { r.mg = 'other'; r.dec = 2; end(); };
        fr.readAsArrayBuffer(f.slice(0, 16));
      } catch (e) { r.mg = 'other'; after(); }
    }
    function fileChanged(ev) {
      try {
        if (FCN >= 4 || !ev) return;                                            // (a pick, a drop or a paste: every way a page gets files ends in a change of the input; at most 4 tests per page keeps a script from making work)
        var t = ev.target;
        if (!t || String(t.tagName).toUpperCase() !== 'INPUT' || String(t.type).toLowerCase() !== 'file') return;
        var fl = t.files, now = perf.now();
        if (!fl || !fl.length || now - fcLast < 1500) return;                  // (a change and an input event of the same pick count once)
        fcLast = now; FCN++;
        try { browser.runtime.sendMessage({ k: 'fc', n: fl.length, a: [] }).catch(function () { /* no background page */ }); } catch (e1) { /* same */ }   // PERF12: told at once, before the (slower) test: proves the page DID see a file arrive even if the test below never finishes
        var total = Math.min(fl.length, 3), out = [], i;
        function one(r) { out.push(r); if (out.length === total) { try { browser.runtime.sendMessage({ k: 'fc', n: fl.length, a: out }).catch(function () { /* no background page */ }); } catch (e) { /* same */ } } }
        for (i = 0; i < total; i++) probeFile(fl[i], one);
      } catch (e) { /* a page must never feel this script */ }
    }
    try { document.addEventListener('change', fileChanged, true); document.addEventListener('input', fileChanged, true); } catch (e0) { /* optional */ }

    // ---- the page's lateness
    var lagMax = 0, lagTot = 0, lagTicks = 0, expected = perf.now() + TICK, timer = 0, sampling = true, tickN = 0;

    // ---- PERF17d (owner 2026-11-24: "Cloudflare tests for anti bots are extremely slow"): is this page a bot check, and for how long? 1 = the full-page interstitial (title or its own page elements), 2 = a check widget inside a normal
    // page (a Turnstile box). Looked at once a second from the lateness tick; the first time it is seen is kept. Nothing but a number leaves the page. (The jsd script every protected site loads is NOT a check: not looked at.)
    var chKind = 0, chAt = -1;
    function checkKind() {
      try {
        var t = String(document.title || '').toLowerCase();
        if (/^(just a moment|attention required|checking your browser|verifying you are human|one more step|performing security verification)/.test(t)) return 1;
        if (document.getElementById('challenge-form') || document.getElementById('challenge-running') || document.getElementById('challenge-stage') || document.getElementById('cf-challenge-running')) return 1;
        if (document.querySelector('.cf-turnstile, iframe[src*="challenges.cloudflare.com"]')) return 2;
      } catch (e) { /* a page must never feel this script */ }
      return 0;
    }
    function tick() {
      if (!sampling) return;
      var now = perf.now(), late = now - expected;
      if (late > 50) { lagTicks++; lagTot += late; }
      if (late > lagMax) lagMax = late;
      expected = now + TICK;                                 // from now: one long block counts once, not once per missed tick
      tickN++;
      if (chKind !== 1 && (tickN % 10) === 0) { var ck = checkKind(); if (ck && (chKind === 0 || ck === 1)) { if (chKind === 0) chAt = now; chKind = ck; } }
      timer = setTimeout(tick, TICK);
    }

    // ---- PERF17c (owner 2026-11-24: "Animations on websites are laggy"): the page's own frame cadence right after the load: how many frame callbacks came in 2.5 s, how many gaps over 50 ms and over 100 ms, the longest gap.
    // A page that animates shows its drawing rate; an idle page shows the engine's rate. One window, started 100 ms after the load event (so it ends before the report GRACE later), skipped when the page is hidden.
    var fmRes = null;
    function cadence() {
      try {
        if (typeof requestAnimationFrame !== 'function' || document.hidden) return;
        var t0 = perf.now(), last = 0, n = 0, a = 0, b = 0, mx = 0;
        var f = function (t) {
          if (document.hidden || sent) return;
          if (last) { var d = t - last; n++; if (d > 50) a++; if (d > 100) b++; if (d > mx) mx = d; }
          last = t;
          if (perf.now() - t0 >= 2500) { fmRes = [n, a, b, Math.round(mx)]; return; }
          requestAnimationFrame(f);
        };
        requestAnimationFrame(f);
      } catch (e) { /* optional */ }
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
        fm: fmRes, ch: chKind || checkKind(), chs: chAt >= 0 ? Math.round(perf.now() - chAt) : -1,
        rd: n ? (+n.redirectCount || 0) : 0
      };
      try { browser.runtime.sendMessage(m).catch(function () { /* the background page is not there: nothing to do */ }); } catch (e) { /* same */ }
    }

    // when to tell: GRACE after the load event, never later than CAP after the start, and at once if the page is left
    function afterLoad() { setTimeout(cadence, 100); setTimeout(function () { report(false); }, GRACE); }
    if (document.readyState === 'complete') afterLoad(); else window.addEventListener('load', afterLoad, false);
    setTimeout(function () { report(true); }, CAP);
    try { window.addEventListener('pagehide', function () { report(true); }, false); } catch (e6) { /* optional */ }
    window.KestrelPT = { report: function () { report(false); } };           // for the tests
  } catch (e) { /* a page must never feel this script */ }
})();
