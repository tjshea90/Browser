# CHECKPOINT 856 — read me first, then TASKS.md

**Written:** 2026-11-12T05:30:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ba52b47` (this checkpoint is the commit after it)

## Just done
post-alpha17 in the tree (NOT shipped): Make MP4 for finished .ts rows (StreamEngine.makeMp4, Action.MAKE_MP4, DownloadsScreen button, service kept alive while converting, 4 engine tests, emulator UI wiring checked), unmarked-splice detection, clearer refusal wording (H.265 (HEVC), not H.264; the writer's reason kept over the platform's), debug switch debug.kestrel.rawts, two-tab media attribution fix (X20, reproduced + fixed on emulator), stress corpus. All suites + lint green. alpha17 itself is RELEASED + recorded in BUILDLOG (run 43157262742, tag kestrel-g-v0.3.0-alpha17).

## Do this next
Answer the owner NOW (message 11 + 'remux only' remark) with the alpha17 Release link on its own line: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha17 - say what is verified (emulator only) and what is not (tablet, real sites). Then wait for the owner's feedback before alpha18 (bundle: Make MP4, splice detection, X20, plus whatever the owner reports). Open from the owner: Proton Mail 'Page loads' numbers (+DoH off, HTTP/3 off), keyboard/accessibility/autofill/5GHz facts, yes/no on a scheduled CI list-build workflow. Backlog: X3 rest (M6 digest paste field, M8 priorities, M9, M10, T0), X5 scriptlets, X12 typing path, X19c/d/f, P5, U1c. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN (4-hourly).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  0948a0d ckpt 843: post-dispatch hardening (NOT in alpha17): TsToMp4.peekFormatChange (unmarked s
  376dc8d ckpt 837: alpha17 DISPATCHED: android.yml run 43157262742 (run_number 60) on ccr-a57c163
  2726199 ckpt 836: gated kestrel-g-v0.3.0-alpha17 (gecko code 116) and pushed it: handoff self-te
  7f4771d ckpt 835: pre-ship: alpha17: the download button is now an icon in the bottom bar (gray 
  791bba1 ckpt 833: X15 + X18 DONE and emulator-proven (alpha-debug x86_64, Android 10): dock popu
  4f4235e ckpt 826: X18b DONE: StreamEngine writes TS HLS straight into an MP4 (decideMux probes s
  70b4d0b ckpt 804: X18a DONE (core, JVM-tested, ffmpeg-verified): Avc.kt (SPS/NAL), TsDemux.kt (s
  1b3baa2 ckpt 756: X17 dock download icon VERIFIED on emulator after a real redraw (home+relaunch
  51942ec ckpt 744: owner message 11 written into TASKS as X14-X18 in their words (long-press down
  e8ca3f6 ckpt 741: M8-lite: Settings > Downloads (speed limit 2 MB/s..250 KB/s applied at once, d
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
