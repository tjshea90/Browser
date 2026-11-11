/*
 * Kestrel page-side protections. Injected by BrowserWebViewClient after the document exists (onPageCommitVisible / onPageFinished),
 * prefixed with:  var __KST_CFG={"fp":1,"sd":1,"pn":1,"seed":<int>};
 *
 * Deliberately conservative ES5 (the tablet's WebView version is unknown and may be old): no arrow functions, let/const,
 * optional chaining, classes. Every feature is wrapped so that a failure can never break the page. Idempotent.
 *
 *  fp  fingerprint "farbling": tiny, DETERMINISTIC (per browser-session + site) noise on canvas readbacks and audio buffers, and a
 *      reduced hardwareConcurrency. Same input -> same output within a session (so apps keep working); differs across sessions/sites.
 *  sd  Data-saver signal: navigator.connection.saveData === true, so sites that honour it send lighter pages.
 *  pn  Media never preloads or autoplays (video preload is the biggest silent data drain on a hotspot).
 */
(function () {
  'use strict';
  var cfg = (typeof __KST_CFG !== 'undefined' && __KST_CFG) || {};
  if (window.__kst) return;
  try { Object.defineProperty(window, '__kst', { value: 1 }); } catch (e) { window.__kst = 1; }

  /** Make replaced functions/getters look native to toString()-based tamper detection. */
  function nativeLook(fn, name) {
    try {
      Object.defineProperty(fn, 'toString', {
        value: function () { return 'function ' + name + '() { [native code] }'; },
        configurable: true
      });
    } catch (e) { /* ignore */ }
    return fn;
  }

  function hash32(str) {
    var h = 2166136261;
    for (var i = 0; i < str.length; i++) { h ^= str.charCodeAt(i); h = Math.imul(h, 16777619); }
    return h >>> 0;
  }

  // Site-specific seed: browser-session seed mixed with the last two host labels (so a.example.com and b.example.com agree).
  var host = '';
  try { host = String(location.hostname || ''); } catch (e) { /* ignore */ }
  var labels = host.split('.');
  var siteKey = labels.slice(-2).join('.');
  var siteSeed = (hash32(siteKey) ^ (cfg.seed >>> 0)) >>> 0;

  // ------------------------------------------------------------------ fingerprint farbling
  if (cfg.fp) {
    try {
      /** Flip the lowest bit of one colour channel in ~1/12 of the pixels, at deterministic positions. Max visible change: 1/255. */
      var farbleBytes = function (data, seed) {
        var n = data.length >> 2;
        var s = seed >>> 0;
        var p = s % 13;
        while (p < n) {
          s = (Math.imul(s, 1664525) + 1013904223) >>> 0;
          data[(p << 2) + (s % 3)] ^= 1;
          p += 8 + (s >>> 28);
        }
      };

      var CanvasProto = window.HTMLCanvasElement && HTMLCanvasElement.prototype;
      var Ctx2D = window.CanvasRenderingContext2D && CanvasRenderingContext2D.prototype;
      var MIN_PIXELS = 256, MAX_PIXELS = 4000000;

      if (CanvasProto && CanvasProto.getContext) {
        var origGetContext = CanvasProto.getContext;
        CanvasProto.getContext = nativeLook(function (type) {
          var ctx = origGetContext.apply(this, arguments);
          try { if (ctx && /webgl|bitmaprenderer/i.test(String(type))) this.__kstSkip = 1; } catch (e) { /* ignore */ }
          return ctx;
        }, 'getContext');
      }

      var origGetImageData = Ctx2D && Ctx2D.getImageData;
      if (origGetImageData) {
        Ctx2D.getImageData = nativeLook(function (x, y, w, h) {
          var img = origGetImageData.apply(this, arguments);
          try {
            var px = img.width * img.height;
            if (px >= MIN_PIXELS && px <= MAX_PIXELS) farbleBytes(img.data, siteSeed ^ Math.imul(img.width, 7919) ^ img.height);
          } catch (e) { /* ignore */ }
          return img;
        }, 'getImageData');
      }

      // toDataURL / toBlob: read back through a farbled COPY so the visible canvas is never modified.
      var farbledCopy = function (src) {
        if (src.__kstSkip) return null; // WebGL canvases: copying could return a blank frame
        var w = src.width, h = src.height;
        if (w * h < MIN_PIXELS || w * h > MAX_PIXELS) return null;
        var c = document.createElement('canvas');
        c.width = w; c.height = h;
        var ctx = origGetContext.call(c, '2d');
        if (!ctx) return null;
        ctx.drawImage(src, 0, 0);
        var img = origGetImageData.call(ctx, 0, 0, w, h); // throws SecurityError for tainted canvases -> caller falls back
        farbleBytes(img.data, siteSeed ^ Math.imul(w, 7919) ^ h);
        ctx.putImageData(img, 0, 0);
        return c;
      };
      if (CanvasProto && CanvasProto.toDataURL && origGetContext && origGetImageData) {
        var origToDataURL = CanvasProto.toDataURL;
        CanvasProto.toDataURL = nativeLook(function () {
          try { var c = farbledCopy(this); if (c) return origToDataURL.apply(c, arguments); } catch (e) { /* fall through */ }
          return origToDataURL.apply(this, arguments);
        }, 'toDataURL');
      }
      if (CanvasProto && CanvasProto.toBlob && origGetContext && origGetImageData) {
        var origToBlob = CanvasProto.toBlob;
        CanvasProto.toBlob = nativeLook(function () {
          try { var c = farbledCopy(this); if (c) return origToBlob.apply(c, arguments); } catch (e) { /* fall through */ }
          return origToBlob.apply(this, arguments);
        }, 'toBlob');
      }

      // Audio: nudge a sparse set of samples by < 1e-5 (inaudible) once per channel buffer.
      var AB = window.AudioBuffer && AudioBuffer.prototype;
      if (AB && AB.getChannelData && window.WeakMap) {
        var done = new WeakMap();
        var origGetChannelData = AB.getChannelData;
        AB.getChannelData = nativeLook(function (ch) {
          var arr = origGetChannelData.apply(this, arguments);
          try {
            if (arr && !done.has(arr) && arr.length > 64) {
              done.set(arr, 1);
              var s = (siteSeed ^ Math.imul((ch | 0) + 1, 2654435761)) >>> 0;
              for (var i = s % 97; i < arr.length; i += 40 + (s >>> 27)) {
                s = (Math.imul(s, 1664525) + 1013904223) >>> 0;
                arr[i] += ((s >>> 16) & 255) * 4e-8;
              }
            }
          } catch (e) { /* ignore */ }
          return arr;
        }, 'getChannelData');
      }

      // Fewer cores than reality (never below 2): shrinks the entropy of the commonest fingerprinting signal.
      var real = navigator.hardwareConcurrency | 0;
      if (real > 2) {
        var fake = 2 + (siteSeed % (real - 1));
        var getter = nativeLook(function () { return fake; }, 'get hardwareConcurrency');
        Object.defineProperty(Navigator.prototype, 'hardwareConcurrency', { get: getter, configurable: true, enumerable: true });
      }
    } catch (e) { /* protections are best-effort; never break the page */ }
  }

  // ------------------------------------------------------------------ data saver
  if (cfg.sd) {
    try {
      var conn = navigator.connection;
      if (conn) {
        var proto = Object.getPrototypeOf(conn) || conn;
        Object.defineProperty(proto, 'saveData', { get: nativeLook(function () { return true; }, 'get saveData'), configurable: true, enumerable: true });
      }
    } catch (e) { /* ignore */ }
  }

  if (cfg.pn) {
    try {
      var tame = function (el) {
        try {
          if (el.paused !== false) { // never touch something already playing (the user started it)
            if (el.preload !== 'none') el.preload = 'none';
            if (el.autoplay) el.autoplay = false;
          }
        } catch (e) { /* ignore */ }
      };
      var scan = function (root) {
        if (!root || !root.querySelectorAll) return;
        var list = root.querySelectorAll('video,audio');
        for (var i = 0; i < list.length; i++) tame(list[i]);
      };
      scan(document);
      if (window.MutationObserver) {
        new MutationObserver(function (records) {
          for (var i = 0; i < records.length; i++) {
            var added = records[i].addedNodes;
            for (var j = 0; j < added.length; j++) {
              var n = added[j];
              if (n.nodeType !== 1) continue;
              var tag = n.tagName;
              if (tag === 'VIDEO' || tag === 'AUDIO') tame(n); else scan(n);
            }
          }
        }).observe(document.documentElement, { childList: true, subtree: true });
      }
    } catch (e) { /* ignore */ }
  }
})();
