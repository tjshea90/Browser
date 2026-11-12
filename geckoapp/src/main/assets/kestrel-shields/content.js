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
    var wanted = true;
    function push() { try { document.dispatchEvent(new CustomEvent(CFG, { detail: wanted ? '1' : '0' })); } catch (e) { /* ignore */ } }
    document.addEventListener(READY, push, true); // the page-side script may come up after our first answer
    document.addEventListener(POP, function (e) {
      try { browser.runtime.sendMessage({ k: 'pop', d: String(e.detail).slice(0, 700), p: location.href }).catch(function () {}); } catch (x) { /* ignore */ }
    }, true);
    browser.runtime.sendMessage({ k: 'guard', h: location.hostname }).then(function (r) { wanted = !(r && r.on === false); push(); }, function () {});
  } catch (e) { /* never break the page */ }
})();
