# CHECKPOINT 2206 — read me first, then TASKS.md

**Written:** 2026-11-14T16:41:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7012bad` (this checkpoint is the commit after it)

## Just done
alpha38 RELEASED AND RECORDED (run 43616372537 green, tag kestrel-g-v0.3.0-alpha38, gecko code 139, APK in the Release); AX2 + AX2b ticked. AX1 research read: GlideDecoder / PersonalModel / GlideSupport / MiniNav (confirmGlide, pickCandidate, Backspace sets afterUndo+lastDeleted, learnFromText); the alpha36 correction memory is keyed by WORD PAIR (guessed -> meant), lifts at most 1.6 and only for words within 0.9 per-letter cost, never looks at the SHAPE of the glide = why a repeated sloppy for/fir pattern is not learned decisively. GestureSim.path got an optional per-letter shift (a hand habit) for the lab.

## Do this next
ANSWER THE OWNER for alpha38 (plain words: the crash was passkeys/Google services; reproduced + fixed on the emulator only; NOT tried on the tablet or real ally.com; next Diagnostics shows EARLIER CRASH if it recurs; AX1 keyboard learning is being built; re-list open items) - Release link: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha38 . THEN AX1a: write core/src/test/.../glide/GlideHabitEval.kt (a person whose hand always lands o toward i for the word for; per-word motor habits), run it with java -cp core/build/classes/kotlin/main:core/build/classes/kotlin/test:kotlin-stdlib-2.3.10.jar to MEASURE fir-vs-for and corrections needed today; then AX1b: core/.../glide/PatternMemory.kt (canonical 16-point fingerprint of the glide, per-word templates with counts, merge near-identical ones, weaken the wrong guess's templates on a correction) owned by PersonalModel (new line kind s), GlideDecoder.applyPatterns before applyCorrections, GlideTyping keeps lastPrint/deletedPrint, MiniNav hooks (pick, kept, Backspace then retyped word at space/Go)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2d5a35b ckpt 2201: alpha38 DISPATCHED (AX2 fix: passkey API off + crash note): android.yml run_w
  da5124b ckpt 2200: gated kestrel-g-v0.3.0-alpha38 (gecko code 139) and pushed it: handoff self-t
  b72f9b2 ckpt 2199: pre-ship: alpha38 (includes alpha37): Fixes the app closing on some bank page
  3495b5b ckpt 2198: AX2 root cause found, reproduced and fixed: GeckoView's Android WebAuthn need
  ac84c28 ckpt 2190: owner message 38 written into TASKS (AX1 keyboard learns sloppy glide pattern
  3d38f35 ckpt 2185: alpha37 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha37 (gecko code 138), and
  aef75f4 ckpt 2180: alpha37 DISPATCHED: android.yml run 43613953896 (run_number 88, app=gecko ful
  551bd7e ckpt 2179: gated kestrel-g-v0.3.0-alpha37 (gecko code 138) and pushed it: handoff self-t
  ec2ea4b ckpt 2178: pre-ship: alpha37 (includes alpha36): Better ad blocking. 1) A site now gets 
  4bd8519 ckpt 2177: AW1d DONE (network $domain=site.* entity domains in Kotlin + engine.js, diffe
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
