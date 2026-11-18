# CHECKPOINT 2688 — read me first, then TASKS.md

**Written:** 2026-11-18T07:00:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `2e8c8251` (this checkpoint is the commit after it)

## Just done
alpha70 DISPATCHED ONCE at 07:00Z: android.yml run 44255522156 (run_number 242, app=gecko full_build=true, head 2e8c8251). Do NOT dispatch again. Lab run 44254127252 (tb) was clean: 3 scenes ok, 0 issues (TP1, UC1, MT1, PV1 proven on the real engine in the simulator).

## Do this next
WAIT for run 44255522156 (list_workflow_jobs; ~12 min: test-jvm ~5, test-js ~8, build-gecko ~4); do not use get_workflow_run (huge). If ALL jobs green: git ls-remote --tags origin kestrel-g-v0.3.0-alpha70, mcp__github__get_release_by_tag (APK asset), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha70 with the IDENTICAL note (cat docs/handoff/ALPHA70_RELEASE_NOTE.txt), tick VS2/TP1/UC1/MT1/PV1 and CR1f/g in TASKS.md, answer the owner in plain words with the Release link on its own line. If a job is red: read it (get_job_logs only after the job ended); a flaky test (BrowserFlowTest.viewIntentOpensTheUrlInTheBrowser, PhoneBackendTest) -> make it deterministic, ship a FRESH dispatch only if no tag exists

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2e8c8251 ckpt 2687: alpha70 GATED locally (ship.sh rc=0: handoff self-test, unit suites, version
  30a0825b ckpt 2686: gated kestrel-g-v0.3.0-alpha70 (gecko code 171) and pushed it: handoff self-
  caf37927 ckpt 2685: Lab run 44252661300 (new2) READ: the app behaved right on every check (UC1 v
  e5fc78d7 ckpt 2684: Lab run 44252661300 (suite new2 = tb + lastrun_round, head d0e7d41d) DISPATC
  d0e7d41d ckpt 2683: Lab run 44248671431 READ: freeze scene proves CR1f on a real Android 10 (SCR
  6617fbe2 ckpt 2682: Full JVM suite 9 GREEN (rc=0) + web 616/616 with TP1, UC1 (dock), MT1, PV1 c
  3dda7232 ckpt 2681: PV1 coded: CurtainRule (core, 3 tests) + curtain view over the shared page v
  51f3e413 ckpt 2680: Owner message PV1 written into TASKS (new private tab flashed an old regular
  92b293d1 ckpt 2679: MT1 coded (mini panel Tabs button + tab tray, TabTrayModel, host wiring, 9 R
  bd1bb9e4 ckpt 2678: UC1 coded + tested: Copy button inside the address pill (Toolbar.copyButton)
```
