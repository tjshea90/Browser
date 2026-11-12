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
    if (window.__kstGuard) return;
    Object.defineProperty(window, '__kstGuard', { value: 1 });
  } catch (e) { return; }

  var on = true;                 // the guard applies until the content script says it does not
  var TAP_MS = 1500;             // how long after a real tap a window may open
  var tap = { t: 0, href: '' };
  var realOpen = window.open;

  // sign-in and payment windows the user asked for with a tap: allowed from any site
  var AUTH = /(^|\.)(accounts\.google\.com|appleid\.apple\.com|login\.microsoftonline\.com|login\.live\.com|www\.facebook\.com|m\.facebook\.com|github\.com|api\.twitter\.com|twitter\.com|x\.com|discord\.com|www\.paypal\.com|paypal\.com|checkout\.stripe\.com|pay\.google\.com|accounts\.spotify\.com|id\.twitch\.tv|www\.linkedin\.com)$/;

  function note(kind, url) {
    try { document.dispatchEvent(new CustomEvent(POP_EVENT, { detail: kind + '|' + (url || '') })); } catch (e) { /* ignore */ }
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
  function pageHost() { return location.hostname.toLowerCase(); }

  function anchorOf(el) {
    try { for (var n = el; n && n !== document; n = n.parentNode) if (n.tagName === 'A' && n.href) return n; } catch (e) { /* ignore */ }
    return null;
  }
  function opensWindow(a) {
    var t = (a.target || '').toLowerCase();
    return t !== '' && t !== '_self' && t !== '_top' && t !== '_parent';
  }
  function fresh() { return Date.now() - tap.t < TAP_MS; }

  // A transparent link laid over the page (or the video) to catch the tap meant for "play".
  function isOverlay(a) {
    try {
      var r = a.getBoundingClientRect();
      if (r.width * r.height < 0.35 * innerWidth * innerHeight) return false;
      var cs = getComputedStyle(a);
      if (parseFloat(cs.opacity) < 0.05 || cs.visibility === 'hidden') return true;
      var empty = !(a.textContent || '').trim() && !a.querySelector('img,svg,video,canvas,picture,button,input');
      return empty && (cs.position === 'fixed' || cs.position === 'absolute');
    } catch (e) { return false; }
  }

  function allowOpen(u) {
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
  }
  function onClick(e) {
    var a = anchorOf(e.target);
    if (e.isTrusted) note('tap', a ? absolute(a.href) : ''); // the extension needs to know what the last real tap was on, to tell a user's link from a script's redirect
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
    if (on && a && isOverlay(a)) { e.preventDefault(); e.stopImmediatePropagation(); note('overlay', absolute(a.href)); }
  }
  window.addEventListener('click', onClick, true);
  window.addEventListener('auxclick', onClick, true);
  window.addEventListener('touchend', function (e) { if (e.isTrusted) onTap(e); }, true);
  window.addEventListener('pointerup', function (e) { if (e.isTrusted) onTap(e); }, true);

  // ---- forms that open a window
  try {
    var realSubmit = HTMLFormElement.prototype.submit;
    HTMLFormElement.prototype.submit = function () {
      var t = (this.target || '').toLowerCase();
      if (on && t && t !== '_self' && t !== '_top' && t !== '_parent' && !fresh()) { note('form', absolute(this.action)); return undefined; }
      return realSubmit.apply(this, arguments);
    };
  } catch (e) { /* ignore */ }

  // ---- the content script tells us whether the guard applies here
  document.addEventListener(CFG_EVENT, function (e) { try { on = String(e.detail).charAt(0) === '1'; } catch (x) { /* ignore */ } }, true);
  try { document.dispatchEvent(new CustomEvent(READY_EVENT)); } catch (e) { /* ignore */ }
})();
