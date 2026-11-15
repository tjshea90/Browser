// VC1 (owner message 44, research/VIDEO_CONTROLS.md): "I played a video on this site, and the player had no controls at all. I couldn't pause or stop the video or seek ... Anywhere I can start a video in the
// browser, I should be able to control it." A tap on a video is judged 450 ms later: when the page then shows no control of its own over that video (no native `controls`, no visible button or progress
// bar with a control-like name), Kestrel draws its own bar on top of the page: play / pause, a seek track you can tap and drag, the time, mute and full screen. It finds the video through KestrelVF
// (vfind.js: also inside shadow roots and under layers that take no pointer events, which is how finance.yahoo.com builds its player) and works inside frames: a tap that lands in a FRAME that lies over
// a video of the page (the ad overlays of Google IMA do) is relayed by the background to the top frame, which looks for the video by the frame's size.
// The bar is a closed shadow root with `position: fixed` on the document element (above frames and the page's layers), built with DOM calls and CSSOM only (a page's content-security-policy cannot
// block that), hides after 4 s, and is never drawn on a page that has controls of its own unless the Settings row says "Always". Conservative ES5, passive listeners, no work until a tap.
// Settings: the answer to the `guard` question carries `vb` (Automatic or Always) and `vba` (Always); nothing is done unless `vb` is exactly true. Numbers go to the app as `vbk` (never an address).
window.KestrelVB = window.KestrelVB || (function () {
  var api = {};
  try {
    var VF = window.KestrelVF;
    if (!VF || !window.addEventListener || !document.createElement) return api;
    var TOP = window.top === window;
    var SVGNS = 'http://www.w3.org/2000/svg';
    // tap: shorter than the engine's long press (500 ms): a press released at 500-600 ms was a video menu AND a tap, and the bar came up behind the sheet (RW5)
    var T = { judge: 450, late: 1700, hide: 4000, hidePaused: 8000, drag: 140, tap: 450, pre: 50 };    // the clocks (a test shortens them through window.__kstVBcfg)
    try { if (window.__kstVBcfg) for (var key in window.__kstVBcfg) T[key] = window.__kstVBcfg[key]; } catch (e0) { /* optional */ }
    api.t = T;

    var cfg = null, cfgWait = [], bar = null, down = null, judgeTimer = 0, lateTimer = 0, ownFor = null;

    // ------------------------------------------------------------------------------------------ what the app says (once per page)
    // RW11: an answer that did not come (the background page was busy or gone) is given to the waiting taps as "off" but NOT kept: the next tap asks again
    function ask(cb) {
      if (cfg) { cb(cfg); return; }
      cfgWait.push(cb);
      if (cfgWait.length > 1) return;
      function done(c, keep) { if (keep) cfg = c; var w = cfgWait; cfgWait = []; for (var i = 0; i < w.length; i++) { try { w[i](c); } catch (e) { /* never break the page */ } } }
      try {
        browser.runtime.sendMessage({ k: 'guard', h: location.hostname }).then(function (r) { done({ on: !!(r && r.vb === true), always: !!(r && r.vba === true) }, !!r); }, function () { done({ on: false, always: false }, false); });
      } catch (e) { done({ on: false, always: false }, false); }
    }
    function report(e, a) { try { browser.runtime.sendMessage({ k: 'vbk', e: e, a: a || '' }).catch(function () {}); } catch (x) { /* nobody listens */ } }

    // ------------------------------------------------------------------------------------------ does the page show a control of its own over the video?
    var CTRL = /(^|[^a-z])(play|pause|replay|mute|unmute|volume|seek|scrub|progress|timeline|slider|fullscreen|full-screen|duration|controls?|skip|forward|rewind)/i;
    var CAND = 'button,[role="button"],[role="slider"],[role="progressbar"],input,progress,[tabindex],a,[class*="progress"],[class*="seek"],[class*="scrub"],[class*="timeline"],[class*="slider"],[class*="volume"],[class*="fullscreen"],[class*="play"],[class*="control"]';
    function nameOf(el) {
      var c = el.className; if (c && c.baseVal !== undefined) c = c.baseVal;      // an svg's className is an object
      var s = String(c || '') + ' ' + (el.id || '') + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.getAttribute('title') || '') + ' ' + (el.getAttribute('data-testid') || '');
      return s.replace(/([a-z])([A-Z])/g, '$1-$2');                                // playButton -> play-Button
    }
    function shown(el) {   // a finger could use it: displayed, not hidden, not (nearly) transparent counting the boxes around it, takes touches
      var op = 1, n = el, i = 0;
      while (n && n.nodeType === 1 && i++ < 10) {
        var cs = window.getComputedStyle(n);
        if (cs.display === 'none' || cs.visibility === 'hidden') return false;
        op *= parseFloat(cs.opacity);
        if (!(op >= 0.25)) return false;
        n = n.parentNode; if (n && n.nodeType === 11) n = n.host;
      }
      return window.getComputedStyle(el).pointerEvents !== 'none';
    }
    // RW3 (second review): the page's own controls are looked for where a player keeps them, not in the first 1500 candidates of the whole document (an article with 1700 links in front of a Video.js
    // player never got to its play button). The places, nearest first: the shadow trees and boxes around the film (up to eight levels, a whole web component at once), the shadow trees of whatever lies over
    // the middle of the picture (a web-component overlay), and a few points over the picture and below it (a control that is not in the film's box but lies over it).
    function scopesOf(v) {
      var out = [], n = v, hops = 0;
      while (n && hops++ < 8) {
        var p = n.parentNode;
        if (!p) break;
        if (p.nodeType === 11) { out.push(p); n = p.host; continue; }            // the top of a shadow tree: all of it is the player's
        if (p.nodeType !== 1 || p === document.documentElement) break;
        out.push(p); n = p;
        if (p === document.body) break;                                          // the body is the last box, and only when the film sits near the top of the page's boxes
      }
      return out;
    }
    function overlayRoots(r, have) {
      var roots = [];
      try {
        var els = document.elementsFromPoint(r.left + r.width / 2, r.top + r.height / 2);
        for (var i = 0; i < els.length && i < 24; i++) { var sr = VF.rootOf(els[i]); if (sr && roots.indexOf(sr) < 0 && have.indexOf(sr) < 0) roots.push(sr); }
      } catch (e) { /* ignore */ }
      return roots;
    }
    // one element: is it a control a finger could use over / next to the film? (range / progress / slider by tag or role, at least 40 px wide; a button by tag, role or pointer cursor with a name like play /
    // pause / mute / seek / full screen, at least 14 px each way, not a layer over the whole picture). A container whose class says "controls" is nothing: Yahoo's player has one that is 5 px tall while its ad plays.
    function isControl(el, v, r) {
      if (el === v || el.tagName === 'VIDEO' || (bar && el === bar.host)) return false;
      var nm = nameOf(el), tag = el.tagName, role = el.getAttribute('role') || '', type = String(el.type || '').toLowerCase();
      var rangeLike = (tag === 'INPUT' && type === 'range') || tag === 'PROGRESS' || /^(slider|progressbar)$/i.test(role);
      var named = CTRL.test(nm);
      if (!rangeLike && !named) return false;                                    // (cheap exit before any geometry: most elements are neither)
      var cr = el.getBoundingClientRect();
      if (cr.width < 10 || cr.height < 3 || cr.right < r.left - 4 || cr.left > r.right + 4 || cr.bottom < r.top - 4 || cr.top > r.bottom + 80) return false;   // players that keep the bar BELOW the picture count too
      if (cr.width * cr.height >= 0.85 * r.width * r.height) return false;       // a layer over the whole picture takes taps, it is not a control
      var range = rangeLike && cr.width >= 40;
      var semantic = tag === 'BUTTON' || /^button$/i.test(role) || (tag === 'INPUT' && (type === 'button' || type === 'submit'));
      var pointer = false;
      if (named && !semantic && !range) { try { pointer = window.getComputedStyle(el).cursor === 'pointer'; } catch (e) { /* no style */ } }
      var btn = named && (semantic || pointer) && cr.width >= 14 && cr.height >= 14;
      if (!(range || btn)) return false;
      return shown(el);
    }
    function scanList(list, skipIn, v, r) {
      var n = 0;
      for (var j = 0; j < list.length && n < 400; j++) {
        var el = list[j];
        if (skipIn && skipIn.contains && skipIn.contains(el)) continue;           // (already looked at one level down)
        n++;
        if (isControl(el, v, r)) return true;
      }
      return false;
    }
    function ownControls(v, r) {
      try {
        if (v.controls) return true;
        var levels = scopesOf(v), prev = null, i, list;
        for (i = 0; i < levels.length; i++) {
          try { list = levels[i].querySelectorAll(CAND); } catch (e) { continue; }
          if (scanList(list, prev, v, r)) return true;
          if (list.length > 600) break;                                          // a box that holds half the page: no further levels (the points below look at what lies over the picture)
          prev = levels[i];
        }
        var extra = overlayRoots(r, levels);
        for (i = 0; i < extra.length; i++) {
          try { list = extra[i].querySelectorAll(CAND); } catch (e) { continue; }
          if (scanList(list, null, v, r)) return true;
        }
        return pointsOver(v, r);
      } catch (e) { return false; }
    }
    // a few points over the picture and just below it: whatever the page has stacked there (also inside shadow trees) that a finger could use
    function pointsOver(v, r) {
      try {
        var xs = [r.left + 24, r.left + r.width * 0.25, r.left + r.width * 0.5, r.left + r.width * 0.75, r.right - 24];
        var ys = [r.bottom - 22, r.bottom + 30, r.top + r.height * 0.5];
        for (var yi = 0; yi < ys.length; yi++) for (var xi = 0; xi < xs.length; xi++) {
          var x = xs[xi], y = ys[yi];
          if (x < 0 || y < 0 || x > window.innerWidth || y > window.innerHeight) continue;
          var st = document.elementsFromPoint(x, y), k, m;
          for (k = 0; k < st.length && k < 24; k++) {
            if (isControl(st[k], v, r)) return true;
            var sr = VF.rootOf(st[k]);
            if (sr && sr.elementsFromPoint) { var inner = sr.elementsFromPoint(x, y); for (m = 0; m < inner.length && m < 24; m++) if (isControl(inner[m], v, r)) return true; }
          }
        }
      } catch (e) { /* ignore */ }
      return false;
    }

    // ------------------------------------------------------------------------------------------ taps
    function judge(v, late) {
      try {
        if (!v.isConnected) return;
        if (bar) { if (bar.v === v && !late) keep(); return; }
        if (late && v.paused) return;                             // the second look is for a video that started after the tap (an ad that follows the play button)
        var r = VF.rect(v);
        if (!r) return;
        if (!(cfg && cfg.always) && ownControls(v, r)) { if (!late) report('own'); return; }
        show(v);
      } catch (e) { /* never break the page */ }
    }
    function judgeSoon(v) {
      if (judgeTimer) clearTimeout(judgeTimer);
      if (lateTimer) clearTimeout(lateTimer);
      judgeTimer = setTimeout(function () { judgeTimer = 0; judge(v, false); }, T.judge);
      lateTimer = setTimeout(function () { lateTimer = 0; judge(v, true); }, T.late);
    }
    function insideBar(x, y) {
      if (!bar) return false;
      var r = bar.wrap.getBoundingClientRect();
      return x >= r.left - 4 && x <= r.right + 4 && y >= r.top - 4 && y <= r.bottom + 4;
    }
    function tapped(x, y, d) {
      if (insideBar(x, y)) return;
      ask(function (c) {
        if (!c.on) return;
        var v = (d && d.v) || VF.at(x, y);
        if (v) {
          // a page whose controls were showing when the finger came down (a tap on the film hides them again, as on YouTube) has controls of its own: not a reason to draw ours as they fade
          if (d && d.own && !c.always) { report('own'); return; }
          judgeSoon(v);
          return;
        }
        if (!TOP) relay('tap');                                   // maybe this frame lies over a video of the page above it
      });
    }
    var relayedAt = 0;
    function relay(kind) {
      var now = Date.now();
      if (now - relayedAt < 1500) return;                         // a banner frame that is tapped over and over must not make the page above it look for videos every time
      relayedAt = now;
      try { browser.runtime.sendMessage({ k: 'ft', t: kind, w: window.innerWidth, h: window.innerHeight }).catch(function () {}); } catch (e) { /* nobody listens */ }
    }
    if (TOP) setTimeout(function () { ask(function () {}); }, 1200);   // the answer is there before the first touch: the controls showing at touch start can be seen
    // a touch that starts a scroll costs nothing on a page with no video at all and no shadow host under the finger (the look at the page's controls is only for a film that could be there)
    function mayHaveVideo(e) {
      try {
        if (document.getElementsByTagName('video').length) return true;
        var n = e.target, i = 0;
        while (n && n.nodeType === 1 && i++ < 10) { if (VF.rootOf(n)) return true; n = n.parentNode; if (n && n.nodeType === 11) n = n.host; }
      } catch (x) { /* never break the page */ }
      return false;
    }
    window.addEventListener('pointerdown', function (e) {
      down = e.isTrusted ? { x: e.clientX, y: e.clientY, t: Date.now(), id: e.pointerId, v: null, own: false } : null;
      if (down && cfg && cfg.on && !cfg.always && mayHaveVideo(e)) {
        try { var v = VF.at(down.x, down.y, 300), r = v ? VF.rect(v) : null; if (v && r) { down.v = v; down.own = ownControls(v, r); } } catch (x) { /* never break the page */ }
      }
    }, { capture: true, passive: true });
    window.addEventListener('pointercancel', function () { down = null; }, { capture: true, passive: true });
    window.addEventListener('pointerup', function (e) {
      var d = down; down = null;
      if (!d || !e.isTrusted || e.pointerId !== d.id) return;
      if (Date.now() - d.t > T.tap || Math.abs(e.clientX - d.x) > 12 || Math.abs(e.clientY - d.y) > 12) return;
      tapped(e.clientX, e.clientY, d);
    }, { capture: true, passive: true });

    // the top frame answers what a frame above a video relayed (the background forwards it): a tap there, or a long press there
    if (TOP) {
      browser.runtime.onMessage.addListener(function (m) {
        if (!m || m.k !== 'ftq') return undefined;
        return new Promise(function (resolve) {
          try {
            var v = VF.covered(m.w | 0, m.h | 0);
            if (!v) { resolve({ video: false }); return; }
            if (m.t === 'lp') { VF.menu(v); resolve({ video: true }); return; }
            ask(function (c) { if (c.on) judgeSoon(v); resolve({ video: true }); });
          } catch (e) { resolve({ video: false }); }
        });
      });
    }

    // ------------------------------------------------------------------------------------------ the bar
    var PLAY = 'M8 5v14l11-7z', PAUSE = 'M6 19h4V5H6v14zm8-14v14h4V5h-4z';
    var VOL = 'M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0 0 14 7.97v8.05c1.48-.73 2.5-2.25 2.5-4.02z';
    var MUTED = 'M16.5 12A4.5 4.5 0 0 0 14 7.97v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51A8.8 8.8 0 0 0 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06a8.99 8.99 0 0 0 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z';
    var FULL = 'M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z';
    var EXIT = 'M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z';

    function mk(tag, css, parent, ns) {
      var e = ns ? document.createElementNS(ns, tag) : document.createElement(tag);
      if (css) e.style.cssText = css;
      if (parent) parent.appendChild(e);
      return e;
    }
    function button(path, label, parent) {
      var b = mk('button', 'flex:none;width:40px;height:40px;margin:0;padding:0;border:0;background:transparent;display:flex;align-items:center;justify-content:center;outline:0;-webkit-tap-highlight-color:transparent', parent);
      b.setAttribute('type', 'button'); b.setAttribute('aria-label', label);
      var s = mk('svg', 'width:24px;height:24px;fill:#fff;pointer-events:none', b, SVGNS); s.setAttribute('viewBox', '0 0 24 24');
      var p = mk('path', '', s, SVGNS); p.setAttribute('d', path);
      return { el: b, path: p };
    }
    function fmt(t) {
      t = Math.max(0, Math.floor(t || 0));
      var h = Math.floor(t / 3600), m = Math.floor((t % 3600) / 60), s = t % 60;
      return (h ? h + ':' + (m < 10 ? '0' : '') + m : m) + ':' + (s < 10 ? '0' : '') + s;
    }
    function rangeOf(v) {
      var d = v.duration;
      if (d > 0 && isFinite(d)) return { a: 0, b: d, live: false };
      try { var s = v.seekable; if (s && s.length) { var a = s.start(0), b = s.end(s.length - 1); if (b - a > 30) return { a: a, b: b, live: true }; } } catch (e) { /* no range */ }
      return null;
    }
    function playIt(v) { try { var p = v.play(); if (p && p.catch) p.catch(function () {}); } catch (e) { /* the page decides */ } }

    function fsTarget(v) {            // the player's box: the nearest ancestor about as big as the picture (through shadow hosts), or the video itself
      var r = v.getBoundingClientRect(), n = v;
      for (var i = 0; i < 6; i++) {
        var p = n.parentNode;
        if (p && p.nodeType === 11) p = p.host;
        if (!p || p.nodeType !== 1 || p === document.body || p === document.documentElement) break;
        var pr = p.getBoundingClientRect();
        if (pr.width > r.width * 1.25 + 8 || pr.height > r.height * 1.25 + 8) break;
        n = p;
      }
      return n;
    }
    function fullscreenEl() {         // the element that is full screen, also when it sits in a shadow tree (document.fullscreenElement names the host)
      var e = document.fullscreenElement, i = 0;
      while (e && i++ < 5) { var sr = VF.rootOf(e); if (sr && sr.fullscreenElement) e = sr.fullscreenElement; else break; }
      return e || null;
    }
    var forced = null;                // a film that went full screen WITHOUT a box around it: nothing of ours can be drawn over a <video>, so the engine's own controls are on while it lasts
    function undoForced() { if (forced) { try { forced.controls = false; } catch (e) { /* ignore */ } forced = null; } }
    function toggleFull(v) {
      try {
        if (document.fullscreenElement) { if (document.exitFullscreen) document.exitFullscreen(); return; }
        var t = fsTarget(v);
        if (t === v && !v.controls) { v.controls = true; forced = v; }
        var p = t.requestFullscreen ? t.requestFullscreen() : null;
        if (p && p.catch) p.catch(function () { undoForced(); });
      } catch (e) { undoForced(); /* the page or the engine refused */ }
    }
    function onFullscreen() {         // the bar must be inside the full-screen element to be seen there (the top layer shows nothing else)
      if (!document.fullscreenElement) undoForced();
      if (!bar) return;
      try {
        var fe = fullscreenEl();
        var home = (fe && fe.tagName !== 'VIDEO') ? fe : document.documentElement;
        if (bar.host.parentNode !== home) home.appendChild(bar.host);
        place(); render();
      } catch (e) { /* never break the page */ }
    }
    document.addEventListener('fullscreenchange', onFullscreen, true);

    function show(v) {
      hide();
      var host = mk('div', 'position:fixed;left:0;top:0;width:0;height:0;margin:0;padding:0;border:0;z-index:2147483647;pointer-events:none;display:block', null);
      host.setAttribute('data-kestrel-bar', '1');
      var root = host.attachShadow({ mode: 'closed' });
      var wrap = mk('div', 'position:fixed;left:0;top:0;width:300px;height:44px;box-sizing:border-box;display:flex;align-items:center;padding:0 4px;border-radius:12px;background:rgba(18,20,24,0.86);color:#fff;font:12px/1 sans-serif;pointer-events:auto;touch-action:none;-webkit-user-select:none;user-select:none;box-shadow:0 1px 6px rgba(0,0,0,.5)', root);
      var playB = button(PLAY, 'Play or pause', wrap);
      var cur = mk('span', 'flex:none;min-width:34px;text-align:center;color:#fff', wrap); cur.textContent = '0:00';
      var track = mk('div', 'flex:1 1 auto;position:relative;height:44px;margin:0 6px;touch-action:none', wrap);
      mk('div', 'position:absolute;left:0;right:0;top:20px;height:4px;border-radius:2px;background:rgba(255,255,255,0.35)', track);
      var fill = mk('div', 'position:absolute;left:0;top:20px;height:4px;width:0;border-radius:2px;background:#fff', track);
      var thumb = mk('div', 'position:absolute;top:14px;left:0;width:16px;height:16px;margin-left:-8px;border-radius:8px;background:#fff;box-shadow:0 0 3px rgba(0,0,0,.6)', track);
      var dur = mk('span', 'flex:none;min-width:34px;text-align:center;color:#fff', wrap); dur.textContent = '0:00';
      var muteB = button(VOL, 'Mute or unmute', wrap);
      var fullB = button(FULL, 'Full screen', wrap);
      var b = { v: v, host: host, wrap: wrap, playB: playB, cur: cur, track: track, fill: fill, thumb: thumb, dur: dur, muteB: muteB, fullB: fullB, drag: false, timer: 0, tick: 0, lastSeek: 0, on: [] };
      bar = b;

      // the page must not see what the person does on the bar (events from inside a closed tree stop here; a page's own capture listeners on the window still do)
      var evs = ['pointerdown', 'pointerup', 'pointermove', 'click', 'mousedown', 'mouseup', 'touchstart', 'touchend', 'touchmove', 'contextmenu', 'dblclick'];
      for (var i = 0; i < evs.length; i++) wrap.addEventListener(evs[i], function (e) { e.stopPropagation(); if (e.type === 'contextmenu') e.preventDefault(); });

      playB.el.addEventListener('click', function () { if (v.paused || v.ended) { playIt(v); act('play'); } else { v.pause(); act('pause'); } keep(); render(); });
      muteB.el.addEventListener('click', function () { v.muted = !v.muted; if (!v.muted && v.volume === 0) v.volume = 1; act('mute'); keep(); render(); });
      fullB.el.addEventListener('click', function () { toggleFull(v); act('full'); keep(); });
      function seekFrom(e, final) {
        var rg = rangeOf(v); if (!rg) return;
        var tr = track.getBoundingClientRect();
        var f = Math.max(0, Math.min(1, (e.clientX - tr.left) / Math.max(1, tr.width)));
        var t = rg.a + f * (rg.b - rg.a), now = Date.now();
        fill.style.width = (f * 100) + '%'; thumb.style.left = (f * 100) + '%'; cur.textContent = fmt(t - (rg.live ? rg.a : 0));
        if (final || now - b.lastSeek >= T.drag) { b.lastSeek = now; try { v.currentTime = t; } catch (x) { /* not seekable */ } }
      }
      track.addEventListener('pointerdown', function (e) { b.drag = true; try { track.setPointerCapture(e.pointerId); } catch (x) { /* ignore */ } seekFrom(e, false); e.preventDefault(); });
      track.addEventListener('pointermove', function (e) { if (b.drag) seekFrom(e, false); });
      var end = function (e) { if (!b.drag) return; b.drag = false; seekFrom(e, true); act('seek'); keep(); render(); };
      track.addEventListener('pointerup', end);
      track.addEventListener('pointercancel', function () { b.drag = false; keep(); });

      var names = ['timeupdate', 'play', 'pause', 'playing', 'volumechange', 'durationchange', 'seeked', 'ended', 'emptied'];
      for (var k = 0; k < names.length; k++) { v.addEventListener(names[k], render); b.on.push(names[k]); }
      window.addEventListener('scroll', place, { capture: true, passive: true });
      window.addEventListener('resize', place, { passive: true });
      b.tick = setInterval(place, 250);

      var fe = fullscreenEl();
      (fe && fe.tagName !== 'VIDEO' ? fe : document.documentElement).appendChild(host);
      place(); render(); keep();
      report('shown');
    }
    function act(what) { report('act', what); }

    function place() {
      var b = bar; if (!b) return;
      try {
        var v = b.v, r = v.getBoundingClientRect(), vw = window.innerWidth, vh = window.innerHeight;
        if (!v.isConnected || r.width < 60 || r.bottom < 50 || r.top > vh - 24 || r.right < 20 || r.left > vw - 20) { hide(); return; }
        var w = Math.min(Math.max(r.width - 16, 200), 640); w = Math.min(w, vw - 8);
        var left = Math.max(4, Math.min(vw - w - 4, r.left + (r.width - w) / 2));
        var top = Math.max(4, Math.min(r.bottom, vh) - 10 - 44);
        var s = b.wrap.style;
        s.left = left + 'px'; s.top = top + 'px'; s.width = w + 'px';
        var narrow = w < 330;
        b.cur.style.display = narrow ? 'none' : ''; b.dur.style.display = narrow ? 'none' : '';
      } catch (e) { /* never break the page */ }
    }
    function render() {
      var b = bar; if (!b) return;
      try {
        var v = b.v, rg = rangeOf(v);
        b.playB.path.setAttribute('d', (v.paused || v.ended) ? PLAY : PAUSE);
        b.muteB.path.setAttribute('d', (v.muted || v.volume === 0) ? MUTED : VOL);
        b.fullB.path.setAttribute('d', document.fullscreenElement ? EXIT : FULL);
        if (rg) {
          b.track.style.visibility = 'visible';
          var f = Math.max(0, Math.min(1, ((v.currentTime || 0) - rg.a) / Math.max(0.001, rg.b - rg.a)));
          if (!b.drag) { b.fill.style.width = (f * 100) + '%'; b.thumb.style.left = (f * 100) + '%'; b.cur.textContent = fmt((v.currentTime || 0) - (rg.live ? rg.a : 0)); }
          b.dur.textContent = rg.live ? 'LIVE' : fmt(rg.b);
        } else {
          b.track.style.visibility = 'hidden';
          b.cur.textContent = fmt(v.currentTime || 0); b.dur.textContent = 'LIVE';
        }
      } catch (e) { /* never break the page */ }
    }
    function keep() {
      var b = bar; if (!b) return;
      if (b.timer) clearTimeout(b.timer);
      b.timer = setTimeout(function () { if (bar === b && !b.drag) hide(); else if (bar === b) keep(); }, b.v.paused ? T.hidePaused : T.hide);
    }
    function hide() {
      var b = bar; if (!b) return;
      bar = null;
      try { if (b.timer) clearTimeout(b.timer); if (b.tick) clearInterval(b.tick); } catch (e) { /* ignore */ }
      try { for (var i = 0; i < b.on.length; i++) b.v.removeEventListener(b.on[i], render); } catch (e) { /* ignore */ }
      try { window.removeEventListener('scroll', place, true); window.removeEventListener('resize', place); } catch (e) { /* ignore */ }
      try { if (b.host.parentNode) b.host.parentNode.removeChild(b.host); } catch (e) { /* ignore */ }
    }

    api.dbg = function () { return bar; };   // tests only: the bar's parts (its shadow tree is closed to the page)
    api.hide = hide;
  } catch (e) { /* never break the page */ }
  return api;
})();
