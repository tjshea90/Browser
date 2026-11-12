// The tap-to-load box of an embedded player (EX-10). ES5, no libraries. The address to load travels in the URL fragment (the background script put it there).
(function () {
  var u = '';
  try { u = decodeURIComponent(location.hash.slice(1)); } catch (e) { u = ''; }
  var t = document.getElementById('t'), h = document.getElementById('h');
  var host = '';
  try { host = new URL(u).hostname; } catch (e) { host = ''; }
  if (!/^https?:\/\//i.test(u) || !host) { t.textContent = 'Nothing to load'; return; }
  h.textContent = host;
  var started = false;
  document.body.addEventListener('click', function () {
    if (started) return;
    started = true;
    t.textContent = 'Loading...';
    var go = function () { location.replace(u); };
    try { browser.runtime.sendMessage({ k: 'embed-allow', u: u }).then(go, go); } catch (e) { go(); }
  });
})();
