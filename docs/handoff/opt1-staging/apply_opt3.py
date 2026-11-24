import os, re
R = '/home/user/Browser'

def sub(p, old, new, count=1):
    p = os.path.join(R, p)
    s = open(p, encoding='utf-8').read()
    assert s.count(old) == count, (p, old[:70], s.count(old))
    open(p, 'w', encoding='utf-8').write(s.replace(old, new))

# ---- the lab suite `opt`
OPT = '''
    def opt_round(self):
        """OPT1 (research/OPTIMIZER.md): Auto-tune END TO END on the real engine, the short plan (debug builds only: three experiments, fast pages). It proves what no unit test can: the loopback server and the benchmark
        pages through the real engine and the real extension, an engine restart that comes back by itself (the trampoline), the guard that writes the start configuration, a restart-needing setting that REACHES the
        engine (the read-back), samples that make a decision, and the results file. The lab does not press anything: `am start ... --es auto quick` starts the run, the app does the rest."""
        pkg = self.a.pkg
        self.adb.sh("am force-stop %s" % pkg, timeout=30); time.sleep(2)
        self.adb.sh("run-as %s sh -c 'for f in files/optimizer/*; do rm -f $f; done'" % pkg, timeout=30)
        lm = self.log.size()
        self.adb.sh("am start -n %s/app.kestrel.browser.optimizer.OptimizerActivity --es auto quick" % pkg, timeout=60)
        t0 = time.time(); result = ""; last_state = ""
        while time.time() - t0 < 17 * 60:
            time.sleep(20)
            result = self.adb.sh("run-as %s cat files/optimizer/last.json" % pkg, timeout=30)
            if '"kind":"kestrel-autotune"' in result:
                break
            st = self.adb.sh("run-as %s cat files/optimizer/state.txt" % pkg, timeout=30)
            cur = [l for l in st.splitlines() if l.startswith(("cursor|", "status|", "bootMark|"))]
            if cur != last_state:
                say("opt state: %s" % " ".join(cur)); last_state = cur
        ev = health_events(self.log.since(lm), "opt")
        kinds = {}
        for e in ev:
            kinds[e["ev"]] = kinds.get(e["ev"], 0) + 1
        rows = []
        j = None
        try:
            j = json.loads(result) if '"kind":"kestrel-autotune"' in result else None
        except ValueError:
            j = None
        if j is None:
            self.issue("high", "opt", "Auto-tune did not finish in 17 minutes (events seen: %s)" % kinds, shot=True)
            rows.append({"case": "finished", "ok": False, "file": "no last.json; events %s" % kinds})
        else:
            dec = {d["id"]: d for d in j.get("decisions", [])}
            rows.append({"case": "finished", "ok": j.get("status") == "DONE", "file": "status %s %s" % (j.get("status"), j.get("note"))})
            for need in ("scroll0", "gcpar", "fps30"):
                d = dec.get(need)
                ok = d is not None and d.get("verdict") != "FAILED" and not str(d.get("why", "")).startswith("skipped")
                rows.append({"case": "experiment " + need, "ok": ok, "file": (d or {}).get("verdict", "missing") + ": " + str((d or {}).get("why", ""))[:120]})
                if not ok:
                    self.issue("high", "opt", "the %s experiment did not run to a verdict: %s" % (need, (d or {}).get("why", "missing")), shot=False)
            n_samples = sum(len(s.get("samples", [])) for s in j.get("steps", []))
            rows.append({"case": "samples", "ok": n_samples >= 8, "file": "%d samples" % n_samples})
            rows.append({"case": "restart that came back", "ok": kinds.get("restart", 0) >= 1 and kinds.get("sample", 0) >= 8, "file": "restarts %d, samples %d, guard %d" % (kinds.get("restart", 0), kinds.get("sample", 0), kinds.get("guard", 0))})
            if kinds.get("restart", 0) < 1:
                self.issue("high", "opt", "no engine restart happened in the whole run (gcpar and fps30 need one)", shot=False)
            self.adb.sh("run-as %s cat files/optimizer/last.json > /dev/null" % pkg, timeout=30)
        pids = self.ph.pids()
        self.main_pid = pids[0] if pids else None
        self.record({"kind": "opt", "name": "auto-tune-quick", "status": "ok" if all(r["ok"] for r in rows) else "failed", "results": rows, "opt_events": kinds, "opt_json": (result or "")[:6000]})
'''
sub('tools/emu/soak.py', '    def lastrun_round(self):', OPT.lstrip('\n') + '\n    def lastrun_round(self):')
sub('tools/emu/soak.py', '        if a.suite == "lr":\n            s.lastrun_round()', '        if a.suite == "opt":\n            s.opt_round()                                    # (OPT1: Auto-tune end to end: the short plan, a restart that comes back, the results file)\n        if a.suite == "lr":\n            s.lastrun_round()')
s = open(os.path.join(R, 'tools/emu/soak.py'), encoding='utf-8').read()
m = re.search(r'choices=\["smoke", "fs", "soak", "all",', s)
assert m, 'choices'
s = s.replace('choices=["smoke", "fs", "soak", "all",', 'choices=["smoke", "fs", "soak", "all", "opt",', 1)
open(os.path.join(R, 'tools/emu/soak.py'), 'w', encoding='utf-8').write(s)
sub('.github/workflows/android.yml', 'options: [smoke, fs, vc,', 'options: [smoke, opt, fs, vc,')
sub('.github/workflows/emu-lab.yml', 'options: [smoke, fs, vc,', 'options: [smoke, opt, fs, vc,')
print('applied3')
