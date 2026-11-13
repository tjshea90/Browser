# CHECKPOINT 1761 — read me first, then TASKS.md

**Written:** 2026-11-13T22:35:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `e611605` (this checkpoint is the commit after it)

## Just done
EY-1 last look ticked (emulator-proven: first-frame picture, scroll place restored and reported, revive path); AO3 icon + Recents task description + rename to Kestrel done (emulator Recents shows the new icon); JVM 977 tests, web 137, lint green; alpha33 (versionCode 132, extension 1.0.132) prepared, NOT shipped; AP1 (one press plays) recorded in TASKS, design: assist in guard-main.js

## Do this next
1) AP1: in geckoapp/src/main/assets/kestrel-shields/guard-main.js add the assist (after a refused pop-up/overlay/redirect within 1.5 s of a real press on a player: play the video of that frame, else press through at the same place, up to 3 times, cancelled by any play event; parent forwards to a player iframe via postMessage), web tests in web-tests/guard.test.js (overlay link, overlay div, ad script that eats the first press, 2 and 3 layers, normal page unchanged, cap 3), lab page tools/sitelab + emulator proof; 2) AO4 storage cleanup, AO2 size estimates, AO1 smoothness research; 3) ship alpha33 (bash ship.sh gecko note; dispatch android.yml; record-release) and answer the owner (list open items); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stands down on wakes

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  edbe131 ckpt 1750: owner message 29 recorded (AO1 smoothness audit, AO2 download size accuracy, 
  4220445 ckpt 1729: alpha32 SHIPPED and recorded (run 43514151508 green, APK 92,539,679 B vs alph
  8999097 ckpt 1719: gated kestrel-g-v0.3.0-alpha32 (gecko code 131) and pushed it: handoff self-t
  e9da0f9 ckpt 1718: EY-2 (start timeline, emulator marks + Diagnostics line, research/COLD_START.
  caaf9c8 ckpt 1700: AL1-AL6 + AM1 ticked with evidence (AL5 final emulator chain: S1->S2 dropped=
  42dbcff ckpt 1683: AL5 root cause found with a debug logcat mirror of the Health events (new: He
  37e9055 ckpt 1678: AL5: found why a script-named address in a player frame outlived a server swi
  a3bd814 ckpt 1673: report saved as research/REVIEW-EXTERNAL-2.md and its TASKS block (EY-1..EY-3
  d19f54f ckpt 1672: owner message 28 written into TASKS as AN1 (review and complete all fixes and
  d14b0ab ckpt 1665: AL5 emulator lab found a real bug and it is fixed in code: after two server s
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
