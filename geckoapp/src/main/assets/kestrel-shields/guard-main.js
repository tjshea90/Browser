'use strict';
// Kestrel's pop-up and forced-redirect guard. Runs INSIDE the page (world MAIN, every frame, document_start) because only there can window.open be replaced before the page's own scripts use it.
// Movie and streaming sites open ad tabs from the "play" click: a transparent layer or an ad script listens for the tap and calls window.open (or clicks a hidden <a target=_blank>), and the browser's own
// pop-up blocker lets it through because a real tap just happened. The rule here is stricter and simple: a new window is allowed only when
//   * the user really tapped (isTrusted) within the last 1.5 seconds, AND
//   * it is the very link that was tapped, or a page of the same site, or a known sign-in / payment window.
// Everything else (timers, page load, clicks on the video layer, hidden links) is refused; the page is handed a harmless stand-in window so scripts that check the result do not fall back to
// redirecting the whole page. Overlay links (invisible, nearly full screen) never open anything on tap. The isolated content script (content.js) relays what was blocked to the app and tells this
// script whether the guard applies to this site (Shields off for the site, or the setting off in Settings, turns it off).
// Tested in headless Chromium by web-tests/guard.test.js (the logic is plain DOM, the same in Gecko).
(function () {
  var CFG_EVENT = '__kst_cfg_v1', POP_EVENT = '__kst_pop_v1', READY_EVENT = '__kst_ready_v1';
  try {
    if (typeof browser !== 'undefined' && browser && browser.runtime) return; // RX3-G1: declared in the manifest as well; a copy that ever ran in the isolated world (extension API present) must do nothing there
    if (window.__kstGuard) return;
    Object.defineProperty(window, '__kstGuard', { value: 1 });
  } catch (e) { return; }

  var on = true;                 // the guard applies until the content script says it does not
  var topHost = '';              // the site of the TOP page (told by the content script): inside an embedded player "the same site" means the same site as THAT page, not merely as the player's own host
  var TAP_MS = 1500;             // how long after a real tap a window may open
  var tap = { t: 0, href: '' };
  var realOpen = window.open;

  // sign-in and payment windows the user asked for with a tap: allowed from any site
  var AUTH = /(^|\.)(accounts\.google\.com|appleid\.apple\.com|login\.microsoftonline\.com|login\.live\.com|www\.facebook\.com|m\.facebook\.com|github\.com|api\.twitter\.com|twitter\.com|x\.com|discord\.com|www\.paypal\.com|paypal\.com|checkout\.stripe\.com|pay\.google\.com|accounts\.spotify\.com|id\.twitch\.tv|www\.linkedin\.com)$/;

  function note(kind, url) {
    try { document.dispatchEvent(new CustomEvent(POP_EVENT, { detail: kind + '|' + (url || '') })); } catch (e) { /* ignore */ }
    if (kind !== 'tap') { refusedAt = Date.now(); scheduleAssist(); }
  }

  function hostOf(u) { try { return new URL(u, location.href).hostname.toLowerCase(); } catch (e) { return ''; } }
  function absolute(u) { try { return new URL(u, location.href).href; } catch (e) { return ''; } }
  // registrable domain without a Public Suffix List: the last two labels, or three under a two-letter country code with a generic second level (example.co.uk)
  function registrable(h) {
    var p = (h || '').toLowerCase().split('.');
    if (p.length <= 2) return p.join('.');
    var cc = p[p.length - 1].length === 2, generic = /^(co|com|org|net|gov|edu|ac|ne|or|go)$/.test(p[p.length - 2]);
    return p.slice(-(cc && generic ? 3 : 2)).join('.');
  }
  function sameSite(a, b) { return !!a && !!b && registrable(a) === registrable(b); }
  // The page's own address for "same site": inside an iframe that is the iframe's host, so an embedded player may open ITS OWN pages only; the embedding page is checked the same way.
  function pageHost() { return topHost || location.hostname.toLowerCase(); }

  function anchorOf(el) {
    try { for (var n = el; n && n !== document; n = n.parentNode) if (n.tagName === 'A' && n.href) return n; } catch (e) { /* ignore */ }
    return null;
  }
  function opensWindow(a) {
    var t = (a.target || '').toLowerCase();
    return t !== '' && t !== '_self' && t !== '_top' && t !== '_parent';
  }
  function fresh() { return Date.now() - tap.t < TAP_MS; }

  // A link laid over the page (or the video) to catch the tap meant for "play": invisible, empty, or - the spoof play button - a visible image or icon of another site sitting on the player.
  function crossSiteLink(a) { try { return !sameSite(hostOf(a.href), pageHost()); } catch (e) { return false; } }
  function overPlayer(r) {
    try {
      var list = document.querySelectorAll('video, iframe, canvas, [class*="player" i], [id*="player" i]');
      for (var i = 0; i < list.length && i < 12; i++) {
        var m = list[i].getBoundingClientRect();
        if (m.width < 120 || m.height < 80) continue;
        var cx = r.left + r.width / 2, cy = r.top + r.height / 2;
        if (cx >= m.left && cx <= m.right && cy >= m.top && cy <= m.bottom) return true;
      }
    } catch (e) { /* ignore */ }
    return false;
  }
  function isOverlay(a) {
    try {
      var r = a.getBoundingClientRect();
      var cs = getComputedStyle(a);
      var big = r.width * r.height >= 0.35 * innerWidth * innerHeight;
      if (big) {
        if (parseFloat(cs.opacity) < 0.05 || cs.visibility === 'hidden') return true;
        var words = !!(a.textContent || '').trim();
        var empty = !words && !a.querySelector('img,svg,video,canvas,picture,button,input');
        // an empty positioned link is a layer over the page only when it is for ANOTHER site or lies over a player: the empty stretched link of a card of the site's own ("the whole card is the link") is not
        if (empty && (cs.position === 'fixed' || cs.position === 'absolute') && (crossSiteLink(a) || overPlayer(r))) return true;
        if (crossSiteLink(a) && !words) return true;     // a link to another site that fills the screen and has no words of its own (a picture) is an advertisement layer; a card with a title is a link
      }
      // the spoof play button: a link to ANOTHER site with no words of its own (an image, an icon) whose centre sits on a player
      if (crossSiteLink(a) && !(a.textContent || '').trim() && overPlayer(r)) return true;
      return false;
    } catch (e) { return false; }
  }

  var inAssist = false;          // true while the guard repeats a press itself (AP1): nothing opened by a repeated press is the owner's doing
  function allowOpen(u) {
    if (inAssist) return false;
    if (!fresh()) return false;                         // no real tap just now: timers, page load, background code
    var abs = absolute(u);
    if (!abs || abs.indexOf('http') !== 0) return false; // about:blank pop-unders, javascript:, data:
    if (tap.href && absolute(tap.href) === abs) return true;   // the very link that was tapped
    var h = hostOf(abs);
    if (sameSite(h, pageHost())) return true;           // part of the same site
    if (AUTH.test(h)) return true;                      // sign-in / payment
    return false;
  }

  // What a blocked window.open hands back: enough of a window that scripts carry on without a fallback redirect.
  function standIn() {
    var w = {
      closed: false, name: '', length: 0, opener: window,
      focus: function () {}, blur: function () {}, stop: function () {}, print: function () {}, postMessage: function () {}, close: function () { w.closed = true; },
      location: { href: 'about:blank', assign: function () {}, replace: function () {}, reload: function () {}, toString: function () { return 'about:blank'; } },
      document: { write: function () {}, writeln: function () {}, open: function () {}, close: function () {}, body: {}, head: {}, createElement: function () { return {}; } },
    };
    w.window = w; w.self = w; w.top = w; w.parent = w;
    return w;
  }

  function guardedOpen(url) {
    var u = '';
    try { u = (url === undefined || url === null) ? '' : String(url); } catch (e) { /* ignore */ }
    if (!on || allowOpen(u)) return realOpen.apply(window, arguments);
    note('open', absolute(u) || u);
    return standIn();
  }
  try { Object.defineProperty(window, 'open', { value: guardedOpen, writable: true, configurable: true }); } catch (e) { window.open = guardedOpen; }

  // ---- clicks: remember the real taps, refuse scripted clicks on links that open windows, refuse overlay links
  function onTap(e) {
    var a = anchorOf(e.target);
    tap.t = Date.now(); tap.href = a ? a.href : '';
    notePress(e);
  }
  function onClick(e) {
    var a = anchorOf(e.target);
    // (the app learns what the last real tap was on from content.js, in the isolated world: this page-side script cannot be believed about taps, RX3-G2)
    if (!e.isTrusted) {
      // a script "clicked" a link: it may not open a window, and may not move the page to another site, unless a real tap on that very link just happened
      if (!on || !a) return;
      if (a.hasAttribute('download')) return;                     // saving a file the page generated
      var abs = absolute(a.href);
      var sameTap = fresh() && tap.href && absolute(tap.href) === abs;
      if (sameTap) return;
      var blocks = opensWindow(a) || !sameSite(hostOf(abs), pageHost());
      if (blocks && abs.indexOf('http') === 0) { e.preventDefault(); e.stopImmediatePropagation(); note(opensWindow(a) ? 'click' : 'redirect', abs); }
      return;
    }
    onTap(e);
    if (on && a && isOverlay(a)) { e.preventDefault(); e.stopImmediatePropagation(); try { a.style.setProperty('pointer-events', 'none', 'important'); } catch (x) { /* ignore */ } note('overlay', absolute(a.href)); }
  }
  window.addEventListener('click', onClick, true);
  window.addEventListener('auxclick', onClick, true);
  // these two only record the tap and never cancel it: passive, so the engine never has to wait for them before it scrolls or zooms
  window.addEventListener('touchend', function (e) { if (e.isTrusted) onTap(e); }, { capture: true, passive: true });
  window.addEventListener('pointerup', function (e) { if (e.isTrusted) onTap(e); }, { capture: true, passive: true });

  // ---- forms that open a window
  try {
    var realSubmit = HTMLFormElement.prototype.submit;
    HTMLFormElement.prototype.submit = function () {
      var t = (this.target || '').toLowerCase();
      if (on && t && t !== '_self' && t !== '_top' && t !== '_parent' && !fresh()) { note('form', absolute(this.action)); return undefined; }
      return realSubmit.apply(this, arguments);
    };
  } catch (e) { /* ignore */ }

  // ---- AP1: one press plays the video. A press on a player that makes the page open an ad (refused above) usually never reached the player: a transparent layer or an ad script took it, and only a later press
  // gets through ("I have to press play several times"). After a refusal that follows a real press on a player, this starts the player itself - the real video of this frame - and when there is no video yet,
  // presses through the layer that took the press, at the same place. At most three rounds, stopped by any playback; never for a press that was not on a player, and never for a press made while the video was
  // playing (that one was a pause). A player in a frame is told by message (the frame's own copy of this script plays it). Nothing here opens, loads or navigates anything.
  var ASSIST_MAX = 5, ASSIST_DELAY = 200, ASSIST_WINDOW = 2000, ASSIST_EVENT = '__kst_assist_v1';
  var PLAYERISH = /(^|[\s_-])(player|jwplayer|video-js|vjs|plyr|flowplayer|mejs|videoplayer|video-player|embed-player)([\s_-]|$)/i;
  var press = { t: 0, x: -1, y: -1, playing: false, player: null, target: null };
  var assist = { n: 0, timer: 0, last: 0 };
  var refusedAt = 0;

  function playingNow() {
    try {
      var l = document.getElementsByTagName('video');
      for (var i = 0; i < l.length; i++) { var v = l[i]; if (!v.paused && !v.ended && v.readyState > 2 && v.currentTime > 0) return true; }
    } catch (e) { /* ignore */ }
    return false;
  }
  function pointOf(e) {
    try {
      var t = e.changedTouches && e.changedTouches[0];
      var x = t ? t.clientX : e.clientX, y = t ? t.clientY : e.clientY;
      return (typeof x === 'number' && typeof y === 'number') ? { x: x, y: y } : null;
    } catch (x2) { return null; }
  }
  // the player under a point: a <video>, a frame or canvas of player size, or a container that calls itself a player. Looked for under whatever layer lies on top of it.
  function playerAt(x, y) {
    try {
      var list = document.elementsFromPoint ? document.elementsFromPoint(x, y) : [document.elementFromPoint(x, y)];
      for (var i = 0; i < list.length && i < 14; i++) {
        for (var n = list[i], d = 0; n && n !== document.body && n !== document.documentElement && d < 6; n = n.parentElement, d++) {
          var tag = n.tagName;
          if (tag === 'VIDEO') return n;
          var named = typeof n.className === 'string' && PLAYERISH.test(n.className + ' ' + (n.id || ''));
          if (tag === 'IFRAME' || tag === 'CANVAS' || named) { var r = n.getBoundingClientRect(); if (r.width >= 120 && r.height >= 80) return n; }
        }
      }
    } catch (e) { /* ignore */ }
    return null;
  }
  function videoIn(root) {
    try {
      if (!root) return null;
      if (root.tagName === 'VIDEO') return root;
      return root.querySelector ? root.querySelector('video') : null;
    } catch (e) { return null; }
  }
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
  function hasSource(v) {
    try { return !!(v.currentSrc || v.getAttribute('src') || v.srcObject || (v.querySelector && v.querySelector('source[src]'))); } catch (e) { return false; }
  }
  function notePress(e) {
    var now = Date.now(), pt = pointOf(e);
    if (now - press.t > 400) { // the first event of a press (touchend, pointerup, click all come): what the page looked like BEFORE any handler ran
      press = { t: now, x: pt ? pt.x : -1, y: pt ? pt.y : -1, playing: playingNow(), player: pt ? playerAt(pt.x, pt.y) : null, target: e.target };
      if (now - assist.last > 4000) assist.n = 0;
    } else { press.t = now; if (pt && press.x < 0) { press.x = pt.x; press.y = pt.y; } }
    if (now - refusedAt < 800) scheduleAssist(); // the ad was opened by an earlier event of this same press (a mousedown handler)
  }
  function scheduleAssist() {
    if (!on || assist.timer || assist.n >= ASSIST_MAX) return;
    if (!press.t || !press.player || press.playing || Date.now() - press.t > ASSIST_WINDOW) return;
    assist.timer = setTimeout(runAssist, ASSIST_DELAY);
  }
  function startMedia(v, viaFrame) {
    try {
      var p = v.play();
      // a frame that was not pressed itself may be refused sound: start it muted rather than not at all
      if (p && p.catch) p.catch(function () { if (!viaFrame) return; try { v.muted = true; var q = v.play(); if (q && q.catch) q.catch(function () { /* ignore */ }); } catch (e) { /* ignore */ } });
    } catch (e) { /* ignore */ }
  }
  // a layer that took the press: positioned, not the player or something that holds it, not a control, no words of its own
  function isLayer(el, root) {
    try {
      if (!el || /^(VIDEO|IFRAME|CANVAS|BUTTON|INPUT|SELECT|TEXTAREA|HTML|BODY|A)$/.test(el.tagName)) return false;
      if (root && (el === root || el.contains(root))) return false;
      if (el.querySelector && el.querySelector('video, iframe, canvas')) return false;
      var pos = getComputedStyle(el).position;
      if (pos !== 'fixed' && pos !== 'absolute') return false;
      if ((el.textContent || '').trim().length > 24) return false;
      return true;
    } catch (e) { return false; }
  }
  // ---- RX3-G4 (version 2): an INVISIBLE FRAME of another site laid over a player is switched off. The first version (alpha44) also looked, at every touch, for invisible layers of this document over a player and
  // switched them off before the press; an independent review the same day showed that to be unsafe (it disabled a page's own transparent click layers, dialog backdrops, touch pads and icon boxes, for good), and
  // the assist above already answers the same-document layers from EVIDENCE (a press that made the page open an ad). So the touch path is gone. What stays needs no guess: a touch that lands inside a FRAME never
  // reaches this document (events do not leave a frame), a frame of ANOTHER site that nobody can see (opacity under 0.05 or hidden), positioned, covering at least 60 percent of a player, is not a thing a person
  // put there, and it is found by a look at the page: when it is parsed, when it has loaded and a few times in the seconds after (ad scripts add their frames late), for each player of the page (a <video> or canvas
  // of player size, or a container that calls itself a player). A frame that is later seen (a lazy embed that fades in) is given back by the next look and by a slow check for half a minute.
  var fakeSeen = (typeof WeakSet === 'function') ? new WeakSet() : null;
  var offFrames = [], giveBackRounds = 0;
  function frameHidden(cs) { return parseFloat(cs.opacity) < 0.05 || cs.visibility === 'hidden'; }
  function coversMostOf(el, player) {
    var a = el.getBoundingClientRect(), b = player.getBoundingClientRect();
    var w = Math.min(a.right, b.right) - Math.max(a.left, b.left), h = Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top);
    return w > 0 && h > 0 && b.width > 0 && b.height > 0 && w * h >= 0.6 * b.width * b.height;
  }
  function fakeFrameOver(el, player) {
    try {
      if (!el || el.tagName !== 'IFRAME' || el === player || (player.contains && player.contains(el))) return false;
      var cs = getComputedStyle(el);
      if ((cs.position !== 'fixed' && cs.position !== 'absolute') || !frameHidden(cs)) return false;
      var h = hostOf(absolute(el.getAttribute('src') || ''));
      if (!h || sameSite(h, pageHost())) return false;
      return coversMostOf(el, player);
    } catch (e) { return false; }
  }
  function giveBack() { // a frame that can be seen now was only fading in: it gets its touches back
    giveBackRounds++;
    for (var i = offFrames.length - 1; i >= 0; i--) {
      var f = offFrames[i];
      try {
        if (!f.isConnected) { offFrames.splice(i, 1); continue; }
        if (!frameHidden(getComputedStyle(f))) { f.style.removeProperty('pointer-events'); if (fakeSeen) fakeSeen.delete(f); offFrames.splice(i, 1); }
      } catch (e) { offFrames.splice(i, 1); }
    }
    if (offFrames.length && giveBackRounds < 15) setTimeout(giveBack, 2000);
  }
  function switchOffFrames(x, y, player) {
    var list = document.elementsFromPoint(x, y), n = 0;
    for (var i = 0; i < list.length && i < 8 && n < 4; i++) {
      var el = list[i];
      if (el === player || (player.contains && player.contains(el))) break;      // reached the player: what lies beneath is not over it
      if (fakeSeen && fakeSeen.has(el)) continue;
      if (fakeFrameOver(el, player)) {
        if (fakeSeen) fakeSeen.add(el);
        el.style.setProperty('pointer-events', 'none', 'important');
        offFrames.push(el); n++;
        if (offFrames.length === 1) { giveBackRounds = 0; setTimeout(giveBack, 2000); }
      }
    }
  }
  function sweepFrames() {
    try {
      if (!on || !document.elementsFromPoint || !document.querySelector('iframe')) return;
      giveBack();
      var cands = document.querySelectorAll('video, canvas, [class*="player" i], [id*="player" i]');
      for (var i = 0; i < cands.length && i < 12; i++) {
        var r = cands[i].getBoundingClientRect();
        if (r.width < 120 || r.height < 80 || r.bottom < 0 || r.top > innerHeight || r.right < 0 || r.left > innerWidth) continue;
        var x = Math.min(Math.max(r.left + r.width / 2, 1), innerWidth - 1), y = Math.min(Math.max(r.top + r.height / 2, 1), innerHeight - 1);
        switchOffFrames(x, y, cands[i]);
      }
    } catch (e) { /* ignore */ }
  }
  [0, 1500, 4000, 9000].forEach(function (ms) { setTimeout(sweepFrames, ms); });
  document.addEventListener('DOMContentLoaded', sweepFrames, true);
  window.addEventListener('load', sweepFrames, true);

  // press through at a point: the pointer events of a press, to what lies beneath (a layer on top is switched off for a moment so it does not take the press again)
  function pressThrough(x, y, layer) {
    try {
      if (x < 0 || y < 0) return;
      var off = null;
      if (layer && layer.isConnected && isLayer(layer, null)) { off = layer; off.style.setProperty('pointer-events', 'none', 'important'); }
      var el = document.elementFromPoint(x, y);
      var a = el ? anchorOf(el) : null;
      if (el && el !== document.body && el !== document.documentElement && !(a && (crossSiteLink(a) || opensWindow(a)))) {
        var init = { bubbles: true, cancelable: true, composed: true, view: window, clientX: x, clientY: y, button: 0, buttons: 1 };
        var seq = ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'];
        inAssist = true;
        try {
          for (var i = 0; i < seq.length; i++) {
            var type = seq[i];
            var ev = (type.indexOf('pointer') === 0 && window.PointerEvent) ? new PointerEvent(type, init) : new MouseEvent(type, init);
            el.dispatchEvent(ev);
          }
        } finally { inAssist = false; }
      }
      if (off) setTimeout(function () { try { off.style.removeProperty('pointer-events'); } catch (e) { /* ignore */ } }, 1500);
    } catch (e) { /* ignore */ }
  }
  // A player in a FRAME is not told by this script (RX3-G3): a page cannot be trusted to speak to a frame it embeds, and a frame must not obey its parent (any page could then play videos and click
  // buttons inside any frame it embeds, with no action of the owner). This only says "go" to the extension's content script (a DOM event without parameters); that script works out the frame and the place
  // from the owner's own recorded press, and the background page asks the frame through the extension (a channel no web page can use).
  function askFrame() { try { document.dispatchEvent(new CustomEvent(ASSIST_EVENT, { detail: '' })); } catch (e) { /* ignore */ } }
  function runAssist() {
    assist.timer = 0;
    if (!on || playingNow()) return;
    assist.n++; assist.last = Date.now();
    var root = playerAt(press.x, press.y) || (press.player && press.player.isConnected ? press.player : null);
    var v = videoIn(root) || bestVideo();
    if (v && !hasSource(v)) v = null;                  // a player that only builds its video after the first press: that press has to go through instead
    if (v) { if (v.paused && !v.ended) startMedia(v, false); return; }
    if (root && root.tagName === 'IFRAME') {
      askFrame();
      try { if (press.target && isLayer(press.target, root)) { var t = press.target; t.style.setProperty('pointer-events', 'none', 'important'); setTimeout(function () { try { t.style.removeProperty('pointer-events'); } catch (e) { /* ignore */ } }, 1500); } } catch (e) { /* ignore */ }
      return;
    }
    pressThrough(press.x, press.y, press.target);
  }
  // any playback ends the rounds
  document.addEventListener('play', function () { assist.n = 0; if (assist.timer) { clearTimeout(assist.timer); assist.timer = 0; } }, true);

  // ---- the content script tells us whether the guard applies here
  document.addEventListener(CFG_EVENT, function (e) {
    try {
      var d = String(e.detail);
      on = d.charAt(0) === '1';
      var bar = d.indexOf('|');
      if (bar > 0) topHost = d.slice(bar + 1).toLowerCase();
    } catch (x) { /* ignore */ }
  }, true);
  try { document.dispatchEvent(new CustomEvent(READY_EVENT)); } catch (e) { /* ignore */ }
})();
