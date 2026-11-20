# CHECKPOINT 2764 — read me first, then TASKS.md

**Written:** 2026-11-20T20:45:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `61ca6460` (this checkpoint is the commit after it)

## Just done
YTC1 written into TASKS.md in the owner's words (mid-turn owner message: YouTube controls flash and vanish on tap; laggy playback; slow buffering; unsure if only YouTube). alpha76 tag kestrel-g-v0.3.0-alpha76 EXISTS (run 44811598154): still to confirm all jobs green + Release APK, then record-release with the identical note (docs/handoff/ALPHA76_RELEASE_NOTE.txt).

## Do this next
1) mcp__github__actions_list list_workflow_jobs 44811598154 until build-gecko etc. are all success, get_release_by_tag kestrel-g-v0.3.0-alpha76 shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha76 "$(cat docs/handoff/ALPHA76_RELEASE_NOTE.txt)". 2) YTC1a: read geckoapp/src/main/assets/kestrel-shields/youtube.js videobar.js player.js seekbar.js and the tap path (BrowserActivity touch handling) for a YouTube tap that shows then hides controls at once; YTC1b measure lag/buffering cost of our scripts on youtube.com (headless Chromium gets 429: use the modelled pages in tools/sitelab/ytlab.py and research/YOUTUBE_ADS.md)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  61ca6460 ckpt 2763: REL76b: alpha76 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5eb5e6aa ckpt 2762: gated kestrel-g-v0.3.0-alpha76 (gecko code 177) and pushed it: handoff self-
  7df4d1c8 ckpt 2761: RVW5 gates all green: :core:test 1916 (OwnerRulesTest guard kept: VideoLevel
  8665429e ckpt 2760: RVW5: coded + tested in core (media tests 524 of 524 green): NA1 title clean
  58b7c173 ckpt 2759: RVW5 written into TASKS.md in the owner's words (new levers: speed, playback
  80ae116d ckpt 2758: RVW4 DONE AND SHIPPED: alpha75 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a0ad95ec ckpt 2757: REL75b: alpha75 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  eebefd5f ckpt 2756: gated kestrel-g-v0.3.0-alpha75 (gecko code 176) and pushed it: handoff self-
  9e99464e ckpt 2755: RVW4: whole JVM suite GREEN (:core:test 1907 tests 0 failures 12 skipped as 
  b1ec2fd4 ckpt 2754: NL1 PROVEN on the real engine: lab run 44787922316 (daily, 62 scenes, 12 min
```
