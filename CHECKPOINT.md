# CHECKPOINT 2790 — read me first, then TASKS.md

**Written:** 2026-11-21T06:41:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `22e2ca2c` (this checkpoint is the commit after it)

## Just done
LD1c: app side written + geckoapp compiles: diag/LiveDiag.kt (master switch, observe hook in Health.log, bounded scrubbed journal file, private-repo check, GitHub contents PUT with owner token, schedule), Settings section 'Live diagnostics (for Claude)' with token dialog / Send now / Erase, front-of-app trigger in BrowserActivity (+120 s), snapshot without ring, Diagnostics status line, Flavor.hasLiveDiag

## Do this next
LD1c tests: app/src/test LiveDiagTest (Robolectric: off = nothing kept, on = scrubbed, erase, token format, no token in BackupManager keys, observe checks switch first, only REPO + private check in source); then LD1d tools/diag_digest.py + resume.sh line, LD1a research/LIVE_DIAG.md + CLAUDE.md decision, LD1e full suite + alpha81 ship (versionCode 182)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5d077b35 ckpt 2789: LD1b: core livediag package written + tested (DiagScrub: links -> host, sens
  808aa4f6 ckpt 2787: alpha80 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha80, gecko code 181, run
  f12802a2 ckpt 2786: LD1 written into TASKS.md in the owner's words (live diagnostics engine with
  5c4edd25 ckpt 2785: REL80b: alpha80 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  0c2b1e08 ckpt 2784: gated kestrel-g-v0.3.0-alpha80 (gecko code 181) and pushed it: handoff self-
  0f713e4a ckpt 2783: BK10 PROVEN on the real engine (lab run 44873006097): no-store page + Instan
  08d2776b ckpt 2782: BK10 lab run 44872447722 (suite back): all three rounds failed before any Ba
  2a29901b ckpt 2781: BK10 coded + tested: (1) extension: blocking main_frame listener turns Cache
  d91a5fd5 ckpt 2780: BK10 written into TASKS.md in the owner's words (Back button: slow load whil
  a70d55af ckpt 2779: alpha79 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha79, gecko code 180, run
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
