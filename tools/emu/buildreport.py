#!/usr/bin/env python3
"""LAB3d (owner message 62): ONE report for a build, from one run of the emulator lab.

  python3 -I tools/emu/buildreport.py --out <lab-out dir> [--repo-root .] [--baseline research/lab-baseline.json] [--write-baseline FILE]
                                      [--suite daily] [--profile tablet] [--net full] [--run-id N] [--sha S] [--notes research/LAB3_IMPROVEMENTS.md]

Reads what a lab run wrote (events.jsonl, summary.json, health-lines.txt, diagnostics-*.txt, device.json, device-props.txt, ui-*.txt, report.md) and writes
BUILD-REPORT.md + build-report.json next to them. Standard library only; never raises out (a half-written run still gets a report that says what is missing).

Reading rules (they are printed in the report): the emulator is x86 under KVM with a software GPU, so ABSOLUTE times and smoothness are not the tablet's. Behaviour (did it load,
play, refuse, crash) and the CHANGE from one build to the next, divided by the run's own CPU-calibration page, are what mean something.
"""
import argparse
import collections
import glob
import json
import os
import re
import statistics
import sys
import time

SEV = {"high": 0, "medium": 1, "low": 2, "info": 3}

# what a finding usually points at in the code (a starting place for the person who fixes it, not a diagnosis)
CODE_HINTS = [
    (r"^load ", "page loading: the network path (hotspot profile), Shields request handling (research/ADBLOCK_ENGINE.md), engine prefs (research/GECKO_SPEED.md); on the emulator also an overloaded phone (lab limit)"),
    (r"STALL|late-beat|ui slow|UI thread was blocked", "BrowserActivity (UI-thread work: a synchronous call into the engine), diag/Health.kt watchdog stack in the Diagnostics 'ui STALL' line names the call; research/FREEZE.md, research/VIDEO_ACCEL_DEVICE.md"),
    (r"captureLook|read-back|look slow", "BrowserActivity.captureLook / GeckoView.capturePixels (synchronous read-back; alpha48 back-off)"),
    (r"scroll", "GeckoEngine scroll prefs (research/SCROLL.md, GECKO_SPEED.md), page JS cost (content scripts), Shields cosmetic filters"),
    (r"typed|address|omnibox", "ui/MiniNav.kt address control + core omnibox (URL vs search decision)"),
    (r"field|typing", "PageFieldInput.kt / MiniKeyboard (research/TYPING.md, GLIDE_TYPING.md)"),
    (r"film|video|picture|player", "VideoFlow / WebHandOver / NativePlayerActivity / PlayerFactory (research/ANS60_PLAYBACK_OPTIMISATION.md)"),
    (r"memory|pss|trim|hibernat", "BrowserActivity.enforceLiveLimit / hibernate / onTrimMemory, core LiveBudget (docs/HARDWARE.md memory rules)"),
    (r"certificate|https|mixed", "GeckoEngine policy prefs (gecko-prefs.txt), core HttpsPolicy, ShieldsBridge"),
    (r"cookie|referer|fingerprint|fp-|webrtc|popup|camera|location|notification|clipboard|scheme|file-url", "gecko-prefs.txt (ETP / FPP / permission prefs), GeckoEngine permission delegate, research/SECURITY.md"),
    (r"access|accessibility|not found by", "ui/*.kt contentDescription of the control (TalkBack and the lab's uiautomator both read it)"),
    (r"cold|engine|start", "engine start (GeckoEngine.runtime, research/COLD_START.md)"),
    (r"download", "download manager (research/DOWNLOADS.md) / DownloadGuard"),
]


def load_jsonl(p):
    out = []
    try:
        for ln in open(p, encoding="utf-8", errors="replace"):
            ln = ln.strip()
            if ln:
                try:
                    out.append(json.loads(ln))
                except ValueError:
                    pass
    except OSError:
        pass
    return out


def load_json(p, default=None):
    try:
        return json.load(open(p, encoding="utf-8"))
    except (OSError, ValueError):
        return default


def read(p, limit=4_000_000):
    try:
        return open(p, encoding="utf-8", errors="replace").read(limit)
    except OSError:
        return ""


def pct(vals, p):
    vals = sorted(v for v in vals if v is not None)
    if not vals:
        return None
    k = (len(vals) - 1) * p / 100.0
    lo, hi = int(k), min(int(k) + 1, len(vals) - 1)
    return vals[lo] + (vals[hi] - vals[lo]) * (k - lo)


def f0(v, unit=""):
    return "-" if v is None else ("%d%s" % (round(v), unit))


def f1(v, unit=""):
    return "-" if v is None else ("%.1f%s" % (v, unit))


def build_identity(root, sha, run_id):
    name = code = None
    txt = read(os.path.join(root, "geckoapp", "build.gradle.kts"), 200_000)
    m = re.search(r'versionName\s*=\s*"([^"]+)"', txt)
    name = m.group(1) if m else None
    m = re.search(r"versionCode\s*=\s*(\d+)", txt)
    code = int(m.group(1)) if m else None
    return {"app": "Kestrel G (Firefox engine)", "versionName": name, "versionCode": code, "commit": (sha or "")[:8], "run": run_id}


def hardware_table(dev, health_text, net, lowram_note=""):
    """(what, the tablet, this run, verdict) rows: matched / partly / NOT matched, so nobody reads a number past its limits."""
    d = dev or {}
    def mem_mb():
        m = re.search(r"(\d+)\s*kB", d.get("memtotal", ""))
        return int(m.group(1)) // 1024 if m else None
    heap = re.search(r"limit (\d+) MB", health_text)
    rows = [
        ("Android version", "10 (API 29)", "Android %s (API %s)" % (d.get("android", "?"), d.get("sdk", "?")), "matched" if d.get("sdk") == "29" else "NOT matched"),
        ("Userland", "32-bit (sys_mssi_t_32_ago, armeabi-v7a)", "%s-bit, ABI %s (%s)" % (d.get("bits", "?"), d.get("abi", "?"), d.get("abilist", "?")), "matched (32-bit x86)" if d.get("bits") == "32" else "NOT matched: 64-bit x86_64 (Mozilla publishes no 32-bit x86 GeckoView for 160: lab run 43997001918 could not resolve geckoview-x86); pointers are twice as big, so memory numbers read high"),
        ("Screen", "600x973 at 170 dpi", "%s, %s" % (d.get("wm_size", "?").replace("Physical size: ", ""), d.get("wm_density", "?").replace("Physical density: ", "")), "matched" if "600" in d.get("wm_size", "") and "170" in d.get("wm_density", "") else "check"),
        ("Font scale", "1.2", d.get("font_scale", "?"), "matched" if d.get("font_scale", "").startswith("1.2") else "NOT matched (1.0)"),
        ("RAM", "2 GB", ("%d MB" % mem_mb()) if mem_mb() else "?", "matched" if mem_mb() and 1700 <= mem_mb() <= 2100 else "check"),
        ("Low-RAM (Go) flag", "ro.config.low_ram=true, memory class 128 MB", "low_ram=%s, app heap limit %s" % (d.get("low_ram") or "(unset)", ("%s MB" % heap.group(1)) if heap else "?"), "matched" if d.get("low_ram") == "true" else "NOT matched (AOSP image, not the Go edition: no Android 10 Go image exists for the emulator)" + ((" - " + lowram_note) if lowram_note else "")),
        ("CPU", "4 x Cortex-A53 (MT8168B), in-order, about 2 GHz", "%s cores of an x86 host CPU (%s)" % (d.get("cores", "?"), d.get("cpu", "?")[:60]), "NOT matched: the host core is several times faster; only changes between builds mean something"),
        ("GPU", "Mali-G52 MC2", "SwiftShader software GL", "NOT matched: smoothness and GPU memory are not the tablet's"),
        ("Video decoders", "MediaTek hardware H.264 / VP9", "software decoders", "NOT matched: 'plays / does not play' counts, smoothness does not"),
        ("Google services", "none (no Play, no GMS)", "GMS present: %s" % d.get("gms", "?"), "matched" if d.get("gms") == "no" else "NOT matched"),
        ("Storage", "unencrypted", d.get("crypto") or "(unset)", "matched" if d.get("crypto") in ("", "unencrypted", None) else "NOT matched (the emulator image is file-encrypted; no effect on speed worth chasing)"),
        ("Network", "phone hotspot, 5 Mbit/s cap (owner: mostly fast Wi-Fi now)", "shaped like the hotspot" if net == "hotspot" else "the runner's fast link (datacentre)", "partly" if net != "hotspot" else "matched"),
    ]
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--repo-root", default=".")
    ap.add_argument("--baseline", default="")
    ap.add_argument("--write-baseline", default="")
    ap.add_argument("--suite", default="daily")
    ap.add_argument("--profile", default="")
    ap.add_argument("--net", default="full")
    ap.add_argument("--run-id", default="")
    ap.add_argument("--sha", default="")
    ap.add_argument("--notes", default="", help="a hand-written Markdown file (the improvement research) printed as its own section")
    a = ap.parse_args()
    out = a.out
    ev = load_jsonl(os.path.join(out, "events.jsonl"))
    summ = load_json(os.path.join(out, "summary.json"), {}) or {}
    dev = load_json(os.path.join(out, "device.json"), {}) or {}
    health = read(os.path.join(out, "health-lines.txt"))
    diag = ""
    for p in sorted(glob.glob(os.path.join(out, "diagnostics-*.txt"))):
        diag = read(p)                                    # the newest one sorts last enough for a summary
    build = build_identity(a.repo_root, a.sha, a.run_id)

    steps = [e for e in ev if e.get("kind") == "step"]
    sec = [e for e in ev if e.get("kind") == "sec"]
    feat = [e for e in ev if e.get("kind") == "feat"]
    pages_ev = [e for e in ev if e.get("kind") in ("page", "labpage", "videosite")]
    bench = [e for e in ev if e.get("kind") == "bench" and e.get("status") == "ok"]
    issues = [tuple(i) for i in (summ.get("issues") or [])]

    # ---- the numbers of the run
    loads = []                                            # every page the app finished loading: (name, paint, done, blocked, console errors)
    LAB_HOSTS = ("localhost", "127.0.0.1")
    for e in steps:
        pgs = e.get("pages") or []
        for p in pgs:
            # a page that ended 'ok=false' is a refused or failed load (a bad certificate, a blocked site): not a speed sample
            if "error" not in p and p.get("done_ms", -1) >= 0 and p.get("ok") != "false":
                loads.append((e["name"], p.get("host"), p.get("paint_ms"), p.get("done_ms"), p.get("blocked", 0), (e.get("console") or {}).get("errors", 0) if len(pgs) == 1 else None))
    for e in pages_ev:
        if e.get("status") in ("ok", "slow") and e.get("done_ms") is not None and e.get("shields", "on") == "on":
            loads.append((e.get("name"), e.get("url_host"), e.get("paint_ms"), e.get("done_ms"), e.get("blocked", 0), (e.get("console") or {}).get("errors", 0)))
    public = [l for l in loads if (l[1] or "") not in LAB_HOSTS]          # the speed numbers are about real sites; the lab's own pages load in milliseconds and would flatter them
    paints = [l[2] for l in public if l[2] is not None and l[2] >= 0]
    dones = [l[3] for l in public if l[3] is not None and l[3] >= 0]
    scroll = collections.Counter()
    for e in steps + pages_ev:
        s = e.get("scroll")
        if s:
            for k in ("bursts", "frames", "gaps50", "gaps100"):
                scroll[k] += s.get(k, 0)
            scroll["worst_ms"] = max(scroll["worst_ms"], s.get("worst_ms", 0))
    stall_steps = [(e["name"], e.get("ui_stalls", 0), e.get("ui_worst_ms", 0)) for e in steps + pages_ev if e.get("ui_stalls")]
    late_steps = [(e["name"], e.get("ui_late", 0), e.get("ui_worst_ms", 0)) for e in steps + pages_ev if e.get("ui_late")]
    by = {e["name"]: e for e in steps}
    bench_total = min([b["ms"].get("total", 0) for b in bench if b.get("ms") and b["ms"].get("total", 0) > 0] or [0]) or None   # the fastest = the steady state (the first runs while the engine and the JIT are still warming up)
    mem = by.get("many-tabs-and-memory-warning", {})
    metrics = {
        "page_paint_p50_ms": pct(paints, 50), "page_paint_p90_ms": pct(paints, 90), "page_done_p50_ms": pct(dones, 50), "page_done_p90_ms": pct(dones, 90), "pages_loaded": len(public), "lab_pages_loaded": len(loads) - len(public),
        "scroll_gap50_per_1000_frames": (1000.0 * scroll["gaps50"] / scroll["frames"]) if scroll["frames"] else None,
        "scroll_gap100_per_1000_frames": (1000.0 * scroll["gaps100"] / scroll["frames"]) if scroll["frames"] else None, "scroll_worst_gap_ms": scroll["worst_ms"] or None,
        "ui_stall_count": sum(s[1] for s in stall_steps), "ui_worst_ms": max([s[2] for s in stall_steps] or [0]) or None,
        "ui_late_beats": sum(s[1] for s in late_steps), "ui_late_worst_ms": max([s[2] for s in late_steps] or [0]) or None,
        "cold_start_am_total_ms": (by.get("cold-start") or {}).get("am_total_ms"), "cold_start_engine_ready_s": (by.get("cold-start") or {}).get("engine_ready_s"),
        "typing_21_chars_s": (by.get("typing-in-a-form-field") or {}).get("typing_s"), "film_tap_to_picture_s": (by.get("film-in-a-page-press-play") or {}).get("tap_to_picture_s"),
        "tab_switch_s": (by.get("several-tabs-and-switching") or {}).get("switch_s"), "resume_s": (by.get("leave-the-app-and-return") or {}).get("resume_s"),
        "memory_max_pss_mb": mem.get("max_pss"), "memory_min_avail_mb": mem.get("min_avail"), "memory_after_trim_pss_mb": mem.get("after_trim_pss"),
        "security_pass": sum(1 for s in sec if s.get("result") == "pass"), "security_fail": sum(1 for s in sec if s.get("result") == "fail"), "security_info": sum(1 for s in sec if s.get("result") == "info"),
        "issues_high": sum(1 for i in issues if i[0] == "high"), "issues_medium": sum(1 for i in issues if i[0] == "medium"), "issues_low": sum(1 for i in issues if i[0] == "low"),
        "bench_total_ms": bench_total,
    }

    # ---- findings
    F = []                                                # (key, sev, area, title, evidence, hint)
    def hint_for(text):
        for rx, h in CODE_HINTS:
            if re.search(rx, text, re.I):
                return h
        return ""
    LAB_AREAS = ("emulator", "tooling")
    seen = set()
    slow_loads = collections.defaultdict(list)             # "<site> did not finish loading within N s" x 20 is ONE finding, not twenty
    for sev, area, text in issues:
        m = re.match(r"^(\S+) did not finish loading within (\d+) s", text)
        if m and area == "load":
            slow_loads[(sev, m.group(2))].append(m.group(1))
            continue
        key = "%s/%s/%s" % (sev, area, re.sub(r"\d+", "N", text)[:90])
        if key in seen:
            continue
        seen.add(key)
        if area in LAB_AREAS:
            continue
        F.append((key, sev, area, text[:200], "lab issue line", hint_for(area + " " + text)))
    for (sev, secs), names in slow_loads.items():
        F.append(("load/timeout-%s" % secs, sev, "load", "%d page(s) did not finish loading within %s s: %s" % (len(names), secs, ", ".join(sorted(set(names))[:12]) + (" ..." if len(set(names)) > 12 else "")),
                  "when the phone's own system restarted in the same run these are lab overload, not site faults (see the lab limits line)", hint_for("load x")))
    for e in sec:
        if e.get("result") == "fail":
            F.append(("sec/" + e["name"], "high", "security", "%s FAILED" % e["name"], e.get("detail", ""), hint_for(e["name"])))
    for e in feat:
        if e.get("result") == "fail":
            F.append(("feat/" + e["name"], "medium", "feature", "%s FAILED" % e["name"], e.get("detail", ""), hint_for(e["name"])))
    for e in steps:
        if e.get("status") in ("no-navigation", "no-picture", "crash", "no-engine", "no-layout") and not any(f[0].endswith(e["name"]) for f in F):
            F.append(("step/" + e["name"], "high" if e["status"] in ("crash", "no-engine") else "medium", "stability" if e["status"] in ("crash", "no-engine") else "feature",
                      "step '%s' ended '%s'" % (e["name"], e["status"]), e.get("note", ""), hint_for(e["name"])))
    if metrics["ui_worst_ms"] and metrics["ui_worst_ms"] >= 700:
        F.append(("speed/ui-stall", "medium", "speed", "the UI thread was blocked for up to %d ms (%d stalls)" % (metrics["ui_worst_ms"], metrics["ui_stall_count"]), "; ".join("%s x%d (%d ms)" % s for s in stall_steps[:6]), hint_for("STALL")))
    if metrics["ui_late_worst_ms"] and metrics["ui_late_worst_ms"] >= 700 and not (metrics["ui_worst_ms"] or 0) >= 700:
        F.append(("speed/ui-late-beat", "medium", "speed", "the UI thread did not answer for %d ms in %d place(s) (late heartbeat, below the stall watchdog)" % (metrics["ui_late_worst_ms"], len(late_steps)),
                  "; ".join("%s (%d, worst %d ms)" % s for s in late_steps[:6]) + ". The tablet shows the same family at start (ui slow 865 ms)", hint_for("late-beat")))
    idle = by.get("idle-cpu-60-seconds") or {}
    if idle.get("busy_total_pct") and idle["busy_total_pct"] >= 6:
        F.append(("speed/idle-cpu", "medium", "speed", "the app burns %.1f %% CPU for 60 s while doing nothing on a static page" % idle["busy_total_pct"],
                  "busiest threads: " + ", ".join("%s %.1f%%" % (r["thread"], r["cpu"]) for r in (idle.get("busy_threads") or [])[:5]) + " (the tablet report of alpha33 showed 5 % idle)", "diag/Health.kt watchdog cadence, Neon animation, CpuMeter, engine threads"))
    tour_blank = (by.get("site-tour-nine-public-sites") or {}).get("blank_first_screen") or []
    if tour_blank:
        F.append(("load/blank-first-screen", "medium", "load", "pages whose first screen is blank after loading: " + ", ".join(tour_blank), "screen sample: one colour over 97 % and no edges", "Shields cosmetic filters / scriptlets, a blocked first-party script (research/ADBLOCK_ENGINE.md)"))
    if metrics["scroll_gap100_per_1000_frames"] and metrics["scroll_gap100_per_1000_frames"] > 20:
        F.append(("scroll/gaps", "low", "scroll", "%.0f gaps over 100 ms per 1000 frames while scrolling (worst %s ms)" % (metrics["scroll_gap100_per_1000_frames"], metrics["scroll_worst_gap_ms"]), "SwiftShader software GL: an emulator number, the tablet measured 62-65 fps and 0 of 32 swipes with a gap over 100 ms (research/TABLET_DIAG_2026-11-13.md); watch the CHANGE between builds, not the value", hint_for("scroll")))
    if metrics["page_done_p90_ms"] and metrics["page_done_p90_ms"] > 12000:
        F.append(("load/slow-p90", "medium", "speed", "the slowest tenth of page loads finished after %.1f s" % (metrics["page_done_p90_ms"] / 1000.0), "slowest: " + ", ".join("%s %.1fs" % (l[0], l[3] / 1000.0) for l in sorted(loads, key=lambda l: -l[3])[:4]), "page loading: Shields blocking cost (research/ADBLOCK_ENGINE.md), network profile"))
    noisy = sorted([l for l in public if l[5] and l[5] >= 15], key=lambda l: -l[5])[:4]
    if noisy:
        F.append(("console/noisy", "low", "compat", "pages with many script errors: " + ", ".join("%s (%d)" % (l[1] or l[0], l[5]) for l in noisy), "a Shields A/B on the same page tells whether our blocking causes them (LAB2c)", "Shields lists / scriptlets (research/ADBLOCK_ENGINE.md)"))
    gab = (by.get("private-tab-and-shields-switch") or {}).get("shields_ab_google") or {}
    if (gab.get("on") or {}).get("secs_median") and (gab.get("off") or {}).get("secs_median"):
        gon, goff = gab["on"], gab["off"]
        if gon["secs_median"] > goff["secs_median"] * 1.3 and gon["secs_median"] - goff["secs_median"] > 1.0:
            F.append(("shields/google-slower", "high", "speed", "a Google results page took %.1f s with Shields ON against %.1f s OFF (median of 3; %d requests blocked)" % (gon["secs_median"], goff["secs_median"], gon.get("blocked", 0)),
                      "samples on %s, off %s: the owner's most common page (typed text is searched on Google)" % (gon["samples"], goff["samples"]), "Shields rules / scriptlets for google.com (research/ADBLOCK_ENGINE.md), the 'asked the app one by one' path"))
    ab = (by.get("private-tab-and-shields-switch") or {}).get("shields_ab") or {}
    if ab.get("on", {}).get("done_ms", -1) > 0 and ab.get("off", {}).get("done_ms", -1) > 0:
        on, off = ab["on"], ab["off"]
        if on["done_ms"] > off["done_ms"] * 1.3 and on["done_ms"] - off["done_ms"] > 1500:
            F.append(("shields/slower", "medium", "speed", "with Shields ON the Guardian page finished %.1f s later than with Shields OFF (%d vs %d ms; %d blocked)" % ((on["done_ms"] - off["done_ms"]) / 1000.0, on["done_ms"], off["done_ms"], on.get("blocked", 0)), "blocking should make pages FASTER", "the 'asked the app one by one' path (research/COMPAT_RUN1.md item 5), extension request handling"))
    if mem and mem.get("after_trim_pss") and mem.get("max_pss") and mem["after_trim_pss"] > mem["max_pss"] * 0.92:
        F.append(("memory/trim-gives-little", "low", "memory", "after Android's 'running critical' warning the app still holds %d MB (peak %d MB)" % (mem["after_trim_pss"], mem["max_pss"]), "hibernated tabs in the run: %s" % mem.get("hibernated"), hint_for("trim")))
    if mem and mem.get("max_pss") and mem["max_pss"] > 700:
        F.append(("memory/peak", "medium", "memory", "app PSS peaked at %d MB with %d tabs (the tablet has 2 GB in total)" % (mem["max_pss"], mem.get("tabs", 0)), "", hint_for("memory")))
    F.sort(key=lambda f: (SEV.get(f[1], 9), f[2], f[3]))

    # ---- verdicts per area
    def verdict(areas, extra_red=False):
        rel = [f for f in F if f[2] in areas]
        if any(f[1] == "high" for f in rel) or extra_red:
            return "RED"
        if any(f[1] == "medium" for f in rel):
            return "AMBER"
        return "GREEN" if rel == [] else "GREEN (low items)"
    crashed = [e for e in steps + pages_ev if e.get("status") == "crash"]
    area_rows = [
        ("Stability (crash, ANR, restart)", verdict(("crash", "restart", "stability", "start"), bool(crashed)), "%d scene(s) ended in a crash; app restarts seen: %s" % (len(crashed), summ.get("restarts", "?"))),
        ("Security", verdict(("security",)) if sec else "NOT MEASURED", "%d pass, %d FAIL, %d info rows" % (metrics["security_pass"], metrics["security_fail"], metrics["security_info"])),
        ("Page loading", verdict(("load", "compat")) if loads or pages_ev else "NOT MEASURED", "%d pages; paint p50 %s, done p50 %s / p90 %s" % (len(loads), f0(metrics["page_paint_p50_ms"], " ms"), f0(metrics["page_done_p50_ms"], " ms"), f0(metrics["page_done_p90_ms"], " ms"))),
        ("Speed", verdict(("speed", "freeze")) if loads else "NOT MEASURED", "UI stalls %d (worst %s); cold start %s s" % (metrics["ui_stall_count"], f0(metrics["ui_worst_ms"], " ms"), f1(metrics["cold_start_engine_ready_s"]))),
        ("Scrolling", verdict(("scroll",)) if scroll["frames"] else "NOT MEASURED", "%d frames in %d bursts, gaps over 100 ms per 1000 frames: %s" % (scroll["frames"], scroll["bursts"], f1(metrics["scroll_gap100_per_1000_frames"]))),
        ("Typing and input", verdict(("typing",)) if by.get("typing-in-a-form-field") else "NOT MEASURED", "21 characters in %s s, UI late-beats while typing: %s" % (f1(metrics["typing_21_chars_s"]), (by.get("typing-in-a-form-field") or {}).get("typing_ui_late", "-"))),
        ("Video", verdict(("video", "web-hand-over")) if by.get("film-in-a-page-press-play") else "NOT MEASURED", "tap to first picture %s s" % f1(metrics["film_tap_to_picture_s"])),
        ("Features (address bar, tabs, downloads, menu screens)", verdict(("feature",)) if steps else "NOT MEASURED", "%d feature checks failed" % sum(1 for e in feat if e.get("result") == "fail")),
        ("Memory", verdict(("memory",)) if mem else "NOT MEASURED", "peak %s MB, lowest available %s MB, after warning %s MB" % (f0(metrics["memory_max_pss_mb"]), f0(metrics["memory_min_avail_mb"]), f0(metrics["memory_after_trim_pss_mb"]))),
        ("Accessibility tree", verdict(("access",)) if steps else "NOT MEASURED", "controls must be findable by name (TalkBack and the lab read the same tree)"),
    ]

    # ---- change against the last report of this suite
    base = load_json(a.baseline, None) if a.baseline else None
    delta_rows = []
    if base and base.get("metrics"):
        bb = base["metrics"]
        scale = (bench_total / bb["bench_total_ms"]) if (bench_total and bb.get("bench_total_ms")) else None
        time_keys = {"page_paint_p50_ms", "page_paint_p90_ms", "page_done_p50_ms", "page_done_p90_ms", "cold_start_am_total_ms", "cold_start_engine_ready_s", "typing_21_chars_s", "film_tap_to_picture_s", "tab_switch_s", "resume_s"}
        for k, v in metrics.items():
            o = bb.get(k)
            if v is None or o is None or not isinstance(v, (int, float)) or k == "bench_total_ms":
                continue
            nv = v / scale if (scale and k in time_keys) else v
            ch = ((nv - o) / o * 100.0) if o else None
            delta_rows.append((k, o, v, nv if (scale and k in time_keys) else None, ch))
        delta_head = "Baseline: %s (%s). Runner speed this run vs the baseline's: %s (bench page total %s ms vs %s ms): times marked 'normalised' are divided by it." % (
            (base.get("build") or {}).get("versionName"), base.get("date", "?"), ("x%.2f" % scale) if scale else "unknown", f0(bench_total), f0(bb.get("bench_total_ms")))
        new_keys = {f[0] for f in F} - set(base.get("findings", []))
        gone_keys = set(base.get("findings", [])) - {f[0] for f in F}
    else:
        delta_head, new_keys, gone_keys = "No baseline yet: this run becomes the baseline when research/lab-baseline.json is written from it.", set(), set()

    # ---- write it
    L = []
    L.append("# Kestrel build report: %s %s (code %s)" % (build["app"], build["versionName"] or "?", build["versionCode"] or "?"))
    L.append("")
    L.append("Lab run %s, commit %s, suite **%s**, device profile **%s**, network **%s**, written %s UTC. Findings are about THIS build; the emulator is not the tablet (section 2)." % (build["run"] or "?", build["commit"] or "?", a.suite, a.profile or "?", a.net, time.strftime("%Y-%m-%d %H:%M", time.gmtime())))
    L.append("")
    L.append("## 1. Verdict per area")
    L.append("")
    L.append("| Area | Verdict | Evidence |")
    L.append("| :- | :- | :- |")
    for n, v, e in area_rows:
        L.append("| %s | **%s** | %s |" % (n, v, e))
    L.append("")
    L.append("Rule: RED = at least one high finding in the area, AMBER = a medium one, GREEN = none; NOT MEASURED = the run did not get to it (see section 7).")
    L.append("")
    L.append("## 2. How closely the lab matched the tablet (LAB3a)")
    L.append("")
    L.append("| What | The owner's tablet | This run | Verdict |")
    L.append("| :- | :- | :- | :- |")
    lowram_note = " ".join(read(os.path.join(out, "lowram-attempt.txt")).split())[:240]
    for r in hardware_table(dev, health, a.net, lowram_note):
        L.append("| %s | %s | %s | %s |" % r)
    if bench:
        L.append("")
        L.append("CPU calibration (the smallest total is used as the runner's speed, the first run is slowed by the app still warming up)")
        L.append("")
        L.append("CPU calibration page (a fixed amount of JavaScript, sort, JSON, regex, DOM and canvas work, timed in the page): " + "; ".join("%s total %s ms (loop %s, sort %s, json %s, dom %s, canvas %s)" % (b["name"], b["ms"].get("total"), b["ms"].get("loop"), b["ms"].get("sort"), b["ms"].get("json"), b["ms"].get("dom"), b["ms"].get("canvas")) for b in bench) +
                 ". Compare it between runs: a faster runner makes every page look faster.")
    L.append("")
    L.append("## 3. Findings, most serious first")
    L.append("")
    if F:
        L.append("| # | Sev | Area | Finding | Evidence | Where to look |")
        L.append("| -: | :- | :- | :- | :- | :- |")
        for i, (key, sev, area, title, evid, hint) in enumerate(F, 1):
            tag = " (NEW)" if key in new_keys else ""
            L.append("| F%02d | %s | %s | %s%s | %s | %s |" % (i, sev, area, title.replace("|", "/")[:220], tag, (evid or "").replace("|", "/").replace("\n", " ")[:220], (hint or "").replace("|", "/")[:200]))
    else:
        L.append("No findings: nothing in the run crossed a threshold. (Check section 7 for what was not measured.)")
    if gone_keys:
        L.append("")
        L.append("Findings of the baseline that did not come back: %s." % ", ".join(sorted(gone_keys)[:12]))
    lab_notes = sorted({"%s/%s: %s" % (s, ar, t[:140]) for s, ar, t in issues if ar in LAB_AREAS})
    if lab_notes:
        L.append("")
        L.append("Limits of the lab itself seen in this run (not faults of the app): " + "; ".join(lab_notes[:6]))
    L.append("")
    L.append("## 4. The day, step by step (what a person does, what it cost)")
    L.append("")
    if steps:
        L.append("| Step | Result | Seconds | Page loads (paint / done ms) | UI stalls (worst ms) | Script errors | Memory PSS MB | Notes |")
        L.append("| :- | :- | -: | :- | -: | -: | -: | :- |")
        for e in steps:
            pg = "; ".join("%s %s/%s" % ((p.get("host") or "?")[:24], p.get("paint_ms"), p.get("done_ms")) if "error" not in p else "%s ERROR %s" % ((p.get("host") or "?")[:24], p.get("error")) for p in (e.get("pages") or [])[:3]) or "-"
            extra = {k: v for k, v in e.items() if k in ("am_total_ms", "engine_ready_s", "nav_s", "host", "typing_s", "tap_to_picture_s", "switch_s", "resume_s", "page_reloaded", "max_pss", "min_avail", "hibernated", "tabs", "cards", "opened", "backs", "sites", "nodes", "kestrel_player_took_it", "field_focused", "downloads_screen", "typed_text_arrived", "page_reported_position", "typed", "control_found", "shields_ab", "opened", "private_indicator_on_screen", "shields_ab_google", "switch_note", "cpuinfo", "busy_threads", "busy_total_pct", "blank_first_screen", "alive_after_screen_off", "engine_died", "after_trim_pss", "trim_lines")}
            L.append("| %s | %s | %s | %s | %s (%s) | %s | %s | %s |" % (e["name"], e.get("status"), e.get("wall_s"), pg, e.get("ui_stalls", 0), e.get("ui_worst_ms", 0), (e.get("console") or {}).get("errors", 0), e.get("pss", "-"),
                                                                          (e.get("note") or json.dumps(extra, default=str))[:230].replace("|", "/")))
    else:
        L.append("(This run did not use the daily suite: no steps.)")
    L.append("")
    if loads:
        L.append("### Page loads")
        L.append("")
        L.append("| Page | Host | First paint ms | Done ms | Blocked requests | Script errors |")
        L.append("| :- | :- | -: | -: | -: | -: |")
        for l in sorted(loads, key=lambda l: -(l[3] or 0))[:40]:
            L.append("| %s | %s | %s | %s | %s | %s |" % l)
        L.append("")
    if sec or feat:
        L.append("### Security and feature checks")
        L.append("")
        L.append("| Check | Result | Detail |")
        L.append("| :- | :- | :- |")
        for e in sec + feat:
            L.append("| %s | %s | %s |" % (e["name"], "**FAIL**" if e.get("result") == "fail" else e.get("result", "-"), (e.get("detail") or "").replace("|", "/")[:260]))
        L.append("")
    app_lines = [ln for ln in diag.splitlines() if re.search(r"Page loads|Real scrolling|Real video|frames\.jank|Kestrel's own player|UI thread|upgrad|blocked|Blocker|stats", ln, re.I)][:14]
    if app_lines:
        L.append("### What the app says about itself (its own Diagnostics, end of the run)")
        L.append("")
        for ln in app_lines:
            L.append("- " + ln.strip()[:260])
        L.append("")
    L.append("## 5. Change since the last report")
    L.append("")
    L.append(delta_head)
    L.append("")
    if delta_rows:
        L.append("| Metric | Before | Now | Now, normalised | Change |")
        L.append("| :- | -: | -: | -: | -: |")
        for k, o, v, nv, ch in delta_rows:
            L.append("| %s | %s | %s | %s | %s |" % (k, f1(o), f1(v), f1(nv) if nv is not None else "-", ("%+.0f %%" % ch) if ch is not None else "-"))
        L.append("")
    L.append("## 6. Ways to improve (especially speed)")
    L.append("")
    notes = read(a.notes) if a.notes else ""
    if notes.strip():
        L.append(notes.strip())
    else:
        L.append("(no hand-written research attached to this report: pass --notes research/<file>.md; the findings table above lists what the numbers point at)")
    L.append("")
    L.append("## 7. What this run did not measure")
    L.append("")
    for t in ("The tablet's real CPU, GPU and video decoders (section 2): every absolute time and every smoothness number is an emulator number.",
              "Sites that block data-centre addresses show as upstream errors or as a consent / challenge page; they are not app faults.",
              "Android 16 behaviour (the lab image is Android 10), the phone-hotspot link (unless net=hotspot), a real touch screen (events are injected), the owner's own keyboard, real passwords and logins.",
              "Steps that report 'skipped' ran out of the day's time budget; steps that report 'tooling-error' are faults of the lab script, not of the app."):
        L.append("- " + t)
    skipped = [e["name"] for e in steps if e.get("status") == "skipped"]
    broken = [e["name"] for e in steps if e.get("status") == "tooling-error"]
    if skipped:
        L.append("- Skipped in this run: " + ", ".join(skipped))
    if broken:
        L.append("- Lab script errors in: " + ", ".join(broken))
    ui_files = sorted(glob.glob(os.path.join(out, "ui-*.txt")))
    if ui_files:
        L.append("- The lab saved the native view tree of %d screens (ui-*.txt in the artifact) for tuning the next run." % len(ui_files))
    text = "\n".join(L) + "\n"
    with open(os.path.join(out, "BUILD-REPORT.md"), "w") as f:
        f.write(text)
    js = {"build": build, "suite": a.suite, "profile": a.profile, "net": a.net, "date": time.strftime("%Y-%m-%d", time.gmtime()), "metrics": metrics,
          "verdicts": {n: v for n, v, _ in area_rows}, "findings": [f[0] for f in F], "findings_full": [{"key": f[0], "severity": f[1], "area": f[2], "title": f[3], "evidence": f[4], "where": f[5]} for f in F]}
    with open(os.path.join(out, "build-report.json"), "w") as f:
        json.dump(js, f, indent=1, default=str)
    if a.write_baseline:
        with open(a.write_baseline, "w") as f:
            json.dump({k: js[k] for k in ("build", "suite", "profile", "net", "date", "metrics", "findings")}, f, indent=1, default=str)
    print("build report: %d findings, %d steps, %d page loads -> %s" % (len(F), len(steps), len(loads), os.path.join(out, "BUILD-REPORT.md")))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except SystemExit:
        raise
    except Exception as e:                                # a report problem must never fail a lab run
        print("buildreport failed: %r" % (e,))
        sys.exit(0)
