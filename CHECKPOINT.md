# CHECKPOINT 2770 — read me first, then TASKS.md

**Written:** 2026-11-20T22:13:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `1c15f326` (this checkpoint is the commit after it)

## Just done
YTC2: cause NOT proven. Read every tap path again (ControlsMachine, tap matching, videobar, seekbar, gate, guard, youtube.js, app touch): nothing shows a second tap or a hide. NEW suspect: Mozilla's three YouTube interventions (bundled since alpha75; picture-in-picture and background-playback fixes that rewrite document.hidden / visibilitychange / outerWidth / exitFullscreen); PrepareWebcompat now points their match patterns at youtube.invalid (run.js still parses, 3+ patterns swapped, core SiteFixesRulesTest + OwnerRulesTest green 44/44). Bumped code 179 / alpha78, note written. NOT shipped yet.

## Do this next
REL78: bash ship.sh gecko "$(cat docs/handoff/ALPHA78_RELEASE_NOTE.txt)", ONE dispatch (android.yml app=gecko full_build=true ref claude/sweet-gates-gr3bj5, same note), wait tag kestrel-g-v0.3.0-alpha78 + all jobs, get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha78 with the identical note; then answer the owner (YTC2): link alone on its line, the Settings bisect list, the two questions

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json
    ?? docs/handoff/ALPHA78_RELEASE_NOTE.txt

## Last ten checkpoints
```
  2ffbb683 ckpt 2769: YTC2 written into TASKS.md in the owner's words: alpha77 did not change the 
  36547fc5 ckpt 2768: alpha77 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha77, gecko code 178, run
  3f0787f7 ckpt 2767: REL77b: alpha77 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  2a4e281c ckpt 2766: gated kestrel-g-v0.3.0-alpha77 (gecko code 178) and pushed it: handoff self-
  855b173d ckpt 2765: YTC1: not reproduced (the YouTube film does not start behind its bot check i
  1cfd914e ckpt 2764: YTC1 written into TASKS.md in the owner's words (mid-turn owner message: You
  61ca6460 ckpt 2763: REL76b: alpha76 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5eb5e6aa ckpt 2762: gated kestrel-g-v0.3.0-alpha76 (gecko code 177) and pushed it: handoff self-
  7df4d1c8 ckpt 2761: RVW5 gates all green: :core:test 1916 (OwnerRulesTest guard kept: VideoLevel
  8665429e ckpt 2760: RVW5: coded + tested in core (media tests 524 of 524 green): NA1 title clean
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
