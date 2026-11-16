# CHECKPOINT 2461 — read me first, then TASKS.md

**Written:** 2026-11-16T02:21:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `9db97c74` (this checkpoint is the commit after it)

## Just done
owner message 51 (the real tablet's Diagnostics of alpha55): read in research/TABLET_DIAG_2026-11-16.md, TASKS rows ANS51a-g written before code (a done, b-g open); findings: auto-sideways never fired (picture 0x0 at entry, player.js knew 1920x1080 2.3 s later), turn stats lost the slow page (0 ms / 0 measured), watchdog 3.5 s ended+restarted the session 4 times, page script silent 3.5-14 s after turns, UI thread blocked 0.9-2 s at each turn

## Do this next
ANS51b: in BrowserActivity.playerMessage (post to main) set tab.gecko.pictureW/H from the report's w/h (picturePlayed=true) and call fullscreenPictureKnown(); then ANS51d (PlayerController.SILENT_END_MS 12000 + test), ANS51c (TurnSettleMachine silent + late first report stats + tests), ANS51e (player.js lt/pc/tf/df -> background.js relay -> PlayerController -> Diagnostics + Real video), ANS51f (Health turn ui), then kp lab run, versionCode 157 / 0.3.0-alpha56, ship.sh gecko, record-release, answer owner; heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv; compat run 43784927532 read at 03:30Z (trig_019ZZh4wE2UHrvMLbwxEFmrV) into research/COMPAT_RUN1.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  36982a2b ckpt 2460: stuck-screen safety net (review F1 beyond the Settings toggle): the app's an
  0c425e84 ckpt 2459: owner message 50 (the alpha51 half of message 49 sent again: 'some of the fi
  7ee3a4a1 ckpt 2458: alpha55 RELEASED and recorded: Release kestrel-g-v0.3.0-alpha55 (run 4378408
  90d4d6e8 ckpt 2457: gated kestrel-g-v0.3.0-alpha55 (gecko code 156) and pushed it: handoff self-
  6d36261e ckpt 2456: independent review of the alpha55 code (agent, 12 findings) ALL addressed wi
  1bf5e48d ckpt 2455: lab fsy run 43778698760: kp scene 22/22 ok again, sound ok, Yahoo live playe
  8eab718d ckpt 2454: kp lab run 5 (43778006356, 512 MB heap boot): 22 of 22 checks GREEN on the r
  b74a874d ckpt 2453: kp lab run 3 (43776782679, good boot 512 MB heap): 18 of 21 checks pass on t
  4b0bd9aa ckpt 2452: kp lab run 2 (43775890786) read: it says NOTHING about the controls: the emu
  05a26786 ckpt 2451: VP1 lab scene kp fixed after the first real-engine run (soak.py kp_scene: pa
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
