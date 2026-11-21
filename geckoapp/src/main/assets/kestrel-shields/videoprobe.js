'use strict';
// VD1 (owner 2026-11-21: "diagnose why Instagram reels don't load and some videos don't load on websites"): tells the app WHEN a media element fails and how, so a video that "does not load" has a reason in the diagnostics.
// What the page-side gate (gate-main.js) holds back and what the format policy (codec-main.js) refuses is counted there; what neither of them sees is the engine saying no: an element's `error` event (the code of
// MediaError: 1 aborted, 2 network, 3 could not be decoded, 4 source not supported) or a <source> child that failed (told here as 9: the engine gives that error to the child, not to the element). A few small words and
// numbers go to the extension's background page, once per kind of failure per frame, at most five per frame: the element's kind (video or audio), the code, networkState and readyState, where the file comes from
// (none / blob: a script-made MediaSource / hls / dash / http: the kind, NEVER the address) and whether it is the top page or a frame. The background page checks them again and the app adds the site. Runs in every
// frame at document_start; a few capture listeners, nothing else. Conservative ES5. Tested in headless Chromium by web-tests/videoprobe.test.js.
(function () {
  try {
    if (window.__kstVP) return;
    if (typeof browser === 'undefined' || !browser || !browser.runtime) return;
    if (!/^https?:$/.test(String(location.protocol))) return;
    window.__kstVP = 1;
    var left = 5, seen = [];
    function kind(el) {
      var u = '';
      try { u = String(el.currentSrc || el.src || ''); } catch (e) { u = ''; }
      if (!u) return 'none';
      if (u.indexOf('blob:') === 0) return 'blob';
      if (/\.m3u8([?#]|$)/i.test(u)) return 'hls';
      if (/\.mpd([?#]|$)/i.test(u)) return 'dash';
      return 'http';
    }
    function onError(ev) {
      try {
        var t = ev && ev.target;
        if (!t || left <= 0) return;
        var name = t.nodeName, media = t, code;
        if (name === 'SOURCE') {                              // a <source> that failed: the engine tells the child, not the element
          media = t.parentNode;
          if (!media || (media.nodeName !== 'VIDEO' && media.nodeName !== 'AUDIO')) return;
          code = 9;
        } else if (name === 'VIDEO' || name === 'AUDIO') {
          code = (t.error && t.error.code) ? t.error.code : 0;
        } else return;
        var k = kind(media), key = media.nodeName + code + k;
        if (seen.indexOf(key) >= 0) return;
        seen.push(key); left--;
        browser.runtime.sendMessage({ k: 'verr', c: code, ns: media.networkState | 0, rs: media.readyState | 0, s: k, f: window === window.top ? 1 : 0, e: media.nodeName === 'AUDIO' ? 'a' : 'v' }).catch(function () { /* no background page: nothing to do */ });
      } catch (e) { /* a page must never feel this script */ }
    }
    document.addEventListener('error', onError, true);

    // VD2c (owner 2026-11-21: a pornhub video "constantly loads and doesn't play", with Shields off and the press-to-play rule off): a player that waits for something that never comes gives NO error event, so the
    // failure above never fires. A media element that is trying (it was told to play, has autoplay, or is loading) and still has no data to play (readyState below 3) after STALL_MS is reported once as code 99
    // ("stuck loading") with its two states, where its file comes from, whether it is paused and how many pieces are buffered. Nothing about the page or the file leaves: numbers and a kind. At most one timer per element.
    var STALL_MS = (typeof window.__kstVPstallMs === 'number' && window.__kstVPstallMs >= 200) ? window.__kstVPstallMs : 15000, timers = [], reported = [];
    function slot(el) { for (var i = 0; i < timers.length; i++) if (timers[i].el === el) return i; return -1; }
    function disarm(el) { var i = slot(el); if (i >= 0) { try { clearTimeout(timers[i].t); } catch (e) { /* gone */ } timers.splice(i, 1); } }
    function check(el) {
      try {
        disarm(el);
        if (left <= 0 || !el.isConnected || el.error || el.ended || el.readyState >= 3) return;
        var trying = !el.paused || el.autoplay || el.networkState === 2;
        if (!trying) return;
        var k = kind(el), key = 'S' + el.nodeName + k + (el.networkState | 0) + (el.readyState | 0);
        if (reported.indexOf(key) >= 0) return;
        reported.push(key); left--;
        var nb = 0; try { nb = Math.min(9, el.buffered ? el.buffered.length : 0); } catch (e) { nb = 0; }
        browser.runtime.sendMessage({ k: 'verr', c: 99, ns: el.networkState | 0, rs: el.readyState | 0, s: k, f: window === window.top ? 1 : 0, e: el.nodeName === 'AUDIO' ? 'a' : 'v', pa: el.paused ? 1 : 0, b: nb }).catch(function () { /* nothing to do */ });
      } catch (e) { /* a page must never feel this script */ }
    }
    function arm(ev) {
      try {
        var el = ev && ev.target;
        if (!el || (el.nodeName !== 'VIDEO' && el.nodeName !== 'AUDIO') || left <= 0 || slot(el) >= 0 || timers.length >= 8) return;
        timers.push({ el: el, t: setTimeout(function () { check(el); }, STALL_MS) });
      } catch (e) { /* ignore */ }
    }
    function okay(ev) { try { var el = ev && ev.target; if (el && el.readyState >= 3) disarm(el); else if (ev && (ev.type === 'ended' || ev.type === 'emptied')) disarm(el); } catch (e) { /* ignore */ } }
    var armers = ['loadstart', 'play', 'waiting', 'stalled'], clearers = ['canplay', 'canplaythrough', 'playing', 'ended', 'emptied'], q;
    for (q = 0; q < armers.length; q++) document.addEventListener(armers[q], arm, true);
    for (q = 0; q < clearers.length; q++) document.addEventListener(clearers[q], okay, true);
    window.KestrelVP = { seen: function () { return seen.slice(); }, stalls: function () { return reported.slice(); } };      // for the tests
  } catch (e) { /* ignore */ }
})();
