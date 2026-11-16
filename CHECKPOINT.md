# CHECKPOINT 2479 — read me first, then TASKS.md

**Written:** 2026-11-16T04:54:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `1d05d91d` (this checkpoint is the commit after it)

## Just done
lab run 43806840560 dispatched (android.yml app=lab lab_suite=cp, ref ccr-b3c2484c-sbew36, head 1d05d91d, 04:54Z): proves ANS52g (page codec policy) and ANS52h (HLS picture cap) on the real Firefox engine; local gates green: web-tests 524 ok, core + app + geckoapp unit suites green (offline lint could not run: lint-gradle is not cached)

## Do this next
Read lab run 43806840560 when it ends (about 10 minutes): mcp__github__actions_list list_workflow_jobs resource_id 43806840560 (NOT get_workflow_run: it prints the note twice), then artifacts emulator-lab-text via actions_list list_workflow_run_artifacts + actions_get download_workflow_run_artifact + tools/emu/fetch_artifact.py into the scratchpad; read report.md section 'The page's video format policy' and 'The picture size cap for HLS pages' and diagnostics-cp.txt / diagnostics-hs.txt lines 'Video formats offered to sites' and 'Picture size cap'; FIRST check health-lines.txt for 'java heap limit'. If the gzip master list was passed on untouched, decide: Accept-Encoding identity for .m3u8 requests (onBeforeSendHeaders blocking, only while the cap is on). Then tick ANS52g.5 / ANS52h.2-4 in TASKS, write HANDOFF alpha57 map, ship.sh gecko alpha57 + ONE fresh dispatch

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1d05d91d ckpt 2478: ANS52h CODED + UNIT-TESTED: hlscap.js (pure rewriteMaster, web-tests/hlscap.
  d0eb6865 ckpt 2477: ANS52g.5 CODED (not yet run on the emulator): lab page /formats in tools/sit
  85bc93dc ckpt 2476: ANS52g.2-4 CODED + TESTED: codec-main.js (web-tests/codec.test.js 15 ok), re
  f6c00b7a ckpt 2475: ANS52g designed (research/VIDEO_ACCELERATION.md section 'ANS52g design'), cl
  ec1665a5 ckpt 2474: alpha56 RELEASED and recorded: https://github.com/tjshea90/Browser/releases/
  e4d999c9 ckpt 2473: alpha56 re-dispatched on ckpt 2472 (the re-run of the first attempt failed w
  5bcc109b ckpt 2472: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  e074b402 ckpt 2471: alpha57 work started while alpha56 builds on GitHub (run 43799695061: test-j
  7a7ad1ae ckpt 2470: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  4ab1e93b ckpt 2469: alpha56 bundle COMPLETE in code and tests after the worker restart: research
```
