#!/usr/bin/env python3
"""Runs the Back lab (tools/sitelab/backlab.py) on the Android emulator: RX3-BK1 / RX3-BK2, research/BACK_SCROLL.md.

  python3 -u -I tools/emu/back_emu.py round <case> [--port 8089] [--log <lab logfile>] [--place 1800]
  cases: static | nostore | dynamic | loading | mse

One round = open /back-<case>?y=<place> in the debug build (the page scrolls itself there), tap a link (every block is one), wait for /back-next, press Back, and report:
  * the page's OWN view (ground truth, from the lab's logfile): was the show `persisted` (the engine's back/forward cache), the navigation type, the scroll place at +500 ms and +2000 ms after the
    show compared with the place it was left at, and whether the server saw a new GET for the page (= loaded again from the network),
  * the app's view (Health lines read with `adb logcat -s KestrelHealth`, debug builds only): back-arm, back-picture, the press line `kind=... y=a/b`, cover-hide ... y=a/b.
Start the server first (python3 tools/sitelab/backlab.py 8089 <logfile>) and boot the emulator (docs: HANDOFF.md section 3). The emulator runs in software mode: every step takes seconds, a page load a minute.
Nothing here is part of the app. Screenshots, if any, stay OUT of the repository (it is public).
"""
import os, re, subprocess, sys, time

ADB = os.environ.get("ADB", "/opt/android-sdk/platform-tools/adb")
PKG = os.environ.get("KESTREL_PKG", "app.kestrel.browser.gecko.debug")


def adb(*args, timeout=120):
    r = subprocess.run([ADB, *args], capture_output=True, text=True, timeout=timeout)
    return r.stdout


def shell(cmd, timeout=120):
    return adb("shell", cmd, timeout=timeout)


def size(path):
    return os.path.getsize(path) if os.path.exists(path) else 0


def lines_since(path, off):
    with open(path, "rb") as f:
        f.seek(off)
        return f.read().decode("utf-8", "replace").splitlines()


def wait_for(pred, timeout, what, step=1.0):
    t0 = time.time()
    while time.time() - t0 < timeout:
        v = pred()
        if v:
            return v
        time.sleep(step)
    raise TimeoutError("timed out after %ds waiting for %s" % (timeout, what))


def last_y(lines, case):
    ys = [int(m.group(1)) for ln in lines for m in [re.search(r"scrolled back-%s (\d+)" % case, ln)] if m]
    return ys[-1] if ys else 0


def health(pattern):
    out = adb("logcat", "-d", "-s", "KestrelHealth")
    return [ln.split("KestrelHealth:", 1)[-1].strip() for ln in out.splitlines() if re.search(pattern, ln)]


def round_(case, port, log, place=1800):
    url = "http://localhost:%d/back-%s?y=%d" % (port, case, place)
    adb("reverse", "tcp:%d" % port, "tcp:%d" % port)
    adb("logcat", "-c")
    off0 = size(log)
    shell("am start -a android.intent.action.VIEW -d %s -p %s" % (url, PKG))
    first = wait_for(lambda: [l for l in lines_since(log, off0) if "pageshow back-%s" % case in l], 300, "the first show of /back-" + case)
    print("opened:", first[0], flush=True)
    # the page scrolls ITSELF to ?y=<place> (see backlab.py: `input swipe` does not scroll a software-rendered emulator reliably); wait until it reports being there
    wait_for(lambda: last_y(lines_since(log, off0), case) >= place - 4, 120, "the page at y=%d" % place)
    time.sleep(2.5)  # the page's 300 ms scroll report reaches the app, and the app has seen it
    left_at = last_y(lines_since(log, off0), case)
    print("page is at y=%d, tapping a link" % left_at, flush=True)
    shell("input tap 300 480")
    wait_for(lambda: [l for l in lines_since(log, off0) if "pageshow back-next" in l], 300, "/back-next")
    time.sleep(2.0)
    off1 = size(log)
    t_press = time.time()
    shell("input keyevent KEYCODE_BACK")
    shown = wait_for(lambda: [l for l in lines_since(log, off1) if "pageshow back-%s" % case in l], 300, "the show after Back")
    ms_show = int((time.time() - t_press) * 1000)  # includes adb round trips on a slow emulator: compare kinds, not milliseconds
    wait_for(lambda: [l for l in lines_since(log, off1) if "y+2000 back-%s" % case in l], 60, "the +2000 ms reading")
    time.sleep(3.5)  # the lab closes a press 3 s after its show
    after = lines_since(log, off1)
    persisted = any("persisted=true" in l for l in after if "pageshow back-%s" % case in l)
    got = any(("GET /back-%s" % case) in l for l in after)
    y500 = next((int(l.split()[-2]) for l in after if "y+500 back-%s" % case in l), None)
    y2000 = next((int(l.split()[-2]) for l in after if "y+2000 back-%s" % case in l), None)
    print("--- page's own view after Back (%s)" % case)
    for l in after:
        print("   ", l)
    print("--- app's Health lines")
    for l in health(r"back|cover-|look-"):
        print("   ", l)
    verdict = "KEPT by the engine's cache" if persisted else ("loaded again FROM THE NETWORK" if got else "loaded again, answered by a cache")
    final = last_y(after, case) if any("scrolled back-%s" % case in l for l in after) else None
    place = final if final is not None else y2000   # the last scroll the page reported (the +500/+2000 ms timers can run late on a stalled emulator)
    ok = (place is not None and abs(place - left_at) <= 4)
    print("RESULT %-9s left_at=%-5d +500ms=%-5s +2000ms=%-5s last_scroll=%-5s place_kept=%-5s %s (wall %d ms incl. adb)" % (case, left_at, y500, y2000, final, ok, verdict, ms_show))
    return dict(case=case, left_at=left_at, y500=y500, y2000=y2000, ok=ok, persisted=persisted, network=got)


if __name__ == "__main__":
    a = sys.argv[1:]
    if len(a) < 2 or a[0] != "round":
        print(__doc__); sys.exit(2)
    port = int(a[a.index("--port") + 1]) if "--port" in a else 8089
    log = a[a.index("--log") + 1] if "--log" in a else "backlab.log"
    place = int(a[a.index("--place") + 1]) if "--place" in a else 1800
    round_(a[1], port, log, place)
