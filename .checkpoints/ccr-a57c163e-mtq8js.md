# CHECKPOINT 1458 — read me first, then TASKS.md

**Written:** 2026-11-13T04:12:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `43d5ece` (this checkpoint is the commit after it)

## Just done
AF1 diagnostics built (not yet compiled/shipped): extension page log (blocked/failed/HTTP>=400/script errors per tab, 'rep' push 4 s after the last news, max 3 per page), content.js error reporter (max 8 per frame), core PageReports + test, ShieldsBridge 'rep' handler (drops private tabs), Diagnostics 'Page report' block; web tests 33 background + 37 content pass. Real-Gecko emulator proof: local mirror of doraby.com with cdn.jsdelivr.net failing shows EXACTLY the owner's symptom (no photos, no menu/search icons)

## Do this next
1) ./gradlew :core:test :geckoapp:compileDebugKotlin; 2) lab fps fix (video.html: measure from first to last presented picture, judge by dropped + gaps); 3) menu item 'Reload without cache' (GeckoSession.LOAD_FLAGS_BYPASS_CACHE); 4) build debug x86_64, install, load http://10.0.2.2:8002/ (mirror with jsDelivr failing: python3 dora_srv.py dora/mirror 8002 /_cdn/cdn.jsdelivr.net) and read Diagnostics > Page report: expect FAILED lines for the jsDelivr files and 'Swiper is not defined'; 5) research/DORABY.md + TASKS ticks; 6) alpha28 only with versionCode 127 / extension 1.0.127; answer owner (doraby + lab numbers)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  697f62b ckpt 1451: Owner message 20 (their Video lab / Tap lab / Decode lab numbers from the tab
  101b4b1 ckpt 1447: Owner message 19 (doraby.com: no photos, no search bar in Kestrel; Brave fine
  c0c149f ckpt 1443: alpha27 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha27 (gecko code 126), run 
  d107bc4 ckpt 1440: alpha27 (kestrel-g-v0.3.0-alpha27, gecko code 126) gated, pushed and DISPATCH
  f453a50 ckpt 1439: gated kestrel-g-v0.3.0-alpha27 (gecko code 126) and pushed it: handoff self-t
  6b35128 ckpt 1437: AE1 alpha27 built+tested: autoplay fix PROVEN on emulator, DRM prompt proven,
  fd0f98f ckpt 1426: AE1: CAUSE OF STUCK LOADING SYMBOL PROVEN on emulator with alpha26 (muted aut
  08a5ebd ckpt 1410: AE1 research phase done (evidence in summary); re-oriented after compaction; 
  9b93232 ckpt 1409: Owner message 18 (video audit + research + implement) written into TASKS as A
  b79c4dd ckpt 1406: alpha26 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha26 (gecko code 125), run 
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
