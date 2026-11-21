# CHECKPOINT 2809 — read me first, then TASKS.md

**Written:** 2026-11-21T16:01:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `51209a26` (this checkpoint is the commit after it)

## Just done
LD3 audit written into research/LIVE_DIAG.md from the 4 real uploads: manual path works end to end, automatic path never seen (re-arm bug + manual presses eat the 4/day cap), no overwrite (create-only PUT, git history A-only), Claude read works via GitHub MCP (tested) but diag_digest.py/private.sh look in ../kestrel-private while the clone is ../Kestrel-Private (case); defects A journal trim re-sends 9 lines + loses 9 silently, B noisy kinds crowd the 800 cap, C no run marker/clock, D advisor only sees one upload

## Do this next
FIX ORDER: (1) tools/diag_digest.py + tools/private.sh case-insensitive clone resolver + read origin/main + --verify (no M/D in history) + dedupe + performance section; (2) core DiagJournal (exact commit by sequence, honest lost count, per-kind cap) + tests in core livediag; (3) LiveDiag.kt: use DiagJournal, in-flight guard, create-only path retry on 422, re-arm sendTask on time-based refusal, manual send not counted in the automatic cap, run marker + uptimeS, rolling 7-day DiagStats for the advisor; (4) source-reading test: no sha, no DELETE; (5) gate, bump alpha84, ship per REL-STANDING

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/LIVE_DIAG.md

## Last ten checkpoints
```
  51209a26 ckpt 2808: LD3 written into TASKS.md in the owner's words (4 checks of the live diagnos
  91a90882 ckpt 2807: alpha83 SHIPPED and RECORDED: run 44882471063 (run_number 264, head 805cabb9
  6b5bf826 ckpt 2806: alpha83 DISPATCHED once at 07:55Z on 2026-11-21 (android.yml, app=gecko, ful
  805cabb9 ckpt 2805: gated kestrel-g-v0.3.0-alpha83 (gecko code 184) and pushed it: handoff self-
  6eac0f1a ckpt 2804: LD2e: full gate green (:core:test :app:testDebugUnitTest :geckoapp:compileDe
  d2831830 ckpt 2803: LD2a-d built and tested: app LiveDiag (tables fed after switch+whitelist, fa
  9ebf9afd ckpt 2802: LD2 core written + tested (33 tests in core livediag): DiagStats (event hist
  6719ce3e ckpt 2801: gated kestrel-g-v0.3.0-alpha82 (gecko code 183) and pushed it: handoff self-
  f95e13ba ckpt 2800: LD2 written into TASKS.md in the owner's words (LD2a-e): smarter + more comp
  946369a8 ckpt 2799: alpha82 prepared: versionCode 183 / 0.3.0-alpha82, extension version synced,
```
