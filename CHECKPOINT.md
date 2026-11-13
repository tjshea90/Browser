# CHECKPOINT 1700 — read me first, then TASKS.md

**Written:** 2026-11-13T21:31:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `68eb951` (this checkpoint is the commit after it)

## Just done
AL1-AL6 + AM1 ticked with evidence (AL5 final emulator chain: S1->S2 dropped=2, S2->S3 dropped=3, S3->S1 dropped=3, all left=0); EY-17 coded + tested (MediaList rank/touch, background.js probe refill 4 per 20 s, web-tests 133 pass); EY-2 StartTimeline compiled; JVM suite 940 tests green, lint 0 errors

## Do this next
1) rebuild debug APK (./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64), install on the emulator, cold start and read adb logcat -s KestrelHealth for the 'start' marks (EY-2 proof; write research/COLD_START.md skeleton); 2) bump nothing (versionCode 131 / alpha32 already set), bash ship.sh gecko note, dispatch android.yml app=gecko full_build=true ref=branch, wait for tag + green run, get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha32; 3) answer the owner (AL1-AL6, AM1, AN1 verdict) in plain words with the Release link on its own line; 4) AN1 Wave 1 next rows: EY-7 (StreamEngine Denied + L0 retry, Config.denyDelaysMs, core tests with FakeCdn.statusFor/failFirst, update the existing test serverErrorsAreRetriedButMissingAndForbiddenFailAtOnce), EY-8, EY-22 step 1, EY-1; heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stands down on wakes

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  42dbcff ckpt 1683: AL5 root cause found with a debug logcat mirror of the Health events (new: He
  37e9055 ckpt 1678: AL5: found why a script-named address in a player frame outlived a server swi
  a3bd814 ckpt 1673: report saved as research/REVIEW-EXTERNAL-2.md and its TASKS block (EY-1..EY-3
  d19f54f ckpt 1672: owner message 28 written into TASKS as AN1 (review and complete all fixes and
  d14b0ab ckpt 1665: AL5 emulator lab found a real bug and it is fixed in code: after two server s
  e03327f ckpt 1661: AM1 CODE DONE: removed Tap lab, Decode lab (assets/labs 394 KB), Scroll test 
  bab3a84 ckpt 1650: owner message 27 written into TASKS as AM1 (remove finished diagnosis tools);
  a395248 ckpt 1648: AL2 coded (core/download/MiniTracker + 9 tests, ui/DownloadPopup banner + top
  f3ad3c6 ckpt 1634: AL3 coded: Format.barText/belowBar (core, PlannerTest green), new ui/Download
  e688a4c ckpt 1629: AL4 done (cancel deletes partial file on every path; Clear/restore leaks clos
```

(16 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
