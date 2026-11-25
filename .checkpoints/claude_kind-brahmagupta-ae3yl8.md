# CHECKPOINT 3324 — read me first, then TASKS.md

**Written:** 2026-11-25T02:59:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `54085c03` (this checkpoint is the commit after it)

## Just done
OPT1c lab v14 read: the screen thread froze inside a native draw call right after the quiet restart was requested (emulator graphics), so no more evidence is coming from the emulator. SHIPPING alpha107 (code 208): Flavor.hasAutoTune=true as EXPERIMENT, Settings row says experimental, ALPHA107 note finished honestly (what the lab proved and did not)

## Do this next
ship: bash ship.sh gecko with the note from docs/handoff/ALPHA107_RELEASE_NOTE.txt (tr -d newline; background), ONE dispatch app=gecko full_build=true, find run id, poll list_workflow_jobs, get_release_by_tag kestrel-g-v0.3.0-alpha107, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha107 with the identical note, tick TASKS OPT1d/OPT1e, answer the owner with the Release link and what was NOT run on the tablet (Auto-tune whole run, restarts on Android 10 Go, upload + report screen)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  62999022 ckpt 3321: OPT1c: lab opt v14 dispatched (run 45533868639, head c64696f3)
  c64696f3 ckpt 3320: OPT1c lab v13 read: clean up to the first experiment's restart, then the emu
  d05a3d57 ckpt 3318: OPT1c: lab opt v13 dispatched (run 45530873833, head 86130d11)
  86130d11 ckpt 3317: OPT1c lab v12 read: baseline fine, run stops at the first LIVE experiment (p
  ac1b71e1 ckpt 3314: OPT1c: lab opt v12 dispatched (run 45526821428, head 673b3ac6)
  673b3ac6 ckpt 3313: OPT1c lab v11 read (log readable thanks to the step timeout): first restart 
  ef7e930c ckpt 3311: OPT1c: lab opt v11 dispatched (run 45523035361, head 5fbe60ec)
  5fbe60ec ckpt 3310: OPT1c lab v10 lost: opt step ran to its 17 min cap, then a dead emulator hun
  599acf9b ckpt 3308: OPT1c: lab opt v10 dispatched (run 45517648960, head d8c21030)
  d8c21030 ckpt 3307: OPT1c lab v9 read: the restart after a wedged page was lost because the old 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
