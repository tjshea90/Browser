# CHECKPOINT 678 — read me first, then TASKS.md

**Written:** 2026-11-12T01:35:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `68297ea` (this checkpoint is the commit after it)

## Just done
emulator-verified: Save-video chip opens the Download video sheet; page scan lists mp4/m4a found in og:video/anchor/inline script without playing; pop-up guard (window.open ad, hidden link, redirect, timer blocked; same-site + real link open). Coded: size of page-found files via Range probe; StreamRec.breaks + StreamEngine.partsOf + StreamFinisher.Part; Remuxer.convertParts (runs laid end to end, other-format runs dropped, unsupported tracks ignored, Health.log remux) - core tests green, NOT yet run on emulator

## Do this next
1) build x86_64 debug, adb install -r, test http://10.0.2.2:8000/disc.html (index.m3u8 has A+ad+C, two.m3u8 A+C): download via Save video, check result is .mp4 and ffprobe it (adb pull from /sdcard/Download): duration ~16-20 s, monotonic. 2) fix Remuxer issues found. 3) bump alpha15 (code 114, 0.3.0-alpha15), ship.sh gecko, dispatch android.yml, verify, record-release, tell owner. 4) X2 daily update dialog, X5 assessment, X3 download manager (research/DOWNLOAD_MANAGERS.md), X12 audit (research/GECKO_TUNING_2.md now written)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e1721e0 ckpt 647: X7 page scan coded: content.js finds video/source/og:video/links/inline-script
  2c7b49a ckpt 628: owner request 10 written into TASKS.md as X11 (everything still on the list) a
  6b28616 ckpt 625: alpha14 SHIPPED and recorded (run 43127174913 green): https://github.com/tjshe
  d1af18c ckpt 615: alpha14 RE-DISPATCHED: run 43127174913 (android.yml, head 0cebcca, 00:25:31Z, 
  0cebcca ckpt 614: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  fec7b3d ckpt 609: alpha14 DISPATCHED: run 43126355049 (android.yml, head 3ed1b75, started 00:18:
  3ed1b75 ckpt 608: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  b646b31 ckpt 606: X1 DONE in code and tests: updater root cause found (GitHub lists releases by 
  6400e12 ckpt 600: owner request 9 written into TASKS.md as X1-X10 in the owner's words (INBOX ha
  f1f1d48 ckpt 597: alpha13 SHIPPED and recorded (run 43123971432 green, BUILDLOG written): https:
```

(30 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
