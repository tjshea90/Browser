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
