# CHECKPOINT 175 — read me first, then TASKS.md

**Written:** 2026-11-11T04:16:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `4430453` (this checkpoint is the commit after it)

## Just done
A3 P0 part 1 (Kestrel G): GeckoView attached ONCE (ntp/error as overlays, setSession swaps), updateUi coalesced + Toolbar.bind diff, FLAG_SECURE only on change, autofill off, no per-block Log.d, blocked-count refresh 4/s. Unit tests green. R1,R2,R5,R6 notes read; R3,R4 files exist.

## Do this next
Next: evidence layer (core health/RingLog + app diag Watchdog/Health + Diagnostics screen + menu items Refresh screen/Diagnostics, Gecko only), read GECKO_SPEED.md + SECURITY.md when their agents finish, apply P0 prefs, emulator smoke test (build -PgeckoAbi=x86_64), ship alpha3. Then D1 engine in extension (prototype: scratchpad jsbench/kfs.mjs - COPY into repo geckoapp assets before the container dies).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  21706f6 ckpt 119: A1 part 1: native UI animations off - instant pressed-state overlay instead of
  3cd7e40 ckpt 117: M1-M2 done: tools/sync.sh (auto-merge other sessions), per-branch cards .check
  cb69194 ckpt 109: Owner request 2 recorded: INBOX captured by the hook, TASKS.md boxes M1-M4 R1-
  286f09a ckpt 104: GitHub Actions pipeline proven: run 42951742468 built, signed, verified and pu
  ecfd90c ckpt 98: ship.sh gecko OK (local gates green, pushed). Dispatched android.yml app=both f
  cdbefdf ckpt 97: gated kestrel-g-v0.2.0-alpha2 (gecko code 101) and pushed it: handoff self-test
  d41eb73 ckpt 94: Ported the owner's save system (Launcher/Portfolio) into Browser: tools/{ckpt,a
```

(55 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
