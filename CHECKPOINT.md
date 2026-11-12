# CHECKPOINT 1123 — read me first, then TASKS.md

**Written:** 2026-11-12T17:38:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `40ea83f` (this checkpoint is the commit after it)

## Just done
AA3-8 engine check (Mozilla Maven <latest> = ours 160.0.20261104181552), AA3-9 partly (workflow read-only default + write on the 3 publish jobs + persist-credentials false, gate run #70 green; ExternalIntents action allow-list + test), AA3-10 bookkeeping (18 boxes ticked with BUILDLOG evidence, stale weekly/metered wording fixed)

## Do this next
WAIT for run 43270755926 (alpha22 full gecko build): mcp__github__actions_get get_workflow_run; when success: get_release_by_tag kestrel-g-v0.3.0-alpha22, bash tools/record-release.sh kestrel-g-v0.3.0-alpha22 '<note from CHECKPOINT/ship log>', answer owner message 14 (Release link on its own line). Then AA3-2 (NetworkMonitor.start off UI thread, keyboard A/B switch, Typing lab), AA3-7 Backup/Restore, AA3-6 UI polish, AA3-3 D2 cosmetic index, AA3-4, AA3-5. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M BRIEF.md
     M CLAUDE.md
     M TASKS.md
     M app/src/main/kotlin/app/kestrel/browser/ui/TabGrid.kt
     M docs/FILTER-LISTS.md

## Last ten checkpoints
```
  b39b8fd ckpt 1118: alpha22 shipped to GitHub: ship.sh gates green, pushed; workflow run 43270755
  4f54c04 ckpt 1117: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  5c226ea ckpt 1103: AA4: K1 K2 K5 K6 verified on the emulator (evidence in research/SPEED3.md 6.1
  084b5ba ckpt 1101: AA4 speed batch code step: K1 LiveBudget (core, 9 tests) + RAM-adaptive live 
  6fc25a2 ckpt 1077: Reviews saved: research/PERF_KOTLIN.md, research/PERF_JS.md, research/PROMPT_
  7ceb366 ckpt 1075: AA3 audit done by an independent read-only agent: research/PROMPT_AUDIT.md (2
  9c34e86 ckpt 1072: AA1 proven on the emulator (simulated VPN: Network line, trr=0, http3 off, ti
  a0a56be ckpt 1069: AA2 + AA1 core done: Courier closed in the browser (every option OFF by defau
  ac00a1f ckpt 1061: Owner message 14 written into TASKS.md as AA1-AA4 in their words (INBOX has i
  d75864a ckpt 1057: Owner message 13 ANSWERED and SHIPPED: Kestrel Courier alpha3 (kestrel-courie
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
