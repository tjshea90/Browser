# CHECKPOINT 3335 — read me first, then TASKS.md

**Written:** 2026-11-25T04:56:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `1cc7c437` (this checkpoint is the commit after it)

## Just done
OPT4: alpha108 built from the evidence: hw2 gone (Settings, Prefs 0..1, Catalog.RETIRED), bench clips allowed to start (permission delegate), budget in real minutes, outlier guard, catalog v2, digest dedupe; note written; core 2105 + app 508 + web 10 green

## Do this next
ship alpha108: NOTE=$(tr -d newline < docs/handoff/ALPHA108_RELEASE_NOTE.txt); bash ship.sh gecko "$NOTE" (background), ONE dispatch app=gecko full_build=true note=$NOTE ref=claude/kind-brahmagupta-ae3yl8, poll list_workflow_jobs, get_release_by_tag kestrel-g-v0.3.0-alpha108, bash tools/record-release.sh kestrel-g-v0.3.0-alpha108 with the identical note, tick OPT4c, answer the owner (Release link on its own line)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json
    ?? docs/handoff/ALPHA108_RELEASE_NOTE.txt

## Last ten checkpoints
```
  0aea0586 ckpt 3330: OPT4 written into TASKS in the owner's words (act on the Auto-tune run + all
  803695fb ckpt 3329: reviewed the owner's first real Auto-tune run (run 20261125-034537, DONE on 
  eeac4ff3 ckpt 3328: alpha107 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha107, run 455370
  788928f7 ckpt 3326: alpha107 build dispatched ONCE (run 45537054629, head d23bd30b, app=gecko fu
  d23bd30b ckpt 3325: gated kestrel-g-v0.3.0-alpha107 (gecko code 208) and pushed it: handoff self
  13f6ec0b ckpt 3324: OPT1c lab v14 read: the screen thread froze inside a native draw call right 
  62999022 ckpt 3321: OPT1c: lab opt v14 dispatched (run 45533868639, head c64696f3)
  c64696f3 ckpt 3320: OPT1c lab v13 read: clean up to the first experiment's restart, then the emu
  d05a3d57 ckpt 3318: OPT1c: lab opt v13 dispatched (run 45530873833, head 86130d11)
  86130d11 ckpt 3317: OPT1c lab v12 read: baseline fine, run stops at the first LIVE experiment (p
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
