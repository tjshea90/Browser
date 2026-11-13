# CHECKPOINT 1844 — read me first, then TASKS.md

**Written:** 2026-11-13T23:57:12Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `bc3be0b` (this checkpoint is the commit after it)

## Just done
AQ1: found on the emulator that restore's show() cleared the Home flag (the placeholder tab was 'previous'); fixed with current = null before show(sel). AR1 BlockedChip (ui/BlockedChip.kt, ic_page_blocked, wired in onPopupBlocked + TabDelegates.onPopupPrompt, NoticeBar removed) and AR2 MiniNav (ui/MiniNav.kt: toggle, black panel, Back/Home/Forward, address bar, in-app mini keyboard over core input/MiniKeyboard.kt, own-clock slide, 130 ms neon flash, Settings 'Mini navigation tab', prefs miniNavOpen/miniNavToggle, Flavor.hasMiniNav) written; tests green: MiniKeyboardTest (9, core), BlockedChipTest (5), MiniNavTest (13, app Robolectric). Emulator APK built/installing

## Do this next
1) run scratchpad aq1_flow.sh (Home restore: expect Home after reopen, tabs state NOT restored behind it, Back brings the page) and ar_flow.sh (screenshots ar_*.png: panel slide, bar+keyboard, Go, chip, chip tap opens new tab; logcat 'guard chip-show/chip-open'); look at the screenshots and fix layout (coordinates in ar_flow.sh are computed for 600x973 @170dpi: toggle (570,44), bar (500,133), q (291,175), Go (560,304)). 2) claim+tick AQ1, AR1, AR2 with evidence (tools/claim.sh <ID> --done). 3) tick AO1. 4) full suite: ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug (two core tests flake under load only) + cd web-tests && npm test. 5) ship alpha33 (versionCode 134 already set; now also AQ1/AR1/AR2): bash ship.sh gecko note, dispatch android.yml app=gecko full_build=true ref=branch, wait, record-release.sh, answer owner. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN: stand down on wakes.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ebe2fa5 ckpt 1829: Owner message 32 written into TASKS (AR1 blocked-page icon bottom middle 1 s,
  39cf669 ckpt 1819: Owner message 31 written into TASKS (AQ1 Home restored as last page, AQ2 cont
  fa60b0e ckpt 1812: AO1 code + research written: core/diag/CpuUse.kt (ProcStat/CpuSeries, CpuUseT
  33ceee7 ckpt 1804: AO2 DONE and ticked: SizeEstimate + VideoFlow calibration + engine running es
  da2a562 ckpt 1790: AO4 DONE and ticked: emulator proof with planted files (engine start pruned t
  6b3800f ckpt 1788: AO4 code written and green: core ApkFiles (+5 tests), app diag/StorageTidy (+
  c2b65d1 ckpt 1761: EY-1 last look ticked (emulator-proven: first-frame picture, scroll place res
  edbe131 ckpt 1750: owner message 29 recorded (AO1 smoothness audit, AO2 download size accuracy, 
  4220445 ckpt 1729: alpha32 SHIPPED and recorded (run 43514151508 green, APK 92,539,679 B vs alph
  8999097 ckpt 1719: gated kestrel-g-v0.3.0-alpha32 (gecko code 131) and pushed it: handoff self-t
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
