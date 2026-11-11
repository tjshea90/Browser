# CHECKPOINT 214 — read me first, then TASKS.md

**Written:** 2026-11-11T04:22:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9f08306` (this checkpoint is the commit after it)

## Just done
A2/A4 part: gecko-prefs.txt -> YAML config (AV1 off, memory/cache budget, no animations, push/notifications off, WebRTC LAN leak off) + check-gecko-prefs.sh (18/18 names exist in GeckoView 160); GeckoEngine pins (enterprise roots off, LNA, FPP, CT, PQ, fetchpriority), runtime created after first frame + warmUp; bridge sender check; manifest CSP + host perms + version 1.0.<code>; geckoapp 0.2.0-alpha3 (102) in progress.

## Do this next
Build x86_64 debug (-PgeckoAbi=x86_64), boot AVD kestrel29x64 (scratchpad emu.sh), smoke test: launch, page loads, tabs, typing, Diagnostics screen + Refresh screen, ads blocked; screenshots. Then add check-gecko-prefs.sh to android.yml build-gecko job, ship alpha3.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1739d76 ckpt 209: A3 evidence layer: core RingLog (+tests), app Health (watchdog, frame jank, re
  9c46642 ckpt 175: A3 P0 part 1 (Kestrel G): GeckoView attached ONCE (ntp/error as overlays, setS
  21706f6 ckpt 119: A1 part 1: native UI animations off - instant pressed-state overlay instead of
  3cd7e40 ckpt 117: M1-M2 done: tools/sync.sh (auto-merge other sessions), per-branch cards .check
  cb69194 ckpt 109: Owner request 2 recorded: INBOX captured by the hook, TASKS.md boxes M1-M4 R1-
  286f09a ckpt 104: GitHub Actions pipeline proven: run 42951742468 built, signed, verified and pu
  ecfd90c ckpt 98: ship.sh gecko OK (local gates green, pushed). Dispatched android.yml app=both f
  cdbefdf ckpt 97: gated kestrel-g-v0.2.0-alpha2 (gecko code 101) and pushed it: handoff self-test
  d41eb73 ckpt 94: Ported the owner's save system (Launcher/Portfolio) into Browser: tools/{ckpt,a
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
