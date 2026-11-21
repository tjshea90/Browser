# CHECKPOINT 2791 — read me first, then TASKS.md

**Written:** 2026-11-21T06:43:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `9d412fa9` (this checkpoint is the commit after it)

## Just done
LD1c tested: LiveDiagTest 13/13 (switch off = nothing kept, switch survives restart, scrubbed before kept, sensitive sites never named, non-whitelisted layers dropped, off erases journal + file, bounded 800, token format, source guards: switch first, only api.github.com + private check, token not in backups)

## Do this next
LD1d: tools/diag_digest.py (reads ../kestrel-private/diag/**.json, prints crashes / slow hosts / compat errors / playback stalls, synthetic fixtures in tools/test_diag_digest.sh) + one line in tools/resume.sh; then LD1a research/LIVE_DIAG.md + CLAUDE.md decision; LD1e full suite + alpha81 ship (versionCode 182, note docs/handoff/ALPHA81_RELEASE_NOTE.txt)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2050ef12 ckpt 2790: LD1c: app side written + geckoapp compiles: diag/LiveDiag.kt (master switch,
  5d077b35 ckpt 2789: LD1b: core livediag package written + tested (DiagScrub: links -> host, sens
  808aa4f6 ckpt 2787: alpha80 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha80, gecko code 181, run
  f12802a2 ckpt 2786: LD1 written into TASKS.md in the owner's words (live diagnostics engine with
  5c4edd25 ckpt 2785: REL80b: alpha80 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  0c2b1e08 ckpt 2784: gated kestrel-g-v0.3.0-alpha80 (gecko code 181) and pushed it: handoff self-
  0f713e4a ckpt 2783: BK10 PROVEN on the real engine (lab run 44873006097): no-store page + Instan
  08d2776b ckpt 2782: BK10 lab run 44872447722 (suite back): all three rounds failed before any Ba
  2a29901b ckpt 2781: BK10 coded + tested: (1) extension: blocking main_frame listener turns Cache
  d91a5fd5 ckpt 2780: BK10 written into TASKS.md in the owner's words (Back button: slow load whil
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
