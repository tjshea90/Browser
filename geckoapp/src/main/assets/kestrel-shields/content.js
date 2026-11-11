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
