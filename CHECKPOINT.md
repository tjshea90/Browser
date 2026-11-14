# CHECKPOINT 2355 — read me first, then TASKS.md

**Written:** 2026-11-14T23:41:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `399d766b` (this checkpoint is the commit after it)

## Just done
RX3-V5 and RX3-D1 DONE (ticked): AdMarkers/AdRuns/early look and the outage wait in both download engines; full core suite + app compile green; alpha43 not shipped yet

## Do this next
HEARTBEAT trig_01KEDB7Qxd8zFeawjGcAkSQZ (every 2 h; get_trigger to read; delete_trigger when the RX3 job is shipped). alpha41 AND alpha42 are RELEASED + RECORDED + ANSWERED (alpha42 note: G7 other-player ad skipping, V1 decoys not counted, V2 markup links verified). The next release is alpha43 (versionCode 144, NOT bumped yet: edit geckoapp/build.gradle.kts, run bash tools/sync-ext-version.sh, then bash ship.sh gecko "<note>", then dispatch android.yml app=gecko full_build=true ref claude/new-session-7d9rxz with the same note, wait for tag kestrel-g-v0.3.0-alpha43 + all jobs success + get_release_by_tag APK, bash tools/record-release.sh, answer the owner in plain words with the Release link). DONE since alpha42 and NOT yet shipped: V5 (AdMarkers + Hls Seg.ad + AdRuns + StreamEngine pre-seed + early look: ticked), D1 (outage wait in StreamEngine and Engine + StreamEngine.tick: ticked, JVM-proven only). NEXT, in this order (owner-asked first): S1 hold the glow while a video plays (GeckoSession.setMediaSessionDelegate -> Tab.mediaPlaying -> Neon.busy in BrowserActivity.updateNeonBusy; MediaSession.Delegate exists in the AAR: onActivated/onDeactivated/onPlay/onPause/onStop; pure busy-rule unit test; known limit: only audible media opens a media session), then B2 (AdGuard Tracking Protection + Hagezi Light as FilterUpdater sources, Settings row 'Extra tracker lists' default ON, verify each URL/licence first, docs/FILTER-LISTS.md, DefaultListsRegressionTest), G5 + G6 (PopupGuard app wall uses the tap evidence; late scripted jump to a listed ad host), G4 (fake play layers), V3 (which element plays), then Wave C. Always: claim (bash tools/claim.sh RX3-<id>), failing test first, ckpt after every step, check that a research/*.md exists BEFORE writing it (I once overwrote research/DOWNLOAD_MANAGERS.md and restored it from git history). Tablet-only checks are still the owner's: say so in every answer.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  dec2df9d ckpt 2354: alpha42 shipped to GitHub (gated + pushed + android.yml dispatched): G7 + V1
  652f583c ckpt 2353: gated kestrel-g-v0.3.0-alpha42 (gecko code 143) and pushed it: handoff self-
  feda2b32 ckpt 2352: RX3-V1 DONE (ticked) and RX3-V2 coded + proven on the emulator (teaser-link 
  21cda298 ckpt 2351: RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-e
  265fcf6e ckpt 2350: RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/r
  69fcfcf1 ckpt 2349: RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools
  d10a5f4a ckpt 2348: alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test a
  ff61cb61 ckpt 2347: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  79518152 ckpt 2346: first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpen
  d6379d16 ckpt 2345: alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml
```

(25 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
