# CHECKPOINT 3337 — read me first, then TASKS.md

**Written:** 2026-11-25T04:57:27Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `ad9506c7` (this checkpoint is the commit after it)

## Just done
alpha108 build dispatched ONCE (run 45550204316, head ad9506c7, app=gecko full_build=true); do NOT dispatch it again

## Do this next
Poll run 45550204316 with mcp__github__actions_list list_workflow_jobs (a full gecko build took about 13-30 min). When green: mcp__github__get_release_by_tag tjshea90/Browser kestrel-g-v0.3.0-alpha108 (APK present), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha108 with the IDENTICAL note (docs/handoff/ALPHA108_RELEASE_NOTE.txt with newlines removed), tick TASKS OPT4c, then answer the owner: Release link on its own line, what changed and why (research/OPT4_ACTIONS.md), what was NOT run on the tablet, how to run Auto-tune again (about 40 min, tests everything with the fixed clip arm), the open questions (hardware drawing trial 1 missing-text check, Brave device)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ad9506c7 ckpt 3336: gated kestrel-g-v0.3.0-alpha108 (gecko code 209) and pushed it: handoff self
  e096411f ckpt 3335: OPT4: alpha108 built from the evidence: hw2 gone (Settings, Prefs 0..1, Cata
  0aea0586 ckpt 3330: OPT4 written into TASKS in the owner's words (act on the Auto-tune run + all
  803695fb ckpt 3329: reviewed the owner's first real Auto-tune run (run 20261125-034537, DONE on 
  eeac4ff3 ckpt 3328: alpha107 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha107, run 455370
  788928f7 ckpt 3326: alpha107 build dispatched ONCE (run 45537054629, head d23bd30b, app=gecko fu
  d23bd30b ckpt 3325: gated kestrel-g-v0.3.0-alpha107 (gecko code 208) and pushed it: handoff self
  13f6ec0b ckpt 3324: OPT1c lab v14 read: the screen thread froze inside a native draw call right 
  62999022 ckpt 3321: OPT1c: lab opt v14 dispatched (run 45533868639, head c64696f3)
  c64696f3 ckpt 3320: OPT1c lab v13 read: clean up to the first experiment's restart, then the emu
```
