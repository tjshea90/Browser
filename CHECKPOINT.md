# CHECKPOINT 1437 — read me first, then TASKS.md

**Written:** 2026-11-13T03:37:15Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `297ec57` (this checkpoint is the commit after it)

## Just done
AE1 alpha27 built+tested: autoplay fix PROVEN on emulator, DRM prompt proven, Video lab runs, Settings>Video, Diagnostics decoders; JVM gates green; 116 web tests; research/VIDEO_AUDIT.md + EX_RESULTS alpha27 written; versionCode 126 / 0.3.0-alpha27

## Do this next
bash ship.sh gecko "<note from scratchpad/note27.txt>" then dispatch android.yml (inputs app=gecko full_build=true note) on ref ccr-a57c163e-mtq8js, wait for tag kestrel-g-v0.3.0-alpha27 + green run, get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha27 note, answer owner in plain words (Release link on its own line) + ask for the site/URL of the choppy video + re-list open owner items

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  fd0f98f ckpt 1426: AE1: CAUSE OF STUCK LOADING SYMBOL PROVEN on emulator with alpha26 (muted aut
  08a5ebd ckpt 1410: AE1 research phase done (evidence in summary); re-oriented after compaction; 
  9b93232 ckpt 1409: Owner message 18 (video audit + research + implement) written into TASKS as A
  b79c4dd ckpt 1406: alpha26 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha26 (gecko code 125), run 
  5e34d92 ckpt 1404: alpha26 (code 125) gated + pushed (ckpt 1403) and DISPATCHED: android.yml run
  33c7277 ckpt 1403: gated kestrel-g-v0.3.0-alpha26 (gecko code 125) and pushed it: handoff self-t
  bc3596c ckpt 1401: AD1b-3/4 + AD1a-3 DONE: Settings cards + launch screen + Downloads queue UI v
  af1938b ckpt 1388: AD1b-3 UI polish: Settings sections are chamfered cards with dividers (like t
  7cb96e0 ckpt 1384: AD1b-2 mp4 always DONE in core: Hevc.kt (hvc1/hvcC), AudioFrames.kt (AC-3/E-A
  7919d0f ckpt 1373: AD1b-1 download manager DONE in core + UI wiring: Start now / Move up / Move 
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
