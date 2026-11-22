# CHECKPOINT 2938 — read me first, then TASKS.md

**Written:** 2026-11-22T06:19:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `27fbe592` (this checkpoint is the commit after it)

## Just done
PERF7c step 1: Settings > Network lab 'Safe drawing' (lab_draw, default ON) writes 4 engine graphics prefs into the start-up config (GeckoPrefs.drawLines: scissored-cache-clears off, batched-texture-uploads off, max-partial-present-rects 0, allow-partial-present-buffer-age off); names verified in GeckoView 160 libxul (tools/check-gecko-prefs.sh now also reads GeckoPrefs.kt: 60 names, 0 missing); read-back names added to GeckoEngine; OwnerRulesTest.perf7SafeDrawing pin; core SiblingQualities.startFor + test (PERF5b core). NOT yet compiled or tested

## Do this next
Next: (1) wire PERF5b: PlayerFactory.Built.newSource(url) closure; PlayerQuality.alts/currentUrl/switch + alt rows; NativePlayerActivity EXTRA_ALTS + switchSource; InlinePlayer/InlineHost start(alts) + full-screen handoff keeps alts; VideoFlow.siblingsOf(tab,url) used in start(), playNative; (2) run ./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin in background (poll g2.log for EXIT); (3) bump versionCode 193 / alpha92, sync-ext-version, ship.sh gecko, ONE dispatch, record-release; (4) answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  449480c7 ckpt 2933: PERF7 started on branch ccr-144aedec-7apwin: owner request + 2 screenshots w
  4da54c18 ckpt 2915: alpha91 RELEASED and recorded (run 44987313373 green, Release has the APK). 
  a42e5268 ckpt 2913: alpha91 (code 192) gated (ship.sh green on attempt 3) and ONE dispatch made 
  df3d34d1 ckpt 2912: gated kestrel-g-v0.3.0-alpha91 (gecko code 192) and pushed it: handoff self-
  345be8b0 ckpt 2911: alpha91 ship attempt 3 running: attempt 1 red on OwnerRulesTest pin (10 s wi
  7693a032 ckpt 2908: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
  32a10e12 ckpt 2906: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
  0b23637d ckpt 2903: PERF6a evidence written: hand-over works when the film list is known (inline
  1935dd23 ckpt 2902: Owner sent diagnostics (rule ON): now even 240p won't load (PERF6 in TASKS.m
  5a294d7e ckpt 2901: alpha91 gate: web 676/676 green; Gradle g10 failed ONLY in core EngineTest.a
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
