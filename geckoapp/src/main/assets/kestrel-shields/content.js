'use strict';
// Element hiding at document_start: ask for the CSS that hides ad containers on this host and inject it before the page paints them.
// D2: when that CSS starts with the marker, the generic selectors that name a class or an id were NOT sent (there are ~29,000 of them): this top document tells the app which classes and ids it holds,
// in small batches (the page at load, then what gets added), and injects only the selectors that can match (research/ADBLOCK_ENGINE.md, core/filter/CosmeticTokens.kt).
(function () {
  function inject(css, tag) {
    var st = document.createElement('style');
    st.setAttribute('data-kestrel', tag);
    st.textContent = css;
    var into = document.head || document.documentElement;
    if (into) { into.appendChild(st); return; }
    // document_start can run before the page's first element exists (an answer from the cache comes back at once): wait for it instead of losing the CSS
    var mo = new MutationObserver(function () {
      var p = document.head || document.documentElement;
      if (p) { mo.disconnect(); p.appendChild(st); }
    });
    mo.observe(document, { childList: true });
  }
  function startTokens(host) {
    try {
      var seen = Object.create(null), fresh = [], timer = 0, batches = 0, sent = 0, off = false;
      var MAX_BATCHES = 60, MAX_SENT = 8000, BATCH = 300, DELAY = 350; // a page that keeps inventing names (generated ids) stops being asked
      // RX3-S3: a TIME budget as well (the procedural observer below has one): this callback runs on the page's own main thread for as long as the page keeps adding nodes with names it has not seen, so on a page that
      // never stops it costs time for ever. Now: 300 ms of its own work in total, or 20 s after the page was parsed, whichever comes first; nothing while the page is hidden (looked at again when it is shown);
      // nothing for what happens inside an editable region (typing in a rich editor adds nodes all the time). The tests shorten the limits through window.__kstTokCfg (a page's globals are not visible here in the real extension).
      var TC = (window.__kstTokCfg && typeof window.__kstTokCfg === 'object') ? window.__kstTokCfg : {};
      var BUDGET_MS = TC.budgetMs || 300, WINDOW_MS = TC.windowMs || 20000, spent = 0, startedAt = 0;
      function nowMs() { if (typeof TC.now === 'function') return TC.now(); return (window.performance && performance.now) ? performance.now() : Date.now(); }   // (TC.now: the tests' own clock, so a slow or busy machine cannot change what they prove)
      function note(t) {
        if (t.length > 81 || seen[t]) return;
        seen[t] = 1; fresh.push(t);
      }
      var seenCls = Object.create(null), nCls = 0;
      function addEl(el) {
        var id = el.id;
        if (id && typeof id === 'string') note('#' + id.toLowerCase());
        var cls = el.getAttribute ? el.getAttribute('class') : null; // not className: on an SVG element that is an object
        if (cls) {
          // LAB5 (research/LAB5_EXTENSION.md row 3): the same attribute text again (500 list items with class="item", and the second whole-page pass at load) holds only names that are already noted: skip the split,
          // the lower-casing and the per-name table lookups. Bounded: short strings only, 3000 of them.
          if (seenCls[cls] === 1) return;
          if (cls.length <= 160 && nCls < 3000) { seenCls[cls] = 1; nCls++; }
          var parts = cls.split(/\s+/);
          for (var i = 0; i < parts.length; i++) if (parts[i]) note('.' + parts[i].toLowerCase());
        }
      }
      function scan(root) {
        var list = root.querySelectorAll('[id],[class]');
        for (var i = 0; i < list.length && i < 6000; i++) addEl(list[i]);
      }
      function flush() {
        timer = 0;
        if (off || fresh.length === 0) return;
        var batch = fresh.splice(0, BATCH);
        batches++; sent += batch.length;
        if (batches >= MAX_BATCHES || sent >= MAX_SENT) off = true;
        browser.runtime.sendMessage({ k: 'gen', h: host, t: batch }).then(function (css) { if (css) inject(css, 'g'); }, function () {});
        if (fresh.length && !off) timer = setTimeout(flush, DELAY);
      }
      function later() { if (!timer && !off) timer = setTimeout(flush, DELAY); }
      function first() { if (!startedAt) startedAt = nowMs(); scan(document); later(); }
      if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', first); else first();
      // LAB5: the second whole-page pass (classes that scripts gave to EXISTING elements after DOMContentLoaded; the observer below only sees new nodes) runs when the page is idle, not inside the busiest moment of the load
      window.addEventListener('load', function () { if (window.requestIdleCallback) requestIdleCallback(first, { timeout: 1000 }); else setTimeout(first, 300); });
      var missed = false;   // RVW1 P6: the page changed while it was hidden (the observer saw it and dropped it)
      if (window.MutationObserver) {
        var mo = new MutationObserver(function (recs) {
          if (off) { mo.disconnect(); return; }
          if (document.hidden) { missed = true; return; }                     // nobody sees the page: visibilitychange looks again when it is shown (RVW1 P6: only if something changed meanwhile)
          var t0 = nowMs();
          if (spent > BUDGET_MS || (startedAt && t0 - startedAt > WINDOW_MS)) { off = true; mo.disconnect(); return; }
          for (var r = 0; r < recs.length && r < 200; r++) {
            var tg = recs[r].target;
            if (tg && tg.isContentEditable) continue;                         // typing in an editor
            var added = recs[r].addedNodes;
            for (var k = 0; k < added.length && k < 50; k++) {
              var n = added[k];
              if (n.nodeType !== 1) continue;
              addEl(n);
              if (n.firstElementChild) { var sub = n.querySelectorAll('[id],[class]'); for (var m = 0; m < sub.length && m < 300; m++) addEl(sub[m]); }
            }
          }
          if (fresh.length) later();
          spent += nowMs() - t0;
        });
        mo.observe(document, { childList: true, subtree: true }); // the document itself: its root element may not exist yet
        // RVW1 P6: this rescanned the WHOLE page (querySelectorAll over every id / class, up to 6000 elements: 2-3 ms on a desktop, 15-25 ms on the tablet) at every unlock and tab return even when nothing had changed while it was hidden
        document.addEventListener('visibilitychange', function () { if (!document.hidden && !off && missed) { missed = false; scan(document); later(); } });
      }
    } catch (e) { /* never break the page */ }
  }
  // ---- AD1b-4: procedural cosmetic rules (`:has-text()`, `:upward()`, `:remove()` ...). The app parsed and checked them (core ProceduralRules); a line is the base selector, then for every operator
  // OP name ARG argument, with the control characters below. Names: t = has-text, u = upward, r = remove, a = remove-attr, c = remove-class. No eval, no innerHTML: the only things done with the page are
  // querySelectorAll, text reads, a hide (display none), a removal, an attribute or class change. The work is bounded: few candidates per run, text read up to 20,000 characters, a time budget, few runs.
  function parseProcedural(text) {
    var out = [], lines = String(text).split('\n');
    for (var i = 0; i < lines.length && out.length < 200; i++) {
      var parts = lines[i].split('\u0003');
      if (!parts[0]) continue;
      var rule = { sel: parts[0], ops: [] }, ok = true;
      for (var k = 1; k < parts.length; k++) {
        var j = parts[k].indexOf('\u0004');
        if (j < 0) { ok = false; break; }
        var name = parts[k].slice(0, j), arg = parts[k].slice(j + 1), op = { n: name, a: arg };
        if (name === 't') {
          var e = arg.lastIndexOf('/');
          if (arg.charAt(0) === '/' && e > 0) { try { op.re = new RegExp(arg.slice(1, e), arg.slice(e + 1).replace(/[gy]/g, '')); } catch (x) { ok = false; break; } }
        } else if (name === 'u') { var nn = parseInt(arg, 10); if (String(nn) === arg) op.k = nn; }
        rule.ops.push(op);
      }
      if (ok && rule.ops.length) out.push(rule);
    }
    return out;
  }
  function textOf(el) { var t = el.textContent || ''; return t.length > 20000 ? t.slice(0, 20000) : t; }
  function applyProcedural(rule, budget) {
    var nodes;
    try { nodes = document.querySelectorAll(rule.sel); } catch (x) { return 0; }
    var seen = 0;
    for (var i = 0; i < nodes.length && budget.n > 0; i++) {
      var cur = nodes[i], act = null;
      if (cur.getAttribute && cur.getAttribute('data-kst-p') === '1') continue;      // already handled
      budget.n--; seen++;
      for (var o = 0; o < rule.ops.length && cur; o++) {
        var op = rule.ops[o];
        if (op.n === 't') { var t = textOf(cur); if (!(op.re ? op.re.test(t) : t.indexOf(op.a) >= 0)) cur = null; }
        else if (op.n === 'u') {
          if (op.k) { for (var h = 0; h < op.k && cur; h++) cur = cur.parentElement; }
          else { try { cur = cur.parentElement ? cur.parentElement.closest(op.a) : null; } catch (x) { cur = null; } }
        } else act = op;
      }
      if (!cur || cur === document.documentElement || cur === document.body) continue;
      if (!act) { cur.style.setProperty('display', 'none', 'important'); cur.setAttribute('data-kst-p', '1'); }
      else if (act.n === 'r') { if (cur.parentNode) cur.parentNode.removeChild(cur); }
      else if (act.n === 'a') { cur.removeAttribute(act.a); cur.setAttribute('data-kst-p', '1'); }
      else if (act.n === 'c') { cur.classList.remove(act.a); cur.setAttribute('data-kst-p', '1'); }
    }
    return seen;
  }
  function startProcedural(host) {
    try {
      browser.runtime.sendMessage({ k: 'proc', h: host }).then(function (text) {
        var rules = text ? parseProcedural(text) : [];
        if (!rules.length) return;
        var runs = 0, spent = 0, timer = 0;
        function run() {
          timer = 0;
          if (runs >= 40 || spent > 1500) return;
          runs++;
          var t0 = Date.now(), budget = { n: 400 };
          for (var r = 0; r < rules.length && budget.n > 0; r++) applyProcedural(rules[r], budget);
          spent += Date.now() - t0;
        }
        function later() { if (!timer && runs < 40 && spent <= 1500) timer = setTimeout(run, 400); }
        if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', later); else later();
        window.addEventListener('load', later);
        if (window.MutationObserver) {
          var mo = new MutationObserver(function () { if (runs >= 40 || spent > 1500) { mo.disconnect(); return; } later(); });
          mo.observe(document, { childList: true, subtree: true, characterData: true });
        }
      }, function () {});
    } catch (e) { /* never break the page */ }
  }
  // AW1b: the scriptlet calls of this site reach the page-world library (scriptlets-main.js, world MAIN) two ways: EARLY, as a script the extension registered for the site before the document started
  // (background.js, it runs ahead of the page's own scripts), and LATE, from here: they ride in the CSS answer as a comment right after the markers and are handed over by a DOM event (the library
  // cannot reach the extension and we cannot reach the page's world). The library runs every call once, so the second delivery is harmless; it says when it is up (READY: it may come after our answer)
  // and how many calls were new (REPORT). We listen for the report from document start, because the early run happens before the CSS answer exists.
  var slFirst = 1;
  try {
    document.addEventListener('__kst_sl_n_v1', function (e) {
      try { browser.runtime.sendMessage({ k: 'sl', r: String(e.detail).slice(0, 20), h: location.hostname, f: slFirst }).catch(function () {}); slFirst = 0; } catch (x) { /* ignore */ }
    }, true);
  } catch (e) { /* never break the page */ }
  function startScriptlets(json, host) {
    try {
      var SL = '__kst_sl_v1', READY = '__kst_sl_ready_v1';
      function send() { try { document.dispatchEvent(new CustomEvent(SL, { detail: json })); } catch (e) { /* ignore */ } }
      document.addEventListener(READY, send, true);
      send();
    } catch (e) { /* never break the page */ }
  }
  try {
    var host = location.hostname;
    if (!host) return;
    var top = window.top === window;
    browser.runtime.sendMessage({ k: 'css', h: host, top: top }).then(function (css) {
      if (!css) return;
      var ks = css.indexOf('/*ks');
      if (ks >= 0 && ks <= 12) { // cut the calls out before the CSS is injected: a comment of a few KB is not something a style sheet should carry
        var end = css.indexOf('*/', ks);
        if (end > ks) { startScriptlets(css.slice(ks + 4, end), host); css = css.slice(0, ks) + css.slice(end + 2); }
      }
      if (!css) return;
      inject(css, '1');
      if (top && css.indexOf('/*kg*/') === 0) startTokens(host);
      var kp = css.indexOf('/*kp*/');
      if (kp >= 0 && kp < 12) startProcedural(host);
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
    // RX3-V1 (research/FINDER_LAB.md section 1): what this element is FOR. Facts only, as small whole numbers; the app decides (core VideoRoleRules). muted 1, loop 2, autoplay 4, controls 8, visible 16, ad 32.
    // controls = the attribute OR a known player's own control bar around the element; ad = an ancestor (8 levels, never body) whose class or id holds ad / ads / advert / advertisement / adslot as a WHOLE word
    // (split at every character that is not a letter or digit: ad-container, banner-ad, adslot-3, jw-flag-ads, vjs-ad-playing, ima-ad-container, plyr__ads, ad-showing; not address, headline, download, shadow).
    var OWN_CONTROLS = ['jwplayer', 'video-js', 'plyr', 'mejs__container', 'flowplayer', 'shaka-video-container', 'html5-video-player', 'fp-player'];
    var AD_WORDS = { ad: 1, ads: 1, advert: 1, advertisement: 1, adslot: 1 };
    function hasAdWord(s) {
      if (!s || typeof s !== 'string') return false;
      var t = s.toLowerCase().split(/[^a-z0-9]+/);
      for (var i = 0; i < t.length; i++) if (AD_WORDS[t[i]] === 1) return true;
      return false;
    }
    function factsOf(v) {
      var f = 0, controls = !!v.controls, ad = false;
      try {
        if (v.muted) f |= 1;
        if (v.loop) f |= 2;
        if (v.autoplay) f |= 4;
        var n = v.parentNode, i = 0;
        while (n && n.nodeType === 1 && i < 8 && n !== document.body && n !== document.documentElement) {
          var cl = n.classList;
          if (cl && !controls) for (var k = 0; k < OWN_CONTROLS.length; k++) if (cl.contains(OWN_CONTROLS[k])) { controls = true; break; }
          if (!ad) ad = hasAdWord(n.getAttribute('class')) || hasAdWord(n.id);
          n = n.parentNode; i++;
        }
        if (controls) f |= 8;
        if (ad) f |= 32;
        var r = v.getBoundingClientRect(), rw = Math.round(r.width), rh = Math.round(r.height), vp = 0;
        var cs = window.getComputedStyle(v);
        if (rw > 0 && rh > 0 && cs.visibility !== 'hidden' && cs.display !== 'none') f |= 16;
        if (r.width > 0 && r.height > 0) {
          var vw = window.innerWidth || document.documentElement.clientWidth || 0, vh = window.innerHeight || document.documentElement.clientHeight || 0;
          var ix = Math.max(0, Math.min(r.right, vw) - Math.max(r.left, 0)), iy = Math.max(0, Math.min(r.bottom, vh) - Math.max(r.top, 0));
          vp = Math.max(0, Math.min(100, Math.round(100 * ix * iy / (r.width * r.height))));
        }
        return { f: f, rw: Math.max(0, Math.min(20000, rw)), rh: Math.max(0, Math.min(20000, rh)), vp: vp };
      } catch (e) { return { f: f, rw: 0, rh: 0, vp: 0 }; }
    }
    // RX3-V4: a player that feeds a blob: address (a MediaSource: hls.js, shaka, dash.js, most embeds) has no address worth sending, but its length and picture height can be matched to a playlist the app has read. Only the
    // word "blob" goes (never the blob: address); the app's core MediaList.applyBlob does the matching.
    function isBlob(src) { return src.indexOf('blob:') === 0; }
    function report(v, force) {
      var src = v.currentSrc || v.src || '';
      var blob = isBlob(src);
      if (!src || (!blob && src.indexOf('http') !== 0)) return;
      var key = (blob ? 'blob|' + elId(v) : src) + '|' + v.videoHeight + '|' + Math.round(v.duration || 0);
      if (sent[key] && !force) return;
      sent[key] = true;
      var x = factsOf(v);
      try { if (window.top !== window && window.KestrelVF) window.KestrelVF.offered = true; } catch (e) { /* ignore */ }
      browser.runtime.sendMessage({ k: 'vid', u: blob ? 'blob' : src, w: v.videoWidth || 0, h: v.videoHeight || 0, d: isFinite(v.duration) ? v.duration : 0, l: blob ? '' : labelOf(v, src), p: location.href, f: x.f, rw: x.rw, rh: x.rh, vp: x.vp, el: elId(v) }).catch(function () {});
    }
    function onMeta(e) { var t = e.target; if (t && (t.tagName === 'VIDEO' || t.tagName === 'AUDIO')) report(t); }
    document.addEventListener('loadedmetadata', onMeta, true);
    // RVW1 F7: the role of an element is sent once per file; a muted autoplay clip with no controls (a feed's preview, X / Reels) says "decoration", and when the person UNMUTES it, it is the video they came for: say it
    // again, once per element, so the app stops hiding it. (volumechange also fires when a script unmutes: the app only learns the element's facts, the 'played' proof still needs 3 s of real playback.)
    var unmuted = {};
    document.addEventListener('volumechange', function (e) {
      try {
        var t = e.target;
        if (!t || t.tagName !== 'VIDEO' || t.muted || !(t.volume > 0)) return;
        var id = elId(t);
        if (!id || unmuted[id]) return;
        unmuted[id] = 1; report(t, true);
      } catch (x) { /* never break the page */ }
    }, true);
    // RX3-V3 (research/FINDER_LAB.md section 4): which element really PLAYS. `el` names an element for the page's lifetime (a small whole number, nothing else about it), so the app can tell "the same player went from a
    // 15 s clip to the film" (a pre-roll) from "two different videos". After 3 s of real playback of a file, once per element and file, the same report goes again with ev:'played'.
    var elIds = (typeof WeakMap === 'function') ? new WeakMap() : null, elNext = 1, playedKeys = {};
    function elId(v) { try { if (!elIds) return 0; var i = elIds.get(v); if (!i) { i = elNext++; elIds.set(v, i); } return i; } catch (e) { return 0; } }
    function onPlaying(e) {
      try {
        var v = e.target;
        if (!v || (v.tagName !== 'VIDEO' && v.tagName !== 'AUDIO') || !e.isTrusted) return;
        var src = v.currentSrc || v.src || '';
        var blob = isBlob(src);
        if (!src || (!blob && src.indexOf('http') !== 0)) return;
        var dur0 = Math.round(v.duration || 0);
        var key = elId(v) + '|' + src + (blob ? '|' + dur0 : '');       // a blob: player may go from an advertisement to the film without a new address: the length tells them apart
        if (playedKeys[key]) return;
        var t0 = v.currentTime, tries = 0;
        var check = function () {
          try {
            if (playedKeys[key] || v.paused || v.ended || (v.currentSrc || v.src || '') !== src || (blob && Math.round(v.duration || 0) !== dur0)) return;
            if (v.currentTime < t0 + 2.5) { if (++tries < 8) setTimeout(check, 1500); return; }   // playing, but slowly (a slow tablet): look again a few times before giving up
            playedKeys[key] = true;
            var x = factsOf(v);
            browser.runtime.sendMessage({ k: 'vid', ev: 'played', u: blob ? 'blob' : src, w: v.videoWidth || 0, h: v.videoHeight || 0, d: isFinite(v.duration) ? v.duration : 0, l: blob ? '' : labelOf(v, src), p: location.href, f: x.f, rw: x.rw, rh: x.rh, vp: x.vp, el: elId(v) }).catch(function () {});
          } catch (err) { /* ignore */ }
        };
        setTimeout(check, 3000);
      } catch (err) { /* ignore */ }
    }
    document.addEventListener('playing', onPlaying, true);

    // RVW1 F5: a video INSIDE A SHADOW ROOT (Reddit's shreddit-player, Vidstack / Media Chrome, Yahoo's player) never told the document: media events do not cross a shadow boundary, so the two document-level listeners above never
    // saw it (no picture size, no length, no "it played", no match with its playlist). The page's videos are looked for in the shadow trees (KestrelVF, bounded) at most three times per page: once after load and once
    // after each of the first two real taps (the player is often built when it is pressed); each one found gets the same two listeners and, when it already has its metadata, reports at once.
    var watched = (typeof WeakMap === 'function') ? new WeakMap() : null, shadowRuns = 0, shadowTimer = 0;
    function watchShadow() {
      try {
        if (!watched || !window.KestrelVF || shadowRuns >= 3) return;
        shadowRuns++;
        var vs = window.KestrelVF.all('video', true);
        for (var i = 0; i < vs.length; i++) {
          var v = vs[i];
          if (watched.has(v) || v.getRootNode() === document) continue;              // a light-DOM video is covered by the document-level listeners
          watched.set(v, 1);
          v.addEventListener('loadedmetadata', onMeta, true);
          v.addEventListener('playing', onPlaying, true);
          if (v.readyState >= 1) report(v);
        }
      } catch (x) { /* never break the page */ }
    }
    function shadowSoon(ms) { if (shadowRuns >= 3 || shadowTimer) return; shadowTimer = setTimeout(function () { shadowTimer = 0; watchShadow(); }, ms); }
    window.addEventListener('load', function () { shadowSoon((window.__kstVfCfg && window.__kstVfCfg.shadowMs) || 3000); });
    window.addEventListener('click', function (e) { if (e && e.isTrusted) shadowSoon((window.__kstVfCfg && window.__kstVfCfg.shadowTapMs) || 1500); }, { capture: true, passive: true });

    // RX4-M0a (research/REVIEW-EXTERNAL-4.md): how smoothly do REAL videos play on this tablet? While an element plays, every 15 s (and when it pauses or ends, or the page is hidden) its
    // getVideoPlaybackQuality() is read and only the DIFFERENCE since the last look is sent: frames shown, dropped, corrupted, seconds, picture size. Numbers; never an address. One timer for
    // every element, none while nothing plays; the first look of an element only sets its baseline.
    var vqState = (typeof WeakMap === 'function') ? new WeakMap() : null, vqList = [], vqTimer = 0;
    function vqClamp(n, max) { n = Math.round(n) || 0; return n < 0 ? 0 : (n > max ? max : n); }
    function vqLook(v, final) {
      try {
        if (!vqState || !v || typeof v.getVideoPlaybackQuality !== 'function') return;
        var q = v.getVideoPlaybackQuality(), s = vqState.get(v), now = Date.now();
        var total = q.totalVideoFrames || 0, dropped = q.droppedVideoFrames || 0, corrupt = q.corruptedVideoFrames || 0;
        if (!s) { vqState.set(v, { t: total, d: dropped, c: corrupt, at: now }); return; }
        var dt = total - s.t, dd = dropped - s.d, dc = corrupt - s.c, secs = (now - s.at) / 1000;
        if (dt < 0 || dd < 0) { s.t = total; s.d = dropped; s.c = corrupt; s.at = now; return; }   // a new source started its counters again
        if (dt < (final ? 15 : 30)) return;                                                         // too few frames to say anything: look again later
        s.t = total; s.d = dropped; s.c = corrupt; s.at = now;
        browser.runtime.sendMessage({ k: 'vq', t: vqClamp(dt, 20000), d: vqClamp(dd, 20000), c: vqClamp(dc, 20000), s: vqClamp(secs, 600), w: vqClamp(v.videoWidth, 20000), h: vqClamp(v.videoHeight, 20000) });
      } catch (err) { /* ignore */ }
    }
    function vqTick() {
      vqTimer = 0;
      var any = false;
      for (var i = vqList.length - 1; i >= 0; i--) { var v = vqList[i]; if (v.paused || v.ended) { vqList.splice(i, 1); continue; } any = true; vqLook(v, false); }
      if (any) vqTimer = setTimeout(vqTick, 15000);
    }
    document.addEventListener('playing', function (e) {
      var v = e.target;
      if (!v || v.tagName !== 'VIDEO' || !e.isTrusted) return;
      if (vqList.indexOf(v) < 0) vqList.push(v);
      vqLook(v, false);
      if (!vqTimer) vqTimer = setTimeout(vqTick, 15000);
    }, true);
    function vqEnd(e) { var v = e.target; if (v && v.tagName === 'VIDEO') vqLook(v, true); }
    document.addEventListener('pause', vqEnd, true);
    document.addEventListener('ended', vqEnd, true);
    function vqAll() { for (var i = 0; i < vqList.length; i++) vqLook(vqList[i], true); }
    document.addEventListener('visibilitychange', function () { if (document.hidden) vqAll(); }, true);
    window.addEventListener('pagehide', vqAll, true);
  } catch (e) { /* never break the page */ }
})();

// RX4-M0e (research/REVIEW-EXTERNAL-4.md): how smooth is scrolling on REAL pages? Only while the Settings switch "Measure scrolling on real pages" is on (the app's answer to the `guard` question carries `sg`; the
// switch is OFF by default and nothing below runs without a true answer). A frame callback is requested only from a page's first scroll event until 200 ms after its last one; the frames, the gaps over 50 ms and over
// 100 ms and the longest gap of the burst go to the app as numbers. The callback makes the page's own thread tick every frame DURING the burst, so the numbers say how well the page keeps up, not how smooth the engine is.
(function () {
  try {
    if (window.top !== window || typeof window.requestAnimationFrame !== 'function') return;
    var state = 0, asked = false, lastScroll = 0, running = false, last = 0, n = 0, a = 0, b = 0, mx = 0;
    function ask() {
      if (asked) return;
      asked = true;
      try { browser.runtime.sendMessage({ k: 'guard', h: location.hostname }).then(function (r) { state = (r && r.sg === true) ? 2 : 1; }, function () { state = 1; }); } catch (e) { state = 1; }
    }
    function flush() {
      if (n >= 5) browser.runtime.sendMessage({ k: 'sg', n: Math.min(n, 5000), a: Math.min(a, 5000), b: Math.min(b, 5000), mx: Math.min(Math.round(mx), 10000) });
      n = a = b = mx = 0; last = 0;
    }
    function tick(t) {
      try {
        if (last) { var d = t - last; n++; if (d > 50) a++; if (d > 100) b++; if (d > mx) mx = d; }
        last = t;
        if (performance.now() - lastScroll > 200) { running = false; flush(); return; }   // 200 ms without a scroll event: the burst is over
        window.requestAnimationFrame(tick);
      } catch (e) { running = false; }
    }
    window.addEventListener('scroll', function () {
      lastScroll = performance.now();
      if (state === 0) { ask(); return; }                  // the first scroll only asks; the switch is OFF for almost everybody
      if (state !== 2 || running) return;
      running = true; last = 0; n = a = b = mx = 0;
      window.requestAnimationFrame(tick);
    }, { passive: true });
  } catch (e) { /* never break the page */ }
})();

// EY-1: where the page is scrolled, for the app's saved view of the page (top frame only). The engine's own scroll callbacks do not report the place a restored page scrolls itself to, the page does: a
// scroll event 300 ms after the last scroll, and once after the load. Passive and debounced: nothing happens per frame. The number is CSS pixels (pageYOffset); the app compares it only with itself.
(function () {
  try {
    if (window.top !== window) return;
    var timer = 0, last = -1;
    function report() {
      timer = 0;
      var y = Math.round(window.pageYOffset || (document.documentElement && document.documentElement.scrollTop) || 0);
      if (y === last) return;
      last = y;
      browser.runtime.sendMessage({ k: 'scroll', y: y, p: location.href }).catch(function () {});
    }
    window.addEventListener('scroll', function () { if (!timer) timer = setTimeout(report, 300); }, { passive: true });
    window.addEventListener('load', function () { if (!timer) timer = setTimeout(report, 800); }, false);
    // RX3-BK2: the page is being left (a link, Back, a typed address): its place goes to the app NOW, not 300 ms after the last scroll, so that the next Back knows where the owner left it.
    window.addEventListener('pagehide', function () { if (timer) { clearTimeout(timer); timer = 0; } report(); }, false);
  } catch (e) { /* never break the page */ }
})();

// RX3-BK1 / RX3-BK2 (research/REVIEW-EXTERNAL-3.md section 3; top frame only). Back and Forward: (1) tell the app what kind of show this was (the engine's cache, or loaded again, and what the page's file cost)
// so the real hit rate is measured (the Back lab); (2) when the engine loaded the page again, ask the app for the place the owner left it at and scroll there if the engine did not get there (pages that change
// height after the load, pages that restore their own place). Five looks in 3 s, each acts only when the page is more than 4 px off, none once the owner touched the page. One question per page load, a number back.
(function () {
  try {
    if (window.top !== window) return;
    var touched = false, asked = false, evs = ['touchstart', 'wheel', 'keydown', 'mousedown'], i;
    function drop() { for (var j = 0; j < evs.length; j++) window.removeEventListener(evs[j], touch, true); }
    function touch() { touched = true; drop(); }
    for (i = 0; i < evs.length; i++) window.addEventListener(evs[i], touch, { capture: true, passive: true });
    function place() { return Math.round(window.pageYOffset || (document.documentElement && document.documentElement.scrollTop) || 0); }
    function moveTo(y) {
      try { window.scrollTo({ top: y, left: 0, behavior: 'instant' }); } catch (e) { try { window.scrollTo(0, y); } catch (e2) { /* ignore */ } }
    }
    function pull() {
      if (asked) { drop(); return; }
      asked = true;
      browser.runtime.sendMessage({ k: 'bk', p: location.href }).then(function (r) {
        var y = parseInt(r, 10), at = [0, 250, 700, 1500, 3000], n = 0;
        if (!(y > 0) || touched) { drop(); return; }
        (function look() {
          if (touched) return;
          var room = (document.documentElement ? document.documentElement.scrollHeight : 0) - window.innerHeight;
          var lastLook = n === at.length - 1;
          if (Math.abs(place() - y) > 4 && (room >= y - 4 || lastLook)) moveTo(y);
          n++;
          if (n < at.length) setTimeout(look, at[n] - at[n - 1]); else drop();
        })();
      }, function () { drop(); });
    }
    window.addEventListener('pageshow', function (e) {
      try {
        var kept = !!(e && e.persisted), nt = '', ts = 0;
        try { var nav = performance.getEntriesByType('navigation')[0]; if (nav) { nt = String(nav.type || ''); ts = Math.round(nav.transferSize || 0); } } catch (x) { /* an older engine */ }
        if (!kept && nt !== 'back_forward') { drop(); return; } // a normal visit: nothing to report, nothing to ask
        browser.runtime.sendMessage({ k: 'bf', p: location.href, ps: kept ? 1 : 0, nt: nt, ts: kept ? 0 : ts, y: place() }).catch(function () {});
        if (kept) drop(); else pull();
      } catch (x) { /* never break the page */ }
    }, false);
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
    // ANS52g: the page-side video format policy (codec-main.js) is told the policy the app wants (the `cx` of the guard answer below: "" = none) and reports what it refused: numbers and one short word. Any page script
    // can dispatch the same event, so what comes in is checked, the word is cut to a few characters and a document gets only a handful of reports through.
    // ANS53a / ANS53b: the page-side gate (gate-main.js: no video loads or starts before the person presses it) is told whether the rule is on (`gt` of the guard answer; closed until then) and says what it held and
    // opened: seven numbers, checked here (any page script can dispatch the same event), a handful per document.
    var GCFG = '__kst_gate_cfg_v1', GREP = '__kst_gate_rep_v1', GREADY = '__kst_gate_ready_v1', gt = null, gReports = 0, ho = false, pp = false;   // pp (VP2): this frame was started by the person's tap on the page around it
    function pushGate() { if (gt !== null) { try { document.dispatchEvent(new CustomEvent(GCFG, { detail: gt ? ((ho ? '1+' : '1') + (pp ? 'p' : '')) : '0' })); } catch (e) { /* ignore */ } } }
    document.addEventListener(GREADY, pushGate, true);
    document.addEventListener(GREP, function (e) {
      try {
        if (gReports >= 8 || typeof e.detail !== 'string') return;
        var p = e.detail.slice(0, 80).split('|'), n = [], i;
        if (p.length !== 7) return;
        for (i = 0; i < 7; i++) { var v = parseInt(p[i], 10); if (!(v >= 0 && v <= 1000000)) return; n.push(v); }
        gReports++;
        browser.runtime.sendMessage({ k: 'vgt', n: n }).catch(function () {});
      } catch (x) { /* ignore */ }
    }, true);
    // ANS54e: the gate asks whether Kestrel's own player takes the film the person pressed. Any page script can dispatch the same event, so everything is checked here (the address must be http(s), numbers are
    // clamped, a document gets a few dozen questions) and only the answer of the app goes back. The app decides; a refusal or no answer means the page plays the film itself.
    var AEV = '__kst_gate_ask_v1', NEV = '__kst_gate_ans_v1', asked = 0, seenAt = 0;
    // The gate runs in the PAGE's world: any script of the page can dispatch its question. A genuine question follows a press, so it is relayed only while the engine itself says a person touched this document a moment ago
    // (navigator.userActivation: a script cannot set it) or while our own listeners saw a trusted tap, key or play of the engine's own controls in the last 12 s (the activation lasts 5 s; a slow page answers a tap later).
    function seen(e) { if (e && e.isTrusted) seenAt = Date.now(); }
    ['pointerup', 'click', 'keydown', 'touchend', 'play'].forEach(function (t) { try { window.addEventListener(t, seen, { capture: true, passive: true }); } catch (x) { /* ignore */ } });
    function touched() { try { var ua = navigator.userActivation; if (ua && typeof ua.isActive === 'boolean' && ua.isActive) return true; } catch (x) { /* an older engine */ } return Date.now() - seenAt < 12000; }
    document.addEventListener(AEV, function (e) {
      try {
        if (!ho || asked >= 60 || typeof e.detail !== 'string' || e.detail.length > 3000 || !touched()) return;
        var q = JSON.parse(e.detail), id = q.i | 0;
        if (!(id > 0 && id < 1000000)) return;
        function num(v, hi) { v = +v; return (v >= 0 && v <= hi) ? v : 0; }
        // ANS55f: the film's box (gate geomOf): eleven numbers, each inside its own range, or nothing (the film then plays in the full-screen player)
        function geo(a) {
          if (!Array.isArray(a) || a.length !== 11) return null;
          var lo = [-1e6, -1e6, 0, 0, 0.05, -1e6, -1e6, 1, -1e7, -1e7, 0], hi = [1e6, 1e6, 1e5, 1e5, 20, 1e6, 1e6, 1e5, 1e7, 1e7, 1], o = [], j, v;
          for (j = 0; j < 11; j++) { v = +a[j]; if (!(v >= lo[j] && v <= hi[j])) return null; o.push(Math.round(v * 100) / 100); }
          return o;
        }
        var src = (typeof q.s === 'string' && /^https?:\/\//i.test(q.s) && q.s.length <= 2000) ? q.s : '';
        var why = (q.y === 'play' || q.y === 'press' || q.y === 'kestrel') ? q.y : 'play';
        asked++;
        browser.runtime.sendMessage({ k: 'vhand', y: why, s: src, b: q.b === 1 ? 1 : 0, m: String(q.m || '').replace(/[^A-Za-z0-9\/.+;=, -]/g, '').slice(0, 60), l: q.l === 1 ? 1 : 0,
          w: Math.round(num(q.w, 20000)), h: Math.round(num(q.h, 20000)), t: num(q.t, 1e7), d: num(q.d, 1e7), g: geo(q.g), a: q.a === 1 ? 1 : 0 }).then(function (r) {
          try { document.dispatchEvent(new CustomEvent(NEV, { detail: id + '|' + ((r && r.h === 1) ? '1' : '0') })); } catch (x) { /* ignore */ }
        }, function () { try { document.dispatchEvent(new CustomEvent(NEV, { detail: id + '|0' })); } catch (x) { /* ignore */ } });
      } catch (x) { /* ignore */ }
    }, true);
    var CCFG = '__kst_codec_cfg_v1', CREP = '__kst_codec_rep_v1', CREADY = '__kst_codec_ready_v1', cx = null, cReports = 0;
    function pushCodec() { if (cx !== null) { try { document.dispatchEvent(new CustomEvent(CCFG, { detail: cx })); } catch (e) { /* ignore */ } } }
    document.addEventListener(CREADY, pushCodec, true);
    document.addEventListener(CREP, function (e) {
      try {
        if (cReports >= 10 || typeof e.detail !== 'string') return;
        var p = e.detail.slice(0, 120).split('|');
        if (p.length !== 5) return;
        var n = [], i;
        for (i = 0; i < 4; i++) { var v = parseInt(p[i], 10); if (!(v >= 0 && v <= 1000000000)) return; n.push(v); }
        cReports++;
        browser.runtime.sendMessage({ k: 'vcx', a: n[0], s: n[1], m: n[2], f: n[3], w: p[4].replace(/[^a-z0-9 x@.]/g, '').slice(0, 30) }).catch(function () {});
      } catch (x) { /* ignore */ }
    }, true);
    // What the page-side guard may REPORT through this event: it refused something. A `tap` (the evidence that lets a jump through) or a `vmenu` arriving through it is never believed: any script of the
    // page can dispatch the same event itself (RX3-G2). The owner's real taps are recorded below, here in the isolated world, from trusted events only.
    var KINDS = { open: 1, click: 1, overlay: 1, form: 1, redirect: 1 };
    document.addEventListener(POP, function (e) {
      try {
        var d = String(e.detail).slice(0, 700), c = d.indexOf('|'), kind = c > 0 ? d.slice(0, c) : d;
        if (!Object.prototype.hasOwnProperty.call(KINDS, kind)) return;
        browser.runtime.sendMessage({ k: 'pop', d: d, p: location.href }).catch(function () {});
      } catch (x) { /* ignore */ }
    }, true);
    var lastTapHref = null, lastTapAt = 0;
    function anchorHref(el) { try { for (var n = el; n && n !== document; n = n.parentNode) if (n.tagName === 'A' && n.href) return String(n.href); } catch (x) { /* ignore */ } return ''; }
    function realTap(e) {
      try {
        if (!e.isTrusted) return; // a script's own click or pointer event is no tap
        var href = anchorHref(e.target), now = Date.now();
        if (lastTapHref === href && now - lastTapAt < 700) return; // the pointerup of a tap and the click that follows it are one tap
        lastTapHref = href; lastTapAt = now;
        browser.runtime.sendMessage({ k: 'pop', d: 'tap|' + href.slice(0, 600), p: location.href }).catch(function () {});
      } catch (x) { /* ignore */ }
    }
    // A tap is the PRIMARY button, up within 500 ms (Android's long press, which opens the menu, starts there) and within 12 px of where it went down: a long press, a right click, a drag and a swipe are no
    // taps. The click of a mouse or a finger follows the same rule (a keyboard click has no press before it); a page that cancels the click still gets its tap counted through pointerup.
    var down = { t: 0, x: 0, y: 0 };
    window.addEventListener('pointerdown', function (e) { if (e.isTrusted) { down.t = Date.now(); down.x = e.clientX; down.y = e.clientY; } }, { capture: true, passive: true });
    window.addEventListener('click', function (e) {
      if (e.detail > 0 && down.t && Date.now() - down.t > 500) return;
      realTap(e);
    }, true);
    window.addEventListener('pointerup', function (e) {
      if (!e.isTrusted || e.button !== 0 || !down.t) return;
      if (Date.now() - down.t > 500 || Math.abs(e.clientX - down.x) > 12 || Math.abs(e.clientY - down.y) > 12) return;
      realTap(e);
    }, { capture: true, passive: true });

    // RX3-G3: the assist of a player inside a FRAME (the guard's one-press-plays help). The guard in the page says "go" with no parameters; THIS script decides, from the owner's own trusted press (recorded
    // above): the press must be fresh (2 s), the frame and the place are worked out from THAT press, never from the event, and at most five per press. The background page relays to the frame
    // (tabs.sendMessage: a channel a web page cannot reach) and the frame's own copy of this script plays its video or presses through (below). Same-frame help stays in the guard.
    var ASSIST = '__kst_assist_v1', assistN = 0, assistFor = 0;
    document.addEventListener(ASSIST, function () {
      try {
        if (!wanted || !down.t || Date.now() - down.t > 2000) return;
        if (assistFor !== down.t) { assistFor = down.t; assistN = 0; }
        if (assistN >= 5) return;
        var list = document.elementsFromPoint ? document.elementsFromPoint(down.x, down.y) : [], f = null, i;
        for (i = 0; i < list.length && i < 14 && !f; i++) if (list[i].tagName === 'IFRAME') { var r = list[i].getBoundingClientRect(); if (r.width >= 120 && r.height >= 80) f = list[i]; }
        if (!f) return;
        assistN++;
        var rr = f.getBoundingClientRect();
        browser.runtime.sendMessage({ k: 'assist', x: Math.round(down.x - rr.left), y: Math.round(down.y - rr.top), s: String(f.src || '').slice(0, 600) }).catch(function () {});
      } catch (x) { /* ignore */ }
    }, true);

    // the frame's own part: the extension asks (only the background page can) to play the frame's video, or to press at a place when the player has no video yet. Never on a link or a form control.
    function playingNow() {
      try { var l = document.getElementsByTagName('video'); for (var i = 0; i < l.length; i++) { var v = l[i]; if (!v.paused && !v.ended && v.readyState > 2 && v.currentTime > 0) return true; } } catch (e) { /* ignore */ }
      return false;
    }
    function hasSource(v) { try { return !!(v.currentSrc || v.getAttribute('src') || v.srcObject || (v.querySelector && v.querySelector('source[src]'))); } catch (e) { return false; } }
    function bestVideo() {
      var best = null, area = 0;
      try {
        var l = document.getElementsByTagName('video');
        for (var i = 0; i < l.length; i++) {
          var r = l[i].getBoundingClientRect();
          var w = Math.min(r.right, innerWidth) - Math.max(r.left, 0), h = Math.min(r.bottom, innerHeight) - Math.max(r.top, 0);
          if (w >= 120 && h >= 80 && w * h > area) { area = w * h; best = l[i]; }
        }
      } catch (e) { /* ignore */ }
      return best;
    }
    function startMedia(v) {
      try {
        if (window.KestrelVF && window.KestrelVF.gate) window.KestrelVF.gate(v);       // ANS53a/b: the person's press: the page-side gate lets this film load
        var p = v.play();
        // a frame that was not pressed itself may be refused sound: start it muted rather than not at all
        if (p && p.catch) p.catch(function () { try { v.muted = true; var q = v.play(); if (q && q.catch) q.catch(function () { /* ignore */ }); } catch (e) { /* ignore */ } });
      } catch (e) { /* ignore */ }
    }
    function pressThrough(x, y) {
      try {
        var el = document.elementFromPoint(x, y);
        if (!el || el === document.body || el === document.documentElement) return;
        for (var n = el; n && n !== document; n = n.parentNode) { if (/^(A|BUTTON|INPUT|SELECT|TEXTAREA|LABEL|OPTION|FORM|SUMMARY)$/.test(n.tagName) || n.isContentEditable) return; }
        var init = { bubbles: true, cancelable: true, composed: true, clientX: x, clientY: y, button: 0, buttons: 1 };
        var seq = ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'];
        for (var i = 0; i < seq.length; i++) {
          var type = seq[i];
          el.dispatchEvent((type.indexOf('pointer') === 0 && window.PointerEvent) ? new PointerEvent(type, init) : new MouseEvent(type, init));
        }
      } catch (e) { /* ignore */ }
    }
    browser.runtime.onMessage.addListener(function (m) {
      try {
        if (!m || m.k !== 'assist' || window.top === window || !wanted) return undefined;
        var x = Math.round(m.x), y = Math.round(m.y);
        if (!(x >= 0 && y >= 0 && x < 10000 && y < 10000)) return undefined; // not a number: the comparison fails
        if (playingNow()) return undefined;
        var v = bestVideo();
        if (v && !hasSource(v)) v = null; // a player that only builds its video after the first press: that press has to go through instead
        if (v) { if (v.paused && !v.ended) startMedia(v); return undefined; }
        pressThrough(x, y);
      } catch (e) { /* ignore */ }
      return undefined;
    });
    browser.runtime.sendMessage({ k: 'guard', h: location.hostname }).then(function (r) { wanted = !(r && r.on === false); topHost = (r && r.top) ? String(r.top) : ''; push(); cx = (r && typeof r.cx === 'string') ? r.cx.slice(0, 160) : ''; pushCodec(); gt = !(r && r.gt === false); ho = !!(r && r.ho === true); pp = !!(r && r.pp === true); pushGate(); if (r && r.lab === true && window.KestrelVF) window.KestrelVF.probe(); }, function () {});
  } catch (e) { /* never break the page */ }
})();

// VP2: a trusted click in the TOP page is told to the background page (nothing but the fact that it happened): a player frame the page starts within a few seconds of it is the person's press of that player.
(function () {
  try {
    if (window !== window.top) return;
    var last = 0;
    function clicked(e) {
      try {
        if (!e || !e.isTrusted) return;
        if (e.type === 'keydown' && e.key !== 'Enter' && e.key !== ' ') return;
        var now = Date.now(); if (now - last < 250) return; last = now;
        browser.runtime.sendMessage({ k: 'ptap' }).catch(function () {});
      } catch (x) { /* ignore */ }
    }
    window.addEventListener('click', clicked, { capture: true, passive: true });
    window.addEventListener('keydown', clicked, { capture: true, passive: true });
  } catch (e) { /* never break the page */ }
})();

// Media addresses that sit in the page itself (owner request X7: "I need to press play ... I want to download the video cleanly"): <video> / <source> elements, og:video tags, links to media files and the
// player configuration inside inline scripts and JSON. Found without playing anything, so the download list exists before the page's play button (and the spam behind it) is touched.
(function () {
  try {
    var EXT = /\.(m3u8|mpd|mp4|webm|mkv|m4v|mov|mp3|m4a|aac|ogg|opus|flac)(?:$|[?#])/i;
    // RVW1 F9: the extension must END the file name: `.../a.mp4/index.m3u8` (Kaltura, Wowza) and `vid.mp4.m3u8` were cut at the FIRST `.mp4` and the app was told about a path that is not a video. The lookahead
    // refuses a name that goes on (a letter, a digit, `_`, `/`, `%`, `-` or `.` plus a letter), so the lazy match grows to the next extension instead; a full stop after it (end of a sentence) is still the end.
    var FIND = /https?:\/\/[^\s"'<>()\\]+?\.(?:m3u8|mpd|mp4|webm|mkv|m4v|mov|mp3|m4a|aac|ogg|opus|flac)(?![A-Za-z0-9_\/%-]|\.[A-Za-z0-9])(?:\?[^\s"'<>()\\]*)?/gi;
    // RVW1 F9: a player configuration's relative or scheme-relative address (JW Player `file:"//cdn.example/x.mp4"`, `src: "/media/b.m3u8"`): only after a key that says "this is the media", so a stylesheet or a picture path is never run
    var FINDREL = /(?:file|src|source|url|hls|dash|mp4|stream|video|contentUrl|playlist|manifest)["']?\s*[:=]\s*["'](\/[^"'\s\\<>]+?\.(?:m3u8|mpd|mp4|webm|mkv|m4v|mov|mp3|m4a|aac|ogg|opus|flac)(?![A-Za-z0-9_\/%-]|\.[A-Za-z0-9])(?:\?[^"'\s\\<>]*)?)/gi;
    // RVW1 F9: the same address percent-encoded inside another address (`player.php?url=https%3A%2F%2Fcdn.example%2Fa.m3u8`): decoded on its own, the text around it is never decoded
    var FINDENC = /https?%3A%2F%2F[^\s"'<>()\\&]+?\.(?:m3u8|mpd|mp4|webm|mkv|m4v|mov|mp3|m4a|aac|ogg|opus|flac)(?![A-Za-z0-9_-]|\.[A-Za-z0-9]|%2F)(?:%3F[^\s"'<>()\\&]*)?/gi;
    var HINT = /\.(?:m3u8|mpd|mp4|webm|mkv|m4v|mov|mp3|m4a|aac|ogg|opus|flac)/i; // a script that names no media file at all is not run through the three replaces and FIND below
    var sent = {}, n = 0, scriptsDone = 0, linksDone = 0; // the scan at load only looks at what appeared since the first one
    // RVW1 F12: attributes that hold a source or a player configuration (lazy-loaded players, video.js data-setup, Reddit-style custom elements): an address, or JSON that names the files
    var SRC_ATTRS = ['data-src', 'data-video-src', 'data-hls', 'data-hls-url', 'data-mp4', 'data-stream', 'data-video-url', 'data-file', 'src', 'data'];
    var CFG_ATTRS = ['data-setup', 'data-config', 'data-sources', 'data-player-config'];
    var ATTR_SELECTOR = '[data-src]:not(img):not(picture), [data-video-src], [data-hls], [data-hls-url], [data-mp4], [data-stream], [data-video-url], [data-file], embed[src], object[data], [data-setup], [data-config], [data-sources], [data-player-config]';
    // RVW1 F11: the quality a <source> says it is ("720p", "HD", "1080"): video.js and plain markup put it in label / title / res / size. Sent with the address so a list of several qualities is not nameless (and the best
    // one is checked first, not the last one the page happened to list).
    // RVW2 F13: a FRAME that offered something says so when it is unloaded (pagehide): the player iframe REMOVED by a server switch never gets a request of its own that would tell the extension
    function offered() { try { if (window.top !== window && window.KestrelVF) window.KestrelVF.offered = true; } catch (e) { /* ignore */ } }
    window.addEventListener('pagehide', function () {
      try { if (window.top !== window && window.KestrelVF && window.KestrelVF.offered) browser.runtime.sendMessage({ k: 'bye' }).catch(function () {}); } catch (e) { /* never break the page */ }
    });
    function labelOfSource(e) {
      try {
        if (!e || e.tagName !== 'SOURCE') return '';
        var v = e.getAttribute('label') || e.getAttribute('title') || e.getAttribute('res') || e.getAttribute('data-res') || e.getAttribute('data-quality') || e.getAttribute('size') || '';
        v = String(v).replace(/\s+/g, ' ').trim();
        return (v.length > 0 && v.length <= 24) ? v : '';
      } catch (x) { return ''; }
    }
    function report(u, label) {
      if (!u || n >= 40) return;
      try { u = new URL(u.replace(/&amp;/g, '&'), location.href).href; } catch (e) { return; }
      if (u.indexOf('http') !== 0 || sent[u] || !EXT.test(u)) return;
      sent[u] = 1; n++;
      var msg = { k: 'dom', u: u, p: location.href };
      offered();
      if (label) msg.lb = label;
      browser.runtime.sendMessage(msg).catch(function () {});
    }
    // the text of a script, a JSON blob or a configuration attribute: every media address it names (escaped slashes undone first)
    function scanText(t) {
      if (!HINT.test(t)) return;
      t = t.replace(/\\u002f/gi, '/').replace(/\\u0026/gi, '&').replace(/\\\//g, '/');
      var m; FIND.lastIndex = 0;
      while ((m = FIND.exec(t)) !== null && n < 40) report(m[0]);
      FINDREL.lastIndex = 0;
      while ((m = FINDREL.exec(t)) !== null && n < 40) report(m[1]);
      if (t.indexOf('%3A%2F%2F') >= 0 || t.indexOf('%3a%2f%2f') >= 0) {
        FINDENC.lastIndex = 0;
        while ((m = FINDENC.exec(t)) !== null && n < 40) { try { report(decodeURIComponent(m[0])); } catch (x) { /* a broken escape: not an address */ } }
      }
    }
    function scan() {
      try {
        // RVW1 F11: the <source> elements FIRST, each with its label: the video element's own currentSrc is usually one of them, and whoever reports an address first wins (the label was lost to the label-less report)
        var i, e, list = document.querySelectorAll('source[src]');
        for (i = 0; i < list.length; i++) { e = list[i]; report(e.src || e.getAttribute('src'), labelOfSource(e)); }
        list = document.querySelectorAll('video, audio');
        for (i = 0; i < list.length; i++) { e = list[i]; report(e.currentSrc || e.src || e.getAttribute('src')); }
        var holders = document.querySelectorAll(ATTR_SELECTOR), j, v;
        for (i = 0; i < holders.length && i < 200 && n < 40; i++) {
          e = holders[i];
          for (j = 0; j < SRC_ATTRS.length; j++) { v = e.getAttribute(SRC_ATTRS[j]); if (v && v.length < 2000) report(v); }
          for (j = 0; j < CFG_ATTRS.length; j++) { v = e.getAttribute(CFG_ATTRS[j]); if (v && v.length < 20000) scanText(v); }
        }
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
          scanText(t);
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
        if (window.KestrelVF) window.KestrelVF.pressedAt = Date.now();                              // RW5: the release of this press is no tap (videobar.js)
        if (window.KestrelVB && window.KestrelVB.isBar && window.KestrelVB.isBar(e.target)) return; // Kestrel's own video bar (videobar.js; RW7: known by identity, a page cannot mark its own layer as "the bar")
        var els = document.elementsFromPoint ? document.elementsFromPoint(e.clientX, e.clientY) : [e.target];
        var media = null;
        for (var i = 0; i < els.length && i < 16; i++) {
          var n = els[i].tagName;
          if (n === 'VIDEO' || n === 'AUDIO') { media = els[i]; break; }
        }
        // LP1 (owner message 44): a video inside a shadow root (finance.yahoo.com's player is one: the point test returns only the shadow HOST) or under a layer with pointer-events: none is not in that list
        if (!media && window.KestrelVF) media = window.KestrelVF.at(e.clientX, e.clientY);
        if (!media) return;
        var src = media.currentSrc || media.src || '';
        if (!src && !(media.querySelector && media.querySelector('source'))) return;
        e.preventDefault(); e.stopImmediatePropagation();
        browser.runtime.sendMessage({ k: 'pop', d: 'vmenu|' + (src.indexOf('http') === 0 ? src : ''), p: location.href }).catch(function () {});
      } catch (x) { /* never break the page */ }
    }, true);
  } catch (e) { /* never break the page */ }
})();

// Long press on OPEN space (AY2, owner message 39: "make it so I can long press anywhere in the browser and it will show up under where I long pressed, but only if I am long pressing an open space. If I'm
// long pressing a link, give me the options for that link, not the navigation panel"). The engine's own long press reaches the page as a trusted `contextmenu` event on whatever is under the finger (blank
// space too: found on the emulator, research/MINI_NAV.md section AY2). It is told to the app only when nothing there could have an action of its own: no link, picture, video, form control, editable text,
// widget role or pointer cursor in the stack of elements under the finger, and the engine did NOT select a word 200 ms later (the selection is new and not empty: the press was on text, the selection belongs to the person; a selection that was there before and is gone now was cleared by the press itself). The app puts the
// panel where the finger is; nothing about the page is sent, only that the press was on open space.
(function () {
  try {
    var TAGS = { IMG: 1, PICTURE: 1, VIDEO: 1, AUDIO: 1, CANVAS: 1, OBJECT: 1, EMBED: 1, IFRAME: 1, INPUT: 1, TEXTAREA: 1, SELECT: 1, OPTION: 1, BUTTON: 1, LABEL: 1, SUMMARY: 1, DETAILS: 0 };
    var ROLES = /^(link|button|checkbox|radio|menuitem|menuitemcheckbox|menuitemradio|tab|switch|slider|spinbutton|textbox|searchbox|combobox|listbox|option|treeitem|gridcell)$/i;
    // a wrapper that fills the whole screen (an app root with a tab stop, a page with an onclick on its body) is not a control: it must not make the whole page "not open space"
    function huge(el) { try { var r = el.getBoundingClientRect(); return r.width >= window.innerWidth * 0.95 && r.height >= window.innerHeight * 0.9; } catch (x) { return false; } }
    function interactive(el) {
      var t = el.tagName ? String(el.tagName).toUpperCase() : '';
      if (!t) return false;
      if (t === 'A' || t === 'AREA') { if (el.hasAttribute('href')) return true; }
      else if (TAGS[t] === 1) return true;
      if (el.isContentEditable) return true;
      var r = el.getAttribute ? el.getAttribute('role') : null;
      if (r && ROLES.test(r)) return true;
      if (el.hasAttribute && (el.hasAttribute('onclick') || (el.hasAttribute('tabindex') && el.tabIndex >= 0)) && !huge(el)) return true;
      try { if (window.getComputedStyle(el).cursor === 'pointer') return true; } catch (x) { /* a node without style */ }
      return false;
    }
    window.addEventListener('contextmenu', function (e) {
      try {
        if (!e.isTrusted) return;
        if (window.KestrelVB && window.KestrelVB.isBar && window.KestrelVB.isBar(e.target)) return; // Kestrel's own video bar (videobar.js, by identity: RW7)
        var els = document.elementsFromPoint ? document.elementsFromPoint(e.clientX, e.clientY) : [e.target];
        for (var i = 0; i < els.length && i < 10; i++) if (interactive(els[i])) return;
        if (window.KestrelVF && window.KestrelVF.at(e.clientX, e.clientY)) return; // LP1: a video here (also in a shadow root, also under a layer that takes no pointer events) has its own menu: never the panel
        var before = window.getSelection ? String(window.getSelection()) : ''; // the engine selects the word AFTER this event, and clears an older selection when the press is elsewhere (found on the emulator): neither may hide a press on blank space
        if (before && window.getSelection().rangeCount) { // ... but a press ON the text that is selected belongs to that selection (the engine would select the same word again and nothing would change)
          var rs = window.getSelection().getRangeAt(0).getClientRects();
          for (var j = 0; j < rs.length && j < 60; j++) { var q = rs[j]; if (e.clientX >= q.left - 4 && e.clientX <= q.right + 4 && e.clientY >= q.top - 4 && e.clientY <= q.bottom + 4) return; }
        }
        setTimeout(function () {
          try {
            var sel = window.getSelection ? String(window.getSelection()) : '';
            if (sel.length > 0 && sel !== before) return; // the engine selected a word: the press was on text (a selection it CLEARED, as it does on a press elsewhere, is not that)
            if (window.top === window) { browser.runtime.sendMessage({ k: 'os', p: location.href }).catch(function () {}); return; }
            // a press inside a FRAME: an ad overlay (Google IMA) lies over a player of the page above and takes every touch. The top frame is asked whether a frame of this size covers a video; only if not is it open space.
            // RW4: exactly ONE of the two goes to the app: the panel (told after 700 ms of silence, or when the answer is "no video") or the video menu (the answer carries the film's address and THIS frame sends it, only
            // while the panel has not been told). A late answer after the panel was told changes nothing, so the panel and the sheet never open together.
            var told = false;
            var openHere = function () { if (told) return; told = true; browser.runtime.sendMessage({ k: 'os', p: location.href }).catch(function () {}); };
            var lateTimer = setTimeout(openHere, 700);
            browser.runtime.sendMessage({ k: 'ft', t: 'lp', w: window.innerWidth, h: window.innerHeight }).then(function (r) {
              clearTimeout(lateTimer);
              if (r && r.video === true) {
                if (told) return;
                told = true;
                var src = (typeof r.src === 'string' && r.src.indexOf('http') === 0) ? r.src.slice(0, 2000) : '';
                browser.runtime.sendMessage({ k: 'pop', d: 'vmenu|' + src, p: location.href }).catch(function () {});
              } else openHere();
            }, function () { clearTimeout(lateTimer); openHere(); });
          } catch (x) { /* never break the page */ }
        }, 200);
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


// EX-15 (research/REVIEW-EXTERNAL-1.md 4.2): a head start for the NEXT page. Gecko closes its idle connections when the app goes to the background and opens a connection only when a link is tapped, so a
// tap pays DNS + TCP + TLS (2-3 round trips, a second on the hotspot) before the first byte. Two triggers tell the app which origin the owner is likely to open next; the app opens the connection through the
// engine's own speculative-connect (principal = the target, so the navigation can reuse it). Never an injected <link rel=preconnect> (it would be partitioned under THIS page).
//  1. finger down on a cross-site https link (the touch lasts about 100 ms before the tap fires),
//  2. on a search engine's result page, once at idle: the first three results.
// Limits here: top frame only, https only, another origin than the page, once per origin, 4 touches and 3 results per page. The app adds: the Warm-up switch, never a private tab, Data saver, once per
// origin per 30 s. A page cannot cause any of this (only trusted touches count) and learns nothing from it.
(function () {
  try {
    if (window.top !== window || !window.URL) return;
    var sent = {}, touches = 0, results = 0;
    function originOf(href) {
      try {
        var u = new URL(href, location.href);
        if (u.protocol !== 'https:' || u.username || u.password || u.origin === location.origin || !u.hostname) return '';
        return u.origin + '/';
      } catch (e) { return ''; }
    }
    function warm(o, kind) {
      if (!o || sent[o]) return false;
      sent[o] = 1;
      browser.runtime.sendMessage({ k: 'warm', o: o, p: location.href, w: kind }).catch(function () {});
      return true;
    }
    window.addEventListener('touchstart', function (e) {
      try {
        if (!e.isTrusted || touches >= 4) return;
        var t = e.target, hops = 0;
        while (t && hops++ < 8 && !(t.nodeType === 1 && t.tagName === 'A')) t = t.parentNode;
        if (!t || t.nodeType !== 1) return;
        var h = t.getAttribute('href');
        if (!h || h.charAt(0) === '#') return;
        if (warm(originOf(h), 'touch')) touches++;
      } catch (x) { /* never break the page */ }
    }, { capture: true, passive: true });

    // 2. search results. The page must look like a result page of a search engine (host + a query), the links are the ones a person would tap: an https link with a heading inside it (or around it),
    // visible, to a site that is not the engine itself or one of its ad redirectors. The first three different origins in page order.
    var ENGINE = /^(?:[a-z0-9-]+\.)*(?:google\.[a-z.]+|bing\.com|duckduckgo\.com|search\.brave\.com|startpage\.com|ecosia\.org|search\.yahoo\.com|yandex\.[a-z.]+|qwant\.com|kagi\.com)$/;
    var OWN = /(?:^|\.)(?:google|gstatic|googleadservices|googlesyndication|googleusercontent|doubleclick|bing|msn|microsoft|duckduckgo|yahoo|yimg|brave|startpage|ecosia|yandex|qwant|kagi)\.[a-z.]+$/;
    function isResultPage() {
      return ENGINE.test(location.hostname) && /[?&](?:q|p|query|text)=/.test(location.search + (location.hash || '')) && location.pathname.indexOf('/maps') !== 0;
    }
    function scanResults() {
      try {
        if (results >= 3 || !isResultPage()) return;
        var list = document.querySelectorAll('a[href^="https:"]');
        for (var i = 0; i < list.length && i < 400 && results < 3; i++) {
          var a = list[i];
          var h = a.getAttribute('href');
          var o = originOf(h);
          if (!o) continue;
          var host = o.slice(8, -1).replace(/:\d+$/, '');
          if (OWN.test(host)) continue;
          var head = a.querySelector('h1,h2,h3,[role="heading"]') || (a.closest ? a.closest('h1,h2,h3,[role="heading"]') : null);
          if (!head) continue;
          var r = a.getBoundingClientRect();
          if (r.width < 40 || r.height < 12) continue;
          if (warm(o, 'result')) results++;
        }
      } catch (x) { /* never break the page */ }
    }
    function later(ms) { setTimeout(function () { if (window.requestIdleCallback) requestIdleCallback(scanResults, { timeout: 2000 }); else scanResults(); }, ms); }
    if (isResultPage()) {
      if (document.readyState === 'complete') later(300); else window.addEventListener('load', function () { later(300); });
      later(2500); // results drawn by script after the load event
    }
  } catch (e) { /* never break the page */ }
})();

// ---------------------------------------------------------------- page problems for Diagnostics > Page report
// A script that throws, or a <script>, <link> or <img> that could not be loaded or used. The background keeps one line for each (at most 8 per frame here, 12 per page there) and hands them to the app
// once the page has settled; nothing leaves the tablet, and a private tab's lines are never kept. A failed load fires `error` on the ELEMENT (it does not bubble: the capture phase sees it).
(function () {
  var sent = 0;
  function tell(o) {
    if (sent >= 8) return;
    sent++;
    try { browser.runtime.sendMessage(o).catch(function () {}); } catch (x) { /* never break the page */ }
  }
  try {
    window.addEventListener('error', function (e) {
      var t = e.target;
      if (t && t !== window && t.nodeType === 1) {
        var u = t.src || t.href || '';
        if (u && /^https?:/.test(u) && !t.hasAttribute('data-kestrel')) tell({ k: 'jserr', t: 'load', m: String(t.tagName || '').toLowerCase(), s: u.slice(0, 200) });
        return;
      }
      tell({ k: 'jserr', t: 'js', m: String(e.message || '').slice(0, 140), s: String(e.filename || '').slice(0, 200), l: e.lineno | 0 });
    }, true);
  } catch (e) { /* never break the page */ }
})();
