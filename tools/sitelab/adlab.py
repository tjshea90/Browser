#!/usr/bin/env python3
"""Ad blocking lab (AW1): python3 adlab.py <port> <logfile>

One server, two site names, the same page. On the emulator `adb reverse tcp:<port> tcp:<port>` plus the engine preference `network.dns.localDomains` (set through `setprop debug.kestrel.pref1
"network.dns.localDomains=adlab.test,control.test,ads.adlab.test"`) make http://adlab.test:<port>/ and http://control.test:<port>/ reach it. The lab filter set (made with tools:run from
tools/sitelab/adlab-rules.txt, pushed over files/filters/current.kfs of the debug app) has rules for adlab.test and NONE for control.test, so the control page shows what each trick does when nothing
defuses it, and the adlab page shows what the browser's blocking does about it.

The page plays the tricks real sites play and writes one line per check into its own text (for a picture or a uiautomator dump) and into the log (/log?...):
  wall      an anti-adblock wall that appears when the page's bait variable says "blocked"          -> defused by  set, adblockDetected, false
  nag       a "please whitelist us" box shown by a timer                                             -> defused by  nostif, nagBox
  countdown a "wait 8 seconds" button that opens after a timer                                       -> sped up by  nano-stb, countdown, *, 0.02
  popunder  a click listener that would open an ad window (it marks the page instead)                -> removed by  aeld, click, popunder
  player    a player config fetched as JSON that carries ad placements                               -> cut by      json-prune-fetch-response, adPlacements adSlots, , propsToMatch, /player.json
  bait      a script the page inserts that holds a detector                                          -> emptied by  rmnt, script, adbait
  beacon    a request to ads.adlab.test                                                              -> answered by no-fetch-if (a stub answer, nothing leaves) or blocked by the list; a LEAK is a 'BEACON reached the server' line in the log
  opener    window.open from a timer (an ad tab)                                                     -> answered by nowoif
  entity    set, entityOk, true  for  adlab.*  (an entity rule: any suffix)
  banner    an element the list hides                                                                -> .ad-box
 AW1c: blocked files that are answered with a harmless stand-in instead of a bare cancel (rules `$redirect=` in adlab-rules.txt; every one of these files is REAL on the control page and its request
 shows up in the log as "REAL /r/...": on the adlab page none of those lines may appear, the request is answered inside the browser)
  rscript   <script src=/r/tag.js>       redirect=noopjs               -> onload fires, the real file (window.__realTag) never ran
  rplain    <script src=/r/plain.js>     blocked, no stand-in          -> onerror
  rima      <script src=/r/ima.js>       redirect=google-ima.js        -> google.ima is there, an ad request answers adError 1009
  rimage    new Image().src=/r/pixel.gif redirect=1x1.gif              -> loads 1x1
  rxhr      XMLHttpRequest /r/data.json  redirect=noopjson             -> status 200, '{}'
  rfetch    fetch /r/data.json?f=1       redirect=noopjson             -> '{}'
  rmedia    <video src=/r/clip.mp4>      redirect=noopmp4-1s           -> loadedmetadata (the control page's file is a 404)
  rframe    <iframe src=/r/frame.html>   redirect=noopframe            -> load fires, the frame is empty
"""
import http.server, sys, urllib.parse, time, json

PAGE = r"""<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Ad lab __HOST__</title>
<style>
 body { font: 16px sans-serif; margin: 10px; background: #fff; color: #111 }
 #wall { display: none; position: fixed; left: 0; right: 0; top: 0; padding: 18px; background: #b00; color: #fff; z-index: 9; font-size: 20px }
 #nag { display: none; margin: 6px 0; padding: 8px; background: #fc0 }
 .ad-box { margin: 6px 0; padding: 14px; background: #e8d; }
 pre { background: #eef; padding: 8px; font-size: 15px; white-space: pre-wrap }
 button { font-size: 18px; padding: 10px 14px }
</style></head><body>
<div id="wall">Please turn off your ad blocker to continue</div>
<h3>Ad lab on __HOST__</h3>
<div class="ad-box" id="banner">BANNER (the list hides this)</div>
<div id="nag">NAG: please whitelist us</div>
<button id="cd" disabled>Wait ...</button>
<pre id="out">running ...</pre>
<script>
 var R = {}; function say(k, v) { R[k] = v; try { new Image().src = '/log?k=' + k + '&v=' + encodeURIComponent(v) + '&h=__HOST__&t=' + Date.now(); } catch (e) {} paint(); }
 function paint() { var s = ''; for (var k in R) s += k + ': ' + R[k] + '\n'; var o = document.getElementById('out'); if (o) o.textContent = s; }
 // timing: when did the browser's scriptlets run, compared with this script (the library reports "<ran>:<failed>" on the document when it has run its calls)
 var T0 = Math.round(performance.now()); var TS = Date.now();
 document.addEventListener('__kst_sl_n_v1', function (e) { say('timing', 'scriptlets ran (' + e.detail + ') ' + (Date.now() - TS) + ' ms after the first page script (page script at ' + T0 + ' ms)'); }, true);
 document.addEventListener('DOMContentLoaded', function () { say('dcl', (Date.now() - TS) + ' ms after the first page script'); }, true);
 // wall: the page's own detector decides "blocked" because the bait never loaded
 window.adblockDetected = true;
 // nag + countdown + popunder
 setTimeout(function nagBox() { document.getElementById('nag').style.display = 'block'; }, 700);
 var t0 = Date.now();
 setTimeout(function countdown() { var b = document.getElementById('cd'); b.disabled = false; b.textContent = 'GO'; say('countdown', 'opened after ' + (Date.now() - t0) + ' ms'); }, 8000);
 window.__pop = 0;
 document.addEventListener('click', function popunder() { window.__pop = 1; document.title = 'POPPED'; say('popunder', 'listener RAN (page marked)'); });
 // entity rule
 window.entityOk = false;
 // the bait script the page inserts (its text holds the detector word)
 var B = 'ad' + 'bait';
 var s = document.createElement('script'); s.textContent = 'window.__baitRan = 1; /* ' + B + ' */'; document.head.appendChild(s);
 document.addEventListener('DOMContentLoaded', function () {
   if (window.adblockDetected) { document.getElementById('wall').style.display = 'block'; say('wall', 'SHOWN (detector says blocked)'); } else say('wall', 'hidden');
   setTimeout(function () {
     var nag = getComputedStyle(document.getElementById('nag')).display;
     say('nag', nag === 'none' ? 'not shown' : 'SHOWN');
     say('banner', getComputedStyle(document.getElementById('banner')).display === 'none' ? 'hidden by the list' : 'visible');
     say('bait', window.__baitRan ? 'bait script RAN' : 'bait script did not run');
     say('entity', window.entityOk ? 'entity rule applied' : 'no entity rule');
   }, 1500);
   fetch('/player.json').then(function (r) { return r.json(); }).then(function (j) { say('player', 'adPlacements=' + (j.adPlacements ? j.adPlacements.length : 0) + ' adSlots=' + (j.adSlots ? j.adSlots.length : 0) + ' video=' + j.video); }, function (e) { say('player', 'failed ' + e); });
   fetch('http://ads.adlab.test:__PORT__/beacon').then(function (r) { return r.text(); }).then(function (t) { say('beacon', 'answered ' + t.length + ' chars'); }, function (e) { say('beacon', 'did not leave (blocked)'); });
   // AW1c: stand-ins
   var s1 = document.createElement('script'); s1.onload = function () { say('rscript', 'load fired' + (window.__realTag ? ', the REAL file ran' : ', the real file did not run')); }; s1.onerror = function () { say('rscript', 'error (blocked, no stand-in)'); }; s1.src = '/r/tag.js'; document.head.appendChild(s1);
   var s2 = document.createElement('script'); s2.onload = function () { say('rplain', 'load fired (not blocked)'); }; s2.onerror = function () { say('rplain', 'error (blocked)'); }; s2.src = '/r/plain.js'; document.head.appendChild(s2);
   var s3 = document.createElement('script');
   s3.onload = function () { try { var l = new google.ima.AdsLoader(new google.ima.AdDisplayContainer(document.body, document.createElement('video'))); l.addEventListener('adError', function (e) { say('rima', 'adError ' + e.getError().getErrorCode()); }); l.requestAds(new google.ima.AdsRequest()); } catch (e) { say('rima', 'script loaded, no google.ima (' + (window.__realIma ? 'the REAL file ran' : 'nothing ran') + ')'); } };
   s3.onerror = function () { say('rima', 'error (blocked, no stand-in)'); }; s3.src = '/r/ima.js'; document.head.appendChild(s3);
   var im = new Image(); im.onload = function () { say('rimage', 'loaded ' + im.naturalWidth + 'x' + im.naturalHeight); }; im.onerror = function () { say('rimage', 'error (blocked, no stand-in)'); }; im.src = '/r/pixel.gif';
   var x = new XMLHttpRequest(); x.open('GET', '/r/data.json'); x.onload = function () { say('rxhr', 'status ' + x.status + ' text ' + x.responseText); }; x.onerror = function () { say('rxhr', 'error (blocked, no stand-in)'); }; x.send();
   fetch('/r/data.json?f=1').then(function (r) { return r.text(); }).then(function (t) { say('rfetch', 'text ' + t); }, function (e) { say('rfetch', 'error ' + e); });
   var v = document.createElement('video'); v.muted = true; v.onloadedmetadata = function () { say('rmedia', 'metadata, duration ' + v.duration); }; v.onerror = function () { say('rmedia', 'error (blocked or 404 or undecodable): ' + (v.error ? v.error.code + ' ' + String(v.error.message).slice(0, 80) : '?')); }; v.src = '/r/clip.mp4'; document.body.appendChild(v);
   var fr = document.createElement('iframe'); fr.onload = function () { var d = ''; try { d = ', body text "' + fr.contentDocument.body.textContent + '"'; } catch (e) { d = ', not readable'; } say('rframe', 'load fired' + d); }; fr.src = '/r/frame.html'; document.body.appendChild(fr);
   setTimeout(function () { var w = window.open('/popup', '_blank'); say('opener', w ? ('window.open returned ' + (w.closed === false ? 'a stand-in' : 'a window')) : 'window.open returned null'); }, 2200);
 });
</script></body></html>
"""

class H(http.server.BaseHTTPRequestHandler):
    logfile = None
    port = 0

    def _send(self, code, body=b"", ctype="text/html; charset=utf-8"):
        self.send_response(code); self.send_header("Content-Type", ctype); self.send_header("Content-Length", str(len(body))); self.send_header("Cache-Control", "no-store"); self.end_headers()
        self.wfile.write(body)

    def _write(self, line):
        with open(self.logfile, "a", encoding="utf-8") as f:
            f.write("%s %s\n" % (time.strftime("%H:%M:%S"), line))

    def do_GET(self):
        u = urllib.parse.urlparse(self.path)
        q = urllib.parse.parse_qs(u.query)
        host = (self.headers.get("Host") or "?").split(":")[0]
        if u.path == "/":
            page = PAGE.replace("__HOST__", host).replace("__PORT__", str(self.port))
            self._write("page host=%s" % host)
            return self._send(200, page.encode())
        if u.path == "/player.json":
            self._write("player.json host=%s" % host)
            return self._send(200, json.dumps({"video": "movie.mp4", "adPlacements": [1, 2], "adSlots": [3]}).encode(), "application/json")
        if u.path == "/beacon":
            self._write("BEACON reached the server host=%s (the ad host was not blocked)" % host)
            return self._send(200, b"pong")
        if u.path == "/popup":
            self._write("POPUP opened host=%s" % host)
            return self._send(200, b"<!doctype html><title>popup</title><h1>an ad window opened</h1>")
        if u.path.startswith("/r/"):
            self._write("REAL %s host=%s (this request reached the server: nothing answered it inside the browser)" % (u.path, host))
            if u.path == "/r/tag.js": return self._send(200, b"window.__realTag = 1;", "text/javascript")
            if u.path == "/r/plain.js": return self._send(200, b"window.__realPlain = 1;", "text/javascript")
            if u.path == "/r/ima.js": return self._send(200, b"window.__realIma = 1;", "text/javascript")
            if u.path == "/r/pixel.gif": return self._send(200, b"GIF89a\x01\x00\x01\x00\x80\x00\x00\x00\x00\x00\xff\xff\xff!\xf9\x04\x01\x00\x00\x00\x00,\x00\x00\x00\x00\x01\x00\x01\x00\x00\x02\x02D\x01\x00;", "image/gif")
            if u.path == "/r/data.json": return self._send(200, b'{"real":true,"padding":"xxxxxxxxxxxxxxxx"}', "application/json")
            if u.path == "/r/frame.html": return self._send(200, b"<!doctype html><title>real</title><body>REAL FRAME</body>")
            return self._send(404)
        if u.path == "/log":
            self._write("check %s = %s (%s)" % (q.get("k", ["?"])[0], q.get("v", [""])[0], q.get("h", ["?"])[0]))
            return self._send(204)
        if u.path == "/banner.png":
            self._write("BANNER image requested host=%s" % host)
            return self._send(200, b"GIF89a\x01\x00\x01\x00\x80\x00\x00\x00\x00\x00\xff\xff\xff!\xf9\x04\x01\x00\x00\x00\x00,\x00\x00\x00\x00\x01\x00\x01\x00\x00\x02\x02D\x01\x00;", "image/gif")
        self._send(404)

    def log_message(self, *a):
        pass

if __name__ == "__main__":
    H.port, H.logfile = int(sys.argv[1]), sys.argv[2]
    open(H.logfile, "a").close()
    http.server.ThreadingHTTPServer(("0.0.0.0", H.port), H).serve_forever()
