# CHECKPOINT 2812 — read me first, then TASKS.md

**Written:** 2026-11-21T16:15:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `8d9aff18` (this checkpoint is the commit after it)

## Just done
LD3 docs + TASKS ticks (a-d) written; digest prints sends automatic/manual + runs + thinned; CLAUDE.md says how to read the diagnostics and where the clone is

## Do this next
Full gate then release: ./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest :geckoapp:compileDebugKotlin (background, poll log for GRADLE_EXIT, check XML not the tail), :app:lintDebug; then bump geckoapp/build.gradle.kts to versionCode 185 / 0.3.0-alpha84, bash tools/sync-ext-version.sh, write docs/handoff/ALPHA84_RELEASE_NOTE.txt, bash ship.sh gecko "note", ONE dispatch (app=gecko full_build=true), wait, record-release with the identical note; then LD3e answer to the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M CLAUDE.md
     M TASKS.md
     M research/LIVE_DIAG.md

## Last ten checkpoints
```
  7369e028 ckpt 2811: LD3 code done and green: core DiagJournal (exact commit, honest lost/thinned
  bb443992 ckpt 2810: LD3d tools done: private.sh + diag_digest.py find the clone in any letter ca
  2015daad ckpt 2809: LD3 audit written into research/LIVE_DIAG.md from the 4 real uploads: manual
  51209a26 ckpt 2808: LD3 written into TASKS.md in the owner's words (4 checks of the live diagnos
  91a90882 ckpt 2807: alpha83 SHIPPED and RECORDED: run 44882471063 (run_number 264, head 805cabb9
  6b5bf826 ckpt 2806: alpha83 DISPATCHED once at 07:55Z on 2026-11-21 (android.yml, app=gecko, ful
  805cabb9 ckpt 2805: gated kestrel-g-v0.3.0-alpha83 (gecko code 184) and pushed it: handoff self-
  6eac0f1a ckpt 2804: LD2e: full gate green (:core:test :app:testDebugUnitTest :geckoapp:compileDe
  d2831830 ckpt 2803: LD2a-d built and tested: app LiveDiag (tables fed after switch+whitelist, fa
  9ebf9afd ckpt 2802: LD2 core written + tested (33 tests in core livediag): DiagStats (event hist
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
