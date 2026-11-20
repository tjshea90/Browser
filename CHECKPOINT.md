# CHECKPOINT 2716 — read me first, then TASKS.md

**Written:** 2026-11-20T03:51:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `d1eee4a4` (this checkpoint is the commit after it)

## Just done
RVW1a DONE: all four review lanes saved (research/REVIEW_2026-11-20{,_FINDER,_DOWNLOAD,_SECURITY,_SPEED}.md); first fix IN THE TREE: background.js vid handler uses the tab's page + a per-frame element number (F1), test not written yet

## Do this next
Add the F1 test to web-tests/background.test.js (message from frameId 5 with p = the frame's own address must come out with p = tabTop page and el = 1000000 * n + el), run: cd web-tests && node --test --test-timeout=60000 background.test.js; then F2/F3 in core MediaList + VideoFlow; baseline core test log is in the scratchpad core-test.log

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  64f64826 ckpt 2715: RVW1a finder lane saved to research/REVIEW_2026-11-20_FINDER.md (F1-F24: F1 
  d9a44bf6 ckpt 2714: RVW1a speed lane saved to research/REVIEW_2026-11-20_SPEED.md (P1-P11); secu
  cf89270d ckpt 2713: RVW1a security lane saved to research/REVIEW_2026-11-20_SECURITY.md (S1-S10,
  6a9b6024 ckpt 2712: RVW1 written into TASKS.md in the owner's words (whole-browser review + vide
  9bdcb1ba ckpt 2711: DG2: owner pasted the tail of the same Diagnostics (events +2892..+2925 s): 
  79448c72 ckpt 2710: DG1e answered to the owner; DG1 stays open until alpha72 ships and the next 
  34502e99 ckpt 2709: DG1a-d DONE in code: LastRun.kt tells an update from a crash and marks the f
  e8adf8c4 ckpt 2708: DG1 written into TASKS (owner Diagnostics paste, alpha71): finding 1 = every
  4acfe271 ckpt 2707: alpha71 RELEASED and RECORDED: https://github.com/tjshea90/Browser/releases/
  1aa1cf0c ckpt 2706: Lab dr v2 (run 44439418098) READ: wrapper-direct rows are NOT the owner's fl
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
