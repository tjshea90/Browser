#!/usr/bin/env python3
"""LAB5b: the engine preference sweep of the emulator lab.

  python3 -I tools/emu/prefsweep.py --out DIR --repo-root . --pkg app.kestrel.browser.gecko.debug --rounds 5 \
      --variant base= --variant base2= --variant stylo2=layout.css.stylo-threads=2 --variant gcpar="javascript.options.parallel_marking=true" ...

A variant is NAME=pref=value[;pref=value...] (up to 8 prefs, `pref=-` removes a pref from the base config, an empty list = the app's own config). The debug build of the app reads the
system properties debug.kestrel.gp1..gp8 into the engine config file BEFORE the engine starts (geckoapp GeckoPrefs.writeConfig; a release build never does), so every variant is a full restart.
Each round runs every variant once, in a rotated order; 'base' and 'base2' are the SAME configuration: their difference is the noise floor of this run and decides what counts as a real effect.
Per start: the lab ruler /bench2 (8 kinds of work x 3 passes, tools/sitelab/soaklab.py: pass 1 = cold code, pass 3 = warm), the engine-ready time, the longest stall of the UI thread, and the memory of
every app process (/proc/<pid>/smaps_rollup). Output: SWEEP-REPORT.md + sweep-results.json. Standard library only. An x86 emulator with a software GPU and a fast link: it can say whether a CPU- or
memory-bound preference moves things RELATIVE to the same run's base; it cannot say anything about the network, the Mali GPU or absolute speed.
"""
import argparse
import importlib.util
import json
import os
import re
import statistics
import sys
import time


def load_packlab(root):
    spec = importlib.util.spec_from_file_location("packlab", os.path.join(root, "tools", "emu", "packlab.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def parse_variant(spec):
    name, _, rest = spec.partition("=")
    prefs = [p.strip() for p in rest.split(";") if p.strip()]
    return name.strip(), prefs


def mad(vals):
    m = statistics.median(vals)
    return statistics.median([abs(v - m) for v in vals])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="sweep-out")
    ap.add_argument("--repo-root", default=".")
    ap.add_argument("--pkg", default="app.kestrel.browser.gecko.debug")
    ap.add_argument("--variant", action="append", default=[])
    ap.add_argument("--rounds", type=int, default=5)
    ap.add_argument("--port", type=int, default=8092)
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args()
    root = os.path.abspath(a.repo_root)
    pl = load_packlab(root)
    if a.selftest:
        assert parse_variant("x=a.b=1;c.d=-") == ("x", ["a.b=1", "c.d=-"])
        assert parse_variant("base=") == ("base", [])
        assert mad([1, 2, 3, 4, 100]) == 1
        pl.selftest()
        print("prefsweep selftest ok")
        return 0
    os.makedirs(a.out, exist_ok=True)
    variants = [parse_variant(v) for v in a.variant]
    names = [n for n, _ in variants]

    class Args:
        pkg = a.pkg
        port = a.port
        starts = 1
        rounds = a.rounds

    lab = pl.Lab(Args)
    lab.adb.run("root", timeout=60)
    time.sleep(3)
    lab.adb.run("wait-for-device", timeout=120)
    lab.rooted = "uid=0" in lab.adb.sh("id", timeout=30)
    srv, reports = pl.start_server(a.port, root, os.path.join(os.path.abspath(a.out), "lab-tmp"))
    lab.adb.run("reverse", "tcp:%d" % a.port, "tcp:%d" % a.port)
    for s in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
        lab.adb.sh("settings put global %s 0" % s)
    lab.adb.sh("svc power stayon true")
    lab.adb.sh("input keyevent KEYCODE_WAKEUP")
    lab.adb.sh("wm dismiss-keyguard")
    apk = None
    for cand in ("geckoapp/build/outputs/apk/debug/geckoapp-debug.apk",):
        if os.path.exists(os.path.join(root, cand)):
            apk = os.path.join(root, cand)
    if apk:
        lab.adb.run("uninstall", a.pkg, timeout=120)
        say = pl.say
        say("install", lab.adb.run("install", "-t", apk, timeout=600).strip()[:80])
    rows = []
    bad = []
    url = "http://localhost:%d/bench2" % a.port

    def set_props(prefs):
        for i in range(1, 9):
            val = prefs[i - 1] if i - 1 < len(prefs) else ""
            lab.adb.sh("setprop debug.kestrel.gp%d %s" % (i, "'%s'" % val if val else "''"), timeout=30)

    def one(name, prefs, rnd):
        set_props(prefs)
        lab.kill()
        lab.drop_caches()
        time.sleep(2)
        mark = len(reports)
        lab.logcat_clear()
        t0 = time.time()
        out = lab.adb.sh("am start -W -a android.intent.action.VIEW -d %s -p %s" % (url, a.pkg), timeout=90)
        total, _ = pl.parse_am_start(out)
        got = []
        end = time.time() + 120
        while time.time() < end:
            got = [r for r in reports[mark:] if "kind=bench2" in r]
            if len(got) >= 3:
                break
            time.sleep(1.0)
        ev = lab.health_since_clear()
        ready = next((e["t"] for e in ev if e["layer"] == "engine" and e["ev"] == "runtime-ready"), None)
        swept = [e["rest"] for e in ev if e["layer"] == "engine" and e["ev"] == "sweep"]
        blk = max([int(re.sub(r"\D", "", e["rest"].split("ms")[0]) or 0) for e in ev if e["layer"] == "ui" and e["ev"] in ("slow", "late-beat", "STALL")] or [0])
        passes = []
        for r in got[:3]:
            q = dict(kv.split("=", 1) for kv in r.split("?", 1)[1].split("&") if "=" in kv)
            passes.append({k: int(v) for k, v in q.items() if v.isdigit()})
        time.sleep(8)
        procs = []
        for pid, pname in lab.pids():
            d = pl.parse_smaps_rollup(lab.adb.sh("cat /proc/%d/smaps_rollup" % pid, timeout=30))
            if d:
                procs.append({"name": pname.replace(a.pkg, "app"), "pss": d.get("Pss", 0), "anon": d.get("Pss_Anon", 0), "file": d.get("Pss_File", 0)})
        cfg = lab.adb.sh("run-as %s cat files/gecko-config.yaml" % a.pkg, timeout=30)
        applied_ok = all(((p.split("=")[0] + ":") in cfg) or p.endswith("=-") for p in prefs)
        row = {"variant": name, "round": rnd, "am_total_ms": total, "runtime_ready_s": ready, "ui_block_ms": blk, "passes": passes, "procs": procs,
               "total_pss_mb": round(sum(p["pss"] for p in procs) / 1024.0, 1), "anon_mb": round(sum(p["anon"] for p in procs) / 1024.0, 1), "applied": applied_ok, "sweep_line": swept[:1]}
        if len(passes) < 3:
            bad.append({"variant": name, "round": rnd, "why": "only %d bench passes reported" % len(passes)})
        if not applied_ok:
            bad.append({"variant": name, "round": rnd, "why": "the config file does not hold the preference lines"})
        rows.append(row)
        pl.say("sweep", name, "round", rnd, "bench p1/p3 total", [p.get("total") for p in passes], "ready", ready, "pss", row["total_pss_mb"], "applied", applied_ok)

    try:
        for rnd in range(1, a.rounds + 1):
            k = (rnd - 1) % len(variants)
            for name, prefs in variants[k:] + variants[:k]:
                one(name, prefs, rnd)
    except KeyboardInterrupt:
        pass
    finally:
        set_props([])
        text = report(rows, names, bad)
        with open(os.path.join(a.out, "SWEEP-REPORT.md"), "w") as f:
            f.write(text)
        with open(os.path.join(a.out, "sweep-results.json"), "w") as f:
            json.dump({"rows": rows, "failures": bad, "variants": [{"name": n, "prefs": p} for n, p in variants]}, f, indent=1)
        print("\n" + text, flush=True)
    return 0


def report(rows, names, bad):
    def col(name, fn):
        return [v for v in (fn(r) for r in rows if r["variant"] == name) if v is not None]

    def p(r, i, key):
        return r["passes"][i].get(key) if len(r["passes"]) > i else None
    metrics = [("bench pass 1 (cold code) ms", lambda r: p(r, 0, "total")), ("bench pass 3 (warm) ms", lambda r: p(r, 2, "total")), ("JS-heavy parts pass 3 (oo+str+json+sorts) ms", lambda r: (sum(p(r, 2, k) or 0 for k in ("oo", "str", "json", "sorts")) if len(r["passes"]) > 2 else None)),
               ("alloc/GC part pass 3 ms", lambda r: p(r, 2, "alloc")), ("longest GC-like stall pass 3 ms", lambda r: p(r, 2, "gcmax")), ("style+layout parts pass 3 (dom+layout) ms", lambda r: (sum(p(r, 2, k) or 0 for k in ("dom", "layout")) if len(r["passes"]) > 2 else None)),
               ("engine ready s", lambda r: r.get("runtime_ready_s")), ("start to bench done (am start -W) ms", lambda r: r.get("am_total_ms")), ("total PSS MB (all app processes)", lambda r: r.get("total_pss_mb")), ("anonymous PSS MB", lambda r: r.get("anon_mb")),
               ("worst UI block ms", lambda r: r.get("ui_block_ms"))]
    base = names[0]
    twin = names[1] if len(names) > 1 else None
    L = ["# Engine preference sweep (LAB5b)", "",
         "Variants: " + ", ".join(names) + ". %d rounds, order rotated. **'%s' and '%s' are the same configuration: their gap is this run's noise floor.** Emulator, software GPU, fast link: relative effects of CPU- and memory-bound preferences only." % (max([r["round"] for r in rows] or [0]), base, twin), ""]
    for label, fn in metrics:
        L.append("### " + label)
        L.append("")
        L.append("| Variant | n | median | spread (MAD) | vs " + base + " |")
        L.append("| :- | -: | -: | -: | -: |")
        b = statistics.median(col(base, fn)) if col(base, fn) else None
        noise = None
        if twin and col(base, fn) and col(twin, fn):
            noise = abs(statistics.median(col(base, fn)) - statistics.median(col(twin, fn)))
        for n in names:
            v = col(n, fn)
            if not v:
                L.append("| %s | 0 | - | - | - |" % n)
                continue
            m = statistics.median(v)
            d = ""
            if b and n != base:
                pct = (m - b) / b * 100.0 if b else 0.0
                real = noise is not None and abs(m - b) > 2.0 * max(noise, mad(v), 0.5)
                d = "%+.1f %%%s" % (pct, "  **beyond noise**" if real else "")
            L.append("| %s | %d | %.1f | %.1f | %s |" % (n, len(v), m, mad(v) if len(v) > 1 else 0.0, d))
        L.append("")
    if bad:
        L.append("## Failures")
        L.append("")
        for x in bad:
            L.append("- %s round %s: %s" % (x["variant"], x["round"], x["why"]))
        L.append("")
    return "\n".join(L)


if __name__ == "__main__":
    sys.exit(main())
