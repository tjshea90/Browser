# CHECKPOINT 1678 — read me first, then TASKS.md

**Written:** 2026-11-13T20:56:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `a275dce` (this checkpoint is the commit after it)

## Just done
AL5: found why a script-named address in a player frame outlived a server switch (content-script message had no tab, so the address was filed under the player document as 'page' with no frame; the later real request could not take it over). Fixed: background.js frameTab (frame->tab from the sub_frame request), docOf() normalisation, dom message names page+frame; MediaList.add lets the newest sighting name the frame; web tests 132 pass, MediaListTest added. Lab rerun in progress (build -> install -> lab2 -> read frame-gone lines by hand)

## Do this next
read frame-gone lines: S2->S3 and S3->S1 must now say dropped=3 left=0; then tick AL1/AL5 etc, full suites (core, app, lint, web), ship alpha32; then Wave 1 (EY-2 ...)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a3bd814 ckpt 1673: report saved as research/REVIEW-EXTERNAL-2.md and its TASKS block (EY-1..EY-3
  d19f54f ckpt 1672: owner message 28 written into TASKS as AN1 (review and complete all fixes and
  d14b0ab ckpt 1665: AL5 emulator lab found a real bug and it is fixed in code: after two server s
  e03327f ckpt 1661: AM1 CODE DONE: removed Tap lab, Decode lab (assets/labs 394 KB), Scroll test 
  bab3a84 ckpt 1650: owner message 27 written into TASKS as AM1 (remove finished diagnosis tools);
  a395248 ckpt 1648: AL2 coded (core/download/MiniTracker + 9 tests, ui/DownloadPopup banner + top
  f3ad3c6 ckpt 1634: AL3 coded: Format.barText/belowBar (core, PlannerTest green), new ui/Download
  e688a4c ckpt 1629: AL4 done (cancel deletes partial file on every path; Clear/restore leaks clos
  f43eec4 ckpt 1619: owner message 26 ('The download works now' = alpha31 saved the vidsrc movie) 
  cdab618 ckpt 1609: alpha31 DONE and recorded (BUILDLOG line, run 43482256190 green, APK 93,443,1
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
