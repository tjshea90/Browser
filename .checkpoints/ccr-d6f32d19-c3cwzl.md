# CHECKPOINT 2717 — read me first, then TASKS.md

**Written:** 2026-11-20T03:53:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `75d84d6b` (this checkpoint is the commit after it)

## Just done
RVW1b: F1 (vid reports name the tab page + per-frame element numbers, 2 web tests), F4 (ranged files report the whole-file URL, 2 web tests; background.test.js 110/110), F2 (MediaList.add clears fromMarkup on a wire sighting, core test written), F3 (VideoFlow looks at DASH + maybe() streams, compile-only) are IN THE TREE; core MediaListTest run in background (scratchpad ml.log)

## Do this next
Read scratchpad ml.log for EXIT 0 (MediaListTest + FinderCorpusTest); then compile :geckoapp:compileDebugKotlin (needs platforms;android-37.0 per CLAUDE.md if missing); then F9 + F12 + F6 in content.js/manifest.json with tests in web-tests/content.test.js (60 s per-file limit: put new tests in a NEW file web-tests/finder2.test.js)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  edc76eba ckpt 2716: RVW1a DONE: all four review lanes saved (research/REVIEW_2026-11-20{,_FINDER
  64f64826 ckpt 2715: RVW1a finder lane saved to research/REVIEW_2026-11-20_FINDER.md (F1-F24: F1 
  d9a44bf6 ckpt 2714: RVW1a speed lane saved to research/REVIEW_2026-11-20_SPEED.md (P1-P11); secu
  cf89270d ckpt 2713: RVW1a security lane saved to research/REVIEW_2026-11-20_SECURITY.md (S1-S10,
  6a9b6024 ckpt 2712: RVW1 written into TASKS.md in the owner's words (whole-browser review + vide
  9bdcb1ba ckpt 2711: DG2: owner pasted the tail of the same Diagnostics (events +2892..+2925 s): 
  79448c72 ckpt 2710: DG1e answered to the owner; DG1 stays open until alpha72 ships and the next 
  34502e99 ckpt 2709: DG1a-d DONE in code: LastRun.kt tells an update from a crash and marks the f
  e8adf8c4 ckpt 2708: DG1 written into TASKS (owner Diagnostics paste, alpha71): finding 1 = every
  4acfe271 ckpt 2707: alpha71 RELEASED and RECORDED: https://github.com/tjshea90/Browser/releases/
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
