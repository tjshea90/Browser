# CHECKPOINT 372 — read me first, then TASKS.md

**Written:** 2026-11-11T16:10:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3c8f9fe` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha6: a page that Android frees to save memory now comes back where you were (dimmed preview instead of a white screen, scroll position kept); the decoded-image cache is capped at about 95 MB (it could grow to about 475 MB and set off memory-flush stutter); new Settings > Scroll feel and Diagnostics > Scroll test (measures smoothness), GPU info in Diagnostics; the neon glow and page previews stay out of the way while you scroll"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha6), then:  bash tools/record-release.sh kestrel-g-v0.2.0-alpha6 "alpha6: a page that Android frees to save memory now comes back where you were (dimmed preview instead of a white screen, scroll position kept); the decoded-image cache is capped at about 95 MB (it could grow to about 475 MB and set off memory-flush stutter); new Settings > Scroll feel and Diagnostics > Scroll test (measures smoothness), GPU info in Diagnostics; the neon glow and page previews stay out of the way while you scroll"
NEVER ship kestrel-g-v0.2.0-alpha6 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3c8f9fe ckpt 371: pre-ship: alpha6: a page that Android frees to save memory now comes back wher
  1d4be6f ckpt 369: Q2 work for alpha6 (code compiles, emulator-tested: Q2a recovery + Scroll test
  6339191 ckpt 352: alpha5 SHIPPED + RECORDED (run 42989432755 green, kestrel-g-v0.2.0-alpha5, APK
  e9f12cd ckpt 344: gated kestrel-g-v0.2.0-alpha5 (gecko code 104) and pushed it: handoff self-tes
  b84308a ckpt 342: P3+P4 DONE and verified on the emulator (blue REAPER theme: Home flames/scythe
  bf1ed62 ckpt 316: P3/P4 neon wired and compiling (both modules): Neon engine (RenderThread alpha
  6438c0a ckpt 306: Owner request 4 recorded (P1-P4: hardware speed research, engine research incl
  8d8143c ckpt 301: N6 step 2 compiles: Android download shell (SqliteStore, MediaStoreSinks, UrlC
  c7112dc ckpt 294: N6 step 1 done: pure-Kotlin download engine in core/ (app.kestrel.core.downloa
  ce4c6e7 ckpt 281: handoff saved via context-handoff plugin (.handoff/handoff.md + json + archive
```
