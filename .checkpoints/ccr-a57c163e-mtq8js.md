# CHECKPOINT 1661 — read me first, then TASKS.md

**Written:** 2026-11-13T20:00:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `64c9cab` (this checkpoint is the commit after it)

## Just done
AM1 CODE DONE: removed Tap lab, Decode lab (assets/labs 394 KB), Scroll test + Compare 3 feels (ScrollTest.kt, assets/scrolltest 556 KB, runner), blocker fast/compat switch (pref engineInExtension), Test the pulse (Neon.test), Keyboard/Warm-ups/GPU/decoder lines + GlInfo.kt + EX-2 debug proof hook; research/DIAG_TOOLS.md lists what went and what stays (restore point bab3a84); full suite green (936 tests, lint 0 errors). AL4 PROVEN on the emulator (cancel running + cancel paused both delete the partial file from /sdcard/Download). AL2/AL3/AL6 seen on the emulator (banner, mini window, pause icon, X confirm, 30 dp bars with the text flip, Ozark S02E05 names)

## Do this next
1) extend tools/sitelab/vidlab_srv.py (pages /watch2 with Server 1/2/3 buttons, /plain, ad-first player, second HLS set /hls/def) and prove AL1/AL5 on the emulator, 2) rebuild the x86_64 debug APK (gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64) and check Diagnostics opens + Copy works + header X hides the mini window + resume button, 3) web-tests (cd web-tests && npm test), 4) tick AL1/AL2/AL3/AL5/AL6/AM1 with evidence via tools/claim.sh, bump versionCode 131 / 0.3.0-alpha32, ship.sh gecko, dispatch android.yml, record, answer the owner (plain words, Release link on its own line, list open items)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bab3a84 ckpt 1650: owner message 27 written into TASKS as AM1 (remove finished diagnosis tools);
  a395248 ckpt 1648: AL2 coded (core/download/MiniTracker + 9 tests, ui/DownloadPopup banner + top
  f3ad3c6 ckpt 1634: AL3 coded: Format.barText/belowBar (core, PlannerTest green), new ui/Download
  e688a4c ckpt 1629: AL4 done (cancel deletes partial file on every path; Clear/restore leaks clos
  f43eec4 ckpt 1619: owner message 26 ('The download works now' = alpha31 saved the vidsrc movie) 
  cdab618 ckpt 1609: alpha31 DONE and recorded (BUILDLOG line, run 43482256190 green, APK 93,443,1
  064ab78 ckpt 1606: alpha31 shipped to GitHub: ship.sh gecko gates green + pushed, android.yml di
  3336bd5 ckpt 1605: gated kestrel-g-v0.3.0-alpha31 (gecko code 130) and pushed it: handoff self-t
  e4be816 ckpt 1596: AK4 steps 1-3 done in code + tests: (1) extension background.js captures the 
  a32efe5 ckpt 1582: AK1 answered in chat (appops REQUEST_INSTALL_PACKAGES/RUN_IN_BACKGROUND/RUN_A
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
