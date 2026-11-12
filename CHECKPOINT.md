# CHECKPOINT 1132 — read me first, then TASKS.md

**Written:** 2026-11-12T19:33:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d0a4f81` (this checkpoint is the commit after it)

## Just done
alpha22 SHIPPED and recorded (run 43272414589 green incl. the first publish under the new per-job permissions; Release kestrel-g-v0.3.0-alpha22, APK 92.85 MB, certificate verified); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN now every 2 h with the AA3 work list; in the tree but NOT shipped: swipe-to-close on tab cards (TabGridSwipeTest 5), early 'Opening <host>' cover at cold start, NetworkMonitor.start/stop on the IO lane, ConnectProxyTest race fix, ExternalIntents allow-list (shipped in alpha22)

## Do this next
1) ANSWER owner message 14 in chat (Release link on its own line; AA1 Fast Wi-Fi profile, AA2 Courier off by default, AA3 audit + what finished, AA4 alpha22 list, relay U1-U4 answers: ad-block verdict, hardware, Firefox engine, ask what 'Start building the next step' meant; re-list owner-only O1-O18 compactly). 2) verify early cover + swipe-to-close on the emulator (emulator profile had clear_exit=true, now false; app runs build from 17:48), then AA3-2 Typing test page, AA3-7 Back up/Restore, AA3-6 Settings tile rows, AA3-3 D2; ship alpha23 only when several items justify 92 MB. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b51c7bc ckpt 1126: alpha22 first run (#69) FAILED in test-jvm: ConnectProxyTest.statsCountTunnel
  df466e6 ckpt 1125: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  e283585 ckpt 1123: AA3-8 engine check (Mozilla Maven <latest> = ours 160.0.20261104181552), AA3-
  b39b8fd ckpt 1118: alpha22 shipped to GitHub: ship.sh gates green, pushed; workflow run 43270755
  4f54c04 ckpt 1117: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  5c226ea ckpt 1103: AA4: K1 K2 K5 K6 verified on the emulator (evidence in research/SPEED3.md 6.1
  084b5ba ckpt 1101: AA4 speed batch code step: K1 LiveBudget (core, 9 tests) + RAM-adaptive live 
  6fc25a2 ckpt 1077: Reviews saved: research/PERF_KOTLIN.md, research/PERF_JS.md, research/PROMPT_
  7ceb366 ckpt 1075: AA3 audit done by an independent read-only agent: research/PROMPT_AUDIT.md (2
  9c34e86 ckpt 1072: AA1 proven on the emulator (simulated VPN: Network line, trr=0, http3 off, ti
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
