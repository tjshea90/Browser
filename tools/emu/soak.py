#!/usr/bin/env python3
"""SOAK1 / RX4-E1: a scripted hour of browsing on the emulator, with a plain-text report of everything that went wrong or slow.

  python3 -u -I tools/emu/soak.py --out DIR [--suite smoke|fs|soak|all] [--minutes 60] [--pkg app.kestrel.browser.gecko.debug] [--port 8091] [--seed 1]
  python3 -u -I tools/emu/soak.py --selftest        (no device: a fake phone answers; proves the parsers and the control flow)

What it does (the emulator is the one the GitHub workflow .github/workflows/emu-lab.yml starts: Android 10, x86_64, 600 x 1024 at 170 dpi, 2 GB, 4 cores):
  smoke  install-free check: the app runs, the engine comes up, one lab page loads, the Diagnostics text can be dumped
  fs     the full-screen turning of owner message 43 (FS1): full screen, the screen turns sideways for a wide video, the tablet is "turned" with the emulator's virtual
         sensor, the rotate button, a page's own screen.orientation.lock, leaving full screen
  soak   for --minutes: public pages, videos (the lab clips, public MP4 / WebM / HLS files), downloads (lab files, public files), "find the video" pages, scrolling, a full-screen
         round now and then, tabs closed every few scenes, the app watched all the time (crash, ANR, restart, memory, UI stalls, slow loads)
Everything the app says about itself comes from its own Health lines (adb logcat, tag KestrelHealth, a DEBUG build only), the lab server's report.jsonl (what the pages saw),
dumpsys and the files in /sdcard/Download. The report is OUT/report.md (also printed at the end); OUT/events.jsonl holds one line per scene.
Honest limits (also written into the report): x86_64 + software GL is not the tablet's Mali GPU; the JIT is faster; absolute times mean nothing, only "did it load / play / finish /
crash" and comparisons inside one run do. A site that is down or blocks this runner is reported as UPSTREAM, not as an app fault.
"""
import argparse, collections, json, os, random, re, shlex, shutil, subprocess, sys, threading, time, urllib.error, urllib.parse, urllib.request
import xml.etree.ElementTree as ET

T0 = time.time()
PKG_DEFAULT = "app.kestrel.browser.gecko.debug"
ACT = "app.kestrel.browser.MainActivity"
DEFAULT_SCREEN = (600, 1024)


def say(*a):
    print("[%s +%4ds] %s" % (time.strftime("%H:%M:%S", time.gmtime()), time.time() - T0, " ".join(str(x) for x in a)), flush=True)


# ------------------------------------------------------------------------------------------------ the phone (real adb, or a fake one for --selftest)
class Adb:
    def __init__(self):
        self.bin = os.environ.get("ADB") or shutil.which("adb") or os.path.join(os.environ.get("ANDROID_HOME", "/opt/android-sdk"), "platform-tools", "adb")

    def run(self, *args, timeout=60, binary=False):
        try:
            r = subprocess.run([self.bin, *args], capture_output=True, timeout=timeout)
            return r.stdout if binary else r.stdout.decode("utf-8", "replace")
        except subprocess.TimeoutExpired:
            return b"" if binary else ""
        except Exception as e:  # adb missing, emulator gone ...
            say("adb failed:", args[:3], e)
            return b"" if binary else ""

    def sh(self, cmd, timeout=60):
        return self.run("shell", cmd, timeout=timeout)

    def popen_logcat(self, path):
        f = open(path, "wb")
        return subprocess.Popen([self.bin, "logcat", "-b", "main,crash,events", "-v", "threadtime"], stdout=f, stderr=subprocess.DEVNULL), f


class Phone:
    """What the driver does to the device. Every call is safe to repeat and never raises."""

    def __init__(self, adb, pkg):
        self.a, self.pkg = adb, pkg

    # ---- input
    def open_url(self, url):
        return self.a.sh("am start -a android.intent.action.VIEW -d %s -p %s" % (shlex.quote(url), self.pkg), timeout=30)

    def start_app(self):
        return self.a.sh("am start -n %s/%s" % (self.pkg, ACT), timeout=30)

    def debug(self, what):
        return self.a.sh("am start -n %s/%s --es kestrel_debug %s" % (self.pkg, ACT, what), timeout=30)

    def tap(self, x, y):
        self.a.sh("input tap %d %d" % (x, y), timeout=15)

    def swipe(self, x1, y1, x2, y2, ms=350):
        self.a.sh("input swipe %d %d %d %d %d" % (x1, y1, x2, y2, ms), timeout=15)

    def key(self, code):
        self.a.sh("input keyevent %s" % code, timeout=15)

    def home(self):
        self.key("KEYCODE_HOME")

    # ---- state
    def pids(self):
        out = self.a.sh("pidof %s" % self.pkg, timeout=15).strip()
        return [int(x) for x in out.split() if x.isdigit()]

    def procs(self):
        """All processes of the app (the engine's content process runs in its own), name -> pid."""
        out = self.a.sh("ps -A -o PID,NAME", timeout=15)
        res = {}
        for ln in out.splitlines():
            m = re.match(r"\s*(\d+)\s+(\S+)", ln)
            if m and m.group(2).startswith(self.pkg):
                res[m.group(2)] = int(m.group(1))
        return res

    def meminfo(self):
        """(app PSS in MB summed over its processes, MemAvailable in MB)."""
        out = self.a.sh("dumpsys meminfo %s" % self.pkg, timeout=45)
        # Android 10 prints "TOTAL PSS: n" once per process; older tables print "TOTAL n": count one of the two forms, never both
        nums = re.findall(r"TOTAL PSS:\s+(\d+)", out) or re.findall(r"^\s*TOTAL\s+(\d+)", out, re.M)
        pss = sum(int(n) for n in nums)
        avail = 0
        mi = self.a.sh("cat /proc/meminfo", timeout=15)
        m = re.search(r"MemAvailable:\s+(\d+) kB", mi)
        if m:
            avail = int(m.group(1)) // 1024
        return pss // 1024, avail

    def downloads(self):
        out = self.a.sh("ls -l /sdcard/Download/", timeout=20)
        files = {}
        for ln in out.splitlines():
            m = re.match(r"^\S+\s+\d+\s+\S+\s+\S+\s+(\d+)\s+\S+\s+\S+\s+(.+)$", ln.strip())
            if m:
                files[m.group(2).strip()] = int(m.group(1))
        return files

    def screenshot(self, path):
        data = self.a.run("exec-out", "screencap", "-p", timeout=30, binary=True)
        if data and data[:4] == b"\x89PNG":
            with open(path, "wb") as f:
                f.write(data)
            return True
        return False

    def ui_nodes(self):
        """The native views on screen (not the web page): [(text, content-desc, id, (x, y), clickable)]."""
        for _ in range(3):
            self.a.sh("uiautomator dump /sdcard/kestrel-ui.xml", timeout=40)
            xml = self.a.sh("cat /sdcard/kestrel-ui.xml", timeout=20)
            if "<hierarchy" in xml:
                break
            time.sleep(1.5)
        else:
            return []
        try:
            root = ET.fromstring(xml[xml.index("<?xml"):] if "<?xml" in xml else xml)
        except ET.ParseError:
            return []
        res = []
        for n in root.iter("node"):
            m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
            if not m:
                continue
            x1, y1, x2, y2 = (int(v) for v in m.groups())
            res.append((n.get("text", ""), n.get("content-desc", ""), n.get("resource-id", ""), ((x1 + x2) // 2, (y1 + y2) // 2), n.get("clickable") == "true"))
        return res

    def tap_text(self, pattern):
        rx = re.compile(pattern, re.I)
        for text, desc, rid, (x, y), clickable in self.ui_nodes():
            if rx.search(text) or rx.search(desc):
                self.tap(x, y)
                return text or desc
        return None

    def physical(self, ax, ay, az):
        """Turn the emulator's virtual accelerometer: (0, 9.81, 0) = held upright, (9.81, 0, 0) / (-9.81, 0, 0) = on its side."""
        return self.a.run("emu", "sensor", "set", "acceleration", "%s:%s:%s" % (ax, ay, az), timeout=20)

    def setting(self, ns, key, value):
        return self.a.sh("settings put %s %s %s" % (ns, key, value), timeout=15)

    def display_hint(self):
        out = self.a.sh("dumpsys display", timeout=30)
        m = re.search(r"mCurrentOrientation=(\d)", out) or re.search(r"rotation (\d)", out)
        return m.group(0) if m else "?"


# ------------------------------------------------------------------------------------------------ the log
HEALTH = re.compile(r"KestrelHealth\s*:\s*\+(?P<t>[\d.]+)s (?P<layer>\S+) (?P<ev>\S+)(?: (?P<rest>.*))?$")
KV = re.compile(r"(\w+)=(\S+)")
CRASH_PATTERNS = [
    (re.compile(r"FATAL EXCEPTION"), "java crash"),
    (re.compile(r"ANR in app\.kestrel"), "ANR"),
    (re.compile(r"Fatal signal \d+ \(SIG\w+\).*(?:app\.kestrel|GeckoMain|Gecko|Web Content|tab)"), "native crash"),
    (re.compile(r"am_crash.*app\.kestrel"), "am_crash"),
    (re.compile(r"am_anr.*app\.kestrel"), "am_anr"),
    (re.compile(r"Process app\.kestrel\S* \(pid \d+\) has died"), "process died"),
    (re.compile(r"am_proc_died.*app\.kestrel"), "am_proc_died"),
    (re.compile(r"am_kill.*app\.kestrel.*(?:lowmem|low mem|empty|cached|memory)", re.I), "killed for memory"),
]


class LogCap:
    def __init__(self, adb, path):
        self.adb, self.path, self.proc, self.f = adb, path, None, None

    def start(self):
        self.adb.run("logcat", "-G", "32M")
        self.adb.run("logcat", "-c")
        try:
            self.proc, self.f = self.adb.popen_logcat(self.path)
        except Exception as e:
            say("logcat could not start:", e)
            open(self.path, "wb").close()

    def size(self):
        try:
            return os.path.getsize(self.path)
        except OSError:
            return 0

    def since(self, mark):
        """The complete lines written after byte offset [mark]."""
        try:
            with open(self.path, "rb") as f:
                f.seek(mark)
                data = f.read()
        except OSError:
            return []
        return data.decode("utf-8", "replace").splitlines()

    def stop(self):
        try:
            if self.proc:
                self.proc.terminate()
            if self.f:
                self.f.close()
        except Exception:
            pass


def health_events(lines, layer=None, ev=None):
    out = []
    for ln in lines:
        m = HEALTH.search(ln)
        if not m:
            continue
        if layer and m.group("layer") != layer:
            continue
        if ev and m.group("ev") != ev:
            continue
        rest = m.group("rest") or ""
        out.append({"t": float(m.group("t")), "layer": m.group("layer"), "ev": m.group("ev"), "rest": rest, "kv": dict(KV.findall(rest))})
    return out


def crash_lines(lines):
    found = []
    for ln in lines:
        for rx, what in CRASH_PATTERNS:
            if rx.search(ln):
                found.append((what, ln.strip()[:240]))
                break
        else:
            m = HEALTH.search(ln)
            if m and m.group("layer") == "gecko" and m.group("ev") in ("onCrash", "onKill"):
                found.append(("engine " + m.group("ev"), ln.strip()[:240]))
    return found


def ms_of(v):
    """"1234ms" -> 1234, anything else -> -1."""
    v = v[:-2] if v.endswith("ms") else v
    return int(v) if v.lstrip("-").isdigit() else -1


def host_of(url):
    m = re.match(r"^[a-z]+://([^/:?#]+)", url, re.I)
    return (m.group(1).lower() if m else "")


def tail(host):
    """The part of a host that survives redirects between www. / m. / regional names: the last two labels."""
    p = host.split(".")
    return ".".join(p[-2:]) if len(p) >= 2 else host


def pctl(vals, p):
    if not vals:
        return None
    s = sorted(vals)
    return s[min(len(s) - 1, int(round((p / 100.0) * (len(s) - 1))))]


# ------------------------------------------------------------------------------------------------ the lab server's report.jsonl
class LabReport:
    def __init__(self, path):
        self.path = path

    def size(self):
        try:
            return os.path.getsize(self.path)
        except OSError:
            return 0

    def since(self, mark):
        try:
            with open(self.path, "rb") as f:
                f.seek(mark)
                data = f.read().decode("utf-8", "replace")
        except OSError:
            return []
        rows = []
        for ln in data.splitlines():
            try:
                rows.append(json.loads(ln))
            except ValueError:
                pass
        return rows


# ------------------------------------------------------------------------------------------------ the scenes
LAB_PAGES = [("lab-text", "/page/text", 6), ("lab-images", "/page/images", 6), ("lab-iframes", "/page/iframes", 2), ("lab-form", "/page/form", 0)]

# name, url, screens to scroll, seconds to stay.   Public sites that allow ordinary browsing; none of them is a streaming-piracy site. A site that blocks this runner is reported UPSTREAM.
PUBLIC_PAGES = [
    ("wikipedia", "https://en.wikipedia.org/wiki/Firefox", 6, 3),
    ("mdn-orientation", "https://developer.mozilla.org/en-US/docs/Web/API/Screen/orientation", 4, 3),
    ("hacker-news", "https://news.ycombinator.com/", 4, 2),
    ("npr-text", "https://text.npr.org/", 3, 2),
    ("bbc-news", "https://www.bbc.com/news", 5, 4),
    ("cnn-lite", "https://lite.cnn.com/", 3, 2),
    ("guardian", "https://www.theguardian.com/international", 5, 4),
    ("github-repo", "https://github.com/mozilla/gecko-dev", 4, 4),
    ("stackoverflow", "https://stackoverflow.com/questions/tagged/android", 4, 3),
    ("reddit", "https://www.reddit.com/r/android/", 4, 4),
    ("espn", "https://www.espn.com/", 5, 4),
    ("nytimes", "https://www.nytimes.com/", 4, 4),
    ("duckduckgo", "https://duckduckgo.com/?q=kestrel+browser+android", 2, 2),
    ("google-search", "https://www.google.com/search?q=android+16+release+date", 2, 2),
    ("youtube-home", "https://m.youtube.com/", 4, 6),
    ("twitch", "https://www.twitch.tv/", 3, 6),
    ("weather-wttr", "https://wttr.in/London?0", 1, 2),
    ("example", "https://example.com/", 0, 1),
]

# videos: lab clips (served by soaklab) and public files, all through the lab's player page so the same events are reported. (name, src, seconds to watch)
VIDEOS = [
    ("lab-landscape", "lab:/clip/landscape.mp4", 25),
    ("lab-portrait", "lab:/clip/portrait.mp4", 20),
    ("lab-720p", "lab:/clip/big.mp4", 25),
    ("mdn-flower-h264", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4", 20),
    ("bbb-360p-h264", "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4", 15),
    ("bbb-720p-h264", "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4", 15),
    ("bbb-long-h264", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", 40),
    ("wikimedia-vp9", "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/c0/Big_Buck_Bunny_4K.webm/Big_Buck_Bunny_4K.webm.360p.vp9.webm", 25),
    ("hls-mux", "https://test-streams.mux.dev/x36xhzz/x3zhzz.m3u8", 25),
    ("hls-apple", "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8", 25),
]

# real video sites: does the page play, does the app's "download video" finder see a video?  (name, url, seconds to watch)
VIDEO_PAGES = [
    ("youtube-short", "https://m.youtube.com/watch?v=jNQXAC9IVRw", 30),
    ("youtube-long", "https://m.youtube.com/watch?v=aqz-KE-bpKQ", 45),
    ("vimeo", "https://vimeo.com/22439234", 30),
    ("archive-org", "https://archive.org/details/BigBuckBunny_124", 30),
    ("wikimedia-commons", "https://commons.wikimedia.org/wiki/File:Big_Buck_Bunny_4K.webm", 25),
]

# Safe Browsing test pages (RX4-X3): Mozilla's are answered from the engine's built-in test tables (they prove the machinery is on), Google's from the real lists (they prove the list download
# and the key work). A page the engine blocks shows the app's warning screen: the Health line "page error ... kind=Dangerous".
SAFEBROWSING = [
    ("mozilla-phishing", "https://itisatrap.org/firefox/its-a-trap.html"),
    ("mozilla-malware", "https://itisatrap.org/firefox/its-an-attack.html"),
    ("mozilla-unwanted", "https://itisatrap.org/firefox/unwanted.html"),
    ("mozilla-harmful", "https://itisatrap.org/firefox/harmful.html"),
    ("google-phishing", "https://testsafebrowsing.appspot.com/s/phishing.html"),
    ("google-malware", "https://testsafebrowsing.appspot.com/s/malware.html"),
    ("google-unwanted", "https://testsafebrowsing.appspot.com/s/unwanted.html"),
]

# downloads: (name, url or lab path, expected bytes or 0 = just "something arrived", seconds to wait)
DOWNLOADS = [
    ("lab-1mb", "lab:/dl/1mb.bin", 1048576, 60),
    ("lab-5mb", "lab:/dl/5mb.bin", 5 * 1048576, 120),
    ("lab-20mb", "lab:/dl/20mb.bin", 20 * 1048576, 240),
    ("lab-slow-5mb", "lab:/dl/slow.bin", 5 * 1048576, 150),
    ("cloudflare-10mb", "https://speed.cloudflare.com/__down?bytes=10000000", 10000000, 180),
    ("ovh-10mb", "https://proof.ovh.net/files/10Mb.dat", 0, 180),   # size not asserted: any file that arrives
]


class Soak:
    def __init__(self, a):
        self.a = a
        self.out = a.out
        os.makedirs(os.path.join(self.out, "shots"), exist_ok=True)
        self.adb = FakeAdb(self) if a.selftest else Adb()
        self.ph = Phone(self.adb, a.pkg)
        self.log = LogCap(self.adb, os.path.join(self.out, "logcat.txt"))
        self.lab = LabReport(os.path.join(self.out, "lab", "report.jsonl"))
        self.rng = random.Random(a.seed)
        self.events = []        # one dict per scene
        self.issues = []        # (severity, area, text)
        self.samples = []       # (elapsed s, pss MB, MemAvailable MB, processes)
        self.seq = 0
        self.main_pid = None
        self.restarts = 0
        self.shots = 0
        self.screen = DEFAULT_SCREEN
        self.evf = open(os.path.join(self.out, "events.jsonl"), "w")
        self.port = a.port
        self.lab_base = "http://localhost:%d" % a.port
        self.fs_result = None
        self.sb_rounds = []

    # ---- bookkeeping
    def issue(self, sev, area, text, shot=False):
        self.issues.append((sev, area, text))
        say("ISSUE [%s] %s: %s" % (sev, area, text))
        if shot:
            self.shot(area)

    def shot(self, label):
        if self.shots >= 40:
            return
        self.shots += 1
        p = os.path.join(self.out, "shots", "%02d-%s.png" % (self.shots, re.sub(r"[^a-z0-9-]+", "-", label.lower())[:40]))
        self.ph.screenshot(p)

    def record(self, ev):
        ev["at"] = round(time.time() - T0, 1)
        self.events.append(ev)
        self.evf.write(json.dumps(ev) + "\n")
        self.evf.flush()
        say("scene %-18s %-9s %s" % (ev.get("name", "?"), ev.get("status", "?"), ev.get("note", "")))

    def url(self, u):
        return self.lab_base + u[4:] if u.startswith("lab:") else u

    def upstream(self, url):
        """Does the runner itself get an answer from this address? -> (state, note): "ok", "http-error" (the probe got a 4xx / 5xx: the site may only be blocking this probe, the app
        still tries) or "unreachable" (no answer at all: the scene is skipped, it is not an app fault)."""
        if url.startswith("lab:") or self.a.selftest:
            return "ok", ""
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Linux; Android 10; Mobile) Gecko/160.0 Firefox/160.0", "Range": "bytes=0-1023"})
            with urllib.request.urlopen(req, timeout=15) as r:
                return "ok", "http %d" % r.status
        except urllib.error.HTTPError as e:
            return ("ok", "http %d" % e.code) if e.code in (206, 416) else ("http-error", "http %d" % e.code)
        except Exception as e:
            return "unreachable", str(e)[:70]

    # ---- the app
    def check_alive(self, where):
        """True when the app's main process is the one we saw before. A new pid = it died and Android (or we) started it again."""
        pids = self.ph.pids()
        if not pids:
            self.issue("high", "crash", "the app is not running after %s" % where, shot=True)
            self.restarts += 1
            self.ph.start_app()
            time.sleep(8)
            self.main_pid = (self.ph.pids() or [None])[0]
            return False
        if self.main_pid is None:
            self.main_pid = pids[0]
        elif self.main_pid not in pids:
            self.issue("high", "restart", "the app's process changed from %s to %s after %s (it died and came back)" % (self.main_pid, pids[0], where))
            self.restarts += 1
            self.main_pid = pids[0]
            return False
        return True

    def sample(self):
        pss, avail = self.ph.meminfo()
        procs = self.ph.procs()
        self.samples.append((round(time.time() - T0), pss, avail, len(procs)))
        return pss, avail, len(procs)

    def scan_log(self, lines, scene):
        found = crash_lines(lines)
        for what, text in found:
            self.issue("high", "crash", "%s during %s: %s" % (what, scene, text), shot=False)
        stalls = [e for e in health_events(lines, "ui") if e["ev"] in ("STALL", "slow", "late-beat")]
        return found, stalls

    # ---- one page
    def load_page(self, name, url, scrolls, dwell, timeout=75, kind="page"):
        up_state, up_note = self.upstream(url)
        if up_state == "unreachable":
            self.record({"kind": kind, "name": name, "status": "upstream", "note": "the site does not answer from the runner (%s): not an app fault" % up_note})
            return None
        sev = "medium" if up_state == "ok" else "low"
        mark = self.log.size()
        t0 = time.time()
        self.ph.open_url(self.url(url))
        want = tail(host_of(self.url(url)))
        loaded = None
        errored = None
        end = t0 + timeout
        while time.time() < end:
            lines = self.log.since(mark)
            for e in health_events(lines, "page"):
                if e["ev"] == "loaded" and tail(e["kv"].get("host", "")) == want:
                    loaded = e
                if e["ev"] == "error" and tail(e["kv"].get("host", "")) == want:
                    errored = e
            if loaded or errored:
                break
            time.sleep(1.5)
        wall = time.time() - t0
        if not loaded and not errored:
            # a redirect to another domain still counts as a load: any "page loaded" line at all
            any_loaded = [e for e in health_events(self.log.since(mark), "page") if e["ev"] == "loaded"]
            if any_loaded:
                loaded = any_loaded[-1]
        ev = {"kind": kind, "name": name, "url_host": host_of(self.url(url)), "wall_s": round(wall, 1)}
        if errored:
            ev.update(status="error-page", note="the app showed an error page (%s)" % errored["rest"])
            self.issue(sev, "load", "%s: %s%s" % (name, ev["note"], "" if up_state == "ok" else " (the runner's own probe got %s: the site may be blocking data-centre addresses)" % up_note), shot=True)
        elif not loaded:
            ev.update(status="timeout", note="no 'page loaded' line within %d s" % timeout)
            self.issue(sev, "load", "%s did not finish loading within %d s%s" % (name, timeout, "" if up_state == "ok" else " (the runner's own probe got %s)" % up_note), shot=True)
        else:
            kv = loaded["kv"]
            paint, done = ms_of(kv.get("paint", "")), ms_of(kv.get("done", ""))
            ev.update(status="ok", paint_ms=paint, done_ms=done, blocked=int(kv.get("blocked", "0") or 0), visit=int(kv.get("visit", "1") or 1), ok=kv.get("ok"))
            if done > 25000:
                ev["status"] = "slow"
                ev["note"] = "load finished after %.1f s" % (done / 1000.0)
                self.issue("low", "speed", "%s was slow to finish loading (%.1f s, paint %s ms)" % (name, done / 1000.0, paint))
            elif paint > 12000:
                ev["status"] = "slow"
                ev["note"] = "first paint after %.1f s" % (paint / 1000.0)
                self.issue("low", "speed", "%s was slow to first paint (%.1f s)" % (name, paint / 1000.0))
        # scroll like a person
        if loaded or errored:
            time.sleep(dwell)
            for _ in range(scrolls):
                self.ph.swipe(300, 720, 300, 260, 380)
                time.sleep(0.9)
            for _ in range(min(2, scrolls)):
                self.ph.swipe(300, 300, 300, 760, 300)
                time.sleep(0.7)
        lines = self.log.since(mark)
        crashes, stalls = self.scan_log(lines, name)
        worst = max([int(re.sub(r"\D", "", e["rest"].split("ms")[0]) or 0) for e in stalls] or [0])
        ev["ui_stalls"] = len([s for s in stalls if s["ev"] == "STALL"])
        ev["ui_late"] = len(stalls)
        ev["ui_worst_ms"] = worst
        if ev["ui_stalls"]:
            self.issue("medium", "freeze", "%s: the UI thread was blocked %d time(s), worst %d ms" % (name, ev["ui_stalls"], worst))
        if crashes:
            ev["status"] = "crash"
        if not self.check_alive(name):
            ev["status"] = "crash"
            ev["note"] = (ev.get("note", "") + " the app died or restarted during this scene").strip()
        self.record(ev)
        return ev

    # ---- one video
    def play_video(self, name, src, watch, via_lab=True):
        up_state, up_note = self.upstream(src)
        if up_state == "unreachable":
            self.record({"kind": "video", "name": name, "status": "upstream", "note": "the file does not answer from the runner (%s): not an app fault" % up_note})
            return None
        self.seq += 1
        vid = "%s-%d" % (name, self.seq)
        url = "%s/video/%s?id=%d&src=%s" % (self.lab_base, name, self.seq, urllib.parse.quote(self.url(src), safe="")) if not src.startswith("lab:") else \
              "%s/video/%s?id=%d" % (self.lab_base, src.split("/")[-1].rsplit(".", 1)[0], self.seq)
        lmark, rmark = self.log.size(), self.lab.size()
        t0 = time.time()
        self.ph.open_url(url)
        playing_at = None
        end = t0 + 60 + watch
        rows = []
        while time.time() < end:
            rows = [r for r in self.lab.since(rmark) if r.get("kind") == "video" and r.get("name", "").endswith("#%d" % self.seq)]
            if playing_at is None and any(r.get("ev") == "playing" for r in rows):
                playing_at = time.time()
                end = min(end, playing_at + watch)
            if any(r.get("ev") == "error" for r in rows):
                break
            time.sleep(2)
        rows = [r for r in self.lab.since(rmark) if r.get("kind") == "video" and r.get("name", "").endswith("#%d" % self.seq)]
        evs = collections.Counter(r.get("ev") for r in rows)
        last = rows[-1] if rows else {}
        total = int(last.get("total", 0) or 0)
        dropped = int(last.get("dropped", 0) or 0)
        ct = float(last.get("ct", 0) or 0)
        ev = {"kind": "video", "name": name, "src_host": host_of(self.url(src)), "events": dict(evs), "wall_s": round(time.time() - t0, 1),
              "start_s": round(playing_at - t0, 1) if playing_at else None, "frames": total, "dropped": dropped, "corrupt": int(last.get("corrupt", 0) or 0),
              "played_s": ct, "picture": "%sx%s" % (last.get("w", "?"), last.get("h", "?"))}
        lines = self.log.since(lmark)
        heal = health_events(lines)
        ev["app_video_info"] = len([e for e in heal if e["layer"] == "video" and e["ev"] in ("info", "info-blob")])
        ev["app_found"] = max([int(m.group(1)) for e in heal if e["layer"] == "video" for m in [re.search(r"counted=(\d+)", e["rest"])] if m] or [0])
        err = next((r for r in rows if r.get("ev") == "error"), None)
        if err:
            ev.update(status="play-error", note="the player reported error %s" % err.get("err"))
            self.issue("medium", "video", "%s: the video element failed (%s)" % (name, err.get("err")), shot=True)
        elif not playing_at:
            rej = next((r for r in rows if r.get("ev") == "playrejected"), None)
            ev.update(status="no-play", note="never reached 'playing' within 60 s" + (" (play() rejected: %s)" % rej.get("why") if rej else ""))
            self.issue("medium", "video", "%s did not start playing" % name, shot=True)
        else:
            stall = evs.get("waiting", 0) + evs.get("stalled", 0)
            ev["stalls"] = stall
            ev["status"] = "ok"
            if total >= 60 and dropped * 100 // max(total, 1) >= 5:
                ev["status"] = "choppy"
                ev["note"] = "%d of %d frames dropped" % (dropped, total)
                self.issue("medium", "video", "%s dropped %d%% of its frames (%d of %d)" % (name, dropped * 100 // total, dropped, total))
            if stall >= 3:
                ev["status"] = "stalling"
                ev["note"] = (ev.get("note", "") + " %d times buffering" % stall).strip()
                self.issue("medium", "video", "%s stopped to buffer %d times in %d s" % (name, stall, watch))
            if playing_at and ct > 0 and ct < 0.6 * (time.time() - playing_at) and ev["status"] == "ok":
                ev["note"] = "played only %.0f s in %.0f s" % (ct, time.time() - playing_at)
                ev["status"] = "stalling"
                self.issue("low", "video", "%s: %s" % (name, ev["note"]))
        crashes, stalls = self.scan_log(lines, name)
        ev["ui_stalls"] = len([s for s in stalls if s["ev"] == "STALL"])
        if ev["ui_stalls"]:
            self.issue("medium", "freeze", "%s: the UI thread was blocked %d time(s) while the video played" % (name, ev["ui_stalls"]))
        if not self.check_alive(name):
            ev["status"] = "crash"
        self.record(ev)
        return ev

    # ---- a video site: play + "does the finder see it"
    def video_site(self, name, url, watch):
        ev = self.load_page(name, url, 0, 2, timeout=90, kind="videosite")
        if ev is None or ev.get("status") in ("timeout", "error-page", "crash"):
            return
        lmark = self.log.size()
        self.ph.tap(300, 330)          # the usual place of a player's big play button on a phone-width page
        time.sleep(watch)
        lines = self.log.since(lmark)
        heal = health_events(lines)
        playing = [e for e in heal if e["layer"] == "neon" and e["ev"] == "video" and "playing=true" in e["rest"]]
        found = max([int(m.group(1)) for e in heal if e["layer"] == "video" for m in [re.search(r"counted=(\d+)", e["rest"])] if m] or [0])
        r = {"kind": "videosite-play", "name": name, "status": "ok" if playing else "no-play", "app_saw_playing": bool(playing), "app_found_videos": found, "watched_s": watch}
        if not playing:
            r["note"] = "no playing state seen after a tap on the page centre (the page may need another tap, a consent, or blocks this runner)"
            self.issue("low", "video", "%s: the app never saw playback start after a tap" % name, shot=True)
        self.scan_log(lines, name)
        self.check_alive(name)
        self.record(r)

    # ---- Safe Browsing (RX4-X3)
    def safe_browsing(self, tag=""):
        rows = []
        for name, url in SAFEBROWSING:
            state, note = self.upstream(url)
            if state == "unreachable":
                rows.append((name, "UPSTREAM (%s)" % note)); continue
            mark = self.log.size()
            self.ph.open_url(url)
            t0, res = time.time(), "no answer in 45 s"
            while time.time() - t0 < 45:
                hs = health_events(self.log.since(mark), "page")
                if any(e["ev"] == "error" and "Dangerous" in e["kv"].get("kind", "") for e in hs):
                    res = "BLOCKED: the app showed its warning screen"; break
                if any(e["ev"] == "loaded" for e in hs):
                    time.sleep(4)
                    hs = health_events(self.log.since(mark), "page")
                    res = "BLOCKED: the app showed its warning screen" if any(e["ev"] == "error" and "Dangerous" in e["kv"].get("kind", "") for e in hs) else "NOT BLOCKED: the page loaded"
                    break
                time.sleep(2)
            rows.append((name, res))
        self.sb_rounds.append((tag or "t%d" % int((time.time() - T0) / 60), rows))
        self.record({"kind": "safebrowsing", "name": "safe-browsing", "status": "ok", "results": [{"page": n, "result": r} for n, r in rows]})
        for n, r in rows:
            if r.startswith("NOT BLOCKED"):
                self.issue("medium", "safe-browsing", "%s was not blocked (%s): the engine's own Safe Browsing is not protecting this page" % (n, tag or "this round"))
        self.check_alive("safe-browsing")

    # ---- a download
    def download(self, name, src, size, wait):
        up_state, up_note = self.upstream(src)
        if up_state == "unreachable":
            self.record({"kind": "download", "name": name, "status": "upstream", "note": "the file does not answer from the runner (%s)" % up_note})
            return
        before = self.ph.downloads()
        lmark = self.log.size()
        t0 = time.time()
        self.ph.open_url(self.url(src))
        tapped = None
        for _ in range(8):  # the "Download this file?" question (plain-http and risky files always ask)
            time.sleep(2)
            tapped = self.ph.tap_text(r"^download$")
            if tapped:
                break
            if self.ph.downloads() != before:
                break
        got = None
        end = t0 + wait
        while time.time() < end:
            now = self.ph.downloads()
            new = {k: v for k, v in now.items() if k not in before or before.get(k) != v}
            new = {k: v for k, v in new.items() if not k.startswith(".pending")}
            if new:
                k, v = max(new.items(), key=lambda kv: kv[1])
                if size and v >= size or (not size and time.time() - t0 > 12):
                    got = (k, v)
                    break
                got = (k, v)
            time.sleep(3)
        wall = time.time() - t0
        ev = {"kind": "download", "name": name, "wall_s": round(wall, 1), "asked": bool(tapped), "expected": size}
        if got is None:
            ev.update(status="dl-none", note="no new file in Downloads after %d s%s" % (wait, "" if tapped else " (and no question appeared)"))
            self.issue("medium", "download", "%s: nothing arrived" % name, shot=True)
        else:
            ev.update(file=got[0], size=got[1])
            if size and got[1] != size:
                ev.update(status="dl-incomplete", note="%d of %d bytes after %d s" % (got[1], size, wait))
                self.issue("medium", "download", "%s: only %d of %d bytes arrived" % (name, got[1], size))
            else:
                ev.update(status="ok", kbps=int(got[1] / 1024 / max(wall, 1)))
        lines = self.log.since(lmark)
        self.scan_log(lines, name)
        self.check_alive(name)
        self.record(ev)

    # ---- the full-screen turning (FS1)
    # where the lab's /fs page puts its buttons, as a fraction of the page area's height (see soaklab.page_fs)
    FS_GO, FS_LOCK, FS_UNLOCK, FS_EXIT = 0.255, 0.445, 0.635, 0.825

    def fullscreen_round(self, thorough=True):
        """Tap 'Go full screen' on the lab page and watch what the app does with the screen (FS1). The result is a list of checks, also written into the report."""
        steps = []
        self.ph.setting("system", "accelerometer_rotation", 0)  # Android's own rotation lock ON, like the owner's tablet
        self.ph.setting("system", "user_rotation", 0)
        self.ph.physical(0, 9.81, 0)
        rmark, lmark = self.lab.size(), self.log.size()
        self.ph.open_url(self.lab_base + "/fs")
        t0 = time.time()
        while time.time() - t0 < 40 and not [r for r in self.lab.since(rmark) if r.get("kind") == "fs" and r.get("ev") == "page"]:
            time.sleep(1.5)
        w, h = self.screen
        top, bottom = 25, h - 51 - 60  # status bar; nav bar + the app's dock

        def y_of(frac):
            return int(top + (bottom - top) * frac)

        def turns():
            return health_events(self.log.since(lmark), "fullscreen", "turn")

        def configs():
            return [e["kv"].get("orientation") for e in health_events(self.log.since(lmark), "life", "config")]

        def fs_rows():
            return [r for r in self.lab.since(rmark) if r.get("kind") == "fs"]

        # 1. full screen on an upright screen with a wide video: the screen must turn sideways by itself
        self.ph.tap(w // 2, y_of(self.FS_GO))
        time.sleep(4)
        rows = fs_rows()
        steps.append(("the page went full screen (the lab page saw fullscreenchange)", any(r.get("ev") == "fullscreenchange" and r.get("fs") == "1" for r in rows)))
        steps.append(("the app asked Android to turn the screen when full screen began ('turn' line)", any("full screen starts" in e["rest"] for e in turns())))
        steps.append(("the screen is landscape now (the wide video was turned sideways)", "landscape" in configs()))
        steps.append(("the page itself sees a wider-than-tall window", any(int(r.get("iw", 0) or 0) > int(r.get("ih", 0) or 0) for r in rows if r.get("ev") in ("resize", "fullscreenchange", "orientationchange"))))
        self.shot("fs-entered")
        if thorough:
            # 2. the tablet is turned physically (the emulator's virtual sensor) with Android's rotation lock still ON
            self.ph.physical(9.81, 0, 0); time.sleep(3)      # held on its side: the one-time turn is let go
            n0 = len(configs())
            self.ph.physical(0, 9.81, 0); time.sleep(4)      # held upright again: the picture must turn back
            steps.append(("holding the tablet upright again turned the screen back to portrait", "portrait" in configs()[n0:]))
            self.shot("fs-upright")
            n1 = len(configs())
            self.ph.physical(9.81, 0, 0); time.sleep(4)
            steps.append(("holding the tablet on its side turned the screen sideways again", "landscape" in configs()[n1:]))
            # 3. the rotate button: the tablet upright, the screen follows; tap shows the button, tap on the button turns the screen sideways
            self.ph.physical(0, 9.81, 0); time.sleep(4)
            n2 = len(configs())
            self.ph.tap(w // 2, h // 2); time.sleep(0.7)
            self.ph.tap(40, h // 2); time.sleep(3)
            steps.append(("the rotate button was pressed and asked for a turn ('rotate button' turn line)", any("rotate button" in e["rest"] for e in turns())))
            steps.append(("and the screen really turned (a new configuration line)", len(configs()) > n2))
            self.shot("fs-after-button")
        # 4. leaving full screen puts the screen back
        self.ph.key("KEYCODE_BACK")
        time.sleep(4)
        steps.append(("leaving full screen restored the orientation ('full screen ends' turn line)", any("full screen ends" in e["rest"] for e in turns())))
        cfg = configs()
        steps.append(("and the app is upright again with Android's rotation lock on", bool(cfg) and cfg[-1] == "portrait"))
        self.shot("fs-left")
        # 5. a page asks to lock the screen while it is NOT full screen: refused / ignored, the screen must not turn
        n3 = len(configs())
        self.ph.tap(w // 2, y_of(self.FS_LOCK)); time.sleep(3)
        steps.append(("a page's screen.orientation.lock outside full screen does not turn the screen", len(configs()) == n3))
        # 6. a page that locks to PORTRAIT as soon as it is full screen (a wide video!): the page's lock wins
        n4 = len(configs())
        self.ph.open_url(self.lab_base + "/fs?lock=portrait"); time.sleep(5)
        self.ph.tap(w // 2, y_of(self.FS_GO)); time.sleep(5)
        late = fs_rows()
        steps.append(("(info) the page's lock request answers: %s" % (",".join(sorted({r.get("ev") for r in late if str(r.get("ev", "")).startswith(("lock", "autolock"))})) or "none"), True))
        steps.append(("a page that locks to portrait in full screen keeps the screen upright in the end", (configs()[n4:] or ["portrait"])[-1] == "portrait"))
        self.ph.key("KEYCODE_BACK"); time.sleep(3)
        lines = self.log.since(lmark)
        crashes, _ = self.scan_log(lines, "fullscreen")
        alive = self.check_alive("fullscreen")
        for label, ok in steps:
            if not ok:
                self.issue("high" if ("turn" in label or "landscape" in label) else "medium", "fullscreen", "FAILED: " + label)
        self.fs_result = steps
        self.shot("fs-end")
        self.record({"kind": "fullscreen", "name": "fs-turning", "status": "ok" if all(ok for _, ok in steps) and alive and not crashes else "fs-failed",
                     "steps": [{"check": l, "ok": ok} for l, ok in steps], "turn_lines": [e["rest"][:160] for e in turns()][:14], "display": self.ph.display_hint()})

    # ---- the whole run
    def setup_device(self):
        for k in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
            self.ph.setting("global", k, 0)
        self.ph.a.sh("svc power stayon true")
        self.ph.key("KEYCODE_WAKEUP")
        self.ph.a.sh("wm dismiss-keyguard")
        self.ph.setting("system", "accelerometer_rotation", 0)
        self.ph.setting("system", "user_rotation", 0)
        size = self.ph.a.sh("wm size")
        m = re.search(r"(\d+)x(\d+)", size)
        if m:
            self.screen = (int(m.group(1)), int(m.group(2)))
        say("screen", self.screen, "| density", self.ph.a.sh("wm density").strip().replace("\n", " "), "| android", self.ph.a.sh("getprop ro.build.version.release").strip())

    def start_app(self):
        self.log.start()
        self.ph.a.sh("am force-stop %s" % self.a.pkg)
        time.sleep(2)
        t0 = time.time()
        self.ph.start_app()
        eng = None
        while time.time() - t0 < 300:
            if health_events(self.log.since(0), "engine", "runtime-ready"):
                eng = time.time() - t0
                break
            time.sleep(3)
        if eng is None:
            self.issue("high", "start", "the engine did not report 'runtime-ready' within 300 s of the app starting", shot=True)
        else:
            say("engine ready after %.0f s" % eng)
        time.sleep(5)
        self.main_pid = (self.ph.pids() or [None])[0]
        self.record({"kind": "start", "name": "app-start", "status": "ok" if eng else "no-engine", "engine_ready_s": round(eng, 1) if eng else None})

    def smoke(self):
        ev = self.load_page("lab-text", "lab:/page/text", 2, 1, timeout=60)
        self.safe_browsing("smoke")
        self.dump_diag("smoke")
        return ev

    def dump_diag(self, tag):
        self.ph.debug("dump")
        time.sleep(4)
        txt = self.adb.sh("run-as %s cat files/debug-diag.txt" % self.a.pkg, timeout=30)
        if txt and "Kestrel diagnostics" in txt:
            with open(os.path.join(self.out, "diagnostics-%s.txt" % tag), "w") as f:
                f.write(txt)
            return txt
        self.issue("low", "tooling", "the Diagnostics dump (%s) did not arrive" % tag)
        return ""

    def schedule(self, minutes):
        """The scene list for the soak, shuffled but fair: every kind appears again and again until the time is up."""
        pools = [("page", p) for p in PUBLIC_PAGES] * 1 + [("lab", p) for p in LAB_PAGES] * 2 + [("video", v) for v in VIDEOS] * 1 + [("vsite", v) for v in VIDEO_PAGES] * 1 + \
                [("dl", d) for d in DOWNLOADS] * 1 + [("fs", None)] * 2
        order = []
        while len(order) < 400:
            batch = list(pools)
            self.rng.shuffle(batch)
            order.extend(batch)
        return order

    def soak(self, minutes):
        deadline = time.time() + minutes * 60
        n = 0
        last_dump = time.time()
        last_sample = 0
        for kind, item in self.schedule(minutes):
            if time.time() > deadline:
                break
            n += 1
            try:
                if kind == "page":
                    self.load_page(item[0], item[1], item[2], item[3])
                elif kind == "lab":
                    self.load_page(item[0], "lab:" + item[1], item[2], 1, timeout=60, kind="labpage")
                elif kind == "video":
                    self.play_video(item[0], item[1], item[2])
                elif kind == "vsite":
                    self.video_site(item[0], item[1], item[2])
                elif kind == "dl":
                    self.download(item[0], item[1], item[2], item[3])
                elif kind == "fs":
                    self.fullscreen_round(thorough=False)
            except Exception as e:  # a bug in this script must never end the hour
                self.issue("low", "tooling", "scene %s %s raised %r" % (kind, item[0] if item else "", e))
            if n % 8 == 0:
                self.ph.debug("closetabs")
                time.sleep(3)
            if time.time() - last_sample > 90:
                pss, avail, procs = self.sample()
                last_sample = time.time()
                say("memory: app PSS %d MB, MemAvailable %d MB, %d app processes, scene %d, %.0f min left" % (pss, avail, procs, n, (deadline - time.time()) / 60))
            if time.time() - last_dump > 20 * 60:
                self.dump_diag("t%d" % int((time.time() - T0) / 60))
                self.safe_browsing()
                last_dump = time.time()
        self.sample()

    # ---- the report
    def report(self):
        ev = self.events
        loads = [e for e in ev if e.get("kind") in ("page", "labpage", "videosite") and e.get("status") in ("ok", "slow")]
        by_host = collections.defaultdict(list)
        for e in loads:
            by_host[e["name"]].append(e)
        vids = [e for e in ev if e.get("kind") == "video"]
        dls = [e for e in ev if e.get("kind") == "download"]
        counts = collections.Counter(e.get("status") for e in ev)
        L = []
        L.append("# Kestrel emulator lab: %s run" % self.a.suite)
        L.append("")
        L.append("Run: %s, %d scenes in %.0f min. Device: Android 10 emulator, %dx%d, package %s. **x86_64 + software GL is NOT the tablet: only behaviour counts, not absolute times.**" %
                 (time.strftime("%Y-%m-%d %H:%M UTC", time.gmtime()), len(ev), (time.time() - T0) / 60, self.screen[0], self.screen[1], self.a.pkg))
        L.append("")
        L.append("## Summary")
        L.append("")
        L.append("Scene results: " + ", ".join("%s %d" % (k, v) for k, v in sorted(counts.items(), key=lambda kv: -kv[1])))
        L.append("App restarts / crashes seen: %d. Issues logged: %d (high %d, medium %d, low %d)." % (self.restarts, len(self.issues), *[sum(1 for s, _, _ in self.issues if s == x) for x in ("high", "medium", "low")]))
        L.append("")
        if self.issues:
            L.append("## Issues (what to look at, most serious first)")
            L.append("")
            order = {"high": 0, "medium": 1, "low": 2}
            seen = set()
            for sev, area, text in sorted(self.issues, key=lambda i: order[i[0]]):
                key = (sev, area, text)
                if key in seen:
                    continue
                seen.add(key)
                L.append("- **%s / %s**: %s" % (sev, area, text))
            L.append("")
        if self.sb_rounds:
            L.append("## Safe Browsing (RX4-X3: does the engine's own protection work in this build?)")
            L.append("")
            L.append("| test page | " + " | ".join(t for t, _ in self.sb_rounds) + " |")
            L.append("| :- | " + " | ".join(":-" for _ in self.sb_rounds) + " |")
            for i, (name, _) in enumerate(SAFEBROWSING):
                L.append("| %s | " % name + " | ".join(next((r for n, r in rows if n == name), "-") for _, rows in self.sb_rounds) + " |")
            L.append("")
        if self.fs_result:
            L.append("## Full-screen turning (FS1)")
            L.append("")
            for label, ok in self.fs_result:
                L.append("- [%s] %s" % ("x" if ok else " ", label))
            L.append("")
        if by_host:
            L.append("## Page loads (ms; the same page again later shows what the cache saves)")
            L.append("")
            L.append("| page | loads | paint p50 | done p50 | done max | blocked p50 | UI stalls |")
            L.append("| :- | -: | -: | -: | -: | -: | -: |")
            for name, es in sorted(by_host.items(), key=lambda kv: -(pctl([e.get("done_ms", 0) for e in kv[1]], 50) or 0)):
                paints = [e["paint_ms"] for e in es if e.get("paint_ms", -1) >= 0]
                dones = [e["done_ms"] for e in es if e.get("done_ms", -1) >= 0]
                L.append("| %s | %d | %s | %s | %s | %s | %d |" % (name, len(es), pctl(paints, 50), pctl(dones, 50), max(dones) if dones else "-", pctl([e.get("blocked", 0) for e in es], 50), sum(e.get("ui_stalls", 0) for e in es)))
            L.append("")
        if vids:
            L.append("## Video")
            L.append("")
            L.append("| video | result | start s | picture | played s | frames | dropped | buffering | app found it | note |")
            L.append("| :- | :- | -: | :- | -: | -: | -: | -: | -: | :- |")
            for e in vids:
                L.append("| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |" % (e["name"], e["status"], e.get("start_s", "-"), e.get("picture", "-"), e.get("played_s", "-"), e.get("frames", "-"), e.get("dropped", "-"), e.get("stalls", "-"), e.get("app_found", "-"), e.get("note", "")))
            L.append("")
        vs = [e for e in ev if e.get("kind") == "videosite-play"]
        if vs:
            L.append("## Video sites (a tap on the page centre, then watch)")
            L.append("")
            for e in vs:
                L.append("- %s: %s (the app saw playback: %s, videos the finder counted: %s)%s" % (e["name"], e["status"], e.get("app_saw_playing"), e.get("app_found_videos"), " - " + e["note"] if e.get("note") else ""))
            L.append("")
        if dls:
            L.append("## Downloads")
            L.append("")
            L.append("| download | result | bytes | expected | seconds | KB/s | asked first | note |")
            L.append("| :- | :- | -: | -: | -: | -: | :- | :- |")
            for e in dls:
                L.append("| %s | %s | %s | %s | %s | %s | %s | %s |" % (e["name"], e["status"], e.get("size", "-"), e.get("expected", "-"), e.get("wall_s", "-"), e.get("kbps", "-"), e.get("asked"), e.get("note", "")))
            L.append("")
        if self.samples:
            L.append("## Memory over the run (every ~90 s)")
            L.append("")
            L.append("| elapsed s | app PSS MB | MemAvailable MB | app processes |")
            L.append("| -: | -: | -: | -: |")
            step = max(1, len(self.samples) // 25)
            for s in self.samples[::step]:
                L.append("| %d | %d | %d | %d |" % s)
            pss = [s[1] for s in self.samples if s[1]]
            if pss:
                L.append("")
                L.append("App PSS: first %d MB, last %d MB, max %d MB. Lowest MemAvailable: %d MB." % (pss[0], pss[-1], max(pss), min(s[2] for s in self.samples)))
            L.append("")
        other = [e for e in ev if e.get("kind") in ("start",)]
        for e in other:
            L.append("Engine ready %s s after the app started." % e.get("engine_ready_s"))
        L.append("")
        L.append("## What this run cannot say")
        L.append("")
        L.append("- Software GL and an x86 JIT: scrolling smoothness, GPU memory and absolute speeds are not the tablet's. Dropped-frame counts of the player page are the emulator's.")
        L.append("- Sites that block data-centre addresses show as UPSTREAM or as a consent / challenge page, not as app faults. A challenge page counts as a successful load here.")
        L.append("- Video decoding is software only (no MediaTek decoders): 'plays' and 'no errors' are meaningful, smoothness is not.")
        text = "\n".join(L)
        with open(os.path.join(self.out, "report.md"), "w") as f:
            f.write(text)
        with open(os.path.join(self.out, "summary.json"), "w") as f:
            json.dump({"scenes": len(ev), "counts": dict(counts), "issues": self.issues, "restarts": self.restarts}, f)
        return text


# ------------------------------------------------------------------------------------------------ a fake phone for --selftest (no emulator in the cloud sandbox)
class FakeAdb:
    """Answers like a phone: every VIEW intent 'loads' its host, the lab pages report 'playing', downloads grow. Enough to exercise the parsers and the control flow."""

    def __init__(self, soak):
        self.s = soak
        self.bin = "fake-adb"
        self.t = 0.0
        self.files = {}
        self.pid = 4242
        self.nodes_download = False
        self.cfg = "portrait"

    def _health(self, text):
        with open(self.s.log.path, "a") as f:
            f.write("11-15 06:00:00.000  %d  %d D KestrelHealth: +%.3fs %s\n" % (self.pid, self.pid, time.time() - T0, text))

    def _lab(self, row):
        os.makedirs(os.path.dirname(self.s.lab.path), exist_ok=True)
        row["srv"] = time.time()
        with open(self.s.lab.path, "a") as f:
            f.write(json.dumps(row) + "\n")

    def popen_logcat(self, path):
        open(path, "ab").close()
        return None, None

    def run(self, *args, timeout=60, binary=False):
        if args and args[0] == "shell":
            out = self.sh(args[1])
        elif args and args[0] == "emu":
            out = ""
            if "9.81:0:0" in args[-1] and "-" not in args[-1]:
                self.cfg = "landscape"; self._health("life config orientation=landscape fullscreen=true")
            elif args[-1].startswith("0:9.81"):
                if self.cfg != "portrait":
                    self.cfg = "portrait"; self._health("life config orientation=portrait fullscreen=true")
        else:
            out = ""
        return out.encode() if binary else out

    def sh(self, cmd, timeout=60):
        if cmd.startswith("am start") and "-d " in cmd:
            url = shlex.split(cmd)[shlex.split(cmd).index("-d") + 1]
            host = host_of(url)
            if "/dl/" in url or "speed.cloudflare" in url or "proof.ovh" in url:
                self.nodes_download = True
                self._health("page error host=%s kind=none" % host) if False else None
            elif "/video/" in url:
                q = dict(re.findall(r"[?&](\w+)=([^&]*)", url))
                name = url.split("/video/")[1].split("?")[0] + "#" + q.get("id", "0")
                for ev in ("page", "loadedmetadata", "playing", "tick", "tick"):
                    self._lab({"kind": "video", "name": name, "ev": ev, "w": 640, "h": 360, "ct": 12.0, "total": 300, "dropped": 3, "corrupt": 0})
                self._health("video info PLAYED el=1 role=main length=24s counted=1 of 1")
                self._health("page loaded host=%s paint=800ms done=1500ms blocked=0 visit=1 ok=true" % host)
            elif url.endswith("/fs"):
                self._lab({"kind": "fs", "ev": "page", "iw": 600, "ih": 886})
                self._health("page loaded host=%s paint=500ms done=900ms blocked=0 visit=1 ok=true" % host)
            else:
                self._health("page loaded host=%s paint=%dms done=%dms blocked=4 visit=1 ok=true" % (host, 700 + len(host) * 20, 1500 + len(host) * 60))
            return "Starting: Intent"
        if cmd.startswith("am start -n") and "kestrel_debug" in cmd:
            return "Starting"
        if cmd.startswith("am start -n"):
            self._health("engine runtime-ready")
            return "Starting"
        if cmd.startswith("input tap"):
            x, y = (int(v) for v in cmd.split()[2:4])
            if x < 80:  # the rotate button
                self._health("fullscreen turn to=7 (upright) was=6 why=the rotate button"); self._health("life config orientation=portrait fullscreen=true")
            elif 200 < y < 330 and self.cfg == "portrait":
                self._lab({"kind": "fs", "ev": "fullscreenchange", "fs": "1", "iw": 1024, "ih": 600, "ot": "landscape-primary"})
                self._health("fullscreen turn to=6 (sideways) was=-1 why=full screen starts"); self._health("life config orientation=landscape fullscreen=true"); self.cfg = "landscape"
            return ""
        if cmd.startswith("input keyevent KEYCODE_BACK"):
            self._health("fullscreen turn to=-1 (as Android had it) was=10 why=full screen ends: put back what Android had"); self._health("life config orientation=portrait fullscreen=false"); self.cfg = "portrait"
            return ""
        if cmd.startswith("pidof"):
            return str(self.pid)
        if cmd.startswith("ps -A"):
            return "  4242 %s\n  4300 %s:tab\n" % (self.s.a.pkg, self.s.a.pkg)
        if cmd.startswith("dumpsys meminfo"):
            return "** MEMINFO in pid 4242 **\n           TOTAL PSS:   412345            TOTAL RSS: 500000\n"
        if cmd.startswith("cat /proc/meminfo"):
            return "MemAvailable:     812345 kB\n"
        if cmd.startswith("ls -l /sdcard/Download"):
            if self.nodes_download and "download" in self.files:
                return "total 1\n-rw-rw---- 1 u0_a1 media_rw %d 2026-11-15 06:00 kestrel-lab-5mb.bin\n" % self.files["download"]
            return "total 0\n"
        if cmd.startswith("uiautomator dump"):
            self.files["ui"] = True
            return ""
        if cmd.startswith("cat /sdcard/kestrel-ui.xml"):
            if self.nodes_download:
                self.files["download"] = 5 * 1048576
                return "<?xml version='1.0'?><hierarchy><node text='Download' content-desc='' resource-id='' bounds='[100,500][300,560]' clickable='true'/></hierarchy>".replace("'", '"')
            return "<?xml version='1.0'?><hierarchy></hierarchy>"
        if cmd.startswith("run-as") and "debug-diag" in cmd:
            return "Kestrel diagnostics  fake\nPage loads: ..."
        if cmd.startswith("wm size"):
            return "Physical size: 600x1024"
        if cmd.startswith("dumpsys display"):
            return "mCurrentOrientation=1"
        return ""


# ------------------------------------------------------------------------------------------------ main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="soak-out")
    ap.add_argument("--suite", default="soak", choices=["smoke", "fs", "soak", "all"])
    ap.add_argument("--minutes", type=float, default=60)
    ap.add_argument("--pkg", default=PKG_DEFAULT)
    ap.add_argument("--port", type=int, default=8091)
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    if a.selftest:
        a.minutes, a.suite = 0.4, "all"
        _sleep = time.sleep
        time.sleep = lambda s: _sleep(min(s, 0.05))   # the fake phone needs no waiting
    s = Soak(a)
    say("suite %s, %.1f min, package %s" % (a.suite, a.minutes, a.pkg))
    try:
        s.setup_device()
        s.start_app()
        if a.suite in ("smoke", "all"):
            s.smoke()
        if a.suite in ("fs", "all"):
            s.fullscreen_round(thorough=True)
        if a.suite in ("soak", "all"):
            s.soak(a.minutes)
        s.dump_diag("end")
    except KeyboardInterrupt:
        pass
    except Exception as e:
        s.issue("high", "tooling", "the soak script itself failed: %r" % (e,))
    finally:
        s.log.stop()
        # the whole log is big: keep the lines that matter (the app's own Health lines, crashes, ANRs, kills) as a separate small file
        try:
            keep = [ln for ln in open(s.log.path, "rb").read().decode("utf-8", "replace").splitlines() if "KestrelHealth" in ln or any(rx.search(ln) for rx, _ in CRASH_PATTERNS)]
            with open(os.path.join(a.out, "health-lines.txt"), "w") as f:
                f.write("\n".join(keep[-6000:]))
        except OSError:
            pass
        text = s.report()
    print("\n" + "=" * 100 + "\n" + text + "\n" + "=" * 100, flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
