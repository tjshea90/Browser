#!/usr/bin/env python3
"""AK4 lab: a movie page (port 8000) with an embedded player page (8003) whose stream comes from a CDN (8004) that wants the PLAYER's Referer.
The playlists are served as text/plain under .txt names, the parts are named .jpg, wear a real PNG header and are AES-128 encrypted, the key sits behind the same Referer check."""
import http.server, os, struct, subprocess, sys, threading, time, zlib, binascii

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
        if port == 8004 and path in FILES:
            ref = self.headers.get("Referer") or ""; org = self.headers.get("Origin")
            if not ref.startswith(PLAYER + "/") or (org is not None and org != PLAYER):
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
