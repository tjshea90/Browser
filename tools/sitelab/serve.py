#!/usr/bin/env python3
"""Serve a page copy made by mirror.js: python3 serve.py <dir> <port> [prefix,prefix...]. A request whose path starts with one of the prefixes answers 404 (an unreachable CDN: /_cdn/cdn.example.net)."""
import http.server, sys
root, port = sys.argv[1], int(sys.argv[2])
fails = [p for p in (sys.argv[3].split(',') if len(sys.argv) > 3 else []) if p]
class H(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **k): super().__init__(*a, directory=root, **k)
    def do_GET(self):
        if any(self.path.startswith(p) for p in fails):
            self.send_response(404); self.send_header('Content-Length', '0'); self.end_headers(); return
        super().do_GET()
    def log_message(self, *a): pass
http.server.ThreadingHTTPServer(('0.0.0.0', port), H).serve_forever()
