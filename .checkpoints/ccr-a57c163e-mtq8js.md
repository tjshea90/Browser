# CHECKPOINT 1875 — read me first, then TASKS.md

**Written:** 2026-11-14T00:38:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `8285c3c` (this checkpoint is the commit after it)

## Just done
AO1 ticked in TASKS (emulator Diagnostics screen shows the CPU by process, Storage tidy and Video size estimates lines; the earlier found=0 was my script cutting rows at 150 chars, not an app fault).

## Do this next
1) run the full suite in the background with the emulator idle (stop the emulator first to free CPU): ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug --console=plain > scratchpad/full_suite.out 2>&1 ; plus cd web-tests && npm test; fix anything red. 2) ship alpha33 (versionCode 134 set): bash ship.sh gecko note, dispatch android.yml app=gecko full_build=true ref=ccr-a57c163e-mtq8js, wait for tag + green run, get_release_by_tag, bash tools/record-release.sh. 3) answer the owner in plain words and re-list the open items. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN: stand down on wakes.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9689afe ckpt 1860: AQ1, AR1 and AR2 ticked in TASKS with evidence: Home restore proven on the em
  2874e84 ckpt 1844: AQ1: found on the emulator that restore's show() cleared the Home flag (the p
  ebe2fa5 ckpt 1829: Owner message 32 written into TASKS (AR1 blocked-page icon bottom middle 1 s,
  39cf669 ckpt 1819: Owner message 31 written into TASKS (AQ1 Home restored as last page, AQ2 cont
  fa60b0e ckpt 1812: AO1 code + research written: core/diag/CpuUse.kt (ProcStat/CpuSeries, CpuUseT
  33ceee7 ckpt 1804: AO2 DONE and ticked: SizeEstimate + VideoFlow calibration + engine running es
  da2a562 ckpt 1790: AO4 DONE and ticked: emulator proof with planted files (engine start pruned t
  6b3800f ckpt 1788: AO4 code written and green: core ApkFiles (+5 tests), app diag/StorageTidy (+
  c2b65d1 ckpt 1761: EY-1 last look ticked (emulator-proven: first-frame picture, scroll place res
  edbe131 ckpt 1750: owner message 29 recorded (AO1 smoothness audit, AO2 download size accuracy, 
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
