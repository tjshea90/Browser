# CHECKPOINT 2765 — read me first, then TASKS.md

**Written:** 2026-11-20T20:52:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `49992212` (this checkpoint is the commit after it)

## Just done
YTC1: not reproduced (the YouTube film does not start behind its bot check in the sandbox); research/YTC1_YOUTUBE_CONTROLS.md written; ControlsMachine.lifeLine diagnostic coded + tested (core player tests + OwnerRulesTest 130 of 130, :geckoapp compile EXIT 0; the guarded line 'if (shown) hide(false) else show(hideAfterMs, false)' kept verbatim). alpha76 RELEASED AND RECORDED (run 44811598154, Release kestrel-g-v0.3.0-alpha76). Bumped to code 178 / alpha77, note written. NOT shipped yet.

## Do this next
REL77: bash ship.sh gecko "$(cat docs/handoff/ALPHA77_RELEASE_NOTE.txt)" then ONE dispatch (android.yml app=gecko full_build=true ref claude/sweet-gates-gr3bj5 note same text), wait tag kestrel-g-v0.3.0-alpha77 + all jobs, get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha77 with the IDENTICAL note; then answer the owner (YTC1c + RVW5g: both Release links each on its own line, the two questions about YouTube, private-clone denial, open items)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json
    ?? docs/handoff/ALPHA77_RELEASE_NOTE.txt

## Last ten checkpoints
```
  1cfd914e ckpt 2764: YTC1 written into TASKS.md in the owner's words (mid-turn owner message: You
  61ca6460 ckpt 2763: REL76b: alpha76 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5eb5e6aa ckpt 2762: gated kestrel-g-v0.3.0-alpha76 (gecko code 177) and pushed it: handoff self-
  7df4d1c8 ckpt 2761: RVW5 gates all green: :core:test 1916 (OwnerRulesTest guard kept: VideoLevel
  8665429e ckpt 2760: RVW5: coded + tested in core (media tests 524 of 524 green): NA1 title clean
  58b7c173 ckpt 2759: RVW5 written into TASKS.md in the owner's words (new levers: speed, playback
  80ae116d ckpt 2758: RVW4 DONE AND SHIPPED: alpha75 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a0ad95ec ckpt 2757: REL75b: alpha75 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  eebefd5f ckpt 2756: gated kestrel-g-v0.3.0-alpha75 (gecko code 176) and pushed it: handoff self-
  9e99464e ckpt 2755: RVW4: whole JVM suite GREEN (:core:test 1907 tests 0 failures 12 skipped as 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
