# CHECKPOINT 1819 — read me first, then TASKS.md

**Written:** 2026-11-13T23:38:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `36be1e8` (this checkpoint is the commit after it)

## Just done
Owner message 31 written into TASKS (AQ1 Home restored as last page, AQ2 continue after the owner's stop); suite still running in the background (two core tests ProxyLinkTest.theRenewalKeepsItOpenPastTheFirstArming and ServerTest.manyClientsAtOnceGetTheirOwnAnswers failed under load, to re-run alone), cpu_flow2 emulator run pending

## Do this next
AQ1: read geckoapp BrowserActivity saveSessionNow/restore + Tab.showHome + core session SessionCodec/TabRecord (the saved session has no 'on Home' flag, so a Home tab restores its last website); add the flag to the codec (old files load as not-Home), restore as Home without loading, skip the last look for Home tabs, tests, emulator proof; then AQ2: re-run the two failing core tests alone, tick AO1, full suite + lint, ship alpha33 (versionCode 134), answer the owner.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  fa60b0e ckpt 1812: AO1 code + research written: core/diag/CpuUse.kt (ProcStat/CpuSeries, CpuUseT
  33ceee7 ckpt 1804: AO2 DONE and ticked: SizeEstimate + VideoFlow calibration + engine running es
  da2a562 ckpt 1790: AO4 DONE and ticked: emulator proof with planted files (engine start pruned t
  6b3800f ckpt 1788: AO4 code written and green: core ApkFiles (+5 tests), app diag/StorageTidy (+
  c2b65d1 ckpt 1761: EY-1 last look ticked (emulator-proven: first-frame picture, scroll place res
  edbe131 ckpt 1750: owner message 29 recorded (AO1 smoothness audit, AO2 download size accuracy, 
  4220445 ckpt 1729: alpha32 SHIPPED and recorded (run 43514151508 green, APK 92,539,679 B vs alph
  8999097 ckpt 1719: gated kestrel-g-v0.3.0-alpha32 (gecko code 131) and pushed it: handoff self-t
  e9da0f9 ckpt 1718: EY-2 (start timeline, emulator marks + Diagnostics line, research/COLD_START.
  caaf9c8 ckpt 1700: AL1-AL6 + AM1 ticked with evidence (AL5 final emulator chain: S1->S2 dropped=
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
