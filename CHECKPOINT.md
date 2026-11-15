# CHECKPOINT 2421 — read me first, then TASKS.md

**Written:** 2026-11-15T20:12:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `d163315b` (this checkpoint is the commit after it)

## Just done
compat run 1 (43747605584) read: only 4 sites measured, the emulated phone's system process was killed 3 times (software GL + 2 GB), 'crashes' were com.android.phone / svc, the ANR was the software GPU draw; lab made gentler: 4 GB for compat, tabs closed each site, nolook debug switch, 90 s page timeout, app-only crash detector; research/COMPAT_RUN1.md written

## Do this next
NEXT: read the fsy run (dispatched ~19:58Z behind the compat run; find it with actions_list list_workflow_runs, then artifacts emulator-lab-text/shots); then alpha50 (RA1-RA4 + nolook hook + whatever fsy shows); then re-dispatch compat with lab_minutes=120 (lab_net=full)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M tools/emu/soak.py
    ?? research/COMPAT_RUN1.md

## Last ten checkpoints
```
  b0aa0482 ckpt 2420: lab driver: Yahoo Full-button tap x 556 (inside the button for film widths 5
  a88bd7cb ckpt 2419: alpha49 review (a1b60178ab815123f) fixed: RA1 media state forgotten with the
  2de3f20b ckpt 2418: alpha49 shipped and recorded: Release kestrel-g-v0.3.0-alpha49 (run 43751318
  7e7d3319 ckpt 2417: lab scene states fixed (the app turns a wide video sideways by itself at ful
  0a81c2b8 ckpt 2416: gated kestrel-g-v0.3.0-alpha49 (gecko code 150) and pushed it: handoff self-
  afdbb7c6 ckpt 2415: pre-ship: alpha49: full-screen video fixes after your three screenshots. (1)
  7c987e9f ckpt 2414: code for owner message 46 + second review: FSB1 (applyImmersive without LAYO
  7f2c4c92 ckpt 2413: owner message 46 written into TASKS.md (FSB1 bars cover the controls in full
  b8c5d333 ckpt 2412: hour soak 43740398387 read (commit 281c48f4, hotspot-shaped, 70 min, 79 scen
  9edf1ab7 ckpt 2411: LAB2 harness written: lab-only page-health probe (vfind.js probe(), guard an
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
