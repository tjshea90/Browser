# CHECKPOINT 1409 — read me first, then TASKS.md

**Written:** 2026-11-13T02:49:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ab7af60` (this checkpoint is the commit after it)

## Just done
Owner message 18 (video audit + research + implement) written into TASKS as AE1 (AE1a audit, AE1b stuck loading symbol, AE1c choppy video, AE1d research+implement, AE1e ship alpha27)

## Do this next
Read geckoapp/src/main/assets/gecko-prefs.txt, GeckoEngine media/animation prefs (ui.prefersReducedMotion, image.animation_mode, media.*), extension content.js/guard-main.js/embed code touching <video>, TabDelegates onFullScreen/media; then research (WebSearch) Gecko Android MediaCodec/WebRender on Mali-G52/MT8168 and H.264-vs-VP9 selection; write research/VIDEO_AUDIT.md; implement Diagnostics > Video lab + MediaCodecList/GL probe + fixes

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  b79c4dd ckpt 1406: alpha26 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha26 (gecko code 125), run 
  5e34d92 ckpt 1404: alpha26 (code 125) gated + pushed (ckpt 1403) and DISPATCHED: android.yml run
  33c7277 ckpt 1403: gated kestrel-g-v0.3.0-alpha26 (gecko code 125) and pushed it: handoff self-t
  bc3596c ckpt 1401: AD1b-3/4 + AD1a-3 DONE: Settings cards + launch screen + Downloads queue UI v
  af1938b ckpt 1388: AD1b-3 UI polish: Settings sections are chamfered cards with dividers (like t
  7cb96e0 ckpt 1384: AD1b-2 mp4 always DONE in core: Hevc.kt (hvc1/hvcC), AudioFrames.kt (AC-3/E-A
  7919d0f ckpt 1373: AD1b-1 download manager DONE in core + UI wiring: Start now / Move up / Move 
  7b80ff7 ckpt 1365: AD1a-1 DONE: link warm-up triggers (finger down + top three search results) i
  ac0f553 ckpt 1353: Owner message 17 written into TASKS as AD1 (AD1a review rows, AD1b earlier AA
  16fc889 ckpt 1350: alpha25 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha25 (gecko code 124), run 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
