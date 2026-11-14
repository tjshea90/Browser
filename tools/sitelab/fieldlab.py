#!/usr/bin/env python3
"""Lab page for typing into a web page's own text fields (AU3): python3 fieldlab.py <port> <logfile>

Serves one page with the kinds of field a real site has and writes everything the page's fields receive to <logfile>, one line per event, so a test can prove from the SERVER side what the keyboard put
into the page (not only what a screenshot shows):

  /            the page: a search form (Enter submits), a stand-alone text input, a search input without a form, a text area, a password input
  /search?q=x  where the form goes (proves Enter submitted it)
  /log?...     the page's own script reports each `input` event ("input id=q value=hello") and each Enter key ("key id=q Enter")

Nothing here is part of the app; it only runs on a developer machine / the sandbox next to an emulator (the emulator reaches it as http://10.0.2.2:<port>/).
"""
import http.server, sys, urllib.parse, time

PAGE = b"""<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Field lab</title>
<style>
 body { font: 18px sans-serif; margin: 12px; background: #fff; color: #111 }
 label { display: block; margin: 14px 0 4px; font-weight: bold }
 input[type=text], input[type=search], input[type=password], textarea { width: 92%; font-size: 20px; padding: 8px; box-sizing: border-box }
 textarea { height: 90px }
 button { font-size: 18px; padding: 8px 16px; margin-left: 6px }
 pre { background: #eef; padding: 8px; font-size: 14px; white-space: pre-wrap }
</style></head><body>
<h2>Field lab</h2>
<form action="/search" method="get">
 <label for="q">1. Search form (Enter submits)</label>
 <input type="text" id="q" name="q" placeholder="search the lab"><button type="submit">Go</button>
</form>
<label for="free">2. Stand-alone text input (no form)</label>
<input type="text" id="free" placeholder="free text">
<label for="s2">3. Search input without a form</label>
<input type="search" id="s2" placeholder="search without a form">
<label for="ta">4. Text area</label>
<textarea id="ta" placeholder="many lines"></textarea>
<label for="pw">5. Password</label>
<input type="password" id="pw" placeholder="password">
<h3>What the fields hold</h3>
<pre id="out">(nothing typed yet)</pre>
<script>
 var ids = ['q', 'free', 's2', 'ta', 'pw'];
 function show() { var t = ''; for (var i = 0; i < ids.length; i++) t += ids[i] + ' = [' + document.getElementById(ids[i]).value + ']\\n'; document.getElementById('out').textContent = t; }
 function report(kind, id, extra) { try { new Image().src = '/log?kind=' + kind + '&id=' + id + '&v=' + encodeURIComponent(extra) + '&t=' + Date.now(); } catch (e) {} }
 for (var i = 0; i < ids.length; i++) (function (id) {
   var el = document.getElementById(id);
   el.addEventListener('input', function () { show(); report('input', id, el.value); });
   el.addEventListener('keydown', function (e) { if (e.key === 'Enter') report('key', id, 'Enter'); });
   el.addEventListener('focus', function () { report('focus', id, ''); });
   el.addEventListener('blur', function () { report('blur', id, ''); });
 })(ids[i]);
 show();
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
        if u.path == "/search":
            what = q.get("q", [""])[0]
            self._write("search q=%s" % what)
            body = ("<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width, initial-scale=1'><title>Results</title><body style='font:20px sans-serif;margin:16px'><h2>Results for: <span id=r>%s</span></h2><p><a href='/'>back to the lab</a></p></body>" % urllib.parse.quote(what, safe=" ")).encode()
            return self._send(200, body)
        if u.path == "/log":
            self._write("%s id=%s value=%s" % (q.get("kind", ["?"])[0], q.get("id", ["?"])[0], q.get("v", [""])[0]))
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
