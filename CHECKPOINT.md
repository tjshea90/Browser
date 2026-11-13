# CHECKPOINT 1683 — read me first, then TASKS.md

**Written:** 2026-11-13T21:21:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `367f4d1` (this checkpoint is the commit after it)

## Just done
AL5 root cause found with a debug logcat mirror of the Health events (new: Health.mirror, debug builds only): on the first page after a cold start the extension has not seen the page's own request, so its tabTop map is empty and an address named by a player frame's script was filed under the player document; fix = learnTop from the frame's ancestors + docOf normalisation + MediaList adopts the newest frame; web tests 132 pass; final lab run in progress

## Do this next
read /tmp .../scratchpad/m7_media.txt after the lab chain (build_al11 -> install -> al5_lab6): S2->S3 must say dropped=3 left=0; then tick AL1-AL6/AM1, run suites, ship alpha32

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  37e9055 ckpt 1678: AL5: found why a script-named address in a player frame outlived a server swi
  a3bd814 ckpt 1673: report saved as research/REVIEW-EXTERNAL-2.md and its TASKS block (EY-1..EY-3
  d19f54f ckpt 1672: owner message 28 written into TASKS as AN1 (review and complete all fixes and
  d14b0ab ckpt 1665: AL5 emulator lab found a real bug and it is fixed in code: after two server s
  e03327f ckpt 1661: AM1 CODE DONE: removed Tap lab, Decode lab (assets/labs 394 KB), Scroll test 
  bab3a84 ckpt 1650: owner message 27 written into TASKS as AM1 (remove finished diagnosis tools);
  a395248 ckpt 1648: AL2 coded (core/download/MiniTracker + 9 tests, ui/DownloadPopup banner + top
  f3ad3c6 ckpt 1634: AL3 coded: Format.barText/belowBar (core, PlannerTest green), new ui/Download
  e688a4c ckpt 1629: AL4 done (cancel deletes partial file on every path; Clear/restore leaks clos
  f43eec4 ckpt 1619: owner message 26 ('The download works now' = alpha31 saved the vidsrc movie) 
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
