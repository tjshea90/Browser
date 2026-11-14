# CHECKPOINT 2343 — read me first, then TASKS.md

**Written:** 2026-11-14T21:29:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `ee766db9` (this checkpoint is the commit after it)

## Just done
RX3-G1 DONE (guard+library declared in the manifest, world MAIN; emulator-proven: page-world marker at the first script, about:blank frames covered, dynamic registrations removed, handoff gate + tests), RX3-G2 DONE (web tests), RX3-Y1 DONE for the mechanics (caps 4096/8192 trusted, 64 calls/32000 chars, json-edit family + trusted-edit-inbound-object + trusted-prevent-dom-bypass, JSON edit engine in scriptlets-main.js with 19 web tests, scriptlet accounting line in Diagnostics) - NOT yet proven on real YouTube. Version bumped to alpha41 (gecko code 142, manifest 1.0.142) in the working tree (NOT shipped). GUARD_FIRST_PAGE.md, GUARD_HARDENING.md, YOUTUBE_ADS.md section 3, BACK_SCROLL.md written. G3 relay in the REAL engine is still UNPROVEN: the emulator assist lab (/assist-parent with a cross-origin frame) showed the layer tap and the refused pop-up but NO 'child play() called': added assist counters (background.js assistText in the st state line, ShieldsBridge logs 'shields ext-state' to Health) and re-running the lab

## Do this next
(1) read scratchpad assist2.out (lab re-run after pm clear): look for the 'shields ext-state' Health line, it ends with ' | assist <sent>/<asked> (<reason>)': reason 'tabs.sendMessage is not there' means GeckoView has no tabs.sendMessage -> redesign the relay (candidates: browser.tabs.sendMessage absent -> use a port: the child content.js opens runtime.connect and the background keeps ports per frameId, or content.js broadcasts through the background to all frames with an address match); 'frame not found among N' means the frameParent table lacked the sub_frame (the iframe request may not reach onBeforeRequest as sub_frame with parentFrameId); no assist line at all means the parent content.js never sent it (check press recording / elementsFromPoint). (2) fix, re-run web tests (assist.test.js models the relay), rebuild x86_64 (-PgeckoAbi=x86_64), pm clear, run scratchpad assist_run.sh. (3) THEN tick RX3-G3, run the whole gate: ./gradlew :core:test :app:testDebugUnitTest, cd web-tests && npm test, bash tools/test_handoff.sh, then ship alpha41: bash ship.sh gecko note (version already bumped), dispatch android.yml (app=gecko full_build=true ref claude/new-session-7d9rxz), record-release after green; owner note must say: Back scroll + Back lab, trusted-only scriptlets, forged-tap fix, frame-assist channel, guard from the first page, JSON-edit engine; NOT run on the tablet

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e462351b ckpt 2342: RX3-G3 DONE (script side): the cross-frame postMessage assist channel is gon
  279f3b45 ckpt 2341: RX3-BK1/BK2 first emulator proof (static case): page left at y=1800, Back re
  8888c78b ckpt 2340: RX3-BK1/BK2 Kotlin wiring DONE and COMPILES (geckoapp + app debug): ShieldsB
  687414dd ckpt 2339: owner message 40 (a screenshot, no words: YouTube Movies and TV page 'Video 
  2d3b6a3a ckpt 2338: RX3-BK1/BK2 core logic DONE and green: core/session/BackScroll.kt (ScrollMem
  13adf44e ckpt 2334: saved research/REVIEW-EXTERNAL-3.md (v2, byte-identical copy of the owner's 
  094aed9b ckpt 2337: alpha40 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha40 (gecko code 141, run
  e2056d0c ckpt 2333: alpha40 (gecko code 141) gated, pushed and the android.yml run DISPATCHED (a
  13e4374a ckpt 2325: gated kestrel-g-v0.3.0-alpha40 (gecko code 141) and pushed it: handoff self-
  917a3327 ckpt 2320: AY2 final: content.js treats only a NEW non-empty selection as text (emulato
```

(32 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
