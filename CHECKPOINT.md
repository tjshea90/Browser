# CHECKPOINT 2366 — read me first, then TASKS.md

**Written:** 2026-11-15T02:17:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `c1e8dbd8` (this checkpoint is the commit after it)

## Just done
alpha45 (gecko code 146) RELEASED, RECORDED in BUILDLOG.md (run 43664857352 green, APK on the Release, certificate verified, built from d636a48a) and the heartbeat trig_01KEDB7Qxd8zFeawjGcAkSQZ DELETED (no automatic resume is scheduled now). The owner's earlier two messages (REVIEW-EXTERNAL-3.md, the YouTube 'Video unavailable' screenshot) were re-delivered after a worker restart: both already worked/answered (identical file sha256; same screenshot), re-answered briefly. RX3 job: every row that can be proven without the tablet is done.

## Do this next
NOTHING is running or half-done. The alpha45 owner answer is given in the reply that follows this checkpoint (Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha45). WAIT for the owner: BK5 (Back instant for no-store pages on chosen sites, YES/NO), AX1g (separate system keyboard app YES/NO), tablet feedback on alpha40-45, ONE Diagnostics copy after a Proton Mail session (Blocker line 'Extension listeners (calls, time held)': decides RX3-S2b by the rule of thumb in research/EXT_COST.md section 2) and a Back-lab copy (decides BK3/BK4/M1 per research/BACK_SCROLL.md). When numbers arrive: S2b, BK3, BK4, M1, H1 from them. Still doable WITHOUT the tablet if the owner asks for more: RX3-S4 (blocker histogram cold/warm, counters only, background.js engine.check + the lt field), RX3-D3 (HLS start-up), RX3-Y4 (refresh quick-fixes: design worry = a recompile of ~211k rules on the tablet is unmeasured), a SECOND independent review (fresh agent, give it code not conclusions) of content.js cosmetic/token code and scriptlets-main.js - the first review of alpha44 (research/REVIEW_ALPHA44.md) found 9 real bugs in scripts that run on every page, so ask for a review BEFORE the next page-wide script ships. Release recipe unchanged (bump versionCode 147 + versionName, bash tools/sync-ext-version.sh, gates, bash ship.sh gecko NOTE, dispatch android.yml, record-release.sh AFTER green).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ebd2f1ef ckpt 2365: alpha45 gated (JVM + web 373/373), pushed and dispatched to GitHub (workflow
  d636a48a ckpt 2364: gated kestrel-g-v0.3.0-alpha45 (gecko code 146) and pushed it: handoff self-
  c8a968e7 ckpt 2363: RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchB
  1dea80c7 ckpt 2362: RX3-S2 DONE and ticked (counters only): background.js LST + lstTimed around 
  d90ec3e7 ckpt 2361: alpha44 gated, pushed and dispatched (G4 fake play layers, V3 played element
  02082e19 ckpt 2360: gated kestrel-g-v0.3.0-alpha44 (gecko code 145) and pushed it: handoff self-
  072024e2 ckpt 2359: alpha43 gated, pushed and dispatched to GitHub (V5, D1, S1, B2, G5, G6); wai
  cca67983 ckpt 2358: gated kestrel-g-v0.3.0-alpha43 (gecko code 144) and pushed it: handoff self-
  18b548ee ckpt 2357: pre-ship: alpha43: the glow stops pulsing while a video plays; two more trac
  b7d3f84a ckpt 2356: RX3-S1, B2, G5, G6 DONE and ticked (S1 + G6 emulator-proven, B2 measured wit
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
