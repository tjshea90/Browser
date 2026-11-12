# CHECKPOINT 729 — read me first, then TASKS.md

**Written:** 2026-11-12T02:46:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `976f724` (this checkpoint is the commit after it)

## Just done
stream engine: a dropped job (merged-away sound of a pair, cancelled, cleared) is never written back by a late checkpoint (CI flake pictureAndSoundAreDownloadedTogetherAndMergedIntoOneFile: the hidden sound row came back); stream tests 10x green incl. under CPU load. Emulator: 12 MB download through the real Gecko fetcher completes, sha256 equals the original. CI run 43142630457 failed only on that flake (test-js green, 57 tests)

## Do this next
re-dispatch android.yml at HEAD (note: alpha16 list in /tmp/ship16.log + extras), then watch for the tag; if Publish the Release 403 repeats, it is persistent (not the note, not the commit): ask the owner to check repo Settings > Actions > General > Workflow permissions and Settings > Rules

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9226e37 ckpt 726: TASKS X4/X5/X12 statuses with evidence; HLS auto-resume test; lintRelease clea
  d41caca ckpt 724: M7 content sanity (web page where a file/video was expected -> BAD_CONTENT, HL
  61e1894 ckpt 721: M1b validator-less resume: Sink.readable/pread (Os.pread on both Android sinks
  f17fcb1 ckpt 719: web guard tests fixed (tap reports) + 2 new; CI test-js now gates the Gecko bu
  b1d2872 ckpt 715: Last-Modified splice guard (EngineTest), research/AUDIT-2026-11.md written, do
  7be9a12 ckpt 712: alpha16 dispatched to GitHub Actions (tag kestrel-g-v0.3.0-alpha16 appears whe
  21a22c2 ckpt 711: gated kestrel-g-v0.3.0-alpha16 (gecko code 115) and pushed it: handoff self-te
  7d6778a ckpt 710: pre-ship: alpha16: daily update dialog (app, engine, ad-block lists) with Upda
  6ab16b5 ckpt 709: X3 first batch done+tested: auto-resume after kill (both engines, SQLite v3 cr
  2d7afd4 ckpt 700: X2 verified on emulator (dialog found/ready/install flow); X12 first pieces co
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
