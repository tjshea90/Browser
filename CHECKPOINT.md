# CHECKPOINT 2901 — read me first, then TASKS.md

**Written:** 2026-11-22T04:06:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `c5fa9011` (this checkpoint is the commit after it)

## Just done
alpha91 gate: web 676/676 green; Gradle g10 failed ONLY in core EngineTest.anOutageThatHitsEveryConnectionOfAParallelDownloadWaitsInsteadOfFailing (a download-engine timing test, unrelated, ran while web tests loaded the CPU); rerunning the whole gate alone (g11.log, --continue). New unshipped files: core SiblingQualities + test (PERF5b, not wired yet)

## Do this next
Next: g11 GRADLE_EXIT=0 -> bump 192 / 0.3.0-alpha91, sync-ext-version, ship.sh gecko with docs/handoff/ALPHA91_RELEASE_NOTE.txt, ONE dispatch, send_later +24 min, record-release. Then PERF5b: wire SiblingQualities (VideoFlow.start builds alts from tab.media + masterRungs; NativePlayerActivity EXTRA_ALTS; PlayerQuality.alts rows + onAlt; PlayerFactory.makeSource to swap the source at the position)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  fdc46394 ckpt 2898: Owner after turning the press-to-play rule ON: pornhub still only starts at 
  c85eda2c ckpt 2897: alpha91 built, gates running (g10.log, w4.log): CosmeticFixes now drops porn
  237a0bc2 ckpt 2892: alpha90 upload read (PERF4f in TASKS): typing keys reach the page fast (13 m
  4e9d13f6 ckpt 2891: Owner clarified: 'typing here' and 'photos to this Claude chat' = claude.ai 
  ed86677d ckpt 2890: Owner after alpha90 (PERF4 in TASKS.md): photos still cannot be uploaded to 
  b02ae3d2 ckpt 2889: alpha90 RELEASED and recorded (run 44970827834 green, Release has the APK). 
  16cc4189 ckpt 2887: alpha90 (code 191) gated and shipped: ONE dispatch made right after ship.sh 
  eb28b328 ckpt 2886: gated kestrel-g-v0.3.0-alpha90 (gecko code 191) and pushed it: handoff self-
  9b7f6817 ckpt 2885: pre-ship: alpha90: finding out where the typing delay and the stuck video re
  a4f81831 ckpt 2883: PERF3 findings from the owner's alpha88/89 uploads: Battery Saver is OFF now
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
