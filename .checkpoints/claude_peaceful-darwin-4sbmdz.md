# CHECKPOINT 2813 — read me first, then TASKS.md

**Written:** 2026-11-21T16:16:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `92e24c3b` (this checkpoint is the commit after it)

## Just done
ALPHA84 release note written (docs/handoff/ALPHA84_RELEASE_NOTE.txt); platform android-37.0 installed with sdkmanager (documented fresh-container fix); gate run for :geckoapp:testDebugUnitTest + :app:lintDebug started in background (log in the session scratchpad gate2.log, ends with GRADLE_EXIT=n)

## Do this next
HEARTBEAT trigger trig_01LyJ9xdLZhzt46J8vWq4DgX (delete when alpha84 is recorded). Next: read gate2 result from the XML (never trust a piped exit); if green: set geckoapp/build.gradle.kts versionCode 185 / versionName 0.3.0-alpha84, bash tools/sync-ext-version.sh, bash ship.sh gecko "<note file text, identical later in record-release>", ONE android.yml dispatch (app=gecko full_build=true note), wait, git ls-remote --tags, get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha84 "<same note>"; then tick LD3e and answer the owner in plain words

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c4318ee7 ckpt 2812: LD3 docs + TASKS ticks (a-d) written; digest prints sends automatic/manual +
  7369e028 ckpt 2811: LD3 code done and green: core DiagJournal (exact commit, honest lost/thinned
  bb443992 ckpt 2810: LD3d tools done: private.sh + diag_digest.py find the clone in any letter ca
  2015daad ckpt 2809: LD3 audit written into research/LIVE_DIAG.md from the 4 real uploads: manual
  51209a26 ckpt 2808: LD3 written into TASKS.md in the owner's words (4 checks of the live diagnos
  91a90882 ckpt 2807: alpha83 SHIPPED and RECORDED: run 44882471063 (run_number 264, head 805cabb9
  6b5bf826 ckpt 2806: alpha83 DISPATCHED once at 07:55Z on 2026-11-21 (android.yml, app=gecko, ful
  805cabb9 ckpt 2805: gated kestrel-g-v0.3.0-alpha83 (gecko code 184) and pushed it: handoff self-
  6eac0f1a ckpt 2804: LD2e: full gate green (:core:test :app:testDebugUnitTest :geckoapp:compileDe
  d2831830 ckpt 2803: LD2a-d built and tested: app LiveDiag (tables fed after switch+whitelist, fa
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
