// VP1 (owner message 49, research/VIDEO_PLAYER_AUDIT.md): "This video player needs an audit and overhaul ... each time I tap the screen it should toggle playback controls shown or hidden, just like other popular
// video players." The page side of Kestrel's OWN full-screen video controls. The controls themselves are drawn by the app (Android views over the engine: never under a system bar, never laid out late by the
// engine, nothing of them in the page), this script is the part that has to live in the page:
//  - it finds the film that is full screen (also inside a web component's shadow tree: KestrelVF) and tells the app its state (`vst`: position, length as the page reports it, what is buffered, paused,
//    muted, volume, speed, ready state, whether the stream is a Media Source one) four times a second while the controls are up and once a second otherwise (a film event brings the state at once); the app answers with what the controls want done
//    (play, pause, seek, skip, mute, speed) and whether they are shown: so there is no second channel into the page;
//  - it takes the taps on the picture: a tap on the film, or on the page's own player controls that Kestrel's replace (play, pause, mute, volume, seek, progress, full screen), is NOT passed on to the page
//    (the page's player would toggle ITS controls, or not: Shaka's default is that a tap does not hide them) but told to the app (`vtap`, the place across the picture): one tap toggles the controls,
//    a double tap on a side seeks, in the middle plays / pauses (the app decides, `core/player/TapMachine`). A tap on anything else the page has (a link, a button such as "Skip ad" or captions, a form
//    field) goes to the page untouched;
//  - a film that goes full screen with a box that is not its shape (the page's `object-fit: fill`, or `cover`) is shown whole (`object-fit: contain`) for as long as it is full screen (owner message 49: "it stretched the video
//    to fill the screen which messed up the aspect ratio").
// It runs only when the app says so (the `guard` answer carries `kp`: Settings "Full-screen video controls"), only while a film is full screen, does nothing in a frame that has none, and does no work at all
// when the page is not full screen. Conservative ES5, passive listeners, numbers only (never an address, no text of the page).
window.KestrelVP = window.KestrelVP || (function () {
  var api = {};
  try {
    var VF = window.KestrelVF;
    if (!VF || !window.addEventListener || !document.createElement) return api;
    var C = { idle: 1000, busy: 250, tapMs: 450, slop: 14, soon: 60, askRetry: 4000 };                  // the clocks (a test shortens them through window.__kstVPcfg)
    try { if (window.__kstVPcfg) for (var key in window.__kstVPcfg) C[key] = window.__kstVPcfg[key]; } catch (e0) { /* optional */ }
    api.c = C;

    // the page's own player controls that Kestrel's controls replace: a tap on one is a tap on the film. NOT in it: skip, next, captions, settings, quality, share, close ... they stay the page's.
    var CTRL = /(^|[^a-z])(play|pause|replay|mute|unmute|volume|seek|scrub|scrubber|progress|timeline|slider|fullscreen|full-screen|exit-fullscreen|duration)/i;
    var EVS = ['pointerdown', 'pointerup', 'pointercancel', 'mousedown', 'mouseup', 'click', 'dblclick', 'touchstart', 'touchend'];
    var FILM_EVENTS = ['play', 'pause', 'playing', 'ended', 'seeked', 'volumechange', 'ratechange', 'durationchange', 'loadedmetadata', 'emptied', 'waiting', 'error'];

    var cfgWait = [];
    var S = null;                       // the session: one full-screen film under Kestrel's controls
    function noop() {}

    // ------------------------------------------------------------------------------------------ what the app says (asked again at every full screen: a Settings change must reach a page that is already open)
    function ask(cb) {
      cfgWait.push(cb);
      if (cfgWait.length > 1) return;
      function done(c) { var w = cfgWait; cfgWait = []; for (var i = 0; i < w.length; i++) { try { w[i](c); } catch (e) { /* never break the page */ } } }
      try {
        browser.runtime.sendMessage({ k: 'guard', h: location.hostname }).then(function (r) { done({ kp: !!(r && r.kp === true) }); }, function () { done({ kp: false }); });
      } catch (e) { done({ kp: false }); }
    }

    // ------------------------------------------------------------------------------------------ the film that is full screen
    function fullscreenEl() {           // the element that is full screen, also when it sits in a shadow tree (document.fullscreenElement names the host)
      var e = document.fullscreenElement, i = 0;
      while (e && i++ < 5) { var sr = VF.rootOf(e); if (sr && sr.fullscreenElement) e = sr.fullscreenElement; else break; }
      return e || null;
    }
    function within(root, el) {         // is el inside root, through shadow trees
      for (var n = el, i = 0; n && i < 40; i++) {
        if (n === root) return true;
        n = n.parentNode;
        if (n && n.nodeType === 11) n = n.host;
      }
      return false;
    }
    function pickVideo(fs) {            // the film of the full-screen box: the video itself, or the biggest one inside it (a playing one wins ties)
      if (!fs) return null;
      if (fs.tagName === 'VIDEO') return fs;
      var best = null, bestA = 0, list;
      try { list = VF.all('video'); } catch (e) { list = []; }
      for (var i = 0; i < list.length; i++) {
        var v = list[i];
        if (!v.isConnected || !within(fs, v)) continue;
        var r = v.getBoundingClientRect(), a = r.width * r.height;
        if (!v.paused) a *= 1.5;
        if (a > bestA) { best = v; bestA = a; }
      }
      return best;
    }

    function onFullscreen() {
      try {
        var fs = fullscreenEl();
        if (!fs) { stop(); return; }
        ask(function (c) {
          if (!c.kp) return;
          var cur = fullscreenEl();
          if (!cur) return;
          var v = pickVideo(cur);
          if (!v) return;                                                     // a full-screen box without a film (a slide show, a game): nothing to control
          if (S && S.v === v) return;
          stop();
          start(v, cur);
        });
      } catch (e) { /* never break the page */ }
    }
    document.addEventListener('fullscreenchange', onFullscreen, true);

    // ------------------------------------------------------------------------------------------ the session
    function start(v, fs) {
      S = { v: v, fs: fs, shown: 0, timer: 0, soonTimer: 0, fit: null, down: null, taps: 0, tapWin: 0, ls: [], reports: 0, gone: [], native: false, awayAt: 0, ack: 0, busyUntil: 0, pickedAt: Date.now() };
      for (var i = 0; i < FILM_EVENTS.length; i++) { v.addEventListener(FILM_EVENTS[i], soon, { passive: true }); S.ls.push(FILM_EVENTS[i]); }
      for (var j = 0; j < EVS.length; j++) window.addEventListener(EVS[j], onEvent, { capture: true, passive: true });
      window.addEventListener('resize', fitSoon, { passive: true });
      fit(v);
      putAway(S);
      report();
      loop();
    }
    function stop() {
      var s = S;
      if (!s) return;
      S = null;
      try { if (s.timer) clearTimeout(s.timer); if (s.soonTimer) clearTimeout(s.soonTimer); } catch (e) { /* ignore */ }
      try { for (var i = 0; i < s.ls.length; i++) s.v.removeEventListener(s.ls[i], soon); } catch (e1) { /* ignore */ }
      try { for (var j = 0; j < EVS.length; j++) window.removeEventListener(EVS[j], onEvent, true); window.removeEventListener('resize', fitSoon); } catch (e2) { /* ignore */ }
      try { unfit(s); } catch (e3) { /* ignore */ }
      try { bringBack(s); } catch (e4) { /* ignore */ }
    }
    api.active = function () { return !!S; };            // videobar.js draws nothing while Kestrel's controls own the film
    api.stop = stop;

    function loop() {
      var s = S;
      if (!s) return;
      s.timer = setTimeout(function () { if (S !== s) return; report(); loop(); }, (s.shown || Date.now() < s.busyUntil) ? C.busy : C.idle);
    }
    function speedUp(s, ms) {                             // a tap, or the app showing its controls: the next answers carry commands, so for a moment the reports come at the quick pace and one comes soon
      s.busyUntil = Date.now() + ms;
      try { if (s.timer) clearTimeout(s.timer); } catch (e) { /* ignore */ }
      loop();
      soon();
    }
    function soon() {                                     // a film event: the state goes soon, once for a burst of them
      var s = S;
      if (!s || s.soonTimer) return;
      s.soonTimer = setTimeout(function () { s.soonTimer = 0; if (S === s) report(); }, C.soon);
    }

    // ------------------------------------------------------------------------------------------ what the page tells (numbers only)
    function r2(x) { return Math.round(x * 100) / 100; }
    function isMse(v) {
      try { var u = v.currentSrc || v.src || ''; return !!v.srcObject || u.indexOf('blob:') === 0; } catch (e) { return false; }
    }
    function sample(v) {
      var d = v.duration, live = 0, dd = -1, b = 0;
      if (d === Infinity) { live = 1; dd = -2; } else if (d > 0) dd = r2(d);
      try { var br = v.buffered; if (br && br.length) b = br.end(br.length - 1); } catch (e) { /* none */ }
      return { k: 'vst', t: r2(v.currentTime || 0), d: dd, b: r2(b), p: v.paused ? 1 : 0, e: v.ended ? 1 : 0, m: (v.muted || v.volume === 0) ? 1 : 0, v: Math.round((v.volume || 0) * 100),
               r: r2(v.playbackRate || 1), rs: v.readyState | 0, ms: isMse(v) ? 1 : 0, l: live, w: v.videoWidth | 0, h: v.videoHeight | 0, ak: S ? S.ack | 0 : 0, aw: S ? S.gone.length : 0 };   // ak = the last command of the app carried out
    }
    function send(m) {
      var s = S;
      try { browser.runtime.sendMessage(m).then(function (r) { if (S === s) answer(r); }, noop); } catch (e) { /* nobody listens */ }
    }
    function report() {
      var s = S;
      if (!s) return;
      if (!s.v.isConnected) { if (!repick(s, true)) stop(); return; }
      s.reports++;
      if (Date.now() - s.pickedAt >= 1000) {                                  // an ad element that ended and a film that plays in another element, a page that swapped its element
        var r0 = null; try { r0 = s.v.getBoundingClientRect(); } catch (e0) { r0 = null; }
        if (s.v.ended || (r0 && (r0.width < 10 || r0.height < 10)) || (s.v.paused && Date.now() - s.pickedAt >= 3000)) { if (repick(s, false)) return; }
        s.pickedAt = Date.now();
      }
      if (!s.fit && s.reports < 8) fit(s.v);                                  // (the picture's size may be known only after the first reports)
      if (Date.now() - s.awayAt >= 900) putAway(s);                           // (a page that makes its controls late, or again)
      send(sample(s.v));
    }
    // the app's answer: { c: [commands], s: shown 1 | 0, on: 0 = the controls are not Kestrel's here }
    function answer(r) {
      var s = S;
      if (!s || !r || typeof r !== 'object') return;
      if (r.on === 0) { stop(); return; }
      if (typeof r.s === 'number') { var was = s.shown; s.shown = r.s ? 1 : 0; if (s.shown && !was) speedUp(s, 0); }
      var c = r.c;
      if (c && c.length) for (var i = 0; i < c.length && i < 8; i++) run(c[i]);
    }
    function repick(s, gone) {                            // the film of this full screen is another element now: the session moves to it (the app's count of carried-out commands goes along)
      var fs = null, v2 = null;
      try { fs = fullscreenEl(); v2 = fs ? pickVideo(fs) : null; } catch (e) { v2 = null; }
      s.pickedAt = Date.now();
      if (!fs || !v2 || v2 === s.v) return false;
      var ack = s.ack;
      stop();
      start(v2, fs);
      if (S) S.ack = ack;
      return true;
    }
    function seekEnd(v) { try { var s = v.seekable; return s && s.length ? s.end(s.length - 1) : 0; } catch (e) { return 0; } }
    function run(c) {
      var s = S, v = s && s.v;
      if (!v || !c) return;
      try {
        switch (c.a) {
          case 'play': { var p = v.play(); if (p && p.catch) p.catch(noop); break; }
          case 'pause': v.pause(); break;
          case 'seek': if (isFinite(c.t)) v.currentTime = Math.max(0, +c.t); break;
          case 'skip': {
            var d = isFinite(c.d) ? +c.d : 0, top = (v.duration > 0 && isFinite(v.duration)) ? v.duration : seekEnd(v), t = (v.currentTime || 0) + d;
            if (top > 0) t = Math.min(t, top);
            v.currentTime = Math.max(0, t);
            break;
          }
          case 'mute': v.muted = !!c.on; if (!c.on && v.volume === 0) v.volume = 1; break;
          case 'rate': { var rate = +c.r; if (rate >= 0.25 && rate <= 4) v.playbackRate = rate; break; }
          default: break;
        }
      } catch (e) { /* the page decides */ }
      if (c.i > s.ack) s.ack = c.i | 0;                                        // (the app numbers its commands: a report that names this one was taken after the film did what was asked)
      soon();
    }

    // ------------------------------------------------------------------------------------------ the taps
    function bigLayer(el) {                              // a layer over (nearly) the whole picture is the film's, not a button
      try { var f = S.fs.getBoundingClientRect(), r = el.getBoundingClientRect(); return r.width >= 0.95 * f.width && r.height >= 0.9 * f.height; } catch (e) { return false; }
    }
    function interactive(el) {
      var t = el.tagName, role = el.getAttribute ? (el.getAttribute('role') || '') : '';
      if ((t === 'A' || t === 'AREA') && el.hasAttribute('href')) return true;
      if (/^(BUTTON|INPUT|SELECT|TEXTAREA|OPTION|LABEL|SUMMARY)$/.test(t)) return true;
      if (/^(link|button|checkbox|radio|menuitem|menuitemcheckbox|menuitemradio|tab|switch|textbox|searchbox|combobox|listbox|option)$/i.test(role)) return true;
      if (el.isContentEditable) return true;
      try {
        if ((el.hasAttribute('onclick') || (el.hasAttribute('tabindex') && el.tabIndex >= 0)) && !bigLayer(el)) return true;
        if (window.getComputedStyle(el).cursor === 'pointer' && !bigLayer(el)) return true;
      } catch (e) { /* not an element with a style */ }
      return false;
    }
    function nameOf(el) {
      var c = el.className; if (c && c.baseVal !== undefined) c = c.baseVal;
      var s = String(c || '') + ' ' + (el.id || '') + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.getAttribute('title') || '') + ' ' + (el.getAttribute('data-testid') || '');
      return s.replace(/([a-z])([A-Z])/g, '$1-$2');
    }
    // 'film' = Kestrel's: the picture, a layer over it, or one of the page's player controls that Kestrel's replace; 'page' = something the page has for the person to use
    function classify(e) {
      var path = e.composedPath ? e.composedPath() : [e.target];
      for (var i = 0; i < path.length && i < 24; i++) {
        var el = path[i];
        if (!el || el.nodeType !== 1) continue;
        if (el === S.fs || el === document.documentElement) break;
        if (interactive(el)) return CTRL.test(nameOf(el)) ? 'film' : 'page';
        if (el.getAttribute && /^(slider|progressbar)$/i.test(el.getAttribute('role') || '')) return 'film';
      }
      return 'film';
    }
    function insideFs(e) {
      var path = e.composedPath ? e.composedPath() : [e.target];
      for (var i = 0; i < path.length && i < 40; i++) if (path[i] === S.fs || path[i] === S.v) return true;
      return false;
    }
    function onEvent(e) {
      try {
        if (!S || !e.isTrusted || !insideFs(e)) return;
        if (classify(e) !== 'film') { S.down = null; return; }                // the page's own button: the page gets it, untouched
        e.stopImmediatePropagation();                                        // the page's player would toggle (or not) ITS controls: Kestrel's are the controls now
        var ty = e.type;
        if (ty === 'pointerdown') {
          S.down = (e.isPrimary === false) ? null : { x: e.clientX, y: e.clientY, t: Date.now(), id: e.pointerId };
        } else if (ty === 'pointerup') {
          var d = S.down; S.down = null;
          if (!d || d.id !== e.pointerId) return;
          if (Date.now() - d.t > C.tapMs || Math.abs(e.clientX - d.x) > C.slop || Math.abs(e.clientY - d.y) > C.slop) return;
          tap(e.clientX, e.clientY);
        } else if (ty === 'pointercancel') S.down = null;
      } catch (x) { /* never break the page */ }
    }
    function tap(x, y) {
      var s = S;
      if (!s) return;
      var now = Date.now();                                                   // at most ten taps a second: a finger cannot, a script should not
      if (now - s.tapWin > 1000) { s.tapWin = now; s.taps = 0; }
      if (++s.taps > 10) return;
      var r = s.fs.getBoundingClientRect();
      var fx = r.width > 0 ? (x - r.left) / r.width : 0.5;
      fx = Math.max(0, Math.min(1, fx));
      send({ k: 'vtap', x: Math.round(fx * 1000) });
      speedUp(s, 1800);
    }

    // ------------------------------------------------------------------------------------------ the page's own controls are put away
    // The page's player draws controls of its own (YouTube's chrome, Shaka's UI, video.js, JW Player, Plyr, MediaElement ...) and a `<video controls>` has the engine's bar: two sets of controls on one screen, one of
    // them not hiding when the person taps (Shaka keeps its bar up on a tap), is what the owner called a screen that is not given to the video. While Kestrel's controls own the film the page's are not shown, and they
    // are given back when full screen ends. Done through the element's own style (the page's content security policy does not stop that) and looked for again every second (a page makes them late, or again). Captions,
    // "Skip ad", next / previous and every other control of the page OUTSIDE those containers stay (a caption or quality button that lives INSIDE a hidden container goes with it: on some sites Settings > Full-screen video controls > the page's own brings them back).
    var CHROME = '.ytp-chrome-top,.ytp-chrome-bottom,.ytp-gradient-top,.ytp-gradient-bottom,.ytp-pause-overlay,.player-controls-top,.player-controls-bottom,.player-controls-background,' +
      '.shaka-controls-container,.shaka-play-button-container,.vjs-control-bar,.vjs-big-play-button,.jw-controls,.jw-controls-backdrop,.plyr__controls,.plyr__control--overlaid,' +
      '.mejs__controls,.mejs__overlay-play,.fp-controls';
    function putAway(s) {
      s.awayAt = Date.now();
      try { if (s.v.controls) { s.native = true; s.v.controls = false; } } catch (e0) { /* the engine's bar stays */ }
      for (var g = 0; g < s.gone.length; g++) {                                // a page that sets the element's display again (inline, without !important) gets it taken away again
        try { var st = s.gone[g].el.style; if (st.getPropertyValue('display') !== 'none' || st.getPropertyPriority('display') !== 'important') st.setProperty('display', 'none', 'important'); } catch (e6) { /* gone */ }
      }
      var roots = [], rn = null;
      try { rn = s.v.getRootNode ? s.v.getRootNode() : null; } catch (e1) { rn = null; }
      if (rn && rn.querySelectorAll) roots.push(rn);
      if (rn !== document && document.querySelectorAll) roots.push(document);
      for (var r = 0; r < roots.length; r++) {
        var list;
        try { list = roots[r].querySelectorAll(CHROME); } catch (e2) { continue; }
        for (var i = 0; i < list.length && s.gone.length < 80; i++) {
          var el = list[i], known = false;
          for (var k = 0; k < s.gone.length; k++) if (s.gone[k].el === el) { known = true; break; }
          if (known || !within(s.fs, el)) continue;
          try {
            s.gone.push({ el: el, old: el.style.getPropertyValue('display'), prio: el.style.getPropertyPriority('display') });
            el.style.setProperty('display', 'none', 'important');
          } catch (e3) { /* not an element with a style */ }
        }
      }
    }
    function bringBack(s) {
      var g = s.gone || [];
      s.gone = [];
      for (var i = 0; i < g.length; i++) {
        try { if (g[i].old) g[i].el.style.setProperty('display', g[i].old, g[i].prio); else g[i].el.style.removeProperty('display'); } catch (e) { /* the page took it away */ }
      }
      try { if (s.native) { s.native = false; s.v.controls = true; } } catch (e5) { /* ignore */ }
    }
    api.away = function () { return S ? S.gone.length : 0; };           // (tests)

    // ------------------------------------------------------------------------------------------ the picture is shown whole
    function fit(v) {
      var s = S;
      try {
        if (!s || s.fit || !v.videoWidth || !v.videoHeight) return;
        var cs = window.getComputedStyle(v);
        if (cs.objectFit !== 'fill' && cs.objectFit !== 'cover') return;      // the page chose contain / scale-down / none: its choice
        var r = v.getBoundingClientRect();
        if (r.width < 10 || r.height < 10) return;
        var pic = v.videoWidth / v.videoHeight, box = r.width / r.height;
        if (Math.abs(pic - box) / pic < 0.03) return;                         // the box has the picture's shape: nothing to fix
        s.fit = { v: v, old: v.style.getPropertyValue('object-fit'), prio: v.style.getPropertyPriority('object-fit') };
        v.style.setProperty('object-fit', 'contain', 'important');
      } catch (e) { /* never break the page */ }
    }
    function unfit(s) {
      var f = s && s.fit;
      if (!f) return;
      s.fit = null;
      if (f.old) f.v.style.setProperty('object-fit', f.old, f.prio); else f.v.style.removeProperty('object-fit');
    }
    var fitTimer = 0;
    function fitSoon() { if (fitTimer || !S) return; fitTimer = setTimeout(function () { fitTimer = 0; if (S) fit(S.v); }, 200); }
  } catch (e) { /* never break the page */ }
  return api;
})();
