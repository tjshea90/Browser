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
// A stream (`srcObject`: a camera, a call) is never held. Until the content script says otherwise the gate is CLOSED: the person's rule is the default.
// What it did is counted (numbers only) and handed to the content script, which relays it to Diagnostics. Conservative ES5; nothing runs until a page touches a media element.
// Tested in headless Chromium by web-tests/gate.test.js (the logic is plain DOM and the same in Gecko).
(function () {
  var CFG_EVENT = '__kst_gate_cfg_v1', OPEN_EVENT = '__kst_gate_v1', REP_EVENT = '__kst_gate_rep_v1', READY_EVENT = '__kst_gate_ready_v1';
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
  var REFUSED = 'The play method is not allowed by the user agent or the platform in the current context, possibly because the user denied permission.';

  function rec(el) { var r = recs.get(el); if (!r) { r = { open: false, src: null, hasSrc: false, preload: null, hasPreload: false, autoplay: false, hasAutoplay: false, load: false, tracked: false, adopted: false, forced: false }; recs.set(el, r); } return r; }
  function peek(el) { try { return recs.get(el); } catch (e) { return undefined; } }
  function isBlob(v) { return typeof v === 'string' ? /^blob:/i.test(v) : (v !== null && v !== undefined && /^blob:/i.test(String(v))); }
  function isMediaEl(x) { return !!x && x.nodeType === 1 && (x.nodeName === 'VIDEO' || x.nodeName === 'AUDIO'); }

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
    try { var ua = navigator.userActivation; if (ua && typeof ua.isActive === 'boolean') return ua.isActive; } catch (e) { /* an older engine */ }
    return dnow.call(Date) - lastGesture < 1500;
  }

  // ---------------------------------------------------------------- the originals
  var oSetAttr = eproto.setAttribute, oRemoveAttr = eproto.removeAttribute, oGetAttr = eproto.getAttribute, oHasAttr = eproto.hasAttribute;
  var oLoad = proto.load;
  var dSrc = null, dPreload = null, dAutoplay = null;

  function track(el) {
    var r = rec(el);
    if (r.tracked) return;
    r.tracked = true;
    if (gated.length >= 60) { var keep = []; for (var i = 0; i < gated.length; i++) if (gated[i].isConnected && !rec(gated[i]).open) keep.push(gated[i]); gated = keep; }
    if (gated.length < 60) gated.push(el);
    try { el.addEventListener(OPEN_EVENT, function () { openEl(el, 'kestrel'); }, true); } catch (e) { /* ignore */ }   // Kestrel's own controls, also for a film inside a shadow tree
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
      // the person's rule is OFF (Settings): the page's own autoplay comes back, as in any other browser (lab run 43812820865: a muted hero loop loaded but never started with the rule off). A press on the picture, the page's own play()
      // and Kestrel's own controls start nothing by this: the person pressed a play button, or the page plays at the person's request; a tap on a hero loop that has no play button is not a request to play it.
      if (r.hasAutoplay) { var wantAuto = r.autoplay; r.hasAutoplay = false; if (why === 'off' && wantAuto) { if (dAutoplay) dAutoplay.set.call(el, true); else oSetAttr.call(el, 'autoplay', ''); } }
    } catch (e) { /* the page decides */ }
    touch();
  }
  function openAll(why) { for (var i = 0; i < gated.length; i++) { try { openEl(gated[i], why); } catch (e) { /* ignore */ } } }

  function pressAt(x, y) {
    for (var i = 0; i < gated.length; i++) {
      var el = gated[i];
      try {
        if (!el.isConnected || rec(el).open) continue;
        var rc = el.getBoundingClientRect();
        if (rc.width >= 80 && rc.height >= 45 && x >= rc.left - 4 && x <= rc.right + 4 && y >= rc.top - 4 && y <= rc.bottom + 4) openEl(el, 'press');
      } catch (e) { /* ignore */ }
    }
  }

  // ---------------------------------------------------------------- the replacements
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
    function (s) { return function (v) { try { if (v && isMediaEl(this)) rec(this).open = true; } catch (e) { /* ignore */ } return s.call(this, v); }; });   // a stream (camera, call) is never held

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
  defMethod(proto, 'play', function (orig) {
    return function () {
      try {
        if (on && isMediaEl(this)) {
          var r = rec(this);
          if (!r.open) {
            if (!r.adopted) adopt(this);
            track(this);
            if (activation()) openEl(this, 'play');
            else { N.refused++; touch(); return Promise.reject(new DOMException(REFUSED, 'NotAllowedError')); }
          }
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
        var list = node.querySelectorAll('video,audio');
        for (var i = 0; i < list.length && i < 60; i++) adopt(list[i]);
      }
    } catch (e) { /* never break the page */ }
  }
  var observer = null;
  function watch(root) {
    try {
      if (!observer && typeof MutationObserver === 'function') {
        observer = new MutationObserver(function (records) {
          for (var i = 0; i < records.length; i++) { var add = records[i].addedNodes; for (var j = 0; j < add.length && j < 400; j++) scan(add[j]); }
        });
      }
      if (observer) {
        observer.observe(root, { childList: true, subtree: true });
        var list = root.querySelectorAll ? root.querySelectorAll('video,audio') : [];      // what is in the tree already (a shadow root that is filled before it is watched)
        for (var i = 0; i < list.length && i < 60; i++) adopt(list[i]);
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
  window.addEventListener('pointerup', function (e) {
    try {
      if (!e.isTrusted || !down) return;
      var d = down; down = null; lastGesture = dnow.call(Date);
      if (lastGesture - d.t > 600 || Math.abs(e.clientX - d.x) > 14 || Math.abs(e.clientY - d.y) > 14) return;   // a tap, not a swipe or a long press
      pressAt(e.clientX, e.clientY);
    } catch (x) { /* ignore */ }
  }, { capture: true, passive: true });
  window.addEventListener('click', function (e) { try { if (e.isTrusted) { lastGesture = dnow.call(Date); if (e.detail > 0 || e.clientX || e.clientY) pressAt(e.clientX, e.clientY); } } catch (x) { /* ignore */ } }, { capture: true, passive: true });
  window.addEventListener('keydown', function (e) { try { if (e.isTrusted) lastGesture = dnow.call(Date); } catch (x) { /* ignore */ } }, { capture: true, passive: true });

  // ---------------------------------------------------------------- the content script says whether the rule is on
  try {
    document.addEventListener(CFG_EVENT, function (e) {
      try {
        var d = e && e.detail;
        if (d === '0') { if (on) { on = false; openAll('off'); } }
        else if (d === '1') on = true;
      } catch (x) { /* keep what was told before */ }
    }, true);
    document.dispatchEvent(new CustomEvent(READY_EVENT));
  } catch (e) { /* ignore */ }
})();
