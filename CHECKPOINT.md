# CHECKPOINT 2933 — read me first, then TASKS.md

**Written:** 2026-11-22T06:16:12Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `92d65df1` (this checkpoint is the commit after it)

## Just done
PERF7 started on branch ccr-144aedec-7apwin: owner request + 2 screenshots written to TASKS.md (PERF7a-c) and research/PAINT_GLITCH.md (screenshots read: horizontal-stripe loss inside text runs on claude.ai = GPU partial-update corruption signature, not CSS); newest upload 060220 (alpha91, 109 s run) read: pornhub page, finder knew the lists at +28 s, the owner never pressed play on the film (no hand-over / hand-declined line, 'Web videos in Kestrel's own player: no film asked yet'), hidden rules on pornhub = 4 header tabs, 3+1 ad links, cookie banner only

## Do this next
Next: (1) gradle :geckoapp:compileDebugKotlin was started in background (log in scratchpad g1.log) to get the GeckoView AAR so tools/check-gecko-prefs.sh can verify gfx.webrender.* pref names (partial present, batched upload, precache-shaders) before adding a Settings 'Safe drawing' switch (GeckoEngine live prefs + gecko-prefs.txt) and a Diagnostics paint line; (2) PERF5b/alpha92 sibling qualities in Kestrel's player (PlayerQuality.alts, VideoFlow, PlayerFactory.makeSource); (3) answer the owner, ship alpha92 per standing rule (versionCode must exceed 192)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4da54c18 ckpt 2915: alpha91 RELEASED and recorded (run 44987313373 green, Release has the APK). 
  a42e5268 ckpt 2913: alpha91 (code 192) gated (ship.sh green on attempt 3) and ONE dispatch made 
  df3d34d1 ckpt 2912: gated kestrel-g-v0.3.0-alpha91 (gecko code 192) and pushed it: handoff self-
  345be8b0 ckpt 2911: alpha91 ship attempt 3 running: attempt 1 red on OwnerRulesTest pin (10 s wi
  7693a032 ckpt 2908: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
  32a10e12 ckpt 2906: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
  0b23637d ckpt 2903: PERF6a evidence written: hand-over works when the film list is known (inline
  1935dd23 ckpt 2902: Owner sent diagnostics (rule ON): now even 240p won't load (PERF6 in TASKS.m
  5a294d7e ckpt 2901: alpha91 gate: web 676/676 green; Gradle g10 failed ONLY in core EngineTest.a
  fdc46394 ckpt 2898: Owner after turning the press-to-play rule ON: pornhub still only starts at 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
