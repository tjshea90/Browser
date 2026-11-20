# CHECKPOINT 2751 — read me first, then TASKS.md

**Written:** 2026-11-20T17:55:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/adoring-hamilton-58trdj` · **builds on:** `37ccddf3` (this checkpoint is the commit after it)

## Just done
RVW4-NL1 code done and compiling: Gradle prepareWebcompat (AAR from Mozilla Maven -> assets/webcompat, 340 files, Remote Settings listener stubbed, merged into the debug assets: verified), SiteFixes.kt (ensureBuiltIn 12 s after first real page, list+disable when off), Prefs.siteFixes + BackupManager spec + SettingsView toggle + Diagnostics line, core SiteFixesRulesTest 5/5 green, :geckoapp:compileDebugKotlin EXIT 0. NOT run: the extension on a device/emulator.

## Do this next
NL1 proof: dispatch the emulator lab (mcp__github__actions_run_trigger run_workflow android.yml app=lab lab_suite=smoke ref=claude/adoring-hamilton-58trdj) and read artifact for the Health line 'sitefixes running' / Diagnostics 'Site fixes:' (python3 -B -I tools/emu/fetch_artifact.py <url> <empty dir>); meanwhile run :app:testDebugUnitTest BackupManagerTest, write docs/FILTER-LISTS.md MPL-2.0 row + HANDOFF monthly-engine step (bump webcompatVersion with geckoVersion), then look at PB9/PB11/PB10 and a second compat idea before alpha75

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  934b8dad ckpt 2750: RVW4a: researched. Engine prefs / downloads / finder are saturated (all chea
  8d40d448 ckpt 2749: RVW4 written into TASKS.md in the owner's words (new levers for speed, compa
  aeef6973 ckpt 2748: RVW3 DONE AND SHIPPED: alpha74 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a1da4068 ckpt 2747: REL74b: alpha74 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  4c5ff5b3 ckpt 2746: gated kestrel-g-v0.3.0-alpha74 (gecko code 175) and pushed it: handoff self-
  6ab74f37 ckpt 2745: RVW3 fixes CODED + compile green (:geckoapp:compileDebugKotlin EXIT 0): secu
  7496a10a ckpt 2744: RVW3: security lane back and VERIFIED by me against the code (SC1 SC2 SC4 SC
  fe77b529 ckpt 2743: RVW3a launched: three read-only review lanes running as background agents (p
  e4b18cd0 ckpt 2742: RVW3 written into TASKS.md in the owner's words (review + improve speed, vid
  23e83be6 ckpt 2741: Owner decision recorded: keep Firefox's background traffic (P4) as it is; P3
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
