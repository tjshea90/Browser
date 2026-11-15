'use strict';
// RX3-G7 (research/REVIEW-EXTERNAL-3.md part G, research/YOUTUBE_ADS.md section 6): the ad skipper for video players OTHER than YouTube (youtube.js owns the YouTube hosts; this file exits there). Isolated world of the extension
// (it needs the extension API and the page must not see it), every http(s) page and frame, from document start, so that the first `play` of a page is not missed.
//
//  * Nothing runs until a <video> fires `play`. Then ONLY the `class` attribute of that video's ancestors (up to 8 levels, never body) is watched: no polling, no scanning of the page. The app is not asked anything until an
//    ad shows (a page without ad words never causes a message).
//  * An ad shows when an ancestor carries an ad-state class that the player's own public source sets (read on 2026-11-14, not remembered): `jw-flag-ads` (JW Player 8, on its root while an ad plays) or `vjs-ad-playing`
//    (Video.js with videojs-contrib-ads, on the root while the ad plays; vjs-ad-loading and vjs-ad-content-resuming are waiting states with no ad stream and are ignored), or when the video sits INSIDE the ad's own container
//    (`ima-ad-container` of Google IMA / videojs-ima, `plyr__ads` of Plyr): that video is the ad.
//  * While an ad shows (a loop every 250 ms, at most 15 s) AND the element holds a stream that is not the film (a different length than the film's, or a hosted ad video): the video is muted and sped up to 16x, an ad stream
//    shorter than 3 minutes is moved to its last 0.1 s, and a skip button that says skippable (JW: .jw-skip.jw-skippable) is pressed. While the stream is still the film nothing is touched at all (the ad class can come a moment
//    before the ad stream does). When the ad ends the sound and the speed are put back, unless the page already changed them.
//  * Five ads in a row that are still showing after 15 s switch the skipper off for that frame. It never touches playback when no ad word shows.
//  * Counters (numbers only, differences since the last report, at most one report every 3 s, no address) go to the app with w:'p' ("other players") for Diagnostics.
// The Settings row "Skip ads inside video players" (default on) and the site's Shields switch turn this off: the answer to the `guard` question carries `skip`, and nothing is done unless it is exactly true.
// Tested in headless Chromium against mock players that use the class names above (web-tests/players.test.js). A real ad could not be had from the sandbox: what a real ad does is unverified until the owner's Diagnostics
// line "Other players' ads" shows ads skipped.
(function () {
  try {
    if (window.__kstPL) return;
    if (typeof browser === 'undefined' || !browser || !browser.runtime) return;
    if (/(^|\.)(youtube\.com|youtube-nocookie\.com|youtubekids\.com)$/i.test(String(location.hostname))) return;
    window.__kstPL = 1;
    var T = (window.__kstPLcfg && typeof window.__kstPLcfg === 'object') ? window.__kstPLcfg : {}; // the tests shorten the clocks; in the real extension a page's globals are not visible here
    var LOOP = T.loopMs || 250, MAXMS = T.maxMs || 15000, GIVEUP = T.fails || 5, FLUSH = T.flushMs || 3000, HOLD = T.holdMs || 1000, LEVELS = 8, MAXVIDEOS = 12;
    var STATE = ['jw-flag-ads', 'vjs-ad-playing'];       // on an ancestor while an ad plays
    var HOST = ['ima-ad-container', 'plyr__ads'];        // a video inside one of these is the ad itself
    var SKIP = ['.jw-skip.jw-skippable'];                // JW Player: the skip button element gets jw-skippable when skipping is allowed

    var trackers = [], off = false, failsInRow = 0, ask = 0, waiting = [];
    var pend = { a: 0, s: 0, c: 0, o: 0, d: 0, f: 0, g: 0, r: '' }, sendTimer = 0;

    function mark(k) { if (k) pend[k]++; if (!sendTimer) sendTimer = setTimeout(flush, FLUSH); }
    function flush() {
      sendTimer = 0;
      var m = { k: 'yts', w: 'p', a: pend.a, s: pend.s, c: pend.c, o: pend.o, d: pend.d, f: pend.f, g: pend.g, r: pend.r };
      pend = { a: 0, s: 0, c: 0, o: 0, d: 0, f: 0, g: 0, r: '' };
      try { browser.runtime.sendMessage(m).catch(function () {}); } catch (e) { /* ignore */ }
    }

    // The app is asked once per frame, the first time an ad shows. Its answer carries `skip`; nothing is done unless it is exactly true.
    function allowed(cb) {
      if (ask === 2) { cb(); return; }
      if (ask === 3) return;
      waiting.push(cb);
      if (ask === 1) return;
      ask = 1;
      try {
        browser.runtime.sendMessage({ k: 'guard', h: location.hostname, s: 1 }).then(function (r) {
          if (r && r.skip === true) { ask = 2; var w = waiting; waiting = []; for (var i = 0; i < w.length; i++) w[i](); } else { ask = 3; waiting = []; }
        }, function () { ask = 3; waiting = []; /* the app is not answering: nothing is done */ });
      } catch (e) { ask = 3; waiting = []; }
    }

    function visible(el) { try { var r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; } catch (e) { return false; } }
    function hasAny(c, list) { for (var i = 0; i < list.length; i++) if (c.contains(list[i])) return true; return false; }
    /** What the ancestors of the video say: the element that carries an ad-state class (or null) and whether the video sits inside an ad container. */
    function look(v) {
      var res = { state: null, host: false }, n = v.parentNode, i = 0;
      while (n && n.nodeType === 1 && i < LEVELS && n !== document.body && n !== document.documentElement) {
        var c = n.classList;
        if (c) {
          if (!res.state && hasAny(c, STATE)) res.state = n;
          if (!res.host && hasAny(c, HOST)) res.host = true;
        }
        n = n.parentNode; i++;
      }
      return res;
    }
    function adNow(t) {
      var v = t.v;
      if (!document.contains(v)) return false;
      var r = look(v);
      return !!(r.state || (r.host && !v.ended));
    }
    /** True while the element holds something other than the film: a hosted ad video, or a finite stream whose length differs from the film's (a live stream cannot be told, so it counts as the film). */
    function notFilm(t) {
      var v = t.v, d = v.duration;
      if (look(v).host) return true;
      if (!(d > 0) || !isFinite(d)) return false;
      if (!(t.mainDur > 0)) return d < 180;     // the film's length is not known yet (the ad came first): only a stream under 3 minutes can be an ad
      return Math.abs(d - t.mainDur) > 0.5;
    }

    function find(v) { for (var i = 0; i < trackers.length; i++) if (trackers[i].v === v) return trackers[i]; return null; }
    function watch(t) { // the class attribute of every ancestor, one observer: re-done on each play, the player may have been moved
      try {
        t.obs.disconnect();
        var n = t.v.parentNode, i = 0;
        while (n && n.nodeType === 1 && i < LEVELS && n !== document.body && n !== document.documentElement) { t.obs.observe(n, { attributes: true, attributeFilter: ['class'] }); n = n.parentNode; i++; }
      } catch (e) { /* ignore */ }
    }
    function drop(i) { trackers[i].dead = true; trackers[i].obs.disconnect(); trackers.splice(i, 1); }
    function track(v) {
      var t = find(v);
      if (t) return t;
      for (var i = trackers.length - 1; i >= 0; i--) if (!document.contains(trackers[i].v)) drop(i);
      if (trackers.length >= MAXVIDEOS) { // a feed with many videos: forget the oldest one that is not in an ad and not playing; when all of them play, the newest is not followed
        var k = -1;
        for (var j = 0; j < trackers.length; j++) if (!trackers[j].active && (trackers[j].v.paused || trackers[j].v.ended)) { k = j; break; }
        if (k < 0) return null;
        drop(k);
      }
      t = { v: v, obs: null, active: null, given: false, mainDur: 0, dead: false };
      t.obs = new MutationObserver(function () { check(t); });
      v.addEventListener('durationchange', function () { filmLength(t, false); }, true);
      t.onTime = function () { if (t.mainDur > 0 || t.dead) { v.removeEventListener('timeupdate', t.onTime, true); return; } filmLength(t, true); };
      v.addEventListener('timeupdate', t.onTime, true);
      trackers.push(t);
      return t;
    }
    // The length of the film itself, remembered while no ad shows - and only once it is certain to be the film: a stream of 3 minutes or more, or one that played 10 s without an ad class (an ad stream whose
    // class came a moment late must not be taken for the film: the real film would then look like an ad). While it is not known, only streams under 3 minutes count as ads and nothing is moved to its end.
    function filmLength(t, played) {
      try {
        var v = t.v, d = v.duration;
        if (t.dead || adNow(t) || !isFinite(d) || !(d > 0)) return;
        if (d >= 180 || (played && v.currentTime >= 10)) t.mainDur = d;
      } catch (e) { /* ignore */ }
    }

    function check(t) {
      if (off) return;
      var a = adNow(t);
      if (!a) t.given = false;
      if (a && !t.active && !t.given) allowed(function () { if (!off && !t.active && !t.given && adNow(t)) begin(t); });
      else if (!a && t.active) finish(t, true);
    }
    function begin(t) {
      mark('a');
      t.active = { t0: Date.now(), saved: null, timer: 0, jumped: false, clicked: [], acted: false, lockDur: 0, lockSrc: '', holdUntil: 0 };
      tick(t);
    }
    function restore(t, a) { // put back what was ours; what somebody else changed in the meantime stays
      try {
        var v = t.v;
        if (v && a.saved) {
          if (v.muted && !a.saved.muted) v.muted = false;
          if (v.playbackRate === 16) v.playbackRate = a.saved.rate || 1;
        }
      } catch (e) { /* ignore */ }
      a.saved = null;
    }
    function tick(t) {
      var a = t.active;
      if (!a) return;
      a.timer = 0;
      try {
        if (!adNow(t)) { finish(t, true); return; }
        if (Date.now() - a.t0 > MAXMS) { finish(t, false); return; }
        var v = t.v;
        // The stream this ad session began on is remembered. When the element's stream CHANGES while the ad class is still there, it is either the next ad of a pod or the film coming back while the class lingers a
        // moment: what was done to the old stream is undone at once, nothing is done for a second, and only if the class is still there then is the new stream treated as another ad (a film must never be sped up or moved).
        var dNow = v.duration, srcNow = v.currentSrc || v.src || '';
        if (a.lockDur > 0 && isFinite(dNow) && dNow > 0 && (Math.abs(dNow - a.lockDur) > 0.5 || srcNow !== a.lockSrc)) {
          restore(t, a); a.jumped = false; a.lockDur = dNow; a.lockSrc = srcNow; a.holdUntil = Date.now() + HOLD;
        } else if (!(a.lockDur > 0) && isFinite(dNow) && dNow > 0) { a.lockDur = dNow; a.lockSrc = srcNow; }
        if (Date.now() < a.holdUntil) { if (t.active === a) a.timer = setTimeout(function () { tick(t); }, LOOP); return; }
        if (!v.paused && !v.ended && notFilm(t)) {
          if (!a.saved) a.saved = { muted: v.muted, rate: v.playbackRate };
          a.acted = true;
          if (!v.muted) v.muted = true;
          if (v.playbackRate !== 16) { try { v.playbackRate = 16; } catch (e) { pend.r = 'speed refused'; } }
          var d = v.duration;
          if (!a.jumped && Date.now() - a.t0 >= 300 && isFinite(d) && d > 0 && d < 180 && v.currentTime < d - 0.2) {
            try { v.currentTime = d - 0.1; a.jumped = true; mark('f'); } catch (e) { pend.r = 'seek refused'; }
          }
          clickSkip(t, a);
        } else if (a.saved && !notFilm(t)) restore(t, a);   // the film is back while the class has not gone yet
      } catch (e) { pend.r = String((e && e.message) || e).slice(0, 40); }
      if (t.active === a) a.timer = setTimeout(function () { tick(t); }, LOOP);
    }
    function press(el) { // what a finger or a mouse makes: some players listen to the up event, not to click
      var names = ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'];
      try { for (var i = 0; i < names.length; i++) el.dispatchEvent(new MouseEvent(names[i], { bubbles: true, cancelable: true })); } catch (e) { try { el.click(); } catch (e2) { /* ignore */ } }
    }
    function clickSkip(t, a) {
      var root = look(t.v).state;
      if (!root) return;
      for (var s = 0; s < SKIP.length; s++) {
        var list = root.querySelectorAll(SKIP[s]);
        for (var i = 0; i < list.length; i++) {
          if (a.clicked.indexOf(list[i]) < 0 && visible(list[i])) { a.clicked.push(list[i]); press(list[i]); mark('c'); return; }
        }
      }
    }
    function finish(t, ok) {
      var a = t.active;
      t.active = null;
      if (!a) return;
      if (a.timer) clearTimeout(a.timer);
      restore(t, a);
      if (ok) { failsInRow = 0; if (a.acted) mark('s'); }
      else {
        t.given = true; failsInRow++; mark('g');
        pend.r = 'an ad was still showing after ' + Math.round(MAXMS / 1000) + ' s';
        if (failsInRow >= GIVEUP) {
          off = true; pend.r = 'gave up: ' + failsInRow + ' ads in a row would not end';
          for (var i = 0; i < trackers.length; i++) trackers[i].obs.disconnect();
        }
      }
      mark();
    }

    function onPlay(e) {
      try {
        var v = e && e.target;
        if (off || !v || v.nodeType !== 1 || v.tagName !== 'VIDEO') return;
        var t = track(v);
        if (!t) return;
        watch(t); filmLength(t, false); check(t);
      } catch (err) { /* never break the page */ }
    }
    function onEnded(e) { // a hosted ad video that ended: the ad is over
      try { var t = e && e.target && find(e.target); if (t && t.active) check(t); } catch (err) { /* ignore */ }
    }
    document.addEventListener('play', onPlay, true);
    document.addEventListener('ended', onEnded, true);
  } catch (e) { /* never break the page */ }
})();
