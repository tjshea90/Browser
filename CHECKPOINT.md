# CHECKPOINT 2356 — read me first, then TASKS.md

**Written:** 2026-11-15T00:10:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `fde464f7` (this checkpoint is the commit after it)

## Just done
RX3-S1, B2, G5, G6 DONE and ticked (S1 + G6 emulator-proven, B2 measured with the real lists, G5 JVM-proven); alpha43 gates green, ready to ship

## Do this next
HEARTBEAT trig_01KEDB7Qxd8zFeawjGcAkSQZ (every 2 h; get_trigger to read; delete_trigger when the RX3 job is shipped). alpha41+alpha42 RELEASED and ANSWERED. DONE since alpha42 and NOT shipped yet: V5 (AdMarkers/AdRuns/early look), D1 (outage wait), S1 (glow holds still while a video plays), B2 (extra tracker lists + Settings switch), G6 (jump to a listed ad host stopped at any delay, emulator-proven), G5 (new-window wall with tap evidence + AUTH mirror): all ticked in TASKS.md. NEXT STEP = SHIP alpha43: (1) wait for the web gate / run the gates once more if anything changed; (2) edit geckoapp/build.gradle.kts versionCode 144 + versionName 0.3.0-alpha43, run bash tools/sync-ext-version.sh; (3) write the note to a scratchpad file: "alpha43: the glow stops pulsing while a video plays; two more tracker block lists (AdGuard Tracking Protection and HaGeZi Light, new switch Settings > Filter lists > Extra tracker lists, on by default; the first update downloads about 3 MB and rebuilds the lists); a page jump to an advertising site is now stopped even long after your last tap, and a new window opened by a button instead of a link needs one tap on the small blocked-window icon (sign-in windows are not affected); downloads wait out a lost connection instead of failing; video downloads leave out advertising breaks the stream's own list gives away, and live, protected and clip-sized streams no longer count for the download badge. Tested on the emulator and in test browsers, not on your tablet."; (4) bash ship.sh gecko "<note>", then mcp__github__actions_run_trigger run_workflow android.yml ref claude/new-session-7d9rxz inputs app=gecko full_build=true note=<same note>; wait for tag kestrel-g-v0.3.0-alpha43 + all jobs success + get_release_by_tag APK (a courier/timing flake -> rerun_failed_jobs, never a new dispatch); bash tools/record-release.sh kestrel-g-v0.3.0-alpha43 "<same note>"; answer the owner in plain words with the Release link on its own line, say what was proven on the emulator vs NOT on the tablet, WARN that the first list update after install downloads ~3 MB and compiles 210k rules on the tablet (if lists stop updating: Settings > Filter lists > Extra tracker lists OFF) and re-list open items (BK5 question, AX1g, tablet feedback alpha40-43). THEN: G4 (fake play layers, content.js/guard-main.js, web test first), V3 (which element plays), Wave C rows. Before writing any research/*.md check that it does not exist (I overwrote DOWNLOAD_MANAGERS.md once and restored it from git).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  822e687f ckpt 2355: RX3-V5 and RX3-D1 DONE (ticked): AdMarkers/AdRuns/early look and the outage 
  dec2df9d ckpt 2354: alpha42 shipped to GitHub (gated + pushed + android.yml dispatched): G7 + V1
  652f583c ckpt 2353: gated kestrel-g-v0.3.0-alpha42 (gecko code 143) and pushed it: handoff self-
  feda2b32 ckpt 2352: RX3-V1 DONE (ticked) and RX3-V2 coded + proven on the emulator (teaser-link 
  21cda298 ckpt 2351: RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-e
  265fcf6e ckpt 2350: RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/r
  69fcfcf1 ckpt 2349: RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools
  d10a5f4a ckpt 2348: alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test a
  ff61cb61 ckpt 2347: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  79518152 ckpt 2346: first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpen
```

(27 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
