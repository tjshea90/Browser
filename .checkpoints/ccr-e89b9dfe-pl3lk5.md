# CHECKPOINT 2627 — read me first, then TASKS.md

**Written:** 2026-11-17T12:06:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `346038ba` (this checkpoint is the commit after it)

## Just done
alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 44076463792 green: check, test-js, test-jvm, build-gecko incl. pref-name check + verify-apk + publish; BUILDLOG.md line pushed). AOT RUN READ (44074456934, release build): ProfileInstaller wrote the 5.7 KB profile, speed-profile compiled 3.75 MB (verify 0.1 MB), warm start 1898 -> 1671 ms (-12 %, spread +-10 %), cold-cache unchanged; COMPILE AT INSTALL WORKS on Android 10 (PackageInstaller session with base.apk + baselineProfiles/1/*.dm = reason install-dm, 3.75 MB compiled, install +0.3 s; the 0 file is ignored) -> NOT built into the updater (risk to the only update channel), TASKS LAB6 needs the owner's go; the debug 3-mode A/B is not informative (debuggable = quicken only), now behind --debug-ab. TASKS LAB3/LAB4/LAB5 rows ticked with evidence; HANDOFF.md has the alpha64 entry; research/LAB5_*.md complete. A daily lab on the released commit is queued/running (for research/lab-baseline.json).

## Do this next
1) read the daily run (the newest lab_suite=daily dispatched after the release) -> write research/lab-baseline.json from it: python3 -I tools/emu/buildreport.py --out <downloaded emulator-lab-text dir> --repo-root . --suite daily --profile tablet --net full --run-id <id> --sha <sha> --write-baseline research/lab-baseline.json (then commit); 2) delete send_later trigger trig_01B3Wx9aKtTueNWZ2BwMts6R; 3) the final plain-words answer to the owner (Release link on its own line, numbers, what is NOT proven on the tablet, what to try, Neon choice, LAB6 idea, open items)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
  1c468757 ckpt 2623: SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 g
  bd981349 ckpt 2622: LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in t
  c83d9f96 ckpt 2621: LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app
  41f2a308 ckpt 2620: LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFS
  a55d8865 ckpt 2619: LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.pre
  da746ada ckpt 2618: LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/pr
  96e097e3 ckpt 2617: LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestre
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
