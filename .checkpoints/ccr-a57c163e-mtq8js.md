# CHECKPOINT 1440 — read me first, then TASKS.md

**Written:** 2026-11-13T03:39:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f453a50` (this checkpoint is the commit after it)

## Just done
alpha27 (kestrel-g-v0.3.0-alpha27, gecko code 126) gated, pushed and DISPATCHED: android.yml run 43360306667 (run #77, started 03:38Z, ~7 min)

## Do this next
wait for tag kestrel-g-v0.3.0-alpha27 (git ls-remote origin refs/tags/kestrel-g-v0.3.0-alpha27) and a completed/success run (actions_list list_workflow_runs android.yml perPage 1), get_release_by_tag shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha27 "$(cat scratchpad/note27.txt)" ONLY after green; then answer the owner in plain words with the Release link on its own line: what was wrong (muted autoplay refused), what changed (Settings > Video, Video lab, decoders in Diagnostics, DRM ask), ask for the site/URL of the choppy video + Video lab results + whether the symbol appears on page open or at play, mention optional backlog (ad-SDK stand-ins, fMP4 defragmenter, background audio, PiP) and re-list open owner items; tick AE1e in TASKS.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f453a50 ckpt 1439: gated kestrel-g-v0.3.0-alpha27 (gecko code 126) and pushed it: handoff self-t
  6b35128 ckpt 1437: AE1 alpha27 built+tested: autoplay fix PROVEN on emulator, DRM prompt proven,
  fd0f98f ckpt 1426: AE1: CAUSE OF STUCK LOADING SYMBOL PROVEN on emulator with alpha26 (muted aut
  08a5ebd ckpt 1410: AE1 research phase done (evidence in summary); re-oriented after compaction; 
  9b93232 ckpt 1409: Owner message 18 (video audit + research + implement) written into TASKS as A
  b79c4dd ckpt 1406: alpha26 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha26 (gecko code 125), run 
  5e34d92 ckpt 1404: alpha26 (code 125) gated + pushed (ckpt 1403) and DISPATCHED: android.yml run
  33c7277 ckpt 1403: gated kestrel-g-v0.3.0-alpha26 (gecko code 125) and pushed it: handoff self-t
  bc3596c ckpt 1401: AD1b-3/4 + AD1a-3 DONE: Settings cards + launch screen + Downloads queue UI v
  af1938b ckpt 1388: AD1b-3 UI polish: Settings sections are chamfered cards with dividers (like t
```
