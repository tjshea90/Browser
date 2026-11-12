'use strict';
// Element hiding at document_start: ask for the CSS that hides ad containers on this host and inject it before the page paints them.
(function () {
  try {
    var host = location.hostname;
    if (!host) return;
    browser.runtime.sendMessage({ k: 'css', h: host, top: window.top === window }).then(function (css) {
      if (!css) return;
      var st = document.createElement('style');
      st.setAttribute('data-kestrel', '1');
      st.textContent = css;
      (document.head || document.documentElement).appendChild(st);
    }, function () {});
  } catch (e) { /* never break the page */ }
})();

// What this page's players say about the files they play: picture size, length, and the label of the matching <source> ("720p"). The app joins it to the sniffed URL.
(function () {
  try {
    var sent = {};
    function labelOf(v, src) {
      try {
        var list = v.querySelectorAll ? v.querySelectorAll('source') : [];
        for (var i = 0; i < list.length; i++) {
          var e = list[i];
          if (e.src !== src) continue;
          return e.getAttribute('label') || e.getAttribute('title') || e.getAttribute('res') || e.getAttribute('data-res') || e.getAttribute('data-quality') || e.getAttribute('size') || '';
        }
      } catch (x) { /* ignore */ }
      return '';
    }
    function report(v) {
      var src = v.currentSrc || v.src || '';
      if (!src || src.indexOf('http') !== 0) return;
      var key = src + '|' + v.videoHeight + '|' + Math.round(v.duration || 0);
      if (sent[key]) return;
      sent[key] = true;
      browser.runtime.sendMessage({ k: 'vid', u: src, w: v.videoWidth || 0, h: v.videoHeight || 0, d: isFinite(v.duration) ? v.duration : 0, l: labelOf(v, src), p: location.href }).catch(function () {});
    }
    document.addEventListener('loadedmetadata', function (e) { var t = e.target; if (t && (t.tagName === 'VIDEO' || t.tagName === 'AUDIO')) report(t); }, true);
  } catch (e) { /* never break the page */ }
})();

// Pop-up and redirect guard: guard-main.js runs inside the page and talks to us with events (it cannot reach the extension itself). We tell it whether the guard applies to this site and relay what it
// blocked and which link was last tapped to the background page.
(function () {
  try {
    var CFG = '__kst_cfg_v1', POP = '__kst_pop_v1', READY = '__kst_ready_v1';
    var wanted = true, topHost = '';
    function push() { try { document.dispatchEvent(new CustomEvent(CFG, { detail: wanted ? '1|' + topHost : '0' })); } catch (e) { /* ignore */ } }
    document.addEventListener(READY, push, true); // the page-side script may come up after our first answer
    document.addEventListener(POP, function (e) {
      try { browser.runtime.sendMessage({ k: 'pop', d: String(e.detail).slice(0, 700), p: location.href }).catch(function () {}); } catch (x) { /* ignore */ }
    }, true);
    browser.runtime.sendMessage({ k: 'guard', h: location.hostname }).then(function (r) { wanted = !(r && r.on === false); topHost = (r && r.top) ? String(r.top) : ''; push(); }, function () {});
  } catch (e) { /* never break the page */ }
})();

// Media addresses that sit in the page itself (owner request X7: "I need to press play ... I want to download the video cleanly"): <video> / <source> elements, og:video tags, links to media files and the
// player configuration inside inline scripts and JSON. Found without playing anything, so the download list exists before the page's play button (and the spam behind it) is touched.
(function () {
  try {
    var EXT = /\.(m3u8|mpd|mp4|webm|mkv|m4v|mov|mp3|m4a|aac|ogg|opus|flac)(?:$|[?#])/i;
    var FIND = /https?:\/\/[^\s"'<>()\\]+?\.(?:m3u8|mpd|mp4|webm|mkv|m4v|mov|mp3|m4a|aac|ogg|opus|flac)(?:\?[^\s"'<>()\\]*)?/gi;
    var HINT = /\.(?:m3u8|mpd|mp4|webm|mkv|m4v|mov|mp3|m4a|aac|ogg|opus|flac)/i; // a script that names no media file at all is not run through the three replaces and FIND below
    var sent = {}, n = 0, scriptsDone = 0, linksDone = 0; // the scan at load only looks at what appeared since the first one
    function report(u) {
      if (!u || n >= 40) return;
      try { u = new URL(u.replace(/&amp;/g, '&'), location.href).href; } catch (e) { return; }
      if (u.indexOf('http') !== 0 || sent[u] || !EXT.test(u)) return;
      sent[u] = 1; n++;
      browser.runtime.sendMessage({ k: 'dom', u: u, p: location.href }).catch(function () {});
    }
    function scan() {
      try {
        var i, e, list = document.querySelectorAll('video, audio, source[src]');
        for (i = 0; i < list.length; i++) { e = list[i]; report(e.currentSrc || e.src || e.getAttribute('src')); }
        var metas = document.querySelectorAll('meta[property^="og:video"], meta[property^="og:audio"], meta[name="twitter:player:stream"], link[rel="preload"][as="video"]');
        for (i = 0; i < metas.length; i++) report(metas[i].getAttribute('content') || metas[i].getAttribute('href'));
        var links = document.links; // a live collection: no list of a few thousand anchors is built just to read the first 600
        for (i = linksDone; i < links.length && i < 600; i++) if (EXT.test(links[i].href)) report(links[i].href);
        linksDone = Math.min(links.length, 600);
        var scripts = document.scripts, budget = 300000;
        for (i = scriptsDone; i < scripts.length && budget > 0 && n < 40; i++) {
          var s = scripts[i];
          if (s.src) continue;
          var tn = s.firstChild; // the size is read off the text node BEFORE any text is copied: a framework bundle or a state blob of a megabyte or two is never turned into a string
          if (!tn || s.childNodes.length !== 1 || tn.nodeType !== 3 || tn.length > 200000) continue;
          var t = tn.data;
          budget -= t.length;
          if (!HINT.test(t)) continue;
          t = t.replace(/\\u002f/gi, '/').replace(/\\u0026/gi, '&').replace(/\\\//g, '/');
          var m; FIND.lastIndex = 0;
          while ((m = FIND.exec(t)) !== null && n < 40) report(m[0]);
        }
        scriptsDone = i;
      } catch (x) { /* never break the page */ }
    }
    function later() { if (window.requestIdleCallback) requestIdleCallback(scan, { timeout: 3000 }); else setTimeout(scan, 1500); }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', later); else later();
    window.addEventListener('load', function () { setTimeout(later, 2500); });
  } catch (e) { /* never break the page */ }
})();

// Long press on a video (owner requests X8/X14: "I long pressed a video and it still didn't show download"): a site's own player draws its OWN menu on a long press, so the browser's menu - and with
// it the way to the download list - never appears. The long press is taken here, in the capture phase, before the page's players see it, when a real video or audio element sits under the finger
// (even beneath a transparent layer); the app then shows its menu (Download video, Copy video address). Only elements that have a source are taken, so a bare element does not lose the browser's menu.
(function () {
  try {
    window.addEventListener('contextmenu', function (e) {
      try {
        if (!e.isTrusted) return;
        var els = document.elementsFromPoint ? document.elementsFromPoint(e.clientX, e.clientY) : [e.target];
        var media = null;
        for (var i = 0; i < els.length && i < 16; i++) {
          var n = els[i].tagName;
          if (n === 'VIDEO' || n === 'AUDIO') { media = els[i]; break; }
        }
        if (!media) return;
        var src = media.currentSrc || media.src || '';
        if (!src && !(media.querySelector && media.querySelector('source'))) return;
        e.preventDefault(); e.stopImmediatePropagation();
        browser.runtime.sendMessage({ k: 'pop', d: 'vmenu|' + (src.indexOf('http') === 0 ? src : ''), p: location.href }).catch(function () {});
      } catch (x) { /* never break the page */ }
    }, true);
  } catch (e) { /* never break the page */ }
})();

// A field tapped in the lower half of the screen stayed hidden behind the keyboard until the first letter was typed (research/TYPING.md): Firefox shrinks only the VISIBLE area when the keyboard opens (the page's
// layout keeps its size, so nothing is laid out again, which is why typing stays cheap) and scrolls to the caret when a key is typed, not when the field is tapped. When the visible area shrinks while a text field
// has the focus and the field is no longer inside it, bring the field into view. Costs one listener per page and does nothing unless the keyboard opened.
(function () {
  try {
    var vv = window.visualViewport;
    if (!vv || !window.addEventListener) return;
    var last = vv.height, timer = 0;
    function editable(a) {
      if (!a || a === document.body || a === document.documentElement) return false;
      var t = a.tagName;
      if (t === 'TEXTAREA' || t === 'SELECT') return true;
      if (t === 'INPUT') {
        var ty = (a.type || '').toLowerCase();
        return !(ty === 'checkbox' || ty === 'radio' || ty === 'button' || ty === 'submit' || ty === 'reset' || ty === 'image' || ty === 'file' || ty === 'range' || ty === 'color');
      }
      return !!a.isContentEditable;
    }
    function reveal() {
      timer = 0;
      var a = document.activeElement;
      if (!editable(a)) return;
      var r = a.getBoundingClientRect();
      if (r.top >= vv.offsetTop && r.bottom <= vv.offsetTop + vv.height) return; // already inside the visible area
      a.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'instant' });
    }
    vv.addEventListener('resize', function () {
      var h = vv.height, shrank = h < last - 60;
      last = h;
      if (!shrank) return;
      if (timer) clearTimeout(timer);
      timer = setTimeout(reveal, 120); // the keyboard opens in two steps (its suggestion strip comes second): look at the final size
    });
  } catch (e) { /* never break the page */ }
})();

