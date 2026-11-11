# CHECKPOINT 104 — read me first, then TASKS.md

**Written:** 2026-11-11T03:24:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `346a39e` (this checkpoint is the commit after it)

## Just done
GitHub Actions pipeline proven: run 42951742468 built, signed, verified and published kestrel-v0.1.1 (web) and kestrel-g-v0.2.0-alpha2 (gecko, prerelease); both recorded in BUILDLOG.md; save system ported; docs updated.

## Do this next
Wait for the owner: install kestrel-g-0.2.0-alpha2.apk from the Releases page over alpha1 and report claude.ai / Google / speed / memory on the tablet (TASKS.md OWNER lines). Then: generic cosmetic class/id index, Gecko gaps, Gecko unit tests (TASKS.md). Any new release = bump versionCode, bash ship.sh <web|gecko> note, dispatch android.yml, record-release.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ecfd90c ckpt 98: ship.sh gecko OK (local gates green, pushed). Dispatched android.yml app=both f
  cdbefdf ckpt 97: gated kestrel-g-v0.2.0-alpha2 (gecko code 101) and pushed it: handoff self-test
  d41eb73 ckpt 94: Ported the owner's save system (Launcher/Portfolio) into Browser: tools/{ckpt,a
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
