# CHECKPOINT 2266 — read me first, then TASKS.md

**Written:** 2026-11-14T18:00:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `8b3b740` (this checkpoint is the commit after it)

## Just done
alpha39 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha39 (gecko code 140, run 43623337064 green, APK in the Release, BUILDLOG.md line pushed); AX1 + AX1a-f ticked (AX1g = system keyboard app, OWNER YES/NO, not started). Owner answer for message 38 (keyboard part) given in plain words with the Release link. In the tree but NOT in alpha39: GlideTyping.ensurePersonal (export/import/learn load the saved file first; test anExportAndAnImportUseWhatIsSavedEvenBeforeTheEngineWasPrepared), KeyboardFlow empty-text guard + export file written on the IO thread, KEYBOARD-EXPORT.md shortlist note - they ship with the next release

## Do this next
NOTHING is in flight. Wait for the owner: (1) AX1g yes/no: a separate small system-keyboard app reusing :core and importing the export file; (2) the Diagnostics paste after real use (Glide typing line: glide shapes remembered / helped / changed the first guess; Mini keyboard in page fields line; EARLIER CRASH line if the app closes); (3) AI1c Doraby/Cloudflare, choppy video link, scroll/Back feel, Proton Mail numbers, ad-list schedule yes/no, video-site ad switch yes/no, SEC-1 private signing key, network lab results, LIC-1/UI-3/P2b, AS1 yes/no. Optional while idle: AW2 (more scriptlets only if ads still get through), FLK1 (flaky ProxyLinkTest), per-key thumb map (not needed per the lab), shape-injection tuning on real Diagnostics numbers. Any next release: bump geckoapp/build.gradle.kts versionCode 141, bash ship.sh gecko note, dispatch, record

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  418e420 ckpt 2260: alpha39 DISPATCHED (AX1: glide shapes + export/import): android.yml run_workf
  db0854d ckpt 2259: gated kestrel-g-v0.3.0-alpha39 (gecko code 140) and pushed it: handoff self-t
  4c766a1 ckpt 2258: pre-ship: alpha39 (includes alpha38): The keyboard now learns the SHAPE of yo
  899e135 ckpt 2244: AX1: remembered-word injection added (the lab found that for words seen 6+ ti
  54435a1 ckpt 2232: AX1 core built and tested: Fingerprint + PatternMemory (core/input/glide/Patt
  2383c78 ckpt 2206: alpha38 RELEASED AND RECORDED (run 43616372537 green, tag kestrel-g-v0.3.0-al
  2d5a35b ckpt 2201: alpha38 DISPATCHED (AX2 fix: passkey API off + crash note): android.yml run_w
  da5124b ckpt 2200: gated kestrel-g-v0.3.0-alpha38 (gecko code 139) and pushed it: handoff self-t
  b72f9b2 ckpt 2199: pre-ship: alpha38 (includes alpha37): Fixes the app closing on some bank page
  3495b5b ckpt 2198: AX2 root cause found, reproduced and fixed: GeckoView's Android WebAuthn need
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
