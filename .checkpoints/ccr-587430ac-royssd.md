# CHECKPOINT 2689 — read me first, then TASKS.md

**Written:** 2026-11-18T07:12:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `a7fef86c` (this checkpoint is the commit after it)

## Just done
alpha70 RELEASED: tag kestrel-g-v0.3.0-alpha70, run 44255522156 all jobs green, APK 96 MB on the Release (https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha70), recorded in BUILDLOG.md (b25d7899). TASKS ticked (CR1f/g, VS2a/b, VS1e, TP1, UC1, MT1, PV1); VS2c (vidsrc/111movies themselves) stays OPEN for the owner's Diagnostics. HANDOFF lessons added.

## Do this next
NOTHING running. Wait for the owner: install alpha70 from https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha70 and try: tab previews (Diagnostics line Tab previews = 0 wrong), Copy button on the address bar, Tabs button + tray in the floating panel, a new private tab (no flash of an old regular tab), doraby rows staying in the list, and on a vanish send Diagnostics (Last run line with memory). VS2c needs Diagnostics taken while vidsrc / 111movies play. No heartbeat trigger exists.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M HANDOFF.md

## Last ten checkpoints
```
  27d9762c ckpt 2688: alpha70 DISPATCHED ONCE at 07:00Z: android.yml run 44255522156 (run_number 2
  2e8c8251 ckpt 2687: alpha70 GATED locally (ship.sh rc=0: handoff self-test, unit suites, version
  30a0825b ckpt 2686: gated kestrel-g-v0.3.0-alpha70 (gecko code 171) and pushed it: handoff self-
  caf37927 ckpt 2685: Lab run 44252661300 (new2) READ: the app behaved right on every check (UC1 v
  e5fc78d7 ckpt 2684: Lab run 44252661300 (suite new2 = tb + lastrun_round, head d0e7d41d) DISPATC
  d0e7d41d ckpt 2683: Lab run 44248671431 READ: freeze scene proves CR1f on a real Android 10 (SCR
  6617fbe2 ckpt 2682: Full JVM suite 9 GREEN (rc=0) + web 616/616 with TP1, UC1 (dock), MT1, PV1 c
  3dda7232 ckpt 2681: PV1 coded: CurtainRule (core, 3 tests) + curtain view over the shared page v
  51f3e413 ckpt 2680: Owner message PV1 written into TASKS (new private tab flashed an old regular
  92b293d1 ckpt 2679: MT1 coded (mini panel Tabs button + tab tray, TabTrayModel, host wiring, 9 R
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
