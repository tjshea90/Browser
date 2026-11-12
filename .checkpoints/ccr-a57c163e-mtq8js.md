# CHECKPOINT 1075 — read me first, then TASKS.md

**Written:** 2026-11-12T16:00:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `2df4fb3` (this checkpoint is the commit after it)

## Just done
AA3 audit done by an independent read-only agent: research/PROMPT_AUDIT.md (25 owner messages, per-request status, 11 gaps, 18 owner-only items, 4 unanswered questions); every gap is now a box AA3-1..11 in TASKS.md

## Do this next
Wait for the Kotlin perf review (perf-kotlin.md) and the extension JS review (perf-js.md) in the scratchpad; then AA3-1/AA4 first (live-session limit by real available memory, bfcache viewers 6, Back picture for no-store pages via the existing preview/RestoreOverlay, confirmed UI-thread fixes), then AA3-2 .. AA3-11 in order, shipping alpha22 after the speed batch. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN: set to every 2 h with 'continue the AA3 list one step per wake' once the speed batch is shipped

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9c34e86 ckpt 1072: AA1 proven on the emulator (simulated VPN: Network line, trr=0, http3 off, ti
  a0a56be ckpt 1069: AA2 + AA1 core done: Courier closed in the browser (every option OFF by defau
  ac00a1f ckpt 1061: Owner message 14 written into TASKS.md as AA1-AA4 in their words (INBOX has i
  d75864a ckpt 1057: Owner message 13 ANSWERED and SHIPPED: Kestrel Courier alpha3 (kestrel-courie
  5c2891c ckpt 1054: Docs: CLAUDE.md + BRIEF.md record the owner's unmetered/5 Mbit/s facts and th
  95c9050 ckpt 1053: gated kestrel-g-v0.3.0-alpha21 (gecko code 120) and pushed it: handoff self-t
  2a514af ckpt 1052: gated kestrel-courier-v0.1.0-alpha3 (courier code 3) and pushed it: handoff s
  d803d90 ckpt 1051: pre-ship: alpha3: pages through the phone. The phone now loads the tablet's w
  8c64199 ckpt 1048: Z8 proven on the emulator (Android 10, GeckoView 160 + Courier in one emulato
  060b94e ckpt 1042: Z8 tablet side + Z1 unmetered built and unit-tested: ProxyLink-driven PagePro
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
