# CHECKPOINT 1263 — read me first, then TASKS.md

**Written:** 2026-11-12T22:49:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9af58a2` (this checkpoint is the commit after it)

## Just done
alpha24 SHIPPED and recorded (tag kestrel-g-v0.3.0-alpha24, run 43321730068 green, APK 92.9 MB on the Release, BUILDLOG line pushed). In progress for alpha25: (1) kestrel-image worker thread (BridgeLane, done+tested); (2) AA3-3b $removeparam: core QueryRules.kt + parser + builder hook + FilterSet.stripQuery + KFS format 3 (reads 2 and 3) done, QueryRulesTest 21/21 green; NOT yet wired into NavPolicy, FilterSources, make-default-lists.sh

## Do this next
AA3-3b $removeparam finish: 1) app NavPolicy.prepare: after TrackingParams.strip call Engine.peek()?.stripQuery(u) (only when s.shieldsUp && s.blockAds) and Engine warm-up: fs.warmQuery() next to warmCosmetic() (Engine.kt lines ~41,68,89); 2) FilterSources.DEFAULT: add ListSource("ubo-removeparam", UBO + "privacy-removeparam.txt", Category.PRIVACY); scripts/make-default-lists.sh: add abp:privacy:$U/filters/privacy-removeparam.txt; 3) core RealQueryRulesTest against /home/user/ublockorigin/uassets (skipped when absent): parse coverage, known strips; 4) regenerate default.kfs with scripts/make-default-lists.sh (needs DefaultListsRegressionTest green), docs/FILTER-LISTS.md + research/ADBLOCK_ENGINE.md note; 5) app test for NavPolicy; then bump geckoapp to code 124 / 0.3.0-alpha25 (+ extension 1.0.124 via tools/sync-ext-version.sh) and ship with the kestrel-image thread. Then AA3-3b rest: redirect= neutering, procedural :has-text/:upward, false-positive corpus. Owner reply for alpha24 is being written now. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a6028f3 ckpt 1257: AA3-3b step 1 (after dispatching alpha24 run 43321730068): engine-image chunk
  efdc3a2 ckpt 1248: alpha24 gated and pushed (ckpt 1247), workflow DISPATCHED: run 43321730068 (#
  3f19341 ckpt 1247: gated kestrel-g-v0.3.0-alpha24 (gecko code 123) and pushed it: handoff self-t
  6b43626 ckpt 1246: alpha24 content finished and tested: keyboard-reveal fix (content.js), D2 laz
  2e3e341 ckpt 1238: AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untru
  b630671 ckpt 1227: AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was
  15b71c1 ckpt 1196: AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on G
  81f5a25 ckpt 1187: SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-
  05062dd ckpt 1185: alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarn
  65bbb1c ckpt 1183: gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-t
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
