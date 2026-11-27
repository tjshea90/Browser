#!/usr/bin/env python3
"""Is the pop-up guard there for the VERY FIRST page after a cold start? (RX3-G1, research/GUARD_FIRST_PAGE.md)

  python3 -u -I tools/emu/firstpage_emu.py [--port 8090] [--log <guardlab logfile>] [--wait 40]

Force-stops the debug build, opens http://localhost:<port>/fakeplay from the outside (a link launch: the first document of a cold process), waits, and reports what tools/sitelab/guardlab.py saw:
  * "GET /popad n"  = the pop-up of attempt n was NOT stopped (the guard was not in that document),
  * the page's own "attempt n ... window.open returned ..." lines,
  * the app's Health lines about pop-ups and the guard (adb logcat -s KestrelHealth).
Run it twice (before and after a change) and compare. Start the server first (python3 tools/sitelab/guardlab.py 8090 <logfile>). Nothing here is part of the app.
"""
import os, re, subprocess, sys, time

ADB = os.environ.get("ADB", "/opt/android-sdk/platform-tools/adb")
PKG = os.environ.get("KESTREL_PKG", "app.kestrel.browser.gecko.debug")


def adb(*args, timeout=120):
    return subprocess.run([ADB, *args], capture_output=True, text=True, timeout=timeout).stdout


def size(p):
    return os.path.getsize(p) if os.path.exists(p) else 0


def lines_since(p, off):
    with open(p, "rb") as f:
        f.seek(off)
        return f.read().decode("utf-8", "replace").splitlines()


if __name__ == "__main__":
    a = sys.argv[1:]
    port = int(a[a.index("--port") + 1]) if "--port" in a else 8090
    log = a[a.index("--log") + 1] if "--log" in a else "guardlab.log"
    wait = int(a[a.index("--wait") + 1]) if "--wait" in a else 40
    adb("reverse", "tcp:%d" % port, "tcp:%d" % port)
    adb("shell", "am force-stop " + PKG)
    time.sleep(3)
    adb("logcat", "-c")
    off = size(log)
    t0 = time.time()
    adb("shell", "am start -a android.intent.action.VIEW -d http://localhost:%d/fakeplay -p %s" % (port, PKG))
    print("cold start from a link at 0 s; waiting %d s for four pop-up attempts (the last is 15 s after the page loaded)" % wait, flush=True)
    deadline = time.time() + wait + 240
    while time.time() < deadline:
        got = lines_since(log, off)
        if any("attempt 4" in l for l in got):
            break
        time.sleep(3)
    time.sleep(5)
    got = lines_since(log, off)
    print("--- what the lab server saw (%.0f s after the launch)" % (time.time() - t0))
    for l in got:
        print("   ", l)
    stopped = [n for n in "1234" if not any(("GET /popad %s" % n) in l for l in got)]
    print("--- app Health lines (pop-ups, guard)")
    for l in adb("logcat", "-d", "-s", "KestrelHealth").splitlines():
        if re.search(r"popup|guard|pop-up|blocked", l, re.I):
            print("   ", l.split("KestrelHealth:", 1)[-1].strip())
    print("RESULT attempts stopped: %s of 4 (a number missing here means that pop-up got through)" % ",".join(stopped or ["none"]))
