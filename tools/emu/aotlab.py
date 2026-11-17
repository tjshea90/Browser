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


def ls_files(text):
    """[(name, bytes)] of the regular files of an `ls -lR` listing (the oat directory: base.odex = compiled code, base.vdex = verified dex, base.art = app image if any)."""
    out = []
    for ln in text.splitlines():
        if ln.startswith("-"):
            parts = ln.split()
            if len(parts) >= 8 and parts[4].isdigit():
                out.append((parts[-1], int(parts[4])))
    return out


def pct(v, base):
    """' (+12 %)' of v against base, '' when either is missing."""
    if v is None or not base:
        return ""
    return " (%+.0f %%)" % ((v - base) / float(base) * 100.0)


def start_probe(pl, adb, srv, pkg, url, wait_s=60):
    """ONE start of the app on the lab's /ping page: `am start -W` TotalTime (first drawn frame) and the host-clock time from the command to the page's own load stamp (/report arriving at the lab server).
    The RELEASE build writes no Health lines, so this is how its start is measured end to end (engine, session, page load); the adb round trip is the same for every variant. page_ms is None when the page never reported."""
    n0 = len(srv.pings)
    t0 = time.time()
    out = adb.sh("am start -W -a android.intent.action.VIEW -d %s -p %s" % (url, pkg), timeout=120)
    total, wait = pl.parse_am_start(out)
    end = time.time() + wait_s
    t1 = None
    while time.time() < end:
        if len(srv.pings) > n0:
            t1 = srv.pings[n0][0]
            break
        time.sleep(0.2)
    return {"am_total_ms": total, "am_wait_ms": wait, "page_ms": int(round((t1 - t0) * 1000)) if t1 else None}


def report_update(L, upd_rows, upd_notes, med):
    """The LAB7b section of AOT-REPORT.md (appended to the list of lines L)."""
    if upd_rows or upd_notes:
        L.append("## LAB7b: the first launch after an update (debug build with Health lines: the old build is used twice, then `adb install -r` of the next version)")
        L.append("")
        L.append("| Scenario | Launch | n | am start -W ms | engine ready s | page loaded s | worst UI block ms | failed |")
        L.append("| :- | :- | -: | -: | -: | -: | -: | -: |")
        keys = ("am_total_ms", "runtime_ready_s", "page_loaded_s", "ui_block_ms")
        order = ["old build, 1st launch ever", "old build, 2nd launch (steady state)", "warm-up launch (not counted)", "new build, launch 1", "new build, launch 2", "new build, launch 3"]
        for scen in ("plain", "warmup"):
            steady = {k: med([r.get(k) for r in upd_rows if r["scenario"] == scen and r["launch"].startswith("old build, 2nd") and not r.get("crash")]) for k in keys}
            for ln_name in order:
                rs = [r for r in upd_rows if r["scenario"] == scen and r["launch"] == ln_name]
                if not rs:
                    continue
                ok = [r for r in rs if not r.get("crash")]
                m = {k: med([r.get(k) for r in ok]) for k in keys}

                def f(k, fmt, m=m, ln_name=ln_name, steady=steady):
                    v = m.get(k)
                    if v is None:
                        return "-"
                    return (fmt % v) + (pct(v, steady.get(k)) if ln_name.startswith("new build") else "")
                L.append("| %s | %s | %d | %s | %s | %s | %s | %d |" % (scen, ln_name, len(ok), f("am_total_ms", "%.0f"), f("runtime_ready_s", "%.2f"), f("page_loaded_s", "%.2f"), f("ui_block_ms", "%.0f"), len(rs) - len(ok)))
        L.append("")
        L.append("(percentages: the new build's launch against the SAME scenario's old-build steady state; `warmup` = one extra launch of the new build right after the update, force-stopped after 25 s, before the counted launches)")
        L.append("")
        for n in upd_notes:
            L.append("- " + n)
        L.append("")


def report_breadth(L, br_info, br_rows, br_mem, starts, med):
    """The LAB7a section of AOT-REPORT.md (appended to the list of lines L)."""
    if br_info or br_rows:
        L.append("## LAB7a: how much Android compiles at install, and what it costs (the RELEASE build; emulator: read the DIFFERENCES, the tablet's cores are slower)")
        L.append("")
        L.append("| Variant | what | status | compile s | compiled code MB | start -W warm ms | start -W cold ms | page loaded warm ms | page loaded cold ms | PSS app MB | PSS all MB |")
        L.append("| :- | :- | :- | -: | -: | -: | -: | -: | -: | -: | -: |")

        def vstat(name):
            inf = [i for i in br_info if i["variant"] == name]
            srs = [r for r in br_rows if r["variant"] == name]
            mm = [m for m in br_mem if m.get("variant") == name]

            def sm(key, cache):
                vals = [r[key] for r in srs if r["cache"] == cache and r.get(key) is not None]
                return med(vals) if vals else None
            app = [next((p.get("pss_kb") for p in m.get("procs", []) if p.get("name") == "app"), None) for m in mm]
            return {"status": ",".join(sorted(set(str(i["status"]) for i in inf))) or "-", "compile_s": med([i["compile_s"] for i in inf]) if inf else None,
                    "oat_mb": (med([i["oat_kb"] for i in inf]) / 1024.0) if inf else None,
                    "am_warm": sm("am_total_ms", "warm"), "am_cold": sm("am_total_ms", "cold"), "pg_warm": sm("page_ms", "warm"), "pg_cold": sm("page_ms", "cold"),
                    "pss_app": (med([x for x in app if x is not None]) / 1024.0) if any(x is not None for x in app) else None,
                    "pss_all": (med([m.get("total_pss_kb") for m in mm]) / 1024.0) if mm else None}
        base_name = "sp-current"
        bs = vstat(base_name)
        for name, apkkey, mode, what in BREADTH:
            s = vstat(name)
            if s["status"] == "-" and s["am_warm"] is None:
                continue

            def c(k, fmt):
                v = s.get(k)
                if v is None:
                    return "-"
                return (fmt % v) + (pct(v, bs.get(k)) if name != base_name else "")
            L.append("| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |" % (name, what, s["status"], c("compile_s", "%.1f"), c("oat_mb", "%.1f"), c("am_warm", "%.0f"), c("am_cold", "%.0f"),
                                                                             c("pg_warm", "%.0f"), c("pg_cold", "%.0f"), c("pss_app", "%.0f"), c("pss_all", "%.0f")))
        L.append("")
        L.append("(percentages against `%s`, the profile alpha65 ships; n = %d starts per cache state per variant and round; compile s = `cmd package compile -f -m MODE` on the emulator's 2 vCPUs)" % (base_name, starts // 2))
        L.append("")
        seen = set()
        for i in br_info:
            if i["variant"] not in seen:
                seen.add(i["variant"])
                L.append("- files of %s: %s; profile written: %s bytes" % (i["variant"], ", ".join(i["files"]) or "none", i.get("profile_bytes")))
        L.append("")


def selftest():
    sample ="  [app.kestrel.browser.gecko.debug]\n    path: /data/app/x==/base.apk\n      x86_64: [status=speed-profile] [reason=cmdline]\n  [com.android.email]\n    path: y\n      x86_64: [status=quicken] [reason=prebuilt]\n"
    assert parse_dexopt_status(sample, "app.kestrel.browser.gecko.debug") == ("speed-profile", "cmdline")
    assert parse_dexopt_status(sample, "com.other") == (None, None)
    assert sum_ls_bytes("/x/oat/x86_64:\ntotal 10\n-rw-r--r-- 1 system all 1000 2026-11-17 10:00 base.odex\n-rw-r--r-- 1 system all 24 2026-11-17 10:00 base.vdex\ndrwx------ 2 a b 4096 2026 d\n") == 1024
    assert ls_files("/x/oat/x86_64:\ntotal 10\n-rw-r--r-- 1 system all 1000 2026-11-17 10:00 base.odex\n-rw-r--r-- 1 system all 24 2026-11-17 10:00 base.vdex\ndrwx------ 2 a b 4096 2026 d\n") == [("base.odex", 1000), ("base.vdex", 24)]
    assert pct(110, 100) == " (+10 %)" and pct(None, 100) == "" and pct(5, 0) == ""
    # the LAB7 report sections render from synthetic rows (a typo found here costs nothing, one found in the lab run costs an hour)
    med = lambda v: (sorted(x for x in v if x is not None)[len([x for x in v if x is not None]) // 2] if [x for x in v if x is not None] else None)
    L = []
    report_update(L, [{"scenario": "plain", "round": 1, "launch": "old build, 2nd launch (steady state)", "am_total_ms": 1000, "runtime_ready_s": 1.2, "page_loaded_s": 2.0, "ui_block_ms": 300},
                      {"scenario": "plain", "round": 1, "launch": "new build, launch 1", "am_total_ms": 1500, "runtime_ready_s": 2.2, "page_loaded_s": 3.1, "ui_block_ms": 500},
                      {"scenario": "plain", "round": 1, "launch": "new build, launch 2", "crash": ["x"]}], ["a note"], med)
    txt = "\n".join(L)
    assert "new build, launch 1" in txt and "(+50 %)" in txt and "a note" in txt, txt
    L = []
    report_breadth(L, [{"variant": "sp-current", "round": 1, "status": "speed-profile", "compile_s": 20.0, "oat_kb": 4000, "files": ["base.odex 3900 KB"], "profile_bytes": 5000},
                       {"variant": "speed", "round": 1, "status": "speed", "compile_s": 80.0, "oat_kb": 20000, "files": ["base.odex 19000 KB"], "profile_bytes": None}],
                   [{"variant": "sp-current", "round": 1, "cache": "warm", "am_total_ms": 1700, "page_ms": 3000}, {"variant": "speed", "round": 1, "cache": "warm", "am_total_ms": 1600, "page_ms": 2900}],
                   [{"variant": "sp-current", "round": 1, "total_pss_kb": 500000, "procs": [{"name": "app", "pss_kb": 300000}]}], 4, med)
    txt = "\n".join(L)
    assert "| speed |" in txt and "(+300 %)" in txt and "files of speed" in txt, txt
    print("aotlab selftest ok")


def release_check(lab, pl, adb, a, say, rows, notes, progress):
    """The REAL path of the baseline profile, end to end on the release build: install it, let androidx.profileinstaller read assets/dexopt/baseline.prof (the R8-rewritten rules) and write it where Android
    looks for it (the INSTALL_PROFILE broadcast does what the library does itself 5 s after the first frame), compile with it, and compare against a plain `verify` of the SAME APK.
    Measured with the platform's own `am start -W` TotalTime (time to the first drawn frame of the activity, no log lines needed). Returns (rows, notes)."""
    pkg = a.release_pkg
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
                adb.sh("am start -W -a android.intent.action.VIEW -d %s -p %s" % (page, pkg), timeout=120)   # unmeasured first run after a compile (the release build writes no Health lines to logcat: am start -W is the measure)
                time.sleep(15)
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
                progress()
    finally:
        lab.pkg = old


def dm_install_check(lab, pl, adb, a, say, rows, progress):
    """Can the profile be compiled AT INSTALL TIME, and what does the installer do with a bad file? (LAB6a) A PackageInstaller session with the APK and its `.dm` (dex metadata: what Google Play does and what Kestrel's
    own updater would do). The updater writes `kestrel-update.apk`, so its names are used here too (a .dm is matched to its APK by name). Variants: a fresh install, an UPDATE over the installed copy, a .dm that is not a zip,
    a zip with a garbage primary.prof, and the other profile format (the `0` file). For every one: did the install succeed, the answer text of the installer, the compile status and reason, the compiled code size and whether
    the copy that was installed before is still intact after a failure. Fills [rows]."""
    import random
    import zipfile
    pkg = a.release_pkg
    apk = a.release_apk
    size = os.path.getsize(apk)
    remote = "/data/local/tmp/aot-release.apk"
    adb.run("push", apk, remote, timeout=900)
    dm1 = next((d for d in a.release_dm if "/1/" in d.replace(os.sep, "/")), (a.release_dm[0] if a.release_dm else None))
    dm0 = next((d for d in a.release_dm if "/0/" in d.replace(os.sep, "/")), None)
    junk = os.path.join(os.path.abspath(a.out), "not-a-zip.dm")
    with open(junk, "wb") as f:
        f.write(bytes(random.Random(7).randrange(256) for _ in range(300)))
    garbage = os.path.join(os.path.abspath(a.out), "garbage-profile.dm")
    with zipfile.ZipFile(garbage, "w") as z:
        z.writestr("primary.prof", bytes(random.Random(8).randrange(256) for _ in range(400)))

    def state():
        path = adb.sh("pm path %s" % pkg, timeout=30).strip().replace("package:", "")
        vc = adb.sh("dumpsys package %s | grep -m1 versionCode" % pkg, timeout=30).strip()
        st = parse_dexopt_status(adb.sh("dumpsys package dexopt", timeout=60), pkg)
        d = os.path.dirname(path) if path else ""
        oat = sum_ls_bytes(adb.sh("ls -lR %s/oat 2>/dev/null" % d, timeout=60)) if d else 0
        return {"installed": bool(path), "version": vc[:60], "status": st[0], "reason": st[1], "oat_kb": oat // 1024}

    def session(label, dm_path, fresh):
        if fresh:
            adb.run("uninstall", pkg, timeout=120)
        before = state()
        dsize = os.path.getsize(dm_path) if dm_path else 0
        t0 = time.time()
        out = adb.run("shell", "pm", "install-create", "-r", "-t", "-S", str(size + dsize), timeout=60)
        m = re.search(r"\[(\d+)\]", out)
        if not m:
            return {"label": label, "ok": False, "why": "no session: " + out.strip()[:100]}
        sid = m.group(1)
        r1 = adb.sh("pm install-write -S %d %s kestrel-update.apk %s" % (size, sid, remote), timeout=600)
        if dm_path:
            adb.run("push", dm_path, "/data/local/tmp/aot-release.dm", timeout=60)
            r1 += adb.sh("pm install-write -S %d %s kestrel-update.dm /data/local/tmp/aot-release.dm" % (dsize, sid), timeout=60)
        r2 = adb.sh("pm install-commit %s" % sid, timeout=900)
        took = round(time.time() - t0, 1)
        after = state()
        row = {"label": label, "ok": "Success" in r2, "install_s": took, "commit": (r2 + " " + r1).strip().replace("\n", " ")[:200], **{k: after[k] for k in ("status", "reason", "oat_kb")}}
        row["intact"] = (before["installed"] and after["installed"] and before["version"] == after["version"]) if before["installed"] else None
        return row

    plan = [("plain, no dm (fresh install)", None, True)]
    if dm1:
        plan += [("dm 1 (fresh install)", dm1, True), ("dm 1 as an UPDATE over the installed copy", dm1, False)]
    plan += [("dm that is not a zip, over the installed copy", junk, False), ("dm = zip with a garbage primary.prof, over the installed copy", garbage, False)]
    if dm0:
        plan += [("dm 0 (the other profile format), over the installed copy", dm0, False)]
    for label, dm_path, fresh in plan:
        rows.append(session(label, dm_path, fresh))
        say("dm check", rows[-1])
        progress()


# variant -> (which APK, compile mode, what it is). LAB7a: the install-time compile is `speed-profile` (the system's own choice for an install: pm.dexopt.install), which compiles ONLY the methods marked HOT in the
# profile; the profile decides how much is compiled. `speed` is the ceiling (everything), measured for its cost.
BREADTH = [
    ("verify", "base", "verify", "nothing compiled (what an update gets until the idle dexopt)"),
    ("sp-current", "base", "speed-profile", "alpha65 profile: only the methods marked hot (about 2.5k of 15k)"),
    ("sp-exercised", "exercised", "speed-profile", "every method the lab ever ran marked hot (about 4.9k)"),
    ("sp-all", "all", "speed-profile", "ALL methods marked hot (one wildcard rule, 15k), start-up / post-start-up flags kept"),
    ("speed", "base", "speed", "everything (the ceiling)"),
]


def breadth_check(lab, pl, adb, srv, a, say, info, rows, mem, notes, progress):
    """LAB7a. The RELEASE build (R8, not debuggable, the real profile) installed once per APK variant; for each variant `cmd package compile --reset`, the embedded profile put in place the way the library does (INSTALL_PROFILE),
    `cmd package compile -f -m MODE`, then timed starts (alternating warm / dropped file cache) on the lab's ping page and one memory snapshot. Per variant: compile seconds, compiled code size, files, start times, PSS.
    Variant APKs differ ONLY in assets/dexopt/baseline.prof (the profile rules of geckoapp/src/main/baseline-prof.txt, rewritten by R8). Fills [info] (one dict per compile), [rows] (one per start), [mem]."""
    pkg = a.release_pkg
    apks = {"base": a.release_apk, "exercised": a.apk_exercised, "all": a.apk_all}
    present = {k: v for k, v in apks.items() if v and os.path.exists(v)}
    page = "http://localhost:%d/ping" % a.port
    prof = "/data/misc/profiles/cur/0/%s/primary.prof" % pkg
    old = lab.pkg
    lab.pkg = pkg
    try:
        for rnd in range(1, a.breadth_rounds + 1):
            groups = [("base", ["verify", "sp-current", "speed"]), ("exercised", ["sp-exercised"]), ("all", ["sp-all"])]
            if rnd % 2 == 0:                                   # round 2 the other way round: a drifting runner cannot favour one variant
                groups = [(k, list(reversed(v))) for k, v in reversed(groups)]
            for key, names in groups:
                if key not in present:
                    notes.append("round %d: no APK for %s: variants %s skipped" % (rnd, key, ",".join(names)))
                    continue
                adb.run("uninstall", pkg, timeout=120)
                t0 = time.time()
                out = adb.run("install", "-t", present[key], timeout=900)
                say("install", key, out.strip()[:60], "%.0f s" % (time.time() - t0))
                if "Success" not in out:
                    notes.append("round %d: install of %s failed: %s" % (rnd, key, out.strip()[:120]))
                    continue
                for name in names:
                    _, _, mode, what = next(b for b in BREADTH if b[0] == name)
                    adb.sh("cmd package compile --reset %s" % pkg, timeout=300)
                    prof_size = None
                    lab.kill()
                    if mode == "speed-profile":
                        out = adb.sh("am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE %s/androidx.profileinstaller.ProfileInstallReceiver" % pkg, timeout=60)
                        for _ in range(30):
                            time.sleep(1.0)
                            sz = adb.sh("ls -ln %s 2>/dev/null" % prof, timeout=30).split()
                            if len(sz) >= 5 and sz[4].isdigit() and int(sz[4]) > 0:
                                prof_size = int(sz[4])
                                break
                    t0 = time.time()
                    adb.sh("cmd package compile -f -m %s %s" % (mode, pkg), timeout=1500)
                    took = round(time.time() - t0, 1)
                    st = parse_dexopt_status(adb.sh("dumpsys package dexopt", timeout=60), pkg)
                    path = adb.sh("pm path %s" % pkg, timeout=30).strip().replace("package:", "")
                    d = os.path.dirname(path) if path else ""
                    files = ls_files(adb.sh("ls -lR %s/oat 2>/dev/null" % d, timeout=60)) if d else []
                    rec = {"variant": name, "round": rnd, "apk": key, "mode": mode, "status": st[0], "compile_s": took, "oat_kb": sum(b for _, b in files) // 1024,
                           "files": ["%s %d KB" % (n, b // 1024) for n, b in files], "profile_bytes": prof_size}
                    info.append(rec)
                    say("compiled", rec)
                    lab.kill()
                    first = start_probe(pl, adb, srv, pkg, page)       # unmeasured: the first run after a compile pays for the engine's caches
                    say("first start (not counted)", first)
                    time.sleep(10)
                    for k in range(a.starts):
                        cold = (k % 2 == 1)
                        lab.kill()
                        if cold:
                            lab.drop_caches()
                        time.sleep(2)
                        r = start_probe(pl, adb, srv, pkg, page)
                        r.update({"variant": name, "round": rnd, "cache": "cold" if cold else "warm"})
                        rows.append(r)
                        say("start", name, r["cache"], r["am_total_ms"], r["page_ms"])
                        time.sleep(5)
                    lab.kill()                                         # memory: a fresh warm start, settle 10 s after the page loaded
                    r = start_probe(pl, adb, srv, pkg, page)
                    time.sleep(10)
                    lab.snapshot_memory(name, rnd)
                    mem.append(dict(lab.mem[-1], variant=name, round=rnd))
                    lab.kill()
                    progress()
    finally:
        lab.pkg = old


def update_check(lab, pl, adb, a, say, rows, notes, progress):
    """LAB7b: how much slower is the FIRST launch after an update than a later one, and does a warm-up launch right after the update remove it? Two debug builds of the same code, the second with the next versionCode
    and extension version (what an update looks like to the engine: a new install directory, a new built-in extension version). For each scenario: install the old build, launch it twice (the first builds the engine's
    caches, the second is the steady state), `adb install -r` the new build (= the update), [warm-up: launch it once, wait, force-stop], then three launches; every launch reads the app's Health lines (engine ready,
    page loaded). Also records Gecko's start-up cache files and compatibility.ini before and after the update (what the engine thinks changed). Fills [rows]."""
    pkg = a.pkg
    page = "http://localhost:%d/page/text" % a.port

    def state(label):
        out = adb.sh("find /data/data/%s -maxdepth 6 \\( -name 'startupCache*' -o -name compatibility.ini -o -name extensions.json -o -name 'scriptCache*' \\) 2>/dev/null | head -14" % pkg, timeout=60)
        lines = []
        for p in out.split():
            ls = adb.sh("ls -ln %s 2>/dev/null | head -4" % p, timeout=30).strip().replace("\n", " | ")
            lines.append(ls[:150])
            if p.endswith("compatibility.ini"):
                ini = adb.sh("cat %s 2>/dev/null" % p, timeout=30).strip().replace("\n", " ; ")
                lines.append("compatibility.ini: " + ini[:260])
        return "%s: %s" % (label, " || ".join(lines) if lines else "no start-up cache / compatibility.ini found under /data/data/%s" % pkg)

    for rnd in range(1, a.update_rounds + 1):
        for scen in (["plain", "warmup"] if rnd % 2 else ["warmup", "plain"]):
            say("=== update round %d scenario %s" % (rnd, scen))
            adb.run("uninstall", pkg, timeout=120)
            out = adb.run("install", "-t", a.old_apk, timeout=900)
            if "Success" not in out:
                notes.append("update round %d %s: install of the old build failed: %s" % (rnd, scen, out.strip()[:120]))
                continue

            def go(label, wait_after, count=True):
                lab.kill()
                r = lab.launch(page, 120)
                r.update({"scenario": scen, "round": rnd, "launch": label, "counted": count})
                rows.append(r)
                say("launch", scen, label, {k: r.get(k) for k in ("am_total_ms", "runtime_ready_s", "page_loaded_s", "ui_block_ms")}, "CRASH" if r.get("crash") else "")
                time.sleep(wait_after)
                return r
            go("old build, 1st launch ever", 20)
            go("old build, 2nd launch (steady state)", 20)
            if rnd == 1 and scen == "plain":
                notes.append(state("before the update (old build, after 2 launches)"))
            lab.kill()
            t0 = time.time()
            out = adb.run("install", "-r", "-t", a.new_apk, timeout=900)
            took = round(time.time() - t0, 1)
            if "Success" not in out:
                notes.append("update round %d %s: the update failed: %s" % (rnd, scen, out.strip()[:120]))
                continue
            if rnd == 1 and scen == "plain":
                notes.append("the update itself (adb install -r of the debug build) took %.1f s; " % took + state("right after the update"))
            if scen == "warmup":
                go("warm-up launch (not counted)", 25, count=False)
            go("new build, launch 1", 20)
            go("new build, launch 2", 20)
            go("new build, launch 3", 5)
            if rnd == 1 and scen == "plain":
                notes.append(state("after 3 launches of the new build"))
            lab.kill()
            progress()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="aot-out")
    ap.add_argument("--repo-root", default=".")
    ap.add_argument("--pkg", default="app.kestrel.browser.gecko.debug")
    ap.add_argument("--apk", default="")
    ap.add_argument("--release-apk", default="", help="the R8-minified release build for the emulator's ABI: its EMBEDDED baseline profile is installed by androidx.profileinstaller and compiled (the real release path)")
    ap.add_argument("--release-pkg", default="app.kestrel.browser.gecko")
    ap.add_argument("--release-dm", action="append", default=[], help="a dex-metadata file AGP wrote next to the release build (outputs/apk/release/baselineProfiles/<n>/*.dm); each one is tried as `base.dm` in a PackageInstaller session")
    ap.add_argument("--debug-ab", action="store_true", help="also compile the DEBUG build three ways: NOT informative (a debuggable app only ever gets quicken: lab run 44074456934), costs 15+ minutes, off by default")
    ap.add_argument("--dm-only", action="store_true", help="skip the timed starts of the release phase: only the compile-at-install installs (LAB6a, about 10 minutes with the release build)")
    ap.add_argument("--old-apk", default="", help="LAB7b: the debug build the update starts from")
    ap.add_argument("--new-apk", default="", help="LAB7b: the same code with the NEXT versionCode and extension version (the update)")
    ap.add_argument("--apk-exercised", default="", help="LAB7a: the release build whose profile marks every method the lab ran as hot")
    ap.add_argument("--apk-all", default="", help="LAB7a: the release build with one extra wildcard rule (HL**;->**(**)**): every method hot")
    ap.add_argument("--update-rounds", type=int, default=2)
    ap.add_argument("--breadth-rounds", type=int, default=2)
    ap.add_argument("--phases", default="release,dm", help="comma list of: update, breadth, release, dm, debug (the default is what the lab did before LAB7)")
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
    rel_rows, rel_notes, dm_rows = [], [], []
    br_info, br_rows, br_mem, upd_rows, upd_notes = [], [], [], [], []
    med = pl.median

    def say(*x):
        print(time.strftime("[%H:%M:%S] "), *x, flush=True)

    def write_report():
        """Written after every round / install: a cancelled run (the lab run 44069383826 was, after 9 minutes) keeps what it measured. Order of the phases = order of value: the release path, compile at install, then the debug A/B."""
        L = ["# Ahead-of-time compile A/B (LAB5b)", "",
             "%d rounds x %d starts per mode (alternating warm / cold file cache). **Emulator numbers: read the differences between modes of THIS run.**" % (a.rounds, a.starts), ""]
        report_update(L, upd_rows, upd_notes, med)
        report_breadth(L, br_info, br_rows, br_mem, a.starts, med)
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
        if dm_rows:
            L.append("## Compile at install: a PackageInstaller session with `base.apk` + `base.dm` (the dex metadata AGP writes next to the release build)")
            L.append("")
            L.append("| Install | ok | install s | status | reason | compiled code KB | old copy intact | answer of the installer |")
            L.append("| :- | :- | -: | :- | :- | -: | :- | :- |")
            for r in dm_rows:
                L.append("| %s | %s | %s | %s | %s | %s | %s | %s |" % (r.get("label"), r.get("ok"), r.get("install_s", "-"), r.get("status"), r.get("reason"), r.get("oat_kb", "-"), r.get("intact"), (r.get("commit") or r.get("why") or "")[:160].replace("|", "/")))
            L.append("")
        if rows:
            L.append("## The debug build compiled three ways (a profile trained in this run)")
            L.append("")
            L.append("| Mode | status seen | compile s (median) | compiled code MB | cache | n | am start -W ms | engine ready s | page loaded s | worst UI block ms |")
            L.append("| :- | :- | -: | -: | :- | -: | -: | -: | -: | -: |")
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
                        t = fmt % v
                        b = base.get(cache, {}).get(k)
                        if b and mode != MODES[0]:
                            t += " (%+.0f %%)" % ((v - b) / b * 100.0)
                        return t
                    st = status.get(mode, [])
                    L.append("| %s | %s | %s | %s | %s | %d | %s | %s | %s | %s |" % (mode, ",".join(sorted(set(str(x["status"]) for x in st))), med([x["compile_s"] for x in st]), "%.1f" % (med([x["oat_kb"] for x in st]) / 1024.0) if st else "-", cache, len(rs),
                                                                                      d("am_total_ms"), d("runtime_ready_s", "%.2f"), d("page_loaded_s", "%.2f"), d("ui_block_ms")))
            L.append("")
        for n in notes:
            L.append("- " + n)
        text = "\n".join(L) + "\n"
        with open(os.path.join(a.out, "AOT-REPORT.md"), "w") as f:
            f.write(text)
        with open(os.path.join(a.out, "aot-results.json"), "w") as f:
            json.dump({"starts": rows, "compile": status, "notes": notes, "release": rel_rows, "release_notes": rel_notes, "dm_install": dm_rows,
                       "breadth": {"compile": br_info, "starts": br_rows, "memory": br_mem}, "update": {"rows": upd_rows, "notes": upd_notes}}, f, indent=1)
        return text

    def debug_ab():
        adb.run("uninstall", pkg, timeout=120)
        say("install", adb.run("install", "-t", a.apk, timeout=600).strip()[:80])
        uid = adb.sh("stat -c %%u /data/data/%s" % pkg, timeout=30).strip()
        prof_dir = "/data/misc/profiles/cur/0/%s" % pkg
        prof = prof_dir + "/primary.prof"
        kept = "/data/local/tmp/aot-trained.prof"

        # training: two runs, each ends with SIGUSR1 so ART writes its profile
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
        notes.append("trained profile: " + adb.sh("ls -ln %s" % kept, timeout=30).strip()[:100])
        lab.kill()

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
                write_report()

    text = ""
    phases = set(p.strip() for p in a.phases.split(",") if p.strip())
    try:
        # order of value: the first launch after an update (cheap, decisive), the breadth of the install-time compile, then the older checks
        if "update" in phases:
            if a.old_apk and a.new_apk and os.path.exists(a.old_apk) and os.path.exists(a.new_apk):
                try:
                    update_check(lab, pl, adb, a, say, upd_rows, upd_notes, write_report)
                except Exception as e:
                    upd_notes.append("update check failed: %r" % (e,))
            else:
                upd_notes.append("update check skipped: --old-apk / --new-apk missing")
            write_report()
        if a.release_apk and os.path.exists(a.release_apk):
            if "breadth" in phases:
                try:
                    breadth_check(lab, pl, adb, srv, a, say, br_info, br_rows, br_mem, upd_notes, write_report)
                except Exception as e:
                    upd_notes.append("breadth check failed: %r" % (e,))
                write_report()
            try:
                if "release" in phases and not a.dm_only:
                    release_check(lab, pl, adb, a, say, rel_rows, rel_notes, write_report)
            except Exception as e:                          # the other phases must still run and be reported
                rel_notes.append("release check failed: %r" % (e,))
            write_report()
            if "dm" in phases and a.release_dm:
                try:
                    dm_install_check(lab, pl, adb, a, say, dm_rows, write_report)
                except Exception as e:
                    rel_notes.append("dm install check failed: %r" % (e,))
                write_report()
        if a.apk and (a.debug_ab or "debug" in phases):
            try:
                debug_ab()
            except Exception as e:
                notes.append("debug A/B failed: %r" % (e,))
    except KeyboardInterrupt:
        pass
    finally:
        text = write_report()
        print("\n" + text, flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
