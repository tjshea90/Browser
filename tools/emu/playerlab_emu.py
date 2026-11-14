#!/usr/bin/env python3
"""Does the ad skipper for OTHER video players (players.js, RX3-G7, research/YOUTUBE_ADS.md section 6) work in the REAL engine?

  python3 -u -I tools/emu/playerlab_emu.py [--port 8090] [--log <guardlab logfile>] [--wait 240]

Start the lab server first (python3 -B -I tools/sitelab/guardlab.py 8090 <logfile>) and install the debug APK (./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64; after a change to the extension's files without a
version change run `adb shell pm clear app.kestrel.browser.gecko.debug` first: a built-in extension is installed again only when its manifest version changes, and the first start after a clear takes about 70 s).
Force-stops the app, opens http://localhost:<port>/playerlab from the outside and waits for the page's DONE line, then prints what the page measured (how long each real 12 s ad took, at which speed it ran, which events
the skip button got) and the app's Health lines about the skipper. Emulator timings are NOT the tablet's: only "was it sped up / fast-forwarded / pressed" counts. Nothing here is part of the app.
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
    wait = int(a[a.index("--wait") + 1]) if "--wait" in a else 240
    adb("reverse", "tcp:%d" % port, "tcp:%d" % port)
    adb("shell", "am force-stop " + PKG)
    time.sleep(3)
    adb("logcat", "-c")
    off = size(log)
    t0 = time.time()
    adb("shell", "am start -a android.intent.action.VIEW -d http://localhost:%d/playerlab -p %s" % (port, PKG))
    print("cold start from a link at 0 s; waiting up to %d s for the five scenarios" % wait, flush=True)
    while time.time() - t0 < wait:
        if any(" DONE " in l or l.endswith(" DONE") or "DONE " in l for l in lines_since(log, off)):
            break
        time.sleep(3)
    time.sleep(3)
    print("--- what the page measured (%.0f s after the launch)" % (time.time() - t0))
    for l in lines_since(log, off):
        print("   ", l)
    print("--- app Health lines (other players' ads, extension state)")
    for l in adb("logcat", "-d", "-s", "KestrelHealth").splitlines():
        if re.search(r"playerads|ytads|ext-state", l, re.I):
            print("   ", l.split("KestrelHealth:", 1)[-1].strip())
