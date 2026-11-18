#!/usr/bin/env python3
"""AK4 lab: a movie page (port 8000) with an embedded player page (8003) whose stream comes from a CDN (8004) that wants the PLAYER's Referer.
The playlists are served as text/plain under .txt names, the parts are named .jpg, wear a real PNG header and are AES-128 encrypted, the key sits behind the same Referer check.
AL1 / AL5 additions: /watch2 (port 8000) has Server 1 / Server 2 / Server 3 buttons that point ONE iframe at different player pages (a server switch); server 2 offers only the 240p quality
(so its list differs from server 1's), server 3 first loads a short "ad" video and 4 s later the real stream in the SAME document (an ad before the real video must NOT count as a server switch);
/plain is a page with no video at all (a page change must empty the list)."""
import http.server, os, struct, subprocess, sys, threading, time, zlib, binascii
from urllib.parse import parse_qs

HERE = os.path.dirname(os.path.abspath(__file__))
GEN = os.environ.get("VIDLAB_GEN") or os.path.join(HERE, "gen")  # the .ts files made by vidlab_gen.sh
LOG = os.environ.get("VIDLAB_LOG") or os.path.join(HERE, "lab.log")
HOST = "10.0.2.2"
PLAYER = "http://%s:8003" % HOST
KEY = bytes(range(16, 32))

def chunk(t, d): return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
PNG = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(b"\x00\xff\x00\x00")) + chunk(b"IEND", b"")

def encrypt(data, seq):
    iv = seq.to_bytes(16, "big")
    return subprocess.run(["openssl", "enc", "-aes-128-cbc", "-K", KEY.hex(), "-iv", iv.hex()], input=data, capture_output=True, check=True).stdout

FILES = {}   # path -> (content type, bytes)
def build():
    variants = [("360", 640, 360, 800000), ("240", 426, 240, 400000)]
    master = "#EXTM3U\n#EXT-X-VERSION:3\n"
    for q, w, h, bw in variants:
        master += "#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d,CODECS=\"avc1.42c01e,mp4a.40.2\"\nv%s.txt\n" % (bw, w, h, q)
        pl = "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:4\n#EXT-X-MEDIA-SEQUENCE:0\n#EXT-X-KEY:METHOD=AES-128,URI=\"key\"\n"
        for i in range(3):
            ts = open(os.path.join(GEN, "v%s_%d.ts" % (q, i)), "rb").read()
            FILES["/hls/abc/v%s_%d.jpg" % (q, i)] = ("image/jpeg", PNG + encrypt(ts, i))
            pl += "#EXTINF:4.000000,\nv%s_%d.jpg\n" % (q, i)
        pl += "#EXT-X-ENDLIST\n"
        FILES["/hls/abc/v%s.txt" % q] = ("text/plain", pl.encode())
    FILES["/hls/abc/playlist.txt"] = ("text/plain", master.encode())
    FILES["/hls/abc/key"] = ("application/octet-stream", KEY)
    # server 2: the same parts under another prefix, but its master lists only the 240p quality
    for i in range(3): FILES["/hls/def/v240_%d.jpg" % i] = FILES["/hls/abc/v240_%d.jpg" % i]
    FILES["/hls/def/v240.txt"] = FILES["/hls/abc/v240.txt"]
    FILES["/hls/def/playlist.txt"] = ("text/plain", ("#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-STREAM-INF:BANDWIDTH=400000,RESOLUTION=426x240,CODECS=\"avc1.42c01e,mp4a.40.2\"\nv240.txt\n").encode())
    FILES["/hls/def/key"] = ("application/octet-stream", KEY)
    # the "ad": a tiny progressive MP4 (no Referer check, see do_GET)
    ad = os.path.join(GEN, "ad.mp4")
    if not os.path.exists(ad):
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", os.path.join(GEN, "v240_0.ts"), "-c", "copy", "-movflags", "+faststart", ad], check=True)
    FILES["/ads/ad.mp4"] = ("video/mp4", open(ad, "rb").read())
    # VS1d: a fragmented MP4 for the MSE embed (/embed/mse): the player makes a MediaSource, waits for `sourceopen`, appends this file and plays (like the custom players of embed sites)
    mse = os.path.join(GEN, "mse.mp4")
    try:
        if not os.path.exists(mse):
            subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", os.path.join(GEN, "v360_0.ts"), "-c", "copy", "-bsf:a", "aac_adtstoasc", "-movflags", "frag_keyframe+empty_moov+default_base_moof", "-f", "mp4", mse], check=True)
        FILES["/mse/film.mp4"] = ("video/mp4", open(mse, "rb").read())
    except Exception as e:
        print("no MSE film:", e, flush=True)

PAGE_A = b"""<!doctype html><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1"><title>Lab movie page</title>
<body style="background:#111;color:#eee;font:16px sans-serif"><h1>Lab movie</h1><p>The player below comes from another site.</p>
<iframe src="%s/embed/movie?id=7" style="width:100%%;height:260px;border:1px solid #444"></iframe></body>""" % PLAYER.encode()

PAGE_B = b"""<!doctype html><meta charset=utf-8><title>player</title><body style="background:#222;color:#9f9;font:14px monospace">
<div id=s>starting</div><video controls muted playsinline style="width:100%;height:120px;background:#000"></video>
<script>
(function () {
  var base = 'http://10.0.2.2:8004/hls/abc/', out = document.getElementById('s');
  function get(u, bin) { return fetch(base + u).then(function (r) { if (!r.ok) throw new Error(u + ' ' + r.status); return bin ? r.arrayBuffer() : r.text(); }); }
  setTimeout(function () {
    get('playlist.txt').then(function (m) {
      var v = m.split('\\n').filter(function (l) { return l && l[0] !== '#'; });
      return get(v[0]).then(function (t) {
        var segs = t.split('\\n').filter(function (l) { return l && l[0] !== '#'; });
        return get('key', true).then(function () { return get(segs[0], true); }).then(function () { return get(segs[1], true); }).then(function () { out.textContent = 'player loaded: ' + segs.length + ' parts'; });
      });
    }).catch(function (e) { out.textContent = 'player error: ' + e; });
  }, 800);
})();
</script>"""

def player(base, ad=False):
    """A player page like PAGE_B that reads the stream under [base]; with [ad] it first fetches the ad video and starts the real stream 4 s later in the same document."""
    return (b"""<!doctype html><meta charset=utf-8><title>player</title><body style="background:#222;color:#9f9;font:14px monospace">
<div id=s>starting</div><video controls muted playsinline style="width:100%%;height:120px;background:#000"></video>
<script>
(function () {
  var base = 'http://10.0.2.2:8004/hls/%s/', out = document.getElementById('s');
  function get(u, bin) { return fetch(base + u).then(function (r) { if (!r.ok) throw new Error(u + ' ' + r.status); return bin ? r.arrayBuffer() : r.text(); }); }
  function stream() {
    get('playlist.txt').then(function (m) {
      var v = m.split('\\n').filter(function (l) { return l && l[0] !== '#'; });
      return get(v[0]).then(function (t) {
        var segs = t.split('\\n').filter(function (l) { return l && l[0] !== '#'; });
        return get('key', true).then(function () { return get(segs[0], true); }).then(function () { return get(segs[1], true); }).then(function () { out.textContent = 'player %s loaded: ' + segs.length + ' parts'; });
      });
    }).catch(function (e) { out.textContent = 'player error: ' + e; });
  }
  if (%s) { out.textContent = 'ad playing'; fetch('http://10.0.2.2:8004/ads/ad.mp4').then(function (r) { return r.arrayBuffer(); }).then(function () { setTimeout(stream, 4000); }); }
  else setTimeout(stream, 800);
})();
</script>""" % (base.encode(), base.encode(), b"true" if ad else b"false"))

PAGE_C = b"""<!doctype html><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1"><title>Lab movie servers</title>
<body style="background:#111;color:#eee;font:18px sans-serif"><h1>Lab movie 2</h1>
<p><button style="font-size:20px;padding:10px" onclick="go('movie')">Server 1</button> <button style="font-size:20px;padding:10px" onclick="go('server2')">Server 2</button> <button style="font-size:20px;padding:10px" onclick="go('adfirst')">Server 3</button></p>
<iframe id=f src="" style="width:100%%;height:260px;border:1px solid #444"></iframe>
<script>function go(n) { document.getElementById('f').src = '%s/embed/' + n + '?id=7'; }</script></body>""" % PLAYER.encode()

PAGE_PLAIN = b"""<!doctype html><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1"><title>Plain page</title><body style="font:22px sans-serif"><h1>A page with no video</h1><p>Nothing to download here.</p></body>"""

# AP1 lab: a player whose first presses are taken by fake layers (each opens an ad and removes itself); the real play control starts a real video. The page says how many REAL presses it took.
PAGE_FAKE = b"""<!doctype html><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1"><title>Fake play lab</title>
<body style="background:#111;color:#eee;font:16px sans-serif;margin:0">
<h2 style="margin:8px">Fake play lab</h2>
<div id=st style="margin:8px;font:bold 22px monospace;color:#ff6">waiting: press play ONCE</div>
<div id=ads style="margin:8px;font:15px monospace">fake layers fired: 0 | real presses: 0</div>
<div id=dbg style="margin:8px;font:13px monospace;color:#9cf">...</div>
<div id=player class=player style="position:relative;width:100%;height:230px;background:#000">
  <video id=v src="http://10.0.2.2:8000/clip.mp4" playsinline style="position:absolute;left:0;top:0;width:100%;height:100%"></video>
  <div id=realplay style="position:absolute;left:50%;top:50%;width:90px;height:90px;margin:-45px 0 0 -45px;background:#19c;border-radius:45px;color:#fff;text-align:center;line-height:90px;font-size:38px">&#9654;</div>
  <div id=f1 style="position:absolute;left:0;top:0;right:0;bottom:0"></div>
  <div id=f2 style="position:absolute;left:0;top:0;right:0;bottom:0"></div>
</div>
<script>
var fired = 0, presses = 0, v = document.getElementById('v');
function show() { document.getElementById('ads').textContent = 'fake layers fired: ' + fired + ' | real presses: ' + presses; }
document.addEventListener('click', function (e) { if (e.isTrusted) { presses++; show(); } }, true);
document.getElementById('realplay').addEventListener('click', function () { v.play(); });
['f1', 'f2'].forEach(function (id) {
  var el = document.getElementById(id);
  el.addEventListener('click', function () { fired++; show(); window.open('http://10.0.2.3:8000/ad?l=' + id, '_blank'); window.__opens = (window.__opens || 0) + 1; el.remove(); });
});
var realPlay = v.play.bind(v), playCalls = 0, lastResult = '-';
v.play = function () { playCalls++; var r = realPlay(); if (r && r.then) r.then(function () { lastResult = 'ok'; info(); }, function (e) { lastResult = 'rejected ' + (e && e.name); info(); }); return r; };
function info() { document.getElementById('dbg').textContent = 'play() calls: ' + playCalls + ' | last: ' + lastResult + ' | error: ' + (v.error ? v.error.code : '-') + ' | state: ' + v.readyState + '/' + v.networkState; }
v.addEventListener('error', info); v.addEventListener('loadeddata', info);
v.addEventListener('play', function () { var st = document.getElementById('st'); st.textContent = 'PLAYING after ' + presses + ' real press(es)'; st.style.color = '#6f6'; });
</script></body>"""

# VS1 (owner message 2026-11-18: doraby.com + the vidsrc server, the stream is not found): more of the ways a real embed behaves, all reading the SAME Referer-gated stream (base /hls/abc/).
#   /watch3?v=auto|click|nest|worker  (port 8000) a movie page with ONE iframe to the player page /embed/<v> (port 8003):
#   auto   = PAGE_B (the playlist is fetched 0.8 s after the load)           click  = nothing is fetched until a tap on the player's big button (a click-to-play embed)
#   nest   = the player page holds ANOTHER iframe that is the real player (a chain of frames, like vidsrc -> cloudnestra -> player)
#   worker = the player's script does its fetches inside a Worker made from a blob (a player that keeps its network code off the main thread)
PLAYER_JS = b"""
  var base = 'http://10.0.2.2:8004/hls/abc/', out = document.getElementById('s');
  function get(u, bin) { return fetch(base + u).then(function (r) { if (!r.ok) throw new Error(u + ' ' + r.status); return bin ? r.arrayBuffer() : r.text(); }); }
  function stream() {
    get('playlist.txt').then(function (m) {
      var v = m.split('\\n').filter(function (l) { return l && l[0] !== '#'; });
      return get(v[0]).then(function (t) {
        var segs = t.split('\\n').filter(function (l) { return l && l[0] !== '#'; });
        return get('key', true).then(function () { return get(segs[0], true); }).then(function () { return get(segs[1], true); }).then(function () { out.textContent = 'player loaded: ' + segs.length + ' parts'; });
      });
    }).catch(function (e) { out.textContent = 'player error: ' + e; });
  }
"""
WORKER_JS = ("self.onmessage=function(e){var base=e.data;function get(u,bin){return fetch(base+u).then(function(r){if(!r.ok)throw new Error(u+' '+r.status);return bin?r.arrayBuffer():r.text();});}"
             "get('playlist.txt').then(function(m){var v=m.split('\\n').filter(function(l){return l&&l[0]!=='#';});return get(v[0]).then(function(t){var s=t.split('\\n').filter(function(l){return l&&l[0]!=='#';});"
             "return get('key',true).then(function(){return get(s[0],true);}).then(function(){return get(s[1],true);}).then(function(){postMessage('worker loaded '+s.length);});});}).catch(function(x){postMessage('worker error '+x);});};")
def player_mode(mode):
    head = b"""<!doctype html><meta charset=utf-8><title>player %s</title><body style="margin:0;background:#222;color:#9f9;font:14px monospace"><div id=s>starting</div>""" % mode.encode()
    if mode == "click":
        return head + b"""<button id=big style="display:block;width:100%%;height:260px;font-size:34px;background:#136;color:#fff;border:0">Play</button><script>(function () {%s
  document.getElementById('big').onclick = function () { out.textContent = 'starting'; stream(); this.style.display = 'none'; };
})();</script>""" % PLAYER_JS
    if mode == "worker":
        return head + b"""<video controls muted playsinline style="width:100%%;height:120px;background:#000"></video><script>(function () {
  var out = document.getElementById('s'), code = %s;
  setTimeout(function () { try { var w = new Worker(URL.createObjectURL(new Blob([code]))); w.onmessage = function (e) { out.textContent = e.data; }; w.postMessage('http://10.0.2.2:8004/hls/abc/'); } catch (e) { out.textContent = 'no worker: ' + e; } }, 800);
})();</script>""" % repr(WORKER_JS).encode()
    if mode == "nest":
        return head + b"""<iframe src="%s/embed/movie?id=7" style="width:100%%;height:240px;border:0"></iframe>""" % PLAYER.encode()
    return head + b"""<video controls muted playsinline style="width:100%%;height:120px;background:#000"></video><script>(function () {%s  setTimeout(stream, 800);
})();</script>""" % PLAYER_JS

# VS1d (owner message 2026-11-18: "Even the vidfast server on doraby.Com can't find the video. it just stays stuck on loading"): doraby's way. The movie page has a big Play / server button; ITS tap sets the iframe's
# address (servers.min.js: jQuery("#iframe").attr("src", ...)); the embed is another site whose player builds a MediaSource, waits for `sourceopen`, appends the film and plays by itself. The embed tells the lab
# how far it got through requests to /state (the lab log): sourceopen, playing, playrejected_<name>, stuck (14 s without a picture). A person's tap on the PARENT page is the only press there is.
PAGE_MSE = b"""<!doctype html><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1"><title>Lab movie (mse)</title>
<body style="margin:0;background:#111;color:#eee;font:16px sans-serif"><h3 style="margin:8px;height:34px">Lab movie (mse)</h3>
<button id=play class="play-btn server" style="display:block;width:100%;height:120px;font-size:30px;background:#136;color:#fff;border:0">Play</button>
<iframe id=iframe src="about:blank" style="display:block;width:100%;height:230px;border:0"></iframe>
<script>document.getElementById('play').onclick = function () { document.getElementById('iframe').src = 'http://10.0.2.2:8003/embed/mse?id=7'; };</script></body>"""

PLAYER_MSE = b"""<!doctype html><meta charset=utf-8><title>player mse</title><body style="margin:0;background:#222;color:#9f9;font:14px monospace">
<video id=v muted playsinline autoplay style="width:100%;height:150px;background:#000"></video><div id=s>Loading...</div>
<script>(function () {
  var v = document.getElementById('v'), s = document.getElementById('s'), played = false;
  function rep(x) { try { new Image().src = 'http://10.0.2.2:8000/state?s=' + x + '&t=' + Date.now(); } catch (e) {} }
  v.addEventListener('playing', function () { played = true; s.textContent = 'playing'; rep('playing'); });
  var ms = new MediaSource(); v.src = URL.createObjectURL(ms);
  ms.addEventListener('sourceopen', function () {
    rep('sourceopen');
    var sb = ms.addSourceBuffer('video/mp4; codecs="avc1.42E01E,mp4a.40.2"');
    fetch('http://10.0.2.2:8004/mse/film.mp4').then(function (r) { return r.arrayBuffer(); }).then(function (b) {
      sb.addEventListener('updateend', function () { try { ms.endOfStream(); } catch (e) {} var p = v.play(); if (p && p.catch) p.catch(function (e) { rep('playrejected_' + e.name); }); }, { once: true });
      sb.appendBuffer(b);
    }).catch(function (e) { rep('fetcherror'); });
  });
  setTimeout(function () { if (!played) { s.textContent = 'stuck loading'; rep('stuck'); } }, 14000);
})();</script>"""

def page_watch3(mode):
    return (b"""<!doctype html><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1"><title>Lab movie %s</title>
<body style="margin:0;background:#111;color:#eee;font:16px sans-serif"><h3 style="margin:8px;height:34px">Lab movie (%s)</h3>
<iframe src="%s/embed/%s?id=7" style="display:block;width:100%%;height:300px;border:0;border-top:1px solid #444"></iframe></body>""" % (mode.encode(), mode.encode(), PLAYER.encode(), mode.encode()))

class H(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.0"
    def log_message(self, fmt, *a): pass
    def note(self, status):
        with open(LOG, "a") as f: f.write("%.1f port=%d %s %s status=%d referer=%r origin=%r range=%r\n" % (time.time() % 10000, self.server.server_port, self.command, self.path, status, self.headers.get("Referer"), self.headers.get("Origin"), self.headers.get("Range")))
    def send(self, status, ctype, body, extra=None):
        self.send_response(status); self.send_header("Content-Type", ctype); self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*"); self.send_header("Cache-Control", "no-store")
        for k, v in (extra or {}).items(): self.send_header(k, v)
        self.end_headers(); self.wfile.write(body); self.note(status)
    def do_OPTIONS(self): self.send(204, "text/plain", b"", {"Access-Control-Allow-Headers": "*"})
    def do_GET(self):
        port = self.server.server_port; path = self.path.split("?")[0]
        if port == 8000 and path == "/watch": return self.send(200, "text/html; charset=utf-8", PAGE_A)
        if port == 8003 and path == "/embed/movie": return self.send(200, "text/html; charset=utf-8", PAGE_B)
        if port == 8000 and path == "/watch2": return self.send(200, "text/html; charset=utf-8", PAGE_C)
        if port == 8000 and path == "/watch3" and "v=mse" in self.path: return self.send(200, "text/html; charset=utf-8", PAGE_MSE)
        if port == 8003 and path == "/embed/mse": return self.send(200, "text/html; charset=utf-8", PLAYER_MSE)
        if port == 8000 and path == "/watch3": return self.send(200, "text/html; charset=utf-8", page_watch3((parse_qs(self.path.partition("?")[2]).get("v") or ["auto"])[0]))
        if port == 8003 and path in ("/embed/auto", "/embed/click", "/embed/nest", "/embed/worker"): return self.send(200, "text/html; charset=utf-8", player_mode(path.rsplit("/", 1)[1]))
        if port == 8000 and path == "/plain": return self.send(200, "text/html; charset=utf-8", PAGE_PLAIN)
        if port == 8000 and path == "/fakeplay": return self.send(200, "text/html; charset=utf-8", PAGE_FAKE)
        if port == 8000 and path == "/clip.mp4": return self.send(200, "video/mp4", FILES["/ads/ad.mp4"][1])
        if port == 8000 and path == "/ad": return self.send(200, "text/html; charset=utf-8", b"<!doctype html><title>spam</title><h1>THIS IS THE SPAM PAGE</h1>")
        if port == 8003 and path == "/embed/server2": return self.send(200, "text/html; charset=utf-8", player("def"))
        if port == 8003 and path == "/embed/adfirst": return self.send(200, "text/html; charset=utf-8", player("abc", ad=True))
        if port == 8004 and path in FILES:
            ref = self.headers.get("Referer") or ""; org = self.headers.get("Origin")
            if not path.startswith("/ads/") and (not ref.startswith(PLAYER + "/") or (org is not None and org != PLAYER)):
                return self.send(403, "text/plain", b"forbidden: this stream is for its own player only")
            ctype, body = FILES[path]
            rng = self.headers.get("Range")
            if rng and rng.startswith("bytes="):
                a, _, b = rng[6:].partition("-"); a = int(a or 0); b = int(b) if b else len(body) - 1; b = min(b, len(body) - 1)
                part = body[a:b + 1]
                return self.send(206, ctype, part, {"Content-Range": "bytes %d-%d/%d" % (a, b, len(body))})
            return self.send(200, ctype, body)
        self.send(404, "text/plain", b"not found")

def main():
    open(LOG, "w").close(); build()
    servers = []
    for port in (8000, 8003, 8004):
        s = http.server.ThreadingHTTPServer(("0.0.0.0", port), H); servers.append(s)
        threading.Thread(target=s.serve_forever, daemon=True).start()
    print("lab up on 8000 8003 8004; files:", sorted(FILES), flush=True)
    while True: time.sleep(3600)
main()
