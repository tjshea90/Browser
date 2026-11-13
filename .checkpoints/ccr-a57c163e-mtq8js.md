# CHECKPOINT 1451 — read me first, then TASKS.md

**Written:** 2026-11-13T04:02:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `41d8a21` (this checkpoint is the commit after it)

## Just done
Owner message 20 (their Video lab / Tap lab / Decode lab numbers from the tablet) written into TASKS as AG1 (AG1a record, AG1b fix the lab fps arithmetic, AG1c decide: decode/screen/JS fine -> network/site, AG1d AVIF keep off). doraby.com (AF1) in progress: real page + our network verdicts + our extension scripts + our cosmetic CSS all render fine in Chromium; local mirror (scratchpad/dora/mirror served on :8001) renders fine in REAL Gecko on the emulator (icons + posters) once its protocol-relative links are local

## Do this next
AF1a: the owner's tablet differs from every test so far, so instrument instead of guessing: (1) read how the app handles zstd/HTTP3/ECH/DoH for Cloudflare sites and the Fast Wi-Fi VPN profile (NetProfile), (2) add a 'This page' report to Diagnostics from the extension (webRequest.onErrorOccurred + blocked counts per tab + window error / resource error events from content.js) so the owner can copy what failed on doraby.com, (3) lab fps fix AG1b, (4) answer both messages (AF1 + AG1) in plain words

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  101b4b1 ckpt 1447: Owner message 19 (doraby.com: no photos, no search bar in Kestrel; Brave fine
  c0c149f ckpt 1443: alpha27 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha27 (gecko code 126), run 
  d107bc4 ckpt 1440: alpha27 (kestrel-g-v0.3.0-alpha27, gecko code 126) gated, pushed and DISPATCH
  f453a50 ckpt 1439: gated kestrel-g-v0.3.0-alpha27 (gecko code 126) and pushed it: handoff self-t
  6b35128 ckpt 1437: AE1 alpha27 built+tested: autoplay fix PROVEN on emulator, DRM prompt proven,
  fd0f98f ckpt 1426: AE1: CAUSE OF STUCK LOADING SYMBOL PROVEN on emulator with alpha26 (muted aut
  08a5ebd ckpt 1410: AE1 research phase done (evidence in summary); re-oriented after compaction; 
  9b93232 ckpt 1409: Owner message 18 (video audit + research + implement) written into TASKS as A
  b79c4dd ckpt 1406: alpha26 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha26 (gecko code 125), run 
  5e34d92 ckpt 1404: alpha26 (code 125) gated + pushed (ckpt 1403) and DISPATCHED: android.yml run
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
