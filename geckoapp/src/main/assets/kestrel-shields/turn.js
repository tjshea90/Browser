// FB2 / FB3 (owner message 47): "The video didn't pause when switching orientation. When switching orientation, it is laggy and for a few seconds the video is the wrong size in either mode. Make it pause the video
// and properly set up the size and orientation smoothly then resume."
// The app pauses a video that makes sound through the engine's media session (alpha49), but the engine opens a media session only for AUDIBLE media: the muted autoplay video of finance.yahoo.com got nothing.
// So the page does it, for EVERY <video> of the page (also inside shadow roots: vfind.js) and of every frame:
//  - when the screen turns (the orientation of the window flips: a `resize` / `orientationchange` / screen.orientation `change` event that comes with a different shape; a keyboard or a toolbar does not flip it),
//    every video that plays is paused (the decoder and the compositor then only have the new layout to do);
//  - the turn is over when no event of the turn came for 650 ms (at the latest 2.2 s after it began); then the page gets ONE more `resize` event (a player that sized itself from the viewport while it was still
//    turning, which is the "wrong size for a few seconds", redoes its layout from the final one), and 80 ms later the videos that WE paused play again;
//  - a video that is not paused by us is never started, one the person (or the page) started again meanwhile is left alone, and a touch during the turn is the person taking over: nothing is played for them.
// Numbers go to the app as `vbk` (e = 'turn', a = played | skipped | none, n = how many videos), never an address. Conservative ES5, passive listeners, no work until the screen turns.
// FB4 (lab run 43760546128): the engine sizes a page that goes full screen to the DISPLAY (1024 px) while the view between the system bars is 947 px, and keeps it so until the view changes size again. The top frame
// therefore tells the app the viewport it really got (`vpt`: width and height in device pixels, only while the page is full screen, 200 ms after the last resize / fullscreenchange); the app compares it with
// its view and, when they differ, gives the engine a real size change. Numbers only.
// ROT2 (owner message 48, two screenshots: after a turn the page keeps the layout of the OLD orientation for a few seconds): the same report is what lets the app keep its black cover and the pause until the
// page has the new size, so the top frame also sends ONE report (f = 0 when not full screen) a second after the page is up: the app then knows this page can say when it has a turn's new size.
(function () {
  try {
    var VF = window.KestrelVF;
    if (!VF || !window.addEventListener || window.__kestrelTurn) return;
    window.__kestrelTurn = true;                        // (a second load of the script in the same frame keeps the first)
    var T = { settle: 650, cap: 2200, tick: 120, nudge: 80, vp: 200, hello: 1000 };
    try { if (window.__kstTurnCfg) for (var key in window.__kstTurnCfg) T[key] = window.__kstTurnCfg[key]; } catch (e0) { /* optional */ }

    var TOP = window.top === window;
    function landscapeNow() {
      // the top window: its own shape. A frame cannot see the shape of the page above it, but the screen's orientation is the same for all of them
      if (!TOP) { try { var o = screen.orientation && screen.orientation.type; if (typeof o === 'string' && o) return o.indexOf('landscape') === 0; } catch (e) { /* no orientation API */ } }
      return window.innerWidth > window.innerHeight;
    }
    var landscape = landscapeNow();
    var state = null, nudging = false;

    function report(a, n, ms) {
      try { browser.runtime.sendMessage({ k: 'vbk', e: 'turn', a: a, n: Math.min(99, n) }).catch(function () {}); } catch (e) { /* nobody listens */ }
    }
    function playingVideos() {
      var out = [];
      try {
        var list = VF.all('video');
        for (var i = 0; i < list.length && out.length < 12; i++) {
          var v = list[i];
          if (v.isConnected && !v.paused && !v.ended && v.readyState >= 2) out.push(v);
        }
      } catch (e) { /* never break the page */ }
      return out;
    }
    function begin() {
      var now = Date.now();
      state = { t0: now, last: now, paused: [], touched: false, timer: 0 };
      var vids = playingVideos();
      for (var i = 0; i < vids.length; i++) { try { vids[i].pause(); state.paused.push(vids[i]); } catch (e) { /* the page decides */ } }
      state.timer = setInterval(tick, T.tick);
    }
    function tick() {
      var s = state;
      if (!s) return;
      var now = Date.now();
      if (now - s.last >= T.settle || now - s.t0 >= T.cap) finish();
    }
    function finish() {
      var s = state; state = null;
      landscape = landscapeNow();                         // (review of alpha53: a turn and back inside one window left the first shape here, and the next unrelated resize then looked like a flip)
      try { clearInterval(s.timer); } catch (e) { /* ignore */ }
      try { nudging = true; window.dispatchEvent(new Event('resize')); } catch (e) { /* no synthetic events */ } finally { nudging = false; }
      setTimeout(function () {
        var played = 0, skipped = 0;
        for (var i = 0; i < s.paused.length; i++) {
          var v = s.paused[i];
          try {
            if (s.touched || !v.isConnected || !v.paused || v.ended) { skipped++; continue; }
            var p = v.play(); if (p && p.catch) p.catch(function () {});
            played++;
          } catch (e) { skipped++; }
        }
        report(s.paused.length === 0 ? 'none' : (played > 0 ? 'played' : 'skipped'), s.paused.length, Date.now() - s.t0);
      }, T.nudge);
    }
    function changed() {
      if (nudging) return;                                // our own resize event is nobody's turn
      var now = landscapeNow();
      if (state) { state.last = Date.now(); return; }     // the turn goes on
      if (now === landscape) return;                      // a keyboard, a toolbar, a split screen: the shape is the same
      landscape = now;
      begin();
    }
    // FB4: the size the engine really gave this page, told to the app while the page is full screen (the app corrects a viewport that is not its view's)
    var vpTimer = 0;
    function sendViewport(f) {
      try {
        var dpr = window.devicePixelRatio || 1;
        browser.runtime.sendMessage({ k: 'vpt', w: Math.round(window.innerWidth * dpr), h: Math.round(window.innerHeight * dpr), f: f }).catch(function () {});
      } catch (e) { /* nobody listens */ }
    }
    function tellViewport() {
      if (!TOP) return;
      if (vpTimer) clearTimeout(vpTimer);
      vpTimer = setTimeout(function () {
        vpTimer = 0;
        try {
          if (!document.fullscreenElement) return;
          sendViewport(1);
        } catch (e) { /* never break the page */ }
      }, T.vp);
    }
    // ROT2: "this page reports its viewport": one report soon after the page is up
    if (TOP) setTimeout(function () { try { sendViewport(document.fullscreenElement ? 1 : 0); } catch (e) { /* never break the page */ } }, T.hello);
    window.addEventListener('resize', tellViewport, { passive: true });
    document.addEventListener('fullscreenchange', tellViewport, true);
    window.addEventListener('resize', changed, { passive: true });
    window.addEventListener('orientationchange', changed, { passive: true });
    try { if (screen.orientation && screen.orientation.addEventListener) screen.orientation.addEventListener('change', changed); } catch (e1) { /* no orientation API */ }
    window.addEventListener('pointerdown', function (e) { if (state && e.isTrusted) state.touched = true; }, { capture: true, passive: true });
    document.addEventListener('visibilitychange', function () {   // the app was left during a turn: nothing is played (the engine suspends media itself)
      if (document.hidden && state) { try { clearInterval(state.timer); } catch (e) { /* ignore */ } state = null; landscape = landscapeNow(); }
    }, true);
  } catch (e) { /* never break the page */ }
})();
