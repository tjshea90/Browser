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
import argparse, collections, itertools, json, tempfile, os, random, re, shlex, shutil, struct, subprocess, sys, threading, time, urllib.error, urllib.parse, urllib.request
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

    def debug(self, what, arg=None, pick=None):
        return self.a.sh("am start -n %s/%s --es kestrel_debug %s%s%s" % (self.pkg, ACT, what, (" --es kestrel_arg %s" % shlex.quote(arg)) if arg else "", (" --es kestrel_pick %s" % shlex.quote(pick)) if pick else ""), timeout=30)

    def debug_native(self, what, arg=None):
        """A debug command for the screen of Kestrel's own player that is in front (single top: the running screen gets it, no second one is made)."""
        return self.a.sh("am start --activity-single-top -n %s/app.kestrel.browser.NativePlayerActivity --es kestrel_debug %s%s" % (self.pkg, what, (" --es kestrel_arg %s" % shlex.quote(arg)) if arg else ""), timeout=30)

    def tap(self, x, y):
        self.a.sh("input tap %d %d" % (x, y), timeout=15)

    def swipe(self, x1, y1, x2, y2, ms=350):
        self.a.sh("input swipe %d %d %d %d %d" % (x1, y1, x2, y2, ms), timeout=15)

    def key(self, code):
        self.a.sh("input keyevent %s" % code, timeout=15)

    def screen_stats(self):
        """A cheap 'is this page blank or broken' reading of the raw framebuffer (no image library): over the page area, the share of the dominant colour, how many colours hold at least 1 % of the
        samples, and the share of neighbouring samples that differ (content has edges, a blank page has none)."""
        data = self.a.run("exec-out", "screencap", timeout=40, binary=True)
        if not data or len(data) < 1000:
            return None
        try:
            w, h, _fmt = struct.unpack("<III", data[:12])
        except struct.error:
            return None
        off = 12 if len(data) - 12 >= w * h * 4 else 16
        if w < 100 or h < 200 or len(data) - off < w * h * 4:
            return None
        step = 6
        rows = []
        for y in range(30, h - 120, step):
            base = off + y * w * 4
            rows.append([(data[base + x * 4] >> 4, data[base + x * 4 + 1] >> 4, data[base + x * 4 + 2] >> 4) for x in range(0, w, step)])
        flat = [c for r in rows for c in r]
        if not flat:
            return None
        cnt = collections.Counter(flat)
        dom = cnt.most_common(1)[0][1] / float(len(flat))
        diff = pairs = 0
        for r in rows:
            for a, b in zip(r, r[1:]):
                pairs += 1; diff += (a != b)
        for r0, r1 in zip(rows, rows[1:]):
            for a, b in zip(r0, r1):
                pairs += 1; diff += (a != b)
        return {"dominant": round(dom, 3), "colours": sum(1 for v in cnt.values() if v >= len(flat) * 0.01), "edges": round(diff / float(max(1, pairs)), 3)}

    def red_rows(self):
        """FSB1 (owner message 46): the lab page /fsbox draws a 6 px RED strip on the bottom edge of its full-screen box. The framebuffer is read raw: how many pixel rows have a long red run, where the first and the last
        are, and how tall the screen is -> (count, first_y, last_y, height); None = no picture. No strip at all = the bottom of the page is under a system bar."""
        data = self.a.run("exec-out", "screencap", timeout=40, binary=True)
        if not data or len(data) < 1000:
            return None
        try:
            w, h, _fmt = struct.unpack("<III", data[:12])
        except struct.error:
            return None
        off = 12 if len(data) - 12 >= w * h * 4 else 16
        if w < 100 or h < 200 or len(data) - off < w * h * 4:
            return None
        red = []
        for y in range(h):
            base = off + y * w * 4
            hit = n = 0
            for x in range(8, w - 8, 10):
                r, g, b = data[base + x * 4], data[base + x * 4 + 1], data[base + x * 4 + 2]
                n += 1
                if r > 55 and g < 0.3 * r and b < 0.3 * r:      # red, bright or dimmed: Kestrel's full-screen controls lay a dark scrim over the bottom edge for a few seconds after a turn (lab run 43778698760 read it as a stale page)
                    hit += 1
            if n and hit >= 0.7 * n:
                red.append(y)
        return (len(red), red[0] if red else -1, red[-1] if red else -1, h)

    def look(self):
        """ROT2 (owner message 48): one screencap read three ways -> {"red": (count, first, last), "dark": share of black pixels in the page area, "w": .., "h": ..} or None. The page area leaves out the status bar,
        the navigation bar (bottom, or the side on this emulator in landscape) and 8 px of edge. A turn's black cover = nearly all black and no red strip; a page laid out right = the red strip right at the bottom of the
        page area; anything else is a half-laid-out or stale page."""
        data = self.a.run("exec-out", "screencap", timeout=40, binary=True)
        if not data or len(data) < 1000:
            return None
        try:
            w, h, _fmt = struct.unpack("<III", data[:12])
        except struct.error:
            return None
        off = 12 if len(data) - 12 >= w * h * 4 else 16
        if w < 100 or h < 200 or len(data) - off < w * h * 4:
            return None
        red = []
        dark = seen = 0
        for y in range(h):
            base = off + y * w * 4
            hit = n = 0
            inarea = 30 <= y < h - 60
            for x in range(8, w - 8, 10):
                r, g, b = data[base + x * 4], data[base + x * 4 + 1], data[base + x * 4 + 2]
                n += 1
                if r > 55 and g < 0.3 * r and b < 0.3 * r:      # red, bright or dimmed: Kestrel's full-screen controls lay a dark scrim over the bottom edge for a few seconds after a turn (lab run 43778698760 read it as a stale page)
                    hit += 1
                if inarea and x < w - 60:
                    seen += 1
                    if r < 14 and g < 14 and b < 14:
                        dark += 1
            if n and hit >= 0.7 * n:
                red.append(y)
        return {"red": (len(red), red[0] if red else -1, red[-1] if red else -1), "dark": round(dark / float(max(1, seen)), 3), "w": w, "h": h}

    def start_hogs(self, n):
        """Busy loops on the emulator: its two cores are shared with the engine, so a turn's new size reaches the page late, as it does on the tablet (owner message 48). -> pids"""
        pids = []
        for _ in range(n):
            out = self.a.sh("nohup sh -c 'while :; do :; done' >/dev/null 2>&1 & echo $!", timeout=15)
            m = re.search(r"\d+", out or "")
            if m:
                pids.append(m.group(0))
        return pids

    def content_pids(self):
        """The engine's content process(es) (the page's main thread lives there): named <package>:sandboxed_tab... in `ps`."""
        out = self.a.sh("ps -A -o PID,NAME 2>/dev/null", timeout=15) or ""
        pids = []
        for ln in out.splitlines():
            m = re.match(r"\s*(\d+)\s+(\S+)", ln)
            if m and "sandboxed_tab" in m.group(2):
                pids.append(m.group(1))
        return pids

    def nice_of(self, pid):
        out = self.a.sh("ps -p %s -o NI 2>/dev/null" % pid, timeout=15) or ""
        m = re.search(r"-?\d+", out.replace("NI", ""))
        return int(m.group(0)) if m else None

    def set_nice(self, pids, value):
        """Give the process's main thread this nice value whichever way this renice reads its number (absolute or an increment): -> {pid: nice read back}."""
        got = {}
        for p in pids:
            cur = self.nice_of(p)
            if cur is None:
                continue
            self.a.sh("renice -n %d -p %s" % (value, p), timeout=15)
            now = self.nice_of(p)
            if now != value and cur is not None and now == cur + value:
                pass                                          # an increment: the result is what it is
            elif now != value and value == 0 and now is not None and now != 0:
                self.a.sh("renice -n %d -p %s" % (-now, p), timeout=15)   # an increment: take the difference back
                now = self.nice_of(p)
            got[p] = now
        return got

    def touch_dev(self):
        """(path, max_x, max_y) of the emulator's multi-touch device that reaches the main display (the LOWEST numbered: the emulator lists one per display)."""
        out = self.a.sh("getevent -p", timeout=30) or ""
        best = None
        for block in out.split("add device")[1:]:
            m = re.search(r"(/dev/input/event(\d+))", block)
            if m and "multi_touch" in block and (best is None or int(m.group(2)) < best[0]):
                best = (int(m.group(2)), m.group(1), block)
        if not best:
            return None
        mx = my = 32767
        for code, nm in (("0035", "x"), ("0036", "y")):
            mm = re.search(code + r"\s*:\s*value \d+, min \d+, max (\d+)", best[2])
            if mm:
                if nm == "x":
                    mx = int(mm.group(1))
                else:
                    my = int(mm.group(1))
        return best[1], mx, my

    def raw_taps(self, taps, screen, gap_s=0.09, hold_s=0.04):
        """Taps with an exact gap, written to the touch device in ONE shell: `adb shell input tap` costs 150-300 ms a call here, too slow for a double tap (VP1). taps = [(x, y), ...]; true = they were sent."""
        dev = self.touch_dev()
        if not dev:
            return False
        path, mx, my = dev
        sw, sh = screen
        def ev(t, c, v):
            return struct.pack("<qqHHi", 0, 0, t, c, v)
        chunks = []
        for i, (x, y) in enumerate(taps):
            rx, ry = int(round(x / float(sw) * mx)), int(round(y / float(sh) * my))
            chunks.append(ev(3, 0x2f, 0) + ev(3, 0x39, 50 + i) + ev(3, 0x35, rx) + ev(3, 0x36, ry) + ev(3, 0x3a, 100) + ev(0, 0, 0))   # finger down
            chunks.append(ev(3, 0x39, -1) + ev(0, 0, 0))                                                                              # finger up
        with tempfile.TemporaryDirectory() as td:
            f = os.path.join(td, "kp.bin")
            with open(f, "wb") as fh:
                fh.write(b"".join(chunks))
            self.a.run("push", f, "/data/local/tmp/kp.bin", timeout=30)
        cmds = []
        at = 0
        for idx, c in enumerate(chunks):
            n = len(c) // 24
            cmds.append("dd if=/data/local/tmp/kp.bin of=%s bs=24 skip=%d count=%d 2>/dev/null" % (path, at, n))
            cmds.append("sleep %s" % (hold_s if idx % 2 == 0 else gap_s))
            at += n
        cmds.append("rm -f /data/local/tmp/kp.bin")
        self.a.sh("; ".join(cmds), timeout=30)
        return True

    def hogs_running(self):
        """How many of our busy loops are alive (ps), so the report can say whether the emulator was really slowed down."""
        out = self.a.sh("ps -A -o PID,ARGS 2>/dev/null | grep -c 'while :; do :; done'", timeout=15)
        m = re.search(r"\d+", out or "")
        return max(0, int(m.group(0)) - 1) if m else 0        # (the grep itself matches its own line)

    def stop_hogs(self, pids):
        if pids:
            self.a.sh("kill %s" % " ".join(pids), timeout=15)

    def measure_on(self):
        self.debug("measure")

    def shields(self, mode, hosts):
        self.debug("shields", "%s|%s" % (mode, ",".join(hosts)))

    def clear_cache(self):
        self.debug("clearcache")

    def long_press(self, x, y, ms=900):
        self.a.sh("input swipe %d %d %d %d %d" % (x, y, x, y, ms), timeout=20)

    def dismiss_cling(self):
        """Android's one-time 'Viewing full screen / GOT IT' box sits over the app and takes the FIRST Back key (found in lab run 5: it looked like 'Back does not leave full screen'). Tap its button when it is there."""
        out = self.a.sh("dumpsys window windows", timeout=40)
        i = out.find("ImmersiveModeConfirmation")
        if i < 0:
            return False
        best = None
        for m in re.finditer(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", out[i:i + 4000]):
            l, t, r, b = (int(v) for v in m.groups())
            if 250 <= r - l <= 700 and 120 <= b - t <= 450:
                best = (l, t, r, b)
                break
        x, y = (best[2] - 70, best[3] - 40) if best else (413, 215)
        self.tap(x, y)
        say("dismissed Android's 'Viewing full screen' box at", x, y)
        time.sleep(1)
        return True

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
    # only the app's MAIN process (its name has no ":something" after the package): the helper processes (:media, :sandboxed_tab...) end normally when a tab closes or the decoder is idle (lab run 43740398387 counted three as crashes)
    (re.compile(r"am_proc_died: \[\d+,\d+,app\.kestrel[\w.]*,"), "am_proc_died"),
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
    for i, ln in enumerate(lines):
        for rx, what in CRASH_PATTERNS:
            if rx.search(ln):
                # "FATAL EXCEPTION" is printed for ANY process: com.android.phone dying with the system (DeadSystemException) and `svc` shell commands lit up lab run 43747605584; only the app's own counts
                if what == "java crash" and not any("Process: app.kestrel" in l2 for l2 in lines[i:i + 4]):
                    break
                found.append((what, ln.strip()[:240]))
                break
        else:
            m = HEALTH.search(ln)
            if m and m.group("layer") == "gecko" and m.group("ev") in ("onCrash", "onKill"):
                found.append(("engine " + m.group("ev"), ln.strip()[:240]))
    return found


def num(v, default=0):
    """A number from what a page reported (a missing browser property arrives as the text 'undefined')."""
    try:
        return int(float(v))
    except (TypeError, ValueError):
        return default


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


# ------------------------------------------------------------------------------------------------ what the engine says about a page (LAB2)
# The engine writes the script errors and warnings of every page to logcat (tag "Web Content": [JavaScript Error: "..." {file: "..." line: N}]). Messages of the engine's own scripts and of the built-in
# extension (moz-extension:, resource:, chrome:) and the engine's chatter (quirks mode, forced layout, bounce tracker, Safe Browsing) are not the page's.
CONSOLE_RX = re.compile(r'Web Content: \[JavaScript (Error|Warning): "(.*?)"(?: \{file: "(.*?)" line: (\d+)\})?')
NOISE_FILES = ("moz-extension:", "resource:", "chrome:", "about:")
NOISE_TEXT = ("Layout was forced", "Quirks Mode", "bounce tracker", "Safe Browsing", "Unable to preload", "Some cookies are misusing")


SLOW_NICE = 12   # ROT2 slow_turn: the nice value of the engine's content process while the busy loops run (19 = lowest priority; 12 makes its work about 20 times slower beside 3 loops on 2 cores)


def console_summary(lines):
    errs = warns = cors = csp = 0
    seen, first = set(), []
    for ln in lines:
        m = CONSOLE_RX.search(ln)
        if not m:
            continue
        kind, text, file_, _line = m.groups()
        if (file_ and file_.startswith(NOISE_FILES)) or any(n in text for n in NOISE_TEXT):
            continue
        if "Cross-Origin Request Blocked" in text:
            cors += 1
        if "Content-Security-Policy" in text or "Content Security Policy" in text:
            csp += 1
        if kind == "Error":
            errs += 1
            if text[:100] not in seen and len(first) < 3:
                seen.add(text[:100]); first.append(text[:140])
        else:
            warns += 1
    return {"errors": errs, "warnings": warns, "cors": cors, "csp": csp, "first": first}


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
    ("duckduckgo", "https://duckduckgo.com/?q=kestrel+browser+android", 2, 2),
    ("google-search", "https://www.google.com/search?q=android+16+release+date", 2, 2),
    ("weather-wttr", "https://wttr.in/London?0", 1, 2),
    ("example", "https://example.com/", 0, 1),
    ("yahoo-finance", "https://finance.yahoo.com/quote/TTWO/", 8, 4),   # the page of the owner's report (message 44): a video module with an ad that has no controls
    ("lite-youtube", "https://m.youtube.com/results?search_query=android+16", 2, 3),
]

# LAB2 (owner message 45): "web page compatibility for all sites and good speed". About a hundred well-known sites in the order that matters (the first ones are the ones a daily browser must get right),
# each loaded twice by the compat suite: Shields ON (the default) and Shields OFF for that site. A site that is worse with Shields on is a false positive of ours. (name, url, screens to scroll, seconds to stay)
COMPAT_SITES = [
    ("wikipedia-article", "https://en.wikipedia.org/wiki/Android_(operating_system)", 5, 2), ("google-search", "https://www.google.com/search?q=android+16+release+date", 2, 2),
    ("bing-search", "https://www.bing.com/search?q=android+16+release+date", 2, 2), ("duckduckgo", "https://duckduckgo.com/?q=kestrel+browser+android", 2, 2),
    ("yahoo-finance-ttwo", "https://finance.yahoo.com/quote/TTWO/", 6, 4), ("yahoo-finance-videos", "https://finance.yahoo.com/videos/", 3, 5), ("yahoo-home", "https://www.yahoo.com/", 4, 3),
    ("bbc-news", "https://www.bbc.com/news", 5, 3), ("guardian", "https://www.theguardian.com/international", 5, 3), ("npr", "https://www.npr.org/", 4, 3), ("cnn-lite", "https://lite.cnn.com/", 3, 1),
    ("apnews", "https://apnews.com/", 4, 3), ("aljazeera", "https://www.aljazeera.com/", 4, 3), ("cbsnews", "https://www.cbsnews.com/", 4, 3), ("nbcnews", "https://www.nbcnews.com/", 4, 3),
    ("abcnews", "https://abcnews.go.com/", 4, 3), ("usatoday", "https://www.usatoday.com/", 4, 3), ("foxnews", "https://www.foxnews.com/", 4, 3), ("latimes", "https://www.latimes.com/", 4, 3),
    ("huffpost", "https://www.huffpost.com/", 4, 3), ("politico", "https://www.politico.com/", 4, 3), ("axios", "https://www.axios.com/", 4, 3), ("dw", "https://www.dw.com/en/top-stories/s-9097", 3, 3),
    ("dailymail", "https://www.dailymail.co.uk/home/index.html", 4, 3), ("nypost", "https://nypost.com/", 4, 3), ("independent", "https://www.independent.co.uk/", 4, 3), ("mirror", "https://www.mirror.co.uk/", 4, 3),
    ("theverge", "https://www.theverge.com/", 4, 3), ("arstechnica", "https://arstechnica.com/", 4, 3), ("wired", "https://www.wired.com/", 4, 3), ("techcrunch", "https://techcrunch.com/", 4, 3),
    ("engadget", "https://www.engadget.com/", 4, 3), ("cnet", "https://www.cnet.com/", 4, 3), ("hacker-news", "https://news.ycombinator.com/", 3, 1), ("lobsters", "https://lobste.rs/", 3, 1),
    ("slashdot", "https://slashdot.org/", 3, 2), ("npr-text", "https://text.npr.org/", 3, 1), ("marketwatch", "https://www.marketwatch.com/", 4, 3), ("investopedia", "https://www.investopedia.com/", 4, 3),
    ("cnbc", "https://www.cnbc.com/", 4, 3), ("cbssports", "https://www.cbssports.com/", 4, 3), ("yahoo-sports", "https://sports.yahoo.com/", 4, 3), ("espn", "https://www.espn.com/", 4, 4),
    ("amazon", "https://www.amazon.com/", 4, 3), ("ebay", "https://www.ebay.com/", 4, 3), ("walmart", "https://www.walmart.com/", 4, 3), ("target", "https://www.target.com/", 4, 3),
    ("bestbuy", "https://www.bestbuy.com/", 4, 3), ("etsy", "https://www.etsy.com/", 4, 3), ("homedepot", "https://www.homedepot.com/", 4, 3), ("ikea", "https://www.ikea.com/us/en/", 4, 3),
    ("apple", "https://www.apple.com/", 4, 3), ("microsoft", "https://www.microsoft.com/en-us/", 3, 3), ("github-repo", "https://github.com/mozilla/gecko-dev", 4, 3),
    ("stackoverflow", "https://stackoverflow.com/questions/tagged/android", 4, 3), ("mdn", "https://developer.mozilla.org/en-US/docs/Web/API/Screen/orientation", 4, 3),
    ("w3schools", "https://www.w3schools.com/html/html_intro.asp", 3, 3), ("npmjs", "https://www.npmjs.com/package/react", 3, 3), ("pypi", "https://pypi.org/project/requests/", 3, 2),
    ("python-docs", "https://docs.python.org/3/tutorial/index.html", 3, 2), ("android-dev", "https://developer.android.com/guide", 3, 3), ("kotlinlang", "https://kotlinlang.org/docs/home.html", 3, 3),
    ("archive-org", "https://archive.org/", 3, 3), ("gutenberg", "https://www.gutenberg.org/", 3, 2), ("openstreetmap", "https://www.openstreetmap.org/#map=12/37.77/-122.42", 1, 6),
    ("weather-gov", "https://forecast.weather.gov/MapClick.php?lat=40.71&lon=-74.01", 3, 2), ("weather-com", "https://weather.com/", 3, 3), ("accuweather", "https://www.accuweather.com/", 3, 3),
    ("timeanddate", "https://www.timeanddate.com/", 3, 2), ("nasa", "https://www.nasa.gov/", 4, 3), ("imdb", "https://www.imdb.com/chart/top/", 4, 3), ("rottentomatoes", "https://www.rottentomatoes.com/", 4, 3),
    ("goodreads", "https://www.goodreads.com/", 3, 3), ("quora", "https://www.quora.com/", 3, 3), ("medium", "https://medium.com/", 3, 3), ("substack", "https://substack.com/", 3, 3),
    ("wordpress", "https://wordpress.com/", 3, 3), ("imgur", "https://imgur.com/", 3, 4), ("flickr", "https://www.flickr.com/explore", 3, 4), ("unsplash", "https://unsplash.com/", 3, 4),
    ("pexels", "https://www.pexels.com/", 3, 4), ("pinterest", "https://www.pinterest.com/", 3, 3), ("tumblr", "https://www.tumblr.com/explore/trending", 3, 3), ("x-twitter", "https://x.com/", 2, 3),
    ("facebook", "https://www.facebook.com/", 2, 3), ("vimeo", "https://vimeo.com/", 3, 4), ("dailymotion", "https://www.dailymotion.com/", 3, 4), ("soundcloud", "https://soundcloud.com/", 3, 4),
    ("bandcamp", "https://bandcamp.com/", 3, 3), ("youtube-results", "https://m.youtube.com/results?search_query=android+16", 2, 4), ("craigslist", "https://sfbay.craigslist.org/", 3, 2),
    ("indeed", "https://www.indeed.com/", 3, 3), ("zillow", "https://www.zillow.com/", 3, 3), ("tripadvisor", "https://www.tripadvisor.com/", 3, 3), ("booking", "https://www.booking.com/", 3, 4),
    ("expedia", "https://www.expedia.com/", 3, 3), ("airbnb", "https://www.airbnb.com/", 3, 4), ("yelp", "https://www.yelp.com/", 3, 3), ("msn", "https://www.msn.com/", 4, 4),
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

# the videos whose file is also saved through the app's "Save video" finder after they played (small ones only: the lab keeps the hour short)
SAVE_VIDEO = {"lab-landscape", "lab-portrait", "mdn-flower-h264", "bbb-360p-h264"}

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
        self.guard_rows = []
        self.apk_rows = []
        self.system_restarts = 0
        self.net_status = ""
        self.sys_pid = ""
        self.last_system_restart = 0.0
        self.consecutive_down = 0
        self.hard_restarts = 0
        self.emu_proc = None
        self.vbar_rows = []
        self.compat_sites_done = []
        self.snd_rows = []
        self.fsb_results = []
        self.slow_results = []
        self.kp_results = []
        self.np_results = []                   # ANS52i: Kestrel's own player (suite np)
        self.cp_results = []                   # ANS52g: the page's video format policy on the real engine (suite cp)
        self.cp_table = []                     # (label, row, mse, can, mc) of every case
        self.cp_line = ""
        self.hs_results = []                   # ANS52h: the picture size cap for HLS pages on the real engine (suite cp, second part)
        self.hs_table = []
        self.hs_line = ""
        self.ap_results = []                   # ANS53a / ANS53b: videos start only when pressed (suite ap)
        self.ap_table = []
        self.ap_line = ""
        self.kp_wake = []                      # (label, the wake-lock lines of `dumpsys power`) while the film plays in full screen and after it ended
        self.kp_ff = []                        # ANS52a: ms from each turn to the app window's first frame at the new size (kp scene)
        self.kp_pic_path = ""                  # ANS51b: how the film's picture size reached automatic turning (known at entry, or from the player's first report)
        self.yahoo_fs = None
        self.shields_mode = "on"
        self.quiet_issues = False
        self.last_diag = ""

    # ---- bookkeeping
    def issue(self, sev, area, text, shot=False):
        if self.quiet_issues and sev != "high":
            return                                       # the Shields-off pass of the compat suite: only the comparison matters, not a second list of the same slow sites
        self.issues.append((sev, area, text))
        say("ISSUE [%s] %s: %s" % (sev, area, text))
        if shot:
            self.shot(area)

    def shot(self, label):
        if self.shots >= (120 if self.a.suite == "fsy" else 40):        # fsy: about 60 pictures for the 6 box variants + the live Yahoo page + its turns (the old cap of 70 cut the Yahoo pictures off)
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

    # ---- the emulated phone itself
    def system_ok(self):
        return self.ph.a.sh("getprop sys.boot_completed", timeout=15).strip() == "1"

    def system_pid(self):
        out = self.ph.a.sh("pidof system_server", timeout=15).split()
        return out[0] if out else ""

    def wait_boot(self, limit=420):
        t0 = time.time()
        while time.time() - t0 < limit:
            if self.system_ok() and self.ph.a.sh("pm path android", timeout=20).strip().startswith("package:") and self.system_pid():
                return True
            time.sleep(5)
        return False

    def rewire(self):
        """What the lab put on the phone, again after a restart of the system (the settings survive, the reverse tunnel and the app do not)."""
        time.sleep(10)
        self.adb.run("reverse", "tcp:%d" % self.port, "tcp:%d" % self.port)
        for k in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
            self.ph.setting("global", k, 0)
        self.ph.a.sh("svc power stayon true")
        self.ph.key("KEYCODE_WAKEUP")
        self.ph.setting("system", "accelerometer_rotation", 0)
        if self.a.net == "hotspot":
            self.ph.a.run("emu", "network", "speed", "5000:5000"); self.ph.a.run("emu", "network", "delay", "none")
        self.sys_pid = self.system_pid()

    def hard_restart(self, where, quiet=False):
        """The system keeps dying (a boot loop under load, found in lab run 5: 20 minutes without the app): start the emulator again from a clean disk and install the app again."""
        if not self.a.emu_cmd:
            return False
        if not quiet:
            self.issue("high", "emulator", "the emulated phone could not recover after %s: the emulator was started again from a clean disk (a limit of the lab, not of the app)" % where)
        try:
            self.adb.run("emu", "kill", timeout=30)
        except Exception:
            pass
        time.sleep(10)
        try:
            subprocess.run("pkill -f qemu-system-x86_64", shell=True, timeout=20)
        except Exception:
            pass
        time.sleep(5)
        self.emu_proc = subprocess.Popen(self.a.emu_cmd, shell=True, stdout=open(os.path.join(self.out, "emulator-restart.txt"), "ab"), stderr=subprocess.STDOUT, start_new_session=True)
        self.adb.run("wait-for-device", timeout=300)
        if not self.wait_boot(420):
            return False
        if self.a.apk:
            self.adb.run("install", "-r", "-t", self.a.apk, timeout=300)
        self.rewire()
        self.ph.start_app()
        time.sleep(20)
        self.main_pid = (self.ph.pids() or [None])[0]
        self.hard_restarts += 1
        return True

    def recover_system(self, where):
        """Android's own system process died (the emulator was overloaded): wait for the phone to come back, put the lab's wiring back. Not an app fault, but it is written down."""
        self.system_restarts += 1
        self.issue("high", "emulator", "the emulated phone's system restarted during %s (system_server died: the emulator was overloaded; a limit of the lab, not of the app)" % where)
        self.adb.run("wait-for-device", timeout=300)
        ok = self.wait_boot(420)
        self.rewire()
        if ok:
            self.ph.start_app()
            time.sleep(20)
            self.main_pid = (self.ph.pids() or [None])[0]
        if not ok or not self.main_pid or self.system_restarts >= 2 and (time.time() - self.last_system_restart) < 600:
            self.last_system_restart = time.time()
            if self.hard_restart(where):
                return
        self.last_system_restart = time.time()

    # ---- the app
    def check_alive(self, where):
        """True when the app's main process is the one we saw before. A new pid = it died and Android (or we) started it again."""
        if not self.system_ok() or (self.sys_pid and self.system_pid() not in ("", self.sys_pid)):
            self.recover_system(where)
            return False
        pids = self.ph.pids()
        if not pids and self.consecutive_down >= 2:     # the app does not come back: not the app's doing any more, the phone is sick
            self.consecutive_down = 0
            self.recover_system(where + " (the app would not start again)")
            return False
        self.consecutive_down = self.consecutive_down + 1 if not pids else 0
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

    # ---- the app's questions ("Download this file?" ...): a DEBUG build logs each one and answers on request
    def wait_dialog(self, lmark, timeout=20):
        t0 = time.time()
        while time.time() - t0 < timeout:
            ev = health_events(self.log.since(lmark), "dialog", "shown")
            if ev:
                return ev[-1]["rest"]
            time.sleep(1)
        return None

    def answer(self, yes):
        self.ph.debug("answer", "yes" if yes else "no")
        time.sleep(1.5)

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
            if kind == "page":
                ev["screen"] = self.ph.screen_stats()        # the first screen as the person sees it (LAB2: blank or broken?)
            for _ in range(scrolls):
                self.ph.swipe(300, 720, 300, 260, 380)
                time.sleep(0.9)
            for _ in range(min(2, scrolls)):
                self.ph.swipe(300, 300, 300, 760, 300)
                time.sleep(0.7)
        lines = self.log.since(mark)
        if ev.get("paint_ms", 0) < 0:      # a fast page paints after its load ended: the app says so in a second line
            late = [e for e in health_events(lines, "page", "painted") if tail(e["kv"].get("host", "")) == want]
            if late:
                ev["paint_ms"] = ms_of(late[0]["kv"].get("paint", ""))
        # LAB2: what the page really has, what the engine said about it, how its scrolling went, what it cost
        if kind == "page":
            probe = [e for e in health_events(lines, "probe", "page")]
            ev["probe"] = probe[-1]["kv"] if probe else None
            ev["console"] = console_summary(lines)
            bursts = health_events(lines, "scroll", "burst")
            if bursts:
                ev["scroll"] = {"bursts": len(bursts), "frames": sum(int(b["kv"].get("n", 0) or 0) for b in bursts), "gaps50": sum(int(b["kv"].get("a", 0) or 0) for b in bursts),
                                "gaps100": sum(int(b["kv"].get("b", 0) or 0) for b in bursts), "worst_ms": max(int(b["kv"].get("mx", 0) or 0) for b in bursts)}
            try:
                ev["pss"] = self.sample()[0]
            except Exception:
                pass
            ev["shields"] = self.shields_mode
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
        total = num(last.get("total"))
        dropped = num(last.get("dropped"))
        ct = float(num(float(last.get("ct", 0) or 0) * 10)) / 10.0
        ev = {"kind": "video", "name": name, "src_host": host_of(self.url(src)), "events": dict(evs), "wall_s": round(time.time() - t0, 1),
              "start_s": round(playing_at - t0, 1) if playing_at else None, "frames": total, "dropped": dropped, "corrupt": num(last.get("corrupt")),
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
        if ev["status"] in ("ok", "choppy", "stalling") and src.lower().split("?")[0].endswith(".mp4") and name in SAVE_VIDEO:
            size = 0
            if src.startswith("lab:"):
                try: size = os.path.getsize(os.path.join(self.out, "lab", os.path.basename(src)))
                except OSError: size = 0
            self.found_video_download(name, size)
        return ev

    # ---- "Save video": what the app's finder offers on the page that just played a file, and does one tap on its first row really save it
    def found_video_download(self, name, expect_size=0):
        before, lmark = self.ph.downloads(), self.log.size()
        self.ph.debug("videodl")
        said = None
        for _ in range(10):
            time.sleep(2)
            ev = health_events(self.log.since(lmark), "lab", "videodl")
            if ev:
                said = ev[-1]["rest"]; break
        r = {"kind": "videodl", "name": name, "finder_said": said}
        if not said:
            r.update(status="no-answer", note="the app did not answer the lab's 'save the found video' request")
            self.issue("low", "tooling", "%s: no answer from the finder hook" % name)
        elif "pick=none" in said:
            r.update(status="not-found", note="the finder offered nothing although the page played this file")
            self.issue("medium", "finding-video", "%s: the app's 'Save video' finder found NOTHING on a page that played the file (%s)" % (name, said))
        else:
            tapped = self.wait_dialog(lmark, 12)
            if tapped:
                self.answer(True)
            got = None
            t0 = time.time()
            while time.time() - t0 < 120:
                now = self.ph.downloads()
                new = {k: v for k, v in now.items() if k not in before and "lab-found-video" in k}
                if new:
                    k, v = max(new.items(), key=lambda kv: kv[1])
                    got = (k, v)
                    if not expect_size or v >= expect_size:
                        break
                time.sleep(3)
            if got is None:
                r.update(status="dl-none", note="found it (%s) but the saved file never appeared" % said)
                self.issue("medium", "download-video", "%s: the finder found the video but saving it produced no file (%s)" % (name, said))
            elif expect_size and got[1] != expect_size:
                r.update(status="dl-incomplete", file=got[0], size=got[1], note="%d of %d bytes" % (got[1], expect_size))
                self.issue("medium", "download-video", "%s: the saved video has %d of %d bytes" % (name, got[1], expect_size))
            else:
                r.update(status="ok", file=got[0], size=got[1], asked=bool(tapped))
        self.record(r)
        return r

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

    # ---- RX4-X2: which downloads ask first, and what the question says
    def guard_downloads(self):
        w, h = self.screen
        rows = []
        for kind in ("auto-link", "auto-nav", "tap-link", "tap-nav"):
            time.sleep(9)                                 # nobody touches the screen for longer than the app's 8 s window
            before, lmark = self.ph.downloads(), self.log.size()
            self.ph.open_url("%s/dl/guard?kind=%s" % (self.lab_base, kind))
            t0 = time.time()
            while time.time() - t0 < 30 and not any(e["ev"] == "loaded" for e in health_events(self.log.since(lmark), "page")):
                time.sleep(1.5)
            if kind.startswith("tap"):
                time.sleep(1.5)
                self.ph.tap(w // 2, int(25 + (h - 51 - 60 - 25) * 0.5))
            text = self.wait_dialog(lmark, 20)
            seen = text is not None
            by_itself = bool(text) and "by itself" in text
            if seen:
                self.answer(False)                         # Cancel (also arms the "refuse quietly for a minute" rule)
            refused = any(e["ev"] == "auto-refused" for e in health_events(self.log.since(lmark), "download"))
            rows.append((kind, "asked, by itself wording" if seen and by_itself else ("asked (plain wording)" if seen else ("refused quietly" if refused else "no question"))))
            self.shot("guard-" + kind)
        expect = {"auto-link": "asked, by itself wording", "auto-nav": "asked, by itself wording", "tap-link": "asked (plain wording)", "tap-nav": "asked (plain wording)"}
        self.guard_rows = rows
        for kind, res in rows:
            if kind.startswith("auto") and res == "no question":
                self.issue("high", "download-guard", "%s: a download the page started by itself arrived with NO question" % kind)
            if kind.startswith("tap") and res.endswith("by itself wording"):
                self.issue("medium", "download-guard", "%s: the person tapped, yet the question says the page started it by itself" % kind)
        self.record({"kind": "guard", "name": "download-guard", "status": "ok", "results": [{"page": k, "result": r, "expected": expect[k]} for k, r in rows]})
        self.check_alive("download-guard")

    # ---- RX4-X1: what the Downloads list would do with an installer that says it is Kestrel (a copy of the app under test, as an .apk and disguised as a video) and with a plain video
    def apk_guard(self):
        pkg = self.a.pkg
        path = self.adb.sh("pm path %s" % pkg).strip().splitlines()[0].replace("package:", "").strip() if self.adb.sh("pm path %s" % pkg).strip() else ""
        rows = []
        if not path:
            self.issue("low", "tooling", "apk guard: could not find the installed APK")
            return
        self.adb.sh("run-as %s cp %s files/fake-update.apk" % (pkg, path), timeout=120)
        self.adb.sh("run-as %s cp %s files/holiday.mp4" % (pkg, path), timeout=120)
        self.adb.sh("run-as %s sh -c 'head -c 300000 /dev/urandom > files/plain.mp4'" % pkg, timeout=30)
        want = {"fake-update.apk": "verdict=REFUSE_LOOKALIKE", "holiday.mp4": "verdict=REFUSE_LOOKALIKE", "plain.mp4": "manifest=false"}
        for name, expect in want.items():
            lmark = self.log.size()
            self.ph.debug("apkcheck", name)
            got = ""
            for _ in range(15):
                time.sleep(2)
                ev = [e for e in health_events(self.log.since(lmark), "download", "apk-verdict") if ("file=%s " % name) in e["rest"] + " "]
                if ev:
                    got = ev[-1]["rest"]; break
            ok = expect in got
            rows.append((name, ok, got or "no answer"))
            if not ok:
                self.issue("high", "apk-guard", "%s: expected %s, the app said: %s" % (name, expect, got or "nothing"))
        self.apk_rows = rows
        self.adb.sh("run-as %s rm -f files/fake-update.apk files/holiday.mp4 files/plain.mp4" % pkg, timeout=30)
        self.record({"kind": "apkguard", "name": "apk-guard", "status": "ok" if all(r[1] for r in rows) else "apk-guard-failed", "results": [{"file": n, "ok": o, "said": g[:200]} for n, o, g in rows]})
        self.check_alive("apk-guard")

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
        text = self.wait_dialog(lmark, 12 if src.startswith("lab:") else 5)  # the "Download this file?" question (plain-http and risky files always ask)
        tapped = text
        if text:
            self.answer(True)
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
        self.ph.dismiss_cling()
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
        # 4. leaving full screen puts the screen back: the Back key first; when it does nothing, the app's own call (so the later scenes start clean) and the Back key is reported as a fault
        self.ph.key("KEYCODE_BACK")
        time.sleep(4)
        back_worked = any("full screen ends" in e["rest"] for e in turns())
        steps.append(("the Back key leaves full screen ('full screen ends' turn line)", back_worked))
        if not back_worked:
            self.ph.debug("exitfs")
            time.sleep(4)
            steps.append(("(fallback) the app's own exitFullScreen call leaves full screen", any("full screen ends" in e["rest"] for e in turns())))
            if not any("full screen ends" in e["rest"] for e in turns()):
                self.ph.a.sh("am force-stop %s" % self.a.pkg); time.sleep(3); self.ph.start_app(); time.sleep(12)
                self.main_pid = (self.ph.pids() or [None])[0]
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

    # ---- FSB1 / ROT1 / FSC1 (owner message 46): "the controls in full screen get covered up by the Android bottom navigation bar", "when I rotate the tablet from portrait to landscape and back, it is laggy and
    # glitchy", "I don't see any video player controls in full screen landscape mode". A player BOX (not the bare video) goes full screen on the lab page /fsbox, in a web component's shadow root (as finance.yahoo.com's
    # player) and as a plain box; the page's bottom edge carries a red strip. Run twice: the emulator as it is (the system hides its bars) and with the debug switch nohide (a tablet that KEEPS its bars, like the owner's:
    # screenshots 2026-11-15 showed both bars visible in full screen). The tablet is turned sideways and back with the virtual sensor; the strip must stay visible, the app must pause / cover / settle.
    def fsbox_wait(self, rmark, ev="page", limit=40):
        t0 = time.time()
        while time.time() - t0 < limit and not [r for r in self.lab.since(rmark) if r.get("kind") == "fsb" and r.get("ev") == ev]:
            time.sleep(1.5)

    def fullscreen_bars(self):
        w, h = self.screen
        self.ph.setting("system", "accelerometer_rotation", 0)    # Android's own rotation lock ON, like the owner's tablet: the app's full-sensor request must still turn the screen
        self.ph.setting("system", "user_rotation", 0)
        results = []
        for mode in ("keep", "hide"):                       # FB1: the app's Settings choice "System bars in full-screen video": Keep (the default) and Hide
            self.ph.a.sh("setprop debug.kestrel.nohide 0")
            self.ph.debug("bars", mode)
            time.sleep(2)
            for kind, nobar in (("shadow", False), ("light", False), ("shadow", True)):
                self.ph.physical(0, 9.81, 0)
                time.sleep(2)
                rmark, lmark = self.lab.size(), self.log.size()
                self.ph.open_url("%s/fsbox?kind=%s%s" % (self.lab_base, kind, "&nobar=1" if nobar else ""))
                self.fsbox_wait(rmark)
                time.sleep(2)
                self.ph.tap(w // 2, 140)                                  # Play and go full screen (a real tap: the user gesture both need)
                time.sleep(5)
                self.ph.dismiss_cling()
                frows = [r for r in self.lab.since(rmark) if r.get("kind") == "fsb"]
                entered = any(r.get("ev") == "fullscreenchange" and r.get("fs") == "1" for r in frows)
                tag = "%s-%s%s" % (mode, kind, "-nobar" if nobar else "")
                bar_shown = None
                if nobar and entered:                                      # FSC1: no controls of the page: a tap on the film brings up Kestrel's bar INSIDE the full-screen web component
                    tm = self.log.size()
                    self.ph.tap(h // 2, w // 2)                            # the screen is sideways now (the app turned the wide video): the middle of it
                    time.sleep(2.5)
                    lines_tm = self.log.since(tm)
                    # VP1: Kestrel's own full-screen controls (the app's native ones, the default) own the film now and the injected DOM bar draws nothing then: either one showing is the answer
                    bar_shown = bool([e for e in health_events(lines_tm, "videobar") if e["ev"] == "shown"]) or bool(health_events(lines_tm, "player", "shown"))
                    self.shot("fsb-%s-bar" % tag)
                states = []
                # the app turns a wide video sideways BY ITSELF when full screen begins (FS1), so the first state is landscape; then the tablet is held sideways (the one-time turn is let go), upright (a REAL turn to
                # portrait), sideways (a turn to landscape) and upright again
                for label, accel in (("began (sideways by itself)", None), ("held sideways", (9.81, 0, 0)), ("held upright: turn to portrait", (0, 9.81, 0)), ("held sideways: turn to landscape", (9.81, 0, 0)), ("held upright again: portrait", (0, 9.81, 0))):
                    lm = self.log.size()
                    if accel:
                        self.ph.physical(*accel)
                    time.sleep(0.5 if accel else 0.1)
                    self.shot("fsb-%s-%s-early" % (tag, label.split(":")[0].replace(" ", "-")[:18]))
                    time.sleep(4.0)
                    self.shot("fsb-%s-%s" % (tag, label.split(":")[0].replace(" ", "-")[:18]))
                    red = self.ph.red_rows()
                    lines = self.log.since(lm)
                    last = [r for r in self.lab.since(rmark) if r.get("kind") == "fsb"]
                    lastrow = last[-1] if last else {}
                    sizes = [e for e in health_events(lines, "gecko", "size")]
                    settled = [e["rest"] for e in health_events(lines, "fullscreen", "turn-settled")]
                    paused = health_events(lines, "fullscreen", "turn-pause")
                    nosess = health_events(lines, "fullscreen", "turn-no-session")
                    bars = [e["rest"][:110] for e in health_events(lines, "fullscreen", "bars")]
                    vps = [e["rest"][:120] for e in health_events(lines, "fullscreen", "viewport")]     # FB4: what the page told about its viewport and what the app did about it
                    nconf = len(health_events(lines, "life", "config"))                                  # ROT3: how many times the screen turned for this one move of the tablet (2 = a flash of the wrong orientation first)
                    states.append({"state": label, "red": red, "ih": lastrow.get("ih"), "iw": lastrow.get("iw"), "mb": lastrow.get("mb"), "size_events": len(sizes), "paused": len(paused), "no_session": len(nosess),
                                   "settled": settled[-1] if settled else "", "bars": bars[-1] if bars else "", "viewport": vps[-3:], "configs": nconf})
                lm = self.log.size()
                self.ph.key("KEYCODE_BACK")
                time.sleep(3)
                results.append({"mode": mode, "kind": kind, "nobar": nobar, "entered": entered, "bar_shown": bar_shown, "states": states})
                self.ph.physical(0, 9.81, 0)
            self.dump_diag("fsb-" + mode)                       # the app's own words about the bars after this mode (diagnostics-fsb-hidden.txt / -kept.txt)
        self.ph.a.sh("setprop debug.kestrel.nohide 0")
        self.ph.debug("bars", "keep")
        # verdicts: the red strip must be visible (>= 3 rows) in every state; with the bars KEPT (the default) it must end above the system bar (the navigation bar is 52 px tall), with the bars hidden it sits on the very edge
        ok = True
        for r in results:
            if not r["entered"]:
                self.issue("medium", "fullscreen-bars", "%s %s: the box did not go full screen (the lab page saw no fullscreenchange)" % (r["mode"], r["kind"]))
                ok = False
                continue
            for st in r["states"]:
                red = st["red"]
                if red is None:
                    continue
                count, first, lasty, height = red
                if st.get("configs", 0) >= 2:
                    self.issue("medium", "fullscreen-turn", "%s %s %s: the screen turned %d times for one move of the tablet (a flash of the wrong orientation before the right one)" % (r["mode"], r["kind"], st["state"], st["configs"]))
                if count < 3:
                    self.issue("high", "fullscreen-bars", "%s %s %s: the bottom edge of the full-screen page is NOT visible (no red strip): it is under a system bar" % (r["mode"], r["kind"], st["state"]), shot=True)
                    ok = False
                elif r["mode"] == "keep" and height > 700 and lasty >= height - 40:    # (portrait only: this emulator puts the navigation bar at the SIDE in landscape, so the page legitimately reaches the screen's bottom edge there; the owner's tablet keeps it at the bottom in both)
                    self.issue("high", "fullscreen-bars", "%s %s %s: the page reaches y=%d of %d: it still extends under the kept navigation bar" % (r["mode"], r["kind"], st["state"], lasty, height))
                    ok = False
            if r["nobar"] and r["bar_shown"] is False:
                self.issue("high", "full-screen-controls", "%s %s: a tap on the full-screen film did not bring up Kestrel's controls (neither the app's own nor the injected bar)" % (r["mode"], r["kind"]), shot=True)
                ok = False
        self.fsb_results = results
        self.record({"kind": "fsbars", "name": "fullscreen-bars", "status": "ok" if ok else "failed", "results": results})
        self.check_alive("fullscreen-bars")

    # ---- the owner's page itself (message 46): the live Yahoo Finance player taken full screen the way the owner did, then the tablet turned sideways and back. Kestrel's bar (a tap on the player) has the Full button
    # at a place worked out from the page: the player was measured at 190-507 css px down the /videos/ page (lab run 17:05), so the bar sits 10 css px above its bottom edge and the button is its last one.
    # Nothing here is asserted hard (a live page changes): pictures at every step, the app's own lines, and whether full screen began at all.
    def yahoo_fullscreen(self):
        w, h = self.screen
        d = 1.0625                                           # 170 dpi
        self.ph.a.sh("setprop debug.kestrel.nohide 0")
        self.ph.debug("bars", "keep"); time.sleep(2)
        self.ph.setting("system", "accelerometer_rotation", 0); self.ph.setting("system", "user_rotation", 0)
        self.ph.physical(0, 9.81, 0); time.sleep(2)
        lmark = self.log.size()
        entered = False
        how = "none"
        bar = []
        for attempt in range(3):
            if attempt:
                say("the live Yahoo page did not go full screen: loading it again (%d)" % (attempt + 1))
                self.ph.physical(0, 9.81, 0); time.sleep(2)
            self.ph.open_url("https://finance.yahoo.com/videos/")
            t0 = time.time()
            while time.time() - t0 < 90 and not any(e["ev"] in ("loaded", "error") for e in health_events(self.log.since(lmark), "page")):
                time.sleep(2)
            time.sleep(10)
            self.shot("yahoofs-page")
            self.ph.tap(w // 2, 25 + int(350 * d)); time.sleep(3)                       # the bar (or the page's own controls)
            bar = [e["ev"] for e in health_events(self.log.since(lmark), "videobar")]
            self.shot("yahoofs-tap")
            time.sleep(1.5); self.shot("yahoofs-tap2")                                   # (the page's own controls come and go: a second picture says which button is where)
            fm = self.log.size()
            if "shown" in bar:
                self.ph.tap(556, 25 + int(475 * d)); time.sleep(5)   # the bar's Full button: x 556 px lies inside it for a 532 and a 564 css px wide film (probe of the live page: the film is 532 wide at x 16)
                entered = bool(health_events(self.log.since(fm), "fullscreen", "turn"))
                how = "Kestrel's Full button"
            else:
                # the page's own player (Shaka) shows its controls after the tap: its full-screen button is the square icon at the TOP-LEFT of the film (headless probe of the live page, 42 css px right of and below the film's
                # corner). Where the film starts changes with the ads of the day, so a few heights are tried: a tap shows the controls, a second tap on the same place presses the button; a picture after each try.
                how = "the page's own full-screen button (top-left of the film), tried at several heights (Kestrel's bar: %s)" % (",".join(bar) or "nothing")
                def began(mark):
                    return bool(health_events(self.log.since(mark), "fullscreen", "turn") or health_events(self.log.since(mark), "fullscreen", "bars"))
                for i, (fx, fy) in enumerate(((62, 271), (62, 250), (62, 295), (62, 320))):
                    if entered:
                        break
                    for press in (1, 2):
                        fm = self.log.size()
                        self.ph.tap(fx, fy); time.sleep(2.5)
                        self.shot("yahoofs-try%d-%d" % (i + 1, press))
                        if began(fm):
                            entered = True
                            break
            if entered:
                break
        self.shot("yahoofs-full")
        states = []
        if entered:
            for label, accel in (("began (sideways by itself)", None), ("held sideways", (9.81, 0, 0)), ("held upright: turn to portrait", (0, 9.81, 0)), ("held sideways: turn to landscape", (9.81, 0, 0)), ("held upright again: portrait", (0, 9.81, 0))):
                lm = self.log.size()
                if accel:
                    self.ph.physical(*accel)
                time.sleep(0.5 if accel else 0.1); self.shot("yahoofs-%s-early" % label.split(":")[0].replace(" ", "-")[:18])
                time.sleep(4.0); self.shot("yahoofs-%s" % label.split(":")[0].replace(" ", "-")[:18])
                lines = self.log.since(lm)
                states.append({"state": label, "settled": [e["rest"] for e in health_events(lines, "fullscreen", "turn-settled")][-1:] or [""], "paused": len(health_events(lines, "fullscreen", "turn-pause")),
                               "sizes": len(health_events(lines, "gecko", "size")), "bars": [e["rest"][:100] for e in health_events(lines, "fullscreen", "bars")][-1:] or [""],
                               "viewport": [e["rest"][:120] for e in health_events(lines, "fullscreen", "viewport")][-3:]})
            self.ph.key("KEYCODE_BACK"); time.sleep(3)
        self.ph.physical(0, 9.81, 0)
        # VP1 / VP2: did Kestrel's own controls start on the live player, and how many of the page's own player controls did they put away (Health 'player away' lines: the number of elements)?
        yl = self.log.since(lmark)
        started = bool(health_events(yl, "player", "start"))
        away_n = []
        for e in health_events(yl, "player", "away"):
            mm = re.search(r"(\d+) element", e["rest"])
            if mm:
                away_n.append(int(mm.group(1)))
        if entered and not started:
            self.issue("high", "full-screen-controls", "the live Yahoo player went full screen but Kestrel's controls did not start", shot=True)
        elif entered and not any(n > 0 for n in away_n):
            self.issue("medium", "full-screen-controls", "Kestrel's controls started on the live Yahoo player but none of the page's own player controls were put away (away lines: %s)" % away_n, shot=True)
        self.yahoo_fs = {"entered": entered, "how": how, "states": states, "kp_started": started, "away": away_n}
        self.record({"kind": "yahoo", "name": "yahoo-fullscreen", "status": "ok", "note": "full screen %s (%s); %d turn(s) photographed" % ("began" if entered else "did NOT begin", how[:90], len(states))})
        self.check_alive("yahoo-fullscreen")

    # ---- ROT2 (owner message 48, two screenshots: after a turn the page keeps the layout of the OLD orientation for a few seconds): the engine of the tablet hands a turn's new size to the page late. Busy loops slow this
    # emulator's engine down the same way; each turn is watched with screen reads for 9 s (the red strip of /fsbox at the bottom of the page = laid out right; nearly all black without a strip = the app's cover; anything
    # else = a half-laid-out or stale page). Played twice: with the app's wait for the page (the new rule) and without it (the rule of alpha51 / 52: the cover lifts when the app's own view is still), so the run shows
    # the symptom AND that it is gone.
    def slow_turn(self):
        w, h = self.screen
        self.ph.setting("system", "accelerometer_rotation", 0); self.ph.setting("system", "user_rotation", 0)
        self.ph.a.sh("setprop debug.kestrel.nohide 0"); self.ph.debug("bars", "keep"); time.sleep(2)
        results = []
        for mode in ("on", "off"):
            self.ph.debug("turnwait", mode); time.sleep(2)
            self.ph.physical(0, 9.81, 0); time.sleep(2)
            rmark = self.lab.size()
            self.ph.open_url("%s/fsbox?kind=light" % self.lab_base)
            self.fsbox_wait(rmark); time.sleep(2)
            self.ph.tap(w // 2, 140); time.sleep(6); self.ph.dismiss_cling()
            entered = any(r.get("ev") == "fullscreenchange" and r.get("fs") == "1" for r in self.lab.since(rmark) if r.get("kind") == "fsb")
            turns = []
            nhogs = 0
            slowed = {}
            cpids = []
            if entered:
                hogs = self.ph.start_hogs(3)
                time.sleep(1)
                nhogs = self.ph.hogs_running()
                cpids = self.ph.content_pids()                 # the page's main thread gets a low priority beside the busy loops: its layout of a turn's new size then takes seconds, as on the tablet
                slowed = self.ph.set_nice(cpids, SLOW_NICE)
                try:
                    time.sleep(2)
                    for label, accel in (("sideways", (9.81, 0, 0)), ("upright", (0, 9.81, 0)), ("sideways again", (9.81, 0, 0)), ("upright again", (0, 9.81, 0))):
                        lm = self.log.size()
                        self.ph.physical(*accel)
                        t0 = time.time()
                        samples = []
                        shot_taken = False
                        while time.time() - t0 < (0.2 if self.a.selftest else 9.0):
                            lk = self.ph.look()
                            dt = round(time.time() - t0, 1)
                            if lk is None:
                                continue
                            cnt, _first, last = lk["red"]
                            if cnt >= 3 and last >= lk["h"] - 56:
                                cls = "right"
                            elif lk["dark"] >= 0.985 and cnt == 0:
                                cls = "cover"
                            else:
                                cls = "stale"
                                if not shot_taken:
                                    shot_taken = True
                                    self.shot("slowturn-%s-%s" % (mode, label.replace(" ", "-")))
                            samples.append((dt, cls, lk["w"], lk["h"], cnt, last))
                        lines = self.log.since(lm)
                        settled = [e["rest"] for e in health_events(lines, "fullscreen", "turn-settled")][-1:]
                        turns.append({"turn": label, "samples": samples, "settled": settled[0][:150] if settled else "", "nudges": len(health_events(lines, "fullscreen", "turn-nudge")),
                                      "viewport": [e["rest"][:100] for e in health_events(lines, "fullscreen", "viewport")][-3:]})
                finally:
                    self.ph.set_nice(cpids, 0)
                    self.ph.stop_hogs(hogs)
            self.ph.key("KEYCODE_BACK"); time.sleep(3)
            self.ph.physical(0, 9.81, 0); time.sleep(2)
            results.append({"mode": mode, "entered": entered, "turns": turns, "hogs": nhogs, "slowed": slowed})
        self.ph.debug("turnwait", "on")
        ok = True
        for r in results:
            if not r["entered"]:
                self.issue("medium", "slow-turn", "%s: the box did not go full screen" % r["mode"])
                ok = False
                continue
            for t in r["turns"]:
                classes = [c for _dt, c, *_rest in t["samples"]]
                stale_run = max([len(list(g)) for k, g in itertools.groupby(classes) if k == "stale"] or [0])
                if r["mode"] == "on":
                    if stale_run >= 2:
                        self.issue("high", "slow-turn", "with the wait for the page, the turn '%s' still showed a stale or half-laid-out page in %d screen reads in a row: %s" % (t["turn"], stale_run, " ".join("%.1f:%s" % (dt, c) for dt, c, *_ in t["samples"])))
                        ok = False
                    if classes and classes[-1] == "cover":
                        self.issue("high", "slow-turn", "with the wait for the page, the cover was still up 9 s after the turn '%s'" % t["turn"])
                        ok = False
        self.slow_results = results
        self.record({"kind": "slowturn", "name": "slow-turn", "status": "ok" if ok else "failed", "results": results})
        self.check_alive("slow-turn")

    # ---- VP1 (owner message 49): Kestrel's own full-screen controls on the real engine: a film with no controls of its own goes full screen (the app turns it sideways by itself), the controls show, hide by themselves,
    # toggle with a tap, a double tap seeks and plays / pauses, the screen is turned upright, the mute and exit buttons work. Pictures after every step; the app's own `player` lines and the film's own
    # events (the lab page reports seeked / pause / play and every click the PAGE gets) say what really happened.
    def kp_scene(self):
        w, h = self.screen
        checks = []
        def check(name, ok, detail=""):
            checks.append({"check": name, "ok": bool(ok), "detail": detail if not ok else ""})
            if not ok:
                self.issue("high", "player", "%s %s" % (name, detail))
        self.ph.setting("system", "accelerometer_rotation", 0); self.ph.setting("system", "user_rotation", 0)
        self.ph.a.sh("setprop debug.kestrel.nohide 0"); self.ph.debug("bars", "keep"); self.ph.debug("turnwait", "on"); time.sleep(2)
        self.ph.physical(0, 9.81, 0); time.sleep(2)
        rmark = self.lab.size()
        self.ph.open_url("%s/fsbox?kind=shadow&nobar=1&chrome=1" % self.lab_base)     # a film with no controls of its own, in a web component (like finance.yahoo.com's player), and a bar named like YouTube's chrome
        self.fsbox_wait(rmark); time.sleep(2)
        lm = self.log.size()
        self.ph.tap(w // 2, 140); time.sleep(3.2); self.ph.dismiss_cling()           # play and full screen; the wide film is turned sideways by the app
        ev = lambda layer, name, since=None: health_events(self.log.since(lm if since is None else since), layer, name)
        rows = lambda name: [r for r in self.lab.since(rmark) if r.get("kind") == "fsb" and r.get("ev") == name]
        def is_shown():                                                             # shown minus hidden lines since full screen began
            return len(ev("player", "shown")) > len(ev("player", "hidden"))
        def box():
            lk = self.ph.look()
            land = bool(lk and lk["w"] > lk["h"])
            return (973, 574, land) if land else (600, 947, land)
        def double_tap(x, y):                                                       # two `input tap` processes at once: their taps land a few tens of ms apart (one after the other costs 150-300 ms each, too slow)
            self.ph.a.sh("input tap %d %d & input tap %d %d & wait" % (x, y, x, y), timeout=30)
        def ensure_shown():                                                         # a tap on an empty place of the picture (away from every control) when they are hidden
            if is_shown():
                return
            vw, vh, land = box()
            self.ph.tap(int(vw * 0.25), 26 + int(vh * 0.2)); time.sleep(0.9)
        def wake(label):                                                            # ANS50 / research: does the screen stay on while a film plays? (the engine holds a wake lock itself: GeckoAppShell.setWakeLockState)
            out = self.ph.a.sh("dumpsys power | grep -i 'wake_lock\\|Wake Locks'", timeout=30) or ""
            self.kp_wake.append((label, [ln.strip()[:150] for ln in out.splitlines() if ln.strip()][:8]))
        check("a full-screen film got Kestrel's controls", bool(ev("player", "start")), "no 'player start' line")
        check("the controls show when full screen begins", bool(ev("player", "shown")), "no 'player shown' line")
        # ANS51b (owner message 51): the old video finder does not see a film inside a web component (the tablet's entry said 'video 0x0'), so automatic turning must get the size from the player's own first report: the
        # screen goes sideways by itself a moment after full screen began. Either the entry knew the size (a turn 'to=6' at once) or a 'film-picture' line and a 'picture became known' turn follow.
        t_end = time.time() + 10
        while time.time() < t_end and not any("became known" in e["rest"] or "to=6" in e["rest"] for e in ev("fullscreen", "turn")):
            time.sleep(0.5)
        turns = ev("fullscreen", "turn"); fpic = ev("fullscreen", "film-picture")
        entry0 = [e for e in turns if "why=full screen starts" in e["rest"] and "video 0x0" in e["rest"]]
        check("the wide film turned the screen sideways by itself", any("became known" in e["rest"] or "to=6" in e["rest"] for e in turns), "turn lines: %s" % [e["rest"][:90] for e in turns])
        check("a picture that was unknown at entry came from the player's report", (not entry0) or bool(fpic), "entry said video 0x0 and there is no 'film-picture' line (lines: %s)" % [e["rest"][:90] for e in turns])
        self.kp_pic_path = "unknown at entry, from the player's report: %s" % [e["rest"][:40] for e in fpic] if entry0 else "known at entry"
        self.shot("kp-1-entered")
        time.sleep(4.5)
        wake("the film plays in full screen")
        check("they hide by themselves", bool(ev("player", "hidden")), "no 'player hidden' 8 s after full screen began")
        self.shot("kp-2-hidden")
        vw, vh, land = box()
        ex, ey = int(vw * 0.25), 26 + int(vh * 0.2)                                  # an empty place (the middle belongs to the play button while the controls show)
        clicks0 = len(rows("page-click"))
        m = self.log.size(); self.ph.tap(ex, ey); time.sleep(1.4)
        check("a tap on the picture shows the controls", bool(ev("player", "tap", m)) and bool(ev("player", "shown", m)), "tap lines %d, shown lines %d" % (len(ev("player", "tap", m)), len(ev("player", "shown", m))))
        self.shot("kp-3-shown-by-tap")
        m = self.log.size(); self.ph.tap(ex, ey); time.sleep(1.4)
        check("the next tap hides them", bool(ev("player", "hidden", m)), "no 'player hidden' after the second tap")
        self.shot("kp-4-hidden-by-tap")
        check("the PAGE never got those taps", len(rows("page-click")) == clicks0, "%d clicks reached the page" % (len(rows("page-click")) - clicks0))
        # the buttons: with the controls up, pause by the big button, play again, mute
        ensure_shown()
        vw, vh, land = box()
        m = self.log.size(); self.ph.tap(vw // 2, 26 + vh // 2); time.sleep(1.2)
        check("the big button pauses (a command 'pause' and the film paused)", any('"pause"' in c["rest"] for c in ev("player", "cmd", m)) and len(rows("v-pause")) > 0, "commands %s" % [c["rest"][:20] for c in ev("player", "cmd", m)])
        self.shot("kp-5-paused-by-button")
        m = self.log.size(); self.ph.tap(vw // 2, 26 + vh // 2); time.sleep(1.2)
        check("and plays again", any('"play"' in c["rest"] for c in ev("player", "cmd", m)), "")
        step = 102                                                                   # the skip buttons sit 96 dp left and right of the middle (1 dp = 1.0625 px at 170 dpi)
        m = self.log.size(); self.ph.tap(vw // 2 + step, 26 + vh // 2); time.sleep(1.2)
        check("the 10 s button skips forward", any('"skip"' in c["rest"] and '"d":10' in c["rest"] for c in ev("player", "cmd", m)), "commands %s" % [c["rest"][:30] for c in ev("player", "cmd", m)])
        m = self.log.size(); self.ph.tap(vw // 2 - step, 26 + vh // 2); time.sleep(1.2)
        check("and the 10 s button back skips back", any('"d":-10' in c["rest"] for c in ev("player", "cmd", m)), "commands %s" % [c["rest"][:30] for c in ev("player", "cmd", m)])
        # the real turn to portrait (the sensor hand-back waits 650 ms)
        self.ph.physical(9.81, 0, 0); time.sleep(1.7)
        self.shot("kp-5b-landscape-controls")                                        # the controls come back about 0.7 s after the turn settled and stay 2.5 s
        time.sleep(1.3)
        self.ph.physical(0, 9.81, 0); time.sleep(3.0)
        vw, vh, land = box()
        check("portrait now", not land, "the screen is still sideways")
        self.shot("kp-6-portrait-after-turn")
        # ANS52a (owner message 52: after a turn the status bar icons stayed in the middle of the screen and the video was cropped at the old width for several moments): the engine's view is held at its old size for the first
        # frame of a turn, the cover and the bars are drawn at the new size at once, and the view gets its new size behind the cover. The emulator's engine is fast, so this proves the mechanism, not the tablet's seconds.
        ff_ms = []
        for e in ev("fullscreen", "turn-first-frame"):
            mm = re.search(r"new size (\d+) ms", e["rest"])
            if mm:
                ff_ms.append(int(mm.group(1)))
        holds = ev("fullscreen", "turn-hold"); rels = ev("fullscreen", "turn-hold-released")
        check("ANS52a: a turn holds the engine's view at its old size for the first frame and gives it the new size after that frame", bool(holds) and len(rels) >= len(holds), "holds %d, released %d" % (len(holds), len(rels)))
        check("ANS52a: the app's window drew its first frame at the new size within 3 s of every turn (times %s ms)" % ff_ms, bool(ff_ms) and max(ff_ms) < 3000, "first-frame lines: %s" % ff_ms)
        self.kp_ff = ff_ms
        # double taps (ANS53c, owner message 53: "zero delay"): the FIRST tap toggles the controls at once, the second tap on a side within 300 ms is the seek; the middle has no double-tap meaning (two toggles, no command)
        time.sleep(4.0)                                                              # (they hide by themselves)
        cx, cy = vw // 2, 26 + vh // 2
        m = self.log.size(); seeks0 = len(rows("v-seeked")); sh0 = len(ev("player", "shown", m)) + len(ev("player", "hidden", m))
        double_tap(int(vw * 0.85), cy - 120); time.sleep(1.6)
        cmds = ev("player", "cmd", m)
        check("a double tap on the right side skips 10 s forward", any('"skip"' in c["rest"] and '"d":10' in c["rest"] for c in cmds), "commands %s" % [c["rest"][:30] for c in cmds])
        check("the film really jumped (the page saw 'seeked')", len(rows("v-seeked")) > seeks0, "no seeked row")
        check("the double tap toggled the controls exactly once (its first tap, at once) and the second tap only seeked", len(ev("player", "shown", m)) + len(ev("player", "hidden", m)) - sh0 == 1, "toggles after the double tap: %d" % (len(ev("player", "shown", m)) + len(ev("player", "hidden", m)) - sh0))
        self.shot("kp-7-double-tap-right")
        m = self.log.size()
        double_tap(int(vw * 0.15), cy - 120); time.sleep(1.4)
        check("a double tap on the left side skips back", any('"d":-10' in c["rest"] for c in ev("player", "cmd", m)), "")
        m = self.log.size()
        sh1 = len(ev("player", "shown", m)) + len(ev("player", "hidden", m))
        double_tap(cx, cy - 200); time.sleep(1.4)
        mid_cmds = [c["rest"][:20] for c in ev("player", "cmd", m) if '"pause"' in c["rest"] or '"play"' in c["rest"] or '"skip"' in c["rest"]]
        check("two quick taps in the middle are two toggles and make no command (no pause by accident)", not mid_cmds and len(ev("player", "shown", m)) + len(ev("player", "hidden", m)) - sh1 == 2, "commands %s, toggles %d" % (mid_cmds, len(ev("player", "shown", m)) + len(ev("player", "hidden", m)) - sh1))
        taps_here = [e for e in ev("player", "tap") if "on the screen itself" in e["rest"]]
        ms_list = [int(mm.group(1)) for mm in (re.search(r"itself, (\d+) ms after", e["rest"]) for e in taps_here) if mm]
        check("ANS53c: taps on the picture are answered on the glass itself, within 150 ms of the finger lifting (the lab's times: %s)" % ms_list[-8:], bool(ms_list) and max(ms_list) <= 150, "no tap on the glass was logged" if not ms_list else "worst %d ms" % max(ms_list))
        # mute and exit
        ensure_shown()
        m = self.log.size(); self.ph.tap(vw - 40, 26 + vh - 40); time.sleep(1.2)
        check("the mute button sends 'mute'", any('"mute"' in c["rest"] for c in ev("player", "cmd", m)), "")
        self.shot("kp-8-after-mute")
        ensure_shown()
        m = self.log.size(); self.ph.tap(vw - 40, 26 + 41); time.sleep(3.0)
        check("the exit button leaves full screen", bool(health_events(self.log.since(m), "fullscreen", "turn")) or bool(ev("player", "stop", m)), "no 'full screen ends' / 'player stop' after the exit tap")
        self.shot("kp-9-after-exit")
        time.sleep(1.5); wake("after full screen ended (the film is paused)")
        self.ph.physical(0, 9.81, 0)
        # VP2: the page's own player controls (the lab page has a bar with YouTube's class name) are not shown while Kestrel's controls own the film, and are given back when full screen ends
        cr = [r for r in self.lab.since(rmark) if r.get("kind") == "fsb" and r.get("chrome")]
        inside = [r for r in cr if str(r.get("fs")) == "1" and str(r.get("ev", "")).startswith("v-")]
        check("the page's own player controls are put away while Kestrel's own the film", bool(inside) and all(r.get("chrome") == "none" for r in inside[-3:]), "rows (event, display): %s" % [(r.get("ev"), r.get("chrome")) for r in cr][-8:])
        outside = [r for r in cr if r.get("ev") == "fullscreenchange" and str(r.get("fs")) == "0"]
        check("and they are given back when full screen ends", bool(outside) and outside[-1].get("chrome") not in ("none", None), "rows (event, display): %s" % [(r.get("ev"), r.get("chrome")) for r in cr][-8:])
        # VP3 (owner message 49: "if I pull up the navigation bar, it covers the video controls"): the second pass hides the system bars (Settings "Hide"), pulls the navigation bar up over the film with a swipe from the
        # bottom edge, and the controls must move up by the bar's height (a Health line 'player inset ...' and a picture)
        self.ph.debug("bars", "hide"); time.sleep(2)
        rmark2 = self.lab.size()
        self.ph.open_url("%s/fsbox?kind=shadow&nobar=1" % self.lab_base)
        self.fsbox_wait(rmark2); time.sleep(2)
        m = self.log.size()
        lm = m                                                                       # (is_shown() counts the shown / hidden lines of THIS pass)
        self.ph.tap(w // 2, 140); time.sleep(2.5); self.ph.dismiss_cling()
        check("Hide mode: the controls show at full screen start", bool(ev("player", "shown", m)), "")
        # Android 10 does not tell an app that a transient navigation bar was pulled up (lab run 43776782679, picture kp-11), so the controls keep the bar's height free at the bottom the whole time the bars are hidden
        check("Hide mode: the controls keep the navigation bar's height free at the bottom", any("free at the bottom" in e["rest"] for e in ev("player", "inset", m)), "inset lines: %s" % [e["rest"][:60] for e in ev("player", "inset", m)])
        self.shot("kp-10-hide-mode")
        lk = self.ph.look()
        sw, sh = (lk["w"], lk["h"]) if lk else (w, h)
        ensure_shown()
        self.ph.tap(sw // 2, sh // 2); time.sleep(1.0)                               # the big button pauses: the controls stay up while the film is paused, so the picture shows them with the bar over the bottom
        self.ph.a.sh("input swipe %d %d %d %d 250" % (sw // 2, sh - 2, sw // 2, sh - 160), timeout=20); time.sleep(0.6)   # pull the navigation bar up
        self.shot("kp-11-navigation-bar-pulled-up")                                  # the picture shows whether the bottom row is clear of the bar
        self.ph.key("KEYCODE_BACK"); time.sleep(2)
        self.ph.debug("bars", "keep")
        self.kp_results = checks
        self.record({"kind": "player", "name": "kestrel-player", "status": "ok" if all(c["ok"] for c in checks) else "failed", "checks": checks})
        self.check_alive("kestrel-player")

    def np_scene(self, hls=False):
        """ANS52i (owner message 52, research/VIDEO_ACCELERATION.md plan item 6): Kestrel's own player. The lab's plain video page loads its clip, the app's sniffer finds the file, the lab command hands the best row to the player
        (the same path as the Save video sheet's second button), and the player is checked: it starts, a decoder starts, the first picture comes, the controls show and hide by taps, the big button and the 10 s buttons make
        commands that the player carries out (the film's position moves), Back ends it and the browser is back. The emulator has no MediaTek decoders: 'plays' and 'no errors' count, smoothness does not."""
        w, h = self.screen
        checks = []
        def check(name, ok, detail=""):
            name = ("HLS: " + name) if hls else name
            checks.append({"check": name, "ok": bool(ok), "detail": detail if not ok else ""})
            if not ok:
                self.issue("high", "native-player", "%s %s" % (name, detail))
        self.ph.setting("system", "accelerometer_rotation", 0); self.ph.setting("system", "user_rotation", 0)
        self.ph.a.sh("setprop debug.kestrel.nohide 0"); self.ph.debug("bars", "keep"); time.sleep(2)
        self.ph.physical(0, 9.81, 0); time.sleep(2)
        if not hls:
            self.ph.open_url("%s/video/landscape" % self.lab_base)                 # a page with the lab's clip (the sniffer sees the file when the page loads it)
            time.sleep(7)
        lm = self.log.size()
        ev = lambda layer, name, since=None: health_events(self.log.since(lm if since is None else since), layer, name)
        if hls:
            self.ph.debug("nativeurl", "%s/hls/film/master.m3u8" % self.lab_base)  # the HLS film of the lab server (an address of the lab server only: the intent refuses any other)
        else:
            self.ph.debug("nativeplay")
        t_end = time.time() + 25
        while time.time() < t_end and not ev("native", "first-picture"):
            time.sleep(1)
        time.sleep(1.5)
        self.shot("np-1-started")
        check("the best row of the page was handed to Kestrel's own player" if not hls else "the HLS master list was handed to Kestrel's own player", (hls or bool(ev("lab", "nativeplay"))) and bool(ev("native", "start")), "lab lines %s, native start lines %d" % ([e["rest"][:80] for e in ev("lab", "nativeplay")], len(ev("native", "start"))))
        check("a video decoder started", bool(ev("native", "decoder")), "no 'native decoder' line (%s)" % [e["rest"][:60] for e in ev("native", "error")])
        check("the first picture came", bool(ev("native", "first-picture")), "no 'native first-picture' line")
        check("the controls showed when it started", bool(ev("native", "shown")), "no 'native shown' line")
        lk = self.ph.look()
        land = bool(lk and lk["w"] > lk["h"])
        vw, vh = (973, 574) if land else (600, 947)
        # ANS53f (owner message 53: "my tablet was being held in portrait ... It should have played in portrait"): the tablet is held upright, the film is wide: the screen STAYS upright (no forced landscape)
        check("ANS53f: held upright, the screen stays upright: a wide film does not turn it sideways by itself", not land, "the screen is %s" % (lk,))
        time.sleep(4.5)
        check("the controls hide by themselves", bool(ev("native", "hidden")), "no 'native hidden' line after 6 s")
        ex, ey = int(vw * 0.25), 26 + int(vh * 0.2)
        m = self.log.size(); self.ph.tap(ex, ey); time.sleep(1.4)
        check("a tap on the picture shows the controls", bool(ev("native", "shown", m)), "no 'native shown' after the tap")
        self.shot("np-2-controls")
        m = self.log.size(); self.ph.tap(ex, ey); time.sleep(1.4)
        check("the next tap hides them", bool(ev("native", "hidden", m)), "no 'native hidden' after the second tap")
        self.ph.tap(ex, ey); time.sleep(1.2)                                       # shown again for the buttons
        cx, cy = vw // 2, 26 + vh // 2
        m = self.log.size(); self.ph.tap(cx, cy); time.sleep(1.5)
        check("the big button pauses (a command 'pause' reached the player)", any('"pause"' in c["rest"] for c in ev("native", "cmd", m)), "commands %s" % [c["rest"][:30] for c in ev("native", "cmd", m)])
        m = self.log.size(); self.ph.tap(cx, cy); time.sleep(1.5)
        check("and plays again", any('"play"' in c["rest"] for c in ev("native", "cmd", m)), "commands %s" % [c["rest"][:30] for c in ev("native", "cmd", m)])
        m = self.log.size(); self.ph.tap(cx + 102, cy); time.sleep(1.5)
        check("the 10 s button skips forward", any('"skip"' in c["rest"] and '"d":10' in c["rest"] for c in ev("native", "cmd", m)), "commands %s" % [c["rest"][:40] for c in ev("native", "cmd", m)])
        self.shot("np-3-after-skip")
        # ANS53f: the screen follows the tablet, and the rotate button turns it for good (the lab drives the button's logic with the debug command: the button's place is in the screenshot)
        self.ph.physical(9.81, 0, 0); time.sleep(4)
        lk2 = self.ph.look()
        check("ANS53f: turning the tablet sideways turns the picture with it", bool(lk2 and lk2["w"] > lk2["h"]), "the screen is %s" % (lk2,))
        self.shot("np-4-tablet-sideways")
        self.ph.physical(0, 9.81, 0); time.sleep(4)
        lk3 = self.ph.look()
        check("ANS53f: and turning it upright again turns it back", bool(lk3 and lk3["h"] > lk3["w"]), "the screen is %s" % (lk3,))
        m = self.log.size(); self.ph.debug_native("rotate"); time.sleep(4)
        lk4 = self.ph.look()
        check("ANS53f: the rotate button turns the screen the other way although the tablet is held upright", bool(ev("native", "rotate", m)) and bool(lk4 and lk4["w"] > lk4["h"]), "the screen is %s, rotate lines %d" % (lk4, len(ev("native", "rotate", m))))
        self.shot("np-5-rotate-button")
        m = self.log.size(); self.ph.debug_native("rotate"); time.sleep(4)
        lk5 = self.ph.look()
        check("ANS53f: and pressed again it turns back", bool(lk5 and lk5["h"] > lk5["w"]), "the screen is %s" % (lk5,))
        if hls:
            # ANS53d: the master list names two pictures; the quality list offers both and the pick is carried out and remembered
            tracks = ev("native", "tracks")
            check("ANS53d: the player lists the two pictures the stream offers (480p class and 180p class)", any("2 picture(s) on offer" in t["rest"] for t in tracks), "tracks lines: %s" % [t["rest"][:100] for t in tracks])
            m = self.log.size(); self.ph.debug_native("qopen"); time.sleep(1.5)
            check("ANS53d: the quality list opens with Automatic and the two pictures", any("3 row(s)" in t["rest"] for t in ev("native", "quality-open", m)), "lines: %s" % [t["rest"][:60] for t in ev("native", "quality-open", m)])
            self.shot("np-6-quality-list")
            m = self.log.size(); self.ph.debug_native("qpick", "2"); time.sleep(3)
            check("ANS53d: picking the smaller picture is carried out", any("320x180" in q["rest"] or "180p" in q["rest"] for q in ev("native", "quality", m)), "lines: %s" % [q["rest"][:60] for q in ev("native", "quality", m)])
            m = self.log.size(); self.ph.debug_native("qpick", "0"); time.sleep(2)
            check("ANS53d: and Automatic gives the choice back", any("Automatic" in q["rest"] for q in ev("native", "quality", m)), "lines: %s" % [q["rest"][:60] for q in ev("native", "quality", m)])
        m = self.log.size(); self.ph.key("KEYCODE_BACK"); time.sleep(3)
        check("Back ends Kestrel's own player", bool(ev("native", "stop", m)), "no 'native stop' line")
        if hls:
            # ANS53d.2 (owner message 53: a quality button in the page's own controls): the page's list hands the film over with a pick; the lab drives the same launch (an address of the lab server + the pick as text)
            m = self.log.size()
            self.ph.debug("nativeurl", "%s/hls/film/master.m3u8" % self.lab_base, pick="320,180,0,0")
            t_end = time.time() + 25
            while time.time() < t_end and not ev("native", "pick-applied", m):
                time.sleep(1)
            sq = ev("native", "start-quality", m)
            check("ANS53d.2: a pick made in the page's list reaches Kestrel's own player (the cap follows it)", any("picked in the page's list" in q["rest"] and "cap 180p" in q["rest"] for q in sq), "lines: %s" % [q["rest"][:90] for q in sq])
            pa = ev("native", "pick-applied", m)
            check("ANS53d.2: the picture it names is found among the stream's pictures and played", any("180p -> 180p" in q["rest"] for q in pa), "lines: %s" % [q["rest"][:90] for q in pa])
            self.shot("np-7-handover-pick")
            m = self.log.size(); self.ph.key("KEYCODE_BACK"); time.sleep(3)
            check("ANS53d.2: Back ends that player too", bool(ev("native", "stop", m)), "no 'native stop' line")
        self.dump_diag("np-hls" if hls else "np")
        diag = ""
        try:
            diag = open(os.path.join(self.out, "diagnostics-np-hls.txt" if hls else "diagnostics-np.txt")).read()
        except OSError:
            pass
        line = [ln for ln in diag.splitlines() if ln.startswith("Kestrel's own player:")]
        check("Diagnostics says how it went (frames shown, decoder)", bool(line) and "frames shown" in line[-1], "line: %s" % (line[-1][:200] if line else "none"))
        self.np_line = line[-1] if line else ""
        self.np_results = self.np_results + checks
        self.record({"kind": "player", "name": "kestrel-native-player-hls" if hls else "kestrel-native-player", "status": "ok" if all(c["ok"] for c in checks) else "failed", "checks": checks})
        self.check_alive("kestrel-native-player-hls" if hls else "kestrel-native-player")

    # ANS52g (owner message 52, research/VIDEO_ACCELERATION.md section "ANS52g design"): the page script that answers "no" to a video format the settings or the tablet's chips rule out, on the REAL engine. The emulator has no
    # hardware decoders, so the debug intent `vfmt` (arg "<mode>,<30 fps switch>,<tablet|none>") plays the owner's two settings and, with `tablet`, the chip limits of the real tablet (VP9 1080p30, H.264 1080p64). The
    # lab page /formats asks the three questions a player asks for a table of types and reports the answers; every case is compared with the case "engine" (policy off = the engine's own answers), so a "no" can only
    # be the policy's.
    CP_CASES = [("engine", "2,0,none"), ("auto-tablet", "0,0,tablet"), ("h264only", "1,0,tablet"), ("fps30", "0,1,tablet"), ("auto-nochip", "0,0,none")]
    CP_REFUSED = {                                          # rows the policy must answer no (where the engine said yes), per case
        "auto-tablet": ["vp9-1080p60", "vp9-1440p30", "vp9-4k30", "avc-4k30"],
        "h264only": ["vp9-720p30", "vp9-720p60", "vp9-1080p30", "vp9-1080p60", "vp9-1440p30", "vp9-4k30", "vp9-bare", "avc-4k30"],
        "fps30": ["vp9-720p60", "vp9-1080p60", "avc-720p60", "avc-1080p60", "vp9-1440p30", "vp9-4k30", "avc-4k30"],
        "auto-nochip": [],
    }
    CP_KEPT = {                                             # rows that must stay exactly as the engine says
        "auto-tablet": ["vp9-720p30", "vp9-720p60", "vp9-1080p30", "avc-720p30", "avc-720p60", "avc-1080p60", "vp9-bare", "avc-bare", "audio-aac"],
        "h264only": ["avc-720p30", "avc-720p60", "avc-1080p60", "avc-bare", "audio-aac"],
        "fps30": ["vp9-720p30", "vp9-1080p30", "avc-720p30", "vp9-bare", "avc-bare", "audio-aac"],
        "auto-nochip": ["vp9-720p30", "vp9-720p60", "vp9-1080p30", "vp9-1080p60", "vp9-1440p30", "vp9-4k30", "vp9-bare", "avc-720p30", "avc-720p60", "avc-1080p60", "avc-4k30", "avc-bare", "audio-aac"],   # no limit known: nothing is refused
    }

    def cp_scene(self):
        checks = []
        def check(name, ok, detail=""):
            checks.append({"check": name, "ok": bool(ok), "detail": detail if not ok else ""})
            if not ok:
                self.issue("high", "video-formats", "%s %s" % (name, detail))
        got = {}                                            # label -> pass -> {row: (mse, can, mc)}
        for label, arg in self.CP_CASES:
            self.ph.debug("vfmt", arg)
            time.sleep(2.5)
            rmark = self.lab.size()
            self.ph.open_url("%s/formats?tag=%s" % (self.lab_base, label))
            t0 = time.time()
            while time.time() - t0 < 40 and not [r for r in self.lab.since(rmark) if r.get("kind") == "fmt" and r.get("tag") == label and r.get("pass") == "b"]:
                time.sleep(1)
            time.sleep(1)
            self.shot("cp-" + label)
            passes = {}
            for r in self.lab.since(rmark):
                if r.get("kind") != "fmt" or r.get("tag") != label:
                    continue
                rows = {}
                for item in (r.get("d") or "").split(","):
                    f = item.split(":")
                    if len(f) == 4:
                        rows[f[0]] = (f[1], f[2], f[3])
                passes[r.get("pass")] = rows
            got[label] = passes
            check("%s: the page asked its questions (two passes)" % label, "a" in passes and "b" in passes and len(passes.get("b", {})) >= 13, "passes %s" % {k: len(v) for k, v in passes.items()})
        base = got.get("engine", {}).get("b", {})
        check("the engine itself answers yes to H.264 720p30 (the proof below is not vacuous)", base.get("avc-720p30", ("0",))[0] == "1", "engine row %s" % (base.get("avc-720p30"),))
        yes = lambda t: t not in ("0", "-", "x", "e")           # an answer that says "yes" in any of the three forms (1, probably, maybe)
        for label, _ in self.CP_CASES[1:]:
            rows = got.get(label, {}).get("b", {})
            informative = 0
            for name in self.CP_REFUSED[label]:
                b, a = base.get(name), rows.get(name)
                if not b or not a:
                    check("%s: row %s was answered" % (label, name), False, "missing row")
                    continue
                for qi, q in enumerate(("isTypeSupported", "canPlayType", "decodingInfo")):
                    if a[qi] == "x" or not yes(b[qi]):
                        continue                                # not asked, or the engine says no anyway: nothing to prove
                    informative += 1
                    check("%s: %s answers no to %s" % (label, q, name), not yes(a[qi]), "engine said %s, with the policy %s" % (b[qi], a[qi]))
            check("%s: at least 6 refusals were proven against yes answers of the engine" % label, informative >= 6 or not self.CP_REFUSED[label], "only %d informative answers" % informative)
            for name in self.CP_KEPT[label]:
                b, a = base.get(name), rows.get(name)
                if b and a:
                    cols = (0, 1, 2)
                    if name.startswith("vp9") and label in ("auto-nochip", "h264only"):    # there the engine's own VP9 preference (off unless a chip makes Gecko offer it) decides isTypeSupported, not the page script
                        cols = (1, 2)
                    check("%s: %s is left to the engine (%s)" % (label, name, "/".join(b)), all(a[i] == b[i] for i in cols), "engine %s, with the policy %s (columns %s)" % (b, a, cols))
            for name, a in sorted(rows.items()):
                self.cp_table.append((label, name) + tuple(a))
        for name, b in sorted(base.items()):
            self.cp_table.append(("engine", name) + tuple(b))
        late = [l for l, _ in self.CP_CASES[1:] if got.get(l, {}).get("a") != got.get(l, {}).get("b")]
        check("the policy was already there at the page's first questions (1.5 s after it started)", not late, "pass a differs from pass b in %s" % late)
        self.dump_diag("cp")
        diag = ""
        try:
            diag = open(os.path.join(self.out, "diagnostics-cp.txt")).read()
        except OSError:
            pass
        line = [ln for ln in diag.splitlines() if ln.startswith("Video formats offered to sites")]
        self.cp_line = line[-1] if line else ""
        check("Diagnostics says what the page script refused (the counts came back through the extension)", bool(line) and "refused so far: questions" in line[-1], "line: %s" % (line[-1][-300:] if line else "none"))
        self.ph.debug("vfmt", "0,0,none")                     # the owner's defaults again
        self.cp_results = checks
        self.record({"kind": "formats", "name": "video-formats-policy", "status": "ok" if all(c["ok"] for c in checks) else "failed", "checks": checks})
        self.check_alive("video-formats-policy")

    # ANS52h (owner message 52, research/VIDEO_ACCELERATION.md section "ANS52h design"): the picture size cap for HLS pages on the REAL engine. The lab page /hlsfetch asks for four playlists with XMLHttpRequest, as
    # hls.js does: a master list with four rungs (1920, 1280, 960 and 640 wide) + an audio rung + a picture strip, the same one gzip-compressed (a CDN does that; this is where the lab learns whether Gecko hands the
    # extension's filter the decoded or the raw bytes), a media list and a master list that is within every cap. The debug intent `vcap` plays the five settings; what the page received is compared with what
    # each setting must leave: rungs and the longest picture side. A playlist the extension cannot read is passed on untouched, so the gzip row may legitimately come back unchanged: it is reported either way.
    HS_CASES = [("off", "1"), ("auto", "0"), ("p480", "2"), ("p720", "3"), ("p1080", "4")]
    HS_EXPECT = {                                         # case -> name -> (stream-inf lines, longest picture side, picture strips)
        "off": {"master": (5, 1920, 1), "master-small": (3, 960, 0)},
        "auto": {"master": (4, 1280, 0), "master-small": (3, 960, 0)},
        "p480": {"master": (2, 640, 0), "master-small": (2, 640, 0)},
        "p720": {"master": (4, 1280, 0), "master-small": (3, 960, 0)},
        "p1080": {"master": (5, 1920, 1), "master-small": (3, 960, 0)},
    }

    def hls_scene(self):
        checks = []
        def check(name, ok, detail=""):
            checks.append({"check": name, "ok": bool(ok), "detail": detail if not ok else ""})
            if not ok:
                self.issue("high", "hls-cap", "%s %s" % (name, detail))
        got = {}
        for label, arg in self.HS_CASES:
            self.ph.debug("vcap", arg)
            time.sleep(2.5)
            rmark = self.lab.size()
            self.ph.open_url("%s/hlsfetch?tag=%s" % (self.lab_base, label))
            t0 = time.time()
            while time.time() - t0 < 40 and not [r for r in self.lab.since(rmark) if r.get("kind") == "hls" and r.get("tag") == label and r.get("pass") == "b"]:
                time.sleep(1)
            time.sleep(1)
            self.shot("hs-" + label)
            passes = {}
            for r in self.lab.since(rmark):
                if r.get("kind") != "hls" or r.get("tag") != label:
                    continue
                rows = {}
                for item in (r.get("d") or "").split(","):
                    f = item.split(":")
                    if len(f) == 6:
                        rows[f[0]] = (f[1], int(f[2] or 0), int(f[3] or 0), int(f[4] or 0), int(f[5] or 0))   # status, length, stream-inf lines, longest side, strips
                passes[r.get("pass")] = rows
            got[label] = passes
            check("%s: the page fetched its four playlists (both passes)" % label, "a" in passes and "b" in passes and len(passes.get("b", {})) == 4, "passes %s" % {k: len(v) for k, v in passes.items()})
        off = got.get("off", {}).get("b", {})
        check("without a cap the master list arrives whole (5 stream tags, 1920 wide, a picture strip)", off.get("master", (0, 0, 0, 0, 0))[2:] == (5, 1920, 1), "got %s" % (off.get("master"),))
        for label, _ in self.HS_CASES:
            rows = got.get(label, {}).get("b", {})
            for name, want in self.HS_EXPECT[label].items():
                row = rows.get(name)
                have = (row[2], row[3], row[4]) if row else None
                check("%s: %s arrives with %d stream tags, longest side %d, %d picture strips" % (label, name, want[0], want[1], want[2]), have == want, "got %s" % (have,))
            m = rows.get("media")
            check("%s: a media playlist is passed on byte for byte" % label, bool(m) and bool(off.get("media")) and m[1] == off["media"][1] and m[2] == 0, "got %s, without a cap %s" % (m, off.get("media")))
            gz = rows.get("master-gz")
            ref = rows.get("master")
            check("%s: the gzip-compressed master list reaches the page whole and readable (cut like the plain one or untouched)" % label,
                  bool(gz) and gz[0] == "200" and gz[2] > 0 and ((ref and (gz[2], gz[3], gz[4]) == (ref[2], ref[3], ref[4])) or (gz[2], gz[3], gz[4]) == (5, 1920, 1)), "got %s, plain list %s" % (gz, ref))
            self.hs_table.append((label, "master", ) + tuple(rows.get("master", ("?",) * 5)))
            self.hs_table.append((label, "master-gz", ) + tuple(rows.get("master-gz", ("?",) * 5)))
        gz_cut = [l for l, _ in self.HS_CASES[1:4] if got.get(l, {}).get("b", {}).get("master-gz", (0, 0, 0, 0, 0))[2:] != (5, 1920, 1)]
        self.hs_gzip = "the gzip-compressed list was cut like the plain one (the filter gets DECODED bytes)" if len(gz_cut) == 3 else ("the gzip-compressed list was passed on untouched (the filter gets RAW bytes, or the extension left it alone)" if not gz_cut else "mixed: cut in %s" % gz_cut)
        late = [l for l, _ in self.HS_CASES if got.get(l, {}).get("a") != got.get(l, {}).get("b")]
        check("the cap was already there at the page's first requests (1.5 s after it started)", not late, "pass a differs from pass b in %s" % late)
        self.dump_diag("hs")
        diag = ""
        try:
            diag = open(os.path.join(self.out, "diagnostics-hs.txt")).read()
        except OSError:
            pass
        line = [ln for ln in diag.splitlines() if ln.startswith("Picture size cap for HLS pages")]
        self.hs_line = line[-1] if line else ""
        check("Diagnostics says what the extension did to the playlists (cut count above 0)", bool(line) and re.search(r"cut ([1-9]\d*) \(", line[-1]) is not None, "line: %s" % (line[-1][-300:] if line else "none"))
        self.ph.debug("vcap", "0")                         # the owner's default again
        self.hs_results = checks
        self.record({"kind": "hls", "name": "hls-picture-cap", "status": "ok" if all(c["ok"] for c in checks) else "failed", "checks": checks})
        self.check_alive("hls-picture-cap")

    # ANS53a / ANS53b (owner message 53: "Never, on any site, ever, anywhere in the app, automatically load a video and start playing it" and "No data should ever be used downloading or buffering videos on web sites
    # unless I press play"): the real engine, the rule ON. Every lab page below is a way a page starts or loads a video by itself (autoplay, preload auto / metadata / nothing said, a script that calls play() with no
    # finger, a script player over a MediaSource, a play button outside the picture, all of it in a frame). The lab server writes every request for a film file with its bytes to media.jsonl; the driver checks that
    # NOT ONE BYTE went out and nothing played before a press, then taps (the picture, or the page's own button) and checks that the film is asked for and, where the page has a play button, plays. Last: the rule off
    # gives the old behaviour (so the switch works), and the Diagnostics line counts what was held.
    AP_CASES = [("autoplay", "picture"), ("preload-auto", "picture"), ("preload-meta", "picture"), ("no-attr", "picture"), ("script", "picture"), ("mse", "button"), ("button", "button"), ("iframe", "picture")]

    def ap_scene(self):
        checks = []
        def check(name, ok, detail=""):
            checks.append({"check": name, "ok": bool(ok), "detail": detail if not ok else ""})
            if not ok:
                self.issue("high", "press-to-play", "%s %s" % (name, detail))
        media = LabReport(os.path.join(self.out, "lab", "media.jsonl"))
        films = ("/clip/landscape.mp4", "/clip/frag.mp4")
        def film_bytes(mark):
            rows = [r for r in media.since(mark) if r.get("method") == "GET" and r.get("path") in films]
            return sum(int(r.get("bytes") or 0) for r in rows), len(rows)
        self.ph.debug("vpress", "1"); time.sleep(2.0)
        for variant, how in self.AP_CASES:
            tag = "ap-" + variant
            rm, mm = self.lab.size(), media.size()
            self.ph.open_url("%s/ap?v=%s&tag=%s" % (self.lab_base, variant, tag))
            t0 = time.time()
            while time.time() - t0 < 30 and not [r for r in self.lab.since(rm) if r.get("kind") == "ap" and r.get("ev") == "layout" and str(r.get("tag", "")).startswith(tag)]:
                time.sleep(1)
            time.sleep(7)                                                              # (long enough for the page's own timers: the script variant plays at 1.2 s, 3.2 s and 5.2 s)
            rows = [r for r in self.lab.since(rm) if r.get("kind") == "ap" and str(r.get("tag", "")).startswith(tag)]
            evs = [r.get("ev") for r in rows]
            n0, req0 = film_bytes(mm)
            self.shot("ap-%s-before" % variant)
            check("%s: not one byte of the film was asked for before a press (%d request(s))" % (variant, req0), n0 == 0, "%d bytes in %d request(s)" % (n0, req0))
            check("%s: nothing played by itself" % variant, "playing" not in evs and "playing-promise" not in evs, "events %s" % sorted(set(evs)))
            if variant == "script":
                rej = [r for r in rows if r.get("ev") == "rejected"]
                check("script: the page's own play() calls without a finger were refused (NotAllowedError)", len(rej) >= 1 and all(r.get("name") == "NotAllowedError" for r in rej), "rejected rows %s" % [(r.get("name")) for r in rej])
            if variant == "mse":
                check("mse: the script player was kept from fetching (no 'sourceopen', no 'fetched')", "sourceopen" not in evs and "fetched" not in evs, "events %s" % sorted(set(evs)))
            lay = [r for r in rows if r.get("ev") == "layout"]
            if not lay:
                check("%s: the page told where its picture is (for the tap)" % variant, False, "no layout row")
                continue
            L = lay[-1]
            tx, ty = (int(L.get("bx") or 0), int(L.get("by") or 0)) if how == "button" else (int(L.get("vx") or 0), int(L.get("vy") or 0))
            if tx <= 0 or ty <= 0:
                tx, ty = self.screen[0] // 2, 260
            m2 = media.size(); r2 = self.lab.size()
            self.ph.tap(tx, ty); time.sleep(9)
            n1, req1 = film_bytes(m2)
            rows2 = [r for r in self.lab.since(r2) if r.get("kind") == "ap" and str(r.get("tag", "")).startswith(tag)]
            ev2 = [r.get("ev") for r in rows2]
            self.shot("ap-%s-after" % variant)
            check("%s: after the press the film is asked for (%d bytes)" % (variant, n1), n1 > 0, "no byte after a tap at %d,%d (page events %s)" % (tx, ty, sorted(set(ev2))))
            if variant in ("mse", "button"):
                check("%s: and it plays (the page's own play button)" % variant, "playing" in ev2 or "playing-promise" in ev2, "events %s" % sorted(set(ev2)))
            if variant == "mse":
                check("mse: the script player now fetched its film", "sourceopen" in ev2 and "fetched" in ev2, "events %s" % sorted(set(ev2)))
            self.ap_table.append((variant, "button" if how == "button" else "picture", n0, n1, "yes" if ("playing" in ev2 or "playing-promise" in ev2) else "no"))
        # the switch off: the old behaviour (a muted autoplay video loads and plays by itself), proof that the switch really is the rule
        self.ph.debug("vpress", "0"); time.sleep(2.0)
        rm, mm = self.lab.size(), media.size()
        self.ph.open_url("%s/ap?v=autoplay&tag=ap-off" % self.lab_base)
        time.sleep(12)
        n_off, req_off = film_bytes(mm)
        evs_off = [r.get("ev") for r in self.lab.since(rm) if r.get("kind") == "ap" and str(r.get("tag", "")).startswith("ap-off")]
        check("off: with the rule switched off the page's muted autoplay video loads and plays by itself again (%d bytes)" % n_off, n_off > 0 and "playing" in evs_off, "bytes %d, events %s" % (n_off, sorted(set(evs_off))))
        self.ap_table.append(("autoplay, rule OFF", "-", n_off, n_off, "yes" if "playing" in evs_off else "no"))
        self.ph.debug("vpress", "1"); time.sleep(1.5)
        self.dump_diag("ap")
        diag = ""
        try:
            diag = open(os.path.join(self.out, "diagnostics-ap.txt")).read()
        except OSError:
            pass
        line = [ln for ln in diag.splitlines() if ln.startswith("Videos start only when pressed")]
        self.ap_line = line[-1] if line else ""
        check("Diagnostics counts what the gate did (refused plays and opened by the person)", bool(line) and re.search(r"refused ([1-9]\d*) plays", line[-1]) is not None and re.search(r"opened by the person: ([1-9]\d*)", line[-1]) is not None, "line: %s" % (line[-1][-300:] if line else "none"))
        self.ph.debug("vpress", "0")
        self.ap_results = checks
        self.record({"kind": "press-to-play", "name": "videos-start-only-when-pressed", "status": "ok" if all(c["ok"] for c in checks) else "failed", "checks": checks})
        self.check_alive("videos-start-only-when-pressed")

    # ---- the whole run
    # ---- VC1 / LP1 (owner message 44): a video with no controls gets Kestrel's own bar when it is tapped; a long press on a video opens the video options and never the floating panel
    VBAR_PAGES = [("naked", "/video/vc?kind=naked", "shown"), ("shadow-root", "/video/vc?kind=shadow", "shown"), ("shadow-root + frame over it", "/video/vc?kind=shadow&ad=1", "shown"), ("own controls", "/video/vc?kind=own", "own")]

    def vbar_round(self):
        w, h = self.screen
        rows = []
        for name, path, expect in self.VBAR_PAGES:
            lmark = self.log.size()
            self.ph.open_url(self.lab_base + path)
            t0 = time.time()
            while time.time() - t0 < 40 and not any(e["ev"] == "loaded" for e in health_events(self.log.since(lmark), "page")):
                time.sleep(1.5)
            time.sleep(4)                                     # the clip plays
            y = 250                                           # inside the 315 px picture (status bar, heading, margin)
            self.ph.tap(w // 2, y); time.sleep(3)
            ev = [e["ev"] for e in health_events(self.log.since(lmark), "videobar")]
            got = "shown" if "shown" in ev else ("own" if "own" in ev else "nothing")
            self.shot("vbar-" + name.split()[0])
            lm2 = self.log.size()
            self.ph.long_press(w // 2, y - 60 if got == "shown" else y, 900); time.sleep(3.5)   # (the bar covers the bottom of the picture: press higher)
            lines = self.log.since(lm2)
            menu = bool(health_events(lines, "video", "long-press-menu"))
            sheet = bool(health_events(lines, "video", "sheet-shown")) or bool(health_events(lines, "video", "sheet-empty"))
            panel = bool(health_events(lines, "panel", "open-space"))
            self.shot("vbar-press-" + name.split()[0])
            self.ph.key("KEYCODE_BACK"); time.sleep(1.5)       # closes the sheet
            rows.append((name, expect, got, menu, sheet, panel))
            if got != expect:
                self.issue("high" if expect == "shown" else "medium", "video-controls", "%s: a tap gave '%s', expected '%s'" % (name, got, expect), shot=True)
            if not menu:
                self.issue("high", "long-press-video", "%s: a long press on the video did not open the video options (menu message missing)" % name, shot=True)
            if panel:
                self.issue("high", "long-press-video", "%s: a long press on the video opened the floating panel" % name, shot=True)
        self.vbar_rows = rows + [r for r in self.vbar_rows if r[0].startswith("yahoo")]   # (the live Yahoo row of an earlier round is kept)
        self.record({"kind": "vbar", "name": "video-controls", "status": "ok" if all(r[2] == r[1] and r[3] and not r[5] for r in rows) else "failed", "results": [{"page": r[0], "tap": r[2], "expected": r[1], "menu": r[3], "sheet": r[4], "panel": r[5]} for r in rows]})
        self.check_alive("video-controls")

    # ---- the owner's pages (message 44) on the real engine. finance.yahoo.com/videos/ has its player (a web component with a shadow root, found by a probe) at the top of the page, autoplaying and muted, 190-507 css
    # pixels down: a tap and a long press there are logged as a row of the table. The quote page TTWO shows the module somewhere down the page that changes from load to load (lab run 17:05: after four swipes it
    # was a list of headlines, and a tap there opened an article): only pictures are taken of it.
    def yahoo_scan(self):
        w, h = self.screen
        lmark = self.log.size()
        self.ph.open_url("https://finance.yahoo.com/videos/")
        t0 = time.time()
        while time.time() - t0 < 90 and not any(e["ev"] in ("loaded", "error") for e in health_events(self.log.since(lmark), "page")):
            time.sleep(2)
        time.sleep(10)                                    # the player starts by itself
        self.shot("yahoo-videos-player")
        y = 25 + int(350 * 1.0625)                        # about the middle of the picture
        tmark = self.log.size()
        self.ph.tap(w // 2, y); time.sleep(3)
        ev = [e["ev"] for e in health_events(self.log.since(tmark), "videobar")]
        got = "shown" if "shown" in ev else ("own" if "own" in ev else "nothing")
        self.shot("yahoo-videos-after-tap")
        lm2 = self.log.size()
        self.ph.long_press(w // 2, y - 40, 900); time.sleep(3.5)
        lines = self.log.since(lm2)
        menu = bool(health_events(lines, "video", "long-press-menu"))
        sheet = bool(health_events(lines, "video", "sheet-shown")) or bool(health_events(lines, "video", "sheet-empty"))
        panel = bool(health_events(lines, "panel", "open-space"))
        self.shot("yahoo-videos-after-press")
        self.ph.key("KEYCODE_BACK"); time.sleep(1.5)
        self.vbar_rows.append(("yahoo-finance /videos/ (live page, player at the top)", "own or shown", got, menu, sheet, panel))
        if panel or not menu:
            self.issue("medium", "long-press-video", "yahoo-finance /videos/: a long press on the page's video player gave menu=%s panel=%s (a live page: look at the pictures yahoo-videos-*)" % (menu, panel), shot=True)
        lmark = self.log.size()
        self.ph.open_url("https://finance.yahoo.com/quote/TTWO/")
        t0 = time.time()
        while time.time() - t0 < 90 and not any(e["ev"] in ("loaded", "error") for e in health_events(self.log.since(lmark), "page")):
            time.sleep(2)
        time.sleep(6)
        for i in range(8):
            self.ph.swipe(w // 2, int(h * 0.78), w // 2, int(h * 0.30), 450); time.sleep(1.8)
            self.shot("yahoo-quote-%02d" % i)
        self.record({"kind": "yahoo", "name": "yahoo-scan", "status": "ok", "note": "videos page: tap gave %s, long press: menu %s sheet %s panel %s; the quote page was only photographed" % (got, menu, sheet, panel)})
        self.check_alive("yahoo-scan")

    # ---- SND1 (owner message 45): "Sound should not keep playing if I am not currently on the Kestrel tab."  The app is left (Home) and the screen is turned off while a clip plays WITH sound; Android's own list of
    # playing audio (dumpsys audio: AudioPlaybackConfiguration ... state:started) says whether anything still makes sound, and the page's own media events say what the engine did to the element.
    def audio_started(self):
        out = self.ph.a.sh("dumpsys audio", timeout=40)
        i = out.find("players:")
        seg = out[i:i + 8000] if i >= 0 else out
        return len(re.findall(r"state:started", seg))

    def audio_round(self):
        """SND1: the clip plays with sound, the app is left (Home / screen off) for 6 s and comes back. The verdict is what the PAGE saw: did the element's currentTime advance while the page was hidden
        (sound = the clock moves)? Lab run 43745675957 showed that Android's own list of playing audio streams (dumpsys audio) cannot be used for this: its 'state:started' entries pile up (1, 3, 5, 7 ... one more
        per round) while the clip's clock stood still and the engine's media session said 'paused' (the Health line 'neon video playing=false')."""
        w, h = self.screen
        rows = []
        for kind in ("video", "audio"):
            for how in ("home", "screen-off"):
                rmark, lmark = self.lab.size(), self.log.size()
                self.ph.open_url("%s/video/audible?kind=%s" % (self.lab_base, kind))
                t0 = time.time()
                while time.time() - t0 < 40 and not any(e["ev"] == "loaded" for e in health_events(self.log.since(lmark), "page")):
                    time.sleep(1.5)
                time.sleep(2)
                self.ph.tap(w // 2, 175); time.sleep(5)           # the big button: sound starts
                playing = self.audio_started()
                lm_away = self.log.size()
                if how == "home":
                    self.ph.home()
                else:
                    self.ph.key("KEYCODE_SLEEP")
                time.sleep(6)
                away = self.audio_started()
                away_lines = self.log.since(lm_away)
                if how == "home":
                    self.ph.start_app()
                else:
                    self.ph.key("KEYCODE_WAKEUP"); self.ph.a.sh("wm dismiss-keyguard")
                time.sleep(5)
                back = self.audio_started()
                self.shot("snd-%s-%s" % (kind, how))
                snd = [r for r in self.lab.since(rmark) if r.get("kind") == "snd"]
                adv, hidden_s = None, None
                try:
                    hi = next(i for i, r in enumerate(snd) if r.get("ev") == "vis" and r.get("vis") == "hidden")
                    vi = next((i for i in range(hi + 1, len(snd)) if snd[i].get("ev") == "vis" and snd[i].get("vis") == "visible"), len(snd) - 1)
                    ct0 = float(snd[hi].get("ct", 0))
                    # the clock at the LAST sign of life while hidden (a tick, or the visible event itself): it must not have moved
                    ct1 = max(float(r.get("ct", 0)) for r in snd[hi:vi + 1])
                    adv = round(ct1 - ct0, 1)
                    hidden_s = round((int(snd[vi]["t"]) - int(snd[hi]["t"])) / 1000.0, 1)
                except (StopIteration, ValueError, KeyError):
                    pass
                session_paused = any("playing=false" in ln and "KestrelHealth" in ln for ln in away_lines)
                rows.append((kind, how, playing, away, back, adv, hidden_s, session_paused))
                if adv is None:
                    self.issue("low", "tooling", "sound lab %s/%s: the page never reported 'hidden': the test says nothing about leaving the app" % (kind, how))
                elif adv >= 1.5:
                    self.issue("high", "sound", "%s: the clip's clock moved %.1f s while the app was left by %s (hidden %s s): sound kept playing" % (kind, adv, how, hidden_s))
        self.snd_rows = rows
        self.record({"kind": "snd", "name": "sound-when-left", "status": "ok" if all(r[5] is not None and r[5] < 1.5 for r in rows) else "failed",
                     "results": [{"clip": r[0], "left_by": r[1], "audio_streams_before": r[2], "away": r[3], "back": r[4], "clock_advance_while_hidden_s": r[5], "hidden_s": r[6], "engine_session_paused": r[7]} for r in rows]})
        self.check_alive("sound-when-left")

    # ---- LAB2 (owner message 45): every site twice, Shields ON then Shields OFF for that site, from a cold cache each time; the report puts the two side by side
    def compat_round(self, minutes):
        budget = minutes * 60 * 0.46
        self.ph.measure_on(); time.sleep(2)
        self.ph.debug("nolook"); time.sleep(2)               # the page's picture read-back (Last look) stalls the UI thread for seconds on the emulator's software GL (lab run 43747605584: an ANR on Yahoo): off for the compat runs
        done = []
        t_start = time.time()
        self.shields_mode = "on"
        for i, (name, url, scrolls, dwell) in enumerate(COMPAT_SITES):
            if time.time() - t_start > budget:
                break
            try:
                ev = self.load_page(name, url, scrolls, dwell, timeout=90, kind="page")
                if ev is not None:
                    done.append((name, url, scrolls, dwell))
            except Exception as e:
                self.issue("low", "tooling", "compat scene %s raised %r" % (name, e))
            self.ph.debug("closetabs"); time.sleep(2)             # every VIEW intent opens a new tab: eight heavy pages in eight tabs killed the emulator's system process (run 43747605584)
            if i % 12 == 11:
                self.dump_diag("compat%d" % (i + 1))
        hosts = []
        for _n, url, _s, _d in done:
            h = host_of(self.url(url))
            for v in (h, "www." + h if not h.startswith("www.") else h[4:], "m." + h if not h.startswith("m.") else h[2:]):
                if v and v not in hosts:
                    hosts.append(v)
        self.compat_sites_done = [d[0] for d in done]
        self.ph.shields("off", hosts); time.sleep(3)
        self.ph.clear_cache(); self.ph.debug("closetabs"); time.sleep(6)
        self.shields_mode = "off"; self.quiet_issues = True
        for i, (name, url, scrolls, dwell) in enumerate(done):
            try:
                self.load_page(name, url, scrolls, dwell, timeout=90, kind="page")
            except Exception as e:
                self.issue("low", "tooling", "compat scene (shields off) %s raised %r" % (name, e))
            self.ph.debug("closetabs"); time.sleep(2)
        self.quiet_issues = False; self.shields_mode = "on"
        self.ph.shields("on", hosts); time.sleep(2)

    def setup_device(self):
        for k in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
            self.ph.setting("global", k, 0)
        self.ph.a.sh("svc power stayon true")
        self.ph.key("KEYCODE_WAKEUP")
        self.ph.a.sh("wm dismiss-keyguard")
        self.ph.setting("system", "accelerometer_rotation", 0)
        self.ph.setting("system", "user_rotation", 0)
        self.ph.setting("secure", "immersive_mode_confirmations", "confirmed")   # Android's one-time "Viewing full screen" box would sit over the picture in every screenshot
        size = self.ph.a.sh("wm size")
        m = re.search(r"(\d+)x(\d+)", size)
        if m:
            self.screen = (int(m.group(1)), int(m.group(2)))
        if self.a.net == "hotspot":
            self.ph.a.run("emu", "network", "speed", "5000:5000")
            self.ph.a.run("emu", "network", "delay", "none")   # the emulator's delay option (60-100 ms) made hacker-news take 33 s and cnn-lite never load (lab run 109): the speed cap alone is used
            st = self.ph.a.run("emu", "network", "status").replace("\n", " ").replace("OK", "").strip()
            self.net_status = st[:200]
            say("network shaped like the owner's hotspot: 5 Mbit/s each way (no extra delay); the emulator says:", self.net_status)
        self.sys_pid = self.system_pid()
        say("screen", self.screen, "| density", self.ph.a.sh("wm density").strip().replace("\n", " "), "| android", self.ph.a.sh("getprop ro.build.version.release").strip())

    def heap_limit_mb(self):
        """The java heap limit this boot gave the app (the app's own 'start heap' Health line), or None when no line came."""
        for e in health_events(self.log.since(0), "start", "heap"):
            m = re.search(r"limit (\d+) MB", e["rest"])
            if m:
                return int(m.group(1))
        return None

    def start_app(self):
        # lab runs 43753283814 and 43775890786: some boots of the emulated phone give EVERY app a 16 MB java heap limit (the usual boot gives 512 MB): the app dies of OutOfMemoryError in a crash loop and the run says
        # nothing about the app. Such a boot is thrown away (the emulator started again from a clean disk, twice at most) before anything is measured.
        for attempt in range(3):
            self.log.stop(); self.log.start()
            self.ph.a.sh("am force-stop %s" % self.a.pkg)
            time.sleep(2)
            t0 = time.time()
            self.ph.start_app()
            eng = None
            while time.time() - t0 < 300:
                if health_events(self.log.since(0), "engine", "runtime-ready"):
                    eng = time.time() - t0
                    break
                if self.heap_limit_mb() is not None and self.heap_limit_mb() < 64 and time.time() - t0 > 12:
                    break                                         # (the app is already dying of it: no need to wait for an engine that will not come)
                time.sleep(3)
            lim = self.heap_limit_mb()
            if lim is None or lim >= 64 or attempt == 2 or self.a.selftest:
                break
            say("this boot of the emulated phone gave the app a %d MB java heap limit (a lottery of the lab's emulator, not the app): starting the emulator again (%d)" % (lim, attempt + 1))
            self.log.stop()
            if not self.hard_restart("a %d MB java heap limit at boot" % lim, quiet=True):
                self.log.start()
                break
        if eng is None:
            self.issue("high", "start", "the engine did not report 'runtime-ready' within 300 s of the app starting", shot=True)
        else:
            say("engine ready after %.0f s" % eng)
        time.sleep(5)
        if eng is not None and self.a.suite != "ap":
            self.ph.debug("vpress", "0"); time.sleep(1.5)          # ANS53: the other scenes were written for pages whose videos start by themselves; the rule has its own suite (ap)
        self.main_pid = (self.ph.pids() or [None])[0]
        self.record({"kind": "start", "name": "app-start", "status": "ok" if eng else "no-engine", "engine_ready_s": round(eng, 1) if eng else None})

    def smoke(self):
        ev = self.load_page("lab-text", "lab:/page/text", 2, 1, timeout=60)
        self.safe_browsing("smoke")
        self.dump_diag("smoke")
        return ev

    def dump_diag(self, tag):
        self.ph.debug("dump")
        time.sleep(7)   # the app reads the engine's preferences first (2.5 s), then writes the text
        txt = self.adb.sh("run-as %s cat files/debug-diag.txt" % self.a.pkg, timeout=30)
        if txt and "Kestrel diagnostics" in txt:
            with open(os.path.join(self.out, "diagnostics-%s.txt" % tag), "w") as f:
                f.write(txt)
            self.last_diag = txt
            return txt
        self.issue("low", "tooling", "the Diagnostics dump (%s) did not arrive" % tag)
        return ""

    def schedule(self, minutes):
        """The scene list for the soak, shuffled but fair: every kind appears again and again until the time is up."""
        pools = [("page", p) for p in PUBLIC_PAGES] * 1 + [("lab", p) for p in LAB_PAGES] * 2 + [("video", v) for v in VIDEOS] * 1 + [("vsite", v) for v in VIDEO_PAGES] * 1 + \
                [("dl", d) for d in DOWNLOADS] * 1 + [("fs", None)] * 2 + [("guard", None)] + [("vbar", None)] * 2 + [("yahoo", None)]
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
                elif kind == "guard":
                    self.guard_downloads()
                    self.apk_guard()
                elif kind == "vbar":
                    self.vbar_round()
                elif kind == "yahoo":
                    self.yahoo_scan()
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
        loads = [e for e in ev if e.get("kind") in ("page", "labpage", "videosite") and e.get("status") in ("ok", "slow") and e.get("shields", "on") == "on"]
        by_host = collections.defaultdict(list)
        for e in loads:
            by_host[e["name"]].append(e)
        vids = [e for e in ev if e.get("kind") == "video"]
        dls = [e for e in ev if e.get("kind") == "download"]
        counts = collections.Counter(e.get("status") for e in ev)
        L = []
        L.append("# Kestrel emulator lab: %s run" % self.a.suite)
        L.append("")
        L.append("Run: %s, %d scenes in %.0f min. Device: Android 10 emulator, %dx%d, package %s, network %s. **x86_64 + software GL is NOT the tablet: only behaviour counts, not absolute times.**" %
                 (time.strftime("%Y-%m-%d %H:%M UTC", time.gmtime()), len(ev), (time.time() - T0) / 60, self.screen[0], self.screen[1], self.a.pkg,
                  "shaped like the owner's hotspot (public sites: 5 Mbit/s by the emulator, it reports: %s; the lab's own clips and downloads: 600 KB/s and 80 ms by the lab server)" % (self.net_status or "no answer") if self.a.net == "hotspot" else "unshaped (the runner's own fast link)"))
        L.append("")
        L.append("## Summary")
        L.append("")
        L.append("Scene results: " + ", ".join("%s %d" % (k, v) for k, v in sorted(counts.items(), key=lambda kv: -kv[1])))
        L.append("The emulated phone's own system restarted %d time(s), the emulator itself started again from scratch %d time(s) (an overloaded emulator, not the app)." % (self.system_restarts, self.hard_restarts))
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
        if self.guard_rows:
            L.append("## Downloads nobody tapped (RX4-X2)")
            L.append("")
            for kind, res in self.guard_rows:
                L.append("- %s: %s" % (kind, res))
            L.append("")
        if self.apk_rows:
            L.append("## Installers that pretend to be Kestrel (RX4-X1)")
            L.append("")
            for n, ok, said in self.apk_rows:
                L.append("- [%s] %s: %s" % ("x" if ok else " ", n, said))
            L.append("")
        if self.snd_rows:
            L.append("## Sound when the app is left (SND1, owner message 45)")
            L.append("")
            L.append("The verdict is the clip's own clock while the page was hidden (it must stand still: no sound); the engine's media session must say paused. Android's `dumpsys audio` stream counts are shown for the record only (their 'started' entries pile up).")
            L.append("")
            L.append("| clip | the app was left by | clock moved while hidden (must be 0) | hidden for | engine media session paused | audio streams before / away / back (record only) |")
            L.append("| :- | :- | -: | -: | :- | :- |")
            for kind, how, playing, away, back, adv, hidden_s, sp in self.snd_rows:
                L.append("| %s | %s | %s s | %s s | %s | %d / %d / %d |" % (kind, how, "?" if adv is None else adv, "?" if hidden_s is None else hidden_s, "yes" if sp else "no", playing, away, back))
            L.append("")
        if self.np_results:
            L.append("## Kestrel's own player (Media3 on a SurfaceView, ANS52i, owner message 52)")
            L.append("")
            L.append("| check | result | detail |")
            L.append("| :- | :- | :- |")
            for c in self.np_results:
                L.append("| %s | %s | %s |" % (c["check"], "ok" if c["ok"] else "**FAILED**", (c["detail"] or "")[:160].replace("|", "/")))
            L.append("")
        if self.ap_results:
            L.append("## Videos start only when pressed (ANS53a / ANS53b, owner message 53), on the real engine")
            L.append("")
            L.append("| check | result | detail |")
            L.append("| :- | :- | :- |")
            for c in self.ap_results:
                L.append("| %s | %s | %s |" % (c["check"], "ok" if c["ok"] else "**FAILED**", (c["detail"] or "")[:160].replace("|", "/")))
            L.append("")
            if self.ap_line:
                L.append("Diagnostics line: %s" % self.ap_line[:700])
                L.append("")
            L.append("| page | pressed | film bytes before the press | film bytes after | played |")
            L.append("| :- | :- | -: | -: | :- |")
            for row in self.ap_table:
                L.append("| %s | %s | %s | %s | %s |" % row)
            L.append("")
        if self.hs_results:
            L.append("## The picture size cap for HLS pages on the real engine (ANS52h, owner message 52)")
            L.append("")
            L.append("| check | result | detail |")
            L.append("| :- | :- | :- |")
            for c in self.hs_results:
                L.append("| %s | %s | %s |" % (c["check"], "ok" if c["ok"] else "**FAILED**", (c["detail"] or "")[:160].replace("|", "/")))
            L.append("")
            L.append("What the gzip test showed: %s." % getattr(self, "hs_gzip", "not run"))
            L.append("")
            if self.hs_line:
                L.append("Diagnostics line: %s" % self.hs_line[:700])
                L.append("")
            L.append("What the page received (status, bytes, stream tags, longest picture side, picture strips):")
            L.append("")
            L.append("| setting | playlist | status | bytes | stream tags | longest side | strips |")
            L.append("| :- | :- | :- | -: | -: | -: | -: |")
            for row in self.hs_table:
                L.append("| %s | %s | %s | %s | %s | %s | %s |" % row)
            L.append("")
        if self.cp_results:
            L.append("## The page's video format policy on the real engine (ANS52g, owner message 52)")
            L.append("")
            L.append("| check | result | detail |")
            L.append("| :- | :- | :- |")
            for c in self.cp_results:
                L.append("| %s | %s | %s |" % (c["check"], "ok" if c["ok"] else "**FAILED**", (c["detail"] or "")[:160].replace("|", "/")))
            L.append("")
            if self.cp_line:
                L.append("Diagnostics line: %s" % self.cp_line[:600])
                L.append("")
            L.append("Answers per case (isTypeSupported / canPlayType / decodingInfo; 1 or probably = yes, 0 or - = no, x = not asked):")
            L.append("")
            L.append("| case | type | isTypeSupported | canPlayType | decodingInfo |")
            L.append("| :- | :- | :- | :- | :- |")
            for row in self.cp_table:
                L.append("| %s | %s | %s | %s | %s |" % row)
            L.append("")
        if self.kp_results:
            L.append("## Kestrel's own full-screen controls on the real engine (VP1, owner message 49)")
            L.append("")
            L.append("| check | result | detail |")
            L.append("| :- | :- | :- |")
            for c in self.kp_results:
                L.append("| %s | %s | %s |" % (c["check"], "ok" if c["ok"] else "**FAILED**", (c["detail"] or "")[:160].replace("|", "/")))
            L.append("")
            if self.kp_pic_path:
                L.append("The film's picture size for automatic turning (ANS51b): %s." % self.kp_pic_path)
                L.append("")
            if self.kp_ff:
                L.append("First frame at the new size after each turn (ANS52a), ms: %s." % ", ".join(str(x) for x in self.kp_ff))
                L.append("")
            if self.kp_wake:
                L.append("Wake locks of the emulated phone (`dumpsys power`), for the question whether the screen stays on while a film plays (the lab keeps the screen on by itself with `svc power stayon`, so read the lock names, not the screen):")
                L.append("")
                for lab, lines in self.kp_wake:
                    L.append("- %s: %s" % (lab, " / ".join(lines) or "no wake-lock line"))
                L.append("")
        if self.slow_results:
            L.append("## A turn under a slow engine (ROT2, owner message 48: the page keeps the layout of the old orientation for a few seconds)")
            L.append("")
            L.append("Busy loops slow the emulator's engine; the screen is read for 9 s after each turn: `right` = the red strip sits at the bottom of the page area, `cover` = the app's black cover, `stale` = anything else (a half-laid-out or stale page). `wait on` = the new rule (the cover and the pause stay until the page says it has the new size); `wait off` = the rule of alpha51 / alpha52 (the cover lifts when the app's own view is still).")
            L.append("")
            L.append("Busy loops started per mode: " + ", ".join("%s %d" % (r["mode"], r.get("hogs", 0)) for r in self.slow_results) + " (0 = the emulator was not slowed down and the table shows nothing); nice value read back for the engine's content process: " + ", ".join("%s %s" % (r["mode"], r.get("slowed") or "none found") for r in self.slow_results) + ".")
            L.append("")
            L.append("| mode | turn | screen reads (seconds after the turn: what the screen showed) | the turn | engine nudged | the page's report |")
            L.append("| :- | :- | :- | :- | -: | :- |")
            for r in self.slow_results:
                if not r["entered"]:
                    L.append("| wait %s | did not go full screen | - | - | - | - |" % r["mode"])
                    continue
                for t in r["turns"]:
                    L.append("| wait %s | %s | %s | %s | %d | %s |" % (r["mode"], t["turn"], " ".join("%.1f %s" % (dt, c) for dt, c, *_ in t["samples"]), (t["settled"] or "-")[:110], t["nudges"], " / ".join(t["viewport"]).replace("|", "/")[:120] or "-"))
            L.append("")
        if self.fsb_results:
            L.append("## Full screen: the system bars, the turn and the controls (FSB1 / ROT1 / FSC1, owner message 46)")
            L.append("")
            L.append("`keep` = Settings > System bars in full-screen video on Keep (the default: the bars stay, the page is laid out between them, like the owner's tablet needs); `hide` = on Hide (this emulator's system really hides them). The page's red strip must stay visible in every state; `keep` must end above the 52 px navigation bar.")
            L.append("")
            L.append("| bars | player box | state | red strip rows (first-last of screen height) | page window w x h | video paused for the turn | turn settled | system bars note |")
            L.append("| :- | :- | :- | :- | :- | -: | :- | :- |")
            for r in self.fsb_results:
                nm = "%s%s" % (r["kind"], " (no controls of its own)" if r["nobar"] else "")
                if not r["entered"]:
                    L.append("| %s | %s | did not go full screen | - | - | - | - | - |" % (r["mode"], nm))
                    continue
                for st in r["states"]:
                    red = st["red"]
                    redtxt = "%d (%d-%d of %d)" % red if red else "no picture"
                    L.append("| %s | %s | %s | %s | %sx%s | %s%s | %s | %s |" % (r["mode"], nm, st["state"], redtxt, st.get("iw") or "?", st.get("ih") or "?", st["paused"], " (silent clip: no session)" if st["no_session"] else "", ((st["settled"] or "-")[:70] + (" [%d config changes]" % st["configs"] if st.get("configs", 0) >= 2 else "")), (st["bars"] or "-")[:90]))
                    if st.get("viewport"):                                  # FB4: what the page told about its viewport and what the app did (numbers only)
                        L.append("| | | | viewport (FB4): %s | | | | |" % " / ".join(v[:100].replace("|", "/") for v in st["viewport"]))
                if r["nobar"]:
                    L.append("| %s | %s | Kestrel's bar after a tap on the full-screen film | %s | | | | |" % (r["mode"], nm, "shown" if r["bar_shown"] else "NOT SHOWN"))
            L.append("")
        if self.yahoo_fs:
            y = self.yahoo_fs
            L.append("## The owner's Yahoo Finance player in full screen, turned sideways and back (message 46; pictures yahoofs-*)")
            L.append("")
            L.append("- full screen %s (%s)" % ("began" if y["entered"] else "did NOT begin", y["how"]))
            L.append("- Kestrel's own controls %s; the page's own player controls put away (elements, in the order the page made them): %s" % ("started" if y.get("kp_started") else "did NOT start", y.get("away") or "none"))
            for st in y["states"]:
                L.append("- %s: video paused for the turn %d time(s); %s; page-area size changes %d; bars: %s" % (st["state"], st["paused"], st["settled"][0] or "no 'turn settled' line", st["sizes"], st["bars"][0] or "-"))
            L.append("")
        if self.vbar_rows:
            L.append("## Video controls and the long press on a video (VC1 / LP1, owner message 44)")
            L.append("")
            L.append("| page | a tap gave | expected | long press: video options | the sheet | floating panel (must be no) |")
            L.append("| :- | :- | :- | :- | :- | :- |")
            for name, expect, got, menu, sheet, panel in self.vbar_rows:
                L.append("| %s | %s | %s | %s | %s | %s |" % (name, got, expect, "yes" if menu else "NO", "yes" if sheet else "-", "YES" if panel else "no"))
            L.append("")
        if self.fs_result:
            L.append("## Full-screen turning (FS1)")
            L.append("")
            for label, ok in self.fs_result:
                L.append("- [%s] %s" % ("x" if ok else " ", label))
            L.append("")
        # ---------------------------------------------------------------- LAB2: speed, compatibility, errors, scrolling, memory of the measured pages
        measured = [e for e in ev if e.get("kind") == "page" and "shields" in e]
        on_pass = [e for e in measured if e["shields"] == "on"]
        off_pass = {e["name"]: e for e in measured if e["shields"] == "off"}
        okstat = ("ok", "slow")

        def cell(e):
            if not e:
                return "-"
            if e.get("status") not in okstat:
                return str(e.get("status"))
            pr, c = e.get("probe") or {}, e.get("console") or {}
            return "%s / %s ms, text %s, pictures %s (broken %s), errors %s" % (e.get("paint_ms"), e.get("done_ms"), pr.get("t", "?"), pr.get("iv", "?"), pr.get("ib", "?"), c.get("errors", "?"))

        def verdict(on, off):
            if not on or not off:
                return ""
            if off.get("status") in okstat and on.get("status") not in okstat:
                return "BREAKS WITH SHIELDS (%s only with Shields on)" % on.get("status")
            if not (on.get("status") in okstat and off.get("status") in okstat):
                return ""
            reasons = []
            pon, poff = on.get("probe"), off.get("probe")
            if pon and poff:
                t_on, t_off = int(pon.get("t", 0)), int(poff.get("t", 0))
                if t_off >= 500 and t_on < 0.6 * t_off:
                    reasons.append("less text (%d vs %d)" % (t_on, t_off))
                iv_on, iv_off = int(pon.get("iv", 0)), int(poff.get("iv", 0))
                if iv_off >= 10 and iv_on < 0.6 * iv_off:
                    reasons.append("fewer pictures (%d vs %d)" % (iv_on, iv_off))
                if int(poff.get("vd", 0)) > int(pon.get("vd", 0)):
                    reasons.append("fewer videos")
                if int(pon.get("ib", 0)) > int(poff.get("ib", 0)) + 5:
                    reasons.append("more broken pictures")
            con, coff = (on.get("console") or {}).get("errors", 0), (off.get("console") or {}).get("errors", 0)
            if con > coff + 8:
                reasons.append("more script errors (%d vs %d)" % (con, coff))
            son, soff = on.get("screen") or {}, off.get("screen") or {}
            if soff and son and soff.get("edges", 0) > 0.05 and son.get("edges", 0) < 0.5 * soff.get("edges", 0):
                reasons.append("emptier first screen")
            if reasons:
                return "WORSE WITH SHIELDS: " + "; ".join(reasons)
            if on.get("done_ms", 0) > off.get("done_ms", 0) * 1.5 + 1500:
                return "slower with Shields (%d vs %d ms)" % (on["done_ms"], off["done_ms"])
            return ""

        if on_pass:
            def pct(vals, q):
                vals = sorted(v for v in vals if isinstance(v, (int, float)) and v >= 0)
                return vals[min(len(vals) - 1, int(len(vals) * q))] if vals else None
            good = [e for e in on_pass if e.get("status") in okstat]
            L.append("## Speed of the measured pages (%d sites loaded, %d did not)" % (len(good), len(on_pass) - len(good)))
            L.append("")
            L.append("First paint: median %s ms, 90th percentile %s ms. Load finished: median %s ms, 90th percentile %s ms. (Fast link, software GL: compare runs with each other, not with the tablet.)" %
                     (pct([e.get("paint_ms") for e in good], 0.5), pct([e.get("paint_ms") for e in good], 0.9), pct([e.get("done_ms") for e in good], 0.5), pct([e.get("done_ms") for e in good], 0.9)))
            L.append("")
            L.append("Slowest to finish: " + ", ".join("%s %.1f s" % (e["name"], (e.get("done_ms") or 0) / 1000.0) for e in sorted(good, key=lambda e: -(e.get("done_ms") or 0))[:10]))
            L.append("")
            bad = []
            for e in on_pass:
                why = []
                scr, pr, c = e.get("screen") or {}, e.get("probe") or {}, e.get("console") or {}
                if e.get("status") not in okstat:
                    why.append("did not load (%s)" % e.get("status"))
                else:
                    if scr and (scr.get("dominant", 0) >= 0.97 or scr.get("edges", 1) < 0.01):
                        why.append("first screen looks blank (dominant colour %.0f %%, edges %s)" % (scr.get("dominant", 0) * 100, scr.get("edges")))
                    if pr and int(pr.get("t", 99)) < 20:
                        why.append("no text on the page")
                    if pr and int(pr.get("ww", 0)) > int(pr.get("iw", 0)) + 8 and int(pr.get("iw", 0)) > 0:
                        why.append("wider than the screen (%s vs %s px)" % (pr.get("ww"), pr.get("iw")))
                    if pr and int(pr.get("ib", 0)) >= max(4, 0.3 * int(pr.get("im", 0))):
                        why.append("%s of %s pictures broken" % (pr.get("ib"), pr.get("im")))
                    if c.get("errors", 0) >= 20:
                        why.append("%d script errors" % c.get("errors"))
                if why:
                    bad.append((e["name"], "; ".join(why)))
            L.append("## Pages that did not load correctly (Shields on, the default)")
            L.append("")
            for n, w in bad:
                L.append("- **%s**: %s" % (n, w))
            if not bad:
                L.append("- none")
            L.append("")
            if off_pass:
                L.append("## Compatibility: every site with Shields ON and with Shields OFF (a site that is worse with Shields on is OUR false positive)")
                L.append("")
                L.append("| site | Shields ON: paint / done, text, pictures, script errors | Shields OFF | verdict |")
                L.append("| :- | :- | :- | :- |")
                worse_list = []
                for e in on_pass:
                    off = off_pass.get(e["name"])
                    if not off:
                        continue
                    v = verdict(e, off)
                    L.append("| %s | %s | %s | %s |" % (e["name"], cell(e), cell(off), v or "same"))
                    if v.startswith(("WORSE", "BREAKS")):
                        worse_list.append((e["name"], v))
                L.append("")
                L.append("Suspects (worse with Shields on): " + ("; ".join("%s: %s" % w for w in worse_list) if worse_list else "none"))
                L.append("")
            sc = [e for e in on_pass if e.get("scroll")]
            if sc:
                tot_frames = sum(e["scroll"]["frames"] for e in sc)
                tot50 = sum(e["scroll"]["gaps50"] for e in sc)
                tot100 = sum(e["scroll"]["gaps100"] for e in sc)
                L.append("## Scrolling (the app's own counters while the lab flings each page; software GL: only the differences between pages and between builds mean something)")
                L.append("")
                L.append("%d pages scrolled, %d frames counted, %d screen updates over 50 ms (%.1f %%), %d over 100 ms (%.1f %%)." % (len(sc), tot_frames, tot50, 100.0 * tot50 / max(1, tot_frames), tot100, 100.0 * tot100 / max(1, tot_frames)))
                L.append("")
                L.append("| page | scroll bursts | frames | gaps over 50 ms | over 100 ms | longest gap ms |")
                L.append("| :- | -: | -: | -: | -: | -: |")
                for e in sorted(sc, key=lambda e: -(e["scroll"]["gaps100"] * 5 + e["scroll"]["gaps50"]))[:15]:
                    x = e["scroll"]
                    L.append("| %s | %d | %d | %d | %d | %d |" % (e["name"], x["bursts"], x["frames"], x["gaps50"], x["gaps100"], x["worst_ms"]))
                L.append("")
            mem = [e for e in on_pass if e.get("pss")]
            if mem:
                L.append("## Memory after each page (app PSS, MB; the tablet has 2 GB)")
                L.append("")
                L.append("Largest: " + ", ".join("%s %d" % (e["name"], e["pss"]) for e in sorted(mem, key=lambda e: -e["pss"])[:10]) + ". Median " + str(pct([e["pss"] for e in mem], 0.5)) + " MB.")
                L.append("")
            msgs = collections.Counter()
            for e in on_pass:
                for m_ in (e.get("console") or {}).get("first", []):
                    msgs[m_[:90]] += 1
            noisy = sorted(((e.get("console") or {}).get("errors", 0), e["name"]) for e in on_pass)[-8:]
            L.append("## Script errors of the pages (the engine's own log; the first error of each page, most frequent first)")
            L.append("")
            L.append("Most errors: " + ", ".join("%s %d" % (n, c_) for c_, n in reversed(noisy) if c_ > 0))
            for m_, c_ in msgs.most_common(12):
                L.append("- %d x %s" % (c_, m_))
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
        vdl = [e for e in ev if e.get("kind") == "videodl"]
        if vdl:
            L.append("## Finding and saving videos (the app's 'Save video' finder, then one tap on its first row)")
            L.append("")
            for e in vdl:
                L.append("- %s: %s%s (finder: %s)" % (e["name"], e["status"], (", " + e.get("note", "")) if e.get("note") else (", %s bytes" % e.get("size", "?")), e.get("finder_said")))
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
        if self.last_diag:
            keep = ("Real video", "Real scrolling", "Full-screen turning", "Video controls", "Blocker:", "Page loads", "  ", "Counters:", "Cold start", "Engine prefs", "CPU", "Processes:", "Memory:", "Network:", "Video:")
            lines = [ln for ln in self.last_diag.splitlines() if ln.startswith(keep[:6]) or ln.startswith(("Counters:", "Cold start", "Engine prefs", "CPU by process", "Processes:", "Memory:", "Network:", "Video:", "Safe", "Downloads"))]
            L.append("## What the app says about itself (its own Diagnostics text at the end of the run)")
            L.append("")
            for ln in lines[:40]:
                L.append("- " + ln[:520].replace("|", "/"))
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
            elif "/fsbox" in url:
                self._lab({"kind": "fsb", "ev": "page", "iw": 600, "ih": 886})
                self._lab({"kind": "fsb", "ev": "fullscreenchange", "fs": "1", "iw": 600, "ih": 1024, "mb": 1024})
                self._health("page loaded host=%s paint=500ms done=900ms blocked=0 visit=1 ok=true" % host)
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
        if cmd.startswith("getprop sys.boot_completed"):
            return "1"
        if cmd.startswith("pidof system_server"):
            return "1111"
        if cmd.startswith("pm path android"):
            return "package:/system/framework/framework-res.apk"
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
    ap.add_argument("--suite", default="soak", choices=["smoke", "fs", "soak", "all", "vc", "snd", "compat", "fsy", "kp", "yh", "np", "cp", "ap", "vp"])
    ap.add_argument("--minutes", type=float, default=60)
    ap.add_argument("--pkg", default=PKG_DEFAULT)
    ap.add_argument("--port", type=int, default=8091)
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--net", default="full", choices=["full", "hotspot"], help="hotspot = the owner's link: 5 Mbit/s each way and about 80 ms of delay (the carrier caps his phone hotspot)")
    ap.add_argument("--emu-cmd", default="", help="how the workflow started the emulator: used to start it again from a clean disk when the phone's system keeps dying")
    ap.add_argument("--apk", default="", help="the debug APK, installed again after such a restart")
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    if a.selftest:
        a.minutes, a.suite = 0.4, (a.suite if a.suite in ("vc", "snd", "compat", "fsy", "kp", "yh", "np") else "all")
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
        if a.suite in ("fs", "all"):
            s.guard_downloads()
            s.apk_guard()
        if a.suite in ("vc", "all"):
            s.vbar_round()
            s.yahoo_scan()
        if a.suite in ("snd", "all"):
            s.audio_round()
        if a.suite in ("fsy", "all"):
            s.fullscreen_bars()
        if a.suite == "kp":
            s.kp_scene()
        if a.suite in ("np", "vp"):
            s.np_scene()
            s.np_scene(hls=True)
        if a.suite in ("cp", "vp"):
            s.cp_scene()
            s.hls_scene()
        if a.suite in ("ap", "vp"):
            s.ap_scene()
        if a.suite == "yh":
            s.yahoo_fullscreen()                             # (only the live Yahoo Finance player in full screen with Kestrel's controls: about 4 minutes)
        if a.suite == "fsy":
            s.kp_scene()
            s.yahoo_fullscreen()
            s.slow_turn()
            s.audio_round()      # (the sound check rides along: its verdict is the page's own clock now, run 43745675957 showed the old one was wrong)
        if a.suite == "compat":
            s.compat_round(a.minutes)
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
