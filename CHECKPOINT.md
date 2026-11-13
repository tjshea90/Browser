# CHECKPOINT 1665 — read me first, then TASKS.md

**Written:** 2026-11-13T20:11:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `947eed9` (this checkpoint is the commit after it)

## Just done
AL5 emulator lab found a real bug and it is fixed in code: after two server switches the third server's disguised playlist was never looked at (the extension's probe budget of 12 was per PAGE, not per document); now 12 per frame document + 48 per page, refunded when the frame navigates (web test added, 131/131 web tests). Server 1 -> Server 2 switch already proven: the old 360p row vanished and only the new server's 240p was listed

## Do this next
rebuild APK (build_al3.log in the scratchpad), reinstall, rerun the lab: /watch2 Server 3 (ad first) must list the ad AND the 360p/240p stream, then Server 1 again (ad gone), then open /plain (icon grey = AL1). Then Diagnostics open + Copy check, mini window header X + resume, ticks via tools/claim.sh, version bump 131, ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e03327f ckpt 1661: AM1 CODE DONE: removed Tap lab, Decode lab (assets/labs 394 KB), Scroll test 
  bab3a84 ckpt 1650: owner message 27 written into TASKS as AM1 (remove finished diagnosis tools);
  a395248 ckpt 1648: AL2 coded (core/download/MiniTracker + 9 tests, ui/DownloadPopup banner + top
  f3ad3c6 ckpt 1634: AL3 coded: Format.barText/belowBar (core, PlannerTest green), new ui/Download
  e688a4c ckpt 1629: AL4 done (cancel deletes partial file on every path; Clear/restore leaks clos
  f43eec4 ckpt 1619: owner message 26 ('The download works now' = alpha31 saved the vidsrc movie) 
  cdab618 ckpt 1609: alpha31 DONE and recorded (BUILDLOG line, run 43482256190 green, APK 93,443,1
  064ab78 ckpt 1606: alpha31 shipped to GitHub: ship.sh gecko gates green + pushed, android.yml di
  3336bd5 ckpt 1605: gated kestrel-g-v0.3.0-alpha31 (gecko code 130) and pushed it: handoff self-t
  e4be816 ckpt 1596: AK4 steps 1-3 done in code + tests: (1) extension background.js captures the 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
