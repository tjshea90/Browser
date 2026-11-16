'use strict';
// ANS53a / ANS53b (owner message 53: "Never, on any site, ever, anywhere in the app, automatically load a video and start playing it. Only play a video if I press the play button on the video." and "videos should not
// be downloaded or buffered at all until I press the play button. No data should ever be used downloading or buffering videos on web sites unless I press play. Only the usual picture/frame of the video that usually
// shows on web sites should be downloaded"). Design and the Gecko facts behind it: research/ANS53_DESIGN.md.
// Runs INSIDE the page (world MAIN, every frame, document_start, declared in the manifest like guard-main.js): the engine's own rules are not enough (a document that has had ANY tap lets its videos start by itself, and a
// player written in script - hls.js, video.js, Shaka, dash.js, YouTube - fetches its pictures with XHR whatever the preload rules say). So a media element is CLOSED until the person presses it, and while it is closed:
//   - `preload` is none whatever the page says (its own value is remembered and given back on opening), `autoplay` is not applied (the attribute of the markup is taken off, the property is remembered);
//   - a `blob:` source (a MediaSource: every player written in script) is HELD: the player then waits for `sourceopen` and fetches no segment at all (the playlist is small and goes: Kestrel needs it for its lists);
//   - `load()` is held, and `play()` is REFUSED (NotAllowedError, what the engine's own autoplay rule says) unless it comes with a transient user activation: a page the person tapped once cannot start a video later.
// It OPENS - what was held is applied, in order - when (a) `play()` is called inside a user activation (`navigator.userActivation.isActive`: the page's own play button, a keyboard, a script that answers a tap),
// (b) a real tap lands inside the picture of the element (a custom play overlay, the engine's own controls, a player that waits for `canplay` before it calls play(): it could never get there otherwise), (c) Kestrel's
// own controls tell it with `__kst_gate_v1` on the element (a content script's own play() does not pass through this page's replacement of play()), (d) the person turned the rule off (Settings).
// A stream (`srcObject`: a camera, a call) is never held (and its autoplay is given back). Only VIDEO is gated (the person's rule is about video: a music player's next track or a sound must not be refused), and only a
// blob: address made from a MediaSource is held (a File the person chose for an upload form is no stream). Until the content script says otherwise the gate is CLOSED: the person's rule is the default.
// ANS54e (owner message 54: "I want the web video players to use this"): when the person presses a film's player (its play() inside a tap on that player's box, the engine's own controls, Kestrel's own bar) the gate
// FIRST asks the app whether Kestrel's own player takes the film (event __kst_gate_ask_v1 -> content script -> background page -> app; the answer comes back as __kst_gate_ans_v1). Yes: the element stays closed, the
// page's play() is rejected with AbortError (players read that as "interrupted", not as an error) and the app has started its own player. No answer in 700 ms, or no: the film plays here as before. The app decides everything.
// Reviewed by an independent agent (research/ANS53_GATE_REVIEW.md): a tap counts for the video it landed on or in (its player box), for 30 s; a gesture counts 1.2 s even when the activation was consumed.
// What it did is counted (numbers only) and handed to the content script, which relays it to Diagnostics. Conservative ES5; nothing runs until a page touches a media element.
// Tested in headless Chromium by web-tests/gate.test.js (the logic is plain DOM and the same in Gecko).
(function () {
  var CFG_EVENT = '__kst_gate_cfg_v1', OPEN_EVENT = '__kst_gate_v1', REP_EVENT = '__kst_gate_rep_v1', READY_EVENT = '__kst_gate_ready_v1', ASK_EVENT = '__kst_gate_ask_v1', ANS_EVENT = '__kst_gate_ans_v1';
  try {
    if (typeof browser !== 'undefined' && browser && browser.runtime) return; // declared as MAIN world; a copy that ever ran in the isolated world (extension API present) must do nothing there
    if (window.__kstGate) return;
    Object.defineProperty(window, '__kstGate', { value: 1 });
  } catch (e) { return; }
  if (typeof HTMLMediaElement === 'undefined' || typeof WeakMap !== 'function') return;

  var proto = HTMLMediaElement.prototype, eproto = Element.prototype;
  var dnow = Date.now, later = window.setTimeout;
  var on = true;                                         // closed until the person's setting says the rule is off
  var recs = new WeakMap();                              // element -> what is held for it
  var gated = [];                                        // the elements that hold something back (a tap on their picture opens them)
  var N = { held: 0, opened: 0, play: 0, press: 0, kestrel: 0, off: 0, deferred: 0, refused: 0 };
  var dirty = false, timer = 0, reportsLeft = 6, lastGesture = 0;
  var hand = false, askSeq = 0, asks = Object.create(null), jstr = JSON.stringify, oPause = null, playOrig = null;   // hand: the app may take a film over (told with the rule); asks: questions waiting for the app
  var told = false, waiting = [];                         // told: the content script has said on or off at least once; waiting: play() calls that came before that (carried out on OFF, refused on ON)
  var lastTap = { t: 0, target: null };                  // the last real tap or key press: when, and what it landed on
  var mseUrls = Object.create(null), mseCount = 0;       // blob: addresses made from a MediaSource
  var REFUSED = 'The play method is not allowed by the user agent or the platform in the current context, possibly because the user denied permission.';
  var INTERRUPTED = 'The play() request was interrupted: Kestrel\'s own player plays this video.';

  function rec(el) { var r = recs.get(el); if (!r) { r = { open: false, src: null, hasSrc: false, preload: null, hasPreload: false, autoplay: false, hasAutoplay: false, load: false, tracked: false, adopted: false, forced: false, ask: null, askedAt: 0, noUntil: 0, yesAt: 0 }; recs.set(el, r); } return r; }
  function peek(el) { try { return recs.get(el); } catch (e) { return undefined; } }
  function isBlob(v) { var u = (typeof v === 'string') ? v : ((v !== null && v !== undefined) ? String(v) : ''); return mseUrls[u] === 1; }   // only a MediaSource's address (see the createObjectURL wrapper below)
  function isMediaEl(x) { return !!x && x.nodeType === 1 && x.nodeName === 'VIDEO'; }

  // ---------------------------------------------------------------- looking like the engine's own functions
  function mask(fn, name, orig) {
    try {
      Object.defineProperty(fn, 'name', { value: name, configurable: true });
      Object.defineProperty(fn, 'length', { value: orig.length, configurable: true });
      Object.defineProperty(fn, 'toString', { value: function toString() { return 'function ' + name + '() { [native code] }'; }, configurable: true, writable: true });
    } catch (e) { /* the wrapper still works */ }
  }
  function defMethod(obj, name, make) {
    try {
      var d = Object.getOwnPropertyDescriptor(obj, name);
      var orig = d ? d.value : obj[name];
      if (typeof orig !== 'function') return null;
      var fn = make(orig);
      mask(fn, name, orig);
      Object.defineProperty(obj, name, { value: fn, writable: true, configurable: true, enumerable: d ? d.enumerable : false });
      return orig;
    } catch (e) { return null; }
  }
  function defAccessor(obj, name, makeGet, makeSet) {
    try {
      var d = Object.getOwnPropertyDescriptor(obj, name);
      if (!d || !d.get || !d.set) return null;
      var g = makeGet(d.get), s = makeSet(d.set);
      mask(g, 'get ' + name, d.get); mask(s, 'set ' + name, d.set);
      Object.defineProperty(obj, name, { get: g, set: s, configurable: d.configurable, enumerable: d.enumerable });
      return d;
    } catch (e) { return null; }
  }

  // ---------------------------------------------------------------- what was done, told to the content script (numbers only, throttled)
  function touch() { dirty = true; if (!timer && reportsLeft > 0) { try { timer = later.call(window, flush, 1500); } catch (e) { /* no timers */ } } }
  function flush() {
    timer = 0;
    if (!dirty || reportsLeft <= 0) return;
    dirty = false; reportsLeft--;
    var d = [N.held, N.opened, N.play, N.press, N.kestrel, N.deferred, N.refused].join('|');
    N.held = 0; N.opened = 0; N.play = 0; N.press = 0; N.kestrel = 0; N.off = 0; N.deferred = 0; N.refused = 0;
    try { document.dispatchEvent(new CustomEvent(REP_EVENT, { detail: d })); } catch (e) { /* ignore */ }
  }

  // ---------------------------------------------------------------- user activation
  function activation() {
    var near = dnow.call(Date) - lastGesture < 1200;                              // (a click that called requestFullscreen() first has used its activation up: its play() is the same press)
    try { var ua = navigator.userActivation; if (ua && typeof ua.isActive === 'boolean') return ua.isActive || near; } catch (e) { /* an older engine */ }
    return dnow.call(Date) - lastGesture < 1500;
  }
  // where a tap landed: the node itself, or inside the player box of the video (the video, an overlay or a control bar over it, up to five ancestors, never the page's body), shadow trees followed
  function inside(t, a) { var n = 0; while (t && n < 80) { if (t === a) return true; var rn = t.getRootNode ? t.getRootNode() : null; t = t.parentElement || ((rn && rn !== document && rn.host) ? rn.host : null); n++; } return false; }
  function sameBox(el, t) {
    try {
      if (!t || t.nodeType !== 1) return false;
      if (inside(t, el)) return true;
      var up = el;
      for (var n = 0; n < 5; n++) {
        var rn = up.getRootNode ? up.getRootNode() : null;
        up = up.parentElement || ((rn && rn !== document && rn.host) ? rn.host : null);
        if (!up || up === document.body || up === document.documentElement) return false;
        if (inside(t, up)) return true;
      }
    } catch (e) { /* ignore */ }
    return false;
  }
  function pressedRecently(el) { return lastTap.t > 0 && dnow.call(Date) - lastTap.t < 30000 && sameBox(el, lastTap.target); }

  // ---------------------------------------------------------------- the originals
  var oSetAttr = eproto.setAttribute, oRemoveAttr = eproto.removeAttribute, oGetAttr = eproto.getAttribute, oHasAttr = eproto.hasAttribute;
  var oLoad = proto.load;
  oPause = proto.pause;
  var dSrc = null, dPreload = null, dAutoplay = null;

  function track(el) {
    var r = rec(el);
    if (r.tracked) return;
    r.tracked = true;
    if (gated.length >= 400) { var keep = []; for (var i = 0; i < gated.length; i++) if (gated[i].isConnected && !rec(gated[i]).open) keep.push(gated[i]); gated = keep; }
    if (gated.length < 400) gated.push(el);
    try { el.addEventListener(OPEN_EVENT, function () { var was = rec(el).open; openEl(el, 'kestrel'); if (!was && hand) ask(el, 'kestrel', function (h) { if (h) pauseEl(el); }); }, true); } catch (e) { /* ignore */ }   // Kestrel's own controls, also for a film inside a shadow tree
  }

  // ---------------------------------------------------------------- opening
  function openEl(el, why) {
    var r = rec(el);
    if (r.open) return;
    r.open = true; N.opened++; N[why] = (N[why] || 0) + 1;
    try {
      if (r.hasPreload) { if (dPreload) dPreload.set.call(el, r.preload); else oSetAttr.call(el, 'preload', r.preload); }
      else if (r.forced) { oRemoveAttr.call(el, 'preload'); }                               // the page said nothing: the engine's own default again
      r.forced = false;
      if (r.hasSrc) { var s = r.src; r.hasSrc = false; r.src = null; r.load = false; if (dSrc) dSrc.set.call(el, s); else oSetAttr.call(el, 'src', s); }
      else if (r.load) { r.load = false; if (el.networkState === 0) oLoad.call(el); }                 // (giving the preload back already resumes a load that was waiting; a second load() would start it again and ask for the file twice)
      // the person's rule is OFF (Settings): the page's own autoplay comes back, as in any other browser (lab run 43812820865: a muted hero loop loaded but never started with the rule off); so does the autoplay of a video
      // that is given a STREAM (a camera, a call: it must show). A press on the picture, the page's own play() and Kestrel's own controls start nothing by this: the person pressed a play button, or the page plays at the
      // person's request; a tap on a hero loop that has no play button is not a request to play it.
      if (why === 'off' || why === 'stream') giveAutoplay(el, r);
    } catch (e) { /* the page decides */ }
    touch();
  }
  function giveAutoplay(el, r) {
    if (!r.hasAutoplay) return;
    var want = r.autoplay; r.hasAutoplay = false;
    if (want) { try { if (dAutoplay) dAutoplay.set.call(el, true); else oSetAttr.call(el, 'autoplay', ''); } catch (e) { /* the page decides */ } }
  }
  function openAll(why) { for (var i = 0; i < gated.length; i++) { try { var el = gated[i]; openEl(el, why); if (why === 'off') giveAutoplay(el, rec(el)); } catch (e) { /* ignore */ } } }

  function pressAt(x, y, target) {
    for (var i = 0; i < gated.length; i++) {
      var el = gated[i];
      try {
        if (!el.isConnected || rec(el).open) continue;
        var rc = el.getBoundingClientRect();
        if (rc.width >= 80 && rc.height >= 45 && x >= rc.left - 4 && x <= rc.right + 4 && y >= rc.top - 4 && y <= rc.bottom + 4 && sameBox(el, target)) {
          if (hand && !el.controls && target !== el && playLike(target, el)) {                                  // a play button over the picture: the app is asked first, the element stays closed while it thinks
            var rr = rec(el), nowT = dnow.call(Date);
            if (nowT - rr.askedAt > 400 && nowT >= rr.noUntil) ask(el, 'press', function (handed) { if (!handed) openEl(el, 'press'); });
            else if (!(rr.ask && !rr.ask.done) && !rr.open && nowT < rr.noUntil) openEl(el, 'press');             // (the app said no a moment ago: the film plays here; the same press seen again after a yes changes nothing)
          }
          else { openEl(el, 'press'); if (hand && el.controls) askPress(el); }
        }   // (a cookie button or a menu that merely sits over the picture opens nothing)
      } catch (e) { /* ignore */ }
    }
  }

  // ---------------------------------------------------------------- ANS54e: ask the app whether Kestrel's own player takes the film the person pressed
  function pauseEl(el) { try { if (oPause) oPause.call(el); } catch (e) { /* ignore */ } }
  function factsOf(el, why) {
    var r = peek(el) || {}, s = '', mime = '', blob = false, w = 0, h = 0, t = 0, d = 0, lp = 0;
    try {
      if (r.hasSrc) blob = true;
      else {
        s = el.currentSrc || el.src || '';
        if (!s) { var c = el.querySelector ? el.querySelector('source[src]') : null; if (c) { s = c.src || ''; mime = String(c.type || ''); } }
        if (/^blob:/i.test(s)) { blob = true; s = ''; }
      }
    } catch (e) { s = ''; }
    try { var rc = el.getBoundingClientRect(); w = Math.round(rc.width); h = Math.round(rc.height); } catch (e) { /* ignore */ }
    try { t = el.currentTime || 0; d = isFinite(el.duration) ? (el.duration || 0) : 0; lp = el.loop ? 1 : 0; } catch (e) { /* ignore */ }
    return { i: 0, y: why, s: String(s).slice(0, 2000), b: blob ? 1 : 0, m: mime.slice(0, 60), l: lp, w: w, h: h, t: t, d: d };
  }
  // cb(true) = the app took the film over (Kestrel's own player runs it); cb(false) = no, or no answer within 700 ms. An answer that comes later and says yes still stops the element. A second question about the SAME
  // press while the first is in the air joins it (the tap on a play button over the picture, then the page's play() a moment later).
  function ask(el, why, cb) {
    if (!hand || askSeq >= 60) { cb(false); return; }
    var r = rec(el), q = r.ask;
    if (q && !q.done) { q.cbs.push(cb); return; }
    if (why !== 'kestrel' && dnow.call(Date) < r.noUntil) { cb(false); return; }                                 // the app said no (or did not answer) a moment ago: the page plays without another wait
    var id = ++askSeq, f = factsOf(el, why);
    f.i = id; q = { el: el, cbs: [cb], done: false }; r.ask = q; r.askedAt = dnow.call(Date); asks[id] = q;
    q.el = el;
    try { later.call(window, function () { if (!q.done) { r.noUntil = dnow.call(Date) + 30000; finishAsk(q, false); } }, 700); } catch (e) { /* no timers */ }
    try { later.call(window, function () { delete asks[id]; }, 8000); } catch (e) { /* ignore */ }
    try { document.dispatchEvent(new CustomEvent(ASK_EVENT, { detail: jstr(f) })); } catch (e) { finishAsk(q, false); }
  }
  function finishAsk(q, yes) {
    if (q.done) return false;
    q.done = true;
    var cbs = q.cbs; q.cbs = [];
    try { var rq = rec(q.el); rq.noUntil = yes ? 0 : Math.max(rq.noUntil, dnow.call(Date) + 15000); rq.yesAt = yes ? dnow.call(Date) : 0; } catch (e) { /* ignore */ }
    for (var i = 0; i < cbs.length; i++) { try { cbs[i](yes); } catch (e) { /* ignore */ } }
    return true;
  }
  // a new press on this player (a tap on its box that came after the last question) and the film is not playing yet
  function wantsAsk(el) { var r = rec(el); return hand && el.paused && lastTap.t > r.askedAt + 400 && dnow.call(Date) >= r.noUntil && pressedRecently(el); }
  // does the tap's target look like a play button that sits over the picture (not the picture itself)?
  function playLike(t, el) {
    try {
      for (var n = t, i = 0; n && n !== el && i < 6; i++, n = n.parentElement) {
        if (n.nodeName === 'BUTTON' || (n.getAttribute && n.getAttribute('role') === 'button')) return true;
        var cls = (typeof n.className === 'string') ? n.className : '';
        if (/play|start|poster|big-?button|cover/i.test(String(n.getAttribute ? (n.getAttribute('aria-label') || '') + ' ' + (n.getAttribute('title') || '') : '') + ' ' + cls + ' ' + (n.id || ''))) return true;
      }
    } catch (e) { /* ignore */ }
    return false;
  }
  function askPlay(el, args) {
    return new Promise(function (resolve, reject) {
      ask(el, 'play', function (handed) {
        if (handed) { pauseEl(el); reject(new DOMException(INTERRUPTED, 'AbortError')); return; }
        try { openEl(el, 'play'); var p = playOrig.apply(el, args); if (p && typeof p.then === 'function') p.then(resolve, reject); else resolve(); } catch (x) { reject(x); }
      });
    });
  }
  function askPress(el) { ask(el, 'press', function (handed) { if (handed) pauseEl(el); }); }
  try {
    document.addEventListener(ANS_EVENT, function (e) {
      try {
        var d = String(e && e.detail).split('|'), q = asks[+d[0]];
        if (!q) return;
        var yes = d[1] === '1';
        if (!finishAsk(q, yes) && yes) pauseEl(q.el);                                    // too late for the page's play() (it went through): the film is Kestrel's now, stop the page's copy
        delete asks[+d[0]];
      } catch (x) { /* ignore */ }
    }, true);
  } catch (e) { /* ignore */ }

  // ---------------------------------------------------------------- the replacements
  try {
    defMethod(URL, 'createObjectURL', function (orig) {                              // the addresses of MediaSources are the only blob: sources that are held
      return function (obj) {
        var u = orig.apply(this, arguments);
        try { if (typeof MediaSource === 'function' && obj instanceof MediaSource) { if (mseCount > 400) { mseUrls = Object.create(null); mseCount = 0; } mseUrls[u] = 1; mseCount++; } } catch (e) { /* ignore */ }
        return u;
      };
    });
  } catch (e) { /* ignore */ }
  dSrc = defAccessor(proto, 'src',
    function (g) { return function () { var r = peek(this); return (r && r.hasSrc) ? r.src : g.call(this); }; },
    function (s) {
      return function (v) {
        try {
          if (on && isMediaEl(this)) {
            var r = rec(this);
            if (!r.open && !r.adopted) adopt(this);
            if (!r.open && isBlob(v)) { r.src = String(v); r.hasSrc = true; N.deferred++; track(this); touch(); return; }
            if (r.hasSrc && !isBlob(v)) { r.hasSrc = false; r.src = null; }
          }
        } catch (e) { /* the engine decides */ }
        return s.call(this, v);
      };
    });
  dPreload = defAccessor(proto, 'preload',
    function (g) { return function () { var r = peek(this); return (r && r.hasPreload && !r.open) ? r.preload : g.call(this); }; },
    function (s) {
      return function (v) {
        try {
          if (on && isMediaEl(this)) {
            var r = rec(this);
            if (!r.open) { r.preload = String(v); r.hasPreload = true; track(this); return s.call(this, 'none'); }
          }
        } catch (e) { /* the engine decides */ }
        return s.call(this, v);
      };
    });
  dAutoplay = defAccessor(proto, 'autoplay',
    function (g) { return function () { var r = peek(this); return (r && r.hasAutoplay && !r.open) ? r.autoplay : g.call(this); }; },
    function (s) {
      return function (v) {
        try {
          if (on && isMediaEl(this)) {
            var r = rec(this);
            if (!r.open) { r.autoplay = !!v; r.hasAutoplay = true; track(this); return s.call(this, false); }
          }
        } catch (e) { /* the engine decides */ }
        return s.call(this, v);
      };
    });
  defAccessor(proto, 'srcObject',
    function (g) { return function () { return g.call(this); }; },
    function (s) { return function (v) { try { if (v && isMediaEl(this)) { var r = rec(this); if (!r.open) openEl(this, 'stream'); else giveAutoplay(this, r); r.open = true; } } catch (e) { /* ignore */ } return s.call(this, v); }; });   // a stream (camera, call) is never held, and it autoplays as its page meant

  defMethod(proto, 'setAttribute', function (orig) {
    return function (name, value) {
      try {
        if (on && isMediaEl(this)) {
          var r = rec(this);
          if (!r.open) {
            var n = String(name).toLowerCase();
            if (!r.adopted && (n === 'src' || n === 'autoplay')) adopt(this);
            if (n === 'src' && isBlob(value)) { r.src = String(value); r.hasSrc = true; N.deferred++; track(this); touch(); return; }
            if (n === 'preload') { r.preload = String(value); r.hasPreload = true; track(this); return orig.call(this, 'preload', 'none'); }
            if (n === 'autoplay') { r.autoplay = true; r.hasAutoplay = true; track(this); return; }
          }
        }
      } catch (e) { /* the engine decides */ }
      return orig.apply(this, arguments);
    };
  });
  defMethod(proto, 'removeAttribute', function (orig) {
    return function (name) {
      try { var r = peek(this); if (r && r.hasSrc && String(name).toLowerCase() === 'src') { r.hasSrc = false; r.src = null; } } catch (e) { /* ignore */ }
      return orig.apply(this, arguments);
    };
  });
  defMethod(proto, 'load', function (orig) {
    return function () {
      try { if (on && isMediaEl(this)) { var r = rec(this); if (!r.open) { if (!r.adopted) adopt(this); r.load = true; track(this); touch(); return; } } } catch (e) { /* the engine decides */ }
      return orig.apply(this, arguments);
    };
  });
  playOrig = defMethod(proto, 'play', function (orig) {
    return function () {
      try {
        if (on && isMediaEl(this)) {
          var r = rec(this);
          if (hand && r.yesAt && dnow.call(Date) - r.yesAt < 1500) return Promise.reject(new DOMException(INTERRUPTED, 'AbortError'));   // the app took this press over a moment ago: the page's own play() that follows it is the same press
          if (!r.open) {
            if (!r.adopted) adopt(this);
            track(this);
            if (r.ask && !r.ask.done) return askPlay(this, arguments);                              // a question for this very press is in the air: wait for the same answer
            if (activation() || pressedRecently(this)) {
              if (wantsAsk(this)) return askPlay(this, arguments);                                    // the person pressed THIS player: Kestrel's own player may take the film
              openEl(this, 'play');
            }
            else if (!told) {                                                        // the app has not answered yet: the page's play() waits for the answer (carried out when the rule is OFF)
              var self = this, args = arguments;
              return new Promise(function (resolve, reject) { waiting.push({ el: self, args: args, resolve: resolve, reject: reject }); try { later.call(window, settle, 4000); } catch (e) { settle(); } });
            }
            else { N.refused++; touch(); return Promise.reject(new DOMException(REFUSED, 'NotAllowedError')); }
          }
          else if (wantsAsk(this)) return askPlay(this, arguments);                                   // opened by the press already (the picture, the engine's controls): the question comes with the film's play()
        }
      } catch (e) { /* the engine decides */ }
      return orig.apply(this, arguments);
    };
  });

  // ---------------------------------------------------------------- markup: preload and autoplay in the HTML are taken off before the engine starts the element's load
  function adopt(el) {
    var r = rec(el);
    if (r.adopted) return;
    r.adopted = true;
    if (r.open || !on) return;
    track(el);
    var changed = false;
    try {
      var p = oGetAttr.call(el, 'preload');
      if (p === null) { oSetAttr.call(el, 'preload', 'none'); r.forced = true; }                     // (no attribute: the engine's default is `metadata`, which fetches bytes)
      else if (String(p).toLowerCase() !== 'none') { r.preload = p; r.hasPreload = true; oSetAttr.call(el, 'preload', 'none'); changed = true; }
      if (oHasAttr.call(el, 'autoplay')) { r.autoplay = true; r.hasAutoplay = true; oRemoveAttr.call(el, 'autoplay'); changed = true; }
    } catch (e) { /* ignore */ }
    if (changed) { N.held++; touch(); }
  }
  function scan(node) {
    try {
      if (!node || node.nodeType !== 1) return;
      if (isMediaEl(node)) adopt(node);
      if (node.firstElementChild) {
        var list = node.querySelectorAll('video');
        for (var i = 0; i < list.length && i < 500; i++) adopt(list[i]);
      }
    } catch (e) { /* never break the page */ }
  }
  var observer = null;
  function watch(root) {
    try {
      if (!observer && typeof MutationObserver === 'function') {
        observer = new MutationObserver(function (records) {
          for (var i = 0; i < records.length; i++) { var add = records[i].addedNodes; for (var j = 0; j < add.length && j < 2000; j++) scan(add[j]); }
        });
      }
      if (observer) {
        observer.observe(root, { childList: true, subtree: true });
        var list = root.querySelectorAll ? root.querySelectorAll('video') : [];            // what is in the tree already (a shadow root that is filled before it is watched)
        for (var i = 0; i < list.length && i < 500; i++) adopt(list[i]);
      }
    } catch (e) { /* ignore */ }
  }
  watch(document);
  defMethod(eproto, 'attachShadow', function (orig) {                            // a player in a web component (finance.yahoo.com's): its shadow tree is watched too
    return function () { var root = orig.apply(this, arguments); try { watch(root); } catch (e) { /* ignore */ } return root; };
  });

  // ---------------------------------------------------------------- a real tap on a picture is the person's press
  var down = null;
  window.addEventListener('pointerdown', function (e) { try { if (e.isTrusted && e.isPrimary !== false) { down = { x: e.clientX, y: e.clientY, t: dnow.call(Date) }; lastGesture = down.t; } } catch (x) { /* ignore */ } }, { capture: true, passive: true });
  function targetOf(e) { try { var p = e.composedPath ? e.composedPath() : null; return (p && p.length) ? p[0] : e.target; } catch (x) { return e.target; } }
  window.addEventListener('pointerup', function (e) {
    try {
      if (!e.isTrusted || !down) return;
      var d = down; down = null; lastGesture = dnow.call(Date);
      if (lastGesture - d.t > 600 || Math.abs(e.clientX - d.x) > 14 || Math.abs(e.clientY - d.y) > 14) return;   // a tap, not a swipe or a long press
      var tg = targetOf(e); lastTap.t = lastGesture; lastTap.target = tg;
      pressAt(e.clientX, e.clientY, tg);
    } catch (x) { /* ignore */ }
  }, { capture: true, passive: true });
  window.addEventListener('click', function (e) { try { if (e.isTrusted) { lastGesture = dnow.call(Date); var tg = targetOf(e); lastTap.t = lastGesture; lastTap.target = tg; if (e.detail > 0 || e.clientX || e.clientY) pressAt(e.clientX, e.clientY, tg); } } catch (x) { /* ignore */ } }, { capture: true, passive: true });
  window.addEventListener('keydown', function (e) { try { if (e.isTrusted) { lastGesture = dnow.call(Date); lastTap.t = lastGesture; lastTap.target = targetOf(e); } } catch (x) { /* ignore */ } }, { capture: true, passive: true });

  // ---------------------------------------------------------------- the content script says whether the rule is on
  // the app's answer is there (or too slow: 4 s): what waited is carried out when the rule is OFF, refused when it is ON
  function settle() {
    told = true;
    var w = waiting; waiting = [];
    for (var i = 0; i < w.length; i++) {
      var q = w[i];
      try {
        if (!on) { var p = proto.play.apply(q.el, q.args); if (p && p.then) p.then(q.resolve, q.reject); else q.resolve(); }
        else { N.refused++; touch(); q.reject(new DOMException(REFUSED, 'NotAllowedError')); }
      } catch (x) { try { q.reject(x); } catch (y) { /* ignore */ } }
    }
  }
  try {
    document.addEventListener(CFG_EVENT, function (e) {
      try {
        var d = e && e.detail;
        if (d === '0') { hand = false; if (on) { on = false; openAll('off'); } told = true; settle(); }
        else if (d === '1' || d === '1+') { hand = (d === '1+'); on = true; settle(); }
      } catch (x) { /* keep what was told before */ }
    }, true);
    document.dispatchEvent(new CustomEvent(READY_EVENT));
  } catch (e) { /* ignore */ }
})();
