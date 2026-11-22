# CHECKPOINT 3031 — read me first, then TASKS.md

**Written:** 2026-11-22T23:05:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `35a6819a` (this checkpoint is the commit after it)

## Just done
alpha95 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha95 (gecko code 196, run 45079749209 green: check, test-js, test-jvm, build-gecko incl. the pref-name check; Release has APK + sha256 + dm; BUILDLOG line pushed). PERF10b/c shipped, NOT confirmed on the tablet; PERF10a/d done

## Do this next
Next: waiting for the owner: (1) Settings > Network lab > Software drawing ON, restart, say whether bands still appear; (2) attach one picture in claude.ai in Kestrel G and press Send now in Diagnostics. Then python3 -B -I tools/diag_digest.py --days 2: read FILE PICKS 'what the PAGE received' (names engine vs claude.ai) and the engine draw-prefs line (software=true, pbo-uploads=false). If bands survive Software drawing: suspect our own overlays (BrowserActivity curtain / look picture / Neon) over the GeckoView. Still open from before: PERF9 tablet confirmation, per-list fps in quality rows, claude.ai FPP canvas A/B.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  f107edb6 ckpt 3029: alpha95 (gecko code 196) gated by ship.sh (core+app+geckoapp suites, handoff
  12cc7bbd ckpt 3028: gated kestrel-g-v0.3.0-alpha95 (gecko code 196) and pushed it: handoff self-
  36dea037 ckpt 3024: pre-ship: alpha95: half-drawn text, attaching photos, and the files permissi
  a732be58 ckpt 3020: PERF10 coded (not yet gated): pbo-uploads off in Safe drawing + Software dra
  5f4fb686 ckpt 3009: PERF10a research written: paint (draw-prefs read-back proves Safe drawing re
  89edc274 ckpt 3008: PERF10 written to TASKS.md (owner after alpha94: paint still chopped, cannot
  37893d32 ckpt 2991: alpha94 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha94 (gecko code 195, run
  41643a29 ckpt 2989: alpha94 (gecko code 195) gated by ship.sh (web 683/683, core+app suites, lin
  117a4f55 ckpt 2988: gated kestrel-g-v0.3.0-alpha94 (gecko code 195) and pushed it: handoff self-
  6fcdd4e1 ckpt 2986: pre-ship: alpha94: the second film that never played. Your upload shows what
```
