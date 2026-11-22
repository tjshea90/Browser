# CHECKPOINT 2906 — read me first, then TASKS.md

**Written:** 2026-11-22T04:19:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `9f9213e9` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. (1) Your last upload shows why 240p plays only after you change the quality: Kestrel's own player takes over a pornhub film fine (hardware decoder, no stalls, 1.6 percent frames dropped) but only when it already knows the film's list; on a fresh page pornhub asks for its list only AFTER you press play, so the first press found nothing, was refused, and the page tried to play the 60 frames a second stream itself (40 percent of its frames dropped, or it never started). The refused press is now remembered for 20 seconds (it was 10, and only for some players) and the film moves to Kestrel's player when its list appears; every refusal is now logged with why. Pornhub gives one list per quality, so Kestrel's player offers no quality choice yet: I am building that next from the page's other lists. (2) The clipped loading circle: the watch page report names pornhub's ad boxes around the player as the hidden elements, the only ones that change the layout, so those rules are now dropped for pornhub (the ads themselves stay blocked on the network, so a box left standing is empty); the page report also looks again 12 seconds after the load. (3) Typing: your upload shows keys reach the page at once (7 ms on a page, 13 ms in the address bar), so the lag is what claude.ai does after each key; alpha91 measures that too. (4) Photos: your two pictures were fine and were handed to claude.ai; alpha91 records a page's unhandled errors, so try attaching a photo once more, then press Send now. Not run on the tablet: everything above.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M docs/handoff/ALPHA91_RELEASE_NOTE.txt
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  0b23637d ckpt 2903: PERF6a evidence written: hand-over works when the film list is known (inline
  1935dd23 ckpt 2902: Owner sent diagnostics (rule ON): now even 240p won't load (PERF6 in TASKS.m
  5a294d7e ckpt 2901: alpha91 gate: web 676/676 green; Gradle g10 failed ONLY in core EngineTest.a
  fdc46394 ckpt 2898: Owner after turning the press-to-play rule ON: pornhub still only starts at 
  c85eda2c ckpt 2897: alpha91 built, gates running (g10.log, w4.log): CosmeticFixes now drops porn
  237a0bc2 ckpt 2892: alpha90 upload read (PERF4f in TASKS): typing keys reach the page fast (13 m
  4e9d13f6 ckpt 2891: Owner clarified: 'typing here' and 'photos to this Claude chat' = claude.ai 
  ed86677d ckpt 2890: Owner after alpha90 (PERF4 in TASKS.md): photos still cannot be uploaded to 
  b02ae3d2 ckpt 2889: alpha90 RELEASED and recorded (run 44970827834 green, Release has the APK). 
  16cc4189 ckpt 2887: alpha90 (code 191) gated and shipped: ONE dispatch made right after ship.sh 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
