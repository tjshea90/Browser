# CHECKPOINT 227 — read me first, then TASKS.md

**Written:** 2026-11-11T05:29:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f8c25d1` (this checkpoint is the commit after it)

## Just done
Owner request 3 recorded in TASKS.md (N1-N7); owner likes alpha3; UI-1 answered: everything at the bottom.

## Do this next
Claim N2-N5 (bash tools/claim.sh N2 ...), read Toolbar.kt/NewTabPage/TabSwitcher/Tab + BrowserActivity show/closeTab/handleBack, then: dark tokens + ChamferDrawable, BottomDock + HomeView in the Gecko activity, private/regular tab separation, thumbnail tab grid; emulator screenshots; then updater N1; ship alpha4; then downloads N6 (research/DOWNLOADS.md).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  04080cd ckpt 224: Alpha3 (kestrel-g-v0.2.0-alpha3, run 42961586675) built, signed, released and 
  1ab978e ckpt 221: ship.sh gecko OK; dispatched android.yml app=gecko full_build=true: run 429615
  85eab84 ckpt 220: gated kestrel-g-v0.2.0-alpha3 (gecko code 102) and pushed it: handoff self-tes
  57a6d62 ckpt 219: alpha3 candidate: freeze fixes verified on emulator (worker bridge, cosmetics 
  04dab0c ckpt 214: A2/A4 part: gecko-prefs.txt -> YAML config (AV1 off, memory/cache budget, no a
  1739d76 ckpt 209: A3 evidence layer: core RingLog (+tests), app Health (watchdog, frame jank, re
  9c46642 ckpt 175: A3 P0 part 1 (Kestrel G): GeckoView attached ONCE (ntp/error as overlays, setS
  21706f6 ckpt 119: A1 part 1: native UI animations off - instant pressed-state overlay instead of
  3cd7e40 ckpt 117: M1-M2 done: tools/sync.sh (auto-merge other sessions), per-branch cards .check
  cb69194 ckpt 109: Owner request 2 recorded: INBOX captured by the hook, TASKS.md boxes M1-M4 R1-
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
