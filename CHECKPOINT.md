# CHECKPOINT 2719 — read me first, then TASKS.md

**Written:** 2026-11-20T04:01:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `6581cbee` (this checkpoint is the commit after it)

## Just done
RVW1b-2 DONE: F7 F10 F14 F15 F18 F20 F22 fixed with tests (core MediaSniff/MediaList/FinderCorpus EXIT 0; background.test.js RVW1 + sniffer 14/14; finder2.test.js 7)

## Do this next
Next: download-engine robustness in core/.../media/StreamEngine.kt: V1 (retry + FetchException for playlist/key reads, key cache by URL), V2 (200 reply to a Range request must keep the whole body), S4/S5 (Hls.parse caps, 17-byte key read), V6/V7 (Dash r=-1 ceil + BOM trim), V10 (final URL after redirects), V12 (Retry-After clamp via HttpSemantics.retryAfterMs), V13 (read-ahead cap); tests in StreamEngineTest / HlsTest / DashTest; run: ./gradlew --offline -q :core:test --tests '*StreamEngine*' --tests '*HlsTest*' --tests '*DashTest*'

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  5364b783 ckpt 2718: RVW1b-1 DONE: finder F1 F2 F3 F4 F9 F12 fixed with tests (finder2.test.js 6/
  aff75506 ckpt 2717: RVW1b: F1 (vid reports name the tab page + per-frame element numbers, 2 web 
  edc76eba ckpt 2716: RVW1a DONE: all four review lanes saved (research/REVIEW_2026-11-20{,_FINDER
  64f64826 ckpt 2715: RVW1a finder lane saved to research/REVIEW_2026-11-20_FINDER.md (F1-F24: F1 
  d9a44bf6 ckpt 2714: RVW1a speed lane saved to research/REVIEW_2026-11-20_SPEED.md (P1-P11); secu
  cf89270d ckpt 2713: RVW1a security lane saved to research/REVIEW_2026-11-20_SECURITY.md (S1-S10,
  6a9b6024 ckpt 2712: RVW1 written into TASKS.md in the owner's words (whole-browser review + vide
  9bdcb1ba ckpt 2711: DG2: owner pasted the tail of the same Diagnostics (events +2892..+2925 s): 
  79448c72 ckpt 2710: DG1e answered to the owner; DG1 stays open until alpha72 ships and the next 
  34502e99 ckpt 2709: DG1a-d DONE in code: LastRun.kt tells an update from a crash and marks the f
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
