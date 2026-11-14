# CHECKPOINT 2232 — read me first, then TASKS.md

**Written:** 2026-11-14T16:58:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `6542b41` (this checkpoint is the commit after it)

## Just done
AX1 core built and tested: Fingerprint + PatternMemory (core/input/glide/PatternMemory.kt: 16-point glide shape in key units, per-word shapes merged within 0.3, evidence = n*exp(-0.5(d/0.35)^2), contradict on correction, caps, decay), owned by PersonalModel (new line kind s; parse now bounded + finite-only; Locale.ROOT numbers; mergeFrom = max, idempotent), GlideDecoder.applyPatterns (patGain 2.5, cap 4.5, maxGap 2.2) before applyCorrections, GlideTyping learnShape/glideDeleted/retyped/exportText/importText, LearnedExport (versioned plain text, docs/KEYBOARD-EXPORT.md with an example a test reads), MiniNav hooks (confirm, Backspace + retype at space/Go within 30 s), MiniNav.exportLearned/importLearned, KeyboardFlow (Downloads file + picker), Settings buttons Export/Import, BrowserActivity wiring. Lab (GlideHabitEval, 40 hands, o lands 1.0-1.2 key toward i for 'for'): old keyboard stays ~80% right (flat for 30 glides, wrong words fur/fir/fit/fire/due), with shapes 90% after 1 correction, ~100% from the 5th glide; long mixed use: +1..3 points, mended 66 vs spoiled 7 at 10 dp habit. New tests: PatternMemoryTest, GlideShapeLearningTest, LearnedExportTest, 6 new GlideSupportTest cases - all green; compile of :app and :geckoapp OK

## Do this next
1) read core test log /tmp/claude-0/-home-user-Browser/30a5aa00-41c9-5bd3-87d1-b10052a52d84/scratchpad/test2.log (full :core:test) 2) app tests: add MiniNavTest cases (pick keeps shape, Backspace+retype at space, private tab learns nothing, export/import through MiniNav) and KeyboardFlowTest (Robolectric, model on BackupFlowTest) then run :app:testDebugUnitTest 3) AX1c write-up + research/GLIDE_TYPING.md section 11 with the lab numbers 4) Diagnostics line check 5) emulator proof: x86_64 debug APK ./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64, real multi-point glide, export + import round trip on the emulator 6) bump version, bash ship.sh gecko note (alpha39), dispatch, record

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2383c78 ckpt 2206: alpha38 RELEASED AND RECORDED (run 43616372537 green, tag kestrel-g-v0.3.0-al
  2d5a35b ckpt 2201: alpha38 DISPATCHED (AX2 fix: passkey API off + crash note): android.yml run_w
  da5124b ckpt 2200: gated kestrel-g-v0.3.0-alpha38 (gecko code 139) and pushed it: handoff self-t
  b72f9b2 ckpt 2199: pre-ship: alpha38 (includes alpha37): Fixes the app closing on some bank page
  3495b5b ckpt 2198: AX2 root cause found, reproduced and fixed: GeckoView's Android WebAuthn need
  ac84c28 ckpt 2190: owner message 38 written into TASKS (AX1 keyboard learns sloppy glide pattern
  3d38f35 ckpt 2185: alpha37 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha37 (gecko code 138), and
  aef75f4 ckpt 2180: alpha37 DISPATCHED: android.yml run 43613953896 (run_number 88, app=gecko ful
  551bd7e ckpt 2179: gated kestrel-g-v0.3.0-alpha37 (gecko code 138) and pushed it: handoff self-t
  ec2ea4b ckpt 2178: pre-ship: alpha37 (includes alpha36): Better ad blocking. 1) A site now gets 
```

(25 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
