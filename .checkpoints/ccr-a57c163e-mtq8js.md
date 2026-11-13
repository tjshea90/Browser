# CHECKPOINT 1470 — read me first, then TASKS.md

**Written:** 2026-11-13T04:44:27Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `c69d647` (this checkpoint is the commit after it)

## Just done
AF1 cause found + fixed + proven in real Gecko on the emulator: Kestrel asked Firefox for AntiTracking.STRICT (766, includes CONTENT = Disconnect's Content list: jsDelivr, unpkg, JW Player jwpcdn.com, Brightcove, Kaltura, Mux, Bitmovin, Vimeo ...) so Firefox blocked those CDNs/players (TRACKING_URI); fix = STRICT minus CONTENT (EtpCategories + test, read-back shows the table without content-track-digest256); also Page report (proven), engine-blocked Diagnostics line, Reload without cache, lab fps fix, tools/sitelab, debug-only pref hook. versionCode 127 / ext 1.0.127 / alpha28 set. Docs: research/DORABY.md, VIDEO_AUDIT s9, EX_RESULTS

## Do this next
run full gates (./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin; cd web-tests && npm test; bash tools/test_handoff.sh; bash tools/check-gecko-prefs.sh), write scratchpad/note28.txt, bash ship.sh gecko "$(cat note28.txt)", dispatch android.yml (inputs app=gecko full_build=true note), wait tag kestrel-g-v0.3.0-alpha28 + green run, record-release.sh, answer the owner (doraby cause+fix, lab numbers meaning, video complaint probably same cause, what to send if still broken) and re-list open items

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/DORABY.md
     M research/EX_RESULTS.md

## Last ten checkpoints
```
  a0403ec ckpt 1458: AF1 diagnostics built (not yet compiled/shipped): extension page log (blocked
  697f62b ckpt 1451: Owner message 20 (their Video lab / Tap lab / Decode lab numbers from the tab
  101b4b1 ckpt 1447: Owner message 19 (doraby.com: no photos, no search bar in Kestrel; Brave fine
  c0c149f ckpt 1443: alpha27 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha27 (gecko code 126), run 
  d107bc4 ckpt 1440: alpha27 (kestrel-g-v0.3.0-alpha27, gecko code 126) gated, pushed and DISPATCH
  f453a50 ckpt 1439: gated kestrel-g-v0.3.0-alpha27 (gecko code 126) and pushed it: handoff self-t
  6b35128 ckpt 1437: AE1 alpha27 built+tested: autoplay fix PROVEN on emulator, DRM prompt proven,
  fd0f98f ckpt 1426: AE1: CAUSE OF STUCK LOADING SYMBOL PROVEN on emulator with alpha26 (muted aut
  08a5ebd ckpt 1410: AE1 research phase done (evidence in summary); re-oriented after compaction; 
  9b93232 ckpt 1409: Owner message 18 (video audit + research + implement) written into TASKS as A
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
