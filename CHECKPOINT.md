# CHECKPOINT 2750 — read me first, then TASKS.md

**Written:** 2026-11-20T17:50:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/adoring-hamilton-58trdj` · **builds on:** `b4506ed2` (this checkpoint is the commit after it)

## Just done
RVW4a: researched. Engine prefs / downloads / finder are saturated (all cheap levers already set or a Network-lab switch; font delay is already ON). ONE structural gap found: Mozilla's webcompat built-in extension (feature-webcompat 160.0.1, 321 Android interventions) is not bundled; written to research/REVIEW_2026-11-22_NEWLEVERS.md as NL1. Gradle fetch of the AAR works in this sandbox (scratch project in the scratchpad).

## Do this next
RVW4-NL1 implement: (1) settings.gradle.kts add includeModule org.mozilla.components:feature-webcompat to the mozilla exclusiveContent; (2) geckoapp/build.gradle.kts: configuration webcompat (non-transitive) + abstract task PrepareWebcompat (unzip assets/extensions/webcompat -> assets/webcompat, drop pycache/.deps/*.md/*.py/*.mozbuild, STUB lib/remote_settings_update_check.js to an empty listenForRemoteSettingsUpdates) wired with androidComponents onVariants sources.assets.addGeneratedSourceDirectory; (3) geckoapp WebCompat.kt ensureBuiltIn resource://android/assets/webcompat/ 12 s after first page stop; (4) Prefs.siteFixes (key site_fixes default true) + BackupManager Spec + SettingsView toggle in the Network lab section; (5) Diagnostics line; (6) tests; (7) docs/FILTER-LISTS.md licence row (MPL-2.0); then :geckoapp:mergeDebugAssets to prove the asset pipeline, then look for 2-3 more levers (PB9 buffer sizing, PB11) before shipping alpha75

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8d40d448 ckpt 2749: RVW4 written into TASKS.md in the owner's words (new levers for speed, compa
  aeef6973 ckpt 2748: RVW3 DONE AND SHIPPED: alpha74 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a1da4068 ckpt 2747: REL74b: alpha74 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  4c5ff5b3 ckpt 2746: gated kestrel-g-v0.3.0-alpha74 (gecko code 175) and pushed it: handoff self-
  6ab74f37 ckpt 2745: RVW3 fixes CODED + compile green (:geckoapp:compileDebugKotlin EXIT 0): secu
  7496a10a ckpt 2744: RVW3: security lane back and VERIFIED by me against the code (SC1 SC2 SC4 SC
  fe77b529 ckpt 2743: RVW3a launched: three read-only review lanes running as background agents (p
  e4b18cd0 ckpt 2742: RVW3 written into TASKS.md in the owner's words (review + improve speed, vid
  23e83be6 ckpt 2741: Owner decision recorded: keep Firefox's background traffic (P4) as it is; P3
  94c87088 ckpt 2740: alpha73 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha73 (gecko code 174, run
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
