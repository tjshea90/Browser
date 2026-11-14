#!/usr/bin/env python3
"""Guard lab (RX3-G1, research/REVIEW-EXTERNAL-3.md part G): python3 guardlab.py <port> <logfile>

  /fakeplay   a page that, WITHOUT any tap, tries to open a pop-up window at 0.3 s, 3 s, 8 s and 15 s after it loaded (window.open of /popad), and writes what it saw to <logfile>.
              With the pop-up guard in the page, window.open is refused (no GET /popad ever reaches this server); without it a window opens and the server logs "GET /popad".
  /popad      the page an ad would open; it only exists to be requested.
  /assist-parent  RX3-G3: a page with a cross-origin frame (the same server under another host name: 127.0.0.1) holding a "player", and a transparent layer over it that opens an ad when TAPPED.
              A real tap on the layer: the pop-up is refused, and the assist asks the frame (through the extension, never through a message the page could send) to start its video:
              the frame logs "child play event: the video STARTED" (a real tiny silent media element, the page listens for its play event). Without the assist the log has no such line.
  /assist-child   the frame's page (served for the host 127.0.0.1).
  /playerlab  RX3-G7: five mock video players on one page, driven by the page itself (no touch needed): an ordinary video (no ad words), a JW-style player (jw-flag-ads on its root), a Video.js-style player
              (vjs-ad-loading, then vjs-ad-playing), a Google-IMA-style host (the ad video inside .ima-ad-container) and a JW-style ad with a skip button (.jw-skip, .jw-skippable). Each ad is a REAL 12 s silent
              media element. The log says how long the ad took (about 12000 ms = not skipped, a few hundred = fast-forwarded), at what speed it ran, and which events the skip button got. Ends with a DONE line.

Used as the FIRST page after a force-stop (a cold start from a link: the extension may not be active yet) and as a later page, to see whether the guard was there from the very first document.
Nothing here is part of the app. Reach it with `adb reverse tcp:<port> tcp:<port>` and http://localhost:<port>/fakeplay. The driver is tools/emu/firstpage_emu.py.
"""
import base64, http.server, struct, sys, time, urllib.parse


def silent_wav(seconds=1):
    """Silence (8-bit mono, 8 kHz) of the given length as a data: address: a media element that REALLY plays in the engine, with no codec or file needed."""
    n = 8000 * seconds
    hdr = b"RIFF" + struct.pack("<I", 36 + n) + b"WAVEfmt " + struct.pack("<IHHIIHH", 16, 1, 1, 8000, 8000, 1, 8) + b"data" + struct.pack("<I", n)
    return "data:audio/wav;base64," + base64.b64encode(hdr + b"\x80" * n).decode()


WAV = silent_wav()

FAKEPLAY = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>fakeplay</title></head>
<body style="font:20px sans-serif;margin:16px"><p>fakeplay: tries to open a pop-up by script, with no tap, four times.</p>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
// the guard and the scriptlet library each leave a marker in the page world: is the guard in THIS document, from its very first script on? (RX3-G1)
L('first-script', 'guard in the page world: ' + (window.__kstGuard === 1 ? 'YES' : 'no') + ', scriptlet library: ' + (window.__kstSL === 1 ? 'YES' : 'no'));
L('load', 'fakeplay');
// an about:blank frame the page creates itself (the "iframe race": a script makes a frame and calls the frame's window.open before anything could have been injected into it)
try {
  var f = document.createElement('iframe'); f.style.cssText = 'width:10px;height:10px'; document.body.appendChild(f);
  var fw = f.contentWindow, early = fw.__kstGuard === 1;
  var fr = null; try { fr = fw.open('/popad?n=frame', '_blank'); } catch (e) {}
  L('blank-frame', 'guard marker in the frame right after it was created: ' + (early ? 'YES' : 'no') + ', its window.open returned ' + (fr ? 'a window object' : 'null'));
  setTimeout(function () { L('blank-frame-later', 'guard marker in the frame now: ' + (fw.__kstGuard === 1 ? 'YES' : 'no')); }, 2000);
} catch (e) { L('blank-frame', 'error ' + e); }
[300, 3000, 8000, 15000].forEach(function (ms, i) {
  setTimeout(function () {
    var w = null, err = '';
    try { w = window.open('/popad?n=' + (i + 1), '_blank'); } catch (e) { err = String(e); }
    L('attempt', (i + 1) + ' at ' + ms + ' ms: guard marker ' + (window.__kstGuard === 1 ? 'YES' : 'no') + ', window.open returned ' + (w ? 'a window object' : 'null') + (err ? ' error ' + err : ''));
  }, ms);
});
</script></body></html>
"""

ASSIST_PARENT = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>assist-parent</title></head>
<body style="margin:0;font:18px sans-serif"><div style="position:relative;width:100%;height:420px">
<iframe id="f" src="http://127.0.0.1:%PORT%/assist-child" style="position:absolute;left:0;top:0;width:100%;height:420px;border:0"></iframe>
<div id="layer" style="position:absolute;left:0;top:0;width:100%;height:420px" onclick="window.open('http://127.0.0.1:%PORT%/popad?n=layer','_blank')"></div></div>
<p>assist-parent: tap the dark box. The layer on top tries to open an ad; the frame's video should start instead.</p>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
L('load', 'assist-parent first-script guard=' + (window.__kstGuard === 1 ? 'YES' : 'no'));
document.getElementById('layer').addEventListener('click', function () { L('layer-click', 'the layer got the tap and called window.open'); }, true);
</script></body></html>
"""

ASSIST_CHILD = b"""<!doctype html><html><head><meta charset="utf-8"><title>assist-child</title></head>
<body style="margin:0;background:#111;color:#eee;font:18px sans-serif"><video id="v" src="%WAV%" style="width:100%;height:380px;background:#000" muted loop></video>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
// a real, tiny, silent media element: the assist's own play() call makes the engine fire a play event that THIS page can see (a stubbed play() could not be seen: the extension's calls go through the engine's own wrappers)
document.getElementById('v').addEventListener('play', function () { L('child', 'child play event: the video STARTED'); });
L('load', 'assist-child first-script guard=' + (window.__kstGuard === 1 ? 'YES' : 'no') + ' host=' + location.host);
</script></body></html>
"""

WAV12 = silent_wav(12)
WAV20 = silent_wav(20)

PLAYERLAB = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>playerlab</title></head>
<body style="margin:8px;font:16px sans-serif">
<p>playerlab: mock players, driven by this page (RX3-G7).</p>
<div id="plain" class="post video-wrapper"><video id="plainv" style="width:200px;height:60px;background:#000" muted loop></video></div>
<div id="jw" class="jwplayer jw-reset jw-skin-seven jw-state-playing" style="position:relative;width:300px;height:80px"><div class="jw-media jw-reset"><video id="jwv" class="jw-video jw-reset" style="width:300px;height:80px;background:#000" muted></video></div></div>
<div id="vjs" class="video-js vjs-default-skin" style="position:relative;width:300px;height:80px"><video id="vjsv" class="vjs-tech" style="width:300px;height:80px;background:#000" muted></video></div>
<div id="ima" style="position:relative;width:300px;height:80px"><video id="imac" style="width:300px;height:80px;background:#000" muted loop></video><div class="ima-ad-container" style="position:absolute;left:0;top:0;width:300px;height:80px"><video id="imaad" style="width:300px;height:80px" muted></video></div></div>
<div id="jws" class="jwplayer jw-reset" style="position:relative;width:300px;height:80px"><div class="jw-media jw-reset"><video id="jwsv" class="jw-video jw-reset" style="width:300px;height:80px;background:#000" muted></video></div></div>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
var MAIN = '%WAV20%', AD = '%WAV12%';
function until(fn, ms) { return new Promise(function (res) { var t0 = Date.now(); (function p() { var r = false; try { r = fn(); } catch (e) {} if (r || Date.now() - t0 > ms) res(!!r); else setTimeout(p, 120); })(); }); }
function sleep(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }
function startFilm(v) { v.muted = true; v.loop = true; v.src = MAIN; return v.play().catch(function (e) { L('error', 'film play refused: ' + e); }); }
function sampler(v) { var s = { n: 0, fast: 0 }; var id = setInterval(function () { s.n++; if (v.playbackRate === 16) s.fast++; }, 100); return { stop: function () { clearInterval(id); return s; } }; }
// the ad takes the SAME element (state-class players) or its own element (hosted players); [ended] is set by the ad element's own ended event
function runAd(name, v, before, during, afterAd) {
  var t0 = Date.now(), ended = false, sm = sampler(v);
  return Promise.resolve(before && before()).then(function () {
    during && during();
    v.loop = false; v.src = AD; v.muted = true;
    v.addEventListener('ended', function f() { ended = true; v.removeEventListener('ended', f); });
    return v.play().catch(function (e) { L('error', name + ' ad play refused: ' + e); });
  }).then(function () { return until(function () { return ended; }, 30000); }).then(function (ok) {
    var took = Date.now() - t0, s = sm.stop();
    L(name, 'ad ' + (ok ? 'ended after ' + took + ' ms' : 'did NOT end within 30 s') + ' (12000 ms = normal speed, not skipped); samples ' + s.n + ', at speed 16 in ' + s.fast);
    afterAd && afterAd();
  });
}
function plain() {
  var v = document.getElementById('plainv'), sm = sampler(v);
  return startFilm(v).then(function () { return sleep(3500); }).then(function () { var s = sm.stop(); L('plain', 'ordinary video, no ad words: samples ' + s.n + ', at speed 16 in ' + s.fast + ' (must be 0), playing=' + (!v.paused)); });
}
function jw() {
  var r = document.getElementById('jw'), v = document.getElementById('jwv');
  return startFilm(v).then(function () { return sleep(1500); }).then(function () {
    return runAd('jw', v, null, function () { r.classList.add('jw-flag-ads'); }, function () { r.classList.remove('jw-flag-ads'); });
  });
}
function vjs() {
  var r = document.getElementById('vjs'), v = document.getElementById('vjsv'), sm;
  return startFilm(v).then(function () { return sleep(1500); }).then(function () {
    r.classList.add('vjs-ad-loading'); sm = sampler(v);
    return sleep(1200);
  }).then(function () {
    var s = sm.stop(); L('vjs-loading', 'waiting for the ad (vjs-ad-loading only): samples ' + s.n + ', at speed 16 in ' + s.fast + ' (must be 0)');
    return runAd('vjs', v, null, function () { r.classList.remove('vjs-ad-loading'); r.classList.add('vjs-ad-playing'); }, function () { r.classList.remove('vjs-ad-playing'); });
  });
}
function ima() {
  var c = document.getElementById('imac'), a = document.getElementById('imaad'), sm = sampler(c);
  return startFilm(c).then(function () { return sleep(1500); }).then(function () {
    return runAd('ima', a, null, null, function () { var s = sm.stop(); L('ima-film', 'the film video beside the ad container: samples ' + s.n + ', at speed 16 in ' + s.fast + ' (must be 0), playing=' + (!c.paused)); });
  });
}
function jwskip() {
  var r = document.getElementById('jws'), v = document.getElementById('jwsv'), seen = [];
  return startFilm(v).then(function () { return sleep(1500); }).then(function () {
    r.classList.add('jw-flag-ads');
    Object.defineProperty(v, 'duration', { configurable: true, get: function () { return 400; } });   // a 'long' ad: it is not moved to its end, so the skip button has to be used
    v.loop = true; v.src = AD; v.muted = true;
    var b = document.createElement('div'); b.className = 'jw-skip jw-reset'; b.style.cssText = 'position:absolute;right:4px;bottom:4px;width:80px;height:30px;background:#333';
    ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function (n) { b.addEventListener(n, function (e) { if (b.classList.contains('jw-skippable')) seen.push(n + (e.isTrusted ? '(trusted)' : '')); }); });
    var pressed = false;
    b.addEventListener('click', function () { if (b.classList.contains('jw-skippable')) pressed = true; });
    r.appendChild(b);
    var t0 = Date.now();
    return v.play().catch(function () {}).then(function () { return sleep(1500); }).then(function () {
      var before = pressed;
      b.classList.add('jw-skippable');
      return until(function () { return pressed; }, 20000).then(function (ok) {
        L('jw-skip', 'skip button pressed ' + (ok ? 'by the script ' + (Date.now() - t0) + ' ms after the ad began' : 'NEVER within 20 s') + '; pressed before it was skippable: ' + before + '; events it got once skippable: ' + seen.join(' '));
      });
    });
  });
}
L('load', 'playerlab');
plain().then(jw).then(vjs).then(ima).then(jwskip).then(function () { L('DONE', 'all five scenarios finished'); }, function (e) { L('error', 'lab: ' + e); L('DONE', 'with an error'); });
</script></body></html>
"""

POPAD = b"""<!doctype html><meta charset=utf-8><title>an ad</title><body style="font:20px sans-serif;margin:24px">popad: this is the ad page. If you can see it, the pop-up was NOT stopped.</body>"""


class H(http.server.BaseHTTPRequestHandler):
    logfile = None

    def _send(self, code, body=b"", ctype="text/html; charset=utf-8"):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _write(self, line):
        with open(self.logfile, "a", encoding="utf-8") as f:
            f.write("%s %s\n" % (time.strftime("%H:%M:%S"), line))

    def do_GET(self):
        u = urllib.parse.urlparse(self.path)
        q = urllib.parse.parse_qs(u.query)
        if u.path == "/fakeplay":
            self._write("GET /fakeplay")
            return self._send(200, FAKEPLAY)
        if u.path == "/playerlab":
            self._write("GET /playerlab")
            return self._send(200, PLAYERLAB.replace(b"%WAV12%", WAV12.encode()).replace(b"%WAV20%", WAV20.encode()))
        if u.path == "/popad":
            self._write("GET /popad %s   <- the pop-up was NOT stopped" % q.get("n", ["?"])[0])
            return self._send(200, POPAD)
        if u.path == "/assist-parent":
            self._write("GET /assist-parent")
            return self._send(200, ASSIST_PARENT.replace(b"%PORT%", str(self.server.server_address[1]).encode()))
        if u.path == "/assist-child":
            self._write("GET /assist-child (host %s)" % self.headers.get("Host", "?"))
            return self._send(200, ASSIST_CHILD.replace(b"%PORT%", str(self.server.server_address[1]).encode()).replace(b"%WAV%", WAV.encode()))
        if u.path == "/clip.mp4":
            return self._send(404)
        if u.path == "/log":
            self._write("%s %s" % (q.get("kind", ["?"])[0], q.get("v", [""])[0]))
            return self._send(204)
        self._send(404)

    def log_message(self, *a):
        pass


if __name__ == "__main__":
    port, H.logfile = int(sys.argv[1]), sys.argv[2]
    open(H.logfile, "a").close()
    http.server.ThreadingHTTPServer(("0.0.0.0", port), H).serve_forever()
