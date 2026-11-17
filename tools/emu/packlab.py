#!/usr/bin/env python3
"""LAB5b: the packaging A/B of the emulator lab (owner message 2026-11-17 06:42Z: "ultra fast, snappy, and lightweight").

  python3 -I tools/emu/packlab.py --out DIR --pkg app.kestrel.browser.gecko.debug --apk A=/path/A.apk --apk B=/path/B.apk [...] [--rounds 3] [--starts 4]

The question each variant answers is one build switch of geckoapp/build.gradle.kts (see research/LAB5_PACKAGING.md):
  A = today's packaging, B = omni.ja stored uncompressed (-PomniStored=true), C = native libraries kept in the APK (-PlibsInApk=true), D = both.
For every variant, round after round (the order is rotated so a drifting runner cannot favour one): a FRESH install (timed, sizes read), one unmeasured first launch (builds the engine's start-up caches), then
`--starts` measured cold starts to a lab page (alternating: a warm file cache = just force-stop, a cold file cache = force-stop + drop_caches), then a fixed page set and a memory snapshot of EVERY app process from
/proc/<pid>/smaps_rollup (Pss split into anonymous = private heap, and file-backed = shared, evictable): a deflated omni.ja is inflated into anonymous memory in each Gecko process, a stored one is mapped from the APK.
Output: PACK-REPORT.md + pack-results.json. Standard library only. Absolute times are an x86 emulator's: only the differences between variants inside ONE run mean something.
"""
import argparse
import http.server
import importlib.util
import json
import os
import re
import socketserver
import statistics
import subprocess
import sys
import threading
import time

HEALTH = re.compile(r"KestrelHealth\s*:\s*\+(?P<t>[\d.]+)s (?P<layer>\S+) (?P<ev>\S+)(?: (?P<rest>.*))?$")
T0 = time.time()


def say(*a):
    print("[%s] " % time.strftime("%H:%M:%S"), *a, flush=True)


class Adb:
    def __init__(self, binary="adb"):
        self.bin = binary

    def run(self, *args, timeout=120):
        try:
            r = subprocess.run([self.bin, *args], capture_output=True, timeout=timeout)
            return (r.stdout + (b"\n" + r.stderr if r.returncode and r.stderr else b"")).decode("utf-8", "replace")
        except subprocess.TimeoutExpired:
            return "TIMEOUT"
        except Exception as e:                                  # adb missing / emulator gone
            return "ERROR %r" % (e,)

    def sh(self, cmd, timeout=120):
        return self.run("shell", cmd, timeout=timeout)


# ------------------------------------------------------------------ parsers (unit-tested in selftest)
def parse_health(text):
    out = []
    for ln in text.splitlines():
        m = HEALTH.search(ln)
        if m:
            rest = m.group("rest") or ""
            out.append({"t": float(m.group("t")), "layer": m.group("layer"), "ev": m.group("ev"), "rest": rest, "kv": dict(re.findall(r"(\w+)=(\S+)", rest))})
    return out


def parse_smaps_rollup(text):
    d = {}
    for ln in text.splitlines():
        m = re.match(r"^(\w+):\s+(\d+) kB", ln)
        if m:
            d[m.group(1)] = int(m.group(2))
    return d


def parse_ps(text, pkg):
    """[(pid, name)] of the app's processes from `ps -A` (the NAME is the last column)."""
    out = []
    for ln in text.splitlines():
        parts = ln.split()
        if len(parts) >= 3 and parts[-1].startswith(pkg) and parts[1].isdigit():
            out.append((int(parts[1]), parts[-1]))
    return out


def parse_am_start(text):
    tot = re.search(r"TotalTime:\s*(\d+)", text)
    wait = re.search(r"WaitTime:\s*(\d+)", text)
    return (int(tot.group(1)) if tot else None), (int(wait.group(1)) if wait else None)


def median(vals):
    vals = [v for v in vals if v is not None]
    return statistics.median(vals) if vals else None


# ------------------------------------------------------------------ the lab page server (the pages of tools/sitelab/soaklab.py, reused by path)
def start_server(port, root):
    sys.argv = ["soaklab", str(port), os.path.join(root, "lab-tmp")]
    os.makedirs(sys.argv[2], exist_ok=True)
    spec = importlib.util.spec_from_file_location("soaklab", os.path.join(root, "tools", "sitelab", "soaklab.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    reports = []

    class H(http.server.BaseHTTPRequestHandler):
        def log_message(self, *a):
            pass

        def _send(self, body, ctype="text/html; charset=utf-8", code=200):
            self.send_response(code)
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def do_GET(self):
            p = self.path.split("?")[0]
            try:
                if p == "/health":
                    return self._send(b"ok", "text/plain")
                if p == "/page/text":
                    return self._send(mod.page_text())
                if p == "/page/images":
                    return self._send(mod.page_images())
                if p == "/bench":
                    return self._send(mod.page_bench())
                if p.startswith("/img/") and p.endswith(".svg"):
                    return self._send(mod.svg(int(p[5:-4] or 0)), "image/svg+xml")
                if p == "/report":
                    reports.append(self.path)
                    return self._send(b"", "image/gif", 204)
                return self._send(b"not found", "text/plain", 404)
            except (BrokenPipeError, ConnectionResetError):
                pass

    class S(socketserver.ThreadingMixIn, http.server.HTTPServer):
        daemon_threads = True
        allow_reuse_address = True

    srv = S(("0.0.0.0", port), H)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    return srv, reports


# ------------------------------------------------------------------ one variant, one round
class Lab:
    def __init__(self, a):
        self.a = a
        self.adb = Adb()
        self.pkg = a.pkg
        self.act = "app.kestrel.browser.MainActivity"
        self.rows = []                                         # one dict per measured start
        self.mem = []                                          # one dict per memory snapshot
        self.inst = []                                         # one dict per install
        self.crashes = []
        self.rooted = False

    def logcat_clear(self):
        self.adb.run("logcat", "-c")

    def health_since_clear(self):
        return parse_health(self.adb.run("logcat", "-d", "-v", "threadtime", "-s", "KestrelHealth:D", timeout=60))

    def pids(self):
        return parse_ps(self.adb.sh("ps -A", timeout=30), self.pkg)

    def kill(self):
        self.adb.sh("am force-stop %s" % self.pkg, timeout=30)
        for _ in range(20):
            if not self.pids():
                break
            time.sleep(0.5)

    def launch(self, url, wait_s=60):
        """Start the app on a page; returns a dict of timings, or {'crash': text}."""
        self.logcat_clear()
        t0 = time.time()
        out = self.adb.sh("am start -W -a android.intent.action.VIEW -d %s -p %s" % (url, self.pkg), timeout=90)
        total, waitt = parse_am_start(out)
        ready = loaded = None
        slow = 0
        end = time.time() + wait_s
        while time.time() < end:
            ev = self.health_since_clear()
            ready = next((e["t"] for e in ev if e["layer"] == "engine" and e["ev"] == "runtime-ready"), None)
            pg = next((e for e in ev if e["layer"] == "page" and e["ev"] == "loaded" and e["kv"].get("host", "").startswith("localhost")), None)
            loaded = pg["t"] if pg else None
            slow = max([int(re.sub(r"\D", "", e["rest"].split("ms")[0]) or 0) for e in ev if e["layer"] == "ui" and e["ev"] in ("slow", "late-beat", "STALL")] or [0])
            if loaded is not None:
                break
            time.sleep(1.0)
        res = {"am_total_ms": total, "am_wait_ms": waitt, "runtime_ready_s": ready, "page_loaded_s": loaded, "ui_block_ms": slow, "wall_s": round(time.time() - t0, 1)}
        if loaded is None:
            tail = self.adb.run("logcat", "-d", "-t", "400", timeout=60)
            bad = [ln.strip()[:200] for ln in tail.splitlines() if re.search(r"FATAL EXCEPTION|UnsatisfiedLinkError|Fatal signal|Process: " + re.escape(self.pkg), ln)][:6]
            res["crash"] = bad or ["no page loaded in %d s (and no crash line)" % wait_s]
        return res

    def install(self, name, apk):
        self.adb.run("uninstall", self.pkg, timeout=120)
        t0 = time.time()
        out = self.adb.run("install", "-t", apk, timeout=600)
        took = time.time() - t0
        ok = "Success" in out
        path = self.adb.sh("pm path %s" % self.pkg, timeout=30).strip().replace("package:", "")
        d = os.path.dirname(path) if path else ""
        sizes = {}
        if d:
            for label, sub in (("package_dir_kb", ""), ("lib_kb", "lib"), ("oat_kb", "oat")):
                o = self.adb.sh("du -sk %s/%s 2>/dev/null" % (d, sub) if sub else "du -sk %s 2>/dev/null" % d, timeout=30).split()
                sizes[label] = int(o[0]) if o and o[0].isdigit() else None
        dp = self.adb.sh("dumpsys package %s | grep -i 'extractNativeLibs\\|legacyNativeLibraryDir\\|primaryCpuAbi'" % self.pkg, timeout=30)
        row = {"variant": name, "install_ok": ok, "install_s": round(took, 1), "apk_kb": os.path.getsize(apk) // 1024, "extract_flag": ("extractNativeLibs=true" in dp.replace(" ", "")) , **sizes}
        self.inst.append(row)
        say("install", name, row)
        return ok

    def snapshot_memory(self, name, rnd):
        """Every app process: Pss split into anonymous (private heap) and file-backed (shared, evictable)."""
        procs = []
        for pid, pname in self.pids():
            d = parse_smaps_rollup(self.adb.sh("cat /proc/%d/smaps_rollup" % pid, timeout=30))
            if d:
                procs.append({"name": pname.replace(self.pkg, "app"), "pss_kb": d.get("Pss"), "pss_anon_kb": d.get("Pss_Anon"), "pss_file_kb": d.get("Pss_File"), "private_dirty_kb": d.get("Private_Dirty"), "rss_kb": d.get("Rss")})
        tot = {k: sum((p[k] or 0) for p in procs) for k in ("pss_kb", "pss_anon_kb", "pss_file_kb", "private_dirty_kb", "rss_kb")}
        self.mem.append({"variant": name, "round": rnd, "procs": procs, **{"total_" + k: v for k, v in tot.items()}})
        say("memory", name, "total pss %d MB (anon %d, file %d), %d processes" % (tot["pss_kb"] // 1024, tot["pss_anon_kb"] // 1024, tot["pss_file_kb"] // 1024, len(procs)))

    def drop_caches(self):
        if self.rooted:
            self.adb.sh("sync; echo 3 > /proc/sys/vm/drop_caches", timeout=60)

    def variant_round(self, name, apk, rnd):
        say("=== round %d variant %s" % (rnd, name))
        if not self.install(name, apk):
            self.crashes.append({"variant": name, "round": rnd, "why": "install failed"})
            return
        url = "http://localhost:%d/page/text" % self.a.port
        first = self.launch(url, 120)                          # unmeasured: creates the engine's caches
        if first.get("crash"):
            self.crashes.append({"variant": name, "round": rnd, "why": "first launch", "lines": first["crash"]})
            say("FIRST LAUNCH FAILED", name, first["crash"])
            return
        time.sleep(5)
        for k in range(self.a.starts):
            cold = (k % 2 == 1)
            self.kill()
            if cold:
                self.drop_caches()
            time.sleep(2)
            r = self.launch(url, 90)
            r.update({"variant": name, "round": rnd, "cache": "cold" if cold else "warm"})
            self.rows.append(r)
            say("start", name, "cache=%s" % r["cache"], {k2: r.get(k2) for k2 in ("am_total_ms", "runtime_ready_s", "page_loaded_s", "ui_block_ms")}, "CRASH" if r.get("crash") else "")
            if r.get("crash"):
                self.crashes.append({"variant": name, "round": rnd, "why": "measured start", "lines": r["crash"]})
        # the page set and the memory picture
        for p in ("/page/images", "/bench", "/page/text"):
            self.adb.sh("am start -a android.intent.action.VIEW -d http://localhost:%d%s -p %s" % (self.a.port, p, self.pkg), timeout=60)
            time.sleep(8)
        time.sleep(15)
        self.snapshot_memory(name, rnd)
        self.kill()

    def report(self, names):
        L = ["# Packaging A/B (LAB5b)", "",
             "Variants: " + ", ".join(names) + ". Rounds %d, measured starts per round %d (alternating warm / cold file cache). **Emulator numbers: read the differences between variants of THIS run, not the milliseconds.**" % (self.a.rounds, self.a.starts), ""]
        L.append("## Install")
        L.append("")
        L.append("| Variant | APK MB | Package dir MB | Unpacked libs MB | Compiled code MB | Install s | extract libs at install |")
        L.append("| :- | -: | -: | -: | -: | -: | :- |")
        for n in names:
            rs = [r for r in self.inst if r["variant"] == n]
            if rs:
                r = rs[-1]
                f = lambda v: "-" if v is None else "%.1f" % (v / 1024.0)
                L.append("| %s | %s | %s | %s | %s | %s | %s |" % (n, f(r["apk_kb"]), f(r.get("package_dir_kb")), f(r.get("lib_kb")), f(r.get("oat_kb")), median([x["install_s"] for x in rs]), r.get("extract_flag")))
        L.append("")
        L.append("## Cold starts to a lab page (medians; lower is better)")
        L.append("")
        L.append("| Variant | cache | n | am start -W ms | engine ready s | page loaded s (since process start) | worst UI block ms |")
        L.append("| :- | :- | -: | -: | -: | -: | -: |")
        base = {}
        for n in names:
            for cache in ("warm", "cold"):
                rs = [r for r in self.rows if r["variant"] == n and r["cache"] == cache and not r.get("crash")]
                if not rs:
                    continue
                m = {k: median([r.get(k) for r in rs]) for k in ("am_total_ms", "runtime_ready_s", "page_loaded_s", "ui_block_ms")}
                if names[0] == n:
                    base[cache] = m
                def d(k, fmt="%.0f"):
                    v = m.get(k)
                    if v is None:
                        return "-"
                    s = fmt % v
                    b = base.get(cache, {}).get(k)
                    if b and n != names[0]:
                        s += " (%+.0f %%)" % ((v - b) / b * 100.0)
                    return s
                L.append("| %s | %s | %d | %s | %s | %s | %s |" % (n, cache, len(rs), d("am_total_ms"), d("runtime_ready_s", "%.2f"), d("page_loaded_s", "%.2f"), d("ui_block_ms")))
        L.append("")
        L.append("## Memory after the page set (sum over every app process; kB / 1024 = MB)")
        L.append("")
        L.append("| Variant | n | total PSS MB | anonymous MB | file-backed MB | processes | per process (PSS MB, anon MB) |")
        L.append("| :- | -: | -: | -: | -: | -: | :- |")
        mb = lambda v: "-" if v is None else "%.1f" % (v / 1024.0)
        for n in names:
            ms = [m for m in self.mem if m["variant"] == n]
            if not ms:
                continue
            last = ms[-1]
            per = "; ".join("%s %.0f/%.0f" % (p["name"].replace("app:", "")[:22], (p["pss_kb"] or 0) / 1024.0, (p["pss_anon_kb"] or 0) / 1024.0) for p in last["procs"])
            L.append("| %s | %d | %s | %s | %s | %d | %s |" % (n, len(ms), mb(median([m["total_pss_kb"] for m in ms])), mb(median([m["total_pss_anon_kb"] for m in ms])), mb(median([m["total_pss_file_kb"] for m in ms])), len(last["procs"]), per))
        if self.crashes:
            L.append("")
            L.append("## Failures")
            L.append("")
            for c in self.crashes:
                L.append("- %s round %s: %s %s" % (c["variant"], c["round"], c["why"], "; ".join(c.get("lines", []))[:300]))
        L.append("")
        return "\n".join(L)


def selftest():
    assert parse_am_start("Status: ok\nTotalTime: 1667\nWaitTime: 1690\n") == (1667, 1690)
    assert parse_smaps_rollup("Rss:   123 kB\nPss:   100 kB\nPss_Anon:    60 kB\nPss_File:    35 kB\n") == {"Rss": 123, "Pss": 100, "Pss_Anon": 60, "Pss_File": 35}
    ps = "USER PID PPID VSZ RSS WCHAN ADDR S NAME\nu0_a116 3914 1000 1 2 x y S app.kestrel.browser.gecko.debug\nu0_a116 4014 3914 1 2 x y S app.kestrel.browser.gecko.debug:gpu\nroot 1 0 1 2 x y S init\n"
    assert [p[0] for p in parse_ps(ps, "app.kestrel.browser.gecko.debug")] == [3914, 4014]
    ev = parse_health("11-17 04:35:22.683  3572  36 D KestrelHealth: +1.124s ui slow 937ms\n11-17 04:35:23.000  3572  36 D KestrelHealth: +2.135s engine runtime-ready\n")
    assert ev[0]["ev"] == "slow" and ev[1]["t"] == 2.135
    assert median([1, 3, None, 2]) == 2
    print("packlab selftest ok")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="pack-out")
    ap.add_argument("--pkg", default="app.kestrel.browser.gecko.debug")
    ap.add_argument("--apk", action="append", default=[], help="NAME=path, the first one is the baseline")
    ap.add_argument("--rounds", type=int, default=3)
    ap.add_argument("--starts", type=int, default=4)
    ap.add_argument("--port", type=int, default=8092)
    ap.add_argument("--repo-root", default=".")
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args()
    if a.selftest:
        selftest()
        return 0
    os.makedirs(a.out, exist_ok=True)
    variants = []
    for spec in a.apk:
        n, _, p = spec.partition("=")
        variants.append((n, p))
    lab = Lab(a)
    lab.adb.run("root", timeout=60)
    time.sleep(3)
    lab.adb.run("wait-for-device", timeout=120)
    lab.rooted = "uid=0" in lab.adb.sh("id", timeout=30)
    say("adb root:", lab.rooted)
    srv, _ = start_server(a.port, os.path.abspath(a.repo_root))
    lab.adb.run("reverse", "tcp:%d" % a.port, "tcp:%d" % a.port)
    for s in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
        lab.adb.sh("settings put global %s 0" % s)
    lab.adb.sh("svc power stayon true")
    lab.adb.sh("input keyevent KEYCODE_WAKEUP")
    lab.adb.sh("wm dismiss-keyguard")
    names = [n for n, _ in variants]
    try:
        for rnd in range(1, a.rounds + 1):
            order = variants[(rnd - 1) % len(variants):] + variants[:(rnd - 1) % len(variants)]       # rotated: no variant is always first (or always after a cold boot)
            for n, p in order:
                lab.variant_round(n, p, rnd)
    except KeyboardInterrupt:
        pass
    finally:
        text = lab.report(names)
        with open(os.path.join(a.out, "PACK-REPORT.md"), "w") as f:
            f.write(text)
        with open(os.path.join(a.out, "pack-results.json"), "w") as f:
            json.dump({"starts": lab.rows, "memory": lab.mem, "installs": lab.inst, "failures": lab.crashes}, f, indent=1)
        print("\n" + text, flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
