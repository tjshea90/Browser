# CHECKPOINT 2418 — read me first, then TASKS.md

**Written:** 2026-11-15T19:22:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `a0d117be` (this checkpoint is the commit after it)

## Just done
alpha49 shipped and recorded: Release kestrel-g-v0.3.0-alpha49 (run 43751318494 green, APK 94.7 MB, commit 0a81c2b8); interim answer to owner messages 44/45/46 given with the Release link

## Do this next
NEXT: (1) review agent a1b60178ab815123f result (Kotlin diff of alpha49): fix real findings, ship alpha50 if any is real; (2) lab: CANCEL the pending run 43751321208 (pins the old driver commit 0a81c2b8) and re-dispatch app=lab lab_suite=fsy lab_minutes=10 lab_net=full with the current head (soak.py states fixed); read its report: fullscreen_bars table (strip rows hidden vs kept, turn settled lines, Kestrel bar shown), yahoo_fullscreen pictures yahoofs-*, sound table; update research/FULLSCREEN_BARS.md with the proof, tick FSB1/ROT1/FSC1/SND1b rows with the run id; (3) compat run 43747605584 (commit d288355b, ends about 20:10Z): read the artifact into research/COMPAT_RUN1.md, one TASKS row per finding, fix cheap ones; (4) final answer to the owner (ANS44/45/46), delete heartbeat trigger trig_01Y8CFHKVb6hmQmEEM78p6gv only when everything is shipped and written up

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  7e7d3319 ckpt 2417: lab scene states fixed (the app turns a wide video sideways by itself at ful
  0a81c2b8 ckpt 2416: gated kestrel-g-v0.3.0-alpha49 (gecko code 150) and pushed it: handoff self-
  afdbb7c6 ckpt 2415: pre-ship: alpha49: full-screen video fixes after your three screenshots. (1)
  7c987e9f ckpt 2414: code for owner message 46 + second review: FSB1 (applyImmersive without LAYO
  7f2c4c92 ckpt 2413: owner message 46 written into TASKS.md (FSB1 bars cover the controls in full
  b8c5d333 ckpt 2412: hour soak 43740398387 read (commit 281c48f4, hotspot-shaped, 70 min, 79 scen
  9edf1ab7 ckpt 2411: LAB2 harness written: lab-only page-health probe (vfind.js probe(), guard an
  4a9a28f4 ckpt 2410: SND1a lab scene written: lab page /video/audible?kind=video|audio (big butto
  44f9516c ckpt 2409: wrote owner message 45 into TASKS.md in the owner's words: AU1 (sound must n
  d5053cd6 ckpt 2408: owner answered in plain words for messages 43 and 44 (alpha48 Release link g
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
