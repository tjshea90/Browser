# CHECKPOINT 2259 — read me first, then TASKS.md

**Written:** 2026-11-14T17:50:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `4c766a1` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha39 (gecko code 140) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha39 (includes alpha38): The keyboard now learns the SHAPE of your glides, so your sloppy patterns stop coming out as the wrong word. Example: you glide for and the finger turns on the i, and the keyboard types fir (I reproduced exactly that on the emulator). Now when you tap for in the strip, or delete the word and type the right one, the keyboard remembers what that glide looked like and gives for the next time you draw it, even sloppily: on a simulated hand it is right about 90 percent of the time after one correction and every time from the fifth. It also keeps the words you write most, and it now always considers the words you write often even when a sloppy glide drifts far from their letters. Shapes it learned wrongly fade when you correct them. Nothing is learned in private tabs or password fields. New in Settings: Export what the keyboard learned saves everything it learned (words, word pairs, corrections, glide shapes, thumb habit) as one plain text file in Downloads, and Import what the keyboard learned brings such a file back; the file format is written down in docs/KEYBOARD-EXPORT.md in the repository so a future system keyboard can read it. Export before you uninstall: what the keyboard learned is deleted with the app. Tried on the emulator with real multi-point touches (glide, pick, export, forget, import, restart the app); not tried on your tablet, and how closely a real thumb repeats its own shapes only your Menu > Diagnostics line (glide shapes remembered, how often one helped) can say."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha39), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha39 "alpha39 (includes alpha38): The keyboard now learns the SHAPE of your glides, so your sloppy patterns stop coming out as the wrong word. Example: you glide for and the finger turns on the i, and the keyboard types fir (I reproduced exactly that on the emulator). Now when you tap for in the strip, or delete the word and type the right one, the keyboard remembers what that glide looked like and gives for the next time you draw it, even sloppily: on a simulated hand it is right about 90 percent of the time after one correction and every time from the fifth. It also keeps the words you write most, and it now always considers the words you write often even when a sloppy glide drifts far from their letters. Shapes it learned wrongly fade when you correct them. Nothing is learned in private tabs or password fields. New in Settings: Export what the keyboard learned saves everything it learned (words, word pairs, corrections, glide shapes, thumb habit) as one plain text file in Downloads, and Import what the keyboard learned brings such a file back; the file format is written down in docs/KEYBOARD-EXPORT.md in the repository so a future system keyboard can read it. Export before you uninstall: what the keyboard learned is deleted with the app. Tried on the emulator with real multi-point touches (glide, pick, export, forget, import, restart the app); not tried on your tablet, and how closely a real thumb repeats its own shapes only your Menu > Diagnostics line (glide shapes remembered, how often one helped) can say."
NEVER ship kestrel-g-v0.3.0-alpha39 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  4c766a1 ckpt 2258: pre-ship: alpha39 (includes alpha38): The keyboard now learns the SHAPE of yo
  899e135 ckpt 2244: AX1: remembered-word injection added (the lab found that for words seen 6+ ti
  54435a1 ckpt 2232: AX1 core built and tested: Fingerprint + PatternMemory (core/input/glide/Patt
  2383c78 ckpt 2206: alpha38 RELEASED AND RECORDED (run 43616372537 green, tag kestrel-g-v0.3.0-al
  2d5a35b ckpt 2201: alpha38 DISPATCHED (AX2 fix: passkey API off + crash note): android.yml run_w
  da5124b ckpt 2200: gated kestrel-g-v0.3.0-alpha38 (gecko code 139) and pushed it: handoff self-t
  b72f9b2 ckpt 2199: pre-ship: alpha38 (includes alpha37): Fixes the app closing on some bank page
  3495b5b ckpt 2198: AX2 root cause found, reproduced and fixed: GeckoView's Android WebAuthn need
  ac84c28 ckpt 2190: owner message 38 written into TASKS (AX1 keyboard learns sloppy glide pattern
  3d38f35 ckpt 2185: alpha37 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha37 (gecko code 138), and
```
