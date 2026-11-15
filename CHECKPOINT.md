# CHECKPOINT 2425 — read me first, then TASKS.md

**Written:** 2026-11-15T20:43:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `a894c59f` (this checkpoint is the commit after it)

## Just done
alpha50 shipped and recorded (Release kestrel-g-v0.3.0-alpha50, run 43758859623 green, commit 6689210f): RA1-RA4 review fixes; fsy lab re-dispatched after the 16 MB heap anomaly (ShieldsBridge OOM guard + heap log + workflow heap props print are in the new build)

## Do this next
NEXT: read the fsy re-run when it ends (about 21:20Z): actions_list list_workflow_run_artifacts on the newest lab run id (list_workflow_runs perPage 1 filter completed), download emulator-lab-text, fetch_artifact.py to scratchpad/fsy2; look at report.md sections 'Full screen: the system bars' (red strip rows hidden vs kept; kept must end above y=height-52; bars note must say KEPT), turn settled lines, Kestrel bar in the full-screen web component (nobar row), yahoofs-* pictures from emulator-lab-shots, sound table; fix what shows; then re-dispatch compat with lab_minutes=120; final answer to the owner; delete heartbeat trigger trig_01Y8CFHKVb6hmQmEEM78p6gv only when everything is written up

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  4415fbaf ckpt 2424: lab run 43753283814 (fsy) read: it says NOTHING about the full-screen code: 
  6689210f ckpt 2423: gated kestrel-g-v0.3.0-alpha50 (gecko code 151) and pushed it: handoff self-
  ca8bc3fb ckpt 2422: pre-ship: alpha50: fixes from an independent check of alpha49 (everything el
  342c62e0 ckpt 2421: compat run 1 (43747605584) read: only 4 sites measured, the emulated phone's
  b0aa0482 ckpt 2420: lab driver: Yahoo Full-button tap x 556 (inside the button for film widths 5
  a88bd7cb ckpt 2419: alpha49 review (a1b60178ab815123f) fixed: RA1 media state forgotten with the
  2de3f20b ckpt 2418: alpha49 shipped and recorded: Release kestrel-g-v0.3.0-alpha49 (run 43751318
  7e7d3319 ckpt 2417: lab scene states fixed (the app turns a wide video sideways by itself at ful
  0a81c2b8 ckpt 2416: gated kestrel-g-v0.3.0-alpha49 (gecko code 150) and pushed it: handoff self-
  afdbb7c6 ckpt 2415: pre-ship: alpha49: full-screen video fixes after your three screenshots. (1)
```
