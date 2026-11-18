'use strict';
// QL1 (owner message 2026-11-18: "When i put YouTube video in full screen, it doesn't let me choose the video quality. I want to be able to choose the video quality on as many sites and videos as possible").
// The page-world half of the quality choice (research/QL1_QUALITY.md). A site's own player (hls.js, dash.js, Shaka) keeps its quality list and its "use this one" switch in an object that nothing on the page
// points to; quality.js (the isolated half, which answers the full-screen controls) can only use what it can find. This script only WATCHES: when a page's script sets one of the three well-known globals
// (Hls, dashjs, shaka) it wraps the constructor so that every player the page makes is noted in a small list (at most 6, the page's own objects, nothing copied). It changes nothing a page sees except that
// the constructor is a Proxy of the original (statics, prototype and instanceof still work). It never calls a player: only quality.js does, and only while a film is full screen under Kestrel's controls.
// Firefox only (Proxy, Reflect, accessors on window): the extension runs nowhere else. Every step is inside try/catch: a failed hook leaves the page exactly as it was.
(function () {
  try {
    if (window.__kstQM) return;
    if (typeof Proxy !== 'function' || typeof Reflect !== 'object') return;
    window.__kstQM = 1;
    var reg = [];                                                       // {k: 'hls' | 'dash' | 'shaka', o: the player}
    try { Object.defineProperty(window, '__kstq1', { value: reg, enumerable: false, configurable: false, writable: false }); } catch (e0) { return; }
    function note(kind, o) {
      try {
        if (!o) return;
        if (reg.length >= 6) reg.shift();
        reg.push({ k: kind, o: o });
      } catch (e) { /* never break the page */ }
    }
    function wrapCtor(C, kind) {                                        // new C(...) still returns what C returns; the result is noted on the way
      if (typeof C !== 'function' || C.__kstw) return C;
      try {
        var P = new Proxy(C, {
          construct: function (t, args, nt) { var o = Reflect.construct(t, args, nt); note(kind, o); return o; },
          get: function (t, k, r) { if (k === '__kstw') return 1; return Reflect.get(t, k, t); }
        });
        return P;
      } catch (e) { return C; }
    }
    function hookGlobal(name, onSet) {                                 // a page script that assigns window[name] later is seen; one that already did is wrapped now
      try {
        var d = Object.getOwnPropertyDescriptor(window, name);
        var val = d && 'value' in d ? d.value : undefined;
        if (d && !d.configurable) { try { onSet(val); } catch (e1) { /* ignore */ } return; }
        Object.defineProperty(window, name, {
          configurable: true, enumerable: true,
          get: function () { return val; },
          set: function (v) { try { v = onSet(v); } catch (e2) { /* keep v */ } val = v; }
        });
        if (val !== undefined) { try { val = onSet(val); } catch (e3) { /* keep */ } }
      } catch (e) { /* the page keeps its global as it is */ }
    }
    hookGlobal('Hls', function (v) { return wrapCtor(v, 'hls'); });
    hookGlobal('dashjs', function (v) {                                // dashjs.MediaPlayer() answers a factory whose create() makes the player
      try {
        if (!v || typeof v.MediaPlayer !== 'function' || v.MediaPlayer.__kstw) return v;
        var MP = v.MediaPlayer;
        var wrapped = new Proxy(MP, {
          apply: function (t, th, args) {
            var f = Reflect.apply(t, th, args);
            try {
              if (f && typeof f.create === 'function' && !f.__kstc) {
                var c = f.create;
                f.create = function () { var p = c.apply(this, arguments); note('dash', p); return p; };
                f.__kstc = 1;
              }
            } catch (e) { /* ignore */ }
            return f;
          },
          get: function (t, k, r) { if (k === '__kstw') return 1; return Reflect.get(t, k, t); }
        });
        try { v.MediaPlayer = wrapped; } catch (e4) { /* a frozen namespace: leave it */ }
      } catch (e) { /* ignore */ }
      return v;
    });
    hookGlobal('shaka', function (v) {                                 // shaka.Player is assigned onto the namespace, perhaps after the namespace itself
      try {
        if (!v || typeof v !== 'object') return v;
        var pd = Object.getOwnPropertyDescriptor(v, 'Player');
        if (pd && pd.configurable === false) return v;
        var cur = pd && 'value' in pd ? pd.value : undefined;
        Object.defineProperty(v, 'Player', {
          configurable: true, enumerable: true,
          get: function () { return cur; },
          set: function (x) { cur = wrapCtor(x, 'shaka'); }
        });
        if (cur !== undefined) cur = wrapCtor(cur, 'shaka');
      } catch (e) { /* ignore */ }
      return v;
    });
  } catch (e) { /* nothing here may break a page */ }
})();
