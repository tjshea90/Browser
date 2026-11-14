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
      function note(t) {
        if (t.length > 81 || seen[t]) return;
        seen[t] = 1; fresh.push(t);
      }
      function addEl(el) {
        var id = el.id;
        if (id && typeof id === 'string') note('#' + id.toLowerCase());
        var cls = el.getAttribute ? el.getAttribute('class') : null; // not className: on an SVG element that is an object
        if (cls) {
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
      function first() { scan(document); later(); }
      if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', first); else first();
      window.addEventListener('load', first);
      if (window.MutationObserver) {
        var mo = new MutationObserver(function (recs) {
          if (off) { mo.disconnect(); return; }
          for (var r = 0; r < recs.length && r < 200; r++) {
            var added = recs[r].addedNodes;
            for (var k = 0; k < added.length && k < 50; k++) {
              var n = added[k];
              if (n.nodeType !== 1) continue;
              addEl(n);
              if (n.firstElementChild) { var sub = n.querySelectorAll('[id],[class]'); for (var m = 0; m < sub.length && m < 300; m++) addEl(sub[m]); }
            }
          }
          if (fresh.length) later();
        });
        mo.observe(document, { childList: true, subtree: true }); // the document itself: its root element may not exist yet
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
    window.addEventListener('click', realTap, true);
    window.addEventListener('pointerup', realTap, { capture: true, passive: true }); // a page that cancels the click still gets its tap counted; a swipe that scrolls ends in pointercancel, never here
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
        var els = document.elementsFromPoint ? document.elementsFromPoint(e.clientX, e.clientY) : [e.target];
        for (var i = 0; i < els.length && i < 10; i++) if (interactive(els[i])) return;
        var before = window.getSelection ? String(window.getSelection()) : ''; // the engine selects the word AFTER this event, and clears an older selection when the press is elsewhere (found on the emulator): neither may hide a press on blank space
        if (before && window.getSelection().rangeCount) { // ... but a press ON the text that is selected belongs to that selection (the engine would select the same word again and nothing would change)
          var rs = window.getSelection().getRangeAt(0).getClientRects();
          for (var j = 0; j < rs.length && j < 60; j++) { var q = rs[j]; if (e.clientX >= q.left - 4 && e.clientX <= q.right + 4 && e.clientY >= q.top - 4 && e.clientY <= q.bottom + 4) return; }
        }
        setTimeout(function () {
          try {
            var sel = window.getSelection ? String(window.getSelection()) : '';
            if (sel.length > 0 && sel !== before) return; // the engine selected a word: the press was on text (a selection it CLEARED, as it does on a press elsewhere, is not that)
            browser.runtime.sendMessage({ k: 'os', p: location.href }).catch(function () {});
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
