#!/usr/bin/env python3
"""Guard lab (RX3-G1, research/REVIEW-EXTERNAL-3.md part G): python3 guardlab.py <port> <logfile>

  /fakeplay   a page that, WITHOUT any tap, tries to open a pop-up window at 0.3 s, 3 s, 8 s and 15 s after it loaded (window.open of /popad), and writes what it saw to <logfile>.
              With the pop-up guard in the page, window.open is refused (no GET /popad ever reaches this server); without it a window opens and the server logs "GET /popad".
  /popad      the page an ad would open; it only exists to be requested.

Used as the FIRST page after a force-stop (a cold start from a link: the extension may not be active yet) and as a later page, to see whether the guard was there from the very first document.
Nothing here is part of the app. Reach it with `adb reverse tcp:<port> tcp:<port>` and http://localhost:<port>/fakeplay. The driver is tools/emu/firstpage_emu.py.
"""
import http.server, sys, time, urllib.parse

FAKEPLAY = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>fakeplay</title></head>
<body style="font:20px sans-serif;margin:16px"><p>fakeplay: tries to open a pop-up by script, with no tap, four times.</p>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
L('load', 'fakeplay');
[300, 3000, 8000, 15000].forEach(function (ms, i) {
  setTimeout(function () {
    var w = null, err = '';
    try { w = window.open('/popad?n=' + (i + 1), '_blank'); } catch (e) { err = String(e); }
    L('attempt', (i + 1) + ' at ' + ms + ' ms: window.open returned ' + (w ? 'a window object' : 'null') + (err ? ' error ' + err : ''));
  }, ms);
});
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
