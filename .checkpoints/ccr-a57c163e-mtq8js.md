# CHECKPOINT 1077 — read me first, then TASKS.md

**Written:** 2026-11-12T16:12:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `938dd44` (this checkpoint is the commit after it)

## Just done
Reviews saved: research/PERF_KOTLIN.md, research/PERF_JS.md, research/PROMPT_AUDIT.md (corrected copy, no placeholder); AA4 claimed; SPEED3.md section 6 holds the ordered plan of the speed batch

## Do this next
AA4 batch step 1 = K1 in geckoapp/.../BrowserActivity.kt: enforceLiveLimit (line ~630) budget from /proc/meminfo MemAvailable via Health.meminfo (table in research/PERF_KOTLIN.md K1), onTrimMemory (~361) no unconditional purge at >=10, private hibernate keeps pendingLoad=url, gecko-prefs.txt v5 max_total_viewers 6; add a pure LiveBudget object in :core with JVM tests; then K2 (session state files), K3/K4/K6, K5, cheap ones, JS (B1 first), ship alpha22 (versionCode 121), reply to owner message 14. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/SPEED3.md

## Last ten checkpoints
```
  7ceb366 ckpt 1075: AA3 audit done by an independent read-only agent: research/PROMPT_AUDIT.md (2
  9c34e86 ckpt 1072: AA1 proven on the emulator (simulated VPN: Network line, trr=0, http3 off, ti
  a0a56be ckpt 1069: AA2 + AA1 core done: Courier closed in the browser (every option OFF by defau
  ac00a1f ckpt 1061: Owner message 14 written into TASKS.md as AA1-AA4 in their words (INBOX has i
  d75864a ckpt 1057: Owner message 13 ANSWERED and SHIPPED: Kestrel Courier alpha3 (kestrel-courie
  5c2891c ckpt 1054: Docs: CLAUDE.md + BRIEF.md record the owner's unmetered/5 Mbit/s facts and th
  95c9050 ckpt 1053: gated kestrel-g-v0.3.0-alpha21 (gecko code 120) and pushed it: handoff self-t
  2a514af ckpt 1052: gated kestrel-courier-v0.1.0-alpha3 (courier code 3) and pushed it: handoff s
  d803d90 ckpt 1051: pre-ship: alpha3: pages through the phone. The phone now loads the tablet's w
  8c64199 ckpt 1048: Z8 proven on the emulator (Android 10, GeckoView 160 + Courier in one emulato
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
