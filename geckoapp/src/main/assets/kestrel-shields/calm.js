'use strict';
// PERF8b (owner 2026-11-22: "If i leave the kestrel player, it is stuck on a loading animation in the site's video player" and "The animation is still slightly clipped on the bottom"): a player that is PAUSED and has never played
// (the press-to-play rule holds its film, or Kestrel's player took the film over and the page's copy was paused in the middle of starting) keeps showing its loading circle for ever, and the circle is the first thing the
// person sees. A paused video that shows a loading circle is not loading: this turns the circle (an element with a CSS animation that never ends, sitting in the middle of a big paused video) into a play mark of the same box,
// so the picture reads "press play" (the tap still reaches the player: the press-to-play gate answers it as before). Nothing is removed from the page: one attribute and one style rule, taken off again when the video plays.
// It also tells the app WHAT it found (element tag and first class name, its size, and whether a parent box with overflow hidden cuts it: how many pixels at the bottom), once per circle and at most 3 a page: this is the
// evidence for the clipped circle (a parent that cuts it is page layout; none means the engine's drawing). The app logs it as `video calm` (never in a private tab: the app drops it). Conservative ES5, runs in every frame
// at document_idle, a few timed looks and nothing else. Tested in headless Chromium by web-tests/calm.test.js.
(function () {
  try {
    if (window.__kstCalm) return;
    if (typeof browser === 'undefined' || !browser || !browser.runtime) return;
    if (!/^https?:$/.test(String(location.protocol))) return;
    if (typeof document.getAnimations !== 'function') return;
    window.__kstCalm = 1;
    var CFG = (window.__kstCalmCfg && typeof window.__kstCalmCfg === 'object') ? window.__kstCalmCfg : {};   // (the tests shorten the delays; a page's globals are not visible to a content script in the real extension)
    var DELAYS = CFG.delays || [2500, 6000, 12000, 25000, 50000, 90000];
    var MAX_LOOKS = 40, MAX_TOLD = 3;
    var looks = 0, told = 0, lastLook = 0, pending = 0, calmed = [], styleDone = false;
    var CSS = '[data-kst-calm]{animation:none!important;transition:none!important;border:0!important;box-shadow:none!important;outline:0!important;background:rgba(255,255,255,.92)!important;' +
      'clip-path:polygon(30% 16%,30% 84%,84% 50%)!important;-webkit-clip-path:polygon(30% 16%,30% 84%,84% 50%)!important;opacity:1!important;visibility:visible!important;filter:none!important;mask:none!important;-webkit-mask:none!important}' +
      '[data-kst-calm] *{display:none!important}';

    function name(el) {
      var n = String(el.nodeName || '').toLowerCase(), c = '';
      try { c = String(el.getAttribute('class') || '').replace(/^\s+/, '').split(/\s+/)[0] || ''; } catch (e) { c = ''; }
      return (n + (c ? '.' + c.slice(0, 30) : '')).replace(/[^\x20-\x7e]/g, '');
    }
    function clipOf(t) {
      try {
        var tr = t.getBoundingClientRect(), p = t.parentElement, n = 0;
        while (p && p !== document.documentElement && n < 8) {
          var cs = window.getComputedStyle(p), ov = cs.overflowY;
          if (ov && ov !== 'visible') {
            var pr = p.getBoundingClientRect();
            return name(p) + ' overflow=' + ov + ' cutBottom=' + Math.round(tr.bottom - pr.bottom) + ' cutTop=' + Math.round(pr.top - tr.top) + ' box=' + Math.round(pr.width) + 'x' + Math.round(pr.height);
          }
          p = p.parentElement; n++;
        }
      } catch (e) { /* none */ }
      return 'no overflow clip';
    }
    function addStyle() {
      if (styleDone) return;
      var st = document.createElement('style');
      st.setAttribute('data-kestrel', 'calm');
      st.textContent = CSS;
      var into = document.head || document.documentElement;
      if (!into) return;
      into.appendChild(st); styleDone = true;
    }
    function uncalm() {
      for (var i = 0; i < calmed.length; i++) {
        var c = calmed[i];
        try {
          c.el.removeAttribute('data-kst-calm');
          if (c.label === null) c.el.removeAttribute('aria-label'); else c.el.setAttribute('aria-label', c.label);          // (what the page had is put back)
          if (c.role === null) c.el.removeAttribute('role'); else c.el.setAttribute('role', c.role);
        } catch (e) { /* gone */ }
      }
      calmed = [];
    }
    // the circle of a big paused video: an infinite CSS animation on an element of circle size whose centre lies inside the video's box (an svg child counts as its outermost svg)
    function circleIn(v, r, anims) {
      var small = Math.min(r.width, r.height) * 0.6;
      for (var i = 0; i < anims.length && i < 60; i++) {
        var a = anims[i], ef = a && a.effect, t = ef && ef.target;
        if (!t || t.nodeType !== 1 || ef.pseudoElement || a.playState !== 'running') continue;
        var ct = ef.getComputedTiming ? ef.getComputedTiming() : null;
        if (!ct || ct.iterations !== Infinity) continue;
        while (t.ownerSVGElement) t = t.ownerSVGElement;
        if (t === v || v.contains(t) || t.hasAttribute('data-kst-calm')) continue;
        if (t.closest && t.closest('a,button,[role="button"],input,select,textarea,[role="progressbar"]')) continue;
        var tr = t.getBoundingClientRect();
        if (tr.width < 14 || tr.height < 14 || tr.width > small || tr.height > small) continue;
        var cx = tr.left + tr.width / 2, cy = tr.top + tr.height / 2;
        if (cx < r.left || cx > r.right || cy < r.top || cy > r.bottom) continue;
        return t;
      }
      return null;
    }
    function look() {
      pending = 0;
      if (document.hidden || looks >= MAX_LOOKS) return;
      looks++; lastLook = Date.now();
      var vids = document.getElementsByTagName('video');
      if (!vids.length) return;
      var anims = document.getAnimations();
      if (!anims.length) return;
      for (var i = 0; i < vids.length && i < 6; i++) {
        var v = vids[i];
        if (!v.paused || v.ended || v.controls || v.currentTime > 1) continue;                       // it plays, it was played, or it shows the engine's own controls: not ours
        var r = v.getBoundingClientRect();
        if (r.width < 240 || r.height < 135 || r.bottom < 0 || r.top > (window.innerHeight || 0)) continue;
        var t = circleIn(v, r, anims);
        if (!t) continue;
        var text = 'circle ' + name(t) + ' ' + Math.round(t.getBoundingClientRect().width) + 'x' + Math.round(t.getBoundingClientRect().height) + ' in a paused video ' + Math.round(r.width) + 'x' + Math.round(r.height) + ', ' + clipOf(t);
        addStyle();
        // the mark is a PLAY button: labelled as one (the press-to-play gate asks Kestrel's player first for a tap on a control that is named so: PERF9, a tap on the mark opened the page's own player, which stalled)
        calmed.push({ el: t, label: t.getAttribute('aria-label'), role: t.getAttribute('role') });
        t.setAttribute('data-kst-calm', '1'); t.setAttribute('aria-label', 'Play video'); t.setAttribute('role', 'button');
        if (told < MAX_TOLD) {
          told++;
          try { browser.runtime.sendMessage({ k: 'verr', c: 98, x: text.slice(0, 160), ns: v.networkState | 0, rs: v.readyState | 0, s: 'none', f: window === window.top ? 1 : 0, e: 'v' }).catch(function () { /* no background page */ }); } catch (e) { /* ignore */ }
        }
        return;
      }
    }
    function soon(ms) {
      if (pending || looks >= MAX_LOOKS) return;
      if (Date.now() - lastLook < 700) ms = Math.max(ms, 800);
      pending = window.setTimeout(look, ms);
    }
    for (var d = 0; d < DELAYS.length; d++) window.setTimeout(function () { soon(0); }, DELAYS[d]);
    document.addEventListener('visibilitychange', function () { if (!document.hidden) soon(1500); }, true);   // back from Kestrel's own player: the page is shown again
    document.addEventListener('pause', function () { soon(1500); }, true);                                       // the page's copy paused in the middle of starting
    document.addEventListener('play', function () { uncalm(); }, true);
    document.addEventListener('playing', function () { uncalm(); }, true);
  } catch (e) { /* a page must never feel this script */ }
})();
