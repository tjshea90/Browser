// OPT1 (research/OPTIMIZER.md): helpers shared by the benchmark pages of Auto-tune. These pages are served by the app itself on 127.0.0.1 (random port, random token in the path) and report their numbers
// by POST to /<token>/r. They measure; they never read or send anything of the person's. ES5 style on purpose (the same file runs in the engine on the tablet and in Chromium in web-tests/bench.test.js).
// `?fast=1` shortens every run (tests only).
(function () {
  var K = window.K = {};
  var qs = location.search || '';
  K.token = (location.pathname.split('/')[1]) || '';
  K.n = (/[?&]n=(\d+)/.exec(qs) || [0, '0'])[1];
  K.fast = /[?&]fast=1/.test(qs);
  K.now = function () { return performance.now(); };
  K.sleep = function (ms) { return new Promise(function (r) { setTimeout(r, ms); }); };
  K.median = function (a) { if (!a.length) return 0; var s = a.slice().sort(function (x, y) { return x - y; }), n = s.length; return n % 2 ? s[(n - 1) / 2] : (s[n / 2 - 1] + s[n / 2]) / 2; };
  K.pct = function (a, p) { if (!a.length) return 0; var s = a.slice().sort(function (x, y) { return x - y; }); return s[Math.min(s.length - 1, Math.floor(p * s.length))]; };
  K.round = function (x) { return Math.round(x * 100) / 100; };
  /** Counts the page's own animation frames for [ms]: { fps, late (share of gaps over 50 ms, in %), gap (the longest gap in ms), frames }. */
  K.cadence = function (ms) {
    return new Promise(function (resolve, reject) {
      var t0 = K.now(), last = t0, gaps = [], frames = 0, done = false;
      // no frame at all for 6 s = the page is not being drawn (hidden, or the engine is stuck): say so at once instead of running into the page's time limit
      var dog = setInterval(function () { if (!done && K.now() - last > 6000) { done = true; clearInterval(dog); reject(new Error('no animation frames for 6 s (page not drawn)')); } }, 500);
      function tick(t) {
        if (done) return;
        var now = K.now(); gaps.push(now - last); last = now; frames++;
        if (now - t0 >= ms) {
          done = true; clearInterval(dog);
          var late = 0, mx = 0; for (var i = 1; i < gaps.length; i++) { if (gaps[i] > 50) late++; if (gaps[i] > mx) mx = gaps[i]; }
          resolve({ fps: K.round(frames * 1000 / (now - t0)), late: K.round(100 * late / Math.max(1, gaps.length - 1)), gap: K.round(mx), frames: frames });
        } else requestAnimationFrame(tick);
      }
      requestAnimationFrame(tick);
    });
  };
  /** Tells the app (and the tests) that the page finished. [m] = numbers, [err] = what went wrong, if anything. */
  K.note = '';
  K.report = function (page, m, err) {
    var body = JSON.stringify({ page: page, n: K.n, m: m || {}, err: err || '', note: K.note || '' });
    document.title = 'bench-done ' + page;
    try { fetch('/' + K.token + '/r', { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: body, keepalive: true }).catch(function () {}); } catch (e) { /* the app also reads the title */ }
  };
  K.ready = function (page) { document.title = 'bench-ready ' + page; };
  /** Runs [fn] (async), reports its numbers; any exception becomes the error text so the run records WHY, never a silent hang. */
  K.run = function (page, fn) {
    var guard = setTimeout(function () { K.report(page, {}, 'timeout in the page'); }, K.fast ? 20000 : 60000);
    Promise.resolve().then(fn).then(function (m) { clearTimeout(guard); K.report(page, m, ''); }, function (e) { clearTimeout(guard); K.report(page, {}, String(e && e.message || e)); });
  };
})();
