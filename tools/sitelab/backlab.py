#!/usr/bin/env python3
"""Back lab (RX3-BK1 / RX3-BK2, research/REVIEW-EXTERNAL-3.md section 3 and 10): python3 backlab.py <port> <logfile>

Pages for the one question "when I press Back, how fast is the page there and is it at the place I left it?":

  /back-static    tall page, cacheable (the engine's back/forward cache can keep it)
  /back-nostore   the same page with Cache-Control: no-store (no cache of any kind may keep it: Back loads it again)
  /back-dynamic   no-store, and only 8 blocks at first: 62 more are added by script 600 ms after the load (the engine restores the place BEFORE the page is tall enough)
  /back-loading   a request that hangs for 90 s, so the page is still loading when the link is tapped (the engine cannot keep a page that is still loading)
  /back-mse       a MediaSource player (the engine cannot keep MSE pages)
  /back-next      the page the link leads to

Every tall page has 70 blocks of 100 CSS px, EACH a link to /back-next, so a tap anywhere hits a link wherever the page is scrolled to. The page writes what it sees (its own scroll place at
+500 ms and +2000 ms after every show, whether the show was `persisted`, the navigation type, history.scrollRestoration) to <logfile> through /log, and the server writes every page request
("GET /back-nostore"): a Back that shows NO new GET for a cacheable page was answered by a cache, one with a GET was loaded again from the network. That is ground truth that does not depend on
the app. Nothing here is part of the app; it runs on a developer machine / the sandbox next to an emulator (reach it with `adb reverse tcp:<port> tcp:<port>` and http://localhost:<port>/).
The driver is tools/emu/back_emu.py.
"""
import http.server, sys, time, urllib.parse

HEAD = r"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>%(name)s</title>
<style>
 body { font: 18px sans-serif; margin: 0; background: #fff; color: #111 }
 .blk { height: 100px; box-sizing: border-box; border-bottom: 1px solid #ccd; position: relative }
 .blk a { display: block; height: 100%%; line-height: 100px; padding-left: 16px; color: #124; text-decoration: none }
 .blk:nth-child(odd) { background: #f4f6ff }
 #hd { height: 100px; background: #dfe; padding: 8px 16px; box-sizing: border-box }
</style></head><body>
<div id="hd">%(name)s - tap any block to go on</div>
<div id="blocks">
%(blocks)s
</div>
%(extra_body)s
<script>
var PAGE = %(name_js)s;
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
var nav = (performance.getEntriesByType('navigation')[0] || {}).type;
L('load', PAGE + ' nav=' + nav + ' restoration=' + history.scrollRestoration + ' y=' + Math.round(window.scrollY));
addEventListener('pageshow', function (e) {
  L('pageshow', PAGE + ' persisted=' + e.persisted + ' nav=' + nav + ' y=' + Math.round(window.scrollY));
  [500, 2000].forEach(function (ms) { setTimeout(function () { L('y+' + ms, PAGE + ' ' + Math.round(window.scrollY) + ' h=' + document.documentElement.scrollHeight); }, ms); });
});
// ?y=<place>: the page scrolls ITSELF there once, 300 ms after a normal visit (nav = navigate), later for a page that grows; never on a Back (nav = back_forward), so it cannot hide what Back does.
// (`adb input swipe` does not scroll a software-rendered emulator reliably; a programmatic scroll leaves the same place in the engine's history entry.)
var AUTO = parseInt((location.search.match(/[?&]y=(\d+)/) || [])[1] || '0', 10);
if (AUTO && nav === 'navigate') setTimeout(function () { window.scrollTo(0, AUTO); L('autoscroll', PAGE + ' to ' + AUTO + ' now ' + Math.round(window.scrollY)); }, %(auto_delay)d);
var st; addEventListener('scroll', function () { clearTimeout(st); st = setTimeout(function () { L('scrolled', PAGE + ' ' + Math.round(window.scrollY)); }, 350); }, { passive: true });
addEventListener('pagehide', function (e) { L('pagehide', PAGE + ' persisted=' + e.persisted + ' y=' + Math.round(window.scrollY)); });
%(extra_script)s
</script></body></html>
"""


def block(i, nxt):
    return '<div class="blk" id="b%d"><a href="%s">block %d - tap to go on</a></div>' % (i, nxt, i)


def tall(name, n=70, extra_body="", extra_script="", nxt="/back-next", auto_delay=300):
    blocks = "\n".join(block(i, "%s?from=%s" % (nxt, name)) for i in range(1, n + 1))
    return (HEAD % dict(name=name, name_js=repr(name), blocks=blocks, extra_body=extra_body, extra_script=extra_script, auto_delay=auto_delay)).encode()


DYNAMIC_SCRIPT = """
setTimeout(function () {
  var box = document.getElementById('blocks'), html = '';
  for (var i = 9; i <= 70; i++) html += '<div class="blk" id="b' + i + '"><a href="/back-next?from=back-dynamic">block ' + i + ' - tap to go on</a></div>';
  box.insertAdjacentHTML('beforeend', html);
  L('grew', PAGE + ' h=' + document.documentElement.scrollHeight);
}, 600);
"""

MSE_BODY = '<video id="v" muted playsinline style="width:300px;height:160px;background:#000"></video>'
MSE_SCRIPT = """
try { var ms = new MediaSource(); document.getElementById('v').src = URL.createObjectURL(ms); ms.addEventListener('sourceopen', function () { L('mse', 'sourceopen'); }); } catch (e) { L('mse', 'error ' + e); }
"""

PAGES = {
    "/back-static": (lambda: tall("back-static"), "max-age=600"),
    "/back-nostore": (lambda: tall("back-nostore"), "no-store"),
    # BK10: the same page under a sign-in path: Instant Back must leave its no-store alone (the control of the A/B)
    "/login/back-nostore": (lambda: tall("back-nostore"), "no-store"),
    "/back-dynamic": (lambda: tall("back-dynamic", n=8, extra_script=DYNAMIC_SCRIPT, auto_delay=1300), "no-store"),
    "/back-loading": (lambda: tall("back-loading", extra_body='<img src="/slow?s=90" width="1" height="1" alt="">'), "max-age=600"),
    "/back-mse": (lambda: tall("back-mse", n=40, extra_body=MSE_BODY, extra_script=MSE_SCRIPT), "max-age=600"),
}

NEXT = b"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>next</title></head>
<body style="font:20px sans-serif;margin:24px"><p>back-next: the page the link leads to. Press Back.</p>
<script>
function L(k, v) { try { var x = new XMLHttpRequest(); x.open('GET', '/log?kind=' + encodeURIComponent(k) + '&v=' + encodeURIComponent(v) + '&t=' + Date.now()); x.send(); } catch (e) {} }
addEventListener('pageshow', function (e) { L('pageshow', 'back-next persisted=' + e.persisted); });
</script></body></html>
"""

INDEX = ("<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width, initial-scale=1'><title>Back lab</title><body style='font:20px sans-serif;margin:24px'>"
         "<h3>Back lab</h3>" + "".join("<p><a href='%s'>%s</a></p>" % (p, p) for p in PAGES)).encode()


class H(http.server.BaseHTTPRequestHandler):
    logfile = None

    def _send(self, code, body=b"", ctype="text/html; charset=utf-8", cache="no-store"):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", cache)
        self.end_headers()
        self.wfile.write(body)

    def _write(self, line):
        with open(self.logfile, "a", encoding="utf-8") as f:
            f.write("%s %s\n" % (time.strftime("%H:%M:%S"), line))

    def do_GET(self):
        u = urllib.parse.urlparse(self.path)
        q = urllib.parse.parse_qs(u.query)
        if u.path == "/":
            return self._send(200, INDEX)
        if u.path in PAGES:
            make, cache = PAGES[u.path]
            self._write("GET %s" % u.path)
            return self._send(200, make(), cache=cache)
        if u.path == "/back-next":
            self._write("GET /back-next")
            return self._send(200, NEXT)
        if u.path == "/slow":  # a request that stays open: the page that holds it is "still loading"
            self._write("GET /slow (hangs)")
            try:
                time.sleep(min(int(q.get("s", ["60"])[0]), 120))
                return self._send(204)
            except Exception:
                return
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
