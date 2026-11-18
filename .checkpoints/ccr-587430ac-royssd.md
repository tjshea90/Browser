# CHECKPOINT 2646 — read me first, then TASKS.md

**Written:** 2026-11-18T01:18:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `7a5ff192` (this checkpoint is the commit after it)

## Just done
CR1: added the lab suite 'yt' (tools/emu/soak.py yt_journey + yt_after + mem_hog; YT_PAGES/YT_WATCH real m.youtube.com pages, each watch page watched, scrolled, left with Home for 20 s, returned; every second round runs with a RAM hog file in /dev; report section 'YouTube: leaving the app...'); option yt added to android.yml and emu-lab.yml; --selftest passes. Findings so far: Health ring is RAM only, CrashNote only catches Java exceptions, engine .extra reports only native crashes in Gecko processes, Android 10 has no exit-reason API, so a kill by Android leaves NO trace (the visibility gap = CR1c).

## Do this next
dispatch ONCE: mcp__github__actions_run_trigger run_workflow android.yml ref ccr-587430ac-royssd inputs app=lab lab_suite=yt lab_minutes=32 lab_profile=tablet lab_net=full; poll with actions_list/list_workflow_jobs (small), never get_workflow_run; meanwhile write research/CR1_YOUTUBE_CRASH.md and build CR1c (clean-stop marker + persisted flight recorder, Diagnostics line 'Last run ended')

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  05bdbf31 ckpt 2645: CR1 written into TASKS.md in the owner's words (YouTube crash investigation)
  cef3338a ckpt 2644: gated kestrel-g-v0.3.0-alpha68 (gecko code 169) and pushed it: handoff self-
  e38fb0d8 ckpt 2643: alpha67 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha67 code 168, run
  765d1c8a ckpt 2642: alpha67 DISPATCHED ONCE (android.yml app=gecko full_build=true, ref ccr-e89b
  d8340975 ckpt 2641: gated kestrel-g-v0.3.0-alpha67 (gecko code 168) and pushed it: handoff self-
  fd0ea0b5 ckpt 2640: Diagnostics line 'start oat' added (BrowserActivity.oatSizes: sizes of oat/<
  63fdb931 ckpt 2639: LAB7 CONFIRMATION lab run DISPATCHED ONCE: android.yml run 44137227924 (run_
  ebd3854a ckpt 2638: LAB7 first lab run 44126148207 READ (research/LAB7_RESULTS.md next): (1) the
  5b6a24c8 ckpt 2637: LAB7 CORRECTED lab run DISPATCHED ONCE: android.yml run 44126148207 (run_num
  e40a5b70 ckpt 2636: LAB7 lab run 44125632537 CANCELLED ON PURPOSE and replaced by a corrected on
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
