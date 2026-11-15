# CHECKPOINT 2395 — read me first, then TASKS.md

**Written:** 2026-11-15T16:20:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `247eb41c` (this checkpoint is the commit after it)

## Just done
independent review of the alpha48 diff read (research/REVIEW_ALPHA48.md, 9 findings RV1-RV9 in TASKS.md: 3 HIGH: site orientation lock decoded in the wrong number space, full screen not ended when the session dies (and Back then does nothing), look-alike APK refusal spoofable by file name + size); VC1/LP1 started: research/VIDEO_CONTROLS.md (cause: Yahoo's player is a shadow-root web component, all Kestrel video code is blind to shadow DOM), new content scripts vfind.js + videobar.js written (not wired yet)

## Do this next
wire the new scripts: manifest.json (vfind.js before content.js; videobar.js entry), background.js (guard answer vb/vba, ft relay, vbk relay), content.js LP1 (KestrelVF.at in both long-press handlers, sub-frame ask before os), ShieldsBridge flags 64/128 + vbk, Prefs.videoControls + Settings row, web-tests/videobar.test.js; then fix RV1-RV9, then lab run 5 artifacts (43731411440), then ship alpha48 with VC1/LP1 in it

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
    ?? research/REVIEW_ALPHA48.md

## Last ten checkpoints
```
  a74e3649 ckpt 2394: wrote owner message 44 into TASKS.md in the owner's words: VC1 (video has no
  9ae7442b ckpt 2393: re-dispatched the one-hour hotspot run WITH the lab-server shaping: android.
  13ac03b9 ckpt 2392: lab server follows the hotspot too: soaklab.py LAB_RATE_KBPS / LAB_DELAY_MS 
  9f97e3b4 ckpt 2391: gates for alpha48 are green: web tests 418/418 (npm test, final scripts), JV
  a7ef675c ckpt 2390: hotspot-shaped network option for the lab (--net hotspot = 5 Mbit/s each way
  64223f1f ckpt 2389: run 5 (all, 20 min) = android.yml run 43731411440 in progress; run 6 (all, 6
  a5ac2866 ckpt 2388: lab runs 3+4 read (artifacts via tools/emu/fetch_artifact.py): FS1 WORKS on 
  b486edd4 ckpt 2387: added: Back leaves full screen first (handleBack: exitFullScreen), RX4-X2 re
  e29a9127 ckpt 2386: RX4-M0e done (opt-in 'Measure scrolling on real pages': Settings toggle defa
  4a1e86b2 ckpt 2385: lab smoke run 2 GREEN (run 43684548711: KVM ok, emulator boots in 30 s, Geck
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
