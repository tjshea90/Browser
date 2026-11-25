# CHECKPOINT 3329 — read me first, then TASKS.md

**Written:** 2026-11-25T04:22:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `eeac4ff3` (this checkpoint is the commit after it)

## Just done
reviewed the owner's first real Auto-tune run (run 20261125-034537, DONE on the tablet, nothing proven better) + the general diagnostics digest; findings in research/OPTIMIZER.md, follow-ups OPT3a-e in TASKS (digest dedupe done)

## Do this next
alpha108 = OPT3a (budget by real elapsed minutes in core Machine.next, startedAt vs now, + test), OPT3b (bench clips play: exempt loopback bench pages from the clip limiter in geckoapp/src/main/assets/kestrel-shields content/gate scripts, assert clips_playing>0 in web-tests/bench.test.js), OPT3c (outlier guard in core Decide.kt: both samples worse or third sample; test with 13060/7770 vs 7210), OPT3e (hw1 glitch question even when hw1 is already on). Then ship per the release order and tell the owner. Wait for the owner's answers on PERF25a (Brave device, hardware drawing trial text test)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/OPTIMIZER.md
     M tools/optimizer_digest.py

## Last ten checkpoints
```
  eeac4ff3 ckpt 3328: alpha107 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha107, run 455370
  788928f7 ckpt 3326: alpha107 build dispatched ONCE (run 45537054629, head d23bd30b, app=gecko fu
  d23bd30b ckpt 3325: gated kestrel-g-v0.3.0-alpha107 (gecko code 208) and pushed it: handoff self
  13f6ec0b ckpt 3324: OPT1c lab v14 read: the screen thread froze inside a native draw call right 
  62999022 ckpt 3321: OPT1c: lab opt v14 dispatched (run 45533868639, head c64696f3)
  c64696f3 ckpt 3320: OPT1c lab v13 read: clean up to the first experiment's restart, then the emu
  d05a3d57 ckpt 3318: OPT1c: lab opt v13 dispatched (run 45530873833, head 86130d11)
  86130d11 ckpt 3317: OPT1c lab v12 read: baseline fine, run stops at the first LIVE experiment (p
  ac1b71e1 ckpt 3314: OPT1c: lab opt v12 dispatched (run 45526821428, head 673b3ac6)
  673b3ac6 ckpt 3313: OPT1c lab v11 read (log readable thanks to the step timeout): first restart 
```
