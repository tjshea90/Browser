// PERF21 (owner 2026-11-24, with a screenshot of a page whose hero is a strip of 60 small looping picture clips: "The animations on this page are very laggy and make the entire site laggy. They also each have a play
// button, which is incorrect for in site animations"; research/DECORATIVE_VIDEO.md). A DECORATIVE clip is what a page plays as an animation: silent (muted), looping, and with no controls of its own. It is not a film.
// What the numbers said (real page, headless Chromium that also draws on the processor, CPU slowed 6x): with its clips paused the page drew about 60 frames a second, with even one clip playing 36, with four 32: every
// playing clip ties the whole page's drawing to the clip's own frame rate, and on this tablet each also costs decoding on a weak processor. A page that plays a dozen of them, most of them scrolled out of sight, is slow
// all over.
// So: a decorative clip plays only while it is on screen, and at most MAX of them at once (the most visible ones). The rest are PARKED: paused by us, and played again, only by us, when they are on screen and there is
// room. Rules that keep it harmless:
//   - only a clip that is already playing is touched (a clip held back by the press-to-play gate never started: nothing to do, the gate stays the gate); a clip the PAGE pauses leaves our hands at once;
//   - a clip that is not decorative (sound, or controls, or no loop) is never touched, and neither is anything while the Settings switch is off (the answer to the `guard` question carries `dc`);
//   - a page that keeps starting a clip we park (the page's own carousel logic) is left alone after 4 parkings in 20 s: no tug of war;
//   - one timer, 500 ms, only while a decorative clip is playing or parked; nothing runs for a page without such clips.
// Numbers go to the app as `vbk` (e `deco`, a `cap` / `off` / `back`, never an address). Conservative ES5; a page must never feel this script.
(function () {
  try {
    if (window.__kstDeco) return;
    window.__kstDeco = 1;
    if (typeof HTMLMediaElement === 'undefined' || typeof window.addEventListener !== 'function' || !document.documentElement) return;
    var T = { max: 2, tick: 500, wait: 20000, tries: 4 };
    try { if (window.__kstDecoCfg) for (var k in window.__kstDecoCfg) T[k] = window.__kstDecoCfg[k]; } catch (e0) { /* optional */ }
    var on = null;                       // null = not asked yet, true / false = the app's answer
    var active = [];                     // decorative clips playing now (oldest first)
    var parked = [];                     // clips WE paused, in the order they should come back
    var ours = null;                     // the clip we are pausing right now: its pause event is ours, not the page's
    var tries = new WeakMap();           // clip -> [times of our parkings]
    var free = new WeakMap();            // clips we gave up on (the page keeps starting them)
    var timer = 0, sent = 0, askedAt = 0;

    function deco(v) { try { return !!(v && v.nodeName === 'VIDEO' && v.loop && (v.muted || v.defaultMuted) && !v.controls); } catch (e) { return false; } }
    function idx(a, v) { for (var i = 0; i < a.length; i++) if (a[i] === v) return i; return -1; }
    function drop(a, v) { var i = idx(a, v); if (i >= 0) a.splice(i, 1); }
    function report(a) { if (sent >= 6) return; sent++; try { browser.runtime.sendMessage({ k: 'vbk', e: 'deco', a: a }).catch(function () {}); } catch (e) { /* nobody listens */ } }
    // how much of the clip is on screen, in square pixels (0 = out of sight)
    function seen(v) {
      try {
        if (document.hidden) return 0;
        var r = v.getBoundingClientRect(), w = Math.min(r.right, window.innerWidth) - Math.max(r.left, 0), h = Math.min(r.bottom, window.innerHeight) - Math.max(r.top, 0);
        return (w > 4 && h > 4) ? w * h : 0;
      } catch (e) { return 0; }
    }
    function ask(cb) {
      if (on !== null) { cb(); return; }
      var now = Date.now(); if (now - askedAt < 3000) return; askedAt = now;
      try { browser.runtime.sendMessage({ k: 'guard', h: location.hostname }).then(function (r) { if (r) { on = (r.dc === true); if (on) cb(); } }, function () { /* ask again at the next clip */ }); } catch (e) { /* no background page: nothing is limited */ }
    }

    function park(v, why) {
      try {
        var t = Date.now(), list = tries.get(v) || [];
        while (list.length && t - list[0] > T.wait) list.shift();
        list.push(t); tries.set(v, list);
        if (list.length > T.tries) { free.set(v, 1); drop(active, v); drop(parked, v); return; }      // the page keeps starting it: not ours to manage
        ours = v; try { v.pause(); } finally { ours = null; }
        drop(active, v); if (idx(parked, v) < 0) parked.push(v);
        report(why);
      } catch (e) { ours = null; }
    }
    function unpark(v) {
      try {
        drop(parked, v);
        var p = v.play(); if (p && typeof p.catch === 'function') p.catch(function () {});
        report('back');
      } catch (e) { /* the page's own state wins */ }
    }

    function tick() {
      timer = 0;
      try {
        var i, v;
        for (i = active.length - 1; i >= 0; i--) { v = active[i]; if (!v.isConnected || v.paused || v.ended || !deco(v)) active.splice(i, 1); }
        for (i = parked.length - 1; i >= 0; i--) { v = parked[i]; if (!v.isConnected || !v.paused || !deco(v)) parked.splice(i, 1); }
        // out of sight: park
        for (i = active.length - 1; i >= 0; i--) if (!seen(active[i])) park(active[i], 'off');
        // too many: keep the most visible
        if (active.length > T.max) {
          var order = active.slice().sort(function (a, b) { return seen(b) - seen(a); });
          for (i = T.max; i < order.length; i++) park(order[i], 'cap');
        }
        // room and a clip waiting that is on screen: bring the most visible one back
        if (active.length < T.max && parked.length) {
          var back = parked.filter(function (p) { return seen(p) > 0; }).sort(function (a, b) { return seen(b) - seen(a); });
          for (i = 0; i < back.length && active.length + i < T.max; i++) { active.push(back[i]); unpark(back[i]); }
        }
      } catch (e) { /* never break the page */ }
      if (active.length || parked.length) timer = setTimeout(tick, T.tick);
    }
    function soon() { if (!timer) timer = setTimeout(tick, 0); }

    // a decorative clip started playing (by the page, an attribute or us): it is governed
    window.addEventListener('playing', function (ev) {
      var v = ev && ev.target;
      if (!deco(v) || free.get(v)) return;
      ask(function () {
        if (idx(active, v) < 0) active.push(v);
        drop(parked, v);
        soon();
      });
    }, true);
    // the page paused it, or it is gone: ours no more (a pause WE made is not the page's)
    function released(ev) { var v = ev && ev.target; if (!v || v === ours) return; drop(active, v); drop(parked, v); }
    window.addEventListener('pause', released, true);
    window.addEventListener('emptied', released, true);
    window.addEventListener('abort', released, true);
    window.addEventListener('ended', released, true);
    try { document.addEventListener('visibilitychange', soon, false); } catch (e1) { /* optional */ }
    try { window.addEventListener('scroll', soon, { passive: true, capture: true }); } catch (e2) { /* optional */ }

    // clips the page started before this script ran
    try { var all = document.getElementsByTagName('video'); for (var j = 0; j < all.length && j < 200; j++) if (!all[j].paused && deco(all[j])) { var vv = all[j]; ask(function () { if (idx(active, vv) < 0) active.push(vv); soon(); }); break; } } catch (e3) { /* optional */ }
    window.KestrelDeco = { dbg: function () { return { active: active.length, parked: parked.length, on: on }; } };   // for the tests
  } catch (e) { /* a page must never feel this script */ }
})();
