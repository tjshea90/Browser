# CHECKPOINT 2991 — read me first, then TASKS.md

**Written:** 2026-11-22T08:46:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `3430636e` (this checkpoint is the commit after it)

## Just done
alpha94 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha94 (gecko code 195, run 45003176346 green, Release has APK + sha256 + dm, BUILDLOG line pushed). PERF9b shipped, NOT confirmed on the tablet

## Do this next
Next: waiting for the owner's tablet test of alpha94. Read the next upload (python3 -B -I tools/diag_digest.py --days 2): (1) a 'video hand-declined press' / 'hand-over' for the SECOND film and WebHandStats asked vs opened-by-tap counts (asked should now match taps); (2) 'video calm' lines; (3) clipped circle: NOT page layout (cutBottom -145) and Safe drawing applied (draw-prefs line), so if it persists next candidates are pbo-uploads=true, layer-compositor=false, GPU process off (layers.gpu-process.enabled), or the circle is the 24px svg drawn by WebRender (try a Settings switch for software WebRender gfx.webrender.software); (4) still open: per-list fps in quality rows, auto step-down on a stall, claude.ai exclude / FPP canvas A/B (owner decision), pornhub page player 720p stall cause

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  41643a29 ckpt 2989: alpha94 (gecko code 195) gated by ship.sh (web 683/683, core+app suites, lin
  117a4f55 ckpt 2988: gated kestrel-g-v0.3.0-alpha94 (gecko code 195) and pushed it: handoff self-
  6fcdd4e1 ckpt 2986: pre-ship: alpha94: the second film that never played. Your upload shows what
  ae98cd8b ckpt 2978: PERF9 written to TASKS.md (owner after alpha93: one video worked, another sh
  c97f7f67 ckpt 2977: alpha93 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha93 (gecko code 194, run
  a882ac50 ckpt 2974: alpha93 (gecko code 194) gated by ship.sh and ONE dispatch made on ccr-144ae
  5488bb85 ckpt 2973: gated kestrel-g-v0.3.0-alpha93 (gecko code 194) and pushed it: handoff self-
  bf393943 ckpt 2971: pre-ship: alpha93: quality button in the full-screen player, the stuck loadi
  335cc029 ckpt 2968: PERF8 code (not released): (a) VideoFlow.offerSiblings gives the player that
  06774fc1 ckpt 2959: PERF8 written to TASKS.md (owner after alpha92: fullscreen Kestrel player wo
```
