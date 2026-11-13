# CHECKPOINT 1750 — read me first, then TASKS.md

**Written:** 2026-11-13T22:17:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `c4534d8` (this checkpoint is the commit after it)

## Just done
owner message 29 recorded (AO1 smoothness audit, AO2 download size accuracy, AO3 professional icon + Recents icon, AO4 delete update APKs and stale storage) in TASKS before code; EY-1 last look: capture + early-cover look + hide rule + Settings switch + Diagnostics line written, page-side scroll report added (content.js/background.js/ShieldsBridge, web tests 40 content + 87 background pass), 17 JVM tests for the look (rules, store, overlay pixels), emulator: look shown in first frame, scroll captured; alpha33 versionCode 132 / extension 1.0.132 prepared (NOT shipped)

## Do this next
1) read scratchpad look_flow.out + lk_events_a.txt (chain_look2.sh: build+install+flow with the page-side scroll report): expect look-hide why=look-scroll/look-paint with y=cur/saved close, and look-saved y>0 after the restore; if hide is only by timeout on this slow emulator that is the emulator (page paint takes 70 s), say so; 2) full suite (./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug + cd web-tests && npm test) then tick EY-1 with claim.sh; 3) AO3 icon (geckoapp + app manifests, mipmap-anydpi-v26 adaptive icon, setTaskDescription in BrowserActivity), AO4 storage cleanup (update APKs after the new version runs, stale files), AO2 size estimates, AO1 smoothness research (research/VIDEO_SMOOTHNESS.md); 4) ship alpha33 and answer the owner (plain words, list open items); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stands down on wakes

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4220445 ckpt 1729: alpha32 SHIPPED and recorded (run 43514151508 green, APK 92,539,679 B vs alph
  8999097 ckpt 1719: gated kestrel-g-v0.3.0-alpha32 (gecko code 131) and pushed it: handoff self-t
  e9da0f9 ckpt 1718: EY-2 (start timeline, emulator marks + Diagnostics line, research/COLD_START.
  caaf9c8 ckpt 1700: AL1-AL6 + AM1 ticked with evidence (AL5 final emulator chain: S1->S2 dropped=
  42dbcff ckpt 1683: AL5 root cause found with a debug logcat mirror of the Health events (new: He
  37e9055 ckpt 1678: AL5: found why a script-named address in a player frame outlived a server swi
  a3bd814 ckpt 1673: report saved as research/REVIEW-EXTERNAL-2.md and its TASKS block (EY-1..EY-3
  d19f54f ckpt 1672: owner message 28 written into TASKS as AN1 (review and complete all fixes and
  d14b0ab ckpt 1665: AL5 emulator lab found a real bug and it is fixed in code: after two server s
  e03327f ckpt 1661: AM1 CODE DONE: removed Tap lab, Decode lab (assets/labs 394 KB), Scroll test 
```

(20 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
