'use strict';
// RX3-Y2 (research/REVIEW-EXTERNAL-3.md part Y, research/YOUTUBE_ADS.md section 4): the built-in YouTube ad skipper. It does not depend on the lists (which change every few hours and which our scriptlet
// library only partly answers): it works on the page the owner is looking at, in the isolated world of the extension (it needs the extension API and the page must not see it). It runs only on the YouTube
// hosts (manifest.json: youtube.com, youtube-nocookie.com, youtubekids.com; every frame, once the page is parsed).
//
//  * No polling while no ad. The player is found once (#movie_player is the .html5-video-player element, on the mobile site and on the desktop site: checked live 2026-11-14), looked for again 1 s, 3 s and
//    8 s later and after the site's own navigation events, and only its `class` attribute is watched. An ad is "showing" while the class list has `ad-showing` or `ad-interrupting`.
//  * While an ad shows (a loop every 250 ms, at most 15 s): the video is muted and sped up to 16x, an ad stream shorter than 3 minutes is moved to its last 0.1 s, the first visible skip button is pressed and an
//    overlay ad is closed. When the ad class goes the sound and the speed are put back, unless somebody else already did. The film itself is never moved: the jump only happens when the video element holds
//    a stream whose length differs from the film's (the ad class can come a moment before the ad stream does).
//  * An overlay (banner) ad has no ad class: the small element that holds overlays is watched for new children and they are closed.
//  * The "ad blockers are not allowed" dialog is looked for 1.5 s, 4 s and 9 s after the page and after each navigation: it and its backdrop are removed, page scrolling is unlocked and the film is started.
//  * Five ads in a row that are still showing after 15 s switch the skipper off for that page. It never touches playback when no ad class is on the player.
//  * Counters (numbers only, differences since the last report, at most one report every 3 s, no address) go to the app for Diagnostics "YouTube ads".
// YouTube's terms forbid skipping ads; it may warn or limit playback, more likely when signed in. The Settings row "Skip ads inside video players" (default on) and the site's Shields switch turn this off:
// the answer to the `guard` question carries `skip`, and nothing is done unless it is exactly true.
// Tested in headless Chromium against a mock player by web-tests/youtube.test.js (the ad-state names and the skip-button selectors are the long-standing public ones; a real ad could not be had from the
// sandbox, so they stay unverified on the real site until the owner's Diagnostics line shows ads skipped).
(function () {
  try {
    if (window.__kstYT) return;
    if (typeof browser === 'undefined' || !browser || !browser.runtime) return;
    window.__kstYT = 1;
    var T = (window.__kstYTcfg && typeof window.__kstYTcfg === 'object') ? window.__kstYTcfg : {}; // the tests shorten the clocks; in the real extension a page's globals are not visible here
    var LOOP = T.loopMs || 250, MAXMS = T.maxMs || 15000, GIVEUP = T.fails || 5;
    var SKIP = ['.ytp-skip-ad-button', '.ytp-ad-skip-button', '.ytp-ad-skip-button-modern', '.ytp-ad-skip-button-container button'];
    var OVERLAY = '.ytp-ad-overlay-close-button, .ytp-ad-overlay-close-container';

    var player = null, obs = null, adsObs = null, adsEl = null, active = null, givenUp = false, off = false, failsInRow = 0, mainDur = 0;
    var pend = { a: 0, s: 0, c: 0, o: 0, d: 0, f: 0, g: 0, r: '' }, sendTimer = 0, overlayTimer = 0;

    function mark(k) { if (k) pend[k]++; if (!sendTimer) sendTimer = setTimeout(flush, 3000); }
    function flush() {
      sendTimer = 0;
      var m = { k: 'yts', a: pend.a, s: pend.s, c: pend.c, o: pend.o, d: pend.d, f: pend.f, g: pend.g, r: pend.r };
      pend = { a: 0, s: 0, c: 0, o: 0, d: 0, f: 0, g: 0, r: '' };
      try { browser.runtime.sendMessage(m).catch(function () {}); } catch (e) { /* ignore */ }
    }

    function find() { return document.getElementById('movie_player') || document.querySelector('.html5-video-player'); }
    function mainVideo() { return player ? (player.querySelector('video.html5-main-video') || player.querySelector('video')) : null; }
    function adNow() { var c = player && player.classList; return !!(c && (c.contains('ad-showing') || c.contains('ad-interrupting'))); }
    function visible(el) { try { var r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; } catch (e) { return false; } }

    function attach() {
      if (off) return;
      var p = find();
      if (!p) return;
      if (p !== player) {
        if (obs) obs.disconnect();
        if (adsObs) { adsObs.disconnect(); adsEl = null; }
        if (active) finish(false);
        player = p; givenUp = false;
        obs = new MutationObserver(check);
        obs.observe(p, { attributes: true, attributeFilter: ['class'] });
        p.addEventListener('durationchange', filmLength, true);
        filmLength();
      }
      watchOverlays();
      check();
    }
    function filmLength() { // the length of the film itself: remembered while no ad is showing
      try { var v = mainVideo(); if (v && !adNow() && isFinite(v.duration) && v.duration > 0) mainDur = v.duration; } catch (e) { /* ignore */ }
    }
    function watchOverlays() { // the element that holds overlay ads appears after the player: look for it every time
      try {
        if (!player) return;
        var el = player.querySelector('.video-ads');
        if (el && el !== adsEl) { if (adsObs) adsObs.disconnect(); adsEl = el; adsObs = new MutationObserver(overlaySoon); adsObs.observe(el, { childList: true, subtree: true }); }
      } catch (e) { /* ignore */ }
    }
    function overlaySoon() { if (!overlayTimer) overlayTimer = setTimeout(function () { overlayTimer = 0; closeOverlay(); }, 100); }
    function closeOverlay() {
      try {
        if (off || !player) return;
        var list = player.querySelectorAll(OVERLAY);
        for (var i = 0; i < list.length; i++) if (visible(list[i])) { list[i].click(); mark('o'); return; }
      } catch (e) { /* ignore */ }
    }

    function check() {
      if (off || !player) return;
      var a = adNow();
      if (!a) givenUp = false;
      if (a && !active && !givenUp) begin();
      else if (!a && active) finish(true);
    }
    function begin() {
      mark('a');
      active = { t0: Date.now(), saved: null, timer: 0, jumped: false, clicked: null };
      tick();
    }
    function tick() {
      var a = active;
      if (!a) return;
      a.timer = 0;
      try {
        if (!adNow()) { finish(true); return; }
        if (Date.now() - a.t0 > MAXMS) { finish(false); return; }
        var v = mainVideo();
        if (v) {
          if (!a.saved) a.saved = { muted: v.muted, rate: v.playbackRate };
          if (!v.muted) v.muted = true;
          if (v.playbackRate !== 16) { try { v.playbackRate = 16; } catch (e) { pend.r = 'speed refused'; } }
          var d = v.duration;
          // an ad stream is short and is not the film: only then is it moved to its end (the ad class can come a moment before the ad stream does)
          if (!a.jumped && Date.now() - a.t0 >= 300 && isFinite(d) && d > 0 && d < 180 && Math.abs(d - mainDur) > 0.5 && v.currentTime < d - 0.2) {
            try { v.currentTime = d - 0.1; a.jumped = true; mark('f'); } catch (e) { pend.r = 'seek refused'; }
          }
        }
        clickSkip(a);
        closeOverlay();
      } catch (e) { pend.r = String((e && e.message) || e).slice(0, 40); }
      if (active === a) a.timer = setTimeout(tick, LOOP);
    }
    function clickSkip(a) {
      for (var s = 0; s < SKIP.length; s++) {
        var list = player.querySelectorAll(SKIP[s]);
        for (var i = 0; i < list.length; i++) {
          if (list[i] !== a.clicked && visible(list[i])) { a.clicked = list[i]; list[i].click(); mark('c'); return;
          }
        }
      }
    }
    function finish(ok) {
      var a = active;
      active = null;
      if (!a) return;
      if (a.timer) clearTimeout(a.timer);
      try {
        var v = mainVideo();
        if (v && a.saved) { // put back what was ours; what somebody else changed in the meantime stays
          if (v.muted && !a.saved.muted) v.muted = false;
          if (v.playbackRate === 16) v.playbackRate = a.saved.rate || 1;
        }
      } catch (e) { /* ignore */ }
      if (ok) failsInRow = 0;
      else {
        givenUp = true; failsInRow++; mark('g');
        pend.r = 'an ad was still showing after ' + Math.round(MAXMS / 1000) + ' s';
        if (failsInRow >= GIVEUP) { off = true; pend.r = 'gave up: ' + failsInRow + ' ads in a row would not end'; if (obs) obs.disconnect(); if (adsObs) adsObs.disconnect(); }
      }
      mark();
    }

    function closest(el, tag) { for (var n = el; n && n !== document; n = n.parentNode) if (n.tagName && n.tagName.toLowerCase() === tag) return n; return null; }
    function findDialog() {
      var el = document.querySelector('ytd-enforcement-message-view-model');
      if (el) return el;
      var list = document.querySelectorAll('tp-yt-paper-dialog, ytd-popup-container [role="dialog"]');
      for (var i = 0; i < list.length && i < 12; i++) { var t = String(list[i].textContent || '').toLowerCase(); if (t.indexOf('ad blocker') >= 0) return list[i]; }
      return null;
    }
    function checkDialog() {
      try {
        if (off) return;
        var el = findDialog();
        if (!el) return;
        var dlg = closest(el, 'tp-yt-paper-dialog') || el;
        if (dlg.parentNode) dlg.parentNode.removeChild(dlg);
        var bd = document.querySelectorAll('tp-yt-iron-overlay-backdrop');
        for (var i = 0; i < bd.length; i++) if (bd[i].parentNode) bd[i].parentNode.removeChild(bd[i]);
        if (document.documentElement.style.overflow === 'hidden') document.documentElement.style.overflow = '';
        if (document.body && document.body.style.overflow === 'hidden') document.body.style.overflow = '';
        var v = mainVideo() || document.querySelector('video');
        if (v && v.paused) { try { var p = v.play(); if (p && p.catch) p.catch(function () { /* ignore */ }); } catch (e) { /* ignore */ } }
        mark('d');
      } catch (e) { /* ignore */ }
    }

    function later(fn, list) { for (var i = 0; i < list.length; i++) setTimeout(fn, list[i]); }
    function onNav() { later(attach, [200, 1500, 4000]); later(checkDialog, [1500, 5000]); }
    function start() {
      attach();
      later(attach, [1000, 3000, 8000]);
      later(checkDialog, [1500, 4000, 9000]);
      document.addEventListener('yt-navigate-finish', onNav, true);
      window.addEventListener('popstate', onNav, false);
    }

    browser.runtime.sendMessage({ k: 'guard', h: location.hostname }).then(function (r) { if (r && r.skip === true) start(); }, function () { /* the app is not answering: nothing is done */ });
  } catch (e) { /* never break the page */ }
})();
