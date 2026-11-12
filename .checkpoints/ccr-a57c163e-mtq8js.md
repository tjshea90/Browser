# CHECKPOINT 1126 — read me first, then TASKS.md

**Written:** 2026-11-12T17:43:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `df466e6` (this checkpoint is the commit after it)

## Just done
alpha22 first run (#69) FAILED in test-jvm: ConnectProxyTest.statsCountTunnelsAndBytes raced the relay counters (passed locally, failed on the CI runner); fixed the test (wait up to 5 s for the counters), core suite 549/549 twice with --rerun-tasks; re-shipped the same version 121 (no tag was created by the failed run) and re-dispatched: second full gecko run queued (this is also the first publish under the new per-job permissions)

## Do this next
WAIT for the second alpha22 run (list_workflow_runs output is 75 KB: parse the saved file with python3 -I; newest id after 43271063952 / 43270755926); poll mcp__github__actions_get get_workflow_run; when success: get_release_by_tag kestrel-g-v0.3.0-alpha22, bash tools/record-release.sh, answer owner message 14. If build-gecko fails on permissions (contents write per job), the cause is the new workflow hardening: fix permissions in .github/workflows/android.yml and re-dispatch. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  df466e6 ckpt 1125: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  e283585 ckpt 1123: AA3-8 engine check (Mozilla Maven <latest> = ours 160.0.20261104181552), AA3-
  b39b8fd ckpt 1118: alpha22 shipped to GitHub: ship.sh gates green, pushed; workflow run 43270755
  4f54c04 ckpt 1117: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  5c226ea ckpt 1103: AA4: K1 K2 K5 K6 verified on the emulator (evidence in research/SPEED3.md 6.1
  084b5ba ckpt 1101: AA4 speed batch code step: K1 LiveBudget (core, 9 tests) + RAM-adaptive live 
  6fc25a2 ckpt 1077: Reviews saved: research/PERF_KOTLIN.md, research/PERF_JS.md, research/PROMPT_
  7ceb366 ckpt 1075: AA3 audit done by an independent read-only agent: research/PROMPT_AUDIT.md (2
  9c34e86 ckpt 1072: AA1 proven on the emulator (simulated VPN: Network line, trr=0, http3 off, ti
  a0a56be ckpt 1069: AA2 + AA1 core done: Courier closed in the browser (every option OFF by defau
```
