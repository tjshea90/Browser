# CHECKPOINT 2810 — read me first, then TASKS.md

**Written:** 2026-11-21T16:05:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `b75d850b` (this checkpoint is the commit after it)

## Just done
LD3d tools done: private.sh + diag_digest.py find the clone in any letter case (cloud harness makes ../Kestrel-Private), digest fetches + reads origin/main from git objects (no working-tree change), prints FILES + INTEGRITY (add-only proof from git history) + TUNING NUMBERS per file (cold start, memory, heap, CPU, jank, back, video) + how earlier runs ended, dedupes lines an older app re-sent, no false crash/site hits; private.sh now links INBOX/OUTBOX in this container (they were NOT being saved before); test_handoff 80/80

## Do this next
LD3 code: core DiagJournal (exact commit by sequence number, honest lost count, per-kind cap) + core tests; then LiveDiag.kt uses it (in-flight guard, create-only path retry on 422, re-arm on time-based refusal, manual send not in the auto cap, run marker + uptimeS, rolling 7-day DiagStats for the advisor); source-reading test (no sha, no DELETE); gate; alpha84

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M tools/diag_digest.py

## Last ten checkpoints
```
  2015daad ckpt 2809: LD3 audit written into research/LIVE_DIAG.md from the 4 real uploads: manual
  51209a26 ckpt 2808: LD3 written into TASKS.md in the owner's words (4 checks of the live diagnos
  91a90882 ckpt 2807: alpha83 SHIPPED and RECORDED: run 44882471063 (run_number 264, head 805cabb9
  6b5bf826 ckpt 2806: alpha83 DISPATCHED once at 07:55Z on 2026-11-21 (android.yml, app=gecko, ful
  805cabb9 ckpt 2805: gated kestrel-g-v0.3.0-alpha83 (gecko code 184) and pushed it: handoff self-
  6eac0f1a ckpt 2804: LD2e: full gate green (:core:test :app:testDebugUnitTest :geckoapp:compileDe
  d2831830 ckpt 2803: LD2a-d built and tested: app LiveDiag (tables fed after switch+whitelist, fa
  9ebf9afd ckpt 2802: LD2 core written + tested (33 tests in core livediag): DiagStats (event hist
  6719ce3e ckpt 2801: gated kestrel-g-v0.3.0-alpha82 (gecko code 183) and pushed it: handoff self-
  f95e13ba ckpt 2800: LD2 written into TASKS.md in the owner's words (LD2a-e): smarter + more comp
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
