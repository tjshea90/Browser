# CHECKPOINT 1118 — read me first, then TASKS.md

**Written:** 2026-11-12T17:34:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `4f54c04` (this checkpoint is the commit after it)

## Just done
alpha22 shipped to GitHub: ship.sh gates green, pushed; workflow run 43270755926 (android.yml app=gecko full_build=true, tag kestrel-g-v0.3.0-alpha22) queued 17:33Z; TASKS AA3-1 ticked with evidence, SPEED3 6.1-6.3 written

## Do this next
WAIT for run 43270755926 (poll with mcp__github__actions_get method=get_workflow_run resource_id=43270755926; list_workflow_runs output is 75 KB, use python on the saved file); when green: mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha22 (check the APK asset), bash tools/record-release.sh kestrel-g-v0.3.0-alpha22 '<note>', then ANSWER owner message 14 with the Release link on its own line (summary AA1 AA2 AA3 AA4, relay AA3-11 questions, re-list owner-only items O1-O18). Then AA3-2 typing lag (A/B keyboard switch + Typing lab + NetworkMonitor.start off UI thread), AA3-3.. in order. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4f54c04 ckpt 1117: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  5c226ea ckpt 1103: AA4: K1 K2 K5 K6 verified on the emulator (evidence in research/SPEED3.md 6.1
  084b5ba ckpt 1101: AA4 speed batch code step: K1 LiveBudget (core, 9 tests) + RAM-adaptive live 
  6fc25a2 ckpt 1077: Reviews saved: research/PERF_KOTLIN.md, research/PERF_JS.md, research/PROMPT_
  7ceb366 ckpt 1075: AA3 audit done by an independent read-only agent: research/PROMPT_AUDIT.md (2
  9c34e86 ckpt 1072: AA1 proven on the emulator (simulated VPN: Network line, trr=0, http3 off, ti
  a0a56be ckpt 1069: AA2 + AA1 core done: Courier closed in the browser (every option OFF by defau
  ac00a1f ckpt 1061: Owner message 14 written into TASKS.md as AA1-AA4 in their words (INBOX has i
  d75864a ckpt 1057: Owner message 13 ANSWERED and SHIPPED: Kestrel Courier alpha3 (kestrel-courie
  5c2891c ckpt 1054: Docs: CLAUDE.md + BRIEF.md record the owner's unmetered/5 Mbit/s facts and th
```
