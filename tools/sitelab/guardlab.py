#!/usr/bin/env python3
"""Guard lab (RX3-G1, research/REVIEW-EXTERNAL-3.md part G): python3 guardlab.py <port> <logfile>

  /fakeplay   a page that, WITHOUT any tap, tries to open a pop-up window at 0.3 s, 3 s, 8 s and 15 s after it loaded (window.open of /popad), and writes what it saw to <logfile>.
              With the pop-up guard in the page, window.open is refused (no GET /popad ever reaches this server); without it a window opens and the server logs "GET /popad".
  /popad      the page an ad would open; it only exists to be requested.
  /assist-parent  RX3-G3: a page with a cross-origin frame (the same server under another host name: 127.0.0.1) holding a "player", and a transparent layer over it that opens an ad when TAPPED.
              A real tap on the layer: the pop-up is refused, and the assist asks the frame (through the extension, never through a message the page could send) to start its video:
              the frame logs "child play() called" (a stub counts the call: no media file is needed). Without the assist the log has no such line.
  /assist-child   the frame's page (served for the host 127.0.0.1).

Used as the FIRST page after a force-stop (a cold start from a link: the extension may not be active yet) and as a later page, to see whether the guard was there from the very first document.
Nothing here is part of the app. Reach it with `adb reverse tcp:<port> tcp:<port>` and http://localhost:<port>/fakeplay. The driver is tools/emu/firstpage_emu.py.
"""
import http.server, sys, time, urllib.parse

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
<body style="margin:0;background:#111;color:#eee;font:18px sans-serif"><video id="v" src="http://127.0.0.1:%PORT%/clip.mp4" style="width:100%;height:380px;background:#000" muted></video>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
// no media file is needed: the call to play() is what the assist must make
HTMLMediaElement.prototype.play = function () { L('child', 'child play() called'); return Promise.resolve(); };
L('load', 'assist-child first-script guard=' + (window.__kstGuard === 1 ? 'YES' : 'no') + ' host=' + location.host);
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
        if u.path == "/popad":
            self._write("GET /popad %s   <- the pop-up was NOT stopped" % q.get("n", ["?"])[0])
            return self._send(200, POPAD)
        if u.path == "/assist-parent":
            self._write("GET /assist-parent")
            return self._send(200, ASSIST_PARENT.replace(b"%PORT%", str(self.server.server_address[1]).encode()))
        if u.path == "/assist-child":
            self._write("GET /assist-child (host %s)" % self.headers.get("Host", "?"))
            return self._send(200, ASSIST_CHILD.replace(b"%PORT%", str(self.server.server_address[1]).encode()))
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
