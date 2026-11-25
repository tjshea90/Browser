# CHECKPOINT 3326 — read me first, then TASKS.md

**Written:** 2026-11-25T03:01:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `d23bd30b` (this checkpoint is the commit after it)

## Just done
alpha107 build dispatched ONCE (run 45537054629, head d23bd30b, app=gecko full_build=true); do NOT dispatch it again

## Do this next
Poll run 45537054629 with mcp__github__actions_list list_workflow_jobs (a full gecko build takes ~25-35 min from 03:01Z). When every job is green: mcp__github__get_release_by_tag tjshea90/Browser kestrel-g-v0.3.0-alpha107 (must show the APK), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha107 with the IDENTICAL note (docs/handoff/ALPHA107_RELEASE_NOTE.txt, newlines removed), tick TASKS OPT1d/OPT1e, then answer the owner: the Release link as plain text on its own line, what the lab did and did not prove, what was NOT run on the tablet (a whole Auto-tune run, restarts on Android 10 Go, the upload and report screen), and re-list the open items (PERF25a Brave device + hardware drawing trial test, PERF17c/d, PERF19, PERF23, PERF14e, PERF9, Back missed place, OPT2 ideas)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d23bd30b ckpt 3325: gated kestrel-g-v0.3.0-alpha107 (gecko code 208) and pushed it: handoff self
  13f6ec0b ckpt 3324: OPT1c lab v14 read: the screen thread froze inside a native draw call right 
  62999022 ckpt 3321: OPT1c: lab opt v14 dispatched (run 45533868639, head c64696f3)
  c64696f3 ckpt 3320: OPT1c lab v13 read: clean up to the first experiment's restart, then the emu
  d05a3d57 ckpt 3318: OPT1c: lab opt v13 dispatched (run 45530873833, head 86130d11)
  86130d11 ckpt 3317: OPT1c lab v12 read: baseline fine, run stops at the first LIVE experiment (p
  ac1b71e1 ckpt 3314: OPT1c: lab opt v12 dispatched (run 45526821428, head 673b3ac6)
  673b3ac6 ckpt 3313: OPT1c lab v11 read (log readable thanks to the step timeout): first restart 
  ef7e930c ckpt 3311: OPT1c: lab opt v11 dispatched (run 45523035361, head 5fbe60ec)
  5fbe60ec ckpt 3310: OPT1c lab v10 lost: opt step ran to its 17 min cap, then a dead emulator hun
```
