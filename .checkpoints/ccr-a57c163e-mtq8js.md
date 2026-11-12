# CHECKPOINT 1103 — read me first, then TASKS.md

**Written:** 2026-11-12T16:59:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `141bbfc` (this checkpoint is the commit after it)

## Just done
AA4: K1 K2 K5 K6 verified on the emulator (evidence in research/SPEED3.md 6.1): 5 live tabs of 26 at 1.4 GB free, trims 15/20/40 close nothing, trim 60 keeps only the current tab; page B + scroll + Back stack survived force-stop (tabstate json has no formdata); cover preview 'Opening host' shows on cold start and revive; link launch no longer loads the saved tab. Added cover-show/hide timeline lines and the about:blank page-stop guard

## Do this next
Next in the speed batch: (1) Back picture for pages the engine must reload (no-store): BackPicture map url->480px RGB_565 in BrowserActivity, fed by captureThumb, shown via RestoreOverlay 150 ms after goBack/goForward when the tab is still loading, hidden by firstPaint/page-stop/timeout; (2) K7 adaptive download cap while a page loads (DownloadController.pageLoading); (3) K8 init after the first frame; (4) Settings row 'Tabs kept ready' visual check; (5) bump versionCode 121 / 0.3.0-alpha22, tools/sync-ext-version.sh, ship.sh gecko, dispatch android.yml, record-release.sh; (6) answer owner message 14 incl. the 4 relayed questions; then AA3-2.. in order. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/SPEED3.md

## Last ten checkpoints
```
  084b5ba ckpt 1101: AA4 speed batch code step: K1 LiveBudget (core, 9 tests) + RAM-adaptive live 
  6fc25a2 ckpt 1077: Reviews saved: research/PERF_KOTLIN.md, research/PERF_JS.md, research/PROMPT_
  7ceb366 ckpt 1075: AA3 audit done by an independent read-only agent: research/PROMPT_AUDIT.md (2
  9c34e86 ckpt 1072: AA1 proven on the emulator (simulated VPN: Network line, trr=0, http3 off, ti
  a0a56be ckpt 1069: AA2 + AA1 core done: Courier closed in the browser (every option OFF by defau
  ac00a1f ckpt 1061: Owner message 14 written into TASKS.md as AA1-AA4 in their words (INBOX has i
  d75864a ckpt 1057: Owner message 13 ANSWERED and SHIPPED: Kestrel Courier alpha3 (kestrel-courie
  5c2891c ckpt 1054: Docs: CLAUDE.md + BRIEF.md record the owner's unmetered/5 Mbit/s facts and th
  95c9050 ckpt 1053: gated kestrel-g-v0.3.0-alpha21 (gecko code 120) and pushed it: handoff self-t
  2a514af ckpt 1052: gated kestrel-courier-v0.1.0-alpha3 (courier code 3) and pushed it: handoff s
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
