# CHECKPOINT 1958 — read me first, then TASKS.md

**Written:** 2026-11-14T03:09:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f7546ef` (this checkpoint is the commit after it)

## Just done
Owner message 35 written into TASKS (AU1 invisible long-press corner instead of the visible toggle, AU2 glide keyboard learns from the owner's choices and typing, AU3 mini keyboard types into a focused web text field) before any code. alpha34 is still building on GitHub (run 43549663530): record-release comes first.

## Do this next
1) record alpha34 first (see the previous Do this next: tag wait is running as a background task, then list_workflow_jobs 43549663530 all success, get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha34 with scratchpad alpha34_note.txt; do NOT bump versionCode until recorded); 2) then claim AU1/AU2/AU3 (bash tools/claim.sh AU1 etc.) and work AU1 first (small): MiniNav ToggleView out, invisible hot corner view in BrowserActivity content (touch observed in dispatchTouchEvent so short touches reach the page), panel topMargin = the square's height; AU2 personal model in core/input/glide (PersonalModel) + decoder prior + simulation; AU3 research GeckoView input connection then implement. Version for this batch = alpha35 (versionCode 136).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  7898087 ckpt 1954: alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, st
  0a7c905 ckpt 1953: gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-t
  eedfd73 ckpt 1949: AT1h emulator proof DONE with a real multi-point finger (sendevent-style bina
  58ae170 ckpt 1945: AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs t
  54eae48 ckpt 1931: AT1e core half done and green (45 tests in core input): MiniKeyboardModel.com
  7aca167 ckpt 1925: AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny
  5ae6905 ckpt 1920: AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/mai
  b547793 ckpt 1898: Owner message 34 written into TASKS (AT1 glide typing on the mini keyboard, b
  307f5cf ckpt 1883: alpha33 SHIPPED AND RECORDED: run 43536066565 green (all jobs success, pref c
  79e5865 ckpt 1877: alpha33 (gecko code 134) DISPATCHED: android.yml run 43536066565 (run #84, st
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
