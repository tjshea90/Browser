# CHECKPOINT 2988 — read me first, then TASKS.md

**Written:** 2026-11-22T08:30:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `88462d22` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha94 (gecko code 195) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-144aedec-7apwin, inputs {"app":"gecko","full_build":"true","note":"alpha94: the second film that never played. Your upload shows what happened: the first film went to Kestrel's player, but on the second film nothing asked Kestrel's player at all: only 1 question reached the app while 3 taps opened a player. The gate asks Kestrel's player only when the thing you tap looks like a play button by its name, and a tap on the picture itself or on the small play mark that alpha93 puts in place of the stuck circle was not asked about, so the page's own player opened, loaded for about a second and stalled, exactly what you saw. Now a tap on the picture of a held page player that hits no control (the film itself, a plain mark) asks Kestrel's player first, and the play mark is labelled as a play button; Mute, Share, links, looping clips and films with a file of their own are left alone as before. From your upload too: the Safe drawing settings did reach the engine (the engine line shows scissored clears, batched uploads and partial present all off), and the loading circle on the pornhub player is a 24 pixel svg with 145 pixels of room inside its parent, so no page box cuts it off: the clipped circle is not page layout. Not run on the tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha94), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha94 "alpha94: the second film that never played. Your upload shows what happened: the first film went to Kestrel's player, but on the second film nothing asked Kestrel's player at all: only 1 question reached the app while 3 taps opened a player. The gate asks Kestrel's player only when the thing you tap looks like a play button by its name, and a tap on the picture itself or on the small play mark that alpha93 puts in place of the stuck circle was not asked about, so the page's own player opened, loaded for about a second and stalled, exactly what you saw. Now a tap on the picture of a held page player that hits no control (the film itself, a plain mark) asks Kestrel's player first, and the play mark is labelled as a play button; Mute, Share, links, looping clips and films with a file of their own are left alone as before. From your upload too: the Safe drawing settings did reach the engine (the engine line shows scissored clears, batched uploads and partial present all off), and the loading circle on the pornhub player is a 24 pixel svg with 145 pixels of room inside its parent, so no page box cuts it off: the clipped circle is not page layout. Not run on the tablet."
NEVER ship kestrel-g-v0.3.0-alpha94 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6fcdd4e1 ckpt 2986: pre-ship: alpha94: the second film that never played. Your upload shows what
  ae98cd8b ckpt 2978: PERF9 written to TASKS.md (owner after alpha93: one video worked, another sh
  c97f7f67 ckpt 2977: alpha93 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha93 (gecko code 194, run
  a882ac50 ckpt 2974: alpha93 (gecko code 194) gated by ship.sh and ONE dispatch made on ccr-144ae
  5488bb85 ckpt 2973: gated kestrel-g-v0.3.0-alpha93 (gecko code 194) and pushed it: handoff self-
  bf393943 ckpt 2971: pre-ship: alpha93: quality button in the full-screen player, the stuck loadi
  335cc029 ckpt 2968: PERF8 code (not released): (a) VideoFlow.offerSiblings gives the player that
  06774fc1 ckpt 2959: PERF8 written to TASKS.md (owner after alpha92: fullscreen Kestrel player wo
  f41f8f8b ckpt 2958: alpha92 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha92 (gecko code 193, run
  292067e6 ckpt 2955: alpha92 (gecko code 193) gated by ship.sh (core+app suites, lint, handoff se
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
