# CHECKPOINT 1426 — read me first, then TASKS.md

**Written:** 2026-11-13T03:13:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `24cfa13` (this checkpoint is the commit after it)

## Just done
AE1: CAUSE OF STUCK LOADING SYMBOL PROVEN on emulator with alpha26 (muted autoplay + muted script play() refused: NotAllowedError because TabDelegates denied PERMISSION_AUTOPLAY_INAUDIBLE). Code written, NOT yet built: core VideoPolicy(+test), DeviceVideo probe, GeckoEngine.applyVideo (user-branch prefs: vp9/av1/hevc offer rule + calm pages), TabDelegates autoplay-muted allow + DRM ask, Settings 'Video & page animation' section, Diagnostics Video lab button + decoder/policy lines, labs/video.html + 4 clips, web-tests/videolab.test.js (2 pass), gecko-prefs v7, check-gecko-prefs also checks Kotlin live names

## Do this next
1) ./gradlew :core:test :geckoapp:assembleDebug -PgeckoAbi=x86_64 (log in scratchpad ae1_build.log); 2) install on emulator (ae1_boot.sh already booted emulator-5554, media server :8000 serves scratchpad/site, test page vid/autoplay.html), re-run autoplay page and expect A and C to play; 3) open Diagnostics > Video lab on emulator (check resource:// video playback works, else serve clips another way); 4) write research/VIDEO_AUDIT.md; 5) bump versionCode 126 / 0.3.0-alpha27 / ext 1.0.126, ship.sh gecko, dispatch, record, answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  08a5ebd ckpt 1410: AE1 research phase done (evidence in summary); re-oriented after compaction; 
  9b93232 ckpt 1409: Owner message 18 (video audit + research + implement) written into TASKS as A
  b79c4dd ckpt 1406: alpha26 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha26 (gecko code 125), run 
  5e34d92 ckpt 1404: alpha26 (code 125) gated + pushed (ckpt 1403) and DISPATCHED: android.yml run
  33c7277 ckpt 1403: gated kestrel-g-v0.3.0-alpha26 (gecko code 125) and pushed it: handoff self-t
  bc3596c ckpt 1401: AD1b-3/4 + AD1a-3 DONE: Settings cards + launch screen + Downloads queue UI v
  af1938b ckpt 1388: AD1b-3 UI polish: Settings sections are chamfered cards with dividers (like t
  7cb96e0 ckpt 1384: AD1b-2 mp4 always DONE in core: Hevc.kt (hvc1/hvcC), AudioFrames.kt (AC-3/E-A
  7919d0f ckpt 1373: AD1b-1 download manager DONE in core + UI wiring: Start now / Move up / Move 
  7b80ff7 ckpt 1365: AD1a-1 DONE: link warm-up triggers (finger down + top three search results) i
```

(15 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
