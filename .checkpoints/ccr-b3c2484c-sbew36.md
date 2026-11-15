# CHECKPOINT 2382 — read me first, then TASKS.md

**Written:** 2026-11-15T06:09:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `af616a7f` (this checkpoint is the commit after it)

## Just done
SOAK1/RX4-E1 built: .github/workflows/emu-lab.yml (KVM Android 10 x86_64 600x1024@170 2GB 4 cores, builds the Gecko debug APK, text artifacts only, contents: read) started from android.yml with app=lab (+lab_suite, lab_minutes), tools/emu/soak.py (smoke/fs/soak driver, fake-phone --selftest passes), tools/sitelab/soaklab.py (pages, clips, fs page with lock, downloads, report endpoint), app: kestrel_debug dump/closetabs intents (debuggable builds only) + 'page loaded/error' Health lines; RX4-X4a done (failure-step APK uploads removed from android.yml); gecko prefs v9 dom.screenorientation.allow-lock=true + safebrowsing read-back names; FS1 code compiled + unit tests green

## Do this next
dispatch android.yml with app=lab lab_suite=smoke lab_minutes=3 on this branch (mcp__github__actions_run_trigger, owner tjshea90, repo Browser, workflow android.yml, ref ccr-b3c2484c-sbew36); read the job log (actions_list list_workflow_jobs then get_job_logs); fix the lab until smoke is green, then suite=all lab_minutes=60; meanwhile code RX4-M0/X3 text/X2/X1 and write research/EMU_LAB.md + the FS1 section of research/VIDEO_SMOOTHNESS.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c471f4a5 ckpt 2381: FS1 code: core/media/FullscreenTurn (pure rule + 20 tests), geckoapp/web/Ful
  f81e0818 ckpt 2380: saved research/REVIEW-EXTERNAL-4.md (byte-identical copy of the upload) and 
  1f33dcab ckpt 2379: alpha47 RELEASED and recorded (run 43680608062 green, BUILDLOG line written,
  0665c59e ckpt 2378: alpha47 DISPATCHED: workflow android.yml run 43680608062 (run_number 99, ref
  99978930 ckpt 2377: gated kestrel-g-v0.3.0-alpha47 (gecko code 148) and pushed it: handoff self-
  943f5a5e ckpt 2376: YS1: independent review of seekbar.js applied (real YouTube bar markup from 
  021ec124 ckpt 2375: YS1: seekbar.js wired in (manifest, guard flag 16, sbk relay, Settings row '
  b225c11a ckpt 2374: YS1b: seekbar.js (tap/slide on a player's progress bar -> video jumps when t
  a7c3b677 ckpt 2373: owner message 42 written into TASKS.md as YS1 (+YS1a find why a tap on the Y
  43ba62e0 ckpt 2372: alpha46 (gecko code 147) RELEASED + RECORDED (run 43671662558 green, APK on 
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
