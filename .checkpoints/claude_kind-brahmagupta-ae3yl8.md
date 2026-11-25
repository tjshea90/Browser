# CHECKPOINT 3328 — read me first, then TASKS.md

**Written:** 2026-11-25T03:32:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `7c48d2ae` (this checkpoint is the commit after it)

## Just done
alpha107 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha107, run 45537054629 green, APK 98 MB, certificate verified). TASKS OPT1/1d/1e ticked

## Do this next
Owner was told in the previous reply what alpha107 is. Wait for the owner: (1) he runs Settings > Auto-tune on the tablet; then python3 -B -I tools/optimizer_digest.py reads optimizer/ in the private repo (needs his diagnostics token + switch on); (2) PERF25a: which device Brave ran on + the hardware drawing trial test result (Settings > Network lab > Hardware drawing trial 1 then 2); (3) older open items PERF17c/d, PERF19, PERF23, PERF14e, PERF9, Back missed place. Do NOT run more emulator labs for opt unless the tablet report shows a real fault; the emulator's graphics freeze is not evidence about the tablet

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  788928f7 ckpt 3326: alpha107 build dispatched ONCE (run 45537054629, head d23bd30b, app=gecko fu
  d23bd30b ckpt 3325: gated kestrel-g-v0.3.0-alpha107 (gecko code 208) and pushed it: handoff self
  13f6ec0b ckpt 3324: OPT1c lab v14 read: the screen thread froze inside a native draw call right 
  62999022 ckpt 3321: OPT1c: lab opt v14 dispatched (run 45533868639, head c64696f3)
  c64696f3 ckpt 3320: OPT1c lab v13 read: clean up to the first experiment's restart, then the emu
  d05a3d57 ckpt 3318: OPT1c: lab opt v13 dispatched (run 45530873833, head 86130d11)
  86130d11 ckpt 3317: OPT1c lab v12 read: baseline fine, run stops at the first LIVE experiment (p
  ac1b71e1 ckpt 3314: OPT1c: lab opt v12 dispatched (run 45526821428, head 673b3ac6)
  673b3ac6 ckpt 3313: OPT1c lab v11 read (log readable thanks to the step timeout): first restart 
  ef7e930c ckpt 3311: OPT1c: lab opt v11 dispatched (run 45523035361, head 5fbe60ec)
```
