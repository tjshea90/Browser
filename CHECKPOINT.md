# CHECKPOINT 2911 — read me first, then TASKS.md

**Written:** 2026-11-22T04:45:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `c4aa2245` (this checkpoint is the commit after it)

## Just done
alpha91 ship attempt 3 running: attempt 1 red on OwnerRulesTest pin (10 s window, fixed), attempt 2 red on EngineTest.anOutageInTheMiddle... (sampling race: asserted WAITING at one instant while the engine retries every 40-80 ms): both outage tests now assert the download never ends during the outage (not terminal for 250 ms); EngineTest 59/59 alone. ship91b.log

## Do this next
Next: SHIP_EXIT=0 and 'ready for GitHub' in ship91b.log -> ONE android.yml dispatch (note = docs/handoff/ALPHA91_RELEASE_NOTE.txt), send_later +24 min, record-release; then alpha92 = SiblingQualities wiring (quality choices in Kestrel's player)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7693a032 ckpt 2908: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
  32a10e12 ckpt 2906: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
  0b23637d ckpt 2903: PERF6a evidence written: hand-over works when the film list is known (inline
  1935dd23 ckpt 2902: Owner sent diagnostics (rule ON): now even 240p won't load (PERF6 in TASKS.m
  5a294d7e ckpt 2901: alpha91 gate: web 676/676 green; Gradle g10 failed ONLY in core EngineTest.a
  fdc46394 ckpt 2898: Owner after turning the press-to-play rule ON: pornhub still only starts at 
  c85eda2c ckpt 2897: alpha91 built, gates running (g10.log, w4.log): CosmeticFixes now drops porn
  237a0bc2 ckpt 2892: alpha90 upload read (PERF4f in TASKS): typing keys reach the page fast (13 m
  4e9d13f6 ckpt 2891: Owner clarified: 'typing here' and 'photos to this Claude chat' = claude.ai 
  ed86677d ckpt 2890: Owner after alpha90 (PERF4 in TASKS.md): photos still cannot be uploaded to 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
