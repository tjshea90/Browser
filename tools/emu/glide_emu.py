#!/usr/bin/env python3
"""Drives Kestrel's mini keyboard on the Android emulator with REAL multi-point touches (AT1h / AX1).

`adb shell input swipe` can only draw a straight line, and a glide over letters is a bent path, so the touch is written to the emulator's Linux input device as binary `struct input_event`
records (needs `adb root`, which the emulator allows). Everything here is a helper for a developer machine / the sandbox next to the emulator; nothing is part of the app.

  python3 -I tools/emu/glide_emu.py layout               print the 26 letter key centres (screen pixels) found from the keyboard view's bounds
  python3 -I tools/emu/glide_emu.py glide hello          glide over the letters of a word
  python3 -I tools/emu/glide_emu.py glide for o=-1.0     ... with the finger landing one key LEFT of 'o' (letter=dx[,dy] in key widths): a sloppy habit
  python3 -I tools/emu/glide_emu.py tap 300 400          tap (screen pixels)
  python3 -I tools/emu/glide_emu.py longpress 580 80     long press (screen pixels, 800 ms): the invisible top right corner opens / closes the panel
  python3 -I tools/emu/glide_emu.py shot out.png         screenshot (keep it OUT of the repository: the repo is public)
  python3 -I tools/emu/glide_emu.py taptext "Settings"   tap the first view whose text or description contains this

Key centres come from the same formula the keyboard uses (core/input/MiniKeyboardGeometry: rows share the width by weight, 3 dp gaps, a 30 dp candidate strip above the first row when glide typing is on).
"""
import os, re, struct, subprocess, sys, tempfile, time
import xml.etree.ElementTree as ET

ADB = os.environ.get("ADB", "/opt/android-sdk/platform-tools/adb")
PKG = os.environ.get("KESTREL_PKG", "app.kestrel.browser.gecko.debug")

def adb(*args, check=True, text=True, timeout=120):
    r = subprocess.run([ADB, *args], capture_output=True, text=text, timeout=timeout)
    if check and r.returncode != 0:
        raise RuntimeError("adb %s failed: %s" % (" ".join(args), r.stderr.strip()))
    return r.stdout

def shell(cmd, **kw):
    return adb("shell", cmd, **kw)

_dev = {}
def touch_device():
    """(path, max_x, max_y) of the emulator's multi-touch device."""
    if _dev: return _dev["v"]
    out = shell("getevent -p")
    path = None; mx = my = 32767
    for block in out.split("add device")[1:]:
        m = re.search(r"(/dev/input/event\d+)", block)
        if m and "multi_touch" in block:
            path = m.group(1)
            for code, name in (("0035", "x"), ("0036", "y")):
                mm = re.search(code + r"\s*:\s*value \d+, min \d+, max (\d+)", block)
                if mm:
                    if name == "x": mx = int(mm.group(1))
                    else: my = int(mm.group(1))
            break
    if not path: raise RuntimeError("no multi-touch input device found")
    _dev["v"] = (path, mx, my)
    return _dev["v"]

def screen_size():
    m = re.search(r"(\d+)x(\d+)", shell("wm size"))
    return int(m.group(1)), int(m.group(2))

def density():
    m = re.search(r"(\d+)", shell("wm density"))
    return int(m.group(1)) / 160.0

def dump():
    """The uiautomator tree as an ElementTree root."""
    shell("uiautomator dump /sdcard/kestrel_ui.xml", check=False)
    xml = adb("exec-out", "cat", "/sdcard/kestrel_ui.xml")
    return ET.fromstring(xml)

def bounds(node):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
    return tuple(int(v) for v in m.groups()) if m else None

def find(root, text):
    for n in root.iter("node"):
        if text.lower() in (n.get("text", "") + " " + n.get("content-desc", "")).lower() and bounds(n):
            return n
    return None

def keyboard_bounds(root=None):
    root = root or dump()
    n = find(root, "Keyboard")
    for cand in root.iter("node"):
        if cand.get("content-desc") == "Keyboard" and bounds(cand): n = cand; break
    if n is None: raise RuntimeError("the keyboard view is not on screen (open the panel and tap the bar)")
    return bounds(n)

TOP = [(c, 1.0) for c in "qwertyuiop"]
HOME = [(None, 0.5)] + [(c, 1.0) for c in "asdfghjkl"] + [(None, 0.5)]
BOTTOM = [(None, 1.5)] + [(c, 1.0) for c in "zxcvbnm"] + [(None, 1.5)]
ROWS = [TOP, HOME, BOTTOM, [(None, 10.0)]]

def layout(b, glide=True):
    """Letter -> (x, y) screen pixel of its key centre, for a keyboard view with bounds b = (l, t, r, b)."""
    d = density(); gap = 3 * d; strip = (30 * d) if glide else 0.0
    l, t, r, bt = b; w = r - l; h = bt - t
    rowH = (h - strip - gap * (len(ROWS) - 1)) / len(ROWS)
    out = {}
    for i, row in enumerate(ROWS):
        total = sum(wt for _, wt in row); unit = (w - gap * (len(row) - 1)) / total
        x = 0.0; y = strip + i * (rowH + gap)
        for c, wt in row:
            if c: out[c] = (l + x + wt * unit / 2, t + y + rowH / 2)
            x += wt * unit + gap
    return out

def path_points(word, keys, shifts=None, step_px=5.0):
    """Straight lines between the centres of the word's letters; shifts = {letter: (dx, dy)} in key widths moves where the finger lands for that letter."""
    pitch = keys["p"][0] - keys["o"][0]; pitch = -pitch if pitch < 0 else pitch
    pitch = (keys["p"][0] - keys["q"][0]) / 9.0
    cs = []
    for ch in word:
        x, y = keys[ch]
        if shifts and ch in shifts: x += shifts[ch][0] * pitch; y += shifts[ch][1] * pitch
        if not cs or cs[-1] != (x, y): cs.append((x, y))
    pts = [cs[0]]
    for (ax, ay), (bx, by) in zip(cs, cs[1:]):
        n = max(2, int(((bx - ax) ** 2 + (by - ay) ** 2) ** 0.5 / step_px))
        for s in range(1, n + 1): pts.append((ax + (bx - ax) * s / n, ay + (by - ay) * s / n))
    return pts

def _ev(t, c, v): return struct.pack("<qqHHi", 0, 0, t, c, v)

def inject(points, per_chunk=4, pause=0.03, track=1):
    """Press, move through the points, lift: one chunk of touch events every `pause` seconds (the app batches what arrives within a frame)."""
    path, mx, my = touch_device(); sw, sh = screen_size()
    def raw(p): return int(round(p[0] / sw * mx)), int(round(p[1] / sh * my))
    chunks = []
    x, y = raw(points[0])
    chunks.append(_ev(3, 0x2f, 0) + _ev(3, 0x39, track) + _ev(3, 0x35, x) + _ev(3, 0x36, y) + _ev(3, 0x3a, 100) + _ev(0, 0, 0))
    mids = points[1:-1]
    for i in range(0, len(mids), per_chunk):
        b = b""
        for p in mids[i:i + per_chunk]:
            x, y = raw(p); b += _ev(3, 0x35, x) + _ev(3, 0x36, y) + _ev(0, 0, 0)
        chunks.append(b)
    x, y = raw(points[-1])
    chunks.append(_ev(3, 0x35, x) + _ev(3, 0x36, y) + _ev(0, 0, 0) + _ev(3, 0x39, -1) + _ev(0, 0, 0))
    with tempfile.TemporaryDirectory() as td:
        script = ["set -e"]
        for i, c in enumerate(chunks):
            f = os.path.join(td, "c%d.bin" % i); open(f, "wb").write(c)
            adb("push", f, "/data/local/tmp/kg_c%d.bin" % i)
            script.append("cat /data/local/tmp/kg_c%d.bin > %s; sleep %s" % (i, path, pause))
        script.append("rm -f /data/local/tmp/kg_c*.bin")
        shell("; ".join(script))

def glide(word, shifts=None, keys=None):
    keys = keys or layout(keyboard_bounds())
    inject(path_points(word, keys, shifts))

def tap(x, y): shell("input tap %d %d" % (x, y))
def longpress(x, y, ms=800): shell("input swipe %d %d %d %d %d" % (x, y, x, y, ms))
def shot(path): open(path, "wb").write(adb("exec-out", "screencap", "-p", text=False))

def taptext(text, retries=1):
    for _ in range(retries + 1):
        n = find(dump(), text)
        if n:
            l, t, r, b = bounds(n); tap((l + r) // 2, (t + b) // 2); return True
        shell("input swipe 300 700 300 300 300")
    return False

def parse_shifts(items):
    out = {}
    for it in items:
        if "=" in it:
            k, v = it.split("=", 1); xs = [float(z) for z in v.split(",")]
            out[k] = (xs[0], xs[1] if len(xs) > 1 else 0.0)
    return out

if __name__ == "__main__":
    a = sys.argv[1:]
    if not a: print(__doc__); sys.exit(0)
    if a[0] == "layout":
        b = keyboard_bounds(); k = layout(b); print("keyboard view bounds", b)
        for c in sorted(k): print(c, "%.0f %.0f" % k[c])
    elif a[0] == "glide": glide(a[1], parse_shifts(a[2:]))
    elif a[0] == "tap": tap(int(a[1]), int(a[2]))
    elif a[0] == "longpress": longpress(int(a[1]), int(a[2]))
    elif a[0] == "shot": shot(a[1])
    elif a[0] == "taptext": print("tapped" if taptext(a[1]) else "not found")
    else: print(__doc__)
