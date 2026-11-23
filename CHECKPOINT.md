# CHECKPOINT 3077 — read me first, then TASKS.md

**Written:** 2026-11-23T20:28:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `910050d5` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha97 (gecko code 198) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/kind-brahmagupta-ae3yl8, inputs {"app":"gecko","full_build":"true","note":"alpha97: photos attach, Back keeps pages, and a faster-drawing experiment. (1) PHOTOS: found the real cause. The engine does not read the file you pick; it is handed a raw path into shared storage and opens it itself, and Android 10 forbids that without a storage permission, so the page got a file it could not read (your diagnostics showed the file as the right size and type but with unreadable bytes). So your guess about the all files permission was right about the symptom. Kestrel now copies the file you choose into its own private space (with the permission the picker gives for exactly that file) and gives the engine the copy: no storage permission needed, it also works for PDFs and documents, and the copies are removed at the next start, after 6 hours, and for private tabs when the last private tab closes. The storage permission and the Photos and files permission row from alpha95 are removed again because they are not needed. (2) BACK: your tablet pages were refused by the engine cache although you had finished reading them, because the engine still counted late ad and tracker files as loading. When you tap an ordinary link, the page you leave now stops downloading what it had not finished, so the engine can keep the whole page for Back (switch: Settings, Instant Back, on by default); pictures that were still arriving may be missing when you come back. The next diagnostics line says if it worked. (3) SPEED: the drawing fix (Software drawing) cost one kind of page, a very heavy web app, its first picture (2.0 s became 14.6 s) while ordinary pages were not slower; the numbers and the ranked options are in research/PAGE_SPEED_2026-11-23.md. New experimental switch in the Network lab: Calmer redrawing (30 redraws a second instead of 60, off by default); the diagnostics now compare page load times by your settings by themselves. Not run on the tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha97), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha97 "alpha97: photos attach, Back keeps pages, and a faster-drawing experiment. (1) PHOTOS: found the real cause. The engine does not read the file you pick; it is handed a raw path into shared storage and opens it itself, and Android 10 forbids that without a storage permission, so the page got a file it could not read (your diagnostics showed the file as the right size and type but with unreadable bytes). So your guess about the all files permission was right about the symptom. Kestrel now copies the file you choose into its own private space (with the permission the picker gives for exactly that file) and gives the engine the copy: no storage permission needed, it also works for PDFs and documents, and the copies are removed at the next start, after 6 hours, and for private tabs when the last private tab closes. The storage permission and the Photos and files permission row from alpha95 are removed again because they are not needed. (2) BACK: your tablet pages were refused by the engine cache although you had finished reading them, because the engine still counted late ad and tracker files as loading. When you tap an ordinary link, the page you leave now stops downloading what it had not finished, so the engine can keep the whole page for Back (switch: Settings, Instant Back, on by default); pictures that were still arriving may be missing when you come back. The next diagnostics line says if it worked. (3) SPEED: the drawing fix (Software drawing) cost one kind of page, a very heavy web app, its first picture (2.0 s became 14.6 s) while ordinary pages were not slower; the numbers and the ranked options are in research/PAGE_SPEED_2026-11-23.md. New experimental switch in the Network lab: Calmer redrawing (30 redraws a second instead of 60, off by default); the diagnostics now compare page load times by your settings by themselves. Not run on the tablet."
NEVER ship kestrel-g-v0.3.0-alpha97 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  fc06903d ckpt 3074: PERF14 coded (not gated): photo attach copy (core PickStore + tests, Browser
  75f112b7 ckpt 3059: PERF14a research written: photo attach root cause (GeckoView 160 FilePrompt.
  678153fe ckpt 3057: PERF14 written to TASKS.md (owner after alpha96: Back still slow although th
  d8a9f0e7 ckpt 3056: alpha96 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha96 (gecko code 197, run
  ca317a33 ckpt 3054: alpha96 run 45245013667 FAILED at test-jvm on the GitHub runner: two timing 
  48e2535b ckpt 3053: alpha96 (gecko code 197) gated by ship.sh (core + app + geckoapp suites, han
  09eaca29 ckpt 3052: gated kestrel-g-v0.3.0-alpha96 (gecko code 197) and pushed it: handoff self-
  bd5370ef ckpt 3049: PERF12 + PERF13 coded and tested (targeted): cut-short loads, page loop, qui
  fab351e4 ckpt 3039: PERF13 written to TASKS.md (owner: instant first load and instant Back, find
  d41227d4 ckpt 3034: PERF12 written to TASKS.md (owner 2026-11-23: look at the latest diagnostics
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
