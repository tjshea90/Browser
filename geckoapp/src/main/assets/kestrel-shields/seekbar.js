'use strict';
// YS1 (owner message 42, 2026-11-15; research/SEEKBAR.md): a tap on the red bar of a video player must jump the video to that place, as in other browsers and in player apps.
// On the owner's tablet YouTube's bar answered a tap with only a light "hover" line and no jump. The cause could not be reproduced from the sandbox (it cannot load YouTube's watch page), so this
// script is a safety net that works whatever the cause is: when a finger taps (or slides along) the progress bar of a video player and the PAGE does not move the video within a moment, the video is
// moved to the place of the finger. When the page moves it itself, nothing is done (no double seek; the page's own idea of chapters and gaps wins).
// It does nothing while the player's controls are hidden (the first tap on a hidden player only brings the controls up, as everywhere), for live streams, while an ad plays, for volume / speed / quality
// sliders, for a bar that is not part of a player (it must lie over or right beside a <video>), when another control lies over the bar at the finger, and when the bar's own range is not the video's length.
// Isolated world of the extension, every http(s) page and frame, from document start. Passive listeners only: the page's events are never stopped, delayed or changed. No polling, no scanning: a page without
// a <video> costs one empty check per touch. Buffering is not ours: after a seek the engine (a progressive file: a new range request starts at the new place and the part before it is not read again) or the
// page's own player (YouTube: the segment of the new place) fetches what it needs from there on.
// Counters (numbers, differences, at most one report every 3 s, never an address; plus the tag.class name of a strip it did not know, letters, digits and - _ . only) go to the app as k:'sbk' for Diagnostics. The Settings row "Tap the video bar to jump" (default on) turns it off: the
// answer to the `guard` question carries `seek`, and nothing is done unless it is exactly true.
// Tested in headless Chromium against mock players (web-tests/seekbar.test.js); NOT yet on a real YouTube page.
(function () {
  try {
    if (window.__kstSB) return;
    if (typeof browser === 'undefined' || !browser || !browser.runtime) return;
    window.__kstSB = 1;
    var T = (window.__kstSBcfg && typeof window.__kstSBcfg === 'object') ? window.__kstSBcfg : {}; // the tests shorten the clocks; in the real extension a page's globals are not visible here
    var WAIT = T.waitMs || 450;      // how long the page gets to move the video by itself after the finger went up
    var TAP_MS = T.tapMs || 900;     // a finger that stays longer than this is a long press (the mini panel's), not a tap
    var DRAG_MS = T.dragMs || 8000;  // a slide along the bar that takes longer is not a seek
    var SLOP = T.slop || 14;         // CSS px a finger may drift during a tap
    var FLUSH = T.flushMs || 3000, KEEP = T.keepMs || 60000, REASK = T.reaskMs || 30000, LATE = T.lateMs || 6000;   // LATE: an answer of the app that comes this long after the finger went up is too late (the person has moved on)
    var STACK = 14;                  // how deep the stack of elements under the finger is looked at

    var g = null;                    // the gesture in progress: { id, t0, x, y, bar, video }
    var seeks = [];                  // the last seeks of films in this frame that the PAGE made (our own are not counted), newest last: { t, el }
    var ownAt = 0, ownEl = null;           // our own last seek: its `seeking` event comes a moment later and must not count as the page's
    var ask = 0, askAt = 0, waiting = [];   // 0 = not asked, 1 = asking, 2 = yes, 3 = no
    var pend = { t: 0, s: 0, f: 0, o: 0, u: 0, r: '', n: '' }, sendTimer = 0;
    var unkAt = 0, unkN = 0;         // taps on a bar-like strip that was not known: at most one a second, eight a page

    function now() { return Date.now(); }
    function mark(k, why) {
      if (k) pend[k]++;
      if (why) pend.r = why;
      if (!sendTimer) sendTimer = setTimeout(flush, FLUSH);
    }
    function flush() {
      sendTimer = 0;
      var m = { k: 'sbk', t: pend.t, s: pend.s, f: pend.f, o: pend.o, u: pend.u, r: pend.r, n: pend.n };
      pend = { t: 0, s: 0, f: 0, o: 0, u: 0, r: '', n: '' };
      try { browser.runtime.sendMessage(m).catch(function () {}); } catch (e) { /* ignore */ }
    }

    // The app is asked when the finger lands on a bar (so that the answer is there by the time the page's own moment is over); once a minute at most per page. Its answer carries `seek`; nothing is done unless it is exactly true.
    function askApp(cb) {
      var t = now();
      if (ask === 2 && t - askAt < KEEP) { cb(true); return; }
      if (ask === 3 && t - askAt < REASK) { cb(false); return; }
      waiting.push(cb);
      if (ask === 1) return;
      ask = 1;
      function done(yes) { ask = yes ? 2 : 3; askAt = now(); var w = waiting; waiting = []; for (var i = 0; i < w.length; i++) { try { w[i](yes); } catch (e) { /* ignore */ } } }
      try { browser.runtime.sendMessage({ k: 'guard', h: location.hostname }).then(function (r) { done(!!(r && r.seek === true)); }, function () { done(false); }); } catch (e) { done(false); }
    }

    // ---- what is a seek bar
    var BAD = /volume|bright|contrast|zoom|speed|caption|subtitle|quality|loading|loader|spinner|upload|download|lesson|course|wizard|battery|sponsor|advert|ytp-ad|pagination|swiper|carousel|slick|bullet|stepper|stories|story|(^|[\s_-])(vol|rate|ad|ads|step)([\s_-]|$)/i;
    var GOOD = /progress|seek|scrub|timeline|time-?bar|time-?range|slider|playhead/i;
    function sig(el) {
      var c = el.className;
      if (c && typeof c !== 'string') c = c.baseVal;
      return String(c || '').slice(0, 200) + ' ' + String(el.id || '').slice(0, 80) + ' ' + String(el.getAttribute('aria-label') || '').slice(0, 80) + ' ' + String(el.getAttribute('data-plyr') || el.getAttribute('name') || '').slice(0, 40) + ' ' + (el.localName && el.localName.indexOf('-') > 0 ? el.localName : '');   // a custom element (media-time-range, yt-progress-bar) is named by its tag
    }
    function barLike(el) {
      var s = sig(el);
      if (BAD.test(s) || el.tagName === 'PROGRESS') return false;   // <progress> shows, it does not seek
      if (el.tagName === 'INPUT') return el.type === 'range';
      if (el.getAttribute('role') === 'slider') return true;
      return GOOD.test(s);
    }
    function wideThin(r) { return r.width >= 100 && r.height <= 72 && r.width >= 5 * r.height; }
    /** A short harmless name of an element for Diagnostics: its tag and its first class made of letters, digits, - and _ only. */
    function nameOf(el) {
      var c = el.className;
      if (c && typeof c !== 'string') c = c.baseVal;
      var m = /[A-Za-z][A-Za-z0-9_-]{2,30}/.exec(String(c || ''));
      return String(el.tagName || '').toLowerCase() + (m ? '.' + m[0] : '');
    }
    function interactive(el) {
      var t = el.tagName;
      if (t === 'A' || t === 'BUTTON' || t === 'INPUT' || t === 'SELECT' || t === 'TEXTAREA' || t === 'SUMMARY' || t === 'LABEL') return true;
      var r = el.getAttribute('role');
      return r === 'button' || r === 'link' || r === 'menuitem' || r === 'checkbox' || r === 'switch' || r === 'tab' || r === 'option' || el.hasAttribute('onclick');
    }
    function stackAt(x, y) {
      try { if (document.elementsFromPoint) return document.elementsFromPoint(x, y) || []; } catch (e) { /* ignore */ }
      var one = document.elementFromPoint(x, y);
      return one ? [one] : [];
    }
    /** The <video> a bar belongs to: it lies over the picture, or, when its range can be checked against the film's length (aria-valuemax / a range input's max), up to 32 px beside it, and is at least half as wide as it
     *  (a volume slider is not); controls laid over the lower half of the picture may be narrower, down to 30 percent (a bar between buttons: Plyr at 564 px is 40). */
    function videoFor(r, vids, tight) {
      var best = null, gapBest = 1e9;
      for (var i = 0; i < vids.length && i < 40; i++) {
        var vr = vids[i].getBoundingClientRect();
        if (vr.width < 120 || vr.height < 68) continue;
        var ov = Math.min(r.right, vr.right) - Math.max(r.left, vr.left);
        var low = r.top >= vr.top + 0.5 * vr.height && r.bottom <= vr.bottom + 6;
        if (ov < 0.6 * r.width || r.width < (low ? 0.3 : 0.5) * vr.width) continue;
        var gap = r.top > vr.bottom ? r.top - vr.bottom : (r.bottom < vr.top ? vr.top - r.bottom : 0);
        if (gap > (tight ? 0 : 32)) continue;
        if (gap < gapBest) { best = vids[i]; gapBest = gap; }
      }
      return best;
    }
    /** The films of the player a bar belongs to: those of the nearest ancestor that holds any, so that a second film elsewhere on the page is never taken for this bar's; all the page's films if there is none. */
    function scope(el, vids) {
      for (var n = el.parentElement, k = 0; n && k < 8; n = n.parentElement, k++) { var vs = n.getElementsByTagName('video'); if (vs.length) return vs; }
      return vids;
    }
    /** The bar and what holds it can be seen (the first tap on a player whose controls are hidden only brings them up). */
    function shown(el) {
      var o = 1, k = 0;
      for (var n = el; n && n.nodeType === 1 && k < 14; n = n.parentElement, k++) {
        var cs = getComputedStyle(n);
        if (cs.display === 'none' || cs.visibility === 'hidden') return false;
        if (n !== el) o *= parseFloat(cs.opacity);   // the bar's own opacity is its look (YouTube's rail is .3, Shaka's real range input is 0); hidden CONTROLS hide at a parent
        if (!(o >= 0.5)) return false;
      }
      return true;
    }
    /** A tap in the lower part of a player's picture landed on a thin wide strip that was NOT taken for a bar: its name goes to Diagnostics (a player with a bar this script does not know shows up there). */
    function noteUnknown(x, y, list, vids) {
      var t = now(), i, vr, el, r, inside = false;
      if (t - unkAt < 1000 || unkN >= 8) return;
      for (i = 0; i < vids.length && i < 40 && !inside; i++) {
        vr = vids[i].getBoundingClientRect();
        inside = vr.width >= 120 && vr.height >= 68 && x >= vr.left && x <= vr.right && y >= vr.top + 0.55 * vr.height && y <= vr.bottom + 32;
      }
      if (!inside) return;
      for (i = 0; i < list.length && i < STACK; i++) {
        el = list[i];
        if (!el || el.nodeType !== 1 || el === document.body || el === document.documentElement || el.tagName === 'VIDEO' || el.tagName === 'IFRAME') continue;
        r = el.getBoundingClientRect();
        if (!wideThin(r) || !shown(el)) continue;
        unkAt = t; unkN++; pend.n = nameOf(el); mark('u');
        return;
      }
    }
    function findBar(x, y, vids) {
      var list = stackAt(x, y), n = Math.min(list.length, STACK), best = null, bestAt = -1, i, el, r, rank;
      for (i = 0; i < n; i++) {
        el = list[i];
        if (!el || el.nodeType !== 1 || el === document.body || el === document.documentElement || !barLike(el)) continue;
        r = el.getBoundingClientRect();
        if (!wideThin(r)) continue;
        rank = (el.getAttribute('role') === 'slider' || el.tagName === 'INPUT') ? 1 : 0;   // the real control beats a wider wrapper and a thin decorative line of the same width (YouTube's 3 px rail lies over its 44 px slider)
        if (!best || rank > best.rank || (rank === best.rank && (r.width > best.r.width + 1 || (r.width > best.r.width - 1 && r.height > best.r.height)))) { best = { el: el, r: r, rank: rank }; bestAt = i; }
      }
      if (!best) { noteUnknown(x, y, list, vids); return null; }
      var v = videoFor(best.r, scope(best.el, vids), !(parseFloat(best.el.getAttribute('aria-valuemax')) > 0 || (best.el.tagName === 'INPUT' && parseFloat(best.el.max) > 0)));   // a bar whose range cannot be checked must lie OVER the picture
      if (!v) return null;
      var root = best.el;
      while (root && !root.contains(v)) root = root.parentElement;      // the player: the lowest ancestor of the bar that also holds the film
      for (i = 0; i < bestAt; i++) {
        var o = list[i];
        if (interactive(o) && o.getAttribute('role') !== 'slider') return null;   // a button / link / field (even inside the strip: a playlist of links) is under the finger, not the bar
        if (!best.el.contains(o) && !(root && root.contains(o))) return null;      // something of the page (a scrim, a banner) lies over the bar at the finger
      }
      if (!shown(best.el)) return null;
      return { bar: best.el, video: v };
    }

    // ---- the gesture
    function onDown(e) {
      try {
        g = null;
        if (!e.isTrusted || !e.isPrimary || (e.pointerType === 'mouse' && e.button !== 0)) return;
        if (ask === 3 && now() - askAt < REASK) return;                       // the app said no a moment ago
        var vids = document.getElementsByTagName('video');
        if (!vids.length) return;                                             // most pages end here
        var hit = findBar(e.clientX, e.clientY, vids);
        if (hit) {
          g = { id: e.pointerId, t0: now(), x: e.clientX, y: e.clientY, bar: hit.bar, video: hit.video };
          askApp(function () { /* the answer is only fetched now, so that it is there when the page's moment is over (a busy tablet answers slowly) */ });
        }
      } catch (x) { g = null; }
    }
    function onUp(e) {
      try {
        var d = g;
        if (!d || e.pointerId !== d.id) return;
        g = null;
        if (!e.isTrusted) return;
        var dt = now() - d.t0, dx = e.clientX - d.x, dy = e.clientY - d.y;
        var tap = dt <= TAP_MS && dx * dx + dy * dy <= SLOP * SLOP;
        var slide = !tap && dt <= DRAG_MS && Math.abs(dx) > SLOP && Math.abs(dx) >= 2 * Math.abs(dy);   // along the bar, as when pulling the red dot
        if (!tap && !slide) return;                                           // a long press, a scroll: not ours
        var x = e.clientX;
        d.t1 = now();
        setTimeout(function () { settle(d, x); }, WAIT);
      } catch (x) { /* ignore */ }
    }
    function onCancel() { g = null; }
    function sameFilm(a, b) {
      if (a === b) return true;
      try {
        var p = a.getBoundingClientRect(), q = b.getBoundingClientRect();
        var w = Math.min(p.right, q.right) - Math.max(p.left, q.left), h = Math.min(p.bottom, q.bottom) - Math.max(p.top, q.top);
        return w > 0 && h > 0 && w * h >= 0.5 * Math.min(p.width * p.height, q.width * q.height);
      } catch (e) { return false; }
    }
    /** Any seek of THIS film by the page since the finger went down (another film's loop elsewhere on the page must not hide it). */
    function pageMoved(d) { for (var i = seeks.length - 1; i >= 0; i--) { if (seeks[i].t < d.t0 - 5) return false; if (sameFilm(seeks[i].el, d.video)) return true; } return false; }
    function adShowing(v) {
      for (var n = v.parentElement, k = 0; n && k < 8; n = n.parentElement, k++) {
        var c = n.classList;
        if (c && (c.contains('ad-showing') || c.contains('ad-interrupting') || c.contains('jw-flag-ads') || c.contains('vjs-ad-playing'))) return true;
      }
      return false;
    }
    function settle(d, x) {
      try {
        mark('t');
        if (pageMoved(d)) { mark('s'); return; }                              // the page took the tap itself
        if (!d.video.isConnected || !d.bar.isConnected) { mark('o', 'gone'); return; }
        askApp(function (yes) {
          try {
            if (!yes) { mark('o', 'off'); return; }
            if (now() - d.t1 > LATE) { mark('o', 'late'); return; }
            if (pageMoved(d)) { mark('s'); return; }
            move(d, x);
          } catch (e) { /* ignore */ }
        });
      } catch (e) { /* ignore */ }
    }
    function move(d, x) {
      var v = d.video, bar = d.bar, r = bar.getBoundingClientRect(), dur = v.duration;
      if (!(r.width > 0)) { mark('o', 'gone'); return; }
      if (dur === Infinity || dur > 1e7) { mark('o', 'live'); return; }      // a live stream (Infinity, or 2^32 s as Shaka sets it)
      if (!(dur > 0) || !isFinite(dur) || v.readyState < 1) { mark('o', 'not ready'); return; }
      if (adShowing(v)) { mark('o', 'ad'); return; }
      var mx = parseFloat(bar.getAttribute('aria-valuemax'));
      if (!(mx > 0) && bar.tagName === 'INPUT') mx = parseFloat(bar.max);   // a native range input has `max`
      if (mx > 0 && mx !== 1 && mx !== 100 && mx !== 1000 && mx !== 10000 && Math.abs(mx - dur) > Math.max(5, 0.05 * dur) && Math.abs(mx - dur * 1000) > Math.max(5000, 50 * dur)) { mark('o', 'range'); return; }   // the bar is not the film's whole length
      var f = (x - r.left) / r.width;
      try { if (getComputedStyle(bar).direction === 'rtl') f = 1 - f; } catch (e) { /* ignore */ }
      f = f < 0 ? 0 : (f > 1 ? 1 : f);
      var to = f * dur;
      try { var s = v.seekable; if (s && s.length) { var lo = s.start(0), hi = s.end(s.length - 1); if (hi - lo < 0.9 * dur) { mark('o', 'range'); return; } if (to < lo) to = lo; if (to > hi) to = hi; } } catch (e) { /* ignore */ }
      try { ownAt = now(); ownEl = v; v.currentTime = to; mark('f'); } catch (e) { ownAt = 0; mark('o', 'refused'); }
    }

    var opt = { capture: true, passive: true };
    window.addEventListener('pointerdown', onDown, opt);
    window.addEventListener('pointerup', onUp, opt);
    window.addEventListener('pointercancel', onCancel, opt);
    window.addEventListener('seeking', function (e) {            // a media event does not bubble; the capture phase sees every film of the frame
      var t = now();
      if (e.target === ownEl && t - ownAt < 1000) return;               // our own seek (its event can come late on a busy tablet)
      seeks.push({ t: t, el: e.target }); if (seeks.length > 8) seeks.shift();
    }, true);
  } catch (e) { /* never break a page */ }
})();
