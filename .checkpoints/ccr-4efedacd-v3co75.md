# CHECKPOINT 2748 — read me first, then TASKS.md

**Written:** 2026-11-20T07:58:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-4efedacd-v3co75` · **builds on:** `1598245a` (this checkpoint is the commit after it)

## Just done
RVW3 DONE AND SHIPPED: alpha74 RELEASED AND RECORDED = kestrel-g-v0.3.0-alpha74 (gecko code 175, run 44697177781 all jobs green, APK + sha256 + .dm in the Release, BUILDLOG.md line pushed). Heartbeat trig_012BwCyPqhi5Dfz8amdBigFo deleted. Suites before ship: 2351 JVM tests 0 failures, lint clean, web 649/649

## Do this next
NOTHING is running. Waiting for the owner's tablet test of alpha74 (video retry needs a real hotspot dropout; downloads-yield-to-film needs a download during a film; SP1 per-request time shows in Diagnostics Blocker line). Open, said plainly in research/REVIEW_2026-11-21_*.md: PB2 PB4 PB6 PB8-11, SP2 (lastEvals not yet in Diagnostics) SP3-SP8 (libcrashtools.so, double read-back), SC3 (sync.sh path deny-list: owner decision) SC5 (cert pins need a PC) SC8-SC10 SC12; DNS-rebinding connect-time guard for the probe fetchers

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  a1da4068 ckpt 2747: REL74b: alpha74 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  4c5ff5b3 ckpt 2746: gated kestrel-g-v0.3.0-alpha74 (gecko code 175) and pushed it: handoff self-
  6ab74f37 ckpt 2745: RVW3 fixes CODED + compile green (:geckoapp:compileDebugKotlin EXIT 0): secu
  7496a10a ckpt 2744: RVW3: security lane back and VERIFIED by me against the code (SC1 SC2 SC4 SC
  fe77b529 ckpt 2743: RVW3a launched: three read-only review lanes running as background agents (p
  e4b18cd0 ckpt 2742: RVW3 written into TASKS.md in the owner's words (review + improve speed, vid
  23e83be6 ckpt 2741: Owner decision recorded: keep Firefox's background traffic (P4) as it is; P3
  94c87088 ckpt 2740: alpha73 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha73 (gecko code 174, run
  7f06c310 ckpt 2739: REL73b: alpha73 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5bf338a5 ckpt 2738: gated kestrel-g-v0.3.0-alpha73 (gecko code 174) and pushed it: handoff self-
```
