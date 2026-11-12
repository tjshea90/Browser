# CHECKPOINT 741 — read me first, then TASKS.md

**Written:** 2026-11-12T03:03:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `1e71c97` (this checkpoint is the commit after it)

## Just done
M8-lite: Settings > Downloads (speed limit 2 MB/s..250 KB/s applied at once, downloads at the same time 1-3, connections per download auto/1-3) + RateLimiter shared by both engines + tests; tests, lintRelease, compile green. In the tree, NOT shipped (next alpha batches it with whatever the owner's numbers call for; avoid shipping a 92 MB update for a small change)

## Do this next
WAITING ON OWNER: Proton Mail 'Page loads' numbers + the CI list-build OK. When numbers arrive: decide changes (DoH/HTTP3 default, preconnect, page-resize) and ship alpha17 with M8-lite. MEANWHILE only low-risk items: M6 digest paste field, M10, T0. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN (every 2 h).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  06945e0 ckpt 733: Owner told (alpha16 reply: Release link, repeated message 10 answered part by 
  b87f19d ckpt 732: alpha16 SHIPPED + RECORDED (run 43143728373 green; the two earlier runs failed
  dc57dde ckpt 729: stream engine: a dropped job (merged-away sound of a pair, cancelled, cleared)
  9226e37 ckpt 726: TASKS X4/X5/X12 statuses with evidence; HLS auto-resume test; lintRelease clea
  d41caca ckpt 724: M7 content sanity (web page where a file/video was expected -> BAD_CONTENT, HL
  61e1894 ckpt 721: M1b validator-less resume: Sink.readable/pread (Os.pread on both Android sinks
  f17fcb1 ckpt 719: web guard tests fixed (tap reports) + 2 new; CI test-js now gates the Gecko bu
  b1d2872 ckpt 715: Last-Modified splice guard (EngineTest), research/AUDIT-2026-11.md written, do
  7be9a12 ckpt 712: alpha16 dispatched to GitHub Actions (tag kestrel-g-v0.3.0-alpha16 appears whe
  21a22c2 ckpt 711: gated kestrel-g-v0.3.0-alpha16 (gecko code 115) and pushed it: handoff self-te
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
