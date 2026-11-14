# CHECKPOINT 1860 — read me first, then TASKS.md

**Written:** 2026-11-14T00:28:02Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `6e1542a` (this checkpoint is the commit after it)

## Just done
AQ1, AR1 and AR2 ticked in TASKS with evidence: Home restore proven on the emulator (after the restore-clears-flag fix), blocked-page icon proven (chip-show / chip-open, tab count 3 to 4, screenshot of the icon), mini panel proven (toggle, panel, bar selects all, keyboard, Go opened a typed address); debug-only chip life knob + same-address tab attribution added; research/MINI_NAV.md written. Diagnostics on-screen check (CPU line) still running on the emulator

## Do this next
1) read scratchpad diag_flow.out + diag_cpu.png (CPU by process / Storage tidy / Video size estimates lines on the Diagnostics screen); tick AO1 with claim.sh AO1 --done (evidence: busy events tab 84 % [Web Content 36], gpu 198 % WRSceneBuilder, research/VIDEO_SMOOTHNESS.md, CpuUseTest). 2) with the emulator idle run the full suite: ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug (ProxyLinkTest/ServerTest only flake under load) and cd web-tests && npm test. 3) ship alpha33 (versionCode 134 already set): bash ship.sh gecko "note", dispatch android.yml app=gecko full_build=true ref=<branch>, wait for tag + green run, get_release_by_tag, bash tools/record-release.sh, give the owner the Release link on its own line. 4) answer the owner in plain words (Home restore, blocked-page icon, mini panel, storage tidy, size estimates, CPU line/smoothness findings, icon + Recents + rename, first-press play), what was only emulator-tested, and re-list the open items. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN: stand down on wakes.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2874e84 ckpt 1844: AQ1: found on the emulator that restore's show() cleared the Home flag (the p
  ebe2fa5 ckpt 1829: Owner message 32 written into TASKS (AR1 blocked-page icon bottom middle 1 s,
  39cf669 ckpt 1819: Owner message 31 written into TASKS (AQ1 Home restored as last page, AQ2 cont
  fa60b0e ckpt 1812: AO1 code + research written: core/diag/CpuUse.kt (ProcStat/CpuSeries, CpuUseT
  33ceee7 ckpt 1804: AO2 DONE and ticked: SizeEstimate + VideoFlow calibration + engine running es
  da2a562 ckpt 1790: AO4 DONE and ticked: emulator proof with planted files (engine start pruned t
  6b3800f ckpt 1788: AO4 code written and green: core ApkFiles (+5 tests), app diag/StorageTidy (+
  c2b65d1 ckpt 1761: EY-1 last look ticked (emulator-proven: first-frame picture, scroll place res
  edbe131 ckpt 1750: owner message 29 recorded (AO1 smoothness audit, AO2 download size accuracy, 
  4220445 ckpt 1729: alpha32 SHIPPED and recorded (run 43514151508 green, APK 92,539,679 B vs alph
```

(15 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
