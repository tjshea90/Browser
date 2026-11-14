# CHECKPOINT 1954 — read me first, then TASKS.md

**Written:** 2026-11-14T03:04:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0a7c905` (this checkpoint is the commit after it)

## Just done
alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, started 2026-11-14T03:04:10Z) on ccr-a57c163e-mtq8js, full_build=true. Local gates all green first (core, app incl. 23 MiniNavTest, 68 glide/keyboard tests, gecko compile, lint, web-tests 150/150).

## Do this next
WAIT for run 43549663530 (about 8 min): git ls-remote --tags origin kestrel-g-v0.3.0-alpha34 shows the tag when the publish step runs; mcp__github__actions_list list_workflow_jobs 43549663530 filter all must show every job success (build-gecko incl. pref check + publish); mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha34 shows the APK; THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha34 "$(cat scratchpad/alpha34_note.txt)" (scratchpad = /tmp/claude-0/-home-user-Browser/30a5aa00-41c9-5bd3-87d1-b10052a52d84/scratchpad, file alpha34_note.txt; if lost: the note is in this commit message). NEVER dispatch alpha34 twice. Then: bash tools/claim.sh AT1 --done with evidence (emulator proof screenshots glide_d/e/f/g/trail, Diagnostics line, tests), answer the owner in plain words (glide feature, SIMULATED accuracy table, key-size finding, NOT run on the tablet, questions: Spanish list AT2, bigger keyboard option, paste the Glide typing Diagnostics line; re-list open items), ckpt. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN refreshed earlier.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0a7c905 ckpt 1953: gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-t
  eedfd73 ckpt 1949: AT1h emulator proof DONE with a real multi-point finger (sendevent-style bina
  58ae170 ckpt 1945: AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs t
  54eae48 ckpt 1931: AT1e core half done and green (45 tests in core input): MiniKeyboardModel.com
  7aca167 ckpt 1925: AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny
  5ae6905 ckpt 1920: AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/mai
  b547793 ckpt 1898: Owner message 34 written into TASKS (AT1 glide typing on the mini keyboard, b
  307f5cf ckpt 1883: alpha33 SHIPPED AND RECORDED: run 43536066565 green (all jobs success, pref c
  79e5865 ckpt 1877: alpha33 (gecko code 134) DISPATCHED: android.yml run 43536066565 (run #84, st
  f11808c ckpt 1876: gated kestrel-g-v0.3.0-alpha33 (gecko code 134) and pushed it: handoff self-t
```
