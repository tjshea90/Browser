'use strict';
// Kestrel's page-side video format policy (ANS52g, research/VIDEO_ACCELERATION.md section "ANS52g design"). Runs INSIDE the page (world MAIN, every frame, document_start) because a player asks the engine what it
// can play with three calls, and only a script that is there before the player's own can answer for the engine:
//   MediaSource.isTypeSupported(type)               (MSE players: YouTube, hls.js, dash.js, Shaka ...)
//   HTMLMediaElement.prototype.canPlayType(type)    (simple players, feature checks)
//   navigator.mediaCapabilities.decodingInfo(cfg)   (newer players)
// The engine answers yes to VP9 whenever it can decode it at all, and on the tablet it does so whatever its own preference says (Gecko forces VP9 when a hardware decoder exists), also for a stream the chip
// cannot decode (VP9 above 1080p30), which then runs in software next to the page and stutters. This script says NO for exactly what the app tells it to refuse and passes everything else to the engine:
//   policy = "<mode><fps30>|<avc>|<hevc>|<vp9>|<av1>"   (told by the content script, which gets it from the app)
//   mode 0 = Automatic: a codec is refused ABOVE what the device's hardware decoder declares for it (a size, and macroblocks per second = ceil(w/16) * ceil(h/16) * fps);
//          a codec that has no hardware decoder here (written "-") is left to the engine (its own preferences work then);
//   mode 1 = H.264 only: every other video codec is refused (H.264 itself only above its hardware limit);
//   mode 2 = everything: no codec is refused;
//   fps30  = 1: any video with a stated frame rate above 30 is refused, whatever the mode.
//   each codec is "-" or "<longest side>x<shortest side>@<macroblocks per second>".
// What a stream is: the size and the frame rate come from the hints a site puts into the type (`width=1920; height=1080; framerate=60`, the convention of YouTube and of the TV players that Enhanced h264ify
// reads too) or from the mediaCapabilities video configuration. A question without them (no size, no rate) is NEVER refused for its size: a site that offers only VP9, and asks without a size, keeps working.
// Until the policy is known (it arrives a few tens of milliseconds after the page starts) the script answers like the engine: a policy that is not known is never applied. What it refused is counted and told to the
// content script (an event with numbers and a short word, never an address), which relays it to Diagnostics. Conservative ES5, nothing runs until a player asks.
// Tested in headless Chromium by web-tests/codec.test.js (the logic is plain DOM and the same in Gecko).
(function () {
  var CFG_EVENT = '__kst_codec_cfg_v1', REP_EVENT = '__kst_codec_rep_v1', READY_EVENT = '__kst_codec_ready_v1';
  try {
    if (typeof browser !== 'undefined' && browser && browser.runtime) return; // declared in the manifest as MAIN world; a copy that ever ran in the isolated world (extension API present) must do nothing there
    if (window.__kstCodec) return;
    Object.defineProperty(window, '__kstCodec', { value: 1 });
  } catch (e) { return; }

  var later = window.setTimeout;     // the page's own copy of the timer may be replaced later: this one is ours
  var policy = null;                 // null = not told yet (or told nonsense): every answer is the engine's own
  var cache = {};                    // type string -> verdict ('' = not refused), so a player that asks the same thing a hundred times costs one parse; emptied when the policy changes
  var cacheSize = 0;
  var asked = 0, refusedSize = 0, refusedMode = 0, refusedFps = 0, lastWord = '', reports = 0, timer = 0;

  // ---------------------------------------------------------------- the policy
  function parseLimit(s) {
    var m = /^(\d{1,5})x(\d{1,5})@(\d{1,9})$/.exec(s);
    return m ? { hi: +m[1], lo: +m[2], bps: +m[3] } : null;
  }
  function parsePolicy(s) {
    if (typeof s !== 'string' || s.length > 160) return null;
    var p = s.split('|');
    if (p.length !== 5 || !/^[012][01]$/.test(p[0])) return null;
    return { mode: +p[0].charAt(0), fps30: p[0].charAt(1) === '1', avc: parseLimit(p[1]), hevc: parseLimit(p[2]), vp9: parseLimit(p[3]), av1: parseLimit(p[4]) };
  }

  // ---------------------------------------------------------------- what a type string says
  // the first VIDEO codec named in codecs="...": 'avc', 'hevc', 'vp9', 'av1', 'vp8', or '' when the type names none; 'webm' for the bare container (VP8, VP9 or AV1, never H.264)
  var CODECS = /codecs\s*=\s*(?:"([^"]*)"|'([^']*)'|([^;\s]*))/i;
  function kindOf(lower) {
    var m = CODECS.exec(lower);
    if (m) {
      var list = (m[1] || m[2] || m[3] || '').split(',');
      for (var i = 0; i < list.length; i++) {
        var c = list[i].replace(/^\s+/, '');
        if (c.indexOf('avc1') === 0 || c.indexOf('avc3') === 0) return 'avc';
        if (c.indexOf('hvc1') === 0 || c.indexOf('hev1') === 0 || c.indexOf('dvh1') === 0 || c.indexOf('dvhe') === 0) return 'hevc';
        if (c.indexOf('vp09') === 0 || c.indexOf('vp9') === 0) return 'vp9';
        if (c.indexOf('vp08') === 0 || c.indexOf('vp8') === 0) return 'vp8';
        if (c.indexOf('av01') === 0 || c.indexOf('av1') === 0) return 'av1';
      }
      return '';
    }
    return (lower.indexOf('video/webm') === 0) ? 'webm' : '';
  }
  function hint(lower, name) {
    var m = new RegExp(';\\s*' + name + '\\s*=\\s*"?(\\d+(?:\\.\\d+)?)').exec(lower);
    return m ? +m[1] : 0;
  }
  function over(lim, w, h, fps) {
    if (!(w > 0 && h > 0)) return false;                      // no size told: nothing to judge
    var hi = Math.max(w, h), lo = Math.min(w, h);
    if (hi > lim.hi || lo > lim.lo) return true;
    var blocks = Math.ceil(w / 16) * Math.ceil(h / 16);
    return blocks * (fps > 0 ? fps : 30) > lim.bps * 1.03;     // no rate told: 30 (so 1080p30 passes where the chip does 1080p30, and a 1080p60 question that hides its rate is not caught here)
  }
  // '' = the engine may answer; otherwise why the answer is no: 'size' (above the hardware limit), 'mode' (H.264 only), 'fps' (the 30 a second switch)
  function decide(kind, w, h, fps) {
    var p = policy;
    if (!p || !kind) return '';
    if (p.fps30 && fps > 30.5) return 'fps';
    if (p.mode === 2) return '';
    if (p.mode === 1) {
      if (kind !== 'avc') return 'mode';
    }
    var lim = (kind === 'avc' || kind === 'hevc' || kind === 'vp9' || kind === 'av1') ? p[kind] : null;
    if (lim && over(lim, w, h, fps)) return 'size';
    return '';
  }
  function judgeType(type) {
    var key = type.length > 400 ? type.slice(0, 400) : type, hit = cache[key];
    if (hit !== undefined) return hit;
    var lower = key.toLowerCase(), kind = kindOf(lower), v = '';
    if (kind) {
      var w = hint(lower, 'width'), h = hint(lower, 'height'), f = hint(lower, 'framerate');
      v = decide(kind, w, h, f);
      if (v) v = v + '|' + kind + (w > 0 && h > 0 ? ' ' + w + 'x' + h : '') + (f > 0 ? '@' + (Math.round(f * 10) / 10) : '');
    }
    if (cacheSize >= 200) { cache = {}; cacheSize = 0; }
    cache[key] = v; cacheSize++;
    return v;
  }
  function rateOf(r) {                                          // a frame rate in a configuration: a number, or a fraction such as "30000/1001"
    if (typeof r === 'number') return r > 0 && r < 1000 ? r : 0;
    if (typeof r !== 'string') return 0;
    var m = /^\s*(\d+(?:\.\d+)?)\s*(?:\/\s*(\d+(?:\.\d+)?))?\s*$/.exec(r);
    if (!m) return 0;
    var v = m[2] ? (+m[1]) / (+m[2]) : +m[1];
    return v > 0 && v < 1000 ? v : 0;
  }
  function numOf(x) { return (typeof x === 'number' && x > 0 && x < 100000) ? x : 0; }

  // ---------------------------------------------------------------- what was refused, told to the content script (numbers and a word, throttled)
  function flush() {
    timer = 0;
    if (reports >= 8 || (!refusedSize && !refusedMode && !refusedFps)) return;
    reports++;
    var detail = asked + '|' + refusedSize + '|' + refusedMode + '|' + refusedFps + '|' + lastWord;
    asked = 0; refusedSize = 0; refusedMode = 0; refusedFps = 0;
    try { document.dispatchEvent(new CustomEvent(REP_EVENT, { detail: detail })); } catch (e) { /* ignore */ }
  }
  function refuse(verdict) {
    var c = verdict.indexOf('|'), why = verdict.slice(0, c);
    lastWord = verdict.slice(c + 1).slice(0, 40);
    if (why === 'size') refusedSize++; else if (why === 'mode') refusedMode++; else refusedFps++;
    if (!timer && reports < 8) { try { timer = later.call(window, flush, 2000); } catch (e) { /* no timers */ } }
  }

  // ---------------------------------------------------------------- the wrappers
  function mask(fn, name, orig) {                               // looks like the engine's own function to a page that prints it
    try {
      Object.defineProperty(fn, 'name', { value: name, configurable: true });
      Object.defineProperty(fn, 'length', { value: orig.length, configurable: true });
      Object.defineProperty(fn, 'toString', { value: function toString() { return 'function ' + name + '() { [native code] }'; }, configurable: true, writable: true });
    } catch (e) { /* the wrapper still works */ }
  }
  function replace(obj, name, make) {
    try {
      if (!obj) return;
      var d = Object.getOwnPropertyDescriptor(obj, name);
      if (!d || typeof d.value !== 'function') return;
      var wrapped = make(d.value);
      mask(wrapped, name, d.value);
      d.value = wrapped;
      Object.defineProperty(obj, name, d);
    } catch (e) { /* never break the page */ }
  }
  function asker(orig, no) {                                    // isTypeSupported and canPlayType: a string question, answered `no` when refused
    return function () {
      var t = arguments[0];
      if (typeof t === 'string' && policy) {
        asked++;
        var v = '';
        try { v = judgeType(t); } catch (e) { v = ''; }
        if (v) { refuse(v); return no; }
      }
      return orig.apply(this, arguments);
    };
  }
  try {
    if (typeof MediaSource !== 'undefined') replace(MediaSource, 'isTypeSupported', function (orig) { return asker(orig, false); });
    if (typeof ManagedMediaSource !== 'undefined') replace(ManagedMediaSource, 'isTypeSupported', function (orig) { return asker(orig, false); });
    if (typeof HTMLMediaElement !== 'undefined') replace(HTMLMediaElement.prototype, 'canPlayType', function (orig) { return asker(orig, ''); });
    if (typeof MediaCapabilities !== 'undefined') {
      replace(MediaCapabilities.prototype, 'decodingInfo', function (orig) {
        return function (cfg) {
          try {
            var v = cfg && cfg.video;
            if (policy && v && typeof v.contentType === 'string') {
              asked++;
              var lower = v.contentType.slice(0, 400).toLowerCase(), kind = kindOf(lower);
              if (kind) {
                var w = numOf(v.width), h = numOf(v.height), f = rateOf(v.framerate);
                var why = decide(kind, w, h, f);
                if (why) {
                  refuse(why + '|' + kind + (w > 0 && h > 0 ? ' ' + w + 'x' + h : '') + (f > 0 ? '@' + (Math.round(f * 10) / 10) : ''));
                  return Promise.resolve({ supported: false, smooth: false, powerEfficient: false });
                }
              }
            }
          } catch (e) { /* the engine answers */ }
          return orig.apply(this, arguments);
        };
      });
    }
  } catch (e) { /* never break the page */ }

  // ---------------------------------------------------------------- the policy comes from the content script
  try {
    document.addEventListener(CFG_EVENT, function (e) {
      try {
        var p = parsePolicy(e && e.detail);
        policy = p; cache = {}; cacheSize = 0;
      } catch (x) { /* keep what was told before */ }
    }, true);
    document.dispatchEvent(new CustomEvent(READY_EVENT));
  } catch (e) { /* ignore */ }
})();
