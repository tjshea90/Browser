# CHECKPOINT 1718 — read me first, then TASKS.md

**Written:** 2026-11-13T21:45:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7df9711` (this checkpoint is the commit after it)

## Just done
EY-2 (start timeline, emulator marks + Diagnostics line, research/COLD_START.md), EY-17 (list by rank, touch, probe refill), EY-7 (L0 retry of refused parts, one worker while retrying), EY-8 (recovery data carried and stored, header ladder rung L1) done with tests; StreamEngineTest 43 tests green

## Do this next
1) full suite ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug + cd web-tests && npm test, then EY-22 step 1 (Rec.canContinue + G18 reopen length check; read research/REVIEW-EXTERNAL-2.md section 6.2) and EY-1 (LastLook snapshot; section 3.3) - or ship alpha32 first: bash ship.sh gecko note, dispatch android.yml app=gecko full_build=true ref=branch, wait for tag+green, get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha32; 2) answer the owner in plain words (AL1-AL6, AM1, AN1 progress, what was only emulator-tested); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stands down on wakes

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  caaf9c8 ckpt 1700: AL1-AL6 + AM1 ticked with evidence (AL5 final emulator chain: S1->S2 dropped=
  42dbcff ckpt 1683: AL5 root cause found with a debug logcat mirror of the Health events (new: He
  37e9055 ckpt 1678: AL5: found why a script-named address in a player frame outlived a server swi
  a3bd814 ckpt 1673: report saved as research/REVIEW-EXTERNAL-2.md and its TASKS block (EY-1..EY-3
  d19f54f ckpt 1672: owner message 28 written into TASKS as AN1 (review and complete all fixes and
  d14b0ab ckpt 1665: AL5 emulator lab found a real bug and it is fixed in code: after two server s
  e03327f ckpt 1661: AM1 CODE DONE: removed Tap lab, Decode lab (assets/labs 394 KB), Scroll test 
  bab3a84 ckpt 1650: owner message 27 written into TASKS as AM1 (remove finished diagnosis tools);
  a395248 ckpt 1648: AL2 coded (core/download/MiniTracker + 9 tests, ui/DownloadPopup banner + top
  f3ad3c6 ckpt 1634: AL3 coded: Format.barText/belowBar (core, PlannerTest green), new ui/Download
```

(17 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
