#!/usr/bin/env python3
"""LAB5b: the ahead-of-time compile A/B of the emulator lab (what a baseline profile is worth).

  python3 -I tools/emu/aotlab.py --out DIR --repo-root . --pkg app.kestrel.browser.gecko.debug --apk path/to.apk --rounds 3 --starts 4

One APK, installed once. TRAINING: the app is started and driven over a few lab pages twice, each run ends with SIGUSR1 (ART writes the in-memory JIT profile of the process into
/data/misc/profiles/cur/0/<pkg>/primary.prof), and that profile is kept. Then, for every round and every MODE in rotated order, the package is reset (`cmd package compile --reset`), the trained profile is put back
for `speed-profile`, and the package is compiled with `cmd package compile -f -m MODE`:

  verify         what a sideloaded install gets (Android 10 on a device with no profile): the dex code is verified and RUNS IN THE INTERPRETER until the JIT warms up (today's alpha63 state)
  speed-profile  what Android's idle-and-charging dexopt makes of an app that has a baseline profile: the hot methods of the profile are compiled ahead of time
  speed          everything compiled (the ceiling, and the oat size to compare)

then it takes N cold starts (alternating warm / dropped file cache) to a lab page and reports medians (`am start -W`, engine ready, page loaded, longest UI block) and the size of the compiled code.
An x86 emulator: only the DIFFERENCES between modes of this one run mean anything (the engine's native start-up is the same in all three). Standard library only.
"""
import argparse
import importlib.util
import json
import os
import re
import sys
import time

MODES = ["verify", "speed-profile", "speed"]


def load_packlab(root):
    spec = importlib.util.spec_from_file_location("packlab", os.path.join(root, "tools", "emu", "packlab.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def parse_dexopt_status(text, pkg):
    """The `[status=..] [reason=..]` of the package's first ISA from `dumpsys package dexopt`."""
    block = False
    for ln in text.splitlines():
        if "[%s]" % pkg in ln:
            block = True
            continue
        if block:
            m = re.search(r"\[status=([\w-]+)\]\s*\[reason=([\w-]+)\]", ln)
            if m:
                return m.group(1), m.group(2)
            if re.match(r"^\s*\[", ln):
                block = False
    return None, None


def sum_ls_bytes(text):
    """Total size of the regular files in an `ls -lR` listing (column 5 of every `-rw...` line)."""
    total = 0
    for ln in text.splitlines():
        if ln.startswith("-"):
            parts = ln.split()
            if len(parts) >= 5 and parts[4].isdigit():
                total += int(parts[4])
    return total


def selftest():
    sample = "  [app.kestrel.browser.gecko.debug]\n    path: /data/app/x==/base.apk\n      x86_64: [status=speed-profile] [reason=cmdline]\n  [com.android.email]\n    path: y\n      x86_64: [status=quicken] [reason=prebuilt]\n"
    assert parse_dexopt_status(sample, "app.kestrel.browser.gecko.debug") == ("speed-profile", "cmdline")
    assert parse_dexopt_status(sample, "com.other") == (None, None)
    assert sum_ls_bytes("/x/oat/x86_64:\ntotal 10\n-rw-r--r-- 1 system all 1000 2026-11-17 10:00 base.odex\n-rw-r--r-- 1 system all 24 2026-11-17 10:00 base.vdex\ndrwx------ 2 a b 4096 2026 d\n") == 1024
    print("aotlab selftest ok")


def release_check(lab, pl, adb, a, say):
    """The REAL path of the baseline profile, end to end on the release build: install it, let androidx.profileinstaller read assets/dexopt/baseline.prof (the R8-rewritten rules) and write it where Android
    looks for it (the INSTALL_PROFILE broadcast does what the library does itself 5 s after the first frame), compile with it, and compare against a plain `verify` of the SAME APK.
    Measured with the platform's own `am start -W` TotalTime (time to the first drawn frame of the activity, no log lines needed). Returns (rows, notes)."""
    pkg = a.release_pkg
    rows, notes = [], []
    adb.run("uninstall", pkg, timeout=120)
    say("install release", adb.run("install", "-t", a.release_apk, timeout=900).strip()[:80])
    uid = adb.sh("stat -c %%u /data/data/%s" % pkg, timeout=30).strip()
    prof = "/data/misc/profiles/cur/0/%s/primary.prof" % pkg
    page = "http://localhost:%d/page/text" % a.port
    old = lab.pkg
    lab.pkg = pkg                                           # the lab helpers kill / launch the package they were made for
    try:
        for rnd in range(1, a.rounds + 1):
            for mode in (["verify", "speed-profile"] if rnd % 2 else ["speed-profile", "verify"]):
                adb.sh("cmd package compile --reset %s" % pkg, timeout=300)
                prof_size = None
                if mode == "speed-profile":
                    lab.kill()
                    out = adb.sh("am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE %s/androidx.profileinstaller.ProfileInstallReceiver" % pkg, timeout=60)
                    for _ in range(20):
                        time.sleep(1.0)
                        sz = adb.sh("ls -ln %s 2>/dev/null" % prof, timeout=30).split()
                        if len(sz) >= 5 and sz[4].isdigit() and int(sz[4]) > 0:
                            prof_size = int(sz[4])
                            break
                    notes.append("round %d: INSTALL_PROFILE -> %s ; profile written: %s bytes" % (rnd, out.strip().replace("\n", " ")[:90], prof_size))
                out = adb.sh("cmd package compile -f -m %s %s" % (mode, pkg), timeout=900)
                st = parse_dexopt_status(adb.sh("dumpsys package dexopt", timeout=60), pkg)
                path = adb.sh("pm path %s" % pkg, timeout=30).strip().replace("package:", "")
                d = os.path.dirname(path) if path else ""
                oat = sum_ls_bytes(adb.sh("ls -lR %s/oat 2>/dev/null" % d, timeout=60)) if d else 0
                info = {"mode": mode, "round": rnd, "status": st[0], "oat_kb": oat // 1024, "profile_bytes": prof_size}
                say("release compiled", info)
                notes.append("round %d %s: status=%s oat=%d KB" % (rnd, mode, st[0], oat // 1024))
                lab.kill()
                first = lab.launch(page, 120)               # (the release build writes no Health lines to logcat: page_loaded stays empty; am start -W is the measure)
                time.sleep(3)
                for k in range(a.starts):
                    cold = (k % 2 == 1)
                    lab.kill()
                    if cold:
                        lab.drop_caches()
                    time.sleep(2)
                    out = adb.sh("am start -W -a android.intent.action.VIEW -d %s -p %s" % (page, pkg), timeout=90)
                    total, wait = pl.parse_am_start(out)
                    rows.append({"mode": mode, "round": rnd, "cache": "cold" if cold else "warm", "am_total_ms": total, "am_wait_ms": wait})
                    say("release start", mode, "cache=" + ("cold" if cold else "warm"), total)
                    time.sleep(6)
                lab.kill()
    finally:
        lab.pkg = old
    return rows, notes


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="aot-out")
    ap.add_argument("--repo-root", default=".")
    ap.add_argument("--pkg", default="app.kestrel.browser.gecko.debug")
    ap.add_argument("--apk", default="")
    ap.add_argument("--release-apk", default="", help="the R8-minified release build for the emulator's ABI: its EMBEDDED baseline profile is installed by androidx.profileinstaller and compiled (the real release path)")
    ap.add_argument("--release-pkg", default="app.kestrel.browser.gecko")
    ap.add_argument("--rounds", type=int, default=3)
    ap.add_argument("--starts", type=int, default=4)
    ap.add_argument("--port", type=int, default=8093)
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args()
    root = os.path.abspath(a.repo_root)
    pl = load_packlab(root)
    if a.selftest:
        selftest()
        pl.selftest()
        return 0
    os.makedirs(a.out, exist_ok=True)

    class Args:
        pkg = a.pkg
        port = a.port
        starts = a.starts
        rounds = a.rounds

    lab = pl.Lab(Args)
    adb = lab.adb
    adb.run("root", timeout=60)
    time.sleep(3)
    adb.run("wait-for-device", timeout=120)
    lab.rooted = "uid=0" in adb.sh("id", timeout=30)
    srv, _ = pl.start_server(a.port, root, os.path.join(os.path.abspath(a.out), "lab-tmp"))
    adb.run("reverse", "tcp:%d" % a.port, "tcp:%d" % a.port)
    for s in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
        adb.sh("settings put global %s 0" % s)
    adb.sh("svc power stayon true")
    adb.sh("input keyevent KEYCODE_WAKEUP")
    adb.sh("wm dismiss-keyguard")
    pkg = a.pkg
    page = "http://localhost:%d/page/text" % a.port
    rows, status, notes = [], {}, []

    def say(*x):
        print(time.strftime("[%H:%M:%S] "), *x, flush=True)

    # ---- install once
    adb.run("uninstall", pkg, timeout=120)
    say("install", adb.run("install", "-t", a.apk, timeout=600).strip()[:80])
    uid = adb.sh("stat -c %%u /data/data/%s" % pkg, timeout=30).strip()
    prof_dir = "/data/misc/profiles/cur/0/%s" % pkg
    prof = prof_dir + "/primary.prof"
    kept = "/data/local/tmp/aot-trained.prof"

    # ---- training: two runs, each ends with SIGUSR1 so ART writes its profile
    adb.sh("cmd package compile --reset %s" % pkg, timeout=300)
    for cycle in (1, 2):
        lab.kill()
        r = lab.launch(page, 120)
        say("training start", cycle, {k: r.get(k) for k in ("am_total_ms", "runtime_ready_s", "page_loaded_s")}, "CRASH" if r.get("crash") else "")
        for p in ("/page/images", "/bench2", "/page/text"):
            adb.sh("am start -a android.intent.action.VIEW -d http://localhost:%d%s -p %s" % (a.port, p, pkg), timeout=60)
            time.sleep(8)
        time.sleep(6)
        pids = [int(x) for x in adb.sh("pidof %s" % pkg, timeout=30).split() if x.isdigit()] or [pid for pid, name in lab.pids() if name == pkg]
        for pid in pids:
            adb.sh("kill -s SIGUSR1 %d" % pid, timeout=30)
        time.sleep(4)
        size = adb.sh("ls -ln %s" % prof, timeout=30)
        say("profile after cycle", cycle, size.strip()[:100])
    adb.sh("cp %s %s" % (prof, kept), timeout=30)
    trained = adb.sh("ls -ln %s" % kept, timeout=30).strip()
    notes.append("trained profile: " + trained[:100])
    lab.kill()

    # ---- the modes
    def prepare(mode):
        adb.sh("cmd package compile --reset %s" % pkg, timeout=300)
        if mode == "speed-profile":
            adb.sh("mkdir -p %s; cp %s %s; chown %s:%s %s; chmod 600 %s; restorecon -R %s" % (prof_dir, kept, prof, uid, uid, prof, prof, prof_dir), timeout=60)
        t0 = time.time()
        out = adb.sh("cmd package compile -f -m %s %s" % (mode, pkg), timeout=900)
        took = round(time.time() - t0, 1)
        st = parse_dexopt_status(adb.sh("dumpsys package dexopt", timeout=60), pkg)
        path = adb.sh("pm path %s" % pkg, timeout=30).strip().replace("package:", "")
        d = os.path.dirname(path) if path else ""
        oat = sum_ls_bytes(adb.sh("ls -lR %s/oat 2>/dev/null" % d, timeout=60)) if d else 0
        status.setdefault(mode, []).append({"compile_s": took, "status": st[0], "reason": st[1], "oat_kb": oat // 1024, "out": out.strip()[:120]})
        say("compiled", mode, status[mode][-1])

    for rnd in range(1, a.rounds + 1):
        order = MODES[(rnd - 1) % len(MODES):] + MODES[:(rnd - 1) % len(MODES)]
        for mode in order:
            say("=== round %d mode %s" % (rnd, mode))
            prepare(mode)
            lab.kill()
            first = lab.launch(page, 120)              # unmeasured: the first run after a compile pays for caches
            if first.get("crash"):
                notes.append("round %d %s: first launch failed %s" % (rnd, mode, first["crash"]))
                continue
            time.sleep(4)
            for k in range(a.starts):
                cold = (k % 2 == 1)
                lab.kill()
                if cold:
                    lab.drop_caches()
                time.sleep(2)
                r = lab.launch(page, 90)
                r.update({"mode": mode, "round": rnd, "cache": "cold" if cold else "warm"})
                rows.append(r)
                say("start", mode, r["cache"], {x: r.get(x) for x in ("am_total_ms", "runtime_ready_s", "page_loaded_s", "ui_block_ms")}, "CRASH" if r.get("crash") else "")
            lab.kill()

    rel_rows, rel_notes = [], []
    if a.release_apk and os.path.exists(a.release_apk):
        try:
            rel_rows, rel_notes = release_check(lab, pl, adb, a, say)
        except Exception as e:                              # the debug A/B above must still be reported
            rel_notes.append("release check failed: %r" % (e,))

    med = pl.median
    L = ["# Ahead-of-time compile A/B (LAB5b)", "",
         "One APK, three compile states (`cmd package compile -f -m MODE`), %d rounds x %d starts per mode (alternating warm / cold file cache). **Emulator numbers: read the differences between modes of THIS run.**" % (a.rounds, a.starts), "",
         "| Mode | status seen | compile s (median) | compiled code MB | cache | n | am start -W ms | engine ready s | page loaded s | worst UI block ms |", "| :- | :- | -: | -: | :- | -: | -: | -: | -: | -: |"]
    base = {}
    for mode in MODES:
        for cache in ("warm", "cold"):
            rs = [r for r in rows if r["mode"] == mode and r["cache"] == cache and not r.get("crash")]
            if not rs:
                continue
            m = {k: med([r.get(k) for r in rs]) for k in ("am_total_ms", "runtime_ready_s", "page_loaded_s", "ui_block_ms")}
            if mode == MODES[0]:
                base[cache] = m

            def d(k, fmt="%.0f"):
                v = m.get(k)
                if v is None:
                    return "-"
                s = fmt % v
                b = base.get(cache, {}).get(k)
                if b and mode != MODES[0]:
                    s += " (%+.0f %%)" % ((v - b) / b * 100.0)
                return s
            st = status.get(mode, [])
            L.append("| %s | %s | %s | %s | %s | %d | %s | %s | %s | %s |" % (mode, ",".join(sorted(set(str(s["status"]) for s in st))), med([s["compile_s"] for s in st]), "%.1f" % (med([s["oat_kb"] for s in st]) / 1024.0) if st else "-", cache, len(rs),
                                                                              d("am_total_ms"), d("runtime_ready_s", "%.2f"), d("page_loaded_s", "%.2f"), d("ui_block_ms")))
    L.append("")
    if rel_rows or rel_notes:
        L.append("## The release build, the embedded baseline profile (R8-rewritten rules, installed by androidx.profileinstaller): `am start -W` TotalTime")
        L.append("")
        L.append("| Mode | cache | n | TotalTime ms (median) |")
        L.append("| :- | :- | -: | -: |")
        for mode in ("verify", "speed-profile"):
            for cache in ("warm", "cold"):
                rs = [r["am_total_ms"] for r in rel_rows if r["mode"] == mode and r["cache"] == cache and r["am_total_ms"] is not None]
                if rs:
                    b = med([r["am_total_ms"] for r in rel_rows if r["mode"] == "verify" and r["cache"] == cache and r["am_total_ms"] is not None])
                    v = med(rs)
                    L.append("| %s | %s | %d | %.0f%s |" % (mode, cache, len(rs), v, (" (%+.0f %%)" % ((v - b) / b * 100.0)) if (mode != "verify" and b) else ""))
        L.append("")
        for n in rel_notes:
            L.append("- " + n)
        L.append("")
    for n in notes:
        L.append("- " + n)
    text = "\n".join(L) + "\n"
    with open(os.path.join(a.out, "AOT-REPORT.md"), "w") as f:
        f.write(text)
    with open(os.path.join(a.out, "aot-results.json"), "w") as f:
        json.dump({"starts": rows, "compile": status, "notes": notes, "release": rel_rows, "release_notes": rel_notes}, f, indent=1)
    print("\n" + text, flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
