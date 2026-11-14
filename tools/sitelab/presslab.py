#!/usr/bin/env python3
"""Lab page for long presses (AY2, owner message 39): python3 presslab.py <port> <logfile>

One page with every kind of thing a finger can long press: blank space, a paragraph of text, a link, a picture that is a link, a picture, a button, a text field, a custom clickable box and an iframe. The
page's own script writes what it sees on every long press to <logfile>: the element under the finger, whether the page was given a click afterwards, and what text (if any) the engine selected 250 ms later.
Nothing here is part of the app; it only runs on a developer machine / the sandbox next to an emulator (reach it with `adb reverse tcp:<port> tcp:<port>` and http://localhost:<port>/).
"""
import http.server, sys, urllib.parse, time

PAGE = b"""<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Press lab</title>
<style>
 body { font: 18px sans-serif; margin: 0; background: #fff; color: #111 }
 #top { height: 120px; background: #eef }
 p { margin: 14px 12px; line-height: 1.5 }
 .row { margin: 16px 12px }
 #blank { height: 140px; background: #fff }
 #box { margin: 12px; padding: 18px; background: #fe9; border: 1px solid #ca0 }
 #pic { width: 90px; height: 60px; background: #8c8; display: inline-block }
 iframe { width: 92%; height: 110px; margin: 8px 12px; border: 1px solid #999 }
 pre { margin: 12px; padding: 8px; background: #eef; font-size: 13px; white-space: pre-wrap }
</style></head><body>
<div id="top">top blank band (120 px)</div>
<p id="text">This is a paragraph of ordinary text that wraps over several lines so that a long press can land on a word, between two lines, or at the empty end of the last line. More words follow here to fill the line.</p>
<div class="row"><a id="link" href="/page2">a plain link to page two</a></div>
<div class="row"><a id="piclink" href="/page2"><img id="img2" width="90" height="60" alt="linked picture" src="data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='90' height='60'%3E%3Crect width='90' height='60' fill='%2388f'/%3E%3C/svg%3E"></a>
  <img id="img1" width="90" height="60" alt="a picture" src="data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='90' height='60'%3E%3Crect width='90' height='60' fill='%23f88'/%3E%3C/svg%3E"></div>
<div class="row"><button id="btn">a button</button> <input id="inp" type="text" placeholder="a text field"></div>
<div id="box" onclick="log('click', 'custom box was CLICKED')">a custom clickable box (onclick on a div)</div>
<div id="blank">blank band of white space (140 px)</div>
<iframe id="frame" srcdoc="<body style='margin:0;font:16px sans-serif;background:#ffe'><p>inside an iframe</p><div style='height:60px'></div></body>"></iframe>
<pre id="out">(nothing pressed yet)</pre>
<div style="height:900px"></div>
<script>
 function log(kind, what) { try { new Image().src = '/log?kind=' + kind + '&v=' + encodeURIComponent(what) + '&t=' + Date.now(); } catch (e) {} }
 function describe(e) {
   var els = document.elementsFromPoint ? document.elementsFromPoint(e.clientX, e.clientY) : [e.target];
   var names = []; for (var i = 0; i < els.length && i < 6; i++) names.push(els[i].tagName.toLowerCase() + (els[i].id ? '#' + els[i].id : ''));
   return 'at ' + Math.round(e.clientX) + ',' + Math.round(e.clientY) + ' target=' + (e.target.tagName || '?').toLowerCase() + (e.target.id ? '#' + e.target.id : '') + ' stack=' + names.join('>');
 }
 window.addEventListener('contextmenu', function (e) {
   var d = describe(e);
   log('contextmenu', d + ' trusted=' + e.isTrusted);
   setTimeout(function () { var s = String(window.getSelection()); log('selection', 'after 250 ms: [' + s.slice(0, 40) + '] length=' + s.length); }, 250);
   document.getElementById('out').textContent = d;
 }, true);
 window.addEventListener('click', function (e) { log('click', describe(e)); }, true);
 window.addEventListener('touchstart', function (e) { var t = e.touches[0]; log('touchstart', 'at ' + Math.round(t.clientX) + ',' + Math.round(t.clientY)); }, { capture: true, passive: true });
 window.addEventListener('touchend', function (e) { log('touchend', ''); }, { capture: true, passive: true });
 window.addEventListener('touchcancel', function (e) { log('touchcancel', ''); }, { capture: true, passive: true });
 log('loaded', 'dpr=' + window.devicePixelRatio + ' inner=' + innerWidth + 'x' + innerHeight);
</script>
</body></html>
"""

class H(http.server.BaseHTTPRequestHandler):
    logfile = None

    def _send(self, code, body=b"", ctype="text/html; charset=utf-8"):
        self.send_response(code); self.send_header("Content-Type", ctype); self.send_header("Content-Length", str(len(body))); self.send_header("Cache-Control", "no-store"); self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        u = urllib.parse.urlparse(self.path)
        q = urllib.parse.parse_qs(u.query)
        if u.path == "/":
            return self._send(200, PAGE)
        if u.path == "/page2":
            self._write("navigated page2")
            return self._send(200, b"<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width, initial-scale=1'><title>Page two</title><body style='font:20px sans-serif;margin:16px'><h2>Page two</h2><p><a href='/'>back to the press lab</a></p></body>")
        if u.path == "/busy":  # AY1: a script that runs for 50 s, so the engine asks (after 20 s) whether to stop it; the app must answer "wait" without showing anything
            self._write("busy page served")
            return self._send(200, b"<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width, initial-scale=1'><title>Busy page</title><body style='font:20px sans-serif;margin:16px'><h2>Busy page</h2><p>This page runs a script for 50 seconds.</p><script>setTimeout(function(){var end=Date.now()+50000;while(Date.now()<end){}document.title='Busy page finished';},800);</script></body>")
        if u.path == "/log":
            self._write("%s %s" % (q.get("kind", ["?"])[0], q.get("v", [""])[0]))
            return self._send(204)
        self._send(404)

    def _write(self, line):
        with open(self.logfile, "a", encoding="utf-8") as f:
            f.write("%s %s\n" % (time.strftime("%H:%M:%S"), line))

    def log_message(self, *a):
        pass

if __name__ == "__main__":
    port, H.logfile = int(sys.argv[1]), sys.argv[2]
    open(H.logfile, "a").close()
    http.server.ThreadingHTTPServer(("0.0.0.0", port), H).serve_forever()
