# CHECKPOINT 2789 — read me first, then TASKS.md

**Written:** 2026-11-21T06:37:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `b354576d` (this checkpoint is the commit after it)

## Just done
LD1b: core livediag package written + tested (DiagScrub: links -> host, sensitive sites never named, secrets / cards / emails / phones removed, a line that still looks risky is dropped; SensitiveSites = Kotlin port of the extension's rule with the same corpus; DiagPolicy layer whitelist; DiagBatch JSON + path; DiagSchedule): 13 of 13 tests. The first checkpoint attempt was REFUSED by tools/secretscan.sh (a PEM header literal in a test); fixed with the project's FAKE marker, scanner untouched. alpha80 recorded earlier.

## Do this next
LD1c: app side: LiveDiag.kt (app/diag): prefs liveDiag (default ON) + token + counters; observe() hook in Health.log (volatile off-switch first); bounded journal file; sender (GitHub contents API PUT to tjshea90/kestrel-private, Bearer token, scrubbed batch); Settings section with the token dialog (pattern: pairDialog in SettingsView ~283); trigger from BrowserActivity onResume next to UpdateManager.maybeAutoCheck (~629, delayed 120 s); snapshot = Health.report(this, engineFacts(), 400) via a supplier; Diagnostics line; erase on OFF. Then LD1d tools/diag_digest.py + resume.sh, research/LIVE_DIAG.md, CLAUDE.md decision, tests, ship alpha81

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M .checkpoints/claude_sweet-gates-gr3bj5.md
     M core/src/main/kotlin/app/kestrel/core/livediag/DiagScrub.kt
    ?? core/src/main/kotlin/app/kestrel/core/livediag/DiagBatch.kt
    ?? core/src/test/kotlin/app/kestrel/core/livediag/

## Last ten checkpoints
```
  808aa4f6 ckpt 2787: alpha80 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha80, gecko code 181, run
  f12802a2 ckpt 2786: LD1 written into TASKS.md in the owner's words (live diagnostics engine with
  5c4edd25 ckpt 2785: REL80b: alpha80 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  0c2b1e08 ckpt 2784: gated kestrel-g-v0.3.0-alpha80 (gecko code 181) and pushed it: handoff self-
  0f713e4a ckpt 2783: BK10 PROVEN on the real engine (lab run 44873006097): no-store page + Instan
  08d2776b ckpt 2782: BK10 lab run 44872447722 (suite back): all three rounds failed before any Ba
  2a29901b ckpt 2781: BK10 coded + tested: (1) extension: blocking main_frame listener turns Cache
  d91a5fd5 ckpt 2780: BK10 written into TASKS.md in the owner's words (Back button: slow load whil
  a70d55af ckpt 2779: alpha79 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha79, gecko code 180, run
  5a64fefc ckpt 2778: alpha79 run 44849629774: test-jvm failed ONCE on PhoneBackendTest.aPhoneThat
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
