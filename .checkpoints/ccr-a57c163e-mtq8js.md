# CHECKPOINT 2244 — read me first, then TASKS.md

**Written:** 2026-11-14T17:13:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3cb1a74` (this checkpoint is the commit after it)

## Just done
AX1: remembered-word injection added (the lab found that for words seen 6+ times 78-93% of the remaining misses were words the quick pass never kept among its 200: GlideDecoder.injectRemembered puts up to 48 words that have a shape looking like the glide into the careful pass; decode 3.4 ms vs 3.3 ms with 577 words remembered). Final lab numbers (GlideHabitEval, simulated hands): for/1.0 key hairpin, normal hand: old keyboard ~85% flat for 30 glides, with shapes 80 -> 90 -> 97 -> 95 -> 100 from the 5th glide on; long mixed use 3000 glides: no habit 91-95 -> 94-98; habit 10 dp 79-87 -> 85-94; habit 16 dp 50-63 -> 60-81; spoiled first guesses 1-7 vs mended 124-521. Tests: core full suite + MiniNavTest(72) + KeyboardFlowTest(9) + app lint green before injection; GlideShapeLearningTest(7) PatternMemoryTest GlideSupportTest green after. New tools/emu/glide_emu.py (real multi-point touches on the emulator). x86_64 debug APK rebuilding for the emulator proof

## Do this next
1) wait for apk2.log EXIT 0, adb install -r geckoapp/build/outputs/apk/debug/geckoapp-debug.apk, run tools/sitelab/fieldlab.py 8001 /tmp/fieldlab.log (emulator reaches it as http://10.0.2.2:8001/), adb reverse not needed; open the page, focus field 'free', long press the top right corner (adb: python3 -I tools/emu/glide_emu.py longpress 580 80), tap the bar, glide for with o=-1.0 (glide_emu.py glide for o=-1.0), read the strip with a screenshot, pick for, carry on, glide again and read the server log; then Settings > Export (file in Downloads) and Forget + Import (SAF picker via glide_emu.py taptext) 2) run full :core:test :app:testDebugUnitTest :app:lintDebug again 3) research/GLIDE_TYPING.md section 11 + HANDOFF.md + TASKS ticks (AX1a-e) 4) bump versionCode/versionName (alpha39), bash ship.sh gecko note, dispatch android.yml, record-release after green, answer the owner in plain words (what it learns, the for/fir lab numbers, export file location and format, system keyboard = separate app, NOT run on the tablet)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  54435a1 ckpt 2232: AX1 core built and tested: Fingerprint + PatternMemory (core/input/glide/Patt
  2383c78 ckpt 2206: alpha38 RELEASED AND RECORDED (run 43616372537 green, tag kestrel-g-v0.3.0-al
  2d5a35b ckpt 2201: alpha38 DISPATCHED (AX2 fix: passkey API off + crash note): android.yml run_w
  da5124b ckpt 2200: gated kestrel-g-v0.3.0-alpha38 (gecko code 139) and pushed it: handoff self-t
  b72f9b2 ckpt 2199: pre-ship: alpha38 (includes alpha37): Fixes the app closing on some bank page
  3495b5b ckpt 2198: AX2 root cause found, reproduced and fixed: GeckoView's Android WebAuthn need
  ac84c28 ckpt 2190: owner message 38 written into TASKS (AX1 keyboard learns sloppy glide pattern
  3d38f35 ckpt 2185: alpha37 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha37 (gecko code 138), and
  aef75f4 ckpt 2180: alpha37 DISPATCHED: android.yml run 43613953896 (run_number 88, app=gecko ful
  551bd7e ckpt 2179: gated kestrel-g-v0.3.0-alpha37 (gecko code 138) and pushed it: handoff self-t
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
