# CHECKPOINT 2721 — read me first, then TASKS.md

**Written:** 2026-11-20T04:12:02Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `e63451b9` (this checkpoint is the commit after it)

## Just done
RVW1b-4: P1 names.js gate, P10 map caps, N6 disposition cut done with web tests (names 10/10, background RVW1 tests); :geckoapp + :app compile EXIT 0 (V10, F3 edits compile)

## Do this next
Next: naming batch in core: N1 EpisodeNames.firstSegmentTitle join of up to 2 non-site segments (stop at a year) + leading '(3)' counter strip (N3), N2 MediaNames.isRealName (quality tags, UUID/hex, digit-heavy, GENERIC + hd sd hq lq high low medium normal original source mobile), N4 FileNames.sanitize keeps U+200C/U+200D, N5 FileNames.cutChars for take(120)/take(80), N8 file-name label without 4K dot; tests EpisodeNamesTest MediaNamesTest FileNamesTest; then security batch (Notifier open gate S1, private URL logs S6, UrlParts backslash S9, secretscan S7)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  e139e52d ckpt 2720: RVW1b-3 DONE: download engine V1 V2 V6 V7 V10 V12 V13 + S4 S5 fixed with tes
  25206010 ckpt 2719: RVW1b-2 DONE: F7 F10 F14 F15 F18 F20 F22 fixed with tests (core MediaSniff/M
  5364b783 ckpt 2718: RVW1b-1 DONE: finder F1 F2 F3 F4 F9 F12 fixed with tests (finder2.test.js 6/
  aff75506 ckpt 2717: RVW1b: F1 (vid reports name the tab page + per-frame element numbers, 2 web 
  edc76eba ckpt 2716: RVW1a DONE: all four review lanes saved (research/REVIEW_2026-11-20{,_FINDER
  64f64826 ckpt 2715: RVW1a finder lane saved to research/REVIEW_2026-11-20_FINDER.md (F1-F24: F1 
  d9a44bf6 ckpt 2714: RVW1a speed lane saved to research/REVIEW_2026-11-20_SPEED.md (P1-P11); secu
  cf89270d ckpt 2713: RVW1a security lane saved to research/REVIEW_2026-11-20_SECURITY.md (S1-S10,
  6a9b6024 ckpt 2712: RVW1 written into TASKS.md in the owner's words (whole-browser review + vide
  9bdcb1ba ckpt 2711: DG2: owner pasted the tail of the same Diagnostics (events +2892..+2925 s): 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
